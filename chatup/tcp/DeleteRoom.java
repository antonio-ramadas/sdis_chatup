package chatup.tcp;

import java.io.Serializable;

public class DeleteRoom implements Serializable
{
    public int roomId;

    public DeleteRoom()
    {
    }

    public DeleteRoom(int paramId)
    {
        roomId = paramId;
    }
}