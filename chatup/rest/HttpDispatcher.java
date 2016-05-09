package chatup.rest;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import com.sun.net.httpserver.HttpExchange;

public abstract class HttpDispatcher {

    private HttpExchange httpExchange;
    private String requestParameters;

    public HttpDispatcher(final HttpExchange paramExchange, final String paramBody) {
        httpExchange = paramExchange;
        requestParameters = paramBody;
    }

    public boolean processRequest() {

        final String requestMethod = httpExchange.getRequestMethod();

        switch (requestMethod) {
        case "GET":
            parseGetRequest(Json.parse(requestParameters));
            break;
        case "POST":
            parsePostRequest(Json.parse(requestParameters));
            break;
        case "PUT":
            parsePutRequest(Json.parse(requestParameters));
            break;
        case "DELETE":
            parseDeleteRequest(Json.parse(requestParameters));
            break;
        default:
            System.out.println("invalid request!");
            break;
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

    protected abstract boolean parseGetRequest(final JsonValue jsonValue);
    protected abstract boolean parsePostRequest(final JsonValue jsonValue);
    protected abstract boolean parsePutRequest(final JsonValue jsonValue);
    protected abstract boolean parseDeleteRequest(final JsonValue jsonValue);
}