package chatup.model;

import chatup.server.ServerInfo;

import java.sql.*;
import java.net.UnknownHostException;
import java.util.*;

public class Database {

    private static Database instance;
    private Connection dbConnection = null;

    private Database() throws SQLException {
        dbConnection = DriverManager.getConnection("jdbc:sqlite:test.db");
        dbConnection.setAutoCommit(true);
    }

    public static Database getInstance() throws SQLException {

        if (instance == null) {
            instance = new Database();
        }

        return instance;
    }

    /**********************
     * ROOMS
     **********************/
    public boolean insertRoom(int roomId, final Room paramRoom) {

        final String sqlQuery = "INSERT INTO Rooms(id, name, password) VALUES(?, ?, ?)";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {
            stmt.setInt(1, roomId);
            stmt.setString(2, paramRoom.getName());
            stmt.setString(3, paramRoom.getPassword());
            stmt.executeUpdate();
        }
        catch (SQLException ex) {
            return false;
        }

        return true;
    }

    public boolean deleteRoom(int roomId) {

        final String sqlQuery = "DELETE FROM Rooms WHERE id = ?";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {
            stmt.setInt(1, roomId);
            stmt.executeUpdate();
        }
        catch (SQLException ex) {
            return false;
        }

        return true;
    }

    public final Room getRoom(int roomId) {

        final String sqlQuery = "SELECT * FROM Rooms WHERE id = ?";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {

            stmt.setInt(1, roomId);

            try (final ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {

                    final Room newRoom = new Room(
                        rs.getString(DatabaseFields.RoomName),
                        rs.getString(DatabaseFields.RoomPassword)
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
             final ResultSet rs = stmt.executeQuery("SELECT * FROM Rooms")) {

            while (rs.next()) {

                final Room newRoom = new Room(
                    rs.getString(DatabaseFields.RoomName),
                    rs.getString(DatabaseFields.RoomPassword)
                );

                myRooms.put(rs.getInt(DatabaseFields.RoomId), newRoom);
            }
        }
        catch (SQLException ex) {
            return null;
        }

        return myRooms;
    }

    /**********************
     * MESSAGES
     **********************/
    public boolean insertMessage(final Message paramMessage) {

        final String sqlQuery = "INSERT INTO Messages(room, token, epoch, message) VALUES(?, ?, ?, ?)";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {
            stmt.setInt(1, paramMessage.getRoomId());
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

        final String sqlQuery = "DELETE FROM Messages WHERE id = ?";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {
            stmt.setInt(1, messageId);
            stmt.executeUpdate();
        }
        catch (SQLException ex) {
            return false;
        }

        return true;
    }

    public Message getMessage(int messageId) {

        final String sqlQuery = "SELECT * FROM Messages WHERE id = ?";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {

            stmt.setInt(1, messageId);

            try (final ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return new Message(rs.getInt("room"), rs.getString("token"), rs.getLong("epoch"), rs.getString("message")
                    );
                }
            }

        }
        catch (SQLException ex) {
            return null;
        }

        return null;
    }

    public LinkedList<Message> getMessagesByRoomId(int roomId) {

        final LinkedList<Message> myMessages = new LinkedList<>();
        final String sqlQuery = "SELECT * FROM Messages WHERE room = ? ORDER BY epoch DESC";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {

            stmt.setInt(1, roomId);

            try (final ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    myMessages.add(new Message(rs.getInt("room"), rs.getString("token"), rs.getLong("epoch"), rs.getString("message")
                    ));
                }
            }
        }
        catch (SQLException ex) {
            return null;
        }

        return myMessages;
    }

    /*
    public LinkedList<Message> getLimitedMessagesByRoomId(int roomId, int messagesQuantity) {

        final LinkedList<Message> myMessages = new LinkedList<>();
        final String sqlQuery = "SELECT * FROM Message WHERE room = ? ORDER BY epoch DESC LIMIT " + messagesQuantity;
        System.out.println(sqlQuery);

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {

            stmt.setInt(1, roomId);

            try (final ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    myMessages.add(new Message(rs.getString("message"), rs.getString("token"), rs.getInt("room"), rs.getLong("epoch")));
                }
            }
        }
        catch (SQLException ex) {
            return null;
        }
        return myMessages;
    }*/

    /**********************
     * SERVERS
     **********************/
    public boolean insertServer(int serverId, final ServerInfo paramServer) {

        if (serverExists(serverId)) {
            return updateServer(serverId, paramServer);
        }

        final String sqlQuery = "INSERT INTO Servers(id, address, port) VALUES(?, ?, ?)";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {
            stmt.setInt(1, serverId);
            stmt.setString(2, paramServer.getAddress().getHostAddress());
            stmt.setShort(3, paramServer.getPort());
            stmt.executeUpdate();
        }
        catch (SQLException ex) {
            return false;
        }

        return true;
    }

    public boolean deleteServer(int serverId) {

        final String sqlQuery = "DELETE FROM Servers WHERE id = ?";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {
            stmt.setInt(1, serverId);
            stmt.executeUpdate();
        }
        catch (SQLException ex) {
            return false;
        }

        return true;
    }

    private boolean serverExists(int serverId) {

        final String sqlQuery = "SELECT * FROM Servers WHERE id = ?";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {

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

    public boolean updateServer(int serverId, final ServerInfo paramServer) {

        final String sqlQuery = "UPDATE Servers SET address = ?, port = ? WHERE id = ?";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {
            stmt.setString(1, paramServer.getAddress().getHostAddress().toString());
            stmt.setShort(2, paramServer.getPort());
            stmt.setInt(3, serverId);
            stmt.executeUpdate();
        }
        catch (SQLException ex) {
            return false;
        }

        return true;
    }

    public final ServerInfo getServer(int serverId) throws UnknownHostException {

        try (final PreparedStatement stmt = dbConnection.prepareStatement("SELECT * FROM Servers WHERE id = ?")) {

            stmt.setInt(1, serverId);

            try (final ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {

                    return new ServerInfo(
                        rs.getString(DatabaseFields.ServerAddress),
                        rs.getShort(DatabaseFields.ServerPort)
                    );
                }
            }
        }
        catch (SQLException ex) {
            return null;
        }

        return null;
    }

    public HashMap<Integer, ServerInfo> getServers() throws UnknownHostException {

        final HashMap<Integer, ServerInfo> myServers = new HashMap<>();

        try (final Statement stmt = dbConnection.createStatement();
             final ResultSet rs = stmt.executeQuery("SELECT * FROM Servers")) {

            while (rs.next()) {

                final ServerInfo newServer = new ServerInfo(
                    rs.getString(DatabaseFields.ServerAddress),
                    rs.getShort(DatabaseFields.ServerPort)
                );

                myServers.put(rs.getInt(DatabaseFields.ServerId), newServer);
            }
        }
        catch (SQLException ex) {
            return null;
        }

        return myServers;
    }

    /**********************
     * SERVER ROOMS
     **********************/
    public boolean insertServerRooms(int serverId, int roomId) {

        final String sqlQuery = "INSERT INTO ServerRooms(server, room) VALUES(?, ?)";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {
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

        final String sqlQuery = "DELETE FROM ServerRooms WHERE server = ? AND room = ?";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {
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
        final String sqlQuery = "SELECT * FROM ServerRooms WHERE room = ?";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {

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

    /**********************
     * USERS
     **********************/
    public boolean insertUser(String token, String email) {

        final String sqlQuery = "INSERT INTO Users(token, email) VALUES(?, ?)";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {
            stmt.setString(1, token);
            stmt.setString(2, email);
            stmt.executeUpdate();
        }
        catch (SQLException ex) {
            return false;
        }

        return true;
    }

    public boolean deleteUser(String token) {

        final String sqlQuery = "DELETE FROM Users WHERE token = ?";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {
            stmt.setString(1, token);
            stmt.executeUpdate();
        }
        catch (SQLException ex) {
            return false;
        }

        return true;
    }

    private boolean userExists(String token) {

        final String sqlQuery = "SELECT * FROM Users WHERE token = ?";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {

            stmt.setString(1, token);

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

    public boolean updateUser(String token, String newToken) {

        final String sqlQuery = "UPDATE Users SET token = ? WHERE token = ?";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {
            stmt.setString(1, newToken);
            stmt.setString(2, token);
            stmt.executeUpdate();
        }
        catch (SQLException ex) {
            return false;
        }

        return true;
    }

    public Map.Entry<String, String> getUserByEmail(String email) {

        final String sqlQuery = "SELECT * FROM Users WHERE email = ?";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {

            stmt.setString(1, email);

            try (final ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {

                    return new AbstractMap.SimpleEntry<>(
                        rs.getString(DatabaseFields.UserToken),
                        rs.getString(DatabaseFields.UserEmail)
                    );
                }
            }
        }
        catch (SQLException ex) {
            return null;
        }

        return null;
    }

    public Map.Entry<String, String> getUserByToken(String token) {

        final String sqlQuery = "SELECT * FROM Users WHERE token = ?";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {

            stmt.setString(1, token);

            try (final ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {

                    return new AbstractMap.SimpleEntry<>(
                        rs.getString(DatabaseFields.UserToken),
                        rs.getString(DatabaseFields.UserEmail)
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