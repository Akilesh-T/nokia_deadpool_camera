package com.morphoinc.app.camera_states;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Builder;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import com.morphoinc.app.LogFilter;
import com.morphoinc.app.camera_states.MorphoPanoramaGP3CameraState.CameraStartupInfo;
import com.morphoinc.app.panoramagp3.CameraConstants;

public class AutoFocusState extends MorphoPanoramaGP3CameraState {
    private static final int AF_SAME_STATE_REPEAT_MAX = 30;
    private static final int STATE_INIT = -1;
    private static final int STATE_WAITING_LOCK = 0;
    private static final int STATE_WAITING_NON_PRECAPTURE = 2;
    private static final int STATE_WAITING_PRECAPTURE = 1;
    private boolean mCancel = false;
    private int mPreAFState;
    private int mSameAFStateCount = 0;
    private int mState = -1;

    public void onStart() {
        CameraStartupInfo cameraStartupInfo = getCameraStartupInfo();
        try {
            Builder request = cameraStartupInfo.cameraInfo.getOpenCameraDevice().createCaptureRequest(1);
            request.addTarget(cameraStartupInfo.previewSurface);
            request.set(CaptureRequest.CONTROL_AF_TRIGGER, Integer.valueOf(1));
            PreviewState.setupPreviewRequest(this, request, false, cameraStartupInfo);
            PreviewState.setupAvailableImageQualitySettings(this, request, cameraStartupInfo);
            if (CameraConstants.AutoFocusType == 0) {
                cameraStartupInfo.cameraInfo.getCaptureSession().setRepeatingRequest(request.build(), cameraStartupInfo.captureCallback, backgroundHandler());
            } else if (CameraConstants.AutoFocusType == 1) {
                request.set(CaptureRequest.CONTROL_AF_MODE, Integer.valueOf(1));
                cameraStartupInfo.cameraInfo.getCaptureSession().setRepeatingRequest(request.build(), cameraStartupInfo.captureCallback, backgroundHandler());
            } else if (CameraConstants.AutoFocusType == 2) {
                cameraStartupInfo.cameraInfo.getCaptureSession().capture(request.build(), cameraStartupInfo.captureCallback, backgroundHandler());
            }
            this.mState = 0;
            this.mSameAFStateCount = 0;
            this.mPreAFState = -1;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void onCancel() {
        this.mCancel = true;
        getCameraStartupInfo().gp3Callback.updateCameraState(new PreviewState());
    }

    public boolean canExit() {
        return false;
    }

    public void onCaptureCompleted(CaptureRequest request, TotalCaptureResult result) {
        CameraStartupInfo cameraStartupInfo = getCameraStartupInfo();
        if (cameraStartupInfo.camera2Params.tvAll()) {
            if (cameraStartupInfo.imageReaderIdling != null) {
                MorphoPanoramaGP3CameraState.safeImageClose(cameraStartupInfo.imageReaderIdling.acquireLatestImage());
            }
            if (cameraStartupInfo.imageReader != null) {
                MorphoPanoramaGP3CameraState.safeImageClose(cameraStartupInfo.imageReader.acquireLatestImage());
            }
        }
        Integer afState;
        StringBuilder stringBuilder;
        if (this.mState == 0) {
            afState = (Integer) result.get(CaptureResult.CONTROL_AF_STATE);
            if (afState == null) {
                LogFilter.w("MorphoCamera2State", "AutoFocusState.onCaptureCompleted AF STATE is null");
                this.mState = -1;
                autoFocusEnd();
                return;
            }
            if (afState.intValue() == 4 || afState.intValue() == 5) {
                Integer aeState = (Integer) result.get(CaptureResult.CONTROL_AE_STATE);
                stringBuilder = new StringBuilder();
                stringBuilder.append("AutoFocusState.onCaptureCompleted AF STATE = ");
                stringBuilder.append(afState);
                stringBuilder.append(", AE STATE = ");
                stringBuilder.append(aeState);
                LogFilter.i("MorphoCamera2State", stringBuilder.toString());
                if (!cameraStartupInfo.camera2Params.auto() || aeState == null || aeState.intValue() == 2) {
                    this.mState = -1;
                    autoFocusEnd();
                    return;
                }
            }
            if (afState.intValue() == 1 || afState.intValue() != this.mPreAFState) {
                this.mSameAFStateCount = 0;
            } else {
                this.mSameAFStateCount++;
                if (this.mSameAFStateCount >= 30) {
                    this.mState = -1;
                    autoFocusEnd();
                    return;
                }
            }
            this.mPreAFState = afState.intValue();
        } else if (this.mState == 1) {
            afState = (Integer) result.get(CaptureResult.CONTROL_AE_STATE);
            stringBuilder = new StringBuilder();
            stringBuilder.append("WAITING_PRECAPTURE AE STATE = ");
            stringBuilder.append(afState);
            LogFilter.i("MorphoCamera2State", stringBuilder.toString());
            if (afState == null || afState.intValue() == 5 || afState.intValue() == 4) {
                this.mState = 2;
            } else if (afState.intValue() == 2) {
                this.mState = -1;
                autoFocusEnd();
            }
        } else {
            if (this.mState == 2) {
                afState = (Integer) result.get(CaptureResult.CONTROL_AE_STATE);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("WAITING_NON_PRECAPTURE AE STATE = ");
                stringBuilder2.append(afState);
                LogFilter.i("MorphoCamera2State", stringBuilder2.toString());
                if (afState == null || afState.intValue() != 5) {
                    this.mState = -1;
                    autoFocusEnd();
                }
            }
        }
    }

    private void autoFocusEnd() {
        getCameraStartupInfo().gp3Callback.requestUiRunnable(new Runnable() {
            public void run() {
                if (AutoFocusState.this.mCancel) {
                    LogFilter.i("MorphoCamera2State", "AutoFocus canceled.");
                } else {
                    AutoFocusState.this.toNextState();
                }
            }
        });
    }

    private void toNextState() {
        if (CameraConstants.AutoFocusType == 0) {
            CameraState nextState = new PreviewState();
            getCameraStartupInfo().gp3Callback.updateCameraState(nextState);
            nextState.onStart();
            return;
        }
        PreviewState nextState2 = new PreviewState();
        getCameraStartupInfo().gp3Callback.updateCameraState(nextState2);
        nextState2.toTakePictureState(getCameraStartupInfo().capture_mode);
    }
}
