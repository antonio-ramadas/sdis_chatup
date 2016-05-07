package chatup.room;

import chatup.rest.HttpRequest;
import com.eclipsesource.json.Json;

public class CreateRoom implements HttpRequest {

    private final String roomName;
    private final String roomPassword;
    private final String userToken;

    public CreateRoom(final String paramToken, final String paramName, final String paramPassword) {
        roomName = paramName;
        roomPassword = paramPassword;
        userToken = paramToken;
    }

    public final String getName() {
        return roomName;
    }

    public final String getPassword() {
        return roomPassword;
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
            .add("CreateRoom", Json.object()
            .add("token", userToken)
            .add("name", roomName)
            .add("password", roomPassword)).toString();
    }
}