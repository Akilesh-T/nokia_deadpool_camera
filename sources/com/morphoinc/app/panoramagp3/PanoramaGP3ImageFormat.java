package com.morphoinc.app.panoramagp3;

import android.media.Image;
import com.morphoinc.utils.NativeMemoryAllocator;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;

class PanoramaGP3ImageFormat {
    public static final String YUV420_PLANAR = "YUV420_PLANAR";
    public static final String YUV420_SEMIPLANAR = "YUV420_SEMIPLANAR";
    public static final String YVU420_SEMIPLANAR = "YVU420_SEMIPLANAR";

    PanoramaGP3ImageFormat() {
    }

    private static long getByteBufferAddress(ByteBuffer buffer) {
        try {
            Field address = Buffer.class.getDeclaredField("effectiveDirectAddress");
            address.setAccessible(true);
            return address.getLong(buffer);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return NativeMemoryAllocator.getAddress(buffer);
        }
    }

    private static String getImageFormat(long uvBuffer, long vuBuffer) {
        if (uvBuffer > vuBuffer) {
            return "YVU420_SEMIPLANAR";
        }
        return "YUV420_SEMIPLANAR";
    }

    public static String getImageFormat(Image image) {
        if (image.getPlanes()[1].getPixelStride() == 1) {
            return "YUV420_PLANAR";
        }
        return getImageFormat(getByteBufferAddress(image.getPlanes()[1].getBuffer()), getByteBufferAddress(image.getPlanes()[2].getBuffer()));
    }
}
