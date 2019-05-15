package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Preconditions;
import com.google.common.collect.SortedLists.KeyAbsentBehavior;
import com.google.common.collect.SortedLists.KeyPresentBehavior;
import com.google.common.primitives.Ints;
import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.annotation.Nullable;

@Beta
public final class ImmutableRangeSet<C extends Comparable> extends AbstractRangeSet<C> implements Serializable {
    private static final ImmutableRangeSet<Comparable<?>> ALL = new ImmutableRangeSet(ImmutableList.of(Range.all()));
    private static final ImmutableRangeSet<Comparable<?>> EMPTY = new ImmutableRangeSet(ImmutableList.of());
    private transient ImmutableRangeSet<C> complement;
    private final transient ImmutableList<Range<C>> ranges;

    private static class AsSetSerializedForm<C extends Comparable> implements Serializable {
        private final DiscreteDomain<C> domain;
        private final ImmutableList<Range<C>> ranges;

        AsSetSerializedForm(ImmutableList<Range<C>> ranges, DiscreteDomain<C> domain) {
            this.ranges = ranges;
            this.domain = domain;
        }

        /* Access modifiers changed, original: 0000 */
        public Object readResolve() {
            return new ImmutableRangeSet(this.ranges).asSet(this.domain);
        }
    }

    public static class Builder<C extends Comparable<?>> {
        private final RangeSet<C> rangeSet = TreeRangeSet.create();

        public Builder<C> add(Range<C> range) {
            if (range.isEmpty()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("range must not be empty, but was ");
                stringBuilder.append(range);
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (this.rangeSet.complement().encloses(range)) {
                this.rangeSet.add(range);
                return this;
            } else {
                for (Range<C> currentRange : this.rangeSet.asRanges()) {
                    boolean z = !currentRange.isConnected(range) || currentRange.intersection(range).isEmpty();
                    Preconditions.checkArgument(z, "Ranges may not overlap, but received %s and %s", currentRange, range);
                }
                throw new AssertionError("should have thrown an IAE above");
            }
        }

        public Builder<C> addAll(RangeSet<C> ranges) {
            for (Range<C> range : ranges.asRanges()) {
                add(range);
            }
            return this;
        }

        public ImmutableRangeSet<C> build() {
            return ImmutableRangeSet.copyOf(this.rangeSet);
        }
    }

    private static final class SerializedForm<C extends Comparable> implements Serializable {
        private final ImmutableList<Range<C>> ranges;

        SerializedForm(ImmutableList<Range<C>> ranges) {
            this.ranges = ranges;
        }

        /* Access modifiers changed, original: 0000 */
        public Object readResolve() {
            if (this.ranges.isEmpty()) {
                return ImmutableRangeSet.of();
            }
            if (this.ranges.equals(ImmutableList.of(Range.all()))) {
                return ImmutableRangeSet.all();
            }
            return new ImmutableRangeSet(this.ranges);
        }
    }

    private final class ComplementRanges extends ImmutableList<Range<C>> {
        private final boolean positiveBoundedAbove;
        private final boolean positiveBoundedBelow;
        private final int size;

        ComplementRanges() {
            this.positiveBoundedBelow = ((Range) ImmutableRangeSet.this.ranges.get(0)).hasLowerBound();
            this.positiveBoundedAbove = ((Range) Iterables.getLast(ImmutableRangeSet.this.ranges)).hasUpperBound();
            int size = ImmutableRangeSet.this.ranges.size() - 1;
            if (this.positiveBoundedBelow) {
                size++;
            }
            if (this.positiveBoundedAbove) {
                size++;
            }
            this.size = size;
        }

        public int size() {
            return this.size;
        }

        public Range<C> get(int index) {
            Cut<C> upperBound;
            Preconditions.checkElementIndex(index, this.size);
            Cut<C> lowerBound = this.positiveBoundedBelow ? index == 0 ? Cut.belowAll() : ((Range) ImmutableRangeSet.this.ranges.get(index - 1)).upperBound : ((Range) ImmutableRangeSet.this.ranges.get(index)).upperBound;
            if (this.positiveBoundedAbove && index == this.size - 1) {
                upperBound = Cut.aboveAll();
            } else {
                upperBound = ((Range) ImmutableRangeSet.this.ranges.get((this.positiveBoundedBelow ^ 1) + index)).lowerBound;
            }
            return Range.create(lowerBound, upperBound);
        }

        /* Access modifiers changed, original: 0000 */
        public boolean isPartialView() {
            return true;
        }
    }

    private final class AsSet extends ImmutableSortedSet<C> {
        private final DiscreteDomain<C> domain;
        private transient Integer size;

        AsSet(DiscreteDomain<C> domain) {
            super(Ordering.natural());
            this.domain = domain;
        }

        public int size() {
            Integer result = this.size;
            if (result == null) {
                long total = 0;
                UnmodifiableIterator it = ImmutableRangeSet.this.ranges.iterator();
                while (it.hasNext()) {
                    total += (long) ContiguousSet.create((Range) it.next(), this.domain).size();
                    if (total >= 2147483647L) {
                        break;
                    }
                }
                Integer valueOf = Integer.valueOf(Ints.saturatedCast(total));
                this.size = valueOf;
                result = valueOf;
            }
            return result.intValue();
        }

        public UnmodifiableIterator<C> iterator() {
            return new AbstractIterator<C>() {
                Iterator<C> elemItr = Iterators.emptyIterator();
                final Iterator<Range<C>> rangeItr = ImmutableRangeSet.this.ranges.iterator();

                /* Access modifiers changed, original: protected */
                public C computeNext() {
                    while (!this.elemItr.hasNext()) {
                        if (!this.rangeItr.hasNext()) {
                            return (Comparable) endOfData();
                        }
                        this.elemItr = ContiguousSet.create((Range) this.rangeItr.next(), AsSet.this.domain).iterator();
                    }
                    return (Comparable) this.elemItr.next();
                }
            };
        }

        @GwtIncompatible("NavigableSet")
        public UnmodifiableIterator<C> descendingIterator() {
            return new AbstractIterator<C>() {
                Iterator<C> elemItr = Iterators.emptyIterator();
                final Iterator<Range<C>> rangeItr = ImmutableRangeSet.this.ranges.reverse().iterator();

                /* Access modifiers changed, original: protected */
                public C computeNext() {
                    while (!this.elemItr.hasNext()) {
                        if (!this.rangeItr.hasNext()) {
                            return (Comparable) endOfData();
                        }
                        this.elemItr = ContiguousSet.create((Range) this.rangeItr.next(), AsSet.this.domain).descendingIterator();
                    }
                    return (Comparable) this.elemItr.next();
                }
            };
        }

        /* Access modifiers changed, original: 0000 */
        public ImmutableSortedSet<C> subSet(Range<C> range) {
            return ImmutableRangeSet.this.subRangeSet((Range) range).asSet(this.domain);
        }

        /* Access modifiers changed, original: 0000 */
        public ImmutableSortedSet<C> headSetImpl(C toElement, boolean inclusive) {
            return subSet(Range.upTo(toElement, BoundType.forBoolean(inclusive)));
        }

        /* Access modifiers changed, original: 0000 */
        public ImmutableSortedSet<C> subSetImpl(C fromElement, boolean fromInclusive, C toElement, boolean toInclusive) {
            if (fromInclusive || toInclusive || Range.compareOrThrow(fromElement, toElement) != 0) {
                return subSet(Range.range(fromElement, BoundType.forBoolean(fromInclusive), toElement, BoundType.forBoolean(toInclusive)));
            }
            return ImmutableSortedSet.of();
        }

        /* Access modifiers changed, original: 0000 */
        public ImmutableSortedSet<C> tailSetImpl(C fromElement, boolean inclusive) {
            return subSet(Range.downTo(fromElement, BoundType.forBoolean(inclusive)));
        }

        public boolean contains(@Nullable Object o) {
            if (o == null) {
                return false;
            }
            try {
                return ImmutableRangeSet.this.contains((Comparable) o);
            } catch (ClassCastException e) {
                return false;
            }
        }

        /* Access modifiers changed, original: 0000 */
        public int indexOf(Object target) {
            if (!contains(target)) {
                return -1;
            }
            Comparable c = (Comparable) target;
            long total = 0;
            UnmodifiableIterator it = ImmutableRangeSet.this.ranges.iterator();
            while (it.hasNext()) {
                Range<C> range = (Range) it.next();
                if (range.contains(c)) {
                    return Ints.saturatedCast(((long) ContiguousSet.create(range, this.domain).indexOf(c)) + total);
                }
                total += (long) ContiguousSet.create(range, this.domain).size();
            }
            throw new AssertionError("impossible");
        }

        /* Access modifiers changed, original: 0000 */
        public boolean isPartialView() {
            return ImmutableRangeSet.this.ranges.isPartialView();
        }

        public String toString() {
            return ImmutableRangeSet.this.ranges.toString();
        }

        /* Access modifiers changed, original: 0000 */
        public Object writeReplace() {
            return new AsSetSerializedForm(ImmutableRangeSet.this.ranges, this.domain);
        }
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

    public static <C extends Comparable> ImmutableRangeSet<C> of() {
        return EMPTY;
    }

    static <C extends Comparable> ImmutableRangeSet<C> all() {
        return ALL;
    }

    public static <C extends Comparable> ImmutableRangeSet<C> of(Range<C> range) {
        Preconditions.checkNotNull(range);
        if (range.isEmpty()) {
            return of();
        }
        if (range.equals(Range.all())) {
            return all();
        }
        return new ImmutableRangeSet(ImmutableList.of(range));
    }

    public static <C extends Comparable> ImmutableRangeSet<C> copyOf(RangeSet<C> rangeSet) {
        Preconditions.checkNotNull(rangeSet);
        if (rangeSet.isEmpty()) {
            return of();
        }
        if (rangeSet.encloses(Range.all())) {
            return all();
        }
        if (rangeSet instanceof ImmutableRangeSet) {
            ImmutableRangeSet<C> immutableRangeSet = (ImmutableRangeSet) rangeSet;
            if (!immutableRangeSet.isPartialView()) {
                return immutableRangeSet;
            }
        }
        return new ImmutableRangeSet(ImmutableList.copyOf(rangeSet.asRanges()));
    }

    ImmutableRangeSet(ImmutableList<Range<C>> ranges) {
        this.ranges = ranges;
    }

    private ImmutableRangeSet(ImmutableList<Range<C>> ranges, ImmutableRangeSet<C> complement) {
        this.ranges = ranges;
        this.complement = complement;
    }

    public boolean encloses(Range<C> otherRange) {
        int index = SortedLists.binarySearch(this.ranges, Range.lowerBoundFn(), otherRange.lowerBound, Ordering.natural(), KeyPresentBehavior.ANY_PRESENT, KeyAbsentBehavior.NEXT_LOWER);
        return index != -1 && ((Range) this.ranges.get(index)).encloses(otherRange);
    }

    public Range<C> rangeContaining(C value) {
        int index = SortedLists.binarySearch(this.ranges, Range.lowerBoundFn(), Cut.belowValue(value), Ordering.natural(), KeyPresentBehavior.ANY_PRESENT, KeyAbsentBehavior.NEXT_LOWER);
        Range<C> range = null;
        if (index == -1) {
            return null;
        }
        Range<C> range2 = (Range) this.ranges.get(index);
        if (range2.contains(value)) {
            range = range2;
        }
        return range;
    }

    public Range<C> span() {
        if (!this.ranges.isEmpty()) {
            return Range.create(((Range) this.ranges.get(0)).lowerBound, ((Range) this.ranges.get(this.ranges.size() - 1)).upperBound);
        }
        throw new NoSuchElementException();
    }

    public boolean isEmpty() {
        return this.ranges.isEmpty();
    }

    public void add(Range<C> range) {
        throw new UnsupportedOperationException();
    }

    public void addAll(RangeSet<C> rangeSet) {
        throw new UnsupportedOperationException();
    }

    public void remove(Range<C> range) {
        throw new UnsupportedOperationException();
    }

    public void removeAll(RangeSet<C> rangeSet) {
        throw new UnsupportedOperationException();
    }

    public ImmutableSet<Range<C>> asRanges() {
        if (this.ranges.isEmpty()) {
            return ImmutableSet.of();
        }
        return new RegularImmutableSortedSet(this.ranges, Range.RANGE_LEX_ORDERING);
    }

    public ImmutableRangeSet<C> complement() {
        ImmutableRangeSet<C> result = this.complement;
        if (result != null) {
            return result;
        }
        ImmutableRangeSet all;
        if (this.ranges.isEmpty()) {
            all = all();
            this.complement = all;
            return all;
        } else if (this.ranges.size() == 1 && ((Range) this.ranges.get(0)).equals(Range.all())) {
            all = of();
            this.complement = all;
            return all;
        } else {
            ImmutableRangeSet<C> immutableRangeSet = new ImmutableRangeSet(new ComplementRanges(), this);
            this.complement = immutableRangeSet;
            return immutableRangeSet;
        }
    }

    private ImmutableList<Range<C>> intersectRanges(final Range<C> range) {
        if (this.ranges.isEmpty() || range.isEmpty()) {
            return ImmutableList.of();
        }
        if (range.encloses(span())) {
            return this.ranges;
        }
        int fromIndex;
        int toIndex;
        if (range.hasLowerBound()) {
            fromIndex = SortedLists.binarySearch(this.ranges, Range.upperBoundFn(), range.lowerBound, KeyPresentBehavior.FIRST_AFTER, KeyAbsentBehavior.NEXT_HIGHER);
        } else {
            fromIndex = 0;
        }
        if (range.hasUpperBound()) {
            toIndex = SortedLists.binarySearch(this.ranges, Range.lowerBoundFn(), range.upperBound, KeyPresentBehavior.FIRST_PRESENT, KeyAbsentBehavior.NEXT_HIGHER);
        } else {
            toIndex = this.ranges.size();
        }
        final int length = toIndex - fromIndex;
        if (length == 0) {
            return ImmutableList.of();
        }
        return new ImmutableList<Range<C>>() {
            public int size() {
                return length;
            }

            public Range<C> get(int index) {
                Preconditions.checkElementIndex(index, length);
                if (index == 0 || index == length - 1) {
                    return ((Range) ImmutableRangeSet.this.ranges.get(fromIndex + index)).intersection(range);
                }
                return (Range) ImmutableRangeSet.this.ranges.get(fromIndex + index);
            }

            /* Access modifiers changed, original: 0000 */
            public boolean isPartialView() {
                return true;
            }
        };
    }

    public ImmutableRangeSet<C> subRangeSet(Range<C> range) {
        if (!isEmpty()) {
            Range<C> span = span();
            if (range.encloses(span)) {
                return this;
            }
            if (range.isConnected(span)) {
                return new ImmutableRangeSet(intersectRanges(range));
            }
        }
        return of();
    }

    public ImmutableSortedSet<C> asSet(DiscreteDomain<C> domain) {
        Preconditions.checkNotNull(domain);
        if (isEmpty()) {
            return ImmutableSortedSet.of();
        }
        Range<C> span = span().canonical(domain);
        if (span.hasLowerBound()) {
            if (!span.hasUpperBound()) {
                try {
                    domain.maxValue();
                } catch (NoSuchElementException e) {
                    throw new IllegalArgumentException("Neither the DiscreteDomain nor this range set are bounded above");
                }
            }
            return new AsSet(domain);
        }
        throw new IllegalArgumentException("Neither the DiscreteDomain nor this range set are bounded below");
    }

    /* Access modifiers changed, original: 0000 */
    public boolean isPartialView() {
        return this.ranges.isPartialView();
    }

    public static <C extends Comparable<?>> Builder<C> builder() {
        return new Builder();
    }

    /* Access modifiers changed, original: 0000 */
    public Object writeReplace() {
        return new SerializedForm(this.ranges);
    }
}
