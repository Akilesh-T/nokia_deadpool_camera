package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import com.hmdglobal.app.camera.R;

public class ShutterSaveProgressbar extends View {
    private static final int TIME_DELAY_AVERAGE = 10;
    private static final int TIME_DELAY_MAX = 20;
    private static final int TIME_DELAY_MIN = 5;
    private Paint mArcPaint = new Paint();
    private float mArcProgress = 0.0f;
    private int mBarWidth;
    private RectF mCircleBounds = new RectF();
    private Paint mCirclePaint = new Paint();
    private int mCurrentProgress;
    private boolean mIsNeedLoopValidate = false;
    private int mProgressStep = 10;
    private int mProgress_last_second = 110;
    private float mProgress_max = 120.0f;
    private int mTimeDelay = 30;

    public ShutterSaveProgressbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mBarWidth = (int) context.getResources().getDimension(R.dimen.panorama_circle_bar_width);
        intPaints();
    }

    private void intPaints() {
        this.mArcPaint.setColor(getResources().getColor(R.color.panorama_save_arc));
        this.mArcPaint.setAntiAlias(true);
        this.mArcPaint.setStyle(Style.STROKE);
        this.mArcPaint.setStrokeWidth((float) this.mBarWidth);
        this.mCirclePaint.setColor(getResources().getColor(R.color.panorama_save_circle));
        this.mCirclePaint.setAntiAlias(true);
        this.mCirclePaint.setStyle(Style.STROKE);
        this.mCirclePaint.setStrokeWidth((float) this.mBarWidth);
    }

    /* Access modifiers changed, original: protected */
    public void onSizeChanged(int newWidth, int newHeight, int oldWidth, int oldHeight) {
        super.onSizeChanged(newWidth, newHeight, oldWidth, oldHeight);
        this.mCircleBounds = new RectF((float) (this.mBarWidth / 2), (float) (this.mBarWidth / 2), (float) (getWidth() - (this.mBarWidth / 2)), (float) (getHeight() - (this.mBarWidth / 2)));
        invalidate();
    }

    /* Access modifiers changed, original: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawArc(this.mCircleBounds, -90.0f, 360.0f, false, this.mCirclePaint);
        canvas.drawArc(this.mCircleBounds, -90.0f, this.mArcProgress, false, this.mArcPaint);
        if (this.mIsNeedLoopValidate) {
            postValidateDelay();
        }
    }

    public void init(int max, int step) {
        this.mProgress_max = (float) max;
        this.mProgress_last_second = max - step;
        this.mProgressStep = step;
    }

    public void setProgress(int progress) {
        if (progress - this.mCurrentProgress > this.mProgressStep / 2) {
            this.mCurrentProgress = progress;
            this.mTimeDelay = 5;
        } else if (progress - this.mCurrentProgress < 0) {
            this.mTimeDelay = 20;
        } else {
            this.mTimeDelay = 10;
            this.mCurrentProgress = progress;
        }
        if (progress == this.mProgress_last_second) {
            this.mTimeDelay = 5;
        }
    }

    private void postValidateDelay() {
        if (((double) this.mCurrentProgress) < 0.97d * ((double) this.mProgress_max)) {
            this.mCurrentProgress++;
        }
        this.mArcProgress = (float) (360.0d * ((double) (((float) this.mCurrentProgress) / this.mProgress_max)));
        postInvalidateDelayed((long) this.mTimeDelay);
    }

    public void startPlay() {
        this.mIsNeedLoopValidate = true;
        this.mCurrentProgress = 0;
        postInvalidate();
    }

    public void stopPlay() {
        this.mIsNeedLoopValidate = false;
        this.mCurrentProgress = 0;
    }
}
