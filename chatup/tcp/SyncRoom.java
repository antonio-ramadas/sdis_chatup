package chatup.tcp;

import java.io.Serializable;

public class SyncRoom implements Serializable
{
    public int roomId;

    public SyncRoom()
    {
    }

    public SyncRoom(int paramId)
    {
        roomId = paramId;
    }
}