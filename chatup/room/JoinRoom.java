package chatup.room;

import chatup.rest.HttpRequest;
import com.eclipsesource.json.Json;

public class JoinRoom implements HttpRequest {

    private final int roomId;
    private final String userToken;

    public JoinRoom(final String paramToken, int paramId) {
        roomId = paramId;
        userToken = paramToken;
    }

    public final int getRoom() {
        return roomId;
    }

    public final String getToken() {
        return userToken;
    }

    @Override
    public final String getMethod() {
        return "POST";
    }

    @Override
    public final String getMessage() {
        return Json.object()
            .add("JoinRoom", Json.object()
            .add("token", userToken)
            .add("id", roomId)).toString();
    }
}