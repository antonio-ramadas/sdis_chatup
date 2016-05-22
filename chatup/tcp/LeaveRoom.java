package chatup.tcp;

import java.io.Serializable;

public class LeaveRoom implements Serializable
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