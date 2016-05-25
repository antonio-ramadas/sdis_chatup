package chatup.http;

import chatup.main.ChatupGlobals;

import chatup.main.ChatupServer;
import chatup.server.Server;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import com.sun.net.httpserver.HttpExchange;

class HeartbeatServiceHandler extends HttpDispatcher {

    HeartbeatServiceHandler(final HttpExchange httpExchange) {
        super(ChatupGlobals.HeartbeatServiceUrl, httpExchange);
    }

    @Override
    public void parseGetRequest(final String[] jsonValue) {
        sendHeartbeat(parseString(jsonValue[0], HttpFields.UserToken));
    }

    @Override
    public void parsePostRequest(final JsonValue jsonValue) {

        final JsonObject jsonObject = extractCommand(jsonValue, HttpCommands.SendMessage);

        if (jsonObject != null) {
            sendHeartbeat(jsonObject.getString(HttpFields.UserToken, null));
        }
        else {
            sendError(ServerResponse.InvalidOperation);
        }
    }

    @Override
    public void parsePutRequest(final JsonValue jsonValue) {

        final JsonObject jsonObject = extractCommand(jsonValue, HttpCommands.SendMessage);

        if (jsonObject != null) {
            sendHeartbeat(jsonObject.getString(HttpFields.UserToken, null));
        }
        else {
            sendError(ServerResponse.InvalidOperation);
        }
    }

    @Override
    public void parseDeleteRequest(final JsonValue jsonValue) {

        final JsonObject jsonObject = extractCommand(jsonValue, HttpCommands.SendMessage);

        if (jsonObject != null) {
            sendHeartbeat(jsonObject.getString(HttpFields.UserToken, null));
        }
        else {
            sendError(ServerResponse.InvalidOperation);
        }
    }

    private void sendHeartbeat(final String userToken) {

        final Server serverInstance = ChatupServer.getInstance();

        if (userToken != null) {

            if (serverInstance.validateToken(userToken)) {
                sendTextResponse(ServerResponse.SuccessResponse, userToken);
            }
            else {
                sendError(ServerResponse.InvalidToken);
            }
        }
        else {
            sendError(ServerResponse.InvalidToken);
        }
    }
}