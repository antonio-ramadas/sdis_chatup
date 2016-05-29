package chatup.server;

import java.time.Instant;

import java.util.HashSet;
import java.util.Set;

public class ServerInfo implements Comparable<ServerInfo> {

    private String serverAddress;
    private Set<String> serverUsers;

    public ServerInfo(int serverId, long serverTimestamp, final String serverAddress, int paramTcp, int paramHttp) {
        setId(serverId);
        setAddress(serverAddress);
        setTcpPort(paramTcp);
        setHttpPort(paramHttp);
        setTimestamp(serverTimestamp);
        serverStatus = false;
        serverUsers = new HashSet<>();
    }

    private int serverTcpPort;
    private int serverHttpPort;
    private int serverId;
    private long serverTimestamp;
    private boolean serverStatus;

    public int getId() {
        return serverId;
    }

    public int getHttpPort() {
        return serverHttpPort;
    }

    public int getTcpPort() {
        return serverTcpPort;
    }

    public long getTimestamp() {
        return serverTimestamp;
    }

    public String getAddress() {
        return serverAddress;
    }

    private void setId(int paramId) {
        serverId = paramId;
    }

    void setHttpPort(int paramPort) {
        serverHttpPort = paramPort;
    }

    void setTcpPort(int paramPort) {
        serverTcpPort = paramPort;
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
        int otherUsers = serverInfo.serverUsers.size();

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

    boolean isOnline() {
        return serverStatus;
    }

    int getLoad() {
        return serverUsers.size();
    }

    @Override
    public String toString() {
        return serverAddress + ":" + serverHttpPort + "(HTTP)," + serverTcpPort ;
    }

    void updateTimestamp() {
        serverTimestamp = Instant.now().getEpochSecond();
    }
}