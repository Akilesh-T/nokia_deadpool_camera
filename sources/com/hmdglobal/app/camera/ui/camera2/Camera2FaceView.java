package com.hmdglobal.app.camera.ui.camera2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.camera2.params.Face;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.util.ArrayUtil;
import com.morphoinc.utils.multimedia.MediaProviderUtils;

public class Camera2FaceView extends FaceView {
    private static final Tag TAG = new Tag("Camera2FaceView");
    private final int blink_threshold = 60;
    private Rect mCameraBound;
    private ExtendedFace[] mExFaces;
    private Matrix mFaceScale = new Matrix();
    private Face[] mFaces;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                Camera2FaceView.this.mStateSwitchPending = false;
                Camera2FaceView.this.mFaces = Camera2FaceView.this.mPendingFaces;
                Camera2FaceView.this.mExFaces = Camera2FaceView.this.mPendingExFaces;
                Camera2FaceView.this.invalidate();
            }
        }
    };
    private Rect mOriginalCameraBound;
    private ExtendedFace[] mPendingExFaces;
    private Face[] mPendingFaces;
    private float mZoom = 1.0f;
    private final int smile_threashold_no_smile = 30;
    private final int smile_threashold_small_smile = 60;

    public Camera2FaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setCameraBound(Rect cameraBound) {
        this.mCameraBound = cameraBound;
    }

    public void setOriginalCameraBound(Rect originalCameraBound) {
        this.mOriginalCameraBound = originalCameraBound;
    }

    public void setZoom(float zoom) {
        this.mZoom = zoom;
    }

    public void setFaces(@NonNull Face[] faces, ExtendedFace[] extendedFaces) {
        if (!this.mPause) {
            if (this.mFaces == null || ((faces.length <= 0 || this.mFaces.length != 0) && (faces.length != 0 || this.mFaces.length <= 0))) {
                if (this.mStateSwitchPending) {
                    this.mStateSwitchPending = false;
                    this.mHandler.removeMessages(1);
                }
                this.mFaces = faces;
                this.mExFaces = extendedFaces;
                if (!(this.mBlocked || this.mFaces.length <= 0 || this.mCameraBound == null)) {
                    invalidate();
                }
                return;
            }
            this.mPendingFaces = faces;
            this.mPendingExFaces = extendedFaces;
            if (!this.mStateSwitchPending) {
                this.mStateSwitchPending = true;
                this.mHandler.sendEmptyMessageDelayed(1, 70);
            }
        }
    }

    private boolean isFDRectOutOfBound(Rect faceRect) {
        return this.mCameraBound.left > faceRect.left || this.mCameraBound.top > faceRect.top || faceRect.right > this.mCameraBound.right || faceRect.bottom > this.mCameraBound.bottom;
    }

    public boolean faceExists() {
        return ArrayUtil.isEmpty(this.mFaces) ^ 1;
    }

    /* Access modifiers changed, original: protected */
    /* JADX WARNING: Removed duplicated region for block: B:94:0x05bf  */
    /* JADX WARNING: Removed duplicated region for block: B:93:0x05a0  */
    /* JADX WARNING: Removed duplicated region for block: B:95:0x05c2  */
    /* JADX WARNING: Removed duplicated region for block: B:84:0x052a  */
    /* JADX WARNING: Removed duplicated region for block: B:112:0x0769  */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x06c8  */
    /* JADX WARNING: Removed duplicated region for block: B:123:0x0813  */
    /* JADX WARNING: Removed duplicated region for block: B:115:0x077e  */
    /* JADX WARNING: Removed duplicated region for block: B:134:0x095b  */
    /* JADX WARNING: Removed duplicated region for block: B:126:0x081f  */
    public void onDraw(android.graphics.Canvas r50) {
        /*
        r49 = this;
        r0 = r49;
        r7 = r50;
        r1 = r0.mBlocked;
        if (r1 != 0) goto L_0x0994;
    L_0x0008:
        r1 = r0.mFaces;
        if (r1 == 0) goto L_0x0994;
    L_0x000c:
        r1 = r0.mFaces;
        r1 = r1.length;
        if (r1 <= 0) goto L_0x0994;
    L_0x0011:
        r1 = r0.mCameraBound;
        if (r1 == 0) goto L_0x0994;
    L_0x0015:
        r1 = r0.mUncroppedWidth;
        r2 = r0.mUncroppedHeight;
        r8 = 270; // 0x10e float:3.78E-43 double:1.334E-321;
        r9 = 90;
        r10 = 180; // 0xb4 float:2.52E-43 double:8.9E-322;
        if (r2 <= r1) goto L_0x0029;
    L_0x0021:
        r3 = r0.mDisplayOrientation;
        if (r3 == 0) goto L_0x0033;
    L_0x0025:
        r3 = r0.mDisplayOrientation;
        if (r3 == r10) goto L_0x0033;
    L_0x0029:
        if (r1 <= r2) goto L_0x0036;
    L_0x002b:
        r3 = r0.mDisplayOrientation;
        if (r3 == r9) goto L_0x0033;
    L_0x002f:
        r3 = r0.mDisplayOrientation;
        if (r3 != r8) goto L_0x0036;
    L_0x0033:
        r3 = r1;
        r1 = r2;
        r2 = r3;
    L_0x0036:
        r3 = r0.mCameraBound;
        r3 = r3.width();
        r3 = r3 * r1;
        r4 = r0.mCameraBound;
        r4 = r4.height();
        r4 = r4 * r2;
        if (r3 == r4) goto L_0x0074;
    L_0x0046:
        if (r1 == r2) goto L_0x0065;
    L_0x0048:
        r3 = r2 * 288;
        r4 = r1 * 352;
        if (r3 == r4) goto L_0x0065;
    L_0x004e:
        r3 = r2 * 480;
        r4 = r1 * 800;
        if (r3 != r4) goto L_0x0055;
    L_0x0054:
        goto L_0x0065;
    L_0x0055:
        r3 = r0.mCameraBound;
        r3 = r3.height();
        r3 = r3 * r2;
        r4 = r0.mCameraBound;
        r4 = r4.width();
        r1 = r3 / r4;
        goto L_0x0074;
    L_0x0065:
        r3 = r0.mCameraBound;
        r3 = r3.width();
        r3 = r3 * r1;
        r4 = r0.mCameraBound;
        r4 = r4.height();
        r2 = r3 / r4;
    L_0x0074:
        r11 = r1;
        r12 = r2;
        r1 = r0.mMatrix;
        r2 = r0.mMirror;
        r3 = r0.mDisplayOrientation;
        com.hmdglobal.app.camera.util.CameraUtil.prepareMatrix(r1, r2, r3, r11, r12);
        r1 = new android.graphics.Matrix;
        r1.<init>();
        r13 = r1;
        r1 = r0.mCameraBound;
        r1 = r1.width();
        r1 = -r1;
        r1 = (float) r1;
        r2 = 1073741824; // 0x40000000 float:2.0 double:5.304989477E-315;
        r1 = r1 / r2;
        r3 = r0.mCameraBound;
        r3 = r3.height();
        r3 = -r3;
        r3 = (float) r3;
        r3 = r3 / r2;
        r13.preTranslate(r1, r3);
        r1 = r0.mCameraBound;
        r1 = r1.width();
        r1 = (float) r1;
        r3 = 1157234688; // 0x44fa0000 float:2000.0 double:5.717499035E-315;
        r1 = r3 / r1;
        r4 = r0.mCameraBound;
        r4 = r4.height();
        r4 = (float) r4;
        r4 = r3 / r4;
        r13.postScale(r1, r4);
        r1 = new android.graphics.Matrix;
        r1.<init>();
        r14 = r1;
        r1 = r0.mCameraBound;
        r1 = r1.width();
        r1 = -r1;
        r1 = (float) r1;
        r1 = r1 / r2;
        r4 = r0.mZoom;
        r1 = r1 * r4;
        r4 = r0.mCameraBound;
        r4 = r4.height();
        r4 = -r4;
        r4 = (float) r4;
        r4 = r4 / r2;
        r2 = r0.mZoom;
        r4 = r4 * r2;
        r14.preTranslate(r1, r4);
        r1 = r0.mCameraBound;
        r1 = r1.width();
        r1 = (float) r1;
        r1 = r3 / r1;
        r2 = r0.mCameraBound;
        r2 = r2.height();
        r2 = (float) r2;
        r3 = r3 / r2;
        r14.postScale(r1, r3);
        r1 = r49.getWidth();
        r2 = r0.mUncroppedWidth;
        r1 = r1 - r2;
        r15 = 2;
        r1 = r1 / r15;
        r2 = r0.mUncroppedWidth;
        r2 = r11 - r2;
        r2 = r2 / r15;
        r6 = r1 - r2;
        r1 = r49.getHeight();
        r2 = r0.mUncroppedHeight;
        r1 = r1 - r2;
        r1 = r1 / r15;
        r2 = r0.mUncroppedHeight;
        r2 = r12 - r2;
        r2 = r2 / r15;
        r5 = r1 - r2;
        r50.save();
        r1 = r0.mMatrix;
        r2 = r0.mOrientation;
        r2 = (float) r2;
        r1.postRotate(r2);
        r1 = r0.mOrientation;
        r1 = -r1;
        r1 = (float) r1;
        r7.rotate(r1);
        r1 = 0;
        r2 = r0.mExFaces;
        r16 = 0;
        if (r2 != 0) goto L_0x0123;
    L_0x0120:
        r2 = r16;
        goto L_0x0126;
    L_0x0123:
        r2 = r0.mExFaces;
        r2 = r2.length;
    L_0x0126:
        r4 = r2;
        r1 = r16;
    L_0x0129:
        r3 = r1;
        r1 = r0.mFaces;
        r1 = r1.length;
        if (r3 >= r1) goto L_0x0984;
    L_0x012f:
        r1 = r0.mFaces;
        r1 = r1[r3];
        r1 = r1.getScore();
        r2 = 50;
        if (r1 >= r2) goto L_0x0152;
    L_0x013b:
        r40 = r3;
        r36 = r4;
        r3 = r7;
        r17 = r8;
        r18 = r9;
        r21 = r10;
        r27 = r11;
        r28 = r12;
        r29 = r13;
        r7 = r14;
        r13 = r15;
    L_0x014e:
        r10 = r5;
        r15 = r6;
        goto L_0x096d;
    L_0x0152:
        r1 = r0.mFaces;
        r1 = r1[r3];
        r2 = r1.getBounds();
        r1 = r0.mOriginalCameraBound;
        r1 = r1.left;
        r1 = -r1;
        r8 = r0.mOriginalCameraBound;
        r8 = r8.top;
        r8 = -r8;
        r2.offset(r1, r8);
        r1 = r0.isFDRectOutOfBound(r2);
        if (r1 == 0) goto L_0x0181;
    L_0x016d:
        r40 = r3;
        r36 = r4;
        r3 = r7;
        r18 = r9;
        r21 = r10;
        r27 = r11;
        r28 = r12;
        r29 = r13;
        r7 = r14;
        r13 = r15;
        r17 = 270; // 0x10e float:3.78E-43 double:1.334E-321;
        goto L_0x014e;
    L_0x0181:
        r1 = r0.mRect;
        r1.set(r2);
        r1 = r0.mZoom;
        r8 = 1065353216; // 0x3f800000 float:1.0 double:5.263544247E-315;
        r1 = (r1 > r8 ? 1 : (r1 == r8 ? 0 : -1));
        if (r1 == 0) goto L_0x01c6;
    L_0x018e:
        r1 = r0.mRect;
        r8 = r0.mRect;
        r8 = r8.left;
        r9 = r0.mCameraBound;
        r9 = r9.left;
        r9 = (float) r9;
        r8 = r8 - r9;
        r1.left = r8;
        r1 = r0.mRect;
        r8 = r0.mRect;
        r8 = r8.right;
        r9 = r0.mCameraBound;
        r9 = r9.left;
        r9 = (float) r9;
        r8 = r8 - r9;
        r1.right = r8;
        r1 = r0.mRect;
        r8 = r0.mRect;
        r8 = r8.top;
        r9 = r0.mCameraBound;
        r9 = r9.top;
        r9 = (float) r9;
        r8 = r8 - r9;
        r1.top = r8;
        r1 = r0.mRect;
        r8 = r0.mRect;
        r8 = r8.bottom;
        r9 = r0.mCameraBound;
        r9 = r9.top;
        r9 = (float) r9;
        r8 = r8 - r9;
        r1.bottom = r8;
    L_0x01c6:
        r1 = r0.mRect;
        r13.mapRect(r1);
        r1 = r0.mMatrix;
        r8 = r0.mRect;
        r1.mapRect(r8);
        r1 = r0.mRect;
        r8 = (float) r6;
        r9 = (float) r5;
        r1.offset(r8, r9);
        r1 = r0.mRect;
        r1 = r1.height();
        r8 = r0.mRect;
        r8 = r8.width();
        r8 = r1 - r8;
        r1 = 0;
        r9 = (r8 > r1 ? 1 : (r8 == r1 ? 0 : -1));
        if (r9 <= 0) goto L_0x0207;
    L_0x01ec:
        r9 = r0.mRect;
        r15 = r9.top;
        r19 = 1063675494; // 0x3f666666 float:0.9 double:5.2552552E-315;
        r19 = r19 * r8;
        r15 = r15 + r19;
        r9.top = r15;
        r9 = r0.mRect;
        r15 = r9.bottom;
        r19 = 1036831949; // 0x3dcccccd float:0.1 double:5.122630465E-315;
        r19 = r19 * r8;
        r15 = r15 - r19;
        r9.bottom = r15;
        goto L_0x021d;
    L_0x0207:
        r9 = r0.mRect;
        r15 = r9.left;
        r19 = 1056964608; // 0x3f000000 float:0.5 double:5.222099017E-315;
        r20 = r8 * r19;
        r15 = r15 + r20;
        r9.left = r15;
        r9 = r0.mRect;
        r15 = r9.right;
        r19 = r19 * r8;
        r15 = r15 - r19;
        r9.right = r15;
    L_0x021d:
        r9 = r0.mUncroppedHeight;
        r15 = 1424; // 0x590 float:1.995E-42 double:7.035E-321;
        if (r9 != r15) goto L_0x022c;
    L_0x0223:
        r9 = r0.mRect;
        r15 = -1054867456; // 0xffffffffc1200000 float:-10.0 double:NaN;
        r10 = -1035468800; // 0xffffffffc2480000 float:-50.0 double:NaN;
        r9.offset(r15, r10);
    L_0x022c:
        r9 = r0.mUncroppedHeight;
        r10 = 1280; // 0x500 float:1.794E-42 double:6.324E-321;
        if (r9 != r10) goto L_0x0239;
    L_0x0232:
        r9 = r0.mRect;
        r10 = 1109393408; // 0x42200000 float:40.0 double:5.481131706E-315;
        r9.offset(r1, r10);
    L_0x0239:
        r9 = r0.mUncroppedHeight;
        r10 = 960; // 0x3c0 float:1.345E-42 double:4.743E-321;
        if (r9 != r10) goto L_0x0248;
    L_0x023f:
        r9 = r0.mRect;
        r10 = 1092616192; // 0x41200000 float:10.0 double:5.398241246E-315;
        r15 = -1027080192; // 0xffffffffc2c80000 float:-100.0 double:NaN;
        r9.offset(r10, r15);
    L_0x0248:
        r9 = r0.mUncroppedHeight;
        r10 = 720; // 0x2d0 float:1.009E-42 double:3.557E-321;
        if (r9 != r10) goto L_0x0255;
    L_0x024e:
        r9 = r0.mRect;
        r10 = -1024458752; // 0xffffffffc2f00000 float:-120.0 double:NaN;
        r9.offset(r1, r10);
    L_0x0255:
        r1 = r0.mRect;
        r1 = r1.bottom;
        r9 = r0.mRect;
        r9 = r9.top;
        r9 = r1 - r9;
        r1 = r0.mRect;
        r1 = r1.right;
        r10 = r0.mRect;
        r10 = r10.left;
        r10 = r1 - r10;
        r1 = (r9 > r10 ? 1 : (r9 == r10 ? 0 : -1));
        if (r1 <= 0) goto L_0x026f;
    L_0x026d:
        r1 = r10;
        goto L_0x0270;
    L_0x026f:
        r1 = r9;
    L_0x0270:
        r15 = r1;
        r1 = r0.mRect;
        r1 = r1.left;
        r1 = java.lang.Math.round(r1);
        r1 = (float) r1;
        r22 = r1;
        r1 = r0.mRect;
        r1 = r1.top;
        r1 = java.lang.Math.round(r1);
        r1 = (float) r1;
        r23 = r1;
        r1 = r0.mRect;
        r1 = r1.left;
        r19 = 1077936128; // 0x40400000 float:3.0 double:5.325712093E-315;
        r20 = r10 / r19;
        r1 = r1 + r20;
        r1 = java.lang.Math.round(r1);
        r1 = (float) r1;
        r24 = r1;
        r1 = r0.mRect;
        r1 = r1.top;
        r1 = java.lang.Math.round(r1);
        r1 = (float) r1;
        r25 = r6;
        r6 = r0.mPaint;
        r20 = r22;
        r22 = r23;
        r23 = r24;
        r24 = r1;
        r1 = r7;
        r26 = r8;
        r8 = r2;
        r2 = r20;
        r27 = r11;
        r11 = r3;
        r3 = r22;
        r28 = r12;
        r12 = r4;
        r4 = r23;
        r29 = r13;
        r13 = r5;
        r5 = r24;
        r30 = r15;
        r15 = r25;
        r1.drawLine(r2, r3, r4, r5, r6);
        r1 = r0.mRect;
        r1 = r1.left;
        r1 = java.lang.Math.round(r1);
        r2 = (float) r1;
        r1 = r0.mRect;
        r1 = r1.top;
        r1 = java.lang.Math.round(r1);
        r3 = (float) r1;
        r1 = r0.mRect;
        r1 = r1.left;
        r1 = java.lang.Math.round(r1);
        r4 = (float) r1;
        r1 = r0.mRect;
        r1 = r1.top;
        r5 = r9 / r19;
        r1 = r1 + r5;
        r1 = java.lang.Math.round(r1);
        r5 = (float) r1;
        r6 = r0.mPaint;
        r1 = r7;
        r1.drawLine(r2, r3, r4, r5, r6);
        r1 = r0.mRect;
        r1 = r1.left;
        r1 = r1 + r10;
        r1 = java.lang.Math.round(r1);
        r2 = (float) r1;
        r1 = r0.mRect;
        r1 = r1.top;
        r1 = java.lang.Math.round(r1);
        r3 = (float) r1;
        r1 = r0.mRect;
        r1 = r1.left;
        r1 = r1 + r10;
        r4 = r10 / r19;
        r1 = r1 - r4;
        r1 = java.lang.Math.round(r1);
        r4 = (float) r1;
        r1 = r0.mRect;
        r1 = r1.top;
        r1 = java.lang.Math.round(r1);
        r5 = (float) r1;
        r6 = r0.mPaint;
        r1 = r7;
        r1.drawLine(r2, r3, r4, r5, r6);
        r1 = r0.mRect;
        r1 = r1.left;
        r1 = r1 + r10;
        r1 = java.lang.Math.round(r1);
        r2 = (float) r1;
        r1 = r0.mRect;
        r1 = r1.top;
        r1 = java.lang.Math.round(r1);
        r3 = (float) r1;
        r1 = r0.mRect;
        r1 = r1.left;
        r1 = r1 + r10;
        r1 = java.lang.Math.round(r1);
        r4 = (float) r1;
        r1 = r0.mRect;
        r1 = r1.top;
        r5 = r9 / r19;
        r1 = r1 + r5;
        r1 = java.lang.Math.round(r1);
        r5 = (float) r1;
        r6 = r0.mPaint;
        r1 = r7;
        r1.drawLine(r2, r3, r4, r5, r6);
        r1 = r0.mRect;
        r1 = r1.left;
        r1 = java.lang.Math.round(r1);
        r2 = (float) r1;
        r1 = r0.mRect;
        r1 = r1.top;
        r1 = r1 + r9;
        r1 = java.lang.Math.round(r1);
        r3 = (float) r1;
        r1 = r0.mRect;
        r1 = r1.left;
        r1 = java.lang.Math.round(r1);
        r4 = (float) r1;
        r1 = r0.mRect;
        r1 = r1.top;
        r1 = r1 + r9;
        r5 = r9 / r19;
        r1 = r1 - r5;
        r1 = java.lang.Math.round(r1);
        r5 = (float) r1;
        r6 = r0.mPaint;
        r1 = r7;
        r1.drawLine(r2, r3, r4, r5, r6);
        r1 = r0.mRect;
        r1 = r1.left;
        r1 = java.lang.Math.round(r1);
        r2 = (float) r1;
        r1 = r0.mRect;
        r1 = r1.top;
        r1 = r1 + r9;
        r1 = java.lang.Math.round(r1);
        r3 = (float) r1;
        r1 = r0.mRect;
        r1 = r1.left;
        r4 = r10 / r19;
        r1 = r1 + r4;
        r1 = java.lang.Math.round(r1);
        r4 = (float) r1;
        r1 = r0.mRect;
        r1 = r1.top;
        r1 = r1 + r9;
        r1 = java.lang.Math.round(r1);
        r5 = (float) r1;
        r6 = r0.mPaint;
        r1 = r7;
        r1.drawLine(r2, r3, r4, r5, r6);
        r1 = r0.mRect;
        r1 = r1.right;
        r1 = java.lang.Math.round(r1);
        r2 = (float) r1;
        r1 = r0.mRect;
        r1 = r1.bottom;
        r1 = java.lang.Math.round(r1);
        r3 = (float) r1;
        r1 = r0.mRect;
        r1 = r1.right;
        r1 = java.lang.Math.round(r1);
        r4 = (float) r1;
        r1 = r0.mRect;
        r1 = r1.bottom;
        r5 = r9 / r19;
        r1 = r1 - r5;
        r1 = java.lang.Math.round(r1);
        r5 = (float) r1;
        r6 = r0.mPaint;
        r1 = r7;
        r1.drawLine(r2, r3, r4, r5, r6);
        r1 = r0.mRect;
        r1 = r1.right;
        r1 = java.lang.Math.round(r1);
        r2 = (float) r1;
        r1 = r0.mRect;
        r1 = r1.bottom;
        r1 = java.lang.Math.round(r1);
        r3 = (float) r1;
        r1 = r0.mRect;
        r1 = r1.right;
        r4 = r10 / r19;
        r1 = r1 - r4;
        r1 = java.lang.Math.round(r1);
        r4 = (float) r1;
        r1 = r0.mRect;
        r1 = r1.bottom;
        r1 = java.lang.Math.round(r1);
        r5 = (float) r1;
        r6 = r0.mPaint;
        r1 = r7;
        r1.drawLine(r2, r3, r4, r5, r6);
        r1 = r0.mPaint;
        r2 = 1067030938; // 0x3f99999a float:1.2 double:5.271833295E-315;
        r1.setStrokeWidth(r2);
        if (r11 >= r12) goto L_0x095f;
    L_0x0416:
        r1 = r0.mExFaces;
        r1 = r1[r11];
        if (r1 == 0) goto L_0x095f;
    L_0x041c:
        r1 = r0.mExFaces;
        r6 = r1[r11];
        r1 = r0.mFaces;
        r5 = r1[r11];
        r1 = 4;
        r4 = new float[r1];
        r1 = r8.width();
        r1 = r1 / 12;
        r2 = r8.height();
        r2 = r2 / 12;
        r3 = (float) r1;
        r31 = r1;
        r1 = r0.mZoom;
        r3 = r3 * r1;
        r3 = (int) r3;
        r1 = (float) r2;
        r32 = r2;
        r2 = r0.mZoom;
        r1 = r1 * r2;
        r2 = (int) r1;
        r1 = TAG;
        r33 = r8;
        r8 = new java.lang.StringBuilder;
        r8.<init>();
        r34 = r9;
        r9 = "blink: (";
        r8.append(r9);
        r9 = r6.getLeyeBlink();
        r8.append(r9);
        r9 = ", ";
        r8.append(r9);
        r9 = r6.getReyeBlink();
        r8.append(r9);
        r9 = ")";
        r8.append(r9);
        r8 = r8.toString();
        com.hmdglobal.app.camera.debug.Log.e(r1, r8);
        r1 = r5.getLeftEyePosition();
        r9 = 3;
        r19 = 1;
        if (r1 == 0) goto L_0x051a;
    L_0x0479:
        r1 = r0.mDisplayRotation;
        if (r1 == 0) goto L_0x04b0;
    L_0x047d:
        r1 = r0.mDisplayRotation;
        r8 = 180; // 0xb4 float:2.52E-43 double:8.9E-322;
        if (r1 != r8) goto L_0x0484;
    L_0x0483:
        goto L_0x04b0;
    L_0x0484:
        r1 = r5.getLeftEyePosition();
        r1 = r1.x;
        r8 = r3 / 2;
        r1 = r1 - r8;
        r1 = (float) r1;
        r4[r16] = r1;
        r1 = r5.getLeftEyePosition();
        r1 = r1.y;
        r1 = (float) r1;
        r4[r19] = r1;
        r1 = r5.getLeftEyePosition();
        r1 = r1.x;
        r8 = r3 / 2;
        r1 = r1 + r8;
        r1 = (float) r1;
        r8 = 2;
        r4[r8] = r1;
        r1 = r5.getLeftEyePosition();
        r1 = r1.y;
        r1 = (float) r1;
        r4[r9] = r1;
        goto L_0x04db;
    L_0x04b0:
        r1 = r5.getLeftEyePosition();
        r1 = r1.x;
        r1 = (float) r1;
        r4[r16] = r1;
        r1 = r5.getLeftEyePosition();
        r1 = r1.y;
        r8 = r2 / 2;
        r1 = r1 - r8;
        r1 = (float) r1;
        r4[r19] = r1;
        r1 = r5.getLeftEyePosition();
        r1 = r1.x;
        r1 = (float) r1;
        r8 = 2;
        r4[r8] = r1;
        r1 = r5.getLeftEyePosition();
        r1 = r1.y;
        r8 = r2 / 2;
        r1 = r1 + r8;
        r1 = (float) r1;
        r4[r9] = r1;
    L_0x04db:
        r14.mapPoints(r4);
        r1 = r0.mMatrix;
        r1.mapPoints(r4);
        r1 = r6.getLeyeBlink();
        r8 = 60;
        if (r1 < r8) goto L_0x051a;
    L_0x04eb:
        r1 = r4[r16];
        r8 = (float) r15;
        r8 = r8 + r1;
        r1 = r4[r19];
        r9 = (float) r13;
        r9 = r9 + r1;
        r1 = 2;
        r20 = r4[r1];
        r1 = (float) r15;
        r20 = r20 + r1;
        r1 = 3;
        r22 = r4[r1];
        r1 = (float) r13;
        r22 = r22 + r1;
        r1 = r0.mPaint;
        r23 = r1;
        r1 = r7;
        r24 = r2;
        r2 = r8;
        r8 = r3;
        r3 = r9;
        r9 = r4;
        r4 = r20;
        r35 = r10;
        r10 = r5;
        r5 = r22;
        r36 = r12;
        r12 = r6;
        r6 = r23;
        r1.drawLine(r2, r3, r4, r5, r6);
        goto L_0x0524;
    L_0x051a:
        r24 = r2;
        r8 = r3;
        r9 = r4;
        r35 = r10;
        r36 = r12;
        r10 = r5;
        r12 = r6;
    L_0x0524:
        r1 = r10.getRightEyePosition();
        if (r1 == 0) goto L_0x05c2;
    L_0x052a:
        r1 = r0.mDisplayRotation;
        if (r1 == 0) goto L_0x0562;
    L_0x052e:
        r1 = r0.mDisplayRotation;
        r6 = 180; // 0xb4 float:2.52E-43 double:8.9E-322;
        if (r1 != r6) goto L_0x0535;
    L_0x0534:
        goto L_0x0564;
    L_0x0535:
        r1 = r10.getRightEyePosition();
        r1 = r1.x;
        r3 = r8 / 2;
        r1 = r1 - r3;
        r1 = (float) r1;
        r9[r16] = r1;
        r1 = r10.getRightEyePosition();
        r1 = r1.y;
        r1 = (float) r1;
        r9[r19] = r1;
        r1 = r10.getRightEyePosition();
        r1 = r1.x;
        r3 = r8 / 2;
        r1 = r1 + r3;
        r1 = (float) r1;
        r2 = 2;
        r9[r2] = r1;
        r1 = r10.getRightEyePosition();
        r1 = r1.y;
        r1 = (float) r1;
        r2 = 3;
        r9[r2] = r1;
        goto L_0x0590;
    L_0x0562:
        r6 = 180; // 0xb4 float:2.52E-43 double:8.9E-322;
    L_0x0564:
        r1 = r10.getRightEyePosition();
        r1 = r1.x;
        r1 = (float) r1;
        r9[r16] = r1;
        r1 = r10.getRightEyePosition();
        r1 = r1.y;
        r2 = r24 / 2;
        r1 = r1 - r2;
        r1 = (float) r1;
        r9[r19] = r1;
        r1 = r10.getRightEyePosition();
        r1 = r1.x;
        r1 = (float) r1;
        r2 = 2;
        r9[r2] = r1;
        r1 = r10.getRightEyePosition();
        r1 = r1.y;
        r2 = r24 / 2;
        r1 = r1 + r2;
        r1 = (float) r1;
        r2 = 3;
        r9[r2] = r1;
    L_0x0590:
        r14.mapPoints(r9);
        r1 = r0.mMatrix;
        r1.mapPoints(r9);
        r1 = r12.getReyeBlink();
        r2 = 60;
        if (r1 < r2) goto L_0x05bf;
    L_0x05a0:
        r1 = r9[r16];
        r2 = (float) r15;
        r2 = r2 + r1;
        r1 = r9[r19];
        r3 = (float) r13;
        r3 = r3 + r1;
        r1 = 2;
        r4 = r9[r1];
        r1 = (float) r15;
        r4 = r4 + r1;
        r1 = 3;
        r5 = r9[r1];
        r1 = (float) r13;
        r5 = r5 + r1;
        r1 = r0.mPaint;
        r20 = r1;
        r1 = r7;
        r21 = r6;
        r6 = r20;
        r1.drawLine(r2, r3, r4, r5, r6);
        goto L_0x05c4;
    L_0x05bf:
        r21 = r6;
        goto L_0x05c4;
    L_0x05c2:
        r21 = 180; // 0xb4 float:2.52E-43 double:8.9E-322;
    L_0x05c4:
        r1 = r12.getLeftrightGaze();
        if (r1 != 0) goto L_0x05df;
    L_0x05ca:
        r1 = r12.getTopbottomGaze();
        if (r1 == 0) goto L_0x05d1;
    L_0x05d0:
        goto L_0x05df;
    L_0x05d1:
        r39 = r8;
        r40 = r11;
        r7 = r14;
        r17 = 270; // 0x10e float:3.78E-43 double:1.334E-321;
        r18 = 90;
        r11 = r9;
        r9 = r10;
        r10 = r13;
        goto L_0x0819;
        r1 = r10.getLeftEyePosition();
        r1 = r1.x;
        r2 = r10.getRightEyePosition();
        r2 = r2.x;
        r1 = r1 - r2;
        r2 = r10.getLeftEyePosition();
        r2 = r2.x;
        r3 = r10.getRightEyePosition();
        r3 = r3.x;
        r2 = r2 - r3;
        r1 = r1 * r2;
        r2 = r10.getLeftEyePosition();
        r2 = r2.y;
        r3 = r10.getRightEyePosition();
        r3 = r3.y;
        r2 = r2 - r3;
        r3 = r10.getLeftEyePosition();
        r3 = r3.y;
        r4 = r10.getRightEyePosition();
        r4 = r4.y;
        r3 = r3 - r4;
        r2 = r2 * r3;
        r1 = r1 + r2;
        r1 = (double) r1;
        r1 = java.lang.Math.sqrt(r1);
        r3 = 4611686018427387904; // 0x4000000000000000 float:0.0 double:2.0;
        r5 = r1 / r3;
        r1 = r12.getLeftrightGaze();
        r1 = -r1;
        r3 = (double) r1;
        r1 = r12.getTopbottomGaze();
        r1 = -r1;
        r1 = (double) r1;
        r22 = 4640537203540230144; // 0x4066800000000000 float:0.0 double:180.0;
        r31 = r3 / r22;
        r37 = 4614256656552045848; // 0x400921fb54442d18 float:3.37028055E12 double:3.141592653589793;
        r39 = r8;
        r7 = r31 * r37;
        r7 = java.lang.Math.sin(r7);
        r7 = -r7;
        r40 = r11;
        r11 = r12.getRollDirection();
        r11 = -r11;
        r42 = r13;
        r41 = r14;
        r13 = (double) r11;
        r13 = r13 / r22;
        r13 = r13 * r37;
        r13 = java.lang.Math.cos(r13);
        r7 = r7 * r13;
        r13 = r1 / r22;
        r13 = r13 * r37;
        r13 = java.lang.Math.sin(r13);
        r31 = r3 / r22;
        r43 = r9;
        r44 = r10;
        r9 = r31 * r37;
        r9 = java.lang.Math.cos(r9);
        r13 = r13 * r9;
        r9 = r12.getRollDirection();
        r9 = -r9;
        r9 = (double) r9;
        r9 = r9 / r22;
        r9 = r9 * r37;
        r9 = java.lang.Math.sin(r9);
        r13 = r13 * r9;
        r7 = r7 + r13;
        r9 = -r5;
        r7 = r7 * r9;
        r9 = 4602678819172646912; // 0x3fe0000000000000 float:0.0 double:0.5;
        r7 = r7 + r9;
        r7 = (float) r7;
        r8 = -r3;
        r8 = r8 / r22;
        r8 = r8 * r37;
        r8 = java.lang.Math.sin(r8);
        r10 = r12.getRollDirection();
        r10 = -r10;
        r10 = (double) r10;
        r10 = r10 / r22;
        r10 = r10 * r37;
        r10 = java.lang.Math.sin(r10);
        r8 = r8 * r10;
        r10 = r1 / r22;
        r10 = r10 * r37;
        r10 = java.lang.Math.sin(r10);
        r13 = r3 / r22;
        r13 = r13 * r37;
        r13 = java.lang.Math.cos(r13);
        r10 = r10 * r13;
        r13 = r12.getRollDirection();
        r13 = -r13;
        r13 = (double) r13;
        r13 = r13 / r22;
        r13 = r13 * r37;
        r13 = java.lang.Math.cos(r13);
        r10 = r10 * r13;
        r8 = r8 - r10;
        r10 = -r5;
        r8 = r8 * r10;
        r10 = 4602678819172646912; // 0x3fe0000000000000 float:0.0 double:0.5;
        r8 = r8 + r10;
        r8 = (float) r8;
        r9 = r12.getLeyeBlink();
        r10 = 60;
        if (r9 >= r10) goto L_0x0769;
    L_0x06c8:
        r9 = r0.mDisplayRotation;
        r10 = 90;
        if (r9 == r10) goto L_0x0702;
    L_0x06ce:
        r9 = r0.mDisplayRotation;
        r10 = 270; // 0x10e float:3.78E-43 double:1.334E-321;
        if (r9 != r10) goto L_0x06d7;
    L_0x06d4:
        r9 = r44;
        goto L_0x0704;
    L_0x06d7:
        r9 = r44;
        r10 = r9.getLeftEyePosition();
        r10 = r10.x;
        r10 = (float) r10;
        r43[r16] = r10;
        r10 = r9.getLeftEyePosition();
        r10 = r10.y;
        r10 = (float) r10;
        r43[r19] = r10;
        r10 = r9.getLeftEyePosition();
        r10 = r10.x;
        r10 = (float) r10;
        r10 = r10 + r8;
        r11 = 2;
        r43[r11] = r10;
        r10 = r9.getLeftEyePosition();
        r10 = r10.y;
        r10 = (float) r10;
        r10 = r10 + r7;
        r11 = 3;
        r43[r11] = r10;
        goto L_0x072c;
    L_0x0702:
        r9 = r44;
    L_0x0704:
        r10 = r9.getLeftEyePosition();
        r10 = r10.x;
        r10 = (float) r10;
        r43[r16] = r10;
        r10 = r9.getLeftEyePosition();
        r10 = r10.y;
        r10 = (float) r10;
        r43[r19] = r10;
        r10 = r9.getLeftEyePosition();
        r10 = r10.x;
        r10 = (float) r10;
        r10 = r10 + r7;
        r11 = 2;
        r43[r11] = r10;
        r10 = r9.getLeftEyePosition();
        r10 = r10.y;
        r10 = (float) r10;
        r10 = r10 + r8;
        r11 = 3;
        r43[r11] = r10;
    L_0x072c:
        r10 = r41;
        r11 = r43;
        r10.mapPoints(r11);
        r13 = r0.mMatrix;
        r13.mapPoints(r11);
        r13 = r11[r16];
        r14 = (float) r15;
        r13 = r13 + r14;
        r14 = r11[r19];
        r46 = r1;
        r45 = r10;
        r10 = r42;
        r1 = (float) r10;
        r14 = r14 + r1;
        r1 = 2;
        r2 = r11[r1];
        r1 = (float) r15;
        r20 = r2 + r1;
        r1 = 3;
        r2 = r11[r1];
        r1 = (float) r10;
        r22 = r2 + r1;
        r2 = r0.mPaint;
        r31 = r46;
        r1 = r50;
        r23 = r2;
        r2 = r13;
        r37 = r3;
        r3 = r14;
        r4 = r20;
        r13 = r5;
        r5 = r22;
        r6 = r23;
        r1.drawLine(r2, r3, r4, r5, r6);
        goto L_0x0776;
    L_0x0769:
        r31 = r1;
        r37 = r3;
        r13 = r5;
        r45 = r41;
        r10 = r42;
        r11 = r43;
        r9 = r44;
    L_0x0776:
        r1 = r12.getReyeBlink();
        r2 = 60;
        if (r1 >= r2) goto L_0x0813;
    L_0x077e:
        r1 = r0.mDisplayRotation;
        r6 = 90;
        if (r1 == r6) goto L_0x07b4;
    L_0x0784:
        r1 = r0.mDisplayRotation;
        r5 = 270; // 0x10e float:3.78E-43 double:1.334E-321;
        if (r1 != r5) goto L_0x078b;
    L_0x078a:
        goto L_0x07b6;
    L_0x078b:
        r1 = r9.getRightEyePosition();
        r1 = r1.x;
        r1 = (float) r1;
        r11[r16] = r1;
        r1 = r9.getRightEyePosition();
        r1 = r1.y;
        r1 = (float) r1;
        r11[r19] = r1;
        r1 = r9.getRightEyePosition();
        r1 = r1.x;
        r1 = (float) r1;
        r1 = r1 + r8;
        r2 = 2;
        r11[r2] = r1;
        r1 = r9.getRightEyePosition();
        r1 = r1.y;
        r1 = (float) r1;
        r1 = r1 + r7;
        r2 = 3;
        r11[r2] = r1;
        goto L_0x07de;
    L_0x07b4:
        r5 = 270; // 0x10e float:3.78E-43 double:1.334E-321;
    L_0x07b6:
        r1 = r9.getRightEyePosition();
        r1 = r1.x;
        r1 = (float) r1;
        r11[r16] = r1;
        r1 = r9.getRightEyePosition();
        r1 = r1.y;
        r1 = (float) r1;
        r11[r19] = r1;
        r1 = r9.getRightEyePosition();
        r1 = r1.x;
        r1 = (float) r1;
        r1 = r1 + r7;
        r2 = 2;
        r11[r2] = r1;
        r1 = r9.getRightEyePosition();
        r1 = r1.y;
        r1 = (float) r1;
        r1 = r1 + r8;
        r2 = 3;
        r11[r2] = r1;
    L_0x07de:
        r4 = r45;
        r4.mapPoints(r11);
        r1 = r0.mMatrix;
        r1.mapPoints(r11);
        r1 = r11[r16];
        r2 = (float) r15;
        r2 = r2 + r1;
        r1 = r11[r19];
        r3 = (float) r10;
        r3 = r3 + r1;
        r1 = 2;
        r17 = r11[r1];
        r1 = (float) r15;
        r17 = r17 + r1;
        r1 = 3;
        r18 = r11[r1];
        r1 = (float) r10;
        r18 = r18 + r1;
        r1 = r0.mPaint;
        r20 = r1;
        r1 = r50;
        r48 = r7;
        r7 = r4;
        r4 = r17;
        r17 = r5;
        r5 = r18;
        r18 = r6;
        r6 = r20;
        r1.drawLine(r2, r3, r4, r5, r6);
        goto L_0x0819;
    L_0x0813:
        r7 = r45;
        r17 = 270; // 0x10e float:3.78E-43 double:1.334E-321;
        r18 = 90;
    L_0x0819:
        r1 = r9.getMouthPosition();
        if (r1 == 0) goto L_0x095b;
    L_0x081f:
        r1 = TAG;
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r3 = "smile: ";
        r2.append(r3);
        r3 = r12.getSmileDegree();
        r2.append(r3);
        r3 = ",";
        r2.append(r3);
        r3 = r12.getSmileConfidence();
        r2.append(r3);
        r2 = r2.toString();
        com.hmdglobal.app.camera.debug.Log.e(r1, r2);
        r1 = r12.getSmileDegree();
        r2 = 30;
        if (r1 >= r2) goto L_0x08be;
    L_0x084d:
        r1 = r9.getMouthPosition();
        r1 = r1.x;
        r1 = r1 + r15;
        r1 = r1 - r39;
        r1 = (float) r1;
        r11[r16] = r1;
        r1 = r9.getMouthPosition();
        r1 = r1.y;
        r1 = (float) r1;
        r11[r19] = r1;
        r1 = r9.getMouthPosition();
        r1 = r1.x;
        r1 = r1 + r15;
        r1 = r1 + r39;
        r1 = (float) r1;
        r2 = 2;
        r11[r2] = r1;
        r1 = r9.getMouthPosition();
        r1 = r1.y;
        r1 = (float) r1;
        r2 = 3;
        r11[r2] = r1;
        r1 = new android.graphics.Matrix;
        r1.<init>();
        r8 = r1;
        r1 = r12.getRollDirection();
        r1 = (float) r1;
        r2 = r9.getMouthPosition();
        r2 = r2.x;
        r2 = (float) r2;
        r3 = r9.getMouthPosition();
        r3 = r3.y;
        r3 = (float) r3;
        r8.preRotate(r1, r2, r3);
        r8.mapPoints(r11);
        r7.mapPoints(r11);
        r1 = r0.mMatrix;
        r1.mapPoints(r11);
        r1 = r11[r16];
        r2 = (float) r15;
        r2 = r2 + r1;
        r1 = r11[r19];
        r3 = (float) r10;
        r3 = r3 + r1;
        r13 = 2;
        r1 = r11[r13];
        r4 = (float) r15;
        r4 = r4 + r1;
        r1 = 3;
        r1 = r11[r1];
        r5 = (float) r10;
        r5 = r5 + r1;
        r6 = r0.mPaint;
        r1 = r50;
        r1.drawLine(r2, r3, r4, r5, r6);
    L_0x08ba:
        r3 = r50;
        goto L_0x096d;
    L_0x08be:
        r13 = 2;
        r1 = r12.getSmileDegree();
        r2 = 60;
        if (r1 >= r2) goto L_0x0915;
    L_0x08c7:
        r1 = r0.mDisplayRotation;
        r8 = 360 - r1;
        r1 = r0.mRect;
        r2 = r9.getMouthPosition();
        r2 = r2.x;
        r2 = r2 - r39;
        r2 = (float) r2;
        r3 = r9.getMouthPosition();
        r3 = r3.y;
        r3 = r3 - r24;
        r3 = (float) r3;
        r4 = r9.getMouthPosition();
        r4 = r4.x;
        r4 = r4 + r39;
        r4 = (float) r4;
        r5 = r9.getMouthPosition();
        r5 = r5.y;
        r5 = r5 + r24;
        r5 = (float) r5;
        r1.set(r2, r3, r4, r5);
        r1 = r0.mRect;
        r7.mapRect(r1);
        r1 = r0.mMatrix;
        r2 = r0.mRect;
        r1.mapRect(r2);
        r1 = r0.mRect;
        r2 = (float) r15;
        r3 = (float) r10;
        r1.offset(r2, r3);
        r2 = r0.mRect;
        r3 = (float) r8;
        r4 = 1127481344; // 0x43340000 float:180.0 double:5.570497984E-315;
        r5 = 1;
        r6 = r0.mPaint;
        r1 = r50;
        r1.drawArc(r2, r3, r4, r5, r6);
        goto L_0x08ba;
    L_0x0915:
        r1 = r0.mRect;
        r2 = r9.getMouthPosition();
        r2 = r2.x;
        r2 = r2 - r39;
        r2 = (float) r2;
        r3 = r9.getMouthPosition();
        r3 = r3.y;
        r3 = r3 - r24;
        r3 = (float) r3;
        r4 = r9.getMouthPosition();
        r4 = r4.x;
        r4 = r4 + r39;
        r4 = (float) r4;
        r5 = r9.getMouthPosition();
        r5 = r5.y;
        r5 = r5 + r24;
        r5 = (float) r5;
        r1.set(r2, r3, r4, r5);
        r1 = r0.mRect;
        r7.mapRect(r1);
        r1 = r0.mMatrix;
        r2 = r0.mRect;
        r1.mapRect(r2);
        r1 = r0.mRect;
        r2 = (float) r15;
        r3 = (float) r10;
        r1.offset(r2, r3);
        r1 = r0.mRect;
        r2 = r0.mPaint;
        r3 = r50;
        r3.drawOval(r1, r2);
        goto L_0x096d;
    L_0x095b:
        r3 = r50;
        r13 = 2;
        goto L_0x096d;
    L_0x095f:
        r3 = r7;
        r40 = r11;
        r36 = r12;
        r10 = r13;
        r7 = r14;
        r13 = 2;
        r17 = 270; // 0x10e float:3.78E-43 double:1.334E-321;
        r18 = 90;
        r21 = 180; // 0xb4 float:2.52E-43 double:8.9E-322;
    L_0x096d:
        r1 = r40 + 1;
        r14 = r7;
        r5 = r10;
        r6 = r15;
        r8 = r17;
        r9 = r18;
        r10 = r21;
        r11 = r27;
        r12 = r28;
        r4 = r36;
        r7 = r3;
        r15 = r13;
        r13 = r29;
        goto L_0x0129;
    L_0x0984:
        r36 = r4;
        r10 = r5;
        r15 = r6;
        r3 = r7;
        r27 = r11;
        r28 = r12;
        r29 = r13;
        r7 = r14;
        r50.restore();
        goto L_0x0995;
    L_0x0994:
        r3 = r7;
    L_0x0995:
        super.onDraw(r50);
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.ui.camera2.Camera2FaceView.onDraw(android.graphics.Canvas):void");
    }

    private void drawExtendedFace(Canvas canvas, Matrix bsgcTranslateMatrix, int dx, int dy, int i, Rect faceBound) {
        int i2;
        int delta_x;
        int delta_y;
        Matrix matrix;
        Matrix matrix2 = bsgcTranslateMatrix;
        int i3 = dx;
        int i4 = dy;
        ExtendedFace exFace = this.mExFaces[i];
        Face face = this.mFaces[i];
        float[] point = new float[4];
        int delta_x2 = (int) (((float) (faceBound.width() / 12)) * this.mZoom);
        int delta_y2 = (int) (((float) (faceBound.height() / 12)) * this.mZoom);
        if (face.getLeftEyePosition() != null) {
            if (this.mDisplayRotation == 0 || this.mDisplayRotation == MediaProviderUtils.ROTATION_180) {
                point[0] = (float) face.getLeftEyePosition().x;
                point[1] = (float) (face.getLeftEyePosition().y - (delta_y2 / 2));
                point[2] = (float) face.getLeftEyePosition().x;
                point[3] = (float) (face.getLeftEyePosition().y + (delta_y2 / 2));
            } else {
                point[0] = (float) (face.getLeftEyePosition().x - (delta_x2 / 2));
                point[1] = (float) face.getLeftEyePosition().y;
                point[2] = (float) (face.getLeftEyePosition().x + (delta_x2 / 2));
                point[3] = (float) face.getLeftEyePosition().y;
            }
            matrix2.mapPoints(point);
            this.mMatrix.mapPoints(point);
            if (exFace.getLeyeBlink() >= 60) {
                canvas.drawLine(point[0] + ((float) i3), point[1] + ((float) i4), point[2] + ((float) i3), point[3] + ((float) i4), this.mPaint);
            }
        }
        if (face.getRightEyePosition() != null) {
            if (this.mDisplayRotation == 0 || this.mDisplayRotation == MediaProviderUtils.ROTATION_180) {
                point[0] = (float) face.getRightEyePosition().x;
                point[1] = (float) (face.getRightEyePosition().y - (delta_y2 / 2));
                point[2] = (float) face.getRightEyePosition().x;
                point[3] = (float) (face.getRightEyePosition().y + (delta_y2 / 2));
            } else {
                point[0] = (float) (face.getRightEyePosition().x - (delta_x2 / 2));
                point[1] = (float) face.getRightEyePosition().y;
                point[2] = (float) (face.getRightEyePosition().x + (delta_x2 / 2));
                point[3] = (float) face.getRightEyePosition().y;
            }
            matrix2.mapPoints(point);
            this.mMatrix.mapPoints(point);
            if (exFace.getReyeBlink() >= 60) {
                canvas.drawLine(point[0] + ((float) i3), point[1] + ((float) i4), point[2] + ((float) i3), point[3] + ((float) i4), this.mPaint);
            }
        }
        if (exFace.getLeftrightGaze() == 0 && exFace.getTopbottomGaze() == 0) {
            i2 = i4;
            delta_x = delta_x2;
            delta_y = delta_y2;
            delta_y2 = i3;
            matrix = matrix2;
        } else {
            double length = Math.sqrt((double) (((face.getLeftEyePosition().x - face.getRightEyePosition().x) * (face.getLeftEyePosition().x - face.getRightEyePosition().x)) + ((face.getLeftEyePosition().y - face.getRightEyePosition().y) * (face.getLeftEyePosition().y - face.getRightEyePosition().y)))) / 2.0d;
            double nGazeYaw = (double) (-exFace.getLeftrightGaze());
            delta_x = delta_x2;
            delta_y = delta_y2;
            double nGazePitch = (double) (-exFace.getTopbottomGaze());
            float gazeRollX = (float) (((((-Math.sin((nGazeYaw / 180.0d) * 3.141592653589793d)) * Math.cos((((double) (-exFace.getRollDirection())) / 180.0d) * 3.141592653589793d)) + ((Math.sin((nGazePitch / 180.0d) * 3.141592653589793d) * Math.cos((nGazeYaw / 180.0d) * 3.141592653589793d)) * Math.sin((((double) (-exFace.getRollDirection())) / 180.0d) * 3.141592653589793d))) * (-length)) + 0.5d);
            float gazeRollY = (float) ((((Math.sin(((-nGazeYaw) / 180.0d) * 3.141592653589793d) * Math.sin((((double) (-exFace.getRollDirection())) / 180.0d) * 3.141592653589793d)) - ((Math.sin((nGazePitch / 180.0d) * 3.141592653589793d) * Math.cos((nGazeYaw / 180.0d) * 3.141592653589793d)) * Math.cos((((double) (-exFace.getRollDirection())) / 180.0d) * 3.141592653589793d))) * (-length)) + 0.5d);
            if (exFace.getLeyeBlink() < 60) {
                if (this.mDisplayRotation == 90 || this.mDisplayRotation == MediaProviderUtils.ROTATION_270) {
                    point[0] = (float) face.getLeftEyePosition().x;
                    point[1] = (float) face.getLeftEyePosition().y;
                    point[2] = ((float) face.getLeftEyePosition().x) + gazeRollX;
                    point[3] = ((float) face.getLeftEyePosition().y) + gazeRollY;
                } else {
                    point[0] = (float) face.getLeftEyePosition().x;
                    point[1] = (float) face.getLeftEyePosition().y;
                    point[2] = ((float) face.getLeftEyePosition().x) + gazeRollY;
                    point[3] = ((float) face.getLeftEyePosition().y) + gazeRollX;
                }
                matrix = bsgcTranslateMatrix;
                matrix.mapPoints(point);
                this.mMatrix.mapPoints(point);
                delta_y2 = dx;
                i2 = dy;
                Canvas canvas2 = canvas;
                canvas2.drawLine(point[0] + ((float) delta_y2), point[1] + ((float) i2), point[2] + ((float) delta_y2), point[3] + ((float) i2), this.mPaint);
            } else {
                matrix = bsgcTranslateMatrix;
                delta_y2 = dx;
                i2 = dy;
            }
            if (exFace.getReyeBlink() < 60) {
                if (this.mDisplayRotation == 90 || this.mDisplayRotation == MediaProviderUtils.ROTATION_270) {
                    point[0] = (float) face.getRightEyePosition().x;
                    point[1] = (float) face.getRightEyePosition().y;
                    point[2] = ((float) face.getRightEyePosition().x) + gazeRollX;
                    point[3] = ((float) face.getRightEyePosition().y) + gazeRollY;
                } else {
                    point[0] = (float) face.getRightEyePosition().x;
                    point[1] = (float) face.getRightEyePosition().y;
                    point[2] = ((float) face.getRightEyePosition().x) + gazeRollY;
                    point[3] = ((float) face.getRightEyePosition().y) + gazeRollX;
                }
                matrix.mapPoints(point);
                this.mMatrix.mapPoints(point);
                canvas.drawLine(point[0] + ((float) delta_y2), point[1] + ((float) i2), point[2] + ((float) delta_y2), point[3] + ((float) i2), this.mPaint);
            }
        }
        if (face.getMouthPosition() != null) {
            if (exFace.getSmileDegree() < 30) {
                point[0] = (float) ((face.getMouthPosition().x + delta_y2) - delta_x);
                point[1] = (float) face.getMouthPosition().y;
                point[2] = (float) ((face.getMouthPosition().x + delta_y2) + delta_x);
                point[3] = (float) face.getMouthPosition().y;
                matrix2 = new Matrix();
                matrix2.preRotate((float) exFace.getRollDirection(), (float) face.getMouthPosition().x, (float) face.getMouthPosition().y);
                matrix2.mapPoints(point);
                matrix.mapPoints(point);
                this.mMatrix.mapPoints(point);
                canvas.drawLine(point[0] + ((float) delta_y2), point[1] + ((float) i2), point[2] + ((float) delta_y2), point[3] + ((float) i2), this.mPaint);
            } else if (exFace.getSmileDegree() < 60) {
                int rotation_mouth = 360 - this.mDisplayRotation;
                this.mRect.set((float) (face.getMouthPosition().x - delta_x), (float) (face.getMouthPosition().y - delta_y), (float) (face.getMouthPosition().x + delta_x), (float) (face.getMouthPosition().y + delta_y));
                matrix.mapRect(this.mRect);
                this.mMatrix.mapRect(this.mRect);
                this.mRect.offset((float) delta_y2, (float) i2);
                canvas.drawArc(this.mRect, (float) rotation_mouth, 180.0f, true, this.mPaint);
            } else {
                this.mRect.set((float) (face.getMouthPosition().x - delta_x), (float) (face.getMouthPosition().y - delta_y), (float) (face.getMouthPosition().x + delta_x), (float) (face.getMouthPosition().y + delta_y));
                matrix.mapRect(this.mRect);
                this.mMatrix.mapRect(this.mRect);
                this.mRect.offset((float) delta_y2, (float) i2);
                canvas.drawOval(this.mRect, this.mPaint);
                return;
            }
        }
        Canvas canvas3 = canvas;
    }

    public void clear() {
        this.mColor = this.mFocusingColor;
        this.mFaces = null;
        this.mExFaces = null;
        invalidate();
    }
}
