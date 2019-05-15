package com.hmdglobal.app.camera.ui;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.debug.Log.Tag;

public class EvoSlider extends View {
    private static final int ALPHA_GRADIENT_DURATION = 200;
    private static final float ALPHA_GRADIENT_END = 0.5f;
    private static final float ALPHA_GRADIENT_START = 1.0f;
    private static final float MAX_FRACTION = 1.0f;
    private static final float MIN_FRACTION = 0.3f;
    private static final int ORIENTATION_LANDSCAPE = 1;
    private static final int ORIENTATION_PORTRAIT = 0;
    private static final int STAY_GRADIENT_DELAY = 1000;
    private static final Tag TAG = new Tag("EvoSlider");
    private static float mFraction = 1.0f;
    private final float BUTTON_RADIUS;
    private final float DASH;
    private final int SLIDER_LENGTH;
    private final int TOUCH_GAP;
    private float mAlpha = 1.0f;
    private int mBoundBottom = 0;
    private int mBoundRight = 0;
    private Paint mDashPaint;
    EVOChangedListener mEVOListener;
    private final Drawable mExposureIconDrawable;
    private final ValueAnimator mGradientAnimation = new ValueAnimator();
    private int mLowerBoundForSlider = 0;
    private int mMaxCompensationSteps = 0;
    private int mMinCompensationSteps = 0;
    private boolean mNeedShowStroke = false;
    private int mOrientation = 0;
    private float mOrigin = 0.0f;
    private Paint mSolidPaint;
    private float mStep = 0.0f;
    private int mUpperBoundForSlider = 0;
    private float mX;
    private float mY;

    public interface EVOChangedListener {
        void onEVOChanged(int i);
    }

    public EvoSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.TOUCH_GAP = (int) context.getResources().getDimension(R.dimen.evo_slider_gap);
        this.SLIDER_LENGTH = (int) context.getResources().getDimension(R.dimen.evo_slider_length);
        this.BUTTON_RADIUS = (float) ((int) context.getResources().getDimension(R.dimen.evo_slider_icon_radius));
        this.DASH = context.getResources().getDimension(R.dimen.evo_slider_stroke_dash);
        this.mExposureIconDrawable = context.getResources().getDrawable(R.drawable.ic_focus_exposure);
        setVisibility(8);
        initAnimation();
    }

    public EvoSlider(Context context) {
        super(context);
        this.TOUCH_GAP = (int) context.getResources().getDimension(R.dimen.evo_slider_gap);
        this.SLIDER_LENGTH = (int) context.getResources().getDimension(R.dimen.evo_slider_length);
        this.BUTTON_RADIUS = (float) ((int) context.getResources().getDimension(R.dimen.evo_slider_icon_radius));
        this.DASH = context.getResources().getDimension(R.dimen.evo_slider_stroke_dash);
        this.mExposureIconDrawable = context.getResources().getDrawable(R.drawable.ic_focus_exposure);
        setVisibility(8);
        initAnimation();
    }

    private void initAnimation() {
        this.mGradientAnimation.setFloatValues(new float[]{1.0f, ALPHA_GRADIENT_END});
        this.mGradientAnimation.setDuration(200);
        this.mGradientAnimation.setStartDelay(1000);
        this.mGradientAnimation.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                EvoSlider.this.mAlpha = ((Float) valueAnimator.getAnimatedValue()).floatValue();
                EvoSlider.this.invalidate();
            }
        });
        this.mGradientAnimation.addListener(new AnimatorListener() {
            public void onAnimationStart(Animator animator) {
            }

            public void onAnimationEnd(Animator animator) {
            }

            public void onAnimationCancel(Animator animator) {
            }

            public void onAnimationRepeat(Animator animator) {
            }
        });
    }

    public void setEvoListener(EVOChangedListener listener) {
        this.mEVOListener = listener;
    }

    private float getFraction() {
        return mFraction;
    }

    public void slideTo(float deltaX, float deltaY) {
        float deltaStep;
        this.mGradientAnimation.cancel();
        this.mAlpha = 1.0f;
        if (this.mOrientation == 0) {
            deltaStep = getFraction() * deltaY;
        } else {
            deltaStep = getFraction() * deltaX;
        }
        if (deltaStep != 0.0f) {
            float expectValue = this.mStep + deltaStep;
            if (expectValue <= ((float) this.mUpperBoundForSlider) && expectValue >= ((float) this.mLowerBoundForSlider)) {
                this.mStep += deltaStep;
                if (this.mEVOListener != null) {
                    float realValue = (this.mStep - this.mOrigin) * (((float) (this.mMaxCompensationSteps - this.mMinCompensationSteps)) / ((float) (this.mUpperBoundForSlider - this.mLowerBoundForSlider)));
                    int totalSteps = (int) realValue;
                    float tolerance = realValue - ((float) totalSteps);
                    if (((double) tolerance) >= 0.5d) {
                        totalSteps++;
                    } else if (((double) tolerance) <= -0.5d) {
                        totalSteps--;
                    }
                    if (totalSteps > this.mMaxCompensationSteps) {
                        totalSteps = this.mMaxCompensationSteps;
                    }
                    if (totalSteps < this.mMinCompensationSteps) {
                        totalSteps = this.mMinCompensationSteps;
                    }
                    this.mEVOListener.onEVOChanged(totalSteps);
                }
                this.mNeedShowStroke = true;
                invalidate();
            }
        }
    }

    public void setValueBound(int maxCompensation, int minCompensation) {
        this.mUpperBoundForSlider = this.SLIDER_LENGTH;
        this.mLowerBoundForSlider = 0;
        this.mMaxCompensationSteps = maxCompensation;
        this.mMinCompensationSteps = minCompensation;
        this.mOrigin = (float) ((this.SLIDER_LENGTH * Math.abs(minCompensation)) / (Math.abs(maxCompensation) + Math.abs(minCompensation)));
    }

    public void resetSlider(int value) {
        if (value == 0) {
            this.mStep = this.mOrigin;
        } else {
            this.mStep = (float) ((this.SLIDER_LENGTH * (value - this.mMinCompensationSteps)) / (this.mMaxCompensationSteps - this.mMinCompensationSteps));
        }
        this.mNeedShowStroke = false;
        if (this.mEVOListener != null) {
            this.mEVOListener.onEVOChanged(value);
        }
    }

    public void resetSlider() {
        resetSlider(0);
    }

    public synchronized void setCoord(float x, float y) {
        this.mX = x;
        this.mY = y;
        invalidate();
    }

    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility == 0) {
            this.mAlpha = 1.0f;
            this.mGradientAnimation.start();
        } else {
            this.mNeedShowStroke = false;
        }
        invalidate();
    }

    public synchronized void setBound(int right, int bottom) {
        this.mBoundRight = right;
        this.mBoundBottom = bottom;
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() != 1) {
            return false;
        }
        if (event.getAction() != 2) {
            return super.onTouchEvent(event);
        }
        if (event.getHistorySize() == 0) {
            return true;
        }
        float startX = event.getHistoricalX(0);
        slideTo(event.getX() - startX, event.getHistoricalY(0) - event.getY());
        return true;
    }

    private Paint initPaint(boolean needDash) {
        Paint paint = new Paint();
        paint.setColor(-1);
        if (needDash) {
            paint.setPathEffect(new DashPathEffect(new float[]{this.DASH, this.DASH}, 1.0f));
        }
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(1.0f);
        return paint;
    }

    /* Access modifiers changed, original: protected|declared_synchronized */
    public synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        setAlpha(this.mAlpha);
        if (this.mDashPaint == null || this.mSolidPaint == null) {
            this.mDashPaint = initPaint(true);
            this.mSolidPaint = initPaint(false);
        }
        PointF start = getSliderStart();
        PointF end = getSliderEnd();
        PointF buttonCoord = getSliderButtonCoord();
        Path dashPath = new Path();
        dashPath.moveTo(start.x, start.y);
        dashPath.lineTo(buttonCoord.x, buttonCoord.y);
        Path solidPath = new Path();
        solidPath.moveTo(end.x, end.y);
        solidPath.lineTo(buttonCoord.x, buttonCoord.y);
        if (this.mNeedShowStroke) {
            canvas.drawPath(solidPath, this.mSolidPaint);
            canvas.drawPath(dashPath, this.mDashPaint);
        }
        this.mExposureIconDrawable.setBounds(new Rect((int) (buttonCoord.x - this.BUTTON_RADIUS), (int) (buttonCoord.y - this.BUTTON_RADIUS), (int) (buttonCoord.x + this.BUTTON_RADIUS), (int) (buttonCoord.y + this.BUTTON_RADIUS)));
        this.mExposureIconDrawable.draw(canvas);
    }

    private PointF getSliderCenterCoord() {
        PointF p = new PointF();
        if (this.mOrientation == 0) {
            if (this.mX + ((float) this.TOUCH_GAP) > ((float) this.mBoundRight)) {
                p.x = this.mX - ((float) this.TOUCH_GAP);
            } else {
                p.x = this.mX + ((float) this.TOUCH_GAP);
            }
            p.y = (float) ((int) this.mY);
        } else {
            if (this.mY + ((float) this.TOUCH_GAP) > ((float) this.mBoundBottom)) {
                p.y = this.mY + ((float) this.TOUCH_GAP);
            } else {
                p.y = this.mY - ((float) this.TOUCH_GAP);
            }
            p.x = this.mX;
        }
        return p;
    }

    private PointF getSliderStart() {
        PointF center = getSliderCenterCoord();
        PointF startPoint = new PointF();
        if (this.mOrientation == 0) {
            startPoint.y = center.y - ((float) (this.SLIDER_LENGTH / 2));
            startPoint.x = center.x;
        } else {
            startPoint.x = center.x + ((float) (this.SLIDER_LENGTH / 2));
            startPoint.y = center.y;
        }
        return startPoint;
    }

    private PointF getSliderEnd() {
        PointF center = getSliderCenterCoord();
        PointF endPoint = new PointF();
        if (this.mOrientation == 0) {
            endPoint.y = center.y + ((float) (this.SLIDER_LENGTH / 2));
            endPoint.x = center.x;
        } else {
            endPoint.x = center.x - ((float) (this.SLIDER_LENGTH / 2));
            endPoint.y = center.y;
        }
        return endPoint;
    }

    private PointF getSliderButtonCoord() {
        PointF end = getSliderEnd();
        PointF buttonCoord = new PointF();
        if (this.mOrientation == 0) {
            buttonCoord.x = end.x;
            buttonCoord.y = end.y - this.mStep;
        } else {
            buttonCoord.y = end.y;
            buttonCoord.x = end.x - this.mStep;
        }
        return buttonCoord;
    }
}
