package chatup.server;

public class ServerMessage {

    public static String replaceServer(int serverId, final String newIp, short newPort) {
        return String.join(" ", new String[]{"ReplaceServer", Integer.toString(serverId), newIp, Short.toString(newPort)}) + "\r\n";
    }

    public static String deleteServer(int serverId) {
        return String.join(" ", new String[]{"DeleteServer", Integer.toString(serverId)}) + "\r\n";
    }

    public static String createRoom(final String roomName, final String roomPassword) {
        return String.join(" ", new String[]{"CreateRoom", roomName, roomPassword}) + "\r\n";
    }

    public static String joinRoom(int roomId, final String userToken) {
        return String.join(" ", new String[]{"JoinRoom", Integer.toString(roomId), userToken}) + "\r\n";
    }

    public static String leaveRoom(int roomId, final String userToken) {
        return String.join(" ", new String[]{"LeaveRoom", Integer.toString(roomId), userToken}) + "\r\n";
    }

    public static String userDisconnect(final String userToken, final String userEmail) {
        return String.join(" ", new String[]{"UserDisconnect", userToken, userEmail}) + "\r\n";
    }
}