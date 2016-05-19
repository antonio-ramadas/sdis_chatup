package chatup.server;

public class ServerInfo implements Comparable<ServerInfo> {

    private int serverId;
    private int serverPort;
    private int numUsers;

    public ServerInfo(final String serverAddress, int serverPort) {
        setAddress(serverAddress);
        setPort(serverPort);
        numUsers = 0;
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

    public int getNumUsers() { return numUsers; }

    public void incNumUsers() { numUsers++; }

    @Override
    public int compareTo(ServerInfo o) {
        if (numUsers < o.getNumUsers())
            return -1;
        else if (numUsers > o.getNumUsers())
            return 1;
        else
            return 0;
    }
}