package chatup.rest;

import chatup.main.ChatupServer;
import chatup.server.Server;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import com.sun.net.httpserver.HttpExchange;

public class RoomServiceHandler extends HttpDispatcher {

    public RoomServiceHandler(final HttpExchange httpExchange) {
        super(httpExchange);
    }

    @Override
    protected void parseGetRequest(JsonValue jsonValue) {

        final Server serverInstance = ChatupServer.getInstance();

        if (extractCommand(jsonValue, "GetRooms") == null) {
            sendError(ResponseMessages.msgInvalidCommand);
        }
        else {
            sendSuccess(serverInstance.getRooms());
        }
    }

    @Override
    protected void parsePostRequest(JsonValue jsonValue) {

        final Server serverInstance = ChatupServer.getInstance();
        final JsonObject jsonObject = extractCommand(jsonValue, "JoinRoom");

        if (jsonObject != null) {

            int roomId = jsonObject.getInt("roomId", -1);
            final String userToken = jsonObject.getString("token", null);

            if (roomId == -1 || userToken == null) {
                sendError(ResponseMessages.msgMissingParameters);
            }
            else if (serverInstance.joinRoom(roomId, userToken)) {
                sendSuccess("joinedRoom");
            }
            else {
                sendError(ResponseMessages.msgOperationFailed);
            }
        }
        else {
            sendError(ResponseMessages.msgInvalidCommand);
        }
    }

    @Override
    protected void parsePutRequest(JsonValue jsonValue) {

        final Server serverInstance = ChatupServer.getInstance();
        final JsonObject jsonObject = extractCommand(jsonValue, "CreateRoom");

        if (jsonObject != null) {

            final String roomName = jsonObject.getString("name", null);
            final String roomPassword = jsonObject.getString("password", null);
            final String userToken = jsonObject.getString("token", null);

            if (roomName == null || userToken == null) {
                sendError(ResponseMessages.msgMissingParameters);
            }
            else if (serverInstance.createRoom(roomName, roomPassword, userToken)) {
                sendSuccess(ResponseMessages.msgCreateRoom);
            }
            else {
                sendError(ResponseMessages.msgOperationFailed);
            }
        }
        else {
            sendError(ResponseMessages.msgInvalidCommand);
        }
    }

    @Override
    protected void parseDeleteRequest(JsonValue jsonValue) {

        final Server serverInstance = ChatupServer.getInstance();
        final JsonObject jsonObject = extractCommand(jsonValue, "DeleteRoom");

        if (jsonObject != null) {

            int roomId = jsonObject.getInt("roomId", -1);
            final String userToken = jsonObject.getString("token", null);

            if (roomId == -1 || userToken == null) {
                sendError(ResponseMessages.msgMissingParameters);
            }
            else if (serverInstance.deleteRoom(roomId, userToken)) {
                sendSuccess(ResponseMessages.messageDeleteRoom);
            }
            else {
                sendError(ResponseMessages.msgOperationFailed);
            }
        }
        else {
            sendError(ResponseMessages.msgInvalidCommand);
        }
    }
}