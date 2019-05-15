package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.hmdglobal.app.camera.R;

class ModeSelectorItem extends FrameLayout {
    private ModeIconView mIcon;
    private VisibleWidthChangedListener mListener = null;
    private final int mMinVisibleWidth;
    private int mModeId;
    private TextView mText;
    private int mVisibleWidth = 0;
    private int mWidth;

    public interface VisibleWidthChangedListener {
        void onVisibleWidthChanged(int i);
    }

    public ModeSelectorItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);
        setClickable(true);
        this.mMinVisibleWidth = getResources().getDimensionPixelSize(R.dimen.mode_selector_icon_block_width);
    }

    public void onFinishInflate() {
        this.mIcon = (ModeIconView) findViewById(R.id.selector_icon);
        this.mText = (TextView) findViewById(R.id.selector_text);
    }

    public void setDefaultBackgroundColor(int color) {
        setBackgroundColor(color);
    }

    public void setVisibleWidthChangedListener(VisibleWidthChangedListener listener) {
        this.mListener = listener;
    }

    public void setSelected(boolean selected) {
        this.mIcon.setSelected(selected);
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        return false;
    }

    public boolean onTouchEvent(MotionEvent ev) {
        super.onTouchEvent(ev);
        return false;
    }

    public void onSwipeModeChanged(boolean swipeIn) {
        this.mText.setTranslationX(0.0f);
    }

    public void setText(CharSequence text) {
        this.mText.setText(text);
    }

    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        this.mWidth = right - left;
        if (changed && this.mVisibleWidth > 0) {
            setVisibleWidth(this.mWidth);
        }
    }

    public void setImageResource(int resource) {
        Drawable drawableIcon = getResources().getDrawable(resource);
        if (drawableIcon != null) {
            drawableIcon = drawableIcon.mutate();
        }
        this.mIcon.setIconDrawable(drawableIcon);
    }

    public void setVisibleWidth(int newWidth) {
        newWidth = Math.min(Math.max(newWidth, 0), getMaxVisibleWidth());
        if (this.mVisibleWidth != newWidth) {
            this.mVisibleWidth = newWidth;
            if (this.mListener != null) {
                this.mListener.onVisibleWidthChanged(newWidth);
            }
        }
        invalidate();
    }

    public int getVisibleWidth() {
        return this.mVisibleWidth;
    }

    public void draw(Canvas canvas) {
        float transX = 0.0f;
        if (this.mVisibleWidth < this.mMinVisibleWidth + this.mIcon.getLeft()) {
            transX = (float) ((this.mMinVisibleWidth + this.mIcon.getLeft()) - this.mVisibleWidth);
        }
        canvas.save();
        canvas.translate(-transX, 0.0f);
        super.draw(canvas);
        canvas.restore();
    }

    public void setHighlightColor(int highlightColor) {
        this.mIcon.setHighlightColor(highlightColor);
    }

    public int getHighlightColor() {
        return this.mIcon.getHighlightColor();
    }

    public int getMaxVisibleWidth() {
        return this.mIcon.getLeft() + this.mMinVisibleWidth;
    }

    public void getIconCenterLocationInWindow(int[] loc) {
        this.mIcon.getLocationInWindow(loc);
        loc[0] = loc[0] + (this.mMinVisibleWidth / 2);
        loc[1] = loc[1] + (this.mMinVisibleWidth / 2);
    }

    public void setModeId(int modeId) {
        this.mModeId = modeId;
    }

    public int getModeId() {
        return this.mModeId;
    }

    public ModeIconView getIcon() {
        return this.mIcon;
    }

    public void setTextAlpha(float alpha) {
        this.mText.setAlpha(alpha);
    }
}
