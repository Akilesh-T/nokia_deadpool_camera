package com.android.external.plantform;

import android.content.Context;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ExtBuild {
    public static final int DEFAULT = 0;
    private static final String EXTCAMERA_JIRS_DIR = "plantform";
    private static final String TAG = "ExtBuild";
    private static int mValue = 0;

    public static void init() {
        mValue = 0;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DEVICE=");
        stringBuilder.append(Build.DEVICE);
        Log.w(str, stringBuilder.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("HARDWARE=");
        stringBuilder.append(Build.HARDWARE);
        Log.w(str, stringBuilder.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("PRODUCT=");
        stringBuilder.append(Build.PRODUCT);
        Log.w(str, stringBuilder.toString());
    }

    public static int device() {
        return mValue;
    }

    public static IExtCamera createCamera(Camera camera, Context context) {
        return device() != 0 ? null : null;
    }

    private static File doGetFilePath(Context context, String jarFileName) {
        File path = context.getDir(EXTCAMERA_JIRS_DIR, 0);
        if (path == null) {
            return null;
        }
        if (!path.exists() || path.isFile()) {
            path.mkdirs();
        }
        File dexFileFullName = new File(path.getPath(), jarFileName);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("dexFileFullName=");
        stringBuilder.append(dexFileFullName);
        Log.w(str, stringBuilder.toString());
        if (dexFileFullName.exists()) {
            return dexFileFullName;
        }
        try {
            InputStream ios = context.getAssets();
            stringBuilder = new StringBuilder();
            stringBuilder.append(EXTCAMERA_JIRS_DIR);
            stringBuilder.append(File.separator);
            stringBuilder.append(jarFileName);
            ios = ios.open(stringBuilder.toString());
            FileOutputStream fos = new FileOutputStream(dexFileFullName);
            byte[] buffer = new byte[4096];
            while (true) {
                int read = ios.read(buffer, 0, buffer.length);
                int i = read;
                if (read > 0) {
                    fos.write(buffer, 0, i);
                } else {
                    ios.close();
                    fos.flush();
                    fos.close();
                    return dexFileFullName;
                }
            }
        } catch (IOException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
            return null;
        }
    }
}
