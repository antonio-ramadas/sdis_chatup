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

    public final String generateReplaceServer(final InetAddress oldIp, final InetAddress newIp) {
        return String.join(" ", new String[]{"ReplaceServer", oldIp.getHostAddress(), newIp.getHostAddress()}) + "\r\n";
    }

    public final String generateCreateRoom(final String roomName, final String roomPassword) {
        return String.join(" ", new String[]{"CreateRoom", roomName, roomPassword}) + "\r\n";
    }

    public final String generateDeleteRoom(final String roomName, final String userToken) {
        return String.join(" ", new String[]{"DeleteRoom", roomName, userToken}) + "\r\n";
    }

    public final String generateRegisterUser(final String userToken, final String userEmail, int roomId) {
        return String.join(" ", new String[]{"ResgisterUser", userToken, userEmail, Integer.toString(roomId)}) + "\r\n";
    }

    public final String generateRemoveUser(final String userToken, int roomId) {
        return String.join(" ", new String[]{"RemoveUser", userToken, Integer.toString(roomId)}) + "\r\n";
    }
}