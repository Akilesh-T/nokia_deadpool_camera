package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;
import javax.annotation.Nullable;

@GwtCompatible
abstract class AbstractSortedSetMultimap<K, V> extends AbstractSetMultimap<K, V> implements SortedSetMultimap<K, V> {
    private static final long serialVersionUID = 430848587173315748L;

    public abstract SortedSet<V> createCollection();

    protected AbstractSortedSetMultimap(Map<K, Collection<V>> map) {
        super(map);
    }

    /* Access modifiers changed, original: 0000 */
    public SortedSet<V> createUnmodifiableEmptyCollection() {
        if (valueComparator() == null) {
            return Collections.unmodifiableSortedSet(createCollection());
        }
        return ImmutableSortedSet.emptySet(valueComparator());
    }

    public SortedSet<V> get(@Nullable K key) {
        return (SortedSet) super.get((Object) key);
    }

    public SortedSet<V> removeAll(@Nullable Object key) {
        return (SortedSet) super.removeAll(key);
    }

    public SortedSet<V> replaceValues(@Nullable K key, Iterable<? extends V> values) {
        return (SortedSet) super.replaceValues((Object) key, (Iterable) values);
    }

    public Map<K, Collection<V>> asMap() {
        return super.asMap();
    }

    public Collection<V> values() {
        return super.values();
    }
}
