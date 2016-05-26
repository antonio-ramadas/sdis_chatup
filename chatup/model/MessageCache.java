package chatup.model;

import chatup.main.ChatupGlobals;

import java.io.Serializable;
import java.util.*;

public class MessageCache implements Serializable {

    private volatile BoundedSet<Message> myQueue;

    MessageCache() {
        myQueue = new BoundedSet<>(ChatupGlobals.DefaultCacheSize);
    }

    synchronized void push(final Message paramMessage) {
        myQueue.add(paramMessage);
    }

    synchronized Message getLast() {
        return myQueue.first();
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