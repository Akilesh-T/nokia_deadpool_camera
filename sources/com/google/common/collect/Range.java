package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import javax.annotation.Nullable;

@GwtCompatible
public final class Range<C extends Comparable> implements Predicate<C>, Serializable {
    private static final Range<Comparable> ALL = new Range(Cut.belowAll(), Cut.aboveAll());
    private static final Function<Range, Cut> LOWER_BOUND_FN = new Function<Range, Cut>() {
        public Cut apply(Range range) {
            return range.lowerBound;
        }
    };
    static final Ordering<Range<?>> RANGE_LEX_ORDERING = new Ordering<Range<?>>() {
        public int compare(Range<?> left, Range<?> right) {
            return ComparisonChain.start().compare(left.lowerBound, right.lowerBound).compare(left.upperBound, right.upperBound).result();
        }
    };
    private static final Function<Range, Cut> UPPER_BOUND_FN = new Function<Range, Cut>() {
        public Cut apply(Range range) {
            return range.upperBound;
        }
    };
    private static final long serialVersionUID = 0;
    final Cut<C> lowerBound;
    final Cut<C> upperBound;

    static <C extends Comparable<?>> Function<Range<C>, Cut<C>> lowerBoundFn() {
        return LOWER_BOUND_FN;
    }

    static <C extends Comparable<?>> Function<Range<C>, Cut<C>> upperBoundFn() {
        return UPPER_BOUND_FN;
    }

    static <C extends Comparable<?>> Range<C> create(Cut<C> lowerBound, Cut<C> upperBound) {
        return new Range(lowerBound, upperBound);
    }

    public static <C extends Comparable<?>> Range<C> open(C lower, C upper) {
        return create(Cut.aboveValue(lower), Cut.belowValue(upper));
    }

    public static <C extends Comparable<?>> Range<C> closed(C lower, C upper) {
        return create(Cut.belowValue(lower), Cut.aboveValue(upper));
    }

    public static <C extends Comparable<?>> Range<C> closedOpen(C lower, C upper) {
        return create(Cut.belowValue(lower), Cut.belowValue(upper));
    }

    public static <C extends Comparable<?>> Range<C> openClosed(C lower, C upper) {
        return create(Cut.aboveValue(lower), Cut.aboveValue(upper));
    }

    public static <C extends Comparable<?>> Range<C> range(C lower, BoundType lowerType, C upper, BoundType upperType) {
        Cut<C> lowerBound;
        Cut<C> upperBound;
        Preconditions.checkNotNull(lowerType);
        Preconditions.checkNotNull(upperType);
        if (lowerType == BoundType.OPEN) {
            lowerBound = Cut.aboveValue(lower);
        } else {
            lowerBound = Cut.belowValue(lower);
        }
        if (upperType == BoundType.OPEN) {
            upperBound = Cut.belowValue(upper);
        } else {
            upperBound = Cut.aboveValue(upper);
        }
        return create(lowerBound, upperBound);
    }

    public static <C extends Comparable<?>> Range<C> lessThan(C endpoint) {
        return create(Cut.belowAll(), Cut.belowValue(endpoint));
    }

    public static <C extends Comparable<?>> Range<C> atMost(C endpoint) {
        return create(Cut.belowAll(), Cut.aboveValue(endpoint));
    }

    public static <C extends Comparable<?>> Range<C> upTo(C endpoint, BoundType boundType) {
        switch (boundType) {
            case OPEN:
                return lessThan(endpoint);
            case CLOSED:
                return atMost(endpoint);
            default:
                throw new AssertionError();
        }
    }

    public static <C extends Comparable<?>> Range<C> greaterThan(C endpoint) {
        return create(Cut.aboveValue(endpoint), Cut.aboveAll());
    }

    public static <C extends Comparable<?>> Range<C> atLeast(C endpoint) {
        return create(Cut.belowValue(endpoint), Cut.aboveAll());
    }

    public static <C extends Comparable<?>> Range<C> downTo(C endpoint, BoundType boundType) {
        switch (boundType) {
            case OPEN:
                return greaterThan(endpoint);
            case CLOSED:
                return atLeast(endpoint);
            default:
                throw new AssertionError();
        }
    }

    public static <C extends Comparable<?>> Range<C> all() {
        return ALL;
    }

    public static <C extends Comparable<?>> Range<C> singleton(C value) {
        return closed(value, value);
    }

    public static <C extends Comparable<?>> Range<C> encloseAll(Iterable<C> values) {
        Preconditions.checkNotNull(values);
        if (values instanceof ContiguousSet) {
            return ((ContiguousSet) values).range();
        }
        Iterator<C> valueIterator = values.iterator();
        C max = (Comparable) Preconditions.checkNotNull(valueIterator.next());
        C min = max;
        while (valueIterator.hasNext()) {
            Comparable value = (Comparable) Preconditions.checkNotNull(valueIterator.next());
            Comparable min2 = (Comparable) Ordering.natural().min(min2, value);
            Comparable max2 = (Comparable) Ordering.natural().max(max2, value);
        }
        return closed(min2, max2);
    }

    private Range(Cut<C> lowerBound, Cut<C> upperBound) {
        if (lowerBound.compareTo((Cut) upperBound) > 0 || lowerBound == Cut.aboveAll() || upperBound == Cut.belowAll()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid range: ");
            stringBuilder.append(toString(lowerBound, upperBound));
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        this.lowerBound = (Cut) Preconditions.checkNotNull(lowerBound);
        this.upperBound = (Cut) Preconditions.checkNotNull(upperBound);
    }

    public boolean hasLowerBound() {
        return this.lowerBound != Cut.belowAll();
    }

    public C lowerEndpoint() {
        return this.lowerBound.endpoint();
    }

    public BoundType lowerBoundType() {
        return this.lowerBound.typeAsLowerBound();
    }

    public boolean hasUpperBound() {
        return this.upperBound != Cut.aboveAll();
    }

    public C upperEndpoint() {
        return this.upperBound.endpoint();
    }

    public BoundType upperBoundType() {
        return this.upperBound.typeAsUpperBound();
    }

    public boolean isEmpty() {
        return this.lowerBound.equals(this.upperBound);
    }

    public boolean contains(C value) {
        Preconditions.checkNotNull(value);
        return this.lowerBound.isLessThan(value) && !this.upperBound.isLessThan(value);
    }

    @Deprecated
    public boolean apply(C input) {
        return contains(input);
    }

    public boolean containsAll(Iterable<? extends C> values) {
        boolean z = true;
        if (Iterables.isEmpty(values)) {
            return true;
        }
        if (values instanceof SortedSet) {
            SortedSet<? extends C> set = cast(values);
            Comparator<?> comparator = set.comparator();
            if (Ordering.natural().equals(comparator) || comparator == null) {
                if (!(contains((Comparable) set.first()) && contains((Comparable) set.last()))) {
                    z = false;
                }
                return z;
            }
        }
        Iterator it = values.iterator();
        while (it.hasNext()) {
            if (!contains((Comparable) it.next())) {
                return false;
            }
        }
        return true;
    }

    public boolean encloses(Range<C> other) {
        return this.lowerBound.compareTo(other.lowerBound) <= 0 && this.upperBound.compareTo(other.upperBound) >= 0;
    }

    public boolean isConnected(Range<C> other) {
        return this.lowerBound.compareTo(other.upperBound) <= 0 && other.lowerBound.compareTo(this.upperBound) <= 0;
    }

    public Range<C> intersection(Range<C> connectedRange) {
        int lowerCmp = this.lowerBound.compareTo(connectedRange.lowerBound);
        int upperCmp = this.upperBound.compareTo(connectedRange.upperBound);
        if (lowerCmp >= 0 && upperCmp <= 0) {
            return this;
        }
        if (lowerCmp <= 0 && upperCmp >= 0) {
            return connectedRange;
        }
        return create(lowerCmp >= 0 ? this.lowerBound : connectedRange.lowerBound, upperCmp <= 0 ? this.upperBound : connectedRange.upperBound);
    }

    public Range<C> span(Range<C> other) {
        int lowerCmp = this.lowerBound.compareTo(other.lowerBound);
        int upperCmp = this.upperBound.compareTo(other.upperBound);
        if (lowerCmp <= 0 && upperCmp >= 0) {
            return this;
        }
        if (lowerCmp >= 0 && upperCmp <= 0) {
            return other;
        }
        return create(lowerCmp <= 0 ? this.lowerBound : other.lowerBound, upperCmp >= 0 ? this.upperBound : other.upperBound);
    }

    public Range<C> canonical(DiscreteDomain<C> domain) {
        Preconditions.checkNotNull(domain);
        Cut<C> lower = this.lowerBound.canonical(domain);
        Cut<C> upper = this.upperBound.canonical(domain);
        return (lower == this.lowerBound && upper == this.upperBound) ? this : create(lower, upper);
    }

    public boolean equals(@Nullable Object object) {
        boolean z = false;
        if (!(object instanceof Range)) {
            return false;
        }
        Range<?> other = (Range) object;
        if (this.lowerBound.equals(other.lowerBound) && this.upperBound.equals(other.upperBound)) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        return (this.lowerBound.hashCode() * 31) + this.upperBound.hashCode();
    }

    public String toString() {
        return toString(this.lowerBound, this.upperBound);
    }

    private static String toString(Cut<?> lowerBound, Cut<?> upperBound) {
        StringBuilder sb = new StringBuilder(16);
        lowerBound.describeAsLowerBound(sb);
        sb.append(8229);
        upperBound.describeAsUpperBound(sb);
        return sb.toString();
    }

    private static <T> SortedSet<T> cast(Iterable<T> iterable) {
        return (SortedSet) iterable;
    }

    /* Access modifiers changed, original: 0000 */
    public Object readResolve() {
        if (equals(ALL)) {
            return all();
        }
        return this;
    }

    static int compareOrThrow(Comparable left, Comparable right) {
        return left.compareTo(right);
    }
}
