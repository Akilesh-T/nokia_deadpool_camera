package com.morphoinc.app.panoramagp3;

import android.media.Image;
import java.nio.ByteBuffer;

public class ConvertFromYuv420Planar implements IImage2BytesConverter {
    public byte[] image2bytes(Image image) {
        int h;
        int width = image.getWidth();
        int height = image.getHeight();
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();
        int yStride = image.getPlanes()[0].getRowStride();
        int uStride = image.getPlanes()[1].getRowStride();
        int vStride = image.getPlanes()[2].getRowStride();
        int yLength = yBuffer.remaining();
        byte[] bytes = new byte[(((width * height) * 3) / 2)];
        if (width < yStride) {
            for (h = 0; h < height; h++) {
                yBuffer.position(h * yStride);
                yBuffer.get(bytes, h * width, width);
            }
        } else {
            yBuffer.get(bytes, 0, yLength);
        }
        h = width / 2;
        int offset = width * height;
        int h2 = 0;
        while (h2 < height / 2) {
            if (h < uStride) {
                uBuffer.position(h2 * uStride);
            }
            if (h < vStride) {
                vBuffer.position(h2 * vStride);
            }
            int offset2 = offset;
            for (offset = 0; offset < h; offset++) {
                bytes[offset2] = uBuffer.get();
                bytes[offset2 + 1] = vBuffer.get();
                offset2 += 2;
            }
            h2++;
            offset = offset2;
        }
        yBuffer.clear();
        uBuffer.clear();
        vBuffer.clear();
        return bytes;
    }
}
