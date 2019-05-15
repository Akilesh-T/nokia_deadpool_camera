package com.hmdglobal.app.camera.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.widget.AspectRatioSelector.AspectRatio;

public class AspectRatioDialogLayout extends FrameLayout {
    private AspectRatio mAspectRatio;
    private AspectRatioSelector mAspectRatioSelector;
    private View mConfirmButton;
    private boolean mInitialized;
    private int mLastOrientation = getResources().getConfiguration().orientation;
    private AspectRatioChangedListener mListener;

    public interface AspectRatioChangedListener {
        void onAspectRatioChanged(AspectRatio aspectRatio);
    }

    public AspectRatioDialogLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void onFinishInflate() {
        updateViewReference();
    }

    private void updateViewReference() {
        this.mAspectRatioSelector = (AspectRatioSelector) findViewById(R.id.aspect_ratio_selector);
        this.mConfirmButton = findViewById(R.id.confirm_button);
        this.mConfirmButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (AspectRatioDialogLayout.this.mListener != null) {
                    AspectRatioDialogLayout.this.mListener.onAspectRatioChanged(AspectRatioDialogLayout.this.mAspectRatioSelector.getAspectRatio());
                }
            }
        });
        if (this.mInitialized) {
            this.mAspectRatioSelector.setAspectRatio(this.mAspectRatio);
        }
    }

    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        if (config.orientation != this.mLastOrientation) {
            this.mLastOrientation = config.orientation;
            this.mAspectRatio = this.mAspectRatioSelector.getAspectRatio();
            removeAllViews();
            inflate(getContext(), R.layout.aspect_ratio_dialog_content, this);
            updateViewReference();
        }
    }

    public void setAspectRatio(AspectRatio aspectRatio) {
        this.mAspectRatioSelector.setAspectRatio(aspectRatio);
    }

    public void initialize(AspectRatioChangedListener listener, AspectRatio aspectRatio) {
        this.mInitialized = true;
        this.mListener = listener;
        this.mAspectRatio = aspectRatio;
        if (this.mAspectRatioSelector != null) {
            this.mAspectRatioSelector.setAspectRatio(this.mAspectRatio);
        }
    }
}
