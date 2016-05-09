package chatup.server;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ServerInfo{

    private int serverId;
    private short httpPort;
    private short tcpPort;

    public ServerInfo(final InetAddress serverAddress, short httpPort, short tcpPort) {
        setId(0);
        setAddress(serverAddress);
        setHttpPort(httpPort);
        setTcpPort(tcpPort);
    }

    private InetAddress serverAddress;

    public ServerInfo(int serverId, final String serverAddress, short httpPort, short tcpPort) throws UnknownHostException {
        setId(serverId);
        setAddress(InetAddress.getByName(serverAddress));
        setHttpPort(httpPort);
        setTcpPort(tcpPort);
    }

    public InetAddress getAddress() {
        return serverAddress;
    }

    public void setAddress(InetAddress ipAddress) {
        this.serverAddress = ipAddress;
    }

    public short getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(short httpPort) {
        this.httpPort = httpPort;
    }

    public short getTcpPort() { return tcpPort; }

    public void setTcpPort(short tcpPort) { this.tcpPort = tcpPort; }

    public int getId() {
        return serverId;
    }

    public void setId(int serverId) {
        this.serverId = serverId;
    }
}