package com.google.common.reflect;

import com.google.common.annotations.Beta;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

@Beta
public final class ImmutableTypeToInstanceMap<B> extends ForwardingMap<TypeToken<? extends B>, B> implements TypeToInstanceMap<B> {
    private final ImmutableMap<TypeToken<? extends B>, B> delegate;

    @Beta
    public static final class Builder<B> {
        private final com.google.common.collect.ImmutableMap.Builder<TypeToken<? extends B>, B> mapBuilder;

        private Builder() {
            this.mapBuilder = ImmutableMap.builder();
        }

        public <T extends B> Builder<B> put(Class<T> key, T value) {
            this.mapBuilder.put(TypeToken.of((Class) key), value);
            return this;
        }

        public <T extends B> Builder<B> put(TypeToken<T> key, T value) {
            this.mapBuilder.put(key.rejectTypeVariables(), value);
            return this;
        }

        public ImmutableTypeToInstanceMap<B> build() {
            return new ImmutableTypeToInstanceMap(this.mapBuilder.build());
        }
    }

    public static <B> ImmutableTypeToInstanceMap<B> of() {
        return new ImmutableTypeToInstanceMap(ImmutableMap.of());
    }

    public static <B> Builder<B> builder() {
        return new Builder();
    }

    private ImmutableTypeToInstanceMap(ImmutableMap<TypeToken<? extends B>, B> delegate) {
        this.delegate = delegate;
    }

    public <T extends B> T getInstance(TypeToken<T> type) {
        return trustedGet(type.rejectTypeVariables());
    }

    public <T extends B> T putInstance(TypeToken<T> typeToken, T t) {
        throw new UnsupportedOperationException();
    }

    public <T extends B> T getInstance(Class<T> type) {
        return trustedGet(TypeToken.of((Class) type));
    }

    public <T extends B> T putInstance(Class<T> cls, T t) {
        throw new UnsupportedOperationException();
    }

    /* Access modifiers changed, original: protected */
    public Map<TypeToken<? extends B>, B> delegate() {
        return this.delegate;
    }

    private <T extends B> T trustedGet(TypeToken<T> type) {
        return this.delegate.get(type);
    }
}
