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

        final String userToken = parseString(jsonValue[0], HttpFields.UserToken);
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