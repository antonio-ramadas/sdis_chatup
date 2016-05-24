package chatup.tcp;

import chatup.http.ServerResponse;
import chatup.model.CommandQueue;
import chatup.server.SecondaryServer;
import chatup.server.ServerInfo;

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

    // TODO: Verified!
    private void createRoom(final CreateRoom createRoom) {

        final ServerResponse operationResult = secondaryServer.createRoom
        (
            createRoom.roomName,
            createRoom.roomPassword,
            createRoom.userToken
        );

        switch (operationResult) {
        case SuccessResponse:
            secondaryServer.registerUser(createRoom.userToken, createRoom.userEmail);
            secondaryServer.getLogger().createRoom(createRoom.userToken, createRoom.roomName);
            break;
        case RoomExists:
            secondaryServer.getLogger().roomExists(createRoom.roomName);
            break;
        case DatabaseError:
            secondaryServer.getLogger().databaseError();
            break;
        }
    }

    // TODO: Verified!
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
        case AlreadyJoined:
            secondaryServer.getLogger().alreadyJoined(joinRoom.roomId, joinRoom.userToken);
            break;
        }
    }

    // TODO: Verified!
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
        }
    }

    // TODO: Verified!
    private void deleteRoom(final DeleteRoom deleteRoom) {

        final ServerResponse operationResult = secondaryServer.deleteRoom(deleteRoom.roomId);

        switch (operationResult) {
        case SuccessResponse:
            secondaryServer.getLogger().deleteRoom(deleteRoom.roomId);
            break;
        case RoomNotFound:
            secondaryServer.getLogger().roomNotFound(deleteRoom.roomId);
            break;
        case DatabaseError:
            secondaryServer.getLogger().databaseError();
            break;
        }
    }

    // TODO: Verified!
    private void deleteServer(final DeleteServer deleteServer) {

        final ServerResponse operationResult = secondaryServer.deleteServer(deleteServer.serverId);

        switch (operationResult) {
        case SuccessResponse:
            secondaryServer.getLogger().deleteServer(deleteServer.serverId);
            break;
        case ServerNotFound:
            secondaryServer.getLogger().serverNotFound(deleteServer.serverId);
            break;
        case DatabaseError:
            secondaryServer.getLogger().databaseError();
            break;
        }
    }

    @Override
    public void received(final Connection paramConnection, final Object paramObject) {

        secondaryServer.getLogger().invalidOperation(paramObject);

        if (paramObject instanceof JoinRoom) {
            joinRoom((JoinRoom)paramObject);
        }
        else if (paramObject instanceof LeaveRoom) {
            leaveRoom((LeaveRoom)paramObject);
        }
        else if (paramObject instanceof CommandQueue) {
            syncServer((CommandQueue)paramObject);
        }
        else if (paramObject instanceof UserDisconnect) {
            userDisconnect((UserDisconnect) paramObject);
        }
        else if (paramObject instanceof CreateRoom) {
            createRoom((CreateRoom)paramObject);
        }
        else if (paramObject instanceof DeleteRoom) {
            deleteRoom((DeleteRoom)paramObject);
        }
        else if (paramObject instanceof DeleteServer) {
            deleteServer((DeleteServer)paramObject);
        }
        else {
            secondaryServer.getLogger().invalidOperation(paramObject);
        }
    }

    // TODO: Verified!
    private void updateServer(final ServerOnline serverOnline) {

        final ServerInfo serverInfo = new ServerInfo(
            serverOnline.serverId,
            serverOnline.serverTimestamp,
            serverOnline.serverAddress,
            serverOnline.serverPort
        );

        final ServerResponse operationResult = secondaryServer.updateServer(serverInfo);

        switch (operationResult) {
        case SuccessResponse:
            secondaryServer.getLogger().serverOnline(serverOnline.serverId, serverInfo.getAddress());
            break;
        case ServerNotFound:
            secondaryServer.getLogger().serverNotFound(serverOnline.serverId);
            break;
        case DatabaseError:
            secondaryServer.getLogger().databaseError();
            break;
        }
    }

    private void syncServer(final CommandQueue messageQueue) {

        while (!messageQueue.empty()) {

            final Object paramObject = messageQueue.pop();

            if (paramObject instanceof DeleteRoom) {
                deleteRoom((DeleteRoom) paramObject);
            }
            else if (paramObject instanceof ServerOnline) {
                updateServer((ServerOnline)paramObject);
            }
            else if (paramObject instanceof DeleteServer) {
                deleteServer((DeleteServer) paramObject);
            }
        }
    }

    // TODO: Verified!
    private void userDisconnect(final UserDisconnect userDisconnect) {

        final ServerResponse operationResult = secondaryServer.userDisconnect
        (
            userDisconnect.userEmail,
            userDisconnect.userToken
        );

        switch (operationResult) {
        case SuccessResponse:
            secondaryServer.getLogger().userDisconnected(userDisconnect.userToken);
            break;
        case InvalidToken:
            secondaryServer.getLogger().userNotFound(userDisconnect.userToken);
            break;
        }
    }

    @Override
    public void connected(final Connection paramConnection) {
        kryoClient.sendTCP(secondaryServer.getInformation());
    }
}