package com.android.ex.camera2.utils;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Handler;

public class Camera2CaptureCallbackForwarder extends CaptureCallback {
    private Handler mHandler;
    private CaptureCallback mListener;

    public Camera2CaptureCallbackForwarder(CaptureCallback listener, Handler handler) {
        this.mListener = listener;
        this.mHandler = handler;
    }

    public void onCaptureCompleted(final CameraCaptureSession session, final CaptureRequest request, final TotalCaptureResult result) {
        this.mHandler.post(new Runnable() {
            public void run() {
                Camera2CaptureCallbackForwarder.this.mListener.onCaptureCompleted(session, request, result);
            }
        });
    }

    public void onCaptureFailed(final CameraCaptureSession session, final CaptureRequest request, final CaptureFailure failure) {
        this.mHandler.post(new Runnable() {
            public void run() {
                Camera2CaptureCallbackForwarder.this.mListener.onCaptureFailed(session, request, failure);
            }
        });
    }

    public void onCaptureProgressed(final CameraCaptureSession session, final CaptureRequest request, final CaptureResult partialResult) {
        this.mHandler.post(new Runnable() {
            public void run() {
                Camera2CaptureCallbackForwarder.this.mListener.onCaptureProgressed(session, request, partialResult);
            }
        });
    }

    public void onCaptureSequenceAborted(final CameraCaptureSession session, final int sequenceId) {
        this.mHandler.post(new Runnable() {
            public void run() {
                Camera2CaptureCallbackForwarder.this.mListener.onCaptureSequenceAborted(session, sequenceId);
            }
        });
    }

    public void onCaptureSequenceCompleted(CameraCaptureSession session, int sequenceId, long frameNumber) {
        final CameraCaptureSession cameraCaptureSession = session;
        final int i = sequenceId;
        final long j = frameNumber;
        this.mHandler.post(new Runnable() {
            public void run() {
                Camera2CaptureCallbackForwarder.this.mListener.onCaptureSequenceCompleted(cameraCaptureSession, i, j);
            }
        });
    }

    public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
        final CameraCaptureSession cameraCaptureSession = session;
        final CaptureRequest captureRequest = request;
        final long j = timestamp;
        final long j2 = frameNumber;
        this.mHandler.post(new Runnable() {
            public void run() {
                Camera2CaptureCallbackForwarder.this.mListener.onCaptureStarted(cameraCaptureSession, captureRequest, j, j2);
            }
        });
    }
}
