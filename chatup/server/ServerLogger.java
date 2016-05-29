package chatup.server;

import chatup.main.ChatupGlobals;
import com.esotericsoftware.minlog.Log;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

class ServerLogger {

    private BufferedWriter fileOutput;
    private ServerType serverType;

    ServerLogger(final Server paramServer) {

        serverType = paramServer.getType();

        final String serverName;

        if (serverType  == ServerType.PRIMARY) {
            serverName = "primary";
        }
        else {
            serverName = "secondary-" + paramServer.getId();
        }

        if (ChatupGlobals.createDirectory(serverName)) {

            final File fileObject = new File(serverName + generateFilename());

            try {
                fileOutput = new BufferedWriter(new FileWriter(fileObject));
            }
            catch (final IOException ex) {
                ChatupGlobals.abort(serverType, ex);
            }
        }
    }

    private String generateFilename() {
        return new SimpleDateFormat("'/'yyyy-MM-dd-HH_mm_ss'.log'").format(new Date());
    }

    private String generateTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss' | '").format(new Date());
    }

    void invalidOperation(final Object paramObject) {
        error(paramObject.getClass().getSimpleName() + " received empty or invalid command!");
    }

    void roomExists(final String roomName) {
        warning("Room #" + roomName + " already exists!");
    }

    void roomInvalidToken(int roomId, final String userToken) {
        error("Received message from User #" + userToken + ", but user is currently not inside Room #" + roomId + "!");
    }

    void roomNotFound(int roomId) {
        error("Room #" + roomId + " is not registered on this server!");
    }

    void syncRoom(int roomId, int serverId) {
        information("Sending Room #" + roomId + " metadata to Server #" + serverId + "...");
    }

    void updateRoom(int roomId) {
        information("Received Room #" + roomId + " metadata from server.");
    }

    void userConnected(final String userName) {
        information(userName + " has connected.");
    }

    void alreadyJoined(int roomId, final String userToken) {
        warning("User #" + userToken + " has already joined Room #" + roomId + "!");
    }

    void insertMessage(int roomId) {
        information("Received Message #" + roomId + " metadata from server!");
    }

    void sendMessage(int roomId) {
        information("Sending message from Room #" + roomId + " to server!");
    }

    void serverOffline(int serverId) {
        information("Server #" + serverId + " disconnected.");
    }

    void serverOnline(int serverId, final String hostName) {
        information("Server #" + serverId + " connected from " + hostName + ".");
    }

    void serverNotFound(int serverId) {
        error("Server #" + serverId + " not registered on this server!");
    }

    void deleteServer(int serverId) {
        warning("Server #" + serverId + " has been delete due to inactivity!");
    }

    void updateServer(int serverId)  {
        information("Server #" + serverId + " information has been updated!");
    }

    void userDisconnected(final String userEmail) {
        information(userEmail + " has logged out");
    }

    void createRoom(final String userToken, final String roomName) {
        information(userToken + " has created Room " + roomName + ".");
    }

    void joinRoom(final String userEmail, int roomId) {
        information(userEmail + " has joined Room #" + roomId + ".");
    }

    void deleteRoom(int roomId) {
        warning("Room #" + roomId + " has been deleted due to inactivity!");
    }

    void leaveRoom(final String userEmail, int roomId) {
        information(userEmail + " has left Room #" + roomId + ".");
    }

    private void warning(final String paramMessage) {
        Log.warn(serverType.toString(), paramMessage);
        write(fileOutput, String.format("%s | WARNING | %s", generateTimestamp(), paramMessage));
    }

    private void error(final String paramMessage) {
        Log.error(serverType.toString(), paramMessage);
        write(fileOutput, String.format("%s | ERROR | %s", generateTimestamp(), paramMessage));
    }

    private void information(final String paramMessage) {
        Log.info(serverType.toString(), paramMessage);
        write(fileOutput, String.format("%s | INFORMATION | %s", generateFilename(), paramMessage));
    }

    private void write(final BufferedWriter buffer, final String message) {

        try {
            buffer.write(message);
            buffer.newLine();
        }
        catch (final IOException e) {
            e.printStackTrace();
        }
    }

    void userNotFound(final String userToken) {
        warning("User #" + userToken + " not registed on this server!");
    }

    void databaseError() {
        error("Database access error, could not save changes to disk!");
    }

    void removeUser(final String userToken) {
        information("User #" + userToken + " is not connected to this server anymore!");
    }

    void insertMirror(int roomId, int serverId) {
        information("Registering Server #" + serverId + " on Room #" + roomId);
    }

    void insertServer(int serverId) {
        information("Inserting server " + serverId + " into local database...");
    }

    void mirrorExists(int roomId, int serverId) {
        information("Server #" + serverId + " already registered on Room #" + roomId);
    }
}