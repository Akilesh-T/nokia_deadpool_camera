package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import java.io.Serializable;
import java.util.Map.Entry;
import javax.annotation.Nullable;

@GwtCompatible(emulated = true)
final class ImmutableMapValues<K, V> extends ImmutableCollection<V> {
    private final ImmutableMap<K, V> map;

    @GwtIncompatible("serialization")
    private static class SerializedForm<V> implements Serializable {
        private static final long serialVersionUID = 0;
        final ImmutableMap<?, V> map;

        SerializedForm(ImmutableMap<?, V> map) {
            this.map = map;
        }

        /* Access modifiers changed, original: 0000 */
        public Object readResolve() {
            return this.map.values();
        }
    }

    ImmutableMapValues(ImmutableMap<K, V> map) {
        this.map = map;
    }

    public int size() {
        return this.map.size();
    }

    public UnmodifiableIterator<V> iterator() {
        return Maps.valueIterator(this.map.entrySet().iterator());
    }

    public boolean contains(@Nullable Object object) {
        return object != null && Iterators.contains(iterator(), object);
    }

    /* Access modifiers changed, original: 0000 */
    public boolean isPartialView() {
        return true;
    }

    /* Access modifiers changed, original: 0000 */
    public ImmutableList<V> createAsList() {
        final ImmutableList<Entry<K, V>> entryList = this.map.entrySet().asList();
        return new ImmutableAsList<V>() {
            public V get(int index) {
                return ((Entry) entryList.get(index)).getValue();
            }

            /* Access modifiers changed, original: 0000 */
            public ImmutableCollection<V> delegateCollection() {
                return ImmutableMapValues.this;
            }
        };
    }

    /* Access modifiers changed, original: 0000 */
    @GwtIncompatible("serialization")
    public Object writeReplace() {
        return new SerializedForm(this.map);
    }
}
