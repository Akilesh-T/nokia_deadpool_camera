package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import java.io.Serializable;
import java.util.Map.Entry;
import javax.annotation.Nullable;

@GwtCompatible(emulated = true, serializable = true)
class RegularImmutableBiMap<K, V> extends ImmutableBiMap<K, V> {
    static final double MAX_LOAD_FACTOR = 1.2d;
    private final transient ImmutableMapEntry<K, V>[] entries;
    private final transient int hashCode;
    private transient ImmutableBiMap<V, K> inverse;
    private final transient ImmutableMapEntry<K, V>[] keyTable;
    private final transient int mask;
    private final transient ImmutableMapEntry<K, V>[] valueTable;

    private static class InverseSerializedForm<K, V> implements Serializable {
        private static final long serialVersionUID = 1;
        private final ImmutableBiMap<K, V> forward;

        InverseSerializedForm(ImmutableBiMap<K, V> forward) {
            this.forward = forward;
        }

        /* Access modifiers changed, original: 0000 */
        public Object readResolve() {
            return this.forward.inverse();
        }
    }

    private final class Inverse extends ImmutableBiMap<V, K> {

        final class InverseEntrySet extends ImmutableMapEntrySet<V, K> {
            InverseEntrySet() {
            }

            /* Access modifiers changed, original: 0000 */
            public ImmutableMap<V, K> map() {
                return Inverse.this;
            }

            /* Access modifiers changed, original: 0000 */
            public boolean isHashCodeFast() {
                return true;
            }

            public int hashCode() {
                return RegularImmutableBiMap.this.hashCode;
            }

            public UnmodifiableIterator<Entry<V, K>> iterator() {
                return asList().iterator();
            }

            /* Access modifiers changed, original: 0000 */
            public ImmutableList<Entry<V, K>> createAsList() {
                return new ImmutableAsList<Entry<V, K>>() {
                    public Entry<V, K> get(int index) {
                        Entry<K, V> entry = RegularImmutableBiMap.this.entries[index];
                        return Maps.immutableEntry(entry.getValue(), entry.getKey());
                    }

                    /* Access modifiers changed, original: 0000 */
                    public ImmutableCollection<Entry<V, K>> delegateCollection() {
                        return InverseEntrySet.this;
                    }
                };
            }
        }

        private Inverse() {
        }

        /* synthetic */ Inverse(RegularImmutableBiMap x0, AnonymousClass1 x1) {
            this();
        }

        public int size() {
            return inverse().size();
        }

        public ImmutableBiMap<K, V> inverse() {
            return RegularImmutableBiMap.this;
        }

        public K get(@Nullable Object value) {
            if (value == null) {
                return null;
            }
            for (ImmutableMapEntry<K, V> entry = RegularImmutableBiMap.this.valueTable[Hashing.smear(value.hashCode()) & RegularImmutableBiMap.this.mask]; entry != null; entry = entry.getNextInValueBucket()) {
                if (value.equals(entry.getValue())) {
                    return entry.getKey();
                }
            }
            return null;
        }

        /* Access modifiers changed, original: 0000 */
        public ImmutableSet<Entry<V, K>> createEntrySet() {
            return new InverseEntrySet();
        }

        /* Access modifiers changed, original: 0000 */
        public boolean isPartialView() {
            return false;
        }

        /* Access modifiers changed, original: 0000 */
        public Object writeReplace() {
            return new InverseSerializedForm(RegularImmutableBiMap.this);
        }
    }

    private static final class NonTerminalBiMapEntry<K, V> extends ImmutableMapEntry<K, V> {
        @Nullable
        private final ImmutableMapEntry<K, V> nextInKeyBucket;
        @Nullable
        private final ImmutableMapEntry<K, V> nextInValueBucket;

        NonTerminalBiMapEntry(K key, V value, @Nullable ImmutableMapEntry<K, V> nextInKeyBucket, @Nullable ImmutableMapEntry<K, V> nextInValueBucket) {
            super(key, value);
            this.nextInKeyBucket = nextInKeyBucket;
            this.nextInValueBucket = nextInValueBucket;
        }

        NonTerminalBiMapEntry(ImmutableMapEntry<K, V> contents, @Nullable ImmutableMapEntry<K, V> nextInKeyBucket, @Nullable ImmutableMapEntry<K, V> nextInValueBucket) {
            super(contents);
            this.nextInKeyBucket = nextInKeyBucket;
            this.nextInValueBucket = nextInValueBucket;
        }

        /* Access modifiers changed, original: 0000 */
        @Nullable
        public ImmutableMapEntry<K, V> getNextInKeyBucket() {
            return this.nextInKeyBucket;
        }

        /* Access modifiers changed, original: 0000 */
        @Nullable
        public ImmutableMapEntry<K, V> getNextInValueBucket() {
            return this.nextInValueBucket;
        }
    }

    RegularImmutableBiMap(TerminalEntry<?, ?>... entriesToAdd) {
        this(entriesToAdd.length, entriesToAdd);
    }

    RegularImmutableBiMap(int n, TerminalEntry<?, ?>[] entriesToAdd) {
        int i = n;
        int tableSize = Hashing.closedTableSize(i, 858993459);
        this.mask = tableSize - 1;
        ImmutableMapEntry<K, V>[] keyTable = createEntryArray(tableSize);
        ImmutableMapEntry<K, V>[] valueTable = createEntryArray(tableSize);
        ImmutableMapEntry<K, V>[] entries = createEntryArray(n);
        int hashCode = 0;
        int i2 = 0;
        while (i2 < i) {
            int tableSize2;
            ImmutableMapEntry<K, V> entry = entriesToAdd[i2];
            K key = entry.getKey();
            V value = entry.getValue();
            int keyHash = key.hashCode();
            int valueHash = value.hashCode();
            int keyBucket = Hashing.smear(keyHash) & this.mask;
            i = this.mask & Hashing.smear(valueHash);
            ImmutableMapEntry<K, V> nextInKeyBucket = keyTable[keyBucket];
            ImmutableMapEntry<K, V> keyEntry = nextInKeyBucket;
            while (true) {
                tableSize2 = tableSize;
                tableSize = keyEntry;
                if (tableSize == 0) {
                    break;
                }
                K key2 = key;
                ImmutableMap.checkNoConflict(key.equals(tableSize.getKey()) ^ 1, "key", entry, tableSize);
                keyEntry = tableSize.getNextInKeyBucket();
                tableSize = tableSize2;
                key = key2;
            }
            tableSize = valueTable[i];
            ImmutableMapEntry<K, V> valueEntry = tableSize;
            while (valueEntry != null) {
                V value2 = value;
                ImmutableMap.checkNoConflict(value.equals(valueEntry.getValue()) ^ 1, "value", entry, valueEntry);
                valueEntry = valueEntry.getNextInValueBucket();
                value = value2;
            }
            valueEntry = (nextInKeyBucket == null && tableSize == 0) ? entry : new NonTerminalBiMapEntry(entry, nextInKeyBucket, tableSize);
            keyTable[keyBucket] = valueEntry;
            valueTable[i] = valueEntry;
            entries[i2] = valueEntry;
            hashCode += keyHash ^ valueHash;
            i2++;
            tableSize = tableSize2;
            i = n;
        }
        this.keyTable = keyTable;
        this.valueTable = valueTable;
        this.entries = entries;
        this.hashCode = hashCode;
    }

    RegularImmutableBiMap(Entry<?, ?>[] entriesToAdd) {
        int tableSize;
        int hashCode;
        RegularImmutableBiMap regularImmutableBiMap = this;
        Entry<?, ?>[] entryArr = entriesToAdd;
        int n = entryArr.length;
        int tableSize2 = Hashing.closedTableSize(n, 858993459);
        regularImmutableBiMap.mask = tableSize2 - 1;
        ImmutableMapEntry<K, V>[] keyTable = createEntryArray(tableSize2);
        ImmutableMapEntry<K, V>[] valueTable = createEntryArray(tableSize2);
        ImmutableMapEntry<K, V>[] entries = createEntryArray(n);
        int hashCode2 = 0;
        int i = 0;
        while (i < n) {
            int n2;
            Entry<K, V> entry = entryArr[i];
            K key = entry.getKey();
            V value = entry.getValue();
            CollectPreconditions.checkEntryNotNull(key, value);
            int keyHash = key.hashCode();
            int valueHash = value.hashCode();
            int keyBucket = Hashing.smear(keyHash) & regularImmutableBiMap.mask;
            int valueBucket = regularImmutableBiMap.mask & Hashing.smear(valueHash);
            ImmutableMapEntry<K, V> nextInKeyBucket = keyTable[keyBucket];
            ImmutableMapEntry<K, V> keyEntry = nextInKeyBucket;
            while (true) {
                n2 = n;
                n = keyEntry;
                if (n == 0) {
                    break;
                }
                tableSize = tableSize2;
                ImmutableMap.checkNoConflict(key.equals(n.getKey()) ^ 1, "key", entry, n);
                keyEntry = n.getNextInKeyBucket();
                n = n2;
                tableSize2 = tableSize;
            }
            tableSize = tableSize2;
            ImmutableMapEntry<K, V> nextInValueBucket = valueTable[valueBucket];
            n = nextInValueBucket;
            while (n != 0) {
                hashCode = hashCode2;
                ImmutableMap.checkNoConflict(value.equals(n.getValue()) ^ 1, "value", entry, n);
                n = n.getNextInValueBucket();
                hashCode2 = hashCode;
            }
            hashCode = hashCode2;
            if (nextInKeyBucket == null && nextInValueBucket == null) {
                n = new TerminalEntry(key, value);
            } else {
                n = new NonTerminalBiMapEntry(key, value, nextInKeyBucket, nextInValueBucket);
            }
            keyTable[keyBucket] = n;
            valueTable[valueBucket] = n;
            entries[i] = n;
            hashCode2 = hashCode + (keyHash ^ valueHash);
            i++;
            n = n2;
            tableSize2 = tableSize;
            regularImmutableBiMap = this;
            entryArr = entriesToAdd;
        }
        tableSize = tableSize2;
        hashCode = hashCode2;
        this.keyTable = keyTable;
        this.valueTable = valueTable;
        this.entries = entries;
        this.hashCode = hashCode2;
    }

    private static <K, V> ImmutableMapEntry<K, V>[] createEntryArray(int length) {
        return new ImmutableMapEntry[length];
    }

    @Nullable
    public V get(@Nullable Object key) {
        if (key == null) {
            return null;
        }
        for (ImmutableMapEntry<K, V> entry = this.keyTable[Hashing.smear(key.hashCode()) & this.mask]; entry != null; entry = entry.getNextInKeyBucket()) {
            if (key.equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    /* Access modifiers changed, original: 0000 */
    public ImmutableSet<Entry<K, V>> createEntrySet() {
        return new ImmutableMapEntrySet<K, V>() {
            /* Access modifiers changed, original: 0000 */
            public ImmutableMap<K, V> map() {
                return RegularImmutableBiMap.this;
            }

            public UnmodifiableIterator<Entry<K, V>> iterator() {
                return asList().iterator();
            }

            /* Access modifiers changed, original: 0000 */
            public ImmutableList<Entry<K, V>> createAsList() {
                return new RegularImmutableAsList((ImmutableCollection) this, RegularImmutableBiMap.this.entries);
            }

            /* Access modifiers changed, original: 0000 */
            public boolean isHashCodeFast() {
                return true;
            }

            public int hashCode() {
                return RegularImmutableBiMap.this.hashCode;
            }
        };
    }

    /* Access modifiers changed, original: 0000 */
    public boolean isPartialView() {
        return false;
    }

    public int size() {
        return this.entries.length;
    }

    public ImmutableBiMap<V, K> inverse() {
        ImmutableBiMap<V, K> result = this.inverse;
        if (result != null) {
            return result;
        }
        ImmutableBiMap<V, K> inverse = new Inverse(this, null);
        this.inverse = inverse;
        return inverse;
    }
}
