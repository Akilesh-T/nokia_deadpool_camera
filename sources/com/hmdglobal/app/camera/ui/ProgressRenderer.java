package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.view.View;
import com.hmdglobal.app.camera.R;
import com.morphoinc.utils.multimedia.MediaProviderUtils;

public class ProgressRenderer {
    private static final int SHOW_PROGRESS_X_ADDITIONAL_MS = 100;
    private RectF mArcBounds = new RectF(0.0f, 0.0f, 1.0f, 1.0f);
    private final Runnable mInvalidateParentViewRunnable = new Runnable() {
        public void run() {
            ProgressRenderer.this.mParentView.invalidate();
        }
    };
    private final View mParentView;
    private int mProgressAngleDegrees = MediaProviderUtils.ROTATION_270;
    private final Paint mProgressBasePaint;
    private final Paint mProgressPaint;
    private final int mProgressRadius;
    private boolean mVisible = false;

    public ProgressRenderer(Context context, View parent) {
        this.mParentView = parent;
        this.mProgressRadius = context.getResources().getDimensionPixelSize(R.dimen.pie_progress_radius);
        int pieProgressWidth = context.getResources().getDimensionPixelSize(R.dimen.pie_progress_width);
        this.mProgressBasePaint = createProgressPaint(pieProgressWidth, 0.2f);
        this.mProgressPaint = createProgressPaint(pieProgressWidth, 1.0f);
    }

    public void setProgress(int percent) {
        percent = Math.min(100, Math.max(percent, 0));
        this.mProgressAngleDegrees = (int) (3.6f * ((float) percent));
        if (percent < 100) {
            this.mVisible = true;
        }
        this.mParentView.post(this.mInvalidateParentViewRunnable);
    }

    public void onDraw(Canvas canvas, int centerX, int centerY) {
        if (this.mVisible) {
            this.mArcBounds = new RectF((float) (centerX - this.mProgressRadius), (float) (centerY - this.mProgressRadius), (float) (this.mProgressRadius + centerX), (float) (this.mProgressRadius + centerY));
            canvas.drawCircle((float) centerX, (float) centerY, (float) this.mProgressRadius, this.mProgressBasePaint);
            canvas.drawArc(this.mArcBounds, -90.0f, (float) this.mProgressAngleDegrees, false, this.mProgressPaint);
            if (this.mProgressAngleDegrees == 360) {
                this.mVisible = false;
                this.mParentView.postDelayed(new Runnable() {
                    public void run() {
                        ProgressRenderer.this.mParentView.invalidate();
                    }
                }, 100);
            }
        }
    }

    public boolean isVisible() {
        return this.mVisible;
    }

    private static Paint createProgressPaint(int width, float alpha) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.argb((int) (255.0f * alpha), 255, 255, 255));
        paint.setStrokeWidth((float) width);
        paint.setStyle(Style.STROKE);
        return paint;
    }
}
