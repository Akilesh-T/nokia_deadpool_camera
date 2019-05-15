package com.hmdglobal.app.camera.ui.camera2;

import android.util.Log;
import java.lang.reflect.Field;

public class Wrapper {
    protected static final boolean DEBUG = false;
    protected static final String TAG = "Wrapper";

    protected static int getFieldValue(Field field, int def) {
        int value = def;
        if (field == null) {
            return value;
        }
        try {
            return ((Integer) field.get(null)).intValue();
        } catch (Exception exception) {
            exception.printStackTrace();
            return value;
        }
    }

    protected static String getFieldValue(Field field, String def) {
        String value = def;
        if (field == null) {
            return value;
        }
        try {
            return (String) field.get(null);
        } catch (Exception exception) {
            exception.printStackTrace();
            return value;
        }
    }

    protected static Field getField(Class<?> classInstance, String name) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getField:");
        stringBuilder.append(classInstance);
        stringBuilder.append(" field:");
        stringBuilder.append(name);
        Log.d(str, stringBuilder.toString());
        Field field = null;
        try {
            field = classInstance.getField(name);
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getField:");
            stringBuilder2.append(classInstance);
            stringBuilder2.append(" ");
            stringBuilder2.append(name);
            Log.d(str2, stringBuilder2.toString());
            return field;
        } catch (Exception exception) {
            exception.printStackTrace();
            return field;
        }
    }
}
