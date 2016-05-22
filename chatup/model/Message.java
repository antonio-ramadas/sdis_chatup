package chatup.model;

import java.util.Objects;

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

    public final void setRoomId(int paramId) {
        messageRoom = paramId;
    }

    public final void setAuthor(final String paramAuthor) {
        messageAuthor = paramAuthor;
    }

    public final void setTimestamp(long paramTimestamp) {
        messageTimestamp = paramTimestamp;
    }

    public final void setMessage(final String paramMessage) {
        messageContents = paramMessage;
    }

    @Override
    public boolean equals(final Object otherObject) {
        return otherObject instanceof Message
            && ((Message) otherObject).getRoomId() == messageRoom
            && ((Message) otherObject).getTimestamp() == messageTimestamp
            && ((Message) otherObject).getSender().equals(messageAuthor);
    }

    @Override
    public int hashCode() {

        int hash = 3;

        hash = 11 * hash + (int) (messageTimestamp ^ (messageTimestamp >>> 32));
        hash = 11 * hash + messageRoom;
        hash = 11 * hash + Objects.hashCode(messageAuthor);

        return hash;
    }

    @Override
    public int compareTo(final Message otherMessage) {
        return Long.compare(messageTimestamp, otherMessage.getTimestamp());
    }
}