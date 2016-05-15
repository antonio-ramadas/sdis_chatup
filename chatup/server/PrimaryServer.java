package chatup.server;

import chatup.http.PrimaryDispatcher;
import chatup.http.ServerResponse;
import chatup.main.ServerLogger;
import chatup.room.Room;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

public class PrimaryServer extends Server {

    public PrimaryServer(ServerKeystore serverKeystore, short httpPort, short tcpPort) {
        super(serverKeystore, new PrimaryDispatcher(), httpPort, tcpPort);
    }

    @Override
    public ServerType getType() {
        return ServerType.PRIMARY;
    }

    private boolean notifyJoinRoom(int roomId, final String userToken) {

        final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null || selectedRoom.hasUser(userToken)) {
            ServerLogger.getInstance("0").error("User" + userToken + " has already joined room \"" + selectedRoom.getName() + "\"!");
            return false;
        }

        final Set<Integer> roomServers = selectedRoom.getServers();

        if (roomServers == null || roomServers.isEmpty()) {
            ServerLogger.getInstance("0").debug("Room \"" + selectedRoom.getName() + "\" not found!");
            return false;
        }

        for (final Integer serverId : roomServers) {

            final ServerInfo currentServer = servers.get(serverId);

            if (currentServer != null) {
                tcpConnection.send(currentServer, ServerMessage.joinRoom(roomId, userToken));
            }
        }

        return true;
    }

    @Override
    public ServerResponse insertServer(int serverId, String newIp, short newPort) {
        final ServerInfo selectedServer = servers.get(serverId);
        return ServerResponse.SuccessResponse;
    }

    @Override
    public ServerResponse updateServer(int serverId, final String newIp, short newPort) {

        final ServerInfo selectedServer = servers.get(serverId);

        if (selectedServer == null) {
            return ServerResponse.ServerNotFound;
        }

        try {
            selectedServer.setAddress(InetAddress.getByName(newIp));
            selectedServer.setTcpPort(newPort);
        }
        catch (UnknownHostException ex) {
            return ServerResponse.ProtocolError;
        }

        final String generatedMessage = ServerMessage.replaceServer(serverId, newIp, newPort);

        servers.forEach((currentId, currentServer) -> {

            if (currentId != serverId) {
                tcpConnection.send(currentServer, generatedMessage);
            }
        });

        return ServerResponse.SuccessResponse;
    }

    @Override
    public ServerResponse userDisconnect(final String userToken, final String userEmail) {

        System.out.println("email:" + userEmail);
        System.out.println("token:" + userToken);

        final String userRecord = users.get(userToken);

        if (userRecord == null || !userRecord.equals(userEmail)) {
            return ServerResponse.InvalidToken;
        }

        final String generatedMessage = ServerMessage.userDisconnect(userToken, userEmail);

        servers.forEach((severId, server) -> tcpConnection.send(server, generatedMessage));
        users.remove(userToken);

        return ServerResponse.SuccessResponse;
    }

    @Override
    public ServerResponse removeServer(int serverId) {

        final ServerInfo selectedServer = servers.get(serverId);

        if (selectedServer == null) {
            return ServerResponse.ServerNotFound;
        }

        final String generatedMessage = ServerMessage.deleteServer(serverId);

        servers.forEach((currentId, currentServer) -> {

            if (currentId != serverId) {
                tcpConnection.send(currentServer, generatedMessage);
            }
        });

        servers.remove(serverId);

        return ServerResponse.SuccessResponse;
    }
}