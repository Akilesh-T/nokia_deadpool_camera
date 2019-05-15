package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedSet.Builder;
import java.util.NoSuchElementException;

@GwtCompatible(emulated = true)
@Beta
public abstract class ContiguousSet<C extends Comparable> extends ImmutableSortedSet<C> {
    final DiscreteDomain<C> domain;

    public abstract ContiguousSet<C> headSetImpl(C c, boolean z);

    public abstract ContiguousSet<C> intersection(ContiguousSet<C> contiguousSet);

    public abstract Range<C> range();

    public abstract Range<C> range(BoundType boundType, BoundType boundType2);

    public abstract ContiguousSet<C> subSetImpl(C c, boolean z, C c2, boolean z2);

    public abstract ContiguousSet<C> tailSetImpl(C c, boolean z);

    public static <C extends Comparable> ContiguousSet<C> create(Range<C> range, DiscreteDomain<C> domain) {
        Preconditions.checkNotNull(range);
        Preconditions.checkNotNull(domain);
        Range<C> effectiveRange = range;
        try {
            if (!range.hasLowerBound()) {
                effectiveRange = effectiveRange.intersection(Range.atLeast(domain.minValue()));
            }
            if (!range.hasUpperBound()) {
                effectiveRange = effectiveRange.intersection(Range.atMost(domain.maxValue()));
            }
            boolean empty = effectiveRange.isEmpty() || Range.compareOrThrow(range.lowerBound.leastValueAbove(domain), range.upperBound.greatestValueBelow(domain)) > 0;
            if (empty) {
                return new EmptyContiguousSet(domain);
            }
            return new RegularContiguousSet(effectiveRange, domain);
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException(e);
        }
    }

    ContiguousSet(DiscreteDomain<C> domain) {
        super(Ordering.natural());
        this.domain = domain;
    }

    public ContiguousSet<C> headSet(C toElement) {
        return headSetImpl((Comparable) Preconditions.checkNotNull(toElement), false);
    }

    @GwtIncompatible("NavigableSet")
    public ContiguousSet<C> headSet(C toElement, boolean inclusive) {
        return headSetImpl((Comparable) Preconditions.checkNotNull(toElement), inclusive);
    }

    public ContiguousSet<C> subSet(C fromElement, C toElement) {
        Preconditions.checkNotNull(fromElement);
        Preconditions.checkNotNull(toElement);
        Preconditions.checkArgument(comparator().compare(fromElement, toElement) <= 0);
        return subSetImpl((Comparable) fromElement, true, (Comparable) toElement, false);
    }

    @GwtIncompatible("NavigableSet")
    public ContiguousSet<C> subSet(C fromElement, boolean fromInclusive, C toElement, boolean toInclusive) {
        Preconditions.checkNotNull(fromElement);
        Preconditions.checkNotNull(toElement);
        Preconditions.checkArgument(comparator().compare(fromElement, toElement) <= 0);
        return subSetImpl((Comparable) fromElement, fromInclusive, (Comparable) toElement, toInclusive);
    }

    public ContiguousSet<C> tailSet(C fromElement) {
        return tailSetImpl((Comparable) Preconditions.checkNotNull(fromElement), true);
    }

    @GwtIncompatible("NavigableSet")
    public ContiguousSet<C> tailSet(C fromElement, boolean inclusive) {
        return tailSetImpl((Comparable) Preconditions.checkNotNull(fromElement), inclusive);
    }

    public String toString() {
        return range().toString();
    }

    @Deprecated
    public static <E> Builder<E> builder() {
        throw new UnsupportedOperationException();
    }
}
