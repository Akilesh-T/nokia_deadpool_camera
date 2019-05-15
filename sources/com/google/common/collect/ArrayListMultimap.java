package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

@GwtCompatible(emulated = true, serializable = true)
public final class ArrayListMultimap<K, V> extends AbstractListMultimap<K, V> {
    private static final int DEFAULT_VALUES_PER_KEY = 3;
    @GwtIncompatible("Not needed in emulated source.")
    private static final long serialVersionUID = 0;
    @VisibleForTesting
    transient int expectedValuesPerKey;

    public /* bridge */ /* synthetic */ Map asMap() {
        return super.asMap();
    }

    public /* bridge */ /* synthetic */ void clear() {
        super.clear();
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

    public /* bridge */ /* synthetic */ Collection entries() {
        return super.entries();
    }

    public /* bridge */ /* synthetic */ boolean equals(@Nullable Object obj) {
        return super.equals(obj);
    }

    public /* bridge */ /* synthetic */ List get(@Nullable Object obj) {
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

    public /* bridge */ /* synthetic */ List removeAll(@Nullable Object obj) {
        return super.removeAll(obj);
    }

    public /* bridge */ /* synthetic */ List replaceValues(@Nullable Object obj, Iterable iterable) {
        return super.replaceValues(obj, iterable);
    }

    public /* bridge */ /* synthetic */ int size() {
        return super.size();
    }

    public /* bridge */ /* synthetic */ String toString() {
        return super.toString();
    }

    public /* bridge */ /* synthetic */ Collection values() {
        return super.values();
    }

    public static <K, V> ArrayListMultimap<K, V> create() {
        return new ArrayListMultimap();
    }

    public static <K, V> ArrayListMultimap<K, V> create(int expectedKeys, int expectedValuesPerKey) {
        return new ArrayListMultimap(expectedKeys, expectedValuesPerKey);
    }

    public static <K, V> ArrayListMultimap<K, V> create(Multimap<? extends K, ? extends V> multimap) {
        return new ArrayListMultimap(multimap);
    }

    private ArrayListMultimap() {
        super(new HashMap());
        this.expectedValuesPerKey = 3;
    }

    private ArrayListMultimap(int expectedKeys, int expectedValuesPerKey) {
        super(Maps.newHashMapWithExpectedSize(expectedKeys));
        CollectPreconditions.checkNonnegative(expectedValuesPerKey, "expectedValuesPerKey");
        this.expectedValuesPerKey = expectedValuesPerKey;
    }

    private ArrayListMultimap(Multimap<? extends K, ? extends V> multimap) {
        int i;
        int size = multimap.keySet().size();
        if (multimap instanceof ArrayListMultimap) {
            i = ((ArrayListMultimap) multimap).expectedValuesPerKey;
        } else {
            i = 3;
        }
        this(size, i);
        putAll(multimap);
    }

    /* Access modifiers changed, original: 0000 */
    public List<V> createCollection() {
        return new ArrayList(this.expectedValuesPerKey);
    }

    public void trimToSize() {
        for (Collection<V> collection : backingMap().values()) {
            ((ArrayList) collection).trimToSize();
        }
    }

    @GwtIncompatible("java.io.ObjectOutputStream")
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeInt(this.expectedValuesPerKey);
        Serialization.writeMultimap(this, stream);
    }

    @GwtIncompatible("java.io.ObjectOutputStream")
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        this.expectedValuesPerKey = stream.readInt();
        int distinctKeys = Serialization.readCount(stream);
        setMap(Maps.newHashMapWithExpectedSize(distinctKeys));
        Serialization.populateMultimap(this, stream, distinctKeys);
    }
}
