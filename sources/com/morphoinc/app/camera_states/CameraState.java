package com.morphoinc.app.camera_states;

import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;

public class CameraState {
    public void onStart() {
    }

    public void onCaptureCompleted(CaptureRequest request, TotalCaptureResult result) {
    }

    public void onProgressed(CaptureResult partialResult) {
    }

    public void onCaptureSequenceCompleted(int sequenceId) {
    }

    public void onTouch() {
    }

    public boolean onFinish() {
        return false;
    }

    public void onCancel() {
    }

    public void onStop() {
    }

    public void onTakePictureStart() {
    }

    public void onRequestParamChange() {
    }

    public boolean canExit() {
        return true;
    }
}
