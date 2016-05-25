package chatup.tcp;

import chatup.model.Room;

import java.io.Serializable;

public class CreateRoom implements Serializable
{
    public String userEmail;
    public String userToken;
    public String roomName;
    public String roomPassword;

    public CreateRoom()
    {
    }

    public int roomId;

    public CreateRoom(int paramId, final Room paramRoom, final String paramEmail)
    {
        roomId = paramId;
        roomName = paramRoom.getName();
        roomPassword = paramRoom.getPassword();
        userToken = paramRoom.getOwner();
        userEmail = paramEmail;
    }
}