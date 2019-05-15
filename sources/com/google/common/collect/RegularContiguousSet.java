package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.Collection;
import javax.annotation.Nullable;

@GwtCompatible(emulated = true)
final class RegularContiguousSet<C extends Comparable> extends ContiguousSet<C> {
    private static final long serialVersionUID = 0;
    private final Range<C> range;

    @GwtIncompatible("serialization")
    private static final class SerializedForm<C extends Comparable> implements Serializable {
        final DiscreteDomain<C> domain;
        final Range<C> range;

        /* synthetic */ SerializedForm(Range x0, DiscreteDomain x1, AnonymousClass1 x2) {
            this(x0, x1);
        }

        private SerializedForm(Range<C> range, DiscreteDomain<C> domain) {
            this.range = range;
            this.domain = domain;
        }

        private Object readResolve() {
            return new RegularContiguousSet(this.range, this.domain);
        }
    }

    RegularContiguousSet(Range<C> range, DiscreteDomain<C> domain) {
        super(domain);
        this.range = range;
    }

    private ContiguousSet<C> intersectionInCurrentDomain(Range<C> other) {
        if (this.range.isConnected(other)) {
            return ContiguousSet.create(this.range.intersection(other), this.domain);
        }
        return new EmptyContiguousSet(this.domain);
    }

    /* Access modifiers changed, original: 0000 */
    public ContiguousSet<C> headSetImpl(C toElement, boolean inclusive) {
        return intersectionInCurrentDomain(Range.upTo(toElement, BoundType.forBoolean(inclusive)));
    }

    /* Access modifiers changed, original: 0000 */
    public ContiguousSet<C> subSetImpl(C fromElement, boolean fromInclusive, C toElement, boolean toInclusive) {
        if (fromElement.compareTo(toElement) != 0 || fromInclusive || toInclusive) {
            return intersectionInCurrentDomain(Range.range(fromElement, BoundType.forBoolean(fromInclusive), toElement, BoundType.forBoolean(toInclusive)));
        }
        return new EmptyContiguousSet(this.domain);
    }

    /* Access modifiers changed, original: 0000 */
    public ContiguousSet<C> tailSetImpl(C fromElement, boolean inclusive) {
        return intersectionInCurrentDomain(Range.downTo(fromElement, BoundType.forBoolean(inclusive)));
    }

    /* Access modifiers changed, original: 0000 */
    @GwtIncompatible("not used by GWT emulation")
    public int indexOf(Object target) {
        return contains(target) ? (int) this.domain.distance(first(), (Comparable) target) : -1;
    }

    public UnmodifiableIterator<C> iterator() {
        return new AbstractSequentialIterator<C>(first()) {
            final C last = RegularContiguousSet.this.last();

            /* Access modifiers changed, original: protected */
            public C computeNext(C previous) {
                return RegularContiguousSet.equalsOrThrow(previous, this.last) ? null : RegularContiguousSet.this.domain.next(previous);
            }
        };
    }

    @GwtIncompatible("NavigableSet")
    public UnmodifiableIterator<C> descendingIterator() {
        return new AbstractSequentialIterator<C>(last()) {
            final C first = RegularContiguousSet.this.first();

            /* Access modifiers changed, original: protected */
            public C computeNext(C previous) {
                return RegularContiguousSet.equalsOrThrow(previous, this.first) ? null : RegularContiguousSet.this.domain.previous(previous);
            }
        };
    }

    private static boolean equalsOrThrow(Comparable<?> left, @Nullable Comparable<?> right) {
        return right != null && Range.compareOrThrow(left, right) == 0;
    }

    /* Access modifiers changed, original: 0000 */
    public boolean isPartialView() {
        return false;
    }

    public C first() {
        return this.range.lowerBound.leastValueAbove(this.domain);
    }

    public C last() {
        return this.range.upperBound.greatestValueBelow(this.domain);
    }

    public int size() {
        long distance = this.domain.distance(first(), last());
        return distance >= 2147483647L ? ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED : ((int) distance) + 1;
    }

    public boolean contains(@Nullable Object object) {
        if (object == null) {
            return false;
        }
        try {
            return this.range.contains((Comparable) object);
        } catch (ClassCastException e) {
            return false;
        }
    }

    public boolean containsAll(Collection<?> targets) {
        return Collections2.containsAllImpl(this, targets);
    }

    public boolean isEmpty() {
        return false;
    }

    public ContiguousSet<C> intersection(ContiguousSet<C> other) {
        Preconditions.checkNotNull(other);
        Preconditions.checkArgument(this.domain.equals(other.domain));
        if (other.isEmpty()) {
            return other;
        }
        ContiguousSet<C> create;
        Comparable lowerEndpoint = (Comparable) Ordering.natural().max(first(), other.first());
        Comparable upperEndpoint = (Comparable) Ordering.natural().min(last(), other.last());
        if (lowerEndpoint.compareTo(upperEndpoint) < 0) {
            create = ContiguousSet.create(Range.closed(lowerEndpoint, upperEndpoint), this.domain);
        } else {
            create = new EmptyContiguousSet(this.domain);
        }
        return create;
    }

    public Range<C> range() {
        return range(BoundType.CLOSED, BoundType.CLOSED);
    }

    public Range<C> range(BoundType lowerBoundType, BoundType upperBoundType) {
        return Range.create(this.range.lowerBound.withLowerBoundType(lowerBoundType, this.domain), this.range.upperBound.withUpperBoundType(upperBoundType, this.domain));
    }

    public boolean equals(@Nullable Object object) {
        boolean z = true;
        if (object == this) {
            return true;
        }
        if (object instanceof RegularContiguousSet) {
            RegularContiguousSet<?> that = (RegularContiguousSet) object;
            if (this.domain.equals(that.domain)) {
                if (!(first().equals(that.first()) && last().equals(that.last()))) {
                    z = false;
                }
                return z;
            }
        }
        return super.equals(object);
    }

    public int hashCode() {
        return Sets.hashCodeImpl(this);
    }

    /* Access modifiers changed, original: 0000 */
    @GwtIncompatible("serialization")
    public Object writeReplace() {
        return new SerializedForm(this.range, this.domain, null);
    }
}
