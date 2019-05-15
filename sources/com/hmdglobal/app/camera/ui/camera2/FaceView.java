package com.hmdglobal.app.camera.ui.camera2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.hardware.Camera.Face;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.ui.FocusIndicator;
import com.hmdglobal.app.camera.ui.Rotatable;

public class FaceView extends View implements FocusIndicator, Rotatable {
    protected static final int MSG_SWITCH_FACES = 1;
    protected static final int SWITCH_DELAY = 70;
    private static final Tag TAG = new Tag("FaceView");
    protected final boolean LOGV = false;
    private final int blink_threshold = 60;
    protected volatile boolean mBlocked;
    protected int mColor;
    protected int mDisplayOrientation;
    protected int mDisplayRotation = 0;
    private Face[] mFaces;
    private final int mFailColor;
    private final int mFocusedColor;
    protected final int mFocusingColor;
    @SuppressLint({"HandlerLeak"})
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                FaceView.this.mStateSwitchPending = false;
                FaceView.this.mFaces = FaceView.this.mPendingFaces;
                FaceView.this.invalidate();
            }
        }
    };
    protected Matrix mMatrix = new Matrix();
    protected boolean mMirror;
    protected int mOrientation;
    protected Paint mPaint;
    protected boolean mPause;
    private Face[] mPendingFaces;
    protected RectF mRect = new RectF();
    protected boolean mStateSwitchPending = false;
    protected int mUncroppedHeight;
    protected int mUncroppedWidth;
    private final int smile_threashold_no_smile = 30;
    private final int smile_threashold_small_smile = 60;

    public FaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources res = getResources();
        this.mFocusingColor = res.getColor(R.color.face_detect_start);
        this.mFocusedColor = res.getColor(R.color.face_detect_success);
        this.mFailColor = res.getColor(R.color.face_detect_fail);
        this.mColor = this.mFocusingColor;
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);
        this.mPaint.setStyle(Style.STROKE);
        this.mPaint.setStrokeWidth(res.getDimension(R.dimen.face_circle_stroke));
        this.mPaint.setDither(true);
        this.mPaint.setColor(-1);
        this.mPaint.setStrokeCap(Cap.ROUND);
    }

    public void onPreviewLayoutChanged(int uncroppedWidth, int uncroppedHeight) {
    }

    public void onPreviewAreaChanged(RectF previewArea) {
        this.mUncroppedWidth = (int) previewArea.width();
        this.mUncroppedHeight = (int) previewArea.height();
    }

    public void setFaces(Face[] faces) {
        if (!this.mPause) {
            if (this.mFaces == null || ((faces.length <= 0 || this.mFaces.length != 0) && (faces.length != 0 || this.mFaces.length <= 0))) {
                if (this.mStateSwitchPending) {
                    this.mStateSwitchPending = false;
                    this.mHandler.removeMessages(1);
                }
                this.mFaces = faces;
                if (!(this.mBlocked || this.mFaces == null || this.mFaces.length <= 0)) {
                    invalidate();
                }
                return;
            }
            this.mPendingFaces = faces;
            if (!this.mStateSwitchPending) {
                this.mStateSwitchPending = true;
                this.mHandler.sendEmptyMessageDelayed(1, 70);
            }
        }
    }

    public void setDisplayOrientation(int orientation) {
        this.mDisplayOrientation = orientation;
    }

    public void setOrientation(int orientation, boolean animation) {
        this.mOrientation = orientation;
        invalidate();
    }

    public void setMirror(boolean mirror) {
        this.mMirror = mirror;
    }

    public boolean faceExists() {
        return this.mFaces != null && this.mFaces.length > 0;
    }

    public void showStart() {
        this.mColor = this.mFocusingColor;
        invalidate();
    }

    public void showSuccess(boolean timeout) {
        this.mColor = this.mFocusedColor;
        invalidate();
    }

    public void showFail(boolean timeout) {
        this.mColor = this.mFailColor;
        invalidate();
    }

    public void clear() {
        this.mColor = this.mFocusingColor;
        this.mFaces = null;
        invalidate();
    }

    public void pause() {
        Log.d("20190111", "FaceView.pause");
        this.mPause = true;
        setVisibility(4);
    }

    public void resume() {
        Log.d("20190111", "FaceView.resume");
        this.mPause = false;
        setVisibility(0);
    }

    public void setBlockDraw(boolean block) {
        this.mBlocked = block;
    }

    public void setDisplayRotation(int orientation) {
        this.mDisplayRotation = orientation;
    }

    /* Access modifiers changed, original: protected */
    /* JADX WARNING: Removed duplicated region for block: B:84:0x05aa  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x0485  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x0485  */
    /* JADX WARNING: Removed duplicated region for block: B:84:0x05aa  */
    /* JADX WARNING: Removed duplicated region for block: B:40:0x0179  */
    /* JADX WARNING: Removed duplicated region for block: B:64:0x03dd  */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x0341  */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x03f3  */
    /* JADX WARNING: Removed duplicated region for block: B:84:0x05aa  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x0485  */
    public void onDraw(android.graphics.Canvas r38) {
        /*
        r37 = this;
        r0 = r37;
        r7 = r38;
        r1 = r0.mBlocked;
        if (r1 != 0) goto L_0x05ce;
    L_0x0008:
        r1 = r0.mFaces;
        if (r1 == 0) goto L_0x05ce;
    L_0x000c:
        r1 = r0.mFaces;
        r1 = r1.length;
        if (r1 <= 0) goto L_0x05ce;
    L_0x0011:
        r1 = r0.mUncroppedWidth;
        r2 = r0.mUncroppedHeight;
        r8 = 270; // 0x10e float:3.78E-43 double:1.334E-321;
        r9 = 90;
        r10 = 180; // 0xb4 float:2.52E-43 double:8.9E-322;
        if (r2 <= r1) goto L_0x0025;
    L_0x001d:
        r3 = r0.mDisplayOrientation;
        if (r3 == 0) goto L_0x002f;
    L_0x0021:
        r3 = r0.mDisplayOrientation;
        if (r3 == r10) goto L_0x002f;
    L_0x0025:
        if (r1 <= r2) goto L_0x0032;
    L_0x0027:
        r3 = r0.mDisplayOrientation;
        if (r3 == r9) goto L_0x002f;
    L_0x002b:
        r3 = r0.mDisplayOrientation;
        if (r3 != r8) goto L_0x0032;
    L_0x002f:
        r3 = r1;
        r1 = r2;
        r2 = r3;
    L_0x0032:
        r11 = r1;
        r12 = r2;
        r1 = r0.mMatrix;
        r2 = r0.mMirror;
        r3 = r0.mDisplayOrientation;
        com.hmdglobal.app.camera.util.CameraUtil.prepareMatrix(r1, r2, r3, r11, r12);
        r1 = r37.getWidth();
        r1 = r1 - r11;
        r13 = 2;
        r14 = r1 / 2;
        r1 = r37.getHeight();
        r1 = r1 - r12;
        r15 = r1 / 2;
        r38.save();
        r1 = r0.mMatrix;
        r2 = r0.mOrientation;
        r2 = (float) r2;
        r1.postRotate(r2);
        r1 = r0.mOrientation;
        r1 = -r1;
        r1 = (float) r1;
        r7.rotate(r1);
        r16 = 0;
        r1 = r16;
    L_0x0062:
        r6 = r1;
        r1 = r0.mFaces;
        r1 = r1.length;
        if (r6 >= r1) goto L_0x05c5;
    L_0x0068:
        r1 = r0.mFaces;
        r1 = r1[r6];
        r1 = r1.score;
        r2 = 50;
        if (r1 >= r2) goto L_0x007f;
        r29 = r11;
        r30 = r12;
        r11 = r13;
        r12 = r14;
        r13 = r15;
        r15 = r8;
        r14 = r9;
        r8 = r6;
        goto L_0x05b6;
    L_0x007f:
        r1 = r0.mRect;
        r2 = r0.mFaces;
        r2 = r2[r6];
        r2 = r2.rect;
        r1.set(r2);
        r1 = r0.mMatrix;
        r2 = r0.mRect;
        r1.mapRect(r2);
        r1 = r0.mRect;
        r2 = (float) r14;
        r3 = (float) r15;
        r1.offset(r2, r3);
        r1 = r0.mRect;
        r2 = r0.mPaint;
        r7.drawOval(r1, r2);
        r1 = r0.mFaces;
        r1 = r1[r6];
        r1 = com.hmdglobal.app.camera.ui.camera2.ExtendedFaceWrapper.isExtendedFaceInstance(r1);
        if (r1 == 0) goto L_0x05ac;
    L_0x00a9:
        r1 = 4;
        r5 = new float[r1];
        r1 = r0.mFaces;
        r1 = r1[r6];
        r1 = r1.rect;
        r1 = r1.width();
        r17 = r1 / 12;
        r1 = r0.mFaces;
        r1 = r1[r6];
        r1 = r1.rect;
        r1 = r1.height();
        r18 = r1 / 12;
        r1 = r0.mFaces;
        r1 = r1[r6];
        r1 = r1.leftEye;
        r4 = 60;
        r19 = 3;
        r20 = 1;
        if (r1 == 0) goto L_0x016f;
    L_0x00d2:
        r1 = r0.mDisplayRotation;
        if (r1 == 0) goto L_0x010e;
    L_0x00d6:
        r1 = r0.mDisplayRotation;
        if (r1 != r10) goto L_0x00db;
    L_0x00da:
        goto L_0x010e;
    L_0x00db:
        r1 = r0.mFaces;
        r1 = r1[r6];
        r1 = r1.leftEye;
        r1 = r1.x;
        r2 = r17 / 2;
        r1 = r1 - r2;
        r1 = (float) r1;
        r5[r16] = r1;
        r1 = r0.mFaces;
        r1 = r1[r6];
        r1 = r1.leftEye;
        r1 = r1.y;
        r1 = (float) r1;
        r5[r20] = r1;
        r1 = r0.mFaces;
        r1 = r1[r6];
        r1 = r1.leftEye;
        r1 = r1.x;
        r2 = r17 / 2;
        r1 = r1 + r2;
        r1 = (float) r1;
        r5[r13] = r1;
        r1 = r0.mFaces;
        r1 = r1[r6];
        r1 = r1.leftEye;
        r1 = r1.y;
        r1 = (float) r1;
        r5[r19] = r1;
        goto L_0x0140;
    L_0x010e:
        r1 = r0.mFaces;
        r1 = r1[r6];
        r1 = r1.leftEye;
        r1 = r1.x;
        r1 = (float) r1;
        r5[r16] = r1;
        r1 = r0.mFaces;
        r1 = r1[r6];
        r1 = r1.leftEye;
        r1 = r1.y;
        r2 = r18 / 2;
        r1 = r1 - r2;
        r1 = (float) r1;
        r5[r20] = r1;
        r1 = r0.mFaces;
        r1 = r1[r6];
        r1 = r1.leftEye;
        r1 = r1.x;
        r1 = (float) r1;
        r5[r13] = r1;
        r1 = r0.mFaces;
        r1 = r1[r6];
        r1 = r1.leftEye;
        r1 = r1.y;
        r2 = r18 / 2;
        r1 = r1 + r2;
        r1 = (float) r1;
        r5[r19] = r1;
    L_0x0140:
        r1 = r0.mMatrix;
        r1.mapPoints(r5);
        r1 = r0.mFaces;
        r1 = r1[r6];
        r1 = com.hmdglobal.app.camera.ui.camera2.ExtendedFaceWrapper.getLeftEyeBlinkDegree(r1);
        if (r1 < r4) goto L_0x016f;
    L_0x014f:
        r1 = r5[r16];
        r2 = (float) r14;
        r2 = r2 + r1;
        r1 = r5[r20];
        r3 = (float) r15;
        r3 = r3 + r1;
        r1 = r5[r13];
        r4 = (float) r14;
        r4 = r4 + r1;
        r1 = r5[r19];
        r8 = (float) r15;
        r8 = r8 + r1;
        r1 = r0.mPaint;
        r22 = r1;
        r1 = r7;
        r9 = 60;
        r9 = r5;
        r5 = r8;
        r8 = r6;
        r6 = r22;
        r1.drawLine(r2, r3, r4, r5, r6);
        goto L_0x0171;
    L_0x016f:
        r9 = r5;
        r8 = r6;
    L_0x0171:
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.rightEye;
        if (r1 == 0) goto L_0x020e;
    L_0x0179:
        r1 = r0.mDisplayRotation;
        if (r1 == 0) goto L_0x01b5;
    L_0x017d:
        r1 = r0.mDisplayRotation;
        if (r1 != r10) goto L_0x0182;
    L_0x0181:
        goto L_0x01b5;
    L_0x0182:
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.rightEye;
        r1 = r1.x;
        r2 = r17 / 2;
        r1 = r1 - r2;
        r1 = (float) r1;
        r9[r16] = r1;
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.rightEye;
        r1 = r1.y;
        r1 = (float) r1;
        r9[r20] = r1;
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.rightEye;
        r1 = r1.x;
        r2 = r17 / 2;
        r1 = r1 + r2;
        r1 = (float) r1;
        r9[r13] = r1;
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.rightEye;
        r1 = r1.y;
        r1 = (float) r1;
        r9[r19] = r1;
        goto L_0x01e7;
    L_0x01b5:
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.rightEye;
        r1 = r1.x;
        r1 = (float) r1;
        r9[r16] = r1;
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.rightEye;
        r1 = r1.y;
        r2 = r18 / 2;
        r1 = r1 - r2;
        r1 = (float) r1;
        r9[r20] = r1;
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.rightEye;
        r1 = r1.x;
        r1 = (float) r1;
        r9[r13] = r1;
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.rightEye;
        r1 = r1.y;
        r2 = r18 / 2;
        r1 = r1 + r2;
        r1 = (float) r1;
        r9[r19] = r1;
    L_0x01e7:
        r1 = r0.mMatrix;
        r1.mapPoints(r9);
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = com.hmdglobal.app.camera.ui.camera2.ExtendedFaceWrapper.getRightEyeBlinkDegree(r1);
        r2 = 60;
        if (r1 < r2) goto L_0x020e;
    L_0x01f8:
        r1 = r9[r16];
        r2 = (float) r14;
        r2 = r2 + r1;
        r1 = r9[r20];
        r3 = (float) r15;
        r3 = r3 + r1;
        r1 = r9[r13];
        r4 = (float) r14;
        r4 = r4 + r1;
        r1 = r9[r19];
        r5 = (float) r15;
        r5 = r5 + r1;
        r6 = r0.mPaint;
        r1 = r7;
        r1.drawLine(r2, r3, r4, r5, r6);
    L_0x020e:
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = com.hmdglobal.app.camera.ui.camera2.ExtendedFaceWrapper.getLeftRightGazeDegree(r1);
        if (r1 != 0) goto L_0x022f;
    L_0x0218:
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = com.hmdglobal.app.camera.ui.camera2.ExtendedFaceWrapper.getTopBottomGazeDegree(r1);
        if (r1 == 0) goto L_0x0223;
    L_0x0222:
        goto L_0x022f;
    L_0x0223:
        r29 = r11;
        r30 = r12;
        r12 = r14;
        r13 = r15;
    L_0x0229:
        r14 = 90;
        r15 = 270; // 0x10e float:3.78E-43 double:1.334E-321;
        goto L_0x047d;
    L_0x022f:
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.leftEye;
        r1 = r1.x;
        r2 = r0.mFaces;
        r2 = r2[r8];
        r2 = r2.rightEye;
        r2 = r2.x;
        r1 = r1 - r2;
        r2 = r0.mFaces;
        r2 = r2[r8];
        r2 = r2.leftEye;
        r2 = r2.x;
        r3 = r0.mFaces;
        r3 = r3[r8];
        r3 = r3.rightEye;
        r3 = r3.x;
        r2 = r2 - r3;
        r1 = r1 * r2;
        r2 = r0.mFaces;
        r2 = r2[r8];
        r2 = r2.leftEye;
        r2 = r2.y;
        r3 = r0.mFaces;
        r3 = r3[r8];
        r3 = r3.rightEye;
        r3 = r3.y;
        r2 = r2 - r3;
        r3 = r0.mFaces;
        r3 = r3[r8];
        r3 = r3.leftEye;
        r3 = r3.y;
        r4 = r0.mFaces;
        r4 = r4[r8];
        r4 = r4.rightEye;
        r4 = r4.y;
        r3 = r3 - r4;
        r2 = r2 * r3;
        r1 = r1 + r2;
        r1 = (double) r1;
        r1 = java.lang.Math.sqrt(r1);
        r3 = 4611686018427387904; // 0x4000000000000000 float:0.0 double:2.0;
        r5 = r1 / r3;
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = com.hmdglobal.app.camera.ui.camera2.ExtendedFaceWrapper.getLeftRightGazeDegree(r1);
        r1 = -r1;
        r3 = (double) r1;
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = com.hmdglobal.app.camera.ui.camera2.ExtendedFaceWrapper.getTopBottomGazeDegree(r1);
        r1 = -r1;
        r1 = (double) r1;
        r23 = 4640537203540230144; // 0x4066800000000000 float:0.0 double:180.0;
        r25 = r3 / r23;
        r27 = 4614256656552045848; // 0x400921fb54442d18 float:3.37028055E12 double:3.141592653589793;
        r29 = r11;
        r10 = r25 * r27;
        r10 = java.lang.Math.sin(r10);
        r10 = -r10;
        r13 = r0.mFaces;
        r13 = r13[r8];
        r13 = com.hmdglobal.app.camera.ui.camera2.ExtendedFaceWrapper.getRollDirection(r13);
        r13 = -r13;
        r30 = r12;
        r12 = (double) r13;
        r12 = r12 / r23;
        r12 = r12 * r27;
        r12 = java.lang.Math.cos(r12);
        r10 = r10 * r12;
        r12 = r1 / r23;
        r12 = r12 * r27;
        r12 = java.lang.Math.sin(r12);
        r25 = r3 / r23;
        r31 = r14;
        r32 = r15;
        r14 = r25 * r27;
        r14 = java.lang.Math.cos(r14);
        r12 = r12 * r14;
        r14 = r0.mFaces;
        r14 = r14[r8];
        r14 = com.hmdglobal.app.camera.ui.camera2.ExtendedFaceWrapper.getRollDirection(r14);
        r14 = -r14;
        r14 = (double) r14;
        r14 = r14 / r23;
        r14 = r14 * r27;
        r14 = java.lang.Math.sin(r14);
        r12 = r12 * r14;
        r10 = r10 + r12;
        r12 = -r5;
        r10 = r10 * r12;
        r12 = 4602678819172646912; // 0x3fe0000000000000 float:0.0 double:0.5;
        r10 = r10 + r12;
        r10 = (float) r10;
        r14 = -r3;
        r14 = r14 / r23;
        r14 = r14 * r27;
        r14 = java.lang.Math.sin(r14);
        r11 = r0.mFaces;
        r11 = r11[r8];
        r11 = com.hmdglobal.app.camera.ui.camera2.ExtendedFaceWrapper.getRollDirection(r11);
        r11 = -r11;
        r12 = (double) r11;
        r12 = r12 / r23;
        r12 = r12 * r27;
        r11 = java.lang.Math.sin(r12);
        r14 = r14 * r11;
        r11 = r1 / r23;
        r11 = r11 * r27;
        r11 = java.lang.Math.sin(r11);
        r25 = r3 / r23;
        r33 = r1;
        r1 = r25 * r27;
        r1 = java.lang.Math.cos(r1);
        r11 = r11 * r1;
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = com.hmdglobal.app.camera.ui.camera2.ExtendedFaceWrapper.getRollDirection(r1);
        r1 = -r1;
        r1 = (double) r1;
        r1 = r1 / r23;
        r1 = r1 * r27;
        r1 = java.lang.Math.cos(r1);
        r11 = r11 * r1;
        r14 = r14 - r11;
        r1 = -r5;
        r14 = r14 * r1;
        r1 = 4602678819172646912; // 0x3fe0000000000000 float:0.0 double:0.5;
        r14 = r14 + r1;
        r11 = (float) r14;
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = com.hmdglobal.app.camera.ui.camera2.ExtendedFaceWrapper.getLeftEyeBlinkDegree(r1);
        r2 = 60;
        if (r1 >= r2) goto L_0x03dd;
    L_0x0341:
        r1 = r0.mDisplayRotation;
        r2 = 90;
        if (r1 == r2) goto L_0x037e;
    L_0x0347:
        r1 = r0.mDisplayRotation;
        r2 = 270; // 0x10e float:3.78E-43 double:1.334E-321;
        if (r1 != r2) goto L_0x034e;
    L_0x034d:
        goto L_0x037e;
    L_0x034e:
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.leftEye;
        r1 = r1.x;
        r1 = (float) r1;
        r9[r16] = r1;
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.leftEye;
        r1 = r1.y;
        r1 = (float) r1;
        r9[r20] = r1;
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.leftEye;
        r1 = r1.x;
        r1 = (float) r1;
        r1 = r1 + r11;
        r2 = 2;
        r9[r2] = r1;
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.leftEye;
        r1 = r1.y;
        r1 = (float) r1;
        r1 = r1 + r10;
        r9[r19] = r1;
        goto L_0x03ad;
    L_0x037e:
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.leftEye;
        r1 = r1.x;
        r1 = (float) r1;
        r9[r16] = r1;
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.leftEye;
        r1 = r1.y;
        r1 = (float) r1;
        r9[r20] = r1;
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.leftEye;
        r1 = r1.x;
        r1 = (float) r1;
        r1 = r1 + r10;
        r2 = 2;
        r9[r2] = r1;
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.leftEye;
        r1 = r1.y;
        r1 = (float) r1;
        r1 = r1 + r11;
        r9[r19] = r1;
    L_0x03ad:
        r1 = r0.mMatrix;
        r1.mapPoints(r9);
        r1 = r9[r16];
        r12 = r31;
        r2 = (float) r12;
        r2 = r2 + r1;
        r1 = r9[r20];
        r13 = r32;
        r14 = (float) r13;
        r14 = r14 + r1;
        r1 = 2;
        r15 = r9[r1];
        r1 = (float) r12;
        r15 = r15 + r1;
        r1 = r9[r19];
        r35 = r3;
        r3 = (float) r13;
        r21 = r1 + r3;
        r4 = r0.mPaint;
        r23 = r33;
        r1 = r7;
        r25 = r35;
        r3 = r14;
        r14 = r4;
        r4 = r15;
        r27 = r5;
        r5 = r21;
        r6 = r14;
        r1.drawLine(r2, r3, r4, r5, r6);
        goto L_0x03e7;
    L_0x03dd:
        r25 = r3;
        r27 = r5;
        r12 = r31;
        r13 = r32;
        r23 = r33;
    L_0x03e7:
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = com.hmdglobal.app.camera.ui.camera2.ExtendedFaceWrapper.getRightEyeBlinkDegree(r1);
        r2 = 60;
        if (r1 >= r2) goto L_0x0229;
    L_0x03f3:
        r1 = r0.mDisplayRotation;
        r14 = 90;
        if (r1 == r14) goto L_0x0430;
    L_0x03f9:
        r1 = r0.mDisplayRotation;
        r15 = 270; // 0x10e float:3.78E-43 double:1.334E-321;
        if (r1 != r15) goto L_0x0400;
    L_0x03ff:
        goto L_0x0432;
    L_0x0400:
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.rightEye;
        r1 = r1.x;
        r1 = (float) r1;
        r9[r16] = r1;
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.rightEye;
        r1 = r1.y;
        r1 = (float) r1;
        r9[r20] = r1;
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.rightEye;
        r1 = r1.x;
        r1 = (float) r1;
        r1 = r1 + r11;
        r2 = 2;
        r9[r2] = r1;
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.rightEye;
        r1 = r1.y;
        r1 = (float) r1;
        r1 = r1 + r10;
        r9[r19] = r1;
        goto L_0x0461;
    L_0x0430:
        r15 = 270; // 0x10e float:3.78E-43 double:1.334E-321;
    L_0x0432:
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.rightEye;
        r1 = r1.x;
        r1 = (float) r1;
        r9[r16] = r1;
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.rightEye;
        r1 = r1.y;
        r1 = (float) r1;
        r9[r20] = r1;
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.rightEye;
        r1 = r1.x;
        r1 = (float) r1;
        r1 = r1 + r10;
        r2 = 2;
        r9[r2] = r1;
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.rightEye;
        r1 = r1.y;
        r1 = (float) r1;
        r1 = r1 + r11;
        r9[r19] = r1;
    L_0x0461:
        r1 = r0.mMatrix;
        r1.mapPoints(r9);
        r1 = r9[r16];
        r2 = (float) r12;
        r2 = r2 + r1;
        r1 = r9[r20];
        r3 = (float) r13;
        r3 = r3 + r1;
        r1 = 2;
        r4 = r9[r1];
        r1 = (float) r12;
        r4 = r4 + r1;
        r1 = r9[r19];
        r5 = (float) r13;
        r5 = r5 + r1;
        r6 = r0.mPaint;
        r1 = r7;
        r1.drawLine(r2, r3, r4, r5, r6);
    L_0x047d:
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.mouth;
        if (r1 == 0) goto L_0x05aa;
    L_0x0485:
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = com.hmdglobal.app.camera.ui.camera2.ExtendedFaceWrapper.getSmileDegree(r1);
        r2 = 30;
        if (r1 >= r2) goto L_0x0506;
    L_0x0491:
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.mouth;
        r1 = r1.x;
        r1 = r1 + r12;
        r1 = r1 - r17;
        r1 = (float) r1;
        r9[r16] = r1;
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.mouth;
        r1 = r1.y;
        r1 = (float) r1;
        r9[r20] = r1;
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.mouth;
        r1 = r1.x;
        r1 = r1 + r12;
        r1 = r1 + r17;
        r1 = (float) r1;
        r2 = 2;
        r9[r2] = r1;
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = r1.mouth;
        r1 = r1.y;
        r1 = (float) r1;
        r9[r19] = r1;
        r1 = new android.graphics.Matrix;
        r2 = r0.mMatrix;
        r1.<init>(r2);
        r10 = r1;
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = com.hmdglobal.app.camera.ui.camera2.ExtendedFaceWrapper.getRollDirection(r1);
        r1 = (float) r1;
        r2 = r0.mFaces;
        r2 = r2[r8];
        r2 = r2.mouth;
        r2 = r2.x;
        r2 = (float) r2;
        r3 = r0.mFaces;
        r3 = r3[r8];
        r3 = r3.mouth;
        r3 = r3.y;
        r3 = (float) r3;
        r10.preRotate(r1, r2, r3);
        r10.mapPoints(r9);
        r1 = r9[r16];
        r2 = (float) r12;
        r2 = r2 + r1;
        r1 = r9[r20];
        r3 = (float) r13;
        r3 = r3 + r1;
        r11 = 2;
        r1 = r9[r11];
        r4 = (float) r12;
        r4 = r4 + r1;
        r1 = r9[r19];
        r5 = (float) r13;
        r5 = r5 + r1;
        r6 = r0.mPaint;
        r1 = r7;
        r1.drawLine(r2, r3, r4, r5, r6);
        goto L_0x05b6;
    L_0x0506:
        r11 = 2;
        r1 = r0.mFaces;
        r1 = r1[r8];
        r1 = com.hmdglobal.app.camera.ui.camera2.ExtendedFaceWrapper.getSmileDegree(r1);
        r2 = 60;
        if (r1 >= r2) goto L_0x0563;
    L_0x0513:
        r1 = r0.mDisplayRotation;
        r10 = 360 - r1;
        r1 = r0.mRect;
        r2 = r0.mFaces;
        r2 = r2[r8];
        r2 = r2.mouth;
        r2 = r2.x;
        r2 = r2 - r17;
        r2 = (float) r2;
        r3 = r0.mFaces;
        r3 = r3[r8];
        r3 = r3.mouth;
        r3 = r3.y;
        r3 = r3 - r18;
        r3 = (float) r3;
        r4 = r0.mFaces;
        r4 = r4[r8];
        r4 = r4.mouth;
        r4 = r4.x;
        r4 = r4 + r17;
        r4 = (float) r4;
        r5 = r0.mFaces;
        r5 = r5[r8];
        r5 = r5.mouth;
        r5 = r5.y;
        r5 = r5 + r18;
        r5 = (float) r5;
        r1.set(r2, r3, r4, r5);
        r1 = r0.mMatrix;
        r2 = r0.mRect;
        r1.mapRect(r2);
        r1 = r0.mRect;
        r2 = (float) r12;
        r3 = (float) r13;
        r1.offset(r2, r3);
        r2 = r0.mRect;
        r3 = (float) r10;
        r4 = 1127481344; // 0x43340000 float:180.0 double:5.570497984E-315;
        r5 = 1;
        r6 = r0.mPaint;
        r1 = r7;
        r1.drawArc(r2, r3, r4, r5, r6);
        goto L_0x05b6;
    L_0x0563:
        r1 = r0.mRect;
        r2 = r0.mFaces;
        r2 = r2[r8];
        r2 = r2.mouth;
        r2 = r2.x;
        r2 = r2 - r17;
        r2 = (float) r2;
        r3 = r0.mFaces;
        r3 = r3[r8];
        r3 = r3.mouth;
        r3 = r3.y;
        r3 = r3 - r18;
        r3 = (float) r3;
        r4 = r0.mFaces;
        r4 = r4[r8];
        r4 = r4.mouth;
        r4 = r4.x;
        r4 = r4 + r17;
        r4 = (float) r4;
        r5 = r0.mFaces;
        r5 = r5[r8];
        r5 = r5.mouth;
        r5 = r5.y;
        r5 = r5 + r18;
        r5 = (float) r5;
        r1.set(r2, r3, r4, r5);
        r1 = r0.mMatrix;
        r2 = r0.mRect;
        r1.mapRect(r2);
        r1 = r0.mRect;
        r2 = (float) r12;
        r3 = (float) r13;
        r1.offset(r2, r3);
        r1 = r0.mRect;
        r2 = r0.mPaint;
        r7.drawOval(r1, r2);
        goto L_0x05b6;
    L_0x05aa:
        r11 = 2;
        goto L_0x05b6;
    L_0x05ac:
        r29 = r11;
        r30 = r12;
        r11 = r13;
        r12 = r14;
        r13 = r15;
        r15 = r8;
        r14 = r9;
        r8 = r6;
    L_0x05b6:
        r1 = r8 + 1;
        r9 = r14;
        r8 = r15;
        r10 = 180; // 0xb4 float:2.52E-43 double:8.9E-322;
        r14 = r12;
        r15 = r13;
        r12 = r30;
        r13 = r11;
        r11 = r29;
        goto L_0x0062;
    L_0x05c5:
        r29 = r11;
        r30 = r12;
        r12 = r14;
        r13 = r15;
        r38.restore();
    L_0x05ce:
        super.onDraw(r38);
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.ui.camera2.FaceView.onDraw(android.graphics.Canvas):void");
    }
}
