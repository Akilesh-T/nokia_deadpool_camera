package com.google.common.cache;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import java.util.Map.Entry;
import javax.annotation.Nullable;

@GwtCompatible
@Beta
public final class RemovalNotification<K, V> implements Entry<K, V> {
    private static final long serialVersionUID = 0;
    private final RemovalCause cause;
    @Nullable
    private final K key;
    @Nullable
    private final V value;

    RemovalNotification(@Nullable K key, @Nullable V value, RemovalCause cause) {
        this.key = key;
        this.value = value;
        this.cause = (RemovalCause) Preconditions.checkNotNull(cause);
    }

    public RemovalCause getCause() {
        return this.cause;
    }

    public boolean wasEvicted() {
        return this.cause.wasEvicted();
    }

    @Nullable
    public K getKey() {
        return this.key;
    }

    @Nullable
    public V getValue() {
        return this.value;
    }

    public final V setValue(V v) {
        throw new UnsupportedOperationException();
    }

    public boolean equals(@Nullable Object object) {
        boolean z = false;
        if (!(object instanceof Entry)) {
            return false;
        }
        Entry<?, ?> that = (Entry) object;
        if (Objects.equal(getKey(), that.getKey()) && Objects.equal(getValue(), that.getValue())) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        K k = getKey();
        V v = getValue();
        int i = 0;
        int hashCode = k == null ? 0 : k.hashCode();
        if (v != null) {
            i = v.hashCode();
        }
        return i ^ hashCode;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getKey());
        stringBuilder.append("=");
        stringBuilder.append(getValue());
        return stringBuilder.toString();
    }
}
