package com.hmdglobal.app.camera.data;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import java.io.FileOutputStream;
import java.io.IOException;

public class DataUtils {
    public static String getPathFromURI(ContentResolver contentResolver, Uri contentUri) {
        Cursor cursor = contentResolver.query(contentUri, new String[]{"_data"}, null, null, null);
        if (cursor == null) {
            return null;
        }
        try {
            int columnIndex = cursor.getColumnIndexOrThrow("_data");
            if (!cursor.moveToFirst()) {
                return null;
            }
            String string = cursor.getString(columnIndex);
            cursor.close();
            return string;
        } finally {
            cursor.close();
        }
    }

    public static void saveFile(byte[] yuv420sp, String fileName, boolean append) {
        try {
            FileOutputStream fos = new FileOutputStream(fileName, append);
            fos.write(yuv420sp);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
