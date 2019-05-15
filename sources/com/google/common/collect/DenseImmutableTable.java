package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Table.Cell;
import java.lang.reflect.Array;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

@GwtCompatible
@Immutable
final class DenseImmutableTable<R, C, V> extends RegularImmutableTable<R, C, V> {
    private final int[] columnCounts = new int[this.columnKeyToIndex.size()];
    private final ImmutableMap<C, Integer> columnKeyToIndex;
    private final ImmutableMap<C, Map<R, V>> columnMap;
    private final int[] iterationOrderColumn;
    private final int[] iterationOrderRow;
    private final int[] rowCounts = new int[this.rowKeyToIndex.size()];
    private final ImmutableMap<R, Integer> rowKeyToIndex;
    private final ImmutableMap<R, Map<C, V>> rowMap;
    private final V[][] values;

    private static abstract class ImmutableArrayMap<K, V> extends ImmutableMap<K, V> {
        private final int size;

        @Nullable
        public abstract V getValue(int i);

        public abstract ImmutableMap<K, Integer> keyToIndex();

        ImmutableArrayMap(int size) {
            this.size = size;
        }

        private boolean isFull() {
            return this.size == keyToIndex().size();
        }

        /* Access modifiers changed, original: 0000 */
        public K getKey(int index) {
            return keyToIndex().keySet().asList().get(index);
        }

        /* Access modifiers changed, original: 0000 */
        public ImmutableSet<K> createKeySet() {
            return isFull() ? keyToIndex().keySet() : super.createKeySet();
        }

        public int size() {
            return this.size;
        }

        public V get(@Nullable Object key) {
            Integer keyIndex = (Integer) keyToIndex().get(key);
            return keyIndex == null ? null : getValue(keyIndex.intValue());
        }

        /* Access modifiers changed, original: 0000 */
        public ImmutableSet<Entry<K, V>> createEntrySet() {
            return new ImmutableMapEntrySet<K, V>() {
                /* Access modifiers changed, original: 0000 */
                public ImmutableMap<K, V> map() {
                    return ImmutableArrayMap.this;
                }

                public UnmodifiableIterator<Entry<K, V>> iterator() {
                    return new AbstractIterator<Entry<K, V>>() {
                        private int index = -1;
                        private final int maxIndex = ImmutableArrayMap.this.keyToIndex().size();

                        /* Access modifiers changed, original: protected */
                        public Entry<K, V> computeNext() {
                            int value = this.index;
                            while (true) {
                                this.index = value + 1;
                                if (this.index >= this.maxIndex) {
                                    return (Entry) endOfData();
                                }
                                V value2 = ImmutableArrayMap.this.getValue(this.index);
                                if (value2 != null) {
                                    return Maps.immutableEntry(ImmutableArrayMap.this.getKey(this.index), value2);
                                }
                                value = this.index;
                            }
                        }
                    };
                }
            };
        }
    }

    private final class Column extends ImmutableArrayMap<R, V> {
        private final int columnIndex;

        Column(int columnIndex) {
            super(DenseImmutableTable.this.columnCounts[columnIndex]);
            this.columnIndex = columnIndex;
        }

        /* Access modifiers changed, original: 0000 */
        public ImmutableMap<R, Integer> keyToIndex() {
            return DenseImmutableTable.this.rowKeyToIndex;
        }

        /* Access modifiers changed, original: 0000 */
        public V getValue(int keyIndex) {
            return DenseImmutableTable.this.values[keyIndex][this.columnIndex];
        }

        /* Access modifiers changed, original: 0000 */
        public boolean isPartialView() {
            return true;
        }
    }

    private final class ColumnMap extends ImmutableArrayMap<C, Map<R, V>> {
        private ColumnMap() {
            super(DenseImmutableTable.this.columnCounts.length);
        }

        /* Access modifiers changed, original: 0000 */
        public ImmutableMap<C, Integer> keyToIndex() {
            return DenseImmutableTable.this.columnKeyToIndex;
        }

        /* Access modifiers changed, original: 0000 */
        public Map<R, V> getValue(int keyIndex) {
            return new Column(keyIndex);
        }

        /* Access modifiers changed, original: 0000 */
        public boolean isPartialView() {
            return false;
        }
    }

    private final class Row extends ImmutableArrayMap<C, V> {
        private final int rowIndex;

        Row(int rowIndex) {
            super(DenseImmutableTable.this.rowCounts[rowIndex]);
            this.rowIndex = rowIndex;
        }

        /* Access modifiers changed, original: 0000 */
        public ImmutableMap<C, Integer> keyToIndex() {
            return DenseImmutableTable.this.columnKeyToIndex;
        }

        /* Access modifiers changed, original: 0000 */
        public V getValue(int keyIndex) {
            return DenseImmutableTable.this.values[this.rowIndex][keyIndex];
        }

        /* Access modifiers changed, original: 0000 */
        public boolean isPartialView() {
            return true;
        }
    }

    private final class RowMap extends ImmutableArrayMap<R, Map<C, V>> {
        private RowMap() {
            super(DenseImmutableTable.this.rowCounts.length);
        }

        /* Access modifiers changed, original: 0000 */
        public ImmutableMap<R, Integer> keyToIndex() {
            return DenseImmutableTable.this.rowKeyToIndex;
        }

        /* Access modifiers changed, original: 0000 */
        public Map<C, V> getValue(int keyIndex) {
            return new Row(keyIndex);
        }

        /* Access modifiers changed, original: 0000 */
        public boolean isPartialView() {
            return false;
        }
    }

    private static <E> ImmutableMap<E, Integer> makeIndex(ImmutableSet<E> set) {
        Builder<E, Integer> indexBuilder = ImmutableMap.builder();
        int i = 0;
        UnmodifiableIterator it = set.iterator();
        while (it.hasNext()) {
            indexBuilder.put(it.next(), Integer.valueOf(i));
            i++;
        }
        return indexBuilder.build();
    }

    DenseImmutableTable(ImmutableList<Cell<R, C, V>> cellList, ImmutableSet<R> rowSpace, ImmutableSet<C> columnSpace) {
        this.values = (Object[][]) Array.newInstance(Object.class, new int[]{rowSpace.size(), columnSpace.size()});
        this.rowKeyToIndex = makeIndex(rowSpace);
        this.columnKeyToIndex = makeIndex(columnSpace);
        int[] iterationOrderRow = new int[cellList.size()];
        int[] iterationOrderColumn = new int[cellList.size()];
        for (int i = 0; i < cellList.size(); i++) {
            Cell<R, C, V> cell = (Cell) cellList.get(i);
            R rowKey = cell.getRowKey();
            C columnKey = cell.getColumnKey();
            int rowIndex = ((Integer) this.rowKeyToIndex.get(rowKey)).intValue();
            int columnIndex = ((Integer) this.columnKeyToIndex.get(columnKey)).intValue();
            Preconditions.checkArgument(this.values[rowIndex][columnIndex] == null, "duplicate key: (%s, %s)", rowKey, columnKey);
            this.values[rowIndex][columnIndex] = cell.getValue();
            int[] iArr = this.rowCounts;
            iArr[rowIndex] = iArr[rowIndex] + 1;
            iArr = this.columnCounts;
            iArr[columnIndex] = iArr[columnIndex] + 1;
            iterationOrderRow[i] = rowIndex;
            iterationOrderColumn[i] = columnIndex;
        }
        ImmutableList<Cell<R, C, V>> immutableList = cellList;
        this.iterationOrderRow = iterationOrderRow;
        this.iterationOrderColumn = iterationOrderColumn;
        this.rowMap = new RowMap();
        this.columnMap = new ColumnMap();
    }

    public ImmutableMap<C, Map<R, V>> columnMap() {
        return this.columnMap;
    }

    public ImmutableMap<R, Map<C, V>> rowMap() {
        return this.rowMap;
    }

    public V get(@Nullable Object rowKey, @Nullable Object columnKey) {
        Integer rowIndex = (Integer) this.rowKeyToIndex.get(rowKey);
        Integer columnIndex = (Integer) this.columnKeyToIndex.get(columnKey);
        if (rowIndex == null || columnIndex == null) {
            return null;
        }
        return this.values[rowIndex.intValue()][columnIndex.intValue()];
    }

    public int size() {
        return this.iterationOrderRow.length;
    }

    /* Access modifiers changed, original: 0000 */
    public Cell<R, C, V> getCell(int index) {
        int rowIndex = this.iterationOrderRow[index];
        int columnIndex = this.iterationOrderColumn[index];
        return ImmutableTable.cellOf(rowKeySet().asList().get(rowIndex), columnKeySet().asList().get(columnIndex), this.values[rowIndex][columnIndex]);
    }

    /* Access modifiers changed, original: 0000 */
    public V getValue(int index) {
        return this.values[this.iterationOrderRow[index]][this.iterationOrderColumn[index]];
    }
}
