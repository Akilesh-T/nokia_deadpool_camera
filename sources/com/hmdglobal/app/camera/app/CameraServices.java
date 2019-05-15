package com.hmdglobal.app.camera.app;

import com.hmdglobal.app.camera.remote.RemoteShutterListener;
import com.hmdglobal.app.camera.session.CaptureSessionManager;
import com.hmdglobal.app.camera.settings.SettingsManager;

public interface CameraServices {
    CaptureSessionManager getCaptureSessionManager();

    @Deprecated
    MediaSaver getMediaSaver();

    MemoryManager getMemoryManager();

    MotionManager getMotionManager();

    RemoteShutterListener getRemoteShutterListener();

    SettingsManager getSettingsManager();

    boolean isReversibleEnabled();
}
