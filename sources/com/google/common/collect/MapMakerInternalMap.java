package com.google.common.collect;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Equivalence;
import com.google.common.base.Preconditions;
import com.google.common.base.Ticker;
import com.google.common.primitives.Ints;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
import java.util.concurrent.CancellationException;
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

class MapMakerInternalMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V>, Serializable {
    static final long CLEANUP_EXECUTOR_DELAY_SECS = 60;
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
            return Iterators.emptyIterator();
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

        public ReferenceEntry<Object, Object> getEntry() {
            return null;
        }

        public ValueReference<Object, Object> copyFor(ReferenceQueue<Object> referenceQueue, @Nullable Object value, ReferenceEntry<Object, Object> referenceEntry) {
            return this;
        }

        public boolean isComputingReference() {
            return false;
        }

        public Object waitForValue() {
            return null;
        }

        public void clear(ValueReference<Object, Object> valueReference) {
        }
    };
    private static final Logger logger = Logger.getLogger(MapMakerInternalMap.class.getName());
    private static final long serialVersionUID = 5;
    final int concurrencyLevel;
    final transient EntryFactory entryFactory;
    transient Set<Entry<K, V>> entrySet;
    final long expireAfterAccessNanos;
    final long expireAfterWriteNanos;
    final Equivalence<Object> keyEquivalence;
    transient Set<K> keySet;
    final Strength keyStrength;
    final int maximumSize;
    final RemovalListener<K, V> removalListener;
    final Queue<RemovalNotification<K, V>> removalNotificationQueue;
    final transient int segmentMask;
    final transient int segmentShift;
    final transient Segment<K, V>[] segments;
    final Ticker ticker;
    final Equivalence<Object> valueEquivalence = this.valueStrength.defaultEquivalence();
    final Strength valueStrength;
    transient Collection<V> values;

    static final class CleanupMapTask implements Runnable {
        final WeakReference<MapMakerInternalMap<?, ?>> mapReference;

        public CleanupMapTask(MapMakerInternalMap<?, ?> map) {
            this.mapReference = new WeakReference(map);
        }

        public void run() {
            MapMakerInternalMap<?, ?> map = (MapMakerInternalMap) this.mapReference.get();
            if (map != null) {
                for (Segment<?, ?> segment : map.segments) {
                    segment.runCleanup();
                }
                return;
            }
            throw new CancellationException();
        }
    }

    enum EntryFactory {
        STRONG {
            /* Access modifiers changed, original: 0000 */
            public <K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
                return new StrongEntry(key, hash, next);
            }
        },
        STRONG_EXPIRABLE {
            /* Access modifiers changed, original: 0000 */
            public <K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
                return new StrongExpirableEntry(key, hash, next);
            }

            /* Access modifiers changed, original: 0000 */
            public <K, V> ReferenceEntry<K, V> copyEntry(Segment<K, V> segment, ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
                ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
                copyExpirableEntry(original, newEntry);
                return newEntry;
            }
        },
        STRONG_EVICTABLE {
            /* Access modifiers changed, original: 0000 */
            public <K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
                return new StrongEvictableEntry(key, hash, next);
            }

            /* Access modifiers changed, original: 0000 */
            public <K, V> ReferenceEntry<K, V> copyEntry(Segment<K, V> segment, ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
                ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
                copyEvictableEntry(original, newEntry);
                return newEntry;
            }
        },
        STRONG_EXPIRABLE_EVICTABLE {
            /* Access modifiers changed, original: 0000 */
            public <K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
                return new StrongExpirableEvictableEntry(key, hash, next);
            }

            /* Access modifiers changed, original: 0000 */
            public <K, V> ReferenceEntry<K, V> copyEntry(Segment<K, V> segment, ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
                ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
                copyExpirableEntry(original, newEntry);
                copyEvictableEntry(original, newEntry);
                return newEntry;
            }
        },
        WEAK {
            /* Access modifiers changed, original: 0000 */
            public <K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
                return new WeakEntry(segment.keyReferenceQueue, key, hash, next);
            }
        },
        WEAK_EXPIRABLE {
            /* Access modifiers changed, original: 0000 */
            public <K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
                return new WeakExpirableEntry(segment.keyReferenceQueue, key, hash, next);
            }

            /* Access modifiers changed, original: 0000 */
            public <K, V> ReferenceEntry<K, V> copyEntry(Segment<K, V> segment, ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
                ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
                copyExpirableEntry(original, newEntry);
                return newEntry;
            }
        },
        WEAK_EVICTABLE {
            /* Access modifiers changed, original: 0000 */
            public <K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
                return new WeakEvictableEntry(segment.keyReferenceQueue, key, hash, next);
            }

            /* Access modifiers changed, original: 0000 */
            public <K, V> ReferenceEntry<K, V> copyEntry(Segment<K, V> segment, ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
                ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
                copyEvictableEntry(original, newEntry);
                return newEntry;
            }
        },
        WEAK_EXPIRABLE_EVICTABLE {
            /* Access modifiers changed, original: 0000 */
            public <K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
                return new WeakExpirableEvictableEntry(segment.keyReferenceQueue, key, hash, next);
            }

            /* Access modifiers changed, original: 0000 */
            public <K, V> ReferenceEntry<K, V> copyEntry(Segment<K, V> segment, ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
                ReferenceEntry<K, V> newEntry = super.copyEntry(segment, original, newNext);
                copyExpirableEntry(original, newEntry);
                copyEvictableEntry(original, newEntry);
                return newEntry;
            }
        };
        
        static final int EVICTABLE_MASK = 2;
        static final int EXPIRABLE_MASK = 1;
        static final EntryFactory[][] factories = null;

        public abstract <K, V> ReferenceEntry<K, V> newEntry(Segment<K, V> segment, K k, int i, @Nullable ReferenceEntry<K, V> referenceEntry);

        static {
            r0 = new EntryFactory[3][];
            r0[0] = new EntryFactory[]{STRONG, STRONG_EXPIRABLE, STRONG_EVICTABLE, STRONG_EXPIRABLE_EVICTABLE};
            r0[1] = new EntryFactory[0];
            r0[2] = new EntryFactory[]{WEAK, WEAK_EXPIRABLE, WEAK_EVICTABLE, WEAK_EXPIRABLE_EVICTABLE};
            factories = r0;
        }

        static EntryFactory getFactory(Strength keyStrength, boolean expireAfterWrite, boolean evictsBySize) {
            return factories[keyStrength.ordinal()][(evictsBySize ? 2 : 0) | expireAfterWrite];
        }

        /* Access modifiers changed, original: 0000 */
        public <K, V> ReferenceEntry<K, V> copyEntry(Segment<K, V> segment, ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
            return newEntry(segment, original.getKey(), original.getHash(), newNext);
        }

        /* Access modifiers changed, original: 0000 */
        public <K, V> void copyExpirableEntry(ReferenceEntry<K, V> original, ReferenceEntry<K, V> newEntry) {
            newEntry.setExpirationTime(original.getExpirationTime());
            MapMakerInternalMap.connectExpirables(original.getPreviousExpirable(), newEntry);
            MapMakerInternalMap.connectExpirables(newEntry, original.getNextExpirable());
            MapMakerInternalMap.nullifyExpirable(original);
        }

        /* Access modifiers changed, original: 0000 */
        public <K, V> void copyEvictableEntry(ReferenceEntry<K, V> original, ReferenceEntry<K, V> newEntry) {
            MapMakerInternalMap.connectEvictables(original.getPreviousEvictable(), newEntry);
            MapMakerInternalMap.connectEvictables(newEntry, original.getNextEvictable());
            MapMakerInternalMap.nullifyEvictable(original);
        }
    }

    final class EntrySet extends AbstractSet<Entry<K, V>> {
        EntrySet() {
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
            V v = MapMakerInternalMap.this.get(key);
            if (v != null && MapMakerInternalMap.this.valueEquivalence.equivalent(e.getValue(), v)) {
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
            if (key != null && MapMakerInternalMap.this.remove(key, e.getValue())) {
                z = true;
            }
            return z;
        }

        public int size() {
            return MapMakerInternalMap.this.size();
        }

        public boolean isEmpty() {
            return MapMakerInternalMap.this.isEmpty();
        }

        public void clear() {
            MapMakerInternalMap.this.clear();
        }
    }

    static final class EvictionQueue<K, V> extends AbstractQueue<ReferenceEntry<K, V>> {
        final ReferenceEntry<K, V> head = new AbstractReferenceEntry<K, V>() {
            ReferenceEntry<K, V> nextEvictable = this;
            ReferenceEntry<K, V> previousEvictable = this;

            public ReferenceEntry<K, V> getNextEvictable() {
                return this.nextEvictable;
            }

            public void setNextEvictable(ReferenceEntry<K, V> next) {
                this.nextEvictable = next;
            }

            public ReferenceEntry<K, V> getPreviousEvictable() {
                return this.previousEvictable;
            }

            public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
                this.previousEvictable = previous;
            }
        };

        EvictionQueue() {
        }

        public boolean offer(ReferenceEntry<K, V> entry) {
            MapMakerInternalMap.connectEvictables(entry.getPreviousEvictable(), entry.getNextEvictable());
            MapMakerInternalMap.connectEvictables(this.head.getPreviousEvictable(), entry);
            MapMakerInternalMap.connectEvictables(entry, this.head);
            return true;
        }

        public ReferenceEntry<K, V> peek() {
            ReferenceEntry<K, V> next = this.head.getNextEvictable();
            return next == this.head ? null : next;
        }

        public ReferenceEntry<K, V> poll() {
            ReferenceEntry<K, V> next = this.head.getNextEvictable();
            if (next == this.head) {
                return null;
            }
            remove(next);
            return next;
        }

        public boolean remove(Object o) {
            ReferenceEntry<K, V> e = (ReferenceEntry) o;
            ReferenceEntry<K, V> previous = e.getPreviousEvictable();
            ReferenceEntry<K, V> next = e.getNextEvictable();
            MapMakerInternalMap.connectEvictables(previous, next);
            MapMakerInternalMap.nullifyEvictable(e);
            return next != NullEntry.INSTANCE;
        }

        public boolean contains(Object o) {
            return ((ReferenceEntry) o).getNextEvictable() != NullEntry.INSTANCE;
        }

        public boolean isEmpty() {
            return this.head.getNextEvictable() == this.head;
        }

        public int size() {
            int size = 0;
            ReferenceEntry<K, V> e = this.head;
            while (true) {
                e = e.getNextEvictable();
                if (e == this.head) {
                    return size;
                }
                size++;
            }
        }

        public void clear() {
            ReferenceEntry<K, V> e = this.head.getNextEvictable();
            while (e != this.head) {
                ReferenceEntry<K, V> next = e.getNextEvictable();
                MapMakerInternalMap.nullifyEvictable(e);
                e = next;
            }
            this.head.setNextEvictable(this.head);
            this.head.setPreviousEvictable(this.head);
        }

        public Iterator<ReferenceEntry<K, V>> iterator() {
            return new AbstractSequentialIterator<ReferenceEntry<K, V>>(peek()) {
                /* Access modifiers changed, original: protected */
                public ReferenceEntry<K, V> computeNext(ReferenceEntry<K, V> previous) {
                    ReferenceEntry<K, V> next = previous.getNextEvictable();
                    return next == EvictionQueue.this.head ? null : next;
                }
            };
        }
    }

    static final class ExpirationQueue<K, V> extends AbstractQueue<ReferenceEntry<K, V>> {
        final ReferenceEntry<K, V> head = new AbstractReferenceEntry<K, V>() {
            ReferenceEntry<K, V> nextExpirable = this;
            ReferenceEntry<K, V> previousExpirable = this;

            public long getExpirationTime() {
                return Long.MAX_VALUE;
            }

            public void setExpirationTime(long time) {
            }

            public ReferenceEntry<K, V> getNextExpirable() {
                return this.nextExpirable;
            }

            public void setNextExpirable(ReferenceEntry<K, V> next) {
                this.nextExpirable = next;
            }

            public ReferenceEntry<K, V> getPreviousExpirable() {
                return this.previousExpirable;
            }

            public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
                this.previousExpirable = previous;
            }
        };

        ExpirationQueue() {
        }

        public boolean offer(ReferenceEntry<K, V> entry) {
            MapMakerInternalMap.connectExpirables(entry.getPreviousExpirable(), entry.getNextExpirable());
            MapMakerInternalMap.connectExpirables(this.head.getPreviousExpirable(), entry);
            MapMakerInternalMap.connectExpirables(entry, this.head);
            return true;
        }

        public ReferenceEntry<K, V> peek() {
            ReferenceEntry<K, V> next = this.head.getNextExpirable();
            return next == this.head ? null : next;
        }

        public ReferenceEntry<K, V> poll() {
            ReferenceEntry<K, V> next = this.head.getNextExpirable();
            if (next == this.head) {
                return null;
            }
            remove(next);
            return next;
        }

        public boolean remove(Object o) {
            ReferenceEntry<K, V> e = (ReferenceEntry) o;
            ReferenceEntry<K, V> previous = e.getPreviousExpirable();
            ReferenceEntry<K, V> next = e.getNextExpirable();
            MapMakerInternalMap.connectExpirables(previous, next);
            MapMakerInternalMap.nullifyExpirable(e);
            return next != NullEntry.INSTANCE;
        }

        public boolean contains(Object o) {
            return ((ReferenceEntry) o).getNextExpirable() != NullEntry.INSTANCE;
        }

        public boolean isEmpty() {
            return this.head.getNextExpirable() == this.head;
        }

        public int size() {
            int size = 0;
            ReferenceEntry<K, V> e = this.head;
            while (true) {
                e = e.getNextExpirable();
                if (e == this.head) {
                    return size;
                }
                size++;
            }
        }

        public void clear() {
            ReferenceEntry<K, V> e = this.head.getNextExpirable();
            while (e != this.head) {
                ReferenceEntry<K, V> next = e.getNextExpirable();
                MapMakerInternalMap.nullifyExpirable(e);
                e = next;
            }
            this.head.setNextExpirable(this.head);
            this.head.setPreviousExpirable(this.head);
        }

        public Iterator<ReferenceEntry<K, V>> iterator() {
            return new AbstractSequentialIterator<ReferenceEntry<K, V>>(peek()) {
                /* Access modifiers changed, original: protected */
                public ReferenceEntry<K, V> computeNext(ReferenceEntry<K, V> previous) {
                    ReferenceEntry<K, V> next = previous.getNextExpirable();
                    return next == ExpirationQueue.this.head ? null : next;
                }
            };
        }
    }

    abstract class HashIterator<E> implements Iterator<E> {
        Segment<K, V> currentSegment;
        AtomicReferenceArray<ReferenceEntry<K, V>> currentTable;
        WriteThroughEntry lastReturned;
        ReferenceEntry<K, V> nextEntry;
        WriteThroughEntry nextExternal;
        int nextSegmentIndex;
        int nextTableIndex = -1;

        public abstract E next();

        HashIterator() {
            this.nextSegmentIndex = MapMakerInternalMap.this.segments.length - 1;
            advance();
        }

        /* Access modifiers changed, original: final */
        public final void advance() {
            this.nextExternal = null;
            if (!nextInChain() && !nextInTable()) {
                while (this.nextSegmentIndex >= 0) {
                    Segment[] segmentArr = MapMakerInternalMap.this.segments;
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
                K key = entry.getKey();
                V value = MapMakerInternalMap.this.getLiveValue(entry);
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
            CollectPreconditions.checkRemove(this.lastReturned != null);
            MapMakerInternalMap.this.remove(this.lastReturned.getKey());
            this.lastReturned = null;
        }
    }

    final class KeySet extends AbstractSet<K> {
        KeySet() {
        }

        public Iterator<K> iterator() {
            return new KeyIterator();
        }

        public int size() {
            return MapMakerInternalMap.this.size();
        }

        public boolean isEmpty() {
            return MapMakerInternalMap.this.isEmpty();
        }

        public boolean contains(Object o) {
            return MapMakerInternalMap.this.containsKey(o);
        }

        public boolean remove(Object o) {
            return MapMakerInternalMap.this.remove(o) != null;
        }

        public void clear() {
            MapMakerInternalMap.this.clear();
        }
    }

    interface ReferenceEntry<K, V> {
        long getExpirationTime();

        int getHash();

        K getKey();

        ReferenceEntry<K, V> getNext();

        ReferenceEntry<K, V> getNextEvictable();

        ReferenceEntry<K, V> getNextExpirable();

        ReferenceEntry<K, V> getPreviousEvictable();

        ReferenceEntry<K, V> getPreviousExpirable();

        ValueReference<K, V> getValueReference();

        void setExpirationTime(long j);

        void setNextEvictable(ReferenceEntry<K, V> referenceEntry);

        void setNextExpirable(ReferenceEntry<K, V> referenceEntry);

        void setPreviousEvictable(ReferenceEntry<K, V> referenceEntry);

        void setPreviousExpirable(ReferenceEntry<K, V> referenceEntry);

        void setValueReference(ValueReference<K, V> valueReference);
    }

    static class Segment<K, V> extends ReentrantLock {
        volatile int count;
        @GuardedBy("Segment.this")
        final Queue<ReferenceEntry<K, V>> evictionQueue;
        @GuardedBy("Segment.this")
        final Queue<ReferenceEntry<K, V>> expirationQueue;
        final ReferenceQueue<K> keyReferenceQueue;
        final MapMakerInternalMap<K, V> map;
        final int maxSegmentSize;
        int modCount;
        final AtomicInteger readCount = new AtomicInteger();
        final Queue<ReferenceEntry<K, V>> recencyQueue;
        volatile AtomicReferenceArray<ReferenceEntry<K, V>> table;
        int threshold;
        final ReferenceQueue<V> valueReferenceQueue;

        Segment(MapMakerInternalMap<K, V> map, int initialCapacity, int maxSegmentSize) {
            Queue concurrentLinkedQueue;
            this.map = map;
            this.maxSegmentSize = maxSegmentSize;
            initTable(newEntryArray(initialCapacity));
            ReferenceQueue referenceQueue = null;
            this.keyReferenceQueue = map.usesKeyReferences() ? new ReferenceQueue() : null;
            if (map.usesValueReferences()) {
                referenceQueue = new ReferenceQueue();
            }
            this.valueReferenceQueue = referenceQueue;
            if (map.evictsBySize() || map.expiresAfterAccess()) {
                concurrentLinkedQueue = new ConcurrentLinkedQueue();
            } else {
                concurrentLinkedQueue = MapMakerInternalMap.discardingQueue();
            }
            this.recencyQueue = concurrentLinkedQueue;
            if (map.evictsBySize()) {
                concurrentLinkedQueue = new EvictionQueue();
            } else {
                concurrentLinkedQueue = MapMakerInternalMap.discardingQueue();
            }
            this.evictionQueue = concurrentLinkedQueue;
            if (map.expires()) {
                concurrentLinkedQueue = new ExpirationQueue();
            } else {
                concurrentLinkedQueue = MapMakerInternalMap.discardingQueue();
            }
            this.expirationQueue = concurrentLinkedQueue;
        }

        /* Access modifiers changed, original: 0000 */
        public AtomicReferenceArray<ReferenceEntry<K, V>> newEntryArray(int size) {
            return new AtomicReferenceArray(size);
        }

        /* Access modifiers changed, original: 0000 */
        public void initTable(AtomicReferenceArray<ReferenceEntry<K, V>> newTable) {
            this.threshold = (newTable.length() * 3) / 4;
            if (this.threshold == this.maxSegmentSize) {
                this.threshold++;
            }
            this.table = newTable;
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("Segment.this")
        public ReferenceEntry<K, V> newEntry(K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            return this.map.entryFactory.newEntry(this, key, hash, next);
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("Segment.this")
        public ReferenceEntry<K, V> copyEntry(ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
            if (original.getKey() == null) {
                return null;
            }
            ValueReference<K, V> valueReference = original.getValueReference();
            V value = valueReference.get();
            if (value == null && !valueReference.isComputingReference()) {
                return null;
            }
            ReferenceEntry<K, V> newEntry = this.map.entryFactory.copyEntry(this, original, newNext);
            newEntry.setValueReference(valueReference.copyFor(this.valueReferenceQueue, value, newEntry));
            return newEntry;
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("Segment.this")
        public void setValue(ReferenceEntry<K, V> entry, V value) {
            entry.setValueReference(this.map.valueStrength.referenceValue(this, entry, value));
            recordWrite(entry);
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
        @GuardedBy("Segment.this")
        public void drainReferenceQueues() {
            if (this.map.usesKeyReferences()) {
                drainKeyReferenceQueue();
            }
            if (this.map.usesValueReferences()) {
                drainValueReferenceQueue();
            }
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("Segment.this")
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
        @GuardedBy("Segment.this")
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
        public void recordRead(ReferenceEntry<K, V> entry) {
            if (this.map.expiresAfterAccess()) {
                recordExpirationTime(entry, this.map.expireAfterAccessNanos);
            }
            this.recencyQueue.add(entry);
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("Segment.this")
        public void recordLockedRead(ReferenceEntry<K, V> entry) {
            this.evictionQueue.add(entry);
            if (this.map.expiresAfterAccess()) {
                recordExpirationTime(entry, this.map.expireAfterAccessNanos);
                this.expirationQueue.add(entry);
            }
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("Segment.this")
        public void recordWrite(ReferenceEntry<K, V> entry) {
            drainRecencyQueue();
            this.evictionQueue.add(entry);
            if (this.map.expires()) {
                long expiration;
                if (this.map.expiresAfterAccess()) {
                    expiration = this.map.expireAfterAccessNanos;
                } else {
                    expiration = this.map.expireAfterWriteNanos;
                }
                recordExpirationTime(entry, expiration);
                this.expirationQueue.add(entry);
            }
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("Segment.this")
        public void drainRecencyQueue() {
            while (true) {
                ReferenceEntry<K, V> referenceEntry = (ReferenceEntry) this.recencyQueue.poll();
                ReferenceEntry<K, V> e = referenceEntry;
                if (referenceEntry != null) {
                    if (this.evictionQueue.contains(e)) {
                        this.evictionQueue.add(e);
                    }
                    if (this.map.expiresAfterAccess() && this.expirationQueue.contains(e)) {
                        this.expirationQueue.add(e);
                    }
                } else {
                    return;
                }
            }
        }

        /* Access modifiers changed, original: 0000 */
        public void recordExpirationTime(ReferenceEntry<K, V> entry, long expirationNanos) {
            entry.setExpirationTime(this.map.ticker.read() + expirationNanos);
        }

        /* Access modifiers changed, original: 0000 */
        public void tryExpireEntries() {
            if (tryLock()) {
                try {
                    expireEntries();
                } finally {
                    unlock();
                }
            }
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("Segment.this")
        public void expireEntries() {
            drainRecencyQueue();
            if (!this.expirationQueue.isEmpty()) {
                long now = this.map.ticker.read();
                while (true) {
                    ReferenceEntry<K, V> referenceEntry = (ReferenceEntry) this.expirationQueue.peek();
                    ReferenceEntry<K, V> e = referenceEntry;
                    if (referenceEntry != null && this.map.isExpired(e, now)) {
                        if (!removeEntry(e, e.getHash(), RemovalCause.EXPIRED)) {
                            throw new AssertionError();
                        }
                    }
                }
            }
        }

        /* Access modifiers changed, original: 0000 */
        public void enqueueNotification(ReferenceEntry<K, V> entry, RemovalCause cause) {
            enqueueNotification(entry.getKey(), entry.getHash(), entry.getValueReference().get(), cause);
        }

        /* Access modifiers changed, original: 0000 */
        public void enqueueNotification(@Nullable K key, int hash, @Nullable V value, RemovalCause cause) {
            if (this.map.removalNotificationQueue != MapMakerInternalMap.DISCARDING_QUEUE) {
                this.map.removalNotificationQueue.offer(new RemovalNotification(key, value, cause));
            }
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("Segment.this")
        public boolean evictEntries() {
            if (!this.map.evictsBySize() || this.count < this.maxSegmentSize) {
                return false;
            }
            drainRecencyQueue();
            ReferenceEntry<K, V> e = (ReferenceEntry) this.evictionQueue.remove();
            if (removeEntry(e, e.getHash(), RemovalCause.SIZE)) {
                return true;
            }
            throw new AssertionError();
        }

        /* Access modifiers changed, original: 0000 */
        public ReferenceEntry<K, V> getFirst(int hash) {
            AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
            return (ReferenceEntry) table.get((table.length() - 1) & hash);
        }

        /* Access modifiers changed, original: 0000 */
        public ReferenceEntry<K, V> getEntry(Object key, int hash) {
            if (this.count != 0) {
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
            }
            return null;
        }

        /* Access modifiers changed, original: 0000 */
        public ReferenceEntry<K, V> getLiveEntry(Object key, int hash) {
            ReferenceEntry<K, V> e = getEntry(key, hash);
            if (e == null) {
                return null;
            }
            if (!this.map.expires() || !this.map.isExpired(e)) {
                return e;
            }
            tryExpireEntries();
            return null;
        }

        /* Access modifiers changed, original: 0000 */
        public V get(Object key, int hash) {
            try {
                ReferenceEntry<K, V> e = getLiveEntry(key, hash);
                if (e == null) {
                    return null;
                }
                V value = e.getValueReference().get();
                if (value != null) {
                    recordRead(e);
                } else {
                    tryDrainReferenceQueues();
                }
                postReadCleanup();
                return value;
            } finally {
                postReadCleanup();
            }
        }

        /* Access modifiers changed, original: 0000 */
        public boolean containsKey(Object key, int hash) {
            try {
                boolean z = false;
                if (this.count != 0) {
                    ReferenceEntry<K, V> e = getLiveEntry(key, hash);
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
                    AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
                    int length = table.length();
                    for (int i = 0; i < length; i++) {
                        for (ReferenceEntry<K, V> e = (ReferenceEntry) table.get(i); e != null; e = e.getNext()) {
                            V entryValue = getLiveValue(e);
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
        /* JADX WARNING: Missing block: B:13:0x003e, code skipped:
            r7 = r4.getValueReference();
            r8 = r7.get();
     */
        /* JADX WARNING: Missing block: B:14:0x0046, code skipped:
            if (r8 != null) goto L_0x0074;
     */
        /* JADX WARNING: Missing block: B:15:0x0048, code skipped:
            r10.modCount++;
            setValue(r4, r13);
     */
        /* JADX WARNING: Missing block: B:16:0x0055, code skipped:
            if (r7.isComputingReference() != false) goto L_0x0060;
     */
        /* JADX WARNING: Missing block: B:17:0x0057, code skipped:
            enqueueNotification(r11, r12, r8, com.google.common.collect.MapMaker.RemovalCause.COLLECTED);
            r0 = r10.count;
     */
        /* JADX WARNING: Missing block: B:19:0x0064, code skipped:
            if (evictEntries() == false) goto L_0x006a;
     */
        /* JADX WARNING: Missing block: B:20:0x0066, code skipped:
            r0 = r10.count + 1;
     */
        /* JADX WARNING: Missing block: B:21:0x006a, code skipped:
            r10.count = r0;
     */
        /* JADX WARNING: Missing block: B:23:0x0074, code skipped:
            if (r14 == false) goto L_0x0081;
     */
        /* JADX WARNING: Missing block: B:25:?, code skipped:
            recordLockedRead(r4);
     */
        /* JADX WARNING: Missing block: B:26:0x0079, code skipped:
            unlock();
            postWriteCleanup();
     */
        /* JADX WARNING: Missing block: B:27:0x0080, code skipped:
            return r8;
     */
        /* JADX WARNING: Missing block: B:29:?, code skipped:
            r10.modCount++;
            r5 = com.google.common.collect.MapMaker.RemovalCause.REPLACED;
            enqueueNotification(r11, r12, r8, r5);
            setValue(r4, r13);
     */
        /* JADX WARNING: Missing block: B:30:0x008f, code skipped:
            unlock();
            postWriteCleanup();
     */
        /* JADX WARNING: Missing block: B:31:0x0096, code skipped:
            return r8;
     */
        public V put(K r11, int r12, V r13, boolean r14) {
            /*
            r10 = this;
            r10.lock();
            r10.preWriteCleanup();	 Catch:{ all -> 0x00c1 }
            r0 = r10.count;	 Catch:{ all -> 0x00c1 }
            r0 = r0 + 1;
            r1 = r10.threshold;	 Catch:{ all -> 0x00c1 }
            if (r0 <= r1) goto L_0x0015;
        L_0x000e:
            r10.expand();	 Catch:{ all -> 0x00c1 }
            r1 = r10.count;	 Catch:{ all -> 0x00c1 }
            r0 = r1 + 1;
        L_0x0015:
            r1 = r10.table;	 Catch:{ all -> 0x00c1 }
            r2 = r1.length();	 Catch:{ all -> 0x00c1 }
            r2 = r2 + -1;
            r2 = r2 & r12;
            r3 = r1.get(r2);	 Catch:{ all -> 0x00c1 }
            r3 = (com.google.common.collect.MapMakerInternalMap.ReferenceEntry) r3;	 Catch:{ all -> 0x00c1 }
            r4 = r3;
        L_0x0025:
            r5 = 0;
            if (r4 == 0) goto L_0x009d;
        L_0x0028:
            r6 = r4.getKey();	 Catch:{ all -> 0x00c1 }
            r7 = r4.getHash();	 Catch:{ all -> 0x00c1 }
            if (r7 != r12) goto L_0x0097;
        L_0x0032:
            if (r6 == 0) goto L_0x0097;
        L_0x0034:
            r7 = r10.map;	 Catch:{ all -> 0x00c1 }
            r7 = r7.keyEquivalence;	 Catch:{ all -> 0x00c1 }
            r7 = r7.equivalent(r11, r6);	 Catch:{ all -> 0x00c1 }
            if (r7 == 0) goto L_0x0097;
        L_0x003e:
            r7 = r4.getValueReference();	 Catch:{ all -> 0x00c1 }
            r8 = r7.get();	 Catch:{ all -> 0x00c1 }
            if (r8 != 0) goto L_0x0074;
        L_0x0048:
            r9 = r10.modCount;	 Catch:{ all -> 0x00c1 }
            r9 = r9 + 1;
            r10.modCount = r9;	 Catch:{ all -> 0x00c1 }
            r10.setValue(r4, r13);	 Catch:{ all -> 0x00c1 }
            r9 = r7.isComputingReference();	 Catch:{ all -> 0x00c1 }
            if (r9 != 0) goto L_0x0060;
        L_0x0057:
            r9 = com.google.common.collect.MapMaker.RemovalCause.COLLECTED;	 Catch:{ all -> 0x00c1 }
            r10.enqueueNotification(r11, r12, r8, r9);	 Catch:{ all -> 0x00c1 }
            r9 = r10.count;	 Catch:{ all -> 0x00c1 }
            r0 = r9;
            goto L_0x006a;
        L_0x0060:
            r9 = r10.evictEntries();	 Catch:{ all -> 0x00c1 }
            if (r9 == 0) goto L_0x006a;
        L_0x0066:
            r9 = r10.count;	 Catch:{ all -> 0x00c1 }
            r0 = r9 + 1;
        L_0x006a:
            r10.count = r0;	 Catch:{ all -> 0x00c1 }
            r10.unlock();
            r10.postWriteCleanup();
            return r5;
        L_0x0074:
            if (r14 == 0) goto L_0x0081;
        L_0x0076:
            r10.recordLockedRead(r4);	 Catch:{ all -> 0x00c1 }
            r10.unlock();
            r10.postWriteCleanup();
            return r8;
        L_0x0081:
            r5 = r10.modCount;	 Catch:{ all -> 0x00c1 }
            r5 = r5 + 1;
            r10.modCount = r5;	 Catch:{ all -> 0x00c1 }
            r5 = com.google.common.collect.MapMaker.RemovalCause.REPLACED;	 Catch:{ all -> 0x00c1 }
            r10.enqueueNotification(r11, r12, r8, r5);	 Catch:{ all -> 0x00c1 }
            r10.setValue(r4, r13);	 Catch:{ all -> 0x00c1 }
            r10.unlock();
            r10.postWriteCleanup();
            return r8;
        L_0x0097:
            r5 = r4.getNext();	 Catch:{ all -> 0x00c1 }
            r4 = r5;
            goto L_0x0025;
        L_0x009d:
            r4 = r10.modCount;	 Catch:{ all -> 0x00c1 }
            r4 = r4 + 1;
            r10.modCount = r4;	 Catch:{ all -> 0x00c1 }
            r4 = r10.newEntry(r11, r12, r3);	 Catch:{ all -> 0x00c1 }
            r10.setValue(r4, r13);	 Catch:{ all -> 0x00c1 }
            r1.set(r2, r4);	 Catch:{ all -> 0x00c1 }
            r6 = r10.evictEntries();	 Catch:{ all -> 0x00c1 }
            if (r6 == 0) goto L_0x00b7;
        L_0x00b3:
            r6 = r10.count;	 Catch:{ all -> 0x00c1 }
            r0 = r6 + 1;
        L_0x00b7:
            r10.count = r0;	 Catch:{ all -> 0x00c1 }
            r10.unlock();
            r10.postWriteCleanup();
            return r5;
        L_0x00c1:
            r0 = move-exception;
            r10.unlock();
            r10.postWriteCleanup();
            throw r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.google.common.collect.MapMakerInternalMap$Segment.put(java.lang.Object, int, java.lang.Object, boolean):java.lang.Object");
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("Segment.this")
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
        public boolean replace(K key, int hash, V oldValue, V newValue) {
            Throwable th;
            K k = key;
            int i = hash;
            lock();
            V v;
            V v2;
            try {
                preWriteCleanup();
                AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
                int index = (table.length() - 1) & i;
                ReferenceEntry<K, V> first = (ReferenceEntry) table.get(index);
                for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
                    K entryKey = e.getKey();
                    if (e.getHash() == i && entryKey != null && this.map.keyEquivalence.equivalent(k, entryKey)) {
                        ValueReference<K, V> valueReference = e.getValueReference();
                        V entryValue = valueReference.get();
                        if (entryValue == null) {
                            if (isCollected(valueReference)) {
                                int newCount = this.count - 1;
                                this.modCount++;
                                enqueueNotification(entryKey, i, entryValue, RemovalCause.COLLECTED);
                                int newCount2 = this.count - 1;
                                table.set(index, removeFromChain(first, e));
                                this.count = newCount2;
                            }
                            unlock();
                            postWriteCleanup();
                            return false;
                        }
                        try {
                            if (this.map.valueEquivalence.equivalent(oldValue, entryValue)) {
                                this.modCount++;
                                enqueueNotification(k, i, entryValue, RemovalCause.REPLACED);
                                try {
                                    setValue(e, newValue);
                                    unlock();
                                    postWriteCleanup();
                                    return true;
                                } catch (Throwable th2) {
                                    th = th2;
                                    unlock();
                                    postWriteCleanup();
                                    throw th;
                                }
                            }
                            v = newValue;
                            recordLockedRead(e);
                            unlock();
                            postWriteCleanup();
                            return false;
                        } catch (Throwable th3) {
                            th = th3;
                            v = newValue;
                            unlock();
                            postWriteCleanup();
                            throw th;
                        }
                    }
                    v2 = oldValue;
                    v = newValue;
                }
                v2 = oldValue;
                v = newValue;
                unlock();
                postWriteCleanup();
                return false;
            } catch (Throwable th4) {
                th = th4;
                v2 = oldValue;
                v = newValue;
                unlock();
                postWriteCleanup();
                throw th;
            }
        }

        /* Access modifiers changed, original: 0000 */
        public V replace(K key, int hash, V newValue) {
            lock();
            try {
                preWriteCleanup();
                AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
                int index = (table.length() - 1) & hash;
                ReferenceEntry<K, V> first = (ReferenceEntry) table.get(index);
                for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
                    K entryKey = e.getKey();
                    if (e.getHash() == hash && entryKey != null && this.map.keyEquivalence.equivalent(key, entryKey)) {
                        ValueReference<K, V> valueReference = e.getValueReference();
                        V entryValue = valueReference.get();
                        if (entryValue == null) {
                            if (isCollected(valueReference)) {
                                int newCount = this.count - 1;
                                this.modCount++;
                                enqueueNotification(entryKey, hash, entryValue, RemovalCause.COLLECTED);
                                int newCount2 = this.count - 1;
                                table.set(index, removeFromChain(first, e));
                                this.count = newCount2;
                            }
                            unlock();
                            postWriteCleanup();
                            return null;
                        }
                        this.modCount++;
                        enqueueNotification(key, hash, entryValue, RemovalCause.REPLACED);
                        setValue(e, newValue);
                        unlock();
                        postWriteCleanup();
                        return entryValue;
                    }
                }
                unlock();
                postWriteCleanup();
                return null;
            } catch (Throwable th) {
                unlock();
                postWriteCleanup();
            }
        }

        /* Access modifiers changed, original: 0000 */
        public V remove(Object key, int hash) {
            lock();
            try {
                preWriteCleanup();
                int newCount = this.count - 1;
                AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
                int index = (table.length() - 1) & hash;
                ReferenceEntry<K, V> first = (ReferenceEntry) table.get(index);
                for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
                    K entryKey = e.getKey();
                    if (e.getHash() == hash && entryKey != null && this.map.keyEquivalence.equivalent(key, entryKey)) {
                        RemovalCause cause;
                        ValueReference<K, V> valueReference = e.getValueReference();
                        V entryValue = valueReference.get();
                        if (entryValue != null) {
                            cause = RemovalCause.EXPLICIT;
                        } else if (isCollected(valueReference)) {
                            cause = RemovalCause.COLLECTED;
                        } else {
                            unlock();
                            postWriteCleanup();
                            return null;
                        }
                        this.modCount++;
                        enqueueNotification(entryKey, hash, entryValue, cause);
                        int newCount2 = this.count - 1;
                        table.set(index, removeFromChain(first, e));
                        this.count = newCount2;
                        return entryValue;
                    }
                }
                unlock();
                postWriteCleanup();
                return null;
            } finally {
                unlock();
                postWriteCleanup();
            }
        }

        /* Access modifiers changed, original: 0000 */
        public boolean remove(Object key, int hash, Object value) {
            Throwable th;
            Object obj;
            int i = hash;
            lock();
            Object obj2;
            try {
                preWriteCleanup();
                int newCount = this.count - 1;
                AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
                int index = (table.length() - 1) & i;
                ReferenceEntry<K, V> first = (ReferenceEntry) table.get(index);
                for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
                    K entryKey = e.getKey();
                    if (e.getHash() != i || entryKey == null) {
                        obj2 = key;
                    } else {
                        try {
                            if (this.map.keyEquivalence.equivalent(key, entryKey)) {
                                ValueReference<K, V> valueReference = e.getValueReference();
                                V entryValue = valueReference.get();
                                try {
                                    RemovalCause cause;
                                    if (this.map.valueEquivalence.equivalent(value, entryValue)) {
                                        cause = RemovalCause.EXPLICIT;
                                    } else if (isCollected(valueReference)) {
                                        cause = RemovalCause.COLLECTED;
                                    } else {
                                        unlock();
                                        postWriteCleanup();
                                        return false;
                                    }
                                    this.modCount++;
                                    enqueueNotification(entryKey, i, entryValue, cause);
                                    int newCount2 = this.count - 1;
                                    table.set(index, removeFromChain(first, e));
                                    this.count = newCount2;
                                    boolean z = cause == RemovalCause.EXPLICIT;
                                    unlock();
                                    postWriteCleanup();
                                    return z;
                                } catch (Throwable th2) {
                                    th = th2;
                                    unlock();
                                    postWriteCleanup();
                                    throw th;
                                }
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            obj = value;
                            unlock();
                            postWriteCleanup();
                            throw th;
                        }
                    }
                    obj = value;
                }
                obj2 = key;
                obj = value;
                unlock();
                postWriteCleanup();
                return false;
            } catch (Throwable th4) {
                th = th4;
                obj2 = key;
                obj = value;
                unlock();
                postWriteCleanup();
                throw th;
            }
        }

        /* Access modifiers changed, original: 0000 */
        public void clear() {
            if (this.count != 0) {
                lock();
                try {
                    int i;
                    AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
                    if (this.map.removalNotificationQueue != MapMakerInternalMap.DISCARDING_QUEUE) {
                        for (i = 0; i < table.length(); i++) {
                            for (ReferenceEntry<K, V> e = (ReferenceEntry) table.get(i); e != null; e = e.getNext()) {
                                if (!e.getValueReference().isComputingReference()) {
                                    enqueueNotification(e, RemovalCause.EXPLICIT);
                                }
                            }
                        }
                    }
                    for (i = 0; i < table.length(); i++) {
                        table.set(i, null);
                    }
                    clearReferenceQueues();
                    this.evictionQueue.clear();
                    this.expirationQueue.clear();
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
        @GuardedBy("Segment.this")
        public ReferenceEntry<K, V> removeFromChain(ReferenceEntry<K, V> first, ReferenceEntry<K, V> entry) {
            this.evictionQueue.remove(entry);
            this.expirationQueue.remove(entry);
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
        public void removeCollectedEntry(ReferenceEntry<K, V> entry) {
            enqueueNotification(entry, RemovalCause.COLLECTED);
            this.evictionQueue.remove(entry);
            this.expirationQueue.remove(entry);
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
                        enqueueNotification(e.getKey(), hash, e.getValueReference().get(), RemovalCause.COLLECTED);
                        int newCount2 = this.count - z;
                        table.set(index, removeFromChain(first, e));
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
        public boolean reclaimValue(K key, int hash, ValueReference<K, V> valueReference) {
            lock();
            boolean z;
            try {
                z = true;
                int newCount = this.count - 1;
                AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
                int index = (table.length() - 1) & hash;
                ReferenceEntry<K, V> first = (ReferenceEntry) table.get(index);
                ReferenceEntry<K, V> e = first;
                while (e != null) {
                    K entryKey = e.getKey();
                    if (e.getHash() != hash || entryKey == null || !this.map.keyEquivalence.equivalent(key, entryKey)) {
                        e = e.getNext();
                    } else if (e.getValueReference() == valueReference) {
                        this.modCount++;
                        enqueueNotification(key, hash, valueReference.get(), RemovalCause.COLLECTED);
                        int newCount2 = this.count - 1;
                        table.set(index, removeFromChain(first, e));
                        this.count = newCount2;
                        return z;
                    } else {
                        unlock();
                        if (!isHeldByCurrentThread()) {
                            postWriteCleanup();
                        }
                        return false;
                    }
                }
                unlock();
                if (!isHeldByCurrentThread()) {
                    postWriteCleanup();
                }
                return false;
            } finally {
                unlock();
                z = isHeldByCurrentThread();
                if (!z) {
                    postWriteCleanup();
                }
            }
        }

        /* Access modifiers changed, original: 0000 */
        public boolean clearValue(K key, int hash, ValueReference<K, V> valueReference) {
            lock();
            try {
                AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
                boolean z = true;
                int index = (table.length() - z) & hash;
                ReferenceEntry<K, V> first = (ReferenceEntry) table.get(index);
                ReferenceEntry<K, V> e = first;
                while (e != null) {
                    K entryKey = e.getKey();
                    if (e.getHash() != hash || entryKey == null || !this.map.keyEquivalence.equivalent(key, entryKey)) {
                        e = e.getNext();
                    } else if (e.getValueReference() == valueReference) {
                        table.set(index, removeFromChain(first, e));
                        return z;
                    } else {
                        unlock();
                        postWriteCleanup();
                        return false;
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
        @GuardedBy("Segment.this")
        public boolean removeEntry(ReferenceEntry<K, V> entry, int hash, RemovalCause cause) {
            int newCount = this.count - 1;
            AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
            int index = (table.length() - 1) & hash;
            ReferenceEntry<K, V> first = (ReferenceEntry) table.get(index);
            for (ReferenceEntry<K, V> e = first; e != null; e = e.getNext()) {
                if (e == entry) {
                    this.modCount++;
                    enqueueNotification(e.getKey(), hash, e.getValueReference().get(), cause);
                    int newCount2 = this.count - 1;
                    table.set(index, removeFromChain(first, e));
                    this.count = newCount2;
                    return true;
                }
            }
            return false;
        }

        /* Access modifiers changed, original: 0000 */
        public boolean isCollected(ValueReference<K, V> valueReference) {
            boolean z = false;
            if (valueReference.isComputingReference()) {
                return false;
            }
            if (valueReference.get() == null) {
                z = true;
            }
            return z;
        }

        /* Access modifiers changed, original: 0000 */
        public V getLiveValue(ReferenceEntry<K, V> entry) {
            if (entry.getKey() == null) {
                tryDrainReferenceQueues();
                return null;
            }
            V value = entry.getValueReference().get();
            if (value == null) {
                tryDrainReferenceQueues();
                return null;
            } else if (!this.map.expires() || !this.map.isExpired(entry)) {
                return value;
            } else {
                tryExpireEntries();
                return null;
            }
        }

        /* Access modifiers changed, original: 0000 */
        public void postReadCleanup() {
            if ((this.readCount.incrementAndGet() & 63) == 0) {
                runCleanup();
            }
        }

        /* Access modifiers changed, original: 0000 */
        @GuardedBy("Segment.this")
        public void preWriteCleanup() {
            runLockedCleanup();
        }

        /* Access modifiers changed, original: 0000 */
        public void postWriteCleanup() {
            runUnlockedCleanup();
        }

        /* Access modifiers changed, original: 0000 */
        public void runCleanup() {
            runLockedCleanup();
            runUnlockedCleanup();
        }

        /* Access modifiers changed, original: 0000 */
        public void runLockedCleanup() {
            if (tryLock()) {
                try {
                    drainReferenceQueues();
                    expireEntries();
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
            public <K, V> ValueReference<K, V> referenceValue(Segment<K, V> segment, ReferenceEntry<K, V> referenceEntry, V value) {
                return new StrongValueReference(value);
            }

            /* Access modifiers changed, original: 0000 */
            public Equivalence<Object> defaultEquivalence() {
                return Equivalence.equals();
            }
        },
        SOFT {
            /* Access modifiers changed, original: 0000 */
            public <K, V> ValueReference<K, V> referenceValue(Segment<K, V> segment, ReferenceEntry<K, V> entry, V value) {
                return new SoftValueReference(segment.valueReferenceQueue, value, entry);
            }

            /* Access modifiers changed, original: 0000 */
            public Equivalence<Object> defaultEquivalence() {
                return Equivalence.identity();
            }
        },
        WEAK {
            /* Access modifiers changed, original: 0000 */
            public <K, V> ValueReference<K, V> referenceValue(Segment<K, V> segment, ReferenceEntry<K, V> entry, V value) {
                return new WeakValueReference(segment.valueReferenceQueue, value, entry);
            }

            /* Access modifiers changed, original: 0000 */
            public Equivalence<Object> defaultEquivalence() {
                return Equivalence.identity();
            }
        };

        public abstract Equivalence<Object> defaultEquivalence();

        public abstract <K, V> ValueReference<K, V> referenceValue(Segment<K, V> segment, ReferenceEntry<K, V> referenceEntry, V v);
    }

    interface ValueReference<K, V> {
        void clear(@Nullable ValueReference<K, V> valueReference);

        ValueReference<K, V> copyFor(ReferenceQueue<V> referenceQueue, @Nullable V v, ReferenceEntry<K, V> referenceEntry);

        V get();

        ReferenceEntry<K, V> getEntry();

        boolean isComputingReference();

        V waitForValue() throws ExecutionException;
    }

    final class Values extends AbstractCollection<V> {
        Values() {
        }

        public Iterator<V> iterator() {
            return new ValueIterator();
        }

        public int size() {
            return MapMakerInternalMap.this.size();
        }

        public boolean isEmpty() {
            return MapMakerInternalMap.this.isEmpty();
        }

        public boolean contains(Object o) {
            return MapMakerInternalMap.this.containsValue(o);
        }

        public void clear() {
            MapMakerInternalMap.this.clear();
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

        public long getExpirationTime() {
            throw new UnsupportedOperationException();
        }

        public void setExpirationTime(long time) {
            throw new UnsupportedOperationException();
        }

        public ReferenceEntry<K, V> getNextExpirable() {
            throw new UnsupportedOperationException();
        }

        public void setNextExpirable(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        public ReferenceEntry<K, V> getPreviousExpirable() {
            throw new UnsupportedOperationException();
        }

        public void setPreviousExpirable(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        public ReferenceEntry<K, V> getNextEvictable() {
            throw new UnsupportedOperationException();
        }

        public void setNextEvictable(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        public ReferenceEntry<K, V> getPreviousEvictable() {
            throw new UnsupportedOperationException();
        }

        public void setPreviousEvictable(ReferenceEntry<K, V> referenceEntry) {
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

    final class KeyIterator extends HashIterator<K> {
        KeyIterator() {
            super();
        }

        public K next() {
            return nextEntry().getKey();
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

        public long getExpirationTime() {
            return 0;
        }

        public void setExpirationTime(long time) {
        }

        public ReferenceEntry<Object, Object> getNextExpirable() {
            return this;
        }

        public void setNextExpirable(ReferenceEntry<Object, Object> referenceEntry) {
        }

        public ReferenceEntry<Object, Object> getPreviousExpirable() {
            return this;
        }

        public void setPreviousExpirable(ReferenceEntry<Object, Object> referenceEntry) {
        }

        public ReferenceEntry<Object, Object> getNextEvictable() {
            return this;
        }

        public void setNextEvictable(ReferenceEntry<Object, Object> referenceEntry) {
        }

        public ReferenceEntry<Object, Object> getPreviousEvictable() {
            return this;
        }

        public void setPreviousEvictable(ReferenceEntry<Object, Object> referenceEntry) {
        }
    }

    static class SoftEntry<K, V> extends SoftReference<K> implements ReferenceEntry<K, V> {
        final int hash;
        final ReferenceEntry<K, V> next;
        volatile ValueReference<K, V> valueReference = MapMakerInternalMap.unset();

        SoftEntry(ReferenceQueue<K> queue, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(key, queue);
            this.hash = hash;
            this.next = next;
        }

        public K getKey() {
            return get();
        }

        public long getExpirationTime() {
            throw new UnsupportedOperationException();
        }

        public void setExpirationTime(long time) {
            throw new UnsupportedOperationException();
        }

        public ReferenceEntry<K, V> getNextExpirable() {
            throw new UnsupportedOperationException();
        }

        public void setNextExpirable(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        public ReferenceEntry<K, V> getPreviousExpirable() {
            throw new UnsupportedOperationException();
        }

        public void setPreviousExpirable(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        public ReferenceEntry<K, V> getNextEvictable() {
            throw new UnsupportedOperationException();
        }

        public void setNextEvictable(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        public ReferenceEntry<K, V> getPreviousEvictable() {
            throw new UnsupportedOperationException();
        }

        public void setPreviousEvictable(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        public ValueReference<K, V> getValueReference() {
            return this.valueReference;
        }

        public void setValueReference(ValueReference<K, V> valueReference) {
            ValueReference<K, V> previous = this.valueReference;
            this.valueReference = valueReference;
            previous.clear(valueReference);
        }

        public int getHash() {
            return this.hash;
        }

        public ReferenceEntry<K, V> getNext() {
            return this.next;
        }
    }

    static final class SoftValueReference<K, V> extends SoftReference<V> implements ValueReference<K, V> {
        final ReferenceEntry<K, V> entry;

        SoftValueReference(ReferenceQueue<V> queue, V referent, ReferenceEntry<K, V> entry) {
            super(referent, queue);
            this.entry = entry;
        }

        public ReferenceEntry<K, V> getEntry() {
            return this.entry;
        }

        public void clear(ValueReference<K, V> valueReference) {
            clear();
        }

        public ValueReference<K, V> copyFor(ReferenceQueue<V> queue, V value, ReferenceEntry<K, V> entry) {
            return new SoftValueReference(queue, value, entry);
        }

        public boolean isComputingReference() {
            return false;
        }

        public V waitForValue() {
            return get();
        }
    }

    static class StrongEntry<K, V> implements ReferenceEntry<K, V> {
        final int hash;
        final K key;
        final ReferenceEntry<K, V> next;
        volatile ValueReference<K, V> valueReference = MapMakerInternalMap.unset();

        StrongEntry(K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            this.key = key;
            this.hash = hash;
            this.next = next;
        }

        public K getKey() {
            return this.key;
        }

        public long getExpirationTime() {
            throw new UnsupportedOperationException();
        }

        public void setExpirationTime(long time) {
            throw new UnsupportedOperationException();
        }

        public ReferenceEntry<K, V> getNextExpirable() {
            throw new UnsupportedOperationException();
        }

        public void setNextExpirable(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        public ReferenceEntry<K, V> getPreviousExpirable() {
            throw new UnsupportedOperationException();
        }

        public void setPreviousExpirable(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        public ReferenceEntry<K, V> getNextEvictable() {
            throw new UnsupportedOperationException();
        }

        public void setNextEvictable(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        public ReferenceEntry<K, V> getPreviousEvictable() {
            throw new UnsupportedOperationException();
        }

        public void setPreviousEvictable(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        public ValueReference<K, V> getValueReference() {
            return this.valueReference;
        }

        public void setValueReference(ValueReference<K, V> valueReference) {
            ValueReference<K, V> previous = this.valueReference;
            this.valueReference = valueReference;
            previous.clear(valueReference);
        }

        public int getHash() {
            return this.hash;
        }

        public ReferenceEntry<K, V> getNext() {
            return this.next;
        }
    }

    static final class StrongValueReference<K, V> implements ValueReference<K, V> {
        final V referent;

        StrongValueReference(V referent) {
            this.referent = referent;
        }

        public V get() {
            return this.referent;
        }

        public ReferenceEntry<K, V> getEntry() {
            return null;
        }

        public ValueReference<K, V> copyFor(ReferenceQueue<V> referenceQueue, V v, ReferenceEntry<K, V> referenceEntry) {
            return this;
        }

        public boolean isComputingReference() {
            return false;
        }

        public V waitForValue() {
            return get();
        }

        public void clear(ValueReference<K, V> valueReference) {
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
        volatile ValueReference<K, V> valueReference = MapMakerInternalMap.unset();

        WeakEntry(ReferenceQueue<K> queue, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(key, queue);
            this.hash = hash;
            this.next = next;
        }

        public K getKey() {
            return get();
        }

        public long getExpirationTime() {
            throw new UnsupportedOperationException();
        }

        public void setExpirationTime(long time) {
            throw new UnsupportedOperationException();
        }

        public ReferenceEntry<K, V> getNextExpirable() {
            throw new UnsupportedOperationException();
        }

        public void setNextExpirable(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        public ReferenceEntry<K, V> getPreviousExpirable() {
            throw new UnsupportedOperationException();
        }

        public void setPreviousExpirable(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        public ReferenceEntry<K, V> getNextEvictable() {
            throw new UnsupportedOperationException();
        }

        public void setNextEvictable(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        public ReferenceEntry<K, V> getPreviousEvictable() {
            throw new UnsupportedOperationException();
        }

        public void setPreviousEvictable(ReferenceEntry<K, V> referenceEntry) {
            throw new UnsupportedOperationException();
        }

        public ValueReference<K, V> getValueReference() {
            return this.valueReference;
        }

        public void setValueReference(ValueReference<K, V> valueReference) {
            ValueReference<K, V> previous = this.valueReference;
            this.valueReference = valueReference;
            previous.clear(valueReference);
        }

        public int getHash() {
            return this.hash;
        }

        public ReferenceEntry<K, V> getNext() {
            return this.next;
        }
    }

    static final class WeakValueReference<K, V> extends WeakReference<V> implements ValueReference<K, V> {
        final ReferenceEntry<K, V> entry;

        WeakValueReference(ReferenceQueue<V> queue, V referent, ReferenceEntry<K, V> entry) {
            super(referent, queue);
            this.entry = entry;
        }

        public ReferenceEntry<K, V> getEntry() {
            return this.entry;
        }

        public void clear(ValueReference<K, V> valueReference) {
            clear();
        }

        public ValueReference<K, V> copyFor(ReferenceQueue<V> queue, V value, ReferenceEntry<K, V> entry) {
            return new WeakValueReference(queue, value, entry);
        }

        public boolean isComputingReference() {
            return false;
        }

        public V waitForValue() {
            return get();
        }
    }

    final class WriteThroughEntry extends AbstractMapEntry<K, V> {
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

        public V setValue(V newValue) {
            V oldValue = MapMakerInternalMap.this.put(this.key, newValue);
            this.value = newValue;
            return oldValue;
        }
    }

    static final class SoftEvictableEntry<K, V> extends SoftEntry<K, V> implements ReferenceEntry<K, V> {
        ReferenceEntry<K, V> nextEvictable = MapMakerInternalMap.nullEntry();
        ReferenceEntry<K, V> previousEvictable = MapMakerInternalMap.nullEntry();

        SoftEvictableEntry(ReferenceQueue<K> queue, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(queue, key, hash, next);
        }

        public ReferenceEntry<K, V> getNextEvictable() {
            return this.nextEvictable;
        }

        public void setNextEvictable(ReferenceEntry<K, V> next) {
            this.nextEvictable = next;
        }

        public ReferenceEntry<K, V> getPreviousEvictable() {
            return this.previousEvictable;
        }

        public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
            this.previousEvictable = previous;
        }
    }

    static final class SoftExpirableEntry<K, V> extends SoftEntry<K, V> implements ReferenceEntry<K, V> {
        ReferenceEntry<K, V> nextExpirable = MapMakerInternalMap.nullEntry();
        ReferenceEntry<K, V> previousExpirable = MapMakerInternalMap.nullEntry();
        volatile long time = Long.MAX_VALUE;

        SoftExpirableEntry(ReferenceQueue<K> queue, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(queue, key, hash, next);
        }

        public long getExpirationTime() {
            return this.time;
        }

        public void setExpirationTime(long time) {
            this.time = time;
        }

        public ReferenceEntry<K, V> getNextExpirable() {
            return this.nextExpirable;
        }

        public void setNextExpirable(ReferenceEntry<K, V> next) {
            this.nextExpirable = next;
        }

        public ReferenceEntry<K, V> getPreviousExpirable() {
            return this.previousExpirable;
        }

        public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
            this.previousExpirable = previous;
        }
    }

    static final class SoftExpirableEvictableEntry<K, V> extends SoftEntry<K, V> implements ReferenceEntry<K, V> {
        ReferenceEntry<K, V> nextEvictable = MapMakerInternalMap.nullEntry();
        ReferenceEntry<K, V> nextExpirable = MapMakerInternalMap.nullEntry();
        ReferenceEntry<K, V> previousEvictable = MapMakerInternalMap.nullEntry();
        ReferenceEntry<K, V> previousExpirable = MapMakerInternalMap.nullEntry();
        volatile long time = Long.MAX_VALUE;

        SoftExpirableEvictableEntry(ReferenceQueue<K> queue, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(queue, key, hash, next);
        }

        public long getExpirationTime() {
            return this.time;
        }

        public void setExpirationTime(long time) {
            this.time = time;
        }

        public ReferenceEntry<K, V> getNextExpirable() {
            return this.nextExpirable;
        }

        public void setNextExpirable(ReferenceEntry<K, V> next) {
            this.nextExpirable = next;
        }

        public ReferenceEntry<K, V> getPreviousExpirable() {
            return this.previousExpirable;
        }

        public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
            this.previousExpirable = previous;
        }

        public ReferenceEntry<K, V> getNextEvictable() {
            return this.nextEvictable;
        }

        public void setNextEvictable(ReferenceEntry<K, V> next) {
            this.nextEvictable = next;
        }

        public ReferenceEntry<K, V> getPreviousEvictable() {
            return this.previousEvictable;
        }

        public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
            this.previousEvictable = previous;
        }
    }

    static final class StrongEvictableEntry<K, V> extends StrongEntry<K, V> implements ReferenceEntry<K, V> {
        ReferenceEntry<K, V> nextEvictable = MapMakerInternalMap.nullEntry();
        ReferenceEntry<K, V> previousEvictable = MapMakerInternalMap.nullEntry();

        StrongEvictableEntry(K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(key, hash, next);
        }

        public ReferenceEntry<K, V> getNextEvictable() {
            return this.nextEvictable;
        }

        public void setNextEvictable(ReferenceEntry<K, V> next) {
            this.nextEvictable = next;
        }

        public ReferenceEntry<K, V> getPreviousEvictable() {
            return this.previousEvictable;
        }

        public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
            this.previousEvictable = previous;
        }
    }

    static final class StrongExpirableEntry<K, V> extends StrongEntry<K, V> implements ReferenceEntry<K, V> {
        ReferenceEntry<K, V> nextExpirable = MapMakerInternalMap.nullEntry();
        ReferenceEntry<K, V> previousExpirable = MapMakerInternalMap.nullEntry();
        volatile long time = Long.MAX_VALUE;

        StrongExpirableEntry(K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(key, hash, next);
        }

        public long getExpirationTime() {
            return this.time;
        }

        public void setExpirationTime(long time) {
            this.time = time;
        }

        public ReferenceEntry<K, V> getNextExpirable() {
            return this.nextExpirable;
        }

        public void setNextExpirable(ReferenceEntry<K, V> next) {
            this.nextExpirable = next;
        }

        public ReferenceEntry<K, V> getPreviousExpirable() {
            return this.previousExpirable;
        }

        public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
            this.previousExpirable = previous;
        }
    }

    static final class StrongExpirableEvictableEntry<K, V> extends StrongEntry<K, V> implements ReferenceEntry<K, V> {
        ReferenceEntry<K, V> nextEvictable = MapMakerInternalMap.nullEntry();
        ReferenceEntry<K, V> nextExpirable = MapMakerInternalMap.nullEntry();
        ReferenceEntry<K, V> previousEvictable = MapMakerInternalMap.nullEntry();
        ReferenceEntry<K, V> previousExpirable = MapMakerInternalMap.nullEntry();
        volatile long time = Long.MAX_VALUE;

        StrongExpirableEvictableEntry(K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(key, hash, next);
        }

        public long getExpirationTime() {
            return this.time;
        }

        public void setExpirationTime(long time) {
            this.time = time;
        }

        public ReferenceEntry<K, V> getNextExpirable() {
            return this.nextExpirable;
        }

        public void setNextExpirable(ReferenceEntry<K, V> next) {
            this.nextExpirable = next;
        }

        public ReferenceEntry<K, V> getPreviousExpirable() {
            return this.previousExpirable;
        }

        public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
            this.previousExpirable = previous;
        }

        public ReferenceEntry<K, V> getNextEvictable() {
            return this.nextEvictable;
        }

        public void setNextEvictable(ReferenceEntry<K, V> next) {
            this.nextEvictable = next;
        }

        public ReferenceEntry<K, V> getPreviousEvictable() {
            return this.previousEvictable;
        }

        public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
            this.previousEvictable = previous;
        }
    }

    static final class WeakEvictableEntry<K, V> extends WeakEntry<K, V> implements ReferenceEntry<K, V> {
        ReferenceEntry<K, V> nextEvictable = MapMakerInternalMap.nullEntry();
        ReferenceEntry<K, V> previousEvictable = MapMakerInternalMap.nullEntry();

        WeakEvictableEntry(ReferenceQueue<K> queue, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(queue, key, hash, next);
        }

        public ReferenceEntry<K, V> getNextEvictable() {
            return this.nextEvictable;
        }

        public void setNextEvictable(ReferenceEntry<K, V> next) {
            this.nextEvictable = next;
        }

        public ReferenceEntry<K, V> getPreviousEvictable() {
            return this.previousEvictable;
        }

        public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
            this.previousEvictable = previous;
        }
    }

    static final class WeakExpirableEntry<K, V> extends WeakEntry<K, V> implements ReferenceEntry<K, V> {
        ReferenceEntry<K, V> nextExpirable = MapMakerInternalMap.nullEntry();
        ReferenceEntry<K, V> previousExpirable = MapMakerInternalMap.nullEntry();
        volatile long time = Long.MAX_VALUE;

        WeakExpirableEntry(ReferenceQueue<K> queue, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(queue, key, hash, next);
        }

        public long getExpirationTime() {
            return this.time;
        }

        public void setExpirationTime(long time) {
            this.time = time;
        }

        public ReferenceEntry<K, V> getNextExpirable() {
            return this.nextExpirable;
        }

        public void setNextExpirable(ReferenceEntry<K, V> next) {
            this.nextExpirable = next;
        }

        public ReferenceEntry<K, V> getPreviousExpirable() {
            return this.previousExpirable;
        }

        public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
            this.previousExpirable = previous;
        }
    }

    static final class WeakExpirableEvictableEntry<K, V> extends WeakEntry<K, V> implements ReferenceEntry<K, V> {
        ReferenceEntry<K, V> nextEvictable = MapMakerInternalMap.nullEntry();
        ReferenceEntry<K, V> nextExpirable = MapMakerInternalMap.nullEntry();
        ReferenceEntry<K, V> previousEvictable = MapMakerInternalMap.nullEntry();
        ReferenceEntry<K, V> previousExpirable = MapMakerInternalMap.nullEntry();
        volatile long time = Long.MAX_VALUE;

        WeakExpirableEvictableEntry(ReferenceQueue<K> queue, K key, int hash, @Nullable ReferenceEntry<K, V> next) {
            super(queue, key, hash, next);
        }

        public long getExpirationTime() {
            return this.time;
        }

        public void setExpirationTime(long time) {
            this.time = time;
        }

        public ReferenceEntry<K, V> getNextExpirable() {
            return this.nextExpirable;
        }

        public void setNextExpirable(ReferenceEntry<K, V> next) {
            this.nextExpirable = next;
        }

        public ReferenceEntry<K, V> getPreviousExpirable() {
            return this.previousExpirable;
        }

        public void setPreviousExpirable(ReferenceEntry<K, V> previous) {
            this.previousExpirable = previous;
        }

        public ReferenceEntry<K, V> getNextEvictable() {
            return this.nextEvictable;
        }

        public void setNextEvictable(ReferenceEntry<K, V> next) {
            this.nextEvictable = next;
        }

        public ReferenceEntry<K, V> getPreviousEvictable() {
            return this.previousEvictable;
        }

        public void setPreviousEvictable(ReferenceEntry<K, V> previous) {
            this.previousEvictable = previous;
        }
    }

    static abstract class AbstractSerializationProxy<K, V> extends ForwardingConcurrentMap<K, V> implements Serializable {
        private static final long serialVersionUID = 3;
        final int concurrencyLevel;
        transient ConcurrentMap<K, V> delegate;
        final long expireAfterAccessNanos;
        final long expireAfterWriteNanos;
        final Equivalence<Object> keyEquivalence;
        final Strength keyStrength;
        final int maximumSize;
        final RemovalListener<? super K, ? super V> removalListener;
        final Equivalence<Object> valueEquivalence;
        final Strength valueStrength;

        AbstractSerializationProxy(Strength keyStrength, Strength valueStrength, Equivalence<Object> keyEquivalence, Equivalence<Object> valueEquivalence, long expireAfterWriteNanos, long expireAfterAccessNanos, int maximumSize, int concurrencyLevel, RemovalListener<? super K, ? super V> removalListener, ConcurrentMap<K, V> delegate) {
            this.keyStrength = keyStrength;
            this.valueStrength = valueStrength;
            this.keyEquivalence = keyEquivalence;
            this.valueEquivalence = valueEquivalence;
            this.expireAfterWriteNanos = expireAfterWriteNanos;
            this.expireAfterAccessNanos = expireAfterAccessNanos;
            this.maximumSize = maximumSize;
            this.concurrencyLevel = concurrencyLevel;
            this.removalListener = removalListener;
            this.delegate = delegate;
        }

        /* Access modifiers changed, original: protected */
        public ConcurrentMap<K, V> delegate() {
            return this.delegate;
        }

        /* Access modifiers changed, original: 0000 */
        public void writeMapTo(ObjectOutputStream out) throws IOException {
            out.writeInt(this.delegate.size());
            for (Entry<K, V> entry : this.delegate.entrySet()) {
                out.writeObject(entry.getKey());
                out.writeObject(entry.getValue());
            }
            out.writeObject(null);
        }

        /* Access modifiers changed, original: 0000 */
        public MapMaker readMapMaker(ObjectInputStream in) throws IOException {
            MapMaker mapMaker = new MapMaker().initialCapacity(in.readInt()).setKeyStrength(this.keyStrength).setValueStrength(this.valueStrength).keyEquivalence(this.keyEquivalence).concurrencyLevel(this.concurrencyLevel);
            mapMaker.removalListener(this.removalListener);
            if (this.expireAfterWriteNanos > 0) {
                mapMaker.expireAfterWrite(this.expireAfterWriteNanos, TimeUnit.NANOSECONDS);
            }
            if (this.expireAfterAccessNanos > 0) {
                mapMaker.expireAfterAccess(this.expireAfterAccessNanos, TimeUnit.NANOSECONDS);
            }
            if (this.maximumSize != -1) {
                mapMaker.maximumSize(this.maximumSize);
            }
            return mapMaker;
        }

        /* Access modifiers changed, original: 0000 */
        public void readEntries(ObjectInputStream in) throws IOException, ClassNotFoundException {
            while (true) {
                K key = in.readObject();
                if (key != null) {
                    this.delegate.put(key, in.readObject());
                } else {
                    return;
                }
            }
        }
    }

    private static final class SerializationProxy<K, V> extends AbstractSerializationProxy<K, V> {
        private static final long serialVersionUID = 3;

        SerializationProxy(Strength keyStrength, Strength valueStrength, Equivalence<Object> keyEquivalence, Equivalence<Object> valueEquivalence, long expireAfterWriteNanos, long expireAfterAccessNanos, int maximumSize, int concurrencyLevel, RemovalListener<? super K, ? super V> removalListener, ConcurrentMap<K, V> delegate) {
            super(keyStrength, valueStrength, keyEquivalence, valueEquivalence, expireAfterWriteNanos, expireAfterAccessNanos, maximumSize, concurrencyLevel, removalListener, delegate);
        }

        private void writeObject(ObjectOutputStream out) throws IOException {
            out.defaultWriteObject();
            writeMapTo(out);
        }

        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            this.delegate = readMapMaker(in).makeMap();
            readEntries(in);
        }

        private Object readResolve() {
            return this.delegate;
        }
    }

    MapMakerInternalMap(MapMaker builder) {
        Queue discardingQueue;
        this.concurrencyLevel = Math.min(builder.getConcurrencyLevel(), 65536);
        this.keyStrength = builder.getKeyStrength();
        this.valueStrength = builder.getValueStrength();
        this.keyEquivalence = builder.getKeyEquivalence();
        this.maximumSize = builder.maximumSize;
        this.expireAfterAccessNanos = builder.getExpireAfterAccessNanos();
        this.expireAfterWriteNanos = builder.getExpireAfterWriteNanos();
        this.entryFactory = EntryFactory.getFactory(this.keyStrength, expires(), evictsBySize());
        this.ticker = builder.getTicker();
        this.removalListener = builder.getRemovalListener();
        if (this.removalListener == NullListener.INSTANCE) {
            discardingQueue = discardingQueue();
        } else {
            discardingQueue = new ConcurrentLinkedQueue();
        }
        this.removalNotificationQueue = discardingQueue;
        int initialCapacity = Math.min(builder.getInitialCapacity(), 1073741824);
        if (evictsBySize()) {
            initialCapacity = Math.min(initialCapacity, this.maximumSize);
        }
        int segmentShift = 0;
        int segmentCount = 1;
        while (segmentCount < this.concurrencyLevel && (!evictsBySize() || segmentCount * 2 <= this.maximumSize)) {
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
        int segmentSize = 1;
        while (segmentSize < segmentCapacity) {
            segmentSize <<= 1;
        }
        int i = 0;
        int remainder;
        if (evictsBySize()) {
            int maximumSegmentSize = (this.maximumSize / segmentCount) + 1;
            remainder = this.maximumSize % segmentCount;
            while (i < this.segments.length) {
                if (i == remainder) {
                    maximumSegmentSize--;
                }
                this.segments[i] = createSegment(segmentSize, maximumSegmentSize);
                i++;
            }
            return;
        }
        while (true) {
            remainder = i;
            if (remainder < this.segments.length) {
                this.segments[remainder] = createSegment(segmentSize, -1);
                i = remainder + 1;
            } else {
                return;
            }
        }
    }

    /* Access modifiers changed, original: 0000 */
    public boolean evictsBySize() {
        return this.maximumSize != -1;
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
        return segmentFor(hash).newEntry(key, hash, next);
    }

    /* Access modifiers changed, original: 0000 */
    @VisibleForTesting
    public ReferenceEntry<K, V> copyEntry(ReferenceEntry<K, V> original, ReferenceEntry<K, V> newNext) {
        return segmentFor(original.getHash()).copyEntry(original, newNext);
    }

    /* Access modifiers changed, original: 0000 */
    @VisibleForTesting
    public ValueReference<K, V> newValueReference(ReferenceEntry<K, V> entry, V value) {
        return this.valueStrength.referenceValue(segmentFor(entry.getHash()), entry, value);
    }

    /* Access modifiers changed, original: 0000 */
    public int hash(Object key) {
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
    public boolean isLive(ReferenceEntry<K, V> entry) {
        return segmentFor(entry.getHash()).getLiveValue(entry) != null;
    }

    /* Access modifiers changed, original: 0000 */
    public Segment<K, V> segmentFor(int hash) {
        return this.segments[(hash >>> this.segmentShift) & this.segmentMask];
    }

    /* Access modifiers changed, original: 0000 */
    public Segment<K, V> createSegment(int initialCapacity, int maxSegmentSize) {
        return new Segment(this, initialCapacity, maxSegmentSize);
    }

    /* Access modifiers changed, original: 0000 */
    public V getLiveValue(ReferenceEntry<K, V> entry) {
        if (entry.getKey() == null) {
            return null;
        }
        V value = entry.getValueReference().get();
        if (value == null) {
            return null;
        }
        if (expires() && isExpired(entry)) {
            return null;
        }
        return value;
    }

    /* Access modifiers changed, original: 0000 */
    public boolean isExpired(ReferenceEntry<K, V> entry) {
        return isExpired(entry, this.ticker.read());
    }

    /* Access modifiers changed, original: 0000 */
    public boolean isExpired(ReferenceEntry<K, V> entry, long now) {
        return now - entry.getExpirationTime() > 0;
    }

    static <K, V> void connectExpirables(ReferenceEntry<K, V> previous, ReferenceEntry<K, V> next) {
        previous.setNextExpirable(next);
        next.setPreviousExpirable(previous);
    }

    static <K, V> void nullifyExpirable(ReferenceEntry<K, V> nulled) {
        ReferenceEntry<K, V> nullEntry = nullEntry();
        nulled.setNextExpirable(nullEntry);
        nulled.setPreviousExpirable(nullEntry);
    }

    /* Access modifiers changed, original: 0000 */
    public void processPendingNotifications() {
        while (true) {
            RemovalNotification<K, V> removalNotification = (RemovalNotification) this.removalNotificationQueue.poll();
            RemovalNotification<K, V> notification = removalNotification;
            if (removalNotification != null) {
                try {
                    this.removalListener.onRemoval(notification);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Exception thrown by removal listener", e);
                }
            } else {
                return;
            }
        }
    }

    static <K, V> void connectEvictables(ReferenceEntry<K, V> previous, ReferenceEntry<K, V> next) {
        previous.setNextEvictable(next);
        next.setPreviousEvictable(previous);
    }

    static <K, V> void nullifyEvictable(ReferenceEntry<K, V> nulled) {
        ReferenceEntry<K, V> nullEntry = nullEntry();
        nulled.setNextEvictable(nullEntry);
        nulled.setPreviousEvictable(nullEntry);
    }

    /* Access modifiers changed, original: final */
    public final Segment<K, V>[] newSegmentArray(int ssize) {
        return new Segment[ssize];
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

    public int size() {
        long sum = 0;
        for (Segment segment : this.segments) {
            sum += (long) segment.count;
        }
        return Ints.saturatedCast(sum);
    }

    public V get(@Nullable Object key) {
        if (key == null) {
            return null;
        }
        int hash = hash(key);
        return segmentFor(hash).get(key, hash);
    }

    /* Access modifiers changed, original: 0000 */
    public ReferenceEntry<K, V> getEntry(@Nullable Object key) {
        if (key == null) {
            return null;
        }
        int hash = hash(key);
        return segmentFor(hash).getEntry(key, hash);
    }

    public boolean containsKey(@Nullable Object key) {
        if (key == null) {
            return false;
        }
        int hash = hash(key);
        return segmentFor(hash).containsKey(key, hash);
    }

    public boolean containsValue(@Nullable Object value) {
        Object obj = value;
        boolean z = false;
        if (obj == null) {
            return false;
        }
        Segment<K, V>[] segments = this.segments;
        long last = -1;
        int i = 0;
        while (i < 3) {
            Segment<K, V>[] segments2;
            int length = segments.length;
            long sum = 0;
            int sum2 = z;
            while (sum2 < length) {
                Segment<K, V> segment = segments[sum2];
                int c = segment.count;
                AtomicReferenceArray<ReferenceEntry<K, V>> table = segment.table;
                for (int j = z; j < table.length(); j++) {
                    ReferenceEntry<K, V> e = (ReferenceEntry) table.get(j);
                    while (e != null) {
                        V v = segment.getLiveValue(e);
                        if (v != null) {
                            segments2 = segments;
                            if (this.valueEquivalence.equivalent(obj, v)) {
                                return true;
                            }
                        } else {
                            segments2 = segments;
                        }
                        e = e.getNext();
                        segments = segments2;
                    }
                }
                sum += (long) segment.modCount;
                sum2++;
                segments = segments;
                z = false;
            }
            segments2 = segments;
            if (sum == last) {
                break;
            }
            last = sum;
            i++;
            segments = segments2;
            z = false;
        }
        return false;
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

    public Set<K> keySet() {
        Set<K> ks = this.keySet;
        if (ks != null) {
            return ks;
        }
        KeySet keySet = new KeySet();
        this.keySet = keySet;
        return keySet;
    }

    public Collection<V> values() {
        Collection<V> vs = this.values;
        if (vs != null) {
            return vs;
        }
        Values values = new Values();
        this.values = values;
        return values;
    }

    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> es = this.entrySet;
        if (es != null) {
            return es;
        }
        EntrySet entrySet = new EntrySet();
        this.entrySet = entrySet;
        return entrySet;
    }

    /* Access modifiers changed, original: 0000 */
    public Object writeReplace() {
        return new SerializationProxy(this.keyStrength, this.valueStrength, this.keyEquivalence, this.valueEquivalence, this.expireAfterWriteNanos, this.expireAfterAccessNanos, this.maximumSize, this.concurrencyLevel, this.removalListener, this);
    }
}
