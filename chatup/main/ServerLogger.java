package chatup.main;

import chatup.model.Room;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ServerLogger {

    private static ServerLogger instance;

    private BufferedWriter fileOutput;
    private Path filePath;
    private BufferedWriter consoleOutput;

    private final boolean createDirectory(final String paramDirectory) {

        final File myFile = new File(paramDirectory);

        if (!myFile.exists() || !myFile.isDirectory()) {
            return myFile.mkdir();
        }

        return true;
    }

    public void flush() {

        try {
            consoleOutput.flush();
            fileOutput.flush();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final String generateFilename() {
        return new SimpleDateFormat("'/'yyyy-MM-dd-HH_mm_ss'.log'").format(new Date());
    }

    private final String generateTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss' |'").format(new Date());
    }

    private ServerLogger(final String serverName) {

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

    public void userConnected(final String userName) {
        information(userName + " has connected.");
    }

    public void userConnectionLost(final String userName) {
        information(userName + " timed out and was disconnected from the server.");
    }

    public void userDisconnected(final String userName) {
        information(userName + " has logged out");
    }

    public void userJoinedRoom(final String userName, final Room roomObject) {
        information(userName + " has joined room \"" + roomObject.getName() + "\"");
    }

    public void userLeftRoom(final String userName, final Room roomObject) {
        information(userName + " has left room \"" + roomObject.getName() + "\"");
    }

    public void warning(final String paramMessage) {
        write(consoleOutput, "[WARNING] " + paramMessage);
        write(fileOutput, String.format("%s | WARNING | %s", generateTimestamp(), paramMessage));
    }

    public void error(final String paramMessage) {
        write(consoleOutput, "[ERROR]" + paramMessage);
        write(fileOutput, String.format("%s | ERROR | %s", generateTimestamp(), paramMessage));
    }

    public void debug(final String paramMessage) {
        write(fileOutput, String.format("%s | DEBUG | %s", generateFilename(), paramMessage));
    }

    public void information(final String paramMessage) {
        write(fileOutput, String.format("%s | INFORMATION | %s", generateFilename(), paramMessage));
    }

    private void write(final BufferedWriter buffer, final String message) {

        try {
            buffer.write(message);
            buffer.newLine();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ServerLogger getInstance(final String serverName) {

        if (instance == null) {
            instance = new ServerLogger(serverName);
        }

        return instance;
    }
}