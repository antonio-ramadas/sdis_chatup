package chatup.backend;

import java.net.InetAddress;

public class ServerMessage {

    public static String generateReplaceServer(final InetAddress oldIp, final InetAddress newIp) {
        return String.join(" ", new String[]{"ReplaceServer", oldIp.getHostAddress(), newIp.getHostAddress()}) + "\r\n";
    }

    public static String createRoom(final String roomName, final String roomPassword) {
        return String.join(" ", new String[]{"CreateRoom", roomName, roomPassword}) + "\r\n";
    }

    public static String deleteRoom(final String roomName, final String userToken) {
        return String.join(" ", new String[]{"DeleteRoom", roomName, userToken}) + "\r\n";
    }

    public static String registerUser(final String userToken, final String userEmail, int roomId) {
        return String.join(" ", new String[]{"ResgisterUser", userToken, userEmail, Integer.toString(roomId)}) + "\r\n";
    }

    public static String removeUser(final String userToken, int roomId) {
        return String.join(" ", new String[]{"RemoveUser", userToken, Integer.toString(roomId)}) + "\r\n";
    }
}