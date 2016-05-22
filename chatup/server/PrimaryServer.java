package chatup.server;

import chatup.http.PrimaryDispatcher;
import chatup.http.ServerResponse;
import chatup.model.Message;
import chatup.model.MessageCache;
import chatup.model.Room;
import chatup.tcp.*;
import com.esotericsoftware.minlog.Log;

import kryonet.Connection;
import kryonet.KryoServer;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
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

    @Override
    public ServerType getType() {
        return ServerType.PRIMARY;
    }

    @Override
    public int getId() {
        return -1;
    }

    public ServerResponse createRoom(String roomName, String roomPassword, String roomOwner){

        final ArrayList<ServerInfo> serversList = new ArrayList<>();

        serversList.addAll(servers.values());
        Collections.sort(serversList);

        int n = (int)(Math.floor(servers.size()/2) + 1);

        int roomId = ++sequenceRoom;
        final ArrayList<ServerInfo> mostEmpty = (ArrayList<ServerInfo>) serversList.subList(0, n);
        final Room newRoom = new Room(roomName, roomPassword, roomOwner);

        rooms.put(roomId, newRoom);

        for (int i = 0; i < mostEmpty.size() ; i++){

            myServerListener.send(i, newRoom);
            rooms.get(roomId).registerMirror(i);

            if (!serverDatabase.insertRoom(roomId, newRoom)) {
                return ServerResponse.OperationFailed;
            }
        }

        return ServerResponse.SuccessResponse;
    }

    @Override
    public ServerResponse notifyMessage(int roomId, String userToken, String messageBody) {

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
    public ServerResponse registerMessage(int roomId, final Message paramMessage) {
        return ServerResponse.OperationFailed;
    }

    @Override
    public ServerResponse deleteRoom(int roomId) {
        return ServerResponse.OperationFailed;
    }

    @Override
    public ServerResponse leaveRoom(int roomId, final String userToken) {
        return ServerResponse.OperationFailed;
    }

    @Override
    public ServerResponse joinRoom(int roomId, final String userToken) {

        final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null || selectedRoom.hasUser(userToken)) {
            getLogger().alreadyJoined(roomId, userToken);
            return ServerResponse.InvalidToken;
        }

        final Set<Integer> roomServers = selectedRoom.getServers();

        if (roomServers == null || roomServers.isEmpty()) {
            return ServerResponse.RoomNotFound;
        }

        for (final Integer serverId : roomServers) {
            myServerListener.send(serverId, new JoinRoom(roomId, userToken));
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
    public ServerResponse removeServer(int serverId) {

        final ServerInfo selectedServer = servers.get(serverId);

        if (selectedServer == null) {
            return ServerResponse.ServerNotFound;
        }

        final DeleteServer deleteServer = new DeleteServer(serverId);

        servers.forEach((currentId, currentServer) -> {

            if (currentId != serverId) {
                myServerListener.send(serverId, deleteServer);
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

    @Override
    public ServerResponse syncRoom(int roomId, final MessageCache messageCache) {

        System.out.println("roomId:" + roomId);
        System.out.println("#messages:" + messageCache.size());

        if (roomId < 0 || messageCache == null) {
            return ServerResponse.MissingParameters;
        }

        final Room selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        final SyncRoom syncRoom = new SyncRoom(roomId, messageCache);
        final Set<Integer> roomServers = selectedRoom.getServers();

        if (roomServers == null || roomServers.isEmpty()) {
            return ServerResponse.RoomNotFound;
        }

        servers.remove(serverId);

        return ServerResponse.SuccessResponse;
    }
}