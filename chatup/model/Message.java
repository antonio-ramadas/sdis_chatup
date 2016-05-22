package chatup.model;

import java.io.Serializable;
import java.time.Instant;

public class Message implements Comparable<Message>, Serializable {

    private long messageTimestamp;
    private int messageRoom;

    public Message(int roomId, final String messageAuthor, long messageTimestamp, final String messageContents) {
        setId(roomId);
        setAuthor(messageAuthor);
        setMessage(messageContents);
        setTimestamp(messageTimestamp);
    }

    public Message(int roomId, final String messageAuthor, final String messageContents) {
        this(roomId, messageAuthor, Instant.now().toEpochMilli(), messageContents);
    }

    private String messageAuthor;
    private String messageContents;

    public int getId() {
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

    private void setId(int paramId) {
        messageRoom = paramId;
    }

    private void setAuthor(final String paramAuthor) {
        messageAuthor = paramAuthor;
    }

    private void setTimestamp(long paramTimestamp) {
        messageTimestamp = paramTimestamp;
    }

    private void setMessage(final String paramMessage) {
        messageContents = paramMessage;
    }

    @Override
    public boolean equals(final Object otherObject) {
        return otherObject instanceof Message
            && ((Message) otherObject).getId() == messageRoom
            && ((Message) otherObject).getTimestamp() == messageTimestamp
            && ((Message) otherObject).getSender().equals(messageAuthor);
    }

    @Override
    public int hashCode() {

        int hash = 3;

        hash = 11 * hash + (int) (messageTimestamp ^ (messageTimestamp >>> 32));
        hash = 11 * hash + messageRoom;
        hash = 11 * hash + messageAuthor.hashCode();

        return hash;
    }

    @Override
    public int compareTo(final Message otherMessage) {
        return Long.compare(messageTimestamp, otherMessage.getTimestamp());
    }
}