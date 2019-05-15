package com.hmdglobal.app.camera.util;

import android.util.Log;

public final class SystemProperties {
    private static final String TAG = "SystemProperties";

    public static String get(String key, String defaultValue) {
        try {
            return (String) Class.forName("android.os.SystemProperties").getMethod("get", new Class[]{String.class, String.class}).invoke(null, new Object[]{key, defaultValue});
        } catch (Exception e) {
            Log.e(TAG, "Exception while getting system property: ", e);
            return defaultValue;
        }
    }

    private SystemProperties() {
    }
}
