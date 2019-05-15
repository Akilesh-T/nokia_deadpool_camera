package com.android.ex.camera2.portability;

import com.android.ex.camera2.portability.debug.Log;
import com.android.ex.camera2.portability.debug.Log.Tag;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class CameraCapabilities {
    public static final String FOCUS_MODE_MANUAL = "manual";
    public static final String KEY_AEC_SETTLED = "cur-exposure-settled";
    public static final String KEY_AUTO_ISO = "auto";
    public static final String KEY_CAMERA_AIS = "mfb";
    public static final String KEY_HSR_SIZES = "hfr-size-values";
    public static final String KEY_INSTANT_AEC = "instant-aec";
    public static final String KEY_MANUAL_ISO = "manual";
    public static final String KEY_VIDEO_HIGH_FRAME_RATE_MODES = "video-hfr-values";
    public static final String KEY_VIDEO_HSR = "video-hsr";
    public static final String KEY_VIDEO_SIZE = "video-size";
    private static Tag TAG = new Tag("CamCapabs");
    public static final String VALUE_INSTANT_AEC_OFF = "0";
    public static final String VALUE_INSTANT_AEC_ON = "1";
    protected static final float ZOOM_RATIO_UNZOOMED = 1.0f;
    protected float mExposureCompensationStep;
    protected float mHorizontalViewAngle;
    protected int mMaxExposureCompensation;
    protected String mMaxExposureTime = "0.0";
    protected int mMaxFocusScale;
    protected int mMaxISO;
    protected int mMaxNumOfFacesSupported;
    protected int mMaxNumOfFocusAreas;
    protected int mMaxNumOfMeteringArea;
    protected float mMaxZoomRatio;
    protected int mMinExposureCompensation;
    protected String mMinExposureTime = "0.0";
    protected float mMinFocusDistsnce;
    protected int mMinFocusScale;
    protected int mMinISO;
    protected Size mPreferredPreviewSizeForVideo;
    private final Stringifier mStringifier;
    protected List<String> mSupportedAntibanding = new ArrayList();
    protected final EnumSet<Feature> mSupportedFeatures = EnumSet.noneOf(Feature.class);
    protected final EnumSet<FlashMode> mSupportedFlashModes = EnumSet.noneOf(FlashMode.class);
    protected final EnumSet<FocusMode> mSupportedFocusModes = EnumSet.noneOf(FocusMode.class);
    protected final ArrayList<Size> mSupportedHsrSizes = new ArrayList();
    protected List<String> mSupportedIsoValues = new ArrayList();
    protected final TreeSet<Integer> mSupportedPhotoFormats = new TreeSet();
    protected final ArrayList<Size> mSupportedPhotoSizes = new ArrayList();
    protected final TreeSet<Integer> mSupportedPreviewFormats = new TreeSet();
    protected final ArrayList<int[]> mSupportedPreviewFpsRange = new ArrayList();
    protected final ArrayList<Size> mSupportedPreviewSizes = new ArrayList();
    protected final EnumSet<SceneMode> mSupportedSceneModes = EnumSet.noneOf(SceneMode.class);
    protected final ArrayList<String> mSupportedVideoHighFrameRates = new ArrayList();
    protected final ArrayList<Size> mSupportedVideoSizes = new ArrayList();
    protected final EnumSet<WhiteBalance> mSupportedWhiteBalances = EnumSet.noneOf(WhiteBalance.class);
    protected float mVerticalViewAngle;
    protected boolean mZslSupported;

    public enum Feature {
        ZOOM,
        VIDEO_SNAPSHOT,
        FOCUS_AREA,
        METERING_AREA,
        AUTO_EXPOSURE_LOCK,
        AUTO_WHITE_BALANCE_LOCK,
        VIDEO_STABILIZATION
    }

    public enum FlashMode {
        NO_FLASH,
        AUTO,
        OFF,
        ON,
        TORCH,
        RED_EYE
    }

    public enum FocusMode {
        AUTO,
        CONTINUOUS_PICTURE,
        CONTINUOUS_VIDEO,
        EXTENDED_DOF,
        FIXED,
        INFINITY,
        MACRO,
        MANUAL;

        public String toString() {
            if (this == MANUAL) {
                return super.name().toLowerCase();
            }
            return super.name();
        }
    }

    public enum SceneMode {
        NO_SCENE_MODE,
        AUTO,
        ACTION,
        BARCODE,
        BEACH,
        CANDLELIGHT,
        FIREWORKS,
        HDR,
        LANDSCAPE,
        NIGHT,
        NIGHT_PORTRAIT,
        PARTY,
        PORTRAIT,
        SNOW,
        SPORTS,
        STEADYPHOTO,
        SUNSET,
        THEATRE
    }

    public static class Stringifier {
        private static String toApiCase(String enumCase) {
            return enumCase.toLowerCase(Locale.US).replaceAll("_", "-");
        }

        private static String toEnumCase(String apiCase) {
            return apiCase.toUpperCase(Locale.US).replaceAll("-", "_");
        }

        public String stringify(FocusMode focus) {
            return toApiCase(focus.toString());
        }

        public FocusMode focusModeFromString(String val) {
            if (val == null) {
                return FocusMode.values()[0];
            }
            try {
                return FocusMode.valueOf(toEnumCase(val));
            } catch (IllegalArgumentException e) {
                return FocusMode.values()[0];
            }
        }

        public String stringify(FlashMode flash) {
            return toApiCase(flash.name());
        }

        public FlashMode flashModeFromString(String val) {
            if (val == null) {
                return FlashMode.values()[0];
            }
            try {
                return FlashMode.valueOf(toEnumCase(val));
            } catch (IllegalArgumentException e) {
                return FlashMode.values()[0];
            }
        }

        public String stringify(SceneMode scene) {
            return toApiCase(scene.name());
        }

        public SceneMode sceneModeFromString(String val) {
            if (val == null) {
                return SceneMode.values()[0];
            }
            try {
                return SceneMode.valueOf(toEnumCase(val));
            } catch (IllegalArgumentException e) {
                return SceneMode.values()[0];
            }
        }

        public String stringify(WhiteBalance wb) {
            return toApiCase(wb.name());
        }

        public WhiteBalance whiteBalanceFromString(String val) {
            if (val == null) {
                return WhiteBalance.values()[0];
            }
            try {
                return WhiteBalance.valueOf(toEnumCase(val));
            } catch (IllegalArgumentException e) {
                return WhiteBalance.values()[0];
            }
        }
    }

    public enum WhiteBalance {
        AUTO,
        CLOUDY_DAYLIGHT,
        DAYLIGHT,
        FLUORESCENT,
        INCANDESCENT,
        SHADE,
        TWILIGHT,
        WARM_FLUORESCENT
    }

    CameraCapabilities(Stringifier stringifier) {
        this.mStringifier = stringifier;
    }

    public CameraCapabilities(CameraCapabilities src) {
        this.mSupportedPreviewFpsRange.addAll(src.mSupportedPreviewFpsRange);
        this.mSupportedPreviewSizes.addAll(src.mSupportedPreviewSizes);
        this.mSupportedPreviewFormats.addAll(src.mSupportedPreviewFormats);
        this.mSupportedVideoSizes.addAll(src.mSupportedVideoSizes);
        this.mSupportedPhotoSizes.addAll(src.mSupportedPhotoSizes);
        this.mSupportedPhotoFormats.addAll(src.mSupportedPhotoFormats);
        this.mSupportedSceneModes.addAll(src.mSupportedSceneModes);
        this.mSupportedFlashModes.addAll(src.mSupportedFlashModes);
        this.mSupportedFocusModes.addAll(src.mSupportedFocusModes);
        this.mSupportedWhiteBalances.addAll(src.mSupportedWhiteBalances);
        this.mSupportedFeatures.addAll(src.mSupportedFeatures);
        this.mSupportedVideoHighFrameRates.addAll(src.mSupportedVideoHighFrameRates);
        this.mSupportedHsrSizes.addAll(src.mSupportedHsrSizes);
        this.mPreferredPreviewSizeForVideo = src.mPreferredPreviewSizeForVideo;
        this.mMaxExposureCompensation = src.mMaxExposureCompensation;
        this.mMinExposureCompensation = src.mMinExposureCompensation;
        this.mZslSupported = src.mZslSupported;
        this.mExposureCompensationStep = src.mExposureCompensationStep;
        this.mMaxNumOfFacesSupported = src.mMaxNumOfFacesSupported;
        this.mMaxNumOfFocusAreas = src.mMaxNumOfFocusAreas;
        this.mMaxNumOfMeteringArea = src.mMaxNumOfMeteringArea;
        this.mMaxZoomRatio = src.mMaxZoomRatio;
        this.mHorizontalViewAngle = src.mHorizontalViewAngle;
        this.mVerticalViewAngle = src.mVerticalViewAngle;
        this.mStringifier = src.mStringifier;
        this.mMinISO = src.mMinISO;
        this.mMaxISO = src.mMaxISO;
        this.mMinExposureTime = src.mMinExposureTime;
        this.mMaxExposureTime = src.mMaxExposureTime;
        this.mMinFocusScale = src.mMinFocusScale;
        this.mMaxFocusScale = src.mMaxFocusScale;
        this.mSupportedIsoValues.addAll(src.mSupportedIsoValues);
        this.mSupportedAntibanding.addAll(src.mSupportedAntibanding);
    }

    public float getHorizontalViewAngle() {
        return this.mHorizontalViewAngle;
    }

    public float getVerticalViewAngle() {
        return this.mVerticalViewAngle;
    }

    public Set<Integer> getSupportedPhotoFormats() {
        return new TreeSet(this.mSupportedPhotoFormats);
    }

    public Set<Integer> getSupportedPreviewFormats() {
        return new TreeSet(this.mSupportedPreviewFormats);
    }

    public List<Size> getSupportedPhotoSizes() {
        return new ArrayList(this.mSupportedPhotoSizes);
    }

    public final List<int[]> getSupportedPreviewFpsRange() {
        return new ArrayList(this.mSupportedPreviewFpsRange);
    }

    public final List<Size> getSupportedPreviewSizes() {
        return new ArrayList(this.mSupportedPreviewSizes);
    }

    public final Size getPreferredPreviewSizeForVideo() {
        return new Size(this.mPreferredPreviewSizeForVideo);
    }

    public final List<Size> getSupportedVideoSizes() {
        return new ArrayList(this.mSupportedVideoSizes);
    }

    public final List<String> getSupportedVideoHighFrameRates() {
        return new ArrayList(this.mSupportedVideoHighFrameRates);
    }

    public final List<Size> getSupportedHsrSizes() {
        return new ArrayList(this.mSupportedHsrSizes);
    }

    public final Set<SceneMode> getSupportedSceneModes() {
        return new HashSet(this.mSupportedSceneModes);
    }

    public final boolean supports(SceneMode scene) {
        return scene != null && this.mSupportedSceneModes.contains(scene);
    }

    public boolean supports(CameraSettings settings) {
        if (zoomCheck(settings) && exposureCheck(settings) && focusCheck(settings) && flashCheck(settings) && photoSizeCheck(settings) && previewSizeCheck(settings) && videoStabilizationCheck(settings)) {
            return true;
        }
        return false;
    }

    public final Set<FlashMode> getSupportedFlashModes() {
        return new HashSet(this.mSupportedFlashModes);
    }

    public final boolean supports(FlashMode flash) {
        return flash != null && this.mSupportedFlashModes.contains(flash);
    }

    public final Set<FocusMode> getSupportedFocusModes() {
        return new HashSet(this.mSupportedFocusModes);
    }

    public final boolean supports(FocusMode focus) {
        return focus != null && this.mSupportedFocusModes.contains(focus);
    }

    public final Set<WhiteBalance> getSupportedWhiteBalance() {
        return new HashSet(this.mSupportedWhiteBalances);
    }

    public boolean supports(WhiteBalance wb) {
        return wb != null && this.mSupportedWhiteBalances.contains(wb);
    }

    public final Set<Feature> getSupportedFeature() {
        return new HashSet(this.mSupportedFeatures);
    }

    public boolean supports(Feature ft) {
        return ft != null && this.mSupportedFeatures.contains(ft);
    }

    public float getMaxZoomRatio() {
        return this.mMaxZoomRatio;
    }

    public final int getMinExposureCompensation() {
        return this.mMinExposureCompensation;
    }

    public final boolean isZslSupported() {
        return this.mZslSupported;
    }

    public final int getMaxExposureCompensation() {
        return this.mMaxExposureCompensation;
    }

    public final float getExposureCompensationStep() {
        return this.mExposureCompensationStep;
    }

    public final int getMaxNumOfFacesSupported() {
        return this.mMaxNumOfFacesSupported;
    }

    public Stringifier getStringifier() {
        return this.mStringifier;
    }

    private boolean zoomCheck(CameraSettings settings) {
        float ratio = settings.getCurrentZoomRatio();
        if (supports(Feature.ZOOM)) {
            if (settings.getCurrentZoomRatio() > getMaxZoomRatio()) {
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Zoom ratio is not supported: ratio = ");
                stringBuilder.append(settings.getCurrentZoomRatio());
                Log.i(tag, stringBuilder.toString());
                return false;
            }
        } else if (ratio != 1.0f) {
            Log.i(TAG, "Zoom is not supported");
            return false;
        }
        return true;
    }

    private boolean exposureCheck(CameraSettings settings) {
        int index = settings.getExposureCompensationIndex();
        if (index <= getMaxExposureCompensation() && index >= getMinExposureCompensation()) {
            return true;
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Exposure compensation index is not supported. Min = ");
        stringBuilder.append(getMinExposureCompensation());
        stringBuilder.append(", max = ");
        stringBuilder.append(getMaxExposureCompensation());
        stringBuilder.append(", setting = ");
        stringBuilder.append(index);
        Log.i(tag, stringBuilder.toString());
        return false;
    }

    private boolean focusCheck(CameraSettings settings) {
        FocusMode focusMode = settings.getCurrentFocusMode();
        if (!supports(focusMode)) {
            if (supports(FocusMode.FIXED)) {
                Log.i(TAG, "Focus mode not supported... trying FIXED");
                settings.setFocusMode(FocusMode.FIXED);
            } else {
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Focus mode not supported:");
                stringBuilder.append(focusMode != null ? focusMode.name() : "null");
                Log.i(tag, stringBuilder.toString());
                return false;
            }
        }
        return true;
    }

    private boolean flashCheck(CameraSettings settings) {
        FlashMode flashMode = settings.getCurrentFlashMode();
        if (supports(flashMode)) {
            return true;
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Flash mode not supported:");
        stringBuilder.append(flashMode != null ? flashMode.name() : "null");
        Log.i(tag, stringBuilder.toString());
        return false;
    }

    private boolean photoSizeCheck(CameraSettings settings) {
        Size photoSize = settings.getCurrentPhotoSize();
        if (this.mSupportedPhotoSizes.contains(photoSize)) {
            return true;
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unsupported photo size:");
        stringBuilder.append(photoSize);
        Log.i(tag, stringBuilder.toString());
        return false;
    }

    private boolean videoSizeCheck(CameraSettings settings) {
        Size videoSize = settings.getCurrentVideoSize();
        if (this.mSupportedVideoSizes.contains(videoSize)) {
            return true;
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unsupported video size:");
        stringBuilder.append(videoSize);
        Log.i(tag, stringBuilder.toString());
        return false;
    }

    private boolean previewSizeCheck(CameraSettings settings) {
        Size previewSize = settings.getCurrentPreviewSize();
        if (this.mSupportedPreviewSizes.contains(previewSize)) {
            return true;
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unsupported preview size:");
        stringBuilder.append(previewSize);
        Log.i(tag, stringBuilder.toString());
        return false;
    }

    private boolean videoStabilizationCheck(CameraSettings settings) {
        if (!settings.isVideoStabilizationEnabled() || supports(Feature.VIDEO_STABILIZATION)) {
            return true;
        }
        Log.i(TAG, "Video stabilization is not supported");
        return false;
    }

    public final int getMinISO() {
        return this.mMinISO;
    }

    public final int getMaxIso() {
        return this.mMaxISO;
    }

    public final int getMinFocusScale() {
        return this.mMinFocusScale;
    }

    public final int getMaxFocusScale() {
        return this.mMaxFocusScale;
    }

    public final String getMinExposureTime() {
        return this.mMinExposureTime;
    }

    public final String getMaxExposureTime() {
        return this.mMaxExposureTime;
    }

    public final List<String> getSupportedIsoValues() {
        return this.mSupportedIsoValues;
    }

    public List<String> getSupportedAntibanding() {
        return this.mSupportedAntibanding;
    }
}
