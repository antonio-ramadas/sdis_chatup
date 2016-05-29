package chatup.http;

import chatup.main.ChatupGlobals;
import chatup.main.ChatupServer;
import chatup.server.AbstractServer;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import com.sun.net.httpserver.HttpExchange;

import java.time.Instant;

public class MessageService extends HttpDispatcher {

    public MessageService() {
        super(ChatupGlobals.MessageServiceUrl);
    }

    @Override
    public void parseGetRequest(final HttpExchange httpExchange, final String[] getValues) {

        final AbstractServer serverInstance = ChatupServer.getInstance();
        final String userToken = parseString(getValues, 1, HttpFields.MessageSender);
        int roomId = parseInteger(getValues, 0, HttpFields.MessageRoomId);
        long roomTimestamp = parseLong(getValues, 2, HttpFields.MessageTimestamp);

        if (userToken == null || roomId < 0) {
            sendError(httpExchange, ServerResponse.MissingParameters);
        }
        else {

            final ServerResponse serverResponse = serverInstance.getMessages(
                httpExchange,
                userToken,
                roomId,
                roomTimestamp
            );

            if (serverResponse != ServerResponse.SuccessResponse) {
                sendError(httpExchange, serverResponse);
            }
        }
    }

    @Override
    public void parsePostRequest(final HttpExchange httpExchange, final JsonValue jsonValue) {

        final AbstractServer serverInstance = ChatupServer.getInstance();
        final JsonObject jsonObject = extractCommand(jsonValue, HttpCommands.SendMessage);

        if (jsonObject != null) {

            int roomId = jsonObject.getInt(HttpFields.MessageRoomId, -1);
            final String userToken = jsonObject.getString(HttpFields.MessageSender, null);
            final String messageBody = jsonObject.getString(HttpFields.MessageContents, null);

            if (roomId < 0 || userToken == null || userToken.isEmpty()) {
                sendError(httpExchange, ServerResponse.MissingParameters);
            }
            else if (messageBody == null || messageBody.isEmpty()) {
                sendError(httpExchange, ServerResponse.MissingParameters);
            }
            else {
                sendJsonResponse(
                    httpExchange,
                    serverInstance.sendMessage(roomId, userToken, messageBody), jsonObject
                    .add(HttpFields.MessageTimestamp, Instant.now().toEpochMilli())
                    .add(HttpFields.UserEmail, serverInstance.getEmail(userToken))
                );
            }
        }
        else {
            sendError(httpExchange, ServerResponse.InvalidOperation);
        }
    }
}