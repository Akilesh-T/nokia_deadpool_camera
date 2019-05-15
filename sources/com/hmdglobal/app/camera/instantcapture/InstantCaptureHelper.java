package com.hmdglobal.app.camera.instantcapture;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.media.CameraProfile;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.OrientationEventListener;
import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.debug.Log;
import com.android.ex.camera2.portability.debug.Log.Tag;
import com.android.external.ExtendKey;
import com.android.external.ExtendParameters;
import com.hmdglobal.app.camera.CameraActivity;
import com.hmdglobal.app.camera.Exif;
import com.hmdglobal.app.camera.PhotoModule.NamedImages;
import com.hmdglobal.app.camera.PhotoModule.NamedImages.NamedEntity;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.SecureCameraActivity;
import com.hmdglobal.app.camera.app.CameraServices;
import com.hmdglobal.app.camera.app.MediaSaver.OnMediaSavedListener;
import com.hmdglobal.app.camera.exif.ExifInterface;
import com.hmdglobal.app.camera.instantcapture.InstantViewImageActivity.OnUiUpdateListener;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.settings.SettingsUtil;
import com.hmdglobal.app.camera.util.BoostUtil;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.hmdglobal.app.camera.util.CustomFields;
import com.hmdglobal.app.camera.util.CustomUtil;
import com.hmdglobal.app.camera.util.ExternalExifInterface;
import com.hmdglobal.app.camera.util.PictureSizePerso;
import com.morphoinc.utils.multimedia.MediaProviderUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstantCaptureHelper {
    private static int BURST_MAX = 10;
    private static final int CAMERA_DIAPLAY_MODE_CONTINUE = 2;
    private static final int CAMERA_DIAPLAY_MODE_STOP = 1;
    private static final int CAMERA_HAL_API_VERSION_1_0 = 256;
    private static final int CAMERA_ID = 0;
    private static final String CAMERA_STOP_DIAPLAY_MODE = "stop-display";
    private static final String INSTANT_AEC = "instant-aec";
    private static final String INSTANT_CAPTURE = "instant-capture";
    private static final String INSTANT_CAPTURE_OFF = "0";
    private static final String INSTANT_CAPTURE_ON = "1";
    private static final String SNAPSHOT_BURST_NUM = "snapshot-burst-num";
    private static final Tag TAG = new Tag("InstantHelper");
    public static boolean USE_JPEG_AS_PICTURE_DISLAY = false;
    private static final String ZSL = "zsl";
    private static final String ZSL_ON = "on";
    private static InstantCaptureHelper instantCaptureHelper;
    public boolean gFirstFrame = false;
    private int mBurstCount = 0;
    private WakeLock mCPUWakeLock;
    private Camera mCamera;
    private Object mCameraLock = new Object();
    private Method mCameraOpenMethod;
    private boolean mCanStartViewImageActivity = false;
    private Context mContext;
    private boolean mDeferUpdateDisplay;
    private Method mDisableSoundMethod;
    private int mDisplayRotation = 0;
    private boolean mFreezePreview;
    private boolean mInitialized = false;
    private int mJpegRotation = 0;
    private KeyguardManager mKeyguardManager;
    private OnUiUpdateListener mListener;
    private MediaActionSound mMediaActionSound;
    private NamedImages mNamedImages;
    private OnMediaSavedListener mOnMediaSavedListener = new OnMediaSavedListener() {
        public void onMediaSaved(Uri uri) {
            if (uri != null) {
                CameraUtil.broadcastNewPicture(InstantCaptureHelper.this.mContext, uri);
                InstantCaptureHelper.this.mUris.add(uri);
                Tag access$100 = InstantCaptureHelper.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onMediaSaved ");
                stringBuilder.append(uri);
                Log.i(access$100, stringBuilder.toString());
                if (((InstantCaptureHelper.this.mBurstCount > 0 && InstantCaptureHelper.this.mBurstCount == InstantCaptureHelper.this.mUris.size()) || InstantCaptureHelper.this.mBurstCount == 0) && InstantCaptureHelper.this.mListener != null) {
                    InstantCaptureHelper.this.mListener.onUiUpdated(InstantCaptureHelper.this.mBurstCount);
                }
            }
        }
    };
    private int mOrientation = 0;
    private MyOrientationListener mOrientationListener;
    private Parameters mParameters;
    private int mPicHeight;
    private int mPicWidth;
    private ArrayList<byte[]> mPictureDatas = new ArrayList();
    private PowerManager mPowerManager;
    private InstantCaptureService mService;
    private boolean mShutterSoundOn;
    private volatile SurfaceTexture mSurfaceTexture;
    private boolean mSurfacetextureAttachedToActivity = false;
    private ArrayList<Uri> mUris = new ArrayList();
    private InstantViewImageActivity mViewImageActivityInstance;

    class InstantPictureCallback implements PictureCallback {
        InstantPictureCallback() {
        }

        public void onPictureTaken(byte[] data, Camera camera) {
            InstantCaptureHelper.this.mService.getServiceHandler().removeMessages(7);
            InstantCaptureHelper.this.mService.getServiceHandler().removeMessages(6);
            if (!InstantCaptureHelper.this.isInCaptureProgress()) {
                Log.i(InstantCaptureHelper.TAG, "onPictureTaken, state invalid ");
            } else if (!InstantCaptureHelper.this.mService.checkCameraState(8) || InstantCaptureHelper.this.mBurstCount <= 0) {
                InstantCaptureHelper.this.mPictureDatas.add(data);
                if (!((!InstantCaptureHelper.USE_JPEG_AS_PICTURE_DISLAY && InstantCaptureHelper.this.mSurfacetextureAttachedToActivity && !InstantCaptureHelper.this.mDeferUpdateDisplay) || InstantCaptureHelper.this.mViewImageActivityInstance == null || InstantCaptureHelper.this.mViewImageActivityInstance.isFinishing())) {
                    Log.i(InstantCaptureHelper.TAG, "update picture");
                    InstantCaptureHelper.this.mViewImageActivityInstance.showResultImageView(false);
                }
                if (InstantCaptureHelper.this.mService.checkCameraState(6)) {
                    Log.i(InstantCaptureHelper.TAG, "instant capture kpi, onPictureTaken for single capture");
                    InstantCaptureHelper.this.mService.changeCameraState(10);
                    InstantCaptureHelper.this.saveFinalPhoto(data, null);
                    InstantCaptureHelper.this.mService.getMainHandler().sendEmptyMessage(1);
                } else if (InstantCaptureHelper.this.mService.checkCameraState(7) || InstantCaptureHelper.this.mService.checkCameraState(8)) {
                    if (InstantCaptureHelper.this.mShutterSoundOn && InstantCaptureHelper.this.mBurstCount != 0) {
                        InstantCaptureHelper.this.mMediaActionSound.play(0);
                        Log.i(InstantCaptureHelper.TAG, "Playing sound");
                    }
                    Tag access$100 = InstantCaptureHelper.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("instant capture kpi, onPictureTaken for burst capture ");
                    stringBuilder.append(InstantCaptureHelper.this.mBurstCount);
                    Log.i(access$100, stringBuilder.toString());
                    Map<String, Object> externalBundle = new HashMap();
                    externalBundle.put(ExternalExifInterface.BURST_SHOT_ID, Integer.valueOf(hashCode()));
                    externalBundle.put(ExternalExifInterface.BURST_SHOT_INDEX, Integer.valueOf(InstantCaptureHelper.this.mBurstCount = InstantCaptureHelper.this.mBurstCount + 1));
                    if (InstantCaptureHelper.this.mListener != null) {
                        InstantCaptureHelper.this.mListener.onUiUpdating(InstantCaptureHelper.this.mBurstCount);
                    }
                    InstantCaptureHelper.this.saveFinalPhoto(data, externalBundle);
                    if (InstantCaptureHelper.this.mService.checkCameraState(8) || InstantCaptureHelper.this.mBurstCount >= InstantCaptureHelper.BURST_MAX) {
                        InstantCaptureHelper.this.mService.changeCameraState(10);
                    } else {
                        InstantCaptureHelper.this.setJpegRotation();
                        InstantCaptureHelper.this.mService.getServiceHandler().sendEmptyMessageDelayed(6, 500);
                    }
                } else {
                    Log.i(InstantCaptureHelper.TAG, "invalid onPictureTaken, skip");
                }
            } else {
                InstantCaptureHelper.this.mService.changeCameraState(10);
                if (InstantCaptureHelper.this.mListener != null) {
                    InstantCaptureHelper.this.mListener.onUiUpdated(InstantCaptureHelper.this.mBurstCount);
                }
            }
        }
    }

    class InstantShutterCallback implements ShutterCallback {
        InstantShutterCallback() {
        }

        public void onShutter() {
            Tag access$100 = InstantCaptureHelper.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("instant capture kpi, onShutter ");
            stringBuilder.append(InstantCaptureHelper.this.mShutterSoundOn);
            Log.i(access$100, stringBuilder.toString());
            boolean z = false;
            if (InstantCaptureHelper.this.mShutterSoundOn) {
                InstantCaptureHelper.this.mMediaActionSound.play(0);
            }
            InstantCaptureHelper instantCaptureHelper = InstantCaptureHelper.this;
            if (!InstantCaptureHelper.USE_JPEG_AS_PICTURE_DISLAY && InstantCaptureHelper.this.isSingleShot()) {
                z = true;
            }
            instantCaptureHelper.updateInstantCaptureAndStopDisplay(true, z);
        }
    }

    class MyOrientationListener extends OrientationEventListener {
        public MyOrientationListener(Context context) {
            super(context);
        }

        public MyOrientationListener(Context context, int rate) {
            super(context, rate);
        }

        public void onOrientationChanged(int orientation) {
            if (orientation != -1) {
                InstantCaptureHelper.this.mOrientation = roundOrientation(orientation, InstantCaptureHelper.this.mOrientation);
            }
        }

        public int roundOrientation(int orientation, int orientationHistory) {
            boolean changeOrientation;
            if (orientationHistory == -1) {
                changeOrientation = true;
            } else {
                int dist = Math.abs(orientation - orientationHistory);
                if (Math.min(dist, 360 - dist) >= 50) {
                    changeOrientation = true;
                } else {
                    changeOrientation = false;
                }
            }
            if (changeOrientation) {
                return (90 * ((orientation + 45) / 90)) % 360;
            }
            return orientationHistory;
        }
    }

    private InstantCaptureHelper() {
    }

    public boolean isInitialized() {
        return this.mInitialized;
    }

    public boolean needLowBatteryCheck(Context context) {
        boolean needCheck = true;
        if (!(CustomUtil.getInstance(context).getBoolean(CustomFields.DEF_CAMERA_LOW_BATTERY_FEATURE_INDEPENDENT, false) || CameraUtil.isBatterySaverEnabled(context))) {
            needCheck = false;
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("needLowBatteryCheck ");
        stringBuilder.append(needCheck);
        Log.i(tag, stringBuilder.toString());
        return needCheck;
    }

    public static InstantCaptureHelper getInstance() {
        if (instantCaptureHelper == null) {
            instantCaptureHelper = new InstantCaptureHelper();
        }
        return instantCaptureHelper;
    }

    public void init(InstantCaptureService service) {
        this.mService = service;
        this.mContext = service.getApplicationContext();
        BURST_MAX = CustomUtil.getInstance(this.mContext).getInt(CustomFields.DEF_CAMERA_BURST_MAX, 10);
        USE_JPEG_AS_PICTURE_DISLAY = CustomUtil.getInstance(this.mContext).getBoolean(CustomFields.DEF_SHOW_JPEG_FOR_INSTANT_CAPTURE, true);
        this.mOrientationListener = new MyOrientationListener(this.mContext, 0);
        try {
            this.mCameraOpenMethod = Class.forName("android.hardware.Camera").getMethod("openLegacy", new Class[]{Integer.TYPE, Integer.TYPE});
            this.mDisableSoundMethod = Class.forName("android.hardware.Camera").getMethod("disableShutterSound", new Class[0]);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e2) {
            e2.printStackTrace();
        }
        this.mMediaActionSound = new MediaActionSound();
        this.mMediaActionSound.load(0);
        this.mPowerManager = (PowerManager) this.mContext.getSystemService("power");
        this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService("keyguard");
        Keys.setDefaults(((CameraServices) this.mContext).getSettingsManager(), this.mContext);
        this.mInitialized = true;
    }

    private void createSurfaceTexture() {
        this.mSurfaceTexture = new SurfaceTexture(0);
        this.mSurfaceTexture.detachFromGLContext();
    }

    public void openCamera() throws Exception {
        Log.i(TAG, "instant capture kpi, openCamera");
        this.mCamera = (Camera) this.mCameraOpenMethod.invoke(null, new Object[]{Integer.valueOf(0), Integer.valueOf(256)});
        this.mDisableSoundMethod.invoke(this.mCamera, new Object[0]);
        Log.i(TAG, "instant capture kpi, has open camera");
        this.mCamera.setPreviewTexture(this.mSurfaceTexture);
    }

    private void startPreview() {
        Log.i(TAG, "instant capture kpi, startPreview");
        this.mCamera.startPreview();
        Log.i(TAG, "instant capture kpi, has start preview");
    }

    public void start() throws Exception {
        Log.w(TAG, "instant capture kpi, Try start instantCaptureHelper");
        this.mBurstCount = 0;
        this.mUris.clear();
        this.mPictureDatas.clear();
        this.mSurfacetextureAttachedToActivity = false;
        this.mDisplayRotation = 0;
        this.mOrientation = 0;
        this.mCanStartViewImageActivity = false;
        this.mNamedImages = new NamedImages();
        this.mFreezePreview = false;
        this.mDeferUpdateDisplay = false;
        createSurfaceTexture();
        BoostUtil.getInstance().acquireCpuLock();
        acquireCpuWakeLock();
        this.mOrientationListener.enable();
        synchronized (this.mCameraLock) {
            openCamera();
            if (this.mViewImageActivityInstance != null) {
                this.mViewImageActivityInstance.setSurfaceTexture();
            }
            setCameraParameters();
            startPreview();
            this.gFirstFrame = true;
        }
    }

    public SurfaceTexture getSurfaceTexture() {
        return this.mSurfaceTexture;
    }

    public void setSurfaceTextureAttached() {
        this.mSurfacetextureAttachedToActivity = true;
    }

    private void setCameraParameters() {
        Log.i(TAG, "setCameraParameters");
        if (this.mCamera == null) {
            Log.i(TAG, "Camera is null.");
            return;
        }
        this.mParameters = this.mCamera.getParameters();
        getCameraParametersFromSetting();
        this.mParameters.set(SNAPSHOT_BURST_NUM, BURST_MAX);
        this.mParameters.set(ZSL, ZSL_ON);
        this.mParameters.setRotation(90);
        this.mParameters.setFocusMode("infinity");
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setFocusMode for ");
        stringBuilder.append(this.mParameters.getFocusMode());
        Log.w(tag, stringBuilder.toString());
        this.mParameters.setFlashMode(this.mContext.getString(R.string.pref_camera_flashmode_off));
        this.mParameters.set(INSTANT_CAPTURE, "1");
        this.mParameters.set(ExtendKey.VISIDON_MODE, ExtendKey.FLIP_MODE_OFF);
        ExtendParameters extParams = ExtendParameters.getInstance(this.mParameters);
        this.mCamera.setParameters(this.mParameters);
        setDisplayOrientation();
    }

    private void getCameraParametersFromSetting() {
        SettingsManager settingsManager = ((CameraServices) this.mContext).getSettingsManager();
        this.mShutterSoundOn = Keys.isShutterSoundOn(settingsManager);
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("enable sound:");
        stringBuilder.append(this.mShutterSoundOn);
        Log.e(tag, stringBuilder.toString());
        String pictureSizeKey = Keys.KEY_PICTURE_SIZE_BACK;
        String pictureSize = settingsManager.getString(SettingsManager.SCOPE_GLOBAL, pictureSizeKey, null);
        if (pictureSize == null) {
            Log.i(TAG, "pictureSize null, perso init");
            PictureSizePerso perso = PictureSizePerso.getInstance();
            List<Size> supportedPictureSizes = this.mParameters.getSupportedPictureSizes();
            List<com.android.ex.camera2.portability.Size> supportedSizes = new ArrayList();
            if (supportedPictureSizes != null) {
                for (Size s : supportedPictureSizes) {
                    supportedSizes.add(new com.android.ex.camera2.portability.Size(s.width, s.height));
                }
            }
            perso.init(this.mContext, supportedSizes, 0);
            pictureSize = settingsManager.getString(SettingsManager.SCOPE_GLOBAL, pictureSizeKey, null);
        }
        Tag tag2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("pictureSize from settingsmanager:");
        stringBuilder2.append(pictureSize);
        Log.i(tag2, stringBuilder2.toString());
        if (pictureSize != null) {
            com.android.ex.camera2.portability.Size size = SettingsUtil.sizeFromString(pictureSize);
            this.mPicWidth = size.width();
            this.mPicHeight = size.height();
        } else {
            Size a = (Size) this.mParameters.getSupportedPictureSizes().get(0);
            this.mPicWidth = a.width;
            this.mPicHeight = a.height;
        }
        tag2 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("final PictureSize = ");
        stringBuilder2.append(this.mPicWidth);
        stringBuilder2.append(com.android.ex.camera2.portability.Size.DELIMITER);
        stringBuilder2.append(this.mPicHeight);
        Log.i(tag2, stringBuilder2.toString());
        this.mParameters.setPictureSize(this.mPicWidth, this.mPicHeight);
        int jpegQuality = CameraProfile.getJpegEncodingQualityParameter(0, 2);
        tag2 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("jpegQuality: ");
        stringBuilder2.append(jpegQuality);
        Log.i(tag2, stringBuilder2.toString());
        this.mParameters.setJpegQuality(jpegQuality);
    }

    public void capture() {
        if (this.mCamera != null) {
            Log.i(TAG, "instant capture kpi, capture");
            this.mNamedImages.nameNewImage(System.currentTimeMillis());
            synchronized (this.mCameraLock) {
                setJpegRotation();
                this.mCamera.takePicture(new InstantShutterCallback(), null, new InstantPictureCallback());
            }
            this.mService.getServiceHandler().sendEmptyMessageDelayed(7, CameraAgent.CAMERA_OPERATION_TIMEOUT_MS);
        }
    }

    public void changeDisplayOrientation(int orientation) {
        if (!USE_JPEG_AS_PICTURE_DISLAY && this.mDisplayRotation != orientation) {
            this.mDisplayRotation = orientation;
            this.mService.getServiceHandler().sendEmptyMessage(4);
        }
    }

    public void setDisplayOrientation() {
        if (isCaptureDone() && this.mViewImageActivityInstance != null) {
            this.mViewImageActivityInstance.showResultImageView(true);
        } else if (this.mFreezePreview) {
            Log.i(TAG, "setDisplayOrientation after stop display,no need");
            this.mDeferUpdateDisplay = true;
        } else if (!USE_JPEG_AS_PICTURE_DISLAY && this.mCamera != null) {
            int rotation = 0;
            switch (this.mDisplayRotation) {
                case 0:
                    rotation = 90;
                    break;
                case 1:
                    rotation = 0;
                    break;
                case 2:
                    rotation = MediaProviderUtils.ROTATION_270;
                    break;
                case 3:
                    rotation = MediaProviderUtils.ROTATION_180;
                    break;
            }
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("change display orientation to ");
            stringBuilder.append(rotation);
            Log.i(tag, stringBuilder.toString());
            synchronized (this.mCameraLock) {
                try {
                    if (this.mCamera != null) {
                        this.mCamera.setDisplayOrientation(rotation);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "changeDisplayOrientation fail ", e);
                }
            }
        }
    }

    public void setJpegRotation() {
        synchronized (this.mCameraLock) {
            try {
                if (this.mCamera == null) {
                    return;
                }
                this.mParameters = this.mCamera.getParameters();
                int tmp = this.mOrientation + 90;
                if (tmp >= 360) {
                    tmp -= 360;
                }
                this.mParameters.setRotation(tmp);
                this.mJpegRotation = tmp;
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("picture rotation = ");
                stringBuilder.append(tmp);
                Log.i(tag, stringBuilder.toString());
                this.mCamera.setParameters(this.mParameters);
            } catch (Exception e) {
                Log.e(TAG, "setJpegRotation fail ", e);
            }
        }
    }

    public void stop() {
        Log.i(TAG, "instant capture kpi, stop");
        this.mService.getMainHandler().removeMessages(6);
        this.mService.getServiceHandler().removeMessages(7);
        this.mOrientationListener.disable();
        this.mNamedImages = null;
        if (this.mCamera == null) {
            if (!(this.mSurfacetextureAttachedToActivity || this.mSurfaceTexture == null)) {
                this.mSurfaceTexture.release();
            }
            this.mSurfaceTexture = null;
            BoostUtil.getInstance().releaseCpuLock();
            releaseCpuWakeLock();
            return;
        }
        synchronized (this.mCameraLock) {
            CameraLock instance;
            try {
                this.mParameters = this.mCamera.getParameters();
                this.mParameters.set(CAMERA_STOP_DIAPLAY_MODE, 1);
                this.mParameters.set(SNAPSHOT_BURST_NUM, 0);
                this.mCamera.setParameters(this.mParameters);
                this.mCamera.stopPreview();
                this.mCamera.release();
                this.mCamera = null;
                instance = CameraLock.getInstance();
            } catch (Exception e) {
                try {
                    if (this.mCamera != null) {
                        Log.i(TAG, "Release camera since mCamera is not null.");
                        try {
                            this.mCamera.release();
                        } catch (Exception e2) {
                            Log.e(TAG, "Fail when calling Camera.release().", e);
                        }
                        this.mCamera = null;
                    }
                    instance = CameraLock.getInstance();
                } catch (Throwable th) {
                    CameraLock.getInstance().open();
                }
            }
            instance.open();
        }
        if (!(this.mSurfacetextureAttachedToActivity || this.mSurfaceTexture == null)) {
            this.mSurfaceTexture.release();
        }
        this.mSurfaceTexture = null;
        BoostUtil.getInstance().releaseCpuLock();
        releaseCpuWakeLock();
        Log.i(TAG, "stop end");
    }

    public void destroy() {
        this.mMediaActionSound.release();
        this.mInitialized = false;
        this.mService = null;
    }

    public boolean isSingleShot() {
        return this.mService.isSingleShot();
    }

    public boolean isCaptureDone() {
        return this.mService.isCaptureDone();
    }

    public boolean hasSaveDone() {
        return isCaptureDone() && getPictureDatas().size() > 0 && getPictureDatas().size() == getPictureUris().size();
    }

    public void updateInstantCaptureAndStopDisplay(boolean cancelInstantCapture, boolean stopDisplay) {
        Log.i(TAG, "updateInstantCaptureAndStopDisplay");
        synchronized (this.mCameraLock) {
            try {
                if (this.mCamera == null) {
                    return;
                }
                this.mParameters = this.mCamera.getParameters();
                if (cancelInstantCapture) {
                    this.mParameters.set(INSTANT_CAPTURE, "0");
                }
                if (stopDisplay) {
                    this.mFreezePreview = true;
                    this.mParameters.set(CAMERA_STOP_DIAPLAY_MODE, 1);
                }
                this.mCamera.setParameters(this.mParameters);
            } catch (Exception e) {
            }
        }
    }

    public int getBurstCount() {
        return this.mBurstCount;
    }

    public ArrayList<Uri> getPictureUris() {
        return this.mUris;
    }

    public ArrayList<byte[]> getPictureDatas() {
        return this.mPictureDatas;
    }

    public boolean isScreenOn() {
        return this.mPowerManager.isScreenOn();
    }

    public void wakeUpScreen() {
        Log.i(TAG, "wakeUpScreen before");
        if (!isScreenOn()) {
            Log.i(TAG, "wakeUpScreen");
            WakeLock screenWakeLock = this.mPowerManager.newWakeLock(268435482, "screenwakelock");
            screenWakeLock.acquire(500);
            screenWakeLock.setReferenceCounted(false);
        }
    }

    public void acquireCpuWakeLock() {
        if (this.mCPUWakeLock == null) {
            this.mCPUWakeLock = this.mPowerManager.newWakeLock(1, "cpuwakelock");
            this.mCPUWakeLock.acquire();
            this.mCPUWakeLock.setReferenceCounted(false);
        }
    }

    public void releaseCpuWakeLock() {
        if (this.mCPUWakeLock != null) {
            this.mCPUWakeLock.release();
            this.mCPUWakeLock = null;
        }
    }

    public void registerOnUiUpdateListener(OnUiUpdateListener listener, InstantViewImageActivity instance) {
        this.mListener = listener;
        this.mViewImageActivityInstance = instance;
    }

    public void unRegisterOnUiUpdateListener() {
        this.mListener = null;
        this.mViewImageActivityInstance = null;
    }

    public boolean isInCaptureProgress() {
        return this.mService.checkInCaptureProgress();
    }

    /* Access modifiers changed, original: 0000 */
    public void saveFinalPhoto(byte[] jpegData, Map<String, Object> externalInfos) {
        ExifInterface exif = Exif.getExif(jpegData);
        NamedEntity name = this.mNamedImages.getNextNameEntity();
        if (externalInfos != null) {
            String externalJson = CameraUtil.serializeToJson(externalInfos);
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("saving burst shot info:");
            stringBuilder.append(externalJson);
            Log.i(tag, stringBuilder.toString());
            exif.setTag(exif.buildTag(ExifInterface.TAG_USER_COMMENT, externalJson));
        }
        int orientation = Exif.getOrientation(exif);
        String title = name == null ? null : name.title;
        long date = name == null ? -1 : name.date;
        int width = this.mPicWidth;
        int height = this.mPicHeight;
        if ((this.mJpegRotation + orientation) % MediaProviderUtils.ROTATION_180 != 0) {
            width = this.mPicHeight;
            height = this.mPicWidth;
        }
        ((CameraServices) this.mContext).getMediaSaver().addImage(jpegData, title, date, null, width, height, orientation, exif, this.mOnMediaSavedListener, this.mContext.getContentResolver());
    }

    public void setForbidStartViewImageActivity(boolean forbidStartViewImageActivity) {
        this.mCanStartViewImageActivity = forbidStartViewImageActivity;
    }

    public boolean getForbidStartViewImageActivity() {
        return this.mCanStartViewImageActivity;
    }

    public void startCameraActivity(Context context, ArrayList<Uri> uris) {
        Intent intent;
        this.mService.getMainHandler().removeMessages(2);
        this.mService.getMainHandler().sendEmptyMessageDelayed(2, 400);
        if (this.mKeyguardManager.isKeyguardSecure() && this.mKeyguardManager.isKeyguardLocked()) {
            intent = new Intent(CameraActivity.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE);
            intent.setClass(context, SecureCameraActivity.class);
            if (uris != null && uris.size() > 0) {
                intent.putParcelableArrayListExtra("uris", uris);
            }
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("start SecureCameraActivity ");
            stringBuilder.append(uris);
            Log.i(tag, stringBuilder.toString());
        } else {
            dismissKeyguard();
            intent = new Intent("android.media.action.STILL_IMAGE_CAMERA");
            intent.setClass(context, CameraActivity.class);
            intent.addFlags(67108864);
            Log.i(TAG, "start CameraActivity");
        }
        intent.addFlags(268435456);
        context.startActivity(intent);
    }

    public void dismissKeyguard() {
        if (!this.mKeyguardManager.isKeyguardSecure() && this.mKeyguardManager.isKeyguardLocked()) {
            try {
                Object oRemoteService = Class.forName("android.os.ServiceManager").getMethod("getService", new Class[]{String.class}).invoke(null, new Object[]{"window"});
                Object oIWindowManager = Class.forName("android.view.IWindowManager$Stub").getMethod("asInterface", new Class[]{IBinder.class}).invoke(null, new Object[]{oRemoteService});
                oIWindowManager.getClass().getMethod("dismissKeyguard", new Class[0]).invoke(oIWindowManager, new Object[0]);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e2) {
                e2.printStackTrace();
            } catch (InvocationTargetException e3) {
                e3.printStackTrace();
            } catch (IllegalAccessException e4) {
                e4.printStackTrace();
            }
        }
    }

    public void startViewImageActivity(Context context) {
        Log.i(TAG, "startViewImageActivity");
        Intent cameraIntent = new Intent();
        cameraIntent.setClass(context, InstantViewImageActivity.class);
        cameraIntent.addFlags(268517376);
        context.startActivity(cameraIntent);
    }

    public void dismissViewImageActivity() {
        Log.i(TAG, "dismissViewImageActivity");
        if (this.mViewImageActivityInstance != null) {
            this.mViewImageActivityInstance.finish();
        }
    }
}
