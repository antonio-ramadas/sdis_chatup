package chatup.server;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class ServerInfo implements Comparable<ServerInfo> {

    private String serverAddress;
    private Set<String> serverUsers;

    public ServerInfo(int serverId, long serverTimestamp, final String serverAddress, int serverPort) {
        setId(serverId);
        setAddress(serverAddress);
        setPort(serverPort);
        setTimestamp(serverTimestamp);
        serverStatus = false;
        serverUsers = new HashSet<>();
    }

    private int serverPort;
    private int serverId;
    private long serverTimestamp;
    private boolean serverStatus;

    public int getId() {
        return serverId;
    }

    public int getPort() {
        return serverPort;
    }

    public long getTimestamp() {
        return serverTimestamp;
    }

    public String getAddress() {
        return serverAddress;
    }

    public Set<String> getUsers() {
        return serverUsers;
    }

    void setId(int paramId) {
        serverId = paramId;
    }

    void setPort(int paramPort) {
        serverPort = paramPort;
    }

    void setTimestamp(long paramTimestamp) {
        serverTimestamp = paramTimestamp;
    }

    void setAddress(final String paramAddress) {
        serverAddress = paramAddress;
    }

    boolean registerUser(final String userToken) {
        return serverUsers.add(userToken);
    }

    boolean removeUser(final String userToken) {

        if (serverUsers.contains(userToken)) {
            serverUsers.remove(userToken);
        }
        else {
            return false;
        }

        return true;
    }

    @Override
    public int compareTo(final ServerInfo serverInfo) {

        int thisUsers = serverUsers.size();
        int otherUsers = serverInfo.getUsers().size();

        if (thisUsers < otherUsers) {
            return -1;
        }

        if (thisUsers > otherUsers) {
            return 1;
        }

        return 0;
    }

    void setStatus(boolean paramStatus) {
        serverStatus = paramStatus;
    }

    public boolean hasUser(final String userToken) {
        return serverUsers.contains(userToken);
    }

    public boolean isOnline() {
        return serverStatus;
    }

    public int getLoad() {
        return serverUsers.size();
    }

    @Override
    public String toString() {
        return serverAddress + ":" + serverPort;
    }

    public void updateTimestamp() {
        serverTimestamp = Instant.now().getEpochSecond();
    }
}