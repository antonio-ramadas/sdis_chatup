package chatup.http;

import chatup.main.ChatupGlobals;
import com.sun.net.httpserver.HttpExchange;

public class SecondaryDispatcher extends ServerDispatcher {

    @Override
    public void handle(HttpExchange httpExchange) {

        final String[] urlQuery = httpExchange.getRequestURI().getPath().split("/");

        switch (urlQuery[1]) {
        case ChatupGlobals.MessageServiceUrl:
            new MessageServiceHandler(httpExchange).processRequest();
            break;
        default:
            sendError(httpExchange, ServerResponse.InvalidService);
            break;
        }
    }
}