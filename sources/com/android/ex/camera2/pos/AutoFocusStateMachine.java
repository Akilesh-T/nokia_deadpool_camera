package com.android.ex.camera2.pos;

import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Builder;
import android.hardware.camera2.CaptureResult;
import android.util.Log;
import com.android.ex.camera2.utils.SysTrace;

public class AutoFocusStateMachine {
    private static final int AF_UNINITIALIZED = -1;
    private static final boolean DEBUG_LOGGING = Log.isLoggable(TAG, 3);
    private static final String TAG = "AutoFocusStateMachine";
    private static final boolean VERBOSE_LOGGING = Log.isLoggable(TAG, 2);
    private int mCurrentAfCookie = -1;
    private int mCurrentAfMode = -1;
    private String mCurrentAfTrace = "";
    private int mCurrentAfTrigger = -1;
    private int mLastAfCookie = 0;
    private int mLastAfMode = -1;
    private int mLastAfState = -1;
    private final AutoFocusStateListener mListener;

    public interface AutoFocusStateListener {
        void onAutoFocusFail(CaptureResult captureResult, boolean z);

        void onAutoFocusInactive(CaptureResult captureResult);

        void onAutoFocusScan(CaptureResult captureResult);

        void onAutoFocusSuccess(CaptureResult captureResult, boolean z);
    }

    public AutoFocusStateMachine(AutoFocusStateListener listener) {
        if (listener != null) {
            this.mListener = listener;
            return;
        }
        throw new IllegalArgumentException("listener should not be null");
    }

    /* JADX WARNING: Missing block: B:55:0x00eb, code skipped:
            return;
     */
    public synchronized void onCaptureCompleted(android.hardware.camera2.CaptureResult r8) {
        /*
        r7 = this;
        monitor-enter(r7);
        if (r8 != 0) goto L_0x000f;
    L_0x0003:
        r0 = "AutoFocusStateMachine";
        r1 = "onCaptureCompleted - missing result, skipping AF update";
        android.util.Log.w(r0, r1);	 Catch:{ all -> 0x000c }
        monitor-exit(r7);
        return;
    L_0x000c:
        r8 = move-exception;
        goto L_0x00ec;
    L_0x000f:
        r0 = android.hardware.camera2.CaptureResult.CONTROL_AF_STATE;	 Catch:{ all -> 0x000c }
        if (r0 != 0) goto L_0x001c;
    L_0x0013:
        r1 = "AutoFocusStateMachine";
        r2 = "onCaptureCompleted - missing android.control.afState key, skipping AF update";
        android.util.Log.e(r1, r2);	 Catch:{ all -> 0x000c }
        monitor-exit(r7);
        return;
    L_0x001c:
        r1 = android.hardware.camera2.CaptureResult.CONTROL_AF_MODE;	 Catch:{ all -> 0x000c }
        if (r1 != 0) goto L_0x0029;
    L_0x0020:
        r2 = "AutoFocusStateMachine";
        r3 = "onCaptureCompleted - missing android.control.afMode key, skipping AF update";
        android.util.Log.e(r2, r3);	 Catch:{ all -> 0x000c }
        monitor-exit(r7);
        return;
    L_0x0029:
        r2 = android.hardware.camera2.CaptureResult.CONTROL_AF_STATE;	 Catch:{ all -> 0x000c }
        r2 = r8.get(r2);	 Catch:{ all -> 0x000c }
        r2 = (java.lang.Integer) r2;	 Catch:{ all -> 0x000c }
        r3 = android.hardware.camera2.CaptureResult.CONTROL_AF_MODE;	 Catch:{ all -> 0x000c }
        r3 = r8.get(r3);	 Catch:{ all -> 0x000c }
        r3 = (java.lang.Integer) r3;	 Catch:{ all -> 0x000c }
        if (r2 != 0) goto L_0x0044;
    L_0x003b:
        r4 = "AutoFocusStateMachine";
        r5 = "onCaptureCompleted - missing android.control.afState !";
        android.util.Log.w(r4, r5);	 Catch:{ all -> 0x000c }
        monitor-exit(r7);
        return;
    L_0x0044:
        if (r3 != 0) goto L_0x004f;
    L_0x0046:
        r4 = "AutoFocusStateMachine";
        r5 = "onCaptureCompleted - missing android.control.afMode !";
        android.util.Log.w(r4, r5);	 Catch:{ all -> 0x000c }
        monitor-exit(r7);
        return;
    L_0x004f:
        r4 = DEBUG_LOGGING;	 Catch:{ all -> 0x000c }
        if (r4 == 0) goto L_0x0071;
    L_0x0053:
        r4 = "AutoFocusStateMachine";
        r5 = new java.lang.StringBuilder;	 Catch:{ all -> 0x000c }
        r5.<init>();	 Catch:{ all -> 0x000c }
        r6 = "onCaptureCompleted - new AF mode = ";
        r5.append(r6);	 Catch:{ all -> 0x000c }
        r5.append(r3);	 Catch:{ all -> 0x000c }
        r6 = " new AF state = ";
        r5.append(r6);	 Catch:{ all -> 0x000c }
        r5.append(r2);	 Catch:{ all -> 0x000c }
        r5 = r5.toString();	 Catch:{ all -> 0x000c }
        android.util.Log.d(r4, r5);	 Catch:{ all -> 0x000c }
    L_0x0071:
        r4 = r7.mLastAfState;	 Catch:{ all -> 0x000c }
        r5 = r2.intValue();	 Catch:{ all -> 0x000c }
        if (r4 != r5) goto L_0x0083;
    L_0x0079:
        r4 = r3.intValue();	 Catch:{ all -> 0x000c }
        r5 = r7.mLastAfMode;	 Catch:{ all -> 0x000c }
        if (r4 != r5) goto L_0x0083;
    L_0x0081:
        monitor-exit(r7);
        return;
    L_0x0083:
        r4 = VERBOSE_LOGGING;	 Catch:{ all -> 0x000c }
        if (r4 == 0) goto L_0x00a5;
    L_0x0087:
        r4 = "AutoFocusStateMachine";
        r5 = new java.lang.StringBuilder;	 Catch:{ all -> 0x000c }
        r5.<init>();	 Catch:{ all -> 0x000c }
        r6 = "onCaptureCompleted - new AF mode = ";
        r5.append(r6);	 Catch:{ all -> 0x000c }
        r5.append(r3);	 Catch:{ all -> 0x000c }
        r6 = " new AF state = ";
        r5.append(r6);	 Catch:{ all -> 0x000c }
        r5.append(r2);	 Catch:{ all -> 0x000c }
        r5 = r5.toString();	 Catch:{ all -> 0x000c }
        android.util.Log.v(r4, r5);	 Catch:{ all -> 0x000c }
    L_0x00a5:
        r4 = r2.intValue();	 Catch:{ all -> 0x000c }
        r7.mLastAfState = r4;	 Catch:{ all -> 0x000c }
        r4 = r3.intValue();	 Catch:{ all -> 0x000c }
        r7.mLastAfMode = r4;	 Catch:{ all -> 0x000c }
        r4 = r2.intValue();	 Catch:{ all -> 0x000c }
        r5 = 1;
        r6 = 0;
        switch(r4) {
            case 0: goto L_0x00e5;
            case 1: goto L_0x00df;
            case 2: goto L_0x00d9;
            case 3: goto L_0x00d3;
            case 4: goto L_0x00ca;
            case 5: goto L_0x00c1;
            case 6: goto L_0x00bb;
            default: goto L_0x00ba;
        };	 Catch:{ all -> 0x000c }
    L_0x00ba:
        goto L_0x00ea;
    L_0x00bb:
        r4 = r7.mListener;	 Catch:{ all -> 0x000c }
        r4.onAutoFocusFail(r8, r6);	 Catch:{ all -> 0x000c }
        goto L_0x00ea;
    L_0x00c1:
        r4 = r7.mListener;	 Catch:{ all -> 0x000c }
        r4.onAutoFocusFail(r8, r5);	 Catch:{ all -> 0x000c }
        r7.endTraceAsync();	 Catch:{ all -> 0x000c }
        goto L_0x00ea;
    L_0x00ca:
        r4 = r7.mListener;	 Catch:{ all -> 0x000c }
        r4.onAutoFocusSuccess(r8, r5);	 Catch:{ all -> 0x000c }
        r7.endTraceAsync();	 Catch:{ all -> 0x000c }
        goto L_0x00ea;
    L_0x00d3:
        r4 = r7.mListener;	 Catch:{ all -> 0x000c }
        r4.onAutoFocusScan(r8);	 Catch:{ all -> 0x000c }
        goto L_0x00ea;
    L_0x00d9:
        r4 = r7.mListener;	 Catch:{ all -> 0x000c }
        r4.onAutoFocusSuccess(r8, r6);	 Catch:{ all -> 0x000c }
        goto L_0x00ea;
    L_0x00df:
        r4 = r7.mListener;	 Catch:{ all -> 0x000c }
        r4.onAutoFocusScan(r8);	 Catch:{ all -> 0x000c }
        goto L_0x00ea;
    L_0x00e5:
        r4 = r7.mListener;	 Catch:{ all -> 0x000c }
        r4.onAutoFocusInactive(r8);	 Catch:{ all -> 0x000c }
    L_0x00ea:
        monitor-exit(r7);
        return;
    L_0x00ec:
        monitor-exit(r7);
        throw r8;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.ex.camera2.pos.AutoFocusStateMachine.onCaptureCompleted(android.hardware.camera2.CaptureResult):void");
    }

    public synchronized void resetState() {
        if (VERBOSE_LOGGING) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("resetState - last state was ");
            stringBuilder.append(this.mLastAfState);
            Log.v(str, stringBuilder.toString());
        }
        this.mLastAfState = -1;
    }

    public synchronized void lockAutoFocus(Builder repeatingBuilder, Builder requestBuilder) {
        if (VERBOSE_LOGGING) {
            Log.v(TAG, "lockAutoFocus");
        }
        if (this.mCurrentAfMode != -1) {
            beginTraceAsync("AFSM_lockAutoFocus");
            this.mCurrentAfTrigger = 1;
            repeatingBuilder.set(CaptureRequest.CONTROL_AF_MODE, Integer.valueOf(this.mCurrentAfMode));
            requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, Integer.valueOf(this.mCurrentAfMode));
            repeatingBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, Integer.valueOf(0));
            requestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, Integer.valueOf(1));
        } else {
            throw new IllegalStateException("AF mode was not enabled");
        }
    }

    public synchronized void unlockAutoFocus(Builder repeatingBuilder, Builder requestBuilder) {
        if (VERBOSE_LOGGING) {
            Log.v(TAG, "unlockAutoFocus");
        }
        if (this.mCurrentAfMode != -1) {
            this.mCurrentAfTrigger = 2;
            repeatingBuilder.set(CaptureRequest.CONTROL_AF_MODE, Integer.valueOf(this.mCurrentAfMode));
            requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, Integer.valueOf(this.mCurrentAfMode));
            repeatingBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, Integer.valueOf(0));
            requestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, Integer.valueOf(2));
        } else {
            throw new IllegalStateException("AF mode was not enabled");
        }
    }

    public synchronized void setActiveAutoFocus(Builder repeatingBuilder, Builder requestBuilder) {
        if (VERBOSE_LOGGING) {
            Log.v(TAG, "setActiveAutoFocus");
        }
        beginTraceAsync("AFSM_setActiveAutoFocus");
        this.mCurrentAfMode = 1;
        repeatingBuilder.set(CaptureRequest.CONTROL_AF_MODE, Integer.valueOf(this.mCurrentAfMode));
        requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, Integer.valueOf(this.mCurrentAfMode));
        repeatingBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, Integer.valueOf(0));
        requestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, Integer.valueOf(1));
    }

    public synchronized void setPassiveAutoFocus(boolean picture, Builder repeatingBuilder) {
        if (VERBOSE_LOGGING) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setPassiveAutoFocus - picture ");
            stringBuilder.append(picture);
            Log.v(str, stringBuilder.toString());
        }
        if (picture) {
            this.mCurrentAfMode = 4;
        } else {
            this.mCurrentAfMode = 3;
        }
        repeatingBuilder.set(CaptureRequest.CONTROL_AF_MODE, Integer.valueOf(this.mCurrentAfMode));
    }

    private synchronized void beginTraceAsync(String sectionName) {
        if (this.mCurrentAfCookie != -1) {
            SysTrace.endSectionAsync(this.mCurrentAfTrace, this.mCurrentAfCookie);
        }
        this.mLastAfCookie++;
        this.mCurrentAfCookie = this.mLastAfCookie;
        this.mCurrentAfTrace = sectionName;
        SysTrace.beginSectionAsync(sectionName, this.mCurrentAfCookie);
    }

    private synchronized void endTraceAsync() {
        if (this.mCurrentAfCookie == -1) {
            Log.w(TAG, "endTraceAsync - no current trace active");
            return;
        }
        SysTrace.endSectionAsync(this.mCurrentAfTrace, this.mCurrentAfCookie);
        this.mCurrentAfCookie = -1;
    }

    public synchronized void updateCaptureRequest(Builder repeatingBuilder) {
        if (repeatingBuilder == null) {
            throw new IllegalArgumentException("repeatingBuilder shouldn't be null");
        } else if (this.mCurrentAfMode != -1) {
            repeatingBuilder.set(CaptureRequest.CONTROL_AF_MODE, Integer.valueOf(this.mCurrentAfMode));
        } else {
            throw new IllegalStateException("AF mode was not enabled");
        }
    }
}
