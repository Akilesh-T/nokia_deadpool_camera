package com.hmdglobal.app.camera.util;

import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import java.lang.reflect.Method;

public class BoostUtil {
    private static final Tag TAG = new Tag("BoostUtil");
    Class Performance;
    Object mPerf;
    Method perfLockAcquire;
    Method perfLockRelease;

    private static class BoostUtilHolder {
        private static final BoostUtil mBoostUtilHolder = new BoostUtil();

        private BoostUtilHolder() {
        }
    }

    public static BoostUtil getInstance() {
        return BoostUtilHolder.mBoostUtilHolder;
    }

    public void acquireCpuLock() {
        try {
            this.Performance = Class.forName("android.util.BoostFramework");
            this.perfLockAcquire = this.Performance.getDeclaredMethod("perfLockAcquire", new Class[]{Integer.TYPE, int[].class});
            this.perfLockRelease = this.Performance.getDeclaredMethod("perfLockRelease", new Class[0]);
            if (this.mPerf == null) {
                this.mPerf = this.Performance.getConstructor(new Class[0]).newInstance(new Object[0]);
            }
            if (this.mPerf != null) {
                this.perfLockAcquire.invoke(this.mPerf, new Object[]{Integer.valueOf(4000), new int[]{527, 7951}});
            }
        } catch (Exception e) {
            Log.v(TAG, e.toString());
        }
    }

    public void releaseCpuLock() {
        try {
            if (this.mPerf != null) {
                this.perfLockRelease.invoke(this.mPerf, new Object[0]);
                this.mPerf = null;
            }
        } catch (Exception e) {
            Log.v(TAG, e.toString());
        }
    }
}
