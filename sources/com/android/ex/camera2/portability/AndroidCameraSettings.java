package com.android.ex.camera2.portability;

import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.text.TextUtils;
import com.android.ex.camera2.portability.CameraCapabilities.Feature;
import com.android.ex.camera2.portability.CameraCapabilities.SceneMode;
import com.android.ex.camera2.portability.CameraCapabilities.Stringifier;
import com.android.ex.camera2.portability.debug.Log;
import com.android.ex.camera2.portability.debug.Log.Tag;
import com.android.external.ExtendKey;
import com.android.external.ExtendParameters;
import com.hmdglobal.app.camera.util.CustomFields;
import com.hmdglobal.app.camera.util.CustomUtil;

public class AndroidCameraSettings extends CameraSettings {
    private static final String RECORDING_HINT = "recording-hint";
    private static final Tag TAG = new Tag("AndCamSet");
    private static final String TRUE = "true";

    public AndroidCameraSettings(CameraCapabilities capabilities, Parameters params) {
        if (params == null) {
            Log.w(TAG, "Settings ctor requires a non-null Camera.Parameters.");
            return;
        }
        String smooth;
        int skinSmooth;
        Stringifier stringifier = capabilities.getStringifier();
        setSizesLocked(false);
        Size paramPreviewSize = params.getPreviewSize();
        setPreviewSize(new Size(paramPreviewSize.width, paramPreviewSize.height));
        setPreviewFrameRate(params.getPreviewFrameRate());
        int[] previewFpsRange = new int[2];
        params.getPreviewFpsRange(previewFpsRange);
        setPreviewFpsRange(previewFpsRange[0], previewFpsRange[1]);
        setPreviewFormat(params.getPreviewFormat());
        if (capabilities.supports(Feature.ZOOM)) {
            setZoomRatio(((float) ((Integer) params.getZoomRatios().get(params.getZoom())).intValue()) / 100.0f);
        } else {
            setZoomRatio(1.0f);
        }
        setExposureCompensationIndex(params.getExposureCompensation());
        setFlashMode(stringifier.flashModeFromString(params.getFlashMode()));
        setFocusMode(stringifier.focusModeFromString(params.getFocusMode()));
        setSceneMode(stringifier.sceneModeFromString(params.getSceneMode()));
        String tsMode = params.get(ExtendKey.HMD_MODE);
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getsettings tsMode:");
        stringBuilder.append(tsMode);
        Log.i(tag, stringBuilder.toString());
        if (TextUtils.equals(tsMode, "1") && CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_ENABLE_HMD_HDR, false)) {
            setSceneMode(SceneMode.HDR);
        }
        String visidonMode = params.get(ExtendKey.VISIDON_MODE);
        Tag tag2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("getsettings visidonMode:");
        stringBuilder2.append(visidonMode);
        Log.i(tag2, stringBuilder2.toString());
        if (visidonMode != null) {
            if (visidonMode.equals(ExtendKey.VISIDON_LOW_LIGHT)) {
                setLowLight(true);
            } else if (visidonMode.equals(ExtendKey.VISIDON_FACE_BEAUTY)) {
                smooth = params.get(ExtendKey.VISIDON_SKIN_SMOOTHING);
                skinSmooth = 50;
                if (smooth != null) {
                    skinSmooth = Integer.parseInt(smooth);
                }
                setFaceBeauty(true, skinSmooth);
            } else if (visidonMode.equals(ExtendKey.VISIDON_SUPER_RESOLUTION)) {
                setSuperResolutionOn(true);
            }
        }
        if (CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_HMD_EIS, false)) {
            if (TextUtils.equals(params.get(ExtendKey.HMD_EIS_ENABLE), "on")) {
                setVideoStabilization(true);
            } else {
                setVideoStabilization(false);
            }
        } else if (capabilities.supports(Feature.VIDEO_STABILIZATION)) {
            setVideoStabilization(isVideoStabilizationEnabled());
        }
        setRecordingHintEnabled(TRUE.equals(params.get(RECORDING_HINT)));
        setPhotoJpegCompressionQuality(params.getJpegQuality());
        Size paramPictureSize = params.getPictureSize();
        setPhotoSize(new Size(paramPictureSize.width, paramPictureSize.height));
        setPhotoFormat(params.getPictureFormat());
        smooth = params.get(CameraCapabilities.KEY_VIDEO_SIZE);
        if (smooth != null) {
            skinSmooth = smooth.indexOf(120);
            if (skinSmooth != -1) {
                int width = Integer.parseInt(smooth.substring(0, skinSmooth));
                int height = Integer.parseInt(smooth.substring(skinSmooth + 1));
                if (width > 0 && height > 0) {
                    setVideoSize(new Size(width, height));
                }
            }
        }
        setHsr(params.get(CameraCapabilities.KEY_VIDEO_HSR));
        setAec(params.get(CameraCapabilities.KEY_AEC_SETTLED));
        this.isZslOn = ExtendParameters.getInstance(params).getZSLMode();
    }

    public AndroidCameraSettings(AndroidCameraSettings other) {
        super(other);
    }

    public CameraSettings copy() {
        return new AndroidCameraSettings(this);
    }
}
