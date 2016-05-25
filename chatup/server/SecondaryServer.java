package chatup.server;

import chatup.http.HttpFields;
import chatup.http.SecondaryDispatcher;
import chatup.http.ServerResponse;
import chatup.main.ChatupGlobals;
import chatup.model.Database;
import chatup.model.Room;
import chatup.model.Message;
import chatup.tcp.*;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;
import javafx.util.Pair;
import kryonet.Connection;
import kryonet.KryoClient;
import kryonet.KryoServer;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

public class SecondaryServer extends Server {

    private final Database serverDatabase;
    private final ServerLogger serverLogger;
    private final SecondaryServerListener myServerListener;
	private final SecondaryClientListener myClientListener;
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

        serverId = paramPrimary.getId();
        serverTimestamp = 0L;
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

        if (getLogger().debugEnabled()) {

            System.out.println("*=====     servers     =====*");

            servers.forEach((serverId, serverInformation) -> {
                System.out.println("[" + serverId + "] " + serverInformation);
            });
        }

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

        if (getLogger().debugEnabled()) {
            System.out.println("*=====      rooms      =====*");
        }

        rooms.forEach((roomId, roomInformation) -> {

            final Set<Integer> roomServers = serverDatabase.getServerByRoom(roomId);
            final LinkedList<Message> roomMessages = serverDatabase.getMessagesByRoom(roomId);

            if (roomServers != null) {
                roomInformation.setServers(roomServers);
            }

            if (roomMessages != null) {
                roomInformation.insertMessages(roomMessages);
            }

            if (getLogger().debugEnabled()) {
                System.out.println("[" + roomId + "] " + roomInformation);
            }
        });

        //--------------------------------------------------------------------------
        // 5) Inicializar cliente TCP/SSL para receber comandos do servidor primário
        //--------------------------------------------------------------------------

        final KryoClient myClient = new KryoClient();

        myClientListener = new SecondaryClientListener(this, myClient);
        TcpNetwork.register(myClient);
        myClient.addListener(myClientListener);
        myClient.start();
        myClient.connect(ChatupGlobals.DefaultTimeout, paramPrimary.getAddress(), paramPrimary.getPort());

        //--------------------------------------------------------------------------------
        // 6) Inicializar servidor TCP/SSL para receber pedidos dos servidores secundários
        //--------------------------------------------------------------------------------

        final KryoServer myServer = new KryoServer() {

            @Override
            protected Connection newConnection() {
                return new ServerConnection();
            }
        };

        myServerListener = new SecondaryServerListener(this, myServer);
        TcpNetwork.register(myServer);
        myServer.addListener(myServerListener);
        myServer.bind(tcpPort);
        myServer.start();
	}

	private int serverId;
    private int roomSequence;
    private long serverTimestamp;

    @Override
    public int getId() {
        return serverId;
    }

    public ServerLogger getLogger() {
        return serverLogger;
    }

    public void registerUser(final String userToken, final String userEmail) {

        final String userRecord = users.get(userToken);

        if (userRecord == null) {
            users.put(userToken, userEmail);
        }
    }

    @Override
    public ServerResponse createRoom(final String roomName, final String roomPassword, final String roomOwner) {

        int roomId = ++roomSequence;

        System.out.println("------ CreateRoom ------");
        System.out.println("roomId:" + roomId);
        System.out.println("roomName:" + roomName);
        System.out.println("roomPassword:" + roomPassword);
        System.out.println("roomOwner:" + roomOwner);

        if (rooms.containsKey(roomId)) {
            return ServerResponse.RoomNotFound;
        }

        final Room newRoom = new Room(roomName, roomPassword, roomOwner);

        if (serverDatabase.insertRoom(roomId, newRoom)) {
            rooms.put(1,newRoom );
        }
        else {
            return ServerResponse.DatabaseError;
        }

        return ServerResponse.SuccessResponse;
	}

    @Override
	public Pair<ServerResponse, ServerInfo> joinRoom(int roomId, final String userEmail, final String userToken) {

		final String userRecord = users.get(userToken);

		if (userRecord == null) {
			users.put(userToken, userEmail);
		}

		final Room selectedRoom = rooms.get(roomId);

		if (selectedRoom == null) {
			return new Pair<>(ServerResponse.RoomNotFound, null);
		}

		if (selectedRoom.hasUser(userToken)) {
            return new Pair<>(ServerResponse.AlreadyJoined, null);
        }

		selectedRoom.registerUser(userToken);

		return new Pair<>(ServerResponse.SuccessResponse, null);
	}

    @Override
    public ServerResponse leaveRoom(int roomId, final String userToken) {

        final String userRecord = users.get(userToken);

        if (userRecord == null) {
            return ServerResponse.InvalidToken;
        }

        final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        selectedRoom.removeUser(userToken);

        if (cascadeUser(userToken)) {
            getLogger().removeUser(userToken);
        }

        return ServerResponse.SuccessResponse;
    }

    private boolean cascadeUser(final String userToken) {

        boolean userRemoved = true;

        for (final Room currentRoom : rooms.values()) {

            if (currentRoom.hasUser(userToken)) {
                userRemoved = false;
            }
        }

        if (userRemoved) {
            users.remove(userToken);
        }

        return userRemoved;
    }

    private void cascadeRoom(final Set<String> roomUsers) {

        for (final String userToken : roomUsers) {

            if (cascadeUser(userToken)) {
                getLogger().removeUser(userToken);
            }
        }
    }

    @Override
    public ServerResponse deleteRoom(int roomId) {

        System.out.println("------ DeleteRoom ------");
        System.out.println("roomId:" + roomId);
        System.out.println("------------------------");

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

        cascadeRoom(roomUsers);

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

        if (selectedRoom.insertMessage(paramMessage)) {
            return ServerResponse.SuccessResponse;
        }

        return ServerResponse.OperationFailed;
    }

    @Override
    public boolean validateToken(final String userToken) {
        return users.containsKey(userToken);
    }

    public ServerResponse notifyMessage(final Message paramMessage) {

        final String userToken = paramMessage.getAuthor();
        final String userRecord = users.get(userToken);

        if (userRecord == null) {
            return ServerResponse.InvalidToken;
        }

        int roomId = paramMessage.getId();
        final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        final Set<Integer> roomServers = selectedRoom.getServers();

        for (final Integer currentRoom : roomServers) {
            myServerListener.sendServer(currentRoom, paramMessage);
        }

        return ServerResponse.SuccessResponse;
    }

	public JsonValue getMessages(final String userToken, int roomId) {

        final JsonValue jsonArray = Json.array();
        final String userRecord = users.get(userToken);

        if (userRecord == null) {
            getLogger().userNotFound(userToken);
            return jsonArray;
        }

		final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            getLogger().roomNotFound(roomId);
            return jsonArray;
        }

		/*if (!selectedRoom.hasUser(userToken)) {
			return jsonArray;
		}*/

        final Message[] myMessages = selectedRoom.getMessages();

        for (final Message currentMessage : myMessages) {
            jsonArray.asArray().add(Json.object()
                .add(HttpFields.MessageSender, currentMessage.getAuthor())
                .add(HttpFields.MessageTimestamp, currentMessage.getTimestamp())
                .add(HttpFields.MessageContents, currentMessage.getMessage())
                .add(HttpFields.MessageRoomId, currentMessage.getId()));
        }

        return jsonArray;
	}

	@Override
	public ServerResponse insertServer(final ServerInfo serverInfo) {

        System.out.println("------ InsertServer ------");
        System.out.println("serverId:" + serverInfo.getId());
        System.out.println("serverAddress:" + serverInfo.getAddress());
        System.out.println("serverPort:" + serverInfo.getPort());
        System.out.println("--------------------------");

		if (servers.containsKey(serverId)) {
			return updateServer(serverInfo);
		}

		if (serverDatabase.insertServer(serverInfo)) {
            servers.put(serverId, serverInfo);
        }
        else {
            return ServerResponse.DatabaseError;
        }

        serverTimestamp = Instant.now().getEpochSecond();

		return ServerResponse.SuccessResponse;
	}

	@Override
	public ServerResponse updateServer(final ServerInfo serverInfo) {

        System.out.println("------ UpdateServer ------");
        System.out.println("serverId:" + serverInfo.getId());
        System.out.println("serverAddress:" + serverInfo.getAddress());
        System.out.println("serverPort:" + serverInfo.getPort());
        System.out.println("--------------------------");

        final ServerInfo selectedServer = servers.get(serverId);

		if (selectedServer == null) {
			return ServerResponse.ServerNotFound;
		}

        if (serverDatabase.updateServer(serverInfo)) {
            selectedServer.setAddress(serverInfo.getAddress());
            selectedServer.setPort(serverInfo.getPort());
            selectedServer.setTimestamp(serverInfo.getTimestamp());
        }
        else {
            return ServerResponse.DatabaseError;
        }

        serverTimestamp = Instant.now().getEpochSecond();

		return ServerResponse.SuccessResponse;
	}

	@Override
	public ServerResponse deleteServer(int serverId) {

        System.out.println("------ DeleteServer ------");
        System.out.println("serverId:" + serverId);
        System.out.println("--------------------------");

        if (servers.get(serverId) == null) {
            return ServerResponse.ServerNotFound;
        }

        if (serverDatabase.deleteServer(serverId)) {
            servers.remove(serverId);
        }
        else {
            return ServerResponse.DatabaseError;
        }

        serverTimestamp = Instant.now().getEpochSecond();

        rooms.forEach((roomId, room) -> {
            room.removeServer(serverId);
        });

        return ServerResponse.SuccessResponse;
	}

	@Override
	public ServerResponse userDisconnect(final String userEmail, final String userToken) {

		final String userRecord = users.get(userToken);

		if (userRecord == null) {
            return ServerResponse.InvalidToken;
		}

        if (userRecord.equals(userEmail)) {
            users.remove(userToken);
        }
        else {
            return ServerResponse.InvalidToken;
        }

        rooms.forEach((roomId, room) -> {
            room.removeUser(userToken);
        });

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

        myServerListener.sendServer(serverId, selectedRoom);

        return ServerResponse.SuccessResponse;
    }

    public ServerResponse updateRoom(final SyncRoomResponse updateRoom) {

        System.out.println("------ UpdateRoom ------");
        System.out.println("roomId:" + updateRoom.roomId);
        System.out.println("roomTimestamp:" + updateRoom.roomTimestamp);
        System.out.println("#roomServers:" + updateRoom.roomServers.size());
        System.out.println("#roomUsers:" + updateRoom.roomUsers.size());
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

    public ServerResponse syncRoom(int roomId, final Message[] messageCache) {

        System.out.println("------ SyncRoom ------");
        System.out.println("roomId:" + roomId);
        System.out.println("#messages:" + messageCache.length);
        System.out.println("----------------------");

        if (roomId < 0) {
            return ServerResponse.MissingParameters;
        }

        final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        selectedRoom.syncMessages(messageCache);

        return ServerResponse.SuccessResponse;
    }

    public ServerOnline getInformation() {
        return new ServerOnline(serverId, serverTimestamp, serverPort);
    }
}