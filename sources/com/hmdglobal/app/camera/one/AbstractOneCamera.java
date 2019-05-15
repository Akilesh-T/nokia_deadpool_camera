package com.hmdglobal.app.camera.one;

import android.os.Build;
import com.hmdglobal.app.camera.one.OneCamera.CameraErrorListener;
import com.hmdglobal.app.camera.one.OneCamera.FocusStateListener;
import com.hmdglobal.app.camera.one.OneCamera.ReadyStateChangedListener;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public abstract class AbstractOneCamera implements OneCamera {
    static final int DEBUG_FOLDER_SERIAL_LENGTH = 4;
    protected CameraErrorListener mCameraErrorListener;
    protected FocusStateListener mFocusStateListener;
    protected ReadyStateChangedListener mReadyStateChangedListener;

    public final void setCameraErrorListener(CameraErrorListener listener) {
        this.mCameraErrorListener = listener;
    }

    public final void setFocusStateListener(FocusStateListener listener) {
        this.mFocusStateListener = listener;
    }

    public void setReadyStateChangedListener(ReadyStateChangedListener listener) {
        this.mReadyStateChangedListener = listener;
    }

    protected static String makeDebugDir(File root, String folderName) {
        if (root == null) {
            return null;
        }
        if (root.exists() && root.isDirectory()) {
            String serialSubstring = "";
            String serial = Build.SERIAL;
            if (serial != null) {
                int length = serial.length();
                if (length > 4) {
                    serialSubstring = serial.substring(length - 4, length);
                } else {
                    serialSubstring = serial;
                }
            }
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS");
            simpleDateFormat.setTimeZone(TimeZone.getDefault());
            String currentDateAndTime = simpleDateFormat.format(new Date());
            File destFolder = new File(new File(root, folderName), String.format("%s_%s", new Object[]{serialSubstring, currentDateAndTime}));
            if (destFolder.mkdirs()) {
                return destFolder.getAbsolutePath();
            }
            throw new RuntimeException("Could not create Gcam debug data folder.");
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Gcam debug directory not valid or doesn't exist: ");
        stringBuilder.append(root.getAbsolutePath());
        throw new RuntimeException(stringBuilder.toString());
    }

    /* Access modifiers changed, original: protected */
    public void broadcastReadyState(boolean readyForCapture) {
        if (this.mReadyStateChangedListener != null) {
            this.mReadyStateChangedListener.onReadyStateChanged(readyForCapture);
        }
    }

    public float getMaxZoom() {
        return 1.0f;
    }

    public void setZoom(float zoom) {
    }
}
