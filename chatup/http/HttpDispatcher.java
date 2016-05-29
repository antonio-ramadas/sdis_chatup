package chatup.http;

import chatup.main.ChatupGlobals;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.esotericsoftware.minlog.Log;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.HttpURLConnection;

abstract class HttpDispatcher implements HttpHandler {

    private final String httpService;

    HttpDispatcher(final String paramService) {
        httpService = paramService;
    }

    @Override
    public void handle(final HttpExchange httpExchange) {

        final String requestMethod = httpExchange.getRequestMethod();

        Log.info(httpService, requestMethod + " request from " + httpExchange.getRemoteAddress() + "...");

        switch (requestMethod) {
        case "GET":

            final String httpQuery = httpExchange.getRequestURI().getQuery();

            if (httpQuery == null || httpQuery.isEmpty()) {
                sendError(httpExchange, ServerResponse.MissingParameters);
            }
            else {
                parseGetRequest(httpExchange, httpQuery.split("&"));
            }

            break;

        case "POST":

            final String postBody = parseRequestBody(httpExchange.getRequestBody());

            if (postBody == null || postBody.isEmpty()) {
                sendError(httpExchange, ServerResponse.MissingParameters);
            }
            else {
                parsePostRequest(httpExchange, Json.parse(postBody));
            }

            break;

        case "PUT":

            final String putBody = parseRequestBody(httpExchange.getRequestBody());

            if (putBody == null || putBody.isEmpty()) {
                sendError(httpExchange, ServerResponse.MissingParameters);
            }
            else {
                parsePutRequest(httpExchange, Json.parse(putBody));
            }

            break;

        case "DELETE":

            final String deleteBody = parseRequestBody(httpExchange.getRequestBody());

            if (deleteBody == null || deleteBody.isEmpty()) {
                sendError(httpExchange, ServerResponse.MissingParameters);
            }
            else {
                parseDeleteRequest(httpExchange, Json.parse(deleteBody));
            }

            break;

        default:

            sendError(httpExchange, ServerResponse.InvalidMethod);

            break;
        }
    }

    void sendError(final HttpExchange httpExchange, final ServerResponse serverResponse) {

        try {
            sendResponse(
                httpExchange,
                Json.object().add(HttpCommands.GenericError, serverResponse.toString()).toString(),
                HttpURLConnection.HTTP_OK
            );
        }
        catch (final IOException ex) {
            ChatupGlobals.warn(httpService, ex);
        }
    }

    private void sendResponse(final HttpExchange httpExchange, final String serverResponse, int statusCode) throws IOException {

        Log.info(httpService, "sending response: " + serverResponse);
        httpExchange.sendResponseHeaders(statusCode, serverResponse.length());

        try (final OutputStream os = httpExchange.getResponseBody()) {
            os.write(serverResponse.getBytes());
            os.close();
        }
        catch (final IOException ex) {
            ChatupGlobals.warn(httpService, ex);
        }
    }

    void sendTextResponse(final HttpExchange httpExchange, final ServerResponse httpResponse, final String httpParameters) {

        if (httpResponse == ServerResponse.SuccessResponse) {

            try {
                sendResponse(
                    httpExchange,
                    Json.object().add(HttpCommands.GenericSuccess, httpParameters).toString(),
                    HttpURLConnection.HTTP_OK
                );
            }
            catch (final IOException ex) {
                ChatupGlobals.warn(httpService, ex);
            }
        }
        else {
            sendError(httpExchange, httpResponse);
        }
    }

    void sendJsonResponse(final HttpExchange httpExchange, final ServerResponse httpResponse, final JsonValue httpParameters) {

        if (httpResponse == ServerResponse.SuccessResponse) {

            try {
                sendResponse(
                    httpExchange,
                    Json.object().add(HttpCommands.GenericSuccess, httpParameters).toString(),
                    HttpURLConnection.HTTP_OK
                );
            }
            catch (final IOException ex) {
                ChatupGlobals.warn(httpService, ex);
            }
        }
        else {
            sendError(httpExchange, httpResponse);
        }
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

    public void parseGetRequest(final HttpExchange httpExchange, final String[] getValues) {
        sendError(httpExchange, ServerResponse.InvalidMethod);
    }

    public void parsePostRequest(final HttpExchange httpExchange, final JsonValue jsonValue) {
        sendError(httpExchange, ServerResponse.InvalidMethod);
    }

    public void parsePutRequest(final HttpExchange httpExchange, final JsonValue jsonValue) {
        sendError(httpExchange, ServerResponse.InvalidMethod);
    }

    public void parseDeleteRequest(final HttpExchange httpExchange, final JsonValue jsonValue) {
        sendError(httpExchange, ServerResponse.InvalidMethod);
    }
}