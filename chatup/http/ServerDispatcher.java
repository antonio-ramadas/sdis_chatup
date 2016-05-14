package chatup.http;

import com.eclipsesource.json.Json;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

public abstract class ServerDispatcher implements HttpHandler {

    protected boolean sendError(final HttpExchange httpExchange, final String requestCommand) {

        final String errorResponse = Json.object().add(HttpResponses.ErrorResponse, requestCommand).toString();

        try {
            httpExchange.sendResponseHeaders(404, errorResponse.length());

            try (final OutputStream os = httpExchange.getResponseBody()) {
                os.write(errorResponse.getBytes());
            }
        }
        catch (IOException ex) {
            return false;
        }

        return true;
    }
}