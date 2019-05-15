package com.hmdglobal.app.camera.one;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import com.hmdglobal.app.camera.CameraActivity;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.one.OneCamera.Facing;
import com.hmdglobal.app.camera.one.OneCamera.OpenCallback;
import com.hmdglobal.app.camera.one.v1.OneCameraManagerImpl;
import com.hmdglobal.app.camera.util.ApiHelper;
import com.hmdglobal.app.camera.util.Size;

public abstract class OneCameraManager {
    private static Tag TAG = new Tag("OneCameraManager");

    public abstract boolean hasCameraFacing(Facing facing);

    public abstract void open(Facing facing, boolean z, Size size, OpenCallback openCallback, Handler handler);

    public static OneCameraManager get(CameraActivity activity) throws OneCameraException {
        return create(activity);
    }

    private static OneCameraManager create(CameraActivity activity) throws OneCameraException {
        CameraManager cameraManager;
        DisplayMetrics displayMetrics = getDisplayMetrics(activity);
        CameraManager cameraManager2 = null;
        CameraManager cameraManager3 = null;
        try {
            if (ApiHelper.HAS_CAMERA_2_API) {
                cameraManager2 = (CameraManager) activity.getSystemService("camera");
            }
            cameraManager = cameraManager2;
        } catch (IllegalStateException ex) {
            Log.e(TAG, "Could not get camera service v2", ex);
            cameraManager = null;
        }
        if (cameraManager == null || !isCamera2Supported(cameraManager)) {
            return new OneCameraManagerImpl();
        }
        return new com.hmdglobal.app.camera.one.v2.OneCameraManagerImpl(activity.getAndroidContext(), cameraManager, activity.getServices().getMemoryManager().getMaxAllowedNativeMemoryAllocation(), displayMetrics, activity.getSoundPlayer());
    }

    private static boolean isCamera2Supported(CameraManager cameraManager) throws OneCameraException {
        boolean z = false;
        if (!ApiHelper.HAS_CAMERA_2_API) {
            return false;
        }
        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            if (cameraIds.length != 0) {
                if (((Integer) cameraManager.getCameraCharacteristics(cameraIds[0]).get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)).intValue() != 2) {
                    z = true;
                }
                return z;
            }
            throw new OneCameraException("Camera 2 API supported but no devices available.");
        } catch (CameraAccessException e) {
            Log.e(TAG, "Could not access camera to determine hardware-level API support.");
            return false;
        }
    }

    private static DisplayMetrics getDisplayMetrics(Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService("window");
        if (wm == null) {
            return displayMetrics;
        }
        displayMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics;
    }
}
