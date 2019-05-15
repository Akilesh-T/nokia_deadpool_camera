package com.hmdglobal.app.camera;

import android.view.View;
import com.hmdglobal.app.camera.ShutterButton.OnShutterButtonListener;

public interface PhotoController extends OnShutterButtonListener {
    public static final int AE_AF_LOCKED = 5;
    public static final int FOCUSING = 2;
    public static final int IDLE = 1;
    public static final int PREVIEW_STOPPED = 0;
    public static final int SCANING_FOR_AE_AF_LOCK = 6;
    public static final int SNAPSHOT_IN_PROGRESS = 3;
    public static final int SNAPSHOT_IN_PROGRESS_DURING_LOCKED = 7;
    public static final int SNAPSHOT_LONGSHOT = 9;
    public static final int SNAPSHOT_LONGSHOT_PENDING_START = 8;
    public static final int SNAPSHOT_LONGSHOT_PENDING_STOP = 10;
    public static final int SWITCHING_CAMERA = 4;

    boolean canCloseCamera();

    boolean cancelAutoFocus();

    int getCameraState();

    boolean isAttentionSeekerShow();

    boolean isCameraIdle();

    boolean isFacebeautyEnabled();

    boolean isGesturePalmShow();

    boolean isImageCaptureIntent();

    void onCaptureCancelled();

    void onCaptureDone();

    void onCaptureRetake();

    void onEvoChanged(int i);

    void onLongPress(float f, float f2);

    void onPreviewUIDestroyed();

    void onPreviewUIReady();

    void onSingleTapUp(View view, int i, int i2);

    void onZoomChanged(float f);

    void startPreCaptureAnimation();

    void stopPreview();

    void updateCameraOrientation();

    void updateFaceBeautySetting(String str, int i);

    void updatePreviewAspectRatio(float f);
}
