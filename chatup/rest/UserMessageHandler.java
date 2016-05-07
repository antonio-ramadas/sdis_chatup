package chatup.rest;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.sun.net.httpserver.HttpExchange;

public class UserMessageHandler extends HttpDispatcher {

    public UserMessageHandler(final HttpExchange httpExchange, final String requestBody) {
        super(httpExchange, requestBody);
    }

    @Override
    protected boolean parseGetRequest(JsonValue jsonValue) {
        return false;
    }

    @Override
    protected boolean parsePostRequest(JsonValue jsonValue) {
        return false;
    }

    @Override
    protected boolean parseDeleteRequest(final JsonValue jsonValue) {

        final JsonObject jsonObject = extractCommand(jsonValue, "UserDisconnect");

        if (jsonObject == null) {
            return false;
        }

        final String userToken = jsonObject.getString("token", null);

        System.out.println("token:" + userToken);

        return true;
    }

    @Override
    protected boolean parsePutRequest(final JsonValue jsonValue) {

        final JsonObject jsonObject = extractCommand(jsonValue, "UserLogin");

        if (jsonObject == null) {
            return false;
        }

        final String userEmail = jsonObject.getString("email", null);
        final String userToken = jsonObject.getString("token", null);

        System.out.println("email:" + userEmail);
        System.out.println("token:" + userToken);

        return true;
    }
}