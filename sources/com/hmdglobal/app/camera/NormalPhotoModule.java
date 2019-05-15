package com.hmdglobal.app.camera;

import com.android.ex.camera2.portability.CameraCapabilities.FocusMode;
import com.android.ex.camera2.portability.CameraCapabilities.Stringifier;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.util.CustomFields;
import com.hmdglobal.app.camera.util.CustomUtil;

public class NormalPhotoModule extends PhotoModule {
    public static final String AUTO_MODULE_STRING_ID = "AutoModule";
    private static final Tag TAG = new Tag(AUTO_MODULE_STRING_ID);

    public NormalPhotoModule(AppController app) {
        super(app);
    }

    public String getModuleStringIdentifier() {
        return AUTO_MODULE_STRING_ID;
    }

    public int getModuleId() {
        return this.mAppController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_photo);
    }

    /* Access modifiers changed, original: protected */
    public boolean isLowLightShow() {
        return isCameraFrontFacing() ^ 1;
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
        boolean z = true;
        if (!(CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_PHOTO_FACEBEAUTY_SUPPORT, true) && Keys.isFacebeautyOn(this.mActivity.getSettingsManager()) && isCameraFrontFacing())) {
            z = false;
        }
        return z;
    }

    public boolean isAttentionSeekerShow() {
        return Keys.isAttentionseekerOn(this.mActivity.getSettingsManager()) && !isCameraFrontFacing();
    }

    public boolean isGesturePalmShow() {
        return isEnableGestureRecognization() && "android.intent.action.MAIN".equals(this.mActivity.getIntent().getAction());
    }

    public void resume() {
        super.resume();
        Log.w(TAG, "on Resume");
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
}
