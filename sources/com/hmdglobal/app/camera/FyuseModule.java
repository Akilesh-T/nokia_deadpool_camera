package com.hmdglobal.app.camera;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.RectF;
import android.view.KeyEvent;
import android.view.View;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.app.CameraAppUI.BottomBarUISpec;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.hardware.HardwareSpec;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.ui.TouchCoordinate;
import java.io.ByteArrayOutputStream;

public class FyuseModule extends CameraModule {
    public static final String FYUSE_MODULE_STRING_ID = "FyuseModule";
    private final Tag TAG = new Tag(FYUSE_MODULE_STRING_ID);
    private CameraActivity mActivity;
    private Integer mModeSelectionLockToken = null;
    private boolean mStartFyuse = true;

    static class CameraMode {
        public static final String MANUAL = "Manual";
        public static final String MICRO_VIDEO = "MicroVideo";
        public static final String PANO = "Pano";
        public static final String PHOTO = "Camera";
        public static final String SLO_MO = "SlowMo";
        public static final String SQUARE = "square";

        CameraMode() {
        }
    }

    static class FyuseRequest {
        public static final int FILE_LISTING = 1;
        public static final int START_CAMERA_MODE = 2;
        public static final int VIEWER = 0;

        FyuseRequest() {
        }
    }

    public FyuseModule(AppController app) {
        super(app);
    }

    public String getModuleStringIdentifier() {
        return FYUSE_MODULE_STRING_ID;
    }

    public int getModuleId() {
        return this.mActivity.getResources().getInteger(R.integer.camera_mode_parallax);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    public void onSingleTapUp(View view, int x, int y) {
    }

    public String getPeekAccessibilityString() {
        return null;
    }

    public void init(CameraActivity activity, boolean isSecureCamera, boolean isCaptureIntent) {
        this.mActivity = activity;
    }

    public void resume() {
        Tag tag = this.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Resume ");
        stringBuilder.append(this.mStartFyuse);
        Log.i(tag, stringBuilder.toString());
        if (this.mStartFyuse) {
            startFyuse(this.mActivity);
        }
    }

    public void pause() {
    }

    public boolean isPaused() {
        return false;
    }

    public void destroy() {
    }

    public boolean isCameraOpened() {
        return true;
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
        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 2 && this.mActivity != null) {
            if (this.mModeSelectionLockToken != null) {
                this.mActivity.unlockModuleSelection(this.mModeSelectionLockToken);
            }
            this.mActivity.setSecureFyuseModule(false);
            if (resultCode == -1) {
                this.mStartFyuse = false;
                if (data != null) {
                    String mode = data.getStringExtra("selectedMode");
                    if (mode == null || mode.equals("")) {
                        this.mActivity.onBackPressed();
                    } else {
                        this.mActivity.updateModeForFyusion(getIndexByName(mode));
                    }
                }
            }
        }
    }

    private int getIndexByName(String mode) {
        int index = this.mActivity.getResources().getInteger(R.integer.camera_mode_photo);
        if (mode == null) {
            return index;
        }
        if (mode.equals(CameraMode.MANUAL)) {
            return this.mActivity.getResources().getInteger(R.integer.camera_mode_manual);
        }
        if (mode.equals(CameraMode.SQUARE)) {
            return this.mActivity.getResources().getInteger(R.integer.camera_mode_square);
        }
        if (mode.equals(CameraMode.PANO)) {
            return this.mActivity.getResources().getInteger(R.integer.camera_mode_pano);
        }
        if (mode.equals(CameraMode.SLO_MO)) {
            return this.mActivity.getResources().getInteger(R.integer.camera_mode_slowmotion);
        }
        if (mode.equals(CameraMode.MICRO_VIDEO)) {
            return this.mActivity.getResources().getInteger(R.integer.camera_mode_micro_video);
        }
        return this.mActivity.getResources().getInteger(R.integer.camera_mode_photo);
    }

    public boolean startFyuse(Context context) {
        try {
            Log.e(this.TAG, "KPI startFyuse e");
            if (this.mModeSelectionLockToken == null) {
                this.mModeSelectionLockToken = this.mActivity.lockModuleSelection();
            }
            boolean isSecureCamera = this.mActivity.isSecureCamera();
            if (isSecureCamera) {
                this.mActivity.setSecureFyuseModule(true);
            }
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(FyuseAPI.FYUSE_PACKAGE_NAME, "com.fyusion.fyuse.Camera.CameraActivity"));
            intent.putExtra("secure_camera", isSecureCamera);
            intent.putExtra(Keys.KEY_CAMERA_ID, this.mActivity.getCurrentCameraId());
            intent.putExtra(Keys.KEY_THUMB_URI, this.mActivity.getPeekThumbUri());
            Bitmap coveredBitmap = this.mActivity.getCameraAppUI().getCoveredBitmap();
            if (coveredBitmap != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                coveredBitmap.compress(CompressFormat.JPEG, 60, baos);
                intent.putExtra(Keys.KEY_BLURRED_BITMAP_BYTE, baos.toByteArray());
                RectF previewArea = this.mActivity.getCameraAppUI().getCoveredArea();
                if (previewArea.top > 0.0f || previewArea.bottom > 0.0f) {
                    intent.putExtra(Keys.KEY_PREVIEW_AREA, new float[]{previewArea.top, previewArea.bottom});
                }
            }
            intent.addCategory("android.intent.category.LAUNCHER");
            this.mActivity.startActivityForResult(intent, 2);
            this.mActivity.overridePendingTransition(0, 0);
            Log.e(this.TAG, "KPI startFyuse x");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(this.TAG, "No Fyuse installed");
            if (this.mModeSelectionLockToken != null) {
                this.mActivity.unlockModuleSelection(this.mModeSelectionLockToken);
            }
            this.mStartFyuse = false;
            this.mActivity.setSecureFyuseModule(false);
            this.mActivity.switchToMode(this.mActivity.getResources().getInteger(R.integer.camera_mode_photo));
            return false;
        }
    }

    public void onShutterButtonFocus(boolean pressed) {
    }

    public void onShutterCoordinate(TouchCoordinate coord) {
    }

    public void onShutterButtonClick() {
    }

    public void onShutterButtonLongClick() {
    }
}
