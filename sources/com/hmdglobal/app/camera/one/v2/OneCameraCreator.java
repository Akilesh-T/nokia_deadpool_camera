package com.hmdglobal.app.camera.one.v2;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.util.DisplayMetrics;
import com.hmdglobal.app.camera.SoundPlayer;
import com.hmdglobal.app.camera.one.OneCamera;
import com.hmdglobal.app.camera.util.Size;

public class OneCameraCreator {
    public static OneCamera create(Context context, boolean useHdr, CameraDevice device, CameraCharacteristics characteristics, Size pictureSize, int maxMemoryMB, DisplayMetrics displayMetrics, SoundPlayer soundPlayer) {
        return new OneCameraImpl(device, characteristics, pictureSize);
    }
}
