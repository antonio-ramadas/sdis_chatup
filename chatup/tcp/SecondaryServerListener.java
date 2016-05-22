package chatup.tcp;

import chatup.http.ServerResponse;
import chatup.model.Message;
import chatup.server.SecondaryServer;

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

        if (paramObject instanceof Message) {
            sendMessage((Message) paramObject);
        }
        else if (paramObject instanceof SyncRoom) {
            syncRoom((SyncRoom) paramObject);
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

    private void syncRoom(final SyncRoom syncRoom) {

        final ServerResponse operationResult = secondaryServer.syncRoom
        (
            syncRoom.roomId,
            syncRoom.messageCache
        );

        switch (operationResult) {
        case SuccessResponse:
            secondaryServer.getLogger().sendBlock(syncRoom.roomId);
            break;
        case RoomNotFound:
            secondaryServer.getLogger().roomNotFound(syncRoom.roomId);
            break;
        default:
            secondaryServer.getLogger().invalidCommand("SyncRoom");
            break;
        }
    }

    public void send(int serverId, final Object paramObject) {

        if (myConnections.containsKey(serverId)) {
            kryoServer.sendToTCP(myConnections.get(serverId), paramObject);
        }
        else {
            secondaryServer.getLogger().serverNotFound(serverId);
        }
    }
}