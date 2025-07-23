package hieu.vn;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CacheTTL<K, V> implements Map<K, V> {
    private final int ttlSeconds;
    private final int idleSeconds;
    private final Map<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();
    private final AtomicInteger hits = new AtomicInteger();
    private final AtomicInteger total = new AtomicInteger();

    public CacheTTL(int ttlSeconds, int idleSeconds) {
        this.ttlSeconds = ttlSeconds;
        this.idleSeconds = idleSeconds;
        cleaner.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.SECONDS);
    }

    private static class CacheEntry<V> {
        V value;
        long insertTime;
        long lastAccess;

        CacheEntry(V value) {
            this.value = value;
            this.insertTime = System.currentTimeMillis();
            this.lastAccess = insertTime;
        }

        boolean isExpired(long now, int ttl, int idle) {
            long age = (now - insertTime) / 1000;
            long idleTime = (now - lastAccess) / 1000;
            return age > ttl || idleTime > idle;
        }
    }

    @Override
    public V get(Object key) {
        total.incrementAndGet();
        CacheEntry<V> entry = cache.get(key);
        if (entry == null) return null;

        long now = System.currentTimeMillis();
        if (entry.isExpired(now, ttlSeconds, idleSeconds)) {
            cache.remove(key);
            return null;
        }

        entry.lastAccess = now;
        hits.incrementAndGet();
        return entry.value;
    }

    @Override
    public V put(K key, V value) {
        cache.put(key, new CacheEntry<>(value));
        return value;
    }

    public Map<K, V> getMap() {
        Map<K, V> result = new HashMap<>();
        long now = System.currentTimeMillis();
        for (Map.Entry<K, CacheEntry<V>> e : cache.entrySet()) {
            if (!e.getValue().isExpired(now, ttlSeconds, idleSeconds)) {
                result.put(e.getKey(), e.getValue().value);
            }
        }
        return result;
    }

    public int getHitRate() {
        int t = total.get();
        return t == 0 ? 0 : (int) ((hits.get() * 100.0) / t);
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<K, CacheEntry<V>>> it = cache.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<K, CacheEntry<V>> entry = it.next();
            if (entry.getValue().isExpired(now, ttlSeconds, idleSeconds)) {
                it.remove();
            }
        }
    }

    // Map methods (delegate or minimal)
    @Override public int size() { return getMap().size(); }
    @Override public boolean isEmpty() { return getMap().isEmpty(); }
    @Override public boolean containsKey(Object key) { return getMap().containsKey(key); }
    @Override public boolean containsValue(Object value) { return getMap().containsValue(value); }
    @Override public V remove(Object key) { return cache.remove(key).value; }
    @Override public void putAll(Map<? extends K, ? extends V> m) { m.forEach(this::put); }
    @Override public void clear() { cache.clear(); }
    @Override public Set<K> keySet() { return getMap().keySet(); }
    @Override public Collection<V> values() { return getMap().values(); }
    @Override public Set<Entry<K, V>> entrySet() { return getMap().entrySet(); }
}
