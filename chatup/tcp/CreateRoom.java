package chatup.tcp;

import java.io.Serializable;

public class CreateRoom implements Serializable
{
    public String roomName;
    public String roomPassword;
    public String userToken;
    public String userEmail;

    public CreateRoom()
    {
    }

    public CreateRoom(final String paramName, final String paramPassword, final String paramOwner, final String paramEmail)
    {
        roomName = paramName;
        roomPassword = paramPassword;
        userEmail = paramEmail;
        userToken = paramOwner;
    }
}