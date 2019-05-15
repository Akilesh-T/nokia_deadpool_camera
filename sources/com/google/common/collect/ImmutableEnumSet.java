package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import java.io.Serializable;
import java.util.Collection;
import java.util.EnumSet;

@GwtCompatible(emulated = true, serializable = true)
final class ImmutableEnumSet<E extends Enum<E>> extends ImmutableSet<E> {
    private final transient EnumSet<E> delegate;
    private transient int hashCode;

    private static class EnumSerializedForm<E extends Enum<E>> implements Serializable {
        private static final long serialVersionUID = 0;
        final EnumSet<E> delegate;

        EnumSerializedForm(EnumSet<E> delegate) {
            this.delegate = delegate;
        }

        /* Access modifiers changed, original: 0000 */
        public Object readResolve() {
            return new ImmutableEnumSet(this.delegate.clone());
        }
    }

    static <E extends Enum<E>> ImmutableSet<E> asImmutable(EnumSet<E> set) {
        switch (set.size()) {
            case 0:
                return ImmutableSet.of();
            case 1:
                return ImmutableSet.of(Iterables.getOnlyElement(set));
            default:
                return new ImmutableEnumSet(set);
        }
    }

    private ImmutableEnumSet(EnumSet<E> delegate) {
        this.delegate = delegate;
    }

    /* Access modifiers changed, original: 0000 */
    public boolean isPartialView() {
        return false;
    }

    public UnmodifiableIterator<E> iterator() {
        return Iterators.unmodifiableIterator(this.delegate.iterator());
    }

    public int size() {
        return this.delegate.size();
    }

    public boolean contains(Object object) {
        return this.delegate.contains(object);
    }

    public boolean containsAll(Collection<?> collection) {
        return this.delegate.containsAll(collection);
    }

    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    public boolean equals(Object object) {
        return object == this || this.delegate.equals(object);
    }

    public int hashCode() {
        int result = this.hashCode;
        if (result != 0) {
            return result;
        }
        int hashCode = this.delegate.hashCode();
        this.hashCode = hashCode;
        return hashCode;
    }

    public String toString() {
        return this.delegate.toString();
    }

    /* Access modifiers changed, original: 0000 */
    public Object writeReplace() {
        return new EnumSerializedForm(this.delegate);
    }
}
