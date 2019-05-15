package com.google.common.base;

import com.google.common.annotations.GwtCompatible;
import java.io.Serializable;
import java.util.Iterator;
import javax.annotation.Nullable;

@GwtCompatible(serializable = true)
final class PairwiseEquivalence<T> extends Equivalence<Iterable<T>> implements Serializable {
    private static final long serialVersionUID = 1;
    final Equivalence<? super T> elementEquivalence;

    PairwiseEquivalence(Equivalence<? super T> elementEquivalence) {
        this.elementEquivalence = (Equivalence) Preconditions.checkNotNull(elementEquivalence);
    }

    /* Access modifiers changed, original: protected */
    public boolean doEquivalent(Iterable<T> iterableA, Iterable<T> iterableB) {
        Iterator<T> iteratorA = iterableA.iterator();
        Iterator<T> iteratorB = iterableB.iterator();
        do {
            boolean z = false;
            if (!iteratorA.hasNext() || !iteratorB.hasNext()) {
                if (!(iteratorA.hasNext() || iteratorB.hasNext())) {
                    z = true;
                }
                return z;
            }
        } while (this.elementEquivalence.equivalent(iteratorA.next(), iteratorB.next()));
        return false;
    }

    /* Access modifiers changed, original: protected */
    public int doHash(Iterable<T> iterable) {
        int hash = 78721;
        for (T element : iterable) {
            hash = (hash * 24943) + this.elementEquivalence.hash(element);
        }
        return hash;
    }

    public boolean equals(@Nullable Object object) {
        if (!(object instanceof PairwiseEquivalence)) {
            return false;
        }
        return this.elementEquivalence.equals(((PairwiseEquivalence) object).elementEquivalence);
    }

    public int hashCode() {
        return this.elementEquivalence.hashCode() ^ 1185147655;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.elementEquivalence);
        stringBuilder.append(".pairwise()");
        return stringBuilder.toString();
    }
}
