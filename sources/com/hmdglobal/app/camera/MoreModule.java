package com.hmdglobal.app.camera;

import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.view.KeyEvent;
import com.android.ex.camera2.portability.CameraAgent.CameraAFCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.android.ex.camera2.portability.CameraAgent.CameraStartPreviewCallback;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraCapabilities.Feature;
import com.android.ex.camera2.portability.CameraCapabilities.FocusMode;
import com.android.ex.camera2.portability.CameraCapabilities.Stringifier;
import com.android.ex.camera2.portability.CameraDeviceInfo.Characteristics;
import com.android.ex.camera2.portability.CameraSettings;
import com.android.ex.camera2.portability.Size;
import com.android.external.ExtendKey;
import com.hmdglobal.app.camera.FocusOverlayManager.Listener;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.app.CameraAppUI.BottomBarUISpec;
import com.hmdglobal.app.camera.app.CameraAppUI.BottomBarUISpec.ExposureCompensationSetCallback;
import com.hmdglobal.app.camera.app.MotionManager;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.hardware.HardwareSpec;
import com.hmdglobal.app.camera.hardware.HardwareSpecImpl;
import com.hmdglobal.app.camera.module.ModuleController;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.settings.SettingsManager.OnSettingChangedListener;
import com.hmdglobal.app.camera.settings.SettingsUtil;
import com.hmdglobal.app.camera.ui.TouchCoordinate;
import com.hmdglobal.app.camera.util.BoostUtil;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.hmdglobal.app.camera.util.CustomFields;
import com.hmdglobal.app.camera.util.CustomUtil;
import com.hmdglobal.app.camera.util.GcamHelper;
import com.hmdglobal.app.camera.util.GservicesHelper;
import com.hmdglobal.app.camera.util.SessionStatsCollector;
import com.hmdglobal.app.camera.util.ToastUtil;
import java.util.ArrayList;

public class MoreModule extends CameraModule implements PhotoController, ModuleController, Listener, OnSettingChangedListener {
    private static final String PHOTO_MODULE_STRING_ID = "MoreModule";
    private static final Tag TAG = new Tag(PHOTO_MODULE_STRING_ID);
    public static boolean firstFrame = true;
    int curOritation = -1;
    protected CameraActivity mActivity;
    protected AppController mAppController;
    private final AutoFocusCallback mAutoFocusCallback = new AutoFocusCallback(this, null);
    protected CameraCapabilities mCameraCapabilities;
    protected CameraProxy mCameraDevice;
    private int mCameraId;
    private boolean mCameraKeyLongPressed = false;
    protected CameraSettings mCameraSettings;
    protected int mCameraState = 0;
    private int mDisplayOrientation;
    protected int mDisplayRotation;
    private final Runnable mDoSnapRunnable = new Runnable() {
        public void run() {
            MoreModule.this.onShutterButtonClick();
        }
    };
    private Integer mEvoFlashLock = null;
    private boolean mFaceDetectionStarted = false;
    private boolean mFocusAreaSupported;
    protected FocusOverlayManager mFocusManager;
    protected final Handler mHandler = new MainHandler(this);
    private boolean mIsImageCaptureIntent;
    private boolean mIsInIntentReviewUI = false;
    private int mLockedEvoIndex = 0;
    private boolean mMeteringAreaSupported;
    private boolean mMirror;
    private MotionManager mMotionManager;
    private long mOnResumeTime;
    protected int mOrientation = 0;
    protected boolean mPaused;
    private int mSensorOrientation;
    private boolean mSnapshotOnIdle = false;
    private final float mSuperZoomThreshold = 1.5f;
    private MoreUI mUI;
    protected boolean mVolumeButtonClickedFlag = false;
    private Integer mZoomFlashLock = null;
    private float mZoomValue;

    private class MainHandler extends Handler {
        public MainHandler(MoreModule module) {
            super(Looper.getMainLooper());
        }

        public void handleMessage(Message msg) {
        }
    }

    private final class AutoFocusCallback implements CameraAFCallback {
        private AutoFocusCallback() {
        }

        /* synthetic */ AutoFocusCallback(MoreModule x0, AnonymousClass1 x1) {
            this();
        }

        public void onAutoFocus(boolean focused, CameraProxy camera) {
            SessionStatsCollector.instance().autofocusResult(focused);
            if (!MoreModule.this.mPaused && !MoreModule.this.isInBurstshot()) {
                if (!focused || MoreModule.this.mFocusManager == null || MoreModule.this.mFocusManager.getFocusAreas() == null) {
                    Log.v(MoreModule.TAG, "focus failed , set camera state back to IDLE");
                    MoreModule.this.setCameraState(1);
                } else {
                    Log.v(MoreModule.TAG, "focus succeed , show exposure slider");
                    if (MoreModule.this.mCameraState == 2) {
                        MoreModule.this.mUI.showEvoSlider();
                    }
                    MoreModule.this.setCameraState(6);
                }
                MoreModule.this.mFocusManager.onAutoFocus(focused, FocusOverlayManager.ACTION_RESTORE_CAF_LATER);
            }
        }
    }

    public MoreModule(AppController app) {
        super(app);
    }

    public String getPeekAccessibilityString() {
        return this.mAppController.getAndroidContext().getResources().getString(R.string.photo_accessibility_peek);
    }

    public String getModuleStringIdentifier() {
        return PHOTO_MODULE_STRING_ID;
    }

    private void checkDisplayRotation() {
        if (!this.mPaused) {
            if (CameraUtil.getDisplayRotation(this.mActivity) != this.mDisplayRotation) {
                setDisplayOrientation();
            }
            if (SystemClock.uptimeMillis() - this.mOnResumeTime < 5000) {
                this.mHandler.postDelayed(new Runnable() {
                    public void run() {
                        MoreModule.this.checkDisplayRotation();
                    }
                }, 100);
            }
        }
    }

    /* Access modifiers changed, original: protected */
    public MoreUI getMoreUI() {
        if (this.mUI == null) {
            this.mUI = new MoreUI(this.mActivity, this, this.mActivity.getModuleLayoutRoot());
        }
        return this.mUI;
    }

    public void init(CameraActivity activity, boolean isSecureCamera, boolean isCaptureIntent) {
        this.mActivity = activity;
        this.mAppController = this.mActivity;
        this.mUI = getMoreUI();
        this.mActivity.getCameraAppUI().transitionToCapture();
        this.mCameraId = this.mActivity.getSettingsManager().getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID).intValue();
        this.mIsImageCaptureIntent = isImageCaptureIntent();
    }

    /* Access modifiers changed, original: protected */
    public void updateParametersPictureSize() {
        if (this.mCameraDevice == null) {
            Log.w(TAG, "attempting to set picture size without camera device");
            return;
        }
        String pictureSizeKey;
        Tag tag;
        StringBuilder stringBuilder;
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        if (isCameraFrontFacing()) {
            pictureSizeKey = Keys.KEY_PICTURE_SIZE_FRONT;
        } else {
            pictureSizeKey = Keys.KEY_PICTURE_SIZE_BACK;
        }
        Size size = SettingsUtil.sizeFromString(settingsManager.getString(SettingsManager.SCOPE_GLOBAL, pictureSizeKey, SettingsUtil.getDefaultPictureSize(isCameraFrontFacing())));
        this.mCameraSettings.setPhotoSize(size);
        Size optimalSize = CameraUtil.getOptimalPreviewSize(this.mActivity, this.mCameraCapabilities.getSupportedPreviewSizes(), ((double) size.width()) / ((double) size.height()));
        Size original = this.mCameraSettings.getCurrentPreviewSize();
        Log.w(TAG, String.format("KPI original size is %s, optimal size is %s", new Object[]{original.toString(), optimalSize.toString()}));
        if (!optimalSize.equals(original)) {
            tag = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setting preview size. optimal: ");
            stringBuilder.append(optimalSize);
            stringBuilder.append("original: ");
            stringBuilder.append(original);
            Log.v(tag, stringBuilder.toString());
            this.mCameraSettings.setPreviewSize(optimalSize);
            this.mCameraDevice.applySettings(this.mCameraSettings);
            this.mCameraSettings = this.mCameraDevice.getSettings();
            if (this.mCameraSettings == null) {
                Log.e(TAG, "camera setting is null ?");
            }
        }
        if (!(optimalSize.width() == 0 || optimalSize.height() == 0)) {
            Log.v(TAG, "updating aspect ratio");
            this.mUI.updatePreviewAspectRatio(((float) optimalSize.width()) / ((float) optimalSize.height()));
        }
        tag = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Preview size is ");
        stringBuilder.append(optimalSize);
        Log.d(tag, stringBuilder.toString());
    }

    public boolean isUsingBottomBar() {
        return true;
    }

    /* Access modifiers changed, original: protected */
    public void onPreviewStarted() {
        if (!this.mPaused) {
            Log.w(TAG, "KPI photo preview started");
            this.mAppController.onPreviewStarted();
            if (this.mCameraState == 7) {
                setCameraState(5);
            } else {
                setCameraState(1);
            }
            startFaceDetection();
            BoostUtil.getInstance().releaseCpuLock();
        }
    }

    public void updateCameraOrientation() {
        if (this.mDisplayRotation != CameraUtil.getDisplayRotation(this.mActivity)) {
            setDisplayOrientation();
        }
    }

    private void setDisplayOrientation() {
        this.mDisplayRotation = CameraUtil.getDisplayRotation(this.mActivity);
        Characteristics info = this.mActivity.getCameraProvider().getCharacteristics(this.mCameraId);
        this.mDisplayOrientation = info.getPreviewOrientation(this.mDisplayRotation);
        this.mSensorOrientation = info.getSensorOrientation();
        this.mUI.setSensorOrientation(this.mSensorOrientation);
        this.mUI.setDisplayOrientation(this.mDisplayOrientation);
        this.mUI.setGestureMirrored(isCameraFrontFacing());
        if (this.mFocusManager != null) {
            this.mFocusManager.setDisplayOrientation(this.mDisplayOrientation);
        }
        if (this.mCameraDevice != null) {
            this.mCameraDevice.setDisplayOrientation(this.mDisplayRotation);
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setPostGestureRotation (screen:preview) ");
        stringBuilder.append(this.mDisplayRotation);
        stringBuilder.append(":");
        stringBuilder.append(this.mDisplayOrientation);
        Log.v(tag, stringBuilder.toString());
    }

    public void onPreviewUIReady() {
        Log.i(TAG, "onPreviewUIReady");
        startPreview();
    }

    public void onPreviewUIDestroyed() {
        if (this.mCameraDevice != null) {
            this.mCameraDevice.setPreviewTexture(null);
            stopPreview();
        }
    }

    public void startPreCaptureAnimation() {
        this.mAppController.startPreCaptureAnimation();
    }

    private void requestCameraOpen() {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("requestCameraOpen ");
        stringBuilder.append(this.mCameraId);
        Log.w(tag, stringBuilder.toString());
        this.mActivity.getCameraProvider().requestCamera(this.mCameraId, GservicesHelper.useCamera2ApiThroughPortabilityLayer(this.mActivity.getAndroidContext()));
    }

    public void hardResetSettings(SettingsManager settingsManager) {
        settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR_PLUS, false);
        if (GcamHelper.hasGcamAsSeparateModule()) {
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR, ExtendKey.FLIP_MODE_OFF);
        }
    }

    public HardwareSpec getHardwareSpec() {
        return this.mCameraSettings != null ? new HardwareSpecImpl(getCameraProvider(), this.mCameraCapabilities) : null;
    }

    public BottomBarUISpec getBottomBarSpec() {
        BottomBarUISpec bottomBarSpec = new BottomBarUISpec();
        bottomBarSpec.enableCamera = true;
        bottomBarSpec.hideCamera = hideCamera();
        bottomBarSpec.hideCameraForced = hideCameraForced();
        bottomBarSpec.hideSetting = hideSetting();
        boolean z = this.mActivity.currentBatteryStatusOK() && (this.mZoomValue <= 1.5f || !isSuperResolutionEnabled());
        bottomBarSpec.enableFlash = z;
        bottomBarSpec.hideHdr = true;
        bottomBarSpec.enableHdr = true;
        bottomBarSpec.enableGridLines = true;
        bottomBarSpec.hideGridLines = true;
        bottomBarSpec.hideLowlight = isLowLightShow() ^ 1;
        if (this.mCameraCapabilities != null) {
            bottomBarSpec.enableExposureCompensation = true;
            bottomBarSpec.exposureCompensationSetCallback = new ExposureCompensationSetCallback() {
                public void setExposure(int value) {
                    MoreModule.this.setExposureCompensation(value);
                }
            };
            bottomBarSpec.minExposureCompensation = this.mCameraCapabilities.getMinExposureCompensation();
            bottomBarSpec.maxExposureCompensation = this.mCameraCapabilities.getMaxExposureCompensation();
            bottomBarSpec.exposureCompensationStep = this.mCameraCapabilities.getExposureCompensationStep();
        }
        z = isCountDownShow();
        bottomBarSpec.enableSelfTimer = z;
        bottomBarSpec.showSelfTimer = z;
        bottomBarSpec.hideFlash = false;
        if (isCameraFrontFacing()) {
            ModuleController controller = this.mAppController.getCurrentModuleController();
            if (!(controller.getHardwareSpec() == null || controller.getHardwareSpec().isFlashSupported())) {
                bottomBarSpec.hideFlash = true;
            }
        }
        if (isImageCaptureIntent()) {
            bottomBarSpec.showCancel = true;
            bottomBarSpec.showDone = true;
            bottomBarSpec.showRetake = true;
        }
        bottomBarSpec.showWrapperButton = false;
        bottomBarSpec.hideHdr = true;
        return bottomBarSpec;
    }

    public void updatePreviewAspectRatio(float aspectRatio) {
        this.mAppController.updatePreviewAspectRatio(aspectRatio);
    }

    /* JADX WARNING: Missing block: B:8:0x0021, code skipped:
            return;
     */
    public void startFaceDetection() {
        /*
        r2 = this;
        r0 = r2.mFaceDetectionStarted;
        if (r0 != 0) goto L_0x0021;
    L_0x0004:
        r0 = r2.mCameraDevice;
        if (r0 != 0) goto L_0x0009;
    L_0x0008:
        goto L_0x0021;
    L_0x0009:
        r0 = r2.mCameraCapabilities;
        r0 = r0.getMaxNumOfFacesSupported();
        if (r0 <= 0) goto L_0x0020;
    L_0x0011:
        r0 = 1;
        r2.mFaceDetectionStarted = r0;
        r1 = r2.mCameraDevice;
        r1.startFaceDetection();
        r1 = com.hmdglobal.app.camera.util.SessionStatsCollector.instance();
        r1.faceScanActive(r0);
    L_0x0020:
        return;
    L_0x0021:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.MoreModule.startFaceDetection():void");
    }

    public void stopFaceDetection() {
        Log.d(TAG, "stopFaceDetection 523");
        if (this.mCameraCapabilities.getMaxNumOfFacesSupported() > 0) {
            this.mFaceDetectionStarted = false;
            this.mCameraDevice.setFaceDetectionCallback(null, null);
            this.mCameraDevice.stopFaceDetection();
            SessionStatsCollector.instance().faceScanActive(false);
        }
    }

    public int getModuleId() {
        return this.mAppController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_more);
    }

    /* Access modifiers changed, original: protected */
    public void setCameraState(int state) {
        Log.w(TAG, String.format("set camera State: %d", new Object[]{Integer.valueOf(state)}));
        this.mCameraState = state;
        switch (state) {
            case 1:
                this.mAppController.getLockEventListener().onIdle();
                if (!this.mIsInIntentReviewUI && !this.mUI.isCountingDown()) {
                    this.mAppController.getCameraAppUI().showModeOptions();
                    return;
                }
                return;
            default:
                return;
        }
    }

    public boolean capture() {
        Log.w(TAG, "capture");
        return true;
    }

    public void setFocusParameters() {
    }

    public void onOrientationChanged(int orientation) {
        if (orientation != -1) {
            this.mUI.onOrientationChanged(CameraUtil.roundOrientation(orientation, this.mOrientation));
            this.mOrientation = (360 - orientation) % 360;
            this.mUI.setPostGestureOrientation((this.mSensorOrientation + this.mOrientation) % 360);
            this.mUI.setGestureDisplayOrientation(this.mOrientation);
            if (this.mCameraDevice != null) {
                orientation = getJpegRotation(this.mCameraDevice.getCameraId(), orientation);
                if (this.curOritation != orientation) {
                    this.curOritation = orientation;
                    Camera camera = this.mCameraDevice.getCamera();
                    if (camera != null) {
                        Parameters mParameters = camera.getParameters();
                        if (mParameters != null) {
                            mParameters.setRotation(this.curOritation);
                            this.mCameraDevice.getCamera().setParameters(mParameters);
                        }
                    }
                }
            } else {
                Log.e(TAG, "CameraDevice = null, can't set Parameter.setRotation");
            }
        }
    }

    public static int getJpegRotation(int cameraId, int orientation) {
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        if (orientation == -1) {
            return info.orientation;
        }
        if (info.facing == 1) {
            return ((info.orientation - orientation) + 360) % 360;
        }
        return (info.orientation + orientation) % 360;
    }

    public void onCameraAvailable(CameraProxy cameraProxy) {
        Log.i(TAG, "onCameraAvailable");
        if (!this.mPaused) {
            this.mCameraDevice = cameraProxy;
            this.mCameraDevice.initExtCamera(this.mActivity);
            this.mCameraDevice.setModuleId(getModuleId(), this.mActivity.getWindowManager().getDefaultDisplay().getRotation());
            initializeCapabilities();
            this.mZoomValue = 1.0f;
            if (this.mFocusManager == null) {
                initializeFocusManager();
            }
            this.mFocusManager.updateCapabilities(this.mCameraCapabilities);
            this.mCameraSettings = this.mCameraDevice.getSettings();
            if (this.mCameraSettings == null) {
                Log.e(TAG, "camera setting is null");
            }
            this.mActivity.getSettingsManager().addListener(this);
            startPreview();
        }
    }

    public void onCaptureCancelled() {
        this.mActivity.setResultEx(0, new Intent());
        this.mActivity.finish();
    }

    public void onCaptureRetake() {
        Log.i(TAG, "onCaptureRetake");
        if (!this.mPaused) {
            this.mIsInIntentReviewUI = false;
            this.mUI.hidePostCaptureAlert();
            this.mUI.hideIntentReviewImageView();
            setupPreview();
        }
    }

    public void onCaptureDone() {
        Log.i(TAG, "onCaptureDone");
    }

    public void onShutterCoordinate(TouchCoordinate coord) {
    }

    public void onShutterButtonFocus(boolean pressed) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ShutterButtonFocus ,pressed=");
        stringBuilder.append(pressed);
        Log.w(tag, stringBuilder.toString());
    }

    public void onShutterButtonClick() {
    }

    public void onShutterButtonLongClick() {
    }

    public void resume() {
        this.mPaused = false;
        Log.w(TAG, "KPI Track photo resume E");
        if (this.mActivity != null) {
            this.mCameraId = this.mActivity.getSettingsManager().getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID).intValue();
        }
        if (this.mFocusManager != null) {
            this.mAppController.addPreviewAreaSizeChangedListener(this.mFocusManager);
        }
        this.mAppController.addPreviewAreaSizeChangedListener(this.mUI);
        if (this.mActivity.getCameraProvider() != null) {
            if (!this.mActivity.getCameraProvider().isCameraRequestBoosted()) {
                requestCameraOpen();
            }
            this.mZoomValue = 1.0f;
            this.mOnResumeTime = SystemClock.uptimeMillis();
            checkDisplayRotation();
            SessionStatsCollector.instance().sessionActive(true);
            Log.w(TAG, "KPI Track photo resume X");
        }
    }

    public void preparePause() {
        if (this.mCameraState == 1) {
            stopPreview();
        }
    }

    /* Access modifiers changed, original: protected */
    public boolean isCameraFrontFacing() {
        return this.mAppController.getCameraProvider().getCharacteristics(this.mCameraId).isFacingFront();
    }

    /* Access modifiers changed, original: protected */
    public boolean isLowLightShow() {
        return false;
    }

    /* Access modifiers changed, original: protected */
    public boolean isSuperResolutionEnabled() {
        return false;
    }

    /* Access modifiers changed, original: protected */
    public boolean isHdrShow() {
        return Keys.isCameraBackFacing(this.mActivity.getSettingsManager(), SettingsManager.SCOPE_GLOBAL);
    }

    /* Access modifiers changed, original: protected */
    public boolean isCountDownShow() {
        boolean isCanCountDown = isCameraFrontFacing() ? CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_FRONTFACING_COUNT_DOWN, false) : CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_BACKFACING_COUNT_DOWN, false);
        if (!CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_COUNT_DOWN_ONLY_AUTO_MODE, false) || getModuleId() == this.mActivity.getResources().getInteger(R.integer.camera_mode_photo)) {
            return isCanCountDown;
        }
        return false;
    }

    private void initializeFocusManager() {
        if (this.mFocusManager != null) {
            this.mFocusManager.removeMessages();
        } else {
            this.mMirror = isCameraFrontFacing();
            String[] defaultFocusModesStrings = this.mActivity.getResources().getStringArray(R.array.pref_camera_focusmode_default_array);
            ArrayList<FocusMode> defaultFocusModes = new ArrayList();
            Stringifier stringifier = this.mCameraCapabilities.getStringifier();
            for (String modeString : defaultFocusModesStrings) {
                FocusMode mode = stringifier.focusModeFromString(modeString);
                if (mode != null) {
                    defaultFocusModes.add(mode);
                }
            }
            this.mFocusManager = new FocusOverlayManager(this.mAppController, defaultFocusModes, this.mCameraCapabilities, this, this.mMirror, this.mActivity.getMainLooper(), this.mUI.getFocusUI());
            this.mMotionManager = getServices().getMotionManager();
            if (this.mMotionManager != null) {
                this.mMotionManager.addListener(this.mFocusManager);
            }
        }
        this.mAppController.addPreviewAreaSizeChangedListener(this.mFocusManager);
    }

    public boolean isPaused() {
        return this.mPaused;
    }

    public void pause() {
        Log.v(TAG, "KPI photo pause E");
        this.mPaused = true;
        firstFrame = true;
        getServices().getRemoteShutterListener().onModuleExit();
        SessionStatsCollector.instance().sessionActive(false);
        if (!(this.mCameraDevice == null || this.mCameraState == 0)) {
            this.mCameraDevice.cancelAutoFocus();
        }
        if (this.mZoomFlashLock != null) {
            this.mAppController.getButtonManager().enableButtonWithToken(0, this.mZoomFlashLock.intValue());
            this.mZoomFlashLock = null;
        }
        stopPreview();
        this.mHandler.removeCallbacksAndMessages(null);
        if (this.mMotionManager != null) {
            this.mMotionManager.removeListener(this.mFocusManager);
            this.mMotionManager = null;
        }
        closeCamera();
        this.mActivity.enableKeepScreenOn(false);
        if (this.mFocusManager != null) {
            this.mFocusManager.removeMessages();
        }
        this.mAppController.removePreviewAreaSizeChangedListener(this.mFocusManager);
        this.mAppController.removePreviewAreaSizeChangedListener(this.mUI);
        this.mActivity.getSettingsManager().removeListener(this);
        ToastUtil.cancelToast();
        Log.w(TAG, "KPI photo pause X");
    }

    public void destroy() {
    }

    public void onLayoutOrientationChanged(boolean isLandscape) {
        setDisplayOrientation();
    }

    public void autoFocus() {
        if (this.mCameraDevice != null && !isInBurstshot()) {
            if (this.mFocusManager.getFocusAreas() == null) {
                this.mAutoFocusCallback.onAutoFocus(true, this.mCameraDevice);
                return;
            }
            Log.v(TAG, "Starting auto focus");
            this.mCameraDevice.autoFocus(this.mHandler, this.mAutoFocusCallback);
            SessionStatsCollector.instance().autofocusManualTrigger();
            if (this.mCameraState != 3) {
                setCameraState(2);
            }
        }
    }

    public boolean cancelAutoFocus() {
        if (this.mCameraDevice == null) {
            return false;
        }
        if (isInBurstshot() && this.mEvoFlashLock != null) {
            this.mAppController.getButtonManager().enableButtonWithToken(0, this.mEvoFlashLock.intValue());
            this.mEvoFlashLock = null;
        }
        if (this.mCameraState == 5 || isInBurstshot()) {
            return false;
        }
        Log.w(TAG, "cancel autofocus");
        this.mCameraDevice.cancelAutoFocus();
        this.mFocusManager.setAeAwbLock(false);
        if (this.mEvoFlashLock != null) {
            this.mAppController.getButtonManager().enableButtonWithToken(0, this.mEvoFlashLock.intValue());
            this.mEvoFlashLock = null;
        }
        if (this.mCameraState != 0) {
            setCameraState(1);
        }
        return true;
    }

    /* Access modifiers changed, original: protected */
    public boolean isInBurstshot() {
        return this.mCameraState == 8 || this.mCameraState == 9 || this.mCameraState == 10;
    }

    public void onLongPress(float x, float y) {
    }

    public void onEvoChanged(int index) {
        if (this.mCameraState == 6 && index != 0) {
            setCameraState(5);
        }
        if (this.mCameraState == 5 && this.mFocusManager != null) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("evo index is ");
            stringBuilder.append(index);
            Log.w(tag, stringBuilder.toString());
            this.mLockedEvoIndex = index;
            if (this.mEvoFlashLock == null) {
                this.mEvoFlashLock = Integer.valueOf(this.mAppController.getButtonManager().disableButtonWithLock(0));
            }
            this.mFocusManager.setAeAwbLock(true);
            this.mFocusManager.keepFocusFrame();
            setExposureCompensation(index, false);
        }
    }

    /* JADX WARNING: Missing block: B:41:0x0085, code skipped:
            return;
     */
    public void onSingleTapUp(android.view.View r5, int r6, int r7) {
        /*
        r4 = this;
        r0 = r4.mPaused;
        if (r0 != 0) goto L_0x0085;
    L_0x0004:
        r0 = r4.mCameraDevice;
        if (r0 == 0) goto L_0x0085;
    L_0x0008:
        r0 = r4.mCameraState;
        r1 = 3;
        if (r0 == r1) goto L_0x0085;
    L_0x000d:
        r0 = r4.mCameraState;
        r1 = 7;
        if (r0 == r1) goto L_0x0085;
    L_0x0012:
        r0 = r4.mCameraState;
        r1 = 8;
        if (r0 == r1) goto L_0x0085;
    L_0x0018:
        r0 = r4.mCameraState;
        r1 = 9;
        if (r0 == r1) goto L_0x0085;
    L_0x001e:
        r0 = r4.mCameraState;
        r1 = 10;
        if (r0 == r1) goto L_0x0085;
    L_0x0024:
        r0 = r4.mCameraState;
        r1 = 4;
        if (r0 == r1) goto L_0x0085;
    L_0x0029:
        r0 = r4.mCameraState;
        if (r0 != 0) goto L_0x002e;
    L_0x002d:
        goto L_0x0085;
    L_0x002e:
        r0 = r4.mIsInIntentReviewUI;
        if (r0 == 0) goto L_0x0033;
    L_0x0032:
        return;
    L_0x0033:
        r0 = r4.isCameraFrontFacing();
        if (r0 == 0) goto L_0x0053;
    L_0x0039:
        r0 = r4.mAppController;
        r0 = r0.getHelpTipsManager();
        if (r0 == 0) goto L_0x004f;
    L_0x0041:
        r1 = r0.isHelpTipShowExist();
        if (r1 == 0) goto L_0x004f;
    L_0x0047:
        r1 = TAG;
        r2 = "helptip exists and cancels shutterbutton click";
        com.hmdglobal.app.camera.debug.Log.e(r1, r2);
        return;
    L_0x004f:
        r4.onShutterButtonClick();
        return;
    L_0x0053:
        r0 = r4.mEvoFlashLock;
        r1 = 0;
        if (r0 == 0) goto L_0x006a;
    L_0x0058:
        r0 = r4.mAppController;
        r0 = r0.getButtonManager();
        r2 = r4.mEvoFlashLock;
        r2 = r2.intValue();
        r0.enableButtonWithToken(r1, r2);
        r0 = 0;
        r4.mEvoFlashLock = r0;
    L_0x006a:
        r0 = r4.mUI;
        r2 = (float) r6;
        r3 = (float) r7;
        r0.initEvoSlider(r2, r3);
        r0 = r4.mFocusAreaSupported;
        if (r0 != 0) goto L_0x007a;
    L_0x0075:
        r0 = r4.mMeteringAreaSupported;
        if (r0 != 0) goto L_0x007a;
    L_0x0079:
        return;
    L_0x007a:
        r0 = r4.mFocusManager;
        r0.setAeAwbLock(r1);
        r0 = r4.mFocusManager;
        r0.onSingleTapUp(r6, r7);
        return;
    L_0x0085:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.MoreModule.onSingleTapUp(android.view.View, int, int):void");
    }

    public boolean onBackPressed() {
        return this.mUI.onBackPressed();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode != 23) {
            return false;
        }
        if (event.getRepeatCount() == 0) {
            onShutterButtonFocus(true);
        }
        return true;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode != 80) {
            HelpTipsManager helpTipsManager;
            if (keyCode != CameraUtil.BOOM_KEY) {
                switch (keyCode) {
                    case 24:
                    case 25:
                        break;
                    default:
                        return false;
                }
            }
            this.mAppController.setShutterPress(false);
            if (!(this.mActivity.getCameraAppUI().isInIntentReview() || this.mCameraKeyLongPressed)) {
                this.mVolumeButtonClickedFlag = true;
                onShutterButtonClick();
                helpTipsManager = this.mAppController.getHelpTipsManager();
                if (helpTipsManager != null && helpTipsManager.isHelpTipShowExist()) {
                    helpTipsManager.onBoomKeySingleShotResponse();
                }
            }
            if (this.mCameraKeyLongPressed) {
                onShutterButtonFocus(false);
                this.mCameraKeyLongPressed = false;
                helpTipsManager = this.mAppController.getHelpTipsManager();
                if (helpTipsManager != null && helpTipsManager.isHelpTipShowExist()) {
                    helpTipsManager.onBurstShotResponse();
                }
            }
            return true;
        }
        this.mAppController.setShutterPress(false);
        return true;
    }

    public boolean isCameraOpened() {
        return true;
    }

    private void closeCamera() {
        this.mSnapshotOnIdle = false;
        if (this.mEvoFlashLock != null) {
            this.mAppController.getButtonManager().enableButtonWithToken(0, this.mEvoFlashLock.intValue());
            this.mEvoFlashLock = null;
        }
        stopPreview();
        if (this.mCameraDevice != null) {
            this.mCameraDevice.setZoomChangeListener(null);
            this.mCameraDevice.setFaceDetectionCallback(null, null);
            this.mFaceDetectionStarted = false;
            this.mActivity.getCameraProvider().releaseCamera(this.mCameraDevice.getCameraId());
            this.mCameraDevice = null;
            setCameraState(0);
            this.mFocusManager.onCameraReleased();
        }
    }

    /* Access modifiers changed, original: protected */
    public void setupPreview() {
        Log.i(TAG, "setupPreview");
        this.mFocusManager.resetTouchFocus();
        if (this.mAppController.getCameraProvider().isBoostPreview()) {
            this.mActivity.clearBoost();
        }
        startPreview();
    }

    private void startPreview() {
        if (this.mCameraDevice == null) {
            Log.i(TAG, "attempted to start preview before camera device");
            return;
        }
        android.util.Log.w("AndCamAgntImp", "setup preview");
        setDisplayOrientation();
        if (!this.mSnapshotOnIdle) {
            if (this.mFocusManager.getFocusMode(this.mCameraSettings.getCurrentFocusMode()) == FocusMode.CONTINUOUS_PICTURE && this.mCameraState != 5) {
                this.mCameraDevice.cancelAutoFocus();
            }
            this.mFocusManager.setAeAwbLock(false);
        }
        firstFrame = false;
        updateParametersPictureSize();
        if (!this.mActivity.getCameraProvider().isBoostPreview()) {
            this.mCameraDevice.setPreviewTexture(this.mActivity.getCameraAppUI().getSurfaceTexture());
        }
        Log.i(TAG, "startPreview");
        CameraStartPreviewCallback startPreviewCallback = new CameraStartPreviewCallback() {
            public void onPreviewStarted() {
                MoreModule.this.mFocusManager.onPreviewStarted();
                MoreModule.this.onPreviewStarted();
                SessionStatsCollector.instance().previewActive(true);
                if (MoreModule.this.mSnapshotOnIdle) {
                    Log.v(MoreModule.TAG, "postSnapRunnable");
                    MoreModule.this.mHandler.post(MoreModule.this.mDoSnapRunnable);
                }
            }
        };
        if (GservicesHelper.useCamera2ApiThroughPortabilityLayer(this.mActivity)) {
            this.mCameraDevice.startPreview();
            startPreviewCallback.onPreviewStarted();
        } else if (this.mActivity.getCameraProvider().isBoostPreview()) {
            Log.w(TAG, "KPI boost start preview");
            this.mCameraDevice.waitPreviewWithCallback(new Handler(Looper.getMainLooper()), startPreviewCallback);
        } else {
            Log.w(TAG, "KPI normal start preview");
            this.mCameraDevice.startPreviewWithCallback(new Handler(Looper.getMainLooper()), startPreviewCallback);
        }
    }

    public void stopPreview() {
        if (!(this.mCameraDevice == null || this.mCameraState == 0)) {
            Log.i(TAG, "stopPreview");
            stopFaceDetection();
            this.mCameraDevice.stopPreview();
            this.mActivity.clearBoost();
        }
        setCameraState(0);
        if (this.mFocusManager != null) {
            this.mFocusManager.onPreviewStopped();
        }
        SessionStatsCollector.instance().previewActive(false);
    }

    public void onSettingChanged(SettingsManager settingsManager, String key) {
    }

    public boolean isZslOn() {
        return true;
    }

    public void setExposureCompensation(int value, boolean needCache) {
        Log.d(TAG, "setExposureCompensation 2657");
        int max = this.mCameraCapabilities.getMaxExposureCompensation();
        Tag tag;
        StringBuilder stringBuilder;
        if (value < this.mCameraCapabilities.getMinExposureCompensation() || value > max) {
            tag = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("invalid exposure range: ");
            stringBuilder.append(value);
            Log.w(tag, stringBuilder.toString());
            return;
        }
        tag = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("setExposureCompensation for ");
        stringBuilder.append(value);
        Log.w(tag, stringBuilder.toString());
        this.mCameraSettings.setExposureCompensationIndex(value);
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        if (needCache) {
            settingsManager.set(this.mAppController.getCameraScope(), Keys.KEY_EXPOSURE, value);
        }
    }

    private void setExposureCompensation(int value) {
        Log.d(TAG, "setExposureCompensation 2673");
        if (this.mCameraState == 5 || this.mCameraState == 6) {
            setExposureCompensation(this.mLockedEvoIndex, false);
        } else {
            setExposureCompensation(value, true);
        }
    }

    public boolean isCameraIdle() {
        if (this.mCameraState == 1 || this.mCameraState == 0 || this.mCameraState == 5 || this.mCameraState == 6) {
            return true;
        }
        if (this.mFocusManager == null || !this.mFocusManager.isFocusCompleted() || this.mCameraState == 4) {
            return false;
        }
        return true;
    }

    public boolean canCloseCamera() {
        return isCameraIdle() || this.mCameraState == 2;
    }

    public boolean isImageCaptureIntent() {
        String action = this.mActivity.getIntent().getAction();
        return "android.media.action.IMAGE_CAPTURE".equals(action) || CameraActivity.ACTION_IMAGE_CAPTURE_SECURE.equals(action);
    }

    private void initializeCapabilities() {
        this.mCameraCapabilities = this.mCameraDevice.getCapabilities();
        this.mFocusAreaSupported = this.mCameraCapabilities.supports(Feature.FOCUS_AREA);
        this.mMeteringAreaSupported = this.mCameraCapabilities.supports(Feature.METERING_AREA);
    }

    public void onZoomChanged(float ratio) {
    }

    public int getCameraState() {
        return this.mCameraState;
    }

    /* Access modifiers changed, original: protected */
    public boolean hideCamera() {
        return false;
    }

    /* Access modifiers changed, original: protected */
    public boolean hideCameraForced() {
        return false;
    }

    /* Access modifiers changed, original: protected */
    public boolean hideSetting() {
        return isImageCaptureIntent();
    }

    public void updateFaceBeautySetting(String key, int value) {
        saveFaceBeautySBValue(key, value);
    }

    private void saveFaceBeautySBValue(String key, int value) {
        this.mActivity.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, key, value);
    }

    public boolean isFacebeautyEnabled() {
        return false;
    }

    public boolean isAttentionSeekerShow() {
        return false;
    }

    public boolean isGesturePalmShow() {
        return false;
    }
}
