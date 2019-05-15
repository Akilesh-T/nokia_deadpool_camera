package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.Nullable;

@GwtCompatible(emulated = true, serializable = true)
public abstract class ImmutableSet<E> extends ImmutableCollection<E> implements Set<E> {
    private static final int CUTOFF = 751619276;
    private static final double DESIRED_LOAD_FACTOR = 0.7d;
    static final int MAX_TABLE_SIZE = 1073741824;

    private static class SerializedForm implements Serializable {
        private static final long serialVersionUID = 0;
        final Object[] elements;

        SerializedForm(Object[] elements) {
            this.elements = elements;
        }

        /* Access modifiers changed, original: 0000 */
        public Object readResolve() {
            return ImmutableSet.copyOf(this.elements);
        }
    }

    public static class Builder<E> extends ArrayBasedBuilder<E> {
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

        public Builder<E> add(E... elements) {
            super.add((Object[]) elements);
            return this;
        }

        public Builder<E> addAll(Iterable<? extends E> elements) {
            super.addAll(elements);
            return this;
        }

        public Builder<E> addAll(Iterator<? extends E> elements) {
            super.addAll((Iterator) elements);
            return this;
        }

        public ImmutableSet<E> build() {
            ImmutableSet<E> result = ImmutableSet.construct(this.size, this.contents);
            this.size = result.size();
            return result;
        }
    }

    public abstract UnmodifiableIterator<E> iterator();

    public static <E> ImmutableSet<E> of() {
        return EmptyImmutableSet.INSTANCE;
    }

    public static <E> ImmutableSet<E> of(E element) {
        return new SingletonImmutableSet(element);
    }

    public static <E> ImmutableSet<E> of(E e1, E e2) {
        return construct(2, e1, e2);
    }

    public static <E> ImmutableSet<E> of(E e1, E e2, E e3) {
        return construct(3, e1, e2, e3);
    }

    public static <E> ImmutableSet<E> of(E e1, E e2, E e3, E e4) {
        return construct(4, e1, e2, e3, e4);
    }

    public static <E> ImmutableSet<E> of(E e1, E e2, E e3, E e4, E e5) {
        return construct(5, e1, e2, e3, e4, e5);
    }

    public static <E> ImmutableSet<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E... others) {
        Object[] elements = new Object[(others.length + 6)];
        elements[0] = e1;
        elements[1] = e2;
        elements[2] = e3;
        elements[3] = e4;
        elements[4] = e5;
        elements[5] = e6;
        System.arraycopy(others, 0, elements, 6, others.length);
        return construct(elements.length, elements);
    }

    private static <E> ImmutableSet<E> construct(int n, Object... elements) {
        switch (n) {
            case 0:
                return of();
            case 1:
                return of(elements[0]);
            default:
                int tableSize = chooseTableSize(n);
                Object[] table = new Object[tableSize];
                int mask = tableSize - 1;
                int uniques = 0;
                int hashCode = 0;
                for (int i = 0; i < n; i++) {
                    Object element = ObjectArrays.checkElementNotNull(elements[i], i);
                    int hash = element.hashCode();
                    int j = Hashing.smear(hash);
                    while (true) {
                        int index = j & mask;
                        Object value = table[index];
                        if (value == null) {
                            int uniques2 = uniques + 1;
                            elements[uniques] = element;
                            table[index] = element;
                            hashCode += hash;
                            uniques = uniques2;
                        } else if (!value.equals(element)) {
                            j++;
                        }
                    }
                }
                Arrays.fill(elements, uniques, n, null);
                if (uniques == 1) {
                    return new SingletonImmutableSet(elements[0], hashCode);
                }
                if (tableSize != chooseTableSize(uniques)) {
                    return construct(uniques, elements);
                }
                Object[] uniqueElements;
                if (uniques < elements.length) {
                    uniqueElements = ObjectArrays.arraysCopyOf(elements, uniques);
                } else {
                    uniqueElements = elements;
                }
                return new RegularImmutableSet(uniqueElements, hashCode, table, mask);
        }
    }

    @VisibleForTesting
    static int chooseTableSize(int setSize) {
        boolean z = true;
        if (setSize < CUTOFF) {
            int tableSize = Integer.highestOneBit(setSize - 1) << 1;
            while (((double) tableSize) * DESIRED_LOAD_FACTOR < ((double) setSize)) {
                tableSize <<= 1;
            }
            return tableSize;
        }
        if (setSize >= 1073741824) {
            z = false;
        }
        Preconditions.checkArgument(z, "collection too large");
        return 1073741824;
    }

    public static <E> ImmutableSet<E> copyOf(E[] elements) {
        switch (elements.length) {
            case 0:
                return of();
            case 1:
                return of(elements[0]);
            default:
                return construct(elements.length, (Object[]) elements.clone());
        }
    }

    public static <E> ImmutableSet<E> copyOf(Iterable<? extends E> elements) {
        if (elements instanceof Collection) {
            return copyOf((Collection) elements);
        }
        return copyOf(elements.iterator());
    }

    public static <E> ImmutableSet<E> copyOf(Iterator<? extends E> elements) {
        if (!elements.hasNext()) {
            return of();
        }
        Object first = elements.next();
        if (elements.hasNext()) {
            return new Builder().add(first).addAll((Iterator) elements).build();
        }
        return of(first);
    }

    public static <E> ImmutableSet<E> copyOf(Collection<? extends E> elements) {
        if ((elements instanceof ImmutableSet) && !(elements instanceof ImmutableSortedSet)) {
            ImmutableSet<E> set = (ImmutableSet) elements;
            if (!set.isPartialView()) {
                return set;
            }
        } else if (elements instanceof EnumSet) {
            return copyOfEnumSet((EnumSet) elements);
        }
        Object[] array = elements.toArray();
        return construct(array.length, array);
    }

    private static <E extends Enum<E>> ImmutableSet<E> copyOfEnumSet(EnumSet<E> enumSet) {
        return ImmutableEnumSet.asImmutable(EnumSet.copyOf(enumSet));
    }

    ImmutableSet() {
    }

    /* Access modifiers changed, original: 0000 */
    public boolean isHashCodeFast() {
        return false;
    }

    public boolean equals(@Nullable Object object) {
        if (object == this) {
            return true;
        }
        if ((object instanceof ImmutableSet) && isHashCodeFast() && ((ImmutableSet) object).isHashCodeFast() && hashCode() != object.hashCode()) {
            return false;
        }
        return Sets.equalsImpl(this, object);
    }

    public int hashCode() {
        return Sets.hashCodeImpl(this);
    }

    /* Access modifiers changed, original: 0000 */
    public Object writeReplace() {
        return new SerializedForm(toArray());
    }

    public static <E> Builder<E> builder() {
        return new Builder();
    }
}
