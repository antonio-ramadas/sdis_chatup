package chatup.user;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

public class UserSession {

    private short myPort;

    public UserSession(final InetAddress paramAddress, short paramPort) {
        myAddress = paramAddress;
        myPort = paramPort;
        myRooms = new HashSet<Integer>();
    }

    private InetAddress myAddress;
    private Set<Integer> myRooms;

    public final int getCount() {
        return myRooms.size();
    }

    public final short getPort() {
        return myPort;
    }

    public final void setPort(short paramPort) {
        myPort = paramPort;
    }

    public final InetAddress getAddress() {
        return myAddress;
    }

    public final void setAddress(final InetAddress paramAddress) {
        myAddress = paramAddress;
    }

    public final Set<Integer> getRooms() {
        return myRooms;
    }
}