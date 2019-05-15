package com.morphoinc.app.panoramagp3;

import com.morphoinc.app.LogFilter;
import java.util.Locale;

public class PerformanceCounter {
    private long mTime;

    private static class PerformanceCounterOff extends PerformanceCounter {
        private PerformanceCounterOff() {
            super();
        }

        public void start() {
        }

        public void stop() {
        }

        public long get() {
            return 0;
        }

        public void putLog(String tag, String identifier) {
        }
    }

    private PerformanceCounter() {
    }

    private long getCurTime() {
        return System.nanoTime();
    }

    public void start() {
        this.mTime = getCurTime();
    }

    public void stop() {
        this.mTime = getCurTime() - this.mTime;
    }

    public long get() {
        return this.mTime;
    }

    public void putLog(String tag, String identifier) {
        LogFilter.i(tag, String.format(Locale.US, "PRINT_PROCESSING_TIME :%s : %2$,3d nsec", new Object[]{identifier, Long.valueOf(get())}));
    }

    public static PerformanceCounter newInstance(boolean enabled) {
        return enabled ? new PerformanceCounter() : new PerformanceCounterOff();
    }
}
