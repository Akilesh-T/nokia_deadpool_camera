package com.hmdglobal.app.camera.beauty.util;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import com.hmdglobal.app.camera.app.CameraApp;
import java.util.Map;

public class SharedUtil {
    private static final String FILE_NAME = "data";
    public static final String VERSION_CODE = "version_code";

    public static void saveIntValue(String key, int value) {
        Editor editor = CameraApp.getContext().getSharedPreferences("data", 0).edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public static void saveLongValue(String key, long value) {
        Editor editor = CameraApp.getContext().getSharedPreferences("data", 0).edit();
        editor.putLong(key, value);
        editor.commit();
    }

    public static void writeDownStartApplicationTime() {
        SharedPreferences sp = CameraApp.getContext().getSharedPreferences("data", 0);
        long now = System.currentTimeMillis();
        Editor editor = sp.edit();
        editor.putLong("nowtimekey", now);
        editor.commit();
    }

    public static void saveBooleanValue(String key, boolean value) {
        Editor editor = CameraApp.getContext().getSharedPreferences("data", 0).edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static void removeSharePreferences(String key) {
        Editor editor = CameraApp.getContext().getSharedPreferences("data", 0).edit();
        editor.remove(key);
        editor.commit();
    }

    public static boolean contains(String key) {
        return CameraApp.getContext().getSharedPreferences("data", 0).contains(key);
    }

    public static Map<String, Object> getAllMap() {
        return CameraApp.getContext().getSharedPreferences("data", 0).getAll();
    }

    public static Integer getIntValueByKey(String key) {
        return Integer.valueOf(CameraApp.getContext().getSharedPreferences("data", 0).getInt(key, 0));
    }

    public static Long getLongValueByKey(String key) {
        return Long.valueOf(CameraApp.getContext().getSharedPreferences("data", 0).getLong(key, -1));
    }

    public static void saveStringValue(String key, String value) {
        Editor editor = CameraApp.getContext().getSharedPreferences("data", 0).edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static String getStringValueByKey(String key) {
        return CameraApp.getContext().getSharedPreferences("data", 0).getString(key, null);
    }

    public static Boolean getBooleanValueByKey(String key) {
        return Boolean.valueOf(CameraApp.getContext().getSharedPreferences("data", 0).getBoolean(key, false));
    }

    public static Integer getIntValueAndRemoveByKey(String key) {
        Integer value = getIntValueByKey(key);
        removeSharePreferences(key);
        return value;
    }

    public static void setUserkey(String userkey) {
        saveStringValue("params_userkey", userkey);
    }

    public static String getUserkey() {
        return getStringValueByKey("params_userkey");
    }
}
