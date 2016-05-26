package chatup.server;

import chatup.model.Message;

import chatup.tcp.SyncRoom;

import kryonet.Connection;
import kryonet.KryoServer;
import kryonet.Listener;

import java.util.HashMap;

class SecondaryServerListener extends Listener {

    private HashMap<Integer, Integer> myConnections;

    SecondaryServerListener(final SecondaryServer paramSecondary, final KryoServer paramClient) {
        mKryoServer = paramClient;
        myConnections = new HashMap<>();
        mLogger = paramSecondary.getLogger();
        mSecondary = paramSecondary;
    }

    private final KryoServer mKryoServer;
    private final ServerLogger mLogger;
    private final SecondaryServer mSecondary;

    @Override
    public void received(final Connection paramConnection, final Object paramObject) {

        final ServerConnection serverConnection = (ServerConnection) paramConnection;

        if (paramObject instanceof Message) {
            sendMessage((Message) paramObject);
        }
        else if (paramObject instanceof SyncRoom) {
            sendRoom(serverConnection, (SyncRoom) paramObject);
        }
    }

    @Override
    public void disconnected(final Connection paramConnection) {

        final ServerConnection serverConnection = (ServerConnection) paramConnection;

        if (serverConnection.serverId > 0) {
            myConnections.remove(serverConnection.serverId);
            mSecondary.disconnectServer(serverConnection.serverId);
            mLogger.serverOffline(serverConnection.serverId);
        }
    }

    private void sendRoom(final ServerConnection paramConnection, final SyncRoom syncRoom) {

        switch (mSecondary.syncRoom(syncRoom.roomId, paramConnection.serverId)) {
        case SuccessResponse:
            mLogger.syncRoom(syncRoom.roomId, paramConnection.serverId);
            break;
        case RoomNotFound:
            mLogger.roomNotFound(syncRoom.roomId);
            break;
        }
    }

    private void sendMessage(final Message paramMessage) {

        switch (mSecondary.insertMessage(paramMessage)) {
        case SuccessResponse:
            mLogger.sendMessage(paramMessage.getId());
            break;
        case InvalidToken:
            mLogger.roomInvalidToken(paramMessage.getId(), paramMessage.getAuthor());
            break;
        case RoomNotFound:
            mLogger.roomNotFound(paramMessage.getId());
            break;
        default:
            mLogger.invalidOperation("SendMessage");
            break;
        }
    }

    void sendServer(int serverId, final Object paramObject) {

        if (myConnections.containsKey(serverId)) {
            mKryoServer.sendToTCP(myConnections.get(serverId), paramObject);
        }
        else {
            mLogger.serverNotFound(serverId);
        }
    }
}