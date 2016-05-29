package chatup.http;

import chatup.main.ChatupGlobals;
import chatup.main.ChatupServer;
import chatup.server.AbstractServer;
import chatup.server.ServerInfo;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import com.sun.net.httpserver.HttpExchange;

import javafx.util.Pair;

public class RoomService extends HttpDispatcher {

    public RoomService() {
        super(ChatupGlobals.RoomServiceUrl);
    }

    @Override
    public void parseGetRequest(final HttpExchange httpExchange, final String[] jsonValue) {

        final AbstractServer serverInstance = ChatupServer.getInstance();

        if (jsonValue.length > 0) {

            final String userToken = parseString(jsonValue, 0, HttpFields.UserToken);

            if (userToken == null) {
                sendError(httpExchange, ServerResponse.InvalidToken);
            }
            else if (serverInstance.validateToken(userToken)){
                sendJsonResponse(httpExchange, ServerResponse.SuccessResponse, serverInstance.getRooms());
            }
            else {
                sendError(httpExchange, ServerResponse.InvalidToken);
            }
        }
        else {
            sendError(httpExchange, ServerResponse.MissingParameters);
        }
    }

    @Override
    public void parsePostRequest(final HttpExchange httpExchange, final JsonValue jsonValue) {

        final AbstractServer serverInstance = ChatupServer.getInstance();
        final JsonObject jsonObject = extractCommand(jsonValue, HttpCommands.JoinRoom);

        if (jsonObject != null) {

            int roomId = jsonObject.getInt(HttpFields.RoomId, -1);
            final String userPassword = jsonObject.getString(HttpFields.RoomPassword, null);
            final String userToken = jsonObject.getString(HttpFields.UserToken, null);

            if (roomId < 0 || userToken == null || userToken.isEmpty()) {
                sendError(httpExchange, ServerResponse.MissingParameters);
            }
            else {

                final Pair<ServerResponse, ServerInfo> serverPair = serverInstance.joinRoom(
                    roomId, userPassword, userToken
                );

                final ServerResponse serverResponse = serverPair.getKey();
                final ServerInfo serverInfo = serverPair.getValue();

                if (serverResponse == ServerResponse.SuccessResponse) {
                    sendJsonResponse(httpExchange, serverResponse, jsonObject
                        .add("address", serverInfo.getAddress())
                        .add("port", serverInfo.getHttpPort()));
                }
                else {
                    sendError(httpExchange, serverResponse);
                }
            }
        }
        else {
            sendError(httpExchange, ServerResponse.InvalidOperation);
        }
    }

    @Override
    public void parsePutRequest(final HttpExchange httpExchange, final JsonValue jsonValue) {

        final AbstractServer serverInstance = ChatupServer.getInstance();
        final JsonObject jsonObject = extractCommand(jsonValue, HttpCommands.CreateRoom);

        if (jsonObject != null) {

            final String roomName = jsonObject.getString(HttpFields.RoomName, null);
            final String roomPassword = jsonObject.getString(HttpFields.RoomPassword, null);
            final String userToken = jsonObject.getString(HttpFields.UserToken, null);

            if (roomName == null || roomName.isEmpty()) {
                sendError(httpExchange, ServerResponse.MissingParameters);
            }
            else if (userToken == null || userToken.isEmpty()) {
                sendError(httpExchange, ServerResponse.MissingParameters);
            }
            else {
                sendJsonResponse(httpExchange, serverInstance.createRoom(roomName, roomPassword, userToken), jsonObject);
            }
        }
        else {
            sendError(httpExchange, ServerResponse.InvalidOperation);
        }
    }

    @Override
    public void parseDeleteRequest(final HttpExchange httpExchange, final JsonValue jsonValue) {

        final AbstractServer serverInstance = ChatupServer.getInstance();
        final JsonObject jsonObject = extractCommand(jsonValue, HttpCommands.LeaveRoom);

        if (jsonObject != null) {

            int roomId = jsonObject.getInt(HttpFields.RoomId, -1);
            final String userToken = jsonObject.getString(HttpFields.UserToken, null);

            if (roomId < 0 || userToken == null || userToken.isEmpty()) {
                sendError(httpExchange, ServerResponse.MissingParameters);
            }
            else {
                sendJsonResponse(httpExchange, serverInstance.leaveRoom(roomId, userToken), jsonObject);
            }
        }
        else {
            sendError(httpExchange, ServerResponse.InvalidOperation);
        }
    }
}