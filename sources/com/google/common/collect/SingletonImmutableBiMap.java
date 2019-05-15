package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import java.util.Map.Entry;
import javax.annotation.Nullable;

@GwtCompatible(emulated = true, serializable = true)
final class SingletonImmutableBiMap<K, V> extends ImmutableBiMap<K, V> {
    transient ImmutableBiMap<V, K> inverse;
    final transient K singleKey;
    final transient V singleValue;

    SingletonImmutableBiMap(K singleKey, V singleValue) {
        CollectPreconditions.checkEntryNotNull(singleKey, singleValue);
        this.singleKey = singleKey;
        this.singleValue = singleValue;
    }

    private SingletonImmutableBiMap(K singleKey, V singleValue, ImmutableBiMap<V, K> inverse) {
        this.singleKey = singleKey;
        this.singleValue = singleValue;
        this.inverse = inverse;
    }

    SingletonImmutableBiMap(Entry<? extends K, ? extends V> entry) {
        this(entry.getKey(), entry.getValue());
    }

    public V get(@Nullable Object key) {
        return this.singleKey.equals(key) ? this.singleValue : null;
    }

    public int size() {
        return 1;
    }

    public boolean containsKey(@Nullable Object key) {
        return this.singleKey.equals(key);
    }

    public boolean containsValue(@Nullable Object value) {
        return this.singleValue.equals(value);
    }

    /* Access modifiers changed, original: 0000 */
    public boolean isPartialView() {
        return false;
    }

    /* Access modifiers changed, original: 0000 */
    public ImmutableSet<Entry<K, V>> createEntrySet() {
        return ImmutableSet.of(Maps.immutableEntry(this.singleKey, this.singleValue));
    }

    /* Access modifiers changed, original: 0000 */
    public ImmutableSet<K> createKeySet() {
        return ImmutableSet.of(this.singleKey);
    }

    public ImmutableBiMap<V, K> inverse() {
        ImmutableBiMap<V, K> result = this.inverse;
        if (result != null) {
            return result;
        }
        SingletonImmutableBiMap singletonImmutableBiMap = new SingletonImmutableBiMap(this.singleValue, this.singleKey, this);
        this.inverse = singletonImmutableBiMap;
        return singletonImmutableBiMap;
    }
}
