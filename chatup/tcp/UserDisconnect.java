package chatup.tcp;

import java.io.Serializable;

public class UserDisconnect implements Serializable
{
    public String userEmail;
    public String userToken;

    public UserDisconnect()
    {
    }

    public UserDisconnect(final String paramToken, final String paramEmail)
    {
        userEmail = paramEmail;
        userToken = paramToken;
    }
}