package chatup.server;

import chatup.http.*;
import chatup.main.ChatupGlobals;
import chatup.model.Database;
import chatup.model.MessageCache;
import chatup.model.Room;
import chatup.model.Message;
import chatup.tcp.*;

import kryonet.Connection;
import kryonet.KryoClient;
import kryonet.KryoServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class SecondaryServer extends Server {

    private final Database serverDatabase;
    private final ServerLogger serverLogger;
    private final SecondaryServerListener mServerListener;
    private final HashMap<Integer, ServerInfo> servers;
    private final HashMap<Integer, Room> rooms;

    private int serverPort;

	public SecondaryServer(final ServerInfo paramPrimary, int tcpPort, int httpPort) throws SQLException, IOException {

        //----------------------------------------------------------------
        // 1) Inicializar servidor HTTPS para receber pedidos dos clientes
        //----------------------------------------------------------------

		super(new SecondaryDispatcher(), ServerType.SECONDARY, httpPort);

        //---------------------------------------------------------------
        // 2) Inicialização do servidor; obter data da última atualização
        //---------------------------------------------------------------

        mServerId = paramPrimary.getId();
        mServerTimestamp = 0L;
        serverPort = httpPort;
        serverDatabase = new Database(this);
        serverLogger = new ServerLogger(this);

        //--------------------------------------------------------------------
        // 3) Ler para memória informações dos servidores armazenadas em disco
        //--------------------------------------------------------------------

        final HashMap<Integer, ServerInfo> myServers = serverDatabase.getServers();

        if (myServers == null) {
            servers = new HashMap<>();
        }
        else {
            servers = myServers;
        }

        System.out.println("*=====     servers     =====*");
        servers.forEach((k, v) -> System.out.println("[" + k + "] " + v));

        //---------------------------------------------------------------
        // 4) Ler para memória informações das salas armazenadas em disco
        //---------------------------------------------------------------

        final HashMap<Integer, Room> myRooms = serverDatabase.getRooms();

        if (myRooms == null) {
            rooms = new HashMap<>();
        }
        else {
            rooms = myRooms;
        }

        //-----------------------------------------------------------------------
        // 5) Ler para memória associações servidor <-> sala armazenadas em disco
        //-----------------------------------------------------------------------

        System.out.println("*=====      rooms      =====*");

        rooms.forEach((roomId, roomInformation) -> {

            final Set<Integer> roomServers = serverDatabase.getServerByRoom(roomId);

            if (roomServers != null && roomServers.size() > 0) {
                roomInformation.setServers(roomServers);
            }

            final MessageCache roomMessages = serverDatabase.getMessagesByRoom(roomId);

            if (roomMessages != null && roomMessages.size() > 0) {
                roomInformation.insertMessages(roomMessages);
            }

            System.out.println("[" + roomId + "] " + roomInformation);
        });

        //----------------------------------------------------------------------
        // 6) Inicializar cliente TCP para receber comandos do servidor primário
        //----------------------------------------------------------------------

        final KryoClient myClient = new KryoClient();
        final PrimaryClientListener myClientListener = new PrimaryClientListener(this, myClient);

        TcpNetwork.registerPrimary(myClient);
        myClient.addListener(myClientListener);
        myClient.start();
        myClient.connect(ChatupGlobals.DefaultTimeout, paramPrimary.getAddress(), paramPrimary.getPort());

        //----------------------------------------------------------------------------
        // 7) Inicializar servidor TCP para receber pedidos dos servidores secundários
        //----------------------------------------------------------------------------

        final KryoServer mServer = new KryoServer() {

            @Override
            protected Connection newConnection() {
                return new ServerConnection();
            }
        };

        mServerListener = new SecondaryServerListener(this, mServer);
        TcpNetwork.registerSecondary(mServer);
        mServer.addListener(mServerListener);
        mServer.bind(tcpPort);
        mServer.start();
	}

	private int mServerId;
    private long mServerTimestamp;

    @Override
    public int getId() {
        return mServerId;
    }

    final ServerLogger getLogger() {
        return serverLogger;
    }

    final ServerResponse createRoom(final CreateRoom createRoom) {

        int roomId = createRoom.roomId;

        System.out.println("------ CreateRoom ------");
        System.out.println("roomId:" + createRoom.roomId);
        System.out.println("roomName:" + createRoom.roomName);
        System.out.println("roomPassword:" + createRoom.roomPassword);
        System.out.println("roomOwner:" + createRoom.userToken);
        System.out.println("roomServers:" + createRoom.roomServers);
        System.out.println("--------------------------");

        //----------------------------------------------------------
        // 1) Verificar se sala escolhida já existe na base de dados
        //----------------------------------------------------------

        if (rooms.containsKey(roomId)) {
            return ServerResponse.RoomNotFound;
        }

        //-----------------------------------------------------------
        // 2) Inserir informações da sala escolhida no servidor local
        //-----------------------------------------------------------

        final Room serializedRoom = new Room(
            createRoom.roomName,
            createRoom.roomPassword,
            createRoom.roomTimestamp,
            createRoom.userToken
        );

        if (serverDatabase.insertRoom(roomId, serializedRoom)) {
            serverLogger.createRoom(createRoom.userToken, createRoom.roomName);
            rooms.put(roomId, serializedRoom);
        }
        else {
            return ServerResponse.DatabaseError;
        }

        //------------------------------------------------------
        // 2) Obter lista de servidores mirror da sala escolhida
        //------------------------------------------------------

        final Set<Integer> roomServers = createRoom.roomServers;

        if (roomServers == null) {
            return ServerResponse.OperationFailed;
        }

        final Room insertedRoom = rooms.get(createRoom.roomId);

        if (insertedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        //--------------------------------------------------------
        // 3) Registar servidores mirror da sala no servidor local
        //--------------------------------------------------------

        for (final Integer serverId : roomServers) {

            if (serverId == mServerId) {
                continue;
            }

            final ServerInfo serverInformation = servers.get(serverId);

            if (serverInformation == null) {
                serverLogger.serverNotFound(serverId);
            }
            else {

                if (insertedRoom.registerServer(serverId)) {

                    serverLogger.insertMirror(roomId, serverId);

                    if (!serverDatabase.associateServer(serverId, createRoom.roomId)) {
                        return ServerResponse.DatabaseError;
                    }
                }
                else {
                    serverLogger.mirrorExists(roomId, serverId);
                }

                if (serverInformation.isOnline()) {
                    System.out.println("server is already online, not trying to reconnect...");
                }
                else {

                    System.out.println("connecting to server #" + serverId + "...");

                    final ServerResponse serverResponse = createConnection(serverId);

                    if (serverResponse == ServerResponse.SuccessResponse) {
                        System.out.println("connection successful");
                    }
                    else {
                        return serverResponse;
                    }
                }
            }
        }

        //--------------------------------------------------------------
        // 4) Registar entrada do proprietário da sala no servidor local
        //--------------------------------------------------------------

        final String userRecord = users.get(createRoom.userToken);

        if (userRecord == null) {
            users.put(createRoom.userToken, createRoom.userEmail);
            serverLogger.userConnected(createRoom.userEmail);
        }

        return ServerResponse.SuccessResponse;
	}

    private ServerResponse createConnection(int serverId) {

        final KryoClient kryoClient = new KryoClient();
        final ServerInfo selectedServer = servers.get(serverId);

        TcpNetwork.registerSecondary(kryoClient);
        kryoClient.addListener(new SecondaryClientListener(this, kryoClient));

        if (selectedServer == null) {
            return ServerResponse.ServerNotFound;
        }

        try {
            kryoClient.start();
            kryoClient.connect(ChatupGlobals.DefaultTimeout, selectedServer.getAddress(), selectedServer.getPort());
        }
        catch (final IOException ex) {
            return ServerResponse.ServiceOffline;
        }

        return ServerResponse.SuccessResponse;
    }

    final ServerResponse disconnectServer(int serverId) {

        //-----------------------------------------------------------
        // 1) Verificar se servidor escolhido existe na base de dados
        //-----------------------------------------------------------

        final ServerInfo selectedServer = servers.get(serverId);

        if (selectedServer == null) {
            return ServerResponse.ServerNotFound;
        }

        //------------------------------------------------------
        // 2) Actualizar estado da ligação ao servidor escolhido
        //------------------------------------------------------

        selectedServer.setStatus(false);
        serverLogger.serverOffline(serverId);

        return ServerResponse.SuccessResponse;
    }

    final ServerResponse joinRoom(final JoinRoom joinRoom) {

        //-----------------------------------------------------------
        // 1) Verificar se utilizador tem sessão iniciada no servidor
        //-----------------------------------------------------------

        final String userToken = joinRoom.userToken;
        final String userEmail = users.get(userToken);

		if (userEmail == null) {
			users.put(userToken, joinRoom.userEmail);
		}

        //--------------------------------------------------------
        // 2) Verificar se sala escolhida existe no servidor local
        //--------------------------------------------------------

        final Room selectedRoom = rooms.get(joinRoom.roomId);

		if (selectedRoom == null) {
			return ServerResponse.RoomNotFound;
		}

        //----------------------------------------------------
        // 3) Registar entrada do utilizador na sala escolhida
        //----------------------------------------------------

        if (selectedRoom.hasUser(userToken)) {
            return ServerResponse.AlreadyJoined;
        }

		selectedRoom.registerUser(userToken);

        //-----------------------------------------------------------------------
        // 4) Responder aos pedidos pendentes de todos os utilizadores desta sala
        //-----------------------------------------------------------------------

        for (final PushRequest cometRequest : pendingRequests) {

            if (users.containsKey(cometRequest.getToken())) {
                cometRequest.send();
            }
        }

        pendingRequests.clear();

		return ServerResponse.SuccessResponse;
	}

    @Override
    public ServerResponse leaveRoom(int roomId, final String userToken) {

        //-----------------------------------------------------------
        // 1) Verificar se utilizador tem sessão iniciada no servidor
        //-----------------------------------------------------------

        final String userEmail = users.get(userToken);

        if (userEmail == null) {
            return ServerResponse.InvalidToken;
        }

        //--------------------------------------------------------
        // 2) Verificar se sala escolhida existe no servidor local
        //--------------------------------------------------------

        final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        //-----------------------------------------------
        // 3) Remover utiizador da sala no servidor local
        //-----------------------------------------------

        if (selectedRoom.removeUser(userToken)) {
            cascadeUser(userToken);
        }
        else {
            return ServerResponse.OperationFailed;
        }

        //-----------------------------------------------------------------------
        // 4) Responder aos pedidos pendentes de todos os utilizadores desta sala
        //-----------------------------------------------------------------------

        for (final PushRequest pushRequest : pendingRequests) {

            if (users.containsKey(pushRequest.getToken())) {
                pushRequest.send();
            }
        }

        pendingRequests.clear();

        return ServerResponse.SuccessResponse;
    }

    private void cascadeUser(final String userToken) {

        boolean userRemoved = true;

        for (final Room currentRoom : rooms.values()) {

            if (currentRoom.hasUser(userToken)) {
                userRemoved = false;
            }
        }

        if (userRemoved) {
            serverLogger.removeUser(userToken);
            users.remove(userToken);
        }
    }

    @Override
    public ServerResponse deleteRoom(int roomId) {

        //--------------------------------------------------------
        // 1) Verificar se sala escolhida existe no servidor local
        //--------------------------------------------------------

        final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        //-----------------------------------------------------------------------
        // 2) Apagar sala do servidor local, registar alterações na base de dados
        //-----------------------------------------------------------------------

        if (serverDatabase.deleteRoom(roomId)) {
            rooms.remove(roomId);
        }
        else {
            return ServerResponse.DatabaseError;
        }

        //--------------------------------------------------------------------------
        // 3) Apagar todos os utilizadores que estavam conectados apenas a esta sala
        //--------------------------------------------------------------------------

        selectedRoom.getUsers().forEach(this::cascadeUser);

        return ServerResponse.SuccessResponse;
    }

    public ServerResponse sendMessage(int roomId, final String userToken, final String messageBody) {

        final String userRecord = users.get(userToken);

        if (userRecord == null) {
            return ServerResponse.InvalidToken;
        }

        return sendMessage(
            new Message(roomId, userToken, userRecord, Instant.now().toEpochMilli(), messageBody)
        );
    }

    @Override
    public ServerResponse sendMessage(final Message paramMessage) {

        //-----------------------------------------------------------
        // 1) Verificar se utilizador tem sessão iniciada no servidor
        //-----------------------------------------------------------

        final String userToken = paramMessage.getToken();
        final String userRecord = users.get(userToken);

        if (userRecord == null) {
            return ServerResponse.InvalidToken;
        }

        //---------------------------------------------------------
        // 2) Verificar se sala de destino existe no servidor local
        //---------------------------------------------------------

        final Room selectedRoom = rooms.get(paramMessage.getId());

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        //------------------------------------------------
        // 3) Registar mensagem recebida no servidor local
        //------------------------------------------------

        if (selectedRoom.insertMessage(paramMessage)) {

            if (serverDatabase.insertMessage(paramMessage)) {
                return ServerResponse.SuccessResponse;
            }

            return ServerResponse.DatabaseError;
        }

        //--------------------------------------------------------------------
        // 4) Enviar mensagem para os outros servidores secundários conectados
        //--------------------------------------------------------------------

        final Set<Integer> roomServers = selectedRoom.getServers();

        for (final Integer serverId : roomServers) {
            mServerListener.sendServer(serverId, paramMessage);
        }

        //-----------------------------------------------------------------------
        // 5) Responder aos pedidos pendentes de todos os utilizadores desta sala
        //-----------------------------------------------------------------------

        for (final PushRequest pushRequest : pendingRequests) {

            if (users.containsKey(pushRequest.getToken())) {
                pushRequest.send();
            }
        }

        pendingRequests.clear();
        serverLogger.sendMessage(paramMessage.getId());

        return ServerResponse.SuccessResponse;
    }

    final ServerResponse insertMessage(final Message paramMessage) {

        //-----------------------------------------------------------
        // 1) Verificar se utilizador tem sessão iniciada no servidor
        //-----------------------------------------------------------

        final String userToken = paramMessage.getToken();
        final String userRecord = users.get(userToken);

        if (userRecord == null) {
            return ServerResponse.InvalidToken;
        }

        //------------------------------------------------------
        // 2) Verificar se sala recebida existe na base de dados
        //------------------------------------------------------

        final Room selectedRoom = rooms.get(paramMessage.getId());

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        //----------------------------------------------------
        // 3) Registar alterações na base de dados do servidor
        //----------------------------------------------------

        if (selectedRoom.insertMessage(paramMessage)) {

            if (serverDatabase.insertMessage(paramMessage)) {
                return ServerResponse.SuccessResponse;
            }

            return ServerResponse.DatabaseError;
        }

        return ServerResponse.OperationFailed;
    }

    @Override
    public boolean validateToken(final String userToken) {
        return users.containsKey(userToken);
    }

    private ArrayList<PushRequest> pendingRequests = new ArrayList<>();

    @Override
	public ServerResponse getMessages(final HttpDispatcher httpExchange, final String userToken, int roomId, long lastUpdate) {

        //-----------------------------------------------------------
        // 1) Verificar se utilizador tem sessão iniciada no servidor
        //-----------------------------------------------------------

        final String userRecord = users.get(userToken);

        if (userRecord == null) {
            return ServerResponse.InvalidToken;
        }

        //-------------------------------------------------------
        // 2) Verificar se sala escolhida existe na base de dados
        //-------------------------------------------------------

		final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        //---------------------------------------------------------
        // 1) Verificar se utilizador se encontra registado na sala
        //---------------------------------------------------------

		if (!selectedRoom.hasUser(userToken)) {
			return ServerResponse.InvalidToken;
		}

        //----------------------------------------------------------------------------------
        // 4) Atrasar pedido do utilizador, adicionando aos pedidos pendentes neste servidor
        //----------------------------------------------------------------------------------

        final PushRequest pushRequest = new PushRequest(selectedRoom, userToken, lastUpdate, httpExchange);

        if (lastUpdate <= selectedRoom.getTimestamp()) {
            pushRequest.send();
        }
        else {
            pendingRequests.add(pushRequest);
        }

        return ServerResponse.SuccessResponse;
	}

	@Override
	public ServerResponse insertServer(final ServerInfo serverInfo) {

        System.out.println("------ InsertServer ------");
        System.out.println("serverId:" + serverInfo.getId());
        System.out.println("serverAddress:" + serverInfo.getAddress());
        System.out.println("serverPort:" + serverInfo.getPort());
        System.out.println("--------------------------");

        //--------------------------------------------------------------
        // 1) Verificar se servidor escolhido já existe na base de dados
        //--------------------------------------------------------------

        int serverId = serverInfo.getId();

		if (servers.containsKey(serverId)) {
			return updateServer(serverInfo);
		}

        //------------------------------------------------
        // 2) Registar entrada de novo servidor secundário
        //------------------------------------------------

		if (serverDatabase.insertServer(serverInfo)) {
            servers.put(serverId, serverInfo);
            serverLogger.insertServer(serverId);
        }
        else {
            return ServerResponse.DatabaseError;
        }

        //----------------------------------------------------------
        // 3) Atualizar data da última atualização do servidor local
        //----------------------------------------------------------

        mServerTimestamp = Instant.now().getEpochSecond();

		return ServerResponse.SuccessResponse;
	}

	@Override
	public ServerResponse updateServer(final ServerInfo serverInfo) {

        //-----------------------------------------------------------
        // 1) Verificar se servidor escolhido existe na base de dados
        //-----------------------------------------------------------

        int serverId = serverInfo.getId();
        final ServerInfo selectedServer = servers.get(serverId);

		if (selectedServer == null) {
			return ServerResponse.ServerNotFound;
		}

        //------------------------------------------------
        // 2) Actualizar informações relativas ao servidor
        //------------------------------------------------

        if (serverDatabase.updateServer(serverInfo)) {
            selectedServer.setAddress(serverInfo.getAddress());
            selectedServer.setPort(serverInfo.getPort());
            selectedServer.setTimestamp(serverInfo.getTimestamp());
            serverLogger.updateServer(serverId);
        }
        else {
            return ServerResponse.DatabaseError;
        }

        //---------------------------------------------------------
        // 3) Alterar data da última actualização do servidor local
        //---------------------------------------------------------

        mServerTimestamp = Instant.now().getEpochSecond();

		return ServerResponse.SuccessResponse;
	}

	@Override
	public ServerResponse deleteServer(int serverId) {

        //-----------------------------------------------------------
        // 1) Verificar se servidor escolhido existe na base de dados
        //-----------------------------------------------------------

        if (servers.get(serverId) == null) {
            return ServerResponse.ServerNotFound;
        }

        if (serverDatabase.deleteServerRooms(serverId)) {
            rooms.forEach((roomId, room) -> room.removeServer(serverId));
        }
        else {
            return ServerResponse.DatabaseError;
        }

        if (serverDatabase.deleteServer(serverId)) {
            servers.remove(serverId);
        }
        else {
            return ServerResponse.DatabaseError;
        }

        mServerTimestamp = Instant.now().getEpochSecond();

        return ServerResponse.SuccessResponse;
	}

	@Override
	public ServerResponse userDisconnect(final String userEmail, final String userToken) {

        final String userRecord = users.get(userToken);

        if (userRecord != null && userRecord.equals(userEmail)) {
            rooms.forEach((roomId, room) -> room.removeUser(userToken));
            users.remove(userToken);
        }
        else {
            return ServerResponse.InvalidToken;
        }

		return ServerResponse.SuccessResponse;
	}

    final ServerResponse syncRoom(final SyncRoom syncRoom, int serverId) {

        System.out.println("------ SyncRoom ------");
        System.out.println("roomId:" + syncRoom.roomId);
        System.out.println("roomTimestamp:" + syncRoom.roomTimestamp);
        System.out.println("serverId:" + serverId);
        System.out.println("--------------------------");

        final Room selectedRoom = rooms.get(syncRoom.roomId);

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        final UpdateRoom updateRoom = new UpdateRoom(syncRoom.roomId, selectedRoom);

        if (serverId == mServerId) {
            return ServerResponse.SuccessResponse;
        }

        if (syncRoom.roomTimestamp < selectedRoom.getTimestamp()) {
            mServerListener.sendServer(serverId, updateRoom);
        }
        else {
            mServerListener.sendServer(serverId, null);
        }

        return ServerResponse.SuccessResponse;
    }

    final ServerResponse updateRoom(final UpdateRoom updateRoom) {

        System.out.println("------ UpdateRoom ------");
        System.out.println("roomId:" + updateRoom.roomId);
        System.out.println("roomTimestamp:" + updateRoom.roomObject.getTimestamp());
        System.out.println("roomServers:" + updateRoom.roomObject.getServers());
        System.out.println("roomUsers:" + updateRoom.roomObject.getUsers());
        System.out.println("--------------------------");

        if (updateRoom.roomObject == null || updateRoom.roomId < 0) {
            return ServerResponse.MissingParameters;
        }

        if (rooms.containsKey(updateRoom.roomId)) {
            rooms.put(updateRoom.roomId, updateRoom.roomObject);
        }
        else {
            return ServerResponse.RoomNotFound;
        }

        if (serverDatabase.deleteRoomServers(updateRoom.roomId)) {

            final Set<Integer> roomServers = updateRoom.roomObject.getServers();

            for (final Integer serverId : roomServers) {
                serverDatabase.associateServer(serverId, updateRoom.roomId);
            }
        }
        else {
            return ServerResponse.DatabaseError;
        }

        return ServerResponse.SuccessResponse;
    }

    private String getAddress() {

        final StringBuilder response = new StringBuilder();
        final HttpURLConnection httpConnection;

        try {
            httpConnection = (HttpURLConnection) new URL("http://checkip.amazonaws.com").openConnection();
        }
        catch (final IOException ex) {

            try {
                return InetAddress.getLocalHost().getHostAddress();
            }
            catch (UnknownHostException e1) {
                return null;
            }
        }

        httpConnection.addRequestProperty("Protocol", "Http/1.1");
        httpConnection.addRequestProperty("Connection", "keep-alive");
        httpConnection.addRequestProperty("Keep-Alive", "1000");
        httpConnection.setDoInput(true);

        try (final BufferedReader br = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()))) {
            
            for (String inputLine = br.readLine(); inputLine != null; inputLine = br.readLine()) {
                response.append(inputLine);
            }
        }
        catch (final IOException ex) {

            try {
                return InetAddress.getLocalHost().getHostAddress();
            }
            catch (UnknownHostException e1) {
                return null;
            }
        }

        return response.toString();
    }

    final ServerOnline getInformation() {
        return new ServerOnline(mServerId, mServerTimestamp, getAddress(), serverPort);
    }
}