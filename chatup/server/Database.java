package chatup.server;

import chatup.room.Room;
import chatup.user.Message;

import java.net.UnknownHostException;
import java.sql.*;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class Database{

    private static Database instance;
    private Connection dbConnection = null;

    private Database() throws SQLException {
        dbConnection = DriverManager.getConnection("jdbc:sqlite:test.db");
        dbConnection.setAutoCommit(true);
    }

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

    public boolean insertMessage(final Message paramMessage) {

        final String sqlQuery = "INSERT INTO Messsages(id, room, username, epoch, message) VALUES(DEFAULT, ?, ?, ?, ?)";

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

    public boolean deleteMessage(final Message paramMessage) {

        return true;
    }

    public boolean insertServer(final ServerInfo paramServer) {

        if (serverExists(paramServer.getId())) {
            return updateServer(paramServer);
        }

        final String sqlQuery = "INSERT INTO Servers(id, address, port) VALUES(?, ?, ?)";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {
            stmt.setInt(1, paramServer.getId());
            stmt.setString(2, paramServer.getAddress().getHostAddress());
            stmt.setShort(3, paramServer.getTcpPort());
            stmt.executeUpdate();
        }
        catch (SQLException ex) {
            return false;
        }

        return true;
    }

    private boolean updateServer(final ServerInfo paramServer) {

        final String sqlQuery = "UPDATE Servers SET address = ?, port = ? WHERE id = ?";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {
            stmt.setString(1, paramServer.getAddress().getHostAddress());
            stmt.setShort(2, paramServer.getTcpPort());
            stmt.setInt(3, paramServer.getId());
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

    public Set<Integer> getRoomsByServer(int serverId) {

        final HashSet<Integer> myRooms = new HashSet<>();
        final String sqlQuery = "SELECT * FROM ServerRooms WHERE server = ?";

        try (final PreparedStatement stmt = dbConnection.prepareStatement(sqlQuery)) {

            stmt.setInt(1, serverId);

            try (final ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    myRooms.add(rs.getInt("room"));
                }
            }
        }
        catch (SQLException ex) {
            return null;
        }

        return myRooms;
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

    public LinkedList<ServerInfo> getServers() throws UnknownHostException {

        final LinkedList<ServerInfo> myServers = new LinkedList<>();

        try (final Statement stmt = dbConnection.createStatement();
             final ResultSet rs = stmt.executeQuery("SELECT * FROM Rooms")) {

            while (rs.next()) {
                myServers.add(new ServerInfo(rs.getInt("id"), rs.getString("address"), rs.getShort("port")));
            }
        }
        catch (SQLException ex) {
            return null;
        }

        return myServers;
    }

    public static Database getInstance() throws SQLException {

        if (instance == null) {
            instance = new Database();
        }

        return instance;
    }
}