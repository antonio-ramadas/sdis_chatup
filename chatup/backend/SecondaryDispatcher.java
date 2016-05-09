package chatup.backend;

import chatup.rest.MessageServiceHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;

public class SecondaryDispatcher extends ServerDispatcher {

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {

        final String[] urlQuery = httpExchange.getRequestURI().getPath().split("/");

        switch (urlQuery[1]) {
        case "MessageService":
            new MessageServiceHandler(httpExchange).processRequest();
            break;
        default:
            sendError(httpExchange, "invalidService");
            break;
        }
    }
}