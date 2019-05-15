package com.morphoinc.app.camera_states;

import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import com.morphoinc.app.camera_states.MorphoPanoramaGP3CameraState.CameraStartupInfo;
import com.morphoinc.app.panoramagp3.CameraConstants;
import com.morphoinc.app.panoramagp3.PanoramaState;
import com.morphoinc.app.panoramagp3.PanoramaState.IPanoramaStateEventListener;
import com.morphoinc.utils.os.BuildUtil;

public class TakePictureState extends MorphoPanoramaGP3CameraState implements IPanoramaStateEventListener {
    private int mResultCode = 0;

    public void onStart() {
        CameraStartupInfo cameraStartupInfo = getCameraStartupInfo();
        cameraStartupInfo.gp3Callback.onTakePictureStart(this);
        cameraStartupInfo.imageReader.setOnImageAvailableListener(cameraStartupInfo.takePictureAvailableListener, backgroundHandler());
    }

    public boolean onFinish() {
        if (getCameraStartupInfo().gp3Callback.onTakePictureFinish()) {
            return true;
        }
        IMorphoPanoramaGP3Callback gp3Callback = getCameraStartupInfo().gp3Callback;
        gp3Callback.onAttachEnd();
        if (!BuildUtil.isSony()) {
            gp3Callback.setGravitySensorListener(true);
        }
        gp3Callback.onTakePictureFinish2NextState(this.mResultCode);
        return true;
    }

    public void onCancel() {
        IMorphoPanoramaGP3Callback gp3Callback = getCameraStartupInfo().gp3Callback;
        if (!BuildUtil.isSony()) {
            gp3Callback.setGravitySensorListener(false);
        }
        synchronized (CameraConstants.EngineSynchronizedObject) {
            gp3Callback.onAttachEnd();
            commonTerminate();
            gp3Callback.onTakePictureCancel();
        }
    }

    public boolean canExit() {
        return false;
    }

    private void commonTerminate() {
        CameraStartupInfo cameraStartupInfo = getCameraStartupInfo();
        if (cameraStartupInfo.imageReader != null) {
            cameraStartupInfo.imageReader.setOnImageAvailableListener(cameraStartupInfo.previewAvailableListener, backgroundHandler());
        }
        if (CameraConstants.AutoFocusType == 0) {
            cameraStartupInfo.gp3Callback.updateCameraState(new PreviewState());
        } else {
            cameraStartupInfo.gp3Callback.updateCameraState(new UnlockFocusState());
        }
        if (!BuildUtil.isSony()) {
            cameraStartupInfo.gp3Callback.setGravitySensorListener(true);
        }
    }

    public void requestEnd(PanoramaState sender, int resultCode) {
        this.mResultCode = resultCode;
        IMorphoPanoramaGP3Callback gp3Callback = getCameraStartupInfo().gp3Callback;
        gp3Callback.onAttachEnd();
        commonTerminate();
        gp3Callback.onTakePictureFinish2NextState(this.mResultCode);
    }

    public void onCaptureCompleted(CaptureRequest request, TotalCaptureResult result) {
        CameraStartupInfo cameraStartupInfo = getCameraStartupInfo();
        cameraStartupInfo.totalCaptureResult = result;
        cameraStartupInfo.gp3Callback.onCaptureCompleted(result);
    }
}
