package chatup.http;

import chatup.main.ChatupGlobals;
import chatup.main.ChatupServer;
import chatup.server.Server;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import com.sun.net.httpserver.HttpExchange;

public class RoomServiceHandler extends HttpDispatcher {

    public RoomServiceHandler(final HttpExchange httpExchange) {
        super(ChatupGlobals.RoomServiceUrl, httpExchange);
    }

    @Override
    public void parseGetRequest(String[] getValues) {
        sendJsonSuccess(Json.object().add(HttpCommands.RetrieveRooms, ChatupServer.getInstance().getRooms()));
    }

    @Override
    public void parsePostRequest(JsonValue jsonValue) {

        final Server serverInstance = ChatupServer.getInstance();
        final JsonObject jsonObject = extractCommand(jsonValue, HttpCommands.JoinRoom);

        if (jsonObject != null) {

            int roomId = jsonObject.getInt(HttpFields.RoomId, -1);
            final String userToken = jsonObject.getString(HttpFields.UserToken, null);

            if (roomId == -1 || userToken == null) {
                sendError(HttpResponses.MissingParameters);
            }
            else if (serverInstance.joinRoom(roomId, userToken)) {
                sendJsonSuccess(jsonValue);
            }
            else {
                sendError(HttpResponses.OperationFailed);
            }
        }
        else {
            sendError(HttpResponses.InvalidCommand);
        }
    }

    @Override
    public void parsePutRequest(JsonValue jsonValue) {

        final Server serverInstance = ChatupServer.getInstance();
        final JsonObject jsonObject = extractCommand(jsonValue, HttpCommands.CreateRoom);

        if (jsonObject != null) {

            final String roomName = jsonObject.getString(HttpFields.RoomName, null);
            final String roomPassword = jsonObject.getString(HttpFields.RoomPassword, null);
            final String userToken = jsonObject.getString(HttpFields.UserToken, null);

            if (roomName == null || userToken == null) {
                sendError(HttpResponses.MissingParameters);
            }
            else if (serverInstance.createRoom(roomName, roomPassword, userToken)) {
                sendJsonSuccess(jsonValue);
            }
            else {
                sendError(HttpResponses.OperationFailed);
            }
        }
        else {
            sendError(HttpResponses.InvalidCommand);
        }
    }

    @Override
    public void parseDeleteRequest(JsonValue jsonValue) {

        final Server serverInstance = ChatupServer.getInstance();
        final JsonObject jsonObject = extractCommand(jsonValue, HttpCommands.DeleteRoom);

        if (jsonObject != null) {

            int roomId = jsonObject.getInt(HttpFields.RoomId, -1);
            final String userToken = jsonObject.getString(HttpFields.UserToken, null);

            if (roomId == -1 || userToken == null) {
                sendError(HttpResponses.MissingParameters);
            }
            else if (serverInstance.deleteRoom(roomId, userToken)) {
                sendJsonSuccess(jsonValue);
            }
            else {
                sendError(HttpResponses.OperationFailed);
            }
        }
        else {
            sendError(HttpResponses.InvalidCommand);
        }
    }
}