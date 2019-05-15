package com.hmdglobal.app.camera.beauty.camera;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.os.Build.VERSION;
import com.morphoinc.utils.multimedia.MediaProviderUtils;

public class CameraHelper {
    private final CameraHelperImpl mImpl;

    public interface CameraHelperImpl {
        void getCameraInfo(int i, CameraInfo2 cameraInfo2);

        int getNumberOfCameras();

        boolean hasCamera(int i);

        Camera openCamera(int i);

        Camera openCameraFacing(int i);

        Camera openDefaultCamera();
    }

    public static class CameraInfo2 {
        public int facing;
        public int orientation;
    }

    public CameraHelper(Context context) {
        if (VERSION.SDK_INT >= 9) {
            this.mImpl = new CameraHelperGB();
        } else {
            this.mImpl = new CameraHelperBase(context);
        }
    }

    public int getNumberOfCameras() {
        return this.mImpl.getNumberOfCameras();
    }

    public Camera openCamera(int id) {
        return this.mImpl.openCamera(id);
    }

    public Camera openDefaultCamera() {
        return this.mImpl.openDefaultCamera();
    }

    public Camera openFrontCamera() {
        return this.mImpl.openCameraFacing(1);
    }

    public Camera openBackCamera() {
        return this.mImpl.openCameraFacing(0);
    }

    public boolean hasFrontCamera() {
        return this.mImpl.hasCamera(1);
    }

    public boolean hasBackCamera() {
        return this.mImpl.hasCamera(0);
    }

    public void getCameraInfo(int cameraId, CameraInfo2 cameraInfo) {
        this.mImpl.getCameraInfo(cameraId, cameraInfo);
    }

    public void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
        camera.setDisplayOrientation(getCameraDisplayOrientation(activity, cameraId));
    }

    public int getCameraDisplayOrientation(Activity activity, int cameraId) {
        int degrees = 0;
        switch (activity.getWindowManager().getDefaultDisplay().getRotation()) {
            case 0:
                degrees = 0;
                break;
            case 1:
                degrees = 90;
                break;
            case 2:
                degrees = MediaProviderUtils.ROTATION_180;
                break;
            case 3:
                degrees = MediaProviderUtils.ROTATION_270;
                break;
        }
        CameraInfo2 info = new CameraInfo2();
        getCameraInfo(cameraId, info);
        if (info.facing == 1) {
            return (info.orientation + degrees) % 360;
        }
        return ((info.orientation - degrees) + 360) % 360;
    }
}
