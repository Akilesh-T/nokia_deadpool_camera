package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import com.hmdglobal.app.camera.R;

public class MoreOptionsWrapper extends FrameLayout {
    private FrameLayout mMoreOperatorLayout;

    public MoreOptionsWrapper(Context context) {
        this(context, null);
    }

    public MoreOptionsWrapper(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MoreOptionsWrapper(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /* Access modifiers changed, original: protected */
    public void onFinishInflate() {
        super.onFinishInflate();
        init();
    }

    public void init() {
        this.mMoreOperatorLayout = (FrameLayout) findViewById(R.id.more_operator_layout);
        this.mMoreOperatorLayout.setVisibility(0);
    }
}
