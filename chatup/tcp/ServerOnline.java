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
        this(paramId, paramTimestamp, null, -1);
    }

    public ServerOnline(int paramId, long paramTimestamp, final String paramAddress, int paramPort)
    {
        serverId = paramId;
        serverAddress = paramAddress;
        serverPort = paramPort;
    }

    public String serverAddress;
}