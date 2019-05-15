package com.hmdglobal.app.camera.ui;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.graphics.Canvas;
import com.hmdglobal.app.camera.util.Gusterpolator;

public class AnimatedCircleDrawable extends CustomizeDrawable {
    private static final int CIRCLE_ANIM_DURATION_MS = 300;
    private static int DRAWABLE_MAX_LEVEL = 10000;
    private int mCanvasHeight;
    private int mCanvasWidth;
    private int mRadius;
    private int mSmallRadiusTarget;

    public AnimatedCircleDrawable(int smallRadiusTarget) {
        this.mSmallRadiusTarget = smallRadiusTarget;
    }

    public boolean onLevelChange(int level) {
        invalidateSelf();
        return true;
    }

    public void animateToSmallRadius() {
        int smallLevel = map(this.mSmallRadiusTarget, 0, diagonalLength(this.mCanvasWidth, this.mCanvasHeight) / 2, 0, DRAWABLE_MAX_LEVEL);
        ValueAnimator animator = ValueAnimator.ofInt(new int[]{getLevel(), smallLevel});
        animator.setDuration(300);
        animator.setInterpolator(Gusterpolator.INSTANCE);
        animator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                AnimatedCircleDrawable.this.setLevel(((Integer) animation.getAnimatedValue()).intValue());
            }
        });
        animator.start();
    }

    public void animateToFullSize() {
        ValueAnimator animator = ValueAnimator.ofInt(new int[]{getLevel(), DRAWABLE_MAX_LEVEL});
        animator.setDuration(300);
        animator.setInterpolator(Gusterpolator.INSTANCE);
        animator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                AnimatedCircleDrawable.this.setLevel(((Integer) animation.getAnimatedValue()).intValue());
            }
        });
        animator.start();
    }

    public void draw(Canvas canvas) {
        this.mCanvasWidth = canvas.getWidth();
        this.mCanvasHeight = canvas.getHeight();
        this.mRadius = map(getLevel(), 0, DRAWABLE_MAX_LEVEL, 0, diagonalLength(canvas.getWidth(), canvas.getHeight()) / 2);
        canvas.drawCircle(((float) canvas.getWidth()) / 2.0f, ((float) canvas.getHeight()) / 2.0f, (float) this.mRadius, getPaint());
    }

    private static int map(int x, int in_min, int in_max, int out_min, int out_max) {
        return (((x - in_min) * (out_max - out_min)) / (in_max - in_min)) + out_min;
    }

    private static int diagonalLength(int w, int h) {
        return (int) Math.sqrt((double) ((w * w) + (h * h)));
    }
}
