package com.hmdglobal.app.camera.beauty.video;

import android.opengl.EGL14;
import android.opengl.EGLSurface;
import android.util.Log;
import com.android.grafika.gles.EglCore;

public class EglSurfaceBase {
    protected static final String TAG = "EglSurfaceBase";
    private EGLSurface mEGLSurface = EGL14.EGL_NO_SURFACE;
    protected EglCore mEglCore;
    protected int mHeight = -1;
    protected int mWidth = -1;

    protected EglSurfaceBase(EglCore eglCore) {
        this.mEglCore = eglCore;
    }

    public void createWindowSurface(Object surface) {
        if (this.mEGLSurface == EGL14.EGL_NO_SURFACE) {
            this.mEGLSurface = this.mEglCore.createWindowSurface(surface);
            return;
        }
        throw new IllegalStateException("surface already created");
    }

    public void createOffscreenSurface(int width, int height) {
        if (this.mEGLSurface == EGL14.EGL_NO_SURFACE) {
            this.mEGLSurface = this.mEglCore.createOffscreenSurface(width, height);
            this.mWidth = width;
            this.mHeight = height;
            return;
        }
        throw new IllegalStateException("surface already created");
    }

    public int getWidth() {
        if (this.mWidth < 0) {
            return this.mEglCore.querySurface(this.mEGLSurface, 12375);
        }
        return this.mWidth;
    }

    public int getHeight() {
        if (this.mHeight < 0) {
            return this.mEglCore.querySurface(this.mEGLSurface, 12374);
        }
        return this.mHeight;
    }

    public void releaseEglSurface() {
        this.mEglCore.releaseSurface(this.mEGLSurface);
        this.mEGLSurface = EGL14.EGL_NO_SURFACE;
        this.mHeight = -1;
        this.mWidth = -1;
    }

    public void makeCurrent() {
        this.mEglCore.makeCurrent(this.mEGLSurface);
    }

    public void makeCurrentReadFrom(EglSurfaceBase readSurface) {
        this.mEglCore.makeCurrent(this.mEGLSurface, readSurface.mEGLSurface);
    }

    public boolean swapBuffers() {
        boolean result = this.mEglCore.swapBuffers(this.mEGLSurface);
        if (!result) {
            Log.d(TAG, "WARNING: swapBuffers() failed");
        }
        return result;
    }

    public void setPresentationTime(long nsecs) {
        this.mEglCore.setPresentationTime(this.mEGLSurface, nsecs);
    }
}
