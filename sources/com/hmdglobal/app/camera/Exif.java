package com.hmdglobal.app.camera;

import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.exif.ExifInterface;
import java.io.IOException;

public class Exif {
    private static final Tag TAG = new Tag("CameraExif");

    public static ExifInterface getExif(byte[] jpegData) {
        ExifInterface exif = new ExifInterface();
        if (jpegData != null) {
            try {
                exif.readExif(jpegData);
            } catch (IOException e) {
                Log.w(TAG, "Failed to read EXIF data", e);
            }
        }
        return exif;
    }

    public static int getOrientation(ExifInterface exif) {
        Integer val = exif.getTagIntValue(ExifInterface.TAG_ORIENTATION);
        if (val == null) {
            return 0;
        }
        return ExifInterface.getRotationForOrientationValue(val.shortValue());
    }

    public static int getOrientation(byte[] jpegData) {
        if (jpegData == null) {
            return 0;
        }
        return getOrientation(getExif(jpegData));
    }
}
