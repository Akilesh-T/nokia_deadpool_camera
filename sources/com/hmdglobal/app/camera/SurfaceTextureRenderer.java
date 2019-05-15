package com.hmdglobal.app.camera;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL10;

public class SurfaceTextureRenderer {
    private static final int[] CONFIG_SPEC = new int[]{12352, 4, 12324, 8, 12323, 8, 12322, 8, 12321, 0, 12325, 0, 12326, 0, 12344};
    private static final int EGL_CONTEXT_CLIENT_VERSION = 12440;
    private static final int EGL_OPENGL_ES2_BIT = 4;
    private static final Tag TAG = new Tag("SurfTexRenderer");
    private volatile boolean mDrawPending = false;
    private EGL10 mEgl;
    private EGLConfig mEglConfig;
    private EGLContext mEglContext;
    private EGLDisplay mEglDisplay;
    private final Handler mEglHandler;
    private EGLSurface mEglSurface;
    private final FrameDrawer mFrameDrawer;
    private GL10 mGl;
    private final Object mRenderLock = new Object();
    private final Runnable mRenderTask = new Runnable() {
        public void run() {
            synchronized (SurfaceTextureRenderer.this.mRenderLock) {
                if (!(SurfaceTextureRenderer.this.mEglDisplay == null || SurfaceTextureRenderer.this.mEglSurface == null)) {
                    SurfaceTextureRenderer.this.mFrameDrawer.onDrawFrame(SurfaceTextureRenderer.this.mGl);
                    SurfaceTextureRenderer.this.mEgl.eglSwapBuffers(SurfaceTextureRenderer.this.mEglDisplay, SurfaceTextureRenderer.this.mEglSurface);
                    SurfaceTextureRenderer.this.mDrawPending = false;
                }
                SurfaceTextureRenderer.this.mRenderLock.notifyAll();
            }
        }
    };

    public interface FrameDrawer {
        void onDrawFrame(GL10 gl10);
    }

    public SurfaceTextureRenderer(SurfaceTexture tex, Handler handler, FrameDrawer renderer) {
        this.mEglHandler = handler;
        this.mFrameDrawer = renderer;
        initialize(tex);
    }

    public void release() {
        this.mEglHandler.post(new Runnable() {
            public void run() {
                SurfaceTextureRenderer.this.mEgl.eglDestroySurface(SurfaceTextureRenderer.this.mEglDisplay, SurfaceTextureRenderer.this.mEglSurface);
                SurfaceTextureRenderer.this.mEgl.eglDestroyContext(SurfaceTextureRenderer.this.mEglDisplay, SurfaceTextureRenderer.this.mEglContext);
                SurfaceTextureRenderer.this.mEgl.eglMakeCurrent(SurfaceTextureRenderer.this.mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
                SurfaceTextureRenderer.this.mEgl.eglTerminate(SurfaceTextureRenderer.this.mEglDisplay);
                SurfaceTextureRenderer.this.mEglSurface = null;
                SurfaceTextureRenderer.this.mEglContext = null;
                SurfaceTextureRenderer.this.mEglDisplay = null;
            }
        });
    }

    public void draw(boolean sync) {
        synchronized (this.mRenderLock) {
            if (!this.mDrawPending) {
                this.mEglHandler.post(this.mRenderTask);
                this.mDrawPending = true;
                if (sync) {
                    try {
                        this.mRenderLock.wait();
                    } catch (InterruptedException e) {
                        Log.v(TAG, "RenderLock.wait() interrupted");
                    }
                }
            }
        }
    }

    private void initialize(final SurfaceTexture target) {
        this.mEglHandler.post(new Runnable() {
            public void run() {
                SurfaceTextureRenderer.this.mEgl = (EGL10) EGLContext.getEGL();
                SurfaceTextureRenderer.this.mEglDisplay = SurfaceTextureRenderer.this.mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
                if (SurfaceTextureRenderer.this.mEglDisplay != EGL10.EGL_NO_DISPLAY) {
                    int[] version = new int[2];
                    if (SurfaceTextureRenderer.this.mEgl.eglInitialize(SurfaceTextureRenderer.this.mEglDisplay, version)) {
                        Tag access$800 = SurfaceTextureRenderer.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("EGL version: ");
                        stringBuilder.append(version[0]);
                        stringBuilder.append('.');
                        stringBuilder.append(version[1]);
                        Log.v(access$800, stringBuilder.toString());
                        int[] attribList = new int[]{SurfaceTextureRenderer.EGL_CONTEXT_CLIENT_VERSION, 2, 12344};
                        SurfaceTextureRenderer.this.mEglConfig = SurfaceTextureRenderer.chooseConfig(SurfaceTextureRenderer.this.mEgl, SurfaceTextureRenderer.this.mEglDisplay);
                        SurfaceTextureRenderer.this.mEglContext = SurfaceTextureRenderer.this.mEgl.eglCreateContext(SurfaceTextureRenderer.this.mEglDisplay, SurfaceTextureRenderer.this.mEglConfig, EGL10.EGL_NO_CONTEXT, attribList);
                        if (SurfaceTextureRenderer.this.mEglContext == null || SurfaceTextureRenderer.this.mEglContext == EGL10.EGL_NO_CONTEXT) {
                            throw new RuntimeException("failed to createContext");
                        }
                        SurfaceTextureRenderer.this.mEglSurface = SurfaceTextureRenderer.this.mEgl.eglCreateWindowSurface(SurfaceTextureRenderer.this.mEglDisplay, SurfaceTextureRenderer.this.mEglConfig, target, null);
                        if (SurfaceTextureRenderer.this.mEglSurface == null || SurfaceTextureRenderer.this.mEglSurface == EGL10.EGL_NO_SURFACE) {
                            throw new RuntimeException("failed to createWindowSurface");
                        } else if (SurfaceTextureRenderer.this.mEgl.eglMakeCurrent(SurfaceTextureRenderer.this.mEglDisplay, SurfaceTextureRenderer.this.mEglSurface, SurfaceTextureRenderer.this.mEglSurface, SurfaceTextureRenderer.this.mEglContext)) {
                            SurfaceTextureRenderer.this.mGl = (GL10) SurfaceTextureRenderer.this.mEglContext.getGL();
                            return;
                        } else {
                            throw new RuntimeException("failed to eglMakeCurrent");
                        }
                    }
                    throw new RuntimeException("eglInitialize failed");
                }
                throw new RuntimeException("eglGetDisplay failed");
            }
        });
        waitDone();
    }

    private void waitDone() {
        final Object lock = new Object();
        synchronized (lock) {
            this.mEglHandler.post(new Runnable() {
                public void run() {
                    synchronized (lock) {
                        lock.notifyAll();
                    }
                }
            });
            try {
                lock.wait();
            } catch (InterruptedException e) {
                Log.v(TAG, "waitDone() interrupted");
            }
        }
    }

    private static void checkEglError(String prompt, EGL10 egl) {
        while (true) {
            int eglGetError = egl.eglGetError();
            int error = eglGetError;
            if (eglGetError != 12288) {
                Log.e(TAG, String.format("%s: EGL error: 0x%x", new Object[]{prompt, Integer.valueOf(error)}));
            } else {
                return;
            }
        }
    }

    private static EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
        int[] numConfig = new int[1];
        if (egl.eglChooseConfig(display, CONFIG_SPEC, null, 0, numConfig)) {
            int numConfigs = numConfig[0];
            if (numConfigs > 0) {
                EGLConfig[] configs = new EGLConfig[numConfigs];
                if (egl.eglChooseConfig(display, CONFIG_SPEC, configs, numConfigs, numConfig)) {
                    return configs[0];
                }
                throw new IllegalArgumentException("eglChooseConfig#2 failed");
            }
            throw new IllegalArgumentException("No configs match configSpec");
        }
        throw new IllegalArgumentException("eglChooseConfig failed");
    }
}
