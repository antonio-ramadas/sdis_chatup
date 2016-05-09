package chatup.backend;

import chatup.rest.RoomServiceHandler;
import chatup.rest.UserServiceHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class PrimaryDispatcher extends ServerDispatcher{

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {

        final String[] urlQuery = httpExchange.getRequestURI().getPath().split("/");

        switch (urlQuery[1]) {
        case "roomService":
            new RoomServiceHandler(httpExchange).processRequest();
            break;
        case "userService":
            new UserServiceHandler(httpExchange).processRequest();
            break;
        default:
            sendError(httpExchange, "invalidService");
            break;
        }
    }
}