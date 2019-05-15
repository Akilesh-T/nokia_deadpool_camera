package com.android.external;

import java.lang.reflect.Method;

public class ExtSystemProperties {
    private static Class<?> mClassType = null;
    private static Method mGetBooleanMethod = null;
    private static Method mGetIntMethod = null;
    private static Method mGetMethod = null;

    private static void init() {
        try {
            if (mClassType == null) {
                mClassType = Class.forName("android.os.SystemProperties");
                mGetMethod = mClassType.getDeclaredMethod("get", new Class[]{String.class});
                mGetIntMethod = mClassType.getDeclaredMethod("getInt", new Class[]{String.class, Integer.TYPE});
                mGetBooleanMethod = mClassType.getDeclaredMethod("getBoolean", new Class[]{String.class, Boolean.TYPE});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String get(String key) {
        init();
        try {
            return (String) mGetMethod.invoke(mClassType, new Object[]{key});
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int getInt(String key, int def) {
        init();
        int value = def;
        try {
            return ((Integer) mGetIntMethod.invoke(mClassType, new Object[]{key, Integer.valueOf(def)})).intValue();
        } catch (Exception e) {
            e.printStackTrace();
            return value;
        }
    }

    public static boolean getBoolean(String key) {
        init();
        boolean value = false;
        try {
            return ((Boolean) mGetBooleanMethod.invoke(mClassType, new Object[]{key, Boolean.valueOf(value)})).booleanValue();
        } catch (Exception e) {
            e.printStackTrace();
            return value;
        }
    }
}
