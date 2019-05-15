package com.hmdglobal.app.camera.ui;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.View;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.util.CustomFields;
import com.hmdglobal.app.camera.util.CustomUtil;

public class StereoScrollIndicatorView extends View implements ScrollIndicator {
    private AnimatorSet mAnimatorSet;
    private ColorDrawable mIndicatorColorDrawable;
    private int mIndicatorWidth;
    private ValueAnimator mTransAnimator;
    private int mTransX = 0;
    private ValueAnimator mWidthAnimator;

    public StereoScrollIndicatorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mIndicatorColorDrawable = new ColorDrawable(context.getResources().getColor(R.color.mode_scroll_bar_color_select));
        setBackgroundColor(0);
    }

    public void animateWidth(int from, int to, int duration) {
        if (this.mWidthAnimator != null && this.mWidthAnimator.isRunning()) {
            this.mWidthAnimator.cancel();
            this.mIndicatorWidth = to;
            invalidate();
        }
        this.mWidthAnimator = ValueAnimator.ofInt(new int[]{from, to});
        this.mWidthAnimator.setDuration((long) duration);
        this.mWidthAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                StereoScrollIndicatorView.this.mIndicatorWidth = ((Integer) valueAnimator.getAnimatedValue()).intValue();
                StereoScrollIndicatorView.this.invalidate();
            }
        });
        this.mWidthAnimator.start();
    }

    public void animateTrans(int fromWidth, int toWidth, int fromTrans, int toTrans, int duration) {
        if (duration < 0) {
            this.mTransX = toTrans;
            this.mIndicatorWidth = toWidth;
            invalidate();
            return;
        }
        if (this.mAnimatorSet != null && this.mAnimatorSet.isRunning()) {
            this.mAnimatorSet.cancel();
            fromTrans = this.mTransX;
            fromWidth = this.mIndicatorWidth;
        }
        this.mTransAnimator = ValueAnimator.ofInt(new int[]{fromTrans, toTrans});
        this.mTransAnimator.setDuration((long) duration);
        this.mTransAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                StereoScrollIndicatorView.this.mTransX = ((Integer) valueAnimator.getAnimatedValue()).intValue();
                StereoScrollIndicatorView.this.invalidate();
            }
        });
        this.mWidthAnimator = ValueAnimator.ofInt(new int[]{fromWidth, toWidth});
        this.mWidthAnimator.setDuration((long) duration);
        this.mWidthAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                StereoScrollIndicatorView.this.mIndicatorWidth = ((Integer) valueAnimator.getAnimatedValue()).intValue();
                StereoScrollIndicatorView.this.invalidate();
            }
        });
        this.mAnimatorSet = new AnimatorSet();
        this.mAnimatorSet.playTogether(new Animator[]{this.mWidthAnimator, this.mTransAnimator});
        this.mAnimatorSet.start();
    }

    public int getIndicatorTransX() {
        return this.mTransX;
    }

    public void initializeWidth(int width) {
        this.mIndicatorWidth = width;
        invalidate();
        this.mAnimatorSet = null;
        this.mWidthAnimator = null;
        this.mTransAnimator = null;
    }

    /* Access modifiers changed, original: protected */
    public void onDraw(Canvas canvas) {
        if (!CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_FIX_MODE_SWITCHING, true)) {
            int canvasCenter = canvas.getWidth() / 2;
            this.mIndicatorColorDrawable.setBounds((this.mTransX + canvasCenter) - (this.mIndicatorWidth / 2), 0, (this.mTransX + canvasCenter) + (this.mIndicatorWidth / 2), canvas.getHeight());
            this.mIndicatorColorDrawable.draw(canvas);
            super.onDraw(canvas);
            setVisibility(8);
        }
    }
}
