package chatup.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

public class RoomInfo implements Serializable {

    private String roomName;
    private String roomPassword;
    private String roomOwner;
    private Set<String> roomUsers;
    private Set<Integer> roomServers;

    public RoomInfo(final String paramName, final String paramPassword, final String paramOwner) {
        roomName = paramName;
        roomOwner = paramOwner;
        roomPassword = paramPassword;
        roomTimestamp = Instant.now().getEpochSecond();
        roomUsers = new HashSet<>();
        roomServers = new HashSet<>();
    }

    public RoomInfo(final String paramName, final String paramPassword, final long timestamp, final String paramOwner) {
        roomName = paramName;
        roomOwner = paramOwner;
        roomPassword = paramPassword;
        roomTimestamp = timestamp;
        roomUsers = new HashSet<>();
        roomServers = new HashSet<>();
    }

    private long roomTimestamp;

    public boolean registerServer(int serverId) {

        if (roomServers.contains(serverId)) {
            return false;
        }

        roomServers.add(serverId);

        return true;
    }

    public boolean removeServer(int serverId) {

        if (roomServers.contains(serverId)) {
            roomServers.remove(serverId);
        }
        else {
            return false;
        }

        return true;
    }

    public boolean registerUser(final String userToken) {

        if (roomUsers.contains(userToken)) {
            return false;
        }

        roomUsers.add(userToken);

        return true;
    }

    public boolean removeUser(final String userToken)  {

        if (roomUsers.contains(userToken)) {
            roomUsers.remove(userToken);
        }
        else {
            return false;
        }

        return true;
    }

    public boolean isEmpty() {
        return roomUsers.isEmpty();
    }

    public boolean isPrivate() {
        return roomPassword != null;
    }

    public boolean hasUser(final String userToken) {
        return roomUsers.contains(userToken);
    }

    public final String getName() {
        return roomName;
    }

    public final String getOwner() {
        return roomOwner;
    }

    public final Set<Integer> getServers() {
        return roomServers;
    }

    public long getTimestamp() {
        return roomTimestamp;
    }

    final String getPassword() {
        return roomPassword;
    }

    @Override
    public String toString() {
        return roomName + " " + roomServers;
    }

    public void setServers(final Set<Integer> paramServers) {
       roomServers = paramServers;
    }

    public boolean validatePassword(final String userPassword) {
        return roomPassword.equals(userPassword);
    }
}