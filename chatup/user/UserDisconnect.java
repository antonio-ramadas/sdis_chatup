package chatup.user;

import chatup.http.HttpCommands;
import chatup.http.HttpFields;
import chatup.http.HttpRequest;
import com.eclipsesource.json.Json;

public class UserDisconnect extends HttpRequest
{
    public UserDisconnect(final String userEmail, final String userToken)
    {
        super("DELETE", Json.object()
            .add(HttpCommands.UserDisconnect, Json.object()
            .add(HttpFields.UserEmail, userEmail)
            .add(HttpFields.UserToken, userToken)).toString());
    }
}