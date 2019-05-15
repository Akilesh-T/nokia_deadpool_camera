package com.hmdglobal.app.camera.util;

import android.content.Context;
import android.os.storage.StorageManager;
import android.util.Log;
import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class StorageUtilProxy {
    public static final String TAG = "HG";
    private static Class<?> environmentClazz;
    private static String mExternalState;
    private static long mLastExternalStateGetTime = 0;
    private static Class storagevolumeClazz;

    static {
        environmentClazz = null;
        storagevolumeClazz = null;
        try {
            environmentClazz = Class.forName("android.os.Environment");
            storagevolumeClazz = Class.forName("android.os.storage.StorageVolume");
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("get class of Environment or EnvironmentEx error:");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
    }

    public static File getPathFromReflectFeild(Object obj) {
        File path = null;
        try {
            Field field = storagevolumeClazz.getDeclaredField("mPath");
            field.setAccessible(true);
            path = (File) field.get(obj);
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getPathFromReflectFeild error:");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getPathFromReflectFeild: ");
        stringBuilder2.append(path);
        Log.i(str2, stringBuilder2.toString());
        return path;
    }

    public static String getStoragePath(Context mContext, boolean is_removale) {
        ClassNotFoundException e;
        InvocationTargetException e2;
        NoSuchMethodException e3;
        IllegalAccessException e4;
        StorageManager mStorageManager = (StorageManager) mContext.getSystemService("storage");
        Class<?> storageVolumeClazz = null;
        boolean z;
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList", new Class[0]);
            Method getPath = storageVolumeClazz.getMethod("getPath", new Class[0]);
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable", new Class[0]);
            Object result = getVolumeList.invoke(mStorageManager, new Object[0]);
            int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String path = (String) getPath.invoke(storageVolumeElement, new Object[0]);
                if (is_removale == ((Boolean) isRemovable.invoke(storageVolumeElement, new Object[0])).booleanValue()) {
                    try {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("getStoragePath path: ");
                        stringBuilder.append(path);
                        Log.i(str, stringBuilder.toString());
                        return path;
                    } catch (ClassNotFoundException e5) {
                        e = e5;
                        e.printStackTrace();
                        return null;
                    } catch (InvocationTargetException e6) {
                        e2 = e6;
                        e2.printStackTrace();
                        return null;
                    } catch (NoSuchMethodException e7) {
                        e3 = e7;
                        e3.printStackTrace();
                        return null;
                    } catch (IllegalAccessException e8) {
                        e4 = e8;
                        e4.printStackTrace();
                        return null;
                    }
                }
            }
            z = is_removale;
        } catch (ClassNotFoundException e9) {
            e = e9;
            z = is_removale;
            e.printStackTrace();
            return null;
        } catch (InvocationTargetException e10) {
            e2 = e10;
            z = is_removale;
            e2.printStackTrace();
            return null;
        } catch (NoSuchMethodException e11) {
            e3 = e11;
            z = is_removale;
            e3.printStackTrace();
            return null;
        } catch (IllegalAccessException e12) {
            e4 = e12;
            z = is_removale;
            e4.printStackTrace();
            return null;
        }
        return null;
    }
}
