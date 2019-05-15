package com.google.common.collect;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Primitives;
import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;

public final class ImmutableClassToInstanceMap<B> extends ForwardingMap<Class<? extends B>, B> implements ClassToInstanceMap<B>, Serializable {
    private final ImmutableMap<Class<? extends B>, B> delegate;

    public static final class Builder<B> {
        private final com.google.common.collect.ImmutableMap.Builder<Class<? extends B>, B> mapBuilder = ImmutableMap.builder();

        public <T extends B> Builder<B> put(Class<T> key, T value) {
            this.mapBuilder.put(key, value);
            return this;
        }

        public <T extends B> Builder<B> putAll(Map<? extends Class<? extends T>, ? extends T> map) {
            for (Entry<? extends Class<? extends T>, ? extends T> entry : map.entrySet()) {
                Class<? extends T> type = (Class) entry.getKey();
                this.mapBuilder.put(type, cast(type, entry.getValue()));
            }
            return this;
        }

        private static <B, T extends B> T cast(Class<T> type, B value) {
            return Primitives.wrap(type).cast(value);
        }

        public ImmutableClassToInstanceMap<B> build() {
            return new ImmutableClassToInstanceMap(this.mapBuilder.build());
        }
    }

    public static <B> Builder<B> builder() {
        return new Builder();
    }

    public static <B, S extends B> ImmutableClassToInstanceMap<B> copyOf(Map<? extends Class<? extends S>, ? extends S> map) {
        if (map instanceof ImmutableClassToInstanceMap) {
            return (ImmutableClassToInstanceMap) map;
        }
        return new Builder().putAll(map).build();
    }

    private ImmutableClassToInstanceMap(ImmutableMap<Class<? extends B>, B> delegate) {
        this.delegate = delegate;
    }

    /* Access modifiers changed, original: protected */
    public Map<Class<? extends B>, B> delegate() {
        return this.delegate;
    }

    @Nullable
    public <T extends B> T getInstance(Class<T> type) {
        return this.delegate.get(Preconditions.checkNotNull(type));
    }

    @Deprecated
    public <T extends B> T putInstance(Class<T> cls, T t) {
        throw new UnsupportedOperationException();
    }
}
