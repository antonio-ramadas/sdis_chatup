package chatup.http;

import chatup.main.ChatupGlobals;
import chatup.main.ChatupServer;
import chatup.server.AbstractServer;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import com.sun.net.httpserver.HttpExchange;

public class UserService extends HttpDispatcher {

    public UserService() {
        super(ChatupGlobals.UserServiceUrl);
    }

    @Override
    public void parsePostRequest(final HttpExchange httpExchange, final JsonValue jsonValue) {

        final AbstractServer serverInstance = ChatupServer.getInstance();
        final JsonObject jsonObject = extractCommand(jsonValue, HttpCommands.UserLogin);

        if (jsonObject != null) {

            final String userToken = jsonObject.getString(HttpFields.UserToken, null);

            if (userToken == null || userToken.isEmpty()) {
                sendError(httpExchange, ServerResponse.MissingParameters);
            }
            else {
                sendJsonResponse(
                    httpExchange,
                    serverInstance.userLogin(userToken),
                    jsonObject.add("email", serverInstance.getEmail(userToken))
                );
            }
        }
        else {
            sendError(httpExchange, ServerResponse.InvalidOperation);
        }
    }

    @Override
    public void parseDeleteRequest(final HttpExchange httpExchange, final JsonValue jsonValue) {

        final AbstractServer serverInstance = ChatupServer.getInstance();
        final JsonObject jsonObject = extractCommand(jsonValue, HttpCommands.UserDisconnect);

        if (jsonObject != null) {

            final String userEmail = jsonObject.getString(HttpFields.UserEmail, null);
            final String userToken = jsonObject.getString(HttpFields.UserToken, null);

            if (userEmail == null || userEmail.isEmpty()) {
                sendError(httpExchange, ServerResponse.MissingParameters);
            }
            else if (userToken == null || userToken.isEmpty()) {
                sendError(httpExchange, ServerResponse.MissingParameters);
            }
            else {
                sendJsonResponse(httpExchange, serverInstance.userDisconnect(userEmail, userToken), jsonObject);
            }
        }
        else {
            sendError(httpExchange, ServerResponse.InvalidOperation);
        }
    }
}