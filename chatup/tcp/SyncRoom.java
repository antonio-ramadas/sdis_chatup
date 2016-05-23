package chatup.tcp;

import java.io.Serializable;

public class SyncRoom implements Serializable
{
    public int roomId;
    public long roomTimestamp;

    public SyncRoom()
    {
    }

    public SyncRoom(int paramId, long paramTimestamp)
    {
        roomId = paramId;
        roomTimestamp = paramTimestamp;
    }
}