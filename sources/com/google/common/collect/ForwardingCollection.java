package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;
import java.util.Collection;
import java.util.Iterator;
import javax.annotation.Nullable;

@GwtCompatible
public abstract class ForwardingCollection<E> extends ForwardingObject implements Collection<E> {
    public abstract Collection<E> delegate();

    protected ForwardingCollection() {
    }

    public Iterator<E> iterator() {
        return delegate().iterator();
    }

    public int size() {
        return delegate().size();
    }

    public boolean removeAll(Collection<?> collection) {
        return delegate().removeAll(collection);
    }

    public boolean isEmpty() {
        return delegate().isEmpty();
    }

    public boolean contains(Object object) {
        return delegate().contains(object);
    }

    public boolean add(E element) {
        return delegate().add(element);
    }

    public boolean remove(Object object) {
        return delegate().remove(object);
    }

    public boolean containsAll(Collection<?> collection) {
        return delegate().containsAll(collection);
    }

    public boolean addAll(Collection<? extends E> collection) {
        return delegate().addAll(collection);
    }

    public boolean retainAll(Collection<?> collection) {
        return delegate().retainAll(collection);
    }

    public void clear() {
        delegate().clear();
    }

    public Object[] toArray() {
        return delegate().toArray();
    }

    public <T> T[] toArray(T[] array) {
        return delegate().toArray(array);
    }

    /* Access modifiers changed, original: protected */
    public boolean standardContains(@Nullable Object object) {
        return Iterators.contains(iterator(), object);
    }

    /* Access modifiers changed, original: protected */
    public boolean standardContainsAll(Collection<?> collection) {
        return Collections2.containsAllImpl(this, collection);
    }

    /* Access modifiers changed, original: protected */
    public boolean standardAddAll(Collection<? extends E> collection) {
        return Iterators.addAll(this, collection.iterator());
    }

    /* Access modifiers changed, original: protected */
    public boolean standardRemove(@Nullable Object object) {
        Iterator<E> iterator = iterator();
        while (iterator.hasNext()) {
            if (Objects.equal(iterator.next(), object)) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    /* Access modifiers changed, original: protected */
    public boolean standardRemoveAll(Collection<?> collection) {
        return Iterators.removeAll(iterator(), collection);
    }

    /* Access modifiers changed, original: protected */
    public boolean standardRetainAll(Collection<?> collection) {
        return Iterators.retainAll(iterator(), collection);
    }

    /* Access modifiers changed, original: protected */
    public void standardClear() {
        Iterators.clear(iterator());
    }

    /* Access modifiers changed, original: protected */
    public boolean standardIsEmpty() {
        return iterator().hasNext() ^ 1;
    }

    /* Access modifiers changed, original: protected */
    public String standardToString() {
        return Collections2.toStringImpl(this);
    }

    /* Access modifiers changed, original: protected */
    public Object[] standardToArray() {
        return toArray(new Object[size()]);
    }

    /* Access modifiers changed, original: protected */
    public <T> T[] standardToArray(T[] array) {
        return ObjectArrays.toArrayImpl(this, array);
    }
}
