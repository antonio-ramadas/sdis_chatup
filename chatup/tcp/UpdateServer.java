package chatup.tcp;

import java.io.Serializable;

public class UpdateServer implements Serializable
{
    public int serverId;
    public int serverPort;
    public long serverTimestamp;

    public UpdateServer()
    {
    }

    public UpdateServer(int paramId, long paramTimestamp)
    {
        this(paramId, paramTimestamp, null, -1);
    }

    public UpdateServer(int paramId, long paramTimestamp, final String paramAddress, int paramPort)
    {
        serverAddress = paramAddress;
        serverId = paramId;
        serverTimestamp = paramTimestamp;
        serverPort = paramPort;
    }

    public String serverAddress;
}