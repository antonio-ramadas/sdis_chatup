package chatup.http;

import chatup.main.ChatupGlobals;
import chatup.main.ChatupServer;
import chatup.server.Server;
import chatup.model.Message;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import com.sun.net.httpserver.HttpExchange;

class MessageServiceHandler extends HttpDispatcher {

    MessageServiceHandler(final HttpExchange httpExchange) {
        super(ChatupGlobals.MessageServiceUrl, httpExchange);
    }

    @Override
    public void parseGetRequest(final String[] getValues) {

        final Server serverInstance = ChatupServer.getInstance();
        final String userToken = parseString(getValues[0], HttpFields.UserToken);
        int roomId = parseInteger(getValues[1], HttpFields.RoomId);

        if (userToken == null || roomId < 0) {
            sendError(ServerResponse.MissingParameters);
        }
        else {

            final Message[] userMessages = (Message[]) serverInstance.getMessages(userToken, roomId).getArray();

            if (userMessages != null) {

                final JsonValue jsonObject = Json.array();

                for (final Message userMessage : userMessages) {
                    jsonObject.asArray().add(userMessage.getId());
                }

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
            final Message userMessage = new Message(roomId, userToken, messageBody);

            if (roomId < 0 || userToken == null || messageBody == null) {
                sendError(ServerResponse.MissingParameters);
            }
            else {
                sendTextResponse(serverInstance.insertMessage(userMessage), HttpCommands.SendMessage);
            }
        }
        else {
            sendError(ServerResponse.InvalidCommand);
        }
    }
}