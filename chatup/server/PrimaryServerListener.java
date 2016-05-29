package chatup.server;

import chatup.main.ChatupGlobals;
import chatup.tcp.ServerOffline;
import chatup.tcp.ServerOnline;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.KryoNetException;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.io.IOException;
import java.util.HashMap;

class PrimaryServerListener extends Listener {

    private HashMap<Integer, Integer> mConnections;

    PrimaryServerListener(final PrimaryServer paramPrimary, final Server paramServer) {
        mKryoServer = paramServer;
        mConnections = new HashMap<>();
        mLogger = paramPrimary.getLogger();
        mPrimary = paramPrimary;
    }

    private final Server mKryoServer;
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
            mKryoServer.sendToAllExceptTCP(serverConnection.getID(), serverOnline);
            mConnections.put(serverOnline.serverId, serverConnection.getID());

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
            mKryoServer.sendToAllExceptTCP(serverConnection.getID(), new ServerOffline(serverConnection.serverId));
            mConnections.remove(serverConnection.serverId);
            mPrimary.disconnectServer(serverConnection.serverId);
        }
    }

    void sendServer(int serverId, final Object paramObject) {

        if (mConnections.containsKey(serverId)) {

            try {
                mKryoServer.sendToTCP(mConnections.get(serverId), paramObject);
            }
            catch (final KryoNetException ex) {
                ChatupGlobals.abort(mPrimary.getType(), ex);
            }
        }
        else {
            mLogger.serverNotFound(serverId);
        }
    }
}