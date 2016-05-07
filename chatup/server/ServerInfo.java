package chatup.server;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ServerInfo{

    private int serverId;
    private short serverPort;

    public ServerInfo(final InetAddress serverAddress, short serverPort) {
        setId(0);
        setAddress(serverAddress);
        setPort(serverPort);
    }

    private InetAddress serverAddress;

    public ServerInfo(int serverId, final String serverAddress, short serverPort) throws UnknownHostException {
        setId(serverId);
        setAddress(InetAddress.getByName(serverAddress));
        setPort(serverPort);
    }

    public InetAddress getAddress() {
        return serverAddress;
    }

    public void setAddress(InetAddress ipAddress) {
        this.serverAddress = ipAddress;
    }

    public short getPort() {
        return serverPort;
    }

    public void setPort(short serverPort) {
        this.serverPort = serverPort;
    }

    public int getId() {
        return serverId;
    }

    public void setId(int serverId) {
        this.serverId = serverId;
    }
}