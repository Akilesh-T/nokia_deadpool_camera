package com.hmdglobal.app.camera.processing;

import android.content.Context;
import android.location.Location;
import com.hmdglobal.app.camera.app.CameraServices;
import com.hmdglobal.app.camera.session.CaptureSession;

public interface ProcessingTask {

    public static class ProcessingResult {
        public final CaptureSession mSession;
        public final boolean mSuccess;

        public ProcessingResult(boolean success, CaptureSession session) {
            this.mSuccess = success;
            this.mSession = session;
        }
    }

    public interface ProcessingTaskDoneListener {
        void onDone(ProcessingResult processingResult);
    }

    Location getLocation();

    String getName();

    CaptureSession getSession();

    ProcessingResult process(Context context, CameraServices cameraServices, CaptureSession captureSession);

    void resume();

    void setDoneListener(ProcessingTaskDoneListener processingTaskDoneListener);

    void suspend();
}
