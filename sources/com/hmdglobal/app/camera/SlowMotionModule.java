package com.hmdglobal.app.camera;

import android.media.MediaRecorder;
import android.os.SystemClock;
import android.view.KeyEvent;
import com.android.ex.camera2.portability.CameraCapabilities.FocusMode;
import com.android.ex.camera2.portability.CameraSettings;
import com.android.ex.camera2.portability.Size;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.settings.SettingsUtil;
import com.hmdglobal.app.camera.util.CameraUtil;
import java.util.List;

public class SlowMotionModule extends VideoModule {
    private static final String SLOW_MOTION_MODULE_STRING_ID = "SlowMotionModule";
    private static final Tag TAG = new Tag(SLOW_MOTION_MODULE_STRING_ID);
    private static final int VIDEO_HIGH_FRAME_RATE = 120;
    private Size mHsrSize = null;

    public SlowMotionModule(AppController app) {
        super(app);
    }

    /* Access modifiers changed, original: protected */
    public boolean isNeedStartRecordingOnSwitching() {
        return false;
    }

    public int getModuleId() {
        return this.mAppController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_slowmotion);
    }

    /* Access modifiers changed, original: protected */
    public boolean isVideoStabilizationEnabled() {
        return false;
    }

    public String getModuleStringIdentifier() {
        return SLOW_MOTION_MODULE_STRING_ID;
    }

    /* Access modifiers changed, original: protected */
    public VideoUI getVideoUI() {
        return new SlowMotionUI(this.mActivity, this, this.mActivity.getModuleLayoutRoot());
    }

    /* Access modifiers changed, original: protected */
    public void mediaRecorderParameterFetching(MediaRecorder recorder) {
        Log.w(TAG, "set slow motion mediaRecorder parameters");
        recorder.setVideoSource(1);
        recorder.setAudioSource(5);
        recorder.setOutputFormat(this.mProfile.fileFormat);
        int scaledBitrate = (this.mProfile.videoBitRate * 120) / this.mProfile.videoFrameRate;
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
        recorder.setCaptureRate(120.0d);
        recorder.setVideoFrameRate(120);
    }

    /* Access modifiers changed, original: protected */
    public void tryLockFocus() {
        if (this.mCameraCapabilities.supports(FocusMode.AUTO) && this.mCameraState == 3 && this.mFocusManager.getFocusMode(this.mCameraSettings.getCurrentFocusMode()) != FocusMode.AUTO) {
            this.mFocusManager.overrideFocusMode(FocusMode.AUTO);
            this.mCameraSettings.setFocusMode(FocusMode.AUTO);
            this.mCameraDevice.applySettings(this.mCameraSettings);
        }
    }

    /* Access modifiers changed, original: protected */
    public void Focus() {
        if (this.slFocuse) {
            this.mCameraDevice.cancelAutoFocus();
            this.slFocuse = false;
        }
    }

    /* Access modifiers changed, original: protected */
    public boolean isSupported(int width, int height) {
        if (this.mHsrSize == null || width > this.mHsrSize.width() || height > this.mHsrSize.height()) {
            Log.e(TAG, "Unsupported HSR and video size combinations");
            return false;
        }
        int expectedMBsPerSec = (width * height) * 120;
        return true;
    }

    /* Access modifiers changed, original: protected */
    public void setHsr(CameraSettings cameraSettings) {
        cameraSettings.setHsr("120");
    }

    /* Access modifiers changed, original: protected */
    public int getProfileQuality() {
        int quality = 0;
        List<String> supportedVideoHighFrameRates = this.mCameraCapabilities.getSupportedVideoHighFrameRates();
        boolean isSupported = false;
        if (supportedVideoHighFrameRates != null && supportedVideoHighFrameRates.indexOf(String.valueOf(120)) >= 0) {
            isSupported = true;
        }
        if (isSupported) {
            int index = supportedVideoHighFrameRates.indexOf("120");
            if (index != -1) {
                this.mHsrSize = (Size) this.mCameraCapabilities.getSupportedHsrSizes().get(index);
            }
        }
        if (this.mHsrSize != null) {
            String hsrQuality = new StringBuilder();
            hsrQuality.append(this.mHsrSize.width());
            hsrQuality.append("x");
            hsrQuality.append(this.mHsrSize.height());
            hsrQuality = hsrQuality.toString();
            if (SettingsUtil.VIDEO_QUALITY_TABLE.containsKey(hsrQuality)) {
                quality = ((Integer) SettingsUtil.VIDEO_QUALITY_TABLE.get(hsrQuality)).intValue();
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Selected video quality for '");
                stringBuilder.append(hsrQuality);
                Log.w(tag, stringBuilder.toString());
            } else {
                quality = getProximateQuality(this.mHsrSize.width(), this.mHsrSize.height());
            }
        }
        Tag tag2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Profile quality is ");
        stringBuilder2.append(quality);
        Log.w(tag2, stringBuilder2.toString());
        return quality;
    }

    private int getProximateQuality(int hsrWidth, int hsrHeight) {
        String proximateResolution = CameraUtil.getProximateResolution(SettingsUtil.VIDEO_QUALITY_TABLE.keySet(), hsrWidth, hsrHeight);
        if (proximateResolution == null) {
            return 0;
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Selected video quality for '");
        stringBuilder.append(proximateResolution);
        Log.w(tag, stringBuilder.toString());
        return ((Integer) SettingsUtil.VIDEO_QUALITY_TABLE.get(proximateResolution)).intValue();
    }

    /* Access modifiers changed, original: protected */
    public void overrideProfileSize() {
        Log.w(TAG, "override profile size");
        if (this.mHsrSize != null) {
            int hsrWidth = this.mHsrSize.width();
            int hsrHeight = this.mHsrSize.height();
            if (this.mProfile.videoFrameWidth != hsrWidth || this.mProfile.videoFrameHeight != hsrHeight) {
                if (this.mCameraCapabilities.getSupportedVideoSizes().contains(new Size(hsrWidth, hsrHeight))) {
                    this.mProfile.videoFrameWidth = hsrWidth;
                    this.mProfile.videoFrameHeight = hsrHeight;
                } else {
                    Size maxSize = CameraUtil.getProximateSize(this.mCameraCapabilities.getSupportedVideoSizes(), hsrWidth, hsrHeight);
                    if (maxSize != null) {
                        this.mProfile.videoFrameWidth = maxSize.width();
                        this.mProfile.videoFrameHeight = maxSize.height();
                    }
                }
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("video size is:");
                stringBuilder.append(this.mProfile.videoFrameWidth);
                stringBuilder.append("x");
                stringBuilder.append(this.mProfile.videoFrameHeight);
                Log.w(tag, stringBuilder.toString());
            }
        }
    }

    /* Access modifiers changed, original: protected */
    public boolean hideCamera() {
        return true;
    }

    public void onShutterButtonClick() {
        if (!this.mMediaRecorderRecording || SystemClock.uptimeMillis() - this.mRecordingStartTime > 1000) {
            super.onShutterButtonClick();
        }
    }

    public void pause() {
        super.pause();
    }

    public void hardResetSettings(SettingsManager settingsManager) {
        super.hardResetSettings(settingsManager);
        settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (this.mPaused) {
            return true;
        }
        if (keyCode != CameraUtil.BOOM_KEY) {
            switch (keyCode) {
                case 24:
                case 25:
                    break;
                default:
                    return false;
            }
        }
        if (event.getRepeatCount() == 0 && !this.mActivity.mDuringCall) {
            onShutterButtonClick();
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
}
