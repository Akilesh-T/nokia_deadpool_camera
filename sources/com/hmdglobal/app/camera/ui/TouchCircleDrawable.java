package com.hmdglobal.app.camera.ui;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.util.Gusterpolator;

public class TouchCircleDrawable extends Drawable {
    private static final int CIRCLE_ANIM_DURATION_MS = 250;
    private static final int INVALID = -1;
    private AnimatorListener mAnimatorListener;
    private Paint mBackgroundPaint;
    private int mBackgroundRadius;
    private Point mCenter;
    private int mColor;
    private int mColorAlpha;
    private Paint mColorPaint;
    private int mColorRadius;
    private boolean mDrawBackground;
    private int mH;
    private Drawable mIconDrawable;
    private int mIconDrawableSize;
    private AnimatorUpdateListener mUpdateListener;
    private int mW;

    public TouchCircleDrawable(Resources resources, int color, int baseColor) {
        this.mColorPaint = new Paint();
        this.mBackgroundPaint = new Paint();
        this.mColorAlpha = 255;
        this.mW = -1;
        this.mH = -1;
        this.mColorPaint.setAntiAlias(true);
        this.mBackgroundPaint.setAntiAlias(true);
        this.mBackgroundPaint.setColor(resources.getColor(R.color.mode_icon_hover_highlight));
        setColor(color);
    }

    public TouchCircleDrawable(Resources resources) {
        this(resources, ViewCompat.MEASURED_SIZE_MASK, ViewCompat.MEASURED_SIZE_MASK);
    }

    public void setSize(int w, int h) {
        this.mW = w;
        this.mH = h;
    }

    public void setCenter(Point p) {
        this.mCenter = p;
        updateIconBounds();
    }

    public Point getCenter() {
        return this.mCenter;
    }

    public void draw(Canvas canvas) {
        int w = this.mW;
        int h = this.mH;
        if (w != -1 && h != -1 && this.mCenter != null) {
            if (this.mDrawBackground) {
                canvas.drawCircle((float) this.mCenter.x, (float) this.mCenter.y, (float) this.mBackgroundRadius, this.mBackgroundPaint);
            }
            canvas.drawCircle((float) this.mCenter.x, (float) this.mCenter.y, (float) this.mColorRadius, this.mColorPaint);
            if (this.mIconDrawable != null) {
                this.mIconDrawable.draw(canvas);
            }
        }
    }

    public void setAlpha(int alpha) {
        this.mColorAlpha = alpha;
    }

    public void setColorFilter(ColorFilter cf) {
        this.mColorPaint.setColorFilter(cf);
    }

    public int getOpacity() {
        return -3;
    }

    public void setColor(int color) {
        this.mColor = color;
        this.mColorPaint.setColor(this.mColor);
        this.mColorPaint.setAlpha(this.mColorAlpha);
    }

    public void setIconDrawable(Drawable d, int size) {
        this.mIconDrawable = d;
        this.mIconDrawableSize = size;
        updateIconBounds();
    }

    private void updateIconBounds() {
        if (this.mCenter != null) {
            this.mIconDrawable.setBounds(this.mCenter.x - (this.mIconDrawableSize / 2), this.mCenter.y - (this.mIconDrawableSize / 2), this.mCenter.x + (this.mIconDrawableSize / 2), this.mCenter.y + (this.mIconDrawableSize / 2));
        }
    }

    public void animate() {
        this.mBackgroundRadius = Math.min(this.mW / 2, this.mH / 2);
        ValueAnimator colorAnimator = ValueAnimator.ofInt(new int[]{null, Math.min(this.mW / 2, this.mH / 2)});
        colorAnimator.setDuration(250);
        colorAnimator.setInterpolator(Gusterpolator.INSTANCE);
        colorAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                TouchCircleDrawable.this.mColorRadius = ((Integer) animation.getAnimatedValue()).intValue();
                TouchCircleDrawable.this.invalidateSelf();
                if (TouchCircleDrawable.this.mUpdateListener != null) {
                    TouchCircleDrawable.this.mUpdateListener.onAnimationUpdate(animation);
                }
            }
        });
        colorAnimator.addListener(new AnimatorListener() {
            public void onAnimationStart(Animator animation) {
                TouchCircleDrawable.this.mDrawBackground = true;
                if (TouchCircleDrawable.this.mAnimatorListener != null) {
                    TouchCircleDrawable.this.mAnimatorListener.onAnimationStart(animation);
                }
            }

            public void onAnimationEnd(Animator animation) {
                TouchCircleDrawable.this.mDrawBackground = false;
                if (TouchCircleDrawable.this.mAnimatorListener != null) {
                    TouchCircleDrawable.this.mAnimatorListener.onAnimationEnd(animation);
                }
            }

            public void onAnimationCancel(Animator animation) {
                TouchCircleDrawable.this.mDrawBackground = false;
                if (TouchCircleDrawable.this.mAnimatorListener != null) {
                    TouchCircleDrawable.this.mAnimatorListener.onAnimationCancel(animation);
                }
            }

            public void onAnimationRepeat(Animator animation) {
                if (TouchCircleDrawable.this.mAnimatorListener != null) {
                    TouchCircleDrawable.this.mAnimatorListener.onAnimationRepeat(animation);
                }
            }
        });
        colorAnimator.start();
    }

    public void reset() {
        this.mColorRadius = 0;
    }

    public void setAnimatorListener(AnimatorListener listener) {
        this.mAnimatorListener = listener;
    }

    public void setUpdateListener(AnimatorUpdateListener listener) {
        this.mUpdateListener = listener;
    }
}
