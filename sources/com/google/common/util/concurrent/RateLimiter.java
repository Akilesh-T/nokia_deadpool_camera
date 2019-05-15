package com.google.common.util.concurrent;

import com.google.common.annotations.Beta;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.morphoinc.app.panoramagp3.Camera2ParamsFragment;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.ThreadSafe;

@ThreadSafe
@Beta
public abstract class RateLimiter {
    private volatile Object mutexDoNotUseDirectly;
    private final SleepingStopwatch stopwatch;

    @VisibleForTesting
    static abstract class SleepingStopwatch {
        public abstract long readMicros();

        public abstract void sleepMicrosUninterruptibly(long j);

        SleepingStopwatch() {
        }

        static final SleepingStopwatch createFromSystemTimer() {
            return new SleepingStopwatch() {
                final Stopwatch stopwatch = Stopwatch.createStarted();

                /* Access modifiers changed, original: 0000 */
                public long readMicros() {
                    return this.stopwatch.elapsed(TimeUnit.MICROSECONDS);
                }

                /* Access modifiers changed, original: 0000 */
                public void sleepMicrosUninterruptibly(long micros) {
                    if (micros > 0) {
                        Uninterruptibles.sleepUninterruptibly(micros, TimeUnit.MICROSECONDS);
                    }
                }
            };
        }
    }

    public abstract double doGetRate();

    public abstract void doSetRate(double d, long j);

    public abstract long queryEarliestAvailable(long j);

    public abstract long reserveEarliestAvailable(int i, long j);

    public static RateLimiter create(double permitsPerSecond) {
        return create(SleepingStopwatch.createFromSystemTimer(), permitsPerSecond);
    }

    @VisibleForTesting
    static RateLimiter create(SleepingStopwatch stopwatch, double permitsPerSecond) {
        RateLimiter rateLimiter = new SmoothBursty(stopwatch, 1.0d);
        rateLimiter.setRate(permitsPerSecond);
        return rateLimiter;
    }

    public static RateLimiter create(double permitsPerSecond, long warmupPeriod, TimeUnit unit) {
        Preconditions.checkArgument(warmupPeriod >= 0, "warmupPeriod must not be negative: %s", Long.valueOf(warmupPeriod));
        return create(SleepingStopwatch.createFromSystemTimer(), permitsPerSecond, warmupPeriod, unit);
    }

    @VisibleForTesting
    static RateLimiter create(SleepingStopwatch stopwatch, double permitsPerSecond, long warmupPeriod, TimeUnit unit) {
        RateLimiter rateLimiter = new SmoothWarmingUp(stopwatch, warmupPeriod, unit);
        rateLimiter.setRate(permitsPerSecond);
        return rateLimiter;
    }

    private Object mutex() {
        Object mutex = this.mutexDoNotUseDirectly;
        if (mutex == null) {
            synchronized (this) {
                mutex = this.mutexDoNotUseDirectly;
                if (mutex == null) {
                    Object obj = new Object();
                    mutex = obj;
                    this.mutexDoNotUseDirectly = obj;
                }
            }
        }
        return mutex;
    }

    RateLimiter(SleepingStopwatch stopwatch) {
        this.stopwatch = (SleepingStopwatch) Preconditions.checkNotNull(stopwatch);
    }

    public final void setRate(double permitsPerSecond) {
        boolean z = permitsPerSecond > Camera2ParamsFragment.TARGET_EV && !Double.isNaN(permitsPerSecond);
        Preconditions.checkArgument(z, "rate must be positive");
        synchronized (mutex()) {
            doSetRate(permitsPerSecond, this.stopwatch.readMicros());
        }
    }

    public final double getRate() {
        double doGetRate;
        synchronized (mutex()) {
            doGetRate = doGetRate();
        }
        return doGetRate;
    }

    public double acquire() {
        return acquire(1);
    }

    public double acquire(int permits) {
        long microsToWait = reserve(permits);
        this.stopwatch.sleepMicrosUninterruptibly(microsToWait);
        return (1.0d * ((double) microsToWait)) / ((double) TimeUnit.SECONDS.toMicros(1));
    }

    /* Access modifiers changed, original: final */
    public final long reserve(int permits) {
        long reserveAndGetWaitLength;
        checkPermits(permits);
        synchronized (mutex()) {
            reserveAndGetWaitLength = reserveAndGetWaitLength(permits, this.stopwatch.readMicros());
        }
        return reserveAndGetWaitLength;
    }

    public boolean tryAcquire(long timeout, TimeUnit unit) {
        return tryAcquire(1, timeout, unit);
    }

    public boolean tryAcquire(int permits) {
        return tryAcquire(permits, 0, TimeUnit.MICROSECONDS);
    }

    public boolean tryAcquire() {
        return tryAcquire(1, 0, TimeUnit.MICROSECONDS);
    }

    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) {
        long timeoutMicros = Math.max(unit.toMicros(timeout), 0);
        checkPermits(permits);
        synchronized (mutex()) {
            long nowMicros = this.stopwatch.readMicros();
            if (canAcquire(nowMicros, timeoutMicros)) {
                nowMicros = reserveAndGetWaitLength(permits, nowMicros);
                this.stopwatch.sleepMicrosUninterruptibly(nowMicros);
                return true;
            }
            return false;
        }
    }

    private boolean canAcquire(long nowMicros, long timeoutMicros) {
        return queryEarliestAvailable(nowMicros) - timeoutMicros <= nowMicros;
    }

    /* Access modifiers changed, original: final */
    public final long reserveAndGetWaitLength(int permits, long nowMicros) {
        return Math.max(reserveEarliestAvailable(permits, nowMicros) - nowMicros, 0);
    }

    public String toString() {
        return String.format("RateLimiter[stableRate=%3.1fqps]", new Object[]{Double.valueOf(getRate())});
    }

    private static int checkPermits(int permits) {
        Preconditions.checkArgument(permits > 0, "Requested permits (%s) must be positive", Integer.valueOf(permits));
        return permits;
    }
}
