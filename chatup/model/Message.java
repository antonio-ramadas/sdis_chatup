package chatup.model;

import java.io.Serializable;

public class Message implements Comparable<Message>, Serializable {

    private String messageAuthor;
    private String messageContents;

    public Message(int paramId, final String paramAuthor, long paramTimestamp, final String paramContents) {
        messageRoom = paramId;
        messageAuthor = paramAuthor;
        messageContents = paramContents;
        messageTimestamp = paramTimestamp;
    }

    private long messageTimestamp;
    private int messageRoom;

    public int getId() {
        return messageRoom;
    }

    public long getTimestamp() {
        return messageTimestamp;
    }

    public final String getAuthor() {
        return messageAuthor;
    }

    public final String getMessage() {
        return messageContents;
    }

    @Override
    public boolean equals(final Object otherObject) {
        return otherObject instanceof Message
            && ((Message) otherObject).getId() == messageRoom
            && ((Message) otherObject).getTimestamp() == messageTimestamp
            && ((Message) otherObject).getAuthor().equals(messageAuthor);
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