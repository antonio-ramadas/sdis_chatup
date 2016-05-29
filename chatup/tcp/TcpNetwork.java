package chatup.tcp;

import chatup.model.BoundedSet;
import chatup.model.CommandQueue;
import chatup.model.Message;
import chatup.model.MessageCache;

import com.esotericsoftware.kryo.Kryo;

import kryonet.EndPoint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class TcpNetwork {

    public static void registerPrimary(final EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(CommandQueue.class);
        kryo.register(CreateRoom.class);
        kryo.register(DeleteRoom.class);
        kryo.register(DeleteServer.class);
        kryo.register(HashSet.class);
        kryo.register(JoinRoom.class);
        kryo.register(LeaveRoom.class);
        kryo.register(LinkedList.class);
        kryo.register(ServerOffline.class);
        kryo.register(ServerOnline.class);
        kryo.register(UpdateServer.class);
        kryo.register(UserDisconnect.class);
    }

    public static void registerSecondary(final EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(BoundedSet.class);
        kryo.register(HashMap.class);
        kryo.register(Message.class);
        kryo.register(MessageCache.class);
        kryo.register(SyncRoom.class);
        kryo.register(ServerOnline.class);
        kryo.register(UpdateRoom.class);
    }
}