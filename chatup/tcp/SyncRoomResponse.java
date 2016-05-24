package chatup.tcp;

import chatup.model.Room;

import java.io.Serializable;
import java.util.Set;

public class SyncRoomResponse implements Serializable
{
    public Set<Integer> roomServers;
    public Set<String> roomUsers;

    public SyncRoomResponse()
    {
    }

    public SyncRoomResponse(int paramId, final Room paramRoom)
    {
        roomId = paramId;
        roomServers = paramRoom.getServers();
        roomTimestamp = paramRoom.getTimestamp();
        roomUsers = paramRoom.getUsers();
    }

    public int roomId;
    public long roomTimestamp;
}