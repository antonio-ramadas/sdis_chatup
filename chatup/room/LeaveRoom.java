package chatup.room;

import chatup.http.HttpCommands;
import chatup.http.HttpFields;
import chatup.http.HttpMethod;
import chatup.http.HttpRequest;

import com.eclipsesource.json.Json;

public class LeaveRoom extends HttpRequest
{
    public LeaveRoom(int roomId, final String userToken)
    {
        super(HttpMethod.DELETE, Json.object()
            .add(HttpCommands.LeaveRoom, Json.object()
            .add(HttpFields.RoomId, roomId)
            .add(HttpFields.UserToken, userToken)).toString());
    }
}