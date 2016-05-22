package chatup.tcp;

import chatup.model.Room;

public class SyncRoomResponse
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