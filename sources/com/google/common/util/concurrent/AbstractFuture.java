package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import javax.annotation.Nullable;

public abstract class AbstractFuture<V> implements ListenableFuture<V> {
    private final ExecutionList executionList = new ExecutionList();
    private final Sync<V> sync = new Sync();

    static final class Sync<V> extends AbstractQueuedSynchronizer {
        static final int CANCELLED = 4;
        static final int COMPLETED = 2;
        static final int COMPLETING = 1;
        static final int INTERRUPTED = 8;
        static final int RUNNING = 0;
        private static final long serialVersionUID = 0;
        private Throwable exception;
        private V value;

        Sync() {
        }

        /* Access modifiers changed, original: protected */
        public int tryAcquireShared(int ignored) {
            if (isDone()) {
                return 1;
            }
            return -1;
        }

        /* Access modifiers changed, original: protected */
        public boolean tryReleaseShared(int finalState) {
            setState(finalState);
            return true;
        }

        /* Access modifiers changed, original: 0000 */
        public V get(long nanos) throws TimeoutException, CancellationException, ExecutionException, InterruptedException {
            if (tryAcquireSharedNanos(-1, nanos)) {
                return getValue();
            }
            throw new TimeoutException("Timeout waiting for task.");
        }

        /* Access modifiers changed, original: 0000 */
        public V get() throws CancellationException, ExecutionException, InterruptedException {
            acquireSharedInterruptibly(-1);
            return getValue();
        }

        private V getValue() throws CancellationException, ExecutionException {
            int state = getState();
            if (state != 2) {
                if (state == 4 || state == 8) {
                    throw AbstractFuture.cancellationExceptionWithCause("Task was cancelled.", this.exception);
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Error, synchronizer in invalid state: ");
                stringBuilder.append(state);
                throw new IllegalStateException(stringBuilder.toString());
            } else if (this.exception == null) {
                return this.value;
            } else {
                throw new ExecutionException(this.exception);
            }
        }

        /* Access modifiers changed, original: 0000 */
        public boolean isDone() {
            return (getState() & 14) != 0;
        }

        /* Access modifiers changed, original: 0000 */
        public boolean isCancelled() {
            return (getState() & 12) != 0;
        }

        /* Access modifiers changed, original: 0000 */
        public boolean wasInterrupted() {
            return getState() == 8;
        }

        /* Access modifiers changed, original: 0000 */
        public boolean set(@Nullable V v) {
            return complete(v, null, 2);
        }

        /* Access modifiers changed, original: 0000 */
        public boolean setException(Throwable t) {
            return complete(null, t, 2);
        }

        /* Access modifiers changed, original: 0000 */
        public boolean cancel(boolean interrupt) {
            return complete(null, null, interrupt ? 8 : 4);
        }

        private boolean complete(@Nullable V v, @Nullable Throwable t, int finalState) {
            boolean doCompletion = compareAndSetState(false, 1);
            if (doCompletion) {
                this.value = v;
                this.exception = (finalState & 12) != 0 ? new CancellationException("Future.cancel() was called.") : t;
                releaseShared(finalState);
            } else if (getState() == 1) {
                acquireShared(-1);
            }
            return doCompletion;
        }
    }

    protected AbstractFuture() {
    }

    public V get(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException, ExecutionException {
        return this.sync.get(unit.toNanos(timeout));
    }

    public V get() throws InterruptedException, ExecutionException {
        return this.sync.get();
    }

    public boolean isDone() {
        return this.sync.isDone();
    }

    public boolean isCancelled() {
        return this.sync.isCancelled();
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        if (!this.sync.cancel(mayInterruptIfRunning)) {
            return false;
        }
        this.executionList.execute();
        if (mayInterruptIfRunning) {
            interruptTask();
        }
        return true;
    }

    /* Access modifiers changed, original: protected */
    public void interruptTask() {
    }

    /* Access modifiers changed, original: protected|final */
    public final boolean wasInterrupted() {
        return this.sync.wasInterrupted();
    }

    public void addListener(Runnable listener, Executor exec) {
        this.executionList.add(listener, exec);
    }

    /* Access modifiers changed, original: protected */
    public boolean set(@Nullable V value) {
        boolean result = this.sync.set(value);
        if (result) {
            this.executionList.execute();
        }
        return result;
    }

    /* Access modifiers changed, original: protected */
    public boolean setException(Throwable throwable) {
        boolean result = this.sync.setException((Throwable) Preconditions.checkNotNull(throwable));
        if (result) {
            this.executionList.execute();
        }
        return result;
    }

    static final CancellationException cancellationExceptionWithCause(@Nullable String message, @Nullable Throwable cause) {
        CancellationException exception = new CancellationException(message);
        exception.initCause(cause);
        return exception;
    }
}
