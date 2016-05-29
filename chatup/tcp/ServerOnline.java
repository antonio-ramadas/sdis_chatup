package chatup.tcp;

import java.io.Serializable;

public class ServerOnline implements Serializable
{
    public int serverId;
    public int tcpPort;
    public int httpPort;
    public long serverTimestamp;

    public ServerOnline()
    {
    }

    public String serverAddress;

    public ServerOnline(int paramId, long paramTimestamp, final String paramAddress, int paramTcp, int paramHttp)
    {
        serverId = paramId;
        serverAddress = paramAddress;
        serverTimestamp = paramTimestamp;
        tcpPort = paramTcp;
        httpPort = paramHttp;
    }
}