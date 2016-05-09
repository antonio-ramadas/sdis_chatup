package chatup.rest;

import chatup.main.ChatupServer;
import chatup.server.Server;
import chatup.user.UserMessage;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import com.sun.net.httpserver.HttpExchange;

public class MessageServiceHandler extends HttpDispatcher {

    public MessageServiceHandler(final HttpExchange httpExchange) {
        super(httpExchange);
    }

    @Override
    protected void parseGetRequest(final String[] getValues) {

        final Server serverInstance = ChatupServer.getInstance();
        final String userToken = parseString(getValues[0], "token");
        int roomId = parseInteger(getValues[1], "id");

        if (userToken == null || roomId == -1) {
            sendError(ResponseMessages.msgMissingParameters);
        }
        else {

            final UserMessage[] userMessages = serverInstance.retrieveMessages(userToken, roomId);

            if (userMessages == null) {

                final JsonValue jsonObject = Json.array();

                for (int i = 0; i < userMessages.length; i++) {
                    jsonObject.asArray().add(userMessages[i].getRoomId());
                }

                sendSuccess(jsonObject.toString());
            }
            else {
                sendError(ResponseMessages.msgOperationFailed);
            }
        }
    }

    @Override
    protected void parsePostRequest(final JsonValue jsonValue) {

        final Server serverInstance = ChatupServer.getInstance();
        final JsonObject jsonObject = extractCommand(jsonValue, "SendMessage");

        if (jsonObject != null) {

            int roomId = jsonObject.getInt("roomId", -1);
            final String userToken = jsonObject.getString("token", null);
            final String msgContents = jsonObject.getString("message", null);

            if (roomId == -1 || userToken == null || msgContents == null) {
                sendError(ResponseMessages.msgMissingParameters);
            }
            else if (serverInstance.registerMessage(userToken, roomId, msgContents)) {
                sendSuccess("messageSent");
            }
            else {
                sendError(ResponseMessages.msgOperationFailed);
            }
        }
        else {
            sendError(ResponseMessages.msgInvalidCommand);
        }
    }

    @Override
    protected void parsePutRequest(final JsonValue jsonValue) {
        sendError(ResponseMessages.msgInvalidMethod);
    }

    @Override
    protected void parseDeleteRequest(final JsonValue jsonValue) {
        sendError(ResponseMessages.msgInvalidMethod);
    }
}