package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.logging.Logger;
import javax.annotation.concurrent.GuardedBy;

final class SerializingExecutor implements Executor {
    private static final Logger log = Logger.getLogger(SerializingExecutor.class.getName());
    private final Executor executor;
    private final Object internalLock = new Object() {
        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SerializingExecutor lock: ");
            stringBuilder.append(super.toString());
            return stringBuilder.toString();
        }
    };
    @GuardedBy("internalLock")
    private boolean isThreadScheduled = false;
    private final TaskRunner taskRunner = new TaskRunner(this, null);
    @GuardedBy("internalLock")
    private final Queue<Runnable> waitQueue = new ArrayDeque();

    private class TaskRunner implements Runnable {
        private TaskRunner() {
        }

        /* synthetic */ TaskRunner(SerializingExecutor x0, AnonymousClass1 x1) {
            this();
        }

        /* JADX WARNING: Missing block: B:10:0x0027, code skipped:
            if (false == false) goto L_?;
     */
        /* JADX WARNING: Missing block: B:11:0x0029, code skipped:
            r2 = com.google.common.util.concurrent.SerializingExecutor.access$200(r8.this$0);
     */
        /* JADX WARNING: Missing block: B:12:0x002f, code skipped:
            monitor-enter(r2);
     */
        /* JADX WARNING: Missing block: B:14:?, code skipped:
            com.google.common.util.concurrent.SerializingExecutor.access$102(r8.this$0, false);
     */
        /* JADX WARNING: Missing block: B:15:0x0035, code skipped:
            monitor-exit(r2);
     */
        /* JADX WARNING: Missing block: B:23:?, code skipped:
            r3.run();
     */
        /* JADX WARNING: Missing block: B:50:?, code skipped:
            return;
     */
        /* JADX WARNING: Missing block: B:51:?, code skipped:
            return;
     */
        public void run() {
            /*
            r8 = this;
            r0 = 1;
        L_0x0001:
            r1 = 0;
            r2 = com.google.common.util.concurrent.SerializingExecutor.this;	 Catch:{ all -> 0x005f }
            r2 = r2.isThreadScheduled;	 Catch:{ all -> 0x005f }
            com.google.common.base.Preconditions.checkState(r2);	 Catch:{ all -> 0x005f }
            r2 = com.google.common.util.concurrent.SerializingExecutor.this;	 Catch:{ all -> 0x005f }
            r2 = r2.internalLock;	 Catch:{ all -> 0x005f }
            monitor-enter(r2);	 Catch:{ all -> 0x005f }
            r3 = com.google.common.util.concurrent.SerializingExecutor.this;	 Catch:{ all -> 0x005c }
            r3 = r3.waitQueue;	 Catch:{ all -> 0x005c }
            r3 = r3.poll();	 Catch:{ all -> 0x005c }
            r3 = (java.lang.Runnable) r3;	 Catch:{ all -> 0x005c }
            if (r3 != 0) goto L_0x003b;
        L_0x0020:
            r4 = com.google.common.util.concurrent.SerializingExecutor.this;	 Catch:{ all -> 0x005c }
            r4.isThreadScheduled = r1;	 Catch:{ all -> 0x005c }
            r0 = 0;
            monitor-exit(r2);	 Catch:{ all -> 0x005c }
            if (r0 == 0) goto L_0x003a;
        L_0x0029:
            r2 = com.google.common.util.concurrent.SerializingExecutor.this;
            r2 = r2.internalLock;
            monitor-enter(r2);
            r3 = com.google.common.util.concurrent.SerializingExecutor.this;	 Catch:{ all -> 0x0037 }
            r3.isThreadScheduled = r1;	 Catch:{ all -> 0x0037 }
            monitor-exit(r2);	 Catch:{ all -> 0x0037 }
            goto L_0x003a;
        L_0x0037:
            r1 = move-exception;
            monitor-exit(r2);	 Catch:{ all -> 0x0037 }
            throw r1;
        L_0x003a:
            return;
        L_0x003b:
            monitor-exit(r2);	 Catch:{ all -> 0x005c }
            r3.run();	 Catch:{ RuntimeException -> 0x0040 }
            goto L_0x005b;
        L_0x0040:
            r2 = move-exception;
            r4 = com.google.common.util.concurrent.SerializingExecutor.log;	 Catch:{ all -> 0x005f }
            r5 = java.util.logging.Level.SEVERE;	 Catch:{ all -> 0x005f }
            r6 = new java.lang.StringBuilder;	 Catch:{ all -> 0x005f }
            r6.<init>();	 Catch:{ all -> 0x005f }
            r7 = "Exception while executing runnable ";
            r6.append(r7);	 Catch:{ all -> 0x005f }
            r6.append(r3);	 Catch:{ all -> 0x005f }
            r6 = r6.toString();	 Catch:{ all -> 0x005f }
            r4.log(r5, r6, r2);	 Catch:{ all -> 0x005f }
        L_0x005b:
            goto L_0x0001;
        L_0x005c:
            r3 = move-exception;
            monitor-exit(r2);	 Catch:{ all -> 0x005c }
            throw r3;	 Catch:{ all -> 0x005f }
        L_0x005f:
            r2 = move-exception;
            if (r0 == 0) goto L_0x0073;
        L_0x0062:
            r3 = com.google.common.util.concurrent.SerializingExecutor.this;
            r3 = r3.internalLock;
            monitor-enter(r3);
            r4 = com.google.common.util.concurrent.SerializingExecutor.this;	 Catch:{ all -> 0x0070 }
            r4.isThreadScheduled = r1;	 Catch:{ all -> 0x0070 }
            monitor-exit(r3);	 Catch:{ all -> 0x0070 }
            goto L_0x0073;
        L_0x0070:
            r1 = move-exception;
            monitor-exit(r3);	 Catch:{ all -> 0x0070 }
            throw r1;
        L_0x0073:
            throw r2;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.google.common.util.concurrent.SerializingExecutor$TaskRunner.run():void");
        }
    }

    public SerializingExecutor(Executor executor) {
        Preconditions.checkNotNull(executor, "'executor' must not be null.");
        this.executor = executor;
    }

    public void execute(Runnable r) {
        Preconditions.checkNotNull(r, "'r' must not be null.");
        boolean scheduleTaskRunner = false;
        synchronized (this.internalLock) {
            this.waitQueue.add(r);
            if (!this.isThreadScheduled) {
                this.isThreadScheduled = true;
                scheduleTaskRunner = true;
            }
        }
        if (scheduleTaskRunner) {
            boolean threw = true;
            try {
                this.executor.execute(this.taskRunner);
                if (false) {
                    synchronized (this.internalLock) {
                        this.isThreadScheduled = false;
                    }
                }
            } catch (Throwable th) {
                if (threw) {
                    synchronized (this.internalLock) {
                        this.isThreadScheduled = false;
                    }
                }
            }
        }
    }
}
