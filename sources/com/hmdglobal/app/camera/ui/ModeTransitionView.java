package com.hmdglobal.app.camera.ui;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.app.CameraAppUI.AnimationFinishedListener;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.util.BitmapPackager;
import com.hmdglobal.app.camera.util.Gusterpolator;

public class ModeTransitionView extends View {
    private static final int ALPHA_FULLY_OPAQUE = 255;
    private static final int ALPHA_FULLY_TRANSPARENT = 0;
    private static final int ALPHA_HALF_TRANSPARENT = 127;
    private static final int DELAYED_TIME = 500;
    private static final int FADE_IN_DURATION_MS = 200;
    private static final int FADE_OUT = 4;
    private static final int FADE_OUT_DURATION_MS = 100;
    private static final int ICON_FADE_OUT_DURATION_MS = 850;
    private static final int IDLE = 0;
    private static final int PEEP_HOLE_ANIMATION = 3;
    private static final int PEEP_HOLE_ANIMATION_DURATION_MS = 300;
    private static final int PULL_DOWN_SHADE = 2;
    private static final int PULL_UP_SHADE = 1;
    private static final float SCROLL_DISTANCE_MULTIPLY_FACTOR = 2.0f;
    private static final int SHOW_STATIC_IMAGE = 5;
    private static final int STATE_ANIMATING = 1;
    private static final int STATE_HIDE_ANIMATOR_BEGIN = 4;
    private static final int STATE_HIDE_ANIMATOR_END = 6;
    private static final int STATE_HIDE_ANIMATOR_RUNNING = 5;
    private static final int STATE_IDLE = 0;
    private static final int STATE_SHOW_ANIMATOR_BEGIN = 1;
    private static final int STATE_SHOW_ANIMATOR_END = 3;
    private static final int STATE_SHOW_ANIMATOR_RUNNING = 2;
    private static final int STATE_STOP = 0;
    private static final Tag TAG = new Tag("ModeTransView");
    public static long sCreateTime = 0;
    public static int sDelayedTime = 0;
    public static int sState = 0;
    private AnimationFinishedListener mAnimationFinishedListener;
    private int mAnimationType = 0;
    private BitmapPackager mBackgroundBitmap;
    private int mBackgroundColor;
    private final Drawable mDefaultDrawable = new ColorDrawable();
    private final GestureDetector mGestureDetector;
    private int mHeight = 0;
    private ValueAnimator mHideCoverAnimator;
    private Drawable mIconDrawable;
    private final Rect mIconRect = new Rect();
    private int mIconSize;
    private long mLastHideTime;
    private final Paint mMaskPaint = new Paint();
    private int mModeTransState = 0;
    private AnimatorSet mPeepHoleAnimator;
    private int mPeepHoleCenterX = 0;
    private int mPeepHoleCenterY = 0;
    private RectF mPendingRect;
    private float mRadius = 0.0f;
    private RectF mRect;
    private float mScrollDistance = 0.0f;
    private float mScrollTrend;
    private final Paint mShadePaint = new Paint();
    private final Path mShadePath = new Path();
    private ValueAnimator mShowAnimator;
    private int mWidth = 0;

    public interface OnTransAnimationListener {
        void onAnimationDone();
    }

    public ModeTransitionView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mMaskPaint.setAlpha(0);
        this.mMaskPaint.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
        this.mBackgroundColor = getResources().getColor(R.color.video_mode_color);
        this.mGestureDetector = new GestureDetector(getContext(), new SimpleOnGestureListener() {
            public boolean onDown(MotionEvent ev) {
                ModeTransitionView.this.setScrollDistance(0.0f);
                ModeTransitionView.this.mScrollTrend = 0.0f;
                return true;
            }

            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                ModeTransitionView.this.setScrollDistance(ModeTransitionView.this.getScrollDistance() + (2.0f * distanceY));
                ModeTransitionView.this.mScrollTrend = (0.3f * ModeTransitionView.this.mScrollTrend) + (0.7f * distanceY);
                return false;
            }
        });
        this.mIconSize = getResources().getDimensionPixelSize(R.dimen.mode_transition_view_icon_size);
        setIconDrawable(this.mDefaultDrawable);
    }

    private void updateShade() {
        if (this.mAnimationType == 1 || this.mAnimationType == 2) {
            float shadeHeight;
            this.mShadePath.reset();
            if (this.mAnimationType == 1) {
                this.mShadePath.addRect(0.0f, ((float) this.mHeight) - getScrollDistance(), (float) this.mWidth, (float) this.mHeight, Direction.CW);
                shadeHeight = getScrollDistance();
            } else {
                this.mShadePath.addRect(0.0f, 0.0f, (float) this.mWidth, -getScrollDistance(), Direction.CW);
                shadeHeight = getScrollDistance() * -1.0f;
            }
            if (this.mIconDrawable != null) {
                if (shadeHeight < ((float) (this.mHeight / 2)) || this.mHeight == 0) {
                    this.mIconDrawable.setAlpha(0);
                } else {
                    this.mIconDrawable.setAlpha(((((int) shadeHeight) - (this.mHeight / 2)) * 255) / (this.mHeight / 2));
                }
            }
            invalidate();
        }
    }

    public void setScrollDistance(float scrollDistance) {
        if (this.mAnimationType == 1) {
            scrollDistance = Math.max(Math.min(scrollDistance, (float) this.mHeight), 0.0f);
        } else if (this.mAnimationType == 2) {
            scrollDistance = Math.max(Math.min(scrollDistance, 0.0f), (float) (-this.mHeight));
        }
        this.mScrollDistance = scrollDistance;
        updateShade();
    }

    public float getScrollDistance() {
        return this.mScrollDistance;
    }

    public void onDraw(Canvas canvas) {
        if (this.mAnimationType == 3) {
            canvas.drawColor(this.mBackgroundColor);
            if (this.mPeepHoleAnimator != null) {
                canvas.drawCircle((float) this.mPeepHoleCenterX, (float) this.mPeepHoleCenterY, this.mRadius, this.mMaskPaint);
            }
        } else if (this.mAnimationType == 1 || this.mAnimationType == 2) {
            canvas.drawPath(this.mShadePath, this.mShadePaint);
        } else if (this.mAnimationType == 0 || this.mAnimationType == 4) {
            canvas.drawColor(this.mBackgroundColor);
        } else if (this.mAnimationType == 5) {
            Rect dstRect;
            int i = 0;
            if (this.mRect != null) {
                dstRect = new Rect((int) this.mRect.left, (int) this.mRect.top, (int) this.mRect.right, (int) this.mRect.bottom);
            } else {
                dstRect = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
            }
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("dst rect is ");
            stringBuilder.append(dstRect);
            Log.w(tag, stringBuilder.toString());
            Rect srcRect = new Rect(0, 0, this.mBackgroundBitmap.get().getWidth(), this.mBackgroundBitmap.get().getHeight());
            canvas.drawBitmap(this.mBackgroundBitmap.get(), srcRect, srcRect, null);
            if (this.mRect != null) {
                marginDrawables = new ColorDrawable[4];
                Rect[] marginRects = new Rect[]{new Rect(0, 0, (int) this.mRect.left, canvas.getHeight()), new Rect(0, 0, canvas.getWidth(), (int) this.mRect.top), new Rect((int) this.mRect.right, 0, canvas.getWidth(), canvas.getHeight()), new Rect(0, (int) this.mRect.bottom, canvas.getWidth(), canvas.getHeight())};
                while (true) {
                    int i2 = i;
                    if (i2 >= 4) {
                        break;
                    }
                    marginDrawables[i2] = new ColorDrawable(ViewCompat.MEASURED_STATE_MASK);
                    marginDrawables[i2].setBounds(marginRects[i2]);
                    marginDrawables[i2].draw(canvas);
                    i = i2 + 1;
                }
            }
            super.onDraw(canvas);
            return;
        }
        super.onDraw(canvas);
        this.mIconDrawable.draw(canvas);
    }

    /* Access modifiers changed, original: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        this.mWidth = right - left;
        this.mHeight = bottom - top;
        this.mIconRect.set((this.mWidth / 2) - (this.mIconSize / 2), (this.mHeight / 2) - (this.mIconSize / 2), (this.mWidth / 2) + (this.mIconSize / 2), (this.mHeight / 2) + (this.mIconSize / 2));
        this.mIconDrawable.setBounds(this.mIconRect);
    }

    public void startPeepHoleAnimation() {
        startPeepHoleAnimation((float) (this.mWidth / 2), (float) (this.mHeight / 2));
    }

    /* JADX WARNING: Incorrect type for fill-array insn 0x004d, element type: float, insn element type: null */
    /* JADX WARNING: Incorrect type for fill-array insn 0x005b, element type: int, insn element type: null */
    private void startPeepHoleAnimation(float r13, float r14) {
        /*
        r12 = this;
        r0 = r12.mPeepHoleAnimator;
        if (r0 == 0) goto L_0x000d;
    L_0x0004:
        r0 = r12.mPeepHoleAnimator;
        r0 = r0.isRunning();
        if (r0 == 0) goto L_0x000d;
    L_0x000c:
        return;
    L_0x000d:
        r0 = 3;
        r12.mAnimationType = r0;
        r1 = (int) r13;
        r12.mPeepHoleCenterX = r1;
        r1 = (int) r14;
        r12.mPeepHoleCenterY = r1;
        r1 = r12.mPeepHoleCenterX;
        r2 = r12.mWidth;
        r3 = r12.mPeepHoleCenterX;
        r2 = r2 - r3;
        r1 = java.lang.Math.max(r1, r2);
        r2 = r12.mPeepHoleCenterY;
        r3 = r12.mHeight;
        r4 = r12.mPeepHoleCenterY;
        r3 = r3 - r4;
        r2 = java.lang.Math.max(r2, r3);
        r3 = r1 * r1;
        r4 = r2 * r2;
        r3 = r3 + r4;
        r3 = (double) r3;
        r3 = java.lang.Math.sqrt(r3);
        r3 = (int) r3;
        r4 = 2;
        r5 = new float[r4];
        r6 = 0;
        r7 = 0;
        r5[r7] = r6;
        r6 = (float) r3;
        r8 = 1;
        r5[r8] = r6;
        r5 = android.animation.ValueAnimator.ofFloat(r5);
        r9 = 300; // 0x12c float:4.2E-43 double:1.48E-321;
        r5.setDuration(r9);
        r6 = new float[r4];
        r6 = {1065353216, 1056964608};
        r6 = android.animation.ValueAnimator.ofFloat(r6);
        r9 = 850; // 0x352 float:1.191E-42 double:4.2E-321;
        r6.setDuration(r9);
        r11 = new int[r4];
        r11 = {127, 0};
        r11 = android.animation.ValueAnimator.ofInt(r11);
        r11.setDuration(r9);
        r9 = new android.animation.AnimatorSet;
        r9.<init>();
        r12.mPeepHoleAnimator = r9;
        r9 = r12.mPeepHoleAnimator;
        r0 = new android.animation.Animator[r0];
        r0[r7] = r5;
        r0[r8] = r11;
        r0[r4] = r6;
        r9.playTogether(r0);
        r0 = r12.mPeepHoleAnimator;
        r4 = com.hmdglobal.app.camera.util.Gusterpolator.INSTANCE;
        r0.setInterpolator(r4);
        r0 = new com.hmdglobal.app.camera.ui.ModeTransitionView$2;
        r0.<init>(r5, r11, r6);
        r11.addUpdateListener(r0);
        r0 = r12.mPeepHoleAnimator;
        r4 = new com.hmdglobal.app.camera.ui.ModeTransitionView$3;
        r4.<init>();
        r0.addListener(r4);
        r0 = r12.mPeepHoleAnimator;
        r0.start();
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.ui.ModeTransitionView.startPeepHoleAnimation(float, float):void");
    }

    public boolean onTouchEvent(MotionEvent ev) {
        boolean touchHandled = this.mGestureDetector.onTouchEvent(ev);
        if (ev.getActionMasked() == 1) {
            snap();
        }
        return touchHandled;
    }

    private void snap() {
        if (this.mScrollTrend >= 0.0f && this.mAnimationType == 1) {
            snapShadeTo(this.mHeight, 255);
        } else if (this.mScrollTrend <= 0.0f && this.mAnimationType == 2) {
            snapShadeTo(-this.mHeight, 255);
        } else if (this.mScrollTrend < 0.0f && this.mAnimationType == 1) {
            snapShadeTo(0, 0, false);
        } else if (this.mScrollTrend > 0.0f && this.mAnimationType == 2) {
            snapShadeTo(0, 0, false);
        }
    }

    private void snapShadeTo(int scrollDistance, int alpha) {
        snapShadeTo(scrollDistance, alpha, true);
    }

    private void snapShadeTo(final int scrollDistance, final int alpha, final boolean snapToFullScreen) {
        if (this.mAnimationType == 1 || this.mAnimationType == 2) {
            ObjectAnimator scrollAnimator = ObjectAnimator.ofFloat(this, "scrollDistance", new float[]{(float) scrollDistance});
            scrollAnimator.addListener(new AnimatorListener() {
                public void onAnimationStart(Animator animation) {
                }

                public void onAnimationEnd(Animator animation) {
                    ModeTransitionView.this.setScrollDistance((float) scrollDistance);
                    ModeTransitionView.this.mIconDrawable.setAlpha(alpha);
                    ModeTransitionView.this.mAnimationType = 0;
                    if (!snapToFullScreen) {
                        ModeTransitionView.this.setVisibility(8);
                    }
                    if (ModeTransitionView.this.mAnimationFinishedListener != null) {
                        ModeTransitionView.this.mAnimationFinishedListener.onAnimationFinished(snapToFullScreen);
                        ModeTransitionView.this.mAnimationFinishedListener = null;
                    }
                }

                public void onAnimationCancel(Animator animation) {
                }

                public void onAnimationRepeat(Animator animation) {
                }
            });
            scrollAnimator.setInterpolator(Gusterpolator.INSTANCE);
            scrollAnimator.start();
        }
    }

    public void prepareToPullUpShade(int shadeColorId, int iconId, AnimationFinishedListener listener) {
        prepareShadeAnimation(1, shadeColorId, iconId, listener);
    }

    public void prepareToPullDownShade(int shadeColorId, int modeIconResourceId, AnimationFinishedListener listener) {
        prepareShadeAnimation(2, shadeColorId, modeIconResourceId, listener);
    }

    private void prepareShadeAnimation(int animationType, int shadeColorId, int iconResId, AnimationFinishedListener listener) {
        this.mAnimationFinishedListener = listener;
        if (this.mPeepHoleAnimator != null && this.mPeepHoleAnimator.isRunning()) {
            this.mPeepHoleAnimator.end();
        }
        this.mAnimationType = animationType;
        resetShade(shadeColorId, iconResId);
    }

    private void resetShade(int shadeColorId, int modeIconResourceId) {
        int shadeColor = getResources().getColor(shadeColorId);
        this.mBackgroundColor = shadeColor;
        this.mShadePaint.setColor(shadeColor);
        setScrollDistance(0.0f);
        updateIconDrawableByResourceId(modeIconResourceId);
        this.mIconDrawable.setAlpha(0);
        setVisibility(0);
    }

    private void updateIconDrawableByResourceId(int modeIconResourceId) {
        Drawable iconDrawable = getResources().getDrawable(modeIconResourceId);
        if (iconDrawable == null) {
            Log.e(TAG, "Invalid resource id for icon drawable. Setting icon drawable to null.");
            setIconDrawable(null);
            return;
        }
        setIconDrawable(iconDrawable.mutate());
    }

    private void setIconDrawable(Drawable iconDrawable) {
        if (iconDrawable == null) {
            this.mIconDrawable = this.mDefaultDrawable;
        } else {
            this.mIconDrawable = iconDrawable;
        }
    }

    public void setupModeCover(int colorId, int modeIconResourceId) {
        this.mBackgroundBitmap = null;
        if (this.mPeepHoleAnimator != null && this.mPeepHoleAnimator.isRunning()) {
            this.mPeepHoleAnimator.cancel();
        }
        this.mAnimationType = 0;
        this.mBackgroundColor = getResources().getColor(colorId);
        updateIconDrawableByResourceId(modeIconResourceId);
        this.mIconDrawable.setAlpha(255);
        setVisibility(0);
    }

    public void hideModeCover(final AnimationFinishedListener animationFinishedListener) {
        if (this.mAnimationType == 0) {
            this.mAnimationType = 4;
            ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(this, "alpha", new float[]{1.0f, 0.0f});
            alphaAnimator.setDuration(100);
            alphaAnimator.setInterpolator(null);
            alphaAnimator.addListener(new AnimatorListener() {
                public void onAnimationStart(Animator animation) {
                }

                public void onAnimationEnd(Animator animation) {
                    ModeTransitionView.this.setVisibility(8);
                    ModeTransitionView.this.setAlpha(1.0f);
                    if (animationFinishedListener != null) {
                        animationFinishedListener.onAnimationFinished(true);
                        ModeTransitionView.this.mAnimationType = 0;
                    }
                }

                public void onAnimationCancel(Animator animation) {
                }

                public void onAnimationRepeat(Animator animation) {
                }
            });
            alphaAnimator.start();
        } else if (animationFinishedListener != null) {
            animationFinishedListener.onAnimationFinished(false);
        }
    }

    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
        int alphaScaled = (int) (255.0f * getAlpha());
        this.mBackgroundColor = (this.mBackgroundColor & ViewCompat.MEASURED_SIZE_MASK) | (alphaScaled << 24);
        this.mIconDrawable.setAlpha(alphaScaled);
    }

    public void setupModeCover(Bitmap screenShot) {
        Log.w(TAG, "setup Mode cover");
        this.mModeTransState = 1;
        this.mAnimationType = 5;
        this.mBackgroundBitmap = new BitmapPackager(screenShot);
        setVisibility(0);
    }

    public void setupModeCoverTileAnimationDone(Bitmap screenShot, OnTransAnimationListener... listeners) {
        setupModeCoverTileAnimationDone(screenShot, -1, listeners);
    }

    public void setupModeCoverTileAnimationDone(Bitmap screenShot, int duration, final OnTransAnimationListener... listeners) {
        Log.w(TAG, "setup Mode cover sync");
        if (screenShot != null) {
            StringBuilder stringBuilder;
            if (this.mShowAnimator != null) {
                Tag tag = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("animation running ?");
                stringBuilder.append(this.mShowAnimator.isRunning());
                stringBuilder.append(" alpha is ");
                stringBuilder.append(getAlpha());
                Log.w(tag, stringBuilder.toString());
            }
            if (this.mShowAnimator != null && (this.mShowAnimator.isRunning() || getVisibility() == 0)) {
                this.mShowAnimator.cancel();
            }
            Log.w(TAG, "start mode cover animation");
            sState = 1;
            sCreateTime = 0;
            this.mModeTransState = 1;
            this.mAnimationType = 5;
            this.mBackgroundBitmap = new BitmapPackager(screenShot);
            setAlpha(0.0f);
            setVisibility(0);
            if (this.mShowAnimator == null) {
                initializeBlurAnimator();
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("----------------------------->   ");
            stringBuilder.append(duration);
            android.util.Log.e("MSS", stringBuilder.toString());
            if (duration >= 0) {
                this.mShowAnimator.setDuration((long) duration);
                this.mHideCoverAnimator.setDuration((long) duration);
            } else {
                this.mShowAnimator.setDuration(200);
                this.mHideCoverAnimator.setDuration(100);
            }
            this.mShowAnimator.addListener(new AnimatorListener() {
                public void onAnimationStart(Animator animator) {
                    ModeTransitionView.sState = 2;
                }

                public void onAnimationEnd(Animator animator) {
                    if (!(listeners == null || listeners.length == 0)) {
                        for (OnTransAnimationListener listener : listeners) {
                            if (listener != null) {
                                listener.onAnimationDone();
                            }
                        }
                    }
                    Log.w(ModeTransitionView.TAG, "blur animation start end");
                    android.util.Log.d("20190111", "blur animation start end");
                    ModeTransitionView.this.mShowAnimator.removeListener(this);
                    ModeTransitionView.sState = 3;
                }

                public void onAnimationCancel(Animator animator) {
                    if (!(listeners == null || listeners.length == 0)) {
                        for (OnTransAnimationListener listener : listeners) {
                            if (listener != null) {
                                listener.onAnimationDone();
                            }
                        }
                    }
                    Log.w(ModeTransitionView.TAG, "blur animation start cancel");
                    ModeTransitionView.this.mShowAnimator.removeListener(this);
                }

                public void onAnimationRepeat(Animator animator) {
                }
            });
            this.mShowAnimator.start();
        }
    }

    private void initializeBlurAnimator() {
        this.mShowAnimator = ValueAnimator.ofFloat(new float[]{0.2f, 1.0f});
        this.mShowAnimator.setDuration(200);
        this.mShowAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                ModeTransitionView.this.setAlpha(((Float) valueAnimator.getAnimatedValue()).floatValue());
                ModeTransitionView.this.invalidate();
            }
        });
        this.mHideCoverAnimator = ValueAnimator.ofFloat(new float[]{1.0f, 0.0f});
        this.mHideCoverAnimator.setDuration(100);
        this.mHideCoverAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                ModeTransitionView.this.setAlpha(((Float) valueAnimator.getAnimatedValue()).floatValue());
                ModeTransitionView.this.invalidate();
            }
        });
    }

    public void hideImageCover() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - this.mLastHideTime >= 500) {
            this.mLastHideTime = currentTime;
            int delayedTime = 500;
            if (sDelayedTime > 0) {
                delayedTime = sDelayedTime;
                sDelayedTime = 0;
            }
            sState = 4;
            sCreateTime = System.currentTimeMillis();
            final long createTime = sCreateTime;
            postDelayed(new Runnable() {
                public void run() {
                    if (createTime == ModeTransitionView.sCreateTime || (ModeTransitionView.sCreateTime == 0 && createTime > 0)) {
                        ModeTransitionView.this.hideImageCoverDefault();
                    }
                }
            }, (long) delayedTime);
        }
    }

    public void hideImageCoverDefault() {
        Log.w(TAG, "call hideImageCover");
        if (getVisibility() == 8) {
            this.mBackgroundBitmap = null;
            setVisibility(8);
            this.mModeTransState = 0;
            this.mAnimationType = 0;
            if (this.mPendingRect != null) {
                this.mRect = this.mPendingRect;
                this.mPendingRect = null;
            }
            sState = 0;
        } else if (this.mAnimationType == 5) {
            Log.w(TAG, "try hide mode cover");
            if (this.mHideCoverAnimator == null) {
                initializeBlurAnimator();
            }
            if (!this.mHideCoverAnimator.isRunning()) {
                Log.w(TAG, "start hide mode cover");
                this.mHideCoverAnimator.addListener(new AnimatorListener() {
                    public void onAnimationStart(Animator animator) {
                        ModeTransitionView.sState = 5;
                    }

                    public void onAnimationEnd(Animator animator) {
                        Log.w(ModeTransitionView.TAG, "mode cover hidden");
                        ModeTransitionView.this.mBackgroundBitmap = null;
                        ModeTransitionView.this.setVisibility(8);
                        ModeTransitionView.this.mModeTransState = 0;
                        ModeTransitionView.this.mAnimationType = 0;
                        ModeTransitionView.this.mHideCoverAnimator.removeListener(this);
                        if (ModeTransitionView.this.mPendingRect != null) {
                            ModeTransitionView.this.mRect = ModeTransitionView.this.mPendingRect;
                            ModeTransitionView.this.mPendingRect = null;
                        }
                        ModeTransitionView.sState = 0;
                    }

                    public void onAnimationCancel(Animator animator) {
                        ModeTransitionView.this.mHideCoverAnimator.removeListener(this);
                        ModeTransitionView.this.mBackgroundBitmap = null;
                        ModeTransitionView.this.setVisibility(8);
                        ModeTransitionView.this.mModeTransState = 0;
                        ModeTransitionView.this.mAnimationType = 0;
                        if (ModeTransitionView.this.mPendingRect != null) {
                            ModeTransitionView.this.mRect = ModeTransitionView.this.mPendingRect;
                            ModeTransitionView.this.mPendingRect = null;
                        }
                    }

                    public void onAnimationRepeat(Animator animator) {
                    }
                });
                this.mHideCoverAnimator.start();
            }
        }
    }

    public void mapPreviewRect(RectF rectF) {
        if (this.mModeTransState == 1) {
            this.mPendingRect = new RectF(rectF);
        } else {
            this.mRect = new RectF(rectF);
        }
    }

    public Bitmap getBackgroundBitmap() {
        if (this.mBackgroundBitmap == null) {
            return null;
        }
        return this.mBackgroundBitmap.get();
    }

    public RectF getCoveredRect() {
        return this.mRect;
    }
}
