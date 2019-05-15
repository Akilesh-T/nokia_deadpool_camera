package com.hmdglobal.app.camera.beauty.camera;

import android.content.Context;
import android.hardware.Camera;
import com.hmdglobal.app.camera.beauty.camera.CameraHelper.CameraHelperImpl;
import com.hmdglobal.app.camera.beauty.camera.CameraHelper.CameraInfo2;

public class CameraHelperBase implements CameraHelperImpl {
    private final Context mContext;

    public CameraHelperBase(Context context) {
        this.mContext = context;
    }

    public int getNumberOfCameras() {
        return hasCameraSupport();
    }

    public Camera openCamera(int id) {
        return Camera.open();
    }

    public Camera openDefaultCamera() {
        return Camera.open();
    }

    public boolean hasCamera(int facing) {
        if (facing == 0) {
            return hasCameraSupport();
        }
        return false;
    }

    public void getCameraInfo(int cameraId, CameraInfo2 cameraInfo) {
        cameraInfo.facing = 0;
        cameraInfo.orientation = 90;
    }

    public Camera openCameraFacing(int facing) {
        if (facing == 0) {
            return Camera.open();
        }
        return null;
    }

    private boolean hasCameraSupport() {
        return this.mContext.getPackageManager().hasSystemFeature("android.hardware.camera");
    }
}
