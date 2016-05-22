package chatup.server;

import chatup.http.HttpFields;
import chatup.http.PrimaryDispatcher;
import chatup.http.ServerResponse;
import chatup.model.RoomInfo;
import chatup.tcp.*;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;

import kryonet.Connection;
import kryonet.KryoServer;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class PrimaryServer extends Server {

    private final PrimaryListener myServerListener;
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
    }

    private int sequenceRoom;

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

    @Override
    public ServerResponse createRoom(final String roomName, final String roomPassword, final String roomOwner) {

        final ArrayList<ServerInfo> serversList = new ArrayList<>();

        serversList.addAll(servers.values());
        Collections.sort(serversList);

        int n = (int)(Math.floor(servers.size()/2) + 1);
        int roomId = ++sequenceRoom;
        final RoomInfo newRoom = new RoomInfo(roomName, roomPassword, roomOwner);

        //----------------------------------------------------
        // 2) Registar alterações na base de dados do servidor
        //----------------------------------------------------

        if (serverDatabase.insertRoom(roomId, newRoom)) {
            rooms.put(roomId, newRoom);
        }

        //--------------------------------------------------------
        // 3) Notificar os servidores mirror da criação desta sala
        //--------------------------------------------------------

        final ArrayList<ServerInfo> mostEmpty = (ArrayList<ServerInfo>) serversList.subList(0, n);

        for (int i = 0; i < mostEmpty.size() ; i++){

            myServerListener.send(i, newRoom);
            rooms.get(roomId).registerServer(i);

            if (!serverDatabase.insertRoom(roomId, newRoom)) {
                return ServerResponse.OperationFailed;
            }
        }

        return ServerResponse.SuccessResponse;
    }

    @Override
    public ServerResponse deleteRoom(int roomId) {

        System.out.println("roomId:" + roomId);

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

        //------------------------------------------------------------
        // 3) Notificar os servidores mirror das alterações efectuadas
        //------------------------------------------------------------

        for (final Integer serverId : roomServers) {
            myServerListener.send(serverId, deleteRoom);
        }

        //----------------------------------------------------
        // 3) Registar alterações na base de dados do servidor
        //----------------------------------------------------

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

        System.out.println("roomId:" + roomId);
        System.out.println("userToken:" + userToken);

        //-----------------------------------------------------------
        // 1) Verificar se utilizador tem sessão iniciada no servidor
        //-----------------------------------------------------------

        final String userRecord = users.get(userToken);

        if (userRecord == null) {
            return ServerResponse.InvalidToken;
        }

        //-------------------------------------------------------
        // 2) Verificar se sala escolhida existe na base de dados
        //-------------------------------------------------------

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
    public ServerResponse joinRoom(int roomId, final String userToken) {

        System.out.println("roomId:" + roomId);
        System.out.println("token:" + userToken);

        //-----------------------------------------------------------
        // 1) Verificar se utilizador tem sessão iniciada no servidor
        //-----------------------------------------------------------

        final String userEmail = users.get(userToken);

        if (userEmail == null) {
            return ServerResponse.InvalidToken;
        }

        //-------------------------------------------------------
        // 2) Verificar se sala escolhida existe na base de dados
        //-------------------------------------------------------

        final RoomInfo selectedRoom = rooms.get(roomId);

        if (selectedRoom == null || selectedRoom.hasUser(userToken)) {
            return ServerResponse.InvalidToken;
        }

        //-------------------------------------------------------
        // 3) Obter lista dos servidores mirror da sala escolhida
        //-------------------------------------------------------

        final Set<Integer> roomServers = selectedRoom.getServers();

        if (roomServers == null || roomServers.isEmpty()) {
            return ServerResponse.RoomNotFound;
        }

        //---------------------------------------------
        // 4) Registar entrada do utilizador nesta sala
        //---------------------------------------------

        selectedRoom.registerUser(userToken);

        //-------------------------------------------------------------
        // 5) Registar entrada do utilizador nos servidores secundários
        //-------------------------------------------------------------

        int minimumLoad = Integer.MAX_VALUE;
        int minimumServer = 0;

        for (final Integer serverId : roomServers) {

            final ServerInfo currentServer = servers.get(serverId);

            if (currentServer != null) {

                int currentLoad = currentServer.getLoad();

                if (currentLoad < minimumLoad) {
                    minimumLoad = currentLoad;
                    minimumServer = serverId;
                }
            }
        }

        final ServerInfo currentServer = servers.get(minimumServer);

        if (currentServer != null) {
            currentServer.registerUser(userToken);
        }

        //--------------------------------------------------------------
        // 6) Notificar os restantes servidores da entrada do utilizador
        //--------------------------------------------------------------

        for (final Integer serverId : roomServers) {
            myServerListener.send(serverId, new JoinRoom(roomId, userEmail, userToken));
        }

        return ServerResponse.SuccessResponse;
    }

    @Override
    public ServerResponse insertServer(int serverId, final String serverAddress, int serverPort) {

        //----------------------------------------------------
        // 1) Verificar se servidor já existe na base de dados
        //----------------------------------------------------

        if (servers.containsKey(serverId)) {
            return ServerResponse.OperationFailed;
        }

        //------------------------------------------------------
        // 2) Registar inserção de novo servidor na base de dados
        //------------------------------------------------------

        if (serverDatabase.insertServer(serverId, serverAddress, serverPort)) {
            servers.put(serverId, new ServerInfo(serverAddress, serverPort));
        }
        else {
            return ServerResponse.DatabaseError;
        }

        return ServerResponse.SuccessResponse;
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

        //--------------------------------------------------------------
        // 2) Notificar os restantes servidores da remoção desse servidor
        //--------------------------------------------------------------

        final DeleteServer deleteServer = new DeleteServer(serverId);

        servers.forEach((currentId, currentServer) -> {

            if (currentId != serverId) {
                myServerListener.send(currentId, deleteServer);
            }
        });

        //----------------------------------------------------
        // 3) Registar remoção desse servidor na base de dados
        //----------------------------------------------------

        if (serverDatabase.deleteServer(serverId)) {
            servers.remove(serverId);
        }
        else {
            return ServerResponse.DatabaseError;
        }

        return ServerResponse.SuccessResponse;
    }

    @Override
    public ServerResponse updateServer(int serverId, final String serverAddress, int serverPort) {

        //-----------------------------------------------------------
        // 1) Verificar se servidor escolhido existe na base de dados
        //-----------------------------------------------------------

        final ServerInfo selectedServer = servers.get(serverId);

        if (selectedServer == null) {
            return ServerResponse.ServerNotFound;
        }

        //----------------------------------------------------
        // 2) Registar alterações do servidor na base de dados
        //----------------------------------------------------

        selectedServer.setAddress(serverAddress);
        selectedServer.setPort(serverPort);

        if (!serverDatabase.updateServer(serverId, serverAddress, serverPort)) {
            return ServerResponse.DatabaseError;
        }

        //----------------------------------------------------
        // 3) Notificar os restantes servidores das alterações
        //----------------------------------------------------

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

        //-----------------------------------------------------------
        // 1) verificar se utilizador tem sessão iniciada no servidor
        //-----------------------------------------------------------

        final String userRecord = users.get(userToken);

        if (userRecord == null || !userRecord.equals(userEmail)) {
            return ServerResponse.InvalidToken;
        }

        //-----------------------------------------------------------------------
        // 2) Notificar os restantes servidores do fim da sessão deste utilizador
        //-----------------------------------------------------------------------

        final UserDisconnect userDisconnect = new UserDisconnect(userToken, userEmail);

        servers.forEach((severId, server) -> {

            if (server.removeUser(userToken)) {
                myServerListener.send(severId, userDisconnect);
            }
        });

        //-------------------------------------------------------
        // 2) Remover utilizador das salas existentes no servidor
        //-------------------------------------------------------

        rooms.forEach((roomId, room) -> room.removeUser(userToken));

        //-------------------------------------------------------
        // 2) Remover utilizador da lista de utilizadores ligados
        //-------------------------------------------------------

        users.remove(userToken);

        return ServerResponse.SuccessResponse;
    }

    public ServerResponse userLogin(final String userEmail, final String userToken) {

        System.out.println("email:" + userEmail);
        System.out.println("token:" + userToken);

        //--------------------------------------------------------------
        // 1) Verificar se utilizador já tem sessão iniciada no servidor
        //--------------------------------------------------------------

        if (users.containsKey(userToken)) {
            return ServerResponse.InvalidToken;
        }

        //----------------------------------------------------------
        // 2) Registar início de sessão deste utilizador no servidor
        //----------------------------------------------------------

        if (userEmail.equals("marques999@gmail.com") && userToken.equals("14191091")) {
           users.put(userToken, userEmail);
        }
        else {
            return ServerResponse.AuthenticationFailed;
        }

        return ServerResponse.SuccessResponse;
    }
}