package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Predicate;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nullable;

@GwtCompatible
final class FilteredKeySetMultimap<K, V> extends FilteredKeyMultimap<K, V> implements FilteredSetMultimap<K, V> {

    class EntrySet extends Entries implements Set<Entry<K, V>> {
        EntrySet() {
            super();
        }

        public int hashCode() {
            return Sets.hashCodeImpl(this);
        }

        public boolean equals(@Nullable Object o) {
            return Sets.equalsImpl(this, o);
        }
    }

    FilteredKeySetMultimap(SetMultimap<K, V> unfiltered, Predicate<? super K> keyPredicate) {
        super(unfiltered, keyPredicate);
    }

    public SetMultimap<K, V> unfiltered() {
        return (SetMultimap) this.unfiltered;
    }

    public Set<V> get(K key) {
        return (Set) super.get(key);
    }

    public Set<V> removeAll(Object key) {
        return (Set) super.removeAll(key);
    }

    public Set<V> replaceValues(K key, Iterable<? extends V> values) {
        return (Set) super.replaceValues(key, values);
    }

    public Set<Entry<K, V>> entries() {
        return (Set) super.entries();
    }

    /* Access modifiers changed, original: 0000 */
    public Set<Entry<K, V>> createEntries() {
        return new EntrySet();
    }
}
