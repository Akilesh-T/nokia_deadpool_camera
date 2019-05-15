package com.android.external;

import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.media.CamcorderProfile;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CamcorderProfileEx {
    private static final int QUALITY_IME_LAPSE_LIST_END = (QUALITY_TIME_LAPSE_LIST_START + 18);
    public static final int QUALITY_LIST_END = 18;
    private static final int QUALITY_LIST_START = getQualityNum("QUALITY_LIST_START");
    private static final int QUALITY_TIME_LAPSE_LIST_START = getQualityNum("QUALITY_TIME_LAPSE_LIST_START");
    private static final String TAG = "CamcorderProfileEx";

    public static int getQualityNum(String qualityName) {
        try {
            Field f = CamcorderProfile.class.getDeclaredField(qualityName);
            f.setAccessible(true);
            return f.getInt(null);
        } catch (SecurityException e) {
            Log.e(TAG, "getQualityNum error");
            return 0;
        } catch (NoSuchFieldException e2) {
            Log.e(TAG, "getQualityNum error");
            return 0;
        } catch (IllegalArgumentException e3) {
            Log.e(TAG, "getQualityNum error");
            return 0;
        } catch (IllegalAccessException e4) {
            Log.e(TAG, "getQualityNum error");
            return 0;
        }
    }

    public static CamcorderProfile getProfile(int quality) {
        int numberOfCameras = Camera.getNumberOfCameras();
        CameraInfo cameraInfo = new CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == 0) {
                return getProfile(i, quality);
            }
        }
        return null;
    }

    public static CamcorderProfile getProfile(int cameraId, int quality) {
        quality = getQuality(cameraId, quality);
        if ((quality >= QUALITY_LIST_START && quality <= 18) || (quality >= QUALITY_TIME_LAPSE_LIST_START && quality <= QUALITY_IME_LAPSE_LIST_END)) {
            return native_get_camcorder_profile(cameraId, quality);
        }
        String errMessage = new StringBuilder();
        errMessage.append("Unsupported quality level: ");
        errMessage.append(quality);
        throw new IllegalArgumentException(errMessage.toString());
    }

    public static int getQuality(int cameraId, int quality) {
        boolean m = false;
        if (getQualityNum("QUALITY_HMD_LIST_END") != 0) {
            m = true;
        }
        if (m && cameraId == 0) {
            if (quality == 6 || quality == 5) {
                quality += 5;
            } else if (quality == 4) {
                quality = 8;
            }
        } else if (m && cameraId == 1 && quality == 3) {
            quality = 8;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getQuality cameraId=");
        stringBuilder.append(cameraId);
        stringBuilder.append(" quality=");
        stringBuilder.append(quality);
        Log.d(str, stringBuilder.toString());
        return quality;
    }

    private static final CamcorderProfile native_get_camcorder_profile(int cameraId, int quality) {
        try {
            Method m = CamcorderProfile.class.getDeclaredMethod("native_get_camcorder_profile", new Class[]{Integer.TYPE, Integer.TYPE});
            m.setAccessible(true);
            return (CamcorderProfile) m.invoke(null, new Object[]{Integer.valueOf(cameraId), Integer.valueOf(quality)});
        } catch (SecurityException e) {
            Log.e(TAG, "native_get_camcorder_profile error");
            return null;
        } catch (NoSuchMethodException e2) {
            Log.e(TAG, "native_get_camcorder_profile error");
            return null;
        } catch (IllegalArgumentException e3) {
            Log.e(TAG, "native_get_camcorder_profile error");
            return null;
        } catch (IllegalAccessException e4) {
            Log.e(TAG, "native_get_camcorder_profile error");
            return null;
        } catch (InvocationTargetException e5) {
            Log.e(TAG, "native_get_camcorder_profile error");
            return null;
        }
    }
}
