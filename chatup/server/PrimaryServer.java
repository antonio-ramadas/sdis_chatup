package chatup.server;

import chatup.http.PrimaryDispatcher;
import chatup.http.ServerResponse;
import chatup.model.Message;
import chatup.model.Room;
import chatup.tcp.*;

import kryonet.Connection;
import kryonet.KryoServer;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

public class PrimaryServer extends Server {

    private final PrimaryListener myServerListener;

    public PrimaryServer(int tcpPort, int httpPort) throws IOException, SQLException {

        super(new PrimaryDispatcher(), ServerType.PRIMARY, httpPort);

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

    private int sequenceRoom;

    @Override
    public ServerResponse createRoom(final String roomName, final String roomPassword, final String roomOwner) {

        final ArrayList<ServerInfo> serversList = new ArrayList<>();

        serversList.addAll(servers.values());
        Collections.sort(serversList);

        int n = (int)(Math.floor(servers.size()/2) + 1);
        int roomId = ++sequenceRoom;
        final Room newRoom = new Room(roomName, roomPassword, roomOwner);

        rooms.put(roomId, newRoom);

       /* final ArrayList<ServerInfo> mostEmpty = (ArrayList<ServerInfo>) serversList.subList(0, n);

        for (int i = 0; i < mostEmpty.size() ; i++){

            myServerListener.send(i, newRoom);
            rooms.get(roomId).registerMirror(i);

            if (!serverDatabase.insertRoom(roomId, newRoom)) {
                return ServerResponse.OperationFailed;
            }
        }*/

        return ServerResponse.SuccessResponse;
    }

    @Override
    public ServerResponse deleteRoom(int roomId) {

        System.out.println("roomId:" + roomId);

        final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        final Set<Integer> roomServers = selectedRoom.getServers();
        final DeleteRoom deleteRoom = new DeleteRoom(roomId);

        for (final Integer serverId : roomServers) {
            myServerListener.send(serverId, deleteRoom);
        }

        rooms.remove(roomId);

        return ServerResponse.SuccessResponse;
    }

    @Override
    public ServerResponse leaveRoom(int roomId, final String userToken) {

        System.out.println("roomId:" + roomId);
        System.out.println("userToken:" + userToken);

        final String userRecord = users.get(userToken);

        if (userRecord == null) {
            return ServerResponse.InvalidToken;
        }

        final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        selectedRoom.removeUser(userToken);

        final Set<Integer> roomServers = selectedRoom.getServers();
        final LeaveRoom leaveRoom = new LeaveRoom(roomId, userToken);

        for (final Integer serverId : roomServers) {
            myServerListener.send(serverId, leaveRoom);
        }

        return ServerResponse.SuccessResponse;
    }

    @Override
    public ServerResponse notifyMessage(int roomId, final String userToken, final String messageBody) {

        System.out.println("roomId:" + roomId);
        System.out.println("userToken:" + userToken);
        System.out.println("messageBody:" + messageBody);

        final String userRecord = users.get(userToken);

        if (userRecord == null) {
            return ServerResponse.InvalidToken;
        }

        final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        final Message userMessage = new Message(roomId, userToken, Instant.now().toEpochMilli(), messageBody);
        final Set<Integer> roomServers = selectedRoom.getServers();

        for (final Integer currentRoom : roomServers) {
            System.out.println(currentRoom);
        }

        return ServerResponse.SuccessResponse;
    }

    @Override
    public ServerResponse joinRoom(int roomId, final String userToken) {

        System.out.println("roomId:" + roomId);
        System.out.println("token:" + userToken);


        final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null || selectedRoom.hasUser(userToken)) {
            getLogger().alreadyJoined(roomId, userToken);
            return ServerResponse.InvalidToken;
        }

        final String userEmail = users.get(userToken);

        if (userEmail == null) {
            return ServerResponse.InvalidToken;
        }

        final Set<Integer> roomServers = selectedRoom.getServers();

        if (roomServers == null || roomServers.isEmpty()) {
            return ServerResponse.RoomNotFound;
        }

        for (final Integer serverId : roomServers) {
            myServerListener.send(serverId, new JoinRoom(roomId, userEmail, userToken));
        }

        return ServerResponse.SuccessResponse;
    }

    @Override
    public ServerResponse insertServer(int serverId, final String serverAddress, int serverPort) {

        if (servers.containsKey(serverId)) {
            return ServerResponse.OperationFailed;
        }

        servers.put(serverId, new ServerInfo(serverAddress, serverPort));

        return ServerResponse.SuccessResponse;
    }

    @Override
    public ServerResponse deleteServer(int serverId) {

        final ServerInfo selectedServer = servers.get(serverId);

        if (selectedServer == null) {
            return ServerResponse.ServerNotFound;
        }

        final DeleteServer deleteServer = new DeleteServer(serverId);

        servers.forEach((currentId, currentServer) -> {

            if (currentId != serverId) {
                myServerListener.send(currentId, deleteServer);
            }
        });

        servers.remove(serverId);

        return ServerResponse.SuccessResponse;
    }

    @Override
    public ServerResponse updateServer(int serverId, final String serverAddress, int serverPort) {

        final ServerInfo selectedServer = servers.get(serverId);

        if (selectedServer == null) {
            return ServerResponse.ServerNotFound;
        }

        selectedServer.setAddress(serverAddress);
        selectedServer.setPort(serverPort);

        final ServerOnline serverOnline = new ServerOnline(serverId, serverAddress, serverPort);

        servers.forEach((currentId, currentServer) -> {

            if (currentId != serverId) {
                myServerListener.send(currentId, serverOnline);
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

        final UserDisconnect userDisconnect = new UserDisconnect(userToken, userEmail);

        servers.forEach((severId, server) -> {

            if (server.hasUser(userToken)) {
                server.removeUser(userToken);
                myServerListener.send(severId, userDisconnect);
            }
        });

        users.remove(userToken);

        return ServerResponse.SuccessResponse;
    }

    public ServerResponse userLogin(final String userEmail, final String userToken) {

        System.out.println("email:" + userEmail);
        System.out.println("token:" + userToken);

        if (userEmail.equals("marques999@gmail.com") && userToken.equals("14191091")) {
            return ServerResponse.SuccessResponse;
        }

        return ServerResponse.AuthenticationFailed;
    }
}