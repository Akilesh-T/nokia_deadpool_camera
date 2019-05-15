package com.google.common.base;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import java.io.Serializable;
import javax.annotation.Nullable;

@GwtCompatible
@Beta
final class FunctionalEquivalence<F, T> extends Equivalence<F> implements Serializable {
    private static final long serialVersionUID = 0;
    private final Function<F, ? extends T> function;
    private final Equivalence<T> resultEquivalence;

    FunctionalEquivalence(Function<F, ? extends T> function, Equivalence<T> resultEquivalence) {
        this.function = (Function) Preconditions.checkNotNull(function);
        this.resultEquivalence = (Equivalence) Preconditions.checkNotNull(resultEquivalence);
    }

    /* Access modifiers changed, original: protected */
    public boolean doEquivalent(F a, F b) {
        return this.resultEquivalence.equivalent(this.function.apply(a), this.function.apply(b));
    }

    /* Access modifiers changed, original: protected */
    public int doHash(F a) {
        return this.resultEquivalence.hash(this.function.apply(a));
    }

    public boolean equals(@Nullable Object obj) {
        boolean z = true;
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof FunctionalEquivalence)) {
            return false;
        }
        FunctionalEquivalence<?, ?> that = (FunctionalEquivalence) obj;
        if (!(this.function.equals(that.function) && this.resultEquivalence.equals(that.resultEquivalence))) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return Objects.hashCode(this.function, this.resultEquivalence);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.resultEquivalence);
        stringBuilder.append(".onResultOf(");
        stringBuilder.append(this.function);
        stringBuilder.append(")");
        return stringBuilder.toString();
    }
}
