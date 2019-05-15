package com.google.common.collect;

import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Booleans;
import java.io.Serializable;
import java.util.NoSuchElementException;
import javax.annotation.Nullable;

@GwtCompatible
abstract class Cut<C extends Comparable> implements Comparable<Cut<C>>, Serializable {
    private static final long serialVersionUID = 0;
    final C endpoint;

    private static final class AboveAll extends Cut<Comparable<?>> {
        private static final AboveAll INSTANCE = new AboveAll();
        private static final long serialVersionUID = 0;

        private AboveAll() {
            super(null);
        }

        /* Access modifiers changed, original: 0000 */
        public Comparable<?> endpoint() {
            throw new IllegalStateException("range unbounded on this side");
        }

        /* Access modifiers changed, original: 0000 */
        public boolean isLessThan(Comparable<?> comparable) {
            return false;
        }

        /* Access modifiers changed, original: 0000 */
        public BoundType typeAsLowerBound() {
            throw new AssertionError("this statement should be unreachable");
        }

        /* Access modifiers changed, original: 0000 */
        public BoundType typeAsUpperBound() {
            throw new IllegalStateException();
        }

        /* Access modifiers changed, original: 0000 */
        public Cut<Comparable<?>> withLowerBoundType(BoundType boundType, DiscreteDomain<Comparable<?>> discreteDomain) {
            throw new AssertionError("this statement should be unreachable");
        }

        /* Access modifiers changed, original: 0000 */
        public Cut<Comparable<?>> withUpperBoundType(BoundType boundType, DiscreteDomain<Comparable<?>> discreteDomain) {
            throw new IllegalStateException();
        }

        /* Access modifiers changed, original: 0000 */
        public void describeAsLowerBound(StringBuilder sb) {
            throw new AssertionError();
        }

        /* Access modifiers changed, original: 0000 */
        public void describeAsUpperBound(StringBuilder sb) {
            sb.append("+∞)");
        }

        /* Access modifiers changed, original: 0000 */
        public Comparable<?> leastValueAbove(DiscreteDomain<Comparable<?>> discreteDomain) {
            throw new AssertionError();
        }

        /* Access modifiers changed, original: 0000 */
        public Comparable<?> greatestValueBelow(DiscreteDomain<Comparable<?>> domain) {
            return domain.maxValue();
        }

        public int compareTo(Cut<Comparable<?>> o) {
            return o == this ? 0 : 1;
        }

        public String toString() {
            return "+∞";
        }

        private Object readResolve() {
            return INSTANCE;
        }
    }

    private static final class AboveValue<C extends Comparable> extends Cut<C> {
        private static final long serialVersionUID = 0;

        AboveValue(C endpoint) {
            super((Comparable) Preconditions.checkNotNull(endpoint));
        }

        /* Access modifiers changed, original: 0000 */
        public boolean isLessThan(C value) {
            return Range.compareOrThrow(this.endpoint, value) < 0;
        }

        /* Access modifiers changed, original: 0000 */
        public BoundType typeAsLowerBound() {
            return BoundType.OPEN;
        }

        /* Access modifiers changed, original: 0000 */
        public BoundType typeAsUpperBound() {
            return BoundType.CLOSED;
        }

        /* Access modifiers changed, original: 0000 */
        public Cut<C> withLowerBoundType(BoundType boundType, DiscreteDomain<C> domain) {
            switch (boundType) {
                case CLOSED:
                    C next = domain.next(this.endpoint);
                    return next == null ? Cut.belowAll() : Cut.belowValue(next);
                case OPEN:
                    return this;
                default:
                    throw new AssertionError();
            }
        }

        /* Access modifiers changed, original: 0000 */
        public Cut<C> withUpperBoundType(BoundType boundType, DiscreteDomain<C> domain) {
            switch (boundType) {
                case CLOSED:
                    return this;
                case OPEN:
                    C next = domain.next(this.endpoint);
                    return next == null ? Cut.aboveAll() : Cut.belowValue(next);
                default:
                    throw new AssertionError();
            }
        }

        /* Access modifiers changed, original: 0000 */
        public void describeAsLowerBound(StringBuilder sb) {
            sb.append('(');
            sb.append(this.endpoint);
        }

        /* Access modifiers changed, original: 0000 */
        public void describeAsUpperBound(StringBuilder sb) {
            sb.append(this.endpoint);
            sb.append(']');
        }

        /* Access modifiers changed, original: 0000 */
        public C leastValueAbove(DiscreteDomain<C> domain) {
            return domain.next(this.endpoint);
        }

        /* Access modifiers changed, original: 0000 */
        public C greatestValueBelow(DiscreteDomain<C> discreteDomain) {
            return this.endpoint;
        }

        /* Access modifiers changed, original: 0000 */
        public Cut<C> canonical(DiscreteDomain<C> domain) {
            C next = leastValueAbove(domain);
            return next != null ? Cut.belowValue(next) : Cut.aboveAll();
        }

        public int hashCode() {
            return ~this.endpoint.hashCode();
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("/");
            stringBuilder.append(this.endpoint);
            stringBuilder.append("\\");
            return stringBuilder.toString();
        }
    }

    private static final class BelowAll extends Cut<Comparable<?>> {
        private static final BelowAll INSTANCE = new BelowAll();
        private static final long serialVersionUID = 0;

        private BelowAll() {
            super(null);
        }

        /* Access modifiers changed, original: 0000 */
        public Comparable<?> endpoint() {
            throw new IllegalStateException("range unbounded on this side");
        }

        /* Access modifiers changed, original: 0000 */
        public boolean isLessThan(Comparable<?> comparable) {
            return true;
        }

        /* Access modifiers changed, original: 0000 */
        public BoundType typeAsLowerBound() {
            throw new IllegalStateException();
        }

        /* Access modifiers changed, original: 0000 */
        public BoundType typeAsUpperBound() {
            throw new AssertionError("this statement should be unreachable");
        }

        /* Access modifiers changed, original: 0000 */
        public Cut<Comparable<?>> withLowerBoundType(BoundType boundType, DiscreteDomain<Comparable<?>> discreteDomain) {
            throw new IllegalStateException();
        }

        /* Access modifiers changed, original: 0000 */
        public Cut<Comparable<?>> withUpperBoundType(BoundType boundType, DiscreteDomain<Comparable<?>> discreteDomain) {
            throw new AssertionError("this statement should be unreachable");
        }

        /* Access modifiers changed, original: 0000 */
        public void describeAsLowerBound(StringBuilder sb) {
            sb.append("(-∞");
        }

        /* Access modifiers changed, original: 0000 */
        public void describeAsUpperBound(StringBuilder sb) {
            throw new AssertionError();
        }

        /* Access modifiers changed, original: 0000 */
        public Comparable<?> leastValueAbove(DiscreteDomain<Comparable<?>> domain) {
            return domain.minValue();
        }

        /* Access modifiers changed, original: 0000 */
        public Comparable<?> greatestValueBelow(DiscreteDomain<Comparable<?>> discreteDomain) {
            throw new AssertionError();
        }

        /* Access modifiers changed, original: 0000 */
        public Cut<Comparable<?>> canonical(DiscreteDomain<Comparable<?>> domain) {
            try {
                return Cut.belowValue(domain.minValue());
            } catch (NoSuchElementException e) {
                return this;
            }
        }

        public int compareTo(Cut<Comparable<?>> o) {
            return o == this ? 0 : -1;
        }

        public String toString() {
            return "-∞";
        }

        private Object readResolve() {
            return INSTANCE;
        }
    }

    private static final class BelowValue<C extends Comparable> extends Cut<C> {
        private static final long serialVersionUID = 0;

        BelowValue(C endpoint) {
            super((Comparable) Preconditions.checkNotNull(endpoint));
        }

        /* Access modifiers changed, original: 0000 */
        public boolean isLessThan(C value) {
            return Range.compareOrThrow(this.endpoint, value) <= 0;
        }

        /* Access modifiers changed, original: 0000 */
        public BoundType typeAsLowerBound() {
            return BoundType.CLOSED;
        }

        /* Access modifiers changed, original: 0000 */
        public BoundType typeAsUpperBound() {
            return BoundType.OPEN;
        }

        /* Access modifiers changed, original: 0000 */
        public Cut<C> withLowerBoundType(BoundType boundType, DiscreteDomain<C> domain) {
            switch (boundType) {
                case CLOSED:
                    return this;
                case OPEN:
                    C previous = domain.previous(this.endpoint);
                    return previous == null ? Cut.belowAll() : new AboveValue(previous);
                default:
                    throw new AssertionError();
            }
        }

        /* Access modifiers changed, original: 0000 */
        public Cut<C> withUpperBoundType(BoundType boundType, DiscreteDomain<C> domain) {
            switch (boundType) {
                case CLOSED:
                    C previous = domain.previous(this.endpoint);
                    return previous == null ? Cut.aboveAll() : new AboveValue(previous);
                case OPEN:
                    return this;
                default:
                    throw new AssertionError();
            }
        }

        /* Access modifiers changed, original: 0000 */
        public void describeAsLowerBound(StringBuilder sb) {
            sb.append('[');
            sb.append(this.endpoint);
        }

        /* Access modifiers changed, original: 0000 */
        public void describeAsUpperBound(StringBuilder sb) {
            sb.append(this.endpoint);
            sb.append(')');
        }

        /* Access modifiers changed, original: 0000 */
        public C leastValueAbove(DiscreteDomain<C> discreteDomain) {
            return this.endpoint;
        }

        /* Access modifiers changed, original: 0000 */
        public C greatestValueBelow(DiscreteDomain<C> domain) {
            return domain.previous(this.endpoint);
        }

        public int hashCode() {
            return this.endpoint.hashCode();
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("\\");
            stringBuilder.append(this.endpoint);
            stringBuilder.append("/");
            return stringBuilder.toString();
        }
    }

    public abstract void describeAsLowerBound(StringBuilder stringBuilder);

    public abstract void describeAsUpperBound(StringBuilder stringBuilder);

    public abstract C greatestValueBelow(DiscreteDomain<C> discreteDomain);

    public abstract boolean isLessThan(C c);

    public abstract C leastValueAbove(DiscreteDomain<C> discreteDomain);

    public abstract BoundType typeAsLowerBound();

    public abstract BoundType typeAsUpperBound();

    public abstract Cut<C> withLowerBoundType(BoundType boundType, DiscreteDomain<C> discreteDomain);

    public abstract Cut<C> withUpperBoundType(BoundType boundType, DiscreteDomain<C> discreteDomain);

    Cut(@Nullable C endpoint) {
        this.endpoint = endpoint;
    }

    /* Access modifiers changed, original: 0000 */
    public Cut<C> canonical(DiscreteDomain<C> discreteDomain) {
        return this;
    }

    public int compareTo(Cut<C> that) {
        if (that == belowAll()) {
            return 1;
        }
        if (that == aboveAll()) {
            return -1;
        }
        int result = Range.compareOrThrow(this.endpoint, that.endpoint);
        if (result != 0) {
            return result;
        }
        return Booleans.compare(this instanceof AboveValue, that instanceof AboveValue);
    }

    /* Access modifiers changed, original: 0000 */
    public C endpoint() {
        return this.endpoint;
    }

    public boolean equals(Object obj) {
        boolean z = false;
        if (obj instanceof Cut) {
            try {
                if (compareTo((Cut) obj) == 0) {
                    z = true;
                }
                return z;
            } catch (ClassCastException e) {
            }
        }
        return false;
    }

    static <C extends Comparable> Cut<C> belowAll() {
        return BelowAll.INSTANCE;
    }

    static <C extends Comparable> Cut<C> aboveAll() {
        return AboveAll.INSTANCE;
    }

    static <C extends Comparable> Cut<C> belowValue(C endpoint) {
        return new BelowValue(endpoint);
    }

    static <C extends Comparable> Cut<C> aboveValue(C endpoint) {
        return new AboveValue(endpoint);
    }
}
