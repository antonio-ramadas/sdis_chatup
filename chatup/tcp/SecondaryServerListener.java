package chatup.tcp;

import chatup.http.ServerResponse;
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

        if (paramObject instanceof SendMessage) {
            sendMessage((SendMessage) paramObject);
        }
        else if (paramObject instanceof SyncRoom) {
            syncRoom((SyncRoom) paramObject);
        }
    }

    private void sendMessage(final SendMessage sendMessage) {

        final ServerResponse operationResult = secondaryServer.registerMessage
        (
            sendMessage.roomId,
            sendMessage.message
        );

        final String userToken = sendMessage.message.getSender();

        switch (operationResult) {
        case SuccessResponse:
            secondaryServer.getLogger().sendMessage(sendMessage.roomId);
            break;
        case InvalidToken:
            secondaryServer.getLogger().roomInvalidToken(sendMessage.roomId, userToken);
            break;
        case RoomNotFound:
            secondaryServer.getLogger().roomNotFound(sendMessage.roomId);
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
            secondaryServer.getLogger().sendMessage(syncRoom.roomId);
            break;
        case RoomNotFound:
            secondaryServer.getLogger().roomNotFound(syncRoom.roomId);
            break;
        default:
            secondaryServer.getLogger().invalidCommand("SyncRoom");
            break;
        }
    }
}