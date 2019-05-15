package com.android.ex.camera2.portability;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera.Area;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Builder;
import android.hardware.camera2.CaptureRequest.Key;
import android.hardware.camera2.params.MeteringRectangle;
import android.location.Location;
import android.util.Range;
import android.util.Size;
import com.android.ex.camera2.portability.CameraCapabilities.FlashMode;
import com.android.ex.camera2.portability.debug.Log;
import com.android.ex.camera2.portability.debug.Log.Tag;
import com.android.ex.camera2.utils.Camera2RequestSettingsSet;
import com.morphoinc.app.panoramagp3.Camera2ParamsFragment;
import java.util.List;
import java.util.Objects;

public class AndroidCamera2Settings extends CameraSettings {
    public static final int CONTROL_AE_ANTIBANDING_MODE_AUTO_50HZ = 4;
    public static final int CONTROL_AE_ANTIBANDING_MODE_AUTO_60HZ = 5;
    public static final int RT_DOF_PAUSE = 1;
    public static final int RT_DOF_RESUME = 2;
    private static final Tag TAG = new Tag("AndCam2Set");
    private final Rect mActiveArray;
    private final Rect mCropRectangle;
    private final Camera2RequestSettingsSet mRequestSettings;
    private final Builder mTemplateSettings;
    private Rect mVisiblePreviewRectangle;

    public AndroidCamera2Settings(CameraDevice camera, int template, Rect activeArray, Size preview, Size photo) throws CameraAccessException {
        if (camera == null) {
            throw new NullPointerException("camera must not be null");
        } else if (activeArray != null) {
            this.mTemplateSettings = camera.createCaptureRequest(template);
            this.mRequestSettings = new Camera2RequestSettingsSet();
            this.mActiveArray = activeArray;
            this.mCropRectangle = new Rect(0, 0, activeArray.width(), activeArray.height());
            this.mSizesLocked = false;
            Range<Integer> previewFpsRange = (Range) this.mTemplateSettings.get(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE);
            if (previewFpsRange != null) {
                setPreviewFpsRange(((Integer) previewFpsRange.getLower()).intValue(), ((Integer) previewFpsRange.getUpper()).intValue());
            }
            setPreviewSize(preview);
            setPhotoSize(photo);
            this.mJpegCompressQuality = ((Byte) queryTemplateDefaultOrMakeOneUp(CaptureRequest.JPEG_QUALITY, Byte.valueOf((byte) 0))).byteValue();
            this.mCurrentZoomRatio = 1.0f;
            this.mExposureCompensationIndex = ((Integer) queryTemplateDefaultOrMakeOneUp(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, Integer.valueOf(0))).intValue();
            this.mCurrentFlashMode = flashModeFromRequest();
            Integer currentFocusMode = (Integer) this.mTemplateSettings.get(CaptureRequest.CONTROL_AF_MODE);
            if (currentFocusMode != null) {
                this.mCurrentFocusMode = AndroidCamera2Capabilities.focusModeFromInt(currentFocusMode.intValue());
            }
            Integer currentSceneMode = (Integer) this.mTemplateSettings.get(CaptureRequest.CONTROL_SCENE_MODE);
            if (currentSceneMode != null) {
                this.mCurrentSceneMode = AndroidCamera2Capabilities.sceneModeFromInt(currentSceneMode.intValue());
            }
            Integer whiteBalance = (Integer) this.mTemplateSettings.get(CaptureRequest.CONTROL_AWB_MODE);
            if (whiteBalance != null) {
                this.mWhiteBalance = AndroidCamera2Capabilities.whiteBalanceFromInt(whiteBalance.intValue());
            }
            boolean z = true;
            if (((Integer) queryTemplateDefaultOrMakeOneUp(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, Integer.valueOf(0))).intValue() != 1) {
                z = false;
            }
            this.mVideoStabilizationEnabled = z;
            this.mAutoExposureLocked = ((Boolean) queryTemplateDefaultOrMakeOneUp(CaptureRequest.CONTROL_AE_LOCK, Boolean.valueOf(false))).booleanValue();
            this.mAutoWhiteBalanceLocked = ((Boolean) queryTemplateDefaultOrMakeOneUp(CaptureRequest.CONTROL_AWB_LOCK, Boolean.valueOf(false))).booleanValue();
            Size exifThumbnailSize = (Size) this.mTemplateSettings.get(CaptureRequest.JPEG_THUMBNAIL_SIZE);
            if (exifThumbnailSize != null) {
                this.mExifThumbnailSize = new Size(exifThumbnailSize.getWidth(), exifThumbnailSize.getHeight());
            }
        } else {
            throw new NullPointerException("activeArray must not be null");
        }
    }

    public AndroidCamera2Settings(AndroidCamera2Settings other) {
        super(other);
        this.mTemplateSettings = other.mTemplateSettings;
        this.mRequestSettings = new Camera2RequestSettingsSet(other.mRequestSettings);
        this.mActiveArray = other.mActiveArray;
        this.mCropRectangle = new Rect(other.mCropRectangle);
    }

    public CameraSettings copy() {
        return new AndroidCamera2Settings(this);
    }

    private <T> T queryTemplateDefaultOrMakeOneUp(Key<T> key, T defaultDefault) {
        T val = this.mTemplateSettings.get(key);
        if (val != null) {
            return val;
        }
        this.mTemplateSettings.set(key, defaultDefault);
        return defaultDefault;
    }

    private FlashMode flashModeFromRequest() {
        Integer autoExposure = (Integer) this.mTemplateSettings.get(CaptureRequest.CONTROL_AE_MODE);
        if (autoExposure != null) {
            switch (autoExposure.intValue()) {
                case 1:
                    return FlashMode.OFF;
                case 2:
                    return FlashMode.AUTO;
                case 3:
                    if (((Integer) this.mTemplateSettings.get(CaptureRequest.FLASH_MODE)).intValue() == 2) {
                        return FlashMode.TORCH;
                    }
                    return FlashMode.ON;
                case 4:
                    return FlashMode.RED_EYE;
            }
        }
        return null;
    }

    public void setZoomRatio(float ratio) {
        super.setZoomRatio(ratio);
        this.mCropRectangle.set(0, 0, toIntConstrained((double) (((float) this.mActiveArray.width()) / this.mCurrentZoomRatio), 0, this.mActiveArray.width()), toIntConstrained((double) (((float) this.mActiveArray.height()) / this.mCurrentZoomRatio), 0, this.mActiveArray.height()));
        this.mCropRectangle.offsetTo((this.mActiveArray.width() - this.mCropRectangle.width()) / 2, (this.mActiveArray.height() - this.mCropRectangle.height()) / 2);
        this.mVisiblePreviewRectangle = effectiveCropRectFromRequested(this.mCropRectangle, this.mCurrentPreviewSize);
    }

    private boolean matchesTemplateDefault(Key<?> setting) {
        boolean z = false;
        if (setting == CaptureRequest.CONTROL_AE_REGIONS) {
            if (this.mMeteringAreas.size() == 0) {
                z = true;
            }
            return z;
        } else if (setting == CaptureRequest.CONTROL_AF_REGIONS) {
            if (this.mFocusAreas.size() == 0) {
                z = true;
            }
            return z;
        } else if (setting == CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE) {
            Range<Integer> defaultFpsRange = (Range) this.mTemplateSettings.get(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE);
            if ((this.mPreviewFpsRangeMin == 0 && this.mPreviewFpsRangeMax == 0) || (defaultFpsRange != null && this.mPreviewFpsRangeMin == ((Integer) defaultFpsRange.getLower()).intValue() && this.mPreviewFpsRangeMax == ((Integer) defaultFpsRange.getUpper()).intValue())) {
                z = true;
            }
            return z;
        } else if (setting == CaptureRequest.JPEG_QUALITY) {
            return Objects.equals(Byte.valueOf(this.mJpegCompressQuality), this.mTemplateSettings.get(CaptureRequest.JPEG_QUALITY));
        } else {
            if (setting == CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION) {
                return Objects.equals(Integer.valueOf(this.mExposureCompensationIndex), this.mTemplateSettings.get(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION));
            }
            if (setting == CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE) {
                Integer videoStabilization = (Integer) this.mTemplateSettings.get(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE);
                if ((videoStabilization != null && this.mVideoStabilizationEnabled && videoStabilization.intValue() == 1) || (!this.mVideoStabilizationEnabled && videoStabilization.intValue() == 0)) {
                    z = true;
                }
                return z;
            } else if (setting == CaptureRequest.CONTROL_AE_LOCK) {
                return Objects.equals(Boolean.valueOf(this.mAutoExposureLocked), this.mTemplateSettings.get(CaptureRequest.CONTROL_AE_LOCK));
            } else {
                if (setting == CaptureRequest.CONTROL_AWB_LOCK) {
                    return Objects.equals(Boolean.valueOf(this.mAutoWhiteBalanceLocked), this.mTemplateSettings.get(CaptureRequest.CONTROL_AWB_LOCK));
                }
                if (setting != CaptureRequest.JPEG_THUMBNAIL_SIZE) {
                    Log.w(TAG, "Settings implementation checked default of unhandled option key");
                    return true;
                } else if (this.mExifThumbnailSize == null) {
                    return false;
                } else {
                    Size defaultThumbnailSize = (Size) this.mTemplateSettings.get(CaptureRequest.JPEG_THUMBNAIL_SIZE);
                    if ((this.mExifThumbnailSize.width() == 0 && this.mExifThumbnailSize.height() == 0) || (defaultThumbnailSize != null && this.mExifThumbnailSize.width() == defaultThumbnailSize.getWidth() && this.mExifThumbnailSize.height() == defaultThumbnailSize.getHeight())) {
                        z = true;
                    }
                    return z;
                }
            }
        }
    }

    private <T> void updateRequestSettingOrForceToDefault(Key<T> setting, T possibleChoice) {
        this.mRequestSettings.set(setting, matchesTemplateDefault(setting) ? null : possibleChoice);
    }

    public Camera2RequestSettingsSet getRequestSettings() {
        int i;
        Object valueOf;
        updateRequestSettingOrForceToDefault(CaptureRequest.CONTROL_AE_REGIONS, legacyAreasToMeteringRectangles(this.mMeteringAreas));
        updateRequestSettingOrForceToDefault(CaptureRequest.CONTROL_AF_REGIONS, legacyAreasToMeteringRectangles(this.mFocusAreas));
        updateRequestSettingOrForceToDefault(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range(Integer.valueOf(this.mPreviewFpsRangeMin), Integer.valueOf(this.mPreviewFpsRangeMax)));
        updateRequestSettingOrForceToDefault(CaptureRequest.JPEG_QUALITY, Byte.valueOf(this.mJpegCompressQuality));
        this.mRequestSettings.set(CaptureRequest.SCALER_CROP_REGION, this.mCropRectangle);
        updateRequestSettingOrForceToDefault(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, Integer.valueOf(this.mExposureCompensationIndex));
        updateRequestExposureTimeAndISO();
        updateRequestFlashMode();
        updateRequestFocusMode();
        updateRequestSceneMode();
        updateRequestWhiteBalance();
        updateLiveBokehEnabled();
        updateLiveBokehLevel();
        updateAntibandingValue();
        Key key = CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE;
        if (this.mVideoStabilizationEnabled) {
            i = 1;
        } else {
            i = 0;
        }
        updateRequestSettingOrForceToDefault(key, Integer.valueOf(i));
        Camera2RequestSettingsSet camera2RequestSettingsSet = this.mRequestSettings;
        Key key2 = CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE;
        if (this.mVideoStabilizationEnabled) {
            valueOf = Integer.valueOf(0);
        } else {
            valueOf = null;
        }
        camera2RequestSettingsSet.set(key2, valueOf);
        updateRequestSettingOrForceToDefault(CaptureRequest.CONTROL_AE_LOCK, Boolean.valueOf(this.mAutoExposureLocked));
        updateRequestSettingOrForceToDefault(CaptureRequest.CONTROL_AWB_LOCK, Boolean.valueOf(this.mAutoWhiteBalanceLocked));
        updateRequestGpsData();
        if (this.mExifThumbnailSize != null) {
            updateRequestSettingOrForceToDefault(CaptureRequest.JPEG_THUMBNAIL_SIZE, new Size(this.mExifThumbnailSize.width(), this.mExifThumbnailSize.height()));
        } else {
            updateRequestSettingOrForceToDefault(CaptureRequest.JPEG_THUMBNAIL_SIZE, null);
        }
        return this.mRequestSettings;
    }

    private void updateBokehPoint() {
        Float[] point = new Float[2];
        float focusX = 0.0f;
        float focusY = 0.0f;
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mRTDofMode = ");
        stringBuilder.append(this.mRTDofMode);
        Log.d(tag, stringBuilder.toString());
        if (this.mRTDofMode == 2) {
            focusX = 2.0f;
            focusY = 2.0f;
        } else if (this.mRTDofMode == 1) {
            focusX = 1.0f;
            focusY = 1.0f;
        }
        point[0] = Float.valueOf(focusX);
        point[1] = Float.valueOf(focusY);
        this.mRequestSettings.set(AndroidCamera2AgentImpl.bokeh_point, point);
    }

    private void updateLiveBokehEnabled() {
        this.mRequestSettings.set(AndroidCamera2AgentImpl.bokeh_enable, Boolean.valueOf(this.mLiveBokehEnabled));
        if (this.mRTDofMode != 0) {
            updateBokehPoint();
        }
    }

    private void updateLiveBokehLevel() {
        this.mRequestSettings.set(AndroidCamera2AgentImpl.bokeh_blur_level, Integer.valueOf(getLiveBokehLevel()));
    }

    private MeteringRectangle[] legacyAreasToMeteringRectangles(List<Area> reference) {
        MeteringRectangle[] transformed = null;
        if (reference.size() > 0) {
            transformed = new MeteringRectangle[reference.size()];
            int index = 0;
            while (index < reference.size()) {
                Area source = (Area) reference.get(index);
                Rect rectangle = source.rect;
                double oldLeft = ((double) (rectangle.left + 1000)) / 2000.0d;
                double oldTop = ((double) (rectangle.top + 1000)) / 2000.0d;
                double oldRight = ((double) (rectangle.right + 1000)) / 2000.0d;
                int index2 = index;
                double oldBottom = ((double) (rectangle.bottom + 1000)) / 2000.0d;
                Area source2 = source;
                int left = toIntConstrained(((double) this.mCropRectangle.width()) * oldLeft, 0, this.mCropRectangle.width() - 1) + this.mCropRectangle.left;
                int top = this.mCropRectangle.top + toIntConstrained(((double) this.mCropRectangle.height()) * oldTop, null, this.mCropRectangle.height() - 1);
                int i = left;
                int i2 = top;
                transformed[index2] = new MeteringRectangle(i, i2, (this.mCropRectangle.left + toIntConstrained(((double) this.mCropRectangle.width()) * oldRight, Camera2ParamsFragment.TARGET_EV, this.mCropRectangle.width() - 1)) - left, (this.mCropRectangle.top + toIntConstrained(((double) this.mCropRectangle.height()) * oldBottom, 0, this.mCropRectangle.height() - 1)) - top, source2.weight);
                index = index2 + 1;
            }
        }
        return transformed;
    }

    private int toIntConstrained(double original, int min, int max) {
        return (int) Math.min(Math.max(original, (double) min), (double) max);
    }

    private void updateAntibandingValue() {
        int antibandingMode = 4;
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mAntibanding = ");
        stringBuilder.append(this.mAntibanding);
        Log.d(tag, stringBuilder.toString());
        if (this.mAntibanding != null) {
            String str = this.mAntibanding;
            Object obj = -1;
            int hashCode = str.hashCode();
            if (hashCode != 1628397) {
                if (hashCode != 1658188) {
                    if (hashCode == 3005871 && str.equals("auto")) {
                        obj = null;
                    }
                } else if (str.equals("60hz")) {
                    obj = 2;
                }
            } else if (str.equals("50hz")) {
                obj = 1;
            }
            switch (obj) {
                case null:
                    antibandingMode = 4;
                    break;
                case 1:
                    antibandingMode = 4;
                    break;
                case 2:
                    antibandingMode = 5;
                    break;
                default:
                    antibandingMode = 4;
                    break;
            }
        }
        tag = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("mRequestSettings set antibandingMode = ");
        stringBuilder.append(antibandingMode);
        Log.d(tag, stringBuilder.toString());
        this.mRequestSettings.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, Integer.valueOf(antibandingMode));
    }

    private void updateRequestExposureTimeAndISO() {
        boolean isAutoIso = this.mIso.equals("auto");
        boolean isAutoEt = "0".equals(this.mExposureTime);
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Pro adjust mContinuousIso = ");
        stringBuilder.append(this.mContinuousIso);
        stringBuilder.append(" mExposureTime = ");
        stringBuilder.append(this.mExposureTime);
        Log.d(tag, stringBuilder.toString());
        tag = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Pro adjust isAutoIso = ");
        stringBuilder.append(isAutoIso);
        stringBuilder.append(" isAutoEt = ");
        stringBuilder.append(isAutoEt);
        Log.d(tag, stringBuilder.toString());
        this.mRequestSettings.set(AndroidCamera2AgentImpl.SELECT_PRIORITY, null);
        this.mRequestSettings.set(AndroidCamera2AgentImpl.ISO_EXP, null);
        this.mRequestSettings.set(CaptureRequest.SENSOR_EXPOSURE_TIME, null);
        this.mRequestSettings.set(CaptureRequest.SENSOR_SENSITIVITY, null);
        StringBuilder stringBuilder2;
        if (!isAutoEt && !isAutoIso) {
            Log.d(TAG, "Pro adjust Both");
            this.mRequestSettings.set(CaptureRequest.SENSOR_SENSITIVITY, Integer.valueOf(this.mContinuousIso));
            this.mRequestSettings.set(CaptureRequest.SENSOR_EXPOSURE_TIME, Long.valueOf(Float.valueOf(this.mExposureTime).longValue()));
            this.mRequestSettings.set(CaptureRequest.CONTROL_AE_MODE, Integer.valueOf(0));
        } else if (isAutoEt && !isAutoIso) {
            long intValue = (long) ((Integer) KEY_ISO_INDEX.get(String.valueOf(this.mContinuousIso))).intValue();
            Tag tag2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Pro adjust ISO intValue = ");
            stringBuilder2.append(intValue);
            Log.d(tag2, stringBuilder2.toString());
            this.mRequestSettings.set(AndroidCamera2AgentImpl.SELECT_PRIORITY, Integer.valueOf(0));
            this.mRequestSettings.set(AndroidCamera2AgentImpl.ISO_EXP, Long.valueOf(intValue));
        } else if (isAutoEt || !isAutoIso) {
            Log.d(TAG, "Pro adjust null");
            if (this.mIsAdjustEt) {
                this.mRequestSettings.set(AndroidCamera2AgentImpl.SELECT_PRIORITY, Integer.valueOf(1));
            } else {
                this.mRequestSettings.set(AndroidCamera2AgentImpl.SELECT_PRIORITY, Integer.valueOf(0));
            }
            this.mRequestSettings.set(AndroidCamera2AgentImpl.ISO_EXP, Long.valueOf(0));
        } else {
            Tag tag3 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Pro adjust Et = ");
            stringBuilder2.append(Float.valueOf(this.mExposureTime).longValue());
            Log.d(tag3, stringBuilder2.toString());
            this.mRequestSettings.set(AndroidCamera2AgentImpl.SELECT_PRIORITY, Integer.valueOf(1));
            this.mRequestSettings.set(AndroidCamera2AgentImpl.ISO_EXP, Long.valueOf(Float.valueOf(this.mExposureTime).longValue()));
        }
    }

    private void updateRequestFlashMode() {
        Tag tag;
        StringBuilder stringBuilder;
        Integer aeMode = Integer.valueOf(0);
        Integer flashMode = Integer.valueOf(0);
        boolean isAutoEt = "0".equals(this.mExposureTime);
        if (this.mCurrentFlashMode != null && isAutoEt) {
            switch (this.mCurrentFlashMode) {
                case AUTO:
                    aeMode = Integer.valueOf(2);
                    break;
                case OFF:
                    aeMode = Integer.valueOf(1);
                    flashMode = Integer.valueOf(0);
                    break;
                case ON:
                    aeMode = Integer.valueOf(3);
                    flashMode = Integer.valueOf(1);
                    break;
                case TORCH:
                    flashMode = Integer.valueOf(2);
                    break;
                case RED_EYE:
                    aeMode = Integer.valueOf(4);
                    break;
                default:
                    tag = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to convert to API 2 flash mode: ");
                    stringBuilder.append(this.mCurrentFlashMode);
                    Log.w(tag, stringBuilder.toString());
                    break;
            }
        }
        tag = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("[AndroidCamera2Settings] updateRequestFlashMode aeMode = ");
        stringBuilder.append(aeMode);
        stringBuilder.append(" flashMode = ");
        stringBuilder.append(flashMode);
        Log.d(tag, stringBuilder.toString());
        this.mRequestSettings.set(CaptureRequest.CONTROL_AE_MODE, aeMode);
        this.mRequestSettings.set(CaptureRequest.FLASH_MODE, flashMode);
    }

    private void updateRequestFocusMode() {
        Integer mode = null;
        if (this.mCurrentFocusMode != null) {
            switch (this.mCurrentFocusMode) {
                case AUTO:
                    mode = Integer.valueOf(1);
                    break;
                case CONTINUOUS_PICTURE:
                    mode = Integer.valueOf(4);
                    break;
                case CONTINUOUS_VIDEO:
                    mode = Integer.valueOf(3);
                    break;
                case EXTENDED_DOF:
                    mode = Integer.valueOf(5);
                    break;
                case FIXED:
                    this.mRequestSettings.set(CaptureRequest.LENS_FOCUS_DISTANCE, Float.valueOf(this.mManualFocusPosition));
                    mode = Integer.valueOf(0);
                    Tag tag = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("[AndroidCamera2Settings] updateRequestFocusMode LENS_FOCUS_DISTANCE = ");
                    stringBuilder.append(this.mManualFocusPosition);
                    stringBuilder.append(" afmode = ");
                    stringBuilder.append(0);
                    Log.d(tag, stringBuilder.toString());
                    break;
                case MACRO:
                    mode = Integer.valueOf(2);
                    break;
                default:
                    Tag tag2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unable to convert to API 2 focus mode: ");
                    stringBuilder2.append(this.mCurrentFocusMode);
                    Log.w(tag2, stringBuilder2.toString());
                    break;
            }
        }
        this.mRequestSettings.set(CaptureRequest.CONTROL_AF_MODE, mode);
    }

    private void updateRequestSceneMode() {
        Tag tag;
        StringBuilder stringBuilder;
        Integer mode = null;
        if (this.mCurrentSceneMode != null) {
            switch (this.mCurrentSceneMode) {
                case AUTO:
                    if (!this.mIsProMode) {
                        mode = Integer.valueOf(1);
                        break;
                    } else {
                        mode = Integer.valueOf(0);
                        break;
                    }
                case ACTION:
                    mode = Integer.valueOf(2);
                    break;
                case BARCODE:
                    mode = Integer.valueOf(16);
                    break;
                case BEACH:
                    mode = Integer.valueOf(8);
                    break;
                case CANDLELIGHT:
                    mode = Integer.valueOf(15);
                    break;
                case FIREWORKS:
                    mode = Integer.valueOf(12);
                    break;
                case HDR:
                    if (!this.mIsProMode) {
                        mode = Integer.valueOf(1);
                        break;
                    } else {
                        mode = Integer.valueOf(LegacyVendorTags.CONTROL_SCENE_MODE_HDR);
                        break;
                    }
                case LANDSCAPE:
                    mode = Integer.valueOf(4);
                    break;
                case NIGHT:
                    mode = Integer.valueOf(5);
                    break;
                case PARTY:
                    mode = Integer.valueOf(14);
                    break;
                case PORTRAIT:
                    mode = Integer.valueOf(3);
                    break;
                case SNOW:
                    mode = Integer.valueOf(9);
                    break;
                case SPORTS:
                    mode = Integer.valueOf(13);
                    break;
                case STEADYPHOTO:
                    mode = Integer.valueOf(11);
                    break;
                case SUNSET:
                    mode = Integer.valueOf(10);
                    break;
                case THEATRE:
                    mode = Integer.valueOf(7);
                    break;
                default:
                    tag = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to convert to API 2 scene mode: ");
                    stringBuilder.append(this.mCurrentSceneMode);
                    Log.w(tag, stringBuilder.toString());
                    break;
            }
        }
        tag = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("mode = ");
        stringBuilder.append(mode);
        Log.d(tag, stringBuilder.toString());
        this.mRequestSettings.set(CaptureRequest.CONTROL_SCENE_MODE, mode);
        if (mode != null && 1 == mode.intValue()) {
            this.mRequestSettings.set(CaptureRequest.CONTROL_MODE, Integer.valueOf(2));
        }
    }

    private void updateRequestWhiteBalance() {
        Integer mode = null;
        if (this.mWhiteBalance != null) {
            switch (this.mWhiteBalance) {
                case AUTO:
                    mode = Integer.valueOf(1);
                    break;
                case CLOUDY_DAYLIGHT:
                    mode = Integer.valueOf(6);
                    break;
                case DAYLIGHT:
                    mode = Integer.valueOf(5);
                    break;
                case FLUORESCENT:
                    mode = Integer.valueOf(3);
                    break;
                case INCANDESCENT:
                    mode = Integer.valueOf(2);
                    break;
                case SHADE:
                    mode = Integer.valueOf(8);
                    break;
                case TWILIGHT:
                    mode = Integer.valueOf(7);
                    break;
                case WARM_FLUORESCENT:
                    mode = Integer.valueOf(4);
                    break;
                default:
                    Tag tag = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to convert to API 2 white balance: ");
                    stringBuilder.append(this.mWhiteBalance);
                    Log.w(tag, stringBuilder.toString());
                    break;
            }
        }
        this.mRequestSettings.set(CaptureRequest.CONTROL_AWB_MODE, mode);
    }

    private void updateRequestGpsData() {
        if (this.mGpsData == null || this.mGpsData.processingMethod == null) {
            this.mRequestSettings.set(CaptureRequest.JPEG_GPS_LOCATION, null);
            return;
        }
        Location location = new Location(this.mGpsData.processingMethod);
        location.setTime(this.mGpsData.timeStamp);
        location.setAltitude(this.mGpsData.altitude);
        location.setLatitude(this.mGpsData.latitude);
        location.setLongitude(this.mGpsData.longitude);
        this.mRequestSettings.set(CaptureRequest.JPEG_GPS_LOCATION, location);
    }

    private static Rect effectiveCropRectFromRequested(Rect requestedCrop, Size previewSize) {
        float cropHeight;
        float cropWidth;
        float aspectRatioPreview = (((float) previewSize.width()) * 1.0f) / ((float) previewSize.height());
        if (aspectRatioPreview < (((float) requestedCrop.width()) * 1.0f) / ((float) requestedCrop.height())) {
            cropHeight = (float) requestedCrop.height();
            cropWidth = cropHeight * aspectRatioPreview;
        } else {
            cropWidth = (float) requestedCrop.width();
            cropHeight = cropWidth / aspectRatioPreview;
        }
        Matrix translateMatrix = new Matrix();
        RectF cropRect = new RectF(0.0f, 0.0f, cropWidth, cropHeight);
        translateMatrix.setTranslate(requestedCrop.exactCenterX(), requestedCrop.exactCenterY());
        translateMatrix.postTranslate(-cropRect.centerX(), -cropRect.centerY());
        translateMatrix.mapRect(cropRect);
        Rect result = new Rect();
        cropRect.roundOut(result);
        return result;
    }
}
