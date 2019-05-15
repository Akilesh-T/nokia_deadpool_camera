package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import java.io.Serializable;
import java.util.Map.Entry;
import javax.annotation.Nullable;

@GwtCompatible(emulated = true)
abstract class ImmutableMapEntrySet<K, V> extends ImmutableSet<Entry<K, V>> {

    @GwtIncompatible("serialization")
    private static class EntrySetSerializedForm<K, V> implements Serializable {
        private static final long serialVersionUID = 0;
        final ImmutableMap<K, V> map;

        EntrySetSerializedForm(ImmutableMap<K, V> map) {
            this.map = map;
        }

        /* Access modifiers changed, original: 0000 */
        public Object readResolve() {
            return this.map.entrySet();
        }
    }

    public abstract ImmutableMap<K, V> map();

    ImmutableMapEntrySet() {
    }

    public int size() {
        return map().size();
    }

    public boolean contains(@Nullable Object object) {
        boolean z = false;
        if (!(object instanceof Entry)) {
            return false;
        }
        Entry<?, ?> entry = (Entry) object;
        V value = map().get(entry.getKey());
        if (value != null && value.equals(entry.getValue())) {
            z = true;
        }
        return z;
    }

    /* Access modifiers changed, original: 0000 */
    public boolean isPartialView() {
        return map().isPartialView();
    }

    /* Access modifiers changed, original: 0000 */
    @GwtIncompatible("serialization")
    public Object writeReplace() {
        return new EntrySetSerializedForm(map());
    }
}
