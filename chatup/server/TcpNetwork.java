package chatup.server;

import chatup.model.MessageCache;
import chatup.tcp.*;
import com.esotericsoftware.kryo.Kryo;
import kryonet.EndPoint;

class TcpNetwork {

    static void register(final EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(CreateRoom.class);
        kryo.register(DeleteRoom.class);
        kryo.register(DeleteServer.class);
        kryo.register(JoinRoom.class);
        kryo.register(LeaveRoom.class);
        kryo.register(MessageCache.class);
        kryo.register(ServerOffline.class);
        kryo.register(ServerOnline.class);
        kryo.register(SyncRoom.class);
        kryo.register(SyncRoomResponse.class);
        kryo.register(UserDisconnect.class);
    }
}