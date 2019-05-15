package com.google.common.cache;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Equivalence;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.google.common.cache.AbstractCache.SimpleStatsCounter;
import com.google.common.cache.AbstractCache.StatsCounter;
import com.google.common.cache.CacheLoader.InvalidCacheLoadException;
import com.google.common.collect.AbstractSequentialIterator;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractQueue;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

@GwtCompatible(emulated = true)
class LocalCache<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {
    static final int CONTAINS_VALUE_RETRIES = 3;
    static final Queue<? extends Object> DISCARDING_QUEUE = new AbstractQueue<Object>() {
        public boolean offer(Object o) {
            return true;
        }

        public Object peek() {
            return null;
        }

        public Object poll() {
            return null;
        }

        public int size() {
            return 0;
        }

        public Iterator<Object> iterator() {
            return ImmutableSet.of().iterator();
        }
    };
    static final int DRAIN_MAX = 16;
    static final int DRAIN_THRESHOLD = 63;
    static final int MAXIMUM_CAPACITY = 1073741824;
    static final int MAX_SEGMENTS = 65536;
    static final ValueReference<Object, Object> UNSET = new ValueReference<Object, Object>() {
        public Object get() {
            return null;
        }

        public int getWeight() {
            return 0;
        }

        public ReferenceEntry<Object, Object> getEntry() {
            return null;
        }

        public ValueReference<Object, Object> copyFor(ReferenceQueue<Object> referenceQueue, @Nullable Object value, ReferenceEntry<Object, Object> referenceEntry) {
            return this;
        }

        public boolean isLoading() {
            return false;
        }

        public boolean isActive() {
            return false;
        }

        public Object waitForValue() {
            return null;
        }

        public void notifyNewValue(Object newValue) {
        }
    };
    static final Logger logger = Logger.getLogger(LocalCache.class.getName());
    final int concurrencyLevel;
    @Nullable
    final CacheLoader<? super K, V> defaultLoader;
    final EntryFactory entryFactory;
    Set<Entry<K, V>> entrySet;
    final long expireAfterAccessNanos;
    final long expireAfterWriteNanos;
    final StatsCounter globalStatsCounter;
    final Equivalence<Object> keyEquivalence;
    Set<K> keySet;
    final Strength keyStrength;
    final long maxWeight;
    final long refreshNanos;
    final RemovalListener<K, V> removalListener;
    final Queue<RemovalNotification<K, V>> removalNotificationQueue;
    final int segmentMask;
    final int segmentShift;
    final Segment<K, V>[] segments;
    final Ticker ticker;
    final Equivalence<Object> valueEquivalence;
    final Strength valueStrength;
    Collection<V> values;
    final Weigher<K, V> weigher;

    abstract class AbstractCacheSet<T> extends AbstractSet<T> {
        final ConcurrentMap<?, ?> map;

        AbstractCacheSet(ConcurrentMap<?, ?> map) {
            this.map = map;
        }

        public int size() {
            return this.map.size();
        }

        public boolean isEmpty() {
            return this.map.isEmpty();
        }

        public void clear() {
            this.map.clear();
        }
    }

    static final class AccessQueue<K, V> extends AbstractQueue<ReferenceEntry<K, V>> {
        final ReferenceEntry<K, V> head = new AbstractReferenceEntry<K, V>() {
            ReferenceEntry<K, V> nextAccess = this;
            ReferenceEntry<K, V> previousAccess = this;

            public long getAccessTime() {
                return Long.MAX_VALUE;
            }

            public void setAccessTime(long time) {
            }

            public ReferenceEntry<K, V> getNextInAccessQueue() {
                return this.nextAccess;
            }

            public void setNextInAccessQueue(ReferenceEntry<K, V> next) {
                this.nextAccess = next;
            }

            public ReferenceEntry<K, V> getPreviousInAccessQueue() {
                return this.previousAccess;
            }

            public void setPreviousInAccessQueue(ReferenceEntry<K, V> previous) {
                this.previousAccess = previous;
            }
        };

        AccessQueue() {
        }

        public boolean offer(ReferenceEntry<K, V> entry) {
            LocalCache.connectAccessOrder(entry.getPreviousInAccessQueue(), entry.getNextInAccessQueue());
            LocalCache.connectAccessOrder(this.head.getPreviousInAccessQueue(), entry);
            LocalCache.connectAccessOrder(entry, this.head);
            return true;
        }

        public ReferenceEntry<K, V> peek() {
            ReferenceEntry<K, V> next = this.head.getNextInAccessQueue();
            return next == this.head ? null : next;
        }

        public ReferenceEntry<K, V> poll() {
            ReferenceEntry<K, V> next = this.head.getNextInAccessQueue();
            if (next == this.head) {
                return null;
            }
            remove(next);
            return next;
        }

        public boolean remove(Object o) {
            ReferenceEntry<K, V> e = (ReferenceEntry) o;
            ReferenceEntry<K, V> previous = e.getPreviousInAccessQueue();
            ReferenceEntry<K, V> next = e.getNextInAccessQueue();
            LocalCache.connectAccessOrder(previous, next);
            LocalCache.nullifyAccessOrder(e);
            return next != NullEntry.INSTANCE;
        }

        public boolean contains(Object o) {
            return ((ReferenceEntry) o).getNextInAccessQueue() != NullEntry.INSTANCE;
        }

        public boolean isEmpty() {
            return this.head.getNextInAccessQueue() == this.head;
        }

        public int size() {
            int size = 0;
            for (ReferenceEntry<K, V> e = this.head.getNextInAccessQueue(); e != this.head; e = e.getNextInAccessQueue()) {
                size++;
            }
            return size;
        }

        public void clear() {
            ReferenceEntry<K, V> e = this.head.getNextInAccessQueue();
            while (e != this.head) {
                ReferenceEntry<K, V> next = e.getNextInAccessQueue();
                LocalCache.nullifyAccessOrder(e);
                e = next;
            }
            this.head.setNextInAccessQueue(this.head);
            this.head.setPreviousInAccessQueue(this.head);
        }

        public Iterator<ReferenceEntry<K, V>> iterator() {
            return new AbstractSequentialIterator<ReferenceEntry<K, V>>(peek()) {
                /* Access modifiers changed, original: protected */
                public ReferenceEntry<K, V> computeNext(ReferenceEntry<K, V> previous) {
                    ReferenceEntry<K, V> next = previous.getNextInAccessQueue();
                    return next == AccessQueue.this.head ? null : next;
                }
            };
        }
    }

    enum EntryFactory {
        STRONG {
            /* Access modifiers changed, original: 0000 */
            public <K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
                return new StrongEntry(key, hash, next);
            }
        },
        STRONG_ACCESS {
            /* Access modifiers changed, original: 0000 */
            public <K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
                return new StrongAccessEntry(key, hash, next);
            }

            /* Access modifiers changed, original: 0000 */
            public <K, V> ReferenceEntry<K, V> copyEntry(Segment<K, V> segment, ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
                ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
                copyAccessEntry(original, newEntry);
                return newEntry;
            }
        },
        STRONG_WRITE {
            /* Access modifiers changed, original: 0000 */
            public <K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
                return new StrongWriteEntry(key, hash, next);
            }

            /* Access modifiers changed, original: 0000 */
            public <K, V> ReferenceEntry<K, V> copyEntry(Segment<K, V> segment, ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
                ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
                copyWriteEntry(original, newEntry);
                return newEntry;
            }
        },
        STRONG_ACCESS_WRITE {
            /* Access modifiers changed, original: 0000 */
            public <K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
                return new StrongAccessWriteEntry(key, hash, next);
            }

            /* Access modifiers changed, original: 0000 */
            public <K, V> ReferenceEntry<K, V> copyEntry(Segment<K, V> segment, ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
                ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
                copyAccessEntry(original, newEntry);
                copyWriteEntry(original, newEntry);
                return newEntry;
            }
        },
        WEAK {
            /* Access modifiers changed, original: 0000 */
            public <K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
                return new WeakEntry(segment.keyReferenceQueue, key, hash, next);
            }
        },
        WEAK_ACCESS {
            /* Access modifiers changed, original: 0000 */
            public <K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
                return new WeakAccessEntry(segment.keyReferenceQueue, key, hash, next);
            }

            /* Access modifiers changed, original: 0000 */
            public <K, V> ReferenceEntry<K, V> copyEntry(Segment<K, V> segment, ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
                ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
                copyAccessEntry(original, newEntry);
                return newEntry;
            }
        },
        WEAK_WRITE {
            /* Access modifiers changed, original: 0000 */
            public <K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
                return new WeakWriteEntry(segment.keyReferenceQueue, key, hash, next);
            }

            /* Access modifiers changed, original: 0000 */
            public <K, V> ReferenceEntry<K, V> copyEntry(Segment<K, V> segment, ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
                ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
                copyWriteEntry(original, newEntry);
                return newEntry;
            }
        },
        WEAK_ACCESS_WRITE {
            /* Access modifiers changed, original: 0000 */
            public <K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
                return new WeakAccessWriteEntry(segment.keyReferenceQueue, key, hash, next);
            }

            /* Access modifiers changed, original: 0000 */
            public <K, V> ReferenceEntry<K, V> copyEntry(Segment<K, V> segment, ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
                ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
                copyAccessEntry(original, newEntry);
                copyWriteEntry(original, newEntry);
                return newEntry;
            }
        };
        
        static final int ACCESS_MASK = 1;
        static final int WEAK_MASK = 4;
        static final int WRITE_MASK = 2;
        static final EntryFactory[] factories = null;

        public abstract <K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K k, int i, @Nullable ReferenceEntry<K, V> referenceEntry);

        static {
            factories = new EntryFactory[]{STRONG, STRONG_ACCESS, STRONG_WRITE, STRONG_ACCESS_WRITE, WEAK, WEAK_ACCESS, WEAK_WRITE, WEAK_ACCESS_WRITE};
        }

        static EntryFactory getFactory(Strength keyStrength, boolean usesAccessQueue, boolean usesWriteQueue) {
            int flags;
            int i = 0;
            if (keyStrength == Strength.WEAK) {
                flags = 4;
            } else {
                flags = 0;
            }
            flags |= usesAccessQueue;
            if (usesWriteQueue) {
                i = 2;
            }
            return factories[flags | i];
        }

        /* Access modifiers changed, original: 0000 */
        public <K, V> ReferenceEntry<K, V> copyEntry(Segment<K, V> segment, ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
            return newEntry(segment, original.getKey(), original.getHash(), newNext);
        }

        /* Access modifiers changed, original: 0000 */
        public <K, V> void copyAccessEntry(ReferenceEntry<K, V> original, ReferenceEntry<K, V> newEntry) {
            newEntry.setAccessTime(original.getAccessTime());
            LocalCache.connectAccessOrder(original.getPreviousInAccessQueue(), newEntry);
            LocalCache.connectAccessOrder(newEntry, original.getNextInAccessQueue());
            LocalCache.nullifyAccessOrder(original);
        }

        /* Access modifiers changed, original: 0000 */
        public <K, V> void copyWriteEntry(ReferenceEntry<K, V> original, ReferenceEntry<K, V> newEntry) {
            newEntry.setWriteTime(original.getWriteTime());
            LocalCache.connectWriteOrder(original.getPreviousInWriteQueue(), newEntry);
            LocalCache.connectWriteOrder(newEntry, original.getNextInWriteQueue());
            LocalCache.nullifyWriteOrder(original);
        }
    }

    abstract class HashIterator<T> implements Iterator<T> {
        Segment<K, V> currentSegment;
        AtomicReferenceArray<ReferenceEntry<K, V>> currentTable;
        WriteThroughEntry lastReturned;
        ReferenceEntry<K, V> nextEntry;
        WriteThroughEntry nextExternal;
        int nextSegmentIndex;
        int nextTableIndex = -1;

        public abstract T next();

        HashIterator() {
            this.nextSegmentIndex = LocalCache.this.segments.length - 1;
            advance();
        }

        /* Access modifiers changed, original: final */
        public final void advance() {
            this.nextExternal = null;
            if (!nextInChain() && !nextInTable()) {
                while (this.nextSegmentIndex >= 0) {
                    Segment[] segmentArr = LocalCache.this.segments;
                    int i = this.nextSegmentIndex;
                    this.nextSegmentIndex = i - 1;
                    this.currentSegment = segmentArr[i];
                    if (this.currentSegment.count != 0) {
                        this.currentTable = this.currentSegment.table;
                        this.nextTableIndex = this.currentTable.length() - 1;
                        if (nextInTable()) {
                            return;
                        }
                    }
                }
            }
        }

        /* Access modifiers changed, original: 0000 */
        public boolean nextInChain() {
            if (this.nextEntry != null) {
                do {
                    this.nextEntry = this.nextEntry.getNext();
                    if (this.nextEntry != null) {
                    }
                } while (!advanceTo(this.nextEntry));
                return true;
            }
            return false;
        }

        /* Access modifiers changed, original: 0000 */
        public boolean nextInTable() {
            while (this.nextTableIndex >= 0) {
                AtomicReferenceArray atomicReferenceArray = this.currentTable;
                int i = this.nextTableIndex;
                this.nextTableIndex = i - 1;
                ReferenceEntry referenceEntry = (ReferenceEntry) atomicReferenceArray.get(i);
                this.nextEntry = referenceEntry;
                if (referenceEntry != null && (advanceTo(this.nextEntry) || nextInChain())) {
                    return true;
                }
            }
            return false;
        }

        /* Access modifiers changed, original: 0000 */
        public boolean advanceTo(ReferenceEntry<K, V> entry) {
            try {
                long now = LocalCache.this.ticker.read();
                K key = entry.getKey();
                V value = LocalCache.this.getLiveValue(entry, now);
                if (value != null) {
                    this.nextExternal = new WriteThroughEntry(key, value);
                    return true;
                }
                this.currentSegment.postReadCleanup();
                return false;
            } finally {
                this.currentSegment.postReadCleanup();
            }
        }

        public boolean hasNext() {
            return this.nextExternal != null;
        }

        /* Access modifiers changed, original: 0000 */
        public WriteThroughEntry nextEntry() {
            if (this.nextExternal != null) {
                this.lastReturned = this.nextExternal;
                advance();
                return this.lastReturned;
            }
            throw new NoSuchElementException();
        }

        public void remove() {
            Preconditions.checkState(this.lastReturned != null);
            LocalCache.this.remove(this.lastReturned.getKey());
            this.lastReturned = null;
        }
    }

    interface ReferenceEntry<K, V> {
        long getAccessTime();

        int getHash();

        @Nullable
        K getKey();

        @Nullable
        ReferenceEntry<K, V> getNext();

        ReferenceEntry<K, V> getNextInAccessQueue();

        ReferenceEntry<K, V> getNextInWriteQueue();

        ReferenceEntry<K, V> getPreviousInAccessQueue();

        ReferenceEntry<K, V> getPreviousInWriteQueue();

        ValueReference<K, V> getValueReference();

        long getWriteTime();

        void setAccessTime(long j);

        void setNextInAccessQueue(ReferenceEntry<K, V> referenceEntry);

        void setNextInWriteQueue(ReferenceEntry<K, V> referenceEntry);

        void setPreviousInAccessQueue(ReferenceEntry<K, V> referenceEntry);

        void setPreviousInWriteQueue(ReferenceEntry<K, V> referenceEntry);

        void setValueReference(ValueReference<K, V> valueReference);

        void setWriteTime(long j);
    }

    static class Segment<K, V> extends ReentrantLock {
        @GuardedBy("this")
        final Queue<ReferenceEntry<K, V>> accessQueue;
        volatile int count;
        final ReferenceQueue<K> keyReferenceQueue;
        final LocalCache<K, V> map;
        final long maxSegmentWeight;
        int modCount;
        final AtomicInteger readCount = new AtomicInteger();
        final Queue<ReferenceEntry<K, V>> recencyQueue;
        final StatsCounter statsCounter;
        volatile AtomicReferenceArray<ReferenceEntry<K, V>> table;
        int threshold;
        @GuardedBy("this")
        long totalWeight;
        final ReferenceQueue<V> valueReferenceQueue;
        @GuardedBy("this")
        final Queue<ReferenceEntry<K, V>> writeQueue;

        Segment(LocalCache<K, V> map, int initialCapacity, long maxSegmentWeight, StatsCounter statsCounter) {
            Queue concurrentLinkedQueue;
            this.map = map;
            this.maxSegmentWeight = maxSegmentWeight;
            this.statsCounter = (StatsCounter) Preconditions.checkNotNull(statsCounter);
            initTable(newEntryArray(initialCapacity));
            ReferenceQueue referenceQueue = null;
            this.keyReferenceQueue = map.usesKeyReferences() ? new ReferenceQueue() : null;
            if (map.usesValueReferences()) {
                referenceQueue = new ReferenceQueue();
            }
            this.valueReferenceQueue = referenceQueue;
            if (map.usesAccessQueue()) {
                concurrentLinkedQueue = new ConcurrentLinkedQueue();
            } else {
                concurrentLinkedQueue = LocalCache.discardingQueue();
            }
            this.recencyQueue = concurrentLinkedQueue;
            if (map.usesWriteQueue()) {
                concurrentLinkedQueue = new WriteQueue();
            } else {
                concurrentLinkedQueue = LocalCache.discardingQueue();
            }
            this.writeQueue = concurrentLinkedQueue;
            if (map.usesAccessQueue()) {
                concurrentLinkedQueue = new AccessQueue();
            } else {
                concurrentLinkedQueue = LocalCache.discardingQueue();
            }
            this.accessQueue = concurrentLinkedQueue;
        }

        /* Access modifiers changed, original: 0000 */
        public AtomicReferenceArray<ReferenceEntry<K, V>> newEntryArray(int size) {
            return new AtomicReferenceArray(size);
        }

        /* Access modifiers changed, original: 0000 */
        public void initTable(AtomicReferenceArray<ReferenceEntry<K, V>> newTable) {
            this.threshold = (newTable.length() * 3) / 4;
            if (!this.map.customWeigher() && ((long) this.threshold) == this.maxSegmentWeight) {
                this.threshold++;
            }
            this.table = newTable;
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("this")
        public ReferenceEntry<K, V> newEntry(K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            return this.map.entryFactory.newEntry(this, Preconditions.checkNotNull(key), hash, next);
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("this")
        public ReferenceEntry<K, V> copyEntry(ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
            if (original.getKey() == null) {
                return null;
            }
            ValueReference<K, V> valueReference = original.getValueReference();
            V value = valueReference.get();
            if (value == null && valueReference.isActive()) {
                return null;
            }
            ReferenceEntry<K, V> newEntry = this.map.entryFactory.copyEntry(this, original, newNext);
            newEntry.setValueReference(valueReference.copyFor(this.valueReferenceQueue, value, newEntry));
            return newEntry;
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("this")
        public void setValue(ReferenceEntry<K, V> entry, K key, V value, long now) {
            ValueReference<K, V> previous = entry.getValueReference();
            int weight = this.map.weigher.weigh(key, value);
            Preconditions.checkState(weight >= 0, "Weights must be non-negative");
            entry.setValueReference(this.map.valueStrength.referenceValue(this, entry, value, weight));
            recordWrite(entry, weight, now);
            previous.notifyNewValue(value);
        }

        /* Access modifiers changed, original: 0000 */
        public V get(K key, int hash, CacheLoader<? super K, V> loader) throws ExecutionException {
            Preconditions.checkNotNull(key);
            Preconditions.checkNotNull(loader);
            try {
                if (this.count != 0) {
                    ReferenceEntry<K, V> e = getEntry(key, hash);
                    if (e != null) {
                        long now = this.map.ticker.read();
                        V value = getLiveValue(e, now);
                        if (value != null) {
                            recordRead(e, now);
                            this.statsCounter.recordHits(1);
                            Object scheduleRefresh = scheduleRefresh(e, key, hash, value, now, loader);
                            postReadCleanup();
                            return scheduleRefresh;
                        }
                        ValueReference<K, V> valueReference = e.getValueReference();
                        if (valueReference.isLoading()) {
                            Object waitForLoadingValue = waitForLoadingValue(e, key, valueReference);
                            postReadCleanup();
                            return waitForLoadingValue;
                        }
                    }
                }
                Object lockedGetOrLoad = lockedGetOrLoad(key, hash, loader);
                postReadCleanup();
                return lockedGetOrLoad;
            } catch (ExecutionException ee) {
                Throwable cause = ee.getCause();
                if (cause instanceof Error) {
                    throw new ExecutionError((Error) cause);
                } else if (cause instanceof RuntimeException) {
                    throw new UncheckedExecutionException(cause);
                } else {
                    throw ee;
                }
            } catch (Throwable th) {
                postReadCleanup();
            }
        }

        /* Access modifiers changed, original: 0000 */
        /* JADX WARNING: Removed duplicated region for block: B:39:0x009d A:{Catch:{ all -> 0x00e3 }} */
        /* JADX WARNING: Removed duplicated region for block: B:63:0x00dc  */
        /* JADX WARNING: Removed duplicated region for block: B:45:0x00be A:{SYNTHETIC, Splitter:B:45:0x00be} */
        /* JADX WARNING: Missing block: B:58:0x00d0, code skipped:
            r0 = th;
     */
        public V lockedGetOrLoad(K r18, int r19, com.google.common.cache.CacheLoader<? super K, V> r20) throws java.util.concurrent.ExecutionException {
            /*
            r17 = this;
            r1 = r17;
            r2 = r18;
            r3 = r19;
            r4 = 0;
            r5 = 0;
            r6 = 1;
            r17.lock();
            r0 = r1.map;	 Catch:{ all -> 0x00e3 }
            r0 = r0.ticker;	 Catch:{ all -> 0x00e3 }
            r7 = r0.read();	 Catch:{ all -> 0x00e3 }
            r1.preWriteCleanup(r7);	 Catch:{ all -> 0x00e3 }
            r0 = r1.count;	 Catch:{ all -> 0x00e3 }
            r9 = 1;
            r0 = r0 - r9;
            r10 = r1.table;	 Catch:{ all -> 0x00e3 }
            r11 = r10.length();	 Catch:{ all -> 0x00e3 }
            r11 = r11 - r9;
            r11 = r11 & r3;
            r12 = r10.get(r11);	 Catch:{ all -> 0x00e3 }
            r12 = (com.google.common.cache.LocalCache.ReferenceEntry) r12;	 Catch:{ all -> 0x00e3 }
            r13 = r12;
        L_0x002a:
            if (r13 == 0) goto L_0x009b;
        L_0x002c:
            r14 = r13.getKey();	 Catch:{ all -> 0x00e3 }
            r15 = r13.getHash();	 Catch:{ all -> 0x00e3 }
            if (r15 != r3) goto L_0x0094;
        L_0x0036:
            if (r14 == 0) goto L_0x0094;
        L_0x0038:
            r15 = r1.map;	 Catch:{ all -> 0x00e3 }
            r15 = r15.keyEquivalence;	 Catch:{ all -> 0x00e3 }
            r15 = r15.equivalent(r2, r14);	 Catch:{ all -> 0x00e3 }
            if (r15 == 0) goto L_0x0094;
        L_0x0042:
            r15 = r13.getValueReference();	 Catch:{ all -> 0x00e3 }
            r4 = r15;
            r15 = r4.isLoading();	 Catch:{ all -> 0x008e }
            if (r15 == 0) goto L_0x004f;
        L_0x004d:
            r6 = 0;
            goto L_0x009b;
        L_0x004f:
            r15 = r4.get();	 Catch:{ all -> 0x008e }
            if (r15 != 0) goto L_0x005b;
        L_0x0055:
            r9 = com.google.common.cache.RemovalCause.COLLECTED;	 Catch:{ all -> 0x00e3 }
            r1.enqueueNotification(r14, r3, r4, r9);	 Catch:{ all -> 0x00e3 }
            goto L_0x0068;
        L_0x005b:
            r9 = r1.map;	 Catch:{ all -> 0x008e }
            r9 = r9.isExpired(r13, r7);	 Catch:{ all -> 0x008e }
            if (r9 == 0) goto L_0x0075;
        L_0x0063:
            r9 = com.google.common.cache.RemovalCause.EXPIRED;	 Catch:{ all -> 0x00e3 }
            r1.enqueueNotification(r14, r3, r4, r9);	 Catch:{ all -> 0x00e3 }
        L_0x0068:
            r9 = r1.writeQueue;	 Catch:{ all -> 0x00e3 }
            r9.remove(r13);	 Catch:{ all -> 0x00e3 }
            r9 = r1.accessQueue;	 Catch:{ all -> 0x00e3 }
            r9.remove(r13);	 Catch:{ all -> 0x00e3 }
            r1.count = r0;	 Catch:{ all -> 0x00e3 }
            goto L_0x009b;
        L_0x0075:
            r1.recordLockedRead(r13, r7);	 Catch:{ all -> 0x008e }
            r9 = r1.statsCounter;	 Catch:{ all -> 0x008e }
            r16 = r4;
            r4 = 1;
            r9.recordHits(r4);	 Catch:{ all -> 0x0088 }
            r17.unlock();
            r17.postWriteCleanup();
            return r15;
        L_0x0088:
            r0 = move-exception;
            r8 = r20;
            r4 = r16;
            goto L_0x00e6;
        L_0x008e:
            r0 = move-exception;
            r16 = r4;
            r8 = r20;
            goto L_0x00e6;
        L_0x0094:
            r9 = r13.getNext();	 Catch:{ all -> 0x00e3 }
            r13 = r9;
            r9 = 1;
            goto L_0x002a;
        L_0x009b:
            if (r6 == 0) goto L_0x00b4;
        L_0x009d:
            r9 = new com.google.common.cache.LocalCache$LoadingValueReference;	 Catch:{ all -> 0x00e3 }
            r9.<init>();	 Catch:{ all -> 0x00e3 }
            r5 = r9;
            if (r13 != 0) goto L_0x00b1;
        L_0x00a5:
            r9 = r1.newEntry(r2, r3, r12);	 Catch:{ all -> 0x00e3 }
            r13 = r9;
            r13.setValueReference(r5);	 Catch:{ all -> 0x00e3 }
            r10.set(r11, r13);	 Catch:{ all -> 0x00e3 }
            goto L_0x00b4;
        L_0x00b1:
            r13.setValueReference(r5);	 Catch:{ all -> 0x00e3 }
        L_0x00b4:
            r17.unlock();
            r17.postWriteCleanup();
            r7 = r13;
            if (r6 == 0) goto L_0x00dc;
        L_0x00be:
            monitor-enter(r7);	 Catch:{ all -> 0x00d2 }
            r8 = r20;
            r0 = r1.loadSync(r2, r3, r5, r8);	 Catch:{ all -> 0x00cd }
            monitor-exit(r7);	 Catch:{ all -> 0x00cd }
            r9 = r1.statsCounter;
            r10 = 1;
            r9.recordMisses(r10);
            return r0;
        L_0x00cd:
            r0 = move-exception;
            monitor-exit(r7);	 Catch:{ all -> 0x00cd }
            throw r0;	 Catch:{ all -> 0x00d0 }
        L_0x00d0:
            r0 = move-exception;
            goto L_0x00d5;
        L_0x00d2:
            r0 = move-exception;
            r8 = r20;
        L_0x00d5:
            r9 = r1.statsCounter;
            r10 = 1;
            r9.recordMisses(r10);
            throw r0;
        L_0x00dc:
            r8 = r20;
            r0 = r1.waitForLoadingValue(r7, r2, r4);
            return r0;
        L_0x00e3:
            r0 = move-exception;
            r8 = r20;
        L_0x00e6:
            r17.unlock();
            r17.postWriteCleanup();
            throw r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.google.common.cache.LocalCache$Segment.lockedGetOrLoad(java.lang.Object, int, com.google.common.cache.CacheLoader):java.lang.Object");
        }

        /* Access modifiers changed, original: 0000 */
        public V waitForLoadingValue(ReferenceEntry<K, V> e, K key, ValueReference<K, V> valueReference) throws ExecutionException {
            if (valueReference.isLoading()) {
                Preconditions.checkState(Thread.holdsLock(e) ^ 1, "Recursive load of: %s", key);
                try {
                    V value = valueReference.waitForValue();
                    if (value != null) {
                        recordRead(e, this.map.ticker.read());
                        return value;
                    }
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("CacheLoader returned null for key ");
                    stringBuilder.append(key);
                    stringBuilder.append(".");
                    throw new InvalidCacheLoadException(stringBuilder.toString());
                } finally {
                    this.statsCounter.recordMisses(1);
                }
            } else {
                throw new AssertionError();
            }
        }

        /* Access modifiers changed, original: 0000 */
        public V loadSync(K key, int hash, LoadingValueReference<K, V> loadingValueReference, CacheLoader<? super K, V> loader) throws ExecutionException {
            return getAndRecordStats(key, hash, loadingValueReference, loadingValueReference.loadFuture(key, loader));
        }

        /* Access modifiers changed, original: 0000 */
        public ListenableFuture<V> loadAsync(K key, int hash, LoadingValueReference<K, V> loadingValueReference, CacheLoader<? super K, V> loader) {
            ListenableFuture<V> loadingFuture = loadingValueReference.loadFuture(key, loader);
            final K k = key;
            final int i = hash;
            final LoadingValueReference<K, V> loadingValueReference2 = loadingValueReference;
            final ListenableFuture<V> listenableFuture = loadingFuture;
            loadingFuture.addListener(new Runnable() {
                public void run() {
                    try {
                        Segment.this.getAndRecordStats(k, i, loadingValueReference2, listenableFuture);
                    } catch (Throwable t) {
                        LocalCache.logger.log(Level.WARNING, "Exception thrown during refresh", t);
                        loadingValueReference2.setException(t);
                    }
                }
            }, MoreExecutors.directExecutor());
            return loadingFuture;
        }

        /* Access modifiers changed, original: 0000 */
        public V getAndRecordStats(K key, int hash, LoadingValueReference<K, V> loadingValueReference, ListenableFuture<V> newValue) throws ExecutionException {
            V value = null;
            try {
                value = Uninterruptibles.getUninterruptibly(newValue);
                if (value != null) {
                    this.statsCounter.recordLoadSuccess(loadingValueReference.elapsedNanos());
                    storeLoadedValue(key, hash, loadingValueReference, value);
                    return value;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("CacheLoader returned null for key ");
                stringBuilder.append(key);
                stringBuilder.append(".");
                throw new InvalidCacheLoadException(stringBuilder.toString());
            } finally {
                if (value == null) {
                    this.statsCounter.recordLoadException(loadingValueReference.elapsedNanos());
                    removeLoadingValue(key, hash, loadingValueReference);
                }
            }
        }

        /* Access modifiers changed, original: 0000 */
        public V scheduleRefresh(ReferenceEntry<K, V> entry, K key, int hash, V oldValue, long now, CacheLoader<? super K, V> loader) {
            if (this.map.refreshes() && now - entry.getWriteTime() > this.map.refreshNanos && !entry.getValueReference().isLoading()) {
                V newValue = refresh(key, hash, loader, true);
                if (newValue != null) {
                    return newValue;
                }
            }
            return oldValue;
        }

        /* Access modifiers changed, original: 0000 */
        @Nullable
        public V refresh(K key, int hash, CacheLoader<? super K, V> loader, boolean checkTime) {
            LoadingValueReference<K, V> loadingValueReference = insertLoadingValueReference(key, hash, checkTime);
            if (loadingValueReference == null) {
                return null;
            }
            ListenableFuture<V> result = loadAsync(key, hash, loadingValueReference, loader);
            if (result.isDone()) {
                try {
                    return Uninterruptibles.getUninterruptibly(result);
                } catch (Throwable th) {
                }
            }
            return null;
        }

        /* Access modifiers changed, original: 0000 */
        @Nullable
        public LoadingValueReference<K, V> insertLoadingValueReference(K key, int hash, boolean checkTime) {
            lock();
            try {
                long now = this.map.ticker.read();
                preWriteCleanup(now);
                AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
                int index = (table.length() - 1) & hash;
                ReferenceEntry<K, V> first = (ReferenceEntry) table.get(index);
                ReferenceEntry<K, V> e = first;
                while (e != null) {
                    K entryKey = e.getKey();
                    if (e.getHash() == hash && entryKey != null && this.map.keyEquivalence.equivalent(key, entryKey)) {
                        ValueReference<K, V> valueReference = e.getValueReference();
                        if (!valueReference.isLoading()) {
                            if (!checkTime || now - e.getWriteTime() >= this.map.refreshNanos) {
                                this.modCount++;
                                LoadingValueReference<K, V> loadingValueReference = new LoadingValueReference(valueReference);
                                e.setValueReference(loadingValueReference);
                                unlock();
                                postWriteCleanup();
                                return loadingValueReference;
                            }
                        }
                        unlock();
                        postWriteCleanup();
                        return null;
                    }
                    e = e.getNext();
                }
                this.modCount++;
                LoadingValueReference<K, V> loadingValueReference2 = new LoadingValueReference();
                e = newEntry(key, hash, first);
                e.setValueReference(loadingValueReference2);
                table.set(index, e);
                unlock();
                postWriteCleanup();
                return loadingValueReference2;
            } catch (Throwable th) {
                unlock();
                postWriteCleanup();
            }
        }

        /* Access modifiers changed, original: 0000 */
        public void tryDrainReferenceQueues() {
            if (tryLock()) {
                try {
                    drainReferenceQueues();
                } finally {
                    unlock();
                }
            }
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("this")
        public void drainReferenceQueues() {
            if (this.map.usesKeyReferences()) {
                drainKeyReferenceQueue();
            }
            if (this.map.usesValueReferences()) {
                drainValueReferenceQueue();
            }
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("this")
        public void drainKeyReferenceQueue() {
            int i = 0;
            while (true) {
                Reference<? extends K> poll = this.keyReferenceQueue.poll();
                Reference<? extends K> ref = poll;
                if (poll != null) {
                    this.map.reclaimKey((ReferenceEntry) ref);
                    i++;
                    if (i == 16) {
                        return;
                    }
                } else {
                    return;
                }
            }
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("this")
        public void drainValueReferenceQueue() {
            int i = 0;
            while (true) {
                Reference<? extends V> poll = this.valueReferenceQueue.poll();
                Reference<? extends V> ref = poll;
                if (poll != null) {
                    this.map.reclaimValue((ValueReference) ref);
                    i++;
                    if (i == 16) {
                        return;
                    }
                } else {
                    return;
                }
            }
        }

        /* Access modifiers changed, original: 0000 */
        public void clearReferenceQueues() {
            if (this.map.usesKeyReferences()) {
                clearKeyReferenceQueue();
            }
            if (this.map.usesValueReferences()) {
                clearValueReferenceQueue();
            }
        }

        /* Access modifiers changed, original: 0000 */
        public void clearKeyReferenceQueue() {
            while (this.keyReferenceQueue.poll() != null) {
            }
        }

        /* Access modifiers changed, original: 0000 */
        public void clearValueReferenceQueue() {
            while (this.valueReferenceQueue.poll() != null) {
            }
        }

        /* Access modifiers changed, original: 0000 */
        public void recordRead(ReferenceEntry<K, V> entry, long now) {
            if (this.map.recordsAccess()) {
                entry.setAccessTime(now);
            }
            this.recencyQueue.add(entry);
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("this")
        public void recordLockedRead(ReferenceEntry<K, V> entry, long now) {
            if (this.map.recordsAccess()) {
                entry.setAccessTime(now);
            }
            this.accessQueue.add(entry);
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("this")
        public void recordWrite(ReferenceEntry<K, V> entry, int weight, long now) {
            drainRecencyQueue();
            this.totalWeight += (long) weight;
            if (this.map.recordsAccess()) {
                entry.setAccessTime(now);
            }
            if (this.map.recordsWrite()) {
                entry.setWriteTime(now);
            }
            this.accessQueue.add(entry);
            this.writeQueue.add(entry);
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("this")
        public void drainRecencyQueue() {
            while (true) {
                ReferenceEntry<K, V> referenceEntry = (ReferenceEntry) this.recencyQueue.poll();
                ReferenceEntry<K, V> e = referenceEntry;
                if (referenceEntry == null) {
                    return;
                }
                if (this.accessQueue.contains(e)) {
                    this.accessQueue.add(e);
                }
            }
        }

        /* Access modifiers changed, original: 0000 */
        public void tryExpireEntries(long now) {
            if (tryLock()) {
                try {
                    expireEntries(now);
                } finally {
                    unlock();
                }
            }
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("this")
        public void expireEntries(long now) {
            ReferenceEntry<K, V> referenceEntry;
            ReferenceEntry<K, V> e;
            drainRecencyQueue();
            while (true) {
                referenceEntry = (ReferenceEntry) this.writeQueue.peek();
                e = referenceEntry;
                if (referenceEntry == null || !this.map.isExpired(e, now)) {
                    while (true) {
                        referenceEntry = (ReferenceEntry) this.accessQueue.peek();
                        e = referenceEntry;
                    }
                } else if (!removeEntry(e, e.getHash(), RemovalCause.EXPIRED)) {
                    throw new AssertionError();
                }
            }
            while (true) {
                referenceEntry = (ReferenceEntry) this.accessQueue.peek();
                e = referenceEntry;
                if (referenceEntry != null && this.map.isExpired(e, now)) {
                    if (!removeEntry(e, e.getHash(), RemovalCause.EXPIRED)) {
                        throw new AssertionError();
                    }
                } else {
                    return;
                }
            }
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("this")
        public void enqueueNotification(ReferenceEntry<K, V> entry, RemovalCause cause) {
            enqueueNotification(entry.getKey(), entry.getHash(), entry.getValueReference(), cause);
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("this")
        public void enqueueNotification(@Nullable K key, int hash, ValueReference<K, V> valueReference, RemovalCause cause) {
            this.totalWeight -= (long) valueReference.getWeight();
            if (cause.wasEvicted()) {
                this.statsCounter.recordEviction();
            }
            if (this.map.removalNotificationQueue != LocalCache.DISCARDING_QUEUE) {
                this.map.removalNotificationQueue.offer(new RemovalNotification(key, valueReference.get(), cause));
            }
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("this")
        public void evictEntries() {
            if (this.map.evictsBySize()) {
                drainRecencyQueue();
                while (this.totalWeight > this.maxSegmentWeight) {
                    ReferenceEntry<K, V> e = getNextEvictable();
                    if (!removeEntry(e, e.getHash(), RemovalCause.SIZE)) {
                        throw new AssertionError();
                    }
                }
            }
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("this")
        public ReferenceEntry<K, V> getNextEvictable() {
            for (ReferenceEntry<K, V> e : this.accessQueue) {
                if (e.getValueReference().getWeight() > 0) {
                    return e;
                }
            }
            throw new AssertionError();
        }

        /* Access modifiers changed, original: 0000 */
        public ReferenceEntry<K, V> getFirst(int hash) {
            AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
            return (ReferenceEntry) table.get((table.length() - 1) & hash);
        }

        /* Access modifiers changed, original: 0000 */
        @Nullable
        public ReferenceEntry<K, V> getEntry(Object key, int hash) {
            for (ReferenceEntry<K, V> e = getFirst(hash); e != null; e = e.getNext()) {
                if (e.getHash() == hash) {
                    K entryKey = e.getKey();
                    if (entryKey == null) {
                        tryDrainReferenceQueues();
                    } else if (this.map.keyEquivalence.equivalent(key, entryKey)) {
                        return e;
                    }
                }
            }
            return null;
        }

        /* Access modifiers changed, original: 0000 */
        @Nullable
        public ReferenceEntry<K, V> getLiveEntry(Object key, int hash, long now) {
            ReferenceEntry<K, V> e = getEntry(key, hash);
            if (e == null) {
                return null;
            }
            if (!this.map.isExpired(e, now)) {
                return e;
            }
            tryExpireEntries(now);
            return null;
        }

        /* Access modifiers changed, original: 0000 */
        public V getLiveValue(ReferenceEntry<K, V> entry, long now) {
            if (entry.getKey() == null) {
                tryDrainReferenceQueues();
                return null;
            }
            V value = entry.getValueReference().get();
            if (value == null) {
                tryDrainReferenceQueues();
                return null;
            } else if (!this.map.isExpired(entry, now)) {
                return value;
            } else {
                tryExpireEntries(now);
                return null;
            }
        }

        /* Access modifiers changed, original: 0000 */
        @Nullable
        public V get(Object key, int hash) {
            try {
                V v = null;
                if (this.count != 0) {
                    long now = this.map.ticker.read();
                    ReferenceEntry<K, V> e = getLiveEntry(key, hash, now);
                    if (e == null) {
                        return v;
                    }
                    V value = e.getValueReference().get();
                    if (value != null) {
                        recordRead(e, now);
                        Object scheduleRefresh = scheduleRefresh(e, e.getKey(), hash, value, now, this.map.defaultLoader);
                        postReadCleanup();
                        return scheduleRefresh;
                    }
                    tryDrainReferenceQueues();
                }
                postReadCleanup();
                return v;
            } finally {
                postReadCleanup();
            }
        }

        /* Access modifiers changed, original: 0000 */
        public boolean containsKey(Object key, int hash) {
            try {
                boolean z = false;
                if (this.count != 0) {
                    ReferenceEntry<K, V> e = getLiveEntry(key, hash, this.map.ticker.read());
                    if (e == null) {
                        return z;
                    }
                    if (e.getValueReference().get() != null) {
                        z = true;
                    }
                    postReadCleanup();
                    return z;
                }
                postReadCleanup();
                return z;
            } finally {
                postReadCleanup();
            }
        }

        /* Access modifiers changed, original: 0000 */
        @VisibleForTesting
        public boolean containsValue(Object value) {
            try {
                if (this.count != 0) {
                    long now = this.map.ticker.read();
                    AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
                    int length = table.length();
                    for (int i = 0; i < length; i++) {
                        for (ReferenceEntry<K, V> e = (ReferenceEntry) table.get(i); e != null; e = e.getNext()) {
                            V entryValue = getLiveValue(e, now);
                            if (entryValue != null) {
                                if (this.map.valueEquivalence.equivalent(value, entryValue)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
                postReadCleanup();
                return false;
            } finally {
                postReadCleanup();
            }
        }

        /* Access modifiers changed, original: 0000 */
        /* JADX WARNING: Missing block: B:13:0x0053, code skipped:
            r6 = r15.getValueReference();
            r17 = r6.get();
     */
        /* JADX WARNING: Missing block: B:14:0x005e, code skipped:
            if (r17 != null) goto L_0x00a1;
     */
        /* JADX WARNING: Missing block: B:15:0x0060, code skipped:
            r7.modCount++;
     */
        /* JADX WARNING: Missing block: B:16:0x006a, code skipped:
            if (r6.isActive() == false) goto L_0x0082;
     */
        /* JADX WARNING: Missing block: B:17:0x006c, code skipped:
            enqueueNotification(r8, r9, r6, com.google.common.cache.RemovalCause.COLLECTED);
            r19 = r0;
            r18 = r5;
            r0 = r6;
            setValue(r15, r8, r24, r10);
            r1 = r7.count;
     */
        /* JADX WARNING: Missing block: B:18:0x0082, code skipped:
            r19 = r0;
            r18 = r5;
            r0 = r6;
            setValue(r15, r8, r24, r10);
            r1 = r7.count + 1;
     */
        /* JADX WARNING: Missing block: B:19:0x0094, code skipped:
            r7.count = r1;
            evictEntries();
     */
        /* JADX WARNING: Missing block: B:21:0x00a1, code skipped:
            r19 = r0;
            r18 = r5;
            r0 = r6;
     */
        /* JADX WARNING: Missing block: B:22:0x00a6, code skipped:
            if (r25 == false) goto L_0x00b3;
     */
        /* JADX WARNING: Missing block: B:24:?, code skipped:
            recordLockedRead(r15, r10);
     */
        /* JADX WARNING: Missing block: B:25:0x00ab, code skipped:
            unlock();
            postWriteCleanup();
     */
        /* JADX WARNING: Missing block: B:26:0x00b2, code skipped:
            return r17;
     */
        /* JADX WARNING: Missing block: B:28:?, code skipped:
            r7.modCount++;
            enqueueNotification(r8, r9, r0, com.google.common.cache.RemovalCause.REPLACED);
            setValue(r15, r8, r24, r10);
            evictEntries();
     */
        /* JADX WARNING: Missing block: B:29:0x00ca, code skipped:
            unlock();
            postWriteCleanup();
     */
        /* JADX WARNING: Missing block: B:30:0x00d1, code skipped:
            return r17;
     */
        @javax.annotation.Nullable
        public V put(K r22, int r23, V r24, boolean r25) {
            /*
            r21 = this;
            r7 = r21;
            r8 = r22;
            r9 = r23;
            r21.lock();
            r0 = r7.map;	 Catch:{ all -> 0x0106 }
            r0 = r0.ticker;	 Catch:{ all -> 0x0106 }
            r0 = r0.read();	 Catch:{ all -> 0x0106 }
            r10 = r0;
            r7.preWriteCleanup(r10);	 Catch:{ all -> 0x0106 }
            r0 = r7.count;	 Catch:{ all -> 0x0106 }
            r0 = r0 + 1;
            r1 = r7.threshold;	 Catch:{ all -> 0x0106 }
            if (r0 <= r1) goto L_0x0024;
        L_0x001d:
            r21.expand();	 Catch:{ all -> 0x0106 }
            r1 = r7.count;	 Catch:{ all -> 0x0106 }
            r0 = r1 + 1;
        L_0x0024:
            r1 = r7.table;	 Catch:{ all -> 0x0106 }
            r12 = r1;
            r1 = r12.length();	 Catch:{ all -> 0x0106 }
            r1 = r1 + -1;
            r13 = r9 & r1;
            r1 = r12.get(r13);	 Catch:{ all -> 0x0106 }
            r1 = (com.google.common.cache.LocalCache.ReferenceEntry) r1;	 Catch:{ all -> 0x0106 }
            r14 = r1;
        L_0x0037:
            r15 = r1;
            r16 = 0;
            if (r15 == 0) goto L_0x00dd;
        L_0x003c:
            r1 = r15.getKey();	 Catch:{ all -> 0x0106 }
            r5 = r1;
            r1 = r15.getHash();	 Catch:{ all -> 0x0106 }
            if (r1 != r9) goto L_0x00d2;
        L_0x0047:
            if (r5 == 0) goto L_0x00d2;
        L_0x0049:
            r1 = r7.map;	 Catch:{ all -> 0x0106 }
            r1 = r1.keyEquivalence;	 Catch:{ all -> 0x0106 }
            r1 = r1.equivalent(r8, r5);	 Catch:{ all -> 0x0106 }
            if (r1 == 0) goto L_0x00d2;
        L_0x0053:
            r1 = r15.getValueReference();	 Catch:{ all -> 0x0106 }
            r6 = r1;
            r1 = r6.get();	 Catch:{ all -> 0x0106 }
            r17 = r1;
            if (r17 != 0) goto L_0x00a1;
        L_0x0060:
            r1 = r7.modCount;	 Catch:{ all -> 0x0106 }
            r1 = r1 + 1;
            r7.modCount = r1;	 Catch:{ all -> 0x0106 }
            r1 = r6.isActive();	 Catch:{ all -> 0x0106 }
            if (r1 == 0) goto L_0x0082;
        L_0x006c:
            r1 = com.google.common.cache.RemovalCause.COLLECTED;	 Catch:{ all -> 0x0106 }
            r7.enqueueNotification(r8, r9, r6, r1);	 Catch:{ all -> 0x0106 }
            r1 = r7;
            r2 = r15;
            r3 = r8;
            r4 = r24;
            r19 = r0;
            r18 = r5;
            r0 = r6;
            r5 = r10;
            r1.setValue(r2, r3, r4, r5);	 Catch:{ all -> 0x0106 }
            r1 = r7.count;	 Catch:{ all -> 0x0106 }
            goto L_0x0094;
        L_0x0082:
            r19 = r0;
            r18 = r5;
            r0 = r6;
            r1 = r7;
            r2 = r15;
            r3 = r8;
            r4 = r24;
            r5 = r10;
            r1.setValue(r2, r3, r4, r5);	 Catch:{ all -> 0x0106 }
            r1 = r7.count;	 Catch:{ all -> 0x0106 }
            r1 = r1 + 1;
        L_0x0094:
            r7.count = r1;	 Catch:{ all -> 0x0106 }
            r21.evictEntries();	 Catch:{ all -> 0x0106 }
            r21.unlock();
            r21.postWriteCleanup();
            return r16;
        L_0x00a1:
            r19 = r0;
            r18 = r5;
            r0 = r6;
            if (r25 == 0) goto L_0x00b3;
        L_0x00a8:
            r7.recordLockedRead(r15, r10);	 Catch:{ all -> 0x0106 }
            r21.unlock();
            r21.postWriteCleanup();
            return r17;
        L_0x00b3:
            r1 = r7.modCount;	 Catch:{ all -> 0x0106 }
            r1 = r1 + 1;
            r7.modCount = r1;	 Catch:{ all -> 0x0106 }
            r1 = com.google.common.cache.RemovalCause.REPLACED;	 Catch:{ all -> 0x0106 }
            r7.enqueueNotification(r8, r9, r0, r1);	 Catch:{ all -> 0x0106 }
            r1 = r7;
            r2 = r15;
            r3 = r8;
            r4 = r24;
            r5 = r10;
            r1.setValue(r2, r3, r4, r5);	 Catch:{ all -> 0x0106 }
            r21.evictEntries();	 Catch:{ all -> 0x0106 }
            r21.unlock();
            r21.postWriteCleanup();
            return r17;
        L_0x00d2:
            r19 = r0;
            r0 = r15.getNext();	 Catch:{ all -> 0x0106 }
            r1 = r0;
            r0 = r19;
            goto L_0x0037;
        L_0x00dd:
            r19 = r0;
            r0 = r7.modCount;	 Catch:{ all -> 0x0106 }
            r0 = r0 + 1;
            r7.modCount = r0;	 Catch:{ all -> 0x0106 }
            r0 = r7.newEntry(r8, r9, r14);	 Catch:{ all -> 0x0106 }
            r1 = r7;
            r2 = r0;
            r3 = r8;
            r4 = r24;
            r5 = r10;
            r1.setValue(r2, r3, r4, r5);	 Catch:{ all -> 0x0106 }
            r12.set(r13, r0);	 Catch:{ all -> 0x0106 }
            r1 = r7.count;	 Catch:{ all -> 0x0106 }
            r1 = r1 + 1;
            r7.count = r1;	 Catch:{ all -> 0x0106 }
            r21.evictEntries();	 Catch:{ all -> 0x0106 }
            r21.unlock();
            r21.postWriteCleanup();
            return r16;
        L_0x0106:
            r0 = move-exception;
            r21.unlock();
            r21.postWriteCleanup();
            throw r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.google.common.cache.LocalCache$Segment.put(java.lang.Object, int, java.lang.Object, boolean):java.lang.Object");
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("this")
        public void expand() {
            AtomicReferenceArray<ReferenceEntry<K, V>> oldTable = this.table;
            int oldCapacity = oldTable.length();
            if (oldCapacity < 1073741824) {
                int newCount = this.count;
                AtomicReferenceArray<ReferenceEntry<K, V>> newTable = newEntryArray(oldCapacity << 1);
                this.threshold = (newTable.length() * 3) / 4;
                int newMask = newTable.length() - 1;
                for (int oldIndex = 0; oldIndex < oldCapacity; oldIndex++) {
                    ReferenceEntry<K, V> head = (ReferenceEntry) oldTable.get(oldIndex);
                    if (head != null) {
                        ReferenceEntry<K, V> next = head.getNext();
                        int headIndex = head.getHash() & newMask;
                        if (next == null) {
                            newTable.set(headIndex, head);
                        } else {
                            int newIndex;
                            int tailIndex = headIndex;
                            ReferenceEntry<K, V> tail = head;
                            for (ReferenceEntry<K, V> e = next; e != null; e = e.getNext()) {
                                newIndex = e.getHash() & newMask;
                                if (newIndex != tailIndex) {
                                    tailIndex = newIndex;
                                    tail = e;
                                }
                            }
                            newTable.set(tailIndex, tail);
                            int newCount2 = newCount;
                            for (ReferenceEntry<K, V> e2 = head; e2 != tail; e2 = e2.getNext()) {
                                newIndex = e2.getHash() & newMask;
                                ReferenceEntry<K, V> newFirst = copyEntry(e2, (ReferenceEntry) newTable.get(newIndex));
                                if (newFirst != null) {
                                    newTable.set(newIndex, newFirst);
                                } else {
                                    removeCollectedEntry(e2);
                                    newCount2--;
                                }
                            }
                            newCount = newCount2;
                        }
                    }
                }
                this.table = newTable;
                this.count = newCount;
            }
        }

        /* Access modifiers changed, original: 0000 */
        /* JADX WARNING: Missing block: B:10:0x0043, code skipped:
            r6 = r8.getValueReference();
            r5 = r6.get();
     */
        /* JADX WARNING: Missing block: B:11:0x004d, code skipped:
            if (r5 != null) goto L_0x0086;
     */
        /* JADX WARNING: Missing block: B:13:0x0053, code skipped:
            if (r6.isActive() == false) goto L_0x0079;
     */
        /* JADX WARNING: Missing block: B:14:0x0055, code skipped:
            r17 = r9.count - 1;
            r9.modCount++;
            r19 = r5;
            r20 = r6;
            r21 = r7;
            r3 = r9.count - 1;
            r0.set(r15, removeValueFromChain(r2, r8, r7, r11, r6, com.google.common.cache.RemovalCause.COLLECTED));
            r9.count = r3;
     */
        /* JADX WARNING: Missing block: B:15:0x0079, code skipped:
            r19 = r5;
            r20 = r6;
            r21 = r7;
     */
        /* JADX WARNING: Missing block: B:16:0x007f, code skipped:
            unlock();
            postWriteCleanup();
     */
        /* JADX WARNING: Missing block: B:17:0x0085, code skipped:
            return false;
     */
        /* JADX WARNING: Missing block: B:18:0x0086, code skipped:
            r20 = r6;
            r21 = r7;
     */
        /* JADX WARNING: Missing block: B:20:?, code skipped:
            r6 = r5;
     */
        /* JADX WARNING: Missing block: B:21:0x0098, code skipped:
            if (r9.map.valueEquivalence.equivalent(r26, r6) == false) goto L_0x00bf;
     */
        /* JADX WARNING: Missing block: B:22:0x009a, code skipped:
            r9.modCount++;
            r5 = r20;
            enqueueNotification(r10, r11, r5, com.google.common.cache.RemovalCause.REPLACED);
            r1 = r5;
            r17 = r6;
            r22 = r8;
            setValue(r8, r10, r27, r12);
            evictEntries();
     */
        /* JADX WARNING: Missing block: B:23:0x00b7, code skipped:
            unlock();
            postWriteCleanup();
     */
        /* JADX WARNING: Missing block: B:24:0x00be, code skipped:
            return true;
     */
        /* JADX WARNING: Missing block: B:25:0x00bf, code skipped:
            r17 = r6;
            r1 = r20;
     */
        /* JADX WARNING: Missing block: B:27:?, code skipped:
            recordLockedRead(r8, r12);
     */
        /* JADX WARNING: Missing block: B:28:0x00ca, code skipped:
            unlock();
            postWriteCleanup();
     */
        /* JADX WARNING: Missing block: B:29:0x00d1, code skipped:
            return false;
     */
        public boolean replace(K r24, int r25, V r26, V r27) {
            /*
            r23 = this;
            r9 = r23;
            r10 = r24;
            r11 = r25;
            r23.lock();
            r0 = r9.map;	 Catch:{ all -> 0x00e1 }
            r0 = r0.ticker;	 Catch:{ all -> 0x00e1 }
            r0 = r0.read();	 Catch:{ all -> 0x00e1 }
            r12 = r0;
            r9.preWriteCleanup(r12);	 Catch:{ all -> 0x00e1 }
            r0 = r9.table;	 Catch:{ all -> 0x00e1 }
            r1 = r0.length();	 Catch:{ all -> 0x00e1 }
            r14 = 1;
            r1 = r1 - r14;
            r15 = r11 & r1;
            r1 = r0.get(r15);	 Catch:{ all -> 0x00e1 }
            r2 = r1;
            r2 = (com.google.common.cache.LocalCache.ReferenceEntry) r2;	 Catch:{ all -> 0x00e1 }
            r1 = r2;
        L_0x0027:
            r8 = r1;
            r16 = 0;
            if (r8 == 0) goto L_0x00d9;
        L_0x002c:
            r1 = r8.getKey();	 Catch:{ all -> 0x00e1 }
            r7 = r1;
            r1 = r8.getHash();	 Catch:{ all -> 0x00e1 }
            if (r1 != r11) goto L_0x00d2;
        L_0x0037:
            if (r7 == 0) goto L_0x00d2;
        L_0x0039:
            r1 = r9.map;	 Catch:{ all -> 0x00e1 }
            r1 = r1.keyEquivalence;	 Catch:{ all -> 0x00e1 }
            r1 = r1.equivalent(r10, r7);	 Catch:{ all -> 0x00e1 }
            if (r1 == 0) goto L_0x00d2;
        L_0x0043:
            r1 = r8.getValueReference();	 Catch:{ all -> 0x00e1 }
            r6 = r1;
            r1 = r6.get();	 Catch:{ all -> 0x00e1 }
            r5 = r1;
            if (r5 != 0) goto L_0x0086;
        L_0x004f:
            r1 = r6.isActive();	 Catch:{ all -> 0x00e1 }
            if (r1 == 0) goto L_0x0079;
        L_0x0055:
            r1 = r9.count;	 Catch:{ all -> 0x00e1 }
            r17 = r1 + -1;
            r1 = r9.modCount;	 Catch:{ all -> 0x00e1 }
            r1 = r1 + r14;
            r9.modCount = r1;	 Catch:{ all -> 0x00e1 }
            r18 = com.google.common.cache.RemovalCause.COLLECTED;	 Catch:{ all -> 0x00e1 }
            r1 = r9;
            r3 = r8;
            r4 = r7;
            r19 = r5;
            r5 = r11;
            r20 = r6;
            r21 = r7;
            r7 = r18;
            r1 = r1.removeValueFromChain(r2, r3, r4, r5, r6, r7);	 Catch:{ all -> 0x00e1 }
            r3 = r9.count;	 Catch:{ all -> 0x00e1 }
            r3 = r3 - r14;
            r0.set(r15, r1);	 Catch:{ all -> 0x00e1 }
            r9.count = r3;	 Catch:{ all -> 0x00e1 }
            goto L_0x007f;
        L_0x0079:
            r19 = r5;
            r20 = r6;
            r21 = r7;
        L_0x007f:
            r23.unlock();
            r23.postWriteCleanup();
            return r16;
        L_0x0086:
            r19 = r5;
            r20 = r6;
            r21 = r7;
            r1 = r9.map;	 Catch:{ all -> 0x00e1 }
            r1 = r1.valueEquivalence;	 Catch:{ all -> 0x00e1 }
            r7 = r26;
            r6 = r19;
            r1 = r1.equivalent(r7, r6);	 Catch:{ all -> 0x00e1 }
            if (r1 == 0) goto L_0x00bf;
        L_0x009a:
            r1 = r9.modCount;	 Catch:{ all -> 0x00e1 }
            r1 = r1 + r14;
            r9.modCount = r1;	 Catch:{ all -> 0x00e1 }
            r1 = com.google.common.cache.RemovalCause.REPLACED;	 Catch:{ all -> 0x00e1 }
            r5 = r20;
            r9.enqueueNotification(r10, r11, r5, r1);	 Catch:{ all -> 0x00e1 }
            r3 = r9;
            r4 = r8;
            r1 = r5;
            r5 = r10;
            r17 = r6;
            r6 = r27;
            r22 = r8;
            r7 = r12;
            r3.setValue(r4, r5, r6, r7);	 Catch:{ all -> 0x00e1 }
            r23.evictEntries();	 Catch:{ all -> 0x00e1 }
            r23.unlock();
            r23.postWriteCleanup();
            return r14;
        L_0x00bf:
            r17 = r6;
            r22 = r8;
            r1 = r20;
            r3 = r22;
            r9.recordLockedRead(r3, r12);	 Catch:{ all -> 0x00e1 }
            r23.unlock();
            r23.postWriteCleanup();
            return r16;
        L_0x00d2:
            r3 = r8;
            r1 = r3.getNext();	 Catch:{ all -> 0x00e1 }
            goto L_0x0027;
            r23.unlock();
            r23.postWriteCleanup();
            return r16;
        L_0x00e1:
            r0 = move-exception;
            r23.unlock();
            r23.postWriteCleanup();
            throw r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.google.common.cache.LocalCache$Segment.replace(java.lang.Object, int, java.lang.Object, java.lang.Object):boolean");
        }

        /* Access modifiers changed, original: 0000 */
        /* JADX WARNING: Missing block: B:10:0x0042, code skipped:
            r6 = r15.getValueReference();
            r16 = r6.get();
     */
        /* JADX WARNING: Missing block: B:11:0x004d, code skipped:
            if (r16 != null) goto L_0x0084;
     */
        /* JADX WARNING: Missing block: B:13:0x0053, code skipped:
            if (r6.isActive() == false) goto L_0x0079;
     */
        /* JADX WARNING: Missing block: B:14:0x0055, code skipped:
            r17 = r9.count - 1;
            r9.modCount++;
            r19 = r6;
            r20 = r7;
            r3 = r9.count - 1;
            r0.set(r14, removeValueFromChain(r2, r15, r7, r11, r6, com.google.common.cache.RemovalCause.COLLECTED));
            r9.count = r3;
     */
        /* JADX WARNING: Missing block: B:15:0x0079, code skipped:
            r19 = r6;
            r20 = r7;
     */
        /* JADX WARNING: Missing block: B:16:0x007d, code skipped:
            unlock();
            postWriteCleanup();
     */
        /* JADX WARNING: Missing block: B:17:0x0083, code skipped:
            return null;
     */
        /* JADX WARNING: Missing block: B:18:0x0084, code skipped:
            r19 = r6;
            r20 = r7;
     */
        /* JADX WARNING: Missing block: B:20:?, code skipped:
            r9.modCount++;
            r7 = r19;
            enqueueNotification(r10, r11, r7, com.google.common.cache.RemovalCause.REPLACED);
            r1 = r7;
            setValue(r15, r10, r24, r12);
            evictEntries();
     */
        /* JADX WARNING: Missing block: B:21:0x00a2, code skipped:
            unlock();
            postWriteCleanup();
     */
        /* JADX WARNING: Missing block: B:22:0x00a9, code skipped:
            return r16;
     */
        @javax.annotation.Nullable
        public V replace(K r22, int r23, V r24) {
            /*
            r21 = this;
            r9 = r21;
            r10 = r22;
            r11 = r23;
            r21.lock();
            r0 = r9.map;	 Catch:{ all -> 0x00b8 }
            r0 = r0.ticker;	 Catch:{ all -> 0x00b8 }
            r0 = r0.read();	 Catch:{ all -> 0x00b8 }
            r12 = r0;
            r9.preWriteCleanup(r12);	 Catch:{ all -> 0x00b8 }
            r0 = r9.table;	 Catch:{ all -> 0x00b8 }
            r1 = r0.length();	 Catch:{ all -> 0x00b8 }
            r1 = r1 + -1;
            r14 = r11 & r1;
            r1 = r0.get(r14);	 Catch:{ all -> 0x00b8 }
            r2 = r1;
            r2 = (com.google.common.cache.LocalCache.ReferenceEntry) r2;	 Catch:{ all -> 0x00b8 }
            r1 = r2;
        L_0x0027:
            r15 = r1;
            r8 = 0;
            if (r15 == 0) goto L_0x00b0;
        L_0x002b:
            r1 = r15.getKey();	 Catch:{ all -> 0x00b8 }
            r7 = r1;
            r1 = r15.getHash();	 Catch:{ all -> 0x00b8 }
            if (r1 != r11) goto L_0x00aa;
        L_0x0036:
            if (r7 == 0) goto L_0x00aa;
        L_0x0038:
            r1 = r9.map;	 Catch:{ all -> 0x00b8 }
            r1 = r1.keyEquivalence;	 Catch:{ all -> 0x00b8 }
            r1 = r1.equivalent(r10, r7);	 Catch:{ all -> 0x00b8 }
            if (r1 == 0) goto L_0x00aa;
        L_0x0042:
            r1 = r15.getValueReference();	 Catch:{ all -> 0x00b8 }
            r6 = r1;
            r1 = r6.get();	 Catch:{ all -> 0x00b8 }
            r16 = r1;
            if (r16 != 0) goto L_0x0084;
        L_0x004f:
            r1 = r6.isActive();	 Catch:{ all -> 0x00b8 }
            if (r1 == 0) goto L_0x0079;
        L_0x0055:
            r1 = r9.count;	 Catch:{ all -> 0x00b8 }
            r17 = r1 + -1;
            r1 = r9.modCount;	 Catch:{ all -> 0x00b8 }
            r1 = r1 + 1;
            r9.modCount = r1;	 Catch:{ all -> 0x00b8 }
            r18 = com.google.common.cache.RemovalCause.COLLECTED;	 Catch:{ all -> 0x00b8 }
            r1 = r9;
            r3 = r15;
            r4 = r7;
            r5 = r11;
            r19 = r6;
            r20 = r7;
            r7 = r18;
            r1 = r1.removeValueFromChain(r2, r3, r4, r5, r6, r7);	 Catch:{ all -> 0x00b8 }
            r3 = r9.count;	 Catch:{ all -> 0x00b8 }
            r3 = r3 + -1;
            r0.set(r14, r1);	 Catch:{ all -> 0x00b8 }
            r9.count = r3;	 Catch:{ all -> 0x00b8 }
            goto L_0x007d;
        L_0x0079:
            r19 = r6;
            r20 = r7;
        L_0x007d:
            r21.unlock();
            r21.postWriteCleanup();
            return r8;
        L_0x0084:
            r19 = r6;
            r20 = r7;
            r1 = r9.modCount;	 Catch:{ all -> 0x00b8 }
            r1 = r1 + 1;
            r9.modCount = r1;	 Catch:{ all -> 0x00b8 }
            r1 = com.google.common.cache.RemovalCause.REPLACED;	 Catch:{ all -> 0x00b8 }
            r7 = r19;
            r9.enqueueNotification(r10, r11, r7, r1);	 Catch:{ all -> 0x00b8 }
            r3 = r9;
            r4 = r15;
            r5 = r10;
            r6 = r24;
            r1 = r7;
            r7 = r12;
            r3.setValue(r4, r5, r6, r7);	 Catch:{ all -> 0x00b8 }
            r21.evictEntries();	 Catch:{ all -> 0x00b8 }
            r21.unlock();
            r21.postWriteCleanup();
            return r16;
        L_0x00aa:
            r1 = r15.getNext();	 Catch:{ all -> 0x00b8 }
            goto L_0x0027;
            r21.unlock();
            r21.postWriteCleanup();
            return r8;
        L_0x00b8:
            r0 = move-exception;
            r21.unlock();
            r21.postWriteCleanup();
            throw r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.google.common.cache.LocalCache$Segment.replace(java.lang.Object, int, java.lang.Object):java.lang.Object");
        }

        /* Access modifiers changed, original: 0000 */
        /* JADX WARNING: Missing block: B:10:0x0047, code skipped:
            r5 = r14.getValueReference();
            r16 = r5.get();
     */
        /* JADX WARNING: Missing block: B:11:0x0052, code skipped:
            if (r16 == null) goto L_0x0058;
     */
        /* JADX WARNING: Missing block: B:12:0x0054, code skipped:
            r1 = com.google.common.cache.RemovalCause.EXPLICIT;
     */
        /* JADX WARNING: Missing block: B:13:0x0056, code skipped:
            r7 = r1;
     */
        /* JADX WARNING: Missing block: B:15:0x005c, code skipped:
            if (r5.isActive() == false) goto L_0x0085;
     */
        /* JADX WARNING: Missing block: B:16:0x005e, code skipped:
            r1 = com.google.common.cache.RemovalCause.COLLECTED;
     */
        /* JADX WARNING: Missing block: B:17:0x0061, code skipped:
            r8.modCount++;
            r3 = r8.count - 1;
            r12.set(r13, removeValueFromChain(r2, r14, r15, r9, r5, r7));
            r8.count = r3;
     */
        /* JADX WARNING: Missing block: B:19:0x0085, code skipped:
            r17 = r5;
            unlock();
            postWriteCleanup();
     */
        /* JADX WARNING: Missing block: B:20:0x008d, code skipped:
            return null;
     */
        @javax.annotation.Nullable
        public V remove(java.lang.Object r19, int r20) {
            /*
            r18 = this;
            r8 = r18;
            r9 = r20;
            r18.lock();
            r0 = r8.map;	 Catch:{ all -> 0x009b }
            r0 = r0.ticker;	 Catch:{ all -> 0x009b }
            r0 = r0.read();	 Catch:{ all -> 0x009b }
            r10 = r0;
            r8.preWriteCleanup(r10);	 Catch:{ all -> 0x009b }
            r0 = r8.count;	 Catch:{ all -> 0x009b }
            r0 = r0 + -1;
            r1 = r8.table;	 Catch:{ all -> 0x009b }
            r12 = r1;
            r1 = r12.length();	 Catch:{ all -> 0x009b }
            r1 = r1 + -1;
            r13 = r9 & r1;
            r1 = r12.get(r13);	 Catch:{ all -> 0x009b }
            r2 = r1;
            r2 = (com.google.common.cache.LocalCache.ReferenceEntry) r2;	 Catch:{ all -> 0x009b }
            r1 = r2;
        L_0x002a:
            r14 = r1;
            r1 = 0;
            if (r14 == 0) goto L_0x0093;
        L_0x002e:
            r3 = r14.getKey();	 Catch:{ all -> 0x009b }
            r15 = r3;
            r3 = r14.getHash();	 Catch:{ all -> 0x009b }
            if (r3 != r9) goto L_0x008e;
        L_0x0039:
            if (r15 == 0) goto L_0x008e;
        L_0x003b:
            r3 = r8.map;	 Catch:{ all -> 0x009b }
            r3 = r3.keyEquivalence;	 Catch:{ all -> 0x009b }
            r6 = r19;
            r3 = r3.equivalent(r6, r15);	 Catch:{ all -> 0x009b }
            if (r3 == 0) goto L_0x008e;
        L_0x0047:
            r3 = r14.getValueReference();	 Catch:{ all -> 0x009b }
            r5 = r3;
            r3 = r5.get();	 Catch:{ all -> 0x009b }
            r16 = r3;
            if (r16 == 0) goto L_0x0058;
        L_0x0054:
            r1 = com.google.common.cache.RemovalCause.EXPLICIT;	 Catch:{ all -> 0x009b }
        L_0x0056:
            r7 = r1;
            goto L_0x0061;
        L_0x0058:
            r3 = r5.isActive();	 Catch:{ all -> 0x009b }
            if (r3 == 0) goto L_0x0085;
        L_0x005e:
            r1 = com.google.common.cache.RemovalCause.COLLECTED;	 Catch:{ all -> 0x009b }
            goto L_0x0056;
            r1 = r8.modCount;	 Catch:{ all -> 0x009b }
            r1 = r1 + 1;
            r8.modCount = r1;	 Catch:{ all -> 0x009b }
            r1 = r8;
            r3 = r14;
            r4 = r15;
            r17 = r5;
            r5 = r9;
            r6 = r17;
            r1 = r1.removeValueFromChain(r2, r3, r4, r5, r6, r7);	 Catch:{ all -> 0x009b }
            r3 = r8.count;	 Catch:{ all -> 0x009b }
            r3 = r3 + -1;
            r12.set(r13, r1);	 Catch:{ all -> 0x009b }
            r8.count = r3;	 Catch:{ all -> 0x009b }
            r18.unlock();
            r18.postWriteCleanup();
            return r16;
        L_0x0085:
            r17 = r5;
            r18.unlock();
            r18.postWriteCleanup();
            return r1;
        L_0x008e:
            r1 = r14.getNext();	 Catch:{ all -> 0x009b }
            goto L_0x002a;
            r18.unlock();
            r18.postWriteCleanup();
            return r1;
        L_0x009b:
            r0 = move-exception;
            r18.unlock();
            r18.postWriteCleanup();
            throw r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.google.common.cache.LocalCache$Segment.remove(java.lang.Object, int):java.lang.Object");
        }

        /* Access modifiers changed, original: 0000 */
        /* JADX WARNING: Missing block: B:13:0x0052, code skipped:
            r3 = r6.getValueReference();
            r16 = r3.get();
     */
        /* JADX WARNING: Missing block: B:14:0x005d, code skipped:
            if (r10 == r3) goto L_0x0080;
     */
        /* JADX WARNING: Missing block: B:15:0x005f, code skipped:
            if (r16 != null) goto L_0x0066;
     */
        /* JADX WARNING: Missing block: B:17:0x0063, code skipped:
            if (r3 == com.google.common.cache.LocalCache.UNSET) goto L_0x0066;
     */
        /* JADX WARNING: Missing block: B:22:?, code skipped:
            enqueueNotification(r8, r9, new com.google.common.cache.LocalCache.WeightedStrongValueReference(r26, 0), com.google.common.cache.RemovalCause.REPLACED);
     */
        /* JADX WARNING: Missing block: B:23:0x0073, code skipped:
            unlock();
            postWriteCleanup();
     */
        /* JADX WARNING: Missing block: B:24:0x007a, code skipped:
            return false;
     */
        /* JADX WARNING: Missing block: B:25:0x007b, code skipped:
            r0 = th;
     */
        /* JADX WARNING: Missing block: B:26:0x007c, code skipped:
            r13 = r26;
     */
        /* JADX WARNING: Missing block: B:28:?, code skipped:
            r7.modCount++;
     */
        /* JADX WARNING: Missing block: B:29:0x0089, code skipped:
            if (r25.isActive() == false) goto L_0x0097;
     */
        /* JADX WARNING: Missing block: B:30:0x008b, code skipped:
            if (r16 != null) goto L_0x0090;
     */
        /* JADX WARNING: Missing block: B:31:0x008d, code skipped:
            r1 = com.google.common.cache.RemovalCause.COLLECTED;
     */
        /* JADX WARNING: Missing block: B:32:0x0090, code skipped:
            r1 = com.google.common.cache.RemovalCause.REPLACED;
     */
        /* JADX WARNING: Missing block: B:33:0x0092, code skipped:
            enqueueNotification(r8, r9, r10, r1);
            r0 = r0 - 1;
     */
        /* JADX WARNING: Missing block: B:34:0x0097, code skipped:
            r17 = r3;
            r18 = r4;
            r19 = r5;
            r20 = r6;
            setValue(r6, r8, r26, r11);
            r7.count = r0;
            evictEntries();
     */
        /* JADX WARNING: Missing block: B:35:0x00ad, code skipped:
            unlock();
            postWriteCleanup();
     */
        /* JADX WARNING: Missing block: B:36:0x00b4, code skipped:
            return true;
     */
        public boolean storeLoadedValue(K r23, int r24, com.google.common.cache.LocalCache.LoadingValueReference<K, V> r25, V r26) {
            /*
            r22 = this;
            r7 = r22;
            r8 = r23;
            r9 = r24;
            r10 = r25;
            r22.lock();
            r0 = r7.map;	 Catch:{ all -> 0x00ef }
            r0 = r0.ticker;	 Catch:{ all -> 0x00ef }
            r0 = r0.read();	 Catch:{ all -> 0x00ef }
            r11 = r0;
            r7.preWriteCleanup(r11);	 Catch:{ all -> 0x00ef }
            r0 = r7.count;	 Catch:{ all -> 0x00ef }
            r13 = 1;
            r0 = r0 + r13;
            r1 = r7.threshold;	 Catch:{ all -> 0x00ef }
            if (r0 <= r1) goto L_0x0026;
        L_0x001f:
            r22.expand();	 Catch:{ all -> 0x00ef }
            r1 = r7.count;	 Catch:{ all -> 0x00ef }
            r0 = r1 + 1;
        L_0x0026:
            r1 = r7.table;	 Catch:{ all -> 0x00ef }
            r14 = r1;
            r1 = r14.length();	 Catch:{ all -> 0x00ef }
            r1 = r1 - r13;
            r15 = r9 & r1;
            r1 = r14.get(r15);	 Catch:{ all -> 0x00ef }
            r1 = (com.google.common.cache.LocalCache.ReferenceEntry) r1;	 Catch:{ all -> 0x00ef }
            r5 = r1;
        L_0x0038:
            r6 = r1;
            if (r6 == 0) goto L_0x00c4;
        L_0x003b:
            r1 = r6.getKey();	 Catch:{ all -> 0x00ef }
            r4 = r1;
            r1 = r6.getHash();	 Catch:{ all -> 0x00ef }
            if (r1 != r9) goto L_0x00b5;
        L_0x0046:
            if (r4 == 0) goto L_0x00b5;
        L_0x0048:
            r1 = r7.map;	 Catch:{ all -> 0x00ef }
            r1 = r1.keyEquivalence;	 Catch:{ all -> 0x00ef }
            r1 = r1.equivalent(r8, r4);	 Catch:{ all -> 0x00ef }
            if (r1 == 0) goto L_0x00b5;
        L_0x0052:
            r1 = r6.getValueReference();	 Catch:{ all -> 0x00ef }
            r3 = r1;
            r1 = r3.get();	 Catch:{ all -> 0x00ef }
            r16 = r1;
            if (r10 == r3) goto L_0x0080;
        L_0x005f:
            if (r16 != 0) goto L_0x0066;
        L_0x0061:
            r1 = com.google.common.cache.LocalCache.UNSET;	 Catch:{ all -> 0x00ef }
            if (r3 == r1) goto L_0x0066;
        L_0x0065:
            goto L_0x0080;
        L_0x0066:
            r1 = new com.google.common.cache.LocalCache$WeightedStrongValueReference;	 Catch:{ all -> 0x007b }
            r2 = 0;
            r13 = r26;
            r1.<init>(r13, r2);	 Catch:{ all -> 0x00ef }
            r3 = com.google.common.cache.RemovalCause.REPLACED;	 Catch:{ all -> 0x00ef }
            r7.enqueueNotification(r8, r9, r1, r3);	 Catch:{ all -> 0x00ef }
            r22.unlock();
            r22.postWriteCleanup();
            return r2;
        L_0x007b:
            r0 = move-exception;
            r13 = r26;
            goto L_0x00f0;
        L_0x0080:
            r1 = r7.modCount;	 Catch:{ all -> 0x00ef }
            r1 = r1 + r13;
            r7.modCount = r1;	 Catch:{ all -> 0x00ef }
            r1 = r25.isActive();	 Catch:{ all -> 0x00ef }
            if (r1 == 0) goto L_0x0097;
        L_0x008b:
            if (r16 != 0) goto L_0x0090;
        L_0x008d:
            r1 = com.google.common.cache.RemovalCause.COLLECTED;	 Catch:{ all -> 0x00ef }
            goto L_0x0092;
        L_0x0090:
            r1 = com.google.common.cache.RemovalCause.REPLACED;	 Catch:{ all -> 0x00ef }
        L_0x0092:
            r7.enqueueNotification(r8, r9, r10, r1);	 Catch:{ all -> 0x00ef }
            r0 = r0 + -1;
        L_0x0097:
            r1 = r7;
            r2 = r6;
            r17 = r3;
            r3 = r8;
            r18 = r4;
            r4 = r26;
            r19 = r5;
            r20 = r6;
            r5 = r11;
            r1.setValue(r2, r3, r4, r5);	 Catch:{ all -> 0x00ef }
            r7.count = r0;	 Catch:{ all -> 0x00ef }
            r22.evictEntries();	 Catch:{ all -> 0x00ef }
            r22.unlock();
            r22.postWriteCleanup();
            return r13;
        L_0x00b5:
            r19 = r5;
            r20 = r6;
            r1 = r20;
            r2 = r1.getNext();	 Catch:{ all -> 0x00ef }
            r1 = r2;
            r5 = r19;
            goto L_0x0038;
        L_0x00c4:
            r19 = r5;
            r1 = r7.modCount;	 Catch:{ all -> 0x00ef }
            r1 = r1 + r13;
            r7.modCount = r1;	 Catch:{ all -> 0x00ef }
            r5 = r19;
            r1 = r7.newEntry(r8, r9, r5);	 Catch:{ all -> 0x00ef }
            r6 = r1;
            r1 = r7;
            r2 = r6;
            r3 = r8;
            r4 = r26;
            r16 = r5;
            r13 = r6;
            r5 = r11;
            r1.setValue(r2, r3, r4, r5);	 Catch:{ all -> 0x00ef }
            r14.set(r15, r13);	 Catch:{ all -> 0x00ef }
            r7.count = r0;	 Catch:{ all -> 0x00ef }
            r22.evictEntries();	 Catch:{ all -> 0x00ef }
            r22.unlock();
            r22.postWriteCleanup();
            r1 = 1;
            return r1;
        L_0x00ef:
            r0 = move-exception;
        L_0x00f0:
            r22.unlock();
            r22.postWriteCleanup();
            throw r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.google.common.cache.LocalCache$Segment.storeLoadedValue(java.lang.Object, int, com.google.common.cache.LocalCache$LoadingValueReference, java.lang.Object):boolean");
        }

        /* Access modifiers changed, original: 0000 */
        public boolean remove(Object key, int hash, Object value) {
            int i = hash;
            lock();
            try {
                preWriteCleanup(this.map.ticker.read());
                int i2 = 1;
                int newCount = this.count - 1;
                AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
                int index = i & (table.length() - 1);
                ReferenceEntry<K, V> first = (ReferenceEntry) table.get(index);
                ReferenceEntry<K, V> e = first;
                while (true) {
                    ReferenceEntry<K, V> e2 = e;
                    boolean z = false;
                    if (e2 != null) {
                        K entryKey = e2.getKey();
                        if (e2.getHash() == i && entryKey != null && this.map.keyEquivalence.equivalent(key, entryKey)) {
                            K cause;
                            ValueReference<K, V> valueReference = e2.getValueReference();
                            V entryValue = valueReference.get();
                            if (this.map.valueEquivalence.equivalent(value, entryValue)) {
                                cause = RemovalCause.EXPLICIT;
                            } else if (entryValue == null && valueReference.isActive()) {
                                cause = RemovalCause.COLLECTED;
                            } else {
                                ValueReference<K, V> valueReference2 = valueReference;
                                K k = entryKey;
                                unlock();
                                postWriteCleanup();
                                return false;
                            }
                            this.modCount++;
                            K cause2 = cause;
                            int newCount2 = this.count - 1;
                            table.set(index, removeValueFromChain(first, e2, entryKey, i, valueReference, cause2));
                            this.count = newCount2;
                            if (cause2 == RemovalCause.EXPLICIT) {
                                z = true;
                            }
                            unlock();
                            postWriteCleanup();
                            return z;
                        }
                        int i3 = i2;
                        e = e2.getNext();
                        i2 = i3;
                    } else {
                        unlock();
                        postWriteCleanup();
                        return false;
                    }
                }
            } catch (Throwable th) {
                unlock();
                postWriteCleanup();
            }
        }

        /* Access modifiers changed, original: 0000 */
        public void clear() {
            if (this.count != 0) {
                lock();
                try {
                    int i;
                    AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
                    for (i = 0; i < table.length(); i++) {
                        for (ReferenceEntry<K, V> e = (ReferenceEntry) table.get(i); e != null; e = e.getNext()) {
                            if (e.getValueReference().isActive()) {
                                enqueueNotification(e, RemovalCause.EXPLICIT);
                            }
                        }
                    }
                    for (i = 0; i < table.length(); i++) {
                        table.set(i, null);
                    }
                    clearReferenceQueues();
                    this.writeQueue.clear();
                    this.accessQueue.clear();
                    this.readCount.set(0);
                    this.modCount++;
                    this.count = 0;
                } finally {
                    unlock();
                    postWriteCleanup();
                }
            }
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("this")
        @Nullable
        public ReferenceEntry<K, V> removeValueFromChain(ReferenceEntry<K, V> first, ReferenceEntry<K, V> entry, @Nullable K key, int hash, ValueReference<K, V> valueReference, RemovalCause cause) {
            enqueueNotification(key, hash, valueReference, cause);
            this.writeQueue.remove(entry);
            this.accessQueue.remove(entry);
            if (!valueReference.isLoading()) {
                return removeEntryFromChain(first, entry);
            }
            valueReference.notifyNewValue(null);
            return first;
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("this")
        @Nullable
        public ReferenceEntry<K, V> removeEntryFromChain(ReferenceEntry<K, V> first, ReferenceEntry<K, V> entry) {
            int newCount = this.count;
            ReferenceEntry<K, V> newFirst = entry.getNext();
            int newCount2 = newCount;
            for (ReferenceEntry<K, V> e = first; e != entry; e = e.getNext()) {
                ReferenceEntry<K, V> next = copyEntry(e, newFirst);
                if (next != null) {
                    newFirst = next;
                } else {
                    removeCollectedEntry(e);
                    newCount2--;
                }
            }
            this.count = newCount2;
            return newFirst;
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("this")
        public void removeCollectedEntry(ReferenceEntry<K, V> entry) {
            enqueueNotification(entry, RemovalCause.COLLECTED);
            this.writeQueue.remove(entry);
            this.accessQueue.remove(entry);
        }

        /* Access modifiers changed, original: 0000 */
        public boolean reclaimKey(ReferenceEntry<K, V> entry, int hash) {
            lock();
            try {
                boolean z = true;
                int newCount = this.count - z;
                AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
                int index = (table.length() - z) & hash;
                ReferenceEntry<K, V> first = (ReferenceEntry) table.get(index);
                for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
                    if (e == entry) {
                        this.modCount += z;
                        ReferenceEntry<K, V> newFirst = removeValueFromChain(first, e, e.getKey(), hash, e.getValueReference(), RemovalCause.COLLECTED);
                        int newCount2 = this.count - z;
                        table.set(index, newFirst);
                        this.count = newCount2;
                        return z;
                    }
                }
                unlock();
                postWriteCleanup();
                return false;
            } finally {
                unlock();
                postWriteCleanup();
            }
        }

        /* Access modifiers changed, original: 0000 */
        /* JADX WARNING: Removed duplicated region for block: B:37:0x00a1  */
        public boolean reclaimValue(K r19, int r20, com.google.common.cache.LocalCache.ValueReference<K, V> r21) {
            /*
            r18 = this;
            r8 = r18;
            r9 = r20;
            r18.lock();
            r0 = r8.count;	 Catch:{ all -> 0x0095 }
            r10 = 1;
            r0 = r0 - r10;
            r1 = r8.table;	 Catch:{ all -> 0x0095 }
            r11 = r1;
            r1 = r11.length();	 Catch:{ all -> 0x0095 }
            r1 = r1 - r10;
            r12 = r9 & r1;
            r1 = r11.get(r12);	 Catch:{ all -> 0x0095 }
            r2 = r1;
            r2 = (com.google.common.cache.LocalCache.ReferenceEntry) r2;	 Catch:{ all -> 0x0095 }
            r1 = r2;
        L_0x001d:
            r13 = r1;
            r1 = 0;
            if (r13 == 0) goto L_0x0086;
        L_0x0021:
            r3 = r13.getKey();	 Catch:{ all -> 0x0095 }
            r14 = r3;
            r3 = r13.getHash();	 Catch:{ all -> 0x0095 }
            if (r3 != r9) goto L_0x007d;
        L_0x002c:
            if (r14 == 0) goto L_0x007d;
        L_0x002e:
            r3 = r8.map;	 Catch:{ all -> 0x0095 }
            r3 = r3.keyEquivalence;	 Catch:{ all -> 0x0095 }
            r15 = r19;
            r3 = r3.equivalent(r15, r14);	 Catch:{ all -> 0x0084 }
            if (r3 == 0) goto L_0x007f;
        L_0x003a:
            r3 = r13.getValueReference();	 Catch:{ all -> 0x0084 }
            r7 = r3;
            r6 = r21;
            if (r7 != r6) goto L_0x006e;
        L_0x0043:
            r1 = r8.modCount;	 Catch:{ all -> 0x0084 }
            r1 = r1 + r10;
            r8.modCount = r1;	 Catch:{ all -> 0x0084 }
            r16 = com.google.common.cache.RemovalCause.COLLECTED;	 Catch:{ all -> 0x0084 }
            r1 = r8;
            r3 = r13;
            r4 = r14;
            r5 = r9;
            r6 = r21;
            r17 = r7;
            r7 = r16;
            r1 = r1.removeValueFromChain(r2, r3, r4, r5, r6, r7);	 Catch:{ all -> 0x0084 }
            r3 = r8.count;	 Catch:{ all -> 0x0084 }
            r3 = r3 - r10;
            r11.set(r12, r1);	 Catch:{ all -> 0x0084 }
            r8.count = r3;	 Catch:{ all -> 0x0084 }
            r18.unlock();
            r0 = r18.isHeldByCurrentThread();
            if (r0 != 0) goto L_0x006d;
        L_0x006a:
            r18.postWriteCleanup();
        L_0x006d:
            return r10;
        L_0x006e:
            r17 = r7;
            r18.unlock();
            r3 = r18.isHeldByCurrentThread();
            if (r3 != 0) goto L_0x007c;
        L_0x0079:
            r18.postWriteCleanup();
        L_0x007c:
            return r1;
        L_0x007d:
            r15 = r19;
        L_0x007f:
            r1 = r13.getNext();	 Catch:{ all -> 0x0084 }
            goto L_0x001d;
        L_0x0084:
            r0 = move-exception;
            goto L_0x0098;
        L_0x0086:
            r15 = r19;
            r18.unlock();
            r3 = r18.isHeldByCurrentThread();
            if (r3 != 0) goto L_0x0094;
        L_0x0091:
            r18.postWriteCleanup();
        L_0x0094:
            return r1;
        L_0x0095:
            r0 = move-exception;
            r15 = r19;
        L_0x0098:
            r18.unlock();
            r1 = r18.isHeldByCurrentThread();
            if (r1 != 0) goto L_0x00a4;
        L_0x00a1:
            r18.postWriteCleanup();
        L_0x00a4:
            throw r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.google.common.cache.LocalCache$Segment.reclaimValue(java.lang.Object, int, com.google.common.cache.LocalCache$ValueReference):boolean");
        }

        /* Access modifiers changed, original: 0000 */
        public boolean removeLoadingValue(K key, int hash, LoadingValueReference<K, V> valueReference) {
            lock();
            try {
                AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
                int index = (table.length() - 1) & hash;
                ReferenceEntry<K, V> first = (ReferenceEntry) table.get(index);
                ReferenceEntry<K, V> e = first;
                while (e != null) {
                    K entryKey = e.getKey();
                    if (e.getHash() != hash || entryKey == null || !this.map.keyEquivalence.equivalent(key, entryKey)) {
                        e = e.getNext();
                    } else if (e.getValueReference() == valueReference) {
                        if (valueReference.isActive()) {
                            e.setValueReference(valueReference.getOldValue());
                        } else {
                            table.set(index, removeEntryFromChain(first, e));
                        }
                        unlock();
                        postWriteCleanup();
                        return true;
                    } else {
                        unlock();
                        postWriteCleanup();
                        return false;
                    }
                }
                unlock();
                postWriteCleanup();
                return false;
            } catch (Throwable th) {
                unlock();
                postWriteCleanup();
            }
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("this")
        public boolean removeEntry(ReferenceEntry<K, V> entry, int hash, RemovalCause cause) {
            int newCount = this.count - 1;
            AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
            int index = hash & (table.length() - 1);
            ReferenceEntry<K, V> first = (ReferenceEntry) table.get(index);
            ReferenceEntry<K, V> e = first;
            while (true) {
                ReferenceEntry<K, V> e2 = e;
                if (e2 == null) {
                    ReferenceEntry<K, V> referenceEntry = entry;
                    return false;
                } else if (e2 == entry) {
                    this.modCount++;
                    e = removeValueFromChain(first, e2, e2.getKey(), hash, e2.getValueReference(), cause);
                    int newCount2 = this.count - 1;
                    table.set(index, e);
                    this.count = newCount2;
                    return true;
                } else {
                    e = e2.getNext();
                }
            }
        }

        /* Access modifiers changed, original: 0000 */
        public void postReadCleanup() {
            if ((this.readCount.incrementAndGet() & 63) == 0) {
                cleanUp();
            }
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("this")
        public void preWriteCleanup(long now) {
            runLockedCleanup(now);
        }

        /* Access modifiers changed, original: 0000 */
        public void postWriteCleanup() {
            runUnlockedCleanup();
        }

        /* Access modifiers changed, original: 0000 */
        public void cleanUp() {
            runLockedCleanup(this.map.ticker.read());
            runUnlockedCleanup();
        }

        /* Access modifiers changed, original: 0000 */
        public void runLockedCleanup(long now) {
            if (tryLock()) {
                try {
                    drainReferenceQueues();
                    expireEntries(now);
                    this.readCount.set(0);
                } finally {
                    unlock();
                }
            }
        }

        /* Access modifiers changed, original: 0000 */
        public void runUnlockedCleanup() {
            if (!isHeldByCurrentThread()) {
                this.map.processPendingNotifications();
            }
        }
    }

    enum Strength {
        STRONG {
            /* Access modifiers changed, original: 0000 */
            public <K, V> ValueReference<K, V> referenceValue(Segment<K, V> segment, ReferenceEntry<K, V> referenceEntry, V value, int weight) {
                if (weight == 1) {
                    return new StrongValueReference(value);
                }
                return new WeightedStrongValueReference(value, weight);
            }

            /* Access modifiers changed, original: 0000 */
            public Equivalence<Object> defaultEquivalence() {
                return Equivalence.equals();
            }
        },
        SOFT {
            /* Access modifiers changed, original: 0000 */
            public <K, V> ValueReference<K, V> referenceValue(Segment<K, V> segment, ReferenceEntry<K, V> entry, V value, int weight) {
                if (weight == 1) {
                    return new SoftValueReference(segment.valueReferenceQueue, value, entry);
                }
                return new WeightedSoftValueReference(segment.valueReferenceQueue, value, entry, weight);
            }

            /* Access modifiers changed, original: 0000 */
            public Equivalence<Object> defaultEquivalence() {
                return Equivalence.identity();
            }
        },
        WEAK {
            /* Access modifiers changed, original: 0000 */
            public <K, V> ValueReference<K, V> referenceValue(Segment<K, V> segment, ReferenceEntry<K, V> entry, V value, int weight) {
                if (weight == 1) {
                    return new WeakValueReference(segment.valueReferenceQueue, value, entry);
                }
                return new WeightedWeakValueReference(segment.valueReferenceQueue, value, entry, weight);
            }

            /* Access modifiers changed, original: 0000 */
            public Equivalence<Object> defaultEquivalence() {
                return Equivalence.identity();
            }
        };

        public abstract Equivalence<Object> defaultEquivalence();

        public abstract <K, V> ValueReference<K, V> referenceValue(Segment<K, V> segment, ReferenceEntry<K, V> referenceEntry, V v, int i);
    }

    interface ValueReference<K, V> {
        ValueReference<K, V> copyFor(ReferenceQueue<V> referenceQueue, @Nullable V v, ReferenceEntry<K, V> referenceEntry);

        @Nullable
        V get();

        @Nullable
        ReferenceEntry<K, V> getEntry();

        int getWeight();

        boolean isActive();

        boolean isLoading();

        void notifyNewValue(@Nullable V v);

        V waitForValue() throws ExecutionException;
    }

    final class Values extends AbstractCollection<V> {
        private final ConcurrentMap<?, ?> map;

        Values(ConcurrentMap<?, ?> map) {
            this.map = map;
        }

        public int size() {
            return this.map.size();
        }

        public boolean isEmpty() {
            return this.map.isEmpty();
        }

        public void clear() {
            this.map.clear();
        }

        public Iterator<V> iterator() {
            return new ValueIterator();
        }

        public boolean contains(Object o) {
            return this.map.containsValue(o);
        }
    }

    static final class WriteQueue<K, V> extends AbstractQueue<ReferenceEntry<K, V>> {
        final ReferenceEntry<K, V> head = new AbstractReferenceEntry<K, V>() {
            ReferenceEntry<K, V> nextWrite = this;
            ReferenceEntry<K, V> previousWrite = this;

            public long getWriteTime() {
                return Long.MAX_VALUE;
            }

            public void setWriteTime(long time) {
            }

            public ReferenceEntry<K, V> getNextInWriteQueue() {
                return this.nextWrite;
            }

            public void setNextInWriteQueue(ReferenceEntry<K, V> next) {
                this.nextWrite = next;
            }

            public ReferenceEntry<K, V> getPreviousInWriteQueue() {
                return this.previousWrite;
            }

            public void setPreviousInWriteQueue(ReferenceEntry<K, V> previous) {
                this.previousWrite = previous;
            }
        };

        WriteQueue() {
        }

        public boolean offer(ReferenceEntry<K, V> entry) {
            LocalCache.connectWriteOrder(entry.getPreviousInWriteQueue(), entry.getNextInWriteQueue());
            LocalCache.connectWriteOrder(this.head.getPreviousInWriteQueue(), entry);
            LocalCache.connectWriteOrder(entry, this.head);
            return true;
        }

        public ReferenceEntry<K, V> peek() {
            ReferenceEntry<K, V> next = this.head.getNextInWriteQueue();
            return next == this.head ? null : next;
        }

        public ReferenceEntry<K, V> poll() {
            ReferenceEntry<K, V> next = this.head.getNextInWriteQueue();
            if (next == this.head) {
                return null;
            }
            remove(next);
            return next;
        }

        public boolean remove(Object o) {
            ReferenceEntry<K, V> e = (ReferenceEntry) o;
            ReferenceEntry<K, V> previous = e.getPreviousInWriteQueue();
            ReferenceEntry<K, V> next = e.getNextInWriteQueue();
            LocalCache.connectWriteOrder(previous, next);
            LocalCache.nullifyWriteOrder(e);
            return next != NullEntry.INSTANCE;
        }

        public boolean contains(Object o) {
            return ((ReferenceEntry) o).getNextInWriteQueue() != NullEntry.INSTANCE;
        }

        public boolean isEmpty() {
            return this.head.getNextInWriteQueue() == this.head;
        }

        public int size() {
            int size = 0;
            for (ReferenceEntry<K, V> e = this.head.getNextInWriteQueue(); e != this.head; e = e.getNextInWriteQueue()) {
                size++;
            }
            return size;
        }

        public void clear() {
            ReferenceEntry<K, V> e = this.head.getNextInWriteQueue();
            while (e != this.head) {
                ReferenceEntry<K, V> next = e.getNextInWriteQueue();
                LocalCache.nullifyWriteOrder(e);
                e = next;
            }
            this.head.setNextInWriteQueue(this.head);
            this.head.setPreviousInWriteQueue(this.head);
        }

        public Iterator<ReferenceEntry<K, V>> iterator() {
            return new AbstractSequentialIterator<ReferenceEntry<K, V>>(peek()) {
                /* Access modifiers changed, original: protected */
                public ReferenceEntry<K, V> computeNext(ReferenceEntry<K, V> previous) {
                    ReferenceEntry<K, V> next = previous.getNextInWriteQueue();
                    return next == WriteQueue.this.head ? null : next;
                }
            };
        }
    }

    final class WriteThroughEntry implements Entry<K, V> {
        final K key;
        V value;

        WriteThroughEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return this.key;
        }

        public V getValue() {
            return this.value;
        }

        public boolean equals(@Nullable Object object) {
            boolean z = false;
            if (!(object instanceof Entry)) {
                return false;
            }
            Entry<?, ?> that = (Entry) object;
            if (this.key.equals(that.getKey()) && this.value.equals(that.getValue())) {
                z = true;
            }
            return z;
        }

        public int hashCode() {
            return this.key.hashCode() ^ this.value.hashCode();
        }

        public V setValue(V v) {
            throw new UnsupportedOperationException();
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(getKey());
            stringBuilder.append("=");
            stringBuilder.append(getValue());
            return stringBuilder.toString();
        }
    }

    static abstract class AbstractReferenceEntry<K, V> implements ReferenceEntry<K, V> {
        AbstractReferenceEntry() {
        }

        public ValueReference<K, V> getValueReference() {
            throw new UnsupportedOperationException();
        }

        public void setValueReference(ValueReference<K, V> valueReference) {
            throw new UnsupportedOperationException();
        }

        public ReferenceEntry<K, V> getNext() {
            throw new UnsupportedOperationException();
        }

        public int getHash() {
            throw new UnsupportedOperationException();
        }

        public K getKey() {
            throw new UnsupportedOperationException();
        }

        public long getAccessTime() {
            throw new UnsupportedOperationException();
        }

        public void setAccessTime(long time) {
            throw new UnsupportedOperationException();
        }

        public ReferenceEntry<K, V> getNextInAccessQueue() {
            throw new UnsupportedOperationException();
        }

        public void setNextInAccessQueue(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        public ReferenceEntry<K, V> getPreviousInAccessQueue() {
            throw new UnsupportedOperationException();
        }

        public void setPreviousInAccessQueue(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        public long getWriteTime() {
            throw new UnsupportedOperationException();
        }

        public void setWriteTime(long time) {
            throw new UnsupportedOperationException();
        }

        public ReferenceEntry<K, V> getNextInWriteQueue() {
            throw new UnsupportedOperationException();
        }

        public void setNextInWriteQueue(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        public ReferenceEntry<K, V> getPreviousInWriteQueue() {
            throw new UnsupportedOperationException();
        }

        public void setPreviousInWriteQueue(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }
    }

    final class EntryIterator extends HashIterator<Entry<K, V>> {
        EntryIterator() {
            super();
        }

        public Entry<K, V> next() {
            return nextEntry();
        }
    }

    final class EntrySet extends AbstractCacheSet<Entry<K, V>> {
        EntrySet(ConcurrentMap<?, ?> map) {
            super(map);
        }

        public Iterator<Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        public boolean contains(Object o) {
            boolean z = false;
            if (!(o instanceof Entry)) {
                return false;
            }
            Entry<?, ?> e = (Entry) o;
            Object key = e.getKey();
            if (key == null) {
                return false;
            }
            V v = LocalCache.this.get(key);
            if (v != null && LocalCache.this.valueEquivalence.equivalent(e.getValue(), v)) {
                z = true;
            }
            return z;
        }

        public boolean remove(Object o) {
            boolean z = false;
            if (!(o instanceof Entry)) {
                return false;
            }
            Entry<?, ?> e = (Entry) o;
            Object key = e.getKey();
            if (key != null && LocalCache.this.remove(key, e.getValue())) {
                z = true;
            }
            return z;
        }
    }

    final class KeyIterator extends HashIterator<K> {
        KeyIterator() {
            super();
        }

        public K next() {
            return nextEntry().getKey();
        }
    }

    final class KeySet extends AbstractCacheSet<K> {
        KeySet(ConcurrentMap<?, ?> map) {
            super(map);
        }

        public Iterator<K> iterator() {
            return new KeyIterator();
        }

        public boolean contains(Object o) {
            return this.map.containsKey(o);
        }

        public boolean remove(Object o) {
            return this.map.remove(o) != null;
        }
    }

    static class LoadingValueReference<K, V> implements ValueReference<K, V> {
        final SettableFuture<V> futureValue;
        volatile ValueReference<K, V> oldValue;
        final Stopwatch stopwatch;

        public LoadingValueReference() {
            this(LocalCache.unset());
        }

        public LoadingValueReference(ValueReference<K, V> oldValue) {
            this.futureValue = SettableFuture.create();
            this.stopwatch = Stopwatch.createUnstarted();
            this.oldValue = oldValue;
        }

        public boolean isLoading() {
            return true;
        }

        public boolean isActive() {
            return this.oldValue.isActive();
        }

        public int getWeight() {
            return this.oldValue.getWeight();
        }

        public boolean set(@Nullable V newValue) {
            return this.futureValue.set(newValue);
        }

        public boolean setException(Throwable t) {
            return this.futureValue.setException(t);
        }

        private ListenableFuture<V> fullyFailedFuture(Throwable t) {
            return Futures.immediateFailedFuture(t);
        }

        public void notifyNewValue(@Nullable V newValue) {
            if (newValue != null) {
                set(newValue);
            } else {
                this.oldValue = LocalCache.unset();
            }
        }

        public ListenableFuture<V> loadFuture(K key, CacheLoader<? super K, V> loader) {
            this.stopwatch.start();
            V previousValue = this.oldValue.get();
            if (previousValue == null) {
                try {
                    V newValue = loader.load(key);
                    return set(newValue) ? this.futureValue : Futures.immediateFuture(newValue);
                } catch (Throwable t) {
                    if (t instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    return setException(t) ? this.futureValue : fullyFailedFuture(t);
                }
            }
            ListenableFuture t2 = loader.reload(key, previousValue);
            if (t2 == null) {
                return Futures.immediateFuture(null);
            }
            return Futures.transform(t2, new Function<V, V>() {
                public V apply(V newValue) {
                    LoadingValueReference.this.set(newValue);
                    return newValue;
                }
            });
        }

        public long elapsedNanos() {
            return this.stopwatch.elapsed(TimeUnit.NANOSECONDS);
        }

        public V waitForValue() throws ExecutionException {
            return Uninterruptibles.getUninterruptibly(this.futureValue);
        }

        public V get() {
            return this.oldValue.get();
        }

        public ValueReference<K, V> getOldValue() {
            return this.oldValue;
        }

        public ReferenceEntry<K, V> getEntry() {
            return null;
        }

        public ValueReference<K, V> copyFor(ReferenceQueue<V> referenceQueue, @Nullable V v, ReferenceEntry<K, V> referenceEntry) {
            return this;
        }
    }

    static class LocalManualCache<K, V> implements Cache<K, V>, Serializable {
        private static final long serialVersionUID = 1;
        final LocalCache<K, V> localCache;

        /* synthetic */ LocalManualCache(LocalCache x0, AnonymousClass1 x1) {
            this(x0);
        }

        LocalManualCache(CacheBuilder<? super K, ? super V> builder) {
            this(new LocalCache(builder, null));
        }

        private LocalManualCache(LocalCache<K, V> localCache) {
            this.localCache = localCache;
        }

        @Nullable
        public V getIfPresent(Object key) {
            return this.localCache.getIfPresent(key);
        }

        public V get(K key, final Callable<? extends V> valueLoader) throws ExecutionException {
            Preconditions.checkNotNull(valueLoader);
            return this.localCache.get(key, new CacheLoader<Object, V>() {
                public V load(Object key) throws Exception {
                    return valueLoader.call();
                }
            });
        }

        public ImmutableMap<K, V> getAllPresent(Iterable<?> keys) {
            return this.localCache.getAllPresent(keys);
        }

        public void put(K key, V value) {
            this.localCache.put(key, value);
        }

        public void putAll(Map<? extends K, ? extends V> m) {
            this.localCache.putAll(m);
        }

        public void invalidate(Object key) {
            Preconditions.checkNotNull(key);
            this.localCache.remove(key);
        }

        public void invalidateAll(Iterable<?> keys) {
            this.localCache.invalidateAll(keys);
        }

        public void invalidateAll() {
            this.localCache.clear();
        }

        public long size() {
            return this.localCache.longSize();
        }

        public ConcurrentMap<K, V> asMap() {
            return this.localCache;
        }

        public CacheStats stats() {
            SimpleStatsCounter aggregator = new SimpleStatsCounter();
            aggregator.incrementBy(this.localCache.globalStatsCounter);
            for (Segment<K, V> segment : this.localCache.segments) {
                aggregator.incrementBy(segment.statsCounter);
            }
            return aggregator.snapshot();
        }

        public void cleanUp() {
            this.localCache.cleanUp();
        }

        /* Access modifiers changed, original: 0000 */
        public Object writeReplace() {
            return new ManualSerializationProxy(this.localCache);
        }
    }

    private enum NullEntry implements ReferenceEntry<Object, Object> {
        INSTANCE;

        public ValueReference<Object, Object> getValueReference() {
            return null;
        }

        public void setValueReference(ValueReference<Object, Object> valueReference) {
        }

        public ReferenceEntry<Object, Object> getNext() {
            return null;
        }

        public int getHash() {
            return 0;
        }

        public Object getKey() {
            return null;
        }

        public long getAccessTime() {
            return 0;
        }

        public void setAccessTime(long time) {
        }

        public ReferenceEntry<Object, Object> getNextInAccessQueue() {
            return this;
        }

        public void setNextInAccessQueue(ReferenceEntry<Object, Object> referenceEntry) {
        }

        public ReferenceEntry<Object, Object> getPreviousInAccessQueue() {
            return this;
        }

        public void setPreviousInAccessQueue(ReferenceEntry<Object, Object> referenceEntry) {
        }

        public long getWriteTime() {
            return 0;
        }

        public void setWriteTime(long time) {
        }

        public ReferenceEntry<Object, Object> getNextInWriteQueue() {
            return this;
        }

        public void setNextInWriteQueue(ReferenceEntry<Object, Object> referenceEntry) {
        }

        public ReferenceEntry<Object, Object> getPreviousInWriteQueue() {
            return this;
        }

        public void setPreviousInWriteQueue(ReferenceEntry<Object, Object> referenceEntry) {
        }
    }

    static class SoftValueReference<K, V> extends SoftReference<V> implements ValueReference<K, V> {
        final ReferenceEntry<K, V> entry;

        SoftValueReference(ReferenceQueue<V> queue, V referent, ReferenceEntry<K, V> entry) {
            super(referent, queue);
            this.entry = entry;
        }

        public int getWeight() {
            return 1;
        }

        public ReferenceEntry<K, V> getEntry() {
            return this.entry;
        }

        public void notifyNewValue(V v) {
        }

        public ValueReference<K, V> copyFor(ReferenceQueue<V> queue, V value, ReferenceEntry<K, V> entry) {
            return new SoftValueReference(queue, value, entry);
        }

        public boolean isLoading() {
            return false;
        }

        public boolean isActive() {
            return true;
        }

        public V waitForValue() {
            return get();
        }
    }

    static class StrongValueReference<K, V> implements ValueReference<K, V> {
        final V referent;

        StrongValueReference(V referent) {
            this.referent = referent;
        }

        public V get() {
            return this.referent;
        }

        public int getWeight() {
            return 1;
        }

        public ReferenceEntry<K, V> getEntry() {
            return null;
        }

        public ValueReference<K, V> copyFor(ReferenceQueue<V> referenceQueue, V v, ReferenceEntry<K, V> referenceEntry) {
            return this;
        }

        public boolean isLoading() {
            return false;
        }

        public boolean isActive() {
            return true;
        }

        public V waitForValue() {
            return get();
        }

        public void notifyNewValue(V v) {
        }
    }

    final class ValueIterator extends HashIterator<V> {
        ValueIterator() {
            super();
        }

        public V next() {
            return nextEntry().getValue();
        }
    }

    static class WeakEntry<K, V> extends WeakReference<K> implements ReferenceEntry<K, V> {
        final int hash;
        final ReferenceEntry<K, V> next;
        volatile ValueReference<K, V> valueReference = LocalCache.unset();

        WeakEntry(ReferenceQueue<K> queue, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(key, queue);
            this.hash = hash;
            this.next = next;
        }

        public K getKey() {
            return get();
        }

        public long getAccessTime() {
            throw new UnsupportedOperationException();
        }

        public void setAccessTime(long time) {
            throw new UnsupportedOperationException();
        }

        public ReferenceEntry<K, V> getNextInAccessQueue() {
            throw new UnsupportedOperationException();
        }

        public void setNextInAccessQueue(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        public ReferenceEntry<K, V> getPreviousInAccessQueue() {
            throw new UnsupportedOperationException();
        }

        public void setPreviousInAccessQueue(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        public long getWriteTime() {
            throw new UnsupportedOperationException();
        }

        public void setWriteTime(long time) {
            throw new UnsupportedOperationException();
        }

        public ReferenceEntry<K, V> getNextInWriteQueue() {
            throw new UnsupportedOperationException();
        }

        public void setNextInWriteQueue(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        public ReferenceEntry<K, V> getPreviousInWriteQueue() {
            throw new UnsupportedOperationException();
        }

        public void setPreviousInWriteQueue(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        public ValueReference<K, V> getValueReference() {
            return this.valueReference;
        }

        public void setValueReference(ValueReference<K, V> valueReference) {
            this.valueReference = valueReference;
        }

        public int getHash() {
            return this.hash;
        }

        public ReferenceEntry<K, V> getNext() {
            return this.next;
        }
    }

    static class WeakValueReference<K, V> extends WeakReference<V> implements ValueReference<K, V> {
        final ReferenceEntry<K, V> entry;

        WeakValueReference(ReferenceQueue<V> queue, V referent, ReferenceEntry<K, V> entry) {
            super(referent, queue);
            this.entry = entry;
        }

        public int getWeight() {
            return 1;
        }

        public ReferenceEntry<K, V> getEntry() {
            return this.entry;
        }

        public void notifyNewValue(V v) {
        }

        public ValueReference<K, V> copyFor(ReferenceQueue<V> queue, V value, ReferenceEntry<K, V> entry) {
            return new WeakValueReference(queue, value, entry);
        }

        public boolean isLoading() {
            return false;
        }

        public boolean isActive() {
            return true;
        }

        public V waitForValue() {
            return get();
        }
    }

    static class LocalLoadingCache<K, V> extends LocalManualCache<K, V> implements LoadingCache<K, V> {
        private static final long serialVersionUID = 1;

        LocalLoadingCache(CacheBuilder<? super K, ? super V> builder, CacheLoader<? super K, V> loader) {
            super(new LocalCache(builder, (CacheLoader) Preconditions.checkNotNull(loader)), null);
        }

        public V get(K key) throws ExecutionException {
            return this.localCache.getOrLoad(key);
        }

        public V getUnchecked(K key) {
            try {
                return get(key);
            } catch (ExecutionException e) {
                throw new UncheckedExecutionException(e.getCause());
            }
        }

        public ImmutableMap<K, V> getAll(Iterable<? extends K> keys) throws ExecutionException {
            return this.localCache.getAll(keys);
        }

        public void refresh(K key) {
            this.localCache.refresh(key);
        }

        public final V apply(K key) {
            return getUnchecked(key);
        }

        /* Access modifiers changed, original: 0000 */
        public Object writeReplace() {
            return new LoadingSerializationProxy(this.localCache);
        }
    }

    static class ManualSerializationProxy<K, V> extends ForwardingCache<K, V> implements Serializable {
        private static final long serialVersionUID = 1;
        final int concurrencyLevel;
        transient Cache<K, V> delegate;
        final long expireAfterAccessNanos;
        final long expireAfterWriteNanos;
        final Equivalence<Object> keyEquivalence;
        final Strength keyStrength;
        final CacheLoader<? super K, V> loader;
        final long maxWeight;
        final RemovalListener<? super K, ? super V> removalListener;
        final Ticker ticker;
        final Equivalence<Object> valueEquivalence;
        final Strength valueStrength;
        final Weigher<K, V> weigher;

        ManualSerializationProxy(LocalCache<K, V> cache) {
            LocalCache<K, V> localCache = cache;
            this(localCache.keyStrength, localCache.valueStrength, localCache.keyEquivalence, localCache.valueEquivalence, localCache.expireAfterWriteNanos, localCache.expireAfterAccessNanos, localCache.maxWeight, localCache.weigher, localCache.concurrencyLevel, localCache.removalListener, localCache.ticker, localCache.defaultLoader);
        }

        private ManualSerializationProxy(Strength keyStrength, Strength valueStrength, Equivalence<Object> keyEquivalence, Equivalence<Object> valueEquivalence, long expireAfterWriteNanos, long expireAfterAccessNanos, long maxWeight, Weigher<K, V> weigher, int concurrencyLevel, RemovalListener<? super K, ? super V> removalListener, Ticker ticker, CacheLoader<? super K, V> loader) {
            Ticker ticker2 = ticker;
            this.keyStrength = keyStrength;
            this.valueStrength = valueStrength;
            this.keyEquivalence = keyEquivalence;
            this.valueEquivalence = valueEquivalence;
            this.expireAfterWriteNanos = expireAfterWriteNanos;
            this.expireAfterAccessNanos = expireAfterAccessNanos;
            this.maxWeight = maxWeight;
            this.weigher = weigher;
            this.concurrencyLevel = concurrencyLevel;
            this.removalListener = removalListener;
            Ticker ticker3 = (ticker2 == Ticker.systemTicker() || ticker2 == CacheBuilder.NULL_TICKER) ? null : ticker2;
            this.ticker = ticker3;
            this.loader = loader;
        }

        /* Access modifiers changed, original: 0000 */
        public CacheBuilder<K, V> recreateCacheBuilder() {
            CacheBuilder<K, V> builder = CacheBuilder.newBuilder().setKeyStrength(this.keyStrength).setValueStrength(this.valueStrength).keyEquivalence(this.keyEquivalence).valueEquivalence(this.valueEquivalence).concurrencyLevel(this.concurrencyLevel).removalListener(this.removalListener);
            builder.strictParsing = false;
            if (this.expireAfterWriteNanos > 0) {
                builder.expireAfterWrite(this.expireAfterWriteNanos, TimeUnit.NANOSECONDS);
            }
            if (this.expireAfterAccessNanos > 0) {
                builder.expireAfterAccess(this.expireAfterAccessNanos, TimeUnit.NANOSECONDS);
            }
            if (this.weigher != OneWeigher.INSTANCE) {
                builder.weigher(this.weigher);
                if (this.maxWeight != -1) {
                    builder.maximumWeight(this.maxWeight);
                }
            } else if (this.maxWeight != -1) {
                builder.maximumSize(this.maxWeight);
            }
            if (this.ticker != null) {
                builder.ticker(this.ticker);
            }
            return builder;
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            this.delegate = recreateCacheBuilder().build();
        }

        private Object readResolve() {
            return this.delegate;
        }

        /* Access modifiers changed, original: protected */
        public Cache<K, V> delegate() {
            return this.delegate;
        }
    }

    static class StrongEntry<K, V> extends AbstractReferenceEntry<K, V> {
        final int hash;
        final K key;
        final ReferenceEntry<K, V> next;
        volatile ValueReference<K, V> valueReference = LocalCache.unset();

        StrongEntry(K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            this.key = key;
            this.hash = hash;
            this.next = next;
        }

        public K getKey() {
            return this.key;
        }

        public ValueReference<K, V> getValueReference() {
            return this.valueReference;
        }

        public void setValueReference(ValueReference<K, V> valueReference) {
            this.valueReference = valueReference;
        }

        public int getHash() {
            return this.hash;
        }

        public ReferenceEntry<K, V> getNext() {
            return this.next;
        }
    }

    static final class WeakAccessEntry<K, V> extends WeakEntry<K, V> {
        volatile long accessTime = Long.MAX_VALUE;
        ReferenceEntry<K, V> nextAccess = LocalCache.nullEntry();
        ReferenceEntry<K, V> previousAccess = LocalCache.nullEntry();

        WeakAccessEntry(ReferenceQueue<K> queue, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(queue, key, hash, next);
        }

        public long getAccessTime() {
            return this.accessTime;
        }

        public void setAccessTime(long time) {
            this.accessTime = time;
        }

        public ReferenceEntry<K, V> getNextInAccessQueue() {
            return this.nextAccess;
        }

        public void setNextInAccessQueue(ReferenceEntry<K, V> next) {
            this.nextAccess = next;
        }

        public ReferenceEntry<K, V> getPreviousInAccessQueue() {
            return this.previousAccess;
        }

        public void setPreviousInAccessQueue(ReferenceEntry<K, V> previous) {
            this.previousAccess = previous;
        }
    }

    static final class WeakAccessWriteEntry<K, V> extends WeakEntry<K, V> {
        volatile long accessTime = Long.MAX_VALUE;
        ReferenceEntry<K, V> nextAccess = LocalCache.nullEntry();
        ReferenceEntry<K, V> nextWrite = LocalCache.nullEntry();
        ReferenceEntry<K, V> previousAccess = LocalCache.nullEntry();
        ReferenceEntry<K, V> previousWrite = LocalCache.nullEntry();
        volatile long writeTime = Long.MAX_VALUE;

        WeakAccessWriteEntry(ReferenceQueue<K> queue, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(queue, key, hash, next);
        }

        public long getAccessTime() {
            return this.accessTime;
        }

        public void setAccessTime(long time) {
            this.accessTime = time;
        }

        public ReferenceEntry<K, V> getNextInAccessQueue() {
            return this.nextAccess;
        }

        public void setNextInAccessQueue(ReferenceEntry<K, V> next) {
            this.nextAccess = next;
        }

        public ReferenceEntry<K, V> getPreviousInAccessQueue() {
            return this.previousAccess;
        }

        public void setPreviousInAccessQueue(ReferenceEntry<K, V> previous) {
            this.previousAccess = previous;
        }

        public long getWriteTime() {
            return this.writeTime;
        }

        public void setWriteTime(long time) {
            this.writeTime = time;
        }

        public ReferenceEntry<K, V> getNextInWriteQueue() {
            return this.nextWrite;
        }

        public void setNextInWriteQueue(ReferenceEntry<K, V> next) {
            this.nextWrite = next;
        }

        public ReferenceEntry<K, V> getPreviousInWriteQueue() {
            return this.previousWrite;
        }

        public void setPreviousInWriteQueue(ReferenceEntry<K, V> previous) {
            this.previousWrite = previous;
        }
    }

    static final class WeakWriteEntry<K, V> extends WeakEntry<K, V> {
        ReferenceEntry<K, V> nextWrite = LocalCache.nullEntry();
        ReferenceEntry<K, V> previousWrite = LocalCache.nullEntry();
        volatile long writeTime = Long.MAX_VALUE;

        WeakWriteEntry(ReferenceQueue<K> queue, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(queue, key, hash, next);
        }

        public long getWriteTime() {
            return this.writeTime;
        }

        public void setWriteTime(long time) {
            this.writeTime = time;
        }

        public ReferenceEntry<K, V> getNextInWriteQueue() {
            return this.nextWrite;
        }

        public void setNextInWriteQueue(ReferenceEntry<K, V> next) {
            this.nextWrite = next;
        }

        public ReferenceEntry<K, V> getPreviousInWriteQueue() {
            return this.previousWrite;
        }

        public void setPreviousInWriteQueue(ReferenceEntry<K, V> previous) {
            this.previousWrite = previous;
        }
    }

    static final class WeightedSoftValueReference<K, V> extends SoftValueReference<K, V> {
        final int weight;

        WeightedSoftValueReference(ReferenceQueue<V> queue, V referent, ReferenceEntry<K, V> entry, int weight) {
            super(queue, referent, entry);
            this.weight = weight;
        }

        public int getWeight() {
            return this.weight;
        }

        public ValueReference<K, V> copyFor(ReferenceQueue<V> queue, V value, ReferenceEntry<K, V> entry) {
            return new WeightedSoftValueReference(queue, value, entry, this.weight);
        }
    }

    static final class WeightedStrongValueReference<K, V> extends StrongValueReference<K, V> {
        final int weight;

        WeightedStrongValueReference(V referent, int weight) {
            super(referent);
            this.weight = weight;
        }

        public int getWeight() {
            return this.weight;
        }
    }

    static final class WeightedWeakValueReference<K, V> extends WeakValueReference<K, V> {
        final int weight;

        WeightedWeakValueReference(ReferenceQueue<V> queue, V referent, ReferenceEntry<K, V> entry, int weight) {
            super(queue, referent, entry);
            this.weight = weight;
        }

        public int getWeight() {
            return this.weight;
        }

        public ValueReference<K, V> copyFor(ReferenceQueue<V> queue, V value, ReferenceEntry<K, V> entry) {
            return new WeightedWeakValueReference(queue, value, entry, this.weight);
        }
    }

    static final class LoadingSerializationProxy<K, V> extends ManualSerializationProxy<K, V> implements LoadingCache<K, V>, Serializable {
        private static final long serialVersionUID = 1;
        transient LoadingCache<K, V> autoDelegate;

        LoadingSerializationProxy(LocalCache<K, V> cache) {
            super(cache);
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            this.autoDelegate = recreateCacheBuilder().build(this.loader);
        }

        public V get(K key) throws ExecutionException {
            return this.autoDelegate.get(key);
        }

        public V getUnchecked(K key) {
            return this.autoDelegate.getUnchecked(key);
        }

        public ImmutableMap<K, V> getAll(Iterable<? extends K> keys) throws ExecutionException {
            return this.autoDelegate.getAll(keys);
        }

        public final V apply(K key) {
            return this.autoDelegate.apply(key);
        }

        public void refresh(K key) {
            this.autoDelegate.refresh(key);
        }

        private Object readResolve() {
            return this.autoDelegate;
        }
    }

    static final class StrongAccessEntry<K, V> extends StrongEntry<K, V> {
        volatile long accessTime = Long.MAX_VALUE;
        ReferenceEntry<K, V> nextAccess = LocalCache.nullEntry();
        ReferenceEntry<K, V> previousAccess = LocalCache.nullEntry();

        StrongAccessEntry(K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(key, hash, next);
        }

        public long getAccessTime() {
            return this.accessTime;
        }

        public void setAccessTime(long time) {
            this.accessTime = time;
        }

        public ReferenceEntry<K, V> getNextInAccessQueue() {
            return this.nextAccess;
        }

        public void setNextInAccessQueue(ReferenceEntry<K, V> next) {
            this.nextAccess = next;
        }

        public ReferenceEntry<K, V> getPreviousInAccessQueue() {
            return this.previousAccess;
        }

        public void setPreviousInAccessQueue(ReferenceEntry<K, V> previous) {
            this.previousAccess = previous;
        }
    }

    static final class StrongAccessWriteEntry<K, V> extends StrongEntry<K, V> {
        volatile long accessTime = Long.MAX_VALUE;
        ReferenceEntry<K, V> nextAccess = LocalCache.nullEntry();
        ReferenceEntry<K, V> nextWrite = LocalCache.nullEntry();
        ReferenceEntry<K, V> previousAccess = LocalCache.nullEntry();
        ReferenceEntry<K, V> previousWrite = LocalCache.nullEntry();
        volatile long writeTime = Long.MAX_VALUE;

        StrongAccessWriteEntry(K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(key, hash, next);
        }

        public long getAccessTime() {
            return this.accessTime;
        }

        public void setAccessTime(long time) {
            this.accessTime = time;
        }

        public ReferenceEntry<K, V> getNextInAccessQueue() {
            return this.nextAccess;
        }

        public void setNextInAccessQueue(ReferenceEntry<K, V> next) {
            this.nextAccess = next;
        }

        public ReferenceEntry<K, V> getPreviousInAccessQueue() {
            return this.previousAccess;
        }

        public void setPreviousInAccessQueue(ReferenceEntry<K, V> previous) {
            this.previousAccess = previous;
        }

        public long getWriteTime() {
            return this.writeTime;
        }

        public void setWriteTime(long time) {
            this.writeTime = time;
        }

        public ReferenceEntry<K, V> getNextInWriteQueue() {
            return this.nextWrite;
        }

        public void setNextInWriteQueue(ReferenceEntry<K, V> next) {
            this.nextWrite = next;
        }

        public ReferenceEntry<K, V> getPreviousInWriteQueue() {
            return this.previousWrite;
        }

        public void setPreviousInWriteQueue(ReferenceEntry<K, V> previous) {
            this.previousWrite = previous;
        }
    }

    static final class StrongWriteEntry<K, V> extends StrongEntry<K, V> {
        ReferenceEntry<K, V> nextWrite = LocalCache.nullEntry();
        ReferenceEntry<K, V> previousWrite = LocalCache.nullEntry();
        volatile long writeTime = Long.MAX_VALUE;

        StrongWriteEntry(K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(key, hash, next);
        }

        public long getWriteTime() {
            return this.writeTime;
        }

        public void setWriteTime(long time) {
            this.writeTime = time;
        }

        public ReferenceEntry<K, V> getNextInWriteQueue() {
            return this.nextWrite;
        }

        public void setNextInWriteQueue(ReferenceEntry<K, V> next) {
            this.nextWrite = next;
        }

        public ReferenceEntry<K, V> getPreviousInWriteQueue() {
            return this.previousWrite;
        }

        public void setPreviousInWriteQueue(ReferenceEntry<K, V> previous) {
            this.previousWrite = previous;
        }
    }

    LocalCache(CacheBuilder<? super K, ? super V> builder, @Nullable CacheLoader<? super K, V> loader) {
        Queue discardingQueue;
        this.concurrencyLevel = Math.min(builder.getConcurrencyLevel(), 65536);
        this.keyStrength = builder.getKeyStrength();
        this.valueStrength = builder.getValueStrength();
        this.keyEquivalence = builder.getKeyEquivalence();
        this.valueEquivalence = builder.getValueEquivalence();
        this.maxWeight = builder.getMaximumWeight();
        this.weigher = builder.getWeigher();
        this.expireAfterAccessNanos = builder.getExpireAfterAccessNanos();
        this.expireAfterWriteNanos = builder.getExpireAfterWriteNanos();
        this.refreshNanos = builder.getRefreshNanos();
        this.removalListener = builder.getRemovalListener();
        if (this.removalListener == NullListener.INSTANCE) {
            discardingQueue = discardingQueue();
        } else {
            discardingQueue = new ConcurrentLinkedQueue();
        }
        this.removalNotificationQueue = discardingQueue;
        this.ticker = builder.getTicker(recordsTime());
        this.entryFactory = EntryFactory.getFactory(this.keyStrength, usesAccessEntries(), usesWriteEntries());
        this.globalStatsCounter = (StatsCounter) builder.getStatsCounterSupplier().get();
        this.defaultLoader = loader;
        int initialCapacity = Math.min(builder.getInitialCapacity(), 1073741824);
        if (evictsBySize() && !customWeigher()) {
            initialCapacity = Math.min(initialCapacity, (int) this.maxWeight);
        }
        int segmentSize = 1;
        int segmentShift = 0;
        int segmentCount = 1;
        while (segmentCount < this.concurrencyLevel && (!evictsBySize() || ((long) (segmentCount * 20)) <= this.maxWeight)) {
            segmentShift++;
            segmentCount <<= 1;
        }
        this.segmentShift = 32 - segmentShift;
        this.segmentMask = segmentCount - 1;
        this.segments = newSegmentArray(segmentCount);
        int segmentCapacity = initialCapacity / segmentCount;
        if (segmentCapacity * segmentCount < initialCapacity) {
            segmentCapacity++;
        }
        while (segmentSize < segmentCapacity) {
            segmentSize <<= 1;
        }
        int i;
        int i2;
        if (evictsBySize()) {
            long j = 1;
            long maxSegmentWeight = (this.maxWeight / ((long) segmentCount)) + 1;
            long remainder = this.maxWeight % ((long) segmentCount);
            i = 0;
            while (true) {
                i2 = i;
                if (i2 < this.segments.length) {
                    if (((long) i2) == remainder) {
                        maxSegmentWeight -= j;
                    }
                    long maxSegmentWeight2 = maxSegmentWeight;
                    this.segments[i2] = createSegment(segmentSize, maxSegmentWeight2, (StatsCounter) builder.getStatsCounterSupplier().get());
                    i = i2 + 1;
                    maxSegmentWeight = maxSegmentWeight2;
                    j = 1;
                } else {
                    return;
                }
            }
        }
        i = 0;
        while (true) {
            i2 = i;
            if (i2 < this.segments.length) {
                this.segments[i2] = createSegment(segmentSize, -1, (StatsCounter) builder.getStatsCounterSupplier().get());
                i = i2 + 1;
            } else {
                return;
            }
        }
    }

    /* Access modifiers changed, original: 0000 */
    public boolean evictsBySize() {
        return this.maxWeight >= 0;
    }

    /* Access modifiers changed, original: 0000 */
    public boolean customWeigher() {
        return this.weigher != OneWeigher.INSTANCE;
    }

    /* Access modifiers changed, original: 0000 */
    public boolean expires() {
        return expiresAfterWrite() || expiresAfterAccess();
    }

    /* Access modifiers changed, original: 0000 */
    public boolean expiresAfterWrite() {
        return this.expireAfterWriteNanos > 0;
    }

    /* Access modifiers changed, original: 0000 */
    public boolean expiresAfterAccess() {
        return this.expireAfterAccessNanos > 0;
    }

    /* Access modifiers changed, original: 0000 */
    public boolean refreshes() {
        return this.refreshNanos > 0;
    }

    /* Access modifiers changed, original: 0000 */
    public boolean usesAccessQueue() {
        return expiresAfterAccess() || evictsBySize();
    }

    /* Access modifiers changed, original: 0000 */
    public boolean usesWriteQueue() {
        return expiresAfterWrite();
    }

    /* Access modifiers changed, original: 0000 */
    public boolean recordsWrite() {
        return expiresAfterWrite() || refreshes();
    }

    /* Access modifiers changed, original: 0000 */
    public boolean recordsAccess() {
        return expiresAfterAccess();
    }

    /* Access modifiers changed, original: 0000 */
    public boolean recordsTime() {
        return recordsWrite() || recordsAccess();
    }

    /* Access modifiers changed, original: 0000 */
    public boolean usesWriteEntries() {
        return usesWriteQueue() || recordsWrite();
    }

    /* Access modifiers changed, original: 0000 */
    public boolean usesAccessEntries() {
        return usesAccessQueue() || recordsAccess();
    }

    /* Access modifiers changed, original: 0000 */
    public boolean usesKeyReferences() {
        return this.keyStrength != Strength.STRONG;
    }

    /* Access modifiers changed, original: 0000 */
    public boolean usesValueReferences() {
        return this.valueStrength != Strength.STRONG;
    }

    static <K, V> ValueReference<K, V> unset() {
        return UNSET;
    }

    static <K, V> ReferenceEntry<K, V> nullEntry() {
        return NullEntry.INSTANCE;
    }

    static <E> Queue<E> discardingQueue() {
        return DISCARDING_QUEUE;
    }

    static int rehash(int h) {
        h += (h << 15) ^ -12931;
        h ^= h >>> 10;
        h += h << 3;
        h ^= h >>> 6;
        h += (h << 2) + (h << 14);
        return (h >>> 16) ^ h;
    }

    /* Access modifiers changed, original: 0000 */
    @VisibleForTesting
    public ReferenceEntry<K, V> newEntry(K key, int hash, @Nullable ReferenceEntry<K, V> next) {
        Segment<K, V> segment = segmentFor(hash);
        segment.lock();
        try {
            ReferenceEntry<K, V> newEntry = segment.newEntry(key, hash, next);
            return newEntry;
        } finally {
            segment.unlock();
        }
    }

    /* Access modifiers changed, original: 0000 */
    @VisibleForTesting
    public ReferenceEntry<K, V> copyEntry(ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
        return segmentFor(original.getHash()).copyEntry(original, newNext);
    }

    /* Access modifiers changed, original: 0000 */
    @VisibleForTesting
    public ValueReference<K, V> newValueReference(ReferenceEntry<K, V> entry, V value, int weight) {
        return this.valueStrength.referenceValue(segmentFor(entry.getHash()), entry, Preconditions.checkNotNull(value), weight);
    }

    /* Access modifiers changed, original: 0000 */
    public int hash(@Nullable Object key) {
        return rehash(this.keyEquivalence.hash(key));
    }

    /* Access modifiers changed, original: 0000 */
    public void reclaimValue(ValueReference<K, V> valueReference) {
        ReferenceEntry<K, V> entry = valueReference.getEntry();
        int hash = entry.getHash();
        segmentFor(hash).reclaimValue(entry.getKey(), hash, valueReference);
    }

    /* Access modifiers changed, original: 0000 */
    public void reclaimKey(ReferenceEntry<K, V> entry) {
        int hash = entry.getHash();
        segmentFor(hash).reclaimKey(entry, hash);
    }

    /* Access modifiers changed, original: 0000 */
    @VisibleForTesting
    public boolean isLive(ReferenceEntry<K, V> entry, long now) {
        return segmentFor(entry.getHash()).getLiveValue(entry, now) != null;
    }

    /* Access modifiers changed, original: 0000 */
    public Segment<K, V> segmentFor(int hash) {
        return this.segments[(hash >>> this.segmentShift) & this.segmentMask];
    }

    /* Access modifiers changed, original: 0000 */
    public Segment<K, V> createSegment(int initialCapacity, long maxSegmentWeight, StatsCounter statsCounter) {
        return new Segment(this, initialCapacity, maxSegmentWeight, statsCounter);
    }

    /* Access modifiers changed, original: 0000 */
    @Nullable
    public V getLiveValue(ReferenceEntry<K, V> entry, long now) {
        if (entry.getKey() == null) {
            return null;
        }
        V value = entry.getValueReference().get();
        if (value == null || isExpired(entry, now)) {
            return null;
        }
        return value;
    }

    /* Access modifiers changed, original: 0000 */
    public boolean isExpired(ReferenceEntry<K, V> entry, long now) {
        Preconditions.checkNotNull(entry);
        if (expiresAfterAccess() && now - entry.getAccessTime() >= this.expireAfterAccessNanos) {
            return true;
        }
        if (!expiresAfterWrite() || now - entry.getWriteTime() < this.expireAfterWriteNanos) {
            return false;
        }
        return true;
    }

    static <K, V> void connectAccessOrder(ReferenceEntry<K, V> previous, ReferenceEntry<K, V> next) {
        previous.setNextInAccessQueue(next);
        next.setPreviousInAccessQueue(previous);
    }

    static <K, V> void nullifyAccessOrder(ReferenceEntry<K, V> nulled) {
        ReferenceEntry<K, V> nullEntry = nullEntry();
        nulled.setNextInAccessQueue(nullEntry);
        nulled.setPreviousInAccessQueue(nullEntry);
    }

    static <K, V> void connectWriteOrder(ReferenceEntry<K, V> previous, ReferenceEntry<K, V> next) {
        previous.setNextInWriteQueue(next);
        next.setPreviousInWriteQueue(previous);
    }

    static <K, V> void nullifyWriteOrder(ReferenceEntry<K, V> nulled) {
        ReferenceEntry<K, V> nullEntry = nullEntry();
        nulled.setNextInWriteQueue(nullEntry);
        nulled.setPreviousInWriteQueue(nullEntry);
    }

    /* Access modifiers changed, original: 0000 */
    public void processPendingNotifications() {
        while (true) {
            RemovalNotification<K, V> removalNotification = (RemovalNotification) this.removalNotificationQueue.poll();
            RemovalNotification<K, V> notification = removalNotification;
            if (removalNotification != null) {
                try {
                    this.removalListener.onRemoval(notification);
                } catch (Throwable e) {
                    logger.log(Level.WARNING, "Exception thrown by removal listener", e);
                }
            } else {
                return;
            }
        }
    }

    /* Access modifiers changed, original: final */
    public final Segment<K, V>[] newSegmentArray(int ssize) {
        return new Segment[ssize];
    }

    public void cleanUp() {
        for (Segment<?, ?> segment : this.segments) {
            segment.cleanUp();
        }
    }

    public boolean isEmpty() {
        Segment<K, V>[] segments = this.segments;
        long sum = 0;
        for (int i = 0; i < segments.length; i++) {
            if (segments[i].count != 0) {
                return false;
            }
            sum += (long) segments[i].modCount;
        }
        if (sum != 0) {
            long sum2 = sum;
            for (int i2 = 0; i2 < segments.length; i2++) {
                if (segments[i2].count != 0) {
                    return false;
                }
                sum2 -= (long) segments[i2].modCount;
            }
            if (sum2 != 0) {
                return false;
            }
        }
        return true;
    }

    /* Access modifiers changed, original: 0000 */
    public long longSize() {
        long sum = 0;
        for (Segment segment : this.segments) {
            sum += (long) segment.count;
        }
        return sum;
    }

    public int size() {
        return Ints.saturatedCast(longSize());
    }

    @Nullable
    public V get(@Nullable Object key) {
        if (key == null) {
            return null;
        }
        int hash = hash(key);
        return segmentFor(hash).get(key, hash);
    }

    @Nullable
    public V getIfPresent(Object key) {
        int hash = hash(Preconditions.checkNotNull(key));
        V value = segmentFor(hash).get(key, hash);
        if (value == null) {
            this.globalStatsCounter.recordMisses(1);
        } else {
            this.globalStatsCounter.recordHits(1);
        }
        return value;
    }

    /* Access modifiers changed, original: 0000 */
    public V get(K key, CacheLoader<? super K, V> loader) throws ExecutionException {
        int hash = hash(Preconditions.checkNotNull(key));
        return segmentFor(hash).get(key, hash, loader);
    }

    /* Access modifiers changed, original: 0000 */
    public V getOrLoad(K key) throws ExecutionException {
        return get(key, this.defaultLoader);
    }

    /* Access modifiers changed, original: 0000 */
    public ImmutableMap<K, V> getAllPresent(Iterable<?> keys) {
        int hits = 0;
        int misses = 0;
        Map<K, V> result = Maps.newLinkedHashMap();
        for (K key : keys) {
            V value = get(key);
            if (value == null) {
                misses++;
            } else {
                result.put(key, value);
                hits++;
            }
        }
        this.globalStatsCounter.recordHits(hits);
        this.globalStatsCounter.recordMisses(misses);
        return ImmutableMap.copyOf(result);
    }

    /* Access modifiers changed, original: 0000 */
    public ImmutableMap<K, V> getAll(Iterable<? extends K> keys) throws ExecutionException {
        int hits = 0;
        int misses = 0;
        Map<K, V> result = Maps.newLinkedHashMap();
        Set<K> keysToLoad = Sets.newLinkedHashSet();
        for (K key : keys) {
            V value = get(key);
            if (!result.containsKey(key)) {
                result.put(key, value);
                if (value == null) {
                    misses++;
                    keysToLoad.add(key);
                } else {
                    hits++;
                }
            }
        }
        try {
            if (!keysToLoad.isEmpty()) {
                Map<K, V> newEntries = loadAll(keysToLoad, this.defaultLoader);
                for (K key2 : keysToLoad) {
                    V value2 = newEntries.get(key2);
                    if (value2 != null) {
                        result.put(key2, value2);
                    } else {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("loadAll failed to return a value for ");
                        stringBuilder.append(key2);
                        throw new InvalidCacheLoadException(stringBuilder.toString());
                    }
                }
            }
        } catch (UnsupportedLoadingOperationException e) {
            for (K key22 : keysToLoad) {
                misses--;
                result.put(key22, get(key22, this.defaultLoader));
            }
        } catch (Throwable th) {
            this.globalStatsCounter.recordHits(hits);
            this.globalStatsCounter.recordMisses(misses);
        }
        ImmutableMap copyOf = ImmutableMap.copyOf(result);
        this.globalStatsCounter.recordHits(hits);
        this.globalStatsCounter.recordMisses(misses);
        return copyOf;
    }

    /* Access modifiers changed, original: 0000 */
    @Nullable
    public Map<K, V> loadAll(Set<? extends K> keys, CacheLoader<? super K, V> loader) throws ExecutionException {
        Preconditions.checkNotNull(loader);
        Preconditions.checkNotNull(keys);
        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            Map<K, V> result = loader.loadAll(keys);
            if (!true) {
                this.globalStatsCounter.recordLoadException(stopwatch.elapsed(TimeUnit.NANOSECONDS));
            }
            if (result != null) {
                stopwatch.stop();
                boolean nullsPresent = false;
                for (Entry<K, V> entry : result.entrySet()) {
                    K key = entry.getKey();
                    V value = entry.getValue();
                    if (key == null || value == null) {
                        nullsPresent = true;
                    } else {
                        put(key, value);
                    }
                }
                if (nullsPresent) {
                    this.globalStatsCounter.recordLoadException(stopwatch.elapsed(TimeUnit.NANOSECONDS));
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(loader);
                    stringBuilder.append(" returned null keys or values from loadAll");
                    throw new InvalidCacheLoadException(stringBuilder.toString());
                }
                this.globalStatsCounter.recordLoadSuccess(stopwatch.elapsed(TimeUnit.NANOSECONDS));
                return result;
            }
            this.globalStatsCounter.recordLoadException(stopwatch.elapsed(TimeUnit.NANOSECONDS));
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(loader);
            stringBuilder2.append(" returned null map from loadAll");
            throw new InvalidCacheLoadException(stringBuilder2.toString());
        } catch (UnsupportedLoadingOperationException e) {
            throw e;
        } catch (InterruptedException e2) {
            Thread.currentThread().interrupt();
            throw new ExecutionException(e2);
        } catch (RuntimeException e3) {
            throw new UncheckedExecutionException(e3);
        } catch (Exception e4) {
            throw new ExecutionException(e4);
        } catch (Error e5) {
            throw new ExecutionError(e5);
        } catch (Throwable th) {
            if (!false) {
                this.globalStatsCounter.recordLoadException(stopwatch.elapsed(TimeUnit.NANOSECONDS));
            }
        }
    }

    /* Access modifiers changed, original: 0000 */
    public ReferenceEntry<K, V> getEntry(@Nullable Object key) {
        if (key == null) {
            return null;
        }
        int hash = hash(key);
        return segmentFor(hash).getEntry(key, hash);
    }

    /* Access modifiers changed, original: 0000 */
    public void refresh(K key) {
        int hash = hash(Preconditions.checkNotNull(key));
        segmentFor(hash).refresh(key, hash, this.defaultLoader, false);
    }

    public boolean containsKey(@Nullable Object key) {
        if (key == null) {
            return false;
        }
        int hash = hash(key);
        return segmentFor(hash).containsKey(key, hash);
    }

    /* JADX WARNING: Missing block: B:22:0x0063, code skipped:
            r19 = r11;
            r12 = r12 + ((long) r10.modCount);
            r9 = r9 + 1;
            r5 = r18;
            r3 = r3;
            r2 = false;
     */
    public boolean containsValue(@javax.annotation.Nullable java.lang.Object r23) {
        /*
        r22 = this;
        r0 = r22;
        r1 = r23;
        r2 = 0;
        if (r1 != 0) goto L_0x0008;
    L_0x0007:
        return r2;
    L_0x0008:
        r3 = r0.ticker;
        r3 = r3.read();
        r5 = r0.segments;
        r6 = -1;
        r7 = r6;
        r6 = r2;
    L_0x0014:
        r9 = 3;
        if (r6 >= r9) goto L_0x0085;
    L_0x0017:
        r9 = 0;
        r11 = r5.length;
        r12 = r9;
        r9 = r2;
    L_0x001c:
        if (r9 >= r11) goto L_0x0073;
    L_0x001e:
        r10 = r5[r9];
        r14 = r10.count;
        r15 = r10.table;
        r16 = r2;
    L_0x0026:
        r17 = r16;
        r2 = r15.length();
        r18 = r5;
        r5 = r17;
        if (r5 >= r2) goto L_0x0063;
    L_0x0032:
        r2 = r15.get(r5);
        r2 = (com.google.common.cache.LocalCache.ReferenceEntry) r2;
    L_0x0038:
        if (r2 == 0) goto L_0x0059;
    L_0x003a:
        r19 = r11;
        r11 = r10.getLiveValue(r2, r3);
        if (r11 == 0) goto L_0x004e;
    L_0x0042:
        r20 = r3;
        r3 = r0.valueEquivalence;
        r3 = r3.equivalent(r1, r11);
        if (r3 == 0) goto L_0x0050;
    L_0x004c:
        r3 = 1;
        return r3;
    L_0x004e:
        r20 = r3;
    L_0x0050:
        r2 = r2.getNext();
        r11 = r19;
        r3 = r20;
        goto L_0x0038;
    L_0x0059:
        r20 = r3;
        r19 = r11;
        r16 = r5 + 1;
        r5 = r18;
        r2 = 0;
        goto L_0x0026;
    L_0x0063:
        r20 = r3;
        r19 = r11;
        r2 = r10.modCount;
        r2 = (long) r2;
        r12 = r12 + r2;
        r9 = r9 + 1;
        r5 = r18;
        r3 = r20;
        r2 = 0;
        goto L_0x001c;
    L_0x0073:
        r20 = r3;
        r18 = r5;
        r2 = (r12 > r7 ? 1 : (r12 == r7 ? 0 : -1));
        if (r2 != 0) goto L_0x007c;
    L_0x007b:
        goto L_0x0089;
    L_0x007c:
        r7 = r12;
        r6 = r6 + 1;
        r5 = r18;
        r3 = r20;
        r2 = 0;
        goto L_0x0014;
    L_0x0085:
        r20 = r3;
        r18 = r5;
    L_0x0089:
        r2 = 0;
        return r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.common.cache.LocalCache.containsValue(java.lang.Object):boolean");
    }

    public V put(K key, V value) {
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(value);
        int hash = hash(key);
        return segmentFor(hash).put(key, hash, value, false);
    }

    public V putIfAbsent(K key, V value) {
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(value);
        int hash = hash(key);
        return segmentFor(hash).put(key, hash, value, true);
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    public V remove(@Nullable Object key) {
        if (key == null) {
            return null;
        }
        int hash = hash(key);
        return segmentFor(hash).remove(key, hash);
    }

    public boolean remove(@Nullable Object key, @Nullable Object value) {
        if (key == null || value == null) {
            return false;
        }
        int hash = hash(key);
        return segmentFor(hash).remove(key, hash, value);
    }

    public boolean replace(K key, @Nullable V oldValue, V newValue) {
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(newValue);
        if (oldValue == null) {
            return false;
        }
        int hash = hash(key);
        return segmentFor(hash).replace(key, hash, oldValue, newValue);
    }

    public V replace(K key, V value) {
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(value);
        int hash = hash(key);
        return segmentFor(hash).replace(key, hash, value);
    }

    public void clear() {
        for (Segment<K, V> segment : this.segments) {
            segment.clear();
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void invalidateAll(Iterable<?> keys) {
        for (Object key : keys) {
            remove(key);
        }
    }

    public Set<K> keySet() {
        Set<K> ks = this.keySet;
        if (ks != null) {
            return ks;
        }
        KeySet keySet = new KeySet(this);
        this.keySet = keySet;
        return keySet;
    }

    public Collection<V> values() {
        Collection<V> vs = this.values;
        if (vs != null) {
            return vs;
        }
        Values values = new Values(this);
        this.values = values;
        return values;
    }

    @GwtIncompatible("Not supported.")
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> es = this.entrySet;
        if (es != null) {
            return es;
        }
        EntrySet entrySet = new EntrySet(this);
        this.entrySet = entrySet;
        return entrySet;
    }
}
