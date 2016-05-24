package chatup.http;

import chatup.main.ChatupGlobals;
import chatup.main.ChatupServer;
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
        final String userToken = parseString(getValues[1], HttpFields.UserToken);
        int roomId = parseInteger(getValues[0], HttpFields.RoomId);

        if (userToken == null || roomId < 0) {
            sendError(ServerResponse.MissingParameters);
        }
        else {

            final JsonValue jsonObject = serverInstance.getMessages(userToken, roomId);

            if (jsonObject != null) {
                sendJsonResponse(ServerResponse.SuccessResponse, jsonObject);
            }
            else {
                sendError(ServerResponse.OperationFailed);
            }
        }
    }

    @Override
    public void parsePostRequest(final JsonValue jsonValue) {

        final Server serverInstance = ChatupServer.getInstance();
        final JsonObject jsonObject = extractCommand(jsonValue, HttpCommands.SendMessage);

        if (jsonObject != null) {

            int roomId = jsonObject.getInt(HttpFields.RoomId, -1);
            final String userToken = jsonObject.getString(HttpFields.UserToken, null);
            final String messageBody = jsonObject.getString(HttpFields.MessageContents, null);
            //final Message userMessage = new Message(roomId, userToken, Instant.now().toEpochMilli(), messageBody);

            if (roomId < 0 || userToken == null || messageBody == null) {
                sendError(ServerResponse.MissingParameters);
            }
            else {
                sendJsonResponse(ServerResponse.SuccessResponse, jsonObject.add(HttpFields.MessageTimestamp, Instant.now().toEpochMilli()));
            }
        }
        else {
            sendError(ServerResponse.InvalidCommand);
        }
    }
}