package chatup.tcp;

public class ReplaceServer implements TcpMessage {

    private int serverId;
    private short serverPort;

    public ReplaceServer(int paramId, final String paramAddress, short paramPort) {
        serverId = paramId;
        serverAddress = paramAddress;
        serverPort = paramPort;
    }

    private final String serverAddress;

    public final String getAddress() {
        return serverAddress;
    }

    @Override
    public final TcpCommand getType() {
        return TcpCommand.ReplaceServer;
    }

    public short getPort() {
        return serverPort;
    }

    public int getServerId() {
        return serverId;
    }
}