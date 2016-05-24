package chatup.model;

import java.io.Serializable;

import java.util.LinkedList;

public class CommandQueue implements Serializable {

    private LinkedList<Object> myQueue;

    public CommandQueue() {
        myQueue = new LinkedList<>();
    }

    public void push(final Object paramObject) {
        myQueue.addLast(paramObject);
    }

    public boolean empty() {
        return myQueue.isEmpty();
    }

    public final Object pop() {
        return myQueue.removeFirst();
    }
}