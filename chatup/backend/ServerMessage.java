package chatup.backend;

import java.net.InetAddress;

public class ServerMessage {

    private ServerMessage instance;

    private ServerMessage() {}

    public final ServerMessage getInstance() {

        if (instance == null) {
            instance = new ServerMessage();
        }

        return instance;
    }

    public final String generateReplace(final InetAddress oldIp, final InetAddress newIp) {
        return String.join(" ", new String[]{"REPLACE", oldIp.getHostAddress(), newIp.getHostAddress()}) + "\r\n";
    }

    public final String generateCreateRoom(final String roomName, final String roomPassword) {
        return String.join(" ", new String[]{"ROOM", roomName, roomPassword}) + "\r\n";
    }

    public final String generateUser(final String token, final String email, int roomId) {
        return String.join(" ", new String[]{"USER", token, email, Integer.toString(roomId)}) + "\r\n";
    }
}