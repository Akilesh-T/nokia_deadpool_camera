package com.android.ex.camera2.blocking;

import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraDevice.StateCallback;
import android.os.SystemClock;
import android.util.Log;
import com.android.ex.camera2.exceptions.TimeoutRuntimeException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class BlockingStateCallback extends StateCallback {
    private static int NUM_STATES = 4;
    public static final int STATE_CLOSED = 1;
    public static final int STATE_DISCONNECTED = 2;
    public static final int STATE_ERROR = 3;
    public static final int STATE_OPENED = 0;
    public static final int STATE_UNINITIALIZED = -1;
    private static final String TAG = "BlockingStateCallback";
    private static final boolean VERBOSE = Log.isLoggable(TAG, 2);
    private static final String[] mStateNames = new String[]{"STATE_UNINITIALIZED", "STATE_OPENED", "STATE_CLOSED", "STATE_DISCONNECTED", "STATE_ERROR"};
    private final Object mLock;
    private final StateCallback mProxy;
    private final LinkedBlockingQueue<Integer> mRecentStates;
    private boolean mWaiting;

    private void setCurrentState(int state) {
        if (VERBOSE) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Camera device state now ");
            stringBuilder.append(stateToString(state));
            Log.v(str, stringBuilder.toString());
        }
        try {
            this.mRecentStates.put(Integer.valueOf(state));
        } catch (InterruptedException e) {
            throw new RuntimeException("Unable to set device state", e);
        }
    }

    public BlockingStateCallback() {
        this.mLock = new Object();
        this.mWaiting = false;
        this.mRecentStates = new LinkedBlockingQueue();
        this.mProxy = null;
    }

    public BlockingStateCallback(StateCallback listener) {
        this.mLock = new Object();
        this.mWaiting = false;
        this.mRecentStates = new LinkedBlockingQueue();
        this.mProxy = listener;
    }

    public void onOpened(CameraDevice camera) {
        if (this.mProxy != null) {
            this.mProxy.onOpened(camera);
        }
        setCurrentState(0);
    }

    public void onDisconnected(CameraDevice camera) {
        if (this.mProxy != null) {
            this.mProxy.onDisconnected(camera);
        }
        setCurrentState(2);
    }

    public void onError(CameraDevice camera, int error) {
        if (this.mProxy != null) {
            this.mProxy.onError(camera, error);
        }
        setCurrentState(3);
    }

    public void onClosed(CameraDevice camera) {
        if (this.mProxy != null) {
            this.mProxy.onClosed(camera);
        }
        setCurrentState(1);
    }

    public void waitForState(int state, long timeout) {
        waitForAnyOfStates(Arrays.asList(new Integer[]{Integer.valueOf(state)}), timeout);
    }

    public int waitForAnyOfStates(Collection<Integer> states, long timeout) {
        StringBuilder s;
        Integer nextState;
        InterruptedException e;
        synchronized (this.mLock) {
            if (this.mWaiting) {
                throw new IllegalStateException("Only one waiter allowed at a time");
            }
            this.mWaiting = true;
        }
        if (VERBOSE) {
            s = new StringBuilder("Waiting for state(s) ");
            appendStates(s, states);
            Log.v(TAG, s.toString());
        }
        Integer nextState2 = null;
        long timeoutLeft = timeout;
        long startMs = SystemClock.elapsedRealtime();
        while (true) {
            try {
                Integer num = (Integer) this.mRecentStates.poll(timeoutLeft, TimeUnit.MILLISECONDS);
                nextState = num;
                if (num == null) {
                    break;
                }
                try {
                    if (VERBOSE) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("  Saw transition to ");
                        stringBuilder.append(stateToString(nextState.intValue()));
                        Log.v(str, stringBuilder.toString());
                    }
                    if (states.contains(nextState)) {
                        break;
                    }
                    long endMs = SystemClock.elapsedRealtime();
                    timeoutLeft -= endMs - startMs;
                    startMs = endMs;
                    nextState2 = nextState;
                } catch (InterruptedException e2) {
                    e = e2;
                    throw new UnsupportedOperationException("Does not support interrupts on waits", e);
                }
            } catch (InterruptedException e3) {
                nextState = nextState2;
                e = e3;
                throw new UnsupportedOperationException("Does not support interrupts on waits", e);
            }
        }
        synchronized (this.mLock) {
            this.mWaiting = false;
        }
        if (states.contains(nextState)) {
            return nextState.intValue();
        }
        s = new StringBuilder("Timed out after ");
        s.append(timeout);
        s.append(" ms waiting for state(s) ");
        appendStates(s, states);
        throw new TimeoutRuntimeException(s.toString());
    }

    public static String stateToString(int state) {
        return mStateNames[state + 1];
    }

    public static void appendStates(StringBuilder s, Collection<Integer> states) {
        boolean start = true;
        for (Integer state : states) {
            if (!start) {
                s.append(" ");
            }
            s.append(stateToString(state.intValue()));
            start = false;
        }
    }
}
