package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import java.io.Serializable;
import java.util.Map.Entry;
import javax.annotation.Nullable;

@GwtCompatible(emulated = true)
final class ImmutableMapKeySet<K, V> extends ImmutableSet<K> {
    private final ImmutableMap<K, V> map;

    @GwtIncompatible("serialization")
    private static class KeySetSerializedForm<K> implements Serializable {
        private static final long serialVersionUID = 0;
        final ImmutableMap<K, ?> map;

        KeySetSerializedForm(ImmutableMap<K, ?> map) {
            this.map = map;
        }

        /* Access modifiers changed, original: 0000 */
        public Object readResolve() {
            return this.map.keySet();
        }
    }

    ImmutableMapKeySet(ImmutableMap<K, V> map) {
        this.map = map;
    }

    public int size() {
        return this.map.size();
    }

    public UnmodifiableIterator<K> iterator() {
        return asList().iterator();
    }

    public boolean contains(@Nullable Object object) {
        return this.map.containsKey(object);
    }

    /* Access modifiers changed, original: 0000 */
    public ImmutableList<K> createAsList() {
        final ImmutableList<Entry<K, V>> entryList = this.map.entrySet().asList();
        return new ImmutableAsList<K>() {
            public K get(int index) {
                return ((Entry) entryList.get(index)).getKey();
            }

            /* Access modifiers changed, original: 0000 */
            public ImmutableCollection<K> delegateCollection() {
                return ImmutableMapKeySet.this;
            }
        };
    }

    /* Access modifiers changed, original: 0000 */
    public boolean isPartialView() {
        return true;
    }

    /* Access modifiers changed, original: 0000 */
    @GwtIncompatible("serialization")
    public Object writeReplace() {
        return new KeySetSerializedForm(this.map);
    }
}
