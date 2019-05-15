package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;
import java.util.Set;
import javax.annotation.Nullable;

@GwtCompatible(emulated = true, serializable = true)
final class SingletonImmutableSet<E> extends ImmutableSet<E> {
    private transient int cachedHashCode;
    final transient E element;

    SingletonImmutableSet(E element) {
        this.element = Preconditions.checkNotNull(element);
    }

    SingletonImmutableSet(E element, int hashCode) {
        this.element = element;
        this.cachedHashCode = hashCode;
    }

    public int size() {
        return 1;
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean contains(Object target) {
        return this.element.equals(target);
    }

    public UnmodifiableIterator<E> iterator() {
        return Iterators.singletonIterator(this.element);
    }

    /* Access modifiers changed, original: 0000 */
    public boolean isPartialView() {
        return false;
    }

    /* Access modifiers changed, original: 0000 */
    public int copyIntoArray(Object[] dst, int offset) {
        dst[offset] = this.element;
        return offset + 1;
    }

    public boolean equals(@Nullable Object object) {
        boolean z = true;
        if (object == this) {
            return true;
        }
        if (!(object instanceof Set)) {
            return false;
        }
        Set<?> that = (Set) object;
        if (!(that.size() == 1 && this.element.equals(that.iterator().next()))) {
            z = false;
        }
        return z;
    }

    public final int hashCode() {
        int code = this.cachedHashCode;
        if (code != 0) {
            return code;
        }
        int hashCode = this.element.hashCode();
        code = hashCode;
        this.cachedHashCode = hashCode;
        return code;
    }

    /* Access modifiers changed, original: 0000 */
    public boolean isHashCodeFast() {
        return this.cachedHashCode != 0;
    }

    public String toString() {
        String elementToString = this.element.toString();
        StringBuilder stringBuilder = new StringBuilder(elementToString.length() + 2);
        stringBuilder.append('[');
        stringBuilder.append(elementToString);
        stringBuilder.append(']');
        return stringBuilder.toString();
    }
}
