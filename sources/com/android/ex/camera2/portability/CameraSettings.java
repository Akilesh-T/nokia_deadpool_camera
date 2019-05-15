package com.android.ex.camera2.portability;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera.Area;
import com.android.ex.camera2.portability.CameraCapabilities.FlashMode;
import com.android.ex.camera2.portability.CameraCapabilities.FocusMode;
import com.android.ex.camera2.portability.CameraCapabilities.SceneMode;
import com.android.ex.camera2.portability.CameraCapabilities.WhiteBalance;
import com.android.ex.camera2.portability.debug.Log;
import com.android.ex.camera2.portability.debug.Log.Tag;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.morphoinc.app.panoramagp3.Camera2ParamsFragment;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public abstract class CameraSettings {
    public static final HashMap<String, Integer> KEY_ISO_INDEX = new HashMap();
    private static final int MAX_JPEG_COMPRESSION_QUALITY = 100;
    private static final int MIN_JPEG_COMPRESSION_QUALITY = 1;
    private static final Tag TAG = new Tag("CamSet");
    public int aec = 0;
    public boolean isZslOn = false;
    public String mAntibanding;
    protected boolean mAutoExposureLocked;
    protected boolean mAutoWhiteBalanceLocked;
    protected boolean mCameraAisEnabled;
    protected int mContinuousIso;
    protected FlashMode mCurrentFlashMode;
    protected FocusMode mCurrentFocusMode;
    protected int mCurrentPhotoFormat;
    protected Size mCurrentPhotoSize;
    private int mCurrentPreviewFormat;
    protected Size mCurrentPreviewSize;
    protected SceneMode mCurrentSceneMode;
    protected Size mCurrentVideoSize;
    protected float mCurrentZoomRatio;
    protected List<EvInfo> mEvInfos;
    protected Size mExifThumbnailSize;
    protected int mExposureCompensationIndex;
    protected String mExposureTime = "0";
    private int mFaceBeautySkinSmoothing;
    protected final List<Area> mFocusAreas = new ArrayList();
    protected final Map<String, String> mGeneralSetting = new TreeMap();
    protected GpsData mGpsData;
    protected List<Float> mHdrEv;
    protected String mHsr;
    protected boolean mIsAddWaterMarkEnabled;
    protected boolean mIsAdjustEt;
    protected boolean mIsDepthOn;
    private boolean mIsFaceBeauty;
    private boolean mIsLowLight;
    private boolean mIsMirrorSelfieOn = false;
    protected boolean mIsMotionOn;
    protected boolean mIsProMode;
    protected boolean mIsSaveDngEnabled;
    private boolean mIsSuperResolution;
    protected String mIso = "auto";
    protected byte mJpegCompressQuality;
    protected boolean mLiveBokehEnabled;
    protected int mLiveBokehLevel;
    protected boolean mManualExposureSupported = false;
    protected float mManualFocusPosition;
    protected final List<Area> mMeteringAreas = new ArrayList();
    private float mMinFocusDistsnce;
    protected boolean mNeedBurst;
    protected int mPreviewFpsRangeMax;
    protected int mPreviewFpsRangeMin;
    protected int mPreviewFrameRate;
    protected int mRTDofMode;
    protected boolean mRecordingHintEnabled;
    protected boolean mShouldSaveDng;
    protected boolean mSizesLocked;
    protected boolean mUseJpeg;
    protected boolean mVideoStabilizationEnabled;
    protected WhiteBalance mWhiteBalance;

    public static class BoostParameters {
        public int cameraId;
        public Context context;
        public boolean isZslOn;
        public SettingsManager settingsManager;
        public SurfaceTexture surfaceTexture;
    }

    public static class GpsData {
        public final double altitude;
        public final double latitude;
        public final double longitude;
        public final String processingMethod;
        public final long timeStamp;

        public GpsData(double latitude, double longitude, double altitude, long timeStamp, String processingMethod) {
            if (processingMethod == null && !(latitude == Camera2ParamsFragment.TARGET_EV && longitude == Camera2ParamsFragment.TARGET_EV && altitude == Camera2ParamsFragment.TARGET_EV)) {
                Log.w(CameraSettings.TAG, "GpsData's nonzero data will be ignored due to null processingMethod");
            }
            this.latitude = latitude;
            this.longitude = longitude;
            this.altitude = altitude;
            this.timeStamp = timeStamp;
            this.processingMethod = processingMethod;
        }

        public GpsData(GpsData src) {
            this.latitude = src.latitude;
            this.longitude = src.longitude;
            this.altitude = src.altitude;
            this.timeStamp = src.timeStamp;
            this.processingMethod = src.processingMethod;
        }
    }

    public abstract CameraSettings copy();

    static {
        KEY_ISO_INDEX.put("auto", Integer.valueOf(0));
        KEY_ISO_INDEX.put("deblur", Integer.valueOf(1));
        KEY_ISO_INDEX.put("100", Integer.valueOf(2));
        KEY_ISO_INDEX.put("200", Integer.valueOf(3));
        KEY_ISO_INDEX.put("400", Integer.valueOf(4));
        KEY_ISO_INDEX.put("800", Integer.valueOf(5));
        KEY_ISO_INDEX.put("1600", Integer.valueOf(6));
    }

    protected CameraSettings() {
    }

    protected CameraSettings(CameraSettings src) {
        this.mGeneralSetting.putAll(src.mGeneralSetting);
        this.mMeteringAreas.addAll(src.mMeteringAreas);
        this.mFocusAreas.addAll(src.mFocusAreas);
        this.mSizesLocked = src.mSizesLocked;
        this.mPreviewFpsRangeMin = src.mPreviewFpsRangeMin;
        this.mPreviewFpsRangeMax = src.mPreviewFpsRangeMax;
        this.mPreviewFrameRate = src.mPreviewFrameRate;
        Size size = null;
        this.mCurrentPreviewSize = src.mCurrentPreviewSize == null ? null : new Size(src.mCurrentPreviewSize);
        this.mCurrentPreviewFormat = src.mCurrentPreviewFormat;
        this.mCurrentPhotoSize = src.mCurrentPhotoSize == null ? null : new Size(src.mCurrentPhotoSize);
        if (src.mCurrentVideoSize != null) {
            size = new Size(src.mCurrentVideoSize);
        }
        this.mCurrentVideoSize = size;
        this.mJpegCompressQuality = src.mJpegCompressQuality;
        this.mCurrentPhotoFormat = src.mCurrentPhotoFormat;
        this.mCurrentZoomRatio = src.mCurrentZoomRatio;
        this.mExposureCompensationIndex = src.mExposureCompensationIndex;
        this.mCurrentFlashMode = src.mCurrentFlashMode;
        this.mCurrentFocusMode = src.mCurrentFocusMode;
        this.mCurrentSceneMode = src.mCurrentSceneMode;
        this.mWhiteBalance = src.mWhiteBalance;
        this.mVideoStabilizationEnabled = src.mVideoStabilizationEnabled;
        this.mAutoExposureLocked = src.mAutoExposureLocked;
        this.mAutoWhiteBalanceLocked = src.mAutoWhiteBalanceLocked;
        this.mRecordingHintEnabled = src.mRecordingHintEnabled;
        this.mGpsData = src.mGpsData;
        this.mExifThumbnailSize = src.mExifThumbnailSize;
        this.mManualFocusPosition = src.mManualFocusPosition;
        this.mContinuousIso = src.mContinuousIso;
        this.mIso = src.mIso;
        this.mExposureTime = src.mExposureTime;
        this.isZslOn = src.isZslOn;
        this.mHsr = src.mHsr;
        this.mIsSaveDngEnabled = src.mIsSaveDngEnabled;
        this.mIsAddWaterMarkEnabled = src.mIsAddWaterMarkEnabled;
        this.mIsFaceBeauty = src.mIsFaceBeauty;
        this.mFaceBeautySkinSmoothing = src.mFaceBeautySkinSmoothing;
        this.mIsLowLight = src.mIsLowLight;
        this.mIsSuperResolution = src.mIsSuperResolution;
        this.aec = src.aec;
        this.mAntibanding = src.mAntibanding;
        this.mIsMirrorSelfieOn = src.mIsMirrorSelfieOn;
        this.mLiveBokehEnabled = src.mLiveBokehEnabled;
        this.mLiveBokehLevel = src.mLiveBokehLevel;
        this.mHdrEv = src.mHdrEv;
        this.mNeedBurst = src.mNeedBurst;
        this.mEvInfos = src.mEvInfos;
        this.mIsMotionOn = src.mIsMotionOn;
        this.mIsDepthOn = src.mIsDepthOn;
        this.mIsAdjustEt = src.mIsAdjustEt;
        this.mUseJpeg = src.mUseJpeg;
        this.mIsProMode = src.mIsProMode;
        this.mShouldSaveDng = src.mShouldSaveDng;
        this.mRTDofMode = src.mRTDofMode;
        this.mManualExposureSupported = src.mManualExposureSupported;
    }

    @Deprecated
    public void setSetting(String key, String value) {
        this.mGeneralSetting.put(key, value);
    }

    public void setSizesLocked(boolean locked) {
        this.mSizesLocked = locked;
    }

    public void setPreviewFpsRange(int min, int max) {
        if (min > max) {
            int temp = max;
            max = min;
            min = temp;
        }
        this.mPreviewFpsRangeMax = max;
        this.mPreviewFpsRangeMin = min;
        this.mPreviewFrameRate = -1;
    }

    public int getPreviewFpsRangeMin() {
        return this.mPreviewFpsRangeMin;
    }

    public int getPreviewFpsRangeMax() {
        return this.mPreviewFpsRangeMax;
    }

    public void setPreviewFrameRate(int frameRate) {
        if (frameRate > 0) {
            this.mPreviewFrameRate = frameRate;
            this.mPreviewFpsRangeMax = frameRate;
            this.mPreviewFpsRangeMin = frameRate;
        }
    }

    public int getPreviewFrameRate() {
        return this.mPreviewFrameRate;
    }

    public Size getCurrentPreviewSize() {
        return new Size(this.mCurrentPreviewSize);
    }

    public boolean setPreviewSize(Size previewSize) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set preview size:");
        stringBuilder.append(previewSize);
        Log.d(tag, stringBuilder.toString());
        if (this.mSizesLocked) {
            Log.w(TAG, "Attempt to change preview size while locked");
            return false;
        }
        this.mCurrentPreviewSize = new Size(previewSize);
        return true;
    }

    public void setPreviewFormat(int format) {
        this.mCurrentPreviewFormat = format;
    }

    public int getCurrentPreviewFormat() {
        return this.mCurrentPreviewFormat;
    }

    public Size getCurrentPhotoSize() {
        return new Size(this.mCurrentPhotoSize);
    }

    public boolean setPhotoSize(Size photoSize) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set photo size:");
        stringBuilder.append(photoSize);
        Log.d(tag, stringBuilder.toString());
        if (this.mSizesLocked) {
            Log.w(TAG, "Attempt to change photo size while locked");
            return false;
        }
        this.mCurrentPhotoSize = new Size(photoSize);
        tag = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("mCurrentPhotoSize size:");
        stringBuilder.append(this.mCurrentPhotoSize);
        Log.d(tag, stringBuilder.toString());
        return true;
    }

    public Size getCurrentVideoSize() {
        return new Size(this.mCurrentVideoSize);
    }

    public void setHdrEv(List<Float> hdrEv) {
        this.mHdrEv = hdrEv;
    }

    public List<Float> getHdrEv() {
        return this.mHdrEv;
    }

    public void setEvInfo(List<EvInfo> evInfo) {
        this.mEvInfos = evInfo;
    }

    public List<EvInfo> getEvInfo() {
        return this.mEvInfos;
    }

    public boolean needBurst() {
        return this.mNeedBurst;
    }

    public void setNeedBurst(boolean needBurst) {
        this.mNeedBurst = needBurst;
    }

    public boolean isMotionOn() {
        return this.mIsMotionOn;
    }

    public void setMotionOn(boolean motionOn) {
        this.mIsMotionOn = motionOn;
    }

    public boolean isDepthOn() {
        return this.mIsDepthOn;
    }

    public void setDepthOn(boolean depthOn) {
        this.mIsDepthOn = depthOn;
    }

    public boolean shouldSaveDng() {
        return this.mShouldSaveDng;
    }

    public void setShouldSaveDng(boolean shouldSaveDng) {
        this.mShouldSaveDng = shouldSaveDng;
    }

    public int getRTDofMode() {
        return this.mRTDofMode;
    }

    public void setRTDofMode(int RTDofMode) {
        this.mRTDofMode = RTDofMode;
    }

    public boolean isProMode() {
        return this.mIsProMode;
    }

    public void setProMode(boolean isProMode) {
        this.mIsProMode = isProMode;
    }

    public boolean isAdjustEt() {
        return this.mIsAdjustEt;
    }

    public void setAdjustEt(boolean adjustEt) {
        this.mIsAdjustEt = adjustEt;
    }

    public boolean isUseJpeg() {
        return this.mUseJpeg;
    }

    public void setUseJpeg(boolean useJpeg) {
        this.mUseJpeg = useJpeg;
    }

    public boolean setVideoSize(Size videoSize) {
        if (this.mSizesLocked) {
            Log.w(TAG, "Attempt to change video size while locked");
            return false;
        }
        this.mCurrentVideoSize = new Size(videoSize);
        return true;
    }

    public void setPhotoFormat(int format) {
        this.mCurrentPhotoFormat = format;
    }

    public int getCurrentPhotoFormat() {
        return this.mCurrentPhotoFormat;
    }

    public void setPhotoJpegCompressionQuality(int quality) {
        if (quality < 1 || quality > 100) {
            Log.w(TAG, "Ignoring JPEG quality that falls outside the expected range");
        } else {
            this.mJpegCompressQuality = (byte) quality;
        }
    }

    public int getPhotoJpegCompressionQuality() {
        return this.mJpegCompressQuality;
    }

    public float getCurrentZoomRatio() {
        return this.mCurrentZoomRatio;
    }

    public void setZoomRatio(float ratio) {
        this.mCurrentZoomRatio = ratio;
    }

    public void setLiveBokehEnabled(boolean liveBokehEnabled) {
        this.mLiveBokehEnabled = liveBokehEnabled;
    }

    public boolean getLiveBokehEnabled() {
        return this.mLiveBokehEnabled;
    }

    public void setLiveBokehLevel(int level) {
        this.mLiveBokehLevel = level;
    }

    public int getLiveBokehLevel() {
        return this.mLiveBokehLevel;
    }

    public void setExposureCompensationIndex(int index) {
        this.mExposureCompensationIndex = index;
    }

    public int getExposureCompensationIndex() {
        return this.mExposureCompensationIndex;
    }

    public void setAutoExposureLock(boolean locked) {
        this.mAutoExposureLocked = locked;
    }

    public boolean isAutoExposureLocked() {
        return this.mAutoExposureLocked;
    }

    public void setMeteringAreas(List<Area> areas) {
        this.mMeteringAreas.clear();
        if (areas != null) {
            this.mMeteringAreas.addAll(areas);
        }
    }

    public List<Area> getMeteringAreas() {
        return new ArrayList(this.mMeteringAreas);
    }

    public FlashMode getCurrentFlashMode() {
        return this.mCurrentFlashMode;
    }

    public void setFlashMode(FlashMode flashMode) {
        this.mCurrentFlashMode = flashMode;
    }

    public void setFocusMode(FocusMode focusMode) {
        this.mCurrentFocusMode = focusMode;
    }

    public FocusMode getCurrentFocusMode() {
        return this.mCurrentFocusMode;
    }

    public void setFocusAreas(List<Area> areas) {
        this.mFocusAreas.clear();
        if (areas != null) {
            this.mFocusAreas.addAll(areas);
        }
    }

    public List<Area> getFocusAreas() {
        return new ArrayList(this.mFocusAreas);
    }

    public void setWhiteBalance(WhiteBalance whiteBalance) {
        this.mWhiteBalance = whiteBalance;
    }

    public WhiteBalance getWhiteBalance() {
        return this.mWhiteBalance;
    }

    public void setAutoWhiteBalanceLock(boolean locked) {
        this.mAutoWhiteBalanceLocked = locked;
    }

    public boolean isAutoWhiteBalanceLocked() {
        return this.mAutoWhiteBalanceLocked;
    }

    public SceneMode getCurrentSceneMode() {
        return this.mCurrentSceneMode;
    }

    public void setSceneMode(SceneMode sceneMode) {
        if (sceneMode == SceneMode.HDR) {
            this.mIsFaceBeauty = false;
            this.mIsLowLight = false;
        }
        this.mCurrentSceneMode = sceneMode;
    }

    public void setVideoStabilization(boolean enabled) {
        this.mVideoStabilizationEnabled = enabled;
    }

    public boolean isVideoStabilizationEnabled() {
        return this.mVideoStabilizationEnabled;
    }

    public void setCameraAisEnabled(boolean enabled) {
        this.mCameraAisEnabled = enabled;
    }

    public boolean isCameraAisEnabled() {
        return this.mCameraAisEnabled;
    }

    public void setRecordingHintEnabled(boolean hintEnabled) {
        this.mRecordingHintEnabled = hintEnabled;
    }

    public boolean isRecordingHintEnabled() {
        return this.mRecordingHintEnabled;
    }

    public void setGpsData(GpsData data) {
        this.mGpsData = new GpsData(data);
    }

    public GpsData getGpsData() {
        return this.mGpsData == null ? null : new GpsData(this.mGpsData);
    }

    public void clearGpsData() {
        this.mGpsData = null;
    }

    public void setExifThumbnailSize(Size s) {
        this.mExifThumbnailSize = s;
    }

    public Size getExifThumbnailSize() {
        return this.mExifThumbnailSize == null ? null : new Size(this.mExifThumbnailSize);
    }

    public void setContinuousIso(int continuousIso) {
        this.mContinuousIso = continuousIso;
    }

    public int getContinuousIso() {
        return this.mContinuousIso;
    }

    public void setISOValue(String iso) {
        this.mIso = iso;
    }

    public String getISOValue() {
        return this.mIso;
    }

    public void setManualFocusPosition(float focusMode) {
        this.mManualFocusPosition = focusMode;
    }

    public float getManualFocusPosition() {
        return this.mManualFocusPosition;
    }

    public void setExposureTime(String exposureTime) {
        this.mExposureTime = exposureTime;
    }

    public String getExposureTime() {
        return this.mExposureTime;
    }

    public boolean isManualExposureSupported() {
        return this.mManualExposureSupported;
    }

    public void setManualExposureSupported(boolean mManualExposureSupported) {
        this.mManualExposureSupported = mManualExposureSupported;
    }

    public String getHsr() {
        return this.mHsr;
    }

    public void setHsr(String mHsr) {
        this.mHsr = mHsr;
    }

    public boolean isSaveDngEnabled() {
        return this.mIsSaveDngEnabled;
    }

    public boolean isAddWaterMarkEnabled() {
        return this.mIsAddWaterMarkEnabled;
    }

    public void setSaveDngEnabled(boolean enabled) {
        this.mIsSaveDngEnabled = enabled;
    }

    public void setAddWaterMarkEnabled(boolean enabled) {
        this.mIsAddWaterMarkEnabled = enabled;
    }

    public boolean getLowLight() {
        return this.mIsLowLight;
    }

    public boolean getFaceBeauty() {
        return this.mIsFaceBeauty;
    }

    public int getFaceBeautySkinSmoothing() {
        return this.mFaceBeautySkinSmoothing;
    }

    public void setLowLight(boolean isLowLightOn) {
        if (isLowLightOn) {
            this.mIsFaceBeauty = false;
            setSceneMode(SceneMode.AUTO);
        }
        this.mIsLowLight = isLowLightOn;
    }

    public void setFaceBeauty(boolean isFaceBeautyOn, int skinSmoothing) {
        if (isFaceBeautyOn) {
            this.mFaceBeautySkinSmoothing = skinSmoothing;
            this.mIsLowLight = false;
            setSceneMode(SceneMode.AUTO);
        }
        this.mIsFaceBeauty = isFaceBeautyOn;
    }

    public void setSuperResolutionOn(boolean isSuperResolutionOn) {
        this.mIsSuperResolution = isSuperResolutionOn;
    }

    public boolean isSuperResolutionOn() {
        return this.mIsSuperResolution;
    }

    public void setAec(String aecValue) {
        if (aecValue != null) {
            try {
                this.aec = Integer.parseInt(aecValue);
            } catch (NumberFormatException e) {
            }
        }
    }

    public int getAec() {
        return this.aec;
    }

    public void setAntibanding(String antibanding) {
        this.mAntibanding = antibanding;
    }

    public String getAntibanding() {
        return this.mAntibanding;
    }

    public boolean getMirrorSelfieOn() {
        return this.mIsMirrorSelfieOn;
    }

    public void setMirrorSelfieOn(boolean isMirror) {
        this.mIsMirrorSelfieOn = isMirror;
    }

    public float getMinFocusDistsnce() {
        return this.mMinFocusDistsnce;
    }
}
