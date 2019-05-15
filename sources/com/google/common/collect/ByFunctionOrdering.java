package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import java.io.Serializable;
import javax.annotation.Nullable;

@GwtCompatible(serializable = true)
final class ByFunctionOrdering<F, T> extends Ordering<F> implements Serializable {
    private static final long serialVersionUID = 0;
    final Function<F, ? extends T> function;
    final Ordering<T> ordering;

    ByFunctionOrdering(Function<F, ? extends T> function, Ordering<T> ordering) {
        this.function = (Function) Preconditions.checkNotNull(function);
        this.ordering = (Ordering) Preconditions.checkNotNull(ordering);
    }

    public int compare(F left, F right) {
        return this.ordering.compare(this.function.apply(left), this.function.apply(right));
    }

    public boolean equals(@Nullable Object object) {
        boolean z = true;
        if (object == this) {
            return true;
        }
        if (!(object instanceof ByFunctionOrdering)) {
            return false;
        }
        ByFunctionOrdering<?, ?> that = (ByFunctionOrdering) object;
        if (!(this.function.equals(that.function) && this.ordering.equals(that.ordering))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return Objects.hashCode(this.function, this.ordering);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.ordering);
        stringBuilder.append(".onResultOf(");
        stringBuilder.append(this.function);
        stringBuilder.append(")");
        return stringBuilder.toString();
    }
}
