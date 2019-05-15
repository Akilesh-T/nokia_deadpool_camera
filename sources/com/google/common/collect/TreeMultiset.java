package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Multiset.Entry;
import com.google.common.primitives.Ints;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.annotation.Nullable;

@GwtCompatible(emulated = true)
public final class TreeMultiset<E> extends AbstractSortedMultiset<E> implements Serializable {
    @GwtIncompatible("not needed in emulated source")
    private static final long serialVersionUID = 1;
    private final transient AvlNode<E> header;
    private final transient GeneralRange<E> range;
    private final transient Reference<AvlNode<E>> rootReference;

    private enum Aggregate {
        SIZE {
            /* Access modifiers changed, original: 0000 */
            public int nodeAggregate(AvlNode<?> node) {
                return node.elemCount;
            }

            /* Access modifiers changed, original: 0000 */
            public long treeAggregate(@Nullable AvlNode<?> root) {
                return root == null ? 0 : root.totalCount;
            }
        },
        DISTINCT {
            /* Access modifiers changed, original: 0000 */
            public int nodeAggregate(AvlNode<?> avlNode) {
                return 1;
            }

            /* Access modifiers changed, original: 0000 */
            public long treeAggregate(@Nullable AvlNode<?> root) {
                return root == null ? 0 : (long) root.distinctElements;
            }
        };

        public abstract int nodeAggregate(AvlNode<?> avlNode);

        public abstract long treeAggregate(@Nullable AvlNode<?> avlNode);
    }

    private static final class Reference<T> {
        @Nullable
        private T value;

        private Reference() {
        }

        /* synthetic */ Reference(AnonymousClass1 x0) {
            this();
        }

        @Nullable
        public T get() {
            return this.value;
        }

        public void checkAndSet(@Nullable T expected, T newValue) {
            if (this.value == expected) {
                this.value = newValue;
                return;
            }
            throw new ConcurrentModificationException();
        }
    }

    private static final class AvlNode<E> extends AbstractEntry<E> {
        private int distinctElements;
        @Nullable
        private final E elem;
        private int elemCount;
        private int height;
        private AvlNode<E> left;
        private AvlNode<E> pred;
        private AvlNode<E> right;
        private AvlNode<E> succ;
        private long totalCount;

        AvlNode(@Nullable E elem, int elemCount) {
            Preconditions.checkArgument(elemCount > 0);
            this.elem = elem;
            this.elemCount = elemCount;
            this.totalCount = (long) elemCount;
            this.distinctElements = 1;
            this.height = 1;
            this.left = null;
            this.right = null;
        }

        public int count(Comparator<? super E> comparator, E e) {
            int cmp = comparator.compare(e, this.elem);
            int i = 0;
            if (cmp < 0) {
                if (this.left != null) {
                    i = this.left.count(comparator, e);
                }
                return i;
            } else if (cmp <= 0) {
                return this.elemCount;
            } else {
                if (this.right != null) {
                    i = this.right.count(comparator, e);
                }
                return i;
            }
        }

        private AvlNode<E> addRightChild(E e, int count) {
            this.right = new AvlNode(e, count);
            TreeMultiset.successor(this, this.right, this.succ);
            this.height = Math.max(2, this.height);
            this.distinctElements++;
            this.totalCount += (long) count;
            return this;
        }

        private AvlNode<E> addLeftChild(E e, int count) {
            this.left = new AvlNode(e, count);
            TreeMultiset.successor(this.pred, this.left, this);
            this.height = Math.max(2, this.height);
            this.distinctElements++;
            this.totalCount += (long) count;
            return this;
        }

        /* Access modifiers changed, original: 0000 */
        public AvlNode<E> add(Comparator<? super E> comparator, @Nullable E e, int count, int[] result) {
            int cmp = comparator.compare(e, this.elem);
            boolean z = true;
            AvlNode<E> initLeft;
            int initHeight;
            if (cmp < 0) {
                initLeft = this.left;
                if (initLeft == null) {
                    result[0] = 0;
                    return addLeftChild(e, count);
                }
                initHeight = initLeft.height;
                this.left = initLeft.add(comparator, e, count, result);
                if (result[0] == 0) {
                    this.distinctElements++;
                }
                this.totalCount += (long) count;
                return this.left.height == initHeight ? this : rebalance();
            } else if (cmp > 0) {
                initLeft = this.right;
                if (initLeft == null) {
                    result[0] = 0;
                    return addRightChild(e, count);
                }
                initHeight = initLeft.height;
                this.right = initLeft.add(comparator, e, count, result);
                if (result[0] == 0) {
                    this.distinctElements++;
                }
                this.totalCount += (long) count;
                return this.right.height == initHeight ? this : rebalance();
            } else {
                result[0] = this.elemCount;
                if (((long) this.elemCount) + ((long) count) > 2147483647L) {
                    z = false;
                }
                Preconditions.checkArgument(z);
                this.elemCount += count;
                this.totalCount += (long) count;
                return this;
            }
        }

        /* Access modifiers changed, original: 0000 */
        public AvlNode<E> remove(Comparator<? super E> comparator, @Nullable E e, int count, int[] result) {
            int cmp = comparator.compare(e, this.elem);
            AvlNode<E> initLeft;
            if (cmp < 0) {
                initLeft = this.left;
                if (initLeft == null) {
                    result[0] = 0;
                    return this;
                }
                this.left = initLeft.remove(comparator, e, count, result);
                if (result[0] > 0) {
                    if (count >= result[0]) {
                        this.distinctElements--;
                        this.totalCount -= (long) result[0];
                    } else {
                        this.totalCount -= (long) count;
                    }
                }
                return result[0] == 0 ? this : rebalance();
            } else if (cmp > 0) {
                initLeft = this.right;
                if (initLeft == null) {
                    result[0] = 0;
                    return this;
                }
                this.right = initLeft.remove(comparator, e, count, result);
                if (result[0] > 0) {
                    if (count >= result[0]) {
                        this.distinctElements--;
                        this.totalCount -= (long) result[0];
                    } else {
                        this.totalCount -= (long) count;
                    }
                }
                return rebalance();
            } else {
                result[0] = this.elemCount;
                if (count >= this.elemCount) {
                    return deleteMe();
                }
                this.elemCount -= count;
                this.totalCount -= (long) count;
                return this;
            }
        }

        /* Access modifiers changed, original: 0000 */
        public AvlNode<E> setCount(Comparator<? super E> comparator, @Nullable E e, int count, int[] result) {
            int cmp = comparator.compare(e, this.elem);
            AvlNode<E> initLeft;
            if (cmp < 0) {
                initLeft = this.left;
                if (initLeft == null) {
                    result[0] = 0;
                    return count > 0 ? addLeftChild(e, count) : this;
                }
                this.left = initLeft.setCount(comparator, e, count, result);
                if (count == 0 && result[0] != 0) {
                    this.distinctElements--;
                } else if (count > 0 && result[0] == 0) {
                    this.distinctElements++;
                }
                this.totalCount += (long) (count - result[0]);
                return rebalance();
            } else if (cmp > 0) {
                initLeft = this.right;
                if (initLeft == null) {
                    result[0] = 0;
                    return count > 0 ? addRightChild(e, count) : this;
                }
                this.right = initLeft.setCount(comparator, e, count, result);
                if (count == 0 && result[0] != 0) {
                    this.distinctElements--;
                } else if (count > 0 && result[0] == 0) {
                    this.distinctElements++;
                }
                this.totalCount += (long) (count - result[0]);
                return rebalance();
            } else {
                result[0] = this.elemCount;
                if (count == 0) {
                    return deleteMe();
                }
                this.totalCount += (long) (count - this.elemCount);
                this.elemCount = count;
                return this;
            }
        }

        /* Access modifiers changed, original: 0000 */
        public AvlNode<E> setCount(Comparator<? super E> comparator, @Nullable E e, int expectedCount, int newCount, int[] result) {
            int cmp = comparator.compare(e, this.elem);
            AvlNode<E> initLeft;
            if (cmp < 0) {
                initLeft = this.left;
                if (initLeft == null) {
                    result[0] = 0;
                    if (expectedCount != 0 || newCount <= 0) {
                        return this;
                    }
                    return addLeftChild(e, newCount);
                }
                this.left = initLeft.setCount(comparator, e, expectedCount, newCount, result);
                if (result[0] == expectedCount) {
                    if (newCount == 0 && result[0] != 0) {
                        this.distinctElements--;
                    } else if (newCount > 0 && result[0] == 0) {
                        this.distinctElements++;
                    }
                    this.totalCount += (long) (newCount - result[0]);
                }
                return rebalance();
            } else if (cmp > 0) {
                initLeft = this.right;
                if (initLeft == null) {
                    result[0] = 0;
                    if (expectedCount != 0 || newCount <= 0) {
                        return this;
                    }
                    return addRightChild(e, newCount);
                }
                this.right = initLeft.setCount(comparator, e, expectedCount, newCount, result);
                if (result[0] == expectedCount) {
                    if (newCount == 0 && result[0] != 0) {
                        this.distinctElements--;
                    } else if (newCount > 0 && result[0] == 0) {
                        this.distinctElements++;
                    }
                    this.totalCount += (long) (newCount - result[0]);
                }
                return rebalance();
            } else {
                result[0] = this.elemCount;
                if (expectedCount == this.elemCount) {
                    if (newCount == 0) {
                        return deleteMe();
                    }
                    this.totalCount += (long) (newCount - this.elemCount);
                    this.elemCount = newCount;
                }
                return this;
            }
        }

        private AvlNode<E> deleteMe() {
            int oldElemCount = this.elemCount;
            this.elemCount = 0;
            TreeMultiset.successor(this.pred, this.succ);
            if (this.left == null) {
                return this.right;
            }
            if (this.right == null) {
                return this.left;
            }
            AvlNode<E> newTop;
            if (this.left.height >= this.right.height) {
                newTop = this.pred;
                newTop.left = this.left.removeMax(newTop);
                newTop.right = this.right;
                newTop.distinctElements = this.distinctElements - 1;
                newTop.totalCount = this.totalCount - ((long) oldElemCount);
                return newTop.rebalance();
            }
            newTop = this.succ;
            newTop.right = this.right.removeMin(newTop);
            newTop.left = this.left;
            newTop.distinctElements = this.distinctElements - 1;
            newTop.totalCount = this.totalCount - ((long) oldElemCount);
            return newTop.rebalance();
        }

        private AvlNode<E> removeMin(AvlNode<E> node) {
            if (this.left == null) {
                return this.right;
            }
            this.left = this.left.removeMin(node);
            this.distinctElements--;
            this.totalCount -= (long) node.elemCount;
            return rebalance();
        }

        private AvlNode<E> removeMax(AvlNode<E> node) {
            if (this.right == null) {
                return this.left;
            }
            this.right = this.right.removeMax(node);
            this.distinctElements--;
            this.totalCount -= (long) node.elemCount;
            return rebalance();
        }

        private void recomputeMultiset() {
            this.distinctElements = (1 + TreeMultiset.distinctElements(this.left)) + TreeMultiset.distinctElements(this.right);
            this.totalCount = (((long) this.elemCount) + totalCount(this.left)) + totalCount(this.right);
        }

        private void recomputeHeight() {
            this.height = 1 + Math.max(height(this.left), height(this.right));
        }

        private void recompute() {
            recomputeMultiset();
            recomputeHeight();
        }

        private AvlNode<E> rebalance() {
            int balanceFactor = balanceFactor();
            if (balanceFactor == -2) {
                if (this.right.balanceFactor() > 0) {
                    this.right = this.right.rotateRight();
                }
                return rotateLeft();
            } else if (balanceFactor != 2) {
                recomputeHeight();
                return this;
            } else {
                if (this.left.balanceFactor() < 0) {
                    this.left = this.left.rotateLeft();
                }
                return rotateRight();
            }
        }

        private int balanceFactor() {
            return height(this.left) - height(this.right);
        }

        private AvlNode<E> rotateLeft() {
            Preconditions.checkState(this.right != null);
            AvlNode<E> newTop = this.right;
            this.right = newTop.left;
            newTop.left = this;
            newTop.totalCount = this.totalCount;
            newTop.distinctElements = this.distinctElements;
            recompute();
            newTop.recomputeHeight();
            return newTop;
        }

        private AvlNode<E> rotateRight() {
            Preconditions.checkState(this.left != null);
            AvlNode<E> newTop = this.left;
            this.left = newTop.right;
            newTop.right = this;
            newTop.totalCount = this.totalCount;
            newTop.distinctElements = this.distinctElements;
            recompute();
            newTop.recomputeHeight();
            return newTop;
        }

        private static long totalCount(@Nullable AvlNode<?> node) {
            return node == null ? 0 : node.totalCount;
        }

        private static int height(@Nullable AvlNode<?> node) {
            return node == null ? 0 : node.height;
        }

        @Nullable
        private AvlNode<E> ceiling(Comparator<? super E> comparator, E e) {
            int cmp = comparator.compare(e, this.elem);
            if (cmp < 0) {
                return this.left == null ? this : (AvlNode) MoreObjects.firstNonNull(this.left.ceiling(comparator, e), this);
            } else if (cmp == 0) {
                return this;
            } else {
                return this.right == null ? null : this.right.ceiling(comparator, e);
            }
        }

        @Nullable
        private AvlNode<E> floor(Comparator<? super E> comparator, E e) {
            int cmp = comparator.compare(e, this.elem);
            if (cmp > 0) {
                return this.right == null ? this : (AvlNode) MoreObjects.firstNonNull(this.right.floor(comparator, e), this);
            } else if (cmp == 0) {
                return this;
            } else {
                return this.left == null ? null : this.left.floor(comparator, e);
            }
        }

        public E getElement() {
            return this.elem;
        }

        public int getCount() {
            return this.elemCount;
        }

        public String toString() {
            return Multisets.immutableEntry(getElement(), getCount()).toString();
        }
    }

    public /* bridge */ /* synthetic */ boolean add(@Nullable Object obj) {
        return super.add(obj);
    }

    public /* bridge */ /* synthetic */ boolean addAll(Collection collection) {
        return super.addAll(collection);
    }

    public /* bridge */ /* synthetic */ void clear() {
        super.clear();
    }

    public /* bridge */ /* synthetic */ Comparator comparator() {
        return super.comparator();
    }

    public /* bridge */ /* synthetic */ boolean contains(@Nullable Object obj) {
        return super.contains(obj);
    }

    public /* bridge */ /* synthetic */ SortedMultiset descendingMultiset() {
        return super.descendingMultiset();
    }

    public /* bridge */ /* synthetic */ NavigableSet elementSet() {
        return super.elementSet();
    }

    public /* bridge */ /* synthetic */ Set entrySet() {
        return super.entrySet();
    }

    public /* bridge */ /* synthetic */ boolean equals(@Nullable Object obj) {
        return super.equals(obj);
    }

    public /* bridge */ /* synthetic */ Entry firstEntry() {
        return super.firstEntry();
    }

    public /* bridge */ /* synthetic */ int hashCode() {
        return super.hashCode();
    }

    public /* bridge */ /* synthetic */ boolean isEmpty() {
        return super.isEmpty();
    }

    public /* bridge */ /* synthetic */ Iterator iterator() {
        return super.iterator();
    }

    public /* bridge */ /* synthetic */ Entry lastEntry() {
        return super.lastEntry();
    }

    public /* bridge */ /* synthetic */ Entry pollFirstEntry() {
        return super.pollFirstEntry();
    }

    public /* bridge */ /* synthetic */ Entry pollLastEntry() {
        return super.pollLastEntry();
    }

    public /* bridge */ /* synthetic */ boolean remove(@Nullable Object obj) {
        return super.remove(obj);
    }

    public /* bridge */ /* synthetic */ boolean removeAll(Collection collection) {
        return super.removeAll(collection);
    }

    public /* bridge */ /* synthetic */ boolean retainAll(Collection collection) {
        return super.retainAll(collection);
    }

    public /* bridge */ /* synthetic */ SortedMultiset subMultiset(@Nullable Object obj, BoundType boundType, @Nullable Object obj2, BoundType boundType2) {
        return super.subMultiset(obj, boundType, obj2, boundType2);
    }

    public /* bridge */ /* synthetic */ String toString() {
        return super.toString();
    }

    public static <E extends Comparable> TreeMultiset<E> create() {
        return new TreeMultiset(Ordering.natural());
    }

    public static <E> TreeMultiset<E> create(@Nullable Comparator<? super E> comparator) {
        if (comparator == null) {
            return new TreeMultiset(Ordering.natural());
        }
        return new TreeMultiset(comparator);
    }

    public static <E extends Comparable> TreeMultiset<E> create(Iterable<? extends E> elements) {
        TreeMultiset<E> multiset = create();
        Iterables.addAll(multiset, elements);
        return multiset;
    }

    TreeMultiset(Reference<AvlNode<E>> rootReference, GeneralRange<E> range, AvlNode<E> endLink) {
        super(range.comparator());
        this.rootReference = rootReference;
        this.range = range;
        this.header = endLink;
    }

    TreeMultiset(Comparator<? super E> comparator) {
        super(comparator);
        this.range = GeneralRange.all(comparator);
        this.header = new AvlNode(null, 1);
        successor(this.header, this.header);
        this.rootReference = new Reference();
    }

    private long aggregateForEntries(Aggregate aggr) {
        AvlNode<E> root = (AvlNode) this.rootReference.get();
        long total = aggr.treeAggregate(root);
        if (this.range.hasLowerBound()) {
            total -= aggregateBelowRange(aggr, root);
        }
        if (this.range.hasUpperBound()) {
            return total - aggregateAboveRange(aggr, root);
        }
        return total;
    }

    private long aggregateBelowRange(Aggregate aggr, @Nullable AvlNode<E> node) {
        if (node == null) {
            return 0;
        }
        int cmp = comparator().compare(this.range.getLowerEndpoint(), node.elem);
        if (cmp < 0) {
            return aggregateBelowRange(aggr, node.left);
        }
        if (cmp != 0) {
            return (aggr.treeAggregate(node.left) + ((long) aggr.nodeAggregate(node))) + aggregateBelowRange(aggr, node.right);
        }
        switch (this.range.getLowerBoundType()) {
            case OPEN:
                return ((long) aggr.nodeAggregate(node)) + aggr.treeAggregate(node.left);
            case CLOSED:
                return aggr.treeAggregate(node.left);
            default:
                throw new AssertionError();
        }
    }

    private long aggregateAboveRange(Aggregate aggr, @Nullable AvlNode<E> node) {
        if (node == null) {
            return 0;
        }
        int cmp = comparator().compare(this.range.getUpperEndpoint(), node.elem);
        if (cmp > 0) {
            return aggregateAboveRange(aggr, node.right);
        }
        if (cmp != 0) {
            return (aggr.treeAggregate(node.right) + ((long) aggr.nodeAggregate(node))) + aggregateAboveRange(aggr, node.left);
        }
        switch (this.range.getUpperBoundType()) {
            case OPEN:
                return ((long) aggr.nodeAggregate(node)) + aggr.treeAggregate(node.right);
            case CLOSED:
                return aggr.treeAggregate(node.right);
            default:
                throw new AssertionError();
        }
    }

    public int size() {
        return Ints.saturatedCast(aggregateForEntries(Aggregate.SIZE));
    }

    /* Access modifiers changed, original: 0000 */
    public int distinctElements() {
        return Ints.saturatedCast(aggregateForEntries(Aggregate.DISTINCT));
    }

    public int count(@Nullable Object element) {
        E e = element;
        try {
            AvlNode<E> root = (AvlNode) this.rootReference.get();
            if (this.range.contains(e)) {
                if (root != null) {
                    return root.count(comparator(), e);
                }
            }
            return 0;
        } catch (ClassCastException e2) {
            return 0;
        } catch (NullPointerException e3) {
            return 0;
        }
    }

    public int add(@Nullable E element, int occurrences) {
        CollectPreconditions.checkNonnegative(occurrences, "occurrences");
        if (occurrences == 0) {
            return count(element);
        }
        Preconditions.checkArgument(this.range.contains(element));
        AvlNode<E> root = (AvlNode) this.rootReference.get();
        if (root == null) {
            comparator().compare(element, element);
            AvlNode<E> newRoot = new AvlNode(element, occurrences);
            successor(this.header, newRoot, this.header);
            this.rootReference.checkAndSet(root, newRoot);
            return 0;
        }
        int[] result = new int[1];
        this.rootReference.checkAndSet(root, root.add(comparator(), element, occurrences, result));
        return result[0];
    }

    public int remove(@Nullable Object element, int occurrences) {
        CollectPreconditions.checkNonnegative(occurrences, "occurrences");
        if (occurrences == 0) {
            return count(element);
        }
        AvlNode<E> root = (AvlNode) this.rootReference.get();
        int[] result = new int[1];
        E e = element;
        try {
            if (this.range.contains(e)) {
                if (root != null) {
                    this.rootReference.checkAndSet(root, root.remove(comparator(), e, occurrences, result));
                    return result[0];
                }
            }
            return 0;
        } catch (ClassCastException e2) {
            return 0;
        } catch (NullPointerException e3) {
            return 0;
        }
    }

    public int setCount(@Nullable E element, int count) {
        CollectPreconditions.checkNonnegative(count, "count");
        boolean result = true;
        if (this.range.contains(element)) {
            AvlNode<E> root = (AvlNode) this.rootReference.get();
            if (root == null) {
                if (count > 0) {
                    add(element, count);
                }
                return 0;
            }
            int[] result2 = new int[1];
            this.rootReference.checkAndSet(root, root.setCount(comparator(), element, count, result2));
            return result2[0];
        }
        if (count != 0) {
            result = false;
        }
        Preconditions.checkArgument(result);
        return 0;
    }

    public boolean setCount(@Nullable E element, int oldCount, int newCount) {
        CollectPreconditions.checkNonnegative(newCount, "newCount");
        CollectPreconditions.checkNonnegative(oldCount, "oldCount");
        Preconditions.checkArgument(this.range.contains(element));
        AvlNode<E> root = (AvlNode) this.rootReference.get();
        boolean z = false;
        if (root != null) {
            int[] result = new int[1];
            this.rootReference.checkAndSet(root, root.setCount(comparator(), element, oldCount, newCount, result));
            if (result[0] == oldCount) {
                z = true;
            }
            return z;
        } else if (oldCount != 0) {
            return false;
        } else {
            if (newCount > 0) {
                add(element, newCount);
            }
            return true;
        }
    }

    private Entry<E> wrapEntry(final AvlNode<E> baseEntry) {
        return new AbstractEntry<E>() {
            public E getElement() {
                return baseEntry.getElement();
            }

            public int getCount() {
                int result = baseEntry.getCount();
                if (result == 0) {
                    return TreeMultiset.this.count(getElement());
                }
                return result;
            }
        };
    }

    @Nullable
    private AvlNode<E> firstNode() {
        AvlNode<E> avlNode = null;
        if (((AvlNode) this.rootReference.get()) == null) {
            return null;
        }
        AvlNode<E> node;
        if (this.range.hasLowerBound()) {
            E endpoint = this.range.getLowerEndpoint();
            node = ((AvlNode) this.rootReference.get()).ceiling(comparator(), endpoint);
            if (node == null) {
                return null;
            }
            if (this.range.getLowerBoundType() == BoundType.OPEN && comparator().compare(endpoint, node.getElement()) == 0) {
                node = node.succ;
            }
        } else {
            node = this.header.succ;
        }
        AvlNode<E> node2 = node;
        if (node2 != this.header && this.range.contains(node2.getElement())) {
            avlNode = node2;
        }
        return avlNode;
    }

    @Nullable
    private AvlNode<E> lastNode() {
        AvlNode<E> avlNode = null;
        if (((AvlNode) this.rootReference.get()) == null) {
            return null;
        }
        AvlNode<E> node;
        if (this.range.hasUpperBound()) {
            E endpoint = this.range.getUpperEndpoint();
            node = ((AvlNode) this.rootReference.get()).floor(comparator(), endpoint);
            if (node == null) {
                return null;
            }
            if (this.range.getUpperBoundType() == BoundType.OPEN && comparator().compare(endpoint, node.getElement()) == 0) {
                node = node.pred;
            }
        } else {
            node = this.header.pred;
        }
        AvlNode<E> node2 = node;
        if (node2 != this.header && this.range.contains(node2.getElement())) {
            avlNode = node2;
        }
        return avlNode;
    }

    /* Access modifiers changed, original: 0000 */
    public Iterator<Entry<E>> entryIterator() {
        return new Iterator<Entry<E>>() {
            AvlNode<E> current = TreeMultiset.this.firstNode();
            Entry<E> prevEntry;

            public boolean hasNext() {
                if (this.current == null) {
                    return false;
                }
                if (!TreeMultiset.this.range.tooHigh(this.current.getElement())) {
                    return true;
                }
                this.current = null;
                return false;
            }

            public Entry<E> next() {
                if (hasNext()) {
                    Entry<E> result = TreeMultiset.this.wrapEntry(this.current);
                    this.prevEntry = result;
                    if (this.current.succ == TreeMultiset.this.header) {
                        this.current = null;
                    } else {
                        this.current = this.current.succ;
                    }
                    return result;
                }
                throw new NoSuchElementException();
            }

            public void remove() {
                CollectPreconditions.checkRemove(this.prevEntry != null);
                TreeMultiset.this.setCount(this.prevEntry.getElement(), 0);
                this.prevEntry = null;
            }
        };
    }

    /* Access modifiers changed, original: 0000 */
    public Iterator<Entry<E>> descendingEntryIterator() {
        return new Iterator<Entry<E>>() {
            AvlNode<E> current = TreeMultiset.this.lastNode();
            Entry<E> prevEntry = null;

            public boolean hasNext() {
                if (this.current == null) {
                    return false;
                }
                if (!TreeMultiset.this.range.tooLow(this.current.getElement())) {
                    return true;
                }
                this.current = null;
                return false;
            }

            public Entry<E> next() {
                if (hasNext()) {
                    Entry<E> result = TreeMultiset.this.wrapEntry(this.current);
                    this.prevEntry = result;
                    if (this.current.pred == TreeMultiset.this.header) {
                        this.current = null;
                    } else {
                        this.current = this.current.pred;
                    }
                    return result;
                }
                throw new NoSuchElementException();
            }

            public void remove() {
                CollectPreconditions.checkRemove(this.prevEntry != null);
                TreeMultiset.this.setCount(this.prevEntry.getElement(), 0);
                this.prevEntry = null;
            }
        };
    }

    public SortedMultiset<E> headMultiset(@Nullable E upperBound, BoundType boundType) {
        return new TreeMultiset(this.rootReference, this.range.intersect(GeneralRange.upTo(comparator(), upperBound, boundType)), this.header);
    }

    public SortedMultiset<E> tailMultiset(@Nullable E lowerBound, BoundType boundType) {
        return new TreeMultiset(this.rootReference, this.range.intersect(GeneralRange.downTo(comparator(), lowerBound, boundType)), this.header);
    }

    static int distinctElements(@Nullable AvlNode<?> node) {
        return node == null ? 0 : node.distinctElements;
    }

    private static <T> void successor(AvlNode<T> a, AvlNode<T> b) {
        a.succ = b;
        b.pred = a;
    }

    private static <T> void successor(AvlNode<T> a, AvlNode<T> b, AvlNode<T> c) {
        successor(a, b);
        successor(b, c);
    }

    @GwtIncompatible("java.io.ObjectOutputStream")
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        stream.writeObject(elementSet().comparator());
        Serialization.writeMultiset(this, stream);
    }

    @GwtIncompatible("java.io.ObjectInputStream")
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        Object comparator = (Comparator) stream.readObject();
        Serialization.getFieldSetter(AbstractSortedMultiset.class, "comparator").set((Object) this, comparator);
        Serialization.getFieldSetter(TreeMultiset.class, "range").set((Object) this, GeneralRange.all(comparator));
        Serialization.getFieldSetter(TreeMultiset.class, "rootReference").set((Object) this, new Reference());
        Object header = new AvlNode(null, 1);
        Serialization.getFieldSetter(TreeMultiset.class, "header").set((Object) this, header);
        successor(header, header);
        Serialization.populateMultiset(this, stream);
    }
}
