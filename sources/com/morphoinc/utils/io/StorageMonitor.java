package com.morphoinc.utils.io;

import android.os.Environment;
import android.os.StatFs;
import java.io.File;

class StorageMonitor {
    StorageMonitor() {
    }

    public static boolean isExternalStorageMounted() {
        return Environment.getExternalStorageState().equals("mounted");
    }

    public static long getAvailableExternalStorageSpace() {
        if (!Environment.getExternalStorageState().equals("mounted")) {
            return -1;
        }
        File externalStorage = Environment.getExternalStorageDirectory();
        if (externalStorage != null) {
            return getAvailableStorageSpace(externalStorage);
        }
        return -1;
    }

    public static long getAvailableInternalStorageSpace() {
        File internalStorage = Environment.getDataDirectory();
        if (internalStorage != null) {
            return getAvailableStorageSpace(internalStorage);
        }
        return -1;
    }

    private static long getAvailableStorageSpace(File file) {
        StatFs fs = new StatFs(file.getPath());
        return fs.getBlockSizeLong() * fs.getAvailableBlocksLong();
    }
}
