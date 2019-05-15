package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Queue;

@GwtIncompatible("java.util.ArrayDeque")
@Beta
public final class EvictingQueue<E> extends ForwardingQueue<E> implements Serializable {
    private static final long serialVersionUID = 0;
    private final Queue<E> delegate;
    @VisibleForTesting
    final int maxSize;

    private EvictingQueue(int maxSize) {
        Preconditions.checkArgument(maxSize >= 0, "maxSize (%s) must >= 0", Integer.valueOf(maxSize));
        this.delegate = new ArrayDeque(maxSize);
        this.maxSize = maxSize;
    }

    public static <E> EvictingQueue<E> create(int maxSize) {
        return new EvictingQueue(maxSize);
    }

    public int remainingCapacity() {
        return this.maxSize - size();
    }

    /* Access modifiers changed, original: protected */
    public Queue<E> delegate() {
        return this.delegate;
    }

    public boolean offer(E e) {
        return add(e);
    }

    public boolean add(E e) {
        Preconditions.checkNotNull(e);
        if (this.maxSize == 0) {
            return true;
        }
        if (size() == this.maxSize) {
            this.delegate.remove();
        }
        this.delegate.add(e);
        return true;
    }

    public boolean addAll(Collection<? extends E> collection) {
        return standardAddAll(collection);
    }

    public boolean contains(Object object) {
        return delegate().contains(Preconditions.checkNotNull(object));
    }

    public boolean remove(Object object) {
        return delegate().remove(Preconditions.checkNotNull(object));
    }
}
