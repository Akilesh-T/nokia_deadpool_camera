package com.hmdglobal.app.camera.one.v1;

import android.os.Handler;
import com.hmdglobal.app.camera.one.OneCamera.Facing;
import com.hmdglobal.app.camera.one.OneCamera.OpenCallback;
import com.hmdglobal.app.camera.one.OneCameraManager;
import com.hmdglobal.app.camera.util.Size;

public class OneCameraManagerImpl extends OneCameraManager {
    public void open(Facing facing, boolean enableHdr, Size pictureSize, OpenCallback callback, Handler handler) {
        throw new RuntimeException("Not implemented yet.");
    }

    public boolean hasCameraFacing(Facing facing) {
        throw new RuntimeException("Not implemented yet.");
    }
}
