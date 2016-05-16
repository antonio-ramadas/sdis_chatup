package chatup.tcp;

public class CreateRoom {

    public String roomName;
    public String roomPassword;
    public String roomOwner;

    public CreateRoom()
    {
    }

    public CreateRoom(final String paramName, final String paramPassword, final String paramOwner)
    {
        roomName = paramName;
        roomPassword = paramPassword;
        roomOwner = paramOwner;
    }
}