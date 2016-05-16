package chatup.server;

public class ServerInfo {

    private int serverId;
    private int serverPort;

    public ServerInfo(final String serverAddress, int serverPort) {
        setAddress(serverAddress);
        setPort(serverPort);
    }

    private String serverAddress;

    public int getPort() {
        return serverPort;
    }

    public String getAddress() {
        return serverAddress;
    }

    public void setPort(int paramPort) {
        serverPort = paramPort;
    }

    public void setAddress(final String paramAddress) {
        serverAddress = paramAddress;
    }
}