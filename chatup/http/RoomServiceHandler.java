package chatup.http;

import chatup.main.ChatupGlobals;
import chatup.main.ChatupServer;
import chatup.server.Server;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import com.sun.net.httpserver.HttpExchange;

class RoomServiceHandler extends HttpDispatcher {

    RoomServiceHandler(final HttpExchange httpExchange) {
        super(ChatupGlobals.RoomServiceUrl, httpExchange);
    }

    @Override
    public void parseGetRequest(String[] getValues) {
        sendJsonResponse(ServerResponse.SuccessResponse, ChatupServer.getInstance().getRooms());
    }

    @Override
    public void parsePostRequest(final JsonValue jsonValue) {

        final Server serverInstance = ChatupServer.getInstance();
        final JsonObject jsonObject = extractCommand(jsonValue, HttpCommands.JoinRoom);

        if (jsonObject != null) {

            int roomId = jsonObject.getInt(HttpFields.RoomId, -1);
            final String userPassword = jsonObject.getString(HttpFields.RoomPassword, null);
            final String userToken = jsonObject.getString(HttpFields.UserToken, null);

            if (roomId < 0 || userToken == null) {
                sendError(ServerResponse.MissingParameters);
            }
            else {
                sendJsonResponse(serverInstance.joinRoom(roomId, userPassword, userToken), jsonObject);
            }
        }
        else {
            sendError(ServerResponse.InvalidCommand);
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
                sendError(ServerResponse.MissingParameters);
            }
            else {
                sendJsonResponse(serverInstance.createRoom(roomName, roomPassword, userToken), jsonObject);
            }
        }
        else {
            sendError(ServerResponse.InvalidCommand);
        }
    }

    @Override
    public void parseDeleteRequest(JsonValue jsonValue) {

        final Server serverInstance = ChatupServer.getInstance();
        final JsonObject jsonObject = extractCommand(jsonValue, HttpCommands.LeaveRoom);

        if (jsonObject != null) {

            int roomId = jsonObject.getInt(HttpFields.RoomId, -1);
            final String userToken = jsonObject.getString(HttpFields.UserToken, null);

            if (roomId < 0 || userToken == null) {
                sendError(ServerResponse.MissingParameters);
            }
            else {
                sendJsonResponse(serverInstance.leaveRoom(roomId, userToken), jsonObject);
            }
        }
        else {
            sendError(ServerResponse.InvalidCommand);
        }
    }
}