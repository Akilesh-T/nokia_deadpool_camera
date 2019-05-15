package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.Table.Cell;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nullable;

@GwtCompatible
class StandardTable<R, C, V> extends AbstractTable<R, C, V> implements Serializable {
    private static final long serialVersionUID = 0;
    @GwtTransient
    final Map<R, Map<C, V>> backingMap;
    private transient Set<C> columnKeySet;
    private transient ColumnMap columnMap;
    @GwtTransient
    final Supplier<? extends Map<C, V>> factory;
    private transient Map<R, Map<C, V>> rowMap;

    private class CellIterator implements Iterator<Cell<R, C, V>> {
        Iterator<Entry<C, V>> columnIterator;
        Entry<R, Map<C, V>> rowEntry;
        final Iterator<Entry<R, Map<C, V>>> rowIterator;

        private CellIterator() {
            this.rowIterator = StandardTable.this.backingMap.entrySet().iterator();
            this.columnIterator = Iterators.emptyModifiableIterator();
        }

        public boolean hasNext() {
            return this.rowIterator.hasNext() || this.columnIterator.hasNext();
        }

        public Cell<R, C, V> next() {
            if (!this.columnIterator.hasNext()) {
                this.rowEntry = (Entry) this.rowIterator.next();
                this.columnIterator = ((Map) this.rowEntry.getValue()).entrySet().iterator();
            }
            Entry<C, V> columnEntry = (Entry) this.columnIterator.next();
            return Tables.immutableCell(this.rowEntry.getKey(), columnEntry.getKey(), columnEntry.getValue());
        }

        public void remove() {
            this.columnIterator.remove();
            if (((Map) this.rowEntry.getValue()).isEmpty()) {
                this.rowIterator.remove();
            }
        }
    }

    private class Column extends ImprovedAbstractMap<R, V> {
        final C columnKey;

        private class EntrySet extends ImprovedAbstractSet<Entry<R, V>> {
            private EntrySet() {
            }

            public Iterator<Entry<R, V>> iterator() {
                return new EntrySetIterator();
            }

            public int size() {
                int size = 0;
                for (Map<C, V> map : StandardTable.this.backingMap.values()) {
                    if (map.containsKey(Column.this.columnKey)) {
                        size++;
                    }
                }
                return size;
            }

            public boolean isEmpty() {
                return StandardTable.this.containsColumn(Column.this.columnKey) ^ 1;
            }

            public void clear() {
                Column.this.removeFromColumnIf(Predicates.alwaysTrue());
            }

            public boolean contains(Object o) {
                if (!(o instanceof Entry)) {
                    return false;
                }
                Entry<?, ?> entry = (Entry) o;
                return StandardTable.this.containsMapping(entry.getKey(), Column.this.columnKey, entry.getValue());
            }

            public boolean remove(Object obj) {
                if (!(obj instanceof Entry)) {
                    return false;
                }
                Entry<?, ?> entry = (Entry) obj;
                return StandardTable.this.removeMapping(entry.getKey(), Column.this.columnKey, entry.getValue());
            }

            public boolean retainAll(Collection<?> c) {
                return Column.this.removeFromColumnIf(Predicates.not(Predicates.in(c)));
            }
        }

        private class Values extends Values<R, V> {
            Values() {
                super(Column.this);
            }

            public boolean remove(Object obj) {
                return obj != null && Column.this.removeFromColumnIf(Maps.valuePredicateOnEntries(Predicates.equalTo(obj)));
            }

            public boolean removeAll(Collection<?> c) {
                return Column.this.removeFromColumnIf(Maps.valuePredicateOnEntries(Predicates.in(c)));
            }

            public boolean retainAll(Collection<?> c) {
                return Column.this.removeFromColumnIf(Maps.valuePredicateOnEntries(Predicates.not(Predicates.in(c))));
            }
        }

        private class EntrySetIterator extends AbstractIterator<Entry<R, V>> {
            final Iterator<Entry<R, Map<C, V>>> iterator;

            private EntrySetIterator() {
                this.iterator = StandardTable.this.backingMap.entrySet().iterator();
            }

            /* Access modifiers changed, original: protected */
            public Entry<R, V> computeNext() {
                while (this.iterator.hasNext()) {
                    final Entry<R, Map<C, V>> entry = (Entry) this.iterator.next();
                    if (((Map) entry.getValue()).containsKey(Column.this.columnKey)) {
                        return new AbstractMapEntry<R, V>() {
                            public R getKey() {
                                return entry.getKey();
                            }

                            public V getValue() {
                                return ((Map) entry.getValue()).get(Column.this.columnKey);
                            }

                            public V setValue(V value) {
                                return ((Map) entry.getValue()).put(Column.this.columnKey, Preconditions.checkNotNull(value));
                            }
                        };
                    }
                }
                return (Entry) endOfData();
            }
        }

        private class KeySet extends KeySet<R, V> {
            KeySet() {
                super(Column.this);
            }

            public boolean contains(Object obj) {
                return StandardTable.this.contains(obj, Column.this.columnKey);
            }

            public boolean remove(Object obj) {
                return StandardTable.this.remove(obj, Column.this.columnKey) != null;
            }

            public boolean retainAll(Collection<?> c) {
                return Column.this.removeFromColumnIf(Maps.keyPredicateOnEntries(Predicates.not(Predicates.in(c))));
            }
        }

        Column(C columnKey) {
            this.columnKey = Preconditions.checkNotNull(columnKey);
        }

        public V put(R key, V value) {
            return StandardTable.this.put(key, this.columnKey, value);
        }

        public V get(Object key) {
            return StandardTable.this.get(key, this.columnKey);
        }

        public boolean containsKey(Object key) {
            return StandardTable.this.contains(key, this.columnKey);
        }

        public V remove(Object key) {
            return StandardTable.this.remove(key, this.columnKey);
        }

        /* Access modifiers changed, original: 0000 */
        public boolean removeFromColumnIf(Predicate<? super Entry<R, V>> predicate) {
            boolean changed = false;
            Iterator<Entry<R, Map<C, V>>> iterator = StandardTable.this.backingMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Entry<R, Map<C, V>> entry = (Entry) iterator.next();
                Map<C, V> map = (Map) entry.getValue();
                V value = map.get(this.columnKey);
                if (value != null && predicate.apply(Maps.immutableEntry(entry.getKey(), value))) {
                    map.remove(this.columnKey);
                    changed = true;
                    if (map.isEmpty()) {
                        iterator.remove();
                    }
                }
            }
            return changed;
        }

        /* Access modifiers changed, original: 0000 */
        public Set<Entry<R, V>> createEntrySet() {
            return new EntrySet();
        }

        /* Access modifiers changed, original: 0000 */
        public Set<R> createKeySet() {
            return new KeySet();
        }

        /* Access modifiers changed, original: 0000 */
        public Collection<V> createValues() {
            return new Values();
        }
    }

    private class ColumnMap extends ImprovedAbstractMap<C, Map<R, V>> {

        private class ColumnMapValues extends Values<C, Map<R, V>> {
            ColumnMapValues() {
                super(ColumnMap.this);
            }

            public boolean remove(Object obj) {
                for (Entry<C, Map<R, V>> entry : ColumnMap.this.entrySet()) {
                    if (((Map) entry.getValue()).equals(obj)) {
                        StandardTable.this.removeColumn(entry.getKey());
                        return true;
                    }
                }
                return false;
            }

            public boolean removeAll(Collection<?> c) {
                Preconditions.checkNotNull(c);
                boolean changed = false;
                Iterator it = Lists.newArrayList(StandardTable.this.columnKeySet().iterator()).iterator();
                while (it.hasNext()) {
                    C columnKey = it.next();
                    if (c.contains(StandardTable.this.column(columnKey))) {
                        StandardTable.this.removeColumn(columnKey);
                        changed = true;
                    }
                }
                return changed;
            }

            public boolean retainAll(Collection<?> c) {
                Preconditions.checkNotNull(c);
                boolean changed = false;
                Iterator it = Lists.newArrayList(StandardTable.this.columnKeySet().iterator()).iterator();
                while (it.hasNext()) {
                    C columnKey = it.next();
                    if (!c.contains(StandardTable.this.column(columnKey))) {
                        StandardTable.this.removeColumn(columnKey);
                        changed = true;
                    }
                }
                return changed;
            }
        }

        class ColumnMapEntrySet extends TableSet<Entry<C, Map<R, V>>> {
            ColumnMapEntrySet() {
                super();
            }

            public Iterator<Entry<C, Map<R, V>>> iterator() {
                return Maps.asMapEntryIterator(StandardTable.this.columnKeySet(), new Function<C, Map<R, V>>() {
                    public Map<R, V> apply(C columnKey) {
                        return StandardTable.this.column(columnKey);
                    }
                });
            }

            public int size() {
                return StandardTable.this.columnKeySet().size();
            }

            public boolean contains(Object obj) {
                if (obj instanceof Entry) {
                    Entry<?, ?> entry = (Entry) obj;
                    if (StandardTable.this.containsColumn(entry.getKey())) {
                        return ColumnMap.this.get(entry.getKey()).equals(entry.getValue());
                    }
                }
                return false;
            }

            public boolean remove(Object obj) {
                if (!contains(obj)) {
                    return false;
                }
                StandardTable.this.removeColumn(((Entry) obj).getKey());
                return true;
            }

            public boolean removeAll(Collection<?> c) {
                Preconditions.checkNotNull(c);
                return Sets.removeAllImpl((Set) this, c.iterator());
            }

            public boolean retainAll(Collection<?> c) {
                Preconditions.checkNotNull(c);
                boolean changed = false;
                Iterator it = Lists.newArrayList(StandardTable.this.columnKeySet().iterator()).iterator();
                while (it.hasNext()) {
                    C columnKey = it.next();
                    if (!c.contains(Maps.immutableEntry(columnKey, StandardTable.this.column(columnKey)))) {
                        StandardTable.this.removeColumn(columnKey);
                        changed = true;
                    }
                }
                return changed;
            }
        }

        private ColumnMap() {
        }

        public Map<R, V> get(Object key) {
            return StandardTable.this.containsColumn(key) ? StandardTable.this.column(key) : null;
        }

        public boolean containsKey(Object key) {
            return StandardTable.this.containsColumn(key);
        }

        public Map<R, V> remove(Object key) {
            return StandardTable.this.containsColumn(key) ? StandardTable.this.removeColumn(key) : null;
        }

        public Set<Entry<C, Map<R, V>>> createEntrySet() {
            return new ColumnMapEntrySet();
        }

        public Set<C> keySet() {
            return StandardTable.this.columnKeySet();
        }

        /* Access modifiers changed, original: 0000 */
        public Collection<Map<R, V>> createValues() {
            return new ColumnMapValues();
        }
    }

    class Row extends ImprovedAbstractMap<C, V> {
        Map<C, V> backingRowMap;
        final R rowKey;

        private final class RowEntrySet extends EntrySet<C, V> {
            private RowEntrySet() {
            }

            /* Access modifiers changed, original: 0000 */
            public Map<C, V> map() {
                return Row.this;
            }

            public int size() {
                Map<C, V> map = Row.this.backingRowMap();
                return map == null ? 0 : map.size();
            }

            public Iterator<Entry<C, V>> iterator() {
                Map<C, V> map = Row.this.backingRowMap();
                if (map == null) {
                    return Iterators.emptyModifiableIterator();
                }
                final Iterator<Entry<C, V>> iterator = map.entrySet().iterator();
                return new Iterator<Entry<C, V>>() {
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    public Entry<C, V> next() {
                        final Entry<C, V> entry = (Entry) iterator.next();
                        return new ForwardingMapEntry<C, V>() {
                            /* Access modifiers changed, original: protected */
                            public Entry<C, V> delegate() {
                                return entry;
                            }

                            public V setValue(V value) {
                                return super.setValue(Preconditions.checkNotNull(value));
                            }

                            public boolean equals(Object object) {
                                return standardEquals(object);
                            }
                        };
                    }

                    public void remove() {
                        iterator.remove();
                        Row.this.maintainEmptyInvariant();
                    }
                };
            }
        }

        Row(R rowKey) {
            this.rowKey = Preconditions.checkNotNull(rowKey);
        }

        /* Access modifiers changed, original: 0000 */
        public Map<C, V> backingRowMap() {
            if (this.backingRowMap != null && (!this.backingRowMap.isEmpty() || !StandardTable.this.backingMap.containsKey(this.rowKey))) {
                return this.backingRowMap;
            }
            Map computeBackingRowMap = computeBackingRowMap();
            this.backingRowMap = computeBackingRowMap;
            return computeBackingRowMap;
        }

        /* Access modifiers changed, original: 0000 */
        public Map<C, V> computeBackingRowMap() {
            return (Map) StandardTable.this.backingMap.get(this.rowKey);
        }

        /* Access modifiers changed, original: 0000 */
        public void maintainEmptyInvariant() {
            if (backingRowMap() != null && this.backingRowMap.isEmpty()) {
                StandardTable.this.backingMap.remove(this.rowKey);
                this.backingRowMap = null;
            }
        }

        public boolean containsKey(Object key) {
            Map<C, V> backingRowMap = backingRowMap();
            return (key == null || backingRowMap == null || !Maps.safeContainsKey(backingRowMap, key)) ? false : true;
        }

        public V get(Object key) {
            Map<C, V> backingRowMap = backingRowMap();
            if (key == null || backingRowMap == null) {
                return null;
            }
            return Maps.safeGet(backingRowMap, key);
        }

        public V put(C key, V value) {
            Preconditions.checkNotNull(key);
            Preconditions.checkNotNull(value);
            if (this.backingRowMap == null || this.backingRowMap.isEmpty()) {
                return StandardTable.this.put(this.rowKey, key, value);
            }
            return this.backingRowMap.put(key, value);
        }

        public V remove(Object key) {
            Map<C, V> backingRowMap = backingRowMap();
            if (backingRowMap == null) {
                return null;
            }
            V result = Maps.safeRemove(backingRowMap, key);
            maintainEmptyInvariant();
            return result;
        }

        public void clear() {
            Map<C, V> backingRowMap = backingRowMap();
            if (backingRowMap != null) {
                backingRowMap.clear();
            }
            maintainEmptyInvariant();
        }

        /* Access modifiers changed, original: protected */
        public Set<Entry<C, V>> createEntrySet() {
            return new RowEntrySet();
        }
    }

    class RowMap extends ImprovedAbstractMap<R, Map<C, V>> {

        class EntrySet extends TableSet<Entry<R, Map<C, V>>> {
            EntrySet() {
                super();
            }

            public Iterator<Entry<R, Map<C, V>>> iterator() {
                return Maps.asMapEntryIterator(StandardTable.this.backingMap.keySet(), new Function<R, Map<C, V>>() {
                    public Map<C, V> apply(R rowKey) {
                        return StandardTable.this.row(rowKey);
                    }
                });
            }

            public int size() {
                return StandardTable.this.backingMap.size();
            }

            public boolean contains(Object obj) {
                boolean z = false;
                if (!(obj instanceof Entry)) {
                    return false;
                }
                Entry<?, ?> entry = (Entry) obj;
                if (entry.getKey() != null && (entry.getValue() instanceof Map) && Collections2.safeContains(StandardTable.this.backingMap.entrySet(), entry)) {
                    z = true;
                }
                return z;
            }

            public boolean remove(Object obj) {
                boolean z = false;
                if (!(obj instanceof Entry)) {
                    return false;
                }
                Entry<?, ?> entry = (Entry) obj;
                if (entry.getKey() != null && (entry.getValue() instanceof Map) && StandardTable.this.backingMap.entrySet().remove(entry)) {
                    z = true;
                }
                return z;
            }
        }

        RowMap() {
        }

        public boolean containsKey(Object key) {
            return StandardTable.this.containsRow(key);
        }

        public Map<C, V> get(Object key) {
            return StandardTable.this.containsRow(key) ? StandardTable.this.row(key) : null;
        }

        public Map<C, V> remove(Object key) {
            return key == null ? null : (Map) StandardTable.this.backingMap.remove(key);
        }

        /* Access modifiers changed, original: protected */
        public Set<Entry<R, Map<C, V>>> createEntrySet() {
            return new EntrySet();
        }
    }

    private abstract class TableSet<T> extends ImprovedAbstractSet<T> {
        private TableSet() {
        }

        public boolean isEmpty() {
            return StandardTable.this.backingMap.isEmpty();
        }

        public void clear() {
            StandardTable.this.backingMap.clear();
        }
    }

    private class ColumnKeyIterator extends AbstractIterator<C> {
        Iterator<Entry<C, V>> entryIterator;
        final Iterator<Map<C, V>> mapIterator;
        final Map<C, V> seen;

        private ColumnKeyIterator() {
            this.seen = (Map) StandardTable.this.factory.get();
            this.mapIterator = StandardTable.this.backingMap.values().iterator();
            this.entryIterator = Iterators.emptyIterator();
        }

        /* Access modifiers changed, original: protected */
        public C computeNext() {
            while (true) {
                if (this.entryIterator.hasNext()) {
                    Entry<C, V> entry = (Entry) this.entryIterator.next();
                    if (!this.seen.containsKey(entry.getKey())) {
                        this.seen.put(entry.getKey(), entry.getValue());
                        return entry.getKey();
                    }
                } else if (!this.mapIterator.hasNext()) {
                    return endOfData();
                } else {
                    this.entryIterator = ((Map) this.mapIterator.next()).entrySet().iterator();
                }
            }
        }
    }

    private class ColumnKeySet extends TableSet<C> {
        private ColumnKeySet() {
            super();
        }

        public Iterator<C> iterator() {
            return StandardTable.this.createColumnKeyIterator();
        }

        public int size() {
            return Iterators.size(iterator());
        }

        public boolean remove(Object obj) {
            if (obj == null) {
                return false;
            }
            boolean changed = false;
            Iterator<Map<C, V>> iterator = StandardTable.this.backingMap.values().iterator();
            while (iterator.hasNext()) {
                Map<C, V> map = (Map) iterator.next();
                if (map.keySet().remove(obj)) {
                    changed = true;
                    if (map.isEmpty()) {
                        iterator.remove();
                    }
                }
            }
            return changed;
        }

        public boolean removeAll(Collection<?> c) {
            Preconditions.checkNotNull(c);
            boolean changed = false;
            Iterator<Map<C, V>> iterator = StandardTable.this.backingMap.values().iterator();
            while (iterator.hasNext()) {
                Map<C, V> map = (Map) iterator.next();
                if (Iterators.removeAll(map.keySet().iterator(), c)) {
                    changed = true;
                    if (map.isEmpty()) {
                        iterator.remove();
                    }
                }
            }
            return changed;
        }

        public boolean retainAll(Collection<?> c) {
            Preconditions.checkNotNull(c);
            boolean changed = false;
            Iterator<Map<C, V>> iterator = StandardTable.this.backingMap.values().iterator();
            while (iterator.hasNext()) {
                Map<C, V> map = (Map) iterator.next();
                if (map.keySet().retainAll(c)) {
                    changed = true;
                    if (map.isEmpty()) {
                        iterator.remove();
                    }
                }
            }
            return changed;
        }

        public boolean contains(Object obj) {
            return StandardTable.this.containsColumn(obj);
        }
    }

    StandardTable(Map<R, Map<C, V>> backingMap, Supplier<? extends Map<C, V>> factory) {
        this.backingMap = backingMap;
        this.factory = factory;
    }

    public boolean contains(@Nullable Object rowKey, @Nullable Object columnKey) {
        return (rowKey == null || columnKey == null || !super.contains(rowKey, columnKey)) ? false : true;
    }

    public boolean containsColumn(@Nullable Object columnKey) {
        if (columnKey == null) {
            return false;
        }
        for (Map<C, V> map : this.backingMap.values()) {
            if (Maps.safeContainsKey(map, columnKey)) {
                return true;
            }
        }
        return false;
    }

    public boolean containsRow(@Nullable Object rowKey) {
        return rowKey != null && Maps.safeContainsKey(this.backingMap, rowKey);
    }

    public boolean containsValue(@Nullable Object value) {
        return value != null && super.containsValue(value);
    }

    public V get(@Nullable Object rowKey, @Nullable Object columnKey) {
        if (rowKey == null || columnKey == null) {
            return null;
        }
        return super.get(rowKey, columnKey);
    }

    public boolean isEmpty() {
        return this.backingMap.isEmpty();
    }

    public int size() {
        int size = 0;
        for (Map<C, V> map : this.backingMap.values()) {
            size += map.size();
        }
        return size;
    }

    public void clear() {
        this.backingMap.clear();
    }

    private Map<C, V> getOrCreate(R rowKey) {
        Map<C, V> map = (Map) this.backingMap.get(rowKey);
        if (map != null) {
            return map;
        }
        map = (Map) this.factory.get();
        this.backingMap.put(rowKey, map);
        return map;
    }

    public V put(R rowKey, C columnKey, V value) {
        Preconditions.checkNotNull(rowKey);
        Preconditions.checkNotNull(columnKey);
        Preconditions.checkNotNull(value);
        return getOrCreate(rowKey).put(columnKey, value);
    }

    public V remove(@Nullable Object rowKey, @Nullable Object columnKey) {
        if (rowKey == null || columnKey == null) {
            return null;
        }
        Map<C, V> map = (Map) Maps.safeGet(this.backingMap, rowKey);
        if (map == null) {
            return null;
        }
        V value = map.remove(columnKey);
        if (map.isEmpty()) {
            this.backingMap.remove(rowKey);
        }
        return value;
    }

    private Map<R, V> removeColumn(Object column) {
        Map<R, V> output = new LinkedHashMap();
        Iterator<Entry<R, Map<C, V>>> iterator = this.backingMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<R, Map<C, V>> entry = (Entry) iterator.next();
            V value = ((Map) entry.getValue()).remove(column);
            if (value != null) {
                output.put(entry.getKey(), value);
                if (((Map) entry.getValue()).isEmpty()) {
                    iterator.remove();
                }
            }
        }
        return output;
    }

    private boolean containsMapping(Object rowKey, Object columnKey, Object value) {
        return value != null && value.equals(get(rowKey, columnKey));
    }

    private boolean removeMapping(Object rowKey, Object columnKey, Object value) {
        if (!containsMapping(rowKey, columnKey, value)) {
            return false;
        }
        remove(rowKey, columnKey);
        return true;
    }

    public Set<Cell<R, C, V>> cellSet() {
        return super.cellSet();
    }

    /* Access modifiers changed, original: 0000 */
    public Iterator<Cell<R, C, V>> cellIterator() {
        return new CellIterator();
    }

    public Map<C, V> row(R rowKey) {
        return new Row(rowKey);
    }

    public Map<R, V> column(C columnKey) {
        return new Column(columnKey);
    }

    public Set<R> rowKeySet() {
        return rowMap().keySet();
    }

    public Set<C> columnKeySet() {
        Set<C> result = this.columnKeySet;
        if (result != null) {
            return result;
        }
        Set<C> columnKeySet = new ColumnKeySet();
        this.columnKeySet = columnKeySet;
        return columnKeySet;
    }

    /* Access modifiers changed, original: 0000 */
    public Iterator<C> createColumnKeyIterator() {
        return new ColumnKeyIterator();
    }

    public Collection<V> values() {
        return super.values();
    }

    public Map<R, Map<C, V>> rowMap() {
        Map<R, Map<C, V>> result = this.rowMap;
        if (result != null) {
            return result;
        }
        Map<R, Map<C, V>> createRowMap = createRowMap();
        this.rowMap = createRowMap;
        return createRowMap;
    }

    /* Access modifiers changed, original: 0000 */
    public Map<R, Map<C, V>> createRowMap() {
        return new RowMap();
    }

    public Map<C, Map<R, V>> columnMap() {
        ColumnMap result = this.columnMap;
        if (result != null) {
            return result;
        }
        Map<C, Map<R, V>> columnMap = new ColumnMap();
        this.columnMap = columnMap;
        return columnMap;
    }
}
