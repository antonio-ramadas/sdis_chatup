package chatup.model;

import chatup.main.ChatupGlobals;

import java.io.Serializable;
import java.util.*;

public class MessageCache implements Serializable {

    private final PriorityQueue<Message> cache;
    private final Set<Integer> messageIds;

    MessageCache() {
        maximumLength = ChatupGlobals.DefaultCacheSize;
        messageIds = new HashSet<>();
        cache = new PriorityQueue<>();
    }

    private int maximumLength;

    public Object[] getArray() {
        return cache.toArray(new Object[cache.size()]);
    }

    public void add(final Message paramMessage) {

        if (messageIds.contains(paramMessage.getId())) {
            return;
        }

        cache.add(paramMessage);

        if (cache.size() > maximumLength) {
            cache.poll();
        }
    }

    public int size() {
        return cache.size();
    }

    final PriorityQueue<Message> getCache() {
        return cache;
    }

    void putAll(Collection<Message> paramCollection) {
        paramCollection.forEach(this::add);
    }
}