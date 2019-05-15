package com.android.ex.camera2.portability;

import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.util.Range;
import android.util.Rational;
import com.android.ex.camera2.portability.CameraCapabilities.Feature;
import com.android.ex.camera2.portability.CameraCapabilities.FlashMode;
import com.android.ex.camera2.portability.CameraCapabilities.FocusMode;
import com.android.ex.camera2.portability.CameraCapabilities.SceneMode;
import com.android.ex.camera2.portability.CameraCapabilities.Stringifier;
import com.android.ex.camera2.portability.CameraCapabilities.WhiteBalance;
import com.android.ex.camera2.portability.debug.Log;
import com.android.ex.camera2.portability.debug.Log.Tag;
import java.util.Arrays;

public class AndroidCamera2Capabilities extends CameraCapabilities {
    private static Tag TAG = new Tag("AndCam2Capabs");

    @TargetApi(21)
    AndroidCamera2Capabilities(CameraCharacteristics p) {
        super(new Stringifier());
        StreamConfigurationMap s = (StreamConfigurationMap) p.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        for (Range<Integer> fpsRange : (Range[]) p.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)) {
            this.mSupportedPreviewFpsRange.add(new int[]{((Integer) fpsRange.getLower()).intValue(), ((Integer) fpsRange.getUpper()).intValue()});
        }
        this.mSupportedPreviewSizes.addAll(Size.buildListFromAndroidSizes(Arrays.asList(s.getOutputSizes(SurfaceTexture.class))));
        this.mSupportedPreviewSizes.addAll(0, Size.buildListFromAndroidSizes(Arrays.asList(s.getHighResolutionOutputSizes(256))));
        for (int format : s.getOutputFormats()) {
            this.mSupportedPreviewFormats.add(Integer.valueOf(format));
        }
        this.mSupportedVideoSizes.addAll(Size.buildListFromAndroidSizes(Arrays.asList(s.getOutputSizes(MediaRecorder.class))));
        this.mSupportedPhotoSizes.addAll(Size.buildListFromAndroidSizes(Arrays.asList(s.getOutputSizes(256))));
        this.mSupportedPhotoSizes.addAll(0, Size.buildListFromAndroidSizes(Arrays.asList(s.getHighResolutionOutputSizes(256))));
        this.mSupportedPhotoFormats.addAll(this.mSupportedPreviewFormats);
        buildSceneModes(p);
        buildFlashModes(p);
        buildFocusModes(p);
        buildWhiteBalances(p);
        Range<Integer> ecRange = (Range) p.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
        this.mMinExposureCompensation = ((Integer) ecRange.getLower()).intValue();
        this.mMaxExposureCompensation = ((Integer) ecRange.getUpper()).intValue();
        Rational ecStep = (Rational) p.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP);
        this.mExposureCompensationStep = ((float) ecStep.getNumerator()) / ((float) ecStep.getDenominator());
        this.mMinFocusDistsnce = ((Float) p.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)).floatValue();
        Range<Integer> isoRange = (Range) p.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
        this.mMaxISO = ((Integer) isoRange.getUpper()).intValue();
        this.mMinISO = ((Integer) isoRange.getLower()).intValue();
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mMinISO = ");
        stringBuilder.append(this.mMinISO);
        stringBuilder.append(" mMaxISO = ");
        stringBuilder.append(this.mMaxISO);
        Log.d(tag, stringBuilder.toString());
        Range<Long> etRange = (Range) p.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
        this.mMaxExposureTime = String.valueOf(etRange.getUpper());
        this.mMinExposureTime = String.valueOf(etRange.getLower());
        Tag tag2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("mMinExposureTime = ");
        stringBuilder2.append(this.mMinExposureTime);
        stringBuilder2.append(" mMaxExposureTime = ");
        stringBuilder2.append(this.mMaxExposureTime);
        Log.d(tag2, stringBuilder2.toString());
        this.mMaxNumOfFacesSupported = ((Integer) p.get(CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT)).intValue();
        this.mMaxNumOfMeteringArea = ((Integer) p.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE)).intValue();
        this.mMaxZoomRatio = ((Float) p.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)).floatValue();
        if (supports(FocusMode.AUTO)) {
            this.mMaxNumOfFocusAreas = ((Integer) p.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF)).intValue();
            if (this.mMaxNumOfFocusAreas > 0) {
                this.mSupportedFeatures.add(Feature.FOCUS_AREA);
            }
        }
        if (this.mMaxNumOfMeteringArea > 0) {
            this.mSupportedFeatures.add(Feature.METERING_AREA);
        }
        if (this.mMaxZoomRatio > 1.0f) {
            this.mSupportedFeatures.add(Feature.ZOOM);
        }
        this.mZslSupported = true;
    }

    @TargetApi(21)
    private void buildSceneModes(CameraCharacteristics p) {
        int[] scenes = (int[]) p.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES);
        if (scenes != null) {
            for (int scene : scenes) {
                SceneMode equiv = sceneModeFromInt(scene);
                if (equiv != null) {
                    this.mSupportedSceneModes.add(equiv);
                }
            }
        }
    }

    @TargetApi(21)
    private void buildFlashModes(CameraCharacteristics p) {
        this.mSupportedFlashModes.add(FlashMode.OFF);
        if (((Boolean) p.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)).booleanValue()) {
            this.mSupportedFlashModes.add(FlashMode.AUTO);
            this.mSupportedFlashModes.add(FlashMode.ON);
            this.mSupportedFlashModes.add(FlashMode.TORCH);
            for (int expose : (int[]) p.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES)) {
                if (expose == 4) {
                    this.mSupportedFlashModes.add(FlashMode.RED_EYE);
                }
            }
        }
    }

    @TargetApi(21)
    private void buildFocusModes(CameraCharacteristics p) {
        int[] focuses = (int[]) p.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
        if (focuses != null) {
            for (int focus : focuses) {
                FocusMode equiv = focusModeFromInt(focus);
                if (equiv != null) {
                    this.mSupportedFocusModes.add(equiv);
                }
            }
        }
    }

    @TargetApi(21)
    private void buildWhiteBalances(CameraCharacteristics p) {
        int[] bals = (int[]) p.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);
        if (bals != null) {
            for (int bal : bals) {
                WhiteBalance equiv = whiteBalanceFromInt(bal);
                if (equiv != null) {
                    this.mSupportedWhiteBalances.add(equiv);
                }
            }
        }
    }

    public static FocusMode focusModeFromInt(int fm) {
        switch (fm) {
            case 0:
                return FocusMode.FIXED;
            case 1:
                return FocusMode.AUTO;
            case 2:
                return FocusMode.MACRO;
            case 3:
                return FocusMode.CONTINUOUS_VIDEO;
            case 4:
                return FocusMode.CONTINUOUS_PICTURE;
            case 5:
                return FocusMode.EXTENDED_DOF;
            default:
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to convert from API 2 focus mode: ");
                stringBuilder.append(fm);
                Log.w(tag, stringBuilder.toString());
                return null;
        }
    }

    public static SceneMode sceneModeFromInt(int sm) {
        switch (sm) {
            case 0:
                return SceneMode.AUTO;
            case 2:
                return SceneMode.ACTION;
            case 3:
                return SceneMode.PORTRAIT;
            case 4:
                return SceneMode.LANDSCAPE;
            case 5:
                return SceneMode.NIGHT;
            case 7:
                return SceneMode.THEATRE;
            case 8:
                return SceneMode.BEACH;
            case 9:
                return SceneMode.SNOW;
            case 10:
                return SceneMode.SUNSET;
            case 11:
                return SceneMode.STEADYPHOTO;
            case 12:
                return SceneMode.FIREWORKS;
            case 13:
                return SceneMode.SPORTS;
            case 14:
                return SceneMode.PARTY;
            case 15:
                return SceneMode.CANDLELIGHT;
            case 16:
                return SceneMode.BARCODE;
            default:
                if (sm == LegacyVendorTags.CONTROL_SCENE_MODE_HDR) {
                    return SceneMode.HDR;
                }
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to convert from API 2 scene mode: ");
                stringBuilder.append(sm);
                Log.w(tag, stringBuilder.toString());
                return null;
        }
    }

    public static WhiteBalance whiteBalanceFromInt(int wb) {
        switch (wb) {
            case 1:
                return WhiteBalance.AUTO;
            case 2:
                return WhiteBalance.INCANDESCENT;
            case 3:
                return WhiteBalance.FLUORESCENT;
            case 4:
                return WhiteBalance.WARM_FLUORESCENT;
            case 5:
                return WhiteBalance.DAYLIGHT;
            case 6:
                return WhiteBalance.CLOUDY_DAYLIGHT;
            case 7:
                return WhiteBalance.TWILIGHT;
            case 8:
                return WhiteBalance.SHADE;
            default:
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to convert from API 2 white balance: ");
                stringBuilder.append(wb);
                Log.w(tag, stringBuilder.toString());
                return null;
        }
    }
}
