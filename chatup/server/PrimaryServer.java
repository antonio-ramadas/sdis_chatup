package chatup.server;

import chatup.http.PrimaryDispatcher;
import chatup.http.ServerResponse;
import chatup.main.ServerLogger;
import chatup.model.Room;
import chatup.tcp.PrimaryListener;
import chatup.tcp.TcpNetwork;
import kryonet.Connection;
import kryonet.KryoServer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Set;

public class PrimaryServer extends Server {

    private final KryoServer myServer;
    private final PrimaryListener myServerListener;

    public PrimaryServer(int tcpPort, int httpPort) throws IOException, SQLException {

        super(new PrimaryDispatcher(), httpPort);

        myServer = new KryoServer(){

            @Override
            protected Connection newConnection() {
                return new ServerConnection();
            }
        };

        TcpNetwork.register(myServer);
        myServerListener = new PrimaryListener(this, myServer);
        myServer.addListener(myServerListener);
        myServer.bind(tcpPort);
        myServer.start();
    }

    @Override
    public ServerType getType() {
        return ServerType.PRIMARY;
    }

    private boolean notifyJoinRoom(int roomId, final String userToken) {

        final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null || selectedRoom.hasUser(userToken)) {
            ServerLogger.getInstance("0")
                        .error("User" + userToken + " has already joined room \"" + selectedRoom.getName() + "\"!");
            return false;
        }

        final Set<Integer> roomServers = selectedRoom.getServers();

        if (roomServers == null || roomServers.isEmpty()) {
            ServerLogger.getInstance("0").debug("Room \"" + selectedRoom.getName() + "\" not found!");
            return false;
        }

        for (final Integer serverId : roomServers) {
            myServerListener.send(serverId, ServerMessage.joinRoom(roomId, userToken));
        }

        return true;
    }

    @Override
    public boolean insertServer(int serverId, final String newIp, int newPort) {

        final ServerInfo selectedServer = servers.get(serverId);

        if (selectedServer != null) {
            return false;
        }

        servers.put(serverId, new ServerInfo(newIp, newPort));

        return true;
    }

    @Override
    public boolean updateServer(int serverId, final String newIp, int newPort) {

        final ServerInfo selectedServer = servers.get(serverId);

        if (selectedServer == null) {
            return false;
        }

        selectedServer.setAddress(newIp);
        selectedServer.setPort(newPort);

        return true;
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

        servers.forEach((severId, server) -> myServerListener.send(severId, generatedMessage));
        users.remove(userToken);

        return ServerResponse.SuccessResponse;
    }

    @Override
    public boolean removeServer(int serverId) {

        final ServerInfo selectedServer = servers.get(serverId);

        if (selectedServer == null) {
            return false;
        }

        final String generatedMessage = ServerMessage.deleteServer(serverId);

        servers.forEach((currentId, currentServer) -> {

            if (currentId != serverId) {
                myServerListener.send(serverId, generatedMessage);
            }
        });

        servers.remove(serverId);

        return true;
    }
}