package com.hmdglobal.app.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraDevice.StateCallback;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Builder;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.hmdglobal.app.camera.ButtonManager.ButtonCallback;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.app.CameraAppUI;
import com.hmdglobal.app.camera.app.CameraAppUI.BottomBarUISpec;
import com.hmdglobal.app.camera.app.LocationManager;
import com.hmdglobal.app.camera.app.MediaSaver.QueueListener;
import com.hmdglobal.app.camera.debug.DebugPropertyHelper;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.hardware.HardwareSpec;
import com.hmdglobal.app.camera.module.ModuleController;
import com.hmdglobal.app.camera.one.OneCamera;
import com.hmdglobal.app.camera.one.OneCamera.AutoFocusState;
import com.hmdglobal.app.camera.one.OneCamera.CaptureReadyCallback;
import com.hmdglobal.app.camera.one.OneCamera.Facing;
import com.hmdglobal.app.camera.one.OneCamera.FocusStateListener;
import com.hmdglobal.app.camera.one.OneCamera.OpenCallback;
import com.hmdglobal.app.camera.one.OneCamera.PhotoCaptureParameters;
import com.hmdglobal.app.camera.one.OneCamera.PhotoCaptureParameters.Flash;
import com.hmdglobal.app.camera.one.OneCamera.PictureCallback;
import com.hmdglobal.app.camera.one.OneCamera.ReadyStateChangedListener;
import com.hmdglobal.app.camera.one.OneCameraManager;
import com.hmdglobal.app.camera.one.Settings3A;
import com.hmdglobal.app.camera.one.v2.OneCameraManagerImpl;
import com.hmdglobal.app.camera.remote.RemoteCameraModule;
import com.hmdglobal.app.camera.session.CaptureSession;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.settings.SettingsManager.OnSettingChangedListener;
import com.hmdglobal.app.camera.ui.CountDownView.OnCountDownStatusListener;
import com.hmdglobal.app.camera.ui.PreviewStatusListener.PreviewAreaChangedListener;
import com.hmdglobal.app.camera.ui.TouchCoordinate;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.hmdglobal.app.camera.util.GcamHelper;
import com.hmdglobal.app.camera.util.GservicesHelper;
import com.hmdglobal.app.camera.util.Size;
import com.hmdglobal.app.camera.util.ToastUtil;
import com.hmdglobal.app.camera.util.UsageStatistics;
import com.morphoinc.utils.multimedia.MediaProviderUtils;
import java.io.File;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CaptureModule extends CameraModule implements QueueListener, ModuleController, OnCountDownStatusListener, PictureCallback, FocusStateListener, ReadyStateChangedListener, PreviewAreaChangedListener, RemoteCameraModule, SensorEventListener, OnSettingChangedListener, SurfaceTextureListener {
    public static final int BAYER_ID = 0;
    public static final int BAYER_MODE = 1;
    private static final int CAMERA_OPEN_CLOSE_TIMEOUT_MILLIS = 2500;
    private static final boolean CAPTURE_DEBUG_UI = DebugPropertyHelper.showCaptureDebugUI();
    private static final boolean DEBUG = true;
    public static final int DUAL_MODE = 0;
    private static final int FOCUS_HOLD_UI_MILLIS = 0;
    private static final int FOCUS_UI_TIMEOUT_MILLIS = 2000;
    public static int FRONT_ID = -1;
    public static final float FULLSCREEN_ASPECT_RATIO = 1.7777778f;
    private static final int MAX_NUM_CAM = 4;
    public static int MONO_ID = -1;
    public static final int MONO_MODE = 2;
    private static final int OPEN_CAMERA = 0;
    private static final String PHOTO_MODULE_STRING_ID = "PhotoModule";
    public static int SWITCH_ID = -1;
    public static final int SWITCH_MODE = 3;
    private static final Tag TAG = new Tag("CaptureModule");
    private Sensor mAccelerometerSensor;
    private final AppController mAppController;
    private long mAutoFocusScanStartFrame;
    private long mAutoFocusScanStartTime;
    private boolean mBokehEnabled;
    private Builder mBokehRequestBuilder;
    private OneCamera mCamera;
    private CameraDevice[] mCameraDevice;
    private Facing mCameraFacing;
    private Handler mCameraHandler;
    private String[] mCameraId;
    private OneCameraManager mCameraManager;
    private final Semaphore mCameraOpenCloseLock;
    private boolean[] mCameraOpened;
    private HandlerThread mCameraThread;
    private boolean mCamerasOpened;
    private CameraCaptureSession[] mCaptureSession;
    private final Context mContext;
    private int mControlAFMode;
    private SoundPlayer mCountdownSoundPlayer;
    private CameraCaptureSession mCurrentSession;
    private final File mDebugDataDir;
    private final Object mDimensionLock;
    private int mDisplayRotation;
    private boolean mFocusedAtEnd;
    private final float[] mGData;
    private boolean mHdrEnabled;
    private int mHeading;
    Runnable mHideAutoFocusTargetRunnable;
    private boolean mIsImageCaptureIntent;
    private final OnLayoutChangeListener mLayoutListener;
    private LocationManager mLocationManager;
    private final float[] mMData;
    private Sensor mMagneticSensor;
    private Handler mMainHandler;
    private int mOrientation;
    private boolean mPaused;
    RectF mPreviewArea;
    private int mPreviewBufferHeight;
    private int mPreviewBufferWidth;
    private Builder[] mPreviewRequestBuilder;
    private SurfaceTexture mPreviewTexture;
    private Matrix mPreviewTranformationMatrix;
    private final float[] mR;
    private int mScreenHeight;
    private int mScreenWidth;
    private SensorManager mSensorManager;
    private final SettingsManager mSettingsManager;
    private ModuleState mState;
    private final StateCallback mStateCallback;
    private final boolean mStickyGcamCamera;
    private final Object mSurfaceLock;
    private boolean mTapToFocusWaitForActiveScan;
    private int mTimerDuration;
    private CaptureModuleUI mUI;
    private float mZoomValue;

    private enum ModuleState {
        IDLE,
        WATCH_FOR_NEXT_FRAME_AFTER_PREVIEW_STARTED,
        UPDATE_TRANSFORM_ON_NEXT_SURFACE_TEXTURE_UPDATE
    }

    private class MyCameraHandler extends Handler {
        public MyCameraHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            int id = msg.arg1;
            if (msg.what == 0) {
                CaptureModule.this.mAppController.getCameraProvider().requestCamera(id, GservicesHelper.useCamera2ApiThroughPortabilityLayer(CaptureModule.this.mAppController.getAndroidContext()));
                CaptureModule.this.openCamera(id);
            }
        }
    }

    public CaptureModule(AppController appController) {
        this(appController, false);
    }

    public CaptureModule(AppController appController, boolean stickyHdr) {
        super(appController);
        this.mLayoutListener = new OnLayoutChangeListener() {
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                CaptureModule.this.updatePreviewTransform(right - left, bottom - top, false);
            }
        };
        this.mHideAutoFocusTargetRunnable = new Runnable() {
            public void run() {
                if (CaptureModule.this.mFocusedAtEnd) {
                    CaptureModule.this.mUI.showAutoFocusSuccess();
                } else {
                    CaptureModule.this.mUI.showAutoFocusFailure();
                }
            }
        };
        this.mDimensionLock = new Object();
        this.mSurfaceLock = new Object();
        this.mCameraOpenCloseLock = new Semaphore(1);
        this.mCameraFacing = Facing.BACK;
        this.mHdrEnabled = false;
        this.mState = ModuleState.IDLE;
        this.mOrientation = -1;
        this.mZoomValue = 1.0f;
        this.mTapToFocusWaitForActiveScan = false;
        this.mAutoFocusScanStartFrame = -1;
        this.mGData = new float[3];
        this.mMData = new float[3];
        this.mR = new float[16];
        this.mHeading = -1;
        this.mPreviewTranformationMatrix = new Matrix();
        this.mCameraDevice = new CameraDevice[4];
        this.mCameraOpened = new boolean[4];
        this.mPreviewRequestBuilder = new Builder[4];
        this.mCaptureSession = new CameraCaptureSession[4];
        this.mControlAFMode = 4;
        this.mBokehEnabled = false;
        this.mCamerasOpened = false;
        this.mCameraId = new String[4];
        this.mStateCallback = new StateCallback() {
            public void onOpened(CameraDevice cameraDevice) {
                int id = Integer.parseInt(cameraDevice.getId());
                Tag access$900 = CaptureModule.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onOpened ");
                stringBuilder.append(id);
                Log.d(access$900, stringBuilder.toString());
                CaptureModule.this.mCameraOpenCloseLock.release();
                if (!CaptureModule.this.mPaused) {
                    CaptureModule.this.mCameraDevice[id] = cameraDevice;
                    CaptureModule.this.mCameraOpened[id] = true;
                    if (CaptureModule.this.isBackCamera() && CaptureModule.this.getCameraMode() == 0 && id == 0) {
                        CaptureModule.this.mCameraHandler.sendMessage(CaptureModule.this.mCameraHandler.obtainMessage(0, CaptureModule.MONO_ID, 0));
                    } else {
                        CaptureModule.this.mCamerasOpened = true;
                        try {
                            CaptureModule.this.createSessions();
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            public void onDisconnected(CameraDevice cameraDevice) {
                int id = Integer.parseInt(cameraDevice.getId());
                Tag access$900 = CaptureModule.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onDisconnected ");
                stringBuilder.append(id);
                Log.d(access$900, stringBuilder.toString());
                cameraDevice.close();
                CaptureModule.this.mCameraDevice[id] = null;
                CaptureModule.this.mCameraOpenCloseLock.release();
                CaptureModule.this.mCamerasOpened = false;
            }

            public void onError(CameraDevice cameraDevice, int error) {
                int id = Integer.parseInt(cameraDevice.getId());
                Tag access$900 = CaptureModule.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onError ");
                stringBuilder.append(id);
                stringBuilder.append(" ");
                stringBuilder.append(error);
                Log.e(access$900, stringBuilder.toString());
                if (CaptureModule.this.mCamerasOpened) {
                    CaptureModule.this.mCameraDevice[id].close();
                    CaptureModule.this.mCameraDevice[id] = null;
                }
                CaptureModule.this.mCameraOpenCloseLock.release();
                CaptureModule.this.mCamerasOpened = false;
            }

            public void onClosed(CameraDevice cameraDevice) {
                int id = Integer.parseInt(cameraDevice.getId());
                Tag access$900 = CaptureModule.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onClosed ");
                stringBuilder.append(id);
                Log.d(access$900, stringBuilder.toString());
                CaptureModule.this.mCameraDevice[id] = null;
                CaptureModule.this.mCameraOpenCloseLock.release();
                CaptureModule.this.mCamerasOpened = false;
            }
        };
        this.mAppController = appController;
        this.mContext = this.mAppController.getAndroidContext();
        this.mSettingsManager = this.mAppController.getSettingsManager();
        this.mSettingsManager.addListener(this);
        this.mDebugDataDir = this.mContext.getExternalCacheDir();
        this.mStickyGcamCamera = stickyHdr;
    }

    public void init(CameraActivity activity, boolean isSecureCamera, boolean isCaptureIntent) {
        Log.d(TAG, "init");
        this.mMainHandler = new Handler(activity.getMainLooper());
        new HandlerThread("CaptureModule.mCameraHandler").start();
        this.mCameraThread = new HandlerThread("CaptureModule.mCameraHandler");
        this.mCameraThread.start();
        this.mCameraHandler = new MyCameraHandler(this.mCameraThread.getLooper());
        this.mCameraManager = this.mAppController.getCameraManager();
        this.mLocationManager = this.mAppController.getLocationManager();
        this.mDisplayRotation = CameraUtil.getDisplayRotation(this.mContext);
        this.mCameraFacing = getFacingFromCameraId(this.mSettingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID).intValue());
        this.mUI = new CaptureModuleUI(activity, this, this.mAppController.getModuleLayoutRoot(), this.mLayoutListener);
        this.mAppController.setPreviewStatusListener(this.mUI);
        this.mPreviewTexture = this.mAppController.getCameraAppUI().getSurfaceTexture();
        this.mSensorManager = (SensorManager) this.mContext.getSystemService("sensor");
        boolean z = true;
        this.mAccelerometerSensor = this.mSensorManager.getDefaultSensor(1);
        this.mMagneticSensor = this.mSensorManager.getDefaultSensor(2);
        this.mCountdownSoundPlayer = new SoundPlayer(this.mContext);
        String action = activity.getIntent().getAction();
        if (!("android.media.action.IMAGE_CAPTURE".equals(action) || CameraActivity.ACTION_IMAGE_CAPTURE_SECURE.equals(action))) {
            z = false;
        }
        this.mIsImageCaptureIntent = z;
        activity.findViewById(R.id.shutter_cancel_button).setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                CaptureModule.this.cancelCountDown();
            }
        });
    }

    public void onShutterButtonFocus(boolean pressed) {
    }

    public void onShutterCoordinate(TouchCoordinate coord) {
    }

    public void onShutterButtonClick() {
        if (this.mCamera != null) {
            int countDownDuration = this.mSettingsManager.getInteger(this.mAppController.getCameraScope(), Keys.KEY_COUNTDOWN_DURATION).intValue();
            this.mTimerDuration = countDownDuration;
            if (countDownDuration > 0) {
                this.mAppController.getCameraAppUI().transitionToCancel();
                this.mAppController.getCameraAppUI().hideModeOptions();
                this.mUI.setCountdownFinishedListener(this);
                this.mUI.startCountdown(countDownDuration);
            } else {
                takePictureNow();
            }
        }
    }

    public void onShutterButtonLongClick() {
    }

    private void takePictureNow() {
        Location location = this.mLocationManager.getCurrentLocation();
        long sessionTime = System.currentTimeMillis();
        String title = CameraUtil.createJpegName(sessionTime);
        CaptureSession session = getServices().getCaptureSessionManager().createNewSession(title, sessionTime, location);
        PhotoCaptureParameters params = new PhotoCaptureParameters();
        params.title = title;
        params.callback = this;
        params.orientation = getOrientation();
        params.flashMode = getFlashModeFromSettings();
        params.heading = this.mHeading;
        params.debugDataFolder = this.mDebugDataDir;
        params.location = location;
        params.zoom = this.mZoomValue;
        params.timerSeconds = this.mTimerDuration > 0 ? Float.valueOf((float) this.mTimerDuration) : null;
        this.mCamera.takePicture(params, session);
    }

    public void onCountDownFinished() {
        this.mAppController.getCameraAppUI().transitionToCapture();
        this.mAppController.getCameraAppUI().showModeOptions();
        if (!this.mPaused) {
            takePictureNow();
        }
    }

    public void onRemainingSecondsChanged(int remainingSeconds) {
        if (remainingSeconds == 1) {
            this.mCountdownSoundPlayer.play(R.raw.timer_final_second, 0.6f);
        } else if (remainingSeconds == 2 || remainingSeconds == 3) {
            this.mCountdownSoundPlayer.play(R.raw.timer_increment, 0.6f);
        }
    }

    private void cancelCountDown() {
        if (this.mUI.isCountingDown()) {
            this.mUI.cancelCountDown();
        }
        this.mAppController.getCameraAppUI().showModeOptions();
        this.mAppController.getCameraAppUI().transitionToCapture();
    }

    public void onQuickExpose() {
        this.mMainHandler.post(new Runnable() {
            public void run() {
                CaptureModule.this.mAppController.startPreCaptureAnimation(true);
            }
        });
    }

    public void onPreviewAreaChanged(RectF previewArea) {
        this.mPreviewArea = previewArea;
        this.mUI.onPreviewAreaChanged(previewArea);
        this.mUI.positionProgressOverlay(previewArea);
    }

    public void onSensorChanged(SensorEvent event) {
        float[] data;
        int type = event.sensor.getType();
        if (type == 1) {
            data = this.mGData;
        } else if (type == 2) {
            data = this.mMData;
        } else {
            Log.w(TAG, String.format("Unexpected sensor type %s", new Object[]{event.sensor.getName()}));
            return;
        }
        for (int i = 0; i < 3; i++) {
            data[i] = event.values[i];
        }
        float[] orientation = new float[3];
        SensorManager.getRotationMatrix(this.mR, null, this.mGData, this.mMData);
        SensorManager.getOrientation(this.mR, orientation);
        this.mHeading = ((int) (((double) (orientation[0] * 180.0f)) / 3.141592653589793d)) % 360;
        if (this.mHeading < 0) {
            this.mHeading += 360;
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onQueueStatus(boolean full) {
    }

    public void onRemoteShutterPress() {
        Log.d(TAG, "onRemoteShutterPress");
        takePictureNow();
    }

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureAvailable");
        updatePreviewTransform(width, height, true);
        initSurface(surface);
    }

    public void initSurface(SurfaceTexture surface) {
        this.mPreviewTexture = surface;
        closeCamera();
        openCameraAndStartPreview();
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureSizeChanged");
        resetDefaultBufferSize();
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d(TAG, "onSurfaceTextureDestroyed");
        this.mPreviewTexture = null;
        closeCamera();
        return true;
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (this.mState == ModuleState.UPDATE_TRANSFORM_ON_NEXT_SURFACE_TEXTURE_UPDATE) {
            Log.d(TAG, "onSurfaceTextureUpdated --> updatePreviewTransform");
            this.mState = ModuleState.IDLE;
            CameraAppUI appUI = this.mAppController.getCameraAppUI();
            updatePreviewTransform(appUI.getSurfaceWidth(), appUI.getSurfaceHeight(), true);
        }
    }

    public String getModuleStringIdentifier() {
        return PHOTO_MODULE_STRING_ID;
    }

    public void resume() {
        Message msg = Message.obtain();
        boolean z = false;
        msg.what = 0;
        if (Keys.isCameraBackFacing(this.mSettingsManager, SettingsManager.SCOPE_GLOBAL)) {
            switch (getCameraMode()) {
                case 0:
                case 1:
                    msg.arg1 = 0;
                    this.mCameraHandler.sendMessage(msg);
                    break;
                case 2:
                    msg.arg1 = MONO_ID;
                    this.mCameraHandler.sendMessage(msg);
                    break;
                case 3:
                    msg.arg1 = SWITCH_ID;
                    this.mCameraHandler.sendMessage(msg);
                    break;
            }
        }
        msg.arg1 = SWITCH_ID == -1 ? FRONT_ID : SWITCH_ID;
        this.mCameraHandler.sendMessage(msg);
        this.mPaused = false;
        this.mAppController.getCameraAppUI().onChangeCamera();
        this.mAppController.addPreviewAreaSizeChangedListener(this);
        resetDefaultBufferSize();
        getServices().getRemoteShutterListener().onModuleReady(this, -1);
        this.mAppController.getCameraAppUI().enableModeOptions();
        this.mAppController.setShutterEnabled(true);
        if (this.mAccelerometerSensor != null) {
            this.mSensorManager.registerListener(this, this.mAccelerometerSensor, 3);
        }
        if (this.mMagneticSensor != null) {
            this.mSensorManager.registerListener(this, this.mMagneticSensor, 3);
        }
        if (this.mStickyGcamCamera || Keys.isHdrOn(this.mAppController.getSettingsManager())) {
            z = true;
        }
        this.mHdrEnabled = z;
        if (this.mPreviewTexture != null) {
            initSurface(this.mPreviewTexture);
        }
        this.mCountdownSoundPlayer.loadSound(R.raw.timer_final_second);
        this.mCountdownSoundPlayer.loadSound(R.raw.timer_increment);
    }

    public void pause() {
        this.mPaused = true;
        getServices().getRemoteShutterListener().onModuleExit();
        cancelCountDown();
        closeCamera();
        resetTextureBufferSize();
        try {
            this.mCameraThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.mCameraThread.quitSafely();
        this.mCameraThread = null;
        this.mCameraHandler = null;
        this.mCountdownSoundPlayer.unloadSound(R.raw.timer_final_second);
        this.mCountdownSoundPlayer.unloadSound(R.raw.timer_increment);
        this.mMainHandler.removeCallbacksAndMessages(null);
        if (this.mAccelerometerSensor != null) {
            this.mSensorManager.unregisterListener(this, this.mAccelerometerSensor);
        }
        if (this.mMagneticSensor != null) {
            this.mSensorManager.unregisterListener(this, this.mMagneticSensor);
        }
        ToastUtil.cancelToast();
    }

    public boolean isPaused() {
        return this.mPaused;
    }

    public void destroy() {
        this.mCountdownSoundPlayer.release();
    }

    public void onLayoutOrientationChanged(boolean isLandscape) {
        Log.d(TAG, "onLayoutOrientationChanged");
    }

    public void onOrientationChanged(int orientation) {
        if (orientation != -1) {
            this.mOrientation = (360 - orientation) % 360;
        }
    }

    public void onCameraAvailable(CameraProxy cameraProxy) {
    }

    public void hardResetSettings(SettingsManager settingsManager) {
        if (this.mStickyGcamCamera) {
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR_PLUS, true);
            if (this.mCameraManager != null) {
                settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID, getBackFacingCameraId());
            } else {
                settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID);
            }
        }
    }

    public HardwareSpec getHardwareSpec() {
        return new HardwareSpec() {
            public boolean isFrontCameraSupported() {
                return true;
            }

            public boolean isHdrSupported() {
                return false;
            }

            public boolean isHdrPlusSupported() {
                return GcamHelper.hasGcamCapture();
            }

            public boolean isFlashSupported() {
                return true;
            }
        };
    }

    public BottomBarUISpec getBottomBarSpec() {
        BottomBarUISpec bottomBarSpec = new BottomBarUISpec();
        bottomBarSpec.enableGridLines = true;
        bottomBarSpec.enableCamera = true;
        bottomBarSpec.cameraCallback = getCameraCallback();
        bottomBarSpec.enableHdr = GcamHelper.hasGcamCapture();
        bottomBarSpec.hdrCallback = getHdrButtonCallback();
        bottomBarSpec.enableSelfTimer = true;
        bottomBarSpec.showSelfTimer = true;
        if (!this.mHdrEnabled) {
            bottomBarSpec.enableFlash = true;
        }
        if (this.mStickyGcamCamera) {
            bottomBarSpec.enableFlash = false;
        }
        return bottomBarSpec;
    }

    public boolean isUsingBottomBar() {
        return true;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 23:
            case 27:
                if (this.mUI.isCountingDown()) {
                    cancelCountDown();
                } else if (event.getRepeatCount() == 0) {
                    onShutterButtonClick();
                }
                return true;
            case 24:
            case 25:
                return true;
            default:
                return false;
        }
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 24:
            case 25:
                onShutterButtonClick();
                return true;
            default:
                return false;
        }
    }

    public void onSingleTapUp(View view, int x, int y) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onSingleTapUp x=");
        stringBuilder.append(x);
        stringBuilder.append(" y=");
        stringBuilder.append(y);
        Log.v(tag, stringBuilder.toString());
        if (this.mCameraFacing != Facing.FRONT) {
            triggerFocusAtScreenCoord(x, y);
        }
    }

    private void triggerFocusAtScreenCoord(int x, int y) {
        if (this.mCamera != null) {
            this.mTapToFocusWaitForActiveScan = true;
            float minEdge = Math.min(this.mPreviewArea.width(), this.mPreviewArea.height());
            this.mUI.setAutoFocusTarget(x, y, false, (int) ((Settings3A.getAutoFocusRegionWidth() * this.mZoomValue) * minEdge), (int) ((Settings3A.getMeteringRegionWidth() * this.mZoomValue) * minEdge));
            this.mUI.showAutoFocusInProgress();
            this.mMainHandler.removeCallbacks(this.mHideAutoFocusTargetRunnable);
            this.mMainHandler.postDelayed(new Runnable() {
                public void run() {
                    CaptureModule.this.mMainHandler.post(CaptureModule.this.mHideAutoFocusTargetRunnable);
                }
            }, 2000);
            float[] points = new float[]{(((float) x) - this.mPreviewArea.left) / this.mPreviewArea.width(), (((float) y) - this.mPreviewArea.top) / this.mPreviewArea.height()};
            Matrix rotationMatrix = new Matrix();
            rotationMatrix.setRotate((float) this.mDisplayRotation, 0.5f, 0.5f);
            rotationMatrix.mapPoints(points);
            this.mCamera.triggerFocusAndMeterAtPoint(points[0], points[1]);
            if (this.mZoomValue == 1.0f) {
                UsageStatistics.instance().tapToFocus(new TouchCoordinate(((float) x) - this.mPreviewArea.left, ((float) y) - this.mPreviewArea.top, this.mPreviewArea.width(), this.mPreviewArea.height()), null);
            }
        }
    }

    private void setAutoFocusTargetPassive() {
        float minEdge = Math.min(this.mPreviewArea.width(), this.mPreviewArea.height());
        this.mUI.setAutoFocusTarget((int) this.mPreviewArea.centerX(), (int) this.mPreviewArea.centerY(), true, (int) ((Settings3A.getAutoFocusRegionWidth() * this.mZoomValue) * minEdge), (int) ((Settings3A.getMeteringRegionWidth() * this.mZoomValue) * minEdge));
        this.mUI.showAutoFocusInProgress();
    }

    public void onFocusStatusUpdate(final AutoFocusState state, long frameNumber) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("AF status is state:");
        stringBuilder.append(state);
        Log.v(tag, stringBuilder.toString());
        boolean z = false;
        switch (state) {
            case PASSIVE_SCAN:
                this.mMainHandler.removeCallbacks(this.mHideAutoFocusTargetRunnable);
                this.mMainHandler.post(new Runnable() {
                    public void run() {
                        CaptureModule.this.setAutoFocusTargetPassive();
                    }
                });
                break;
            case ACTIVE_SCAN:
                this.mTapToFocusWaitForActiveScan = false;
                break;
            case PASSIVE_FOCUSED:
            case PASSIVE_UNFOCUSED:
                this.mMainHandler.post(new Runnable() {
                    public void run() {
                        CaptureModule.this.mUI.setPassiveFocusSuccess(state == AutoFocusState.PASSIVE_FOCUSED);
                    }
                });
                break;
            case ACTIVE_FOCUSED:
            case ACTIVE_UNFOCUSED:
                if (!this.mTapToFocusWaitForActiveScan) {
                    if (state != AutoFocusState.ACTIVE_UNFOCUSED) {
                        z = true;
                    }
                    this.mFocusedAtEnd = z;
                    this.mMainHandler.removeCallbacks(this.mHideAutoFocusTargetRunnable);
                    this.mMainHandler.post(this.mHideAutoFocusTargetRunnable);
                    break;
                }
                break;
        }
        if (CAPTURE_DEBUG_UI) {
            measureAutoFocusScans(state, frameNumber);
        }
    }

    private void measureAutoFocusScans(AutoFocusState state, long frameNumber) {
        long j = frameNumber;
        boolean passive = false;
        switch (state) {
            case PASSIVE_SCAN:
            case ACTIVE_SCAN:
                if (this.mAutoFocusScanStartFrame == -1) {
                    this.mAutoFocusScanStartFrame = j;
                    this.mAutoFocusScanStartTime = SystemClock.uptimeMillis();
                    return;
                }
                return;
            case PASSIVE_FOCUSED:
            case PASSIVE_UNFOCUSED:
                passive = true;
                break;
            case ACTIVE_FOCUSED:
            case ACTIVE_UNFOCUSED:
                break;
            default:
                return;
        }
        if (this.mAutoFocusScanStartFrame != -1) {
            long frames = j - this.mAutoFocusScanStartFrame;
            int fps = Math.round((((float) frames) * 1000.0f) / ((float) (SystemClock.uptimeMillis() - this.mAutoFocusScanStartTime)));
            String str = "%s scan: fps=%d frames=%d";
            Object[] objArr = new Object[3];
            objArr[0] = passive ? "CAF" : "AF";
            objArr[1] = Integer.valueOf(fps);
            objArr[2] = Long.valueOf(frames);
            Log.v(TAG, String.format(str, objArr));
            this.mUI.showDebugMessage(String.format("%d / %d", new Object[]{Long.valueOf(frames), Integer.valueOf(fps)}));
            this.mAutoFocusScanStartFrame = -1;
        }
    }

    public void onReadyStateChanged(boolean readyForCapture) {
        if (readyForCapture) {
            this.mAppController.getCameraAppUI().enableModeOptions();
        }
        this.mAppController.setShutterEnabled(readyForCapture);
    }

    public String getPeekAccessibilityString() {
        return this.mAppController.getAndroidContext().getResources().getString(R.string.photo_accessibility_peek);
    }

    public void onThumbnailResult(byte[] jpegData) {
        getServices().getRemoteShutterListener().onPictureTaken(jpegData);
    }

    public void onPictureTaken(CaptureSession session) {
        this.mAppController.getCameraAppUI().enableModeOptions();
    }

    public void onPictureSaved(Uri uri) {
        this.mAppController.notifyNewMedia(uri);
    }

    public void onTakePictureProgress(float progress) {
        this.mUI.setPictureTakingProgress((int) (100.0f * progress));
    }

    public void onPictureTakenFailed() {
    }

    public void onSettingChanged(SettingsManager settingsManager, String key) {
    }

    public void updatePreviewTransform() {
        int width;
        int height;
        synchronized (this.mDimensionLock) {
            width = this.mScreenWidth;
            height = this.mScreenHeight;
        }
        updatePreviewTransform(width, height);
    }

    public void setZoom(float zoom) {
        this.mZoomValue = zoom;
        if (this.mCamera != null) {
            this.mCamera.setZoom(zoom);
        }
    }

    private String getBackFacingCameraId() {
        if (this.mCameraManager instanceof OneCameraManagerImpl) {
            return this.mCameraManager.getFirstBackCameraId();
        }
        throw new IllegalStateException("This should never be called with Camera API V1");
    }

    private ButtonCallback getHdrButtonCallback() {
        return this.mStickyGcamCamera ? new ButtonCallback() {
            public void onStateChanged(int state) {
                if (!CaptureModule.this.mPaused) {
                    if (state != 1) {
                        CaptureModule.this.mAppController.getSettingsManager().set(CaptureModule.this.mAppController.getModuleScope(), Keys.KEY_REQUEST_RETURN_HDR_PLUS, false);
                        CaptureModule.this.switchToRegularCapture();
                        return;
                    }
                    throw new IllegalStateException("Can't leave hdr plus mode if switching to hdr plus mode.");
                }
            }
        } : new ButtonCallback() {
            public void onStateChanged(int hdrEnabled) {
                if (!CaptureModule.this.mPaused) {
                    Tag access$900 = CaptureModule.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("HDR enabled =");
                    stringBuilder.append(hdrEnabled);
                    Log.d(access$900, stringBuilder.toString());
                    CaptureModule captureModule = CaptureModule.this;
                    boolean z = true;
                    if (hdrEnabled != 1) {
                        z = false;
                    }
                    captureModule.mHdrEnabled = z;
                    CaptureModule.this.switchCamera();
                }
            }
        };
    }

    private ButtonCallback getCameraCallback() {
        return this.mStickyGcamCamera ? new ButtonCallback() {
            public void onStateChanged(int state) {
                if (!CaptureModule.this.mPaused) {
                    SettingsManager settingsManager = CaptureModule.this.mAppController.getSettingsManager();
                    if (Keys.isCameraBackFacing(settingsManager, SettingsManager.SCOPE_GLOBAL)) {
                        throw new IllegalStateException("Hdr plus should never be switching from front facing camera.");
                    }
                    settingsManager.set(CaptureModule.this.mAppController.getModuleScope(), Keys.KEY_REQUEST_RETURN_HDR_PLUS, true);
                    CaptureModule.this.switchToRegularCapture();
                }
            }
        } : new ButtonCallback() {
            public void onStateChanged(int cameraId) {
                if (!CaptureModule.this.mPaused) {
                    CaptureModule.this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID, cameraId);
                    Tag access$900 = CaptureModule.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Start to switch camera. cameraId=");
                    stringBuilder.append(cameraId);
                    Log.d(access$900, stringBuilder.toString());
                    CaptureModule.this.mCameraFacing = CaptureModule.getFacingFromCameraId(cameraId);
                    CaptureModule.this.switchCamera();
                }
            }
        };
    }

    private void switchToRegularCapture() {
        this.mAppController.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR_PLUS, false);
        ButtonManager buttonManager = this.mAppController.getButtonManager();
        buttonManager.disableButtonClick(4);
        this.mAppController.getCameraAppUI().freezeScreenUntilPreviewReady();
        this.mAppController.onModeSelected(this.mContext.getResources().getInteger(R.integer.camera_mode_photo));
        buttonManager.enableButtonClick(4);
    }

    private void onPreviewStarted() {
        if (this.mState == ModuleState.WATCH_FOR_NEXT_FRAME_AFTER_PREVIEW_STARTED) {
            this.mState = ModuleState.UPDATE_TRANSFORM_ON_NEXT_SURFACE_TEXTURE_UPDATE;
        }
        this.mAppController.onPreviewStarted();
    }

    private void updatePreviewTransform(int incomingWidth, int incomingHeight) {
        updatePreviewTransform(incomingWidth, incomingHeight, false);
    }

    private void updatePreviewTransform(int incomingWidth, int incomingHeight, boolean forceUpdate) {
        int i = incomingWidth;
        int i2 = incomingHeight;
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updatePreviewTransform: ");
        stringBuilder.append(i);
        stringBuilder.append(" x ");
        stringBuilder.append(i2);
        Log.d(tag, stringBuilder.toString());
        synchronized (this.mDimensionLock) {
            int incomingRotation = CameraUtil.getDisplayRotation(this.mContext);
            if (this.mScreenHeight == i2 && this.mScreenWidth == i && incomingRotation == this.mDisplayRotation && !forceUpdate) {
                return;
            }
            this.mDisplayRotation = incomingRotation;
            this.mScreenWidth = i;
            this.mScreenHeight = i2;
            updatePreviewBufferDimension();
            this.mPreviewTranformationMatrix = this.mAppController.getCameraAppUI().getPreviewTransform(this.mPreviewTranformationMatrix);
            int width = this.mScreenWidth;
            int height = this.mScreenHeight;
            int naturalOrientation = CaptureModuleUtil.getDeviceNaturalOrientation(this.mContext);
            int effectiveWidth = this.mPreviewBufferWidth;
            int effectiveHeight = this.mPreviewBufferHeight;
            Tag tag2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Rotation: ");
            stringBuilder2.append(this.mDisplayRotation);
            Log.v(tag2, stringBuilder2.toString());
            tag2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Screen Width: ");
            stringBuilder2.append(this.mScreenWidth);
            Log.v(tag2, stringBuilder2.toString());
            tag2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Screen Height: ");
            stringBuilder2.append(this.mScreenHeight);
            Log.v(tag2, stringBuilder2.toString());
            tag2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Buffer width: ");
            stringBuilder2.append(this.mPreviewBufferWidth);
            Log.v(tag2, stringBuilder2.toString());
            tag2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Buffer height: ");
            stringBuilder2.append(this.mPreviewBufferHeight);
            Log.v(tag2, stringBuilder2.toString());
            tag2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Natural orientation: ");
            stringBuilder2.append(naturalOrientation);
            Log.v(tag2, stringBuilder2.toString());
            if (naturalOrientation == 1) {
                int temp = effectiveWidth;
                effectiveWidth = effectiveHeight;
                effectiveHeight = temp;
            }
            RectF viewRect = new RectF(0.0f, 0.0f, (float) width, (float) height);
            RectF bufRect = new RectF(0.0f, 0.0f, (float) effectiveWidth, (float) effectiveHeight);
            float centerX = viewRect.centerX();
            float centerY = viewRect.centerY();
            bufRect.offset(centerX - bufRect.centerX(), centerY - bufRect.centerY());
            this.mPreviewTranformationMatrix.setRectToRect(viewRect, bufRect, ScaleToFit.FILL);
            this.mPreviewTranformationMatrix.postRotate((float) getPreviewOrientation(this.mDisplayRotation), centerX, centerY);
            if (this.mDisplayRotation % MediaProviderUtils.ROTATION_180 == 90) {
                incomingRotation = effectiveWidth;
                effectiveWidth = effectiveHeight;
                effectiveHeight = incomingRotation;
            }
            float scale = Math.min(((float) width) / ((float) effectiveWidth), ((float) height) / ((float) effectiveHeight));
            this.mPreviewTranformationMatrix.postScale(scale, scale, centerX, centerY);
            float previewWidth = ((float) effectiveWidth) * scale;
            this.mPreviewTranformationMatrix.postTranslate((previewWidth / 2.0f) - centerX, ((((float) effectiveHeight) * scale) / 2.0f) - centerY);
            this.mAppController.updatePreviewTransform(this.mPreviewTranformationMatrix);
        }
    }

    private void updatePreviewBufferDimension() {
        if (this.mCamera != null) {
            Size previewBufferSize = this.mCamera.pickPreviewSize(getPictureSizeFromSettings(), this.mContext);
            this.mPreviewBufferWidth = previewBufferSize.getWidth();
            this.mPreviewBufferHeight = previewBufferSize.getHeight();
        }
    }

    private void resetDefaultBufferSize() {
        synchronized (this.mSurfaceLock) {
            if (this.mPreviewTexture != null) {
                this.mPreviewTexture.setDefaultBufferSize(this.mPreviewBufferWidth, this.mPreviewBufferHeight);
            }
        }
    }

    private void openCameraAndStartPreview() {
        boolean z = this.mHdrEnabled && this.mCameraFacing == Facing.BACK;
        boolean useHdr = z;
        try {
            if (!this.mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to acquire camera-open lock.");
            } else if (this.mCamera != null) {
                Log.d(TAG, "Camera already open, not re-opening.");
                this.mCameraOpenCloseLock.release();
            } else {
                this.mCameraManager.open(this.mCameraFacing, useHdr, getPictureSizeFromSettings(), new OpenCallback() {
                    public void onFailure() {
                        Log.e(CaptureModule.TAG, "Could not open camera.");
                        CaptureModule.this.mCamera = null;
                        CaptureModule.this.mCameraOpenCloseLock.release();
                        CaptureModule.this.mAppController.showErrorAndFinish(R.string.cannot_connect_camera);
                    }

                    public void onCameraClosed() {
                        CaptureModule.this.mCamera = null;
                        CaptureModule.this.mCameraOpenCloseLock.release();
                    }

                    public void onCameraOpened(OneCamera camera) {
                        Tag access$900 = CaptureModule.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("onCameraOpened: ");
                        stringBuilder.append(camera);
                        Log.d(access$900, stringBuilder.toString());
                        CaptureModule.this.mCamera = camera;
                        CaptureModule.this.updatePreviewBufferDimension();
                        CaptureModule.this.resetDefaultBufferSize();
                        CaptureModule.this.mState = ModuleState.WATCH_FOR_NEXT_FRAME_AFTER_PREVIEW_STARTED;
                        Log.d(CaptureModule.TAG, "starting preview ...");
                        camera.startPreview(new Surface(CaptureModule.this.mPreviewTexture), new CaptureReadyCallback() {
                            public void onSetupFailed() {
                                CaptureModule.this.mCameraOpenCloseLock.release();
                                Log.e(CaptureModule.TAG, "Could not set up preview.");
                                CaptureModule.this.mMainHandler.post(new Runnable() {
                                    public void run() {
                                        if (CaptureModule.this.mCamera == null) {
                                            Log.d(CaptureModule.TAG, "Camera closed, aborting.");
                                            return;
                                        }
                                        CaptureModule.this.mCamera.close(null);
                                        CaptureModule.this.mCamera = null;
                                    }
                                });
                            }

                            public void onReadyForCapture() {
                                CaptureModule.this.mCameraOpenCloseLock.release();
                                CaptureModule.this.mMainHandler.post(new Runnable() {
                                    public void run() {
                                        Log.d(CaptureModule.TAG, "Ready for capture.");
                                        if (CaptureModule.this.mCamera == null) {
                                            Log.d(CaptureModule.TAG, "Camera closed, aborting.");
                                            return;
                                        }
                                        CaptureModule.this.onPreviewStarted();
                                        CaptureModule.this.mUI.initializeZoom(CaptureModule.this.mCamera.getMaxZoom());
                                        CaptureModule.this.mCamera.setFocusStateListener(CaptureModule.this);
                                        CaptureModule.this.mCamera.setReadyStateChangedListener(CaptureModule.this);
                                    }
                                });
                            }
                        });
                    }
                }, this.mCameraHandler);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while waiting to acquire camera-open lock.", e);
        }
    }

    private void closeCamera() {
        try {
            this.mCameraOpenCloseLock.acquire();
            try {
                if (this.mCamera != null) {
                    this.mCamera.close(null);
                    this.mCamera.setFocusStateListener(null);
                    this.mCamera = null;
                }
                this.mCameraOpenCloseLock.release();
            } catch (Throwable th) {
                this.mCameraOpenCloseLock.release();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while waiting to acquire camera-open lock.", e);
        }
    }

    private int getOrientation() {
        if (this.mAppController.isAutoRotateScreen()) {
            return this.mDisplayRotation;
        }
        return this.mOrientation;
    }

    private static boolean isResumeFromLockscreen(Activity activity) {
        String action = activity.getIntent().getAction();
        return "android.media.action.STILL_IMAGE_CAMERA".equals(action) || CameraActivity.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE.equals(action);
    }

    private void switchCamera() {
        if (!this.mPaused) {
            cancelCountDown();
            this.mAppController.freezeScreenUntilPreviewReady();
            initSurface(this.mPreviewTexture);
        }
    }

    private Size getPictureSizeFromSettings() {
        String pictureSizeKey;
        if (this.mCameraFacing == Facing.FRONT) {
            pictureSizeKey = Keys.KEY_PICTURE_SIZE_FRONT;
        } else {
            pictureSizeKey = Keys.KEY_PICTURE_SIZE_BACK;
        }
        return this.mSettingsManager.getSize(SettingsManager.SCOPE_GLOBAL, pictureSizeKey);
    }

    private int getPreviewOrientation(int deviceOrientationDegrees) {
        if (this.mCameraFacing == Facing.FRONT) {
            deviceOrientationDegrees += MediaProviderUtils.ROTATION_180;
        }
        return (360 - deviceOrientationDegrees) % 360;
    }

    private static Facing getFacingFromCameraId(int cameraId) {
        return cameraId == 1 ? Facing.FRONT : Facing.BACK;
    }

    private void resetTextureBufferSize() {
        if (this.mPreviewTexture != null) {
            this.mPreviewTexture.setDefaultBufferSize(this.mAppController.getCameraAppUI().getSurfaceWidth(), this.mAppController.getCameraAppUI().getSurfaceHeight());
        }
    }

    private Flash getFlashModeFromSettings() {
        try {
            return Flash.valueOf(this.mSettingsManager.getString(this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE).toUpperCase());
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Could not parse Flash Setting. Defaulting to AUTO.");
            return Flash.AUTO;
        }
    }

    public int getModuleId() {
        return -1;
    }

    private void applyBokehSettings(Builder builder, int id) {
        builder.set(CaptureRequest.CONTROL_MODE, Integer.valueOf(1));
        builder.set(CaptureRequest.CONTROL_AF_MODE, Integer.valueOf(this.mControlAFMode));
        enableBokeh(builder);
    }

    private void enableBokeh(Builder request) {
        if (this.mBokehEnabled) {
            this.mBokehRequestBuilder = request;
            try {
                PreferenceManager.getDefaultSharedPreferences((Context) this.mAppController);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "can not find vendor tag : org.codeaurora.qcamera3.bokeh");
            }
        }
    }

    public boolean isCameraOpened() {
        return true;
    }

    private void openCamera(int id) {
        if (!this.mPaused) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("openCamera ");
            stringBuilder.append(id);
            Log.d(tag, stringBuilder.toString());
            try {
                CameraManager manager = (CameraManager) this.mContext.getSystemService("camera");
                this.mCameraId[id] = manager.getCameraIdList()[id];
                if (this.mCameraOpenCloseLock.tryAcquire(5000, TimeUnit.MILLISECONDS)) {
                    manager.openCamera(this.mCameraId[id], this.mStateCallback, this.mCameraHandler);
                } else {
                    Log.d(TAG, "Time out waiting to lock camera opening.");
                    throw new RuntimeException("Time out waiting to lock camera opening");
                }
            } catch (CameraAccessException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void initializePreviewConfiguration(int id) {
        this.mPreviewRequestBuilder[id].set(CaptureRequest.CONTROL_AF_TRIGGER, Integer.valueOf(0));
        applyBokehSettings(this.mPreviewRequestBuilder[id], id);
    }

    private Builder getRequestBuilder(int id) throws CameraAccessException {
        if (id == getMainCameraId()) {
            return this.mCameraDevice[id].createCaptureRequest(5);
        }
        return this.mCameraDevice[id].createCaptureRequest(1);
    }

    private void createSession(final int id) throws CameraAccessException {
        if (!this.mPaused && this.mCameraOpened[id]) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("createSession ");
            stringBuilder.append(id);
            Log.d(tag, stringBuilder.toString());
            this.mPreviewRequestBuilder[id] = getRequestBuilder(id);
            this.mPreviewRequestBuilder[id].setTag(Integer.valueOf(id));
            CameraCaptureSession.StateCallback captureSessionCallback = new CameraCaptureSession.StateCallback() {
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    if (!CaptureModule.this.mPaused && CaptureModule.this.mCameraDevice[id] != null) {
                        CaptureModule.this.mCaptureSession[id] = cameraCaptureSession;
                        if (id == CaptureModule.this.getMainCameraId()) {
                            CaptureModule.this.mCurrentSession = cameraCaptureSession;
                        }
                        CaptureModule.this.initializePreviewConfiguration(id);
                    }
                }

                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Tag access$900 = CaptureModule.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("cameracapturesession - onConfigureFailed ");
                    stringBuilder.append(id);
                    Log.e(access$900, stringBuilder.toString());
                }

                public void onClosed(CameraCaptureSession session) {
                    Log.d(CaptureModule.TAG, "cameracapturesession - onClosed");
                }
            };
        }
    }

    public void createSessions() throws CameraAccessException {
        if (!this.mPaused) {
            if (Keys.isCameraBackFacing(this.mSettingsManager, SettingsManager.SCOPE_GLOBAL)) {
                switch (getCameraMode()) {
                    case 0:
                        createSession(0);
                        createSession(MONO_ID);
                        break;
                    case 1:
                        createSession(0);
                        break;
                    case 2:
                        createSession(MONO_ID);
                        break;
                    case 3:
                        createSession(SWITCH_ID);
                        break;
                }
            }
            createSession(SWITCH_ID == -1 ? FRONT_ID : SWITCH_ID);
        }
    }

    public int getMainCameraId() {
        if (Keys.isCameraBackFacing(this.mSettingsManager, SettingsManager.SCOPE_GLOBAL)) {
            switch (getCameraMode()) {
                case 0:
                case 1:
                    return 0;
                case 2:
                    return MONO_ID;
                case 3:
                    return SWITCH_ID;
                default:
                    return 0;
            }
        }
        return SWITCH_ID == -1 ? FRONT_ID : SWITCH_ID;
    }

    public boolean isBackCamera() {
        if (Keys.isCameraBackFacing(this.mSettingsManager, SettingsManager.SCOPE_GLOBAL)) {
            return true;
        }
        return false;
    }

    public int getCameraMode() {
        String switchValue = this.mSettingsManager.getString(SettingsManager.SCOPE_GLOBAL, Keys.KEY_SWITCH_CAMERA);
        if (switchValue == null || switchValue.equals("-1")) {
            return -1;
        }
        SWITCH_ID = Integer.parseInt(this.mSettingsManager.getString(SettingsManager.SCOPE_GLOBAL, Keys.KEY_SWITCH_CAMERA));
        return 3;
    }
}
