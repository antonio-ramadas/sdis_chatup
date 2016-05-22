package chatup.tcp;

import java.io.Serializable;

public class DeleteServer implements Serializable
{
    public int serverId;

    public DeleteServer()
    {
    }

    public DeleteServer(int paramId)
    {
        serverId = paramId;
    }
}