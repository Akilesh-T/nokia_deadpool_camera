package com.morphoinc.app.panoramagp3;

import com.morphoinc.utils.os.BuildUtil;

public class CameraConstants {
    public static final int AutoFocusType = (BuildUtil.isSony() ? 2 : 1);
    public static final int BURST_SHOT_NUM = 600;
    public static final int CAPTURE_MODE_BURST = 3;
    public static final int CAPTURE_MODE_PREVIEW = 0;
    public static final int CAPTURE_MODE_REPEATING_BURST = 4;
    public static final int CAPTURE_MODE_STILL = 1;
    public static final int CAPTURE_MODE_ZERO_SHUTTER_LAG = 2;
    public static final Object CameraSynchronizedObject = new Object();
    public static final Object EngineSynchronizedObject = new Object();
    public static final int MAX_ISO_VALUE = 1600;
    public static final int REPEATING_BURST_SHOT_NUM;
    public static final boolean SHOW_UI_CONTROL_MODE = true;
    public static final long TV_EXPOSURE_TIME_DIFF = 500000;
    public static final long TV_EXPOSURE_TIME_MAX = 16666666;
    public static final int TV_SENSOR_SENSITIVITY_DIFF = 5;

    static {
        int i = 1;
        if (BuildUtil.isLGE()) {
            i = 2;
        }
        REPEATING_BURST_SHOT_NUM = i;
    }
}
