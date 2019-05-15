package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import com.hmdglobal.app.camera.R;

public class ModeIconView extends View {
    private final GradientDrawable mBackground = ((GradientDrawable) getResources().getDrawable(R.drawable.mode_icon_background).mutate());
    private final int mBackgroundDefaultColor = getResources().getColor(R.color.mode_selector_icon_background);
    private int mHighlightColor;
    private final int mIconBackgroundSize = getResources().getDimensionPixelSize(R.dimen.mode_selector_icon_block_width);
    private Drawable mIconDrawable = null;
    private final int mIconDrawableSize;

    public ModeIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mBackground.setBounds(0, 0, this.mIconBackgroundSize, this.mIconBackgroundSize);
        this.mIconDrawableSize = getResources().getDimensionPixelSize(R.dimen.mode_selector_icon_drawable_size);
    }

    public void setIconDrawable(Drawable drawable) {
        this.mIconDrawable = drawable;
        if (this.mIconDrawable != null) {
            this.mIconDrawable.setBounds((this.mIconBackgroundSize / 2) - (this.mIconDrawableSize / 2), (this.mIconBackgroundSize / 2) - (this.mIconDrawableSize / 2), (this.mIconBackgroundSize / 2) + (this.mIconDrawableSize / 2), (this.mIconBackgroundSize / 2) + (this.mIconDrawableSize / 2));
            invalidate();
        }
    }

    public void draw(Canvas canvas) {
        super.draw(canvas);
        this.mBackground.draw(canvas);
        if (this.mIconDrawable != null) {
            this.mIconDrawable.draw(canvas);
        }
    }

    public Drawable getIconDrawableClone() {
        return this.mIconDrawable.getConstantState().newDrawable();
    }

    public int getIconDrawableSize() {
        return this.mIconDrawableSize;
    }

    public void setSelected(boolean selected) {
        if (selected) {
            this.mBackground.setColor(this.mHighlightColor);
        } else {
            this.mBackground.setColor(this.mBackgroundDefaultColor);
        }
        invalidate();
    }

    public void setHighlightColor(int highlightColor) {
        this.mHighlightColor = highlightColor;
    }

    public int getHighlightColor() {
        return this.mHighlightColor;
    }
}
