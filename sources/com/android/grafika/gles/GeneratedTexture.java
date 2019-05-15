package com.android.grafika.gles;

import android.support.v4.internal.view.SupportMenu;
import android.support.v4.view.InputDeviceCompat;
import java.nio.ByteBuffer;

public class GeneratedTexture {
    private static final int BLACK = 0;
    private static final int BLUE = 16711680;
    private static final int BYTES_PER_PIXEL = 4;
    private static final int CYAN = 16776960;
    private static final int FORMAT = 6408;
    private static final int GREEN = 65280;
    private static final int[] GRID = new int[]{-16776961, -16711681, -16711936, -65281, -1, 1073742079, 1073807104, -16711681, -65281, 65280, -2147483393, -16777216, InputDeviceCompat.SOURCE_ANY, -65281, InputDeviceCompat.SOURCE_ANY, SupportMenu.CATEGORY_MASK};
    private static final int HALF = Integer.MIN_VALUE;
    private static final int LOW = 1073741824;
    private static final int MAGENTA = 16711935;
    private static final int OPAQUE = -16777216;
    private static final int RED = 255;
    private static final int TEX_SIZE = 64;
    private static final int TRANSP = 0;
    private static final int WHITE = 16777215;
    private static final int YELLOW = 65535;
    private static final ByteBuffer sCoarseImageData = generateCoarseData();
    private static final ByteBuffer sFineImageData = generateFineData();

    public enum Image {
        COARSE,
        FINE
    }

    public static int createTestTexture(Image which) {
        ByteBuffer buf;
        switch (which) {
            case COARSE:
                buf = sCoarseImageData;
                break;
            case FINE:
                buf = sFineImageData;
                break;
            default:
                throw new RuntimeException("unknown image");
        }
        return GlUtil.createImageTexture(buf, 64, 64, FORMAT);
    }

    private static ByteBuffer generateCoarseData() {
        byte[] buf = new byte[16384];
        for (int i = 0; i < buf.length; i += 4) {
            int color = GRID[((((i / 4) / 64) / 16) * 4) + (((i / 4) % 64) / 16)];
            if (i == 0) {
                color = -1;
            } else if (i == buf.length - 4) {
                color = -1;
            }
            int green = (color >> 8) & 255;
            int blue = (color >> 16) & 255;
            int alpha = (color >> 24) & 255;
            float alphaM = ((float) alpha) / 255.0f;
            buf[i] = (byte) ((int) (((float) (color & 255)) * alphaM));
            buf[i + 1] = (byte) ((int) (((float) green) * alphaM));
            buf[i + 2] = (byte) ((int) (((float) blue) * alphaM));
            buf[i + 3] = (byte) alpha;
        }
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(buf.length);
        byteBuf.put(buf);
        byteBuf.position(0);
        return byteBuf;
    }

    private static ByteBuffer generateFineData() {
        byte[] buf = new byte[16384];
        byte[] bArr = buf;
        checkerPattern(bArr, 0, 0, 32, 32, -16776961, SupportMenu.CATEGORY_MASK, 1);
        checkerPattern(bArr, 32, 32, 64, 64, -16776961, -16711936, 2);
        checkerPattern(bArr, 0, 32, 32, 64, SupportMenu.CATEGORY_MASK, -16711936, 4);
        checkerPattern(bArr, 32, 0, 64, 32, -1, -16777216, 8);
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(buf.length);
        byteBuf.put(buf);
        byteBuf.position(0);
        return byteBuf;
    }

    private static void checkerPattern(byte[] buf, int left, int top, int right, int bottom, int color1, int color2, int bit) {
        for (int row = top; row < bottom; row++) {
            int rowOffset = (row * 64) * 4;
            for (int col = left; col < right; col++) {
                int color;
                int offset = (col * 4) + rowOffset;
                if (((row & bit) ^ (col & bit)) == 0) {
                    color = color1;
                } else {
                    color = color2;
                }
                int green = (color >> 8) & 255;
                int blue = (color >> 16) & 255;
                int alpha = (color >> 24) & 255;
                float alphaM = ((float) alpha) / 255.0f;
                buf[offset] = (byte) ((int) (((float) (color & 255)) * alphaM));
                buf[offset + 1] = (byte) ((int) (((float) green) * alphaM));
                buf[offset + 2] = (byte) ((int) (((float) blue) * alphaM));
                buf[offset + 3] = (byte) alpha;
            }
        }
        int i = right;
    }
}
