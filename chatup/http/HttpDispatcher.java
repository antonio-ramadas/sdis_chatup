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

public abstract class HttpDispatcher {

    private final String httpService;
    private final HttpExchange httpExchange;

    HttpDispatcher(final String paramService, final HttpExchange paramExchange) {
        httpService = paramService;
        httpExchange = paramExchange;
    }

    public void processRequest() {

        final String requestMethod = httpExchange.getRequestMethod();

        System.out.println(httpService + " received " + requestMethod + " request from " + httpExchange.getRemoteAddress() + "...");

        switch (requestMethod) {
        case "GET":
            parseGetRequest(httpExchange.getRequestURI().getQuery().split("&"));
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
            sendError(HttpResponses.InvalidMethod);
            break;
        }
    }

    protected boolean sendError(final String serverResponse) {

        try {
            sendResponse(Json.object().add(HttpResponses.ErrorResponse, serverResponse).toString(), HttpURLConnection.HTTP_OK);
        }
        catch (IOException ex) {
            return false;
        }

        return true;
    }

    private void sendResponse(final String serverResponse, int statusCode) throws IOException {

        System.out.println(httpService +  " sending response: " + serverResponse);
        httpExchange.sendResponseHeaders(statusCode, serverResponse.length());

        try (final OutputStream os = httpExchange.getResponseBody()) {
            os.write(serverResponse.getBytes());
            os.close();
        }
    }

    protected boolean sendSuccess(final String requestCommand) {

        try {
            sendResponse(
                Json.object().add(HttpResponses.SuccessResponse, requestCommand).toString(),
                HttpURLConnection.HTTP_OK
            );
        }
        catch (IOException ex) {
            return false;
        }

        return true;
    }

    protected boolean sendJsonSuccess(final JsonValue requestParameters) {

        try {
            sendResponse(
                Json.object().add(HttpResponses.SuccessResponse, requestParameters).toString(),
                HttpURLConnection.HTTP_OK
            );
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

    private String[] extractGetCommand(final String requestBody) {

        if (requestBody == null) {
            return null;
        }

        final String[] requestArray = requestBody.substring(1).split("&");

        if (requestArray == null)
        {
            return new String[]{};
        }

        return requestArray;
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

            System.out.println( new String(out.toByteArray(), ChatupGlobals.DefaultEncoding));

            return new String(out.toByteArray(), ChatupGlobals.DefaultEncoding);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public void parseGetRequest(final String[] getValues) {
        sendError(HttpResponses.InvalidMethod);
    }

    public void parsePostRequest(final JsonValue jsonValue) {
        sendError(HttpResponses.InvalidMethod);
    }

    public void parsePutRequest(final JsonValue jsonValue) {
        sendError(HttpResponses.InvalidMethod);
    }

    public void parseDeleteRequest(final JsonValue jsonValue) {
        sendError(HttpResponses.InvalidMethod);
    }
}