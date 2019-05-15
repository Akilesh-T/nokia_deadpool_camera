package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nullable;

@GwtCompatible
class FilteredEntryMultimap<K, V> extends AbstractMultimap<K, V> implements FilteredMultimap<K, V> {
    final Predicate<? super Entry<K, V>> predicate;
    final Multimap<K, V> unfiltered;

    class AsMap extends ImprovedAbstractMap<K, Collection<V>> {
        AsMap() {
        }

        public boolean containsKey(@Nullable Object key) {
            return get(key) != null;
        }

        public void clear() {
            FilteredEntryMultimap.this.clear();
        }

        public Collection<V> get(@Nullable Object key) {
            Collection<V> result = (Collection) FilteredEntryMultimap.this.unfiltered.asMap().get(key);
            Collection<V> collection = null;
            if (result == null) {
                return null;
            }
            result = FilteredEntryMultimap.filterCollection(result, new ValuePredicate(key));
            if (!result.isEmpty()) {
                collection = result;
            }
            return collection;
        }

        public Collection<V> remove(@Nullable Object key) {
            Collection<V> collection = (Collection) FilteredEntryMultimap.this.unfiltered.asMap().get(key);
            if (collection == null) {
                return null;
            }
            K k = key;
            List<V> result = Lists.newArrayList();
            Iterator<V> itr = collection.iterator();
            while (itr.hasNext()) {
                V v = itr.next();
                if (FilteredEntryMultimap.this.satisfies(k, v)) {
                    itr.remove();
                    result.add(v);
                }
            }
            if (result.isEmpty()) {
                return null;
            }
            if (FilteredEntryMultimap.this.unfiltered instanceof SetMultimap) {
                return Collections.unmodifiableSet(Sets.newLinkedHashSet(result));
            }
            return Collections.unmodifiableList(result);
        }

        /* Access modifiers changed, original: 0000 */
        public Set<K> createKeySet() {
            return new KeySet<K, Collection<V>>(this) {
                public boolean removeAll(Collection<?> c) {
                    return FilteredEntryMultimap.this.removeEntriesIf(Maps.keyPredicateOnEntries(Predicates.in(c)));
                }

                public boolean retainAll(Collection<?> c) {
                    return FilteredEntryMultimap.this.removeEntriesIf(Maps.keyPredicateOnEntries(Predicates.not(Predicates.in(c))));
                }

                public boolean remove(@Nullable Object o) {
                    return AsMap.this.remove(o) != null;
                }
            };
        }

        /* Access modifiers changed, original: 0000 */
        public Set<Entry<K, Collection<V>>> createEntrySet() {
            return new EntrySet<K, Collection<V>>() {
                /* Access modifiers changed, original: 0000 */
                public Map<K, Collection<V>> map() {
                    return AsMap.this;
                }

                public Iterator<Entry<K, Collection<V>>> iterator() {
                    return new AbstractIterator<Entry<K, Collection<V>>>() {
                        final Iterator<Entry<K, Collection<V>>> backingIterator = FilteredEntryMultimap.this.unfiltered.asMap().entrySet().iterator();

                        /* Access modifiers changed, original: protected */
                        public Entry<K, Collection<V>> computeNext() {
                            while (this.backingIterator.hasNext()) {
                                Entry<K, Collection<V>> entry = (Entry) this.backingIterator.next();
                                K key = entry.getKey();
                                Collection<V> collection = FilteredEntryMultimap.filterCollection((Collection) entry.getValue(), new ValuePredicate(key));
                                if (!collection.isEmpty()) {
                                    return Maps.immutableEntry(key, collection);
                                }
                            }
                            return (Entry) endOfData();
                        }
                    };
                }

                public boolean removeAll(Collection<?> c) {
                    return FilteredEntryMultimap.this.removeEntriesIf(Predicates.in(c));
                }

                public boolean retainAll(Collection<?> c) {
                    return FilteredEntryMultimap.this.removeEntriesIf(Predicates.not(Predicates.in(c)));
                }

                public int size() {
                    return Iterators.size(iterator());
                }
            };
        }

        /* Access modifiers changed, original: 0000 */
        public Collection<Collection<V>> createValues() {
            return new Values<K, Collection<V>>(this) {
                public boolean remove(@Nullable Object o) {
                    if (o instanceof Collection) {
                        Collection<?> c = (Collection) o;
                        Iterator<Entry<K, Collection<V>>> entryIterator = FilteredEntryMultimap.this.unfiltered.asMap().entrySet().iterator();
                        while (entryIterator.hasNext()) {
                            Entry<K, Collection<V>> entry = (Entry) entryIterator.next();
                            Collection<V> collection = FilteredEntryMultimap.filterCollection((Collection) entry.getValue(), new ValuePredicate(entry.getKey()));
                            if (!collection.isEmpty() && c.equals(collection)) {
                                if (collection.size() == ((Collection) entry.getValue()).size()) {
                                    entryIterator.remove();
                                } else {
                                    collection.clear();
                                }
                                return true;
                            }
                        }
                    }
                    return false;
                }

                public boolean removeAll(Collection<?> c) {
                    return FilteredEntryMultimap.this.removeEntriesIf(Maps.valuePredicateOnEntries(Predicates.in(c)));
                }

                public boolean retainAll(Collection<?> c) {
                    return FilteredEntryMultimap.this.removeEntriesIf(Maps.valuePredicateOnEntries(Predicates.not(Predicates.in(c))));
                }
            };
        }
    }

    final class ValuePredicate implements Predicate<V> {
        private final K key;

        ValuePredicate(K key) {
            this.key = key;
        }

        public boolean apply(@Nullable V value) {
            return FilteredEntryMultimap.this.satisfies(this.key, value);
        }
    }

    class Keys extends Keys<K, V> {
        Keys() {
            super(FilteredEntryMultimap.this);
        }

        public int remove(@Nullable Object key, int occurrences) {
            CollectPreconditions.checkNonnegative(occurrences, "occurrences");
            if (occurrences == 0) {
                return count(key);
            }
            Collection<V> collection = (Collection) FilteredEntryMultimap.this.unfiltered.asMap().get(key);
            if (collection == null) {
                return 0;
            }
            K k = key;
            int oldCount = 0;
            Iterator<V> itr = collection.iterator();
            while (itr.hasNext()) {
                if (FilteredEntryMultimap.this.satisfies(k, itr.next())) {
                    oldCount++;
                    if (oldCount <= occurrences) {
                        itr.remove();
                    }
                }
            }
            return oldCount;
        }

        public Set<Multiset.Entry<K>> entrySet() {
            return new EntrySet<K>() {
                /* Access modifiers changed, original: 0000 */
                public Multiset<K> multiset() {
                    return Keys.this;
                }

                public Iterator<Multiset.Entry<K>> iterator() {
                    return Keys.this.entryIterator();
                }

                public int size() {
                    return FilteredEntryMultimap.this.keySet().size();
                }

                private boolean removeEntriesIf(final Predicate<? super Multiset.Entry<K>> predicate) {
                    return FilteredEntryMultimap.this.removeEntriesIf(new Predicate<Entry<K, Collection<V>>>() {
                        public boolean apply(Entry<K, Collection<V>> entry) {
                            return predicate.apply(Multisets.immutableEntry(entry.getKey(), ((Collection) entry.getValue()).size()));
                        }
                    });
                }

                public boolean removeAll(Collection<?> c) {
                    return removeEntriesIf(Predicates.in(c));
                }

                public boolean retainAll(Collection<?> c) {
                    return removeEntriesIf(Predicates.not(Predicates.in(c)));
                }
            };
        }
    }

    FilteredEntryMultimap(Multimap<K, V> unfiltered, Predicate<? super Entry<K, V>> predicate) {
        this.unfiltered = (Multimap) Preconditions.checkNotNull(unfiltered);
        this.predicate = (Predicate) Preconditions.checkNotNull(predicate);
    }

    public Multimap<K, V> unfiltered() {
        return this.unfiltered;
    }

    public Predicate<? super Entry<K, V>> entryPredicate() {
        return this.predicate;
    }

    public int size() {
        return entries().size();
    }

    private boolean satisfies(K key, V value) {
        return this.predicate.apply(Maps.immutableEntry(key, value));
    }

    static <E> Collection<E> filterCollection(Collection<E> collection, Predicate<? super E> predicate) {
        if (collection instanceof Set) {
            return Sets.filter((Set) collection, (Predicate) predicate);
        }
        return Collections2.filter(collection, predicate);
    }

    public boolean containsKey(@Nullable Object key) {
        return asMap().get(key) != null;
    }

    public Collection<V> removeAll(@Nullable Object key) {
        return (Collection) MoreObjects.firstNonNull(asMap().remove(key), unmodifiableEmptyCollection());
    }

    /* Access modifiers changed, original: 0000 */
    public Collection<V> unmodifiableEmptyCollection() {
        if (this.unfiltered instanceof SetMultimap) {
            return Collections.emptySet();
        }
        return Collections.emptyList();
    }

    public void clear() {
        entries().clear();
    }

    public Collection<V> get(K key) {
        return filterCollection(this.unfiltered.get(key), new ValuePredicate(key));
    }

    /* Access modifiers changed, original: 0000 */
    public Collection<Entry<K, V>> createEntries() {
        return filterCollection(this.unfiltered.entries(), this.predicate);
    }

    /* Access modifiers changed, original: 0000 */
    public Collection<V> createValues() {
        return new FilteredMultimapValues(this);
    }

    /* Access modifiers changed, original: 0000 */
    public Iterator<Entry<K, V>> entryIterator() {
        throw new AssertionError("should never be called");
    }

    /* Access modifiers changed, original: 0000 */
    public Map<K, Collection<V>> createAsMap() {
        return new AsMap();
    }

    public Set<K> keySet() {
        return asMap().keySet();
    }

    /* Access modifiers changed, original: 0000 */
    public boolean removeEntriesIf(Predicate<? super Entry<K, Collection<V>>> predicate) {
        Iterator<Entry<K, Collection<V>>> entryIterator = this.unfiltered.asMap().entrySet().iterator();
        boolean changed = false;
        while (entryIterator.hasNext()) {
            Entry<K, Collection<V>> entry = (Entry) entryIterator.next();
            K key = entry.getKey();
            Collection<V> collection = filterCollection((Collection) entry.getValue(), new ValuePredicate(key));
            if (!collection.isEmpty() && predicate.apply(Maps.immutableEntry(key, collection))) {
                if (collection.size() == ((Collection) entry.getValue()).size()) {
                    entryIterator.remove();
                } else {
                    collection.clear();
                }
                changed = true;
            }
        }
        return changed;
    }

    /* Access modifiers changed, original: 0000 */
    public Multiset<K> createKeys() {
        return new Keys();
    }
}
