package com.google.common.util.concurrent;

import com.google.common.base.Preconditions;
import com.google.common.collect.Queues;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.concurrent.GuardedBy;

final class ListenerCallQueue<L> implements Runnable {
    private static final Logger logger = Logger.getLogger(ListenerCallQueue.class.getName());
    private final Executor executor;
    @GuardedBy("this")
    private boolean isThreadScheduled;
    private final L listener;
    @GuardedBy("this")
    private final Queue<Callback<L>> waitQueue = Queues.newArrayDeque();

    static abstract class Callback<L> {
        private final String methodCall;

        public abstract void call(L l);

        Callback(String methodCall) {
            this.methodCall = methodCall;
        }

        /* Access modifiers changed, original: 0000 */
        public void enqueueOn(Iterable<ListenerCallQueue<L>> queues) {
            for (ListenerCallQueue<L> queue : queues) {
                queue.add(this);
            }
        }
    }

    ListenerCallQueue(L listener, Executor executor) {
        this.listener = Preconditions.checkNotNull(listener);
        this.executor = (Executor) Preconditions.checkNotNull(executor);
    }

    /* Access modifiers changed, original: declared_synchronized */
    public synchronized void add(Callback<L> callback) {
        this.waitQueue.add(callback);
    }

    /* Access modifiers changed, original: 0000 */
    public void execute() {
        boolean scheduleTaskRunner = false;
        synchronized (this) {
            if (!this.isThreadScheduled) {
                this.isThreadScheduled = true;
                scheduleTaskRunner = true;
            }
        }
        if (scheduleTaskRunner) {
            try {
                this.executor.execute(this);
            } catch (RuntimeException e) {
                synchronized (this) {
                    this.isThreadScheduled = false;
                    Logger logger = logger;
                    Level level = Level.SEVERE;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Exception while running callbacks for ");
                    stringBuilder.append(this.listener);
                    stringBuilder.append(" on ");
                    stringBuilder.append(this.executor);
                    logger.log(level, stringBuilder.toString(), e);
                    throw e;
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:9:0x0016, code skipped:
            if (false == false) goto L_?;
     */
    /* JADX WARNING: Missing block: B:10:0x0018, code skipped:
            monitor-enter(r8);
     */
    /* JADX WARNING: Missing block: B:12:?, code skipped:
            r8.isThreadScheduled = false;
     */
    /* JADX WARNING: Missing block: B:13:0x001b, code skipped:
            monitor-exit(r8);
     */
    /* JADX WARNING: Missing block: B:21:?, code skipped:
            r2.call(r8.listener);
     */
    /* JADX WARNING: Missing block: B:47:?, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:48:?, code skipped:
            return;
     */
    public void run() {
        /*
        r8 = this;
        r0 = 1;
    L_0x0001:
        r1 = 0;
        monitor-enter(r8);	 Catch:{ all -> 0x0053 }
        r2 = r8.isThreadScheduled;	 Catch:{ all -> 0x0050 }
        com.google.common.base.Preconditions.checkState(r2);	 Catch:{ all -> 0x0050 }
        r2 = r8.waitQueue;	 Catch:{ all -> 0x0050 }
        r2 = r2.poll();	 Catch:{ all -> 0x0050 }
        r2 = (com.google.common.util.concurrent.ListenerCallQueue.Callback) r2;	 Catch:{ all -> 0x0050 }
        if (r2 != 0) goto L_0x0021;
    L_0x0012:
        r8.isThreadScheduled = r1;	 Catch:{ all -> 0x0050 }
        r0 = 0;
        monitor-exit(r8);	 Catch:{ all -> 0x0050 }
        if (r0 == 0) goto L_0x0020;
    L_0x0018:
        monitor-enter(r8);
        r8.isThreadScheduled = r1;	 Catch:{ all -> 0x001d }
        monitor-exit(r8);	 Catch:{ all -> 0x001d }
        goto L_0x0020;
    L_0x001d:
        r1 = move-exception;
        monitor-exit(r8);	 Catch:{ all -> 0x001d }
        throw r1;
    L_0x0020:
        return;
    L_0x0021:
        monitor-exit(r8);	 Catch:{ all -> 0x0050 }
        r3 = r8.listener;	 Catch:{ RuntimeException -> 0x0028 }
        r2.call(r3);	 Catch:{ RuntimeException -> 0x0028 }
        goto L_0x004f;
    L_0x0028:
        r3 = move-exception;
        r4 = logger;	 Catch:{ all -> 0x0053 }
        r5 = java.util.logging.Level.SEVERE;	 Catch:{ all -> 0x0053 }
        r6 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0053 }
        r6.<init>();	 Catch:{ all -> 0x0053 }
        r7 = "Exception while executing callback: ";
        r6.append(r7);	 Catch:{ all -> 0x0053 }
        r7 = r8.listener;	 Catch:{ all -> 0x0053 }
        r6.append(r7);	 Catch:{ all -> 0x0053 }
        r7 = ".";
        r6.append(r7);	 Catch:{ all -> 0x0053 }
        r7 = r2.methodCall;	 Catch:{ all -> 0x0053 }
        r6.append(r7);	 Catch:{ all -> 0x0053 }
        r6 = r6.toString();	 Catch:{ all -> 0x0053 }
        r4.log(r5, r6, r3);	 Catch:{ all -> 0x0053 }
    L_0x004f:
        goto L_0x0001;
    L_0x0050:
        r2 = move-exception;
        monitor-exit(r8);	 Catch:{ all -> 0x0050 }
        throw r2;	 Catch:{ all -> 0x0053 }
    L_0x0053:
        r2 = move-exception;
        if (r0 == 0) goto L_0x005e;
    L_0x0056:
        monitor-enter(r8);
        r8.isThreadScheduled = r1;	 Catch:{ all -> 0x005b }
        monitor-exit(r8);	 Catch:{ all -> 0x005b }
        goto L_0x005e;
    L_0x005b:
        r1 = move-exception;
        monitor-exit(r8);	 Catch:{ all -> 0x005b }
        throw r1;
    L_0x005e:
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.common.util.concurrent.ListenerCallQueue.run():void");
    }
}
