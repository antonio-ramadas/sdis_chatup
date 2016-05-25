package chatup.tcp;

import java.io.Serializable;

public class ServerOnline implements Serializable
{
    public int serverId;
    public int serverPort;
    public long serverTimestamp;

    public ServerOnline()
    {
    }

    public ServerOnline(int paramId, long paramTimestamp)
    {
        this(paramId, paramTimestamp, -1);
    }

    public ServerOnline(int paramId, long paramTimestamp, int paramPort)
    {
        serverId = paramId;
        serverTimestamp = paramTimestamp;
        serverPort = paramPort;
    }
}