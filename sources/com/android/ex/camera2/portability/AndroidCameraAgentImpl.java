package com.android.ex.camera2.portability;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.AutoFocusMoveCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.OnZoomChangeListener;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Size;
import com.android.ex.camera2.portability.CameraAgent.CameraAFCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraAFMoveCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraFaceDetectionCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraFinalPreviewCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraGDCallBack;
import com.android.ex.camera2.portability.CameraAgent.CameraPanoramaCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraPanoramaMoveCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraPictureCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraPreviewDataCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraPreviewResultCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.android.ex.camera2.portability.CameraAgent.CameraShutterCallback;
import com.android.ex.camera2.portability.CameraAgent.CaptureCompleteCallBack;
import com.android.ex.camera2.portability.CameraAgent.WaitDoneBundle;
import com.android.ex.camera2.portability.CameraCapabilities.Feature;
import com.android.ex.camera2.portability.CameraCapabilities.FlashMode;
import com.android.ex.camera2.portability.CameraCapabilities.FocusMode;
import com.android.ex.camera2.portability.CameraCapabilities.Stringifier;
import com.android.ex.camera2.portability.CameraCapabilities.WhiteBalance;
import com.android.ex.camera2.portability.CameraDeviceInfo.Characteristics;
import com.android.ex.camera2.portability.CameraSettings.BoostParameters;
import com.android.ex.camera2.portability.CameraSettings.GpsData;
import com.android.ex.camera2.portability.debug.Log;
import com.android.ex.camera2.portability.debug.Log.Tag;
import com.android.external.ExtendKey;
import com.android.external.ExtendParameters;
import com.android.external.ExtendParameters.ExposureMode;
import com.android.external.plantform.ExtBuild;
import com.android.external.plantform.IExtCamera;
import com.android.external.plantform.IExtGestureCallback;
import com.android.external.plantform.IExtPanoramaCallback;
import com.android.external.plantform.IExtPanoramaMoveCallback;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.settings.SettingsUtil;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.hmdglobal.app.camera.util.CustomFields;
import com.hmdglobal.app.camera.util.CustomUtil;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

class AndroidCameraAgentImpl extends CameraAgent {
    private static final String ANTIBANDING_AUTO_50 = "auto_50";
    private static final String ANTIBANDING_AUTO_60 = "auto_60";
    private static final int CAMERA_HAL_API_VERSION_1_0 = 256;
    private static final Tag TAG = new Tag("AndCamAgntImp");
    private static final CameraExceptionHandler sDefaultExceptionHandler = new CameraExceptionHandler(null) {
        public void onCameraError(int errorCode) {
            Tag access$000 = AndroidCameraAgentImpl.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onCameraError called with no handler set: ");
            stringBuilder.append(errorCode);
            Log.w(access$000, stringBuilder.toString());
        }

        public void onCameraException(RuntimeException ex, String commandHistory, int action, int state) {
            Log.w(AndroidCameraAgentImpl.TAG, "onCameraException called with no handler set", ex);
        }

        public void onDispatchThreadException(RuntimeException ex) {
            Log.w(AndroidCameraAgentImpl.TAG, "onDispatchThreadException called with no handler set", ex);
        }
    };
    private final CameraHandler mCameraHandler;
    private final HandlerThread mCameraHandlerThread;
    private final CameraStateHolder mCameraState;
    private AndroidCameraCapabilities mCapabilities;
    private Characteristics mCharacteristics;
    private final DispatchThread mDispatchThread;
    private CameraExceptionHandler mExceptionHandler = sDefaultExceptionHandler;
    private boolean mRecycled = false;

    private static class AFCallbackForward implements AutoFocusCallback {
        private final CameraAFCallback mCallback;
        private final CameraProxy mCamera;
        private final Handler mHandler;

        public static AFCallbackForward getNewInstance(Handler handler, CameraProxy camera, CameraAFCallback cb) {
            if (handler == null || camera == null || cb == null) {
                return null;
            }
            return new AFCallbackForward(handler, camera, cb);
        }

        private AFCallbackForward(Handler h, CameraProxy camera, CameraAFCallback cb) {
            this.mHandler = h;
            this.mCamera = camera;
            this.mCallback = cb;
        }

        public void onAutoFocus(final boolean b, Camera camera) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    AFCallbackForward.this.mCallback.onAutoFocus(b, AFCallbackForward.this.mCamera);
                }
            });
        }
    }

    @TargetApi(16)
    private static class AFMoveCallbackForward implements AutoFocusMoveCallback {
        private final CameraAFMoveCallback mCallback;
        private final CameraProxy mCamera;
        private final Handler mHandler;

        public static AFMoveCallbackForward getNewInstance(Handler handler, CameraProxy camera, CameraAFMoveCallback cb) {
            if (handler == null || camera == null || cb == null) {
                return null;
            }
            return new AFMoveCallbackForward(handler, camera, cb);
        }

        private AFMoveCallbackForward(Handler h, CameraProxy camera, CameraAFMoveCallback cb) {
            this.mHandler = h;
            this.mCamera = camera;
            this.mCallback = cb;
        }

        public void onAutoFocusMoving(final boolean moving, Camera camera) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    AFMoveCallbackForward.this.mCallback.onAutoFocusMoving(moving, AFMoveCallbackForward.this.mCamera);
                }
            });
        }
    }

    private static class FaceDetectionCallbackForward implements FaceDetectionListener {
        private final CameraFaceDetectionCallback mCallback;
        private final CameraProxy mCamera;
        private final Handler mHandler;

        public static FaceDetectionCallbackForward getNewInstance(Handler handler, CameraProxy camera, CameraFaceDetectionCallback cb) {
            if (handler == null || camera == null || cb == null) {
                return null;
            }
            return new FaceDetectionCallbackForward(handler, camera, cb);
        }

        private FaceDetectionCallbackForward(Handler h, CameraProxy camera, CameraFaceDetectionCallback cb) {
            this.mHandler = h;
            this.mCamera = camera;
            this.mCallback = cb;
        }

        public void onFaceDetection(final Face[] faces, Camera camera) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    FaceDetectionCallbackForward.this.mCallback.onFaceDetection(faces, FaceDetectionCallbackForward.this.mCamera);
                }
            });
        }
    }

    private static class ParametersCache {
        private Camera mCamera;
        private Parameters mParameters;

        public ParametersCache(Camera camera) {
            this.mCamera = camera;
        }

        public synchronized void invalidate() {
            this.mParameters = null;
        }

        public synchronized Parameters getBlocking() {
            if (this.mParameters == null) {
                this.mParameters = this.mCamera.getParameters();
                if (this.mParameters == null) {
                    Log.e(AndroidCameraAgentImpl.TAG, "Camera object returned null parameters!");
                    throw new IllegalStateException("camera.getParameters returned null");
                }
            }
            return this.mParameters;
        }
    }

    private static class PictureCallbackForward implements PictureCallback {
        private final CameraPictureCallback mCallback;
        private final CameraProxy mCamera;
        private final Handler mHandler;

        public static PictureCallbackForward getNewInstance(Handler handler, CameraProxy camera, CameraPictureCallback cb) {
            if (handler == null || camera == null || cb == null) {
                return null;
            }
            return new PictureCallbackForward(handler, camera, cb);
        }

        private PictureCallbackForward(Handler h, CameraProxy camera, CameraPictureCallback cb) {
            this.mHandler = h;
            this.mCamera = camera;
            this.mCallback = cb;
        }

        public void onPictureTaken(final byte[] data, Camera camera) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    PictureCallbackForward.this.mCallback.onPictureTaken(data, PictureCallbackForward.this.mCamera);
                }
            });
        }
    }

    private static class PreviewCallbackForward implements PreviewCallback {
        private final CameraPreviewDataCallback mCallback;
        private final CameraProxy mCamera;
        private final Handler mHandler;

        public static PreviewCallbackForward getNewInstance(Handler handler, CameraProxy camera, CameraPreviewDataCallback cb) {
            if (handler == null || camera == null || cb == null) {
                return null;
            }
            return new PreviewCallbackForward(handler, camera, cb);
        }

        private PreviewCallbackForward(Handler h, CameraProxy camera, CameraPreviewDataCallback cb) {
            this.mHandler = h;
            this.mCamera = camera;
            this.mCallback = cb;
        }

        public void onPreviewFrame(final byte[] data, Camera camera) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    PreviewCallbackForward.this.mCallback.onPreviewFrame(data, PreviewCallbackForward.this.mCamera);
                }
            });
        }
    }

    private static class ShutterCallbackForward implements ShutterCallback {
        private final CameraShutterCallback mCallback;
        private final CameraProxy mCamera;
        private final Handler mHandler;

        public static ShutterCallbackForward getNewInstance(Handler handler, CameraProxy camera, CameraShutterCallback cb) {
            if (handler == null || camera == null || cb == null) {
                return null;
            }
            return new ShutterCallbackForward(handler, camera, cb);
        }

        private ShutterCallbackForward(Handler h, CameraProxy camera, CameraShutterCallback cb) {
            this.mHandler = h;
            this.mCamera = camera;
            this.mCallback = cb;
        }

        public void onShutter() {
            this.mHandler.post(new Runnable() {
                public void run() {
                    ShutterCallbackForward.this.mCallback.onShutter(ShutterCallbackForward.this.mCamera);
                }
            });
        }
    }

    private static class AndroidCameraDeviceInfo implements CameraDeviceInfo {
        private final CameraInfo[] mCameraInfos;
        private final int mFirstBackCameraId;
        private final int mFirstFrontCameraId;
        private final int mNumberOfCameras;

        private static class AndroidCharacteristics extends Characteristics {
            private CameraInfo mCameraInfo;

            AndroidCharacteristics(CameraInfo cameraInfo) {
                this.mCameraInfo = cameraInfo;
            }

            public Size[] getSupportRawSize() {
                return null;
            }

            public boolean isFacingBack() {
                return this.mCameraInfo.facing == 0;
            }

            public boolean isFacingFront() {
                return this.mCameraInfo.facing == 1;
            }

            public int getSensorOrientation() {
                return this.mCameraInfo.orientation;
            }

            public boolean canDisableShutterSound() {
                return this.mCameraInfo.canDisableShutterSound;
            }

            public int getSupportedHardwareLevel(int id) {
                return 2;
            }
        }

        private AndroidCameraDeviceInfo(CameraInfo[] info, int numberOfCameras, int firstBackCameraId, int firstFrontCameraId) {
            this.mCameraInfos = info;
            this.mNumberOfCameras = numberOfCameras;
            this.mFirstBackCameraId = firstBackCameraId;
            this.mFirstFrontCameraId = firstFrontCameraId;
        }

        public static AndroidCameraDeviceInfo create() {
            try {
                int i;
                int numberOfCameras = Camera.getNumberOfCameras();
                CameraInfo[] cameraInfos = new CameraInfo[numberOfCameras];
                for (i = 0; i < numberOfCameras; i++) {
                    cameraInfos[i] = new CameraInfo();
                    Camera.getCameraInfo(i, cameraInfos[i]);
                }
                i = -1;
                int firstBack = -1;
                for (int i2 = numberOfCameras - 1; i2 >= 0; i2--) {
                    if (cameraInfos[i2].facing == 0) {
                        firstBack = i2;
                    } else if (cameraInfos[i2].facing == 1) {
                        i = i2;
                    }
                }
                return new AndroidCameraDeviceInfo(cameraInfos, numberOfCameras, firstBack, i);
            } catch (RuntimeException ex) {
                Log.e(AndroidCameraAgentImpl.TAG, "Exception while creating CameraDeviceInfo", ex);
                return null;
            }
        }

        public Characteristics getCharacteristics(int cameraId) {
            CameraInfo info = this.mCameraInfos[cameraId];
            if (info != null) {
                return new AndroidCharacteristics(info);
            }
            return null;
        }

        public int getNumberOfCameras() {
            return this.mNumberOfCameras;
        }

        public int getFirstBackCameraId() {
            return this.mFirstBackCameraId;
        }

        public int getFirstFrontCameraId() {
            return this.mFirstFrontCameraId;
        }
    }

    private class AndroidCameraProxyImpl extends CameraProxy {
        private final Camera mCamera;
        private final CameraAgent mCameraAgent;
        private final int mCameraId;
        private final AndroidCameraCapabilities mCapabilities;
        private final Characteristics mCharacteristics;
        private IExtCamera mExtCamera;

        /* synthetic */ AndroidCameraProxyImpl(AndroidCameraAgentImpl x0, CameraAgent x1, int x2, Camera x3, Characteristics x4, AndroidCameraCapabilities x5, AnonymousClass1 x6) {
            this(x1, x2, x3, x4, x5);
        }

        private AndroidCameraProxyImpl(CameraAgent cameraAgent, int cameraId, Camera camera, Characteristics characteristics, AndroidCameraCapabilities capabilities) {
            this.mCameraAgent = cameraAgent;
            this.mCamera = camera;
            this.mCameraId = cameraId;
            this.mCharacteristics = characteristics;
            this.mCapabilities = capabilities;
        }

        @Deprecated
        public Camera getCamera() {
            if (getCameraState().isInvalid()) {
                return null;
            }
            return this.mCamera;
        }

        public int getCameraId() {
            return this.mCameraId;
        }

        public void setModuleId(int id, int oritention) {
        }

        public Characteristics getCharacteristics() {
            return this.mCharacteristics;
        }

        public CameraCapabilities getCapabilities() {
            return new AndroidCameraCapabilities(this.mCapabilities);
        }

        public CameraAgent getAgent() {
            return this.mCameraAgent;
        }

        public void setPreviewResultCallback(Handler handler, CameraPreviewResultCallback cb) {
        }

        public void setCaptureResultCallback(Handler handler, CaptureCompleteCallBack cb) {
        }

        public void setFinalPreviewCallback(CameraFinalPreviewCallback cb) {
        }

        public void setPreviewDataCallback(final Handler handler, final CameraPreviewDataCallback cb) {
            AndroidCameraAgentImpl.this.mDispatchThread.runJob(new Runnable() {
                public void run() {
                    AndroidCameraAgentImpl.this.mCameraHandler.obtainMessage(107, PreviewCallbackForward.getNewInstance(handler, AndroidCameraProxyImpl.this, cb)).sendToTarget();
                }
            });
        }

        public void setOneShotPreviewCallback(final Handler handler, final CameraPreviewDataCallback cb) {
            AndroidCameraAgentImpl.this.mDispatchThread.runJob(new Runnable() {
                public void run() {
                    AndroidCameraAgentImpl.this.mCameraHandler.obtainMessage(108, PreviewCallbackForward.getNewInstance(handler, AndroidCameraProxyImpl.this, cb)).sendToTarget();
                }
            });
        }

        public void setPreviewDataCallbackWithBuffer(final Handler handler, final CameraPreviewDataCallback cb) {
            AndroidCameraAgentImpl.this.mDispatchThread.runJob(new Runnable() {
                public void run() {
                    AndroidCameraAgentImpl.this.mCameraHandler.obtainMessage(104, PreviewCallbackForward.getNewInstance(handler, AndroidCameraProxyImpl.this, cb)).sendToTarget();
                }
            });
        }

        public void autoFocus(final Handler handler, final CameraAFCallback cb) {
            final AutoFocusCallback afCallback = new AutoFocusCallback() {
                public void onAutoFocus(final boolean b, Camera camera) {
                    if (AndroidCameraAgentImpl.this.mCameraState.getState() != 16) {
                        Log.w(AndroidCameraAgentImpl.TAG, "onAutoFocus callback returning when not focusing");
                    } else {
                        AndroidCameraAgentImpl.this.mCameraState.setState(2);
                    }
                    handler.post(new Runnable() {
                        public void run() {
                            cb.onAutoFocus(b, AndroidCameraProxyImpl.this);
                        }
                    });
                }
            };
            AndroidCameraAgentImpl.this.mDispatchThread.runJobInstance(new Runnable() {
                public void run() {
                    if (!AndroidCameraProxyImpl.this.getCameraState().isInvalid()) {
                        AndroidCameraAgentImpl.this.mCameraHandler.removeMessages(CameraActions.AUTO_FOCUS);
                        AndroidCameraAgentImpl.this.mCameraState.waitForStates(2);
                        AndroidCameraAgentImpl.this.mCameraHandler.obtainMessage(CameraActions.AUTO_FOCUS, afCallback).sendToTarget();
                    }
                }
            }, CameraActions.AUTO_FOCUS);
        }

        @TargetApi(16)
        public void setAutoFocusMoveCallback(final Handler handler, final CameraAFMoveCallback cb) {
            try {
                AndroidCameraAgentImpl.this.mDispatchThread.runJob(new Runnable() {
                    public void run() {
                        AndroidCameraAgentImpl.this.mCameraHandler.obtainMessage(CameraActions.SET_AUTO_FOCUS_MOVE_CALLBACK, AFMoveCallbackForward.getNewInstance(handler, AndroidCameraProxyImpl.this, cb)).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                this.mCameraAgent.getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void takePicture(final Handler handler, CameraShutterCallback shutter, CameraPictureCallback raw, CameraPictureCallback post, final CameraPictureCallback jpeg) {
            final PictureCallback jpegCallback = new PictureCallback() {
                public void onPictureTaken(final byte[] data, Camera camera) {
                    if (AndroidCameraAgentImpl.this.mCameraState.getState() != 8) {
                        Log.w(AndroidCameraAgentImpl.TAG, "picture callback returning when not capturing");
                    } else {
                        AndroidCameraAgentImpl.this.mCameraState.setState(2);
                    }
                    handler.post(new Runnable() {
                        public void run() {
                            jpeg.onPictureTaken(data, AndroidCameraProxyImpl.this);
                        }
                    });
                }
            };
            try {
                final Handler handler2 = handler;
                final CameraShutterCallback cameraShutterCallback = shutter;
                final CameraPictureCallback cameraPictureCallback = raw;
                final CameraPictureCallback cameraPictureCallback2 = post;
                AndroidCameraAgentImpl.this.mDispatchThread.runJob(new Runnable() {
                    public void run() {
                        if (!AndroidCameraProxyImpl.this.getCameraState().isInvalid()) {
                            AndroidCameraAgentImpl.this.mCameraState.waitForStates(6);
                            AndroidCameraAgentImpl.this.mCameraHandler.requestTakePicture(ShutterCallbackForward.getNewInstance(handler2, AndroidCameraProxyImpl.this, cameraShutterCallback), PictureCallbackForward.getNewInstance(handler2, AndroidCameraProxyImpl.this, cameraPictureCallback), PictureCallbackForward.getNewInstance(handler2, AndroidCameraProxyImpl.this, cameraPictureCallback2), jpegCallback);
                        }
                    }
                });
            } catch (RuntimeException ex) {
                this.mCameraAgent.getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void takePictureWithoutWaiting(final Handler handler, CameraShutterCallback shutter, CameraPictureCallback raw, CameraPictureCallback post, final CameraPictureCallback jpeg) {
            final PictureCallback jpegCallback = new PictureCallback() {
                public void onPictureTaken(final byte[] data, Camera camera) {
                    if (AndroidCameraAgentImpl.this.mCameraState.getState() != 8) {
                        Log.w(AndroidCameraAgentImpl.TAG, "picture callback returning when not capturing");
                    } else {
                        AndroidCameraAgentImpl.this.mCameraState.setState(2);
                    }
                    handler.post(new Runnable() {
                        public void run() {
                            jpeg.onPictureTaken(data, AndroidCameraProxyImpl.this);
                        }
                    });
                }
            };
            try {
                final Handler handler2 = handler;
                final CameraShutterCallback cameraShutterCallback = shutter;
                final CameraPictureCallback cameraPictureCallback = raw;
                final CameraPictureCallback cameraPictureCallback2 = post;
                AndroidCameraAgentImpl.this.mDispatchThread.runJob(new Runnable() {
                    public void run() {
                        if (!AndroidCameraProxyImpl.this.getCameraState().isInvalid()) {
                            AndroidCameraAgentImpl.this.mCameraHandler.requestTakePicture(ShutterCallbackForward.getNewInstance(handler2, AndroidCameraProxyImpl.this, cameraShutterCallback), PictureCallbackForward.getNewInstance(handler2, AndroidCameraProxyImpl.this, cameraPictureCallback), PictureCallbackForward.getNewInstance(handler2, AndroidCameraProxyImpl.this, cameraPictureCallback2), jpegCallback);
                        }
                    }
                });
            } catch (RuntimeException ex) {
                this.mCameraAgent.getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void burstShot(Handler handler, CameraShutterCallback shutter, CameraPictureCallback raw, CameraPictureCallback postview, CameraPictureCallback jpeg) {
            AndroidCameraAgentImpl.this.mDispatchThread.runJob(new Runnable() {
                public void run() {
                    AndroidCameraAgentImpl.this.mCameraHandler.sendEmptyMessage(CameraActions.BURST_SHOT);
                }
            });
        }

        public void startPreAllocBurstShot() {
            AndroidCameraAgentImpl.this.mDispatchThread.runJob(new Runnable() {
                public void run() {
                    AndroidCameraAgentImpl.this.mCameraHandler.sendEmptyMessage(CameraActions.PRE_ALLOC_BURST_SHOT);
                }
            });
        }

        public void stopPreAllocBurstShot() {
            AndroidCameraAgentImpl.this.mDispatchThread.runJob(new Runnable() {
                public void run() {
                    AndroidCameraAgentImpl.this.mCameraHandler.sendEmptyMessage(CameraActions.ABORT_PRE_ALLOC_BURST_SHOT);
                }
            });
        }

        public void initExtCamera(Context context) {
            this.mExtCamera = ExtBuild.createCamera(this.mCamera, context);
        }

        public void setGestureCallback(Handler handler, final CameraGDCallBack cb) {
            if (this.mExtCamera == null) {
                Log.e(AndroidCameraAgentImpl.TAG, "setGestureCallback(): mExtCamera is null");
                return;
            }
            this.mExtCamera.setGestureCallback(new IExtGestureCallback() {
                public void onGesture() {
                    Log.w(AndroidCameraAgentImpl.TAG, "Camera onGesture() Callback");
                    if (cb != null) {
                        cb.onGesture();
                    }
                }
            });
        }

        public void startGestureDetection() {
            if (this.mExtCamera == null) {
                Log.e(AndroidCameraAgentImpl.TAG, "startGestureDetection(): mExtCamera is null");
            } else {
                this.mExtCamera.startGestureDetection();
            }
        }

        public void stopGestureDetection() {
            if (this.mExtCamera == null) {
                Log.e(AndroidCameraAgentImpl.TAG, "stopGestureDetection(): mExtCamera is null");
            } else {
                this.mExtCamera.stopGestureDetection();
            }
        }

        public void startRama(Handler handler, int num) {
            if (this.mExtCamera == null) {
                Log.e(AndroidCameraAgentImpl.TAG, "startRama(): mExtCamera is null");
            } else {
                this.mExtCamera.startRama(num);
            }
        }

        public void stopRama(Handler handler, int isMerge) {
            if (this.mExtCamera == null) {
                Log.e(AndroidCameraAgentImpl.TAG, "stopRama(): mExtCamera is null");
            } else {
                this.mExtCamera.stopRama(isMerge);
            }
        }

        public void setRamaCallback(Handler handler, final CameraPanoramaCallback cb) {
            if (this.mExtCamera == null) {
                Log.e(AndroidCameraAgentImpl.TAG, "setRamaCallback(): mExtCamera is null");
                return;
            }
            this.mExtCamera.setRamaCallback(new IExtPanoramaCallback() {
                public void onCapture(byte[] jpegData) {
                    Log.w(AndroidCameraAgentImpl.TAG, "Camera onCapture() Callback");
                    if (cb != null) {
                        cb.onCapture(jpegData);
                    }
                }
            });
        }

        public void setRamaMoveCallback(Handler handler, final CameraPanoramaMoveCallback cb) {
            if (this.mExtCamera == null) {
                Log.e(AndroidCameraAgentImpl.TAG, "setRamaMoveCallback(): mExtCamera is null");
                return;
            }
            this.mExtCamera.setRamaMoveCallback(new IExtPanoramaMoveCallback() {
                public void onFrame(int xx, int yy) {
                    Tag access$000 = AndroidCameraAgentImpl.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Camera onFrame() Callback: xx=");
                    stringBuilder.append(xx);
                    stringBuilder.append(", yy=");
                    stringBuilder.append(yy);
                    Log.w(access$000, stringBuilder.toString());
                    if (cb != null) {
                        cb.onFrame(xx, yy);
                    }
                }
            });
        }

        public void abortBurstShot() {
            AndroidCameraAgentImpl.this.mCameraHandler.removeMessages(CameraActions.CAPTURE_PHOTO);
            AndroidCameraAgentImpl.this.mDispatchThread.runJob(new Runnable() {
                public void run() {
                    AndroidCameraAgentImpl.this.mCameraHandler.obtainMessage(CameraActions.ABORT_SHOT).sendToTarget();
                }
            });
        }

        public void setZoomChangeListener(final OnZoomChangeListener listener) {
            try {
                AndroidCameraAgentImpl.this.mDispatchThread.runJob(new Runnable() {
                    public void run() {
                        AndroidCameraAgentImpl.this.mCameraHandler.obtainMessage(CameraActions.SET_ZOOM_CHANGE_LISTENER, listener).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                this.mCameraAgent.getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        public void setFaceDetectionCallback(final Handler handler, final CameraFaceDetectionCallback cb) {
            try {
                AndroidCameraAgentImpl.this.mDispatchThread.runJob(new Runnable() {
                    public void run() {
                        AndroidCameraAgentImpl.this.mCameraHandler.obtainMessage(CameraActions.SET_FACE_DETECTION_LISTENER, FaceDetectionCallbackForward.getNewInstance(handler, AndroidCameraProxyImpl.this, cb)).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                this.mCameraAgent.getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        @Deprecated
        public void setParameters(Parameters params) {
            if (params == null) {
                Log.v(AndroidCameraAgentImpl.TAG, "null parameters in setParameters()");
                return;
            }
            final String flattenedParameters = params.flatten();
            try {
                AndroidCameraAgentImpl.this.mDispatchThread.runJob(new Runnable() {
                    public void run() {
                        AndroidCameraAgentImpl.this.mCameraState.waitForStates(6);
                        AndroidCameraAgentImpl.this.mCameraHandler.obtainMessage(201, flattenedParameters).sendToTarget();
                    }
                });
            } catch (RuntimeException ex) {
                this.mCameraAgent.getCameraExceptionHandler().onDispatchThreadException(ex);
            }
        }

        @Deprecated
        public Parameters getParameters() {
            final WaitDoneBundle bundle = new WaitDoneBundle();
            final Parameters[] parametersHolder = new Parameters[1];
            try {
                AndroidCameraAgentImpl.this.mDispatchThread.runJobSync(new Runnable() {
                    public void run() {
                        AndroidCameraAgentImpl.this.mCameraHandler.obtainMessage(202, parametersHolder).sendToTarget();
                        AndroidCameraAgentImpl.this.mCameraHandler.post(bundle.mUnlockRunnable);
                    }
                }, bundle.mWaitLock, CameraAgent.CAMERA_OPERATION_TIMEOUT_MS, "get parameters");
            } catch (RuntimeException ex) {
                this.mCameraAgent.getCameraExceptionHandler().onDispatchThreadException(ex);
            }
            return parametersHolder[0];
        }

        public CameraSettings getSettings() {
            return new AndroidCameraSettings(this.mCapabilities, getParameters());
        }

        public boolean applySettings(CameraSettings settings) {
            return applySettingsHelper(settings, 6);
        }

        public String dumpDeviceSettings() {
            if (getParameters() == null) {
                return "[no parameters retrieved]";
            }
            StringTokenizer tokenizer = new StringTokenizer(getParameters().flatten(), ";");
            String dumpedSettings = new String();
            while (tokenizer.hasMoreElements()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(dumpedSettings);
                stringBuilder.append(tokenizer.nextToken());
                stringBuilder.append(10);
                dumpedSettings = stringBuilder.toString();
            }
            return dumpedSettings;
        }

        public Handler getCameraHandler() {
            return AndroidCameraAgentImpl.this.getCameraHandler();
        }

        public DispatchThread getDispatchThread() {
            return AndroidCameraAgentImpl.this.getDispatchThread();
        }

        public CameraStateHolder getCameraState() {
            return AndroidCameraAgentImpl.this.mCameraState;
        }
    }

    private static class AndroidCameraStateHolder extends CameraStateHolder {
        public static final int CAMERA_CAPTURING = 8;
        public static final int CAMERA_FOCUSING = 16;
        public static final int CAMERA_IDLE = 2;
        public static final int CAMERA_UNLOCKED = 4;
        public static final int CAMERA_UNOPENED = 1;

        public AndroidCameraStateHolder() {
            this(1);
        }

        public AndroidCameraStateHolder(int state) {
            super(state);
        }
    }

    private class CameraHandler extends HistoryHandler implements ErrorCallback {
        private CameraAgent mAgent;
        private Camera mCamera;
        private int mCameraId = -1;
        private int mCancelAfPending = 0;
        private ParametersCache mParameterCache;

        private class CaptureCallbacks {
            public final PictureCallback mJpeg;
            public final PictureCallback mPostView;
            public final PictureCallback mRaw;
            public final ShutterCallback mShutter;

            CaptureCallbacks(ShutterCallback shutter, PictureCallback raw, PictureCallback postView, PictureCallback jpeg) {
                this.mShutter = shutter;
                this.mRaw = raw;
                this.mPostView = postView;
                this.mJpeg = jpeg;
            }
        }

        CameraHandler(CameraAgent agent, Looper looper) {
            super(looper);
            this.mAgent = agent;
        }

        private void startFaceDetection() {
            try {
                this.mCamera.startFaceDetection();
            } catch (RuntimeException e) {
                Log.e(AndroidCameraAgentImpl.TAG, "faceDetection already started , ignore it");
            }
        }

        private void stopFaceDetection() {
            this.mCamera.stopFaceDetection();
        }

        private void setFaceDetectionListener(FaceDetectionListener listener) {
            this.mCamera.setFaceDetectionListener(listener);
        }

        private void setPreviewTexture(Object surfaceTexture) {
            try {
                this.mCamera.setPreviewTexture((SurfaceTexture) surfaceTexture);
            } catch (IOException e) {
                Log.e(AndroidCameraAgentImpl.TAG, "Could not set preview texture", e);
            }
        }

        @TargetApi(17)
        private void enableShutterSound(boolean enable) {
            this.mCamera.enableShutterSound(enable);
        }

        @TargetApi(16)
        private void setAutoFocusMoveCallback(Camera camera, Object cb) {
            try {
                camera.setAutoFocusMoveCallback((AutoFocusMoveCallback) cb);
            } catch (RuntimeException ex) {
                Log.w(AndroidCameraAgentImpl.TAG, ex.getMessage());
            }
        }

        public void requestTakePicture(ShutterCallback shutter, PictureCallback raw, PictureCallback postView, PictureCallback jpeg) {
            obtainMessage(CameraActions.CAPTURE_PHOTO, new CaptureCallbacks(shutter, raw, postView, jpeg)).sendToTarget();
        }

        public void onError(int errorCode, Camera camera) {
            AndroidCameraAgentImpl.this.mExceptionHandler.onCameraError(errorCode);
            if (errorCode == 100) {
                AndroidCameraAgentImpl.this.mExceptionHandler.onCameraException(new RuntimeException("Media server died."), generateHistoryString(this.mCameraId), getCurrentMessage().intValue(), AndroidCameraAgentImpl.this.mCameraState.getState());
            }
        }

        /* JADX WARNING: Exception block dominator not found, dom blocks: [B:13:0x0079, B:59:0x029e, B:119:0x04c4, B:130:0x0532] */
        /* JADX WARNING: Missing block: B:17:0x0098, code skipped:
            r0 = move-exception;
     */
        /* JADX WARNING: Missing block: B:61:0x02a9, code skipped:
            r0 = move-exception;
     */
        /* JADX WARNING: Missing block: B:64:0x02af, code skipped:
            throw new java.lang.RuntimeException(r0);
     */
        /* JADX WARNING: Missing block: B:67:0x02c6, code skipped:
            r1.mCamera.stopPreview();
     */
        /* JADX WARNING: Missing block: B:68:0x02cd, code skipped:
            r0 = r2.obj;
            r1.mCamera.startPreview();
     */
        /* JADX WARNING: Missing block: B:69:0x02d6, code skipped:
            if (r0 == null) goto L_0x049b;
     */
        /* JADX WARNING: Missing block: B:70:0x02d8, code skipped:
            r0.onPreviewStarted();
     */
        /* JADX WARNING: Missing block: B:118:0x04c3, code skipped:
            r5 = r0;
     */
        /* JADX WARNING: Missing block: B:120:?, code skipped:
            r7 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.access$200(r1.this$0).getState();
            r0 = new java.lang.StringBuilder();
            r0.append("CameraAction[");
            r0.append(com.android.ex.camera2.portability.CameraActions.stringify(r3));
            r0.append("] at CameraState[");
            r0.append(r7);
            r0.append("]");
            r8 = r0.toString();
            r0 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.access$000();
            r9 = new java.lang.StringBuilder();
            r9.append("RuntimeException during ");
            r9.append(r8);
            com.android.ex.camera2.portability.debug.Log.e(r0, r9.toString(), r5);
            com.android.ex.camera2.portability.AndroidCameraAgentImpl.access$200(r1.this$0).invalidate();
     */
        /* JADX WARNING: Missing block: B:121:0x0515, code skipped:
            if (r1.mCamera != null) goto L_0x0517;
     */
        /* JADX WARNING: Missing block: B:122:0x0517, code skipped:
            com.android.ex.camera2.portability.debug.Log.i(com.android.ex.camera2.portability.AndroidCameraAgentImpl.access$000(), "Release camera since mCamera is not null.");
     */
        /* JADX WARNING: Missing block: B:124:?, code skipped:
            r1.mCamera.release();
     */
        /* JADX WARNING: Missing block: B:126:?, code skipped:
            r1.mCamera = null;
            r0 = com.hmdglobal.app.camera.instantcapture.CameraLock.getInstance();
     */
        /* JADX WARNING: Missing block: B:127:0x052b, code skipped:
            r0.open();
     */
        /* JADX WARNING: Missing block: B:129:0x0531, code skipped:
            r0 = move-exception;
     */
        /* JADX WARNING: Missing block: B:131:?, code skipped:
            com.android.ex.camera2.portability.debug.Log.e(com.android.ex.camera2.portability.AndroidCameraAgentImpl.access$000(), "Fail when calling Camera.release().", r0);
     */
        /* JADX WARNING: Missing block: B:133:?, code skipped:
            r1.mCamera = null;
            r0 = com.hmdglobal.app.camera.instantcapture.CameraLock.getInstance();
     */
        /* JADX WARNING: Missing block: B:134:0x0542, code skipped:
            r1.mCamera = null;
            com.hmdglobal.app.camera.instantcapture.CameraLock.getInstance().open();
     */
        /* JADX WARNING: Missing block: B:137:0x054e, code skipped:
            if (r2.what == 1) goto L_0x0550;
     */
        /* JADX WARNING: Missing block: B:140:0x0554, code skipped:
            r0 = r2.arg1;
     */
        /* JADX WARNING: Missing block: B:141:0x0558, code skipped:
            if (r2.obj != null) goto L_0x055a;
     */
        /* JADX WARNING: Missing block: B:142:0x055a, code skipped:
            ((com.android.ex.camera2.portability.CameraAgent.CameraOpenCallback) r2.obj).onDeviceOpenFailure(r2.arg1, generateHistoryString(r0));
     */
        /* JADX WARNING: Missing block: B:144:0x0568, code skipped:
            r1.mAgent.getCameraExceptionHandler().onCameraException(r5, generateHistoryString(r1.mCameraId), r3, r7);
     */
        /* JADX WARNING: Missing block: B:145:0x0577, code skipped:
            r0 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.access$000();
            r4 = new java.lang.StringBuilder();
     */
        /* JADX WARNING: Missing block: B:147:0x0583, code skipped:
            r4 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.access$000();
            r5 = new java.lang.StringBuilder();
            r5.append("handleMessage - action = '");
            r5.append(com.android.ex.camera2.portability.CameraActions.stringify(r2.what));
            r5.append("' done ");
            com.android.ex.camera2.portability.debug.Log.w(r4, r5.toString());
            com.android.ex.camera2.portability.CameraAgent.WaitDoneBundle.unblockSyncWaiters(r18);
     */
        public void handleMessage(android.os.Message r18) {
            /*
            r17 = this;
            r1 = r17;
            r2 = r18;
            super.handleMessage(r18);
            r0 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.this;
            r0 = r0.getCameraState();
            r0 = r0.isInvalid();
            if (r0 == 0) goto L_0x0037;
        L_0x0013:
            r0 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.TAG;
            r3 = new java.lang.StringBuilder;
            r3.<init>();
            r4 = "Skip handleMessage - action = '";
            r3.append(r4);
            r4 = r2.what;
            r4 = com.android.ex.camera2.portability.CameraActions.stringify(r4);
            r3.append(r4);
            r4 = "'";
            r3.append(r4);
            r3 = r3.toString();
            com.android.ex.camera2.portability.debug.Log.w(r0, r3);
            return;
        L_0x0037:
            r0 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.TAG;
            r3 = new java.lang.StringBuilder;
            r3.<init>();
            r4 = "handleMessage - action = '";
            r3.append(r4);
            r4 = r2.what;
            r4 = com.android.ex.camera2.portability.CameraActions.stringify(r4);
            r3.append(r4);
            r4 = "'";
            r3.append(r4);
            r3 = r3.toString();
            com.android.ex.camera2.portability.debug.Log.w(r0, r3);
            r0 = r2.what;
            r3 = r0;
            r4 = 0;
            r5 = 2;
            r0 = 0;
            r6 = 1;
            switch(r3) {
                case 1: goto L_0x037b;
                case 2: goto L_0x034b;
                case 3: goto L_0x0305;
                case 4: goto L_0x02f4;
                case 5: goto L_0x02e4;
                default: goto L_0x0064;
            };
        L_0x0064:
            switch(r3) {
                case 101: goto L_0x02dd;
                case 102: goto L_0x02cd;
                case 103: goto L_0x02c6;
                case 104: goto L_0x02bb;
                case 105: goto L_0x02b0;
                case 106: goto L_0x029e;
                case 107: goto L_0x0293;
                case 108: goto L_0x0288;
                case 109: goto L_0x027d;
                default: goto L_0x0067;
            };
        L_0x0067:
            switch(r3) {
                case 201: goto L_0x0264;
                case 202: goto L_0x0256;
                case 203: goto L_0x024f;
                case 204: goto L_0x0236;
                case 205: goto L_0x021d;
                default: goto L_0x006a;
            };
        L_0x006a:
            switch(r3) {
                case 301: goto L_0x01e2;
                case 302: goto L_0x01c4;
                case 303: goto L_0x01bb;
                case 304: goto L_0x01b0;
                case 305: goto L_0x01a9;
                default: goto L_0x006d;
            };
        L_0x006d:
            switch(r3) {
                case 461: goto L_0x01a0;
                case 462: goto L_0x019b;
                case 463: goto L_0x0196;
                default: goto L_0x0070;
            };
        L_0x0070:
            switch(r3) {
                case 501: goto L_0x018b;
                case 502: goto L_0x0153;
                case 503: goto L_0x013c;
                default: goto L_0x0073;
            };
        L_0x0073:
            switch(r3) {
                case 601: goto L_0x00f6;
                case 602: goto L_0x00e5;
                case 603: goto L_0x00d4;
                case 604: goto L_0x00bb;
                case 605: goto L_0x00a5;
                default: goto L_0x0076;
            };
        L_0x0076:
            switch(r3) {
                case 701: goto L_0x009b;
                case 702: goto L_0x02cd;
                case 703: goto L_0x02c6;
                default: goto L_0x0079;
            };
        L_0x0079:
            r0 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.TAG;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0098 }
            r5.<init>();	 Catch:{ RuntimeException -> 0x0098 }
            r7 = "Invalid CameraProxy message=";
            r5.append(r7);	 Catch:{ RuntimeException -> 0x0098 }
            r7 = r2.what;	 Catch:{ RuntimeException -> 0x0098 }
            r5.append(r7);	 Catch:{ RuntimeException -> 0x0098 }
            r5 = r5.toString();	 Catch:{ RuntimeException -> 0x0098 }
            com.android.ex.camera2.portability.debug.Log.e(r0, r5);	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x0095:
            r0 = move-exception;
            goto L_0x0583;
        L_0x0098:
            r0 = move-exception;
            goto L_0x04c3;
        L_0x009b:
            r5 = new android.graphics.SurfaceTexture;	 Catch:{ RuntimeException -> 0x0098 }
            r5.<init>(r0);	 Catch:{ RuntimeException -> 0x0098 }
            r1.setPreviewTexture(r5);	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x00a5:
            r0 = r1.mParameterCache;	 Catch:{ RuntimeException -> 0x0098 }
            r0 = r0.getBlocking();	 Catch:{ RuntimeException -> 0x0098 }
            r5 = "snapshot-burst-num";
            r0.set(r5, r6);	 Catch:{ RuntimeException -> 0x0098 }
            r0 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            r0 = com.android.external.ExtendCamera.getInstance(r0);	 Catch:{ RuntimeException -> 0x0098 }
            r0.cancelPreAllocBurst();	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x00bb:
            r0 = com.hmdglobal.app.camera.util.CustomUtil.getInstance();	 Catch:{ RuntimeException -> 0x0098 }
            r5 = "def_camera_burst_max";
            r7 = 10;
            r0 = r0.getInt(r5, r7);	 Catch:{ RuntimeException -> 0x0098 }
            r5 = r1.mParameterCache;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = r5.getBlocking();	 Catch:{ RuntimeException -> 0x0098 }
            r7 = "snapshot-burst-num";
            r5.set(r7, r0);	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x00d4:
            r5 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = com.android.external.ExtendCamera.getInstance(r5);	 Catch:{ RuntimeException -> 0x0098 }
            r7 = r1.mParameterCache;	 Catch:{ RuntimeException -> 0x0098 }
            r7 = r7.getBlocking();	 Catch:{ RuntimeException -> 0x0098 }
            r5.setLongshot(r0, r7);	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x00e5:
            r0 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            r0 = com.android.external.ExtendCamera.getInstance(r0);	 Catch:{ RuntimeException -> 0x0098 }
            r5 = r1.mParameterCache;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = r5.getBlocking();	 Catch:{ RuntimeException -> 0x0098 }
            r0.setLongshot(r6, r5);	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x00f6:
            r0 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.this;	 Catch:{ Exception -> 0x0114 }
            r0 = r0.mCameraState;	 Catch:{ Exception -> 0x0114 }
            r7 = 8;
            r0.setState(r7);	 Catch:{ Exception -> 0x0114 }
            r0 = r2.obj;	 Catch:{ Exception -> 0x0114 }
            r0 = (com.android.ex.camera2.portability.AndroidCameraAgentImpl.CameraHandler.CaptureCallbacks) r0;	 Catch:{ Exception -> 0x0114 }
            r7 = r1.mCamera;	 Catch:{ Exception -> 0x0114 }
            r8 = r0.mShutter;	 Catch:{ Exception -> 0x0114 }
            r9 = r0.mRaw;	 Catch:{ Exception -> 0x0114 }
            r10 = r0.mPostView;	 Catch:{ Exception -> 0x0114 }
            r11 = r0.mJpeg;	 Catch:{ Exception -> 0x0114 }
            r7.takePicture(r8, r9, r10, r11);	 Catch:{ Exception -> 0x0114 }
            goto L_0x049b;
        L_0x0114:
            r0 = move-exception;
            r7 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.TAG;	 Catch:{ RuntimeException -> 0x0098 }
            r8 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0098 }
            r8.<init>();	 Catch:{ RuntimeException -> 0x0098 }
            r9 = "takePicture Error, ignore";
            r8.append(r9);	 Catch:{ RuntimeException -> 0x0098 }
            r9 = r0.toString();	 Catch:{ RuntimeException -> 0x0098 }
            r8.append(r9);	 Catch:{ RuntimeException -> 0x0098 }
            r8 = r8.toString();	 Catch:{ RuntimeException -> 0x0098 }
            com.android.ex.camera2.portability.debug.Log.e(r7, r8);	 Catch:{ RuntimeException -> 0x0098 }
            r7 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.this;	 Catch:{ RuntimeException -> 0x0098 }
            r7 = r7.mCameraState;	 Catch:{ RuntimeException -> 0x0098 }
            r7.setState(r5);	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x013c:
            r0 = r1.mParameterCache;	 Catch:{ RuntimeException -> 0x0098 }
            r0 = r0.getBlocking();	 Catch:{ RuntimeException -> 0x0098 }
            r5 = r2.arg1;	 Catch:{ RuntimeException -> 0x0098 }
            r0.setRotation(r5);	 Catch:{ RuntimeException -> 0x0098 }
            r5 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            r5.setParameters(r0);	 Catch:{ RuntimeException -> 0x0098 }
            r5 = r1.mParameterCache;	 Catch:{ RuntimeException -> 0x0098 }
            r5.invalidate();	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x0153:
            r5 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            r7 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.this;	 Catch:{ RuntimeException -> 0x0098 }
            r7 = r7.mCharacteristics;	 Catch:{ RuntimeException -> 0x0098 }
            r8 = r2.arg1;	 Catch:{ RuntimeException -> 0x0098 }
            r7 = r7.getPreviewOrientation(r8);	 Catch:{ RuntimeException -> 0x0098 }
            r5.setDisplayOrientation(r7);	 Catch:{ RuntimeException -> 0x0098 }
            r5 = r1.mParameterCache;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = r5.getBlocking();	 Catch:{ RuntimeException -> 0x0098 }
            r7 = r2.arg2;	 Catch:{ RuntimeException -> 0x0098 }
            if (r7 <= 0) goto L_0x017c;
        L_0x016f:
            r0 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.this;	 Catch:{ RuntimeException -> 0x0098 }
            r0 = r0.mCharacteristics;	 Catch:{ RuntimeException -> 0x0098 }
            r7 = r2.arg1;	 Catch:{ RuntimeException -> 0x0098 }
            r0 = r0.getJpegOrientation(r7);	 Catch:{ RuntimeException -> 0x0098 }
        L_0x017c:
            r5.setRotation(r0);	 Catch:{ RuntimeException -> 0x0098 }
            r0 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            r0.setParameters(r5);	 Catch:{ RuntimeException -> 0x0098 }
            r0 = r1.mParameterCache;	 Catch:{ RuntimeException -> 0x0098 }
            r0.invalidate();	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x018b:
            r5 = r2.arg1;	 Catch:{ RuntimeException -> 0x0098 }
            if (r5 != r6) goto L_0x0191;
        L_0x018f:
            r0 = r6;
        L_0x0191:
            r1.enableShutterSound(r0);	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x0196:
            r17.stopFaceDetection();	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x019b:
            r17.startFaceDetection();	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x01a0:
            r0 = r2.obj;	 Catch:{ RuntimeException -> 0x0098 }
            r0 = (android.hardware.Camera.FaceDetectionListener) r0;	 Catch:{ RuntimeException -> 0x0098 }
            r1.setFaceDetectionListener(r0);	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x01a9:
            r0 = r1.mCancelAfPending;	 Catch:{ RuntimeException -> 0x0098 }
            r0 = r0 - r6;
            r1.mCancelAfPending = r0;	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x01b0:
            r0 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = r2.obj;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = (android.hardware.Camera.OnZoomChangeListener) r5;	 Catch:{ RuntimeException -> 0x0098 }
            r0.setZoomChangeListener(r5);	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x01bb:
            r0 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = r2.obj;	 Catch:{ RuntimeException -> 0x0098 }
            r1.setAutoFocusMoveCallback(r0, r5);	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x01c4:
            r0 = r1.mCancelAfPending;	 Catch:{ RuntimeException -> 0x0098 }
            r0 = r0 + r6;
            r1.mCancelAfPending = r0;	 Catch:{ RuntimeException -> 0x0098 }
            r0 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            r0.cancelAutoFocus();	 Catch:{ RuntimeException -> 0x0098 }
            r0 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.TAG;	 Catch:{ RuntimeException -> 0x0098 }
            r7 = "cancel autofocus";
            com.android.ex.camera2.portability.debug.Log.w(r0, r7);	 Catch:{ RuntimeException -> 0x0098 }
            r0 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.this;	 Catch:{ RuntimeException -> 0x0098 }
            r0 = r0.mCameraState;	 Catch:{ RuntimeException -> 0x0098 }
            r0.setState(r5);	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x01e2:
            r0 = r1.mCancelAfPending;	 Catch:{ RuntimeException -> 0x0098 }
            if (r0 <= 0) goto L_0x0207;
        L_0x01e6:
            r0 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.TAG;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0098 }
            r5.<init>();	 Catch:{ RuntimeException -> 0x0098 }
            r7 = "handleMessage - Ignored AUTO_FOCUS because there was ";
            r5.append(r7);	 Catch:{ RuntimeException -> 0x0098 }
            r7 = r1.mCancelAfPending;	 Catch:{ RuntimeException -> 0x0098 }
            r5.append(r7);	 Catch:{ RuntimeException -> 0x0098 }
            r7 = " pending CANCEL_AUTO_FOCUS messages";
            r5.append(r7);	 Catch:{ RuntimeException -> 0x0098 }
            r5 = r5.toString();	 Catch:{ RuntimeException -> 0x0098 }
            com.android.ex.camera2.portability.debug.Log.v(r0, r5);	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x0207:
            r0 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.this;	 Catch:{ RuntimeException -> 0x0098 }
            r0 = r0.mCameraState;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = 16;
            r0.setState(r5);	 Catch:{ RuntimeException -> 0x0098 }
            r0 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = r2.obj;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = (android.hardware.Camera.AutoFocusCallback) r5;	 Catch:{ RuntimeException -> 0x0098 }
            r0.autoFocus(r5);	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x021d:
            r0 = r1.mParameterCache;	 Catch:{ RuntimeException -> 0x0098 }
            r0 = r0.getBlocking();	 Catch:{ RuntimeException -> 0x0098 }
            r5 = r2.obj;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = (com.android.ex.camera2.portability.CameraSettings.BoostParameters) r5;	 Catch:{ RuntimeException -> 0x0098 }
            r1.applyPreviewRelatedSettingsToParameters(r5, r0);	 Catch:{ RuntimeException -> 0x0098 }
            r7 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            r7.setParameters(r0);	 Catch:{ RuntimeException -> 0x0098 }
            r7 = r1.mParameterCache;	 Catch:{ RuntimeException -> 0x0098 }
            r7.invalidate();	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x0236:
            r0 = r1.mParameterCache;	 Catch:{ RuntimeException -> 0x0098 }
            r0 = r0.getBlocking();	 Catch:{ RuntimeException -> 0x0098 }
            r5 = r2.obj;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = (com.android.ex.camera2.portability.CameraSettings) r5;	 Catch:{ RuntimeException -> 0x0098 }
            r1.applySettingsToParameters(r5, r0);	 Catch:{ RuntimeException -> 0x0098 }
            r7 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            r7.setParameters(r0);	 Catch:{ RuntimeException -> 0x0098 }
            r7 = r1.mParameterCache;	 Catch:{ RuntimeException -> 0x0098 }
            r7.invalidate();	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x024f:
            r0 = r1.mParameterCache;	 Catch:{ RuntimeException -> 0x0098 }
            r0.invalidate();	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x0256:
            r5 = r2.obj;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = (android.hardware.Camera.Parameters[]) r5;	 Catch:{ RuntimeException -> 0x0098 }
            r7 = r1.mParameterCache;	 Catch:{ RuntimeException -> 0x0098 }
            r7 = r7.getBlocking();	 Catch:{ RuntimeException -> 0x0098 }
            r5[r0] = r7;	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x0264:
            r0 = r1.mParameterCache;	 Catch:{ RuntimeException -> 0x0098 }
            r0 = r0.getBlocking();	 Catch:{ RuntimeException -> 0x0098 }
            r5 = r2.obj;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = (java.lang.String) r5;	 Catch:{ RuntimeException -> 0x0098 }
            r0.unflatten(r5);	 Catch:{ RuntimeException -> 0x0098 }
            r5 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            r5.setParameters(r0);	 Catch:{ RuntimeException -> 0x0098 }
            r5 = r1.mParameterCache;	 Catch:{ RuntimeException -> 0x0098 }
            r5.invalidate();	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x027d:
            r0 = r2.obj;	 Catch:{ RuntimeException -> 0x0098 }
            r0 = (com.android.ex.camera2.portability.CameraAgent.CameraStartPreviewCallbackForward) r0;	 Catch:{ RuntimeException -> 0x0098 }
            if (r0 == 0) goto L_0x049b;
        L_0x0283:
            r0.onPreviewStarted();	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x0288:
            r0 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = r2.obj;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = (android.hardware.Camera.PreviewCallback) r5;	 Catch:{ RuntimeException -> 0x0098 }
            r0.setOneShotPreviewCallback(r5);	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x0293:
            r0 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = r2.obj;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = (android.hardware.Camera.PreviewCallback) r5;	 Catch:{ RuntimeException -> 0x0098 }
            r0.setPreviewCallback(r5);	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x029e:
            r0 = r1.mCamera;	 Catch:{ IOException -> 0x02a9 }
            r5 = r2.obj;	 Catch:{ IOException -> 0x02a9 }
            r5 = (android.view.SurfaceHolder) r5;	 Catch:{ IOException -> 0x02a9 }
            r0.setPreviewDisplay(r5);	 Catch:{ IOException -> 0x02a9 }
            goto L_0x049b;
        L_0x02a9:
            r0 = move-exception;
            r5 = new java.lang.RuntimeException;	 Catch:{ RuntimeException -> 0x0098 }
            r5.<init>(r0);	 Catch:{ RuntimeException -> 0x0098 }
            throw r5;	 Catch:{ RuntimeException -> 0x0098 }
        L_0x02b0:
            r0 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = r2.obj;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = (byte[]) r5;	 Catch:{ RuntimeException -> 0x0098 }
            r0.addCallbackBuffer(r5);	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x02bb:
            r0 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = r2.obj;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = (android.hardware.Camera.PreviewCallback) r5;	 Catch:{ RuntimeException -> 0x0098 }
            r0.setPreviewCallbackWithBuffer(r5);	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x02c6:
            r0 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            r0.stopPreview();	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x02cd:
            r0 = r2.obj;	 Catch:{ RuntimeException -> 0x0098 }
            r0 = (com.android.ex.camera2.portability.CameraAgent.CameraStartPreviewCallbackForward) r0;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            r5.startPreview();	 Catch:{ RuntimeException -> 0x0098 }
            if (r0 == 0) goto L_0x049b;
        L_0x02d8:
            r0.onPreviewStarted();	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x02dd:
            r0 = r2.obj;	 Catch:{ RuntimeException -> 0x0098 }
            r1.setPreviewTexture(r0);	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x02e4:
            r0 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            r0.lock();	 Catch:{ RuntimeException -> 0x0098 }
            r0 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.this;	 Catch:{ RuntimeException -> 0x0098 }
            r0 = r0.mCameraState;	 Catch:{ RuntimeException -> 0x0098 }
            r0.setState(r5);	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x02f4:
            r0 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            r0.unlock();	 Catch:{ RuntimeException -> 0x0098 }
            r0 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.this;	 Catch:{ RuntimeException -> 0x0098 }
            r0 = r0.mCameraState;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = 4;
            r0.setState(r5);	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x0305:
            r0 = r2.obj;	 Catch:{ RuntimeException -> 0x0098 }
            r0 = (com.android.ex.camera2.portability.CameraAgent.CameraOpenCallbackForward) r0;	 Catch:{ RuntimeException -> 0x0098 }
            r7 = r0;
            r11 = r2.arg1;	 Catch:{ RuntimeException -> 0x0098 }
            r0 = r1.mCamera;	 Catch:{ IOException -> 0x033b }
            r0.reconnect();	 Catch:{ IOException -> 0x033b }
            r0 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.this;	 Catch:{ RuntimeException -> 0x0098 }
            r0 = r0.mCameraState;	 Catch:{ RuntimeException -> 0x0098 }
            r0.setState(r5);	 Catch:{ RuntimeException -> 0x0098 }
            if (r7 == 0) goto L_0x049b;
        L_0x031d:
            r0 = new com.android.ex.camera2.portability.AndroidCameraAgentImpl$AndroidCameraProxyImpl;	 Catch:{ RuntimeException -> 0x0098 }
            r9 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.this;	 Catch:{ RuntimeException -> 0x0098 }
            r10 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.this;	 Catch:{ RuntimeException -> 0x0098 }
            r12 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.this;	 Catch:{ RuntimeException -> 0x0098 }
            r13 = r5.mCharacteristics;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.this;	 Catch:{ RuntimeException -> 0x0098 }
            r14 = r5.mCapabilities;	 Catch:{ RuntimeException -> 0x0098 }
            r15 = 0;
            r8 = r0;
            r8.<init>(r9, r10, r11, r12, r13, r14, r15);	 Catch:{ RuntimeException -> 0x0098 }
            r7.onCameraOpened(r0);	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x033b:
            r0 = move-exception;
            if (r7 == 0) goto L_0x0349;
        L_0x033e:
            r5 = r1.mAgent;	 Catch:{ RuntimeException -> 0x0098 }
            r8 = r1.mCameraId;	 Catch:{ RuntimeException -> 0x0098 }
            r8 = r1.generateHistoryString(r8);	 Catch:{ RuntimeException -> 0x0098 }
            r7.onReconnectionFailure(r5, r8);	 Catch:{ RuntimeException -> 0x0098 }
        L_0x0349:
            goto L_0x049b;
        L_0x034b:
            r0 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            if (r0 == 0) goto L_0x0370;
        L_0x034f:
            r0 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            r0.setPreviewCallback(r4);	 Catch:{ RuntimeException -> 0x0098 }
            r0 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            r0.release();	 Catch:{ RuntimeException -> 0x0098 }
            r0 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.this;	 Catch:{ RuntimeException -> 0x0098 }
            r0 = r0.mCameraState;	 Catch:{ RuntimeException -> 0x0098 }
            r0.setState(r6);	 Catch:{ RuntimeException -> 0x0098 }
            r1.mCamera = r4;	 Catch:{ RuntimeException -> 0x0098 }
            r0 = -1;
            r1.mCameraId = r0;	 Catch:{ RuntimeException -> 0x0098 }
            r0 = com.hmdglobal.app.camera.instantcapture.CameraLock.getInstance();	 Catch:{ RuntimeException -> 0x0098 }
            r0.open();	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x0370:
            r0 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.TAG;	 Catch:{ RuntimeException -> 0x0098 }
            r5 = "Releasing camera without any camera opened.";
            com.android.ex.camera2.portability.debug.Log.w(r0, r5);	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x037b:
            r7 = r2.obj;	 Catch:{ RuntimeException -> 0x0098 }
            r7 = (com.android.ex.camera2.portability.CameraAgent.CameraOpenCallback) r7;	 Catch:{ RuntimeException -> 0x0098 }
            r8 = r2.arg1;	 Catch:{ RuntimeException -> 0x0098 }
            r9 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.this;	 Catch:{ RuntimeException -> 0x0098 }
            r9 = r9.mCameraState;	 Catch:{ RuntimeException -> 0x0098 }
            r9 = r9.getState();	 Catch:{ RuntimeException -> 0x0098 }
            if (r9 == r6) goto L_0x0396;
        L_0x038d:
            r0 = r1.generateHistoryString(r8);	 Catch:{ RuntimeException -> 0x0098 }
            r7.onDeviceOpenedAlready(r8, r0);	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x049b;
        L_0x0396:
            r9 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.TAG;	 Catch:{ RuntimeException -> 0x0098 }
            r10 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x0098 }
            r10.<init>();	 Catch:{ RuntimeException -> 0x0098 }
            r11 = "Opening camera ";
            r10.append(r11);	 Catch:{ RuntimeException -> 0x0098 }
            r10.append(r8);	 Catch:{ RuntimeException -> 0x0098 }
            r11 = " with camera1 API";
            r10.append(r11);	 Catch:{ RuntimeException -> 0x0098 }
            r10 = r10.toString();	 Catch:{ RuntimeException -> 0x0098 }
            com.android.ex.camera2.portability.debug.Log.i(r9, r10);	 Catch:{ RuntimeException -> 0x0098 }
            r9 = com.hmdglobal.app.camera.instantcapture.CameraLock.getInstance();	 Catch:{ RuntimeException -> 0x0098 }
            r10 = 1000; // 0x3e8 float:1.401E-42 double:4.94E-321;
            r9.block(r10);	 Catch:{ RuntimeException -> 0x0098 }
            r9 = com.hmdglobal.app.camera.instantcapture.CameraLock.getInstance();	 Catch:{ RuntimeException -> 0x0098 }
            r9.close();	 Catch:{ RuntimeException -> 0x0098 }
            r9 = r4;
            r10 = "android.hardware.Camera";
            r10 = java.lang.Class.forName(r10);	 Catch:{ ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6 }
            r11 = "openLegacy";
            r12 = new java.lang.Class[r5];	 Catch:{ ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6 }
            r13 = java.lang.Integer.TYPE;	 Catch:{ ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6 }
            r12[r0] = r13;	 Catch:{ ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6 }
            r13 = java.lang.Integer.TYPE;	 Catch:{ ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6 }
            r12[r6] = r13;	 Catch:{ ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6 }
            r10 = r10.getMethod(r11, r12);	 Catch:{ ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6 }
            r9 = r10;
            r10 = new java.lang.Object[r5];	 Catch:{ ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6 }
            r11 = r2.arg1;	 Catch:{ ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6 }
            r11 = java.lang.Integer.valueOf(r11);	 Catch:{ ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6 }
            r10[r0] = r11;	 Catch:{ ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6 }
            r0 = 256; // 0x100 float:3.59E-43 double:1.265E-321;
            r0 = java.lang.Integer.valueOf(r0);	 Catch:{ ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6 }
            r10[r6] = r0;	 Catch:{ ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6 }
            r0 = r9.invoke(r4, r10);	 Catch:{ ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6 }
            r0 = (android.hardware.Camera) r0;	 Catch:{ ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6 }
            r1.mCamera = r0;	 Catch:{ ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6, ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException -> 0x03f6 }
            goto L_0x03fd;
        L_0x03f6:
            r0 = move-exception;
            r10 = android.hardware.Camera.open(r8);	 Catch:{ RuntimeException -> 0x0098 }
            r1.mCamera = r10;	 Catch:{ RuntimeException -> 0x0098 }
        L_0x03fd:
            r0 = r9;
            r9 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            if (r9 == 0) goto L_0x048a;
        L_0x0402:
            r1.mCameraId = r8;	 Catch:{ RuntimeException -> 0x0098 }
            r9 = new com.android.ex.camera2.portability.AndroidCameraAgentImpl$ParametersCache;	 Catch:{ RuntimeException -> 0x0098 }
            r10 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            r9.<init>(r10);	 Catch:{ RuntimeException -> 0x0098 }
            r1.mParameterCache = r9;	 Catch:{ RuntimeException -> 0x0098 }
            r9 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.this;	 Catch:{ RuntimeException -> 0x0098 }
            r10 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.AndroidCameraDeviceInfo.create();	 Catch:{ RuntimeException -> 0x0098 }
            r10 = r10.getCharacteristics(r8);	 Catch:{ RuntimeException -> 0x0098 }
            r9.mCharacteristics = r10;	 Catch:{ RuntimeException -> 0x0098 }
            r9 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.this;	 Catch:{ RuntimeException -> 0x0098 }
            r10 = new com.android.ex.camera2.portability.AndroidCameraCapabilities;	 Catch:{ RuntimeException -> 0x0098 }
            r11 = r1.mParameterCache;	 Catch:{ RuntimeException -> 0x0098 }
            r11 = r11.getBlocking();	 Catch:{ RuntimeException -> 0x0098 }
            r10.<init>(r11);	 Catch:{ RuntimeException -> 0x0098 }
            r9.mCapabilities = r10;	 Catch:{ RuntimeException -> 0x0098 }
            r9 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            r9.setErrorCallback(r1);	 Catch:{ RuntimeException -> 0x0098 }
            r9 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.this;	 Catch:{ RuntimeException -> 0x0098 }
            r9 = r9.mCameraState;	 Catch:{ RuntimeException -> 0x0098 }
            r9.setState(r5);	 Catch:{ RuntimeException -> 0x0098 }
            if (r7 == 0) goto L_0x049b;
        L_0x043a:
            r5 = r7.isReleased();	 Catch:{ RuntimeException -> 0x0098 }
            if (r5 != 0) goto L_0x049b;
        L_0x0440:
            r5 = new com.android.ex.camera2.portability.AndroidCameraAgentImpl$AndroidCameraProxyImpl;	 Catch:{ RuntimeException -> 0x0098 }
            r10 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.this;	 Catch:{ RuntimeException -> 0x0098 }
            r11 = r1.mAgent;	 Catch:{ RuntimeException -> 0x0098 }
            r13 = r1.mCamera;	 Catch:{ RuntimeException -> 0x0098 }
            r9 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.this;	 Catch:{ RuntimeException -> 0x0098 }
            r14 = r9.mCharacteristics;	 Catch:{ RuntimeException -> 0x0098 }
            r9 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.this;	 Catch:{ RuntimeException -> 0x0098 }
            r15 = r9.mCapabilities;	 Catch:{ RuntimeException -> 0x0098 }
            r16 = 0;
            r9 = r5;
            r12 = r8;
            r9.<init>(r10, r11, r12, r13, r14, r15, r16);	 Catch:{ RuntimeException -> 0x0098 }
            r9 = r7.getBoostParam();	 Catch:{ RuntimeException -> 0x0098 }
            r10 = r7.getCallbackContext();	 Catch:{ RuntimeException -> 0x0098 }
            if (r10 == 0) goto L_0x047c;
        L_0x0465:
            r10 = com.hmdglobal.app.camera.util.PictureSizePerso.getInstance();	 Catch:{ RuntimeException -> 0x0098 }
            r11 = r7.getCallbackContext();	 Catch:{ RuntimeException -> 0x0098 }
            r12 = r5.getCapabilities();	 Catch:{ RuntimeException -> 0x0098 }
            r12 = r12.getSupportedPhotoSizes();	 Catch:{ RuntimeException -> 0x0098 }
            r13 = r5.getCameraId();	 Catch:{ RuntimeException -> 0x0098 }
            r10.init(r11, r12, r13, r9);	 Catch:{ RuntimeException -> 0x0098 }
        L_0x047c:
            r10 = r7.isBoostPreview();	 Catch:{ RuntimeException -> 0x0098 }
            if (r10 == 0) goto L_0x0486;
        L_0x0482:
            r7.onCameraOpenedBoost(r5);	 Catch:{ RuntimeException -> 0x0098 }
            goto L_0x0489;
        L_0x0486:
            r7.onCameraOpened(r5);	 Catch:{ RuntimeException -> 0x0098 }
        L_0x0489:
            goto L_0x049b;
        L_0x048a:
            if (r7 == 0) goto L_0x0493;
        L_0x048c:
            r5 = r1.generateHistoryString(r8);	 Catch:{ RuntimeException -> 0x0098 }
            r7.onDeviceOpenFailure(r8, r5);	 Catch:{ RuntimeException -> 0x0098 }
        L_0x0493:
            r5 = com.hmdglobal.app.camera.instantcapture.CameraLock.getInstance();	 Catch:{ RuntimeException -> 0x0098 }
            r5.open();	 Catch:{ RuntimeException -> 0x0098 }
        L_0x049b:
            r0 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.TAG;
            r4 = new java.lang.StringBuilder;
            r4.<init>();
        L_0x04a4:
            r5 = "handleMessage - action = '";
            r4.append(r5);
            r5 = r2.what;
            r5 = com.android.ex.camera2.portability.CameraActions.stringify(r5);
            r4.append(r5);
            r5 = "' done ";
            r4.append(r5);
            r4 = r4.toString();
            com.android.ex.camera2.portability.debug.Log.w(r0, r4);
            com.android.ex.camera2.portability.CameraAgent.WaitDoneBundle.unblockSyncWaiters(r18);
            goto L_0x0582;
        L_0x04c3:
            r5 = r0;
            r0 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.this;	 Catch:{ all -> 0x0095 }
            r0 = r0.mCameraState;	 Catch:{ all -> 0x0095 }
            r0 = r0.getState();	 Catch:{ all -> 0x0095 }
            r7 = r0;
            r0 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0095 }
            r0.<init>();	 Catch:{ all -> 0x0095 }
            r8 = "CameraAction[";
            r0.append(r8);	 Catch:{ all -> 0x0095 }
            r8 = com.android.ex.camera2.portability.CameraActions.stringify(r3);	 Catch:{ all -> 0x0095 }
            r0.append(r8);	 Catch:{ all -> 0x0095 }
            r8 = "] at CameraState[";
            r0.append(r8);	 Catch:{ all -> 0x0095 }
            r0.append(r7);	 Catch:{ all -> 0x0095 }
            r8 = "]";
            r0.append(r8);	 Catch:{ all -> 0x0095 }
            r0 = r0.toString();	 Catch:{ all -> 0x0095 }
            r8 = r0;
            r0 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.TAG;	 Catch:{ all -> 0x0095 }
            r9 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0095 }
            r9.<init>();	 Catch:{ all -> 0x0095 }
            r10 = "RuntimeException during ";
            r9.append(r10);	 Catch:{ all -> 0x0095 }
            r9.append(r8);	 Catch:{ all -> 0x0095 }
            r9 = r9.toString();	 Catch:{ all -> 0x0095 }
            com.android.ex.camera2.portability.debug.Log.e(r0, r9, r5);	 Catch:{ all -> 0x0095 }
            r0 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.this;	 Catch:{ all -> 0x0095 }
            r0 = r0.mCameraState;	 Catch:{ all -> 0x0095 }
            r0.invalidate();	 Catch:{ all -> 0x0095 }
            r0 = r1.mCamera;	 Catch:{ all -> 0x0095 }
            if (r0 == 0) goto L_0x054c;
        L_0x0517:
            r0 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.TAG;	 Catch:{ all -> 0x0095 }
            r9 = "Release camera since mCamera is not null.";
            com.android.ex.camera2.portability.debug.Log.i(r0, r9);	 Catch:{ all -> 0x0095 }
            r0 = r1.mCamera;	 Catch:{ Exception -> 0x0531 }
            r0.release();	 Catch:{ Exception -> 0x0531 }
            r1.mCamera = r4;	 Catch:{ all -> 0x0095 }
            r0 = com.hmdglobal.app.camera.instantcapture.CameraLock.getInstance();	 Catch:{ all -> 0x0095 }
        L_0x052b:
            r0.open();	 Catch:{ all -> 0x0095 }
            goto L_0x054c;
        L_0x052f:
            r0 = move-exception;
            goto L_0x0542;
        L_0x0531:
            r0 = move-exception;
            r9 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.TAG;	 Catch:{ all -> 0x052f }
            r10 = "Fail when calling Camera.release().";
            com.android.ex.camera2.portability.debug.Log.e(r9, r10, r0);	 Catch:{ all -> 0x052f }
            r1.mCamera = r4;	 Catch:{ all -> 0x0095 }
            r0 = com.hmdglobal.app.camera.instantcapture.CameraLock.getInstance();	 Catch:{ all -> 0x0095 }
            goto L_0x052b;
        L_0x0542:
            r1.mCamera = r4;	 Catch:{ all -> 0x0095 }
            r4 = com.hmdglobal.app.camera.instantcapture.CameraLock.getInstance();	 Catch:{ all -> 0x0095 }
            r4.open();	 Catch:{ all -> 0x0095 }
            throw r0;	 Catch:{ all -> 0x0095 }
        L_0x054c:
            r0 = r2.what;	 Catch:{ all -> 0x0095 }
            if (r0 != r6) goto L_0x0568;
        L_0x0550:
            r0 = r1.mCamera;	 Catch:{ all -> 0x0095 }
            if (r0 != 0) goto L_0x0568;
        L_0x0554:
            r0 = r2.arg1;	 Catch:{ all -> 0x0095 }
            r4 = r2.obj;	 Catch:{ all -> 0x0095 }
            if (r4 == 0) goto L_0x0567;
        L_0x055a:
            r4 = r2.obj;	 Catch:{ all -> 0x0095 }
            r4 = (com.android.ex.camera2.portability.CameraAgent.CameraOpenCallback) r4;	 Catch:{ all -> 0x0095 }
            r6 = r2.arg1;	 Catch:{ all -> 0x0095 }
            r9 = r1.generateHistoryString(r0);	 Catch:{ all -> 0x0095 }
            r4.onDeviceOpenFailure(r6, r9);	 Catch:{ all -> 0x0095 }
        L_0x0567:
            goto L_0x0577;
        L_0x0568:
            r0 = r1.mAgent;	 Catch:{ all -> 0x0095 }
            r0 = r0.getCameraExceptionHandler();	 Catch:{ all -> 0x0095 }
            r4 = r1.mCameraId;	 Catch:{ all -> 0x0095 }
            r4 = r1.generateHistoryString(r4);	 Catch:{ all -> 0x0095 }
            r0.onCameraException(r5, r4, r3, r7);	 Catch:{ all -> 0x0095 }
        L_0x0577:
            r0 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.TAG;
            r4 = new java.lang.StringBuilder;
            r4.<init>();
            goto L_0x04a4;
        L_0x0582:
            return;
        L_0x0583:
            r4 = com.android.ex.camera2.portability.AndroidCameraAgentImpl.TAG;
            r5 = new java.lang.StringBuilder;
            r5.<init>();
            r6 = "handleMessage - action = '";
            r5.append(r6);
            r6 = r2.what;
            r6 = com.android.ex.camera2.portability.CameraActions.stringify(r6);
            r5.append(r6);
            r6 = "' done ";
            r5.append(r6);
            r5 = r5.toString();
            com.android.ex.camera2.portability.debug.Log.w(r4, r5);
            com.android.ex.camera2.portability.CameraAgent.WaitDoneBundle.unblockSyncWaiters(r18);
            throw r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.ex.camera2.portability.AndroidCameraAgentImpl$CameraHandler.handleMessage(android.os.Message):void");
        }

        private Size getCachedPictureSize(SettingsManager settingsManager, boolean isCameraFacingFront) {
            String pictureSizeKey;
            if (isCameraFacingFront) {
                pictureSizeKey = Keys.KEY_PICTURE_SIZE_FRONT;
            } else {
                pictureSizeKey = Keys.KEY_PICTURE_SIZE_BACK;
            }
            return SettingsUtil.sizeFromString(settingsManager.getString(SettingsManager.SCOPE_GLOBAL, pictureSizeKey, SettingsUtil.getDefaultPictureSize(isCameraFacingFront)));
        }

        private void applyPreviewRelatedSettingsToParameters(BoostParameters settings, Parameters parameters) {
            Size size = settings.settingsManager;
            boolean z = true;
            if (settings.cameraId != 1) {
                z = false;
            }
            size = getCachedPictureSize(size, z);
            Context context = settings.context;
            z = settings.isZslOn;
            if (AndroidCameraAgentImpl.this.mCapabilities.getSupportedPhotoSizes().contains(size)) {
                Tag access$000 = AndroidCameraAgentImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("update photo size for ");
                stringBuilder.append(size);
                Log.i(access$000, stringBuilder.toString());
                parameters.setPictureSize(size.width(), size.height());
            }
            Size optimalSize = CameraUtil.getOptimalPreviewSize(context, AndroidCameraAgentImpl.this.mCapabilities.getSupportedPreviewSizes(), ((double) size.width()) / ((double) size.height()));
            parameters.set(CameraCapabilities.KEY_INSTANT_AEC, "1");
            parameters.setPreviewSize(optimalSize.width(), optimalSize.height());
            Tag access$0002 = AndroidCameraAgentImpl.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("preview size in boost is ");
            stringBuilder2.append(optimalSize);
            stringBuilder2.append(" zsl on ?");
            stringBuilder2.append(z);
            Log.w(access$0002, stringBuilder2.toString());
            ExtendParameters.getInstance(parameters).setZSLMode(z ? "on" : ExtendKey.FLIP_MODE_OFF);
        }

        private void applySettingsToParameters(CameraSettings settings, Parameters parameters) {
            String str;
            ExtendParameters extParams = ExtendParameters.getInstance(parameters);
            Stringifier stringifier = AndroidCameraAgentImpl.this.mCapabilities.getStringifier();
            Size photoSize = settings.getCurrentPhotoSize();
            Tag access$000 = AndroidCameraAgentImpl.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("KPI picture size is ");
            stringBuilder.append(photoSize);
            Log.w(access$000, stringBuilder.toString());
            parameters.setPictureSize(photoSize.width(), photoSize.height());
            Size videoSize = settings.getCurrentVideoSize();
            String str2 = CameraCapabilities.KEY_VIDEO_SIZE;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(Integer.toString(videoSize.width()));
            stringBuilder2.append("x");
            stringBuilder2.append(Integer.toString(videoSize.height()));
            parameters.set(str2, stringBuilder2.toString());
            Size previewSize = settings.getCurrentPreviewSize();
            parameters.setPreviewSize(previewSize.width(), previewSize.height());
            if (settings.getPreviewFrameRate() == -1) {
                parameters.setPreviewFpsRange(settings.getPreviewFpsRangeMin(), settings.getPreviewFpsRangeMax());
            } else {
                parameters.setPreviewFrameRate(settings.getPreviewFrameRate());
            }
            parameters.set(CameraCapabilities.KEY_INSTANT_AEC, "0");
            parameters.setPreviewFormat(settings.getCurrentPreviewFormat());
            parameters.setJpegQuality(settings.getPhotoJpegCompressionQuality());
            if (AndroidCameraAgentImpl.this.mCapabilities.supports(Feature.ZOOM)) {
                parameters.setZoom(zoomRatioToIndex(settings.getCurrentZoomRatio(), parameters.getZoomRatios()));
            }
            parameters.setExposureCompensation(settings.getExposureCompensationIndex());
            if (AndroidCameraAgentImpl.this.mCapabilities.supports(Feature.AUTO_EXPOSURE_LOCK)) {
                parameters.setAutoExposureLock(settings.isAutoExposureLocked());
            }
            parameters.setFocusMode(stringifier.stringify(settings.getCurrentFocusMode()));
            if (settings.getCurrentFocusMode() == FocusMode.MANUAL) {
                extParams.updateManualFocusPosition((int) settings.mManualFocusPosition);
            }
            if (CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_HMD_EIS, false)) {
                parameters.set(ExtendKey.HMD_EIS_ENABLE, settings.isVideoStabilizationEnabled() ? "on" : ExtendKey.FLIP_MODE_OFF);
            } else if (AndroidCameraAgentImpl.this.mCapabilities.supports(Feature.VIDEO_STABILIZATION)) {
                parameters.setVideoStabilization(settings.isVideoStabilizationEnabled());
            }
            if (AndroidCameraAgentImpl.this.mCapabilities.supports(Feature.AUTO_WHITE_BALANCE_LOCK)) {
                parameters.setAutoWhiteBalanceLock(settings.isAutoWhiteBalanceLocked());
            }
            if (AndroidCameraAgentImpl.this.mCapabilities.supports(Feature.FOCUS_AREA)) {
                if (settings.getFocusAreas().size() != 0) {
                    parameters.setFocusAreas(settings.getFocusAreas());
                } else {
                    parameters.setFocusAreas(null);
                }
            }
            if (AndroidCameraAgentImpl.this.mCapabilities.supports(Feature.METERING_AREA)) {
                if (settings.getMeteringAreas().size() != 0) {
                    parameters.setMeteringAreas(settings.getMeteringAreas());
                } else {
                    parameters.setMeteringAreas(null);
                }
            }
            if (settings.getCurrentFlashMode() != FlashMode.NO_FLASH) {
                if (!settings.isSuperResolutionOn()) {
                    parameters.setFlashMode(stringifier.stringify(settings.getCurrentFlashMode()));
                } else if (AndroidCameraAgentImpl.this.mCapabilities.supports(FlashMode.OFF)) {
                    parameters.setFlashMode(stringifier.stringify(FlashMode.OFF));
                }
            }
            parameters.setSceneMode("portrait");
            parameters.setRecordingHint(settings.isRecordingHintEnabled());
            Size jpegThumbSize = settings.getExifThumbnailSize();
            if (jpegThumbSize != null) {
                parameters.setJpegThumbnailSize(jpegThumbSize.width(), jpegThumbSize.height());
            } else {
                Camera.Size optimalThumbSize = CameraUtil.getOptimalExifSize(parameters.getSupportedJpegThumbnailSizes(), photoSize);
                if (optimalThumbSize != null) {
                    parameters.setJpegThumbnailSize(optimalThumbSize.width, optimalThumbSize.height);
                }
            }
            parameters.setPictureFormat(settings.getCurrentPhotoFormat());
            GpsData gpsData = settings.getGpsData();
            if (gpsData == null) {
                parameters.removeGpsData();
            } else {
                parameters.setGpsTimestamp(gpsData.timeStamp);
                if (gpsData.processingMethod != null) {
                    parameters.setGpsAltitude(gpsData.altitude);
                    parameters.setGpsLatitude(gpsData.latitude);
                    parameters.setGpsLongitude(gpsData.longitude);
                    parameters.setGpsProcessingMethod(gpsData.processingMethod);
                }
            }
            if (!TextUtils.isEmpty(settings.getISOValue())) {
                extParams.setISO(settings.getISOValue());
            }
            if (TextUtils.equals(settings.getISOValue(), "manual")) {
                extParams.setManualISO(settings.getContinuousIso());
            }
            String wb = whiteBalanceToString(settings.getWhiteBalance());
            if (wb != null) {
                parameters.setWhiteBalance(wb);
            }
            if (!TextUtils.isEmpty(settings.getExposureTime())) {
                extParams.setExposureTime(settings.getExposureTime());
            }
            extParams.setAutoExposure(ExposureMode.CENTER_WEIGHTED);
            Tag access$0002 = AndroidCameraAgentImpl.TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("set ZSL to ");
            stringBuilder3.append(settings.isZslOn);
            Log.v(access$0002, stringBuilder3.toString());
            extParams.setZSLMode(settings.isZslOn ? "on" : ExtendKey.FLIP_MODE_OFF);
            if (settings.isSuperResolutionOn()) {
                parameters.set(ExtendKey.VISIDON_MODE, ExtendKey.VISIDON_SUPER_RESOLUTION);
            } else if (settings.getLowLight()) {
                parameters.set(ExtendKey.VISIDON_MODE, ExtendKey.VISIDON_LOW_LIGHT);
            } else if (settings.getFaceBeauty()) {
                parameters.set(ExtendKey.VISIDON_MODE, ExtendKey.VISIDON_FACE_BEAUTY);
                str = ExtendKey.VISIDON_SKIN_SMOOTHING;
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("");
                stringBuilder3.append(settings.getFaceBeautySkinSmoothing());
                parameters.set(str, stringBuilder3.toString());
            } else {
                parameters.set(ExtendKey.VISIDON_MODE, "");
            }
            str = getAntibanding(settings.getAntibanding());
            if (str != null) {
                parameters.setAntibanding(str);
            }
            if (settings.getMirrorSelfieOn() && ExtendParameters.isFlipSupported(ExtendKey.FLIP_MODE_H)) {
                parameters.set(ExtendKey.KEY_QC_SNAPSHOT_PICTURE_FLIP, ExtendKey.FLIP_MODE_H);
            } else if (ExtendParameters.isFlipSupported(ExtendKey.FLIP_MODE_OFF)) {
                parameters.set(ExtendKey.KEY_QC_SNAPSHOT_PICTURE_FLIP, ExtendKey.FLIP_MODE_OFF);
            }
        }

        private String getAntibanding(String antibanding) {
            String antibandingValue = AndroidCameraAgentImpl.ANTIBANDING_AUTO_50;
            if (antibanding == null) {
                return antibandingValue;
            }
            Object obj = -1;
            int hashCode = antibanding.hashCode();
            if (hashCode != 1628397) {
                if (hashCode != 1658188) {
                    if (hashCode == 3005871 && antibanding.equals("auto")) {
                        obj = null;
                    }
                } else if (antibanding.equals("60hz")) {
                    obj = 2;
                }
            } else if (antibanding.equals("50hz")) {
                obj = 1;
            }
            switch (obj) {
                case null:
                    return AndroidCameraAgentImpl.ANTIBANDING_AUTO_50;
                case 1:
                    return AndroidCameraAgentImpl.ANTIBANDING_AUTO_50;
                case 2:
                    return AndroidCameraAgentImpl.ANTIBANDING_AUTO_60;
                default:
                    return AndroidCameraAgentImpl.ANTIBANDING_AUTO_50;
            }
        }

        private String whiteBalanceToString(WhiteBalance wb) {
            if (wb == null) {
                return null;
            }
            switch (wb) {
                case AUTO:
                    return "auto";
                case CLOUDY_DAYLIGHT:
                    return "cloudy-daylight";
                case DAYLIGHT:
                    return "daylight";
                case FLUORESCENT:
                    return "fluorescent";
                case INCANDESCENT:
                    return "incandescent";
                case SHADE:
                    return "shade";
                case TWILIGHT:
                    return "twilight";
                case WARM_FLUORESCENT:
                    return "warm-fluorescent";
                default:
                    Tag access$000 = AndroidCameraAgentImpl.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to convert from API 1 white balance: ");
                    stringBuilder.append(wb);
                    Log.w(access$000, stringBuilder.toString());
                    return null;
            }
        }

        private int zoomRatioToIndex(float ratio, List<Integer> percentages) {
            int index = Collections.binarySearch(percentages, Integer.valueOf((int) (1120403456 * ratio)));
            if (index >= 0) {
                return index;
            }
            index = -(index + 1);
            if (index == percentages.size()) {
                index--;
            }
            return index;
        }
    }

    AndroidCameraAgentImpl() {
        Log.v(TAG, "construct agent");
        ExtBuild.init();
        this.mCameraHandlerThread = new HandlerThread("Camera Handler Thread");
        this.mCameraHandlerThread.start();
        this.mCameraHandler = new CameraHandler(this, this.mCameraHandlerThread.getLooper());
        this.mExceptionHandler = new CameraExceptionHandler(this.mCameraHandler);
        this.mCameraState = new AndroidCameraStateHolder();
        this.mDispatchThread = new DispatchThread(this.mCameraHandler, this.mCameraHandlerThread);
        this.mDispatchThread.start();
    }

    public void recycle() {
        this.mRecycled = true;
        Log.v(TAG, "recycle agent");
        closeCamera(null, true);
        this.mDispatchThread.end();
        this.mCameraState.invalidate();
    }

    public boolean isRecycled() {
        return this.mRecycled;
    }

    public CameraDeviceInfo getCameraDeviceInfo() {
        return AndroidCameraDeviceInfo.create();
    }

    /* Access modifiers changed, original: protected */
    public Handler getCameraHandler() {
        return this.mCameraHandler;
    }

    /* Access modifiers changed, original: protected */
    public DispatchThread getDispatchThread() {
        return this.mDispatchThread;
    }

    /* Access modifiers changed, original: protected */
    public CameraStateHolder getCameraState() {
        return this.mCameraState;
    }

    /* Access modifiers changed, original: protected */
    public CameraExceptionHandler getCameraExceptionHandler() {
        return this.mExceptionHandler;
    }

    public void setCameraExceptionHandler(CameraExceptionHandler exceptionHandler) {
        this.mExceptionHandler = exceptionHandler != null ? exceptionHandler : sDefaultExceptionHandler;
    }
}
