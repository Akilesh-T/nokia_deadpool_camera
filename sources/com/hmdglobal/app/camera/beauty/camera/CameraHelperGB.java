package com.hmdglobal.app.camera.beauty.camera;

import android.annotation.TargetApi;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import com.hmdglobal.app.camera.beauty.camera.CameraHelper.CameraHelperImpl;
import com.hmdglobal.app.camera.beauty.camera.CameraHelper.CameraInfo2;

@TargetApi(9)
public class CameraHelperGB implements CameraHelperImpl {
    public int getNumberOfCameras() {
        return Camera.getNumberOfCameras();
    }

    public Camera openCamera(int id) {
        return Camera.open(id);
    }

    public Camera openDefaultCamera() {
        return Camera.open(0);
    }

    public boolean hasCamera(int facing) {
        return getCameraId(facing) != -1;
    }

    public Camera openCameraFacing(int facing) {
        return Camera.open(getCameraId(facing));
    }

    public void getCameraInfo(int cameraId, CameraInfo2 cameraInfo) {
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        cameraInfo.facing = info.facing;
        cameraInfo.orientation = info.orientation;
    }

    private int getCameraId(int facing) {
        int numberOfCameras = Camera.getNumberOfCameras();
        CameraInfo info = new CameraInfo();
        for (int id = 0; id < numberOfCameras; id++) {
            Camera.getCameraInfo(id, info);
            if (info.facing == facing) {
                return id;
            }
        }
        return -1;
    }
}
