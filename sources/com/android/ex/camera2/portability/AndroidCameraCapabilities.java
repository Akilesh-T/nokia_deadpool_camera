package com.android.ex.camera2.portability;

import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.text.TextUtils.SimpleStringSplitter;
import android.text.TextUtils.StringSplitter;
import com.android.ex.camera2.portability.CameraCapabilities.Feature;
import com.android.ex.camera2.portability.CameraCapabilities.FlashMode;
import com.android.ex.camera2.portability.CameraCapabilities.FocusMode;
import com.android.ex.camera2.portability.CameraCapabilities.SceneMode;
import com.android.ex.camera2.portability.CameraCapabilities.Stringifier;
import com.android.ex.camera2.portability.CameraCapabilities.WhiteBalance;
import com.android.ex.camera2.portability.debug.Log.Tag;
import com.android.external.ExtendKey;
import com.android.external.ExtendParameters;
import com.hmdglobal.app.camera.util.CustomFields;
import com.hmdglobal.app.camera.util.CustomUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class AndroidCameraCapabilities extends CameraCapabilities {
    private static Tag TAG = new Tag("AndCamCapabs");
    public static final float ZOOM_MULTIPLIER = 100.0f;
    private FpsComparator mFpsComparator = new FpsComparator();
    private SizeComparator mSizeComparator = new SizeComparator();

    private static class FpsComparator implements Comparator<int[]> {
        private FpsComparator() {
        }

        public int compare(int[] fps1, int[] fps2) {
            int i;
            int i2;
            if (fps1[0] == fps2[0]) {
                i = fps1[1];
                i2 = fps2[1];
            } else {
                i = fps1[0];
                i2 = fps2[0];
            }
            return i - i2;
        }
    }

    private static class SizeComparator implements Comparator<Size> {
        private SizeComparator() {
        }

        public int compare(Size size1, Size size2) {
            if (size1.width() == size2.width()) {
                return size1.height() - size2.height();
            }
            return size1.width() - size2.width();
        }
    }

    AndroidCameraCapabilities(Parameters p) {
        super(new Stringifier());
        this.mMaxExposureCompensation = p.getMaxExposureCompensation();
        List<String> modes = ExtendParameters.getInstance(p).getSupportedZSLValues();
        boolean z = modes != null && modes.size() > 0;
        this.mZslSupported = z;
        this.mMinExposureCompensation = p.getMinExposureCompensation();
        this.mExposureCompensationStep = p.getExposureCompensationStep();
        this.mMaxNumOfFacesSupported = p.getMaxNumDetectedFaces();
        this.mMaxNumOfMeteringArea = p.getMaxNumMeteringAreas();
        this.mPreferredPreviewSizeForVideo = new Size(p.getPreferredPreviewSizeForVideo());
        this.mSupportedPreviewFormats.addAll(p.getSupportedPreviewFormats());
        this.mSupportedPhotoFormats.addAll(p.getSupportedPictureFormats());
        this.mHorizontalViewAngle = p.getHorizontalViewAngle();
        this.mVerticalViewAngle = p.getVerticalViewAngle();
        buildPreviewFpsRange(p);
        buildPreviewSizes(p);
        buildVideoSizes(p);
        buildPictureSizes(p);
        buildSceneModes(p);
        buildFlashModes(p);
        buildFocusModes(p);
        buildWhiteBalances(p);
        buildVideoHighFrameRates(p);
        buildHsrSizes(p);
        if (p.isZoomSupported()) {
            this.mMaxZoomRatio = ((float) ((Integer) p.getZoomRatios().get(p.getMaxZoom())).intValue()) / 100.0f;
            this.mSupportedFeatures.add(Feature.ZOOM);
        }
        if (p.isVideoSnapshotSupported()) {
            this.mSupportedFeatures.add(Feature.VIDEO_SNAPSHOT);
        }
        if (p.isVideoStabilizationSupported() || CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_HMD_EIS, false)) {
            this.mSupportedFeatures.add(Feature.VIDEO_STABILIZATION);
        }
        if (p.isAutoExposureLockSupported()) {
            this.mSupportedFeatures.add(Feature.AUTO_EXPOSURE_LOCK);
        }
        if (p.isAutoWhiteBalanceLockSupported()) {
            this.mSupportedFeatures.add(Feature.AUTO_WHITE_BALANCE_LOCK);
        }
        if (supports(FocusMode.AUTO)) {
            this.mMaxNumOfFocusAreas = p.getMaxNumFocusAreas();
            if (this.mMaxNumOfFocusAreas > 0) {
                this.mSupportedFeatures.add(Feature.FOCUS_AREA);
            }
        }
        if (this.mMaxNumOfMeteringArea > 0) {
            this.mSupportedFeatures.add(Feature.METERING_AREA);
        }
        buildManualModes(p);
        List<String> supportedAntibanding = p.getSupportedAntibanding();
        if (supportedAntibanding != null) {
            this.mSupportedAntibanding.addAll(supportedAntibanding);
        }
    }

    AndroidCameraCapabilities(AndroidCameraCapabilities src) {
        super((CameraCapabilities) src);
    }

    private void buildPreviewFpsRange(Parameters p) {
        List<int[]> supportedPreviewFpsRange = p.getSupportedPreviewFpsRange();
        if (supportedPreviewFpsRange != null) {
            this.mSupportedPreviewFpsRange.addAll(supportedPreviewFpsRange);
        }
        Collections.sort(this.mSupportedPreviewFpsRange, this.mFpsComparator);
    }

    private void buildPreviewSizes(Parameters p) {
        List<Size> supportedPreviewSizes = p.getSupportedPreviewSizes();
        if (supportedPreviewSizes != null) {
            for (Size s : supportedPreviewSizes) {
                this.mSupportedPreviewSizes.add(new Size(s.width, s.height));
            }
        }
        Collections.sort(this.mSupportedPreviewSizes, this.mSizeComparator);
    }

    private void buildVideoSizes(Parameters p) {
        List<Size> supportedVideoSizes = p.getSupportedVideoSizes();
        if (supportedVideoSizes != null) {
            for (Size s : supportedVideoSizes) {
                this.mSupportedVideoSizes.add(new Size(s.width, s.height));
            }
        }
        Collections.sort(this.mSupportedVideoSizes, this.mSizeComparator);
    }

    private void buildPictureSizes(Parameters p) {
        List<Size> supportedPictureSizes = p.getSupportedPictureSizes();
        if (supportedPictureSizes != null) {
            for (Size s : supportedPictureSizes) {
                this.mSupportedPhotoSizes.add(new Size(s.width, s.height));
            }
        }
        Collections.sort(this.mSupportedPhotoSizes, this.mSizeComparator);
    }

    private void buildSceneModes(Parameters p) {
        List<String> supportedSceneModes = p.getSupportedSceneModes();
        if (supportedSceneModes != null) {
            for (String scene : supportedSceneModes) {
                if ("auto".equals(scene)) {
                    this.mSupportedSceneModes.add(SceneMode.AUTO);
                } else if ("action".equals(scene)) {
                    this.mSupportedSceneModes.add(SceneMode.ACTION);
                } else if ("barcode".equals(scene)) {
                    this.mSupportedSceneModes.add(SceneMode.BARCODE);
                } else if ("beach".equals(scene)) {
                    this.mSupportedSceneModes.add(SceneMode.BEACH);
                } else if ("candlelight".equals(scene)) {
                    this.mSupportedSceneModes.add(SceneMode.CANDLELIGHT);
                } else if ("fireworks".equals(scene)) {
                    this.mSupportedSceneModes.add(SceneMode.FIREWORKS);
                } else if ("hdr".equals(scene)) {
                    this.mSupportedSceneModes.add(SceneMode.HDR);
                } else if ("landscape".equals(scene)) {
                    this.mSupportedSceneModes.add(SceneMode.LANDSCAPE);
                } else if ("night".equals(scene)) {
                    this.mSupportedSceneModes.add(SceneMode.NIGHT);
                } else if ("night-portrait".equals(scene)) {
                    this.mSupportedSceneModes.add(SceneMode.NIGHT_PORTRAIT);
                } else if ("party".equals(scene)) {
                    this.mSupportedSceneModes.add(SceneMode.PARTY);
                } else if ("portrait".equals(scene)) {
                    this.mSupportedSceneModes.add(SceneMode.PORTRAIT);
                } else if ("snow".equals(scene)) {
                    this.mSupportedSceneModes.add(SceneMode.SNOW);
                } else if ("sports".equals(scene)) {
                    this.mSupportedSceneModes.add(SceneMode.SPORTS);
                } else if ("steadyphoto".equals(scene)) {
                    this.mSupportedSceneModes.add(SceneMode.STEADYPHOTO);
                } else if ("sunset".equals(scene)) {
                    this.mSupportedSceneModes.add(SceneMode.SUNSET);
                } else if ("theatre".equals(scene)) {
                    this.mSupportedSceneModes.add(SceneMode.THEATRE);
                }
            }
        }
    }

    private void buildFlashModes(Parameters p) {
        List<String> supportedFlashModes = p.getSupportedFlashModes();
        if (supportedFlashModes == null) {
            this.mSupportedFlashModes.add(FlashMode.NO_FLASH);
            return;
        }
        for (String flash : supportedFlashModes) {
            if ("auto".equals(flash)) {
                this.mSupportedFlashModes.add(FlashMode.AUTO);
            } else if (ExtendKey.FLIP_MODE_OFF.equals(flash)) {
                this.mSupportedFlashModes.add(FlashMode.OFF);
            } else if ("on".equals(flash)) {
                this.mSupportedFlashModes.add(FlashMode.ON);
            } else if ("red-eye".equals(flash)) {
                this.mSupportedFlashModes.add(FlashMode.RED_EYE);
            } else if ("torch".equals(flash)) {
                this.mSupportedFlashModes.add(FlashMode.TORCH);
            }
        }
    }

    private void buildFocusModes(Parameters p) {
        List<String> supportedFocusModes = p.getSupportedFocusModes();
        if (supportedFocusModes != null) {
            for (String focus : supportedFocusModes) {
                if ("auto".equals(focus)) {
                    this.mSupportedFocusModes.add(FocusMode.AUTO);
                } else if ("continuous-picture".equals(focus)) {
                    this.mSupportedFocusModes.add(FocusMode.CONTINUOUS_PICTURE);
                } else if ("continuous-video".equals(focus)) {
                    this.mSupportedFocusModes.add(FocusMode.CONTINUOUS_VIDEO);
                } else if ("edof".equals(focus)) {
                    this.mSupportedFocusModes.add(FocusMode.EXTENDED_DOF);
                } else if ("fixed".equals(focus)) {
                    this.mSupportedFocusModes.add(FocusMode.FIXED);
                } else if ("infinity".equals(focus)) {
                    this.mSupportedFocusModes.add(FocusMode.INFINITY);
                } else if ("macro".equals(focus)) {
                    this.mSupportedFocusModes.add(FocusMode.MACRO);
                } else if ("manual".equals(focus)) {
                    this.mSupportedFocusModes.add(FocusMode.MANUAL);
                }
            }
        }
    }

    private void buildWhiteBalances(Parameters p) {
        List<String> supportedWhiteBalances = p.getSupportedWhiteBalance();
        if (supportedWhiteBalances != null) {
            for (String wb : supportedWhiteBalances) {
                if ("auto".equals(wb)) {
                    this.mSupportedWhiteBalances.add(WhiteBalance.AUTO);
                } else if ("cloudy-daylight".equals(wb)) {
                    this.mSupportedWhiteBalances.add(WhiteBalance.CLOUDY_DAYLIGHT);
                } else if ("daylight".equals(wb)) {
                    this.mSupportedWhiteBalances.add(WhiteBalance.DAYLIGHT);
                } else if ("fluorescent".equals(wb)) {
                    this.mSupportedWhiteBalances.add(WhiteBalance.FLUORESCENT);
                } else if ("incandescent".equals(wb)) {
                    this.mSupportedWhiteBalances.add(WhiteBalance.INCANDESCENT);
                } else if ("shade".equals(wb)) {
                    this.mSupportedWhiteBalances.add(WhiteBalance.SHADE);
                } else if ("twilight".equals(wb)) {
                    this.mSupportedWhiteBalances.add(WhiteBalance.TWILIGHT);
                } else if ("warm-fluorescent".equals(wb)) {
                    this.mSupportedWhiteBalances.add(WhiteBalance.WARM_FLUORESCENT);
                }
            }
        }
    }

    private void buildManualModes(Parameters p) {
        ExtendParameters parameters = ExtendParameters.getInstance(p);
        this.mMinISO = parameters.getMinISO();
        this.mSupportedIsoValues = parameters.getSupportedISOValues();
        this.mMaxISO = parameters.getMaxISO();
        this.mMinExposureTime = parameters.getMinExposureTime();
        this.mMaxExposureTime = parameters.getMaxExposureTime();
        this.mMinFocusScale = parameters.getMinFocusScale();
        this.mMaxFocusScale = parameters.getMaxFocusScale();
    }

    private ArrayList<String> split(String str) {
        if (str == null) {
            return null;
        }
        StringSplitter<String> splitter = new SimpleStringSplitter(',');
        splitter.setString(str);
        ArrayList<String> substrings = new ArrayList();
        for (String s : splitter) {
            substrings.add(s);
        }
        return substrings;
    }

    private void buildVideoHighFrameRates(Parameters p) {
        String supportedVideoHighFrameRates = p.get(CameraCapabilities.KEY_VIDEO_HIGH_FRAME_RATE_MODES);
        if (supportedVideoHighFrameRates != null) {
            StringSplitter<String> splitter = new SimpleStringSplitter(',');
            splitter.setString(supportedVideoHighFrameRates);
            for (String s : splitter) {
                this.mSupportedVideoHighFrameRates.add(s);
            }
        }
    }

    private void buildHsrSizes(Parameters p) {
        String supportedHsrSizes = p.get(CameraCapabilities.KEY_HSR_SIZES);
        if (supportedHsrSizes != null) {
            StringSplitter<String> splitter = new SimpleStringSplitter(',');
            splitter.setString(supportedHsrSizes);
            for (String s : splitter) {
                int pos = s.indexOf(120);
                if (pos != -1) {
                    this.mSupportedHsrSizes.add(new Size(Integer.parseInt(s.substring(null, pos)), Integer.parseInt(s.substring(pos + 1))));
                }
            }
        }
    }
}
