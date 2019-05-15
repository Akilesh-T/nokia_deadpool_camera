package com.hmdglobal.app.camera.beauty.util;

import android.content.Context;
import com.megvii.beautify.jni.BeaurifyJniSdk;

public class Util {
    public static final int BEAUTIFY_TRANS_COEFFICIENT = 20;
    public static float CURRENT_MG_BEAUTIFY_ADD_PINK = 0.0f;
    public static float CURRENT_MG_BEAUTIFY_BRIGHTEN_EYE = 0.0f;
    public static float CURRENT_MG_BEAUTIFY_BRIGHTNESS = 0.0f;
    public static float CURRENT_MG_BEAUTIFY_DENOISE = 0.0f;
    public static float CURRENT_MG_BEAUTIFY_ENLARGE_EYE = 0.0f;
    public static float CURRENT_MG_BEAUTIFY_SHRINK_FACE = 0.0f;
    public static float CURRENT_MG_BEAUTIFY_THIN_FACE = 0.0f;
    public static float CURRENT_MG_BEAUTIFY_TOOTH = 0.0f;
    public static final float DEFAULT_BEAUTIRY_VALUE = 0.0f;
    public static final String[] KEYS = new String[]{"KEY_STICKER", "KEY_FILTER"};
    public static final int MG_FPP_DENSEDETECTIONMODE_FULL_SIZE = 1;
    public static final int MG_FPP_DENSEDETECTIONMODE_PREVIEW = 0;
    public static final int MG_FPP_DETECTIONMODE = 3;
    public static final int MG_FPP_DETECTIONMODE_NORMAL = 0;
    public static final int MG_FPP_DETECTIONMODE_TRACKING = 1;
    public static final int MG_FPP_DETECTIONMODE_TRACKING_FAST = 3;
    public static final int MG_FPP_DETECTIONMODE_TRACKING_ROBUST = 4;
    public static final int MG_FPP_DETECTIONMODE_TRACKING_SMOOTH = 2;
    public static final int MG_IMAGEMODE_BGR = 1;
    public static final int MG_IMAGEMODE_COUNT = 5;
    public static final int MG_IMAGEMODE_GRAY = 0;
    public static final int MG_IMAGEMODE_NV21 = 2;
    public static final int MG_IMAGEMODE_RGB = 4;
    public static final int MG_IMAGEMODE_RGBA = 3;
    public static final int TYPE_FILTER = 1;
    public static final int TYPE_STICKER = 0;
    public static int TimesofLivebokeh = 0;
    static byte[] aligned;
    static BeaurifyJniSdk beaurifyImageJniSdk;
    public static volatile String filterPath;
    public static boolean isDebuging = false;
    public static volatile boolean isDebugingLandMark = false;
    public static volatile boolean isFiltered;
    public static boolean isLocationOn = true;
    public static boolean isTestHAL = false;
    static byte[] outAligned;
    public static String sCurrentStickerPath;
    public static boolean switchcamera = false;
    public static int switchcount = 0;

    public static int dp2px(Context context, float dipValue) {
        return (int) ((dipValue * context.getResources().getDisplayMetrics().density) + 0.5f);
    }
}
