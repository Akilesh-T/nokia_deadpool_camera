package com.hmdglobal.app.camera.beauty.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import java.io.File;

public class FileUtil {
    public static String getZipPath(Context context, String subFileName) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getDiskCachePath(context));
        stringBuilder.append("/");
        stringBuilder.append(subFileName);
        stringBuilder.append("/");
        return stringBuilder.toString();
    }

    public static String getDiskCachePath(Context context) {
        if ("mounted".equals(Environment.getExternalStorageState()) || !Environment.isExternalStorageRemovable()) {
            return context.getExternalFilesDir("beauty").getPath();
        }
        return context.getFilesDir().getPath();
    }

    public static boolean isFileExist(String path) {
        if (new File(path).exists()) {
            return true;
        }
        return false;
    }

    public static boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.isFile() && file.exists()) {
            return file.delete();
        }
        return false;
    }

    public static void renameFile(String oldname, String newname) {
        if (!oldname.equals(newname)) {
            File oldfile = new File(oldname);
            File newfile = new File(newname);
            if (oldfile.exists() && !newfile.exists()) {
                oldfile.renameTo(newfile);
            }
        }
    }

    public static int getVersionCode(Context context) {
        PackageInfo packInfo = null;
        try {
            packInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return packInfo.versionCode;
    }
}
