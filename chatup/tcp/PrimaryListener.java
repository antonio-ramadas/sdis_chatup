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

        if (serverConnection.serverId <= 0) {
            return;
        }

        if (paramObject instanceof ServerOnline) {

            final ServerInfo serverInfo;
            final ServerOnline serverOnline = (ServerOnline) paramObject;

            if (serverOnline.serverAddress == null || serverOnline.serverPort < 1) {

                final InetSocketAddress serverSocket = serverConnection.getRemoteAddressTCP();

                serverInfo = new ServerInfo(
                    serverOnline.serverId,
                    serverOnline.serverTimestamp,
                    serverSocket.getHostName(),
                    serverSocket.getPort()
                );
            }
            else {
                serverInfo = new ServerInfo(
                    serverOnline.serverId,
                    serverOnline.serverTimestamp,
                    serverOnline.serverAddress,
                    serverOnline.serverPort
                );
            }

            int serverId = serverOnline.serverId;

            serverConnection.serverId = serverId;
            kryoServer.sendToAllExceptTCP(serverConnection.getId(), serverOnline);
            myConnections.put(serverId, serverConnection.getId());

            final ServerResponse operationResult = primaryServer.insertServer(serverInfo);

            switch (operationResult) {
            case SuccessResponse:
                primaryServer.getLogger().serverOnline(serverId, serverInfo.getAddress());
                break;
            case ServerNotFound:
                primaryServer.getLogger().serverNotFound(serverId);
                break;
            default:
                primaryServer.getLogger().invalidCommand("ServerOnline");
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