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
    private final PrimaryServerListener myServerListener;
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

        System.out.println("*=====     servers     =====*");
        servers.forEach((k, v) -> System.out.println("[" + k + "] " + v));

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

        System.out.println("*=====      rooms      =====*");

        rooms.forEach((roomId, roomInformation) -> {

            final Set<Integer> roomServers = serverDatabase.getServerByRoom(roomId);

            if (roomServers != null) {
                roomInformation.setServers(roomServers);
            }

            System.out.println("[" + roomId + "] " + roomInformation);
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

        TcpNetwork.registerPrimary(myServer);
        myServerListener = new PrimaryServerListener(this, myServer);
        myServer.addListener(myServerListener);
        myServer.bind(tcpPort);
        myServer.start();

        //------------------------------------------------------------
        // 6) Apaga todas os servidores inactivos nas últimas 24 horas
        //------------------------------------------------------------

        purgeServers();

        //-------------------------------------------------------
        // 7) Apaga todas as salas inactivas nas últimas 24 horas
        //-------------------------------------------------------

        purgeRooms();
    }

    private int sequenceRoom;

    final ServerLogger getLogger() {
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

    // TODO: verificar a atribuição das salas aos servidores, testar em casos excepcionais (ver também @createRoom)
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

    // TODO: verificar algoritmo de replicação de emergência de uma sala (ver também @joinRoom)
    private Pair<ServerResponse, ServerInfo> relocateRoom(int roomId) {

        final RoomInfo selectedRoom = rooms.get(roomId);

        if (selectedRoom == null) {
            return new Pair<>(ServerResponse.RoomNotFound, null);
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
            return new Pair<>(ServerResponse.ServiceOffline, null);
        }

        Collections.sort(serversList);

        final ServerInfo selectedServer = serversList.get(0);
        int serverId = selectedServer.getId();

        myServerListener.sendServer(serverId, selectedRoom);
        selectedRoom.registerServer(serverId);

        if (serverDatabase.associateServer(serverId, roomId)) {
            selectedRoom.registerServer(serverId);
        }
        else {
            return new Pair<>(ServerResponse.DatabaseError, null);
        }

        return new Pair<>(ServerResponse.SuccessResponse, selectedServer);
    }

    // TODO: verificar a atribuição das salas aos servidores, testar em casos excepcionais (ver também @cloneRoom)
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

        for (final ServerInfo serverInformation : mostEmpty) {

            int serverId = serverInformation.getId();

            newRoom.registerServer(serverId);

            if (serverDatabase.associateServer(serverId, roomId)) {
                newRoom.registerServer(serverId);
            }
            else {
                return ServerResponse.DatabaseError;
            }
        }

        for (final ServerInfo serverInformation : mostEmpty) {
            myServerListener.sendServer(serverInformation.getId(), newRoom);
        }

        return ServerResponse.SuccessResponse;
    }

    // TODO: Verified!
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

        final Set<Integer> roomServers = selectedRoom.getServers();

        if (roomServers == null || roomServers.isEmpty()) {
            return ServerResponse.OperationFailed;
        }

        //-------------------------------------------------------------
        // 2) Registar alterações na base de dados do servidor primário
        //-------------------------------------------------------------

        if (serverDatabase.deleteRoom(roomId)) {
            rooms.remove(roomId);
        }
        else {
            return ServerResponse.DatabaseError;
        }

        //-----------------------------------------------------------------
        // 3) Notificar os servidores secundários das alterações efectuadas
        //-----------------------------------------------------------------

        final DeleteRoom deleteRoom = new DeleteRoom(roomId);

        for (final Integer serverId : roomServers) {

            final ServerInfo currentServer = servers.get(serverId);

            if (currentServer == null) {
                continue;
            }

            currentServer.updateTimestamp();
            serverDatabase.updateServer(currentServer);

            if (currentServer.isOnline()) {
                myServerListener.sendServer(serverId, deleteRoom);
            }
            else {
                insertQueue(serverId, deleteRoom);
            }
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

        //-----------------------------------------------------------------------
        // 4) Notificar os restantes servidores da saída do utilizador dessa sala
        //-----------------------------------------------------------------------

        final Set<Integer> roomServers = selectedRoom.getServers();

        if (roomServers == null || roomServers.isEmpty()) {
            return ServerResponse.OperationFailed;
        }

        final LeaveRoom leaveRoom = new LeaveRoom(roomId, userToken);

        for (final Integer serverId : roomServers) {
            myServerListener.sendServer(serverId, leaveRoom);
        }

        return ServerResponse.SuccessResponse;
    }

    // TODO: entrar numa sala quando nenhum dos servidores está disponível
    // TODO: verificar se servidor primário replica a sala num outro servidor disponível
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

        //---------------------------------------------------------------
        // 3) Verificar palavra-passe da sala introduzida pelo utilizador
        //---------------------------------------------------------------

        if (selectedRoom.isPrivate()) {

            if (!selectedRoom.validatePassword(userPassword)) {
                return new Pair<>(ServerResponse.WrongPassword, null);
            }
        }

        //------------------------------------------------------------
        // 4) Verificar se utilizador já se encontra na sala escolhida
        //------------------------------------------------------------

        if (selectedRoom.hasUser(userToken)) {
            return new Pair<>(ServerResponse.AlreadyJoined, null);
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

            if (selectedServer.isOnline()) {

                serviceOffline = false;

                int currentLoad = selectedServer.getLoad();

                if (currentLoad < minimumLoad) {
                    minimumLoad = currentLoad;
                    minimumServer = serverId;
                }
            }
        }

        if (serviceOffline || minimumServer < 1) {

            final Pair<ServerResponse, ServerInfo> operationResult = relocateRoom(roomId);
            final ServerResponse serverResponse = operationResult.getKey();

            if (operationResult.getKey() == ServerResponse.SuccessResponse) {
                minimumServer = operationResult.getValue().getId();
            }
            else {
                return new Pair<>(serverResponse, null);
            }
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
            myServerListener.sendServer(serverId, joinRoom);
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
            serverLogger.insertServer(serverId);
        }
        else {
            return ServerResponse.DatabaseError;
        }

        return serverOnline(serverId, serverInfo.getTimestamp());
    }

    // TODO: testar algoritmo para apagar servidores inactivos (24 horas ou mais de inactividade)
    private ServerResponse purgeServers() {

        final long currentTimestamp = Instant.now().getEpochSecond() - (3600 * 24);
        final Iterator<Map.Entry<Integer, ServerInfo>> serverIterator = servers.entrySet().iterator();

        while (serverIterator.hasNext()) {

            //---------------------------------------------------------------------------------
            // 1) Escolher um servidor vazio que não tenha sido atualizado nas últimas 24 horas
            //---------------------------------------------------------------------------------

            final Map.Entry<Integer, ServerInfo> currentEntry = serverIterator.next();
            final ServerInfo selectedServer = currentEntry.getValue();

            if (selectedServer.getLoad() > 0 || selectedServer.getTimestamp() > currentTimestamp) {
                continue;
            }

            int serverId = currentEntry.getKey();

            //---------------------------------------------------------------
            // 2) Notificar os restantes servidores da remoção desse servidor
            //---------------------------------------------------------------

            final DeleteServer deleteServer = new DeleteServer(serverId);

            servers.forEach((currentId, currentServer) -> {

                if (currentId != serverId) {

                    if (currentServer.isOnline()) {
                        myServerListener.sendServer(currentId, deleteServer);
                    }
                    else {
                        insertQueue(serverId, deleteServer);
                    }

                    currentServer.updateTimestamp();
                    serverDatabase.updateServer(currentServer);
                }
            });

            //---------------------------------------------
            // 2) Registar alterações efectuadas localmente
            //---------------------------------------------

            if (serverDatabase.deleteServerRooms(serverId)) {
                rooms.forEach((roomId, room) -> room.removeServer(serverId));
            }
            else {
                return ServerResponse.DatabaseError;
            }

            if (serverDatabase.deleteServer(serverId)) {
                servers.remove(serverId);
            }
            else {
                return ServerResponse.DatabaseError;
            }

            serverIterator.remove();
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

        //---------------------------------------------------------------
        // 2) Notificar os restantes servidores da remoção desse servidor
        //---------------------------------------------------------------

        final DeleteServer deleteServer = new DeleteServer(serverId);

        servers.forEach((currentId, currentServer) -> {

            if (currentId != serverId) {

                if (currentServer.isOnline()) {
                    myServerListener.sendServer(currentId, deleteServer);
                }
                else {
                    insertQueue(serverId, deleteServer);
                }

                currentServer.updateTimestamp();
                serverDatabase.updateServer(currentServer);
            }
        });

        //---------------------------------------------------
        // 3) Registar alterações efectuadas na base de dados
        //---------------------------------------------------

        if (serverDatabase.deleteServerRooms(serverId)) {
            rooms.forEach((roomId, room) -> room.removeServer(serverId));
        }
        else {
            return ServerResponse.DatabaseError;
        }

        if (serverDatabase.deleteServer(serverId)) {
            servers.remove(serverId);
        }
        else {
            return ServerResponse.DatabaseError;
        }

        return ServerResponse.SuccessResponse;
    }

    // TODO: testar algoritmo para apagar salas inactivas (24 horas ou mais de inactividade)
    private ServerResponse purgeRooms() {

        final long currentTimestamp = Instant.now().getEpochSecond() - (3600 * 24);
        final Iterator<Map.Entry<Integer, RoomInfo>> roomsIterator = rooms.entrySet().iterator();

        while (roomsIterator.hasNext()) {

            //------------------------------------------------------------------------------
            // 1) Escolher uma sala vazia que não tenha sido atualizada nas últimas 24 horas
            //------------------------------------------------------------------------------

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
                    myServerListener.sendServer(serverId, deleteRoom);
                }
                else {
                    insertQueue(roomId, deleteRoom);
                }

                currentServer.updateTimestamp();
                serverDatabase.updateServer(currentServer);
            }

            //----------------------------------------------------
            // 3) Registar remoção desse servidor na base de dados
            //----------------------------------------------------

            if (serverDatabase.deleteRoom(roomId)) {
                rooms.remove(roomId);
            }
            else {
                return ServerResponse.DatabaseError;
            }

            roomsIterator.remove();
        }

        return ServerResponse.SuccessResponse;
    }

    // TODO: testar algoritmo que envia comandos UpdateServer, DeleteRoom, UpdateRoom
    // TODO: para um servidor que estava offline quando estes foram gerados
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

            myServerListener.sendServer(serverId, serverQueue);
            messageQueue.remove(serverId);
        }
        else {
            selectedServer.setTimestamp(serverTimestamp);
        }

        selectedServer.setStatus(true);

        return ServerResponse.SuccessResponse;
    }

    final ServerResponse disconnectServer(int serverId) {

        final ServerInfo selectedServer = servers.get(serverId);

        if (selectedServer == null) {
            return ServerResponse.ServerNotFound;
        }

        selectedServer.setStatus(false);
        serverLogger.serverOffline(serverId);

        return ServerResponse.SuccessResponse;
    }

    private boolean informationChanged(final Object lhs, final Object rhs) {
        return !lhs.equals(rhs);
    }

    // TODO: verificar se todos os servidores recebem a notificação updateServer
    //       alterar porta HTTP de um servidor secundário, verificar se todos os restantes recebem a nova porta
    //       os secundários que estavam offline devem receber a mensagem assim que se reconectarem (@serverOnline)
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

        if (informationChanged(selectedServer.getAddress(), serverInfo.getAddress())) {
            selectedServer.setAddress(serverInfo.getAddress());
            sameInformation = false;
        }

        if (selectedServer.getPort() != serverInfo.getPort()) {
            selectedServer.setPort(serverInfo.getPort());
            sameInformation = false;
        }

        if (sameInformation) {
            return serverOnline(serverId, serverInfo.getTimestamp());
        }

        if (serverDatabase.updateServer(serverInfo)) {
            serverLogger.updateServer(serverId);
        }
        else {
            return ServerResponse.DatabaseError;
        }

        //---------------------------------------------------------------------------
        // 3) Notificar os restantes servidores das alterações efectuadas no servidor
        //---------------------------------------------------------------------------

        final UpdateServer updateServer = new UpdateServer(selectedServer);

        servers.forEach((currentId, currentServer) -> {

            if (currentId != serverId) {

                currentServer.updateTimestamp();
                serverDatabase.updateServer(currentServer);

                if (currentServer.isOnline()) {
                    myServerListener.sendServer(currentId, updateServer);
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
                myServerListener.sendServer(severId, userDisconnect);
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

    // TODO: implementar autenticação via facebook
    public ServerResponse userLogin(final String userEmail, final String userToken) {

        if (users.containsKey(userToken)) {
            return ServerResponse.SessionExists;
        }

        users.put(userToken, userEmail);


        return ServerResponse.SuccessResponse;
    }

    public boolean validateToken(final String userToken) {
        return users.containsKey(userToken);
    }
}