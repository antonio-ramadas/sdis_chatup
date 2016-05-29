package chatup.tcp;

import chatup.server.ServerInfo;

import java.io.Serializable;

public class UpdateServer implements Serializable
{
    public int serverId;
    public int httpPort;
    public int tcpPort;
    public long serverTimestamp;

    public UpdateServer()
    {
    }

    public UpdateServer(final ServerInfo serverInfo)
    {
        serverAddress = serverInfo.getAddress();
        serverTimestamp = serverInfo.getTimestamp();
        serverId = serverInfo.getId();
        httpPort = serverInfo.getHttpPort();
        tcpPort = serverInfo.getTcpPort();
    }

    public String serverAddress;
}