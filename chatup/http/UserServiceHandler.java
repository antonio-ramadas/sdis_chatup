package chatup.http;

import chatup.main.ChatupGlobals;
import chatup.main.ChatupServer;
import chatup.server.Server;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import com.sun.net.httpserver.HttpExchange;

class UserServiceHandler extends HttpDispatcher {

    UserServiceHandler(final HttpExchange httpExchange) {
        super(ChatupGlobals.UserServiceUrl, httpExchange);
    }

    @Override
    public void parsePostRequest(JsonValue jsonValue) {

        final Server serverInstance = ChatupServer.getInstance();
        final JsonObject jsonObject = extractCommand(jsonValue, HttpCommands.UserLogin);

        if (jsonObject != null) {

            final String userEmail = jsonObject.getString(HttpFields.UserEmail, null);
            final String userToken = jsonObject.getString(HttpFields.UserToken, null);

            if (userEmail == null || userEmail.isEmpty()) {
                sendError(ServerResponse.MissingParameters);
            }
            else if (userToken == null || userToken.isEmpty()) {
                sendError(ServerResponse.MissingParameters);
            }
            else {
                sendJsonResponse(serverInstance.userLogin(userEmail, userToken), jsonObject);
            }
        }
        else {
            sendError(ServerResponse.InvalidOperation);
        }
    }

    @Override
    public void parseDeleteRequest(final JsonValue jsonValue) {

        final Server serverInstance = ChatupServer.getInstance();
        final JsonObject jsonObject = extractCommand(jsonValue, HttpCommands.UserDisconnect);

        if (jsonObject != null) {

            final String userEmail = jsonObject.getString(HttpFields.UserEmail, null);
            final String userToken = jsonObject.getString(HttpFields.UserToken, null);

            if (userEmail == null || userEmail.isEmpty()) {
                sendError(ServerResponse.MissingParameters);
            }
            else if (userToken == null || userToken.isEmpty()) {
                sendError(ServerResponse.MissingParameters);
            }
            else {
                sendJsonResponse(serverInstance.userDisconnect(userEmail, userToken), jsonObject);
            }
        }
        else {
            sendError(ServerResponse.InvalidOperation);
        }
    }
}