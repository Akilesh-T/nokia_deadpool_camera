package android.util;

public class CameraPerformanceTracker {
    public static final int ACTIVITY_PAUSE = 1;
    public static final int ACTIVITY_RESUME = 2;
    public static final int ACTIVITY_START = 0;
    private static final boolean DEBUG = false;
    public static final int FIRST_PREVIEW_FRAME = 5;
    public static final int MODE_SWITCH_START = 3;
    private static final String TAG = "CameraPerformanceTracker";
    public static final int UNSET = -1;
    private static CameraPerformanceTracker sInstance;
    private long mAppResumeTime = -1;
    private long mAppStartTime = -1;
    private long mFirstPreviewFrameLatencyColdStart = -1;
    private long mFirstPreviewFrameLatencyWarmStart = -1;
    private long mModeSwitchDuration = -1;
    private long mModeSwitchStartTime = -1;

    private CameraPerformanceTracker() {
    }

    public static void onEvent(int eventType) {
        if (sInstance == null) {
            sInstance = new CameraPerformanceTracker();
        }
        long currentTime = System.currentTimeMillis();
        if (eventType != 5) {
            switch (eventType) {
                case 0:
                    sInstance.mAppStartTime = currentTime;
                    return;
                case 1:
                    sInstance.mFirstPreviewFrameLatencyWarmStart = -1;
                    return;
                case 2:
                    sInstance.mAppResumeTime = currentTime;
                    return;
                case 3:
                    sInstance.mModeSwitchStartTime = currentTime;
                    return;
                default:
                    return;
            }
        }
        Log.d(TAG, "First preview frame received");
        if (sInstance.mFirstPreviewFrameLatencyColdStart == -1) {
            sInstance.mFirstPreviewFrameLatencyColdStart = currentTime - sInstance.mAppStartTime;
        } else {
            sInstance.mFirstPreviewFrameLatencyWarmStart = currentTime - sInstance.mAppResumeTime;
        }
        if (sInstance.mModeSwitchStartTime != -1) {
            sInstance.mModeSwitchDuration = currentTime - sInstance.mModeSwitchStartTime;
            sInstance.mModeSwitchStartTime = -1;
        }
    }

    public static long getColdStartLatency() {
        if (sInstance == null) {
            return -1;
        }
        return sInstance.mFirstPreviewFrameLatencyColdStart;
    }

    public static long getWarmStartLatency() {
        if (sInstance == null) {
            return -1;
        }
        return sInstance.mFirstPreviewFrameLatencyWarmStart;
    }

    public static long getModeSwitchDuration() {
        if (sInstance == null) {
            return -1;
        }
        return sInstance.mModeSwitchDuration;
    }
}
