package chatup.user;

import chatup.http.HttpCommands;
import chatup.http.HttpFields;
import chatup.http.HttpRequest;
import com.eclipsesource.json.Json;

public class UserLogin extends HttpRequest
{
    public UserLogin(final String userEmail, final String userToken)
    {
        super("POST", Json.object()
            .add(HttpCommands.UserLogin, Json.object()
            .add(HttpFields.UserEmail, userEmail)
            .add(HttpFields.UserToken, userToken)).toString());
    }
}