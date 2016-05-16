package chatup.tcp;

public class UserDisconnect
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