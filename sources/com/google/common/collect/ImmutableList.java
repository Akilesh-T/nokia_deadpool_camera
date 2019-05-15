package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;
import javax.annotation.Nullable;

@GwtCompatible(emulated = true, serializable = true)
public abstract class ImmutableList<E> extends ImmutableCollection<E> implements List<E>, RandomAccess {
    private static final ImmutableList<Object> EMPTY = new RegularImmutableList(ObjectArrays.EMPTY_ARRAY);

    static class SerializedForm implements Serializable {
        private static final long serialVersionUID = 0;
        final Object[] elements;

        SerializedForm(Object[] elements) {
            this.elements = elements;
        }

        /* Access modifiers changed, original: 0000 */
        public Object readResolve() {
            return ImmutableList.copyOf(this.elements);
        }
    }

    public static final class Builder<E> extends ArrayBasedBuilder<E> {
        public Builder() {
            this(4);
        }

        Builder(int capacity) {
            super(capacity);
        }

        public Builder<E> add(E element) {
            super.add((Object) element);
            return this;
        }

        public Builder<E> addAll(Iterable<? extends E> elements) {
            super.addAll(elements);
            return this;
        }

        public Builder<E> add(E... elements) {
            super.add((Object[]) elements);
            return this;
        }

        public Builder<E> addAll(Iterator<? extends E> elements) {
            super.addAll((Iterator) elements);
            return this;
        }

        public ImmutableList<E> build() {
            return ImmutableList.asImmutableList(this.contents, this.size);
        }
    }

    private static class ReverseImmutableList<E> extends ImmutableList<E> {
        private final transient ImmutableList<E> forwardList;

        ReverseImmutableList(ImmutableList<E> backingList) {
            this.forwardList = backingList;
        }

        private int reverseIndex(int index) {
            return (size() - 1) - index;
        }

        private int reversePosition(int index) {
            return size() - index;
        }

        public ImmutableList<E> reverse() {
            return this.forwardList;
        }

        public boolean contains(@Nullable Object object) {
            return this.forwardList.contains(object);
        }

        public int indexOf(@Nullable Object object) {
            int index = this.forwardList.lastIndexOf(object);
            return index >= 0 ? reverseIndex(index) : -1;
        }

        public int lastIndexOf(@Nullable Object object) {
            int index = this.forwardList.indexOf(object);
            return index >= 0 ? reverseIndex(index) : -1;
        }

        public ImmutableList<E> subList(int fromIndex, int toIndex) {
            Preconditions.checkPositionIndexes(fromIndex, toIndex, size());
            return this.forwardList.subList(reversePosition(toIndex), reversePosition(fromIndex)).reverse();
        }

        public E get(int index) {
            Preconditions.checkElementIndex(index, size());
            return this.forwardList.get(reverseIndex(index));
        }

        public int size() {
            return this.forwardList.size();
        }

        /* Access modifiers changed, original: 0000 */
        public boolean isPartialView() {
            return this.forwardList.isPartialView();
        }
    }

    class SubList extends ImmutableList<E> {
        final transient int length;
        final transient int offset;

        SubList(int offset, int length) {
            this.offset = offset;
            this.length = length;
        }

        public int size() {
            return this.length;
        }

        public E get(int index) {
            Preconditions.checkElementIndex(index, this.length);
            return ImmutableList.this.get(this.offset + index);
        }

        public ImmutableList<E> subList(int fromIndex, int toIndex) {
            Preconditions.checkPositionIndexes(fromIndex, toIndex, this.length);
            return ImmutableList.this.subList(this.offset + fromIndex, this.offset + toIndex);
        }

        /* Access modifiers changed, original: 0000 */
        public boolean isPartialView() {
            return true;
        }
    }

    public static <E> ImmutableList<E> of() {
        return EMPTY;
    }

    public static <E> ImmutableList<E> of(E element) {
        return new SingletonImmutableList(element);
    }

    public static <E> ImmutableList<E> of(E e1, E e2) {
        return construct(e1, e2);
    }

    public static <E> ImmutableList<E> of(E e1, E e2, E e3) {
        return construct(e1, e2, e3);
    }

    public static <E> ImmutableList<E> of(E e1, E e2, E e3, E e4) {
        return construct(e1, e2, e3, e4);
    }

    public static <E> ImmutableList<E> of(E e1, E e2, E e3, E e4, E e5) {
        return construct(e1, e2, e3, e4, e5);
    }

    public static <E> ImmutableList<E> of(E e1, E e2, E e3, E e4, E e5, E e6) {
        return construct(e1, e2, e3, e4, e5, e6);
    }

    public static <E> ImmutableList<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
        return construct(e1, e2, e3, e4, e5, e6, e7);
    }

    public static <E> ImmutableList<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
        return construct(e1, e2, e3, e4, e5, e6, e7, e8);
    }

    public static <E> ImmutableList<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
        return construct(e1, e2, e3, e4, e5, e6, e7, e8, e9);
    }

    public static <E> ImmutableList<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
        return construct(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10);
    }

    public static <E> ImmutableList<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10, E e11) {
        return construct(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11);
    }

    public static <E> ImmutableList<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10, E e11, E e12, E... others) {
        Object obj = others;
        Object[] array = new Object[(obj.length + 12)];
        array[0] = e1;
        array[1] = e2;
        array[2] = e3;
        array[3] = e4;
        array[4] = e5;
        array[5] = e6;
        array[6] = e7;
        array[7] = e8;
        array[8] = e9;
        array[9] = e10;
        array[10] = e11;
        array[11] = e12;
        System.arraycopy(obj, 0, array, 12, obj.length);
        return construct(array);
    }

    public static <E> ImmutableList<E> copyOf(Iterable<? extends E> elements) {
        Preconditions.checkNotNull(elements);
        if (elements instanceof Collection) {
            return copyOf((Collection) elements);
        }
        return copyOf(elements.iterator());
    }

    public static <E> ImmutableList<E> copyOf(Collection<? extends E> elements) {
        if (!(elements instanceof ImmutableCollection)) {
            return construct(elements.toArray());
        }
        ImmutableList<E> asImmutableList;
        ImmutableList<E> list = ((ImmutableCollection) elements).asList();
        if (list.isPartialView()) {
            asImmutableList = asImmutableList(list.toArray());
        } else {
            asImmutableList = list;
        }
        return asImmutableList;
    }

    public static <E> ImmutableList<E> copyOf(Iterator<? extends E> elements) {
        if (!elements.hasNext()) {
            return of();
        }
        Object first = elements.next();
        if (elements.hasNext()) {
            return new Builder().add(first).addAll((Iterator) elements).build();
        }
        return of(first);
    }

    public static <E> ImmutableList<E> copyOf(E[] elements) {
        switch (elements.length) {
            case 0:
                return of();
            case 1:
                return new SingletonImmutableList(elements[0]);
            default:
                return new RegularImmutableList(ObjectArrays.checkElementsNotNull((Object[]) elements.clone()));
        }
    }

    private static <E> ImmutableList<E> construct(Object... elements) {
        return asImmutableList(ObjectArrays.checkElementsNotNull(elements));
    }

    static <E> ImmutableList<E> asImmutableList(Object[] elements) {
        return asImmutableList(elements, elements.length);
    }

    static <E> ImmutableList<E> asImmutableList(Object[] elements, int length) {
        switch (length) {
            case 0:
                return of();
            case 1:
                return new SingletonImmutableList(elements[0]);
            default:
                if (length < elements.length) {
                    elements = ObjectArrays.arraysCopyOf(elements, length);
                }
                return new RegularImmutableList(elements);
        }
    }

    ImmutableList() {
    }

    public UnmodifiableIterator<E> iterator() {
        return listIterator();
    }

    public UnmodifiableListIterator<E> listIterator() {
        return listIterator(0);
    }

    public UnmodifiableListIterator<E> listIterator(int index) {
        return new AbstractIndexedListIterator<E>(size(), index) {
            /* Access modifiers changed, original: protected */
            public E get(int index) {
                return ImmutableList.this.get(index);
            }
        };
    }

    public int indexOf(@Nullable Object object) {
        return object == null ? -1 : Lists.indexOfImpl(this, object);
    }

    public int lastIndexOf(@Nullable Object object) {
        return object == null ? -1 : Lists.lastIndexOfImpl(this, object);
    }

    public boolean contains(@Nullable Object object) {
        return indexOf(object) >= 0;
    }

    public ImmutableList<E> subList(int fromIndex, int toIndex) {
        Preconditions.checkPositionIndexes(fromIndex, toIndex, size());
        switch (toIndex - fromIndex) {
            case 0:
                return of();
            case 1:
                return of(get(fromIndex));
            default:
                return subListUnchecked(fromIndex, toIndex);
        }
    }

    /* Access modifiers changed, original: 0000 */
    public ImmutableList<E> subListUnchecked(int fromIndex, int toIndex) {
        return new SubList(fromIndex, toIndex - fromIndex);
    }

    @Deprecated
    public final boolean addAll(int index, Collection<? extends E> collection) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public final E set(int index, E e) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public final void add(int index, E e) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public final E remove(int index) {
        throw new UnsupportedOperationException();
    }

    public final ImmutableList<E> asList() {
        return this;
    }

    /* Access modifiers changed, original: 0000 */
    public int copyIntoArray(Object[] dst, int offset) {
        int size = size();
        for (int i = 0; i < size; i++) {
            dst[offset + i] = get(i);
        }
        return offset + size;
    }

    public ImmutableList<E> reverse() {
        return new ReverseImmutableList(this);
    }

    public boolean equals(@Nullable Object obj) {
        return Lists.equalsImpl(this, obj);
    }

    public int hashCode() {
        int hashCode = 1;
        int n = size();
        for (int i = 0; i < n; i++) {
            hashCode = ~(~((31 * hashCode) + get(i).hashCode()));
        }
        return hashCode;
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Use SerializedForm");
    }

    /* Access modifiers changed, original: 0000 */
    public Object writeReplace() {
        return new SerializedForm(toArray());
    }

    public static <E> Builder<E> builder() {
        return new Builder();
    }
}
