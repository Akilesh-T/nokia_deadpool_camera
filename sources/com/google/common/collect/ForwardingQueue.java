package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import java.util.NoSuchElementException;
import java.util.Queue;

@GwtCompatible
public abstract class ForwardingQueue<E> extends ForwardingCollection<E> implements Queue<E> {
    public abstract Queue<E> delegate();

    protected ForwardingQueue() {
    }

    public boolean offer(E o) {
        return delegate().offer(o);
    }

    public E poll() {
        return delegate().poll();
    }

    public E remove() {
        return delegate().remove();
    }

    public E peek() {
        return delegate().peek();
    }

    public E element() {
        return delegate().element();
    }

    /* Access modifiers changed, original: protected */
    public boolean standardOffer(E e) {
        try {
            return add(e);
        } catch (IllegalStateException e2) {
            return false;
        }
    }

    /* Access modifiers changed, original: protected */
    public E standardPeek() {
        try {
            return element();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    /* Access modifiers changed, original: protected */
    public E standardPoll() {
        try {
            return remove();
        } catch (NoSuchElementException e) {
            return null;
        }
    }
}
