package chatup.user;

import chatup.rest.HttpRequest;
import com.eclipsesource.json.Json;

public class UserDisconnect implements HttpRequest {

    private String userToken;

    public UserDisconnect(final String paramUrl, final String paramEmail, final String paramToken) {
        userToken = paramToken;
    }

    public final String getToken() {
        return userToken;
    }

    @Override
    public final String getMethod() {
        return "DELETE";
    }

    @Override
    public final String getMessage() {
        return Json.object()
            .add("UserDisconnect", Json.object()
            .add("token", userToken)).toString();
    }
}