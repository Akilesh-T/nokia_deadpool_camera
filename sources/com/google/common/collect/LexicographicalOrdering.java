package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import java.io.Serializable;
import java.util.Iterator;
import javax.annotation.Nullable;

@GwtCompatible(serializable = true)
final class LexicographicalOrdering<T> extends Ordering<Iterable<T>> implements Serializable {
    private static final long serialVersionUID = 0;
    final Ordering<? super T> elementOrder;

    LexicographicalOrdering(Ordering<? super T> elementOrder) {
        this.elementOrder = elementOrder;
    }

    public int compare(Iterable<T> leftIterable, Iterable<T> rightIterable) {
        Iterator<T> right = rightIterable.iterator();
        for (T compare : leftIterable) {
            if (!right.hasNext()) {
                return 1;
            }
            int result = this.elementOrder.compare(compare, right.next());
            if (result != 0) {
                return result;
            }
        }
        if (right.hasNext()) {
            return -1;
        }
        return 0;
    }

    public boolean equals(@Nullable Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof LexicographicalOrdering)) {
            return false;
        }
        return this.elementOrder.equals(((LexicographicalOrdering) object).elementOrder);
    }

    public int hashCode() {
        return this.elementOrder.hashCode() ^ 2075626741;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.elementOrder);
        stringBuilder.append(".lexicographical()");
        return stringBuilder.toString();
    }
}
