package chatup.server;

import chatup.http.HttpFields;
import chatup.http.PrimaryDispatcher;
import chatup.http.ServerResponse;
import chatup.model.Database;
import chatup.model.CommandQueue;
import chatup.model.RoomInfo;
import chatup.tcp.*;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;

import javafx.util.Pair;
import kryonet.Connection;
import kryonet.KryoServer;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

public class PrimaryServer extends Server {

    private final Database serverDatabase;
    private final ServerLogger serverLogger;
    private final PrimaryListener myServerListener;
    private final HashMap<Integer, CommandQueue> messageQueue;
    private final HashMap<Integer, ServerInfo> servers;
    private final HashMap<Integer, RoomInfo> rooms;

    public PrimaryServer(int tcpPort, int httpPort) throws IOException, SQLException {

        //----------------------------------------------------------------
        // 1) Inicializar servidor HTTPS para receber pedidos dos clientes
        //----------------------------------------------------------------

        super(new PrimaryDispatcher(), ServerType.PRIMARY, httpPort);

        //--------------------------------------------------------------------
        // 2) Ler para memória informações dos servidores armazenadas em disco
        //--------------------------------------------------------------------

        messageQueue = new HashMap<>();
        serverDatabase = new Database(this);
        serverLogger = new ServerLogger(this);

        final HashMap<Integer, ServerInfo> myServers = serverDatabase.getServers();

        if (myServers == null) {
            servers = new HashMap<>();
        }
        else {
            servers = myServers;
        }

        if (getLogger().debugEnabled()) {

            System.out.println("*=====     servers     =====*");

            servers.forEach((serverId, serverInformation) -> {
                System.out.println("[" + serverId + "] " + serverInformation);
            });
        }

        //---------------------------------------------------------------
        // 3) Ler para memória informações das salas armazenadas em disco
        //---------------------------------------------------------------

        final HashMap<Integer, RoomInfo> myRooms = serverDatabase.getRoomInformation();

        if (myRooms == null) {
            rooms = new HashMap<>();
        }
        else {
            rooms = myRooms;
        }

        sequenceRoom = Collections.max(rooms.keySet());

        //-----------------------------------------------------------------------
        // 4) Ler para memória associações servidor <-> sala armazenadas em disco
        //-----------------------------------------------------------------------

        if (getLogger().debugEnabled()) {
            System.out.println("*=====      rooms      =====*");
        }

        rooms.forEach((roomId, roomInformation) -> {

            final Set<Integer> roomServers = serverDatabase.getServerByRoom(roomId);

            if (roomServers != null) {
                roomInformation.setServers(roomServers);
            }

            if (getLogger().debugEnabled()) {
                System.out.println("[" + roomId + "] " + roomInformation);
            }
        });

        //--------------------------------------------------------------------------------
        // 5) Inicializar servidor TCP/SSL para receber pedidos dos servidores secundários
        //--------------------------------------------------------------------------------

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

        //------------------------------------------------------------
        // 6) Apaga todas as salas inactivas registadas neste servidor
        //------------------------------------------------------------

        deleteRooms();
    }

    private int sequenceRoom;

    public ServerLogger getLogger() {
        return serverLogger;
    }

    @Override
    public JsonValue getRooms() {

        final JsonValue newArray = Json.array();

        rooms.forEach((k, v) -> newArray.asArray()
            .add(Json.object()
            .add(HttpFields.RoomName, v.getName())
            .add(HttpFields.UserToken, v.getOwner())
            .add(HttpFields.RoomPrivate, v.isPrivate())
            .add(HttpFields.RoomId, k)
        ));

        return newArray;
    }

    public ArrayList<ServerInfo> cloneRoom(int roomId, int replicationDegree) {

        final RoomInfo selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            return null;
        }

        final Set<Integer> roomServers = selectedRoom.getServers();
        final ArrayList<ServerInfo> serversList = new ArrayList<>();

        for (final HashMap.Entry<Integer, ServerInfo> entry : servers.entrySet()) {

            int serverId = entry.getKey();

            if (roomServers.contains(serverId)) {
                continue;
            }

            final ServerInfo currentServer = entry.getValue();

            if (currentServer.isOnline()) {
                serversList.add(currentServer);
            }
        }

        if (serversList.isEmpty()) {
            return null;
        }

        Collections.sort(serversList);

        if (serversList.size() < replicationDegree) {
            replicationDegree = serversList.size();
        }

        return (ArrayList<ServerInfo>) serversList.subList(0, replicationDegree);
    }

    private ArrayList<ServerInfo> cloneRoom(int replicationDegree) {

        final ArrayList<ServerInfo> serversList = new ArrayList<>();

        for (final HashMap.Entry<Integer, ServerInfo> entry : servers.entrySet()) {

            final ServerInfo currentServer = entry.getValue();

            if (currentServer.isOnline()) {
                serversList.add(currentServer);
            }
        }

        if (serversList.isEmpty()) {
            return null;
        }

        Collections.sort(serversList);

        if (serversList.size() < replicationDegree) {
            replicationDegree = serversList.size();
        }

        return (ArrayList<ServerInfo>) serversList.subList(0, replicationDegree);
    }

    @Override
    public ServerResponse createRoom(final String roomName, final String roomPassword, final String roomOwner) {

        //-----------------------------------------------------------
        // 1) Verificar se utilizador tem sessão iniciada no servidor
        //-----------------------------------------------------------

        final String userRecord = users.get(roomOwner);

        if (userRecord == null) {
            return ServerResponse.InvalidToken;
        }

        //--------------------------------------------------------
        // 2) Calcular nível de replicação desejado para esta sala
        //--------------------------------------------------------

        int roomId = ++sequenceRoom;
        int n = (int)(Math.floor(servers.size() / 2) + 1);
        final RoomInfo newRoom = new RoomInfo(roomName, roomPassword, roomOwner);

        //----------------------------------------------------
        // 3) Registar alterações na base de dados do servidor
        //----------------------------------------------------

        if (serverDatabase.insertRoom(roomId, newRoom)) {
            rooms.put(roomId, newRoom);
        }
        else {
            return ServerResponse.DatabaseError;
        }

        //-----------------------------------------------------------
        // 4) Notificar os restantes servidores da criação desta sala
        //-----------------------------------------------------------

        final ArrayList<ServerInfo> mostEmpty = cloneRoom(n);

        if (mostEmpty == null || mostEmpty.isEmpty()) {
            return ServerResponse.ServiceOffline;
        }

        for (int i = 0; i < mostEmpty.size() ; i++){

            int serverId = mostEmpty.get(i).getId();

            myServerListener.send(serverId, newRoom);
            newRoom.registerServer(i);

            if (serverDatabase.insertServerRoom(serverId, roomId)) {
                newRoom.registerServer(serverId);
            }
            else {
                return ServerResponse.DatabaseError;
            }
        }

        return ServerResponse.SuccessResponse;
    }

    private void insertQueue(int serverId, final Object paramObject) {

        final CommandQueue serverMessages = messageQueue.get(serverId);

        if (serverMessages == null) {
            messageQueue.put(serverId, new CommandQueue());
            messageQueue.get(serverId).push(paramObject);
        }
        else {
            serverMessages.push(paramObject);
        }
    }

    @Override
    public ServerResponse deleteRoom(int roomId) {

        //-------------------------------------------------------
        // 1) Verificar se sala escolhida existe na base de dados
        //-------------------------------------------------------

        final RoomInfo selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        //------------------------------------------------
        // 2) Obter lista dos servidores mirror dessa sala
        //------------------------------------------------

        final Set<Integer> roomServers = selectedRoom.getServers();
        final DeleteRoom deleteRoom = new DeleteRoom(roomId);

        //-----------------------------------------------------------------
        // 3) Notificar os servidores secundários das alterações efectuadas
        //-----------------------------------------------------------------

        for (final Integer serverId : roomServers) {

            final ServerInfo currentServer = servers.get(serverId);

            if (currentServer == null) {
                continue;
            }

            currentServer.updateTimestamp();

            if (currentServer.isOnline()) {
                myServerListener.send(serverId, deleteRoom);
            }
            else {
                insertQueue(serverId, deleteRoom);
            }
        }

        //-------------------------------------------------------------
        // 4) Registar alterações na base de dados do servidor primário
        //-------------------------------------------------------------

        if (serverDatabase.deleteRoom(roomId)) {
            rooms.remove(roomId);
        }
        else {
            return ServerResponse.DatabaseError;
        }

        return ServerResponse.SuccessResponse;
    }

    @Override
    public ServerResponse leaveRoom(int roomId, final String userToken) {

        //-----------------------------------------------------------
        // 1) Verificar se utilizador tem sessão iniciada no servidor
        //-----------------------------------------------------------

        final String userRecord = users.get(userToken);

        if (userRecord == null) {
            return ServerResponse.InvalidToken;
        }

        //-----------------------------------------------------------
        // 2) Verificar se sala escolhida existe no servidor primário
        //-----------------------------------------------------------

        final RoomInfo selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            return ServerResponse.RoomNotFound;
        }

        //-------------------------------------------
        // 3) Registar saída do utilizador dessa sala
        //-------------------------------------------

        if (!selectedRoom.removeUser(userToken)) {
            return ServerResponse.OperationFailed;
        }

        //------------------------------------------------
        // 4) Obter lista dos servidores mirror dessa sala
        //------------------------------------------------

        final Set<Integer> roomServers = selectedRoom.getServers();
        final LeaveRoom leaveRoom = new LeaveRoom(roomId, userToken);

        //-----------------------------------------------------------------------
        // 5) Notificar os restantes servidores da saída do utilizador dessa sala
        //-----------------------------------------------------------------------

        for (final Integer serverId : roomServers) {
            myServerListener.send(serverId, leaveRoom);
        }

        return ServerResponse.SuccessResponse;
    }

    @Override
    public Pair<ServerResponse, ServerInfo> joinRoom(int roomId, final String userPassword, final String userToken) {

        //-----------------------------------------------------------
        // 1) Verificar se utilizador tem sessão iniciada no servidor
        //-----------------------------------------------------------

        final String userEmail = users.get(userToken);

        if (userEmail == null) {
            return new Pair<>(ServerResponse.InvalidToken, null);
        }

        //-----------------------------------------------------------
        // 2) Verificar se sala escolhida existe no servidor primário
        //-----------------------------------------------------------

        final RoomInfo selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            return new Pair<>(ServerResponse.RoomNotFound, null);
        }

        //------------------------------------------------------------
        // 3) Verificar se utilizador já se encontra na sala escolhida
        //------------------------------------------------------------

        if (selectedRoom.hasUser(userToken)) {
            return new Pair<>(ServerResponse.AlreadyJoined, null);
        }

        //---------------------------------------------------------------
        // 4) Verificar palavra-passe da sala introduzida pelo utilizador
        //---------------------------------------------------------------

        if (selectedRoom.isPrivate()) {

            if (!selectedRoom.validatePassword(userPassword)) {
                return new Pair<>(ServerResponse.WrongPassword, null);
            }
        }

        //-------------------------------------------------------
        // 5) Obter lista dos servidores mirror da sala escolhida
        //-------------------------------------------------------

        final Set<Integer> roomServers = selectedRoom.getServers();

        if (roomServers == null || roomServers.isEmpty()) {
            return new Pair<>(ServerResponse.RoomNotFound, null);
        }

        //------------------------------------------------------------
        // 6) Escolher melhor servidor disponível para este utilizador
        //------------------------------------------------------------

        boolean serviceOffline = true;
        int minimumLoad = Integer.MAX_VALUE;
        int minimumServer = 0;

        for (final Integer serverId : roomServers) {

            final ServerInfo selectedServer = servers.get(serverId);

            if (selectedServer == null) {
                continue;
            }

            System.out.println("server " + serverId + "is " + selectedServer.isOnline());

            if (selectedServer.isOnline()) {

                serviceOffline = false;

                int currentLoad = selectedServer.getLoad();

                if (currentLoad < minimumLoad) {
                    minimumLoad = currentLoad;
                    minimumServer = serverId;
                }
            }
        }

        System.out.println(minimumLoad);
        System.out.println(minimumServer);

        if (serviceOffline || minimumServer < 1) {
            // criar sala noutro servidor
            return new Pair<>(ServerResponse.ServiceOffline, null);
        }

        //---------------------------------------------
        // 7) Registar entrada do utilizador nesta sala
        //---------------------------------------------

        selectedRoom.registerUser(userToken);

        //-------------------------------------------------------------
        // 8) Registar entrada do utilizador nos servidores secundários
        //-------------------------------------------------------------

        final ServerInfo currentServer = servers.get(minimumServer);

        if (currentServer != null) {
            currentServer.registerUser(userToken);
        }
        else {
            return new Pair<>(ServerResponse.ServerNotFound, null);
        }

        //--------------------------------------------------------------
        // 9) Notificar os restantes servidores da entrada do utilizador
        //--------------------------------------------------------------

        final JoinRoom joinRoom = new JoinRoom(roomId, userEmail, userToken);

        for (final Integer serverId : roomServers) {
            myServerListener.send(serverId, joinRoom);
        }

        return new Pair<>(ServerResponse.SuccessResponse, currentServer);
    }

    @Override
    public ServerResponse insertServer(final ServerInfo serverInfo) {

        int serverId = serverInfo.getId();

        if (servers.containsKey(serverId)) {
            return updateServer(serverInfo);
        }

        if (serverDatabase.insertServer(serverInfo)) {
            servers.put(serverId, serverInfo);
            getLogger().insertServer(serverId);
        }
        else {
            return ServerResponse.DatabaseError;
        }

        return serverOnline(serverId, serverInfo.getTimestamp());
    }

    @Override
    public ServerResponse deleteServer(int serverId) {

        //-----------------------------------------------------------
        // 1) Verificar se servidor escolhido existe na base de dados
        //-----------------------------------------------------------

        final ServerInfo selectedServer = servers.get(serverId);

        if (selectedServer == null) {
            return ServerResponse.ServerNotFound;
        }

        //---------------------------------------------------------------
        // 2) Notificar os restantes servidores da remoção desse servidor
        //---------------------------------------------------------------

        final DeleteServer deleteServer = new DeleteServer(serverId);

        servers.forEach((currentId, currentServer) -> {

            if (currentId != serverId) {

                if (currentServer.isOnline()) {
                    myServerListener.send(currentId, deleteServer);
                }
                else {
                    insertQueue(serverId, deleteServer);
                }

                currentServer.updateTimestamp();
            }
        });

        //----------------------------------------------------
        // 3) Registar remoção desse servidor na base de dados
        //----------------------------------------------------

        rooms.forEach((roomId, room) -> {

            if (room.removeServer(serverId)) {
                serverDatabase.deleteServerRoom(serverId, roomId);
            }
        });

        if (serverDatabase.deleteServer(serverId)) {
            servers.remove(serverId);
        }
        else {
            return ServerResponse.DatabaseError;
        }

        return ServerResponse.SuccessResponse;
    }

    private ServerResponse deleteRooms() {

        final long currentTimestamp = Instant.now().getEpochSecond() - 3600;
        final Iterator<Map.Entry<Integer, RoomInfo>> roomsIterator = rooms.entrySet().iterator();

        while (roomsIterator.hasNext()) {

            final Map.Entry<Integer, RoomInfo> currentEntry = roomsIterator.next();
            final RoomInfo selectedRoom = currentEntry.getValue();

            if (!selectedRoom.isEmpty() || selectedRoom.getTimestamp() > currentTimestamp) {
                continue;
            }

            int roomId = currentEntry.getKey();

            //---------------------------------------------------------------
            // 2) Notificar os restantes servidores da remoção desse servidor
            //---------------------------------------------------------------

            final DeleteRoom deleteRoom = new DeleteRoom(roomId);
            final Set<Integer> serverRooms = selectedRoom.getServers();

            for (final Integer serverId : serverRooms) {

                final ServerInfo currentServer = servers.get(serverId);

                if (currentServer == null) {
                    continue;
                }

                if (currentServer.isOnline()) {
                    myServerListener.send(serverId, deleteRoom);
                }
                else {
                    insertQueue(roomId, deleteRoom);
                }

                currentServer.updateTimestamp();
            }

            //----------------------------------------------------
            // 3) Registar remoção desse servidor na base de dados
            //----------------------------------------------------

            if (serverDatabase.deleteServer(roomId)) {
                servers.remove(roomId);
            }
            else {
                return ServerResponse.DatabaseError;
            }

            roomsIterator.remove();
        }

        return ServerResponse.SuccessResponse;
    }

    private ServerResponse serverOnline(int serverId, long serverTimestamp) {

        final ServerInfo selectedServer = servers.get(serverId);

        if (selectedServer == null) {
            return ServerResponse.ServerNotFound;
        }

        long currentTimestamp = selectedServer.getTimestamp();

        if (Long.compare(currentTimestamp, serverTimestamp) > 0) {

            final CommandQueue serverQueue = messageQueue.get(serverId);

            if (serverQueue == null || serverQueue.empty()) {
                return ServerResponse.OperationFailed;
            }

            myServerListener.send(serverId, serverQueue);
            messageQueue.remove(serverId);
        }
        else {
            selectedServer.setTimestamp(serverTimestamp);
        }

        selectedServer.setStatus(true);

        return ServerResponse.SuccessResponse;
    }

    public ServerResponse disconnectServer(int serverId) {

        final ServerInfo selectedServer = servers.get(serverId);

        if (selectedServer == null) {
            return ServerResponse.ServerNotFound;
        }

        selectedServer.setStatus(false);

        return ServerResponse.SuccessResponse;
    }

    @Override
    public ServerResponse updateServer(final ServerInfo serverInfo) {

        //-----------------------------------------------------------
        // 1) Verificar se servidor escolhido existe na base de dados
        //-----------------------------------------------------------

        int serverId = serverInfo.getId();
        final ServerInfo selectedServer = servers.get(serverId);

        if (selectedServer == null) {
            return ServerResponse.ServerNotFound;
        }

        //----------------------------------------------------
        // 2) Registar alterações do servidor na base de dados
        //----------------------------------------------------

        boolean sameInformation = true;

        if (!selectedServer.getAddress().equals(serverInfo.getAddress())) {
            selectedServer.setAddress(serverInfo.getAddress());
            sameInformation = false;
        }

        if (selectedServer.getPort() != serverInfo.getPort()) {
            selectedServer.setPort(serverInfo.getPort());
            sameInformation = false;
        }

        if (sameInformation) {
            return ServerResponse.SuccessResponse;
        }

        if (!serverDatabase.updateServer(serverInfo)) {
            return ServerResponse.DatabaseError;
        }

        //---------------------------------------------------------------------------
        // 3) Notificar os restantes servidores das alterações efectuadas no servidor
        //---------------------------------------------------------------------------

        final UpdateServer updateServer = new UpdateServer
        (
            selectedServer.getId(),
            selectedServer.getTimestamp(),
            selectedServer.getAddress(),
            selectedServer.getPort()
        );

        servers.forEach((currentId, currentServer) -> {

            if (currentId != serverId) {

                currentServer.updateTimestamp();

                if (currentServer.isOnline()) {
                    myServerListener.send(currentId, updateServer);
                }
                else {
                    insertQueue(serverId, updateServer);
                }
            }
        });

        return serverOnline(serverId, serverInfo.getTimestamp());
    }

    @Override
    public ServerResponse userDisconnect(final String userEmail, final String userToken) {

        //-----------------------------------------------------------
        // 1) Verificar se utilizador tem sessão iniciada no servidor
        //-----------------------------------------------------------

        final String userRecord = users.get(userToken);

        if (userRecord == null || !userRecord.equals(userEmail)) {
            return ServerResponse.InvalidToken;
        }

        //-----------------------------------------------------------------------
        // 2) Notificar os restantes servidores do fim da sessão desse utilizador
        //-----------------------------------------------------------------------

        final UserDisconnect userDisconnect = new UserDisconnect(userToken, userEmail);

        servers.forEach((severId, server) -> {

            if (server.removeUser(userToken)) {
                myServerListener.send(severId, userDisconnect);
            }
        });

        //----------------------------------------------------------------
        // 3) Remover utilizador das salas existentes no servidor primário
        //----------------------------------------------------------------

        rooms.forEach((roomId, room) -> room.removeUser(userToken));

        //-------------------------------------------------------------------
        // 4) Remover utilizador da lista de utilizadores ligados ao servidor
        //-------------------------------------------------------------------

        users.remove(userToken);

        return ServerResponse.SuccessResponse;
    }

    public ServerResponse userLogin(final String userEmail, final String userToken) {

        if (users.containsKey(userToken)) {
            return ServerResponse.InvalidToken;
        }

        if (userEmail.equals("marques999@gmail.com") && userToken.equals("14191091")) {
            users.put(userToken, userEmail);
        }
        else {
            return ServerResponse.AuthenticationFailed;
        }

        return ServerResponse.SuccessResponse;
    }

    public boolean validateToken(final String userToken) {
        return users.containsKey(userToken);
    }
}