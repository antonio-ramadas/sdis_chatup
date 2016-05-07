package chatup.backend;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class ServerDispatcher implements HttpHandler {

    protected final String parseRequestBody(final InputStream is) {

        try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            byte buf[] = new byte[4096];

            for (int n = is.read(buf); n > 0; n = is.read(buf)) {
                out.write(buf, 0, n);
            }

            return new String(out.toByteArray(), "utf-8");
        }
        catch (IOException ex) {
            return null;
        }
    }

    protected void sendResponse(final HttpExchange httpExchange, final String serverResponse, int statusCode) throws IOException {

        httpExchange.sendResponseHeaders(statusCode, serverResponse.length());

        try (final OutputStream os = httpExchange.getResponseBody()) {
            os.write(serverResponse.getBytes());
        }
    }
}