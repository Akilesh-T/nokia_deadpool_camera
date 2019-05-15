package com.morphoinc.app.camera_states;

import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Builder;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;
import com.morphoinc.app.panoramagp3.Camera2ParamsFragment;
import com.morphoinc.app.panoramagp3.CameraInfo;
import com.morphoinc.app.panoramagp3.PanoramaState.IPanoramaStateEventListener;
import java.util.ArrayList;
import java.util.List;

public class MorphoPanoramaGP3CameraState extends CameraState {
    static final String LOG_TAG = "MorphoCamera2State";
    private static CameraStartupInfo cameraStartup = new CameraStartupInfo(0);
    private static final double[] mTvBuf = new double[8];
    private static int mTvBufIndex = 0;
    static final IMorphoPanoramaGP3Callback nullGP3Callback = new IMorphoPanoramaGP3Callback() {
        public boolean isEngineRunning() {
            return false;
        }

        public boolean isTvLock() {
            return false;
        }

        public boolean useOis() {
            return false;
        }

        public int getAntiBanding() {
            return 0;
        }

        public int getCaptureMode() {
            return 0;
        }

        public int getColorCorrectionMode() {
            return 0;
        }

        public int getEdgeMode() {
            return 0;
        }

        public int getNoiseReductionMode() {
            return 0;
        }

        public int getShadingMode() {
            return 0;
        }

        public int getTonemapMode() {
            return 0;
        }

        public int getAntiFlickerMode() {
            return 1;
        }

        public boolean isAutoAELock() {
            return false;
        }

        public boolean isAutoWBLock() {
            return false;
        }

        public boolean isAutoEdgeNR() {
            return false;
        }

        public void onPreviewStart() {
        }

        public void playAutoFocusSound() {
        }

        public void requestUiRunnable(Runnable action) {
        }

        public void onTakePicturePreprocess() {
        }

        public void onTakePictureStart(IPanoramaStateEventListener listener) {
        }

        public boolean onTakePictureFinish() {
            return false;
        }

        public void onTakePictureFinish2NextState(int resultCode) {
        }

        public void onTakePictureCancel() {
        }

        public void onAttachEnd() {
        }

        public void setGravitySensorListener(boolean register) {
        }

        public void setUnsharpStrength(int strength) {
        }

        public void setEdgeMode(int mode) {
        }

        public void setNoiseReductionMode(int mode) {
        }

        public void updateTvValue() {
        }

        public boolean isInfinityFocus() {
            return false;
        }

        public void updateCameraState(CameraState newState) {
        }

        public void setNullDirectionFunction() {
        }

        public void onCaptureCompleted(TotalCaptureResult result) {
        }
    };

    public static class CameraStartupInfo {
        public boolean available_ae_mode = false;
        public boolean available_af_mode = false;
        public boolean available_antibanding_mode = false;
        public boolean available_image_quality_settings = false;
        public boolean available_ois_mode = false;
        public boolean available_scene_mode_sports = false;
        public Handler backgroundHandler = null;
        public HandlerThread backgroundHandlerThread = null;
        public int burstRemaining;
        public final List<CaptureRequest> burstRequestList;
        public Camera2ParamsFragment camera2Params;
        public CameraInfo cameraInfo;
        public CaptureCallback captureCallback;
        public CaptureRequest captureRequest;
        public final int capture_mode;
        public Builder currentBurstRequestBuilder;
        public Builder currentRequestBuilder;
        public IMorphoPanoramaGP3Callback gp3Callback;
        public ImageReader imageReader = null;
        public ImageReader imageReaderIdling = null;
        public OnImageAvailableListener previewAvailableListener;
        public Surface previewSurface = null;
        public OnImageAvailableListener takePictureAvailableListener;
        public TotalCaptureResult totalCaptureResult;

        public CameraStartupInfo(int capture_mode) {
            this.capture_mode = capture_mode;
            this.burstRequestList = new ArrayList();
            this.gp3Callback = MorphoPanoramaGP3CameraState.nullGP3Callback;
        }
    }

    public static void initialize(int capture_mode) {
        cameraStartup = new CameraStartupInfo(capture_mode);
    }

    public CameraStartupInfo getCameraStartupInfo() {
        return cameraStartup;
    }

    public void setCamera2Params(Camera2ParamsFragment camera2Params) {
        cameraStartup.camera2Params = camera2Params;
    }

    public void setCameraInfo(CameraInfo cameraInfo) {
        cameraStartup.cameraInfo = cameraInfo;
    }

    public void setMorphoPanoramaGP3Interface(IMorphoPanoramaGP3Callback gp3Callback) {
        if (gp3Callback == null) {
            cameraStartup.gp3Callback = nullGP3Callback;
            return;
        }
        cameraStartup.gp3Callback = gp3Callback;
    }

    public void setPreviewSurface(Surface previewSurface) {
        cameraStartup.previewSurface = previewSurface;
    }

    public void setBackgroundHandlerThread(HandlerThread thread) {
        cameraStartup.backgroundHandlerThread = thread;
    }

    public void setCaptureCallback(CaptureCallback captureCallback) {
        cameraStartup.captureCallback = captureCallback;
    }

    public void setImageReader(ImageReader reader) {
        cameraStartup.imageReader = reader;
    }

    public void setImageReaderIdling(ImageReader reader) {
        cameraStartup.imageReaderIdling = reader;
    }

    public void setOnPreviewImageAvailableListener(OnImageAvailableListener listener) {
        cameraStartup.previewAvailableListener = listener;
    }

    public void setOnTakePictureImageAvailableListener(OnImageAvailableListener listener) {
        cameraStartup.takePictureAvailableListener = listener;
    }

    public void setBurstRemaining(int value) {
        cameraStartup.burstRemaining = value;
    }

    static double getSmoothenedEv(double x) {
        double[] dArr = mTvBuf;
        int i = mTvBufIndex;
        mTvBufIndex = i + 1;
        dArr[i & 7] = x;
        double y = Camera2ParamsFragment.TARGET_EV;
        for (int i2 = 3; i2 < 8; i2++) {
            y += ((double) i2) * mTvBuf[(mTvBufIndex + i2) & 7];
        }
        return 0.04d * y;
    }

    static void safeImageClose(Image image) {
        if (image != null) {
            image.close();
        }
    }

    public Camera2ParamsFragment camera2Params() {
        return cameraStartup.camera2Params;
    }

    public Handler backgroundHandler() {
        return cameraStartup.backgroundHandler;
    }

    static void setupFocusModeInfinity(Builder request) {
        request.set(CaptureRequest.CONTROL_MODE, Integer.valueOf(1));
        request.set(CaptureRequest.CONTROL_AF_MODE, Integer.valueOf(0));
        request.set(CaptureRequest.LENS_FOCUS_DISTANCE, Float.valueOf(0.0f));
    }
}
