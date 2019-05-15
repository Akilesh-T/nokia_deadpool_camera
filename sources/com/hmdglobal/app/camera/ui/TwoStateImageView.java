package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class TwoStateImageView extends ImageView {
    private static final int DISABLED_ALPHA = 102;
    private static final int ENABLED_ALPHA = 255;
    private boolean mFilterEnabled;

    public TwoStateImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mFilterEnabled = true;
    }

    public TwoStateImageView(Context context) {
        this(context, null);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!this.mFilterEnabled) {
            return;
        }
        if (enabled) {
            setAlpha(255);
        } else {
            setAlpha(102);
        }
    }

    public void enableFilter(boolean enabled) {
        this.mFilterEnabled = enabled;
    }
}
