package chatup.http;

import chatup.main.ChatupGlobals;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import com.sun.net.httpserver.HttpExchange;

public class HeartbeatServiceHandler extends HttpDispatcher {

    HeartbeatServiceHandler(final HttpExchange httpExchange) {
        super(ChatupGlobals.HeartbeatServiceUrl, httpExchange);
    }

    @Override
    public void parseGetRequest(final String[] getValues) {
        sendHeartbeat(parseString(getValues[0], HttpFields.UserToken));
    }

    @Override
    public void parsePostRequest(final JsonValue jsonValue) {

        final JsonObject jsonObject = extractCommand(jsonValue, HttpCommands.SendMessage);

        if (jsonObject != null) {
            sendHeartbeat(jsonObject.getString(HttpFields.UserToken, null));
        }
        else {
            sendError(ServerResponse.InvalidCommand);
        }
    }

    @Override
    public void parsePutRequest(final JsonValue jsonValue) {

        final JsonObject jsonObject = extractCommand(jsonValue, HttpCommands.SendMessage);

        if (jsonObject != null) {
            sendHeartbeat(jsonObject.getString(HttpFields.UserToken, null));
        }
        else {
            sendError(ServerResponse.InvalidCommand);
        }
    }

    @Override
    public void parseDeleteRequest(final JsonValue jsonValue) {

        final JsonObject jsonObject = extractCommand(jsonValue, HttpCommands.SendMessage);

        if (jsonObject != null) {
            sendHeartbeat(jsonObject.getString(HttpFields.UserToken, null));
        }
        else {
            sendError(ServerResponse.InvalidCommand);
        }
    }

    private void sendHeartbeat(final String userToken) {

        if (userToken != null) {
            sendTextResponse(ServerResponse.SuccessResponse, "true");
        }
        else {
            sendError(ServerResponse.InvalidToken);
        }
    }
}