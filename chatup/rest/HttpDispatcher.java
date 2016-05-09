package chatup.rest;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class HttpDispatcher {

    private final HttpExchange httpExchange;
    private final String httpParameters;

    HttpDispatcher(final HttpExchange paramExchange) {
        httpExchange = paramExchange;
        httpParameters = parseRequestBody(httpExchange.getRequestBody());
    }

    public void processRequest() {

        final String requestMethod = httpExchange.getRequestMethod();

        switch (requestMethod) {
        case "GET":
            parseGetRequest(extractGetCommand(httpParameters));
            break;
        case "POST":
            parsePostRequest(Json.parse(httpParameters));
            break;
        case "PUT":
            parsePutRequest(Json.parse(httpParameters));
            break;
        case "DELETE":
            parseDeleteRequest(Json.parse(httpParameters));
            break;
        default:
            sendError(ResponseMessages.msgInvalidMethod);
            break;
        }
    }

    protected boolean sendError(final String serverResponse) {

        try {
            sendResponse(Json.object().add("error", serverResponse).toString(), 404);
        }
        catch (IOException ex) {
            return false;
        }

        return true;
    }

    private void sendResponse(final String serverResponse, int statusCode) throws IOException {

        httpExchange.sendResponseHeaders(statusCode, serverResponse.length());

        try (final OutputStream os = httpExchange.getResponseBody()) {
            os.write(serverResponse.getBytes());
        }
    }

    protected boolean sendSuccess(final String requestCommand) {

        try {
            sendResponse(Json.object().add("success", requestCommand).toString(), 200);
        }
        catch (IOException ex) {
            return false;
        }

        return true;
    }

    protected final JsonObject extractCommand(final JsonValue jsonObject, final String commandName) {

        final JsonValue extractedCommand = jsonObject.asObject().get(commandName);

        if (extractedCommand != null) {

            if (extractedCommand.isObject()) {
                return extractedCommand.asObject();
            }

            return null;
        }

        return null;
    }

    protected final String[] extractGetCommand(final String requestBody) {
        return requestBody.substring(1).split("&");
    }

    protected final String parseString(final String parameterString, final String commandName) {

        final String[] parameterValues  = parameterString.split("=");

        if (parameterValues.length != 2) {
            return null;
        }

        if (parameterValues[0].equals(commandName)) {
            return parameterValues[1];
        }

        return null;
    }

    protected int parseInteger(final String parameterString, final String commandName) {

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

            return new String(out.toByteArray(), "utf-8");
        }
        catch (IOException ex) {
            return null;
        }
    }

    protected abstract void parseGetRequest(final String[] getValues);
    protected abstract void parsePostRequest(final JsonValue jsonValue);
    protected abstract void parsePutRequest(final JsonValue jsonValue);
    protected abstract void parseDeleteRequest(final JsonValue jsonValue);
}