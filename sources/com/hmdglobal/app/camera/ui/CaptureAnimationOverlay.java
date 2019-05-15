package com.hmdglobal.app.camera.ui;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.ui.PreviewStatusListener.PreviewAreaChangedListener;

public class CaptureAnimationOverlay extends View implements PreviewAreaChangedListener {
    private static final int FLASH_COLOR = -16777216;
    private static final long FLASH_DECREASE_DURATION_MS = 150;
    private static final long FLASH_FULL_DURATION_MS = 65;
    private static final float FLASH_MAX_ALPHA = 0.85f;
    private static final long SHORT_FLASH_DECREASE_DURATION_MS = 100;
    private static final long SHORT_FLASH_FULL_DURATION_MS = 34;
    private static final float SHORT_FLASH_MAX_ALPHA = 0.8f;
    private static final Tag TAG = new Tag("CaptureAnimOverlay");
    private final Interpolator mFlashAnimInterpolator;
    private final AnimatorListener mFlashAnimListener;
    private final AnimatorUpdateListener mFlashAnimUpdateListener;
    private AnimatorSet mFlashAnimation;
    private final Paint mPaint = new Paint();
    private RectF mPreviewArea = new RectF();

    public CaptureAnimationOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mPaint.setColor(-16777216);
        this.mFlashAnimInterpolator = new LinearInterpolator();
        this.mFlashAnimUpdateListener = new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                CaptureAnimationOverlay.this.setAlpha(((Float) animation.getAnimatedValue()).floatValue());
                CaptureAnimationOverlay.this.invalidate();
            }
        };
        this.mFlashAnimListener = new AnimatorListener() {
            public void onAnimationStart(Animator animation) {
                CaptureAnimationOverlay.this.setVisibility(0);
            }

            public void onAnimationEnd(Animator animation) {
                CaptureAnimationOverlay.this.mFlashAnimation = null;
                CaptureAnimationOverlay.this.setVisibility(4);
            }

            public void onAnimationCancel(Animator animation) {
            }

            public void onAnimationRepeat(Animator animation) {
            }
        };
    }

    public void startFlashAnimation(boolean shortFlash) {
        float maxAlpha;
        if (this.mFlashAnimation != null && this.mFlashAnimation.isRunning()) {
            this.mFlashAnimation.cancel();
        }
        if (shortFlash) {
            maxAlpha = SHORT_FLASH_MAX_ALPHA;
        } else {
            maxAlpha = FLASH_MAX_ALPHA;
        }
        ValueAnimator flashAnim1 = ValueAnimator.ofFloat(new float[]{maxAlpha, maxAlpha});
        ValueAnimator flashAnim2 = ValueAnimator.ofFloat(new float[]{maxAlpha, null});
        if (shortFlash) {
            flashAnim1.setDuration(SHORT_FLASH_FULL_DURATION_MS);
            flashAnim2.setDuration(SHORT_FLASH_DECREASE_DURATION_MS);
        } else {
            flashAnim1.setDuration(FLASH_FULL_DURATION_MS);
            flashAnim2.setDuration(FLASH_DECREASE_DURATION_MS);
        }
        flashAnim1.addUpdateListener(this.mFlashAnimUpdateListener);
        flashAnim2.addUpdateListener(this.mFlashAnimUpdateListener);
        flashAnim1.setInterpolator(this.mFlashAnimInterpolator);
        flashAnim2.setInterpolator(this.mFlashAnimInterpolator);
        this.mFlashAnimation = new AnimatorSet();
        this.mFlashAnimation.play(flashAnim1).before(flashAnim2);
        this.mFlashAnimation.addListener(this.mFlashAnimListener);
        this.mFlashAnimation.start();
    }

    public void onDraw(Canvas canvas) {
        if (this.mFlashAnimation != null && this.mFlashAnimation.isRunning()) {
            canvas.drawRect(this.mPreviewArea, this.mPaint);
        }
    }

    public void onPreviewAreaChanged(RectF previewArea) {
        this.mPreviewArea.set(previewArea);
    }
}
