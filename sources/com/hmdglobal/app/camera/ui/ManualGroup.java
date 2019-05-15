package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View.MeasureSpec;
import android.widget.FrameLayout;
import com.google.common.primitives.Ints;
import com.hmdglobal.app.camera.CaptureLayoutHelper;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.ShutterButton.OnShutterButtonListener;
import com.hmdglobal.app.camera.app.CameraAppUI.OnModeOptionsVisibilityChangedListener;
import com.hmdglobal.app.camera.ui.PreviewOverlay.OnPreviewTouchedListener;
import com.hmdglobal.app.camera.widget.FloatingActionsMenu;

public class ManualGroup extends FrameLayout implements OnPreviewTouchedListener, OnShutterButtonListener, OnModeOptionsVisibilityChangedListener {
    private CaptureLayoutHelper mCaptureLayoutHelper = null;
    private FloatingActionsMenu mFloatingActionsMenu;

    public ManualGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void onFinishInflate() {
        super.onFinishInflate();
        this.mFloatingActionsMenu = (FloatingActionsMenu) findViewById(R.id.multiple_actions);
    }

    /* Access modifiers changed, original: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    /* Access modifiers changed, original: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.mCaptureLayoutHelper == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        RectF uncoveredPreviewRect = this.mCaptureLayoutHelper.getUncoveredPreviewRect();
        super.onMeasure(MeasureSpec.makeMeasureSpec((int) uncoveredPreviewRect.width(), Ints.MAX_POWER_OF_TWO), MeasureSpec.makeMeasureSpec((int) uncoveredPreviewRect.height(), Ints.MAX_POWER_OF_TWO));
    }

    public void setCaptureLayoutHelper(CaptureLayoutHelper helper) {
        this.mCaptureLayoutHelper = helper;
    }

    public void onPreviewTouched(MotionEvent ev) {
        this.mFloatingActionsMenu.collapse();
    }

    public void onShutterButtonClick() {
        this.mFloatingActionsMenu.collapse();
    }

    public void onShutterButtonLongClick() {
    }

    public void onShutterCoordinate(TouchCoordinate coord) {
    }

    public void onShutterButtonFocus(boolean pressed) {
        this.mFloatingActionsMenu.collapse();
    }

    public void onModeOptionsVisibilityChanged(int vis) {
        setVisibility(vis);
    }
}
