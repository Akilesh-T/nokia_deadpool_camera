package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.View.OnTouchListener;
import com.hmdglobal.app.camera.HelpTipsManager;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.ui.PreviewStatusListener.PreviewAreaChangedListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PreviewOverlay extends View implements PreviewAreaChangedListener {
    private static final Tag TAG = new Tag("PreviewOverlay");
    private static final boolean USE_ZOOM_BAR = true;
    private static final long ZOOM_MINIMUM_WAIT_MILLIS = 33;
    public static final float ZOOM_MIN_RATIO = 1.0f;
    private long mDelayZoomCallUntilMillis = 0;
    private GestureDetector mGestureDetector = null;
    private HelpTipsManager mHelpTipListener;
    private OnPreviewTouchedListener mOnPreviewTouchedListener;
    private final ArrayList<OnPreviewTouchedListener> mOnPreviewTouchedListeners = new ArrayList();
    private final ZoomGestureDetector mScaleDetector = new ZoomGestureDetector();
    private boolean mTouchEnabled = true;
    private OnTouchListener mTouchListener = null;
    private View mZoomBar;
    private OnZoomChangedListener mZoomListener = null;
    private final ZoomProcessor mZoomProcessor = new ZoomProcessor();

    public interface OnPreviewTouchedListener {
        void onPreviewTouched(MotionEvent motionEvent);
    }

    public interface OnZoomChangedListener {
        void onZoomEnd();

        void onZoomStart();

        void onZoomValueChanged(float f);
    }

    private class ZoomGestureDetector extends ScaleGestureDetector {
        private float mDeltaX;
        private float mDeltaY;
        private int mPointerCount;

        public ZoomGestureDetector() {
            super(PreviewOverlay.this.getContext(), PreviewOverlay.this.mZoomProcessor);
        }

        public boolean onTouchEvent(MotionEvent ev) {
            if (PreviewOverlay.this.mZoomListener == null) {
                return false;
            }
            boolean handled = super.onTouchEvent(ev);
            this.mPointerCount = ev.getPointerCount();
            if (ev.getPointerCount() > 1) {
                this.mDeltaX = ev.getX(1) - ev.getX(0);
                this.mDeltaY = ev.getY(1) - ev.getY(0);
            }
            return handled;
        }

        public float getAngle() {
            return (float) Math.atan2((double) (-this.mDeltaY), (double) this.mDeltaX);
        }

        public int getPointerCount() {
            return this.mPointerCount;
        }
    }

    private class ZoomProcessor implements OnScaleGestureListener {
        private static final float ZOOM_UI_DONUT = 0.25f;
        private static final float ZOOM_UI_SIZE = 0.8f;
        private final Tag TAG = new Tag("ZoomProcessor");
        private int mCenterX;
        private int mCenterY;
        private float mCurrentRatio;
        private double mFingerAngle;
        private float mInnerRadius;
        private float mMaxRatio;
        private final float mMinRatio = 1.0f;
        private float mOuterRadius;
        private final Paint mPaint;
        private boolean mVisible = false;
        private List<Integer> mZoomRatios;
        private final int mZoomStroke;

        public ZoomProcessor() {
            this.mZoomStroke = PreviewOverlay.this.getResources().getDimensionPixelSize(R.dimen.zoom_stroke);
            this.mPaint = new Paint();
            this.mPaint.setAntiAlias(true);
            this.mPaint.setColor(-1);
            this.mPaint.setStyle(Style.STROKE);
            this.mPaint.setStrokeWidth((float) this.mZoomStroke);
            this.mPaint.setStrokeCap(Cap.ROUND);
        }

        public void setZoomMax(float zoomMaxRatio) {
            this.mMaxRatio = zoomMaxRatio;
        }

        public void setZoom(float ratio) {
            this.mCurrentRatio = ratio;
        }

        public void layout(int l, int t, int r, int b) {
            this.mCenterX = (r - l) / 2;
            this.mCenterY = (b - t) / 2;
            this.mOuterRadius = (0.5f * ((float) Math.min(PreviewOverlay.this.getWidth(), PreviewOverlay.this.getHeight()))) * ZOOM_UI_SIZE;
            this.mInnerRadius = this.mOuterRadius * ZOOM_UI_DONUT;
        }

        public void draw(Canvas canvas) {
            if (!this.mVisible) {
            }
        }

        public boolean onScale(ScaleGestureDetector detector) {
            float sf = detector.getScaleFactor();
            this.mCurrentRatio = (((this.mCurrentRatio + 0.33f) * sf) * sf) - 0.33f;
            if (this.mCurrentRatio < 1.0f) {
                this.mCurrentRatio = 1.0f;
            }
            if (this.mCurrentRatio > this.mMaxRatio) {
                this.mCurrentRatio = this.mMaxRatio;
            }
            long now = SystemClock.uptimeMillis();
            if (now > PreviewOverlay.this.mDelayZoomCallUntilMillis) {
                if (PreviewOverlay.this.mZoomListener != null) {
                    PreviewOverlay.this.mZoomListener.onZoomValueChanged(this.mCurrentRatio);
                }
                if (PreviewOverlay.this.mHelpTipListener != null) {
                    PreviewOverlay.this.mHelpTipListener.notifyEventFinshed();
                    PreviewOverlay.this.mHelpTipListener = null;
                }
                PreviewOverlay.this.mDelayZoomCallUntilMillis = PreviewOverlay.ZOOM_MINIMUM_WAIT_MILLIS + now;
            }
            this.mFingerAngle = (double) PreviewOverlay.this.mScaleDetector.getAngle();
            PreviewOverlay.this.invalidate();
            return true;
        }

        public boolean onScaleBegin(ScaleGestureDetector detector) {
            if (PreviewOverlay.this.mScaleDetector != null && PreviewOverlay.this.mScaleDetector.getPointerCount() < 2) {
                return false;
            }
            PreviewOverlay.this.mZoomProcessor.showZoomUI();
            if (PreviewOverlay.this.mZoomListener == null) {
                return false;
            }
            if (PreviewOverlay.this.mZoomListener != null) {
                PreviewOverlay.this.mZoomListener.onZoomStart();
            }
            this.mFingerAngle = (double) PreviewOverlay.this.mScaleDetector.getAngle();
            PreviewOverlay.this.invalidate();
            return true;
        }

        public void onScaleEnd(ScaleGestureDetector detector) {
            PreviewOverlay.this.mZoomProcessor.hideZoomUI();
            if (PreviewOverlay.this.mZoomListener != null) {
                PreviewOverlay.this.mZoomListener.onZoomEnd();
            }
            PreviewOverlay.this.invalidate();
        }

        public boolean isVisible() {
            return this.mVisible;
        }

        public void showZoomUI() {
            if (PreviewOverlay.this.mZoomListener != null) {
                this.mVisible = true;
                this.mFingerAngle = (double) PreviewOverlay.this.mScaleDetector.getAngle();
                PreviewOverlay.this.invalidate();
            }
        }

        public void hideZoomUI() {
            if (PreviewOverlay.this.mZoomListener != null) {
                this.mVisible = false;
                PreviewOverlay.this.invalidate();
            }
        }

        private void setupZoom(float zoomMax, float zoom) {
            setZoomMax(zoomMax);
            setZoom(zoom);
        }

        private void resetZoom() {
            setZoom(1.0f);
        }
    }

    /* Access modifiers changed, original: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mZoomBar = findViewById(R.id.zoom_bar);
    }

    public PreviewOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setHelpTipsListener(HelpTipsManager Listener) {
        this.mHelpTipListener = Listener;
    }

    public void setupZoom(float zoomMaxRatio, float zoom, OnZoomChangedListener zoomChangeListener) {
        this.mZoomListener = zoomChangeListener;
        this.mZoomProcessor.setupZoom(zoomMaxRatio, zoom);
    }

    public void resetZoom() {
        this.mZoomProcessor.resetZoom();
    }

    public void setRatio(float zoom) {
        this.mZoomProcessor.setZoom(zoom);
    }

    public boolean onTouchEvent(MotionEvent m) {
        if (!this.mTouchEnabled) {
            return true;
        }
        if (this.mGestureDetector != null) {
            this.mGestureDetector.onTouchEvent(m);
        }
        if (!this.mZoomProcessor.isVisible() && ((this.mZoomBar == null || this.mZoomBar.getVisibility() != 0) && this.mTouchListener != null)) {
            this.mTouchListener.onTouch(this, m);
        }
        this.mScaleDetector.onTouchEvent(m);
        Iterator it = this.mOnPreviewTouchedListeners.iterator();
        while (it.hasNext()) {
            ((OnPreviewTouchedListener) it.next()).onPreviewTouched(m);
        }
        return true;
    }

    public void setOnPreviewTouchedListener(OnPreviewTouchedListener listener) {
        this.mOnPreviewTouchedListener = listener;
    }

    public void addOnPreviewTouchedListener(OnPreviewTouchedListener listener) {
        if (listener != null && !this.mOnPreviewTouchedListeners.contains(listener)) {
            this.mOnPreviewTouchedListeners.add(listener);
        }
    }

    public void removeOnPreviewTouchedListener(OnPreviewTouchedListener listener) {
        if (listener != null && this.mOnPreviewTouchedListeners.contains(listener)) {
            this.mOnPreviewTouchedListeners.remove(listener);
        }
    }

    public void onPreviewAreaChanged(RectF previewArea) {
        this.mZoomProcessor.layout((int) previewArea.left, (int) previewArea.top, (int) previewArea.right, (int) previewArea.bottom);
    }

    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.mZoomProcessor.draw(canvas);
    }

    public void setGestureListener(OnGestureListener gestureListener) {
        if (gestureListener != null) {
            this.mGestureDetector = new GestureDetector(getContext(), gestureListener);
        }
    }

    public void setTouchListener(OnTouchListener touchListener) {
        this.mTouchListener = touchListener;
    }

    public void reset() {
        this.mZoomListener = null;
        this.mGestureDetector = null;
        this.mTouchListener = null;
    }

    public void setTouchEnabled(boolean enable) {
        this.mTouchEnabled = enable;
    }
}
