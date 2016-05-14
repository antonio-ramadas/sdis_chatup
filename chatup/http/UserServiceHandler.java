package chatup.http;

import chatup.main.ChatupGlobals;
import chatup.main.ChatupServer;
import chatup.server.Server;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import com.sun.net.httpserver.HttpExchange;

public class UserServiceHandler extends HttpDispatcher {

    public UserServiceHandler(final HttpExchange httpExchange) {
        super(ChatupGlobals.UserServiceUrl, httpExchange);
    }

    @Override
    public void parsePostRequest(JsonValue jsonValue) {

        final Server serverInstance = ChatupServer.getInstance();
        final JsonObject jsonObject = extractCommand(jsonValue, HttpCommands.UserLogin);

        if (jsonObject != null) {

            final String userEmail = jsonObject.getString(HttpFields.UserEmail, null);
            final String userToken = jsonObject.getString(HttpFields.UserToken, null);

            if (userEmail == null || userToken == null) {
                sendError(HttpResponses.MissingParameters);
            }
            else if (serverInstance.userLogin(userEmail, userToken)) {
                sendJsonSuccess(jsonValue);
            }
            else {
                sendError(HttpResponses.OperationFailed);
            }
        }
        else {
            sendError(HttpResponses.InvalidCommand);
        }
    }

    @Override
    public void parseDeleteRequest(final JsonValue jsonValue) {

        final Server serverInstance = ChatupServer.getInstance();
        final JsonObject jsonObject = extractCommand(jsonValue, HttpCommands.UserDisconnect);

        if (jsonObject != null) {

            final String userEmail = jsonObject.getString(HttpFields.UserEmail, null);
            final String userToken = jsonObject.getString(HttpFields.UserToken, null);

            if (userEmail == null || userToken == null) {
                sendError(HttpResponses.MissingParameters);
            }
            else if (serverInstance.userLogout(userEmail, userToken)) {
                sendJsonSuccess(jsonValue);
            }
            else {
                sendError(HttpResponses.OperationFailed);
            }
        }
        else {
            sendError(HttpResponses.InvalidCommand);
        }
    }
}