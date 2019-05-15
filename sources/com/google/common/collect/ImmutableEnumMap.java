package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map.Entry;
import javax.annotation.Nullable;

@GwtCompatible(emulated = true, serializable = true)
final class ImmutableEnumMap<K extends Enum<K>, V> extends ImmutableMap<K, V> {
    private final transient EnumMap<K, V> delegate;

    private static class EnumSerializedForm<K extends Enum<K>, V> implements Serializable {
        private static final long serialVersionUID = 0;
        final EnumMap<K, V> delegate;

        EnumSerializedForm(EnumMap<K, V> delegate) {
            this.delegate = delegate;
        }

        /* Access modifiers changed, original: 0000 */
        public Object readResolve() {
            return new ImmutableEnumMap(this.delegate, null);
        }
    }

    /* synthetic */ ImmutableEnumMap(EnumMap x0, AnonymousClass1 x1) {
        this(x0);
    }

    static <K extends Enum<K>, V> ImmutableMap<K, V> asImmutable(EnumMap<K, V> map) {
        switch (map.size()) {
            case 0:
                return ImmutableMap.of();
            case 1:
                Entry<K, V> entry = (Entry) Iterables.getOnlyElement(map.entrySet());
                return ImmutableMap.of(entry.getKey(), entry.getValue());
            default:
                return new ImmutableEnumMap(map);
        }
    }

    private ImmutableEnumMap(EnumMap<K, V> delegate) {
        this.delegate = delegate;
        Preconditions.checkArgument(delegate.isEmpty() ^ 1);
    }

    /* Access modifiers changed, original: 0000 */
    public ImmutableSet<K> createKeySet() {
        return new ImmutableSet<K>() {
            public boolean contains(Object object) {
                return ImmutableEnumMap.this.delegate.containsKey(object);
            }

            public int size() {
                return ImmutableEnumMap.this.size();
            }

            public UnmodifiableIterator<K> iterator() {
                return Iterators.unmodifiableIterator(ImmutableEnumMap.this.delegate.keySet().iterator());
            }

            /* Access modifiers changed, original: 0000 */
            public boolean isPartialView() {
                return true;
            }
        };
    }

    public int size() {
        return this.delegate.size();
    }

    public boolean containsKey(@Nullable Object key) {
        return this.delegate.containsKey(key);
    }

    public V get(Object key) {
        return this.delegate.get(key);
    }

    /* Access modifiers changed, original: 0000 */
    public ImmutableSet<Entry<K, V>> createEntrySet() {
        return new ImmutableMapEntrySet<K, V>() {
            /* Access modifiers changed, original: 0000 */
            public ImmutableMap<K, V> map() {
                return ImmutableEnumMap.this;
            }

            public UnmodifiableIterator<Entry<K, V>> iterator() {
                return new UnmodifiableIterator<Entry<K, V>>() {
                    private final Iterator<Entry<K, V>> backingIterator = ImmutableEnumMap.this.delegate.entrySet().iterator();

                    public boolean hasNext() {
                        return this.backingIterator.hasNext();
                    }

                    public Entry<K, V> next() {
                        Entry<K, V> entry = (Entry) this.backingIterator.next();
                        return Maps.immutableEntry(entry.getKey(), entry.getValue());
                    }
                };
            }
        };
    }

    /* Access modifiers changed, original: 0000 */
    public boolean isPartialView() {
        return false;
    }

    /* Access modifiers changed, original: 0000 */
    public Object writeReplace() {
        return new EnumSerializedForm(this.delegate);
    }
}
