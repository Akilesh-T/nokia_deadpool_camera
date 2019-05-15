package com.hmdglobal.app.camera.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.google.common.primitives.Ints;
import com.hmdglobal.app.camera.CaptureLayoutHelper;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.ShutterButton.OnShutterButtonListener;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.ui.PreviewOverlay.OnPreviewTouchedListener;
import com.hmdglobal.app.camera.ui.TouchCoordinate;

public class ModeOptionsOverlay extends FrameLayout implements OnPreviewTouchedListener, OnShutterButtonListener {
    private static final int BOTTOMBAR_OPTIONS_TIMEOUT_MS = 2000;
    private static final int BOTTOM_RIGHT = 85;
    private static final Tag TAG = new Tag("ModeOptionsOverlay");
    private static final int TOP_RIGHT = 53;
    private CaptureLayoutHelper mCaptureLayoutHelper = null;
    private ModeOptions mModeOptions;
    private LinearLayout mModeOptionsToggle;
    private ImageView mThreeDots;

    public ModeOptionsOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setCaptureLayoutHelper(CaptureLayoutHelper helper) {
        this.mCaptureLayoutHelper = helper;
    }

    public void setToggleClickable(boolean clickable) {
        this.mModeOptionsToggle.setClickable(clickable);
    }

    public void onFinishInflate() {
        this.mModeOptions = (ModeOptions) findViewById(R.id.mode_options);
        this.mModeOptions.setClickable(true);
        this.mModeOptions.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ModeOptionsOverlay.this.closeModeOptions();
            }
        });
        this.mModeOptionsToggle = (LinearLayout) findViewById(R.id.mode_options_toggle);
        this.mModeOptionsToggle.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                ModeOptionsOverlay.this.mModeOptions.animateVisible();
            }
        });
        this.mModeOptions.setViewToShowHide(this.mModeOptionsToggle);
        this.mThreeDots = (ImageView) findViewById(R.id.three_dots);
    }

    public void onPreviewTouched(MotionEvent ev) {
        closeModeOptions();
    }

    public void onShutterButtonClick() {
        closeModeOptions();
    }

    public void onShutterButtonLongClick() {
    }

    public void onShutterCoordinate(TouchCoordinate coord) {
    }

    public void onShutterButtonFocus(boolean pressed) {
    }

    public void closeModeOptions() {
        this.mModeOptions.animateHidden();
    }

    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        checkOrientation(configuration.orientation);
    }

    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (this.mCaptureLayoutHelper == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            Log.e(TAG, "Capture layout helper needs to be set first.");
            return;
        }
        RectF uncoveredPreviewRect = this.mCaptureLayoutHelper.getUncoveredPreviewRect();
        super.onMeasure(MeasureSpec.makeMeasureSpec((int) uncoveredPreviewRect.width(), Ints.MAX_POWER_OF_TWO), MeasureSpec.makeMeasureSpec((int) uncoveredPreviewRect.height(), Ints.MAX_POWER_OF_TWO));
    }

    private void checkOrientation(int orientation) {
        boolean isPortrait = true;
        if (1 != orientation) {
            isPortrait = false;
        }
        int modeOptionsDimension = (int) getResources().getDimension(R.dimen.mode_options_height);
        LayoutParams modeOptionsParams = (LayoutParams) this.mModeOptions.getLayoutParams();
        LayoutParams modeOptionsToggleParams = (LayoutParams) this.mModeOptionsToggle.getLayoutParams();
        if (isPortrait) {
            modeOptionsParams.height = modeOptionsDimension;
            modeOptionsParams.width = -1;
            modeOptionsParams.gravity = 48;
            modeOptionsToggleParams.gravity = 85;
            this.mThreeDots.setImageResource(R.drawable.ic_options_port);
        } else {
            modeOptionsParams.width = modeOptionsDimension;
            modeOptionsParams.height = -1;
            modeOptionsParams.gravity = 5;
            modeOptionsToggleParams.gravity = 53;
            this.mThreeDots.setImageResource(R.drawable.ic_options_land);
        }
        requestLayout();
    }
}
