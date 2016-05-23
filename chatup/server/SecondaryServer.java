package chatup.server;

import chatup.http.SecondaryDispatcher;
import chatup.http.ServerResponse;
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
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Set;

public class SecondaryServer extends Server {

    private final Database serverDatabase;
    private final ServerLogger serverLogger;
    private final SecondaryServerListener myServerListener;
	private final SecondaryClientListener myClientListener;
    private final HashMap<Integer, ServerInfo> servers;
    private final HashMap<Integer, Room> rooms;

	public SecondaryServer(final ServerInfo paramPrimary, int tcpPort, int httpPort) throws SQLException, IOException {

        //----------------------------------------------------------------
        // 1) Inicializar servidor HTTPS para receber pedidos dos clientes
        //----------------------------------------------------------------

		super(new SecondaryDispatcher(), ServerType.SECONDARY, httpPort);

        //--------------------------------------------------------------------
        // 2) Ler para memória informações dos servidores armazenadas em disco
        //--------------------------------------------------------------------

        serverId = paramPrimary.getId();
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

            if (roomServers != null) {
                roomInformation.setServers(roomServers);
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
        myClient.connect(ChatupGlobals.DefaultTimeout, InetAddress.getByName(paramPrimary.getAddress()), paramPrimary.getPort());

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

    @Override
    public int getId() {
        return serverId;
    }

    public ServerLogger getLogger() {
        return serverLogger;
    }

    @Override
    public ServerResponse createRoom(final String roomName, final String roomPassword, final String roomOwner) {

        rooms.put(1, new Room(roomName, roomPassword, roomOwner));

        return ServerResponse.SuccessResponse;
	}

    @Override
	public ServerResponse joinRoom(int roomId, final String userEmail, final String userToken) {

        System.out.println("roomId:" + roomId);
        System.out.println("userEmail:" + userEmail);
        System.out.println("userToken:" + userToken);

		final String userRecord = users.get(userToken);

		if (userRecord == null) {
			users.put(userToken, userEmail);
		}

		final Room selectedRoom = rooms.get(roomId);

		if (selectedRoom == null) {
			return ServerResponse.RoomNotFound;
		}

		if (selectedRoom.hasUser(userToken)) {
			return ServerResponse.OperationFailed;
		}

		selectedRoom.registerUser(userToken);

		return ServerResponse.SuccessResponse;
	}

    @Override
    public ServerResponse leaveRoom(int roomId, final String userToken) {

        System.out.println("roomId:" + roomId);
        System.out.println("userToken:" + userToken);

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

        rooms.remove(roomId);
        cascadeRoom(roomUsers);

        return ServerResponse.SuccessResponse;
    }

    @Override
    public ServerResponse insertMessage(final Message paramMessage) {

        System.out.println("------ RegisterMessage ------");
        System.out.println("roomId:"      + paramMessage.getId());
        System.out.println("userToken:"   + paramMessage.getSender());
        System.out.println("messageBody:" + paramMessage.getMessage());
        System.out.println("-----------------------------");

        final String userToken = paramMessage.getSender();
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
    public ServerResponse notifyMessage(int roomId, final String userToken, final String messageBody) {

        System.out.println("------ NotifyMessage ------");
        System.out.println("roomId:"      + roomId);
        System.out.println("userToken:"   + userToken);
        System.out.println("messageBody:" + messageBody);
        System.out.println("---------------------------");

        final String userRecord = users.get(userToken);

        if (userRecord == null) {
            return ServerResponse.InvalidToken;
        }

        final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        final Message userMessage = new Message(roomId, userToken, messageBody);
        final Set<Integer> roomServers = selectedRoom.getServers();

        for (final Integer currentRoom : roomServers) {
            System.out.println(currentRoom);
        }

        return ServerResponse.SuccessResponse;
    }

	@Override
	public MessageCache<Integer, Message> getMessages(final String userToken, int roomId) {

        System.out.println("------ RetrieveMessages ------");
        System.out.println("roomId:" + roomId);
        System.out.println("userToken:" + userToken);
        System.out.println("------------------------------");

        final String userRecord = users.get(userToken);

        if (userRecord == null) {
            return null;
        }

		final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            getLogger().roomNotFound(roomId);
            return null;
        }

		if (!selectedRoom.hasUser(userToken)) {
			return null;
		}

		return selectedRoom.getMessages();
	}

	@Override
	public ServerResponse insertServer(int serverId, final String serverAddress, int serverPort) {

        System.out.println("------ InsertServer ------");
        System.out.println("serverId:" + serverId);
        System.out.println("serverAddress:" + serverAddress);
        System.out.println("serverPort:" + serverPort);
        System.out.println("--------------------------");

		if (servers.containsKey(serverId)) {
			return ServerResponse.OperationFailed;
		}

		servers.put(serverId, new ServerInfo(serverId, serverAddress, serverPort));

		return ServerResponse.SuccessResponse;
	}

	@Override
	public ServerResponse updateServer(int serverId, final String serverAddress, int serverPort) {

        System.out.println("------ UpdateServer ------");
        System.out.println("serverId:" + serverId);
        System.out.println("serverAddress:" + serverAddress);
        System.out.println("serverPort:" + serverPort);
        System.out.println("--------------------------");

        final ServerInfo selectedServer = servers.get(serverId);

		if (selectedServer == null) {
			return ServerResponse.ServerNotFound;
		}

		selectedServer.setAddress(serverAddress);
		selectedServer.setPort(serverPort);

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

        servers.remove(serverId);
        rooms.forEach((roomId, room) -> room.removeMirror(serverId));

        return ServerResponse.SuccessResponse;
	}

	@Override
	public ServerResponse userDisconnect(final String userToken, final String userEmail) {

        System.out.println("------ UserDisconnect ------");
		System.out.println("userEmail:" + userEmail);
		System.out.println("userToken:" + userToken);
        System.out.println("----------------------------");

		final String userRecord = users.get(userToken);

		if (userRecord == null) {
            return ServerResponse.InvalidToken;
		}

        if (!userRecord.equals(userEmail)) {
            return ServerResponse.InvalidToken;
        }

		rooms.forEach((roomId, room) -> room.removeUser(userToken));
		users.remove(userToken);

		return ServerResponse.SuccessResponse;
	}

    @Override
    public ServerResponse syncRoom(int roomId, int serverId) {

        System.out.println("roomId:" + roomId);
        System.out.println("serverId:" + serverId);

        final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        myServerListener.sendServer(serverId, selectedRoom);

        return ServerResponse.SuccessResponse;
    }

    public ServerResponse updateRoom(final SyncRoomResponse updateRoom) {

        System.out.println("roomId:" + updateRoom.roomId);
        System.out.println("roomName:" + updateRoom.roomObject);

        final Room selectedRoom = rooms.get(updateRoom.roomId);

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        rooms.put(updateRoom.roomId, updateRoom.roomObject);

        return ServerResponse.SuccessResponse;
    }

    @Override
    public ServerResponse syncRoom(int roomId, final MessageCache messageCache) {

        System.out.println("------ SyncRoom ------");
        System.out.println("roomId:" + roomId);
        System.out.println("#messages:" + messageCache.size());
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
}