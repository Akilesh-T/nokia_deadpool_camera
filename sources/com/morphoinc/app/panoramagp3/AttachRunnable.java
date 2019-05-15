package com.morphoinc.app.panoramagp3;

import android.media.Image;
import android.media.Image.Plane;
import com.morphoinc.utils.NativeMemoryAllocator;
import java.nio.ByteBuffer;

abstract class AttachRunnable implements Runnable {
    final ByteBuffer[] byteBuffer = new ByteBuffer[3];
    private boolean isNativeBuffer = false;
    final int[] pixelStride = new int[3];
    final int[] rowStride = new int[3];
    private CaptureImage srcImage;

    AttachRunnable() {
    }

    /* Access modifiers changed, original: 0000 */
    public void setImage(CaptureImage attachImage) {
        Image image = attachImage.image();
        if (image != null) {
            setPlane(image.getPlanes()[0], 0);
            setPlane(image.getPlanes()[1], 1);
            setPlane(image.getPlanes()[2], 2);
        } else {
            int width = attachImage.getWidth();
            int height = attachImage.getHeight();
            byte[] src = attachImage.raw();
            this.byteBuffer[0] = createBuffer(src, 0, width * height);
            this.byteBuffer[1] = createBuffer(src, (width * height) + 1, ((width * height) / 2) - 1);
            this.byteBuffer[2] = createBuffer(src, width * height, (width * height) / 2);
            this.rowStride[0] = width;
            this.rowStride[1] = width;
            this.rowStride[2] = width;
            this.pixelStride[0] = 1;
            this.pixelStride[1] = 2;
            this.pixelStride[2] = 2;
            this.isNativeBuffer = true;
        }
        this.srcImage = attachImage;
    }

    /* Access modifiers changed, original: 0000 */
    public void closeSrc() {
        this.srcImage.close();
        if (this.isNativeBuffer) {
            NativeMemoryAllocator.freeBuffer(this.byteBuffer[0]);
            NativeMemoryAllocator.freeBuffer(this.byteBuffer[1]);
            NativeMemoryAllocator.freeBuffer(this.byteBuffer[2]);
            this.byteBuffer[0] = null;
            this.byteBuffer[1] = null;
            this.byteBuffer[2] = null;
            this.isNativeBuffer = false;
        }
    }

    private void setPlane(Plane plane, int index) {
        this.byteBuffer[index] = plane.getBuffer();
        this.rowStride[index] = plane.getRowStride();
        this.pixelStride[index] = plane.getPixelStride();
    }

    private static ByteBuffer createBuffer(byte[] src, int offset, int capacity) {
        ByteBuffer buffer = NativeMemoryAllocator.allocateBuffer(capacity);
        buffer.put(src, offset, capacity);
        buffer.clear();
        return buffer;
    }
}
