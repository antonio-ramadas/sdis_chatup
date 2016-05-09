package chatup.user;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

public class UserSession {

    private short userPort;

    public UserSession(final InetAddress userAddress, short userPort) {
        setAddress(userAddress);
        setPort(userPort);
        setRooms(new HashSet<>());
    }

    private InetAddress userAddress;
    private Set<Integer> userRooms;

    public boolean enterRoom(int roomId) {

        if (userRooms.contains(roomId)) {
            return false;
        }

        userRooms.add(roomId);

        return true;
    }

    public boolean leaveRoom(int roomId) {

        if (!userRooms.contains(roomId)) {
            return false;
        }

        userRooms.remove(roomId);

        return true;
    }

    public int getCount() {
        return userRooms.size();
    }

    public short getPort() {
        return userPort;
    }

    public void setPort(short paramPort) {
        userPort = paramPort;
    }

    public final InetAddress getAddress() {
        return userAddress;
    }

    public void setAddress(final InetAddress paramAddress) {
        userAddress = paramAddress;
    }

    public void setRooms(final Set<Integer> paramRooms) {
        userRooms = paramRooms;
    }

    public final Set<Integer> getRooms() {
        return userRooms;
    }
}