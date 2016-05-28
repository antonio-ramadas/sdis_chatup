package chatup.http;

import chatup.main.ChatupServer;
import chatup.model.Message;
import chatup.model.Room;
import chatup.server.Server;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class PushRequest {

    private HttpDispatcher httpDispatcher;
    private String userToken;
    private Room myRoom;

    public PushRequest(final Room paramRoom, final String paramToken, long paramTimestamp, final HttpDispatcher paramDispatcher) {
        httpDispatcher = paramDispatcher;
        lastUpdate = paramTimestamp;
        userToken = paramToken;
        myRoom = paramRoom;
    }

    public String getToken()
    {
        return userToken;
    }

    private long lastUpdate;

    public void send() {

        final JsonValue messagesArray = Json.array();
        final ArrayList<Message> myMessages = myRoom.getMessages(lastUpdate);
        final Server chatupInstance = ChatupServer.getInstance();

        for (int i = myMessages.size() - 1; i >= 0; i--) {

            final Message currentMessage = myMessages.get(i);

            messagesArray.asArray().add(Json.object()
                .add(HttpFields.MessageSender, currentMessage.getToken())
                .add(HttpFields.MessageTimestamp, currentMessage.getTimestamp())
                .add(HttpFields.MessageContents, currentMessage.getMessage())
                .add(HttpFields.MessageRoomId, currentMessage.getId()));
        }

        final Set<String> roomUsers = myRoom.getUsers();
        final HashMap<String, String> serverUsers = chatupInstance.getUsers();
        final JsonValue usersArray = Json.array();

        for (final String otherToken : roomUsers) {

            final String otherEmail = serverUsers.get(otherToken);

            if (otherEmail != null) {
                usersArray.asArray().add(otherEmail);
            }
        }

        httpDispatcher.sendJsonResponse(
            ServerResponse.SuccessResponse,
            Json.object().add("users", usersArray).add("messages", messagesArray)
        );
    }
}