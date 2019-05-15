package com.android.ex.camera2.portability;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.OnZoomChangeListener;
import android.hardware.Camera.Parameters;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.view.SurfaceHolder;
import com.android.ex.camera2.portability.CameraDeviceInfo.Characteristics;
import com.android.ex.camera2.portability.CameraSettings.BoostParameters;
import com.android.ex.camera2.portability.debug.Log;
import com.android.ex.camera2.portability.debug.Log.Tag;
import com.hmdglobal.app.camera.ui.camera2.ExtendedFace;

public abstract class CameraAgent {
    private static final int CAMERA_CAPTURING = 8;
    private static final int CAMERA_FOCUSING = 16;
    private static final int CAMERA_IDLE = 2;
    public static final long CAMERA_OPERATION_TIMEOUT_MS = 3500;
    private static final int CAMERA_UNLOCKED = 4;
    private static final int CAMERA_UNOPENED = 1;
    private static final Tag TAG = new Tag("CamAgnt");
    public static int mLiveBolkenFrontLevel = 4;
    public static int mLiveBolkenRearLevel = 4;

    public interface CameraAFCallback {
        void onAutoFocus(boolean z, CameraProxy cameraProxy);
    }

    public interface CameraAFMoveCallback {
        void onAutoFocusMoving(boolean z, CameraProxy cameraProxy);
    }

    public interface CameraErrorCallback {
        void onError(int i, CameraProxy cameraProxy);
    }

    public interface CameraFaceDetectionCallback {
        void onFaceDetection(Rect rect, @Nullable Face[] faceArr, @Nullable ExtendedFace[] extendedFaceArr, CameraProxy cameraProxy);

        void onFaceDetection(Camera.Face[] faceArr, CameraProxy cameraProxy);
    }

    public interface CameraFinalPreviewCallback {
        void onFinalPreviewReturn();
    }

    public interface CameraGDCallBack {
        void onGesture();
    }

    public interface CameraOpenCallback {
        BoostParameters getBoostParam();

        Context getCallbackContext();

        boolean isBoostPreview();

        boolean isReleased();

        void onCameraClosed();

        void onCameraDisabled(int i);

        void onCameraOpened(CameraProxy cameraProxy);

        void onCameraOpenedBoost(CameraProxy cameraProxy);

        void onCameraRequested();

        void onDeviceOpenFailure(int i, String str);

        void onDeviceOpenedAlready(int i, String str);

        void onReconnectionFailure(CameraAgent cameraAgent, String str);
    }

    public interface CameraPanoramaCallback {
        void onCapture(byte[] bArr);
    }

    public interface CameraPanoramaMoveCallback {
        void onFrame(int i, int i2);
    }

    public interface CameraPictureCallback {
        void onPictureTaken(byte[] bArr, CameraProxy cameraProxy);
    }

    public interface CameraPreviewDataCallback {
        void onPreviewFrame(byte[] bArr, CameraProxy cameraProxy);
    }

    protected static class CameraPreviewDataCallbackWithHandler {
        Handler handler;
        CameraPreviewDataCallback previewDataCallback;

        public CameraPreviewDataCallbackWithHandler(CameraPreviewDataCallback callback, Handler handler) {
            this.previewDataCallback = callback;
            this.handler = handler;
        }

        public boolean equals(Object o) {
            if (o == null || !(o instanceof CameraPreviewDataCallbackWithHandler)) {
                return false;
            }
            CameraPreviewDataCallbackWithHandler cb = (CameraPreviewDataCallbackWithHandler) o;
            if (this.previewDataCallback == cb.previewDataCallback && this.handler == cb.previewDataCallback) {
                return true;
            }
            return false;
        }
    }

    public interface CameraPreviewResultCallback {
        void onCaptureComplete(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult);
    }

    public static abstract class CameraProxy {
        public abstract void abortBurstShot();

        public abstract boolean applySettings(CameraSettings cameraSettings);

        public abstract void autoFocus(Handler handler, CameraAFCallback cameraAFCallback);

        public abstract void burstShot(Handler handler, CameraShutterCallback cameraShutterCallback, CameraPictureCallback cameraPictureCallback, CameraPictureCallback cameraPictureCallback2, CameraPictureCallback cameraPictureCallback3);

        public abstract String dumpDeviceSettings();

        public abstract CameraAgent getAgent();

        @Deprecated
        public abstract Camera getCamera();

        public abstract Handler getCameraHandler();

        public abstract int getCameraId();

        public abstract CameraStateHolder getCameraState();

        public abstract CameraCapabilities getCapabilities();

        public abstract Characteristics getCharacteristics();

        public abstract DispatchThread getDispatchThread();

        @Deprecated
        public abstract Parameters getParameters();

        public abstract CameraSettings getSettings();

        public abstract void initExtCamera(Context context);

        @TargetApi(16)
        public abstract void setAutoFocusMoveCallback(Handler handler, CameraAFMoveCallback cameraAFMoveCallback);

        public abstract void setCaptureResultCallback(Handler handler, CaptureCompleteCallBack captureCompleteCallBack);

        public abstract void setFaceDetectionCallback(Handler handler, CameraFaceDetectionCallback cameraFaceDetectionCallback);

        public abstract void setFinalPreviewCallback(CameraFinalPreviewCallback cameraFinalPreviewCallback);

        public abstract void setGestureCallback(Handler handler, CameraGDCallBack cameraGDCallBack);

        public abstract void setModuleId(int i, int i2);

        public abstract void setOneShotPreviewCallback(Handler handler, CameraPreviewDataCallback cameraPreviewDataCallback);

        @Deprecated
        public abstract void setParameters(Parameters parameters);

        public abstract void setPreviewDataCallback(Handler handler, CameraPreviewDataCallback cameraPreviewDataCallback);

        public abstract void setPreviewDataCallbackWithBuffer(Handler handler, CameraPreviewDataCallback cameraPreviewDataCallback);

        public abstract void setPreviewResultCallback(Handler handler, CameraPreviewResultCallback cameraPreviewResultCallback);

        public abstract void setRamaCallback(Handler handler, CameraPanoramaCallback cameraPanoramaCallback);

        public abstract void setRamaMoveCallback(Handler handler, CameraPanoramaMoveCallback cameraPanoramaMoveCallback);

        public abstract void setZoomChangeListener(OnZoomChangeListener onZoomChangeListener);

        public abstract void startGestureDetection();

        public abstract void startPreAllocBurstShot();

        public abstract void startRama(Handler handler, int i);

        public abstract void stopGestureDetection();

        public abstract void stopPreAllocBurstShot();

        public abstract void stopRama(Handler handler, int i);

        public abstract void takePicture(Handler handler, CameraShutterCallback cameraShutterCallback, CameraPictureCallback cameraPictureCallback, CameraPictureCallback cameraPictureCallback2, CameraPictureCallback cameraPictureCallback3);

        public abstract void takePictureWithoutWaiting(Handler handler, CameraShutterCallback cameraShutterCallback, CameraPictureCallback cameraPictureCallback, CameraPictureCallback cameraPictureCallback2, CameraPictureCallback cameraPictureCallback3);

        public void reconnect(final Handler handler, final CameraOpenCallback cb) {
            try {
                getDispatchThread().runJob(new Runnable() {
                    public void run() {
                        CameraProxy.this.getCameraHandler().obtainMessage(3, CameraProxy.this.getCameraId(), 0, CameraOpenCallbackForward.getNewInstance(handler, cb)).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void unlock() {
            if (!getCameraState().isInvalid()) {
                final WaitDoneBundle bundle = new WaitDoneBundle();
                try {
                    getDispatchThread().runJobSync(new Runnable() {
                        public void run() {
                            CameraProxy.this.getCameraHandler().sendEmptyMessage(4);
                            CameraProxy.this.getCameraHandler().post(bundle.mUnlockRunnable);
                        }
                    }, bundle.mWaitLock, CameraAgent.CAMERA_OPERATION_TIMEOUT_MS, "camera unlock");
                } catch (RuntimeException ex) {
                    getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
                }
            }
        }

        public void lock() {
            try {
                getDispatchThread().runJob(new Runnable() {
                    public void run() {
                        CameraProxy.this.getCameraHandler().sendEmptyMessage(5);
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void setPreviewTexture(final SurfaceTexture surfaceTexture) {
            try {
                getDispatchThread().runJob(new Runnable() {
                    public void run() {
                        CameraProxy.this.getCameraHandler().obtainMessage(101, surfaceTexture).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void setPreviewTextureSync(final SurfaceTexture surfaceTexture) {
            if (!getCameraState().isInvalid()) {
                final WaitDoneBundle bundle = new WaitDoneBundle();
                try {
                    getDispatchThread().runJobSync(new Runnable() {
                        public void run() {
                            CameraProxy.this.getCameraHandler().obtainMessage(101, surfaceTexture).sendToTarget();
                            CameraProxy.this.getCameraHandler().post(bundle.mUnlockRunnable);
                        }
                    }, bundle.mWaitLock, CameraAgent.CAMERA_OPERATION_TIMEOUT_MS, "set preview texture");
                } catch (RuntimeException ex) {
                    getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
                }
            }
        }

        public void setPreviewDisplay(final SurfaceHolder surfaceHolder) {
            try {
                getDispatchThread().runJob(new Runnable() {
                    public void run() {
                        CameraProxy.this.getCameraHandler().obtainMessage(106, surfaceHolder).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void startPreview() {
            try {
                getDispatchThread().runJob(new Runnable() {
                    public void run() {
                        CameraProxy.this.getCameraHandler().obtainMessage(102, null).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void fakeStartPreview() {
            try {
                getDispatchThread().runJob(new Runnable() {
                    public void run() {
                        CameraProxy.this.getCameraHandler().obtainMessage(702, null).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void fakeSetPreviewTexture() {
            try {
                getDispatchThread().runJob(new Runnable() {
                    public void run() {
                        CameraProxy.this.getCameraHandler().obtainMessage(701, null).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void fakeStopPreview() {
            if (!getCameraState().isInvalid()) {
                final WaitDoneBundle bundle = new WaitDoneBundle();
                try {
                    getDispatchThread().runJobSync(new Runnable() {
                        public void run() {
                            CameraProxy.this.getCameraHandler().obtainMessage(703, bundle).sendToTarget();
                        }
                    }, bundle.mWaitLock, CameraAgent.CAMERA_OPERATION_TIMEOUT_MS, "stop preview");
                } catch (RuntimeException ex) {
                    getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
                }
            }
        }

        public void startPreviewWithCallback(final Handler h, final CameraStartPreviewCallback cb) {
            try {
                getDispatchThread().runJob(new Runnable() {
                    public void run() {
                        CameraProxy.this.getCameraHandler().obtainMessage(102, CameraStartPreviewCallbackForward.getNewInstance(h, cb)).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void waitPreviewWithCallback(final Handler h, final CameraStartPreviewCallback cb) {
            try {
                getDispatchThread().runJob(new Runnable() {
                    public void run() {
                        CameraProxy.this.getCameraHandler().obtainMessage(109, CameraStartPreviewCallbackForward.getNewInstance(h, cb)).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void stopPreview() {
            if (!getCameraState().isInvalid()) {
                try {
                    getDispatchThread().runJob(new Runnable() {
                        public void run() {
                            CameraProxy.this.getCameraHandler().sendEmptyMessage(103);
                        }
                    });
                } catch (RuntimeException ex) {
                    getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
                }
            }
        }

        public void addCallbackBuffer(final byte[] callbackBuffer) {
            try {
                getDispatchThread().runJob(new Runnable() {
                    public void run() {
                        CameraProxy.this.getCameraHandler().obtainMessage(105, callbackBuffer).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void cancelAutoFocus() {
            getCameraHandler().removeMessages(CameraActions.AUTO_FOCUS);
            getCameraHandler().sendMessageAtFrontOfQueue(getCameraHandler().obtainMessage(CameraActions.CANCEL_AUTO_FOCUS));
            getCameraHandler().sendEmptyMessage(CameraActions.CANCEL_AUTO_FOCUS_FINISH);
        }

        public void setPreviewBolkenLevel(final int level) {
            try {
                getDispatchThread().runJob(new Runnable() {
                    public void run() {
                        CameraProxy.this.getCameraHandler().obtainMessage(800, Integer.valueOf(level)).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void setDisplayOrientation(int degrees) {
            setDisplayOrientation(degrees, true);
        }

        public void setDisplayOrientation(final int degrees, final boolean capture) {
            try {
                getDispatchThread().runJob(new Runnable() {
                    public void run() {
                        CameraProxy.this.getCameraHandler().obtainMessage(CameraActions.SET_DISPLAY_ORIENTATION, degrees, capture).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void setJpegOrientation(final int degrees) {
            try {
                getDispatchThread().runJob(new Runnable() {
                    public void run() {
                        CameraProxy.this.getCameraHandler().obtainMessage(CameraActions.SET_JPEG_ORIENTATION, degrees, 0).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void startFaceDetection() {
            try {
                getDispatchThread().runJob(new Runnable() {
                    public void run() {
                        CameraProxy.this.getCameraHandler().sendEmptyMessage(CameraActions.START_FACE_DETECTION);
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void stopFaceDetection() {
            try {
                getDispatchThread().runJob(new Runnable() {
                    public void run() {
                        CameraProxy.this.getCameraHandler().sendEmptyMessage(CameraActions.STOP_FACE_DETECTION);
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        /* Access modifiers changed, original: protected */
        public boolean applySettingsHelper(CameraSettings settings, final int statesToAwait) {
            if (settings == null) {
                Log.v(CameraAgent.TAG, "null argument in applySettings()");
                return false;
            } else if (getCapabilities().supports(settings)) {
                final CameraSettings copyOfSettings = settings.copy();
                try {
                    getDispatchThread().runJob(new Runnable() {
                        public void run() {
                            CameraStateHolder cameraState = CameraProxy.this.getCameraState();
                            if (!cameraState.isInvalid()) {
                                Tag access$100 = CameraAgent.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("wait for state ");
                                stringBuilder.append(statesToAwait);
                                stringBuilder.append(" currentState is ");
                                stringBuilder.append(cameraState.getState());
                                Log.w(access$100, stringBuilder.toString());
                                cameraState.waitForStates(statesToAwait);
                                Log.w(CameraAgent.TAG, "wait for state done");
                                CameraProxy.this.getCameraHandler().obtainMessage(204, copyOfSettings).sendToTarget();
                            }
                        }
                    });
                } catch (RuntimeException ex) {
                    getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
                }
                return true;
            } else {
                Log.w(CameraAgent.TAG, "Unsupported settings in applySettings()");
                return false;
            }
        }

        public void refreshSettings() {
            try {
                getDispatchThread().runJob(new Runnable() {
                    public void run() {
                        CameraProxy.this.getCameraHandler().sendEmptyMessage(203);
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void enableShutterSound(final boolean enable) {
            try {
                getDispatchThread().runJob(new Runnable() {
                    public void run() {
                        CameraProxy.this.getCameraHandler().obtainMessage(CameraActions.ENABLE_SHUTTER_SOUND, enable, 0).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                getAgent().getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }
    }

    public interface CameraShutterCallback {
        void onShutter(CameraProxy cameraProxy);
    }

    public interface CameraStartPreviewCallback {
        void onPreviewStarted();
    }

    public interface CaptureCompleteCallBack {
        void onCaptureCompleted(CameraCaptureSession cameraCaptureSession, CaptureRequest captureRequest, TotalCaptureResult totalCaptureResult);
    }

    public static class WaitDoneBundle {
        public final Runnable mUnlockRunnable = new Runnable() {
            public void run() {
                synchronized (WaitDoneBundle.this.mWaitLock) {
                    WaitDoneBundle.this.mWaitLock.notifyAll();
                }
            }
        };
        public final Object mWaitLock = new Object();

        WaitDoneBundle() {
        }

        static void unblockSyncWaiters(Message msg) {
            if (msg != null && (msg.obj instanceof WaitDoneBundle)) {
                msg.obj.mUnlockRunnable.run();
            }
        }
    }

    public static class CameraOpenCallbackForward implements CameraOpenCallback {
        private BoostParameters mBoostParameters;
        private final CameraOpenCallback mCallback;
        private final Handler mHandler = new Handler(Looper.getMainLooper());

        public static CameraOpenCallbackForward getNewInstance(Handler handler, CameraOpenCallback cb) {
            if (handler == null || cb == null) {
                return null;
            }
            return new CameraOpenCallbackForward(handler, cb);
        }

        public static CameraOpenCallbackForward getNewInstance(Handler handler, CameraOpenCallback cb, BoostParameters parameters) {
            if (handler == null || cb == null) {
                return null;
            }
            return new CameraOpenCallbackForward(handler, cb, parameters);
        }

        private CameraOpenCallbackForward(Handler h, CameraOpenCallback cb) {
            this.mCallback = cb;
        }

        private CameraOpenCallbackForward(Handler h, CameraOpenCallback cb, BoostParameters parameters) {
            this.mCallback = cb;
            this.mBoostParameters = parameters;
        }

        public void onCameraOpened(final CameraProxy camera) {
            Log.w(CameraAgent.TAG, "KPI post open E");
            this.mHandler.post(new Runnable() {
                public void run() {
                    Log.w(CameraAgent.TAG, "KPI post open X");
                    CameraOpenCallbackForward.this.mCallback.onCameraOpened(camera);
                }
            });
        }

        public void onCameraOpenedBoost(final CameraProxy camera) {
            Log.w(CameraAgent.TAG, "KPI post open E");
            this.mHandler.post(new Runnable() {
                public void run() {
                    Log.w(CameraAgent.TAG, "KPI post open X");
                    CameraOpenCallbackForward.this.mCallback.onCameraOpened(camera);
                }
            });
        }

        public void onCameraDisabled(final int cameraId) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    CameraOpenCallbackForward.this.mCallback.onCameraDisabled(cameraId);
                }
            });
        }

        public void onDeviceOpenFailure(final int cameraId, final String info) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    CameraOpenCallbackForward.this.mCallback.onDeviceOpenFailure(cameraId, info);
                }
            });
        }

        public void onDeviceOpenedAlready(final int cameraId, final String info) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    CameraOpenCallbackForward.this.mCallback.onDeviceOpenedAlready(cameraId, info);
                }
            });
        }

        public void onReconnectionFailure(final CameraAgent mgr, final String info) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    CameraOpenCallbackForward.this.mCallback.onReconnectionFailure(mgr, info);
                }
            });
        }

        public void onCameraRequested() {
        }

        public void onCameraClosed() {
        }

        public boolean isBoostPreview() {
            return this.mCallback.isBoostPreview();
        }

        public Context getCallbackContext() {
            return this.mCallback.getCallbackContext();
        }

        public BoostParameters getBoostParam() {
            return this.mBoostParameters;
        }

        public boolean isReleased() {
            return this.mCallback != null ? this.mCallback.isReleased() : false;
        }
    }

    public static class CameraStartPreviewCallbackForward implements CameraStartPreviewCallback {
        private final CameraStartPreviewCallback mCallback;
        private final Handler mHandler;

        public static CameraStartPreviewCallbackForward getNewInstance(Handler handler, CameraStartPreviewCallback cb) {
            if (handler == null || cb == null) {
                return null;
            }
            return new CameraStartPreviewCallbackForward(handler, cb);
        }

        private CameraStartPreviewCallbackForward(Handler h, CameraStartPreviewCallback cb) {
            this.mHandler = h;
            this.mCallback = cb;
        }

        public void onPreviewStarted() {
            this.mHandler.post(new Runnable() {
                public void run() {
                    CameraStartPreviewCallbackForward.this.mCallback.onPreviewStarted();
                }
            });
        }
    }

    public abstract CameraDeviceInfo getCameraDeviceInfo();

    public abstract CameraExceptionHandler getCameraExceptionHandler();

    public abstract Handler getCameraHandler();

    public abstract CameraStateHolder getCameraState();

    public abstract DispatchThread getDispatchThread();

    public abstract void recycle();

    public abstract void setCameraExceptionHandler(CameraExceptionHandler cameraExceptionHandler);

    public void openCamera(final Handler handler, final int cameraId, final CameraOpenCallback callback) {
        try {
            getDispatchThread().runJob(new Runnable() {
                public void run() {
                    CameraAgent.this.getCameraHandler().obtainMessage(1, cameraId, 0, CameraOpenCallbackForward.getNewInstance(handler, callback)).sendToTarget();
                }
            });
        } catch (RuntimeException ex) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("open camera post to handler failed :");
            stringBuilder.append(ex.getMessage());
            Log.e(tag, stringBuilder.toString());
            getCameraExceptionHandler().onDispatchThreadException(ex);
        }
    }

    public void openCameraBoost(Handler handler, int cameraId, CameraOpenCallback callback, BoostParameters param) {
        try {
            final int i = cameraId;
            final Handler handler2 = handler;
            final CameraOpenCallback cameraOpenCallback = callback;
            final BoostParameters boostParameters = param;
            getDispatchThread().runJob(new Runnable() {
                public void run() {
                    CameraAgent.this.getCameraHandler().obtainMessage(1, i, 0, CameraOpenCallbackForward.getNewInstance(handler2, cameraOpenCallback, boostParameters)).sendToTarget();
                }
            });
        } catch (RuntimeException ex) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("open camera post to handler failed :");
            stringBuilder.append(ex.getMessage());
            Log.e(tag, stringBuilder.toString());
            getCameraExceptionHandler().onDispatchThreadException(ex);
        }
    }

    public boolean applySettings(final BoostParameters settings) {
        if (settings == null) {
            Log.v(TAG, "null argument in applySettings()");
            return false;
        }
        try {
            getDispatchThread().runJob(new Runnable() {
                public void run() {
                    CameraStateHolder cameraState = CameraAgent.this.getCameraState();
                    if (!cameraState.isInvalid()) {
                        Tag access$100 = CameraAgent.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("wait for state 6 currentState is ");
                        stringBuilder.append(cameraState.getState());
                        Log.w(access$100, stringBuilder.toString());
                        cameraState.waitForStates(6);
                        Log.w(CameraAgent.TAG, "wait for state done");
                        CameraAgent.this.getCameraHandler().obtainMessage(CameraActions.APPLY_PREVIEW_RELATED_SETTINGS, settings).sendToTarget();
                    }
                }
            });
        } catch (RuntimeException ex) {
            getCameraExceptionHandler().onDispatchThreadException(ex);
        }
        return true;
    }

    public void startPreviewAsync(final Handler handler, final CameraStartPreviewCallback callback) {
        try {
            getDispatchThread().runJob(new Runnable() {
                public void run() {
                    CameraAgent.this.getCameraHandler().obtainMessage(102, CameraStartPreviewCallbackForward.getNewInstance(handler, callback)).sendToTarget();
                }
            });
        } catch (RuntimeException ex) {
            getCameraExceptionHandler().onDispatchThreadException(ex);
        }
    }

    public void setPreviewTexture(final SurfaceTexture surfaceTexture) {
        try {
            getDispatchThread().runJob(new Runnable() {
                public void run() {
                    CameraAgent.this.getCameraHandler().obtainMessage(101, surfaceTexture).sendToTarget();
                }
            });
        } catch (RuntimeException ex) {
            getCameraExceptionHandler().onDispatchThreadException(ex);
        }
    }

    public void closeCamera(CameraProxy camera, boolean synced) {
        if (synced) {
            try {
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("valid");
                stringBuilder.append(getCameraState().isInvalid());
                Log.d(tag, stringBuilder.toString());
                if (!getCameraState().isInvalid()) {
                    final WaitDoneBundle bundle = new WaitDoneBundle();
                    getDispatchThread().runJobSync(new Runnable() {
                        public void run() {
                            CameraAgent.this.getCameraHandler().removeCallbacksAndMessages(null);
                            CameraAgent.this.getCameraHandler().obtainMessage(2).sendToTarget();
                            CameraAgent.this.getCameraHandler().post(bundle.mUnlockRunnable);
                        }
                    }, bundle.mWaitLock, CAMERA_OPERATION_TIMEOUT_MS, "camera release");
                }
            } catch (RuntimeException ex) {
                getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        } else {
            getDispatchThread().runJob(new Runnable() {
                public void run() {
                    CameraAgent.this.getCameraHandler().removeCallbacksAndMessages(null);
                    CameraAgent.this.getCameraHandler().obtainMessage(2).sendToTarget();
                }
            });
        }
    }
}
