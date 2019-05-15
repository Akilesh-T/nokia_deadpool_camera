package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View.MeasureSpec;
import android.widget.GridView;

public class ExpandedGridView extends GridView {
    public ExpandedGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* Access modifiers changed, original: protected */
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(65536, Integer.MIN_VALUE));
    }
}
