package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.View;
import com.hmdglobal.app.camera.R;
import java.util.LinkedList;
import java.util.List;

public class MicroVideoProgressBar extends View {
    private static final int DEFAULT_BACKGROUND_COLOR = Color.argb(51, 255, 255, 255);
    private static final int DEFAULT_MINIMUM_PROGRESS_COLOR = Color.argb(Const.CODE_C1_HDW, 255, 255, 255);
    private static final int DEFAULT_PROGRESS_COLOR = Color.argb(255, 30, 136, 229);
    private static final int DEFAULT_SEGMENT_SELECTED_COLOR = Color.argb(255, 255, 82, 82);
    private static final int DEFAULT_UPDATING_COLOR = Color.argb(255, 30, 136, 229);
    private int mBackgroundColor = DEFAULT_BACKGROUND_COLOR;
    private boolean mLastSegmentSelected;
    private float mProgress = 0.0f;
    private int mProgressColor = DEFAULT_PROGRESS_COLOR;
    private float mProgressLowerBound = 0.0f;
    private float mProgressUpperBound = 100.0f;
    private int mSegmentSelectedColor = DEFAULT_SEGMENT_SELECTED_COLOR;
    List<Float> mSegmentStarts = new LinkedList();
    private int mSegmentWidth;
    private int mUpdatingColor = DEFAULT_UPDATING_COLOR;

    public MicroVideoProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        parseAttributeSet(attrs);
        this.mSegmentWidth = (int) getResources().getDimension(R.dimen.micro_segment_width);
    }

    private void parseAttributeSet(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.MicroVideoProgressBar);
        this.mProgressColor = a.getColor(1, DEFAULT_PROGRESS_COLOR);
        this.mBackgroundColor = a.getColor(0, DEFAULT_BACKGROUND_COLOR);
        a.recycle();
    }

    /* Access modifiers changed, original: protected */
    public void onDraw(Canvas canvas) {
        int start;
        Canvas canvas2 = canvas;
        canvas.save();
        ColorDrawable progressColor = new ColorDrawable(this.mProgressColor);
        ColorDrawable backgroundColor = new ColorDrawable(this.mBackgroundColor);
        ColorDrawable segmentColor = new ColorDrawable(this.mBackgroundColor);
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        if (this.mProgress == 0.0f) {
            backgroundColor.setBounds(0, 0, width, height);
            backgroundColor.draw(canvas2);
        } else {
            int firstSegment = 0 - this.mSegmentWidth;
            float lastSegment = 0.0f;
            if (this.mSegmentStarts.size() > 0) {
                lastSegment = ((Float) this.mSegmentStarts.get(this.mSegmentStarts.size() - 1)).floatValue();
            }
            int progressBound = (int) ((this.mProgress * ((float) width)) / this.mProgressUpperBound);
            int lastSegmentBound = (int) ((((float) width) * lastSegment) / this.mProgressUpperBound);
            backgroundColor.setBounds(progressBound, 0, width, height);
            backgroundColor.draw(canvas2);
            for (Float segmentStart : this.mSegmentStarts) {
                float segmentStart2 = segmentStart.floatValue();
                if (this.mLastSegmentSelected && segmentStart2 == lastSegment) {
                    progressColor = new ColorDrawable(this.mSegmentSelectedColor);
                }
                start = (int) ((((float) width) * segmentStart2) / this.mProgressUpperBound);
                progressColor.setBounds(this.mSegmentWidth + firstSegment, 0, start, height);
                progressColor.draw(canvas2);
                if (start != lastSegmentBound) {
                    segmentColor.setBounds(start, 0, this.mSegmentWidth + start, height);
                    segmentColor.draw(canvas2);
                }
                firstSegment = start;
            }
            ColorDrawable updatingColor = new ColorDrawable(this.mUpdatingColor);
            int updatingStart = lastSegmentBound == 0 ? 0 : this.mSegmentWidth + lastSegmentBound;
            if (updatingStart < progressBound) {
                updatingColor.setBounds(updatingStart, 0, progressBound, height);
            }
            updatingColor.draw(canvas2);
        }
        if (this.mProgress > 0.0f && this.mProgress < this.mProgressLowerBound) {
            ColorDrawable lowerBoundColor = new ColorDrawable(DEFAULT_MINIMUM_PROGRESS_COLOR);
            start = (int) ((this.mProgressLowerBound * ((float) width)) / this.mProgressUpperBound);
            lowerBoundColor.setBounds(start, 0, this.mSegmentWidth + start, height);
            lowerBoundColor.draw(canvas2);
        }
        canvas.restore();
        super.onDraw(canvas);
        this.mLastSegmentSelected = false;
    }

    public void updateProgress(float progress) {
        if (progress > this.mProgressUpperBound) {
            progress = this.mProgressUpperBound;
        }
        if (progress < 0.0f) {
            progress = 0.0f;
        }
        this.mProgress = progress;
        invalidate();
    }

    public void markSegmentStart(float duration) {
        if (duration < 0.0f) {
            duration = 0.0f;
        }
        if (duration >= this.mProgressUpperBound) {
            duration = this.mProgressUpperBound;
        }
        float lastSegmentStart = 0.0f;
        if (this.mSegmentStarts.size() > 0) {
            lastSegmentStart = ((Float) this.mSegmentStarts.get(this.mSegmentStarts.size() - 1)).floatValue();
        }
        float currentStart = lastSegmentStart + duration;
        if (lastSegmentStart + duration >= this.mProgressUpperBound) {
            currentStart = this.mProgressUpperBound;
        }
        this.mSegmentStarts.add(Float.valueOf(currentStart));
        this.mProgress = currentStart;
        invalidate();
    }

    public float getSumDuration() {
        if (this.mSegmentStarts.size() > 0) {
            return ((Float) this.mSegmentStarts.get(this.mSegmentStarts.size() - 1)).floatValue();
        }
        return 0.0f;
    }

    public float segmentRemove() {
        float removedStart = 0.0f;
        if (this.mSegmentStarts.size() > 0) {
            removedStart = ((Float) this.mSegmentStarts.remove(this.mSegmentStarts.size() - 1)).floatValue();
            if (this.mSegmentStarts.size() > 0) {
                this.mProgress = ((Float) this.mSegmentStarts.get(this.mSegmentStarts.size() - 1)).floatValue();
            } else {
                this.mProgress = 0.0f;
            }
        } else {
            this.mProgress = 0.0f;
        }
        invalidate();
        return removedStart - this.mProgress;
    }

    public void clearProgress() {
        this.mProgress = 0.0f;
        this.mSegmentStarts.clear();
        invalidate();
    }

    public void setProgressUpperBound(float upperBound) {
        if (upperBound > 0.0f) {
            this.mProgressUpperBound = upperBound;
            return;
        }
        throw new RuntimeException("Invalid upper bound for ProgressBar");
    }

    public void setProgressLowerBound(float lowerBound) {
        if (lowerBound > 0.0f) {
            this.mProgressLowerBound = lowerBound;
            return;
        }
        throw new RuntimeException("Invalid lower bound for ProgressBar");
    }

    public void clearPendingProgress() {
        if (this.mSegmentStarts.size() == 0) {
            this.mProgress = 0.0f;
        } else {
            this.mProgress = ((Float) this.mSegmentStarts.get(this.mSegmentStarts.size() - 1)).floatValue();
        }
        invalidate();
    }

    public void changeLastSegmentColor() {
        this.mLastSegmentSelected = true;
        invalidate();
    }
}
