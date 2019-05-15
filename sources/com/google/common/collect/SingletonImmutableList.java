package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;
import java.util.List;
import javax.annotation.Nullable;

@GwtCompatible(emulated = true, serializable = true)
final class SingletonImmutableList<E> extends ImmutableList<E> {
    final transient E element;

    SingletonImmutableList(E element) {
        this.element = Preconditions.checkNotNull(element);
    }

    public E get(int index) {
        Preconditions.checkElementIndex(index, 1);
        return this.element;
    }

    public int indexOf(@Nullable Object object) {
        return this.element.equals(object) ? 0 : -1;
    }

    public UnmodifiableIterator<E> iterator() {
        return Iterators.singletonIterator(this.element);
    }

    public int lastIndexOf(@Nullable Object object) {
        return indexOf(object);
    }

    public int size() {
        return 1;
    }

    public ImmutableList<E> subList(int fromIndex, int toIndex) {
        Preconditions.checkPositionIndexes(fromIndex, toIndex, 1);
        return fromIndex == toIndex ? ImmutableList.of() : this;
    }

    public ImmutableList<E> reverse() {
        return this;
    }

    public boolean contains(@Nullable Object object) {
        return this.element.equals(object);
    }

    public boolean equals(@Nullable Object object) {
        boolean z = true;
        if (object == this) {
            return true;
        }
        if (!(object instanceof List)) {
            return false;
        }
        List<?> that = (List) object;
        if (!(that.size() == 1 && this.element.equals(that.get(0)))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return 31 + this.element.hashCode();
    }

    public String toString() {
        String elementToString = this.element.toString();
        StringBuilder stringBuilder = new StringBuilder(elementToString.length() + 2);
        stringBuilder.append('[');
        stringBuilder.append(elementToString);
        stringBuilder.append(']');
        return stringBuilder.toString();
    }

    public boolean isEmpty() {
        return false;
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
}
