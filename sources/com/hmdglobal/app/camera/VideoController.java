package com.hmdglobal.app.camera;

import android.view.View;
import com.hmdglobal.app.camera.ShutterButton.OnShutterButtonListener;

public interface VideoController extends OnShutterButtonListener {
    public static final int IDLE = 1;
    public static final int PREVIEW_STOPPED = 0;
    public static final int RECORDING = 3;
    public static final int RECORDING_PENDING_START = 2;
    public static final int RECORDING_PENDING_STOP = 4;

    void doVideoCapture();

    boolean isInReviewMode();

    boolean isVideoCaptureIntent();

    void onEvoChanged(int i);

    void onPreviewUIDestroyed();

    void onPreviewUIReady();

    void onReviewCancelClicked(View view);

    void onReviewDoneClicked(View view);

    void onReviewPlayClicked(View view);

    void onSingleTapUp(View view, int i, int i2);

    void onZoomChanged(float f);

    void pauseVideoRecording();

    void startPreCaptureAnimation();

    void stopPreview();

    void updateCameraOrientation();

    void updatePreviewAspectRatio(float f);
}
