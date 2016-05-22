package chatup.server;

import chatup.http.SecondaryDispatcher;
import chatup.http.ServerResponse;
import chatup.model.MessageCache;
import chatup.model.Room;
import chatup.model.Message;
import chatup.tcp.*;

import com.esotericsoftware.minlog.Log;

import kryonet.Connection;
import kryonet.KryoClient;
import kryonet.KryoServer;


import java.io.IOException;
import java.net.InetAddress;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Set;

public class SecondaryServer extends Server {

	private final ServerInfo myPrimary;
    private final SecondaryServerListener myServerListener;
	private final SecondaryClientListener myClientListener;

	public SecondaryServer(int paramId, final ServerInfo paramPrimary, int tcpPort, int httpPort) throws SQLException, IOException {

		super(new SecondaryDispatcher(), httpPort);

		myPrimary = paramPrimary;
		serverId = paramId;

        /*
         * COMUNICAÇÃO PRIMÁRIO <=> SECUNDÁRIO
         */

        final KryoClient myClient = new KryoClient();

        myClientListener = new SecondaryClientListener(this, myClient);
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

    public ServerResponse createRoom(final String roomName, final String roomPassword, final String roomOwner) {

        rooms.put(1, new Room(roomName, roomPassword, roomOwner));

        return ServerResponse.SuccessResponse;
	}

    private void syncRoom(final SyncRoom syncRoom) {

        final ServerResponse operationResult = syncRoom
        (
            syncRoom.roomId,
            syncRoom.messageCache
        );

        switch (operationResult) {
        case SuccessResponse:
            Log.info("primary", "Received message block for Room #" + syncRoom.roomId + ".");
            break;
        case RoomNotFound:
            getLogger().roomNotFound(syncRoom.roomId);
            break;
        default:
            getLogger().invalidCommand("SyncRoom");
            break;
        }
    }

    private void insertMessage(final SendMessage sendMessage) {

        final ServerResponse operationResult = registerMessage
        (
            sendMessage.roomId,
            sendMessage.message
        );

        final Room selectedRoom = rooms.get(sendMessage.roomId);

        if (selectedRoom == null) {
            getLogger().roomNotFound(sendMessage.roomId);
        }
        else {

            if (selectedRoom.insertMessage(sendMessage.message)) {
                Log.info("primary", "Sending message block for Room #" + sendMessage.roomId + ".");
            }
            else {
                getLogger().invalidCommand("SendMessage");
            }
        }
    }

    @Override
	public ServerResponse joinRoom(int roomId, final String userToken) {

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

        if (!selectedRoom.hasUser(userToken)) {
            return ServerResponse.OperationFailed;
        }

        selectedRoom.removeUser(userToken);

        return ServerResponse.SuccessResponse;
    }

    @Override
    public ServerResponse deleteRoom(int roomId) {

        System.out.println("roomId:" + roomId);

        final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        rooms.remove(roomId);

        return ServerResponse.SuccessResponse;
    }

    @Override
    public ServerResponse registerMessage(int roomId, final Message paramMessage) {

        final String userToken = paramMessage.getSender();
        final String userRecord = users.get(userToken);

        System.out.println("roomId:" + roomId);
        System.out.println("userToken:" + userToken);
        System.out.println("messageBody:" + paramMessage.getMessage());

        if (roomId < 0 || paramMessage == null) {
            return ServerResponse.MissingParameters;
        }

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

        selectedRoom.insertMessage(paramMessage);

        return ServerResponse.SuccessResponse;
    }

    @Override
    public ServerResponse notifyMessage(int roomId, final String userToken, final String messageBody) {

        System.out.println("roomId:" + roomId);
        System.out.println("userToken:" + userToken);
        System.out.println("messageBody:" + messageBody);

        final String userRecord = users.get(userToken);

        if (userRecord == null) {
            return ServerResponse.InvalidToken;
        }

        final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            getLogger().roomNotFound(roomId);
            return ServerResponse.RoomNotFound;
        }

        final Message userMessage = new Message(roomId, userToken, Instant.now().toEpochMilli(), messageBody);
        final Set<Integer> roomServers = selectedRoom.getServers();

        for (final Integer currentRoom : roomServers) {
            System.out.println(currentRoom);
        }

        return ServerResponse.SuccessResponse;
    }

	@Override
	public Message[] retrieveMessages(final String userToken, int roomId) {

        System.out.println("roomId:" + roomId);
        System.out.println("userToken:" + userToken);

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

        System.out.println("serverId:" + serverId);
        System.out.println("serverAddress:" + serverAddress);
        System.out.println("serverPort:" + serverPort);

		if (servers.containsKey(serverId)) {
			return ServerResponse.OperationFailed;
		}

		servers.put(serverId, new ServerInfo(serverAddress, serverPort));

		return ServerResponse.SuccessResponse;
	}

	@Override
	public ServerResponse updateServer(int serverId, final String serverAddress, int serverPort) {

        System.out.println("serverId:" + serverId);
        System.out.println("serverAddress:" + serverAddress);
        System.out.println("serverPort:" + serverPort);

        final ServerInfo selectedServer = servers.get(serverId);

		if (selectedServer == null) {
			return ServerResponse.ServerNotFound;
		}

		selectedServer.setAddress(serverAddress);
		selectedServer.setPort(serverPort);

		return ServerResponse.SuccessResponse;
	}

	@Override
	public ServerResponse removeServer(int serverId) {

        System.out.println("serverId:" + serverId);

		if (servers.containsKey(serverId)) {
			servers.remove(serverId);
		}
		else {
			return ServerResponse.ServerNotFound;
		}

		return ServerResponse.SuccessResponse;
	}

	@Override
	public ServerResponse userDisconnect(final String userToken, final String userEmail) {

		System.out.println("userEmail:" + userEmail);
		System.out.println("userToken:" + userToken);

		final String userRecord = users.get(userToken);

		if (userRecord == null) {
            return ServerResponse.MissingParameters;
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
	public ServerType getType() {
		return ServerType.SECONDARY;
	}

    @Override
    public ServerResponse syncRoom(int roomId, final MessageCache messageCache) {

        System.out.println("roomId:" + roomId);
        System.out.println("#messages:" + messageCache.size());

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

    public ServerResponse deleteServer(int serverId) {
        return null;
    }
}