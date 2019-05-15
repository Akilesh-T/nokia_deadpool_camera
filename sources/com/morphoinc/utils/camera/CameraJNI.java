package com.morphoinc.utils.camera;

import android.media.Image;
import android.util.Log;
import com.android.ex.camera2.utils.image.ImageFormatAnalyzer;
import java.nio.ByteBuffer;

public class CameraJNI {
    private static double GAIN_COEF = 1.0d;

    private static native int nativeGetGain(ByteBuffer byteBuffer, ByteBuffer byteBuffer2, ByteBuffer byteBuffer3, int i, int i2, int i3, int i4, int i5, int i6, String str, int i7, int i8, double[] dArr);

    private static native int nativeRenderByteArray(byte[] bArr, ByteBuffer byteBuffer, ByteBuffer byteBuffer2, ByteBuffer byteBuffer3, int i, int i2, int i3, int i4, int i5, int i6, String str, int i7, int i8);

    static {
        try {
            System.loadLibrary("morpho_camera_util");
            Log.d("CameraJNI", "successfully loaded");
        } catch (UnsatisfiedLinkError e) {
            Log.e("CameraJNI", e.getMessage());
            Log.e("CameraJNI", "can't loadLibrary");
        }
    }

    public static double getGain(Image image, double coef) {
        String imageFormat = ImageFormatAnalyzer.getImageFormat(image);
        double[] gain = new double[1];
        if (nativeGetGain(image.getPlanes()[0].getBuffer(), image.getPlanes()[1].getBuffer(), image.getPlanes()[2].getBuffer(), image.getPlanes()[0].getRowStride(), image.getPlanes()[1].getRowStride(), image.getPlanes()[2].getRowStride(), image.getPlanes()[0].getPixelStride(), image.getPlanes()[1].getPixelStride(), image.getPlanes()[2].getPixelStride(), imageFormat, image.getWidth(), image.getHeight(), gain) != 0) {
            return 1.0d;
        }
        gain[0] = gain[0] * coef;
        return gain[0];
    }

    public static double getGain(Image image) {
        return getGain(image, GAIN_COEF);
    }

    public static int renderByteArray(byte[] buffer, Image image) {
        if (image == null) {
            return 0;
        }
        String imageFormat = ImageFormatAnalyzer.getImageFormat(image);
        return nativeRenderByteArray(buffer, image.getPlanes()[0].getBuffer(), image.getPlanes()[1].getBuffer(), image.getPlanes()[2].getBuffer(), image.getPlanes()[0].getRowStride(), image.getPlanes()[1].getRowStride(), image.getPlanes()[2].getRowStride(), image.getPlanes()[0].getPixelStride(), image.getPlanes()[1].getPixelStride(), image.getPlanes()[2].getPixelStride(), imageFormat, image.getWidth(), image.getHeight());
    }
}
