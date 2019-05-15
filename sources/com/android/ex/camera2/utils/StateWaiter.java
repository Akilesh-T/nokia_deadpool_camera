package com.android.ex.camera2.utils;

import android.os.SystemClock;
import android.util.Log;
import com.android.ex.camera2.exceptions.TimeoutRuntimeException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class StateWaiter {
    private static final String TAG = "StateWaiter";
    private static final boolean VERBOSE = Log.isLoggable(TAG, 2);
    private final StateChangeListener mListener;
    private final LinkedBlockingQueue<Integer> mQueuedStates = new LinkedBlockingQueue();
    private final int mStateCount;
    private final String[] mStateNames;
    private final AtomicBoolean mWaiting = new AtomicBoolean(false);

    public StateWaiter(String[] stateNames) {
        this.mStateCount = stateNames.length;
        this.mStateNames = new String[this.mStateCount];
        System.arraycopy(stateNames, 0, this.mStateNames, 0, this.mStateCount);
        this.mListener = new StateChangeListener() {
            public void onStateChanged(int state) {
                StateWaiter.this.queueStateTransition(StateWaiter.this.checkStateInRange(state));
            }
        };
    }

    public StateChangeListener getListener() {
        return this.mListener;
    }

    public void waitForState(int state, long timeoutMs) {
        waitForAnyOfStates(Arrays.asList(new Integer[]{Integer.valueOf(checkStateInRange(state))}), timeoutMs);
    }

    public int waitForAnyOfStates(Collection<Integer> states, long timeoutMs) {
        checkStateCollectionInRange(states);
        if (this.mWaiting.getAndSet(true)) {
            throw new IllegalStateException("Only one waiter allowed at a time");
        }
        try {
            Integer nextState;
            if (VERBOSE) {
                StringBuilder s = new StringBuilder("Waiting for state(s) ");
                appendStateNames(s, states);
                Log.v(TAG, s.toString());
            }
            long timeoutLeft = timeoutMs;
            long startMs = SystemClock.elapsedRealtime();
            while (true) {
                Integer num = (Integer) this.mQueuedStates.poll(timeoutLeft, TimeUnit.MILLISECONDS);
                nextState = num;
                if (num == null) {
                    break;
                }
                if (VERBOSE) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("  Saw transition to ");
                    stringBuilder.append(getStateName(nextState.intValue()));
                    Log.v(str, stringBuilder.toString());
                }
                if (states.contains(nextState)) {
                    break;
                }
                long endMs = SystemClock.elapsedRealtime();
                timeoutLeft -= endMs - startMs;
                startMs = endMs;
            }
            this.mWaiting.set(false);
            if (states.contains(nextState)) {
                return nextState.intValue();
            }
            StringBuilder s2 = new StringBuilder("Timed out after ");
            s2.append(timeoutMs);
            s2.append(" ms waiting for state(s) ");
            appendStateNames(s2, states);
            throw new TimeoutRuntimeException(s2.toString());
        } catch (InterruptedException e) {
            throw new UnsupportedOperationException("Does not support interrupts on waits", e);
        } catch (Throwable th) {
            this.mWaiting.set(false);
        }
    }

    public String getStateName(int state) {
        return this.mStateNames[checkStateInRange(state)];
    }

    public void appendStateNames(StringBuilder s, Collection<Integer> states) {
        checkStateCollectionInRange(states);
        boolean start = true;
        for (Integer state : states) {
            if (!start) {
                s.append(" ");
            }
            s.append(getStateName(state.intValue()));
            start = false;
        }
    }

    private void queueStateTransition(int state) {
        if (VERBOSE) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setCurrentState - state now ");
            stringBuilder.append(getStateName(state));
            Log.v(str, stringBuilder.toString());
        }
        try {
            this.mQueuedStates.put(Integer.valueOf(state));
        } catch (InterruptedException e) {
            throw new UnsupportedOperationException("Unable to set current state", e);
        }
    }

    private int checkStateInRange(int state) {
        if (state >= 0 && state < this.mStateCount) {
            return state;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("State out of range ");
        stringBuilder.append(state);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private Collection<Integer> checkStateCollectionInRange(Collection<Integer> states) {
        for (Integer state : states) {
            checkStateInRange(state.intValue());
        }
        return states;
    }
}
