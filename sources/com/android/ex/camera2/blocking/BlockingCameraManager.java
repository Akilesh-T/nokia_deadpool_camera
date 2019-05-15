package com.android.ex.camera2.blocking;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraDevice.StateCallback;
import android.hardware.camera2.CameraManager;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.android.ex.camera2.exceptions.TimeoutRuntimeException;
import java.util.Objects;

public class BlockingCameraManager {
    private static final int OPEN_TIME_OUT = 2000;
    private static final String TAG = "CameraBlockingOpener";
    private static final boolean VERBOSE = Log.isLoggable(TAG, 2);
    private final CameraManager mManager;

    public static class BlockingOpenException extends Exception {
        public static final int ERROR_DISCONNECTED = 0;
        private static final long serialVersionUID = 12397123891238912L;
        private final int mError;

        public boolean wasDisconnected() {
            return this.mError == 0;
        }

        public boolean wasError() {
            return this.mError != 0;
        }

        public int getCode() {
            return this.mError;
        }

        public BlockingOpenException(int errorCode, String message) {
            super(message);
            this.mError = errorCode;
        }
    }

    private class OpenListener extends StateCallback {
        private static final int ERROR_UNINITIALIZED = -1;
        private final String mCameraId;
        private CameraDevice mDevice = null;
        private final ConditionVariable mDeviceReady = new ConditionVariable();
        private boolean mDisconnected = false;
        private int mError = -1;
        private final Object mLock = new Object();
        private boolean mNoReply = true;
        private final StateCallback mProxy;
        private boolean mSuccess = false;
        private boolean mTimedOut = false;

        OpenListener(CameraManager manager, String cameraId, StateCallback listener, Handler handler) throws CameraAccessException {
            this.mCameraId = cameraId;
            this.mProxy = listener;
            manager.openCamera(cameraId, this, handler);
        }

        private void assertInitialState() {
            BlockingCameraManager.assertEquals(null, this.mDevice);
            BlockingCameraManager.assertEquals(Boolean.valueOf(false), Boolean.valueOf(this.mDisconnected));
            BlockingCameraManager.assertEquals(Integer.valueOf(-1), Integer.valueOf(this.mError));
            BlockingCameraManager.assertEquals(Boolean.valueOf(false), Boolean.valueOf(this.mSuccess));
        }

        /* JADX WARNING: Missing block: B:18:0x0046, code skipped:
            if (r3.mProxy == null) goto L_0x004d;
     */
        /* JADX WARNING: Missing block: B:19:0x0048, code skipped:
            r3.mProxy.onOpened(r4);
     */
        /* JADX WARNING: Missing block: B:20:0x004d, code skipped:
            return;
     */
        public void onOpened(android.hardware.camera2.CameraDevice r4) {
            /*
            r3 = this;
            r0 = com.android.ex.camera2.blocking.BlockingCameraManager.VERBOSE;
            if (r0 == 0) goto L_0x0025;
        L_0x0006:
            r0 = "CameraBlockingOpener";
            r1 = new java.lang.StringBuilder;
            r1.<init>();
            r2 = "onOpened: camera ";
            r1.append(r2);
            if (r4 == 0) goto L_0x0019;
        L_0x0014:
            r2 = r4.getId();
            goto L_0x001b;
        L_0x0019:
            r2 = "null";
        L_0x001b:
            r1.append(r2);
            r1 = r1.toString();
            android.util.Log.v(r0, r1);
        L_0x0025:
            r0 = r3.mLock;
            monitor-enter(r0);
            r3.assertInitialState();	 Catch:{ all -> 0x004e }
            r1 = 0;
            r3.mNoReply = r1;	 Catch:{ all -> 0x004e }
            r1 = 1;
            r3.mSuccess = r1;	 Catch:{ all -> 0x004e }
            r3.mDevice = r4;	 Catch:{ all -> 0x004e }
            r1 = r3.mDeviceReady;	 Catch:{ all -> 0x004e }
            r1.open();	 Catch:{ all -> 0x004e }
            r1 = r3.mTimedOut;	 Catch:{ all -> 0x004e }
            if (r1 == 0) goto L_0x0043;
        L_0x003c:
            if (r4 == 0) goto L_0x0043;
        L_0x003e:
            r4.close();	 Catch:{ all -> 0x004e }
            monitor-exit(r0);	 Catch:{ all -> 0x004e }
            return;
        L_0x0043:
            monitor-exit(r0);	 Catch:{ all -> 0x004e }
            r0 = r3.mProxy;
            if (r0 == 0) goto L_0x004d;
        L_0x0048:
            r0 = r3.mProxy;
            r0.onOpened(r4);
        L_0x004d:
            return;
        L_0x004e:
            r1 = move-exception;
            monitor-exit(r0);	 Catch:{ all -> 0x004e }
            throw r1;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.ex.camera2.blocking.BlockingCameraManager$OpenListener.onOpened(android.hardware.camera2.CameraDevice):void");
        }

        /* JADX WARNING: Missing block: B:18:0x0046, code skipped:
            if (r3.mProxy == null) goto L_0x004d;
     */
        /* JADX WARNING: Missing block: B:19:0x0048, code skipped:
            r3.mProxy.onDisconnected(r4);
     */
        /* JADX WARNING: Missing block: B:20:0x004d, code skipped:
            return;
     */
        public void onDisconnected(android.hardware.camera2.CameraDevice r4) {
            /*
            r3 = this;
            r0 = com.android.ex.camera2.blocking.BlockingCameraManager.VERBOSE;
            if (r0 == 0) goto L_0x0025;
        L_0x0006:
            r0 = "CameraBlockingOpener";
            r1 = new java.lang.StringBuilder;
            r1.<init>();
            r2 = "onDisconnected: camera ";
            r1.append(r2);
            if (r4 == 0) goto L_0x0019;
        L_0x0014:
            r2 = r4.getId();
            goto L_0x001b;
        L_0x0019:
            r2 = "null";
        L_0x001b:
            r1.append(r2);
            r1 = r1.toString();
            android.util.Log.v(r0, r1);
        L_0x0025:
            r0 = r3.mLock;
            monitor-enter(r0);
            r3.assertInitialState();	 Catch:{ all -> 0x004e }
            r1 = 0;
            r3.mNoReply = r1;	 Catch:{ all -> 0x004e }
            r1 = 1;
            r3.mDisconnected = r1;	 Catch:{ all -> 0x004e }
            r3.mDevice = r4;	 Catch:{ all -> 0x004e }
            r1 = r3.mDeviceReady;	 Catch:{ all -> 0x004e }
            r1.open();	 Catch:{ all -> 0x004e }
            r1 = r3.mTimedOut;	 Catch:{ all -> 0x004e }
            if (r1 == 0) goto L_0x0043;
        L_0x003c:
            if (r4 == 0) goto L_0x0043;
        L_0x003e:
            r4.close();	 Catch:{ all -> 0x004e }
            monitor-exit(r0);	 Catch:{ all -> 0x004e }
            return;
        L_0x0043:
            monitor-exit(r0);	 Catch:{ all -> 0x004e }
            r0 = r3.mProxy;
            if (r0 == 0) goto L_0x004d;
        L_0x0048:
            r0 = r3.mProxy;
            r0.onDisconnected(r4);
        L_0x004d:
            return;
        L_0x004e:
            r1 = move-exception;
            monitor-exit(r0);	 Catch:{ all -> 0x004e }
            throw r1;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.ex.camera2.blocking.BlockingCameraManager$OpenListener.onDisconnected(android.hardware.camera2.CameraDevice):void");
        }

        /* JADX WARNING: Missing block: B:20:0x0044, code skipped:
            if (r3.mProxy == null) goto L_0x004b;
     */
        /* JADX WARNING: Missing block: B:21:0x0046, code skipped:
            r3.mProxy.onError(r4, r5);
     */
        /* JADX WARNING: Missing block: B:22:0x004b, code skipped:
            return;
     */
        public void onError(android.hardware.camera2.CameraDevice r4, int r5) {
            /*
            r3 = this;
            r0 = com.android.ex.camera2.blocking.BlockingCameraManager.VERBOSE;
            if (r0 == 0) goto L_0x0025;
        L_0x0006:
            r0 = "CameraBlockingOpener";
            r1 = new java.lang.StringBuilder;
            r1.<init>();
            r2 = "onError: camera ";
            r1.append(r2);
            if (r4 == 0) goto L_0x0019;
        L_0x0014:
            r2 = r4.getId();
            goto L_0x001b;
        L_0x0019:
            r2 = "null";
        L_0x001b:
            r1.append(r2);
            r1 = r1.toString();
            android.util.Log.v(r0, r1);
        L_0x0025:
            if (r5 <= 0) goto L_0x004f;
        L_0x0027:
            r0 = r3.mLock;
            monitor-enter(r0);
            r1 = 0;
            r3.mNoReply = r1;	 Catch:{ all -> 0x004c }
            r3.mError = r5;	 Catch:{ all -> 0x004c }
            r3.mDevice = r4;	 Catch:{ all -> 0x004c }
            r1 = r3.mDeviceReady;	 Catch:{ all -> 0x004c }
            r1.open();	 Catch:{ all -> 0x004c }
            r1 = r3.mTimedOut;	 Catch:{ all -> 0x004c }
            if (r1 == 0) goto L_0x0041;
        L_0x003a:
            if (r4 == 0) goto L_0x0041;
        L_0x003c:
            r4.close();	 Catch:{ all -> 0x004c }
            monitor-exit(r0);	 Catch:{ all -> 0x004c }
            return;
        L_0x0041:
            monitor-exit(r0);	 Catch:{ all -> 0x004c }
            r0 = r3.mProxy;
            if (r0 == 0) goto L_0x004b;
        L_0x0046:
            r0 = r3.mProxy;
            r0.onError(r4, r5);
        L_0x004b:
            return;
        L_0x004c:
            r1 = move-exception;
            monitor-exit(r0);	 Catch:{ all -> 0x004c }
            throw r1;
        L_0x004f:
            r0 = new java.lang.AssertionError;
            r1 = "Expected error to be a positive number";
            r0.<init>(r1);
            throw r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.ex.camera2.blocking.BlockingCameraManager$OpenListener.onError(android.hardware.camera2.CameraDevice, int):void");
        }

        public void onClosed(CameraDevice camera) {
            if (this.mProxy != null) {
                this.mProxy.onClosed(camera);
            }
        }

        /* Access modifiers changed, original: 0000 */
        public CameraDevice blockUntilOpen() throws BlockingOpenException {
            CameraDevice cameraDevice;
            if (!this.mDeviceReady.block(2000)) {
                synchronized (this.mLock) {
                    if (this.mNoReply) {
                        this.mTimedOut = true;
                        throw new TimeoutRuntimeException(String.format("Timed out after %d ms while trying to open camera device %s", new Object[]{Integer.valueOf(BlockingCameraManager.OPEN_TIME_OUT), this.mCameraId}));
                    }
                }
            }
            synchronized (this.mLock) {
                if (!(this.mSuccess || this.mDevice == null)) {
                    this.mDevice.close();
                }
                if (this.mSuccess) {
                    cameraDevice = this.mDevice;
                } else if (this.mDisconnected) {
                    throw new BlockingOpenException(0, "Failed to open camera device: it is disconnected");
                } else if (this.mError != -1) {
                    int i = this.mError;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to open camera device: error code ");
                    stringBuilder.append(this.mError);
                    throw new BlockingOpenException(i, stringBuilder.toString());
                } else {
                    throw new AssertionError("Failed to open camera device (impl bug)");
                }
            }
            return cameraDevice;
        }
    }

    public BlockingCameraManager(CameraManager manager) {
        if (manager != null) {
            this.mManager = manager;
            return;
        }
        throw new IllegalArgumentException("manager must not be null");
    }

    public CameraDevice openCamera(String cameraId, StateCallback listener, Handler handler) throws CameraAccessException, BlockingOpenException {
        if (handler == null) {
            throw new IllegalArgumentException("handler must not be null");
        } else if (handler.getLooper() != Looper.myLooper()) {
            return new OpenListener(this.mManager, cameraId, listener, handler).blockUntilOpen();
        } else {
            throw new IllegalArgumentException("handler's looper must not be the current looper");
        }
    }

    private static void assertEquals(Object a, Object b) {
        if (!Objects.equals(a, b)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Expected ");
            stringBuilder.append(a);
            stringBuilder.append(", but got ");
            stringBuilder.append(b);
            throw new AssertionError(stringBuilder.toString());
        }
    }
}
