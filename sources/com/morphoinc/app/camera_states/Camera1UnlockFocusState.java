package com.morphoinc.app.camera_states;

import android.hardware.Camera;
import com.morphoinc.app.panoramagp3.CameraInfo;
import com.morphoinc.app.panoramagp3.MorphoCameraBase.IMorphoCameraListener;

public class Camera1UnlockFocusState extends Camera1State {
    public Camera1UnlockFocusState(Camera camera, CameraInfo cameraInfo, IMorphoPanoramaGP3Callback gp3Callback, IMorphoCameraListener listener) {
        super(camera, cameraInfo, gp3Callback, listener);
    }

    public Camera1UnlockFocusState(CameraState currentState) {
        super((Camera1State) currentState);
    }

    public void onStart() {
        this.mCamera.autoFocus(null);
        Camera1PreviewState nextState = new Camera1PreviewState(this);
        this.mGP3Callback.updateCameraState(nextState);
        nextState.onStart();
    }

    public boolean canExit() {
        return false;
    }
}
