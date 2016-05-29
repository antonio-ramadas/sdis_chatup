package chatup.http;

import chatup.main.ChatupGlobals;
import chatup.main.ChatupServer;
import chatup.model.Message;
import chatup.server.Server;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import com.sun.net.httpserver.HttpExchange;

import java.time.Instant;

class MessageServiceHandler extends HttpDispatcher {

    MessageServiceHandler(final HttpExchange httpExchange) {
        super(ChatupGlobals.MessageServiceUrl, httpExchange);
    }

    @Override
    public void parseGetRequest(final String[] getValues) {

        final Server serverInstance = ChatupServer.getInstance();
        final String userToken = parseString(getValues, 1, HttpFields.MessageSender);
        int roomId = parseInteger(getValues, 0, HttpFields.MessageRoomId);
        long roomTimestamp = parseLong(getValues, 2, HttpFields.MessageTimestamp);

        if (userToken == null || roomId < 0) {
            sendError(ServerResponse.MissingParameters);
        }
        else {

            final ServerResponse serverResponse = serverInstance.getMessages(this, userToken,roomId,roomTimestamp);

            if (serverResponse != ServerResponse.SuccessResponse) {
                sendError(serverResponse);
            }
        }
    }

    @Override
    public void parsePostRequest(final JsonValue jsonValue) {

        final Server serverInstance = ChatupServer.getInstance();
        final JsonObject jsonObject = extractCommand(jsonValue, HttpCommands.SendMessage);

        if (jsonObject != null) {

            int roomId = jsonObject.getInt(HttpFields.MessageRoomId, -1);
            final String userToken = jsonObject.getString(HttpFields.MessageSender, null);
            final String messageBody = jsonObject.getString(HttpFields.MessageContents, null);

            if (roomId < 0 || userToken == null || userToken.isEmpty()) {
                sendError(ServerResponse.MissingParameters);
            }
            else if (messageBody == null || messageBody.isEmpty()) {
                sendError(ServerResponse.MissingParameters);
            }
            else {
                sendJsonResponse(
                    serverInstance.sendMessage(roomId, userToken, messageBody),
                    jsonObject.add(HttpFields.MessageTimestamp, Instant.now().toEpochMilli())
                );
            }
        }
        else {
            sendError(ServerResponse.InvalidOperation);
        }
    }
}