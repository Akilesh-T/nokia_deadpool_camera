package com.hmdglobal.app.camera.util;

import android.os.SystemClock;
import android.util.Log;

public class Timer {
    public static final String TAG = "BeautifyProfiler";
    private String name;
    long time;

    /* Access modifiers changed, original: 0000 */
    public void start(String name) {
        this.name = name;
        this.time = SystemClock.elapsedRealtime();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(name);
        stringBuilder.append(": start");
        Log.e(str, stringBuilder.toString());
    }

    /* Access modifiers changed, original: 0000 */
    public void end() {
        this.time = SystemClock.elapsedRealtime() - this.time;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.name);
        stringBuilder.append(": end ");
        stringBuilder.append(this.time);
        stringBuilder.append(" ms");
        Log.e(str, stringBuilder.toString());
    }
}
