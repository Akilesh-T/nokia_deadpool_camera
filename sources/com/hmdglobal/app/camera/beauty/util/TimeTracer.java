package com.hmdglobal.app.camera.beauty.util;

import com.morphoinc.app.panoramagp3.ButtonsFragment;

public class TimeTracer {
    private String mName;
    private long mStartTime;

    public void startTrace(String name) {
        if (this.mName != null) {
            stopTrace();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("TimeTracer_");
        stringBuilder.append(this.mName);
        MLog.e(stringBuilder.toString(), ButtonsFragment.ACTION_START, null);
        this.mName = name;
        this.mStartTime = System.currentTimeMillis();
    }

    public void stopTrace() {
        long time = System.currentTimeMillis() - this.mStartTime;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("TimeTracer_");
        stringBuilder.append(this.mName);
        String stringBuilder2 = stringBuilder.toString();
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("end time is ");
        stringBuilder3.append(time);
        MLog.e(stringBuilder2, stringBuilder3.toString(), null);
        this.mName = null;
    }
}
