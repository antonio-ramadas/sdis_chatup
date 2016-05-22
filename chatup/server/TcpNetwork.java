package chatup.server;

import chatup.tcp.*;
import com.esotericsoftware.kryo.Kryo;
import kryonet.EndPoint;

public class TcpNetwork {

    private static Kryo kryo;

    static public void register(final EndPoint endPoint) {
        kryo = endPoint.getKryo();
        kryo.register(CreateRoom.class);
        kryo.register(DeleteRoom.class);
        kryo.register(DeleteServer.class);
        kryo.register(JoinRoom.class);
        kryo.register(LeaveRoom.class);
        kryo.register(ServerOffline.class);
        kryo.register(ServerOnline.class);
        kryo.register(SyncRoom.class);
        kryo.register(SyncRoomResponse.class);
        kryo.register(UserDisconnect.class);
    }
}