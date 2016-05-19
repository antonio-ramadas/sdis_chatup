package chatup.server;

import chatup.http.SecondaryDispatcher;
import chatup.http.ServerResponse;
import chatup.model.MessageCache;
import chatup.model.Room;
import chatup.model.Message;
import chatup.tcp.SecondaryListener;
import chatup.tcp.TcpNetwork;
import kryonet.KryoClient;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.SQLException;
import java.time.Instant;

public class SecondaryServer extends Server {

	private final ServerInfo myPrimary;
	private final KryoClient myClient;
	private final SecondaryListener myClientListener;

	public SecondaryServer(int paramId, final ServerInfo paramPrimary, int tcpPort, int httpPort) throws SQLException {

		super(new SecondaryDispatcher(), httpPort);

		myPrimary = paramPrimary;
		serverId = paramId;
		myClient = new KryoClient();
		myClientListener = new SecondaryListener(this, myClient);
		TcpNetwork.register(myClient);

		try {
			myClient.start();
			myClient.addListener(myClientListener);
			myClient.connect(5, InetAddress.getByName(paramPrimary.getAddress()), paramPrimary.getPort());
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private int serverId;

    public ServerResponse registerMessage(int roomId, final Message paramMessage) {

        final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        if (!selectedRoom.hasUser(paramMessage.getMessage())) {
            return ServerResponse.InvalidToken;
        }

        selectedRoom.insertMessage(paramMessage);

        return ServerResponse.SuccessResponse;
    }

	@Override
	public ServerResponse registerMessage(final String userToken, int roomId, final String messageBody) {

		final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        if (!selectedRoom.hasUser(userToken)) {
            return ServerResponse.InvalidToken;
		}

		selectedRoom.insertMessage(new Message(roomId, userToken, Instant.now().toEpochMilli(), messageBody));

		return ServerResponse.SuccessResponse;
	}

	@Override
	public Message[] retrieveMessages(final String userToken, int roomId) {

		if (!rooms.containsKey(roomId)) {
			return null;
		}

		final Room selectedRoom = rooms.get(roomId);

		if (!selectedRoom.hasUser(userToken)) {
			return null;
		}

		return selectedRoom.getMessages();
	}

	@Override
	public boolean insertServer(int serverId, final String newIp, int newPort) {
		return false;
	}

	@Override
	public boolean updateServer(int serverId, final String newIp, int newPort) {

		final ServerInfo selectedServer = servers.get(serverId);

		if (selectedServer == null) {
			servers.put(serverId, new ServerInfo(newIp, newPort));
		}
		else {
			selectedServer.setAddress(newIp);
			selectedServer.setPort(newPort);
		}

		return true;
	}

	@Override
	public boolean removeServer(int serverId) {

		final ServerInfo selectedServer = servers.get(serverId);

		if (selectedServer == null) {
			return false;
		}

		servers.remove(serverId);

		return true;
	}

	@Override
	public ServerResponse userDisconnect(final String userToken, final String userEmail) {

		System.out.println("email:" + userEmail);
		System.out.println("token:" + userToken);

		final String userRecord = users.get(userToken);

		if (userRecord == null || !userRecord.equals(userEmail)) {
			return ServerResponse.InvalidToken;
		}

		rooms.forEach((roomId, room) -> {

			if (room.hasUser(userToken)) {
				notifyLeaveRoom(roomId, userToken);
			}
		});

		users.remove(userToken);

		return ServerResponse.SuccessResponse;
	}

	@Override
	protected void notifyLeaveRoom(int roomId, final String userToken) {
		throw new UnsupportedOperationException("NotifyLeaveRoom");
	}

	@Override
	public ServerType getType() {
		return ServerType.SECONDARY;
	}

	public int getId() {
		return 1;
	}

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
}