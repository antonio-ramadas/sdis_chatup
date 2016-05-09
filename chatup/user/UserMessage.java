package chatup.user;

public class UserMessage implements Comparable<UserMessage> {

    private long messageTimestamp;
    private int messageRoom;

    public UserMessage(final String paramBody, final String paramSender, int paramRoom, long paramTimestamp) {
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
        return other instanceof UserMessage
            && ((UserMessage) other).getRoomId() == messageRoom
            && ((UserMessage) other).getTimestamp() == messageTimestamp
            && ((UserMessage) other).getSender().equals(messageSender);
    }

    @Override
    public int compareTo(final UserMessage other) {
        return Long.compare(messageTimestamp, other.getTimestamp());
    }
}