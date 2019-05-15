package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multiset.Entry;
import java.util.Collection;
import java.util.Comparator;
import javax.annotation.Nullable;

final class EmptyImmutableSortedMultiset<E> extends ImmutableSortedMultiset<E> {
    private final ImmutableSortedSet<E> elementSet;

    EmptyImmutableSortedMultiset(Comparator<? super E> comparator) {
        this.elementSet = ImmutableSortedSet.emptySet(comparator);
    }

    public Entry<E> firstEntry() {
        return null;
    }

    public Entry<E> lastEntry() {
        return null;
    }

    public int count(@Nullable Object element) {
        return 0;
    }

    public boolean containsAll(Collection<?> targets) {
        return targets.isEmpty();
    }

    public int size() {
        return 0;
    }

    public ImmutableSortedSet<E> elementSet() {
        return this.elementSet;
    }

    /* Access modifiers changed, original: 0000 */
    public Entry<E> getEntry(int index) {
        throw new AssertionError("should never be called");
    }

    public ImmutableSortedMultiset<E> headMultiset(E upperBound, BoundType boundType) {
        Preconditions.checkNotNull(upperBound);
        Preconditions.checkNotNull(boundType);
        return this;
    }

    public ImmutableSortedMultiset<E> tailMultiset(E lowerBound, BoundType boundType) {
        Preconditions.checkNotNull(lowerBound);
        Preconditions.checkNotNull(boundType);
        return this;
    }

    public UnmodifiableIterator<E> iterator() {
        return Iterators.emptyIterator();
    }

    public boolean equals(@Nullable Object object) {
        if (object instanceof Multiset) {
            return ((Multiset) object).isEmpty();
        }
        return false;
    }

    /* Access modifiers changed, original: 0000 */
    public boolean isPartialView() {
        return false;
    }

    /* Access modifiers changed, original: 0000 */
    public int copyIntoArray(Object[] dst, int offset) {
        return offset;
    }

    public ImmutableList<E> asList() {
        return ImmutableList.of();
    }
}
