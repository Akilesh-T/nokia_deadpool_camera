package com.android.ex.camera2.blocking;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.util.Log;
import com.android.ex.camera2.utils.StateChangeListener;
import com.android.ex.camera2.utils.StateWaiter;

public class BlockingCaptureCallback extends CaptureCallback {
    public static final int CAPTURE_COMPLETED = 2;
    public static final int CAPTURE_FAILED = 3;
    public static final int CAPTURE_PROGRESSED = 1;
    public static final int CAPTURE_SEQUENCE_ABORTED = 5;
    public static final int CAPTURE_SEQUENCE_COMPLETED = 4;
    public static final int CAPTURE_STARTED = 0;
    private static final String TAG = "BlockingCaptureCallback";
    private static final boolean VERBOSE = Log.isLoggable(TAG, 2);
    private static final String[] sStateNames = new String[]{"CAPTURE_STARTED", "CAPTURE_PROGRESSED", "CAPTURE_COMPLETED", "CAPTURE_FAILED", "CAPTURE_SEQUENCE_COMPLETED", "CAPTURE_SEQUENCE_ABORTED"};
    private final CaptureCallback mProxy;
    private final StateChangeListener mStateChangeListener;
    private final StateWaiter mStateWaiter;

    public BlockingCaptureCallback() {
        this.mStateWaiter = new StateWaiter(sStateNames);
        this.mStateChangeListener = this.mStateWaiter.getListener();
        this.mProxy = null;
    }

    public BlockingCaptureCallback(CaptureCallback listener) {
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

    public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
        if (this.mProxy != null) {
            this.mProxy.onCaptureStarted(session, request, timestamp, frameNumber);
        }
        this.mStateChangeListener.onStateChanged(0);
    }

    public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
        if (this.mProxy != null) {
            this.mProxy.onCaptureProgressed(session, request, partialResult);
        }
        this.mStateChangeListener.onStateChanged(1);
    }

    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
        if (this.mProxy != null) {
            this.mProxy.onCaptureCompleted(session, request, result);
        }
        this.mStateChangeListener.onStateChanged(2);
    }

    public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
        if (this.mProxy != null) {
            this.mProxy.onCaptureFailed(session, request, failure);
        }
        this.mStateChangeListener.onStateChanged(3);
    }

    public void onCaptureSequenceCompleted(CameraCaptureSession session, int sequenceId, long frameNumber) {
        if (this.mProxy != null) {
            this.mProxy.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
        }
        this.mStateChangeListener.onStateChanged(4);
    }

    public void onCaptureSequenceAborted(CameraCaptureSession session, int sequenceId) {
        if (this.mProxy != null) {
            this.mProxy.onCaptureSequenceAborted(session, sequenceId);
        }
        this.mStateChangeListener.onStateChanged(5);
    }
}
