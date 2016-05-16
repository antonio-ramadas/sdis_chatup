package chatup.tcp;

public class DeleteServer implements TcpMessage {

    private int serverId;

    public DeleteServer(int paramId) {
        serverId = paramId;
    }

    public int getServerId() {
        return serverId;
    }

    @Override
    public TcpCommand getType() {
        return TcpCommand.DeleteServer;
    }
}