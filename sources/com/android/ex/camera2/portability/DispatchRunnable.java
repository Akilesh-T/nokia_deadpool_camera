package com.android.ex.camera2.portability;

/* compiled from: DispatchThread */
abstract class DispatchRunnable implements Runnable {
    private Integer mInstanceAction;

    DispatchRunnable(Integer action) {
        this.mInstanceAction = action;
    }

    public Integer getIntanceAction() {
        return this.mInstanceAction;
    }
}
