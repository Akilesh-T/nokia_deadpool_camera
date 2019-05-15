package com.google.common.collect;

import com.google.common.annotations.GwtIncompatible;
import javax.annotation.Nullable;

class DescendingImmutableSortedSet<E> extends ImmutableSortedSet<E> {
    private final ImmutableSortedSet<E> forward;

    DescendingImmutableSortedSet(ImmutableSortedSet<E> forward) {
        super(Ordering.from(forward.comparator()).reverse());
        this.forward = forward;
    }

    public int size() {
        return this.forward.size();
    }

    public UnmodifiableIterator<E> iterator() {
        return this.forward.descendingIterator();
    }

    /* Access modifiers changed, original: 0000 */
    public ImmutableSortedSet<E> headSetImpl(E toElement, boolean inclusive) {
        return this.forward.tailSet((Object) toElement, inclusive).descendingSet();
    }

    /* Access modifiers changed, original: 0000 */
    public ImmutableSortedSet<E> subSetImpl(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        return this.forward.subSet((Object) toElement, toInclusive, (Object) fromElement, fromInclusive).descendingSet();
    }

    /* Access modifiers changed, original: 0000 */
    public ImmutableSortedSet<E> tailSetImpl(E fromElement, boolean inclusive) {
        return this.forward.headSet((Object) fromElement, inclusive).descendingSet();
    }

    @GwtIncompatible("NavigableSet")
    public ImmutableSortedSet<E> descendingSet() {
        return this.forward;
    }

    @GwtIncompatible("NavigableSet")
    public UnmodifiableIterator<E> descendingIterator() {
        return this.forward.iterator();
    }

    /* Access modifiers changed, original: 0000 */
    @GwtIncompatible("NavigableSet")
    public ImmutableSortedSet<E> createDescendingSet() {
        throw new AssertionError("should never be called");
    }

    public E lower(E element) {
        return this.forward.higher(element);
    }

    public E floor(E element) {
        return this.forward.ceiling(element);
    }

    public E ceiling(E element) {
        return this.forward.floor(element);
    }

    public E higher(E element) {
        return this.forward.lower(element);
    }

    /* Access modifiers changed, original: 0000 */
    public int indexOf(@Nullable Object target) {
        int index = this.forward.indexOf(target);
        if (index == -1) {
            return index;
        }
        return (size() - 1) - index;
    }

    /* Access modifiers changed, original: 0000 */
    public boolean isPartialView() {
        return this.forward.isPartialView();
    }
}
