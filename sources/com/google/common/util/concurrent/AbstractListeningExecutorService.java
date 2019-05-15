package com.google.common.util.concurrent;

import com.google.common.annotations.Beta;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import javax.annotation.Nullable;

@Beta
public abstract class AbstractListeningExecutorService extends AbstractExecutorService implements ListeningExecutorService {
    /* Access modifiers changed, original: protected|final */
    public final <T> ListenableFutureTask<T> newTaskFor(Runnable runnable, T value) {
        return ListenableFutureTask.create(runnable, value);
    }

    /* Access modifiers changed, original: protected|final */
    public final <T> ListenableFutureTask<T> newTaskFor(Callable<T> callable) {
        return ListenableFutureTask.create(callable);
    }

    public ListenableFuture<?> submit(Runnable task) {
        return (ListenableFuture) super.submit(task);
    }

    public <T> ListenableFuture<T> submit(Runnable task, @Nullable T result) {
        return (ListenableFuture) super.submit(task, result);
    }

    public <T> ListenableFuture<T> submit(Callable<T> task) {
        return (ListenableFuture) super.submit(task);
    }
}
