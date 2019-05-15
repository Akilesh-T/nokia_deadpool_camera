package com.google.common.util.concurrent;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Service.Listener;
import com.google.common.util.concurrent.Service.State;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import javax.annotation.concurrent.GuardedBy;

@Beta
public abstract class AbstractScheduledService implements Service {
    private static final Logger logger = Logger.getLogger(AbstractScheduledService.class.getName());
    private final AbstractService delegate = new AbstractService() {
        private volatile ScheduledExecutorService executorService;
        private final ReentrantLock lock = new ReentrantLock();
        private volatile Future<?> runningTask;
        private final Runnable task = new Runnable() {
            public void run() {
                AnonymousClass1.this.lock.lock();
                try {
                    AbstractScheduledService.this.runOneIteration();
                    AnonymousClass1.this.lock.unlock();
                } catch (Throwable th) {
                    AnonymousClass1.this.lock.unlock();
                }
            }
        };

        /* Access modifiers changed, original: protected|final */
        public final void doStart() {
            this.executorService = MoreExecutors.renamingDecorator(AbstractScheduledService.this.executor(), new Supplier<String>() {
                public String get() {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(AbstractScheduledService.this.serviceName());
                    stringBuilder.append(" ");
                    stringBuilder.append(AnonymousClass1.this.state());
                    return stringBuilder.toString();
                }
            });
            this.executorService.execute(new Runnable() {
                public void run() {
                    AnonymousClass1.this.lock.lock();
                    try {
                        AbstractScheduledService.this.startUp();
                        AnonymousClass1.this.runningTask = AbstractScheduledService.this.scheduler().schedule(AbstractScheduledService.this.delegate, AnonymousClass1.this.executorService, AnonymousClass1.this.task);
                        AnonymousClass1.this.notifyStarted();
                        AnonymousClass1.this.lock.unlock();
                    } catch (Throwable th) {
                        AnonymousClass1.this.lock.unlock();
                    }
                }
            });
        }

        /* Access modifiers changed, original: protected|final */
        public final void doStop() {
            this.runningTask.cancel(false);
            this.executorService.execute(new Runnable() {
                public void run() {
                    try {
                        AnonymousClass1.this.lock.lock();
                        if (AnonymousClass1.this.state() != State.STOPPING) {
                            AnonymousClass1.this.lock.unlock();
                            return;
                        }
                        AbstractScheduledService.this.shutDown();
                        AnonymousClass1.this.lock.unlock();
                        AnonymousClass1.this.notifyStopped();
                    } catch (Throwable t) {
                        AnonymousClass1.this.notifyFailed(t);
                        RuntimeException propagate = Throwables.propagate(t);
                    }
                }
            });
        }
    };

    public static abstract class Scheduler {
        public abstract Future<?> schedule(AbstractService abstractService, ScheduledExecutorService scheduledExecutorService, Runnable runnable);

        /* synthetic */ Scheduler(AnonymousClass1 x0) {
            this();
        }

        public static Scheduler newFixedDelaySchedule(long initialDelay, long delay, TimeUnit unit) {
            final long j = initialDelay;
            final long j2 = delay;
            final TimeUnit timeUnit = unit;
            return new Scheduler() {
                public Future<?> schedule(AbstractService service, ScheduledExecutorService executor, Runnable task) {
                    return executor.scheduleWithFixedDelay(task, j, j2, timeUnit);
                }
            };
        }

        public static Scheduler newFixedRateSchedule(long initialDelay, long period, TimeUnit unit) {
            final long j = initialDelay;
            final long j2 = period;
            final TimeUnit timeUnit = unit;
            return new Scheduler() {
                public Future<?> schedule(AbstractService service, ScheduledExecutorService executor, Runnable task) {
                    return executor.scheduleAtFixedRate(task, j, j2, timeUnit);
                }
            };
        }

        private Scheduler() {
        }
    }

    @Beta
    public static abstract class CustomScheduler extends Scheduler {

        @Beta
        protected static final class Schedule {
            private final long delay;
            private final TimeUnit unit;

            public Schedule(long delay, TimeUnit unit) {
                this.delay = delay;
                this.unit = (TimeUnit) Preconditions.checkNotNull(unit);
            }
        }

        private class ReschedulableCallable extends ForwardingFuture<Void> implements Callable<Void> {
            @GuardedBy("lock")
            private Future<Void> currentFuture;
            private final ScheduledExecutorService executor;
            private final ReentrantLock lock = new ReentrantLock();
            private final AbstractService service;
            private final Runnable wrappedRunnable;

            ReschedulableCallable(AbstractService service, ScheduledExecutorService executor, Runnable runnable) {
                this.wrappedRunnable = runnable;
                this.executor = executor;
                this.service = service;
            }

            public Void call() throws Exception {
                this.wrappedRunnable.run();
                reschedule();
                return null;
            }

            public void reschedule() {
                this.lock.lock();
                try {
                    if (this.currentFuture == null || !this.currentFuture.isCancelled()) {
                        Schedule schedule = CustomScheduler.this.getNextSchedule();
                        this.currentFuture = this.executor.schedule(this, schedule.delay, schedule.unit);
                    }
                } catch (Throwable th) {
                    this.lock.unlock();
                }
                this.lock.unlock();
            }

            public boolean cancel(boolean mayInterruptIfRunning) {
                this.lock.lock();
                try {
                    boolean cancel = this.currentFuture.cancel(mayInterruptIfRunning);
                    return cancel;
                } finally {
                    this.lock.unlock();
                }
            }

            /* Access modifiers changed, original: protected */
            public Future<Void> delegate() {
                throw new UnsupportedOperationException("Only cancel is supported by this future");
            }
        }

        public abstract Schedule getNextSchedule() throws Exception;

        public CustomScheduler() {
            super();
        }

        /* Access modifiers changed, original: final */
        public final Future<?> schedule(AbstractService service, ScheduledExecutorService executor, Runnable runnable) {
            ReschedulableCallable task = new ReschedulableCallable(service, executor, runnable);
            task.reschedule();
            return task;
        }
    }

    public abstract void runOneIteration() throws Exception;

    public abstract Scheduler scheduler();

    protected AbstractScheduledService() {
    }

    /* Access modifiers changed, original: protected */
    public void startUp() throws Exception {
    }

    /* Access modifiers changed, original: protected */
    public void shutDown() throws Exception {
    }

    /* Access modifiers changed, original: protected */
    public ScheduledExecutorService executor() {
        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            public Thread newThread(Runnable runnable) {
                return MoreExecutors.newThread(AbstractScheduledService.this.serviceName(), runnable);
            }
        });
        addListener(new Listener() {
            public void terminated(State from) {
                executor.shutdown();
            }

            public void failed(State from, Throwable failure) {
                executor.shutdown();
            }
        }, MoreExecutors.directExecutor());
        return executor;
    }

    /* Access modifiers changed, original: protected */
    public String serviceName() {
        return getClass().getSimpleName();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(serviceName());
        stringBuilder.append(" [");
        stringBuilder.append(state());
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    public final boolean isRunning() {
        return this.delegate.isRunning();
    }

    public final State state() {
        return this.delegate.state();
    }

    public final void addListener(Listener listener, Executor executor) {
        this.delegate.addListener(listener, executor);
    }

    public final Throwable failureCause() {
        return this.delegate.failureCause();
    }

    public final Service startAsync() {
        this.delegate.startAsync();
        return this;
    }

    public final Service stopAsync() {
        this.delegate.stopAsync();
        return this;
    }

    public final void awaitRunning() {
        this.delegate.awaitRunning();
    }

    public final void awaitRunning(long timeout, TimeUnit unit) throws TimeoutException {
        this.delegate.awaitRunning(timeout, unit);
    }

    public final void awaitTerminated() {
        this.delegate.awaitTerminated();
    }

    public final void awaitTerminated(long timeout, TimeUnit unit) throws TimeoutException {
        this.delegate.awaitTerminated(timeout, unit);
    }
}
