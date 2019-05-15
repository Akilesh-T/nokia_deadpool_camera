package com.hmdglobal.app.camera.drawable;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.util.TypedValue;

public class TextDrawable extends Drawable {
    private static final int DEFAULT_COLOR = -1;
    private static final int DEFAULT_TEXTSIZE = 15;
    private int mIntrinsicHeight;
    private int mIntrinsicWidth;
    private Paint mPaint;
    private CharSequence mText;
    private boolean mUseDropShadow;

    public TextDrawable(Resources res) {
        this(res, "");
    }

    public TextDrawable(Resources res, CharSequence text) {
        this.mText = text;
        updatePaint();
        this.mPaint.setTextSize(TypedValue.applyDimension(2, 15.0f, res.getDisplayMetrics()));
        this.mIntrinsicWidth = (int) (((double) this.mPaint.measureText(this.mText, 0, this.mText.length())) + 0.5d);
        this.mIntrinsicHeight = this.mPaint.getFontMetricsInt(null);
    }

    private void updatePaint() {
        if (this.mPaint == null) {
            this.mPaint = new Paint(1);
        }
        this.mPaint.setColor(-1);
        this.mPaint.setTextAlign(Align.CENTER);
        if (this.mUseDropShadow) {
            this.mPaint.setTypeface(Typeface.DEFAULT_BOLD);
            this.mPaint.setShadowLayer(10.0f, 0.0f, 0.0f, ViewCompat.MEASURED_STATE_MASK);
            return;
        }
        this.mPaint.setTypeface(Typeface.DEFAULT);
        this.mPaint.setShadowLayer(0.0f, 0.0f, 0.0f, 0);
    }

    public void setText(CharSequence txt) {
        this.mText = txt;
        if (txt == null) {
            this.mIntrinsicWidth = 0;
            this.mIntrinsicHeight = 0;
            return;
        }
        this.mIntrinsicWidth = (int) (((double) this.mPaint.measureText(this.mText, 0, this.mText.length())) + 0.5d);
        this.mIntrinsicHeight = this.mPaint.getFontMetricsInt(null);
    }

    public void draw(Canvas canvas) {
        if (this.mText != null) {
            Rect bounds = getBounds();
            canvas.drawText(this.mText, 0, this.mText.length(), (float) bounds.centerX(), (float) bounds.centerY(), this.mPaint);
        }
    }

    public void setDropShadow(boolean shadow) {
        this.mUseDropShadow = shadow;
        updatePaint();
    }

    public int getOpacity() {
        return this.mPaint.getAlpha();
    }

    public int getIntrinsicWidth() {
        return this.mIntrinsicWidth;
    }

    public int getIntrinsicHeight() {
        return this.mIntrinsicHeight;
    }

    public void setAlpha(int alpha) {
        this.mPaint.setAlpha(alpha);
    }

    public void setColorFilter(ColorFilter filter) {
        this.mPaint.setColorFilter(filter);
    }
}
