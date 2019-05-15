package com.google.common.util.concurrent;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

@Beta
public final class Futures {
    private static final AsyncFunction<ListenableFuture<Object>, Object> DEREFERENCER = new AsyncFunction<ListenableFuture<Object>, Object>() {
        public ListenableFuture<Object> apply(ListenableFuture<Object> input) {
            return input;
        }
    };
    private static final Ordering<Constructor<?>> WITH_STRING_PARAM_FIRST = Ordering.natural().onResultOf(new Function<Constructor<?>, Boolean>() {
        public Boolean apply(Constructor<?> input) {
            return Boolean.valueOf(Arrays.asList(input.getParameterTypes()).contains(String.class));
        }
    }).reverse();

    private interface FutureCombiner<V, C> {
        C combine(List<Optional<V>> list);
    }

    private static final class WrappedCombiner<T> implements Callable<T> {
        final Callable<T> delegate;
        CombinerFuture<T> outputFuture;

        WrappedCombiner(Callable<T> delegate) {
            this.delegate = (Callable) Preconditions.checkNotNull(delegate);
        }

        public T call() throws Exception {
            try {
                return this.delegate.call();
            } catch (ExecutionException e) {
                this.outputFuture.setException(e.getCause());
                return null;
            } catch (CancellationException e2) {
                this.outputFuture.cancel(false);
                return null;
            }
        }
    }

    private static abstract class ImmediateFuture<V> implements ListenableFuture<V> {
        private static final Logger log = Logger.getLogger(ImmediateFuture.class.getName());

        public abstract V get() throws ExecutionException;

        private ImmediateFuture() {
        }

        /* synthetic */ ImmediateFuture(AnonymousClass1 x0) {
            this();
        }

        public void addListener(Runnable listener, Executor executor) {
            Preconditions.checkNotNull(listener, "Runnable was null.");
            Preconditions.checkNotNull(executor, "Executor was null.");
            try {
                executor.execute(listener);
            } catch (RuntimeException e) {
                Logger logger = log;
                Level level = Level.SEVERE;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("RuntimeException while executing runnable ");
                stringBuilder.append(listener);
                stringBuilder.append(" with executor ");
                stringBuilder.append(executor);
                logger.log(level, stringBuilder.toString(), e);
            }
        }

        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        public V get(long timeout, TimeUnit unit) throws ExecutionException {
            Preconditions.checkNotNull(unit);
            return get();
        }

        public boolean isCancelled() {
            return false;
        }

        public boolean isDone() {
            return true;
        }
    }

    private static class ChainingListenableFuture<I, O> extends AbstractFuture<O> implements Runnable {
        private AsyncFunction<? super I, ? extends O> function;
        private ListenableFuture<? extends I> inputFuture;
        private volatile ListenableFuture<? extends O> outputFuture;

        /* synthetic */ ChainingListenableFuture(AsyncFunction x0, ListenableFuture x1, AnonymousClass1 x2) {
            this(x0, x1);
        }

        private ChainingListenableFuture(AsyncFunction<? super I, ? extends O> function, ListenableFuture<? extends I> inputFuture) {
            this.function = (AsyncFunction) Preconditions.checkNotNull(function);
            this.inputFuture = (ListenableFuture) Preconditions.checkNotNull(inputFuture);
        }

        public boolean cancel(boolean mayInterruptIfRunning) {
            if (!super.cancel(mayInterruptIfRunning)) {
                return false;
            }
            cancel(this.inputFuture, mayInterruptIfRunning);
            cancel(this.outputFuture, mayInterruptIfRunning);
            return true;
        }

        private void cancel(@Nullable Future<?> future, boolean mayInterruptIfRunning) {
            if (future != null) {
                future.cancel(mayInterruptIfRunning);
            }
        }

        public void run() {
            try {
                try {
                    final ListenableFuture<? extends O> outputFuture = (ListenableFuture) Preconditions.checkNotNull(this.function.apply(Uninterruptibles.getUninterruptibly(this.inputFuture)), "AsyncFunction may not return null.");
                    this.outputFuture = outputFuture;
                    if (isCancelled()) {
                        outputFuture.cancel(wasInterrupted());
                        this.outputFuture = null;
                        this.function = null;
                        this.inputFuture = null;
                        return;
                    }
                    outputFuture.addListener(new Runnable() {
                        public void run() {
                            try {
                                ChainingListenableFuture.this.set(Uninterruptibles.getUninterruptibly(outputFuture));
                            } catch (CancellationException e) {
                                ChainingListenableFuture.this.cancel(false);
                                ChainingListenableFuture.this.outputFuture = null;
                                return;
                            } catch (ExecutionException e2) {
                                ChainingListenableFuture.this.setException(e2.getCause());
                            } catch (Throwable th) {
                                ChainingListenableFuture.this.outputFuture = null;
                            }
                            ChainingListenableFuture.this.outputFuture = null;
                        }
                    }, MoreExecutors.directExecutor());
                    this.function = null;
                    this.inputFuture = null;
                } catch (UndeclaredThrowableException e) {
                    setException(e.getCause());
                } catch (Throwable th) {
                    this.function = null;
                    this.inputFuture = null;
                }
            } catch (CancellationException e2) {
                cancel(false);
                this.function = null;
                this.inputFuture = null;
            } catch (ExecutionException e3) {
                setException(e3.getCause());
                this.function = null;
                this.inputFuture = null;
            }
        }
    }

    private static class CombinedFuture<V, C> extends AbstractFuture<C> {
        private static final Logger logger = Logger.getLogger(CombinedFuture.class.getName());
        final boolean allMustSucceed;
        FutureCombiner<V, C> combiner;
        ImmutableCollection<? extends ListenableFuture<? extends V>> futures;
        final AtomicInteger remaining;
        Set<Throwable> seenExceptions;
        final Object seenExceptionsLock = new Object();
        List<Optional<V>> values;

        CombinedFuture(ImmutableCollection<? extends ListenableFuture<? extends V>> futures, boolean allMustSucceed, Executor listenerExecutor, FutureCombiner<V, C> combiner) {
            this.futures = futures;
            this.allMustSucceed = allMustSucceed;
            this.remaining = new AtomicInteger(futures.size());
            this.combiner = combiner;
            this.values = Lists.newArrayListWithCapacity(futures.size());
            init(listenerExecutor);
        }

        /* Access modifiers changed, original: protected */
        public void init(Executor listenerExecutor) {
            addListener(new Runnable() {
                public void run() {
                    if (CombinedFuture.this.isCancelled()) {
                        UnmodifiableIterator it = CombinedFuture.this.futures.iterator();
                        while (it.hasNext()) {
                            ((ListenableFuture) it.next()).cancel(CombinedFuture.this.wasInterrupted());
                        }
                    }
                    CombinedFuture.this.futures = null;
                    CombinedFuture.this.values = null;
                    CombinedFuture.this.combiner = null;
                }
            }, MoreExecutors.directExecutor());
            if (this.futures.isEmpty()) {
                set(this.combiner.combine(ImmutableList.of()));
                return;
            }
            int i;
            for (i = 0; i < this.futures.size(); i++) {
                this.values.add(null);
            }
            i = 0;
            UnmodifiableIterator it = this.futures.iterator();
            while (it.hasNext()) {
                final ListenableFuture<? extends V> listenable = (ListenableFuture) it.next();
                int i2 = i + 1;
                listenable.addListener(new Runnable() {
                    public void run() {
                        CombinedFuture.this.setOneValue(i, listenable);
                    }
                }, listenerExecutor);
                i = i2;
            }
        }

        private void setExceptionAndMaybeLog(Throwable throwable) {
            boolean visibleFromOutputFuture = false;
            boolean firstTimeSeeingThisException = true;
            if (this.allMustSucceed) {
                boolean visibleFromOutputFuture2 = super.setException(throwable);
                synchronized (this.seenExceptionsLock) {
                    if (this.seenExceptions == null) {
                        this.seenExceptions = Sets.newHashSet();
                    }
                    firstTimeSeeingThisException = this.seenExceptions.add(throwable);
                }
                visibleFromOutputFuture = visibleFromOutputFuture2;
            }
            if ((throwable instanceof Error) || (this.allMustSucceed && !visibleFromOutputFuture && firstTimeSeeingThisException)) {
                logger.log(Level.SEVERE, "input future failed.", throwable);
            }
        }

        /* JADX WARNING: Missing block: B:21:0x004a, code skipped:
            if (r0 != null) goto L_0x006b;
     */
        /* JADX WARNING: Missing block: B:33:0x0069, code skipped:
            if (r0 != null) goto L_0x006b;
     */
        /* JADX WARNING: Missing block: B:34:0x006b, code skipped:
            set(r2.combine(r0));
     */
        /* JADX WARNING: Missing block: B:46:0x0098, code skipped:
            if (r0 != null) goto L_0x006b;
     */
        /* JADX WARNING: Missing block: B:59:0x00b8, code skipped:
            if (r0 != null) goto L_0x006b;
     */
        /* JADX WARNING: Missing block: B:74:?, code skipped:
            return;
     */
        private void setOneValue(int r6, java.util.concurrent.Future<? extends V> r7) {
            /*
            r5 = this;
            r0 = r5.values;
            r1 = r5.isDone();
            r2 = 1;
            r3 = 0;
            if (r1 != 0) goto L_0x000c;
        L_0x000a:
            if (r0 != 0) goto L_0x001f;
        L_0x000c:
            r1 = r5.allMustSucceed;
            if (r1 != 0) goto L_0x0019;
        L_0x0010:
            r1 = r5.isCancelled();
            if (r1 == 0) goto L_0x0017;
        L_0x0016:
            goto L_0x0019;
        L_0x0017:
            r1 = r3;
            goto L_0x001a;
        L_0x0019:
            r1 = r2;
        L_0x001a:
            r4 = "Future was done before all dependencies completed";
            com.google.common.base.Preconditions.checkState(r1, r4);
        L_0x001f:
            r1 = r7.isDone();	 Catch:{ CancellationException -> 0x009b, ExecutionException -> 0x007b, Throwable -> 0x0050 }
            r4 = "Tried to set value from future which is not done";
            com.google.common.base.Preconditions.checkState(r1, r4);	 Catch:{ CancellationException -> 0x009b, ExecutionException -> 0x007b, Throwable -> 0x0050 }
            r1 = com.google.common.util.concurrent.Uninterruptibles.getUninterruptibly(r7);	 Catch:{ CancellationException -> 0x009b, ExecutionException -> 0x007b, Throwable -> 0x0050 }
            if (r0 == 0) goto L_0x0035;
        L_0x002e:
            r4 = com.google.common.base.Optional.fromNullable(r1);	 Catch:{ CancellationException -> 0x009b, ExecutionException -> 0x007b, Throwable -> 0x0050 }
            r0.set(r6, r4);	 Catch:{ CancellationException -> 0x009b, ExecutionException -> 0x007b, Throwable -> 0x0050 }
        L_0x0035:
            r1 = r5.remaining;
            r1 = r1.decrementAndGet();
            if (r1 < 0) goto L_0x003e;
        L_0x003d:
            goto L_0x003f;
        L_0x003e:
            r2 = r3;
        L_0x003f:
            r3 = "Less than 0 remaining futures";
            com.google.common.base.Preconditions.checkState(r2, r3);
            if (r1 != 0) goto L_0x007a;
        L_0x0046:
            r2 = r5.combiner;
            if (r2 == 0) goto L_0x0073;
        L_0x004a:
            if (r0 == 0) goto L_0x0073;
        L_0x004c:
            goto L_0x006b;
        L_0x004d:
            r1 = move-exception;
            goto L_0x00bc;
        L_0x0050:
            r1 = move-exception;
            r5.setExceptionAndMaybeLog(r1);	 Catch:{ all -> 0x004d }
            r1 = r5.remaining;
            r1 = r1.decrementAndGet();
            if (r1 < 0) goto L_0x005d;
        L_0x005c:
            goto L_0x005e;
        L_0x005d:
            r2 = r3;
        L_0x005e:
            r3 = "Less than 0 remaining futures";
            com.google.common.base.Preconditions.checkState(r2, r3);
            if (r1 != 0) goto L_0x007a;
        L_0x0065:
            r2 = r5.combiner;
            if (r2 == 0) goto L_0x0073;
        L_0x0069:
            if (r0 == 0) goto L_0x0073;
        L_0x006b:
            r3 = r2.combine(r0);
            r5.set(r3);
            goto L_0x007a;
        L_0x0073:
            r3 = r5.isDone();
            com.google.common.base.Preconditions.checkState(r3);
        L_0x007a:
            goto L_0x00bb;
        L_0x007b:
            r1 = move-exception;
            r4 = r1.getCause();	 Catch:{ all -> 0x004d }
            r5.setExceptionAndMaybeLog(r4);	 Catch:{ all -> 0x004d }
            r1 = r5.remaining;
            r1 = r1.decrementAndGet();
            if (r1 < 0) goto L_0x008c;
        L_0x008b:
            goto L_0x008d;
        L_0x008c:
            r2 = r3;
        L_0x008d:
            r3 = "Less than 0 remaining futures";
            com.google.common.base.Preconditions.checkState(r2, r3);
            if (r1 != 0) goto L_0x007a;
        L_0x0094:
            r2 = r5.combiner;
            if (r2 == 0) goto L_0x0073;
        L_0x0098:
            if (r0 == 0) goto L_0x0073;
        L_0x009a:
            goto L_0x006b;
        L_0x009b:
            r1 = move-exception;
            r4 = r5.allMustSucceed;	 Catch:{ all -> 0x004d }
            if (r4 == 0) goto L_0x00a3;
        L_0x00a0:
            r5.cancel(r3);	 Catch:{ all -> 0x004d }
        L_0x00a3:
            r1 = r5.remaining;
            r1 = r1.decrementAndGet();
            if (r1 < 0) goto L_0x00ac;
        L_0x00ab:
            goto L_0x00ad;
        L_0x00ac:
            r2 = r3;
        L_0x00ad:
            r3 = "Less than 0 remaining futures";
            com.google.common.base.Preconditions.checkState(r2, r3);
            if (r1 != 0) goto L_0x007a;
        L_0x00b4:
            r2 = r5.combiner;
            if (r2 == 0) goto L_0x0073;
        L_0x00b8:
            if (r0 == 0) goto L_0x0073;
        L_0x00ba:
            goto L_0x006b;
        L_0x00bb:
            return;
        L_0x00bc:
            r4 = r5.remaining;
            r4 = r4.decrementAndGet();
            if (r4 < 0) goto L_0x00c5;
        L_0x00c4:
            goto L_0x00c6;
        L_0x00c5:
            r2 = r3;
        L_0x00c6:
            r3 = "Less than 0 remaining futures";
            com.google.common.base.Preconditions.checkState(r2, r3);
            if (r4 != 0) goto L_0x00e2;
        L_0x00cd:
            r2 = r5.combiner;
            if (r2 == 0) goto L_0x00db;
        L_0x00d1:
            if (r0 == 0) goto L_0x00db;
        L_0x00d3:
            r3 = r2.combine(r0);
            r5.set(r3);
            goto L_0x00e2;
        L_0x00db:
            r3 = r5.isDone();
            com.google.common.base.Preconditions.checkState(r3);
        L_0x00e2:
            throw r1;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.google.common.util.concurrent.Futures$CombinedFuture.setOneValue(int, java.util.concurrent.Future):void");
        }
    }

    private static final class CombinerFuture<V> extends ListenableFutureTask<V> {
        ImmutableList<ListenableFuture<?>> futures;

        CombinerFuture(Callable<V> callable, ImmutableList<ListenableFuture<?>> futures) {
            super(callable);
            this.futures = futures;
        }

        public boolean cancel(boolean mayInterruptIfRunning) {
            ImmutableList<ListenableFuture<?>> futures = this.futures;
            if (!super.cancel(mayInterruptIfRunning)) {
                return false;
            }
            UnmodifiableIterator it = futures.iterator();
            while (it.hasNext()) {
                ((ListenableFuture) it.next()).cancel(mayInterruptIfRunning);
            }
            return true;
        }

        /* Access modifiers changed, original: protected */
        public void done() {
            super.done();
            this.futures = null;
        }

        /* Access modifiers changed, original: protected */
        public void setException(Throwable t) {
            super.setException(t);
        }
    }

    private static class FallbackFuture<V> extends AbstractFuture<V> {
        private volatile ListenableFuture<? extends V> running;

        FallbackFuture(ListenableFuture<? extends V> input, final FutureFallback<? extends V> fallback, Executor executor) {
            this.running = input;
            Futures.addCallback(this.running, new FutureCallback<V>() {
                public void onSuccess(V value) {
                    FallbackFuture.this.set(value);
                }

                public void onFailure(Throwable t) {
                    if (!FallbackFuture.this.isCancelled()) {
                        try {
                            FallbackFuture.this.running = fallback.create(t);
                            if (FallbackFuture.this.isCancelled()) {
                                FallbackFuture.this.running.cancel(FallbackFuture.this.wasInterrupted());
                            } else {
                                Futures.addCallback(FallbackFuture.this.running, new FutureCallback<V>() {
                                    public void onSuccess(V value) {
                                        FallbackFuture.this.set(value);
                                    }

                                    public void onFailure(Throwable t) {
                                        if (FallbackFuture.this.running.isCancelled()) {
                                            FallbackFuture.this.cancel(false);
                                        } else {
                                            FallbackFuture.this.setException(t);
                                        }
                                    }
                                }, MoreExecutors.directExecutor());
                            }
                        } catch (Throwable e) {
                            FallbackFuture.this.setException(e);
                        }
                    }
                }
            }, executor);
        }

        public boolean cancel(boolean mayInterruptIfRunning) {
            if (!super.cancel(mayInterruptIfRunning)) {
                return false;
            }
            this.running.cancel(mayInterruptIfRunning);
            return true;
        }
    }

    private static class ImmediateCancelledFuture<V> extends ImmediateFuture<V> {
        private final CancellationException thrown = new CancellationException("Immediate cancelled future.");

        ImmediateCancelledFuture() {
            super();
        }

        public boolean isCancelled() {
            return true;
        }

        public V get() {
            throw AbstractFuture.cancellationExceptionWithCause("Task was cancelled.", this.thrown);
        }
    }

    private static class ImmediateFailedCheckedFuture<V, X extends Exception> extends ImmediateFuture<V> implements CheckedFuture<V, X> {
        private final X thrown;

        ImmediateFailedCheckedFuture(X thrown) {
            super();
            this.thrown = thrown;
        }

        public V get() throws ExecutionException {
            throw new ExecutionException(this.thrown);
        }

        public V checkedGet() throws Exception {
            throw this.thrown;
        }

        public V checkedGet(long timeout, TimeUnit unit) throws Exception {
            Preconditions.checkNotNull(unit);
            throw this.thrown;
        }
    }

    private static class ImmediateFailedFuture<V> extends ImmediateFuture<V> {
        private final Throwable thrown;

        ImmediateFailedFuture(Throwable thrown) {
            super();
            this.thrown = thrown;
        }

        public V get() throws ExecutionException {
            throw new ExecutionException(this.thrown);
        }
    }

    private static class ImmediateSuccessfulCheckedFuture<V, X extends Exception> extends ImmediateFuture<V> implements CheckedFuture<V, X> {
        @Nullable
        private final V value;

        ImmediateSuccessfulCheckedFuture(@Nullable V value) {
            super();
            this.value = value;
        }

        public V get() {
            return this.value;
        }

        public V checkedGet() {
            return this.value;
        }

        public V checkedGet(long timeout, TimeUnit unit) {
            Preconditions.checkNotNull(unit);
            return this.value;
        }
    }

    private static class ImmediateSuccessfulFuture<V> extends ImmediateFuture<V> {
        @Nullable
        private final V value;

        ImmediateSuccessfulFuture(@Nullable V value) {
            super();
            this.value = value;
        }

        public V get() {
            return this.value;
        }
    }

    private static class NonCancellationPropagatingFuture<V> extends AbstractFuture<V> {
        NonCancellationPropagatingFuture(final ListenableFuture<V> delegate) {
            Preconditions.checkNotNull(delegate);
            Futures.addCallback(delegate, new FutureCallback<V>() {
                public void onSuccess(V result) {
                    NonCancellationPropagatingFuture.this.set(result);
                }

                public void onFailure(Throwable t) {
                    if (delegate.isCancelled()) {
                        NonCancellationPropagatingFuture.this.cancel(false);
                    } else {
                        NonCancellationPropagatingFuture.this.setException(t);
                    }
                }
            }, MoreExecutors.directExecutor());
        }
    }

    private static class MappingCheckedFuture<V, X extends Exception> extends AbstractCheckedFuture<V, X> {
        final Function<? super Exception, X> mapper;

        MappingCheckedFuture(ListenableFuture<V> delegate, Function<? super Exception, X> mapper) {
            super(delegate);
            this.mapper = (Function) Preconditions.checkNotNull(mapper);
        }

        /* Access modifiers changed, original: protected */
        public X mapException(Exception e) {
            return (Exception) this.mapper.apply(e);
        }
    }

    private Futures() {
    }

    public static <V, X extends Exception> CheckedFuture<V, X> makeChecked(ListenableFuture<V> future, Function<? super Exception, X> mapper) {
        return new MappingCheckedFuture((ListenableFuture) Preconditions.checkNotNull(future), mapper);
    }

    public static <V> ListenableFuture<V> immediateFuture(@Nullable V value) {
        return new ImmediateSuccessfulFuture(value);
    }

    public static <V, X extends Exception> CheckedFuture<V, X> immediateCheckedFuture(@Nullable V value) {
        return new ImmediateSuccessfulCheckedFuture(value);
    }

    public static <V> ListenableFuture<V> immediateFailedFuture(Throwable throwable) {
        Preconditions.checkNotNull(throwable);
        return new ImmediateFailedFuture(throwable);
    }

    public static <V> ListenableFuture<V> immediateCancelledFuture() {
        return new ImmediateCancelledFuture();
    }

    public static <V, X extends Exception> CheckedFuture<V, X> immediateFailedCheckedFuture(X exception) {
        Preconditions.checkNotNull(exception);
        return new ImmediateFailedCheckedFuture(exception);
    }

    public static <V> ListenableFuture<V> withFallback(ListenableFuture<? extends V> input, FutureFallback<? extends V> fallback) {
        return withFallback(input, fallback, MoreExecutors.directExecutor());
    }

    public static <V> ListenableFuture<V> withFallback(ListenableFuture<? extends V> input, FutureFallback<? extends V> fallback, Executor executor) {
        Preconditions.checkNotNull(fallback);
        return new FallbackFuture(input, fallback, executor);
    }

    public static <I, O> ListenableFuture<O> transform(ListenableFuture<I> input, AsyncFunction<? super I, ? extends O> function) {
        ChainingListenableFuture<I, O> output = new ChainingListenableFuture(function, input, null);
        input.addListener(output, MoreExecutors.directExecutor());
        return output;
    }

    public static <I, O> ListenableFuture<O> transform(ListenableFuture<I> input, AsyncFunction<? super I, ? extends O> function, Executor executor) {
        Preconditions.checkNotNull(executor);
        ChainingListenableFuture<I, O> output = new ChainingListenableFuture(function, input, null);
        input.addListener(rejectionPropagatingRunnable(output, output, executor), MoreExecutors.directExecutor());
        return output;
    }

    private static Runnable rejectionPropagatingRunnable(final AbstractFuture<?> outputFuture, final Runnable delegateTask, final Executor delegateExecutor) {
        return new Runnable() {
            public void run() {
                final AtomicBoolean thrownFromDelegate = new AtomicBoolean(true);
                try {
                    delegateExecutor.execute(new Runnable() {
                        public void run() {
                            thrownFromDelegate.set(false);
                            delegateTask.run();
                        }
                    });
                } catch (RejectedExecutionException e) {
                    if (thrownFromDelegate.get()) {
                        outputFuture.setException(e);
                    }
                }
            }
        };
    }

    public static <I, O> ListenableFuture<O> transform(ListenableFuture<I> input, Function<? super I, ? extends O> function) {
        Preconditions.checkNotNull(function);
        ChainingListenableFuture<I, O> output = new ChainingListenableFuture(asAsyncFunction(function), input, null);
        input.addListener(output, MoreExecutors.directExecutor());
        return output;
    }

    public static <I, O> ListenableFuture<O> transform(ListenableFuture<I> input, Function<? super I, ? extends O> function, Executor executor) {
        Preconditions.checkNotNull(function);
        return transform((ListenableFuture) input, asAsyncFunction(function), executor);
    }

    private static <I, O> AsyncFunction<I, O> asAsyncFunction(final Function<? super I, ? extends O> function) {
        return new AsyncFunction<I, O>() {
            public ListenableFuture<O> apply(I input) {
                return Futures.immediateFuture(function.apply(input));
            }
        };
    }

    public static <I, O> Future<O> lazyTransform(final Future<I> input, final Function<? super I, ? extends O> function) {
        Preconditions.checkNotNull(input);
        Preconditions.checkNotNull(function);
        return new Future<O>() {
            public boolean cancel(boolean mayInterruptIfRunning) {
                return input.cancel(mayInterruptIfRunning);
            }

            public boolean isCancelled() {
                return input.isCancelled();
            }

            public boolean isDone() {
                return input.isDone();
            }

            public O get() throws InterruptedException, ExecutionException {
                return applyTransformation(input.get());
            }

            public O get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                return applyTransformation(input.get(timeout, unit));
            }

            private O applyTransformation(I input) throws ExecutionException {
                try {
                    return function.apply(input);
                } catch (Throwable t) {
                    ExecutionException executionException = new ExecutionException(t);
                }
            }
        };
    }

    public static <V> ListenableFuture<V> dereference(ListenableFuture<? extends ListenableFuture<? extends V>> nested) {
        return transform((ListenableFuture) nested, DEREFERENCER);
    }

    @Beta
    public static <V> ListenableFuture<List<V>> allAsList(ListenableFuture<? extends V>... futures) {
        return listFuture(ImmutableList.copyOf((Object[]) futures), true, MoreExecutors.directExecutor());
    }

    @Beta
    public static <V> ListenableFuture<List<V>> allAsList(Iterable<? extends ListenableFuture<? extends V>> futures) {
        return listFuture(ImmutableList.copyOf((Iterable) futures), true, MoreExecutors.directExecutor());
    }

    public static <V> ListenableFuture<V> nonCancellationPropagating(ListenableFuture<V> future) {
        return new NonCancellationPropagatingFuture(future);
    }

    @Beta
    public static <V> ListenableFuture<List<V>> successfulAsList(ListenableFuture<? extends V>... futures) {
        return listFuture(ImmutableList.copyOf((Object[]) futures), false, MoreExecutors.directExecutor());
    }

    @Beta
    public static <V> ListenableFuture<List<V>> successfulAsList(Iterable<? extends ListenableFuture<? extends V>> futures) {
        return listFuture(ImmutableList.copyOf((Iterable) futures), false, MoreExecutors.directExecutor());
    }

    @Beta
    public static <T> ImmutableList<ListenableFuture<T>> inCompletionOrder(Iterable<? extends ListenableFuture<? extends T>> futures) {
        final ConcurrentLinkedQueue<AsyncSettableFuture<T>> delegates = Queues.newConcurrentLinkedQueue();
        Builder<ListenableFuture<T>> listBuilder = ImmutableList.builder();
        SerializingExecutor executor = new SerializingExecutor(MoreExecutors.directExecutor());
        for (final ListenableFuture<? extends T> future : futures) {
            Object delegate = AsyncSettableFuture.create();
            delegates.add(delegate);
            future.addListener(new Runnable() {
                public void run() {
                    ((AsyncSettableFuture) delegates.remove()).setFuture(future);
                }
            }, executor);
            listBuilder.add(delegate);
        }
        return listBuilder.build();
    }

    public static <V> void addCallback(ListenableFuture<V> future, FutureCallback<? super V> callback) {
        addCallback(future, callback, MoreExecutors.directExecutor());
    }

    public static <V> void addCallback(final ListenableFuture<V> future, final FutureCallback<? super V> callback, Executor executor) {
        Preconditions.checkNotNull(callback);
        future.addListener(new Runnable() {
            public void run() {
                try {
                    callback.onSuccess(Uninterruptibles.getUninterruptibly(future));
                } catch (ExecutionException e) {
                    callback.onFailure(e.getCause());
                } catch (RuntimeException e2) {
                    callback.onFailure(e2);
                } catch (Error e3) {
                    callback.onFailure(e3);
                }
            }
        }, executor);
    }

    public static <V, X extends Exception> V get(Future<V> future, Class<X> exceptionClass) throws Exception {
        Preconditions.checkNotNull(future);
        Preconditions.checkArgument(RuntimeException.class.isAssignableFrom(exceptionClass) ^ 1, "Futures.get exception type (%s) must not be a RuntimeException", exceptionClass);
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw newWithCause(exceptionClass, e);
        } catch (ExecutionException e2) {
            wrapAndThrowExceptionOrError(e2.getCause(), exceptionClass);
            throw new AssertionError();
        }
    }

    public static <V, X extends Exception> V get(Future<V> future, long timeout, TimeUnit unit, Class<X> exceptionClass) throws Exception {
        Preconditions.checkNotNull(future);
        Preconditions.checkNotNull(unit);
        Preconditions.checkArgument(RuntimeException.class.isAssignableFrom(exceptionClass) ^ 1, "Futures.get exception type (%s) must not be a RuntimeException", exceptionClass);
        try {
            return future.get(timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw newWithCause(exceptionClass, e);
        } catch (TimeoutException e2) {
            throw newWithCause(exceptionClass, e2);
        } catch (ExecutionException e3) {
            wrapAndThrowExceptionOrError(e3.getCause(), exceptionClass);
            throw new AssertionError();
        }
    }

    private static <X extends Exception> void wrapAndThrowExceptionOrError(Throwable cause, Class<X> exceptionClass) throws Exception {
        if (cause instanceof Error) {
            throw new ExecutionError((Error) cause);
        } else if (cause instanceof RuntimeException) {
            throw new UncheckedExecutionException(cause);
        } else {
            throw newWithCause(exceptionClass, cause);
        }
    }

    public static <V> V getUnchecked(Future<V> future) {
        Preconditions.checkNotNull(future);
        try {
            return Uninterruptibles.getUninterruptibly(future);
        } catch (ExecutionException e) {
            wrapAndThrowUnchecked(e.getCause());
            throw new AssertionError();
        }
    }

    private static void wrapAndThrowUnchecked(Throwable cause) {
        if (cause instanceof Error) {
            throw new ExecutionError((Error) cause);
        }
        throw new UncheckedExecutionException(cause);
    }

    private static <X extends Exception> X newWithCause(Class<X> exceptionClass, Throwable cause) {
        for (Constructor<X> constructor : preferringStrings(Arrays.asList(exceptionClass.getConstructors()))) {
            Exception instance = (Exception) newFromConstructor(constructor, cause);
            if (instance != null) {
                if (instance.getCause() == null) {
                    instance.initCause(cause);
                }
                return instance;
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("No appropriate constructor for exception of type ");
        stringBuilder.append(exceptionClass);
        stringBuilder.append(" in response to chained exception");
        throw new IllegalArgumentException(stringBuilder.toString(), cause);
    }

    private static <X extends Exception> List<Constructor<X>> preferringStrings(List<Constructor<X>> constructors) {
        return WITH_STRING_PARAM_FIRST.sortedCopy(constructors);
    }

    @Nullable
    private static <X> X newFromConstructor(Constructor<X> constructor, Throwable cause) {
        Class<?>[] paramTypes = constructor.getParameterTypes();
        Object[] params = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> paramType = paramTypes[i];
            if (paramType.equals(String.class)) {
                params[i] = cause.toString();
            } else if (!paramType.equals(Throwable.class)) {
                return null;
            } else {
                params[i] = cause;
            }
        }
        try {
            return constructor.newInstance(params);
        } catch (IllegalArgumentException e) {
            return null;
        } catch (InstantiationException e2) {
            return null;
        } catch (IllegalAccessException e3) {
            return null;
        } catch (InvocationTargetException e4) {
            return null;
        }
    }

    private static <V> ListenableFuture<List<V>> listFuture(ImmutableList<ListenableFuture<? extends V>> futures, boolean allMustSucceed, Executor listenerExecutor) {
        return new CombinedFuture(futures, allMustSucceed, listenerExecutor, new FutureCombiner<V, List<V>>() {
            public List<V> combine(List<Optional<V>> values) {
                List<V> result = Lists.newArrayList();
                for (Optional<V> element : values) {
                    result.add(element != null ? element.orNull() : null);
                }
                return Collections.unmodifiableList(result);
            }
        });
    }
}
