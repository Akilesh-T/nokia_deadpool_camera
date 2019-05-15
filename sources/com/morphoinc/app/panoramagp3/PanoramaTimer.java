package com.morphoinc.app.panoramagp3;

import java.util.Timer;
import java.util.TimerTask;

public class PanoramaTimer {
    private boolean isStarted;
    private PanoramaTimerListener mListner;
    private Object mLock;
    private Timer mTimer;
    private TimerTask mTimerTask;

    public interface PanoramaTimerListener {
        void onTimeout();
    }

    public PanoramaTimer() {
        this.isStarted = false;
        this.mTimer = new Timer();
        this.mLock = new Object();
    }

    public PanoramaTimer(boolean isDeamon) {
        this.isStarted = false;
        this.mTimer = new Timer(isDeamon);
        this.mLock = new Object();
    }

    public boolean isStarted() {
        return this.isStarted;
    }

    public void start(long time) {
        synchronized (this.mLock) {
            if (this.mTimerTask == null && this.mTimer != null) {
                this.mTimerTask = new TimerTask() {
                    public void run() {
                        PanoramaTimer.this.taskCancel();
                        if (PanoramaTimer.this.mListner != null) {
                            PanoramaTimer.this.mListner.onTimeout();
                        }
                    }
                };
                this.mTimer.schedule(this.mTimerTask, time);
                this.isStarted = true;
            }
        }
    }

    public void cancel() {
        taskCancel();
    }

    private void taskCancel() {
        synchronized (this.mLock) {
            if (this.mTimerTask != null) {
                this.mTimerTask.cancel();
                this.mTimerTask = null;
                this.isStarted = false;
            }
        }
    }

    public void release() {
        synchronized (this.mLock) {
            if (this.mTimerTask != null) {
                this.mTimerTask.cancel();
                this.mTimerTask = null;
                this.isStarted = false;
            }
            if (this.mTimer != null) {
                this.mTimer.cancel();
                this.mTimer = null;
            }
        }
    }

    public void setTimerListener(PanoramaTimerListener listener) {
        this.mListner = listener;
    }
}
