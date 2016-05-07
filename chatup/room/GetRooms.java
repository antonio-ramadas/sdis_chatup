package chatup.room;

import chatup.rest.HttpRequest;
import com.eclipsesource.json.Json;

public class GetRooms implements HttpRequest {

    public GetRooms() {}

    @Override
    public String getMethod() {
        return "GET";
    }

    @Override
    public String getMessage() {
        return Json.object().add("CreateRoom", Json.object()).toString();
    }
}