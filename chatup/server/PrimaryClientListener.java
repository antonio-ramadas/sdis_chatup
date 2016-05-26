package chatup.server;

import chatup.http.ServerResponse;
import chatup.model.CommandQueue;
import chatup.tcp.*;

import kryonet.Connection;
import kryonet.KryoClient;
import kryonet.Listener;

class PrimaryClientListener extends Listener {

    PrimaryClientListener(final SecondaryServer paramSecondary, final KryoClient paramClient) {
        mKryoClient = paramClient;
        mLogger = paramSecondary.getLogger();
        mSecondary = paramSecondary;
    }

    private final KryoClient mKryoClient;
    private final ServerLogger mLogger;
    private final SecondaryServer mSecondary;

    // TODO: Verified!
    private void createRoom(final CreateRoom createRoom) {

        switch (mSecondary.createRoom(createRoom)) {
        case SuccessResponse:
            mLogger.createRoom(createRoom.userToken, createRoom.roomName);
            break;
        case RoomExists:
            mLogger.roomExists(createRoom.roomName);
            break;
        case DatabaseError:
            mLogger.databaseError();
            break;
        }
    }

    // TODO: Verified!
    private void joinRoom(final JoinRoom joinRoom) {

        switch (mSecondary.joinRoom(joinRoom)) {
        case SuccessResponse:
            mLogger.joinRoom(joinRoom.userToken, joinRoom.roomId);
            break;
        case RoomNotFound:
            mLogger.roomNotFound(joinRoom.roomId);
            break;
        case AlreadyJoined:
            mLogger.alreadyJoined(joinRoom.roomId, joinRoom.userToken);
            break;
        }
    }

    // TODO: Verified!
    private void leaveRoom(final LeaveRoom leaveRoom) {

        final ServerResponse operationResult = mSecondary.leaveRoom
        (
            leaveRoom.roomId,
            leaveRoom.userToken
        );

        switch (operationResult) {
        case SuccessResponse:
            mLogger.leaveRoom(leaveRoom.userToken, leaveRoom.roomId);
            break;
        case InvalidToken:
            mLogger.roomInvalidToken(leaveRoom.roomId, leaveRoom.userToken);
            break;
        case RoomNotFound:
            mLogger.roomNotFound(leaveRoom.roomId);
            break;
        }
    }

    // TODO: Verified!
    private void deleteRoom(final DeleteRoom deleteRoom) {

        switch (mSecondary.deleteRoom(deleteRoom.roomId)) {
        case SuccessResponse:
            mLogger.deleteRoom(deleteRoom.roomId);
            break;
        case RoomNotFound:
            mLogger.roomNotFound(deleteRoom.roomId);
            break;
        case DatabaseError:
            mLogger.databaseError();
            break;
        }
    }

    // TODO: Verified!
    private void deleteServer(final DeleteServer deleteServer) {

        switch (mSecondary.deleteServer(deleteServer.serverId)) {
        case SuccessResponse:
            mLogger.deleteServer(deleteServer.serverId);
            break;
        case ServerNotFound:
            mLogger.serverNotFound(deleteServer.serverId);
            break;
        case DatabaseError:
            mLogger.databaseError();
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
        else if (paramObject instanceof UpdateServer) {
            updateServer((UpdateServer)paramObject);
        }
    }

    // TODO: Verified!
    private void updateServer(final UpdateServer serverOnline) {

        final ServerInfo serverInfo = new ServerInfo
        (
            serverOnline.serverId,
            serverOnline.serverTimestamp,
            serverOnline.serverAddress,
            serverOnline.serverPort
        );

        final ServerResponse operationResult = mSecondary.updateServer(serverInfo);

        switch (operationResult) {
        case SuccessResponse:
            mLogger.serverOnline(serverOnline.serverId, serverInfo.getAddress());
            break;
        case ServerNotFound:
            mLogger.serverNotFound(serverOnline.serverId);
            break;
        case DatabaseError:
            mLogger.databaseError();
            break;
        }
    }

    private void syncServer(final CommandQueue messageQueue) {

        while (!messageQueue.empty()) {

            final Object paramObject = messageQueue.pop();

            if (paramObject instanceof DeleteRoom) {
                deleteRoom((DeleteRoom) paramObject);
            }
            else if (paramObject instanceof UpdateServer) {
                updateServer((UpdateServer)paramObject);
            }
            else if (paramObject instanceof DeleteServer) {
                deleteServer((DeleteServer) paramObject);
            }
        }
    }

    // TODO: Verified!
    private void userDisconnect(final UserDisconnect userDisconnect) {

        final ServerResponse operationResult = mSecondary.userDisconnect
        (
            userDisconnect.userEmail,
            userDisconnect.userToken
        );

        switch (operationResult) {
        case SuccessResponse:
            mLogger.userDisconnected(userDisconnect.userToken);
            break;
        case InvalidToken:
            mLogger.userNotFound(userDisconnect.userToken);
            break;
        }
    }

    @Override
    public void connected(final Connection paramConnection) {
        mKryoClient.sendTCP(mSecondary.getInformation());
    }
}