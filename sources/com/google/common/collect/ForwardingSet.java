package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.Set;
import javax.annotation.Nullable;

@GwtCompatible
public abstract class ForwardingSet<E> extends ForwardingCollection<E> implements Set<E> {
    public abstract Set<E> delegate();

    protected ForwardingSet() {
    }

    public boolean equals(@Nullable Object object) {
        return object == this || delegate().equals(object);
    }

    public int hashCode() {
        return delegate().hashCode();
    }

    /* Access modifiers changed, original: protected */
    public boolean standardRemoveAll(Collection<?> collection) {
        return Sets.removeAllImpl((Set) this, (Collection) Preconditions.checkNotNull(collection));
    }

    /* Access modifiers changed, original: protected */
    public boolean standardEquals(@Nullable Object object) {
        return Sets.equalsImpl(this, object);
    }

    /* Access modifiers changed, original: protected */
    public int standardHashCode() {
        return Sets.hashCodeImpl(this);
    }
}
