package com.hmdglobal.app.camera.ui;

/* compiled from: StereoModeStripView */
class VelocityObserver {
    private long mEndTime = 0;
    private long mStartTime = 0;

    VelocityObserver() {
    }

    private void resetTimer() {
        this.mStartTime = System.currentTimeMillis();
        this.mEndTime = this.mStartTime;
    }

    public float getVelocityX(float deltaX) {
        long gap = this.mEndTime - this.mStartTime;
        float f = 0.0f;
        if (gap == 0) {
            return 0.0f;
        }
        float velocityX = Math.abs(deltaX / ((float) gap));
        if (velocityX > 0.0f) {
            f = velocityX;
        }
        return f;
    }

    public void start() {
        resetTimer();
    }

    public void record() {
        this.mEndTime = System.currentTimeMillis();
    }
}
