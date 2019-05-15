package com.google.common.collect;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Multiset.Entry;
import com.google.common.primitives.Ints;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.annotation.Nullable;

@GwtCompatible
public final class Multisets {
    private static final Ordering<Entry<?>> DECREASING_COUNT_ORDERING = new Ordering<Entry<?>>() {
        public int compare(Entry<?> entry1, Entry<?> entry2) {
            return Ints.compare(entry2.getCount(), entry1.getCount());
        }
    };

    static final class MultisetIteratorImpl<E> implements Iterator<E> {
        private boolean canRemove;
        private Entry<E> currentEntry;
        private final Iterator<Entry<E>> entryIterator;
        private int laterCount;
        private final Multiset<E> multiset;
        private int totalCount;

        MultisetIteratorImpl(Multiset<E> multiset, Iterator<Entry<E>> entryIterator) {
            this.multiset = multiset;
            this.entryIterator = entryIterator;
        }

        public boolean hasNext() {
            return this.laterCount > 0 || this.entryIterator.hasNext();
        }

        public E next() {
            if (hasNext()) {
                if (this.laterCount == 0) {
                    this.currentEntry = (Entry) this.entryIterator.next();
                    int count = this.currentEntry.getCount();
                    this.laterCount = count;
                    this.totalCount = count;
                }
                this.laterCount--;
                this.canRemove = true;
                return this.currentEntry.getElement();
            }
            throw new NoSuchElementException();
        }

        public void remove() {
            CollectPreconditions.checkRemove(this.canRemove);
            if (this.totalCount == 1) {
                this.entryIterator.remove();
            } else {
                this.multiset.remove(this.currentEntry.getElement());
            }
            this.totalCount--;
            this.canRemove = false;
        }
    }

    static abstract class AbstractEntry<E> implements Entry<E> {
        AbstractEntry() {
        }

        public boolean equals(@Nullable Object object) {
            boolean z = false;
            if (!(object instanceof Entry)) {
                return false;
            }
            Entry<?> that = (Entry) object;
            if (getCount() == that.getCount() && Objects.equal(getElement(), that.getElement())) {
                z = true;
            }
            return z;
        }

        public int hashCode() {
            E e = getElement();
            return (e == null ? 0 : e.hashCode()) ^ getCount();
        }

        public String toString() {
            String text = String.valueOf(getElement());
            int n = getCount();
            if (n == 1) {
                return text;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(text);
            stringBuilder.append(" x ");
            stringBuilder.append(n);
            return stringBuilder.toString();
        }
    }

    static abstract class ElementSet<E> extends ImprovedAbstractSet<E> {
        public abstract Multiset<E> multiset();

        ElementSet() {
        }

        public void clear() {
            multiset().clear();
        }

        public boolean contains(Object o) {
            return multiset().contains(o);
        }

        public boolean containsAll(Collection<?> c) {
            return multiset().containsAll(c);
        }

        public boolean isEmpty() {
            return multiset().isEmpty();
        }

        public Iterator<E> iterator() {
            return new TransformedIterator<Entry<E>, E>(multiset().entrySet().iterator()) {
                /* Access modifiers changed, original: 0000 */
                public E transform(Entry<E> entry) {
                    return entry.getElement();
                }
            };
        }

        public boolean remove(Object o) {
            int count = multiset().count(o);
            if (count <= 0) {
                return false;
            }
            multiset().remove(o, count);
            return true;
        }

        public int size() {
            return multiset().entrySet().size();
        }
    }

    static abstract class EntrySet<E> extends ImprovedAbstractSet<Entry<E>> {
        public abstract Multiset<E> multiset();

        EntrySet() {
        }

        public boolean contains(@Nullable Object o) {
            boolean z = false;
            if (!(o instanceof Entry)) {
                return false;
            }
            Entry<?> entry = (Entry) o;
            if (entry.getCount() <= 0) {
                return false;
            }
            if (multiset().count(entry.getElement()) == entry.getCount()) {
                z = true;
            }
            return z;
        }

        public boolean remove(Object object) {
            if (object instanceof Entry) {
                Entry<?> entry = (Entry) object;
                Object element = entry.getElement();
                int entryCount = entry.getCount();
                if (entryCount != 0) {
                    return multiset().setCount(element, entryCount, 0);
                }
            }
            return false;
        }

        public void clear() {
            multiset().clear();
        }
    }

    private static final class FilteredMultiset<E> extends AbstractMultiset<E> {
        final Predicate<? super E> predicate;
        final Multiset<E> unfiltered;

        FilteredMultiset(Multiset<E> unfiltered, Predicate<? super E> predicate) {
            this.unfiltered = (Multiset) Preconditions.checkNotNull(unfiltered);
            this.predicate = (Predicate) Preconditions.checkNotNull(predicate);
        }

        public UnmodifiableIterator<E> iterator() {
            return Iterators.filter(this.unfiltered.iterator(), this.predicate);
        }

        /* Access modifiers changed, original: 0000 */
        public Set<E> createElementSet() {
            return Sets.filter(this.unfiltered.elementSet(), this.predicate);
        }

        /* Access modifiers changed, original: 0000 */
        public Set<Entry<E>> createEntrySet() {
            return Sets.filter(this.unfiltered.entrySet(), new Predicate<Entry<E>>() {
                public boolean apply(Entry<E> entry) {
                    return FilteredMultiset.this.predicate.apply(entry.getElement());
                }
            });
        }

        /* Access modifiers changed, original: 0000 */
        public Iterator<Entry<E>> entryIterator() {
            throw new AssertionError("should never be called");
        }

        /* Access modifiers changed, original: 0000 */
        public int distinctElements() {
            return elementSet().size();
        }

        public int count(@Nullable Object element) {
            int count = this.unfiltered.count(element);
            int i = 0;
            if (count <= 0) {
                return 0;
            }
            if (this.predicate.apply(element)) {
                i = count;
            }
            return i;
        }

        public int add(@Nullable E element, int occurrences) {
            Preconditions.checkArgument(this.predicate.apply(element), "Element %s does not match predicate %s", element, this.predicate);
            return this.unfiltered.add(element, occurrences);
        }

        public int remove(@Nullable Object element, int occurrences) {
            CollectPreconditions.checkNonnegative(occurrences, "occurrences");
            if (occurrences == 0) {
                return count(element);
            }
            return contains(element) ? this.unfiltered.remove(element, occurrences) : 0;
        }

        public void clear() {
            elementSet().clear();
        }
    }

    static final class ImmutableEntry<E> extends AbstractEntry<E> implements Serializable {
        private static final long serialVersionUID = 0;
        final int count;
        @Nullable
        final E element;

        ImmutableEntry(@Nullable E element, int count) {
            this.element = element;
            this.count = count;
            CollectPreconditions.checkNonnegative(count, "count");
        }

        @Nullable
        public E getElement() {
            return this.element;
        }

        public int getCount() {
            return this.count;
        }
    }

    static class UnmodifiableMultiset<E> extends ForwardingMultiset<E> implements Serializable {
        private static final long serialVersionUID = 0;
        final Multiset<? extends E> delegate;
        transient Set<E> elementSet;
        transient Set<Entry<E>> entrySet;

        UnmodifiableMultiset(Multiset<? extends E> delegate) {
            this.delegate = delegate;
        }

        /* Access modifiers changed, original: protected */
        public Multiset<E> delegate() {
            return this.delegate;
        }

        /* Access modifiers changed, original: 0000 */
        public Set<E> createElementSet() {
            return Collections.unmodifiableSet(this.delegate.elementSet());
        }

        public Set<E> elementSet() {
            Set<E> es = this.elementSet;
            if (es != null) {
                return es;
            }
            Set<E> createElementSet = createElementSet();
            this.elementSet = createElementSet;
            return createElementSet;
        }

        public Set<Entry<E>> entrySet() {
            Set<Entry<E>> es = this.entrySet;
            if (es != null) {
                return es;
            }
            Set<Entry<E>> unmodifiableSet = Collections.unmodifiableSet(this.delegate.entrySet());
            this.entrySet = unmodifiableSet;
            return unmodifiableSet;
        }

        public Iterator<E> iterator() {
            return Iterators.unmodifiableIterator(this.delegate.iterator());
        }

        public boolean add(E e) {
            throw new UnsupportedOperationException();
        }

        public int add(E e, int occurences) {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(Collection<? extends E> collection) {
            throw new UnsupportedOperationException();
        }

        public boolean remove(Object element) {
            throw new UnsupportedOperationException();
        }

        public int remove(Object element, int occurrences) {
            throw new UnsupportedOperationException();
        }

        public boolean removeAll(Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        public boolean retainAll(Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        public void clear() {
            throw new UnsupportedOperationException();
        }

        public int setCount(E e, int count) {
            throw new UnsupportedOperationException();
        }

        public boolean setCount(E e, int oldCount, int newCount) {
            throw new UnsupportedOperationException();
        }
    }

    private Multisets() {
    }

    public static <E> Multiset<E> unmodifiableMultiset(Multiset<? extends E> multiset) {
        if ((multiset instanceof UnmodifiableMultiset) || (multiset instanceof ImmutableMultiset)) {
            return multiset;
        }
        return new UnmodifiableMultiset((Multiset) Preconditions.checkNotNull(multiset));
    }

    @Deprecated
    public static <E> Multiset<E> unmodifiableMultiset(ImmutableMultiset<E> multiset) {
        return (Multiset) Preconditions.checkNotNull(multiset);
    }

    @Beta
    public static <E> SortedMultiset<E> unmodifiableSortedMultiset(SortedMultiset<E> sortedMultiset) {
        return new UnmodifiableSortedMultiset((SortedMultiset) Preconditions.checkNotNull(sortedMultiset));
    }

    public static <E> Entry<E> immutableEntry(@Nullable E e, int n) {
        return new ImmutableEntry(e, n);
    }

    @Beta
    public static <E> Multiset<E> filter(Multiset<E> unfiltered, Predicate<? super E> predicate) {
        if (!(unfiltered instanceof FilteredMultiset)) {
            return new FilteredMultiset(unfiltered, predicate);
        }
        FilteredMultiset<E> filtered = (FilteredMultiset) unfiltered;
        return new FilteredMultiset(filtered.unfiltered, Predicates.and(filtered.predicate, predicate));
    }

    static int inferDistinctElements(Iterable<?> elements) {
        if (elements instanceof Multiset) {
            return ((Multiset) elements).elementSet().size();
        }
        return 11;
    }

    @Beta
    public static <E> Multiset<E> union(final Multiset<? extends E> multiset1, final Multiset<? extends E> multiset2) {
        Preconditions.checkNotNull(multiset1);
        Preconditions.checkNotNull(multiset2);
        return new AbstractMultiset<E>() {
            public boolean contains(@Nullable Object element) {
                return multiset1.contains(element) || multiset2.contains(element);
            }

            public boolean isEmpty() {
                return multiset1.isEmpty() && multiset2.isEmpty();
            }

            public int count(Object element) {
                return Math.max(multiset1.count(element), multiset2.count(element));
            }

            /* Access modifiers changed, original: 0000 */
            public Set<E> createElementSet() {
                return Sets.union(multiset1.elementSet(), multiset2.elementSet());
            }

            /* Access modifiers changed, original: 0000 */
            public Iterator<Entry<E>> entryIterator() {
                final Iterator<? extends Entry<? extends E>> iterator1 = multiset1.entrySet().iterator();
                final Iterator<? extends Entry<? extends E>> iterator2 = multiset2.entrySet().iterator();
                return new AbstractIterator<Entry<E>>() {
                    /* Access modifiers changed, original: protected */
                    public Entry<E> computeNext() {
                        Entry<? extends E> entry1;
                        E element;
                        if (iterator1.hasNext()) {
                            entry1 = (Entry) iterator1.next();
                            element = entry1.getElement();
                            return Multisets.immutableEntry(element, Math.max(entry1.getCount(), multiset2.count(element)));
                        }
                        while (iterator2.hasNext()) {
                            entry1 = (Entry) iterator2.next();
                            element = entry1.getElement();
                            if (!multiset1.contains(element)) {
                                return Multisets.immutableEntry(element, entry1.getCount());
                            }
                        }
                        return (Entry) endOfData();
                    }
                };
            }

            /* Access modifiers changed, original: 0000 */
            public int distinctElements() {
                return elementSet().size();
            }
        };
    }

    public static <E> Multiset<E> intersection(final Multiset<E> multiset1, final Multiset<?> multiset2) {
        Preconditions.checkNotNull(multiset1);
        Preconditions.checkNotNull(multiset2);
        return new AbstractMultiset<E>() {
            public int count(Object element) {
                int count1 = multiset1.count(element);
                return count1 == 0 ? 0 : Math.min(count1, multiset2.count(element));
            }

            /* Access modifiers changed, original: 0000 */
            public Set<E> createElementSet() {
                return Sets.intersection(multiset1.elementSet(), multiset2.elementSet());
            }

            /* Access modifiers changed, original: 0000 */
            public Iterator<Entry<E>> entryIterator() {
                final Iterator<Entry<E>> iterator1 = multiset1.entrySet().iterator();
                return new AbstractIterator<Entry<E>>() {
                    /* Access modifiers changed, original: protected */
                    public Entry<E> computeNext() {
                        while (iterator1.hasNext()) {
                            Entry<E> entry1 = (Entry) iterator1.next();
                            E element = entry1.getElement();
                            int count = Math.min(entry1.getCount(), multiset2.count(element));
                            if (count > 0) {
                                return Multisets.immutableEntry(element, count);
                            }
                        }
                        return (Entry) endOfData();
                    }
                };
            }

            /* Access modifiers changed, original: 0000 */
            public int distinctElements() {
                return elementSet().size();
            }
        };
    }

    @Beta
    public static <E> Multiset<E> sum(final Multiset<? extends E> multiset1, final Multiset<? extends E> multiset2) {
        Preconditions.checkNotNull(multiset1);
        Preconditions.checkNotNull(multiset2);
        return new AbstractMultiset<E>() {
            public boolean contains(@Nullable Object element) {
                return multiset1.contains(element) || multiset2.contains(element);
            }

            public boolean isEmpty() {
                return multiset1.isEmpty() && multiset2.isEmpty();
            }

            public int size() {
                return multiset1.size() + multiset2.size();
            }

            public int count(Object element) {
                return multiset1.count(element) + multiset2.count(element);
            }

            /* Access modifiers changed, original: 0000 */
            public Set<E> createElementSet() {
                return Sets.union(multiset1.elementSet(), multiset2.elementSet());
            }

            /* Access modifiers changed, original: 0000 */
            public Iterator<Entry<E>> entryIterator() {
                final Iterator<? extends Entry<? extends E>> iterator1 = multiset1.entrySet().iterator();
                final Iterator<? extends Entry<? extends E>> iterator2 = multiset2.entrySet().iterator();
                return new AbstractIterator<Entry<E>>() {
                    /* Access modifiers changed, original: protected */
                    public Entry<E> computeNext() {
                        Entry<? extends E> entry1;
                        E element;
                        if (iterator1.hasNext()) {
                            entry1 = (Entry) iterator1.next();
                            element = entry1.getElement();
                            return Multisets.immutableEntry(element, entry1.getCount() + multiset2.count(element));
                        }
                        while (iterator2.hasNext()) {
                            entry1 = (Entry) iterator2.next();
                            element = entry1.getElement();
                            if (!multiset1.contains(element)) {
                                return Multisets.immutableEntry(element, entry1.getCount());
                            }
                        }
                        return (Entry) endOfData();
                    }
                };
            }

            /* Access modifiers changed, original: 0000 */
            public int distinctElements() {
                return elementSet().size();
            }
        };
    }

    @Beta
    public static <E> Multiset<E> difference(final Multiset<E> multiset1, final Multiset<?> multiset2) {
        Preconditions.checkNotNull(multiset1);
        Preconditions.checkNotNull(multiset2);
        return new AbstractMultiset<E>() {
            public int count(@Nullable Object element) {
                int count1 = multiset1.count(element);
                if (count1 == 0) {
                    return 0;
                }
                return Math.max(0, count1 - multiset2.count(element));
            }

            /* Access modifiers changed, original: 0000 */
            public Iterator<Entry<E>> entryIterator() {
                final Iterator<Entry<E>> iterator1 = multiset1.entrySet().iterator();
                return new AbstractIterator<Entry<E>>() {
                    /* Access modifiers changed, original: protected */
                    public Entry<E> computeNext() {
                        while (iterator1.hasNext()) {
                            Entry<E> entry1 = (Entry) iterator1.next();
                            E element = entry1.getElement();
                            int count = entry1.getCount() - multiset2.count(element);
                            if (count > 0) {
                                return Multisets.immutableEntry(element, count);
                            }
                        }
                        return (Entry) endOfData();
                    }
                };
            }

            /* Access modifiers changed, original: 0000 */
            public int distinctElements() {
                return Iterators.size(entryIterator());
            }
        };
    }

    public static boolean containsOccurrences(Multiset<?> superMultiset, Multiset<?> subMultiset) {
        Preconditions.checkNotNull(superMultiset);
        Preconditions.checkNotNull(subMultiset);
        for (Entry<?> entry : subMultiset.entrySet()) {
            if (superMultiset.count(entry.getElement()) < entry.getCount()) {
                return false;
            }
        }
        return true;
    }

    public static boolean retainOccurrences(Multiset<?> multisetToModify, Multiset<?> multisetToRetain) {
        return retainOccurrencesImpl(multisetToModify, multisetToRetain);
    }

    private static <E> boolean retainOccurrencesImpl(Multiset<E> multisetToModify, Multiset<?> occurrencesToRetain) {
        Preconditions.checkNotNull(multisetToModify);
        Preconditions.checkNotNull(occurrencesToRetain);
        Iterator<Entry<E>> entryIterator = multisetToModify.entrySet().iterator();
        boolean changed = false;
        while (entryIterator.hasNext()) {
            Entry<E> entry = (Entry) entryIterator.next();
            int retainCount = occurrencesToRetain.count(entry.getElement());
            if (retainCount == 0) {
                entryIterator.remove();
                changed = true;
            } else if (retainCount < entry.getCount()) {
                multisetToModify.setCount(entry.getElement(), retainCount);
                changed = true;
            }
        }
        return changed;
    }

    public static boolean removeOccurrences(Multiset<?> multisetToModify, Iterable<?> occurrencesToRemove) {
        if (occurrencesToRemove instanceof Multiset) {
            return removeOccurrencesImpl((Multiset) multisetToModify, (Multiset) occurrencesToRemove);
        }
        return removeOccurrencesImpl((Multiset) multisetToModify, (Iterable) occurrencesToRemove);
    }

    private static boolean removeOccurrencesImpl(Multiset<?> multisetToModify, Iterable<?> occurrencesToRemove) {
        Preconditions.checkNotNull(multisetToModify);
        Preconditions.checkNotNull(occurrencesToRemove);
        boolean changed = false;
        for (Object o : occurrencesToRemove) {
            changed |= multisetToModify.remove(o);
        }
        return changed;
    }

    private static <E> boolean removeOccurrencesImpl(Multiset<E> multisetToModify, Multiset<?> occurrencesToRemove) {
        Preconditions.checkNotNull(multisetToModify);
        Preconditions.checkNotNull(occurrencesToRemove);
        boolean changed = false;
        Iterator<Entry<E>> entryIterator = multisetToModify.entrySet().iterator();
        while (entryIterator.hasNext()) {
            Entry<E> entry = (Entry) entryIterator.next();
            int removeCount = occurrencesToRemove.count(entry.getElement());
            if (removeCount >= entry.getCount()) {
                entryIterator.remove();
                changed = true;
            } else if (removeCount > 0) {
                multisetToModify.remove(entry.getElement(), removeCount);
                changed = true;
            }
        }
        return changed;
    }

    static boolean equalsImpl(Multiset<?> multiset, @Nullable Object object) {
        if (object == multiset) {
            return true;
        }
        if (!(object instanceof Multiset)) {
            return false;
        }
        Multiset<?> that = (Multiset) object;
        if (multiset.size() != that.size() || multiset.entrySet().size() != that.entrySet().size()) {
            return false;
        }
        for (Entry<?> entry : that.entrySet()) {
            if (multiset.count(entry.getElement()) != entry.getCount()) {
                return false;
            }
        }
        return true;
    }

    static <E> boolean addAllImpl(Multiset<E> self, Collection<? extends E> elements) {
        if (elements.isEmpty()) {
            return false;
        }
        if (elements instanceof Multiset) {
            for (Entry<? extends E> entry : cast(elements).entrySet()) {
                self.add(entry.getElement(), entry.getCount());
            }
        } else {
            Iterators.addAll(self, elements.iterator());
        }
        return true;
    }

    static boolean removeAllImpl(Multiset<?> self, Collection<?> elementsToRemove) {
        return self.elementSet().removeAll(elementsToRemove instanceof Multiset ? ((Multiset) elementsToRemove).elementSet() : elementsToRemove);
    }

    static boolean retainAllImpl(Multiset<?> self, Collection<?> elementsToRetain) {
        Preconditions.checkNotNull(elementsToRetain);
        return self.elementSet().retainAll(elementsToRetain instanceof Multiset ? ((Multiset) elementsToRetain).elementSet() : elementsToRetain);
    }

    static <E> int setCountImpl(Multiset<E> self, E element, int count) {
        CollectPreconditions.checkNonnegative(count, "count");
        int oldCount = self.count(element);
        int delta = count - oldCount;
        if (delta > 0) {
            self.add(element, delta);
        } else if (delta < 0) {
            self.remove(element, -delta);
        }
        return oldCount;
    }

    static <E> boolean setCountImpl(Multiset<E> self, E element, int oldCount, int newCount) {
        CollectPreconditions.checkNonnegative(oldCount, "oldCount");
        CollectPreconditions.checkNonnegative(newCount, "newCount");
        if (self.count(element) != oldCount) {
            return false;
        }
        self.setCount(element, newCount);
        return true;
    }

    static <E> Iterator<E> iteratorImpl(Multiset<E> multiset) {
        return new MultisetIteratorImpl(multiset, multiset.entrySet().iterator());
    }

    static int sizeImpl(Multiset<?> multiset) {
        long size = 0;
        for (Entry<?> entry : multiset.entrySet()) {
            size += (long) entry.getCount();
        }
        return Ints.saturatedCast(size);
    }

    static <T> Multiset<T> cast(Iterable<T> iterable) {
        return (Multiset) iterable;
    }

    @Beta
    public static <E> ImmutableMultiset<E> copyHighestCountFirst(Multiset<E> multiset) {
        return ImmutableMultiset.copyFromEntries(DECREASING_COUNT_ORDERING.immutableSortedCopy(multiset.entrySet()));
    }
}
