package com.android.ex.camera2.utils;

import android.util.Log;

public final class SysTrace {
    private static final String TAG = "SysTrace";
    private static final boolean VERBOSE = Log.isLoggable(TAG, 2);
    private static int sNestingLevel = 0;

    public static void traceCounter(String counterName, int counterValue) {
        if (VERBOSE) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("traceCounter ");
            stringBuilder.append(counterName);
            stringBuilder.append(" ");
            stringBuilder.append(counterValue);
            Log.v(str, stringBuilder.toString());
        }
    }

    public static void beginSection(String sectionName) {
        if (VERBOSE) {
            Log.v(TAG, String.format("beginSection[%d] %s", new Object[]{Integer.valueOf(sNestingLevel), sectionName}));
            sNestingLevel++;
        }
    }

    public static void endSection() {
        if (VERBOSE) {
            sNestingLevel--;
            Log.v(TAG, String.format("endSection[%d]", new Object[]{Integer.valueOf(sNestingLevel)}));
        }
    }

    public static void beginSectionAsync(String methodName, int cookie) {
        if (VERBOSE) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("beginSectionAsync ");
            stringBuilder.append(methodName);
            stringBuilder.append(" ");
            stringBuilder.append(cookie);
            Log.v(str, stringBuilder.toString());
        }
    }

    public static void endSectionAsync(String methodName, int cookie) {
        if (VERBOSE) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("endSectionAsync ");
            stringBuilder.append(methodName);
            stringBuilder.append(" ");
            stringBuilder.append(cookie);
            Log.v(str, stringBuilder.toString());
        }
    }
}
