package chatup.tcp;

import chatup.http.ServerResponse;
import chatup.server.PrimaryServer;
import chatup.server.ServerConnection;
import chatup.server.ServerInfo;

import kryonet.Connection;
import kryonet.KryoServer;
import kryonet.Listener;

import java.net.InetSocketAddress;
import java.util.HashMap;

public class PrimaryListener extends Listener {

    private HashMap<Integer, Integer> myConnections;

    public PrimaryListener(final PrimaryServer paramPrimary, final KryoServer paramServer) {
        kryoServer = paramServer;
        myConnections = new HashMap<>();
        primaryServer = paramPrimary;
    }

    private final KryoServer kryoServer;
    private final PrimaryServer primaryServer;

    @Override
    public void received(final Connection paramConnection, final Object paramObject) {

        final ServerConnection serverConnection = (ServerConnection) paramConnection;

        if (paramObject instanceof ServerOnline) {

            final ServerInfo serverInfo;
            final ServerOnline serverOnline = (ServerOnline) paramObject;
            final InetSocketAddress serverSocket = serverConnection.getRemoteAddressTCP();

            serverInfo = new ServerInfo(
                serverOnline.serverId,
                serverOnline.serverTimestamp,
                serverSocket.getHostName(),
                serverOnline.serverPort
            );

            serverConnection.serverId = serverOnline.serverId;
            kryoServer.sendToAllExceptTCP(serverConnection.getId(), serverOnline);
            myConnections.put(serverOnline.serverId, serverConnection.getId());

            final ServerResponse operationResult = primaryServer.insertServer(serverInfo);

            switch (operationResult) {
            case SuccessResponse:
                primaryServer.getLogger().serverOnline(serverOnline.serverId, serverInfo.getAddress());
                break;
            case ServerNotFound:
                primaryServer.getLogger().serverNotFound(serverOnline.serverId);
                break;
            case DatabaseError:
                primaryServer.getLogger().databaseError();
                break;
            default:
                primaryServer.getLogger().invalidOperation(serverOnline);
                break;
            }
        }
    }

    @Override
    public void disconnected(final Connection paramConnection) {

        final ServerConnection serverConnection = (ServerConnection) paramConnection;

        if (serverConnection.serverId > 0) {
            kryoServer.sendToAllExceptTCP(serverConnection.getId(), new ServerOffline(serverConnection.serverId));
            myConnections.remove(serverConnection.serverId);
            primaryServer.getLogger().serverOffline(serverConnection.serverId);
            primaryServer.disconnectServer(serverConnection.serverId);
        }
    }

    public void send(int serverId, final Object paramObject) {

        if (myConnections.containsKey(serverId)) {
            kryoServer.sendToTCP(myConnections.get(serverId), paramObject);
        }
        else {
            primaryServer.getLogger().serverNotFound(serverId);
        }
    }
}