package com.hmdglobal.app.camera.beauty.util;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Debug;
import android.os.Debug.MemoryInfo;
import android.util.Log;
import java.util.HashMap;

public class SysUtil {
    public static String API_KEY;
    public static String API_SECRET;
    public static HashMap<String, byte[]> featureMap = new HashMap();

    public static int getNativeMemoryInfo() {
        MemoryInfo memoryInfo = new MemoryInfo();
        Debug.getMemoryInfo(memoryInfo);
        return memoryInfo.nativePss;
    }

    public static boolean checkCameraHasPerm() {
        Camera mCamera = null;
        try {
            mCamera = Camera.open(0);
        } catch (Exception e) {
        }
        if (mCamera == null) {
            Log.i("xie", "xie camera null");
            return false;
        }
        Log.i("xie", "xie camera not null");
        mCamera.release();
        return true;
    }

    public static void getAppDetailSettingIntent(Activity context) {
        Intent intent = new Intent();
        intent.addFlags(268435456);
        if (VERSION.SDK_INT >= 9) {
            intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
            intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        } else if (VERSION.SDK_INT <= 8) {
            intent.setAction("android.intent.action.VIEW");
            intent.setClassName("com.android.settings", "com.android.settings.InstalledAppDetails");
            intent.putExtra("com.android.settings.ApplicationPkgName", context.getPackageName());
        }
        context.startActivity(intent);
    }
}
