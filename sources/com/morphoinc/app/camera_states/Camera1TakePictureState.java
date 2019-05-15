package com.morphoinc.app.camera_states;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import com.morphoinc.app.panoramagp3.Camera1Image;
import com.morphoinc.app.panoramagp3.CameraConstants;
import com.morphoinc.app.panoramagp3.CameraInfo;
import com.morphoinc.app.panoramagp3.MorphoCameraBase.IMorphoCameraListener;
import com.morphoinc.app.panoramagp3.PanoramaState;
import com.morphoinc.app.panoramagp3.PanoramaState.IPanoramaStateEventListener;
import com.morphoinc.utils.os.BuildUtil;

public class Camera1TakePictureState extends Camera1State implements IPanoramaStateEventListener, PreviewCallback {
    private int mResultCode = 0;

    public Camera1TakePictureState(Camera camera, CameraInfo cameraInfo, IMorphoPanoramaGP3Callback gp3Callback, IMorphoCameraListener listener) {
        super(camera, cameraInfo, gp3Callback, listener);
    }

    public Camera1TakePictureState(CameraState currentState) {
        super((Camera1State) currentState);
    }

    public void onStart() {
        this.mGP3Callback.onTakePictureStart(this);
        this.mCamera.setPreviewCallback(this);
        Parameters parameters = this.mCamera.getParameters();
        parameters.setAutoExposureLock(this.mGP3Callback.isAutoAELock());
        parameters.setAutoWhiteBalanceLock(this.mGP3Callback.isAutoWBLock());
        this.mCamera.setParameters(parameters);
    }

    public boolean onFinish() {
        if (this.mGP3Callback.onTakePictureFinish()) {
            return true;
        }
        this.mGP3Callback.onAttachEnd();
        if (!BuildUtil.isSony()) {
            this.mGP3Callback.setGravitySensorListener(true);
        }
        this.mGP3Callback.onTakePictureFinish2NextState(this.mResultCode);
        return true;
    }

    public void onCancel() {
        if (!BuildUtil.isSony()) {
            this.mGP3Callback.setGravitySensorListener(false);
        }
        synchronized (CameraConstants.EngineSynchronizedObject) {
            this.mGP3Callback.onAttachEnd();
            commonTerminate();
            this.mGP3Callback.onTakePictureCancel();
        }
    }

    public boolean canExit() {
        return false;
    }

    private void commonTerminate() {
        this.mCamera.setPreviewCallback(null);
        if (CameraConstants.AutoFocusType == 0) {
            this.mGP3Callback.updateCameraState(new Camera1PreviewState(this));
        } else {
            this.mGP3Callback.updateCameraState(new Camera1UnlockFocusState(this));
        }
        if (!BuildUtil.isSony()) {
            this.mGP3Callback.setGravitySensorListener(true);
        }
    }

    public void requestEnd(PanoramaState sender, int resultCode) {
        this.mResultCode = resultCode;
        this.mGP3Callback.onAttachEnd();
        commonTerminate();
        this.mGP3Callback.onTakePictureFinish2NextState(this.mResultCode);
    }

    public void onPreviewFrame(byte[] data, Camera camera) {
        Camera1Image captureImage = new Camera1Image(data, this.mCameraInfo.getCaptureWidth(), this.mCameraInfo.getCaptureHeight());
        if (!this.mListener.onPictureTaken(captureImage)) {
            captureImage.close();
        }
    }
}
