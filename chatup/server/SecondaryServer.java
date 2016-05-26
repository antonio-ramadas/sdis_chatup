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

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class SecondaryServer extends Server {

    private final Database serverDatabase;
    private final ServerLogger serverLogger;
    private final KryoServer mServer;
    private final HashMap<Integer, KryoClient> mClients;
    private final SecondaryServerListener mServerListener;
    private final HashMap<Integer, ServerInfo> servers;
    private final HashMap<Integer, Room> rooms;

    private int serverPort;

	public SecondaryServer(final ServerInfo paramPrimary, int tcpPort, int httpPort) throws SQLException, IOException {

        //----------------------------------------------------------------
        // 1) Inicializar servidor HTTPS para receber pedidos dos clientes
        //----------------------------------------------------------------

		super(new SecondaryDispatcher(), ServerType.SECONDARY, httpPort);

        //--------------------------------------------------------------------
        // 2) Ler para memória informações dos servidores armazenadas em disco
        //--------------------------------------------------------------------

        mServerId = paramPrimary.getId();
        mClients = new HashMap<>();
        mServerTimestamp = 0L;
        serverPort = httpPort;
        serverDatabase = new Database(this);
        serverLogger = new ServerLogger(this);

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
        // 3) Ler para memória informações das salas armazenadas em disco
        //---------------------------------------------------------------

        final HashMap<Integer, Room> myRooms = serverDatabase.getRooms();

        if (myRooms == null) {
            rooms = new HashMap<>();
        }
        else {
            rooms = myRooms;
        }

        //-----------------------------------------------------------------------
        // 4) Ler para memória associações servidor <-> sala armazenadas em disco
        //-----------------------------------------------------------------------

        System.out.println("*=====      rooms      =====*");

        rooms.forEach((roomId, roomInformation) -> {

            final Set<Integer> roomServers = serverDatabase.getServerByRoom(roomId);

            if (roomServers != null) {
                roomInformation.setServers(roomServers);
            }

            final MessageCache roomMessages = serverDatabase.getMessagesByRoom(roomId);

            if (roomMessages != null) {
                roomInformation.insertMessages(roomMessages);
            }

            System.out.println("[" + roomId + "] " + roomInformation);
        });

        //--------------------------------------------------------------------------
        // 5) Inicializar cliente TCP/SSL para receber comandos do servidor primário
        //--------------------------------------------------------------------------

        final KryoClient myClient = new KryoClient();
        final PrimaryClientListener myClientListener = new PrimaryClientListener(this, myClient);

        TcpNetwork.register(myClient);
        myClient.addListener(myClientListener);
        myClient.start();
        myClient.connect(ChatupGlobals.DefaultTimeout, paramPrimary.getAddress(), paramPrimary.getPort());

        //--------------------------------------------------------------------------------
        // 6) Inicializar servidor TCP/SSL para receber pedidos dos servidores secundários
        //--------------------------------------------------------------------------------

         mServer = new KryoServer() {

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
        return serverLogger;
    }

    private void registerUser(final String userToken, final String userEmail) {

        final String userRecord = users.get(userToken);

        if (userRecord == null) {
            users.put(userToken, userEmail);
            serverLogger.userConnected(userEmail);
        }
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

        if (rooms.containsKey(roomId)) {
            return ServerResponse.RoomNotFound;
        }

        final Room serializedRoom = new Room(
            createRoom.roomName,
            createRoom.roomPassword,
            createRoom.userToken
        );

        if (serverDatabase.insertRoom(roomId, serializedRoom)) {
            serverLogger.createRoom(createRoom.userToken, createRoom.roomName);
            rooms.put(roomId, serializedRoom);
        }
        else {
            return ServerResponse.DatabaseError;
        }

        final Room insertedRoom = rooms.get(createRoom.roomId);

        if (insertedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        final Set<Integer> roomServers = createRoom.roomServers;

        if (roomServers == null || roomServers.isEmpty()) {
            return ServerResponse.OperationFailed;
        }

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

                    if (!serverDatabase.insertServerRoom(serverId, createRoom.roomId)) {
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
                    createConnection(serverId);
                }
            }
        }

        registerUser(createRoom.userToken, createRoom.userEmail);

        return ServerResponse.SuccessResponse;
	}

    private void notifyComet() {

        for (final CometRequest cometRequest : pendingRequests) {

            if (users.containsKey(cometRequest.getToken())) {
                cometRequest.send();
            }
        }

        pendingRequests.clear();
    }

    private ServerResponse createConnection(int serverId) {

        final KryoClient kryoClient = new KryoClient();
        final ServerInfo selectedServer = servers.get(serverId);

        TcpNetwork.register(kryoClient);
        kryoClient.addListener(new SecondaryClientListener(this, kryoClient));

        if (selectedServer == null) {
           return ServerResponse.ServerNotFound;
        }

        mClients.put(serverId, kryoClient);

        try {
            kryoClient.start();
            kryoClient.connect(ChatupGlobals.DefaultTimeout, selectedServer.getAddress(), selectedServer.getPort());
        }
        catch (IOException ex) {
            return ServerResponse.ServiceOffline;
        }

        return ServerResponse.SuccessResponse;
    }

    final ServerResponse disconnectServer(int serverId) {

        System.out.println("------ ServerOffline ------");
        System.out.println("serverId:" + serverId);
        System.out.println("--------------------------");

        final ServerInfo selectedServer = servers.get(serverId);

        if (selectedServer == null) {
            return ServerResponse.ServerNotFound;
        }

        selectedServer.setStatus(false);
        serverLogger.serverOffline(serverId);

        return ServerResponse.SuccessResponse;
    }

    // TODO: Verified!
    final ServerResponse joinRoom(final JoinRoom joinRoom) {

        final String userToken = joinRoom.userToken;
        final String userEmail = users.get(userToken);

		if (userEmail == null) {
			users.put(userToken, joinRoom.userEmail);
		}

		final Room selectedRoom = rooms.get(joinRoom.roomId);

		if (selectedRoom == null) {
			return ServerResponse.RoomNotFound;
		}

		if (selectedRoom.hasUser(userToken)) {
            return ServerResponse.AlreadyJoined;
        }

		selectedRoom.registerUser(userToken);
        notifyComet();

		return ServerResponse.SuccessResponse;
	}

    // TODO: Verified!
    @Override
    public ServerResponse leaveRoom(int roomId, final String userToken) {

        final String userEmail = users.get(userToken);

        if (userEmail == null) {
            return ServerResponse.InvalidToken;
        }

        final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        if (selectedRoom.removeUser(userToken)) {
            cascadeUser(userToken);
        }
        else {
            return ServerResponse.OperationFailed;
        }

        notifyComet();

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

        final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        final Set<String> roomUsers = selectedRoom.getUsers();

        if (serverDatabase.deleteRoom(roomId)) {
            rooms.remove(roomId);
        }
        else {
            return ServerResponse.DatabaseError;
        }

        roomUsers.forEach(this::cascadeUser);

        return ServerResponse.SuccessResponse;
    }

    public ServerResponse insertMessage(final Message paramMessage) {

        final String userToken = paramMessage.getAuthor();
        final String userRecord = users.get(userToken);

        if (userRecord == null) {
            return ServerResponse.InvalidToken;
        }

        final Room selectedRoom = rooms.get(paramMessage.getId());

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        if (serverDatabase.insertMessage(paramMessage)) {

            if (selectedRoom.insertMessage(paramMessage)) {

                final Set<Integer> roomServers = selectedRoom.getServers();

                for (final Integer serverId : roomServers) {
                    mServerListener.sendServer(serverId, paramMessage);
                }

                notifyComet();

                return ServerResponse.SuccessResponse;
            }

            return ServerResponse.OperationFailed;
        }

        return ServerResponse.DatabaseError;
    }

    @Override
    public boolean validateToken(final String userToken) {
        return users.containsKey(userToken);
    }

    private ArrayList<CometRequest> pendingRequests = new ArrayList<>();

    @Override
	public ServerResponse getMessages(final HttpDispatcher httpExchange, final String userToken, int roomId, long roomTimestamp) {

        final String userRecord = users.get(userToken);

        if (userRecord == null) {
            return ServerResponse.InvalidToken;
        }

		final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

		if (!selectedRoom.hasUser(userToken)) {
			return ServerResponse.InvalidToken;
		}

        final CometRequest cometRequest = new CometRequest(selectedRoom, userToken, roomTimestamp, httpExchange);

        if (roomTimestamp <= 0L) {
            cometRequest.send();
        }
        else {
            pendingRequests.add(cometRequest);
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

        int serverId = serverInfo.getId();

		if (servers.containsKey(serverId)) {
			return updateServer(serverInfo);
		}

		if (serverDatabase.insertServer(serverInfo)) {
            servers.put(serverId, serverInfo);
            serverLogger.insertServer(serverId);
        }
        else {
            return ServerResponse.DatabaseError;
        }

        mServerTimestamp = Instant.now().getEpochSecond();

		return ServerResponse.SuccessResponse;
	}

	@Override
	public ServerResponse updateServer(final ServerInfo serverInfo) {

        int serverId = serverInfo.getId();
        final ServerInfo selectedServer = servers.get(serverId);

		if (selectedServer == null) {
			return ServerResponse.ServerNotFound;
		}

        if (serverDatabase.updateServer(serverInfo)) {
            selectedServer.setAddress(serverInfo.getAddress());
            selectedServer.setPort(serverInfo.getPort());
            selectedServer.setTimestamp(serverInfo.getTimestamp());
            serverLogger.updateServer(serverId);
        }
        else {
            return ServerResponse.DatabaseError;
        }

        mServerTimestamp = Instant.now().getEpochSecond();

		return ServerResponse.SuccessResponse;
	}

	@Override
	public ServerResponse deleteServer(int serverId) {

        if (servers.get(serverId) == null) {
            return ServerResponse.ServerNotFound;
        }

        rooms.forEach((roomId, room) -> {

            if (room.removeServer(serverId)) {
                serverDatabase.deleteServerRoom(serverId, roomId);
            }
        });

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

    @Override
    public ServerResponse syncRoom(int roomId, int serverId) {

        System.out.println("------ SyncRoom ------");
        System.out.println("roomId:" + roomId);
        System.out.println("serverId:" + serverId);
        System.out.println("--------------------------");

        final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        if (serverId != mServerId) {

            mServerListener.sendServer(serverId,
                new SyncRoomResponse(roomId, selectedRoom)
            );
        }

        return ServerResponse.SuccessResponse;
    }

    final ServerResponse updateRoom(final SyncRoomResponse updateRoom) {

        System.out.println("------ UpdateRoom ------");
        System.out.println("roomId:" + updateRoom.roomId);
        System.out.println("roomTimestamp:" + updateRoom.roomTimestamp);
        System.out.println("roomServers:" + updateRoom.roomServers);
        System.out.println("roomUsers:" + updateRoom.roomUsers);
        System.out.println("--------------------------");

        final Room selectedRoom = rooms.get(updateRoom.roomId);

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        boolean roomUpdated = false;
        final Set<Integer> newServers = selectedRoom.updateServers(updateRoom.roomServers);

        if (newServers.size() > 0) {

            roomUpdated = true;

            for (final Integer serverId : newServers) {
                serverDatabase.insertServerRoom(serverId, updateRoom.roomId);
            }

            final Set<Integer> deletedServers = selectedRoom.getServers();

            deletedServers.removeAll(updateRoom.roomServers);

            for (final Integer serverId : deletedServers) {
                serverDatabase.deleteServerRoom(serverId, updateRoom.roomId);
            }
        }

        if (selectedRoom.updateUsers(updateRoom.roomUsers)) {
            roomUpdated = true;
        }

        if (roomUpdated) {
            return ServerResponse.SuccessResponse;
        }

        return ServerResponse.OperationFailed;
    }

    final ServerOnline getInformation() {
        return new ServerOnline(mServerId, mServerTimestamp, serverPort);
    }
}