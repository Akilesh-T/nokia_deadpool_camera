package com.hmdglobal.app.camera.app;

import android.os.Handler;

public interface OrientationManager {
    public static final int ORIENTATION_UNKNOWN = -1;

    public interface OnOrientationChangeListener {
        void onOrientationChanged(int i);
    }

    void addOnOrientationChangeListener(Handler handler, OnOrientationChangeListener onOrientationChangeListener);

    int getDisplayRotation();

    boolean isOrientationLocked();

    void lockOrientation();

    void removeOnOrientationChangeListener(Handler handler, OnOrientationChangeListener onOrientationChangeListener);

    void unlockOrientation();
}
