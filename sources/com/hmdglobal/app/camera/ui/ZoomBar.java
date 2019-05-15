package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.graphics.RectF;
import android.os.Handler;
import android.support.graphics.drawable.PathInterpolatorCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import com.hmdglobal.app.camera.CaptureLayoutHelper;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.morphoinc.utils.multimedia.MediaProviderUtils;

public class ZoomBar extends LinearLayout implements OnSeekBarChangeListener, OnTouchListener {
    public static final boolean ROTATABLE = false;
    private static final long TIME_ADD = 100;
    private static final float ZOOM_STEP = 0.1f;
    private final int SEEKBAR_MAX = 100;
    private final int SEEKBAR_MIN = 0;
    private final Tag TAG = new Tag("ZoomBar");
    private final int ZOOM_HIDE_TIME = PathInterpolatorCompat.MAX_NUM_POINTS;
    protected final Runnable hideRunnable = new Runnable() {
        public void run() {
            ZoomBar.this.hideZoomBar();
        }
    };
    private CaptureLayoutHelper mCaptureLayoutHelper;
    private Handler mHandler = new Handler();
    private boolean mIsDefaultShow = true;
    private ProgressChangeListener mListener;
    private int mOrientation = 0;
    private ZoomBar mZoomBar;
    private boolean mZoomInPressed = false;
    private boolean mZoomOutPressed = false;
    final Runnable mZoomProgressRunnable = new Runnable() {
        public void run() {
            if (ZoomBar.this.mZoomInPressed != ZoomBar.this.mZoomOutPressed) {
                ZoomBar.this.changeRatioBar(ZoomBar.this.mZoomInPressed ? ZoomBar.ZOOM_STEP : -0.1f);
                ZoomBar.this.postDelayed(this, ZoomBar.TIME_ADD);
            }
        }
    };
    private float maxRatio;
    private float minRatio = 1.0f;
    private int navigationBarHeight = 0;
    private int preview_left = 0;
    private float ratio;
    private SeekBar zoomBar;
    private ImageView zoomIn;
    private LinearLayout zoomLayout;
    private ImageView zoomOut;

    public interface ProgressChangeListener {
        void onProgressChanged(float f);
    }

    public ZoomBar(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    /* Access modifiers changed, original: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        this.mZoomBar = (ZoomBar) findViewById(R.id.zoom_bar);
        this.zoomLayout = (LinearLayout) findViewById(R.id.zoom_layout);
        this.zoomBar = (SeekBar) findViewById(R.id.zoom_seek);
        this.zoomIn = (ImageView) findViewById(R.id.zoom_in);
        this.zoomOut = (ImageView) findViewById(R.id.zoom_out);
        this.zoomIn.setOnTouchListener(this);
        this.zoomOut.setOnTouchListener(this);
        this.zoomBar.setMax(100);
        this.zoomBar.setOnSeekBarChangeListener(this);
        resetLayoutParam();
    }

    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        this.ratio = (((float) progress) * ((this.maxRatio - this.minRatio) / 100.0f)) + this.minRatio;
        if (fromUser && this.mListener != null) {
            this.mListener.onProgressChanged(this.ratio);
            userAction();
        }
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
        showZoomBar();
        this.mHandler.removeCallbacks(this.hideRunnable);
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
        userAction();
    }

    private void changeRatioBar(float r) {
        this.ratio += r;
        if (this.ratio >= this.maxRatio) {
            this.ratio = this.maxRatio;
            resetPressedState();
        } else if (this.ratio <= this.minRatio) {
            this.ratio = this.minRatio;
            resetPressedState();
        }
        if (this.mListener != null) {
            this.mListener.onProgressChanged(this.ratio);
        }
        updateSeekBar();
        userAction();
    }

    private void resetPressedState() {
        this.mZoomInPressed = false;
        this.mZoomOutPressed = false;
    }

    public boolean onTouch(View view, MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (action != 3) {
            switch (action) {
                case 0:
                    if (view == this.zoomIn) {
                        this.mZoomInPressed = true;
                    } else if (view == this.zoomOut) {
                        this.mZoomOutPressed = true;
                    }
                    if (this.mZoomInPressed && this.mZoomOutPressed) {
                        resetPressedState();
                    }
                    if (this.mZoomInPressed != this.mZoomOutPressed) {
                        removeCallbacks(this.mZoomProgressRunnable);
                        this.mZoomProgressRunnable.run();
                    }
                    return true;
                case 1:
                    break;
            }
        }
        resetPressedState();
        return false;
    }

    public void setProgressChangeListener(ProgressChangeListener l) {
        this.mListener = l;
    }

    public void setZoomMax(float max) {
        this.maxRatio = max;
        Tag tag = this.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Max zoom is ");
        stringBuilder.append(max);
        Log.e(tag, stringBuilder.toString());
    }

    public void zoomRatioChanged(float r) {
        this.ratio = r;
        updateSeekBar();
        userAction();
    }

    public void resetZoomRatio() {
        this.ratio = this.minRatio;
        hideZoomBar();
    }

    public void setPreviewArea(RectF previewArea) {
        this.preview_left = (int) previewArea.left;
    }

    private void updateSeekBar() {
        int pos = (int) ((this.ratio - this.minRatio) * (100.0f / (this.maxRatio - this.minRatio)));
        Tag tag = this.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ZoomBar,  current ratio is ");
        stringBuilder.append(this.ratio);
        stringBuilder.append(", set pos ");
        stringBuilder.append(pos);
        Log.e(tag, stringBuilder.toString());
        if (this.zoomBar != null) {
            this.zoomBar.setProgress(pos);
        }
    }

    private void userAction() {
        showZoomBar();
        this.mHandler.removeCallbacks(this.hideRunnable);
        this.mHandler.postDelayed(this.hideRunnable, 3000);
    }

    private void showZoomBar() {
        post(new Runnable() {
            public void run() {
                if (ZoomBar.this.mZoomBar != null && ZoomBar.this.mZoomBar.getVisibility() != 0) {
                    if (ZoomBar.this.mIsDefaultShow) {
                        ZoomBar.this.mZoomBar.setVisibility(0);
                    } else {
                        ZoomBar.this.mZoomBar.setVisibility(4);
                    }
                    ZoomBar.this.resetPressedState();
                }
            }
        });
    }

    public void setZoomBarTouchVisible(boolean isVisible) {
        this.mIsDefaultShow = isVisible;
    }

    private void hideZoomBar() {
        post(new Runnable() {
            public void run() {
                if (ZoomBar.this.mZoomBar != null && ZoomBar.this.mZoomBar.getVisibility() == 0) {
                    ZoomBar.this.mZoomBar.setVisibility(4);
                    ZoomBar.this.resetPressedState();
                }
            }
        });
    }

    public void setLayoutOrientation(int ori) {
    }

    public void setCaptureLayoutHelper(CaptureLayoutHelper helper) {
        this.mCaptureLayoutHelper = helper;
    }

    private int getBottomBarHeight() {
        int height = getContext().getResources().getDimensionPixelSize(R.dimen.bottom_bar_height_max);
        if (this.mCaptureLayoutHelper == null) {
            return height;
        }
        RectF bottomBarRect = this.mCaptureLayoutHelper.getBottomBarRect();
        if (bottomBarRect == null || bottomBarRect.top <= 0.0f) {
            return height;
        }
        return (int) (bottomBarRect.bottom - bottomBarRect.top);
    }

    public void setNavigationBarHeight(int height) {
        if (height != this.navigationBarHeight) {
            this.navigationBarHeight = height;
            resetLayoutParam();
        }
    }

    public void resetLayoutParam() {
        int display_with = getContext().getResources().getDisplayMetrics().widthPixels;
        int display_height = getContext().getResources().getDisplayMetrics().heightPixels;
        int modeOption_height = getContext().getResources().getDimensionPixelSize(R.dimen.mode_options_height);
        int margin_side = getContext().getResources().getDimensionPixelSize(R.dimen.zoom_bar_margin_side);
        int bottomBar_height = getBottomBarHeight();
        LayoutParams params = new LayoutParams(-2, -2);
        int diff;
        if (this.mOrientation == MediaProviderUtils.ROTATION_270) {
            diff = ((display_height - modeOption_height) - this.zoomLayout.getWidth()) - bottomBar_height;
            params.setMargins(diff > 0 ? (diff / 2) + modeOption_height : modeOption_height, ((display_with - this.preview_left) - this.zoomLayout.getHeight()) - margin_side, 0, 0);
        } else if (this.mOrientation == MediaProviderUtils.ROTATION_180) {
            params.setMargins(0, ((this.navigationBarHeight + display_height) - modeOption_height) - this.zoomLayout.getHeight(), 0, 0);
        } else if (this.mOrientation == 90) {
            int left;
            diff = ((display_height - modeOption_height) - this.zoomLayout.getWidth()) - bottomBar_height;
            if (diff > 0) {
                left = (this.navigationBarHeight + bottomBar_height) + (diff / 2);
            } else {
                left = this.navigationBarHeight + bottomBar_height;
            }
            params.setMargins(left, ((display_with - this.preview_left) - this.zoomLayout.getHeight()) - margin_side, 0, 0);
        } else {
            params.setMargins(0, 0, 0, (this.navigationBarHeight + getContext().getResources().getDimensionPixelSize(R.dimen.bottom_bar_height_max)) + getContext().getResources().getDimensionPixelSize(R.dimen.zoom_bar_marginBottom));
        }
        this.zoomLayout.setLayoutParams(params);
        Tag tag = this.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("resetLayoutParam, mOrientation ");
        stringBuilder.append(this.mOrientation);
        stringBuilder.append(" left ");
        stringBuilder.append(params.leftMargin);
        stringBuilder.append(" top ");
        stringBuilder.append(params.topMargin);
        stringBuilder.append(" right ");
        stringBuilder.append(params.rightMargin);
        stringBuilder.append(" bottom ");
        stringBuilder.append(params.bottomMargin);
        Log.e(tag, stringBuilder.toString());
        requestLayout();
    }

    public int getZoomScale() {
        return this.zoomBar.getProgress();
    }
}
