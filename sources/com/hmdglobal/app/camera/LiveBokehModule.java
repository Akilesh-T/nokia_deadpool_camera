package com.hmdglobal.app.camera;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Builder;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.location.Location;
import android.media.AudioManager;
import android.media.CameraProfile;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue.IdleHandler;
import android.os.SystemClock;
import android.support.v4.media.MediaPlayer2;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.CameraAgent.CameraAFCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraAFMoveCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraFinalPreviewCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraGDCallBack;
import com.android.ex.camera2.portability.CameraAgent.CameraPictureCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraPreviewDataCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraPreviewResultCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.android.ex.camera2.portability.CameraAgent.CameraShutterCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraStartPreviewCallback;
import com.android.ex.camera2.portability.CameraAgent.CaptureCompleteCallBack;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraCapabilities.Feature;
import com.android.ex.camera2.portability.CameraCapabilities.FlashMode;
import com.android.ex.camera2.portability.CameraCapabilities.FocusMode;
import com.android.ex.camera2.portability.CameraCapabilities.SceneMode;
import com.android.ex.camera2.portability.CameraCapabilities.Stringifier;
import com.android.ex.camera2.portability.CameraDeviceInfo.Characteristics;
import com.android.ex.camera2.portability.CameraSettings;
import com.android.ex.camera2.portability.Size;
import com.android.external.ExtendKey;
import com.hmdglobal.app.camera.ButtonManager.ButtonCallback;
import com.hmdglobal.app.camera.FocusOverlayManager.Listener;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.app.CameraAppUI;
import com.hmdglobal.app.camera.app.CameraAppUI.BottomBarUISpec;
import com.hmdglobal.app.camera.app.CameraAppUI.BottomBarUISpec.ExposureCompensationSetCallback;
import com.hmdglobal.app.camera.app.MediaSaver.OnMediaSavedListener;
import com.hmdglobal.app.camera.app.MemoryManager.MemoryListener;
import com.hmdglobal.app.camera.app.MotionManager;
import com.hmdglobal.app.camera.beauty.cameragl.CameraRender.onPreviewBytes;
import com.hmdglobal.app.camera.beauty.cameragl.CameraSurfaceView;
import com.hmdglobal.app.camera.beauty.util.ImageUtils;
import com.hmdglobal.app.camera.beauty.util.Util;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.exif.ExifInterface;
import com.hmdglobal.app.camera.exif.ExifTag;
import com.hmdglobal.app.camera.exif.Rational;
import com.hmdglobal.app.camera.gdepthfilter.GDepth;
import com.hmdglobal.app.camera.gdepthfilter.GDepth.DepthMap;
import com.hmdglobal.app.camera.gdepthfilter.GImage;
import com.hmdglobal.app.camera.hardware.HardwareSpec;
import com.hmdglobal.app.camera.hardware.HardwareSpecImpl;
import com.hmdglobal.app.camera.module.ModuleController;
import com.hmdglobal.app.camera.motion.MotionPictureHelper;
import com.hmdglobal.app.camera.mpo.MpoInterface;
import com.hmdglobal.app.camera.remote.RemoteCameraModule;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.ResolutionUtil;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.settings.SettingsManager.OnSettingChangedListener;
import com.hmdglobal.app.camera.settings.SettingsUtil;
import com.hmdglobal.app.camera.specialtype.ProcessingMediaManager;
import com.hmdglobal.app.camera.specialtype.ProcessingMediaManager.ProcessingMedia;
import com.hmdglobal.app.camera.specialtype.utils.ProviderUtils;
import com.hmdglobal.app.camera.ui.CountDownView.OnCountDownStatusListener;
import com.hmdglobal.app.camera.ui.TouchCoordinate;
import com.hmdglobal.app.camera.util.ApiHelper;
import com.hmdglobal.app.camera.util.BeautifyHandler;
import com.hmdglobal.app.camera.util.BlurUtil;
import com.hmdglobal.app.camera.util.BoostUtil;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.hmdglobal.app.camera.util.CustomFields;
import com.hmdglobal.app.camera.util.CustomUtil;
import com.hmdglobal.app.camera.util.DepthUtil;
import com.hmdglobal.app.camera.util.ExternalExifInterface;
import com.hmdglobal.app.camera.util.GcamHelper;
import com.hmdglobal.app.camera.util.GservicesHelper;
import com.hmdglobal.app.camera.util.SessionStatsCollector;
import com.hmdglobal.app.camera.util.ToastUtil;
import com.hmdglobal.app.camera.util.UsageStatistics;
import com.hmdglobal.app.camera.util.XmpUtil;
import com.hmdglobal.app.camera.widget.AspectRatioSelector.AspectRatio;
import com.megvii.beautify.jni.BeaurifyJniSdk;
import com.morphoinc.app.panoramagp3.CameraConstants;
import com.morphoinc.utils.multimedia.MediaProviderUtils;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;
import java.util.concurrent.Semaphore;

public class LiveBokehModule extends CameraModule implements LiveBokehController, ModuleController, MemoryListener, Listener, SensorEventListener, OnSettingChangedListener, RemoteCameraModule, OnCountDownStatusListener {
    public static final int BAYER_ID = 0;
    public static final int BAYER_MODE = 1;
    private static final int BURST_DELAY = 0;
    protected static final int BURST_MAX = 99;
    protected static final int BURST_STOP_DELAY = 150;
    private static final String DEBUG_IMAGE_PREFIX = "DEBUG_";
    private static final int DEFAULT_GESTURE_SHOT_COUNT_DURATION = 3;
    public static final int DUAL_MODE = 0;
    private static final String EXTRA_QUICK_CAPTURE = "android.intent.extra.quickCapture";
    public static int FRONT_ID = 0;
    private static final String GESTURE_HANDLER_NAME = "gesture_handler";
    private static final int MAX_NUM_CAM = 4;
    private static final int MIN_CAMERA_LAUNCHING_TIMES = 3;
    public static int MONO_ID = -1;
    public static final int MONO_MODE = 2;
    private static final int MSG_CAPTURE_BURST = 3;
    private static final int MSG_FIRST_TIME_INIT = 1;
    private static final int MSG_GDEPTH_ON_TOAST = 6;
    private static final int MSG_HIDE_GESTURE = 4;
    private static final int MSG_SET_CAMERA_PARAMETERS_WHEN_IDLE = 2;
    private static final int MSG_UPDATE_FACE_BEAUTY = 5;
    private static final int OPEN_CAMERA = 0;
    public static final String PHOTO_MODULE_STRING_ID = "LiveBokehModule";
    private static final int REQUEST_CROP = 1000;
    private static final int SHUTTER_DELAY_LOW = 80;
    private static final int SHUTTER_DELAY_UP = 100;
    private static final int SHUTTER_PROGRESS_ACCELERATE_THRESHOLD = 30;
    private static final int SHUTTER_PROGRESS_FAKE_END = 90;
    private static final int SHUTTER_PROGRESS_HIDE_DELAY = 80;
    private static final int SHUTTER_PROGRESS_INIT = 0;
    private static final int SHUTTER_PROGRESS_MAX = 100;
    private static final int SHUTTER_PROGRESS_STEP = 5;
    protected static final int SHUTTER_STOP_DELAY = 500;
    public static final int SKIN_SMOOTHING_DEFAULT = 50;
    public static final int SKIN_SMOOTHING_MAX = 90;
    public static final int SKIN_SMOOTHING_RANGE = 100;
    public static int SWITCH_ID = -1;
    public static final int SWITCH_MODE = 3;
    private static final Tag TAG = new Tag(PHOTO_MODULE_STRING_ID);
    protected static final int UPDATE_PARAM_ALL = -1;
    protected static final int UPDATE_PARAM_INITIALIZE = 1;
    protected static final int UPDATE_PARAM_PREFERENCE = 4;
    protected static final int UPDATE_PARAM_VISIDON = 8;
    protected static final int UPDATE_PARAM_ZOOM = 2;
    public static final String action = "LiveBokehModule.broadcast.action";
    public static boolean firstFrame = true;
    private static final String sTempCropFilename = "crop-temp";
    private CameraDevice[] CameraDevices = new CameraDevice[4];
    private String[] CameraIds = new String[4];
    private OnSeekBarChangeListener bolkenListener;
    int curOritation;
    private boolean isOptimeizeSnapshot = false;
    private boolean isOptimizeCapture = true;
    private final boolean isOptimizeSwitchCamera = CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_ENABLE_OPTIMIZE_SWITCH, false);
    protected CameraActivity mActivity;
    private boolean mAeLockSupported;
    protected AppController mAppController;
    private AudioManager mAudioManager;
    private final AutoFocusCallback mAutoFocusCallback = new AutoFocusCallback(this, null);
    protected final Object mAutoFocusMoveCallback;
    public long mAutoFocusTime;
    private boolean mAwbLockSupported;
    private boolean mBokehEnabled = true;
    private int mBokehLevel = 4;
    private Builder mBokehRequestBuilder;
    private int mBurstNumForOneSingleBurst;
    protected LinkedList<TotalCaptureResult> mBurstResultQueue = new LinkedList();
    protected final BurstShotCheckQueue mBurstShotCheckQueue;
    private boolean mBurstShotNotifyHelpTip;
    private final ButtonCallback mCameraCallback;
    protected CameraCapabilities mCameraCapabilities;
    protected CameraProxy mCameraDevice;
    private CameraFinalPreviewCallback mCameraFinalPreviewCallback;
    private Handler mCameraHandler;
    private int mCameraId;
    private boolean mCameraKeyLongPressed = false;
    private final Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private boolean mCameraPreviewParamsReady;
    protected CameraSettings mCameraSettings;
    protected int mCameraState = 0;
    private HandlerThread mCameraThread;
    private final OnClickListener mCancelCallback;
    public long mCaptureStartTime;
    private ContentResolver mContentResolver;
    private boolean mContinuousFocusSupported;
    private int mControlAFMode = 4;
    private SoundPlayer mCountdownSoundPlayer;
    private Rect[] mCropRegion;
    private String mCropValue;
    private Uri mDebugUri;
    private int mDisplayOrientation;
    protected int mDisplayRotation;
    private final Runnable mDoSnapRunnable = new Runnable() {
        public void run() {
            LiveBokehModule.this.onShutterButtonClick();
        }
    };
    private final OnClickListener mDoneCallback;
    private Integer mEvoFlashLock;
    private boolean mFaceDetectionStarted = false;
    private ArrayList<RectF> mFaces;
    private boolean mFirstTimeInitialized;
    private String mFlashModeBeforeSceneMode;
    private boolean mFocusAreaSupported;
    protected FocusOverlayManager mFocusManager;
    private long mFocusStartTime;
    private final float[] mGData;
    private final int mGcamModeIndex;
    private Handler mGestureHandler;
    private GestureHandlerThread mGesturehandlerThread;
    protected final Handler mHandler;
    private final ButtonCallback mHdrPlusCallback;
    private int mHeading;
    private boolean mIsBurstShotSupport = false;
    private boolean mIsCameraOpened;
    private boolean mIsGlMode;
    private boolean mIsImageCaptureIntent;
    private boolean mIsInIntentReviewUI;
    public long mJpegCallbackFinishTime;
    private byte[] mJpegImageData;
    private long mJpegPictureCallbackTime;
    private int mJpegRotation;
    private final float[] mLData;
    protected String mLastISOValue;
    private boolean mLastMaskEnable;
    private Location mLocation;
    private int mLockedEvoIndex;
    protected LongshotPictureCallback mLongshotPictureTakenCallback;
    private ShutterCallback mLongshotShutterCallback;
    private final ButtonCallback mLowlightCallback;
    private final float[] mMData;
    private MainHandlerCallback mMainHandlerCallback;
    private boolean mMeteringAreaSupported;
    private boolean mMirror;
    private MotionManager mMotionManager;
    protected NamedImages mNamedImages;
    protected final OnMediaSavedListener mOnMediaSavedListener;
    private long mOnResumeTime;
    private ProgressDialog mOptimisingPhotoDialog;
    protected int mOrientation = 0;
    private Rect[] mOriginalCropRegion;
    protected boolean mPaused;
    protected int mPendingSwitchCameraId = -1;
    public long mPictureDisplayedToJpegCallbackTime;
    private PictureTaskListener mPictureTaken;
    private final PostViewPictureCallback mPostViewPictureCallback = new PostViewPictureCallback(this, null);
    private long mPostViewPictureCallbackTime;
    private onPreviewBytes mPreviewBytesBack;
    private Builder[] mPreviewRequestBuilder = new Builder[4];
    private TotalCaptureResult mPreviewResult;
    private ProgressDialog mProgressDialog;
    private final OnKeyListener mProgressDialogKeyListener;
    private ProgressUpdateRunnable mProgressUpdateRunnable;
    private boolean mQuickCapture;
    private final float[] mR;
    private final RawPictureCallback mRawPictureCallback = new RawPictureCallback(this, null);
    private long mRawPictureCallbackTime;
    protected int mReceivedBurstNum;
    private final OnClickListener mRetakeCallback;
    private Uri mSaveUri;
    private SceneMode mSceneMode;
    private SensorManager mSensorManager;
    private int mSensorOrientation;
    private final SettingsManager mSettingsManager;
    private boolean mShouldResizeTo16x9;
    private long mShutterCallbackTime;
    public long mShutterLag;
    public long mShutterToPictureDisplayedTime;
    private TouchCoordinate mShutterTouchCoordinate;
    private boolean mSnapshotOnIdle = false;
    protected SoundPlayer mSoundPlayer;
    private int mStreamId;
    private final float mSuperZoomThreshold = 1.5f;
    private int mTimerDuration;
    private LiveBokehUI mUI;
    private boolean mUnderLowMemory;
    private int mUpdateSet;
    protected boolean mVolumeButtonClickedFlag = false;
    private float mZoomValue;
    protected boolean mforceISOManual;
    private Builder previewRequestBuilder;
    private OnSeekBarChangeListener seekListener;
    private ContinueShot snapShot;

    public interface AspectRatioDialogCallback {
        AspectRatio getCurrentAspectRatio();

        void onAspectRatioSelected(AspectRatio aspectRatio, Runnable runnable);
    }

    protected final class BurstShotCheckQueue {
        private static final int INVALID_VALUE = -1;
        private int mCapacity = 99;
        private SparseArray<Integer> mJobQueue = new SparseArray();
        private SparseArray<Integer> mResultQueue = new SparseArray();
        private Runnable mSupposePictureTakenAction = null;

        protected BurstShotCheckQueue() {
        }

        public void pushToJobQueue(int index) {
            this.mJobQueue.put(index, Integer.valueOf(index));
        }

        public void popToResultQueue(int index) {
            int value = ((Integer) this.mJobQueue.get(index, Integer.valueOf(-1))).intValue();
            if (value != -1) {
                this.mJobQueue.remove(index);
                this.mResultQueue.put(index, Integer.valueOf(value));
            }
        }

        public boolean setCapacity(int capcacity) {
            this.mCapacity = capcacity;
            if (this.mResultQueue.size() != this.mCapacity) {
                return true;
            }
            if (this.mSupposePictureTakenAction != null) {
                this.mSupposePictureTakenAction.run();
                this.mSupposePictureTakenAction = null;
            }
            this.mCapacity = 99;
            this.mJobQueue.clear();
            this.mResultQueue.clear();
            return false;
        }

        public void setPictureTakenActionCache(Runnable runnable) {
            this.mSupposePictureTakenAction = runnable;
        }

        public void clearCheckQueue() {
            this.mCapacity = 99;
            this.mSupposePictureTakenAction = null;
            this.mJobQueue.clear();
            this.mResultQueue.clear();
        }

        public int getCapacity() {
            return this.mCapacity;
        }
    }

    private static class GestureHandlerThread extends HandlerThread {
        public GestureHandlerThread(String name) {
            super(name);
        }

        public void run() {
            try {
                super.run();
            } catch (Exception e) {
                Log.e(LiveBokehModule.TAG, "Gesture engine encounter a fatal error , ignore it");
            }
        }
    }

    public interface LocationDialogCallback {
        void onLocationTaggingSelected(boolean z);
    }

    private class MainHandler extends Handler {
        private final WeakReference<LiveBokehModule> mModule;

        public MainHandler(LiveBokehModule module) {
            super(Looper.getMainLooper());
            this.mModule = new WeakReference(module);
        }

        public void handleMessage(Message msg) {
            LiveBokehModule module = (LiveBokehModule) this.mModule.get();
            if (module != null) {
                switch (msg.what) {
                    case 1:
                        module.initializeFirstTime();
                        break;
                    case 2:
                        module.setCameraParametersWhenIdle(0);
                        break;
                    case 3:
                        if (LiveBokehModule.this.mReceivedBurstNum < 99) {
                            LiveBokehModule.this.mCameraDevice.burstShot(LiveBokehModule.this.mHandler, LiveBokehModule.this.mLongshotShutterCallback, LiveBokehModule.this.mRawPictureCallback, LiveBokehModule.this.mPostViewPictureCallback, LiveBokehModule.this.mLongshotPictureTakenCallback);
                            LiveBokehModule.this.mHandler.sendEmptyMessageDelayed(3, 300);
                            break;
                        }
                        break;
                    case 4:
                        LiveBokehModule.this.mUI.hideGesture();
                        break;
                    case 5:
                        if (!(!LiveBokehModule.this.isCameraFrontFacing() || LiveBokehModule.this.mPaused || LiveBokehModule.this.mCameraSettings == null)) {
                            Log.e(LiveBokehModule.TAG, "update facebeauty");
                            SettingsManager settingsManager = LiveBokehModule.this.mActivity.getSettingsManager();
                            int skinSmoothing = LiveBokehModule.this.mActivity.getSettingsManager().getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_FACEBEAUTY_SKIN_SMOOTHING, Integer.valueOf(50)).intValue();
                            LiveBokehModule.this.mCameraSettings.setFaceBeauty(Keys.isFacebeautyOn(settingsManager), (skinSmoothing * 90) / 100);
                            LiveBokehModule.this.updateFaceBeautySetting(Keys.KEY_FACEBEAUTY_SKIN_SMOOTHING, skinSmoothing);
                            break;
                        }
                    case 6:
                        if (LiveBokehModule.this.mCameraId != 1) {
                            ToastUtil.showHigherToast(LiveBokehModule.this.mActivity, (int) R.string.gdepth_on, 0);
                            break;
                        }
                        break;
                    default:
                        if (LiveBokehModule.this.mMainHandlerCallback != null) {
                            LiveBokehModule.this.mMainHandlerCallback.handleMessageEx(msg);
                            break;
                        }
                        break;
                }
            }
        }
    }

    protected interface MainHandlerCallback {
        void handleMessageEx(Message message);
    }

    private class MyCameraHandler extends Handler {
        public MyCameraHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            int id = msg.arg1;
            if (msg.what == 0) {
                LiveBokehModule.this.mAppController.getCameraProvider().requestCamera(id, GservicesHelper.useCamera2ApiThroughPortabilityLayer(LiveBokehModule.this.mAppController.getAndroidContext()));
            }
        }
    }

    public static class NamedImages {
        private final Vector<NamedEntity> mQueue = new Vector();

        public static class NamedEntity {
            public long date;
            public String title;
        }

        public void nameNewImage(long date) {
            synchronized (this.mQueue) {
                NamedEntity r = new NamedEntity();
                r.title = CameraUtil.createJpegName(date);
                r.date = date;
                this.mQueue.add(r);
            }
        }

        public NamedEntity getNextNameEntity() {
            synchronized (this.mQueue) {
                if (this.mQueue.isEmpty()) {
                    NamedEntity r = new NamedEntity();
                    long date = System.currentTimeMillis();
                    r.title = CameraUtil.createJpegName(date);
                    r.date = date;
                    return r;
                }
                NamedEntity namedEntity = (NamedEntity) this.mQueue.remove(0);
                return namedEntity;
            }
        }

        public NamedEntity getNextNameEntityForThumbnailOnly() {
            NamedEntity r;
            synchronized (this.mQueue) {
                if (this.mQueue.isEmpty()) {
                    r = new NamedEntity();
                    long date = System.currentTimeMillis();
                    r.title = CameraUtil.createJpegName(date);
                    r.date = date;
                    this.mQueue.add(r);
                }
                r = (NamedEntity) this.mQueue.get(0);
            }
            return r;
        }
    }

    public class PictureTaskListener implements Runnable {
        public Bitmap modifiedBytes;
        public byte[] originalBytes;

        public void run() {
            Size s = LiveBokehModule.this.mCameraSettings.getCurrentPhotoSize();
            ExifInterface exif = Exif.getExif(this.originalBytes);
            NamedEntity name = LiveBokehModule.this.mNamedImages.getNextNameEntity();
            Map<String, Object> externalInfo = LiveBokehModule.this.buildExternalBundle();
            if (externalInfo != null) {
                exif.setTag(exif.buildTag(ExifInterface.TAG_USER_COMMENT, CameraUtil.serializeToJson(externalInfo)));
            }
            final String title = name == null ? null : name.title;
            new Thread(new Runnable() {
                public void run() {
                    String path = Storage.generateFilepath(title);
                    Uri uri = ImageUtils.saveImageToGallery(LiveBokehModule.this.mActivity, PictureTaskListener.this.modifiedBytes, path, LiveBokehModule.this.mLocation, LiveBokehModule.this.mDisplayRotation, 0);
                    if (uri != null) {
                        LiveBokehModule.this.mActivity.notifyNewMedia(uri);
                    }
                }
            }).start();
            LiveBokehModule.this.mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    LiveBokehModule.this.mAppController.setShutterEnabled(true);
                    LiveBokehModule.this.setCameraState(1);
                    LiveBokehModule.this.mActivity.updateStorageSpaceAndHint(null);
                }
            });
        }
    }

    private class ProgressUpdateRunnable implements Runnable {
        private int mProgress;

        public ProgressUpdateRunnable(int progress) {
            this.mProgress = progress;
        }

        public ProgressUpdateRunnable setProgress(int progress) {
            this.mProgress = progress;
            return this;
        }

        public void run() {
            int delay = 80;
            if (this.mProgress > 30) {
                delay = 100;
            }
            if (this.mProgress < 90) {
                LiveBokehModule.this.mHandler.postDelayed(setProgress(this.mProgress + 5), (long) delay);
            }
        }
    }

    private static class ResizeBundle {
        ExifInterface exif;
        byte[] jpegData;
        float targetAspectRatio;

        private ResizeBundle() {
        }

        /* synthetic */ ResizeBundle(AnonymousClass1 x0) {
            this();
        }
    }

    private static class TE {
        public Bitmap bitmap;
        public long date;

        public TE(long date, Bitmap b) {
            this.date = date;
            this.bitmap = b;
        }
    }

    private final class AutoFocusCallback implements CameraAFCallback {
        private AutoFocusCallback() {
        }

        /* synthetic */ AutoFocusCallback(LiveBokehModule x0, AnonymousClass1 x1) {
            this();
        }

        public void onAutoFocus(boolean focused, CameraProxy camera) {
            SessionStatsCollector.instance().autofocusResult(focused);
            if (!LiveBokehModule.this.mPaused && !LiveBokehModule.this.isInBurstshot()) {
                LiveBokehModule.this.mAutoFocusTime = System.currentTimeMillis() - LiveBokehModule.this.mFocusStartTime;
                Tag access$500 = LiveBokehModule.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mAutoFocusTime = ");
                stringBuilder.append(LiveBokehModule.this.mAutoFocusTime);
                stringBuilder.append("ms   focused = ");
                stringBuilder.append(focused);
                Log.v(access$500, stringBuilder.toString());
                if (!LiveBokehModule.this.needEnableExposureAdjustment() || !focused || LiveBokehModule.this.mFocusManager == null || LiveBokehModule.this.mFocusManager.getFocusAreas() == null) {
                    Log.v(LiveBokehModule.TAG, "focus failed , set camera state back to IDLE");
                    LiveBokehModule.this.setCameraState(1);
                } else {
                    Log.v(LiveBokehModule.TAG, "focus succeed , show exposure slider");
                    if (LiveBokehModule.this.mCameraState == 2) {
                        LiveBokehModule.this.mUI.showEvoSlider();
                    }
                    LiveBokehModule.this.setCameraState(6);
                }
                int action = FocusOverlayManager.ACTION_RESTORE_CAF_LATER;
                if (LiveBokehModule.this.needEnableExposureAdjustment()) {
                    action |= FocusOverlayManager.ACTION_KEEP_FOCUS_FRAME;
                }
                LiveBokehModule.this.mFocusManager.onAutoFocus(focused, action);
            }
        }
    }

    @TargetApi(16)
    private final class AutoFocusMoveCallback implements CameraAFMoveCallback {
        private AutoFocusMoveCallback() {
        }

        /* synthetic */ AutoFocusMoveCallback(LiveBokehModule x0, AnonymousClass1 x1) {
            this();
        }

        public void onAutoFocusMoving(boolean moving, CameraProxy camera) {
            if (LiveBokehModule.this.mCameraState != 4) {
                LiveBokehModule.this.mUI.clearEvoPendingUI();
                if (LiveBokehModule.this.mEvoFlashLock != null) {
                    LiveBokehModule.this.mAppController.getButtonManager().enableButtonWithToken(0, LiveBokehModule.this.mEvoFlashLock.intValue());
                    LiveBokehModule.this.mEvoFlashLock = null;
                }
                LiveBokehModule.this.mFocusManager.onAutoFocusMoving(moving);
                SessionStatsCollector.instance().autofocusMoving(moving);
            }
        }
    }

    private final class BurstShotSaveListener implements OnMediaSavedListener {
        private int mCount = 0;

        public BurstShotSaveListener(int currentCount) {
            this.mCount = currentCount;
            LiveBokehModule.this.mBurstShotCheckQueue.pushToJobQueue(this.mCount);
        }

        public void onMediaSaved(final Uri uri) {
            Tag access$500 = LiveBokehModule.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Burst Waiting for ");
            stringBuilder.append(LiveBokehModule.this.mBurstShotCheckQueue.getCapacity());
            stringBuilder.append(" current count is ");
            stringBuilder.append(this.mCount);
            Log.w(access$500, stringBuilder.toString());
            LiveBokehModule.this.mBurstShotCheckQueue.popToResultQueue(this.mCount);
            if (this.mCount < LiveBokehModule.this.mBurstShotCheckQueue.getCapacity()) {
                access$500 = LiveBokehModule.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("burst image saved without updating thumbnail, uri=");
                stringBuilder.append(uri.toString());
                Log.v(access$500, stringBuilder.toString());
                LiveBokehModule.this.mActivity.notifyNewMedia(uri, 0);
                LiveBokehModule.this.mBurstShotCheckQueue.setPictureTakenActionCache(new Runnable() {
                    public void run() {
                        LiveBokehModule.this.mActivity.notifyNewMedia(uri, LiveBokehModule.this.getBurstShotMediaSaveAction());
                    }
                });
                return;
            }
            if (uri != null) {
                access$500 = LiveBokehModule.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("update thumbnail with burst image, uri=");
                stringBuilder.append(uri.toString());
                Log.v(access$500, stringBuilder.toString());
                LiveBokehModule.this.mActivity.notifyNewMedia(uri, LiveBokehModule.this.getBurstShotMediaSaveAction());
            }
            LiveBokehModule.this.dismissSavingHint();
        }
    }

    private final class BurstShutterCallback implements CameraShutterCallback {
        private BurstShutterCallback() {
        }

        /* synthetic */ BurstShutterCallback(LiveBokehModule x0, AnonymousClass1 x1) {
            this();
        }

        public void onShutter(CameraProxy camera) {
            Log.v(LiveBokehModule.TAG, "burst onShutterCallback");
            if (!LiveBokehModule.this.mPaused && LiveBokehModule.this.mCameraState != 0) {
                if (LiveBokehModule.this.mCameraState == 8 || LiveBokehModule.this.mCameraState == 9) {
                    LiveBokehModule.this.mShutterCallbackTime = System.currentTimeMillis();
                    LiveBokehModule.this.mNamedImages.nameNewImage(LiveBokehModule.this.mShutterCallbackTime);
                    LiveBokehModule.this.mShutterLag = LiveBokehModule.this.mShutterCallbackTime - LiveBokehModule.this.mCaptureStartTime;
                    Tag access$500 = LiveBokehModule.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("burst mShutterLag = ");
                    stringBuilder.append(LiveBokehModule.this.mShutterLag);
                    stringBuilder.append("ms");
                    Log.v(access$500, stringBuilder.toString());
                    LiveBokehModule.this.mCameraDevice.takePictureWithoutWaiting(LiveBokehModule.this.mHandler, this, LiveBokehModule.this.mRawPictureCallback, LiveBokehModule.this.mPostViewPictureCallback, LiveBokehModule.this.mLongshotPictureTakenCallback);
                    return;
                }
                Log.w(LiveBokehModule.TAG, "stop burst in shutter callback");
            }
        }
    }

    private final class CaptureComplete implements CaptureCompleteCallBack {
        private CaptureComplete() {
        }

        /* synthetic */ CaptureComplete(LiveBokehModule x0, AnonymousClass1 x1) {
            this();
        }

        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            Log.d(LiveBokehModule.TAG, "onCaptureCompleted");
            LiveBokehModule.this.mBurstResultQueue.add(result);
        }
    }

    private final class JpegPictureCallback implements CameraPictureCallback {
        Location mLocation;

        public JpegPictureCallback(Location loc) {
            this.mLocation = loc;
        }

        public void onPictureTaken(byte[] originalJpegData, final CameraProxy camera) {
            Tag access$500 = LiveBokehModule.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onPictureTaken, camera state is ");
            stringBuilder.append(LiveBokehModule.this.mCameraState);
            Log.w(access$500, stringBuilder.toString());
            LiveBokehModule.this.mAppController.setShutterEnabled(true);
            LiveBokehModule.this.mCameraSettings.setRTDofMode(2);
            if (!LiveBokehModule.this.mPaused) {
                HelpTipsManager helpTipsManager = LiveBokehModule.this.mAppController.getHelpTipsManager();
                if (helpTipsManager != null) {
                    helpTipsManager.onRecentTipResponse();
                }
                LiveBokehModule.this.dismissOptimisingPhotoHint();
                if (LiveBokehModule.this.mIsImageCaptureIntent) {
                    LiveBokehModule.this.stopPreview();
                }
                if (LiveBokehModule.this.mSceneMode == SceneMode.HDR) {
                    LiveBokehModule.this.mUI.setSwipingEnabled(true);
                }
                LiveBokehModule.this.mJpegPictureCallbackTime = System.currentTimeMillis();
                if (LiveBokehModule.this.mPostViewPictureCallbackTime != 0) {
                    LiveBokehModule.this.mShutterToPictureDisplayedTime = LiveBokehModule.this.mPostViewPictureCallbackTime - LiveBokehModule.this.mShutterCallbackTime;
                    LiveBokehModule.this.mPictureDisplayedToJpegCallbackTime = LiveBokehModule.this.mJpegPictureCallbackTime - LiveBokehModule.this.mPostViewPictureCallbackTime;
                } else {
                    LiveBokehModule.this.mShutterToPictureDisplayedTime = LiveBokehModule.this.mRawPictureCallbackTime - LiveBokehModule.this.mShutterCallbackTime;
                    LiveBokehModule.this.mPictureDisplayedToJpegCallbackTime = LiveBokehModule.this.mJpegPictureCallbackTime - LiveBokehModule.this.mRawPictureCallbackTime;
                }
                Tag access$5002 = LiveBokehModule.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("mPictureDisplayedToJpegCallbackTime = ");
                stringBuilder2.append(LiveBokehModule.this.mPictureDisplayedToJpegCallbackTime);
                stringBuilder2.append("ms");
                Log.v(access$5002, stringBuilder2.toString());
                boolean needRestart = false;
                if (!(LiveBokehModule.this.mCameraSettings == null || LiveBokehModule.this.mCameraSettings.isZslOn)) {
                    needRestart = true;
                }
                if (needRestart) {
                    LiveBokehModule.this.setCameraState(0);
                    if (!LiveBokehModule.this.mIsImageCaptureIntent) {
                        LiveBokehModule.this.mUI.clearEvoPendingUI();
                        if (LiveBokehModule.this.mEvoFlashLock != null) {
                            LiveBokehModule.this.mAppController.getButtonManager().enableButtonWithToken(0, LiveBokehModule.this.mEvoFlashLock.intValue());
                            LiveBokehModule.this.mEvoFlashLock = null;
                        }
                        LiveBokehModule.this.mUI.clearFocus();
                        LiveBokehModule.this.mFocusManager.resetTouchFocus();
                        LiveBokehModule.this.setupPreview();
                    }
                } else {
                    if (LiveBokehModule.this.mCameraState == 7) {
                        LiveBokehModule.this.setCameraState(5);
                        LiveBokehModule.this.mAppController.getLockEventListener().onIdle();
                        LiveBokehModule.this.mAppController.getCameraAppUI().showModeOptions();
                    } else if (!LiveBokehModule.this.mIsImageCaptureIntent) {
                        LiveBokehModule.this.setCameraState(1);
                    }
                    if (LiveBokehModule.this.mCameraState != 5) {
                        LiveBokehModule.this.mFocusManager.cancelAutoFocus();
                    }
                    LiveBokehModule.this.mHandler.removeMessages(4);
                    LiveBokehModule.this.mUI.hideGesture();
                    if (!(LiveBokehModule.this.mCameraDevice == null || LiveBokehModule.this.mIsImageCaptureIntent || !LiveBokehModule.this.isEnableGestureRecognization())) {
                        if (LiveBokehModule.this.mGesturehandlerThread == null || !LiveBokehModule.this.mGesturehandlerThread.isAlive()) {
                            Log.w(LiveBokehModule.TAG, "GestureCore open looper , tray start thread");
                            LiveBokehModule.this.mGesturehandlerThread = new GestureHandlerThread(LiveBokehModule.GESTURE_HANDLER_NAME);
                            LiveBokehModule.this.mGesturehandlerThread.start();
                            LiveBokehModule.this.mGestureHandler = new Handler(LiveBokehModule.this.mGesturehandlerThread.getLooper());
                        }
                        LiveBokehModule.this.mCameraDevice.startPreview();
                    }
                }
                long now = System.currentTimeMillis();
                LiveBokehModule.this.mJpegCallbackFinishTime = now - LiveBokehModule.this.mJpegPictureCallbackTime;
                LiveBokehModule.this.mJpegPictureCallbackTime = 0;
                Size size = LiveBokehModule.this.mCameraSettings.getCurrentPhotoSize();
                LiveBokehModule.this.setPreviewCallback();
                byte[] mJpegData = originalJpegData;
                ExifInterface exif = Exif.getExif(mJpegData);
                final NamedEntity name = LiveBokehModule.this.mNamedImages.getNextNameEntity();
                if (LiveBokehModule.this.mShouldResizeTo16x9) {
                    ResizeBundle dataBundle = new ResizeBundle();
                    dataBundle.jpegData = mJpegData;
                    dataBundle.targetAspectRatio = 1.7777778f;
                    dataBundle.exif = exif;
                    new AsyncTask<ResizeBundle, Void, ResizeBundle>() {
                        /* Access modifiers changed, original: protected|varargs */
                        public ResizeBundle doInBackground(ResizeBundle... resizeBundles) {
                            return LiveBokehModule.this.cropJpegDataToAspectRatio(resizeBundles[0]);
                        }

                        /* Access modifiers changed, original: protected */
                        public void onPostExecute(ResizeBundle result) {
                            JpegPictureCallback.this.saveFinalPhoto(result.jpegData, name, result.exif, camera);
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new ResizeBundle[]{dataBundle});
                } else {
                    saveFinalPhoto(mJpegData, name, exif, camera);
                }
                if (LiveBokehModule.this.isOptimizeCapture && exif.hasThumbnail()) {
                    LiveBokehModule.this.updateThumbnail(exif);
                }
            }
        }

        /* Access modifiers changed, original: protected */
        public byte[] addExifTags(byte[] jpeg, TotalCaptureResult result) {
            ExifInterface exif = new ExifInterface();
            exif.addMakeAndModelTag();
            exif.addDateTimeStampTag(ExifInterface.TAG_DATE_TIME_ORIGINAL, System.currentTimeMillis(), TimeZone.getDefault());
            if (result != null) {
                if (result.get(CaptureResult.FLASH_MODE) != null) {
                    exif.addFlashTag(((Integer) result.get(CaptureResult.FLASH_MODE)).intValue() != 0);
                }
                if (result.get(CaptureResult.LENS_FOCAL_LENGTH) != null) {
                    exif.addFocalLength(new Rational((long) ((int) (((Float) result.get(CaptureResult.LENS_FOCAL_LENGTH)).floatValue() * 100.0f)), 100));
                }
                if (result.get(CaptureResult.CONTROL_AWB_MODE) != null) {
                    exif.addWhiteBalanceMode(((Integer) result.get(CaptureResult.CONTROL_AWB_MODE)).intValue());
                }
                if (result.get(CaptureResult.LENS_APERTURE) != null) {
                    exif.addAperture(new Rational((long) ((int) (((Float) result.get(CaptureResult.LENS_APERTURE)).floatValue() * 100.0f)), 100));
                    exif.addFNumber(new Rational((long) ((int) (((Float) result.get(CaptureResult.LENS_APERTURE)).floatValue() * 100.0f)), 100));
                }
                if (result.get(CaptureResult.SENSOR_EXPOSURE_TIME) != null) {
                    exif.addExposureTime(new Rational(((Long) result.get(CaptureResult.SENSOR_EXPOSURE_TIME)).longValue() / 1000000, 1000));
                }
                if (result.get(CaptureResult.SENSOR_SENSITIVITY) != null) {
                    int senstivityBoost = 100;
                    Tag access$500 = LiveBokehModule.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("senstivityBoost = ");
                    stringBuilder.append(result.get(CaptureResult.CONTROL_POST_RAW_SENSITIVITY_BOOST));
                    Log.d(access$500, stringBuilder.toString());
                    if (result.get(CaptureResult.CONTROL_POST_RAW_SENSITIVITY_BOOST) != null) {
                        senstivityBoost = ((Integer) result.get(CaptureResult.CONTROL_POST_RAW_SENSITIVITY_BOOST)).intValue();
                    }
                    exif.addISO((((Integer) result.get(CaptureResult.SENSOR_SENSITIVITY)).intValue() * senstivityBoost) / 100);
                }
                if (result.get(CaptureResult.JPEG_ORIENTATION) != null) {
                    exif.addOrientationTag(((Integer) result.get(CaptureResult.JPEG_ORIENTATION)).intValue());
                }
                Tag access$5002 = LiveBokehModule.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("JPEG_ORIENTATION = ");
                stringBuilder2.append(result.get(CaptureResult.JPEG_ORIENTATION));
                Log.d(access$5002, stringBuilder2.toString());
                access$5002 = LiveBokehModule.TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("JPEG_GPS_LOCATION = ");
                stringBuilder2.append(this.mLocation);
                Log.d(access$5002, stringBuilder2.toString());
                exif.addMeteringMode();
                exif.addExposureProgram();
                exif.addSoftware();
            }
            OutputStream jpegOut = new ByteArrayOutputStream();
            try {
                exif.writeExif(jpeg, jpegOut);
            } catch (IOException e) {
                Log.e(LiveBokehModule.TAG, "Could not write EXIF", e);
            }
            return jpegOut.toByteArray();
        }

        /* Access modifiers changed, original: protected */
        public byte[] convertN21ToJpeg(byte[] bytesN21, int w, int h) {
            byte[] rez = new byte[0];
            YuvImage yuvImage = new YuvImage(bytesN21, 17, w, h, null);
            Rect rect = new Rect(0, 0, w, h);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            boolean compressToJpeg = yuvImage.compressToJpeg(rect, true, outputStream);
            return outputStream.toByteArray();
        }

        /* Access modifiers changed, original: 0000 */
        public void saveFinalPhoto(byte[] jpegData, NamedEntity name, ExifInterface exif, CameraProxy camera) {
            saveFinalPhoto(jpegData, name, exif, camera, LiveBokehModule.this.buildExternalBundle());
        }

        public byte[] Bitmap2Bytes(Bitmap bm) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bm.compress(CompressFormat.JPEG, 100, baos);
            return baos.toByteArray();
        }

        /* Access modifiers changed, original: 0000 */
        public void saveFinalPhoto(byte[] jpegData, NamedEntity name, ExifInterface exif, CameraProxy camera, Map<String, Object> map) {
            byte[] jpegData2;
            byte[] bArr = jpegData;
            NamedEntity namedEntity = name;
            ExifInterface exifInterface = exif;
            final int orientation = Exif.getOrientation(exif);
            float zoomValue = 1.0f;
            if (LiveBokehModule.this.mCameraCapabilities.supports(Feature.ZOOM)) {
                zoomValue = LiveBokehModule.this.mCameraSettings.getCurrentZoomRatio();
            }
            float zoomValue2 = zoomValue;
            boolean hdrOn = SceneMode.HDR == LiveBokehModule.this.mSceneMode;
            String flashSetting = LiveBokehModule.this.mActivity.getSettingsManager().getString(LiveBokehModule.this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE);
            boolean gridLinesOn = Keys.areGridLinesOn(LiveBokehModule.this.mActivity.getSettingsManager());
            UsageStatistics instance = UsageStatistics.instance();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(namedEntity.title);
            stringBuilder.append(Storage.JPEG_POSTFIX);
            instance.photoCaptureDoneEvent(10000, stringBuilder.toString(), exifInterface, camera.getCharacteristics().isFacingFront(), hdrOn, zoomValue2, flashSetting, gridLinesOn, Float.valueOf((float) LiveBokehModule.this.mTimerDuration), LiveBokehModule.this.mShutterTouchCoordinate, Boolean.valueOf(LiveBokehModule.this.mVolumeButtonClickedFlag));
            LiveBokehModule.this.mShutterTouchCoordinate = null;
            LiveBokehModule.this.mVolumeButtonClickedFlag = false;
            int orientation2;
            if (LiveBokehModule.this.mIsImageCaptureIntent) {
                orientation2 = orientation;
                LiveBokehModule.this.mJpegImageData = bArr;
                LiveBokehModule.this.mUI.disableZoom();
                if (LiveBokehModule.this.mQuickCapture) {
                    LiveBokehModule.this.onCaptureDone();
                } else {
                    Log.v(LiveBokehModule.TAG, "showing UI");
                    LiveBokehModule.this.mUI.showCapturedImageForReview(bArr, orientation2, false);
                    LiveBokehModule.this.mIsInIntentReviewUI = true;
                }
                jpegData2 = bArr;
            } else {
                int width;
                int height;
                int width2;
                int i;
                Integer exifWidth = exifInterface.getTagIntValue(ExifInterface.TAG_PIXEL_X_DIMENSION);
                Integer exifHeight = exifInterface.getTagIntValue(ExifInterface.TAG_PIXEL_Y_DIMENSION);
                if (!LiveBokehModule.this.mShouldResizeTo16x9 || exifWidth == null || exifHeight == null) {
                    Size s = LiveBokehModule.this.mCameraSettings.getCurrentPhotoSize();
                    if ((LiveBokehModule.this.mJpegRotation + orientation) % MediaProviderUtils.ROTATION_180 == 0) {
                        width = s.width();
                        height = s.height();
                    } else {
                        width2 = s.height();
                        height = s.width();
                        width = width2;
                    }
                } else {
                    width = exifWidth.intValue();
                    height = exifHeight.intValue();
                }
                String title = namedEntity == null ? null : namedEntity.title;
                if (LiveBokehModule.this.mDebugUri != null) {
                    LiveBokehModule.this.saveToDebugUri(bArr);
                    if (title != null) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(LiveBokehModule.DEBUG_IMAGE_PREFIX);
                        stringBuilder2.append(title);
                        title = stringBuilder2.toString();
                    }
                }
                String title2 = title;
                if (title2 == null) {
                    Log.e(LiveBokehModule.TAG, "Unbalanced name/data pair");
                    jpegData2 = bArr;
                } else {
                    if (namedEntity.date == -1) {
                        namedEntity.date = LiveBokehModule.this.mCaptureStartTime;
                    }
                    if (LiveBokehModule.this.mHeading >= 0) {
                        ExifTag directionRefTag = exifInterface.buildTag(ExifInterface.TAG_GPS_IMG_DIRECTION_REF, "M");
                        ExifTag directionTag = exifInterface.buildTag(ExifInterface.TAG_GPS_IMG_DIRECTION, new Rational((long) LiveBokehModule.this.mHeading, 1));
                        exifInterface.setTag(directionRefTag);
                        exifInterface.setTag(directionTag);
                    }
                    if (camera.getCharacteristics().isFacingFront() && LiveBokehModule.this.isNeedMirrorSelfie()) {
                        bArr = LiveBokehModule.this.aftMirrorJpeg(bArr);
                    }
                    jpegData2 = bArr;
                    ArrayList<byte[]> bokehBytes = MpoInterface.generateXmpFromMpo(jpegData2);
                    final int i2;
                    if (!LiveBokehModule.this.mBokehEnabled || bokehBytes == null || bokehBytes.size() <= 2) {
                        if (LiveBokehModule.this.mIsGlMode && LiveBokehModule.this.mActivity.getCameraAppUI().getBeautySeek() > 0.0f && LiveBokehModule.this.mCameraId == 1) {
                            final String t = title2;
                            final long j = namedEntity.date;
                            final byte[] sourceData = jpegData2;
                            AnonymousClass2 anonymousClass2 = r0;
                            i2 = width;
                            final int i3 = height;
                            long date2 = j;
                            orientation2 = orientation;
                            exifInterface = exif;
                            AnonymousClass2 anonymousClass22 = new Runnable() {
                                public void run() {
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(sourceData, 0, sourceData.length);
                                    Bitmap bitmap2 = bitmap;
                                    Bitmap out = BeautifyHandler.processImageBitmap(LiveBokehModule.this.mActivity, bitmap2, i2, i3, LiveBokehModule.this.mActivity.getCameraAppUI().getBeautySeek() / 20.0f, MediaProviderUtils.ROTATION_270);
                                    final byte[] result = JpegPictureCallback.this.Bitmap2Bytes(out);
                                    if (out != null) {
                                        out.recycle();
                                    }
                                    if (bitmap != null) {
                                        bitmap.recycle();
                                    }
                                    LiveBokehModule.this.mActivity.runOnUiThread(new Runnable() {
                                        public void run() {
                                            LiveBokehModule.this.getServices().getMediaSaver().addImage(result, t, j, JpegPictureCallback.this.mLocation, i2, i3, orientation, exifInterface, LiveBokehModule.this.mOnMediaSavedListener, LiveBokehModule.this.mContentResolver);
                                        }
                                    });
                                }
                            };
                            new Thread(anonymousClass2).start();
                        } else {
                            orientation2 = orientation;
                            ArrayList<byte[]> arrayList = bokehBytes;
                            Integer num = exifHeight;
                            LiveBokehModule.this.getServices().getMediaSaver().addImage(jpegData2, title2, namedEntity.date, this.mLocation, width, height, orientation2, exif, LiveBokehModule.this.mOnMediaSavedListener, LiveBokehModule.this.mContentResolver);
                        }
                        i = orientation2;
                    } else {
                        int DEPTH_W;
                        int DEPTH_H;
                        GImage gImage = new GImage((byte[]) bokehBytes.get(1), "image/jpeg");
                        Size photoSize = LiveBokehModule.this.mCameraSettings.getCurrentPhotoSize();
                        i2 = photoSize.width();
                        width2 = photoSize.height();
                        if (((float) i2) / ((float) width2) == 1.3333334f) {
                            DEPTH_W = 896;
                            DEPTH_H = DepthUtil.DEPTH_HEIGHT_4_3;
                            Log.d(LiveBokehModule.TAG, "set width x height to 4:3 size by default");
                        } else if (((float) i2) / ((float) width2) == 1.7777778f) {
                            DEPTH_W = 896;
                            DEPTH_H = DepthUtil.DEPTH_HEIGHT_16_9;
                            Log.d(LiveBokehModule.TAG, "set width x height to 16:9 size");
                        } else {
                            DEPTH_W = 1000;
                            DEPTH_H = 500;
                            Log.d(LiveBokehModule.TAG, "set width x height to 18:9 size");
                        }
                        DepthMap depthMap = new DepthMap(DEPTH_W, DEPTH_H, MediaProviderUtils.ROTATION_180);
                        depthMap.buffer = (byte[]) bokehBytes.get(bokehBytes.size() - 1);
                        GDepth gDepth = GDepth.createGDepth(depthMap);
                        float depthNear = ((Float) LiveBokehModule.this.mPreviewResult.get(DepthUtil.bokeh_gdepth_near)).floatValue();
                        zoomValue = ((Float) LiveBokehModule.this.mPreviewResult.get(DepthUtil.bokeh_gdepth_far)).floatValue();
                        byte depthFormat = ((Byte) LiveBokehModule.this.mPreviewResult.get(DepthUtil.bokeh_gdepth_format)).byteValue();
                        gDepth.setNear(depthNear);
                        gDepth.setFar(zoomValue);
                        gDepth.setFormat(depthFormat);
                        Tag access$500 = LiveBokehModule.TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("westalgo depth_near: ");
                        stringBuilder3.append(depthNear);
                        stringBuilder3.append("depth_far: ");
                        stringBuilder3.append(zoomValue);
                        stringBuilder3.append("depth_format:");
                        stringBuilder3.append(depthFormat);
                        Log.d(access$500, stringBuilder3.toString());
                        exif.addMakeAndModelTag();
                        LiveBokehModule.this.getServices().getMediaSaver().addXmpImage((byte[]) bokehBytes.get(0), gImage, gDepth, title2, namedEntity.date, null, width, height, orientation, exifInterface, XmpUtil.BOKEH_TYPE, LiveBokehModule.this.mOnMediaSavedListener, LiveBokehModule.this.mContentResolver, "jpeg");
                    }
                }
                orientation2 = orientation;
                i = orientation2;
            }
            LiveBokehModule.this.getServices().getRemoteShutterListener().onPictureTaken(jpegData2);
            LiveBokehModule.this.mActivity.updateStorageSpaceAndHint(null);
        }
    }

    protected class LongshotPictureCallback implements CameraPictureCallback {
        Location mLocation;
        private short mLongshotCount = (short) 0;

        public LongshotPictureCallback(Location loc) {
            this.mLocation = loc;
        }

        public void onPictureTaken(byte[] originalJpegData, CameraProxy camera) {
            Log.w(LiveBokehModule.TAG, "OnPictureTaken in burst");
            if (!LiveBokehModule.this.mPaused) {
                if (LiveBokehModule.this.mCameraState != 8 && LiveBokehModule.this.mCameraState != 9) {
                    Log.w(LiveBokehModule.TAG, "stop burst in picture taken");
                    LiveBokehModule.this.stopBurst();
                } else if (LiveBokehModule.this.mActivity.getStorageSpaceBytes() <= Storage.LOW_STORAGE_THRESHOLD_BYTES) {
                    Tag access$500 = LiveBokehModule.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Not enough space or storage not ready. remaining=");
                    stringBuilder.append(LiveBokehModule.this.mActivity.getStorageSpaceBytes());
                    Log.i(access$500, stringBuilder.toString());
                    LiveBokehModule.this.mVolumeButtonClickedFlag = false;
                    LiveBokehModule.this.stopBurst();
                } else {
                    if (LiveBokehModule.this.mCameraState == 8) {
                        LiveBokehModule.this.setCameraState(9);
                    }
                    LiveBokehModule liveBokehModule = LiveBokehModule.this;
                    liveBokehModule.mReceivedBurstNum++;
                    Log.v(LiveBokehModule.TAG, "update burst count");
                    LiveBokehModule.this.mUI.updateBurstCount(LiveBokehModule.this.mReceivedBurstNum);
                    int mode = LiveBokehModule.this.mAudioManager.getRingerMode();
                    if (Keys.isShutterSoundOn(LiveBokehModule.this.mAppController.getSettingsManager()) && mode == 2 && LiveBokehModule.this.mSoundPlayer != null) {
                        LiveBokehModule.this.mStreamId = LiveBokehModule.this.mSoundPlayer.play(R.raw.camera_burst, 1.0f, 98);
                    }
                    Tag access$5002 = LiveBokehModule.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("burst receiveNum is ");
                    stringBuilder2.append(LiveBokehModule.this.mReceivedBurstNum);
                    Log.w(access$5002, stringBuilder2.toString());
                    updateExifAndSave(Exif.getExif(originalJpegData), originalJpegData, camera);
                    if (LiveBokehModule.this.mReceivedBurstNum >= 99) {
                        LiveBokehModule.this.setCameraState(10);
                        LiveBokehModule.this.mHandler.postDelayed(new Runnable() {
                            public void run() {
                                LiveBokehModule.this.stopBurst();
                            }
                        }, 150);
                    }
                }
            }
        }

        /* Access modifiers changed, original: protected */
        public void updateExifAndSave(ExifInterface exif, byte[] originalJpegData, final CameraProxy camera) {
            final NamedEntity name = LiveBokehModule.this.mNamedImages.getNextNameEntity();
            final Map externalBundle = new HashMap();
            externalBundle.put(ExternalExifInterface.BURST_SHOT_ID, Integer.valueOf(hashCode()));
            String str = ExternalExifInterface.BURST_SHOT_INDEX;
            short s = this.mLongshotCount;
            this.mLongshotCount = (short) (s + 1);
            externalBundle.put(str, Short.valueOf(s));
            Tag access$500 = LiveBokehModule.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("long shot taken for ");
            stringBuilder.append(this.mLongshotCount);
            Log.w(access$500, stringBuilder.toString());
            if (LiveBokehModule.this.mShouldResizeTo16x9) {
                ResizeBundle dataBundle = new ResizeBundle();
                dataBundle.jpegData = originalJpegData;
                dataBundle.targetAspectRatio = 1.7777778f;
                dataBundle.exif = exif;
                new AsyncTask<ResizeBundle, Void, ResizeBundle>() {
                    /* Access modifiers changed, original: protected|varargs */
                    public ResizeBundle doInBackground(ResizeBundle... resizeBundles) {
                        return LiveBokehModule.this.cropJpegDataToAspectRatio(resizeBundles[0]);
                    }

                    /* Access modifiers changed, original: protected */
                    public void onPostExecute(ResizeBundle result) {
                        LongshotPictureCallback.this.saveFinalPhoto(result.jpegData, name, result.exif, camera, externalBundle);
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new ResizeBundle[]{dataBundle});
                return;
            }
            saveFinalPhoto(originalJpegData, name, exif, camera, externalBundle);
        }

        /* Access modifiers changed, original: protected|final */
        public final void saveFinalPhoto(byte[] jpegData, NamedEntity name, ExifInterface exif, CameraProxy camera) {
            saveFinalPhoto(jpegData, name, exif, camera, null);
        }

        /* Access modifiers changed, original: protected|final */
        public final void saveFinalPhoto(byte[] jpegData, NamedEntity name, ExifInterface exif, CameraProxy camera, Map<String, Object> externalInfos) {
            byte[] bArr = jpegData;
            NamedEntity namedEntity = name;
            ExifInterface exifInterface = exif;
            int orientation = Exif.getOrientation(exif);
            float zoomValue = 1.0f;
            if (LiveBokehModule.this.mCameraCapabilities.supports(Feature.ZOOM)) {
                zoomValue = LiveBokehModule.this.mCameraSettings.getCurrentZoomRatio();
            }
            float zoomValue2 = zoomValue;
            boolean hdrOn = SceneMode.HDR == LiveBokehModule.this.mSceneMode;
            String flashSetting = LiveBokehModule.this.mActivity.getSettingsManager().getString(LiveBokehModule.this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE);
            boolean gridLinesOn = Keys.areGridLinesOn(LiveBokehModule.this.mActivity.getSettingsManager());
            UsageStatistics instance = UsageStatistics.instance();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(namedEntity.title);
            stringBuilder.append(Storage.JPEG_POSTFIX);
            instance.photoCaptureDoneEvent(10000, stringBuilder.toString(), exifInterface, camera.getCharacteristics().isFacingFront(), hdrOn, zoomValue2, flashSetting, gridLinesOn, Float.valueOf((float) LiveBokehModule.this.mTimerDuration), LiveBokehModule.this.mShutterTouchCoordinate, Boolean.valueOf(LiveBokehModule.this.mVolumeButtonClickedFlag));
            LiveBokehModule.this.mShutterTouchCoordinate = null;
            LiveBokehModule.this.mVolumeButtonClickedFlag = false;
            int orientation2;
            if (LiveBokehModule.this.mIsImageCaptureIntent) {
                orientation2 = orientation;
                LiveBokehModule.this.mJpegImageData = bArr;
                if (LiveBokehModule.this.mQuickCapture) {
                    LiveBokehModule.this.onCaptureDone();
                } else {
                    Log.v(LiveBokehModule.TAG, "showing UI");
                    LiveBokehModule.this.mUI.showCapturedImageForReview(bArr, orientation2, LiveBokehModule.this.mMirror);
                }
            } else {
                int width;
                int height;
                Integer exifWidth = exifInterface.getTagIntValue(ExifInterface.TAG_PIXEL_X_DIMENSION);
                Integer exifHeight = exifInterface.getTagIntValue(ExifInterface.TAG_PIXEL_Y_DIMENSION);
                if (!LiveBokehModule.this.mShouldResizeTo16x9 || exifWidth == null || exifHeight == null) {
                    Size s = LiveBokehModule.this.mCameraSettings.getCurrentPhotoSize();
                    if ((LiveBokehModule.this.mJpegRotation + orientation) % MediaProviderUtils.ROTATION_180 == 0) {
                        width = s.width();
                        height = s.height();
                    } else {
                        int width2 = s.height();
                        height = s.width();
                        width = width2;
                    }
                } else {
                    width = exifWidth.intValue();
                    height = exifHeight.intValue();
                }
                String title = namedEntity == null ? null : namedEntity.title;
                long date = namedEntity == null ? -1 : namedEntity.date;
                if (LiveBokehModule.this.mDebugUri != null) {
                    LiveBokehModule.this.saveToDebugUri(bArr);
                    if (title != null) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(LiveBokehModule.DEBUG_IMAGE_PREFIX);
                        stringBuilder2.append(title);
                        title = stringBuilder2.toString();
                    }
                }
                String title2 = title;
                if (title2 == null) {
                    Log.e(LiveBokehModule.TAG, "Unbalanced name/data pair");
                    orientation2 = orientation;
                } else {
                    long date2;
                    if (date == -1) {
                        date2 = LiveBokehModule.this.mCaptureStartTime;
                    } else {
                        date2 = date;
                    }
                    if (LiveBokehModule.this.mHeading >= 0) {
                        ExifTag directionRefTag = exifInterface.buildTag(ExifInterface.TAG_GPS_IMG_DIRECTION_REF, "M");
                        ExifTag directionTag = exifInterface.buildTag(ExifInterface.TAG_GPS_IMG_DIRECTION, new Rational((long) LiveBokehModule.this.mHeading, 1));
                        exifInterface.setTag(directionRefTag);
                        exifInterface.setTag(directionTag);
                    }
                    if (externalInfos != null) {
                        exifInterface.setTag(exifInterface.buildTag(ExifInterface.TAG_USER_COMMENT, CameraUtil.serializeToJson(externalInfos)));
                    }
                    LiveBokehModule.this.getServices().getMediaSaver().addImage(bArr, title2, date2, this.mLocation, width, height, orientation, exifInterface, new BurstShotSaveListener(this.mLongshotCount), LiveBokehModule.this.mContentResolver);
                }
            }
            LiveBokehModule.this.getServices().getRemoteShutterListener().onPictureTaken(bArr);
            LiveBokehModule.this.mActivity.updateStorageSpaceAndHint(null);
        }
    }

    private final class PostViewPictureCallback implements CameraPictureCallback {
        private PostViewPictureCallback() {
        }

        /* synthetic */ PostViewPictureCallback(LiveBokehModule x0, AnonymousClass1 x1) {
            this();
        }

        public void onPictureTaken(byte[] data, CameraProxy camera) {
            LiveBokehModule.this.mPostViewPictureCallbackTime = System.currentTimeMillis();
            Tag access$500 = LiveBokehModule.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mShutterToPostViewCallonbackTime = ");
            stringBuilder.append(LiveBokehModule.this.mPostViewPictureCallbackTime - LiveBokehModule.this.mShutterCallbackTime);
            stringBuilder.append("ms");
            Log.v(access$500, stringBuilder.toString());
        }
    }

    private final class RawPictureCallback implements CameraPictureCallback {
        private RawPictureCallback() {
        }

        /* synthetic */ RawPictureCallback(LiveBokehModule x0, AnonymousClass1 x1) {
            this();
        }

        public void onPictureTaken(byte[] rawData, CameraProxy camera) {
            Tag access$500 = LiveBokehModule.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("rawData size:");
            stringBuilder.append(rawData != null ? Integer.valueOf(rawData.length) : "0");
            Log.v(access$500, stringBuilder.toString());
            LiveBokehModule.this.mRawPictureCallbackTime = System.currentTimeMillis();
            access$500 = LiveBokehModule.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("mShutterToRawCallbackTime = ");
            stringBuilder.append(LiveBokehModule.this.mRawPictureCallbackTime - LiveBokehModule.this.mShutterCallbackTime);
            stringBuilder.append("ms");
            Log.v(access$500, stringBuilder.toString());
        }
    }

    private final class ShutterCallback implements CameraShutterCallback {
        private boolean isFromLongshot;
        private boolean mIsfirst;
        private final boolean mNeedsAnimation;

        public ShutterCallback(LiveBokehModule liveBokehModule, boolean needsAnimation, boolean fromLongshot) {
            this(needsAnimation);
            this.isFromLongshot = fromLongshot;
        }

        public ShutterCallback(boolean needsAnimation) {
            this.mIsfirst = true;
            this.isFromLongshot = false;
            this.mNeedsAnimation = needsAnimation;
        }

        public void onShutter(CameraProxy camera) {
            LiveBokehModule.this.mShutterCallbackTime = System.currentTimeMillis();
            if (this.isFromLongshot) {
                LiveBokehModule.this.mNamedImages.nameNewImage(LiveBokehModule.this.mShutterCallbackTime);
            }
            LiveBokehModule.this.mShutterLag = LiveBokehModule.this.mShutterCallbackTime - LiveBokehModule.this.mCaptureStartTime;
            Tag access$500 = LiveBokehModule.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mShutterLag = ");
            stringBuilder.append(LiveBokehModule.this.mShutterLag);
            stringBuilder.append("ms");
            Log.v(access$500, stringBuilder.toString());
            if (this.mNeedsAnimation && this.mIsfirst) {
                this.mIsfirst = false;
                LiveBokehModule.this.mActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        LiveBokehModule.this.animateAfterShutter();
                    }
                });
            }
            if (LiveBokehModule.this.isOptimizeCapture) {
                LiveBokehModule.this.mAppController.setShutterEnabled(true);
            }
        }
    }

    /* Access modifiers changed, original: protected */
    public int getBurstShotMediaSaveAction() {
        return 3;
    }

    private void checkDisplayRotation() {
        if (!this.mPaused) {
            if (CameraUtil.getDisplayRotation(this.mActivity) != this.mDisplayRotation) {
                setDisplayOrientation();
            }
            if (SystemClock.uptimeMillis() - this.mOnResumeTime < 5000) {
                this.mHandler.postDelayed(new Runnable() {
                    public void run() {
                        LiveBokehModule.this.checkDisplayRotation();
                    }
                }, 100);
            }
        }
    }

    public void setMainHandlerCallback(MainHandlerCallback l) {
        this.mMainHandlerCallback = l;
    }

    private void switchToGcamCapture() {
        if (this.mActivity != null && this.mGcamModeIndex != 0) {
            this.mActivity.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR_PLUS, true);
            ButtonManager buttonManager = this.mActivity.getButtonManager();
            buttonManager.disableButtonClick(4);
            this.mAppController.getCameraAppUI().freezeScreenUntilPreviewReady();
            this.mActivity.onModeSelected(this.mGcamModeIndex);
            buttonManager.enableButtonClick(4);
        }
    }

    public LiveBokehModule(AppController app) {
        Object autoFocusMoveCallback;
        super(app);
        if (ApiHelper.HAS_AUTO_FOCUS_MOVE_CALLBACK) {
            autoFocusMoveCallback = new AutoFocusMoveCallback(this, null);
        } else {
            autoFocusMoveCallback = null;
        }
        this.mAutoFocusMoveCallback = autoFocusMoveCallback;
        this.mReceivedBurstNum = 0;
        this.mBurstNumForOneSingleBurst = 0;
        this.mHandler = new MainHandler(this);
        this.mLData = new float[3];
        this.mGData = new float[3];
        this.mMData = new float[3];
        this.mR = new float[16];
        this.mHeading = -1;
        this.mIsInIntentReviewUI = false;
        this.mCameraPreviewParamsReady = false;
        this.mUnderLowMemory = false;
        this.mEvoFlashLock = null;
        this.mIsCameraOpened = false;
        this.mLastMaskEnable = false;
        this.mOnMediaSavedListener = new OnMediaSavedListener() {
            public void onMediaSaved(Uri uri) {
                if (uri != null) {
                    int notifyAction = 2;
                    if (LiveBokehModule.this.isOptimizeCapture) {
                        notifyAction = 2 | 4;
                    }
                    LiveBokehModule.this.mActivity.notifyNewMedia(uri, notifyAction);
                }
            }
        };
        this.mBurstShotCheckQueue = new BurstShotCheckQueue();
        this.mBurstShotNotifyHelpTip = false;
        this.mShouldResizeTo16x9 = false;
        this.mMainHandlerCallback = null;
        this.mCameraCallback = new ButtonCallback() {
            public void onStateChanged(int state) {
                if (!LiveBokehModule.this.mPaused && !LiveBokehModule.this.mAppController.getCameraProvider().waitingForCamera()) {
                    LiveBokehModule.this.mPendingSwitchCameraId = state;
                    Tag access$500 = LiveBokehModule.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Start to switch camera. cameraId=");
                    stringBuilder.append(state);
                    Log.d(access$500, stringBuilder.toString());
                    LiveBokehModule.this.switchCamera();
                }
            }
        };
        this.mHdrPlusCallback = new ButtonCallback() {
            public void onStateChanged(int state) {
                SettingsManager settingsManager = LiveBokehModule.this.mActivity.getSettingsManager();
                if (GcamHelper.hasGcamAsSeparateModule()) {
                    settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, Keys.KEY_SWITCH_CAMERA);
                    LiveBokehModule.this.switchToGcamCapture();
                    return;
                }
                if (Keys.isHdrOn(settingsManager)) {
                    if (LiveBokehModule.this.isHdrToastShow()) {
                        ToastUtil.showToast(LiveBokehModule.this.mActivity, LiveBokehModule.this.mActivity.getString(R.string.hdr_on_toast), 0);
                    }
                    settingsManager.set(LiveBokehModule.this.mAppController.getCameraScope(), Keys.KEY_SCENE_MODE, LiveBokehModule.this.mCameraCapabilities.getStringifier().stringify(SceneMode.HDR));
                } else {
                    settingsManager.set(LiveBokehModule.this.mAppController.getCameraScope(), Keys.KEY_SCENE_MODE, LiveBokehModule.this.mCameraCapabilities.getStringifier().stringify(SceneMode.AUTO));
                }
                if (LiveBokehModule.this.mCameraState == 5 || LiveBokehModule.this.mCameraState == 2 || LiveBokehModule.this.mCameraState == 6) {
                    LiveBokehModule.this.setCameraState(1);
                    LiveBokehModule.this.mFocusManager.cancelAutoFocus();
                }
                LiveBokehModule.this.updateSceneMode();
            }
        };
        this.mLowlightCallback = new ButtonCallback() {
            public void onStateChanged(int state) {
                if (Keys.isLowlightOn(LiveBokehModule.this.mAppController.getSettingsManager()) && LiveBokehModule.this.isNightToastShow() && CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_NIGHT_MODE_TOAST_ON, false)) {
                    ToastUtil.showToast(LiveBokehModule.this.mActivity, LiveBokehModule.this.mActivity.getString(R.string.night_mode_on_toast), 0);
                }
                if (LiveBokehModule.this.mCameraState == 5 || LiveBokehModule.this.mCameraState == 2 || LiveBokehModule.this.mCameraState == 6) {
                    LiveBokehModule.this.setCameraState(1);
                    LiveBokehModule.this.mFocusManager.cancelAutoFocus();
                }
                LiveBokehModule.this.updateVisidionMode();
            }
        };
        this.mCancelCallback = new OnClickListener() {
            public void onClick(View v) {
                LiveBokehModule.this.onCaptureCancelled();
            }
        };
        this.mDoneCallback = new OnClickListener() {
            public void onClick(View v) {
                LiveBokehModule.this.onCaptureDone();
            }
        };
        this.mRetakeCallback = new OnClickListener() {
            public void onClick(View v) {
                LiveBokehModule.this.mActivity.getCameraAppUI().transitionToIntentCaptureLayout();
                LiveBokehModule.this.onCaptureRetake();
            }
        };
        this.seekListener = new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                float progress = (float) seekBar.getProgress();
                LiveBokehModule.this.mActivity.getCameraAppUI().setBeautySeek(progress);
                LiveBokehModule.this.mActivity.getButtonManager().setLastBeautySeekProgress((int) progress);
                float chooseValue = LiveBokehModule.this.mActivity.getCameraAppUI().getBeautySeek() / 20.0f;
                if (LiveBokehModule.this.isBeautyShow()) {
                    LiveBokehModule.this.mActivity.getButtonManager().showBeauty2Button();
                } else {
                    LiveBokehModule.this.mActivity.getButtonManager().hideBeauty2Button();
                }
                if (chooseValue > 0.0f) {
                    LiveBokehModule.this.mAppController.getCameraAppUI().setBeautyEnable(true);
                    LiveBokehModule.this.mAppController.getButtonManager().setBeautyState(1);
                } else {
                    LiveBokehModule.this.mAppController.getCameraAppUI().setBeautyEnable(false);
                    LiveBokehModule.this.mAppController.getButtonManager().setBeautyState(0);
                }
                LiveBokehModule.this.updateBeautySeek(chooseValue);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
        this.bolkenListener = new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                if (LiveBokehModule.this.mCameraDevice != null) {
                    int level = seekBar.getProgress();
                    if (level == 10 || level == 9) {
                        level = 8;
                    }
                    level = 8 - level;
                    if (LiveBokehModule.this.mCameraDevice != null) {
                        LiveBokehModule.this.mBokehLevel = level;
                        LiveBokehModule.this.mCameraDevice.setPreviewBolkenLevel(level);
                        LiveBokehModule.this.updateLiveBokehLevel(level);
                        LiveBokehModule.this.mCameraDevice.applySettings(LiveBokehModule.this.mCameraSettings);
                    }
                }
            }
        };
        this.mLongshotShutterCallback = new ShutterCallback(this, false, true);
        this.curOritation = -1;
        this.mCameraFinalPreviewCallback = new CameraFinalPreviewCallback() {
            public void onFinalPreviewReturn() {
                if (LiveBokehModule.this.mAppController != null) {
                    final long time = System.currentTimeMillis();
                    Bitmap preview = LiveBokehModule.this.mAppController.getCameraAppUI().getPreviewBitmap();
                    if (preview != null) {
                        int height = preview.getHeight();
                        int i = 1000 == preview.getHeight() ? MotionPictureHelper.FRAME_HEIGHT_9 : 1130 == preview.getHeight() ? 960 : 1280;
                        final Bitmap preview_final = BlurUtil.cropBitmap(preview, height - i);
                        new Thread(new Runnable() {
                            public void run() {
                                long dateTaken = LiveBokehModule.this.mNamedImages.getNextNameEntityForThumbnailOnly().date;
                                ProcessingMedia media = ProcessingMediaManager.getInstance(LiveBokehModule.this.mActivity).add(dateTaken);
                                Bitmap bitmap = CameraUtil.rotateAndMirror(preview_final, (360 - LiveBokehModule.this.mOrientation) % 360, false);
                                Tag access$500 = LiveBokehModule.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("convert time = ");
                                stringBuilder.append(System.currentTimeMillis() - time);
                                Log.d(access$500, stringBuilder.toString());
                                if (media != null) {
                                    media.setThumbnailPath(ProviderUtils.save2Private(LiveBokehModule.this.mActivity, bitmap));
                                    media.setThumbnailBitmap(bitmap);
                                }
                                final TE te = new TE(dateTaken, bitmap);
                                LiveBokehModule.this.mHandler.postDelayed(new Runnable() {
                                    public void run() {
                                        LiveBokehModule.this.mAppController.getCameraAppUI().updatePeekThumbUri(null);
                                        LiveBokehModule.this.mAppController.getCameraAppUI().updatePeekThumbBitmap(te.bitmap);
                                    }
                                }, 500);
                            }
                        }).start();
                    }
                }
            }
        };
        this.mProgressDialogKeyListener = new OnKeyListener() {
            public boolean onKey(DialogInterface dialogInterface, int keyCode, KeyEvent keyEvent) {
                if (!(keyCode == 80 || keyCode == CameraUtil.BOOM_KEY)) {
                    switch (keyCode) {
                        case 24:
                        case 25:
                            break;
                        default:
                            return false;
                    }
                }
                return true;
            }
        };
        this.mProgressUpdateRunnable = new ProgressUpdateRunnable(0);
        this.mPictureTaken = new PictureTaskListener();
        this.mLockedEvoIndex = 0;
        this.mCropRegion = new Rect[4];
        this.mOriginalCropRegion = new Rect[4];
        this.mIsGlMode = false;
        this.mAppController = app;
        this.mGcamModeIndex = app.getAndroidContext().getResources().getInteger(R.integer.camera_mode_gcam);
        this.mSettingsManager = this.mAppController.getSettingsManager();
    }

    public String getPeekAccessibilityString() {
        return this.mAppController.getAndroidContext().getResources().getString(R.string.photo_accessibility_peek);
    }

    public String getModuleStringIdentifier() {
        return PHOTO_MODULE_STRING_ID;
    }

    /* Access modifiers changed, original: protected */
    public LiveBokehUI getLiveBokehUI() {
        if (this.mUI == null) {
            this.mUI = new LiveBokehUI(this.mActivity, this, this.mActivity.getModuleLayoutRoot());
        }
        return this.mUI;
    }

    public void init(CameraActivity activity, boolean isSecureCamera, boolean isCaptureIntent) {
        this.mActivity = activity;
        this.mAppController = this.mActivity;
        this.mUI = getLiveBokehUI();
        this.mActivity.setPreviewStatusListener(this.mUI);
        this.mCameraId = this.mActivity.getSettingsManager().getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID).intValue();
        this.mContentResolver = this.mActivity.getContentResolver();
        this.mIsImageCaptureIntent = isImageCaptureIntent();
        this.mQuickCapture = this.mActivity.getIntent().getBooleanExtra(EXTRA_QUICK_CAPTURE, false);
        this.mSensorManager = (SensorManager) this.mActivity.getSystemService("sensor");
        this.mAudioManager = (AudioManager) this.mActivity.getSystemService("audio");
        this.mUI.setCountdownFinishedListener(this);
        this.mCountdownSoundPlayer = new SoundPlayer(this.mAppController.getAndroidContext());
        this.mSoundPlayer = new SoundPlayer(this.mAppController.getAndroidContext());
        this.mActivity.findViewById(R.id.shutter_cancel_button).setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                LiveBokehModule.this.cancelCountDown();
            }
        });
    }

    private void cancelCountDown() {
        if (this.mUI.isCountingDown()) {
            if (this.mUI.isCountingDown()) {
                this.mFocusManager.sendMessage();
                this.mFocusManager.noEight();
                this.mUI.cancelCountDown();
            }
            if (this.mLastMaskEnable) {
                this.mLastMaskEnable = false;
                this.mActivity.getButtonManager().setEffectWrapperVisible(0);
            }
            if (!this.mIsInIntentReviewUI) {
                this.mAppController.getCameraAppUI().transitionToCapture();
                this.mAppController.getCameraAppUI().showModeOptions();
                this.mAppController.setShutterEnabled(true);
                transitionToTimer(true);
            }
        }
    }

    public boolean isUsingBottomBar() {
        return true;
    }

    private void initializeControlByIntent() {
        if (this.mIsImageCaptureIntent) {
            if (this.mJpegImageData == null) {
                this.mActivity.getCameraAppUI().transitionToIntentCaptureLayout();
            }
            setupCaptureParams();
        }
    }

    /* Access modifiers changed, original: protected */
    public void transitionToTimer(boolean isShow) {
    }

    /* Access modifiers changed, original: protected */
    public void onPreviewStarted() {
        if (!this.mPaused) {
            Log.w(TAG, "KPI photo preview started");
            this.mAppController.getCameraAppUI().onPreviewStarted();
            this.mAppController.onPreviewStarted();
            this.mAppController.setShutterEnabled(true);
            this.mAppController.setShutterButtonLongClickable(this.mIsImageCaptureIntent ^ 1);
            this.mAppController.getCameraAppUI().enableModeOptions();
            this.mUI.clearEvoPendingUI();
            if (this.mEvoFlashLock != null) {
                this.mAppController.getButtonManager().enableButtonWithToken(0, this.mEvoFlashLock.intValue());
                this.mEvoFlashLock = null;
            }
            if (this.mCameraState == 7) {
                setCameraState(5);
            } else {
                setCameraState(1);
            }
            if (isCameraFrontFacing()) {
                this.mUI.setZoomBarVisible(false);
            } else {
                this.mUI.setZoomBarVisible(true);
            }
            if (this.mActivity.getCameraAppUI().isNeedBlur() || onGLRenderEnable()) {
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        LiveBokehModule.this.startFaceDetection();
                    }
                }, 1500);
            } else {
                startFaceDetection();
            }
            BoostUtil.getInstance().releaseCpuLock();
            if (this.mIsGlMode && this.mCameraId == 1) {
                this.mActivity.getCameraAppUI().hideImageCover();
                if (this.mActivity.getCameraAppUI().getBeautyEnable()) {
                    this.mActivity.getButtonManager().setSeekbarProgress((int) this.mActivity.getCameraAppUI().getBeautySeek());
                    updateBeautySeek(this.mActivity.getCameraAppUI().getBeautySeek() / 20.0f);
                }
            }
        }
    }

    private void settingsFirstRun() {
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        if (!this.mActivity.isSecureCamera() && !isImageCaptureIntent()) {
            boolean locationPrompt = settingsManager.isSet(SettingsManager.SCOPE_GLOBAL, Keys.KEY_RECORD_LOCATION) ^ true;
            boolean aspectRatioPrompt = settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_USER_SELECTED_ASPECT_RATIO) ^ true;
            if ((locationPrompt || aspectRatioPrompt) && this.mAppController.getCameraProvider().getFirstBackCameraId() != -1) {
                if (locationPrompt) {
                    this.mUI.showLocationAndAspectRatioDialog(new LocationDialogCallback() {
                        public void onLocationTaggingSelected(boolean selected) {
                            Keys.setLocation(LiveBokehModule.this.mActivity.getSettingsManager(), selected, LiveBokehModule.this.mActivity.getLocationManager());
                        }
                    }, createAspectRatioDialogCallback());
                } else if (!this.mUI.showAspectRatioDialog(createAspectRatioDialogCallback())) {
                    this.mActivity.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_USER_SELECTED_ASPECT_RATIO, true);
                }
            }
        }
    }

    private AspectRatioDialogCallback createAspectRatioDialogCallback() {
        AspectRatio currentAspectRatio;
        Size currentSize = this.mCameraSettings.getCurrentPhotoSize();
        float aspectRatio = ((float) currentSize.width()) / ((float) currentSize.height());
        float f = 1.0f;
        if (aspectRatio < 1.0f) {
            aspectRatio = 1.0f / aspectRatio;
        }
        float f2 = 1.3333334f;
        if (Math.abs(aspectRatio - 1.3333334f) <= 0.1f) {
            currentAspectRatio = AspectRatio.ASPECT_RATIO_4x3;
        } else if (Math.abs(aspectRatio - 1.7777778f) > 0.1f) {
            return null;
        } else {
            currentAspectRatio = AspectRatio.ASPECT_RATIO_16x9;
        }
        List<Size> pictureSizes = ResolutionUtil.getDisplayableSizesFromSupported(this.mCameraCapabilities.getSupportedPhotoSizes(), true);
        int aspectRatio4x3Resolution = 0;
        int aspectRatio16x9Resolution = 0;
        Size largestSize4x3 = new Size(0, 0);
        Size largestSize16x9 = new Size(0, 0);
        for (Size size : pictureSizes) {
            float f3;
            float pictureAspectRatio = ((float) size.width()) / ((float) size.height());
            float pictureAspectRatio2 = pictureAspectRatio < f ? f / pictureAspectRatio : pictureAspectRatio;
            int resolution = size.width() * size.height();
            if (Math.abs(pictureAspectRatio2 - f2) >= 0.1f) {
                f = 1.7777778f;
                f3 = 0.1f;
                if (Math.abs(pictureAspectRatio2 - 1.7777778f) < 0.1f && resolution > aspectRatio16x9Resolution) {
                    aspectRatio16x9Resolution = resolution;
                    largestSize16x9 = size;
                }
            } else if (resolution > aspectRatio4x3Resolution) {
                largestSize4x3 = size;
                f3 = 0.1f;
                aspectRatio4x3Resolution = resolution;
                f = 1.7777778f;
            } else {
                f = 1.7777778f;
                f3 = 0.1f;
            }
            pictureAspectRatio = f;
            pictureAspectRatio2 = f3;
            f = 1.0f;
            f2 = 1.3333334f;
        }
        final Size size4x3ToSelect = largestSize4x3;
        final Size size16x9ToSelect = largestSize16x9;
        return new AspectRatioDialogCallback() {
            public AspectRatio getCurrentAspectRatio() {
                return currentAspectRatio;
            }

            public void onAspectRatioSelected(AspectRatio newAspectRatio, Runnable dialogHandlingFinishedRunnable) {
                if (newAspectRatio == AspectRatio.ASPECT_RATIO_4x3) {
                    LiveBokehModule.this.mActivity.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_PICTURE_SIZE_BACK, SettingsUtil.sizeToSetting(size4x3ToSelect));
                } else if (newAspectRatio == AspectRatio.ASPECT_RATIO_16x9) {
                    LiveBokehModule.this.mActivity.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_PICTURE_SIZE_BACK, SettingsUtil.sizeToSetting(size16x9ToSelect));
                }
                LiveBokehModule.this.mActivity.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_USER_SELECTED_ASPECT_RATIO, true);
                String aspectRatio = LiveBokehModule.this.mActivity.getSettingsManager().getString(SettingsManager.SCOPE_GLOBAL, Keys.KEY_USER_SELECTED_ASPECT_RATIO);
                Tag access$500 = LiveBokehModule.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("aspect ratio after setting it to true=");
                stringBuilder.append(aspectRatio);
                Log.e(access$500, stringBuilder.toString());
                if (newAspectRatio != currentAspectRatio) {
                    Log.i(LiveBokehModule.TAG, "changing aspect ratio from dialog");
                    LiveBokehModule.this.stopPreview();
                    LiveBokehModule.this.startPreview();
                    LiveBokehModule.this.mUI.setRunnableForNextFrame(dialogHandlingFinishedRunnable);
                    return;
                }
                LiveBokehModule.this.mHandler.post(dialogHandlingFinishedRunnable);
            }
        };
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

    public boolean isCameraOpened() {
        return this.mIsCameraOpened;
    }

    /* Access modifiers changed, original: protected */
    public void onCameraOpened() {
        openCameraCommon();
        initializeControlByIntent();
        new Thread() {
            public void run() {
                try {
                    Thread.sleep(500);
                    LiveBokehModule.this.mIsCameraOpened = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void switchCamera() {
        if (!this.mPaused) {
            CameraAppUI cameraAppUI = this.mActivity.getCameraAppUI();
            if (!cameraAppUI.mIsCameraSwitchAnimationRunning) {
                int holdTime = MediaPlayer2.MEDIA_INFO_TIMED_TEXT_ERROR;
                if (cameraAppUI.getCameraGLSurfaceView().getVisibility() == 0) {
                    holdTime = 650;
                }
                cameraAppUI.playCameraSwitchAnimation(400, holdTime);
                if (null > null) {
                    cameraAppUI.getCameraGLSurfaceView().postDelayed(new Runnable() {
                        public void run() {
                            LiveBokehModule.this.switchCameraDefault();
                        }
                    }, (long) null);
                } else {
                    switchCameraDefault();
                }
            }
        }
    }

    private void switchCameraDefault() {
        if (!this.mPaused) {
            this.mCameraId = this.mPendingSwitchCameraId;
            BoostUtil.getInstance().acquireCpuLock();
            setCameraState(4);
            cancelCountDown();
            SettingsManager settingsManager = this.mActivity.getSettingsManager();
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Start to switch camera. id=");
            stringBuilder.append(this.mPendingSwitchCameraId);
            Log.i(tag, stringBuilder.toString());
            closeCamera();
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID, this.mCameraId);
            Message.obtain().what = 0;
            isBackCamera();
            if (this.mCameraId == 0) {
                this.mCameraId = 3;
            }
            if (this.mIsGlMode && this.mCameraId == 1) {
                boolean needChangeResolution = com.hmdglobal.app.camera.util.ResolutionUtil.getRightResolutionById(getModuleId(), this.mCameraId, this.mActivity);
                final CameraSurfaceView surfaceView = this.mActivity.getCameraAppUI().getCameraGLSurfaceView();
                surfaceView.switchCamera(this.mCameraId);
                surfaceView.queueEvent(new Runnable() {
                    public void run() {
                        surfaceView.surfaceChanged(null, 0, surfaceView.getWidth(), surfaceView.getHeight());
                        if (LiveBokehModule.this.mActivity != null) {
                            LiveBokehModule.this.mActivity.runOnUiThread(new Runnable() {
                                public void run() {
                                    LiveBokehModule.this.mHandler.postDelayed(new Runnable() {
                                        public void run() {
                                            LiveBokehModule.this.mActivity.getCameraAppUI().setTextureViewVisible(8);
                                            if (LiveBokehModule.this.mActivity.getCameraAppUI().getCameraGLSurfaceView().getVisibility() != 0) {
                                                surfaceView.setVisibility(0);
                                            }
                                        }
                                    }, 800);
                                }
                            });
                        }
                    }
                });
            } else {
                if (this.mCameraId == 1) {
                    this.mActivity.getButtonManager().setEffectWrapperVisible(8);
                }
                this.mActivity.getCameraAppUI().showOrHideGLSurface(false);
            }
            requestCameraOpen();
            this.mUI.clearFaces();
            if (this.mFocusManager != null) {
                this.mUI.clearFocus();
                this.mFocusManager.removeMessages();
            }
            this.mMirror = isCameraFrontFacing();
            this.mFocusManager.setMirror(this.mMirror);
            setSeekProgress();
        }
    }

    private void requestCameraOpen() {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("requestCameraOpen ");
        stringBuilder.append(this.mCameraId);
        Log.w(tag, stringBuilder.toString());
        this.mActivity.getCameraProvider().requestCamera(this.mCameraId, GservicesHelper.useCamera2ApiThroughPortabilityLayer(this.mActivity.getAndroidContext()));
    }

    private boolean isHdrToastShow() {
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        if (settingsManager == null) {
            return false;
        }
        int launchingTimes = settingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_NEW_LAUNCHING_TIMES_FOR_HDRTOAST).intValue();
        boolean isHdrToastShow = launchingTimes < 3 || (launchingTimes == 3 && !Keys.isNewLaunchingForHdrtoast(settingsManager));
        if (!isHdrToastShow) {
            return false;
        }
        if (Keys.isNewLaunchingForHdrtoast(settingsManager)) {
            settingsManager.setValueByIndex(SettingsManager.SCOPE_GLOBAL, Keys.KEY_NEW_LAUNCHING_TIMES_FOR_HDRTOAST, launchingTimes + 1);
            Keys.setNewLaunchingForHdrtoast(settingsManager, false);
        }
        return true;
    }

    private boolean isNightToastShow() {
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        if (settingsManager == null) {
            return false;
        }
        int launchingTimes = settingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_NEW_LAUNCHING_TIMES_FOR_NIGHTTOAST).intValue();
        boolean isNightToastShow = launchingTimes < 3 || (launchingTimes == 3 && !Keys.isNewLaunchingForNighttoast(settingsManager));
        if (!isNightToastShow) {
            return false;
        }
        if (Keys.isNewLaunchingForNighttoast(settingsManager)) {
            settingsManager.setValueByIndex(SettingsManager.SCOPE_GLOBAL, Keys.KEY_NEW_LAUNCHING_TIMES_FOR_NIGHTTOAST, launchingTimes + 1);
            Keys.setNewLaunchingForNighttoast(settingsManager, false);
        }
        return true;
    }

    public void intentReviewCancel() {
        onCaptureCancelled();
    }

    public void intentReviewDone() {
        onCaptureDone();
    }

    public void intentReviewRetake() {
        this.mActivity.getCameraAppUI().transitionToIntentCaptureLayout();
        onCaptureRetake();
    }

    public void hardResetSettings(SettingsManager settingsManager) {
        settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR_PLUS, false);
        if (GcamHelper.hasGcamAsSeparateModule()) {
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR, false);
        }
    }

    public HardwareSpec getHardwareSpec() {
        return this.mCameraSettings != null ? new HardwareSpecImpl(getCameraProvider(), this.mCameraCapabilities) : null;
    }

    public BottomBarUISpec getBottomBarSpec() {
        BottomBarUISpec bottomBarSpec = new BottomBarUISpec();
        bottomBarSpec.moduleName = LiveBokehModule.class.getSimpleName();
        bottomBarSpec.enableCamera = true;
        bottomBarSpec.hideCamera = hideCamera();
        bottomBarSpec.hideCameraForced = hideCameraForced();
        bottomBarSpec.hideSetting = hideSetting();
        bottomBarSpec.cameraCallback = this.mCameraCallback;
        bottomBarSpec.enableFlash = this.mActivity.currentBatteryStatusOK();
        bottomBarSpec.hideHdr = isHdrShow() ^ 1;
        bottomBarSpec.enableHdr = true;
        bottomBarSpec.hdrCallback = this.mHdrPlusCallback;
        bottomBarSpec.enableGridLines = true;
        bottomBarSpec.hideGridLines = true;
        bottomBarSpec.hideLowlight = isLowLightShow() ^ 1;
        bottomBarSpec.lowlightCallback = this.mLowlightCallback;
        bottomBarSpec.showBeautyButton = this.mCameraId == 1;
        bottomBarSpec.showEffectButton = false;
        bottomBarSpec.seekbarChangeListener = this.seekListener;
        bottomBarSpec.seekbarBolkenListener = this.bolkenListener;
        bottomBarSpec.hideBolken = false;
        bottomBarSpec.showBeauty2 = isBeautyShow();
        if (this.mCameraCapabilities != null) {
            bottomBarSpec.enableExposureCompensation = true;
            bottomBarSpec.exposureCompensationSetCallback = new ExposureCompensationSetCallback() {
                public void setExposure(int value) {
                    LiveBokehModule.this.setExposureCompensation(value);
                }
            };
            bottomBarSpec.minExposureCompensation = this.mCameraCapabilities.getMinExposureCompensation();
            bottomBarSpec.maxExposureCompensation = this.mCameraCapabilities.getMaxExposureCompensation();
            bottomBarSpec.exposureCompensationStep = this.mCameraCapabilities.getExposureCompensationStep();
        }
        boolean isCountDownShow = isCountDownShow();
        bottomBarSpec.enableSelfTimer = isCountDownShow;
        bottomBarSpec.showSelfTimer = isCountDownShow;
        bottomBarSpec.hideFlash = isFlashShow() ^ 1;
        if (isCameraFrontFacing()) {
            ModuleController controller = this.mAppController.getCurrentModuleController();
            if (!(controller.getHardwareSpec() == null || controller.getHardwareSpec().isFlashSupported())) {
                bottomBarSpec.hideFlash = true;
            }
        }
        if (isImageCaptureIntent()) {
            bottomBarSpec.showCancel = true;
            bottomBarSpec.cancelCallback = this.mCancelCallback;
            bottomBarSpec.showDone = true;
            bottomBarSpec.doneCallback = this.mDoneCallback;
            bottomBarSpec.showRetake = true;
            bottomBarSpec.retakeCallback = this.mRetakeCallback;
        }
        bottomBarSpec.showWrapperButton = isWrapperButtonShow();
        return bottomBarSpec;
    }

    private void openCameraCommon() {
        this.mUI.onCameraOpened(this.mCameraCapabilities, this.mCameraSettings);
        if (this.mIsImageCaptureIntent) {
            this.mActivity.getSettingsManager().setToDefault(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR_PLUS);
        }
        updateSceneMode();
    }

    public void updatePreviewAspectRatio(float aspectRatio) {
        this.mAppController.updatePreviewAspectRatio(aspectRatio);
    }

    private void resetExposureCompensation() {
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        if (settingsManager == null) {
            Log.e(TAG, "Settings manager is null!");
        } else {
            settingsManager.setToDefault(this.mAppController.getCameraScope(), Keys.KEY_EXPOSURE);
        }
    }

    private void initializeFirstTime() {
        if (!this.mFirstTimeInitialized && !this.mPaused) {
            this.mUI.initializeFirstTime();
            getServices().getMemoryManager().addListener(this);
            this.mNamedImages = new NamedImages();
            this.mFirstTimeInitialized = true;
            addIdleHandler();
            this.mActivity.updateStorageSpaceAndHint(null);
        }
    }

    private void initializeSecondTime() {
        getServices().getMemoryManager().addListener(this);
        if (this.mNamedImages == null) {
            this.mNamedImages = new NamedImages();
        }
        this.mUI.initializeSecondTime(this.mCameraCapabilities, this.mCameraSettings);
    }

    private void addIdleHandler() {
        Looper.myQueue().addIdleHandler(new IdleHandler() {
            public boolean queueIdle() {
                Storage.ensureOSXCompatible();
                return false;
            }
        });
    }

    /* Access modifiers changed, original: protected */
    public boolean needFaceDetection() {
        return true;
    }

    /* JADX WARNING: Missing block: B:10:0x004a, code skipped:
            return;
     */
    public void startFaceDetection() {
        /*
        r6 = this;
        r0 = r6.mFaceDetectionStarted;
        if (r0 != 0) goto L_0x004a;
    L_0x0004:
        r0 = r6.mCameraDevice;
        if (r0 == 0) goto L_0x004a;
    L_0x0008:
        r0 = r6.needFaceDetection();
        if (r0 != 0) goto L_0x000f;
    L_0x000e:
        goto L_0x004a;
    L_0x000f:
        r0 = r6.mCameraCapabilities;
        r0 = r0.getMaxNumOfFacesSupported();
        if (r0 <= 0) goto L_0x0049;
    L_0x0017:
        r0 = 1;
        r6.mFaceDetectionStarted = r0;
        r1 = r6.mCameraDevice;
        r2 = r6.mHandler;
        r3 = r6.mUI;
        r1.setFaceDetectionCallback(r2, r3);
        r1 = r6.mUI;
        r2 = r6.mDisplayOrientation;
        r3 = r6.isCameraFrontFacing();
        r4 = r6.mCameraId;
        r4 = r6.cropRegionForZoom(r4);
        r5 = 1065353216; // 0x3f800000 float:1.0 double:5.263544247E-315;
        r1.onStartFaceDetection(r2, r3, r4, r5);
        r1 = TAG;
        r2 = "startFaceDetection";
        com.hmdglobal.app.camera.debug.Log.w(r1, r2);
        r1 = r6.mCameraDevice;
        r1.startFaceDetection();
        r1 = com.hmdglobal.app.camera.util.SessionStatsCollector.instance();
        r1.faceScanActive(r0);
    L_0x0049:
        return;
    L_0x004a:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.LiveBokehModule.startFaceDetection():void");
    }

    /* JADX WARNING: Missing block: B:8:0x0038, code skipped:
            return;
     */
    public void stopFaceDetection() {
        /*
        r3 = this;
        r0 = r3.mFaceDetectionStarted;
        if (r0 == 0) goto L_0x0038;
    L_0x0004:
        r0 = r3.mCameraDevice;
        if (r0 != 0) goto L_0x0009;
    L_0x0008:
        goto L_0x0038;
    L_0x0009:
        r0 = r3.mCameraCapabilities;
        r0 = r0.getMaxNumOfFacesSupported();
        if (r0 <= 0) goto L_0x0037;
    L_0x0011:
        r0 = 0;
        r3.mFaceDetectionStarted = r0;
        r1 = r3.mCameraDevice;
        r2 = 0;
        r1.setFaceDetectionCallback(r2, r2);
        r1 = TAG;
        r2 = "stopFaceDetection";
        com.hmdglobal.app.camera.debug.Log.w(r1, r2);
        r1 = r3.mCameraDevice;
        r1.stopFaceDetection();
        r1 = r3.mUI;
        r1.pauseFaceDetection();
        r1 = r3.mUI;
        r1.clearFaces();
        r1 = com.hmdglobal.app.camera.util.SessionStatsCollector.instance();
        r1.faceScanActive(r0);
    L_0x0037:
        return;
    L_0x0038:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.LiveBokehModule.stopFaceDetection():void");
    }

    private ResizeBundle cropJpegDataToAspectRatio(ResizeBundle dataBundle) {
        int newHeight;
        int newWidth;
        byte[] jpegData = dataBundle.jpegData;
        ExifInterface exif = dataBundle.exif;
        float targetAspectRatio = dataBundle.targetAspectRatio;
        Bitmap original = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();
        if (originalWidth > originalHeight) {
            newHeight = (int) (((float) originalWidth) / targetAspectRatio);
            newWidth = originalWidth;
        } else {
            newWidth = (int) (((float) originalHeight) / targetAspectRatio);
            newHeight = originalHeight;
        }
        int xOffset = (originalWidth - newWidth) / 2;
        int yOffset = (originalHeight - newHeight) / 2;
        if (xOffset < 0 || yOffset < 0) {
            return dataBundle;
        }
        Bitmap resized = Bitmap.createBitmap(original, xOffset, yOffset, newWidth, newHeight);
        exif.setTagValue(ExifInterface.TAG_PIXEL_X_DIMENSION, new Integer(newWidth));
        exif.setTagValue(ExifInterface.TAG_PIXEL_Y_DIMENSION, new Integer(newHeight));
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        resized.compress(CompressFormat.JPEG, 90, stream);
        dataBundle.jpegData = stream.toByteArray();
        return dataBundle;
    }

    private void playLiveBokehSound() {
        int mode = this.mAudioManager.getRingerMode();
        if (Keys.isShutterSoundOn(this.mAppController.getSettingsManager()) && mode == 2 && this.mSoundPlayer != null) {
            this.mHandler.postDelayed(new Runnable() {
                public void run() {
                    if (CustomUtil.getInstance().isSkuid()) {
                        LiveBokehModule.this.mSoundPlayer.play(R.raw.shutter_sound_2, 1.0f);
                    } else {
                        LiveBokehModule.this.mSoundPlayer.play(R.raw.shutter, 1.0f);
                    }
                }
            }, 500);
        }
    }

    public int getmCameraId() {
        return this.mCameraId;
    }

    private void saveAsJPEG(byte[] bytes, String path) {
        Log.d(TAG, "saveAsJPEG");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sdcard/");
        stringBuilder.append(System.currentTimeMillis());
        stringBuilder.append(path);
        stringBuilder.append("_depth.JPEG");
        File file = new File(stringBuilder.toString());
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(file));
            out.write(bytes, 0, bytes.length);
            try {
                out.close();
            } catch (Exception e) {
                Log.d(TAG, e.toString());
            }
        } catch (Exception e2) {
            Log.d(TAG, e2.toString());
            if (out != null) {
                out.close();
            }
        } catch (Throwable th) {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception e3) {
                    Log.d(TAG, e3.toString());
                }
            }
        }
    }

    private byte[] aftMirrorJpeg(byte[] jpegData) {
        Bitmap bitmap2 = CameraUtil.rotateAndMirror(BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length), 0, isNeedMirrorSelfie());
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        bitmap2.compress(CompressFormat.JPEG, 70, buf);
        return buf.toByteArray();
    }

    public Map<String, Object> buildExternalBundle() {
        Map<String, Object> externalInfo = new HashMap();
        List<String> faces = CameraUtil.getCompensatedFaceRects(this.mFaces);
        externalInfo.put(ExternalExifInterface.FACE_RECTS, faces);
        if (faces.size() <= 0 || this.mCameraId == 0) {
            String moduleString = getModuleStringIdentifier();
            externalInfo.put(ExternalExifInterface.MODULE_NAME, getModuleStringIdentifier());
        } else {
            externalInfo.put(ExternalExifInterface.MODULE_NAME, ExternalExifInterface.FACE_SHOW_TAG);
        }
        return externalInfo;
    }

    /* Access modifiers changed, original: protected */
    public void updateThumbnail(ExifInterface exif) {
        if (exif.hasThumbnail()) {
            this.mActivity.getCameraAppUI().updatePeekThumbUri(null);
            this.mActivity.getCameraAppUI().updatePeekThumbBitmapWithAnimation(exif.getThumbnailBitmap());
        }
    }

    public int getModuleId() {
        return this.mAppController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_livebokeh);
    }

    /* Access modifiers changed, original: protected */
    public boolean needEnableExposureAdjustment() {
        boolean z = true;
        if (CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_ENABLE_COMPENSATION_OTHER_THAN_AUTO, true)) {
            return true;
        }
        if (this.mSceneMode != SceneMode.AUTO || Keys.isLowlightOn(this.mAppController.getSettingsManager())) {
            z = false;
        }
        return z;
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
            case 3:
            case 7:
            case 8:
            case 9:
            case 10:
                this.mAppController.getLockEventListener().onShutter();
                return;
            case 4:
                this.mAppController.getLockEventListener().onSwitching();
                return;
            default:
                return;
        }
    }

    private void animateAfterShutter() {
        if (!this.mIsImageCaptureIntent) {
            Log.v(TAG, "show preCapture animation");
            this.mUI.animateFlash();
        }
    }

    public int getJpegRotation(boolean isNeedMirrorSelfie) {
        this.mJpegRotation = this.mActivity.getCameraProvider().getCharacteristics(this.mCameraId).getRelativeImageOrientation(this.mActivity.isAutoRotateScreen() ? this.mDisplayRotation : this.mOrientation, isNeedMirrorSelfie);
        return this.mJpegRotation;
    }

    private void setJpegRotation(boolean isNeedMirrorSelfie) {
        int orientation = this.mActivity.isAutoRotateScreen() ? this.mDisplayRotation : this.mOrientation;
        this.mJpegRotation = this.mActivity.getCameraProvider().getCharacteristics(this.mCameraId).getJpegOrientation(this.mOrientation);
        this.mCameraDevice.setJpegOrientation(this.mJpegRotation);
        this.mCameraDevice.setModuleId(getModuleId(), this.mJpegRotation);
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("capture orientation (screen:device:used:jpeg) ");
        stringBuilder.append(this.mDisplayRotation);
        stringBuilder.append(":");
        stringBuilder.append(this.mOrientation);
        stringBuilder.append(":");
        stringBuilder.append(orientation);
        stringBuilder.append(":");
        stringBuilder.append(this.mJpegRotation);
        Log.v(tag, stringBuilder.toString());
    }

    /* Access modifiers changed, original: protected */
    public boolean isNeedMirrorSelfie() {
        return Keys.isMirrorSelfieOn(this.mAppController.getSettingsManager());
    }

    /* Access modifiers changed, original: protected */
    public void updateFrontPhotoflipMode() {
        if (isCameraFrontFacing()) {
            this.mCameraSettings.setMirrorSelfieOn(Keys.isMirrorSelfieOn(this.mAppController.getSettingsManager()));
        }
    }

    public boolean capture() {
        Log.w(TAG, "capture");
        if (this.mCameraDevice == null || this.mCameraState == 3 || this.mCameraState == 7 || this.mCameraState == 4) {
            return false;
        }
        if (this.mCameraState == 5) {
            setCameraState(7);
        } else if (this.mCameraState != 8) {
            setCameraState(3);
        }
        this.mCaptureStartTime = System.currentTimeMillis();
        this.mPostViewPictureCallbackTime = 0;
        this.mJpegImageData = null;
        boolean animateBefore = this.mSceneMode == SceneMode.HDR;
        if (animateBefore) {
            animateAfterShutter();
        }
        Location loc = this.mActivity.getLocationManager().getCurrentLocation();
        this.mLocation = loc;
        CameraUtil.setGpsParameters(this.mCameraSettings, loc);
        setJpegRotation(isNeedMirrorSelfie());
        this.mCameraSettings.setRTDofMode(1);
        if (this.mCameraState == 8) {
            this.mLongshotPictureTakenCallback = new LongshotPictureCallback(loc);
            this.mReceivedBurstNum = 0;
            this.mCameraDevice.enableShutterSound(false);
            this.mCameraSettings.setFlashMode(FlashMode.OFF);
            if (takeOptimizedBurstShot(loc)) {
                this.mFaceDetectionStarted = false;
                Log.w(TAG, "burst shot started1");
                return true;
            }
            this.mCameraDevice.applySettings(this.mCameraSettings);
            if (1 != this.mAppController.getSupportedHardwarelevel(this.mCameraId)) {
                this.mCameraDevice.burstShot(null, null, null, null, null);
                this.mCameraDevice.takePicture(this.mHandler, new BurstShutterCallback(this, null), this.mRawPictureCallback, this.mPostViewPictureCallback, this.mLongshotPictureTakenCallback);
                this.mNamedImages.nameNewImage(this.mCaptureStartTime);
                this.mFaceDetectionStarted = false;
                Log.w(TAG, "burst shot started2");
                return true;
            }
            this.mHandler.sendEmptyMessage(3);
        } else {
            this.mCameraDevice.enableShutterSound(false);
            String exposureTime = this.mCameraSettings.getExposureTime();
            if (TextUtils.isEmpty(this.mLastISOValue)) {
                this.mLastISOValue = this.mCameraSettings.getISOValue();
            }
            String isoValue = this.mCameraSettings.getISOValue();
            int continuousIso = this.mCameraSettings.getContinuousIso();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("exposureTime=");
            stringBuilder.append(exposureTime);
            stringBuilder.append(" isoValue=");
            stringBuilder.append(isoValue);
            stringBuilder.append(" mLastISOValue=");
            stringBuilder.append(this.mLastISOValue);
            stringBuilder.append(" continuousIso=");
            stringBuilder.append(continuousIso);
            android.util.Log.i("manual", stringBuilder.toString());
            if (exposureTime != null) {
                if (exposureTime.equals("0") || !(isoValue.equals("auto") || (this.mLastISOValue.equals("auto") && isoValue.equals("manual") && this.mforceISOManual))) {
                    if (this.mLastISOValue.equals("auto") && isoValue.equals("manual") && this.mforceISOManual) {
                        android.util.Log.i("manual", "reset the iso value to auto mode");
                        this.mCameraSettings.setISOValue("auto");
                        this.mforceISOManual = false;
                    }
                } else if (Double.parseDouble(exposureTime) < 33.0d) {
                    android.util.Log.i("manual", "force the iso value to be 3200");
                    this.mCameraSettings.setContinuousIso(3200);
                    this.mCameraSettings.setISOValue("manual");
                    this.mforceISOManual = true;
                }
            }
            this.mCameraDevice.applySettings(this.mCameraSettings);
            this.mFaces = this.mUI.filterAndAdjustFaces(isNeedMirrorSelfie(), this.mJpegRotation);
            showOptimisingPhotoHint();
            playLiveBokehSound();
            this.mNamedImages.nameNewImage(this.mCaptureStartTime);
            this.mCameraDevice.takePicture(this.mHandler, new ShutterCallback(!animateBefore), this.mRawPictureCallback, this.mPostViewPictureCallback, getJpegPictureCallback());
        }
        this.mFaceDetectionStarted = false;
        return true;
    }

    /* Access modifiers changed, original: protected */
    public boolean takeOptimizedBurstShot(Location loc) {
        stopFaceDetection();
        return false;
    }

    public CameraPictureCallback getJpegPictureCallback() {
        return new JpegPictureCallback(this.mActivity.getLocationManager().getCurrentLocation());
    }

    public void setFocusParameters() {
        setCameraParameters(4);
    }

    private void updateSceneMode() {
        if (SceneMode.AUTO != this.mSceneMode) {
            overrideCameraSettings(this.mCameraSettings.getCurrentFlashMode(), this.mFocusManager.getFocusMode(this.mCameraSettings.getCurrentFocusMode()));
        }
    }

    private void overrideCameraSettings(FlashMode flashMode, FocusMode focusMode) {
        Stringifier stringifier = this.mCameraCapabilities.getStringifier();
        SettingsManager settingsManager = this.mAppController.getSettingsManager();
        if (!FlashMode.NO_FLASH.equals(flashMode)) {
            if (Keys.isHdrOn(settingsManager) || Keys.isLowlightOn(settingsManager)) {
                settingsManager.set(this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE, stringifier.stringify(FlashMode.OFF));
            } else {
                settingsManager.set(this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE, stringifier.stringify(flashMode));
            }
        }
        if (focusMode != null) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("override focus mode for ");
            stringBuilder.append(focusMode.name());
            Log.v(tag, stringBuilder.toString());
        }
        settingsManager.set(this.mAppController.getCameraScope(), Keys.KEY_FOCUS_MODE, stringifier.stringify(focusMode));
    }

    public void onOrientationChanged(int orientation) {
        if (orientation != -1) {
            this.mUI.onOrientationChanged(CameraUtil.roundOrientation(orientation, this.mOrientation));
            int lastOrientation = this.mOrientation;
            this.mOrientation = (360 - orientation) % 360;
            this.mUI.setPostGestureOrientation((this.mSensorOrientation + this.mOrientation) % 360);
            this.mUI.setGestureDisplayOrientation(this.mOrientation);
            if (this.mCameraDevice != null) {
                orientation = getJpegRotation(this.mCameraDevice.getCameraId(), orientation);
                if (!(this.curOritation == orientation || this.mCameraDevice.getCamera() == null)) {
                    this.curOritation = orientation;
                }
                if (lastOrientation != this.mOrientation) {
                    this.mJpegRotation = this.mActivity.getCameraProvider().getCharacteristics(this.mCameraId).getJpegOrientation(this.mOrientation);
                    this.mCameraDevice.setModuleId(getModuleId(), this.mJpegRotation);
                    this.mCameraDevice.setPreviewBolkenLevel(this.mBokehLevel);
                }
            } else {
                Log.e(TAG, "CameraDevice = null, can't set Parameter.setRotation");
            }
        }
    }

    public static int getJpegRotation(int cameraId, int orientation) {
        return 0;
    }

    public void onCameraAvailable(CameraProxy cameraProxy) {
        Log.i(TAG, "onCameraAvailable");
        if (!this.mPaused) {
            this.mCameraDevice = cameraProxy;
            this.mCameraDevice.initExtCamera(this.mActivity);
            int rotation = this.mActivity.getWindowManager().getDefaultDisplay().getRotation();
            this.mJpegRotation = this.mActivity.getCameraProvider().getCharacteristics(this.mCameraId).getJpegOrientation(this.mOrientation);
            this.mCameraDevice.setModuleId(getModuleId(), this.mJpegRotation);
            initializeCapabilities();
            SessionStatsCollector.instance().faceScanActive(true);
            this.mCameraDevice.setFaceDetectionCallback(this.mHandler, this.mUI);
            this.mCameraDevice.setFinalPreviewCallback(this.mCameraFinalPreviewCallback);
            if (this.mIsGlMode) {
                this.mActivity.getCameraAppUI().initSurfaceRender(this.mCameraDevice);
                this.mActivity.getCameraAppUI().setOrientation(this.mCameraId);
            }
            this.mZoomValue = 1.0f;
            if (this.mFocusManager == null) {
                initializeFocusManager();
            }
            this.mFocusManager.updateCapabilities(this.mCameraCapabilities);
            this.mCameraSettings = this.mCameraDevice.getSettings();
            if (this.mCameraSettings == null) {
                Log.e(TAG, "camera setting is null");
            }
            readCameraInitialParameters();
            this.mActivity.getSettingsManager().addListener(this);
            this.mCameraPreviewParamsReady = true;
            startPreview();
            onCameraOpened();
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
            this.mUI.clearReviewImage();
            this.mJpegImageData = null;
            setupPreview();
        }
    }

    public void onCaptureDone() {
        Log.i(TAG, "onCaptureDone");
        if (!this.mPaused) {
            byte[] data = this.mJpegImageData;
            FileOutputStream tempStream = null;
            if (this.mCropValue != null) {
                Uri tempUri = null;
                try {
                    File path = this.mActivity.getFileStreamPath(sTempCropFilename);
                    path.delete();
                    tempStream = this.mActivity.openFileOutput(sTempCropFilename, 0);
                    tempStream.write(data);
                    tempStream.close();
                    tempUri = Uri.fromFile(path);
                    Log.v(TAG, "wrote temp file for cropping to: crop-temp");
                    Bundle newExtras = new Bundle();
                    if (this.mCropValue.equals("circle")) {
                        newExtras.putString("circleCrop", "true");
                    }
                    if (this.mSaveUri != null) {
                        Tag tag = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("setting output of cropped file to: ");
                        stringBuilder.append(this.mSaveUri);
                        Log.v(tag, stringBuilder.toString());
                        newExtras.putParcelable("output", this.mSaveUri);
                    } else {
                        newExtras.putBoolean(CameraUtil.KEY_RETURN_DATA, true);
                    }
                    if (this.mActivity.isSecureCamera()) {
                        newExtras.putBoolean(CameraUtil.KEY_SHOW_WHEN_LOCKED, true);
                    }
                    String CROP_ACTION = "com.android.camera.action.CROP";
                    Intent cropIntent = new Intent("com.android.camera.action.CROP");
                    cropIntent.setData(tempUri);
                    cropIntent.putExtras(newExtras);
                    Log.v(TAG, "starting CROP intent for capture");
                    this.mActivity.startActivityForResult(cropIntent, 1000);
                } catch (FileNotFoundException ex) {
                    Log.w(TAG, "error writing temp cropping file to: crop-temp", ex);
                    this.mActivity.setResultEx(0);
                    this.mActivity.finish();
                } catch (IOException ex2) {
                    Log.w(TAG, "error writing temp cropping file to: crop-temp", ex2);
                    this.mActivity.setResultEx(0);
                    this.mActivity.finish();
                } finally {
                    CameraUtil.closeSilently(tempStream);
                }
            } else if (this.mSaveUri != null) {
                savePicWhenNotNormal(data);
                OutputStream outputStream = null;
                StringBuilder stringBuilder2;
                try {
                    outputStream = this.mContentResolver.openOutputStream(this.mSaveUri);
                    outputStream.write(data);
                    outputStream.close();
                    Tag tag2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("saved result to URI: ");
                    stringBuilder2.append(this.mSaveUri);
                    Log.v(tag2, stringBuilder2.toString());
                    this.mActivity.setResultEx(-1);
                    this.mActivity.finish();
                } catch (IOException ex3) {
                    Tag tag3 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("exception saving result to URI: ");
                    stringBuilder2.append(this.mSaveUri);
                    Log.w(tag3, stringBuilder2.toString(), ex3);
                } catch (Throwable th) {
                    CameraUtil.closeSilently(outputStream);
                }
                CameraUtil.closeSilently(outputStream);
            } else {
                Bitmap bitmap = CameraUtil.rotate(CameraUtil.makeBitmap(data, 51200), Exif.getOrientation(Exif.getExif(data)));
                Log.v(TAG, "inlined bitmap into capture intent result");
                this.mActivity.setResultEx(-1, new Intent("inline-data").putExtra("data", bitmap));
                this.mActivity.finish();
            }
        }
    }

    private void savePicWhenNotNormal(byte[] data) {
        int width;
        int height;
        ExifInterface exif = Exif.getExif(data);
        int orientation = Exif.getOrientation(exif);
        Integer exifWidth = exif.getTagIntValue(ExifInterface.TAG_PIXEL_X_DIMENSION);
        Integer exifHeight = exif.getTagIntValue(ExifInterface.TAG_PIXEL_Y_DIMENSION);
        if (!this.mShouldResizeTo16x9 || exifWidth == null || exifHeight == null) {
            Size s = this.mCameraSettings.getCurrentPhotoSize();
            if ((this.mJpegRotation + orientation) % MediaProviderUtils.ROTATION_180 == 0) {
                width = s.width();
                height = s.height();
            } else {
                int width2 = s.height();
                height = s.width();
                width = width2;
            }
        } else {
            width = exifWidth.intValue();
            height = exifHeight.intValue();
        }
        NamedEntity name = this.mNamedImages.getNextNameEntity();
        String title = name == null ? null : name.title;
        long date = name == null ? -1 : name.date;
        if (this.mDebugUri != null) {
            saveToDebugUri(data);
            if (title != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(DEBUG_IMAGE_PREFIX);
                stringBuilder.append(title);
                title = stringBuilder.toString();
            }
        }
        String title2 = title;
        long j;
        if (title2 == null) {
            Log.e(TAG, "Unbalanced name/data pair");
            j = date;
            NamedEntity namedEntity = name;
            Integer num = exifHeight;
            return;
        }
        if (date == -1) {
            j = this.mCaptureStartTime;
        } else {
            j = date;
        }
        if (this.mHeading >= 0) {
            ExifTag directionRefTag = exif.buildTag(ExifInterface.TAG_GPS_IMG_DIRECTION_REF, "M");
            ExifTag directionTag = exif.buildTag(ExifInterface.TAG_GPS_IMG_DIRECTION, new Rational((long) this.mHeading, 1));
            exif.setTag(directionRefTag);
            exif.setTag(directionTag);
        }
        getServices().getMediaSaver().addImage(data, title2, j, this.mLocation, width, height, orientation, exif, this.mOnMediaSavedListener, this.mContentResolver);
    }

    public void onShutterCoordinate(TouchCoordinate coord) {
        this.mShutterTouchCoordinate = coord;
    }

    public void onShutterButtonFocus(boolean pressed) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("ShutterButtonFocus ,pressed=");
        stringBuilder.append(pressed);
        Log.w(tag, stringBuilder.toString());
        if (!pressed) {
            if (this.isOptimeizeSnapshot) {
                if (this.mCameraState == 8 || this.mCameraState == 9) {
                    Log.w(TAG, "ShutterButtonFocus ,bustShot.close()");
                    this.snapShot.stop();
                }
            } else if (this.mCameraState == 8 || this.mCameraState == 9) {
                setCameraState(10);
            }
            this.mSoundPlayer.pause(this.mStreamId);
        }
    }

    /* Access modifiers changed, original: protected */
    public void stopBurst() {
        this.mAppController.setShutterEnabled(true);
        int receivedCount = this.mReceivedBurstNum;
        this.mReceivedBurstNum = 0;
        this.mHandler.removeMessages(3);
        updateParametersFlashMode();
        clearFocusWithoutChangingState();
        if (this.mCameraState == 8 || this.mCameraState == 9 || this.mCameraState == 10) {
            abortOptimizedBurstShot();
            Log.w(TAG, "parameters post update");
            if ((this.mCameraState == 9 || this.mCameraState == 10) && receivedCount != 0) {
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Burst current burst num is ");
                stringBuilder.append(this.mBurstNumForOneSingleBurst);
                Log.w(tag, stringBuilder.toString());
                if (this.mBurstShotCheckQueue.setCapacity(receivedCount)) {
                    showSavingHint(receivedCount);
                }
                this.mHandler.post(new Runnable() {
                    public void run() {
                        LiveBokehModule.this.mUI.updateBurstCount(0);
                    }
                });
            }
        }
        setCameraState(1);
        HelpTipsManager helpTipsManager = this.mAppController.getHelpTipsManager();
        if (!this.mBurstShotNotifyHelpTip && helpTipsManager != null) {
            helpTipsManager.onBurstShotResponse();
            this.mBurstShotNotifyHelpTip = true;
        }
    }

    /* Access modifiers changed, original: protected */
    public void abortOptimizedBurstShot() {
        this.mCameraDevice.abortBurstShot();
        if (1 != this.mAppController.getSupportedHardwarelevel(this.mCameraId)) {
            lightlyRestartPreview();
        }
    }

    /* Access modifiers changed, original: protected */
    public void showSavingHint(int count) {
        if (count != 0) {
            if (this.mProgressDialog == null) {
                this.mProgressDialog = new ProgressDialog(this.mActivity);
                this.mProgressDialog.setOnKeyListener(this.mProgressDialogKeyListener);
                this.mProgressDialog.setProgressStyle(0);
                this.mProgressDialog.setCancelable(false);
            }
            this.mProgressDialog.setMessage(String.format(this.mActivity.getAndroidContext().getResources().getString(R.string.burst_saving_hint), new Object[]{Integer.valueOf(count)}));
            this.mProgressDialog.show();
        }
    }

    /* Access modifiers changed, original: protected */
    public void dismissSavingHint() {
        if (this.mProgressDialog != null && this.mProgressDialog.isShowing()) {
            this.mProgressDialog.dismiss();
        }
    }

    private void showOptimisingPhotoHint() {
        SettingsManager settingsManager = this.mAppController.getSettingsManager();
        boolean lowLightOn = true;
        boolean hdrOn = Keys.isHdrOn(settingsManager) && isHdrShow();
        if (!(Keys.isLowlightOn(settingsManager) && isLowLightShow())) {
            lowLightOn = false;
        }
        boolean facebeautyOn = isFacebeautyEnabled();
        if (hdrOn || lowLightOn || facebeautyOn) {
            if (this.mOptimisingPhotoDialog == null) {
                this.mOptimisingPhotoDialog = new ProgressDialog(this.mActivity);
                this.mOptimisingPhotoDialog.setOnKeyListener(this.mProgressDialogKeyListener);
                this.mOptimisingPhotoDialog.setCancelable(false);
                this.mOptimisingPhotoDialog.show();
                LayoutParams params = this.mOptimisingPhotoDialog.getWindow().getAttributes();
                Window w = this.mOptimisingPhotoDialog.getWindow();
                w.setContentView(R.layout.optimise_photo_layout);
                params.dimAmount = 0.0f;
                w.setAttributes(params);
            }
            this.mHandler.removeCallbacks(this.mProgressUpdateRunnable);
            this.mHandler.postDelayed(this.mProgressUpdateRunnable.setProgress(0), 100);
        }
    }

    private void dismissOptimisingPhotoHint() {
        if (this.mOptimisingPhotoDialog != null && this.mOptimisingPhotoDialog.isShowing()) {
            this.mOptimisingPhotoDialog.dismiss();
        }
        this.mOptimisingPhotoDialog = null;
        this.mHandler.removeCallbacks(this.mProgressUpdateRunnable);
    }

    public void onShutterButtonClick() {
        doShutterButtonClick(false);
        this.mBurstResultQueue.clear();
    }

    private void doShutterButtonClick(boolean isGestureShot) {
        Log.w(TAG, "KPI shutter click");
        Tag tag;
        StringBuilder stringBuilder;
        if (this.mPaused || this.mCameraState == 4 || this.mCameraState == 3 || isInBurstshot() || this.mCameraState == 0 || !this.mAppController.isShutterEnabled() || this.mAppController.getCameraAppUI().isShutterLocked() || this.mUnderLowMemory) {
            this.mVolumeButtonClickedFlag = false;
        } else if (this.mAppController.getButtonManager().isMoreOptionsWrapperShow()) {
            this.mAppController.getButtonManager().hideMoreOptionsWrapper();
        } else if (this.mActivity.getStorageSpaceBytes() <= Storage.LOW_STORAGE_THRESHOLD_BYTES) {
            tag = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Not enough space or storage not ready. remaining=");
            stringBuilder.append(this.mActivity.getStorageSpaceBytes());
            Log.i(tag, stringBuilder.toString());
            this.mVolumeButtonClickedFlag = false;
        } else {
            tag = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onShutterButtonClick: mCameraState=");
            stringBuilder.append(this.mCameraState);
            stringBuilder.append(" mVolumeButtonClickedFlag=");
            stringBuilder.append(this.mVolumeButtonClickedFlag);
            Log.d(tag, stringBuilder.toString());
            this.mAppController.setShutterEnabled(false);
            int countDownDuration = this.mActivity.getSettingsManager().getInteger(this.mAppController.getCameraScope(), Keys.KEY_COUNTDOWN_DURATION).intValue();
            this.mTimerDuration = countDownDuration;
            boolean isCanCountDown = isCameraFrontFacing() ? CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_FRONTFACING_COUNT_DOWN, false) : CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_BACKFACING_COUNT_DOWN, false);
            if (isGestureShot) {
                if (countDownDuration <= 0) {
                    countDownDuration = 3;
                    this.mTimerDuration = 3;
                }
                isCanCountDown = true;
            }
            if (CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_COUNT_DOWN_ONLY_AUTO_MODE, false) && getModuleId() != this.mActivity.getResources().getInteger(R.integer.camera_mode_livebokeh)) {
                isCanCountDown = false;
            }
            if (!isCanCountDown) {
                focusAndCapture();
            } else if (countDownDuration > 0) {
                this.mFocusManager.removeFoucsMessages();
                this.mAppController.getCameraAppUI().transitionToCancel();
                this.mAppController.getCameraAppUI().hideModeOptions();
                this.mAppController.getButtonManager().setEffectsEnterToggleButton(8);
                this.mAppController.getButtonManager().setMoreEnterToggleButton(8);
                if (this.mActivity.getButtonManager().getEffectsEnterWrapperVisible() == 0) {
                    this.mLastMaskEnable = true;
                    this.mActivity.getButtonManager().setEffectWrapperVisible(8);
                }
                transitionToTimer(false);
                this.mUI.startCountdown(countDownDuration);
                if (countDownDuration == 8 && this.mFocusManager != null) {
                    this.mFocusManager.isEight();
                }
            } else {
                focusAndCapture();
            }
        }
    }

    public void onShutterButtonLongClick() {
        if (this.mAppController.getButtonManager().isMoreOptionsWrapperShow()) {
            this.mAppController.getButtonManager().hideMoreOptionsWrapper();
            this.mAppController.getLockEventListener().onIdle();
        } else if (!this.mIsBurstShotSupport) {
            ToastUtil.showToast(this.mActivity, (int) R.string.burst_shot_in_live_bokeh_toast, 0);
        } else if (!Keys.isShutterControlOn(this.mAppController.getSettingsManager())) {
        } else {
            if (Keys.isHdrOn(this.mAppController.getSettingsManager())) {
                ToastUtil.showToast(this.mActivity, this.mActivity.getString(R.string.open_hdr_refuse_longshot_toast), 1);
            } else if (this.mPaused || this.mCameraState == 4 || this.mCameraState == 0 || !this.mAppController.isShutterEnabled() || isImageCaptureIntent() || this.mUnderLowMemory) {
                this.mVolumeButtonClickedFlag = false;
            } else if (this.mSceneMode != SceneMode.AUTO || this.mZoomValue > 1.0f || Keys.isLowlightOn(this.mAppController.getSettingsManager()) || isFacebeautyEnabled()) {
                if (this.mZoomValue > 1.0f) {
                    ToastUtil.showToast(this.mActivity, this.mActivity.getString(R.string.livefilter_superzoom_refuse_longshot_toast), 0);
                }
                if (Keys.isLowlightOn(this.mAppController.getSettingsManager())) {
                    ToastUtil.showToast(this.mActivity, this.mActivity.getString(R.string.current_module_refuse_longshot_toast), 0);
                }
            } else {
                int countDownDuration = this.mActivity.getSettingsManager().getInteger(this.mAppController.getCameraScope(), Keys.KEY_COUNTDOWN_DURATION).intValue();
                if (isCountDownShow() && countDownDuration > 0) {
                    return;
                }
                Tag tag;
                StringBuilder stringBuilder;
                if (this.mActivity.getStorageSpaceBytes() <= Storage.LOW_STORAGE_THRESHOLD_BYTES) {
                    tag = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Not enough space or storage not ready. remaining=");
                    stringBuilder.append(this.mActivity.getStorageSpaceBytes());
                    Log.i(tag, stringBuilder.toString());
                    this.mVolumeButtonClickedFlag = false;
                    return;
                }
                tag = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("onShutterButtonClick: mCameraState=");
                stringBuilder.append(this.mCameraState);
                stringBuilder.append(" mVolumeButtonClickedFlag=");
                stringBuilder.append(this.mVolumeButtonClickedFlag);
                Log.d(tag, stringBuilder.toString());
                this.mAppController.setShutterEnabled(false);
                Log.w(TAG, "Longpress sucess and wait for burst shot");
                if (this.mCameraState == 5) {
                    clearFocusWithoutChangingState();
                }
                setCameraState(8);
                this.mBurstShotCheckQueue.clearCheckQueue();
                this.mBurstShotNotifyHelpTip = false;
                this.mUI.disableZoom();
                capture();
            }
        }
    }

    /* Access modifiers changed, original: protected */
    public void clearFocusWithoutChangingState() {
        this.mFocusManager.removeMessages();
        this.mUI.clearFocus();
        this.mCameraDevice.cancelAutoFocus();
        this.mUI.clearEvoPendingUI();
        if (this.mEvoFlashLock != null) {
            this.mAppController.getButtonManager().enableButtonWithToken(0, this.mEvoFlashLock.intValue());
            this.mEvoFlashLock = null;
        }
        this.mFocusManager.setAeAwbLock(false);
        setCameraParameters(4);
    }

    private void focusAndCapture() {
        if (this.mFocusManager != null) {
            if (this.mSceneMode == SceneMode.HDR) {
                this.mUI.setSwipingEnabled(false);
            }
            if (this.mFocusManager.isFocusingSnapOnFinish() || this.mCameraState == 3 || this.mCameraState == 7) {
                if (!this.mIsImageCaptureIntent) {
                    this.mSnapshotOnIdle = true;
                }
                return;
            }
            this.mSnapshotOnIdle = false;
            if (this.mCameraState != 5) {
                this.mFocusManager.focusAndCapture(this.mCameraSettings.getCurrentFocusMode());
            } else {
                capture();
            }
        }
    }

    /* Access modifiers changed, original: protected */
    public void readCameraInitialParameters() {
        if (this.mCameraDevice != null) {
            int maxEvo = this.mCameraDevice.getCapabilities().getMaxExposureCompensation();
            int minEvo = this.mCameraDevice.getCapabilities().getMinExposureCompensation();
            Log.w(TAG, String.format("max Evo is %d and min Evo is %d", new Object[]{Integer.valueOf(maxEvo), Integer.valueOf(minEvo)}));
            this.mUI.parseEvoBound(maxEvo, minEvo);
            initializeFocusModeSettings();
        }
    }

    /* Access modifiers changed, original: protected */
    public void initializeFocusModeSettings() {
    }

    public void onRemainingSecondsChanged(int remainingSeconds) {
        int mode = this.mAudioManager.getRingerMode();
        boolean isShutterSoundOn = Keys.isShutterSoundOn(this.mAppController.getSettingsManager());
        if (remainingSeconds == 1 && mode == 2 && isShutterSoundOn) {
            this.mCountdownSoundPlayer.play(R.raw.timer_final_second, 0.6f);
        } else if ((remainingSeconds == 2 || remainingSeconds == 3) && mode == 2 && isShutterSoundOn) {
            this.mCountdownSoundPlayer.play(R.raw.timer_increment, 0.6f);
        }
    }

    public void onCountDownFinished() {
        this.mFocusManager.noEight();
        this.mAppController.getCameraAppUI().transitionToCapture();
        this.mAppController.getCameraAppUI().showModeOptions();
        transitionToTimer(true);
        if (this.mLastMaskEnable) {
            this.mLastMaskEnable = false;
            this.mActivity.getButtonManager().setEffectWrapperVisible(0);
        }
        if (!this.mPaused) {
            focusAndCapture();
        }
    }

    public void resume() {
        if (this.mActivity != null) {
            Log.d(TAG, "[LiveBokehMoudle] resume onLastMediaDataUpdated");
            this.mActivity.onLastMediaDataUpdated();
        }
        if (Util.TimesofLivebokeh < 6) {
            Util.TimesofLivebokeh++;
        }
        this.mPaused = false;
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("KPI Track photo resume E ");
        stringBuilder.append(Util.TimesofLivebokeh);
        Log.w(tag, stringBuilder.toString());
        this.mSoundPlayer.loadSound(R.raw.camera_burst);
        this.mSoundPlayer.loadSound(R.raw.shutter);
        this.mSoundPlayer.loadSound(R.raw.shutter_sound_2);
        this.mCountdownSoundPlayer.loadSound(R.raw.timer_final_second);
        this.mCountdownSoundPlayer.loadSound(R.raw.timer_increment);
        if (this.mFocusManager != null) {
            this.mAppController.addPreviewAreaSizeChangedListener(this.mFocusManager);
        }
        this.mAppController.addPreviewAreaSizeChangedListener(this.mUI);
        if (this.mActivity.getCameraProvider() != null) {
            this.mJpegPictureCallbackTime = 0;
            this.mZoomValue = 1.0f;
            this.mOnResumeTime = SystemClock.uptimeMillis();
            checkDisplayRotation();
            if (this.mFirstTimeInitialized) {
                initializeSecondTime();
            } else {
                this.mHandler.sendEmptyMessage(1);
            }
            Sensor gsensor = this.mSensorManager.getDefaultSensor(1);
            if (gsensor != null) {
                this.mSensorManager.registerListener(this, gsensor, 3);
            }
            Sensor msensor = this.mSensorManager.getDefaultSensor(2);
            if (msensor != null) {
                this.mSensorManager.registerListener(this, msensor, 3);
            }
            getServices().getRemoteShutterListener().onModuleReady(this, getRemodeShutterIcon());
            SessionStatsCollector.instance().sessionActive(true);
            this.mIsGlMode = this.mActivity.getCameraAppUI().getBeautyEnable();
            if (this.mIsGlMode && this.mCameraId == 1) {
                setPreviewBytesBack(this.mActivity.getCameraAppUI().getPreviewCallback());
                this.mActivity.getCameraAppUI().setOrientation(this.mCameraId);
                BeaurifyJniSdk.preViewInstance().nativeDisablePackage();
                if (this.mActivity.getCameraAppUI().getBeautyEnable()) {
                    if (com.hmdglobal.app.camera.util.ResolutionUtil.getRightResolutionById(getModuleId(), this.mCameraId, this.mActivity)) {
                        this.mActivity.getCameraAppUI().freezeGlSurface();
                    }
                    this.mActivity.getCameraAppUI().showOrHideGLSurface(true);
                    this.mActivity.getButtonManager().setSeekbarProgress((int) this.mActivity.getCameraAppUI().getBeautySeek());
                    updateBeautySeek(this.mActivity.getCameraAppUI().getBeautySeek() / 20.0f);
                    this.mActivity.getButtonManager().initEffectById(getModuleId());
                }
            } else {
                if (this.mCameraId == 1) {
                    this.mActivity.getButtonManager().hideEffectsContainerWrapper();
                }
                this.mActivity.getCameraAppUI().showOrHideGLSurface(false);
            }
            if (this.mCameraId == 0) {
                this.mCameraId = 3;
            }
            requestCameraOpen();
            Log.w(TAG, "KPI Track photo resume X");
            this.mActivity.getCameraAppUI().hideImageCover();
            setSeekProgress();
        }
    }

    private void setSeekProgress() {
        if (this.mCameraId == 1) {
            this.mActivity.getButtonManager().setLiveBokehLevelSeeker(CameraAgent.mLiveBolkenFrontLevel);
        } else {
            this.mActivity.getButtonManager().setLiveBokehLevelSeeker(CameraAgent.mLiveBolkenRearLevel);
        }
    }

    private void updateBeautySeek(float chooseValue) {
        BeaurifyJniSdk.preViewInstance().nativeSetBeautyParam(3, chooseValue);
        BeaurifyJniSdk.preViewInstance().nativeSetBeautyParam(2, chooseValue);
        BeaurifyJniSdk.preViewInstance().nativeSetBeautyParam(4, chooseValue);
        BeaurifyJniSdk.preViewInstance().nativeSetBeautyParam(5, chooseValue);
        BeaurifyJniSdk.preViewInstance().nativeSetBeautyParam(1, chooseValue);
    }

    public void preparePause() {
        if (this.mCameraState == 1) {
            stopPreview();
        }
    }

    /* Access modifiers changed, original: protected */
    public int getRemodeShutterIcon() {
        return CameraUtil.getCameraShutterNormalStateIconId(this.mAppController.getCurrentModuleIndex(), this.mAppController.getAndroidContext());
    }

    /* Access modifiers changed, original: protected */
    public boolean isCameraFrontFacing() {
        if (this.mAppController.getCameraProvider().getCharacteristics(this.mCameraId) != null) {
            return this.mAppController.getCameraProvider().getCharacteristics(this.mCameraId).isFacingFront();
        }
        return false;
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
    public boolean isVisidonModeEnabled() {
        return false;
    }

    /* Access modifiers changed, original: protected */
    public boolean isHdrShow() {
        return false;
    }

    /* Access modifiers changed, original: protected */
    public boolean isFlashShow() {
        return false;
    }

    /* Access modifiers changed, original: protected */
    public boolean isWrapperButtonShow() {
        return false;
    }

    /* Access modifiers changed, original: protected */
    public boolean isCountDownShow() {
        boolean isCanCountDown = isCameraFrontFacing() ? CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_FRONTFACING_COUNT_DOWN, false) : CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_BACKFACING_COUNT_DOWN, false);
        if (!CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_COUNT_DOWN_ONLY_AUTO_MODE, false) || getModuleId() == this.mActivity.getResources().getInteger(R.integer.camera_mode_livebokeh) || getModuleId() == this.mActivity.getResources().getInteger(R.integer.camera_mode_photo) || getModuleId() == this.mActivity.getResources().getInteger(R.integer.camera_mode_square)) {
            return isCanCountDown;
        }
        return false;
    }

    private void initializeFocusManager() {
        if (this.mFocusManager != null) {
            this.mUI.clearFocus();
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

    private boolean isResumeFromLockscreen() {
        String action = this.mActivity.getIntent().getAction();
        return "android.media.action.STILL_IMAGE_CAMERA".equals(action) || CameraActivity.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE.equals(action);
    }

    public boolean isPaused() {
        return this.mPaused;
    }

    public void pause() {
        Log.v(TAG, "KPI photo pause E");
        this.mPaused = true;
        if (!(this.mCameraSettings == null || this.mCameraDevice == null)) {
            this.mCameraDevice.setPreviewBolkenLevel(-1);
            this.mCameraDevice.applySettings(this.mCameraSettings);
        }
        if (this.mCameraState == 8 || this.mCameraState == 9 || this.mCameraState == 10) {
            stopBurst();
        }
        dismissSavingHint();
        dismissOptimisingPhotoHint();
        this.mProgressDialog = null;
        this.mUI.updateBurstCount(0);
        if (this.mUI.isGestureViewShow()) {
            this.mHandler.removeMessages(4);
            this.mUI.hideGesture();
        }
        if (this.mHandler.hasMessages(5)) {
            this.mHandler.removeMessages(5);
        }
        firstFrame = true;
        getServices().getRemoteShutterListener().onModuleExit();
        SessionStatsCollector.instance().sessionActive(false);
        Sensor gsensor = this.mSensorManager.getDefaultSensor(1);
        if (gsensor != null) {
            this.mSensorManager.unregisterListener(this, gsensor);
        }
        Sensor msensor = this.mSensorManager.getDefaultSensor(2);
        if (msensor != null) {
            this.mSensorManager.unregisterListener(this, msensor);
        }
        if (!(this.mCameraDevice == null || this.mCameraState == 0)) {
            this.mCameraDevice.cancelAutoFocus();
        }
        stopPreview();
        cancelCountDown();
        this.mSoundPlayer.unloadSound(R.raw.camera_burst);
        this.mSoundPlayer.unloadSound(R.raw.shutter);
        this.mSoundPlayer.unloadSound(R.raw.shutter_sound_2);
        this.mCountdownSoundPlayer.unloadSound(R.raw.timer_final_second);
        this.mCountdownSoundPlayer.unloadSound(R.raw.timer_increment);
        this.mNamedImages = null;
        if (ProcessingMediaManager.getInstance(this.mActivity).getProcessingMedia().isEmpty()) {
            this.mHandler.removeCallbacksAndMessages(null);
        }
        if (this.mMotionManager != null) {
            this.mMotionManager.removeListener(this.mFocusManager);
            this.mMotionManager = null;
        }
        closeCamera();
        this.mActivity.closeCameraSybc();
        this.mActivity.enableKeepScreenOn(false);
        this.mUI.onPause();
        this.mPendingSwitchCameraId = -1;
        if (this.mFocusManager != null) {
            this.mUI.clearFocus();
            this.mFocusManager.removeMessages();
        }
        getServices().getMemoryManager().removeListener(this);
        this.mAppController.removePreviewAreaSizeChangedListener(this.mFocusManager);
        this.mAppController.removePreviewAreaSizeChangedListener(this.mUI);
        this.mActivity.getSettingsManager().removeListener(this);
        ToastUtil.cancelToast();
        this.mActivity.getButtonManager().hideEffectsContainerWrapper();
        Log.w(TAG, "KPI photo pause X");
    }

    public void destroy() {
        this.mJpegImageData = null;
        if (this.mUI != null) {
            this.mUI.clearReviewImage();
        }
        this.mSoundPlayer.release();
        this.mCountdownSoundPlayer.release();
    }

    public void onLayoutOrientationChanged(boolean isLandscape) {
        setDisplayOrientation();
    }

    public void updateCameraOrientation() {
        if (this.mDisplayRotation != CameraUtil.getDisplayRotation(this.mActivity)) {
            setDisplayOrientation();
        }
    }

    private boolean canTakePicture() {
        return isCameraIdle() && this.mActivity.getStorageSpaceBytes() > Storage.LOW_STORAGE_THRESHOLD_BYTES;
    }

    public void autoFocus() {
        if (this.mCameraDevice != null && !isInBurstshot()) {
            if (this.mFocusManager.getFocusAreas() == null) {
                this.mAutoFocusCallback.onAutoFocus(true, this.mCameraDevice);
                return;
            }
            Log.v(TAG, "Starting auto focus");
            this.mFocusStartTime = System.currentTimeMillis();
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
        if (isInBurstshot()) {
            this.mUI.clearEvoPendingUI();
            if (this.mEvoFlashLock != null) {
                this.mAppController.getButtonManager().enableButtonWithToken(0, this.mEvoFlashLock.intValue());
                this.mEvoFlashLock = null;
            }
        }
        if (this.mCameraState == 5 || isInBurstshot()) {
            return false;
        }
        Log.w(TAG, "cancel autofocus");
        this.mCameraDevice.cancelAutoFocus();
        this.mFocusManager.setAeAwbLock(false);
        this.mUI.clearEvoPendingUI();
        if (this.mEvoFlashLock != null) {
            this.mAppController.getButtonManager().enableButtonWithToken(0, this.mEvoFlashLock.intValue());
            this.mEvoFlashLock = null;
        }
        if (this.mCameraState != 0) {
            setCameraState(1);
        }
        setCameraParameters(4);
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
            this.mFocusManager.setAeAwbLock(true);
            this.mFocusManager.keepFocusFrame();
            setExposureCompensation(index, false);
            setCameraParameters(4);
        }
    }

    /* JADX WARNING: Missing block: B:39:0x0075, code skipped:
            return;
     */
    public void onSingleTapUp(android.view.View r4, int r5, int r6) {
        /*
        r3 = this;
        r0 = r3.mPaused;
        if (r0 != 0) goto L_0x0075;
    L_0x0004:
        r0 = r3.mCameraDevice;
        if (r0 == 0) goto L_0x0075;
    L_0x0008:
        r0 = r3.mFirstTimeInitialized;
        if (r0 == 0) goto L_0x0075;
    L_0x000c:
        r0 = r3.mCameraState;
        r1 = 3;
        if (r0 == r1) goto L_0x0075;
    L_0x0011:
        r0 = r3.mCameraState;
        r1 = 7;
        if (r0 == r1) goto L_0x0075;
    L_0x0016:
        r0 = r3.mCameraState;
        r1 = 8;
        if (r0 == r1) goto L_0x0075;
    L_0x001c:
        r0 = r3.mCameraState;
        r1 = 9;
        if (r0 == r1) goto L_0x0075;
    L_0x0022:
        r0 = r3.mCameraState;
        r1 = 10;
        if (r0 == r1) goto L_0x0075;
    L_0x0028:
        r0 = r3.mCameraState;
        r1 = 4;
        if (r0 == r1) goto L_0x0075;
    L_0x002d:
        r0 = r3.mCameraState;
        if (r0 != 0) goto L_0x0032;
    L_0x0031:
        goto L_0x0075;
    L_0x0032:
        r0 = r3.mIsInIntentReviewUI;
        if (r0 == 0) goto L_0x0037;
    L_0x0036:
        return;
    L_0x0037:
        r0 = r3.isCameraFrontFacing();
        if (r0 == 0) goto L_0x0054;
    L_0x003d:
        r0 = r3.mAppController;
        r0 = r0.getHelpTipsManager();
        if (r0 == 0) goto L_0x0053;
    L_0x0045:
        r1 = r0.isHelpTipShowExist();
        if (r1 == 0) goto L_0x0053;
    L_0x004b:
        r1 = TAG;
        r2 = "helptip exists and cancels shutterbutton click";
        com.hmdglobal.app.camera.debug.Log.e(r1, r2);
        return;
    L_0x0053:
        return;
    L_0x0054:
        r0 = r3.mUI;
        r0.clearEvoPendingUI();
        r0 = r3.mUI;
        r1 = (float) r5;
        r2 = (float) r6;
        r0.initEvoSlider(r1, r2);
        r0 = r3.mFocusAreaSupported;
        if (r0 != 0) goto L_0x0069;
    L_0x0064:
        r0 = r3.mMeteringAreaSupported;
        if (r0 != 0) goto L_0x0069;
    L_0x0068:
        return;
    L_0x0069:
        r0 = r3.mFocusManager;
        r1 = 0;
        r0.setAeAwbLock(r1);
        r0 = r3.mFocusManager;
        r0.onSingleTapUp(r5, r6);
        return;
    L_0x0075:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.LiveBokehModule.onSingleTapUp(android.view.View, int, int):void");
    }

    public boolean onBackPressed() {
        return this.mUI.onBackPressed();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode != 27) {
            if (!(keyCode == 80 || keyCode == CameraUtil.BOOM_KEY)) {
                switch (keyCode) {
                    case 23:
                        if (this.mFirstTimeInitialized && event.getRepeatCount() == 0) {
                            onShutterButtonFocus(true);
                        }
                        return true;
                    case 24:
                    case 25:
                        if (isVolumeKeySystemBehaving()) {
                            return false;
                        }
                        break;
                    default:
                        return false;
                }
            }
            if (this.mFirstTimeInitialized && !this.mActivity.getCameraAppUI().isInIntentReview()) {
                if (event.getRepeatCount() == 0) {
                    this.mAppController.setShutterPress(true);
                    onShutterButtonFocus(true);
                }
                if (event.isLongPress() && !this.mIsImageCaptureIntent) {
                    this.mCameraKeyLongPressed = true;
                    this.mAppController.setShutterPress(true);
                    onShutterButtonLongClick();
                }
            }
            return true;
        }
        if (this.mFirstTimeInitialized && event.getRepeatCount() == 0) {
            onShutterButtonClick();
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
                        if (isVolumeKeySystemBehaving()) {
                            return false;
                        }
                        break;
                    default:
                        return false;
                }
            }
            this.mAppController.setShutterPress(false);
            if (!(!this.mFirstTimeInitialized || this.mActivity.getCameraAppUI().isInIntentReview() || this.mCameraKeyLongPressed)) {
                if (this.mUI.isCountingDown()) {
                    Tag tag = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("mUI.isCountingDown()");
                    stringBuilder.append(this.mUI.isCountingDown());
                    Log.d(tag, stringBuilder.toString());
                } else {
                    this.mVolumeButtonClickedFlag = true;
                    onShutterButtonClick();
                    helpTipsManager = this.mAppController.getHelpTipsManager();
                    if (helpTipsManager != null && helpTipsManager.isHelpTipShowExist()) {
                        helpTipsManager.onBoomKeySingleShotResponse();
                    }
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
        if (this.mFirstTimeInitialized) {
            onShutterButtonFocus(false);
        }
        return true;
    }

    /* Access modifiers changed, original: protected */
    public boolean isVolumeKeySystemBehaving() {
        return false;
    }

    private void closeCamera() {
        this.mIsCameraOpened = false;
        this.mUI.clearEvoPendingUI();
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
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setPostGestureRotation (screen:preview) ");
        stringBuilder.append(this.mDisplayRotation);
        stringBuilder.append(":");
        stringBuilder.append(this.mDisplayOrientation);
        Log.v(tag, stringBuilder.toString());
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

    private void lightlyRestartPreview() {
        if (!this.mPaused) {
            this.mAppController.setShutterEnabled(false);
            this.mCameraDevice.stopPreview();
            CameraStartPreviewCallback startPreviewCallback = new CameraStartPreviewCallback() {
                public void onPreviewStarted() {
                    LiveBokehModule.this.mFocusManager.onPreviewStarted();
                    LiveBokehModule.this.onPreviewStarted();
                }
            };
            if (GservicesHelper.useCamera2ApiThroughPortabilityLayer(this.mActivity)) {
                this.mCameraDevice.startPreview();
                startPreviewCallback.onPreviewStarted();
                return;
            }
            this.mCameraDevice.startPreviewWithCallback(new Handler(Looper.getMainLooper()), startPreviewCallback);
        }
    }

    private boolean checkPreviewPreconditions() {
        if (this.mPaused) {
            return false;
        }
        if (this.mCameraDevice == null) {
            Log.w(TAG, "startPreview: camera device not ready yet.");
            return false;
        } else if (this.mCameraPreviewParamsReady) {
            return true;
        } else {
            Log.w(TAG, "startPreview: parameters for preview is not ready.");
            return false;
        }
    }

    /* Access modifiers changed, original: protected */
    public boolean isEnableGestureRecognization() {
        return false;
    }

    private void startPreview() {
        if (this.mCameraDevice == null) {
            Log.i(TAG, "attempted to start preview before camera device");
        } else if (checkPreviewPreconditions()) {
            setDisplayOrientation();
            if (!this.mSnapshotOnIdle) {
                if (this.mFocusManager.getFocusMode(this.mCameraSettings.getCurrentFocusMode()) == FocusMode.CONTINUOUS_PICTURE && this.mCameraState != 5) {
                    this.mCameraDevice.cancelAutoFocus();
                }
                this.mFocusManager.setAeAwbLock(false);
            }
            firstFrame = false;
            updateParametersPictureSize();
            boolean z = true;
            updateLiveBokehEnabled(true);
            if (this.mCameraId == 1) {
                updateLiveBokehLevel(CameraAgent.mLiveBolkenFrontLevel);
            } else {
                updateLiveBokehLevel(CameraAgent.mLiveBolkenRearLevel);
            }
            if (this.mCameraId != 1 && Util.TimesofLivebokeh <= 5) {
                this.mHandler.removeMessages(6);
                Message msg = Message.obtain();
                msg.what = 6;
                this.mHandler.sendMessageDelayed(msg, 3000);
            }
            setCameraParameters(-1);
            if (!this.mActivity.getCameraProvider().isBoostPreview()) {
                CameraProxy cameraProxy = this.mCameraDevice;
                CameraAppUI cameraAppUI = this.mActivity.getCameraAppUI();
                if (!(this.mIsGlMode && this.mCameraId == 1)) {
                    z = false;
                }
                cameraProxy.setPreviewTexture(cameraAppUI.getSurfaceTexture(z));
            }
            this.mAppController.getCameraAppUI().updateHdrViewVisable(false);
            Log.i(TAG, "startPreview");
            CameraStartPreviewCallback startPreviewCallback = new CameraStartPreviewCallback() {
                public void onPreviewStarted() {
                    LiveBokehModule.this.mFocusManager.onPreviewStarted();
                    LiveBokehModule.this.onPreviewStarted();
                    LiveBokehModule.this.mHandler.postDelayed(new Runnable() {
                        public void run() {
                            if (LiveBokehModule.this.mIsGlMode && LiveBokehModule.this.mCameraId == 1) {
                                LiveBokehModule.this.setPreviewBytesBack(LiveBokehModule.this.mActivity.getCameraAppUI().getPreviewCallback());
                            }
                            LiveBokehModule.this.setPreviewCallback();
                        }
                    }, 500);
                    SessionStatsCollector.instance().previewActive(true);
                    if (LiveBokehModule.this.mSnapshotOnIdle) {
                        Log.v(LiveBokehModule.TAG, "postSnapRunnable");
                        LiveBokehModule.this.mHandler.post(LiveBokehModule.this.mDoSnapRunnable);
                    }
                }
            };
            if (GservicesHelper.useCamera2ApiThroughPortabilityLayer(this.mActivity) && this.mCameraId != 3) {
                this.mCameraDevice.startPreview();
                startPreviewCallback.onPreviewStarted();
            } else if (this.mActivity.getCameraProvider().isBoostPreview()) {
                this.mCameraDevice.startPreviewWithCallback(new Handler(Looper.getMainLooper()), startPreviewCallback);
            } else {
                Log.w(TAG, "KPI normal start preview");
                this.mCameraDevice.startPreviewWithCallback(new Handler(Looper.getMainLooper()), startPreviewCallback);
            }
            if (isEnableGestureRecognization()) {
                this.mCameraDevice.setGestureCallback(this.mHandler, new CameraGDCallBack() {
                    public void onGesture() {
                        LiveBokehModule.this.mHandler.post(new Runnable() {
                            public void run() {
                                if (LiveBokehModule.this.mIsInIntentReviewUI || LiveBokehModule.this.mUI.isCountingDown()) {
                                    Log.v(LiveBokehModule.TAG, "in intent review UI or counting down");
                                    return;
                                }
                                if (LiveBokehModule.this.mCameraState == 1) {
                                    LiveBokehModule.this.doShutterButtonClick(true);
                                }
                            }
                        });
                    }
                });
                this.mCameraDevice.startGestureDetection();
            }
        }
    }

    private void updateLiveBokehEnabled(boolean livebokehenabled) {
        this.mCameraSettings.setLiveBokehEnabled(livebokehenabled);
    }

    private void updateLiveBokehLevel(int level) {
        this.mCameraSettings.setLiveBokehLevel(level);
    }

    private void setPreviewCallback() {
        if (this.mCameraDevice != null) {
            this.mCameraDevice.setPreviewDataCallback(this.mHandler, new CameraPreviewDataCallback() {
                public void onPreviewFrame(byte[] data, CameraProxy camera) {
                    if (LiveBokehModule.this.mPreviewBytesBack != null) {
                        android.util.Log.e("+++++++++++++++++", "mPreviewBytesBack is not null ....");
                        LiveBokehModule.this.mPreviewBytesBack.onPreviewData(data, null);
                    }
                }
            });
            this.mCameraDevice.setPreviewResultCallback(this.mHandler, new CameraPreviewResultCallback() {
                public void onCaptureComplete(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    final Integer iso = (Integer) result.get(TotalCaptureResult.SENSOR_SENSITIVITY);
                    result.get(TotalCaptureResult.SENSOR_EXPOSURE_TIME);
                    LiveBokehModule.this.mActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            if (iso == null || iso.intValue() != CameraConstants.MAX_ISO_VALUE) {
                                LiveBokehModule.this.mAppController.getCameraAppUI().updatePromptViewVisable(false);
                                return;
                            }
                            LiveBokehModule.this.mAppController.getCameraAppUI().updatePromptViewVisable(true);
                            LiveBokehModule.this.mAppController.getCameraAppUI().setPromptMessage(LiveBokehModule.this.mActivity.getString(R.string.more_light_required));
                        }
                    });
                    LiveBokehModule.this.mPreviewResult = result;
                }
            });
            this.mCameraDevice.setCaptureResultCallback(this.mHandler, new CaptureComplete(this, null));
        }
    }

    public void stopPreview() {
        if (!(this.mCameraDevice == null || this.mCameraState == 0)) {
            Log.i(TAG, "stopPreview");
            stopFaceDetection();
            this.mCameraDevice.stopPreview();
            this.mActivity.clearBoost();
            if (isEnableGestureRecognization()) {
                this.mCameraDevice.stopGestureDetection();
            }
        }
        setCameraState(0);
        if (this.mFocusManager != null) {
            this.mFocusManager.onPreviewStopped();
        }
        SessionStatsCollector.instance().previewActive(false);
        this.mHandler.postDelayed(new Runnable() {
            public void run() {
                LiveBokehModule.this.mAppController.getCameraAppUI().updatePromptViewVisable(false);
            }
        }, 400);
    }

    public void onSettingChanged(SettingsManager settingsManager, String key) {
        if (!this.isOptimizeSwitchCamera || !key.equals(Keys.KEY_SWITCH_CAMERA)) {
            if (key.equals(Keys.KEY_FLASH_MODE)) {
                updateParametersFlashMode();
            }
            if (key.equals(Keys.KEY_CAMERA_HDR)) {
                if (Keys.isHdrOn(settingsManager) && isHdrShow()) {
                    settingsManager.set(this.mAppController.getCameraScope(), Keys.KEY_SCENE_MODE, this.mCameraCapabilities.getStringifier().stringify(SceneMode.HDR));
                } else {
                    settingsManager.set(this.mAppController.getCameraScope(), Keys.KEY_SCENE_MODE, this.mCameraCapabilities.getStringifier().stringify(SceneMode.AUTO));
                }
            }
            updateParametersSceneMode();
            updateVisidionMode();
            if (this.mCameraDevice != null) {
                this.mCameraDevice.applySettings(this.mCameraSettings);
            }
        }
    }

    private void updateCameraParametersInitialize() {
        int[] fpsRange = CameraUtil.getPhotoPreviewFpsRange(this.mCameraCapabilities);
        if (fpsRange != null && fpsRange.length > 0) {
            this.mCameraSettings.setPreviewFpsRange(fpsRange[0], fpsRange[1]);
        }
        this.mCameraSettings.setRecordingHintEnabled(false);
        if (this.mCameraCapabilities.supports(Feature.VIDEO_STABILIZATION)) {
            this.mCameraSettings.setVideoStabilization(false);
        }
    }

    private void updateCameraParametersZoom() {
        if (this.mCameraCapabilities.supports(Feature.ZOOM)) {
            this.mCameraSettings.setZoomRatio(this.mZoomValue);
        }
    }

    @TargetApi(16)
    private void setAutoExposureLockIfSupported() {
        if (this.mAeLockSupported) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("lock ae awb ? ");
            stringBuilder.append(this.mFocusManager.getAeAwbLock());
            Log.w(tag, stringBuilder.toString());
            this.mCameraSettings.setAutoExposureLock(this.mFocusManager.getAeAwbLock());
        }
    }

    private void unlockAutoExposureLockIfSupported() {
        if (this.mAeLockSupported) {
            this.mCameraSettings.setAutoExposureLock(this.mFocusManager.getAeAwbLock());
        }
    }

    @TargetApi(16)
    private void setAutoWhiteBalanceLockIfSupported() {
        if (this.mAwbLockSupported) {
            this.mCameraSettings.setAutoWhiteBalanceLock(this.mFocusManager.getAeAwbLock());
        }
    }

    private void setFocusAreasIfSupported() {
        if (this.mFocusAreaSupported) {
            this.mCameraSettings.setFocusAreas(this.mFocusManager.getFocusAreas());
        }
    }

    private void setMeteringAreasIfSupported() {
        if (this.mMeteringAreaSupported) {
            this.mCameraSettings.setMeteringAreas(this.mFocusManager.getMeteringAreas());
        }
    }

    public boolean isZslOn() {
        return true;
    }

    private void updateCameraParametersPreference() {
        if (this.mCameraDevice != null) {
            if (this.mCameraCapabilities.isZslSupported()) {
                this.mCameraSettings.isZslOn = isZslOn();
            }
            this.mCameraSettings.setHsr(ExtendKey.FLIP_MODE_OFF);
            setAutoExposureLockIfSupported();
            setAutoWhiteBalanceLockIfSupported();
            setFocusAreasIfSupported();
            setMeteringAreasIfSupported();
            if (this.mCameraState != 5) {
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("focus mode is ");
                stringBuilder.append(this.mFocusManager.getFocusMode(this.mCameraSettings.getCurrentFocusMode()));
                Log.w(tag, stringBuilder.toString());
                updateParametersFocusMode();
                SessionStatsCollector.instance().autofocusActive(this.mFocusManager.getFocusMode(this.mCameraSettings.getCurrentFocusMode()) == FocusMode.CONTINUOUS_PICTURE);
            }
            updateParametersPictureQuality();
            updateParametersExposureCompensation();
            updateParametersSceneMode();
            updateParametersAntibanding();
            updateFrontPhotoflipMode();
            updateParametersSaveDng();
            updateParametersAddWaterMark();
            if (this.mContinuousFocusSupported && ApiHelper.HAS_AUTO_FOCUS_MOVE_CALLBACK) {
                updateAutoFocusMoveCallback();
            }
        }
    }

    public void updateFaceBeautyWhenFrameReady() {
        this.mHandler.sendEmptyMessageDelayed(5, 250);
    }

    private final void updateVisidionMode() {
        if (isVisidonModeEnabled()) {
            SettingsManager settingsManager = this.mAppController.getSettingsManager();
            if (isCameraFrontFacing()) {
                int skinSmoothing = this.mActivity.getSettingsManager().getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_FACEBEAUTY_SKIN_SMOOTHING, Integer.valueOf(CustomUtil.getInstance().getInt(CustomFields.DEF_CAMERA_SKIN_SMOOTHING, 50))).intValue();
                if (!this.isOptimizeSwitchCamera) {
                    this.mCameraSettings.setFaceBeauty(Keys.isFacebeautyOn(settingsManager), (skinSmoothing * 90) / 100);
                } else if (firstFrame) {
                    this.mCameraSettings.setFaceBeauty(Keys.isFacebeautyOn(settingsManager), (skinSmoothing * 90) / 100);
                } else {
                    this.mCameraSettings.setFaceBeauty(false, 50);
                }
                this.mCameraSettings.setLowLight(false);
            } else {
                if (Keys.isHdrOn(settingsManager)) {
                    this.mCameraSettings.setLowLight(false);
                } else if (Keys.isLowlightOn(settingsManager)) {
                    this.mCameraSettings.setLowLight(true);
                } else {
                    this.mCameraSettings.setLowLight(false);
                }
                this.mCameraSettings.setFaceBeauty(false, 50);
            }
        }
    }

    private void updateZoomValue(float ratio) {
        if (this.mCameraSettings != null) {
            this.mCameraSettings.setZoomRatio(ratio);
        }
        this.mUI.resetZoombar();
    }

    /* Access modifiers changed, original: protected */
    public FocusMode getOverrideFocusMode() {
        return null;
    }

    /* Access modifiers changed, original: protected */
    public void updateParametersPictureSize() {
        if (this.mCameraDevice == null) {
            Log.w(TAG, "attempting to set picture size without camera device");
            return;
        }
        String pictureSizeKey;
        Size size;
        Tag tag;
        StringBuilder stringBuilder;
        this.mCameraSettings.setSizesLocked(false);
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        if (isCameraFrontFacing()) {
            pictureSizeKey = Keys.KEY_PICTURE_SIZE_FRONT;
        } else {
            pictureSizeKey = Keys.KEY_PICTURE_SIZE_BACK;
        }
        String defaultPicSize = SettingsUtil.getDefaultPictureSize(isCameraFrontFacing());
        Tag tag2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("defaultPicSize = ");
        stringBuilder2.append(defaultPicSize);
        Log.d(tag2, stringBuilder2.toString());
        tag2 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("pictureSizeKey = ");
        stringBuilder2.append(pictureSizeKey);
        Log.d(tag2, stringBuilder2.toString());
        String pictureSize = settingsManager.getString(SettingsManager.SCOPE_GLOBAL, pictureSizeKey, defaultPicSize);
        Tag tag3 = TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("pictureSize = ");
        stringBuilder3.append(pictureSize);
        Log.d(tag3, stringBuilder3.toString());
        if (this.mCameraId == 1) {
            size = SettingsUtil.sizeFromString(pictureSize);
        } else {
            size = SettingsUtil.getBokehPhotoSize(this.mActivity, pictureSize);
        }
        Tag tag4 = TAG;
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("size = ");
        stringBuilder4.append(size);
        Log.d(tag4, stringBuilder4.toString());
        this.mCameraSettings.setPhotoSize(size);
        if (ApiHelper.IS_NEXUS_5) {
            if (ResolutionUtil.NEXUS_5_LARGE_16_BY_9.equals(pictureSize)) {
                this.mShouldResizeTo16x9 = true;
            } else {
                this.mShouldResizeTo16x9 = false;
            }
        }
        Size optimalSize = CameraUtil.getOptimalPreviewSize(this.mActivity, this.mCameraCapabilities.getSupportedPreviewSizes(), ((double) size.width()) / ((double) size.height()));
        Size original = this.mCameraSettings.getCurrentPreviewSize();
        if (this.mCameraId == 1) {
            optimalSize = SettingsUtil.getBokehPreviewSize(pictureSize, true);
        } else {
            optimalSize = SettingsUtil.getBokehPreviewSize(pictureSize, false);
        }
        this.mActivity.getCameraAppUI().setSurfaceHeight(optimalSize.height());
        this.mActivity.getCameraAppUI().setSurfaceWidth(optimalSize.width());
        this.mUI.setCaptureSize(optimalSize);
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
        this.mCameraSettings.setSizesLocked(true);
    }

    private void updateParametersExposureCompensation() {
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        if (settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_EXPOSURE_COMPENSATION_ENABLED)) {
            int value = settingsManager.getInteger(this.mAppController.getCameraScope(), Keys.KEY_EXPOSURE).intValue();
            int max = this.mCameraCapabilities.getMaxExposureCompensation();
            if (value < this.mCameraCapabilities.getMinExposureCompensation() || value > max) {
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("invalid exposure range: ");
                stringBuilder.append(value);
                Log.w(tag, stringBuilder.toString());
                return;
            }
            this.mCameraSettings.setExposureCompensationIndex(value);
            return;
        }
        setExposureCompensation(0);
    }

    private void updateParametersPictureQuality() {
        this.mCameraSettings.setPhotoJpegCompressionQuality(CameraProfile.getJpegEncodingQualityParameter(0, 2));
    }

    private void updateParametersAntibanding() {
        this.mCameraSettings.setAntibanding(Keys.getAntibandingValue(this.mActivity.getSettingsManager()));
    }

    private void updateParametersSaveDng() {
        boolean enable = this.mActivity.getSettingsManager().getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_RAW_FILE);
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("raw data saving enabled:");
        stringBuilder.append(enable);
        Log.d(tag, stringBuilder.toString());
        this.mCameraSettings.setSaveDngEnabled(false);
    }

    private void updateParametersAddWaterMark() {
        boolean enable = this.mActivity.getSettingsManager().getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_WATER_MARK);
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("add water mark enabled:");
        stringBuilder.append(enable);
        Log.d(tag, stringBuilder.toString());
        this.mCameraSettings.setAddWaterMarkEnabled(enable);
    }

    /* Access modifiers changed, original: protected */
    public void updateParametersSceneMode() {
        Tag tag;
        StringBuilder stringBuilder;
        if (this.mCameraCapabilities == null) {
            tag = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateParametersSceneMode  = null 11111");
            stringBuilder.append(this.mCameraCapabilities);
            Log.d(tag, stringBuilder.toString());
        } else {
            tag = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateParametersSceneMode != null 22222");
            stringBuilder.append(this.mCameraCapabilities);
            Log.d(tag, stringBuilder.toString());
        }
        Stringifier stringifier = this.mCameraCapabilities.getStringifier();
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        this.mSceneMode = stringifier.sceneModeFromString(settingsManager.getString(this.mAppController.getCameraScope(), Keys.KEY_SCENE_MODE));
        if (Keys.isHdrOn(settingsManager) && !isCameraFrontFacing() && isHdrShow()) {
            if (this.mSceneMode != SceneMode.HDR) {
                this.mSceneMode = SceneMode.HDR;
                settingsManager.set(this.mAppController.getCameraScope(), Keys.KEY_SCENE_MODE, this.mCameraCapabilities.getStringifier().stringify(this.mSceneMode));
            }
        } else if (this.mSceneMode == SceneMode.HDR) {
            this.mSceneMode = SceneMode.AUTO;
            settingsManager.set(this.mAppController.getCameraScope(), Keys.KEY_SCENE_MODE, this.mCameraCapabilities.getStringifier().stringify(this.mSceneMode));
        }
        if (!this.mCameraCapabilities.supports(this.mSceneMode)) {
            this.mSceneMode = this.mCameraSettings.getCurrentSceneMode();
            if (this.mSceneMode == null) {
                this.mSceneMode = SceneMode.AUTO;
            }
        } else if (this.mCameraSettings.getCurrentSceneMode() != this.mSceneMode) {
            this.mCameraSettings.setSceneMode(this.mSceneMode);
            if (this.mCameraDevice != null) {
                this.mCameraDevice.applySettings(this.mCameraSettings);
                this.mCameraSettings = this.mCameraDevice.getSettings();
            }
            this.mCameraSettings.setSceneMode(this.mSceneMode);
        }
        this.mCameraSettings.setSceneMode(this.mSceneMode);
        if (SceneMode.AUTO == this.mSceneMode) {
            updateParametersFlashMode();
            updateParametersFocusMode();
            return;
        }
        this.mFocusManager.overrideFocusMode(this.mCameraSettings.getCurrentFocusMode());
    }

    /* Access modifiers changed, original: protected */
    public void updateParametersFocusMode() {
        this.mFocusManager.overrideFocusMode(getOverrideFocusMode());
        this.mCameraSettings.setFocusMode(this.mFocusManager.getFocusMode(this.mCameraSettings.getCurrentFocusMode()));
    }

    /* Access modifiers changed, original: protected */
    public boolean updateParametersFlashMode() {
        boolean bNeedUpdate = false;
        FlashMode flashMode = this.mCameraCapabilities.getStringifier().flashModeFromString(this.mActivity.getSettingsManager().getString(this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE));
        if (flashMode != FlashMode.OFF) {
            bNeedUpdate = true;
        }
        if (bNeedUpdate && this.mCameraSettings != null && FlashMode.TORCH == this.mCameraSettings.getCurrentFlashMode()) {
            this.mCameraSettings.setFlashMode(FlashMode.OFF);
            if (this.mCameraDevice != null) {
                this.mCameraDevice.applySettings(this.mCameraSettings);
            }
        }
        if (this.mCameraCapabilities.supports(flashMode) && this.mActivity.currentBatteryStatusOK()) {
            this.mCameraSettings.setFlashMode(flashMode);
        } else if (this.mCameraCapabilities.supports(FlashMode.OFF)) {
            this.mCameraSettings.setFlashMode(FlashMode.OFF);
        } else {
            this.mCameraSettings.setFlashMode(FlashMode.NO_FLASH);
        }
        if (!(this.mEvoFlashLock == null || this.mCameraSettings == null)) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("evo setFlashMode ");
            stringBuilder.append(FlashMode.OFF);
            Log.d(tag, stringBuilder.toString());
            this.mCameraSettings.setFlashMode(FlashMode.OFF);
        }
        return bNeedUpdate;
    }

    /* Access modifiers changed, original: protected */
    @TargetApi(16)
    public void updateAutoFocusMoveCallback() {
        if (this.mCameraDevice != null && this.mCameraState != 5) {
            if (this.mCameraSettings.getCurrentFocusMode() == FocusMode.CONTINUOUS_PICTURE) {
                this.mCameraDevice.setAutoFocusMoveCallback(this.mHandler, (CameraAFMoveCallback) this.mAutoFocusMoveCallback);
            } else {
                this.mCameraDevice.setAutoFocusMoveCallback(null, null);
            }
        }
    }

    public void setExposureCompensation(int value, boolean needCache) {
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
        if (this.mCameraState == 5 || this.mCameraState == 6) {
            setExposureCompensation(this.mLockedEvoIndex, false);
        } else {
            setExposureCompensation(value, true);
        }
    }

    /* Access modifiers changed, original: protected */
    public void setCameraParameters(int updateSet) {
        if ((updateSet & 1) != 0) {
            updateCameraParametersInitialize();
        }
        if ((updateSet & 2) != 0) {
            updateCameraParametersZoom();
        }
        if ((updateSet & 4) != 0) {
            updateCameraParametersPreference();
        }
        if ((updateSet & 8) != 0) {
            updateVisidionMode();
        }
        if (this.mCameraDevice != null) {
            this.mCameraDevice.applySettings(this.mCameraSettings);
        }
    }

    private void setCameraParametersWhenIdle(int additionalUpdateSet) {
        this.mUpdateSet |= additionalUpdateSet;
        if (this.mCameraDevice == null) {
            this.mUpdateSet = 0;
            return;
        }
        if (isCameraIdle()) {
            setCameraParameters(this.mUpdateSet);
            updateSceneMode();
            this.mUpdateSet = 0;
        } else if (!this.mHandler.hasMessages(2)) {
            this.mHandler.sendEmptyMessageDelayed(2, 1000);
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

    private void setupCaptureParams() {
        Bundle myExtras = this.mActivity.getIntent().getExtras();
        if (myExtras != null) {
            this.mSaveUri = (Uri) myExtras.getParcelable("output");
            this.mCropValue = myExtras.getString("crop");
        }
    }

    private void initializeCapabilities() {
        this.mCameraCapabilities = this.mCameraDevice.getCapabilities();
        this.mFocusAreaSupported = this.mCameraCapabilities.supports(Feature.FOCUS_AREA);
        this.mMeteringAreaSupported = this.mCameraCapabilities.supports(Feature.METERING_AREA);
        this.mAeLockSupported = this.mCameraCapabilities.supports(Feature.AUTO_EXPOSURE_LOCK);
        this.mAwbLockSupported = this.mCameraCapabilities.supports(Feature.AUTO_WHITE_BALANCE_LOCK);
        this.mContinuousFocusSupported = this.mCameraCapabilities.supports(FocusMode.CONTINUOUS_PICTURE);
        Map<String, Boolean> capMap = new HashMap();
        capMap.put("mFocusAreaSupported", Boolean.valueOf(this.mFocusAreaSupported));
        capMap.put("mMeteringAreaSupported:", Boolean.valueOf(this.mMeteringAreaSupported));
        capMap.put("mAeLockSupported:", Boolean.valueOf(this.mAeLockSupported));
        capMap.put("mAwbLockSupported", Boolean.valueOf(this.mAwbLockSupported));
        capMap.put("mContinuousFocusSupported", Boolean.valueOf(this.mContinuousFocusSupported));
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("CameraCapabilities:\n");
        stringBuilder.append(capMap);
        Log.d(tag, stringBuilder.toString());
    }

    public void onZoomChanged(float ratio) {
        if (!this.mPaused) {
            float lastRatio = this.mZoomValue;
            this.mZoomValue = ratio;
            if (this.mCameraSettings != null && this.mCameraDevice != null) {
                this.mCameraSettings.setZoomRatio(this.mZoomValue);
                if (this.mZoomValue > 1.5f) {
                    isSuperResolutionEnabled();
                }
                this.mCameraDevice.applySettings(this.mCameraSettings);
                this.mUI.onStartFaceDetection(this.mDisplayOrientation, isCameraFrontFacing(), cropRegionForZoom(this.mCameraId), this.mZoomValue);
            }
        }
    }

    public Rect cropRegionForZoom(int id) {
        Rect activeRegion = this.mActivity.getSettingsManager().getSensorActiveArraySize(id);
        Rect cropRegion = new Rect();
        int xCenter = activeRegion.width() / 2;
        int yCenter = activeRegion.height() / 2;
        int xDelta = (int) (((float) activeRegion.width()) / (this.mZoomValue * 2.0f));
        int yDelta = (int) (((float) activeRegion.height()) / (2.0f * this.mZoomValue));
        cropRegion.set(xCenter - xDelta, yCenter - yDelta, xCenter + xDelta, yCenter + yDelta);
        if (this.mZoomValue == 1.0f) {
            this.mOriginalCropRegion[id] = cropRegion;
        }
        this.mCropRegion[id] = cropRegion;
        return this.mCropRegion[id];
    }

    public int getCameraState() {
        return this.mCameraState;
    }

    public void onMemoryStateChanged(int state) {
        boolean z = false;
        this.mUnderLowMemory = state != 0;
        AppController appController = this.mAppController;
        if (state == 0) {
            z = true;
        }
        appController.setShutterEnabled(z);
    }

    public void onLowMemory() {
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        float[] data;
        int type = event.sensor.getType();
        if (type == 1) {
            data = this.mGData;
        } else if (type == 2) {
            data = this.mMData;
        } else if (type == 5) {
            data = this.mLData;
            this.mLData[0] = event.values[0];
        } else {
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

    public void setDebugUri(Uri uri) {
        this.mDebugUri = uri;
    }

    private void saveToDebugUri(byte[] data) {
        if (this.mDebugUri != null) {
            OutputStream outputStream = null;
            try {
                outputStream = this.mContentResolver.openOutputStream(this.mDebugUri);
                outputStream.write(data);
                outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Exception while writing debug jpeg file", e);
            } catch (Throwable th) {
                CameraUtil.closeSilently(outputStream);
            }
            CameraUtil.closeSilently(outputStream);
        }
    }

    public void onRemoteShutterPress() {
        this.mHandler.post(new Runnable() {
            public void run() {
                LiveBokehModule.this.onShutterButtonClick();
            }
        });
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
        setCameraParameters(8);
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

    /* Access modifiers changed, original: protected */
    public boolean isBeautyShow() {
        return this.mActivity.getCameraAppUI().getBeautySeek() > 0.0f && this.mAppController.getCameraProvider().getCurrentCameraId() == 1;
    }

    public boolean isGesturePalmShow() {
        return false;
    }

    private void updateThumbal(Bitmap bitmap) {
        this.mActivity.getCameraAppUI().updatePeekThumbUri(null);
        this.mActivity.getCameraAppUI().updatePeekThumbBitmapWithAnimation(bitmap);
    }

    public boolean isSupportEffects() {
        return true;
    }

    public boolean isSupportBeauty() {
        return true;
    }

    public void openOrCloseEffects(int state, int effects) {
        if (this.mCameraId == 1) {
            boolean isBeautyEnable = this.mActivity.getCameraAppUI().getBeautyEnable();
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("openOrCloseEffects mIsGlMode = ");
            stringBuilder.append(this.mIsGlMode);
            stringBuilder.append(" , isBeautyEnable = ");
            stringBuilder.append(isBeautyEnable);
            Log.d(tag, stringBuilder.toString());
            if (this.mIsGlMode || (state < 0 && effects < 0)) {
                if (!isBeautyEnable) {
                    Log.d(TAG, "openOrCloseEffects closeEffects");
                    this.mIsGlMode = false;
                    this.mActivity.getCameraAppUI().showOrHideGLSurface(false);
                    this.mActivity.getCameraAppUI().hideImageCover();
                    requestCameraOpen();
                }
            } else if (!isBeautyEnable) {
                Log.d(TAG, "openOrCloseEffects openEffects");
                this.mIsGlMode = true;
                this.mActivity.getCameraAppUI().setOrientation(this.mCameraId);
                setPreviewBytesBack(this.mActivity.getCameraAppUI().getPreviewCallback());
                this.mCameraId = 1;
                this.mActivity.getCameraAppUI().showOrHideGLSurface(true);
                requestCameraOpen();
            }
        }
    }

    public boolean onGLRenderEnable() {
        return true;
    }

    public boolean isBackCamera() {
        if (this.mCameraId == 1) {
            return false;
        }
        return true;
    }

    public int getCameraMode() {
        String switchValue = this.mSettingsManager.getString(SettingsManager.SCOPE_GLOBAL, Keys.KEY_SWITCH_CAMERA);
        if (switchValue != null && !switchValue.equals("-1")) {
            SWITCH_ID = Integer.parseInt(this.mSettingsManager.getString(SettingsManager.SCOPE_GLOBAL, Keys.KEY_SWITCH_CAMERA));
            return 3;
        } else if (this.mSettingsManager.getString(SettingsManager.SCOPE_GLOBAL, Keys.KEY_SCENE_MODE) != null) {
            return 0;
        } else {
            return 2;
        }
    }

    public void setPreviewBytesBack(onPreviewBytes back) {
        this.mPreviewBytesBack = back;
    }
}
