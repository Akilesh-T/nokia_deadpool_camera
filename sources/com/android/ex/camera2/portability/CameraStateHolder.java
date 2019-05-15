package com.android.ex.camera2.portability;

import android.os.SystemClock;
import com.android.ex.camera2.portability.debug.Log;
import com.android.ex.camera2.portability.debug.Log.Tag;

public abstract class CameraStateHolder {
    private static final Tag TAG = new Tag("CamStateHolder");
    private boolean mInvalid = false;
    private int mState;

    private interface ConditionChecker {
        boolean success();
    }

    public CameraStateHolder(int state) {
        setState(state);
    }

    public synchronized void setState(int state) {
        if (this.mState != state) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setState - state = ");
            stringBuilder.append(Integer.toBinaryString(state));
            Log.v(tag, stringBuilder.toString());
        }
        this.mState = state;
        notifyAll();
    }

    public synchronized int getState() {
        return this.mState;
    }

    public synchronized void invalidate() {
        this.mInvalid = true;
    }

    public synchronized boolean isInvalid() {
        return this.mInvalid;
    }

    private boolean waitForCondition(ConditionChecker stateChecker, long timeoutMs) {
        long timeBound = SystemClock.uptimeMillis() + timeoutMs;
        synchronized (this) {
            while (!stateChecker.success()) {
                try {
                    wait(timeoutMs);
                } catch (InterruptedException e) {
                    if (SystemClock.uptimeMillis() > timeBound) {
                        Log.w(TAG, "Timeout waiting.");
                    }
                    return false;
                }
            }
        }
        return true;
    }

    public boolean waitForStates(final int states) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("waitForStates - states = ");
        stringBuilder.append(Integer.toBinaryString(states));
        Log.v(tag, stringBuilder.toString());
        return waitForCondition(new ConditionChecker() {
            public boolean success() {
                return (states | CameraStateHolder.this.getState()) == states;
            }
        }, CameraAgent.CAMERA_OPERATION_TIMEOUT_MS);
    }

    public boolean waitToAvoidStates(final int states) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("waitToAvoidStates - states = ");
        stringBuilder.append(Integer.toBinaryString(states));
        Log.v(tag, stringBuilder.toString());
        return waitForCondition(new ConditionChecker() {
            public boolean success() {
                return (states & CameraStateHolder.this.getState()) == 0;
            }
        }, CameraAgent.CAMERA_OPERATION_TIMEOUT_MS);
    }
}
