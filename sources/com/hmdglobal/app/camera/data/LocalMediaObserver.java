package com.hmdglobal.app.camera.data;

import android.database.ContentObserver;

public class LocalMediaObserver extends ContentObserver {
    private boolean mActivityPaused = false;
    private ChangeListener mChangeListener;
    private boolean mMediaDataChangedDuringPause = false;

    public interface ChangeListener {
        void onChange();
    }

    public LocalMediaObserver() {
        super(null);
    }

    public void setForegroundChangeListener(ChangeListener changeListener) {
        this.mChangeListener = changeListener;
    }

    public void removeForegroundChangeListener() {
        this.mChangeListener = null;
    }

    public void onChange(boolean selfChange) {
        if (this.mChangeListener != null) {
            this.mChangeListener.onChange();
        }
        if (this.mActivityPaused) {
            this.mMediaDataChangedDuringPause = true;
        }
    }

    public void setActivityPaused(boolean paused) {
        this.mActivityPaused = paused;
        if (!paused) {
            this.mMediaDataChangedDuringPause = false;
        }
    }

    public boolean isMediaDataChangedDuringPause() {
        return this.mMediaDataChangedDuringPause;
    }
}
