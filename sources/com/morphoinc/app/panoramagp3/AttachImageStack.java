package com.morphoinc.app.panoramagp3;

class AttachImageStack {
    private static final Object mSyncObj = new Object();
    private CaptureImage mImageStack = null;

    AttachImageStack() {
    }

    public void push(CaptureImage image) {
        synchronized (mSyncObj) {
            if (this.mImageStack != null) {
                this.mImageStack.close();
            }
            this.mImageStack = image;
        }
    }

    public final CaptureImage pop() {
        CaptureImage image = null;
        synchronized (mSyncObj) {
            if (this.mImageStack != null) {
                image = this.mImageStack;
                this.mImageStack = null;
            }
        }
        return image;
    }

    public void init() {
        push(null);
    }

    public int length() {
        synchronized (mSyncObj) {
            if (this.mImageStack != null) {
                return 1;
            }
            return 0;
        }
    }
}
