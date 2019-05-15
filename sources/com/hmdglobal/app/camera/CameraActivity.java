package com.hmdglobal.app.camera;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateBeamUrisCallback;
import android.nfc.NfcEvent;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore.Images.Media;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Video.Thumbnails;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.support.v4.media.MediaPlayer2;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.CameraPerformanceTracker;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ShareActionProvider;
import android.widget.ShareActionProvider.OnShareTargetSelectedListener;
import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.CameraAgent.CameraOpenCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraPreviewDataCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.android.ex.camera2.portability.CameraAgentFactory;
import com.android.ex.camera2.portability.CameraAgentFactory.CameraApi;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraCapabilities.SceneMode;
import com.android.ex.camera2.portability.CameraExceptionHandler;
import com.android.ex.camera2.portability.CameraExceptionHandler.CameraExceptionCallback;
import com.android.ex.camera2.portability.CameraSettings;
import com.android.ex.camera2.portability.CameraSettings.BoostParameters;
import com.android.ex.camera2.portability.Size;
import com.android.internal.app.LocalePicker;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.MemoryCategory;
import com.bumptech.glide.load.engine.executor.FifoPriorityThreadPoolExecutor;
import com.google.android.apps.photos.api.PhotosOemApi;
import com.hmdglobal.app.camera.SoundClips.Player;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.app.AppController.ShutterEventsListener;
import com.hmdglobal.app.camera.app.CameraAppUI;
import com.hmdglobal.app.camera.app.CameraAppUI.LockEventListener;
import com.hmdglobal.app.camera.app.CameraController;
import com.hmdglobal.app.camera.app.CameraProvider;
import com.hmdglobal.app.camera.app.CameraServices;
import com.hmdglobal.app.camera.app.LocationManager;
import com.hmdglobal.app.camera.app.MemoryManager;
import com.hmdglobal.app.camera.app.ModuleManager;
import com.hmdglobal.app.camera.app.ModuleManager.ModuleAgent;
import com.hmdglobal.app.camera.app.ModuleManagerImpl;
import com.hmdglobal.app.camera.app.MotionManager;
import com.hmdglobal.app.camera.app.OrientationManager;
import com.hmdglobal.app.camera.app.OrientationManager.OnOrientationChangeListener;
import com.hmdglobal.app.camera.app.OrientationManagerImpl;
import com.hmdglobal.app.camera.beauty.util.SharedUtil;
import com.hmdglobal.app.camera.beauty.util.Util;
import com.hmdglobal.app.camera.data.LocalData;
import com.hmdglobal.app.camera.data.LocalDataUtil;
import com.hmdglobal.app.camera.data.LocalDataViewType;
import com.hmdglobal.app.camera.data.LocalMediaData.PhotoData;
import com.hmdglobal.app.camera.data.LocalMediaData.VideoData;
import com.hmdglobal.app.camera.data.LocalMediaObserver;
import com.hmdglobal.app.camera.data.MetadataLoader;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.exif.ExifInterface.GpsMeasureMode;
import com.hmdglobal.app.camera.filmstrip.FilmstripController;
import com.hmdglobal.app.camera.hardware.HardwareSpecImpl;
import com.hmdglobal.app.camera.module.ModuleController;
import com.hmdglobal.app.camera.module.ModulesInfo;
import com.hmdglobal.app.camera.one.OneCameraManager;
import com.hmdglobal.app.camera.session.CaptureSessionManager.SessionListener;
import com.hmdglobal.app.camera.settings.AppUpgrader;
import com.hmdglobal.app.camera.settings.CameraSettingsActivity;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.settings.SettingsUtil;
import com.hmdglobal.app.camera.specialtype.HmdThumbnailProvider;
import com.hmdglobal.app.camera.specialtype.ProcessingMediaManager;
import com.hmdglobal.app.camera.specialtype.ProcessingMediaManager.ProcessingMedia;
import com.hmdglobal.app.camera.tinyplanet.TinyPlanetFragment;
import com.hmdglobal.app.camera.ui.AbstractTutorialOverlay;
import com.hmdglobal.app.camera.ui.Lockable;
import com.hmdglobal.app.camera.ui.MainActivityLayout;
import com.hmdglobal.app.camera.ui.ModeStrip;
import com.hmdglobal.app.camera.ui.ModeTransitionView.OnTransAnimationListener;
import com.hmdglobal.app.camera.ui.PreviewStatusListener;
import com.hmdglobal.app.camera.ui.PreviewStatusListener.PreviewAreaChangedListener;
import com.hmdglobal.app.camera.ui.Rotatable.RotateEntity;
import com.hmdglobal.app.camera.ui.StereoModeStripView;
import com.hmdglobal.app.camera.util.ApiHelper;
import com.hmdglobal.app.camera.util.BeautifyHandler;
import com.hmdglobal.app.camera.util.BlurUtil;
import com.hmdglobal.app.camera.util.Callback;
import com.hmdglobal.app.camera.util.CameraConstant;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.hmdglobal.app.camera.util.CustomFields;
import com.hmdglobal.app.camera.util.CustomUtil;
import com.hmdglobal.app.camera.util.GcamHelper;
import com.hmdglobal.app.camera.util.GservicesHelper;
import com.hmdglobal.app.camera.util.MccTable;
import com.hmdglobal.app.camera.util.PermissionsUtil;
import com.hmdglobal.app.camera.util.PermissionsUtil.CriticalPermsStatus;
import com.hmdglobal.app.camera.util.PermissionsUtil.RequestingPerms;
import com.hmdglobal.app.camera.util.PhotoSphereHelper.PanoramaViewHelper;
import com.hmdglobal.app.camera.util.QuickActivity;
import com.hmdglobal.app.camera.util.StorageUtilProxy;
import com.hmdglobal.app.camera.util.StorageUtils;
import com.hmdglobal.app.camera.util.ToastUtil;
import com.hmdglobal.app.camera.util.UsageStatistics;
import com.morphoinc.utils.multimedia.MediaProviderUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class CameraActivity extends QuickActivity implements AppController, CameraOpenCallback, OnShareTargetSelectedListener, OnOrientationChangeListener {
    public static final String ACTION_IMAGE_CAPTURE_SECURE = "android.media.action.IMAGE_CAPTURE_SECURE";
    private static final String ACTION_MCC_CHANGED = "android.intent.action.MCC_CHANGED";
    public static final String ACTION_PRO_CAMERA = "com.hmdglobal.action.CAMERA_PROCAMERA";
    private static final String ACTION_SERVICE_STATE_CHANGED = "android.intent.action.SERVICE_STATE";
    private static final String AT = "AT";
    private static final String AU = "AU";
    public static final int BATTERY_STATUS_LOW = 2;
    public static final int BATTERY_STATUS_OK = 0;
    public static final int BATTERY_STATUS_WARNING = 1;
    private static final String CA = "CA";
    private static final String CAMERA_MOTION_PICTURE = "com.hmdglobal.action.CAMERA_MOTION_PICTURE";
    public static final String CAMERA_SCOPE_PREFIX = "_preferences_camera_";
    public static final int CAMERA_VIDEO_MODE_INDEX = 3;
    private static final String CN = "CN";
    private static final String DE = "DE";
    private static final String ES = "ES";
    public static String EXIT_CAMERA_ACTION = "android.action.exit.camera";
    private static final int FILMSTRIP_PRELOAD_AHEAD_ITEMS = 10;
    private static final String FLASH_OFF = "off";
    private static final String FR = "FR";
    private static final String GB = "GB";
    private static final String GR = "GR";
    private static final String HK = "HK";
    private static final String ID = "ID";
    private static final String IN = "IN";
    public static final String INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE = "android.media.action.STILL_IMAGE_CAMERA_SECURE";
    private static final String IT = "IT";
    private static final String JP = "JP";
    private static final String KR = "KR";
    private static final int LIGHTS_OUT_DELAY_MS = 4000;
    public static final int LOW_BATTERY_LEVEL = 0;
    private static final int MAX_PEEK_BITMAP_PIXELS = 1600000;
    public static final int MICRO_MDOE = 10;
    private static final String MO = "MO";
    public static final String MODULE_SCOPE_PREFIX = "_preferences_module_";
    private static final int MSG_CLEAR_SCREEN_ON_FLAG = 2;
    private static final String MX = "MX";
    private static final String MY = "MY";
    private static final String NL = "NL";
    private static final String NO = "NO";
    private static final int NOTIFY_NEW_MEDIA_ACTION_ANIMATION = 1;
    private static final int NOTIFY_NEW_MEDIA_ACTION_OPTIMIZECAPTURE = 4;
    private static final int NOTIFY_NEW_MEDIA_ACTION_UPDATETHUMB = 2;
    private static final int NOTIFY_NEW_MEDIA_DEFALT_ACTION = 3;
    private static final String NZ = "NZ";
    private static final String PH = "PH";
    public static final String PREF_CAMERAACTIVITY = "pref_cameraactivity";
    private static final String RU = "RU";
    private static final long SCREEN_DELAY_MS = 120000;
    private static final String SE = "SE";
    public static final String SECURE_CAMERA_EXTRA = "secure_camera";
    private static final String SG = "SG";
    public static final int SLOMO_MODE = 8;
    private static final Tag TAG = new Tag("CameraActivity");
    private static final String TH = "TH";
    private static final String TIMER_DURATION_SECONDS = "com.google.assistant.extra.TIMER_DURATION_SECONDS";
    private static final String TIZR_PACKAGE_NAME = "com.app_tizr.app.in_house";
    private static final String TW = "TW";
    private static final String US = "US";
    private static final String USE_FRONT_CAMERA = "com.google.assistant.extra.USE_FRONT_CAMERA";
    public static final int WARNING_BATTERY_LEVEL = 15;
    public static boolean gIsCameraActivityRunning = false;
    private static LayoutParams params;
    private static WindowManager wm;
    private final int BASE_SYS_UI_VISIBILITY = 1280;
    private final String CAMERA_ID = "cameraid";
    private final String FUNC_SELFIE = "func_selfie";
    private final long INNER_STORAGE_THRESHOLD = 52428800;
    private final int[] MEXICO_LOCALE_MCC_LIST = new int[]{330, 334, 338, 340, 342, 344, 346, 348, 352, 354, 356, 358, 360, 362, 363, 365, 366, 368, 370, 372, 374, 376, 702, MediaPlayer2.MEDIA_INFO_BUFFERING_UPDATE, 706, 708, 710, 712, 714, 716, 722, 730, 732, 734, 736, 738, 740, 744, 746, 748};
    private final int REQUEST_CODE = 110;
    private String SYSTEM_HOME_KEY = "homekey";
    private String SYSTEM_REASON = "reason";
    private String SYSTEM_RECENT_APPS = "recentapps";
    private final int[] UK_LOCALE_MCC_LIST = new int[]{272};
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Tag access$1000 = CameraActivity.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onReceive CameraId = ");
            stringBuilder.append(CameraActivity.this.mCameraController.getCurrentCameraId());
            stringBuilder.append(" flag = ");
            stringBuilder.append(intent.getStringExtra("flag"));
            Log.d(access$1000, stringBuilder.toString());
            if (CameraActivity.this.mCameraController.getCurrentCameraId() == 1) {
                if (intent.getStringExtra("flag").equals("show")) {
                    CameraActivity.this.count = CameraActivity.this.count + 1;
                } else if (intent.getStringExtra("flag").equals("hide")) {
                    CameraActivity.this.count = CameraActivity.this.count - 1;
                }
                access$1000 = CameraActivity.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("onReceive count = ");
                stringBuilder.append(CameraActivity.this.count);
                Log.d(access$1000, stringBuilder.toString());
                if (CameraActivity.this.count < 0) {
                    CameraActivity.this.count = 0;
                } else if (CameraActivity.this.count == 0) {
                    if (CameraActivity.this.btn_floatView != null) {
                        CameraActivity.this.resetScreenBrightness();
                        CameraActivity.this.removeFloatView();
                    }
                } else if (CameraActivity.this.btn_floatView == null) {
                    CameraActivity.this.saveScreenBrightness();
                    CameraActivity.this.createFloatView();
                }
            }
        }
    };
    private Button btn_floatView;
    private int count = 0;
    private int currentBatteryStatus = 0;
    private long freeInnerStorage;
    private long lastStopStamp = -1;
    private Context mAppContext;
    private boolean mAutoRotateScreen;
    private BatteryBroadcastReceiver mBatteryChangedReceiver;
    private int mBatteryLevel;
    private boolean mBatteryLevelLowFirst = true;
    private AlertDialog mBatteryLowDialog;
    private OnBatteryLowListener mBatteryLowListener;
    private boolean mBatterySaveOn = false;
    private AlertDialog mBatteryWarningDialog;
    private int mBrightnessMode = -1;
    private int mBrightnessValue = 0;
    private ButtonManager mButtonManager;
    private CameraAppUI mCameraAppUI;
    private CameraController mCameraController;
    private final CameraExceptionCallback mCameraExceptionCallback = new CameraExceptionCallback() {
        public void onCameraError(int errorCode) {
            Tag access$1000 = CameraActivity.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Camera error callback. error=");
            stringBuilder.append(errorCode);
            Log.e(access$1000, stringBuilder.toString());
        }

        public void onCameraException(RuntimeException ex, String commandHistory, int action, int state) {
            Log.e(CameraActivity.TAG, "Camera Exception", ex);
            UsageStatistics.instance().cameraFailure(10000, commandHistory, action, state);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" onCameraException  ");
            stringBuilder.append(ex);
            android.util.Log.e("+++===============-----", stringBuilder.toString());
            onFatalError();
        }

        public void onDispatchThreadException(RuntimeException ex) {
            Log.e(CameraActivity.TAG, "DispatchThread Exception", ex);
            UsageStatistics.instance().cameraFailure(10000, null, -1, -1);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" onDispatchThreadException  ");
            stringBuilder.append(ex.getMessage());
            android.util.Log.e("+++===============-----", stringBuilder.toString());
            onFatalError();
        }

        private void onFatalError() {
            if (!CameraActivity.this.mCameraFatalError) {
                CameraActivity.this.mCameraFatalError = true;
                if (!CameraActivity.this.mPaused || CameraActivity.this.isFinishing()) {
                    CameraUtil.showErrorAndFinish(CameraActivity.this, R.string.cannot_connect_camera);
                } else {
                    Log.e(CameraActivity.TAG, "Fatal error during onPause, call Activity.finish()");
                    CameraActivity.this.finishAndQuitProcess();
                }
            }
        }
    };
    private boolean mCameraFatalError = false;
    private OneCameraManager mCameraManager;
    private OnCameraReady mCameraReadyListener;
    private int mCurrentModeIndex;
    private CameraModule mCurrentModule;
    private boolean mDelayTake = false;
    public boolean mDuringCall;
    private ExitBroadCast mExitBroadCast;
    private FilmstripController mFilmstripController;
    private boolean mFilmstripCoversPreview = false;
    private boolean mFilmstripVisible;
    private Intent mGalleryIntent;
    private HelpTipsManager mHelpTipsManager;
    private OnInnerStorageLowListener mInnerStorageLowListener;
    private boolean mIsLaunchFromAssistant = false;
    private boolean mIsUndoingDeletion = false;
    private boolean mKeepScreenOn;
    private boolean mKeepSecureModule = false;
    private int mLastLayoutOrientation;
    private boolean mLastMotionState = true;
    private int mLastRawOrientation;
    private final Runnable mLightsOutRunnable = new Runnable() {
        public void run() {
            CameraActivity.this.getWindow().getDecorView().setSystemUiVisibility(CameraAppUI.SHOW_NAVIGATION_VIEW);
        }
    };
    private Map<Integer, RotateEntity> mListeningRotatableMap = new HashMap();
    private LocalMediaObserver mLocalImagesObserver;
    private LocalMediaObserver mLocalVideosObserver;
    private String[] mLocaleIdArray = null;
    private LocationManager mLocationManager;
    private MainActivityLayout mMainActivityLayout;
    private Handler mMainHandler;
    private BroadcastReceiver mMccChangedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            CameraActivity.this.mccFromIntent = intent.getStringExtra("mcc");
        }
    };
    private final BroadcastReceiver mMediaActionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (CameraActivity.this.mCurrentModule != null) {
                CameraActivity.this.mCurrentModule.onMediaAction(context, intent);
            }
            String action = intent.getAction();
            Tag access$1000 = CameraActivity.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("MediaAction, ");
            stringBuilder.append(action);
            Log.e(access$1000, stringBuilder.toString());
            if (action.equals("android.intent.action.MEDIA_UNMOUNTED") || action.equals("android.intent.action.MEDIA_EJECT")) {
                CameraActivity.this.mMediaMounted = false;
                CameraActivity.this.mCameraAppUI.setViewFinderLayoutVisibile(false);
                if (CameraActivity.this.mStorageLowDialog != null && CameraActivity.this.mStorageLowDialog.isShowing()) {
                    CameraActivity.this.mStorageLowDialog.dismiss();
                    CameraActivity.this.mStorageLowDialog = null;
                }
                if (CameraActivity.this.mMountedDialog != null && CameraActivity.this.mMountedDialog.isShowing()) {
                    CameraActivity.this.mMountedDialog.dismiss();
                    CameraActivity.this.mMountedDialog = null;
                }
            } else if (action.equals("android.intent.action.MEDIA_MOUNTED")) {
                CameraActivity.this.mMediaMounted = true;
                if (CameraActivity.this.mMountedDialog != null && CameraActivity.this.mMountedDialog.isShowing()) {
                    CameraActivity.this.mMountedDialog.dismiss();
                    CameraActivity.this.mMountedDialog = null;
                }
                if (!Storage.getSavePath().equals("1")) {
                    CameraActivity.this.updateStorageSpaceAndHint(null, true);
                } else {
                    return;
                }
            } else if (action.equals("android.intent.action.MEDIA_SCANNER_FINISHED") && (CameraActivity.this.needUpdateWhenScanFinished || Storage.getSavePath().equals("1"))) {
                if (!(CameraActivity.this.isSecureCamera() || CameraActivity.this.isCaptureIntent() || !CameraActivity.this.isExternalStorageAvailable())) {
                    CameraActivity.this.onLastMediaDataUpdated();
                }
                CameraActivity.this.needUpdateWhenScanFinished = false;
            }
            if (false) {
                CameraActivity.this.updateStorageSpaceAndHint(null);
                CameraActivity.this.needUpdateWhenScanFinished = true;
            }
        }
    };
    private boolean mMediaMounted = false;
    private MemoryManager mMemoryManager;
    private final ModeChangeRunnable mModeChangeRunnable = new ModeChangeRunnable() {
        int updateModeIndex;

        public void run() {
            if (CameraActivity.this.mCurrentModule != null) {
                Tag access$1000 = CameraActivity.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("close module ");
                stringBuilder.append(CameraActivity.this.mCurrentModule);
                Log.w(access$1000, stringBuilder.toString());
                CameraActivity.this.closeModule(CameraActivity.this.mCurrentModule);
                CameraActivity.this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_SQUARE_RETURN_TO_INDEX, CameraActivity.this.mCurrentModule.getModuleId());
                this.updateModeIndex = CameraActivity.this.getPreferredChildModeIndex(this.mTargetIndex);
                CameraActivity.this.setModuleFromModeIndex(this.updateModeIndex);
                CameraActivity.this.mCameraAppUI.resetBottomControls(CameraActivity.this.mCurrentModule, this.updateModeIndex);
                CameraActivity.this.mCameraAppUI.addShutterListener(CameraActivity.this.mCurrentModule);
                Log.w(CameraActivity.TAG, "openModule");
                CameraActivity.this.openModule(CameraActivity.this.mCurrentModule);
                CameraActivity.this.mCurrentModule.onOrientationChanged(CameraActivity.this.mLastRawOrientation);
                access$1000 = CameraActivity.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("open module ");
                stringBuilder.append(CameraActivity.this.mCurrentModule);
                Log.w(access$1000, stringBuilder.toString());
                int photoIndex = CameraActivity.this.getResources().getInteger(R.integer.camera_mode_photo);
                if (this.updateModeIndex == CameraActivity.this.getResources().getInteger(R.integer.camera_mode_video)) {
                    this.updateModeIndex = photoIndex;
                }
                CameraActivity.this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_STARTUP_MODULE_INDEX, this.updateModeIndex);
            }
        }
    };
    private boolean mModeListVisible = false;
    private boolean mModeSelectingOnStart = false;
    private ModeStrip mModeStripView;
    private ModuleManagerImpl mModuleManager;
    private boolean mModuleOpenBeforeResume = false;
    private MotionManager mMotionManager;
    private AlertDialog mMountedDialog;
    private boolean mNeedRestoreReversible = false;
    private final Uri[] mNfcPushUris = new Uri[1];
    private long mOnCreateTime;
    private OrientationManagerImpl mOrientationManager;
    private PanoramaViewHelper mPanoramaViewHelper;
    private boolean mPaused;
    private PeekAnimationHandler mPeekAnimationHandler;
    private HandlerThread mPeekAnimationThread;
    private boolean mPendingDeletion = false;
    private PhoneStateListener mPhoneStateListener;
    boolean mRequestPermissionsFinished = false;
    private boolean mResetToPreviewOnResume = true;
    private int mResultCodeForTesting;
    private Intent mResultDataForTesting;
    public boolean mSLOORMIC;
    private boolean mSecureCamera;
    private boolean mSecureFyuseModule = false;
    private ArrayList<Uri> mSecureUris;
    private final SessionListener mSessionListener = new SessionListener() {
        public void onSessionQueued(Uri uri) {
            Tag access$1000 = CameraActivity.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onSessionQueued: ");
            stringBuilder.append(uri);
            Log.v(access$1000, stringBuilder.toString());
        }

        public void onSessionDone(Uri sessionUri) {
            Tag access$1000 = CameraActivity.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onSessionDone:");
            stringBuilder.append(sessionUri);
            Log.v(access$1000, stringBuilder.toString());
        }

        public void onSessionProgress(Uri uri, int progress) {
        }

        public void onSessionProgressText(Uri uri, CharSequence message) {
        }

        public void onSessionUpdated(Uri uri) {
            Tag access$1000 = CameraActivity.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onSessionUpdated: ");
            stringBuilder.append(uri);
            Log.v(access$1000, stringBuilder.toString());
        }

        public void onSessionPreviewAvailable(Uri uri) {
            Tag access$1000 = CameraActivity.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onSessionPreviewAvailable: ");
            stringBuilder.append(uri);
            Log.v(access$1000, stringBuilder.toString());
        }

        public void onSessionFailed(Uri uri, CharSequence reason) {
            Tag access$1000 = CameraActivity.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onSessionFailed:");
            stringBuilder.append(uri);
            Log.v(access$1000, stringBuilder.toString());
        }
    };
    private SettingsManager mSettingsManager;
    private SharedPreferences mSharedPreferences;
    private final BroadcastReceiver mShutdownReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (!CameraActivity.this.mSecureCamera) {
                CameraActivity.this.finish();
            }
        }
    };
    private BroadcastReceiver mSimStateReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            CameraActivity.this.mSubId = intent.getIntExtra("subscription", -1);
        }
    };
    private Player mSoundClipsPlayer;
    private SoundPlayer mSoundPlayer;
    private OnScreenHint mStorageHint;
    private AlertDialog mStorageLowDialog;
    private long mStorageSpaceBytes = Storage.LOW_STORAGE_THRESHOLD_BYTES;
    private final Object mStorageSpaceLock = new Object();
    private int mSubId = -1;
    private TelephonyManager mTelephonyManager;
    private Runnable mThumbUpdateRunnable;
    private ViewGroup mUndoDeletionBar;
    private boolean mUseFrontCamera = false;
    private String mccFromIntent = "";
    private String nameMms = "PREFS_READ_WRITE_MULI_COM_ANDROID_CAMERA_ACTIVITY";
    private boolean needUpdateWhenScanFinished = false;
    private final BroadcastReceiver screenOffReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.CLOSE_SYSTEM_DIALOGS")) {
                String reason = intent.getStringExtra(CameraActivity.this.SYSTEM_REASON);
                if (TextUtils.equals(reason, CameraActivity.this.SYSTEM_HOME_KEY)) {
                    CameraActivity.this.mMainHandler.postDelayed(new Runnable() {
                        public void run() {
                            CameraActivity.this.mCameraAppUI.setVisibleKeyguard();
                            android.util.Log.d("liugz", "onReceive，00 setVisibleKeyguard");
                        }
                    }, 8);
                }
                if (TextUtils.equals(reason, CameraActivity.this.SYSTEM_RECENT_APPS)) {
                    CameraActivity.this.mCameraAppUI.setVisibleKeyguard();
                    CameraActivity.this.mCameraAppUI.setInVisibleKeyguard();
                    android.util.Log.d("liugz", "onReceive， 11 setVisibleKeyguard");
                }
            }
        }
    };

    private class BatteryBroadcastReceiver extends BroadcastReceiver {
        private BatteryBroadcastReceiver() {
        }

        /* synthetic */ BatteryBroadcastReceiver(CameraActivity x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            if ((CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_LOW_BATTERY_FEATURE_INDEPENDENT, false) || CameraUtil.isBatterySaverEnabled(CameraActivity.this)) && "android.intent.action.BATTERY_CHANGED".equals(intent.getAction())) {
                int batteryLevel = intent.getIntExtra("level", -1);
                if (batteryLevel == -1) {
                    Tag access$1000 = CameraActivity.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Bad Battery Changed intent: ");
                    stringBuilder.append(batteryLevel);
                    Log.e(access$1000, stringBuilder.toString());
                    return;
                }
                CameraActivity.this.mBatteryLevel = batteryLevel;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("mBatteryLevel: ");
                stringBuilder2.append(CameraActivity.this.mBatteryLevel);
                android.util.Log.d("BatteryBroadcastReceiver", stringBuilder2.toString());
                boolean bNeedPromptBatteryChanged = false;
                if (CameraActivity.this.mBatteryLevel > 15) {
                    CameraActivity.this.currentBatteryStatus = 0;
                    bNeedPromptBatteryChanged = true;
                    CameraActivity.this.mBatteryLevelLowFirst = true;
                } else if (CameraActivity.this.mBatteryLevel <= 15 && CameraActivity.this.mBatteryLevel > 0 && CameraActivity.this.mBatteryLevelLowFirst) {
                    CameraActivity.this.currentBatteryStatus = 1;
                    if (!"off".equals(CameraActivity.this.mSettingsManager.getString(CameraActivity.this.getCameraScope(), Keys.KEY_FLASH_MODE))) {
                        CameraActivity.this.mSettingsManager.set(CameraActivity.this.getCameraScope(), Keys.KEY_FLASH_MODE, "off");
                    }
                    if (!"off".equals(CameraActivity.this.mSettingsManager.getString(CameraActivity.this.getCameraScope(), Keys.KEY_VIDEOCAMERA_FLASH_MODE))) {
                        CameraActivity.this.mSettingsManager.set(CameraActivity.this.getCameraScope(), Keys.KEY_VIDEOCAMERA_FLASH_MODE, "off");
                    }
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("currentBatteryStatus2: ");
                    stringBuilder3.append(CameraActivity.this.currentBatteryStatus);
                    android.util.Log.d("BatteryBroadcastReceiver", stringBuilder3.toString());
                    bNeedPromptBatteryChanged = true;
                    CameraActivity.this.mBatteryLevelLowFirst = false;
                    CameraActivity.this.getButtonManager().disableButton(0);
                    CameraActivity.this.getButtonManager().disableButton(1);
                } else if (CameraActivity.this.mBatteryLevel <= 0) {
                    CameraActivity.this.currentBatteryStatus = 2;
                    bNeedPromptBatteryChanged = true;
                }
                SharedUtil.saveIntValue("currentBatteryStatus", CameraActivity.this.currentBatteryStatus);
                if (bNeedPromptBatteryChanged) {
                    CameraActivity.this.batteryStatusChange(CameraActivity.this.currentBatteryStatus);
                }
            }
        }
    }

    class ExitBroadCast extends BroadcastReceiver {
        ExitBroadCast() {
        }

        public void onReceive(Context context, Intent intent) {
            CameraActivity.this.finish();
            System.exit(0);
        }
    }

    private static class MainHandler extends Handler {
        final WeakReference<CameraActivity> mActivity;

        public MainHandler(CameraActivity activity, Looper looper) {
            super(looper);
            this.mActivity = new WeakReference(activity);
        }

        public void handleMessage(Message msg) {
            CameraActivity activity = (CameraActivity) this.mActivity.get();
            if (!(activity == null || msg.what != 2 || activity.mPaused)) {
                activity.getWindow().clearFlags(128);
            }
        }
    }

    private abstract class ModeChangeRunnable implements Runnable {
        protected int mTargetIndex;

        private ModeChangeRunnable() {
        }

        /* synthetic */ ModeChangeRunnable(CameraActivity x0, AnonymousClass1 x1) {
            this();
        }

        public void setTargetIndex(int index) {
            this.mTargetIndex = index;
        }
    }

    public interface OnBatteryLowListener {
        void onBatteryLow(int i);
    }

    private static abstract class OnCameraReady {
        protected WeakReference<CameraProxy> mCameraProxy;

        public OnCameraReady(CameraProxy cameraProxy) {
            this.mCameraProxy = new WeakReference(cameraProxy);
        }

        public void onCameraReady() {
        }
    }

    public interface OnInnerStorageLowListener {
        void onInnerStorageLow(long j);
    }

    protected interface OnStorageUpdateDoneListener {
        void onStorageUpdateDone(long j);
    }

    private class PeekAnimationHandler extends Handler {
        private final Handler mMainHandler;
        private final View mMainLayout;

        private class DataAndCallback {
            Callback<Bitmap> mCallback;
            LocalData mData;

            public DataAndCallback(LocalData data, Callback<Bitmap> callback) {
                this.mData = data;
                this.mCallback = callback;
            }
        }

        public PeekAnimationHandler(Looper looper, Handler mainHandler, View mainLayout) {
            super(looper);
            this.mMainHandler = mainHandler;
            this.mMainLayout = mainLayout;
        }

        public void startDecodingJob(LocalData data, Callback<Bitmap> callback) {
            obtainMessage(0, new DataAndCallback(data, callback)).sendToTarget();
        }

        public void handleMessage(Message msg) {
            LocalData data = ((DataAndCallback) msg.obj).mData;
            final Callback<Bitmap> callback = ((DataAndCallback) msg.obj).mCallback;
            if (data != null && callback != null) {
                Bitmap temp;
                Bitmap bitmap = null;
                switch (data.getLocalDataType()) {
                    case 3:
                        try {
                            FileInputStream stream = new FileInputStream(data.getPath());
                            Point dim = CameraUtil.resizeToFill(data.getWidth(), data.getHeight(), data.getRotation(), this.mMainLayout.getWidth(), this.mMainLayout.getMeasuredHeight());
                            if (data.getRotation() % MediaProviderUtils.ROTATION_180 != 0) {
                                int dummy = dim.x;
                                dim.x = dim.y;
                                dim.y = dummy;
                            }
                            bitmap = LocalDataUtil.loadImageThumbnailFromStream(stream, data.getWidth(), data.getHeight(), (int) (((float) dim.x) * 0.7f), (int) (((double) dim.y) * 0.7d), data.getRotation(), CameraActivity.MAX_PEEK_BITMAP_PIXELS);
                            break;
                        } catch (FileNotFoundException e) {
                            Tag access$1000 = CameraActivity.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("File not found:");
                            stringBuilder.append(data.getPath());
                            Log.e(access$1000, stringBuilder.toString());
                            return;
                        }
                    case 4:
                        temp = null;
                        try {
                            temp = Thumbnails.getThumbnail(CameraActivity.this.getApplicationContext().getContentResolver(), ContentUris.parseId(data.getUri()), 1, null);
                        } catch (Exception e2) {
                            temp = null;
                        }
                        if (temp == null) {
                            Log.w(CameraActivity.TAG, " get video thumbnail by decoding ");
                            bitmap = LocalDataUtil.loadVideoThumbnail(data.getPath());
                            break;
                        }
                        Log.w(CameraActivity.TAG, " get video thumbnail by database ");
                        bitmap = temp;
                        break;
                    case 5:
                        byte[] jpegData = Storage.getJpegForSession(data.getUri());
                        if (jpegData == null) {
                            bitmap = null;
                            break;
                        } else {
                            bitmap = BitmapFactory.decodeByteArray(jpegData, null, jpegData.length);
                            break;
                        }
                }
                temp = bitmap;
                if (temp == null) {
                    Log.w(CameraActivity.TAG, " bitmap   is empty -----=+++++===================== ");
                } else {
                    this.mMainHandler.post(new Runnable() {
                        public void run() {
                            callback.onCallback(temp);
                        }
                    });
                }
            }
        }
    }

    protected class innerStorageCheckTask extends AsyncTask<Void, Void, Long> {
        OnInnerStorageLowListener mListener;

        public innerStorageCheckTask(OnInnerStorageLowListener l) {
            this.mListener = l;
        }

        /* Access modifiers changed, original: protected|varargs */
        public Long doInBackground(Void... params) {
            Long valueOf;
            synchronized (CameraActivity.this.mStorageSpaceLock) {
                valueOf = Long.valueOf(Storage.getAvailableSpace());
            }
            return valueOf;
        }

        /* Access modifiers changed, original: protected */
        public void onPostExecute(Long bytes) {
            CameraActivity.this.freeInnerStorage = bytes.longValue();
            if (CameraActivity.this.freeInnerStorage < 52428800) {
                this.mListener.onInnerStorageLow(CameraActivity.this.freeInnerStorage);
            }
        }
    }

    public CameraAppUI getCameraAppUI() {
        return this.mCameraAppUI;
    }

    public LockEventListener getLockEventListener() {
        return this.mCameraAppUI.gLockEventListener;
    }

    public ModuleManager getModuleManager() {
        return this.mModuleManager;
    }

    public void onVideoRecordingStarted() {
        this.mCameraAppUI.onVideoRecordingStateChanged(true);
    }

    public void onVideoRecordingStop() {
        if (!this.mPaused) {
            onModeSelecting(true, new OnTransAnimationListener() {
                public void onAnimationDone() {
                    CameraActivity.this.mCameraAppUI.onModeSelected(CameraActivity.this.getResources().getInteger(R.integer.camera_mode_video));
                    CameraActivity.this.mCameraAppUI.onVideoRecordingStateChanged(false);
                    if (CameraActivity.this.mHelpTipsManager != null) {
                        CameraActivity.this.mHelpTipsManager.checkAlarmTaskHelpTip();
                    }
                }
            });
        }
    }

    public void onCameraOpened(CameraProxy camera) {
        Log.w(TAG, "on Camera opened in camera activity");
        if (this.mPaused) {
            Log.v(TAG, "received onCameraOpened but activity is paused, closing Camera");
            this.mCameraController.closeCamera(false);
            return;
        }
        if (this.mCurrentModule instanceof VideoModule) {
            this.mCameraAppUI.setCameraSurfaceDevice(camera);
        }
        if (!this.mSettingsManager.isSet(SettingsManager.SCOPE_GLOBAL, Keys.KEY_FLASH_SUPPORTED_BACK_CAMERA)) {
            this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_FLASH_SUPPORTED_BACK_CAMERA, new HardwareSpecImpl(getCameraProvider(), camera.getCapabilities()).isFlashSupported());
        }
        if (this.mModuleManager.getModuleAgent(this.mCurrentModeIndex).requestAppForCamera()) {
            if (this.mCurrentModule != null) {
                resetParametersToDefault(camera);
                this.mCurrentModule.onCameraAvailable(camera);
            } else {
                Log.v(TAG, "mCurrentModule null, not invoking onCameraAvailable");
                this.mCameraReadyListener = new OnCameraReady(camera) {
                    public void onCameraReady() {
                        CameraActivity.this.mCurrentModule.onCameraAvailable((CameraProxy) this.mCameraProxy.get());
                    }
                };
            }
            Log.v(TAG, "invoking onChangeCamera");
            this.mCameraAppUI.onChangeCamera();
            Log.v(TAG, "invoking setCameraProxy");
            this.mCameraAppUI.setCameraProxy(camera);
            return;
        }
        this.mCameraController.closeCamera(false);
        throw new IllegalStateException("Camera opened but the module shouldn't be requesting");
    }

    public void onCameraOpenedBoost(CameraProxy camera) {
    }

    public boolean isBoostPreview() {
        return false;
    }

    public Context getCallbackContext() {
        return getApplicationContext();
    }

    public BoostParameters getBoostParam() {
        return null;
    }

    private void resetParametersToDefault(CameraProxy camera) {
        if (!this.mCameraController.isBoostPreview() && camera != null) {
            CameraSettings cameraSettings = camera.getSettings();
            cameraSettings.setExposureCompensationIndex(0);
            cameraSettings.setFaceBeauty(false, 0);
            cameraSettings.setLowLight(false);
            CameraCapabilities cameraCapabilities = camera.getCapabilities();
            if (Keys.isHdrOn(this.mSettingsManager) && cameraCapabilities != null && cameraCapabilities.supports(SceneMode.AUTO)) {
                cameraSettings.setSceneMode(SceneMode.AUTO);
            }
            cameraSettings.setSuperResolutionOn(false);
            camera.applySettings(cameraSettings);
        }
    }

    public void onCameraDisabled(int cameraId) {
        UsageStatistics.instance().cameraFailure(10000, null, -1, -1);
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Camera disabled: ");
        stringBuilder.append(cameraId);
        Log.w(tag, stringBuilder.toString());
        CameraUtil.showErrorAndFinish(this, R.string.camera_disabled);
    }

    public void onDeviceOpenFailure(int cameraId, String info) {
        if (!this.mPaused) {
            UsageStatistics.instance().cameraFailure(10000, info, -1, -1);
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Camera open failure: ");
            stringBuilder.append(info);
            stringBuilder.append("  cameraId");
            stringBuilder.append(cameraId);
            Log.w(tag, stringBuilder.toString());
            this.mTelephonyManager = (TelephonyManager) getSystemService("phone");
            if (this.mTelephonyManager == null || this.mTelephonyManager.getCallState() != 2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("onDeviceOpenFailure   ");
                stringBuilder.append(info);
                android.util.Log.e("+++===============-----", stringBuilder.toString());
                CameraUtil.showErrorAndFinish(this, R.string.cannot_connect_camera);
            } else if (System.getInt(getContentResolver(), CameraConstant.IS_VIDEO_CALL, 0) <= 0) {
                CameraUtil.showErrorAndFinish(this, R.string.camera_error_during_voice_call);
            }
        }
    }

    public void onDeviceOpenedAlready(int cameraId, String info) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Camera open already: ");
        stringBuilder.append(cameraId);
        stringBuilder.append(Size.DELIMITER);
        stringBuilder.append(info);
        Log.w(tag, stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("onDeviceOpenedAlready   ");
        stringBuilder.append(info);
        android.util.Log.e("+++===============-----", stringBuilder.toString());
    }

    public void onReconnectionFailure(CameraAgent mgr, String info) {
        UsageStatistics.instance().cameraFailure(10000, null, -1, -1);
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Camera reconnection failure:");
        stringBuilder.append(info);
        Log.w(tag, stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("onReconnectionFailure   ");
        stringBuilder.append(info);
        android.util.Log.e("+++===============-----", stringBuilder.toString());
        CameraUtil.showErrorAndFinish(this, R.string.cannot_connect_camera);
    }

    public void onCameraRequested() {
    }

    public void onCameraClosed() {
    }

    public boolean isReleased() {
        return false;
    }

    private void setFilmstripUiVisibility(boolean visible) {
        this.mLightsOutRunnable.run();
        this.mFilmstripCoversPreview = visible;
        updatePreviewVisibility();
    }

    private void hideSessionProgress() {
    }

    private void showSessionProgress(CharSequence message) {
    }

    private void showProcessError(CharSequence message) {
    }

    private void updateSessionProgress(int progress) {
    }

    private void updateSessionProgressText(CharSequence message) {
    }

    @TargetApi(16)
    private void setupNfcBeamPush() {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this.mAppContext);
        if (adapter != null) {
            if (ApiHelper.HAS_SET_BEAM_PUSH_URIS) {
                adapter.setBeamPushUris(null, this);
                adapter.setBeamPushUrisCallback(new CreateBeamUrisCallback() {
                    public Uri[] createBeamUris(NfcEvent event) {
                        return CameraActivity.this.mNfcPushUris;
                    }
                }, this);
                return;
            }
            adapter.setNdefPushMessage(null, this, new Activity[0]);
        }
    }

    public boolean onShareTargetSelected(ShareActionProvider shareActionProvider, Intent intent) {
        return false;
    }

    public Context getAndroidContext() {
        return this.mAppContext;
    }

    public void launchActivityByIntent(Intent intent) {
        this.mResetToPreviewOnResume = false;
        intent.addFlags(524288);
        startActivity(intent);
    }

    public int getCurrentModuleIndex() {
        return this.mCurrentModeIndex;
    }

    public int getCurrentCameraId() {
        return this.mCameraController.getCurrentCameraId();
    }

    public String getModuleScope() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(MODULE_SCOPE_PREFIX);
        stringBuilder.append(this.mCurrentModule.getModuleStringIdentifier());
        return stringBuilder.toString();
    }

    public String getCameraScope() {
        int currentCameraId = getCurrentCameraId();
        if (currentCameraId < 0) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getting camera scope with no open camera, using id: ");
            stringBuilder.append(currentCameraId);
            Log.w(tag, stringBuilder.toString());
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(CAMERA_SCOPE_PREFIX);
        stringBuilder2.append(Integer.toString(currentCameraId));
        return stringBuilder2.toString();
    }

    public ModuleController getCurrentModuleController() {
        return this.mCurrentModule;
    }

    public int getQuickSwitchToModuleId(int currentModuleIndex) {
        return this.mModuleManager.getQuickSwitchToModuleId(currentModuleIndex, this.mSettingsManager, this.mAppContext);
    }

    public SurfaceTexture getPreviewBuffer() {
        return null;
    }

    public void onPreviewReadyToStart() {
        this.mCameraAppUI.onPreviewReadyToStart();
    }

    public void onPreviewStarted() {
        if (!this.mCurrentModule.onGLRenderEnable()) {
            this.mCameraAppUI.onPreviewStarted();
        } else if (this.mCurrentModule instanceof VideoModule) {
            this.mCameraAppUI.onGLSurfacePreviewStart();
        } else if (this.mCurrentModule instanceof PhotoModule) {
            this.mCurrentModule.setPreviewBytesBack(this.mCameraAppUI.getPreviewCallback());
        } else if (this.mCurrentModule instanceof LiveBokehModule) {
            this.mCurrentModule.setPreviewBytesBack(this.mCameraAppUI.getPreviewCallback());
        }
    }

    public void addPreviewAreaSizeChangedListener(PreviewAreaChangedListener listener) {
        this.mCameraAppUI.addPreviewAreaChangedListener(listener);
    }

    public void removePreviewAreaSizeChangedListener(PreviewAreaChangedListener listener) {
        this.mCameraAppUI.removePreviewAreaChangedListener(listener);
    }

    public void setupOneShotPreviewListener() {
        this.mCameraController.setOneShotPreviewCallback(this.mMainHandler, new CameraPreviewDataCallback() {
            public void onPreviewFrame(byte[] data, CameraProxy camera) {
                CameraActivity.this.mCurrentModule.onPreviewInitialDataReceived();
                CameraActivity.this.mCameraAppUI.onNewPreviewFrame();
            }
        });
    }

    public void updatePreviewAspectRatio(float aspectRatio) {
        this.mCameraAppUI.updatePreviewAspectRatio(aspectRatio);
    }

    public void updatePreviewTransformFullscreen(Matrix matrix, float aspectRatio) {
        this.mCameraAppUI.updatePreviewTransformFullscreen(matrix, aspectRatio);
    }

    public RectF getFullscreenRect() {
        return this.mCameraAppUI.getFullscreenRect();
    }

    public void updatePreviewTransform(Matrix matrix) {
        this.mCameraAppUI.updatePreviewTransform(matrix);
    }

    public void setPreviewStatusListener(PreviewStatusListener previewStatusListener) {
        this.mCameraAppUI.setPreviewStatusListener(previewStatusListener);
    }

    public FrameLayout getModuleLayoutRoot() {
        return this.mCameraAppUI.getModuleRootView();
    }

    public void setShutterEventsListener(ShutterEventsListener listener) {
    }

    public void setShutterEnabled(boolean enabled) {
        this.mCameraAppUI.setShutterButtonEnabled(enabled);
    }

    public void setShutterEnabledWithNormalAppearence(boolean enabled) {
        this.mCameraAppUI.setShutterButtonEnabled(enabled, false);
    }

    public void setShutterPress(boolean press) {
        this.mCameraAppUI.setShutterButtonPress(press);
    }

    public void setShutterButtonLongClickable(boolean enabled) {
        this.mCameraAppUI.setShutterButtonLongClickable(enabled);
    }

    public boolean isShutterEnabled() {
        return this.mCameraAppUI.isShutterButtonEnabled();
    }

    public void startPreCaptureAnimation(boolean shortFlash) {
        this.mCameraAppUI.startPreCaptureAnimation(shortFlash);
    }

    public void startPreCaptureAnimation() {
        this.mCameraAppUI.startPreCaptureAnimation(false);
    }

    public void cancelPreCaptureAnimation() {
    }

    public void startPostCaptureAnimation() {
    }

    public void startPostCaptureAnimation(Bitmap thumbnail) {
    }

    public void cancelPostCaptureAnimation() {
    }

    public OrientationManager getOrientationManager() {
        return this.mOrientationManager;
    }

    public LocationManager getLocationManager() {
        return this.mLocationManager;
    }

    public void lockOrientation() {
        if (this.mOrientationManager != null) {
            this.mOrientationManager.lockOrientation();
        }
    }

    public void unlockOrientation() {
        if (this.mOrientationManager != null) {
            this.mOrientationManager.unlockOrientation();
        }
    }

    private void startPeekAnimation(final LocalData data, String accessibilityString) {
        Log.d(TAG, "PeekAnimation enter thumbnail KPI");
        if (this.mPeekAnimationHandler == null) {
            this.mThumbUpdateRunnable = new Runnable() {
                public void run() {
                    CameraActivity.this.mPeekAnimationHandler.startDecodingJob(data, new Callback<Bitmap>() {
                        public void onCallback(Bitmap result) {
                            Tag access$1000 = CameraActivity.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(" cached last thumb update for ");
                            stringBuilder.append(data.getUri());
                            Log.w(access$1000, stringBuilder.toString());
                            CameraActivity.this.mCameraAppUI.updatePeekThumbContent(Thumbnail.createThumbnail(data.getUri(), result, data.getRotation()));
                        }
                    });
                }
            };
        }
        if (!this.mFilmstripVisible && this.mPeekAnimationHandler != null) {
            int dataType = data.getLocalDataType();
            if (dataType == 3 || dataType == 5 || dataType == 4) {
                this.mPeekAnimationHandler.startDecodingJob(data, new Callback<Bitmap>() {
                    public void onCallback(Bitmap result) {
                        int rotation = data.getRotation();
                        if (data.getLocalDataType() == 3) {
                            rotation = 0;
                        }
                        Thumbnail lastThumb = Thumbnail.createThumbnail(data.getUri(), result, rotation);
                        if (CameraActivity.this.mPaused) {
                            CameraActivity.this.mCameraAppUI.updatePeekThumbContent(lastThumb);
                            return;
                        }
                        CameraActivity.this.mCameraAppUI.updatePeekThumbBitmapWithAnimation(lastThumb.getBitmap());
                        CameraActivity.this.mCameraAppUI.updatePeekThumbUri(data.getUri());
                        Log.d(CameraActivity.TAG, "PeekAnimation update thumbnail KPI");
                    }
                });
                Log.d(TAG, "PeekAnimation exit thumbnail KPI");
            }
        }
    }

    public void notifyNewMedia(Uri uri, int action) {
        boolean optimizeCapture = false;
        boolean needAnimation = (action & 1) != 0;
        boolean needUpdateThumb = (action & 2) != 0;
        if ((action & 4) != 0) {
            optimizeCapture = true;
        }
        notifyNewMedia(uri, needAnimation, needUpdateThumb, optimizeCapture);
    }

    public void notifyNewMedia(Uri uri) {
        notifyNewMedia(uri, 3);
    }

    private ArrayList<Uri> getSecureUris() {
        if (this.mSecureUris == null) {
            this.mSecureUris = new ArrayList();
        }
        return this.mSecureUris;
    }

    private void clearSecureUris() {
        if (this.mSecureUris != null) {
            this.mSecureUris.clear();
            this.mSecureUris = null;
        }
    }

    private void notifyNewMedia(Uri uri, boolean needAnimation, boolean needUpdateThumb, boolean optimizeCapture) {
        final Uri uri2 = uri;
        final boolean z = needUpdateThumb;
        final boolean z2 = needAnimation;
        final boolean z3 = optimizeCapture;
        new AsyncTask<Void, Void, LocalData>() {
            /* Access modifiers changed, original: protected|varargs */
            public LocalData doInBackground(Void... params) {
                LocalData newData;
                Log.d(CameraActivity.TAG, "NewMedia enter thumbnail KPI");
                if (CameraActivity.this.isSecureCamera()) {
                    CameraActivity.this.mSecureUris = CameraActivity.this.getSecureUris();
                    CameraActivity.this.mSecureUris.add(uri2);
                }
                String mimeType = CameraActivity.this.getContentResolver().getType(uri2);
                Tag access$1000;
                StringBuilder stringBuilder;
                if (LocalDataUtil.isMimeTypeVideo(mimeType)) {
                    CameraActivity.this.sendBroadcast(new Intent(CameraUtil.ACTION_NEW_VIDEO, uri2));
                    newData = VideoData.fromContentUri(CameraActivity.this.getContentResolver(), uri2);
                    if (newData == null) {
                        access$1000 = CameraActivity.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Can't find video data in content resolver:");
                        stringBuilder.append(uri2);
                        Log.e(access$1000, stringBuilder.toString());
                        return null;
                    }
                } else if (LocalDataUtil.isMimeTypeImage(mimeType)) {
                    CameraUtil.broadcastNewPicture(CameraActivity.this.mAppContext, uri2);
                    newData = PhotoData.fromContentUri(CameraActivity.this.getContentResolver(), uri2);
                    if (newData == null) {
                        access$1000 = CameraActivity.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Can't find photo data in content resolver:");
                        stringBuilder.append(uri2);
                        Log.e(access$1000, stringBuilder.toString());
                        return null;
                    }
                } else {
                    access$1000 = CameraActivity.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown new media with MIME type:");
                    stringBuilder.append(mimeType);
                    stringBuilder.append(", uri:");
                    stringBuilder.append(uri2);
                    Log.w(access$1000, stringBuilder.toString());
                    return null;
                }
                if (!z) {
                    return null;
                }
                LocalData data = newData;
                MetadataLoader.loadMetadata(CameraActivity.this.getAndroidContext(), data);
                return data;
            }

            /* Access modifiers changed, original: protected */
            public void onPostExecute(final LocalData data) {
                if (data != null) {
                    Tag access$1000 = CameraActivity.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("get content id = ");
                    stringBuilder.append(data.getContentId());
                    stringBuilder.append(", date = ");
                    stringBuilder.append(data.getDateTaken());
                    Log.d(access$1000, stringBuilder.toString());
                    if (z2) {
                        if (z3) {
                            String mimeType = CameraActivity.this.getContentResolver().getType(data.getUri());
                            if (LocalDataUtil.isMimeTypeImage(mimeType)) {
                                CameraActivity.this.mCameraAppUI.updatePeekThumbUri(data.getUri());
                            } else if (LocalDataUtil.isMimeTypeVideo(mimeType)) {
                                CameraActivity.this.startPeekAnimation(data, CameraActivity.this.mCurrentModule != null ? CameraActivity.this.mCurrentModule.getPeekAccessibilityString() : "");
                            } else {
                                Tag access$10002 = CameraActivity.TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Unknown new media with MIME type:");
                                stringBuilder2.append(mimeType);
                                Log.w(access$10002, stringBuilder2.toString());
                            }
                        } else {
                            CameraActivity.this.startPeekAnimation(data, CameraActivity.this.mCurrentModule != null ? CameraActivity.this.mCurrentModule.getPeekAccessibilityString() : "");
                            Log.d(CameraActivity.TAG, "NewMedia exit thumbnail KPI");
                        }
                        return;
                    }
                    ProcessingMedia pm = ProcessingMediaManager.getInstance(CameraActivity.this.mAppContext).getById(data.getContentId());
                    if (pm != null) {
                        Log.d(CameraActivity.TAG, pm.toString());
                        CameraActivity.this.mCameraAppUI.updatePeekThumbUri(data.getUri());
                        ProcessingMediaManager.getInstance(CameraActivity.this.mAppContext).removeById(data.getContentId());
                    } else {
                        CameraActivity.this.mThumbUpdateRunnable = new Runnable() {
                            public void run() {
                                CameraActivity.this.mPeekAnimationHandler.startDecodingJob(data, new Callback<Bitmap>() {
                                    public void onCallback(Bitmap result) {
                                        CameraActivity.this.mCameraAppUI.updatePeekThumbContent(Thumbnail.createThumbnail(data.getUri(), result, 0));
                                    }
                                });
                            }
                        };
                        if (CameraActivity.this.mPeekAnimationHandler != null) {
                            CameraActivity.this.mThumbUpdateRunnable.run();
                            CameraActivity.this.mThumbUpdateRunnable = null;
                        }
                    }
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    public void enableKeepScreenOn(boolean enabled) {
        if (!this.mPaused) {
            this.mKeepScreenOn = enabled;
            if (this.mKeepScreenOn) {
                this.mMainHandler.removeMessages(2);
                getWindow().addFlags(128);
            } else {
                keepScreenOnForAWhile();
            }
        }
    }

    public CameraProvider getCameraProvider() {
        return this.mCameraController;
    }

    public OneCameraManager getCameraManager() {
        return this.mCameraManager;
    }

    private boolean isCaptureSecureIntent() {
        if (ACTION_IMAGE_CAPTURE_SECURE.equals(getIntent().getAction())) {
            return true;
        }
        return false;
    }

    private boolean isCaptureIntent() {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Action = ");
        stringBuilder.append(getIntent().getAction());
        Log.d(tag, stringBuilder.toString());
        if ("android.media.action.VIDEO_CAPTURE".equals(getIntent().getAction()) || "android.media.action.IMAGE_CAPTURE".equals(getIntent().getAction()) || ACTION_IMAGE_CAPTURE_SECURE.equals(getIntent().getAction())) {
            return true;
        }
        return false;
    }

    private boolean isVideoCaptureIntent() {
        return "android.media.action.VIDEO_CAPTURE".equals(getIntent().getAction());
    }

    private boolean isPhotoCaptureIntent() {
        return "android.media.action.IMAGE_CAPTURE".equals(getIntent().getAction());
    }

    public void finishAndQuitProcess() {
        finish();
        Process.killProcess(Process.myPid());
    }

    public Integer lockModuleSelection() {
        Log.v(TAG, "lock moduleSelection ");
        return this.mModeStripView.lockView();
    }

    public boolean unlockModuleSelection(Integer token) {
        Log.v(TAG, "unlock moduleSelection ");
        return this.mModeStripView.unLockView(token);
    }

    public boolean onPeekThumbClicked(Uri uri) {
        Log.v(TAG, "onPeekThumbClicked");
        int j = 0;
        if (getButtonManager().isMoreOptionsWrapperShow()) {
            getButtonManager().hideMoreOptionsWrapper();
            return false;
        }
        this.mResetToPreviewOnResume = false;
        List<ProcessingMedia> pm = ProcessingMediaManager.getInstance(this.mAppContext).getProcessingMedia();
        boolean pmEmpty = pm.isEmpty();
        String cameraSavePath = this.mSettingsManager.getString(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_SAVEPATH, "0");
        if (uri == null && pmEmpty) {
            return false;
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("A CHANCE, uri = ");
        stringBuilder.append(uri);
        stringBuilder.append(", pmEmpty = ");
        stringBuilder.append(pmEmpty);
        Log.d(tag, stringBuilder.toString());
        long lastProcessingId = pmEmpty ? 0 : ((ProcessingMedia) pm.get(pm.size() - 1)).getMediaStoreId();
        Intent viewIntent = new Intent();
        viewIntent.setAction(CameraUtil.REVIEW_ACTION);
        viewIntent.putExtra("camera_album", true);
        viewIntent.putExtra("secure_camera", this.mSecureCamera);
        viewIntent.setPackage("com.google.android.apps.photos");
        if (this.mSecureCamera) {
            this.mSecureUris = getSecureUris();
            int processingItemsSize = pmEmpty ? 0 : pm.size();
            long[] securePhotoIds = new long[(this.mSecureUris.size() + processingItemsSize)];
            for (int i = 0; i < this.mSecureUris.size(); i++) {
                securePhotoIds[i] = ContentUris.parseId((Uri) this.mSecureUris.get(i));
            }
            while (j < processingItemsSize) {
                securePhotoIds[this.mSecureUris.size() + j] = ((ProcessingMedia) pm.get(j)).getMediaStoreId();
                j++;
            }
            viewIntent.putExtra("com.google.android.apps.photos.api.secure_mode", true);
            viewIntent.putExtra("com.google.android.apps.photos.api.secure_mode_ids", securePhotoIds);
            this.mKeepSecureModule = true;
        }
        uri = setViewIntentData(viewIntent, uri, pmEmpty, lastProcessingId);
        try {
            startActivity(viewIntent);
            getLockEventListener().onModeSwitching();
        } catch (ActivityNotFoundException e) {
            tag = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to start intent=");
            stringBuilder.append(viewIntent);
            stringBuilder.append(": ");
            stringBuilder.append(e);
            Log.w(tag, stringBuilder.toString());
            try {
                startActivity(new Intent("android.intent.action.VIEW", uri));
                getLockEventListener().onModeSwitching();
            } catch (Exception ex) {
                Tag tag2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("No Activity could be found to open image or video");
                stringBuilder2.append(ex);
                Log.w(tag2, stringBuilder2.toString());
            }
        }
        return true;
    }

    private Uri setViewIntentData(Intent intent, Uri uri, boolean pmEmpty, long id) {
        if (uri != null) {
            Log.d(TAG, "setViewIntentData not processing view mode");
            intent.setData(uri);
            return uri;
        } else if (pmEmpty) {
            return uri;
        } else {
            Log.d(TAG, "setViewIntentData processing view mode");
            uri = HmdThumbnailProvider.PLACE_HOLDER_URI.buildUpon().appendPath(String.valueOf(id)).build();
            intent.setDataAndType(uri, "image/jpeg");
            intent.putExtra(PhotosOemApi.ACTION_REVIEW_PROCESSING_URI_INTENT_EXTRA, PhotosOemApi.getQueryProcessingUri(this.mAppContext, id));
            return uri;
        }
    }

    public Uri getPeekThumbUri() {
        if (this.mCameraAppUI == null) {
            return null;
        }
        return this.mCameraAppUI.getPeekThumbUri();
    }

    public void addRotatableToListenerPool(RotateEntity rotatableEntity) {
        if (!this.mListeningRotatableMap.containsKey(Integer.valueOf(rotatableEntity.rotatableHashCode))) {
            this.mListeningRotatableMap.put(Integer.valueOf(rotatableEntity.rotatableHashCode), rotatableEntity);
        }
    }

    public void addLockableToListenerPool(Lockable lockable) {
        if (this.mCameraAppUI != null) {
            this.mCameraAppUI.addLockableToListenerPool(lockable);
        }
    }

    public void removeLockableFromListenerPool(Lockable lockable) {
        if (this.mCameraAppUI != null) {
            this.mCameraAppUI.removeLockableFromListenerPool(lockable);
        }
    }

    public void removeRotatableFromListenerPool(int hashCode) {
        this.mListeningRotatableMap.remove(Integer.valueOf(hashCode));
    }

    public void onNewIntentTasks(Intent intent) {
        String action = intent.getAction();
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onNewIntent ");
        stringBuilder.append(action);
        Log.v(tag, stringBuilder.toString());
        handleActions(intent);
        if (ACTION_PRO_CAMERA.equals(action)) {
            if (this.mCameraAppUI != null) {
                if (getCurrentModuleIndex() == getResources().getInteger(R.integer.camera_mode_pro)) {
                    this.mCameraAppUI.setNeedShowArc(true);
                } else {
                    this.mCameraAppUI.slipUpShutterButton();
                }
            }
            return;
        }
        if (INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE.equals(action) || ACTION_IMAGE_CAPTURE_SECURE.equals(action)) {
            this.mSecureCamera = true;
        } else {
            this.mSecureCamera = intent.getBooleanExtra("secure_camera", false);
        }
        if (this.mSecureCamera) {
            Keys.resetSecureModuleIndex(this.mSettingsManager, getResources().getInteger(R.integer.camera_mode_photo));
        }
        updateCameraForFunc();
        int modeIndex = getModeIndex();
        Tag tag2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("last mode is ");
        stringBuilder2.append(modeIndex);
        stringBuilder2.append(" currentMode index is ");
        stringBuilder2.append(this.mCurrentModeIndex);
        Log.w(tag2, stringBuilder2.toString());
        if (!this.mSecureCamera && !isCaptureIntent() && modeIndex != this.mCurrentModeIndex) {
            this.mModeSelectingOnStart = true;
            switchToMode(modeIndex);
        } else if (this.mSecureCamera) {
            clearSecureUris();
            this.mCameraAppUI.updatePeekThumbContent(null);
        } else {
            long curr = System.currentTimeMillis();
            if (curr - this.lastStopStamp > 50000 && modeIndex != this.mCurrentModeIndex) {
                switchToMode(modeIndex);
                this.mModeSelectingOnStart = true;
            }
            this.lastStopStamp = curr;
        }
    }

    public boolean isUserFrontCamera() {
        return this.mUseFrontCamera;
    }

    public boolean isDelayTake() {
        return this.mDelayTake;
    }

    public boolean islaunchFromAssistant() {
        return this.mIsLaunchFromAssistant;
    }

    public void resetAssistantStatus() {
        this.mIsLaunchFromAssistant = false;
        this.mUseFrontCamera = false;
        this.mDelayTake = false;
    }

    public void resetMotionStatus() {
        if (!this.mLastMotionState) {
            getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_MOTION, false);
        }
    }

    private void handleActions(Intent intent) {
        String action = intent.getAction();
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("action = ");
        stringBuilder.append(action);
        Log.d(tag, stringBuilder.toString());
        if (!((!"android.media.action.STILL_IMAGE_CAMERA".equals(action) && !"android.media.action.VIDEO_CAMERA".equals(action)) || intent.getExtras() == null || intent.getExtras().containsKey("com.android.systemui.camera_launch_source")) || CAMERA_MOTION_PICTURE.equals(action)) {
            Bundle bundle = intent.getExtras();
            if (!(bundle == null || bundle.keySet() == null)) {
                for (String key : bundle.keySet()) {
                    Tag tag2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("key = ");
                    stringBuilder2.append(key);
                    stringBuilder2.append(" value = ");
                    stringBuilder2.append(bundle.get(key));
                    Log.d(tag2, stringBuilder2.toString());
                }
                if (bundle.keySet().contains(USE_FRONT_CAMERA)) {
                    this.mUseFrontCamera = true;
                } else {
                    this.mUseFrontCamera = false;
                }
                if (bundle.keySet().contains(TIMER_DURATION_SECONDS)) {
                    this.mDelayTake = true;
                } else {
                    this.mDelayTake = false;
                }
            }
            if ("android.media.action.STILL_IMAGE_CAMERA".equals(action)) {
                getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_MOTION, false);
            }
            if (CAMERA_MOTION_PICTURE.equals(action)) {
                this.mLastMotionState = Keys.isMotionOn(getSettingsManager());
                getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_MOTION, true);
                Tag tag3 = TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("mLastMotionState = ");
                stringBuilder3.append(this.mLastMotionState);
                Log.d(tag3, stringBuilder3.toString());
            }
            this.mIsLaunchFromAssistant = true;
            return;
        }
        this.mIsLaunchFromAssistant = false;
    }

    private void firstRun() {
        this.mSharedPreferences = getSharedPreferences(PREF_CAMERAACTIVITY, 0);
        if (Boolean.valueOf(this.mSharedPreferences.getBoolean(Keys.KEY_TIPS, true)).booleanValue()) {
            Builder alertDialog = new Builder(this);
            alertDialog.setTitle(R.string.first_time_launch_camera_dialog_title);
            alertDialog.setMessage(getResources().getString(R.string.first_time_launch_camera_dialog_content));
            alertDialog.setCancelable(false);
            alertDialog.setPositiveButton(getResources().getString(R.string.first_time_launch_camera_dialog_agree), new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    CameraActivity.this.mSharedPreferences.edit().putBoolean(Keys.KEY_TIPS, false).apply();
                }
            });
            alertDialog.setNegativeButton(getResources().getString(R.string.first_time_launch_camera_dialog_exit), new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    CameraActivity.this.mSharedPreferences.edit().putBoolean(Keys.KEY_TIPS, false).apply();
                    CameraActivity.this.finish();
                }
            });
            alertDialog.show();
            this.mSharedPreferences.edit().putBoolean(Keys.KEY_TIPS, false).apply();
        }
    }

    public void onCreateTasks(Bundle state) {
        CameraApi cameraApi;
        if (isInMultiWindowMode()) {
            finish();
            System.exit(0);
        }
        Util.TimesofLivebokeh = 0;
        Log.w(TAG, "KPI onCreateTasks");
        this.mAppContext = getApplication().getBaseContext();
        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
            public void run() {
                GcamHelper.init(CameraActivity.this.getContentResolver());
                BlurUtil.initialize(CameraActivity.this.mAppContext);
            }
        });
        if (!StorageUtils.ExternalFileDirIsExist(this.mAppContext)) {
            requestAccessPer(this.mAppContext);
        }
        boolean criticalPermsGranted = checkCriticalPermissions();
        this.mTelephonyManager = (TelephonyManager) getSystemService("phone");
        this.mOnCreateTime = System.currentTimeMillis();
        this.mSoundPlayer = new SoundPlayer(this.mAppContext);
        this.mMainHandler = new MainHandler(this, getMainLooper());
        Context context = this.mAppContext;
        Handler handler = this.mMainHandler;
        CameraAgent androidCameraAgent = CameraAgentFactory.getAndroidCameraAgent(this.mAppContext, CameraApi.API_1);
        Context context2 = this.mAppContext;
        if (GservicesHelper.useCamera2ApiThroughPortabilityLayer(this.mAppContext)) {
            cameraApi = CameraApi.AUTO;
        } else {
            cameraApi = CameraApi.API_1;
        }
        this.mCameraController = new CameraController(context, this, handler, androidCameraAgent, CameraAgentFactory.getAndroidCameraAgent(context2, cameraApi));
        this.mCameraController.setCameraExceptionHandler(new CameraExceptionHandler(this.mCameraExceptionCallback, this.mMainHandler));
        SharedPreferences pref = getSharedPreferences(this.nameMms, 4);
        Editor editor = pref.edit();
        editor.remove("sys.camera.taking_movie");
        editor.putString("sys.camera.taking_movie", "0");
        editor.apply();
        IntentFilter exitFilter = new IntentFilter(EXIT_CAMERA_ACTION);
        this.mExitBroadCast = new ExitBroadCast();
        registerReceiver(this.mExitBroadCast, exitFilter);
        this.mModuleManager = new ModuleManagerImpl();
        if (isPhotoCaptureIntent()) {
            ModulesInfo.setupPhotoCaptureIntentModules(this.mAppContext, this.mModuleManager);
        } else if (isVideoCaptureIntent()) {
            ModulesInfo.setupVideoCaptureIntentModules(this.mAppContext, this.mModuleManager);
        } else {
            ModulesInfo.setupModules(this.mAppContext, this.mModuleManager);
        }
        this.mSettingsManager = getServices().getSettingsManager();
        new AppUpgrader(this).upgrade(this.mSettingsManager);
        Keys.setDefaults(this.mSettingsManager, this.mAppContext);
        Keys.setNewLaunchingForMicroguide(this.mSettingsManager, true);
        Keys.setNewLaunchingForMicrotip(this.mSettingsManager, true);
        Keys.setNewLaunchingForHdrtoast(this.mSettingsManager, true);
        Keys.setNewLaunchingForNighttoast(this.mSettingsManager, true);
        Keys.resetSecureModuleIndex(this.mSettingsManager, getResources().getInteger(R.integer.camera_mode_photo));
        updateCameraForFunc();
        String action = getIntent().getAction();
        handleActions(getIntent());
        int startIndex = getModeDefaultIndex();
        ModuleAgent agent = this.mModuleManager.getModuleAgent(startIndex);
        if (!(isCaptureIntent() || agent == null || (agent.needAddToStrip() && startIndex == agent.getModuleId()))) {
            int photoIndex = getResources().getInteger(R.integer.camera_mode_photo);
            this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_STARTUP_MODULE_INDEX, photoIndex);
            startIndex = photoIndex;
        }
        setModuleFromModeIndex(startIndex);
        if (this.mCurrentModule != null) {
            this.mCurrentModule.hardResetSettings(this.mSettingsManager);
        }
        boolean locationPrompt = this.mSettingsManager.isSet(SettingsManager.SCOPE_GLOBAL, Keys.KEY_RECORD_LOCATION) ^ true;
        getWindow().requestFeature(8);
        getWindow().setNavigationBarColor(0);
        setContentView(R.layout.activity_main);
        if (ApiHelper.HAS_ROTATION_ANIMATION) {
            setRotationAnimation();
        }
        this.mModeStripView = (StereoModeStripView) findViewById(R.id.mode_strip_view);
        this.mModeStripView.init(this.mModuleManager);
        int initialModeIndex = getModeDefaultIndex();
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("expected mode index is ");
        stringBuilder.append(initialModeIndex);
        Log.v(tag, stringBuilder.toString());
        this.mModeStripView.setCurrentModeWithModeIndex(initialModeIndex);
        Intent intent = getIntent();
        if (INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE.equals(action) || ACTION_IMAGE_CAPTURE_SECURE.equals(action)) {
            this.mSecureCamera = true;
        } else {
            this.mSecureCamera = intent.getBooleanExtra("secure_camera", false);
        }
        if (this.mSecureCamera) {
            Window win = getWindow();
            LayoutParams params = win.getAttributes();
            params.flags |= 524288;
            win.setAttributes(params);
            registerReceiver(this.mShutdownReceiver, new IntentFilter("android.intent.action.SCREEN_OFF"));
            registerReceiver(this.mShutdownReceiver, new IntentFilter("android.intent.action.USER_PRESENT"));
        }
        this.mMainActivityLayout = (MainActivityLayout) findViewById(R.id.activity_root_view);
        this.mCameraAppUI = new CameraAppUI(this, this.mMainActivityLayout, isCaptureIntent(), isCaptureSecureIntent());
        CameraPerformanceTracker.onEvent(0);
        if (!Glide.isSetup()) {
            Glide.setup(new GlideBuilder(getAndroidContext()).setResizeService(new FifoPriorityThreadPoolExecutor(2)));
            Glide.get(getAndroidContext()).setMemoryCategory(MemoryCategory.HIGH);
        }
        this.mLocaleIdArray = LocalePicker.getSupportedLocales(this);
        createSimStateReceiver();
        createMccChangedReceiver();
        IntentFilter filter_media_action = new IntentFilter("android.intent.action.MEDIA_UNMOUNTED");
        filter_media_action.addAction("android.intent.action.MEDIA_EJECT");
        filter_media_action.addAction("android.intent.action.MEDIA_MOUNTED");
        filter_media_action.addAction("android.intent.action.MEDIA_SCANNER_STARTED");
        filter_media_action.addAction("android.intent.action.MEDIA_SCANNER_FINISHED");
        filter_media_action.addDataScheme("file");
        registerReceiver(this.mMediaActionReceiver, filter_media_action);
        updateAntibandingValue();
        getServices().getCaptureSessionManager().addSessionListener(this.mSessionListener);
        this.mPanoramaViewHelper = new PanoramaViewHelper(this);
        this.mPanoramaViewHelper.onCreate();
        Tag tag2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Intent action = ");
        stringBuilder2.append(intent.getAction());
        Log.i(tag2, stringBuilder2.toString());
        if ("android.intent.action.MAIN".equals(intent.getAction()) && CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_HELP_TIP_TUTORIAL, false)) {
            this.mHelpTipsManager = new HelpTipsManager(this);
            this.mHelpTipsManager.startAlarmTask();
        }
        this.mCameraAppUI.prepareModuleUI();
        this.mLocationManager = new LocationManager(this.mAppContext);
        if (criticalPermsGranted) {
            if (!locationPrompt) {
                this.mRequestPermissionsFinished = true;
            } else if (checkNonCriticalPermissions()) {
                this.mRequestPermissionsFinished = true;
            }
        }
        this.mOrientationManager = new OrientationManagerImpl(this);
        this.mOrientationManager.addOnOrientationChangeListener(this.mMainHandler, this);
        this.mCurrentModule.init(this, isSecureCamera(), isCaptureIntent());
        if (this.mCameraReadyListener != null) {
            this.mCameraReadyListener.onCameraReady();
        }
        if (this.mSecureCamera) {
            ImageView v = (ImageView) getLayoutInflater().inflate(R.layout.secure_album_placeholder, null);
            v.setTag(R.id.mediadata_tag_viewtype, Integer.valueOf(LocalDataViewType.SECURE_ALBUM_PLACEHOLDER.ordinal()));
            v.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    UsageStatistics.instance().changeScreen(10000, Integer.valueOf(10000));
                    CameraActivity.this.finish();
                }
            });
            v.setContentDescription(getString(R.string.accessibility_unlock_to_camera));
        } else if (!isCaptureIntent() && isExternalStorageAvailable()) {
            onLastMediaDataUpdated();
        }
        this.mLocalImagesObserver = new LocalMediaObserver();
        this.mLocalVideosObserver = new LocalMediaObserver();
        getContentResolver().registerContentObserver(Media.EXTERNAL_CONTENT_URI, true, this.mLocalImagesObserver);
        getContentResolver().registerContentObserver(Video.Media.EXTERNAL_CONTENT_URI, true, this.mLocalVideosObserver);
        this.mMemoryManager = getServices().getMemoryManager();
        AsyncTask.THREAD_POOL_EXECUTOR.execute(new Runnable() {
            public void run() {
                UsageStatistics.instance().reportMemoryConsumed(CameraActivity.this.mMemoryManager.queryMemory(), "launch");
            }
        });
        this.mMotionManager = getServices().getMotionManager();
        this.mPhoneStateListener = new PhoneStateListener() {
            public void onCallStateChanged(int state, String incomingNumber) {
                if (state == 0) {
                    CameraActivity.this.mDuringCall = false;
                    CameraActivity.this.mCameraAppUI.setmVideoShuttterBottondisable(false);
                    CameraActivity.this.mCameraAppUI.setCalldisable(false);
                    CameraActivity.this.mCameraAppUI.resetAlpha(false);
                } else if (state == 1 || state == 2) {
                    CameraActivity.this.mDuringCall = true;
                    CameraActivity.this.mCameraAppUI.setmVideoShuttterBottondisable(true);
                    if (CameraActivity.this.getModeIndex() == 10 || (CameraActivity.this.getModeIndex() == 8 && !CameraActivity.this.isRecording())) {
                        CameraActivity.this.mCameraAppUI.setCalldisable(true);
                    }
                } else {
                    CameraActivity.this.mDuringCall = false;
                }
            }
        };
        registerPhoneStateListener();
        registerReceiver(this.broadcastReceiver, new IntentFilter(PhotoModule.action));
        if (ACTION_PRO_CAMERA.equals(getIntent().getAction()) && this.mCameraAppUI != null) {
            if (getCurrentModuleIndex() == getResources().getInteger(R.integer.camera_mode_pro)) {
                this.mCameraAppUI.setNeedShowArc(true);
            } else {
                this.mCameraAppUI.slipUpShutterButton();
            }
        }
        BeautifyHandler.init(this);
    }

    private void updateAntibandingValue() {
        Tag tag;
        StringBuilder stringBuilder;
        Locale locale = fetchUpdateSimLocale();
        String hz_50 = getString(R.string.pref_camera_antibanding_value_1);
        String hz_60 = getString(R.string.pref_camera_antibanding_value_2);
        String auto = getString(R.string.pref_camera_antibanding_value_3);
        String antibandingValue = auto;
        if (locale != null) {
            tag = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("country code : ");
            stringBuilder.append(locale.getCountry());
            Log.d(tag, stringBuilder.toString());
            String country = locale.getCountry();
            Object obj = -1;
            switch (country.hashCode()) {
                case 2099:
                    if (country.equals(AT)) {
                        obj = 22;
                        break;
                    }
                    break;
                case 2100:
                    if (country.equals(AU)) {
                        obj = 14;
                        break;
                    }
                    break;
                case 2142:
                    if (country.equals(CA)) {
                        obj = 4;
                        break;
                    }
                    break;
                case 2155:
                    if (country.equals(CN)) {
                        obj = 8;
                        break;
                    }
                    break;
                case 2177:
                    if (country.equals(DE)) {
                        obj = 23;
                        break;
                    }
                    break;
                case 2222:
                    if (country.equals(ES)) {
                        obj = 19;
                        break;
                    }
                    break;
                case 2252:
                    if (country.equals(FR)) {
                        obj = 17;
                        break;
                    }
                    break;
                case 2267:
                    if (country.equals(GB)) {
                        obj = 16;
                        break;
                    }
                    break;
                case 2283:
                    if (country.equals(GR)) {
                        obj = 20;
                        break;
                    }
                    break;
                case 2307:
                    if (country.equals(HK)) {
                        obj = 6;
                        break;
                    }
                    break;
                case 2331:
                    if (country.equals(ID)) {
                        obj = 12;
                        break;
                    }
                    break;
                case 2341:
                    if (country.equals(IN)) {
                        obj = 13;
                        break;
                    }
                    break;
                case 2347:
                    if (country.equals(IT)) {
                        obj = 18;
                        break;
                    }
                    break;
                case 2374:
                    if (country.equals(JP)) {
                        obj = 27;
                        break;
                    }
                    break;
                case 2407:
                    if (country.equals(KR)) {
                        obj = null;
                        break;
                    }
                    break;
                case 2466:
                    if (country.equals(MO)) {
                        obj = 7;
                        break;
                    }
                    break;
                case 2475:
                    if (country.equals(MX)) {
                        obj = 5;
                        break;
                    }
                    break;
                case 2476:
                    if (country.equals(MY)) {
                        obj = 10;
                        break;
                    }
                    break;
                case 2494:
                    if (country.equals(NL)) {
                        obj = 24;
                        break;
                    }
                    break;
                case 2497:
                    if (country.equals(NO)) {
                        obj = 25;
                        break;
                    }
                    break;
                case 2508:
                    if (country.equals(NZ)) {
                        obj = 15;
                        break;
                    }
                    break;
                case 2552:
                    if (country.equals(PH)) {
                        obj = 2;
                        break;
                    }
                    break;
                case 2627:
                    if (country.equals(RU)) {
                        obj = 26;
                        break;
                    }
                    break;
                case 2642:
                    if (country.equals(SE)) {
                        obj = 21;
                        break;
                    }
                    break;
                case 2644:
                    if (country.equals(SG)) {
                        obj = 11;
                        break;
                    }
                    break;
                case 2676:
                    if (country.equals(TH)) {
                        obj = 9;
                        break;
                    }
                    break;
                case 2691:
                    if (country.equals(TW)) {
                        obj = 1;
                        break;
                    }
                    break;
                case 2718:
                    if (country.equals(US)) {
                        obj = 3;
                        break;
                    }
                    break;
            }
            switch (obj) {
                case null:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                    antibandingValue = hz_60;
                    break;
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                case 25:
                case 26:
                    antibandingValue = hz_50;
                    break;
                case 27:
                    antibandingValue = auto;
                    break;
                default:
                    antibandingValue = auto;
                    break;
            }
        }
        tag = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("set antibandingValue = ");
        stringBuilder.append(antibandingValue);
        Log.d(tag, stringBuilder.toString());
        this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_ANTIBANDING_VALUE, antibandingValue);
    }

    private void createMccChangedReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_MCC_CHANGED);
        registerReceiver(this.mMccChangedReceiver, filter);
    }

    private void releaseMccChangedReceiver() {
        unregisterReceiver(this.mMccChangedReceiver);
    }

    private void createSimStateReceiver() {
        Log.i(TAG, "createSimStateReceiver()");
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SERVICE_STATE_CHANGED);
        registerReceiver(this.mSimStateReceiver, filter);
    }

    private void releaseSimStateReceiver() {
        Log.i(TAG, "releaseSimStateReceiver()");
        unregisterReceiver(this.mSimStateReceiver);
    }

    private Locale fetchUpdateSimLocale() {
        int mcc;
        int mcc_candidate;
        Tag tag;
        StringBuilder stringBuilder;
        Tag tag2;
        Locale locale = null;
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService("phone");
        Context phoneContext = getPhoneContext();
        if (phoneContext == null) {
            phoneContext = this;
        }
        if (null == null) {
            List<SubscriptionInfo> activeSubs = SubscriptionManager.from(this).getActiveSubscriptionInfoList();
            if (!(activeSubs == null || activeSubs.isEmpty())) {
                mcc = ((SubscriptionInfo) activeSubs.get(0)).getMcc();
                mcc_candidate = mcc;
                tag = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("mcc = ");
                stringBuilder.append(mcc);
                Log.d(tag, stringBuilder.toString());
                if (checkIfSetMexicoLocale(mcc_candidate)) {
                    locale = filterSpanishLocal(MccTable.getLocaleFromMcc(phoneContext, mcc, null), mcc_candidate);
                } else if (checkIfSetEnglishUKLocale(mcc_candidate)) {
                    locale = filterEnglishUKLocal(MccTable.getLocaleFromMcc(phoneContext, mcc, null), mcc_candidate);
                } else {
                    locale = MccTable.getLocaleFromMcc(phoneContext, mcc, null);
                }
                tag = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("2. locale = ");
                stringBuilder.append(locale);
                Log.d(tag, stringBuilder.toString());
            }
        }
        if (locale == null) {
            String networkOperator = telephonyManager.getNetworkOperator();
            tag = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("networkOperator = ");
            stringBuilder.append(networkOperator);
            Log.d(tag, stringBuilder.toString());
            if (networkOperator == null || networkOperator.equals("")) {
                tag = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("mSubId = ");
                stringBuilder.append(this.mSubId);
                Log.d(tag, stringBuilder.toString());
                if (this.mSubId != -1) {
                    networkOperator = telephonyManager.getNetworkOperator(this.mSubId);
                    tag = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("networkOperator 2 = ");
                    stringBuilder.append(networkOperator);
                    Log.d(tag, stringBuilder.toString());
                }
            }
            if (!(networkOperator == null || networkOperator.length() == 0)) {
                mcc = Integer.parseInt(networkOperator.substring(0, 3));
                mcc_candidate = mcc;
                tag2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("mcc = ");
                stringBuilder2.append(mcc);
                Log.d(tag2, stringBuilder2.toString());
                if (checkIfSetMexicoLocale(mcc_candidate)) {
                    locale = filterSpanishLocal(MccTable.getLocaleFromMcc(phoneContext, mcc, null), mcc_candidate);
                } else if (checkIfSetEnglishUKLocale(mcc_candidate)) {
                    locale = filterEnglishUKLocal(MccTable.getLocaleFromMcc(phoneContext, mcc, null), mcc_candidate);
                } else {
                    locale = MccTable.getLocaleFromMcc(phoneContext, mcc, null);
                }
                tag2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("3. locale = ");
                stringBuilder2.append(locale);
                Log.d(tag2, stringBuilder2.toString());
            }
        }
        if (locale != null || this.mccFromIntent.length() == 0) {
            return locale;
        }
        mcc = Integer.parseInt(this.mccFromIntent);
        tag2 = TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("add by lujie,mccFromIntent ,mcc ");
        stringBuilder3.append(mcc);
        Log.d(tag2, stringBuilder3.toString());
        return MccTable.getLocaleFromMcc(phoneContext, mcc, null);
    }

    private boolean checkIfSetMexicoLocale(int mcc) {
        boolean isSupportMexicoLocale = false;
        Locale mexLocale = new Locale("es", MX);
        for (String forLanguageTag : this.mLocaleIdArray) {
            if (Locale.forLanguageTag(forLanguageTag).equals(mexLocale)) {
                isSupportMexicoLocale = true;
                break;
            }
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isSupportMexicoLocale = ");
        stringBuilder.append(isSupportMexicoLocale);
        Log.v(tag, stringBuilder.toString());
        if (!isSupportMexicoLocale) {
            return false;
        }
        boolean match = false;
        for (int i : this.MEXICO_LOCALE_MCC_LIST) {
            if (i == mcc) {
                match = true;
                break;
            }
        }
        Tag tag2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("match = ");
        stringBuilder2.append(match);
        Log.v(tag2, stringBuilder2.toString());
        return match;
    }

    private Context getPhoneContext() {
        Context phoneContext = null;
        try {
            phoneContext = createPackageContext("com.android.phone", 3);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("phoneContext = ");
        stringBuilder.append(phoneContext);
        Log.d(tag, stringBuilder.toString());
        return phoneContext;
    }

    private Locale filterSpanishLocal(Locale locale, int mcc_candidate) {
        if (!checkIfSetMexicoLocale(mcc_candidate)) {
            return locale;
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("filterSpanishLocal success:");
        stringBuilder.append(mcc_candidate);
        Log.d(tag, stringBuilder.toString());
        return new Locale("es", MX);
    }

    private boolean checkIfSetEnglishUKLocale(int mcc) {
        boolean isSupportEnglishUKLocale = false;
        Locale ukLocale = new Locale("en", GB);
        for (String forLanguageTag : this.mLocaleIdArray) {
            if (Locale.forLanguageTag(forLanguageTag).equals(ukLocale)) {
                isSupportEnglishUKLocale = true;
                break;
            }
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isSupportEnglishUKLocale = ");
        stringBuilder.append(isSupportEnglishUKLocale);
        Log.v(tag, stringBuilder.toString());
        if (!isSupportEnglishUKLocale) {
            return false;
        }
        boolean match = false;
        for (int i : this.UK_LOCALE_MCC_LIST) {
            if (i == mcc) {
                match = true;
                break;
            }
        }
        Tag tag2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("uk match = ");
        stringBuilder2.append(match);
        Log.v(tag2, stringBuilder2.toString());
        return match;
    }

    private Locale filterEnglishUKLocal(Locale locale, int mcc_candidate) {
        if (!checkIfSetEnglishUKLocale(mcc_candidate)) {
            return locale;
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("filterEnglishUKLocal success:");
        stringBuilder.append(mcc_candidate);
        Log.d(tag, stringBuilder.toString());
        return new Locale("en", GB);
    }

    private void createFloatView() {
        this.btn_floatView = new Button(getApplicationContext());
        this.btn_floatView.setBackgroundResource(R.drawable.whitewrap);
        wm = (WindowManager) getSystemService("window");
        params = new LayoutParams();
        params.type = 2038;
        params.format = 1;
        params.flags = 2621480;
        DisplayMetrics dm = getResources().getDisplayMetrics();
        params.width = dm.widthPixels;
        params.height = dm.heightPixels;
        wm.addView(this.btn_floatView, params);
    }

    private void removeFloatView() {
        if (this.btn_floatView != null && wm != null && this.btn_floatView.isAttachedToWindow()) {
            wm.removeView(this.btn_floatView);
            this.btn_floatView = null;
        }
    }

    private void saveScreenBrightness() {
        try {
            this.mBrightnessValue = System.getInt(getContentResolver(), "screen_brightness");
            this.mBrightnessMode = System.getInt(getContentResolver(), "screen_brightness_mode");
            if (this.mBrightnessMode == 1) {
                System.putInt(getContentResolver(), "screen_brightness_mode", 0);
            }
        } catch (SettingNotFoundException e) {
            e.printStackTrace();
        }
        System.putInt(getContentResolver(), "screen_brightness", 255);
    }

    public void resetScreenBrightness() {
        if (this.mBrightnessMode != -1 && this.mBrightnessValue > 0) {
            System.putInt(getContentResolver(), "screen_brightness_mode", this.mBrightnessMode);
            System.putInt(getContentResolver(), "screen_brightness", this.mBrightnessValue);
            this.mBrightnessMode = -1;
            this.mBrightnessValue = 0;
        }
    }

    private void registerPhoneStateListener() {
        if (this.mTelephonyManager != null) {
            this.mTelephonyManager.listen(this.mPhoneStateListener, 32);
        }
    }

    private void unregisterPhoneStateListener() {
        if (this.mTelephonyManager != null) {
            this.mTelephonyManager.listen(this.mPhoneStateListener, 0);
        }
    }

    public void clearBoost() {
        this.mCameraController.clearBoostPreview();
    }

    private void updateCameraForFunc() {
        Intent intent = getIntent();
        String action = intent.getAction();
        int BACK_CAMERA = Integer.parseInt(getString(R.string.pref_camera_id_entry_back_value));
        int FONT_CAMERA = Integer.parseInt(getString(R.string.pref_camera_id_entry_front_value));
        boolean updateModule = false;
        int requestCameraId = -1;
        if (INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE.equals(action) && intent.getBooleanExtra("func_selfie", false)) {
            requestCameraId = FONT_CAMERA;
        } else if (intent.hasExtra("cameraid")) {
            updateModule = true;
            requestCameraId = intent.getIntExtra("cameraid", -1);
        }
        if (updateModule || requestCameraId == BACK_CAMERA || requestCameraId == FONT_CAMERA) {
            if (this.mSettingsManager == null) {
                this.mSettingsManager = getServices().getSettingsManager();
            }
            if (updateModule) {
                this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_STARTUP_MODULE_INDEX, getResources().getInteger(R.integer.camera_mode_photo));
            }
            if (requestCameraId == BACK_CAMERA || requestCameraId == FONT_CAMERA) {
                this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID, requestCameraId);
            }
        }
    }

    public int getModeDefaultIndex() {
        int photoIndex = getResources().getInteger(R.integer.camera_mode_photo);
        int videoIndex = getResources().getInteger(R.integer.camera_mode_video);
        int videoCaptureIndex = getResources().getInteger(R.integer.camera_mode_video_capture);
        int gcamIndex = getResources().getInteger(R.integer.camera_mode_gcam);
        if ("android.media.action.VIDEO_CAMERA".equals(getIntent().getAction())) {
            return videoIndex;
        }
        if ("android.media.action.VIDEO_CAPTURE".equals(getIntent().getAction())) {
            return videoCaptureIndex;
        }
        if ("android.media.action.IMAGE_CAPTURE".equals(getIntent().getAction())) {
            return photoIndex;
        }
        int modeIndex;
        if ("android.media.action.STILL_IMAGE_CAMERA".equals(getIntent().getAction()) || INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE.equals(getIntent().getAction()) || ACTION_IMAGE_CAPTURE_SECURE.equals(getIntent().getAction()) || CAMERA_MOTION_PICTURE.equals(getIntent().getAction())) {
            modeIndex = this.mSettingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_MODULE_LAST_USED).intValue();
            if (!this.mSettingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_USER_SELECTED_ASPECT_RATIO)) {
                modeIndex = photoIndex;
            }
            return this.mSettingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_SECURE_MODULE_INDEX, Integer.valueOf(modeIndex)).intValue();
        }
        modeIndex = this.mSettingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_STARTUP_MODULE_INDEX).intValue();
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("last startup mode index is ");
        stringBuilder.append(modeIndex);
        Log.v(tag, stringBuilder.toString());
        if ((modeIndex == gcamIndex && !GcamHelper.hasGcamAsSeparateModule()) || modeIndex < 0) {
            modeIndex = photoIndex;
        }
        int BACK_CAMERA = Integer.parseInt(getString(R.string.pref_camera_id_entry_back_value));
        if (getCurrentCameraId() != BACK_CAMERA) {
            this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID, BACK_CAMERA);
        }
        return photoIndex;
    }

    public int getModeIndex() {
        int photoIndex = getResources().getInteger(R.integer.camera_mode_photo);
        int videoIndex = getResources().getInteger(R.integer.camera_mode_video);
        int videoCaptureIndex = getResources().getInteger(R.integer.camera_mode_video_capture);
        int gcamIndex = getResources().getInteger(R.integer.camera_mode_gcam);
        if ("android.media.action.VIDEO_CAMERA".equals(getIntent().getAction())) {
            return videoIndex;
        }
        if ("android.media.action.VIDEO_CAPTURE".equals(getIntent().getAction())) {
            return videoCaptureIndex;
        }
        if ("android.media.action.IMAGE_CAPTURE".equals(getIntent().getAction())) {
            return photoIndex;
        }
        int modeIndex;
        if ("android.media.action.STILL_IMAGE_CAMERA".equals(getIntent().getAction()) || INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE.equals(getIntent().getAction()) || ACTION_IMAGE_CAPTURE_SECURE.equals(getIntent().getAction()) || CAMERA_MOTION_PICTURE.equals(getIntent().getAction())) {
            modeIndex = this.mSettingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_MODULE_LAST_USED).intValue();
            if (!this.mSettingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_USER_SELECTED_ASPECT_RATIO)) {
                modeIndex = photoIndex;
            }
            return this.mSettingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_SECURE_MODULE_INDEX, Integer.valueOf(modeIndex)).intValue();
        }
        modeIndex = this.mSettingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_STARTUP_MODULE_INDEX).intValue();
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("last startup mode index is ");
        stringBuilder.append(modeIndex);
        Log.v(tag, stringBuilder.toString());
        if ((modeIndex != gcamIndex || GcamHelper.hasGcamAsSeparateModule()) && modeIndex >= 0) {
            return modeIndex;
        }
        return photoIndex;
    }

    private void updatePreviewVisibility() {
        if (this.mCurrentModule != null) {
            int visibility = getPreviewVisibility();
            this.mCameraAppUI.onPreviewVisiblityChanged(visibility);
            updatePreviewRendering(visibility);
            this.mCurrentModule.onPreviewVisibilityChanged(visibility);
        }
    }

    private void updatePreviewRendering(int visibility) {
        if (visibility == 2) {
            this.mCameraAppUI.pausePreviewRendering();
        } else {
            this.mCameraAppUI.resumePreviewRendering();
        }
    }

    private int getPreviewVisibility() {
        if (this.mFilmstripCoversPreview) {
            return 2;
        }
        if (this.mModeListVisible) {
            return 1;
        }
        return 0;
    }

    private void setRotationAnimation() {
        Window win = getWindow();
        LayoutParams winParams = win.getAttributes();
        winParams.rotationAnimation = 1;
        win.setAttributes(winParams);
    }

    public void onUserInteraction() {
        super.onUserInteraction();
        if (!isFinishing()) {
            keepScreenOnForAWhile();
        }
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean result = super.dispatchTouchEvent(ev);
        if (ev.getActionMasked() == 0 && this.mPendingDeletion && !this.mIsUndoingDeletion) {
            performDeletion();
        }
        return result;
    }

    public void onPauseTasks() {
        CameraPerformanceTracker.onEvent(1);
        this.mModeSelectingOnStart = false;
        restoreReversible();
        Log.w(TAG, "onPause");
        if (!(!this.mRequestPermissionsFinished || this.mHelpTipsManager == null || this.mHelpTipsManager == null)) {
            this.mHelpTipsManager.pause();
        }
        if (this.mModeStripView != null) {
            this.mModeStripView.pause();
        }
        int photoIndex = getResources().getInteger(R.integer.camera_mode_photo);
        int videoIndex = getResources().getInteger(R.integer.camera_mode_video);
        int videoCaptureIndex = getResources().getInteger(R.integer.camera_mode_video_capture);
        if (!isCaptureIntent()) {
            int modeIndexToSave = this.mCurrentModeIndex;
            this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_STARTUP_MODULE_INDEX, modeIndexToSave);
            this.mKeepSecureModule = true;
            if (this.mKeepSecureModule || this.mSecureFyuseModule) {
                this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_SECURE_MODULE_INDEX, modeIndexToSave);
                this.mKeepSecureModule = false;
            } else {
                this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_SECURE_MODULE_INDEX, photoIndex);
            }
        }
        this.mPaused = true;
        this.mPeekAnimationHandler = null;
        this.mPeekAnimationThread.quitSafely();
        this.mPeekAnimationThread = null;
        performDeletion();
        if (this.mCurrentModule != null) {
            try {
                this.mCurrentModule.pause();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.mOrientationManager.pause();
        this.mPanoramaViewHelper.onPause();
        pauseLocationManager();
        this.mLocalImagesObserver.setForegroundChangeListener(null);
        this.mLocalVideosObserver.setForegroundChangeListener(null);
        this.mLocalImagesObserver.setActivityPaused(true);
        this.mLocalVideosObserver.setActivityPaused(true);
        resetScreenOn();
        this.mMotionManager.stop();
        if (this.mBatteryWarningDialog != null && this.mBatteryWarningDialog.isShowing()) {
            this.mCameraAppUI.setViewFinderLayoutVisibile(false);
            this.mBatteryWarningDialog.dismiss();
            this.mBatteryWarningDialog = null;
        }
        if (this.mBatteryLowDialog != null && this.mBatteryLowDialog.isShowing()) {
            this.mCameraAppUI.setViewFinderLayoutVisibile(false);
            this.mBatteryLowDialog.dismiss();
            this.mBatteryLowDialog = null;
        }
        if (this.mStorageLowDialog != null && this.mStorageLowDialog.isShowing()) {
            this.mCameraAppUI.setViewFinderLayoutVisibile(false);
            this.mStorageLowDialog.dismiss();
            this.mStorageLowDialog = null;
        }
        if (this.mBatteryChangedReceiver != null) {
            unregisterReceiver(this.mBatteryChangedReceiver);
            this.mBatteryChangedReceiver = null;
        }
        UsageStatistics.instance().backgrounded();
        if (this.mCameraFatalError && !isFinishing()) {
            Log.v(TAG, "onPause when camera is in fatal state, call Activity.finish()");
            finish();
        } else if (this.mCurrentModule == null) {
            this.mCameraController.closeCamera(true);
        }
    }

    /* Access modifiers changed, original: protected */
    public void onReStartTasks() {
        super.onReStartTasks();
        android.util.Log.d("liugz", "onReStartTasks，.................");
        this.mCameraAppUI.setInVisibleKeyguard();
    }

    /* JADX WARNING: Missing block: B:36:0x01d8, code skipped:
            if (r9.equals("android.media.action.VIDEO_CAMERA") != false) goto L_0x0218;
     */
    public void onResumeTasks() {
        /*
        r14 = this;
        r0 = r14.getSettingsManager();
        r1 = "default_scope";
        r2 = "pref_camera_recordlocation_key";
        r0 = r0.getBoolean(r1, r2);
        com.hmdglobal.app.camera.beauty.util.Util.isLocationOn = r0;
        r0 = com.hmdglobal.app.camera.beauty.util.Util.isLocationOn;
        r1 = 1;
        if (r0 == 0) goto L_0x001e;
    L_0x0013:
        r0 = r14.getSettingsManager();
        r2 = r14.getLocationManager();
        com.hmdglobal.app.camera.settings.Keys.setLocation(r0, r1, r2);
    L_0x001e:
        r0 = new android.content.IntentFilter;
        r2 = "android.intent.action.SCREEN_OFF";
        r0.<init>(r2);
        r2 = "android.intent.action.CLOSE_SYSTEM_DIALOGS";
        r0.addAction(r2);
        r2 = r14.screenOffReceiver;
        r14.registerReceiver(r2, r0);
        r2 = r14.mTelephonyManager;
        r3 = 2;
        r4 = 0;
        if (r2 == 0) goto L_0x0055;
    L_0x0035:
        r2 = r14.mTelephonyManager;
        r2 = r2.getCallState();
        if (r2 != r3) goto L_0x0055;
    L_0x003d:
        r2 = r14.getContentResolver();
        r5 = "IS_VIDEO_CALL";
        r2 = android.provider.Settings.System.getInt(r2, r5, r4);
        if (r2 <= 0) goto L_0x0055;
    L_0x0049:
        r2 = com.hmdglobal.app.camera.util.PermissionsUtil.isCriticalPermissionGranted(r14);
        if (r2 == 0) goto L_0x0055;
    L_0x004f:
        r2 = 2131689619; // 0x7f0f0093 float:1.9008258E38 double:1.0531946083E-314;
        com.hmdglobal.app.camera.util.CameraUtil.showErrorAndFinish(r14, r2);
    L_0x0055:
        r2 = "currentBatteryStatus";
        r2 = com.hmdglobal.app.camera.beauty.util.SharedUtil.getIntValueByKey(r2);
        r2 = r2.intValue();
        r14.currentBatteryStatus = r2;
        r2 = r14.currentBatteryStatus;
        if (r2 == 0) goto L_0x006f;
    L_0x0065:
        r2 = 2131689729; // 0x7f0f0101 float:1.9008482E38 double:1.0531946627E-314;
        r2 = r14.getString(r2);
        com.hmdglobal.app.camera.util.ToastUtil.showToast(r14, r2, r4);
    L_0x006f:
        r2 = r14.mCameraAppUI;
        r2.setViewFinderLayoutVisibile(r4);
        android.util.CameraPerformanceTracker.onEvent(r3);
        r2 = TAG;
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r6 = "Build info: ";
        r5.append(r6);
        r6 = android.os.Build.DISPLAY;
        r5.append(r6);
        r5 = r5.toString();
        com.hmdglobal.app.camera.debug.Log.v(r2, r5);
        r2 = r14.getApplicationContext();
        r2 = com.hmdglobal.app.camera.util.CustomUtil.getInstance(r2);
        r2.setCustomFromSystem();
        r2 = r14.mSettingsManager;
        r5 = r14.mAppContext;
        com.hmdglobal.app.camera.settings.Keys.setToDefaults(r2, r5);
        r14.mPaused = r4;
        r2 = r14.mHelpTipsManager;
        if (r2 == 0) goto L_0x00b0;
    L_0x00a7:
        r2 = r14.mRequestPermissionsFinished;
        if (r2 == 0) goto L_0x00b0;
    L_0x00ab:
        r2 = r14.mHelpTipsManager;
        r2.calcCameraUseTimes();
    L_0x00b0:
        r2 = r14.mModeStripView;
        if (r2 == 0) goto L_0x00b9;
    L_0x00b4:
        r2 = r14.mModeStripView;
        r2.resume();
    L_0x00b9:
        r2 = r14.getSettingsManager();
        r5 = "default_scope";
        r6 = "pref_camera_savepath_key";
        r7 = r14.getResources();
        r8 = 2131690029; // 0x7f0f022d float:1.900909E38 double:1.053194811E-314;
        r7 = r7.getString(r8);
        r2 = r2.getString(r5, r6, r7);
        r5 = com.hmdglobal.app.camera.Storage.getSavePath();
        com.hmdglobal.app.camera.Storage.setSavePath(r2);
        r6 = "0";
        r6 = r2.equals(r6);
        r7 = -1;
        if (r6 == 0) goto L_0x0187;
    L_0x00e0:
        r6 = com.hmdglobal.app.camera.Storage.isSDCardAvailable();
        if (r6 == 0) goto L_0x0187;
    L_0x00e6:
        r6 = r14.getSettingsManager();
        r8 = "default_scope";
        r9 = "storage_path";
        r6 = r6.getBoolean(r8, r9);
        r8 = "camera";
        r9 = new java.lang.StringBuilder;
        r9.<init>();
        r10 = "haveAsked=";
        r9.append(r10);
        r9.append(r6);
        r9 = r9.toString();
        android.util.Log.i(r8, r9);
        if (r6 != 0) goto L_0x0187;
    L_0x010a:
        r8 = "camera";
        r9 = "show change storage dialog";
        android.util.Log.i(r8, r9);
        r8 = new android.app.AlertDialog$Builder;
        r8.<init>(r14);
        r8 = r8.create();
        r8.setCancelable(r4);
        r9 = 2131689652; // 0x7f0f00b4 float:1.9008325E38 double:1.0531946246E-314;
        r8.setTitle(r9);
        r9 = r14.getResources();
        r10 = 2131689651; // 0x7f0f00b3 float:1.9008323E38 double:1.053194624E-314;
        r9 = r9.getString(r10);
        r8.setMessage(r9);
        r9 = r14.getResources();
        r10 = 2131689692; // 0x7f0f00dc float:1.9008407E38 double:1.0531946444E-314;
        r9 = r9.getString(r10);
        r10 = new com.hmdglobal.app.camera.CameraActivity$22;
        r10.<init>();
        r8.setButton(r7, r9, r10);
        r9 = r14.getResources();
        r10 = 2131689691; // 0x7f0f00db float:1.9008405E38 double:1.053194644E-314;
        r9 = r9.getString(r10);
        r10 = new com.hmdglobal.app.camera.CameraActivity$23;
        r10.<init>();
        r11 = -2;
        r8.setButton(r11, r9, r10);
        r8.show();
        r9 = r8.getButton(r7);
        r10 = r14.getResources();
        r12 = 2131034181; // 0x7f050045 float:1.7678872E38 double:1.052870779E-314;
        r10 = r10.getColor(r12);
        r9.setTextColor(r10);
        r9 = r8.getButton(r11);
        r10 = r14.getResources();
        r10 = r10.getColor(r12);
        r9.setTextColor(r10);
        r9 = r14.getSettingsManager();
        r10 = "default_scope";
        r11 = "storage_path";
        r9.set(r10, r11, r1);
    L_0x0187:
        r6 = com.hmdglobal.app.camera.Storage.getSavePath();
        r6 = r5.equals(r6);
        r6 = r6 ^ r1;
        r8 = TAG;
        r9 = new java.lang.StringBuilder;
        r9.<init>();
        r10 = "current save path is ";
        r9.append(r10);
        r10 = com.hmdglobal.app.camera.Storage.getSavePath();
        r9.append(r10);
        r9 = r9.toString();
        com.hmdglobal.app.camera.debug.Log.i(r8, r9);
        r8 = 0;
        r14.updateStorageSpaceAndHint(r8);
        r9 = r14.getResources();
        r9 = r9.getConfiguration();
        r9 = r9.orientation;
        r14.mLastLayoutOrientation = r9;
        r9 = r14.getIntent();
        r9 = r9.getAction();
        r10 = 10000; // 0x2710 float:1.4013E-41 double:4.9407E-320;
        if (r9 != 0) goto L_0x01ca;
    L_0x01c6:
        r3 = 10000; // 0x2710 float:1.4013E-41 double:4.9407E-320;
        goto L_0x0232;
    L_0x01ca:
        r11 = r9.hashCode();
        switch(r11) {
            case -1960745709: goto L_0x020d;
            case -1658348509: goto L_0x0203;
            case -1173447682: goto L_0x01f9;
            case 464109999: goto L_0x01ef;
            case 485955591: goto L_0x01e5;
            case 701083699: goto L_0x01db;
            case 1130890360: goto L_0x01d2;
            default: goto L_0x01d1;
        };
    L_0x01d1:
        goto L_0x0217;
    L_0x01d2:
        r11 = "android.media.action.VIDEO_CAMERA";
        r11 = r9.equals(r11);
        if (r11 == 0) goto L_0x0217;
    L_0x01da:
        goto L_0x0218;
    L_0x01db:
        r3 = "android.media.action.VIDEO_CAPTURE";
        r3 = r9.equals(r3);
        if (r3 == 0) goto L_0x0217;
    L_0x01e3:
        r3 = 3;
        goto L_0x0218;
    L_0x01e5:
        r3 = "android.media.action.STILL_IMAGE_CAMERA_SECURE";
        r3 = r9.equals(r3);
        if (r3 == 0) goto L_0x0217;
    L_0x01ed:
        r3 = 4;
        goto L_0x0218;
    L_0x01ef:
        r3 = "android.media.action.STILL_IMAGE_CAMERA";
        r3 = r9.equals(r3);
        if (r3 == 0) goto L_0x0217;
    L_0x01f7:
        r3 = r1;
        goto L_0x0218;
    L_0x01f9:
        r3 = "android.intent.action.MAIN";
        r3 = r9.equals(r3);
        if (r3 == 0) goto L_0x0217;
    L_0x0201:
        r3 = 6;
        goto L_0x0218;
    L_0x0203:
        r3 = "android.media.action.IMAGE_CAPTURE_SECURE";
        r3 = r9.equals(r3);
        if (r3 == 0) goto L_0x0217;
    L_0x020b:
        r3 = 5;
        goto L_0x0218;
    L_0x020d:
        r3 = "android.media.action.IMAGE_CAPTURE";
        r3 = r9.equals(r3);
        if (r3 == 0) goto L_0x0217;
    L_0x0215:
        r3 = r4;
        goto L_0x0218;
    L_0x0217:
        r3 = r7;
    L_0x0218:
        switch(r3) {
            case 0: goto L_0x022f;
            case 1: goto L_0x022c;
            case 2: goto L_0x0229;
            case 3: goto L_0x0226;
            case 4: goto L_0x0223;
            case 5: goto L_0x0220;
            case 6: goto L_0x021d;
            default: goto L_0x021b;
        };
    L_0x021b:
        r3 = r10;
        goto L_0x0232;
    L_0x021d:
        r3 = 10000; // 0x2710 float:1.4013E-41 double:4.9407E-320;
        goto L_0x0232;
    L_0x0220:
        r3 = 10000; // 0x2710 float:1.4013E-41 double:4.9407E-320;
        goto L_0x0232;
    L_0x0223:
        r3 = 10000; // 0x2710 float:1.4013E-41 double:4.9407E-320;
        goto L_0x0232;
    L_0x0226:
        r3 = 10000; // 0x2710 float:1.4013E-41 double:4.9407E-320;
        goto L_0x0232;
    L_0x0229:
        r3 = 10000; // 0x2710 float:1.4013E-41 double:4.9407E-320;
        goto L_0x0232;
    L_0x022c:
        r3 = 10000; // 0x2710 float:1.4013E-41 double:4.9407E-320;
        goto L_0x0232;
    L_0x022f:
        r3 = 10000; // 0x2710 float:1.4013E-41 double:4.9407E-320;
        r7 = com.hmdglobal.app.camera.util.UsageStatistics.instance();
        r11 = r14.currentUserInterfaceMode();
        r7.foregrounded(r3, r11);
        r7 = r14.mAppContext;
        r7 = com.hmdglobal.app.camera.util.IntentHelper.getGalleryIntent(r7);
        r14.mGalleryIntent = r7;
        r7 = r14.mOrientationManager;
        r7.resume();
        r7 = new android.os.HandlerThread;
        r11 = "Peek animation";
        r7.<init>(r11);
        r14.mPeekAnimationThread = r7;
        r7 = r14.mPeekAnimationThread;
        r7.start();
        r7 = new com.hmdglobal.app.camera.CameraActivity$PeekAnimationHandler;
        r11 = r14.mPeekAnimationThread;
        r11 = r11.getLooper();
        r12 = r14.mMainHandler;
        r13 = r14.mMainActivityLayout;
        r7.<init>(r11, r12, r13);
        r14.mPeekAnimationHandler = r7;
        r7 = r14.mModeSelectingOnStart;
        if (r7 != 0) goto L_0x027b;
    L_0x026e:
        r7 = r14.mCurrentModule;
        r11 = r14.mSettingsManager;
        r7.hardResetSettings(r11);
        r7 = r14.mCurrentModule;
        r7.resume();
        goto L_0x0298;
    L_0x027b:
        r7 = r14.mModuleOpenBeforeResume;
        if (r7 == 0) goto L_0x0298;
    L_0x027f:
        r14.mModuleOpenBeforeResume = r4;
        r7 = r14.mCurrentModule;
        r7.resume();
        r7 = com.hmdglobal.app.camera.util.UsageStatistics.instance();
        r11 = r14.currentUserInterfaceMode();
        r12 = java.lang.Integer.valueOf(r10);
        r7.changeScreen(r11, r12);
        r14.updatePreviewVisibility();
    L_0x0298:
        r7 = com.hmdglobal.app.camera.util.UsageStatistics.instance();
        r11 = r14.currentUserInterfaceMode();
        r10 = java.lang.Integer.valueOf(r10);
        r7.changeScreen(r11, r10);
        r14.setSwipingEnabled(r1);
        r7 = r14.mResetToPreviewOnResume;
        r14.mResetToPreviewOnResume = r1;
        r1 = r14.mThumbUpdateRunnable;
        if (r1 == 0) goto L_0x02b9;
    L_0x02b2:
        r1 = r14.mThumbUpdateRunnable;
        r1.run();
        r14.mThumbUpdateRunnable = r8;
    L_0x02b9:
        r1 = r14.needUpdatesForInstanceCapture();
        if (r1 != 0) goto L_0x02d1;
    L_0x02bf:
        r1 = r14.mLocalVideosObserver;
        r1 = r1.isMediaDataChangedDuringPause();
        if (r1 != 0) goto L_0x02d1;
    L_0x02c7:
        r1 = r14.mLocalImagesObserver;
        r1 = r1.isMediaDataChangedDuringPause();
        if (r1 != 0) goto L_0x02d1;
    L_0x02cf:
        if (r6 == 0) goto L_0x02f4;
    L_0x02d1:
        r1 = r14.mSecureCamera;
        if (r1 != 0) goto L_0x02df;
    L_0x02d5:
        r1 = r14.isExternalStorageAvailable();
        if (r1 == 0) goto L_0x02df;
    L_0x02db:
        r14.onLastMediaDataUpdated();
        goto L_0x02f4;
    L_0x02df:
        r1 = r14.mSecureCamera;
        if (r1 == 0) goto L_0x02ed;
    L_0x02e3:
        r1 = r14.isExternalStorageAvailable();
        if (r1 == 0) goto L_0x02ed;
    L_0x02e9:
        r14.onLastMediaDataUpdated();
        goto L_0x02f4;
    L_0x02ed:
        r1 = TAG;
        r7 = "updatePeekimagethree";
        com.hmdglobal.app.camera.debug.Log.d(r1, r7);
    L_0x02f4:
        r1 = r14.mLocalImagesObserver;
        r1.setActivityPaused(r4);
        r1 = r14.mLocalVideosObserver;
        r1.setActivityPaused(r4);
        r1 = r14.mLocalImagesObserver;
        r7 = new com.hmdglobal.app.camera.CameraActivity$24;
        r7.<init>();
        r1.setForegroundChangeListener(r7);
        r1 = r14.mLocalVideosObserver;
        r7 = new com.hmdglobal.app.camera.CameraActivity$25;
        r7.<init>();
        r1.setForegroundChangeListener(r7);
        r14.keepScreenOnForAWhile();
        r1 = r14.mLightsOutRunnable;
        r1.run();
        r1 = r14.getWindow();
        r1 = r1.getDecorView();
        r7 = new com.hmdglobal.app.camera.CameraActivity$26;
        r7.<init>();
        r1.setOnSystemUiVisibilityChangeListener(r7);
        r1 = r14.mPanoramaViewHelper;
        r1.onResume();
        r1 = r14.mSettingsManager;
        com.hmdglobal.app.camera.util.ReleaseHelper.showReleaseInfoDialogOnStart(r14, r1);
        r14.syncLocationManagerSetting();
        r1 = r14.getPreviewVisibility();
        r14.updatePreviewRendering(r1);
        r7 = r14.mMotionManager;
        r7.start();
        r7 = r14.mBatteryChangedReceiver;
        if (r7 != 0) goto L_0x035a;
    L_0x0347:
        r7 = new android.content.IntentFilter;
        r10 = "android.intent.action.BATTERY_CHANGED";
        r7.<init>(r10);
        r10 = new com.hmdglobal.app.camera.CameraActivity$BatteryBroadcastReceiver;
        r10.<init>(r14, r8);
        r14.mBatteryChangedReceiver = r10;
        r8 = r14.mBatteryChangedReceiver;
        r14.registerReceiver(r8, r7);
    L_0x035a:
        r7 = r14.getButtonManager();
        r7.registerOnSharedPreferenceChangeListener();
        r7 = com.hmdglobal.app.camera.util.CustomUtil.getInstance();
        r8 = "def_camera_low_battery_feature_independent";
        r4 = r7.getBoolean(r8, r4);
        if (r4 != 0) goto L_0x0373;
    L_0x036d:
        r4 = com.hmdglobal.app.camera.util.CameraUtil.isBatterySaverEnabled(r14);
        r14.mBatterySaveOn = r4;
    L_0x0373:
        r14.closeReverisble();
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.CameraActivity.onResumeTasks():void");
    }

    private boolean needUpdatesForInstanceCapture() {
        if (!INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE.equals(getIntent().getAction())) {
            return false;
        }
        ArrayList<Uri> uris = getIntent().getParcelableArrayListExtra("uris");
        if (uris == null || uris.isEmpty()) {
            return false;
        }
        this.mSecureUris = getSecureUris();
        this.mSecureUris.addAll(uris);
        return true;
    }

    public void onLastMediaDataUpdated() {
        if (PermissionsUtil.isPermissionGranted(this, PermissionsUtil.PERMS_READ_EXTERNAL_STORAGE)) {
            Log.w(TAG, "last mediaData updated");
            new AsyncTask<Void, Void, Thumbnail>() {
                /* Access modifiers changed, original: protected */
                public void onPreExecute() {
                    super.onPreExecute();
                    Log.w(CameraActivity.TAG, "update media,onPreExecute");
                }

                /* Access modifiers changed, original: protected */
                public Thumbnail doInBackground(Void[] params) {
                    Thumbnail[] thumb = new Thumbnail[1];
                    Thumbnail.getLastThumbnailFromContentResolver(CameraActivity.this.getContentResolver(), thumb);
                    Tag access$1000 = CameraActivity.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("update media,doInBackground:");
                    stringBuilder.append(thumb[0]);
                    Log.w(access$1000, stringBuilder.toString());
                    return thumb[0];
                }

                /* Access modifiers changed, original: protected */
                public void onPostExecute(Thumbnail o) {
                    super.onPostExecute(o);
                    Log.w(CameraActivity.TAG, "need to update Thumb");
                    if (o == null || o.getBitmap() == null) {
                        if (!CameraActivity.this.updatePeekThumbViewGP()) {
                            CameraActivity.this.mCameraAppUI.updatePeekThumbContent(null);
                        }
                    } else if (!CameraActivity.this.isSecureCamera() || (CameraActivity.this.mSecureUris != null && (CameraActivity.this.mSecureUris == null || CameraActivity.this.mSecureUris.contains(o.getUri())))) {
                        Tag access$1000 = CameraActivity.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("latest thumbnail: ");
                        stringBuilder.append(o.getUri());
                        Log.w(access$1000, stringBuilder.toString());
                        if (CameraActivity.this.updatePeekThumbViewGP()) {
                            Log.d(CameraActivity.TAG, "GP_peekThumbUpdated");
                        } else {
                            CameraActivity.this.mCameraAppUI.updatePeekThumbContent(o);
                        }
                        Log.w(CameraActivity.TAG, "peekThumbUpdated");
                    } else {
                        CameraActivity.this.mCameraAppUI.updatePeekThumbContent(null);
                    }
                }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
            return;
        }
        Log.w(TAG, "requires android.permission.READ_EXTERNAL_STORAGE");
    }

    private boolean updatePeekThumbViewGP() {
        List<ProcessingMedia> list = ProcessingMediaManager.getInstance(this.mAppContext).getProcessingMedia();
        if (list == null || list.size() <= 0) {
            Log.d(TAG, "GP_no processing item");
            return false;
        }
        ProcessingMedia media = (ProcessingMedia) list.get(0);
        if (media != null) {
            this.mCameraAppUI.updatePeekThumbBitmap(media.getThumbnailBitmap());
            return true;
        }
        Log.w(TAG, "GP_media is null");
        return false;
    }

    private void fillTemporarySessions() {
        if (!this.mSecureCamera) {
            getServices().getCaptureSessionManager().fillTemporarySession(this.mSessionListener);
        }
    }

    public void onStartTasks() {
        gIsCameraActivityRunning = true;
        this.mPanoramaViewHelper.onStart();
        int modeIndex = getModeIndex();
        if (!(isCaptureIntent() || this.mCurrentModeIndex == modeIndex || this.mCurrentModeIndex >= 0)) {
            this.mModeSelectingOnStart = true;
            switchToMode(modeIndex);
        }
        if (this.mResetToPreviewOnResume) {
            this.mCameraAppUI.onStart();
            this.mResetToPreviewOnResume = false;
        }
    }

    public void updateModeForFyusion(int index) {
        this.mModeSelectingOnStart = true;
        switchToMode(index);
    }

    /* Access modifiers changed, original: protected */
    public void onStopTasks() {
        gIsCameraActivityRunning = false;
        this.mPanoramaViewHelper.onStop();
        this.mLocationManager.disconnect();
        this.mCameraController.closeCamera(true);
        this.lastStopStamp = System.currentTimeMillis();
        android.util.Log.d("liugz", "onStopTasks，...............");
        this.mCameraAppUI.setVisibleKeyguard();
        this.mCameraAppUI.onStop();
        removeFloatView();
        resetScreenBrightness();
    }

    public void onDestroyTasks() {
        CameraApi cameraApi;
        Log.w(TAG, "destory task");
        unregisterReceiver(this.screenOffReceiver);
        PermissionsUtil.dismissAllDialogWhenDestroy();
        BeautifyHandler.release();
        if (this.mCurrentModule != null) {
            this.mCurrentModule.destroy();
        }
        if (this.mRequestPermissionsFinished && this.mHelpTipsManager != null) {
            this.mHelpTipsManager.destroy();
            this.mHelpTipsManager = null;
        }
        if (this.mSecureCamera) {
            unregisterReceiver(this.mShutdownReceiver);
        }
        if (this.mExitBroadCast != null) {
            unregisterReceiver(this.mExitBroadCast);
            this.mExitBroadCast = null;
        }
        if (this.mBatteryChangedReceiver != null) {
            unregisterReceiver(this.mBatteryChangedReceiver);
            this.mBatteryChangedReceiver = null;
        }
        unregisterReceiver(this.mMediaActionReceiver);
        unregisterReceiver(this.broadcastReceiver);
        releaseMccChangedReceiver();
        releaseSimStateReceiver();
        clearSecureUris();
        this.mSettingsManager.removeAllListeners();
        this.mCameraController.removeCallbackReceiver();
        this.mCameraController.setCameraExceptionHandler(null);
        getContentResolver().unregisterContentObserver(this.mLocalImagesObserver);
        getContentResolver().unregisterContentObserver(this.mLocalVideosObserver);
        getServices().getCaptureSessionManager().removeSessionListener(this.mSessionListener);
        unregisterPhoneStateListener();
        this.mCameraAppUI.onDestroy();
        this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_STARTUP_MODULE_INDEX, getResources().getInteger(R.integer.camera_mode_photo));
        this.mSoundPlayer.release();
        if (this.mSoundClipsPlayer != null) {
            this.mSoundClipsPlayer.release();
        }
        CameraAgentFactory.recycle(CameraApi.API_1);
        if (GservicesHelper.useCamera2ApiThroughPortabilityLayer(this.mAppContext)) {
            cameraApi = CameraApi.AUTO;
        } else {
            cameraApi = CameraApi.API_1;
        }
        CameraAgentFactory.recycle(cameraApi);
        BlurUtil.destroy();
    }

    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        Log.v(TAG, "onConfigurationChanged");
        if (!(config.orientation == 0 || this.mLastLayoutOrientation == config.orientation)) {
            this.mLastLayoutOrientation = config.orientation;
            this.mCurrentModule.onLayoutOrientationChanged(this.mLastLayoutOrientation == 2);
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!this.mFilmstripVisible) {
            if (CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_HELP_TIP_TUTORIAL, false) && this.mRequestPermissionsFinished && this.mHelpTipsManager != null && this.mHelpTipsManager.isHelpTipShowExist()) {
                if (keyCode == 4) {
                    return super.onKeyDown(keyCode, event);
                }
                if (!(keyCode == CameraUtil.BOOM_KEY && this.mHelpTipsManager.isNeedBoomKeyResponse())) {
                    return true;
                }
            }
            if (!CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_MODULE_BOOMKEY_RESPONSE, false) && keyCode == CameraUtil.BOOM_KEY) {
                return super.onKeyDown(keyCode, event);
            }
            if ((keyCode != 24 && keyCode != 25) || CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_VOLUME_KEY_RESPONSE, true)) {
                if (keyCode == CameraUtil.BOOM_KEY) {
                    Log.e(TAG, "onKeyDown begin to handle boom key events");
                    int iCameraKeySetting;
                    if (CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_VDF_BOOMKEY_CUSTOMIZE, false)) {
                        iCameraKeySetting = System.getInt(getContentResolver(), CameraUtil.CAMERA_KEY_SETTINGS, 0);
                        Tag tag = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("vdf BoomKey customize onKeyDown iCameraKeySetting = ");
                        stringBuilder.append(iCameraKeySetting);
                        Log.e(tag, stringBuilder.toString());
                        if (iCameraKeySetting != 0) {
                            return true;
                        }
                    }
                    iCameraKeySetting = System.getInt(getContentResolver(), CameraUtil.CAMERA_KEY_SETTINGS, 0);
                    int iBoomEffectSetting = System.getInt(getContentResolver(), CameraUtil.BOOM_EFFECT_SETTINGS, 0);
                    Tag tag2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("onKeyDown iCameraKeySetting = ");
                    stringBuilder2.append(iCameraKeySetting);
                    stringBuilder2.append(",iBoomEffectSetting = ");
                    stringBuilder2.append(iBoomEffectSetting);
                    Log.e(tag2, stringBuilder2.toString());
                    if (!(iBoomEffectSetting == 1 || iCameraKeySetting == 0)) {
                        return true;
                    }
                }
                if (this.mCurrentModule != null && this.mCurrentModule.onKeyDown(keyCode, event)) {
                    return true;
                }
                if ((keyCode == 84 || keyCode == 82) && event.isLongPress()) {
                    return true;
                }
            } else if (CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_VOLUME_KEY_FOLLOW_SYS, false)) {
                return super.onKeyDown(keyCode, event);
            } else {
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    /* JADX WARNING: Missing block: B:55:0x00fc, code skipped:
            return true;
     */
    public boolean onKeyUp(int r9, android.view.KeyEvent r10) {
        /*
        r8 = this;
        r0 = r8.mFilmstripVisible;
        r1 = 22;
        r2 = 21;
        r3 = 1;
        if (r0 != 0) goto L_0x00fd;
    L_0x0009:
        r0 = com.hmdglobal.app.camera.util.CustomUtil.getInstance();
        r4 = "def_camera_support_help_tip_tutorial";
        r5 = 0;
        r0 = r0.getBoolean(r4, r5);
        r4 = 276; // 0x114 float:3.87E-43 double:1.364E-321;
        if (r0 == 0) goto L_0x003c;
    L_0x0018:
        r0 = r8.mRequestPermissionsFinished;
        if (r0 == 0) goto L_0x003c;
    L_0x001c:
        r0 = r8.mHelpTipsManager;
        if (r0 == 0) goto L_0x003c;
    L_0x0020:
        r0 = r8.mHelpTipsManager;
        r0 = r0.isHelpTipShowExist();
        if (r0 == 0) goto L_0x003c;
    L_0x0028:
        r0 = 4;
        if (r9 != r0) goto L_0x0030;
    L_0x002b:
        r0 = super.onKeyUp(r9, r10);
        return r0;
    L_0x0030:
        if (r9 != r4) goto L_0x003b;
    L_0x0032:
        r0 = r8.mHelpTipsManager;
        r0 = r0.isNeedBoomKeyResponse();
        if (r0 != 0) goto L_0x003c;
    L_0x003a:
        return r3;
    L_0x003b:
        return r3;
    L_0x003c:
        r0 = com.hmdglobal.app.camera.util.CustomUtil.getInstance();
        r6 = "def_camera_module_boomkey_response";
        r0 = r0.getBoolean(r6, r5);
        if (r0 != 0) goto L_0x004f;
    L_0x0048:
        if (r9 != r4) goto L_0x004f;
    L_0x004a:
        r0 = super.onKeyUp(r9, r10);
        return r0;
    L_0x004f:
        r0 = 24;
        if (r9 == r0) goto L_0x0057;
    L_0x0053:
        r0 = 25;
        if (r9 != r0) goto L_0x0075;
    L_0x0057:
        r0 = com.hmdglobal.app.camera.util.CustomUtil.getInstance();
        r6 = "def_camera_volume_key_response";
        r0 = r0.getBoolean(r6, r3);
        if (r0 != 0) goto L_0x0075;
    L_0x0063:
        r0 = com.hmdglobal.app.camera.util.CustomUtil.getInstance();
        r1 = "def_camera_volume_key_follow_system";
        r0 = r0.getBoolean(r1, r5);
        if (r0 == 0) goto L_0x0074;
    L_0x006f:
        r0 = super.onKeyUp(r9, r10);
        return r0;
    L_0x0074:
        return r3;
    L_0x0075:
        if (r9 != r4) goto L_0x00e5;
    L_0x0077:
        r0 = TAG;
        r4 = "onKeyUp begin to handle boom key events";
        com.hmdglobal.app.camera.debug.Log.e(r0, r4);
        r0 = com.hmdglobal.app.camera.util.CustomUtil.getInstance();
        r4 = "def_camera_vdf_boomkey_custmize";
        r0 = r0.getBoolean(r4, r5);
        if (r0 != 0) goto L_0x00c2;
    L_0x008a:
        r0 = r8.getContentResolver();
        r4 = "boom_key_action";
        r0 = android.provider.Settings.System.getInt(r0, r4, r5);
        r4 = r8.getContentResolver();
        r6 = "boom_key_unlock_enable";
        r4 = android.provider.Settings.System.getInt(r4, r6, r5);
        r5 = TAG;
        r6 = new java.lang.StringBuilder;
        r6.<init>();
        r7 = "onKeyUp iCameraKeySetting = ";
        r6.append(r7);
        r6.append(r0);
        r7 = ",iBoomEffectSetting = ";
        r6.append(r7);
        r6.append(r4);
        r6 = r6.toString();
        com.hmdglobal.app.camera.debug.Log.e(r5, r6);
        if (r4 == r3) goto L_0x00c1;
    L_0x00be:
        if (r0 == 0) goto L_0x00c1;
    L_0x00c0:
        return r3;
    L_0x00c1:
        goto L_0x00e5;
    L_0x00c2:
        r0 = r8.getContentResolver();
        r4 = "boom_key_action";
        r0 = android.provider.Settings.System.getInt(r0, r4, r5);
        r4 = TAG;
        r5 = new java.lang.StringBuilder;
        r5.<init>();
        r6 = "onKeyUp iCameraKeySetting = ";
        r5.append(r6);
        r5.append(r0);
        r5 = r5.toString();
        com.hmdglobal.app.camera.debug.Log.e(r4, r5);
        if (r0 == 0) goto L_0x00e5;
    L_0x00e4:
        return r3;
    L_0x00e5:
        r0 = r8.mCurrentModule;
        if (r0 == 0) goto L_0x00f2;
    L_0x00e9:
        r0 = r8.mCurrentModule;
        r0 = r0.onKeyUp(r9, r10);
        if (r0 == 0) goto L_0x00f2;
    L_0x00f1:
        return r3;
    L_0x00f2:
        r0 = 82;
        if (r9 == r0) goto L_0x00fc;
    L_0x00f6:
        if (r9 != r2) goto L_0x00f9;
    L_0x00f8:
        goto L_0x00fc;
    L_0x00f9:
        if (r9 != r1) goto L_0x0103;
    L_0x00fb:
        return r3;
    L_0x00fc:
        return r3;
    L_0x00fd:
        if (r9 != r1) goto L_0x0100;
    L_0x00ff:
        return r3;
    L_0x0100:
        if (r9 != r2) goto L_0x0103;
    L_0x0102:
        return r3;
    L_0x0103:
        r0 = super.onKeyUp(r9, r10);
        return r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.CameraActivity.onKeyUp(int, android.view.KeyEvent):boolean");
    }

    public void onBackPressed() {
        Log.e(TAG, "onBackPressed E ");
        if (getButtonManager().isMoreOptionsWrapperShow()) {
            getButtonManager().hideMoreOptionsWrapper();
            return;
        }
        if (!(this.mCameraAppUI.onBackPressed() || this.mCurrentModule.onBackPressed())) {
            super.onBackPressed();
        }
        if (CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_HELP_TIP_TUTORIAL, false) && this.mRequestPermissionsFinished && this.mHelpTipsManager != null && this.mHelpTipsManager.isHelpTipShowExist()) {
            this.mHelpTipsManager.pause();
            finish();
        }
    }

    public boolean isAutoRotateScreen() {
        return this.mAutoRotateScreen;
    }

    /* Access modifiers changed, original: protected */
    public long getStorageSpaceBytes() {
        long j;
        synchronized (this.mStorageSpaceLock) {
            j = this.mStorageSpaceBytes;
        }
        return j;
    }

    /* Access modifiers changed, original: protected */
    public void updateStorageSpaceAndHint(final OnStorageUpdateDoneListener callback) {
        new AsyncTask<Void, Void, Long>() {
            /* Access modifiers changed, original: protected|varargs */
            public Long doInBackground(Void... arg) {
                Long valueOf;
                Long availableStorageBytes = Long.valueOf(Storage.getAvailableSpace());
                synchronized (CameraActivity.this.mStorageSpaceLock) {
                    CameraActivity.this.mStorageSpaceBytes = availableStorageBytes.longValue();
                    valueOf = Long.valueOf(CameraActivity.this.mStorageSpaceBytes);
                }
                return valueOf;
            }

            /* Access modifiers changed, original: protected */
            public void onPostExecute(Long bytes) {
                CameraActivity.this.updateStorageHint(bytes.longValue());
                if (callback == null || CameraActivity.this.mPaused) {
                    Log.v(CameraActivity.TAG, "ignoring storage callback after activity pause");
                } else {
                    callback.onStorageUpdateDone(bytes.longValue());
                }
            }
        }.executeOnExecutor(Executors.newCachedThreadPool(), new Void[0]);
    }

    /* Access modifiers changed, original: protected */
    public void updateStorageSpaceAndHint(final OnStorageUpdateDoneListener callback, final boolean showMountedDialog) {
        new AsyncTask<Void, Void, Long>() {
            /* Access modifiers changed, original: protected|varargs */
            public Long doInBackground(Void... arg) {
                Long valueOf;
                Long availableStorageBytes = Long.valueOf(Storage.getAvailableSpace());
                synchronized (CameraActivity.this.mStorageSpaceLock) {
                    CameraActivity.this.mStorageSpaceBytes = availableStorageBytes.longValue();
                    valueOf = Long.valueOf(CameraActivity.this.mStorageSpaceBytes);
                }
                return valueOf;
            }

            /* Access modifiers changed, original: protected */
            public void onPostExecute(Long bytes) {
                CameraActivity.this.updateStorageHint(bytes.longValue(), showMountedDialog);
                if (callback == null || CameraActivity.this.mPaused) {
                    Log.v(CameraActivity.TAG, "ignoring storage callback after activity pause");
                } else {
                    callback.onStorageUpdateDone(bytes.longValue());
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    /* Access modifiers changed, original: protected */
    public void updateStorageHint(long storageSpace) {
        if (!gIsCameraActivityRunning || isExternalStorageAvailable()) {
        }
    }

    /* Access modifiers changed, original: protected */
    public void updateStorageHint(long storageSpace, boolean showMountedDialog) {
        if (gIsCameraActivityRunning && showMountedDialog) {
            mountedDialog();
        }
    }

    private void mountedDialog() {
        if (this.mMountedDialog == null || !this.mMountedDialog.isShowing()) {
            this.mCameraAppUI.setViewFinderLayoutVisibile(true);
            this.mMountedDialog = CameraUtil.MountedDialog(this, getResources().getString(R.string.insert_sd), getResources().getString(R.string.switch_save_path), getResources().getString(R.string.alert_storage_dialog_ok), getResources().getString(R.string.alert_storage_dialog_cancel));
            Log.d(TAG, "dialog is showing");
        }
    }

    /* Access modifiers changed, original: protected */
    public void setResultEx(int resultCode) {
        this.mResultCodeForTesting = resultCode;
        setResult(resultCode);
    }

    /* Access modifiers changed, original: protected */
    public void setResultEx(int resultCode, Intent data) {
        this.mResultCodeForTesting = resultCode;
        this.mResultDataForTesting = data;
        setResult(resultCode, data);
    }

    public int getResultCode() {
        return this.mResultCodeForTesting;
    }

    public Intent getResultData() {
        return this.mResultDataForTesting;
    }

    public boolean isSecureCamera() {
        return this.mSecureCamera;
    }

    public boolean isPaused() {
        return this.mPaused;
    }

    public int getPreferredChildModeIndex(int modeIndex) {
        if (modeIndex == getResources().getInteger(R.integer.camera_mode_photo) && Keys.isHdrPlusOn(this.mSettingsManager) && GcamHelper.hasGcamAsSeparateModule()) {
            return getResources().getInteger(R.integer.camera_mode_gcam);
        }
        return modeIndex;
    }

    public void onModeSelecting() {
        onModeSelecting(false);
    }

    public void onModeSelecting(boolean disableAnimation) {
        onModeSelecting(disableAnimation, null);
    }

    public void onModeSelecting(boolean disableAnimation, OnTransAnimationListener listener) {
        if (this.mCurrentModule != null) {
            this.mCurrentModule.preparePause();
        }
        if (!disableAnimation) {
            freezeScreenUntilPreviewReady(listener);
        } else if (listener == null) {
            freezeScreenUntilWithoutBlur();
        } else {
            freezeScreenWithoutBlurUntilAnimationDone(listener);
        }
    }

    public void onModeSelected(int modeIndex) {
        CameraPerformanceTracker.onEvent(3);
        if (modeIndex == getResources().getInteger(R.integer.camera_mode_photo) || modeIndex == getResources().getInteger(R.integer.camera_mode_gcam)) {
            this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_MODULE_LAST_USED, modeIndex);
        }
        if (modeIndex == getResources().getInteger(R.integer.camera_mode_square)) {
            this.mCameraController.setSquareModeOn(true);
        }
        if (modeIndex == getResources().getInteger(R.integer.camera_mode_pro)) {
            this.mCameraController.setManualModeOn(true);
        }
        this.mModeChangeRunnable.setTargetIndex(modeIndex);
        if (this.mDuringCall) {
            if (modeIndex == 8 || modeIndex == 10) {
                this.mCameraAppUI.setCalldisable(true);
            } else {
                this.mCameraAppUI.setCalldisable(false);
            }
        }
        if (!CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_HELP_TIP_TUTORIAL, false) || this.mHelpTipsManager == null) {
            this.mModeChangeRunnable.run();
        } else {
            this.mHelpTipsManager.notifyModeChanged(modeIndex, this.mModeChangeRunnable);
        }
    }

    public void closeCameraSybc() {
        this.mCameraController.closeCamera(true);
    }

    public void onSettingsSelected() {
        UsageStatistics.instance().controlUsed(10000);
        Intent intent = new Intent(this, CameraSettingsActivity.class);
        intent.putExtra(Keys.SOURCE_MODULE_SCOPE, getModuleScope());
        intent.putExtra(Keys.SOURCE_CAMERA_ID, getCurrentCameraId());
        intent.putExtra("secure_camera", isSecureCamera());
        startActivity(intent);
        if (this.mCameraAppUI != null) {
            getLockEventListener().forceBlocking();
        }
        this.mKeepSecureModule = true;
    }

    public void freezeScreenUntilPreviewReady() {
        this.mCameraAppUI.freezeScreenUntilPreviewReady();
    }

    public void freezeScreenUntilPreviewReady(OnTransAnimationListener listeners) {
        this.mCameraAppUI.freezeScreenUntilPreviewReady(true, listeners);
    }

    public void freezeScreenUntilWithoutBlur() {
        this.mCameraAppUI.freezeScreenUntilPreviewReady(false, new OnTransAnimationListener[0]);
    }

    public void freezeScreenWithoutBlurUntilAnimationDone(OnTransAnimationListener listeners) {
        this.mCameraAppUI.freezeScreenUntilPreviewReady(false, listeners);
    }

    private void setModuleFromModeIndex(int modeIndex) {
        ModuleAgent agent = this.mModuleManager.getModuleAgent(modeIndex);
        if (agent != null) {
            if (!agent.requestAppForCamera()) {
                this.mCameraController.closeCamera(true);
            }
            this.mCurrentModeIndex = agent.getModuleId();
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("update mode for ");
            stringBuilder.append(this.mCurrentModeIndex);
            Log.w(tag, stringBuilder.toString());
            this.mCurrentModule = (CameraModule) agent.createModule(this);
            Editor editor = getSharedPreferences(this.nameMms, 4).edit();
            if (modeIndex != 3) {
                editor.remove("sys.camera.taking_movie");
                editor.putString("sys.camera.taking_movie", "0");
                editor.apply();
            } else {
                editor.remove("sys.camera.taking_movie");
                editor.putString("sys.camera.taking_movie", GpsMeasureMode.MODE_2_DIMENSIONAL);
                editor.apply();
            }
        }
    }

    public SettingsManager getSettingsManager() {
        return this.mSettingsManager;
    }

    public CameraServices getServices() {
        return (CameraServices) getApplication();
    }

    public List<String> getSupportedModeNames() {
        List<Integer> indices = this.mModuleManager.getSupportedModeIndexList();
        List<String> supported = new ArrayList();
        for (Integer modeIndex : indices) {
            String name = CameraUtil.getCameraModeText(modeIndex.intValue(), this.mAppContext);
            if (!(name == null || name.equals(""))) {
                supported.add(name);
            }
        }
        return supported;
    }

    public ButtonManager getButtonManager() {
        if (this.mButtonManager == null) {
            this.mButtonManager = new ButtonManager(this);
        }
        return this.mButtonManager;
    }

    public SoundPlayer getSoundPlayer() {
        return this.mSoundPlayer;
    }

    public Player getSoundClipPlayer() {
        if (this.mSoundClipsPlayer == null) {
            this.mSoundClipsPlayer = SoundClips.getPlayer(this);
        }
        return this.mSoundClipsPlayer;
    }

    public AlertDialog getFirstTimeLocationAlert() {
        Builder builder = SettingsUtil.getFirstTimeLocationAlertBuilder(new Builder(this), new Callback<Boolean>() {
            public void onCallback(Boolean locationOn) {
                Keys.setLocation(CameraActivity.this.mSettingsManager, locationOn.booleanValue(), CameraActivity.this.mLocationManager);
            }
        });
        if (builder != null) {
            return builder.create();
        }
        return null;
    }

    public void launchEditor(LocalData data) {
        Intent intent = new Intent("android.intent.action.EDIT").setDataAndType(data.getUri(), data.getMimeType()).setFlags(1);
        try {
            launchActivityByIntent(intent);
        } catch (ActivityNotFoundException e) {
            launchActivityByIntent(Intent.createChooser(intent, getResources().getString(R.string.edit_with)));
        }
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.filmstrip_context_menu, menu);
    }

    public void launchTinyPlanetEditor(LocalData data) {
        TinyPlanetFragment fragment = new TinyPlanetFragment();
        Bundle bundle = new Bundle();
        bundle.putString(TinyPlanetFragment.ARGUMENT_URI, data.getUri().toString());
        bundle.putString("title", data.getTitle());
        fragment.setArguments(bundle);
        fragment.show(getFragmentManager(), "tiny_planet");
    }

    private int currentUserInterfaceMode() {
        int mode = 10000;
        if (this.mCurrentModeIndex == getResources().getInteger(R.integer.camera_mode_photo)) {
            mode = 10000;
        }
        if (this.mCurrentModeIndex == getResources().getInteger(R.integer.camera_mode_video)) {
            mode = 10000;
        }
        if (this.mCurrentModeIndex == getResources().getInteger(R.integer.camera_mode_refocus)) {
            mode = 10000;
        }
        if (this.mCurrentModeIndex == getResources().getInteger(R.integer.camera_mode_gcam)) {
            mode = 10000;
        }
        if (this.mCurrentModeIndex == getResources().getInteger(R.integer.camera_mode_photosphere)) {
            mode = 10000;
        }
        if (this.mCurrentModeIndex == getResources().getInteger(R.integer.camera_mode_panorama)) {
            mode = 10000;
        }
        if (this.mFilmstripVisible) {
            return 10000;
        }
        return mode;
    }

    private void openModule(CameraModule module) {
        module.hardResetSettings(this.mSettingsManager);
        module.init(this, isSecureCamera(), isCaptureIntent());
        if (!this.mPaused) {
            module.resume();
            UsageStatistics.instance().changeScreen(currentUserInterfaceMode(), Integer.valueOf(10000));
            updatePreviewVisibility();
        } else if (this.mModeSelectingOnStart) {
            this.mModuleOpenBeforeResume = true;
        }
    }

    private void closeModule(CameraModule module) {
        if (!module.isPaused()) {
            try {
                module.pause();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        this.mCameraAppUI.clearModuleUI();
    }

    private void performDeletion() {
        if (this.mPendingDeletion) {
            hideUndoDeletionBar(false);
        }
    }

    private void hideUndoDeletionBar(boolean withAnimation) {
        Log.v(TAG, "Hiding undo deletion bar");
        this.mPendingDeletion = false;
        if (this.mUndoDeletionBar == null) {
            return;
        }
        if (withAnimation) {
            this.mUndoDeletionBar.animate().setDuration(200).alpha(0.0f).setListener(new AnimatorListener() {
                public void onAnimationStart(Animator animation) {
                }

                public void onAnimationEnd(Animator animation) {
                    CameraActivity.this.mUndoDeletionBar.setVisibility(8);
                }

                public void onAnimationCancel(Animator animation) {
                }

                public void onAnimationRepeat(Animator animation) {
                }
            }).start();
        } else {
            this.mUndoDeletionBar.setVisibility(8);
        }
    }

    public boolean isReversibleWorking() {
        return getServices().isReversibleEnabled() && this.mCameraAppUI != null && this.mCameraAppUI.isScreenReversed();
    }

    public void lockRotatableOrientation(int hashCode) {
        if (this.mListeningRotatableMap != null && this.mListeningRotatableMap.containsKey(Integer.valueOf(hashCode)) && !((RotateEntity) this.mListeningRotatableMap.get(Integer.valueOf(hashCode))).isOrientationLocked()) {
            ((RotateEntity) this.mListeningRotatableMap.get(Integer.valueOf(hashCode))).setOrientationLocked(true);
        }
    }

    public void unlockRotatableOrientation(int hashCode) {
        if (this.mListeningRotatableMap != null && this.mListeningRotatableMap.containsKey(Integer.valueOf(hashCode)) && ((RotateEntity) this.mListeningRotatableMap.get(Integer.valueOf(hashCode))).isOrientationLocked()) {
            ((RotateEntity) this.mListeningRotatableMap.get(Integer.valueOf(hashCode))).setOrientationLocked(false);
        }
    }

    public void openOrCloseEffects(int state, int effects, int id) {
        CameraModule curr = getCurrentModule();
        if (curr.isSupportBeauty() && curr.getModuleId() == id) {
            curr.openOrCloseEffects(state, effects);
        }
    }

    private void closeReverisble() {
        if (DeviceInfo.isReversibleOn(getContentResolver()) && DeviceInfo.updateReversibleSetting(getContentResolver(), false)) {
            this.mNeedRestoreReversible = true;
        }
    }

    private void restoreReversible() {
        if (this.mNeedRestoreReversible) {
            DeviceInfo.updateReversibleSetting(getContentResolver(), true);
            this.mNeedRestoreReversible = false;
        }
    }

    public void onOrientationChanged(int orientation) {
        if (orientation != this.mLastRawOrientation) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("orientation changed (from:to) ");
            stringBuilder.append(this.mLastRawOrientation);
            stringBuilder.append(":");
            stringBuilder.append(orientation);
            Log.w(tag, stringBuilder.toString());
        }
        if (orientation != -1) {
            this.mLastRawOrientation = orientation;
            if (this.mCurrentModule != null) {
                this.mCurrentModule.onOrientationChanged(orientation);
            }
            if (isReversibleWorking()) {
                orientation = (MediaProviderUtils.ROTATION_180 + orientation) % 360;
            }
            for (RotateEntity rotateEntity : this.mListeningRotatableMap.values()) {
                if (!rotateEntity.isOrientationLocked()) {
                    rotateEntity.rotatable.setOrientation(orientation, rotateEntity.animation);
                }
            }
        }
    }

    public void setSwipingEnabled(boolean enable) {
        isCaptureIntent();
    }

    public long getFirstPreviewTime() {
        if (this.mCurrentModule instanceof PhotoModule) {
            long coverHiddenTime = getCameraAppUI().getCoverHiddenTime();
            if (coverHiddenTime != -1) {
                return coverHiddenTime - this.mOnCreateTime;
            }
        }
        return -1;
    }

    public long getAutoFocusTime() {
        return this.mCurrentModule instanceof PhotoModule ? ((PhotoModule) this.mCurrentModule).mAutoFocusTime : -1;
    }

    public long getShutterLag() {
        return this.mCurrentModule instanceof PhotoModule ? ((PhotoModule) this.mCurrentModule).mShutterLag : -1;
    }

    public long getShutterToPictureDisplayedTime() {
        return this.mCurrentModule instanceof PhotoModule ? ((PhotoModule) this.mCurrentModule).mShutterToPictureDisplayedTime : -1;
    }

    public long getPictureDisplayedToJpegCallbackTime() {
        return this.mCurrentModule instanceof PhotoModule ? ((PhotoModule) this.mCurrentModule).mPictureDisplayedToJpegCallbackTime : -1;
    }

    public long getJpegCallbackFinishTime() {
        return this.mCurrentModule instanceof PhotoModule ? ((PhotoModule) this.mCurrentModule).mJpegCallbackFinishTime : -1;
    }

    public long getCaptureStartTime() {
        return this.mCurrentModule instanceof PhotoModule ? ((PhotoModule) this.mCurrentModule).mCaptureStartTime : -1;
    }

    public boolean isRecording() {
        return this.mCurrentModule instanceof VideoModule ? ((VideoModule) this.mCurrentModule).isRecording() : false;
    }

    public CameraOpenCallback getCameraOpenErrorCallback() {
        return this.mCameraController;
    }

    public CameraModule getCurrentModule() {
        return this.mCurrentModule;
    }

    public void showTutorial(AbstractTutorialOverlay tutorial) {
        this.mCameraAppUI.showTutorial(tutorial, getLayoutInflater());
    }

    public void showErrorAndFinish(int messageId) {
        CameraUtil.showErrorAndFinish(this, messageId);
    }

    public int getSupportedHardwarelevel(int id) {
        return this.mCameraController.getSupportedHardwareLevel(id);
    }

    public void syncLocationManagerSetting() {
        Keys.syncLocationManager(this.mSettingsManager, this.mLocationManager);
    }

    public void pauseLocationManager() {
        if (this.mLocationManager != null) {
            Keys.pauseLocationManager(this.mLocationManager);
        }
    }

    private void keepScreenOnForAWhile() {
        if (!this.mKeepScreenOn) {
            this.mMainHandler.removeMessages(2);
            getWindow().addFlags(128);
            this.mMainHandler.sendEmptyMessageDelayed(2, SCREEN_DELAY_MS);
        }
    }

    private void resetScreenOn() {
        this.mKeepScreenOn = false;
        this.mMainHandler.removeMessages(2);
        getWindow().clearFlags(128);
    }

    private void setNfcBeamPushUriFromData(LocalData data) {
        Uri uri = data.getUri();
        if (uri != Uri.EMPTY) {
            this.mNfcPushUris[0] = uri;
        } else {
            this.mNfcPushUris[0] = null;
        }
    }

    public HelpTipsManager getHelpTipsManager() {
        return this.mHelpTipsManager;
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && !CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_LOW_BATTERY_FEATURE_INDEPENDENT, true)) {
            boolean currentBatterySave = !CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_LOW_BATTERY_FEATURE_INDEPENDENT, true) && CameraUtil.isBatterySaverEnabled(this);
            if (currentBatterySave != this.mBatterySaveOn) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mBatterySaveOn:");
                stringBuilder.append(this.mBatterySaveOn);
                android.util.Log.d("onWindowFocusChanged", stringBuilder.toString());
                this.mBatterySaveOn = currentBatterySave;
            } else {
                int batteryLevel = ((BatteryManager) getSystemService("batterymanager")).getIntProperty(4);
                Tag tag = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("mBatterySaveOn changed and current batteryLevel = ");
                stringBuilder2.append(batteryLevel);
                Log.i(tag, stringBuilder2.toString());
                this.mBatteryLevel = batteryLevel;
                if (this.mBatteryLevel > 15) {
                    this.currentBatteryStatus = 0;
                    if (!(this.mCurrentModeIndex == 5 || this.mCurrentModeIndex == 14)) {
                        ModuleController moduleController = getCurrentModuleController();
                        this.mCameraAppUI.applyModuleSpecs(moduleController.getHardwareSpec(), moduleController.getBottomBarSpec());
                    }
                } else if (this.mBatteryLevel <= 15 && this.mBatteryLevel > 0) {
                    this.currentBatteryStatus = 1;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("currentBatteryStatus: ");
                    stringBuilder3.append(this.currentBatteryStatus);
                    android.util.Log.d("onWindowFocusChanged", stringBuilder3.toString());
                    if (!"off".equals(this.mSettingsManager.getString(getCameraScope(), Keys.KEY_FLASH_MODE))) {
                        this.mSettingsManager.set(getCameraScope(), Keys.KEY_FLASH_MODE, "off");
                    }
                    if (!"off".equals(this.mSettingsManager.getString(getCameraScope(), Keys.KEY_VIDEOCAMERA_FLASH_MODE))) {
                        this.mSettingsManager.set(getCameraScope(), Keys.KEY_VIDEOCAMERA_FLASH_MODE, "off");
                    }
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("currentBatteryStatus1: ");
                    stringBuilder4.append(this.currentBatteryStatus);
                    android.util.Log.d("onWindowFocusChanged", stringBuilder4.toString());
                    getButtonManager().disableButton(0);
                    getButtonManager().disableButton(1);
                } else if (this.mBatteryLevel <= 0) {
                    this.currentBatteryStatus = 2;
                }
                this.mCameraAppUI.setViewFinderLayoutVisibile(false);
                SharedUtil.saveIntValue("currentBatteryStatus", this.currentBatteryStatus);
            }
        }
        if (CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_HELP_TIP_TUTORIAL, false) && hasFocus && this.mRequestPermissionsFinished && this.mHelpTipsManager != null) {
            takeHelpTipTutorial();
        }
    }

    private void takeHelpTipTutorial() {
        boolean isPromptWelcome = this.mSettingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_WELCOME_FINISHED, false) ^ true;
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("helptip isPromptWelcome = ");
        stringBuilder.append(isPromptWelcome);
        stringBuilder.append(",mCurrentModeIndex =");
        stringBuilder.append(this.mCurrentModeIndex);
        Log.e(tag, stringBuilder.toString());
        if (isPromptWelcome && this.mCurrentModeIndex == getResources().getInteger(R.integer.camera_mode_photo)) {
            if (this.mHelpTipsManager != null) {
                this.mHelpTipsManager.createAndShowHelpTip(0, true);
            }
        } else if (this.mHelpTipsManager != null) {
            this.mHelpTipsManager.checkAlarmTaskHelpTip();
        }
    }

    public void startBatteryInfoChecking(OnBatteryLowListener l) {
        this.mBatteryLowListener = l;
    }

    public void stopBatteryInfoChecking() {
        this.mBatteryLowListener = null;
    }

    public int getCurrentBatteryStatus() {
        return this.currentBatteryStatus;
    }

    public boolean currentBatteryStatusOK() {
        boolean z = true;
        if (!CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_LOW_BATTERY_FEATURE_INDEPENDENT, true) && !CameraUtil.isBatterySaverEnabled(this)) {
            return true;
        }
        if (this.currentBatteryStatus != 0) {
            z = false;
        }
        return z;
    }

    private void batteryStatusChange(int status) {
        switch (status) {
            case 0:
                if (this.mBatteryWarningDialog != null && this.mBatteryWarningDialog.isShowing()) {
                    this.mCameraAppUI.setViewFinderLayoutVisibile(false);
                    this.mBatteryWarningDialog.dismiss();
                }
                if (this.mBatteryLowDialog != null && this.mBatteryLowDialog.isShowing()) {
                    this.mCameraAppUI.setViewFinderLayoutVisibile(false);
                    this.mBatteryLowDialog.dismiss();
                }
                ModuleController moduleController = getCurrentModuleController();
                this.mCameraAppUI.applyModuleSpecs(moduleController.getHardwareSpec(), moduleController.getBottomBarSpec());
                return;
            case 1:
                if (this.mBatteryLowDialog != null && this.mBatteryLowDialog.isShowing()) {
                    this.mCameraAppUI.setViewFinderLayoutVisibile(false);
                    this.mBatteryLowDialog.dismiss();
                    return;
                }
                return;
            default:
                return;
        }
    }

    /* Access modifiers changed, original: protected */
    public void startInnerStorageChecking(OnInnerStorageLowListener listener) {
        this.mInnerStorageLowListener = listener;
        new Thread(new Runnable() {
            public void run() {
                long INTERVAL = 5000;
                while (CameraActivity.this.mInnerStorageLowListener != null) {
                    new innerStorageCheckTask(CameraActivity.this.mInnerStorageLowListener).execute(new Void[0]);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /* Access modifiers changed, original: protected */
    public void stopInnerStorageChecking() {
        this.mInnerStorageLowListener = null;
    }

    public void intentReviewCancel() {
        if (this.mCurrentModule != null && (this.mCurrentModule instanceof PhotoModule)) {
            ((PhotoModule) this.mCurrentModule).intentReviewCancel();
        } else if (this.mCurrentModule != null && (this.mCurrentModule instanceof VideoModule)) {
            ((VideoModule) this.mCurrentModule).intentReviewCancel();
        }
    }

    public void intentReviewDone() {
        if (this.mCurrentModule != null && (this.mCurrentModule instanceof PhotoModule)) {
            ((PhotoModule) this.mCurrentModule).intentReviewDone();
        } else if (this.mCurrentModule != null && (this.mCurrentModule instanceof VideoModule)) {
            ((VideoModule) this.mCurrentModule).intentReviewDone();
        }
    }

    public void intentReviewRetake() {
        if (this.mCurrentModule != null && (this.mCurrentModule instanceof PhotoModule)) {
            ((PhotoModule) this.mCurrentModule).intentReviewRetake();
        }
    }

    public void intentReviewPlay() {
        if (this.mCurrentModule != null && (this.mCurrentModule instanceof VideoModule)) {
            ((VideoModule) this.mCurrentModule).intentReviewPlay();
        }
    }

    public void onBoomPressed() {
        Log.d(TAG, "Document onBoomPressed");
    }

    public void onBoomLongPress() {
        Log.d(TAG, "Document onBoomLongPress");
    }

    public void onBoomDoublePress() {
        Log.d(TAG, "Document onBoomDoublePress");
    }

    public void switchToMode(int index) {
        switchToMode(index, true);
    }

    public void switchToMode(int index, boolean disableAnimation) {
        if (this.mCurrentModeIndex != index && this.mModeStripView != null) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Switch to mode ");
            stringBuilder.append(index);
            Log.e(tag, stringBuilder.toString());
            onModeSelecting(disableAnimation);
            this.mCameraAppUI.setModeStripViewVisibility(true);
            this.mModeStripView.setCurrentModeWithModeIndex(index);
        }
    }

    public void setSecureFyuseModule(boolean isFyuse) {
        this.mSecureFyuseModule = isFyuse;
    }

    /* Access modifiers changed, original: protected */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (this.mCurrentModeIndex == getResources().getInteger(R.integer.camera_mode_parallax) && this.mCurrentModule != null && (this.mCurrentModule instanceof FyuseModule)) {
            ((FyuseModule) this.mCurrentModule).onActivityResult(requestCode, resultCode, data);
        }
        if (requestCode == 1000) {
            CameraUtil.backFromGpsSetting(this, getSettingsManager());
        }
        if (resultCode == -1 && requestCode == 110) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onActivityResult createAccessIntent OK requestCode = ");
            stringBuilder.append(requestCode);
            stringBuilder.append(" data = ");
            stringBuilder.append(data);
            stringBuilder.append(" resultCode = ");
            stringBuilder.append(resultCode);
            Log.d(tag, stringBuilder.toString());
            if (data != null) {
                getContentResolver().takePersistableUriPermission(data.getData(), data.getFlags() & 3);
                StorageUtils.start2CreateDir(this.mAppContext);
            }
        }
    }

    private boolean isExternalStorageAvailable() {
        return PermissionsUtil.isPermissionGranted(this, PermissionsUtil.PERMS_READ_EXTERNAL_STORAGE) && PermissionsUtil.isPermissionGranted(this, PermissionsUtil.PERMS_WRITE_EXTERNAL_STORAGE);
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissions != null && grantResults != null) {
            if (permissions.length == 0 || grantResults.length == 0) {
                if (PermissionsUtil.inRequesting()) {
                    final RequestingPerms request = PermissionsUtil.getRequestingPerms();
                    this.mMainHandler.postDelayed(new Runnable() {
                        public void run() {
                            if (request.code != 0 && request.perms != null) {
                                PermissionsUtil.requestPermissions(CameraActivity.this, request.code, request.perms);
                            }
                        }
                    }, 300);
                }
                return;
            }
            int i;
            if (PermissionsUtil.inRequesting()) {
                PermissionsUtil.getRequestingPerms().clear();
            }
            int i2 = 0;
            if (PermissionsUtil.DEBUG) {
                for (i = 0; i < permissions.length; i++) {
                    Tag tag = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(i);
                    stringBuilder.append(" For permission ");
                    stringBuilder.append(permissions[i]);
                    stringBuilder.append(",  grantResults is ");
                    stringBuilder.append(grantResults[i]);
                    Log.i(tag, stringBuilder.toString());
                }
            }
            if (requestCode <= 7) {
                while (true) {
                    i = i2;
                    if (i >= grantResults.length) {
                        onCriticalPermsGranted(requestCode);
                        PermissionsUtil.dismissDialogPermsNeeded();
                        checkNonCriticalPermissions();
                        break;
                    } else if (grantResults[i] != 0) {
                        onCriticalPermsDenied(requestCode);
                        return;
                    } else {
                        i2 = i + 1;
                    }
                }
            } else if (requestCode == 8) {
                if (grantResults == null || grantResults[0] != 0) {
                    onLocationPermsDenied();
                } else {
                    onLocationPermsGranted();
                }
                this.mRequestPermissionsFinished = true;
            }
        }
    }

    /* Access modifiers changed, original: protected */
    public boolean checkCriticalPermissions() {
        return PermissionsUtil.checkCriticalPerms(this);
    }

    private void onCriticalPermsDenied(int requestCode) {
        PermissionsUtil.dismissDialogPermsNeeded();
        PermissionsUtil.showDialogGrantAccess(this);
    }

    private void onCriticalPermsGranted(int requestCode) {
        CriticalPermsStatus status = PermissionsUtil.getCriticalPermsStatus(requestCode);
        if (status.cameraGranted) {
            onCameraPermsGranted();
        }
        if (status.storageGranted) {
            onStoragePermsGranted();
        }
        if (status.microphoneGranted) {
            onMircophonePermsGranted();
        }
    }

    private void onCameraPermsGranted() {
    }

    private void onStoragePermsGranted() {
        if (!(isSecureCamera() || isCaptureIntent())) {
            onLastMediaDataUpdated();
        }
        updateStorageSpaceAndHint(null);
    }

    private void onMircophonePermsGranted() {
    }

    /* Access modifiers changed, original: protected */
    public boolean checkNonCriticalPermissions() {
        return PermissionsUtil.checkNonCriticalPerms(this);
    }

    private void onLocationPermsDenied() {
        PermissionsUtil.dismissDialogLocation();
        Keys.setLocation(getSettingsManager(), false, getLocationManager());
        ToastUtil.showToast((Context) this, getString(R.string.location_toast), 1);
    }

    private void onLocationPermsGranted() {
        PermissionsUtil.dismissDialogLocation();
        Keys.setLocation(getSettingsManager(), true, getLocationManager());
        CameraUtil.gotoGpsSetting(this, getSettingsManager(), R.drawable.gps_grey);
    }

    public CaptureLayoutHelper getCaptureLayoutHelper() {
        return this.mCameraAppUI.getCaptureLayoutHelper();
    }

    public void requestAccessPer(Context context) {
        String storagePath = StorageUtilProxy.getStoragePath(context, true);
        if (storagePath == null) {
            Log.i(TAG, "requestAccessPer: there is no SD inert, give up opreation");
            return;
        }
        for (StorageVolume volume : ((StorageManager) context.getSystemService("storage")).getStorageVolumes()) {
            File volumePath = StorageUtilProxy.getPathFromReflectFeild(volume);
            if (!volume.isPrimary() && volumePath != null && Environment.getExternalStorageState(volumePath).equals("mounted") && volumePath.toString().contains(storagePath)) {
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ready to createAccessIntent for : ");
                stringBuilder.append(volumePath);
                Log.i(tag, stringBuilder.toString());
                Intent intent = volume.createAccessIntent(null);
                if (intent != null) {
                    startActivityForResult(intent, 110);
                }
            }
        }
    }
}
