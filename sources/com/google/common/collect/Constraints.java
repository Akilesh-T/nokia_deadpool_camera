package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedSet;

@GwtCompatible
final class Constraints {

    static class ConstrainedCollection<E> extends ForwardingCollection<E> {
        private final Constraint<? super E> constraint;
        private final Collection<E> delegate;

        public ConstrainedCollection(Collection<E> delegate, Constraint<? super E> constraint) {
            this.delegate = (Collection) Preconditions.checkNotNull(delegate);
            this.constraint = (Constraint) Preconditions.checkNotNull(constraint);
        }

        /* Access modifiers changed, original: protected */
        public Collection<E> delegate() {
            return this.delegate;
        }

        public boolean add(E element) {
            this.constraint.checkElement(element);
            return this.delegate.add(element);
        }

        public boolean addAll(Collection<? extends E> elements) {
            return this.delegate.addAll(Constraints.checkElements(elements, this.constraint));
        }
    }

    @GwtCompatible
    private static class ConstrainedList<E> extends ForwardingList<E> {
        final Constraint<? super E> constraint;
        final List<E> delegate;

        ConstrainedList(List<E> delegate, Constraint<? super E> constraint) {
            this.delegate = (List) Preconditions.checkNotNull(delegate);
            this.constraint = (Constraint) Preconditions.checkNotNull(constraint);
        }

        /* Access modifiers changed, original: protected */
        public List<E> delegate() {
            return this.delegate;
        }

        public boolean add(E element) {
            this.constraint.checkElement(element);
            return this.delegate.add(element);
        }

        public void add(int index, E element) {
            this.constraint.checkElement(element);
            this.delegate.add(index, element);
        }

        public boolean addAll(Collection<? extends E> elements) {
            return this.delegate.addAll(Constraints.checkElements(elements, this.constraint));
        }

        public boolean addAll(int index, Collection<? extends E> elements) {
            return this.delegate.addAll(index, Constraints.checkElements(elements, this.constraint));
        }

        public ListIterator<E> listIterator() {
            return Constraints.constrainedListIterator(this.delegate.listIterator(), this.constraint);
        }

        public ListIterator<E> listIterator(int index) {
            return Constraints.constrainedListIterator(this.delegate.listIterator(index), this.constraint);
        }

        public E set(int index, E element) {
            this.constraint.checkElement(element);
            return this.delegate.set(index, element);
        }

        public List<E> subList(int fromIndex, int toIndex) {
            return Constraints.constrainedList(this.delegate.subList(fromIndex, toIndex), this.constraint);
        }
    }

    static class ConstrainedListIterator<E> extends ForwardingListIterator<E> {
        private final Constraint<? super E> constraint;
        private final ListIterator<E> delegate;

        public ConstrainedListIterator(ListIterator<E> delegate, Constraint<? super E> constraint) {
            this.delegate = delegate;
            this.constraint = constraint;
        }

        /* Access modifiers changed, original: protected */
        public ListIterator<E> delegate() {
            return this.delegate;
        }

        public void add(E element) {
            this.constraint.checkElement(element);
            this.delegate.add(element);
        }

        public void set(E element) {
            this.constraint.checkElement(element);
            this.delegate.set(element);
        }
    }

    static class ConstrainedSet<E> extends ForwardingSet<E> {
        private final Constraint<? super E> constraint;
        private final Set<E> delegate;

        public ConstrainedSet(Set<E> delegate, Constraint<? super E> constraint) {
            this.delegate = (Set) Preconditions.checkNotNull(delegate);
            this.constraint = (Constraint) Preconditions.checkNotNull(constraint);
        }

        /* Access modifiers changed, original: protected */
        public Set<E> delegate() {
            return this.delegate;
        }

        public boolean add(E element) {
            this.constraint.checkElement(element);
            return this.delegate.add(element);
        }

        public boolean addAll(Collection<? extends E> elements) {
            return this.delegate.addAll(Constraints.checkElements(elements, this.constraint));
        }
    }

    static class ConstrainedRandomAccessList<E> extends ConstrainedList<E> implements RandomAccess {
        ConstrainedRandomAccessList(List<E> delegate, Constraint<? super E> constraint) {
            super(delegate, constraint);
        }
    }

    private static class ConstrainedSortedSet<E> extends ForwardingSortedSet<E> {
        final Constraint<? super E> constraint;
        final SortedSet<E> delegate;

        ConstrainedSortedSet(SortedSet<E> delegate, Constraint<? super E> constraint) {
            this.delegate = (SortedSet) Preconditions.checkNotNull(delegate);
            this.constraint = (Constraint) Preconditions.checkNotNull(constraint);
        }

        /* Access modifiers changed, original: protected */
        public SortedSet<E> delegate() {
            return this.delegate;
        }

        public SortedSet<E> headSet(E toElement) {
            return Constraints.constrainedSortedSet(this.delegate.headSet(toElement), this.constraint);
        }

        public SortedSet<E> subSet(E fromElement, E toElement) {
            return Constraints.constrainedSortedSet(this.delegate.subSet(fromElement, toElement), this.constraint);
        }

        public SortedSet<E> tailSet(E fromElement) {
            return Constraints.constrainedSortedSet(this.delegate.tailSet(fromElement), this.constraint);
        }

        public boolean add(E element) {
            this.constraint.checkElement(element);
            return this.delegate.add(element);
        }

        public boolean addAll(Collection<? extends E> elements) {
            return this.delegate.addAll(Constraints.checkElements(elements, this.constraint));
        }
    }

    private Constraints() {
    }

    public static <E> Collection<E> constrainedCollection(Collection<E> collection, Constraint<? super E> constraint) {
        return new ConstrainedCollection(collection, constraint);
    }

    public static <E> Set<E> constrainedSet(Set<E> set, Constraint<? super E> constraint) {
        return new ConstrainedSet(set, constraint);
    }

    public static <E> SortedSet<E> constrainedSortedSet(SortedSet<E> sortedSet, Constraint<? super E> constraint) {
        return new ConstrainedSortedSet(sortedSet, constraint);
    }

    public static <E> List<E> constrainedList(List<E> list, Constraint<? super E> constraint) {
        if (list instanceof RandomAccess) {
            return new ConstrainedRandomAccessList(list, constraint);
        }
        return new ConstrainedList(list, constraint);
    }

    private static <E> ListIterator<E> constrainedListIterator(ListIterator<E> listIterator, Constraint<? super E> constraint) {
        return new ConstrainedListIterator(listIterator, constraint);
    }

    static <E> Collection<E> constrainedTypePreservingCollection(Collection<E> collection, Constraint<E> constraint) {
        if (collection instanceof SortedSet) {
            return constrainedSortedSet((SortedSet) collection, constraint);
        }
        if (collection instanceof Set) {
            return constrainedSet((Set) collection, constraint);
        }
        if (collection instanceof List) {
            return constrainedList((List) collection, constraint);
        }
        return constrainedCollection(collection, constraint);
    }

    private static <E> Collection<E> checkElements(Collection<E> elements, Constraint<? super E> constraint) {
        Collection<E> copy = Lists.newArrayList((Iterable) elements);
        for (E element : copy) {
            constraint.checkElement(element);
        }
        return copy;
    }
}
