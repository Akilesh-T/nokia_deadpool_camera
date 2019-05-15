package com.android.camera.imageprocessor.filter;

public interface ImageFilter {
    void deinit();

    void init(int i, int i2, int i3, int i4, float[] fArr);

    boolean isSupported();

    void process(byte[] bArr);
}
