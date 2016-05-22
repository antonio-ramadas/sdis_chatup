package chatup.server;

import java.util.HashSet;
import java.util.Set;

public class ServerInfo implements Comparable<ServerInfo> {

    private String serverAddress;
    private Set<String> serverUsers;

    public ServerInfo(final String serverAddress, int serverPort) {
        setAddress(serverAddress);
        setPort(serverPort);
        serverUsers = new HashSet<>();
    }

    private int serverPort;

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
        return serverUsers.size();
    }

    public boolean registerUser(final String userToken) {
        return serverUsers.add(userToken);
    }

    public boolean removeUser(final String userToken) {
        return serverUsers.remove(userToken);
    }

    @Override
    public int compareTo(final ServerInfo serverInfo) {

        int otherUsers = serverInfo.getNumberUsers();

        if (getNumberUsers() < otherUsers) {
            return -1;
        }

        if (getNumberUsers() > otherUsers) {
            return 1;
        }

        return 0;
    }

    public boolean hasUser(final String userToken) {
        return serverUsers.contains(userToken);
    }
}