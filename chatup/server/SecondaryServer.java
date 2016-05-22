package chatup.server;

import chatup.http.SecondaryDispatcher;
import chatup.http.ServerResponse;
import chatup.model.Message;
import chatup.model.MessageCache;
import chatup.model.Room;
import chatup.tcp.SecondaryListener;
import chatup.tcp.SendMessage;
import chatup.tcp.SyncRoom;
import chatup.tcp.TcpNetwork;

import com.esotericsoftware.minlog.Log;

import kryonet.Connection;
import kryonet.KryoClient;
import kryonet.KryoServer;
import kryonet.Listener;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.SQLException;
import java.time.Instant;

public class SecondaryServer extends Server {

	private final ServerInfo myPrimary;
	private final SecondaryListener myClientListener;

	public SecondaryServer(int paramId, final ServerInfo paramPrimary, int tcpPort, int httpPort) throws SQLException, IOException {

		super(new SecondaryDispatcher(), httpPort);

		myPrimary = paramPrimary;
		serverId = paramId;

        // COMUNICAÇÂO PRIMÁRIO <-> SECUNDÁRIO

        final KryoClient myClient = new KryoClient();

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

        // COMUNICAÇÂO SECUNDÁRIO <-> SECUNDÁRIO

        final KryoServer myServer = new KryoServer(){

            @Override
            protected Connection newConnection() {
                return new ServerConnection();
            }
        };

        myServer.addListener(new Listener() {

            @Override
            public void received(final Connection paramConnection, final Object paramObject) {

                if (paramObject instanceof SendMessage) {
                    insertMessage((SendMessage) paramObject);
                }
                else if (paramObject instanceof SyncRoom) {
                    syncRoom((SyncRoom) paramObject);
                }
            }
        });

        myServer.bind(tcpPort);
        myServer.start();
	}

	private int serverId;

    public int getId() {
        return serverId;
    }

    private void syncRoom(final SyncRoom syncRoom) {

        final ServerResponse operationResult = syncRoomAux
        (
            syncRoom.roomId,
            syncRoom.messageCache
        );

        switch (operationResult) {
        case SuccessResponse:
            Log.info("primary", "Received message block for Room #" + syncRoom.roomId + ".");
            break;
        case RoomNotFound:
            Log.error("primary", "Room #" + syncRoom.roomId + " is not registered on this server!");
            break;
        default:
            Log.error("primary", "Received empty or invalid request!");
            break;
        }
    }

    private void insertMessage(final SendMessage sendMessage) {

        final ServerResponse operationResult = registerMessage
        (
            sendMessage.roomId,
            sendMessage.message
        );

        switch (operationResult) {
        case SuccessResponse:
            Log.info("primary", "Received message block for Room #" + sendMessage.roomId + ".");
            break;
        case InvalidToken:
            Log.error("primary", "User " + sendMessage.message.getSender() + " is not inside Room #" + sendMessage.roomId + "!");
            break;
        case RoomNotFound:
            Log.error("primary", "Room #" + sendMessage.roomId + " is not registered on this server!");
            break;
        default:
            Log.error("primary", "Received empty or invalid request!");
            break;
        }
    }

    private ServerResponse registerMessage(int roomId, final Message paramMessage) {

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

    @Override
    public ServerResponse deleteRoom(int roomId) {
        return null;
    }

    @Override
    public ServerResponse deleteServer(int serverId) {
        return null;
    }

    private ServerResponse syncRoomAux(int roomId, final MessageCache messageCache) {

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

    public ServerResponse createRoom(final String roomName, final String roomPassword, final String roomOwner) {
        return ServerResponse.OperationFailed;
    }
}