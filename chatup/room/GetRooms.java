package chatup.room;

import chatup.http.HttpCommands;
import chatup.http.HttpRequest;

import com.eclipsesource.json.Json;

public class GetRooms extends HttpRequest
{
    public GetRooms()
    {
        super("GET", Json.object().add(HttpCommands.CreateRoom, "").toString());
    }
}