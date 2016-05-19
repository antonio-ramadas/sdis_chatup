package chatup.tcp;

import chatup.server.PrimaryServer;
import chatup.server.ServerConnection;

import com.esotericsoftware.minlog.Log;

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

        if (leaveRoom.userToken == null || leaveRoom.roomId < 0) {
            return;
        }

        kryoServer.sendToAllExceptTCP(serverConnection.getId(), new LeaveRoom(leaveRoom.roomId, leaveRoom.userToken));
        Log.info("primary", "User " + leaveRoom.userToken + " has left room " + leaveRoom.roomId);
    }

    private void serverOnline(final ServerConnection serverConnection, final ServerOnline serverOnline) {

        final InetSocketAddress serverSocket = serverConnection.getRemoteAddressTCP();
        final String serverAddress = serverSocket.getHostName();
        int serverPort = serverSocket.getPort();

        if (primaryServer.insertServer(serverOnline.serverId, serverAddress, serverPort)) {
            serverConnection.serverId = (serverOnline.serverId);
            kryoServer.sendToAllExceptTCP(serverConnection.getId(), new ServerOnline(serverOnline.serverId));
            myConnections.put(serverOnline.serverId, serverConnection.getId());
            Log.info("primary", "Server " + serverOnline.serverId + " connected from " + serverSocket.getHostName() + ".");
            Log.info("primary", "Inserting server " + serverOnline.serverId + " into local database...");
        }
        else {

            if (primaryServer.updateServer(serverOnline.serverId, serverAddress, serverPort)) {
                serverConnection.serverId = (serverOnline.serverId);
                kryoServer.sendToAllExceptTCP(serverConnection.getId(), new ServerOnline(serverOnline.serverId, serverAddress, serverPort));
                myConnections.put(serverOnline.serverId, serverConnection.getId());
                Log.info("primary", "Server " + serverOnline.serverId + " connected from " + serverSocket.getHostName() + ".");
            }
            else {
                Log.error("primary", "Server " + serverOnline.serverId + " not found in database!");
            }
        }
    }

    @Override
    public void received(Connection paramConnection, Object object) {

        final ServerConnection serverConnection = (ServerConnection) paramConnection;

        if (serverConnection.serverId < 0) {
            return;
        }

        if (object instanceof LeaveRoom) {
            leaveRoom(serverConnection, (LeaveRoom)object);
        }
        else if (object instanceof ServerOnline) {
            serverOnline(serverConnection, (ServerOnline)object);
        }
    }

    @Override
    public void disconnected(final Connection paramConnection) {

        final ServerConnection serverConnection = (ServerConnection) paramConnection;

        if (serverConnection.serverId > 0) {
            kryoServer.sendToAllExceptTCP(serverConnection.getId(), new ServerOffline(serverConnection.serverId));
            myConnections.remove(serverConnection.serverId);
            Log.info("primary", "Server " + serverConnection.serverId + " disconnected.");
        }
    }

    public void send(int serverId, final Object paramObject) {

        if (myConnections.containsKey(serverId)) {
            kryoServer.sendToTCP(myConnections.get(serverId), paramObject);
        }
        else {
            Log.error("primary", "server " + serverId + " not found in database!");
        }
    }
}