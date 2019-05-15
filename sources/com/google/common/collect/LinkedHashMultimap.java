package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.annotation.Nullable;

@GwtCompatible(emulated = true, serializable = true)
public final class LinkedHashMultimap<K, V> extends AbstractSetMultimap<K, V> {
    private static final int DEFAULT_KEY_CAPACITY = 16;
    private static final int DEFAULT_VALUE_SET_CAPACITY = 2;
    @VisibleForTesting
    static final double VALUE_SET_LOAD_FACTOR = 1.0d;
    @GwtIncompatible("java serialization not supported")
    private static final long serialVersionUID = 1;
    private transient ValueEntry<K, V> multimapHeaderEntry;
    @VisibleForTesting
    transient int valueSetCapacity = 2;

    private interface ValueSetLink<K, V> {
        ValueSetLink<K, V> getPredecessorInValueSet();

        ValueSetLink<K, V> getSuccessorInValueSet();

        void setPredecessorInValueSet(ValueSetLink<K, V> valueSetLink);

        void setSuccessorInValueSet(ValueSetLink<K, V> valueSetLink);
    }

    @VisibleForTesting
    final class ValueSet extends ImprovedAbstractSet<V> implements ValueSetLink<K, V> {
        private ValueSetLink<K, V> firstEntry;
        @VisibleForTesting
        ValueEntry<K, V>[] hashTable;
        private final K key;
        private ValueSetLink<K, V> lastEntry;
        private int modCount = 0;
        private int size = 0;

        ValueSet(K key, int expectedValues) {
            this.key = key;
            this.firstEntry = this;
            this.lastEntry = this;
            this.hashTable = new ValueEntry[Hashing.closedTableSize(expectedValues, 0)];
        }

        private int mask() {
            return this.hashTable.length - 1;
        }

        public ValueSetLink<K, V> getPredecessorInValueSet() {
            return this.lastEntry;
        }

        public ValueSetLink<K, V> getSuccessorInValueSet() {
            return this.firstEntry;
        }

        public void setPredecessorInValueSet(ValueSetLink<K, V> entry) {
            this.lastEntry = entry;
        }

        public void setSuccessorInValueSet(ValueSetLink<K, V> entry) {
            this.firstEntry = entry;
        }

        public Iterator<V> iterator() {
            return new Iterator<V>() {
                int expectedModCount = ValueSet.this.modCount;
                ValueSetLink<K, V> nextEntry = ValueSet.this.firstEntry;
                ValueEntry<K, V> toRemove;

                private void checkForComodification() {
                    if (ValueSet.this.modCount != this.expectedModCount) {
                        throw new ConcurrentModificationException();
                    }
                }

                public boolean hasNext() {
                    checkForComodification();
                    return this.nextEntry != ValueSet.this;
                }

                public V next() {
                    if (hasNext()) {
                        ValueEntry<K, V> entry = this.nextEntry;
                        V result = entry.getValue();
                        this.toRemove = entry;
                        this.nextEntry = entry.getSuccessorInValueSet();
                        return result;
                    }
                    throw new NoSuchElementException();
                }

                public void remove() {
                    checkForComodification();
                    CollectPreconditions.checkRemove(this.toRemove != null);
                    ValueSet.this.remove(this.toRemove.getValue());
                    this.expectedModCount = ValueSet.this.modCount;
                    this.toRemove = null;
                }
            };
        }

        public int size() {
            return this.size;
        }

        public boolean contains(@Nullable Object o) {
            int smearedHash = Hashing.smearedHash(o);
            for (ValueEntry<K, V> entry = this.hashTable[mask() & smearedHash]; entry != null; entry = entry.nextInValueBucket) {
                if (entry.matchesValue(o, smearedHash)) {
                    return true;
                }
            }
            return false;
        }

        public boolean add(@Nullable V value) {
            ValueEntry<K, V> entry;
            int smearedHash = Hashing.smearedHash(value);
            int bucket = mask() & smearedHash;
            ValueEntry<K, V> rowHead = this.hashTable[bucket];
            for (entry = rowHead; entry != null; entry = entry.nextInValueBucket) {
                if (entry.matchesValue(value, smearedHash)) {
                    return false;
                }
            }
            entry = new ValueEntry(this.key, value, smearedHash, rowHead);
            LinkedHashMultimap.succeedsInValueSet(this.lastEntry, entry);
            LinkedHashMultimap.succeedsInValueSet(entry, this);
            LinkedHashMultimap.succeedsInMultimap(LinkedHashMultimap.this.multimapHeaderEntry.getPredecessorInMultimap(), entry);
            LinkedHashMultimap.succeedsInMultimap(entry, LinkedHashMultimap.this.multimapHeaderEntry);
            this.hashTable[bucket] = entry;
            this.size++;
            this.modCount++;
            rehashIfNecessary();
            return true;
        }

        private void rehashIfNecessary() {
            if (Hashing.needsResizing(this.size, this.hashTable.length, LinkedHashMultimap.VALUE_SET_LOAD_FACTOR)) {
                ValueEntry<K, V>[] hashTable = new ValueEntry[(this.hashTable.length * 2)];
                this.hashTable = hashTable;
                int mask = hashTable.length - 1;
                for (ValueEntry<K, V> entry = this.firstEntry; entry != this; entry = entry.getSuccessorInValueSet()) {
                    ValueEntry<K, V> valueEntry = entry;
                    int bucket = valueEntry.smearedValueHash & mask;
                    valueEntry.nextInValueBucket = hashTable[bucket];
                    hashTable[bucket] = valueEntry;
                }
            }
        }

        public boolean remove(@Nullable Object o) {
            int smearedHash = Hashing.smearedHash(o);
            int bucket = mask() & smearedHash;
            ValueEntry<K, V> prev = null;
            for (ValueEntry<K, V> entry = this.hashTable[bucket]; entry != null; entry = entry.nextInValueBucket) {
                if (entry.matchesValue(o, smearedHash)) {
                    if (prev == null) {
                        this.hashTable[bucket] = entry.nextInValueBucket;
                    } else {
                        prev.nextInValueBucket = entry.nextInValueBucket;
                    }
                    LinkedHashMultimap.deleteFromValueSet(entry);
                    LinkedHashMultimap.deleteFromMultimap(entry);
                    this.size--;
                    this.modCount++;
                    return true;
                }
                prev = entry;
            }
            return false;
        }

        public void clear() {
            Arrays.fill(this.hashTable, null);
            this.size = 0;
            for (ValueEntry<K, V> entry = this.firstEntry; entry != this; entry = entry.getSuccessorInValueSet()) {
                LinkedHashMultimap.deleteFromMultimap(entry);
            }
            LinkedHashMultimap.succeedsInValueSet(this, this);
            this.modCount++;
        }
    }

    @VisibleForTesting
    static final class ValueEntry<K, V> extends ImmutableEntry<K, V> implements ValueSetLink<K, V> {
        @Nullable
        ValueEntry<K, V> nextInValueBucket;
        ValueEntry<K, V> predecessorInMultimap;
        ValueSetLink<K, V> predecessorInValueSet;
        final int smearedValueHash;
        ValueEntry<K, V> successorInMultimap;
        ValueSetLink<K, V> successorInValueSet;

        ValueEntry(@Nullable K key, @Nullable V value, int smearedValueHash, @Nullable ValueEntry<K, V> nextInValueBucket) {
            super(key, value);
            this.smearedValueHash = smearedValueHash;
            this.nextInValueBucket = nextInValueBucket;
        }

        /* Access modifiers changed, original: 0000 */
        public boolean matchesValue(@Nullable Object v, int smearedVHash) {
            return this.smearedValueHash == smearedVHash && Objects.equal(getValue(), v);
        }

        public ValueSetLink<K, V> getPredecessorInValueSet() {
            return this.predecessorInValueSet;
        }

        public ValueSetLink<K, V> getSuccessorInValueSet() {
            return this.successorInValueSet;
        }

        public void setPredecessorInValueSet(ValueSetLink<K, V> entry) {
            this.predecessorInValueSet = entry;
        }

        public void setSuccessorInValueSet(ValueSetLink<K, V> entry) {
            this.successorInValueSet = entry;
        }

        public ValueEntry<K, V> getPredecessorInMultimap() {
            return this.predecessorInMultimap;
        }

        public ValueEntry<K, V> getSuccessorInMultimap() {
            return this.successorInMultimap;
        }

        public void setSuccessorInMultimap(ValueEntry<K, V> multimapSuccessor) {
            this.successorInMultimap = multimapSuccessor;
        }

        public void setPredecessorInMultimap(ValueEntry<K, V> multimapPredecessor) {
            this.predecessorInMultimap = multimapPredecessor;
        }
    }

    public /* bridge */ /* synthetic */ Map asMap() {
        return super.asMap();
    }

    public /* bridge */ /* synthetic */ boolean containsEntry(@Nullable Object obj, @Nullable Object obj2) {
        return super.containsEntry(obj, obj2);
    }

    public /* bridge */ /* synthetic */ boolean containsKey(@Nullable Object obj) {
        return super.containsKey(obj);
    }

    public /* bridge */ /* synthetic */ boolean containsValue(@Nullable Object obj) {
        return super.containsValue(obj);
    }

    public /* bridge */ /* synthetic */ boolean equals(@Nullable Object obj) {
        return super.equals(obj);
    }

    public /* bridge */ /* synthetic */ Set get(@Nullable Object obj) {
        return super.get(obj);
    }

    public /* bridge */ /* synthetic */ int hashCode() {
        return super.hashCode();
    }

    public /* bridge */ /* synthetic */ boolean isEmpty() {
        return super.isEmpty();
    }

    public /* bridge */ /* synthetic */ Set keySet() {
        return super.keySet();
    }

    public /* bridge */ /* synthetic */ Multiset keys() {
        return super.keys();
    }

    public /* bridge */ /* synthetic */ boolean put(@Nullable Object obj, @Nullable Object obj2) {
        return super.put(obj, obj2);
    }

    public /* bridge */ /* synthetic */ boolean putAll(Multimap multimap) {
        return super.putAll(multimap);
    }

    public /* bridge */ /* synthetic */ boolean putAll(@Nullable Object obj, Iterable iterable) {
        return super.putAll(obj, iterable);
    }

    public /* bridge */ /* synthetic */ boolean remove(@Nullable Object obj, @Nullable Object obj2) {
        return super.remove(obj, obj2);
    }

    public /* bridge */ /* synthetic */ Set removeAll(@Nullable Object obj) {
        return super.removeAll(obj);
    }

    public /* bridge */ /* synthetic */ int size() {
        return super.size();
    }

    public /* bridge */ /* synthetic */ String toString() {
        return super.toString();
    }

    public static <K, V> LinkedHashMultimap<K, V> create() {
        return new LinkedHashMultimap(16, 2);
    }

    public static <K, V> LinkedHashMultimap<K, V> create(int expectedKeys, int expectedValuesPerKey) {
        return new LinkedHashMultimap(Maps.capacity(expectedKeys), Maps.capacity(expectedValuesPerKey));
    }

    public static <K, V> LinkedHashMultimap<K, V> create(Multimap<? extends K, ? extends V> multimap) {
        LinkedHashMultimap<K, V> result = create(multimap.keySet().size(), 2);
        result.putAll(multimap);
        return result;
    }

    private static <K, V> void succeedsInValueSet(ValueSetLink<K, V> pred, ValueSetLink<K, V> succ) {
        pred.setSuccessorInValueSet(succ);
        succ.setPredecessorInValueSet(pred);
    }

    private static <K, V> void succeedsInMultimap(ValueEntry<K, V> pred, ValueEntry<K, V> succ) {
        pred.setSuccessorInMultimap(succ);
        succ.setPredecessorInMultimap(pred);
    }

    private static <K, V> void deleteFromValueSet(ValueSetLink<K, V> entry) {
        succeedsInValueSet(entry.getPredecessorInValueSet(), entry.getSuccessorInValueSet());
    }

    private static <K, V> void deleteFromMultimap(ValueEntry<K, V> entry) {
        succeedsInMultimap(entry.getPredecessorInMultimap(), entry.getSuccessorInMultimap());
    }

    private LinkedHashMultimap(int keyCapacity, int valueSetCapacity) {
        super(new LinkedHashMap(keyCapacity));
        CollectPreconditions.checkNonnegative(valueSetCapacity, "expectedValuesPerKey");
        this.valueSetCapacity = valueSetCapacity;
        this.multimapHeaderEntry = new ValueEntry(null, null, 0, null);
        succeedsInMultimap(this.multimapHeaderEntry, this.multimapHeaderEntry);
    }

    /* Access modifiers changed, original: 0000 */
    public Set<V> createCollection() {
        return new LinkedHashSet(this.valueSetCapacity);
    }

    /* Access modifiers changed, original: 0000 */
    public Collection<V> createCollection(K key) {
        return new ValueSet(key, this.valueSetCapacity);
    }

    public Set<V> replaceValues(@Nullable K key, Iterable<? extends V> values) {
        return super.replaceValues((Object) key, (Iterable) values);
    }

    public Set<Entry<K, V>> entries() {
        return super.entries();
    }

    public Collection<V> values() {
        return super.values();
    }

    /* Access modifiers changed, original: 0000 */
    public Iterator<Entry<K, V>> entryIterator() {
        return new Iterator<Entry<K, V>>() {
            ValueEntry<K, V> nextEntry = LinkedHashMultimap.this.multimapHeaderEntry.successorInMultimap;
            ValueEntry<K, V> toRemove;

            public boolean hasNext() {
                return this.nextEntry != LinkedHashMultimap.this.multimapHeaderEntry;
            }

            public Entry<K, V> next() {
                if (hasNext()) {
                    ValueEntry<K, V> result = this.nextEntry;
                    this.toRemove = result;
                    this.nextEntry = this.nextEntry.successorInMultimap;
                    return result;
                }
                throw new NoSuchElementException();
            }

            public void remove() {
                CollectPreconditions.checkRemove(this.toRemove != null);
                LinkedHashMultimap.this.remove(this.toRemove.getKey(), this.toRemove.getValue());
                this.toRemove = null;
            }
        };
    }

    /* Access modifiers changed, original: 0000 */
    public Iterator<V> valueIterator() {
        return Maps.valueIterator(entryIterator());
    }

    public void clear() {
        super.clear();
        succeedsInMultimap(this.multimapHeaderEntry, this.multimapHeaderEntry);
    }

    @GwtIncompatible("java.io.ObjectOutputStream")
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeInt(this.valueSetCapacity);
        stream.writeInt(keySet().size());
        for (K key : keySet()) {
            stream.writeObject(key);
        }
        stream.writeInt(size());
        for (Entry<K, V> entry : entries()) {
            stream.writeObject(entry.getKey());
            stream.writeObject(entry.getValue());
        }
    }

    @GwtIncompatible("java.io.ObjectInputStream")
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        int i;
        K key;
        stream.defaultReadObject();
        int i2 = 0;
        this.multimapHeaderEntry = new ValueEntry(null, null, 0, null);
        succeedsInMultimap(this.multimapHeaderEntry, this.multimapHeaderEntry);
        this.valueSetCapacity = stream.readInt();
        int distinctKeys = stream.readInt();
        Map<K, Collection<V>> map = new LinkedHashMap(Maps.capacity(distinctKeys));
        for (i = 0; i < distinctKeys; i++) {
            key = stream.readObject();
            map.put(key, createCollection(key));
        }
        i = stream.readInt();
        while (i2 < i) {
            key = stream.readObject();
            ((Collection) map.get(key)).add(stream.readObject());
            i2++;
        }
        setMap(map);
    }
}
