package com.android.grafika.gles;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.util.Log;
import android.view.Surface;

public final class EglCore {
    private static final int EGL_RECORDABLE_ANDROID = 12610;
    public static final int FLAG_RECORDABLE = 1;
    public static final int FLAG_TRY_GLES3 = 2;
    private static final String TAG = "Grafika";
    private EGLConfig mEGLConfig;
    private EGLContext mEGLContext;
    private EGLDisplay mEGLDisplay;
    private int mGlVersion;

    public EglCore() {
        this(null, 0);
    }

    public EglCore(EGLContext sharedContext, int flags) {
        this.mEGLDisplay = EGL14.EGL_NO_DISPLAY;
        this.mEGLContext = EGL14.EGL_NO_CONTEXT;
        this.mEGLConfig = null;
        this.mGlVersion = -1;
        if (this.mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            if (sharedContext == null) {
                sharedContext = EGL14.EGL_NO_CONTEXT;
            }
            this.mEGLDisplay = EGL14.eglGetDisplay(0);
            if (this.mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
                int[] version = new int[2];
                if (EGL14.eglInitialize(this.mEGLDisplay, version, 0, version, 1)) {
                    EGLConfig config;
                    if ((flags & 2) != 0) {
                        config = getConfig(flags, 3);
                        if (config != null) {
                            EGLContext context = EGL14.eglCreateContext(this.mEGLDisplay, config, sharedContext, new int[]{12440, 3, 12344}, 0);
                            if (EGL14.eglGetError() == 12288) {
                                this.mEGLConfig = config;
                                this.mEGLContext = context;
                                this.mGlVersion = 3;
                            }
                        }
                    }
                    if (this.mEGLContext == EGL14.EGL_NO_CONTEXT) {
                        config = getConfig(flags, 2);
                        if (config != null) {
                            EGLContext context2 = EGL14.eglCreateContext(this.mEGLDisplay, config, sharedContext, new int[]{12440, 2, 12344}, 0);
                            checkEglError("eglCreateContext");
                            this.mEGLConfig = config;
                            this.mEGLContext = context2;
                            this.mGlVersion = 2;
                        } else {
                            throw new RuntimeException("Unable to find a suitable EGLConfig");
                        }
                    }
                    int[] values = new int[1];
                    EGL14.eglQueryContext(this.mEGLDisplay, this.mEGLContext, 12440, values, 0);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("EGLContext created, client version ");
                    stringBuilder.append(values[0]);
                    Log.d("Grafika", stringBuilder.toString());
                    return;
                }
                this.mEGLDisplay = null;
                throw new RuntimeException("unable to initialize EGL14");
            }
            throw new RuntimeException("unable to get EGL14 display");
        }
        throw new RuntimeException("EGL already set up");
    }

    private EGLConfig getConfig(int flags, int version) {
        int i = version;
        int renderableType = 4;
        if (i >= 3) {
            renderableType = 4 | 64;
        }
        int[] attribList = new int[]{12324, 8, 12323, 8, 12322, 8, 12321, 8, 12352, renderableType, 12344, 0, 12344};
        if ((flags & 1) != 0) {
            attribList[attribList.length - 3] = EGL_RECORDABLE_ANDROID;
            attribList[attribList.length - 2] = 1;
        }
        EGLConfig[] configs = new EGLConfig[1];
        if (EGL14.eglChooseConfig(this.mEGLDisplay, attribList, 0, configs, 0, configs.length, new int[1], 0)) {
            return configs[0];
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unable to find RGB8888 / ");
        stringBuilder.append(i);
        stringBuilder.append(" EGLConfig");
        Log.w("Grafika", stringBuilder.toString());
        return null;
    }

    public void release() {
        if (this.mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(this.mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
            EGL14.eglDestroyContext(this.mEGLDisplay, this.mEGLContext);
            EGL14.eglReleaseThread();
            EGL14.eglTerminate(this.mEGLDisplay);
        }
        this.mEGLDisplay = EGL14.EGL_NO_DISPLAY;
        this.mEGLContext = EGL14.EGL_NO_CONTEXT;
        this.mEGLConfig = null;
    }

    /* Access modifiers changed, original: protected */
    public void finalize() throws Throwable {
        try {
            if (this.mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
                Log.w("Grafika", "WARNING: EglCore was not explicitly released -- state may be leaked");
                release();
            }
            super.finalize();
        } catch (Throwable th) {
            super.finalize();
        }
    }

    public void releaseSurface(EGLSurface eglSurface) {
        EGL14.eglDestroySurface(this.mEGLDisplay, eglSurface);
    }

    public EGLSurface createWindowSurface(Object surface) {
        if ((surface instanceof Surface) || (surface instanceof SurfaceTexture)) {
            EGLSurface eglSurface = EGL14.eglCreateWindowSurface(this.mEGLDisplay, this.mEGLConfig, surface, new int[]{12344}, 0);
            checkEglError("eglCreateWindowSurface");
            if (eglSurface != null) {
                return eglSurface;
            }
            throw new RuntimeException("surface was null");
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("invalid surface: ");
        stringBuilder.append(surface);
        throw new RuntimeException(stringBuilder.toString());
    }

    public EGLSurface createOffscreenSurface(int width, int height) {
        EGLSurface eglSurface = EGL14.eglCreatePbufferSurface(this.mEGLDisplay, this.mEGLConfig, new int[]{12375, width, 12374, height, 12344}, 0);
        checkEglError("eglCreatePbufferSurface");
        if (eglSurface != null) {
            return eglSurface;
        }
        throw new RuntimeException("surface was null");
    }

    public void makeCurrent(EGLSurface eglSurface) {
        if (this.mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            Log.d("Grafika", "NOTE: makeCurrent w/o display");
        }
        if (!EGL14.eglMakeCurrent(this.mEGLDisplay, eglSurface, eglSurface, this.mEGLContext)) {
            throw new RuntimeException("eglMakeCurrent failed");
        }
    }

    public void makeCurrent(EGLSurface drawSurface, EGLSurface readSurface) {
        if (this.mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
            Log.d("Grafika", "NOTE: makeCurrent w/o display");
        }
        if (!EGL14.eglMakeCurrent(this.mEGLDisplay, drawSurface, readSurface, this.mEGLContext)) {
            throw new RuntimeException("eglMakeCurrent(draw,read) failed");
        }
    }

    public void makeNothingCurrent() {
        if (!EGL14.eglMakeCurrent(this.mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)) {
            throw new RuntimeException("eglMakeCurrent failed");
        }
    }

    public boolean swapBuffers(EGLSurface eglSurface) {
        return EGL14.eglSwapBuffers(this.mEGLDisplay, eglSurface);
    }

    public void setPresentationTime(EGLSurface eglSurface, long nsecs) {
        EGLExt.eglPresentationTimeANDROID(this.mEGLDisplay, eglSurface, nsecs);
    }

    public boolean isCurrent(EGLSurface eglSurface) {
        return this.mEGLContext.equals(EGL14.eglGetCurrentContext()) && eglSurface.equals(EGL14.eglGetCurrentSurface(12377));
    }

    public int querySurface(EGLSurface eglSurface, int what) {
        int[] value = new int[1];
        EGL14.eglQuerySurface(this.mEGLDisplay, eglSurface, what, value, 0);
        return value[0];
    }

    public String queryString(int what) {
        return EGL14.eglQueryString(this.mEGLDisplay, what);
    }

    public int getGlVersion() {
        return this.mGlVersion;
    }

    public static void logCurrent(String msg) {
        EGLDisplay display = EGL14.eglGetCurrentDisplay();
        EGLContext context = EGL14.eglGetCurrentContext();
        EGLSurface surface = EGL14.eglGetCurrentSurface(12377);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Current EGL (");
        stringBuilder.append(msg);
        stringBuilder.append("): display=");
        stringBuilder.append(display);
        stringBuilder.append(", context=");
        stringBuilder.append(context);
        stringBuilder.append(", surface=");
        stringBuilder.append(surface);
        Log.i("Grafika", stringBuilder.toString());
    }

    private void checkEglError(String msg) {
        int eglGetError = EGL14.eglGetError();
        int error = eglGetError;
        if (eglGetError != 12288) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(msg);
            stringBuilder.append(": EGL error: 0x");
            stringBuilder.append(Integer.toHexString(error));
            throw new RuntimeException(stringBuilder.toString());
        }
    }
}
