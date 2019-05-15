package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;

@GwtCompatible(emulated = true)
class RegularImmutableAsList<E> extends ImmutableAsList<E> {
    private final ImmutableCollection<E> delegate;
    private final ImmutableList<? extends E> delegateList;

    RegularImmutableAsList(ImmutableCollection<E> delegate, ImmutableList<? extends E> delegateList) {
        this.delegate = delegate;
        this.delegateList = delegateList;
    }

    RegularImmutableAsList(ImmutableCollection<E> delegate, Object[] array) {
        this((ImmutableCollection) delegate, ImmutableList.asImmutableList(array));
    }

    /* Access modifiers changed, original: 0000 */
    public ImmutableCollection<E> delegateCollection() {
        return this.delegate;
    }

    /* Access modifiers changed, original: 0000 */
    public ImmutableList<? extends E> delegateList() {
        return this.delegateList;
    }

    public UnmodifiableListIterator<E> listIterator(int index) {
        return this.delegateList.listIterator(index);
    }

    /* Access modifiers changed, original: 0000 */
    @GwtIncompatible("not present in emulated superclass")
    public int copyIntoArray(Object[] dst, int offset) {
        return this.delegateList.copyIntoArray(dst, offset);
    }

    public E get(int index) {
        return this.delegateList.get(index);
    }
}
