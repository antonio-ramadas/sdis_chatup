package chatup.tcp;

public class LeaveRoom implements TcpMessage {

    private final String userToken;

    public LeaveRoom(int paramId, final String paramToken) {
        roomId = paramId;
        userToken = paramToken;
    }

    private int roomId;

    public int getRoomId() {
        return roomId;
    }

    public final String getToken() {
        return userToken;
    }

    @Override
    public TcpCommand getType() {
        return TcpCommand.LeaveRoom;
    }
}