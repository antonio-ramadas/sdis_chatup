package chatup.model;

import chatup.main.ChatupGlobals;
import google.collections.MinMaxPriorityQueue;

import java.util.Comparator;

public class MessageCache {

    private MinMaxPriorityQueue<Message> myQueue;

    public MessageCache() {

        myQueue = MinMaxPriorityQueue.orderedBy(new Comparator<Message>() {
            @Override
            public int compare(final Message lhsMessage, final Message rhsMessage) {
                return Long.compare(rhsMessage.getTimestamp(), lhsMessage.getTimestamp());
            }

            @Override
            public Comparator<Message> reversed() {
                return (lhsMessage, rhsMessage) -> Long.compare(lhsMessage.getTimestamp(), rhsMessage.getTimestamp());
            }
        }).maximumSize(ChatupGlobals.DefaultCacheSize).create();
    }

    public boolean push(final Message paramMessage) {
        return myQueue.offer(paramMessage);
    }

    public final Message pop() {
        return myQueue.pollFirst();
    }

    public final Message getLast() {
        return myQueue.peekFirst();
    }

    public final Message getFirst() {
        return myQueue.peekLast();
    }

    public final Message[] toArray() {
        return myQueue.toArray(new Message[myQueue.size()]);
    }

    public final Message[] getMessages(long paramTimestamp) {

        if (paramTimestamp <= 0) {
            return toArray();
        }

        if (getFirst().getTimestamp() >= paramTimestamp) {
            return toArray();
        }

        final MinMaxPriorityQueue<Message> returnQueue = MinMaxPriorityQueue.orderedBy(new Comparator<Message>() {
            @Override
            public int compare(final Message lhsMessage, final Message rhsMessage) {
                return Long.compare(rhsMessage.getTimestamp(), lhsMessage.getTimestamp());
            }

            @Override
            public Comparator<Message> reversed() {
                return (lhsMessage, rhsMessage) -> Long.compare(lhsMessage.getTimestamp(), rhsMessage.getTimestamp());
            }
        }).maximumSize(ChatupGlobals.DefaultCacheSize).create();

        for (Message mostRecent = pop(); mostRecent != null && mostRecent.getTimestamp() >= paramTimestamp; mostRecent = pop()) {
            returnQueue.add(mostRecent);
        }

        myQueue.addAll(returnQueue);

        return returnQueue.toArray(new Message[returnQueue.size()]);
    }

    public int size() {
        return myQueue.size();
    }
}