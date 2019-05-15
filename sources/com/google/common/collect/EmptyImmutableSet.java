package com.google.common.collect;

import com.adobe.xmp.XMPConst;
import com.google.common.annotations.GwtCompatible;
import java.util.Collection;
import java.util.Set;
import javax.annotation.Nullable;

@GwtCompatible(emulated = true, serializable = true)
final class EmptyImmutableSet extends ImmutableSet<Object> {
    static final EmptyImmutableSet INSTANCE = new EmptyImmutableSet();
    private static final long serialVersionUID = 0;

    private EmptyImmutableSet() {
    }

    public int size() {
        return 0;
    }

    public boolean isEmpty() {
        return true;
    }

    public boolean contains(@Nullable Object target) {
        return false;
    }

    public boolean containsAll(Collection<?> targets) {
        return targets.isEmpty();
    }

    public UnmodifiableIterator<Object> iterator() {
        return Iterators.emptyIterator();
    }

    /* Access modifiers changed, original: 0000 */
    public boolean isPartialView() {
        return false;
    }

    /* Access modifiers changed, original: 0000 */
    public int copyIntoArray(Object[] dst, int offset) {
        return offset;
    }

    public ImmutableList<Object> asList() {
        return ImmutableList.of();
    }

    public boolean equals(@Nullable Object object) {
        if (object instanceof Set) {
            return ((Set) object).isEmpty();
        }
        return false;
    }

    public final int hashCode() {
        return 0;
    }

    /* Access modifiers changed, original: 0000 */
    public boolean isHashCodeFast() {
        return true;
    }

    public String toString() {
        return XMPConst.ARRAY_ITEM_NAME;
    }

    /* Access modifiers changed, original: 0000 */
    public Object readResolve() {
        return INSTANCE;
    }
}
