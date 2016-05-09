package chatup.rest;

import com.eclipsesource.json.JsonValue;
import com.sun.net.httpserver.HttpExchange;

public class MessageServiceHandler extends HttpDispatcher {

    public MessageServiceHandler(HttpExchange httpExchange) {
        super(httpExchange);
    }

    @Override
    protected void parseGetRequest(JsonValue jsonValue) {

    }

    @Override
    protected void parsePostRequest(JsonValue jsonValue) {

    }

    @Override
    protected void parsePutRequest(JsonValue jsonValue) {
        sendError(ResponseMessages.msgInvalidMethod);
    }

    @Override
    protected void parseDeleteRequest(JsonValue jsonValue) {
        sendError(ResponseMessages.msgInvalidMethod);
    }
}