package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import com.android.ex.camera2.portability.Size;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.ui.PreviewStatusListener.PreviewAreaChangedListener;
import com.morphoinc.utils.multimedia.MediaProviderUtils;

public class GestureView extends View implements PreviewAreaChangedListener {
    private Tag TAG = new Tag("GestureView");
    private int mColor;
    private int mDisplayOrientation;
    private Rect mGestureBound;
    private Paint mPaint;
    private int mPostGestureRotation;
    private RectF mPreviewArea;
    private boolean mPreviewMirrored;
    private Size mPreviewSize;
    private int mSensorOrientation;

    public GestureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources res = getResources();
        this.mColor = -16711936;
        this.mPaint = new Paint();
        this.mPaint.setAntiAlias(true);
        this.mPaint.setStyle(Style.STROKE);
        this.mPaint.setColor(this.mColor);
        this.mPaint.setStrokeWidth(res.getDimension(R.dimen.face_circle_stroke));
        setVisibility(8);
    }

    public void showGesture(Rect bound, Size previewSize) {
        setVisibility(0);
        this.mGestureBound = bound;
        this.mPreviewSize = previewSize;
        invalidate();
    }

    public void hideGesture() {
        this.mGestureBound = null;
        setVisibility(8);
        invalidate();
    }

    public void setPostGestureRotation(int orientation) {
        this.mPostGestureRotation = orientation;
    }

    public void setPreviewMirrored(boolean isPreviewMirrored) {
        this.mPreviewMirrored = isPreviewMirrored;
    }

    public void setDisplayOrientation(int orientation) {
        this.mDisplayOrientation = orientation;
    }

    public void setSensorOrientation(int orientation) {
        this.mSensorOrientation = orientation;
    }

    /* Access modifiers changed, original: protected */
    public void onDraw(Canvas canvas) {
        if (this.mGestureBound != null) {
            Rect gestureBound = new Rect(this.mGestureBound);
            Tag tag = this.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("input bound is ");
            stringBuilder.append(gestureBound);
            Log.v(tag, stringBuilder.toString());
            canvas.save();
            rotateRect(gestureBound, this.mPreviewArea, this.mPreviewMirrored);
            tag = this.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("mPreviewArea is ");
            stringBuilder.append(this.mPreviewArea);
            stringBuilder.append(" canvas is ");
            stringBuilder.append(canvas.getWidth());
            stringBuilder.append("x");
            stringBuilder.append(canvas.getHeight());
            Log.v(tag, stringBuilder.toString());
            gestureBound.offset((int) this.mPreviewArea.left, (int) this.mPreviewArea.top);
            tag = this.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("gesture bound offset is ");
            stringBuilder.append(gestureBound);
            Log.v(tag, stringBuilder.toString());
            canvas.drawRect(gestureBound, this.mPaint);
            canvas.restore();
        }
        super.onDraw(canvas);
    }

    private void rotateRect(Rect gestureBound, RectF previewArea, boolean isFlipped) {
        Rect rect = gestureBound;
        int rw = (int) previewArea.width();
        int rh = (int) previewArea.height();
        int sw = this.mPreviewSize.height();
        int sh = this.mPreviewSize.width();
        if ((rh > rw && (this.mSensorOrientation == 0 || this.mSensorOrientation == MediaProviderUtils.ROTATION_180)) || (rw > rh && (this.mSensorOrientation == 90 || this.mSensorOrientation == MediaProviderUtils.ROTATION_270))) {
            int temp = rw;
            rw = rh;
            rh = temp;
            temp = sw;
            sw = sh;
            sh = temp;
        }
        Point lt = new Point(rect.left, rect.top);
        Point rb = new Point(rect.right, rect.bottom);
        Rect detectBound = new Rect(lt.x, lt.y, rb.x, rb.y);
        Log.w(this.TAG, String.format("post gesture rotation is %d , display rotation is %d", new Object[]{Integer.valueOf(this.mPostGestureRotation), Integer.valueOf(this.mDisplayOrientation)}));
        if (isFlipped) {
            if (this.mDisplayOrientation == 0) {
                detectBound = new Rect(sw - rb.x, lt.y, sw - lt.x, rb.y);
            } else if (this.mDisplayOrientation == 90) {
                detectBound = new Rect(sw - rb.y, sh - rb.x, sw - lt.y, sh - lt.x);
            } else if (this.mDisplayOrientation == MediaProviderUtils.ROTATION_270) {
                detectBound = new Rect(lt.y, lt.x, rb.y, rb.x);
            } else {
                detectBound = new Rect(lt.x, sh - rb.y, rb.x, sh - lt.y);
            }
        } else if (this.mDisplayOrientation == 0) {
            detectBound = new Rect(lt.x, lt.y, rb.x, rb.y);
        } else if (this.mDisplayOrientation == 90) {
            detectBound = new Rect(sw - rb.y, lt.x, sw - lt.y, rb.x);
        } else if (this.mDisplayOrientation == MediaProviderUtils.ROTATION_180) {
            detectBound = new Rect(sw - rb.x, sh - rb.y, sw - lt.x, sh - lt.y);
        } else if (this.mDisplayOrientation == MediaProviderUtils.ROTATION_270) {
            detectBound = new Rect(lt.y, sh - rb.x, rb.y, sh - lt.x);
        }
        Log.v(this.TAG, String.format("rw is %d rh is %d sw is %d sh is %d", new Object[]{Integer.valueOf(rw), Integer.valueOf(rh), Integer.valueOf(sw), Integer.valueOf(sh)}));
        float scaleX = ((float) rw) / ((float) sw);
        float scaleY = ((float) rh) / ((float) sh);
        rect.set((int) (((float) detectBound.left) * scaleX), (int) (((float) detectBound.top) * scaleY), (int) (((float) detectBound.right) * scaleX), (int) (((float) detectBound.bottom) * scaleY));
    }

    public void onPreviewAreaChanged(RectF previewArea) {
        this.mPreviewArea = previewArea;
    }
}
