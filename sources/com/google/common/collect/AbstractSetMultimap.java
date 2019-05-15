package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nullable;

@GwtCompatible
abstract class AbstractSetMultimap<K, V> extends AbstractMapBasedMultimap<K, V> implements SetMultimap<K, V> {
    private static final long serialVersionUID = 7431625294878419160L;

    public abstract Set<V> createCollection();

    protected AbstractSetMultimap(Map<K, Collection<V>> map) {
        super(map);
    }

    /* Access modifiers changed, original: 0000 */
    public Set<V> createUnmodifiableEmptyCollection() {
        return ImmutableSet.of();
    }

    public Set<V> get(@Nullable K key) {
        return (Set) super.get(key);
    }

    public Set<Entry<K, V>> entries() {
        return (Set) super.entries();
    }

    public Set<V> removeAll(@Nullable Object key) {
        return (Set) super.removeAll(key);
    }

    public Set<V> replaceValues(@Nullable K key, Iterable<? extends V> values) {
        return (Set) super.replaceValues(key, values);
    }

    public Map<K, Collection<V>> asMap() {
        return super.asMap();
    }

    public boolean put(@Nullable K key, @Nullable V value) {
        return super.put(key, value);
    }

    public boolean equals(@Nullable Object object) {
        return super.equals(object);
    }
}
