package chatup.tcp;

import chatup.model.*;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

public class TcpNetwork {

    public static void register(final EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(LinkedList.class);
        kryo.register(HashMap.class);
        kryo.register(HashSet.class);
        kryo.register(ConcurrentHashMap.class);
        kryo.register(BoundedSet.class);
        kryo.register(CommandQueue.class);
        kryo.register(Message.class);
        kryo.register(Room.class);
        kryo.register(MessageCache.class);
        kryo.register(CreateRoom.class);
        kryo.register(DeleteRoom.class);
        kryo.register(DeleteServer.class);
        kryo.register(JoinRoom.class);
        kryo.register(LeaveRoom.class);
        kryo.register(ServerOffline.class);
        kryo.register(ServerOnline.class);
        kryo.register(SyncRoom.class);
        kryo.register(UpdateRoom.class);
        kryo.register(UpdateServer.class);
        kryo.register(UserDisconnect.class);
    }
}