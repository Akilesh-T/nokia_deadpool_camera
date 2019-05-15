package com.hmdglobal.app.camera.widget;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.hmdglobal.app.camera.util.Gusterpolator;

public class PeekView extends ImageView {
    private static final float FILMSTRIP_SCALE = 0.8f;
    public static final long PEEK_IN_DURATION_MS = 300;
    public static final long PEEK_IN_FOR_UPDATE_MS = 600;
    private static final long PEEK_OUT_DURATION_MS = 200;
    private static final long PEEK_STAY_DURATION_MS = 100;
    private static final float ROTATE_ANGLE = -7.0f;
    private boolean mAnimationCanceled;
    private Rect mDrawableBound;
    private int mHeight;
    private Drawable mImageDrawable;
    private AnimatorSet mPeekAnimator;
    private float mPeekRotateAngle;
    private float mRotateScale;
    private Point mRotationPivot;
    private float mScaleX;
    private float mScaleY;
    private AnimatorSet mScalingPeekAnimator;
    private float mTransX;
    private float mTransY;
    private int mWidth;
    private int mX;
    private int mY;

    public static class Circle {
        public float centerPoint;
        public float radius;
    }

    public interface OnCaptureStateListener {
        void onCaptureAnimationComplete();
    }

    public PeekView(Context context) {
        super(context);
        init();
    }

    public PeekView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PeekView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        this.mRotationPivot = new Point();
    }

    /* Access modifiers changed, original: protected */
    public void onDraw(Canvas c) {
        super.onDraw(c);
        if (this.mImageDrawable != null) {
            c.save();
            if (this.mScaleX < 0.4f) {
                Path path = new Path();
                float circleX = (float) c.getWidth();
                float circleY = (float) c.getHeight();
                float layoutX = ((float) (this.mDrawableBound.width() / 2)) / ((float) this.mWidth);
                float layoutY = ((float) (this.mDrawableBound.height() / 2)) / ((float) this.mHeight);
                if (this.mDrawableBound.width() < this.mDrawableBound.height()) {
                    path.addCircle(((float) c.getWidth()) * layoutX, ((float) c.getHeight()) * layoutY, ((float) c.getWidth()) * layoutX, Direction.CW);
                } else {
                    path.addCircle(((float) c.getWidth()) * layoutX, ((float) c.getHeight()) * layoutY, ((float) c.getHeight()) * layoutY, Direction.CW);
                }
                c.clipPath(path);
            }
            this.mImageDrawable.setBounds(this.mDrawableBound);
            try {
                this.mImageDrawable.draw(c);
            } catch (Exception e) {
            }
            setPivotX(0.0f);
            setPivotY(0.0f);
            setTranslationX(this.mTransX);
            setTranslationY(this.mTransY);
            setScaleX(this.mScaleX);
            setScaleY(this.mScaleX);
            c.restore();
        }
    }

    /* JADX WARNING: Incorrect type for fill-array insn 0x000a, element type: float, insn element type: null */
    /* JADX WARNING: Incorrect type for fill-array insn 0x0013, element type: float, insn element type: null */
    /* JADX WARNING: Incorrect type for fill-array insn 0x001c, element type: float, insn element type: null */
    public void startPeekAnimation(android.graphics.Bitmap r18, boolean r19, java.lang.String r20) {
        /*
        r17 = this;
        r0 = r17;
        r1 = new com.hmdglobal.app.camera.widget.PeekView$1;
        r1.<init>();
        r2 = 2;
        r3 = new float[r2];
        r3 = {0, -1059061760};
        r3 = android.animation.ValueAnimator.ofFloat(r3);
        r4 = new float[r2];
        r4 = {-1059061760, -1059061760};
        r4 = android.animation.ValueAnimator.ofFloat(r4);
        r5 = new float[r2];
        r5 = {-1059061760, 0};
        r5 = android.animation.ValueAnimator.ofFloat(r5);
        r3.addUpdateListener(r1);
        r5.addUpdateListener(r1);
        r6 = 300; // 0x12c float:4.2E-43 double:1.48E-321;
        r3.setDuration(r6);
        r6 = 100;
        r4.setDuration(r6);
        r6 = 200; // 0xc8 float:2.8E-43 double:9.9E-322;
        r5.setDuration(r6);
        r6 = new android.view.animation.DecelerateInterpolator;
        r6.<init>();
        r3.setInterpolator(r6);
        r6 = new android.view.animation.AccelerateInterpolator;
        r6.<init>();
        r5.setInterpolator(r6);
        r6 = new android.animation.AnimatorSet;
        r6.<init>();
        r0.mPeekAnimator = r6;
        r6 = r0.mPeekAnimator;
        r7 = 3;
        r7 = new android.animation.Animator[r7];
        r8 = 0;
        r7[r8] = r3;
        r9 = 1;
        r7[r9] = r4;
        r7[r2] = r5;
        r6.playSequentially(r7);
        r6 = r0.mPeekAnimator;
        r7 = new com.hmdglobal.app.camera.widget.PeekView$2;
        r7.<init>();
        r6.addListener(r7);
        if (r19 == 0) goto L_0x006e;
    L_0x006b:
        r7 = 1065353216; // 0x3f800000 float:1.0 double:5.263544247E-315;
        goto L_0x0070;
    L_0x006e:
        r7 = 1056964608; // 0x3f000000 float:0.5 double:5.222099017E-315;
    L_0x0070:
        r0.mRotateScale = r7;
        r7 = new android.graphics.drawable.BitmapDrawable;
        r9 = r17.getResources();
        r10 = r18;
        r7.<init>(r9, r10);
        r0.mImageDrawable = r7;
        r7 = r0.mImageDrawable;
        r7 = r7.getIntrinsicWidth();
        r9 = r0.mImageDrawable;
        r9 = r9.getIntrinsicHeight();
        r11 = r17.getWidth();
        r11 = (float) r11;
        r12 = 1061997773; // 0x3f4ccccd float:0.8 double:5.246966156E-315;
        r11 = r11 * r12;
        r11 = (int) r11;
        r13 = r17.getHeight();
        r13 = (float) r13;
        r13 = r13 * r12;
        r12 = (int) r13;
        r7 = com.hmdglobal.app.camera.util.CameraUtil.resizeToFill(r7, r9, r8, r11, r12);
        r8 = r17.getMeasuredWidth();
        r9 = r17.getMeasuredHeight();
        r11 = r7.y;
        r9 = r9 - r11;
        r9 = r9 / r2;
        r2 = new android.graphics.Rect;
        r11 = r7.x;
        r11 = r11 + r8;
        r12 = r7.y;
        r12 = r12 + r9;
        r2.<init>(r8, r9, r11, r12);
        r0.mDrawableBound = r2;
        r2 = r0.mRotationPivot;
        r11 = (double) r9;
        r13 = r7.y;
        r13 = (double) r13;
        r15 = 4607632778762754458; // 0x3ff199999999999a float:-1.5881868E-23 double:1.1;
        r13 = r13 * r15;
        r11 = r11 + r13;
        r11 = (int) r11;
        r2.set(r8, r11);
        r2 = r0.mPeekAnimator;
        r2.start();
        r2 = r20;
        r0.announceForAccessibility(r2);
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.widget.PeekView.startPeekAnimation(android.graphics.Bitmap, boolean, java.lang.String):void");
    }

    /* Access modifiers changed, original: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int[] coords = new int[2];
        getLocationInWindow(coords);
        this.mX = coords[0];
        this.mY = coords[1];
        this.mWidth = getWidth();
        this.mHeight = getHeight();
    }

    public void startScalingPeekAnimation(Rect thumb, Bitmap bitmap, boolean strong, String accessibilityString, OnCaptureStateListener listener) {
        if (this.mScalingPeekAnimator == null || !this.mScalingPeekAnimator.isRunning()) {
            int deltaX;
            int deltaY;
            this.mImageDrawable = new BitmapDrawable(getResources(), bitmap);
            Point drawDim = CameraUtil.resizeToFill(this.mImageDrawable.getIntrinsicWidth(), this.mImageDrawable.getIntrinsicHeight(), 0, getWidth(), getHeight());
            this.mDrawableBound = new Rect(0, 0, drawDim.x, drawDim.y);
            if (getWidth() < getHeight()) {
                deltaX = thumb.left - this.mX;
                deltaY = (thumb.top - this.mY) - (((int) ((((float) (thumb.width() * drawDim.y)) / ((float) drawDim.x)) - ((float) thumb.height()))) / 2);
            } else {
                deltaY = thumb.top - this.mY;
                deltaX = (thumb.left - this.mX) - (((int) ((((float) (thumb.height() * drawDim.x)) / ((float) drawDim.y)) - ((float) thumb.width()))) / 2);
            }
            int width = thumb.width();
            float targetScaleY = ((float) thumb.height()) / ((float) this.mDrawableBound.height());
            playScalingAnimation(((float) width) / ((float) this.mDrawableBound.width()), deltaX, deltaY, listener);
            announceForAccessibility(accessibilityString);
        }
    }

    private void playScalingAnimation(float targetScaleX, int deltaX, int deltaY, final OnCaptureStateListener listener) {
        if (this.mScalingPeekAnimator == null) {
            ValueAnimator scalingXAnima = ValueAnimator.ofFloat(new float[]{1065353216, targetScaleX});
            scalingXAnima.setDuration(300);
            scalingXAnima.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    PeekView.this.mScaleX = ((Float) animation.getAnimatedValue()).floatValue();
                    PeekView.this.invalidate();
                }
            });
            ValueAnimator transXAnima = ValueAnimator.ofFloat(new float[]{null, (float) deltaX});
            transXAnima.setDuration(300);
            transXAnima.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    PeekView.this.mTransX = ((Float) animation.getAnimatedValue()).floatValue();
                    PeekView.this.invalidate();
                }
            });
            ValueAnimator transYAnima = ValueAnimator.ofFloat(new float[]{0.0f, (float) deltaY});
            transYAnima.setDuration(300);
            transYAnima.addUpdateListener(new AnimatorUpdateListener() {
                public void onAnimationUpdate(ValueAnimator animation) {
                    PeekView.this.mTransY = ((Float) animation.getAnimatedValue()).floatValue();
                    PeekView.this.invalidate();
                }
            });
            this.mScalingPeekAnimator = new AnimatorSet();
            this.mScalingPeekAnimator.setInterpolator(Gusterpolator.INSTANCE);
            this.mScalingPeekAnimator.playTogether(new Animator[]{scalingXAnima, transXAnima, transYAnima});
        }
        this.mScalingPeekAnimator.addListener(new AnimatorListener() {
            public void onAnimationStart(Animator animation) {
                PeekView.this.setVisibility(0);
            }

            public void onAnimationEnd(Animator animation) {
                PeekView.this.setVisibility(8);
                PeekView.this.setTranslationX(0.0f);
                PeekView.this.setTranslationY(0.0f);
                PeekView.this.setScaleX(1.0f);
                PeekView.this.setScaleY(1.0f);
                listener.onCaptureAnimationComplete();
                PeekView.this.mScalingPeekAnimator.removeListener(this);
            }

            public void onAnimationCancel(Animator animation) {
                PeekView.this.setVisibility(8);
                PeekView.this.setTranslationX(0.0f);
                PeekView.this.setTranslationY(0.0f);
                PeekView.this.setScaleX(1.0f);
                PeekView.this.setScaleY(1.0f);
                listener.onCaptureAnimationComplete();
                PeekView.this.mScalingPeekAnimator.removeListener(this);
            }

            public void onAnimationRepeat(Animator animation) {
            }
        });
        this.mScalingPeekAnimator.start();
    }

    public boolean isPeekAnimationRunning() {
        return this.mPeekAnimator.isRunning();
    }

    public void stopPeekAnimation() {
        if (isPeekAnimationRunning()) {
            this.mPeekAnimator.end();
        } else {
            clear();
        }
    }

    public void cancelPeekAnimation() {
        if (isPeekAnimationRunning()) {
            this.mPeekAnimator.cancel();
        } else {
            clear();
        }
    }

    private void clear() {
        setVisibility(4);
        setImageDrawable(null);
    }
}
