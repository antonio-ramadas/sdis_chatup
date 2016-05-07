package chatup.user;

import chatup.rest.HttpRequest;
import com.eclipsesource.json.Json;

public class UserLogin implements HttpRequest {

    private String userToken;
    private String userEmail;

    public UserLogin(final String paramEmail, final String paramToken) {
        userToken = paramToken;
        userEmail = paramEmail;
    }

    public final String getEmail() {
        return userEmail;
    }

    public final String getToken() {
        return userToken;
    }

    @Override
    public final String getMethod() {
        return "PUT";
    }

    @Override
    public final String getMessage() {
        return Json.object()
            .add("UserLogin", Json.object()
            .add("email", userEmail)
            .add("token", userToken)).toString();
    }
}