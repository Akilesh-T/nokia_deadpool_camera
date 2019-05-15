package com.hmdglobal.app.camera.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build.VERSION;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.DrawableRes;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.widget.ImageButton;
import com.hmdglobal.app.camera.R;

public class FloatingActionButton extends ImageButton {
    private static final int HALF_TRANSPARENT_BLACK = Color.argb(128, 0, 0, 0);
    private static final int HALF_TRANSPARENT_WHITE = Color.argb(128, 255, 255, 255);
    public static final int SIZE_MINI = 1;
    public static final int SIZE_NORMAL = 0;
    private float mCircleSize;
    int mColorNormal;
    int mColorPressed;
    private int mDrawableSize;
    @DrawableRes
    private int mIcon;
    private float mShadowOffset;
    private float mShadowRadius;
    private int mSize;

    public FloatingActionButton(Context context) {
        this(context, null);
    }

    public FloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public FloatingActionButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    /* Access modifiers changed, original: 0000 */
    public void init(Context context, AttributeSet attributeSet) {
        int i;
        this.mColorNormal = getColor(17170451);
        this.mColorPressed = getColor(17170450);
        this.mIcon = 0;
        this.mSize = 0;
        if (attributeSet != null) {
            initAttributes(context, attributeSet);
        }
        if (this.mSize == 0) {
            i = R.dimen.fab_size_normal;
        } else {
            i = R.dimen.fab_size_mini;
        }
        this.mCircleSize = getDimension(i);
        this.mShadowRadius = getDimension(R.dimen.fab_shadow_radius);
        this.mShadowOffset = getDimension(R.dimen.fab_shadow_offset);
        this.mDrawableSize = (int) this.mCircleSize;
        updateBackground();
    }

    /* Access modifiers changed, original: 0000 */
    public int getColor(@ColorRes int id) {
        return getResources().getColor(id);
    }

    /* Access modifiers changed, original: 0000 */
    public float getDimension(@DimenRes int id) {
        return getResources().getDimension(id);
    }

    private void initAttributes(Context context, AttributeSet attributeSet) {
    }

    /* Access modifiers changed, original: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(this.mDrawableSize, this.mDrawableSize);
    }

    /* Access modifiers changed, original: 0000 */
    public void updateBackground() {
        float circleLeft = this.mShadowRadius;
        float circleTop = this.mShadowRadius - this.mShadowOffset;
        RectF circleRect = new RectF(circleLeft, circleTop, circleLeft, circleTop);
        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{getIconDrawable(), createFillDrawable(circleRect)});
        layerDrawable.setLayerInset(0, 5, 10, 5, 0);
        setBackgroundCompat(layerDrawable);
    }

    /* Access modifiers changed, original: 0000 */
    public Drawable getIconDrawable() {
        if (this.mIcon != 0) {
            return getResources().getDrawable(this.mIcon);
        }
        return new ColorDrawable(0);
    }

    private StateListDrawable createFillDrawable(RectF circleRect) {
        StateListDrawable drawable = new StateListDrawable();
        drawable.addState(new int[]{16842919}, createCircleDrawable(circleRect, this.mColorPressed));
        drawable.addState(new int[0], createCircleDrawable(circleRect, this.mColorNormal));
        return drawable;
    }

    private Drawable createCircleDrawable(RectF circleRect, int color) {
        Bitmap bitmap = Bitmap.createBitmap(this.mDrawableSize, this.mDrawableSize, Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);
        canvas.drawOval(circleRect, paint);
        return new BitmapDrawable(getResources(), bitmap);
    }

    private int opacityToAlpha(float opacity) {
        return (int) (255.0f * opacity);
    }

    private Drawable createStrokesDrawable(RectF circleRect) {
        RectF rectF = circleRect;
        Bitmap bitmap = Bitmap.createBitmap(this.mDrawableSize, this.mDrawableSize, Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        float strokeWidth = getDimension(1.781206E38f);
        float halfStrokeWidth = strokeWidth / 2.0f;
        RectF outerStrokeRect = new RectF(rectF.left - halfStrokeWidth, rectF.top - halfStrokeWidth, rectF.right + halfStrokeWidth, rectF.bottom + halfStrokeWidth);
        RectF innerStrokeRect = new RectF(rectF.left + halfStrokeWidth, rectF.top + halfStrokeWidth, rectF.right - halfStrokeWidth, rectF.bottom - halfStrokeWidth);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(strokeWidth);
        paint.setStyle(Style.STROKE);
        paint.setColor(ViewCompat.MEASURED_STATE_MASK);
        paint.setAlpha(opacityToAlpha(0.02f));
        canvas.drawOval(outerStrokeRect, paint);
        paint.setShader(new LinearGradient(innerStrokeRect.centerX(), innerStrokeRect.top, innerStrokeRect.centerX(), innerStrokeRect.bottom, new int[]{0, HALF_TRANSPARENT_BLACK, ViewCompat.MEASURED_STATE_MASK}, new float[]{0.0f, 0.8f, 1.0f}, TileMode.CLAMP));
        paint.setAlpha(opacityToAlpha(0.04f));
        canvas.drawOval(innerStrokeRect, paint);
        paint.setShader(new LinearGradient(innerStrokeRect.centerX(), innerStrokeRect.top, innerStrokeRect.centerX(), innerStrokeRect.bottom, new int[]{-1, HALF_TRANSPARENT_WHITE, 0}, new float[]{0.0f, 0.2f, 1.0f}, TileMode.CLAMP));
        paint.setAlpha(opacityToAlpha(0.8f));
        canvas.drawOval(innerStrokeRect, paint);
        return new BitmapDrawable(getResources(), bitmap);
    }

    @SuppressLint({"NewApi"})
    private void setBackgroundCompat(Drawable drawable) {
        if (VERSION.SDK_INT >= 16) {
            setBackground(drawable);
        } else {
            setBackgroundDrawable(drawable);
        }
    }

    public int getArrowType() {
        return this.mSize;
    }
}
