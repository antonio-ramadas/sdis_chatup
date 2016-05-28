package chatup.http;

import chatup.main.ChatupGlobals;
import chatup.main.ChatupServer;
import chatup.server.Server;
import chatup.server.ServerInfo;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import com.sun.net.httpserver.HttpExchange;

import javafx.util.Pair;

class RoomServiceHandler extends HttpDispatcher {

    RoomServiceHandler(final HttpExchange httpExchange) {
        super(ChatupGlobals.RoomServiceUrl, httpExchange);
    }

    @Override
    public void parseGetRequest(final String[] jsonValue) {

        final Server serverInstance = ChatupServer.getInstance();

        if (jsonValue.length > 0) {

            final String userToken = parseString(jsonValue, 0, HttpFields.UserToken);

            if (userToken == null) {
                sendError(ServerResponse.InvalidToken);
            }
            else if (serverInstance.validateToken(userToken)){
                sendJsonResponse(ServerResponse.SuccessResponse, serverInstance.getRooms());
            }
            else {
                sendError(ServerResponse.InvalidToken);
            }
        }
        else {
            sendError(ServerResponse.MissingParameters);
        }
    }

    @Override
    public void parsePostRequest(final JsonValue jsonValue) {

        final Server serverInstance = ChatupServer.getInstance();
        final JsonObject jsonObject = extractCommand(jsonValue, HttpCommands.JoinRoom);

        if (jsonObject != null) {

            int roomId = jsonObject.getInt(HttpFields.RoomId, -1);
            final String userPassword = jsonObject.getString(HttpFields.RoomPassword, null);
            final String userToken = jsonObject.getString(HttpFields.UserToken, null);

            if (roomId < 0 || userToken == null || userToken.isEmpty()) {
                sendError(ServerResponse.MissingParameters);
            }
            else {

                final Pair<ServerResponse, ServerInfo> serverPair = serverInstance.joinRoom(
                    roomId, userPassword, userToken
                );

                final ServerResponse serverResponse = serverPair.getKey();
                final ServerInfo serverInfo = serverPair.getValue();

                if (serverResponse == ServerResponse.SuccessResponse) {
                    sendJsonResponse(serverResponse, jsonObject
                        .add("address", serverInfo.getAddress())
                        .add("port", serverInfo.getPort()));
                }
                else {
                    sendError(serverResponse);
                }
            }
        }
        else {
            sendError(ServerResponse.InvalidOperation);
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

            if (roomName == null || roomName.isEmpty()) {
                sendError(ServerResponse.MissingParameters);
            }
            else if (userToken == null || userToken.isEmpty()) {
                sendError(ServerResponse.MissingParameters);
            }
            else {
                sendJsonResponse(serverInstance.createRoom(roomName, roomPassword, userToken), jsonObject);
            }
        }
        else {
            sendError(ServerResponse.InvalidOperation);
        }
    }

    @Override
    public void parseDeleteRequest(JsonValue jsonValue) {

        final Server serverInstance = ChatupServer.getInstance();
        final JsonObject jsonObject = extractCommand(jsonValue, HttpCommands.LeaveRoom);

        if (jsonObject != null) {

            int roomId = jsonObject.getInt(HttpFields.RoomId, -1);
            final String userToken = jsonObject.getString(HttpFields.UserToken, null);

            if (roomId < 0 || userToken == null || userToken.isEmpty()) {
                sendError(ServerResponse.MissingParameters);
            }
            else {
                sendJsonResponse(serverInstance.leaveRoom(roomId, userToken), jsonObject);
            }
        }
        else {
            sendError(ServerResponse.InvalidOperation);
        }
    }
}