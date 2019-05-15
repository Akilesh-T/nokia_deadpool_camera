package com.morphoinc.app.camera_states;

import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import com.morphoinc.app.LogFilter;
import com.morphoinc.app.panoramagp3.CameraConstants;
import com.morphoinc.app.panoramagp3.CameraInfo;
import com.morphoinc.app.panoramagp3.MorphoCameraBase.IMorphoCameraListener;

public class Camera1AutoFocusState extends Camera1State implements AutoFocusCallback {
    private boolean mCancel = false;

    public Camera1AutoFocusState(Camera camera, CameraInfo cameraInfo, IMorphoPanoramaGP3Callback gp3Callback, IMorphoCameraListener listener) {
        super(camera, cameraInfo, gp3Callback, listener);
    }

    public Camera1AutoFocusState(CameraState currentState) {
        super((Camera1State) currentState);
    }

    public void onStart() {
        this.mCamera.autoFocus(this);
    }

    public void onCancel() {
        this.mCancel = true;
        this.mGP3Callback.updateCameraState(new Camera1PreviewState(this));
    }

    public boolean canExit() {
        return false;
    }

    public void onAutoFocus(boolean success, Camera camera) {
        if (this.mCancel) {
            LogFilter.i("MorphoCamera1State", "AutoFocus canceled.");
        } else if (CameraConstants.AutoFocusType == 0) {
            CameraState nextState = new Camera1PreviewState(this);
            this.mGP3Callback.updateCameraState(nextState);
            nextState.onStart();
        } else {
            Camera1PreviewState nextState2 = new Camera1PreviewState(this);
            this.mGP3Callback.updateCameraState(nextState2);
            nextState2.toTakePictureState();
        }
    }
}
