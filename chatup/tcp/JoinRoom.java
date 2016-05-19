package chatup.tcp;

public class JoinRoom
{
    public String userToken;

    public JoinRoom()
    {
    }

    public JoinRoom(int paramId, final String paramToken)
    {
        roomId = paramId;
        userToken = paramToken;
    }

    public int roomId;
}