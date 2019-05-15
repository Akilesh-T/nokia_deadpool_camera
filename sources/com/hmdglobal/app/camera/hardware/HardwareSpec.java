package com.hmdglobal.app.camera.hardware;

public interface HardwareSpec {
    boolean isFlashSupported();

    boolean isFrontCameraSupported();

    boolean isHdrPlusSupported();

    boolean isHdrSupported();
}
