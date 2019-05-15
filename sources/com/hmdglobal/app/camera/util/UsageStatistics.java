package com.hmdglobal.app.camera.util;

import android.content.Context;
import com.hmdglobal.app.camera.exif.ExifInterface;
import com.hmdglobal.app.camera.ui.TouchCoordinate;
import java.util.HashMap;

public class UsageStatistics {
    public static final int NONE = -1;
    public static final long VIEW_TIMEOUT_MILLIS = 0;
    private static UsageStatistics sInstance;

    public static UsageStatistics instance() {
        if (sInstance == null) {
            sInstance = new UsageStatistics();
        }
        return sInstance;
    }

    public void initialize(Context context) {
    }

    public void mediaInteraction(String ref, int interactionType, int cause, float age) {
    }

    public void mediaView(String ref, long modifiedMillis, float zoom) {
    }

    public void foregrounded(int source, int mode) {
    }

    public void backgrounded() {
    }

    public void storageWarning(long storageSpace) {
    }

    public void videoCaptureDoneEvent(String ref, long durationMsec, boolean front, float zoom, int width, int height, long size, String flashSetting, boolean gridLinesOn) {
    }

    public void photoCaptureDoneEvent(int mode, String fileRef, ExifInterface exifRef, boolean front, boolean isHDR, float zoom, String flashSetting, boolean gridLinesOn, Float timerSeconds, TouchCoordinate touch, Boolean volumeButtonShutter) {
    }

    public void cameraFailure(int cause, String info, int agentAction, int agentState) {
    }

    public void changeScreen(int newScreen, Integer interactionCause) {
    }

    public void controlUsed(int control) {
    }

    public void tapToFocus(TouchCoordinate touch, Float duration) {
    }

    public void reportMemoryConsumed(HashMap memoryData, String reportType) {
    }
}
