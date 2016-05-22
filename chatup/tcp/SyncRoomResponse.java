package chatup.tcp;

import chatup.model.Room;

import java.io.Serializable;

public class SyncRoomResponse implements Serializable
{
    public Room roomObject;

    public SyncRoomResponse()
    {
    }

    public SyncRoomResponse(int paramId, final Room paramRoom)
    {
        roomId = paramId;
        roomObject = paramRoom;
    }

    public int roomId;
}