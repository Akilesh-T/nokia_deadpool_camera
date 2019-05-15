package com.morphoinc.utils.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

class FileOperator {
    FileOperator() {
    }

    public static boolean isFileExists(String filePath) {
        return filePath != null && new File(filePath).exists();
    }

    public static boolean copyFile(String srcPath, String dstPath) {
        if (srcPath == null || dstPath == null) {
            return false;
        }
        File src = new File(srcPath);
        File dst = new File(dstPath);
        try {
            FileChannel srcChannel = new FileInputStream(src).getChannel();
            FileChannel dstChannel = new FileOutputStream(dst).getChannel();
            srcChannel.transferTo(0, srcChannel.size(), dstChannel);
            try {
                srcChannel.close();
                dstChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        } catch (FileNotFoundException e2) {
            e2.printStackTrace();
            return false;
        } catch (IOException e3) {
            e3.printStackTrace();
            return false;
        }
    }

    public static boolean renameFile(String srcPath, String dstPath) {
        boolean result = false;
        if (srcPath == null || dstPath == null) {
            return false;
        }
        File src = new File(srcPath);
        File dst = new File(dstPath);
        if (src.exists()) {
            result = src.renameTo(dst);
        }
        return result;
    }

    public static boolean deleteFile(String filePath) {
        if (filePath == null) {
            return false;
        }
        File file = new File(filePath);
        if (file.exists()) {
            return file.delete();
        }
        return false;
    }
}
