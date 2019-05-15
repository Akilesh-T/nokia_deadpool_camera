package com.hmdglobal.app.camera;

import com.hmdglobal.app.camera.app.AppController;

public class VideoCaptureIntentModule extends VideoModule {
    private static final String VIDEO_CAPTURE_MODULE_STRING_ID = "VideoCaptureModule";

    public VideoCaptureIntentModule(AppController app) {
        super(app);
    }

    /* Access modifiers changed, original: protected */
    public boolean isNeedStartRecordingOnSwitching() {
        return false;
    }

    public int getModuleId() {
        return this.mAppController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_video_capture);
    }

    public String getModuleStringIdentifier() {
        return VIDEO_CAPTURE_MODULE_STRING_ID;
    }
}
