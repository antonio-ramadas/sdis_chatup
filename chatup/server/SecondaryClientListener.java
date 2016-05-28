package chatup.server;

import chatup.model.Message;

import chatup.tcp.UpdateRoom;

import kryonet.Connection;
import kryonet.KryoClient;
import kryonet.Listener;

class SecondaryClientListener extends Listener {

    SecondaryClientListener(final SecondaryServer paramSecondary, final KryoClient paramClient) {
        mKryoClient = paramClient;
        mLogger = paramSecondary.getLogger();
        mSecondary = paramSecondary;
    }

    private final KryoClient mKryoClient;
    private final ServerLogger mLogger;
    private final SecondaryServer mSecondary;

    @Override
    public void received(final Connection paramConnection, final Object paramObject) {

        final ServerConnection serverConnection = (ServerConnection) paramConnection;

        if (paramObject instanceof UpdateRoom) {
            updateRoom(serverConnection, (UpdateRoom)paramObject);
        }
        else if (paramObject instanceof Message){
            receiveMessage(serverConnection, (Message)paramObject);
        }
    }

    private void receiveMessage(final ServerConnection paramConnection, final Message paramMessage) {

        switch (mSecondary.insertMessage(paramMessage)) {
        case SuccessResponse:
            mLogger.insertMessage(paramMessage.getId(), paramConnection.serverId);
            break;
        case RoomNotFound:
            mLogger.roomNotFound(paramMessage.getId());
            break;
        case InvalidToken:
            mLogger.userNotFound(paramMessage.getAuthor());
            break;
        case DatabaseError:
            mLogger.databaseError();
            break;
        default:
            mLogger.invalidOperation(paramMessage);
            break;
        }
    }

    private void updateRoom(final ServerConnection paramConnection, final UpdateRoom updateRoom) {

        switch (mSecondary.updateRoom(updateRoom)) {
        case SuccessResponse:
            mLogger.updateRoom(updateRoom.roomId, paramConnection.serverId);
            break;
        case RoomNotFound:
            mLogger.roomNotFound(updateRoom.roomId);
            break;
        case DatabaseError:
            mLogger.databaseError();
            break;
        default:
            mLogger.invalidOperation(updateRoom);
            break;
        }
    }

    @Override
    public void connected(final Connection paramConnection) {
        mKryoClient.sendTCP(mSecondary.getInformation());
    }

    @Override
    public void disconnected(final Connection paramConnection) {

        final ServerConnection serverConnection = (ServerConnection) paramConnection;

        if (serverConnection.serverId > 0) {
            mSecondary.disconnectServer(serverConnection.serverId);
            mLogger.serverOffline(serverConnection.serverId);
        }
    }
}