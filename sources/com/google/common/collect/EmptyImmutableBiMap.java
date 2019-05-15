package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import java.util.Map.Entry;
import javax.annotation.Nullable;

@GwtCompatible(emulated = true)
final class EmptyImmutableBiMap extends ImmutableBiMap<Object, Object> {
    static final EmptyImmutableBiMap INSTANCE = new EmptyImmutableBiMap();

    private EmptyImmutableBiMap() {
    }

    public ImmutableBiMap<Object, Object> inverse() {
        return this;
    }

    public int size() {
        return 0;
    }

    public boolean isEmpty() {
        return true;
    }

    public Object get(@Nullable Object key) {
        return null;
    }

    public ImmutableSet<Entry<Object, Object>> entrySet() {
        return ImmutableSet.of();
    }

    /* Access modifiers changed, original: 0000 */
    public ImmutableSet<Entry<Object, Object>> createEntrySet() {
        throw new AssertionError("should never be called");
    }

    public ImmutableSetMultimap<Object, Object> asMultimap() {
        return ImmutableSetMultimap.of();
    }

    public ImmutableSet<Object> keySet() {
        return ImmutableSet.of();
    }

    /* Access modifiers changed, original: 0000 */
    public boolean isPartialView() {
        return false;
    }

    /* Access modifiers changed, original: 0000 */
    public Object readResolve() {
        return INSTANCE;
    }
}
