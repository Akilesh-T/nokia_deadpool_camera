package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.collect.Multiset.Entry;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;

@GwtCompatible(emulated = true)
abstract class DescendingMultiset<E> extends ForwardingMultiset<E> implements SortedMultiset<E> {
    private transient Comparator<? super E> comparator;
    private transient NavigableSet<E> elementSet;
    private transient Set<Entry<E>> entrySet;

    public abstract Iterator<Entry<E>> entryIterator();

    public abstract SortedMultiset<E> forwardMultiset();

    DescendingMultiset() {
    }

    public Comparator<? super E> comparator() {
        Comparator<? super E> result = this.comparator;
        if (result != null) {
            return result;
        }
        Ordering reverse = Ordering.from(forwardMultiset().comparator()).reverse();
        this.comparator = reverse;
        return reverse;
    }

    public NavigableSet<E> elementSet() {
        NavigableSet<E> result = this.elementSet;
        if (result != null) {
            return result;
        }
        NavigableElementSet navigableElementSet = new NavigableElementSet(this);
        this.elementSet = navigableElementSet;
        return navigableElementSet;
    }

    public Entry<E> pollFirstEntry() {
        return forwardMultiset().pollLastEntry();
    }

    public Entry<E> pollLastEntry() {
        return forwardMultiset().pollFirstEntry();
    }

    public SortedMultiset<E> headMultiset(E toElement, BoundType boundType) {
        return forwardMultiset().tailMultiset(toElement, boundType).descendingMultiset();
    }

    public SortedMultiset<E> subMultiset(E fromElement, BoundType fromBoundType, E toElement, BoundType toBoundType) {
        return forwardMultiset().subMultiset(toElement, toBoundType, fromElement, fromBoundType).descendingMultiset();
    }

    public SortedMultiset<E> tailMultiset(E fromElement, BoundType boundType) {
        return forwardMultiset().headMultiset(fromElement, boundType).descendingMultiset();
    }

    /* Access modifiers changed, original: protected */
    public Multiset<E> delegate() {
        return forwardMultiset();
    }

    public SortedMultiset<E> descendingMultiset() {
        return forwardMultiset();
    }

    public Entry<E> firstEntry() {
        return forwardMultiset().lastEntry();
    }

    public Entry<E> lastEntry() {
        return forwardMultiset().firstEntry();
    }

    public Set<Entry<E>> entrySet() {
        Set<Entry<E>> result = this.entrySet;
        if (result != null) {
            return result;
        }
        Set<Entry<E>> createEntrySet = createEntrySet();
        this.entrySet = createEntrySet;
        return createEntrySet;
    }

    /* Access modifiers changed, original: 0000 */
    public Set<Entry<E>> createEntrySet() {
        return new EntrySet<E>() {
            /* Access modifiers changed, original: 0000 */
            public Multiset<E> multiset() {
                return DescendingMultiset.this;
            }

            public Iterator<Entry<E>> iterator() {
                return DescendingMultiset.this.entryIterator();
            }

            public int size() {
                return DescendingMultiset.this.forwardMultiset().entrySet().size();
            }
        };
    }

    public Iterator<E> iterator() {
        return Multisets.iteratorImpl(this);
    }

    public Object[] toArray() {
        return standardToArray();
    }

    public <T> T[] toArray(T[] array) {
        return standardToArray(array);
    }

    public String toString() {
        return entrySet().toString();
    }
}
