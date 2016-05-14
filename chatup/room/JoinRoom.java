package chatup.room;

import chatup.http.HttpCommands;
import chatup.http.HttpFields;
import chatup.http.HttpRequest;

import com.eclipsesource.json.Json;

public class JoinRoom extends HttpRequest
{
    public JoinRoom(int roomId, final String roomPassword, final String userToken)
    {
        super("POST", Json.object()
            .add(HttpCommands.JoinRoom, Json.object()
            .add(HttpFields.RoomId, roomId)
            .add(HttpFields.RoomPassword, roomPassword)
            .add(HttpFields.UserToken, userToken)).toString());
    }
}