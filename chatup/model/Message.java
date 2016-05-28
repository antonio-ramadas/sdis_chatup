package chatup.model;

import java.io.Serializable;

public class Message implements Comparable<Message>, Serializable {

    private String userEmail;
    private String userToken;
    private String messageContents;

    public Message(int paramId, final String paramAuthor, final String paramEmail, long paramTimestamp, final String paramContents) {
        messageRoom = paramId;
        userEmail = paramEmail;
        userToken = paramAuthor;
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
        return userEmail;
    }

    public final String getToken() {
        return userToken;
    }

    public final String getMessage() {
        return messageContents;
    }

    @Override
    public boolean equals(final Object otherObject) {
        return otherObject instanceof Message
            && ((Message) otherObject).getId() == messageRoom
            && ((Message) otherObject).getTimestamp() == messageTimestamp
            && ((Message) otherObject).getToken().equals(userToken);
    }

    @Override
    public int hashCode() {

        int hash = 3;

        hash = 11 * hash + (int) (messageTimestamp ^ (messageTimestamp >>> 32));
        hash = 11 * hash + messageRoom;
        hash = 11 * hash + userToken.hashCode();

        return hash;
    }

    @Override
    public int compareTo(final Message otherMessage) {
        return Long.compare(messageTimestamp, otherMessage.getTimestamp());
    }
}