package com.android.grafika.gles;

public class OffscreenSurface extends EglSurfaceBase {
    public OffscreenSurface(EglCore eglCore, int width, int height) {
        super(eglCore);
        createOffscreenSurface(width, height);
    }

    public void release() {
        releaseEglSurface();
    }
}
