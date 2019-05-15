package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Predicate;
import java.util.List;
import javax.annotation.Nullable;

@GwtCompatible
final class FilteredKeyListMultimap<K, V> extends FilteredKeyMultimap<K, V> implements ListMultimap<K, V> {
    FilteredKeyListMultimap(ListMultimap<K, V> unfiltered, Predicate<? super K> keyPredicate) {
        super(unfiltered, keyPredicate);
    }

    public ListMultimap<K, V> unfiltered() {
        return (ListMultimap) super.unfiltered();
    }

    public List<V> get(K key) {
        return (List) super.get(key);
    }

    public List<V> removeAll(@Nullable Object key) {
        return (List) super.removeAll(key);
    }

    public List<V> replaceValues(K key, Iterable<? extends V> values) {
        return (List) super.replaceValues(key, values);
    }
}
