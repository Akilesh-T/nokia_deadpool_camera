package com.hmdglobal.app.camera.app;

import android.graphics.SurfaceTexture;
import com.android.ex.camera2.portability.CameraAgent.CameraStartPreviewCallback;
import com.android.ex.camera2.portability.CameraDeviceInfo.Characteristics;
import com.android.ex.camera2.portability.CameraExceptionHandler;
import com.android.ex.camera2.portability.CameraSettings.BoostParameters;

public interface CameraProvider {
    void boostApplySettings(BoostParameters boostParameters);

    void boostSetPreviewTexture(SurfaceTexture surfaceTexture);

    void boostStartPreview(CameraStartPreviewCallback cameraStartPreviewCallback);

    Characteristics getCharacteristics(int i);

    int getCurrentCameraId();

    int getFirstBackCameraId();

    int getFirstFrontCameraId();

    int getNumberOfCameras();

    int getSupportedHardwareLevel(int i);

    boolean isBackFacingCamera(int i);

    boolean isBoostPreview();

    boolean isCameraOpenSuccess();

    boolean isCameraRequestBoosted();

    boolean isFrontFacingCamera(int i);

    void releaseCamera(int i);

    void requestCamera(int i);

    void requestCamera(int i, boolean z);

    void requestCamera(int i, boolean z, boolean z2, BoostParameters boostParameters);

    void setCameraExceptionHandler(CameraExceptionHandler cameraExceptionHandler);

    boolean waitingForCamera();
}
