package chatup.server;

import chatup.http.ServerResponse;
import chatup.tcp.ServerOnline;
import chatup.tcp.SyncRoom;

import kryonet.Connection;
import kryonet.KryoServer;
import kryonet.Listener;

import java.util.HashMap;

class SecondaryServerListener extends Listener {

    private HashMap<Integer, Integer> mConnections;

    SecondaryServerListener(final SecondaryServer paramSecondary, final KryoServer paramClient) {
        mKryoServer = paramClient;
        mConnections = new HashMap<>();
        mLogger = paramSecondary.getLogger();
        mSecondary = paramSecondary;
    }

    private final KryoServer mKryoServer;
    private final ServerLogger mLogger;
    private final SecondaryServer mSecondary;

    @Override
    public void received(final Connection paramConnection, final Object paramObject) {

        final ServerConnection serverConnection = (ServerConnection) paramConnection;

        if (paramObject instanceof ServerOnline) {
            serverOnline(serverConnection, (ServerOnline) paramObject);
        }
        else if (paramObject instanceof SyncRoom) {
            sendRoom(serverConnection, (SyncRoom) paramObject);
        }
    }

    @Override
    public void disconnected(final Connection paramConnection) {

        final ServerConnection serverConnection = (ServerConnection) paramConnection;

        if (serverConnection.serverId > 0) {
            mConnections.remove(serverConnection.serverId);
            mSecondary.disconnectServer(serverConnection.serverId);
            mLogger.serverOffline(serverConnection.serverId);
        }
    }

    private void sendRoom(final ServerConnection paramConnection, final SyncRoom syncRoom) {

        switch (mSecondary.syncRoom(syncRoom, paramConnection.serverId)) {
        case SuccessResponse:
            mLogger.syncRoom(syncRoom.roomId, paramConnection.serverId);
            break;
        case RoomNotFound:
            mLogger.roomNotFound(syncRoom.roomId);
            break;
        }
    }

    private void serverOnline(final ServerConnection paramConnection, final ServerOnline serverOnline) {

        final ServerInfo serverInfo = new ServerInfo(
            serverOnline.serverId,
            serverOnline.serverTimestamp,
            serverOnline.serverAddress,
            serverOnline.serverPort
        );

        paramConnection.serverId = serverOnline.serverId;
        mConnections.put(serverOnline.serverId, paramConnection.getId());

        final ServerResponse operationResult = mSecondary.insertServer(serverInfo);

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
        default:
            mLogger.invalidOperation(serverOnline);
            break;
        }
    }

    void sendServer(int serverId, final Object paramObject) {

        if (mConnections.containsKey(serverId)) {
            mKryoServer.sendToTCP(mConnections.get(serverId), paramObject);
        }
        else {
            mLogger.serverNotFound(serverId);
        }
    }
}