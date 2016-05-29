package chatup.http;

import chatup.main.ChatupGlobals;
import chatup.main.ChatupServer;
import chatup.server.AbstractServer;

import com.sun.net.httpserver.HttpExchange;

public class HeartbeatService extends HttpDispatcher {

    public HeartbeatService() {
        super(ChatupGlobals.HeartbeatServiceUrl);
    }

    @Override
    public void parseGetRequest(final HttpExchange httpExchange, final String[] jsonValue) {

        final String userToken = parseString(jsonValue, 0, HttpFields.UserToken);
        final AbstractServer serverInstance = ChatupServer.getInstance();

        if (userToken != null) {

            if (serverInstance.validateToken(userToken)) {
                sendTextResponse(httpExchange, ServerResponse.SuccessResponse, userToken);
            }
            else {
                sendError(httpExchange, ServerResponse.InvalidToken);
            }
        }
        else {
            sendError(httpExchange, ServerResponse.InvalidToken);
        }
    }
}