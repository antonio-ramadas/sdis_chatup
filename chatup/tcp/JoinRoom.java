package chatup.tcp;

import java.io.Serializable;

public class JoinRoom implements Serializable
{
    public String userEmail;
    public String userToken;

    public JoinRoom()
    {
    }

    public JoinRoom(int paramId, final String paramEmail, final String paramToken)
    {
        roomId = paramId;
        userEmail = paramEmail;
        userToken = paramToken;
    }

    public int roomId;
}