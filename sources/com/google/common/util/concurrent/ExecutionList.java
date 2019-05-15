package com.google.common.util.concurrent;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

public final class ExecutionList {
    @VisibleForTesting
    static final Logger log = Logger.getLogger(ExecutionList.class.getName());
    @GuardedBy("this")
    private boolean executed;
    @GuardedBy("this")
    private RunnableExecutorPair runnables;

    private static final class RunnableExecutorPair {
        final Executor executor;
        @Nullable
        RunnableExecutorPair next;
        final Runnable runnable;

        RunnableExecutorPair(Runnable runnable, Executor executor, RunnableExecutorPair next) {
            this.runnable = runnable;
            this.executor = executor;
            this.next = next;
        }
    }

    public void add(Runnable runnable, Executor executor) {
        Preconditions.checkNotNull(runnable, "Runnable was null.");
        Preconditions.checkNotNull(executor, "Executor was null.");
        synchronized (this) {
            if (this.executed) {
                executeListener(runnable, executor);
                return;
            }
            this.runnables = new RunnableExecutorPair(runnable, executor, this.runnables);
        }
    }

    /* JADX WARNING: Missing block: B:8:0x0011, code skipped:
            if (r0 == null) goto L_0x001a;
     */
    /* JADX WARNING: Missing block: B:9:0x0013, code skipped:
            r2 = r0;
            r0 = r0.next;
            r2.next = r1;
            r1 = r2;
     */
    /* JADX WARNING: Missing block: B:10:0x001a, code skipped:
            if (r1 == null) goto L_0x0026;
     */
    /* JADX WARNING: Missing block: B:11:0x001c, code skipped:
            executeListener(r1.runnable, r1.executor);
            r1 = r1.next;
     */
    /* JADX WARNING: Missing block: B:12:0x0026, code skipped:
            return;
     */
    public void execute() {
        /*
        r4 = this;
        monitor-enter(r4);
        r0 = r4.executed;	 Catch:{ all -> 0x0027 }
        if (r0 == 0) goto L_0x0007;
    L_0x0005:
        monitor-exit(r4);	 Catch:{ all -> 0x0027 }
        return;
    L_0x0007:
        r0 = 1;
        r4.executed = r0;	 Catch:{ all -> 0x0027 }
        r0 = r4.runnables;	 Catch:{ all -> 0x0027 }
        r1 = 0;
        r4.runnables = r1;	 Catch:{ all -> 0x0027 }
        monitor-exit(r4);	 Catch:{ all -> 0x0027 }
    L_0x0011:
        if (r0 == 0) goto L_0x001a;
    L_0x0013:
        r2 = r0;
        r0 = r0.next;
        r2.next = r1;
        r1 = r2;
        goto L_0x0011;
    L_0x001a:
        if (r1 == 0) goto L_0x0026;
    L_0x001c:
        r2 = r1.runnable;
        r3 = r1.executor;
        executeListener(r2, r3);
        r1 = r1.next;
        goto L_0x001a;
    L_0x0026:
        return;
    L_0x0027:
        r0 = move-exception;
        monitor-exit(r4);	 Catch:{ all -> 0x0027 }
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.common.util.concurrent.ExecutionList.execute():void");
    }

    private static void executeListener(Runnable runnable, Executor executor) {
        try {
            executor.execute(runnable);
        } catch (RuntimeException e) {
            Logger logger = log;
            Level level = Level.SEVERE;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("RuntimeException while executing runnable ");
            stringBuilder.append(runnable);
            stringBuilder.append(" with executor ");
            stringBuilder.append(executor);
            logger.log(level, stringBuilder.toString(), e);
        }
    }
}
