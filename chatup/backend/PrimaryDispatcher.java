package chatup.backend;

import chatup.rest.RoomMessageHandler;
import chatup.rest.UserMessageHandler;
import com.eclipsesource.json.Json;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;

public class PrimaryDispatcher extends ServerDispatcher {

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {

        final String path = httpExchange.getRequestURI().getPath();
        final String[] paths = path.split("/");

        switch (paths[1]) {
        case "roomService":
            handleRoomMessage(httpExchange, httpExchange.getRequestBody());
            break;
        case "userService":
            handleUserMessage(httpExchange, httpExchange.getRequestBody());
            break;
        default:
            sendError(httpExchange, "invalidService");
            break;
        }
    }

    private boolean sendError(final HttpExchange httpExchange, final String requestCommand) {

        try {
            sendResponse(httpExchange, Json.object().add("error", requestCommand).toString(), 404);
        }
        catch (IOException ex) {
            return false;
        }

        return true;
    }

    private boolean sendSuccess(final HttpExchange httpExchange, final String requestCommand) {

        try {
            sendResponse(httpExchange, Json.object().add("success", requestCommand).toString(), 200);
        }
        catch (IOException ex) {
            return false;
        }

        return true;
    }

    private void handleRoomMessage(final HttpExchange httpExchange, final InputStream requestBody) {
        new RoomMessageHandler(httpExchange, parseRequestBody(requestBody)).processRequest();
    }

    private void handleUserMessage(HttpExchange httpExchange, InputStream requestBody) {
        new UserMessageHandler(httpExchange, parseRequestBody(requestBody)).processRequest();
    }
}