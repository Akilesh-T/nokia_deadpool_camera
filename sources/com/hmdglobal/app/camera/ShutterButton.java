package com.hmdglobal.app.camera;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnLongClickListener;
import android.widget.ImageView.ScaleType;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.ui.BottomBar.OnArcSeekBarListener;
import com.hmdglobal.app.camera.ui.RotateImageView;
import com.hmdglobal.app.camera.ui.TouchCoordinate;
import java.util.ArrayList;
import java.util.List;

public class ShutterButton extends RotateImageView {
    public static final float ALPHA_WHEN_DISABLED = 0.2f;
    public static final float ALPHA_WHEN_ENABLED = 1.0f;
    private static final float BASE_ALPHA = 0.5f;
    private static Object LISTENER_SYNC_LOCK = new Object();
    private static final float ROTATE_DEGREE_TARGET = 45.0f;
    private static final Tag TAG = new Tag("ShutterButton");
    private static final int TRANSITION_DURATION = 250;
    private float downY;
    private AnimatorSet mAnimatorSet;
    private OnCancelSelectionMenuListener mCancelSelectionMenuListener;
    private Matrix mCombineMatrix;
    private int mCurrentAlpha = 255;
    private AnimatorSet mFadeComboAnimator;
    private ValueAnimator mFadeInAnimator;
    private ValueAnimator mFadeOutAnimator;
    private int mHeight;
    private boolean mIsCanSlide = true;
    private boolean mIsContructed = false;
    private boolean mIsLongClick = false;
    private List<OnShutterButtonListener> mListeners = new ArrayList();
    private OnLongClickListener mLongClick = new OnLongClickListener() {
        public boolean onLongClick(View v) {
            for (OnShutterButtonListener listener : ShutterButton.this.mListeners) {
                ShutterButton.this.mIsLongClick = true;
                listener.onShutterCoordinate(ShutterButton.this.mTouchCoordinate);
                ShutterButton.this.mTouchCoordinate = null;
                listener.onShutterButtonLongClick();
            }
            return false;
        }
    };
    private boolean mNeedSuperDraw = true;
    private boolean mOldPressed;
    private int mPendingAlpha = 0;
    private Drawable mPendingDrawable;
    private Matrix mRotateMatrix;
    private Matrix mScaleMatrix;
    private TouchCoordinate mTouchCoordinate;
    private boolean mTouchEnabled = true;
    private float mTransY = 0.0f;
    private int mWidth;
    private OnArcSeekBarListener onArcSeekBarListener;

    public interface OnShutterButtonListener {
        void onShutterButtonClick();

        void onShutterButtonFocus(boolean z);

        void onShutterButtonLongClick();

        void onShutterCoordinate(TouchCoordinate touchCoordinate);
    }

    public ShutterButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setScaleType(ScaleType.MATRIX);
        setOnLongClickListener(this.mLongClick);
        this.mIsContructed = true;
    }

    public void addOnShutterButtonListener(OnShutterButtonListener listener) {
        synchronized (LISTENER_SYNC_LOCK) {
            if (!this.mListeners.contains(listener)) {
                this.mListeners.add(listener);
            }
        }
    }

    public void removeOnShutterButtonListener(OnShutterButtonListener listener) {
        synchronized (LISTENER_SYNC_LOCK) {
            if (this.mListeners.contains(listener)) {
                this.mListeners.remove(listener);
            }
        }
    }

    public void setArcSeekBarListener(OnArcSeekBarListener onArcSeekBarListener) {
        this.onArcSeekBarListener = onArcSeekBarListener;
    }

    public void setOnCancelSelectionMenu(OnCancelSelectionMenuListener onCancelSelectionMenuListener) {
        this.mCancelSelectionMenuListener = onCancelSelectionMenuListener;
    }

    public void setImageDrawable(Drawable drawable) {
        if (getDrawable() == null || !this.mIsContructed || getWidth() == 0) {
            resetShutter(drawable);
            return;
        }
        this.mPendingAlpha = 0;
        this.mPendingDrawable = drawable;
        resetShutter(drawable);
    }

    public void setImageDrawableWithoutRotation(Drawable drawable) {
        if (this.mFadeComboAnimator != null && this.mFadeComboAnimator.isRunning()) {
            this.mFadeComboAnimator.cancel();
        }
        resetShutter(drawable);
    }

    private void initializeFadeAnimation() {
        AnimatorUpdateListener fadeInAnimatorUpdateListener = new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                ShutterButton.this.mCurrentAlpha = ((Integer) ShutterButton.this.mFadeInAnimator.getAnimatedValue()).intValue();
            }
        };
        AnimatorUpdateListener fadeOutAnimatorUpdateListener = new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                ShutterButton.this.mPendingAlpha = ((Integer) valueAnimator.getAnimatedValue()).intValue();
                ShutterButton.this.invalidate();
            }
        };
        this.mFadeInAnimator = ValueAnimator.ofInt(new int[]{255, 0});
        this.mFadeInAnimator.setDuration(250);
        this.mFadeInAnimator.addUpdateListener(fadeInAnimatorUpdateListener);
        this.mFadeOutAnimator = ValueAnimator.ofInt(new int[]{0, 255});
        this.mFadeOutAnimator.setDuration(250);
        this.mFadeOutAnimator.addUpdateListener(fadeOutAnimatorUpdateListener);
        this.mFadeComboAnimator = new AnimatorSet();
        this.mFadeComboAnimator.playTogether(new Animator[]{this.mFadeInAnimator, this.mFadeOutAnimator});
        this.mFadeComboAnimator.setDuration(250);
    }

    private void playShutterIconFadeInOutAnimation(final Drawable drawable) {
        if (this.mFadeComboAnimator == null) {
            initializeFadeAnimation();
        }
        if (this.mFadeComboAnimator.isRunning()) {
            this.mFadeComboAnimator.cancel();
        }
        this.mFadeComboAnimator.addListener(new AnimatorListener() {
            public void onAnimationStart(Animator animator) {
            }

            public void onAnimationEnd(Animator animator) {
                ShutterButton.this.resetShutter(drawable);
                animator.removeListener(this);
            }

            public void onAnimationCancel(Animator animator) {
                ShutterButton.this.resetShutter(drawable);
                animator.removeListener(this);
            }

            public void onAnimationRepeat(Animator animator) {
            }
        });
        this.mFadeComboAnimator.start();
    }

    private void resetShutter(Drawable drawable) {
        super.setImageDrawable(drawable);
        this.mPendingDrawable = null;
        this.mPendingAlpha = 0;
        this.mCurrentAlpha = 255;
        invalidate();
    }

    /* Access modifiers changed, original: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        this.mWidth = MeasureSpec.getSize(widthMeasureSpec);
        this.mHeight = MeasureSpec.getSize(heightMeasureSpec);
    }

    /* Access modifiers changed, original: protected */
    public void onDraw(Canvas canvas) {
        float f;
        canvas.save();
        if (this.mTransY >= 0.0f) {
            f = (float) (this.mTransY > 0.0f ? -30 : 0);
        } else {
            f = 30.0f;
        }
        canvas.translate(0.0f, f);
        if (this.mPendingDrawable != null) {
            if (getDrawable() != null) {
                getDrawable().setAlpha(this.mCurrentAlpha);
            }
            this.mPendingDrawable.setAlpha(this.mPendingAlpha);
            canvas.save();
            canvas.rotate((float) (-this.mCurrentDegree));
            this.mPendingDrawable.setBounds(canvas.getClipBounds());
            this.mPendingDrawable.draw(canvas);
            canvas.restore();
        }
        super.onDraw(canvas);
        canvas.restore();
    }

    public boolean onTouchEvent(MotionEvent event) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[ShutterButton] isLocked = ");
        stringBuilder.append(isLocked());
        stringBuilder.append(" action = ");
        stringBuilder.append(event.getAction());
        stringBuilder.append(" isIgnoreLock = ");
        stringBuilder.append(isIgnoreLock());
        Log.d(tag, stringBuilder.toString());
        if (isLocked()) {
            if (isIgnoreLock() && event.getAction() == 1 && this.mCancelSelectionMenuListener != null) {
                this.mCancelSelectionMenuListener.onCancelSelectionMenu();
            }
            return super.onTouchEvent(event);
        }
        switch (event.getAction()) {
            case 0:
                Log.d(TAG, "[ShutterButton] onTouchEvent ACTION_DOWN");
                this.downY = event.getY();
                break;
            case 1:
                Log.d(TAG, "[ShutterButton] onTouchEvent ACTION_UP");
                break;
            case 2:
                Log.d(TAG, "[ShutterButton] onTouchEvent ACTION_MOVE");
                if (this.mIsCanSlide) {
                    this.mTransY = this.downY - event.getY();
                    Tag tag2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("[ShutterButton] mTransY = ");
                    stringBuilder2.append(this.mTransY);
                    stringBuilder2.append(" mHeight = ");
                    stringBuilder2.append(this.mHeight);
                    Log.d(tag2, stringBuilder2.toString());
                    if (Math.abs(this.mTransY) > 30.0f) {
                        invalidate();
                        break;
                    }
                }
                break;
            case 3:
                break;
        }
        Log.d(TAG, "[ShutterButton] onTouchEvent ACTION_CANCEL");
        if (this.mTransY > 30.0f) {
            if (this.onArcSeekBarListener != null) {
                this.onArcSeekBarListener.slipUpShutterButton();
            }
        } else if (this.mTransY < -30.0f && this.onArcSeekBarListener != null) {
            this.onArcSeekBarListener.slipDownShutterButton();
        }
        this.mTransY = 0.0f;
        invalidate();
        return super.onTouchEvent(event);
    }

    /* Access modifiers changed, original: protected */
    public boolean needSuperDrawable() {
        return true;
    }

    public boolean dispatchTouchEvent(MotionEvent m) {
        if (!this.mTouchEnabled) {
            return false;
        }
        if (m.getActionMasked() == 1) {
            this.mTouchCoordinate = new TouchCoordinate(m.getX(), m.getY(), (float) getMeasuredWidth(), (float) getMeasuredHeight());
        }
        return super.dispatchTouchEvent(m);
    }

    public void enableTouch(boolean enable) {
        this.mTouchEnabled = enable;
    }

    /* Access modifiers changed, original: protected */
    public void drawableStateChanged() {
        super.drawableStateChanged();
        final boolean pressed = isPressed();
        if (pressed != this.mOldPressed) {
            if (pressed) {
                callShutterButtonFocus(pressed);
            } else {
                post(new Runnable() {
                    public void run() {
                        ShutterButton.this.callShutterButtonFocus(pressed);
                    }
                });
            }
            this.mOldPressed = pressed;
        }
    }

    private void callShutterButtonFocus(boolean pressed) {
        if (!pressed) {
            this.mIsLongClick = false;
        }
        for (OnShutterButtonListener listener : this.mListeners) {
            listener.onShutterButtonFocus(pressed);
        }
    }

    public boolean performClick() {
        boolean result = super.performClick();
        if (getVisibility() == 0 && !this.mIsLongClick) {
            synchronized (LISTENER_SYNC_LOCK) {
                for (OnShutterButtonListener listener : this.mListeners) {
                    listener.onShutterCoordinate(this.mTouchCoordinate);
                    this.mTouchCoordinate = null;
                    listener.onShutterButtonClick();
                }
            }
        }
        return result;
    }

    public boolean isLongClicking() {
        return this.mIsLongClick;
    }

    public void switchSlidingAbility(boolean isSliding) {
        this.mIsCanSlide = isSliding;
    }
}
