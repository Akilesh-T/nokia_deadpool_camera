package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.RandomAccess;
import java.util.Set;
import javax.annotation.Nullable;

@GwtCompatible(emulated = true)
public final class Iterables {

    private static final class UnmodifiableIterable<T> extends FluentIterable<T> {
        private final Iterable<T> iterable;

        /* synthetic */ UnmodifiableIterable(Iterable x0, AnonymousClass1 x1) {
            this(x0);
        }

        private UnmodifiableIterable(Iterable<T> iterable) {
            this.iterable = iterable;
        }

        public Iterator<T> iterator() {
            return Iterators.unmodifiableIterator(this.iterable.iterator());
        }

        public String toString() {
            return this.iterable.toString();
        }
    }

    private static class ConsumingQueueIterator<T> extends AbstractIterator<T> {
        private final Queue<T> queue;

        /* synthetic */ ConsumingQueueIterator(Queue x0, AnonymousClass1 x1) {
            this(x0);
        }

        private ConsumingQueueIterator(Queue<T> queue) {
            this.queue = queue;
        }

        public T computeNext() {
            try {
                return this.queue.remove();
            } catch (NoSuchElementException e) {
                return endOfData();
            }
        }
    }

    private Iterables() {
    }

    public static <T> Iterable<T> unmodifiableIterable(Iterable<T> iterable) {
        Preconditions.checkNotNull(iterable);
        if ((iterable instanceof UnmodifiableIterable) || (iterable instanceof ImmutableCollection)) {
            return iterable;
        }
        return new UnmodifiableIterable(iterable, null);
    }

    @Deprecated
    public static <E> Iterable<E> unmodifiableIterable(ImmutableCollection<E> iterable) {
        return (Iterable) Preconditions.checkNotNull(iterable);
    }

    public static int size(Iterable<?> iterable) {
        if (iterable instanceof Collection) {
            return ((Collection) iterable).size();
        }
        return Iterators.size(iterable.iterator());
    }

    public static boolean contains(Iterable<?> iterable, @Nullable Object element) {
        if (iterable instanceof Collection) {
            return Collections2.safeContains((Collection) iterable, element);
        }
        return Iterators.contains(iterable.iterator(), element);
    }

    public static boolean removeAll(Iterable<?> removeFrom, Collection<?> elementsToRemove) {
        if (removeFrom instanceof Collection) {
            return ((Collection) removeFrom).removeAll((Collection) Preconditions.checkNotNull(elementsToRemove));
        }
        return Iterators.removeAll(removeFrom.iterator(), elementsToRemove);
    }

    public static boolean retainAll(Iterable<?> removeFrom, Collection<?> elementsToRetain) {
        if (removeFrom instanceof Collection) {
            return ((Collection) removeFrom).retainAll((Collection) Preconditions.checkNotNull(elementsToRetain));
        }
        return Iterators.retainAll(removeFrom.iterator(), elementsToRetain);
    }

    public static <T> boolean removeIf(Iterable<T> removeFrom, Predicate<? super T> predicate) {
        if ((removeFrom instanceof RandomAccess) && (removeFrom instanceof List)) {
            return removeIfFromRandomAccessList((List) removeFrom, (Predicate) Preconditions.checkNotNull(predicate));
        }
        return Iterators.removeIf(removeFrom.iterator(), predicate);
    }

    private static <T> boolean removeIfFromRandomAccessList(List<T> list, Predicate<? super T> predicate) {
        boolean z = false;
        int from = 0;
        int to = 0;
        while (from < list.size()) {
            T element = list.get(from);
            if (!predicate.apply(element)) {
                if (from > to) {
                    try {
                        list.set(to, element);
                    } catch (UnsupportedOperationException e) {
                        slowRemoveIfForRemainingElements(list, predicate, to, from);
                        return true;
                    }
                }
                to++;
            }
            from++;
        }
        list.subList(to, list.size()).clear();
        if (from != to) {
            z = true;
        }
        return z;
    }

    private static <T> void slowRemoveIfForRemainingElements(List<T> list, Predicate<? super T> predicate, int to, int from) {
        int n;
        for (n = list.size() - 1; n > from; n--) {
            if (predicate.apply(list.get(n))) {
                list.remove(n);
            }
        }
        for (n = from - 1; n >= to; n--) {
            list.remove(n);
        }
    }

    @Nullable
    static <T> T removeFirstMatching(Iterable<T> removeFrom, Predicate<? super T> predicate) {
        Preconditions.checkNotNull(predicate);
        Iterator<T> iterator = removeFrom.iterator();
        while (iterator.hasNext()) {
            T next = iterator.next();
            if (predicate.apply(next)) {
                iterator.remove();
                return next;
            }
        }
        return null;
    }

    public static boolean elementsEqual(Iterable<?> iterable1, Iterable<?> iterable2) {
        if ((iterable1 instanceof Collection) && (iterable2 instanceof Collection)) {
            if (((Collection) iterable1).size() != ((Collection) iterable2).size()) {
                return false;
            }
        }
        return Iterators.elementsEqual(iterable1.iterator(), iterable2.iterator());
    }

    public static String toString(Iterable<?> iterable) {
        return Iterators.toString(iterable.iterator());
    }

    public static <T> T getOnlyElement(Iterable<T> iterable) {
        return Iterators.getOnlyElement(iterable.iterator());
    }

    @Nullable
    public static <T> T getOnlyElement(Iterable<? extends T> iterable, @Nullable T defaultValue) {
        return Iterators.getOnlyElement(iterable.iterator(), defaultValue);
    }

    @GwtIncompatible("Array.newInstance(Class, int)")
    public static <T> T[] toArray(Iterable<? extends T> iterable, Class<T> type) {
        Collection<? extends T> collection = toCollection(iterable);
        return collection.toArray(ObjectArrays.newArray((Class) type, collection.size()));
    }

    static Object[] toArray(Iterable<?> iterable) {
        return toCollection(iterable).toArray();
    }

    private static <E> Collection<E> toCollection(Iterable<E> iterable) {
        if (iterable instanceof Collection) {
            return (Collection) iterable;
        }
        return Lists.newArrayList(iterable.iterator());
    }

    public static <T> boolean addAll(Collection<T> addTo, Iterable<? extends T> elementsToAdd) {
        if (elementsToAdd instanceof Collection) {
            return addTo.addAll(Collections2.cast(elementsToAdd));
        }
        return Iterators.addAll(addTo, ((Iterable) Preconditions.checkNotNull(elementsToAdd)).iterator());
    }

    public static int frequency(Iterable<?> iterable, @Nullable Object element) {
        if (iterable instanceof Multiset) {
            return ((Multiset) iterable).count(element);
        }
        if (iterable instanceof Set) {
            return ((Set) iterable).contains(element);
        }
        return Iterators.frequency(iterable.iterator(), element);
    }

    public static <T> Iterable<T> cycle(final Iterable<T> iterable) {
        Preconditions.checkNotNull(iterable);
        return new FluentIterable<T>() {
            public Iterator<T> iterator() {
                return Iterators.cycle(iterable);
            }

            public String toString() {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(iterable.toString());
                stringBuilder.append(" (cycled)");
                return stringBuilder.toString();
            }
        };
    }

    public static <T> Iterable<T> cycle(T... elements) {
        return cycle(Lists.newArrayList((Object[]) elements));
    }

    public static <T> Iterable<T> concat(Iterable<? extends T> a, Iterable<? extends T> b) {
        return concat(ImmutableList.of(a, b));
    }

    public static <T> Iterable<T> concat(Iterable<? extends T> a, Iterable<? extends T> b, Iterable<? extends T> c) {
        return concat(ImmutableList.of(a, b, c));
    }

    public static <T> Iterable<T> concat(Iterable<? extends T> a, Iterable<? extends T> b, Iterable<? extends T> c, Iterable<? extends T> d) {
        return concat(ImmutableList.of(a, b, c, d));
    }

    public static <T> Iterable<T> concat(Iterable<? extends T>... inputs) {
        return concat(ImmutableList.copyOf((Object[]) inputs));
    }

    public static <T> Iterable<T> concat(final Iterable<? extends Iterable<? extends T>> inputs) {
        Preconditions.checkNotNull(inputs);
        return new FluentIterable<T>() {
            public Iterator<T> iterator() {
                return Iterators.concat(Iterables.iterators(inputs));
            }
        };
    }

    private static <T> Iterator<Iterator<? extends T>> iterators(Iterable<? extends Iterable<? extends T>> iterables) {
        return new TransformedIterator<Iterable<? extends T>, Iterator<? extends T>>(iterables.iterator()) {
            /* Access modifiers changed, original: 0000 */
            public Iterator<? extends T> transform(Iterable<? extends T> from) {
                return from.iterator();
            }
        };
    }

    public static <T> Iterable<List<T>> partition(final Iterable<T> iterable, final int size) {
        Preconditions.checkNotNull(iterable);
        Preconditions.checkArgument(size > 0);
        return new FluentIterable<List<T>>() {
            public Iterator<List<T>> iterator() {
                return Iterators.partition(iterable.iterator(), size);
            }
        };
    }

    public static <T> Iterable<List<T>> paddedPartition(final Iterable<T> iterable, final int size) {
        Preconditions.checkNotNull(iterable);
        Preconditions.checkArgument(size > 0);
        return new FluentIterable<List<T>>() {
            public Iterator<List<T>> iterator() {
                return Iterators.paddedPartition(iterable.iterator(), size);
            }
        };
    }

    public static <T> Iterable<T> filter(final Iterable<T> unfiltered, final Predicate<? super T> predicate) {
        Preconditions.checkNotNull(unfiltered);
        Preconditions.checkNotNull(predicate);
        return new FluentIterable<T>() {
            public Iterator<T> iterator() {
                return Iterators.filter(unfiltered.iterator(), predicate);
            }
        };
    }

    @GwtIncompatible("Class.isInstance")
    public static <T> Iterable<T> filter(final Iterable<?> unfiltered, final Class<T> type) {
        Preconditions.checkNotNull(unfiltered);
        Preconditions.checkNotNull(type);
        return new FluentIterable<T>() {
            public Iterator<T> iterator() {
                return Iterators.filter(unfiltered.iterator(), type);
            }
        };
    }

    public static <T> boolean any(Iterable<T> iterable, Predicate<? super T> predicate) {
        return Iterators.any(iterable.iterator(), predicate);
    }

    public static <T> boolean all(Iterable<T> iterable, Predicate<? super T> predicate) {
        return Iterators.all(iterable.iterator(), predicate);
    }

    public static <T> T find(Iterable<T> iterable, Predicate<? super T> predicate) {
        return Iterators.find(iterable.iterator(), predicate);
    }

    @Nullable
    public static <T> T find(Iterable<? extends T> iterable, Predicate<? super T> predicate, @Nullable T defaultValue) {
        return Iterators.find(iterable.iterator(), predicate, defaultValue);
    }

    public static <T> Optional<T> tryFind(Iterable<T> iterable, Predicate<? super T> predicate) {
        return Iterators.tryFind(iterable.iterator(), predicate);
    }

    public static <T> int indexOf(Iterable<T> iterable, Predicate<? super T> predicate) {
        return Iterators.indexOf(iterable.iterator(), predicate);
    }

    public static <F, T> Iterable<T> transform(final Iterable<F> fromIterable, final Function<? super F, ? extends T> function) {
        Preconditions.checkNotNull(fromIterable);
        Preconditions.checkNotNull(function);
        return new FluentIterable<T>() {
            public Iterator<T> iterator() {
                return Iterators.transform(fromIterable.iterator(), function);
            }
        };
    }

    public static <T> T get(Iterable<T> iterable, int position) {
        Preconditions.checkNotNull(iterable);
        if (iterable instanceof List) {
            return ((List) iterable).get(position);
        }
        return Iterators.get(iterable.iterator(), position);
    }

    @Nullable
    public static <T> T get(Iterable<? extends T> iterable, int position, @Nullable T defaultValue) {
        Preconditions.checkNotNull(iterable);
        Iterators.checkNonnegative(position);
        if (iterable instanceof List) {
            List<? extends T> list = Lists.cast(iterable);
            return position < list.size() ? list.get(position) : defaultValue;
        }
        Iterator<? extends T> iterator = iterable.iterator();
        Iterators.advance(iterator, position);
        return Iterators.getNext(iterator, defaultValue);
    }

    @Nullable
    public static <T> T getFirst(Iterable<? extends T> iterable, @Nullable T defaultValue) {
        return Iterators.getNext(iterable.iterator(), defaultValue);
    }

    public static <T> T getLast(Iterable<T> iterable) {
        if (!(iterable instanceof List)) {
            return Iterators.getLast(iterable.iterator());
        }
        List<T> list = (List) iterable;
        if (!list.isEmpty()) {
            return getLastInNonemptyList(list);
        }
        throw new NoSuchElementException();
    }

    @Nullable
    public static <T> T getLast(Iterable<? extends T> iterable, @Nullable T defaultValue) {
        if (iterable instanceof Collection) {
            if (Collections2.cast(iterable).isEmpty()) {
                return defaultValue;
            }
            if (iterable instanceof List) {
                return getLastInNonemptyList(Lists.cast(iterable));
            }
        }
        return Iterators.getLast(iterable.iterator(), defaultValue);
    }

    private static <T> T getLastInNonemptyList(List<T> list) {
        return list.get(list.size() - 1);
    }

    public static <T> Iterable<T> skip(final Iterable<T> iterable, final int numberToSkip) {
        Preconditions.checkNotNull(iterable);
        Preconditions.checkArgument(numberToSkip >= 0, "number to skip cannot be negative");
        if (!(iterable instanceof List)) {
            return new FluentIterable<T>() {
                public Iterator<T> iterator() {
                    final Iterator<T> iterator = iterable.iterator();
                    Iterators.advance(iterator, numberToSkip);
                    return new Iterator<T>() {
                        boolean atStart = true;

                        public boolean hasNext() {
                            return iterator.hasNext();
                        }

                        public T next() {
                            T result = iterator.next();
                            this.atStart = false;
                            return result;
                        }

                        public void remove() {
                            CollectPreconditions.checkRemove(this.atStart ^ 1);
                            iterator.remove();
                        }
                    };
                }
            };
        }
        final List<T> list = (List) iterable;
        return new FluentIterable<T>() {
            public Iterator<T> iterator() {
                return list.subList(Math.min(list.size(), numberToSkip), list.size()).iterator();
            }
        };
    }

    public static <T> Iterable<T> limit(final Iterable<T> iterable, final int limitSize) {
        Preconditions.checkNotNull(iterable);
        Preconditions.checkArgument(limitSize >= 0, "limit is negative");
        return new FluentIterable<T>() {
            public Iterator<T> iterator() {
                return Iterators.limit(iterable.iterator(), limitSize);
            }
        };
    }

    public static <T> Iterable<T> consumingIterable(final Iterable<T> iterable) {
        if (iterable instanceof Queue) {
            return new FluentIterable<T>() {
                public Iterator<T> iterator() {
                    return new ConsumingQueueIterator((Queue) iterable, null);
                }

                public String toString() {
                    return "Iterables.consumingIterable(...)";
                }
            };
        }
        Preconditions.checkNotNull(iterable);
        return new FluentIterable<T>() {
            public Iterator<T> iterator() {
                return Iterators.consumingIterator(iterable.iterator());
            }

            public String toString() {
                return "Iterables.consumingIterable(...)";
            }
        };
    }

    public static boolean isEmpty(Iterable<?> iterable) {
        if (iterable instanceof Collection) {
            return ((Collection) iterable).isEmpty();
        }
        return iterable.iterator().hasNext() ^ 1;
    }

    @Beta
    public static <T> Iterable<T> mergeSorted(final Iterable<? extends Iterable<? extends T>> iterables, final Comparator<? super T> comparator) {
        Preconditions.checkNotNull(iterables, "iterables");
        Preconditions.checkNotNull(comparator, "comparator");
        return new UnmodifiableIterable(new FluentIterable<T>() {
            public Iterator<T> iterator() {
                return Iterators.mergeSorted(Iterables.transform(iterables, Iterables.toIterator()), comparator);
            }
        }, null);
    }

    private static <T> Function<Iterable<? extends T>, Iterator<? extends T>> toIterator() {
        return new Function<Iterable<? extends T>, Iterator<? extends T>>() {
            public Iterator<? extends T> apply(Iterable<? extends T> iterable) {
                return iterable.iterator();
            }
        };
    }
}
