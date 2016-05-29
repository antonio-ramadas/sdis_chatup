package chatup.server;

import chatup.http.*;
import chatup.main.ChatupGlobals;
import chatup.model.Database;
import chatup.model.MessageCache;
import chatup.model.Room;
import chatup.model.Message;
import chatup.tcp.*;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.KryoNetException;
import com.esotericsoftware.kryonet.Server;

import com.esotericsoftware.minlog.Log;
import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;

public class SecondaryServer extends AbstractServer {

    private final Database mDatabase;
    private final ServerLogger mLogger;
    private final SecondaryServerListener mServerListener;
    private final ConcurrentHashMap<Integer, ServerInfo> servers;
    private final ConcurrentHashMap<Integer, Room> rooms;

	public SecondaryServer(final ServerInfo paramPrimary, int tcpPort, int httpPort) throws SQLException, IOException {

        //----------------------------------------------------------------
        // 1) Inicializar servidor HTTPS para receber pedidos dos clientes
        //----------------------------------------------------------------

		super(ServerType.SECONDARY, tcpPort, httpPort);
        Log.set(2);

        //---------------------------------------------------------------
        // 2) Inicialização do servidor; obter data da última atualização
        //---------------------------------------------------------------

        mServerId = paramPrimary.getId();
        mServerTimestamp = 0L;
        mDatabase = new Database(this);
        mLogger = new ServerLogger(this);

        //--------------------------------------------------------------------
        // 3) Ler para memória informações dos servidores armazenadas em disco
        //--------------------------------------------------------------------

        final ConcurrentHashMap<Integer, ServerInfo> myServers = mDatabase.getServers();

        if (myServers == null) {
            servers = new ConcurrentHashMap<>();
        }
        else {
            servers = myServers;
        }

        System.out.println("*=====     servers     =====*");
        servers.forEach((k, v) -> System.out.println("[" + k + "] " + v));

        //---------------------------------------------------------------
        // 4) Ler para memória informações das salas armazenadas em disco
        //---------------------------------------------------------------

        final ConcurrentHashMap<Integer, Room> myRooms = mDatabase.getRooms();

        if (myRooms == null) {
            rooms = new ConcurrentHashMap<>();
        }
        else {
            rooms = myRooms;
        }

        //-----------------------------------------------------------------------
        // 5) Ler para memória associações servidor <-> sala armazenadas em disco
        //-----------------------------------------------------------------------

        System.out.println("*=====      rooms      =====*");

        rooms.forEach((roomId, roomInformation) -> {

            final HashSet<Integer> roomServers = mDatabase.getServerByRoom(roomId);

            if (roomServers != null && roomServers.size() > 0) {
                roomInformation.setServers(roomServers);
            }

            final MessageCache roomMessages = mDatabase.getMessagesByRoom(roomId);

            if (roomMessages != null && roomMessages.size() > 0) {
                roomInformation.insertMessages(roomMessages);
            }

            System.out.println("[" + roomId + "] " + roomInformation);
        });

        //----------------------------------------------------------------------
        // 6) Inicializar cliente TCP para receber comandos do servidor primário
        //----------------------------------------------------------------------

        final Client myClient = new Client();
        final PrimaryClientListener myClientListener = new PrimaryClientListener(this, myClient);

        TcpNetwork.register(myClient);
        myClient.addListener(myClientListener);
        myClient.start();
        myClient.connect(ChatupGlobals.DefaultTimeout, paramPrimary.getAddress(), paramPrimary.getTcpPort());

        //----------------------------------------------------------------------------
        // 7) Inicializar servidor TCP para receber pedidos dos servidores secundários
        //----------------------------------------------------------------------------

        final Server mServer = new Server(32768, 8192) {

            @Override
            protected Connection newConnection() {
                return new ServerConnection();
            }
        };

        mServerListener = new SecondaryServerListener(this, mServer);
        TcpNetwork.register(mServer);
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
        return mLogger;
    }

    private ServerResponse refreshRoom(int roomId) {

        final Room selectedRoom;

        selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        if (selectedRoom.hasRefreshed()) {
            return ServerResponse.SuccessResponse;
        }

        final Set<Integer> roomServers = selectedRoom.getServers();
        int mostRecentServer = 0;
        long mostRecentTimestamp = 0L;

        if (roomServers == null || roomServers.isEmpty()) {
            return ServerResponse.ServiceOffline;
        }

        for (final Integer serverId : roomServers) {

            final ServerInfo serverInfo = servers.get(serverId);

            if (serverInfo == null) {
                continue;
            }

            if (serverInfo.isOnline() /*&& serverInfo.getTimestamp() > mostRecentTimestamp*/) {
                mostRecentServer = serverId;
                mostRecentTimestamp = serverInfo.getTimestamp();
            }
        }

        if (mostRecentServer == 0 || mostRecentTimestamp <= 0L) {
            return ServerResponse.ServiceOffline;
        }

        mServerListener.sendServer(mostRecentServer, new SyncRoom(roomId, mServerTimestamp));
        selectedRoom.setRefreshed(true);

        return ServerResponse.SuccessResponse;
    }

    final ServerResponse createRoom(final CreateRoom createRoom) {

        //----------------------------------------------------------
        // 1) Verificar se sala escolhida já existe na base de dados
        //----------------------------------------------------------

        int roomId = createRoom.roomId;

        if (rooms.containsKey(roomId)) {
            return ServerResponse.RoomNotFound;
        }

        final Room serializedRoom = new Room(
            createRoom.roomName,
            createRoom.roomPassword,
            createRoom.roomTimestamp,
            createRoom.userToken
        );

        //--------------------------------------------------------
        // 3) Registar servidores mirror da sala no servidor local
        //--------------------------------------------------------

        final HashSet<Integer> roomServers = createRoom.roomServers;

        if (roomServers != null && roomServers.size() > 0) {

            for (final Integer serverId : roomServers) {

                if (serverId == mServerId) {
                    continue;
                }

                final ServerInfo serverInformation = servers.get(serverId);

                if (serverInformation == null) {
                    mLogger.serverNotFound(serverId);
                }
                else {

                    if (serializedRoom.registerServer(serverId)) {
                        mLogger.insertMirror(roomId, serverId);
                        mDatabase.associateServer(serverId, createRoom.roomId);
                    }
                    else {
                        mLogger.mirrorExists(roomId, serverId);
                    }
                }
            }
        }

        //-----------------------------------------------------------
        // 4) Inserir informações da sala escolhida no servidor local
        //-----------------------------------------------------------

        if (mDatabase.insertRoom(roomId, serializedRoom)) {
            rooms.put(roomId, serializedRoom);
        }
        else {
            return ServerResponse.DatabaseError;
        }

        //--------------------------------------------------------------
        // 4) Registar entrada do proprietário da sala no servidor local
        //--------------------------------------------------------------

        users.putIfAbsent(createRoom.userToken, createRoom.userEmail);
        mLogger.userConnected(createRoom.userEmail);

        return ServerResponse.SuccessResponse;
    }

    private ServerResponse createConnection(int serverId) {

        final Client kryoClient = new Client();
        final ServerInfo selectedServer = servers.get(serverId);

        if (selectedServer == null) {
            return ServerResponse.ServerNotFound;
        }

        TcpNetwork.register(kryoClient);
        kryoClient.addListener(new SecondaryClientListener(this, kryoClient));

        try {

            try {
                kryoClient.start();
                kryoClient.connect(ChatupGlobals.DefaultTimeout, selectedServer.getAddress(), selectedServer.getTcpPort());
            }
            catch (final KryoNetException ex) {
                ChatupGlobals.warn(getType().toString(), ex);
            }

            if (kryoClient.isConnected()) {
                selectedServer.setStatus(true);
            }
        }
        catch (final IOException ex) {
            return ServerResponse.ServiceOffline;
        }

        return ServerResponse.SuccessResponse;
    }

    final ServerResponse disconnectServer(int serverId) {

        final ServerInfo selectedServer = servers.get(serverId);

        if (selectedServer == null) {
            return ServerResponse.ServerNotFound;
        }

        selectedServer.setStatus(false);

        mLogger.serverOffline(serverId);

        return ServerResponse.SuccessResponse;
    }

    final ServerResponse joinRoom(final JoinRoom joinRoom) {

        //-----------------------------------------------------------
        // 1) Verificar se utilizador tem sessão iniciada no servidor
        //-----------------------------------------------------------

        users.putIfAbsent(joinRoom.userToken, joinRoom.userEmail);

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

        if (selectedRoom.hasUser(joinRoom.userToken)) {
            return ServerResponse.AlreadyJoined;
        }

		selectedRoom.registerUser(joinRoom.userToken);
        connectMirrors(selectedRoom);

        //-----------------------------------------------------------------------
        // 4) Responder aos pedidos pendentes de todos os utilizadores desta sala
        //-----------------------------------------------------------------------

        final ServerResponse operationResult = refreshRoom(joinRoom.roomId);

        for (final PushRequest cometRequest : pendingRequests) {

            if (users.containsKey(cometRequest.getToken())) {
                cometRequest.send();
            }
        }

        pendingRequests.clear();

		return operationResult;
	}

    private ServerResponse connectMirrors(final Room paramRoom) {

        final Set<Integer> roomServers = paramRoom.getServers();

        if (roomServers == null || roomServers.isEmpty()) {
            return ServerResponse.SuccessResponse;
        }

        for (final Integer serverId : roomServers) {

            final ServerInfo serverInformation = servers.get(serverId);

            if (serverInformation == null) {
                continue;
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
            mLogger.removeUser(userToken);
            users.remove(userToken);
        }
    }

    @Override
    public ServerResponse deleteRoom(int roomId) {

        final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        if (mDatabase.deleteRoom(roomId)) {
            rooms.remove(roomId);
        }
        else {
            return ServerResponse.DatabaseError;
        }

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

        if (users.get(userToken) == null) {
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

        if (mDatabase.insertMessage(paramMessage)) {
            selectedRoom.insertMessage(paramMessage);
        }
        else {
            return ServerResponse.DatabaseError;
        }

        //--------------------------------------------------------------------
        // 4) Enviar mensagem para os outros servidores secundários conectados
        //--------------------------------------------------------------------

        final Set<Integer> roomServers = selectedRoom.getServers();

        if (roomServers != null && roomServers.size() > 0) {

            for (final Integer serverId : roomServers) {

                if (serverId != mServerId) {
                    mServerListener.sendServer(serverId, paramMessage);
                }
            }
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
        mLogger.sendMessage(paramMessage.getId());

        return ServerResponse.SuccessResponse;
    }

    final ServerResponse insertMessage(final Message paramMessage) {

        final Room selectedRoom = rooms.get(paramMessage.getId());

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        if (mDatabase.insertMessage(paramMessage)) {
            selectedRoom.insertMessage(paramMessage);
        }
        else {
            return ServerResponse.DatabaseError;
        }

        for (final PushRequest pushRequest : pendingRequests) {

            if (users.containsKey(pushRequest.getToken())) {
                pushRequest.send();
            }
        }

        pendingRequests.clear();

        return ServerResponse.SuccessResponse;
    }

    @Override
    public boolean validateToken(final String userToken) {
        return users.containsKey(userToken);
    }

    private ArrayList<PushRequest> pendingRequests = new ArrayList<>();

    @Override
	public ServerResponse getMessages(final HttpExchange httpExchange, final String userToken, int roomId, long lastUpdate) {

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
        System.out.println("serverTcpPort:" + serverInfo.getTcpPort());
        System.out.println("serverHttpPort:" + serverInfo.getHttpPort());
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

		if (mDatabase.insertServer(serverInfo)) {
            servers.put(serverId, serverInfo);
            mLogger.insertServer(serverId);
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

        if (mDatabase.updateServer(serverInfo)) {
            selectedServer.setAddress(serverInfo.getAddress());
            selectedServer.setHttpPort(serverInfo.getHttpPort());
            selectedServer.setTcpPort(serverInfo.getTcpPort());
            selectedServer.setTimestamp(serverInfo.getTimestamp());
            mLogger.updateServer(serverId);
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

        if (mDatabase.deleteServerRooms(serverId)) {
            rooms.forEach((roomId, room) -> room.removeServer(serverId));
        }
        else {
            return ServerResponse.DatabaseError;
        }

        if (mDatabase.deleteServer(serverId)) {
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

        if (mDatabase.deleteRoomServers(updateRoom.roomId)) {

            final Set<Integer> roomServers = updateRoom.roomObject.getServers();

            if (roomServers != null && roomServers.size() > 0) {

                for (final Integer serverId : roomServers) {
                    mDatabase.associateServer(serverId, updateRoom.roomId);
                }
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
        return new ServerOnline(mServerId, mServerTimestamp, getAddress(), getTcpPort(), getHttpPort());
    }
}