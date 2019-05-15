package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import javax.annotation.Nullable;

@GwtIncompatible("uses NavigableMap")
@Beta
public class TreeRangeSet<C extends Comparable<?>> extends AbstractRangeSet<C> {
    private transient Set<Range<C>> asRanges;
    private transient RangeSet<C> complement;
    @VisibleForTesting
    final NavigableMap<Cut<C>, Range<C>> rangesByLowerBound;

    private static final class ComplementRangesByLowerBound<C extends Comparable<?>> extends AbstractNavigableMap<Cut<C>, Range<C>> {
        private final Range<Cut<C>> complementLowerBoundWindow;
        private final NavigableMap<Cut<C>, Range<C>> positiveRangesByLowerBound;
        private final NavigableMap<Cut<C>, Range<C>> positiveRangesByUpperBound;

        ComplementRangesByLowerBound(NavigableMap<Cut<C>, Range<C>> positiveRangesByLowerBound) {
            this(positiveRangesByLowerBound, Range.all());
        }

        private ComplementRangesByLowerBound(NavigableMap<Cut<C>, Range<C>> positiveRangesByLowerBound, Range<Cut<C>> window) {
            this.positiveRangesByLowerBound = positiveRangesByLowerBound;
            this.positiveRangesByUpperBound = new RangesByUpperBound(positiveRangesByLowerBound);
            this.complementLowerBoundWindow = window;
        }

        private NavigableMap<Cut<C>, Range<C>> subMap(Range<Cut<C>> subWindow) {
            if (!this.complementLowerBoundWindow.isConnected(subWindow)) {
                return ImmutableSortedMap.of();
            }
            return new ComplementRangesByLowerBound(this.positiveRangesByLowerBound, subWindow.intersection(this.complementLowerBoundWindow));
        }

        public NavigableMap<Cut<C>, Range<C>> subMap(Cut<C> fromKey, boolean fromInclusive, Cut<C> toKey, boolean toInclusive) {
            return subMap(Range.range(fromKey, BoundType.forBoolean(fromInclusive), toKey, BoundType.forBoolean(toInclusive)));
        }

        public NavigableMap<Cut<C>, Range<C>> headMap(Cut<C> toKey, boolean inclusive) {
            return subMap(Range.upTo(toKey, BoundType.forBoolean(inclusive)));
        }

        public NavigableMap<Cut<C>, Range<C>> tailMap(Cut<C> fromKey, boolean inclusive) {
            return subMap(Range.downTo(fromKey, BoundType.forBoolean(inclusive)));
        }

        public Comparator<? super Cut<C>> comparator() {
            return Ordering.natural();
        }

        /* Access modifiers changed, original: 0000 */
        public Iterator<Entry<Cut<C>, Range<C>>> entryIterator() {
            Collection<Range<C>> positiveRanges;
            Cut<C> firstComplementRangeLowerBound;
            if (this.complementLowerBoundWindow.hasLowerBound()) {
                positiveRanges = this.positiveRangesByUpperBound.tailMap(this.complementLowerBoundWindow.lowerEndpoint(), this.complementLowerBoundWindow.lowerBoundType() == BoundType.CLOSED).values();
            } else {
                positiveRanges = this.positiveRangesByUpperBound.values();
            }
            final PeekingIterator<Range<C>> positiveItr = Iterators.peekingIterator(positiveRanges.iterator());
            if (this.complementLowerBoundWindow.contains(Cut.belowAll()) && (!positiveItr.hasNext() || ((Range) positiveItr.peek()).lowerBound != Cut.belowAll())) {
                firstComplementRangeLowerBound = Cut.belowAll();
            } else if (!positiveItr.hasNext()) {
                return Iterators.emptyIterator();
            } else {
                firstComplementRangeLowerBound = ((Range) positiveItr.next()).upperBound;
            }
            return new AbstractIterator<Entry<Cut<C>, Range<C>>>() {
                Cut<C> nextComplementRangeLowerBound = firstComplementRangeLowerBound;

                /* Access modifiers changed, original: protected */
                public Entry<Cut<C>, Range<C>> computeNext() {
                    if (ComplementRangesByLowerBound.this.complementLowerBoundWindow.upperBound.isLessThan(this.nextComplementRangeLowerBound) || this.nextComplementRangeLowerBound == Cut.aboveAll()) {
                        return (Entry) endOfData();
                    }
                    Range<C> negativeRange;
                    if (positiveItr.hasNext()) {
                        Range<C> positiveRange = (Range) positiveItr.next();
                        negativeRange = Range.create(this.nextComplementRangeLowerBound, positiveRange.lowerBound);
                        this.nextComplementRangeLowerBound = positiveRange.upperBound;
                    } else {
                        negativeRange = Range.create(this.nextComplementRangeLowerBound, Cut.aboveAll());
                        this.nextComplementRangeLowerBound = Cut.aboveAll();
                    }
                    return Maps.immutableEntry(negativeRange.lowerBound, negativeRange);
                }
            };
        }

        /* Access modifiers changed, original: 0000 */
        public Iterator<Entry<Cut<C>, Range<C>>> descendingEntryIterator() {
            Cut<C> startingPoint;
            Cut<C> cut;
            if (this.complementLowerBoundWindow.hasUpperBound()) {
                startingPoint = (Cut) this.complementLowerBoundWindow.upperEndpoint();
            } else {
                startingPoint = Cut.aboveAll();
            }
            boolean inclusive = this.complementLowerBoundWindow.hasUpperBound() && this.complementLowerBoundWindow.upperBoundType() == BoundType.CLOSED;
            final PeekingIterator<Range<C>> positiveItr = Iterators.peekingIterator(this.positiveRangesByUpperBound.headMap(startingPoint, inclusive).descendingMap().values().iterator());
            if (positiveItr.hasNext()) {
                if (((Range) positiveItr.peek()).upperBound == Cut.aboveAll()) {
                    cut = ((Range) positiveItr.next()).lowerBound;
                } else {
                    cut = (Cut) this.positiveRangesByLowerBound.higherKey(((Range) positiveItr.peek()).upperBound);
                }
            } else if (!this.complementLowerBoundWindow.contains(Cut.belowAll()) || this.positiveRangesByLowerBound.containsKey(Cut.belowAll())) {
                return Iterators.emptyIterator();
            } else {
                cut = (Cut) this.positiveRangesByLowerBound.higherKey(Cut.belowAll());
            }
            final Cut<C> firstComplementRangeUpperBound = (Cut) MoreObjects.firstNonNull(cut, Cut.aboveAll());
            return new AbstractIterator<Entry<Cut<C>, Range<C>>>() {
                Cut<C> nextComplementRangeUpperBound = firstComplementRangeUpperBound;

                /* Access modifiers changed, original: protected */
                public Entry<Cut<C>, Range<C>> computeNext() {
                    if (this.nextComplementRangeUpperBound == Cut.belowAll()) {
                        return (Entry) endOfData();
                    }
                    Range<C> positiveRange;
                    if (positiveItr.hasNext()) {
                        positiveRange = (Range) positiveItr.next();
                        Range<C> negativeRange = Range.create(positiveRange.upperBound, this.nextComplementRangeUpperBound);
                        this.nextComplementRangeUpperBound = positiveRange.lowerBound;
                        if (ComplementRangesByLowerBound.this.complementLowerBoundWindow.lowerBound.isLessThan(negativeRange.lowerBound)) {
                            return Maps.immutableEntry(negativeRange.lowerBound, negativeRange);
                        }
                    } else if (ComplementRangesByLowerBound.this.complementLowerBoundWindow.lowerBound.isLessThan(Cut.belowAll())) {
                        positiveRange = Range.create(Cut.belowAll(), this.nextComplementRangeUpperBound);
                        this.nextComplementRangeUpperBound = Cut.belowAll();
                        return Maps.immutableEntry(Cut.belowAll(), positiveRange);
                    }
                    return (Entry) endOfData();
                }
            };
        }

        public int size() {
            return Iterators.size(entryIterator());
        }

        @Nullable
        public Range<C> get(Object key) {
            if (key instanceof Cut) {
                try {
                    Cut cut = (Cut) key;
                    Entry<Cut<C>, Range<C>> firstEntry = tailMap(cut, true).firstEntry();
                    if (firstEntry != null && ((Cut) firstEntry.getKey()).equals(cut)) {
                        return (Range) firstEntry.getValue();
                    }
                } catch (ClassCastException e) {
                    return null;
                }
            }
            return null;
        }

        public boolean containsKey(Object key) {
            return get(key) != null;
        }
    }

    @VisibleForTesting
    static final class RangesByUpperBound<C extends Comparable<?>> extends AbstractNavigableMap<Cut<C>, Range<C>> {
        private final NavigableMap<Cut<C>, Range<C>> rangesByLowerBound;
        private final Range<Cut<C>> upperBoundWindow;

        RangesByUpperBound(NavigableMap<Cut<C>, Range<C>> rangesByLowerBound) {
            this.rangesByLowerBound = rangesByLowerBound;
            this.upperBoundWindow = Range.all();
        }

        private RangesByUpperBound(NavigableMap<Cut<C>, Range<C>> rangesByLowerBound, Range<Cut<C>> upperBoundWindow) {
            this.rangesByLowerBound = rangesByLowerBound;
            this.upperBoundWindow = upperBoundWindow;
        }

        private NavigableMap<Cut<C>, Range<C>> subMap(Range<Cut<C>> window) {
            if (window.isConnected(this.upperBoundWindow)) {
                return new RangesByUpperBound(this.rangesByLowerBound, window.intersection(this.upperBoundWindow));
            }
            return ImmutableSortedMap.of();
        }

        public NavigableMap<Cut<C>, Range<C>> subMap(Cut<C> fromKey, boolean fromInclusive, Cut<C> toKey, boolean toInclusive) {
            return subMap(Range.range(fromKey, BoundType.forBoolean(fromInclusive), toKey, BoundType.forBoolean(toInclusive)));
        }

        public NavigableMap<Cut<C>, Range<C>> headMap(Cut<C> toKey, boolean inclusive) {
            return subMap(Range.upTo(toKey, BoundType.forBoolean(inclusive)));
        }

        public NavigableMap<Cut<C>, Range<C>> tailMap(Cut<C> fromKey, boolean inclusive) {
            return subMap(Range.downTo(fromKey, BoundType.forBoolean(inclusive)));
        }

        public Comparator<? super Cut<C>> comparator() {
            return Ordering.natural();
        }

        public boolean containsKey(@Nullable Object key) {
            return get(key) != null;
        }

        public Range<C> get(@Nullable Object key) {
            if (key instanceof Cut) {
                try {
                    Cut<C> cut = (Cut) key;
                    if (!this.upperBoundWindow.contains(cut)) {
                        return null;
                    }
                    Entry<Cut<C>, Range<C>> candidate = this.rangesByLowerBound.lowerEntry(cut);
                    if (candidate != null && ((Range) candidate.getValue()).upperBound.equals(cut)) {
                        return (Range) candidate.getValue();
                    }
                } catch (ClassCastException e) {
                    return null;
                }
            }
            return null;
        }

        /* Access modifiers changed, original: 0000 */
        public Iterator<Entry<Cut<C>, Range<C>>> entryIterator() {
            Iterator<Range<C>> backingItr;
            if (this.upperBoundWindow.hasLowerBound()) {
                Iterator<Range<C>> backingItr2;
                Entry<Cut<C>, Range<C>> lowerEntry = this.rangesByLowerBound.lowerEntry(this.upperBoundWindow.lowerEndpoint());
                if (lowerEntry == null) {
                    backingItr2 = this.rangesByLowerBound.values().iterator();
                } else if (this.upperBoundWindow.lowerBound.isLessThan(((Range) lowerEntry.getValue()).upperBound)) {
                    backingItr2 = this.rangesByLowerBound.tailMap(lowerEntry.getKey(), true).values().iterator();
                } else {
                    backingItr = this.rangesByLowerBound.tailMap(this.upperBoundWindow.lowerEndpoint(), true).values().iterator();
                }
                backingItr = backingItr2;
            } else {
                backingItr = this.rangesByLowerBound.values().iterator();
            }
            return new AbstractIterator<Entry<Cut<C>, Range<C>>>() {
                /* Access modifiers changed, original: protected */
                public Entry<Cut<C>, Range<C>> computeNext() {
                    if (!backingItr.hasNext()) {
                        return (Entry) endOfData();
                    }
                    Range<C> range = (Range) backingItr.next();
                    if (RangesByUpperBound.this.upperBoundWindow.upperBound.isLessThan(range.upperBound)) {
                        return (Entry) endOfData();
                    }
                    return Maps.immutableEntry(range.upperBound, range);
                }
            };
        }

        /* Access modifiers changed, original: 0000 */
        public Iterator<Entry<Cut<C>, Range<C>>> descendingEntryIterator() {
            Collection<Range<C>> candidates;
            if (this.upperBoundWindow.hasUpperBound()) {
                candidates = this.rangesByLowerBound.headMap(this.upperBoundWindow.upperEndpoint(), false).descendingMap().values();
            } else {
                candidates = this.rangesByLowerBound.descendingMap().values();
            }
            final PeekingIterator<Range<C>> backingItr = Iterators.peekingIterator(candidates.iterator());
            if (backingItr.hasNext() && this.upperBoundWindow.upperBound.isLessThan(((Range) backingItr.peek()).upperBound)) {
                backingItr.next();
            }
            return new AbstractIterator<Entry<Cut<C>, Range<C>>>() {
                /* Access modifiers changed, original: protected */
                public Entry<Cut<C>, Range<C>> computeNext() {
                    if (!backingItr.hasNext()) {
                        return (Entry) endOfData();
                    }
                    Entry<Cut<C>, Range<C>> immutableEntry;
                    Range<C> range = (Range) backingItr.next();
                    if (RangesByUpperBound.this.upperBoundWindow.lowerBound.isLessThan(range.upperBound)) {
                        immutableEntry = Maps.immutableEntry(range.upperBound, range);
                    } else {
                        immutableEntry = (Entry) endOfData();
                    }
                    return immutableEntry;
                }
            };
        }

        public int size() {
            if (this.upperBoundWindow.equals(Range.all())) {
                return this.rangesByLowerBound.size();
            }
            return Iterators.size(entryIterator());
        }

        public boolean isEmpty() {
            if (this.upperBoundWindow.equals(Range.all())) {
                return this.rangesByLowerBound.isEmpty();
            }
            return !entryIterator().hasNext();
        }
    }

    private static final class SubRangeSetRangesByLowerBound<C extends Comparable<?>> extends AbstractNavigableMap<Cut<C>, Range<C>> {
        private final Range<Cut<C>> lowerBoundWindow;
        private final NavigableMap<Cut<C>, Range<C>> rangesByLowerBound;
        private final NavigableMap<Cut<C>, Range<C>> rangesByUpperBound;
        private final Range<C> restriction;

        private SubRangeSetRangesByLowerBound(Range<Cut<C>> lowerBoundWindow, Range<C> restriction, NavigableMap<Cut<C>, Range<C>> rangesByLowerBound) {
            this.lowerBoundWindow = (Range) Preconditions.checkNotNull(lowerBoundWindow);
            this.restriction = (Range) Preconditions.checkNotNull(restriction);
            this.rangesByLowerBound = (NavigableMap) Preconditions.checkNotNull(rangesByLowerBound);
            this.rangesByUpperBound = new RangesByUpperBound(rangesByLowerBound);
        }

        private NavigableMap<Cut<C>, Range<C>> subMap(Range<Cut<C>> window) {
            if (window.isConnected(this.lowerBoundWindow)) {
                return new SubRangeSetRangesByLowerBound(this.lowerBoundWindow.intersection(window), this.restriction, this.rangesByLowerBound);
            }
            return ImmutableSortedMap.of();
        }

        public NavigableMap<Cut<C>, Range<C>> subMap(Cut<C> fromKey, boolean fromInclusive, Cut<C> toKey, boolean toInclusive) {
            return subMap(Range.range(fromKey, BoundType.forBoolean(fromInclusive), toKey, BoundType.forBoolean(toInclusive)));
        }

        public NavigableMap<Cut<C>, Range<C>> headMap(Cut<C> toKey, boolean inclusive) {
            return subMap(Range.upTo(toKey, BoundType.forBoolean(inclusive)));
        }

        public NavigableMap<Cut<C>, Range<C>> tailMap(Cut<C> fromKey, boolean inclusive) {
            return subMap(Range.downTo(fromKey, BoundType.forBoolean(inclusive)));
        }

        public Comparator<? super Cut<C>> comparator() {
            return Ordering.natural();
        }

        public boolean containsKey(@Nullable Object key) {
            return get(key) != null;
        }

        @Nullable
        public Range<C> get(@Nullable Object key) {
            if (key instanceof Cut) {
                try {
                    Cut<C> cut = (Cut) key;
                    if (this.lowerBoundWindow.contains(cut) && cut.compareTo(this.restriction.lowerBound) >= 0) {
                        if (cut.compareTo(this.restriction.upperBound) < 0) {
                            Range<C> candidate;
                            if (cut.equals(this.restriction.lowerBound)) {
                                candidate = (Range) Maps.valueOrNull(this.rangesByLowerBound.floorEntry(cut));
                                if (candidate != null && candidate.upperBound.compareTo(this.restriction.lowerBound) > 0) {
                                    return candidate.intersection(this.restriction);
                                }
                            } else {
                                candidate = (Range) this.rangesByLowerBound.get(cut);
                                if (candidate != null) {
                                    return candidate.intersection(this.restriction);
                                }
                            }
                        }
                    }
                    return null;
                } catch (ClassCastException e) {
                    return null;
                }
            }
            return null;
        }

        /* Access modifiers changed, original: 0000 */
        public Iterator<Entry<Cut<C>, Range<C>>> entryIterator() {
            if (this.restriction.isEmpty()) {
                return Iterators.emptyIterator();
            }
            if (this.lowerBoundWindow.upperBound.isLessThan(this.restriction.lowerBound)) {
                return Iterators.emptyIterator();
            }
            Iterator<Range<C>> completeRangeItr;
            boolean z = false;
            if (this.lowerBoundWindow.lowerBound.isLessThan(this.restriction.lowerBound)) {
                completeRangeItr = this.rangesByUpperBound.tailMap(this.restriction.lowerBound, false).values().iterator();
            } else {
                NavigableMap navigableMap = this.rangesByLowerBound;
                Comparable endpoint = this.lowerBoundWindow.lowerBound.endpoint();
                if (this.lowerBoundWindow.lowerBoundType() == BoundType.CLOSED) {
                    z = true;
                }
                completeRangeItr = navigableMap.tailMap(endpoint, z).values().iterator();
            }
            final Cut<Cut<C>> upperBoundOnLowerBounds = (Cut) Ordering.natural().min(this.lowerBoundWindow.upperBound, Cut.belowValue(this.restriction.upperBound));
            return new AbstractIterator<Entry<Cut<C>, Range<C>>>() {
                /* Access modifiers changed, original: protected */
                public Entry<Cut<C>, Range<C>> computeNext() {
                    if (!completeRangeItr.hasNext()) {
                        return (Entry) endOfData();
                    }
                    Range<C> nextRange = (Range) completeRangeItr.next();
                    if (upperBoundOnLowerBounds.isLessThan(nextRange.lowerBound)) {
                        return (Entry) endOfData();
                    }
                    nextRange = nextRange.intersection(SubRangeSetRangesByLowerBound.this.restriction);
                    return Maps.immutableEntry(nextRange.lowerBound, nextRange);
                }
            };
        }

        /* Access modifiers changed, original: 0000 */
        public Iterator<Entry<Cut<C>, Range<C>>> descendingEntryIterator() {
            if (this.restriction.isEmpty()) {
                return Iterators.emptyIterator();
            }
            Cut<Cut<C>> upperBoundOnLowerBounds = (Cut) Ordering.natural().min(this.lowerBoundWindow.upperBound, Cut.belowValue(this.restriction.upperBound));
            final Iterator<Range<C>> completeRangeItr = this.rangesByLowerBound.headMap(upperBoundOnLowerBounds.endpoint(), upperBoundOnLowerBounds.typeAsUpperBound() == BoundType.CLOSED).descendingMap().values().iterator();
            return new AbstractIterator<Entry<Cut<C>, Range<C>>>() {
                /* Access modifiers changed, original: protected */
                public Entry<Cut<C>, Range<C>> computeNext() {
                    if (!completeRangeItr.hasNext()) {
                        return (Entry) endOfData();
                    }
                    Range<C> nextRange = (Range) completeRangeItr.next();
                    if (SubRangeSetRangesByLowerBound.this.restriction.lowerBound.compareTo(nextRange.upperBound) >= 0) {
                        return (Entry) endOfData();
                    }
                    nextRange = nextRange.intersection(SubRangeSetRangesByLowerBound.this.restriction);
                    if (SubRangeSetRangesByLowerBound.this.lowerBoundWindow.contains(nextRange.lowerBound)) {
                        return Maps.immutableEntry(nextRange.lowerBound, nextRange);
                    }
                    return (Entry) endOfData();
                }
            };
        }

        public int size() {
            return Iterators.size(entryIterator());
        }
    }

    final class AsRanges extends ForwardingCollection<Range<C>> implements Set<Range<C>> {
        AsRanges() {
        }

        /* Access modifiers changed, original: protected */
        public Collection<Range<C>> delegate() {
            return TreeRangeSet.this.rangesByLowerBound.values();
        }

        public int hashCode() {
            return Sets.hashCodeImpl(this);
        }

        public boolean equals(@Nullable Object o) {
            return Sets.equalsImpl(this, o);
        }
    }

    private final class Complement extends TreeRangeSet<C> {
        Complement() {
            super(new ComplementRangesByLowerBound(TreeRangeSet.this.rangesByLowerBound));
        }

        public void add(Range<C> rangeToAdd) {
            TreeRangeSet.this.remove(rangeToAdd);
        }

        public void remove(Range<C> rangeToRemove) {
            TreeRangeSet.this.add(rangeToRemove);
        }

        public boolean contains(C value) {
            return TreeRangeSet.this.contains(value) ^ 1;
        }

        public RangeSet<C> complement() {
            return TreeRangeSet.this;
        }
    }

    private final class SubRangeSet extends TreeRangeSet<C> {
        private final Range<C> restriction;

        SubRangeSet(Range<C> restriction) {
            super(new SubRangeSetRangesByLowerBound(Range.all(), restriction, TreeRangeSet.this.rangesByLowerBound));
            this.restriction = restriction;
        }

        public boolean encloses(Range<C> range) {
            boolean z = false;
            if (this.restriction.isEmpty() || !this.restriction.encloses(range)) {
                return false;
            }
            Range<C> enclosing = TreeRangeSet.this.rangeEnclosing(range);
            if (!(enclosing == null || enclosing.intersection(this.restriction).isEmpty())) {
                z = true;
            }
            return z;
        }

        @Nullable
        public Range<C> rangeContaining(C value) {
            Range<C> range = null;
            if (!this.restriction.contains(value)) {
                return null;
            }
            Range<C> result = TreeRangeSet.this.rangeContaining(value);
            if (result != null) {
                range = result.intersection(this.restriction);
            }
            return range;
        }

        public void add(Range<C> rangeToAdd) {
            Preconditions.checkArgument(this.restriction.encloses(rangeToAdd), "Cannot add range %s to subRangeSet(%s)", rangeToAdd, this.restriction);
            super.add(rangeToAdd);
        }

        public void remove(Range<C> rangeToRemove) {
            if (rangeToRemove.isConnected(this.restriction)) {
                TreeRangeSet.this.remove(rangeToRemove.intersection(this.restriction));
            }
        }

        public boolean contains(C value) {
            return this.restriction.contains(value) && TreeRangeSet.this.contains(value);
        }

        public void clear() {
            TreeRangeSet.this.remove(this.restriction);
        }

        public RangeSet<C> subRangeSet(Range<C> view) {
            if (view.encloses(this.restriction)) {
                return this;
            }
            if (view.isConnected(this.restriction)) {
                return new SubRangeSet(this.restriction.intersection(view));
            }
            return ImmutableRangeSet.of();
        }
    }

    public /* bridge */ /* synthetic */ void addAll(RangeSet rangeSet) {
        super.addAll(rangeSet);
    }

    public /* bridge */ /* synthetic */ void clear() {
        super.clear();
    }

    public /* bridge */ /* synthetic */ boolean contains(Comparable comparable) {
        return super.contains(comparable);
    }

    public /* bridge */ /* synthetic */ boolean enclosesAll(RangeSet rangeSet) {
        return super.enclosesAll(rangeSet);
    }

    public /* bridge */ /* synthetic */ boolean equals(@Nullable Object obj) {
        return super.equals(obj);
    }

    public /* bridge */ /* synthetic */ boolean isEmpty() {
        return super.isEmpty();
    }

    public /* bridge */ /* synthetic */ void removeAll(RangeSet rangeSet) {
        super.removeAll(rangeSet);
    }

    public static <C extends Comparable<?>> TreeRangeSet<C> create() {
        return new TreeRangeSet(new TreeMap());
    }

    public static <C extends Comparable<?>> TreeRangeSet<C> create(RangeSet<C> rangeSet) {
        TreeRangeSet<C> result = create();
        result.addAll(rangeSet);
        return result;
    }

    private TreeRangeSet(NavigableMap<Cut<C>, Range<C>> rangesByLowerCut) {
        this.rangesByLowerBound = rangesByLowerCut;
    }

    public Set<Range<C>> asRanges() {
        Set<Range<C>> result = this.asRanges;
        if (result != null) {
            return result;
        }
        Set<Range<C>> asRanges = new AsRanges();
        this.asRanges = asRanges;
        return asRanges;
    }

    @Nullable
    public Range<C> rangeContaining(C value) {
        Preconditions.checkNotNull(value);
        Entry<Cut<C>, Range<C>> floorEntry = this.rangesByLowerBound.floorEntry(Cut.belowValue(value));
        if (floorEntry == null || !((Range) floorEntry.getValue()).contains(value)) {
            return null;
        }
        return (Range) floorEntry.getValue();
    }

    public boolean encloses(Range<C> range) {
        Preconditions.checkNotNull(range);
        Entry<Cut<C>, Range<C>> floorEntry = this.rangesByLowerBound.floorEntry(range.lowerBound);
        return floorEntry != null && ((Range) floorEntry.getValue()).encloses(range);
    }

    @Nullable
    private Range<C> rangeEnclosing(Range<C> range) {
        Preconditions.checkNotNull(range);
        Entry<Cut<C>, Range<C>> floorEntry = this.rangesByLowerBound.floorEntry(range.lowerBound);
        if (floorEntry == null || !((Range) floorEntry.getValue()).encloses(range)) {
            return null;
        }
        return (Range) floorEntry.getValue();
    }

    public Range<C> span() {
        Entry<Cut<C>, Range<C>> firstEntry = this.rangesByLowerBound.firstEntry();
        Entry<Cut<C>, Range<C>> lastEntry = this.rangesByLowerBound.lastEntry();
        if (firstEntry != null) {
            return Range.create(((Range) firstEntry.getValue()).lowerBound, ((Range) lastEntry.getValue()).upperBound);
        }
        throw new NoSuchElementException();
    }

    public void add(Range<C> rangeToAdd) {
        Preconditions.checkNotNull(rangeToAdd);
        if (!rangeToAdd.isEmpty()) {
            Cut<C> lbToAdd = rangeToAdd.lowerBound;
            Cut<C> ubToAdd = rangeToAdd.upperBound;
            Entry<Cut<C>, Range<C>> entryBelowLB = this.rangesByLowerBound.lowerEntry(lbToAdd);
            if (entryBelowLB != null) {
                Range<C> rangeBelowLB = (Range) entryBelowLB.getValue();
                if (rangeBelowLB.upperBound.compareTo((Cut) lbToAdd) >= 0) {
                    if (rangeBelowLB.upperBound.compareTo((Cut) ubToAdd) >= 0) {
                        ubToAdd = rangeBelowLB.upperBound;
                    }
                    lbToAdd = rangeBelowLB.lowerBound;
                }
            }
            Entry<Cut<C>, Range<C>> entryBelowUB = this.rangesByLowerBound.floorEntry(ubToAdd);
            if (entryBelowUB != null) {
                Range<C> rangeBelowUB = (Range) entryBelowUB.getValue();
                if (rangeBelowUB.upperBound.compareTo((Cut) ubToAdd) >= 0) {
                    ubToAdd = rangeBelowUB.upperBound;
                }
            }
            this.rangesByLowerBound.subMap(lbToAdd, ubToAdd).clear();
            replaceRangeWithSameLowerBound(Range.create(lbToAdd, ubToAdd));
        }
    }

    public void remove(Range<C> rangeToRemove) {
        Preconditions.checkNotNull(rangeToRemove);
        if (!rangeToRemove.isEmpty()) {
            Entry<Cut<C>, Range<C>> entryBelowLB = this.rangesByLowerBound.lowerEntry(rangeToRemove.lowerBound);
            if (entryBelowLB != null) {
                Range<C> rangeBelowLB = (Range) entryBelowLB.getValue();
                if (rangeBelowLB.upperBound.compareTo(rangeToRemove.lowerBound) >= 0) {
                    if (rangeToRemove.hasUpperBound() && rangeBelowLB.upperBound.compareTo(rangeToRemove.upperBound) >= 0) {
                        replaceRangeWithSameLowerBound(Range.create(rangeToRemove.upperBound, rangeBelowLB.upperBound));
                    }
                    replaceRangeWithSameLowerBound(Range.create(rangeBelowLB.lowerBound, rangeToRemove.lowerBound));
                }
            }
            Entry<Cut<C>, Range<C>> entryBelowUB = this.rangesByLowerBound.floorEntry(rangeToRemove.upperBound);
            if (entryBelowUB != null) {
                Range<C> rangeBelowUB = (Range) entryBelowUB.getValue();
                if (rangeToRemove.hasUpperBound() && rangeBelowUB.upperBound.compareTo(rangeToRemove.upperBound) >= 0) {
                    replaceRangeWithSameLowerBound(Range.create(rangeToRemove.upperBound, rangeBelowUB.upperBound));
                }
            }
            this.rangesByLowerBound.subMap(rangeToRemove.lowerBound, rangeToRemove.upperBound).clear();
        }
    }

    private void replaceRangeWithSameLowerBound(Range<C> range) {
        if (range.isEmpty()) {
            this.rangesByLowerBound.remove(range.lowerBound);
        } else {
            this.rangesByLowerBound.put(range.lowerBound, range);
        }
    }

    public RangeSet<C> complement() {
        RangeSet<C> result = this.complement;
        if (result != null) {
            return result;
        }
        RangeSet<C> complement = new Complement();
        this.complement = complement;
        return complement;
    }

    public RangeSet<C> subRangeSet(Range<C> view) {
        return view.equals(Range.all()) ? this : new SubRangeSet(view);
    }
}
