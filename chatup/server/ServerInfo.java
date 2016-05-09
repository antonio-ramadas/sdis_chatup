package chatup.server;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ServerInfo {

    private int serverId;
    private short tcpPort;

    public ServerInfo(int serverId, final String serverAddress, short tcpPort) throws UnknownHostException {
        setId(serverId);
        setAddress(InetAddress.getByName(serverAddress));
        setTcpPort(tcpPort);
    }

    private InetAddress serverAddress;

    public InetAddress getAddress() {
        return serverAddress;
    }

    public void setAddress(InetAddress paramAddress) {
        serverAddress = paramAddress;
    }

    public void setTcpPort(short paramPort) {
        tcpPort = paramPort;
    }

    public short getTcpPort() { return tcpPort; }

    public int getId() {
        return serverId;
    }

    public void setId(int serverId) {
        this.serverId = serverId;
    }
}