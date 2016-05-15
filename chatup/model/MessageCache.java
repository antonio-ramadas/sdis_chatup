package chatup.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class MessageCache<K, V> {

    private Map<K, V> cache;

    MessageCache(final int maxEntries) {

        cache = new LinkedHashMap<K, V>((int) Math.ceil(maxEntries * 1.75), .75f, true) {

            private static final long serialVersionUID = -1650698049637132983L;

            public boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > maxEntries;
            }
        };
    }

    final Object[] getArray() {
        return cache.values().toArray(new Object[cache.size()]);
    }

    final void add(final K key, final V value) {
        cache.put(key, value);
    }

    final V get(final K key) {
        return cache.get(key);
    }
}