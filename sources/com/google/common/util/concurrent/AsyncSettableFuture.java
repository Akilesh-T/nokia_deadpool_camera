package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import javax.annotation.Nullable;

final class AsyncSettableFuture<V> extends ForwardingListenableFuture<V> {
    private final ListenableFuture<V> dereferenced = Futures.dereference(this.nested);
    private final NestedFuture<V> nested = new NestedFuture();

    private static final class NestedFuture<V> extends AbstractFuture<ListenableFuture<? extends V>> {
        private NestedFuture() {
        }

        /* Access modifiers changed, original: 0000 */
        public boolean setFuture(ListenableFuture<? extends V> value) {
            boolean result = set(value);
            if (isCancelled()) {
                value.cancel(wasInterrupted());
            }
            return result;
        }
    }

    public static <V> AsyncSettableFuture<V> create() {
        return new AsyncSettableFuture();
    }

    private AsyncSettableFuture() {
    }

    /* Access modifiers changed, original: protected */
    public ListenableFuture<V> delegate() {
        return this.dereferenced;
    }

    public boolean setFuture(ListenableFuture<? extends V> future) {
        return this.nested.setFuture((ListenableFuture) Preconditions.checkNotNull(future));
    }

    public boolean setValue(@Nullable V value) {
        return setFuture(Futures.immediateFuture(value));
    }

    public boolean setException(Throwable exception) {
        return setFuture(Futures.immediateFailedFuture(exception));
    }

    public boolean isSet() {
        return this.nested.isDone();
    }
}
