package com.google.common.util.concurrent;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ForwardingListenableFuture.SimpleForwardingListenableFuture;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class MoreExecutors {

    @VisibleForTesting
    static class Application {
        Application() {
        }

        /* Access modifiers changed, original: final */
        public final ExecutorService getExitingExecutorService(ThreadPoolExecutor executor, long terminationTimeout, TimeUnit timeUnit) {
            MoreExecutors.useDaemonThreadFactory(executor);
            ExecutorService service = Executors.unconfigurableExecutorService(executor);
            addDelayedShutdownHook(service, terminationTimeout, timeUnit);
            return service;
        }

        /* Access modifiers changed, original: final */
        public final ScheduledExecutorService getExitingScheduledExecutorService(ScheduledThreadPoolExecutor executor, long terminationTimeout, TimeUnit timeUnit) {
            MoreExecutors.useDaemonThreadFactory(executor);
            ScheduledExecutorService service = Executors.unconfigurableScheduledExecutorService(executor);
            addDelayedShutdownHook(service, terminationTimeout, timeUnit);
            return service;
        }

        /* Access modifiers changed, original: final */
        public final void addDelayedShutdownHook(ExecutorService service, long terminationTimeout, TimeUnit timeUnit) {
            Preconditions.checkNotNull(service);
            Preconditions.checkNotNull(timeUnit);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DelayedShutdownHook-for-");
            stringBuilder.append(service);
            final ExecutorService executorService = service;
            final long j = terminationTimeout;
            final TimeUnit timeUnit2 = timeUnit;
            addShutdownHook(MoreExecutors.newThread(stringBuilder.toString(), new Runnable() {
                public void run() {
                    try {
                        executorService.shutdown();
                        executorService.awaitTermination(j, timeUnit2);
                    } catch (InterruptedException e) {
                    }
                }
            }));
        }

        /* Access modifiers changed, original: final */
        public final ExecutorService getExitingExecutorService(ThreadPoolExecutor executor) {
            return getExitingExecutorService(executor, 120, TimeUnit.SECONDS);
        }

        /* Access modifiers changed, original: final */
        public final ScheduledExecutorService getExitingScheduledExecutorService(ScheduledThreadPoolExecutor executor) {
            return getExitingScheduledExecutorService(executor, 120, TimeUnit.SECONDS);
        }

        /* Access modifiers changed, original: 0000 */
        @VisibleForTesting
        public void addShutdownHook(Thread hook) {
            Runtime.getRuntime().addShutdownHook(hook);
        }
    }

    private enum DirectExecutor implements Executor {
        INSTANCE;

        public void execute(Runnable command) {
            command.run();
        }
    }

    private static class DirectExecutorService extends AbstractListeningExecutorService {
        private final Lock lock;
        private int runningTasks;
        private boolean shutdown;
        private final Condition termination;

        private DirectExecutorService() {
            this.lock = new ReentrantLock();
            this.termination = this.lock.newCondition();
            this.runningTasks = 0;
            this.shutdown = false;
        }

        /* synthetic */ DirectExecutorService(AnonymousClass1 x0) {
            this();
        }

        public void execute(Runnable command) {
            startTask();
            try {
                command.run();
            } finally {
                endTask();
            }
        }

        public boolean isShutdown() {
            this.lock.lock();
            try {
                boolean z = this.shutdown;
                return z;
            } finally {
                this.lock.unlock();
            }
        }

        public void shutdown() {
            this.lock.lock();
            try {
                this.shutdown = true;
            } finally {
                this.lock.unlock();
            }
        }

        public List<Runnable> shutdownNow() {
            shutdown();
            return Collections.emptyList();
        }

        public boolean isTerminated() {
            this.lock.lock();
            try {
                boolean z = this.shutdown && this.runningTasks == 0;
                this.lock.unlock();
                return z;
            } catch (Throwable th) {
                this.lock.unlock();
            }
        }

        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            long nanos = unit.toNanos(timeout);
            this.lock.lock();
            while (!isTerminated()) {
                try {
                    if (nanos <= 0) {
                        return false;
                    }
                    nanos = this.termination.awaitNanos(nanos);
                } finally {
                    this.lock.unlock();
                }
            }
            this.lock.unlock();
            return true;
        }

        private void startTask() {
            this.lock.lock();
            try {
                if (isShutdown()) {
                    throw new RejectedExecutionException("Executor already shutdown");
                }
                this.runningTasks++;
            } finally {
                this.lock.unlock();
            }
        }

        private void endTask() {
            this.lock.lock();
            try {
                this.runningTasks--;
                if (isTerminated()) {
                    this.termination.signalAll();
                }
                this.lock.unlock();
            } catch (Throwable th) {
                this.lock.unlock();
            }
        }
    }

    private static class ListeningDecorator extends AbstractListeningExecutorService {
        private final ExecutorService delegate;

        ListeningDecorator(ExecutorService delegate) {
            this.delegate = (ExecutorService) Preconditions.checkNotNull(delegate);
        }

        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return this.delegate.awaitTermination(timeout, unit);
        }

        public boolean isShutdown() {
            return this.delegate.isShutdown();
        }

        public boolean isTerminated() {
            return this.delegate.isTerminated();
        }

        public void shutdown() {
            this.delegate.shutdown();
        }

        public List<Runnable> shutdownNow() {
            return this.delegate.shutdownNow();
        }

        public void execute(Runnable command) {
            this.delegate.execute(command);
        }
    }

    private static class ScheduledListeningDecorator extends ListeningDecorator implements ListeningScheduledExecutorService {
        final ScheduledExecutorService delegate;

        private static final class NeverSuccessfulListenableFutureTask extends AbstractFuture<Void> implements Runnable {
            private final Runnable delegate;

            public NeverSuccessfulListenableFutureTask(Runnable delegate) {
                this.delegate = (Runnable) Preconditions.checkNotNull(delegate);
            }

            public void run() {
                try {
                    this.delegate.run();
                } catch (Throwable t) {
                    setException(t);
                    RuntimeException propagate = Throwables.propagate(t);
                }
            }
        }

        private static final class ListenableScheduledTask<V> extends SimpleForwardingListenableFuture<V> implements ListenableScheduledFuture<V> {
            private final ScheduledFuture<?> scheduledDelegate;

            public ListenableScheduledTask(ListenableFuture<V> listenableDelegate, ScheduledFuture<?> scheduledDelegate) {
                super(listenableDelegate);
                this.scheduledDelegate = scheduledDelegate;
            }

            public boolean cancel(boolean mayInterruptIfRunning) {
                boolean cancelled = super.cancel(mayInterruptIfRunning);
                if (cancelled) {
                    this.scheduledDelegate.cancel(mayInterruptIfRunning);
                }
                return cancelled;
            }

            public long getDelay(TimeUnit unit) {
                return this.scheduledDelegate.getDelay(unit);
            }

            public int compareTo(Delayed other) {
                return this.scheduledDelegate.compareTo(other);
            }
        }

        ScheduledListeningDecorator(ScheduledExecutorService delegate) {
            super(delegate);
            this.delegate = (ScheduledExecutorService) Preconditions.checkNotNull(delegate);
        }

        public ListenableScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            ListenableFutureTask<Void> task = ListenableFutureTask.create(command, null);
            return new ListenableScheduledTask(task, this.delegate.schedule(task, delay, unit));
        }

        public <V> ListenableScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            ListenableFutureTask<V> task = ListenableFutureTask.create(callable);
            return new ListenableScheduledTask(task, this.delegate.schedule(task, delay, unit));
        }

        public ListenableScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            NeverSuccessfulListenableFutureTask task = new NeverSuccessfulListenableFutureTask(command);
            return new ListenableScheduledTask(task, this.delegate.scheduleAtFixedRate(task, initialDelay, period, unit));
        }

        public ListenableScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            NeverSuccessfulListenableFutureTask task = new NeverSuccessfulListenableFutureTask(command);
            return new ListenableScheduledTask(task, this.delegate.scheduleWithFixedDelay(task, initialDelay, delay, unit));
        }
    }

    private MoreExecutors() {
    }

    @Beta
    public static ExecutorService getExitingExecutorService(ThreadPoolExecutor executor, long terminationTimeout, TimeUnit timeUnit) {
        return new Application().getExitingExecutorService(executor, terminationTimeout, timeUnit);
    }

    @Beta
    public static ScheduledExecutorService getExitingScheduledExecutorService(ScheduledThreadPoolExecutor executor, long terminationTimeout, TimeUnit timeUnit) {
        return new Application().getExitingScheduledExecutorService(executor, terminationTimeout, timeUnit);
    }

    @Beta
    public static void addDelayedShutdownHook(ExecutorService service, long terminationTimeout, TimeUnit timeUnit) {
        new Application().addDelayedShutdownHook(service, terminationTimeout, timeUnit);
    }

    @Beta
    public static ExecutorService getExitingExecutorService(ThreadPoolExecutor executor) {
        return new Application().getExitingExecutorService(executor);
    }

    @Beta
    public static ScheduledExecutorService getExitingScheduledExecutorService(ScheduledThreadPoolExecutor executor) {
        return new Application().getExitingScheduledExecutorService(executor);
    }

    private static void useDaemonThreadFactory(ThreadPoolExecutor executor) {
        executor.setThreadFactory(new ThreadFactoryBuilder().setDaemon(true).setThreadFactory(executor.getThreadFactory()).build());
    }

    @Deprecated
    public static ListeningExecutorService sameThreadExecutor() {
        return new DirectExecutorService();
    }

    public static ListeningExecutorService newDirectExecutorService() {
        return new DirectExecutorService();
    }

    public static Executor directExecutor() {
        return DirectExecutor.INSTANCE;
    }

    public static ListeningExecutorService listeningDecorator(ExecutorService delegate) {
        if (delegate instanceof ListeningExecutorService) {
            return (ListeningExecutorService) delegate;
        }
        if (delegate instanceof ScheduledExecutorService) {
            return new ScheduledListeningDecorator((ScheduledExecutorService) delegate);
        }
        return new ListeningDecorator(delegate);
    }

    public static ListeningScheduledExecutorService listeningDecorator(ScheduledExecutorService delegate) {
        if (delegate instanceof ListeningScheduledExecutorService) {
            return (ListeningScheduledExecutorService) delegate;
        }
        return new ScheduledListeningDecorator(delegate);
    }

    /* JADX WARNING: Removed duplicated region for block: B:53:0x00cc A:{LOOP_END, LOOP:2: B:51:0x00c6->B:53:0x00cc} */
    static <T> T invokeAnyImpl(com.google.common.util.concurrent.ListeningExecutorService r18, java.util.Collection<? extends java.util.concurrent.Callable<T>> r19, boolean r20, long r21) throws java.lang.InterruptedException, java.util.concurrent.ExecutionException, java.util.concurrent.TimeoutException {
        /*
        r1 = r18;
        com.google.common.base.Preconditions.checkNotNull(r18);
        r2 = r19.size();
        r3 = 1;
        if (r2 <= 0) goto L_0x000e;
    L_0x000c:
        r0 = r3;
        goto L_0x000f;
    L_0x000e:
        r0 = 0;
    L_0x000f:
        com.google.common.base.Preconditions.checkArgument(r0);
        r4 = com.google.common.collect.Lists.newArrayListWithCapacity(r2);
        r0 = com.google.common.collect.Queues.newLinkedBlockingQueue();
        r5 = r0;
        r0 = 0;
        if (r20 == 0) goto L_0x0028;
    L_0x001e:
        r6 = java.lang.System.nanoTime();	 Catch:{ all -> 0x0023 }
        goto L_0x002a;
    L_0x0023:
        r0 = move-exception;
        r9 = r21;
        goto L_0x00c2;
    L_0x0028:
        r6 = 0;
    L_0x002a:
        r8 = r19.iterator();	 Catch:{ all -> 0x0023 }
        r9 = r8.next();	 Catch:{ all -> 0x0023 }
        r9 = (java.util.concurrent.Callable) r9;	 Catch:{ all -> 0x0023 }
        r9 = submitAndAddQueueListener(r1, r9, r5);	 Catch:{ all -> 0x0023 }
        r4.add(r9);	 Catch:{ all -> 0x0023 }
        r2 = r2 + -1;
        r9 = r21;
        r11 = r6;
        r6 = r0;
        r0 = r3;
    L_0x0042:
        r7 = r5.poll();	 Catch:{ all -> 0x00c1 }
        r7 = (java.util.concurrent.Future) r7;	 Catch:{ all -> 0x00c1 }
        if (r7 != 0) goto L_0x008e;
    L_0x004a:
        if (r2 <= 0) goto L_0x005e;
    L_0x004c:
        r2 = r2 + -1;
        r13 = r8.next();	 Catch:{ all -> 0x00c1 }
        r13 = (java.util.concurrent.Callable) r13;	 Catch:{ all -> 0x00c1 }
        r13 = submitAndAddQueueListener(r1, r13, r5);	 Catch:{ all -> 0x00c1 }
        r4.add(r13);	 Catch:{ all -> 0x00c1 }
        r0 = r0 + 1;
        goto L_0x008e;
    L_0x005e:
        if (r0 != 0) goto L_0x006b;
        if (r6 != 0) goto L_0x006a;
    L_0x0063:
        r7 = new java.util.concurrent.ExecutionException;	 Catch:{ all -> 0x00c1 }
        r13 = 0;
        r7.<init>(r13);	 Catch:{ all -> 0x00c1 }
        r6 = r7;
    L_0x006a:
        throw r6;	 Catch:{ all -> 0x00c1 }
    L_0x006b:
        if (r20 == 0) goto L_0x0087;
    L_0x006d:
        r13 = java.util.concurrent.TimeUnit.NANOSECONDS;	 Catch:{ all -> 0x00c1 }
        r13 = r5.poll(r9, r13);	 Catch:{ all -> 0x00c1 }
        r13 = (java.util.concurrent.Future) r13;	 Catch:{ all -> 0x00c1 }
        r7 = r13;
        if (r7 == 0) goto L_0x0081;
    L_0x0078:
        r13 = java.lang.System.nanoTime();	 Catch:{ all -> 0x00c1 }
        r15 = r13 - r11;
        r9 = r9 - r15;
        r11 = r13;
        goto L_0x008e;
    L_0x0081:
        r13 = new java.util.concurrent.TimeoutException;	 Catch:{ all -> 0x00c1 }
        r13.<init>();	 Catch:{ all -> 0x00c1 }
        throw r13;	 Catch:{ all -> 0x00c1 }
    L_0x0087:
        r13 = r5.take();	 Catch:{ all -> 0x00c1 }
        r13 = (java.util.concurrent.Future) r13;	 Catch:{ all -> 0x00c1 }
        r7 = r13;
    L_0x008e:
        if (r7 == 0) goto L_0x00be;
    L_0x0090:
        r13 = r0 + -1;
        r0 = r7.get();	 Catch:{ ExecutionException -> 0x00b8, RuntimeException -> 0x00af }
        r14 = r4.iterator();
    L_0x009a:
        r15 = r14.hasNext();
        if (r15 == 0) goto L_0x00ae;
    L_0x00a0:
        r15 = r14.next();
        r1 = r15;
        r1 = (java.util.concurrent.Future) r1;
        r1.cancel(r3);
        r1 = r18;
        goto L_0x009a;
    L_0x00ae:
        return r0;
    L_0x00af:
        r0 = move-exception;
        r1 = r0;
        r1 = new java.util.concurrent.ExecutionException;	 Catch:{ all -> 0x00c1 }
        r1.<init>(r0);	 Catch:{ all -> 0x00c1 }
        r0 = r1;
        goto L_0x00bc;
    L_0x00b8:
        r0 = move-exception;
        r1 = r0;
    L_0x00bc:
        r6 = r0;
        r0 = r13;
    L_0x00be:
        r1 = r18;
        goto L_0x0042;
    L_0x00c1:
        r0 = move-exception;
    L_0x00c2:
        r1 = r4.iterator();
    L_0x00c6:
        r6 = r1.hasNext();
        if (r6 == 0) goto L_0x00d6;
    L_0x00cc:
        r6 = r1.next();
        r6 = (java.util.concurrent.Future) r6;
        r6.cancel(r3);
        goto L_0x00c6;
    L_0x00d6:
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.common.util.concurrent.MoreExecutors.invokeAnyImpl(com.google.common.util.concurrent.ListeningExecutorService, java.util.Collection, boolean, long):java.lang.Object");
    }

    private static <T> ListenableFuture<T> submitAndAddQueueListener(ListeningExecutorService executorService, Callable<T> task, final BlockingQueue<Future<T>> queue) {
        final ListenableFuture<T> future = executorService.submit((Callable) task);
        future.addListener(new Runnable() {
            public void run() {
                queue.add(future);
            }
        }, directExecutor());
        return future;
    }

    @Beta
    public static ThreadFactory platformThreadFactory() {
        if (!isAppEngine()) {
            return Executors.defaultThreadFactory();
        }
        try {
            return (ThreadFactory) Class.forName("com.google.appengine.api.ThreadManager").getMethod("currentRequestThreadFactory", new Class[0]).invoke(null, new Object[0]);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Couldn't invoke ThreadManager.currentRequestThreadFactory", e);
        } catch (ClassNotFoundException e2) {
            throw new RuntimeException("Couldn't invoke ThreadManager.currentRequestThreadFactory", e2);
        } catch (NoSuchMethodException e3) {
            throw new RuntimeException("Couldn't invoke ThreadManager.currentRequestThreadFactory", e3);
        } catch (InvocationTargetException e4) {
            throw Throwables.propagate(e4.getCause());
        }
    }

    private static boolean isAppEngine() {
        boolean z = false;
        if (System.getProperty("com.google.appengine.runtime.environment") == null) {
            return false;
        }
        try {
            if (Class.forName("com.google.apphosting.api.ApiProxy").getMethod("getCurrentEnvironment", new Class[0]).invoke(null, new Object[0]) != null) {
                z = true;
            }
            return z;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (InvocationTargetException e2) {
            return false;
        } catch (IllegalAccessException e3) {
            return false;
        } catch (NoSuchMethodException e4) {
            return false;
        }
    }

    static Thread newThread(String name, Runnable runnable) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(runnable);
        Thread result = platformThreadFactory().newThread(runnable);
        try {
            result.setName(name);
        } catch (SecurityException e) {
        }
        return result;
    }

    static Executor renamingDecorator(final Executor executor, final Supplier<String> nameSupplier) {
        Preconditions.checkNotNull(executor);
        Preconditions.checkNotNull(nameSupplier);
        if (isAppEngine()) {
            return executor;
        }
        return new Executor() {
            public void execute(Runnable command) {
                executor.execute(Callables.threadRenaming(command, nameSupplier));
            }
        };
    }

    static ExecutorService renamingDecorator(ExecutorService service, final Supplier<String> nameSupplier) {
        Preconditions.checkNotNull(service);
        Preconditions.checkNotNull(nameSupplier);
        if (isAppEngine()) {
            return service;
        }
        return new WrappingExecutorService(service) {
            /* Access modifiers changed, original: protected */
            public <T> Callable<T> wrapTask(Callable<T> callable) {
                return Callables.threadRenaming((Callable) callable, nameSupplier);
            }

            /* Access modifiers changed, original: protected */
            public Runnable wrapTask(Runnable command) {
                return Callables.threadRenaming(command, nameSupplier);
            }
        };
    }

    static ScheduledExecutorService renamingDecorator(ScheduledExecutorService service, final Supplier<String> nameSupplier) {
        Preconditions.checkNotNull(service);
        Preconditions.checkNotNull(nameSupplier);
        if (isAppEngine()) {
            return service;
        }
        return new WrappingScheduledExecutorService(service) {
            /* Access modifiers changed, original: protected */
            public <T> Callable<T> wrapTask(Callable<T> callable) {
                return Callables.threadRenaming((Callable) callable, nameSupplier);
            }

            /* Access modifiers changed, original: protected */
            public Runnable wrapTask(Runnable command) {
                return Callables.threadRenaming(command, nameSupplier);
            }
        };
    }

    @Beta
    public static boolean shutdownAndAwaitTermination(ExecutorService service, long timeout, TimeUnit unit) {
        Preconditions.checkNotNull(unit);
        service.shutdown();
        try {
            long halfTimeoutNanos = TimeUnit.NANOSECONDS.convert(timeout, unit) / 2;
            if (!service.awaitTermination(halfTimeoutNanos, TimeUnit.NANOSECONDS)) {
                service.shutdownNow();
                service.awaitTermination(halfTimeoutNanos, TimeUnit.NANOSECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            service.shutdownNow();
        }
        return service.isTerminated();
    }
}
