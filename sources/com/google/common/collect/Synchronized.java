package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import javax.annotation.Nullable;

@GwtCompatible(emulated = true)
final class Synchronized {

    static class SynchronizedObject implements Serializable {
        @GwtIncompatible("not needed in emulated source")
        private static final long serialVersionUID = 0;
        final Object delegate;
        final Object mutex;

        SynchronizedObject(Object delegate, @Nullable Object mutex) {
            this.delegate = Preconditions.checkNotNull(delegate);
            this.mutex = mutex == null ? this : mutex;
        }

        /* Access modifiers changed, original: 0000 */
        public Object delegate() {
            return this.delegate;
        }

        public String toString() {
            String obj;
            synchronized (this.mutex) {
                obj = this.delegate.toString();
            }
            return obj;
        }

        @GwtIncompatible("java.io.ObjectOutputStream")
        private void writeObject(ObjectOutputStream stream) throws IOException {
            synchronized (this.mutex) {
                stream.defaultWriteObject();
            }
        }
    }

    @VisibleForTesting
    static class SynchronizedCollection<E> extends SynchronizedObject implements Collection<E> {
        private static final long serialVersionUID = 0;

        private SynchronizedCollection(Collection<E> delegate, @Nullable Object mutex) {
            super(delegate, mutex);
        }

        /* Access modifiers changed, original: 0000 */
        public Collection<E> delegate() {
            return (Collection) super.delegate();
        }

        public boolean add(E e) {
            boolean add;
            synchronized (this.mutex) {
                add = delegate().add(e);
            }
            return add;
        }

        public boolean addAll(Collection<? extends E> c) {
            boolean addAll;
            synchronized (this.mutex) {
                addAll = delegate().addAll(c);
            }
            return addAll;
        }

        public void clear() {
            synchronized (this.mutex) {
                delegate().clear();
            }
        }

        public boolean contains(Object o) {
            boolean contains;
            synchronized (this.mutex) {
                contains = delegate().contains(o);
            }
            return contains;
        }

        public boolean containsAll(Collection<?> c) {
            boolean containsAll;
            synchronized (this.mutex) {
                containsAll = delegate().containsAll(c);
            }
            return containsAll;
        }

        public boolean isEmpty() {
            boolean isEmpty;
            synchronized (this.mutex) {
                isEmpty = delegate().isEmpty();
            }
            return isEmpty;
        }

        public Iterator<E> iterator() {
            return delegate().iterator();
        }

        public boolean remove(Object o) {
            boolean remove;
            synchronized (this.mutex) {
                remove = delegate().remove(o);
            }
            return remove;
        }

        public boolean removeAll(Collection<?> c) {
            boolean removeAll;
            synchronized (this.mutex) {
                removeAll = delegate().removeAll(c);
            }
            return removeAll;
        }

        public boolean retainAll(Collection<?> c) {
            boolean retainAll;
            synchronized (this.mutex) {
                retainAll = delegate().retainAll(c);
            }
            return retainAll;
        }

        public int size() {
            int size;
            synchronized (this.mutex) {
                size = delegate().size();
            }
            return size;
        }

        public Object[] toArray() {
            Object[] toArray;
            synchronized (this.mutex) {
                toArray = delegate().toArray();
            }
            return toArray;
        }

        public <T> T[] toArray(T[] a) {
            Object[] toArray;
            synchronized (this.mutex) {
                toArray = delegate().toArray(a);
            }
            return toArray;
        }
    }

    @GwtIncompatible("works but is needed only for NavigableMap")
    private static class SynchronizedEntry<K, V> extends SynchronizedObject implements Entry<K, V> {
        private static final long serialVersionUID = 0;

        SynchronizedEntry(Entry<K, V> delegate, @Nullable Object mutex) {
            super(delegate, mutex);
        }

        /* Access modifiers changed, original: 0000 */
        public Entry<K, V> delegate() {
            return (Entry) super.delegate();
        }

        public boolean equals(Object obj) {
            boolean equals;
            synchronized (this.mutex) {
                equals = delegate().equals(obj);
            }
            return equals;
        }

        public int hashCode() {
            int hashCode;
            synchronized (this.mutex) {
                hashCode = delegate().hashCode();
            }
            return hashCode;
        }

        public K getKey() {
            Object key;
            synchronized (this.mutex) {
                key = delegate().getKey();
            }
            return key;
        }

        public V getValue() {
            Object value;
            synchronized (this.mutex) {
                value = delegate().getValue();
            }
            return value;
        }

        public V setValue(V value) {
            Object value2;
            synchronized (this.mutex) {
                value2 = delegate().setValue(value);
            }
            return value2;
        }
    }

    private static class SynchronizedMap<K, V> extends SynchronizedObject implements Map<K, V> {
        private static final long serialVersionUID = 0;
        transient Set<Entry<K, V>> entrySet;
        transient Set<K> keySet;
        transient Collection<V> values;

        SynchronizedMap(Map<K, V> delegate, @Nullable Object mutex) {
            super(delegate, mutex);
        }

        /* Access modifiers changed, original: 0000 */
        public Map<K, V> delegate() {
            return (Map) super.delegate();
        }

        public void clear() {
            synchronized (this.mutex) {
                delegate().clear();
            }
        }

        public boolean containsKey(Object key) {
            boolean containsKey;
            synchronized (this.mutex) {
                containsKey = delegate().containsKey(key);
            }
            return containsKey;
        }

        public boolean containsValue(Object value) {
            boolean containsValue;
            synchronized (this.mutex) {
                containsValue = delegate().containsValue(value);
            }
            return containsValue;
        }

        public Set<Entry<K, V>> entrySet() {
            Set set;
            synchronized (this.mutex) {
                if (this.entrySet == null) {
                    this.entrySet = Synchronized.set(delegate().entrySet(), this.mutex);
                }
                set = this.entrySet;
            }
            return set;
        }

        public V get(Object key) {
            Object obj;
            synchronized (this.mutex) {
                obj = delegate().get(key);
            }
            return obj;
        }

        public boolean isEmpty() {
            boolean isEmpty;
            synchronized (this.mutex) {
                isEmpty = delegate().isEmpty();
            }
            return isEmpty;
        }

        public Set<K> keySet() {
            Set set;
            synchronized (this.mutex) {
                if (this.keySet == null) {
                    this.keySet = Synchronized.set(delegate().keySet(), this.mutex);
                }
                set = this.keySet;
            }
            return set;
        }

        public V put(K key, V value) {
            Object put;
            synchronized (this.mutex) {
                put = delegate().put(key, value);
            }
            return put;
        }

        public void putAll(Map<? extends K, ? extends V> map) {
            synchronized (this.mutex) {
                delegate().putAll(map);
            }
        }

        public V remove(Object key) {
            Object remove;
            synchronized (this.mutex) {
                remove = delegate().remove(key);
            }
            return remove;
        }

        public int size() {
            int size;
            synchronized (this.mutex) {
                size = delegate().size();
            }
            return size;
        }

        public Collection<V> values() {
            Collection collection;
            synchronized (this.mutex) {
                if (this.values == null) {
                    this.values = Synchronized.collection(delegate().values(), this.mutex);
                }
                collection = this.values;
            }
            return collection;
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            boolean equals;
            synchronized (this.mutex) {
                equals = delegate().equals(o);
            }
            return equals;
        }

        public int hashCode() {
            int hashCode;
            synchronized (this.mutex) {
                hashCode = delegate().hashCode();
            }
            return hashCode;
        }
    }

    private static class SynchronizedMultimap<K, V> extends SynchronizedObject implements Multimap<K, V> {
        private static final long serialVersionUID = 0;
        transient Map<K, Collection<V>> asMap;
        transient Collection<Entry<K, V>> entries;
        transient Set<K> keySet;
        transient Multiset<K> keys;
        transient Collection<V> valuesCollection;

        /* Access modifiers changed, original: 0000 */
        public Multimap<K, V> delegate() {
            return (Multimap) super.delegate();
        }

        SynchronizedMultimap(Multimap<K, V> delegate, @Nullable Object mutex) {
            super(delegate, mutex);
        }

        public int size() {
            int size;
            synchronized (this.mutex) {
                size = delegate().size();
            }
            return size;
        }

        public boolean isEmpty() {
            boolean isEmpty;
            synchronized (this.mutex) {
                isEmpty = delegate().isEmpty();
            }
            return isEmpty;
        }

        public boolean containsKey(Object key) {
            boolean containsKey;
            synchronized (this.mutex) {
                containsKey = delegate().containsKey(key);
            }
            return containsKey;
        }

        public boolean containsValue(Object value) {
            boolean containsValue;
            synchronized (this.mutex) {
                containsValue = delegate().containsValue(value);
            }
            return containsValue;
        }

        public boolean containsEntry(Object key, Object value) {
            boolean containsEntry;
            synchronized (this.mutex) {
                containsEntry = delegate().containsEntry(key, value);
            }
            return containsEntry;
        }

        public Collection<V> get(K key) {
            Collection access$400;
            synchronized (this.mutex) {
                access$400 = Synchronized.typePreservingCollection(delegate().get(key), this.mutex);
            }
            return access$400;
        }

        public boolean put(K key, V value) {
            boolean put;
            synchronized (this.mutex) {
                put = delegate().put(key, value);
            }
            return put;
        }

        public boolean putAll(K key, Iterable<? extends V> values) {
            boolean putAll;
            synchronized (this.mutex) {
                putAll = delegate().putAll(key, values);
            }
            return putAll;
        }

        public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
            boolean putAll;
            synchronized (this.mutex) {
                putAll = delegate().putAll(multimap);
            }
            return putAll;
        }

        public Collection<V> replaceValues(K key, Iterable<? extends V> values) {
            Collection replaceValues;
            synchronized (this.mutex) {
                replaceValues = delegate().replaceValues(key, values);
            }
            return replaceValues;
        }

        public boolean remove(Object key, Object value) {
            boolean remove;
            synchronized (this.mutex) {
                remove = delegate().remove(key, value);
            }
            return remove;
        }

        public Collection<V> removeAll(Object key) {
            Collection removeAll;
            synchronized (this.mutex) {
                removeAll = delegate().removeAll(key);
            }
            return removeAll;
        }

        public void clear() {
            synchronized (this.mutex) {
                delegate().clear();
            }
        }

        public Set<K> keySet() {
            Set set;
            synchronized (this.mutex) {
                if (this.keySet == null) {
                    this.keySet = Synchronized.typePreservingSet(delegate().keySet(), this.mutex);
                }
                set = this.keySet;
            }
            return set;
        }

        public Collection<V> values() {
            Collection collection;
            synchronized (this.mutex) {
                if (this.valuesCollection == null) {
                    this.valuesCollection = Synchronized.collection(delegate().values(), this.mutex);
                }
                collection = this.valuesCollection;
            }
            return collection;
        }

        public Collection<Entry<K, V>> entries() {
            Collection collection;
            synchronized (this.mutex) {
                if (this.entries == null) {
                    this.entries = Synchronized.typePreservingCollection(delegate().entries(), this.mutex);
                }
                collection = this.entries;
            }
            return collection;
        }

        public Map<K, Collection<V>> asMap() {
            Map map;
            synchronized (this.mutex) {
                if (this.asMap == null) {
                    this.asMap = new SynchronizedAsMap(delegate().asMap(), this.mutex);
                }
                map = this.asMap;
            }
            return map;
        }

        public Multiset<K> keys() {
            Multiset multiset;
            synchronized (this.mutex) {
                if (this.keys == null) {
                    this.keys = Synchronized.multiset(delegate().keys(), this.mutex);
                }
                multiset = this.keys;
            }
            return multiset;
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            boolean equals;
            synchronized (this.mutex) {
                equals = delegate().equals(o);
            }
            return equals;
        }

        public int hashCode() {
            int hashCode;
            synchronized (this.mutex) {
                hashCode = delegate().hashCode();
            }
            return hashCode;
        }
    }

    private static class SynchronizedAsMap<K, V> extends SynchronizedMap<K, Collection<V>> {
        private static final long serialVersionUID = 0;
        transient Set<Entry<K, Collection<V>>> asMapEntrySet;
        transient Collection<Collection<V>> asMapValues;

        SynchronizedAsMap(Map<K, Collection<V>> delegate, @Nullable Object mutex) {
            super(delegate, mutex);
        }

        public Collection<V> get(Object key) {
            Collection<V> collection;
            synchronized (this.mutex) {
                Collection<V> collection2 = (Collection) super.get(key);
                if (collection2 == null) {
                    collection = null;
                } else {
                    collection = Synchronized.typePreservingCollection(collection2, this.mutex);
                }
            }
            return collection;
        }

        public Set<Entry<K, Collection<V>>> entrySet() {
            Set set;
            synchronized (this.mutex) {
                if (this.asMapEntrySet == null) {
                    this.asMapEntrySet = new SynchronizedAsMapEntries(delegate().entrySet(), this.mutex);
                }
                set = this.asMapEntrySet;
            }
            return set;
        }

        public Collection<Collection<V>> values() {
            Collection collection;
            synchronized (this.mutex) {
                if (this.asMapValues == null) {
                    this.asMapValues = new SynchronizedAsMapValues(delegate().values(), this.mutex);
                }
                collection = this.asMapValues;
            }
            return collection;
        }

        public boolean containsValue(Object o) {
            return values().contains(o);
        }
    }

    private static class SynchronizedAsMapValues<V> extends SynchronizedCollection<Collection<V>> {
        private static final long serialVersionUID = 0;

        SynchronizedAsMapValues(Collection<Collection<V>> delegate, @Nullable Object mutex) {
            super(delegate, mutex);
        }

        public Iterator<Collection<V>> iterator() {
            final Iterator<Collection<V>> iterator = super.iterator();
            return new ForwardingIterator<Collection<V>>() {
                /* Access modifiers changed, original: protected */
                public Iterator<Collection<V>> delegate() {
                    return iterator;
                }

                public Collection<V> next() {
                    return Synchronized.typePreservingCollection((Collection) super.next(), SynchronizedAsMapValues.this.mutex);
                }
            };
        }
    }

    @VisibleForTesting
    static class SynchronizedBiMap<K, V> extends SynchronizedMap<K, V> implements BiMap<K, V>, Serializable {
        private static final long serialVersionUID = 0;
        private transient BiMap<V, K> inverse;
        private transient Set<V> valueSet;

        private SynchronizedBiMap(BiMap<K, V> delegate, @Nullable Object mutex, @Nullable BiMap<V, K> inverse) {
            super(delegate, mutex);
            this.inverse = inverse;
        }

        /* Access modifiers changed, original: 0000 */
        public BiMap<K, V> delegate() {
            return (BiMap) super.delegate();
        }

        public Set<V> values() {
            Set set;
            synchronized (this.mutex) {
                if (this.valueSet == null) {
                    this.valueSet = Synchronized.set(delegate().values(), this.mutex);
                }
                set = this.valueSet;
            }
            return set;
        }

        public V forcePut(K key, V value) {
            Object forcePut;
            synchronized (this.mutex) {
                forcePut = delegate().forcePut(key, value);
            }
            return forcePut;
        }

        public BiMap<V, K> inverse() {
            BiMap biMap;
            synchronized (this.mutex) {
                if (this.inverse == null) {
                    this.inverse = new SynchronizedBiMap(delegate().inverse(), this.mutex, this);
                }
                biMap = this.inverse;
            }
            return biMap;
        }
    }

    private static class SynchronizedList<E> extends SynchronizedCollection<E> implements List<E> {
        private static final long serialVersionUID = 0;

        SynchronizedList(List<E> delegate, @Nullable Object mutex) {
            super(delegate, mutex);
        }

        /* Access modifiers changed, original: 0000 */
        public List<E> delegate() {
            return (List) super.delegate();
        }

        public void add(int index, E element) {
            synchronized (this.mutex) {
                delegate().add(index, element);
            }
        }

        public boolean addAll(int index, Collection<? extends E> c) {
            boolean addAll;
            synchronized (this.mutex) {
                addAll = delegate().addAll(index, c);
            }
            return addAll;
        }

        public E get(int index) {
            Object obj;
            synchronized (this.mutex) {
                obj = delegate().get(index);
            }
            return obj;
        }

        public int indexOf(Object o) {
            int indexOf;
            synchronized (this.mutex) {
                indexOf = delegate().indexOf(o);
            }
            return indexOf;
        }

        public int lastIndexOf(Object o) {
            int lastIndexOf;
            synchronized (this.mutex) {
                lastIndexOf = delegate().lastIndexOf(o);
            }
            return lastIndexOf;
        }

        public ListIterator<E> listIterator() {
            return delegate().listIterator();
        }

        public ListIterator<E> listIterator(int index) {
            return delegate().listIterator(index);
        }

        public E remove(int index) {
            Object remove;
            synchronized (this.mutex) {
                remove = delegate().remove(index);
            }
            return remove;
        }

        public E set(int index, E element) {
            Object obj;
            synchronized (this.mutex) {
                obj = delegate().set(index, element);
            }
            return obj;
        }

        public List<E> subList(int fromIndex, int toIndex) {
            List access$200;
            synchronized (this.mutex) {
                access$200 = Synchronized.list(delegate().subList(fromIndex, toIndex), this.mutex);
            }
            return access$200;
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            boolean equals;
            synchronized (this.mutex) {
                equals = delegate().equals(o);
            }
            return equals;
        }

        public int hashCode() {
            int hashCode;
            synchronized (this.mutex) {
                hashCode = delegate().hashCode();
            }
            return hashCode;
        }
    }

    private static class SynchronizedListMultimap<K, V> extends SynchronizedMultimap<K, V> implements ListMultimap<K, V> {
        private static final long serialVersionUID = 0;

        SynchronizedListMultimap(ListMultimap<K, V> delegate, @Nullable Object mutex) {
            super(delegate, mutex);
        }

        /* Access modifiers changed, original: 0000 */
        public ListMultimap<K, V> delegate() {
            return (ListMultimap) super.delegate();
        }

        public List<V> get(K key) {
            List access$200;
            synchronized (this.mutex) {
                access$200 = Synchronized.list(delegate().get(key), this.mutex);
            }
            return access$200;
        }

        public List<V> removeAll(Object key) {
            List removeAll;
            synchronized (this.mutex) {
                removeAll = delegate().removeAll(key);
            }
            return removeAll;
        }

        public List<V> replaceValues(K key, Iterable<? extends V> values) {
            List replaceValues;
            synchronized (this.mutex) {
                replaceValues = delegate().replaceValues(key, values);
            }
            return replaceValues;
        }
    }

    private static class SynchronizedMultiset<E> extends SynchronizedCollection<E> implements Multiset<E> {
        private static final long serialVersionUID = 0;
        transient Set<E> elementSet;
        transient Set<Multiset.Entry<E>> entrySet;

        SynchronizedMultiset(Multiset<E> delegate, @Nullable Object mutex) {
            super(delegate, mutex);
        }

        /* Access modifiers changed, original: 0000 */
        public Multiset<E> delegate() {
            return (Multiset) super.delegate();
        }

        public int count(Object o) {
            int count;
            synchronized (this.mutex) {
                count = delegate().count(o);
            }
            return count;
        }

        public int add(E e, int n) {
            int add;
            synchronized (this.mutex) {
                add = delegate().add(e, n);
            }
            return add;
        }

        public int remove(Object o, int n) {
            int remove;
            synchronized (this.mutex) {
                remove = delegate().remove(o, n);
            }
            return remove;
        }

        public int setCount(E element, int count) {
            int count2;
            synchronized (this.mutex) {
                count2 = delegate().setCount(element, count);
            }
            return count2;
        }

        public boolean setCount(E element, int oldCount, int newCount) {
            boolean count;
            synchronized (this.mutex) {
                count = delegate().setCount(element, oldCount, newCount);
            }
            return count;
        }

        public Set<E> elementSet() {
            Set set;
            synchronized (this.mutex) {
                if (this.elementSet == null) {
                    this.elementSet = Synchronized.typePreservingSet(delegate().elementSet(), this.mutex);
                }
                set = this.elementSet;
            }
            return set;
        }

        public Set<Multiset.Entry<E>> entrySet() {
            Set set;
            synchronized (this.mutex) {
                if (this.entrySet == null) {
                    this.entrySet = Synchronized.typePreservingSet(delegate().entrySet(), this.mutex);
                }
                set = this.entrySet;
            }
            return set;
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            boolean equals;
            synchronized (this.mutex) {
                equals = delegate().equals(o);
            }
            return equals;
        }

        public int hashCode() {
            int hashCode;
            synchronized (this.mutex) {
                hashCode = delegate().hashCode();
            }
            return hashCode;
        }
    }

    private static class SynchronizedQueue<E> extends SynchronizedCollection<E> implements Queue<E> {
        private static final long serialVersionUID = 0;

        SynchronizedQueue(Queue<E> delegate, @Nullable Object mutex) {
            super(delegate, mutex);
        }

        /* Access modifiers changed, original: 0000 */
        public Queue<E> delegate() {
            return (Queue) super.delegate();
        }

        public E element() {
            Object element;
            synchronized (this.mutex) {
                element = delegate().element();
            }
            return element;
        }

        public boolean offer(E e) {
            boolean offer;
            synchronized (this.mutex) {
                offer = delegate().offer(e);
            }
            return offer;
        }

        public E peek() {
            Object peek;
            synchronized (this.mutex) {
                peek = delegate().peek();
            }
            return peek;
        }

        public E poll() {
            Object poll;
            synchronized (this.mutex) {
                poll = delegate().poll();
            }
            return poll;
        }

        public E remove() {
            Object remove;
            synchronized (this.mutex) {
                remove = delegate().remove();
            }
            return remove;
        }
    }

    static class SynchronizedSet<E> extends SynchronizedCollection<E> implements Set<E> {
        private static final long serialVersionUID = 0;

        SynchronizedSet(Set<E> delegate, @Nullable Object mutex) {
            super(delegate, mutex);
        }

        /* Access modifiers changed, original: 0000 */
        public Set<E> delegate() {
            return (Set) super.delegate();
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            boolean equals;
            synchronized (this.mutex) {
                equals = delegate().equals(o);
            }
            return equals;
        }

        public int hashCode() {
            int hashCode;
            synchronized (this.mutex) {
                hashCode = delegate().hashCode();
            }
            return hashCode;
        }
    }

    private static class SynchronizedSetMultimap<K, V> extends SynchronizedMultimap<K, V> implements SetMultimap<K, V> {
        private static final long serialVersionUID = 0;
        transient Set<Entry<K, V>> entrySet;

        SynchronizedSetMultimap(SetMultimap<K, V> delegate, @Nullable Object mutex) {
            super(delegate, mutex);
        }

        /* Access modifiers changed, original: 0000 */
        public SetMultimap<K, V> delegate() {
            return (SetMultimap) super.delegate();
        }

        public Set<V> get(K key) {
            Set set;
            synchronized (this.mutex) {
                set = Synchronized.set(delegate().get(key), this.mutex);
            }
            return set;
        }

        public Set<V> removeAll(Object key) {
            Set removeAll;
            synchronized (this.mutex) {
                removeAll = delegate().removeAll(key);
            }
            return removeAll;
        }

        public Set<V> replaceValues(K key, Iterable<? extends V> values) {
            Set replaceValues;
            synchronized (this.mutex) {
                replaceValues = delegate().replaceValues(key, values);
            }
            return replaceValues;
        }

        public Set<Entry<K, V>> entries() {
            Set set;
            synchronized (this.mutex) {
                if (this.entrySet == null) {
                    this.entrySet = Synchronized.set(delegate().entries(), this.mutex);
                }
                set = this.entrySet;
            }
            return set;
        }
    }

    static class SynchronizedSortedMap<K, V> extends SynchronizedMap<K, V> implements SortedMap<K, V> {
        private static final long serialVersionUID = 0;

        SynchronizedSortedMap(SortedMap<K, V> delegate, @Nullable Object mutex) {
            super(delegate, mutex);
        }

        /* Access modifiers changed, original: 0000 */
        public SortedMap<K, V> delegate() {
            return (SortedMap) super.delegate();
        }

        public Comparator<? super K> comparator() {
            Comparator comparator;
            synchronized (this.mutex) {
                comparator = delegate().comparator();
            }
            return comparator;
        }

        public K firstKey() {
            Object firstKey;
            synchronized (this.mutex) {
                firstKey = delegate().firstKey();
            }
            return firstKey;
        }

        public SortedMap<K, V> headMap(K toKey) {
            SortedMap sortedMap;
            synchronized (this.mutex) {
                sortedMap = Synchronized.sortedMap(delegate().headMap(toKey), this.mutex);
            }
            return sortedMap;
        }

        public K lastKey() {
            Object lastKey;
            synchronized (this.mutex) {
                lastKey = delegate().lastKey();
            }
            return lastKey;
        }

        public SortedMap<K, V> subMap(K fromKey, K toKey) {
            SortedMap sortedMap;
            synchronized (this.mutex) {
                sortedMap = Synchronized.sortedMap(delegate().subMap(fromKey, toKey), this.mutex);
            }
            return sortedMap;
        }

        public SortedMap<K, V> tailMap(K fromKey) {
            SortedMap sortedMap;
            synchronized (this.mutex) {
                sortedMap = Synchronized.sortedMap(delegate().tailMap(fromKey), this.mutex);
            }
            return sortedMap;
        }
    }

    private static class SynchronizedAsMapEntries<K, V> extends SynchronizedSet<Entry<K, Collection<V>>> {
        private static final long serialVersionUID = 0;

        SynchronizedAsMapEntries(Set<Entry<K, Collection<V>>> delegate, @Nullable Object mutex) {
            super(delegate, mutex);
        }

        public Iterator<Entry<K, Collection<V>>> iterator() {
            final Iterator<Entry<K, Collection<V>>> iterator = super.iterator();
            return new ForwardingIterator<Entry<K, Collection<V>>>() {
                /* Access modifiers changed, original: protected */
                public Iterator<Entry<K, Collection<V>>> delegate() {
                    return iterator;
                }

                public Entry<K, Collection<V>> next() {
                    final Entry<K, Collection<V>> entry = (Entry) super.next();
                    return new ForwardingMapEntry<K, Collection<V>>() {
                        /* Access modifiers changed, original: protected */
                        public Entry<K, Collection<V>> delegate() {
                            return entry;
                        }

                        public Collection<V> getValue() {
                            return Synchronized.typePreservingCollection((Collection) entry.getValue(), SynchronizedAsMapEntries.this.mutex);
                        }
                    };
                }
            };
        }

        public Object[] toArray() {
            Object[] toArrayImpl;
            synchronized (this.mutex) {
                toArrayImpl = ObjectArrays.toArrayImpl(delegate());
            }
            return toArrayImpl;
        }

        public <T> T[] toArray(T[] array) {
            Object[] toArrayImpl;
            synchronized (this.mutex) {
                toArrayImpl = ObjectArrays.toArrayImpl(delegate(), array);
            }
            return toArrayImpl;
        }

        public boolean contains(Object o) {
            boolean containsEntryImpl;
            synchronized (this.mutex) {
                containsEntryImpl = Maps.containsEntryImpl(delegate(), o);
            }
            return containsEntryImpl;
        }

        public boolean containsAll(Collection<?> c) {
            boolean containsAllImpl;
            synchronized (this.mutex) {
                containsAllImpl = Collections2.containsAllImpl(delegate(), c);
            }
            return containsAllImpl;
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            boolean equalsImpl;
            synchronized (this.mutex) {
                equalsImpl = Sets.equalsImpl(delegate(), o);
            }
            return equalsImpl;
        }

        public boolean remove(Object o) {
            boolean removeEntryImpl;
            synchronized (this.mutex) {
                removeEntryImpl = Maps.removeEntryImpl(delegate(), o);
            }
            return removeEntryImpl;
        }

        public boolean removeAll(Collection<?> c) {
            boolean removeAll;
            synchronized (this.mutex) {
                removeAll = Iterators.removeAll(delegate().iterator(), c);
            }
            return removeAll;
        }

        public boolean retainAll(Collection<?> c) {
            boolean retainAll;
            synchronized (this.mutex) {
                retainAll = Iterators.retainAll(delegate().iterator(), c);
            }
            return retainAll;
        }
    }

    @GwtIncompatible("Deque")
    private static final class SynchronizedDeque<E> extends SynchronizedQueue<E> implements Deque<E> {
        private static final long serialVersionUID = 0;

        SynchronizedDeque(Deque<E> delegate, @Nullable Object mutex) {
            super(delegate, mutex);
        }

        /* Access modifiers changed, original: 0000 */
        public Deque<E> delegate() {
            return (Deque) super.delegate();
        }

        public void addFirst(E e) {
            synchronized (this.mutex) {
                delegate().addFirst(e);
            }
        }

        public void addLast(E e) {
            synchronized (this.mutex) {
                delegate().addLast(e);
            }
        }

        public boolean offerFirst(E e) {
            boolean offerFirst;
            synchronized (this.mutex) {
                offerFirst = delegate().offerFirst(e);
            }
            return offerFirst;
        }

        public boolean offerLast(E e) {
            boolean offerLast;
            synchronized (this.mutex) {
                offerLast = delegate().offerLast(e);
            }
            return offerLast;
        }

        public E removeFirst() {
            Object removeFirst;
            synchronized (this.mutex) {
                removeFirst = delegate().removeFirst();
            }
            return removeFirst;
        }

        public E removeLast() {
            Object removeLast;
            synchronized (this.mutex) {
                removeLast = delegate().removeLast();
            }
            return removeLast;
        }

        public E pollFirst() {
            Object pollFirst;
            synchronized (this.mutex) {
                pollFirst = delegate().pollFirst();
            }
            return pollFirst;
        }

        public E pollLast() {
            Object pollLast;
            synchronized (this.mutex) {
                pollLast = delegate().pollLast();
            }
            return pollLast;
        }

        public E getFirst() {
            Object first;
            synchronized (this.mutex) {
                first = delegate().getFirst();
            }
            return first;
        }

        public E getLast() {
            Object last;
            synchronized (this.mutex) {
                last = delegate().getLast();
            }
            return last;
        }

        public E peekFirst() {
            Object peekFirst;
            synchronized (this.mutex) {
                peekFirst = delegate().peekFirst();
            }
            return peekFirst;
        }

        public E peekLast() {
            Object peekLast;
            synchronized (this.mutex) {
                peekLast = delegate().peekLast();
            }
            return peekLast;
        }

        public boolean removeFirstOccurrence(Object o) {
            boolean removeFirstOccurrence;
            synchronized (this.mutex) {
                removeFirstOccurrence = delegate().removeFirstOccurrence(o);
            }
            return removeFirstOccurrence;
        }

        public boolean removeLastOccurrence(Object o) {
            boolean removeLastOccurrence;
            synchronized (this.mutex) {
                removeLastOccurrence = delegate().removeLastOccurrence(o);
            }
            return removeLastOccurrence;
        }

        public void push(E e) {
            synchronized (this.mutex) {
                delegate().push(e);
            }
        }

        public E pop() {
            Object pop;
            synchronized (this.mutex) {
                pop = delegate().pop();
            }
            return pop;
        }

        public Iterator<E> descendingIterator() {
            Iterator descendingIterator;
            synchronized (this.mutex) {
                descendingIterator = delegate().descendingIterator();
            }
            return descendingIterator;
        }
    }

    @GwtIncompatible("NavigableMap")
    @VisibleForTesting
    static class SynchronizedNavigableMap<K, V> extends SynchronizedSortedMap<K, V> implements NavigableMap<K, V> {
        private static final long serialVersionUID = 0;
        transient NavigableSet<K> descendingKeySet;
        transient NavigableMap<K, V> descendingMap;
        transient NavigableSet<K> navigableKeySet;

        SynchronizedNavigableMap(NavigableMap<K, V> delegate, @Nullable Object mutex) {
            super(delegate, mutex);
        }

        /* Access modifiers changed, original: 0000 */
        public NavigableMap<K, V> delegate() {
            return (NavigableMap) super.delegate();
        }

        public Entry<K, V> ceilingEntry(K key) {
            Entry access$700;
            synchronized (this.mutex) {
                access$700 = Synchronized.nullableSynchronizedEntry(delegate().ceilingEntry(key), this.mutex);
            }
            return access$700;
        }

        public K ceilingKey(K key) {
            Object ceilingKey;
            synchronized (this.mutex) {
                ceilingKey = delegate().ceilingKey(key);
            }
            return ceilingKey;
        }

        public NavigableSet<K> descendingKeySet() {
            synchronized (this.mutex) {
                NavigableSet navigableSet;
                if (this.descendingKeySet == null) {
                    navigableSet = Synchronized.navigableSet(delegate().descendingKeySet(), this.mutex);
                    this.descendingKeySet = navigableSet;
                    return navigableSet;
                }
                navigableSet = this.descendingKeySet;
                return navigableSet;
            }
        }

        public NavigableMap<K, V> descendingMap() {
            synchronized (this.mutex) {
                NavigableMap navigableMap;
                if (this.descendingMap == null) {
                    navigableMap = Synchronized.navigableMap(delegate().descendingMap(), this.mutex);
                    this.descendingMap = navigableMap;
                    return navigableMap;
                }
                navigableMap = this.descendingMap;
                return navigableMap;
            }
        }

        public Entry<K, V> firstEntry() {
            Entry access$700;
            synchronized (this.mutex) {
                access$700 = Synchronized.nullableSynchronizedEntry(delegate().firstEntry(), this.mutex);
            }
            return access$700;
        }

        public Entry<K, V> floorEntry(K key) {
            Entry access$700;
            synchronized (this.mutex) {
                access$700 = Synchronized.nullableSynchronizedEntry(delegate().floorEntry(key), this.mutex);
            }
            return access$700;
        }

        public K floorKey(K key) {
            Object floorKey;
            synchronized (this.mutex) {
                floorKey = delegate().floorKey(key);
            }
            return floorKey;
        }

        public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
            NavigableMap navigableMap;
            synchronized (this.mutex) {
                navigableMap = Synchronized.navigableMap(delegate().headMap(toKey, inclusive), this.mutex);
            }
            return navigableMap;
        }

        public Entry<K, V> higherEntry(K key) {
            Entry access$700;
            synchronized (this.mutex) {
                access$700 = Synchronized.nullableSynchronizedEntry(delegate().higherEntry(key), this.mutex);
            }
            return access$700;
        }

        public K higherKey(K key) {
            Object higherKey;
            synchronized (this.mutex) {
                higherKey = delegate().higherKey(key);
            }
            return higherKey;
        }

        public Entry<K, V> lastEntry() {
            Entry access$700;
            synchronized (this.mutex) {
                access$700 = Synchronized.nullableSynchronizedEntry(delegate().lastEntry(), this.mutex);
            }
            return access$700;
        }

        public Entry<K, V> lowerEntry(K key) {
            Entry access$700;
            synchronized (this.mutex) {
                access$700 = Synchronized.nullableSynchronizedEntry(delegate().lowerEntry(key), this.mutex);
            }
            return access$700;
        }

        public K lowerKey(K key) {
            Object lowerKey;
            synchronized (this.mutex) {
                lowerKey = delegate().lowerKey(key);
            }
            return lowerKey;
        }

        public Set<K> keySet() {
            return navigableKeySet();
        }

        public NavigableSet<K> navigableKeySet() {
            synchronized (this.mutex) {
                NavigableSet navigableSet;
                if (this.navigableKeySet == null) {
                    navigableSet = Synchronized.navigableSet(delegate().navigableKeySet(), this.mutex);
                    this.navigableKeySet = navigableSet;
                    return navigableSet;
                }
                navigableSet = this.navigableKeySet;
                return navigableSet;
            }
        }

        public Entry<K, V> pollFirstEntry() {
            Entry access$700;
            synchronized (this.mutex) {
                access$700 = Synchronized.nullableSynchronizedEntry(delegate().pollFirstEntry(), this.mutex);
            }
            return access$700;
        }

        public Entry<K, V> pollLastEntry() {
            Entry access$700;
            synchronized (this.mutex) {
                access$700 = Synchronized.nullableSynchronizedEntry(delegate().pollLastEntry(), this.mutex);
            }
            return access$700;
        }

        public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
            NavigableMap navigableMap;
            synchronized (this.mutex) {
                navigableMap = Synchronized.navigableMap(delegate().subMap(fromKey, fromInclusive, toKey, toInclusive), this.mutex);
            }
            return navigableMap;
        }

        public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
            NavigableMap navigableMap;
            synchronized (this.mutex) {
                navigableMap = Synchronized.navigableMap(delegate().tailMap(fromKey, inclusive), this.mutex);
            }
            return navigableMap;
        }

        public SortedMap<K, V> headMap(K toKey) {
            return headMap(toKey, false);
        }

        public SortedMap<K, V> subMap(K fromKey, K toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        public SortedMap<K, V> tailMap(K fromKey) {
            return tailMap(fromKey, true);
        }
    }

    private static class SynchronizedRandomAccessList<E> extends SynchronizedList<E> implements RandomAccess {
        private static final long serialVersionUID = 0;

        SynchronizedRandomAccessList(List<E> list, @Nullable Object mutex) {
            super(list, mutex);
        }
    }

    static class SynchronizedSortedSet<E> extends SynchronizedSet<E> implements SortedSet<E> {
        private static final long serialVersionUID = 0;

        SynchronizedSortedSet(SortedSet<E> delegate, @Nullable Object mutex) {
            super(delegate, mutex);
        }

        /* Access modifiers changed, original: 0000 */
        public SortedSet<E> delegate() {
            return (SortedSet) super.delegate();
        }

        public Comparator<? super E> comparator() {
            Comparator comparator;
            synchronized (this.mutex) {
                comparator = delegate().comparator();
            }
            return comparator;
        }

        public SortedSet<E> subSet(E fromElement, E toElement) {
            SortedSet access$100;
            synchronized (this.mutex) {
                access$100 = Synchronized.sortedSet(delegate().subSet(fromElement, toElement), this.mutex);
            }
            return access$100;
        }

        public SortedSet<E> headSet(E toElement) {
            SortedSet access$100;
            synchronized (this.mutex) {
                access$100 = Synchronized.sortedSet(delegate().headSet(toElement), this.mutex);
            }
            return access$100;
        }

        public SortedSet<E> tailSet(E fromElement) {
            SortedSet access$100;
            synchronized (this.mutex) {
                access$100 = Synchronized.sortedSet(delegate().tailSet(fromElement), this.mutex);
            }
            return access$100;
        }

        public E first() {
            Object first;
            synchronized (this.mutex) {
                first = delegate().first();
            }
            return first;
        }

        public E last() {
            Object last;
            synchronized (this.mutex) {
                last = delegate().last();
            }
            return last;
        }
    }

    private static class SynchronizedSortedSetMultimap<K, V> extends SynchronizedSetMultimap<K, V> implements SortedSetMultimap<K, V> {
        private static final long serialVersionUID = 0;

        SynchronizedSortedSetMultimap(SortedSetMultimap<K, V> delegate, @Nullable Object mutex) {
            super(delegate, mutex);
        }

        /* Access modifiers changed, original: 0000 */
        public SortedSetMultimap<K, V> delegate() {
            return (SortedSetMultimap) super.delegate();
        }

        public SortedSet<V> get(K key) {
            SortedSet access$100;
            synchronized (this.mutex) {
                access$100 = Synchronized.sortedSet(delegate().get(key), this.mutex);
            }
            return access$100;
        }

        public SortedSet<V> removeAll(Object key) {
            SortedSet removeAll;
            synchronized (this.mutex) {
                removeAll = delegate().removeAll(key);
            }
            return removeAll;
        }

        public SortedSet<V> replaceValues(K key, Iterable<? extends V> values) {
            SortedSet replaceValues;
            synchronized (this.mutex) {
                replaceValues = delegate().replaceValues(key, values);
            }
            return replaceValues;
        }

        public Comparator<? super V> valueComparator() {
            Comparator valueComparator;
            synchronized (this.mutex) {
                valueComparator = delegate().valueComparator();
            }
            return valueComparator;
        }
    }

    @GwtIncompatible("NavigableSet")
    @VisibleForTesting
    static class SynchronizedNavigableSet<E> extends SynchronizedSortedSet<E> implements NavigableSet<E> {
        private static final long serialVersionUID = 0;
        transient NavigableSet<E> descendingSet;

        SynchronizedNavigableSet(NavigableSet<E> delegate, @Nullable Object mutex) {
            super(delegate, mutex);
        }

        /* Access modifiers changed, original: 0000 */
        public NavigableSet<E> delegate() {
            return (NavigableSet) super.delegate();
        }

        public E ceiling(E e) {
            Object ceiling;
            synchronized (this.mutex) {
                ceiling = delegate().ceiling(e);
            }
            return ceiling;
        }

        public Iterator<E> descendingIterator() {
            return delegate().descendingIterator();
        }

        public NavigableSet<E> descendingSet() {
            synchronized (this.mutex) {
                if (this.descendingSet == null) {
                    NavigableSet<E> dS = Synchronized.navigableSet(delegate().descendingSet(), this.mutex);
                    this.descendingSet = dS;
                    return dS;
                }
                NavigableSet navigableSet = this.descendingSet;
                return navigableSet;
            }
        }

        public E floor(E e) {
            Object floor;
            synchronized (this.mutex) {
                floor = delegate().floor(e);
            }
            return floor;
        }

        public NavigableSet<E> headSet(E toElement, boolean inclusive) {
            NavigableSet navigableSet;
            synchronized (this.mutex) {
                navigableSet = Synchronized.navigableSet(delegate().headSet(toElement, inclusive), this.mutex);
            }
            return navigableSet;
        }

        public E higher(E e) {
            Object higher;
            synchronized (this.mutex) {
                higher = delegate().higher(e);
            }
            return higher;
        }

        public E lower(E e) {
            Object lower;
            synchronized (this.mutex) {
                lower = delegate().lower(e);
            }
            return lower;
        }

        public E pollFirst() {
            Object pollFirst;
            synchronized (this.mutex) {
                pollFirst = delegate().pollFirst();
            }
            return pollFirst;
        }

        public E pollLast() {
            Object pollLast;
            synchronized (this.mutex) {
                pollLast = delegate().pollLast();
            }
            return pollLast;
        }

        public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
            NavigableSet navigableSet;
            synchronized (this.mutex) {
                navigableSet = Synchronized.navigableSet(delegate().subSet(fromElement, fromInclusive, toElement, toInclusive), this.mutex);
            }
            return navigableSet;
        }

        public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            NavigableSet navigableSet;
            synchronized (this.mutex) {
                navigableSet = Synchronized.navigableSet(delegate().tailSet(fromElement, inclusive), this.mutex);
            }
            return navigableSet;
        }

        public SortedSet<E> headSet(E toElement) {
            return headSet(toElement, false);
        }

        public SortedSet<E> subSet(E fromElement, E toElement) {
            return subSet(fromElement, true, toElement, false);
        }

        public SortedSet<E> tailSet(E fromElement) {
            return tailSet(fromElement, true);
        }
    }

    private Synchronized() {
    }

    private static <E> Collection<E> collection(Collection<E> collection, @Nullable Object mutex) {
        return new SynchronizedCollection(collection, mutex);
    }

    @VisibleForTesting
    static <E> Set<E> set(Set<E> set, @Nullable Object mutex) {
        return new SynchronizedSet(set, mutex);
    }

    private static <E> SortedSet<E> sortedSet(SortedSet<E> set, @Nullable Object mutex) {
        return new SynchronizedSortedSet(set, mutex);
    }

    private static <E> List<E> list(List<E> list, @Nullable Object mutex) {
        if (list instanceof RandomAccess) {
            return new SynchronizedRandomAccessList(list, mutex);
        }
        return new SynchronizedList(list, mutex);
    }

    static <E> Multiset<E> multiset(Multiset<E> multiset, @Nullable Object mutex) {
        if ((multiset instanceof SynchronizedMultiset) || (multiset instanceof ImmutableMultiset)) {
            return multiset;
        }
        return new SynchronizedMultiset(multiset, mutex);
    }

    static <K, V> Multimap<K, V> multimap(Multimap<K, V> multimap, @Nullable Object mutex) {
        if ((multimap instanceof SynchronizedMultimap) || (multimap instanceof ImmutableMultimap)) {
            return multimap;
        }
        return new SynchronizedMultimap(multimap, mutex);
    }

    static <K, V> ListMultimap<K, V> listMultimap(ListMultimap<K, V> multimap, @Nullable Object mutex) {
        if ((multimap instanceof SynchronizedListMultimap) || (multimap instanceof ImmutableListMultimap)) {
            return multimap;
        }
        return new SynchronizedListMultimap(multimap, mutex);
    }

    static <K, V> SetMultimap<K, V> setMultimap(SetMultimap<K, V> multimap, @Nullable Object mutex) {
        if ((multimap instanceof SynchronizedSetMultimap) || (multimap instanceof ImmutableSetMultimap)) {
            return multimap;
        }
        return new SynchronizedSetMultimap(multimap, mutex);
    }

    static <K, V> SortedSetMultimap<K, V> sortedSetMultimap(SortedSetMultimap<K, V> multimap, @Nullable Object mutex) {
        if (multimap instanceof SynchronizedSortedSetMultimap) {
            return multimap;
        }
        return new SynchronizedSortedSetMultimap(multimap, mutex);
    }

    private static <E> Collection<E> typePreservingCollection(Collection<E> collection, @Nullable Object mutex) {
        if (collection instanceof SortedSet) {
            return sortedSet((SortedSet) collection, mutex);
        }
        if (collection instanceof Set) {
            return set((Set) collection, mutex);
        }
        if (collection instanceof List) {
            return list((List) collection, mutex);
        }
        return collection(collection, mutex);
    }

    private static <E> Set<E> typePreservingSet(Set<E> set, @Nullable Object mutex) {
        if (set instanceof SortedSet) {
            return sortedSet((SortedSet) set, mutex);
        }
        return set(set, mutex);
    }

    @VisibleForTesting
    static <K, V> Map<K, V> map(Map<K, V> map, @Nullable Object mutex) {
        return new SynchronizedMap(map, mutex);
    }

    static <K, V> SortedMap<K, V> sortedMap(SortedMap<K, V> sortedMap, @Nullable Object mutex) {
        return new SynchronizedSortedMap(sortedMap, mutex);
    }

    static <K, V> BiMap<K, V> biMap(BiMap<K, V> bimap, @Nullable Object mutex) {
        if ((bimap instanceof SynchronizedBiMap) || (bimap instanceof ImmutableBiMap)) {
            return bimap;
        }
        return new SynchronizedBiMap(bimap, mutex, null);
    }

    @GwtIncompatible("NavigableSet")
    static <E> NavigableSet<E> navigableSet(NavigableSet<E> navigableSet, @Nullable Object mutex) {
        return new SynchronizedNavigableSet(navigableSet, mutex);
    }

    @GwtIncompatible("NavigableSet")
    static <E> NavigableSet<E> navigableSet(NavigableSet<E> navigableSet) {
        return navigableSet(navigableSet, null);
    }

    @GwtIncompatible("NavigableMap")
    static <K, V> NavigableMap<K, V> navigableMap(NavigableMap<K, V> navigableMap) {
        return navigableMap(navigableMap, null);
    }

    @GwtIncompatible("NavigableMap")
    static <K, V> NavigableMap<K, V> navigableMap(NavigableMap<K, V> navigableMap, @Nullable Object mutex) {
        return new SynchronizedNavigableMap(navigableMap, mutex);
    }

    @GwtIncompatible("works but is needed only for NavigableMap")
    private static <K, V> Entry<K, V> nullableSynchronizedEntry(@Nullable Entry<K, V> entry, @Nullable Object mutex) {
        if (entry == null) {
            return null;
        }
        return new SynchronizedEntry(entry, mutex);
    }

    static <E> Queue<E> queue(Queue<E> queue, @Nullable Object mutex) {
        if (queue instanceof SynchronizedQueue) {
            return queue;
        }
        return new SynchronizedQueue(queue, mutex);
    }

    @GwtIncompatible("Deque")
    static <E> Deque<E> deque(Deque<E> deque, @Nullable Object mutex) {
        return new SynchronizedDeque(deque, mutex);
    }
}
