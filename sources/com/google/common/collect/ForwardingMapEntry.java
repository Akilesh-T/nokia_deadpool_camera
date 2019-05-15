package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;
import java.util.Map.Entry;
import javax.annotation.Nullable;

@GwtCompatible
public abstract class ForwardingMapEntry<K, V> extends ForwardingObject implements Entry<K, V> {
    public abstract Entry<K, V> delegate();

    protected ForwardingMapEntry() {
    }

    public K getKey() {
        return delegate().getKey();
    }

    public V getValue() {
        return delegate().getValue();
    }

    public V setValue(V value) {
        return delegate().setValue(value);
    }

    public boolean equals(@Nullable Object object) {
        return delegate().equals(object);
    }

    public int hashCode() {
        return delegate().hashCode();
    }

    /* Access modifiers changed, original: protected */
    public boolean standardEquals(@Nullable Object object) {
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

    /* Access modifiers changed, original: protected */
    public int standardHashCode() {
        K k = getKey();
        V v = getValue();
        int i = 0;
        int hashCode = k == null ? 0 : k.hashCode();
        if (v != null) {
            i = v.hashCode();
        }
        return i ^ hashCode;
    }

    /* Access modifiers changed, original: protected */
    @Beta
    public String standardToString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getKey());
        stringBuilder.append("=");
        stringBuilder.append(getValue());
        return stringBuilder.toString();
    }
}
