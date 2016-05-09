package chatup.server;

import chatup.room.Room;
import chatup.user.Message;
import chatup.user.UserLogin;
import jdk.internal.cmm.SystemResourcePressureImpl;

import java.net.UnknownHostException;
import java.sql.*;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class Database{

    private static Database instance;
    private Connection dbConnection = null;

    // Database
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

    //Rooms
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
                    return new Room(rs.getString("name"), rs.getString("password"));
                }
            }
        }
        catch (SQLException ex) {
            return null;
        }

        return null;
    }

    public LinkedList<Room> getRooms() {

        final LinkedList<Room> myRooms = new LinkedList<>();

        try (final Statement stmt = dbConnection.createStatement();
             final ResultSet rs = stmt.executeQuery("SELECT * FROM Rooms")) {

            while (rs.next()) {
                myRooms.add(new Room(rs.getString("name"), rs.getString("password")));
            }
        }
        catch (SQLException ex) {
            return null;
        }

        return myRooms;
    }

    //Messages
    public boolean insertMessage(final Message paramMessage) {

        final String sqlQuery = "INSERT INTO Messages(room, token, epoch, message) VALUES(?, ?, ?, ?)";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {
            stmt.setInt(1, paramMessage.getRoom());
            stmt.setString(2, paramMessage.getSender());
            stmt.setLong(3, paramMessage.getTimestamp());
            stmt.setString(4, paramMessage.getMessage());
            stmt.executeUpdate();
        }
        catch (SQLException ex) {
            System.out.println(ex);
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

    public Message getMessage(int messageId){

        final String sqlQuery = "SELECT * FROM Messages WHERE id = ?";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {

            stmt.setInt(1, messageId);

            try (final ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Message(rs.getString("message"), rs.getString("token"), rs.getInt("room"), rs.getLong("epoch"));
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
                    myMessages.add(new Message(rs.getString("message"), rs.getString("token"), rs.getInt("room"), rs.getLong("epoch")));
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

    //Servers
    public boolean insertServer(final ServerInfo paramServer) {

        if (serverExists(paramServer.getId())) {
            return updateServer(paramServer);
        }

        final String sqlQuery = "INSERT INTO Servers(id, address, port) VALUES(?, ?, ?)";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {
            stmt.setInt(1, paramServer.getId());
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

    public boolean updateServer(final ServerInfo paramServer) {

        final String sqlQuery = "UPDATE Servers SET address = ?, port = ? WHERE id = ?";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {
            stmt.setString(1, paramServer.getAddress().getHostAddress().toString());
            stmt.setShort(2, paramServer.getPort());
            stmt.setInt(3, paramServer.getId());
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
                    return new ServerInfo(rs.getInt("id"), rs.getString("address"), rs.getShort("port"));
                }
            }
        }
        catch (SQLException ex) {
            return null;
        }

        return null;
    }


    public LinkedList<ServerInfo> getServers() throws UnknownHostException {

        final LinkedList<ServerInfo> myServers = new LinkedList<>();

        try (final Statement stmt = dbConnection.createStatement();
             final ResultSet rs = stmt.executeQuery("SELECT * FROM Servers")) {

            while (rs.next()) {
                myServers.add(new ServerInfo(rs.getInt("id"), rs.getString("address"), rs.getShort("port")));
            }
        }
        catch (SQLException ex) {
            return null;
        }

        return myServers;
    }


    //ServerRooms
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

    //Users
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

    public UserLogin getUserByEmail(String email) {

        final String sqlQuery = "SELECT * FROM Users WHERE email = ?";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {

            stmt.setString(1, email);

            try (final ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return new UserLogin(rs.getString("email"), rs.getString("token"));
                }
            }
        }
        catch (SQLException ex) {
            return null;
        }

        return null;
    }

    public UserLogin getUserByToken(String token) {

        final String sqlQuery = "SELECT * FROM Users WHERE token = ?";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {

            stmt.setString(1, token);

            try (final ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return new UserLogin(rs.getString("email"), rs.getString("token"));
                }
            }
        }
        catch (SQLException ex) {
            return null;
        }

        return null;
    }
}