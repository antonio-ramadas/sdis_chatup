package chatup.rest;

import chatup.main.ChatupServer;
import chatup.server.Server;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import com.sun.net.httpserver.HttpExchange;

public class UserServiceHandler extends HttpDispatcher {

    public UserServiceHandler(final HttpExchange httpExchange) {
        super(httpExchange);
    }

    @Override
    protected void parseGetRequest(JsonValue jsonValue) {
        sendError(ResponseMessages.msgInvalidMethod);
    }

    @Override
    protected void parsePostRequest(JsonValue jsonValue) {
        sendError(ResponseMessages.msgInvalidMethod);
    }

    @Override
    protected void parseDeleteRequest(final JsonValue jsonValue) {

        final Server serverInstance = ChatupServer.getInstance();
        final JsonObject jsonObject = extractCommand(jsonValue, "UserDisconnect");

        if (jsonObject != null) {

            final String userEmail = jsonObject.getString("email", null);
            final String userToken = jsonObject.getString("token", null);

            if (userEmail == null || userToken == null) {
                sendError(ResponseMessages.msgMissingParameters);
            }
            else if (serverInstance.userLogout(userEmail, userToken)) {
                sendSuccess("loggedOut");
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

        final Server serverInstance = ChatupServer.getInstance();
        final JsonObject jsonObject = extractCommand(jsonValue, "UserLogin");

        if (jsonObject != null) {

            final String userEmail = jsonObject.getString("email", null);
            final String userToken = jsonObject.getString("token", null);

            if (userEmail == null || userToken == null) {
                sendError(ResponseMessages.msgMissingParameters);
            }
            else if (serverInstance.userLogin(userEmail, userToken)) {
                sendSuccess("loggedIn");
            }
            else {
                sendError(ResponseMessages.msgOperationFailed);
            }
        }
        else {
            sendError(ResponseMessages.msgInvalidCommand);
        }
    }
}