package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nullable;

@GwtCompatible(emulated = true)
abstract class AbstractBiMap<K, V> extends ForwardingMap<K, V> implements BiMap<K, V>, Serializable {
    @GwtIncompatible("Not needed in emulated source.")
    private static final long serialVersionUID = 0;
    private transient Map<K, V> delegate;
    private transient Set<Entry<K, V>> entrySet;
    transient AbstractBiMap<V, K> inverse;
    private transient Set<K> keySet;
    private transient Set<V> valueSet;

    private class EntrySet extends ForwardingSet<Entry<K, V>> {
        final Set<Entry<K, V>> esDelegate;

        private EntrySet() {
            this.esDelegate = AbstractBiMap.this.delegate.entrySet();
        }

        /* Access modifiers changed, original: protected */
        public Set<Entry<K, V>> delegate() {
            return this.esDelegate;
        }

        public void clear() {
            AbstractBiMap.this.clear();
        }

        public boolean remove(Object object) {
            if (!this.esDelegate.contains(object)) {
                return false;
            }
            Entry<?, ?> entry = (Entry) object;
            AbstractBiMap.this.inverse.delegate.remove(entry.getValue());
            this.esDelegate.remove(entry);
            return true;
        }

        public Iterator<Entry<K, V>> iterator() {
            final Iterator<Entry<K, V>> iterator = this.esDelegate.iterator();
            return new Iterator<Entry<K, V>>() {
                Entry<K, V> entry;

                public boolean hasNext() {
                    return iterator.hasNext();
                }

                public Entry<K, V> next() {
                    this.entry = (Entry) iterator.next();
                    final Entry<K, V> finalEntry = this.entry;
                    return new ForwardingMapEntry<K, V>() {
                        /* Access modifiers changed, original: protected */
                        public Entry<K, V> delegate() {
                            return finalEntry;
                        }

                        public V setValue(V value) {
                            Preconditions.checkState(EntrySet.this.contains(this), "entry no longer in map");
                            if (Objects.equal(value, getValue())) {
                                return value;
                            }
                            Preconditions.checkArgument(AbstractBiMap.this.containsValue(value) ^ 1, "value already present: %s", value);
                            V oldValue = finalEntry.setValue(value);
                            Preconditions.checkState(Objects.equal(value, AbstractBiMap.this.get(getKey())), "entry no longer in map");
                            AbstractBiMap.this.updateInverseMap(getKey(), true, oldValue, value);
                            return oldValue;
                        }
                    };
                }

                public void remove() {
                    CollectPreconditions.checkRemove(this.entry != null);
                    V value = this.entry.getValue();
                    iterator.remove();
                    AbstractBiMap.this.removeFromInverseMap(value);
                }
            };
        }

        public Object[] toArray() {
            return standardToArray();
        }

        public <T> T[] toArray(T[] array) {
            return standardToArray(array);
        }

        public boolean contains(Object o) {
            return Maps.containsEntryImpl(delegate(), o);
        }

        public boolean containsAll(Collection<?> c) {
            return standardContainsAll(c);
        }

        public boolean removeAll(Collection<?> c) {
            return standardRemoveAll(c);
        }

        public boolean retainAll(Collection<?> c) {
            return standardRetainAll(c);
        }
    }

    private static class Inverse<K, V> extends AbstractBiMap<K, V> {
        @GwtIncompatible("Not needed in emulated source.")
        private static final long serialVersionUID = 0;

        private Inverse(Map<K, V> backward, AbstractBiMap<V, K> forward) {
            super(backward, forward);
        }

        /* Access modifiers changed, original: 0000 */
        public K checkKey(K key) {
            return this.inverse.checkValue(key);
        }

        /* Access modifiers changed, original: 0000 */
        public V checkValue(V value) {
            return this.inverse.checkKey(value);
        }

        @GwtIncompatible("java.io.ObjectOuputStream")
        private void writeObject(ObjectOutputStream stream) throws IOException {
            stream.defaultWriteObject();
            stream.writeObject(inverse());
        }

        @GwtIncompatible("java.io.ObjectInputStream")
        private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
            stream.defaultReadObject();
            setInverse((AbstractBiMap) stream.readObject());
        }

        /* Access modifiers changed, original: 0000 */
        @GwtIncompatible("Not needed in the emulated source.")
        public Object readResolve() {
            return inverse().inverse();
        }
    }

    private class KeySet extends ForwardingSet<K> {
        private KeySet() {
        }

        /* Access modifiers changed, original: protected */
        public Set<K> delegate() {
            return AbstractBiMap.this.delegate.keySet();
        }

        public void clear() {
            AbstractBiMap.this.clear();
        }

        public boolean remove(Object key) {
            if (!contains(key)) {
                return false;
            }
            AbstractBiMap.this.removeFromBothMaps(key);
            return true;
        }

        public boolean removeAll(Collection<?> keysToRemove) {
            return standardRemoveAll(keysToRemove);
        }

        public boolean retainAll(Collection<?> keysToRetain) {
            return standardRetainAll(keysToRetain);
        }

        public Iterator<K> iterator() {
            return Maps.keyIterator(AbstractBiMap.this.entrySet().iterator());
        }
    }

    private class ValueSet extends ForwardingSet<V> {
        final Set<V> valuesDelegate;

        private ValueSet() {
            this.valuesDelegate = AbstractBiMap.this.inverse.keySet();
        }

        /* Access modifiers changed, original: protected */
        public Set<V> delegate() {
            return this.valuesDelegate;
        }

        public Iterator<V> iterator() {
            return Maps.valueIterator(AbstractBiMap.this.entrySet().iterator());
        }

        public Object[] toArray() {
            return standardToArray();
        }

        public <T> T[] toArray(T[] array) {
            return standardToArray(array);
        }

        public String toString() {
            return standardToString();
        }
    }

    AbstractBiMap(Map<K, V> forward, Map<V, K> backward) {
        setDelegates(forward, backward);
    }

    private AbstractBiMap(Map<K, V> backward, AbstractBiMap<V, K> forward) {
        this.delegate = backward;
        this.inverse = forward;
    }

    /* Access modifiers changed, original: protected */
    public Map<K, V> delegate() {
        return this.delegate;
    }

    /* Access modifiers changed, original: 0000 */
    public K checkKey(@Nullable K key) {
        return key;
    }

    /* Access modifiers changed, original: 0000 */
    public V checkValue(@Nullable V value) {
        return value;
    }

    /* Access modifiers changed, original: 0000 */
    public void setDelegates(Map<K, V> forward, Map<V, K> backward) {
        boolean z = false;
        Preconditions.checkState(this.delegate == null);
        Preconditions.checkState(this.inverse == null);
        Preconditions.checkArgument(forward.isEmpty());
        Preconditions.checkArgument(backward.isEmpty());
        if (forward != backward) {
            z = true;
        }
        Preconditions.checkArgument(z);
        this.delegate = forward;
        this.inverse = new Inverse(backward, this);
    }

    /* Access modifiers changed, original: 0000 */
    public void setInverse(AbstractBiMap<V, K> inverse) {
        this.inverse = inverse;
    }

    public boolean containsValue(@Nullable Object value) {
        return this.inverse.containsKey(value);
    }

    public V put(@Nullable K key, @Nullable V value) {
        return putInBothMaps(key, value, false);
    }

    public V forcePut(@Nullable K key, @Nullable V value) {
        return putInBothMaps(key, value, true);
    }

    private V putInBothMaps(@Nullable K key, @Nullable V value, boolean force) {
        checkKey(key);
        checkValue(value);
        boolean containedKey = containsKey(key);
        if (containedKey && Objects.equal(value, get(key))) {
            return value;
        }
        if (force) {
            inverse().remove(value);
        } else {
            Preconditions.checkArgument(containsValue(value) ^ 1, "value already present: %s", value);
        }
        V oldValue = this.delegate.put(key, value);
        updateInverseMap(key, containedKey, oldValue, value);
        return oldValue;
    }

    private void updateInverseMap(K key, boolean containedKey, V oldValue, V newValue) {
        if (containedKey) {
            removeFromInverseMap(oldValue);
        }
        this.inverse.delegate.put(newValue, key);
    }

    public V remove(@Nullable Object key) {
        return containsKey(key) ? removeFromBothMaps(key) : null;
    }

    private V removeFromBothMaps(Object key) {
        V oldValue = this.delegate.remove(key);
        removeFromInverseMap(oldValue);
        return oldValue;
    }

    private void removeFromInverseMap(V oldValue) {
        this.inverse.delegate.remove(oldValue);
    }

    public void putAll(Map<? extends K, ? extends V> map) {
        for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public void clear() {
        this.delegate.clear();
        this.inverse.delegate.clear();
    }

    public BiMap<V, K> inverse() {
        return this.inverse;
    }

    public Set<K> keySet() {
        Set<K> result = this.keySet;
        if (result != null) {
            return result;
        }
        Set<K> keySet = new KeySet();
        this.keySet = keySet;
        return keySet;
    }

    public Set<V> values() {
        Set<V> result = this.valueSet;
        if (result != null) {
            return result;
        }
        Set<V> valueSet = new ValueSet();
        this.valueSet = valueSet;
        return valueSet;
    }

    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> result = this.entrySet;
        if (result != null) {
            return result;
        }
        Set<Entry<K, V>> entrySet = new EntrySet();
        this.entrySet = entrySet;
        return entrySet;
    }
}
