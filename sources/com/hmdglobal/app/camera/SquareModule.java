package com.hmdglobal.app.camera;

import com.android.ex.camera2.portability.CameraCapabilities.FocusMode;
import com.android.ex.camera2.portability.CameraCapabilities.Stringifier;
import com.android.ex.camera2.portability.Size;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.app.CameraAppUI.BottomBarUISpec;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.settings.CameraPictureSizesCacher;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.settings.SettingsUtil;
import com.hmdglobal.app.camera.util.CustomUtil;
import java.util.List;

public class SquareModule extends PhotoModule {
    public static final String SQUARE_MODULE_STRING_ID = "SquareModule";
    private static final Tag TAG = new Tag(SQUARE_MODULE_STRING_ID);
    private AppController app;
    private final float mSuperZoomThreshold = 1.5f;
    private float mZoomValue;

    public SquareModule(AppController app) {
        super(app);
        this.app = app;
    }

    public void init(CameraActivity activity, boolean isSecureCamera, boolean isCaptureIntent) {
        super.init(activity, isSecureCamera, isCaptureIntent);
    }

    public void resume() {
        this.mActivity.getCameraAppUI().freezeGlSurface();
        super.resume();
    }

    /* Access modifiers changed, original: protected */
    public void updateParametersPictureSize() {
        if (this.mCameraDevice == null) {
            Log.w(TAG, "attempting to set picture size without camera device");
            return;
        }
        Size picSize;
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        String str;
        if (isCameraFrontFacing()) {
            str = Keys.KEY_PICTURE_SIZE_FRONT;
        } else {
            str = Keys.KEY_PICTURE_SIZE_BACK;
        }
        List<Size> sizes = this.mCameraCapabilities.getSupportedPreviewSizes();
        Size previewSize = new Size(1200, 1200);
        if (!isCameraFrontFacing()) {
            picSize = new Size(3120, 3120);
        } else if (CustomUtil.getInstance().isPanther()) {
            picSize = new Size(2448, 2448);
        } else {
            picSize = new Size(1944, 1944);
        }
        List<Size> sizePics = this.mCameraCapabilities.getSupportedPhotoSizes();
        this.mActivity.getCameraAppUI().setSurfaceHeight(previewSize.height());
        this.mActivity.getCameraAppUI().setSurfaceWidth(previewSize.width());
        this.mUI.setCaptureSize(previewSize);
        CameraPictureSizesCacher.updateSizesForCamera(this.mAppController.getAndroidContext(), this.mCameraDevice.getCameraId(), sizePics);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(picSize.width());
        stringBuilder.append("x");
        stringBuilder.append(picSize.height());
        SettingsUtil.setCameraPictureSize(stringBuilder.toString(), sizePics, this.mCameraSettings, this.mCameraDevice.getCameraId());
        this.mCameraSettings.setPhotoSize(picSize);
        this.mCameraSettings.setPreviewSize(previewSize);
        this.mCameraDevice.applySettings(this.mCameraSettings);
        this.mCameraSettings = this.mCameraDevice.getSettings();
        if (!(previewSize.width() == 0 || previewSize.height() == 0)) {
            Log.v(TAG, "updating aspect ratio");
            this.mUI.updatePreviewAspectRatio(((float) previewSize.width()) / ((float) previewSize.height()));
        }
    }

    public void updatePreviewAspectRatio(float aspectRatio) {
        this.mAppController.updatePreviewAspectRatio(aspectRatio);
    }

    /* Access modifiers changed, original: protected */
    public void stopBurst() {
        resetHdrState();
        super.stopBurst();
    }

    public BottomBarUISpec getBottomBarSpec() {
        BottomBarUISpec bottomBarSpec = super.getBottomBarSpec();
        bottomBarSpec.hideSetting = true;
        boolean isCountDownShow = isCountDownShow();
        bottomBarSpec.enableSelfTimer = isCountDownShow;
        bottomBarSpec.showSelfTimer = isCountDownShow;
        bottomBarSpec.moreName = this.mAppController.getAndroidContext().getResources().getString(R.string.mode_square);
        return bottomBarSpec;
    }

    public int getModuleId() {
        return this.mAppController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_square);
    }

    public String getModuleStringIdentifier() {
        return SQUARE_MODULE_STRING_ID;
    }

    /* Access modifiers changed, original: protected */
    public boolean isLowLightShow() {
        return isCameraFrontFacing() ^ 1;
    }

    /* Access modifiers changed, original: protected */
    public boolean isDepthEnabled() {
        return false;
    }

    /* Access modifiers changed, original: protected */
    public boolean isEnableGestureRecognization() {
        boolean isGestureDetectionOn = Keys.isGestureDetectionOn(this.mActivity.getSettingsManager());
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isEnableGestureRecognization");
        stringBuilder.append(isGestureDetectionOn);
        Log.d(tag, stringBuilder.toString());
        return isGestureDetectionOn && isCameraFrontFacing();
    }

    /* Access modifiers changed, original: protected */
    public boolean isSuperResolutionEnabled() {
        return true;
    }

    /* Access modifiers changed, original: protected */
    public boolean isVisidonModeEnabled() {
        return true;
    }

    public boolean isFacebeautyEnabled() {
        return false;
    }

    public boolean isAttentionSeekerShow() {
        return Keys.isAttentionseekerOn(this.mActivity.getSettingsManager()) && !isCameraFrontFacing();
    }

    public boolean isGesturePalmShow() {
        return isEnableGestureRecognization() && "android.intent.action.MAIN".equals(this.mActivity.getIntent().getAction());
    }

    public void pause() {
        super.pause();
        Log.w(TAG, "on Pause");
    }

    /* Access modifiers changed, original: protected */
    public boolean isNeedMirrorSelfie() {
        return Keys.isMirrorSelfieOn(this.mAppController.getSettingsManager());
    }

    /* Access modifiers changed, original: protected */
    public void updateFrontPhotoflipMode() {
        if (isCameraFrontFacing()) {
            this.mCameraSettings.setMirrorSelfieOn(Keys.isMirrorSelfieOn(this.mAppController.getSettingsManager()));
        }
    }

    /* Access modifiers changed, original: protected */
    public boolean isVolumeKeySystemBehaving() {
        if (!getPhotoUI().isSoundGroupPlaying()) {
            return false;
        }
        Log.w(TAG, "process volume key as system service");
        return true;
    }

    /* Access modifiers changed, original: protected */
    public void setCameraState(int state) {
        super.setCameraState(state);
        if (state == 1) {
            getPhotoUI().enableZoom();
            HelpTipsManager helpTipsManager = this.mAppController.getHelpTipsManager();
            if (helpTipsManager != null && helpTipsManager.isHelpTipShowExist()) {
                helpTipsManager.setVideoReadlyFlags();
            }
        }
    }

    /* Access modifiers changed, original: protected */
    public void transitionToTimer(boolean isShow) {
        if (isShow) {
            getPhotoUI().showFacebeauty();
            getPhotoUI().showGesturePalm();
            getPhotoUI().showSoundGroup();
            return;
        }
        getPhotoUI().hideFacebeauty();
        getPhotoUI().hideGesturePalm();
        getPhotoUI().hideSoundGroup();
    }

    /* Access modifiers changed, original: protected */
    public void initializeFocusModeSettings() {
        Stringifier stringifier = this.mCameraCapabilities.getStringifier();
        SettingsManager settingsManager = this.mAppController.getSettingsManager();
        if (this.mCameraCapabilities.supports(FocusMode.CONTINUOUS_PICTURE)) {
            settingsManager.set(this.mAppController.getCameraScope(), Keys.KEY_FOCUS_MODE, stringifier.stringify(FocusMode.CONTINUOUS_PICTURE));
        } else {
            Log.e(TAG, "Continous Picture Focus not supported");
        }
    }

    public boolean onBackPressed() {
        this.mActivity.getCameraAppUI().onModeIdChanged(this.mActivity.getSettingsManager().getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_SQUARE_RETURN_TO_INDEX).intValue());
        return true;
    }
}
