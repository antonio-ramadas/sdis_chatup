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

    public String serverAddress;

    public ServerOnline(int paramId, long paramTimestamp, final String paramAddress, int paramPort)
    {
        serverId = paramId;
        serverAddress = paramAddress;
        serverTimestamp = paramTimestamp;
        serverPort = paramPort;
    }
}