package chatup.tcp;

import chatup.http.ServerResponse;
import chatup.server.PrimaryServer;
import chatup.server.ServerConnection;

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

    private void leaveRoom(final ServerConnection serverConnection, final LeaveRoom leaveRoom) {

        if (leaveRoom.userToken != null && leaveRoom.roomId >= 0) {
            kryoServer.sendToAllExceptTCP(serverConnection.getId(), leaveRoom);
            primaryServer.getLogger().leaveRoom(leaveRoom.userToken, leaveRoom.roomId);
        }
    }

    private void serverOnline(final ServerConnection serverConnection, final ServerOnline serverOnline) {

        final InetSocketAddress serverSocket = serverConnection.getRemoteAddressTCP();
        int serverId = serverOnline.serverId;

        if (primaryServer.insertServer(serverId, serverSocket.getHostName(), serverSocket.getPort()) == ServerResponse.SuccessResponse) {
            serverConnection.serverId = serverId;
            kryoServer.sendToAllExceptTCP(serverConnection.getId(), serverOnline);
            myConnections.put(serverId, serverConnection.getId());
            primaryServer.getLogger().serverOnline(serverId, serverSocket.getHostName());
            primaryServer.getLogger().insertServer(serverId);
        }
        else {

            final String serverAddress = serverOnline.serverAddress;
            int serverPort = serverOnline.serverPort;

            if (primaryServer.updateServer(serverId, serverAddress, serverPort) == ServerResponse.SuccessResponse) {
                serverConnection.serverId = serverId;
                kryoServer.sendToAllExceptTCP(serverConnection.getId(), serverOnline);
                myConnections.put(serverId, serverConnection.getId());
                primaryServer.getLogger().serverOnline(serverId, serverAddress);
            }
            else {
                primaryServer.getLogger().serverNotFound(serverId);
            }
        }
    }

    @Override
    public void received(final Connection paramConnection, final Object paramObject) {

        final ServerConnection serverConnection = (ServerConnection) paramConnection;

        if (serverConnection.serverId <= 0) {
            return;
        }

        if (paramObject instanceof LeaveRoom) {
            leaveRoom(serverConnection, (LeaveRoom)paramObject);
        }
        else if (paramObject instanceof ServerOnline) {
            serverOnline(serverConnection, (ServerOnline)paramObject);
        }
    }

    @Override
    public void disconnected(final Connection paramConnection) {

        final ServerConnection serverConnection = (ServerConnection) paramConnection;

        if (serverConnection.serverId > 0) {
            kryoServer.sendToAllExceptTCP(serverConnection.getId(), new ServerOffline(serverConnection.serverId));
            myConnections.remove(serverConnection.serverId);
            primaryServer.getLogger().serverOffline(serverConnection.serverId);
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