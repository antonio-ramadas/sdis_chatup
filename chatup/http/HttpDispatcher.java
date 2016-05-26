package chatup.http;

import chatup.main.ChatupGlobals;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import com.esotericsoftware.minlog.Log;
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

        Log.info(httpService, requestMethod + " request from " + httpExchange.getRemoteAddress() + "...");

        switch (requestMethod) {
        case "GET":

            final String httpQuery = httpExchange.getRequestURI().getQuery();

            if (httpQuery == null || httpQuery.isEmpty()) {
                sendError(ServerResponse.MissingParameters);
            }
            else {
                parseGetRequest(httpQuery.split("&"));
            }

            break;

        case "POST":

            final String postBody = parseRequestBody(httpExchange.getRequestBody());

            if (postBody == null || postBody.isEmpty()) {
                sendError(ServerResponse.MissingParameters);
            }
            else {
                parsePostRequest(Json.parse(postBody));
            }

            break;

        case "PUT":

            final String putBody = parseRequestBody(httpExchange.getRequestBody());

            if (putBody == null || putBody.isEmpty()) {
                sendError(ServerResponse.MissingParameters);
            }
            else {
                parsePutRequest(Json.parse(putBody));
            }

            break;

        case "DELETE":

            final String deleteBody = parseRequestBody(httpExchange.getRequestBody());

            if (deleteBody == null || deleteBody.isEmpty()) {
                sendError(ServerResponse.MissingParameters);
            }
            else {
                parseDeleteRequest(Json.parse(deleteBody));
            }

            break;

        default:

            sendError(ServerResponse.InvalidMethod);

            break;
        }
    }

    boolean sendError(final ServerResponse serverResponse) {

        try {
            sendResponse(
                Json.object().add(HttpCommands.GenericError, serverResponse.toString()).toString(),
                HttpURLConnection.HTTP_OK
            );
        }
        catch (final IOException ex) {
            return false;
        }

        return true;
    }

    private void sendResponse(final String serverResponse, int statusCode) throws IOException {

        Log.info(httpService, "sending response: " + serverResponse);
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
                    Json.object().add(HttpCommands.GenericSuccess, httpParameters).toString(),
                    HttpURLConnection.HTTP_OK
                );
            }
            catch (final IOException ex) {
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
                    Json.object().add(HttpCommands.GenericSuccess, httpParameters).toString(),
                    HttpURLConnection.HTTP_OK
                );
            }
            catch (final IOException ex) {
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

    final String parseString(final String[] paramArray, int paramIndex, final String commandName) {

        if (paramIndex < 0 || paramIndex > paramArray.length - 1) {
            return null;
        }

        final String[] parameterValues  = paramArray[paramIndex].split("=");

        if (parameterValues.length != 2) {
            return null;
        }

        if (parameterValues[0].equals(commandName)) {
            return parameterValues[1];
        }

        return null;
    }

    int parseInteger(final String[] paramArray, int paramIndex, final String commandName) {

        final String integerString = parseString(paramArray, paramIndex, commandName);

        if (integerString == null || integerString.isEmpty()) {
            return -1;
        }

        try {
            return Integer.parseInt(integerString);
        }
        catch (final NumberFormatException ex) {
            return -1;
        }
    }

    long parseLong(final String[] paramArray, int paramIndex, final String commandName) {

        final String longString = parseString(paramArray, paramIndex, commandName);

        if (longString == null || longString.isEmpty()) {
            return 0L;
        }

        try {
            return Long.parseLong(longString);
        }
        catch (final NumberFormatException ex) {
            return 0L;
        }
    }

    private String parseRequestBody(final InputStream in) {

        try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            byte buf[] = new byte[ChatupGlobals.DefaultBuffer];

            for (int n = in.read(buf); n > 0; n = in.read(buf)) {
                out.write(buf, 0, n);
            }

            return new String(out.toByteArray(), ChatupGlobals.DefaultEncoding);
        }
        catch (final IOException ex) {
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