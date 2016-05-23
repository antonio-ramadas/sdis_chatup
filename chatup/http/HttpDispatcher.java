package chatup.http;

import chatup.main.ChatupGlobals;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;

abstract class HttpDispatcher {

    private final String httpService;
    private final HttpExchange httpExchange;

    HttpDispatcher(final String paramService, final HttpExchange paramExchange) {
        httpService = paramService;
        httpExchange = paramExchange;
    }

    void processRequest() {

        final String requestMethod = httpExchange.getRequestMethod();

        System.out.println("[" + httpService + "] received " + requestMethod + " request from " + httpExchange.getRemoteAddress() + "...");

        switch (requestMethod) {
        case "GET":

            final String httpQuery = httpExchange.getRequestURI().getQuery();

            if (httpQuery != null) {
                parseGetRequest(httpQuery.split("&"));
            }
            else {
                parseGetRequest(new String[]{});
            }

            break;

        case "POST":
            parsePostRequest(Json.parse(parseRequestBody(httpExchange.getRequestBody())));
            break;
        case "PUT":
            parsePutRequest(Json.parse(parseRequestBody(httpExchange.getRequestBody())));
            break;
        case "DELETE":
            parseDeleteRequest(Json.parse(parseRequestBody(httpExchange.getRequestBody())));
            break;
        default:
            sendError(ServerResponse.InvalidMethod);
            break;
        }
    }

    boolean sendError(final ServerResponse serverResponse) {

        try {
            sendResponse(
                Json.object().add("error", serverResponse.toString()).toString(),
                HttpURLConnection.HTTP_OK
            );
        }
        catch (IOException ex) {
            return false;
        }

        return true;
    }

    private void sendResponse(final String serverResponse, int statusCode) throws IOException {

        System.out.println("[" + httpService +  "] sending response: " + serverResponse);
        httpExchange.sendResponseHeaders(statusCode, serverResponse.length());

        try (final OutputStream os = httpExchange.getResponseBody()) {
            os.write(serverResponse.getBytes());
            os.close();
        }
    }

    boolean sendTextResponse(final ServerResponse httpResponse, final String httpParameters) {

        if (httpResponse == ServerResponse.SuccessResponse) {

            try {
                sendResponse(
                    Json.object().add("success", httpParameters).toString(),
                    HttpURLConnection.HTTP_OK
                );
            }
            catch (IOException ex) {
                return false;
            }
        }
        else {
            return sendError(httpResponse);
        }

        return true;
    }

    boolean sendJsonResponse(final ServerResponse httpResponse, final JsonValue httpParameters) {

        if (httpResponse == ServerResponse.SuccessResponse) {

            try {
                sendResponse(
                    Json.object().add("success", httpParameters).toString(),
                    HttpURLConnection.HTTP_OK
                );
            }
            catch (IOException ex) {
                return false;
            }
        }
        else {
            return sendError(httpResponse);
        }

        return true;
    }

    final JsonObject extractCommand(final JsonValue jsonObject, final String commandName) {

        final JsonValue extractedCommand = jsonObject.asObject().get(commandName);

        if (extractedCommand != null) {

            if (extractedCommand.isObject()) {
                return extractedCommand.asObject();
            }

            return null;
        }

        return null;
    }

    final String parseString(final String parameterString, final String commandName) {

        final String[] parameterValues  = parameterString.split("=");

        if (parameterValues.length != 2) {
            return null;
        }

        if (parameterValues[0].equals(commandName)) {
            return parameterValues[1];
        }

        return null;
    }

    int parseInteger(final String parameterString, final String commandName) {

        final String integerString = parseString(parameterString, commandName);

        if (integerString == null) {
            return -1;
        }

        try {
            return Integer.parseInt(integerString);
        }
        catch (NumberFormatException ex) {
            return -1;
        }
    }

    private String parseRequestBody(final InputStream in) {

        try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            byte buf[] = new byte[4096];

            for (int n = in.read(buf); n > 0; n = in.read(buf)) {
                out.write(buf, 0, n);
            }

            return new String(out.toByteArray(), ChatupGlobals.DefaultEncoding);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public void parseGetRequest(final String[] getValues) {
        sendError(ServerResponse.InvalidMethod);
    }

    public void parsePostRequest(final JsonValue jsonValue) {
        sendError(ServerResponse.InvalidMethod);
    }

    public void parsePutRequest(final JsonValue jsonValue) {
        sendError(ServerResponse.InvalidMethod);
    }

    public void parseDeleteRequest(final JsonValue jsonValue) {
        sendError(ServerResponse.InvalidMethod);
    }
}