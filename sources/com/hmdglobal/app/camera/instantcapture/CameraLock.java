package com.hmdglobal.app.camera.instantcapture;

import android.os.ConditionVariable;

public class CameraLock extends ConditionVariable {
    public static final int CAMERA_BLOCK_TIMEOUT = 1000;

    private static class SingletonHolder {
        private static final CameraLock INSTANCE = new CameraLock(true);

        private SingletonHolder() {
        }
    }

    private CameraLock(boolean state) {
        super(state);
    }

    public static CameraLock getInstance() {
        return SingletonHolder.INSTANCE;
    }
}
