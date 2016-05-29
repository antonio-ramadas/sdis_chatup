package chatup.http;

import chatup.main.ChatupGlobals;
import chatup.main.ChatupServer;
import chatup.model.Message;
import chatup.model.Room;
import chatup.server.AbstractServer;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;

import com.esotericsoftware.minlog.Log;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PushRequest {

    private HttpExchange httpExchange;
    private String userToken;
    private Room myRoom;

    public PushRequest(final Room paramRoom, final String paramToken, long paramTimestamp, final HttpExchange paramDispatcher) {
        httpExchange = paramDispatcher;
        lastUpdate = paramTimestamp;
        userToken = paramToken;
        myRoom = paramRoom;
    }

    public String getToken() {
        return userToken;
    }

    private long lastUpdate;

    public void send() {

        final JsonValue messagesArray = Json.array();
        final ArrayList<Message> myMessages = myRoom.getMessages(lastUpdate);
        final AbstractServer chatupInstance = ChatupServer.getInstance();

        for (int i = myMessages.size() - 1; i >= 0; i--) {

            final Message currentMessage = myMessages.get(i);

            messagesArray.asArray().add(Json.object()
                .add(HttpFields.MessageSender, currentMessage.getAuthor())
                .add(HttpFields.MessageTimestamp, currentMessage.getTimestamp())
                .add(HttpFields.MessageContents, currentMessage.getMessage())
                .add(HttpFields.MessageRoomId, currentMessage.getId()));
        }

        final Set<String> roomUsers = myRoom.getUsers();
        final ConcurrentHashMap<String, String> serverUsers = chatupInstance.getUsers();
        final JsonValue usersArray = Json.array();

        for (final String otherToken : roomUsers) {

            final String otherEmail = serverUsers.get(otherToken);

            if (otherEmail != null) {
                usersArray.asArray().add(otherEmail);
            }
        }

        sendJsonResponse(
            ServerResponse.SuccessResponse,
            Json.object().add("users", usersArray).add("messages", messagesArray)
        );
    }

    private void sendJsonResponse(final ServerResponse httpResponse, final JsonValue httpParameters) {

        if (httpResponse == ServerResponse.SuccessResponse) {

            try {
                sendResponse(
                    Json.object().add(HttpCommands.GenericSuccess, httpParameters).toString(),
                    HttpURLConnection.HTTP_OK
                );
            }
            catch (final IOException ex) {
                ChatupGlobals.warn(ChatupGlobals.MessageServiceUrl, ex);
            }
        }
        else {
            sendError(httpResponse);
        }
    }

    private void sendError(final ServerResponse serverResponse) {

        try {
            sendResponse(
                Json.object().add(HttpCommands.GenericError, serverResponse.toString()).toString(),
                HttpURLConnection.HTTP_OK
            );
        }
        catch (final IOException ex) {
            ChatupGlobals.warn(ChatupGlobals.MessageServiceUrl, ex);
        }
    }

    private void sendResponse(final String serverResponse, int statusCode) throws IOException {

        Log.info("[MessageService] sending response: " + serverResponse);
        httpExchange.sendResponseHeaders(statusCode, serverResponse.length());

        try (final OutputStream os = httpExchange.getResponseBody()) {
            os.write(serverResponse.getBytes());
            os.close();
        }
        catch (final IOException ex) {
            ChatupGlobals.warn(ChatupGlobals.MessageServiceUrl, ex);
        }
    }
}