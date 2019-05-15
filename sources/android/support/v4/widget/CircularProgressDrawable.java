package android.support.v4.widget;

import android.animation.Animator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.FillType;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;
import android.support.v4.util.Preconditions;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class CircularProgressDrawable extends Drawable implements Animatable {
    private static final int ANIMATION_DURATION = 1332;
    private static final int ARROW_HEIGHT = 5;
    private static final int ARROW_HEIGHT_LARGE = 6;
    private static final int ARROW_WIDTH = 10;
    private static final int ARROW_WIDTH_LARGE = 12;
    private static final float CENTER_RADIUS = 7.5f;
    private static final float CENTER_RADIUS_LARGE = 11.0f;
    private static final int[] COLORS = new int[]{ViewCompat.MEASURED_STATE_MASK};
    private static final float COLOR_CHANGE_OFFSET = 0.75f;
    public static final int DEFAULT = 1;
    private static final float GROUP_FULL_ROTATION = 216.0f;
    public static final int LARGE = 0;
    private static final Interpolator LINEAR_INTERPOLATOR = new LinearInterpolator();
    private static final Interpolator MATERIAL_INTERPOLATOR = new FastOutSlowInInterpolator();
    private static final float MAX_PROGRESS_ARC = 0.8f;
    private static final float MIN_PROGRESS_ARC = 0.01f;
    private static final float RING_ROTATION = 0.20999998f;
    private static final float SHRINK_OFFSET = 0.5f;
    private static final float STROKE_WIDTH = 2.5f;
    private static final float STROKE_WIDTH_LARGE = 3.0f;
    private Animator mAnimator;
    private boolean mFinishing;
    private Resources mResources;
    private final Ring mRing = new Ring();
    private float mRotation;
    private float mRotationCount;

    @RestrictTo({Scope.LIBRARY_GROUP})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ProgressDrawableSize {
    }

    private static class Ring {
        int mAlpha = 255;
        Path mArrow;
        int mArrowHeight;
        final Paint mArrowPaint = new Paint();
        float mArrowScale = 1.0f;
        int mArrowWidth;
        final Paint mCirclePaint = new Paint();
        int mColorIndex;
        int[] mColors;
        int mCurrentColor;
        float mEndTrim = 0.0f;
        final Paint mPaint = new Paint();
        float mRingCenterRadius;
        float mRotation = 0.0f;
        boolean mShowArrow;
        float mStartTrim = 0.0f;
        float mStartingEndTrim;
        float mStartingRotation;
        float mStartingStartTrim;
        float mStrokeWidth = 5.0f;
        final RectF mTempBounds = new RectF();

        Ring() {
            this.mPaint.setStrokeCap(Cap.SQUARE);
            this.mPaint.setAntiAlias(true);
            this.mPaint.setStyle(Style.STROKE);
            this.mArrowPaint.setStyle(Style.FILL);
            this.mArrowPaint.setAntiAlias(true);
            this.mCirclePaint.setColor(0);
        }

        /* Access modifiers changed, original: 0000 */
        public void setArrowDimensions(float width, float height) {
            this.mArrowWidth = (int) width;
            this.mArrowHeight = (int) height;
        }

        /* Access modifiers changed, original: 0000 */
        public void setStrokeCap(Cap strokeCap) {
            this.mPaint.setStrokeCap(strokeCap);
        }

        /* Access modifiers changed, original: 0000 */
        public Cap getStrokeCap() {
            return this.mPaint.getStrokeCap();
        }

        /* Access modifiers changed, original: 0000 */
        public float getArrowWidth() {
            return (float) this.mArrowWidth;
        }

        /* Access modifiers changed, original: 0000 */
        public float getArrowHeight() {
            return (float) this.mArrowHeight;
        }

        /* Access modifiers changed, original: 0000 */
        public void draw(Canvas c, Rect bounds) {
            RectF arcBounds = this.mTempBounds;
            float arcRadius = this.mRingCenterRadius + (this.mStrokeWidth / 2.0f);
            if (this.mRingCenterRadius <= 0.0f) {
                arcRadius = (((float) Math.min(bounds.width(), bounds.height())) / 2.0f) - Math.max((((float) this.mArrowWidth) * this.mArrowScale) / 2.0f, this.mStrokeWidth / 2.0f);
            }
            float arcRadius2 = arcRadius;
            arcBounds.set(((float) bounds.centerX()) - arcRadius2, ((float) bounds.centerY()) - arcRadius2, ((float) bounds.centerX()) + arcRadius2, ((float) bounds.centerY()) + arcRadius2);
            float startAngle = (this.mStartTrim + this.mRotation) * 360.0f;
            float sweepAngle = ((this.mEndTrim + this.mRotation) * 360.0f) - startAngle;
            this.mPaint.setColor(this.mCurrentColor);
            this.mPaint.setAlpha(this.mAlpha);
            float inset = this.mStrokeWidth / 2.0f;
            arcBounds.inset(inset, inset);
            c.drawCircle(arcBounds.centerX(), arcBounds.centerY(), arcBounds.width() / 2.0f, this.mCirclePaint);
            arcBounds.inset(-inset, -inset);
            c.drawArc(arcBounds, startAngle, sweepAngle, false, this.mPaint);
            drawTriangle(c, startAngle, sweepAngle, arcBounds);
        }

        /* Access modifiers changed, original: 0000 */
        public void drawTriangle(Canvas c, float startAngle, float sweepAngle, RectF bounds) {
            if (this.mShowArrow) {
                if (this.mArrow == null) {
                    this.mArrow = new Path();
                    this.mArrow.setFillType(FillType.EVEN_ODD);
                } else {
                    this.mArrow.reset();
                }
                float centerRadius = Math.min(bounds.width(), bounds.height()) / 2.0f;
                float inset = (((float) this.mArrowWidth) * this.mArrowScale) / 2.0f;
                this.mArrow.moveTo(0.0f, 0.0f);
                this.mArrow.lineTo(((float) this.mArrowWidth) * this.mArrowScale, 0.0f);
                this.mArrow.lineTo((((float) this.mArrowWidth) * this.mArrowScale) / 2.0f, ((float) this.mArrowHeight) * this.mArrowScale);
                this.mArrow.offset((bounds.centerX() + centerRadius) - inset, bounds.centerY() + (this.mStrokeWidth / 2.0f));
                this.mArrow.close();
                this.mArrowPaint.setColor(this.mCurrentColor);
                this.mArrowPaint.setAlpha(this.mAlpha);
                c.save();
                c.rotate(startAngle + sweepAngle, bounds.centerX(), bounds.centerY());
                c.drawPath(this.mArrow, this.mArrowPaint);
                c.restore();
            }
        }

        /* Access modifiers changed, original: 0000 */
        public void setColors(@NonNull int[] colors) {
            this.mColors = colors;
            setColorIndex(0);
        }

        /* Access modifiers changed, original: 0000 */
        public int[] getColors() {
            return this.mColors;
        }

        /* Access modifiers changed, original: 0000 */
        public void setColor(int color) {
            this.mCurrentColor = color;
        }

        /* Access modifiers changed, original: 0000 */
        public void setBackgroundColor(int color) {
            this.mCirclePaint.setColor(color);
        }

        /* Access modifiers changed, original: 0000 */
        public int getBackgroundColor() {
            return this.mCirclePaint.getColor();
        }

        /* Access modifiers changed, original: 0000 */
        public void setColorIndex(int index) {
            this.mColorIndex = index;
            this.mCurrentColor = this.mColors[this.mColorIndex];
        }

        /* Access modifiers changed, original: 0000 */
        public int getNextColor() {
            return this.mColors[getNextColorIndex()];
        }

        /* Access modifiers changed, original: 0000 */
        public int getNextColorIndex() {
            return (this.mColorIndex + 1) % this.mColors.length;
        }

        /* Access modifiers changed, original: 0000 */
        public void goToNextColor() {
            setColorIndex(getNextColorIndex());
        }

        /* Access modifiers changed, original: 0000 */
        public void setColorFilter(ColorFilter filter) {
            this.mPaint.setColorFilter(filter);
        }

        /* Access modifiers changed, original: 0000 */
        public void setAlpha(int alpha) {
            this.mAlpha = alpha;
        }

        /* Access modifiers changed, original: 0000 */
        public int getAlpha() {
            return this.mAlpha;
        }

        /* Access modifiers changed, original: 0000 */
        public void setStrokeWidth(float strokeWidth) {
            this.mStrokeWidth = strokeWidth;
            this.mPaint.setStrokeWidth(strokeWidth);
        }

        /* Access modifiers changed, original: 0000 */
        public float getStrokeWidth() {
            return this.mStrokeWidth;
        }

        /* Access modifiers changed, original: 0000 */
        public void setStartTrim(float startTrim) {
            this.mStartTrim = startTrim;
        }

        /* Access modifiers changed, original: 0000 */
        public float getStartTrim() {
            return this.mStartTrim;
        }

        /* Access modifiers changed, original: 0000 */
        public float getStartingStartTrim() {
            return this.mStartingStartTrim;
        }

        /* Access modifiers changed, original: 0000 */
        public float getStartingEndTrim() {
            return this.mStartingEndTrim;
        }

        /* Access modifiers changed, original: 0000 */
        public int getStartingColor() {
            return this.mColors[this.mColorIndex];
        }

        /* Access modifiers changed, original: 0000 */
        public void setEndTrim(float endTrim) {
            this.mEndTrim = endTrim;
        }

        /* Access modifiers changed, original: 0000 */
        public float getEndTrim() {
            return this.mEndTrim;
        }

        /* Access modifiers changed, original: 0000 */
        public void setRotation(float rotation) {
            this.mRotation = rotation;
        }

        /* Access modifiers changed, original: 0000 */
        public float getRotation() {
            return this.mRotation;
        }

        /* Access modifiers changed, original: 0000 */
        public void setCenterRadius(float centerRadius) {
            this.mRingCenterRadius = centerRadius;
        }

        /* Access modifiers changed, original: 0000 */
        public float getCenterRadius() {
            return this.mRingCenterRadius;
        }

        /* Access modifiers changed, original: 0000 */
        public void setShowArrow(boolean show) {
            if (this.mShowArrow != show) {
                this.mShowArrow = show;
            }
        }

        /* Access modifiers changed, original: 0000 */
        public boolean getShowArrow() {
            return this.mShowArrow;
        }

        /* Access modifiers changed, original: 0000 */
        public void setArrowScale(float scale) {
            if (scale != this.mArrowScale) {
                this.mArrowScale = scale;
            }
        }

        /* Access modifiers changed, original: 0000 */
        public float getArrowScale() {
            return this.mArrowScale;
        }

        /* Access modifiers changed, original: 0000 */
        public float getStartingRotation() {
            return this.mStartingRotation;
        }

        /* Access modifiers changed, original: 0000 */
        public void storeOriginals() {
            this.mStartingStartTrim = this.mStartTrim;
            this.mStartingEndTrim = this.mEndTrim;
            this.mStartingRotation = this.mRotation;
        }

        /* Access modifiers changed, original: 0000 */
        public void resetOriginals() {
            this.mStartingStartTrim = 0.0f;
            this.mStartingEndTrim = 0.0f;
            this.mStartingRotation = 0.0f;
            setStartTrim(0.0f);
            setEndTrim(0.0f);
            setRotation(0.0f);
        }
    }

    public CircularProgressDrawable(@NonNull Context context) {
        this.mResources = ((Context) Preconditions.checkNotNull(context)).getResources();
        this.mRing.setColors(COLORS);
        setStrokeWidth(STROKE_WIDTH);
        setupAnimators();
    }

    private void setSizeParameters(float centerRadius, float strokeWidth, float arrowWidth, float arrowHeight) {
        Ring ring = this.mRing;
        float screenDensity = this.mResources.getDisplayMetrics().density;
        ring.setStrokeWidth(strokeWidth * screenDensity);
        ring.setCenterRadius(centerRadius * screenDensity);
        ring.setColorIndex(0);
        ring.setArrowDimensions(arrowWidth * screenDensity, arrowHeight * screenDensity);
    }

    public void setStyle(int size) {
        if (size == 0) {
            setSizeParameters(CENTER_RADIUS_LARGE, STROKE_WIDTH_LARGE, 12.0f, 6.0f);
        } else {
            setSizeParameters(CENTER_RADIUS, STROKE_WIDTH, 10.0f, 5.0f);
        }
        invalidateSelf();
    }

    public float getStrokeWidth() {
        return this.mRing.getStrokeWidth();
    }

    public void setStrokeWidth(float strokeWidth) {
        this.mRing.setStrokeWidth(strokeWidth);
        invalidateSelf();
    }

    public float getCenterRadius() {
        return this.mRing.getCenterRadius();
    }

    public void setCenterRadius(float centerRadius) {
        this.mRing.setCenterRadius(centerRadius);
        invalidateSelf();
    }

    public void setStrokeCap(@NonNull Cap strokeCap) {
        this.mRing.setStrokeCap(strokeCap);
        invalidateSelf();
    }

    @NonNull
    public Cap getStrokeCap() {
        return this.mRing.getStrokeCap();
    }

    public float getArrowWidth() {
        return this.mRing.getArrowWidth();
    }

    public float getArrowHeight() {
        return this.mRing.getArrowHeight();
    }

    public void setArrowDimensions(float width, float height) {
        this.mRing.setArrowDimensions(width, height);
        invalidateSelf();
    }

    public boolean getArrowEnabled() {
        return this.mRing.getShowArrow();
    }

    public void setArrowEnabled(boolean show) {
        this.mRing.setShowArrow(show);
        invalidateSelf();
    }

    public float getArrowScale() {
        return this.mRing.getArrowScale();
    }

    public void setArrowScale(float scale) {
        this.mRing.setArrowScale(scale);
        invalidateSelf();
    }

    public float getStartTrim() {
        return this.mRing.getStartTrim();
    }

    public float getEndTrim() {
        return this.mRing.getEndTrim();
    }

    public void setStartEndTrim(float start, float end) {
        this.mRing.setStartTrim(start);
        this.mRing.setEndTrim(end);
        invalidateSelf();
    }

    public float getProgressRotation() {
        return this.mRing.getRotation();
    }

    public void setProgressRotation(float rotation) {
        this.mRing.setRotation(rotation);
        invalidateSelf();
    }

    public int getBackgroundColor() {
        return this.mRing.getBackgroundColor();
    }

    public void setBackgroundColor(int color) {
        this.mRing.setBackgroundColor(color);
        invalidateSelf();
    }

    @NonNull
    public int[] getColorSchemeColors() {
        return this.mRing.getColors();
    }

    public void setColorSchemeColors(@NonNull int... colors) {
        this.mRing.setColors(colors);
        this.mRing.setColorIndex(0);
        invalidateSelf();
    }

    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        canvas.save();
        canvas.rotate(this.mRotation, bounds.exactCenterX(), bounds.exactCenterY());
        this.mRing.draw(canvas, bounds);
        canvas.restore();
    }

    public void setAlpha(int alpha) {
        this.mRing.setAlpha(alpha);
        invalidateSelf();
    }

    public int getAlpha() {
        return this.mRing.getAlpha();
    }

    public void setColorFilter(ColorFilter colorFilter) {
        this.mRing.setColorFilter(colorFilter);
        invalidateSelf();
    }

    private void setRotation(float rotation) {
        this.mRotation = rotation;
    }

    private float getRotation() {
        return this.mRotation;
    }

    public int getOpacity() {
        return -3;
    }

    public boolean isRunning() {
        return this.mAnimator.isRunning();
    }

    public void start() {
        this.mAnimator.cancel();
        this.mRing.storeOriginals();
        if (this.mRing.getEndTrim() != this.mRing.getStartTrim()) {
            this.mFinishing = true;
            this.mAnimator.setDuration(666);
            this.mAnimator.start();
            return;
        }
        this.mRing.setColorIndex(0);
        this.mRing.resetOriginals();
        this.mAnimator.setDuration(1332);
        this.mAnimator.start();
    }

    public void stop() {
        this.mAnimator.cancel();
        setRotation(0.0f);
        this.mRing.setShowArrow(false);
        this.mRing.setColorIndex(0);
        this.mRing.resetOriginals();
        invalidateSelf();
    }

    private int evaluateColorChange(float fraction, int startValue, int endValue) {
        int startA = (startValue >> 24) & 255;
        int startR = (startValue >> 16) & 255;
        int startG = (startValue >> 8) & 255;
        int startB = startValue & 255;
        return ((((((int) (((float) (((endValue >> 24) & 255) - startA)) * fraction)) + startA) << 24) | ((((int) (((float) (((endValue >> 16) & 255) - startR)) * fraction)) + startR) << 16)) | ((((int) (((float) (((endValue >> 8) & 255) - startG)) * fraction)) + startG) << 8)) | (((int) (((float) ((endValue & 255) - startB)) * fraction)) + startB);
    }

    private void updateRingColor(float interpolatedTime, Ring ring) {
        if (interpolatedTime > COLOR_CHANGE_OFFSET) {
            ring.setColor(evaluateColorChange((interpolatedTime - COLOR_CHANGE_OFFSET) / 0.25f, ring.getStartingColor(), ring.getNextColor()));
        } else {
            ring.setColor(ring.getStartingColor());
        }
    }

    private void applyFinishTranslation(float interpolatedTime, Ring ring) {
        updateRingColor(interpolatedTime, ring);
        float targetRotation = (float) (Math.floor((double) (ring.getStartingRotation() / MAX_PROGRESS_ARC)) + 1.0d);
        ring.setStartTrim(ring.getStartingStartTrim() + (((ring.getStartingEndTrim() - MIN_PROGRESS_ARC) - ring.getStartingStartTrim()) * interpolatedTime));
        ring.setEndTrim(ring.getStartingEndTrim());
        ring.setRotation(ring.getStartingRotation() + ((targetRotation - ring.getStartingRotation()) * interpolatedTime));
    }

    private void applyTransformation(float interpolatedTime, Ring ring, boolean lastFrame) {
        if (this.mFinishing) {
            applyFinishTranslation(interpolatedTime, ring);
        } else if (interpolatedTime != 1.0f || lastFrame) {
            float scaledTime;
            float startTrim;
            float startingRotation = ring.getStartingRotation();
            if (interpolatedTime < SHRINK_OFFSET) {
                scaledTime = interpolatedTime / SHRINK_OFFSET;
                startTrim = ring.getStartingStartTrim();
                scaledTime = startTrim;
                startTrim = ((0.79f * MATERIAL_INTERPOLATOR.getInterpolation(scaledTime)) + MIN_PROGRESS_ARC) + startTrim;
            } else {
                startTrim = ring.getStartingStartTrim() + 0.79f;
                scaledTime = startTrim - ((0.79f * (1.0f - MATERIAL_INTERPOLATOR.getInterpolation((interpolatedTime - SHRINK_OFFSET) / SHRINK_OFFSET))) + MIN_PROGRESS_ARC);
            }
            float rotation = (RING_ROTATION * interpolatedTime) + startingRotation;
            float groupRotation = GROUP_FULL_ROTATION * (this.mRotationCount + interpolatedTime);
            ring.setStartTrim(scaledTime);
            ring.setEndTrim(startTrim);
            ring.setRotation(rotation);
            setRotation(groupRotation);
        }
    }

    /* JADX WARNING: Incorrect type for fill-array insn 0x0005, element type: float, insn element type: null */
    private void setupAnimators() {
        /*
        r3 = this;
        r0 = r3.mRing;
        r1 = 2;
        r1 = new float[r1];
        r1 = {0, 1065353216};
        r1 = android.animation.ValueAnimator.ofFloat(r1);
        r2 = new android.support.v4.widget.CircularProgressDrawable$1;
        r2.<init>(r0);
        r1.addUpdateListener(r2);
        r2 = -1;
        r1.setRepeatCount(r2);
        r2 = 1;
        r1.setRepeatMode(r2);
        r2 = LINEAR_INTERPOLATOR;
        r1.setInterpolator(r2);
        r2 = new android.support.v4.widget.CircularProgressDrawable$2;
        r2.<init>(r0);
        r1.addListener(r2);
        r3.mAnimator = r1;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.v4.widget.CircularProgressDrawable.setupAnimators():void");
    }
}
