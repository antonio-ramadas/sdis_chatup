package chatup.tcp;

import chatup.model.CommandQueue;
import chatup.model.Message;

import com.esotericsoftware.kryo.Kryo;

import kryonet.EndPoint;

public class TcpNetwork {

    public static void registerPrimary(final EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(CommandQueue.class);
        kryo.register(CreateRoom.class);
        kryo.register(DeleteRoom.class);
        kryo.register(DeleteServer.class);
        kryo.register(JoinRoom.class);
        kryo.register(LeaveRoom.class);
        kryo.register(ServerOffline.class);
        kryo.register(ServerOnline.class);
        kryo.register(UpdateServer.class);
        kryo.register(UserDisconnect.class);
    }

    public static void registerSecondary(final EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(Message.class);
        kryo.register(SyncRoom.class);
        kryo.register(ServerOnline.class);
        kryo.register(UpdateRoom.class);
    }
}