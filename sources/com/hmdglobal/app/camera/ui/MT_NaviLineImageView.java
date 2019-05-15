package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

public class MT_NaviLineImageView extends ImageView {
    private static final String TAG = "NaviLineImageView";
    private int mBottom = 0;
    private boolean mFirstDraw = false;
    private int mLeft = 0;
    private int mRight = 0;
    private int mTop = 0;

    public MT_NaviLineImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* Access modifiers changed, original: protected */
    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[onLayout]changed=");
        stringBuilder.append(changed);
        stringBuilder.append(" left =");
        stringBuilder.append(left);
        stringBuilder.append(" top = ");
        stringBuilder.append(top);
        stringBuilder.append(" right = ");
        stringBuilder.append(right);
        stringBuilder.append(" bottom = ");
        stringBuilder.append(bottom);
        Log.v(str, stringBuilder.toString());
        super.onLayout(changed, left, top, right, bottom);
    }

    public void layout(int l, int t, int r, int b) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[layout]left =");
        stringBuilder.append(l);
        stringBuilder.append(" top = ");
        stringBuilder.append(t);
        stringBuilder.append(" right = ");
        stringBuilder.append(r);
        stringBuilder.append(" bottom = ");
        stringBuilder.append(b);
        Log.v(str, stringBuilder.toString());
        if (!this.mFirstDraw || (this.mLeft == l && this.mTop == t && this.mRight == r && this.mBottom == b)) {
            super.layout(l, t, r, b);
            this.mFirstDraw = true;
        }
    }

    public void setLayoutPosition(int l, int t, int r, int b) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[setLayoutPosition] left =");
        stringBuilder.append(l);
        stringBuilder.append(" top = ");
        stringBuilder.append(t);
        stringBuilder.append(" right = ");
        stringBuilder.append(r);
        stringBuilder.append(" bottom = ");
        stringBuilder.append(b);
        Log.v(str, stringBuilder.toString());
        this.mLeft = l;
        this.mTop = t;
        this.mRight = r;
        this.mBottom = b;
        layout(l, t, r, b);
    }
}
