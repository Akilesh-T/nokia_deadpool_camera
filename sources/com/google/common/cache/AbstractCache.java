package com.google.common.cache;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

@GwtCompatible
@Beta
public abstract class AbstractCache<K, V> implements Cache<K, V> {

    @Beta
    public interface StatsCounter {
        void recordEviction();

        void recordHits(int i);

        void recordLoadException(long j);

        void recordLoadSuccess(long j);

        void recordMisses(int i);

        CacheStats snapshot();
    }

    @Beta
    public static final class SimpleStatsCounter implements StatsCounter {
        private final LongAddable evictionCount = LongAddables.create();
        private final LongAddable hitCount = LongAddables.create();
        private final LongAddable loadExceptionCount = LongAddables.create();
        private final LongAddable loadSuccessCount = LongAddables.create();
        private final LongAddable missCount = LongAddables.create();
        private final LongAddable totalLoadTime = LongAddables.create();

        public void recordHits(int count) {
            this.hitCount.add((long) count);
        }

        public void recordMisses(int count) {
            this.missCount.add((long) count);
        }

        public void recordLoadSuccess(long loadTime) {
            this.loadSuccessCount.increment();
            this.totalLoadTime.add(loadTime);
        }

        public void recordLoadException(long loadTime) {
            this.loadExceptionCount.increment();
            this.totalLoadTime.add(loadTime);
        }

        public void recordEviction() {
            this.evictionCount.increment();
        }

        public CacheStats snapshot() {
            return new CacheStats(this.hitCount.sum(), this.missCount.sum(), this.loadSuccessCount.sum(), this.loadExceptionCount.sum(), this.totalLoadTime.sum(), this.evictionCount.sum());
        }

        public void incrementBy(StatsCounter other) {
            CacheStats otherStats = other.snapshot();
            this.hitCount.add(otherStats.hitCount());
            this.missCount.add(otherStats.missCount());
            this.loadSuccessCount.add(otherStats.loadSuccessCount());
            this.loadExceptionCount.add(otherStats.loadExceptionCount());
            this.totalLoadTime.add(otherStats.totalLoadTime());
            this.evictionCount.add(otherStats.evictionCount());
        }
    }

    protected AbstractCache() {
    }

    public V get(K k, Callable<? extends V> callable) throws ExecutionException {
        throw new UnsupportedOperationException();
    }

    public ImmutableMap<K, V> getAllPresent(Iterable<?> keys) {
        Map<K, V> result = Maps.newLinkedHashMap();
        for (K key : keys) {
            if (!result.containsKey(key)) {
                K castKey = key;
                V value = getIfPresent(key);
                if (value != null) {
                    result.put(castKey, value);
                }
            }
        }
        return ImmutableMap.copyOf(result);
    }

    public void put(K k, V v) {
        throw new UnsupportedOperationException();
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public void cleanUp() {
    }

    public long size() {
        throw new UnsupportedOperationException();
    }

    public void invalidate(Object key) {
        throw new UnsupportedOperationException();
    }

    public void invalidateAll(Iterable<?> keys) {
        for (Object key : keys) {
            invalidate(key);
        }
    }

    public void invalidateAll() {
        throw new UnsupportedOperationException();
    }

    public CacheStats stats() {
        throw new UnsupportedOperationException();
    }

    public ConcurrentMap<K, V> asMap() {
        throw new UnsupportedOperationException();
    }
}
