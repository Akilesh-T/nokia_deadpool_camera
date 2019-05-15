package com.hmdglobal.app.camera.hardware;

import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraCapabilities.FlashMode;
import com.android.ex.camera2.portability.CameraCapabilities.SceneMode;
import com.hmdglobal.app.camera.app.CameraProvider;
import com.hmdglobal.app.camera.util.GcamHelper;

public class HardwareSpecImpl implements HardwareSpec {
    private final boolean mIsFlashSupported;
    private final boolean mIsFrontCameraSupported;
    private final boolean mIsHdrPlusSupported;
    private final boolean mIsHdrSupported;

    public HardwareSpecImpl(CameraProvider provider, CameraCapabilities capabilities) {
        this.mIsFrontCameraSupported = provider.getFirstFrontCameraId() != -1;
        this.mIsHdrSupported = capabilities.supports(SceneMode.HDR);
        this.mIsHdrPlusSupported = GcamHelper.hasGcamCapture();
        this.mIsFlashSupported = isFlashSupported(capabilities);
    }

    public boolean isFrontCameraSupported() {
        return this.mIsFrontCameraSupported;
    }

    public boolean isHdrSupported() {
        return this.mIsHdrSupported;
    }

    public boolean isHdrPlusSupported() {
        return this.mIsHdrPlusSupported;
    }

    public boolean isFlashSupported() {
        return this.mIsFlashSupported;
    }

    private boolean isFlashSupported(CameraCapabilities capabilities) {
        return capabilities.supports(FlashMode.AUTO) || capabilities.supports(FlashMode.ON);
    }
}
