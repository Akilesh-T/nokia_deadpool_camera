package com.hmdglobal.app.camera;

import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.app.CameraAppUI.BottomBarUISpec;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;

public class TimeLapsedModule extends VideoModule {
    public static final String TIME_SLPASE_VIDEO_MODULE_STRING_ID = "VideoModule";

    public TimeLapsedModule(AppController app) {
        super(app);
    }

    public boolean getTimeLapsedEnable() {
        return true;
    }

    public int getModuleId() {
        return this.mAppController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_time_lapse);
    }

    public void resume() {
        setTimeLapsed(true);
        super.resume();
        this.mActivity.getButtonManager().setMoreEnterToggleButton(8);
        this.mActivity.getButtonManager().setEffectsEnterToggleButton(8);
        this.mActivity.getCameraAppUI().hideFilmstrip();
        this.mActivity.getCameraAppUI().pauseFaceDetection();
        this.mActivity.getButtonManager().hidePhotoModuleRelatedButtons();
        this.mActivity.getButtonManager().hidePhotoModuleRelatedButtons();
    }

    public BottomBarUISpec getBottomBarSpec() {
        BottomBarUISpec spec = super.getBottomBarSpec();
        spec.showEffectButton = false;
        spec.showBeautyButton = false;
        spec.hideLive = true;
        spec.hideSetting = true;
        spec.hideGridLines = true;
        spec.moreName = this.mAppController.getAndroidContext().getResources().getString(R.string.mode_timelapse);
        spec.showEffect2 = false;
        spec.moduleName = TimeLapsedModule.class.getSimpleName();
        return spec;
    }

    public boolean isSupportBeauty() {
        return false;
    }

    public boolean isSupportEffects() {
        return false;
    }

    public boolean onBackPressed() {
        this.mAppController.getCameraAppUI().onModeIdChanged(this.mActivity.getSettingsManager().getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_SQUARE_RETURN_TO_INDEX).intValue());
        return true;
    }

    public void pause() {
        setTimeLapsed(false);
        super.pause();
    }

    public boolean canCloseCamera() {
        return true;
    }
}
