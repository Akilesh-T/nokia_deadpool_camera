package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.AbstractSequentialList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.annotation.Nullable;

@GwtCompatible(emulated = true, serializable = true)
public class LinkedListMultimap<K, V> extends AbstractMultimap<K, V> implements ListMultimap<K, V>, Serializable {
    @GwtIncompatible("java serialization not supported")
    private static final long serialVersionUID = 0;
    private transient Node<K, V> head;
    private transient Map<K, KeyList<K, V>> keyToKeyList;
    private transient int modCount;
    private transient int size;
    private transient Node<K, V> tail;

    private class DistinctKeyIterator implements Iterator<K> {
        Node<K, V> current;
        int expectedModCount;
        Node<K, V> next;
        final Set<K> seenKeys;

        private DistinctKeyIterator() {
            this.seenKeys = Sets.newHashSetWithExpectedSize(LinkedListMultimap.this.keySet().size());
            this.next = LinkedListMultimap.this.head;
            this.expectedModCount = LinkedListMultimap.this.modCount;
        }

        /* synthetic */ DistinctKeyIterator(LinkedListMultimap x0, AnonymousClass1 x1) {
            this();
        }

        private void checkForConcurrentModification() {
            if (LinkedListMultimap.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }

        public boolean hasNext() {
            checkForConcurrentModification();
            return this.next != null;
        }

        public K next() {
            checkForConcurrentModification();
            LinkedListMultimap.checkElement(this.next);
            this.current = this.next;
            this.seenKeys.add(this.current.key);
            do {
                this.next = this.next.next;
                if (this.next == null) {
                    break;
                }
            } while (!this.seenKeys.add(this.next.key));
            return this.current.key;
        }

        public void remove() {
            checkForConcurrentModification();
            CollectPreconditions.checkRemove(this.current != null);
            LinkedListMultimap.this.removeAllNodes(this.current.key);
            this.current = null;
            this.expectedModCount = LinkedListMultimap.this.modCount;
        }
    }

    private static class KeyList<K, V> {
        int count = 1;
        Node<K, V> head;
        Node<K, V> tail;

        KeyList(Node<K, V> firstNode) {
            this.head = firstNode;
            this.tail = firstNode;
        }
    }

    private class NodeIterator implements ListIterator<Entry<K, V>> {
        Node<K, V> current;
        int expectedModCount = LinkedListMultimap.this.modCount;
        Node<K, V> next;
        int nextIndex;
        Node<K, V> previous;

        NodeIterator(int index) {
            int size = LinkedListMultimap.this.size();
            Preconditions.checkPositionIndex(index, size);
            int index2;
            if (index < size / 2) {
                this.next = LinkedListMultimap.this.head;
                while (true) {
                    index2 = index - 1;
                    if (index <= 0) {
                        break;
                    }
                    next();
                    index = index2;
                }
            } else {
                this.previous = LinkedListMultimap.this.tail;
                this.nextIndex = size;
                while (true) {
                    index2 = index + 1;
                    if (index >= size) {
                        break;
                    }
                    previous();
                    index = index2;
                }
            }
            this.current = null;
        }

        private void checkForConcurrentModification() {
            if (LinkedListMultimap.this.modCount != this.expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }

        public boolean hasNext() {
            checkForConcurrentModification();
            return this.next != null;
        }

        public Node<K, V> next() {
            checkForConcurrentModification();
            LinkedListMultimap.checkElement(this.next);
            Node node = this.next;
            this.current = node;
            this.previous = node;
            this.next = this.next.next;
            this.nextIndex++;
            return this.current;
        }

        public void remove() {
            checkForConcurrentModification();
            CollectPreconditions.checkRemove(this.current != null);
            if (this.current != this.next) {
                this.previous = this.current.previous;
                this.nextIndex--;
            } else {
                this.next = this.current.next;
            }
            LinkedListMultimap.this.removeNode(this.current);
            this.current = null;
            this.expectedModCount = LinkedListMultimap.this.modCount;
        }

        public boolean hasPrevious() {
            checkForConcurrentModification();
            return this.previous != null;
        }

        public Node<K, V> previous() {
            checkForConcurrentModification();
            LinkedListMultimap.checkElement(this.previous);
            Node node = this.previous;
            this.current = node;
            this.next = node;
            this.previous = this.previous.previous;
            this.nextIndex--;
            return this.current;
        }

        public int nextIndex() {
            return this.nextIndex;
        }

        public int previousIndex() {
            return this.nextIndex - 1;
        }

        public void set(Entry<K, V> entry) {
            throw new UnsupportedOperationException();
        }

        public void add(Entry<K, V> entry) {
            throw new UnsupportedOperationException();
        }

        /* Access modifiers changed, original: 0000 */
        public void setValue(V value) {
            Preconditions.checkState(this.current != null);
            this.current.value = value;
        }
    }

    private class ValueForKeyIterator implements ListIterator<V> {
        Node<K, V> current;
        final Object key;
        Node<K, V> next;
        int nextIndex;
        Node<K, V> previous;

        ValueForKeyIterator(@Nullable Object key) {
            this.key = key;
            KeyList<K, V> keyList = (KeyList) LinkedListMultimap.this.keyToKeyList.get(key);
            this.next = keyList == null ? null : keyList.head;
        }

        public ValueForKeyIterator(@Nullable Object key, int index) {
            KeyList<K, V> keyList = (KeyList) LinkedListMultimap.this.keyToKeyList.get(key);
            int size = keyList == null ? 0 : keyList.count;
            Preconditions.checkPositionIndex(index, size);
            int index2;
            if (index < size / 2) {
                this.next = keyList == null ? null : keyList.head;
                while (true) {
                    index2 = index - 1;
                    if (index <= 0) {
                        break;
                    }
                    next();
                    index = index2;
                }
            } else {
                this.previous = keyList == null ? null : keyList.tail;
                this.nextIndex = size;
                while (true) {
                    index2 = index + 1;
                    if (index >= size) {
                        break;
                    }
                    previous();
                    index = index2;
                }
            }
            this.key = key;
            this.current = null;
        }

        public boolean hasNext() {
            return this.next != null;
        }

        public V next() {
            LinkedListMultimap.checkElement(this.next);
            Node node = this.next;
            this.current = node;
            this.previous = node;
            this.next = this.next.nextSibling;
            this.nextIndex++;
            return this.current.value;
        }

        public boolean hasPrevious() {
            return this.previous != null;
        }

        public V previous() {
            LinkedListMultimap.checkElement(this.previous);
            Node node = this.previous;
            this.current = node;
            this.next = node;
            this.previous = this.previous.previousSibling;
            this.nextIndex--;
            return this.current.value;
        }

        public int nextIndex() {
            return this.nextIndex;
        }

        public int previousIndex() {
            return this.nextIndex - 1;
        }

        public void remove() {
            CollectPreconditions.checkRemove(this.current != null);
            if (this.current != this.next) {
                this.previous = this.current.previousSibling;
                this.nextIndex--;
            } else {
                this.next = this.current.nextSibling;
            }
            LinkedListMultimap.this.removeNode(this.current);
            this.current = null;
        }

        public void set(V value) {
            Preconditions.checkState(this.current != null);
            this.current.value = value;
        }

        public void add(V value) {
            this.previous = LinkedListMultimap.this.addNode(this.key, value, this.next);
            this.nextIndex++;
            this.current = null;
        }
    }

    private static final class Node<K, V> extends AbstractMapEntry<K, V> {
        final K key;
        Node<K, V> next;
        Node<K, V> nextSibling = null;
        Node<K, V> previous;
        Node<K, V> previousSibling = null;
        V value;

        Node(@Nullable K key, @Nullable V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return this.key;
        }

        public V getValue() {
            return this.value;
        }

        public V setValue(@Nullable V newValue) {
            V result = this.value;
            this.value = newValue;
            return result;
        }
    }

    public /* bridge */ /* synthetic */ Map asMap() {
        return super.asMap();
    }

    public /* bridge */ /* synthetic */ boolean containsEntry(@Nullable Object obj, @Nullable Object obj2) {
        return super.containsEntry(obj, obj2);
    }

    public /* bridge */ /* synthetic */ boolean equals(@Nullable Object obj) {
        return super.equals(obj);
    }

    public /* bridge */ /* synthetic */ int hashCode() {
        return super.hashCode();
    }

    public /* bridge */ /* synthetic */ Set keySet() {
        return super.keySet();
    }

    public /* bridge */ /* synthetic */ Multiset keys() {
        return super.keys();
    }

    public /* bridge */ /* synthetic */ boolean putAll(Multimap multimap) {
        return super.putAll(multimap);
    }

    public /* bridge */ /* synthetic */ boolean putAll(@Nullable Object obj, Iterable iterable) {
        return super.putAll(obj, iterable);
    }

    public /* bridge */ /* synthetic */ boolean remove(@Nullable Object obj, @Nullable Object obj2) {
        return super.remove(obj, obj2);
    }

    public /* bridge */ /* synthetic */ String toString() {
        return super.toString();
    }

    public static <K, V> LinkedListMultimap<K, V> create() {
        return new LinkedListMultimap();
    }

    public static <K, V> LinkedListMultimap<K, V> create(int expectedKeys) {
        return new LinkedListMultimap(expectedKeys);
    }

    public static <K, V> LinkedListMultimap<K, V> create(Multimap<? extends K, ? extends V> multimap) {
        return new LinkedListMultimap((Multimap) multimap);
    }

    LinkedListMultimap() {
        this.keyToKeyList = Maps.newHashMap();
    }

    private LinkedListMultimap(int expectedKeys) {
        this.keyToKeyList = new HashMap(expectedKeys);
    }

    private LinkedListMultimap(Multimap<? extends K, ? extends V> multimap) {
        this(multimap.keySet().size());
        putAll(multimap);
    }

    private Node<K, V> addNode(@Nullable K key, @Nullable V value, @Nullable Node<K, V> nextSibling) {
        Node<K, V> node = new Node(key, value);
        KeyList<K, V> keyList;
        if (this.head == null) {
            this.tail = node;
            this.head = node;
            this.keyToKeyList.put(key, new KeyList(node));
            this.modCount++;
        } else if (nextSibling == null) {
            this.tail.next = node;
            node.previous = this.tail;
            this.tail = node;
            keyList = (KeyList) this.keyToKeyList.get(key);
            if (keyList == null) {
                Map map = this.keyToKeyList;
                KeyList<K, V> keyList2 = new KeyList(node);
                keyList = keyList2;
                map.put(key, keyList2);
                this.modCount++;
            } else {
                keyList.count++;
                Node<K, V> keyTail = keyList.tail;
                keyTail.nextSibling = node;
                node.previousSibling = keyTail;
                keyList.tail = node;
            }
        } else {
            keyList = (KeyList) this.keyToKeyList.get(key);
            keyList.count++;
            node.previous = nextSibling.previous;
            node.previousSibling = nextSibling.previousSibling;
            node.next = nextSibling;
            node.nextSibling = nextSibling;
            if (nextSibling.previousSibling == null) {
                ((KeyList) this.keyToKeyList.get(key)).head = node;
            } else {
                nextSibling.previousSibling.nextSibling = node;
            }
            if (nextSibling.previous == null) {
                this.head = node;
            } else {
                nextSibling.previous.next = node;
            }
            nextSibling.previous = node;
            nextSibling.previousSibling = node;
        }
        this.size++;
        return node;
    }

    private void removeNode(Node<K, V> node) {
        if (node.previous != null) {
            node.previous.next = node.next;
        } else {
            this.head = node.next;
        }
        if (node.next != null) {
            node.next.previous = node.previous;
        } else {
            this.tail = node.previous;
        }
        if (node.previousSibling == null && node.nextSibling == null) {
            ((KeyList) this.keyToKeyList.remove(node.key)).count = 0;
            this.modCount++;
        } else {
            KeyList<K, V> keyList = (KeyList) this.keyToKeyList.get(node.key);
            keyList.count--;
            if (node.previousSibling == null) {
                keyList.head = node.nextSibling;
            } else {
                node.previousSibling.nextSibling = node.nextSibling;
            }
            if (node.nextSibling == null) {
                keyList.tail = node.previousSibling;
            } else {
                node.nextSibling.previousSibling = node.previousSibling;
            }
        }
        this.size--;
    }

    private void removeAllNodes(@Nullable Object key) {
        Iterators.clear(new ValueForKeyIterator(key));
    }

    private static void checkElement(@Nullable Object node) {
        if (node == null) {
            throw new NoSuchElementException();
        }
    }

    public int size() {
        return this.size;
    }

    public boolean isEmpty() {
        return this.head == null;
    }

    public boolean containsKey(@Nullable Object key) {
        return this.keyToKeyList.containsKey(key);
    }

    public boolean containsValue(@Nullable Object value) {
        return values().contains(value);
    }

    public boolean put(@Nullable K key, @Nullable V value) {
        addNode(key, value, null);
        return true;
    }

    public List<V> replaceValues(@Nullable K key, Iterable<? extends V> values) {
        List<V> oldValues = getCopy(key);
        ListIterator<V> keyValues = new ValueForKeyIterator(key);
        Iterator<? extends V> newValues = values.iterator();
        while (keyValues.hasNext() && newValues.hasNext()) {
            keyValues.next();
            keyValues.set(newValues.next());
        }
        while (keyValues.hasNext()) {
            keyValues.next();
            keyValues.remove();
        }
        while (newValues.hasNext()) {
            keyValues.add(newValues.next());
        }
        return oldValues;
    }

    private List<V> getCopy(@Nullable Object key) {
        return Collections.unmodifiableList(Lists.newArrayList(new ValueForKeyIterator(key)));
    }

    public List<V> removeAll(@Nullable Object key) {
        List<V> oldValues = getCopy(key);
        removeAllNodes(key);
        return oldValues;
    }

    public void clear() {
        this.head = null;
        this.tail = null;
        this.keyToKeyList.clear();
        this.size = 0;
        this.modCount++;
    }

    public List<V> get(@Nullable final K key) {
        return new AbstractSequentialList<V>() {
            public int size() {
                KeyList<K, V> keyList = (KeyList) LinkedListMultimap.this.keyToKeyList.get(key);
                return keyList == null ? 0 : keyList.count;
            }

            public ListIterator<V> listIterator(int index) {
                return new ValueForKeyIterator(key, index);
            }
        };
    }

    /* Access modifiers changed, original: 0000 */
    public Set<K> createKeySet() {
        return new ImprovedAbstractSet<K>() {
            public int size() {
                return LinkedListMultimap.this.keyToKeyList.size();
            }

            public Iterator<K> iterator() {
                return new DistinctKeyIterator(LinkedListMultimap.this, null);
            }

            public boolean contains(Object key) {
                return LinkedListMultimap.this.containsKey(key);
            }

            public boolean remove(Object o) {
                return LinkedListMultimap.this.removeAll(o).isEmpty() ^ 1;
            }
        };
    }

    public List<V> values() {
        return (List) super.values();
    }

    /* Access modifiers changed, original: 0000 */
    public List<V> createValues() {
        return new AbstractSequentialList<V>() {
            public int size() {
                return LinkedListMultimap.this.size;
            }

            public ListIterator<V> listIterator(int index) {
                final NodeIterator nodeItr = new NodeIterator(index);
                return new TransformedListIterator<Entry<K, V>, V>(nodeItr) {
                    /* Access modifiers changed, original: 0000 */
                    public V transform(Entry<K, V> entry) {
                        return entry.getValue();
                    }

                    public void set(V value) {
                        nodeItr.setValue(value);
                    }
                };
            }
        };
    }

    public List<Entry<K, V>> entries() {
        return (List) super.entries();
    }

    /* Access modifiers changed, original: 0000 */
    public List<Entry<K, V>> createEntries() {
        return new AbstractSequentialList<Entry<K, V>>() {
            public int size() {
                return LinkedListMultimap.this.size;
            }

            public ListIterator<Entry<K, V>> listIterator(int index) {
                return new NodeIterator(index);
            }
        };
    }

    /* Access modifiers changed, original: 0000 */
    public Iterator<Entry<K, V>> entryIterator() {
        throw new AssertionError("should never be called");
    }

    /* Access modifiers changed, original: 0000 */
    public Map<K, Collection<V>> createAsMap() {
        return new AsMap(this);
    }

    @GwtIncompatible("java.io.ObjectOutputStream")
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeInt(size());
        for (Entry<K, V> entry : entries()) {
            stream.writeObject(entry.getKey());
            stream.writeObject(entry.getValue());
        }
    }

    @GwtIncompatible("java.io.ObjectInputStream")
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        this.keyToKeyList = Maps.newLinkedHashMap();
        int size = stream.readInt();
        for (int i = 0; i < size; i++) {
            put(stream.readObject(), stream.readObject());
        }
    }
}
