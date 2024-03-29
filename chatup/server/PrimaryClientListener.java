package chatup.server;

import chatup.http.ServerResponse;
import chatup.main.ChatupGlobals;
import chatup.model.CommandQueue;
import chatup.tcp.*;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.KryoNetException;
import com.esotericsoftware.kryonet.Listener;

class PrimaryClientListener extends Listener {

    PrimaryClientListener(final SecondaryServer paramSecondary, final Client paramClient) {
        mKryoClient = paramClient;
        mLogger = paramSecondary.getLogger();
        mSecondary = paramSecondary;
    }

    private final Client mKryoClient;
    private final ServerLogger mLogger;
    private final SecondaryServer mSecondary;

    @Override
    public void connected(final Connection paramConnection) {

        try {
            mKryoClient.sendTCP(mSecondary.getInformation());
        }
        catch (final KryoNetException ex) {
            ChatupGlobals.abort(mSecondary.getType(), ex);
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

    private void createRoom(final CreateRoom createRoom) {

        switch (mSecondary.createRoom(createRoom)) {
        case SuccessResponse:
            mLogger.createRoom(createRoom.userEmail, createRoom.roomName);
            break;
        case RoomExists:
            mLogger.roomExists(createRoom.roomName);
            break;
        case DatabaseError:
            mLogger.databaseError();
            break;
        }
    }

    private void joinRoom(final JoinRoom joinRoom) {

        switch (mSecondary.joinRoom(joinRoom)) {
        case SuccessResponse:
            mLogger.joinRoom(joinRoom.userEmail, joinRoom.roomId);
            break;
        case RoomNotFound:
            mLogger.roomNotFound(joinRoom.roomId);
            break;
        case AlreadyJoined:
            mLogger.alreadyJoined(joinRoom.roomId, joinRoom.userEmail);
            break;
        }
    }

    private void leaveRoom(final LeaveRoom leaveRoom) {

        final String userEmail = mSecondary.getEmail(leaveRoom.userToken);

        switch (mSecondary.leaveRoom(leaveRoom.roomId, leaveRoom.userToken)) {
        case SuccessResponse:
            mLogger.leaveRoom(userEmail, leaveRoom.roomId);
            break;
        case InvalidToken:
            mLogger.roomInvalidToken(leaveRoom.roomId, userEmail);
            break;
        case RoomNotFound:
            mLogger.roomNotFound(leaveRoom.roomId);
            break;
        }
    }

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

    private void updateServer(final UpdateServer updateServer) {

        final ServerInfo serverInfo = new ServerInfo(
            updateServer.serverId,
            updateServer.serverTimestamp,
            updateServer.serverAddress,
            updateServer.tcpPort,
            updateServer.httpPort
        );

        final ServerResponse operationResult = mSecondary.updateServer(serverInfo);

        switch (operationResult) {
        case SuccessResponse:
            mLogger.serverOnline(updateServer.serverId, serverInfo.getAddress());
            break;
        case ServerNotFound:
            mLogger.serverNotFound(updateServer.serverId);
            break;
        case DatabaseError:
            mLogger.databaseError();
            break;
        }
    }

    private void userDisconnect(final UserDisconnect userDisconnect) {

        final ServerResponse operationResult = mSecondary.userDisconnect(
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
}