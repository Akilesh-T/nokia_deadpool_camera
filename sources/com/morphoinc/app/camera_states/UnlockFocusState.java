package com.morphoinc.app.camera_states;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Builder;
import android.hardware.camera2.TotalCaptureResult;
import com.morphoinc.app.camera_states.MorphoPanoramaGP3CameraState.CameraStartupInfo;

public class UnlockFocusState extends MorphoPanoramaGP3CameraState {
    public void onStart() {
        CameraStartupInfo cameraStartupInfo = getCameraStartupInfo();
        try {
            Builder request = cameraStartupInfo.cameraInfo.getOpenCameraDevice().createCaptureRequest(1);
            request.addTarget(cameraStartupInfo.previewSurface);
            request.set(CaptureRequest.CONTROL_AF_TRIGGER, Integer.valueOf(2));
            PreviewState.setupPreviewRequest(this, request, false, cameraStartupInfo);
            PreviewState.setupAvailableImageQualitySettings(this, request, cameraStartupInfo);
            cameraStartupInfo.cameraInfo.getCaptureSession().capture(request.build(), cameraStartupInfo.captureCallback, backgroundHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void onCaptureCompleted(CaptureRequest request, TotalCaptureResult result) {
        getCameraStartupInfo().gp3Callback.requestUiRunnable(new Runnable() {
            public void run() {
                CameraState nextState = new PreviewState();
                UnlockFocusState.this.getCameraStartupInfo().gp3Callback.updateCameraState(nextState);
                nextState.onStart();
            }
        });
    }

    public boolean canExit() {
        return false;
    }
}
