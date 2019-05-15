package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;
import com.google.common.collect.Multiset.Entry;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.Nullable;

@GwtCompatible
abstract class AbstractMultiset<E> extends AbstractCollection<E> implements Multiset<E> {
    private transient Set<E> elementSet;
    private transient Set<Entry<E>> entrySet;

    class ElementSet extends ElementSet<E> {
        ElementSet() {
        }

        /* Access modifiers changed, original: 0000 */
        public Multiset<E> multiset() {
            return AbstractMultiset.this;
        }
    }

    class EntrySet extends EntrySet<E> {
        EntrySet() {
        }

        /* Access modifiers changed, original: 0000 */
        public Multiset<E> multiset() {
            return AbstractMultiset.this;
        }

        public Iterator<Entry<E>> iterator() {
            return AbstractMultiset.this.entryIterator();
        }

        public int size() {
            return AbstractMultiset.this.distinctElements();
        }
    }

    public abstract int distinctElements();

    public abstract Iterator<Entry<E>> entryIterator();

    AbstractMultiset() {
    }

    public int size() {
        return Multisets.sizeImpl(this);
    }

    public boolean isEmpty() {
        return entrySet().isEmpty();
    }

    public boolean contains(@Nullable Object element) {
        return count(element) > 0;
    }

    public Iterator<E> iterator() {
        return Multisets.iteratorImpl(this);
    }

    public int count(@Nullable Object element) {
        for (Entry<E> entry : entrySet()) {
            if (Objects.equal(entry.getElement(), element)) {
                return entry.getCount();
            }
        }
        return 0;
    }

    public boolean add(@Nullable E element) {
        add(element, 1);
        return true;
    }

    public int add(@Nullable E e, int occurrences) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(@Nullable Object element) {
        return remove(element, 1) > 0;
    }

    public int remove(@Nullable Object element, int occurrences) {
        throw new UnsupportedOperationException();
    }

    public int setCount(@Nullable E element, int count) {
        return Multisets.setCountImpl(this, element, count);
    }

    public boolean setCount(@Nullable E element, int oldCount, int newCount) {
        return Multisets.setCountImpl(this, element, oldCount, newCount);
    }

    public boolean addAll(Collection<? extends E> elementsToAdd) {
        return Multisets.addAllImpl(this, elementsToAdd);
    }

    public boolean removeAll(Collection<?> elementsToRemove) {
        return Multisets.removeAllImpl(this, elementsToRemove);
    }

    public boolean retainAll(Collection<?> elementsToRetain) {
        return Multisets.retainAllImpl(this, elementsToRetain);
    }

    public void clear() {
        Iterators.clear(entryIterator());
    }

    public Set<E> elementSet() {
        Set<E> result = this.elementSet;
        if (result != null) {
            return result;
        }
        Set<E> createElementSet = createElementSet();
        result = createElementSet;
        this.elementSet = createElementSet;
        return result;
    }

    /* Access modifiers changed, original: 0000 */
    public Set<E> createElementSet() {
        return new ElementSet();
    }

    public Set<Entry<E>> entrySet() {
        Set<Entry<E>> result = this.entrySet;
        if (result != null) {
            return result;
        }
        Set<Entry<E>> createEntrySet = createEntrySet();
        result = createEntrySet;
        this.entrySet = createEntrySet;
        return result;
    }

    /* Access modifiers changed, original: 0000 */
    public Set<Entry<E>> createEntrySet() {
        return new EntrySet();
    }

    public boolean equals(@Nullable Object object) {
        return Multisets.equalsImpl(this, object);
    }

    public int hashCode() {
        return entrySet().hashCode();
    }

    public String toString() {
        return entrySet().toString();
    }
}
