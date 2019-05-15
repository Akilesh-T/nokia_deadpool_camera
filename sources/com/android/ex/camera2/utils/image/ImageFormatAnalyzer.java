package com.android.ex.camera2.utils.image;

import android.media.Image;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;

public class ImageFormatAnalyzer {
    public static final String YUV420_PLANAR = "YUV420_PLANAR";
    public static final String YUV420_SEMIPLANAR = "YUV420_SEMIPLANAR";
    public static final String YVU420_PLANAR = "YVU420_PLANAR";
    public static final String YVU420_SEMIPLANAR = "YVU420_SEMIPLANAR";

    private static final long getByteBufferAddress(ByteBuffer buffer) {
        Field address;
        try {
            address = Buffer.class.getDeclaredField("effectiveDirectAddress");
            address.setAccessible(true);
            return address.getLong(buffer);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            try {
                address = Buffer.class.getDeclaredField("address");
                address.setAccessible(true);
                return address.getLong(buffer);
            } catch (IllegalAccessException | NoSuchFieldException e2) {
                e2.printStackTrace();
                return 0;
            }
        }
    }

    private static String getImageFormat(long uvBuffer, long vuBuffer) {
        if (1 + uvBuffer == vuBuffer) {
            return "YUV420_SEMIPLANAR";
        }
        return "YVU420_SEMIPLANAR";
    }

    public static String getImageFormat(Image image) {
        if (image.getPlanes()[1].getPixelStride() == 1) {
            return "YUV420_PLANAR";
        }
        return getImageFormat(getByteBufferAddress(image.getPlanes()[1].getBuffer()), getByteBufferAddress(image.getPlanes()[2].getBuffer()));
    }
}
