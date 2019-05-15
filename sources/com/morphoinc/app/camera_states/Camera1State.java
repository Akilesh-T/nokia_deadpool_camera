package com.morphoinc.app.camera_states;

import android.hardware.Camera;
import com.morphoinc.app.panoramagp3.CameraInfo;
import com.morphoinc.app.panoramagp3.MorphoCameraBase.IMorphoCameraListener;

public class Camera1State extends CameraState {
    static final String LOG_TAG = "MorphoCamera1State";
    final Camera mCamera;
    final CameraInfo mCameraInfo;
    final IMorphoPanoramaGP3Callback mGP3Callback;
    final IMorphoCameraListener mListener;

    public Camera1State(Camera camera, CameraInfo cameraInfo, IMorphoPanoramaGP3Callback gp3Callback, IMorphoCameraListener listener) {
        this.mCamera = camera;
        this.mCameraInfo = cameraInfo;
        if (gp3Callback == null) {
            this.mGP3Callback = MorphoPanoramaGP3CameraState.nullGP3Callback;
        } else {
            this.mGP3Callback = gp3Callback;
        }
        this.mListener = listener;
    }

    Camera1State(Camera1State currentState) {
        this.mCamera = currentState.mCamera;
        this.mCameraInfo = currentState.mCameraInfo;
        this.mGP3Callback = currentState.mGP3Callback;
        this.mListener = currentState.mListener;
    }
}
