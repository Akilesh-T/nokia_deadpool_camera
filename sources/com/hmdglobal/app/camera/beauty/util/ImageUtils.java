package com.hmdglobal.app.camera.beauty.util;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.location.Location;
import android.net.Uri;
import com.hmdglobal.app.camera.Storage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageUtils {
    public static Uri saveImageToGallery(Activity context, Bitmap bmp, String fileName, Location location, int display, int oreientation) {
        IOException e;
        String str = fileName;
        File file = new File(str);
        try {
            FileOutputStream fos = new FileOutputStream(file.getAbsolutePath());
            try {
                boolean isSuccess = bmp.compress(CompressFormat.JPEG, 100, fos);
                long length = fos.getChannel().size();
                fos.flush();
                fos.close();
                return Storage.addImagePNGToMediaStore(context.getContentResolver(), str, System.currentTimeMillis(), location, oreientation, length, file.getAbsolutePath(), bmp.getWidth(), bmp.getHeight(), "image/jpeg");
            } catch (IOException e2) {
                e = e2;
                e.printStackTrace();
                return null;
            }
        } catch (IOException e3) {
            e = e3;
            Bitmap bitmap = bmp;
            e.printStackTrace();
            return null;
        }
    }
}
