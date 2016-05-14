package chatup.room;

import chatup.http.HttpCommands;
import chatup.http.HttpFields;
import chatup.http.HttpRequest;

import com.eclipsesource.json.Json;

public class DeleteRoom extends HttpRequest
{
    public DeleteRoom(int roomId, final String userToken)
    {
        super("DELETE", Json.object()
            .add(HttpCommands.DeleteRoom, Json.object()
            .add(HttpFields.RoomId, roomId)
            .add(HttpFields.UserToken, userToken)).toString());
    }
}