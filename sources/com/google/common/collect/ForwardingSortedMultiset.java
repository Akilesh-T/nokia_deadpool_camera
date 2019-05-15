package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multiset.Entry;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;

@GwtCompatible(emulated = true)
@Beta
public abstract class ForwardingSortedMultiset<E> extends ForwardingMultiset<E> implements SortedMultiset<E> {

    protected abstract class StandardDescendingMultiset extends DescendingMultiset<E> {
        /* Access modifiers changed, original: 0000 */
        public SortedMultiset<E> forwardMultiset() {
            return ForwardingSortedMultiset.this;
        }
    }

    protected class StandardElementSet extends NavigableElementSet<E> {
        public StandardElementSet() {
            super(ForwardingSortedMultiset.this);
        }
    }

    public abstract SortedMultiset<E> delegate();

    protected ForwardingSortedMultiset() {
    }

    public NavigableSet<E> elementSet() {
        return (NavigableSet) super.elementSet();
    }

    public Comparator<? super E> comparator() {
        return delegate().comparator();
    }

    public SortedMultiset<E> descendingMultiset() {
        return delegate().descendingMultiset();
    }

    public Entry<E> firstEntry() {
        return delegate().firstEntry();
    }

    /* Access modifiers changed, original: protected */
    public Entry<E> standardFirstEntry() {
        Iterator<Entry<E>> entryIterator = entrySet().iterator();
        if (!entryIterator.hasNext()) {
            return null;
        }
        Entry<E> entry = (Entry) entryIterator.next();
        return Multisets.immutableEntry(entry.getElement(), entry.getCount());
    }

    public Entry<E> lastEntry() {
        return delegate().lastEntry();
    }

    /* Access modifiers changed, original: protected */
    public Entry<E> standardLastEntry() {
        Iterator<Entry<E>> entryIterator = descendingMultiset().entrySet().iterator();
        if (!entryIterator.hasNext()) {
            return null;
        }
        Entry<E> entry = (Entry) entryIterator.next();
        return Multisets.immutableEntry(entry.getElement(), entry.getCount());
    }

    public Entry<E> pollFirstEntry() {
        return delegate().pollFirstEntry();
    }

    /* Access modifiers changed, original: protected */
    public Entry<E> standardPollFirstEntry() {
        Iterator<Entry<E>> entryIterator = entrySet().iterator();
        if (!entryIterator.hasNext()) {
            return null;
        }
        Entry<E> entry = (Entry) entryIterator.next();
        entry = Multisets.immutableEntry(entry.getElement(), entry.getCount());
        entryIterator.remove();
        return entry;
    }

    public Entry<E> pollLastEntry() {
        return delegate().pollLastEntry();
    }

    /* Access modifiers changed, original: protected */
    public Entry<E> standardPollLastEntry() {
        Iterator<Entry<E>> entryIterator = descendingMultiset().entrySet().iterator();
        if (!entryIterator.hasNext()) {
            return null;
        }
        Entry<E> entry = (Entry) entryIterator.next();
        entry = Multisets.immutableEntry(entry.getElement(), entry.getCount());
        entryIterator.remove();
        return entry;
    }

    public SortedMultiset<E> headMultiset(E upperBound, BoundType boundType) {
        return delegate().headMultiset(upperBound, boundType);
    }

    public SortedMultiset<E> subMultiset(E lowerBound, BoundType lowerBoundType, E upperBound, BoundType upperBoundType) {
        return delegate().subMultiset(lowerBound, lowerBoundType, upperBound, upperBoundType);
    }

    /* Access modifiers changed, original: protected */
    public SortedMultiset<E> standardSubMultiset(E lowerBound, BoundType lowerBoundType, E upperBound, BoundType upperBoundType) {
        return tailMultiset(lowerBound, lowerBoundType).headMultiset(upperBound, upperBoundType);
    }

    public SortedMultiset<E> tailMultiset(E lowerBound, BoundType boundType) {
        return delegate().tailMultiset(lowerBound, boundType);
    }
}
