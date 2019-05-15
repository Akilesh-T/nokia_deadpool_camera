package com.morphoinc.app.camera_states;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import com.morphoinc.app.panoramagp3.CameraConstants;
import com.morphoinc.app.panoramagp3.CameraInfo;
import com.morphoinc.app.panoramagp3.MorphoCameraBase.IMorphoCameraListener;

public class Camera1PreviewState extends Camera1State implements PreviewCallback {
    public Camera1PreviewState(Camera camera, CameraInfo cameraInfo, IMorphoPanoramaGP3Callback gp3Callback, IMorphoCameraListener listener) {
        super(camera, cameraInfo, gp3Callback, listener);
    }

    public Camera1PreviewState(CameraState currentState) {
        super((Camera1State) currentState);
    }

    public void onStart() {
        Parameters parameters = this.mCamera.getParameters();
        parameters.setAutoExposureLock(false);
        parameters.setAutoWhiteBalanceLock(false);
        this.mCamera.setParameters(parameters);
        this.mCamera.setPreviewCallback(this);
        this.mCamera.startPreview();
        this.mGP3Callback.onPreviewStart();
        this.mGP3Callback.setNullDirectionFunction();
    }

    public void onTouch() {
        CameraState nextState = new Camera1AutoFocusState(this);
        this.mGP3Callback.updateCameraState(nextState);
        nextState.onStart();
    }

    public void onTakePictureStart() {
        if (CameraConstants.AutoFocusType == 0) {
            toTakePictureState();
            return;
        }
        CameraState nextState = new Camera1AutoFocusState(this);
        this.mGP3Callback.updateCameraState(nextState);
        nextState.onStart();
    }

    public void toTakePictureState() {
        onStop();
        this.mGP3Callback.onTakePicturePreprocess();
        CameraState nextState = new Camera1TakePictureState(this);
        this.mGP3Callback.updateCameraState(nextState);
        nextState.onStart();
    }

    public void onPreviewFrame(byte[] data, Camera camera) {
        this.mListener.onPreviewImage(data);
    }
}
