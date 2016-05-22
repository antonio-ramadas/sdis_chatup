package chatup.model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class MessageCache<K, V> implements Serializable {

    private final Map<K, V> cache;

    public MessageCache(final int maxEntries) {

        cache = new LinkedHashMap<K, V>((int) Math.ceil(maxEntries * 1.75), .75f, true) {

            private static final long serialVersionUID = -1650698049637132983L;

            @Override
            public boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxEntries;
            }
        };
    }

    public Object[] getArray() {
        return cache.values().toArray(new Object[cache.size()]);
    }

    public void add(final K key, final V value) {
        cache.put(key, value);
    }

    public int size() {
        return cache.size();
    }

    public final V get(final K key) {
        return cache.get(key);
    }

    public final Map<K,V> getCache() {
        return cache;
    }

    public void putAll(Map<K,V> otherMap) {
        cache.putAll(otherMap);
    }
}