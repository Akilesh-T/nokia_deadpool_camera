package com.hmdglobal.app.camera.util;

import com.android.ex.camera2.portability.Size;
import com.hmdglobal.app.camera.CameraActivity;
import com.hmdglobal.app.camera.motion.MotionPictureHelper;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.settings.SettingsUtil;

public class ResolutionUtil {
    public static boolean getRightResolutionById(int moduleId, int cameraId, CameraActivity activity) {
        int i = moduleId;
        int cameraId2 = cameraId;
        Size size = null;
        SettingsManager settingsManager = activity.getSettingsManager();
        boolean z = true;
        String pictureSizeKeyBokeh;
        Size sizeBokeh;
        float ratioBokeh;
        if (i == 0) {
            if (cameraId2 == 1) {
                pictureSizeKeyBokeh = Keys.KEY_PICTURE_SIZE_FRONT;
            } else {
                pictureSizeKeyBokeh = Keys.KEY_PICTURE_SIZE_BACK;
            }
            if (cameraId2 != 1) {
                z = false;
            }
            sizeBokeh = SettingsUtil.sizeFromString(settingsManager.getString(SettingsManager.SCOPE_GLOBAL, pictureSizeKeyBokeh, SettingsUtil.getDefaultPictureSize(z)));
            ratioBokeh = ((float) sizeBokeh.width()) / ((float) sizeBokeh.height());
            if (ratioBokeh == 1.3333334f) {
                size = new Size(960, MotionPictureHelper.FRAME_HEIGHT_9);
            } else if (ratioBokeh == 1.7777778f) {
                size = new Size(1280, MotionPictureHelper.FRAME_HEIGHT_9);
            }
        } else if (i != 15) {
            switch (i) {
                case 4:
                    if (cameraId2 == 3) {
                        cameraId2 = 0;
                    }
                    if (cameraId2 == 1) {
                        pictureSizeKeyBokeh = Keys.KEY_PICTURE_SIZE_FRONT;
                    } else {
                        pictureSizeKeyBokeh = Keys.KEY_PICTURE_SIZE_BACK;
                    }
                    if (cameraId2 != 1) {
                        z = false;
                    }
                    sizeBokeh = SettingsUtil.sizeFromString(settingsManager.getString(SettingsManager.SCOPE_GLOBAL, pictureSizeKeyBokeh, SettingsUtil.getDefaultPictureSize(z)));
                    ratioBokeh = ((float) sizeBokeh.width()) / ((float) sizeBokeh.height());
                    if (ratioBokeh != 1.3333334f) {
                        if (ratioBokeh != 1.7777778f) {
                            if (ratioBokeh == 2.111675f) {
                                size = new Size(3264, 1546);
                                break;
                            }
                        }
                        size = new Size(1280, MotionPictureHelper.FRAME_HEIGHT_9);
                        break;
                    }
                    size = new Size(960, MotionPictureHelper.FRAME_HEIGHT_9);
                    break;
                    break;
                case 5:
                    String videoSizeKey;
                    if (cameraId2 == 1) {
                        videoSizeKey = Keys.KEY_VIDEO_QUALITY_FRONT;
                    } else {
                        videoSizeKey = Keys.KEY_VIDEO_QUALITY_BACK;
                    }
                    if (settingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, videoSizeKey, Integer.valueOf(6)).intValue() != 6) {
                        size = new Size(1280, MotionPictureHelper.FRAME_HEIGHT_9);
                        break;
                    }
                    size = new Size(1920, 1080);
                    break;
            }
        } else {
            size = new Size(1200, 1200);
        }
        if (size == null) {
            return false;
        }
        if (size.width() == activity.getCameraAppUI().getCameraGLSurfaceView().mCameraPreviewWidth && size.height() == activity.getCameraAppUI().getCameraGLSurfaceView().mCameraPreviewHeight) {
            return false;
        }
        activity.getCameraAppUI().setSurfaceHeight(size.height());
        activity.getCameraAppUI().setSurfaceWidth(size.width());
        return true;
    }
}
