package com.morphoinc.app.camera_states;

import android.hardware.camera2.TotalCaptureResult;
import com.morphoinc.app.panoramagp3.PanoramaState.IPanoramaStateEventListener;

public interface IMorphoPanoramaGP3Callback {
    int getAntiBanding();

    int getAntiFlickerMode();

    int getCaptureMode();

    int getColorCorrectionMode();

    int getEdgeMode();

    int getNoiseReductionMode();

    int getShadingMode();

    int getTonemapMode();

    boolean isAutoAELock();

    boolean isAutoEdgeNR();

    boolean isAutoWBLock();

    boolean isEngineRunning();

    boolean isInfinityFocus();

    boolean isTvLock();

    void onAttachEnd();

    void onCaptureCompleted(TotalCaptureResult totalCaptureResult);

    void onPreviewStart();

    void onTakePictureCancel();

    boolean onTakePictureFinish();

    void onTakePictureFinish2NextState(int i);

    void onTakePicturePreprocess();

    void onTakePictureStart(IPanoramaStateEventListener iPanoramaStateEventListener);

    void playAutoFocusSound();

    void requestUiRunnable(Runnable runnable);

    void setEdgeMode(int i);

    void setGravitySensorListener(boolean z);

    void setNoiseReductionMode(int i);

    void setNullDirectionFunction();

    void setUnsharpStrength(int i);

    void updateCameraState(CameraState cameraState);

    void updateTvValue();

    boolean useOis();
}
