package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.TextView;

public class StereoRotateTextView extends TextView {
    private static final int DEFAULT_GRADIENT_LEVEL = 7;
    private static final int MAX_LEVEL = 10;
    private int TOLERANCE = 10;
    private boolean mAlignLeft = true;
    private Camera mCamera = new Camera();
    private int mCorrespondingLevel;
    private int mDegreeY;
    private Shader mGradientShader;
    private int mMaxRotation = 0;

    public StereoRotateTextView(Context context) {
        super(context);
    }

    public StereoRotateTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StereoRotateTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public StereoRotateTextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void rotateY(int degree, boolean alignLeft) {
        this.mDegreeY = degree;
        this.mAlignLeft = alignLeft;
    }

    public void setMaxRotation(int rotation) {
        this.mMaxRotation = rotation;
    }

    private float getGradientRadius(int degree) {
        float radius = ((float) Math.abs(Math.abs(degree) - this.mMaxRotation)) / ((float) this.mMaxRotation);
        if (Math.abs(degree) - this.mMaxRotation > 0) {
            radius = 1.0f;
        }
        return (7.0f * radius) / 10.0f;
    }

    private boolean isCloseToMaxRotation(int degree) {
        return Math.abs(Math.abs(degree) - this.mMaxRotation) < this.TOLERANCE || Math.abs(degree) > this.mMaxRotation;
    }

    /* Access modifiers changed, original: protected */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }
}
