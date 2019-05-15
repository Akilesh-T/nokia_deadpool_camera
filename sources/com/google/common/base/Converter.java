package com.google.common.base;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import java.io.Serializable;
import java.util.Iterator;
import javax.annotation.Nullable;

@GwtCompatible
@Beta
public abstract class Converter<A, B> implements Function<A, B> {
    private final boolean handleNullAutomatically;
    private transient Converter<B, A> reverse;

    private static final class ConverterComposition<A, B, C> extends Converter<A, C> implements Serializable {
        private static final long serialVersionUID = 0;
        final Converter<A, B> first;
        final Converter<B, C> second;

        ConverterComposition(Converter<A, B> first, Converter<B, C> second) {
            this.first = first;
            this.second = second;
        }

        /* Access modifiers changed, original: protected */
        public C doForward(A a) {
            throw new AssertionError();
        }

        /* Access modifiers changed, original: protected */
        public A doBackward(C c) {
            throw new AssertionError();
        }

        /* Access modifiers changed, original: 0000 */
        @Nullable
        public C correctedDoForward(@Nullable A a) {
            return this.second.correctedDoForward(this.first.correctedDoForward(a));
        }

        /* Access modifiers changed, original: 0000 */
        @Nullable
        public A correctedDoBackward(@Nullable C c) {
            return this.first.correctedDoBackward(this.second.correctedDoBackward(c));
        }

        public boolean equals(@Nullable Object object) {
            boolean z = false;
            if (!(object instanceof ConverterComposition)) {
                return false;
            }
            ConverterComposition<?, ?, ?> that = (ConverterComposition) object;
            if (this.first.equals(that.first) && this.second.equals(that.second)) {
                z = true;
            }
            return z;
        }

        public int hashCode() {
            return (31 * this.first.hashCode()) + this.second.hashCode();
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.first);
            stringBuilder.append(".andThen(");
            stringBuilder.append(this.second);
            stringBuilder.append(")");
            return stringBuilder.toString();
        }
    }

    private static final class FunctionBasedConverter<A, B> extends Converter<A, B> implements Serializable {
        private final Function<? super B, ? extends A> backwardFunction;
        private final Function<? super A, ? extends B> forwardFunction;

        /* synthetic */ FunctionBasedConverter(Function x0, Function x1, AnonymousClass1 x2) {
            this(x0, x1);
        }

        private FunctionBasedConverter(Function<? super A, ? extends B> forwardFunction, Function<? super B, ? extends A> backwardFunction) {
            this.forwardFunction = (Function) Preconditions.checkNotNull(forwardFunction);
            this.backwardFunction = (Function) Preconditions.checkNotNull(backwardFunction);
        }

        /* Access modifiers changed, original: protected */
        public B doForward(A a) {
            return this.forwardFunction.apply(a);
        }

        /* Access modifiers changed, original: protected */
        public A doBackward(B b) {
            return this.backwardFunction.apply(b);
        }

        public boolean equals(@Nullable Object object) {
            boolean z = false;
            if (!(object instanceof FunctionBasedConverter)) {
                return false;
            }
            FunctionBasedConverter<?, ?> that = (FunctionBasedConverter) object;
            if (this.forwardFunction.equals(that.forwardFunction) && this.backwardFunction.equals(that.backwardFunction)) {
                z = true;
            }
            return z;
        }

        public int hashCode() {
            return (this.forwardFunction.hashCode() * 31) + this.backwardFunction.hashCode();
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Converter.from(");
            stringBuilder.append(this.forwardFunction);
            stringBuilder.append(", ");
            stringBuilder.append(this.backwardFunction);
            stringBuilder.append(")");
            return stringBuilder.toString();
        }
    }

    private static final class IdentityConverter<T> extends Converter<T, T> implements Serializable {
        static final IdentityConverter INSTANCE = new IdentityConverter();
        private static final long serialVersionUID = 0;

        private IdentityConverter() {
        }

        /* Access modifiers changed, original: protected */
        public T doForward(T t) {
            return t;
        }

        /* Access modifiers changed, original: protected */
        public T doBackward(T t) {
            return t;
        }

        public IdentityConverter<T> reverse() {
            return this;
        }

        /* Access modifiers changed, original: 0000 */
        public <S> Converter<T, S> doAndThen(Converter<T, S> otherConverter) {
            return (Converter) Preconditions.checkNotNull(otherConverter, "otherConverter");
        }

        public String toString() {
            return "Converter.identity()";
        }

        private Object readResolve() {
            return INSTANCE;
        }
    }

    private static final class ReverseConverter<A, B> extends Converter<B, A> implements Serializable {
        private static final long serialVersionUID = 0;
        final Converter<A, B> original;

        ReverseConverter(Converter<A, B> original) {
            this.original = original;
        }

        /* Access modifiers changed, original: protected */
        public A doForward(B b) {
            throw new AssertionError();
        }

        /* Access modifiers changed, original: protected */
        public B doBackward(A a) {
            throw new AssertionError();
        }

        /* Access modifiers changed, original: 0000 */
        @Nullable
        public A correctedDoForward(@Nullable B b) {
            return this.original.correctedDoBackward(b);
        }

        /* Access modifiers changed, original: 0000 */
        @Nullable
        public B correctedDoBackward(@Nullable A a) {
            return this.original.correctedDoForward(a);
        }

        public Converter<A, B> reverse() {
            return this.original;
        }

        public boolean equals(@Nullable Object object) {
            if (!(object instanceof ReverseConverter)) {
                return false;
            }
            return this.original.equals(((ReverseConverter) object).original);
        }

        public int hashCode() {
            return ~this.original.hashCode();
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.original);
            stringBuilder.append(".reverse()");
            return stringBuilder.toString();
        }
    }

    public abstract A doBackward(B b);

    public abstract B doForward(A a);

    protected Converter() {
        this(true);
    }

    Converter(boolean handleNullAutomatically) {
        this.handleNullAutomatically = handleNullAutomatically;
    }

    @Nullable
    public final B convert(@Nullable A a) {
        return correctedDoForward(a);
    }

    /* Access modifiers changed, original: 0000 */
    @Nullable
    public B correctedDoForward(@Nullable A a) {
        if (!this.handleNullAutomatically) {
            return doForward(a);
        }
        return a == null ? null : Preconditions.checkNotNull(doForward(a));
    }

    /* Access modifiers changed, original: 0000 */
    @Nullable
    public A correctedDoBackward(@Nullable B b) {
        if (!this.handleNullAutomatically) {
            return doBackward(b);
        }
        return b == null ? null : Preconditions.checkNotNull(doBackward(b));
    }

    public Iterable<B> convertAll(final Iterable<? extends A> fromIterable) {
        Preconditions.checkNotNull(fromIterable, "fromIterable");
        return new Iterable<B>() {
            public Iterator<B> iterator() {
                return new Iterator<B>() {
                    private final Iterator<? extends A> fromIterator = fromIterable.iterator();

                    public boolean hasNext() {
                        return this.fromIterator.hasNext();
                    }

                    public B next() {
                        return Converter.this.convert(this.fromIterator.next());
                    }

                    public void remove() {
                        this.fromIterator.remove();
                    }
                };
            }
        };
    }

    public Converter<B, A> reverse() {
        Converter<B, A> result = this.reverse;
        if (result != null) {
            return result;
        }
        Converter<B, A> reverseConverter = new ReverseConverter(this);
        this.reverse = reverseConverter;
        return reverseConverter;
    }

    public final <C> Converter<A, C> andThen(Converter<B, C> secondConverter) {
        return doAndThen(secondConverter);
    }

    /* Access modifiers changed, original: 0000 */
    public <C> Converter<A, C> doAndThen(Converter<B, C> secondConverter) {
        return new ConverterComposition(this, (Converter) Preconditions.checkNotNull(secondConverter));
    }

    @Deprecated
    @Nullable
    public final B apply(@Nullable A a) {
        return convert(a);
    }

    public boolean equals(@Nullable Object object) {
        return super.equals(object);
    }

    public static <A, B> Converter<A, B> from(Function<? super A, ? extends B> forwardFunction, Function<? super B, ? extends A> backwardFunction) {
        return new FunctionBasedConverter(forwardFunction, backwardFunction, null);
    }

    public static <T> Converter<T, T> identity() {
        return IdentityConverter.INSTANCE;
    }
}
