package com.google.common.collect;

import com.google.common.annotations.Beta;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;

public abstract class ForwardingNavigableSet<E> extends ForwardingSortedSet<E> implements NavigableSet<E> {

    @Beta
    protected class StandardDescendingSet extends DescendingSet<E> {
        public StandardDescendingSet() {
            super(ForwardingNavigableSet.this);
        }
    }

    public abstract NavigableSet<E> delegate();

    protected ForwardingNavigableSet() {
    }

    public E lower(E e) {
        return delegate().lower(e);
    }

    /* Access modifiers changed, original: protected */
    public E standardLower(E e) {
        return Iterators.getNext(headSet(e, false).descendingIterator(), null);
    }

    public E floor(E e) {
        return delegate().floor(e);
    }

    /* Access modifiers changed, original: protected */
    public E standardFloor(E e) {
        return Iterators.getNext(headSet(e, true).descendingIterator(), null);
    }

    public E ceiling(E e) {
        return delegate().ceiling(e);
    }

    /* Access modifiers changed, original: protected */
    public E standardCeiling(E e) {
        return Iterators.getNext(tailSet(e, true).iterator(), null);
    }

    public E higher(E e) {
        return delegate().higher(e);
    }

    /* Access modifiers changed, original: protected */
    public E standardHigher(E e) {
        return Iterators.getNext(tailSet(e, false).iterator(), null);
    }

    public E pollFirst() {
        return delegate().pollFirst();
    }

    /* Access modifiers changed, original: protected */
    public E standardPollFirst() {
        return Iterators.pollNext(iterator());
    }

    public E pollLast() {
        return delegate().pollLast();
    }

    /* Access modifiers changed, original: protected */
    public E standardPollLast() {
        return Iterators.pollNext(descendingIterator());
    }

    /* Access modifiers changed, original: protected */
    public E standardFirst() {
        return iterator().next();
    }

    /* Access modifiers changed, original: protected */
    public E standardLast() {
        return descendingIterator().next();
    }

    public NavigableSet<E> descendingSet() {
        return delegate().descendingSet();
    }

    public Iterator<E> descendingIterator() {
        return delegate().descendingIterator();
    }

    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        return delegate().subSet(fromElement, fromInclusive, toElement, toInclusive);
    }

    /* Access modifiers changed, original: protected */
    @Beta
    public NavigableSet<E> standardSubSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        return tailSet(fromElement, fromInclusive).headSet(toElement, toInclusive);
    }

    /* Access modifiers changed, original: protected */
    public SortedSet<E> standardSubSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return delegate().headSet(toElement, inclusive);
    }

    /* Access modifiers changed, original: protected */
    public SortedSet<E> standardHeadSet(E toElement) {
        return headSet(toElement, false);
    }

    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return delegate().tailSet(fromElement, inclusive);
    }

    /* Access modifiers changed, original: protected */
    public SortedSet<E> standardTailSet(E fromElement) {
        return tailSet(fromElement, true);
    }
}
