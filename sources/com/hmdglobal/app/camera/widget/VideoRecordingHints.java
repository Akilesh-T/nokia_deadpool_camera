package com.hmdglobal.app.camera.widget;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.morphoinc.utils.multimedia.MediaProviderUtils;
import java.lang.ref.WeakReference;

public class VideoRecordingHints extends View {
    private static final int FADE_OUT_DURATION_MS = 600;
    private static final float INITIAL_ROTATION = 0.0f;
    private static final int PORTRAIT_ROTATE_DELAY_MS = 1000;
    private static final float ROTATION_DEGREES = 180.0f;
    private static final int ROTATION_DURATION_MS = 1000;
    private static final int UNSET = -1;
    private final ObjectAnimator mAlphaAnimator;
    private int mCenterX = -1;
    private int mCenterY = -1;
    private final boolean mIsDefaultToPortrait;
    private boolean mIsInLandscape = false;
    private int mLastOrientation = -1;
    private final Drawable mPhoneGraphic = getResources().getDrawable(R.drawable.ic_phone_graphic);
    private final int mPhoneGraphicHalfHeight = (getResources().getDimensionPixelSize(R.dimen.video_hint_phone_graphic_height) / 2);
    private final int mPhoneGraphicHalfWidth = (getResources().getDimensionPixelSize(R.dimen.video_hint_phone_graphic_width) / 2);
    private final Drawable mRotateArrows = getResources().getDrawable(R.drawable.rotate_arrows);
    private final int mRotateArrowsHalfSize = (getResources().getDimensionPixelSize(R.dimen.video_hint_arrow_size) / 2);
    private float mRotation = 0.0f;
    private final ValueAnimator mRotationAnimation = ValueAnimator.ofFloat(new float[]{this.mRotation, this.mRotation + ROTATION_DEGREES});

    private static class AlphaAnimatorListener implements AnimatorListener {
        private final WeakReference<VideoRecordingHints> mHints;

        AlphaAnimatorListener(VideoRecordingHints hint) {
            this.mHints = new WeakReference(hint);
        }

        public void onAnimationStart(Animator animation) {
        }

        public void onAnimationEnd(Animator animation) {
            VideoRecordingHints hint = (VideoRecordingHints) this.mHints.get();
            if (hint != null) {
                hint.invalidate();
                hint.setAlpha(1.0f);
                hint.mRotation = 0.0f;
            }
        }

        public void onAnimationCancel(Animator animation) {
        }

        public void onAnimationRepeat(Animator animation) {
        }
    }

    private static class RotationAnimatorListener implements AnimatorListener {
        private boolean mCanceled = false;
        private final WeakReference<VideoRecordingHints> mHints;

        public RotationAnimatorListener(VideoRecordingHints hint) {
            this.mHints = new WeakReference(hint);
        }

        public void onAnimationStart(Animator animation) {
            this.mCanceled = false;
        }

        public void onAnimationEnd(Animator animation) {
            VideoRecordingHints hint = (VideoRecordingHints) this.mHints.get();
            if (hint != null) {
                hint.mRotation = (float) (((int) hint.mRotation) % 360);
                if (!this.mCanceled) {
                    hint.post(new Runnable() {
                        public void run() {
                            VideoRecordingHints hint = (VideoRecordingHints) RotationAnimatorListener.this.mHints.get();
                            if (hint != null) {
                                hint.continueRotationAnimation();
                            }
                        }
                    });
                }
            }
        }

        public void onAnimationCancel(Animator animation) {
            this.mCanceled = true;
        }

        public void onAnimationRepeat(Animator animation) {
        }
    }

    public VideoRecordingHints(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mRotationAnimation.setDuration(1000);
        this.mRotationAnimation.setStartDelay(1000);
        this.mRotationAnimation.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                VideoRecordingHints.this.mRotation = ((Float) animation.getAnimatedValue()).floatValue();
                VideoRecordingHints.this.invalidate();
            }
        });
        this.mRotationAnimation.addListener(new RotationAnimatorListener(this));
        this.mAlphaAnimator = ObjectAnimator.ofFloat(this, "alpha", new float[]{1.0f, 0.0f});
        this.mAlphaAnimator.setDuration(600);
        this.mAlphaAnimator.addListener(new AlphaAnimatorListener(this));
        this.mIsDefaultToPortrait = CameraUtil.isDefaultToPortrait(context);
    }

    private void continueRotationAnimation() {
        if (!this.mRotationAnimation.isRunning()) {
            this.mRotationAnimation.setFloatValues(new float[]{this.mRotation, this.mRotation + ROTATION_DEGREES});
            this.mRotationAnimation.start();
        }
    }

    public void onVisibilityChanged(View v, int visibility) {
        super.onVisibilityChanged(v, visibility);
        if (getVisibility() == 0 && !isInLandscape()) {
            continueRotationAnimation();
        } else if (getVisibility() != 0) {
            this.mRotationAnimation.cancel();
            this.mRotation = 0.0f;
        }
    }

    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        this.mCenterX = (right - left) / 2;
        this.mCenterY = (bottom - top) / 2;
        this.mRotateArrows.setBounds(this.mCenterX - this.mRotateArrowsHalfSize, this.mCenterY - this.mRotateArrowsHalfSize, this.mCenterX + this.mRotateArrowsHalfSize, this.mCenterY + this.mRotateArrowsHalfSize);
        this.mPhoneGraphic.setBounds(this.mCenterX - this.mPhoneGraphicHalfWidth, this.mCenterY - this.mPhoneGraphicHalfHeight, this.mCenterX + this.mPhoneGraphicHalfWidth, this.mCenterY + this.mPhoneGraphicHalfHeight);
        invalidate();
    }

    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (!this.mIsInLandscape || this.mAlphaAnimator.isRunning()) {
            canvas.save();
            canvas.rotate(-this.mRotation, (float) this.mCenterX, (float) this.mCenterY);
            this.mRotateArrows.draw(canvas);
            canvas.restore();
            if (this.mIsInLandscape) {
                canvas.save();
                canvas.rotate(90.0f, (float) this.mCenterX, (float) this.mCenterY);
                this.mPhoneGraphic.draw(canvas);
                canvas.restore();
            } else {
                this.mPhoneGraphic.draw(canvas);
            }
        }
    }

    public void onOrientationChanged(int orientation) {
        if (this.mLastOrientation != orientation) {
            this.mLastOrientation = orientation;
            if (this.mLastOrientation != -1) {
                this.mIsInLandscape = isInLandscape();
                if (getVisibility() == 0) {
                    if (this.mIsInLandscape) {
                        this.mRotationAnimation.cancel();
                        if (!this.mAlphaAnimator.isRunning()) {
                            this.mAlphaAnimator.start();
                        } else {
                            return;
                        }
                    }
                    continueRotationAnimation();
                }
            }
        }
    }

    private boolean isInLandscape() {
        return (this.mLastOrientation % MediaProviderUtils.ROTATION_180 == 90 && this.mIsDefaultToPortrait) || (this.mLastOrientation % MediaProviderUtils.ROTATION_180 == 0 && !this.mIsDefaultToPortrait);
    }
}
