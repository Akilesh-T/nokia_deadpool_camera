package com.hmdglobal.app.camera.ui;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.Canvas;
import android.graphics.Point;
import com.hmdglobal.app.camera.app.CameraApp;
import com.hmdglobal.app.camera.beauty.util.Util;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;

public class BottomMockViewAnimationDrawable extends CustomizeDrawable {
    private static final int CIRCLE_ANIM_DURATION_MS = 300;
    private static int DRAWABLE_MAX_LEVEL = 10000;
    private Tag TAG = new Tag("BottomMockAnimation");
    private int mBackgroundColor;
    private int mCanvasHeight;
    private int mCanvasWidth;
    private Point mCenterPoint;
    private float mRadius;
    private ValueAnimator mScaleValueAnimator = new ValueAnimator();
    private int mSmallRadius;

    public interface OnAnimationAsyncListener {
        void onAnimationFinish();
    }

    public BottomMockViewAnimationDrawable(Point center, int color, int smallRadius) {
        this.mCenterPoint = center;
        setARGB(color);
        this.mBackgroundColor = color;
        initAinimator();
        this.mSmallRadius = smallRadius / 2;
    }

    private void initAinimator() {
        this.mScaleValueAnimator.setDuration(300);
        this.mScaleValueAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                BottomMockViewAnimationDrawable.this.setLevel(((Integer) valueAnimator.getAnimatedValue()).intValue());
            }
        });
    }

    /* Access modifiers changed, original: protected */
    public boolean onLevelChange(int level) {
        invalidateSelf();
        return true;
    }

    public void animateToCircle(final int targetColor, int width, int height) {
        if (this.mScaleValueAnimator.isRunning()) {
            this.mScaleValueAnimator.cancel();
        }
        setARGB(this.mBackgroundColor);
        int smallLevel = map(this.mSmallRadius, 0, diagonalLength(width, height), 0, DRAWABLE_MAX_LEVEL);
        this.mScaleValueAnimator.setIntValues(new int[]{getLevel(), smallLevel + 100});
        this.mScaleValueAnimator.addListener(new AnimatorListener() {
            public void onAnimationStart(Animator animator) {
            }

            public void onAnimationEnd(Animator animator) {
                BottomMockViewAnimationDrawable.this.setARGB(targetColor);
                Tag access$000 = BottomMockViewAnimationDrawable.this.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("level is ");
                stringBuilder.append(BottomMockViewAnimationDrawable.this.getLevel());
                Log.v(access$000, stringBuilder.toString());
                animator.removeListener(this);
            }

            public void onAnimationCancel(Animator animator) {
                BottomMockViewAnimationDrawable.this.setARGB(targetColor);
                animator.removeListener(this);
            }

            public void onAnimationRepeat(Animator animator) {
            }
        });
        this.mScaleValueAnimator.start();
    }

    public void animateToFullSize(final OnAnimationAsyncListener listener) {
        if (this.mScaleValueAnimator.isRunning()) {
            this.mScaleValueAnimator.cancel();
        }
        setARGB(this.mBackgroundColor);
        Tag tag = this.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("level is ");
        stringBuilder.append(getLevel());
        Log.w(tag, stringBuilder.toString());
        this.mScaleValueAnimator.setIntValues(new int[]{getLevel(), DRAWABLE_MAX_LEVEL});
        this.mScaleValueAnimator.addListener(new AnimatorListener() {
            public void onAnimationStart(Animator animator) {
            }

            public void onAnimationEnd(Animator animator) {
                if (listener != null) {
                    listener.onAnimationFinish();
                }
                animator.removeListener(this);
            }

            public void onAnimationCancel(Animator animator) {
                if (listener != null) {
                    listener.onAnimationFinish();
                }
                animator.removeListener(this);
            }

            public void onAnimationRepeat(Animator animator) {
            }
        });
        this.mScaleValueAnimator.start();
    }

    public void draw(Canvas canvas) {
        this.mCanvasHeight = canvas.getHeight();
        this.mCanvasWidth = canvas.getWidth();
        int offsetY = Util.dp2px(CameraApp.getContext(), 20.0f);
        this.mRadius = (float) map(getLevel(), 0, DRAWABLE_MAX_LEVEL, 0, diagonalLength(this.mCanvasWidth, this.mCanvasHeight));
        Tag tag = this.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("canvas width is ");
        stringBuilder.append(this.mCanvasWidth);
        stringBuilder.append(" canvas height is ");
        stringBuilder.append(this.mCanvasHeight);
        stringBuilder.append(" radius is ");
        stringBuilder.append(this.mRadius);
        Log.w(tag, stringBuilder.toString());
        canvas.drawCircle((float) this.mCenterPoint.x, (float) (this.mCenterPoint.y + offsetY), this.mRadius - 15.0f, getPaint());
    }

    private static int map(int x, int in_min, int in_max, int out_min, int out_max) {
        return (((x - in_min) * (out_max - out_min)) / (in_max - in_min)) + out_min;
    }

    private static int diagonalLength(int w, int h) {
        return (int) Math.sqrt((double) ((w * w) + (h * h)));
    }
}
