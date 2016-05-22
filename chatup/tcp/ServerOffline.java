package chatup.tcp;

import java.io.Serializable;

public class ServerOffline implements Serializable
{
    public int serverId;

    public ServerOffline()
    {
    }

    public ServerOffline(int paramId)
    {
        serverId = paramId;
    }
}