package com.hmdglobal.app.camera;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

public class ToggleImageButton extends MultiToggleImageButton {
    private OnStateChangeListener mOnStateChangeListener;

    public interface OnStateChangeListener {
        void stateChanged(View view, boolean z);
    }

    public ToggleImageButton(Context context) {
        super(context);
    }

    public ToggleImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ToggleImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setOnStateChangeListener(OnStateChangeListener onStateChangeListener) {
        this.mOnStateChangeListener = onStateChangeListener;
    }

    public void setState(boolean state) {
        super.setState(state);
    }

    /* Access modifiers changed, original: protected */
    public void init() {
        super.init();
        super.setOnStateChangeListener(new com.hmdglobal.app.camera.MultiToggleImageButton.OnStateChangeListener() {
            public void stateChanged(View v, int state) {
                if (ToggleImageButton.this.mOnStateChangeListener != null) {
                    OnStateChangeListener access$000 = ToggleImageButton.this.mOnStateChangeListener;
                    boolean z = true;
                    if (state != 1) {
                        z = false;
                    }
                    access$000.stateChanged(v, z);
                }
            }
        });
    }
}
