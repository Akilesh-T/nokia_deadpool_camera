package com.hmdglobal.app.camera.ui;

public interface Lockable {
    boolean isLocked();

    int lock();

    void lockSelf();

    void unLockSelf();

    boolean unlockWithToken(int i);
}
