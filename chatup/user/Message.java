package chatup.user;

public class Message implements Comparable<Message> {

    private long messageTimestamp;
    private int messageRoom;

    public Message(final String paramBody, final String paramSender, int paramRoom, long paramTimestamp) {
        messageBody = paramBody;
        messageRoom = paramRoom;
        messageSender = paramSender;
        messageTimestamp = paramTimestamp;
    }

    private String messageSender;
    private String messageBody;

    public final int getRoomId() {
        return messageRoom;
    }

    public final long getTimestamp() {
        return messageTimestamp;
    }

    public final String getMessage() {
        return messageBody;
    }

    public final String getSender() {
        return messageSender;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof Message
            && ((Message) other).getRoomId() == messageRoom
            && ((Message) other).getTimestamp() == messageTimestamp
            && ((Message) other).getSender().equals(messageSender);
    }

    @Override
    public int compareTo(final Message other) {
        return Long.compare(messageTimestamp, other.getTimestamp());
    }
}