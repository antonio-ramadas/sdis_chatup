package chatup.tcp;

public class SyncServers
{
    public int serverId;
    public long serverTimestamp;

    public SyncServers()
    {
    }

    public SyncServers(int paramId, long paramTimestamp)
    {
        serverId = paramId;
        serverTimestamp = paramTimestamp;
    }
}