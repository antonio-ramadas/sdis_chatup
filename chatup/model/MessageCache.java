package chatup.model;

import chatup.main.ChatupGlobals;

import java.io.Serializable;
import java.util.*;

public class MessageCache implements Serializable {

    private volatile TreeSet<Message> myQueue;

    public MessageCache() {

        myQueue = new TreeSet<>(new Comparator<Message>() {
            @Override
            public int compare(final Message lhsMessage, final Message rhsMessage) {
                return Long.compare(lhsMessage.getTimestamp(), rhsMessage.getTimestamp());
            }
        });
    }

    public synchronized Message push(final Message paramMessage) {

        final Message removedMessage;

        System.out.println("queue size: " + myQueue.size());

        if (myQueue.size() + 1 > ChatupGlobals.DefaultCacheSize) {
            removedMessage = myQueue.pollFirst();
        }
        else {
            removedMessage = null;
        }

        myQueue.add(paramMessage);

        return removedMessage;
    }

    public synchronized Message getLast() {
        return myQueue.last();
    }

    synchronized ArrayList<Message> getMessages(long paramTimestamp) {

        final ArrayList<Message> returnQueue = new ArrayList<>();

        if (myQueue.isEmpty()) {
            return returnQueue;
        }

        final Iterator<Message> it = myQueue.descendingIterator();

        while (it.hasNext()) {

            final Message mostRecent = it.next();

            if (mostRecent.getTimestamp() < paramTimestamp) {
                break;
            }

            returnQueue.add(mostRecent);
        }

        return returnQueue;
    }

    int size() {
        return myQueue.size();
    }
}