package chatup.server;

import kryonet.Connection;

public class ServerConnection extends Connection{

    public int serverId;
    public boolean serverPrimary;

    public ServerConnection() {}
}