package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

@GwtCompatible(emulated = true)
public final class EnumHashBiMap<K extends Enum<K>, V> extends AbstractBiMap<K, V> {
    @GwtIncompatible("only needed in emulated source.")
    private static final long serialVersionUID = 0;
    private transient Class<K> keyType;

    public /* bridge */ /* synthetic */ void clear() {
        super.clear();
    }

    public /* bridge */ /* synthetic */ boolean containsValue(@Nullable Object obj) {
        return super.containsValue(obj);
    }

    public /* bridge */ /* synthetic */ Set entrySet() {
        return super.entrySet();
    }

    public /* bridge */ /* synthetic */ BiMap inverse() {
        return super.inverse();
    }

    public /* bridge */ /* synthetic */ Set keySet() {
        return super.keySet();
    }

    public /* bridge */ /* synthetic */ void putAll(Map map) {
        super.putAll(map);
    }

    public /* bridge */ /* synthetic */ Object remove(@Nullable Object obj) {
        return super.remove(obj);
    }

    public /* bridge */ /* synthetic */ Set values() {
        return super.values();
    }

    public static <K extends Enum<K>, V> EnumHashBiMap<K, V> create(Class<K> keyType) {
        return new EnumHashBiMap(keyType);
    }

    public static <K extends Enum<K>, V> EnumHashBiMap<K, V> create(Map<K, ? extends V> map) {
        EnumHashBiMap<K, V> bimap = create(EnumBiMap.inferKeyType(map));
        bimap.putAll(map);
        return bimap;
    }

    private EnumHashBiMap(Class<K> keyType) {
        super(WellBehavedMap.wrap(new EnumMap(keyType)), Maps.newHashMapWithExpectedSize(((Enum[]) keyType.getEnumConstants()).length));
        this.keyType = keyType;
    }

    /* Access modifiers changed, original: 0000 */
    public K checkKey(K key) {
        return (Enum) Preconditions.checkNotNull(key);
    }

    public V put(K key, @Nullable V value) {
        return super.put(key, value);
    }

    public V forcePut(K key, @Nullable V value) {
        return super.forcePut(key, value);
    }

    public Class<K> keyType() {
        return this.keyType;
    }

    @GwtIncompatible("java.io.ObjectOutputStream")
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeObject(this.keyType);
        Serialization.writeMap(this, stream);
    }

    @GwtIncompatible("java.io.ObjectInputStream")
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        this.keyType = (Class) stream.readObject();
        setDelegates(WellBehavedMap.wrap(new EnumMap(this.keyType)), new HashMap((((Enum[]) this.keyType.getEnumConstants()).length * 3) / 2));
        Serialization.populateMap(this, stream);
    }
}
