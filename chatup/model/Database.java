package chatup.model;

import chatup.server.Server;
import chatup.server.ServerInfo;
import chatup.server.ServerType;

import java.sql.*;
import java.util.*;

public class Database {

    private static final String ServerId = "id";
    private static final String ServerAddress = "address";
    private static final String ServerPort = "port";
    private static final String UserEmail = "email";
    private static final String UserToken = "token";
    private static final String RoomId = "id";
    private static final String RoomName = "name";
    private static final String RoomOwner = "owner";
    private static final String RoomPassword = "password";
    private static final String MessageContent = "contents";
    private static final String MessageAuthor = "author";
    private static final String MessageTimestamp = "epoch";
    private static final String MessageRoom = "room";

    public Database(final Server paramServer) throws SQLException {

        final ServerType serverType = paramServer.getType();

        if (serverType == ServerType.PRIMARY) {
            dbConnection = DriverManager.getConnection("jdbc:sqlite:primary.db");
        }
        else {
            dbConnection = DriverManager.getConnection(
                "jdbc:sqlite:" +
                paramServer.getType() + "-" +
                paramServer.getId() + ".db"
            );
        }

        dbConnection.setAutoCommit(true);
    }

    private final Connection dbConnection;

    private static final String queryInsertRoom = "INSERT INTO Rooms(id, name, password, owner) VALUES(?, ?, ?, ?)";
    private static final String querySelectRoomById = "SELECT * FROM Rooms WHERE id = ?";
    private static final String queryDeleteRoom = "DELETE FROM Rooms WHERE id = ?";
    private static final String querySelectRooms = "SELECT * FROM Rooms";

    public boolean insertRoom(int roomId, final RoomInfo paramRoom) {

        try (final PreparedStatement stmt = dbConnection.prepareStatement(queryInsertRoom)) {
            stmt.setInt(1, roomId);
            stmt.setString(2, paramRoom.getName());
            stmt.setString(3, paramRoom.getPassword());
            stmt.setString(4, paramRoom.getOwner());
            stmt.executeUpdate();
        }
        catch (SQLException ex) {
            return false;
        }

        return true;
    }

    public boolean deleteRoom(int roomId) {

        try (final PreparedStatement stmt = dbConnection.prepareStatement(queryDeleteRoom)) {
            stmt.setInt(1, roomId);
            stmt.executeUpdate();
        }
        catch (SQLException ex) {
            return false;
        }

        return true;
    }

    public final Room getRoom(int roomId) {

        try (final PreparedStatement stmt = dbConnection.prepareStatement(querySelectRoomById)) {

            stmt.setInt(1, roomId);

            try (final ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {

                    final Room newRoom = new Room(
                        rs.getString(RoomName),
                        rs.getString(RoomPassword),
                        rs.getString(RoomOwner)
                    );

                    return newRoom;
                }
            }
        }
        catch (SQLException ex) {
            return null;
        }

        return null;
    }

    public HashMap<Integer, Room> getRooms() {

        final HashMap<Integer, Room> myRooms = new HashMap<>();

        try (final Statement stmt = dbConnection.createStatement();
             final ResultSet rs = stmt.executeQuery(querySelectRooms)) {

            while (rs.next()) {

                final Room newRoom = new Room(
                    rs.getString(RoomName),
                    rs.getString(RoomPassword),
                    rs.getString(RoomOwner)
                );

                myRooms.put(rs.getInt(RoomId), newRoom);
            }
        }
        catch (SQLException ex) {
            return null;
        }

        if (myRooms.isEmpty()) {
            return null;
        }

        return myRooms;
    }

    public HashMap<Integer, RoomInfo> getRoomInformation() {

        final HashMap<Integer, RoomInfo> myRooms = new HashMap<>();

        try (final Statement stmt = dbConnection.createStatement();
             final ResultSet rs = stmt.executeQuery(querySelectRooms)) {

            while (rs.next()) {

                final RoomInfo newRoom = new RoomInfo(
                    rs.getString(RoomName),
                    rs.getString(RoomPassword),
                    rs.getString(RoomOwner)
                );

                myRooms.put(rs.getInt(RoomId), newRoom);
            }
        }
        catch (SQLException ex) {
            return null;
        }

        if (myRooms.isEmpty()) {
            return null;
        }

        return myRooms;
    }

    private final static String queryInsertMessage = "INSERT INTO Messages(room, token, epoch, message) VALUES(?, ?, ?, ?)";
    private final static String querySelectMessagesByRoomLimit = "SELECT * FROM Message WHERE room = ? ORDER BY epoch DESC LIMIT ";
    private final static String querySelectMessagesByRoom = "SELECT * FROM Messages WHERE room = ? ORDER BY epoch DESC";
    private final static String queryDeleteMessage = "DELETE FROM Messages WHERE id = ?";

    public boolean insertMessage(final Message paramMessage) {

        try (final PreparedStatement stmt = dbConnection.prepareStatement(queryInsertMessage)) {
            stmt.setInt(1, paramMessage.getId());
            stmt.setString(2, paramMessage.getSender());
            stmt.setLong(3, paramMessage.getTimestamp());
            stmt.setString(4, paramMessage.getMessage());
            stmt.executeUpdate();
        }
        catch (SQLException ex) {
            return false;
        }

        return true;
    }

    public boolean deleteMessage(int messageId) {

        try (final PreparedStatement stmt = dbConnection.prepareStatement(queryDeleteMessage)) {
            stmt.setInt(1, messageId);
            stmt.executeUpdate();
        }
        catch (SQLException ex) {
            return false;
        }

        return true;
    }

    public LinkedList<Message> getMessagesByRoom(int roomId) {

        final LinkedList<Message> myMessages = new LinkedList<>();

        try (final PreparedStatement stmt = dbConnection.prepareStatement(querySelectMessagesByRoom)) {

            stmt.setInt(1, roomId);

            try (final ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {

                    final Message newMessage = new Message(
                        rs.getInt(MessageRoom),
                        rs.getString(MessageAuthor),
                        rs.getLong(MessageTimestamp),
                        rs.getString(MessageContent)
                    );

                    myMessages.add(newMessage);
                }
            }
        }
        catch (SQLException ex) {
            return null;
        }

        return myMessages;
    }

    public LinkedList<Message> getMessagesByRoom(int roomId, int messagesQuantity) {

        final LinkedList<Message> myMessages = new LinkedList<>();
        final String sqlQuery = querySelectMessagesByRoomLimit + messagesQuantity;

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {

            stmt.setInt(1, roomId);

            try (final ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {

                    final Message newMessage = new Message(
                        rs.getInt(MessageRoom),
                        rs.getString(MessageAuthor),
                        rs.getLong(MessageTimestamp),
                        rs.getString(MessageContent)
                    );

                    myMessages.add(newMessage);;
                }
            }
        }
        catch (SQLException ex) {
            return null;
        }

        return myMessages;
    }

    private static final String queryInsertServer = "INSERT INTO Servers(id, address, port) VALUES(?, ?, ?)";
    private static final String queryUpdateServer = "UPDATE Servers SET address = ?, port = ? WHERE id = ?";;
    private static final String querySelectServerById = "SELECT * FROM Servers WHERE id = ?";
    private static final String queryDeleteServer = "DELETE FROM Servers WHERE id = ?";
    private static final String querySelectServers = "SELECT * FROM Servers";

    public boolean insertServer(int serverId, final String serverAddress, int serverPort) {

        if (serverExists(serverId)) {
            return updateServer(serverId, serverAddress, serverPort);
        }

        try (final PreparedStatement stmt = dbConnection.prepareStatement(queryInsertServer)) {
            stmt.setInt(1, serverId);
            stmt.setString(2, serverAddress);
            stmt.setInt(3, serverPort);
            stmt.executeUpdate();
        }
        catch (SQLException ex) {
            return false;
        }

        return true;
    }

    public boolean deleteServer(int serverId) {

        try (final PreparedStatement stmt = dbConnection.prepareStatement(queryDeleteServer)) {
            stmt.setInt(1, serverId);
            stmt.executeUpdate();
        }
        catch (SQLException ex) {
            return false;
        }

        return true;
    }

    public boolean updateServer(int serverId, final String serverAddress, int serverPort) {

        try (final PreparedStatement stmt = dbConnection.prepareStatement(queryUpdateServer)) {
            stmt.setString(1, serverAddress);
            stmt.setInt(2, serverPort);
            stmt.setInt(3, serverId);
            stmt.executeUpdate();
        }
        catch (SQLException ex) {
            return false;
        }

        return true;
    }

    private boolean serverExists(int serverId) {

        try (final PreparedStatement stmt = dbConnection.prepareStatement(querySelectServerById)) {

            stmt.setInt(1, serverId);

            try (final ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return true;
                }
            }
        }
        catch (SQLException ex) {
            return false;
        }

        return false;
    }

    public final HashMap<Integer, ServerInfo> getServers() {

        final HashMap<Integer, ServerInfo> myServers = new HashMap<>();

        try (final Statement stmt = dbConnection.createStatement();
             final ResultSet rs = stmt.executeQuery(querySelectServers)) {

            while (rs.next()) {

                int serverId = rs.getInt(ServerId);

                final ServerInfo newServer = new ServerInfo(
                    rs.getString(ServerAddress),
                    rs.getShort(ServerPort)
                );

                myServers.put(serverId, newServer);
            }
        }
        catch (SQLException ex) {
            return null;
        }

        if (myServers.isEmpty()) {
            return null;
        }

        return myServers;
    }

    private static final String queryInsertRoomServer = "INSERT INTO ServerRooms(server, room) VALUES(?, ?)";
    private static final String queryDeleteRoomServer = "DELETE FROM ServerRooms WHERE server = ? AND room = ?";;
    private static final String querySelectServersByRoom = "SELECT * FROM ServerRooms WHERE room = ?";

    public boolean insertServerRoom(int serverId, int roomId) {

        try (final PreparedStatement stmt = dbConnection.prepareStatement(queryInsertRoomServer)) {
            stmt.setInt(1, serverId);
            stmt.setInt(2, roomId);
            stmt.executeUpdate();
        }
        catch (SQLException ex) {
            return false;
        }

        return true;
    }

    public boolean deleteServerRoom(int serverId, int roomId) {

        try (final PreparedStatement stmt = dbConnection.prepareStatement(queryDeleteRoomServer)) {
            stmt.setInt(1, serverId);
            stmt.setInt(2, roomId);
            stmt.executeUpdate();
        }
        catch (SQLException ex) {
            return false;
        }

        return true;
    }

    public Set<Integer> getServerByRoom(int roomId) {

        final HashSet<Integer> myServers = new HashSet<>();

        try (final PreparedStatement stmt = dbConnection.prepareStatement(querySelectServersByRoom)) {

            stmt.setInt(1, roomId);

            try (final ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    myServers.add(rs.getInt("server"));
                }
            }
        }
        catch (SQLException ex) {
            return null;
        }

        return myServers;
    }

    private static final String queryInsertUser = "INSERT INTO Users(token, email) VALUES(?, ?)";
    private static final String queryUpdateUser = "UPDATE Users SET token = ? WHERE token = ?";
    private static final String queryDeleteUser = "DELETE FROM Users WHERE token = ?";
    private static final String querySelectUserById = "SELECT * FROM Users WHERE token = ?";
    private static final String querySelectUserByEmail = "SELECT * FROM Users WHERE email = ?";

    public boolean insertUser(String token, String email) {

        try (final PreparedStatement stmt = dbConnection.prepareStatement(queryInsertUser)) {
            stmt.setString(1, token);
            stmt.setString(2, email);
            stmt.executeUpdate();
        }
        catch (SQLException ex) {
            return false;
        }

        return true;
    }

    public boolean deleteUser(final String userToken) {

        try (final PreparedStatement stmt = dbConnection.prepareStatement(queryDeleteUser)) {
            stmt.setString(1, userToken);
            stmt.executeUpdate();
        }
        catch (SQLException ex) {
            return false;
        }

        return true;
    }

    private boolean userExists(final String userToken) {

        try (final PreparedStatement stmt = dbConnection.prepareStatement(querySelectUserById)) {

            stmt.setString(1, userToken);

            try (final ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return true;
                }
            }
        }
        catch (SQLException ex) {
            return false;
        }

        return false;
    }

    public boolean updateUser(final String oldToken, final String newToken) {

        try (final PreparedStatement stmt = dbConnection.prepareStatement(queryUpdateUser)) {
            stmt.setString(1, newToken);
            stmt.setString(2, oldToken);
            stmt.executeUpdate();
        }
        catch (SQLException ex) {
            return false;
        }

        return true;
    }

    public Map.Entry<String, String> getUserByEmail(String email) {

        try (final PreparedStatement stmt = dbConnection.prepareStatement(querySelectUserByEmail)) {

            stmt.setString(1, email);

            try (final ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {

                    return new AbstractMap.SimpleEntry<>(
                        rs.getString(UserToken),
                        rs.getString(UserEmail)
                    );
                }
            }
        }
        catch (SQLException ex) {
            return null;
        }

        return null;
    }

    public Map.Entry<String, String> getUserByToken(final String userToken) {

        try (final PreparedStatement stmt = dbConnection.prepareStatement(querySelectUserById)) {

            stmt.setString(1, userToken);

            try (final ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {

                    return new AbstractMap.SimpleEntry<>(
                        rs.getString(UserToken),
                        rs.getString(UserEmail)
                    );
                }
            }
        }
        catch (SQLException ex) {
            return null;
        }

        return null;
    }
}