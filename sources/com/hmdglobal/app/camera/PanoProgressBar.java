package com.hmdglobal.app.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.hmdglobal.app.camera.debug.Log.Tag;

class PanoProgressBar extends ImageView {
    public static final int DIRECTION_LEFT = 1;
    public static final int DIRECTION_NONE = 0;
    public static final int DIRECTION_RIGHT = 2;
    private static final Tag TAG = new Tag("PanoProgressBar");
    private final Paint mBackgroundPaint = new Paint();
    private int mDirection = 0;
    private final Paint mDoneAreaPaint = new Paint();
    private RectF mDrawBounds;
    private float mHeight;
    private final Paint mIndicatorPaint = new Paint();
    private float mIndicatorWidth = 0.0f;
    private float mLeftMostProgress = 0.0f;
    private OnDirectionChangeListener mListener = null;
    private float mMaxProgress = 0.0f;
    private float mProgress = 0.0f;
    private float mProgressOffset = 0.0f;
    private float mRightMostProgress = 0.0f;
    private float mWidth;

    public interface OnDirectionChangeListener {
        void onDirectionChange(int i);
    }

    public PanoProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mDoneAreaPaint.setStyle(Style.FILL);
        this.mDoneAreaPaint.setAlpha(255);
        this.mBackgroundPaint.setStyle(Style.FILL);
        this.mBackgroundPaint.setAlpha(255);
        this.mIndicatorPaint.setStyle(Style.FILL);
        this.mIndicatorPaint.setAlpha(255);
        this.mDrawBounds = new RectF();
    }

    public void setOnDirectionChangeListener(OnDirectionChangeListener l) {
        this.mListener = l;
    }

    private void setDirection(int direction) {
        if (this.mDirection != direction) {
            this.mDirection = direction;
            if (this.mListener != null) {
                this.mListener.onDirectionChange(this.mDirection);
            }
            invalidate();
        }
    }

    public int getDirection() {
        return this.mDirection;
    }

    public void setBackgroundColor(int color) {
        this.mBackgroundPaint.setColor(color);
        invalidate();
    }

    public void setDoneColor(int color) {
        this.mDoneAreaPaint.setColor(color);
        invalidate();
    }

    public void setIndicatorColor(int color) {
        this.mIndicatorPaint.setColor(color);
        invalidate();
    }

    /* Access modifiers changed, original: protected */
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.mWidth = (float) w;
        this.mHeight = (float) h;
        this.mDrawBounds.set(0.0f, 0.0f, this.mWidth, this.mHeight);
    }

    public void setMaxProgress(int progress) {
        this.mMaxProgress = (float) progress;
    }

    public void setIndicatorWidth(float w) {
        this.mIndicatorWidth = w;
        invalidate();
    }

    public void setRightIncreasing(boolean rightIncreasing) {
        if (rightIncreasing) {
            this.mLeftMostProgress = 0.0f;
            this.mRightMostProgress = 0.0f;
            this.mProgressOffset = 0.0f;
            setDirection(2);
        } else {
            this.mLeftMostProgress = this.mWidth;
            this.mRightMostProgress = this.mWidth;
            this.mProgressOffset = this.mWidth;
            setDirection(1);
        }
        invalidate();
    }

    public void setProgress(int progress) {
        if (this.mDirection == 0) {
            if (progress > 10) {
                setRightIncreasing(true);
            } else if (progress < -10) {
                setRightIncreasing(false);
            }
        }
        if (this.mDirection != 0) {
            this.mProgress = ((((float) progress) * this.mWidth) / this.mMaxProgress) + this.mProgressOffset;
            this.mProgress = Math.min(this.mWidth, Math.max(0.0f, this.mProgress));
            if (this.mDirection == 2) {
                this.mRightMostProgress = Math.max(this.mRightMostProgress, this.mProgress);
            }
            if (this.mDirection == 1) {
                this.mLeftMostProgress = Math.min(this.mLeftMostProgress, this.mProgress);
            }
            invalidate();
        }
    }

    public void reset() {
        this.mProgress = 0.0f;
        this.mProgressOffset = 0.0f;
        setDirection(0);
        invalidate();
    }

    /* Access modifiers changed, original: protected */
    public void onDraw(Canvas canvas) {
        canvas.drawRect(this.mDrawBounds, this.mBackgroundPaint);
        if (this.mDirection != 0) {
            float l;
            float f;
            canvas.drawRect(this.mLeftMostProgress, this.mDrawBounds.top, this.mRightMostProgress, this.mDrawBounds.bottom, this.mDoneAreaPaint);
            if (this.mDirection == 2) {
                l = Math.max(this.mProgress - this.mIndicatorWidth, 0.0f);
                f = this.mProgress;
            } else {
                l = this.mProgress;
                f = Math.min(this.mProgress + this.mIndicatorWidth, this.mWidth);
            }
            canvas.drawRect(l, this.mDrawBounds.top, f, this.mDrawBounds.bottom, this.mIndicatorPaint);
        }
        super.onDraw(canvas);
    }
}
