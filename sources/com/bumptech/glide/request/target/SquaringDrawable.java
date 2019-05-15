package com.bumptech.glide.request.target;

import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.Callback;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;

public class SquaringDrawable extends GlideDrawable {
    private final int side;
    private final GlideDrawable wrapped;

    public SquaringDrawable(GlideDrawable wrapped, int side) {
        this.wrapped = wrapped;
        this.side = side;
    }

    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        this.wrapped.setBounds(left, top, right, bottom);
    }

    public void setBounds(Rect bounds) {
        super.setBounds(bounds);
        this.wrapped.setBounds(bounds);
    }

    public void setChangingConfigurations(int configs) {
        this.wrapped.setChangingConfigurations(configs);
    }

    public int getChangingConfigurations() {
        return this.wrapped.getChangingConfigurations();
    }

    public void setDither(boolean dither) {
        this.wrapped.setDither(dither);
    }

    public void setFilterBitmap(boolean filter) {
        this.wrapped.setFilterBitmap(filter);
    }

    @TargetApi(11)
    public Callback getCallback() {
        return this.wrapped.getCallback();
    }

    @TargetApi(19)
    public int getAlpha() {
        return this.wrapped.getAlpha();
    }

    public void setColorFilter(int color, Mode mode) {
        this.wrapped.setColorFilter(color, mode);
    }

    public void clearColorFilter() {
        this.wrapped.clearColorFilter();
    }

    public Drawable getCurrent() {
        return this.wrapped.getCurrent();
    }

    public boolean setVisible(boolean visible, boolean restart) {
        return this.wrapped.setVisible(visible, restart);
    }

    public int getIntrinsicWidth() {
        return this.side;
    }

    public int getIntrinsicHeight() {
        return this.side;
    }

    public int getMinimumWidth() {
        return this.wrapped.getMinimumWidth();
    }

    public int getMinimumHeight() {
        return this.wrapped.getMinimumHeight();
    }

    public boolean getPadding(Rect padding) {
        return this.wrapped.getPadding(padding);
    }

    public void invalidateSelf() {
        super.invalidateSelf();
        this.wrapped.invalidateSelf();
    }

    public void unscheduleSelf(Runnable what) {
        super.unscheduleSelf(what);
        this.wrapped.unscheduleSelf(what);
    }

    public void scheduleSelf(Runnable what, long when) {
        super.scheduleSelf(what, when);
        this.wrapped.scheduleSelf(what, when);
    }

    public void draw(Canvas canvas) {
        this.wrapped.draw(canvas);
    }

    public void setAlpha(int i) {
        this.wrapped.setAlpha(i);
    }

    public void setColorFilter(ColorFilter colorFilter) {
        this.wrapped.setColorFilter(colorFilter);
    }

    public int getOpacity() {
        return this.wrapped.getOpacity();
    }

    public boolean isAnimated() {
        return this.wrapped.isAnimated();
    }

    public void setLoopCount(int loopCount) {
        this.wrapped.setLoopCount(loopCount);
    }

    public void start() {
        this.wrapped.start();
    }

    public void stop() {
        this.wrapped.stop();
    }

    public boolean isRunning() {
        return this.wrapped.isRunning();
    }
}
