package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Table.Cell;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;

@GwtCompatible
abstract class RegularImmutableTable<R, C, V> extends ImmutableTable<R, C, V> {

    private final class CellSet extends ImmutableSet<Cell<R, C, V>> {
        private CellSet() {
        }

        /* synthetic */ CellSet(RegularImmutableTable x0, AnonymousClass1 x1) {
            this();
        }

        public int size() {
            return RegularImmutableTable.this.size();
        }

        public UnmodifiableIterator<Cell<R, C, V>> iterator() {
            return asList().iterator();
        }

        /* Access modifiers changed, original: 0000 */
        public ImmutableList<Cell<R, C, V>> createAsList() {
            return new ImmutableAsList<Cell<R, C, V>>() {
                public Cell<R, C, V> get(int index) {
                    return RegularImmutableTable.this.getCell(index);
                }

                /* Access modifiers changed, original: 0000 */
                public ImmutableCollection<Cell<R, C, V>> delegateCollection() {
                    return CellSet.this;
                }
            };
        }

        public boolean contains(@Nullable Object object) {
            boolean z = false;
            if (!(object instanceof Cell)) {
                return false;
            }
            Cell<?, ?, ?> cell = (Cell) object;
            Object value = RegularImmutableTable.this.get(cell.getRowKey(), cell.getColumnKey());
            if (value != null && value.equals(cell.getValue())) {
                z = true;
            }
            return z;
        }

        /* Access modifiers changed, original: 0000 */
        public boolean isPartialView() {
            return false;
        }
    }

    private final class Values extends ImmutableList<V> {
        private Values() {
        }

        /* synthetic */ Values(RegularImmutableTable x0, AnonymousClass1 x1) {
            this();
        }

        public int size() {
            return RegularImmutableTable.this.size();
        }

        public V get(int index) {
            return RegularImmutableTable.this.getValue(index);
        }

        /* Access modifiers changed, original: 0000 */
        public boolean isPartialView() {
            return true;
        }
    }

    public abstract Cell<R, C, V> getCell(int i);

    public abstract V getValue(int i);

    RegularImmutableTable() {
    }

    /* Access modifiers changed, original: final */
    public final ImmutableSet<Cell<R, C, V>> createCellSet() {
        return isEmpty() ? ImmutableSet.of() : new CellSet(this, null);
    }

    /* Access modifiers changed, original: final */
    public final ImmutableCollection<V> createValues() {
        return isEmpty() ? ImmutableList.of() : new Values(this, null);
    }

    static <R, C, V> RegularImmutableTable<R, C, V> forCells(List<Cell<R, C, V>> cells, @Nullable final Comparator<? super R> rowComparator, @Nullable final Comparator<? super C> columnComparator) {
        Preconditions.checkNotNull(cells);
        if (!(rowComparator == null && columnComparator == null)) {
            Collections.sort(cells, new Comparator<Cell<R, C, V>>() {
                public int compare(Cell<R, C, V> cell1, Cell<R, C, V> cell2) {
                    int i = 0;
                    int rowCompare = rowComparator == null ? 0 : rowComparator.compare(cell1.getRowKey(), cell2.getRowKey());
                    if (rowCompare != 0) {
                        return rowCompare;
                    }
                    if (columnComparator != null) {
                        i = columnComparator.compare(cell1.getColumnKey(), cell2.getColumnKey());
                    }
                    return i;
                }
            });
        }
        return forCellsInternal(cells, rowComparator, columnComparator);
    }

    static <R, C, V> RegularImmutableTable<R, C, V> forCells(Iterable<Cell<R, C, V>> cells) {
        return forCellsInternal(cells, null, null);
    }

    private static final <R, C, V> RegularImmutableTable<R, C, V> forCellsInternal(Iterable<Cell<R, C, V>> cells, @Nullable Comparator<? super R> rowComparator, @Nullable Comparator<? super C> columnComparator) {
        Builder<R> rowSpaceBuilder = ImmutableSet.builder();
        Builder<C> columnSpaceBuilder = ImmutableSet.builder();
        ImmutableList<Cell<R, C, V>> cellList = ImmutableList.copyOf((Iterable) cells);
        UnmodifiableIterator it = cellList.iterator();
        while (it.hasNext()) {
            Cell<R, C, V> cell = (Cell) it.next();
            rowSpaceBuilder.add(cell.getRowKey());
            columnSpaceBuilder.add(cell.getColumnKey());
        }
        ImmutableSet<R> rowSpace = rowSpaceBuilder.build();
        if (rowComparator != null) {
            Collection rowList = Lists.newArrayList((Iterable) rowSpace);
            Collections.sort(rowList, rowComparator);
            rowSpace = ImmutableSet.copyOf(rowList);
        }
        ImmutableSet<C> columnSpace = columnSpaceBuilder.build();
        if (columnComparator != null) {
            Collection columnList = Lists.newArrayList((Iterable) columnSpace);
            Collections.sort(columnList, columnComparator);
            columnSpace = ImmutableSet.copyOf(columnList);
        }
        if (((long) cellList.size()) > (((long) rowSpace.size()) * ((long) columnSpace.size())) / 2) {
            return new DenseImmutableTable(cellList, rowSpace, columnSpace);
        }
        return new SparseImmutableTable(cellList, rowSpace, columnSpace);
    }
}
