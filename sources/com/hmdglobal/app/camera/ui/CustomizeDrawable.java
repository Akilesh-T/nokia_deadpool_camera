package com.hmdglobal.app.camera.ui;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;

public abstract class CustomizeDrawable extends Drawable {
    private int mAlpha = 255;
    private int mColor;
    private Paint mPaint = new Paint();

    public abstract void draw(Canvas canvas);

    public CustomizeDrawable() {
        this.mPaint.setAntiAlias(true);
    }

    public final void setAlpha(int i) {
        this.mAlpha = i;
        updatePaintColor();
    }

    private void updatePaintColor() {
        this.mPaint.setColor((this.mAlpha << 24) | (this.mColor & ViewCompat.MEASURED_SIZE_MASK));
        invalidateSelf();
    }

    public final Paint getPaint() {
        return this.mPaint;
    }

    public void setColorFilter(ColorFilter colorFilter) {
    }

    public final void setColor(int color) {
        this.mColor = color;
        updatePaintColor();
    }

    public void setARGB(int color) {
        this.mAlpha = (ViewCompat.MEASURED_STATE_MASK & color) >> 24;
        this.mColor = ViewCompat.MEASURED_SIZE_MASK & color;
        updatePaintColor();
    }

    public int getOpacity() {
        return -3;
    }
}
