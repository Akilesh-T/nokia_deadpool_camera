package com.hmdglobal.app.camera.util;

import android.os.Build;
import android.os.Build.VERSION;

public class ApiHelper {
    public static final boolean AT_LEAST_16 = (VERSION.SDK_INT >= 16);
    public static final boolean HAS_ANNOUNCE_FOR_ACCESSIBILITY = (VERSION.SDK_INT >= 16);
    public static final boolean HAS_APP_GALLERY = (VERSION.SDK_INT >= 15);
    public static final boolean HAS_AUTO_FOCUS_MOVE_CALLBACK = (VERSION.SDK_INT >= 16);
    public static final boolean HAS_CAMERA_2_API = isLOrHigher();
    public static final boolean HAS_CAMERA_HDR = (VERSION.SDK_INT >= 17);
    public static final boolean HAS_CAMERA_HDR_PLUS = isKitKatOrHigher();
    public static final boolean HAS_DISPLAY_LISTENER = (VERSION.SDK_INT >= 17);
    public static final boolean HAS_HIDEYBARS = isKitKatOrHigher();
    public static final boolean HAS_MEDIA_ACTION_SOUND = (VERSION.SDK_INT >= 16);
    public static final boolean HAS_MEDIA_COLUMNS_WIDTH_AND_HEIGHT = (VERSION.SDK_INT >= 16);
    public static final boolean HAS_ORIENTATION_LOCK = (VERSION.SDK_INT >= 18);
    public static final boolean HAS_ROBOTO_MEDIUM_FONT = (VERSION.SDK_INT >= 16);
    public static final boolean HAS_ROBOTO_REGULAR_FONT = (VERSION.SDK_INT >= 16);
    public static final boolean HAS_ROTATION_ANIMATION = (VERSION.SDK_INT >= 18);
    public static final boolean HAS_SET_BEAM_PUSH_URIS = (VERSION.SDK_INT >= 16);
    public static final boolean HAS_SURFACE_TEXTURE_RECORDING = (VERSION.SDK_INT >= 16);
    public static final boolean HDR_PLUS_CAN_USE_ARBITRARY_ASPECT_RATIOS = isKitKatMR2OrHigher();
    public static final boolean IS_NEXUS_4 = "mako".equalsIgnoreCase(Build.DEVICE);
    public static final boolean IS_NEXUS_5;
    public static final boolean IS_NEXUS_6;

    static {
        boolean z = false;
        boolean z2 = "LGE".equalsIgnoreCase(Build.MANUFACTURER) && "hammerhead".equalsIgnoreCase(Build.DEVICE);
        IS_NEXUS_5 = z2;
        if ("motorola".equalsIgnoreCase(Build.MANUFACTURER) && "shamu".equalsIgnoreCase(Build.DEVICE)) {
            z = true;
        }
        IS_NEXUS_6 = z;
    }

    public static int getIntFieldIfExists(Class<?> klass, String fieldName, Class<?> obj, int defaultVal) {
        try {
            return klass.getDeclaredField(fieldName).getInt(obj);
        } catch (Exception e) {
            return defaultVal;
        }
    }

    public static boolean isKitKatOrHigher() {
        return VERSION.SDK_INT >= 19 || "KeyLimePie".equals(VERSION.CODENAME);
    }

    public static boolean isKitKatMR2OrHigher() {
        return isLOrHigher() || (isKitKatOrHigher() && ("4.4.4".equals(VERSION.RELEASE) || "4.4.3".equals(VERSION.RELEASE)));
    }

    public static boolean isLOrHigher() {
        return VERSION.SDK_INT >= 21 || "L".equals(VERSION.CODENAME);
    }

    public static boolean isMOrHigher() {
        return VERSION.SDK_INT >= 23 || "M".equals(VERSION.CODENAME);
    }
}
