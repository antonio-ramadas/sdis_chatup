package chatup.server;

import chatup.http.PrimaryDispatcher;
import chatup.http.ServerResponse;
import chatup.main.ServerLogger;
import chatup.model.Room;
import chatup.tcp.*;

import kryonet.Connection;
import kryonet.KryoServer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class PrimaryServer extends Server {

    private final PrimaryListener myServerListener;

    public PrimaryServer(int tcpPort, int httpPort) throws IOException, SQLException {

        super(new PrimaryDispatcher(), httpPort);

        final KryoServer myServer = new KryoServer(){

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

    private int sequenceRoom = 0;

    @Override
    public ServerType getType() {
        return ServerType.PRIMARY;
    }

    public ServerResponse createRoom(String roomName, String roomPassword, String roomOwner){

        ArrayList serversList = new ArrayList<ServerInfo>();

        // fill array list
        Iterator<HashMap.Entry<Integer, ServerInfo>> entries = servers.entrySet().iterator();
        while (entries.hasNext()) {
            HashMap.Entry<Integer, ServerInfo> entry = entries.next();
            serversList.add(entry.getValue());
        }

        // sort array list
        Collections.sort(serversList);

        int n = (int)(Math.floor(servers.size()/2) + 1);

        ArrayList<ServerInfo> mostEmpty = (ArrayList<ServerInfo>) serversList.subList(0, n);

        Room room = new Room(roomName, roomPassword, roomOwner);

        for (int i = 0; i < mostEmpty.size() ; i++){
            myServerListener.send(i, room);
            if (!(rooms.put(++sequenceRoom, room) == null && serverDatabase.insertRoom(sequenceRoom, room)))
                return ServerResponse.OperationFailed;
        }

        return ServerResponse.SuccessResponse;
    }

    @Override
    public ServerResponse deleteRoom(int roomId) {

        final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            ServerLogger.getInstance("0").debug("Room \"" + selectedRoom.getName() + "\" not found!");
            return ServerResponse.RoomNotFound;
        }

        final Set<Integer> roomServers = selectedRoom.getServers();

        if (roomServers == null || roomServers.isEmpty()) {
            ServerLogger.getInstance("0").debug("Room \"" + selectedRoom.getName() + "\" does not exist in any server!");
            return ServerResponse.OperationFailed;
        }

        for (final Integer serverId : roomServers) {
            myServerListener.send(serverId, new DeleteRoom(roomId));
        }

        return ServerResponse.SuccessResponse;
    }

    @Override
    public ServerResponse deleteServer(int serverId) {
        return null;
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
            myServerListener.send(serverId, new JoinRoom(roomId, userToken));
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

        servers.forEach((severId, server) -> myServerListener.send(severId, new UserDisconnect(userToken, userEmail)));
        users.remove(userToken);

        return ServerResponse.SuccessResponse;
    }

    @Override
    public boolean removeServer(int serverId) {

        final ServerInfo selectedServer = servers.get(serverId);

        if (selectedServer == null) {
            return false;
        }

        servers.forEach((currentId, currentServer) -> {

            if (currentId != serverId) {
                myServerListener.send(serverId, new DeleteServer(serverId));
            }
        });

        servers.remove(serverId);

        return true;
    }
}