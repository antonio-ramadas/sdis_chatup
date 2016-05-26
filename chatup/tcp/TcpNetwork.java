package chatup.tcp;

import chatup.model.Message;
import chatup.model.MessageCache;

import com.esotericsoftware.kryo.Kryo;

import kryonet.EndPoint;

public class TcpNetwork {

    public static void register(final EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(CreateRoom.class);
        kryo.register(DeleteRoom.class);
        kryo.register(DeleteServer.class);
        kryo.register(JoinRoom.class);
        kryo.register(LeaveRoom.class);
        kryo.register(Message.class);
        kryo.register(MessageCache.class);
        kryo.register(ServerOffline.class);
        kryo.register(ServerOnline.class);
        kryo.register(SyncRoom.class);
        kryo.register(SyncRoomResponse.class);
        kryo.register(UpdateServer.class);
        kryo.register(UserDisconnect.class);
    }
}