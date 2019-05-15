package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nullable;

@GwtCompatible
class FilteredKeyMultimap<K, V> extends AbstractMultimap<K, V> implements FilteredMultimap<K, V> {
    final Predicate<? super K> keyPredicate;
    final Multimap<K, V> unfiltered;

    class Entries extends ForwardingCollection<Entry<K, V>> {
        Entries() {
        }

        /* Access modifiers changed, original: protected */
        public Collection<Entry<K, V>> delegate() {
            return Collections2.filter(FilteredKeyMultimap.this.unfiltered.entries(), FilteredKeyMultimap.this.entryPredicate());
        }

        public boolean remove(@Nullable Object o) {
            if (o instanceof Entry) {
                Entry<?, ?> entry = (Entry) o;
                if (FilteredKeyMultimap.this.unfiltered.containsKey(entry.getKey()) && FilteredKeyMultimap.this.keyPredicate.apply(entry.getKey())) {
                    return FilteredKeyMultimap.this.unfiltered.remove(entry.getKey(), entry.getValue());
                }
            }
            return false;
        }
    }

    static class AddRejectingList<K, V> extends ForwardingList<V> {
        final K key;

        AddRejectingList(K key) {
            this.key = key;
        }

        public boolean add(V v) {
            add(0, v);
            return true;
        }

        public boolean addAll(Collection<? extends V> collection) {
            addAll(0, collection);
            return true;
        }

        public void add(int index, V v) {
            Preconditions.checkPositionIndex(index, 0);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Key does not satisfy predicate: ");
            stringBuilder.append(this.key);
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        public boolean addAll(int index, Collection<? extends V> elements) {
            Preconditions.checkNotNull(elements);
            Preconditions.checkPositionIndex(index, 0);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Key does not satisfy predicate: ");
            stringBuilder.append(this.key);
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        /* Access modifiers changed, original: protected */
        public List<V> delegate() {
            return Collections.emptyList();
        }
    }

    static class AddRejectingSet<K, V> extends ForwardingSet<V> {
        final K key;

        AddRejectingSet(K key) {
            this.key = key;
        }

        public boolean add(V v) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Key does not satisfy predicate: ");
            stringBuilder.append(this.key);
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        public boolean addAll(Collection<? extends V> collection) {
            Preconditions.checkNotNull(collection);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Key does not satisfy predicate: ");
            stringBuilder.append(this.key);
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        /* Access modifiers changed, original: protected */
        public Set<V> delegate() {
            return Collections.emptySet();
        }
    }

    FilteredKeyMultimap(Multimap<K, V> unfiltered, Predicate<? super K> keyPredicate) {
        this.unfiltered = (Multimap) Preconditions.checkNotNull(unfiltered);
        this.keyPredicate = (Predicate) Preconditions.checkNotNull(keyPredicate);
    }

    public Multimap<K, V> unfiltered() {
        return this.unfiltered;
    }

    public Predicate<? super Entry<K, V>> entryPredicate() {
        return Maps.keyPredicateOnEntries(this.keyPredicate);
    }

    public int size() {
        int size = 0;
        for (Collection<V> collection : asMap().values()) {
            size += collection.size();
        }
        return size;
    }

    public boolean containsKey(@Nullable Object key) {
        if (!this.unfiltered.containsKey(key)) {
            return false;
        }
        return this.keyPredicate.apply(key);
    }

    public Collection<V> removeAll(Object key) {
        return containsKey(key) ? this.unfiltered.removeAll(key) : unmodifiableEmptyCollection();
    }

    /* Access modifiers changed, original: 0000 */
    public Collection<V> unmodifiableEmptyCollection() {
        if (this.unfiltered instanceof SetMultimap) {
            return ImmutableSet.of();
        }
        return ImmutableList.of();
    }

    public void clear() {
        keySet().clear();
    }

    /* Access modifiers changed, original: 0000 */
    public Set<K> createKeySet() {
        return Sets.filter(this.unfiltered.keySet(), this.keyPredicate);
    }

    public Collection<V> get(K key) {
        if (this.keyPredicate.apply(key)) {
            return this.unfiltered.get(key);
        }
        if (this.unfiltered instanceof SetMultimap) {
            return new AddRejectingSet(key);
        }
        return new AddRejectingList(key);
    }

    /* Access modifiers changed, original: 0000 */
    public Iterator<Entry<K, V>> entryIterator() {
        throw new AssertionError("should never be called");
    }

    /* Access modifiers changed, original: 0000 */
    public Collection<Entry<K, V>> createEntries() {
        return new Entries();
    }

    /* Access modifiers changed, original: 0000 */
    public Collection<V> createValues() {
        return new FilteredMultimapValues(this);
    }

    /* Access modifiers changed, original: 0000 */
    public Map<K, Collection<V>> createAsMap() {
        return Maps.filterKeys(this.unfiltered.asMap(), this.keyPredicate);
    }

    /* Access modifiers changed, original: 0000 */
    public Multiset<K> createKeys() {
        return Multisets.filter(this.unfiltered.keys(), this.keyPredicate);
    }
}
