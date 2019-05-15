package com.hmdglobal.app.camera;

import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Vibrator;
import android.provider.Settings.System;
import android.view.KeyEvent;
import android.view.View;
import com.android.ex.camera2.portability.CameraSettings;
import com.android.ex.camera2.portability.Size;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.settings.SettingsUtil;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.hmdglobal.app.camera.util.CustomFields;
import com.hmdglobal.app.camera.util.CustomUtil;
import java.util.List;

public class NormalVideoModule extends VideoModule {
    public static final String NORMAL_VIDEO_MODULE_STRING_ID = "NormalVideoModule";
    private static final Tag TAG = new Tag(NORMAL_VIDEO_MODULE_STRING_ID);
    private static final int VIDEO_HIGH_FRAME_RATE = 60;
    private boolean mIsHsrVideo;

    public NormalVideoModule(AppController app) {
        super(app);
    }

    public int getModuleId() {
        return this.mAppController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_video);
    }

    /* Access modifiers changed, original: protected */
    public void disableShutterDuringResume() {
        if (this.mCameraState == 0) {
            this.mAppController.getLockEventListener().forceBlocking();
        }
    }

    public String getModuleStringIdentifier() {
        return NORMAL_VIDEO_MODULE_STRING_ID;
    }

    /* Access modifiers changed, original: protected */
    public boolean isFacebeautyEnabled() {
        if (CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_PHOTO_FACEBEAUTY_SUPPORT, true) && CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_VIDEO_FACEBEAUTY_SUPPORT, true)) {
            return true;
        }
        return false;
    }

    public void onSingleTapUp(View view, int x, int y) {
        super.onSingleTapUp(view, x, y);
    }

    /* Access modifiers changed, original: protected */
    public void showBoomKeyTip() {
        if (System.getInt(this.mContentResolver, CameraUtil.BOOM_EFFECT_SETTINGS, 0) == 1) {
            getVideoUI().showBoomKeyTipUI();
        }
    }

    /* Access modifiers changed, original: protected */
    public void hideBoomKeyTip() {
        getVideoUI().hideBoomKeyTipUI();
    }

    private void onBoomPressed() {
        if (CustomUtil.getInstance().getBoolean(CustomFields.DEF_VIDEO_BOOMKEY_TIZR_SHARE_ON, false)) {
            try {
                String packageName = CameraUtil.TIZR_PACKAGE_NAME;
                if (this.mActivity.getPackageManager().getLaunchIntentForPackage(packageName) == null) {
                    Tag tag = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("No ");
                    stringBuilder.append(packageName);
                    stringBuilder.append(" installed.");
                    Log.e(tag, stringBuilder.toString());
                    this.mVideoBoomKeyFlags = false;
                } else {
                    this.mVideoBoomKeyFlags = true;
                    if (((AudioManager) this.mActivity.getSystemService("audio")).getRingerMode() != 0) {
                        Vibrator vibrator = (Vibrator) this.mActivity.getSystemService("vibrator");
                        if (vibrator != null) {
                            vibrator.vibrate(110);
                        }
                    }
                }
                if (!(this.mPaused || this.mIsVideoCaptureIntent || !this.mMediaRecorderRecording)) {
                    Log.i(TAG, "onBoomPressed stop video recording");
                    onStopVideoRecording();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* Access modifiers changed, original: protected */
    public void enableTorchMode(boolean enable) {
        super.enableTorchMode(enable);
    }

    /* Access modifiers changed, original: protected */
    public void startVideoNotityHelpTip() {
        HelpTipsManager helpTipsManager = this.mAppController.getHelpTipsManager();
        if (helpTipsManager != null) {
            helpTipsManager.startRecordVideoResponse();
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (this.mPaused) {
            return true;
        }
        if (keyCode != CameraUtil.BOOM_KEY) {
            switch (keyCode) {
                case 24:
                case 25:
                    if (event.getRepeatCount() == 0 && !this.mActivity.getCameraAppUI().isInIntentReview() && this.mAppController.isShutterEnabled() && !this.mActivity.isRecording()) {
                        onShutterButtonClick();
                    } else if (this.mActivity.isRecording()) {
                        onShutterButtonClick();
                    }
                    return true;
                default:
                    return false;
            }
        }
        Boolean bVdfCustomizeBoomKey = Boolean.valueOf(CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_VDF_BOOMKEY_CUSTOMIZE, false));
        int iBoomEffectSetting = System.getInt(this.mContentResolver, CameraUtil.BOOM_EFFECT_SETTINGS, 0);
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onKeyDown mMediaRecorderRecording = ");
        stringBuilder.append(this.mMediaRecorderRecording);
        stringBuilder.append(",bVdfCustomizeBoomKey = ");
        stringBuilder.append(bVdfCustomizeBoomKey);
        stringBuilder.append(",iBoomEffectSetting = ");
        stringBuilder.append(iBoomEffectSetting);
        Log.i(tag, stringBuilder.toString());
        if (bVdfCustomizeBoomKey.booleanValue()) {
            if (this.mMediaRecorderRecording) {
                Log.e(TAG, "onKeyDown doVideoCapture when MediaRecorder is Recording");
                doVideoCapture();
            } else if (event.getRepeatCount() == 0 && !this.mActivity.getCameraAppUI().isInIntentReview() && this.mAppController.isShutterEnabled()) {
                onShutterButtonClick();
            }
        } else if (!this.mActivity.isSecureCamera() && iBoomEffectSetting == 1 && this.mMediaRecorderRecording) {
            onBoomPressed();
        } else {
            Log.e(TAG, "onKeyDown BOOM_KEY is handled as shuttonbutton clicking");
            if (event.getRepeatCount() == 0 && !this.mActivity.getCameraAppUI().isInIntentReview() && this.mAppController.isShutterEnabled()) {
                onShutterButtonClick();
            }
        }
        return true;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode != CameraUtil.BOOM_KEY) {
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

    /* Access modifiers changed, original: protected */
    public int getProfileQuality() {
        int quality = super.getProfileQuality();
        this.mIsHsrVideo = false;
        if (!String.valueOf(quality).equals(SettingsUtil.QUALITY_1080P_60FPS)) {
            return quality;
        }
        this.mIsHsrVideo = true;
        return ((Integer) SettingsUtil.VIDEO_QUALITY_VALUE_TABLE.get(String.valueOf(quality))).intValue();
    }

    /* Access modifiers changed, original: protected */
    public void setHsr(CameraSettings cameraSettings) {
        if (this.mIsHsrVideo) {
            cameraSettings.setHsr("60");
        } else {
            super.setHsr(cameraSettings);
        }
    }

    /* Access modifiers changed, original: protected */
    public boolean isSupported(int width, int height) {
        if (!this.mIsHsrVideo) {
            return super.isSupported(width, height);
        }
        Size maxHsrSize = null;
        List<String> supportedVideoHighFrameRates = this.mCameraCapabilities.getSupportedVideoHighFrameRates();
        boolean isSupported = supportedVideoHighFrameRates != null && supportedVideoHighFrameRates.indexOf(String.valueOf(60)) >= 0;
        if (isSupported) {
            int index = supportedVideoHighFrameRates.indexOf("60");
            if (index != -1) {
                maxHsrSize = (Size) this.mCameraCapabilities.getSupportedHsrSizes().get(index);
            }
        }
        if (maxHsrSize != null && width <= maxHsrSize.width() && height <= maxHsrSize.height()) {
            return true;
        }
        Log.e(TAG, "Unsupported HSR and video size combinations");
        return false;
    }

    /* Access modifiers changed, original: protected */
    public void mediaRecorderParameterFetching(MediaRecorder recorder) {
        if (this.mIsHsrVideo) {
            Log.w(TAG, "set mediaRecorder parameters");
            recorder.setVideoSource(1);
            recorder.setAudioSource(5);
            recorder.setOutputFormat(this.mProfile.fileFormat);
            int scaledBitrate = (this.mProfile.videoBitRate * 60) / this.mProfile.videoFrameRate;
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Scaled Video bitrate : ");
            stringBuilder.append(scaledBitrate);
            Log.i(tag, stringBuilder.toString());
            recorder.setVideoEncodingBitRate(scaledBitrate);
            recorder.setAudioEncodingBitRate(this.mProfile.audioBitRate);
            recorder.setAudioChannels(this.mProfile.audioChannels);
            recorder.setAudioSamplingRate(this.mProfile.audioSampleRate);
            recorder.setVideoEncoder(this.mProfile.videoCodec);
            recorder.setAudioEncoder(this.mProfile.audioCodec);
            recorder.setVideoSize(this.mProfile.videoFrameWidth, this.mProfile.videoFrameHeight);
            recorder.setMaxDuration(getOverrodeVideoDuration());
            recorder.setCaptureRate(60.0d);
            recorder.setVideoFrameRate(60);
            return;
        }
        super.mediaRecorderParameterFetching(recorder);
    }
}
