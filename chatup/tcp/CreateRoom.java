package chatup.tcp;

import chatup.model.Room;

import java.io.Serializable;
import java.util.HashSet;

public class CreateRoom implements Serializable
{
    public String userEmail;
    public String userToken;
    public String roomName;
    public String roomPassword;
    public HashSet<Integer> roomServers;

    public CreateRoom()
    {
    }

    public int roomId;
    public long roomTimestamp;

    public CreateRoom(int paramId, final Room paramRoom, final String paramEmail)
    {
        roomId = paramId;
        roomName = paramRoom.getName();
        roomPassword = paramRoom.getPassword();
        roomServers = paramRoom.getServers();
        roomTimestamp = paramRoom.getTimestamp();
        userToken = paramRoom.getOwner();
        userEmail = paramEmail;
    }
}