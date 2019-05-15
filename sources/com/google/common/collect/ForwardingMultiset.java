package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;
import com.google.common.collect.Multiset.Entry;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.Nullable;

@GwtCompatible
public abstract class ForwardingMultiset<E> extends ForwardingCollection<E> implements Multiset<E> {

    @Beta
    protected class StandardElementSet extends ElementSet<E> {
        /* Access modifiers changed, original: 0000 */
        public Multiset<E> multiset() {
            return ForwardingMultiset.this;
        }
    }

    public abstract Multiset<E> delegate();

    protected ForwardingMultiset() {
    }

    public int count(Object element) {
        return delegate().count(element);
    }

    public int add(E element, int occurrences) {
        return delegate().add(element, occurrences);
    }

    public int remove(Object element, int occurrences) {
        return delegate().remove(element, occurrences);
    }

    public Set<E> elementSet() {
        return delegate().elementSet();
    }

    public Set<Entry<E>> entrySet() {
        return delegate().entrySet();
    }

    public boolean equals(@Nullable Object object) {
        return object == this || delegate().equals(object);
    }

    public int hashCode() {
        return delegate().hashCode();
    }

    public int setCount(E element, int count) {
        return delegate().setCount(element, count);
    }

    public boolean setCount(E element, int oldCount, int newCount) {
        return delegate().setCount(element, oldCount, newCount);
    }

    /* Access modifiers changed, original: protected */
    public boolean standardContains(@Nullable Object object) {
        return count(object) > 0;
    }

    /* Access modifiers changed, original: protected */
    public void standardClear() {
        Iterators.clear(entrySet().iterator());
    }

    /* Access modifiers changed, original: protected */
    @Beta
    public int standardCount(@Nullable Object object) {
        for (Entry<?> entry : entrySet()) {
            if (Objects.equal(entry.getElement(), object)) {
                return entry.getCount();
            }
        }
        return 0;
    }

    /* Access modifiers changed, original: protected */
    public boolean standardAdd(E element) {
        add(element, 1);
        return true;
    }

    /* Access modifiers changed, original: protected */
    @Beta
    public boolean standardAddAll(Collection<? extends E> elementsToAdd) {
        return Multisets.addAllImpl(this, elementsToAdd);
    }

    /* Access modifiers changed, original: protected */
    public boolean standardRemove(Object element) {
        return remove(element, 1) > 0;
    }

    /* Access modifiers changed, original: protected */
    public boolean standardRemoveAll(Collection<?> elementsToRemove) {
        return Multisets.removeAllImpl(this, elementsToRemove);
    }

    /* Access modifiers changed, original: protected */
    public boolean standardRetainAll(Collection<?> elementsToRetain) {
        return Multisets.retainAllImpl(this, elementsToRetain);
    }

    /* Access modifiers changed, original: protected */
    public int standardSetCount(E element, int count) {
        return Multisets.setCountImpl(this, element, count);
    }

    /* Access modifiers changed, original: protected */
    public boolean standardSetCount(E element, int oldCount, int newCount) {
        return Multisets.setCountImpl(this, element, oldCount, newCount);
    }

    /* Access modifiers changed, original: protected */
    public Iterator<E> standardIterator() {
        return Multisets.iteratorImpl(this);
    }

    /* Access modifiers changed, original: protected */
    public int standardSize() {
        return Multisets.sizeImpl(this);
    }

    /* Access modifiers changed, original: protected */
    public boolean standardEquals(@Nullable Object object) {
        return Multisets.equalsImpl(this, object);
    }

    /* Access modifiers changed, original: protected */
    public int standardHashCode() {
        return entrySet().hashCode();
    }

    /* Access modifiers changed, original: protected */
    public String standardToString() {
        return entrySet().toString();
    }
}
