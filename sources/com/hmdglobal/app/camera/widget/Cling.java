package com.hmdglobal.app.camera.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.widget.TextView;

public class Cling extends TextView {
    private boolean mDelayDrawingUntilNextLayout;
    private final OnLayoutChangeListener mLayoutChangeListener;
    private final int[] mLocation;
    private View mReferenceView;

    public Cling(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mReferenceView = null;
        this.mLocation = new int[2];
        this.mLayoutChangeListener = new OnLayoutChangeListener() {
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                Cling.this.mDelayDrawingUntilNextLayout = false;
                Cling.this.adjustPosition();
            }
        };
        this.mDelayDrawingUntilNextLayout = false;
    }

    public Cling(Context context) {
        super(context);
        this.mReferenceView = null;
        this.mLocation = new int[2];
        this.mLayoutChangeListener = /* anonymous class already generated */;
        this.mDelayDrawingUntilNextLayout = false;
    }

    public void setReferenceView(View v) {
        if (v == null) {
            if (this.mReferenceView != null) {
                this.mReferenceView.removeOnLayoutChangeListener(this.mLayoutChangeListener);
                this.mReferenceView = null;
            }
            return;
        }
        this.mReferenceView = v;
        this.mReferenceView.addOnLayoutChangeListener(this.mLayoutChangeListener);
        if (this.mReferenceView.getVisibility() == 8) {
            this.mDelayDrawingUntilNextLayout = true;
        } else {
            adjustPosition();
        }
    }

    public void adjustPosition() {
        if (this.mReferenceView != null) {
            this.mReferenceView.getLocationInWindow(this.mLocation);
            int refCenterX = this.mLocation[0] + (this.mReferenceView.getWidth() / 2);
            int left = refCenterX - (getWidth() / 2);
            int top = this.mLocation[1] - getHeight();
            getLocationInWindow(this.mLocation);
            int currentTop = this.mLocation[1] - ((int) getTranslationY());
            setTranslationX((float) (left - (this.mLocation[0] - ((int) getTranslationX()))));
            setTranslationY((float) (top - currentTop));
        }
    }

    public void draw(Canvas canvas) {
        if (!this.mDelayDrawingUntilNextLayout) {
            super.draw(canvas);
        }
    }
}
