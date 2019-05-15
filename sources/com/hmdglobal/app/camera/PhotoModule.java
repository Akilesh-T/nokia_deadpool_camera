package com.hmdglobal.app.camera;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.CaptureResult.Key;
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
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationManagerCompat;
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
import android.widget.Toast;
import com.android.camera.imageprocessor.filter.HDRCheckerFilter;
import com.android.camera.imageprocessor.listener.MAlgoProcessCallback;
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
import com.android.ex.camera2.portability.EvInfo;
import com.android.ex.camera2.portability.Size;
import com.android.external.ExtendKey;
import com.hmdglobal.app.camera.ButtonManager.ButtonCallback;
import com.hmdglobal.app.camera.ContinueShot.onContinueShotFinishListener;
import com.hmdglobal.app.camera.FocusOverlayManager.FocusArea;
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
import com.hmdglobal.app.camera.beauty.util.SharedUtil;
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
import com.hmdglobal.app.camera.settings.CameraPictureSizesCacher;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.ResolutionUtil;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.settings.SettingsManager.OnSettingChangedListener;
import com.hmdglobal.app.camera.settings.SettingsUtil;
import com.hmdglobal.app.camera.specialtype.ProcessingMediaManager;
import com.hmdglobal.app.camera.specialtype.ProcessingMediaManager.ProcessingMedia;
import com.hmdglobal.app.camera.specialtype.ProcessingMediaManager.ProcessingQueueListener;
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
import com.morphoinc.core.ImageRefiner;
import com.morphoinc.core.ImageRefiner.EngineParam;
import com.morphoinc.utils.multimedia.MediaProviderUtils;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TimeZone;
import java.util.Vector;

public class PhotoModule extends CameraModule implements PhotoController, ModuleController, MemoryListener, Listener, SensorEventListener, OnSettingChangedListener, RemoteCameraModule, OnCountDownStatusListener {
    private static final int BURST_DELAY = 0;
    protected static final int BURST_MAX = CustomUtil.getInstance().getInt(CustomFields.DEF_CAMERA_BURST_MAX, 10);
    protected static final int BURST_STOP_DELAY = 150;
    private static final String DEBUG_IMAGE_PREFIX = "DEBUG_";
    private static final int DEFAULT_GESTURE_SHOT_COUNT_DURATION = 3;
    private static final String EXTRA_QUICK_CAPTURE = "android.intent.extra.quickCapture";
    public static final int FRONT_FLASH_END = 1002;
    public static final int FRONT_FLASH_START = 1001;
    private static final String GESTURE_HANDLER_NAME = "gesture_handler";
    protected static final int MANUAL_MODULE_ID = 1;
    private static final int MAX_NUM_CAM = 4;
    private static final int MIN_CAMERA_LAUNCHING_TIMES = 3;
    private static final int MSG_BURST_STOP_SHOW_TOAST = 8;
    private static final int MSG_CAPTURE_BURST = 3;
    private static final int MSG_FIRST_TIME_INIT = 1;
    private static final int MSG_HIDE_GESTURE = 4;
    private static final int MSG_SET_CAMERA_PARAMETERS_WHEN_IDLE = 2;
    private static final int MSG_SET_CAMERA_STATE_TO_IDLE = 7;
    private static final int MSG_TAKE_PICTURE = 6;
    private static final int MSG_UPDATE_FACE_BEAUTY = 5;
    private static final int NEED_NOISY_ISO = 600;
    protected static final int PHOTO_MODULE_ID = 4;
    private static final String PHOTO_MODULE_STRING_ID = "PhotoModule";
    private static final String RATIO_18_9 = "2.11";
    private static final int REQUEST_CROP = 1000;
    private static final int SHUTTER_DELAY_LOW = 80;
    private static final int SHUTTER_DELAY_UP = 100;
    private static final int SHUTTER_PROGRESS_ACCELERATE_THRESHOLD = 30;
    private static final int SHUTTER_PROGRESS_FAKE_END = 90;
    private static final int SHUTTER_PROGRESS_HIDE_DELAY = 80;
    private static final int SHUTTER_PROGRESS_INIT = 0;
    private static final int SHUTTER_PROGRESS_MAX = 100;
    private static final int SHUTTER_PROGRESS_STEP = 5;
    public static final int SKIN_SMOOTHING_DEFAULT = 50;
    public static final int SKIN_SMOOTHING_MAX = 90;
    public static final int SKIN_SMOOTHING_RANGE = 100;
    protected static final int SQUARE_MODULE_ID = 15;
    private static final Tag TAG = new Tag(PHOTO_MODULE_STRING_ID);
    protected static final int UPDATE_PARAM_ALL = -1;
    protected static final int UPDATE_PARAM_INITIALIZE = 1;
    protected static final int UPDATE_PARAM_PREFERENCE = 4;
    protected static final int UPDATE_PARAM_VISIDON = 8;
    protected static final int UPDATE_PARAM_ZOOM = 2;
    public static final String action = "photomodule.broadcast.action";
    public static boolean firstFrame = true;
    private static Object mSyncObject = new Object();
    private static final String sTempCropFilename = "crop-temp";
    int curOritation;
    private long firsttime;
    private FrontFlashHandler frontFlashHandler;
    private boolean isOptimeizeSnapshot = false;
    private boolean isOptimizeCapture = true;
    private final boolean isOptimizeSwitchCamera = CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_ENABLE_OPTIMIZE_SWITCH, false);
    protected CameraActivity mActivity;
    private List<byte[]> mAddedImages;
    private int mAddedNumber;
    private boolean mAeLockSupported;
    protected AppController mAppController;
    private AudioManager mAudioManager;
    private final AutoFocusCallback mAutoFocusCallback = new AutoFocusCallback(this, null);
    protected final Object mAutoFocusMoveCallback;
    public long mAutoFocusTime;
    private boolean mAwbLockSupported;
    private int mBurstNumForOneSingleBurst;
    protected LinkedList<TotalCaptureResult> mBurstResultQueue;
    protected final BurstShotCheckQueue mBurstShotCheckQueue;
    private boolean mBurstShotNotifyHelpTip;
    private MultiToggleImageButton mButtonBeauty;
    private final ButtonCallback mCameraCallback;
    protected CameraCapabilities mCameraCapabilities;
    protected CameraProxy mCameraDevice;
    private CameraFinalPreviewCallback mCameraFinalPreviewCallback;
    private int mCameraId;
    private boolean mCameraKeyLongPressed = false;
    private boolean mCameraPreviewParamsReady;
    protected CameraSettings mCameraSettings;
    protected int mCameraState = 0;
    private final OnClickListener mCancelCallback;
    public long mCaptureStartTime;
    private ContentResolver mContentResolver;
    private boolean mContinuousFocusSupported;
    private SoundPlayer mCountdownSoundPlayer;
    private Rect[] mCropRegion;
    private String mCropValue;
    private Uri mDebugUri;
    private int mDisplayOrientation;
    protected int mDisplayRotation;
    private final Runnable mDoSnapRunnable = new Runnable() {
        public void run() {
            PhotoModule.this.onShutterButtonClick();
        }
    };
    private final OnClickListener mDoneCallback;
    List<Float> mDrCheckResult;
    private int mDropFrameIndex;
    private float[] mEvLowLightBack;
    private float[] mEvLowLightFront;
    private float[] mEvNormalBack;
    private float[] mEvNormalFront;
    private Integer mEvoFlashLock;
    private boolean mFaceDetectionStarted = false;
    private ArrayList<RectF> mFaces;
    List<Float> mFinalDrCheckResult;
    private boolean mFirstTimeInitialized;
    private String mFlashModeBeforeSceneMode;
    private boolean mFocusAreaSupported;
    protected FocusOverlayManager mFocusManager;
    private long mFocusStartTime;
    private int mFrameCount;
    protected final FrontFlashHandler mFrontFlashHandler;
    private final float[] mGData;
    private final int mGcamModeIndex;
    private Handler mGestureHandler;
    private GestureHandlerThread mGesturehandlerThread;
    protected final Handler mHandler;
    private boolean mHasBurstStoped = false;
    private boolean mHdrEnabled;
    private final ButtonCallback mHdrPlusCallback;
    private String mHdrState;
    private int mHeading;
    private Runnable mHideProgressRunnable;
    private ImageRefiner mImageRefiner;
    private boolean mInHdrProcess;
    private boolean mIsAddWaterMark;
    private boolean mIsCameraOpened;
    private boolean mIsFrontFlashEnd;
    protected boolean mIsGlMode;
    private boolean mIsImageCaptureIntent;
    private boolean mIsInIntentReviewUI;
    public long mJpegCallbackFinishTime;
    private byte[] mJpegImageData;
    private long mJpegPictureCallbackTime;
    private int mJpegRotation;
    private final float[] mLData;
    protected String mLastISOValue;
    private boolean mLastMaskEnable = false;
    private TotalCaptureResult mLastPreviewResult;
    private Location mLocation;
    private int mLockedEvoIndex;
    protected LongshotPictureCallback mLongshotPictureTakenCallback;
    private ShutterCallback mLongshotShutterCallback;
    private final ButtonCallback mLowlightCallback;
    private final float[] mMData;
    private MainHandlerCallback mMainHandlerCallback;
    private String mManualEtValue;
    private boolean mMeteringAreaSupported;
    private boolean mMirror;
    private MultiToggleImageButton mMoreEnterToggleButton;
    private MotionPictureHelper mMotionHelper;
    private MotionManager mMotionManager;
    private AlertDialog mMountedDialog;
    protected NamedImages mNamedImages;
    private boolean mNeedDeNoise;
    private boolean mNeedFocus;
    private boolean mNeedHdrBurst;
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
    private TotalCaptureResult mPreviewResult;
    private ProcessingQueueListener mProcessingQueueListener;
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
    private boolean mShouldResizeTo16x9;
    private long mShutterCallbackTime;
    public long mShutterLag;
    public long mShutterToPictureDisplayedTime;
    private TouchCoordinate mShutterTouchCoordinate;
    private boolean mSnapshotOnIdle = false;
    protected SoundPlayer mSoundPlayer;
    private boolean mStorageLocationCheck = false;
    private int mStreamId;
    protected final float mSuperZoomThreshold = 1.5f;
    private Bitmap mThumbnailCache;
    private int mTimerDuration;
    protected PhotoUI mUI;
    private boolean mUnderLowMemory;
    private int mUpdateSet;
    protected boolean mVolumeButtonClickedFlag = false;
    private byte[] mWaterBuff;
    private Integer mZoomFlashLock;
    protected float mZoomValue;
    protected boolean mforceISOManual;
    private MyThread myThread;
    private boolean pressflag;
    private OnSeekBarChangeListener seekListener;
    private ContinueShot snapShot;

    public interface AspectRatioDialogCallback {
        AspectRatio getCurrentAspectRatio();

        void onAspectRatioSelected(AspectRatio aspectRatio, Runnable runnable);
    }

    protected final class BurstShotCheckQueue {
        private static final int INVALID_VALUE = -1;
        private int mCapacity = PhotoModule.BURST_MAX;
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
            this.mCapacity = PhotoModule.BURST_MAX;
            this.mJobQueue.clear();
            this.mResultQueue.clear();
            return false;
        }

        public void setPictureTakenActionCache(Runnable runnable) {
            this.mSupposePictureTakenAction = runnable;
        }

        public void clearCheckQueue() {
            this.mCapacity = PhotoModule.BURST_MAX;
            this.mSupposePictureTakenAction = null;
            this.mJobQueue.clear();
            this.mResultQueue.clear();
        }

        public int getCapacity() {
            return this.mCapacity;
        }
    }

    private static class FrontFlashHandler extends Handler {
        private final WeakReference<PhotoModule> mPhotoModule;

        public FrontFlashHandler(PhotoModule module) {
            this.mPhotoModule = new WeakReference(module);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1001:
                    Log.d(PhotoModule.TAG, "[FrontFlashHandler] FRONT_FLASH_START");
                    ((PhotoModule) this.mPhotoModule.get()).setFrontFlashEnd(false);
                    return;
                case 1002:
                    Log.d(PhotoModule.TAG, "[FrontFlashHandler] FRONT_FLASH_END");
                    ((PhotoModule) this.mPhotoModule.get()).setFrontFlashEnd(true);
                    return;
                default:
                    return;
            }
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
                Log.e(PhotoModule.TAG, "Gesture engine encounter a fatal error , ignore it");
            }
        }
    }

    public interface LocationDialogCallback {
        void onLocationTaggingSelected(boolean z);
    }

    private class MainHandler extends Handler {
        private final WeakReference<PhotoModule> mModule;

        public MainHandler(PhotoModule module) {
            super(Looper.getMainLooper());
            this.mModule = new WeakReference(module);
        }

        public void handleMessage(Message msg) {
            PhotoModule module = (PhotoModule) this.mModule.get();
            if (module != null) {
                switch (msg.what) {
                    case 1:
                        module.initializeFirstTime();
                        break;
                    case 2:
                        module.setCameraParametersWhenIdle(0);
                        break;
                    case 3:
                        if (PhotoModule.this.mReceivedBurstNum < PhotoModule.BURST_MAX) {
                            PhotoModule.this.mCameraDevice.takePicture(PhotoModule.this.mHandler, PhotoModule.this.mLongshotShutterCallback, PhotoModule.this.mRawPictureCallback, PhotoModule.this.mPostViewPictureCallback, PhotoModule.this.mLongshotPictureTakenCallback);
                            if (!CustomUtil.getInstance().isPanther()) {
                                if (!PhotoModule.this.isCameraFrontFacing()) {
                                    PhotoModule.this.mHandler.sendEmptyMessageDelayed(3, 600);
                                    break;
                                } else {
                                    PhotoModule.this.mHandler.sendEmptyMessageDelayed(3, 700);
                                    break;
                                }
                            } else if (!PhotoModule.this.isCameraFrontFacing()) {
                                PhotoModule.this.mHandler.sendEmptyMessageDelayed(3, 300);
                                break;
                            } else {
                                PhotoModule.this.mHandler.sendEmptyMessageDelayed(3, 400);
                                break;
                            }
                        }
                        break;
                    case 4:
                        PhotoModule.this.mUI.hideGesture();
                        break;
                    case 5:
                        if (!(!PhotoModule.this.isCameraFrontFacing() || PhotoModule.this.mPaused || PhotoModule.this.mCameraSettings == null)) {
                            Log.e(PhotoModule.TAG, "update facebeauty");
                            SettingsManager settingsManager = PhotoModule.this.mActivity.getSettingsManager();
                            int skinSmoothing = PhotoModule.this.mActivity.getSettingsManager().getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_FACEBEAUTY_SKIN_SMOOTHING, Integer.valueOf(50)).intValue();
                            PhotoModule.this.mCameraSettings.setFaceBeauty(Keys.isFacebeautyOn(settingsManager), (skinSmoothing * 90) / 100);
                            PhotoModule.this.updateFaceBeautySetting(Keys.KEY_FACEBEAUTY_SKIN_SMOOTHING, skinSmoothing);
                            break;
                        }
                    case 6:
                        PhotoModule.this.doShutterButtonClick(false);
                        break;
                    case 7:
                        Log.d(PhotoModule.TAG, "MSG_SET_CAMERA_STATE_TO_IDLE");
                        PhotoModule.this.mInHdrProcess = false;
                        PhotoModule.this.setCameraState(1);
                        break;
                    case 8:
                        Log.d(PhotoModule.TAG, "MSG_BURST_STOP_SHOW_TOAST");
                        ToastUtil.showToast(PhotoModule.this.mActivity, PhotoModule.this.mActivity.getString(R.string.burst_shot_stop_toast), 0);
                        break;
                    default:
                        if (PhotoModule.this.mMainHandlerCallback != null) {
                            PhotoModule.this.mMainHandlerCallback.handleMessageEx(msg);
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

    class MyThread extends Thread {
        MyThread() {
        }

        public void run() {
            while (true) {
                Tag access$500;
                StringBuilder stringBuilder;
                if (System.currentTimeMillis() - PhotoModule.this.firsttime > 300 && PhotoModule.this.pressflag) {
                    access$500 = PhotoModule.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("intel 1 time = ");
                    stringBuilder.append(System.currentTimeMillis() - PhotoModule.this.firsttime);
                    Log.d(access$500, stringBuilder.toString());
                    if (PhotoModule.this.mCameraSettings != null) {
                        PhotoModule.this.mCameraSettings.setFlashMode(FlashMode.OFF);
                    }
                    if (PhotoModule.this.mCameraDevice != null) {
                        PhotoModule.this.mCameraDevice.applySettings(PhotoModule.this.mCameraSettings);
                        return;
                    }
                    return;
                } else if (PhotoModule.this.pressflag) {
                    try {
                        sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    access$500 = PhotoModule.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("intel 2 time = ");
                    stringBuilder.append(System.currentTimeMillis() - PhotoModule.this.firsttime);
                    Log.d(access$500, stringBuilder.toString());
                    return;
                }
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
            Size s = PhotoModule.this.mCameraSettings.getCurrentPhotoSize();
            ExifInterface exif = Exif.getExif(this.originalBytes);
            NamedEntity name = PhotoModule.this.mNamedImages.getNextNameEntity();
            Map<String, Object> externalInfo = PhotoModule.this.buildExternalBundle();
            if (externalInfo != null) {
                exif.setTag(exif.buildTag(ExifInterface.TAG_USER_COMMENT, CameraUtil.serializeToJson(externalInfo)));
            }
            PhotoModule.this.mInHdrProcess = false;
            final String title = name == null ? null : name.title;
            new Thread(new Runnable() {
                public void run() {
                    String path = Storage.generateFilepath(title);
                    Bitmap roatedBitmap = CameraUtil.rotateBitmap(PictureTaskListener.this.modifiedBytes, (float) PhotoModule.this.getMaskImageRotation(PhotoModule.this.mOrientation));
                    Bitmap finalBitmap = roatedBitmap;
                    if (PhotoModule.this.isCameraFrontFacing() && !PhotoModule.this.isNeedMirrorSelfie()) {
                        finalBitmap = CameraUtil.mirrorBitmap(roatedBitmap);
                    }
                    Bitmap finalBitmap2 = finalBitmap;
                    Uri uri = ImageUtils.saveImageToGallery(PhotoModule.this.mActivity, finalBitmap2, path, PhotoModule.this.mLocation, PhotoModule.this.mDisplayRotation, 0);
                    if (uri != null) {
                        PhotoModule.this.mActivity.notifyNewMedia(uri);
                    }
                }
            }).start();
            PhotoModule.this.mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    PhotoModule.this.mAppController.setShutterEnabled(true);
                    PhotoModule.this.setCameraState(1);
                    PhotoModule.this.mActivity.updateStorageSpaceAndHint(null);
                    PhotoModule.this.hideScreenBrightness();
                }
            });
            int mode = PhotoModule.this.mAudioManager.getRingerMode();
            if (!Keys.isShutterSoundOn(PhotoModule.this.mAppController.getSettingsManager()) || mode != 2 || PhotoModule.this.mSoundPlayer == null) {
                return;
            }
            if (CustomUtil.getInstance().isSkuid()) {
                PhotoModule.this.mSoundPlayer.play(R.raw.shutter_sound_2, 1.0f);
            } else {
                PhotoModule.this.mSoundPlayer.play(R.raw.shutter, 1.0f);
            }
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
                PhotoModule.this.mHandler.postDelayed(setProgress(this.mProgress + 5), (long) delay);
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

        /* synthetic */ AutoFocusCallback(PhotoModule x0, AnonymousClass1 x1) {
            this();
        }

        public void onAutoFocus(boolean focused, CameraProxy camera) {
            SessionStatsCollector.instance().autofocusResult(focused);
            if (!PhotoModule.this.mPaused && !PhotoModule.this.isInBurstshot()) {
                PhotoModule.this.mAutoFocusTime = System.currentTimeMillis() - PhotoModule.this.mFocusStartTime;
                Tag access$500 = PhotoModule.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mAutoFocusTime = ");
                stringBuilder.append(PhotoModule.this.mAutoFocusTime);
                stringBuilder.append("ms   focused = ");
                stringBuilder.append(focused);
                Log.v(access$500, stringBuilder.toString());
                if (!PhotoModule.this.needEnableExposureAdjustment() || !focused || PhotoModule.this.mFocusManager == null || PhotoModule.this.mFocusManager.getFocusAreas() == null) {
                    Log.v(PhotoModule.TAG, "focus failed , set camera state back to IDLE");
                    PhotoModule.this.setCameraState(1);
                } else {
                    Log.v(PhotoModule.TAG, "focus succeed , show exposure slider");
                    if (PhotoModule.this.mCameraState == 2) {
                        PhotoModule.this.mUI.showEvoSlider();
                    }
                    PhotoModule.this.setCameraState(6);
                }
                int action = FocusOverlayManager.ACTION_RESTORE_CAF_LATER;
                if (PhotoModule.this.needEnableExposureAdjustment()) {
                    action |= FocusOverlayManager.ACTION_KEEP_FOCUS_FRAME;
                }
                PhotoModule.this.mFocusManager.onAutoFocus(focused, action);
            }
        }
    }

    @TargetApi(16)
    private final class AutoFocusMoveCallback implements CameraAFMoveCallback {
        private AutoFocusMoveCallback() {
        }

        /* synthetic */ AutoFocusMoveCallback(PhotoModule x0, AnonymousClass1 x1) {
            this();
        }

        public void onAutoFocusMoving(boolean moving, CameraProxy camera) {
            if (PhotoModule.this.mCameraState != 4) {
                PhotoModule.this.mUI.clearEvoPendingUI();
                if (PhotoModule.this.mEvoFlashLock != null) {
                    PhotoModule.this.mAppController.getButtonManager().enableButtonWithToken(0, PhotoModule.this.mEvoFlashLock.intValue());
                    PhotoModule.this.mEvoFlashLock = null;
                }
                PhotoModule.this.mFocusManager.onAutoFocusMoving(moving);
                SessionStatsCollector.instance().autofocusMoving(moving);
            }
        }
    }

    private final class BurstShotSaveListener implements OnMediaSavedListener {
        private int mCount = 0;

        public BurstShotSaveListener(int currentCount) {
            this.mCount = currentCount;
            PhotoModule.this.mBurstShotCheckQueue.pushToJobQueue(this.mCount);
        }

        public void onMediaSaved(Uri uri) {
            Tag access$500 = PhotoModule.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Burst Waiting for ");
            stringBuilder.append(PhotoModule.this.mBurstShotCheckQueue.getCapacity());
            stringBuilder.append(" current count is ");
            stringBuilder.append(this.mCount);
            Log.w(access$500, stringBuilder.toString());
            PhotoModule.this.mBurstShotCheckQueue.popToResultQueue(this.mCount);
            if (uri != null) {
                int notifyAction = 2;
                if (PhotoModule.this.isOptimizeCapture) {
                    notifyAction = 2 | 4;
                }
                Tag access$5002 = PhotoModule.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("update thumbnail with burst image, uri=");
                stringBuilder2.append(uri.toString());
                Log.v(access$5002, stringBuilder2.toString());
                PhotoModule.this.mActivity.notifyNewMedia(uri, notifyAction);
            } else {
                ToastUtil.showToast(PhotoModule.this.mActivity, PhotoModule.this.mActivity.getString(R.string.error_when_saving_picture_toast), 0);
                ToastUtil.showToast(PhotoModule.this.mActivity, PhotoModule.this.mActivity.getString(R.string.photos_were_not_saved_to_the_device_toast), 0);
            }
            PhotoModule.this.dismissSavingHint();
        }
    }

    private final class BurstShutterCallback implements CameraShutterCallback {
        private BurstShutterCallback() {
        }

        public void onShutter(CameraProxy camera) {
            Log.v(PhotoModule.TAG, "burst onShutterCallback");
            if (!PhotoModule.this.mPaused && PhotoModule.this.mCameraState != 0) {
                if (PhotoModule.this.mCameraState == 8 || PhotoModule.this.mCameraState == 9) {
                    PhotoModule.this.mShutterCallbackTime = System.currentTimeMillis();
                    PhotoModule.this.mNamedImages.nameNewImage(PhotoModule.this.mShutterCallbackTime);
                    PhotoModule.this.mShutterLag = PhotoModule.this.mShutterCallbackTime - PhotoModule.this.mCaptureStartTime;
                    Tag access$500 = PhotoModule.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("burst mShutterLag = ");
                    stringBuilder.append(PhotoModule.this.mShutterLag);
                    stringBuilder.append("ms");
                    Log.v(access$500, stringBuilder.toString());
                    PhotoModule.this.mCameraDevice.takePictureWithoutWaiting(PhotoModule.this.mHandler, this, PhotoModule.this.mRawPictureCallback, PhotoModule.this.mPostViewPictureCallback, PhotoModule.this.mLongshotPictureTakenCallback);
                    return;
                }
                Log.w(PhotoModule.TAG, "stop burst in shutter callback");
            }
        }
    }

    private final class CaptureComplete implements CaptureCompleteCallBack {
        private CaptureComplete() {
        }

        /* synthetic */ CaptureComplete(PhotoModule x0, AnonymousClass1 x1) {
            this();
        }

        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            Log.d(PhotoModule.TAG, "onCaptureCompleted");
            PhotoModule.this.mBurstResultQueue.add(result);
        }
    }

    private final class JpegPictureCallback implements CameraPictureCallback {
        Location mLocation;

        public JpegPictureCallback(Location loc) {
            this.mLocation = loc;
        }

        public void onPictureTaken(byte[] originalJpegData, CameraProxy camera) {
            Throwable th;
            byte[] bArr = originalJpegData;
            Tag access$500 = PhotoModule.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onPictureTaken, camera state is ");
            stringBuilder.append(PhotoModule.this.mCameraState);
            Log.w(access$500, stringBuilder.toString());
            PhotoModule.this.hideScreenBrightness();
            PhotoModule.this.mCameraSettings.setExposureTime("0");
            if (PhotoModule.this.mCameraDevice != null) {
                PhotoModule.this.mCameraDevice.applySettings(PhotoModule.this.mCameraSettings);
            }
            if (!(PhotoModule.this.mAppController == null || PhotoModule.this.mAppController.getCameraAppUI() == null)) {
                PhotoModule.this.mAppController.getCameraAppUI().resetRings();
            }
            if ("0".equals(PhotoModule.this.getManualEtValue()) || PhotoModule.this.mAppController.getCurrentModuleIndex() != PhotoModule.this.mAppController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_pro)) {
                PhotoModule.this.mCameraSettings.setAdjustEt(false);
            } else {
                PhotoModule.this.mCameraSettings.setAdjustEt(true);
                PhotoModule.this.stopPreview();
                PhotoModule.this.startPreview();
            }
            PhotoModule.this.mAppController.setShutterEnabled(true);
            if (PhotoModule.this.mPaused && ProcessingMediaManager.getInstance(PhotoModule.this.mActivity).getProcessingMedia().isEmpty()) {
                PhotoModule.this.mActivity.getButtonManager().mBeautyEnable = true;
                return;
            }
            if (bArr != null) {
                int sf = PhotoModule.this.mFinalDrCheckResult.size();
                boolean isLastPicture = sf + -1 == PhotoModule.this.mAddedNumber;
                if (((sf > 1 && isLastPicture) || PhotoModule.this.isBeautyOnGP()) && PhotoModule.this.mThumbnailCache != null) {
                    new AsyncTask<byte[], Void, TE>() {
                        /* Access modifiers changed, original: protected|varargs */
                        public TE doInBackground(byte[]... data) {
                            return PhotoModule.this.convertN21ToBitmap(data[0]);
                        }

                        /* Access modifiers changed, original: protected */
                        public void onPostExecute(TE te) {
                            if (te != null) {
                                PhotoModule.this.mAppController.getCameraAppUI().updatePeekThumbUri(null);
                                PhotoModule.this.mAppController.getCameraAppUI().updatePeekThumbBitmap(te.bitmap);
                            }
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new byte[][]{bArr});
                }
            }
            HelpTipsManager helpTipsManager = PhotoModule.this.mAppController.getHelpTipsManager();
            if (helpTipsManager != null) {
                helpTipsManager.onRecentTipResponse();
            }
            PhotoModule.this.dismissOptimisingPhotoHint();
            if (PhotoModule.this.mIsImageCaptureIntent) {
                PhotoModule.this.stopPreview();
            }
            if (PhotoModule.this.mSceneMode == SceneMode.HDR) {
                PhotoModule.this.mUI.setSwipingEnabled(true);
            }
            PhotoModule.this.mJpegPictureCallbackTime = System.currentTimeMillis();
            if (PhotoModule.this.mPostViewPictureCallbackTime != 0) {
                PhotoModule.this.mShutterToPictureDisplayedTime = PhotoModule.this.mPostViewPictureCallbackTime - PhotoModule.this.mShutterCallbackTime;
                PhotoModule.this.mPictureDisplayedToJpegCallbackTime = PhotoModule.this.mJpegPictureCallbackTime - PhotoModule.this.mPostViewPictureCallbackTime;
            } else {
                PhotoModule.this.mShutterToPictureDisplayedTime = PhotoModule.this.mRawPictureCallbackTime - PhotoModule.this.mShutterCallbackTime;
                PhotoModule.this.mPictureDisplayedToJpegCallbackTime = PhotoModule.this.mJpegPictureCallbackTime - PhotoModule.this.mRawPictureCallbackTime;
            }
            access$500 = PhotoModule.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("mPictureDisplayedToJpegCallbackTime = ");
            stringBuilder2.append(PhotoModule.this.mPictureDisplayedToJpegCallbackTime);
            stringBuilder2.append("ms");
            Log.v(access$500, stringBuilder2.toString());
            boolean needRestart = false;
            if (!(PhotoModule.this.mCameraSettings == null || PhotoModule.this.mCameraSettings.isZslOn)) {
                needRestart = true;
            }
            if (needRestart) {
                PhotoModule.this.setCameraState(0);
                if (!PhotoModule.this.mIsImageCaptureIntent) {
                    PhotoModule.this.mUI.clearEvoPendingUI();
                    if (PhotoModule.this.mEvoFlashLock != null) {
                        PhotoModule.this.mAppController.getButtonManager().enableButtonWithToken(0, PhotoModule.this.mEvoFlashLock.intValue());
                        PhotoModule.this.mEvoFlashLock = null;
                    }
                    PhotoModule.this.mUI.clearFocus();
                    PhotoModule.this.mFocusManager.resetTouchFocus();
                    PhotoModule.this.setupPreview();
                }
            } else {
                if (PhotoModule.this.mCameraState == 7) {
                    PhotoModule.this.setCameraState(5);
                    PhotoModule.this.mAppController.getLockEventListener().onIdle();
                    PhotoModule.this.mAppController.getCameraAppUI().showModeOptions();
                } else if (!PhotoModule.this.mIsImageCaptureIntent) {
                    PhotoModule.this.setCameraState(1);
                }
                if (PhotoModule.this.mCameraState != 5) {
                    PhotoModule.this.mFocusManager.cancelAutoFocus();
                }
                PhotoModule.this.mHandler.removeMessages(4);
                PhotoModule.this.mUI.hideGesture();
                if (!(PhotoModule.this.mCameraDevice == null || PhotoModule.this.mIsImageCaptureIntent || !PhotoModule.this.isEnableGestureRecognization())) {
                    if (PhotoModule.this.mGesturehandlerThread == null || !PhotoModule.this.mGesturehandlerThread.isAlive()) {
                        Log.w(PhotoModule.TAG, "GestureCore open looper , tray start thread");
                        PhotoModule.this.mGesturehandlerThread = new GestureHandlerThread(PhotoModule.GESTURE_HANDLER_NAME);
                        PhotoModule.this.mGesturehandlerThread.start();
                        PhotoModule.this.mGestureHandler = new Handler(PhotoModule.this.mGesturehandlerThread.getLooper());
                    }
                    PhotoModule.this.mCameraDevice.startPreview();
                }
            }
            PhotoModule.this.mJpegCallbackFinishTime = System.currentTimeMillis() - PhotoModule.this.mJpegPictureCallbackTime;
            Tag access$5002 = PhotoModule.TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("mJpegCallbackFinishTime = ");
            stringBuilder3.append(PhotoModule.this.mJpegCallbackFinishTime);
            stringBuilder3.append("ms");
            Log.v(access$5002, stringBuilder3.toString());
            PhotoModule.this.mJpegPictureCallbackTime = 0;
            if (PhotoModule.this.mFinalDrCheckResult.size() > 1 && PhotoModule.this.mActivity.getCameraAppUI().getBeautySeek() == 0.0f) {
                if (PhotoModule.this.mImageRefiner == null || bArr == null) {
                    Log.d(PhotoModule.TAG, "mImageRefiner == null");
                    return;
                }
                EvInfo evInfo = (EvInfo) PhotoModule.this.mCameraSettings.getEvInfo().get(PhotoModule.this.mAddedNumber);
                long exposureTime = evInfo.exposureTime;
                int iso = evInfo.isoSpeed;
                if (PhotoModule.this.mAddedNumber == PhotoModule.this.mDropFrameIndex) {
                    Log.d(PhotoModule.TAG, "drop");
                } else {
                    Tag access$5003 = PhotoModule.TAG;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("add exposureTime = ");
                    stringBuilder4.append(exposureTime);
                    stringBuilder4.append(" iso = ");
                    stringBuilder4.append(iso);
                    Log.d(access$5003, stringBuilder4.toString());
                    int addResult = PhotoModule.this.mImageRefiner.addImageRaw(bArr, exposureTime, iso);
                    Tag access$5004 = PhotoModule.TAG;
                    StringBuilder stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("addResult = ");
                    stringBuilder5.append(addResult);
                    Log.d(access$5004, stringBuilder5.toString());
                    if (PhotoModule.this.mAddedNumber == PhotoModule.this.mDropFrameIndex + 1) {
                        access$500 = PhotoModule.TAG;
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("add exposureTime = ");
                        stringBuilder4.append(exposureTime);
                        stringBuilder4.append(" iso = ");
                        stringBuilder4.append(iso);
                        Log.d(access$500, stringBuilder4.toString());
                        addResult = PhotoModule.this.mImageRefiner.addImageRaw(bArr, exposureTime, iso);
                        access$500 = PhotoModule.TAG;
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("addResult = ");
                        stringBuilder4.append(addResult);
                        Log.d(access$500, stringBuilder4.toString());
                    }
                    PhotoModule.this.mAddedImages.add(bArr);
                }
                PhotoModule.this.mAddedNumber = PhotoModule.this.mAddedNumber + 1;
                access$500 = PhotoModule.TAG;
                StringBuilder stringBuilder6 = new StringBuilder();
                stringBuilder6.append("mAddedNumber = ");
                stringBuilder6.append(PhotoModule.this.mAddedNumber);
                stringBuilder6.append(" mFinalDrCheckResult.size() = ");
                stringBuilder6.append(PhotoModule.this.mFinalDrCheckResult.size());
                Log.d(access$500, stringBuilder6.toString());
                PhotoModule.this.mActivity.getButtonManager().mBeautyEnable = true;
                if (PhotoModule.this.mAddedNumber != PhotoModule.this.mFinalDrCheckResult.size()) {
                    return;
                }
            }
            PhotoModule.this.mAddedNumber = 0;
            PhotoModule.this.setPreviewCallback();
            PhotoModule.this.mHandler.removeMessages(7);
            int mode = PhotoModule.this.mAudioManager.getRingerMode();
            if (Keys.isShutterSoundOn(PhotoModule.this.mAppController.getSettingsManager()) && mode == 2 && PhotoModule.this.mSoundPlayer != null) {
                if (CustomUtil.getInstance().isSkuid()) {
                    PhotoModule.this.mSoundPlayer.play(R.raw.shutter_sound_2, 1.0f);
                } else {
                    PhotoModule.this.mSoundPlayer.play(R.raw.shutter, 1.0f);
                }
            }
            synchronized (PhotoModule.mSyncObject) {
                final CameraProxy cameraProxy;
                try {
                    cameraProxy = camera;
                    new AsyncTask<byte[], Void, byte[]>() {
                        /* Access modifiers changed, original: protected|varargs */
                        public byte[] doInBackground(byte[]... data) {
                            byte[] originalJpegData;
                            byte[] originalJpegData2 = data[0];
                            if (PhotoModule.this.mActivity.getCameraAppUI().getBeautySeek() > 0.0f && PhotoModule.this.isSupportBeauty() && (PhotoModule.this.mCameraId == 0 || PhotoModule.this.mCameraId == 1)) {
                                Size size = PhotoModule.this.mCameraSettings.getCurrentPhotoSize();
                                float seek = PhotoModule.this.mActivity.getCameraAppUI().getBeautySeek() / 20.0f;
                                int angle = PhotoModule.this.mCameraId == 1 ? MediaProviderUtils.ROTATION_270 : 90;
                                if (originalJpegData2 != null) {
                                    originalJpegData2 = BeautifyHandler.processImageNV21(PhotoModule.this.mActivity, originalJpegData2, size.width(), size.height(), seek, angle);
                                }
                            }
                            PhotoModule.this.mActivity.getButtonManager().mBeautyEnable = true;
                            Size size2 = PhotoModule.this.mCameraSettings.getCurrentPhotoSize();
                            PhotoModule.this.mIsAddWaterMark = PhotoModule.this.mCameraSettings.isAddWaterMarkEnabled();
                            if (PhotoModule.this.mFinalDrCheckResult.size() <= 1) {
                                Log.d(PhotoModule.TAG, "start to jpeg");
                                originalJpegData = PhotoModule.this.convertN21ToJpeg(originalJpegData2, size2.width(), size2.height());
                                Log.d(PhotoModule.TAG, "end to jpeg");
                            } else {
                                int result = PhotoModule.this.mImageRefiner.processImage();
                                Tag access$500 = PhotoModule.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("processImage result = ");
                                stringBuilder.append(result);
                                Log.d(access$500, stringBuilder.toString());
                                byte[] output = new byte[originalJpegData2.length];
                                if (result < 0) {
                                    output = (byte[]) PhotoModule.this.mAddedImages.get(0);
                                } else {
                                    int getOutputResult = PhotoModule.this.mImageRefiner.getOutputImageRaw(output);
                                    Tag access$5002 = PhotoModule.TAG;
                                    StringBuilder stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("getOutputResult = ");
                                    stringBuilder2.append(getOutputResult);
                                    Log.d(access$5002, stringBuilder2.toString());
                                }
                                PhotoModule.this.mImageRefiner.finish();
                                PhotoModule.this.mImageRefiner = null;
                                Log.d(PhotoModule.TAG, "mImageRefiner = null");
                                Log.d(PhotoModule.TAG, "compressToJpeg");
                                originalJpegData = PhotoModule.this.convertN21ToJpeg(output, size2.width(), size2.height());
                            }
                            if (cameraProxy.getCharacteristics().isFacingFront() && PhotoModule.this.isNeedMirrorSelfie()) {
                                originalJpegData = PhotoModule.this.aftMirrorJpeg(originalJpegData);
                            }
                            if (!Keys.isAlgorithmsOn(PhotoModule.this.mActivity.getSettingsManager()) || PhotoModule.this.isDepthEnabled() || PhotoModule.this.mBurstResultQueue.isEmpty()) {
                                return originalJpegData;
                            }
                            TotalCaptureResult totalCaptureResult = (TotalCaptureResult) PhotoModule.this.mBurstResultQueue.removeFirst();
                            if (PhotoModule.this.mInHdrProcess) {
                                PhotoModule.this.mBurstResultQueue.clear();
                            }
                            return PhotoModule.this.addExifTags(originalJpegData, totalCaptureResult, cameraProxy.getCharacteristics().isFacingFront(), PhotoModule.this.mJpegRotation);
                        }

                        /* Access modifiers changed, original: protected */
                        public void onPostExecute(byte[] originalJpegData) {
                            PhotoModule.this.mInHdrProcess = false;
                            ExifInterface exif = Exif.getExif(originalJpegData);
                            final NamedEntity name = PhotoModule.this.mNamedImages.getNextNameEntity();
                            if (PhotoModule.this.mShouldResizeTo16x9) {
                                ResizeBundle dataBundle = new ResizeBundle();
                                dataBundle.jpegData = originalJpegData;
                                dataBundle.targetAspectRatio = 1.7777778f;
                                dataBundle.exif = exif;
                                new AsyncTask<ResizeBundle, Void, ResizeBundle>() {
                                    /* Access modifiers changed, original: protected|varargs */
                                    public ResizeBundle doInBackground(ResizeBundle... resizeBundles) {
                                        return PhotoModule.this.cropJpegDataToAspectRatio(resizeBundles[0]);
                                    }

                                    /* Access modifiers changed, original: protected */
                                    public void onPostExecute(ResizeBundle result) {
                                        JpegPictureCallback.this.saveFinalPhoto(result.jpegData, name, result.exif, cameraProxy);
                                    }
                                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new ResizeBundle[]{dataBundle});
                            } else {
                                JpegPictureCallback.this.saveFinalPhoto(originalJpegData, name, exif, cameraProxy);
                            }
                            if (PhotoModule.this.isOptimizeCapture && exif.hasThumbnail()) {
                                PhotoModule.this.updateThumbnail(exif);
                            }
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new byte[][]{bArr});
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            }
        }

        private byte[] addWaterMark(byte[] output, Size size) {
            int h = size.height();
            int w = size.width();
            if (PhotoModule.this.mIsAddWaterMark) {
                int j = h - 76;
                while (true) {
                    int i = 0;
                    if (j >= h) {
                        break;
                    }
                    while (i < 554) {
                        output[(j * w) + i] = PhotoModule.this.mWaterBuff[((j - (h - 76)) * 554) + i];
                        i++;
                    }
                    j++;
                }
                j = 554 * 76;
                int offsetBig = w * h;
                int uvHeight = h >> 1;
                for (int j2 = uvHeight - (76 / 2); j2 < uvHeight; j2++) {
                    for (int i2 = 0; i2 < 554 / 2; i2 += 2) {
                        int index = (((j2 - (uvHeight - (76 / 2))) * 554) + j) + i2;
                        output[((j2 * w) + offsetBig) + i2] = PhotoModule.this.mWaterBuff[index];
                        output[(((j2 * w) + offsetBig) + i2) + 1] = PhotoModule.this.mWaterBuff[index + 1];
                    }
                }
            }
            return output;
        }

        /* Access modifiers changed, original: 0000 */
        public void saveFinalPhoto(byte[] jpegData, NamedEntity name, ExifInterface exif, CameraProxy camera) {
            saveFinalPhoto(jpegData, name, exif, camera, PhotoModule.this.buildExternalBundle());
        }

        /* Access modifiers changed, original: 0000 */
        public void saveFinalPhoto(byte[] jpegData, NamedEntity name, ExifInterface exif, CameraProxy camera, Map<String, Object> externalInfo) {
            byte[] bArr = jpegData;
            NamedEntity namedEntity = name;
            ExifInterface exifInterface = exif;
            int orientation = Exif.getOrientation(exif);
            float zoomValue = 1.0f;
            if (PhotoModule.this.mCameraCapabilities.supports(Feature.ZOOM)) {
                zoomValue = PhotoModule.this.mCameraSettings.getCurrentZoomRatio();
            }
            float zoomValue2 = zoomValue;
            boolean hdrOn = SceneMode.HDR == PhotoModule.this.mSceneMode;
            String flashSetting = PhotoModule.this.mActivity.getSettingsManager().getString(PhotoModule.this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE);
            boolean gridLinesOn = Keys.areGridLinesOn(PhotoModule.this.mActivity.getSettingsManager());
            UsageStatistics instance = UsageStatistics.instance();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(namedEntity.title);
            stringBuilder.append(Storage.JPEG_POSTFIX);
            instance.photoCaptureDoneEvent(10000, stringBuilder.toString(), exifInterface, camera.getCharacteristics().isFacingFront(), hdrOn, zoomValue2, flashSetting, gridLinesOn, Float.valueOf((float) PhotoModule.this.mTimerDuration), PhotoModule.this.mShutterTouchCoordinate, Boolean.valueOf(PhotoModule.this.mVolumeButtonClickedFlag));
            PhotoModule.this.mShutterTouchCoordinate = null;
            PhotoModule.this.mVolumeButtonClickedFlag = false;
            int orientation2;
            if (PhotoModule.this.mIsImageCaptureIntent) {
                orientation2 = orientation;
                PhotoModule.this.mJpegImageData = bArr;
                PhotoModule.this.mUI.disableZoom();
                if (PhotoModule.this.mQuickCapture) {
                    PhotoModule.this.onCaptureDone();
                } else {
                    Log.v(PhotoModule.TAG, "showing UI");
                    PhotoModule.this.mUI.showCapturedImageForReview(bArr, orientation2, false);
                    PhotoModule.this.mIsInIntentReviewUI = true;
                }
            } else {
                int width;
                int height;
                int width2;
                int DEPTH_H;
                Integer exifWidth = exifInterface.getTagIntValue(ExifInterface.TAG_PIXEL_X_DIMENSION);
                Integer exifHeight = exifInterface.getTagIntValue(ExifInterface.TAG_PIXEL_Y_DIMENSION);
                if (!PhotoModule.this.mShouldResizeTo16x9 || exifWidth == null || exifHeight == null) {
                    Size s = PhotoModule.this.mCameraSettings.getCurrentPhotoSize();
                    if ((PhotoModule.this.mJpegRotation + orientation) % MediaProviderUtils.ROTATION_180 == 0) {
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
                if (PhotoModule.this.mDebugUri != null) {
                    PhotoModule.this.saveToDebugUri(bArr);
                    if (title != null) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(PhotoModule.DEBUG_IMAGE_PREFIX);
                        stringBuilder2.append(title);
                        title = stringBuilder2.toString();
                    }
                }
                if (title == null) {
                    Log.e(PhotoModule.TAG, "Unbalanced name/data pair");
                    orientation2 = orientation;
                } else {
                    StringBuilder stringBuilder3;
                    if (namedEntity.date == -1) {
                        namedEntity.date = PhotoModule.this.mCaptureStartTime;
                    }
                    if (PhotoModule.this.mHeading >= 0) {
                        ExifTag directionRefTag = exifInterface.buildTag(ExifInterface.TAG_GPS_IMG_DIRECTION_REF, "M");
                        ExifTag directionTag = exifInterface.buildTag(ExifInterface.TAG_GPS_IMG_DIRECTION, new Rational((long) PhotoModule.this.mHeading, 1));
                        exifInterface.setTag(directionRefTag);
                        exifInterface.setTag(directionTag);
                    }
                    if (externalInfo != null) {
                        exifInterface.setTag(exifInterface.buildTag(ExifInterface.TAG_USER_COMMENT, CameraUtil.serializeToJson(externalInfo)));
                    }
                    if (PhotoModule.this.mCameraSettings.isMotionOn()) {
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("MV");
                        stringBuilder3.append(title);
                        title = stringBuilder3.toString();
                    }
                    String title2 = title;
                    Integer num;
                    if (PhotoModule.this.isDepthEnabled()) {
                        ArrayList<byte[]> bokehBytes = MpoInterface.generateXmpFromMpo(jpegData);
                        if (bokehBytes != null) {
                            Tag access$500 = PhotoModule.TAG;
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("bokehBytes.size()");
                            stringBuilder3.append(bokehBytes.size());
                            Log.d(access$500, stringBuilder3.toString());
                        }
                        if (bokehBytes == null || bokehBytes.size() <= 2) {
                            Log.v(PhotoModule.TAG, "save addImage");
                            orientation2 = orientation;
                            PhotoModule.this.getServices().getMediaSaver().addImage(bArr, title2, namedEntity.date, this.mLocation, width, height, orientation, exifInterface, PhotoModule.this.mOnMediaSavedListener, PhotoModule.this.mContentResolver);
                        } else {
                            int DEPTH_W;
                            GImage gImage = new GImage((byte[]) bokehBytes.get(1), "image/jpeg");
                            Size photoSize = PhotoModule.this.mCameraSettings.getCurrentPhotoSize();
                            int photoWidth = photoSize.width();
                            width2 = photoSize.height();
                            if (((float) photoWidth) / ((float) width2) == 1.3333334f) {
                                DEPTH_W = 896;
                                DEPTH_H = DepthUtil.DEPTH_HEIGHT_4_3;
                                Log.d(PhotoModule.TAG, "set width x height to 4:3 size by default");
                            } else if (((float) photoWidth) / ((float) width2) == 1.7777778f) {
                                DEPTH_W = 896;
                                DEPTH_H = DepthUtil.DEPTH_HEIGHT_16_9;
                                Log.d(PhotoModule.TAG, "set width x height to 16:9 size");
                            } else {
                                DEPTH_W = 1000;
                                DEPTH_H = 500;
                                Log.d(PhotoModule.TAG, "set width x height to 18:9 size");
                            }
                            DepthMap depthMap = new DepthMap(DEPTH_W, DEPTH_H, MediaProviderUtils.ROTATION_180);
                            depthMap.buffer = (byte[]) bokehBytes.get(bokehBytes.size() - 1);
                            Tag access$5002 = PhotoModule.TAG;
                            StringBuilder stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("depthMap.buffer = ");
                            stringBuilder4.append(depthMap.buffer.length);
                            Log.v(access$5002, stringBuilder4.toString());
                            GDepth gDepth = GDepth.createGDepth(depthMap);
                            float depthNear = ((Float) PhotoModule.this.mPreviewResult.get(DepthUtil.bokeh_gdepth_near)).floatValue();
                            float depthFar = ((Float) PhotoModule.this.mPreviewResult.get(DepthUtil.bokeh_gdepth_far)).floatValue();
                            byte depthFormat = ((Byte) PhotoModule.this.mPreviewResult.get(DepthUtil.bokeh_gdepth_format)).byteValue();
                            gDepth.setNear(depthNear);
                            gDepth.setFar(depthFar);
                            gDepth.setFormat(depthFormat);
                            Tag access$5003 = PhotoModule.TAG;
                            StringBuilder stringBuilder5 = new StringBuilder();
                            stringBuilder5.append("westalgo depth_near: ");
                            stringBuilder5.append(depthNear);
                            stringBuilder5.append("depth_far: ");
                            stringBuilder5.append(depthFar);
                            stringBuilder5.append("depth_format:");
                            stringBuilder5.append(depthFormat);
                            Log.d(access$5003, stringBuilder5.toString());
                            Log.v(PhotoModule.TAG, "save addXmpImage");
                            exif.addMakeAndModelTag();
                            PhotoModule.this.getServices().getMediaSaver().addXmpImage((byte[]) bokehBytes.get(0), gImage, gDepth, title2, namedEntity.date, null, width, height, orientation, exifInterface, XmpUtil.GDEPTH_TYPE, PhotoModule.this.mOnMediaSavedListener, PhotoModule.this.mContentResolver, "jpeg");
                            Integer num2 = exifHeight;
                            num = exifWidth;
                            orientation2 = orientation;
                        }
                    } else {
                        num = exifWidth;
                        orientation2 = orientation;
                        PhotoModule.this.getServices().getMediaSaver().addImage(bArr, title2, namedEntity.date, this.mLocation, width, height, orientation2, exif, PhotoModule.this.mOnMediaSavedListener, PhotoModule.this.mContentResolver);
                    }
                }
                DEPTH_H = orientation2;
            }
            PhotoModule.this.getServices().getRemoteShutterListener().onPictureTaken(bArr);
            PhotoModule.this.mActivity.updateStorageSpaceAndHint(null);
        }
    }

    protected class LongshotPictureCallback implements CameraPictureCallback {
        Location mLocation;
        private short mLongshotCount = (short) 0;
        private NamedEntity mName;

        public LongshotPictureCallback(Location loc) {
            this.mLocation = loc;
        }

        public void onPictureTaken(byte[] originalJpegData, final CameraProxy camera) {
            Log.w(PhotoModule.TAG, "OnPictureTaken in burst");
            if (!PhotoModule.this.mPaused && !PhotoModule.this.mHasBurstStoped) {
                if (PhotoModule.this.mCameraState != 8 && PhotoModule.this.mCameraState != 9) {
                    Log.w(PhotoModule.TAG, "stop burst in picture taken");
                    PhotoModule.this.stopBurst();
                } else if (PhotoModule.this.mActivity.getStorageSpaceBytes() <= Storage.LOW_STORAGE_THRESHOLD_BYTES) {
                    Tag access$500 = PhotoModule.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Not enough space or storage not ready. remaining=");
                    stringBuilder.append(PhotoModule.this.mActivity.getStorageSpaceBytes());
                    Log.i(access$500, stringBuilder.toString());
                    PhotoModule.this.mVolumeButtonClickedFlag = false;
                    PhotoModule.this.stopBurst();
                    ToastUtil.showToast(PhotoModule.this.mActivity, PhotoModule.this.mActivity.getString(R.string.low_memory_toast), 1);
                } else {
                    if (PhotoModule.this.mCameraState == 8) {
                        PhotoModule.this.setCameraState(9);
                    }
                    PhotoModule photoModule = PhotoModule.this;
                    photoModule.mReceivedBurstNum++;
                    Log.v(PhotoModule.TAG, "update burst count");
                    PhotoModule.this.mUI.updateBurstCount(PhotoModule.this.mReceivedBurstNum);
                    int mode = PhotoModule.this.mAudioManager.getRingerMode();
                    if (Keys.isShutterSoundOn(PhotoModule.this.mAppController.getSettingsManager()) && mode == 2 && PhotoModule.this.mSoundPlayer != null) {
                        PhotoModule.this.mStreamId = PhotoModule.this.mSoundPlayer.play(R.raw.camera_burst, 1.0f, 98);
                    }
                    new AsyncTask<byte[], Void, byte[]>() {
                        /* Access modifiers changed, original: protected|varargs */
                        public byte[] doInBackground(byte[]... data) {
                            byte[] originalJpegData = data[null];
                            Size size = PhotoModule.this.mCameraSettings.getCurrentPhotoSize();
                            originalJpegData = PhotoModule.this.convertN21ToJpeg(originalJpegData, size.width(), size.height());
                            if (camera.getCharacteristics().isFacingFront() && PhotoModule.this.isNeedMirrorSelfie()) {
                                originalJpegData = PhotoModule.this.aftMirrorJpeg(originalJpegData);
                            }
                            try {
                                if (!Keys.isAlgorithmsOn(PhotoModule.this.mActivity.getSettingsManager()) || PhotoModule.this.isDepthEnabled() || PhotoModule.this.mBurstResultQueue.isEmpty()) {
                                    return originalJpegData;
                                }
                                return PhotoModule.this.addExifTags(originalJpegData, (TotalCaptureResult) PhotoModule.this.mBurstResultQueue.removeFirst(), camera.getCharacteristics().isFacingFront(), PhotoModule.this.mJpegRotation);
                            } catch (NoSuchElementException e) {
                                e.printStackTrace();
                                return originalJpegData;
                            }
                        }

                        /* Access modifiers changed, original: protected */
                        public void onPostExecute(byte[] originalJpegData) {
                            LongshotPictureCallback.this.updateExifAndSave(Exif.getExif(originalJpegData), originalJpegData, camera);
                        }
                    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new byte[][]{originalJpegData});
                    if (PhotoModule.this.mReceivedBurstNum >= PhotoModule.BURST_MAX) {
                        PhotoModule.this.setCameraState(10);
                        PhotoModule.this.mHandler.postDelayed(new Runnable() {
                            public void run() {
                                PhotoModule.this.stopBurst();
                            }
                        }, 150);
                    }
                }
            }
        }

        /* Access modifiers changed, original: protected */
        public void updateExifAndSave(ExifInterface exif, byte[] originalJpegData, final CameraProxy camera) {
            if (this.mLongshotCount == (short) 0) {
                this.mName = PhotoModule.this.mNamedImages.getNextNameEntity();
            }
            final Map<String, Object> externalBundle = new HashMap();
            externalBundle.put(ExternalExifInterface.BURST_SHOT_ID, Integer.valueOf(hashCode()));
            String str = ExternalExifInterface.BURST_SHOT_INDEX;
            short s = this.mLongshotCount;
            this.mLongshotCount = (short) (s + 1);
            externalBundle.put(str, Short.valueOf(s));
            Tag access$500 = PhotoModule.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("long shot taken for ");
            stringBuilder.append(this.mLongshotCount);
            Log.w(access$500, stringBuilder.toString());
            if (PhotoModule.this.mShouldResizeTo16x9) {
                ResizeBundle dataBundle = new ResizeBundle();
                dataBundle.jpegData = originalJpegData;
                dataBundle.targetAspectRatio = 1.7777778f;
                dataBundle.exif = exif;
                new AsyncTask<ResizeBundle, Void, ResizeBundle>() {
                    /* Access modifiers changed, original: protected|varargs */
                    public ResizeBundle doInBackground(ResizeBundle... resizeBundles) {
                        return PhotoModule.this.cropJpegDataToAspectRatio(resizeBundles[0]);
                    }

                    /* Access modifiers changed, original: protected */
                    public void onPostExecute(ResizeBundle result) {
                        LongshotPictureCallback.this.saveFinalPhoto(result.jpegData, LongshotPictureCallback.this.mName, result.exif, camera, externalBundle);
                    }
                }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new ResizeBundle[]{dataBundle});
                return;
            }
            saveFinalPhoto(originalJpegData, this.mName, exif, camera, externalBundle);
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
            if (PhotoModule.this.mCameraCapabilities.supports(Feature.ZOOM)) {
                zoomValue = PhotoModule.this.mCameraSettings.getCurrentZoomRatio();
            }
            float zoomValue2 = zoomValue;
            boolean hdrOn = SceneMode.HDR == PhotoModule.this.mSceneMode;
            String flashSetting = PhotoModule.this.mActivity.getSettingsManager().getString(PhotoModule.this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE);
            boolean gridLinesOn = Keys.areGridLinesOn(PhotoModule.this.mActivity.getSettingsManager());
            UsageStatistics instance = UsageStatistics.instance();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(namedEntity.title);
            stringBuilder.append(Storage.JPEG_POSTFIX);
            instance.photoCaptureDoneEvent(10000, stringBuilder.toString(), exifInterface, PhotoModule.this.isCameraFrontFacing(), hdrOn, zoomValue2, flashSetting, gridLinesOn, Float.valueOf((float) PhotoModule.this.mTimerDuration), PhotoModule.this.mShutterTouchCoordinate, Boolean.valueOf(PhotoModule.this.mVolumeButtonClickedFlag));
            PhotoModule.this.mShutterTouchCoordinate = null;
            PhotoModule.this.mVolumeButtonClickedFlag = false;
            int orientation2;
            if (PhotoModule.this.mIsImageCaptureIntent) {
                orientation2 = orientation;
                PhotoModule.this.mJpegImageData = bArr;
                if (PhotoModule.this.mQuickCapture) {
                    PhotoModule.this.onCaptureDone();
                } else {
                    Log.v(PhotoModule.TAG, "showing UI");
                    PhotoModule.this.mUI.showCapturedImageForReview(bArr, orientation2, PhotoModule.this.mMirror);
                }
            } else {
                int width;
                int height;
                Integer exifWidth = exifInterface.getTagIntValue(ExifInterface.TAG_PIXEL_X_DIMENSION);
                Integer exifHeight = exifInterface.getTagIntValue(ExifInterface.TAG_PIXEL_Y_DIMENSION);
                if (!PhotoModule.this.mShouldResizeTo16x9 || exifWidth == null || exifHeight == null) {
                    Size s = PhotoModule.this.mCameraSettings.getCurrentPhotoSize();
                    if ((PhotoModule.this.mJpegRotation + orientation) % MediaProviderUtils.ROTATION_180 == 0) {
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
                if (PhotoModule.this.mDebugUri != null) {
                    PhotoModule.this.saveToDebugUri(bArr);
                    if (title != null) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(PhotoModule.DEBUG_IMAGE_PREFIX);
                        stringBuilder2.append(title);
                        title = stringBuilder2.toString();
                    }
                }
                if (title == null) {
                    Log.e(PhotoModule.TAG, "Unbalanced name/data pair");
                    orientation2 = orientation;
                } else {
                    long date2;
                    if (date == -1) {
                        date2 = PhotoModule.this.mCaptureStartTime;
                    } else {
                        date2 = date;
                    }
                    Integer exifHeight2;
                    Integer exifWidth2;
                    if (PhotoModule.this.mHeading >= 0) {
                        ExifTag directionRefTag = exifInterface.buildTag(ExifInterface.TAG_GPS_IMG_DIRECTION_REF, "M");
                        exifHeight2 = exifHeight;
                        exifWidth2 = exifWidth;
                        ExifTag directionTag = exifInterface.buildTag(ExifInterface.TAG_GPS_IMG_DIRECTION, new Rational((long) PhotoModule.this.mHeading, 1));
                        exifInterface.setTag(directionRefTag);
                        exifInterface.setTag(directionTag);
                    } else {
                        exifHeight2 = exifHeight;
                        exifWidth2 = exifWidth;
                    }
                    if (externalInfos != null) {
                        exifInterface.setTag(exifInterface.buildTag(ExifInterface.TAG_USER_COMMENT, CameraUtil.serializeToJson(externalInfos)));
                    }
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(title);
                    stringBuilder3.append("_BURST");
                    stringBuilder3.append(System.currentTimeMillis());
                    PhotoModule.this.getServices().getMediaSaver().addImage(bArr, stringBuilder3.toString(), date2, this.mLocation, width, height, orientation, exifInterface, new BurstShotSaveListener(this.mLongshotCount), PhotoModule.this.mContentResolver);
                }
            }
            PhotoModule.this.getServices().getRemoteShutterListener().onPictureTaken(bArr);
            PhotoModule.this.mActivity.updateStorageSpaceAndHint(null);
        }
    }

    private final class PostViewPictureCallback implements CameraPictureCallback {
        private PostViewPictureCallback() {
        }

        /* synthetic */ PostViewPictureCallback(PhotoModule x0, AnonymousClass1 x1) {
            this();
        }

        public void onPictureTaken(byte[] data, CameraProxy camera) {
            PhotoModule.this.mPostViewPictureCallbackTime = System.currentTimeMillis();
            Tag access$500 = PhotoModule.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mShutterToPostViewCallonbackTime = ");
            stringBuilder.append(PhotoModule.this.mPostViewPictureCallbackTime - PhotoModule.this.mShutterCallbackTime);
            stringBuilder.append("ms");
            Log.v(access$500, stringBuilder.toString());
        }
    }

    private final class RawPictureCallback implements CameraPictureCallback {
        private RawPictureCallback() {
        }

        /* synthetic */ RawPictureCallback(PhotoModule x0, AnonymousClass1 x1) {
            this();
        }

        public void onPictureTaken(byte[] rawData, CameraProxy camera) {
            Tag access$500 = PhotoModule.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("rawData size:");
            stringBuilder.append(rawData != null ? Integer.valueOf(rawData.length) : "0");
            Log.v(access$500, stringBuilder.toString());
            PhotoModule.this.mRawPictureCallbackTime = System.currentTimeMillis();
            access$500 = PhotoModule.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("mShutterToRawCallbackTime = ");
            stringBuilder.append(PhotoModule.this.mRawPictureCallbackTime - PhotoModule.this.mShutterCallbackTime);
            stringBuilder.append("ms");
            Log.v(access$500, stringBuilder.toString());
        }
    }

    private final class ShutterCallback implements CameraShutterCallback {
        private boolean isFromLongshot;
        private boolean mIsfirst;
        private final boolean mNeedsAnimation;

        public ShutterCallback(PhotoModule photoModule, boolean needsAnimation, boolean fromLongshot) {
            this(needsAnimation);
            this.isFromLongshot = fromLongshot;
        }

        public ShutterCallback(boolean needsAnimation) {
            this.mIsfirst = true;
            this.isFromLongshot = false;
            this.mNeedsAnimation = needsAnimation;
        }

        public void onShutter(CameraProxy camera) {
            PhotoModule.this.mShutterCallbackTime = System.currentTimeMillis();
            boolean z = this.isFromLongshot;
            PhotoModule.this.mShutterLag = PhotoModule.this.mShutterCallbackTime - PhotoModule.this.mCaptureStartTime;
            Tag access$500 = PhotoModule.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mShutterLag = ");
            stringBuilder.append(PhotoModule.this.mShutterLag);
            stringBuilder.append("ms");
            Log.v(access$500, stringBuilder.toString());
            if (this.mNeedsAnimation && this.mIsfirst) {
                this.mIsfirst = false;
                PhotoModule.this.mActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        PhotoModule.this.animateAfterShutter();
                    }
                });
            }
            if (PhotoModule.this.isOptimizeCapture) {
                PhotoModule.this.mAppController.setShutterEnabled(true);
            }
        }
    }

    private String getRealFilePath(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        String scheme = uri.getScheme();
        String data = null;
        if (scheme == null) {
            data = uri.getPath();
        } else if ("file".equals(scheme)) {
            data = uri.getPath();
        } else if ("content".equals(scheme)) {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex("_data");
                    if (index > -1) {
                        data = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return data;
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
                        PhotoModule.this.checkDisplayRotation();
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

    public PhotoModule(AppController app) {
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
        this.mDrCheckResult = new ArrayList();
        this.mFinalDrCheckResult = new ArrayList();
        this.mAddedNumber = 0;
        this.mFrameCount = 0;
        this.mNeedHdrBurst = false;
        this.mInHdrProcess = false;
        this.mIsAddWaterMark = false;
        this.mAddedImages = new ArrayList();
        this.mBurstResultQueue = new LinkedList();
        this.mEvLowLightBack = new float[5];
        this.mEvLowLightFront = new float[5];
        this.mEvNormalBack = new float[5];
        this.mEvNormalFront = new float[5];
        this.mHdrEnabled = false;
        this.mDropFrameIndex = NotificationManagerCompat.IMPORTANCE_UNSPECIFIED;
        this.mHdrState = "auto";
        this.mNeedFocus = false;
        this.mIsFrontFlashEnd = true;
        this.mHandler = new MainHandler(this);
        this.mFrontFlashHandler = new FrontFlashHandler(this);
        this.mLData = new float[3];
        this.mGData = new float[3];
        this.mMData = new float[3];
        this.mR = new float[16];
        this.mHeading = -1;
        this.mIsInIntentReviewUI = false;
        this.mCameraPreviewParamsReady = false;
        this.mUnderLowMemory = false;
        this.mZoomFlashLock = null;
        this.mEvoFlashLock = null;
        this.mIsCameraOpened = false;
        this.mOnMediaSavedListener = new OnMediaSavedListener() {
            public void onMediaSaved(Uri uri) {
                PhotoModule.this.mActivity.resetMotionStatus();
                if (uri != null) {
                    int notifyAction = 2;
                    if (PhotoModule.this.isOptimizeCapture) {
                        notifyAction = 2 | 4;
                    }
                    PhotoModule.this.mActivity.notifyNewMedia(uri, notifyAction);
                    return;
                }
                ToastUtil.showToast(PhotoModule.this.mActivity, PhotoModule.this.mActivity.getString(R.string.error_when_saving_picture_toast), 0);
                ToastUtil.showToast(PhotoModule.this.mActivity, PhotoModule.this.mActivity.getString(R.string.photos_were_not_saved_to_the_device_toast), 0);
            }
        };
        this.mBurstShotCheckQueue = new BurstShotCheckQueue();
        this.mBurstShotNotifyHelpTip = false;
        this.mManualEtValue = "0";
        this.mShouldResizeTo16x9 = false;
        this.mMainHandlerCallback = null;
        this.mCameraCallback = new ButtonCallback() {
            public void onStateChanged(int state) {
                StringBuilder stringBuilder;
                if (PhotoModule.this.getModuleId() == 15) {
                    Size picSize;
                    if (!PhotoModule.this.isCameraFrontFacing()) {
                        picSize = new Size(3120, 3120);
                    } else if (CustomUtil.getInstance().isPanther()) {
                        picSize = new Size(2448, 2448);
                    } else {
                        picSize = new Size(1944, 1944);
                    }
                    Log.d(PhotoModule.TAG, " update pic size");
                    List<Size> sizePics = PhotoModule.this.mCameraCapabilities.getSupportedPhotoSizes();
                    if (PhotoModule.this.mCameraDevice != null) {
                        CameraPictureSizesCacher.updateSizesForCamera(PhotoModule.this.mAppController.getAndroidContext(), PhotoModule.this.mCameraDevice.getCameraId(), sizePics);
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(picSize.width());
                        stringBuilder.append("x");
                        stringBuilder.append(picSize.height());
                        SettingsUtil.setCameraPictureSize(stringBuilder.toString(), sizePics, PhotoModule.this.mCameraSettings, PhotoModule.this.mCameraDevice.getCameraId());
                    }
                    PhotoModule.this.mCameraSettings.setPhotoSize(picSize);
                    PhotoModule.this.mCameraDevice.applySettings(PhotoModule.this.mCameraSettings);
                    PhotoModule.this.mCameraSettings = PhotoModule.this.mCameraDevice.getSettings();
                }
                if (!PhotoModule.this.mPaused && !PhotoModule.this.mAppController.getCameraProvider().waitingForCamera()) {
                    SettingsManager settingsManager = PhotoModule.this.mActivity.getSettingsManager();
                    if (Keys.isCameraBackFacing(settingsManager, SettingsManager.SCOPE_GLOBAL) && Keys.requestsReturnToHdrPlus(settingsManager, PhotoModule.this.mAppController.getModuleScope())) {
                        PhotoModule.this.switchToGcamCapture();
                        return;
                    }
                    PhotoModule.this.mPendingSwitchCameraId = state;
                    Tag access$500 = PhotoModule.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Start to switch camera. cameraId=");
                    stringBuilder.append(state);
                    Log.d(access$500, stringBuilder.toString());
                    PhotoModule.this.switchCamera();
                }
            }
        };
        this.mHdrPlusCallback = new ButtonCallback() {
            public void onStateChanged(int state) {
                SettingsManager settingsManager = PhotoModule.this.mActivity.getSettingsManager();
                if (GcamHelper.hasGcamAsSeparateModule()) {
                    settingsManager.setToDefault(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID);
                    PhotoModule.this.switchToGcamCapture();
                    return;
                }
                if (Keys.isHdrOn(settingsManager)) {
                    settingsManager.set(PhotoModule.this.mAppController.getCameraScope(), Keys.KEY_SCENE_MODE, PhotoModule.this.mCameraCapabilities.getStringifier().stringify(SceneMode.HDR));
                } else {
                    settingsManager.set(PhotoModule.this.mAppController.getCameraScope(), Keys.KEY_SCENE_MODE, PhotoModule.this.mCameraCapabilities.getStringifier().stringify(SceneMode.AUTO));
                }
                if (PhotoModule.this.mCameraState == 5 || PhotoModule.this.mCameraState == 2 || PhotoModule.this.mCameraState == 6) {
                    PhotoModule.this.setCameraState(1);
                    PhotoModule.this.mFocusManager.cancelAutoFocus();
                }
                PhotoModule.this.updateSceneMode();
            }
        };
        this.mLowlightCallback = new ButtonCallback() {
            public void onStateChanged(int state) {
                if (Keys.isLowlightOn(PhotoModule.this.mAppController.getSettingsManager()) && PhotoModule.this.isNightToastShow() && CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_NIGHT_MODE_TOAST_ON, false)) {
                    ToastUtil.showToast(PhotoModule.this.mActivity, PhotoModule.this.mActivity.getString(R.string.night_mode_on_toast), 0);
                }
                if (PhotoModule.this.mCameraState == 5 || PhotoModule.this.mCameraState == 2 || PhotoModule.this.mCameraState == 6) {
                    PhotoModule.this.setCameraState(1);
                    PhotoModule.this.mFocusManager.cancelAutoFocus();
                }
                PhotoModule.this.updateVisidionMode();
            }
        };
        this.mCancelCallback = new OnClickListener() {
            public void onClick(View v) {
                PhotoModule.this.onCaptureCancelled();
            }
        };
        this.mDoneCallback = new OnClickListener() {
            public void onClick(View v) {
                PhotoModule.this.onCaptureDone();
            }
        };
        this.mRetakeCallback = new OnClickListener() {
            public void onClick(View v) {
                PhotoModule.this.mActivity.getCameraAppUI().transitionToIntentCaptureLayout();
                PhotoModule.this.onCaptureRetake();
            }
        };
        this.mLongshotShutterCallback = new ShutterCallback(this, false, true);
        this.curOritation = -1;
        this.mThumbnailCache = null;
        this.mCameraFinalPreviewCallback = new CameraFinalPreviewCallback() {
            public void onFinalPreviewReturn() {
                boolean isPhotoModule = PhotoModule.this.getModuleId() == 4 || PhotoModule.this.getModuleId() == 15;
                if (!isPhotoModule) {
                    return;
                }
                if (PhotoModule.this.isImageCaptureIntent()) {
                    Log.d(PhotoModule.TAG, "isImageCaptureIntent, return for CTS");
                } else if (PhotoModule.this.mAppController != null && !PhotoModule.this.isInBurstshot() && !PhotoModule.this.mCameraSettings.isMotionOn()) {
                    long time = System.currentTimeMillis();
                    Bitmap preview = PhotoModule.this.mAppController.getCameraAppUI().getPreviewBitmap();
                    if (preview != null) {
                        int height = preview.getHeight();
                        int i = 1000 == preview.getHeight() ? MotionPictureHelper.FRAME_HEIGHT_9 : 1130 == preview.getHeight() ? 960 : 1280;
                        final Bitmap bitmap = CameraUtil.rotateAndMirror(BlurUtil.cropBitmap(preview, height - i), (360 - PhotoModule.this.mOrientation) % 360, false);
                        Tag access$500 = PhotoModule.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("convert time = ");
                        stringBuilder.append(System.currentTimeMillis() - time);
                        Log.d(access$500, stringBuilder.toString());
                        if (PhotoModule.this.mFinalDrCheckResult.size() > 1 || PhotoModule.this.isBeautyOnGP()) {
                            Log.d(PhotoModule.TAG, "HDR on, will update the preview on onPictureTaken");
                            PhotoModule.this.mThumbnailCache = bitmap;
                            return;
                        }
                        PhotoModule.this.mThumbnailCache = null;
                        new Thread(new Runnable() {
                            public void run() {
                                long dateTaken = PhotoModule.this.mNamedImages.getNextNameEntityForThumbnailOnly().date;
                                ProcessingMedia media = ProcessingMediaManager.getInstance(PhotoModule.this.mActivity).add(dateTaken);
                                if (media != null) {
                                    media.setThumbnailPath(ProviderUtils.save2Private(PhotoModule.this.mActivity, bitmap));
                                    media.setThumbnailBitmap(bitmap);
                                }
                                final TE te = new TE(dateTaken, bitmap);
                                PhotoModule.this.mActivity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        PhotoModule.this.mAppController.getCameraAppUI().updatePeekThumbUri(null);
                                        PhotoModule.this.mAppController.getCameraAppUI().updatePeekThumbBitmap(te.bitmap);
                                    }
                                });
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
        this.mHideProgressRunnable = new Runnable() {
            public void run() {
            }
        };
        this.mPictureTaken = new PictureTaskListener();
        this.mProcessingQueueListener = new ProcessingQueueListener() {
            public void onQueueEmpty() {
                if (PhotoModule.this.mPaused) {
                    PhotoModule.this.mHandler.removeCallbacksAndMessages(null);
                }
            }
        };
        this.mLockedEvoIndex = 0;
        this.mCropRegion = new Rect[4];
        this.mOriginalCropRegion = new Rect[4];
        this.seekListener = new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                PhotoModule.this.mActivity.getCameraAppUI().setBeautySeek((float) seekBar.getProgress());
                if (PhotoModule.this.mAppController.getCurrentModuleIndex() == 4 || PhotoModule.this.mAppController.getCurrentModuleIndex() == 15) {
                    if (PhotoModule.this.isBeautyShow() || PhotoModule.this.isMaskSelected()) {
                        PhotoModule.this.mActivity.getButtonManager().hideButtons(0);
                        PhotoModule.this.mActivity.getButtonManager().hideButtons(5);
                        PhotoModule.this.mActivity.getButtonManager().hideButtons(18);
                    } else {
                        PhotoModule.this.mActivity.getButtonManager().showButtons(0);
                        if (PhotoModule.this.isMotionShow()) {
                            PhotoModule.this.mActivity.getButtonManager().showButtons(18);
                        }
                        if (PhotoModule.this.isHdrShow()) {
                            PhotoModule.this.mActivity.getButtonManager().showButtons(5);
                        }
                    }
                }
                if (PhotoModule.this.mAppController.getCurrentModuleIndex() == 4 || PhotoModule.this.mAppController.getCurrentModuleIndex() == 15) {
                    if (PhotoModule.this.isBeautyShow()) {
                        PhotoModule.this.mActivity.getButtonManager().showBeauty2Button();
                    } else {
                        PhotoModule.this.mActivity.getButtonManager().hideBeauty2Button();
                    }
                }
                float chooseValue = PhotoModule.this.mActivity.getCameraAppUI().getBeautySeek() / 20.0f;
                if (chooseValue > 0.0f) {
                    PhotoModule.this.mAppController.getCameraAppUI().setBeautyEnable(true);
                    PhotoModule.this.mAppController.getButtonManager().setBeautyState(1);
                } else {
                    PhotoModule.this.mAppController.getCameraAppUI().setBeautyEnable(false);
                    PhotoModule.this.mAppController.getButtonManager().setBeautyState(0);
                }
                PhotoModule.this.updateBeautySeek(chooseValue);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
        this.mIsGlMode = false;
        this.mGcamModeIndex = app.getAndroidContext().getResources().getInteger(R.integer.camera_mode_gcam);
    }

    public String getPeekAccessibilityString() {
        return this.mAppController.getAndroidContext().getResources().getString(R.string.photo_accessibility_peek);
    }

    public String getModuleStringIdentifier() {
        return PHOTO_MODULE_STRING_ID;
    }

    /* Access modifiers changed, original: protected */
    public PhotoUI getPhotoUI() {
        if (this.mUI == null) {
            this.mUI = new PhotoUI(this.mActivity, this, this.mActivity.getModuleLayoutRoot());
        }
        return this.mUI;
    }

    public void init(CameraActivity activity, boolean isSecureCamera, boolean isCaptureIntent) {
        this.mActivity = activity;
        this.mAppController = this.mActivity;
        this.mActivity.getButtonManager().mBeautyEnable = true;
        this.mUI = getPhotoUI();
        this.mActivity.setPreviewStatusListener(this.mUI);
        initEvValues();
        this.mCameraId = this.mActivity.getSettingsManager().getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID).intValue();
        if (isDepthEnabled() && getModuleId() == this.mAppController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_photo) && this.mCameraId == 0) {
            this.mCameraId = 3;
        }
        this.mContentResolver = this.mActivity.getContentResolver();
        this.mIsImageCaptureIntent = isImageCaptureIntent();
        this.mQuickCapture = this.mActivity.getIntent().getBooleanExtra(EXTRA_QUICK_CAPTURE, false);
        this.mSensorManager = (SensorManager) this.mActivity.getSystemService("sensor");
        this.mAudioManager = (AudioManager) this.mActivity.getSystemService("audio");
        this.mUI.setCountdownFinishedListener(this);
        this.mCountdownSoundPlayer = new SoundPlayer(this.mAppController.getAndroidContext());
        this.mSoundPlayer = new SoundPlayer(this.mAppController.getAndroidContext());
        View cancelButton = this.mActivity.findViewById(R.id.shutter_cancel_button);
        this.mMoreEnterToggleButton = (MultiToggleImageButton) this.mActivity.findViewById(R.id.more_enter_toggle_button);
        cancelButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                PhotoModule.this.cancelCountDown();
            }
        });
        ProcessingMediaManager.getInstance(this.mActivity).setProcessingQueueListener(this.mProcessingQueueListener);
    }

    private void initEvValues() {
        ImageRefiner imageRefiner = new ImageRefiner();
        int ret = imageRefiner.initialize(1000, 1000, 3, 3, "YUV420_SEMIPLANAR", false);
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("initialize ret = ");
        stringBuilder.append(ret);
        Log.d(tag, stringBuilder.toString());
        ret = imageRefiner.getEVs(true, true, this.mEvLowLightBack);
        tag = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("mEvLowLightBack ret = ");
        stringBuilder2.append(ret);
        Log.d(tag, stringBuilder2.toString());
        ret = imageRefiner.getEVs(true, false, this.mEvLowLightFront);
        tag = TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("mEvLowLightFront ret = ");
        stringBuilder3.append(ret);
        Log.d(tag, stringBuilder3.toString());
        ret = imageRefiner.getEVs(false, true, this.mEvNormalBack);
        tag = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("mEvNormalBack ret = ");
        stringBuilder.append(ret);
        Log.d(tag, stringBuilder.toString());
        ret = imageRefiner.getEVs(false, false, this.mEvNormalFront);
        tag = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("mEvNormalFront ret = ");
        stringBuilder.append(ret);
        Log.d(tag, stringBuilder.toString());
        imageRefiner.finish();
    }

    private float[] getEvValue(boolean lowLightMode) {
        float[] evValue = new float[5];
        if (lowLightMode && !isCameraFrontFacing()) {
            return this.mEvLowLightBack;
        }
        if (lowLightMode && isCameraFrontFacing()) {
            return this.mEvLowLightFront;
        }
        if (lowLightMode || isCameraFrontFacing()) {
            return this.mEvNormalFront;
        }
        return this.mEvNormalBack;
    }

    private void cancelCountDown() {
        this.mAppController.getCameraAppUI().resetRings();
        this.mAppController.getCameraAppUI().setStereoModeStripViewSlideable(true);
        this.mActivity.getButtonManager().mBeautyEnable = true;
        if (this.mUI.isCountingDown()) {
            if (this.mUI.isCountingDown()) {
                this.mFocusManager.sendMessage();
                this.mFocusManager.noEight();
                this.mUI.cancelCountDown();
            }
            if (!this.mIsInIntentReviewUI) {
                if (this.mLastMaskEnable) {
                    this.mLastMaskEnable = false;
                    this.mActivity.getButtonManager().setEffectWrapperVisible(0);
                }
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
                        PhotoModule.this.startFaceDetection();
                    }
                }, 1500);
            } else {
                startFaceDetection();
            }
            BoostUtil.getInstance().releaseCpuLock();
            if (this.mIsGlMode) {
                this.mActivity.getCameraAppUI().hideImageCover();
                if (this.mActivity.getCameraAppUI().getBeautyEnable()) {
                    this.mActivity.getCameraAppUI().getCameraGLSurfaceView().queueEvent(new Runnable() {
                        public void run() {
                            PhotoModule.this.mActivity.getButtonManager().setSeekbarProgress((int) PhotoModule.this.mActivity.getCameraAppUI().getBeautySeek());
                            PhotoModule.this.updateBeautySeek(PhotoModule.this.mActivity.getCameraAppUI().getBeautySeek() / 20.0f);
                        }
                    });
                }
                if (this.mActivity.getCameraAppUI().getEffectEnable()) {
                    this.mActivity.getCameraAppUI().getCameraGLSurfaceView().queueEvent(new Runnable() {
                        public void run() {
                            if (TextUtils.isEmpty(PhotoModule.this.mActivity.getCameraAppUI().getCurrSelect())) {
                                BeaurifyJniSdk.preViewInstance().nativeDisablePackage();
                            } else {
                                BeaurifyJniSdk.preViewInstance().nativeChangePackage(PhotoModule.this.mActivity.getCameraAppUI().getCurrSelect());
                            }
                        }
                    });
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
                            Keys.setLocation(PhotoModule.this.mActivity.getSettingsManager(), selected, PhotoModule.this.mActivity.getLocationManager());
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
                    PhotoModule.this.mActivity.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_PICTURE_SIZE_BACK, SettingsUtil.sizeToSetting(size4x3ToSelect));
                } else if (newAspectRatio == AspectRatio.ASPECT_RATIO_16x9) {
                    PhotoModule.this.mActivity.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_PICTURE_SIZE_BACK, SettingsUtil.sizeToSetting(size16x9ToSelect));
                }
                PhotoModule.this.mActivity.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_USER_SELECTED_ASPECT_RATIO, true);
                String aspectRatio = PhotoModule.this.mActivity.getSettingsManager().getString(SettingsManager.SCOPE_GLOBAL, Keys.KEY_USER_SELECTED_ASPECT_RATIO);
                Tag access$500 = PhotoModule.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("aspect ratio after setting it to true=");
                stringBuilder.append(aspectRatio);
                Log.e(access$500, stringBuilder.toString());
                if (newAspectRatio != currentAspectRatio) {
                    Log.i(PhotoModule.TAG, "changing aspect ratio from dialog");
                    PhotoModule.this.stopPreview();
                    PhotoModule.this.startPreview();
                    PhotoModule.this.mUI.setRunnableForNextFrame(dialogHandlingFinishedRunnable);
                    return;
                }
                PhotoModule.this.mHandler.post(dialogHandlingFinishedRunnable);
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
                    PhotoModule.this.mIsCameraOpened = true;
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
                int holdTime = 300;
                int delayTime = 0;
                if (cameraAppUI.getCameraGLSurfaceView().getVisibility() == 0) {
                    holdTime = MediaPlayer2.MEDIA_INFO_TIMED_TEXT_ERROR;
                    delayTime = 300;
                    if (15 == this.mAppController.getCurrentModuleIndex()) {
                        holdTime = AnimationManager.SLIDE_DURATION;
                        delayTime = 400;
                    }
                }
                cameraAppUI.playCameraSwitchAnimation(400, holdTime);
                if (delayTime > 0) {
                    cameraAppUI.getCameraGLSurfaceView().postDelayed(new Runnable(400) {
                        public void run() {
                            PhotoModule.this.switchCameraDefault(400);
                        }
                    }, (long) delayTime);
                } else {
                    switchCameraDefault(400);
                }
            }
        }
    }

    private void switchCameraDefault(int playTime) {
        BoostUtil.getInstance().acquireCpuLock();
        setCameraState(4);
        if (getModuleId() != this.mAppController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_pro)) {
            cancelCountDown();
        }
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Start to switch camera. id=");
        stringBuilder.append(this.mPendingSwitchCameraId);
        Log.i(tag, stringBuilder.toString());
        stopPreview();
        closeCamera();
        if (Keys.isDepthOn(this.mActivity.getSettingsManager()) && this.mPendingSwitchCameraId == 0 && getModuleId() == this.mAppController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_photo)) {
            this.mCameraId = 3;
        } else {
            this.mCameraId = this.mPendingSwitchCameraId;
        }
        settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID, this.mPendingSwitchCameraId);
        if ((this.mIsGlMode || this.mActivity.getCameraAppUI().getCameraGLSurfaceView().getVisibility() == 0) && this.mCameraId != 3) {
            boolean needChangeResolution = com.hmdglobal.app.camera.util.ResolutionUtil.getRightResolutionById(getModuleId(), this.mCameraId, this.mActivity);
            final CameraSurfaceView surfaceView = this.mActivity.getCameraAppUI().getCameraGLSurfaceView();
            surfaceView.switchCamera(this.mCameraId);
            surfaceView.queueEvent(new Runnable() {
                public void run() {
                    surfaceView.surfaceChanged(null, 0, surfaceView.getWidth(), surfaceView.getHeight());
                    if (PhotoModule.this.mActivity != null) {
                        PhotoModule.this.mActivity.runOnUiThread(new Runnable() {
                            public void run() {
                                PhotoModule.this.mActivity.getCameraAppUI().setTextureViewVisible(8);
                                if (PhotoModule.this.mActivity.getCameraAppUI().getCameraGLSurfaceView().getVisibility() != 0) {
                                    surfaceView.setVisibility(0);
                                }
                            }
                        });
                    }
                }
            });
        } else {
            if (this.mCameraId == 1) {
                this.mActivity.getButtonManager().setEffectWrapperVisible(8);
            }
            if (Keys.isDepthOn(this.mActivity.getSettingsManager())) {
                this.mActivity.getCameraAppUI().setTextureViewVisible(0);
                this.mActivity.getCameraAppUI().getCameraGLSurfaceView().setVisibility(8);
            } else {
                this.mActivity.getCameraAppUI().showOrHideGLSurface(false);
            }
        }
        requestCameraOpen();
        this.mUI.clearFaces();
        if (this.mFocusManager != null) {
            this.mUI.clearFocus();
            this.mFocusManager.removeMessages();
        }
        this.mMirror = isCameraFrontFacing();
        this.mFocusManager.setMirror(this.mMirror);
    }

    private void requestCameraOpen() {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("requestCameraOpen ");
        stringBuilder.append(this.mCameraId);
        Log.w(tag, stringBuilder.toString());
        if (this.mCameraId == 0) {
            setMorphoCamera(true);
        } else if (this.mCameraId == 1) {
            setMorphoCamera(false);
        }
        this.mActivity.getCameraProvider().requestCamera(this.mCameraId, GservicesHelper.useCamera2ApiThroughPortabilityLayer(this.mActivity.getAndroidContext()));
    }

    private void setMorphoCamera(boolean isBackCamera) {
        float[] evIDoNotCare = new float[5];
        ImageRefiner mImageRefiner = new ImageRefiner();
        int ret = mImageRefiner.initialize(1000, 1000, 3, 3, "YUV420_SEMIPLANAR", false);
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setMorphoCamera initialize ImageRefiner : ");
        stringBuilder.append(ret);
        Log.d(tag, stringBuilder.toString());
        ret = mImageRefiner.getEVs(true, isBackCamera, evIDoNotCare);
        tag = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("setMorphoCamera getEVs : ");
        stringBuilder.append(ret);
        Log.d(tag, stringBuilder.toString());
        mImageRefiner.finish();
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
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR, ExtendKey.FLIP_MODE_OFF);
        }
    }

    public HardwareSpec getHardwareSpec() {
        return this.mCameraSettings != null ? new HardwareSpecImpl(getCameraProvider(), this.mCameraCapabilities) : null;
    }

    public BottomBarUISpec getBottomBarSpec() {
        BottomBarUISpec bottomBarSpec = new BottomBarUISpec();
        bottomBarSpec.moduleName = PhotoModule.class.getSimpleName();
        bottomBarSpec.enableCamera = true;
        bottomBarSpec.hideCamera = hideCamera();
        bottomBarSpec.hideCameraForced = hideCameraForced();
        bottomBarSpec.hideSetting = hideSetting();
        bottomBarSpec.cameraCallback = this.mCameraCallback;
        bottomBarSpec.enableFlash = this.mActivity.currentBatteryStatusOK();
        bottomBarSpec.enableHdr = true;
        bottomBarSpec.hdrCallback = this.mHdrPlusCallback;
        bottomBarSpec.enableGridLines = true;
        bottomBarSpec.hideGridLines = true;
        bottomBarSpec.hideLowlight = isLowLightShow() ^ 1;
        bottomBarSpec.lowlightCallback = this.mLowlightCallback;
        bottomBarSpec.showBeautyButton = isImageCaptureIntent() ^ 1;
        boolean z = false;
        bottomBarSpec.showEffectButton = false;
        bottomBarSpec.seekbarChangeListener = this.seekListener;
        bottomBarSpec.hideBolken = true;
        bottomBarSpec.showBeauty2 = isBeautyShow();
        bottomBarSpec.showEffect2 = isMaskSelected();
        if (!isImageCaptureIntent()) {
            z = isMotionShow();
        }
        bottomBarSpec.showMotion = z;
        bottomBarSpec.hideHdr = isHdrShow() ^ 1;
        bottomBarSpec.hideFlash = isFlashShow() ^ 1;
        if (this.mCameraCapabilities != null) {
            bottomBarSpec.enableExposureCompensation = true;
            bottomBarSpec.exposureCompensationSetCallback = new ExposureCompensationSetCallback() {
                public void setExposure(int value) {
                    PhotoModule.this.setExposureCompensation(value);
                }
            };
            bottomBarSpec.minExposureCompensation = this.mCameraCapabilities.getMinExposureCompensation();
            bottomBarSpec.maxExposureCompensation = this.mCameraCapabilities.getMaxExposureCompensation();
            bottomBarSpec.exposureCompensationStep = this.mCameraCapabilities.getExposureCompensationStep();
        }
        z = isCountDownShow();
        bottomBarSpec.enableSelfTimer = z;
        bottomBarSpec.showSelfTimer = z;
        if (isCameraFrontFacing()) {
            ModuleController controller = this.mAppController.getCurrentModuleController();
            if (!(controller.getHardwareSpec() == null || controller.getHardwareSpec().isFlashSupported())) {
                bottomBarSpec.hideFlash = isFlashShow() ^ 1;
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
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.PhotoModule.startFaceDetection():void");
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
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.PhotoModule.stopFaceDetection():void");
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

    /* Access modifiers changed, original: protected */
    public byte[] addExifTags(byte[] jpeg, TotalCaptureResult result, boolean isFacingFont, int jpegOrientation) {
        ExifInterface exif = new ExifInterface();
        exif.addMakeAndModelTag();
        exif.addDateTimeStampTag(ExifInterface.TAG_DATE_TIME_ORIGINAL, System.currentTimeMillis(), TimeZone.getDefault());
        if (result != null) {
            int senstivityBoost;
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
                long et = ((Long) result.get(CaptureResult.SENSOR_EXPOSURE_TIME)).longValue();
                if (et == 1993865) {
                    et = 2000000;
                }
                exif.addExposureTime(new Rational(et, 1000000000));
            }
            if (result.get(CaptureResult.SENSOR_SENSITIVITY) != null) {
                senstivityBoost = 100;
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("senstivityBoost = ");
                stringBuilder.append(result.get(CaptureResult.CONTROL_POST_RAW_SENSITIVITY_BOOST));
                Log.d(tag, stringBuilder.toString());
                if (result.get(CaptureResult.CONTROL_POST_RAW_SENSITIVITY_BOOST) != null) {
                    senstivityBoost = ((Integer) result.get(CaptureResult.CONTROL_POST_RAW_SENSITIVITY_BOOST)).intValue();
                }
                exif.addISO((((Integer) result.get(CaptureResult.SENSOR_SENSITIVITY)).intValue() * senstivityBoost) / 100);
            }
            if (result.get(CaptureResult.JPEG_ORIENTATION) != null) {
                senstivityBoost = jpegOrientation;
                if (isFacingFont && isNeedMirrorSelfie()) {
                    senstivityBoost = (360 - senstivityBoost) % 360;
                }
                exif.addOrientationTag(senstivityBoost);
            }
            Tag tag2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("JPEG_ORIENTATION = ");
            stringBuilder2.append(result.get(CaptureResult.JPEG_ORIENTATION));
            Log.d(tag2, stringBuilder2.toString());
            tag2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("JPEG_GPS_LOCATION = ");
            stringBuilder2.append(this.mLocation);
            Log.d(tag2, stringBuilder2.toString());
            exif.addMeteringMode();
            exif.addExposureProgram();
            exif.addSoftware();
        }
        OutputStream jpegOut = new ByteArrayOutputStream();
        try {
            exif.writeExif(jpeg, jpegOut);
        } catch (IOException e) {
            Log.e(TAG, "Could not write EXIF", e);
        }
        return jpegOut.toByteArray();
    }

    /* Access modifiers changed, original: protected */
    public byte[] convertN21ToJpeg(byte[] bytesN21, int w, int h) {
        boolean isAlgorithmsOn = Keys.isAlgorithmsOn(this.mActivity.getSettingsManager());
        if (isDepthEnabled() || !isAlgorithmsOn) {
            Log.d(TAG, "depth on return jpeg");
            return bytesN21;
        }
        byte[] rez = new byte[0];
        YuvImage yuvImage = new YuvImage(bytesN21, 17, w, h, null);
        Rect rect = new Rect(0, 0, w, h);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        boolean compressToJpeg = yuvImage.compressToJpeg(rect, true, outputStream);
        return outputStream.toByteArray();
    }

    private TE convertN21ToBitmap(byte[] bytesN21) {
        if (this.mThumbnailCache == null) {
            return null;
        }
        long dateTaken = this.mNamedImages.getNextNameEntityForThumbnailOnly().date;
        ProcessingMedia media = ProcessingMediaManager.getInstance(this.mActivity).add(dateTaken);
        if (media != null) {
            media.setThumbnailPath(ProviderUtils.save2Private(this.mActivity, this.mThumbnailCache));
            media.setThumbnailBitmap(this.mThumbnailCache);
        }
        return new TE(dateTaken, this.mThumbnailCache);
    }

    private void hideScreenBrightness() {
        if (!this.mCameraCapabilities.getStringifier().stringify(FlashMode.OFF).equals(this.mActivity.getSettingsManager().getString(this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE))) {
            Intent intent = new Intent(action);
            intent.putExtra("flag", "hide");
            this.mActivity.sendBroadcast(intent);
            if (this.mFrontFlashHandler != null) {
                Message msg = Message.obtain();
                msg.what = 1002;
                this.mFrontFlashHandler.sendMessageDelayed(msg, 200);
            }
        }
    }

    private byte[] aftMirrorJpeg(byte[] jpegData) {
        Bitmap bitmap2 = CameraUtil.rotateAndMirror(BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length), 0, isNeedMirrorSelfie());
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        if (bitmap2 != null) {
            bitmap2.compress(CompressFormat.JPEG, 100, buf);
        }
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
        return this.mAppController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_photo);
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
        return true;
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
            this.mActivity.getButtonManager().mBeautyEnable = true;
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
        if (this.mIsGlMode && !TextUtils.isEmpty(this.mActivity.getCameraAppUI().getCurrSelect()) && this.mCameraId != 3) {
            this.mCameraDevice.enableShutterSound(false);
            this.mActivity.getButtonManager().mBeautyEnable = true;
            this.mShutterCallbackTime = System.currentTimeMillis();
            this.mNamedImages.nameNewImage(this.mShutterCallbackTime);
            this.mAppController.getCameraAppUI().takePicture(this.mPictureTaken);
        } else if (this.mCameraState == 8) {
            this.mLongshotPictureTakenCallback = new LongshotPictureCallback(loc);
            this.mReceivedBurstNum = 0;
            this.mCameraDevice.enableShutterSound(false);
            this.mCameraSettings.setFlashMode(FlashMode.OFF);
            this.mActivity.getButtonManager().mBeautyEnable = true;
            if (takeOptimizedBurstShot(loc)) {
                this.mFaceDetectionStarted = false;
                Log.w(TAG, "burst shot started1");
                return true;
            }
            this.mCameraDevice.applySettings(this.mCameraSettings);
            disableHdr();
            this.mHandler.sendEmptyMessage(3);
        } else {
            Tag tag;
            StringBuilder stringBuilder;
            this.mCameraDevice.enableShutterSound(false);
            if (getModuleId() == this.mActivity.getResources().getInteger(R.integer.camera_mode_pro)) {
                this.mCameraSettings.setExposureTime(this.mManualEtValue);
                tag = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Pro mManualET = ");
                stringBuilder.append(this.mManualEtValue);
                Log.d(tag, stringBuilder.toString());
            }
            this.mCameraDevice.applySettings(this.mCameraSettings);
            this.mFaces = this.mUI.filterAndAdjustFaces(isNeedMirrorSelfie(), this.mJpegRotation);
            showOptimisingPhotoHint();
            if (!CustomUtil.getInstance().isPanther()) {
                updateNeedHdrBurst();
            }
            tag = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("mNeedHdrBurst = ");
            stringBuilder.append(this.mNeedHdrBurst);
            Log.d(tag, stringBuilder.toString());
            this.mNamedImages.nameNewImage(this.mCaptureStartTime);
            if (this.mNeedHdrBurst) {
                this.mCameraSettings.setNeedBurst(true);
                updateFinalDrCheckResult();
                initializeImageRefiner();
                this.mActivity.getButtonManager().mBeautyEnable = true;
                this.mInHdrProcess = true;
                this.mHandler.sendEmptyMessageDelayed(7, 4000);
                this.mCameraDevice.burstShot(this.mHandler, new ShutterCallback(!animateBefore), this.mRawPictureCallback, this.mPostViewPictureCallback, getJpegPictureCallback());
            } else {
                if (this.mActivity.getCameraAppUI().getBeautySeek() > 0.0f) {
                    this.mActivity.getButtonManager().mBeautyEnable = false;
                }
                if (!isDepthEnabled()) {
                    this.mHandler.sendEmptyMessageDelayed(7, 1500);
                }
                this.mCameraDevice.takePicture(this.mHandler, new ShutterCallback(!animateBefore), this.mRawPictureCallback, this.mPostViewPictureCallback, getJpegPictureCallback());
            }
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
        SceneMode sceneMode = SceneMode.AUTO;
        sceneMode = this.mSceneMode;
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
            this.mOrientation = (360 - orientation) % 360;
            this.mUI.setPostGestureOrientation((this.mSensorOrientation + this.mOrientation) % 360);
            this.mUI.setGestureDisplayOrientation(this.mOrientation);
            if (this.mCameraDevice != null) {
                orientation = getJpegRotation(this.mCameraDevice.getCameraId(), orientation);
                if (!(this.curOritation == orientation || this.mCameraDevice.getCamera() == null)) {
                    this.curOritation = orientation;
                    Parameters mParameters = this.mCameraDevice.getCamera().getParameters();
                    mParameters.setRotation(this.curOritation);
                    this.mCameraDevice.getCamera().setParameters(mParameters);
                }
            } else {
                Log.e(TAG, "CameraDevice = null, can't set Parameter.setRotation");
            }
        }
    }

    public static int getJpegRotation(int cameraId, int orientation) {
        CameraInfo info = new CameraInfo();
        try {
            int rotation;
            Camera.getCameraInfo(cameraId, info);
            if (orientation == -1) {
                rotation = info.orientation;
            } else if (info.facing == 1) {
                rotation = ((info.orientation - orientation) + 360) % 360;
            } else {
                rotation = (info.orientation + orientation) % 360;
            }
            return rotation;
        } catch (Exception e) {
            return 0;
        }
    }

    public void onCameraAvailable(CameraProxy cameraProxy) {
        Log.i(TAG, "onCameraAvailable");
        if (this.mMotionHelper != null) {
            this.mMotionHelper.clearQueue();
        }
        if (!this.mPaused) {
            this.mCameraDevice = cameraProxy;
            this.mCameraDevice.initExtCamera(this.mActivity);
            this.mCameraDevice.setModuleId(getModuleId(), this.mActivity.getWindowManager().getDefaultDisplay().getRotation());
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

    private boolean isBeautyOnGP() {
        if (this.mActivity.getCameraAppUI().getBeautySeek() > 0.0f && isSupportBeauty() && (this.mCameraId == 0 || this.mCameraId == 1)) {
            return true;
        }
        return false;
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
        stringBuilder.append("onShutterButtonFocus pressed = ");
        stringBuilder.append(pressed);
        Log.d(tag, stringBuilder.toString());
        this.firsttime = System.currentTimeMillis();
        this.pressflag = pressed;
        if (pressed) {
            this.myThread = new MyThread();
            new MyThread().start();
        } else {
            try {
                if (this.myThread != null) {
                    this.myThread.join();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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
        this.mHasBurstStoped = true;
        Log.d(TAG, "stopBurst");
        this.mAppController.setShutterEnabled(true);
        int receivedCount = this.mReceivedBurstNum;
        this.mReceivedBurstNum = 0;
        this.mHandler.removeMessages(3);
        updateParametersFlashMode();
        clearFocusWithoutChangingState();
        if (this.mCameraState == 8 || this.mCameraState == 9 || this.mCameraState == 10) {
            abortOptimizedBurstShot();
            Log.w(TAG, "parameters post update");
            if (receivedCount != 0) {
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
                        PhotoModule.this.mUI.updateBurstCount(0);
                    }
                });
            }
        }
        setCameraState(1);
        HelpTipsManager helpTipsManager = this.mAppController.getHelpTipsManager();
        if (!(this.mBurstShotNotifyHelpTip || helpTipsManager == null)) {
            helpTipsManager.onBurstShotResponse();
            this.mBurstShotNotifyHelpTip = true;
        }
        this.mHandler.removeMessages(8);
        Message msg = Message.obtain();
        msg.what = 8;
        this.mHandler.sendMessageDelayed(msg, 500);
    }

    private void disableHdr() {
        this.mHdrState = this.mAppController.getSettingsManager().getString(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR);
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("save hdr state : ");
        stringBuilder.append(this.mHdrState);
        Log.d(tag, stringBuilder.toString());
        if (Keys.isHdrOn(this.mAppController.getSettingsManager())) {
            ToastUtil.showToast(this.mActivity, this.mActivity.getString(R.string.hdr_auto_off_toast), 1);
            this.mAppController.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR, ExtendKey.FLIP_MODE_OFF);
        }
    }

    /* Access modifiers changed, original: protected */
    public void resetHdrState() {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("reset hdr state to : ");
        stringBuilder.append(this.mHdrState);
        Log.d(tag, stringBuilder.toString());
        this.mAppController.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR, this.mHdrState);
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
        boolean superZoomOn;
        if (this.mZoomValue <= 1.5f || !isSuperResolutionEnabled()) {
            superZoomOn = false;
        } else {
            superZoomOn = true;
        }
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
        this.mHideProgressRunnable.run();
    }

    public void onShutterButtonClick() {
        Log.d(TAG, "onShutterButtonClick");
        if (!this.mInHdrProcess) {
            if (this.mActivity.getButtonManager().mBeautyEnable) {
                if (this.mActivity.getCameraAppUI().getBeautySeek() > 0.0f && isSupportBeauty() && (this.mCameraId == 0 || this.mCameraId == 1)) {
                    this.mActivity.getButtonManager().mBeautyEnable = false;
                } else {
                    this.mActivity.getButtonManager().mBeautyEnable = true;
                }
                if (this.mCameraDevice == null || this.mCameraDevice.getCameraState().getState() < 16 || this.mPreviewResult == null) {
                    this.mActivity.getButtonManager().mBeautyEnable = true;
                    return;
                } else if (this.mCameraSettings.isMotionOn() && this.mMotionHelper != null && !this.mMotionHelper.isComposeDone()) {
                    Log.w(TAG, "return for not compose done");
                    this.mActivity.getButtonManager().mBeautyEnable = true;
                    return;
                } else if (this.mAppController.getButtonManager().isMoreOptionsWrapperShow()) {
                    this.mAppController.getButtonManager().hideMoreOptionsWrapper();
                    this.mAppController.getLockEventListener().onIdle();
                    this.mMoreEnterToggleButton.setState(0);
                    return;
                } else {
                    doShutterButtonClick(false);
                    return;
                }
            }
            Log.d(TAG, "Beauty function is process after take a picture");
        }
    }

    private void StoreAceessDialog(CameraActivity activity) {
        if (Storage.getSavePath().equals("1") && !SDCard.instance().isWriteable()) {
            this.mActivity.getCameraAppUI().setViewFinderLayoutVisibile(true);
            if (this.mMountedDialog != null && this.mMountedDialog.isShowing()) {
                this.mMountedDialog.dismiss();
                this.mMountedDialog = null;
            }
            this.mMountedDialog = CameraUtil.UnAccessDialog(activity, activity.getResources().getString(R.string.sd_access_error), activity.getResources().getString(R.string.sd_access_photo_error_message), activity.getResources().getString(R.string.alert_storage_dialog_ok));
            if (Storage.getSavePath().equals("1")) {
                Storage.setSavePath("0");
                activity.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_SAVEPATH, "0");
                this.mStorageLocationCheck = true;
            }
        }
    }

    private void doShutterButtonClick(boolean isGestureShot) {
        Log.w(TAG, "KPI shutter click");
        this.mFinalDrCheckResult.clear();
        this.mAddedImages.clear();
        this.mLastPreviewResult = this.mPreviewResult;
        this.mAddedNumber = 0;
        this.mDropFrameIndex = NotificationManagerCompat.IMPORTANCE_UNSPECIFIED;
        this.mCameraSettings.setShouldSaveDng(true);
        if (this.mPaused || this.mCameraState == 4 || this.mCameraState == 3 || isInBurstshot() || this.mCameraState == 0 || !this.mAppController.isShutterEnabled() || this.mAppController.getCameraAppUI().isShutterLocked() || this.mUnderLowMemory) {
            this.mVolumeButtonClickedFlag = false;
            this.mActivity.getButtonManager().mBeautyEnable = true;
            return;
        }
        StoreAceessDialog(this.mActivity);
        Tag tag;
        StringBuilder stringBuilder;
        if (this.mStorageLocationCheck) {
            this.mStorageLocationCheck = false;
        } else if (this.mActivity.getStorageSpaceBytes() <= Storage.LOW_STORAGE_THRESHOLD_BYTES) {
            tag = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Not enough space or storage not ready. remaining=");
            stringBuilder.append(this.mActivity.getStorageSpaceBytes());
            Log.i(tag, stringBuilder.toString());
            this.mVolumeButtonClickedFlag = false;
            ToastUtil.showToast(this.mActivity, this.mActivity.getString(R.string.low_memory_toast), 1);
            this.mActivity.getButtonManager().mBeautyEnable = true;
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
            if (!(!CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_COUNT_DOWN_ONLY_AUTO_MODE, false) || getModuleId() == this.mActivity.getResources().getInteger(R.integer.camera_mode_photo) || getModuleId() == this.mActivity.getResources().getInteger(R.integer.camera_mode_square) || getModuleId() == this.mActivity.getResources().getInteger(R.integer.camera_mode_pro))) {
                isCanCountDown = false;
            }
            if (this.mActivity.islaunchFromAssistant()) {
                if (this.mActivity.isDelayTake()) {
                    countDownDuration = 5;
                } else {
                    countDownDuration = 3;
                }
                this.mTimerDuration = countDownDuration;
                this.mActivity.resetAssistantStatus();
            }
            if (!isCanCountDown) {
                setBrightness();
                focusAndCapture();
            } else if (countDownDuration > 0) {
                this.mFocusManager.removeFoucsMessages();
                this.mAppController.getCameraAppUI().transitionToCancel();
                this.mAppController.getCameraAppUI().hideModeOptions();
                this.mAppController.getButtonManager().setEffectsEnterToggleButton(8);
                this.mAppController.getButtonManager().setMoreEnterToggleButton(8);
                if (getModuleId() == this.mActivity.getResources().getInteger(R.integer.camera_mode_square)) {
                    this.mAppController.getButtonManager().setIndicatorTextVisible(8);
                }
                if (this.mActivity.getButtonManager().getEffectsEnterWrapperVisible() == 0) {
                    this.mLastMaskEnable = true;
                    this.mActivity.getButtonManager().setEffectWrapperVisible(8);
                }
                transitionToTimer(false);
                this.mUI.startCountdown(countDownDuration);
                this.mAppController.getCameraAppUI().setStereoModeStripViewSlideable(false);
                if (countDownDuration == 8 && this.mFocusManager != null) {
                    this.mFocusManager.isEight();
                }
            } else {
                setBrightness();
                focusAndCapture();
            }
        }
    }

    private void setBrightness() {
        if (this.mAppController.getCurrentModuleIndex() != this.mAppController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_pro) || "0".equals(getManualEtValue())) {
            String flashMode = this.mActivity.getSettingsManager().getString(this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE);
            String offFlashMode = this.mCameraCapabilities.getStringifier().stringify(FlashMode.OFF);
            String autoFlashMode = this.mCameraCapabilities.getStringifier().stringify(FlashMode.AUTO);
            String onFlashMode = this.mCameraCapabilities.getStringifier().stringify(FlashMode.ON);
            if (isCameraFrontFacing() && (onFlashMode.equals(flashMode) || (autoFlashMode.equals(flashMode) && this.mLData[0] <= 6.0f))) {
                Intent intent = new Intent(action);
                intent.putExtra("flag", "show");
                this.mActivity.sendBroadcast(intent);
                if (this.mFrontFlashHandler != null) {
                    Message msg = Message.obtain();
                    msg.what = 1001;
                    this.mFrontFlashHandler.sendMessageDelayed(msg, 200);
                }
            }
            return;
        }
        Log.d(TAG, "Adjusted shutter speed in pro mode");
    }

    private void updateFinalDrCheckResult() {
        float ev;
        List<Float> finalDrCheckResult = new ArrayList();
        for (Float ev2 : this.mDrCheckResult) {
            finalDrCheckResult.add(Float.valueOf(ev2.floatValue()));
        }
        String hdrMode = Keys.getHdrMode(this.mActivity.getSettingsManager());
        Integer iso = (Integer) this.mLastPreviewResult.get(TotalCaptureResult.SENSOR_SENSITIVITY);
        if (ExtendKey.FLIP_MODE_OFF.equals(hdrMode)) {
            finalDrCheckResult.clear();
            finalDrCheckResult.add(Float.valueOf(0.0f));
        } else if (!"auto".equals(hdrMode) && finalDrCheckResult.size() == 1) {
            finalDrCheckResult.add(Float.valueOf(-0.8f));
            if (iso.intValue() <= 600) {
                finalDrCheckResult.add(Float.valueOf(0.8f));
            }
        }
        if (finalDrCheckResult.size() == 0) {
            finalDrCheckResult.add(Float.valueOf(0.0f));
        }
        if (isCameraFrontFacing()) {
            ev = ((Float) finalDrCheckResult.get(finalDrCheckResult.size() - 1)).floatValue();
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("in front camera lastEv : ");
            stringBuilder.append(ev);
            Log.d(tag, stringBuilder.toString());
            if (ev < 0.0f) {
                finalDrCheckResult.remove(finalDrCheckResult.size() - 1);
            }
        }
        if (finalDrCheckResult.size() == 1) {
            if (iso.intValue() > 400) {
                this.mFinalDrCheckResult.add(Float.valueOf(0.0f));
                this.mFinalDrCheckResult.add(Float.valueOf(0.0f));
            }
            this.mFinalDrCheckResult.addAll(finalDrCheckResult);
        } else if (finalDrCheckResult.size() > 1) {
            ev = ((Float) finalDrCheckResult.get(finalDrCheckResult.size() - 1)).floatValue();
            if (ev <= 0.0f) {
                this.mFinalDrCheckResult.add(Float.valueOf(0.0f));
                this.mFinalDrCheckResult.add(Float.valueOf(0.0f));
                this.mFinalDrCheckResult.addAll(finalDrCheckResult);
            } else {
                this.mFinalDrCheckResult.addAll(finalDrCheckResult);
                this.mDropFrameIndex = finalDrCheckResult.size() - 1;
                Tag tag2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("mDropFrameIndex = ");
                stringBuilder2.append(this.mDropFrameIndex);
                Log.d(tag2, stringBuilder2.toString());
                this.mFinalDrCheckResult.add(Float.valueOf(ev));
                this.mFinalDrCheckResult.add(Float.valueOf(ev));
            }
        }
        Tag tag3 = TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("mFinalDrCheckResult.size() = ");
        stringBuilder3.append(this.mFinalDrCheckResult.size());
        Log.d(tag3, stringBuilder3.toString());
        if (this.mFinalDrCheckResult.size() == 0) {
            this.mFinalDrCheckResult.add(Float.valueOf(0.0f));
        }
        this.mCameraSettings.setHdrEv(this.mFinalDrCheckResult);
        tag3 = TAG;
        stringBuilder3 = new StringBuilder();
        stringBuilder3.append("iso = ");
        stringBuilder3.append(iso);
        stringBuilder3.append(" mNeedDeNoise = ");
        stringBuilder3.append(this.mNeedDeNoise);
        Log.d(tag3, stringBuilder3.toString());
    }

    private void updateNeedHdrBurst() {
        boolean z = false;
        if (this.mActivity.islaunchFromAssistant()) {
            this.mHdrEnabled = false;
        } else {
            boolean isMotionOn = Keys.isMotionOn(this.mActivity.getSettingsManager());
            boolean isAlgorithmsOn = Keys.isAlgorithmsOn(this.mActivity.getSettingsManager());
            boolean isDepthOn = isDepthEnabled();
            String flashMode = this.mActivity.getSettingsManager().getString(this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE);
            if (CustomUtil.getInstance().isPanther()) {
                if (!(!isAlgorithmsOn || saveDngEnabled() || "on".equals(flashMode) || isDepthOn || isMotionOn || this.mIsGlMode || (getModuleId() != 4 && getModuleId() != 15))) {
                    z = true;
                }
                this.mHdrEnabled = z;
            } else {
                boolean isBatteryOk = SharedUtil.getIntValueByKey("currentBatteryStatus").intValue() == 0;
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateNeedHdrBurst isBatteryOk = ");
                stringBuilder.append(isBatteryOk);
                Log.d(tag, stringBuilder.toString());
                if (!(!isAlgorithmsOn || saveDngEnabled() || "on".equals(flashMode) || isDepthOn || isMotionOn || !isBatteryOk || this.mIsGlMode || (getModuleId() != 4 && getModuleId() != 15))) {
                    z = true;
                }
                this.mHdrEnabled = z;
            }
        }
        this.mNeedHdrBurst = this.mHdrEnabled;
    }

    private void setupParam(EngineParam param) {
        int i = 0;
        if (this.mImageRefiner.getParam(param) != 0) {
            Toast.makeText(this.mActivity, String.format("Engine getParam error.(0x%08X)", new Object[]{Integer.valueOf(ret)}), 1).show();
        }
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mActivity);
        Resources resources = this.mActivity.getResources();
        param.synth_ev_stop = (double) sp.getFloat(resources.getString(R.string.preference_key_synth_ev_stop), 0.0f);
        param.camera_motion_threshold = (double) sp.getFloat(resources.getString(R.string.preference_key_camera_motion_threshold), 0.8f);
        param.ghost_removal_strength = (double) sp.getFloat(resources.getString(R.string.preference_key_ghost_removal_strength), 2.5f);
        param.ghost_rate_threshold = (double) sp.getFloat(resources.getString(R.string.preference_key_ghost_rate_threshold), 0.2f);
        param.color_enhancement_contrast_level = (double) sp.getFloat(resources.getString(R.string.preference_key_color_enhancement_contrast_level), 0.008f);
        param.color_enhancement_saturation_level = (double) sp.getFloat(resources.getString(R.string.preference_key_color_enhancement_saturation_level), 0.002f);
        param.ghost_map_sharing_enabled = sp.getBoolean(resources.getString(R.string.preference_key_ghost_map_sharing_enabled), true);
        param.mfnr_ghost_removal_strength = (double) sp.getFloat(resources.getString(R.string.preference_key_mfnr_ghost_removal_strength), 0.8f);
        param.mfnr_texture_preservation_level = sp.getInt(resources.getString(R.string.preference_key_mfnr_texture_preservation_level), 64);
        param.luma_noise_reduction_coeff = (double) sp.getFloat(resources.getString(R.string.preference_key_luma_noise_reduction_coeff), 1.0f);
        param.chroma_noise_reduction_coeff = (double) sp.getFloat(resources.getString(R.string.preference_key_chroma_noise_reduction_coeff), 1.0f);
        param.chroma_noise_reduction_iteration = sp.getInt(resources.getString(R.string.preference_key_chroma_noise_reduction_iteration), 1);
        param.mfnr_saturation_compensation_level = sp.getInt(resources.getString(R.string.preference_key_mfnr_saturation_compensation_level), 1024);
        param.mfnr_luminance_gain = (double) sp.getFloat(resources.getString(R.string.preference_key_mfnr_luminance_gain), 1.0f);
        param.mfnr_color_gamma = (double) sp.getFloat(resources.getString(R.string.preference_key_mfnr_color_gamma), 1.0f);
        param.mfnr_contrast_strength = sp.getInt(resources.getString(R.string.preference_key_mfnr_contrast_strength), 0);
        param.sharpness_enhancement_level = sp.getInt(resources.getString(R.string.preference_key_sharpness_enhancement_level), MediaPlayer2.MEDIA_INFO_TIMED_TEXT_ERROR);
        int[] keys = new int[]{R.string.preference_key_ynr_table_1, R.string.preference_key_ynr_table_2, R.string.preference_key_ynr_table_3, R.string.preference_key_ynr_table_4, R.string.preference_key_ynr_table_5, R.string.preference_key_ynr_table_6, R.string.preference_key_ynr_table_7, R.string.preference_key_ynr_table_8, R.string.preference_key_ynr_table_9, R.string.preference_key_ynr_table_10};
        for (int i2 = 0; i2 < 10; i2++) {
            param.ynr_table[i2] = (double) resources.getIntArray(2130837654)[i2];
        }
        keys = new int[]{R.string.preference_key_cnr_table_1, R.string.preference_key_cnr_table_2, R.string.preference_key_cnr_table_3, R.string.preference_key_cnr_table_4, R.string.preference_key_cnr_table_5, R.string.preference_key_cnr_table_6, R.string.preference_key_cnr_table_7, R.string.preference_key_cnr_table_8, R.string.preference_key_cnr_table_9, R.string.preference_key_cnr_table_10};
        while (i < 10) {
            param.cnr_table[i] = resources.getIntArray(2130837543)[i];
            i++;
        }
    }

    public void onShutterButtonLongClick() {
        this.mHasBurstStoped = false;
        Log.w(TAG, "onShutterButtonLongClick");
        if (Keys.isMotionOn(this.mAppController.getSettingsManager()) || !Keys.isShutterControlOn(this.mAppController.getSettingsManager())) {
            return;
        }
        if (isDepthEnabled()) {
            Log.d(TAG, "Depth on, skip burst shot");
            ToastUtil.showToast(this.mActivity, (int) R.string.burst_shot_in_depth_toast, 0);
        } else if (this.mAppController.getButtonManager().isMoreOptionsWrapperShow()) {
            this.mAppController.getButtonManager().hideMoreOptionsWrapper();
            this.mAppController.getLockEventListener().onIdle();
            this.mMoreEnterToggleButton.setState(0);
        } else if (isBeautyShow()) {
            ToastUtil.showToast(this.mActivity, this.mActivity.getString(R.string.burst_shot_is_not_supported_in_beautify_mode_toast), 1);
        } else if (this.mPaused || this.mCameraState == 4 || this.mCameraState == 0 || !this.mAppController.isShutterEnabled() || isImageCaptureIntent() || this.mUnderLowMemory) {
            this.mVolumeButtonClickedFlag = false;
        } else if (Keys.isLowlightOn(this.mAppController.getSettingsManager()) || isFacebeautyEnabled()) {
            if (Keys.isLowlightOn(this.mAppController.getSettingsManager())) {
                ToastUtil.showToast(this.mActivity, this.mActivity.getString(R.string.current_module_refuse_longshot_toast), 0);
            }
        } else {
            int countDownDuration = this.mActivity.getSettingsManager().getInteger(this.mAppController.getCameraScope(), Keys.KEY_COUNTDOWN_DURATION).intValue();
            if (isCountDownShow() && countDownDuration > 0) {
                return;
            }
            if (this.mActivity.getStorageSpaceBytes() <= Storage.LOW_STORAGE_THRESHOLD_BYTES) {
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Not enough space or storage not ready. remaining=");
                stringBuilder.append(this.mActivity.getStorageSpaceBytes());
                Log.i(tag, stringBuilder.toString());
                this.mVolumeButtonClickedFlag = false;
                ToastUtil.showToast(this.mActivity, this.mActivity.getString(R.string.low_memory_toast), 1);
                return;
            }
            Tag tag2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onShutterButtonClick: mCameraState=");
            stringBuilder2.append(this.mCameraState);
            stringBuilder2.append(" mVolumeButtonClickedFlag=");
            stringBuilder2.append(this.mVolumeButtonClickedFlag);
            Log.d(tag2, stringBuilder2.toString());
            this.mAppController.setShutterEnabled(false);
            Log.w(TAG, "Longpress sucess and wait for burst shot");
            if (this.mCameraState == 5) {
                clearFocusWithoutChangingState();
            }
            this.mCameraSettings.setShouldSaveDng(false);
            setCameraState(8);
            this.mBurstShotCheckQueue.clearCheckQueue();
            this.mBurstShotNotifyHelpTip = false;
            this.mUI.disableZoom();
            ToastUtil.showToast(this.mActivity, this.mActivity.getString(R.string.flash_is_not_supported_for_burst_shot_toast), 0);
            if (this.isOptimeizeSnapshot) {
                Log.d(TAG, "OptimeizeSnapshot");
                bustShot();
            } else {
                capture();
            }
        }
    }

    private void initializeImageRefiner() {
        Size size = this.mCameraSettings.getCurrentPhotoSize();
        int imageWidth = size.width();
        int imageHeight = size.height();
        int shotNum = this.mFinalDrCheckResult.size();
        Log.d(TAG, "initializeEngine");
        initializeEngine(imageWidth, imageHeight, shotNum, 3, "YVU420_SEMIPLANAR", false);
        EngineParam param = new EngineParam();
        setupParam(param);
        if (this.mImageRefiner.setParam(param) != 0) {
            Toast.makeText(this.mActivity, String.format("Engine setParam error.(0x%08X)", new Object[]{Integer.valueOf(ret)}), 1).show();
        }
        Log.d(TAG, String.format(Locale.US, "setParam %s", new Object[]{param}));
        this.mImageRefiner.getParam(param);
    }

    private boolean initializeEngine(int imageWidth, int imageHeight, int shotNum, int evNum, String format, boolean enableSyntheticEvImage) {
        if (this.mImageRefiner == null) {
            this.mImageRefiner = new ImageRefiner();
        } else {
            this.mImageRefiner.finish();
        }
        if (this.mImageRefiner.initialize(imageWidth, imageHeight, shotNum, evNum, format, enableSyntheticEvImage) != 0) {
            return false;
        }
        return true;
    }

    private void bustShot() {
        android.util.Log.i("bustShot", "bustShot");
        this.snapShot = ContinueShot.create(this);
        this.snapShot.prepare();
        this.snapShot.takePicture(new onContinueShotFinishListener() {
            public void onFinish() {
                PhotoModule.this.mAppController.setShutterEnabled(true);
                PhotoModule.this.mHandler.removeMessages(3);
                PhotoModule.this.setCameraState(1);
            }
        });
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
        Log.d(TAG, "focusAndCapture");
        if (this.mFocusManager == null) {
            this.mActivity.getButtonManager().mBeautyEnable = true;
            return;
        }
        if (("on".equals(this.mActivity.getSettingsManager().getString(this.mAppController.getCameraScope(), Keys.KEY_FLASH_MODE)) || this.mNeedFocus) && !this.mUI.hasFaces() && getModuleId() == 4) {
            FocusArea focusArea = this.mFocusManager.getCaptureFocusArea();
            if (focusArea != null) {
                onSingleTapUp(null, focusArea.x, focusArea.y);
            }
        }
        if (this.mSceneMode == SceneMode.HDR) {
            this.mUI.setSwipingEnabled(false);
        }
        if (this.mFocusManager.isFocusingSnapOnFinish() || this.mCameraState == 3 || this.mCameraState == 7) {
            if (!this.mIsImageCaptureIntent) {
                this.mSnapshotOnIdle = true;
            }
            this.mActivity.getButtonManager().mBeautyEnable = true;
            return;
        }
        this.mSnapshotOnIdle = false;
        if (this.mCameraState != 5) {
            this.mFocusManager.focusAndCapture(this.mCameraSettings.getCurrentFocusMode());
        } else {
            capture();
        }
        if (this.mCameraSettings.isMotionOn() && this.mMotionHelper != null) {
            int motionOrientation = this.mJpegRotation;
            this.mMotionHelper.setComposeEnd(false);
            if (this.mCameraId == 1) {
                if (this.mJpegRotation == MediaProviderUtils.ROTATION_270) {
                    motionOrientation = 90;
                } else if (this.mJpegRotation == 90) {
                    motionOrientation = MediaProviderUtils.ROTATION_270;
                }
            }
            this.mMotionHelper.setOrientation(motionOrientation);
            this.mMotionHelper.startMotion(true);
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
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[PhotoMudule]onRemainingSecondsChanged remainingSeconds = ");
        stringBuilder.append(remainingSeconds);
        Log.d(tag, stringBuilder.toString());
        if (remainingSeconds <= 3) {
            int mode = this.mAudioManager.getRingerMode();
            boolean isShutterSoundOn = Keys.isShutterSoundOn(this.mAppController.getSettingsManager());
            if (remainingSeconds == 1) {
                if (isShutterSoundOn && mode == 2) {
                    this.mCountdownSoundPlayer.play(R.raw.timer_final_second, 0.6f);
                }
            } else if (remainingSeconds != 2 && remainingSeconds != 3) {
                setBrightness();
            } else if (isShutterSoundOn && mode == 2) {
                this.mCountdownSoundPlayer.play(R.raw.timer_increment, 0.6f);
            }
        }
    }

    public void onCountDownFinished() {
        this.mFocusManager.noEight();
        this.mAppController.getCameraAppUI().transitionToCapture();
        if (getModuleId() == this.mActivity.getResources().getInteger(R.integer.camera_mode_square)) {
            this.mAppController.getCameraAppUI().setModeStripViewVisibility(false);
            this.mAppController.getButtonManager().setIndicatorTextVisible(0);
        }
        this.mAppController.getCameraAppUI().showModeOptions();
        transitionToTimer(true);
        if (this.mLastMaskEnable) {
            this.mLastMaskEnable = false;
            this.mActivity.getButtonManager().setEffectWrapperVisible(0);
        }
        if (!this.mPaused) {
            focusAndCapture();
            this.mAppController.getCameraAppUI().setStereoModeStripViewSlideable(true);
        }
    }

    private void switchCameraResolution() {
        String pictureSizeKey;
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        if (isCameraFrontFacing()) {
            pictureSizeKey = Keys.KEY_PICTURE_SIZE_FRONT;
        } else {
            pictureSizeKey = Keys.KEY_PICTURE_SIZE_BACK;
        }
        Size s = SettingsUtil.sizeFromString(settingsManager.getString(SettingsManager.SCOPE_GLOBAL, pictureSizeKey, SettingsUtil.getDefaultPictureSize(isCameraFrontFacing())));
        this.mActivity.getCameraAppUI().freezeGlSurface();
        float ratio = ((float) s.width()) / ((float) s.height());
        if (ratio == 1.3333334f) {
            this.mActivity.getCameraAppUI().setSurfaceHeight(MotionPictureHelper.FRAME_HEIGHT_9);
            this.mActivity.getCameraAppUI().setSurfaceWidth(960);
        } else if (ratio == 1.7777778f) {
            this.mActivity.getCameraAppUI().setSurfaceHeight(MotionPictureHelper.FRAME_HEIGHT_9);
            this.mActivity.getCameraAppUI().setSurfaceWidth(1280);
        } else if (ratio == 2.111675f) {
            this.mActivity.getCameraAppUI().setSurfaceHeight(1546);
            this.mActivity.getCameraAppUI().setSurfaceWidth(3264);
        }
    }

    public void resume() {
        this.mPaused = false;
        Log.w(TAG, "KPI Track photo resume E");
        if (this.mActivity != null) {
            SettingsManager settingsManager = this.mActivity.getSettingsManager();
            if (this.mActivity.islaunchFromAssistant()) {
                this.mCameraId = this.mActivity.isUserFrontCamera();
            } else {
                this.mCameraId = settingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID).intValue();
                if (isDepthEnabled() && getModuleId() == this.mAppController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_photo) && this.mCameraId == 0) {
                    this.mCameraId = 3;
                }
            }
            this.mActivity.onLastMediaDataUpdated();
        }
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
            this.mIsGlMode = onGLRenderEnable();
            if (!isSupportBeauty() && !isSupportEffects()) {
                this.mActivity.getButtonManager().hideEffectsContainerWrapper();
                if (this.mCameraId == 3) {
                    this.mActivity.getCameraAppUI().setTextureViewVisible(0);
                    this.mActivity.getCameraAppUI().getCameraGLSurfaceView().setVisibility(8);
                } else {
                    this.mActivity.getCameraAppUI().showOrHideGLSurface(false);
                }
            } else if (this.mIsGlMode && (this.mCameraId == 1 || this.mCameraId == 0)) {
                setPreviewBytesBack(this.mActivity.getCameraAppUI().getPreviewCallback());
                SettingsManager settingsManager2 = this.mActivity.getSettingsManager();
                if (Keys.isMotionOn(settingsManager2)) {
                    settingsManager2.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_MOTION, false);
                }
                if (com.hmdglobal.app.camera.util.ResolutionUtil.getRightResolutionById(getModuleId(), this.mCameraId, this.mActivity)) {
                    this.mActivity.getCameraAppUI().freezeGlSurface();
                }
                this.mActivity.getCameraAppUI().getCameraGLSurfaceView().switchCamera(this.mCameraId);
                this.mActivity.getCameraAppUI().showOrHideGLSurface(true);
                this.mActivity.getButtonManager().initEffectById(4);
            } else {
                this.mActivity.getButtonManager().hideEffectsContainerWrapper();
                if (this.mCameraId == 3) {
                    this.mActivity.getCameraAppUI().setTextureViewVisible(0);
                    this.mActivity.getCameraAppUI().getCameraGLSurfaceView().setVisibility(8);
                } else {
                    this.mActivity.getCameraAppUI().setTextureViewVisible(0);
                    this.mActivity.getCameraAppUI().getCameraGLSurfaceView().setVisibility(8);
                }
            }
            if (!this.mActivity.getCameraProvider().isCameraRequestBoosted()) {
                if (this.mIsGlMode) {
                    this.mActivity.getCameraAppUI().setOrientation(this.mCameraId);
                }
                requestCameraOpen();
            }
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
            Sensor lightSensor = this.mSensorManager.getDefaultSensor(5);
            if (lightSensor != null) {
                this.mSensorManager.registerListener(this, lightSensor, 3);
            }
            getServices().getRemoteShutterListener().onModuleReady(this, getRemodeShutterIcon());
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
    public int getRemodeShutterIcon() {
        return CameraUtil.getCameraShutterNormalStateIconId(this.mAppController.getCurrentModuleIndex(), this.mAppController.getAndroidContext());
    }

    /* Access modifiers changed, original: protected */
    public boolean isCameraFrontFacing() {
        Characteristics chara = null;
        try {
            chara = this.mAppController.getCameraProvider().getCharacteristics(this.mCameraId);
        } catch (NullPointerException e) {
        }
        if (chara != null) {
            return chara.isFacingFront();
        }
        boolean z = true;
        if (this.mCameraId != 1) {
            z = false;
        }
        return z;
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
    public boolean isMotionShow() {
        return (isDepthEnabled() || isBeautyShow() || isMaskSelected() || saveDngEnabled() || !CustomUtil.getInstance().isPanther()) ? false : true;
    }

    /* Access modifiers changed, original: protected */
    public boolean isHdrShow() {
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        return (!Keys.isAlgorithmsOn(settingsManager) || saveDngEnabled() || isDepthEnabled() || Keys.isMotionOn(settingsManager) || isBeautyShow() || isMaskSelected()) ? false : true;
    }

    /* Access modifiers changed, original: protected */
    public boolean isFlashShow() {
        return (isBeautyShow() || isMaskSelected()) ? false : true;
    }

    /* Access modifiers changed, original: protected */
    public boolean isBeautyShow() {
        return this.mActivity.getCameraAppUI().getBeautySeek() > 0.0f && !isDepthEnabled();
    }

    /* Access modifiers changed, original: protected */
    public boolean isMaskSelected() {
        return false;
    }

    /* Access modifiers changed, original: protected */
    public boolean isWrapperButtonShow() {
        return false;
    }

    /* Access modifiers changed, original: protected */
    public boolean isCountDownShow() {
        boolean isCanCountDown = isCameraFrontFacing() ? CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_FRONTFACING_COUNT_DOWN, false) : CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_BACKFACING_COUNT_DOWN, false);
        if (!CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_COUNT_DOWN_ONLY_AUTO_MODE, false) || getModuleId() == this.mActivity.getResources().getInteger(R.integer.camera_mode_photo) || getModuleId() == this.mActivity.getResources().getInteger(R.integer.camera_mode_square) || getModuleId() == this.mActivity.getResources().getInteger(R.integer.camera_mode_pro)) {
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
        if (this.mCameraState == 8 || this.mCameraState == 9 || this.mCameraState == 10) {
            stopBurst();
        }
        this.mActivity.getButtonManager().mBeautyEnable = true;
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
        Sensor lightSensor = this.mSensorManager.getDefaultSensor(5);
        if (lightSensor != null) {
            this.mSensorManager.unregisterListener(this, lightSensor);
        }
        if (!(this.mCameraDevice == null || this.mCameraState == 0)) {
            this.mCameraDevice.cancelAutoFocus();
        }
        if (!(this.mCameraSettings == null || this.mCameraDevice == null)) {
            this.mCameraDevice.applySettings(this.mCameraSettings);
        }
        stopPreview();
        cancelCountDown();
        this.mSoundPlayer.unloadSound(R.raw.camera_burst);
        this.mSoundPlayer.unloadSound(R.raw.shutter);
        this.mSoundPlayer.unloadSound(R.raw.shutter_sound_2);
        this.mCountdownSoundPlayer.unloadSound(R.raw.timer_final_second);
        this.mCountdownSoundPlayer.unloadSound(R.raw.timer_increment);
        if (ProcessingMediaManager.getInstance(this.mActivity).getProcessingMedia().isEmpty()) {
            Log.d(TAG, "mHandler removeCallbacksAndMessages");
            this.mHandler.removeCallbacksAndMessages(null);
        }
        if (this.mMotionManager != null) {
            this.mMotionManager.removeListener(this.mFocusManager);
            this.mMotionManager = null;
        }
        closeCamera();
        if (this.mCameraId == 3) {
            this.mActivity.closeCameraSybc();
        }
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
        if (this.mMotionHelper != null) {
            this.mMotionHelper.clearQueue();
        }
    }

    public void destroy() {
        this.mJpegImageData = null;
        if (this.mUI != null) {
            this.mUI.clearReviewImage();
        }
        if (onGLRenderEnable()) {
            BeaurifyJniSdk.preViewInstance().nativeDisablePackage();
            updateBeautySeek(0.0f);
        }
        this.mSoundPlayer.release();
        this.mCountdownSoundPlayer.release();
        if (this.mMotionHelper != null) {
            this.mMotionHelper.release();
            this.mMotionHelper = null;
        }
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
            if (this.mEvoFlashLock == null && !this.mAppController.getButtonManager().isFlashButtonHidden()) {
                this.mEvoFlashLock = Integer.valueOf(this.mAppController.getButtonManager().disableButtonWithLock(0));
            }
            this.mFocusManager.setAeAwbLock(true);
            this.mFocusManager.keepFocusFrame();
            setExposureCompensation(index, false);
            setCameraParameters(4);
        }
    }

    /* JADX WARNING: Missing block: B:43:0x0096, code skipped:
            return;
     */
    public void onSingleTapUp(android.view.View r5, int r6, int r7) {
        /*
        r4 = this;
        r0 = r4.mPaused;
        if (r0 != 0) goto L_0x0096;
    L_0x0004:
        r0 = r4.mCameraDevice;
        if (r0 == 0) goto L_0x0096;
    L_0x0008:
        r0 = r4.mFirstTimeInitialized;
        if (r0 == 0) goto L_0x0096;
    L_0x000c:
        r0 = r4.mCameraState;
        r1 = 3;
        if (r0 == r1) goto L_0x0096;
    L_0x0011:
        r0 = r4.mCameraState;
        r1 = 7;
        if (r0 == r1) goto L_0x0096;
    L_0x0016:
        r0 = r4.mCameraState;
        r1 = 8;
        if (r0 == r1) goto L_0x0096;
    L_0x001c:
        r0 = r4.mCameraState;
        r1 = 9;
        if (r0 == r1) goto L_0x0096;
    L_0x0022:
        r0 = r4.mCameraState;
        r1 = 10;
        if (r0 == r1) goto L_0x0096;
    L_0x0028:
        r0 = r4.mCameraState;
        r1 = 4;
        if (r0 == r1) goto L_0x0096;
    L_0x002d:
        r0 = r4.mCameraState;
        if (r0 != 0) goto L_0x0032;
    L_0x0031:
        goto L_0x0096;
    L_0x0032:
        r0 = r4.mIsInIntentReviewUI;
        if (r0 == 0) goto L_0x0037;
    L_0x0036:
        return;
    L_0x0037:
        r0 = r4.isCameraFrontFacing();
        if (r0 == 0) goto L_0x0053;
    L_0x003d:
        r0 = r4.mAppController;
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
        r0 = r4.mUI;
        r0.clearEvoPendingUI();
        r0 = r4.mEvoFlashLock;
        r1 = 0;
        if (r0 == 0) goto L_0x007b;
    L_0x005d:
        r0 = r4.mAppController;
        r0 = r0.getButtonManager();
        r0 = r0.isFlashButtonHidden();
        if (r0 != 0) goto L_0x007b;
    L_0x0069:
        r0 = r4.mAppController;
        r0 = r0.getButtonManager();
        r2 = r4.mEvoFlashLock;
        r2 = r2.intValue();
        r0.enableButtonWithToken(r1, r2);
        r0 = 0;
        r4.mEvoFlashLock = r0;
    L_0x007b:
        r0 = r4.mUI;
        r2 = (float) r6;
        r3 = (float) r7;
        r0.initEvoSlider(r2, r3);
        r0 = r4.mFocusAreaSupported;
        if (r0 != 0) goto L_0x008b;
    L_0x0086:
        r0 = r4.mMeteringAreaSupported;
        if (r0 != 0) goto L_0x008b;
    L_0x008a:
        return;
    L_0x008b:
        r0 = r4.mFocusManager;
        r0.setAeAwbLock(r1);
        r0 = r4.mFocusManager;
        r0.onSingleTapUp(r6, r7);
        return;
    L_0x0096:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.PhotoModule.onSingleTapUp(android.view.View, int, int):void");
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
                        Tag tag = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("onKeyDown KEYCODE_VOLUME_DOWN mIsFrontFlashEnd ");
                        stringBuilder.append(this.mIsFrontFlashEnd);
                        stringBuilder.append("  mCameraId = ");
                        stringBuilder.append(this.mCameraId);
                        Log.d(tag, stringBuilder.toString());
                        if (this.mCameraId == 1 && !isFrontFlashEnd()) {
                            return true;
                        }
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
            Tag tag;
            StringBuilder stringBuilder;
            HelpTipsManager helpTipsManager;
            if (keyCode != CameraUtil.BOOM_KEY) {
                switch (keyCode) {
                    case 24:
                    case 25:
                        tag = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("onKeyUp KEYCODE_VOLUME_DOWN mIsFrontFlashEnd ");
                        stringBuilder.append(this.mIsFrontFlashEnd);
                        stringBuilder.append("  mCameraId = ");
                        stringBuilder.append(this.mCameraId);
                        Log.d(tag, stringBuilder.toString());
                        if (this.mCameraId == 1 && !isFrontFlashEnd()) {
                            return true;
                        }
                    default:
                        return false;
                }
            }
            this.mAppController.setShutterPress(false);
            if (!(!this.mFirstTimeInitialized || this.mActivity.getCameraAppUI().isInIntentReview() || this.mCameraKeyLongPressed)) {
                if (this.mUI.isCountingDown()) {
                    tag = TAG;
                    stringBuilder = new StringBuilder();
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

    public boolean isFrontFlashEnd() {
        return this.mIsFrontFlashEnd;
    }

    public void setFrontFlashEnd(boolean isFrontFlashEnd) {
        this.mIsFrontFlashEnd = isFrontFlashEnd;
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
        if (info != null) {
            this.mDisplayOrientation = info.getPreviewOrientation(this.mDisplayRotation);
            this.mSensorOrientation = info.getSensorOrientation();
        }
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
                    PhotoModule.this.mFocusManager.onPreviewStarted();
                    PhotoModule.this.onPreviewStarted();
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
        } else if (this.mActivity.getCameraAppUI().getSurfaceTexture(this.mIsGlMode) == null) {
            Log.w(TAG, "startPreview: surfaceTexture is not ready.");
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
            boolean z = false;
            if (!this.mSnapshotOnIdle) {
                if (this.mFocusManager.getFocusMode(this.mCameraSettings.getCurrentFocusMode()) == FocusMode.CONTINUOUS_PICTURE && this.mCameraState != 5) {
                    this.mCameraDevice.cancelAutoFocus();
                }
                this.mFocusManager.setAeAwbLock(false);
            }
            updateMotionState();
            firstFrame = false;
            updateParametersPictureSize();
            if (this.mCameraSettings.isMotionOn()) {
                initMotionHelper();
            }
            if (this.mActivity.islaunchFromAssistant()) {
                this.mActivity.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_DEPTH, false);
            }
            boolean isDepthOn = isDepthEnabled();
            this.mCameraSettings.setDepthOn(isDepthOn);
            boolean isAlgorithmsOn = Keys.isAlgorithmsOn(this.mActivity.getSettingsManager());
            if (!isAlgorithmsOn) {
                this.mActivity.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR, ExtendKey.FLIP_MODE_OFF);
            }
            CameraSettings cameraSettings = this.mCameraSettings;
            boolean z2 = (isAlgorithmsOn || this.mIsGlMode) ? false : true;
            cameraSettings.setUseJpeg(z2);
            if (isDepthOn) {
                this.mCameraSettings.setLiveBokehEnabled(true);
                this.mCameraSettings.setLiveBokehLevel(8);
            }
            setCameraParameters(-1);
            updateNeedHdrBurst();
            this.mCameraSettings.setNeedBurst(this.mNeedHdrBurst);
            if (!this.mActivity.getCameraProvider().isBoostPreview()) {
                CameraProxy cameraProxy = this.mCameraDevice;
                CameraAppUI cameraAppUI = this.mActivity.getCameraAppUI();
                if (this.mIsGlMode && this.mCameraId != 3) {
                    z = true;
                }
                cameraProxy.setPreviewTexture(cameraAppUI.getSurfaceTexture(z));
            }
            Log.i(TAG, "startPreview");
            CameraStartPreviewCallback startPreviewCallback = new CameraStartPreviewCallback() {
                public void onPreviewStarted() {
                    PhotoModule.this.mFocusManager.onPreviewStarted();
                    PhotoModule.this.onPreviewStarted();
                    SessionStatsCollector.instance().previewActive(true);
                    if (PhotoModule.this.mSnapshotOnIdle) {
                        Log.v(PhotoModule.TAG, "postSnapRunnable");
                        PhotoModule.this.mHandler.post(PhotoModule.this.mDoSnapRunnable);
                    }
                    Log.d(PhotoModule.TAG, "setPreviewDataCallback");
                    PhotoModule.this.updateHdrView();
                    PhotoModule.this.mHandler.post(new Runnable() {
                        public void run() {
                            PhotoModule.this.setPreviewCallback();
                        }
                    });
                }
            };
            if (GservicesHelper.useCamera2ApiThroughPortabilityLayer(this.mActivity)) {
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
                        PhotoModule.this.mHandler.post(new Runnable() {
                            public void run() {
                                if (PhotoModule.this.mIsInIntentReviewUI || PhotoModule.this.mUI.isCountingDown()) {
                                    Log.v(PhotoModule.TAG, "in intent review UI or counting down");
                                    return;
                                }
                                if (PhotoModule.this.mCameraState == 1) {
                                    PhotoModule.this.doShutterButtonClick(true);
                                }
                            }
                        });
                    }
                });
                this.mCameraDevice.startGestureDetection();
            }
        }
    }

    private void initMotionHelper() {
        Size previewSize = this.mCameraSettings.getCurrentPreviewSize();
        int motionWidth = previewSize.width();
        int motionHeight = previewSize.height();
        if (RATIO_18_9.equals(new DecimalFormat("#.00").format((((double) motionWidth) / 1.0d) / ((double) motionHeight)))) {
            motionWidth = 1280;
            motionHeight = MotionPictureHelper.CROP_FRAME_HEIGHT_9;
        }
        MotionPictureHelper.createHelper(motionWidth, motionHeight);
        this.mMotionHelper = MotionPictureHelper.getHelper();
        this.mMotionHelper.setMotionOn(true);
    }

    /* Access modifiers changed, original: protected */
    public boolean isDepthEnabled() {
        return Keys.isDepthOn(this.mActivity.getSettingsManager()) && !isCameraFrontFacing() && this.mAppController.getCurrentModuleIndex() == 4;
    }

    private void updateMotionState() {
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        boolean z = true;
        boolean isSupportMotion = getModuleId() == 4 || getModuleId() == 15;
        CameraSettings cameraSettings = this.mCameraSettings;
        if (!(isSupportMotion && Keys.isMotionOn(settingsManager) && isMotionShow())) {
            z = false;
        }
        cameraSettings.setMotionOn(z);
        if (MotionPictureHelper.getHelper() != null) {
            MotionPictureHelper.getHelper().setMotionOn(this.mCameraSettings.isMotionOn());
        }
        if (!this.mCameraSettings.isMotionOn() && this.mMotionHelper != null) {
            this.mMotionHelper.setMotionOn(false);
            this.mMotionHelper.release();
            this.mMotionHelper = null;
        }
    }

    private void setPreviewCallback() {
        if (this.mCameraDevice != null) {
            this.mCameraDevice.setPreviewDataCallback(this.mHandler, new CameraPreviewDataCallback() {
                public void onPreviewFrame(byte[] data, CameraProxy camera) {
                    if (PhotoModule.this.mPreviewBytesBack != null && PhotoModule.this.mIsGlMode) {
                        PhotoModule.this.mPreviewBytesBack.onPreviewData(data, null);
                    }
                    PhotoModule.this.mFrameCount = PhotoModule.this.mFrameCount + 1;
                    if (PhotoModule.this.mInHdrProcess || !PhotoModule.this.mNeedHdrBurst) {
                        PhotoModule.this.updateHdrView();
                        PhotoModule.this.mFrameCount = 0;
                        return;
                    }
                    if (PhotoModule.this.mFrameCount == 10) {
                        HDRCheckerFilter filter = new HDRCheckerFilter(new MAlgoProcessCallback() {
                            public void onCheckerProcessDone(float[] result) {
                                PhotoModule.this.mDrCheckResult.clear();
                                for (float dr : result) {
                                    PhotoModule.this.mDrCheckResult.add(Float.valueOf(dr));
                                }
                                PhotoModule.this.updateHdrView();
                            }
                        });
                        Size size = PhotoModule.this.mCameraSettings.getCurrentPreviewSize();
                        if (PhotoModule.this.mPreviewResult == null) {
                            Log.d(PhotoModule.TAG, "mPreviewResult == null");
                            return;
                        }
                        int iso = ((Integer) PhotoModule.this.mPreviewResult.get(TotalCaptureResult.SENSOR_SENSITIVITY)).intValue();
                        filter.init(size.width(), size.height(), size.width(), iso, PhotoModule.this.getEvValue(iso > 600));
                        PhotoModule.this.mFrameCount = 0;
                        filter.process(data);
                        filter.deinit();
                    }
                }
            });
            this.mCameraDevice.setPreviewResultCallback(this.mHandler, new CameraPreviewResultCallback() {
                public void onCaptureComplete(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    result.get(TotalCaptureResult.SENSOR_SENSITIVITY);
                    result.get(TotalCaptureResult.SENSOR_EXPOSURE_TIME);
                    Key<Integer> flash = new Key("org.codeaurora.qcamera3.flash.state", Integer.class);
                    if (result.get(flash) != null) {
                        boolean z = false;
                        PhotoModule.this.mNeedFocus = ((Integer) result.get(flash)).intValue() == 1;
                        PhotoModule photoModule = PhotoModule.this;
                        if (PhotoModule.this.mHdrEnabled && ((Integer) result.get(flash)).intValue() == 0) {
                            z = true;
                        }
                        photoModule.mNeedHdrBurst = z;
                    }
                    PhotoModule.this.mPreviewResult = result;
                    if (PhotoModule.this.mActivity.islaunchFromAssistant()) {
                        PhotoModule.this.mHandler.sendEmptyMessage(6);
                    }
                }
            });
            this.mCameraDevice.setCaptureResultCallback(this.mHandler, new CaptureComplete(this, null));
        }
    }

    private void updateHdrView() {
        boolean show;
        String hdrMode = Keys.getHdrMode(this.mActivity.getSettingsManager());
        if (ExtendKey.FLIP_MODE_OFF.equals(hdrMode) || !this.mNeedHdrBurst) {
            show = false;
        } else if ("auto".equals(hdrMode)) {
            boolean z = true;
            if (this.mDrCheckResult.size() <= 1) {
                z = false;
            }
            show = z;
        } else {
            show = true;
        }
        this.mAppController.getCameraAppUI().updateHdrViewVisable(show);
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
        this.mAppController.getCameraAppUI().updateHdrViewVisable(false);
        setCameraState(0);
        if (this.mFocusManager != null) {
            this.mFocusManager.onPreviewStopped();
        }
        SessionStatsCollector.instance().previewActive(false);
    }

    public void onSettingChanged(SettingsManager settingsManager, String key) {
        if (!this.isOptimizeSwitchCamera || !key.equals(Keys.KEY_CAMERA_ID)) {
            if (key.equals(Keys.KEY_FLASH_MODE)) {
                updateParametersFlashMode();
            }
            if (key.equals(Keys.KEY_CAMERA_HDR)) {
                if (Keys.isHdrOn(settingsManager) && isHdrShow()) {
                    settingsManager.set(this.mAppController.getCameraScope(), Keys.KEY_SCENE_MODE, this.mCameraCapabilities.getStringifier().stringify(SceneMode.HDR));
                    if (!isInBurstshot()) {
                        ToastUtil.showToast(this.mActivity, this.mActivity.getString(R.string.hdr_on_toast), 0);
                    }
                } else {
                    settingsManager.set(this.mAppController.getCameraScope(), Keys.KEY_SCENE_MODE, this.mCameraCapabilities.getStringifier().stringify(SceneMode.AUTO));
                }
            }
            if (!isInBurstshot()) {
                updateParametersSceneMode();
            }
            updateVisidionMode();
            updateNeedHdrBurst();
            updateMotionState();
            updateHdrView();
            if (key.equals(Keys.KEY_CAMERA_MOTION)) {
                if (Keys.isMotionOn(settingsManager)) {
                    this.mActivity.getCameraAppUI().setEffectEnable(false);
                    this.mActivity.getCameraAppUI().setBeautyEnable(false);
                    this.mActivity.getCameraAppUI().setBeautySeek(0.0f);
                    this.mActivity.getButtonManager().setSeekbarProgress(0);
                    this.mActivity.getCameraAppUI().showOrHideGLSurface(false);
                    updateBeautySeek(0.0f);
                    BeaurifyJniSdk.preViewInstance().nativeDisablePackage();
                    this.mIsGlMode = false;
                    requestCameraOpen();
                    initMotionHelper();
                } else if (this.mMotionHelper != null) {
                    this.mMotionHelper.setMotionOn(false);
                    this.mMotionHelper.release();
                    this.mMotionHelper = null;
                }
            }
            if (this.mCameraDevice != null) {
                this.mCameraDevice.applySettings(this.mCameraSettings);
            }
            setPreviewCallback();
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
            boolean z = false;
            this.mCameraSettings.setProMode(getModuleId() == 1);
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
                SessionStatsCollector instance = SessionStatsCollector.instance();
                if (this.mFocusManager.getFocusMode(this.mCameraSettings.getCurrentFocusMode()) == FocusMode.CONTINUOUS_PICTURE) {
                    z = true;
                }
                instance.autofocusActive(z);
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
                if (this.mZoomValue > 1.5f) {
                    isSuperResolutionEnabled();
                }
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
        this.mCameraSettings.setSizesLocked(false);
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        if (isCameraFrontFacing()) {
            pictureSizeKey = Keys.KEY_PICTURE_SIZE_FRONT;
        } else {
            pictureSizeKey = Keys.KEY_PICTURE_SIZE_BACK;
        }
        String pictureSize = settingsManager.getString(SettingsManager.SCOPE_GLOBAL, pictureSizeKey, SettingsUtil.getDefaultPictureSize(isCameraFrontFacing()));
        Size size = new Size(960, MotionPictureHelper.FRAME_HEIGHT_9);
        if (isDepthEnabled()) {
            size = SettingsUtil.getBokehPhotoSize(this.mActivity, pictureSize);
        } else {
            size = SettingsUtil.sizeFromString(pictureSize);
        }
        this.mCameraSettings.setPhotoSize(size);
        if (ApiHelper.IS_NEXUS_5) {
            if (ResolutionUtil.NEXUS_5_LARGE_16_BY_9.equals(pictureSize)) {
                this.mShouldResizeTo16x9 = true;
            } else {
                this.mShouldResizeTo16x9 = false;
            }
        }
        if (size != null) {
            Size optimalSize;
            Tag tag;
            Size optimalSize2 = CameraUtil.getOptimalPreviewSize(this.mActivity, this.mCameraCapabilities.getSupportedPreviewSizes(), ((double) size.width()) / ((double) size.height()));
            Size original = this.mCameraSettings.getCurrentPreviewSize();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Photo module Set preview size ");
            stringBuilder.append(size);
            stringBuilder.append(" calculate size ");
            stringBuilder.append(optimalSize2);
            stringBuilder.append(" original size ");
            stringBuilder.append(original);
            android.util.Log.e("===++++++=====", stringBuilder.toString());
            if (isDepthEnabled()) {
                optimalSize = SettingsUtil.getBokehPreviewSize(pictureSize, false);
            } else {
                if ((size.width() == 4160 && size.height() == 1970) || (size.width() == 3264 && size.height() == 1546)) {
                    optimalSize2 = new Size(1440, MotionPictureHelper.FRAME_HEIGHT_9);
                }
                optimalSize = optimalSize2;
                this.mActivity.getCameraAppUI().setSurfaceHeight(optimalSize.height());
                this.mActivity.getCameraAppUI().setSurfaceWidth(optimalSize.width());
            }
            this.mUI.setCaptureSize(optimalSize);
            Log.w(TAG, String.format("KPI original size is %s, optimal size is %s", new Object[]{original.toString(), optimalSize.toString()}));
            if (!optimalSize.equals(original)) {
                tag = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("setting preview size. optimal: ");
                stringBuilder2.append(optimalSize);
                stringBuilder2.append("original: ");
                stringBuilder2.append(original);
                Log.v(tag, stringBuilder2.toString());
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
            this.mCameraSettings.setSizesLocked(true);
            tag = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Preview size is ");
            stringBuilder3.append(optimalSize);
            Log.d(tag, stringBuilder3.toString());
        }
    }

    private void updateParametersPictureQuality() {
        this.mCameraSettings.setPhotoJpegCompressionQuality(CameraProfile.getJpegEncodingQualityParameter(this.mCameraId > 1 ? 0 : this.mCameraId, 2));
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
        } else if (getModuleId() != this.mAppController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_pro)) {
            setExposureCompensation(0);
        }
    }

    private void updateParametersAntibanding() {
        this.mCameraSettings.setAntibanding(Keys.getAntibandingValue(this.mActivity.getSettingsManager()));
    }

    private void updateParametersSaveDng() {
        boolean enable = saveDngEnabled();
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("raw data saving enabled:");
        stringBuilder.append(enable);
        Log.d(tag, stringBuilder.toString());
        this.mCameraSettings.setSaveDngEnabled(enable);
    }

    private boolean saveDngEnabled() {
        return this.mActivity.getSettingsManager().getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_RAW_FILE);
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
        }
        this.mCameraSettings.setSceneMode(this.mSceneMode);
        updateParametersFlashMode();
        if (SceneMode.AUTO == this.mSceneMode) {
            updateParametersFocusMode();
        } else {
            this.mFocusManager.overrideFocusMode(this.mCameraSettings.getCurrentFocusMode());
        }
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
        if (activeRegion != null) {
            int xCenter = activeRegion.width() / 2;
            int yCenter = activeRegion.height() / 2;
            int xDelta = (int) (((float) activeRegion.width()) / (this.mZoomValue * 2.0f));
            int yDelta = (int) (((float) activeRegion.height()) / (2.0f * this.mZoomValue));
            cropRegion.set(xCenter - xDelta, yCenter - yDelta, xCenter + xDelta, yCenter + yDelta);
        }
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
        if (type == 1 || type == 2) {
            for (int i = 0; i < 3; i++) {
                data[i] = event.values[i];
            }
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
                PhotoModule.this.onShutterButtonClick();
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

    public boolean isGesturePalmShow() {
        return false;
    }

    private int getMaskImageRotation(int curOritation) {
        return (360 - curOritation) % 360;
    }

    private void updateThumbal(Bitmap bitmap) {
        this.mActivity.getCameraAppUI().updatePeekThumbUri(null);
        this.mActivity.getCameraAppUI().updatePeekThumbBitmapWithAnimation(bitmap);
    }

    private void updateBeautySeek(float chooseValue) {
        BeaurifyJniSdk.preViewInstance().nativeSetBeautyParam(3, chooseValue);
        BeaurifyJniSdk.preViewInstance().nativeSetBeautyParam(5, chooseValue);
        BeaurifyJniSdk.preViewInstance().nativeSetBeautyParam(4, chooseValue);
        BeaurifyJniSdk.preViewInstance().nativeSetBeautyParam(2, chooseValue);
        BeaurifyJniSdk.preViewInstance().nativeSetBeautyParam(1, chooseValue);
    }

    public boolean isSupportEffects() {
        return false;
    }

    public boolean isSupportBeauty() {
        return true;
    }

    public void openOrCloseEffects(int state, int effects) {
        boolean isEffectEnable = this.mActivity.getCameraAppUI().getEffectEnable();
        boolean isBeautyEnable = this.mActivity.getCameraAppUI().getBeautyEnable();
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("openOrCloseEffects mIsGlMode = ");
        stringBuilder.append(this.mIsGlMode);
        stringBuilder.append(",  isEffectEnable = ");
        stringBuilder.append(isEffectEnable);
        stringBuilder.append(" , isBeautyEnalbe = ");
        stringBuilder.append(isBeautyEnable);
        stringBuilder.append(",state = ");
        stringBuilder.append(state);
        Log.d(tag, stringBuilder.toString());
        if (this.mIsGlMode || (state < 0 && effects < 0)) {
            if (!isEffectEnable && !isBeautyEnable) {
                Log.d(TAG, "openOrCloseEffects closeEffects");
                this.mIsGlMode = false;
                this.mActivity.getCameraAppUI().showOrHideGLSurface(false);
                requestCameraOpen();
            }
        } else if (!isEffectEnable || !isBeautyEnable) {
            Log.d(TAG, "openOrCloseEffects openEffects");
            this.mIsGlMode = true;
            setPreviewBytesBack(this.mActivity.getCameraAppUI().getPreviewCallback());
            this.mActivity.getCameraAppUI().setOrientation(this.mCameraId);
            this.mActivity.getCameraAppUI().showOrHideGLSurface(true);
            requestCameraOpen();
        }
    }

    public boolean onGLRenderEnable() {
        return this.mActivity.getCameraAppUI().getEffectEnable() || this.mActivity.getCameraAppUI().getBeautyEnable();
    }

    public void setPreviewBytesBack(onPreviewBytes back) {
        this.mPreviewBytesBack = back;
    }

    /* Access modifiers changed, original: protected */
    public String getManualEtValue() {
        return this.mManualEtValue;
    }

    /* Access modifiers changed, original: protected */
    public void setManualEtValue(String etValue) {
        this.mManualEtValue = etValue;
    }
}
