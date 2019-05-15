package com.hmdglobal.app.camera.beauty.video;

import android.graphics.SurfaceTexture;
import android.view.Surface;
import com.android.grafika.gles.EglCore;

public class WindowSurface extends EglSurfaceBase {
    private boolean mReleaseSurface;
    private Surface mSurface;

    public WindowSurface(EglCore eglCore, Surface surface, boolean releaseSurface) {
        super(eglCore);
        createWindowSurface(surface);
        this.mSurface = surface;
        this.mReleaseSurface = releaseSurface;
    }

    public WindowSurface(EglCore eglCore, SurfaceTexture surfaceTexture) {
        super(eglCore);
        createWindowSurface(surfaceTexture);
    }

    public void release() {
        releaseEglSurface();
        if (this.mSurface != null) {
            if (this.mReleaseSurface) {
                this.mSurface.release();
            }
            this.mSurface = null;
        }
    }

    public void recreate(EglCore newEglCore) {
        if (this.mSurface != null) {
            this.mEglCore = newEglCore;
            createWindowSurface(this.mSurface);
            return;
        }
        throw new RuntimeException("not yet implemented for SurfaceTexture");
    }
}
