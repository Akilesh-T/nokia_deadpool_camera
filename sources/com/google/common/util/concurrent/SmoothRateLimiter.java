package com.google.common.util.concurrent;

import com.morphoinc.app.panoramagp3.Camera2ParamsFragment;
import java.util.concurrent.TimeUnit;

abstract class SmoothRateLimiter extends RateLimiter {
    double maxPermits;
    private long nextFreeTicketMicros;
    double stableIntervalMicros;
    double storedPermits;

    static final class SmoothBursty extends SmoothRateLimiter {
        final double maxBurstSeconds;

        SmoothBursty(SleepingStopwatch stopwatch, double maxBurstSeconds) {
            super(stopwatch);
            this.maxBurstSeconds = maxBurstSeconds;
        }

        /* Access modifiers changed, original: 0000 */
        public void doSetRate(double permitsPerSecond, double stableIntervalMicros) {
            double oldMaxPermits = this.maxPermits;
            this.maxPermits = this.maxBurstSeconds * permitsPerSecond;
            if (oldMaxPermits == Double.POSITIVE_INFINITY) {
                this.storedPermits = this.maxPermits;
                return;
            }
            double d = Camera2ParamsFragment.TARGET_EV;
            if (oldMaxPermits != Camera2ParamsFragment.TARGET_EV) {
                d = (this.storedPermits * this.maxPermits) / oldMaxPermits;
            }
            this.storedPermits = d;
        }

        /* Access modifiers changed, original: 0000 */
        public long storedPermitsToWaitTime(double storedPermits, double permitsToTake) {
            return 0;
        }
    }

    static final class SmoothWarmingUp extends SmoothRateLimiter {
        private double halfPermits;
        private double slope;
        private final long warmupPeriodMicros;

        SmoothWarmingUp(SleepingStopwatch stopwatch, long warmupPeriod, TimeUnit timeUnit) {
            super(stopwatch);
            this.warmupPeriodMicros = timeUnit.toMicros(warmupPeriod);
        }

        /* Access modifiers changed, original: 0000 */
        public void doSetRate(double permitsPerSecond, double stableIntervalMicros) {
            double oldMaxPermits = this.maxPermits;
            this.maxPermits = ((double) this.warmupPeriodMicros) / stableIntervalMicros;
            this.halfPermits = this.maxPermits / 2.0d;
            this.slope = ((3.0d * stableIntervalMicros) - stableIntervalMicros) / this.halfPermits;
            if (oldMaxPermits == Double.POSITIVE_INFINITY) {
                this.storedPermits = Camera2ParamsFragment.TARGET_EV;
                return;
            }
            double d;
            if (oldMaxPermits == Camera2ParamsFragment.TARGET_EV) {
                d = this.maxPermits;
            } else {
                d = (this.storedPermits * this.maxPermits) / oldMaxPermits;
            }
            this.storedPermits = d;
        }

        /* Access modifiers changed, original: 0000 */
        public long storedPermitsToWaitTime(double storedPermits, double permitsToTake) {
            double availablePermitsAboveHalf = storedPermits - this.halfPermits;
            long micros = 0;
            if (availablePermitsAboveHalf > Camera2ParamsFragment.TARGET_EV) {
                double permitsAboveHalfToTake = Math.min(availablePermitsAboveHalf, permitsToTake);
                micros = (long) (((permitsToTime(availablePermitsAboveHalf) + permitsToTime(availablePermitsAboveHalf - permitsAboveHalfToTake)) * permitsAboveHalfToTake) / 2.0d);
                permitsToTake -= permitsAboveHalfToTake;
            }
            return (long) (((double) micros) + (this.stableIntervalMicros * permitsToTake));
        }

        private double permitsToTime(double permits) {
            return this.stableIntervalMicros + (this.slope * permits);
        }
    }

    public abstract void doSetRate(double d, double d2);

    public abstract long storedPermitsToWaitTime(double d, double d2);

    private SmoothRateLimiter(SleepingStopwatch stopwatch) {
        super(stopwatch);
        this.nextFreeTicketMicros = 0;
    }

    /* Access modifiers changed, original: final */
    public final void doSetRate(double permitsPerSecond, long nowMicros) {
        resync(nowMicros);
        double stableIntervalMicros = ((double) TimeUnit.SECONDS.toMicros(1)) / permitsPerSecond;
        this.stableIntervalMicros = stableIntervalMicros;
        doSetRate(permitsPerSecond, stableIntervalMicros);
    }

    /* Access modifiers changed, original: final */
    public final double doGetRate() {
        return ((double) TimeUnit.SECONDS.toMicros(1)) / this.stableIntervalMicros;
    }

    /* Access modifiers changed, original: final */
    public final long queryEarliestAvailable(long nowMicros) {
        return this.nextFreeTicketMicros;
    }

    /* Access modifiers changed, original: final */
    public final long reserveEarliestAvailable(int requiredPermits, long nowMicros) {
        resync(nowMicros);
        long returnValue = this.nextFreeTicketMicros;
        double storedPermitsToSpend = Math.min((double) requiredPermits, this.storedPermits);
        this.nextFreeTicketMicros += storedPermitsToWaitTime(this.storedPermits, storedPermitsToSpend) + ((long) (this.stableIntervalMicros * (((double) requiredPermits) - storedPermitsToSpend)));
        this.storedPermits -= storedPermitsToSpend;
        return returnValue;
    }

    private void resync(long nowMicros) {
        if (nowMicros > this.nextFreeTicketMicros) {
            this.storedPermits = Math.min(this.maxPermits, this.storedPermits + (((double) (nowMicros - this.nextFreeTicketMicros)) / this.stableIntervalMicros));
            this.nextFreeTicketMicros = nowMicros;
        }
    }
}
