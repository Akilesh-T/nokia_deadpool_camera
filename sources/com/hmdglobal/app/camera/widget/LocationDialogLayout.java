package com.hmdglobal.app.camera.widget;

import android.content.Context;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import com.hmdglobal.app.camera.R;

public class LocationDialogLayout extends FrameLayout {
    private CheckBox mCheckBox;
    private boolean mCheckBoxChecked = true;
    private View mConfirmButton;
    private int mLastOrientation;
    private LocationTaggingSelectionListener mListener;

    public interface LocationTaggingSelectionListener {
        void onLocationTaggingSelected(boolean z);
    }

    public LocationDialogLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mLastOrientation = context.getResources().getConfiguration().orientation;
    }

    public void onFinishInflate() {
        updateViewReference();
    }

    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        if (config.orientation != this.mLastOrientation) {
            this.mLastOrientation = config.orientation;
            removeAllViews();
            inflate(getContext(), R.layout.location_dialog_content, this);
            updateViewReference();
        }
    }

    private void updateViewReference() {
        this.mCheckBox = (CheckBox) findViewById(R.id.check_box);
        this.mCheckBox.setChecked(this.mCheckBoxChecked);
        this.mCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                LocationDialogLayout.this.mCheckBoxChecked = isChecked;
            }
        });
        this.mConfirmButton = findViewById(R.id.confirm_button);
        this.mConfirmButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (LocationDialogLayout.this.mListener != null) {
                    LocationDialogLayout.this.mListener.onLocationTaggingSelected(LocationDialogLayout.this.mCheckBoxChecked);
                }
            }
        });
    }

    public void setLocationTaggingSelectionListener(LocationTaggingSelectionListener listener) {
        this.mListener = listener;
    }
}
