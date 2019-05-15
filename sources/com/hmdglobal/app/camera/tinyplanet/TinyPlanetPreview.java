package com.hmdglobal.app.camera.tinyplanet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.concurrent.locks.Lock;

public class TinyPlanetPreview extends View {
    private Lock mLock;
    private Paint mPaint = new Paint();
    private Bitmap mPreview;
    private PreviewSizeListener mPreviewSizeListener;
    private int mSize = 0;

    public interface PreviewSizeListener {
        void onSizeChanged(int i);
    }

    public TinyPlanetPreview(Context context) {
        super(context);
    }

    public TinyPlanetPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TinyPlanetPreview(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setBitmap(Bitmap preview, Lock lock) {
        this.mPreview = preview;
        this.mLock = lock;
        invalidate();
    }

    public void setPreviewSizeChangeListener(PreviewSizeListener listener) {
        this.mPreviewSizeListener = listener;
        if (this.mSize > 0) {
            this.mPreviewSizeListener.onSizeChanged(this.mSize);
        }
    }

    /* Access modifiers changed, original: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mLock != null && this.mLock.tryLock()) {
            try {
                if (!(this.mPreview == null || this.mPreview.isRecycled())) {
                    canvas.drawBitmap(this.mPreview, 0.0f, 0.0f, this.mPaint);
                }
                this.mLock.unlock();
            } catch (Throwable th) {
                this.mLock.unlock();
            }
        }
    }

    /* Access modifiers changed, original: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
        setMeasuredDimension(size, size);
    }

    /* Access modifiers changed, original: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed && this.mPreviewSizeListener != null) {
            int mSize = Math.min(right - left, bottom - top);
            if (mSize > 0 && this.mPreviewSizeListener != null) {
                this.mPreviewSizeListener.onSizeChanged(mSize);
            }
        }
    }
}
