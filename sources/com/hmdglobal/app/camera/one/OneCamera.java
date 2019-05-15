package com.hmdglobal.app.camera.one;

import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.view.Surface;
import com.hmdglobal.app.camera.session.CaptureSession;
import com.hmdglobal.app.camera.util.Size;
import java.io.File;

public interface OneCamera {

    public enum AutoFocusMode {
        CONTINUOUS_PICTURE,
        AUTO
    }

    public enum AutoFocusState {
        INACTIVE,
        ACTIVE_SCAN,
        ACTIVE_FOCUSED,
        ACTIVE_UNFOCUSED,
        PASSIVE_SCAN,
        PASSIVE_FOCUSED,
        PASSIVE_UNFOCUSED
    }

    public interface CameraErrorListener {
        void onCameraError();
    }

    public interface CaptureReadyCallback {
        void onReadyForCapture();

        void onSetupFailed();
    }

    public interface CloseCallback {
        void onCameraClosed();
    }

    public enum Facing {
        FRONT,
        BACK
    }

    public interface FocusStateListener {
        void onFocusStatusUpdate(AutoFocusState autoFocusState, long j);
    }

    public interface OpenCallback {
        void onCameraClosed();

        void onCameraOpened(OneCamera oneCamera);

        void onFailure();
    }

    public static final class PhotoCaptureParameters {
        public PictureCallback callback = null;
        public File debugDataFolder;
        public Flash flashMode = Flash.AUTO;
        public int heading = Integer.MIN_VALUE;
        public Location location = null;
        public int orientation = Integer.MIN_VALUE;
        public Float timerSeconds = null;
        public String title = null;
        public float zoom = 1.0f;

        public enum Flash {
            AUTO,
            OFF,
            ON
        }

        public void checkSanity() {
            checkRequired(this.title);
            checkRequired(this.callback);
            checkRequired(this.orientation);
            checkRequired(this.heading);
        }

        private void checkRequired(int num) {
            if (num == Integer.MIN_VALUE) {
                throw new RuntimeException("Photo capture parameter missing.");
            }
        }

        private void checkRequired(Object obj) {
            if (obj == null) {
                throw new RuntimeException("Photo capture parameter missing.");
            }
        }
    }

    public interface PictureCallback {
        void onPictureSaved(Uri uri);

        void onPictureTaken(CaptureSession captureSession);

        void onPictureTakenFailed();

        void onQuickExpose();

        void onTakePictureProgress(float f);

        void onThumbnailResult(byte[] bArr);
    }

    public interface ReadyStateChangedListener {
        void onReadyStateChanged(boolean z);
    }

    void close(CloseCallback closeCallback);

    float getFullSizeAspectRatio();

    float getMaxZoom();

    Size[] getSupportedSizes();

    boolean isBackFacing();

    boolean isFlashSupported(boolean z);

    boolean isFrontFacing();

    boolean isSupportingEnhancedMode();

    Size pickPreviewSize(Size size, Context context);

    void setCameraErrorListener(CameraErrorListener cameraErrorListener);

    void setFocusStateListener(FocusStateListener focusStateListener);

    void setReadyStateChangedListener(ReadyStateChangedListener readyStateChangedListener);

    void setViewfinderSize(int i, int i2);

    void setZoom(float f);

    void startPreview(Surface surface, CaptureReadyCallback captureReadyCallback);

    void takePicture(PhotoCaptureParameters photoCaptureParameters, CaptureSession captureSession);

    void triggerFocusAndMeterAtPoint(float f, float f2);
}
