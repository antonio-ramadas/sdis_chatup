package chatup.server;

public class ServerInfo implements Comparable<ServerInfo>{

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

    public int getNumberUsers() {
        return numUsers;
    }

    public void incrementUsers() {
        numUsers++;
    }

    public void decrementUsers() {
        numUsers++;
    }

    @Override
    public int compareTo(final ServerInfo otherObject) {

        if (numUsers < otherObject.getNumberUsers()) {
            return -1;
        }
        else if (numUsers > otherObject.getNumberUsers()) {
            return 1;
        }
        else {
            return 0;
        }
    }
}