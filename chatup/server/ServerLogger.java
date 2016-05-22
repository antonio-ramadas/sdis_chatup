package chatup.server;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ServerLogger {

    private BufferedWriter fileOutput;
    private BufferedWriter consoleOutput;

    ServerLogger(final Server paramServer) {

        final String serverName;

        if (paramServer.getType() == ServerType.PRIMARY) {
            serverName = "primary";
        }
        else {
            serverName = "secondary" + paramServer.getId();
        }

        if (createDirectory(serverName)) {

            final File fileObject = new File(serverName + generateFilename());

            try {
                fileOutput = new BufferedWriter(new FileWriter(fileObject));
            }
            catch (final IOException ex) {
                ex.printStackTrace();
            }
        }

        consoleOutput = new BufferedWriter(new OutputStreamWriter(System.out));
    }

    private boolean createDirectory(final String paramDirectory) {
        final File myFile = new File(paramDirectory);
        return !(!myFile.exists() || !myFile.isDirectory()) || myFile.mkdir();
    }

    protected void flush() {

        try {
            consoleOutput.flush();
            fileOutput.flush();
        }
        catch (final IOException ex) {
            ex.printStackTrace();
        }
    }

    private final String generateFilename() {
        return new SimpleDateFormat("'/'yyyy-MM-dd-HH_mm_ss'.log'").format(new Date());
    }

    private final String generateTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss' | '").format(new Date());
    }

    public void invalidCommand(final String commandName) {
        error(commandName + "received empty or invalid command!");
    }

    public void roomExists(final String roomName) {
        warning("Room #" + roomName + " already exists!");
    }

    public void roomNotFound(int roomId) {
        error("Room #" + roomId + " is not registered on this server!");
    }

    public void userConnected(final String userName) {
        information(userName + " has connected.");
    }

    public void alreadyJoined(int roomId, final String userToken) {
        warning("User" + userToken + " has already joined Room #" + roomId + "!");
    }

    public void roomInvalidToken(int roomId, final String userToken) {
        error("User " + userToken + " was not inside Room #" + roomId + "!");
    }

    public void sendBlock(int roomId) {
        information("Received message block from Room #" + roomId + "!");
    }

    public void sendMessage(int roomId) {
        information("Received new message from Room #" + roomId + "!");
    }

    public void serverOffline(int serverId) {
        information("Server " + serverId + " disconnected.");
    }

    public void serverNotFound(int serverId) {
        error("Server " + serverId + " not found in database!");
    }

    public void insertServer(int serverId) {
        information("Inserting server " + serverId + " into local database...");
    }

    public void deleteServer(int serverId) {
        warning("Server #" + serverId + " has been delete due to inactivity!");
    }

    public void serverOnline(int serverId, final String hostName) {
        information("Server " + serverId + " connected from " + hostName + ".");
    }

    public void userDisconnected(final String userName) {
        information(userName + " has logged out");
    }

    public void createRoom(final String userToken, final String roomName) {
        information("User " + userToken + " has created Room " + roomName + ".");
    }

    public void joinRoom(final String userName, int roomId) {
        information(userName + " has joined Room #" + roomId + ".");
    }

    public void deleteRoom(int roomId) {
        warning("Room #" + roomId + " has been deleted due to inactivity!");
    }

    public void leaveRoom(final String userToken, int roomId) {
        information("User " + userToken + " has left Room #" + roomId + ".");
    }

    private void warning(final String paramMessage) {
        write(consoleOutput, "[WARNING] " + paramMessage);
        write(fileOutput, String.format("%s | WARNING | %s", generateTimestamp(), paramMessage));
    }

    private void error(final String paramMessage) {
        write(consoleOutput, "[ERROR]" + paramMessage);
        write(fileOutput, String.format("%s | ERROR | %s", generateTimestamp(), paramMessage));
    }

    private void debug(final String paramMessage) {
        write(fileOutput, String.format("%s | DEBUG | %s", generateFilename(), paramMessage));
    }

    private void information(final String paramMessage) {
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

    public void removeUser(final String userToken) {
        information("User " + userToken + " is not connected to this server anymore!");
    }
}