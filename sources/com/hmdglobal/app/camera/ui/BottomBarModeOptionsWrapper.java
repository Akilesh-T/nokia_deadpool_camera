package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import com.hmdglobal.app.camera.CaptureLayoutHelper;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;

public class BottomBarModeOptionsWrapper extends FrameLayout {
    private static final Tag TAG = new Tag("BottomBarWrapper");
    private BottomBar mBottomBar;
    private CaptureLayoutHelper mCaptureLayoutHelper = null;
    private View mModeOptionsOverlay;

    public BottomBarModeOptionsWrapper(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void onFinishInflate() {
        super.onFinishInflate();
        this.mModeOptionsOverlay = findViewById(R.id.mode_options_overlay);
        this.mBottomBar = (BottomBar) findViewById(R.id.bottom_bar);
    }

    public void setCaptureLayoutHelper(CaptureLayoutHelper helper) {
        this.mCaptureLayoutHelper = helper;
    }

    /* Access modifiers changed, original: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.mCaptureLayoutHelper == null) {
            Log.e(TAG, "Capture layout helper needs to be set first.");
            return;
        }
        RectF bottomBarRect = this.mCaptureLayoutHelper.getBottomBarRect();
        this.mModeOptionsOverlay.layout(0, 0, right - left, bottom - top);
        this.mBottomBar.layout((int) bottomBarRect.left, (int) bottomBarRect.top, (int) bottomBarRect.right, (int) bottomBarRect.bottom);
        if (!this.mCaptureLayoutHelper.shouldOverlayBottomBar()) {
            this.mBottomBar.setBottomBarColor(getResources().getColor(R.color.bottombar_background_default));
        } else if (this.mBottomBar.isBackgroundTransparent()) {
            this.mBottomBar.setBottomBarColor(0);
        } else {
            this.mBottomBar.setBottomBarColor(getResources().getColor(R.color.bottombar_background_overlay));
        }
    }
}
