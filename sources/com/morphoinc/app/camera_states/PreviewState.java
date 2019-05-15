package com.morphoinc.app.camera_states;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Builder;
import android.hardware.camera2.CaptureRequest.Key;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.os.Handler;
import android.util.Range;
import com.morphoinc.app.LogFilter;
import com.morphoinc.app.camera_states.MorphoPanoramaGP3CameraState.CameraStartupInfo;
import com.morphoinc.app.panoramagp3.Camera2ParamsFragment;
import com.morphoinc.app.panoramagp3.CameraConstants;
import com.morphoinc.app.panoramagp3.CameraInfo;
import com.morphoinc.app.panoramagp3.MorphoPanoramaGP3;
import com.morphoinc.utils.os.BuildUtil;
import java.util.Iterator;

public class PreviewState extends MorphoPanoramaGP3CameraState {
    private static final ICaptureCompletedWrapper mNullCaptureCompletedWrapper = new ICaptureCompletedWrapper() {
        public void captureCompleted(CaptureRequest request, TotalCaptureResult result) {
        }
    };
    private ICaptureCompletedWrapper mCaptureCompleted = mNullCaptureCompletedWrapper;
    private final ICaptureCompletedWrapper mContinuousTv = new TvRequester(this, null);
    private final ICaptureCompletedWrapper mContinuousTvSimple = new TvSimpleRequester(this, null);
    private final ICaptureCompletedWrapper mRestartAuto = new ICaptureCompletedWrapper() {
        public void captureCompleted(CaptureRequest request, TotalCaptureResult result) {
            Integer afState = (Integer) result.get(CaptureResult.CONTROL_AF_STATE);
            boolean isReady = afState == null || afState.intValue() == 2;
            if (isReady) {
                PreviewState.this.mCaptureCompleted = PreviewState.mNullCaptureCompletedWrapper;
                PreviewState.this.toTakePictureState(PreviewState.this.getCameraStartupInfo().capture_mode);
            }
        }
    };

    public interface ICaptureCompletedWrapper {
        void captureCompleted(CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult);
    }

    private class TvRequester implements ICaptureCompletedWrapper {
        private TvRequester() {
        }

        /* synthetic */ TvRequester(PreviewState x0, AnonymousClass1 x1) {
            this();
        }

        /* Access modifiers changed, original: protected */
        public void setNewRequest(double gain) {
            TvRequestParams newRequestParams = PreviewState.calculateTvParam(gain, PreviewState.this, true);
            if (newRequestParams.isValid) {
                CameraStartupInfo cameraStartupInfo = PreviewState.this.getCameraStartupInfo();
                Float lensApertureSize = (Float) cameraStartupInfo.totalCaptureResult.get(TotalCaptureResult.LENS_APERTURE);
                Camera2ParamsFragment fragment = PreviewState.this.camera2Params();
                if (fragment.sensorSensitivity() != newRequestParams.sensorSensitivity || Math.abs(fragment.exposureTime().longValue() - newRequestParams.exposureTimeNs) >= CameraConstants.TV_EXPOSURE_TIME_DIFF) {
                    newRequestParams.exposureTimeNs = Math.min(newRequestParams.exposureTimeNs, 60000000);
                    fragment.setSensorSensitivity(newRequestParams.sensorSensitivity);
                    fragment.setExposureTime(newRequestParams.exposureTimeNs);
                    fragment.setCalculatedEv(PreviewState.calculateEv(lensApertureSize, Integer.valueOf(newRequestParams.sensorSensitivity), Long.valueOf(newRequestParams.exposureTimeNs)));
                    try {
                        Builder request = cameraStartupInfo.cameraInfo.getOpenCameraDevice().createCaptureRequest(1);
                        request.addTarget(cameraStartupInfo.previewSurface);
                        request.addTarget(cameraStartupInfo.imageReaderIdling.getSurface());
                        if (cameraStartupInfo.available_af_mode) {
                            request.set(CaptureRequest.CONTROL_AF_MODE, Integer.valueOf(4));
                        }
                        if (cameraStartupInfo.available_ae_mode) {
                            request.set(CaptureRequest.CONTROL_AE_MODE, Integer.valueOf(0));
                        }
                        request.set(CaptureRequest.SENSOR_SENSITIVITY, Integer.valueOf(fragment.sensorSensitivity()));
                        request.set(CaptureRequest.SENSOR_EXPOSURE_TIME, fragment.exposureTime());
                        request.set(CaptureRequest.SENSOR_FRAME_DURATION, fragment.frameDuration());
                        request.set(CaptureRequest.CONTROL_MODE, Integer.valueOf(1));
                        request.set(CaptureRequest.CONTROL_AE_LOCK, Boolean.valueOf(false));
                        request.set(CaptureRequest.CONTROL_AWB_LOCK, Boolean.valueOf(false));
                        if (cameraStartupInfo.available_antibanding_mode) {
                            request.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, Integer.valueOf(cameraStartupInfo.gp3Callback.getAntiBanding()));
                        }
                        PreviewState.setupAvailableImageQualitySettings(PreviewState.this, request, cameraStartupInfo);
                        if (cameraStartupInfo.gp3Callback.isInfinityFocus()) {
                            MorphoPanoramaGP3CameraState.setupFocusModeInfinity(request);
                        }
                        PreviewState.this.setRepeatingRequest(request.build());
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void captureCompleted(CaptureRequest request, TotalCaptureResult result) {
            CameraStartupInfo cameraStartupInfo = PreviewState.this.getCameraStartupInfo();
            for (Image image = cameraStartupInfo.imageReaderIdling.acquireNextImage(); image != null; image = cameraStartupInfo.imageReaderIdling.acquireNextImage()) {
                if (cameraStartupInfo.gp3Callback.isTvLock() && PreviewState.this.camera2Params().tv()) {
                    MorphoPanoramaGP3CameraState.safeImageClose(image);
                } else {
                    double gain = MorphoPanoramaGP3.getGain(image);
                    MorphoPanoramaGP3CameraState.safeImageClose(image);
                    setNewRequest(gain);
                }
            }
            MorphoPanoramaGP3CameraState.safeImageClose(cameraStartupInfo.imageReader.acquireLatestImage());
        }
    }

    private class TvSimpleRequester extends TvRequester {
        private TvSimpleRequester() {
            super(PreviewState.this, null);
        }

        /* synthetic */ TvSimpleRequester(PreviewState x0, AnonymousClass1 x1) {
            this();
        }

        /* Access modifiers changed, original: protected */
        public void setNewRequest(double gain) {
            TvRequestParams newRequestParams = PreviewState.calculateTvSimpleParam(gain, PreviewState.this, true);
            if (newRequestParams.isValid) {
                CameraStartupInfo cameraStartupInfo = PreviewState.this.getCameraStartupInfo();
                Float lensApertureSize = (Float) cameraStartupInfo.totalCaptureResult.get(TotalCaptureResult.LENS_APERTURE);
                Camera2ParamsFragment fragment = PreviewState.this.camera2Params();
                fragment.setSensorSensitivity(newRequestParams.sensorSensitivity);
                fragment.setExposureTime(newRequestParams.exposureTimeNs);
                fragment.setCalculatedEv(PreviewState.calculateEv(lensApertureSize, Integer.valueOf(newRequestParams.sensorSensitivity), Long.valueOf(newRequestParams.exposureTimeNs)));
                boolean currentLock = ((Boolean) cameraStartupInfo.totalCaptureResult.get(TotalCaptureResult.CONTROL_AE_LOCK)).booleanValue();
                boolean lock = cameraStartupInfo.gp3Callback.isTvLock();
                if (currentLock != lock) {
                    try {
                        Builder request = cameraStartupInfo.cameraInfo.getOpenCameraDevice().createCaptureRequest(1);
                        request.addTarget(cameraStartupInfo.previewSurface);
                        request.addTarget(cameraStartupInfo.imageReaderIdling.getSurface());
                        PreviewState.setupPreviewRequest(PreviewState.this, request, true, cameraStartupInfo);
                        PreviewState.setupAvailableImageQualitySettings(PreviewState.this, request, cameraStartupInfo);
                        request.set(CaptureRequest.CONTROL_AE_LOCK, Boolean.valueOf(lock));
                        request.set(CaptureRequest.CONTROL_AWB_LOCK, Boolean.valueOf(lock));
                        PreviewState.this.setRepeatingRequest(request.build());
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static double calculateEv(Float lensApertureSize, Integer sensorSensitivity, Long exposureTimeNs) {
        if (lensApertureSize == null || sensorSensitivity == null || exposureTimeNs == null) {
            return Camera2ParamsFragment.TARGET_EV;
        }
        return Math.log(((1.0E11d * ((double) lensApertureSize.floatValue())) * ((double) lensApertureSize.floatValue())) / ((double) (exposureTimeNs.longValue() * ((long) sensorSensitivity.intValue())))) / Math.log(2.0d);
    }

    public void onCaptureCompleted(CaptureRequest request, TotalCaptureResult result) {
        getCameraStartupInfo().totalCaptureResult = result;
        synchronized (CameraConstants.CameraSynchronizedObject) {
            this.mCaptureCompleted.captureCompleted(request, result);
        }
    }

    public void onCaptureSequenceCompleted(int sequenceId) {
    }

    public void onStart() {
        CameraStartupInfo cameraStartupInfo = getCameraStartupInfo();
        if (cameraStartupInfo.gp3Callback.isEngineRunning()) {
            cameraStartupInfo.backgroundHandler = null;
        } else {
            cameraStartupInfo.backgroundHandler = new Handler(cameraStartupInfo.backgroundHandlerThread.getLooper());
        }
        try {
            Builder request = cameraStartupInfo.cameraInfo.getOpenCameraDevice().createCaptureRequest(1);
            request.addTarget(cameraStartupInfo.previewSurface);
            request.addTarget(cameraStartupInfo.imageReaderIdling.getSurface());
            setupPreviewRequest(this, request, true, cameraStartupInfo);
            setupAvailableImageQualitySettings(this, request, cameraStartupInfo);
            setRepeatingRequest(request.build());
            setupCaptureCompletedWrapper();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        cameraStartupInfo.gp3Callback.onPreviewStart();
        if (cameraStartupInfo.gp3Callback.isEngineRunning()) {
            if (!cameraStartupInfo.camera2Params.auto() || cameraStartupInfo.gp3Callback.isInfinityFocus()) {
                toTakePictureState(getCameraStartupInfo().capture_mode);
            } else {
                this.mCaptureCompleted = this.mRestartAuto;
            }
            return;
        }
        cameraStartupInfo.gp3Callback.setNullDirectionFunction();
    }

    public void onStop() {
        synchronized (CameraConstants.CameraSynchronizedObject) {
            this.mCaptureCompleted = mNullCaptureCompletedWrapper;
            getCameraStartupInfo().backgroundHandler = null;
        }
    }

    private void setupCaptureCompletedWrapper() {
        Camera2ParamsFragment fragment = camera2Params();
        if (fragment.tv()) {
            this.mCaptureCompleted = this.mContinuousTv;
        } else if (fragment.tvSimple()) {
            this.mCaptureCompleted = this.mContinuousTvSimple;
        } else {
            this.mCaptureCompleted = mNullCaptureCompletedWrapper;
        }
    }

    public void onTouch() {
        CameraStartupInfo cameraStartupInfo = getCameraStartupInfo();
        if (cameraStartupInfo.cameraInfo.getHardwareLevel() != 2 && cameraStartupInfo.cameraInfo.getHardwareLevel() != 0) {
            CameraState nextState = new AutoFocusState();
            cameraStartupInfo.gp3Callback.updateCameraState(nextState);
            nextState.onStart();
        }
    }

    public void onTakePictureStart() {
        if (CameraConstants.AutoFocusType == 0 || getCameraStartupInfo().gp3Callback.isInfinityFocus()) {
            toTakePictureState(getCameraStartupInfo().capture_mode);
            return;
        }
        CameraState nextState = new AutoFocusState();
        getCameraStartupInfo().gp3Callback.updateCameraState(nextState);
        nextState.onStart();
    }

    public void toTakePictureState(int capture_mode) {
        Builder request;
        onStop();
        CameraStartupInfo cameraStartupInfo = getCameraStartupInfo();
        cameraStartupInfo.gp3Callback.onTakePicturePreprocess();
        Camera2ParamsFragment fragment = camera2Params();
        int num = 1;
        switch (capture_mode) {
            case 1:
            case 3:
            case 4:
                request = cameraStartupInfo.cameraInfo.getOpenCameraDevice().createCaptureRequest(2);
                break;
            case 2:
                request = cameraStartupInfo.cameraInfo.getOpenCameraDevice().createCaptureRequest(5);
                break;
            default:
                try {
                    request = cameraStartupInfo.cameraInfo.getOpenCameraDevice().createCaptureRequest(1);
                    break;
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                    break;
                }
        }
        request.addTarget(cameraStartupInfo.previewSurface);
        request.addTarget(cameraStartupInfo.imageReader.getSurface());
        setupAvailableImageQualitySettings(this, request, cameraStartupInfo, true);
        if (capture_mode == 2) {
            request.set(CaptureRequest.CONTROL_AE_LOCK, Boolean.valueOf(false));
            request.set(CaptureRequest.CONTROL_AWB_LOCK, Boolean.valueOf(false));
            request.set(CaptureRequest.CONTROL_MODE, Integer.valueOf(1));
        } else {
            request.set(CaptureRequest.CONTROL_AE_LOCK, Boolean.valueOf(cameraStartupInfo.gp3Callback.isAutoAELock()));
            request.set(CaptureRequest.CONTROL_AWB_LOCK, Boolean.valueOf(cameraStartupInfo.gp3Callback.isAutoWBLock()));
            if (cameraStartupInfo.available_antibanding_mode) {
                request.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, Integer.valueOf(cameraStartupInfo.gp3Callback.getAntiBanding()));
            }
            if (cameraStartupInfo.available_af_mode) {
                request.set(CaptureRequest.CONTROL_AF_MODE, Integer.valueOf(1));
            }
            if (!fragment.auto()) {
                if (cameraStartupInfo.available_ae_mode) {
                    request.set(CaptureRequest.CONTROL_AE_MODE, Integer.valueOf(0));
                }
                request.set(CaptureRequest.SENSOR_SENSITIVITY, Integer.valueOf(fragment.sensorSensitivity()));
                request.set(CaptureRequest.SENSOR_EXPOSURE_TIME, fragment.exposureTime());
                request.set(CaptureRequest.SENSOR_FRAME_DURATION, fragment.frameDuration());
            } else if (cameraStartupInfo.available_ae_mode) {
                request.set(CaptureRequest.CONTROL_AE_MODE, Integer.valueOf(1));
                float upperFps = fragment.fps();
                int lowerFps = (int) upperFps;
                Iterator it = cameraStartupInfo.cameraInfo.getTargetFpsRanges().iterator();
                while (it.hasNext()) {
                    Range<Integer> r = (Range) it.next();
                    if (((float) ((Integer) r.getUpper()).intValue()) == upperFps && ((Integer) r.getLower()).intValue() < lowerFps) {
                        lowerFps = ((Integer) r.getLower()).intValue();
                    }
                }
                request.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range.create(Integer.valueOf(lowerFps), Integer.valueOf((int) upperFps)));
            }
            if (cameraStartupInfo.gp3Callback.isInfinityFocus()) {
                MorphoPanoramaGP3CameraState.setupFocusModeInfinity(request);
            }
        }
        if (capture_mode == 3 || capture_mode == 4) {
            try {
                Builder burst_request = cameraStartupInfo.cameraInfo.getOpenCameraDevice().createCaptureRequest(2);
                burst_request.addTarget(cameraStartupInfo.previewSurface);
                burst_request.addTarget(cameraStartupInfo.imageReader.getSurface());
                burst_request.set(CaptureRequest.CONTROL_AE_LOCK, Boolean.valueOf(cameraStartupInfo.gp3Callback.isAutoAELock()));
                burst_request.set(CaptureRequest.CONTROL_AWB_LOCK, Boolean.valueOf(cameraStartupInfo.gp3Callback.isAutoWBLock()));
                if (cameraStartupInfo.available_antibanding_mode) {
                    burst_request.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, Integer.valueOf(cameraStartupInfo.gp3Callback.getAntiBanding()));
                }
                setupAvailableImageQualitySettings(this, burst_request, cameraStartupInfo, true);
                if (CameraConstants.AutoFocusType != 2 && cameraStartupInfo.available_af_mode) {
                    burst_request.set(CaptureRequest.CONTROL_AF_MODE, Integer.valueOf(1));
                }
                if (fragment.auto()) {
                    if (cameraStartupInfo.available_ae_mode) {
                        burst_request.set(CaptureRequest.CONTROL_AE_MODE, Integer.valueOf(1));
                    }
                    float upperFps2 = fragment.fps();
                    int lowerFps2 = (int) upperFps2;
                    Iterator it2 = cameraStartupInfo.cameraInfo.getTargetFpsRanges().iterator();
                    while (it2.hasNext()) {
                        Range<Integer> r2 = (Range) it2.next();
                        if (((float) ((Integer) r2.getUpper()).intValue()) == upperFps2 && ((Integer) r2.getLower()).intValue() < lowerFps2) {
                            lowerFps2 = ((Integer) r2.getLower()).intValue();
                        }
                    }
                    if (cameraStartupInfo.available_ae_mode) {
                        burst_request.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range.create(Integer.valueOf(lowerFps2), Integer.valueOf((int) upperFps2)));
                        burst_request.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, Integer.valueOf(fragment.evSteps()));
                    }
                    if (cameraStartupInfo.available_scene_mode_sports) {
                        burst_request.set(CaptureRequest.CONTROL_MODE, Integer.valueOf(2));
                        burst_request.set(CaptureRequest.CONTROL_SCENE_MODE, Integer.valueOf(13));
                    }
                } else {
                    if (cameraStartupInfo.available_ae_mode) {
                        burst_request.set(CaptureRequest.CONTROL_AE_MODE, Integer.valueOf(0));
                    }
                    burst_request.set(CaptureRequest.SENSOR_SENSITIVITY, Integer.valueOf(fragment.sensorSensitivity()));
                    burst_request.set(CaptureRequest.SENSOR_EXPOSURE_TIME, fragment.exposureTime());
                    burst_request.set(CaptureRequest.SENSOR_FRAME_DURATION, fragment.frameDuration());
                }
                if (cameraStartupInfo.gp3Callback.isInfinityFocus()) {
                    MorphoPanoramaGP3CameraState.setupFocusModeInfinity(burst_request);
                }
                burst_request.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, Integer.valueOf(0));
                burst_request.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, Integer.valueOf(cameraStartupInfo.gp3Callback.useOis()));
                cameraStartupInfo.currentBurstRequestBuilder = burst_request;
                CaptureRequest req = burst_request.build();
                cameraStartupInfo.burstRequestList.clear();
                if (!fragment.auto()) {
                    num = capture_mode == 3 ? 600 : CameraConstants.REPEATING_BURST_SHOT_NUM;
                }
                for (int i = 0; i < num; i++) {
                    cameraStartupInfo.burstRequestList.add(req);
                }
            } catch (CameraAccessException e2) {
                e2.printStackTrace();
            }
        }
        cameraStartupInfo.gp3Callback.updateTvValue();
        cameraStartupInfo.currentRequestBuilder = request;
        cameraStartupInfo.captureRequest = request.build();
        CameraCaptureSession session = cameraStartupInfo.cameraInfo.getCaptureSession();
        session.stopRepeating();
        switch (capture_mode) {
            case 1:
                LogFilter.i("MorphoCamera2State", "onTakePictureStart : STILL");
                session.capture(cameraStartupInfo.captureRequest, cameraStartupInfo.captureCallback, backgroundHandler());
                break;
            case 2:
                LogFilter.i("MorphoCamera2State", "onTakePictureStart : ZERO_SHUTTER_LAG");
                session.capture(cameraStartupInfo.captureRequest, cameraStartupInfo.captureCallback, backgroundHandler());
                break;
            case 3:
                LogFilter.i("MorphoCamera2State", "onTakePictureStart : BURST");
                setBurstRemaining(0);
                session.capture(cameraStartupInfo.captureRequest, cameraStartupInfo.captureCallback, backgroundHandler());
                break;
            case 4:
                LogFilter.i("MorphoCamera2State", "onTakePictureStart : REPEATING_BURST");
                session.setRepeatingBurst(cameraStartupInfo.burstRequestList, cameraStartupInfo.captureCallback, backgroundHandler());
                break;
            default:
                LogFilter.i("MorphoCamera2State", "onTakePictureStart : PREVIEW");
                session.setRepeatingRequest(cameraStartupInfo.captureRequest, cameraStartupInfo.captureCallback, backgroundHandler());
                break;
        }
        CameraState nextState = new TakePictureState();
        cameraStartupInfo.gp3Callback.updateCameraState(nextState);
        nextState.onStart();
    }

    public void onRequestParamChange() {
        CameraStartupInfo cameraStartupInfo = getCameraStartupInfo();
        try {
            Builder request = cameraStartupInfo.cameraInfo.getOpenCameraDevice().createCaptureRequest(1);
            request.addTarget(cameraStartupInfo.previewSurface);
            request.addTarget(cameraStartupInfo.imageReaderIdling.getSurface());
            setupPreviewRequest(this, request, true, cameraStartupInfo);
            setupAvailableImageQualitySettings(this, request, cameraStartupInfo);
            setRepeatingRequest(request.build());
            setupCaptureCompletedWrapper();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setRepeatingRequest(CaptureRequest request) throws CameraAccessException {
        synchronized (CameraConstants.CameraSynchronizedObject) {
            CameraStartupInfo cameraStartupInfo = getCameraStartupInfo();
            cameraStartupInfo.cameraInfo.getCaptureSession().setRepeatingRequest(request, cameraStartupInfo.captureCallback, backgroundHandler());
        }
    }

    public static void setupPreviewRequest(MorphoPanoramaGP3CameraState state, Builder request, boolean afContinuous, CameraStartupInfo cameraStartupInfo) {
        Camera2ParamsFragment fragment = state.camera2Params();
        if (fragment.auto() || fragment.tvSimple()) {
            if (cameraStartupInfo.available_ae_mode) {
                request.set(CaptureRequest.CONTROL_AE_MODE, Integer.valueOf(1));
                request.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, Integer.valueOf(fragment.evSteps()));
                if (fragment.auto()) {
                    float upperFps = fragment.fps();
                    int lowerFps = (int) upperFps;
                    Iterator it = cameraStartupInfo.cameraInfo.getTargetFpsRanges().iterator();
                    while (it.hasNext()) {
                        Range<Integer> r = (Range) it.next();
                        if (((float) ((Integer) r.getUpper()).intValue()) == upperFps && ((Integer) r.getLower()).intValue() < lowerFps) {
                            lowerFps = ((Integer) r.getLower()).intValue();
                        }
                    }
                    request.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range.create(Integer.valueOf(lowerFps), Integer.valueOf((int) upperFps)));
                }
            }
            if (!BuildUtil.isSony()) {
                request.set(CaptureRequest.CONTROL_MODE, Integer.valueOf(1));
            } else if (cameraStartupInfo.available_scene_mode_sports) {
                request.set(CaptureRequest.CONTROL_MODE, Integer.valueOf(2));
                request.set(CaptureRequest.CONTROL_SCENE_MODE, Integer.valueOf(13));
            }
        } else if (fragment.tvAll()) {
            if (cameraStartupInfo.available_ae_mode) {
                request.set(CaptureRequest.CONTROL_AE_MODE, Integer.valueOf(0));
            }
            request.set(CaptureRequest.SENSOR_SENSITIVITY, Integer.valueOf(fragment.sensorSensitivity()));
            request.set(CaptureRequest.SENSOR_EXPOSURE_TIME, fragment.exposureTime());
            request.set(CaptureRequest.SENSOR_FRAME_DURATION, fragment.frameDuration());
            request.set(CaptureRequest.CONTROL_MODE, Integer.valueOf(1));
        } else {
            if (cameraStartupInfo.available_ae_mode) {
                request.set(CaptureRequest.CONTROL_AE_MODE, Integer.valueOf(0));
            }
            request.set(CaptureRequest.SENSOR_SENSITIVITY, Integer.valueOf(fragment.sensorSensitivity()));
            request.set(CaptureRequest.SENSOR_EXPOSURE_TIME, fragment.exposureTime());
            request.set(CaptureRequest.SENSOR_FRAME_DURATION, fragment.frameDuration());
            request.set(CaptureRequest.CONTROL_MODE, Integer.valueOf(1));
        }
        if (afContinuous && cameraStartupInfo.available_af_mode) {
            request.set(CaptureRequest.CONTROL_AF_MODE, Integer.valueOf(4));
        }
        if (state.getCameraStartupInfo().gp3Callback.isInfinityFocus()) {
            MorphoPanoramaGP3CameraState.setupFocusModeInfinity(request);
        }
        request.set(CaptureRequest.CONTROL_AE_LOCK, Boolean.valueOf(false));
        request.set(CaptureRequest.CONTROL_AWB_LOCK, Boolean.valueOf(false));
        if (cameraStartupInfo.available_antibanding_mode) {
            request.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, Integer.valueOf(state.getCameraStartupInfo().gp3Callback.getAntiBanding()));
        }
        request.set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, Integer.valueOf(0));
        request.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, Integer.valueOf(state.getCameraStartupInfo().gp3Callback.useOis()));
    }

    public static void setupAvailableImageQualitySettings(MorphoPanoramaGP3CameraState state, Builder request, CameraStartupInfo cameraStartupInfo) {
        setupAvailableImageQualitySettings(state, request, cameraStartupInfo, false);
    }

    private static void setupAvailableImageQualitySettings(MorphoPanoramaGP3CameraState state, Builder request, CameraStartupInfo cameraStartupInfo, boolean toSend) {
        if (cameraStartupInfo.available_image_quality_settings) {
            Camera2ParamsFragment fragment = state.camera2Params();
            IMorphoPanoramaGP3Callback gp3Callback = cameraStartupInfo.gp3Callback;
            setRequestIntParamIfEnable(request, CaptureRequest.COLOR_CORRECTION_MODE, gp3Callback.getColorCorrectionMode());
            int edge_mode;
            if (gp3Callback.isAutoEdgeNR()) {
                if (600 < fragment.sensorSensitivity()) {
                    setRequestIntParamIfEnable(request, CaptureRequest.EDGE_MODE, 0);
                    if (toSend) {
                        cameraStartupInfo.gp3Callback.setEdgeMode(0);
                    }
                } else {
                    edge_mode = BuildUtil.isGalaxyS7() ? 1 : 2;
                    setRequestIntParamIfEnable(request, CaptureRequest.EDGE_MODE, edge_mode);
                    if (toSend) {
                        gp3Callback.setEdgeMode(edge_mode);
                    }
                }
                if (fragment.sensorSensitivity() <= 400) {
                    setRequestIntParamIfEnable(request, CaptureRequest.NOISE_REDUCTION_MODE, 1);
                    if (toSend) {
                        cameraStartupInfo.gp3Callback.setNoiseReductionMode(1);
                    }
                } else {
                    setRequestIntParamIfEnable(request, CaptureRequest.NOISE_REDUCTION_MODE, 2);
                    if (toSend) {
                        gp3Callback.setNoiseReductionMode(2);
                    }
                }
            } else {
                edge_mode = gp3Callback.getEdgeMode();
                int noiseReductionMode = gp3Callback.getNoiseReductionMode();
                setRequestIntParamIfEnable(request, CaptureRequest.EDGE_MODE, edge_mode);
                setRequestIntParamIfEnable(request, CaptureRequest.NOISE_REDUCTION_MODE, noiseReductionMode);
                if (toSend) {
                    gp3Callback.setEdgeMode(edge_mode);
                    gp3Callback.setNoiseReductionMode(noiseReductionMode);
                }
            }
            setRequestIntParamIfEnable(request, CaptureRequest.SHADING_MODE, gp3Callback.getShadingMode());
            setRequestIntParamIfEnable(request, CaptureRequest.TONEMAP_MODE, gp3Callback.getTonemapMode());
        }
    }

    private static void setRequestIntParamIfEnable(Builder request, Key<Integer> key, int val) {
        if (val >= 0) {
            request.set(key, Integer.valueOf(val));
        }
    }

    public static TvRequestParams calculateTvParam(double gain, MorphoPanoramaGP3CameraState camera2State, boolean useSmooth) {
        CameraStartupInfo cameraStartupInfo = camera2State.getCameraStartupInfo();
        Integer currentSensorSensitivity = (Integer) cameraStartupInfo.totalCaptureResult.get(TotalCaptureResult.SENSOR_SENSITIVITY);
        Long currentExposureTime = (Long) cameraStartupInfo.totalCaptureResult.get(TotalCaptureResult.SENSOR_EXPOSURE_TIME);
        double d;
        CameraStartupInfo cameraStartupInfo2;
        Integer num;
        if (currentSensorSensitivity == null) {
            d = gain;
            cameraStartupInfo2 = cameraStartupInfo;
            num = currentSensorSensitivity;
        } else if (currentExposureTime == null) {
            d = gain;
            cameraStartupInfo2 = cameraStartupInfo;
            num = currentSensorSensitivity;
        } else {
            Camera2ParamsFragment fragment = camera2State.camera2Params();
            double clampGain = clampGain(gain, fragment.evSteps(), cameraStartupInfo.cameraInfo.getAeCompensationStep());
            double targetEv = useSmooth ? MorphoPanoramaGP3CameraState.getSmoothenedEv((((double) currentSensorSensitivity.intValue()) * clampGain) * ((double) currentExposureTime.longValue())) : (((double) currentSensorSensitivity.intValue()) * clampGain) * ((double) currentExposureTime.longValue());
            int sensorSensitivity = cameraStartupInfo.cameraInfo.clampSensitivityRange((int) (targetEv / ((double) fragment.shutterSpeedInNanoSeconds())));
            cameraStartupInfo = Math.min(cameraStartupInfo.cameraInfo.clampExposureTime(Long.valueOf((long) (targetEv / ((double) sensorSensitivity)))).longValue(), CameraConstants.TV_EXPOSURE_TIME_MAX);
            TvRequestParams newParams = new TvRequestParams();
            newParams.exposureTimeNs = cameraStartupInfo;
            newParams.sensorSensitivity = sensorSensitivity;
            newParams.isValid = true;
            return newParams;
        }
        LogFilter.e("MorphoCamera2State", "SENSOR_SENSITIVITY or SENSOR_EXPOSURE_TIME is null.");
        return new TvRequestParams();
    }

    public static TvRequestParams calculateTvSimpleParam(double gain, MorphoPanoramaGP3CameraState camera2State, boolean useSmooth) {
        CameraStartupInfo cameraStartupInfo = camera2State.getCameraStartupInfo();
        Integer currentSensorSensitivity = (Integer) cameraStartupInfo.totalCaptureResult.get(TotalCaptureResult.SENSOR_SENSITIVITY);
        Long currentExposureTime = (Long) cameraStartupInfo.totalCaptureResult.get(TotalCaptureResult.SENSOR_EXPOSURE_TIME);
        Integer num;
        Long l;
        if (currentSensorSensitivity == null) {
            num = currentSensorSensitivity;
            l = currentExposureTime;
        } else if (currentExposureTime == null) {
            CameraStartupInfo cameraStartupInfo2 = cameraStartupInfo;
            num = currentSensorSensitivity;
            l = currentExposureTime;
        } else {
            double clampedGain;
            Camera2ParamsFragment fragment = camera2State.camera2Params();
            if (cameraStartupInfo.gp3Callback.isEngineRunning()) {
                clampedGain = clampGain(gain, fragment.evSteps(), cameraStartupInfo.cameraInfo.getAeCompensationStep());
            } else {
                double d = gain;
                clampedGain = 1.0d;
            }
            double targetExposure = useSmooth ? MorphoPanoramaGP3CameraState.getSmoothenedEv((((double) currentSensorSensitivity.intValue()) * clampedGain) * ((double) currentExposureTime.longValue())) : (((double) currentSensorSensitivity.intValue()) * clampedGain) * ((double) currentExposureTime.longValue());
            boolean antiFlickerOff = cameraStartupInfo.gp3Callback.getAntiFlickerMode() == 1;
            Range<Long> exposureTimeRange = cameraStartupInfo.cameraInfo.getExposureTimeRange();
            Range<Integer> sensitivityRange = cameraStartupInfo.cameraInfo.getSensitivityRange();
            boolean antiFlickerOn = cameraStartupInfo.gp3Callback.getAntiFlickerMode() == 0;
            cameraStartupInfo = cameraStartupInfo.gp3Callback.getAntiFlickerMode() == 2 ? true : null;
            int etLvl1 = 100;
            if (!(antiFlickerOff || antiFlickerOn || cameraStartupInfo != null)) {
                etLvl1 = 120;
            }
            int i = 60;
            if (!(antiFlickerOff || antiFlickerOn || cameraStartupInfo == null)) {
                i = 50;
            }
            int etLvl2 = i;
            i = 30;
            if (!(antiFlickerOff || antiFlickerOn || cameraStartupInfo == null)) {
                i = 25;
            }
            int etLvl3 = i;
            i = (antiFlickerOff || antiFlickerOn) ? 10 : cameraStartupInfo != null ? 12 : 15;
            int etLvl4 = i;
            boolean antiFlicker50Hz = cameraStartupInfo;
            if (targetExposure < ((0.5d * ((double) ((Integer) sensitivityRange.getLower()).intValue())) * 0) / ((double) etLvl1)) {
                antiFlickerOff = true;
            }
            cameraStartupInfo = new TvRequestParams();
            if (targetExposure > 6250000.0d || !antiFlickerOff) {
                if (targetExposure <= 2.5E7d && antiFlickerOff) {
                    cameraStartupInfo.exposureTimeNs = ((Long) exposureTimeRange.clamp(Long.valueOf((long) Math.rint(250000.0d)))).longValue();
                    cameraStartupInfo.sensorSensitivity = ((Integer) sensitivityRange.clamp(Integer.valueOf((int) Math.rint(targetExposure / ((double) cameraStartupInfo.exposureTimeNs))))).intValue();
                } else if (targetExposure <= 1.0E8d && antiFlickerOff) {
                    cameraStartupInfo.exposureTimeNs = ((Long) exposureTimeRange.clamp(Long.valueOf((long) Math.rint(500000.0d)))).longValue();
                    cameraStartupInfo.sensorSensitivity = ((Integer) sensitivityRange.clamp(Integer.valueOf((int) Math.rint(targetExposure / ((double) cameraStartupInfo.exposureTimeNs))))).intValue();
                } else if (targetExposure > 4.0E8d || !antiFlickerOff) {
                    Camera2ParamsFragment camera2ParamsFragment = fragment;
                    double d2 = clampedGain;
                    if (targetExposure <= 4.0E11d / ((double) etLvl1) && antiFlickerOff) {
                        cameraStartupInfo.sensorSensitivity = ((Integer) sensitivityRange.clamp(Integer.valueOf(400))).intValue();
                        cameraStartupInfo.exposureTimeNs = ((Long) exposureTimeRange.clamp(Long.valueOf((long) Math.rint(targetExposure / ((double) cameraStartupInfo.sensorSensitivity))))).longValue();
                        currentExposureTime = etLvl3;
                        cameraStartupInfo.isValid = true;
                        return cameraStartupInfo;
                    } else if (targetExposure <= 6.0E11d / ((double) etLvl1)) {
                        cameraStartupInfo.exposureTimeNs = ((Long) exposureTimeRange.clamp(Long.valueOf((long) Math.rint(1.0E9d / ((double) etLvl1))))).longValue();
                        cameraStartupInfo.sensorSensitivity = ((Integer) sensitivityRange.clamp(Integer.valueOf((int) Math.rint(targetExposure / ((double) cameraStartupInfo.exposureTimeNs))))).intValue();
                        currentExposureTime = etLvl3;
                        cameraStartupInfo.isValid = true;
                        return cameraStartupInfo;
                    } else {
                        currentSensorSensitivity = etLvl2;
                        if (targetExposure <= 6.0E11d / ((double) currentSensorSensitivity) && (antiFlickerOff || antiFlickerOn)) {
                            cameraStartupInfo.sensorSensitivity = ((Integer) sensitivityRange.clamp(Integer.valueOf(600))).intValue();
                            cameraStartupInfo.exposureTimeNs = ((Long) exposureTimeRange.clamp(Long.valueOf((long) Math.rint(targetExposure / ((double) cameraStartupInfo.sensorSensitivity))))).longValue();
                            currentExposureTime = etLvl3;
                            cameraStartupInfo.isValid = true;
                            return cameraStartupInfo;
                        } else if (targetExposure <= 1.6E12d / ((double) currentSensorSensitivity)) {
                            cameraStartupInfo.exposureTimeNs = ((Long) exposureTimeRange.clamp(Long.valueOf((long) Math.rint(1.0E9d / ((double) currentSensorSensitivity))))).longValue();
                            cameraStartupInfo.sensorSensitivity = ((Integer) sensitivityRange.clamp(Integer.valueOf((int) Math.rint(targetExposure / ((double) cameraStartupInfo.exposureTimeNs))))).intValue();
                            currentExposureTime = etLvl3;
                            cameraStartupInfo.isValid = true;
                            return cameraStartupInfo;
                        } else {
                            currentExposureTime = etLvl3;
                            if (targetExposure <= 1.6E12d / ((double) currentExposureTime) && (antiFlickerOff || antiFlickerOn)) {
                                cameraStartupInfo.sensorSensitivity = ((Integer) sensitivityRange.clamp(Integer.valueOf(CameraConstants.MAX_ISO_VALUE))).intValue();
                                cameraStartupInfo.exposureTimeNs = ((Long) exposureTimeRange.clamp(Long.valueOf((long) Math.rint(targetExposure / ((double) cameraStartupInfo.sensorSensitivity))))).longValue();
                                cameraStartupInfo.isValid = true;
                                return cameraStartupInfo;
                            } else if (targetExposure <= (((double) ((Integer) sensitivityRange.getUpper()).intValue()) * 1.0E9d) / ((double) currentExposureTime)) {
                                cameraStartupInfo.exposureTimeNs = ((Long) exposureTimeRange.clamp(Long.valueOf((long) Math.rint(1.0E9d / ((double) currentExposureTime))))).longValue();
                                cameraStartupInfo.sensorSensitivity = ((Integer) sensitivityRange.clamp(Integer.valueOf((int) Math.rint(targetExposure / ((double) cameraStartupInfo.exposureTimeNs))))).intValue();
                                cameraStartupInfo.isValid = true;
                                return cameraStartupInfo;
                            } else {
                                int etLvl42 = etLvl4;
                                if (targetExposure > (((double) ((Integer) sensitivityRange.getUpper()).intValue()) * 1.0E9d) / ((double) etLvl42) || !(antiFlickerOff || antiFlickerOn)) {
                                    cameraStartupInfo.exposureTimeNs = ((Long) exposureTimeRange.clamp(Long.valueOf((long) Math.rint(1.0E9d / ((double) etLvl42))))).longValue();
                                    cameraStartupInfo.sensorSensitivity = ((Integer) sensitivityRange.clamp(Integer.valueOf((int) Math.rint(targetExposure / ((double) cameraStartupInfo.exposureTimeNs))))).intValue();
                                } else {
                                    cameraStartupInfo.sensorSensitivity = ((Integer) sensitivityRange.clamp((Integer) sensitivityRange.getUpper())).intValue();
                                    cameraStartupInfo.exposureTimeNs = ((Long) exposureTimeRange.clamp(Long.valueOf((long) Math.rint(targetExposure / ((double) cameraStartupInfo.sensorSensitivity))))).longValue();
                                }
                                cameraStartupInfo.isValid = true;
                                return cameraStartupInfo;
                            }
                        }
                    }
                } else {
                    cameraStartupInfo.exposureTimeNs = ((Long) exposureTimeRange.clamp(Long.valueOf((long) Math.rint(1000000.0d)))).longValue();
                    cameraStartupInfo.sensorSensitivity = ((Integer) sensitivityRange.clamp(Integer.valueOf((int) Math.rint(targetExposure / ((double) cameraStartupInfo.exposureTimeNs))))).intValue();
                }
            } else {
                cameraStartupInfo.exposureTimeNs = ((Long) exposureTimeRange.clamp(Long.valueOf((long) Math.rint(125000.0d)))).longValue();
                cameraStartupInfo.sensorSensitivity = ((Integer) sensitivityRange.clamp(Integer.valueOf((int) Math.rint(targetExposure / ((double) cameraStartupInfo.exposureTimeNs))))).intValue();
            }
            currentExposureTime = etLvl3;
            cameraStartupInfo.isValid = true;
            return cameraStartupInfo;
        }
        LogFilter.e("MorphoCamera2State", "SENSOR_SENSITIVITY or SENSOR_EXPOSURE_TIME is null.");
        return new TvRequestParams();
    }

    private static double clampGain(double gain, int steps, double aecStepValue) {
        if (steps == 0) {
            return ((Double) CameraInfo.PREVIEW_GAIN_RANGE.clamp(Double.valueOf(gain))).doubleValue();
        }
        return ((Double) CameraInfo.PREVIEW_GAIN_RANGE.clamp(Double.valueOf(Math.exp((Math.log(2.0d) * aecStepValue) * ((double) steps)) * gain))).doubleValue();
    }
}
