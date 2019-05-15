package com.hmdglobal.app.camera.ui;

import android.graphics.RectF;
import android.view.GestureDetector.OnGestureListener;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnTouchListener;

public interface PreviewStatusListener extends SurfaceTextureListener {

    public interface PreviewAreaChangedListener {
        void onPreviewAreaChanged(RectF rectF);
    }

    public interface PreviewAspectRatioChangedListener {
        void onPreviewAspectRatioChanged(float f);
    }

    void clearEvoPendingUI();

    OnGestureListener getGestureListener();

    OnTouchListener getTouchListener();

    void onPreviewFlipped();

    void onPreviewLayoutChanged(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8);

    boolean shouldAutoAdjustBottomBar();

    boolean shouldAutoAdjustTransformMatrixOnLayout();
}
