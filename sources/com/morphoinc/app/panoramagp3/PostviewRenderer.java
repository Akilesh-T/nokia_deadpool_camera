package com.morphoinc.app.panoramagp3;

import android.app.Activity;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Handler;
import android.os.Message;
import com.morphoinc.app.viewer.MorphoPanoramaViewer;
import com.morphoinc.app.viewer.MorphoPanoramaViewer.GalleryData;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class PostviewRenderer implements Renderer {
    private static String LOG_TAG = "PostviewRenderer";
    public static final int MSG_REQUEST_REREGISTER_TEXTURE = 1;
    public static final int MSG_REQUEST_SET_POSTVIEW_DATA = 0;
    private boolean isDefault = false;
    private boolean isFileSelect;
    private boolean isRegistered = false;
    private Activity mActivity;
    private int mDispType = 0;
    private GalleryData mGalleryData;
    private Handler mHandler;
    private MorphoPanoramaViewer mMorphoImageStitcher;
    private boolean mRenderEnable = true;
    private double mRotateAngleX;
    private double mRotateAngleY;
    private double mRotateRatioX;
    private double mRotateRatioY;
    private double mScale = 1.0d;
    private Object mSyncObj;
    private Object mTouchSyncObj;
    private int mViewHeight;
    private int mViewWidth;

    public PostviewRenderer(Activity activity, Handler handler, MorphoPanoramaViewer image_stitcher, boolean is_file_select) {
        this.mMorphoImageStitcher = image_stitcher;
        this.isFileSelect = is_file_select;
        this.mHandler = handler;
        this.mActivity = activity;
        this.mSyncObj = new Object();
        this.mTouchSyncObj = new Object();
    }

    public void setSwipeDistance(float distance_x, float distance_y) {
        synchronized (this.mTouchSyncObj) {
            this.mRotateRatioX += (double) (distance_x / ((float) this.mViewWidth));
            this.mRotateRatioY += (double) (distance_y / ((float) this.mViewHeight));
        }
    }

    public void setSwipeAngle(float x, float y) {
        synchronized (this.mTouchSyncObj) {
            this.mRotateAngleX += (double) x;
            this.mRotateAngleY += (double) y;
        }
    }

    public void setScale(float scale) {
        synchronized (this.mTouchSyncObj) {
            this.mScale *= (double) scale;
            if (this.mScale > 3.0d) {
                this.mScale = 3.0d;
            } else if (this.mScale < 0.8d) {
                this.mScale = 0.8d;
            }
            this.mRotateRatioX = Camera2ParamsFragment.TARGET_EV;
            this.mRotateRatioY = Camera2ParamsFragment.TARGET_EV;
            this.mRotateAngleX = Camera2ParamsFragment.TARGET_EV;
            this.mRotateAngleY = Camera2ParamsFragment.TARGET_EV;
        }
    }

    public void setDefaultScale(double scale) {
        synchronized (this.mTouchSyncObj) {
            this.mScale = scale;
        }
    }

    public void setDefault() {
        synchronized (this.mTouchSyncObj) {
            this.mRotateRatioX = Camera2ParamsFragment.TARGET_EV;
            this.mRotateRatioY = Camera2ParamsFragment.TARGET_EV;
            this.mRotateAngleX = Camera2ParamsFragment.TARGET_EV;
            this.mRotateAngleY = Camera2ParamsFragment.TARGET_EV;
            this.mScale = 1.0d;
            this.isDefault = true;
        }
    }

    public void setDispType(int type) {
        synchronized (this.mSyncObj) {
            this.mDispType = type;
        }
    }

    public void setRenderEnable(boolean enabled) {
        synchronized (this.mSyncObj) {
            this.mRenderEnable = enabled;
        }
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        this.isRegistered = false;
        this.mRenderEnable = false;
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
        int disp_w = this.mActivity.getWindowManager().getDefaultDisplay().getWidth();
        int disp_h = this.mActivity.getWindowManager().getDefaultDisplay().getHeight();
        this.mViewWidth = disp_w;
        this.mViewHeight = disp_h;
        gl.glViewport(0, 0, disp_w, disp_h);
        gl.glScissor(0, 0, disp_w, disp_h);
        if (!this.isRegistered) {
            Message msg = Message.obtain();
            if (this.isFileSelect) {
                msg.arg1 = 0;
            }
            this.mHandler.sendMessage(msg);
            this.isRegistered = true;
        }
    }

    /* JADX WARNING: Missing block: B:35:0x00c3, code skipped:
            return;
     */
    public void onDrawFrame(javax.microedition.khronos.opengles.GL10 r22) {
        /*
        r21 = this;
        r1 = r21;
        r2 = r1.mTouchSyncObj;
        monitor-enter(r2);
        r8 = r1.mScale;	 Catch:{ all -> 0x00cb }
        r3 = r1.mRotateRatioX;	 Catch:{ all -> 0x00cb }
        r3 = java.lang.Math.abs(r3);	 Catch:{ all -> 0x00cb }
        r5 = 0;
        r0 = (r3 > r5 ? 1 : (r3 == r5 ? 0 : -1));
        if (r0 > 0) goto L_0x0028;
    L_0x0013:
        r3 = r1.mRotateRatioY;	 Catch:{ all -> 0x00cb }
        r3 = java.lang.Math.abs(r3);	 Catch:{ all -> 0x00cb }
        r0 = (r3 > r5 ? 1 : (r3 == r5 ? 0 : -1));
        if (r0 <= 0) goto L_0x001e;
    L_0x001d:
        goto L_0x0028;
    L_0x001e:
        r0 = 1;
        r3 = r1.mRotateAngleX;	 Catch:{ all -> 0x00cb }
        r10 = r1.mRotateAngleY;	 Catch:{ all -> 0x00cb }
        r1.mRotateAngleX = r5;	 Catch:{ all -> 0x00cb }
        r1.mRotateAngleY = r5;	 Catch:{ all -> 0x00cb }
        goto L_0x0031;
    L_0x0028:
        r0 = 0;
        r3 = r1.mRotateRatioX;	 Catch:{ all -> 0x00cb }
        r10 = r1.mRotateRatioY;	 Catch:{ all -> 0x00cb }
        r1.mRotateRatioX = r5;	 Catch:{ all -> 0x00cb }
        r1.mRotateRatioY = r5;	 Catch:{ all -> 0x00cb }
    L_0x0031:
        monitor-exit(r2);	 Catch:{ all -> 0x00cb }
        r13 = r3;
        r11 = r10;
        r20 = r0;
        r2 = r1.mSyncObj;
        monitor-enter(r2);
        r0 = r1.mRenderEnable;	 Catch:{ all -> 0x00c4 }
        if (r0 != 0) goto L_0x0044;
    L_0x003d:
        monitor-exit(r2);	 Catch:{ all -> 0x003f }
        return;
    L_0x003f:
        r0 = move-exception;
        r4 = r11;
        r6 = r13;
        goto L_0x00c7;
    L_0x0044:
        r0 = r1.isDefault;	 Catch:{ all -> 0x00c4 }
        if (r0 == 0) goto L_0x006b;
    L_0x0048:
        r0 = LOG_TAG;	 Catch:{ all -> 0x003f }
        r3 = new java.lang.StringBuilder;	 Catch:{ all -> 0x003f }
        r3.<init>();	 Catch:{ all -> 0x003f }
        r4 = "the x_rot is ";
        r3.append(r4);	 Catch:{ all -> 0x003f }
        r3.append(r13);	 Catch:{ all -> 0x003f }
        r3 = r3.toString();	 Catch:{ all -> 0x003f }
        android.util.Log.i(r0, r3);	 Catch:{ all -> 0x003f }
        r0 = r1.mMorphoImageStitcher;	 Catch:{ all -> 0x003f }
        r3 = r1.mDispType;	 Catch:{ all -> 0x003f }
        r0.renderPostviewDefault(r3);	 Catch:{ all -> 0x003f }
        r0 = 0;
        r1.isDefault = r0;	 Catch:{ all -> 0x003f }
    L_0x0068:
        r4 = r11;
        r6 = r13;
        goto L_0x00c2;
    L_0x006b:
        if (r20 == 0) goto L_0x0095;
    L_0x006d:
        r3 = r1.mMorphoImageStitcher;	 Catch:{ all -> 0x003f }
        r10 = r1.mDispType;	 Catch:{ all -> 0x003f }
        r4 = r13;
        r6 = r11;
        r3.renderPostviewAngle(r4, r6, r8, r10);	 Catch:{ all -> 0x003f }
        r0 = LOG_TAG;	 Catch:{ all -> 0x003f }
        r3 = new java.lang.StringBuilder;	 Catch:{ all -> 0x003f }
        r3.<init>();	 Catch:{ all -> 0x003f }
        r4 = "rotation";
        r3.append(r4);	 Catch:{ all -> 0x003f }
        r3.append(r13);	 Catch:{ all -> 0x003f }
        r4 = " ";
        r3.append(r4);	 Catch:{ all -> 0x003f }
        r3.append(r11);	 Catch:{ all -> 0x003f }
        r3 = r3.toString();	 Catch:{ all -> 0x003f }
        android.util.Log.i(r0, r3);	 Catch:{ all -> 0x003f }
        goto L_0x0068;
    L_0x0095:
        r0 = r1.mMorphoImageStitcher;	 Catch:{ all -> 0x00c4 }
        r3 = r1.mDispType;	 Catch:{ all -> 0x00c4 }
        r4 = r11;
        r12 = r0;
        r6 = r13;
        r15 = r4;
        r17 = r8;
        r19 = r3;
        r12.renderPostview(r13, r15, r17, r19);	 Catch:{ all -> 0x00c9 }
        r0 = LOG_TAG;	 Catch:{ all -> 0x00c9 }
        r3 = new java.lang.StringBuilder;	 Catch:{ all -> 0x00c9 }
        r3.<init>();	 Catch:{ all -> 0x00c9 }
        r10 = "rotation1";
        r3.append(r10);	 Catch:{ all -> 0x00c9 }
        r3.append(r6);	 Catch:{ all -> 0x00c9 }
        r10 = " ";
        r3.append(r10);	 Catch:{ all -> 0x00c9 }
        r3.append(r4);	 Catch:{ all -> 0x00c9 }
        r3 = r3.toString();	 Catch:{ all -> 0x00c9 }
        android.util.Log.i(r0, r3);	 Catch:{ all -> 0x00c9 }
    L_0x00c2:
        monitor-exit(r2);	 Catch:{ all -> 0x00c9 }
        return;
    L_0x00c4:
        r0 = move-exception;
        r4 = r11;
        r6 = r13;
    L_0x00c7:
        monitor-exit(r2);	 Catch:{ all -> 0x00c9 }
        throw r0;
    L_0x00c9:
        r0 = move-exception;
        goto L_0x00c7;
    L_0x00cb:
        r0 = move-exception;
        monitor-exit(r2);	 Catch:{ all -> 0x00cb }
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.morphoinc.app.panoramagp3.PostviewRenderer.onDrawFrame(javax.microedition.khronos.opengles.GL10):void");
    }

    private double[] initialParam(GalleryData mGalleryData) {
        return new double[]{Camera2ParamsFragment.TARGET_EV, Camera2ParamsFragment.TARGET_EV};
    }
}
