package chatup.model;

import chatup.server.Server;
import chatup.server.ServerInfo;
import chatup.server.ServerType;

import java.sql.*;
import java.util.*;

public class Database {

    private static final String ServerAddress = "address";
    private static final String ServerId = "id";
    private static final String ServerPort = "port";
    private static final String ServerTimestamp = "timestamp";
    private static final String RoomId = "id";
    private static final String RoomName = "name";
    private static final String RoomOwner = "owner";
    private static final String RoomPassword = "password";
    private static final String MessageAuthor = "author";
    private static final String MessageContent = "contents";
    private static final String MessageRoom = "room";
    private static final String MessageTimestamp = "epoch";
    private static final String queryInsertRoom = "INSERT INTO Rooms(id, name, password, owner) VALUES(?, ?, ?, ?)";
    private static final String queryDeleteRoom = "DELETE FROM Rooms WHERE id = ?";
    private static final String querySelectRooms = "SELECT * FROM Rooms";

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

    public boolean insertRoom(int roomId, final Room paramRoom) {

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

    private final static String queryInsertMessage = "INSERT INTO Messages(room, author, epoch, contents) VALUES(?, ?, ?, ?)";
    private final static String querySelectMessagesByRoom = "SELECT * FROM Messages WHERE room = ? ORDER BY epoch DESC";

    public boolean insertMessage(final Message paramMessage) {

        try (final PreparedStatement stmt = dbConnection.prepareStatement(queryInsertMessage)) {
            stmt.setInt(1, paramMessage.getId());
            stmt.setString(2, paramMessage.getAuthor());
            stmt.setLong(3, paramMessage.getTimestamp());
            stmt.setString(4, paramMessage.getMessage());
            stmt.executeUpdate();
        }
        catch (SQLException ex) {
            return false;
        }

        return true;
    }

    public MessageCache getMessagesByRoom(int roomId) {

        final MessageCache myMessages = new MessageCache();

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

                    myMessages.push(newMessage);
                }
            }
        }
        catch (SQLException ex) {
            return null;
        }

        return myMessages;
    }

    private static final String queryInsertServer = "INSERT INTO Servers(id, address, port, timestamp) VALUES(?, ?, ?, ?)";
    private static final String queryUpdateServer = "UPDATE Servers SET address = ?, port = ?, timestamp = ? WHERE id = ?";
    private static final String queryDeleteServer = "DELETE FROM Servers WHERE id = ?";
    private static final String querySelectServers = "SELECT * FROM Servers";

    public boolean insertServer(final ServerInfo paramServer) {

        try (final PreparedStatement stmt = dbConnection.prepareStatement(queryInsertServer)) {
            stmt.setInt(1, paramServer.getId());
            stmt.setString(2, paramServer.getAddress());
            stmt.setInt(3, paramServer.getPort());
            stmt.setLong(4, paramServer.getTimestamp());
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

    public boolean updateServer(final ServerInfo serverInfo) {

        try (final PreparedStatement stmt = dbConnection.prepareStatement(queryUpdateServer)) {
            stmt.setString(1, serverInfo.getAddress());
            stmt.setInt(2, serverInfo.getPort());
            stmt.setLong(3, serverInfo.getTimestamp());
            stmt.setInt(4, serverInfo.getId());
            stmt.executeUpdate();
        }
        catch (SQLException ex) {
            return false;
        }

        return true;
    }

    public HashMap<Integer, ServerInfo> getServers() {

        final HashMap<Integer, ServerInfo> myServers = new HashMap<>();

        try (final Statement stmt = dbConnection.createStatement();
             final ResultSet rs = stmt.executeQuery(querySelectServers)) {

            while (rs.next()) {

                int serverId = rs.getInt(ServerId);

                final ServerInfo newServer = new ServerInfo(
                    serverId,
                    rs.getLong(ServerTimestamp),
                    rs.getString(ServerAddress),
                    rs.getInt(ServerPort)
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
    private static final String queryDeleteRoomServer = "DELETE FROM ServerRooms WHERE server = ? AND room = ?";
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
}