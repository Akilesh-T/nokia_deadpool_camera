package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.annotation.Nullable;

@GwtIncompatible("NavigableMap")
@Beta
public final class TreeRangeMap<K extends Comparable, V> implements RangeMap<K, V> {
    private static final RangeMap EMPTY_SUB_RANGE_MAP = new RangeMap() {
        @Nullable
        public Object get(Comparable key) {
            return null;
        }

        @Nullable
        public Entry<Range, Object> getEntry(Comparable key) {
            return null;
        }

        public Range span() {
            throw new NoSuchElementException();
        }

        public void put(Range range, Object value) {
            Preconditions.checkNotNull(range);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot insert range ");
            stringBuilder.append(range);
            stringBuilder.append(" into an empty subRangeMap");
            throw new IllegalArgumentException(stringBuilder.toString());
        }

        public void putAll(RangeMap rangeMap) {
            if (!rangeMap.asMapOfRanges().isEmpty()) {
                throw new IllegalArgumentException("Cannot putAll(nonEmptyRangeMap) into an empty subRangeMap");
            }
        }

        public void clear() {
        }

        public void remove(Range range) {
            Preconditions.checkNotNull(range);
        }

        public Map<Range, Object> asMapOfRanges() {
            return Collections.emptyMap();
        }

        public RangeMap subRangeMap(Range range) {
            Preconditions.checkNotNull(range);
            return this;
        }
    };
    private final NavigableMap<Cut<K>, RangeMapEntry<K, V>> entriesByLowerBound = Maps.newTreeMap();

    private final class AsMapOfRanges extends AbstractMap<Range<K>, V> {
        private AsMapOfRanges() {
        }

        /* synthetic */ AsMapOfRanges(TreeRangeMap x0, AnonymousClass1 x1) {
            this();
        }

        public boolean containsKey(@Nullable Object key) {
            return get(key) != null;
        }

        public V get(@Nullable Object key) {
            if (key instanceof Range) {
                Range<?> range = (Range) key;
                RangeMapEntry<K, V> rangeMapEntry = (RangeMapEntry) TreeRangeMap.this.entriesByLowerBound.get(range.lowerBound);
                if (rangeMapEntry != null && rangeMapEntry.getKey().equals(range)) {
                    return rangeMapEntry.getValue();
                }
            }
            return null;
        }

        public Set<Entry<Range<K>, V>> entrySet() {
            return new AbstractSet<Entry<Range<K>, V>>() {
                public Iterator<Entry<Range<K>, V>> iterator() {
                    return TreeRangeMap.this.entriesByLowerBound.values().iterator();
                }

                public int size() {
                    return TreeRangeMap.this.entriesByLowerBound.size();
                }
            };
        }
    }

    private static final class RangeMapEntry<K extends Comparable, V> extends AbstractMapEntry<Range<K>, V> {
        private final Range<K> range;
        private final V value;

        RangeMapEntry(Cut<K> lowerBound, Cut<K> upperBound, V value) {
            this(Range.create(lowerBound, upperBound), value);
        }

        RangeMapEntry(Range<K> range, V value) {
            this.range = range;
            this.value = value;
        }

        public Range<K> getKey() {
            return this.range;
        }

        public V getValue() {
            return this.value;
        }

        public boolean contains(K value) {
            return this.range.contains(value);
        }

        /* Access modifiers changed, original: 0000 */
        public Cut<K> getLowerBound() {
            return this.range.lowerBound;
        }

        /* Access modifiers changed, original: 0000 */
        public Cut<K> getUpperBound() {
            return this.range.upperBound;
        }
    }

    private class SubRangeMap implements RangeMap<K, V> {
        private final Range<K> subRange;

        class SubRangeMapAsMap extends AbstractMap<Range<K>, V> {
            SubRangeMapAsMap() {
            }

            public boolean containsKey(Object key) {
                return get(key) != null;
            }

            public V get(Object key) {
                try {
                    if (key instanceof Range) {
                        Range<K> r = (Range) key;
                        if (SubRangeMap.this.subRange.encloses(r)) {
                            if (!r.isEmpty()) {
                                RangeMapEntry<K, V> candidate = null;
                                if (r.lowerBound.compareTo(SubRangeMap.this.subRange.lowerBound) == 0) {
                                    Entry<Cut<K>, RangeMapEntry<K, V>> entry = TreeRangeMap.this.entriesByLowerBound.floorEntry(r.lowerBound);
                                    if (entry != null) {
                                        candidate = (RangeMapEntry) entry.getValue();
                                    }
                                } else {
                                    candidate = (RangeMapEntry) TreeRangeMap.this.entriesByLowerBound.get(r.lowerBound);
                                }
                                if (candidate != null && candidate.getKey().isConnected(SubRangeMap.this.subRange) && candidate.getKey().intersection(SubRangeMap.this.subRange).equals(r)) {
                                    return candidate.getValue();
                                }
                            }
                        }
                        return null;
                    }
                    return null;
                } catch (ClassCastException e) {
                    return null;
                }
            }

            public V remove(Object key) {
                V value = get(key);
                if (value == null) {
                    return null;
                }
                TreeRangeMap.this.remove((Range) key);
                return value;
            }

            public void clear() {
                SubRangeMap.this.clear();
            }

            private boolean removeEntryIf(Predicate<? super Entry<Range<K>, V>> predicate) {
                List<Range<K>> toRemove = Lists.newArrayList();
                for (Entry<Range<K>, V> entry : entrySet()) {
                    if (predicate.apply(entry)) {
                        toRemove.add(entry.getKey());
                    }
                }
                for (Range<K> range : toRemove) {
                    TreeRangeMap.this.remove(range);
                }
                return toRemove.isEmpty() ^ 1;
            }

            public Set<Range<K>> keySet() {
                return new KeySet<Range<K>, V>(this) {
                    public boolean remove(@Nullable Object o) {
                        return SubRangeMapAsMap.this.remove(o) != null;
                    }

                    public boolean retainAll(Collection<?> c) {
                        return SubRangeMapAsMap.this.removeEntryIf(Predicates.compose(Predicates.not(Predicates.in(c)), Maps.keyFunction()));
                    }
                };
            }

            public Set<Entry<Range<K>, V>> entrySet() {
                return new EntrySet<Range<K>, V>() {
                    /* Access modifiers changed, original: 0000 */
                    public Map<Range<K>, V> map() {
                        return SubRangeMapAsMap.this;
                    }

                    public Iterator<Entry<Range<K>, V>> iterator() {
                        if (SubRangeMap.this.subRange.isEmpty()) {
                            return Iterators.emptyIterator();
                        }
                        final Iterator<RangeMapEntry<K, V>> backingItr = TreeRangeMap.this.entriesByLowerBound.tailMap((Cut) MoreObjects.firstNonNull(TreeRangeMap.this.entriesByLowerBound.floorKey(SubRangeMap.this.subRange.lowerBound), SubRangeMap.this.subRange.lowerBound), true).values().iterator();
                        return new AbstractIterator<Entry<Range<K>, V>>() {
                            /* Access modifiers changed, original: protected */
                            public Entry<Range<K>, V> computeNext() {
                                while (backingItr.hasNext()) {
                                    RangeMapEntry<K, V> entry = (RangeMapEntry) backingItr.next();
                                    if (entry.getLowerBound().compareTo(SubRangeMap.this.subRange.upperBound) >= 0) {
                                        break;
                                    } else if (entry.getUpperBound().compareTo(SubRangeMap.this.subRange.lowerBound) > 0) {
                                        return Maps.immutableEntry(entry.getKey().intersection(SubRangeMap.this.subRange), entry.getValue());
                                    }
                                }
                                return (Entry) endOfData();
                            }
                        };
                    }

                    public boolean retainAll(Collection<?> c) {
                        return SubRangeMapAsMap.this.removeEntryIf(Predicates.not(Predicates.in(c)));
                    }

                    public int size() {
                        return Iterators.size(iterator());
                    }

                    public boolean isEmpty() {
                        return iterator().hasNext() ^ 1;
                    }
                };
            }

            public Collection<V> values() {
                return new Values<Range<K>, V>(this) {
                    public boolean removeAll(Collection<?> c) {
                        return SubRangeMapAsMap.this.removeEntryIf(Predicates.compose(Predicates.in(c), Maps.valueFunction()));
                    }

                    public boolean retainAll(Collection<?> c) {
                        return SubRangeMapAsMap.this.removeEntryIf(Predicates.compose(Predicates.not(Predicates.in(c)), Maps.valueFunction()));
                    }
                };
            }
        }

        SubRangeMap(Range<K> subRange) {
            this.subRange = subRange;
        }

        @Nullable
        public V get(K key) {
            if (this.subRange.contains(key)) {
                return TreeRangeMap.this.get(key);
            }
            return null;
        }

        @Nullable
        public Entry<Range<K>, V> getEntry(K key) {
            if (this.subRange.contains(key)) {
                Entry<Range<K>, V> entry = TreeRangeMap.this.getEntry(key);
                if (entry != null) {
                    return Maps.immutableEntry(((Range) entry.getKey()).intersection(this.subRange), entry.getValue());
                }
            }
            return null;
        }

        public Range<K> span() {
            Cut<K> lowerBound;
            Entry<Cut<K>, RangeMapEntry<K, V>> lowerEntry = TreeRangeMap.this.entriesByLowerBound.floorEntry(this.subRange.lowerBound);
            if (lowerEntry == null || ((RangeMapEntry) lowerEntry.getValue()).getUpperBound().compareTo(this.subRange.lowerBound) <= 0) {
                lowerBound = (Cut) TreeRangeMap.this.entriesByLowerBound.ceilingKey(this.subRange.lowerBound);
                if (lowerBound == null || lowerBound.compareTo(this.subRange.upperBound) >= 0) {
                    throw new NoSuchElementException();
                }
            }
            lowerBound = this.subRange.lowerBound;
            Entry<Cut<K>, RangeMapEntry<K, V>> upperEntry = TreeRangeMap.this.entriesByLowerBound.lowerEntry(this.subRange.upperBound);
            if (upperEntry != null) {
                Cut<K> upperBound;
                if (((RangeMapEntry) upperEntry.getValue()).getUpperBound().compareTo(this.subRange.upperBound) >= 0) {
                    upperBound = this.subRange.upperBound;
                } else {
                    upperBound = ((RangeMapEntry) upperEntry.getValue()).getUpperBound();
                }
                return Range.create(lowerBound, upperBound);
            }
            throw new NoSuchElementException();
        }

        public void put(Range<K> range, V value) {
            Preconditions.checkArgument(this.subRange.encloses(range), "Cannot put range %s into a subRangeMap(%s)", range, this.subRange);
            TreeRangeMap.this.put(range, value);
        }

        public void putAll(RangeMap<K, V> rangeMap) {
            if (!rangeMap.asMapOfRanges().isEmpty()) {
                Object[] objArr = new Object[]{rangeMap.span(), this.subRange};
                Preconditions.checkArgument(this.subRange.encloses(rangeMap.span()), "Cannot putAll rangeMap with span %s into a subRangeMap(%s)", objArr);
                TreeRangeMap.this.putAll(rangeMap);
            }
        }

        public void clear() {
            TreeRangeMap.this.remove(this.subRange);
        }

        public void remove(Range<K> range) {
            if (range.isConnected(this.subRange)) {
                TreeRangeMap.this.remove(range.intersection(this.subRange));
            }
        }

        public RangeMap<K, V> subRangeMap(Range<K> range) {
            if (range.isConnected(this.subRange)) {
                return TreeRangeMap.this.subRangeMap(range.intersection(this.subRange));
            }
            return TreeRangeMap.this.emptySubRangeMap();
        }

        public Map<Range<K>, V> asMapOfRanges() {
            return new SubRangeMapAsMap();
        }

        public boolean equals(@Nullable Object o) {
            if (!(o instanceof RangeMap)) {
                return false;
            }
            return asMapOfRanges().equals(((RangeMap) o).asMapOfRanges());
        }

        public int hashCode() {
            return asMapOfRanges().hashCode();
        }

        public String toString() {
            return asMapOfRanges().toString();
        }
    }

    public static <K extends Comparable, V> TreeRangeMap<K, V> create() {
        return new TreeRangeMap();
    }

    private TreeRangeMap() {
    }

    @Nullable
    public V get(K key) {
        Entry<Range<K>, V> entry = getEntry(key);
        return entry == null ? null : entry.getValue();
    }

    @Nullable
    public Entry<Range<K>, V> getEntry(K key) {
        Entry<Cut<K>, RangeMapEntry<K, V>> mapEntry = this.entriesByLowerBound.floorEntry(Cut.belowValue(key));
        if (mapEntry == null || !((RangeMapEntry) mapEntry.getValue()).contains(key)) {
            return null;
        }
        return (Entry) mapEntry.getValue();
    }

    public void put(Range<K> range, V value) {
        if (!range.isEmpty()) {
            Preconditions.checkNotNull(value);
            remove(range);
            this.entriesByLowerBound.put(range.lowerBound, new RangeMapEntry(range, value));
        }
    }

    public void putAll(RangeMap<K, V> rangeMap) {
        for (Entry<Range<K>, V> entry : rangeMap.asMapOfRanges().entrySet()) {
            put((Range) entry.getKey(), entry.getValue());
        }
    }

    public void clear() {
        this.entriesByLowerBound.clear();
    }

    public Range<K> span() {
        Entry<Cut<K>, RangeMapEntry<K, V>> firstEntry = this.entriesByLowerBound.firstEntry();
        Entry<Cut<K>, RangeMapEntry<K, V>> lastEntry = this.entriesByLowerBound.lastEntry();
        if (firstEntry != null) {
            return Range.create(((RangeMapEntry) firstEntry.getValue()).getKey().lowerBound, ((RangeMapEntry) lastEntry.getValue()).getKey().upperBound);
        }
        throw new NoSuchElementException();
    }

    private void putRangeMapEntry(Cut<K> lowerBound, Cut<K> upperBound, V value) {
        this.entriesByLowerBound.put(lowerBound, new RangeMapEntry(lowerBound, upperBound, value));
    }

    public void remove(Range<K> rangeToRemove) {
        if (!rangeToRemove.isEmpty()) {
            Entry<Cut<K>, RangeMapEntry<K, V>> mapEntryBelowToTruncate = this.entriesByLowerBound.lowerEntry(rangeToRemove.lowerBound);
            if (mapEntryBelowToTruncate != null) {
                RangeMapEntry<K, V> rangeMapEntry = (RangeMapEntry) mapEntryBelowToTruncate.getValue();
                if (rangeMapEntry.getUpperBound().compareTo(rangeToRemove.lowerBound) > 0) {
                    if (rangeMapEntry.getUpperBound().compareTo(rangeToRemove.upperBound) > 0) {
                        putRangeMapEntry(rangeToRemove.upperBound, rangeMapEntry.getUpperBound(), ((RangeMapEntry) mapEntryBelowToTruncate.getValue()).getValue());
                    }
                    putRangeMapEntry(rangeMapEntry.getLowerBound(), rangeToRemove.lowerBound, ((RangeMapEntry) mapEntryBelowToTruncate.getValue()).getValue());
                }
            }
            Entry<Cut<K>, RangeMapEntry<K, V>> mapEntryAboveToTruncate = this.entriesByLowerBound.lowerEntry(rangeToRemove.upperBound);
            if (mapEntryAboveToTruncate != null) {
                RangeMapEntry<K, V> rangeMapEntry2 = (RangeMapEntry) mapEntryAboveToTruncate.getValue();
                if (rangeMapEntry2.getUpperBound().compareTo(rangeToRemove.upperBound) > 0) {
                    putRangeMapEntry(rangeToRemove.upperBound, rangeMapEntry2.getUpperBound(), ((RangeMapEntry) mapEntryAboveToTruncate.getValue()).getValue());
                    this.entriesByLowerBound.remove(rangeToRemove.lowerBound);
                }
            }
            this.entriesByLowerBound.subMap(rangeToRemove.lowerBound, rangeToRemove.upperBound).clear();
        }
    }

    public Map<Range<K>, V> asMapOfRanges() {
        return new AsMapOfRanges(this, null);
    }

    public RangeMap<K, V> subRangeMap(Range<K> subRange) {
        if (subRange.equals(Range.all())) {
            return this;
        }
        return new SubRangeMap(subRange);
    }

    private RangeMap<K, V> emptySubRangeMap() {
        return EMPTY_SUB_RANGE_MAP;
    }

    public boolean equals(@Nullable Object o) {
        if (!(o instanceof RangeMap)) {
            return false;
        }
        return asMapOfRanges().equals(((RangeMap) o).asMapOfRanges());
    }

    public int hashCode() {
        return asMapOfRanges().hashCode();
    }

    public String toString() {
        return this.entriesByLowerBound.values().toString();
    }
}
