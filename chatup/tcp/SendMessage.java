package chatup.tcp;

import chatup.model.Message;

public class SendMessage
{
    public int messageId;

    public SendMessage()
    {
    }

    public SendMessage(int paramId, final Message paramMessage)
    {
        messageId = paramId;
        message = paramMessage;
    }

    public Message message;
}