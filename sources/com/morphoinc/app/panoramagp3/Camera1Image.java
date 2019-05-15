package com.morphoinc.app.panoramagp3;

public class Camera1Image extends CaptureImage {
    private final int mHeight;
    private final int mWidth;

    public Camera1Image(byte[] raw, int width, int height) {
        super(raw);
        this.mWidth = width;
        this.mHeight = height;
    }

    public String getImageFormat() {
        return "YVU420_SEMIPLANAR";
    }

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }
}
