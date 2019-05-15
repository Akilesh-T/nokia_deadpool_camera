package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

@GwtCompatible(emulated = true, serializable = true)
public final class HashMultimap<K, V> extends AbstractSetMultimap<K, V> {
    private static final int DEFAULT_VALUES_PER_KEY = 2;
    @GwtIncompatible("Not needed in emulated source")
    private static final long serialVersionUID = 0;
    @VisibleForTesting
    transient int expectedValuesPerKey = 2;

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

    public /* bridge */ /* synthetic */ Set entries() {
        return super.entries();
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

    public /* bridge */ /* synthetic */ Set replaceValues(@Nullable Object obj, Iterable iterable) {
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

    public static <K, V> HashMultimap<K, V> create() {
        return new HashMultimap();
    }

    public static <K, V> HashMultimap<K, V> create(int expectedKeys, int expectedValuesPerKey) {
        return new HashMultimap(expectedKeys, expectedValuesPerKey);
    }

    public static <K, V> HashMultimap<K, V> create(Multimap<? extends K, ? extends V> multimap) {
        return new HashMultimap(multimap);
    }

    private HashMultimap() {
        super(new HashMap());
    }

    private HashMultimap(int expectedKeys, int expectedValuesPerKey) {
        super(Maps.newHashMapWithExpectedSize(expectedKeys));
        Preconditions.checkArgument(expectedValuesPerKey >= 0);
        this.expectedValuesPerKey = expectedValuesPerKey;
    }

    private HashMultimap(Multimap<? extends K, ? extends V> multimap) {
        super(Maps.newHashMapWithExpectedSize(multimap.keySet().size()));
        putAll(multimap);
    }

    /* Access modifiers changed, original: 0000 */
    public Set<V> createCollection() {
        return Sets.newHashSetWithExpectedSize(this.expectedValuesPerKey);
    }

    @GwtIncompatible("java.io.ObjectOutputStream")
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeInt(this.expectedValuesPerKey);
        Serialization.writeMultimap(this, stream);
    }

    @GwtIncompatible("java.io.ObjectInputStream")
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        this.expectedValuesPerKey = stream.readInt();
        int distinctKeys = Serialization.readCount(stream);
        setMap(Maps.newHashMapWithExpectedSize(distinctKeys));
        Serialization.populateMultimap(this, stream, distinctKeys);
    }
}
