package chatup.backend;

import com.eclipsesource.json.Json;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

public abstract class ServerDispatcher implements HttpHandler {

    private void sendResponse(final HttpExchange httpExchange, final String serverResponse, int statusCode) throws IOException {

        httpExchange.sendResponseHeaders(statusCode, serverResponse.length());

        try (final OutputStream os = httpExchange.getResponseBody()) {
            os.write(serverResponse.getBytes());
        }
    }

    protected boolean sendError(final HttpExchange httpExchange, final String requestCommand) {

        try {
            sendResponse(httpExchange, Json.object().add("error", requestCommand).toString(), 404);
        }
        catch (IOException ex) {
            return false;
        }

        return true;
    }

    protected boolean sendSuccess(final HttpExchange httpExchange, final String requestCommand) {

        try {
            sendResponse(httpExchange, Json.object().add("success", requestCommand).toString(), 200);
        }
        catch (IOException ex) {
            return false;
        }

        return true;
    }
}