package chatup.tcp;

public class LeaveRoom
{
    public String userToken;

    public LeaveRoom()
    {
    }

    public LeaveRoom(int paramId, final String paramToken)
    {
        roomId = paramId;
        userToken = paramToken;
    }

    public int roomId;
}