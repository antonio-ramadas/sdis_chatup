package chatup.model;

import java.io.Serializable;

public class Message implements Comparable<Message>, Serializable {

    private long messageTimestamp;
    private int messageRoom;

    public Message(int roomId, final String messageAuthor, long messageTimestamp, final String messageContents) {
        setId(roomId);
        setAuthor(messageAuthor);
        setMessage(messageContents);
        setTimestamp(messageTimestamp);
    }

    private String messageAuthor;
    private String messageContents;

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

    private void setId(int paramId) {
        messageRoom = paramId;
    }

    private void setTimestamp(long paramTimestamp) {
        messageTimestamp = paramTimestamp;
    }

    private void setAuthor(final String paramAuthor) {
        messageAuthor = paramAuthor;
    }

    private void setMessage(final String paramMessage) {
        messageContents = paramMessage;
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