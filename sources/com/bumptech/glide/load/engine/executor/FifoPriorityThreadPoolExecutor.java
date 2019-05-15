package com.bumptech.glide.load.engine.executor;

import android.os.Process;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class FifoPriorityThreadPoolExecutor extends ThreadPoolExecutor {
    AtomicInteger ordering;

    public static class DefaultThreadFactory implements ThreadFactory {
        int threadNum = 0;

        public Thread newThread(Runnable runnable) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("fifo-pool-thread-");
            stringBuilder.append(this.threadNum);
            Thread result = new Thread(runnable, stringBuilder.toString()) {
                public void run() {
                    Process.setThreadPriority(10);
                    super.run();
                }
            };
            this.threadNum++;
            return result;
        }
    }

    static class LoadTask<T> extends FutureTask<T> implements Comparable<LoadTask<?>> {
        private final int order;
        private final int priority;

        public LoadTask(Runnable runnable, T result, int order) {
            super(runnable, result);
            if (runnable instanceof Prioritized) {
                this.priority = ((Prioritized) runnable).getPriority();
                this.order = order;
                return;
            }
            throw new IllegalArgumentException("FifoPriorityThreadPoolExecutor must be given Runnables that implement Prioritized");
        }

        public boolean equals(Object o) {
            boolean z = false;
            if (!(o instanceof LoadTask)) {
                return false;
            }
            LoadTask<Object> other = (LoadTask) o;
            if (this.order == other.order && this.priority == other.priority) {
                z = true;
            }
            return z;
        }

        public int hashCode() {
            return (31 * this.priority) + this.order;
        }

        public int compareTo(LoadTask<?> loadTask) {
            int result = this.priority - loadTask.priority;
            if (result == 0) {
                return this.order - loadTask.order;
            }
            return result;
        }
    }

    public FifoPriorityThreadPoolExecutor(int poolSize) {
        this(poolSize, poolSize, 0, TimeUnit.MILLISECONDS, new DefaultThreadFactory());
    }

    public FifoPriorityThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAlive, TimeUnit timeUnit, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAlive, timeUnit, new PriorityBlockingQueue(), threadFactory);
        this.ordering = new AtomicInteger();
    }

    /* Access modifiers changed, original: protected */
    public <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new LoadTask(runnable, value, this.ordering.getAndIncrement());
    }
}
