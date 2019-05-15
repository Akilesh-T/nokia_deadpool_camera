package com.morphoinc.utils.VideoRec;

import android.annotation.SuppressLint;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.util.Log;
import android.view.Surface;
import com.morphoinc.utils.render.ShaderHelper;

@SuppressLint({"NewApi"})
public class VideoRecorder extends BaseVideoRecoder {
    private static final String TAG = "MORPHO";
    private CodecInputSurface mInputSurface;
    private ShaderHelper mShader = null;

    @SuppressLint({"NewApi"})
    private static class CodecInputSurface {
        private static final int EGL_RECORDABLE_ANDROID = 12610;
        private EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
        private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
        private EGLSurface mEGLSurface = EGL14.EGL_NO_SURFACE;
        private EGLContext mParentContext = EGL14.EGL_NO_CONTEXT;
        private Surface mSurface;

        public CodecInputSurface(Surface surface, EGLContext parent) {
            if (surface != null) {
                this.mSurface = surface;
                this.mParentContext = parent;
                eglSetup();
                return;
            }
            throw new NullPointerException();
        }

        public Surface getSurface() {
            return this.mSurface;
        }

        private void eglSetup() {
            this.mEGLDisplay = EGL14.eglGetDisplay(0);
            if (this.mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
                int[] version = new int[2];
                if (EGL14.eglInitialize(this.mEGLDisplay, version, 0, version, 1)) {
                    EGLConfig[] configs = new EGLConfig[1];
                    EGLConfig[] eGLConfigArr = configs;
                    EGL14.eglChooseConfig(this.mEGLDisplay, new int[]{12324, 8, 12323, 8, 12322, 8, 12352, 4, EGL_RECORDABLE_ANDROID, 1, 12344}, 0, eGLConfigArr, 0, configs.length, new int[1], 0);
                    checkEglError("eglCreateContext RGB888+recordable ES2");
                    this.mEGLContext = EGL14.eglCreateContext(this.mEGLDisplay, configs[0], this.mParentContext, new int[]{12440, 2, 12344}, 0);
                    checkEglError("eglCreateContext");
                    this.mEGLSurface = EGL14.eglCreateWindowSurface(this.mEGLDisplay, configs[0], this.mSurface, new int[]{12344}, 0);
                    checkEglError("eglCreateWindowSurface");
                    return;
                }
                throw new RuntimeException("unable to initialize EGL14");
            }
            throw new RuntimeException("unable to get EGL14 display");
        }

        public void release() {
            if (this.mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(this.mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
                EGL14.eglDestroySurface(this.mEGLDisplay, this.mEGLSurface);
                EGL14.eglDestroyContext(this.mEGLDisplay, this.mEGLContext);
            }
            this.mSurface.release();
            this.mEGLDisplay = EGL14.EGL_NO_DISPLAY;
            this.mEGLContext = EGL14.EGL_NO_CONTEXT;
            this.mEGLSurface = EGL14.EGL_NO_SURFACE;
            this.mSurface = null;
        }

        public void makeCurrent() {
            EGL14.eglMakeCurrent(this.mEGLDisplay, this.mEGLSurface, this.mEGLSurface, this.mEGLContext);
            checkEglError("eglMakeCurrent");
        }

        public void makeCurrent(EGLSurface readSurface) {
            EGL14.eglMakeCurrent(this.mEGLDisplay, this.mEGLSurface, readSurface, this.mEGLContext);
            checkEglError("eglMakeCurrent");
        }

        public void unbind() {
            EGL14.eglMakeCurrent(this.mEGLDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
        }

        public void swapBuffers() {
            EGL14.eglSwapBuffers(this.mEGLDisplay, this.mEGLSurface);
            checkEglError("eglSwapBuffers");
        }

        public void setPresentationTime(long nsecs) {
            EGLExt.eglPresentationTimeANDROID(this.mEGLDisplay, this.mEGLSurface, nsecs);
            checkEglError("eglPresentationTimeANDROID");
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

    public /* bridge */ /* synthetic */ String getOutputFilePath() {
        return super.getOutputFilePath();
    }

    public VideoRecorder(int width, int height, int bitrate, float frame_rate, String output_path) {
        super(width, height, bitrate, frame_rate, 2130708361, output_path);
        Log.d(TAG, "VideoRecorder enter");
        EGLContext context = EGL14.eglGetCurrentContext();
        EGLDisplay display = EGL14.eglGetCurrentDisplay();
        EGLSurface read_surface = EGL14.eglGetCurrentSurface(12378);
        EGLSurface draw_surface = EGL14.eglGetCurrentSurface(12377);
        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
        this.mInputSurface = new CodecInputSurface(this.mSurface, context);
        this.mInputSurface.makeCurrent();
        this.mShader = new ShaderHelper();
        this.mShader.initShader(0);
        this.mInputSurface.unbind();
        EGL14.eglMakeCurrent(display, draw_surface, read_surface, context);
        Log.d(TAG, "VideoRecorder exit");
    }

    public void encodeFrame(long timestamp, int texid) {
        EGLContext context = EGL14.eglGetCurrentContext();
        EGLDisplay display = EGL14.eglGetCurrentDisplay();
        EGLSurface read_surface = EGL14.eglGetCurrentSurface(12378);
        EGLSurface draw_surface = EGL14.eglGetCurrentSurface(12377);
        this.mInputSurface.makeCurrent();
        super.drainEncoder(false);
        this.mShader.renderTexture(texid, null);
        this.mInputSurface.setPresentationTime(timestamp);
        this.mInputSurface.swapBuffers();
        this.mInputSurface.unbind();
        EGL14.eglMakeCurrent(display, draw_surface, read_surface, context);
    }

    public void end() {
        EGLContext context = EGL14.eglGetCurrentContext();
        EGLDisplay display = EGL14.eglGetCurrentDisplay();
        EGLSurface read_surface = EGL14.eglGetCurrentSurface(12378);
        EGLSurface draw_surface = EGL14.eglGetCurrentSurface(12377);
        this.mInputSurface.unbind();
        Log.d(TAG, "end process start...");
        this.mInputSurface.makeCurrent();
        super.drainEncoder(true);
        super.releaseEncoder();
        if (this.mInputSurface != null) {
            this.mInputSurface.release();
            this.mInputSurface = null;
        }
        Log.d(TAG, "record ended!!!!");
        EGL14.eglMakeCurrent(display, draw_surface, read_surface, context);
    }
}
