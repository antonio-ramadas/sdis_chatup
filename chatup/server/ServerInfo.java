package chatup.server;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ServerInfo {

    private int serverId;
    private short tcpPort;

    public ServerInfo(final String serverAddress, short tcpPort) throws UnknownHostException {
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

    public short getPort() { return tcpPort; }
}