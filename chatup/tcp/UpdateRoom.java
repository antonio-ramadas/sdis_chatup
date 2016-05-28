package chatup.tcp;

import chatup.model.Room;

import java.io.Serializable;

public class UpdateRoom implements Serializable
{
    public Room roomObject;

    public UpdateRoom()
    {
    }

    public UpdateRoom(int paramId, final Room paramRoom)
    {
        roomId = paramId;
        roomObject = paramRoom;
    }

    public int roomId;
}