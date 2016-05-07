package chatup.room;

import chatup.rest.HttpRequest;

import com.eclipsesource.json.Json;

public class DeleteRoom implements HttpRequest{

    private final int roomId;
    private final String userToken;

    public DeleteRoom(final String paramToken, int paramId) {
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
    public String getMethod() {
        return "DELETE";
    }

    @Override
    public String getMessage() {
        return Json.object()
            .add("DeleteRoom", Json.object()
            .add("token", userToken)
            .add("id", roomId)).toString();
    }
}