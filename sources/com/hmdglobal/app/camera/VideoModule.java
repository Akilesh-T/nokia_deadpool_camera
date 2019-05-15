package com.hmdglobal.app.camera;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.location.Location;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.CamcorderProfile;
import android.media.CameraProfile;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.support.v4.media.MediaPlayer2;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import com.android.ex.camera2.portability.CameraAgent.CameraAFCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraAFMoveCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraPictureCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.android.ex.camera2.portability.CameraAgent.CameraStartPreviewCallback;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraCapabilities.Feature;
import com.android.ex.camera2.portability.CameraCapabilities.FlashMode;
import com.android.ex.camera2.portability.CameraCapabilities.FocusMode;
import com.android.ex.camera2.portability.CameraCapabilities.Stringifier;
import com.android.ex.camera2.portability.CameraDeviceInfo.Characteristics;
import com.android.ex.camera2.portability.CameraSettings;
import com.android.ex.camera2.portability.Size;
import com.android.external.ExtendKey;
import com.hmdglobal.app.camera.ButtonManager.ButtonCallback;
import com.hmdglobal.app.camera.CameraActivity.OnBatteryLowListener;
import com.hmdglobal.app.camera.CameraActivity.OnInnerStorageLowListener;
import com.hmdglobal.app.camera.FocusOverlayManager.Listener;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.app.CameraAppUI;
import com.hmdglobal.app.camera.app.CameraAppUI.BottomBarUISpec;
import com.hmdglobal.app.camera.app.LocationManager;
import com.hmdglobal.app.camera.app.MediaSaver;
import com.hmdglobal.app.camera.app.MediaSaver.OnMediaSavedListener;
import com.hmdglobal.app.camera.app.MemoryManager.MemoryListener;
import com.hmdglobal.app.camera.beauty.cameragl.CameraRender.onVideoStopListener;
import com.hmdglobal.app.camera.beauty.util.ImageUtils;
import com.hmdglobal.app.camera.data.VideoRotationMetadataLoader;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.exif.ExifInterface;
import com.hmdglobal.app.camera.hardware.HardwareSpec;
import com.hmdglobal.app.camera.hardware.HardwareSpecImpl;
import com.hmdglobal.app.camera.module.ModuleController;
import com.hmdglobal.app.camera.provider.InfoTable;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.settings.SettingsManager.OnSettingChangedListener;
import com.hmdglobal.app.camera.settings.SettingsUtil;
import com.hmdglobal.app.camera.ui.BottomBar.BottomBarSizeListener;
import com.hmdglobal.app.camera.ui.TouchCoordinate;
import com.hmdglobal.app.camera.util.ApiHelper;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.hmdglobal.app.camera.util.CustomFields;
import com.hmdglobal.app.camera.util.CustomUtil;
import com.hmdglobal.app.camera.util.ToastUtil;
import com.hmdglobal.app.camera.util.UsageStatistics;
import com.megvii.beautify.jni.BeaurifyJniSdk;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class VideoModule extends CameraModule implements ModuleController, VideoController, MemoryListener, OnErrorListener, OnSettingChangedListener, OnInfoListener, Listener {
    private static final String EXTRA_QUICK_CAPTURE = "android.intent.extra.quickCapture";
    protected static final long MIN_VIDEO_RECODER_DURATION = 1000;
    protected static final int MSG_CHECK_DISPLAY_ROTATION = 4;
    protected static final int MSG_ENABLE_SHUTTER_BUTTON = 6;
    private static final int MSG_SET_EV = 100;
    protected static final int MSG_START_RECORDING = 11;
    protected static final int MSG_STOP_RECORDING = 10;
    protected static final int MSG_SWITCH_CAMERA = 8;
    protected static final int MSG_SWITCH_CAMERA_START_ANIMATION = 9;
    protected static final int MSG_UPDATE_RECORD_TIME = 5;
    private static final long RECORDER_TIME_OFFSET = 600;
    protected static final long SHUTTER_BUTTON_TIMEOUT = 500;
    private static final Tag TAG = new Tag("VideoModule");
    private static final String VIDEO_MODULE_STRING_ID = "VideoModule";
    protected static final String VIDEO_TEMP_SUFFIXES = ".tmp";
    private boolean PhoneFlag;
    private boolean isAeAfLocked;
    private boolean isFocused;
    private boolean isTL = false;
    protected CameraActivity mActivity;
    protected AppController mAppController;
    private final OnAudioFocusChangeListener mAudioFocusChangeListener;
    private AudioManager mAudioManager;
    private final CameraAFCallback mAutoFocusCallback = new CameraAFCallback() {
        public void onAutoFocus(boolean focused, CameraProxy camera) {
            if (!VideoModule.this.mPaused) {
                if (!(!focused || VideoModule.this.mFocusManager == null || VideoModule.this.mFocusManager.getFocusAreas() == null)) {
                    VideoModule.this.isFocused = true;
                    VideoModule.this.mUI.showEvoSlider();
                }
                int action = FocusOverlayManager.ACTION_RESTORE_CAF_LATER;
                if (VideoModule.this.needEnableExposureAdjustment()) {
                    action |= FocusOverlayManager.ACTION_KEEP_FOCUS_FRAME;
                }
                Tag access$100 = VideoModule.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("on auto focus call back , focused: ");
                stringBuilder.append(focused);
                stringBuilder.append(" action:");
                stringBuilder.append(action);
                Log.v(access$100, stringBuilder.toString());
                VideoModule.this.mFocusManager.onAutoFocus(focused, action);
            }
        }
    };
    private final Object mAutoFocusMoveCallback;
    private final ButtonCallback mCameraCallback;
    protected CameraCapabilities mCameraCapabilities;
    protected CameraProxy mCameraDevice;
    protected Object mCameraDeviceLock = new Object();
    protected int mCameraDisplayOrientation;
    private int mCameraId;
    protected CameraSettings mCameraSettings;
    protected int mCameraState;
    private final OnClickListener mCancelCallback;
    protected ContentResolver mContentResolver;
    protected String mCurrentVideoFilename;
    protected Uri mCurrentVideoUri;
    protected boolean mCurrentVideoUriFromMediaSaved;
    private ContentValues mCurrentVideoValues;
    private int mDesiredPreviewHeight;
    private int mDesiredPreviewWidth;
    protected int mDisplayRotation;
    private final OnClickListener mDoneCallback;
    private boolean mDontResetIntentUiOnResume;
    private Handler mEVHandler = new Handler() {
        public void handleMessage(Message msg) {
            VideoModule.this.setEv(msg.arg1);
        }
    };
    private boolean mFaceDetectionStarted;
    private boolean mFirstEnter;
    private final ButtonCallback mFlashCallback;
    private boolean mFocusAreaSupported;
    protected FocusOverlayManager mFocusManager;
    protected final Handler mHandler = new MainHandler();
    private boolean mIsCameraOpened = false;
    private boolean mIsInReviewMode;
    protected boolean mIsVideoCaptureIntent;
    private long mLastTakePictureTime = -1;
    private long mLastVideoDuration;
    private LocationManager mLocationManager;
    private int mMaxVideoDurationInMs;
    protected boolean mMediaRecoderRecordingPaused = false;
    private MediaRecorder mMediaRecorder;
    protected boolean mMediaRecorderRecording = false;
    private int mMediaRemcoderRotation;
    private boolean mMeteringAreaSupported;
    private boolean mMirror;
    private Integer mModeSelectionLockToken;
    private AlertDialog mMountedDialog;
    private boolean mNeedGLRender;
    private final OnMediaSavedListener mOnPhotoSavedListener = new OnMediaSavedListener() {
        public void onMediaSaved(Uri uri) {
            if (uri != null) {
                VideoModule.this.mActivity.notifyNewMedia(uri, 2);
            }
        }
    };
    private long mOnResumeTime;
    private final OnMediaSavedListener mOnVideoSavedListener = new OnMediaSavedListener() {
        public void onMediaSaved(Uri uri) {
            if (uri != null) {
                VideoModule.this.mCurrentVideoUri = uri;
                VideoModule.this.mCurrentVideoUriFromMediaSaved = true;
                VideoModule.this.onVideoSaved();
                VideoModule.this.mActivity.notifyNewMedia(uri);
                if (VideoModule.this.mVideoBoomKeyFlags) {
                    SettingsManager settingsManager = VideoModule.this.mActivity.getSettingsManager();
                    boolean bTizrPrompt = settingsManager.isSet(SettingsManager.SCOPE_GLOBAL, Keys.KEY_TIZR_PROMPT) ^ true;
                    try {
                        VideoModule.this.mVideoBoomKeyFlags = false;
                        Intent i;
                        if (bTizrPrompt) {
                            i = new Intent(VideoModule.this.mActivity, TizrShareVideoActivity.class);
                            i.setData(VideoModule.this.mCurrentVideoUri);
                            VideoModule.this.mActivity.startActivity(i);
                            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_TIZR_PROMPT, true);
                            return;
                        }
                        Log.e(VideoModule.TAG, "Tony startActivity to TiZR");
                        i = new Intent("android.intent.action.VIEW", Uri.parse(CameraUtil.TIZR_URI));
                        i.setFlags(67108864);
                        VideoModule.this.mActivity.startActivity(i);
                        return;
                    } catch (Exception e) {
                        Log.e(VideoModule.TAG, "Tony VideoBoom exception");
                        e.printStackTrace();
                        return;
                    }
                }
                return;
            }
            ToastUtil.showToast(VideoModule.this.mActivity, VideoModule.this.mActivity.getString(R.string.error_when_saving_video_toast), 0);
            ToastUtil.showToast(VideoModule.this.mActivity, VideoModule.this.mActivity.getString(R.string.videos_were_not_saved_to_the_device_toast), 0);
        }
    };
    private int mOrientation = 0;
    protected boolean mPaused;
    private int mPendingSwitchCameraId;
    private PhoneStateListener mPhoneStateListener;
    private PictureTaskListener mPictureTaken;
    private boolean mPreferenceRead;
    protected CamcorderProfile mProfile;
    private boolean mQuickCapture;
    private BroadcastReceiver mReceiver;
    private boolean mRecordingInterrupted;
    protected long mRecordingStartTime;
    private boolean mRecordingTimeCountsDown = false;
    private final OnClickListener mReviewCallback;
    long mShutterButtonClickTime;
    private int mShutterIconId;
    private boolean mSnapshotInProgress = false;
    protected SoundPlayer mSoundPlayer;
    private onVideoStopListener mStopListener;
    private boolean mStorageLocationCheck = false;
    protected boolean mSwitchingCamera;
    private TelephonyManager mTelephonyManager = null;
    private int mTempEV = -100;
    private int mTempZoom = -100;
    private int mTimeLapseMultiple;
    private VideoUI mUI;
    protected boolean mVideoBoomKeyFlags = false;
    private ParcelFileDescriptor mVideoFileDescriptor;
    protected String mVideoFilename;
    private long mVideoRecordedDuration = 0;
    private float mZoomValue;
    private AlertDialog quitDialog;
    private Runnable saveAndQuit;
    protected boolean slFocuse = true;

    private class MainHandler extends Handler {
        public MainHandler() {
            super(Looper.getMainLooper());
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 4:
                    if (!(CameraUtil.getDisplayRotation(VideoModule.this.mActivity) == VideoModule.this.mDisplayRotation || VideoModule.this.mMediaRecorderRecording || VideoModule.this.mSwitchingCamera)) {
                        VideoModule.this.startPreview();
                    }
                    if (SystemClock.uptimeMillis() - VideoModule.this.mOnResumeTime < 5000) {
                        VideoModule.this.mHandler.sendEmptyMessageDelayed(4, 100);
                        return;
                    }
                    return;
                case 5:
                    VideoModule.this.updateRecordingTime();
                    return;
                case 6:
                    VideoModule.this.mAppController.setShutterEnabled(true);
                    return;
                case 8:
                    VideoModule.this.switchCamera();
                    return;
                case 9:
                    VideoModule.this.mSwitchingCamera = false;
                    return;
                case 10:
                    VideoModule.this.onStopVideoRecording();
                    return;
                case 11:
                    VideoModule.this.onShutterButtonClick();
                    return;
                default:
                    Tag access$100 = VideoModule.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unhandled message: ");
                    stringBuilder.append(msg.what);
                    Log.v(access$100, stringBuilder.toString());
                    return;
            }
        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        private MyBroadcastReceiver() {
        }

        /* synthetic */ MyBroadcastReceiver(VideoModule x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
        }
    }

    public class PictureTaskListener implements Runnable {
        public Bitmap modifiedBytes;
        public byte[] originalBytes;

        public void run() {
            Size s = VideoModule.this.mCameraSettings.getCurrentPhotoSize();
            ExifInterface exif = Exif.getExif(this.originalBytes);
            final String title = CameraUtil.createJpegName(System.currentTimeMillis());
            new Thread(new Runnable() {
                public void run() {
                    CameraActivity cameraActivity = VideoModule.this.mActivity;
                    Bitmap bitmap = PictureTaskListener.this.modifiedBytes;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(title);
                    stringBuilder.append(Storage.JPEG_POSTFIX);
                    Uri uri = ImageUtils.saveImageToGallery(cameraActivity, bitmap, stringBuilder.toString(), VideoModule.this.mLocationManager.getCurrentLocation(), VideoModule.this.mDisplayRotation, VideoModule.this.mOrientation);
                    if (uri != null) {
                        VideoModule.this.mActivity.notifyNewMedia(uri);
                    }
                }
            }).start();
            VideoModule.this.mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    VideoModule.this.mActivity.updateStorageSpaceAndHint(null);
                }
            });
        }
    }

    private final class JpegPictureCallback implements CameraPictureCallback {
        Location mLocation;

        public JpegPictureCallback(Location loc) {
            this.mLocation = loc;
        }

        public void onPictureTaken(byte[] jpegData, CameraProxy camera) {
            Log.i(VideoModule.TAG, "Video snapshot taken.");
            VideoModule.this.mSnapshotInProgress = false;
            if (!VideoModule.this.mPaused) {
                VideoModule.this.showVideoSnapshotUI(false);
                VideoModule.this.storeImage(jpegData, this.mLocation);
            }
        }
    }

    /* Access modifiers changed, original: protected */
    public OnMediaSavedListener getVideoSavedListener() {
        return this.mOnVideoSavedListener;
    }

    /* Access modifiers changed, original: protected */
    public boolean getCameraPreviewState() {
        return this.mCameraState == 3;
    }

    /* Access modifiers changed, original: protected */
    public boolean needEnableExposureAdjustment() {
        if (CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_ENABLE_COMPENSATION_OTHER_THAN_AUTO, true)) {
            return true;
        }
        return false;
    }

    private void readCameraInitialParameters() {
        if (this.mCameraDevice != null) {
            this.mUI.parseEvoBound(this.mCameraDevice.getCapabilities().getMaxExposureCompensation(), this.mCameraDevice.getCapabilities().getMinExposureCompensation());
        }
    }

    public void onSettingChanged(SettingsManager settingsManager, String key) {
        if (key.equals(Keys.KEY_VIDEOCAMERA_FLASH_MODE)) {
            if (this.mCameraCapabilities.getStringifier().stringify(FlashMode.OFF).equals(settingsManager.getString(this.mAppController.getCameraScope(), Keys.KEY_VIDEOCAMERA_FLASH_MODE))) {
                FlashMode mode = FlashMode.OFF;
                if (this.mCameraCapabilities.supports(mode)) {
                    this.mCameraSettings.setFlashMode(mode);
                    if (this.mCameraDevice != null) {
                        this.mCameraDevice.applySettings(this.mCameraSettings);
                    }
                }
            }
        }
    }

    /* Access modifiers changed, original: protected */
    public void onMediaAction(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals("android.intent.action.MEDIA_EJECT")) {
            String currentPath = Storage.getSavePath();
            if (currentPath != null && currentPath.equals("1") && this.mMediaRecorderRecording) {
                this.mRecordingInterrupted = true;
                onStopVideoRecording();
            }
        } else if (action.equals("android.intent.action.MEDIA_SCANNER_STARTED")) {
            ToastUtil.showToast(this.mActivity, this.mActivity.getResources().getString(R.string.wait), 1);
        }
    }

    public VideoModule(AppController app) {
        Object anonymousClass5;
        super(app);
        if (ApiHelper.HAS_AUTO_FOCUS_MOVE_CALLBACK) {
            anonymousClass5 = new CameraAFMoveCallback() {
                public void onAutoFocusMoving(boolean moving, CameraProxy camera) {
                    VideoModule.this.mUI.clearEvoPendingUI();
                    VideoModule.this.mFocusManager.onAutoFocusMoving(moving);
                }
            };
        } else {
            anonymousClass5 = null;
        }
        this.mAutoFocusMoveCallback = anonymousClass5;
        this.mReceiver = null;
        this.mRecordingInterrupted = false;
        this.mFlashCallback = new ButtonCallback() {
            public void onStateChanged(int state) {
                VideoModule.this.enableTorchMode(state != 0);
            }
        };
        this.mCameraCallback = new ButtonCallback() {
            public void onStateChanged(int state) {
                if (!VideoModule.this.mPaused && !VideoModule.this.mAppController.getCameraProvider().waitingForCamera()) {
                    VideoModule.this.mPendingSwitchCameraId = state;
                    Log.d(VideoModule.TAG, "Start to copy texture.");
                    VideoModule.this.mSwitchingCamera = true;
                    VideoModule.this.switchCamera();
                }
            }
        };
        this.mCancelCallback = new OnClickListener() {
            public void onClick(View v) {
                VideoModule.this.onReviewCancelClicked(v);
            }
        };
        this.mDoneCallback = new OnClickListener() {
            public void onClick(View v) {
                VideoModule.this.onReviewDoneClicked(v);
            }
        };
        this.mReviewCallback = new OnClickListener() {
            public void onClick(View v) {
                VideoModule.this.onReviewPlayClicked(v);
            }
        };
        this.mShutterButtonClickTime = 0;
        this.mAudioFocusChangeListener = new OnAudioFocusChangeListener() {
            public void onAudioFocusChange(int focusChange) {
                Tag access$100 = VideoModule.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("test ->onAudioFocusChange focusChange");
                stringBuilder.append(focusChange);
                Log.d(access$100, stringBuilder.toString());
                switch (focusChange) {
                    case -2:
                    case -1:
                        VideoModule.this.stopVideoWhileAudioFocusLoss();
                        return;
                    default:
                        return;
                }
            }
        };
        this.PhoneFlag = false;
        this.mPhoneStateListener = new PhoneStateListener() {
            public void onCallStateChanged(int state, String incomingNumber) {
                if (state == 0) {
                    VideoModule.this.PhoneFlag = false;
                } else if (state != 2) {
                    Tag access$100 = VideoModule.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("onCallStateChanged:");
                    stringBuilder.append(state);
                    Log.d(access$100, stringBuilder.toString());
                    VideoModule.this.PhoneFlag = true;
                } else {
                    VideoModule.this.PhoneFlag = true;
                    if (VideoModule.this.mMediaRecorderRecording) {
                        Log.d(VideoModule.TAG, "minicallStopRecord");
                        VideoModule.this.onStopVideoRecording();
                    }
                }
            }
        };
        this.mModeSelectionLockToken = null;
        this.mLastVideoDuration = -1;
        this.mStopListener = new onVideoStopListener() {
            public void onVideoStoped(String path) {
                long duration;
                try {
                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    mmr.setDataSource(path);
                    String durationStr = mmr.extractMetadata(9);
                    mmr.release();
                    duration = (long) Integer.parseInt(durationStr);
                } catch (Exception e) {
                    duration = VideoModule.this.mLastVideoDuration;
                    Log.e(VideoModule.TAG, "MediaMetadataRetriever error, use estimated duration", e);
                }
                if (VideoModule.this.mCurrentVideoValues == null) {
                    VideoModule.this.generateVideoFilename(VideoModule.this.mProfile.fileFormat);
                }
                VideoModule.this.mCurrentVideoValues.put("_size", Long.valueOf(new File(path).length()));
                VideoModule.this.mCurrentVideoValues.put(InfoTable.DURATION, Long.valueOf(duration));
                if (VideoModule.this.needAddToMediaSaver()) {
                    VideoModule.this.getServices().getMediaSaver().addVideo(path, VideoModule.this.mCurrentVideoValues, VideoModule.this.getVideoSavedListener(), VideoModule.this.mContentResolver);
                }
                VideoModule.this.logVideoCapture(duration);
            }
        };
        this.mTimeLapseMultiple = 3;
        this.mFirstEnter = true;
        this.mFaceDetectionStarted = false;
        this.mPictureTaken = new PictureTaskListener();
        this.saveAndQuit = new Runnable() {
            public void run() {
                VideoModule.this.onStopVideoRecording();
                VideoModule.this.mActivity.finish();
            }
        };
        this.mCameraState = 0;
        this.mNeedGLRender = false;
    }

    public String getPeekAccessibilityString() {
        return this.mAppController.getAndroidContext().getResources().getString(R.string.video_accessibility_peek);
    }

    private String createName(long dateTaken) {
        return new SimpleDateFormat(this.mActivity.getString(R.string.video_file_name_format)).format(new Date(dateTaken));
    }

    public String getModuleStringIdentifier() {
        return "VideoModule";
    }

    /* Access modifiers changed, original: protected */
    public VideoUI getVideoUI() {
        return new VideoUI(this.mActivity, this, this.mActivity.getModuleLayoutRoot());
    }

    public void init(CameraActivity activity, boolean isSecureCamera, boolean isCaptureIntent) {
        this.mActivity = activity;
        this.mAppController = this.mActivity;
        setCameraState(0);
        this.mActivity.updateStorageSpaceAndHint(null);
        this.mUI = getVideoUI();
        this.mActivity.setPreviewStatusListener(this.mUI);
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        settingsManager.addListener(this);
        if (this.mActivity.islaunchFromAssistant()) {
            this.mCameraId = this.mActivity.isUserFrontCamera();
        } else {
            this.mCameraId = settingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID).intValue();
        }
        this.mContentResolver = this.mActivity.getContentResolver();
        this.mIsVideoCaptureIntent = isVideoCaptureIntent();
        this.mQuickCapture = this.mActivity.getIntent().getBooleanExtra(EXTRA_QUICK_CAPTURE, false);
        this.mLocationManager = this.mActivity.getLocationManager();
        this.mAudioManager = (AudioManager) this.mActivity.getSystemService("audio");
        this.mSoundPlayer = new SoundPlayer(this.mAppController.getAndroidContext());
        this.mUI.setOrientationIndicator(0, false);
        this.mAppController.getCameraAppUI().updateHdrViewVisable(false);
        setDisplayOrientation();
        this.mPendingSwitchCameraId = -1;
        this.mShutterIconId = CameraUtil.getCameraShutterIconId(this.mAppController.getCurrentModuleIndex(), this.mAppController.getAndroidContext());
    }

    /* Access modifiers changed, original: protected */
    public void pauseVideoAndRefreshUI() {
        this.mUI.pauseVideo();
    }

    public boolean isUsingBottomBar() {
        return true;
    }

    private void initializeControlByIntent() {
        if (isVideoCaptureIntent()) {
            if (!this.mDontResetIntentUiOnResume) {
                this.mActivity.getCameraAppUI().transitionToIntentCaptureLayout();
            }
            this.mDontResetIntentUiOnResume = false;
        }
    }

    public void onSingleTapUp(View view, int x, int y) {
        if (!this.mPaused && this.mCameraDevice != null) {
            this.mUI.clearEvoPendingUI();
            this.mUI.initEvoSlider((float) x, (float) y);
            if (this.mFocusAreaSupported || this.mMeteringAreaSupported) {
                Focus();
                if (this.mFocusManager != null && (this.mFocusManager.getFocusMode(this.mCameraSettings.getCurrentFocusMode()) == FocusMode.CONTINUOUS_PICTURE || this.mCameraSettings.getCurrentFocusMode() == FocusMode.FIXED)) {
                    this.mCameraDevice.cancelAutoFocus();
                }
                this.mFocusManager.onSingleTapUp(x, y);
            }
        }
    }

    private boolean isCameraStateRecording() {
        return this.mCameraState == 2 || this.mCameraState == 3 || this.mCameraState == 4;
    }

    /* Access modifiers changed, original: protected */
    public void playVideoSound() {
        int mode = this.mAudioManager.getRingerMode();
        if (Keys.isShutterSoundOn(this.mAppController.getSettingsManager()) && mode == 2 && this.mSoundPlayer != null) {
            this.mSoundPlayer.play(R.raw.video_record, 0.6f);
        }
    }

    private void takeASnapshot() {
        if (!this.mCameraCapabilities.supports(Feature.VIDEO_SNAPSHOT)) {
            Log.w(TAG, "Cannot take a video snapshot - not supported by hardware");
        } else if (!this.mIsVideoCaptureIntent && this.mMediaRecorderRecording && !this.mPaused && !this.mSnapshotInProgress && this.mAppController.isShutterEnabled() && this.mCameraDevice != null) {
            Location loc = this.mLocationManager.getCurrentLocation();
            CameraUtil.setGpsParameters(this.mCameraSettings, loc);
            this.mCameraDevice.applySettings(this.mCameraSettings);
            int orientation = getJpegRotation(this.mOrientation);
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Video snapshot orientation is ");
            stringBuilder.append(orientation);
            Log.d(tag, stringBuilder.toString());
            this.mCameraDevice.setJpegOrientation(orientation);
            Log.i(TAG, "Video snapshot start");
            this.mCameraDevice.takePicture(this.mHandler, null, null, null, new JpegPictureCallback(loc));
            showVideoSnapshotUI(true);
            this.mSnapshotInProgress = true;
        }
    }

    private int getJpegRotation(int orientation) {
        int mJpegRotation = 0;
        if (orientation == -1) {
            return 0;
        }
        try {
            mJpegRotation = this.mActivity.getCameraProvider().getCharacteristics(this.mCameraId).getJpegOrientation((360 - orientation) % 360);
        } catch (Exception e) {
            Log.e(TAG, "Error when getJpegOrientation");
        }
        return mJpegRotation;
    }

    @TargetApi(16)
    private void updateAutoFocusMoveCallback() {
        if (!this.mPaused && this.mCameraDevice != null) {
            if (this.mCameraSettings.getCurrentFocusMode() == FocusMode.CONTINUOUS_PICTURE || this.mCameraSettings.getCurrentFocusMode() == FocusMode.FIXED) {
                this.mCameraDevice.setAutoFocusMoveCallback(this.mHandler, (CameraAFMoveCallback) this.mAutoFocusMoveCallback);
            } else {
                this.mCameraDevice.setAutoFocusMoveCallback(null, null);
            }
        }
    }

    /* Access modifiers changed, original: protected */
    public boolean isCameraFrontFacing() {
        return this.mAppController.getCameraProvider().getCharacteristics(this.mCameraId).isFacingFront();
    }

    private boolean isCameraBackFacing() {
        return this.mAppController.getCameraProvider().getCharacteristics(this.mCameraId).isFacingBack();
    }

    private void initializeFocusManager() {
        if (this.mFocusManager != null) {
            this.mFocusManager.removeMessages();
        } else {
            this.mMirror = isCameraFrontFacing();
            String[] defaultFocusModesStrings = this.mActivity.getResources().getStringArray(R.array.pref_camera_focusmode_default_array);
            Stringifier stringifier = this.mCameraCapabilities.getStringifier();
            ArrayList<FocusMode> defaultFocusModes = new ArrayList();
            for (String modeString : defaultFocusModesStrings) {
                FocusMode mode = stringifier.focusModeFromString(modeString);
                if (mode != null) {
                    defaultFocusModes.add(mode);
                }
            }
            this.mFocusManager = new FocusOverlayManager(this.mAppController, defaultFocusModes, this.mCameraCapabilities, this, this.mMirror, this.mActivity.getMainLooper(), this.mUI.getFocusUI());
        }
        this.mAppController.addPreviewAreaSizeChangedListener(this.mFocusManager);
    }

    public void onOrientationChanged(int orientation) {
        if (orientation != -1) {
            int newOrientation = CameraUtil.roundOrientation(orientation, this.mOrientation);
            if (this.mOrientation != newOrientation) {
                this.mOrientation = newOrientation;
            }
            this.mUI.onOrientationChanged(orientation);
        }
    }

    public void intentReviewCancel() {
        if (this.mCurrentVideoUriFromMediaSaved) {
            this.mContentResolver.delete(this.mCurrentVideoUri, null, null);
        }
        this.mIsInReviewMode = false;
        this.mUI.hideReviewControls();
        closeCamera();
        requestCameraOpen();
    }

    public void intentReviewDone() {
        onReviewDoneClicked(null);
    }

    public void intentReviewPlay() {
        onReviewPlayClicked(null);
    }

    public void hardResetSettings(SettingsManager settingsManager) {
    }

    public HardwareSpec getHardwareSpec() {
        return this.mCameraSettings != null ? new HardwareSpecImpl(getCameraProvider(), this.mCameraCapabilities) : null;
    }

    public BottomBarUISpec getBottomBarSpec() {
        BottomBarUISpec bottomBarSpec = new BottomBarUISpec();
        bottomBarSpec.enableCamera = true;
        bottomBarSpec.hideCamera = hideCamera();
        bottomBarSpec.hideCameraForced = false;
        bottomBarSpec.hideSetting = isVideoCaptureIntent();
        bottomBarSpec.cameraCallback = this.mCameraCallback;
        bottomBarSpec.enableTorchFlash = this.mActivity.currentBatteryStatusOK();
        bottomBarSpec.flashCallback = this.mFlashCallback;
        bottomBarSpec.hideHdr = true;
        bottomBarSpec.hideFlash = false;
        bottomBarSpec.hideLive = false;
        bottomBarSpec.showEffect2 = false;
        bottomBarSpec.showBeautyButton = false;
        bottomBarSpec.showEffectButton = false;
        bottomBarSpec.hideBolken = true;
        bottomBarSpec.hideFlash = false;
        bottomBarSpec.hideGridLines = true;
        bottomBarSpec.hideLowlight = true;
        bottomBarSpec.moduleName = VideoModule.class.getSimpleName();
        if (isCameraFrontFacing()) {
            ModuleController controller = this.mAppController.getCurrentModuleController();
            if (!(controller.getHardwareSpec() == null || controller.getHardwareSpec().isFlashSupported())) {
                bottomBarSpec.hideFlash = true;
            }
        }
        if (isVideoCaptureIntent()) {
            bottomBarSpec.showCancel = true;
            bottomBarSpec.cancelCallback = this.mCancelCallback;
            bottomBarSpec.showDone = true;
            bottomBarSpec.doneCallback = this.mDoneCallback;
            bottomBarSpec.showReview = true;
            bottomBarSpec.reviewCallback = this.mReviewCallback;
        }
        return bottomBarSpec;
    }

    public void onCameraAvailable(CameraProxy cameraProxy) {
        Log.w(TAG, "On CameraAvailable");
        if (cameraProxy == null) {
            Log.w(TAG, "onCameraAvailable returns a null CameraProxy object");
            return;
        }
        this.mAppController.getCameraAppUI().onPreviewStarted();
        this.mCameraDevice = cameraProxy;
        this.mCameraDevice.setModuleId(getModuleId(), this.mActivity.getWindowManager().getDefaultDisplay().getRotation());
        if (this.mNeedGLRender && isSupportEffects()) {
            this.mActivity.getCameraAppUI().initSurfaceRender(this.mCameraDevice);
            this.mActivity.getCameraAppUI().setOrientation(this.mCameraId);
        }
        this.mCameraCapabilities = this.mCameraDevice.getCapabilities();
        this.mCameraSettings = this.mCameraDevice.getSettings();
        this.mFocusAreaSupported = this.mCameraCapabilities.supports(Feature.FOCUS_AREA);
        this.mMeteringAreaSupported = this.mCameraCapabilities.supports(Feature.METERING_AREA);
        readVideoPreferences();
        updateDesiredPreviewSize();
        resizeForPreviewAspectRatio();
        initializeFocusManager();
        this.mFocusManager.updateCapabilities(this.mCameraCapabilities);
        readCameraInitialParameters();
        startPreview();
        initializeVideoSnapshot();
        this.mUI.initializeZoom(this.mCameraSettings, this.mCameraCapabilities);
        initializeControlByIntent();
    }

    private void startPlayVideoActivity() {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setFlags(1);
        intent.setDataAndType(this.mCurrentVideoUri, convertOutputFormatToMimeType(this.mProfile.fileFormat));
        try {
            this.mActivity.launchActivityByIntent(intent);
        } catch (ActivityNotFoundException ex) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Couldn't view video ");
            stringBuilder.append(this.mCurrentVideoUri);
            Log.e(tag, stringBuilder.toString(), ex);
        }
    }

    @OnClickAttr
    public void onReviewPlayClicked(View v) {
        startPlayVideoActivity();
    }

    @OnClickAttr
    public void onReviewDoneClicked(View v) {
        this.mIsInReviewMode = false;
        doReturnToCaller(true);
    }

    @OnClickAttr
    public void onReviewCancelClicked(View v) {
        if (this.mCurrentVideoUriFromMediaSaved) {
            this.mContentResolver.delete(this.mCurrentVideoUri, null, null);
        }
        this.mIsInReviewMode = false;
        doReturnToCaller(false);
    }

    public boolean isInReviewMode() {
        return this.mIsInReviewMode;
    }

    /* Access modifiers changed, original: protected */
    public boolean onStopVideoRecording() {
        if (this.mNeedGLRender && isSupportEffects()) {
            this.mCurrentVideoFilename = this.mVideoFilename;
            this.mActivity.getCameraAppUI().stopVideoRecorder();
        }
        if (this.PhoneFlag) {
            int moduleId = getModuleId();
            CameraActivity cameraActivity = this.mActivity;
            if (moduleId == 8) {
                this.mActivity.getCameraAppUI().setCalldisable(true);
                this.mActivity.getCameraAppUI().resetAlpha(true);
            }
        }
        if (this.mMediaRecoderRecordingPaused) {
            this.mRecordingStartTime = SystemClock.uptimeMillis() - this.mVideoRecordedDuration;
            this.mVideoRecordedDuration = 0;
            this.mMediaRecoderRecordingPaused = false;
            this.mUI.mMediaRecoderRecordingPaused = false;
            this.mUI.setRecordingTimeImage(true);
        }
        this.mAppController.getCameraAppUI().showRotateButton();
        this.mAppController.getCameraAppUI().setSwipeEnabled(true);
        this.mActivity.stopBatteryInfoChecking();
        this.mActivity.stopInnerStorageChecking();
        boolean recordFail = stopVideoRecording();
        releaseAudioFocus();
        if (this.mIsVideoCaptureIntent) {
            if (this.mQuickCapture) {
                doReturnToCaller(recordFail ^ 1);
            } else if (recordFail) {
                this.mAppController.getCameraAppUI().showModeOptions();
                this.mHandler.sendEmptyMessageDelayed(6, SHUTTER_BUTTON_TIMEOUT);
            } else {
                showCaptureResult();
            }
        } else if (!(recordFail || this.mPaused)) {
            boolean z = ApiHelper.HAS_SURFACE_TEXTURE_RECORDING;
        }
        return recordFail;
    }

    public void onVideoSaved() {
        if (this.mIsVideoCaptureIntent) {
            showCaptureResult();
        }
    }

    public void onProtectiveCurtainClick(View v) {
    }

    /* Access modifiers changed, original: protected */
    public void startVideoNotityHelpTip() {
    }

    private void StoreAceessDialog(CameraActivity activity) {
        if (Storage.getSavePath().equals("1") && !SDCard.instance().isWriteable()) {
            activity.getCameraAppUI().setViewFinderLayoutVisibile(true);
            if (this.mMountedDialog != null && this.mMountedDialog.isShowing()) {
                this.mMountedDialog.dismiss();
                this.mMountedDialog = null;
            }
            this.mMountedDialog = CameraUtil.UnAccessDialog(activity, activity.getResources().getString(R.string.sd_access_error), activity.getResources().getString(R.string.sd_access_video_error_message), activity.getResources().getString(R.string.alert_storage_dialog_ok));
            Log.d(TAG, "dialog is showing");
            if (Storage.getSavePath().equals("1")) {
                Storage.setSavePath("0");
                activity.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_SAVEPATH, "0");
                this.mStorageLocationCheck = true;
            }
        }
    }

    public void onShutterButtonClick() {
        this.mShutterButtonClickTime = System.currentTimeMillis();
        if (this.mAppController.getButtonManager().isMoreOptionsWrapperShow()) {
            this.mAppController.getButtonManager().hideMoreOptionsWrapper();
        } else if (!this.mSwitchingCamera && this.mCameraState != 0 && this.mCameraState != 2 && this.mCameraState != 4) {
            StoreAceessDialog(this.mActivity);
            if (this.mStorageLocationCheck) {
                this.mStorageLocationCheck = false;
                return;
            }
            boolean needStop = this.mCameraState == 3;
            if (needStop) {
                this.mAppController.getCameraAppUI().enableModeOptions();
                onStopVideoRecording();
            } else {
                startVideoNotityHelpTip();
                this.slFocuse = true;
                this.mAppController.getCameraAppUI().disableModeOptions();
                startVideoRecording();
            }
            this.mAppController.setShutterEnabled(false);
            if (this.mCameraSettings != null) {
                this.mFocusManager.onShutterUp(this.mCameraSettings.getCurrentFocusMode());
            }
            if ((needStop && !this.mIsVideoCaptureIntent) || !(needStop || shouldHoldRecorderForSecond())) {
                this.mHandler.sendEmptyMessageDelayed(6, SHUTTER_BUTTON_TIMEOUT);
            }
        }
    }

    public void onShutterButtonLongClick() {
    }

    public void onShutterCoordinate(TouchCoordinate coord) {
    }

    public void onShutterButtonFocus(boolean pressed) {
    }

    /* Access modifiers changed, original: protected */
    public int getOverrodeVideoDuration() {
        return this.mMaxVideoDurationInMs;
    }

    private void readVideoPreferences() {
        int quality = getProfileQuality();
        Intent intent = this.mActivity.getIntent();
        if (intent.hasExtra("android.intent.extra.videoQuality")) {
            if (intent.getIntExtra("android.intent.extra.videoQuality", 0) > 0) {
                quality = 1;
            } else {
                quality = 0;
            }
        }
        if (intent.hasExtra("android.intent.extra.durationLimit")) {
            this.mMaxVideoDurationInMs = 1000 * intent.getIntExtra("android.intent.extra.durationLimit", 0);
        } else {
            this.mMaxVideoDurationInMs = SettingsUtil.getMaxVideoDuration(this.mActivity.getAndroidContext());
        }
        if (!CamcorderProfile.hasProfile(this.mCameraId, quality)) {
            quality = 1;
        }
        this.mProfile = CamcorderProfile.get(this.mCameraId, quality);
        overrideProfileSize();
        this.mPreferenceRead = true;
    }

    /* Access modifiers changed, original: protected */
    public void overrideProfileSize() {
    }

    private void updateDesiredPreviewSize() {
        if (this.mCameraDevice != null) {
            this.mCameraSettings = this.mCameraDevice.getSettings();
            Point desiredPreviewSize = getDesiredPreviewSize(this.mAppController.getAndroidContext(), this.mCameraSettings, this.mCameraCapabilities, this.mProfile, this.mUI.getPreviewScreenSize());
            this.mDesiredPreviewWidth = desiredPreviewSize.x;
            this.mDesiredPreviewHeight = desiredPreviewSize.y;
            Size size = new Size(this.mDesiredPreviewWidth, this.mDesiredPreviewHeight);
            VideoRotationMetadataLoader.VIDEO_HEIGHT = size.height();
            VideoRotationMetadataLoader.VIDEO_WIDTH = size.width();
            this.mActivity.getCameraAppUI().setSurfaceHeight(this.mDesiredPreviewHeight);
            this.mActivity.getCameraAppUI().setSurfaceWidth(this.mDesiredPreviewWidth);
            this.mUI.setPreviewSize(this.mDesiredPreviewWidth, this.mDesiredPreviewHeight);
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Updated DesiredPreview=");
            stringBuilder.append(this.mDesiredPreviewWidth);
            stringBuilder.append("x");
            stringBuilder.append(this.mDesiredPreviewHeight);
            Log.v(tag, stringBuilder.toString());
        }
    }

    @TargetApi(11)
    private static Point getDesiredPreviewSize(Context context, CameraSettings settings, CameraCapabilities capabilities, CamcorderProfile profile, Point previewScreenSize) {
        if (capabilities.getSupportedVideoSizes() == null) {
            return new Point(profile.videoFrameWidth, profile.videoFrameHeight);
        }
        Size size;
        int previewScreenShortSide = previewScreenSize.x < previewScreenSize.y ? previewScreenSize.x : previewScreenSize.y;
        List<Size> sizes = capabilities.getSupportedPreviewSizes();
        Size preferred = capabilities.getPreferredPreviewSizeForVideo();
        if ((preferred.width() < preferred.height() ? preferred.width() : preferred.height()) * 2 < previewScreenShortSide) {
            preferred = new Size(profile.videoFrameWidth, profile.videoFrameHeight);
        }
        int product = preferred.width() * preferred.height();
        Iterator<Size> it = sizes.iterator();
        while (it.hasNext()) {
            size = (Size) it.next();
            if (size.width() * size.height() > product) {
                it.remove();
            }
        }
        for (Size size2 : sizes) {
            if (size2.width() == profile.videoFrameWidth && size2.height() == profile.videoFrameHeight) {
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Selected =");
                stringBuilder.append(size2.width());
                stringBuilder.append("x");
                stringBuilder.append(size2.height());
                stringBuilder.append(" on WYSIWYG Priority");
                Log.v(tag, stringBuilder.toString());
                return new Point(profile.videoFrameWidth, profile.videoFrameHeight);
            }
        }
        size = CameraUtil.getOptimalPreviewSize(context, sizes, ((double) profile.videoFrameWidth) / ((double) profile.videoFrameHeight));
        return new Point(size.width(), size.height());
    }

    private void resizeForPreviewAspectRatio() {
        this.mUI.setAspectRatio(((float) this.mProfile.videoFrameWidth) / ((float) this.mProfile.videoFrameHeight));
    }

    private void installIntentFilter() {
        IntentFilter intentFilter = new IntentFilter("android.intent.action.MEDIA_EJECT");
        intentFilter.addAction("android.intent.action.MEDIA_SCANNER_STARTED");
        intentFilter.addDataScheme("file");
        this.mReceiver = new MyBroadcastReceiver(this, null);
        this.mActivity.registerReceiver(this.mReceiver, intentFilter);
    }

    private void setDisplayOrientation() {
        this.mDisplayRotation = CameraUtil.getDisplayRotation(this.mActivity);
        Characteristics info = this.mActivity.getCameraProvider().getCharacteristics(this.mCameraId);
        if (info != null) {
            this.mCameraDisplayOrientation = info.getPreviewOrientation(this.mDisplayRotation);
        }
        if (this.mCameraDevice != null) {
            this.mCameraDevice.setDisplayOrientation(this.mDisplayRotation);
        }
        if (this.mFocusManager != null) {
            this.mFocusManager.setDisplayOrientation(this.mCameraDisplayOrientation);
        }
    }

    public void updateCameraOrientation() {
        if (!(this.mMediaRecorderRecording || this.mDisplayRotation == CameraUtil.getDisplayRotation(this.mActivity))) {
            setDisplayOrientation();
        }
    }

    public void updatePreviewAspectRatio(float aspectRatio) {
        this.mAppController.updatePreviewAspectRatio(aspectRatio);
    }

    private float currentZoomValue() {
        return this.mCameraSettings.getCurrentZoomRatio();
    }

    public void onZoomChanged(float ratio) {
        if (!this.mPaused) {
            int zoomR = zoomRatioToIndex(ratio, this.mCameraDevice.getCamera().getParameters().getZoomRatios());
            if (this.mTempZoom != zoomR) {
                this.mTempZoom = zoomR;
                this.mZoomValue = ratio;
                if (this.mCameraSettings != null && this.mCameraDevice != null) {
                    this.mCameraSettings.setZoomRatio(this.mZoomValue);
                    this.mCameraDevice.applySettings(this.mCameraSettings);
                }
            }
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

    private void startPreview() {
        Log.i(TAG, "startPreview");
        boolean z = false;
        SurfaceTexture surfaceTexture = this.mActivity.getCameraAppUI().getSurfaceTexture(false);
        if (!this.mPreferenceRead || surfaceTexture == null || this.mPaused || this.mCameraDevice == null) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mPreferenceRead = ");
            stringBuilder.append(this.mPreferenceRead);
            stringBuilder.append(" surfaceTexture is null ?");
            stringBuilder.append(surfaceTexture == null);
            stringBuilder.append(" mCameraDevice==null?");
            if (this.mCameraDevice == null) {
                z = true;
            }
            stringBuilder.append(z);
            Log.w(tag, stringBuilder.toString());
            return;
        }
        if (this.mCameraState != 0) {
            stopPreview();
        }
        setDisplayOrientation();
        this.mCameraDevice.setDisplayOrientation(this.mDisplayRotation);
        setCameraParameters();
        if (this.mFocusManager != null) {
            if (this.mFocusManager.getFocusMode(this.mCameraSettings.getCurrentFocusMode()) == FocusMode.CONTINUOUS_PICTURE) {
                this.mCameraDevice.cancelAutoFocus();
            }
            if (this.mNeedGLRender && isSupportEffects()) {
                this.mFocusManager.setAeAwbLock(false);
            }
        }
        if (ApiHelper.isLOrHigher()) {
            Log.v(TAG, "on L, no one shot callback necessary");
        } else {
            Log.v(TAG, "calling onPreviewReadyToStart to set one shot callback");
            this.mAppController.onPreviewReadyToStart();
        }
        try {
            CameraStartPreviewCallback cameraStartPreviewCallback = new CameraStartPreviewCallback() {
                public void onPreviewStarted() {
                    VideoModule.this.onPreviewStarted();
                    if (VideoModule.this.mActivity.islaunchFromAssistant()) {
                        VideoModule.this.mHandler.sendEmptyMessage(11);
                        VideoModule.this.mActivity.resetAssistantStatus();
                    }
                    new Thread() {
                        public void run() {
                            try {
                                Thread.sleep(VideoModule.SHUTTER_BUTTON_TIMEOUT);
                                VideoModule.this.mIsCameraOpened = true;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                }
            };
            if (this.mActivity.getCameraProvider().isBoostPreview()) {
                this.mCameraDevice.waitPreviewWithCallback(new Handler(Looper.getMainLooper()), cameraStartPreviewCallback);
            } else {
                this.mCameraDevice.setPreviewTexture(surfaceTexture);
                this.mCameraDevice.startPreviewWithCallback(new Handler(Looper.getMainLooper()), cameraStartPreviewCallback);
            }
            setCameraState(1);
            this.mAppController.getCameraAppUI().updateHdrViewVisable(false);
            this.mCameraDevice.setPreviewBolkenLevel(-1);
        } catch (Throwable ex) {
            RuntimeException runtimeException = new RuntimeException("startPreview failed", ex);
        }
    }

    /* Access modifiers changed, original: protected */
    public void onPreviewStarted() {
        Log.w(TAG, "KPI video preview started");
        this.mAppController.setShutterEnabled(true);
        this.mAppController.onPreviewStarted();
        this.mUI.clearEvoPendingUI();
        if (this.mFocusManager != null) {
            this.mFocusManager.onPreviewStarted();
        }
        if (isNeedStartRecordingOnSwitching()) {
            onShutterButtonClick();
        }
    }

    public int getModuleId() {
        return this.mAppController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_video);
    }

    /* Access modifiers changed, original: protected */
    public boolean isNeedStartRecordingOnSwitching() {
        return this.mIsVideoCaptureIntent;
    }

    public void onPreviewInitialDataReceived() {
    }

    public void stopPreview() {
        if (this.mCameraState == 0) {
            Log.v(TAG, "Skip stopPreview since it's not mPreviewing");
        } else if (this.mCameraDevice == null) {
            Log.v(TAG, "Skip stopPreview since mCameraDevice is null");
        } else {
            stopFaceDetection();
            this.mCameraDevice.cancelAutoFocus();
            Log.v(TAG, "stopPreview");
            this.mCameraDevice.stopPreview();
            this.mActivity.clearBoost();
            enableTorchMode(false);
            if (this.mFocusManager != null) {
                this.mFocusManager.onPreviewStopped();
            }
            setCameraState(0);
        }
    }

    public boolean isCameraOpened() {
        return this.mIsCameraOpened;
    }

    private void closeCamera() {
        Log.i(TAG, "closeCamera");
        this.mIsCameraOpened = false;
        this.mUI.clearEvoPendingUI();
        if (this.mCameraDevice == null) {
            Log.d(TAG, "already stopped.");
            return;
        }
        this.mCameraDevice.setZoomChangeListener(null);
        this.mActivity.getCameraProvider().releaseCamera(this.mCameraDevice.getCameraId());
        this.mCameraDevice = null;
        setCameraState(0);
        this.mSnapshotInProgress = false;
        if (this.mFocusManager != null) {
            this.mFocusManager.onCameraReleased();
        }
    }

    public boolean onBackPressed() {
        if (this.mPaused) {
            return true;
        }
        if (!this.mMediaRecorderRecording) {
            return false;
        }
        onStopVideoRecording();
        return true;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (this.mPaused) {
            return true;
        }
        if (keyCode != 27) {
            if (keyCode != 82) {
                switch (keyCode) {
                    case 23:
                        break;
                    case 24:
                    case 25:
                        if (event.getRepeatCount() == 0 && !this.mActivity.getCameraAppUI().isInIntentReview() && this.mAppController.isShutterEnabled()) {
                            onShutterButtonClick();
                        }
                        return true;
                    default:
                        return false;
                }
            }
            return this.mMediaRecorderRecording;
        } else if (event.getRepeatCount() == 0) {
            onShutterButtonClick();
            return true;
        }
        if (event.getRepeatCount() == 0) {
            onShutterButtonClick();
            return true;
        }
        return this.mMediaRecorderRecording;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == 27) {
            onShutterButtonClick();
            return true;
        } else if (keyCode == 82) {
            return this.mMediaRecorderRecording;
        } else {
            switch (keyCode) {
                case 24:
                case 25:
                    return true;
                default:
                    return false;
            }
        }
    }

    public boolean isVideoCaptureIntent() {
        return "android.media.action.VIDEO_CAPTURE".equals(this.mActivity.getIntent().getAction());
    }

    /* Access modifiers changed, original: protected */
    public boolean isMaskSelected() {
        return (this.mActivity.getCameraAppUI().getCurrSelect() == null || this.mActivity.getCameraAppUI().getCurrSelect() == "") ? false : true;
    }

    private void doReturnToCaller(boolean valid) {
        int resultCode;
        Intent resultIntent = new Intent();
        if (valid) {
            saveVideoWhenNotNormal();
            resultCode = -1;
            resultIntent.setData(this.mCurrentVideoUri);
            resultIntent.addFlags(1);
        } else {
            resultCode = 0;
        }
        this.mActivity.setResultEx(resultCode, resultIntent);
        this.mActivity.finish();
    }

    private void saveVideoWhenNotNormal() {
        generateVideoFilename(this.mProfile.fileFormat);
        InputStream inputStream = null;
        try {
            inputStream = this.mContentResolver.openInputStream(this.mCurrentVideoUri);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
        if (inputStream != null) {
            this.mCurrentVideoFilename = this.mVideoFilename;
            try {
                FileOutputStream fos = new FileOutputStream(new File(this.mCurrentVideoFilename));
                byte[] buffer = new byte[2048];
                while (inputStream.read(buffer) != -1) {
                    fos.write(buffer);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
        saveVideoWithoutNotify();
    }

    private void saveVideoWithoutNotify() {
        long duration = SystemClock.uptimeMillis() - this.mRecordingStartTime;
        try {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            mmr.setDataSource(this.mCurrentVideoFilename);
            String durationStr = mmr.extractMetadata(9);
            mmr.release();
            duration = (long) Integer.parseInt(durationStr);
        } catch (Exception e) {
            Log.e(TAG, "MediaMetadataRetriever error, use estimated duration", e);
        }
        if (duration <= 0) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Video duration <= 0 : ");
            stringBuilder.append(duration);
            Log.w(tag, stringBuilder.toString());
        }
        this.mCurrentVideoValues.put("_size", Long.valueOf(new File(this.mCurrentVideoFilename).length()));
        this.mCurrentVideoValues.put(InfoTable.DURATION, Long.valueOf(duration));
        if (needAddToMediaSaver()) {
            getServices().getMediaSaver().addVideo(this.mCurrentVideoFilename, this.mCurrentVideoValues, getVideoSavedListener(), this.mContentResolver);
        }
        logVideoCapture(duration);
    }

    private void cleanupEmptyFile() {
        if (this.mVideoFilename != null) {
            File f = new File(this.mVideoFilename);
            if (f.length() == 0 && f.delete()) {
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Empty video file deleted: ");
                stringBuilder.append(this.mVideoFilename);
                Log.v(tag, stringBuilder.toString());
                this.mVideoFilename = null;
            }
        }
    }

    /* Access modifiers changed, original: protected */
    public int getMediaRecorderRotation() {
        return this.mMediaRemcoderRotation;
    }

    /* Access modifiers changed, original: protected */
    public boolean getTimeLapsedEnable() {
        return false;
    }

    private void initGlRecorder() {
        if (this.mCameraDevice != null) {
            this.mRecordingInterrupted = false;
            closeVideoFileDescriptor();
            this.mCurrentVideoUriFromMediaSaved = false;
            this.mMediaRecorder = new MediaRecorder();
            this.mMediaRecorder.setOnErrorListener(this);
            this.mMediaRecorder.setOnInfoListener(this);
            mediaRecorderParameterFetching(this.mMediaRecorder);
            setRecordLocation();
            if (this.mVideoFileDescriptor != null) {
                this.mMediaRecorder.setOutputFile(this.mVideoFileDescriptor.getFileDescriptor());
            } else {
                generateVideoFilename(this.mProfile.fileFormat);
                this.mMediaRecorder.setOutputFile(this.mVideoFilename);
            }
            long maxFileSize = this.mActivity.getStorageSpaceBytes() - Storage.LOW_STORAGE_THRESHOLD_BYTES;
            if (0 > 0 && 0 < maxFileSize) {
                maxFileSize = 0;
            }
            try {
                this.mMediaRecorder.setMaxFileSize(maxFileSize);
            } catch (RuntimeException e) {
            }
            int rotation = 0;
            if (this.mOrientation != -1) {
                Characteristics info = this.mActivity.getCameraProvider().getCharacteristics(this.mCameraId);
                if (isCameraFrontFacing()) {
                    rotation = ((info.getSensorOrientation() - this.mOrientation) + 360) % 360;
                } else if (isCameraBackFacing()) {
                    rotation = (info.getSensorOrientation() + this.mOrientation) % 360;
                } else {
                    Log.e(TAG, "Camera is facing unhandled direction");
                }
            }
            this.mMediaRemcoderRotation = rotation;
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("rotation is ");
            stringBuilder.append(rotation);
            Log.w(tag, stringBuilder.toString());
            this.mMediaRecorder.setOrientationHint(rotation);
            this.mActivity.getCameraAppUI().initVideopath(this.mVideoFilename);
            try {
                this.mMediaRecorder.prepare();
            } catch (IOException e2) {
                Tag tag2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("prepare failed for ");
                stringBuilder2.append(this.mVideoFilename);
                Log.e(tag2, stringBuilder2.toString(), e2);
                releaseMediaRecorder();
            }
        }
    }

    private void initializeRecorder() {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("initializeRecorder: ");
        stringBuilder.append(Thread.currentThread());
        Log.i(tag, stringBuilder.toString());
        if (this.mCameraDevice != null) {
            this.mRecordingInterrupted = false;
            Bundle myExtras = this.mActivity.getIntent().getExtras();
            long requestedSizeLimit = 0;
            closeVideoFileDescriptor();
            this.mCurrentVideoUriFromMediaSaved = false;
            if (this.mIsVideoCaptureIntent && myExtras != null) {
                Uri saveUri = (Uri) myExtras.getParcelable("output");
                if (saveUri != null) {
                    try {
                        this.mVideoFileDescriptor = this.mContentResolver.openFileDescriptor(saveUri, "rw");
                        this.mCurrentVideoUri = saveUri;
                    } catch (FileNotFoundException ex) {
                        Log.e(TAG, ex.toString());
                    }
                }
                requestedSizeLimit = myExtras.getLong("android.intent.extra.sizeLimit");
            }
            this.mMediaRecorder = new MediaRecorder();
            this.mMediaRecorder.setOnErrorListener(this);
            this.mMediaRecorder.setOnInfoListener(this);
            this.mCameraDevice.unlock();
            try {
                Tag tag2;
                StringBuilder stringBuilder2;
                this.mMediaRecorder.setCamera(this.mCameraDevice.getCamera());
                mediaRecorderParameterFetching(this.mMediaRecorder);
                setRecordLocation();
                if (getTimeLapsedEnable()) {
                    double fps = 1000.0d / ((double) (((int) ((1000.0f / ((float) this.mProfile.videoFrameRate)) + 0.5f)) * this.mTimeLapseMultiple));
                    tag2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("VideoModule.initializeRecorder mProfile.videoFrameRate:");
                    stringBuilder2.append(this.mProfile.videoFrameRate);
                    stringBuilder2.append(" fps:");
                    stringBuilder2.append(fps);
                    Log.d(tag2, stringBuilder2.toString());
                    setCaptureRate(this.mMediaRecorder, fps);
                }
                if (this.mVideoFileDescriptor != null) {
                    this.mMediaRecorder.setOutputFile(this.mVideoFileDescriptor.getFileDescriptor());
                } else {
                    generateVideoFilename(this.mProfile.fileFormat);
                    this.mMediaRecorder.setOutputFile(this.mVideoFilename);
                }
                long maxFileSize = this.mActivity.getStorageSpaceBytes() - Storage.LOW_STORAGE_THRESHOLD_BYTES;
                if (requestedSizeLimit > 0 && requestedSizeLimit < maxFileSize) {
                    maxFileSize = requestedSizeLimit;
                }
                try {
                    this.mMediaRecorder.setMaxFileSize(maxFileSize);
                } catch (RuntimeException e) {
                }
                int rotation = 0;
                if (this.mOrientation != -1) {
                    Characteristics info = this.mActivity.getCameraProvider().getCharacteristics(this.mCameraId);
                    if (isCameraFrontFacing()) {
                        rotation = ((info.getSensorOrientation() - this.mOrientation) + 360) % 360;
                    } else if (isCameraBackFacing()) {
                        rotation = (info.getSensorOrientation() + this.mOrientation) % 360;
                    } else {
                        Log.e(TAG, "Camera is facing unhandled direction");
                    }
                }
                this.mMediaRemcoderRotation = rotation;
                tag2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("rotation is ");
                stringBuilder2.append(rotation);
                Log.w(tag2, stringBuilder2.toString());
                this.mMediaRecorder.setOrientationHint(rotation);
                try {
                    long mediarecorderPrepareStart = System.currentTimeMillis();
                    this.mMediaRecorder.prepare();
                    Tag tag3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("mMediaRecorder.prepare() cost time : ");
                    stringBuilder3.append(System.currentTimeMillis() - mediarecorderPrepareStart);
                    Log.d(tag3, stringBuilder3.toString());
                } catch (IOException e2) {
                    Tag tag4 = TAG;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("prepare failed for ");
                    stringBuilder4.append(this.mVideoFilename);
                    Log.e(tag4, stringBuilder4.toString(), e2);
                    releaseMediaRecorder();
                }
            } catch (Exception e3) {
                Log.e(TAG, "MediaRecorder setCamera failed");
                e3.printStackTrace();
                releaseMediaRecorder();
            }
        }
    }

    /* Access modifiers changed, original: protected */
    public boolean hideCamera() {
        return false;
    }

    /* Access modifiers changed, original: protected */
    public void mediaRecorderParameterFetching(MediaRecorder recorder) {
        recorder.setAudioSource(5);
        recorder.setVideoSource(1);
        recorder.setProfile(this.mProfile);
        recorder.setVideoSize(this.mProfile.videoFrameWidth, this.mProfile.videoFrameHeight);
        recorder.setMaxDuration(getOverrodeVideoDuration());
    }

    /* Access modifiers changed, original: protected */
    public boolean isSupported(int width, int height) {
        return true;
    }

    /* Access modifiers changed, original: protected */
    public void setHsr(CameraSettings cameraSettings) {
        cameraSettings.setHsr(ExtendKey.FLIP_MODE_OFF);
    }

    /* Access modifiers changed, original: protected */
    public int getProfileQuality() {
        String videoQualityKey;
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        if (isCameraFrontFacing()) {
            videoQualityKey = Keys.KEY_VIDEO_QUALITY_FRONT;
        } else {
            videoQualityKey = Keys.KEY_VIDEO_QUALITY_BACK;
        }
        int videoQuality = settingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, videoQualityKey).intValue();
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Selected video quality for '");
        stringBuilder.append(videoQuality);
        Log.d(tag, stringBuilder.toString());
        return videoQuality;
    }

    private static void setCaptureRate(MediaRecorder recorder, double fps) {
        recorder.setCaptureRate(fps);
    }

    private void setRecordLocation() {
        Location loc = this.mLocationManager.getCurrentLocation();
        if (loc != null) {
            this.mMediaRecorder.setLocation((float) loc.getLatitude(), (float) loc.getLongitude());
        }
    }

    private void releaseMediaRecorder() {
        Log.i(TAG, "Releasing media recorder.");
        if (this.mMediaRecorder != null) {
            cleanupEmptyFile();
            this.mMediaRecorder.reset();
            this.mMediaRecorder.release();
            this.mMediaRecorder = null;
        }
        this.mVideoFilename = null;
    }

    /* Access modifiers changed, original: protected */
    public void generateVideoFilename(int outputFileFormat) {
        long dateTaken = System.currentTimeMillis();
        String title = createName(dateTaken);
        String filename = new StringBuilder();
        filename.append(title);
        filename.append(convertOutputFormatToFileExt(outputFileFormat));
        filename = filename.toString();
        String mime = convertOutputFormatToMimeType(outputFileFormat);
        String path = new StringBuilder();
        path.append(Storage.DIRECTORY);
        path.append('/');
        path.append(filename);
        path = path.toString();
        String tmpPath = path;
        if (!(this.mNeedGLRender || isSupportEffects())) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(path);
            stringBuilder.append(VIDEO_TEMP_SUFFIXES);
            tmpPath = stringBuilder.toString();
        }
        this.mCurrentVideoValues = new ContentValues(9);
        this.mCurrentVideoValues.put("title", title);
        this.mCurrentVideoValues.put("_display_name", filename);
        this.mCurrentVideoValues.put("datetaken", Long.valueOf(dateTaken));
        this.mCurrentVideoValues.put(InfoTable.DATE_MODIFIED, Long.valueOf(dateTaken / MIN_VIDEO_RECODER_DURATION));
        this.mCurrentVideoValues.put(InfoTable.MIME_TYPE, mime);
        this.mCurrentVideoValues.put("_data", path);
        this.mCurrentVideoValues.put(InfoTable.WIDTH, Integer.valueOf(this.mProfile.videoFrameWidth));
        this.mCurrentVideoValues.put(InfoTable.HEIGHT, Integer.valueOf(this.mProfile.videoFrameHeight));
        ContentValues contentValues = this.mCurrentVideoValues;
        String str = InfoTable.RESOLUTION;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(Integer.toString(this.mProfile.videoFrameWidth));
        stringBuilder2.append("x");
        stringBuilder2.append(Integer.toString(this.mProfile.videoFrameHeight));
        contentValues.put(str, stringBuilder2.toString());
        Location loc = this.mLocationManager.getCurrentLocation();
        if (loc != null) {
            this.mCurrentVideoValues.put(InfoTable.LATITUDE, Double.valueOf(loc.getLatitude()));
            this.mCurrentVideoValues.put(InfoTable.LONGITUDE, Double.valueOf(loc.getLongitude()));
        }
        this.mVideoFilename = tmpPath;
        Tag tag = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("New video filename: ");
        stringBuilder2.append(this.mVideoFilename);
        Log.v(tag, stringBuilder2.toString());
    }

    private void logVideoCapture(long duration) {
        String flashSetting = this.mActivity.getSettingsManager().getString(this.mAppController.getCameraScope(), Keys.KEY_VIDEOCAMERA_FLASH_MODE);
        boolean gridLinesOn = Keys.areGridLinesOn(this.mActivity.getSettingsManager());
        int width = ((Integer) this.mCurrentVideoValues.get(InfoTable.WIDTH)).intValue();
        int height = ((Integer) this.mCurrentVideoValues.get(InfoTable.HEIGHT)).intValue();
        long size = new File(this.mCurrentVideoFilename).length();
        UsageStatistics.instance().videoCaptureDoneEvent(new File(this.mCurrentVideoValues.getAsString("_data")).getName(), duration, isCameraFrontFacing(), currentZoomValue(), width, height, size, flashSetting, gridLinesOn);
    }

    /* Access modifiers changed, original: protected */
    public void saveVideo() {
        if (this.mVideoFileDescriptor == null) {
            long duration = SystemClock.uptimeMillis() - this.mRecordingStartTime;
            try {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(this.mCurrentVideoFilename);
                String durationStr = mmr.extractMetadata(9);
                mmr.release();
                duration = (long) Integer.parseInt(durationStr);
            } catch (Exception e) {
                Log.e(TAG, "MediaMetadataRetriever error, use estimated duration", e);
            }
            if (duration <= 0) {
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Video duration <= 0 : ");
                stringBuilder.append(duration);
                Log.w(tag, stringBuilder.toString());
            }
            if (this.mCurrentVideoValues == null) {
                generateVideoFilename(this.mProfile.fileFormat);
            }
            if (this.mCurrentVideoValues != null) {
                this.mCurrentVideoValues.put("_size", Long.valueOf(new File(this.mCurrentVideoFilename).length()));
                this.mCurrentVideoValues.put(InfoTable.DURATION, Long.valueOf(duration));
                if (needAddToMediaSaver()) {
                    getServices().getMediaSaver().addVideo(this.mCurrentVideoFilename, this.mCurrentVideoValues, getVideoSavedListener(), this.mContentResolver);
                }
                logVideoCapture(duration);
            } else {
                return;
            }
        }
        this.mCurrentVideoValues = null;
    }

    /* Access modifiers changed, original: protected */
    public boolean needAddToMediaSaver() {
        return true;
    }

    private void deleteVideoFile(String fileName) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Deleting video ");
        stringBuilder.append(fileName);
        Log.v(tag, stringBuilder.toString());
        if (!new File(fileName).delete()) {
            Tag tag2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Could not delete ");
            stringBuilder2.append(fileName);
            Log.v(tag2, stringBuilder2.toString());
        }
    }

    public void onError(MediaRecorder mr, int what, int extra) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("MediaRecorder error. what=");
        stringBuilder.append(what);
        stringBuilder.append(". extra=");
        stringBuilder.append(extra);
        Log.e(tag, stringBuilder.toString());
        if (what == 1) {
            stopVideoRecording();
            this.mActivity.updateStorageSpaceAndHint(null);
        }
    }

    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (what == 800) {
            if (this.mMediaRecorderRecording) {
                this.mHandler.sendEmptyMessage(10);
            }
            Toast toast = Toast.makeText(this.mActivity, this.mActivity.getString(R.string.video_reach_duration_limit), 1);
            toast.setGravity(1, 0, (int) this.mActivity.getResources().getDimension(R.dimen.toast_margin_Bottom));
            toast.show();
        } else if (what == MediaPlayer2.MEDIA_INFO_NOT_SEEKABLE) {
            if (this.mMediaRecorderRecording) {
                this.mHandler.sendEmptyMessage(10);
            }
            ToastUtil.showToast(this.mActivity, this.mActivity.getString(R.string.video_reach_size_limit), 1);
        }
    }

    private void pauseAudioPlayback() {
        Intent i = new Intent("com.android.music.musicservicecommand");
        i.putExtra(FyuseAPI.COMMAND, "pause");
        this.mActivity.sendBroadcast(i);
        ((AudioManager) this.mActivity.getSystemService("audio")).requestAudioFocus(this.mAudioFocusChangeListener, 3, 2);
    }

    private void releaseAudioFocus() {
        ((AudioManager) this.mActivity.getSystemService("audio")).abandonAudioFocus(this.mAudioFocusChangeListener);
    }

    /* Access modifiers changed, original: protected */
    public void stopVideoWhileAudioFocusLoss() {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("test->stopVideoWhileAudioFocusLoss PhoneFlag:");
        stringBuilder.append(this.PhoneFlag);
        Log.d(tag, stringBuilder.toString());
    }

    public boolean isRecording() {
        return this.mMediaRecorderRecording;
    }

    /* Access modifiers changed, original: protected */
    public void tryLockFocus() {
    }

    /* Access modifiers changed, original: protected */
    public void Focus() {
    }

    /* Access modifiers changed, original: protected */
    public void startVideoRecording() {
        this.mActivity.getCameraAppUI().switchShutterSlidingAbility(false);
        if (this.mCameraState == 1) {
            setCameraState(2);
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("startVideoRecording: ");
            stringBuilder.append(Thread.currentThread());
            Log.i(tag, stringBuilder.toString());
            ToastUtil.showToast(this.mActivity, this.mActivity.getString(R.string.video_recording_start_toast), 0);
            this.mAppController.onVideoRecordingStarted();
            if (this.mModeSelectionLockToken == null) {
                this.mModeSelectionLockToken = this.mAppController.lockModuleSelection();
            }
            this.mUI.showVideoRecordingHints(false);
            this.mUI.cancelAnimations();
            this.mUI.setSwipingEnabled(false);
            this.mUI.showFocusUI(false);
            this.mAppController.getCameraAppUI().hideRotateButton();
            this.mAppController.getButtonManager().hideEffectsContainerWrapper();
            final long updateStorageSpaceTime = System.currentTimeMillis();
            this.mActivity.updateStorageSpaceAndHint(new OnStorageUpdateDoneListener() {
                public void onStorageUpdateDone(long bytes) {
                    Tag access$100 = VideoModule.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("updateStorageSpaceAndHint cost time :");
                    stringBuilder.append(System.currentTimeMillis() - updateStorageSpaceTime);
                    stringBuilder.append("ms.");
                    Log.d(access$100, stringBuilder.toString());
                    if (VideoModule.this.mCameraState != 2) {
                        VideoModule.this.pendingRecordFailed();
                        return;
                    }
                    if (bytes <= Storage.LOW_STORAGE_THRESHOLD_BYTES) {
                        Log.w(VideoModule.TAG, "Storage issue, ignore the start request");
                        VideoModule.this.pendingRecordFailed();
                    } else if (VideoModule.this.mCameraDevice == null) {
                        Log.v(VideoModule.TAG, "in storage callback after camera closed");
                        VideoModule.this.pendingRecordFailed();
                    } else if (VideoModule.this.mPaused) {
                        Log.v(VideoModule.TAG, "in storage callback after module paused");
                        VideoModule.this.pendingRecordFailed();
                    } else if (VideoModule.this.mMediaRecorderRecording) {
                        Log.v(VideoModule.TAG, "in storage callback after recording started");
                    } else if (VideoModule.this.isSupported(VideoModule.this.mProfile.videoFrameWidth, VideoModule.this.mProfile.videoFrameHeight)) {
                        VideoModule.this.mCurrentVideoUri = null;
                        VideoModule.this.mCameraDevice.enableShutterSound(false);
                        if (VideoModule.this.mNeedGLRender && VideoModule.this.isSupportEffects()) {
                            VideoModule.this.playVideoSound();
                            VideoModule.this.initGlRecorder();
                            VideoModule.this.pauseAudioPlayback();
                            VideoModule.this.mActivity.getCameraAppUI().startVideoRecorder();
                        } else {
                            VideoModule.this.initializeRecorder();
                            if (VideoModule.this.mMediaRecorder == null) {
                                Log.e(VideoModule.TAG, "Fail to initialize media recorder");
                                VideoModule.this.pendingRecordFailed();
                                return;
                            }
                            VideoModule.this.pauseAudioPlayback();
                            try {
                                long mediarecorderStart = System.currentTimeMillis();
                                VideoModule.this.mMediaRecorder.start();
                                access$100 = VideoModule.TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("mMediaRecorder.start() cost time : ");
                                stringBuilder2.append(System.currentTimeMillis() - mediarecorderStart);
                                Log.d(access$100, stringBuilder2.toString());
                                VideoModule.this.playVideoSound();
                                VideoModule.this.mCameraDevice.refreshSettings();
                                VideoModule.this.mCameraSettings = VideoModule.this.mCameraDevice.getSettings();
                                VideoModule.this.setFocusParameters();
                            } catch (RuntimeException e) {
                                Log.e(VideoModule.TAG, "Could not start media recorder. ", e);
                                VideoModule.this.releaseMediaRecorder();
                                VideoModule.this.mCameraDevice.lock();
                                VideoModule.this.releaseAudioFocus();
                                if (VideoModule.this.mModeSelectionLockToken != null) {
                                    VideoModule.this.mAppController.unlockModuleSelection(VideoModule.this.mModeSelectionLockToken);
                                }
                                VideoModule.this.setCameraState(1);
                                if (VideoModule.this.shouldHoldRecorderForSecond()) {
                                    VideoModule.this.mAppController.setShutterEnabled(true);
                                }
                                VideoModule.this.mAppController.getCameraAppUI().showModeOptions();
                                if (VideoModule.this.updateModeSwitchUIinModule()) {
                                    VideoModule.this.mAppController.getCameraAppUI().setModeSwitchUIVisibility(true);
                                }
                                if (VideoModule.this.isNeedStartRecordingOnSwitching()) {
                                    VideoModule.this.mAppController.onVideoRecordingStop();
                                }
                                ToastUtil.showToast(VideoModule.this.mActivity.getApplicationContext(), (int) R.string.video_record_start_failed, 0);
                                return;
                            }
                        }
                        VideoModule.this.mAppController.getCameraAppUI().setSwipeEnabled(false);
                        VideoModule.this.setCameraState(3);
                        VideoModule.this.tryLockFocus();
                        VideoModule.this.mMediaRecorderRecording = true;
                        VideoModule.this.mActivity.lockOrientation();
                        VideoModule.this.mRecordingStartTime = SystemClock.uptimeMillis() + 600;
                        VideoModule.this.mAppController.getCameraAppUI().getCameraGLSurfaceView().setAngle(VideoModule.this.mOrientation);
                        VideoModule.this.mAppController.getCameraAppUI().hideModeOptions();
                        if (VideoModule.this.updateModeSwitchUIinModule()) {
                            VideoModule.this.mAppController.getCameraAppUI().setModeSwitchUIVisibility(false);
                        }
                        if (VideoModule.this.isVideoShutterAnimationEnssential()) {
                            VideoModule.this.mAppController.getCameraAppUI().animateBottomBarToVideoStop(R.drawable.ic_stop);
                        }
                        if (VideoModule.this.isNeedStartRecordingOnSwitching()) {
                            VideoModule.this.mAppController.getCameraAppUI().showVideoCaptureButton(true);
                        }
                        if (VideoModule.this.mAppController.getCameraAppUI().getCurrentModeIndex() == VideoModule.this.mActivity.getResources().getInteger(R.integer.camera_mode_video)) {
                            VideoModule.this.mAppController.getCameraAppUI().showVideoPauseButton(false);
                        }
                        VideoModule.this.mUI.showRecordingUI(true);
                        if (VideoModule.this.mAppController.getCameraAppUI().getCameraGLSurfaceView().getVisibility() == 0) {
                            VideoModule.this.mUI.hideCapButton();
                        }
                        VideoModule.this.showBoomKeyTip();
                        VideoModule.this.updateRecordingTime();
                        access$100 = VideoModule.TAG;
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("startVideoRecording cost time 1 : ");
                        stringBuilder3.append(System.currentTimeMillis() - VideoModule.this.mShutterButtonClickTime);
                        Log.d(access$100, stringBuilder3.toString());
                        if (VideoModule.this.isSendMsgEnableShutterButton()) {
                            VideoModule.this.mHandler.sendEmptyMessageDelayed(6, VideoModule.MIN_VIDEO_RECODER_DURATION);
                        }
                        VideoModule.this.mActivity.enableKeepScreenOn(true);
                        VideoModule.this.mActivity.startInnerStorageChecking(new OnInnerStorageLowListener() {
                            public void onInnerStorageLow(long bytes) {
                                VideoModule.this.mActivity.stopInnerStorageChecking();
                                if (VideoModule.this.mCameraState == 3) {
                                    VideoModule.this.showQuitDialog(R.string.quit_dialog_title_storage_low, R.string.quit_dialog_msg, VideoModule.this.saveAndQuit);
                                }
                            }
                        });
                        VideoModule.this.mActivity.startBatteryInfoChecking(new OnBatteryLowListener() {
                            public void onBatteryLow(int level) {
                                VideoModule.this.mActivity.stopBatteryInfoChecking();
                                if (VideoModule.this.mCameraState == 3) {
                                    VideoModule.this.showQuitDialog(R.string.quit_dialog_title_battery_low, R.string.quit_dialog_msg, VideoModule.this.saveAndQuit);
                                }
                            }
                        });
                    } else {
                        Log.e(VideoModule.TAG, "Unsupported parameters");
                        VideoModule.this.pendingRecordFailed();
                    }
                }
            });
        }
    }

    /* Access modifiers changed, original: protected */
    public void showBoomKeyTip() {
    }

    /* Access modifiers changed, original: protected */
    public void hideBoomKeyTip() {
    }

    /* Access modifiers changed, original: protected */
    public void pendingRecordFailed() {
        pendingRecordFailed(0);
    }

    /* Access modifiers changed, original: protected */
    public void pendingRecordFailed(int toastId) {
        if (this.mModeSelectionLockToken != null) {
            this.mAppController.unlockModuleSelection(this.mModeSelectionLockToken);
        }
        if (this.mCameraDevice != null) {
            this.mCameraDevice.lock();
        }
        this.mVideoFilename = null;
        setCameraState(1);
        if (shouldHoldRecorderForSecond()) {
            this.mAppController.setShutterEnabled(true);
        }
        this.mAppController.getCameraAppUI().showModeOptions();
        if (updateModeSwitchUIinModule() && getModuleId() != this.mActivity.getResources().getInteger(R.integer.camera_mode_time_lapse)) {
            this.mAppController.getCameraAppUI().setModeSwitchUIVisibility(true);
        }
        if (isNeedStartRecordingOnSwitching()) {
            this.mAppController.onVideoRecordingStop();
        }
        if (toastId != 0) {
            ToastUtil.showToast(this.mActivity.getApplicationContext(), toastId, 0);
        } else {
            ToastUtil.showToast(this.mActivity.getApplicationContext(), (int) R.string.video_record_start_failed, 0);
        }
    }

    /* Access modifiers changed, original: protected */
    public boolean isVideoShutterAnimationEnssential() {
        return true;
    }

    private Bitmap getVideoThumbnail() {
        Bitmap bitmap = null;
        if (this.mVideoFileDescriptor != null) {
            bitmap = Thumbnail.createVideoThumbnailBitmap(this.mVideoFileDescriptor.getFileDescriptor(), this.mDesiredPreviewWidth);
        } else if (this.mCurrentVideoUri != null) {
            try {
                this.mVideoFileDescriptor = this.mContentResolver.openFileDescriptor(this.mCurrentVideoUri, "r");
                bitmap = Thumbnail.createVideoThumbnailBitmap(this.mVideoFileDescriptor.getFileDescriptor(), this.mDesiredPreviewWidth);
            } catch (FileNotFoundException ex) {
                Log.e(TAG, ex.toString());
            }
        }
        if (bitmap != null) {
            return CameraUtil.rotateAndMirror(bitmap, 0, isCameraFrontFacing());
        }
        return bitmap;
    }

    private void showCaptureResult() {
        this.mIsInReviewMode = true;
        Bitmap bitmap = getVideoThumbnail();
        if (bitmap != null) {
            this.mUI.showReviewImage(bitmap);
        }
        this.mUI.showReviewControls();
    }

    private boolean stopVideoRecording() {
        if (getModuleId() != this.mActivity.getResources().getInteger(R.integer.camera_mode_time_lapse)) {
            this.mActivity.getCameraAppUI().switchShutterSlidingAbility(true);
        }
        Log.v(TAG, "stopVideoRecording");
        BottomBarSizeListener bottomBarSizeListener = null;
        if (this.mFocusManager != null) {
            this.mFocusManager.overrideFocusMode(null);
        }
        this.mLastVideoDuration = -1;
        long endVideo = SystemClock.uptimeMillis();
        this.mUI.setSwipingEnabled(true);
        this.mUI.showFocusUI(true);
        this.mUI.showVideoRecordingHints(true);
        boolean fail = false;
        if (this.mCameraState == 3) {
            boolean shouldAddToMediaStoreNow = false;
            if (this.mNeedGLRender && isSupportEffects()) {
                this.mHandler.postDelayed(new Runnable() {
                    public void run() {
                        VideoModule.this.playVideoSound();
                    }
                }, MIN_VIDEO_RECODER_DURATION);
                this.mCurrentVideoFilename = this.mVideoFilename;
                shouldAddToMediaStoreNow = true;
                fail = false;
            } else {
                try {
                    this.mMediaRecorder.setOnErrorListener(null);
                    this.mMediaRecorder.setOnInfoListener(null);
                    long mediarecorderStopTime = System.currentTimeMillis();
                    this.mMediaRecorder.stop();
                    Tag tag = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("mMediaRecorder.stop() cost time :");
                    stringBuilder.append(System.currentTimeMillis() - mediarecorderStopTime);
                    stringBuilder.append(" ms.");
                    Log.d(tag, stringBuilder.toString());
                    playVideoSound();
                    shouldAddToMediaStoreNow = this.mRecordingInterrupted ^ 1;
                    this.mCurrentVideoFilename = this.mVideoFilename;
                    tag = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("stopVideoRecording: current video filename: ");
                    stringBuilder.append(this.mCurrentVideoFilename);
                    Log.v(tag, stringBuilder.toString());
                } catch (RuntimeException e) {
                    Log.e(TAG, "stop fail", e);
                    if (this.mVideoFilename != null) {
                        deleteVideoFile(this.mVideoFilename);
                    }
                    fail = true;
                    ToastUtil.showToast(this.mActivity, this.mActivity.getString(R.string.video_recording_fail_toast), 1);
                }
            }
            setCameraState(4);
            this.mMediaRecorderRecording = false;
            this.mActivity.unlockOrientation();
            if (this.mPaused) {
                stopPreview();
                closeCamera();
            }
            this.mUI.showRecordingUI(false);
            hideBoomKeyTip();
            this.mUI.setOrientationIndicator(0, true);
            this.mActivity.enableKeepScreenOn(false);
            if (shouldAddToMediaStoreNow && !fail) {
                if (this.mVideoFileDescriptor == null) {
                    if (this.mNeedGLRender && isSupportEffects()) {
                        this.mLastVideoDuration = endVideo - this.mRecordingStartTime;
                        new Handler().postDelayed(new Runnable() {
                            public void run() {
                                VideoModule.this.saveVideo();
                            }
                        }, 1500);
                    } else {
                        saveVideo();
                    }
                } else if (this.mIsVideoCaptureIntent) {
                    showCaptureResult();
                }
            }
        }
        releaseMediaRecorder();
        if (!(this.mPaused || this.mCameraDevice == null || this.mNeedGLRender)) {
            this.mCameraDevice.lock();
            if (!ApiHelper.HAS_SURFACE_TEXTURE_RECORDING) {
                long stopVideoRecordingRestartpreviewTime = System.currentTimeMillis();
                stopPreview();
                startPreview();
                Tag tag2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("stopVideoRecording Restartpreview cost time : ");
                stringBuilder2.append(System.currentTimeMillis() - stopVideoRecordingRestartpreviewTime);
                Log.d(tag2, stringBuilder2.toString());
            }
            this.mCameraSettings = this.mCameraDevice.getSettings();
        }
        this.mActivity.updateStorageSpaceAndHint(null);
        final Runnable resetRunnable = new Runnable() {
            public void run() {
                if (VideoModule.this.mModeSelectionLockToken != null) {
                    VideoModule.this.mAppController.unlockModuleSelection(VideoModule.this.mModeSelectionLockToken);
                }
                if (VideoModule.this.mCameraState == 4) {
                    VideoModule.this.setCameraState(1);
                    if (!VideoModule.this.mNeedGLRender) {
                        VideoModule.this.cancelAutoFocus();
                    }
                }
                if (!VideoModule.this.isVideoCaptureIntent()) {
                    VideoModule.this.mAppController.getCameraAppUI().showModeOptions();
                }
                if (VideoModule.this.updateModeSwitchUIinModule() && !VideoModule.this.isTL) {
                    VideoModule.this.mAppController.getCameraAppUI().setModeSwitchUIVisibility(true);
                }
            }
        };
        if (isVideoShutterAnimationEnssential()) {
            BottomBarSizeListener animateDoneListener = new BottomBarSizeListener() {
                public void onFullSizeReached() {
                    resetRunnable.run();
                    if (!VideoModule.this.isNeedStartRecordingOnSwitching() && VideoModule.this.isNeedStartRecordingOnSwitching()) {
                        VideoModule.this.mAppController.onVideoRecordingStop();
                    }
                }
            };
            if (this.mPaused) {
                resetRunnable.run();
            }
            CameraAppUI cameraAppUI = this.mAppController.getCameraAppUI();
            int i = this.mShutterIconId;
            if (!this.mPaused) {
                bottomBarSizeListener = animateDoneListener;
            }
            cameraAppUI.animateBottomBarToFullSize(i, bottomBarSizeListener, this.isTL);
        } else {
            resetRunnable.run();
        }
        this.mUI.clearEvoPendingUI();
        this.mAppController.getCameraAppUI().hideVideoCaptureButton(false);
        this.mAppController.getCameraAppUI().hideVideoPauseButton(false);
        this.mAppController.getCameraAppUI().onVideoRecordingStateChanged(false);
        if (this.mNeedGLRender && isSupportEffects()) {
            this.mAppController.getButtonManager().showEffectsContainerWrapper();
        }
        if (!(this.mPaused || this.mCameraDevice == null || this.mNeedGLRender)) {
            setFocusParameters();
        }
        ToastUtil.showToast(this.mActivity, this.mActivity.getString(R.string.video_recording_stop_toast), 0);
        Tag tag3 = TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("stopVideoRecording cost time : ");
        stringBuilder3.append(System.currentTimeMillis() - this.mShutterButtonClickTime);
        Log.d(tag3, stringBuilder3.toString());
        return fail;
    }

    private static String millisecondToTimeString(long milliSeconds, boolean displayCentiSeconds) {
        long seconds = milliSeconds / MIN_VIDEO_RECODER_DURATION;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long remainderMinutes = minutes - (hours * 60);
        long remainderSeconds = seconds - (60 * minutes);
        StringBuilder timeStringBuilder = new StringBuilder();
        if (hours > 0) {
            if (hours < 10) {
                timeStringBuilder.append('0');
            }
            timeStringBuilder.append(hours);
            timeStringBuilder.append(':');
        }
        if (remainderMinutes < 10) {
            timeStringBuilder.append('0');
        }
        timeStringBuilder.append(remainderMinutes);
        timeStringBuilder.append(':');
        if (remainderSeconds < 10) {
            timeStringBuilder.append('0');
        }
        timeStringBuilder.append(remainderSeconds);
        if (displayCentiSeconds) {
            timeStringBuilder.append('.');
            long remainderCentiSeconds = (milliSeconds - (MIN_VIDEO_RECODER_DURATION * seconds)) / 10;
            if (remainderCentiSeconds < 10) {
                timeStringBuilder.append('0');
            }
            timeStringBuilder.append(remainderCentiSeconds);
        }
        return timeStringBuilder.toString();
    }

    private void updateRecordingTime() {
        if (this.mMediaRecorderRecording) {
            long deltaAdjusted;
            long targetNextUpdateDelay;
            long delta = SystemClock.uptimeMillis() - this.mRecordingStartTime;
            if (this.mMediaRecoderRecordingPaused) {
                delta = this.mVideoRecordedDuration;
            }
            boolean countdownRemainingTime = this.mMaxVideoDurationInMs != 0 && delta >= ((long) (this.mMaxVideoDurationInMs - 60000));
            if (getTimeLapsedEnable()) {
                deltaAdjusted = delta / ((long) this.mTimeLapseMultiple);
            } else {
                deltaAdjusted = delta;
            }
            if (countdownRemainingTime) {
                deltaAdjusted = Math.max(0, ((long) this.mMaxVideoDurationInMs) - deltaAdjusted) + 999;
            }
            String text = millisecondToTimeString(deltaAdjusted, false);
            if (getTimeLapsedEnable()) {
                targetNextUpdateDelay = (long) (this.mTimeLapseMultiple * 1000);
            } else {
                targetNextUpdateDelay = MIN_VIDEO_RECODER_DURATION;
            }
            this.mUI.setRecordingTime(text);
            if (this.mRecordingTimeCountsDown != countdownRemainingTime) {
                this.mRecordingTimeCountsDown = countdownRemainingTime;
                this.mUI.setRecordingTimeTextColor(this.mActivity.getResources().getColor(R.color.recording_time_remaining_text));
            }
            this.mHandler.sendEmptyMessageDelayed(5, targetNextUpdateDelay - (delta % targetNextUpdateDelay));
            onVideoRecordingStarted();
        }
    }

    /* Access modifiers changed, original: protected */
    public void onVideoRecordingStarted() {
        this.mUI.unlockCaptureView();
    }

    private static boolean isSupported(String value, List<String> supported) {
        return supported != null && supported.indexOf(value) >= 0;
    }

    private void setCameraParameters() {
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        updateDesiredPreviewSize();
        this.mCameraSettings.setSizesLocked(false);
        this.mCameraSettings.setPreviewSize(new Size(this.mDesiredPreviewWidth, this.mDesiredPreviewHeight));
        this.mCameraSettings.setSizesLocked(true);
        this.mCameraSettings.setVideoSize(new Size(this.mProfile.videoFrameWidth, this.mProfile.videoFrameHeight));
        int[] fpsRange = CameraUtil.getPhotoPreviewFpsRange(this.mCameraCapabilities);
        if (fpsRange == null || fpsRange.length <= 0) {
            this.mCameraSettings.setPreviewFrameRate(this.mProfile.videoFrameRate);
        } else {
            this.mCameraSettings.setPreviewFpsRange(fpsRange[0], fpsRange[1]);
        }
        if (this.mCameraCapabilities.supports(Feature.ZOOM)) {
            this.mCameraSettings.setZoomRatio(this.mZoomValue);
        }
        updateFocusParameters();
        this.mCameraSettings.setRecordingHintEnabled(true);
        if (this.mCameraCapabilities.supports(Feature.VIDEO_STABILIZATION)) {
            this.mCameraSettings.setVideoStabilization(isVideoStabilizationEnabled());
        }
        Size optimalSize = CameraUtil.getOptimalVideoSnapshotPictureSize(this.mCameraCapabilities.getSupportedPhotoSizes(), this.mDesiredPreviewWidth, this.mDesiredPreviewHeight);
        if (!new Size(this.mCameraSettings.getCurrentPhotoSize()).equals(optimalSize)) {
            this.mCameraSettings.setPhotoSize(optimalSize);
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Video snapshot size is ");
        stringBuilder.append(optimalSize);
        Log.d(tag, stringBuilder.toString());
        this.mCameraSettings.setPhotoJpegCompressionQuality(CameraProfile.getJpegEncodingQualityParameter(this.mCameraId, 2));
        setHsr(this.mCameraSettings);
        updateParametersAntibanding();
        updateFacebeauty();
        enableTorchMode(true);
        if (this.mCameraDevice != null) {
            this.mCameraDevice.applySettings(this.mCameraSettings);
        }
        this.mUI.updateOnScreenIndicators(this.mCameraSettings);
    }

    /* Access modifiers changed, original: protected */
    public boolean isFacebeautyEnabled() {
        return false;
    }

    private void updateFacebeauty() {
        if (isFacebeautyEnabled()) {
            SettingsManager settingsManager = this.mActivity.getSettingsManager();
            if (isCameraFrontFacing()) {
                this.mCameraSettings.setFaceBeauty(Keys.isFacebeautyOn(settingsManager), (this.mActivity.getSettingsManager().getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_FACEBEAUTY_SKIN_SMOOTHING, Integer.valueOf(CustomUtil.getInstance().getInt(CustomFields.DEF_CAMERA_SKIN_SMOOTHING, 50))).intValue() * 90) / 100);
            }
        }
    }

    /* Access modifiers changed, original: protected */
    public boolean isVideoStabilizationEnabled() {
        return Keys.isVideoStabilizationEnabled(this.mAppController.getSettingsManager());
    }

    private void updateParametersAntibanding() {
        this.mCameraSettings.setAntibanding(Keys.getAntibandingValue(this.mActivity.getSettingsManager()));
    }

    private void updateFocusParameters() {
        if (this.mCameraCapabilities.supports(FocusMode.CONTINUOUS_PICTURE) || this.mCameraCapabilities.supports(FocusMode.FIXED)) {
            this.mCameraSettings.setFocusMode(this.mFocusManager.getFocusMode(this.mCameraSettings.getCurrentFocusMode()));
            if (this.mFocusAreaSupported || this.mMeteringAreaSupported) {
                this.mCameraSettings.setFocusAreas(this.mFocusManager.getFocusAreas());
                this.mCameraSettings.setMeteringAreas(this.mFocusManager.getMeteringAreas());
            }
        }
        updateAutoFocusMoveCallback();
    }

    /* Access modifiers changed, original: protected */
    public void disableShutterDuringResume() {
        this.mAppController.setShutterEnabledWithNormalAppearence(false);
    }

    private void setSizeToSurface(Size size) {
        if (size != null) {
            this.mActivity.getCameraAppUI().setSurfaceHeight(size.height());
            this.mActivity.getCameraAppUI().setSurfaceWidth(size.width());
            return;
        }
        this.mActivity.getCameraAppUI().setSurfaceHeight(1080);
        this.mActivity.getCameraAppUI().setSurfaceWidth(1920);
    }

    public void resume() {
        Log.w(TAG, "KPI video resume");
        if (isVideoCaptureIntent()) {
            this.mDontResetIntentUiOnResume = this.mPaused;
        }
        this.mTelephonyManager = (TelephonyManager) this.mActivity.getSystemService("phone");
        if (this.mTelephonyManager != null) {
            Log.d(TAG, "test->resume mTelephonyManager.listen --->>LISTEN_CALL_STATE");
            this.mTelephonyManager.listen(this.mPhoneStateListener, 32);
        }
        this.mPaused = false;
        if (this.mActivity != null) {
            if (this.mActivity.islaunchFromAssistant()) {
                this.mCameraId = this.mActivity.isUserFrontCamera();
            } else {
                this.mCameraId = this.mActivity.getSettingsManager().getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID).intValue();
            }
            Log.d(TAG, "[VideoMoudle] resume onLastMediaDataUpdated");
            this.mActivity.onLastMediaDataUpdated();
        }
        this.mSoundPlayer.loadSound(R.raw.video_record);
        disableShutterDuringResume();
        this.mZoomValue = 1.0f;
        this.mUI.resetZoombar();
        showVideoSnapshotUI(false);
        this.mAppController.getCameraAppUI().hideVideoPauseButton(false);
        this.mAppController.getCameraAppUI().hideCaptureButton();
        this.mActivity.getCameraAppUI().pauseFaceDetection();
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mCameraState is ");
        stringBuilder.append(this.mCameraState);
        Log.v(tag, stringBuilder.toString());
        this.mNeedGLRender = onGLRenderEnable();
        if (isSupportEffects()) {
            this.mActivity.getCameraAppUI().setTextureViewVisible(0);
            this.mActivity.getCameraAppUI().getCameraGLSurfaceView().setVisibility(8);
        } else {
            this.mActivity.getCameraAppUI().setTextureViewVisible(0);
            this.mActivity.getCameraAppUI().getCameraGLSurfaceView().setVisibility(8);
        }
        if (!this.mActivity.getCameraProvider().isCameraRequestBoosted()) {
            requestCameraOpen();
        }
        if (this.mFocusManager != null) {
            this.mAppController.addPreviewAreaSizeChangedListener(this.mFocusManager);
        }
        this.mActivity.enableKeepScreenOn(true);
        if (this.mCameraState != 0) {
            this.mOnResumeTime = SystemClock.uptimeMillis();
            this.mHandler.sendEmptyMessageDelayed(4, 100);
        }
        getServices().getMemoryManager().addListener(this);
        if (this.mNeedGLRender && isSupportEffects()) {
            this.mActivity.getButtonManager().updateMOBExtraWithHeight(ButtonManager.MASK_TOTAL_HEIGHT);
            this.mActivity.getButtonManager().showEffectsContainerWrapper();
            this.mActivity.getButtonManager().showEffectLayout();
        } else {
            this.mActivity.getButtonManager().hideEffectsContainerWrapper();
        }
        this.mActivity.getCameraAppUI().hideImageCover();
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

    public boolean isPaused() {
        return this.mPaused;
    }

    public void pause() {
        this.mPaused = true;
        Log.w(TAG, "KPI video pause E");
        this.mAppController.getLockEventListener().onIdle();
        if (this.mFocusManager != null) {
            this.mAppController.removePreviewAreaSizeChangedListener(this.mFocusManager);
            this.mFocusManager.removeMessages();
        }
        if (this.mMediaRecorderRecording) {
            onStopVideoRecording();
        } else {
            stopPreview();
            closeCamera();
            releaseMediaRecorder();
        }
        if (this.quitDialog != null && this.quitDialog.isShowing()) {
            this.quitDialog.dismiss();
        }
        closeVideoFileDescriptor();
        if (this.mReceiver != null) {
            this.mActivity.unregisterReceiver(this.mReceiver);
            this.mReceiver = null;
        }
        this.mHandler.removeMessages(4);
        this.mHandler.removeMessages(8);
        this.mHandler.removeMessages(9);
        this.mPendingSwitchCameraId = -1;
        this.mSwitchingCamera = false;
        this.mPreferenceRead = false;
        getServices().getMemoryManager().removeListener(this);
        this.mUI.onPause();
        this.mSoundPlayer.unloadSound(R.raw.video_record);
        hideBoomKeyTip();
        this.mActivity.getSettingsManager().removeListener(this);
        ToastUtil.cancelToast();
        if (this.mTelephonyManager != null) {
            Log.d(TAG, "test->pause mTelephonyManager.listen --->>LISTEN_NONE");
            this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        }
    }

    public void destroy() {
        this.mSoundPlayer.release();
    }

    public boolean isSupportBeauty() {
        return false;
    }

    public void onLayoutOrientationChanged(boolean isLandscape) {
        setDisplayOrientation();
    }

    public void onSharedPreferenceChanged() {
    }

    private void switchCamera() {
        CameraAppUI cameraAppUI = this.mActivity.getCameraAppUI();
        if (!cameraAppUI.mIsCameraSwitchAnimationRunning) {
            int holdTime = 300;
            if (cameraAppUI.getCameraGLSurfaceView().getVisibility() == 0) {
                holdTime = MediaPlayer2.MEDIA_INFO_TIMED_TEXT_ERROR;
            }
            cameraAppUI.playCameraSwitchAnimation(400, holdTime);
            if (null > null) {
                cameraAppUI.getCameraGLSurfaceView().postDelayed(new Runnable() {
                    public void run() {
                        VideoModule.this.switchCameraDefault();
                    }
                }, (long) null);
            } else {
                switchCameraDefault();
            }
        }
    }

    private void switchCameraDefault() {
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        Log.d(TAG, "Start to switch camera.");
        this.mCameraId = this.mPendingSwitchCameraId;
        this.mPendingSwitchCameraId = -1;
        settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID, this.mCameraId);
        closeCamera();
        if (this.mFocusManager != null) {
            this.mFocusManager.removeMessages();
        }
        requestCameraOpen();
        this.mMirror = isCameraFrontFacing();
        if (this.mFocusManager != null) {
            this.mFocusManager.setMirror(this.mMirror);
        }
        this.mZoomValue = 1.0f;
        this.mUI.setOrientationIndicator(0, false);
        this.mHandler.sendEmptyMessage(9);
        this.mUI.updateOnScreenIndicators(this.mCameraSettings);
    }

    private void initializeVideoSnapshot() {
        if (this.mCameraSettings != null) {
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void showVideoSnapshotUI(boolean enabled) {
        if (!(this.mCameraSettings == null || !this.mCameraCapabilities.supports(Feature.VIDEO_SNAPSHOT) || this.mIsVideoCaptureIntent)) {
            if (enabled) {
                this.mUI.animateFlash();
            } else {
                this.mUI.showPreviewBorder(enabled);
            }
            this.mAppController.setShutterEnabled(enabled ^ 1);
        }
    }

    /* Access modifiers changed, original: protected */
    public void enableTorchMode(boolean enable) {
        if (this.mCameraSettings.getCurrentFlashMode() != null) {
            FlashMode flashMode;
            SettingsManager settingsManager = this.mActivity.getSettingsManager();
            Stringifier stringifier = this.mCameraCapabilities.getStringifier();
            if (enable) {
                flashMode = stringifier.flashModeFromString(settingsManager.getString(this.mAppController.getCameraScope(), Keys.KEY_VIDEOCAMERA_FLASH_MODE));
            } else {
                flashMode = FlashMode.OFF;
            }
            if (this.mCameraCapabilities.supports(flashMode)) {
                this.mCameraSettings.setFlashMode(flashMode);
            }
            if (this.mCameraDevice != null) {
                this.mCameraDevice.applySettings(this.mCameraSettings);
            }
            this.mUI.updateOnScreenIndicators(this.mCameraSettings);
        }
    }

    public void onPreviewVisibilityChanged(int visibility) {
        int i = this.mCameraState;
    }

    private void storeImage(byte[] data, Location loc) {
        long dateTaken = System.currentTimeMillis();
        String title = CameraUtil.createJpegName(dateTaken);
        ExifInterface exif = Exif.getExif(data);
        int orientation = Exif.getOrientation(exif);
        String flashSetting = this.mActivity.getSettingsManager().getString(this.mAppController.getCameraScope(), Keys.KEY_VIDEOCAMERA_FLASH_MODE);
        Boolean gridLinesOn = Boolean.valueOf(Keys.areGridLinesOn(this.mActivity.getSettingsManager()));
        UsageStatistics instance = UsageStatistics.instance();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(title);
        stringBuilder.append(".jpeg");
        instance.photoCaptureDoneEvent(10000, stringBuilder.toString(), exif, isCameraFrontFacing(), false, currentZoomValue(), flashSetting, gridLinesOn.booleanValue(), null, null, null);
        MediaSaver mediaSaver = getServices().getMediaSaver();
        OnMediaSavedListener onMediaSavedListener = this.mOnPhotoSavedListener;
        mediaSaver.addImage(data, title, dateTaken, loc, orientation, exif, onMediaSavedListener, this.mContentResolver);
    }

    private String convertOutputFormatToMimeType(int outputFileFormat) {
        if (outputFileFormat == 2) {
            return "video/mp4";
        }
        return "video/3gpp";
    }

    private String convertOutputFormatToFileExt(int outputFileFormat) {
        if (outputFileFormat == 2) {
            return ".mp4";
        }
        return ".3gp";
    }

    private void closeVideoFileDescriptor() {
        if (this.mVideoFileDescriptor != null) {
            try {
                this.mVideoFileDescriptor.close();
            } catch (IOException e) {
                Log.e(TAG, "Fail to close fd", e);
            }
            this.mVideoFileDescriptor = null;
        }
    }

    public void onPreviewUIReady() {
        startPreview();
    }

    public void onPreviewUIDestroyed() {
        stopPreview();
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
        this.mActivity.getCameraProvider().requestCamera(this.mCameraId, false);
    }

    public void onMemoryStateChanged(int state) {
        this.mAppController.setShutterEnabled(state == 0);
    }

    public void onLowMemory() {
    }

    public void autoFocus() {
        if (this.mCameraDevice != null) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("auto focus , is state recording ? ");
            stringBuilder.append(isCameraStateRecording());
            Log.v(tag, stringBuilder.toString());
            this.isFocused = false;
            this.isAeAfLocked = false;
            this.mCameraDevice.autoFocus(this.mHandler, this.mAutoFocusCallback);
        }
    }

    public boolean cancelAutoFocus() {
        clearFocus();
        this.mUI.clearEvoPendingUI();
        return true;
    }

    private void clearFocus() {
        this.mUI.clearEvoPendingUI();
        if (this.mCameraDevice != null) {
            this.mCameraDevice.cancelAutoFocus();
            setFocusParameters();
        }
    }

    public boolean capture() {
        return false;
    }

    /* Access modifiers changed, original: protected */
    public boolean needFaceDetection() {
        return false;
    }

    /* JADX WARNING: Missing block: B:10:0x0036, code skipped:
            return;
     */
    public void startFaceDetection() {
        /*
        r4 = this;
        r0 = r4.mFaceDetectionStarted;
        if (r0 != 0) goto L_0x0036;
    L_0x0004:
        r0 = r4.mCameraDevice;
        if (r0 == 0) goto L_0x0036;
    L_0x0008:
        r0 = r4.needFaceDetection();
        if (r0 != 0) goto L_0x000f;
    L_0x000e:
        goto L_0x0036;
    L_0x000f:
        r0 = r4.mCameraCapabilities;
        r0 = r0.getMaxNumOfFacesSupported();
        if (r0 <= 0) goto L_0x0035;
    L_0x0017:
        r0 = 1;
        r4.mFaceDetectionStarted = r0;
        r1 = TAG;
        r2 = "startFaceDetection";
        com.hmdglobal.app.camera.debug.Log.w(r1, r2);
        r1 = r4.mCameraDevice;
        r2 = r4.mHandler;
        r3 = 0;
        r1.setFaceDetectionCallback(r2, r3);
        r1 = r4.mCameraDevice;
        r1.startFaceDetection();
        r1 = com.hmdglobal.app.camera.util.SessionStatsCollector.instance();
        r1.faceScanActive(r0);
    L_0x0035:
        return;
    L_0x0036:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.VideoModule.startFaceDetection():void");
    }

    /* JADX WARNING: Missing block: B:8:0x002e, code skipped:
            return;
     */
    public void stopFaceDetection() {
        /*
        r3 = this;
        r0 = r3.mFaceDetectionStarted;
        if (r0 == 0) goto L_0x002e;
    L_0x0004:
        r0 = r3.mCameraDevice;
        if (r0 != 0) goto L_0x0009;
    L_0x0008:
        goto L_0x002e;
    L_0x0009:
        r0 = r3.mCameraCapabilities;
        r0 = r0.getMaxNumOfFacesSupported();
        if (r0 <= 0) goto L_0x002d;
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
        r1 = com.hmdglobal.app.camera.util.SessionStatsCollector.instance();
        r1.faceScanActive(r0);
    L_0x002d:
        return;
    L_0x002e:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.VideoModule.stopFaceDetection():void");
    }

    public void setFocusParameters() {
        if (this.mCameraDevice != null) {
            updateFocusParameters();
            this.mCameraDevice.applySettings(this.mCameraSettings);
        }
    }

    /* JADX WARNING: Missing block: B:19:0x0043, code skipped:
            return;
     */
    public void doVideoCapture() {
        /*
        r6 = this;
        r0 = r6.mPaused;
        if (r0 != 0) goto L_0x0043;
    L_0x0004:
        r0 = r6.mCameraDevice;
        if (r0 != 0) goto L_0x0009;
    L_0x0008:
        goto L_0x0043;
    L_0x0009:
        r0 = r6.mMediaRecorderRecording;
        if (r0 == 0) goto L_0x0042;
    L_0x000d:
        r0 = r6.mNeedGLRender;
        if (r0 == 0) goto L_0x0029;
    L_0x0011:
        r0 = r6.isSupportEffects();
        if (r0 == 0) goto L_0x0029;
    L_0x0017:
        r0 = r6.mCameraDevice;
        r1 = 0;
        r0.enableShutterSound(r1);
        r0 = r6.mAppController;
        r0 = r0.getCameraAppUI();
        r1 = r6.mPictureTaken;
        r0.takePicture(r1);
        goto L_0x0041;
    L_0x0029:
        r0 = r6.mSnapshotInProgress;
        if (r0 != 0) goto L_0x0041;
    L_0x002d:
        r0 = java.lang.System.currentTimeMillis();
        r2 = r6.mLastTakePictureTime;
        r2 = r0 - r2;
        r4 = 2000; // 0x7d0 float:2.803E-42 double:9.88E-321;
        r2 = (r2 > r4 ? 1 : (r2 == r4 ? 0 : -1));
        if (r2 >= 0) goto L_0x003c;
    L_0x003b:
        return;
    L_0x003c:
        r6.mLastTakePictureTime = r0;
        r6.takeASnapshot();
    L_0x0041:
        return;
    L_0x0042:
        return;
    L_0x0043:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.VideoModule.doVideoCapture():void");
    }

    public void pauseVideoRecording() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("pauseVideoRecording mMediaRecorderRecording=");
        stringBuilder.append(this.mMediaRecorderRecording);
        stringBuilder.append(" mMediaRecoderRecordingPaused=");
        stringBuilder.append(this.mMediaRecoderRecordingPaused);
        android.util.Log.i("camera", stringBuilder.toString());
        if (!this.mMediaRecorderRecording) {
            return;
        }
        if (this.mMediaRecoderRecordingPaused) {
            try {
                Thread.sleep(400);
            } catch (Exception e) {
            }
            try {
                if (!this.mNeedGLRender) {
                    this.mMediaRecorder.resume();
                }
                this.mRecordingStartTime = SystemClock.uptimeMillis() - this.mVideoRecordedDuration;
                this.mVideoRecordedDuration = 0;
                this.mMediaRecoderRecordingPaused = false;
                return;
            } catch (IllegalStateException e2) {
                Log.e(TAG, "Could not start media recorder. ", e2);
                releaseMediaRecorder();
                return;
            }
        }
        if (!this.mNeedGLRender) {
            try {
                android.util.Log.i("camera", "mMediaRecorder.pause");
                this.mMediaRecorder.pause();
            } catch (IllegalStateException e3) {
                Log.e(TAG, "Could not pause media recorder. ");
            }
        }
        this.mVideoRecordedDuration = SystemClock.uptimeMillis() - this.mRecordingStartTime;
        this.mMediaRecoderRecordingPaused = true;
    }

    private void setEv(int ev) {
        this.mFocusManager.setAeAwbLock(true);
        this.mFocusManager.keepFocusFrame();
        setExposureCompensation(ev, false);
        if (this.mCameraDevice != null) {
            this.mCameraDevice.applySettings(this.mCameraSettings);
        }
    }

    public void onEvoChanged(int index) {
        if (this.mTempEV != index) {
            this.mTempEV = index;
            if (this.isFocused && index != 0) {
                this.isAeAfLocked = true;
            }
            if (this.isAeAfLocked && this.mFocusManager != null) {
                this.mEVHandler.removeMessages(100);
                this.mEVHandler.sendMessageDelayed(this.mEVHandler.obtainMessage(100, index, 0), 50);
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

    private void showQuitDialog(int titleId, int msgId, final Runnable runnable) {
        this.mAppController.getCameraAppUI().setViewFinderLayoutVisibile(true);
        if (this.quitDialog == null || !this.quitDialog.isShowing()) {
            Builder builder = new Builder(this.mActivity);
            builder.setCancelable(false);
            builder.setTitle(titleId);
            builder.setMessage(msgId);
            builder.setNegativeButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (runnable != null) {
                        runnable.run();
                    }
                }
            });
            this.quitDialog = builder.show();
            this.quitDialog.getButton(-1).setTextColor(this.mActivity.getResources().getColor(R.color.dialog_button_font_color));
            this.quitDialog.getButton(-2).setTextColor(this.mActivity.getResources().getColor(R.color.dialog_button_font_color));
        }
    }

    public boolean updateModeSwitchUIinModule() {
        return isVideoCaptureIntent() ^ 1;
    }

    /* Access modifiers changed, original: protected */
    public void setCameraState(int state) {
        switch (state) {
            case 1:
                this.mAppController.getLockEventListener().onIdle();
                break;
            case 2:
                this.mAppController.getLockEventListener().onShutter();
                break;
        }
        this.mCameraState = state;
    }

    /* Access modifiers changed, original: protected */
    public boolean shouldHoldRecorderForSecond() {
        return true;
    }

    /* Access modifiers changed, original: protected */
    public boolean isSendMsgEnableShutterButton() {
        return true;
    }

    public boolean isSupportEffects() {
        return false;
    }

    public void openOrCloseEffects(int state, int effects) {
        if (effects == R.id.effect_toggle_button) {
            boolean isEffectEnable = this.mActivity.getCameraAppUI().getEffectEnable();
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("openOrCloseEffects mNeedGLRender = ");
            stringBuilder.append(this.mNeedGLRender);
            stringBuilder.append(",  isEffectEnable = ");
            stringBuilder.append(isEffectEnable);
            Log.d(tag, stringBuilder.toString());
            if (this.mNeedGLRender) {
                if (!isEffectEnable) {
                    Log.d(TAG, "openOrCloseEffects closeEffects");
                    this.mNeedGLRender = false;
                    this.mActivity.getCameraAppUI().showOrHideGLSurface(false);
                    requestCameraOpen();
                }
            } else if (!isEffectEnable) {
                Log.d(TAG, "openOrCloseEffects openEffects");
                this.mNeedGLRender = true;
                this.mCameraDevice.stopPreview();
                updateDesiredPreviewSize();
                this.mCameraSettings.setPreviewSize(new Size(this.mDesiredPreviewWidth, this.mDesiredPreviewHeight));
                resizeForPreviewAspectRatio();
                this.mActivity.getCameraAppUI().getCameraGLSurfaceView().setOrientation(this.mCameraId);
                this.mActivity.getCameraAppUI().showOrHideGLSurface(true);
                requestCameraOpen();
                this.mCameraDevice.setPreviewTexture(this.mActivity.getCameraAppUI().getSurfaceTexture(this.mNeedGLRender));
                this.mCameraDevice.startPreview();
                this.mActivity.getCameraAppUI().getCameraGLSurfaceView().initRender(this.mCameraDevice);
                this.mActivity.getCameraAppUI().getCameraGLSurfaceView().onPreviewStarted();
            }
        } else if (state < 0 && effects < 0) {
            this.mNeedGLRender = false;
            this.mActivity.getCameraAppUI().showOrHideGLSurface(false);
            requestCameraOpen();
        }
    }

    public boolean onGLRenderEnable() {
        return false;
    }

    /* Access modifiers changed, original: protected */
    public void setTimeLapsed(boolean isTL) {
        this.isTL = isTL;
    }
}
