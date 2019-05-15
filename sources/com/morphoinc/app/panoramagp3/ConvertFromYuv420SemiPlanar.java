package com.morphoinc.app.panoramagp3;

import android.media.Image;
import java.nio.ByteBuffer;

public class ConvertFromYuv420SemiPlanar implements IImage2BytesConverter {
    private static final int PIXEL_STRIDE = 2;

    public byte[] image2bytes(Image image) {
        int h;
        int width = image.getWidth();
        int height = image.getHeight();
        int h2 = 0;
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();
        int yStride = image.getPlanes()[0].getRowStride();
        int uStride = image.getPlanes()[1].getRowStride();
        byte[] bytes = new byte[(((width * height) * 3) / 2)];
        if (width < yStride) {
            for (h = 0; h < height; h++) {
                yBuffer.position(h * yStride);
                yBuffer.get(bytes, h * width, width);
            }
        } else {
            yBuffer.get(bytes, 0, width * height);
        }
        h = width * height;
        if (width < uStride) {
            while (h2 < (height / 2) - 1) {
                uBuffer.position(h2 * uStride);
                uBuffer.get(bytes, h, width);
                h += width;
                h2++;
            }
            uBuffer.get(bytes, h, width - 1);
            h += width - 1;
        } else {
            h2 = uBuffer.remaining();
            uBuffer.get(bytes, h, h2);
            h += h2;
        }
        vBuffer.position(vBuffer.remaining() - 1);
        vBuffer.get(bytes, h, 1);
        yBuffer.clear();
        uBuffer.clear();
        vBuffer.clear();
        return bytes;
    }
}
