package com.hmdglobal.app.camera.app;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.os.Debug;
import android.os.Process;
import android.os.SystemClock;
import android.support.v4.media.session.PlaybackStateCompat;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import java.util.HashMap;

public class MemoryQuery {
    public static final String KEY_DALVIK_PSS = "dalvikPSS";
    public static final String KEY_LARGE_MEMORY_CLASS = "largeMemoryClass";
    public static final String KEY_LAST_TRIM_LEVEL = "lastTrimLevel";
    public static final String KEY_LOW_MEMORY = "lowMemory";
    public static final String KEY_MEMORY_AVAILABLE = "availMem";
    public static final String KEY_MEMORY_CLASS = "memoryClass";
    public static final String KEY_NATIVE_PSS = "nativePSS";
    public static final String KEY_OTHER_PSS = "otherPSS";
    public static final String KEY_THRESHOLD = "threshold";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_TOTAL_MEMORY = "totalMem";
    public static final String KEY_TOTAL_PRIVATE_DIRTY = "totalPrivateDirty";
    public static final String KEY_TOTAL_PSS = "totalPSS";
    public static final String KEY_TOTAL_SHARED_DIRTY = "totalSharedDirty";
    public static final String REPORT_LABEL_LAUNCH = "launch";
    private static final Tag TAG = new Tag("MemoryQuery");
    private final long BYTES_IN_KILOBYTE = PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
    private final long BYTES_IN_MEGABYTE = PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED;
    private ActivityManager mActivityManager;

    public MemoryQuery(ActivityManager activityManager) {
        this.mActivityManager = activityManager;
    }

    public HashMap queryMemory() {
        long threshold;
        int memoryClass;
        int largeMemoryClass;
        long totalSharedDirty;
        long nativePSS;
        long dalvikPSS;
        long otherPSS;
        long totalPrivateDirty;
        int memoryClass2 = this.mActivityManager.getMemoryClass();
        int largeMemoryClass2 = this.mActivityManager.getLargeMemoryClass();
        MemoryInfo memoryInfo = new MemoryInfo();
        this.mActivityManager.getMemoryInfo(memoryInfo);
        long availMem = memoryInfo.availMem / PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED;
        long totalMem = memoryInfo.totalMem / PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED;
        long threshold2 = memoryInfo.threshold / PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED;
        boolean lowMemory = memoryInfo.lowMemory;
        RunningAppProcessInfo info = new RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(info);
        int appPID = Process.myPid();
        long timestamp = SystemClock.elapsedRealtime();
        if (appPID != 0) {
            long totalPrivateDirty2 = 0;
            Debug.MemoryInfo[] memoryInfoArray = this.mActivityManager.getProcessMemoryInfo(new int[]{appPID});
            threshold = threshold2;
            long totalPrivateDirty3 = ((long) memoryInfoArray[0].getTotalPrivateDirty()) / PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
            memoryClass = memoryClass2;
            largeMemoryClass = largeMemoryClass2;
            totalSharedDirty = ((long) memoryInfoArray[0].getTotalSharedDirty()) / PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
            threshold2 = ((long) memoryInfoArray[0].getTotalPss()) / PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
            nativePSS = ((long) memoryInfoArray[0].nativePss) / PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
            dalvikPSS = ((long) memoryInfoArray[0].dalvikPss) / PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
            otherPSS = ((long) memoryInfoArray[0].otherPss) / PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID;
            totalPrivateDirty = totalPrivateDirty3;
        } else {
            threshold = threshold2;
            memoryClass = memoryClass2;
            largeMemoryClass = largeMemoryClass2;
            totalSharedDirty = 0;
            threshold2 = 0;
            nativePSS = 0;
            dalvikPSS = 0;
            otherPSS = 0;
            totalPrivateDirty = 0;
        }
        HashMap outputData = new HashMap();
        boolean lowMemory2 = lowMemory;
        outputData.put(KEY_TIMESTAMP, new Long(timestamp));
        outputData.put(KEY_MEMORY_AVAILABLE, new Long(availMem));
        outputData.put(KEY_TOTAL_MEMORY, new Long(totalMem));
        outputData.put(KEY_TOTAL_PSS, new Long(threshold2));
        long totalPSS = threshold2;
        outputData.put(KEY_LAST_TRIM_LEVEL, new Integer(info.lastTrimLevel));
        outputData.put(KEY_TOTAL_PRIVATE_DIRTY, new Long(totalPrivateDirty));
        outputData.put(KEY_TOTAL_SHARED_DIRTY, new Long(totalSharedDirty));
        int memoryClass3 = memoryClass;
        outputData.put(KEY_MEMORY_CLASS, new Long((long) memoryClass3));
        int largeMemoryClass3 = largeMemoryClass;
        outputData.put(KEY_LARGE_MEMORY_CLASS, new Long((long) largeMemoryClass3));
        totalPrivateDirty = nativePSS;
        outputData.put(KEY_NATIVE_PSS, new Long(totalPrivateDirty));
        threshold2 = dalvikPSS;
        outputData.put(KEY_DALVIK_PSS, new Long(threshold2));
        long dalvikPSS2 = threshold2;
        threshold2 = otherPSS;
        outputData.put(KEY_OTHER_PSS, new Long(threshold2));
        long otherPSS2 = threshold2;
        threshold2 = threshold;
        outputData.put(KEY_THRESHOLD, new Long(threshold2));
        boolean lowMemory3 = lowMemory2;
        outputData.put(KEY_LOW_MEMORY, new Boolean(lowMemory3));
        Tag tag = TAG;
        HashMap outputData2 = outputData;
        Object[] objArr = new Object[11];
        objArr[0] = Long.valueOf(timestamp);
        objArr[1] = Long.valueOf(availMem);
        objArr[2] = Long.valueOf(totalMem);
        availMem = totalPSS;
        objArr[3] = Long.valueOf(availMem);
        objArr[4] = Integer.valueOf(info.lastTrimLevel);
        objArr[5] = Integer.valueOf(largeMemoryClass3);
        objArr[6] = Long.valueOf(totalPrivateDirty);
        totalPrivateDirty = dalvikPSS2;
        objArr[7] = Long.valueOf(totalPrivateDirty);
        objArr[8] = Long.valueOf(otherPSS2);
        objArr[9] = Long.valueOf(threshold2);
        objArr[10] = Boolean.valueOf(lowMemory3);
        Log.d(tag, String.format("timestamp=%d, availMem=%d, totalMem=%d, totalPSS=%d, lastTrimLevel=%d, largeMemoryClass=%d, nativePSS=%d, dalvikPSS=%d, otherPSS=%d,threshold=%d, lowMemory=%s", objArr));
        return outputData2;
    }
}
