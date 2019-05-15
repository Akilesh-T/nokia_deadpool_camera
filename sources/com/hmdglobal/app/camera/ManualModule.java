package com.hmdglobal.app.camera;

import android.util.Log;
import android.view.View;
import com.android.ex.camera2.portability.CameraCapabilities.FocusMode;
import com.android.ex.camera2.portability.CameraCapabilities.Stringifier;
import com.android.ex.camera2.portability.CameraCapabilities.WhiteBalance;
import com.hmdglobal.app.camera.ManualUI.ManualModeCallBackListener;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.app.CameraAppUI.BottomBarUISpec;
import com.hmdglobal.app.camera.beauty.util.SharedUtil;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.util.CustomFields;
import com.hmdglobal.app.camera.util.CustomUtil;
import com.hmdglobal.app.camera.util.ToastUtil;

public class ManualModule extends PhotoModule {
    public static final String MANUAL_MODULE_STRING_ID = "ManualModule";
    private static final String TAG = "ManualModule";
    private ManualUI mManualModuleUI;

    private class CameraManualModeCallBackListener implements ManualModeCallBackListener {
        private CameraManualModeCallBackListener() {
        }

        public void updateISOValue(boolean auto, int isoValue) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[ManualModule] updateISOValue  auto = ");
            stringBuilder.append(auto);
            stringBuilder.append("  isoValue = ");
            stringBuilder.append(isoValue);
            Log.d("ManualModule", stringBuilder.toString());
            if (!ManualModule.this.mPaused && ManualModule.this.mCameraSettings != null && ManualModule.this.mCameraDevice != null) {
                if (auto) {
                    ManualModule.this.mCameraSettings.setISOValue("auto");
                } else {
                    ManualModule.this.clearFocusWithoutChangingState();
                    ManualModule.this.setCameraState(1);
                    ManualModule.this.mCameraSettings.setISOValue("manual");
                    ManualModule.this.mCameraSettings.setContinuousIso(isoValue);
                }
                if (ManualModule.this.mCameraDevice != null) {
                    ManualModule.this.mCameraDevice.applySettings(ManualModule.this.mCameraSettings);
                }
            }
        }

        public void updateManualFocusValue(boolean auto, int focusPos) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[ManualModule] updateManualFocusValue  auto = ");
            stringBuilder.append(auto);
            stringBuilder.append("  focusPos = ");
            stringBuilder.append(focusPos);
            Log.d("ManualModule", stringBuilder.toString());
            if (!ManualModule.this.mPaused && ManualModule.this.mCameraSettings != null && ManualModule.this.mCameraDevice != null) {
                if (auto) {
                    ManualModule.this.mCameraSettings.setFocusMode(FocusMode.CONTINUOUS_PICTURE);
                } else {
                    ManualModule.this.setCameraState(1);
                    ManualModule.this.mCameraSettings.setFocusMode(FocusMode.MANUAL);
                    ManualModule.this.mCameraSettings.setManualFocusPosition(10.0f - (((float) focusPos) * 0.1f));
                }
                if (ManualModule.this.mCameraDevice != null) {
                    ManualModule.this.mCameraDevice.applySettings(ManualModule.this.mCameraSettings);
                }
            }
        }

        public void updateWBValue(boolean auto, String wbValue) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[ManualModule] updateWBValue  auto = ");
            stringBuilder.append(auto);
            stringBuilder.append("  wbValue = ");
            stringBuilder.append(wbValue);
            Log.d("ManualModule", stringBuilder.toString());
            if (!ManualModule.this.mPaused && ManualModule.this.mCameraSettings != null && ManualModule.this.mCameraDevice != null) {
                Stringifier stringifier = ManualModule.this.mCameraCapabilities.getStringifier();
                if (auto) {
                    ManualModule.this.mCameraSettings.setWhiteBalance(WhiteBalance.AUTO);
                } else {
                    ManualModule.this.mCameraSettings.setWhiteBalance(stringifier.whiteBalanceFromString(wbValue));
                }
                if (ManualModule.this.mCameraDevice != null) {
                    ManualModule.this.mCameraDevice.applySettings(ManualModule.this.mCameraSettings);
                }
            }
        }

        public void updateExposureTime(boolean auto, String etValue) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[ManualModule] updateExposureTime  auto = ");
            stringBuilder.append(auto);
            stringBuilder.append("  etValue = ");
            stringBuilder.append(etValue);
            Log.d("ManualModule", stringBuilder.toString());
            if (!ManualModule.this.mPaused && ManualModule.this.mCameraSettings != null && ManualModule.this.mCameraDevice != null) {
                if (auto) {
                    ManualModule.this.setManualEtValue("0");
                    if (SharedUtil.getIntValueByKey("currentBatteryStatus").intValue() == 0) {
                        ManualModule.this.mAppController.getSettingsManager().set(ManualModule.this.mActivity.getCameraScope(), Keys.KEY_PRO_CURRENT_ET_AUTO_STATES, true);
                        ManualModule.this.mAppController.getButtonManager().enableButton(0);
                    }
                } else {
                    ManualModule.this.clearFocusWithoutChangingState();
                    ManualModule.this.setCameraState(1);
                    ManualModule.this.setManualEtValue(etValue);
                    ManualModule.this.mAppController.getSettingsManager().set(ManualModule.this.mActivity.getCameraScope(), Keys.KEY_PRO_CURRENT_ET_AUTO_STATES, false);
                    ManualModule.this.mAppController.getButtonManager().hideButton(0);
                }
            }
        }

        public void updateExposureCompensation(boolean auto, String ecValue) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[ManualModule] updateExposureCompensation  auto = ");
            stringBuilder.append(auto);
            stringBuilder.append("  ecValue = ");
            stringBuilder.append(ecValue);
            Log.d("ManualModule", stringBuilder.toString());
            if (!ManualModule.this.mPaused && ManualModule.this.mCameraSettings != null && ManualModule.this.mCameraDevice != null) {
                ManualModule.this.clearFocusWithoutChangingState();
                ManualModule.this.setCameraState(1);
                ManualModule.this.mCameraSettings.setExposureCompensationIndex(Integer.valueOf(ecValue).intValue());
                if (ManualModule.this.mCameraDevice != null) {
                    ManualModule.this.mCameraDevice.applySettings(ManualModule.this.mCameraSettings);
                }
            }
        }
    }

    public ManualModule(AppController app) {
        super(app);
    }

    /* Access modifiers changed, original: protected */
    public PhotoUI getPhotoUI() {
        if (this.mManualModuleUI == null) {
            this.mManualModuleUI = new ManualUI(this.mActivity, this, this.mActivity.getModuleLayoutRoot(), new CameraManualModeCallBackListener());
        }
        return this.mManualModuleUI;
    }

    public String getModuleStringIdentifier() {
        return "ManualModule";
    }

    public int getModuleId() {
        return this.mAppController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_pro);
    }

    public void resume() {
        super.resume();
        if (CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_HELP_TIP_TUTORIAL, false) && this.mManualModuleUI != null) {
            this.mManualModuleUI.initManualUIForTutorial();
        }
        this.mAppController.getButtonManager().setEffectWrapperVisible(8);
    }

    public boolean onGLRenderEnable() {
        return false;
    }

    public boolean isSupportEffects() {
        return false;
    }

    public boolean isSupportBeauty() {
        return false;
    }

    public BottomBarUISpec getBottomBarSpec() {
        BottomBarUISpec spec = super.getBottomBarSpec();
        spec.showBeautyButton = false;
        spec.showEffectButton = false;
        spec.hideBolken = true;
        spec.hideCamera = false;
        spec.hideFlash = false;
        spec.showMotion = false;
        spec.hideHdr = true;
        spec.enableCamera = true;
        spec.showSelfTimer = true;
        spec.enableSelfTimer = true;
        spec.showBeauty2 = false;
        spec.showEffect2 = false;
        return spec;
    }

    public void pause() {
        this.mAppController.getCameraAppUI().setNeedShowArc(false);
        this.mAppController.getCameraAppUI().removeShowSeekBarMsg();
        resetManualModeParamters();
        super.pause();
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

    public void onShutterButtonLongClick() {
        if ("0".equals(getManualEtValue())) {
            super.onShutterButtonLongClick();
        } else {
            ToastUtil.showToast(this.mActivity, this.mActivity.getString(R.string.burst_shot_in_pro_mode_toast), 1);
        }
    }

    public void resetManualModeParamters() {
        this.mAppController.getSettingsManager().set(this.mActivity.getCameraScope(), Keys.KEY_PRO_CURRENT_ET_AUTO_STATES, true);
        if (this.mCameraSettings != null) {
            this.mCameraSettings.setISOValue("auto");
            this.mCameraSettings.setFocusMode(FocusMode.CONTINUOUS_PICTURE);
            this.mCameraSettings.setWhiteBalance(WhiteBalance.AUTO);
            this.mCameraSettings.setExposureTime("0");
            if (this.mCameraDevice != null) {
                this.mCameraDevice.applySettings(this.mCameraSettings);
            }
        }
    }

    public boolean isZslOn() {
        return true;
    }

    public void hardResetSettings(SettingsManager settingsManager) {
        super.hardResetSettings(settingsManager);
    }

    public void onLongPress(float x, float y) {
    }

    /* Access modifiers changed, original: protected */
    public boolean hideCamera() {
        return true;
    }

    /* Access modifiers changed, original: protected */
    public boolean isMotionShow() {
        return false;
    }

    /* Access modifiers changed, original: protected */
    public boolean isHdrShow() {
        return false;
    }

    /* Access modifiers changed, original: protected */
    public boolean isDepthEnabled() {
        return false;
    }

    /* Access modifiers changed, original: protected */
    public boolean isWrapperButtonShow() {
        return false;
    }

    /* Access modifiers changed, original: protected */
    public boolean needEnableExposureAdjustment() {
        boolean z = true;
        if (CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_ENABLE_COMPENSATION_OTHER_THAN_AUTO, true)) {
            return true;
        }
        if (this.mManualModuleUI.isManualMode(Keys.KEY_MANUAL_ISO_STATE) || this.mManualModuleUI.isManualMode(Keys.KEY_CUR_EXPOSURE_TIME_STATE)) {
            z = false;
        }
        return z;
    }

    public void onSingleTapUp(View view, int x, int y) {
        ((ManualUI) getPhotoUI()).collapseManualMenu();
        if (!this.mManualModuleUI.isManualMode(Keys.KEY_CUR_FOCUS_STATE)) {
            super.onSingleTapUp(view, x, y);
        }
    }

    /* Access modifiers changed, original: protected */
    public void setCameraState(int state) {
        super.setCameraState(state);
        if (state == 1) {
            getPhotoUI().enableZoom();
        }
    }

    /* Access modifiers changed, original: protected */
    public void updateParametersFocusMode() {
        if (!this.mManualModuleUI.isManualMode(Keys.KEY_CUR_FOCUS_STATE)) {
            super.updateParametersFocusMode();
        }
    }
}
