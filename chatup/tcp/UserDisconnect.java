package chatup.tcp;

public class UserDisconnect implements TcpMessage{

    private final String userEmail;
    private final String userToken;

    public UserDisconnect(final String paramToken, final String paramEmail) {
        userEmail = paramEmail;
        userToken = paramToken;
    }

    public final String getEmail() {
        return userEmail;
    }

    public final String getToken() {
        return userToken;
    }

    @Override
    public TcpCommand getType() {
        return TcpCommand.UserDisconnect;
    }
}