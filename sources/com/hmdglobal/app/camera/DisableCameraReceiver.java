package com.hmdglobal.app.camera;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;

public class DisableCameraReceiver extends BroadcastReceiver {
    private static final String[] ACTIVITIES = new String[]{"com.hmdglobal.app.camera.CameraLauncher"};
    private static final boolean CHECK_BACK_CAMERA_ONLY = true;
    private static final Tag TAG = new Tag("DisableCamRcver");

    public void onReceive(Context context, Intent intent) {
        if (!hasBackCamera()) {
            Log.i(TAG, "disable all camera activities");
            for (String disableComponent : ACTIVITIES) {
                disableComponent(context, disableComponent);
            }
        }
        disableComponent(context, "com.hmdglobal.app.camera.DisableCameraReceiver");
    }

    private boolean hasCamera() {
        int n = Camera.getNumberOfCameras();
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("number of camera: ");
        stringBuilder.append(n);
        Log.i(tag, stringBuilder.toString());
        return n > 0;
    }

    private boolean hasBackCamera() {
        int n = Camera.getNumberOfCameras();
        CameraInfo info = new CameraInfo();
        for (int i = 0; i < n; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == 0) {
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("back camera found: ");
                stringBuilder.append(i);
                Log.i(tag, stringBuilder.toString());
                return true;
            }
        }
        Log.i(TAG, "no back camera");
        return false;
    }

    private void disableComponent(Context context, String klass) {
        context.getPackageManager().setComponentEnabledSetting(new ComponentName(context, klass), 2, 1);
    }
}
