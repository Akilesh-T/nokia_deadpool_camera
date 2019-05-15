package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.Comparator;
import javax.annotation.Nullable;

@GwtCompatible(serializable = true)
final class GeneralRange<T> implements Serializable {
    private final Comparator<? super T> comparator;
    private final boolean hasLowerBound;
    private final boolean hasUpperBound;
    private final BoundType lowerBoundType;
    @Nullable
    private final T lowerEndpoint;
    private transient GeneralRange<T> reverse;
    private final BoundType upperBoundType;
    @Nullable
    private final T upperEndpoint;

    static <T extends Comparable> GeneralRange<T> from(Range<T> range) {
        T t = null;
        T lowerEndpoint = range.hasLowerBound() ? range.lowerEndpoint() : null;
        BoundType lowerBoundType = range.hasLowerBound() ? range.lowerBoundType() : BoundType.OPEN;
        if (range.hasUpperBound()) {
            t = range.upperEndpoint();
        }
        return new GeneralRange(Ordering.natural(), range.hasLowerBound(), lowerEndpoint, lowerBoundType, range.hasUpperBound(), t, range.hasUpperBound() ? range.upperBoundType() : BoundType.OPEN);
    }

    static <T> GeneralRange<T> all(Comparator<? super T> comparator) {
        return new GeneralRange(comparator, false, null, BoundType.OPEN, false, null, BoundType.OPEN);
    }

    static <T> GeneralRange<T> downTo(Comparator<? super T> comparator, @Nullable T endpoint, BoundType boundType) {
        return new GeneralRange(comparator, true, endpoint, boundType, false, null, BoundType.OPEN);
    }

    static <T> GeneralRange<T> upTo(Comparator<? super T> comparator, @Nullable T endpoint, BoundType boundType) {
        return new GeneralRange(comparator, false, null, BoundType.OPEN, true, endpoint, boundType);
    }

    static <T> GeneralRange<T> range(Comparator<? super T> comparator, @Nullable T lower, BoundType lowerType, @Nullable T upper, BoundType upperType) {
        return new GeneralRange(comparator, true, lower, lowerType, true, upper, upperType);
    }

    private GeneralRange(Comparator<? super T> comparator, boolean hasLowerBound, @Nullable T lowerEndpoint, BoundType lowerBoundType, boolean hasUpperBound, @Nullable T upperEndpoint, BoundType upperBoundType) {
        this.comparator = (Comparator) Preconditions.checkNotNull(comparator);
        this.hasLowerBound = hasLowerBound;
        this.hasUpperBound = hasUpperBound;
        this.lowerEndpoint = lowerEndpoint;
        this.lowerBoundType = (BoundType) Preconditions.checkNotNull(lowerBoundType);
        this.upperEndpoint = upperEndpoint;
        this.upperBoundType = (BoundType) Preconditions.checkNotNull(upperBoundType);
        if (hasLowerBound) {
            comparator.compare(lowerEndpoint, lowerEndpoint);
        }
        if (hasUpperBound) {
            comparator.compare(upperEndpoint, upperEndpoint);
        }
        if (hasLowerBound && hasUpperBound) {
            int cmp = comparator.compare(lowerEndpoint, upperEndpoint);
            int i = 0;
            Preconditions.checkArgument(cmp <= 0, "lowerEndpoint (%s) > upperEndpoint (%s)", lowerEndpoint, upperEndpoint);
            if (cmp == 0) {
                int i2 = lowerBoundType != BoundType.OPEN ? 1 : 0;
                if (upperBoundType != BoundType.OPEN) {
                    i = 1;
                }
                Preconditions.checkArgument(i | i2);
            }
        }
    }

    /* Access modifiers changed, original: 0000 */
    public Comparator<? super T> comparator() {
        return this.comparator;
    }

    /* Access modifiers changed, original: 0000 */
    public boolean hasLowerBound() {
        return this.hasLowerBound;
    }

    /* Access modifiers changed, original: 0000 */
    public boolean hasUpperBound() {
        return this.hasUpperBound;
    }

    /* Access modifiers changed, original: 0000 */
    public boolean isEmpty() {
        return (hasUpperBound() && tooLow(getUpperEndpoint())) || (hasLowerBound() && tooHigh(getLowerEndpoint()));
    }

    /* Access modifiers changed, original: 0000 */
    public boolean tooLow(@Nullable T t) {
        int i = 0;
        if (!hasLowerBound()) {
            return false;
        }
        int cmp = this.comparator.compare(t, getLowerEndpoint());
        int i2 = cmp < 0 ? 1 : 0;
        int i3 = cmp == 0 ? 1 : 0;
        if (getLowerBoundType() == BoundType.OPEN) {
            i = 1;
        }
        return (i & i3) | i2;
    }

    /* Access modifiers changed, original: 0000 */
    public boolean tooHigh(@Nullable T t) {
        int i = 0;
        if (!hasUpperBound()) {
            return false;
        }
        int cmp = this.comparator.compare(t, getUpperEndpoint());
        int i2 = cmp > 0 ? 1 : 0;
        int i3 = cmp == 0 ? 1 : 0;
        if (getUpperBoundType() == BoundType.OPEN) {
            i = 1;
        }
        return (i & i3) | i2;
    }

    /* Access modifiers changed, original: 0000 */
    public boolean contains(@Nullable T t) {
        return (tooLow(t) || tooHigh(t)) ? false : true;
    }

    /* Access modifiers changed, original: 0000 */
    public GeneralRange<T> intersect(GeneralRange<T> other) {
        int cmp;
        BoundType lowType;
        BoundType upType;
        GeneralRange<T> generalRange = other;
        Preconditions.checkNotNull(other);
        Preconditions.checkArgument(this.comparator.equals(generalRange.comparator));
        boolean hasLowBound = this.hasLowerBound;
        T lowEnd = getLowerEndpoint();
        BoundType lowType2 = getLowerBoundType();
        if (!hasLowerBound()) {
            hasLowBound = generalRange.hasLowerBound;
            lowEnd = other.getLowerEndpoint();
            lowType2 = other.getLowerBoundType();
        } else if (other.hasLowerBound()) {
            cmp = this.comparator.compare(getLowerEndpoint(), other.getLowerEndpoint());
            if (cmp < 0 || (cmp == 0 && other.getLowerBoundType() == BoundType.OPEN)) {
                lowEnd = other.getLowerEndpoint();
                lowType2 = other.getLowerBoundType();
            }
        }
        boolean hasUpBound = this.hasUpperBound;
        T upEnd = getUpperEndpoint();
        BoundType upType2 = getUpperBoundType();
        if (!hasUpperBound()) {
            hasUpBound = generalRange.hasUpperBound;
            upEnd = other.getUpperEndpoint();
            upType2 = other.getUpperBoundType();
        } else if (other.hasUpperBound()) {
            int cmp2 = this.comparator.compare(getUpperEndpoint(), other.getUpperEndpoint());
            if (cmp2 > 0 || (cmp2 == 0 && other.getUpperBoundType() == BoundType.OPEN)) {
                upEnd = other.getUpperEndpoint();
                upType2 = other.getUpperBoundType();
            }
        }
        boolean hasUpBound2 = hasUpBound;
        T upEnd2 = upEnd;
        if (hasLowBound && hasUpBound2) {
            cmp = this.comparator.compare(lowEnd, upEnd2);
            if (cmp > 0 || (cmp == 0 && lowType2 == BoundType.OPEN && upType2 == BoundType.OPEN)) {
                lowEnd = upEnd2;
                lowType = BoundType.OPEN;
                upType = BoundType.CLOSED;
                return new GeneralRange(this.comparator, hasLowBound, lowEnd, lowType, hasUpBound2, upEnd2, upType);
            }
        }
        lowType = lowType2;
        upType = upType2;
        return new GeneralRange(this.comparator, hasLowBound, lowEnd, lowType, hasUpBound2, upEnd2, upType);
    }

    public boolean equals(@Nullable Object obj) {
        boolean z = false;
        if (!(obj instanceof GeneralRange)) {
            return false;
        }
        GeneralRange<?> r = (GeneralRange) obj;
        if (this.comparator.equals(r.comparator) && this.hasLowerBound == r.hasLowerBound && this.hasUpperBound == r.hasUpperBound && getLowerBoundType().equals(r.getLowerBoundType()) && getUpperBoundType().equals(r.getUpperBoundType()) && Objects.equal(getLowerEndpoint(), r.getLowerEndpoint()) && Objects.equal(getUpperEndpoint(), r.getUpperEndpoint())) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        return Objects.hashCode(this.comparator, getLowerEndpoint(), getLowerBoundType(), getUpperEndpoint(), getUpperBoundType());
    }

    /* Access modifiers changed, original: 0000 */
    public GeneralRange<T> reverse() {
        GeneralRange<T> result = this.reverse;
        if (result != null) {
            return result;
        }
        result = new GeneralRange(Ordering.from(this.comparator).reverse(), this.hasUpperBound, getUpperEndpoint(), getUpperBoundType(), this.hasLowerBound, getLowerEndpoint(), getLowerBoundType());
        result.reverse = this;
        this.reverse = result;
        return result;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.comparator);
        stringBuilder.append(":");
        stringBuilder.append(this.lowerBoundType == BoundType.CLOSED ? '[' : '(');
        stringBuilder.append(this.hasLowerBound ? this.lowerEndpoint : "-∞");
        stringBuilder.append(',');
        stringBuilder.append(this.hasUpperBound ? this.upperEndpoint : "∞");
        stringBuilder.append(this.upperBoundType == BoundType.CLOSED ? ']' : ')');
        return stringBuilder.toString();
    }

    /* Access modifiers changed, original: 0000 */
    public T getLowerEndpoint() {
        return this.lowerEndpoint;
    }

    /* Access modifiers changed, original: 0000 */
    public BoundType getLowerBoundType() {
        return this.lowerBoundType;
    }

    /* Access modifiers changed, original: 0000 */
    public T getUpperEndpoint() {
        return this.upperEndpoint;
    }

    /* Access modifiers changed, original: 0000 */
    public BoundType getUpperBoundType() {
        return this.upperBoundType;
    }
}
