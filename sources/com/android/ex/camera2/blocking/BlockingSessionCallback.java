package com.android.ex.camera2.blocking;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.StateCallback;
import android.os.ConditionVariable;
import android.util.Log;
import com.android.ex.camera2.exceptions.TimeoutRuntimeException;
import com.android.ex.camera2.utils.StateChangeListener;
import com.android.ex.camera2.utils.StateWaiter;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class BlockingSessionCallback extends StateCallback {
    public static final int SESSION_ACTIVE = 3;
    public static final int SESSION_CLOSED = 4;
    public static final int SESSION_CONFIGURED = 0;
    public static final int SESSION_CONFIGURE_FAILED = 1;
    public static final int SESSION_READY = 2;
    private static final String TAG = "BlockingSessionCallback";
    private static final boolean VERBOSE = Log.isLoggable(TAG, 2);
    private static final String[] sStateNames = new String[]{"SESSION_CONFIGURED", "SESSION_CONFIGURE_FAILED", "SESSION_READY", "SESSION_ACTIVE", "SESSION_CLOSED"};
    private final int NUM_STATES;
    private final StateCallback mProxy;
    private final SessionFuture mSessionFuture;
    private final StateChangeListener mStateChangeListener;
    private final StateWaiter mStateWaiter;

    private static class SessionFuture implements Future<CameraCaptureSession> {
        ConditionVariable mCondVar;
        private volatile CameraCaptureSession mSession;

        private SessionFuture() {
            this.mCondVar = new ConditionVariable(false);
        }

        public void setSession(CameraCaptureSession session) {
            this.mSession = session;
            this.mCondVar.open();
        }

        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        public boolean isCancelled() {
            return false;
        }

        public boolean isDone() {
            return this.mSession != null;
        }

        public CameraCaptureSession get() {
            this.mCondVar.block();
            return this.mSession;
        }

        public CameraCaptureSession get(long timeout, TimeUnit unit) throws TimeoutException {
            if (!this.mCondVar.block(unit.convert(timeout, TimeUnit.MILLISECONDS))) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to receive session after ");
                stringBuilder.append(timeout);
                stringBuilder.append(" ");
                stringBuilder.append(unit);
                throw new TimeoutException(stringBuilder.toString());
            } else if (this.mSession != null) {
                return this.mSession;
            } else {
                throw new AssertionError();
            }
        }
    }

    public BlockingSessionCallback() {
        this.NUM_STATES = 5;
        this.mSessionFuture = new SessionFuture();
        this.mStateWaiter = new StateWaiter(sStateNames);
        this.mStateChangeListener = this.mStateWaiter.getListener();
        this.mProxy = null;
    }

    public BlockingSessionCallback(StateCallback listener) {
        this.NUM_STATES = 5;
        this.mSessionFuture = new SessionFuture();
        this.mStateWaiter = new StateWaiter(sStateNames);
        this.mStateChangeListener = this.mStateWaiter.getListener();
        if (listener != null) {
            this.mProxy = listener;
            return;
        }
        throw new NullPointerException("listener must not be null");
    }

    public StateWaiter getStateWaiter() {
        return this.mStateWaiter;
    }

    public CameraCaptureSession waitAndGetSession(long timeoutMs) {
        try {
            return this.mSessionFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new TimeoutRuntimeException(String.format("Failed to get session after %s milliseconds", new Object[]{Long.valueOf(timeoutMs)}), e);
        }
    }

    public void onActive(CameraCaptureSession session) {
        this.mSessionFuture.setSession(session);
        if (this.mProxy != null) {
            this.mProxy.onActive(session);
        }
        this.mStateChangeListener.onStateChanged(3);
    }

    public void onClosed(CameraCaptureSession session) {
        this.mSessionFuture.setSession(session);
        if (this.mProxy != null) {
            this.mProxy.onClosed(session);
        }
        this.mStateChangeListener.onStateChanged(4);
    }

    public void onConfigured(CameraCaptureSession session) {
        this.mSessionFuture.setSession(session);
        if (this.mProxy != null) {
            this.mProxy.onConfigured(session);
        }
        this.mStateChangeListener.onStateChanged(0);
    }

    public void onConfigureFailed(CameraCaptureSession session) {
        this.mSessionFuture.setSession(session);
        if (this.mProxy != null) {
            this.mProxy.onConfigureFailed(session);
        }
        this.mStateChangeListener.onStateChanged(1);
    }

    public void onReady(CameraCaptureSession session) {
        this.mSessionFuture.setSession(session);
        if (this.mProxy != null) {
            this.mProxy.onReady(session);
        }
        this.mStateChangeListener.onStateChanged(2);
    }
}
