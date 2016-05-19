package chatup.tcp;

public class ServerOnline
{
    public int serverId;
    public int serverPort;

    public ServerOnline()
    {
    }

    public ServerOnline(int paramId)
    {
        this(paramId, null, -1);
    }

    public ServerOnline(int paramId, final String paramAddress, int paramPort)
    {
        serverId = paramId;
        serverAddress = paramAddress;
        serverPort = paramPort;
    }

    public String serverAddress;
}