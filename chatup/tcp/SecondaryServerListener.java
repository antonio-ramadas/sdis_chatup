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

        switch (operationResult) {
        case SuccessResponse:
            secondaryServer.getLogger().sendMessage(paramMessage.getId());
            break;
        case InvalidToken:
            secondaryServer.getLogger().roomInvalidToken(paramMessage.getId(), paramMessage.getAuthor());
            break;
        case RoomNotFound:
            secondaryServer.getLogger().roomNotFound(paramMessage.getId());
            break;
        default:
            secondaryServer.getLogger().invalidOperation("SendMessage");
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
        }
    }

    private void syncRoom(final ServerConnection paramConnection, final SyncRoomResponse updateRoom) {

        final ServerResponse operationResult = secondaryServer.updateRoom(updateRoom);

        switch (operationResult) {
        case SuccessResponse:
            secondaryServer.getLogger().updateRoom(updateRoom.roomId, paramConnection.serverId);
            break;
        case RoomNotFound:
            secondaryServer.getLogger().roomNotFound(updateRoom.roomId);
            break;
        case DatabaseError:
            secondaryServer.getLogger().databaseError();
            break;
        default:
            secondaryServer.getLogger().invalidOperation(updateRoom);
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