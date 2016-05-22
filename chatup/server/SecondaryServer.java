package chatup.server;

import chatup.http.SecondaryDispatcher;
import chatup.http.ServerResponse;
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
import java.util.Set;

public class SecondaryServer extends Server {

    private final SecondaryServerListener myServerListener;
	private final SecondaryClientListener myClientListener;

	public SecondaryServer(int paramId, final ServerInfo paramPrimary, int tcpPort, int httpPort) throws SQLException, IOException {

		super(new SecondaryDispatcher(), ServerType.SECONDARY, httpPort);

        /*
         * COMUNICAÇÃO PRIMÁRIO <=> SECUNDÁRIO
         */

        final KryoClient myClient = new KryoClient();

        myClientListener = new SecondaryClientListener(this, myClient);
        serverId = paramId;
		TcpNetwork.register(myClient);
        myClient.addListener(myClientListener);
        myClient.start();
        myClient.connect(5, InetAddress.getByName(paramPrimary.getAddress()), paramPrimary.getPort());

        /*
         * COMUNICAÇÃO SECUNDÁRIO <=> SECUNDÁRIO
         */

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

    @Override
    public ServerResponse createRoom(final String roomName, final String roomPassword, final String roomOwner) {

        rooms.put(1, new Room(roomName, roomPassword, roomOwner));

        return ServerResponse.SuccessResponse;
	}

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

        if (removeUser(userToken)) {
            getLogger().removeUser(userToken);
        }

        return ServerResponse.SuccessResponse;
    }

    private boolean removeUser(final String userToken) {

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

        for (final String userToken : roomUsers) {

            if (removeUser(userToken)) {
                getLogger().removeUser(userToken);
            }
        }

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

		servers.put(serverId, new ServerInfo(serverAddress, serverPort));

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

        rooms.forEach((roomId, room) -> {
            room.removeMirror(serverId);
        });

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

		rooms.forEach((roomId, room) -> {
			room.removeUser(userToken);
		});

		users.remove(userToken);

		return ServerResponse.SuccessResponse;
	}

    @Override
    public ServerResponse syncRoom(int roomId) {

        System.out.println("roomId:" + roomId);

        final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        final MessageCache syncMessages = selectedRoom.getMessages();

        System.out.println("#messages:" + syncMessages.size());

        final SyncRoom syncRoom = new SyncRoom(roomId, syncMessages);
        final Set<Integer> roomServers = selectedRoom.getServers();

        if (roomServers == null || roomServers.isEmpty()) {
            return ServerResponse.RoomNotFound;
        }

        for (final Integer serverId : roomServers) {
            myServerListener.send(serverId, syncRoom);
        }

        return ServerResponse.SuccessResponse;
    }

    @Override
    public ServerResponse syncRoom(int roomId, final MessageCache messageCache) {

        System.out.println("------ SyncRoom ------");
        System.out.println("roomId:" + roomId);
        System.out.println("#messages:" + messageCache.size());
        System.out.println("----------------------");

        if (roomId < 0 || messageCache == null) {
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