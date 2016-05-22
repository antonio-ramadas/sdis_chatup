package chatup.tcp;

import chatup.http.ServerResponse;
import chatup.server.SecondaryServer;

import kryonet.Connection;
import kryonet.KryoClient;
import kryonet.Listener;

public class SecondaryClientListener extends Listener {

    public SecondaryClientListener(final SecondaryServer paramSecondary, final KryoClient paramClient) {
        kryoClient = paramClient;
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
            secondaryServer.getLogger().createRoom(createRoom.roomOwner, createRoom.roomName);
            break;
        case OperationFailed:
            secondaryServer.getLogger().roomExists(createRoom.roomName);
            break;
        default:
            secondaryServer.getLogger().invalidCommand("CreateRoom");
            break;
        }
    }

    private void joinRoom(final JoinRoom joinRoom) {

        final ServerResponse operationResult = secondaryServer.joinRoom
        (
            joinRoom.roomId,
            joinRoom.userEmail,
            joinRoom.userToken
        );

        switch (operationResult) {
        case SuccessResponse:
            secondaryServer.getLogger().joinRoom(joinRoom.userToken, joinRoom.roomId);
            break;
        case RoomNotFound:
            secondaryServer.getLogger().roomNotFound(joinRoom.roomId);
            break;
        default:
            secondaryServer.getLogger().invalidCommand("JoinRoom");
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
            secondaryServer.getLogger().leaveRoom(leaveRoom.userToken, leaveRoom.roomId);
            break;
        case InvalidToken:
            secondaryServer.getLogger().roomInvalidToken(leaveRoom.roomId, leaveRoom.userToken);
            break;
        case RoomNotFound:
            secondaryServer.getLogger().roomNotFound(leaveRoom.roomId);
            break;
        default:
            secondaryServer.getLogger().invalidCommand("LeaveRoom");
            break;
        }
    }

    private void deleteRoom(final DeleteRoom deleteRoom) {

        final ServerResponse operationResult = secondaryServer.deleteRoom(deleteRoom.roomId);

        switch (operationResult) {
        case SuccessResponse:
            secondaryServer.getLogger().deleteRoom(deleteRoom.roomId);
            break;
        case RoomNotFound:
            secondaryServer.getLogger().roomNotFound(deleteRoom.roomId);
            break;
        default:
            secondaryServer.getLogger().invalidCommand("DeleteRoom");
            break;
        }
    }

    private void deleteServer(final DeleteServer deleteServer) {

        final ServerResponse operationResult = secondaryServer.deleteServer(deleteServer.serverId);

        switch (operationResult) {
        case SuccessResponse:
            secondaryServer.getLogger().deleteServer(deleteServer.serverId);
            break;
        case ServerNotFound:
            secondaryServer.getLogger().serverNotFound(deleteServer.serverId);
            break;
        default:
            secondaryServer.getLogger().invalidCommand("DeleteServer");
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