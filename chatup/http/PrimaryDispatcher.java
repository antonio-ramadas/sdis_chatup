package chatup.http;

import chatup.main.ChatupGlobals;

import com.sun.net.httpserver.HttpExchange;

public class PrimaryDispatcher extends ServerDispatcher {

    @Override
    public void handle(HttpExchange httpExchange) {

        final String[] urlQuery = httpExchange.getRequestURI().getPath().split("/");

        switch (urlQuery[1]) {
        case ChatupGlobals.RoomServiceUrl:
            new RoomServiceHandler(httpExchange).processRequest();
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