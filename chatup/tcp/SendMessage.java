package chatup.tcp;

import chatup.model.Message;

public class SendMessage
{
    public int roomId;

    public SendMessage()
    {
    }

    public SendMessage(int paramId, final Message paramMessage)
    {
        roomId = paramId;
        message = paramMessage;
    }

    public Message message;
}