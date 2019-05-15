package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map.Entry;
import javax.annotation.Nullable;

@GwtCompatible(emulated = true, serializable = true)
public class ImmutableListMultimap<K, V> extends ImmutableMultimap<K, V> implements ListMultimap<K, V> {
    @GwtIncompatible("Not needed in emulated source")
    private static final long serialVersionUID = 0;
    private transient ImmutableListMultimap<V, K> inverse;

    public static final class Builder<K, V> extends com.google.common.collect.ImmutableMultimap.Builder<K, V> {
        public Builder<K, V> put(K key, V value) {
            super.put(key, value);
            return this;
        }

        public Builder<K, V> put(Entry<? extends K, ? extends V> entry) {
            super.put(entry);
            return this;
        }

        public Builder<K, V> putAll(K key, Iterable<? extends V> values) {
            super.putAll((Object) key, (Iterable) values);
            return this;
        }

        public Builder<K, V> putAll(K key, V... values) {
            super.putAll((Object) key, (Object[]) values);
            return this;
        }

        public Builder<K, V> putAll(Multimap<? extends K, ? extends V> multimap) {
            super.putAll(multimap);
            return this;
        }

        public Builder<K, V> orderKeysBy(Comparator<? super K> keyComparator) {
            super.orderKeysBy(keyComparator);
            return this;
        }

        public Builder<K, V> orderValuesBy(Comparator<? super V> valueComparator) {
            super.orderValuesBy(valueComparator);
            return this;
        }

        public ImmutableListMultimap<K, V> build() {
            return (ImmutableListMultimap) super.build();
        }
    }

    public static <K, V> ImmutableListMultimap<K, V> of() {
        return EmptyImmutableListMultimap.INSTANCE;
    }

    public static <K, V> ImmutableListMultimap<K, V> of(K k1, V v1) {
        Builder<K, V> builder = builder();
        builder.put((Object) k1, (Object) v1);
        return builder.build();
    }

    public static <K, V> ImmutableListMultimap<K, V> of(K k1, V v1, K k2, V v2) {
        Builder<K, V> builder = builder();
        builder.put((Object) k1, (Object) v1);
        builder.put((Object) k2, (Object) v2);
        return builder.build();
    }

    public static <K, V> ImmutableListMultimap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
        Builder<K, V> builder = builder();
        builder.put((Object) k1, (Object) v1);
        builder.put((Object) k2, (Object) v2);
        builder.put((Object) k3, (Object) v3);
        return builder.build();
    }

    public static <K, V> ImmutableListMultimap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        Builder<K, V> builder = builder();
        builder.put((Object) k1, (Object) v1);
        builder.put((Object) k2, (Object) v2);
        builder.put((Object) k3, (Object) v3);
        builder.put((Object) k4, (Object) v4);
        return builder.build();
    }

    public static <K, V> ImmutableListMultimap<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        Builder<K, V> builder = builder();
        builder.put((Object) k1, (Object) v1);
        builder.put((Object) k2, (Object) v2);
        builder.put((Object) k3, (Object) v3);
        builder.put((Object) k4, (Object) v4);
        builder.put((Object) k5, (Object) v5);
        return builder.build();
    }

    public static <K, V> Builder<K, V> builder() {
        return new Builder();
    }

    public static <K, V> ImmutableListMultimap<K, V> copyOf(Multimap<? extends K, ? extends V> multimap) {
        if (multimap.isEmpty()) {
            return of();
        }
        if (multimap instanceof ImmutableListMultimap) {
            ImmutableListMultimap<K, V> kvMultimap = (ImmutableListMultimap) multimap;
            if (!kvMultimap.isPartialView()) {
                return kvMultimap;
            }
        }
        com.google.common.collect.ImmutableMap.Builder<K, ImmutableList<V>> builder = ImmutableMap.builder();
        int size = 0;
        for (Entry<? extends K, ? extends Collection<? extends V>> entry : multimap.asMap().entrySet()) {
            ImmutableList<V> list = ImmutableList.copyOf((Collection) entry.getValue());
            if (!list.isEmpty()) {
                builder.put(entry.getKey(), list);
                size += list.size();
            }
        }
        return new ImmutableListMultimap(builder.build(), size);
    }

    ImmutableListMultimap(ImmutableMap<K, ImmutableList<V>> map, int size) {
        super(map, size);
    }

    public ImmutableList<V> get(@Nullable K key) {
        ImmutableList<V> list = (ImmutableList) this.map.get(key);
        return list == null ? ImmutableList.of() : list;
    }

    public ImmutableListMultimap<V, K> inverse() {
        ImmutableListMultimap<V, K> result = this.inverse;
        if (result != null) {
            return result;
        }
        ImmutableListMultimap<V, K> invert = invert();
        this.inverse = invert;
        return invert;
    }

    private ImmutableListMultimap<V, K> invert() {
        Builder<V, K> builder = builder();
        UnmodifiableIterator it = entries().iterator();
        while (it.hasNext()) {
            Entry<K, V> entry = (Entry) it.next();
            builder.put(entry.getValue(), entry.getKey());
        }
        ImmutableListMultimap<V, K> invertedMultimap = builder.build();
        invertedMultimap.inverse = this;
        return invertedMultimap;
    }

    @Deprecated
    public ImmutableList<V> removeAll(Object key) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    public ImmutableList<V> replaceValues(K k, Iterable<? extends V> iterable) {
        throw new UnsupportedOperationException();
    }

    @GwtIncompatible("java.io.ObjectOutputStream")
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        Serialization.writeMultimap(this, stream);
    }

    @GwtIncompatible("java.io.ObjectInputStream")
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        int keyCount = stream.readInt();
        if (keyCount >= 0) {
            com.google.common.collect.ImmutableMap.Builder<Object, ImmutableList<Object>> builder = ImmutableMap.builder();
            int tmpSize = 0;
            int i = 0;
            while (i < keyCount) {
                Object key = stream.readObject();
                int valueCount = stream.readInt();
                if (valueCount > 0) {
                    Object[] array = new Object[valueCount];
                    for (int j = 0; j < valueCount; j++) {
                        array[j] = stream.readObject();
                    }
                    builder.put(key, ImmutableList.copyOf(array));
                    tmpSize += valueCount;
                    i++;
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid value count ");
                    stringBuilder.append(valueCount);
                    throw new InvalidObjectException(stringBuilder.toString());
                }
            }
            try {
                FieldSettersHolder.MAP_FIELD_SETTER.set((Object) this, builder.build());
                FieldSettersHolder.SIZE_FIELD_SETTER.set((Object) this, tmpSize);
                return;
            } catch (IllegalArgumentException e) {
                throw ((InvalidObjectException) new InvalidObjectException(e.getMessage()).initCause(e));
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Invalid key count ");
        stringBuilder2.append(keyCount);
        throw new InvalidObjectException(stringBuilder2.toString());
    }
}
