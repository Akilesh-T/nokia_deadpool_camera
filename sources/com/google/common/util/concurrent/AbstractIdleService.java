package com.google.common.util.concurrent;

import com.google.common.annotations.Beta;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.Service.Listener;
import com.google.common.util.concurrent.Service.State;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Beta
public abstract class AbstractIdleService implements Service {
    private final Service delegate = new AbstractService() {
        /* Access modifiers changed, original: protected|final */
        public final void doStart() {
            MoreExecutors.renamingDecorator(AbstractIdleService.this.executor(), AbstractIdleService.this.threadNameSupplier).execute(new Runnable() {
                public void run() {
                    try {
                        AbstractIdleService.this.startUp();
                        AnonymousClass2.this.notifyStarted();
                    } catch (Throwable t) {
                        AnonymousClass2.this.notifyFailed(t);
                        RuntimeException propagate = Throwables.propagate(t);
                    }
                }
            });
        }

        /* Access modifiers changed, original: protected|final */
        public final void doStop() {
            MoreExecutors.renamingDecorator(AbstractIdleService.this.executor(), AbstractIdleService.this.threadNameSupplier).execute(new Runnable() {
                public void run() {
                    try {
                        AbstractIdleService.this.shutDown();
                        AnonymousClass2.this.notifyStopped();
                    } catch (Throwable t) {
                        AnonymousClass2.this.notifyFailed(t);
                        RuntimeException propagate = Throwables.propagate(t);
                    }
                }
            });
        }
    };
    private final Supplier<String> threadNameSupplier = new Supplier<String>() {
        public String get() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(AbstractIdleService.this.serviceName());
            stringBuilder.append(" ");
            stringBuilder.append(AbstractIdleService.this.state());
            return stringBuilder.toString();
        }
    };

    public abstract void shutDown() throws Exception;

    public abstract void startUp() throws Exception;

    protected AbstractIdleService() {
    }

    /* Access modifiers changed, original: protected */
    public Executor executor() {
        return new Executor() {
            public void execute(Runnable command) {
                MoreExecutors.newThread((String) AbstractIdleService.this.threadNameSupplier.get(), command).start();
            }
        };
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

    /* Access modifiers changed, original: protected */
    public String serviceName() {
        return getClass().getSimpleName();
    }
}
