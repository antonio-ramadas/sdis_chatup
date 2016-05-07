package chatup.rest;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.sun.net.httpserver.HttpExchange;

public class RoomMessageHandler extends HttpDispatcher {

    public RoomMessageHandler(final HttpExchange httpExchange, final String requestBody) {
        super(httpExchange, requestBody);
    }

    @Override
    protected boolean parseGetRequest(JsonValue jsonValue) {
        return extractCommand(jsonValue, "GetRooms") != null;
    }

    @Override
    protected boolean parsePostRequest(JsonValue jsonValue) {

        final JsonObject jsonObject = extractCommand(jsonValue, "JoinRoom");

        if (jsonObject == null) {
            return false;
        }

        final int roomId = jsonObject.getInt("roomId", -1);
        final String userToken = jsonObject.getString("token", null);

        System.out.println("roomId:" + roomId);
        System.out.println("token:" + userToken);

        return true;
    }

    @Override
    protected boolean parsePutRequest(JsonValue jsonValue) {

        final JsonObject jsonObject = extractCommand(jsonValue, "CreateRoom");

        if (jsonObject == null) {
            return false;
        }

        final String roomName = jsonObject.getString("name", null);
        final String roomPassword = jsonObject.getString("password", null);
        final String userToken = jsonObject.getString("token", null);

        System.out.println("roomName:" + roomName);
        System.out.println("roomPassword:" + roomPassword);
        System.out.println("token:" + userToken);

        return true;
    }

    @Override
    protected boolean parseDeleteRequest(JsonValue jsonValue) {

        final JsonObject jsonObject = extractCommand(jsonValue, "DeleteRoom");

        if (jsonObject == null) {
            return false;
        }

        final int roomId = jsonObject.getInt("roomId", -1);
        final String userToken = jsonObject.getString("token", null);

        System.out.println("roomId:" + roomId);
        System.out.println("token:" + userToken);

        return true;
    }
}