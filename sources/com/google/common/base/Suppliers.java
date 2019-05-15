package com.google.common.base;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.VisibleForTesting;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

@GwtCompatible
public final class Suppliers {

    @VisibleForTesting
    static class ExpiringMemoizingSupplier<T> implements Supplier<T>, Serializable {
        private static final long serialVersionUID = 0;
        final Supplier<T> delegate;
        final long durationNanos;
        volatile transient long expirationNanos;
        volatile transient T value;

        ExpiringMemoizingSupplier(Supplier<T> delegate, long duration, TimeUnit unit) {
            this.delegate = (Supplier) Preconditions.checkNotNull(delegate);
            this.durationNanos = unit.toNanos(duration);
            Preconditions.checkArgument(duration > 0);
        }

        public T get() {
            long nanos = this.expirationNanos;
            long now = Platform.systemNanoTime();
            if (nanos == 0 || now - nanos >= 0) {
                synchronized (this) {
                    if (nanos == this.expirationNanos) {
                        T t = this.delegate.get();
                        this.value = t;
                        nanos = now + this.durationNanos;
                        this.expirationNanos = nanos == 0 ? 1 : nanos;
                        return t;
                    }
                }
            }
            return this.value;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Suppliers.memoizeWithExpiration(");
            stringBuilder.append(this.delegate);
            stringBuilder.append(", ");
            stringBuilder.append(this.durationNanos);
            stringBuilder.append(", NANOS)");
            return stringBuilder.toString();
        }
    }

    @VisibleForTesting
    static class MemoizingSupplier<T> implements Supplier<T>, Serializable {
        private static final long serialVersionUID = 0;
        final Supplier<T> delegate;
        volatile transient boolean initialized;
        transient T value;

        MemoizingSupplier(Supplier<T> delegate) {
            this.delegate = delegate;
        }

        public T get() {
            if (!this.initialized) {
                synchronized (this) {
                    if (!this.initialized) {
                        T t = this.delegate.get();
                        this.value = t;
                        this.initialized = true;
                        return t;
                    }
                }
            }
            return this.value;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Suppliers.memoize(");
            stringBuilder.append(this.delegate);
            stringBuilder.append(")");
            return stringBuilder.toString();
        }
    }

    private static class SupplierComposition<F, T> implements Supplier<T>, Serializable {
        private static final long serialVersionUID = 0;
        final Function<? super F, T> function;
        final Supplier<F> supplier;

        SupplierComposition(Function<? super F, T> function, Supplier<F> supplier) {
            this.function = function;
            this.supplier = supplier;
        }

        public T get() {
            return this.function.apply(this.supplier.get());
        }

        public boolean equals(@Nullable Object obj) {
            boolean z = false;
            if (!(obj instanceof SupplierComposition)) {
                return false;
            }
            SupplierComposition<?, ?> that = (SupplierComposition) obj;
            if (this.function.equals(that.function) && this.supplier.equals(that.supplier)) {
                z = true;
            }
            return z;
        }

        public int hashCode() {
            return Objects.hashCode(this.function, this.supplier);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Suppliers.compose(");
            stringBuilder.append(this.function);
            stringBuilder.append(", ");
            stringBuilder.append(this.supplier);
            stringBuilder.append(")");
            return stringBuilder.toString();
        }
    }

    private interface SupplierFunction<T> extends Function<Supplier<T>, T> {
    }

    private static class SupplierOfInstance<T> implements Supplier<T>, Serializable {
        private static final long serialVersionUID = 0;
        final T instance;

        SupplierOfInstance(@Nullable T instance) {
            this.instance = instance;
        }

        public T get() {
            return this.instance;
        }

        public boolean equals(@Nullable Object obj) {
            if (!(obj instanceof SupplierOfInstance)) {
                return false;
            }
            return Objects.equal(this.instance, ((SupplierOfInstance) obj).instance);
        }

        public int hashCode() {
            return Objects.hashCode(this.instance);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Suppliers.ofInstance(");
            stringBuilder.append(this.instance);
            stringBuilder.append(")");
            return stringBuilder.toString();
        }
    }

    private static class ThreadSafeSupplier<T> implements Supplier<T>, Serializable {
        private static final long serialVersionUID = 0;
        final Supplier<T> delegate;

        ThreadSafeSupplier(Supplier<T> delegate) {
            this.delegate = delegate;
        }

        public T get() {
            Object obj;
            synchronized (this.delegate) {
                obj = this.delegate.get();
            }
            return obj;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Suppliers.synchronizedSupplier(");
            stringBuilder.append(this.delegate);
            stringBuilder.append(")");
            return stringBuilder.toString();
        }
    }

    private enum SupplierFunctionImpl implements SupplierFunction<Object> {
        INSTANCE;

        public Object apply(Supplier<Object> input) {
            return input.get();
        }

        public String toString() {
            return "Suppliers.supplierFunction()";
        }
    }

    private Suppliers() {
    }

    public static <F, T> Supplier<T> compose(Function<? super F, T> function, Supplier<F> supplier) {
        Preconditions.checkNotNull(function);
        Preconditions.checkNotNull(supplier);
        return new SupplierComposition(function, supplier);
    }

    public static <T> Supplier<T> memoize(Supplier<T> delegate) {
        if (delegate instanceof MemoizingSupplier) {
            return delegate;
        }
        return new MemoizingSupplier((Supplier) Preconditions.checkNotNull(delegate));
    }

    public static <T> Supplier<T> memoizeWithExpiration(Supplier<T> delegate, long duration, TimeUnit unit) {
        return new ExpiringMemoizingSupplier(delegate, duration, unit);
    }

    public static <T> Supplier<T> ofInstance(@Nullable T instance) {
        return new SupplierOfInstance(instance);
    }

    public static <T> Supplier<T> synchronizedSupplier(Supplier<T> delegate) {
        return new ThreadSafeSupplier((Supplier) Preconditions.checkNotNull(delegate));
    }

    @Beta
    public static <T> Function<Supplier<T>, T> supplierFunction() {
        return SupplierFunctionImpl.INSTANCE;
    }
}
