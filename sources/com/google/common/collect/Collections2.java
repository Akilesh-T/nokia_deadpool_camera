package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.math.IntMath;
import com.google.common.math.LongMath;
import com.hmdglobal.app.camera.provider.InfoTable;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;

@GwtCompatible
public final class Collections2 {
    static final Joiner STANDARD_JOINER = Joiner.on(", ").useForNull("null");

    static class FilteredCollection<E> extends AbstractCollection<E> {
        final Predicate<? super E> predicate;
        final Collection<E> unfiltered;

        FilteredCollection(Collection<E> unfiltered, Predicate<? super E> predicate) {
            this.unfiltered = unfiltered;
            this.predicate = predicate;
        }

        /* Access modifiers changed, original: 0000 */
        public FilteredCollection<E> createCombined(Predicate<? super E> newPredicate) {
            return new FilteredCollection(this.unfiltered, Predicates.and(this.predicate, newPredicate));
        }

        public boolean add(E element) {
            Preconditions.checkArgument(this.predicate.apply(element));
            return this.unfiltered.add(element);
        }

        public boolean addAll(Collection<? extends E> collection) {
            for (E element : collection) {
                Preconditions.checkArgument(this.predicate.apply(element));
            }
            return this.unfiltered.addAll(collection);
        }

        public void clear() {
            Iterables.removeIf(this.unfiltered, this.predicate);
        }

        public boolean contains(@Nullable Object element) {
            if (!Collections2.safeContains(this.unfiltered, element)) {
                return false;
            }
            return this.predicate.apply(element);
        }

        public boolean containsAll(Collection<?> collection) {
            return Collections2.containsAllImpl(this, collection);
        }

        public boolean isEmpty() {
            return Iterables.any(this.unfiltered, this.predicate) ^ 1;
        }

        public Iterator<E> iterator() {
            return Iterators.filter(this.unfiltered.iterator(), this.predicate);
        }

        public boolean remove(Object element) {
            return contains(element) && this.unfiltered.remove(element);
        }

        public boolean removeAll(Collection<?> collection) {
            return Iterables.removeIf(this.unfiltered, Predicates.and(this.predicate, Predicates.in(collection)));
        }

        public boolean retainAll(Collection<?> collection) {
            return Iterables.removeIf(this.unfiltered, Predicates.and(this.predicate, Predicates.not(Predicates.in(collection))));
        }

        public int size() {
            return Iterators.size(iterator());
        }

        public Object[] toArray() {
            return Lists.newArrayList(iterator()).toArray();
        }

        public <T> T[] toArray(T[] array) {
            return Lists.newArrayList(iterator()).toArray(array);
        }
    }

    private static final class OrderedPermutationCollection<E> extends AbstractCollection<List<E>> {
        final Comparator<? super E> comparator;
        final ImmutableList<E> inputList;
        final int size;

        OrderedPermutationCollection(Iterable<E> input, Comparator<? super E> comparator) {
            this.inputList = Ordering.from((Comparator) comparator).immutableSortedCopy(input);
            this.comparator = comparator;
            this.size = calculateSize(this.inputList, comparator);
        }

        private static <E> int calculateSize(List<E> sortedInputList, Comparator<? super E> comparator) {
            int n = 1;
            long permutations = 1;
            int r = 1;
            while (n < sortedInputList.size()) {
                if (comparator.compare(sortedInputList.get(n - 1), sortedInputList.get(n)) < 0) {
                    permutations *= LongMath.binomial(n, r);
                    r = 0;
                    if (!Collections2.isPositiveInt(permutations)) {
                        return ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED;
                    }
                }
                n++;
                r++;
            }
            permutations *= LongMath.binomial(n, r);
            if (Collections2.isPositiveInt(permutations)) {
                return (int) permutations;
            }
            return ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED;
        }

        public int size() {
            return this.size;
        }

        public boolean isEmpty() {
            return false;
        }

        public Iterator<List<E>> iterator() {
            return new OrderedPermutationIterator(this.inputList, this.comparator);
        }

        public boolean contains(@Nullable Object obj) {
            if (!(obj instanceof List)) {
                return false;
            }
            return Collections2.isPermutation(this.inputList, (List) obj);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("orderedPermutationCollection(");
            stringBuilder.append(this.inputList);
            stringBuilder.append(")");
            return stringBuilder.toString();
        }
    }

    private static final class PermutationCollection<E> extends AbstractCollection<List<E>> {
        final ImmutableList<E> inputList;

        PermutationCollection(ImmutableList<E> input) {
            this.inputList = input;
        }

        public int size() {
            return IntMath.factorial(this.inputList.size());
        }

        public boolean isEmpty() {
            return false;
        }

        public Iterator<List<E>> iterator() {
            return new PermutationIterator(this.inputList);
        }

        public boolean contains(@Nullable Object obj) {
            if (!(obj instanceof List)) {
                return false;
            }
            return Collections2.isPermutation(this.inputList, (List) obj);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("permutations(");
            stringBuilder.append(this.inputList);
            stringBuilder.append(")");
            return stringBuilder.toString();
        }
    }

    static class TransformedCollection<F, T> extends AbstractCollection<T> {
        final Collection<F> fromCollection;
        final Function<? super F, ? extends T> function;

        TransformedCollection(Collection<F> fromCollection, Function<? super F, ? extends T> function) {
            this.fromCollection = (Collection) Preconditions.checkNotNull(fromCollection);
            this.function = (Function) Preconditions.checkNotNull(function);
        }

        public void clear() {
            this.fromCollection.clear();
        }

        public boolean isEmpty() {
            return this.fromCollection.isEmpty();
        }

        public Iterator<T> iterator() {
            return Iterators.transform(this.fromCollection.iterator(), this.function);
        }

        public int size() {
            return this.fromCollection.size();
        }
    }

    private static final class OrderedPermutationIterator<E> extends AbstractIterator<List<E>> {
        final Comparator<? super E> comparator;
        List<E> nextPermutation;

        OrderedPermutationIterator(List<E> list, Comparator<? super E> comparator) {
            this.nextPermutation = Lists.newArrayList((Iterable) list);
            this.comparator = comparator;
        }

        /* Access modifiers changed, original: protected */
        public List<E> computeNext() {
            if (this.nextPermutation == null) {
                return (List) endOfData();
            }
            ImmutableList<E> next = ImmutableList.copyOf(this.nextPermutation);
            calculateNextPermutation();
            return next;
        }

        /* Access modifiers changed, original: 0000 */
        public void calculateNextPermutation() {
            int j = findNextJ();
            if (j == -1) {
                this.nextPermutation = null;
                return;
            }
            Collections.swap(this.nextPermutation, j, findNextL(j));
            Collections.reverse(this.nextPermutation.subList(j + 1, this.nextPermutation.size()));
        }

        /* Access modifiers changed, original: 0000 */
        public int findNextJ() {
            for (int k = this.nextPermutation.size() - 2; k >= 0; k--) {
                if (this.comparator.compare(this.nextPermutation.get(k), this.nextPermutation.get(k + 1)) < 0) {
                    return k;
                }
            }
            return -1;
        }

        /* Access modifiers changed, original: 0000 */
        public int findNextL(int j) {
            E ak = this.nextPermutation.get(j);
            for (int l = this.nextPermutation.size() - 1; l > j; l--) {
                if (this.comparator.compare(ak, this.nextPermutation.get(l)) < 0) {
                    return l;
                }
            }
            throw new AssertionError("this statement should be unreachable");
        }
    }

    private static class PermutationIterator<E> extends AbstractIterator<List<E>> {
        final int[] c;
        int j = ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED;
        final List<E> list;
        final int[] o;

        PermutationIterator(List<E> list) {
            this.list = new ArrayList(list);
            int n = list.size();
            this.c = new int[n];
            this.o = new int[n];
            Arrays.fill(this.c, 0);
            Arrays.fill(this.o, 1);
        }

        /* Access modifiers changed, original: protected */
        public List<E> computeNext() {
            if (this.j <= 0) {
                return (List) endOfData();
            }
            ImmutableList<E> next = ImmutableList.copyOf(this.list);
            calculateNextPermutation();
            return next;
        }

        /* Access modifiers changed, original: 0000 */
        public void calculateNextPermutation() {
            this.j = this.list.size() - 1;
            int s = 0;
            if (this.j != -1) {
                while (true) {
                    int q = this.c[this.j] + this.o[this.j];
                    if (q >= 0) {
                        if (q != this.j + 1) {
                            Collections.swap(this.list, (this.j - this.c[this.j]) + s, (this.j - q) + s);
                            this.c[this.j] = q;
                            break;
                        } else if (this.j == 0) {
                            break;
                        } else {
                            s++;
                            switchDirection();
                        }
                    } else {
                        switchDirection();
                    }
                }
            }
        }

        /* Access modifiers changed, original: 0000 */
        public void switchDirection() {
            this.o[this.j] = -this.o[this.j];
            this.j--;
        }
    }

    private Collections2() {
    }

    public static <E> Collection<E> filter(Collection<E> unfiltered, Predicate<? super E> predicate) {
        if (unfiltered instanceof FilteredCollection) {
            return ((FilteredCollection) unfiltered).createCombined(predicate);
        }
        return new FilteredCollection((Collection) Preconditions.checkNotNull(unfiltered), (Predicate) Preconditions.checkNotNull(predicate));
    }

    static boolean safeContains(Collection<?> collection, @Nullable Object object) {
        Preconditions.checkNotNull(collection);
        try {
            return collection.contains(object);
        } catch (ClassCastException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    static boolean safeRemove(Collection<?> collection, @Nullable Object object) {
        Preconditions.checkNotNull(collection);
        try {
            return collection.remove(object);
        } catch (ClassCastException e) {
            return false;
        } catch (NullPointerException e2) {
            return false;
        }
    }

    public static <F, T> Collection<T> transform(Collection<F> fromCollection, Function<? super F, T> function) {
        return new TransformedCollection(fromCollection, function);
    }

    static boolean containsAllImpl(Collection<?> self, Collection<?> c) {
        return Iterables.all(c, Predicates.in(self));
    }

    static String toStringImpl(final Collection<?> collection) {
        StringBuilder sb = newStringBuilderForCollection(collection.size()).append('[');
        STANDARD_JOINER.appendTo(sb, Iterables.transform(collection, new Function<Object, Object>() {
            public Object apply(Object input) {
                return input == collection ? "(this Collection)" : input;
            }
        }));
        sb.append(']');
        return sb.toString();
    }

    static StringBuilder newStringBuilderForCollection(int size) {
        CollectPreconditions.checkNonnegative(size, InfoTable.SIZE);
        return new StringBuilder((int) Math.min(((long) size) * 8, 1073741824));
    }

    static <T> Collection<T> cast(Iterable<T> iterable) {
        return (Collection) iterable;
    }

    @Beta
    public static <E extends Comparable<? super E>> Collection<List<E>> orderedPermutations(Iterable<E> elements) {
        return orderedPermutations(elements, Ordering.natural());
    }

    @Beta
    public static <E> Collection<List<E>> orderedPermutations(Iterable<E> elements, Comparator<? super E> comparator) {
        return new OrderedPermutationCollection(elements, comparator);
    }

    @Beta
    public static <E> Collection<List<E>> permutations(Collection<E> elements) {
        return new PermutationCollection(ImmutableList.copyOf((Collection) elements));
    }

    private static boolean isPermutation(List<?> first, List<?> second) {
        if (first.size() != second.size()) {
            return false;
        }
        return HashMultiset.create((Iterable) first).equals(HashMultiset.create((Iterable) second));
    }

    private static boolean isPositiveInt(long n) {
        return n >= 0 && n <= 2147483647L;
    }
}
