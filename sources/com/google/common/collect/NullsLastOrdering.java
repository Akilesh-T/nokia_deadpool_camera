package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import java.io.Serializable;
import javax.annotation.Nullable;

@GwtCompatible(serializable = true)
final class NullsLastOrdering<T> extends Ordering<T> implements Serializable {
    private static final long serialVersionUID = 0;
    final Ordering<? super T> ordering;

    NullsLastOrdering(Ordering<? super T> ordering) {
        this.ordering = ordering;
    }

    public int compare(@Nullable T left, @Nullable T right) {
        if (left == right) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return this.ordering.compare(left, right);
    }

    public <S extends T> Ordering<S> reverse() {
        return this.ordering.reverse().nullsFirst();
    }

    public <S extends T> Ordering<S> nullsFirst() {
        return this.ordering.nullsFirst();
    }

    public <S extends T> Ordering<S> nullsLast() {
        return this;
    }

    public boolean equals(@Nullable Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof NullsLastOrdering)) {
            return false;
        }
        return this.ordering.equals(((NullsLastOrdering) object).ordering);
    }

    public int hashCode() {
        return this.ordering.hashCode() ^ -921210296;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.ordering);
        stringBuilder.append(".nullsLast()");
        return stringBuilder.toString();
    }
}
