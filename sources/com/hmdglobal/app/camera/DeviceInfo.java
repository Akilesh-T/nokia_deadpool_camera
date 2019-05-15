package com.hmdglobal.app.camera;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;

public class DeviceInfo {
    public static final String BRAND = Build.BRAND.toLowerCase();
    public static final String DEVICE = Build.DEVICE.toLowerCase();
    private static final String IDOL3 = "idol3";
    private static final String IDOL4 = "idol4";
    private static final int REVERSIBLE_OFF = 0;
    private static final int REVERSIBLE_ON = 1;
    private static final String REVERSIBLE_TAG = "degree_rotation";
    public static final Tag TAG = new Tag("DeviceInfo");

    public static boolean isIdol3() {
        return DEVICE.startsWith(IDOL3);
    }

    public static boolean isIdol4() {
        return DEVICE.startsWith(IDOL4);
    }

    public static Uri getReversibleSettingUri() {
        if (isIdol3()) {
            return Global.getUriFor(REVERSIBLE_TAG);
        }
        if (isIdol4()) {
            return System.getUriFor(REVERSIBLE_TAG);
        }
        return null;
    }

    public static boolean isReversibleOn(ContentResolver resolver) {
        boolean z = true;
        if (isIdol3()) {
            if (Global.getInt(resolver, REVERSIBLE_TAG, 0) != 1) {
                z = false;
            }
            return z;
        } else if (!isIdol4()) {
            return false;
        } else {
            if (Global.getInt(resolver, REVERSIBLE_TAG, 0) != 1) {
                z = false;
            }
            return z;
        }
    }

    public static boolean updateReversibleSetting(ContentResolver resolver, boolean on) {
        Tag tag;
        StringBuilder stringBuilder;
        if (isIdol3()) {
            try {
                Global.putInt(resolver, REVERSIBLE_TAG, on);
                return true;
            } catch (Exception e) {
                tag = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Not permitted:");
                stringBuilder.append(e.getMessage());
                Log.w(tag, stringBuilder.toString());
            }
        } else {
            if (isIdol4()) {
                try {
                    Global.putInt(resolver, REVERSIBLE_TAG, on);
                    return true;
                } catch (Exception e2) {
                    tag = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Not permitted:");
                    stringBuilder.append(e2.getMessage());
                    Log.w(tag, stringBuilder.toString());
                }
            }
            return false;
        }
    }
}
