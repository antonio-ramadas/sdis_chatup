package chatup.model;

import chatup.main.ChatupGlobals;

import google.collections.MinMaxPriorityQueue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;

public class MessageCache implements Serializable {

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

        final ArrayList<Message> returnQueue = new ArrayList<>();

        for (Message mostRecent = pop(); mostRecent != null; mostRecent = pop()) {
            returnQueue.add(mostRecent);
        }

        myQueue.addAll(returnQueue);

        return returnQueue.toArray(new Message[returnQueue.size()]);
    }

    public final Message[] getMessages(long paramTimestamp) {

        if (myQueue.isEmpty()) {
            return new Message[]{};
        }

        if (paramTimestamp <= 0 || getFirst().getTimestamp() >= paramTimestamp) {
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