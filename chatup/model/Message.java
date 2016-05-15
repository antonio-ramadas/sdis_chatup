package chatup.model;

public class Message implements Comparable<Message> {

    private long messageTimestamp;
    private int messageRoom;

    public Message(int roomId, final String messageAuthor, long messageTimestamp, final String messageContents) {
        setRoomId(roomId);
        setAuthor(messageAuthor);
        setMessage(messageContents);
        setTimestamp(messageTimestamp);
    }

    private String messageAuthor;
    private String messageContents;

    public int getRoomId() {
        return messageRoom;
    }

    public long getTimestamp() {
        return messageTimestamp;
    }

    public final String getMessage() {
        return messageContents;
    }

    public final String getSender() {
        return messageAuthor;
    }

    public void setRoomId(int paramId) {
        messageRoom = paramId;
    }

    public void setAuthor(final String paramAuthor) {
        messageAuthor = paramAuthor;
    }

    public void setTimestamp(long paramTimestamp) {
        messageTimestamp = paramTimestamp;
    }

    public void setMessage(final String paramMessage) {
        messageContents = paramMessage;
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof Message
            && ((Message) other).getRoomId() == messageRoom
            && ((Message) other).getTimestamp() == messageTimestamp
            && ((Message) other).getSender().equals(messageAuthor);
    }

    @Override
    public int compareTo(final Message other) {
        return Long.compare(messageTimestamp, other.getTimestamp());
    }
}