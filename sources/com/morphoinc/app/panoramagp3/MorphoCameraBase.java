package com.morphoinc.app.panoramagp3;

import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.util.Size;
import com.morphoinc.app.camera_states.CameraState;
import com.morphoinc.app.camera_states.IMorphoPanoramaGP3Callback;

public abstract class MorphoCameraBase {
    static final IMorphoCameraListener nullMorphoCameraListener = new IMorphoCameraListener() {
        public void onOpened() {
        }

        public void onCaptureCompleted(CaptureRequest request, TotalCaptureResult result) {
        }

        public boolean onPreviewImageAvailable() {
            return false;
        }

        public void onPreviewImage(byte[] data) {
        }

        public boolean onPictureTaken(CaptureImage image) {
            return false;
        }
    };

    public interface IMorphoCameraListener {
        void onCaptureCompleted(CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult);

        void onOpened();

        boolean onPictureTaken(CaptureImage captureImage);

        void onPreviewImage(byte[] bArr);

        boolean onPreviewImageAvailable();
    }

    public abstract int burstRemaining();

    public abstract CameraInfo cameraInfo();

    public abstract CameraState cameraState();

    public abstract void exit();

    public abstract String[] getAllCameras();

    public abstract int[] getSupportedPreviewSizes();

    public abstract boolean isFrontCamera(int i);

    public abstract void pause();

    public abstract void resume(Size size, Size size2);

    public abstract void setBurstRemaining(int i);

    public abstract void setDefaultCameraState();

    public abstract void setMorphoPanoramaGP3Interface(IMorphoPanoramaGP3Callback iMorphoPanoramaGP3Callback);

    public abstract void takePicture();

    public abstract void takePictureBurst();

    public abstract void takePictureZSL();

    public abstract void updateCameraState(CameraState cameraState);

    public String[] getAvailableColorCorrectionMode() {
        return null;
    }

    public String[] getAvailableColorCorrectionModeValues() {
        return null;
    }

    public String[] getColorCorrectionModeDefaultValues() {
        return null;
    }

    public String[] getAvailableEdgeMode() {
        return null;
    }

    public String[] getAvailableEdgeModeValues() {
        return null;
    }

    public String[] getEdgeModeDefaultValues() {
        return null;
    }

    public String[] getAvailableNoiseReductionMode() {
        return null;
    }

    public String[] getAvailableNoiseReductionModeValues() {
        return null;
    }

    public String[] getNoiseReductionModeDefaultValues() {
        return null;
    }

    public String[] getAvailableShadingMode() {
        return null;
    }

    public String[] getAvailableShadingModeValues() {
        return null;
    }

    public String[] getShadingModeDefaultValues() {
        return null;
    }

    public String[] getAvailableTonemapMode() {
        return null;
    }

    public String[] getAvailableTonemapModeValues() {
        return null;
    }

    public String[] getTonemapModeDefaultValues() {
        return null;
    }

    public String getHardwareLevel() {
        return null;
    }

    public String getTimestampSource() {
        return null;
    }
}
