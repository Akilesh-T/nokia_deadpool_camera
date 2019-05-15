package com.hmdglobal.app.camera;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Size;
import android.view.KeyEvent;
import android.view.View;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.app.CameraAppUI.BottomBarUISpec;
import com.hmdglobal.app.camera.app.CameraController;
import com.hmdglobal.app.camera.app.CameraProvider;
import com.hmdglobal.app.camera.app.LocationManager;
import com.hmdglobal.app.camera.exif.ExifInterface;
import com.hmdglobal.app.camera.hardware.HardwareSpec;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.ui.TouchCoordinate;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.morphoinc.app.LogFilter;
import com.morphoinc.app.panoramagp3.Camera2App;

public class PanoCaptureModule extends CameraModule implements PhotoController {
    public static final boolean DEBUG = true;
    private static final int MSG_AUTO_EXIT = 0;
    private static final int SCREEN_DELAY_TIME_MS = 120000;
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAITING_LOCK = 1;
    private static final String TAG = "PanoCaptureModule";
    public static boolean mPanoPreviewIsOk = false;
    protected CameraActivity mActivity;
    protected AppController mAppController;
    private CaptureCallback mCaptureCallback = new CaptureCallback() {
        private void process(CaptureResult result) {
            switch (PanoCaptureModule.this.mState) {
                case 1:
                    Integer afState = (Integer) result.get(CaptureResult.CONTROL_AF_STATE);
                    Integer aeState = (Integer) result.get(CaptureResult.CONTROL_AE_STATE);
                    String str = PanoCaptureModule.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("STATE_WAITING_LOCK afState:");
                    stringBuilder.append(afState);
                    stringBuilder.append(" aeState:");
                    stringBuilder.append(aeState);
                    Log.d(str, stringBuilder.toString());
                    if (4 == afState.intValue() || 5 == afState.intValue()) {
                        PanoCaptureModule.this.mState = 0;
                        return;
                    }
                    return;
                default:
                    return;
            }
        }

        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
        }

        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            CameraUtil.setPreviewIsOk();
            process(result);
        }
    };
    private final Handler mHandler = new MainHandler();
    private LocationManager mLocationManager;
    private Camera2App mMorphoApp;
    private Runnable mOnCloseCameraCallback;
    private int mState = 0;

    private class MainHandler extends Handler {
        public MainHandler() {
            super(Looper.getMainLooper());
        }

        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                PanoCaptureModule.this.mActivity.finish();
            }
        }
    }

    public void initOnThread(CameraActivity activity) {
        initInternal(activity);
    }

    public void initOnMainThread(View parent) {
        this.mMorphoApp.onCreate(parent);
    }

    public PanoCaptureModule(AppController app) {
        super(app);
    }

    public int getModuleId() {
        return this.mActivity.getResources().getInteger(R.integer.camera_mode_pano);
    }

    public boolean isImageCaptureIntent() {
        return false;
    }

    public boolean canCloseCamera() {
        return false;
    }

    public boolean isCameraOpened() {
        return true;
    }

    public void onCaptureDone() {
    }

    public void onCaptureCancelled() {
    }

    public void onCaptureRetake() {
    }

    public boolean cancelAutoFocus() {
        return false;
    }

    public void stopPreview() {
    }

    public int getCameraState() {
        return 0;
    }

    public void onSingleTapUp(View view, int x, int y) {
    }

    public void onLongPress(float x, float y) {
    }

    public void updatePreviewAspectRatio(float aspectRatio) {
    }

    public void updateCameraOrientation() {
    }

    public void onPreviewUIReady() {
    }

    public void onPreviewUIDestroyed() {
    }

    public void startPreCaptureAnimation() {
    }

    public void onEvoChanged(int value) {
    }

    public boolean isFacebeautyEnabled() {
        return false;
    }

    public boolean isAttentionSeekerShow() {
        return false;
    }

    public void updateFaceBeautySetting(String key, int value) {
    }

    public boolean isGesturePalmShow() {
        return false;
    }

    public String getPeekAccessibilityString() {
        return null;
    }

    public String getModuleStringIdentifier() {
        return null;
    }

    public void init(CameraActivity activity, boolean isSecureCamera, boolean isCaptureIntent) {
        initInternal(activity);
        this.mMorphoApp.onCreate(this.mActivity.getModuleLayoutRoot());
    }

    private void initInternal(CameraActivity activity) {
        this.mMorphoApp = new Camera2App(activity, this);
        this.mActivity = activity;
        this.mLocationManager = new LocationManager(this.mActivity);
    }

    public void changePanoStatus(boolean newStatus, boolean isCancelling) {
    }

    public boolean isPanoActive() {
        return this.mMorphoApp.isEngineRunning();
    }

    public Size getPictureOutputSize() {
        return this.mMorphoApp.getPreviewSize();
    }

    private void resetScreenOn() {
        this.mHandler.removeMessages(0);
        this.mActivity.getWindow().clearFlags(128);
    }

    private void keepScreenOnAwhile() {
        this.mHandler.removeMessages(0);
        this.mActivity.getWindow().addFlags(128);
        this.mHandler.sendEmptyMessageDelayed(0, 120000);
    }

    private void keepScreenOn() {
        this.mHandler.removeMessages(0);
        this.mActivity.getWindow().addFlags(128);
    }

    public void onPauseSuper(Runnable callback) {
        LogFilter.d(TAG, "onPauseSuper");
        this.mMorphoApp.onPause();
        this.mOnCloseCameraCallback = callback;
        mPanoPreviewIsOk = false;
        doAfterCloseCamera();
        doRecordingLocation(false);
    }

    private void doAfterCloseCamera() {
        this.mActivity.runOnUiThread(new -$$Lambda$PanoCaptureModule$dWVwAx4Y7z3otpumBZOPL8z6LlM(this));
    }

    public static /* synthetic */ void lambda$doAfterCloseCamera$0(PanoCaptureModule panoCaptureModule) {
        if (panoCaptureModule.mOnCloseCameraCallback != null) {
            panoCaptureModule.mOnCloseCameraCallback.run();
            panoCaptureModule.mOnCloseCameraCallback = null;
        }
    }

    private void updateSaveStorageState() {
    }

    public boolean getRecordLocation() {
        return false;
    }

    public Location getLocation() {
        return this.mLocationManager.getCurrentLocation();
    }

    private static void writeLocation(Location location, ExifInterface exif) {
        if (location != null) {
            exif.addGpsTags(location.getLatitude(), location.getLongitude());
            exif.setTag(exif.buildTag(ExifInterface.TAG_GPS_PROCESSING_METHOD, location.getProvider()));
        }
    }

    public void initRecordLocation() {
    }

    public boolean onBackPressed() {
        if (!isPanoActive()) {
            return false;
        }
        changePanoStatus(false, false);
        return true;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return this.mMorphoApp.onKeyDown(keyCode, event);
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!(keyCode == 80 || keyCode == CameraUtil.BOOM_KEY)) {
            switch (keyCode) {
                case 24:
                case 25:
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    public void resume() {
        doRecordingLocation(true);
        updateSaveStorageState();
        this.mMorphoApp.onResume();
        CameraProvider cp = this.mActivity.getCameraProvider();
        if (!(cp instanceof CameraController)) {
            return;
        }
        if (cp.getCurrentCameraId() == cp.getFirstBackCameraId() || cp.getCurrentCameraId() == 3 || cp.getCurrentCameraId() == 1) {
            ((CameraController) cp).closeCamera(true);
        }
    }

    public void pause() {
        mPanoPreviewIsOk = false;
        this.mMorphoApp.onPause();
        resetScreenOn();
        doAfterCloseCamera();
        doRecordingLocation(false);
    }

    public boolean isPaused() {
        return false;
    }

    public void destroy() {
    }

    public void onLayoutOrientationChanged(boolean isLandscape) {
    }

    public void onOrientationChanged(int orientation) {
    }

    public void onCameraAvailable(CameraProxy cameraProxy) {
    }

    public void hardResetSettings(SettingsManager settingsManager) {
    }

    public HardwareSpec getHardwareSpec() {
        return null;
    }

    public BottomBarUISpec getBottomBarSpec() {
        return null;
    }

    public boolean isUsingBottomBar() {
        return false;
    }

    public void onShutterButtonFocus(boolean pressed) {
    }

    public void onShutterCoordinate(TouchCoordinate coord) {
    }

    public void onShutterButtonLongClick() {
    }

    public void onZoomChanged(float requestedZoom) {
    }

    public boolean isCameraIdle() {
        return false;
    }

    private void doRecordingLocation(boolean enable) {
        if (getRecordLocation()) {
            this.mLocationManager.recordLocation(enable);
        }
    }

    public void onShutterButtonClick() {
        this.mMorphoApp.onClickShutter(null);
    }

    private void lockFocus() {
    }

    public void unlockFocus() {
    }

    public Camera2App getMorphoApp() {
        return this.mMorphoApp;
    }
}
