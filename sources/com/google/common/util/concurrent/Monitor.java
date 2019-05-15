package com.google.common.util.concurrent;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.concurrent.GuardedBy;

@Beta
public final class Monitor {
    @GuardedBy("lock")
    private Guard activeGuards;
    private final boolean fair;
    private final ReentrantLock lock;

    @Beta
    public static abstract class Guard {
        final Condition condition;
        final Monitor monitor;
        @GuardedBy("monitor.lock")
        Guard next;
        @GuardedBy("monitor.lock")
        int waiterCount = 0;

        public abstract boolean isSatisfied();

        protected Guard(Monitor monitor) {
            this.monitor = (Monitor) Preconditions.checkNotNull(monitor, "monitor");
            this.condition = monitor.lock.newCondition();
        }
    }

    public Monitor() {
        this(false);
    }

    public Monitor(boolean fair) {
        this.activeGuards = null;
        this.fair = fair;
        this.lock = new ReentrantLock(fair);
    }

    public void enter() {
        this.lock.lock();
    }

    public void enterInterruptibly() throws InterruptedException {
        this.lock.lockInterruptibly();
    }

    public boolean enter(long time, TimeUnit unit) {
        long timeoutNanos = unit.toNanos(time);
        ReentrantLock lock = this.lock;
        if (!this.fair && lock.tryLock()) {
            return true;
        }
        boolean tryLock;
        long deadline = System.nanoTime() + timeoutNanos;
        boolean interrupted = Thread.interrupted();
        while (true) {
            try {
                tryLock = lock.tryLock(timeoutNanos, TimeUnit.NANOSECONDS);
                break;
            } catch (InterruptedException e) {
                interrupted = true;
                timeoutNanos = deadline - System.nanoTime();
            } catch (Throwable th) {
                if (1 != null) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        return tryLock;
    }

    public boolean enterInterruptibly(long time, TimeUnit unit) throws InterruptedException {
        return this.lock.tryLock(time, unit);
    }

    public boolean tryEnter() {
        return this.lock.tryLock();
    }

    public void enterWhen(Guard guard) throws InterruptedException {
        if (guard.monitor == this) {
            ReentrantLock lock = this.lock;
            boolean signalBeforeWaiting = lock.isHeldByCurrentThread();
            lock.lockInterruptibly();
            boolean satisfied = false;
            try {
                if (!guard.isSatisfied()) {
                    await(guard, signalBeforeWaiting);
                }
                satisfied = true;
            } finally {
                if (!satisfied) {
                    leave();
                }
            }
        } else {
            throw new IllegalMonitorStateException();
        }
    }

    public void enterWhenUninterruptibly(Guard guard) {
        if (guard.monitor == this) {
            ReentrantLock lock = this.lock;
            boolean signalBeforeWaiting = lock.isHeldByCurrentThread();
            lock.lock();
            boolean satisfied = false;
            try {
                if (!guard.isSatisfied()) {
                    awaitUninterruptibly(guard, signalBeforeWaiting);
                }
                satisfied = true;
            } finally {
                if (!satisfied) {
                    leave();
                }
            }
        } else {
            throw new IllegalMonitorStateException();
        }
    }

    public boolean enterWhen(Guard guard, long time, TimeUnit unit) throws InterruptedException {
        long timeoutNanos = unit.toNanos(time);
        if (guard.monitor == this) {
            ReentrantLock lock = this.lock;
            boolean reentrant = lock.isHeldByCurrentThread();
            boolean z = false;
            if (this.fair || !lock.tryLock()) {
                long deadline = System.nanoTime() + timeoutNanos;
                if (!lock.tryLock(time, unit)) {
                    return false;
                }
                timeoutNanos = deadline - System.nanoTime();
            }
            boolean threw = true;
            try {
                if (guard.isSatisfied() || awaitNanos(guard, timeoutNanos, reentrant)) {
                    z = true;
                }
                boolean satisfied = z;
                if (!satisfied) {
                    if (false && !reentrant) {
                        try {
                            signalNextWaiter();
                        } catch (Throwable th) {
                            lock.unlock();
                        }
                    }
                    lock.unlock();
                }
                return satisfied;
            } catch (Throwable th2) {
                lock.unlock();
            }
        } else {
            throw new IllegalMonitorStateException();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:32:0x0061 A:{SYNTHETIC, Splitter:B:32:0x0061} */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x0066  */
    public boolean enterWhenUninterruptibly(com.google.common.util.concurrent.Monitor.Guard r18, long r19, java.util.concurrent.TimeUnit r21) {
        /*
        r17 = this;
        r1 = r17;
        r2 = r18;
        r3 = r19;
        r5 = r21;
        r6 = r5.toNanos(r3);
        r0 = r2.monitor;
        if (r0 != r1) goto L_0x008d;
    L_0x0010:
        r8 = r1.lock;
        r9 = java.lang.System.nanoTime();
        r9 = r9 + r6;
        r11 = r8.isHeldByCurrentThread();
        r0 = java.lang.Thread.interrupted();
        r12 = r0;
        r0 = r1.fair;	 Catch:{ all -> 0x0082 }
        r13 = 0;
        if (r0 != 0) goto L_0x002b;
    L_0x0025:
        r0 = r8.tryLock();	 Catch:{ all -> 0x0082 }
        if (r0 != 0) goto L_0x004d;
    L_0x002b:
        r0 = r13;
    L_0x002c:
        r14 = r0;
        r0 = java.util.concurrent.TimeUnit.NANOSECONDS;	 Catch:{ InterruptedException -> 0x0041 }
        r0 = r8.tryLock(r6, r0);	 Catch:{ InterruptedException -> 0x0041 }
        if (r0 != 0) goto L_0x0040;
        if (r12 == 0) goto L_0x003f;
    L_0x0038:
        r14 = java.lang.Thread.currentThread();
        r14.interrupt();
    L_0x003f:
        return r13;
    L_0x0040:
        goto L_0x0045;
    L_0x0041:
        r0 = move-exception;
        r0 = 1;
        r12 = r0;
        r0 = r14;
    L_0x0045:
        r14 = java.lang.System.nanoTime();	 Catch:{ all -> 0x0082 }
        r6 = r9 - r14;
        if (r0 == 0) goto L_0x002c;
    L_0x004d:
        r0 = r13;
    L_0x004e:
        r14 = r0;
        r0 = r18.isSatisfied();	 Catch:{ InterruptedException -> 0x0070 }
        if (r0 != 0) goto L_0x005d;
    L_0x0055:
        r0 = r1.awaitNanos(r2, r6, r11);	 Catch:{ InterruptedException -> 0x0070 }
        if (r0 == 0) goto L_0x005c;
    L_0x005b:
        goto L_0x005d;
    L_0x005c:
        goto L_0x005e;
    L_0x005d:
        r13 = 1;
    L_0x005e:
        r0 = r13;
        if (r0 != 0) goto L_0x0064;
    L_0x0061:
        r8.unlock();	 Catch:{ all -> 0x0082 }
    L_0x0064:
        if (r12 == 0) goto L_0x006d;
    L_0x0066:
        r14 = java.lang.Thread.currentThread();
        r14.interrupt();
    L_0x006d:
        return r13;
    L_0x006e:
        r0 = move-exception;
        goto L_0x007c;
    L_0x0070:
        r0 = move-exception;
        r12 = 1;
        r11 = 0;
        r15 = java.lang.System.nanoTime();	 Catch:{ all -> 0x006e }
        r6 = r9 - r15;
        r0 = r14;
        goto L_0x004e;
    L_0x007c:
        if (r14 != 0) goto L_0x0081;
    L_0x007e:
        r8.unlock();	 Catch:{ all -> 0x0082 }
    L_0x0081:
        throw r0;	 Catch:{ all -> 0x0082 }
    L_0x0082:
        r0 = move-exception;
        if (r12 == 0) goto L_0x008c;
    L_0x0085:
        r13 = java.lang.Thread.currentThread();
        r13.interrupt();
    L_0x008c:
        throw r0;
    L_0x008d:
        r0 = new java.lang.IllegalMonitorStateException;
        r0.<init>();
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.google.common.util.concurrent.Monitor.enterWhenUninterruptibly(com.google.common.util.concurrent.Monitor$Guard, long, java.util.concurrent.TimeUnit):boolean");
    }

    public boolean enterIf(Guard guard) {
        if (guard.monitor == this) {
            ReentrantLock lock = this.lock;
            lock.lock();
            boolean satisfied = false;
            try {
                boolean isSatisfied = guard.isSatisfied();
                satisfied = isSatisfied;
                return isSatisfied;
            } finally {
                if (!satisfied) {
                    lock.unlock();
                }
            }
        } else {
            throw new IllegalMonitorStateException();
        }
    }

    public boolean enterIfInterruptibly(Guard guard) throws InterruptedException {
        if (guard.monitor == this) {
            ReentrantLock lock = this.lock;
            lock.lockInterruptibly();
            boolean satisfied = false;
            try {
                boolean isSatisfied = guard.isSatisfied();
                satisfied = isSatisfied;
                return isSatisfied;
            } finally {
                if (!satisfied) {
                    lock.unlock();
                }
            }
        } else {
            throw new IllegalMonitorStateException();
        }
    }

    public boolean enterIf(Guard guard, long time, TimeUnit unit) {
        if (guard.monitor != this) {
            throw new IllegalMonitorStateException();
        } else if (!enter(time, unit)) {
            return false;
        } else {
            boolean satisfied = false;
            try {
                boolean isSatisfied = guard.isSatisfied();
                satisfied = isSatisfied;
                return isSatisfied;
            } finally {
                if (!satisfied) {
                    this.lock.unlock();
                }
            }
        }
    }

    public boolean enterIfInterruptibly(Guard guard, long time, TimeUnit unit) throws InterruptedException {
        if (guard.monitor == this) {
            ReentrantLock lock = this.lock;
            if (!lock.tryLock(time, unit)) {
                return false;
            }
            boolean satisfied = false;
            try {
                boolean isSatisfied = guard.isSatisfied();
                satisfied = isSatisfied;
                return isSatisfied;
            } finally {
                if (!satisfied) {
                    lock.unlock();
                }
            }
        } else {
            throw new IllegalMonitorStateException();
        }
    }

    public boolean tryEnterIf(Guard guard) {
        if (guard.monitor == this) {
            ReentrantLock lock = this.lock;
            if (!lock.tryLock()) {
                return false;
            }
            boolean satisfied = false;
            try {
                boolean isSatisfied = guard.isSatisfied();
                satisfied = isSatisfied;
                return isSatisfied;
            } finally {
                if (!satisfied) {
                    lock.unlock();
                }
            }
        } else {
            throw new IllegalMonitorStateException();
        }
    }

    public void waitFor(Guard guard) throws InterruptedException {
        if (((guard.monitor == this ? 1 : 0) & this.lock.isHeldByCurrentThread()) == 0) {
            throw new IllegalMonitorStateException();
        } else if (!guard.isSatisfied()) {
            await(guard, true);
        }
    }

    public void waitForUninterruptibly(Guard guard) {
        if (((guard.monitor == this ? 1 : 0) & this.lock.isHeldByCurrentThread()) == 0) {
            throw new IllegalMonitorStateException();
        } else if (!guard.isSatisfied()) {
            awaitUninterruptibly(guard, true);
        }
    }

    public boolean waitFor(Guard guard, long time, TimeUnit unit) throws InterruptedException {
        long timeoutNanos = unit.toNanos(time);
        if (((guard.monitor == this ? 1 : 0) & this.lock.isHeldByCurrentThread()) == 0) {
            throw new IllegalMonitorStateException();
        } else if (guard.isSatisfied() || awaitNanos(guard, timeoutNanos, true)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean waitForUninterruptibly(Guard guard, long time, TimeUnit unit) {
        long timeoutNanos = unit.toNanos(time);
        if (((guard.monitor == this ? 1 : 0) & this.lock.isHeldByCurrentThread()) == 0) {
            throw new IllegalMonitorStateException();
        } else if (guard.isSatisfied()) {
            return true;
        } else {
            boolean signalBeforeWaiting = true;
            long deadline = System.nanoTime() + timeoutNanos;
            boolean interrupted = Thread.interrupted();
            while (true) {
                try {
                    boolean awaitNanos = awaitNanos(guard, timeoutNanos, signalBeforeWaiting);
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                    return awaitNanos;
                } catch (InterruptedException e) {
                    interrupted = true;
                    if (guard.isSatisfied()) {
                        if (1 != null) {
                            Thread.currentThread().interrupt();
                        }
                        return true;
                    }
                    signalBeforeWaiting = false;
                    timeoutNanos = deadline - System.nanoTime();
                } catch (Throwable th) {
                    if (1 != null) {
                        Thread.currentThread().interrupt();
                    }
                    throw th;
                }
            }
        }
    }

    public void leave() {
        ReentrantLock lock = this.lock;
        try {
            if (lock.getHoldCount() == 1) {
                signalNextWaiter();
            }
            lock.unlock();
        } catch (Throwable th) {
            lock.unlock();
        }
    }

    public boolean isFair() {
        return this.fair;
    }

    public boolean isOccupied() {
        return this.lock.isLocked();
    }

    public boolean isOccupiedByCurrentThread() {
        return this.lock.isHeldByCurrentThread();
    }

    public int getOccupiedDepth() {
        return this.lock.getHoldCount();
    }

    public int getQueueLength() {
        return this.lock.getQueueLength();
    }

    public boolean hasQueuedThreads() {
        return this.lock.hasQueuedThreads();
    }

    public boolean hasQueuedThread(Thread thread) {
        return this.lock.hasQueuedThread(thread);
    }

    public boolean hasWaiters(Guard guard) {
        return getWaitQueueLength(guard) > 0;
    }

    public int getWaitQueueLength(Guard guard) {
        if (guard.monitor == this) {
            this.lock.lock();
            try {
                int i = guard.waiterCount;
                return i;
            } finally {
                this.lock.unlock();
            }
        } else {
            throw new IllegalMonitorStateException();
        }
    }

    @GuardedBy("lock")
    private void signalNextWaiter() {
        for (Guard guard = this.activeGuards; guard != null; guard = guard.next) {
            if (isSatisfied(guard)) {
                guard.condition.signal();
                return;
            }
        }
    }

    @GuardedBy("lock")
    private boolean isSatisfied(Guard guard) {
        try {
            return guard.isSatisfied();
        } catch (Throwable throwable) {
            signalAllWaiters();
            RuntimeException propagate = Throwables.propagate(throwable);
        }
    }

    @GuardedBy("lock")
    private void signalAllWaiters() {
        for (Guard guard = this.activeGuards; guard != null; guard = guard.next) {
            guard.condition.signalAll();
        }
    }

    @GuardedBy("lock")
    private void beginWaitingFor(Guard guard) {
        int waiters = guard.waiterCount;
        guard.waiterCount = waiters + 1;
        if (waiters == 0) {
            guard.next = this.activeGuards;
            this.activeGuards = guard;
        }
    }

    @GuardedBy("lock")
    private void endWaitingFor(Guard guard) {
        int waiters = guard.waiterCount - 1;
        guard.waiterCount = waiters;
        if (waiters == 0) {
            Guard p = this.activeGuards;
            Guard pred = null;
            while (p != guard) {
                pred = p;
                p = p.next;
            }
            if (pred == null) {
                this.activeGuards = p.next;
            } else {
                pred.next = p.next;
            }
            p.next = null;
        }
    }

    @GuardedBy("lock")
    private void await(Guard guard, boolean signalBeforeWaiting) throws InterruptedException {
        if (signalBeforeWaiting) {
            signalNextWaiter();
        }
        beginWaitingFor(guard);
        while (true) {
            try {
                guard.condition.await();
                if (guard.isSatisfied()) {
                    break;
                }
            } finally {
                endWaitingFor(guard);
            }
        }
    }

    @GuardedBy("lock")
    private void awaitUninterruptibly(Guard guard, boolean signalBeforeWaiting) {
        if (signalBeforeWaiting) {
            signalNextWaiter();
        }
        beginWaitingFor(guard);
        while (true) {
            try {
                guard.condition.awaitUninterruptibly();
                if (guard.isSatisfied()) {
                    break;
                }
            } finally {
                endWaitingFor(guard);
            }
        }
    }

    @GuardedBy("lock")
    private boolean awaitNanos(Guard guard, long nanos, boolean signalBeforeWaiting) throws InterruptedException {
        if (signalBeforeWaiting) {
            signalNextWaiter();
        }
        beginWaitingFor(guard);
        while (nanos >= 0) {
            try {
                nanos = guard.condition.awaitNanos(nanos);
                if (guard.isSatisfied()) {
                    return true;
                }
            } finally {
                endWaitingFor(guard);
            }
        }
        endWaitingFor(guard);
        return false;
    }
}
