package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.NinePatch;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.hardware.Camera.Face;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.ui.PreviewStatusListener.PreviewAreaChangedListener;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.morphoinc.utils.multimedia.MediaProviderUtils;

public class FaceView extends View implements FocusIndicator, Rotatable, PreviewAreaChangedListener {
    private static final int MSG_SWITCH_FACES = 1;
    private static final int SWITCH_DELAY = 70;
    private static final Tag TAG = new Tag("FaceView");
    private final Boolean DRAW_SQUARE = Boolean.valueOf(false);
    private final boolean LOGV = false;
    private volatile boolean mBlocked;
    private int mColor;
    private int mDisplayOrientation;
    private final NinePatch mFaceBound;
    private Face[] mFaces;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                FaceView.this.mStateSwitchPending = false;
                FaceView.this.mFaces = FaceView.this.mPendingFaces;
                FaceView.this.invalidate();
            }
        }
    };
    private Matrix mMatrix = new Matrix();
    private boolean mMirror;
    private int mOrientation;
    private Paint mPaint;
    private boolean mPause;
    private Face[] mPendingFaces;
    private final RectF mPreviewArea = new RectF();
    private RectF mRect = new RectF();
    private boolean mStateSwitchPending = false;

    public FaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources res = getResources();
        this.mColor = res.getColor(R.color.face_detect_start);
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);
        this.mPaint.setStyle(Style.STROKE);
        this.mPaint.setStrokeWidth(res.getDimension(R.dimen.face_circle_stroke));
        Bitmap bmp = BitmapFactory.decodeResource(res, R.drawable.ic_focus_face);
        this.mFaceBound = new NinePatch(bmp, bmp.getNinePatchChunk(), null);
    }

    public void setFaces(Face[] faces) {
        if (!this.mPause) {
            if (this.mFaces == null || ((faces.length <= 0 || this.mFaces.length != 0) && (faces.length != 0 || this.mFaces.length <= 0))) {
                if (this.mStateSwitchPending) {
                    this.mStateSwitchPending = false;
                    this.mHandler.removeMessages(1);
                }
                this.mFaces = faces;
                invalidate();
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
        invalidate();
    }

    public void showSuccess(boolean timeout) {
        invalidate();
    }

    public void showFail(boolean timeout) {
        invalidate();
    }

    public void clear() {
        this.mFaces = null;
        invalidate();
    }

    public void pause() {
        this.mPause = true;
    }

    public void resume() {
        this.mPause = false;
    }

    public void setBlockDraw(boolean block) {
        this.mBlocked = block;
    }

    /* Access modifiers changed, original: protected */
    public void onDraw(Canvas canvas) {
        if (!(this.mBlocked || this.mFaces == null || this.mFaces.length <= 0)) {
            int temp;
            int rw = (int) this.mPreviewArea.width();
            int rh = (int) this.mPreviewArea.height();
            if ((rh > rw && (this.mDisplayOrientation == 0 || this.mDisplayOrientation == MediaProviderUtils.ROTATION_180)) || (rw > rh && (this.mDisplayOrientation == 90 || this.mDisplayOrientation == MediaProviderUtils.ROTATION_270))) {
                temp = rw;
                rw = rh;
                rh = temp;
            }
            CameraUtil.prepareMatrix(this.mMatrix, this.mMirror, this.mDisplayOrientation, rw, rh);
            canvas.save();
            this.mMatrix.postRotate((float) this.mOrientation);
            canvas.rotate((float) (-this.mOrientation));
            for (temp = 0; temp < this.mFaces.length; temp++) {
                if (this.mFaces[temp].score >= 50) {
                    this.mRect.set(this.mFaces[temp].rect);
                    this.mMatrix.mapRect(this.mRect);
                    this.mPaint.setColor(this.mColor);
                    this.mRect.offset(this.mPreviewArea.left, this.mPreviewArea.top);
                    if (this.DRAW_SQUARE.booleanValue()) {
                        canvas.drawRect(this.mRect, this.mPaint);
                    } else if (this.mFaceBound != null) {
                        this.mFaceBound.draw(canvas, this.mRect);
                    }
                }
            }
            canvas.restore();
        }
        super.onDraw(canvas);
    }

    public void onPreviewAreaChanged(RectF previewArea) {
        this.mPreviewArea.set(previewArea);
    }
}
