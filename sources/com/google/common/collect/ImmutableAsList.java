package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;

@GwtCompatible(emulated = true, serializable = true)
abstract class ImmutableAsList<E> extends ImmutableList<E> {

    @GwtIncompatible("serialization")
    static class SerializedForm implements Serializable {
        private static final long serialVersionUID = 0;
        final ImmutableCollection<?> collection;

        SerializedForm(ImmutableCollection<?> collection) {
            this.collection = collection;
        }

        /* Access modifiers changed, original: 0000 */
        public Object readResolve() {
            return this.collection.asList();
        }
    }

    public abstract ImmutableCollection<E> delegateCollection();

    ImmutableAsList() {
    }

    public boolean contains(Object target) {
        return delegateCollection().contains(target);
    }

    public int size() {
        return delegateCollection().size();
    }

    public boolean isEmpty() {
        return delegateCollection().isEmpty();
    }

    /* Access modifiers changed, original: 0000 */
    public boolean isPartialView() {
        return delegateCollection().isPartialView();
    }

    @GwtIncompatible("serialization")
    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Use SerializedForm");
    }

    /* Access modifiers changed, original: 0000 */
    @GwtIncompatible("serialization")
    public Object writeReplace() {
        return new SerializedForm(delegateCollection());
    }
}
