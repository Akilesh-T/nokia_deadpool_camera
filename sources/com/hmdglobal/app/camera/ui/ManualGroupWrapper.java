package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import com.hmdglobal.app.camera.CaptureLayoutHelper;
import com.hmdglobal.app.camera.R;

public class ManualGroupWrapper extends FrameLayout {
    private RelativeLayout mArcSeekBarmultiple_actions;
    private CaptureLayoutHelper mCaptureLayoutHelper = null;
    private View mManualGroup;

    public ManualGroupWrapper(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void onFinishInflate() {
        this.mManualGroup = findViewById(R.id.manual_items_layout);
        this.mArcSeekBarmultiple_actions = (RelativeLayout) findViewById(R.id.arcseekbar_multiple_actions);
    }

    public void setCaptureLayoutHelper(CaptureLayoutHelper helper) {
        this.mCaptureLayoutHelper = helper;
    }

    /* Access modifiers changed, original: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.mCaptureLayoutHelper != null) {
            RectF uncoveredPreviewRect = this.mCaptureLayoutHelper.getUncoveredPreviewRect();
            this.mManualGroup.layout(0, 0, 0, 0);
            this.mArcSeekBarmultiple_actions.layout((int) uncoveredPreviewRect.left, 160, (int) uncoveredPreviewRect.right, (int) uncoveredPreviewRect.bottom);
        }
    }
}
