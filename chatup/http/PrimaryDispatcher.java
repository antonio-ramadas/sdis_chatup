package chatup.http;

import chatup.main.ChatupGlobals;

import com.sun.net.httpserver.HttpExchange;

public class PrimaryDispatcher extends ServerDispatcher {

    @Override
    public void handle(final HttpExchange httpExchange) {

        final String[] urlQuery = httpExchange.getRequestURI().getPath().split("/");

        if (urlQuery.length != 2) {
            sendError(httpExchange, ServerResponse.InvalidService);
        }
        else {

            switch (urlQuery[1]) {
            case ChatupGlobals.RoomServiceUrl:
                new RoomServiceHandler(httpExchange).processRequest();
                break;
            case ChatupGlobals.HeartbeatServiceUrl:
                new HeartbeatServiceHandler(httpExchange).processRequest();
                break;
            case ChatupGlobals.UserServiceUrl:
                new UserServiceHandler(httpExchange).processRequest();
                break;
            default:
                sendError(httpExchange, ServerResponse.InvalidService);
                break;
            }
        }
    }
}