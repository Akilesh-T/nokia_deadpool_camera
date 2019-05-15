package com.android.ex.camera2.utils;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import java.util.LinkedList;
import java.util.List;

public class Camera2CaptureCallbackSplitter extends CaptureCallback {
    private final List<CaptureCallback> mRecipients = new LinkedList();

    public Camera2CaptureCallbackSplitter(CaptureCallback... recipients) {
        for (CaptureCallback listener : recipients) {
            if (listener != null) {
                this.mRecipients.add(listener);
            }
        }
    }

    public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
        for (CaptureCallback target : this.mRecipients) {
            target.onCaptureCompleted(session, request, result);
        }
    }

    public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
        for (CaptureCallback target : this.mRecipients) {
            target.onCaptureFailed(session, request, failure);
        }
    }

    public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
        for (CaptureCallback target : this.mRecipients) {
            target.onCaptureProgressed(session, request, partialResult);
        }
    }

    public void onCaptureSequenceAborted(CameraCaptureSession session, int sequenceId) {
        for (CaptureCallback target : this.mRecipients) {
            target.onCaptureSequenceAborted(session, sequenceId);
        }
    }

    public void onCaptureSequenceCompleted(CameraCaptureSession session, int sequenceId, long frameNumber) {
        for (CaptureCallback target : this.mRecipients) {
            target.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
        }
    }

    public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
        for (CaptureCallback target : this.mRecipients) {
            target.onCaptureStarted(session, request, timestamp, frameNumber);
        }
    }
}
