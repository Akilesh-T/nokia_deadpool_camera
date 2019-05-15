package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.hmdglobal.app.camera.CaptureLayoutHelper;
import com.hmdglobal.app.camera.MultiToggleImageButton;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;

public class BottomBarModeOptionsWrapperExtra extends FrameLayout {
    private static final Tag TAG = new Tag("BottomBarWrapper");
    private int height = 0;
    private CaptureLayoutHelper mCaptureLayoutHelper = null;
    private MultiToggleImageButton mMoreEnterToggleButton = null;
    private MoreOptionsWrapper mow = null;

    public BottomBarModeOptionsWrapperExtra(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setCaptureLayoutHelper(CaptureLayoutHelper helper) {
        this.mCaptureLayoutHelper = helper;
    }

    public void setMoreEnterToggleButton(MultiToggleImageButton multiToggleImageButton) {
        this.mMoreEnterToggleButton = multiToggleImageButton;
    }

    public void onResetHeight(int h) {
        this.height = h;
    }

    /* Access modifiers changed, original: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.mCaptureLayoutHelper == null) {
            Log.e(TAG, "Capture layout helper needs to be set first.");
            return;
        }
        RectF bottomBarRect = this.mCaptureLayoutHelper.getBottomBarRect();
        LinearLayout mLinearLayout = (LinearLayout) findViewById(R.id.more_icons_layout);
        EffectsContainerWrapper ecw = (EffectsContainerWrapper) findViewById(R.id.effect_layout_wrapper);
        if (ecw != null) {
            ecw.layout((int) bottomBarRect.left, ((int) bottomBarRect.top) - this.height, (int) bottomBarRect.right, (int) bottomBarRect.top);
            ecw.setBackgroundColor(getResources().getColor(R.color.bottombar_background_overlay));
        }
        this.mow = (MoreOptionsWrapper) findViewById(R.id.more_layout_wrapper);
        if (this.mow != null) {
            RectF preview = this.mCaptureLayoutHelper.getFullscreenRect();
            RectF rectF = this.mCaptureLayoutHelper.getPreviewRect();
            LayoutParams fl_params = (LayoutParams) mLinearLayout.getLayoutParams();
            if (rectF.left == 0.0f && rectF.top == 170.0f && rectF.right == 720.0f && rectF.bottom == 1130.0f) {
                this.mow.layout((int) bottomBarRect.left, (int) (bottomBarRect.top - (preview.bottom - preview.top)), (int) bottomBarRect.right, (int) bottomBarRect.top);
                fl_params.bottomMargin = 0;
                mLinearLayout.setLayoutParams(fl_params);
            } else {
                this.mow.layout((int) bottomBarRect.left, (int) (bottomBarRect.top - (preview.bottom - preview.top)), (int) bottomBarRect.right, (int) bottomBarRect.bottom);
                fl_params.bottomMargin = (int) (bottomBarRect.bottom - bottomBarRect.top);
                mLinearLayout.setLayoutParams(fl_params);
            }
        }
        RelativeLayout rlArcSeekBar = (RelativeLayout) findViewById(R.id.manual_arcseekbar_layout);
        if (rlArcSeekBar != null) {
            rlArcSeekBar.layout(left, top, right, bottom);
        } else {
            int i = left;
            int i2 = top;
            int i3 = right;
            int i4 = bottom;
        }
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (this.mow == null || this.mow.getVisibility() != 0) {
            return super.onTouchEvent(event);
        }
        this.mow.setVisibility(8);
        if (this.mMoreEnterToggleButton != null) {
            this.mMoreEnterToggleButton.setState(0);
        }
        return true;
    }
}
