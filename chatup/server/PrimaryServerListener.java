package chatup.server;

import chatup.tcp.ServerOffline;
import chatup.tcp.ServerOnline;

import kryonet.Connection;
import kryonet.KryoServer;
import kryonet.Listener;

import java.util.HashMap;

class PrimaryServerListener extends Listener {

    private HashMap<Integer, Integer> mConnections;

    PrimaryServerListener(final PrimaryServer paramPrimary, final KryoServer paramServer) {
        mKryoServer = paramServer;
        mConnections = new HashMap<>();
        mLogger = paramPrimary.getLogger();
        mPrimary = paramPrimary;
    }

    private final KryoServer mKryoServer;
    private final ServerLogger mLogger;
    private final PrimaryServer mPrimary;

    @Override
    public void received(final Connection paramConnection, final Object paramObject) {

        final ServerConnection serverConnection = (ServerConnection) paramConnection;

        if (paramObject instanceof ServerOnline) {

            final ServerOnline serverOnline = (ServerOnline) paramObject;

            final ServerInfo serverInfo = new ServerInfo(
                serverOnline.serverId,
                serverOnline.serverTimestamp,
                paramConnection.getRemoteAddressTCP().getAddress().getHostAddress(),
                serverOnline.tcpPort,
                serverOnline.httpPort
            );

            serverConnection.serverId = serverOnline.serverId;
            mKryoServer.sendToAllExceptTCP(serverConnection.getId(), serverOnline);
            mConnections.put(serverOnline.serverId, serverConnection.getId());

            switch (mPrimary.insertServer(serverInfo)) {
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
    }

    @Override
    public void disconnected(final Connection paramConnection) {

        final ServerConnection serverConnection = (ServerConnection) paramConnection;

        if (serverConnection.serverId > 0) {
            mKryoServer.sendToAllExceptTCP(serverConnection.getId(), new ServerOffline(serverConnection.serverId));
            mConnections.remove(serverConnection.serverId);
            mPrimary.disconnectServer(serverConnection.serverId);
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