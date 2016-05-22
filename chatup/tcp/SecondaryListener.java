package chatup.tcp;

import chatup.http.ServerResponse;
import chatup.server.SecondaryServer;

import com.esotericsoftware.minlog.Log;

import kryonet.Connection;
import kryonet.KryoClient;
import kryonet.Listener;

import java.util.HashMap;

public class SecondaryListener extends Listener {

    private HashMap<Integer, Integer> serverConnections;

    public SecondaryListener(final SecondaryServer paramSecondary, final KryoClient paramClient) {
        kryoClient = paramClient;
        serverConnections = new HashMap<>();
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
            Log.error("primary", "Received empty or invalid request!");
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
            Log.error("primary", "Received empty or invalid request!");
            break;
        }
    }

    private void deleteRoom(final DeleteRoom deleteRoom) {

        final ServerResponse operationResult = secondaryServer.deleteRoom(deleteRoom.roomId);

        switch (operationResult) {
        case SuccessResponse:
            Log.info("secondary", "Room #" + deleteRoom.roomId + " has been deleted!");
            break;
        case RoomNotFound:
            Log.error("secondary", "Room #" + deleteRoom.roomId + " is not registered on this server!");
            break;
        default:
            Log.error("primary", "Received empty or invalid request!");
            break;
        }
    }

    private void deleteServer(final DeleteServer deleteServer) {

        final ServerResponse operationResult = secondaryServer.deleteServer(deleteServer.serverId);

        switch (operationResult) {
        case SuccessResponse:
            Log.info("secondary", "Server #" + deleteServer.serverId + " has been deleted!");
            break;
        case ServerNotFound:
            Log.error("secondary", "Server #" + deleteServer.serverId + " is not registered on this server!");
            break;
        default:
            Log.error("primary", "Received empty or invalid request!");
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
            Log.error("primary", "Received empty or invalid request!");
            break;
        }
    }

    @Override
    public void received(final Connection paramConnection, final Object paramObject) {

        if (paramObject instanceof JoinRoom) {
            joinRoom((JoinRoom)paramObject);
        }

        else if (paramObject instanceof LeaveRoom) {
            leaveRoom((LeaveRoom)paramObject);
        }
        else if (paramObject instanceof DeleteRoom) {
            deleteRoom((DeleteRoom)paramObject);
        }
        else if (paramObject instanceof DeleteServer) {
            deleteServer((DeleteServer)paramObject);
        }
        else if (paramObject instanceof CreateRoom) {
            createRoom((CreateRoom)paramObject);
        }
    }

    @Override
    public void connected(final Connection paramConnection) {
        kryoClient.sendTCP(new ServerOnline(secondaryServer.getId()));
    }
}