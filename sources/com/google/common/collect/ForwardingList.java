package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import javax.annotation.Nullable;

@GwtCompatible
public abstract class ForwardingList<E> extends ForwardingCollection<E> implements List<E> {
    public abstract List<E> delegate();

    protected ForwardingList() {
    }

    public void add(int index, E element) {
        delegate().add(index, element);
    }

    public boolean addAll(int index, Collection<? extends E> elements) {
        return delegate().addAll(index, elements);
    }

    public E get(int index) {
        return delegate().get(index);
    }

    public int indexOf(Object element) {
        return delegate().indexOf(element);
    }

    public int lastIndexOf(Object element) {
        return delegate().lastIndexOf(element);
    }

    public ListIterator<E> listIterator() {
        return delegate().listIterator();
    }

    public ListIterator<E> listIterator(int index) {
        return delegate().listIterator(index);
    }

    public E remove(int index) {
        return delegate().remove(index);
    }

    public E set(int index, E element) {
        return delegate().set(index, element);
    }

    public List<E> subList(int fromIndex, int toIndex) {
        return delegate().subList(fromIndex, toIndex);
    }

    public boolean equals(@Nullable Object object) {
        return object == this || delegate().equals(object);
    }

    public int hashCode() {
        return delegate().hashCode();
    }

    /* Access modifiers changed, original: protected */
    public boolean standardAdd(E element) {
        add(size(), element);
        return true;
    }

    /* Access modifiers changed, original: protected */
    public boolean standardAddAll(int index, Iterable<? extends E> elements) {
        return Lists.addAllImpl(this, index, elements);
    }

    /* Access modifiers changed, original: protected */
    public int standardIndexOf(@Nullable Object element) {
        return Lists.indexOfImpl(this, element);
    }

    /* Access modifiers changed, original: protected */
    public int standardLastIndexOf(@Nullable Object element) {
        return Lists.lastIndexOfImpl(this, element);
    }

    /* Access modifiers changed, original: protected */
    public Iterator<E> standardIterator() {
        return listIterator();
    }

    /* Access modifiers changed, original: protected */
    public ListIterator<E> standardListIterator() {
        return listIterator(0);
    }

    /* Access modifiers changed, original: protected */
    @Beta
    public ListIterator<E> standardListIterator(int start) {
        return Lists.listIteratorImpl(this, start);
    }

    /* Access modifiers changed, original: protected */
    @Beta
    public List<E> standardSubList(int fromIndex, int toIndex) {
        return Lists.subListImpl(this, fromIndex, toIndex);
    }

    /* Access modifiers changed, original: protected */
    @Beta
    public boolean standardEquals(@Nullable Object object) {
        return Lists.equalsImpl(this, object);
    }

    /* Access modifiers changed, original: protected */
    @Beta
    public int standardHashCode() {
        return Lists.hashCodeImpl(this);
    }
}
