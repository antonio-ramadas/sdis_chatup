package chatup.tcp;

import chatup.http.ServerResponse;
import chatup.server.SecondaryServer;
import chatup.server.ServerConnection;

import com.esotericsoftware.minlog.Log;

import kryonet.Connection;
import kryonet.KryoClient;
import kryonet.Listener;

import java.util.HashMap;

public class SecondaryListener extends Listener {

    private HashMap<Integer, Integer> myConnections;

    public SecondaryListener(final SecondaryServer paramSecondary, final KryoClient paramClient) {
        kryoClient = paramClient;
        myConnections = new HashMap<>();
        secondaryServer = paramSecondary;
    }

    private final KryoClient kryoClient;
    private final SecondaryServer secondaryServer;

    private void createRoom(final CreateRoom createRoom) {

        final ServerResponse operationResult = secondaryServer.createRoom
        (
            createRoom.roomName,
            createRoom.roomPassword,
            createRoom.roomOwner
        );

        switch (operationResult) {
        case SuccessResponse:
            Log.info("primary", "User " + createRoom.roomOwner + " has joined Room #" + createRoom.roomName + ".");
            break;
        case OperationFailed:
            Log.error("primary", "Room #" + createRoom.roomName + " already exists!");
            break;
        default:
            Log.error("primary", "Received empty or invalid command!");
            break;
        }
    }

    private void joinRoom(final JoinRoom joinRoom) {

        final ServerResponse operationResult = secondaryServer.joinRoom
        (
            joinRoom.roomId,
            joinRoom.userToken
        );

        switch (operationResult) {
        case SuccessResponse:
            Log.info("primary", "User " + joinRoom.userToken + " has joined Room #" + joinRoom.roomId + ".");
            break;
        case RoomNotFound:
            Log.error("primary", "Room #" + joinRoom.roomId + " is not registered on this server!");
            break;
        default:
            Log.error("primary", "Received empty or invalid command!");
            break;
        }
    }

    private void leaveRoom(final LeaveRoom leaveRoom) {

        final ServerResponse operationResult = secondaryServer.leaveRoom
        (
            leaveRoom.roomId,
            leaveRoom.userToken
        );

        switch (operationResult) {
        case SuccessResponse:
            Log.info("primary", "User " + leaveRoom.userToken + " has left Room #" + leaveRoom.roomId + ".");
            break;
        case RoomNotFound:
            Log.error("primary", "Room #" + leaveRoom.roomId + " is not registered on this server!");
            break;
        default:
            Log.error("primary", "Received empty or invalid command!");
            break;
        }
    }

    private void syncRoom(final SyncRoom syncRoom) {

        final ServerResponse operationResult = secondaryServer.syncRoom
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
            Log.error("primary", "Received empty or invalid command!");
            break;
        }
    }

    private void sendMessage(final SendMessage sendMessage) {

        final ServerResponse operationResult = secondaryServer.registerMessage
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
            Log.error("primary", "Received empty or invalid command!");
            break;
        }
    }

    @Override
    public void received(Connection paramConnection, Object object) {

        if (object instanceof JoinRoom) {
            joinRoom((JoinRoom)object);
        }
        else if (object instanceof LeaveRoom) {
            leaveRoom((LeaveRoom)object);
        }
        else if (object instanceof SendMessage) {
            sendMessage((SendMessage)object);
        }
        else if (object instanceof SyncRoom) {
            syncRoom((SyncRoom)object);
        }
        else if (object instanceof CreateRoom) {
            createRoom((CreateRoom)object);
        }
    }

    @Override
    public void connected(final Connection paramConnection) {
        kryoClient.sendTCP(new ServerOnline(secondaryServer.getId()));
    }
}