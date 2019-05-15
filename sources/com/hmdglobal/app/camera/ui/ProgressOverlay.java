package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout.LayoutParams;

public class ProgressOverlay extends View {
    private int mCenterX;
    private int mCenterY;
    private final ProgressRenderer mProgressRenderer;

    public ProgressOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mProgressRenderer = new ProgressRenderer(context, this);
    }

    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            this.mCenterX = (right - left) / 2;
            this.mCenterY = (bottom - top) / 2;
        }
    }

    public void setBounds(RectF area) {
        if (area.width() > 0.0f && area.height() > 0.0f) {
            LayoutParams params = (LayoutParams) getLayoutParams();
            params.width = (int) area.width();
            params.height = (int) area.height();
            params.setMargins((int) area.left, (int) area.top, 0, 0);
            setLayoutParams(params);
        }
    }

    public void onDraw(Canvas canvas) {
        this.mProgressRenderer.onDraw(canvas, this.mCenterX, this.mCenterY);
    }

    public void setProgress(int percent) {
        this.mProgressRenderer.setProgress(percent);
    }
}
