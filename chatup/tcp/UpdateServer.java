package chatup.tcp;

import chatup.server.ServerInfo;

import java.io.Serializable;

public class UpdateServer implements Serializable
{
    public int serverId;
    public int serverPort;
    public long serverTimestamp;

    public UpdateServer()
    {
    }

    public UpdateServer(final ServerInfo serverInfo)
    {
        serverAddress = serverInfo.getAddress();
        serverTimestamp = serverInfo.getTimestamp();
        serverId = serverInfo.getId();
        serverPort = serverInfo.getPort();
    }

    public String serverAddress;
}