package chatup.tcp;

import chatup.http.ServerResponse;
import chatup.model.Message;
import chatup.server.SecondaryServer;
import chatup.server.ServerConnection;

import kryonet.Connection;
import kryonet.KryoServer;
import kryonet.Listener;

import java.util.HashMap;

public class SecondaryServerListener extends Listener {

    private HashMap<Integer, Integer> myConnections;

    public SecondaryServerListener(final SecondaryServer paramSecondary, final KryoServer paramClient) {
        kryoServer = paramClient;
        myConnections = new HashMap<>();
        secondaryServer = paramSecondary;
    }

    private final KryoServer kryoServer;
    private final SecondaryServer secondaryServer;

    @Override
    public void received(final Connection paramConnection, final Object paramObject) {

        final ServerConnection serverConnection = (ServerConnection) paramConnection;

        if (paramObject instanceof Message) {
            sendMessage((Message) paramObject);
        }
        else if (paramObject instanceof SyncRoom) {
            syncRoomResponse(serverConnection, (SyncRoom) paramObject);
        }
        else if (paramObject instanceof SyncRoomResponse) {
            syncRoom(serverConnection, (SyncRoomResponse)paramObject);
        }
    }

    @Override
    public void disconnected(final Connection paramConnection) {

        final ServerConnection serverConnection = (ServerConnection) paramConnection;

        if (serverConnection.serverId > 0) {
            myConnections.remove(serverConnection.serverId);
            secondaryServer.getLogger().serverOffline(serverConnection.serverId);
        }
    }

    private void sendMessage(final Message paramMessage) {

        final ServerResponse operationResult = secondaryServer.insertMessage(paramMessage);
        final String userToken = paramMessage.getSender();
        int roomId = paramMessage.getId();

        switch (operationResult) {
        case SuccessResponse:
            secondaryServer.getLogger().sendMessage(roomId);
            break;
        case InvalidToken:
            secondaryServer.getLogger().roomInvalidToken(roomId, userToken);
            break;
        case RoomNotFound:
            secondaryServer.getLogger().roomNotFound(roomId);
            break;
        default:
            secondaryServer.getLogger().invalidCommand("SendMessage");
            break;
        }
    }

    private void syncRoomResponse(final ServerConnection paramConnection, final SyncRoom syncRoom) {

        final ServerResponse operationResult = secondaryServer.syncRoom
        (
            syncRoom.roomId,
            paramConnection.serverId
        );

        switch (operationResult) {
        case SuccessResponse:
            secondaryServer.getLogger().syncRoom(syncRoom.roomId, paramConnection.serverId);
            break;
        case RoomNotFound:
            secondaryServer.getLogger().roomNotFound(syncRoom.roomId);
            break;
        default:
            secondaryServer.getLogger().invalidCommand("SyncRoom");
            break;
        }
    }

    private void syncRoom(final ServerConnection paramConnection, final SyncRoomResponse updateRoom) {

        final ServerResponse operationResult = secondaryServer.updateRoom(updateRoom);
        final String roomName = updateRoom.roomObject.getName();

        switch (operationResult) {
            case SuccessResponse:
                secondaryServer.getLogger().updateRoom(roomName, paramConnection.serverId);
                break;
            case RoomNotFound:
                secondaryServer.getLogger().roomNotFound(roomName);
                break;
            default:
                secondaryServer.getLogger().invalidCommand("SyncRoom");
                break;
        }
    }

    public void sendServer(int serverId, final Object paramObject) {

        if (myConnections.containsKey(serverId)) {
            kryoServer.sendToTCP(myConnections.get(serverId), paramObject);
        }
        else {
            secondaryServer.getLogger().serverNotFound(serverId);
        }
    }
}