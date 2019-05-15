package com.hmdglobal.app.camera.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import com.hmdglobal.app.camera.ButtonManager;
import com.hmdglobal.app.camera.ButtonManager.ButtonStatusListener;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.settings.SettingsManager.OnSettingChangedListener;
import com.hmdglobal.app.camera.util.PhotoSphereHelper;

public class IndicatorIconController implements OnSettingChangedListener, ButtonStatusListener {
    private static final Tag TAG = new Tag("IndicatorIconCtrlr");
    private AppController mController;
    private ImageView mCountdownTimerIndicator;
    private TypedArray mCountdownTimerIndicatorIcons;
    private ImageView mExposureIndicatorN1;
    private ImageView mExposureIndicatorN2;
    private ImageView mExposureIndicatorP1;
    private ImageView mExposureIndicatorP2;
    private ImageView mFlashIndicator;
    private TypedArray mFlashIndicatorPhotoIcons;
    private TypedArray mFlashIndicatorVideoIcons;
    private ImageView mHdrIndicator;
    private TypedArray mHdrIndicatorIcons;
    private TypedArray mHdrPlusIndicatorIcons;
    private ImageView mPanoIndicator;
    private TypedArray mPanoIndicatorIcons;

    public IndicatorIconController(AppController controller, View root) {
        this.mController = controller;
        Context context = controller.getAndroidContext();
        this.mFlashIndicator = (ImageView) root.findViewById(R.id.flash_indicator);
        this.mFlashIndicatorPhotoIcons = context.getResources().obtainTypedArray(R.array.camera_flashmode_indicator_icons);
        this.mFlashIndicatorVideoIcons = context.getResources().obtainTypedArray(R.array.video_flashmode_indicator_icons);
        this.mHdrIndicator = (ImageView) root.findViewById(R.id.hdr_indicator);
        this.mHdrPlusIndicatorIcons = context.getResources().obtainTypedArray(R.array.pref_camera_hdr_plus_indicator_icons);
        this.mHdrIndicatorIcons = context.getResources().obtainTypedArray(R.array.pref_camera_hdr_indicator_icons);
        int panoIndicatorArrayId = PhotoSphereHelper.getPanoramaOrientationIndicatorArrayId();
        if (panoIndicatorArrayId > 0) {
            this.mPanoIndicator = (ImageView) root.findViewById(R.id.pano_indicator);
            this.mPanoIndicatorIcons = context.getResources().obtainTypedArray(panoIndicatorArrayId);
        }
        this.mCountdownTimerIndicator = (ImageView) root.findViewById(R.id.countdown_timer_indicator);
        this.mCountdownTimerIndicatorIcons = context.getResources().obtainTypedArray(R.array.pref_camera_countdown_indicators);
        this.mExposureIndicatorN2 = (ImageView) root.findViewById(R.id.exposure_n2_indicator);
        this.mExposureIndicatorN1 = (ImageView) root.findViewById(R.id.exposure_n1_indicator);
        this.mExposureIndicatorP1 = (ImageView) root.findViewById(R.id.exposure_p1_indicator);
        this.mExposureIndicatorP2 = (ImageView) root.findViewById(R.id.exposure_p2_indicator);
    }

    public void onButtonVisibilityChanged(ButtonManager buttonManager, int buttonId) {
        syncIndicatorWithButton(buttonId);
    }

    public void onButtonEnabledChanged(ButtonManager buttonManager, int buttonId) {
        syncIndicatorWithButton(buttonId);
    }

    private void syncIndicatorWithButton(int buttonId) {
        switch (buttonId) {
            case 0:
                syncFlashIndicator();
                return;
            case 1:
                syncFlashIndicator();
                return;
            case 4:
                syncHdrIndicator();
                return;
            case 5:
                syncHdrIndicator();
                return;
            case 11:
                syncExposureIndicator();
                return;
            default:
                return;
        }
    }

    public void syncIndicators() {
        syncFlashIndicator();
        syncHdrIndicator();
        syncPanoIndicator();
        syncExposureIndicator();
        syncCountdownTimerIndicator();
    }

    private static void changeVisibility(View view, int visibility) {
        if (view.getVisibility() != visibility) {
            view.setVisibility(visibility);
        }
    }

    private void syncFlashIndicator() {
        ButtonManager buttonManager = this.mController.getButtonManager();
        if (buttonManager.isEnabled(0) && buttonManager.isVisible(0)) {
            int modeIndex = this.mController.getCurrentModuleIndex();
            if (modeIndex == this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_video)) {
                setIndicatorState(this.mController.getCameraScope(), Keys.KEY_VIDEOCAMERA_FLASH_MODE, this.mFlashIndicator, this.mFlashIndicatorVideoIcons, false);
                return;
            } else if (modeIndex == this.mController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_gcam)) {
                setIndicatorState(this.mController.getCameraScope(), Keys.KEY_HDR_PLUS_FLASH_MODE, this.mFlashIndicator, this.mFlashIndicatorPhotoIcons, false);
                return;
            } else {
                setIndicatorState(this.mController.getCameraScope(), Keys.KEY_FLASH_MODE, this.mFlashIndicator, this.mFlashIndicatorPhotoIcons, false);
                return;
            }
        }
        changeVisibility(this.mFlashIndicator, 8);
    }

    private void syncHdrIndicator() {
        ButtonManager buttonManager = this.mController.getButtonManager();
        if (buttonManager.isEnabled(4) && buttonManager.isVisible(4)) {
            setIndicatorState(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR_PLUS, this.mHdrIndicator, this.mHdrPlusIndicatorIcons, false);
        } else if (buttonManager.isEnabled(5) && buttonManager.isVisible(5)) {
            setIndicatorState(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR, this.mHdrIndicator, this.mHdrIndicatorIcons, false);
        } else {
            changeVisibility(this.mHdrIndicator, 8);
        }
    }

    private void syncPanoIndicator() {
        if (this.mPanoIndicator == null) {
            Log.w(TAG, "Trying to sync a pano indicator that is not initialized.");
            return;
        }
        if (this.mController.getButtonManager().isPanoEnabled()) {
            setIndicatorState(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_PANO_ORIENTATION, this.mPanoIndicator, this.mPanoIndicatorIcons, true);
        } else {
            changeVisibility(this.mPanoIndicator, 8);
        }
    }

    private void syncExposureIndicator() {
        if (this.mExposureIndicatorN2 == null || this.mExposureIndicatorN1 == null || this.mExposureIndicatorP1 == null || this.mExposureIndicatorP2 == null) {
            Log.w(TAG, "Trying to sync exposure indicators that are not initialized.");
            return;
        }
        changeVisibility(this.mExposureIndicatorN2, 8);
        changeVisibility(this.mExposureIndicatorN1, 8);
        changeVisibility(this.mExposureIndicatorP1, 8);
        changeVisibility(this.mExposureIndicatorP2, 8);
        ButtonManager buttonManager = this.mController.getButtonManager();
        if (buttonManager.isEnabled(11) && buttonManager.isVisible(11)) {
            switch (Math.round(((float) this.mController.getSettingsManager().getInteger(this.mController.getCameraScope(), Keys.KEY_EXPOSURE).intValue()) * buttonManager.getExposureCompensationStep())) {
                case -2:
                    changeVisibility(this.mExposureIndicatorN2, 0);
                    break;
                case -1:
                    changeVisibility(this.mExposureIndicatorN1, 0);
                    break;
                case 1:
                    changeVisibility(this.mExposureIndicatorP1, 0);
                    break;
                case 2:
                    changeVisibility(this.mExposureIndicatorP2, 0);
                    break;
            }
        }
    }

    private void syncCountdownTimerIndicator() {
        ButtonManager buttonManager = this.mController.getButtonManager();
        if (buttonManager.isEnabled(12) && buttonManager.isVisible(12)) {
            setIndicatorState(this.mController.getCameraScope(), Keys.KEY_COUNTDOWN_DURATION, this.mCountdownTimerIndicator, this.mCountdownTimerIndicatorIcons, false);
            return;
        }
        changeVisibility(this.mCountdownTimerIndicator, 8);
    }

    private void setIndicatorState(String scope, String key, ImageView imageView, TypedArray iconArray, boolean showDefault) {
        SettingsManager settingsManager = this.mController.getSettingsManager();
        int valueIndex = settingsManager.getIndexOfCurrentValue(scope, key);
        if (valueIndex < 0) {
            Log.w(TAG, "The setting for this indicator is not available.");
            imageView.setVisibility(8);
            return;
        }
        Drawable drawable = iconArray.getDrawable(valueIndex);
        if (drawable != null) {
            imageView.setImageDrawable(drawable);
            if (showDefault || !settingsManager.isDefault(scope, key)) {
                changeVisibility(imageView, 0);
            } else {
                changeVisibility(imageView, 8);
            }
            return;
        }
        throw new IllegalStateException("Indicator drawable is null.");
    }

    public void onSettingChanged(SettingsManager settingsManager, String key) {
        if (key.equals(Keys.KEY_FLASH_MODE)) {
            syncFlashIndicator();
        } else if (key.equals(Keys.KEY_VIDEOCAMERA_FLASH_MODE)) {
            syncFlashIndicator();
        } else if (key.equals(Keys.KEY_CAMERA_HDR_PLUS)) {
            syncHdrIndicator();
        } else if (key.equals(Keys.KEY_CAMERA_HDR)) {
            syncHdrIndicator();
        } else if (key.equals(Keys.KEY_CAMERA_PANO_ORIENTATION)) {
            syncPanoIndicator();
        } else if (key.equals(Keys.KEY_EXPOSURE)) {
            syncExposureIndicator();
        } else if (key.equals(Keys.KEY_COUNTDOWN_DURATION)) {
            syncCountdownTimerIndicator();
        }
    }
}
