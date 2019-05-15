package com.google.common.collect;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

final class Serialization {

    static final class FieldSetter<T> {
        private final Field field;

        private FieldSetter(Field field) {
            this.field = field;
            field.setAccessible(true);
        }

        /* Access modifiers changed, original: 0000 */
        public void set(T instance, Object value) {
            try {
                this.field.set(instance, value);
            } catch (IllegalAccessException impossible) {
                throw new AssertionError(impossible);
            }
        }

        /* Access modifiers changed, original: 0000 */
        public void set(T instance, int value) {
            try {
                this.field.set(instance, Integer.valueOf(value));
            } catch (IllegalAccessException impossible) {
                throw new AssertionError(impossible);
            }
        }
    }

    private Serialization() {
    }

    static int readCount(ObjectInputStream stream) throws IOException {
        return stream.readInt();
    }

    static <K, V> void writeMap(Map<K, V> map, ObjectOutputStream stream) throws IOException {
        stream.writeInt(map.size());
        for (Entry<K, V> entry : map.entrySet()) {
            stream.writeObject(entry.getKey());
            stream.writeObject(entry.getValue());
        }
    }

    static <K, V> void populateMap(Map<K, V> map, ObjectInputStream stream) throws IOException, ClassNotFoundException {
        populateMap(map, stream, stream.readInt());
    }

    static <K, V> void populateMap(Map<K, V> map, ObjectInputStream stream, int size) throws IOException, ClassNotFoundException {
        for (int i = 0; i < size; i++) {
            map.put(stream.readObject(), stream.readObject());
        }
    }

    static <E> void writeMultiset(Multiset<E> multiset, ObjectOutputStream stream) throws IOException {
        stream.writeInt(multiset.entrySet().size());
        for (Multiset.Entry<E> entry : multiset.entrySet()) {
            stream.writeObject(entry.getElement());
            stream.writeInt(entry.getCount());
        }
    }

    static <E> void populateMultiset(Multiset<E> multiset, ObjectInputStream stream) throws IOException, ClassNotFoundException {
        populateMultiset(multiset, stream, stream.readInt());
    }

    static <E> void populateMultiset(Multiset<E> multiset, ObjectInputStream stream, int distinctElements) throws IOException, ClassNotFoundException {
        for (int i = 0; i < distinctElements; i++) {
            multiset.add(stream.readObject(), stream.readInt());
        }
    }

    static <K, V> void writeMultimap(Multimap<K, V> multimap, ObjectOutputStream stream) throws IOException {
        stream.writeInt(multimap.asMap().size());
        for (Entry<K, Collection<V>> entry : multimap.asMap().entrySet()) {
            stream.writeObject(entry.getKey());
            stream.writeInt(((Collection) entry.getValue()).size());
            for (V value : (Collection) entry.getValue()) {
                stream.writeObject(value);
            }
        }
    }

    static <K, V> void populateMultimap(Multimap<K, V> multimap, ObjectInputStream stream) throws IOException, ClassNotFoundException {
        populateMultimap(multimap, stream, stream.readInt());
    }

    static <K, V> void populateMultimap(Multimap<K, V> multimap, ObjectInputStream stream, int distinctKeys) throws IOException, ClassNotFoundException {
        for (int i = 0; i < distinctKeys; i++) {
            Collection<V> values = multimap.get(stream.readObject());
            int valueCount = stream.readInt();
            for (int j = 0; j < valueCount; j++) {
                values.add(stream.readObject());
            }
        }
    }

    static <T> FieldSetter<T> getFieldSetter(Class<T> clazz, String fieldName) {
        try {
            return new FieldSetter(clazz.getDeclaredField(fieldName));
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }
}
