package com.hmdglobal.app.camera.app;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.provider.Settings.System;
import android.view.OrientationEventListener;
import com.hmdglobal.app.camera.CameraActivity;
import com.hmdglobal.app.camera.DeviceInfo;
import com.hmdglobal.app.camera.app.OrientationManager.OnOrientationChangeListener;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.util.ApiHelper;
import com.morphoinc.utils.multimedia.MediaProviderUtils;
import java.util.ArrayList;
import java.util.List;

public class OrientationManagerImpl implements OrientationManager {
    private static final int ORIENTATION_HYSTERESIS = 20;
    private static final int ORIENTATION_HYSTERESIS_REVERSIBLEON = 1;
    private static final Tag TAG = new Tag("OrientMgrImpl");
    private final Context mActivity;
    private final List<OrientationChangeCallback> mListeners = new ArrayList();
    private int mOrientation = 0;
    private final MyOrientationEventListener mOrientationListener;
    private boolean mOrientationLocked = false;
    private boolean mRotationLockedSetting = false;

    private class MyOrientationEventListener extends OrientationEventListener {
        public MyOrientationEventListener(Context context) {
            super(context);
        }

        public void onOrientationChanged(int orientation) {
            if (orientation != -1) {
                int roundedOrientation = OrientationManagerImpl.this.roundOrientation(orientation, OrientationManagerImpl.this.mOrientation);
                OrientationManagerImpl.this.mOrientation = roundedOrientation;
                for (OrientationChangeCallback l : OrientationManagerImpl.this.mListeners) {
                    l.postOrientationChangeCallback(roundedOrientation);
                }
            }
        }
    }

    private static class OrientationChangeCallback {
        private final Handler mHandler;
        private final OnOrientationChangeListener mListener;

        OrientationChangeCallback(Handler handler, OnOrientationChangeListener listener) {
            this.mHandler = handler;
            this.mListener = listener;
        }

        public void postOrientationChangeCallback(final int orientation) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    OrientationChangeCallback.this.mListener.onOrientationChanged(orientation);
                }
            });
        }

        public boolean equals(Object o) {
            if (o == null || !(o instanceof OrientationChangeCallback)) {
                return false;
            }
            OrientationChangeCallback c = (OrientationChangeCallback) o;
            if (this.mHandler == c.mHandler && this.mListener == c.mListener) {
                return true;
            }
            return false;
        }
    }

    public OrientationManagerImpl(Context activity) {
        this.mActivity = activity;
        this.mOrientationListener = new MyOrientationEventListener(activity);
    }

    public void resume() {
        boolean z = false;
        if (System.getInt(this.mActivity.getContentResolver(), "accelerometer_rotation", 0) != 1) {
            z = true;
        }
        this.mRotationLockedSetting = z;
        this.mOrientationListener.enable();
    }

    public void pause() {
        this.mOrientationListener.disable();
    }

    public void addOnOrientationChangeListener(Handler handler, OnOrientationChangeListener listener) {
        OrientationChangeCallback callback = new OrientationChangeCallback(handler, listener);
        if (!this.mListeners.contains(callback)) {
            this.mListeners.add(callback);
        }
    }

    public void removeOnOrientationChangeListener(Handler handler, OnOrientationChangeListener listener) {
        if (!this.mListeners.remove(new OrientationChangeCallback(handler, listener))) {
            Log.v(TAG, "Removing non-existing listener.");
        }
    }

    public void lockOrientation() {
        if (!this.mOrientationLocked && !this.mRotationLockedSetting) {
            this.mOrientationLocked = true;
            if (ApiHelper.HAS_ORIENTATION_LOCK) {
                Log.d(TAG, "lock orientation");
                ((CameraActivity) this.mActivity).setRequestedOrientation(14);
            } else {
                ((CameraActivity) this.mActivity).setRequestedOrientation(calculateCurrentScreenOrientation());
            }
        }
    }

    public void unlockOrientation() {
        if (this.mOrientationLocked && !this.mRotationLockedSetting) {
            this.mOrientationLocked = false;
            Log.d(TAG, "unlock orientation");
            ((CameraActivity) this.mActivity).setRequestedOrientation(1);
        }
    }

    public boolean isOrientationLocked() {
        return this.mOrientationLocked || this.mRotationLockedSetting;
    }

    private int calculateCurrentScreenOrientation() {
        int displayRotation = getDisplayRotation();
        boolean z = false;
        int i = 1;
        boolean standard = displayRotation < MediaProviderUtils.ROTATION_180;
        if (this.mActivity.getResources().getConfiguration().orientation == 2) {
            int i2;
            if (!standard) {
                i2 = 8;
            }
            return i2;
        }
        if (displayRotation == 90 || displayRotation == MediaProviderUtils.ROTATION_270) {
            if (!standard) {
                z = true;
            }
            standard = z;
        }
        if (!standard) {
            i = 9;
        }
        return i;
    }

    public int getDisplayRotation() {
        return getDisplayRotation(this.mActivity);
    }

    private int roundOrientation(int orientation, int orientationHistory) {
        boolean changeOrientation;
        if (orientationHistory == -1) {
            changeOrientation = true;
        } else {
            int dist = Math.abs(orientation - orientationHistory);
            changeOrientation = Math.min(dist, 360 - dist) >= 45 + getOrientationHysteresis();
        }
        if (changeOrientation) {
            return (((orientation + 45) / 90) * 90) % 360;
        }
        return orientationHistory;
    }

    private int getOrientationHysteresis() {
        if (this.mActivity != null && DeviceInfo.isReversibleOn(this.mActivity.getContentResolver())) {
            return 1;
        }
        return 20;
    }

    private static int getDisplayRotation(Context activity) {
        if (!(activity instanceof Activity)) {
            return 0;
        }
        switch (((Activity) activity).getWindowManager().getDefaultDisplay().getRotation()) {
            case 0:
                return 0;
            case 1:
                return 90;
            case 2:
                return MediaProviderUtils.ROTATION_180;
            case 3:
                return MediaProviderUtils.ROTATION_270;
            default:
                return 0;
        }
    }
}
