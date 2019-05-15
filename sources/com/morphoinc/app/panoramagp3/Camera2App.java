package com.morphoinc.app.panoramagp3;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.CaptureResult.Key;
import android.hardware.camera2.TotalCaptureResult;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioAttributes;
import android.media.AudioAttributes.Builder;
import android.media.AudioManager;
import android.media.ExifInterface;
import android.media.SoundPool;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Images.Media;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.SimpleArrayMap;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.util.SizeF;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.adobe.xmp.XMPConst;
import com.android.external.ExtendKey;
import com.hmdglobal.app.camera.CameraActivity;
import com.hmdglobal.app.camera.PanoCaptureModule;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.SDCard;
import com.hmdglobal.app.camera.SoundPlayer;
import com.hmdglobal.app.camera.Storage;
import com.hmdglobal.app.camera.Thumbnail;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.app.CameraApp;
import com.hmdglobal.app.camera.exif.ExifInterface.GpsLatitudeRef;
import com.hmdglobal.app.camera.exif.ExifInterface.GpsLongitudeRef;
import com.hmdglobal.app.camera.provider.InfoTable;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.ui.GridLines;
import com.hmdglobal.app.camera.ui.ModuleLayoutWrapper.OnAllViewRemovedListener;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.hmdglobal.app.camera.util.CustomUtil;
import com.hmdglobal.app.camera.util.ToastUtil;
import com.morphoinc.app.LogFilter;
import com.morphoinc.app.camera_states.Camera1PreviewState;
import com.morphoinc.app.camera_states.Camera1UnlockFocusState;
import com.morphoinc.app.camera_states.CameraState;
import com.morphoinc.app.camera_states.IMorphoPanoramaGP3Callback;
import com.morphoinc.app.camera_states.PreviewState;
import com.morphoinc.app.camera_states.UnlockFocusState;
import com.morphoinc.app.panoramagp3.Camera2ParamsFragment.ICamera2ParamsFragmentEvent;
import com.morphoinc.app.panoramagp3.MorphoCameraBase.IMorphoCameraListener;
import com.morphoinc.app.panoramagp3.MorphoPanoramaGP3.GalleryInfoData;
import com.morphoinc.app.panoramagp3.MorphoPanoramaGP3.GravityParam;
import com.morphoinc.app.panoramagp3.MorphoPanoramaGP3.InitParam;
import com.morphoinc.app.panoramagp3.PanoramaState.IPanoramaStateEventListener;
import com.morphoinc.app.panoramagp3.SaveThread.ISaveThreadEventListener;
import com.morphoinc.core.Error;
import com.morphoinc.core.MorphoSensorFusion.SensorData;
import com.morphoinc.utils.VideoRec.VideoRecorderRaw;
import com.morphoinc.utils.VideoRec.VideoRecorderRaw.RawRenderListener;
import com.morphoinc.utils.VideoRec.VideoRecorderRaw.RawRenderListener2;
import com.morphoinc.utils.multimedia.MediaProviderUtils;
import com.morphoinc.utils.os.BuildPropJNI;
import com.morphoinc.utils.os.BuildUtil;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Camera2App implements IPanoramaSaveListener, SensorEventListener, IMorphoPanoramaGP3Callback, IMorphoCameraListener {
    private static final boolean ADD_INFO_TO_OUTPUT_FILENAME = true;
    private static final boolean ALWAYS_AUTO_MODE_AFTER_STARTUP = true;
    private static final int[][] ASPECT_TABLE = new int[][]{new int[]{16, 9}, new int[]{4, 3}, new int[]{1, 1}};
    private static final String CAMERA2_PARAM_FILE = "camera2.ini";
    private static final int CAMERA_ID_AFTER_INSTALL = -2;
    private static final int CAMERA_ID_AFTER_RESTART = -1;
    private static final int DEFAULT_PREVIEW_HEIGHT = 720;
    private static final int DEFAULT_PREVIEW_HEIGHT2 = 2268;
    private static final int DEFAULT_PREVIEW_HEIGHT3 = 1836;
    private static final int DEFAULT_PREVIEW_WIDTH = 960;
    private static final int DEFAULT_PREVIEW_WIDTH2 = 4032;
    private static final int DEFAULT_PREVIEW_WIDTH3 = 3264;
    private static final int DEFAULT_SETTING_ANTI_BANDING = 1;
    private static final int DEFAULT_SETTING_ANTI_FLICKER_FREQ = 0;
    private static final double DEFAULT_SETTING_AOV_GAIN = 1.0d;
    private static final boolean DEFAULT_SETTING_AUTO_AE_LOCK = true;
    private static final boolean DEFAULT_SETTING_AUTO_WB_LOCK = true;
    private static int DEFAULT_SETTING_CAMERA_ID = 0;
    private static final int DEFAULT_SETTING_CAPTURE_MODE = 0;
    private static final int DEFAULT_SETTING_CAPTURE_SIZE_INDEX = 0;
    private static final String DEFAULT_SETTING_COLOR_CORRECTION_MODE = "-1";
    private static final boolean DEFAULT_SETTING_DEFORM = false;
    private static double DEFAULT_SETTING_DISTORTION_K1 = Camera2ParamsFragment.TARGET_EV;
    private static double DEFAULT_SETTING_DISTORTION_K2 = Camera2ParamsFragment.TARGET_EV;
    private static double DEFAULT_SETTING_DISTORTION_K3 = Camera2ParamsFragment.TARGET_EV;
    private static double DEFAULT_SETTING_DISTORTION_K4 = Camera2ParamsFragment.TARGET_EV;
    private static final double DEFAULT_SETTING_DRAW_THRESHOLD = 0.5d;
    private static final String DEFAULT_SETTING_EDGE_MODE = "-1";
    private static final int DEFAULT_SETTING_FOCUS_MODE = 0;
    private static final double DEFAULT_SETTING_INPUT_MOVIE_FPS = 30.0d;
    private static final boolean DEFAULT_SETTING_LUMINANCE_CORRECTION = false;
    private static final boolean DEFAULT_SETTING_MAKE_360 = false;
    private static int DEFAULT_SETTING_MOTION_DETECTION_MODE = 0;
    private static final String DEFAULT_SETTING_NOISE_REDUCTION_MODE = "-1";
    private static final boolean DEFAULT_SETTING_NR_AUTO = true;
    private static final int DEFAULT_SETTING_PREVIEW_SIZE_INDEX = 0;
    private static final int DEFAULT_SETTING_PROJECTION_MODE = 0;
    private static final double DEFAULT_SETTING_ROTATION_RATIO = 1.0d;
    private static final int DEFAULT_SETTING_SAVE_INPUT_IMAGES = 0;
    private static final double DEFAULT_SETTING_SEAMSEARCH_RATIO = 1.0d;
    private static final int DEFAULT_SETTING_SENSOR_MODE = 0;
    private static int DEFAULT_SETTING_SENSOR_USE_MODE = 0;
    private static final String DEFAULT_SETTING_SHADING_MODE = "-1";
    private static final String DEFAULT_SETTING_TONEMAP_MODE = "-1";
    private static final boolean DEFAULT_SETTING_USE_60FPS = false;
    private static final boolean DEFAULT_SETTING_USE_CAMERA2 = true;
    private static final boolean DEFAULT_SETTING_USE_GPS = true;
    private static final boolean DEFAULT_SETTING_USE_GRAVITY_SENSOR = false;
    private static final boolean DEFAULT_SETTING_USE_OIS = BuildUtil.isPixel2();
    private static final int DEFAULT_SETTING_USE_ROUND_AUTO_END = 0;
    private static final boolean DEFAULT_SETTING_USE_WDR2 = false;
    private static final double DEFAULT_SETTING_ZROTATION_COEFF = 0.9d;
    private static final SimpleArrayMap<String, Integer> DEFAULT_UI_CONTROL_MODES = new SimpleArrayMap();
    private static final boolean ENCODE_ON_BACKGROUND = true;
    private static final boolean ENFORCED_SHUTTER_SOUND = false;
    private static final boolean GET_PREVIEW_IMAGE_EVERY_FRAME = false;
    private static final double GOAL_ANGLE = 360.0d;
    private static final int INITIAL = -1;
    public static final String INTENT_FILENAME = "file_name";
    private static final boolean LIMIT_DIRECTION = false;
    private static final String LOG_TAG = "Camera2App";
    private static final int NOT_FOUND = -2;
    private static final String PREF_KEY = "ApplicationPreference";
    private static final String PREF_KEY_VIEW_ANGLE_CAMERA_ID = "VIEW_ANGLE_CAMERA_ID";
    private static final String PREF_KEY_VIEW_ANGLE_H = "VIEW_ANGLE_H";
    private static final String PREF_KEY_VIEW_ANGLE_V = "VIEW_ANGLE_V";
    private static final float PREVIEW_LONG_SIDE_CROP_RATIO = 0.75f;
    private static final boolean PREVIEW_SPREAD_BOTH_SIDES = BuildUtil.isSony();
    private static final boolean PRINT_PROCESSING_TIME = false;
    private static final Object PreviewImageSynchronizedObject = new Object();
    private static final int SENSOR_MODE_GYROSCOPE = 1;
    private static final int SENSOR_MODE_GYROSCOPE_UNCALIBRATED = 2;
    private static final int SENSOR_MODE_GYROSCOPE_WITH_ACCELEROMETER = 4;
    private static final int SENSOR_MODE_MODE_USE_GYROSCOPE_AND_ROTATION_VECTOR = 5;
    private static final int SENSOR_MODE_OFF = 0;
    private static final int SENSOR_MODE_ROTATION_VECTOR = 3;
    private static final int SETTING_ANTI_BANDING_50HZ = 2;
    private static final int SETTING_ANTI_BANDING_60HZ = 3;
    private static final int SETTING_ANTI_BANDING_AUTO = 1;
    private static final int SETTING_ANTI_BANDING_OFF = 0;
    private static final int SETTING_ANTI_FLICKER_50HZ = 1;
    private static final int SETTING_ANTI_FLICKER_60HZ = 2;
    private static final int SETTING_ANTI_FLICKER_UNKNOWN = 0;
    private static final String SETTING_FILE_NAME = "setting.txt";
    private static final int SETTING_FOCUS_MODE_AUTO = 0;
    private static final int SETTING_FOCUS_MODE_INFINITY = 1;
    private static final String SETTING_KEY_ANGLE = "angle";
    private static final String SETTING_KEY_ANTI_BANDING = "anti_banding(0,OFF 1,AUTO 2,50Hz 3,60Hz)";
    private static final String SETTING_KEY_ANTI_FLICKER_FREQ = "anti_flicker_freq(0,Unknown 1,50Hz 2,60Hz)";
    private static final String SETTING_KEY_ATTACH_NUM_DIRECTION_UNDECIDED = "attached num during direction-undecided";
    private static final String SETTING_KEY_AUTO_AE_LOCK = "auto_ae_lock";
    private static final String SETTING_KEY_AUTO_WB_LOCK = "auto_wb_lock";
    private static final String SETTING_KEY_BUILD_MODEL = "build_model";
    private static final String SETTING_KEY_CAMERA_ID = "camera_id";
    private static final String SETTING_KEY_CAPTURE_MODE = "capture_mode";
    private static final String SETTING_KEY_COLOR_CORRECTION_MODE = "color_correction_mode";
    private static final String SETTING_KEY_EDGE_MODE = "edge_mode";
    private static final String SETTING_KEY_FORMAT = "image format";
    private static final String SETTING_KEY_HEIGHT = "height";
    private static final String SETTING_KEY_MAKE_360 = "make_360";
    private static final String SETTING_KEY_MAX_HEIGHT = "max height";
    private static final String SETTING_KEY_MAX_WIDTH = "max width";
    private static final String SETTING_KEY_MOTION_DETECTION_MODE = "motion_detection_mode";
    private static final String SETTING_KEY_NOISE_REDUCTION_MODE = "noise_reduction_mode";
    private static final String SETTING_KEY_NR_AUTO = "nr auto";
    private static final String SETTING_KEY_NR_STRENGTH = "nr strength";
    private static final String SETTING_KEY_PANORAMA_DIRECTION = "panorama direction(0,left 1,right 2,up 3,down)";
    private static final String SETTING_KEY_PREVIEW_SCALE = "preview scale";
    private static final String SETTING_KEY_PROJECTION_MODE = "projection_mode";
    private static final String SETTING_KEY_RENDERING_AREA = "rendering area";
    private static final String SETTING_KEY_SENSOR_MODE = "sensor_mode";
    private static final String SETTING_KEY_SENSOR_USE_MODE = "sensor_use_mode";
    private static final String SETTING_KEY_SHADING_MODE = "shading_mode";
    private static final String SETTING_KEY_TONEMAP_MODE = "tonemap_mode";
    private static final String SETTING_KEY_UNSHARP_STRENGTH = "unsharp strength";
    private static final String SETTING_KEY_USE_60FPS = "use_60fps";
    private static final String SETTING_KEY_USE_CAMERA2 = "use_camera2";
    private static final String SETTING_KEY_USE_GRAVITY_SENSOR = "use_gravity_sensor";
    private static final String SETTING_KEY_USE_ROUND_AUTO_END = "360_auto_end";
    private static final String SETTING_KEY_USE_WDR2 = "use_wdr2";
    private static final String SETTING_KEY_WIDTH = "width";
    private static final int SETTING_MOTION_DETECTION_MODE_FAST = 0;
    private static final int SETTING_MOTION_DETECTION_MODE_HQ = 1;
    private static final int SETTING_PROJECTION_MODE_CENTRAL_CYLINDRICAL = 1;
    private static final int SETTING_PROJECTION_MODE_CENTRAL_CYLINDRICAL2 = 2;
    private static final int SETTING_PROJECTION_MODE_EQUIRECTANGULAR = 0;
    private static final int SETTING_ROUND_AUTO_END_GYROSCOPE = 1;
    private static final int SETTING_ROUND_AUTO_END_MAGNETIC_FIELD = 2;
    private static final int SETTING_ROUND_AUTO_END_OFF = 0;
    private static final int SETTING_SAVE_INPUT_IMAGES_MOVIE = 2;
    private static final int SETTING_SAVE_INPUT_IMAGES_MOVIE_SPECIFY_FPS = 1;
    private static final int SETTING_SAVE_INPUT_IMAGES_OFF = 0;
    private static final int SETTING_SAVE_INPUT_IMAGES_RAW = 3;
    private static final int SETTING_SENSOR_MODE_OFF = 0;
    private static final String SETTING_SEPARATOR = " : ";
    private static final int SLEEP_MICROSEC = (BuildUtil.isSony() ? 5000 : 1000);
    private static final long SLEEP_MILLISEC = ((long) (SLEEP_MICROSEC / 1000));
    private static final int SLEEP_NANOSEC = ((SLEEP_MICROSEC % 1000) * 1000);
    private static final boolean STABILIZE_PREVIEW_FRAME = true;
    private static final String TAG = "Camera2App";
    private static final int UI_CONTROL_MODE1 = 0;
    private static final int UI_CONTROL_MODE2 = 1;
    private static final int UI_CONTROL_MODE3 = 2;
    private static final boolean USE_FILE_DESCRIPTOR = false;
    private static final Object mRecoderLock = new Object();
    private static final CaptureImage sAttachExit = new Camera2Image(null);
    private ImageView back_tophoto;
    private ImageButton baseImage;
    private GridLines gridLines;
    private boolean isInvalidDir;
    private BroadcastReceiver localBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[localBroadcastReceiver] action = ");
            stringBuilder.append(intent.getAction());
            stringBuilder.append(" Extra = ");
            stringBuilder.append(intent.getStringExtra(ButtonsFragment.ACTION_EXTRA));
            LogFilter.d("Camera2App", stringBuilder.toString());
            if (ButtonsFragment.SLIP_UP_ACTION.equals(intent.getAction()) && ButtonsFragment.ACTION_START.equals(intent.getStringExtra(ButtonsFragment.ACTION_EXTRA))) {
                LocalBroadcastManager.getInstance(Camera2App.this.mActivity).unregisterReceiver(Camera2App.this.localBroadcastReceiver);
                Camera2App.this.backToPhoto();
                Intent endIntent = new Intent(ButtonsFragment.SLIP_UP_ACTION);
                endIntent.putExtra(ButtonsFragment.ACTION_EXTRA, ButtonsFragment.ACTION_END);
                LocalBroadcastManager.getInstance(Camera2App.this.mActivity).sendBroadcast(endIntent);
            }
        }
    };
    private LayoutParams lpp;
    private final double[] mACMatrix = new double[9];
    private Sensor mAccelerometer = null;
    private final CameraActivity mActivity;
    private int mAngle;
    private double[] mAovs;
    private AppController mAppController;
    private int mArrowDir;
    private final LinkedBlockingQueue<CaptureImage> mAttachImageQueue = new LinkedBlockingQueue();
    private long mAttachNumDirectionUndecided;
    private AudioManager mAudioManager;
    private ButtonsFragment mButtonsFragment;
    private final Camera2ImageQualitySettings[] mCamera2ImageQualitySettings = new Camera2ImageQualitySettings[5];
    private Camera2ParamsFragment mCamera2ParamsFragment;
    private int mCamera2ParamsFragmentSelectedMode;
    private int mCameraIdTmp = -1;
    private int mCameraOrientation;
    private int mCaptureModeTmp = -1;
    private final PanoCaptureModule mController;
    private int mCurOrientation = -1;
    private View mCurPreviewFrame;
    private SensorInfoManager mCurrentSensorInfoManager;
    private long[] mDateTaken = new long[2];
    private final Size mDefaultPreviewSize;
    private DirectionFunction mDirectionFunction;
    private final ExecutorService mExecutor = Executors.newCachedThreadPool();
    private long[] mExposureTime = new long[2];
    private int mFocusedSoundId;
    private String mFolderPath;
    private boolean mGranted = false;
    private float[] mGravities;
    private final double[] mGyroMatrix = new double[9];
    private Sensor mGyroscope = null;
    private Sensor mGyroscopeUncalibrated = null;
    private final Handler mHandler;
    private String mHardwareLevel;
    private String mImageFormat;
    private final InitParam mInitParam = new InitParam();
    private String mInputFolderPath;
    private final InputSaveState mInputSaveState = new InputSaveState(false);
    private boolean mIsFrontCamera;
    private boolean mIsPreviewState = false;
    private boolean mIsSensorAverage;
    private boolean mIsTvLock;
    private OnAllViewRemovedListener mListener;
    private PanoramaGP3LocationManager mLocationManager;
    private Sensor mMagnetic;
    private int mMaxHeight;
    private int mMaxWidth;
    private RelativeLayout mMenuTopView;
    private Bitmap mMiniPreviewBitmapForCamera1;
    private ImageView mMiniPreviewImageView;
    private Matrix mMiniPreviewMatrix;
    private MorphoCameraBase mMorphoCamera = null;
    private MorphoPanoramaGP3 mMorphoPanoramaGP3;
    private AlertDialog mMountedDialog;
    private boolean mNeedToSet30Fps;
    private int mNumEncodedFrames;
    private int mOisShiftPixelXIndex = -1;
    private int mOisShiftPixelYIndex = -1;
    private int mOisTimestampIndex = -1;
    private MyOrientationEventListener mOrientationEventListener = null;
    private FrameLayout mPanoramaFrameView;
    private PanoramaState mPanoramaState;
    private ImageView mPreviewArrow;
    private Bitmap mPreviewBitmap = null;
    private Bitmap mPreviewFitBitmap = null;
    private Canvas mPreviewFitBitmapCanvas;
    private Paint mPreviewFitBitmapPaint;
    private Matrix mPreviewFitMatrix;
    private FrameLayout mPreviewFrame;
    private ImageView mPreviewImageView;
    private View mPreviewLine1;
    private View mPreviewLine2;
    private View mPreviewLine3;
    private final double[] mRVMatrix = new double[9];
    private final RawRenderListener mRawRenderListener = new RawRenderListener() {
        public int onDraw(ByteBuffer buffer, CaptureImage srcImage) {
            if (Camera2App.this.mMorphoPanoramaGP3 != null) {
                return MorphoPanoramaGP3.renderByteBuffer(buffer, srcImage);
            }
            return 0;
        }
    };
    private final RawRenderListener2 mRawRenderListener2 = new RawRenderListener2() {
        public void onDraw(byte[] buffer, CaptureImage srcImage) {
            MorphoPanoramaGP3.renderByteArrayForEncoder(buffer, srcImage);
        }
    };
    private long mRecordTimestampStart;
    private VideoRecorderRaw mRecorder;
    private View mRootView;
    private Sensor mRotationVector = null;
    private RoundDetector mRoundDetector;
    private int mSensorAspectIndex = 0;
    private int mSensorCnt;
    private SensorFusion mSensorFusion = null;
    private int mSensorFusionMode;
    private ArrayList<SensorInfoManager> mSensorInfoManagerList;
    private SensorManager mSensorManager;
    private int[] mSensorSensitivity = new int[2];
    private SensorSensitivityAverageManager mSensorSensitivityAverageManager = new SensorSensitivityAverageManager(this, null);
    private HandlerThread mSensorThread = null;
    private final Settings mSettings = new Settings();
    private boolean mShaked = false;
    private final ShotSettings mShotSettings = new ShotSettings(this, null);
    private SoundPlayer mSoundPlayer;
    private SoundPool mSoundPool;
    private boolean mStorageLocationCheck = false;
    private final Callback mSurfaceListener = new Callback() {
        public void surfaceCreated(SurfaceHolder holder) {
            MorphoCamera1 morphoCamera = (MorphoCamera1) Camera2App.this.mMorphoCamera;
            if (morphoCamera.openCamera(holder)) {
                Camera2App.this.mViewAngleH = morphoCamera.viewAngleH;
                Camera2App.this.mViewAngleV = morphoCamera.viewAngleV;
                Camera2App.this.getCamera2ParamsFragment().initializeUI(morphoCamera.cameraInfo(), false);
                Camera2App.this.makeEngineParam();
            }
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            ((MorphoCamera1) Camera2App.this.mMorphoCamera).startPreview(Camera2App.this.getDisplayRotation());
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
        }
    };
    private final SurfaceTextureListener mSurfaceTextureListener = new SurfaceTextureListener() {
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            LayoutParams lp;
            LayoutParams framelp;
            CameraInfo cameraInfo = Camera2App.this.mMorphoCamera.cameraInfo();
            cameraInfo.setCaptureWidth(2400);
            cameraInfo.setCaptureHeight(1800);
            Point size = new Point();
            Camera2App.this.mActivity.getWindowManager().getDefaultDisplay().getSize(size);
            int th;
            if (Camera2App.this.mActivity.getResources().getConfiguration().orientation == 1) {
                th = (int) (((float) size.x) * (((float) cameraInfo.getCaptureWidth()) / ((float) cameraInfo.getCaptureHeight())));
                lp = new LayoutParams(size.x, th, 17);
                framelp = new LayoutParams(size.x, th, 48);
                framelp.setMargins(0, Camera2App.this.mMenuTopView.getBottom(), 0, 0);
            } else {
                th = (int) (((float) size.y) * (((float) cameraInfo.getCaptureWidth()) / ((float) cameraInfo.getCaptureHeight())));
                lp = new LayoutParams(th, size.y, 17);
                framelp = new LayoutParams(th, size.y, 48);
            }
            Camera2App.this.mPanoramaFrameView.setLayoutParams(framelp);
            Camera2App.this.mTextureView.setLayoutParams(lp);
            Camera2App.this.layoutMiniPreview();
            Camera2App.this.openCamera();
            Camera2App.this.makeEngineParam();
        }

        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }

        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };
    private SurfaceView mSurfaceView;
    private TextureView mTextureView;
    private ImageView mThumbnailButton;
    private String mTimestampSource;
    private boolean mUseCamera1;
    private boolean mUseCamera1Tmp;
    private float mViewAngleH = 60.0f;
    private float mViewAngleV = 40.0f;
    private TextView mWarningMessage;
    private int mWarningTextDir;
    private TextView mWarningTextView;
    private FrameLayout pano_view;

    public class DeviceInfo {
        public double aov_h;
        public double aov_v;
        public String model = BuildUtil.getModel().replace(' ', '_');
        public double physical_height;
        public double physical_width;

        public DeviceInfo(double aov_h, double aov_v, double physical_width, double physical_height) {
            this.aov_h = aov_h;
            this.aov_v = aov_v;
            this.physical_width = physical_width;
            this.physical_height = physical_height;
        }
    }

    private class MyOrientationEventListener extends OrientationEventListener {
        public MyOrientationEventListener(Context context) {
            super(context);
        }

        public void onOrientationChanged(int orientation) {
            Camera2App.this.updatedOrientation(orientation);
        }
    }

    private class SensorSensitivityAverageManager {
        private int num;
        private int sum;

        private SensorSensitivityAverageManager() {
        }

        /* synthetic */ SensorSensitivityAverageManager(Camera2App x0, AnonymousClass1 x1) {
            this();
        }

        public void init() {
            this.num = 0;
            this.sum = 0;
        }

        public void add(int value) {
            this.sum += value;
            this.num++;
        }

        public int get() {
            if (this.num == 0) {
                return 0;
            }
            return Math.round((float) (this.sum / this.num));
        }
    }

    private class Settings {
        public int anti_banding;
        public int anti_flicker_freq;
        public int anti_flicker_mode;
        public double aov_gain;
        public double aov_x;
        public double aov_y;
        public boolean auto_ae_lock;
        public boolean auto_wb_lock;
        public int calcseam_pixnum;
        public int camera_id;
        public int capture_mode;
        public int capture_size_index;
        public int color_correction_mode;
        public double distortion_k1;
        public double distortion_k2;
        public double distortion_k3;
        public double distortion_k4;
        public double draw_threshold;
        public int edge_mode;
        public int focus_mode;
        public String hardware_level;
        public double input_movie_fps;
        public boolean make_360;
        public int motion_detection_mode;
        public int noise_reduction_mode;
        public boolean nr_auto;
        public int nr_strength;
        public int preview_size_index;
        public int projection_mode;
        public int rendering_area;
        public double rotation_ratio;
        public int save_input_images;
        public double seamsearch_ratio;
        public int sensor_mode;
        public int sensor_use_mode;
        public int shading_mode;
        public double shrink_ratio;
        public String timestamp_source;
        public int tonemap_mode;
        public int ui_control_mode;
        public int unsharp_strength;
        public boolean use_60fps;
        public boolean use_camera2;
        public boolean use_deform;
        public boolean use_gps;
        public boolean use_gravity_sensor;
        public boolean use_luminance_correction;
        public boolean use_ois;
        public int use_round_auto_end;
        public boolean use_wdr2;
        public double zrotation_coeff;

        public Settings() {
            if (BuildUtil.isG5() || BuildUtil.isV20()) {
                Camera2App.DEFAULT_SETTING_CAMERA_ID = 2;
            } else {
                Camera2App.DEFAULT_SETTING_CAMERA_ID = 0;
            }
            this.capture_size_index = 0;
            this.use_ois = Camera2App.DEFAULT_SETTING_USE_OIS;
            this.use_gps = true;
            this.save_input_images = 0;
            this.auto_ae_lock = true;
            this.auto_wb_lock = true;
            this.anti_banding = 1;
            this.anti_flicker_freq = 0;
            this.capture_mode = 0;
            this.rendering_area = 33;
            this.shrink_ratio = 7.5d;
            this.calcseam_pixnum = 0;
            this.aov_x = Camera2ParamsFragment.TARGET_EV;
            this.aov_y = Camera2ParamsFragment.TARGET_EV;
            this.use_deform = false;
            this.use_luminance_correction = false;
            this.seamsearch_ratio = 1.0d;
            this.zrotation_coeff = Camera2App.DEFAULT_SETTING_ZROTATION_COEFF;
            this.draw_threshold = Camera2App.DEFAULT_SETTING_DRAW_THRESHOLD;
            this.sensor_mode = 0;
            this.sensor_use_mode = Camera2App.DEFAULT_SETTING_SENSOR_USE_MODE;
            this.use_gravity_sensor = false;
            this.use_round_auto_end = 0;
            this.unsharp_strength = UnsharpStrengthPreference.DEFAULT_VALUE;
            this.nr_auto = true;
            this.nr_strength = 0;
            this.input_movie_fps = Camera2App.DEFAULT_SETTING_INPUT_MOVIE_FPS;
            this.aov_gain = 1.0d;
            this.distortion_k1 = Camera2App.DEFAULT_SETTING_DISTORTION_K1;
            this.distortion_k2 = Camera2App.DEFAULT_SETTING_DISTORTION_K2;
            this.distortion_k3 = Camera2App.DEFAULT_SETTING_DISTORTION_K3;
            this.distortion_k4 = Camera2App.DEFAULT_SETTING_DISTORTION_K4;
            this.rotation_ratio = 1.0d;
            this.ui_control_mode = 0;
            this.focus_mode = 0;
            this.use_camera2 = true;
            this.camera_id = Camera2App.DEFAULT_SETTING_CAMERA_ID;
            this.use_wdr2 = false;
            this.anti_flicker_mode = 1;
            this.projection_mode = 0;
            this.motion_detection_mode = Camera2App.DEFAULT_SETTING_MOTION_DETECTION_MODE;
            this.make_360 = false;
            this.use_60fps = false;
        }

        public void print() {
            Size[] previewSizes = ((CameraApp) Camera2App.this.mActivity.getApplication()).getSupportedPictureSizes();
            LogFilter.i("Camera2App", "-------- Settings --------");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("preview_size : ");
            stringBuilder.append(previewSizes[this.capture_size_index].getWidth());
            stringBuilder.append(" x ");
            stringBuilder.append(previewSizes[this.capture_size_index].getHeight());
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("use_ois : ");
            stringBuilder.append(this.use_ois);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("use_gps : ");
            stringBuilder.append(this.use_gps);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("save_input_images : ");
            stringBuilder.append(this.save_input_images);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("auto_ae_lock : ");
            stringBuilder.append(this.auto_ae_lock);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("auto_wb_lock : ");
            stringBuilder.append(this.auto_wb_lock);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("anti_banding : ");
            stringBuilder.append(this.anti_banding);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("anti_flicker_freq : ");
            stringBuilder.append(this.anti_flicker_freq);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("capture_mode : ");
            stringBuilder.append(this.capture_mode);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("use_deform : ");
            stringBuilder.append(this.use_deform);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("use_luminance_correction : ");
            stringBuilder.append(this.use_luminance_correction);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("seamsearch_ratio : ");
            stringBuilder.append(this.seamsearch_ratio);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("zrotation_coeff : ");
            stringBuilder.append(this.zrotation_coeff);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("draw_threshold : ");
            stringBuilder.append(this.draw_threshold);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("sensor_mode : ");
            stringBuilder.append(this.sensor_mode);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("sensor_use_mode : ");
            stringBuilder.append(this.sensor_use_mode);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("use_gravity_sensor : ");
            stringBuilder.append(this.use_gravity_sensor);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("360_auto_end : ");
            stringBuilder.append(this.use_round_auto_end);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("unsharp_strength : ");
            stringBuilder.append(this.unsharp_strength);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("nr_auto : ");
            stringBuilder.append(this.nr_auto);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("nr_strength : ");
            stringBuilder.append(this.nr_strength);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("color_correction_mode : ");
            stringBuilder.append(this.color_correction_mode);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("edge_mode : ");
            stringBuilder.append(this.edge_mode);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("noise_reduction_mode : ");
            stringBuilder.append(this.noise_reduction_mode);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("shading_mode : ");
            stringBuilder.append(this.shading_mode);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("tonemap_mode : ");
            stringBuilder.append(this.tonemap_mode);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("aov_gain : ");
            stringBuilder.append(this.aov_gain);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("distortion_k1 : ");
            stringBuilder.append(this.distortion_k1);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("distortion_k2 : ");
            stringBuilder.append(this.distortion_k2);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("distortion_k3 : ");
            stringBuilder.append(this.distortion_k3);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("distortion_k4 : ");
            stringBuilder.append(this.distortion_k4);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("rotation_ratio : ");
            stringBuilder.append(this.rotation_ratio);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("ui_control_mode : ");
            stringBuilder.append(this.ui_control_mode);
            LogFilter.i("Camera2App", stringBuilder.toString());
            String str = "Camera2App";
            stringBuilder = new StringBuilder();
            stringBuilder.append("focus_mode : ");
            stringBuilder.append(this.focus_mode == 0 ? "Auto" : "Infinity");
            LogFilter.i(str, stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("use_camera2 : ");
            stringBuilder.append(this.use_camera2);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("camera_id : ");
            stringBuilder.append(this.camera_id);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("use_wdr2 : ");
            stringBuilder.append(this.use_wdr2);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("projection_mode : ");
            stringBuilder.append(this.projection_mode);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("motion_detection_mode : ");
            stringBuilder.append(this.motion_detection_mode);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("make_360 : ");
            stringBuilder.append(this.make_360);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("use_60fps : ");
            stringBuilder.append(this.use_60fps);
            LogFilter.i("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("shrink_ratio : ");
            stringBuilder.append(this.shrink_ratio);
            LogFilter.i("Camera2App", stringBuilder.toString());
            LogFilter.i("Camera2App", "--------------------------");
        }

        public int getAntiBanding() {
            String antibandingValue = Keys.getAntibandingValue(Camera2App.this.mActivity.getSettingsManager());
            if (antibandingValue == null) {
                Log.d("Camera2App", "antibandingValue is null");
                return 4;
            }
            int ret;
            Object obj = -1;
            int hashCode = antibandingValue.hashCode();
            if (hashCode != 109935) {
                if (hashCode != 1628397) {
                    if (hashCode != 1658188) {
                        if (hashCode == 3005871 && antibandingValue.equals("auto")) {
                            obj = 1;
                        }
                    } else if (antibandingValue.equals("60hz")) {
                        obj = 3;
                    }
                } else if (antibandingValue.equals("50hz")) {
                    obj = 2;
                }
            } else if (antibandingValue.equals(ExtendKey.FLIP_MODE_OFF)) {
                obj = null;
            }
            switch (obj) {
                case null:
                    ret = 4;
                    break;
                case 1:
                    ret = 4;
                    break;
                case 2:
                    ret = 4;
                    break;
                case 3:
                    ret = 5;
                    break;
                default:
                    ret = 4;
                    break;
            }
            return ret;
        }

        public int getCaptureMode() {
            return Camera2App.this.mUseCamera1 ? 1 : this.capture_mode;
        }

        public boolean isInfinityFocus() {
            return this.focus_mode == 1;
        }
    }

    private class ShotSettings {
        public int edgeMode;
        public int noiseReductionMode;
        public int noiseReductionStrength;
        public int unsharpStrength;

        private ShotSettings() {
        }

        /* synthetic */ ShotSettings(Camera2App x0, AnonymousClass1 x1) {
            this();
        }
    }

    private class DecideDirection extends PanoramaState {
        private DecideDirectionAttach mAttachRunnable = null;

        private class DecideDirectionAttach extends AttachRunnable {

            private class DecideFailRunnable implements Runnable {
                private DecideFailRunnable() {
                }

                /* synthetic */ DecideFailRunnable(DecideDirectionAttach x0, AnonymousClass1 x1) {
                    this();
                }

                public void run() {
                    Camera2App.this.mMorphoCamera.cameraState().onCancel();
                    Camera2App.this.mMorphoCamera.cameraState().onStart();
                }
            }

            private class DecideRunnable implements Runnable {
                private DecideRunnable() {
                }

                /* synthetic */ DecideRunnable(DecideDirectionAttach x0, AnonymousClass1 x1) {
                    this();
                }

                public void run() {
                    if (Camera2App.this.mSettings.ui_control_mode == 2) {
                        synchronized (CameraConstants.CameraSynchronizedObject) {
                            runMain();
                        }
                        return;
                    }
                    runMain();
                }

                private void runMain() {
                    synchronized (CameraConstants.EngineSynchronizedObject) {
                        if (Camera2App.this.mMorphoPanoramaGP3 == null) {
                            Camera2App.this.mMorphoCamera.cameraState().onCancel();
                            Camera2App.this.mMorphoCamera.cameraState().onStart();
                            return;
                        }
                        if (BuildUtil.isSony()) {
                            Camera2App.this.mIsSensorAverage = false;
                            Camera2App.this.mSensorCnt = 0;
                        } else {
                            Camera2App.this.unregistGravitySensorListener();
                        }
                        Camera2App.this.mAttachNumDirectionUndecided = Camera2App.this.mMorphoPanoramaGP3.getAttachCount();
                        Camera2App.this.mPanoramaState = new PanoramaPreview(Camera2App.this);
                        Camera2App.this.mPanoramaState.setPanoramaStateEventListener(DecideDirection.this.listener);
                        DecideDirection.this.clearListener();
                    }
                }
            }

            private int getScaleV() {
                if (Camera2App.this.mSurfaceView == null || Camera2App.this.mSurfaceView.getVisibility() != 0) {
                    return Math.max(1, (((Camera2App.this.mMaxWidth + Camera2App.this.mTextureView.getHeight()) - 1) / Camera2App.this.mTextureView.getHeight()) * 2);
                }
                return Math.max(1, (((Camera2App.this.mMaxWidth + Camera2App.this.mSurfaceView.getHeight()) - 1) / Camera2App.this.mSurfaceView.getHeight()) * 2);
            }

            private int getScaleH() {
                if (Camera2App.this.mSurfaceView == null || Camera2App.this.mSurfaceView.getVisibility() != 0) {
                    return Math.max(1, (((Camera2App.this.mMaxHeight + Camera2App.this.mTextureView.getHeight()) - 1) / Camera2App.this.mTextureView.getHeight()) * 2);
                }
                return Math.max(1, (((Camera2App.this.mMaxHeight + Camera2App.this.mSurfaceView.getHeight()) - 1) / Camera2App.this.mSurfaceView.getHeight()) * 2);
            }

            /* JADX WARNING: Missing block: B:35:0x00f3, code skipped:
            if (com.morphoinc.app.panoramagp3.Camera2App.access$8200(r1.this$1.this$0) != false) goto L_0x010b;
     */
            /* JADX WARNING: Missing block: B:36:0x00f5, code skipped:
            r4 = new java.lang.StringBuilder();
            r4.append("mMorphoPanoramaGP3.attach error ret:");
            r4.append(r0);
            com.morphoinc.app.LogFilter.e("Camera2App", r4.toString());
     */
            /* JADX WARNING: Missing block: B:37:0x010b, code skipped:
            com.morphoinc.app.panoramagp3.Camera2App.access$200(r1.this$1.this$0).runOnUiThread(new com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.DecideDirectionAttach.DecideFailRunnable(r1, r2));
     */
            /* JADX WARNING: Missing block: B:41:0x011d, code skipped:
            return;
     */
            /* JADX WARNING: Missing block: B:48:0x0138, code skipped:
            r4 = 0;
     */
            /* JADX WARNING: Missing block: B:56:?, code skipped:
            createDirection(r4);
     */
            /* JADX WARNING: Missing block: B:60:0x0185, code skipped:
            if (com.morphoinc.app.panoramagp3.Camera2App.access$8400(r1.this$1.this$0).enabled() == false) goto L_0x0189;
     */
            /* JADX WARNING: Missing block: B:62:?, code skipped:
            java.lang.Thread.sleep(com.morphoinc.app.panoramagp3.Camera2App.access$8500(), com.morphoinc.app.panoramagp3.Camera2App.access$8600());
     */
            /* JADX WARNING: Missing block: B:114:?, code skipped:
            createDirection(r7);
     */
            /* JADX WARNING: Missing block: B:115:0x0318, code skipped:
            if (com.morphoinc.app.panoramagp3.Camera2App.access$8400(r1.this$1.this$0).enabled() == false) goto L_0x01a8;
     */
            public void run() {
                /*
                r30 = this;
                r1 = r30;
                r2 = 0;
                r0 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ InterruptedException -> 0x0340 }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ InterruptedException -> 0x0340 }
                r0 = r0.mSettings;	 Catch:{ InterruptedException -> 0x0340 }
                r0 = r0.ui_control_mode;	 Catch:{ InterruptedException -> 0x0340 }
                r3 = -1073741823; // 0xffffffffc0000001 float:-2.0000002 double:NaN;
                r4 = 0;
                r6 = 5;
                r8 = 2;
                r9 = 0;
                r10 = 1;
                if (r0 != r8) goto L_0x01a8;
            L_0x0019:
                r0 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ InterruptedException -> 0x0340 }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ InterruptedException -> 0x0340 }
                r0 = r0.mAttachImageQueue;	 Catch:{ InterruptedException -> 0x0340 }
                r0 = r0.take();	 Catch:{ InterruptedException -> 0x0340 }
                r0 = (com.morphoinc.app.panoramagp3.CaptureImage) r0;	 Catch:{ InterruptedException -> 0x0340 }
                r11 = r0;
                r12 = com.morphoinc.app.panoramagp3.Camera2App.sAttachExit;	 Catch:{ InterruptedException -> 0x0340 }
                if (r0 == r12) goto L_0x031e;
            L_0x002e:
                r1.setImage(r11);	 Catch:{ InterruptedException -> 0x0340 }
                r12 = com.morphoinc.app.panoramagp3.CameraConstants.CameraSynchronizedObject;	 Catch:{ InterruptedException -> 0x0340 }
                monitor-enter(r12);	 Catch:{ InterruptedException -> 0x0340 }
                r13 = com.morphoinc.app.panoramagp3.CameraConstants.EngineSynchronizedObject;	 Catch:{ all -> 0x01a5 }
                monitor-enter(r13);	 Catch:{ all -> 0x01a5 }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x01a2 }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x01a2 }
                r0 = r0.isEngineRunning();	 Catch:{ all -> 0x01a2 }
                if (r0 != 0) goto L_0x004e;
            L_0x0041:
                r0 = "Camera2App";
                r3 = "attach thread exit. (engine is stop.)";
                com.morphoinc.app.LogFilter.i(r0, r3);	 Catch:{ all -> 0x01a2 }
                r30.closeSrc();	 Catch:{ all -> 0x01a2 }
                monitor-exit(r13);	 Catch:{ all -> 0x01a2 }
                monitor-exit(r12);	 Catch:{ all -> 0x01a5 }
                return;
            L_0x004e:
                r0 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x01a2 }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x01a2 }
                r0 = r0.mMorphoPanoramaGP3;	 Catch:{ all -> 0x01a2 }
                r14 = r0.getAttachCount();	 Catch:{ all -> 0x01a2 }
                r16 = r14 % r6;
                r0 = (r16 > r4 ? 1 : (r16 == r4 ? 0 : -1));
                if (r0 != 0) goto L_0x006e;
            L_0x0060:
                r0 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x01a2 }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x01a2 }
                r0.setInitialRotationByGravity();	 Catch:{ all -> 0x01a2 }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x01a2 }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x01a2 }
                r0.mIsSensorAverage = r10;	 Catch:{ all -> 0x01a2 }
            L_0x006e:
                r0 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x01a2 }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x01a2 }
                r0.setSensorFusionValue(r11);	 Catch:{ all -> 0x01a2 }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x01a2 }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x01a2 }
                r16 = r0.mMorphoPanoramaGP3;	 Catch:{ all -> 0x01a2 }
                r0 = r1.byteBuffer;	 Catch:{ all -> 0x01a2 }
                r17 = r0[r9];	 Catch:{ all -> 0x01a2 }
                r0 = r1.byteBuffer;	 Catch:{ all -> 0x01a2 }
                r18 = r0[r10];	 Catch:{ all -> 0x01a2 }
                r0 = r1.byteBuffer;	 Catch:{ all -> 0x01a2 }
                r19 = r0[r8];	 Catch:{ all -> 0x01a2 }
                r0 = r1.rowStride;	 Catch:{ all -> 0x01a2 }
                r20 = r0[r9];	 Catch:{ all -> 0x01a2 }
                r0 = r1.rowStride;	 Catch:{ all -> 0x01a2 }
                r21 = r0[r10];	 Catch:{ all -> 0x01a2 }
                r0 = r1.rowStride;	 Catch:{ all -> 0x01a2 }
                r22 = r0[r8];	 Catch:{ all -> 0x01a2 }
                r0 = r1.pixelStride;	 Catch:{ all -> 0x01a2 }
                r23 = r0[r9];	 Catch:{ all -> 0x01a2 }
                r0 = r1.pixelStride;	 Catch:{ all -> 0x01a2 }
                r24 = r0[r10];	 Catch:{ all -> 0x01a2 }
                r0 = r1.pixelStride;	 Catch:{ all -> 0x01a2 }
                r25 = r0[r8];	 Catch:{ all -> 0x01a2 }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x01a2 }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x01a2 }
                r26 = r0.mCurrentSensorInfoManager;	 Catch:{ all -> 0x01a2 }
                r27 = 0;
                r0 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x01a2 }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x01a2 }
                r0 = r0.mActivity;	 Catch:{ all -> 0x01a2 }
                r28 = r0.getBaseContext();	 Catch:{ all -> 0x01a2 }
                r0 = r16.attach(r17, r18, r19, r20, r21, r22, r23, r24, r25, r26, r27, r28);	 Catch:{ all -> 0x01a2 }
                r4 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x01a2 }
                r4 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x01a2 }
                r4 = r4.mSettings;	 Catch:{ all -> 0x01a2 }
                r4 = r4.save_input_images;	 Catch:{ all -> 0x01a2 }
                if (r4 == r8) goto L_0x00d3;
            L_0x00c7:
                r4 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x01a2 }
                r4 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x01a2 }
                r4 = r4.mSettings;	 Catch:{ all -> 0x01a2 }
                r4 = r4.save_input_images;	 Catch:{ all -> 0x01a2 }
                if (r4 != r10) goto L_0x00da;
            L_0x00d3:
                r4 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x01a2 }
                r4 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x01a2 }
                r4.encodeMovie(r11);	 Catch:{ all -> 0x01a2 }
            L_0x00da:
                r30.closeSrc();	 Catch:{ all -> 0x01a2 }
                r4 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x01a2 }
                r4 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x01a2 }
                if (r0 != r3) goto L_0x00e5;
            L_0x00e3:
                r5 = r10;
                goto L_0x00e6;
            L_0x00e5:
                r5 = r9;
            L_0x00e6:
                r4.isInvalidDir = r5;	 Catch:{ all -> 0x01a2 }
                if (r0 == 0) goto L_0x011e;
            L_0x00eb:
                r3 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x01a2 }
                r3 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x01a2 }
                r3 = r3.isInvalidDir;	 Catch:{ all -> 0x01a2 }
                if (r3 != 0) goto L_0x010b;
            L_0x00f5:
                r3 = "Camera2App";
                r4 = new java.lang.StringBuilder;	 Catch:{ all -> 0x01a2 }
                r4.<init>();	 Catch:{ all -> 0x01a2 }
                r5 = "mMorphoPanoramaGP3.attach error ret:";
                r4.append(r5);	 Catch:{ all -> 0x01a2 }
                r4.append(r0);	 Catch:{ all -> 0x01a2 }
                r4 = r4.toString();	 Catch:{ all -> 0x01a2 }
                com.morphoinc.app.LogFilter.e(r3, r4);	 Catch:{ all -> 0x01a2 }
            L_0x010b:
                r3 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x01a2 }
                r3 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x01a2 }
                r3 = r3.mActivity;	 Catch:{ all -> 0x01a2 }
                r4 = new com.morphoinc.app.panoramagp3.Camera2App$DecideDirection$DecideDirectionAttach$DecideFailRunnable;	 Catch:{ all -> 0x01a2 }
                r4.<init>(r1, r2);	 Catch:{ all -> 0x01a2 }
                r3.runOnUiThread(r4);	 Catch:{ all -> 0x01a2 }
                monitor-exit(r13);	 Catch:{ all -> 0x01a2 }
                monitor-exit(r12);	 Catch:{ all -> 0x01a5 }
                return;
            L_0x011e:
                r4 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x01a2 }
                r4 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x01a2 }
                r4 = r4.mMorphoPanoramaGP3;	 Catch:{ all -> 0x01a2 }
                r4 = r4.getDirection();	 Catch:{ all -> 0x01a2 }
                r5 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x01a2 }
                r5 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x01a2 }
                r5 = r5.mInitParam;	 Catch:{ all -> 0x01a2 }
                r5 = r5.direction;	 Catch:{ all -> 0x01a2 }
                if (r4 != r5) goto L_0x013c;
            L_0x0136:
                monitor-exit(r13);	 Catch:{ all -> 0x01a2 }
                monitor-exit(r12);	 Catch:{ all -> 0x01a5 }
                r4 = 0;
                goto L_0x0019;
            L_0x013c:
                r5 = new int[r8];	 Catch:{ all -> 0x01a2 }
                r2 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x01a2 }
                r2 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x01a2 }
                r2 = r2.mMorphoPanoramaGP3;	 Catch:{ all -> 0x01a2 }
                r2 = r2.getOutputImageSize(r5);	 Catch:{ all -> 0x01a2 }
                r0 = r2;
                if (r0 == 0) goto L_0x0162;
            L_0x014d:
                r2 = "Camera2App";
                r3 = java.util.Locale.US;	 Catch:{ all -> 0x01a2 }
                r8 = "MorphoSensorFusion.getOutputImageSize error ret:0x%08X";
                r6 = new java.lang.Object[r10];	 Catch:{ all -> 0x01a2 }
                r7 = java.lang.Integer.valueOf(r0);	 Catch:{ all -> 0x01a2 }
                r6[r9] = r7;	 Catch:{ all -> 0x01a2 }
                r3 = java.lang.String.format(r3, r8, r6);	 Catch:{ all -> 0x01a2 }
                com.morphoinc.app.LogFilter.e(r2, r3);	 Catch:{ all -> 0x01a2 }
            L_0x0162:
                r2 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x01a2 }
                r2 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x01a2 }
                r3 = r5[r9];	 Catch:{ all -> 0x01a2 }
                r2.mMaxWidth = r3;	 Catch:{ all -> 0x01a2 }
                r2 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x01a2 }
                r2 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x01a2 }
                r3 = r5[r10];	 Catch:{ all -> 0x01a2 }
                r2.mMaxHeight = r3;	 Catch:{ all -> 0x01a2 }
                monitor-exit(r13);	 Catch:{ all -> 0x01a2 }
                r1.createDirection(r4);	 Catch:{ all -> 0x01a5 }
                monitor-exit(r12);	 Catch:{ all -> 0x01a5 }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ InterruptedException -> 0x0340 }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ InterruptedException -> 0x0340 }
                r0 = r0.mDirectionFunction;	 Catch:{ InterruptedException -> 0x0340 }
                r0 = r0.enabled();	 Catch:{ InterruptedException -> 0x0340 }
                if (r0 == 0) goto L_0x0189;
            L_0x0187:
                goto L_0x031e;
            L_0x0189:
                r2 = com.morphoinc.app.panoramagp3.Camera2App.SLEEP_MILLISEC;	 Catch:{ InterruptedException -> 0x0195 }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.SLEEP_NANOSEC;	 Catch:{ InterruptedException -> 0x0195 }
                java.lang.Thread.sleep(r2, r0);	 Catch:{ InterruptedException -> 0x0195 }
                goto L_0x0196;
            L_0x0195:
                r0 = move-exception;
                r2 = 0;
                r3 = -1073741823; // 0xffffffffc0000001 float:-2.0000002 double:NaN;
                r4 = 0;
                r6 = 5;
                r8 = 2;
                goto L_0x0019;
            L_0x01a2:
                r0 = move-exception;
                monitor-exit(r13);	 Catch:{ all -> 0x01a2 }
                throw r0;	 Catch:{ all -> 0x01a5 }
            L_0x01a5:
                r0 = move-exception;
                monitor-exit(r12);	 Catch:{ all -> 0x01a5 }
                throw r0;	 Catch:{ InterruptedException -> 0x0340 }
            L_0x01a8:
                r0 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ InterruptedException -> 0x0340 }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ InterruptedException -> 0x0340 }
                r0 = r0.mAttachImageQueue;	 Catch:{ InterruptedException -> 0x0340 }
                r0 = r0.take();	 Catch:{ InterruptedException -> 0x0340 }
                r0 = (com.morphoinc.app.panoramagp3.CaptureImage) r0;	 Catch:{ InterruptedException -> 0x0340 }
                r11 = r0;
                r2 = com.morphoinc.app.panoramagp3.Camera2App.sAttachExit;	 Catch:{ InterruptedException -> 0x0340 }
                if (r0 == r2) goto L_0x031e;
            L_0x01bd:
                r1.setImage(r11);	 Catch:{ InterruptedException -> 0x0340 }
                r2 = com.morphoinc.app.panoramagp3.CameraConstants.EngineSynchronizedObject;	 Catch:{ InterruptedException -> 0x0340 }
                monitor-enter(r2);	 Catch:{ InterruptedException -> 0x0340 }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x031b }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x031b }
                r0 = r0.isEngineRunning();	 Catch:{ all -> 0x031b }
                if (r0 != 0) goto L_0x01d9;
            L_0x01cd:
                r0 = "Camera2App";
                r3 = "attach thread exit. (engine is stop.)";
                com.morphoinc.app.LogFilter.i(r0, r3);	 Catch:{ all -> 0x031b }
                r30.closeSrc();	 Catch:{ all -> 0x031b }
                monitor-exit(r2);	 Catch:{ all -> 0x031b }
                return;
            L_0x01d9:
                r0 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x031b }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x031b }
                r0 = r0.mMorphoPanoramaGP3;	 Catch:{ all -> 0x031b }
                r3 = r0.getAttachCount();	 Catch:{ all -> 0x031b }
                r5 = 5;
                r7 = r3 % r5;
                r12 = 0;
                r0 = (r7 > r12 ? 1 : (r7 == r12 ? 0 : -1));
                if (r0 != 0) goto L_0x01fd;
            L_0x01ef:
                r0 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x031b }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x031b }
                r0.setInitialRotationByGravity();	 Catch:{ all -> 0x031b }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x031b }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x031b }
                r0.mIsSensorAverage = r10;	 Catch:{ all -> 0x031b }
            L_0x01fd:
                r0 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x031b }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x031b }
                r0.setSensorFusionValue(r11);	 Catch:{ all -> 0x031b }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x031b }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x031b }
                r14 = r0.mMorphoPanoramaGP3;	 Catch:{ all -> 0x031b }
                r0 = r1.byteBuffer;	 Catch:{ all -> 0x031b }
                r15 = r0[r9];	 Catch:{ all -> 0x031b }
                r0 = r1.byteBuffer;	 Catch:{ all -> 0x031b }
                r16 = r0[r10];	 Catch:{ all -> 0x031b }
                r0 = r1.byteBuffer;	 Catch:{ all -> 0x031b }
                r7 = 2;
                r17 = r0[r7];	 Catch:{ all -> 0x031b }
                r0 = r1.rowStride;	 Catch:{ all -> 0x031b }
                r18 = r0[r9];	 Catch:{ all -> 0x031b }
                r0 = r1.rowStride;	 Catch:{ all -> 0x031b }
                r19 = r0[r10];	 Catch:{ all -> 0x031b }
                r0 = r1.rowStride;	 Catch:{ all -> 0x031b }
                r7 = 2;
                r20 = r0[r7];	 Catch:{ all -> 0x031b }
                r0 = r1.pixelStride;	 Catch:{ all -> 0x031b }
                r21 = r0[r9];	 Catch:{ all -> 0x031b }
                r0 = r1.pixelStride;	 Catch:{ all -> 0x031b }
                r22 = r0[r10];	 Catch:{ all -> 0x031b }
                r0 = r1.pixelStride;	 Catch:{ all -> 0x031b }
                r7 = 2;
                r23 = r0[r7];	 Catch:{ all -> 0x031b }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x031b }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x031b }
                r24 = r0.mCurrentSensorInfoManager;	 Catch:{ all -> 0x031b }
                r25 = 0;
                r0 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x031b }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x031b }
                r0 = r0.mActivity;	 Catch:{ all -> 0x031b }
                r26 = r0.getBaseContext();	 Catch:{ all -> 0x031b }
                r0 = r14.attach(r15, r16, r17, r18, r19, r20, r21, r22, r23, r24, r25, r26);	 Catch:{ all -> 0x031b }
                r7 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x031b }
                r7 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x031b }
                r7 = r7.mSettings;	 Catch:{ all -> 0x031b }
                r7 = r7.save_input_images;	 Catch:{ all -> 0x031b }
                r8 = 2;
                if (r7 == r8) goto L_0x0266;
            L_0x025a:
                r7 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x031b }
                r7 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x031b }
                r7 = r7.mSettings;	 Catch:{ all -> 0x031b }
                r7 = r7.save_input_images;	 Catch:{ all -> 0x031b }
                if (r7 != r10) goto L_0x026d;
            L_0x0266:
                r7 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x031b }
                r7 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x031b }
                r7.encodeMovie(r11);	 Catch:{ all -> 0x031b }
            L_0x026d:
                r30.closeSrc();	 Catch:{ all -> 0x031b }
                r7 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x031b }
                r7 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x031b }
                r8 = -1073741823; // 0xffffffffc0000001 float:-2.0000002 double:NaN;
                if (r0 != r8) goto L_0x027b;
            L_0x0279:
                r14 = r10;
                goto L_0x027c;
            L_0x027b:
                r14 = r9;
            L_0x027c:
                r7.isInvalidDir = r14;	 Catch:{ all -> 0x031b }
                if (r0 == 0) goto L_0x02b4;
            L_0x0281:
                r5 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x031b }
                r5 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x031b }
                r5 = r5.isInvalidDir;	 Catch:{ all -> 0x031b }
                if (r5 != 0) goto L_0x02a1;
            L_0x028b:
                r5 = "Camera2App";
                r6 = new java.lang.StringBuilder;	 Catch:{ all -> 0x031b }
                r6.<init>();	 Catch:{ all -> 0x031b }
                r7 = "mMorphoPanoramaGP3.attach error ret:";
                r6.append(r7);	 Catch:{ all -> 0x031b }
                r6.append(r0);	 Catch:{ all -> 0x031b }
                r6 = r6.toString();	 Catch:{ all -> 0x031b }
                com.morphoinc.app.LogFilter.e(r5, r6);	 Catch:{ all -> 0x031b }
            L_0x02a1:
                r5 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x031b }
                r5 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x031b }
                r5 = r5.mActivity;	 Catch:{ all -> 0x031b }
                r6 = new com.morphoinc.app.panoramagp3.Camera2App$DecideDirection$DecideDirectionAttach$DecideFailRunnable;	 Catch:{ all -> 0x031b }
                r7 = 0;
                r6.<init>(r1, r7);	 Catch:{ all -> 0x031b }
                r5.runOnUiThread(r6);	 Catch:{ all -> 0x031b }
                monitor-exit(r2);	 Catch:{ all -> 0x031b }
                return;
            L_0x02b4:
                r7 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x031b }
                r7 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x031b }
                r7 = r7.mMorphoPanoramaGP3;	 Catch:{ all -> 0x031b }
                r7 = r7.getDirection();	 Catch:{ all -> 0x031b }
                r14 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x031b }
                r14 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x031b }
                r14 = r14.mInitParam;	 Catch:{ all -> 0x031b }
                r14 = r14.direction;	 Catch:{ all -> 0x031b }
                if (r7 != r14) goto L_0x02cf;
            L_0x02cc:
                monitor-exit(r2);	 Catch:{ all -> 0x031b }
                goto L_0x01a8;
            L_0x02cf:
                r14 = 2;
                r15 = new int[r14];	 Catch:{ all -> 0x031b }
                r5 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x031b }
                r5 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x031b }
                r5 = r5.mMorphoPanoramaGP3;	 Catch:{ all -> 0x031b }
                r5 = r5.getOutputImageSize(r15);	 Catch:{ all -> 0x031b }
                r0 = r5;
                if (r0 == 0) goto L_0x02f6;
            L_0x02e1:
                r5 = "Camera2App";
                r6 = java.util.Locale.US;	 Catch:{ all -> 0x031b }
                r8 = "MorphoSensorFusion.getOutputImageSize error ret:0x%08X";
                r12 = new java.lang.Object[r10];	 Catch:{ all -> 0x031b }
                r13 = java.lang.Integer.valueOf(r0);	 Catch:{ all -> 0x031b }
                r12[r9] = r13;	 Catch:{ all -> 0x031b }
                r6 = java.lang.String.format(r6, r8, r12);	 Catch:{ all -> 0x031b }
                com.morphoinc.app.LogFilter.e(r5, r6);	 Catch:{ all -> 0x031b }
            L_0x02f6:
                r5 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x031b }
                r5 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x031b }
                r6 = r15[r9];	 Catch:{ all -> 0x031b }
                r5.mMaxWidth = r6;	 Catch:{ all -> 0x031b }
                r5 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ all -> 0x031b }
                r5 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x031b }
                r6 = r15[r10];	 Catch:{ all -> 0x031b }
                r5.mMaxHeight = r6;	 Catch:{ all -> 0x031b }
                monitor-exit(r2);	 Catch:{ all -> 0x031b }
                r1.createDirection(r7);	 Catch:{ InterruptedException -> 0x0340 }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;	 Catch:{ InterruptedException -> 0x0340 }
                r0 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ InterruptedException -> 0x0340 }
                r0 = r0.mDirectionFunction;	 Catch:{ InterruptedException -> 0x0340 }
                r0 = r0.enabled();	 Catch:{ InterruptedException -> 0x0340 }
                if (r0 == 0) goto L_0x01a8;
            L_0x031a:
                goto L_0x031e;
            L_0x031b:
                r0 = move-exception;
                monitor-exit(r2);	 Catch:{ all -> 0x031b }
                throw r0;	 Catch:{ InterruptedException -> 0x0340 }
                r0 = r11;
                r2 = com.morphoinc.app.panoramagp3.Camera2App.sAttachExit;
                if (r0 != r2) goto L_0x032e;
            L_0x0326:
                r2 = "Camera2App";
                r3 = "attach thread exit. (request exit)";
                com.morphoinc.app.LogFilter.d(r2, r3);
                return;
            L_0x032e:
                r2 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;
                r2 = com.morphoinc.app.panoramagp3.Camera2App.this;
                r2 = r2.mActivity;
                r3 = new com.morphoinc.app.panoramagp3.Camera2App$DecideDirection$DecideDirectionAttach$DecideRunnable;
                r4 = 0;
                r3.<init>(r1, r4);
                r2.runOnUiThread(r3);
                return;
            L_0x0340:
                r0 = move-exception;
                r0.printStackTrace();
                r2 = com.morphoinc.app.panoramagp3.Camera2App.DecideDirection.this;
                r2 = com.morphoinc.app.panoramagp3.Camera2App.this;
                r2 = r2.mActivity;
                r3 = new com.morphoinc.app.panoramagp3.Camera2App$DecideDirection$DecideDirectionAttach$DecideFailRunnable;
                r4 = 0;
                r3.<init>(r1, r4);
                r2.runOnUiThread(r3);
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.morphoinc.app.panoramagp3.Camera2App$DecideDirection$DecideDirectionAttach.run():void");
            }

            private void createDirection(int direction) {
                float warning_rotation = 0.0f;
                CameraInfo cameraInfo = Camera2App.this.mMorphoCamera.cameraInfo();
                cameraInfo.setCaptureWidth(2400);
                cameraInfo.setCaptureHeight(1800);
                int scale;
                if (Camera2App.this.mInitParam.output_rotation != 90 && Camera2App.this.mInitParam.output_rotation != MediaProviderUtils.ROTATION_270) {
                    int captureHeight;
                    int access$7500;
                    switch (direction) {
                        case 3:
                            LogFilter.i("Camera2App", "direction : VERTICAL_UP");
                            scale = getScaleH();
                            if (Camera2App.this.mCameraOrientation == 90) {
                                Camera2App camera2App = Camera2App.this;
                                int captureWidth = cameraInfo.getCaptureWidth();
                                captureHeight = cameraInfo.getCaptureHeight();
                                access$7500 = Camera2App.this.mMaxWidth;
                                DirectionFunction directionFunction = r11;
                                DirectionFunction upDirectionFunction = new UpDirectionFunction(captureWidth, captureHeight, access$7500, Camera2App.this.mMaxHeight, scale, Camera2App.this.mInitParam.output_rotation);
                                camera2App.mDirectionFunction = directionFunction;
                                Camera2App.this.mArrowDir = 1;
                            } else {
                                Camera2App.this.mDirectionFunction = new DownDirectionFunction(cameraInfo.getCaptureWidth(), cameraInfo.getCaptureHeight(), Camera2App.this.mMaxWidth, Camera2App.this.mMaxHeight, scale, Camera2App.this.mInitParam.output_rotation);
                                Camera2App.this.mArrowDir = 0;
                            }
                            if ((Camera2App.this.mInitParam.output_rotation + Camera2App.this.mCameraOrientation) % 360 != 90) {
                                Camera2App.this.mWarningTextDir = 3;
                                warning_rotation = 270.0f;
                                break;
                            }
                            Camera2App.this.mWarningTextDir = 2;
                            warning_rotation = 90.0f;
                            break;
                        case 4:
                            LogFilter.i("Camera2App", "direction : VERTICAL_DOWN");
                            scale = getScaleH();
                            if (Camera2App.this.mCameraOrientation == 90) {
                                Camera2App.this.mDirectionFunction = new DownDirectionFunction(cameraInfo.getCaptureWidth(), cameraInfo.getCaptureHeight(), Camera2App.this.mMaxWidth, Camera2App.this.mMaxHeight, scale, Camera2App.this.mInitParam.output_rotation);
                                Camera2App.this.mArrowDir = 0;
                            } else {
                                Camera2App camera2App2 = Camera2App.this;
                                captureHeight = cameraInfo.getCaptureWidth();
                                access$7500 = cameraInfo.getCaptureHeight();
                                int access$75002 = Camera2App.this.mMaxWidth;
                                DirectionFunction directionFunction2 = r12;
                                DirectionFunction upDirectionFunction2 = new UpDirectionFunction(captureHeight, access$7500, access$75002, Camera2App.this.mMaxHeight, scale, Camera2App.this.mInitParam.output_rotation);
                                camera2App2.mDirectionFunction = directionFunction2;
                                Camera2App.this.mArrowDir = 1;
                            }
                            if ((Camera2App.this.mInitParam.output_rotation + Camera2App.this.mCameraOrientation) % 360 != 90) {
                                Camera2App.this.mWarningTextDir = 3;
                                warning_rotation = 270.0f;
                                break;
                            }
                            Camera2App.this.mWarningTextDir = 2;
                            warning_rotation = 90.0f;
                            break;
                        case 5:
                            LogFilter.i("Camera2App", "direction : HORIZONTAL_LEFT");
                            scale = getScaleV();
                            if (Camera2App.this.mCameraOrientation == 90) {
                                Camera2App.this.mDirectionFunction = new LeftDirectionFunction(cameraInfo.getCaptureWidth(), cameraInfo.getCaptureHeight(), Camera2App.this.mMaxWidth, Camera2App.this.mMaxHeight, scale, Camera2App.this.mInitParam.output_rotation);
                                Camera2App.this.mArrowDir = 2;
                            } else {
                                Camera2App.this.mDirectionFunction = new RightDirectionFunction(cameraInfo.getCaptureWidth(), cameraInfo.getCaptureHeight(), Camera2App.this.mMaxWidth, Camera2App.this.mMaxHeight, scale, Camera2App.this.mInitParam.output_rotation);
                                Camera2App.this.mArrowDir = 3;
                            }
                            if ((Camera2App.this.mInitParam.output_rotation + Camera2App.this.mCameraOrientation) % 360 != 90) {
                                Camera2App.this.mWarningTextDir = 0;
                                warning_rotation = 270.0f;
                                break;
                            }
                            Camera2App.this.mWarningTextDir = 1;
                            warning_rotation = 90.0f;
                            break;
                        case 6:
                            LogFilter.i("Camera2App", "direction : HORIZONTAL_RIGHT");
                            scale = getScaleV();
                            if (Camera2App.this.mCameraOrientation == 90) {
                                Camera2App.this.mDirectionFunction = new RightDirectionFunction(cameraInfo.getCaptureWidth(), cameraInfo.getCaptureHeight(), Camera2App.this.mMaxWidth, Camera2App.this.mMaxHeight, scale, Camera2App.this.mInitParam.output_rotation);
                                Camera2App.this.mArrowDir = 3;
                            } else {
                                Camera2App.this.mDirectionFunction = new LeftDirectionFunction(cameraInfo.getCaptureWidth(), cameraInfo.getCaptureHeight(), Camera2App.this.mMaxWidth, Camera2App.this.mMaxHeight, scale, Camera2App.this.mInitParam.output_rotation);
                                Camera2App.this.mArrowDir = 2;
                            }
                            if ((Camera2App.this.mInitParam.output_rotation + Camera2App.this.mCameraOrientation) % 360 != 90) {
                                Camera2App.this.mWarningTextDir = 0;
                                warning_rotation = 270.0f;
                                break;
                            }
                            Camera2App.this.mWarningTextDir = 1;
                            warning_rotation = 90.0f;
                            break;
                    }
                }
                switch (direction) {
                    case 3:
                        LogFilter.i("Camera2App", "direction : VERTICAL_UP");
                        scale = getScaleV();
                        if (Camera2App.this.mCameraOrientation == 90) {
                            Camera2App.this.mDirectionFunction = new RightDirectionFunction(cameraInfo.getCaptureWidth(), cameraInfo.getCaptureHeight(), Camera2App.this.mMaxWidth, Camera2App.this.mMaxHeight, scale, Camera2App.this.mInitParam.output_rotation);
                        } else {
                            Camera2App.this.mDirectionFunction = new LeftDirectionFunction(cameraInfo.getCaptureWidth(), cameraInfo.getCaptureHeight(), Camera2App.this.mMaxWidth, Camera2App.this.mMaxHeight, scale, Camera2App.this.mInitParam.output_rotation);
                        }
                        if ((Camera2App.this.mInitParam.output_rotation + Camera2App.this.mCameraOrientation) % 360 != MediaProviderUtils.ROTATION_180) {
                            Camera2App.this.mWarningTextDir = 3;
                            warning_rotation = 180.0f;
                            break;
                        }
                        Camera2App.this.mWarningTextDir = 2;
                        warning_rotation = 0.0f;
                        break;
                    case 4:
                        LogFilter.i("Camera2App", "direction : VERTICAL_DOWN");
                        scale = getScaleV();
                        if (Camera2App.this.mCameraOrientation == 90) {
                            Camera2App.this.mDirectionFunction = new LeftDirectionFunction(cameraInfo.getCaptureWidth(), cameraInfo.getCaptureHeight(), Camera2App.this.mMaxWidth, Camera2App.this.mMaxHeight, scale, Camera2App.this.mInitParam.output_rotation);
                        } else {
                            Camera2App.this.mDirectionFunction = new RightDirectionFunction(cameraInfo.getCaptureWidth(), cameraInfo.getCaptureHeight(), Camera2App.this.mMaxWidth, Camera2App.this.mMaxHeight, scale, Camera2App.this.mInitParam.output_rotation);
                        }
                        if ((Camera2App.this.mInitParam.output_rotation + Camera2App.this.mCameraOrientation) % 360 != MediaProviderUtils.ROTATION_180) {
                            Camera2App.this.mWarningTextDir = 3;
                            warning_rotation = 180.0f;
                            break;
                        }
                        Camera2App.this.mWarningTextDir = 2;
                        warning_rotation = 0.0f;
                        break;
                    case 5:
                        LogFilter.i("Camera2App", "direction : HORIZONTAL_LEFT");
                        scale = getScaleH();
                        if (Camera2App.this.mCameraOrientation == 90) {
                            Camera2App.this.mDirectionFunction = new UpDirectionFunction(cameraInfo.getCaptureWidth(), cameraInfo.getCaptureHeight(), Camera2App.this.mMaxWidth, Camera2App.this.mMaxHeight, scale, Camera2App.this.mInitParam.output_rotation);
                        } else {
                            Camera2App.this.mDirectionFunction = new DownDirectionFunction(cameraInfo.getCaptureWidth(), cameraInfo.getCaptureHeight(), Camera2App.this.mMaxWidth, Camera2App.this.mMaxHeight, scale, Camera2App.this.mInitParam.output_rotation);
                        }
                        if ((Camera2App.this.mInitParam.output_rotation + Camera2App.this.mCameraOrientation) % 360 != MediaProviderUtils.ROTATION_180) {
                            Camera2App.this.mWarningTextDir = 1;
                            warning_rotation = 180.0f;
                            break;
                        }
                        Camera2App.this.mWarningTextDir = 0;
                        warning_rotation = 0.0f;
                        break;
                    case 6:
                        LogFilter.i("Camera2App", "direction : HORIZONTAL_RIGHT");
                        scale = getScaleH();
                        if (Camera2App.this.mCameraOrientation == 90) {
                            Camera2App.this.mDirectionFunction = new DownDirectionFunction(cameraInfo.getCaptureWidth(), cameraInfo.getCaptureHeight(), Camera2App.this.mMaxWidth, Camera2App.this.mMaxHeight, scale, Camera2App.this.mInitParam.output_rotation);
                        } else {
                            Camera2App.this.mDirectionFunction = new UpDirectionFunction(cameraInfo.getCaptureWidth(), cameraInfo.getCaptureHeight(), Camera2App.this.mMaxWidth, Camera2App.this.mMaxHeight, scale, Camera2App.this.mInitParam.output_rotation);
                        }
                        if ((Camera2App.this.mInitParam.output_rotation + Camera2App.this.mCameraOrientation) % 360 != MediaProviderUtils.ROTATION_180) {
                            Camera2App.this.mWarningTextDir = 1;
                            warning_rotation = 180.0f;
                            break;
                        }
                        Camera2App.this.mWarningTextDir = 0;
                        warning_rotation = 0.0f;
                        break;
                }
                Camera2App.this.mArrowDir = Camera2App.this.mDirectionFunction.getDirection();
                if (Camera2App.this.mIsFrontCamera) {
                    Camera2App.this.mArrowDir = DirectionFunction.reverseDirection(Camera2App.this.mArrowDir);
                    Camera2App.this.mWarningTextDir = DirectionFunction.reverseDirection(Camera2App.this.mWarningTextDir);
                    warning_rotation = (warning_rotation + 180.0f) % 360.0f;
                }
                float arrow_rotation = 0.0f;
                switch (Camera2App.this.mArrowDir) {
                    case 0:
                        arrow_rotation = 0.0f;
                        break;
                    case 1:
                        arrow_rotation = 180.0f;
                        break;
                    case 2:
                        arrow_rotation = 90.0f;
                        break;
                    case 3:
                        arrow_rotation = 270.0f;
                        break;
                }
                Camera2App.this.mPreviewArrow.setRotation(arrow_rotation);
                Camera2App.this.mWarningTextView.setRotation(warning_rotation);
                Camera2App.this.mWarningMessage.setRotation(warning_rotation);
                if (warning_rotation == 0.0f || warning_rotation == 180.0f) {
                    Camera2App.this.lpp.setMargins(30, MediaProviderUtils.ROTATION_180, 30, 0);
                } else {
                    Camera2App.this.lpp.setMargins(0, 30, 300, 30);
                }
            }
        }

        public DecideDirection() {
            Camera2App.this.mAngle = Camera2App.this.mInitParam.output_rotation;
        }

        public boolean onSaveImage(CaptureImage image) {
            if (Camera2App.this.isEngineRunning()) {
                Camera2App.this.addAttachQueue(image);
                if (this.mAttachRunnable == null) {
                    this.mAttachRunnable = new DecideDirectionAttach();
                    Camera2App.this.mExecutor.submit(this.mAttachRunnable);
                }
                return true;
            }
            LogFilter.e("Camera2App", "DecideDirection.onSaveImage mMorphoPanoramaGP3 is null!!");
            image.close();
            return false;
        }

        public void repeatTakePicture() {
            switch (Camera2App.this.mSettings.getCaptureMode()) {
                case 1:
                    Camera2App.this.mMorphoCamera.takePicture();
                    return;
                case 2:
                    Camera2App.this.mMorphoCamera.takePictureZSL();
                    return;
                case 3:
                    Camera2App.this.mMorphoCamera.takePictureBurst();
                    return;
                default:
                    return;
            }
        }
    }

    private class PanoramaFirst extends PanoramaState {
        private PanoramaFirst() {
        }

        /* synthetic */ PanoramaFirst(Camera2App x0, AnonymousClass1 x1) {
            this();
        }

        public boolean onSaveImage(CaptureImage image) {
            image.close();
            Camera2App.this.setNullDirectionFunction();
            if (Camera2App.this.isEngineRunning()) {
                CameraInfo cameraInfo = Camera2App.this.mMorphoCamera.cameraInfo();
                cameraInfo.setCaptureWidth(2400);
                cameraInfo.setCaptureHeight(1800);
                Point point = new Point();
                point.x = cameraInfo.getCaptureWidth() / 2;
                point.y = cameraInfo.getCaptureHeight() / 2;
                Camera2App.this.mDateTaken[0] = System.currentTimeMillis();
                String dateStr = Camera2App.createName(Camera2App.this.mDateTaken[0]);
                Camera2App camera2App = Camera2App.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(Camera2App.this.mFolderPath);
                stringBuilder.append(File.separator);
                stringBuilder.append("input");
                stringBuilder.append(File.separator);
                stringBuilder.append(dateStr);
                camera2App.mInputFolderPath = stringBuilder.toString();
                if (Camera2App.this.mInputSaveState.isEnabled() && !Camera2App.this.mUseCamera1) {
                    Integer sensorSensitivity = Integer.valueOf(Camera2App.this.getCamera2ParamsFragment().sensorSensitivity());
                    Integer shutterSpeed = Integer.valueOf(Camera2App.this.getCamera2ParamsFragment().shutterSpeed());
                    Camera2App camera2App2 = Camera2App.this;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(Camera2App.this.mInputFolderPath);
                    stringBuilder2.append("_ISO");
                    stringBuilder2.append(sensorSensitivity.toString());
                    stringBuilder2.append("_SS");
                    stringBuilder2.append(shutterSpeed.toString());
                    camera2App2.mInputFolderPath = stringBuilder2.toString();
                }
                if (Camera2App.this.mSettings.save_input_images == 0) {
                    Camera2App.this.mMorphoPanoramaGP3.disableSaveInputImages();
                } else {
                    File dir = new File(Camera2App.this.mInputFolderPath);
                    if (dir.exists() || dir.mkdirs()) {
                        switch (Camera2App.this.mSettings.save_input_images) {
                            case 1:
                            case 2:
                                Camera2App.this.initializeEncoder();
                                Camera2App.this.mNumEncodedFrames = 0;
                                break;
                            case 3:
                                Camera2App.this.mMorphoPanoramaGP3.enableSaveInputImages(Camera2App.this.mInputFolderPath);
                                break;
                        }
                    }
                    return false;
                }
                if (Camera2App.this.mInputSaveState.isEnabled()) {
                    Camera2App.this.mInputSaveState.init();
                    Camera2App.this.mInputSaveState.resetCount();
                    Camera2App.this.mPanoramaState = Camera2App.this.mInputSaveState;
                    clearListener();
                    return true;
                }
                if (Camera2App.this.mMorphoPanoramaGP3.setShrinkRatio(Camera2App.this.mSettings.shrink_ratio) != 0) {
                    LogFilter.e("Camera2App", String.format(Locale.US, "MorphoPanoramaGP3.setShrinkRatio error ret:0x%08X", new Object[]{Integer.valueOf(Camera2App.this.mMorphoPanoramaGP3.setShrinkRatio(Camera2App.this.mSettings.shrink_ratio))}));
                }
                if (Camera2App.this.mMorphoPanoramaGP3.setCalcseamPixnum(Camera2App.this.mSettings.calcseam_pixnum) != 0) {
                    LogFilter.e("Camera2App", String.format(Locale.US, "MorphoPanoramaGP3.setCalcseamPixnum error ret:0x%08X", new Object[]{Integer.valueOf(Camera2App.this.mMorphoPanoramaGP3.setCalcseamPixnum(Camera2App.this.mSettings.calcseam_pixnum))}));
                }
                if (Camera2App.this.mMorphoPanoramaGP3.setUseDeform(Camera2App.this.mSettings.use_deform) != 0) {
                    LogFilter.e("Camera2App", String.format(Locale.US, "MorphoPanoramaGP3.setUseDeform error ret:0x%08X", new Object[]{Integer.valueOf(Camera2App.this.mMorphoPanoramaGP3.setUseDeform(Camera2App.this.mSettings.use_deform))}));
                }
                if (Camera2App.this.mMorphoPanoramaGP3.setUseLuminanceCorrection(Camera2App.this.mSettings.use_luminance_correction) != 0) {
                    LogFilter.e("Camera2App", String.format(Locale.US, "MorphoPanoramaGP3.setUseLuminanceCorrection error ret:0x%08X", new Object[]{Integer.valueOf(Camera2App.this.mMorphoPanoramaGP3.setUseLuminanceCorrection(Camera2App.this.mSettings.use_luminance_correction))}));
                }
                if (Camera2App.this.mMorphoPanoramaGP3.setSeamsearchRatio(Camera2App.this.mSettings.seamsearch_ratio) != 0) {
                    LogFilter.e("Camera2App", String.format(Locale.US, "MorphoPanoramaGP3.setSeamsearchRatio error ret:0x%08X", new Object[]{Integer.valueOf(Camera2App.this.mMorphoPanoramaGP3.setSeamsearchRatio(Camera2App.this.mSettings.seamsearch_ratio))}));
                }
                if (Camera2App.this.mMorphoPanoramaGP3.setZrotationCoeff(Camera2App.this.mSettings.zrotation_coeff) != 0) {
                    LogFilter.e("Camera2App", String.format(Locale.US, "MorphoPanoramaGP3.setZrotationCoeff error ret:0x%08X", new Object[]{Integer.valueOf(Camera2App.this.mMorphoPanoramaGP3.setZrotationCoeff(Camera2App.this.mSettings.zrotation_coeff))}));
                }
                if (Camera2App.this.mMorphoPanoramaGP3.setDrawThreshold(Camera2App.this.mSettings.draw_threshold) != 0) {
                    LogFilter.e("Camera2App", String.format(Locale.US, "MorphoPanoramaGP3.setDrawThreshold error ret:0x%08X", new Object[]{Integer.valueOf(Camera2App.this.mMorphoPanoramaGP3.setDrawThreshold(Camera2App.this.mSettings.draw_threshold))}));
                }
                if (Camera2App.this.mMorphoPanoramaGP3.setAovGain(Camera2App.this.mSettings.aov_gain) != 0) {
                    LogFilter.e("Camera2App", String.format(Locale.US, "MorphoPanoramaGP3.setAovGain error ret:0x%08X", new Object[]{Integer.valueOf(Camera2App.this.mMorphoPanoramaGP3.setAovGain(Camera2App.this.mSettings.aov_gain))}));
                }
                if (Camera2App.this.mMorphoPanoramaGP3.setDistortionCorrectionParam(Camera2App.this.mSettings.distortion_k1, Camera2App.this.mSettings.distortion_k2, Camera2App.this.mSettings.distortion_k3, Camera2App.this.mSettings.distortion_k4) != 0) {
                    LogFilter.e("Camera2App", String.format(Locale.US, "MorphoPanoramaGP3.setDistortionCorrectionParam error ret:0x%08X", new Object[]{Integer.valueOf(Camera2App.this.mMorphoPanoramaGP3.setDistortionCorrectionParam(Camera2App.this.mSettings.distortion_k1, Camera2App.this.mSettings.distortion_k2, Camera2App.this.mSettings.distortion_k3, Camera2App.this.mSettings.distortion_k4))}));
                }
                if (Camera2App.this.mMorphoPanoramaGP3.setRotationRatio(Camera2App.this.mSettings.rotation_ratio) != 0) {
                    LogFilter.e("Camera2App", String.format(Locale.US, "MorphoPanoramaGP3.setRotationRatio error ret:0x%08X", new Object[]{Integer.valueOf(Camera2App.this.mMorphoPanoramaGP3.setRotationRatio(Camera2App.this.mSettings.rotation_ratio))}));
                }
                if (Camera2App.this.mMorphoPanoramaGP3.setSensorUseMode(Camera2App.this.mSettings.sensor_use_mode) != 0) {
                    LogFilter.e("Camera2App", String.format(Locale.US, "MorphoPanoramaGP3.setSensorUseMode error ret:0x%08X", new Object[]{Integer.valueOf(Camera2App.this.mMorphoPanoramaGP3.setSensorUseMode(Camera2App.this.mSettings.sensor_use_mode))}));
                }
                if (Camera2App.this.mMorphoPanoramaGP3.setProjectionMode(Camera2App.this.mSettings.projection_mode) != 0) {
                    LogFilter.e("Camera2App", String.format(Locale.US, "MorphoPanoramaGP3.setProjectionMode error ret:0x%08X", new Object[]{Integer.valueOf(Camera2App.this.mMorphoPanoramaGP3.setProjectionMode(Camera2App.this.mSettings.projection_mode))}));
                }
                if (Camera2App.this.mMorphoPanoramaGP3.setMotionDetectionMode(Camera2App.this.mSettings.motion_detection_mode) != 0) {
                    LogFilter.e("Camera2App", String.format(Locale.US, "MorphoPanoramaGP3.setMotionDetectionMode error ret:0x%08X", new Object[]{Integer.valueOf(Camera2App.this.mMorphoPanoramaGP3.setMotionDetectionMode(Camera2App.this.mSettings.motion_detection_mode))}));
                }
                int morRet = Camera2App.this.mMorphoPanoramaGP3.start(cameraInfo.getCaptureWidth(), cameraInfo.getCaptureHeight());
                if (morRet != 0) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("mMorphoPanoramaGP3.start error ret:");
                    stringBuilder3.append(morRet);
                    LogFilter.e("Camera2App", stringBuilder3.toString());
                    return false;
                }
                Camera2App.this.mPanoramaState = new DecideDirection();
                Camera2App.this.mPanoramaState.setPanoramaStateEventListener(this.listener);
                clearListener();
                return true;
            }
            LogFilter.e("Camera2App", "PanoramaFirst.onSaveImage mMorphoPanoramaGP3 is null!!");
            return false;
        }

        public void repeatTakePicture() {
            switch (Camera2App.this.mSettings.getCaptureMode()) {
                case 1:
                    Camera2App.this.mMorphoCamera.takePicture();
                    return;
                case 2:
                    Camera2App.this.mMorphoCamera.takePictureZSL();
                    return;
                case 3:
                    Camera2App.this.mMorphoCamera.takePictureBurst();
                    return;
                default:
                    return;
            }
        }
    }

    private class PanoramaInit extends PanoramaState {
        private PanoramaInit() {
        }

        /* synthetic */ PanoramaInit(Camera2App x0, AnonymousClass1 x1) {
            this();
        }

        public boolean onSaveImage(CaptureImage image) {
            Camera2App.this.mImageFormat = image.getImageFormat();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ImageFormat :");
            stringBuilder.append(Camera2App.this.mImageFormat);
            LogFilter.i("Camera2App", stringBuilder.toString());
            if (Camera2App.this.createEngine()) {
                int ret = Camera2App.this.mMorphoPanoramaGP3.setInputImageFormat(Camera2App.this.mImageFormat);
                if (ret != 0) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("mMorphoPanoramaGP3.setImageFormat error ret:");
                    stringBuilder2.append(ret);
                    LogFilter.e("Camera2App", stringBuilder2.toString());
                }
                Camera2App.this.mPanoramaState = new PanoramaFirst(Camera2App.this, null);
                Camera2App.this.mPanoramaState.setPanoramaStateEventListener(this.listener);
                clearListener();
                Camera2App.this.mPanoramaState.onSaveImage(image);
                return true;
            }
            Camera2App.this.mPanoramaState = new PanoramaPreview(Camera2App.this);
            Camera2App.this.mPanoramaState.setPanoramaStateEventListener(this.listener);
            clearListener();
            image.close();
            return true;
        }

        public void repeatTakePicture() {
            switch (Camera2App.this.mSettings.getCaptureMode()) {
                case 1:
                    Camera2App.this.mMorphoCamera.takePicture();
                    return;
                case 2:
                    Camera2App.this.mMorphoCamera.takePictureZSL();
                    return;
                case 3:
                    Camera2App.this.mMorphoCamera.takePictureBurst();
                    return;
                default:
                    return;
            }
        }
    }

    private class PanoramaPreview extends PanoramaState {
        private final int PREVIEW_SKIP_FRAME_NUM = 0;
        private final PositionDetector detector;
        private PreviewAttach mAttachRunnable = null;
        private final float mDrawPreviewFitScale;
        private final int mPreviewHeight;
        private final int mPreviewWidth;
        private int preview_skip_count = 0;
        final /* synthetic */ Camera2App this$0;
        private final UiUpdateRunnable uiUpdateRunnable = new UiUpdateRunnable(this, null);

        private class DiffManager {
            private static final int NUM = 5;
            private int add_num;
            private double ave;
            private int index;
            private final double[] pos = new double[5];

            public DiffManager() {
                clear();
            }

            public void clear() {
                for (int i = 0; i < 5; i++) {
                    this.pos[i] = Camera2ParamsFragment.TARGET_EV;
                }
                this.index = 0;
                this.add_num = 0;
            }

            public void add(double val) {
                this.pos[this.index] = val;
                this.index++;
                if (this.index >= 5) {
                    this.index = 0;
                }
                if (this.add_num < 5) {
                    this.add_num++;
                }
                calc();
            }

            private void calc() {
                double sum = Camera2ParamsFragment.TARGET_EV;
                for (int i = 0; i < this.add_num; i++) {
                    sum += this.pos[i];
                }
                this.ave = sum / ((double) this.add_num);
            }

            public double getDiff() {
                return this.ave;
            }
        }

        private class PositionDetector {
            public static final int COMPLETED = 1;
            public static final int ERROR_DEVIATION = -3;
            public static final int ERROR_IDLE = -1;
            public static final int ERROR_OVERSPEED = -4;
            public static final int ERROR_REVERSE = -2;
            private static final int IDLE_THRES_RATIO = 2;
            private static final long IDLE_TIME = 3000000000L;
            public static final int OK = 0;
            private static final int REVERSE_THRES_RATIO = 7;
            private static final int SPEED_CHECK_CONTINUOUSLY_TIMES = 5;
            private static final int SPEED_CHECK_IGNORE_TIMES = 15;
            private static final int SPEED_CHECK_MODE = 1;
            private static final int SPEED_CHECK_MODE_AVERAGE = 1;
            private static final int SPEED_CHECK_MODE_CONTINUOUSLY = 0;
            private static final double TOO_FAST_THRES_RATIO = 0.8d;
            private static final double TOO_SLOW_THRES_RATIO = 0.05d;
            private static final double TV_ANALYSIS_THRES_RATIO = 0.2d;
            private static final double TV_CHANGE_THRES_RATIO = 1.0d;
            public static final int WARNING_TOO_FAST = 2;
            public static final int WARNING_TOO_SLOW = 3;
            private int centering_margin_left;
            private int centering_margin_top;
            private long count = 0;
            private volatile double cur_x;
            private volatile double cur_y;
            private final int direction;
            private final RectF frame_rect = new RectF();
            private RectF idle_rect = null;
            private long idle_start_time;
            private double idle_thres;
            private final DiffManager mDiffManager = new DiffManager();
            private final int output_height;
            private final int output_width;
            private double peak;
            private double prev_x;
            private double prev_y;
            private boolean reset_idle_timer;
            private double reverse_thres;
            private double reverse_thres2;
            final /* synthetic */ PanoramaPreview this$1;
            private int too_fast_count;
            private double too_fast_thres;
            private int too_slow_count;
            private double too_slow_thres;
            private final double tv_analysis_thres;
            private volatile double tv_analyzed_pos;
            private final double tv_change_thres;
            private double tv_changed_pos;

            public PositionDetector(PanoramaPreview panoramaPreview, int dir, int w, int h) {
                PanoramaPreview panoramaPreview2 = panoramaPreview;
                int i = w;
                int i2 = h;
                this.this$1 = panoramaPreview2;
                this.direction = dir;
                this.output_width = i;
                this.output_height = i2;
                this.reset_idle_timer = true;
                this.too_fast_count = 0;
                this.too_slow_count = 0;
                this.prev_y = Camera2ParamsFragment.TARGET_EV;
                this.prev_x = Camera2ParamsFragment.TARGET_EV;
                this.cur_y = Camera2ParamsFragment.TARGET_EV;
                this.cur_x = Camera2ParamsFragment.TARGET_EV;
                this.centering_margin_top = 0;
                this.centering_margin_left = 0;
                double tvAnalysisThresholdTmp = Camera2ParamsFragment.TARGET_EV;
                double tvChangeThresholdTmp = Camera2ParamsFragment.TARGET_EV;
                switch (this.direction) {
                    case 0:
                        if ((panoramaPreview2.this$0.mInitParam.output_rotation + panoramaPreview2.this$0.mCameraOrientation) % 360 == 90 || (panoramaPreview2.this$0.mInitParam.output_rotation + panoramaPreview2.this$0.mCameraOrientation) % 360 == MediaProviderUtils.ROTATION_180) {
                            this.peak = (double) i;
                        } else {
                            this.peak = Camera2ParamsFragment.TARGET_EV;
                        }
                        this.reverse_thres = (double) (((float) i) * 0.07f);
                        this.reverse_thres2 = (double) (((float) i) * 0.8f);
                        this.idle_thres = (double) (((float) i) * 0.02f);
                        this.too_slow_thres = ((double) i) * 5.0E-4d;
                        this.too_fast_thres = ((double) i) * 0.008d;
                        tvAnalysisThresholdTmp = ((double) i) * 0.002d;
                        tvChangeThresholdTmp = ((double) i) * 0.01d;
                        break;
                    case 1:
                        if ((panoramaPreview2.this$0.mInitParam.output_rotation + panoramaPreview2.this$0.mCameraOrientation) % 360 == 90 || (panoramaPreview2.this$0.mInitParam.output_rotation + panoramaPreview2.this$0.mCameraOrientation) % 360 == MediaProviderUtils.ROTATION_180) {
                            this.peak = Camera2ParamsFragment.TARGET_EV;
                        } else {
                            this.peak = (double) i;
                        }
                        this.reverse_thres = (double) (((float) i) * 0.07f);
                        this.reverse_thres2 = (double) (((float) i) * 0.8f);
                        this.idle_thres = (double) (((float) i) * 0.02f);
                        this.too_slow_thres = ((double) i) * 5.0E-4d;
                        this.too_fast_thres = ((double) i) * 0.008d;
                        tvAnalysisThresholdTmp = ((double) i) * 0.002d;
                        tvChangeThresholdTmp = ((double) i) * 0.01d;
                        break;
                    case 2:
                        if ((panoramaPreview2.this$0.mInitParam.output_rotation + panoramaPreview2.this$0.mCameraOrientation) % 360 == 90 || (panoramaPreview2.this$0.mInitParam.output_rotation + panoramaPreview2.this$0.mCameraOrientation) % 360 == MediaProviderUtils.ROTATION_180) {
                            this.peak = (double) i2;
                        } else {
                            this.peak = Camera2ParamsFragment.TARGET_EV;
                        }
                        this.reverse_thres = (double) (((float) i2) * 0.07f);
                        this.reverse_thres2 = (double) (((float) i2) * 0.8f);
                        this.idle_thres = (double) (((float) i2) * 0.02f);
                        this.too_slow_thres = ((double) i2) * 5.0E-4d;
                        this.too_fast_thres = ((double) i2) * 0.008d;
                        tvAnalysisThresholdTmp = ((double) i2) * 0.002d;
                        tvChangeThresholdTmp = ((double) i2) * 0.01d;
                        break;
                    case 3:
                        if ((panoramaPreview2.this$0.mInitParam.output_rotation + panoramaPreview2.this$0.mCameraOrientation) % 360 == 90 || (panoramaPreview2.this$0.mInitParam.output_rotation + panoramaPreview2.this$0.mCameraOrientation) % 360 == MediaProviderUtils.ROTATION_180) {
                            this.peak = Camera2ParamsFragment.TARGET_EV;
                        } else {
                            this.peak = (double) i2;
                        }
                        this.reverse_thres = (double) (((float) i2) * 0.07f);
                        this.reverse_thres2 = (double) (((float) i2) * 0.8f);
                        this.idle_thres = (double) (((float) i2) * 0.02f);
                        this.too_slow_thres = ((double) i2) * 5.0E-4d;
                        this.too_fast_thres = ((double) i2) * 0.008d;
                        tvAnalysisThresholdTmp = ((double) i2) * 0.002d;
                        tvChangeThresholdTmp = ((double) i2) * 0.01d;
                        break;
                }
                this.tv_analysis_thres = tvAnalysisThresholdTmp;
                this.tv_change_thres = tvChangeThresholdTmp;
                this.tv_analyzed_pos = this.peak;
                this.tv_changed_pos = this.peak;
            }

            public int detect(double x, double y) {
                this.count++;
                if (this.cur_x == Camera2ParamsFragment.TARGET_EV && this.prev_x == Camera2ParamsFragment.TARGET_EV) {
                    this.prev_x = x;
                    this.cur_x = x;
                } else {
                    this.prev_x = this.cur_x;
                    this.cur_x = x;
                }
                if (this.cur_y == Camera2ParamsFragment.TARGET_EV && this.prev_y == Camera2ParamsFragment.TARGET_EV) {
                    this.prev_y = y;
                    this.cur_y = y;
                } else {
                    this.prev_y = this.cur_y;
                    this.cur_y = y;
                }
                if (isReverse()) {
                    return -2;
                }
                if (isComplete() || this.this$1.this$0.mRoundDetector.detect()) {
                    return 1;
                }
                if (isIdle()) {
                    return -1;
                }
                if (isShaked()) {
                    return -3;
                }
                if (2 == checkSpeed()) {
                    return -4;
                }
                int ret = checkSpeed();
                updateFrame();
                return ret;
            }

            public int getPreviewCenteringMarginLeft() {
                return this.centering_margin_left;
            }

            public int getPreviewCenteringMarginTop() {
                return this.centering_margin_top;
            }

            public RectF getFrameRect() {
                return this.frame_rect;
            }

            /* JADX WARNING: Missing block: B:24:0x0061, code skipped:
            return r1;
     */
            /* JADX WARNING: Missing block: B:29:0x0072, code skipped:
            return r1;
     */
            /* JADX WARNING: Missing block: B:43:0x0093, code skipped:
            return r1;
     */
            /* JADX WARNING: Missing block: B:48:0x00a4, code skipped:
            return r1;
     */
            public boolean isEnableTvAnalysis() {
                /*
                r7 = this;
                r0 = r7.this$1;
                r0 = r0.this$0;
                r0 = r0.mSettings;
                r0 = r0.ui_control_mode;
                r1 = 0;
                r2 = 1;
                if (r0 != r2) goto L_0x0037;
            L_0x000e:
                r0 = r7.direction;
                switch(r0) {
                    case 2: goto L_0x0024;
                    case 3: goto L_0x0024;
                    default: goto L_0x0013;
                };
            L_0x0013:
                r3 = r7.cur_x;
                r5 = r7.tv_analyzed_pos;
                r3 = r3 - r5;
                r3 = java.lang.Math.abs(r3);
                r5 = r7.tv_analysis_thres;
                r0 = (r3 > r5 ? 1 : (r3 == r5 ? 0 : -1));
                if (r0 <= 0) goto L_0x0036;
            L_0x0022:
                r1 = r2;
                goto L_0x0036;
            L_0x0024:
                r3 = r7.cur_y;
                r5 = r7.tv_analyzed_pos;
                r3 = r3 - r5;
                r3 = java.lang.Math.abs(r3);
                r5 = r7.tv_analysis_thres;
                r0 = (r3 > r5 ? 1 : (r3 == r5 ? 0 : -1));
                if (r0 <= 0) goto L_0x0035;
            L_0x0033:
                r1 = r2;
            L_0x0035:
                return r1;
            L_0x0036:
                return r1;
            L_0x0037:
                r0 = r7.this$1;
                r0 = r0.this$0;
                r0 = r0.mSettings;
                r0 = r0.ui_control_mode;
                r3 = 2;
                if (r0 != r3) goto L_0x0076;
            L_0x0044:
                r0 = com.morphoinc.app.panoramagp3.CameraConstants.CameraSynchronizedObject;
                monitor-enter(r0);
                r3 = r7.direction;	 Catch:{ all -> 0x0073 }
                switch(r3) {
                    case 2: goto L_0x004f;
                    case 3: goto L_0x004f;
                    default: goto L_0x004c;
                };	 Catch:{ all -> 0x0073 }
            L_0x004c:
                r3 = r7.cur_x;	 Catch:{ all -> 0x0073 }
                goto L_0x0062;
            L_0x004f:
                r3 = r7.cur_y;	 Catch:{ all -> 0x0073 }
                r5 = r7.tv_analyzed_pos;	 Catch:{ all -> 0x0073 }
                r3 = r3 - r5;
                r3 = java.lang.Math.abs(r3);	 Catch:{ all -> 0x0073 }
                r5 = r7.tv_analysis_thres;	 Catch:{ all -> 0x0073 }
                r3 = (r3 > r5 ? 1 : (r3 == r5 ? 0 : -1));
                if (r3 <= 0) goto L_0x0060;
            L_0x005e:
                r1 = r2;
            L_0x0060:
                monitor-exit(r0);	 Catch:{ all -> 0x0073 }
                return r1;
            L_0x0062:
                r5 = r7.tv_analyzed_pos;	 Catch:{ all -> 0x0073 }
                r3 = r3 - r5;
                r3 = java.lang.Math.abs(r3);	 Catch:{ all -> 0x0073 }
                r5 = r7.tv_analysis_thres;	 Catch:{ all -> 0x0073 }
                r3 = (r3 > r5 ? 1 : (r3 == r5 ? 0 : -1));
                if (r3 <= 0) goto L_0x0071;
            L_0x006f:
                r1 = r2;
            L_0x0071:
                monitor-exit(r0);	 Catch:{ all -> 0x0073 }
                return r1;
            L_0x0073:
                r1 = move-exception;
                monitor-exit(r0);	 Catch:{ all -> 0x0073 }
                throw r1;
            L_0x0076:
                r0 = com.morphoinc.app.panoramagp3.CameraConstants.EngineSynchronizedObject;
                monitor-enter(r0);
                r3 = r7.direction;	 Catch:{ all -> 0x00a5 }
                switch(r3) {
                    case 2: goto L_0x0081;
                    case 3: goto L_0x0081;
                    default: goto L_0x007e;
                };	 Catch:{ all -> 0x00a5 }
            L_0x007e:
                r3 = r7.cur_x;	 Catch:{ all -> 0x00a5 }
                goto L_0x0094;
            L_0x0081:
                r3 = r7.cur_y;	 Catch:{ all -> 0x00a5 }
                r5 = r7.tv_analyzed_pos;	 Catch:{ all -> 0x00a5 }
                r3 = r3 - r5;
                r3 = java.lang.Math.abs(r3);	 Catch:{ all -> 0x00a5 }
                r5 = r7.tv_analysis_thres;	 Catch:{ all -> 0x00a5 }
                r3 = (r3 > r5 ? 1 : (r3 == r5 ? 0 : -1));
                if (r3 <= 0) goto L_0x0092;
            L_0x0090:
                r1 = r2;
            L_0x0092:
                monitor-exit(r0);	 Catch:{ all -> 0x00a5 }
                return r1;
            L_0x0094:
                r5 = r7.tv_analyzed_pos;	 Catch:{ all -> 0x00a5 }
                r3 = r3 - r5;
                r3 = java.lang.Math.abs(r3);	 Catch:{ all -> 0x00a5 }
                r5 = r7.tv_analysis_thres;	 Catch:{ all -> 0x00a5 }
                r3 = (r3 > r5 ? 1 : (r3 == r5 ? 0 : -1));
                if (r3 <= 0) goto L_0x00a3;
            L_0x00a1:
                r1 = r2;
            L_0x00a3:
                monitor-exit(r0);	 Catch:{ all -> 0x00a5 }
                return r1;
            L_0x00a5:
                r1 = move-exception;
                monitor-exit(r0);	 Catch:{ all -> 0x00a5 }
                throw r1;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.morphoinc.app.panoramagp3.Camera2App$PanoramaPreview$PositionDetector.isEnableTvAnalysis():boolean");
            }

            public void notifyTvAnalyzed() {
                double cur;
                switch (this.direction) {
                    case 2:
                    case 3:
                        cur = this.cur_y;
                        break;
                    default:
                        cur = this.cur_x;
                        break;
                }
                this.tv_analyzed_pos = cur;
            }

            public boolean isEnableTvChange() {
                boolean z = false;
                switch (this.direction) {
                    case 2:
                    case 3:
                        if (Math.abs(this.cur_y - this.tv_changed_pos) > this.tv_change_thres) {
                            z = true;
                        }
                        return z;
                    default:
                        if (Math.abs(this.cur_x - this.tv_changed_pos) > this.tv_change_thres) {
                            z = true;
                        }
                        return z;
                }
            }

            public void notifyTvChanged() {
                double cur;
                switch (this.direction) {
                    case 2:
                    case 3:
                        cur = this.cur_y;
                        break;
                    default:
                        cur = this.cur_x;
                        break;
                }
                this.tv_changed_pos = cur;
            }

            private void updateFrame() {
                if (this.this$1.this$0.mPreviewBitmap != null) {
                    Rect preview_rect = new Rect();
                    this.this$1.this$0.mPreviewFrame.getGlobalVisibleRect(preview_rect);
                    if (preview_rect.width() > 0) {
                        float frame_cx;
                        float frame_cy;
                        float frame_h;
                        float frame_w;
                        int left_margin = preview_rect.left;
                        int top_margin = preview_rect.top;
                        boolean z = true;
                        int degrees;
                        boolean isRotate;
                        float ratio;
                        float frame_h2;
                        float frame_w2;
                        float frame_cy2;
                        if (this.this$1.this$0.mInitParam.output_rotation != 0 && this.this$1.this$0.mInitParam.output_rotation != MediaProviderUtils.ROTATION_180) {
                            if (this.this$1.this$0.mIsFrontCamera) {
                                degrees = (this.this$1.this$0.mInitParam.output_rotation + this.this$1.this$0.mCameraOrientation) % 360;
                                if (!(degrees == MediaProviderUtils.ROTATION_180 || degrees == 0)) {
                                    z = false;
                                }
                                isRotate = z;
                            } else {
                                isRotate = (this.this$1.this$0.mInitParam.output_rotation + this.this$1.this$0.mCameraOrientation) % 360 == MediaProviderUtils.ROTATION_180;
                            }
                            int i;
                            switch (this.direction) {
                                case 2:
                                case 3:
                                    if (isRotate) {
                                        frame_cx = (((float) this.cur_x) - ((((float) this.this$1.this$0.mInitParam.input_height) * 0.25f) / 2.0f)) - (((float) (this.output_width - this.this$1.this$0.mInitParam.input_height)) / 2.0f);
                                        frame_cy = (float) this.cur_y;
                                    } else {
                                        frame_cx = (((float) this.output_width) - ((float) this.cur_x)) - ((((float) this.this$1.this$0.mInitParam.input_height) * 0.25f) / 2.0f);
                                        frame_cy = ((float) this.output_height) - ((float) this.cur_y);
                                    }
                                    float center = (((float) this.output_width) * Camera2App.PREVIEW_LONG_SIDE_CROP_RATIO) / 2.0f;
                                    float tmp_frame_cx = (((float) this.output_width) / ((float) this.this$1.this$0.mInitParam.input_height)) * frame_cx;
                                    if ((((float) this.output_width) * 0.25f) / 2.0f >= Math.abs(center - tmp_frame_cx)) {
                                        frame_cx = (((float) this.this$1.this$0.mInitParam.input_height) * Camera2App.PREVIEW_LONG_SIDE_CROP_RATIO) / 2.0f;
                                    } else if (tmp_frame_cx < center) {
                                        frame_cx += (((float) this.this$1.this$0.mInitParam.input_height) * 0.25f) / 2.0f;
                                    } else {
                                        frame_cx -= (((float) this.this$1.this$0.mInitParam.input_height) * 0.25f) / 2.0f;
                                    }
                                    ratio = ((float) this.this$1.this$0.mPreviewFrame.getHeight()) / ((float) this.output_height);
                                    frame_h2 = (((float) this.this$1.this$0.mInitParam.input_width) / 2.0f) * ratio;
                                    frame_w2 = ((float) preview_rect.width()) / 2.0f;
                                    frame_cx *= ratio;
                                    frame_cy *= ratio;
                                    if (!Camera2App.PREVIEW_SPREAD_BOTH_SIDES) {
                                        frame_h = frame_h2;
                                        frame_w = frame_w2;
                                        break;
                                    }
                                    if (this.direction == 3) {
                                        if (isRotate) {
                                            frame_h = frame_h2;
                                            frame_w = frame_w2;
                                            this.centering_margin_top = (int) Math.round(((((double) this.output_height) - (this.peak + ((double) (((float) this.this$1.this$0.mInitParam.input_width) / 2.0f)))) * ((double) ratio)) / 2.0d);
                                        } else {
                                            frame_h = frame_h2;
                                            frame_w = frame_w2;
                                            this.centering_margin_top = (int) Math.round(((this.peak - ((double) (((float) this.this$1.this$0.mInitParam.input_width) / 2.0f))) * ((double) ratio)) / 2.0d);
                                        }
                                        if (this.centering_margin_top < 0) {
                                            i = 0;
                                            this.centering_margin_top = 0;
                                            this.centering_margin_left = i;
                                            break;
                                        }
                                    }
                                    frame_h = frame_h2;
                                    frame_w = frame_w2;
                                    if (isRotate) {
                                        this.centering_margin_top = -((int) Math.round(((this.peak - ((double) (((float) this.this$1.this$0.mInitParam.input_width) / 2.0f))) * ((double) ratio)) / 2.0d));
                                    } else {
                                        this.centering_margin_top = -((int) Math.round(((((double) this.output_height) - (this.peak + ((double) (((float) this.this$1.this$0.mInitParam.input_width) / 2.0f)))) * ((double) ratio)) / 2.0d));
                                    }
                                    if (this.centering_margin_top > 0) {
                                        i = 0;
                                        this.centering_margin_top = 0;
                                        this.centering_margin_left = i;
                                    }
                                    i = 0;
                                    this.centering_margin_left = i;
                                default:
                                    float frame_h3;
                                    float frame_w3;
                                    if (isRotate) {
                                        frame_cx = (float) this.cur_x;
                                        frame_cy2 = (((float) this.cur_y) - ((((float) this.output_height) * 0.25f) / 2.0f)) - (((float) (this.output_height - this.this$1.this$0.mInitParam.input_width)) / 2.0f);
                                    } else {
                                        frame_cx = ((float) this.output_width) - ((float) this.cur_x);
                                        frame_cy2 = (((float) this.output_height) - ((float) this.cur_y)) - ((((float) this.this$1.this$0.mInitParam.input_width) * 0.25f) / 2.0f);
                                    }
                                    frame_h2 = (((float) this.output_height) * Camera2App.PREVIEW_LONG_SIDE_CROP_RATIO) / 2.0f;
                                    frame_cy = (((float) this.output_height) / ((float) this.this$1.this$0.mInitParam.input_width)) * frame_cy2;
                                    if ((((float) this.output_height) * 0.25f) / 2.0f >= Math.abs(frame_h2 - frame_cy)) {
                                        frame_cy2 = (((float) this.this$1.this$0.mInitParam.input_width) * Camera2App.PREVIEW_LONG_SIDE_CROP_RATIO) / 2.0f;
                                    } else if (frame_cy < frame_h2) {
                                        frame_cy2 += (((float) this.this$1.this$0.mInitParam.input_width) * 0.25f) / 2.0f;
                                    } else {
                                        frame_cy2 -= (((float) this.this$1.this$0.mInitParam.input_width) * 0.25f) / 2.0f;
                                    }
                                    ratio = ((float) this.this$1.this$0.mPreviewFrame.getWidth()) / ((float) this.output_width);
                                    frame_w2 = (((float) this.this$1.this$0.mInitParam.input_height) / 2.0f) * ratio;
                                    frame_h2 = ((float) preview_rect.height()) / 2.0f;
                                    frame_cx *= ratio;
                                    frame_cy = frame_cy2 * ratio;
                                    if (Camera2App.PREVIEW_SPREAD_BOTH_SIDES) {
                                        if (this.direction == 0) {
                                            if (isRotate) {
                                                this.centering_margin_left = -((int) Math.round(((this.peak - ((double) (((float) this.this$1.this$0.mInitParam.input_height) / 2.0f))) * ((double) ratio)) / 2.0d));
                                                frame_h3 = frame_h2;
                                                frame_w3 = frame_w2;
                                            } else {
                                                frame_h3 = frame_h2;
                                                frame_w3 = frame_w2;
                                                this.centering_margin_left = -((int) Math.round(((((double) this.output_width) - (this.peak + ((double) (((float) this.this$1.this$0.mInitParam.input_height) / 2.0f)))) * ((double) ratio)) / 2.0d));
                                            }
                                            if (this.centering_margin_left > 0) {
                                                i = 0;
                                                this.centering_margin_left = 0;
                                                this.centering_margin_top = i;
                                            }
                                        } else {
                                            frame_h3 = frame_h2;
                                            frame_w3 = frame_w2;
                                            if (isRotate) {
                                                this.centering_margin_left = (int) Math.round(((((double) this.output_width) - (this.peak + ((double) (((float) this.this$1.this$0.mInitParam.input_height) / 2.0f)))) * ((double) ratio)) / 2.0d);
                                            } else {
                                                this.centering_margin_left = (int) Math.round(((this.peak - ((double) (((float) this.this$1.this$0.mInitParam.input_height) / 2.0f))) * ((double) ratio)) / 2.0d);
                                            }
                                            if (this.centering_margin_left < 0) {
                                                i = 0;
                                                this.centering_margin_left = 0;
                                                this.centering_margin_top = i;
                                            }
                                        }
                                        i = 0;
                                        this.centering_margin_top = i;
                                    } else {
                                        frame_h3 = frame_h2;
                                        frame_w3 = frame_w2;
                                    }
                                    frame_w = frame_w3;
                                    frame_h = frame_h3;
                                    break;
                            }
                        }
                        if (this.this$1.this$0.mIsFrontCamera) {
                            degrees = (this.this$1.this$0.mInitParam.output_rotation + this.this$1.this$0.mCameraOrientation) % 360;
                            boolean z2 = degrees == 90 || degrees == MediaProviderUtils.ROTATION_270;
                            isRotate = z2;
                        } else {
                            isRotate = (this.this$1.this$0.mInitParam.output_rotation + this.this$1.this$0.mCameraOrientation) % 360 == 90;
                        }
                        float frame_w4;
                        switch (this.direction) {
                            case 2:
                            case 3:
                                if (isRotate) {
                                    frame_cx = ((float) this.output_height) - ((float) this.cur_y);
                                    frame_cy2 = (((float) this.cur_x) - ((((float) this.this$1.this$0.mInitParam.input_width) * 0.25f) / 2.0f)) - (((float) (this.output_width - this.this$1.this$0.mInitParam.input_width)) / 2.0f);
                                } else {
                                    frame_cx = (float) this.cur_y;
                                    frame_cy2 = (((float) this.output_width) - ((float) this.cur_x)) - ((((float) this.this$1.this$0.mInitParam.input_width) * 0.25f) / 2.0f);
                                }
                                frame_h2 = (((float) this.output_width) * Camera2App.PREVIEW_LONG_SIDE_CROP_RATIO) / 2.0f;
                                frame_cy = (((float) this.output_width) / ((float) this.this$1.this$0.mInitParam.input_width)) * frame_cy2;
                                if ((((float) this.output_width) * 0.25f) / 2.0f >= Math.abs(frame_h2 - frame_cy)) {
                                    frame_cy2 = (((float) this.this$1.this$0.mInitParam.input_width) * Camera2App.PREVIEW_LONG_SIDE_CROP_RATIO) / 2.0f;
                                } else if (frame_cy < frame_h2) {
                                    frame_cy2 += (((float) this.this$1.this$0.mInitParam.input_width) * 0.25f) / 2.0f;
                                } else {
                                    frame_cy2 -= (((float) this.this$1.this$0.mInitParam.input_width) * 0.25f) / 2.0f;
                                }
                                ratio = ((float) this.this$1.this$0.mPreviewFrame.getWidth()) / ((float) this.output_height);
                                frame_w4 = (((float) this.this$1.this$0.mInitParam.input_height) / 2.0f) * ratio;
                                frame_h2 = ((float) preview_rect.height()) / 2.0f;
                                frame_cx *= ratio;
                                frame_cy2 *= ratio;
                                if (Camera2App.PREVIEW_SPREAD_BOTH_SIDES) {
                                    int i2;
                                    if (this.direction == 2) {
                                        if (isRotate) {
                                            this.centering_margin_left = (int) Math.round(((this.peak - ((double) (((float) this.this$1.this$0.mInitParam.input_height) / 2.0f))) * ((double) ratio)) / 2.0d);
                                        } else {
                                            this.centering_margin_left = (int) Math.round(((((double) this.output_height) - (this.peak + ((double) (((float) this.this$1.this$0.mInitParam.input_height) / 2.0f)))) * ((double) ratio)) / 2.0d);
                                        }
                                        if (this.centering_margin_left < 0) {
                                            i2 = 0;
                                            this.centering_margin_left = 0;
                                            this.centering_margin_top = i2;
                                        }
                                    } else {
                                        if (isRotate) {
                                            this.centering_margin_left = -((int) Math.round(((((double) this.output_height) - (this.peak + ((double) (((float) this.this$1.this$0.mInitParam.input_height) / 2.0f)))) * ((double) ratio)) / 2.0d));
                                        } else {
                                            this.centering_margin_left = -((int) Math.round(((this.peak - ((double) (((float) this.this$1.this$0.mInitParam.input_height) / 2.0f))) * ((double) ratio)) / 2.0d));
                                        }
                                        if (this.centering_margin_left > 0) {
                                            i2 = 0;
                                            this.centering_margin_left = 0;
                                            this.centering_margin_top = i2;
                                        }
                                    }
                                    i2 = 0;
                                    this.centering_margin_top = i2;
                                }
                                frame_cy = frame_cy2;
                                frame_w2 = frame_w4;
                                break;
                            default:
                                float frame_cx2;
                                float frame_cy3;
                                if (isRotate) {
                                    frame_cy2 = ((((float) this.output_height) - ((float) this.cur_y)) - ((((float) this.this$1.this$0.mInitParam.input_height) * 0.25f) / 2.0f)) - (((float) (this.output_height - this.this$1.this$0.mInitParam.input_height)) / 2.0f);
                                    frame_w4 = (float) this.cur_x;
                                } else {
                                    frame_cy2 = ((float) this.cur_y) - ((((float) this.this$1.this$0.mInitParam.input_height) * 0.25f) / 2.0f);
                                    frame_w4 = ((float) this.output_width) - ((float) this.cur_x);
                                }
                                frame_cy = (((float) this.output_height) * Camera2App.PREVIEW_LONG_SIDE_CROP_RATIO) / 2.0f;
                                float tmp_frame_cx2 = (((float) this.output_height) / ((float) this.this$1.this$0.mInitParam.input_height)) * frame_cy2;
                                if ((((float) this.output_height) * 0.25f) / 2.0f >= Math.abs(frame_cy - tmp_frame_cx2)) {
                                    frame_cy2 = (((float) this.this$1.this$0.mInitParam.input_height) * Camera2App.PREVIEW_LONG_SIDE_CROP_RATIO) / 2.0f;
                                } else if (tmp_frame_cx2 < frame_cy) {
                                    frame_cy2 += (((float) this.this$1.this$0.mInitParam.input_height) * 0.25f) / 2.0f;
                                } else {
                                    frame_cy2 -= (((float) this.this$1.this$0.mInitParam.input_height) * 0.25f) / 2.0f;
                                }
                                ratio = ((float) this.this$1.this$0.mPreviewFrame.getHeight()) / ((float) this.output_width);
                                frame_h2 = (((float) this.this$1.this$0.mInitParam.input_width) / 2.0f) * ratio;
                                frame_w2 = ((float) preview_rect.width()) / 2.0f;
                                frame_cy2 *= ratio;
                                frame_w4 *= ratio;
                                if (Camera2App.PREVIEW_SPREAD_BOTH_SIDES) {
                                    int i3;
                                    if (this.direction == 1) {
                                        if (isRotate) {
                                            frame_cx2 = frame_cy2;
                                            frame_cy3 = frame_w4;
                                            this.centering_margin_top = (int) Math.round(((((double) this.output_width) - (this.peak + ((double) (((float) this.this$1.this$0.mInitParam.input_width) / 2.0f)))) * ((double) ratio)) / 2.0d);
                                        } else {
                                            frame_cx2 = frame_cy2;
                                            frame_cy3 = frame_w4;
                                            this.centering_margin_top = (int) Math.round(((this.peak - ((double) (((float) this.this$1.this$0.mInitParam.input_width) / 2.0f))) * ((double) ratio)) / 2.0d);
                                        }
                                        if (this.centering_margin_top < 0) {
                                            i3 = 0;
                                            this.centering_margin_top = 0;
                                            this.centering_margin_left = i3;
                                        }
                                    } else {
                                        frame_cx2 = frame_cy2;
                                        frame_cy3 = frame_w4;
                                        if (isRotate) {
                                            this.centering_margin_top = -((int) Math.round(((this.peak - ((double) (((float) this.this$1.this$0.mInitParam.input_width) / 2.0f))) * ((double) ratio)) / 2.0d));
                                        } else {
                                            this.centering_margin_top = -((int) Math.round(((((double) this.output_width) - (this.peak + ((double) (((float) this.this$1.this$0.mInitParam.input_width) / 2.0f)))) * ((double) ratio)) / 2.0d));
                                        }
                                        if (this.centering_margin_top > 0) {
                                            i3 = 0;
                                            this.centering_margin_top = 0;
                                            this.centering_margin_left = i3;
                                        }
                                    }
                                    i3 = 0;
                                    this.centering_margin_left = i3;
                                } else {
                                    frame_cx2 = frame_cy2;
                                    frame_cy3 = frame_w4;
                                }
                                frame_cx = frame_cx2;
                                frame_cy = frame_cy3;
                                break;
                        }
                        frame_h = frame_h2;
                        frame_w = frame_w2;
                        this.frame_rect.set((((float) left_margin) + frame_cx) - frame_w, (((float) top_margin) + frame_cy) - frame_h, (((float) left_margin) + frame_cx) + frame_w, (((float) top_margin) + frame_cy) + frame_h);
                    }
                }
            }

            private int checkSpeed() {
                double cur;
                double prev;
                int ret = 0;
                switch (this.direction) {
                    case 2:
                    case 3:
                        cur = this.cur_y;
                        prev = this.prev_y;
                        break;
                    default:
                        cur = this.cur_x;
                        prev = this.prev_x;
                        break;
                }
                this.mDiffManager.add(Math.abs(cur - prev));
                if (15 < this.count) {
                    if (this.mDiffManager.getDiff() < this.too_slow_thres) {
                        ret = 3;
                    } else if (this.mDiffManager.getDiff() > 240.0d) {
                        ret = 2;
                    }
                }
                if (this.too_slow_count > 0) {
                    this.too_slow_count = 0;
                }
                if (this.too_fast_count > 0) {
                    this.too_fast_count = 0;
                }
                return ret;
            }

            private boolean isIdle() {
                long now = System.nanoTime();
                if (this.reset_idle_timer) {
                    this.reset_idle_timer = false;
                    this.idle_start_time = now;
                }
                if (this.idle_rect == null) {
                    double len = this.idle_thres / 2.0d;
                    this.idle_rect = new RectF((float) (this.cur_x - len), (float) (this.cur_y - len), (float) (this.cur_x + len), (float) (this.cur_y + len));
                }
                if (IDLE_TIME < now - this.idle_start_time) {
                    return true;
                }
                if (!this.idle_rect.contains((float) this.cur_x, (float) this.cur_y)) {
                    this.reset_idle_timer = true;
                    this.idle_rect = null;
                }
                return false;
            }

            private boolean isComplete() {
                double cur;
                int max;
                int half_size;
                switch (this.direction) {
                    case 2:
                    case 3:
                        cur = this.cur_y;
                        max = this.output_height;
                        half_size = this.this$1.mPreviewHeight / 2;
                        break;
                    default:
                        cur = this.cur_x;
                        max = this.output_width;
                        half_size = this.this$1.mPreviewWidth / 2;
                        break;
                }
                int i = this.direction;
                boolean z = false;
                if (i == 1 || i == 3) {
                    if (cur > ((double) (max - half_size))) {
                        z = true;
                    }
                    return z;
                }
                if (cur < ((double) half_size)) {
                    z = true;
                }
                return z;
            }

            private boolean isReverse() {
                double cur;
                double prev;
                int max;
                switch (this.direction) {
                    case 2:
                    case 3:
                        cur = this.cur_y;
                        prev = this.prev_y;
                        max = this.output_height;
                        break;
                    default:
                        cur = this.cur_x;
                        prev = this.prev_x;
                        max = this.output_width;
                        break;
                }
                boolean is_forward_dir = true;
                int i = this.direction;
                if (i == 1 || i == 3) {
                    if ((this.this$1.this$0.mInitParam.output_rotation + this.this$1.this$0.mCameraOrientation) % 360 == 90 || (this.this$1.this$0.mInitParam.output_rotation + this.this$1.this$0.mCameraOrientation) % 360 == MediaProviderUtils.ROTATION_180) {
                        is_forward_dir = false;
                    }
                } else if ((this.this$1.this$0.mInitParam.output_rotation + this.this$1.this$0.mCameraOrientation) % 360 == 0 || (this.this$1.this$0.mInitParam.output_rotation + this.this$1.this$0.mCameraOrientation) % 360 == MediaProviderUtils.ROTATION_270) {
                    is_forward_dir = false;
                }
                if (is_forward_dir) {
                    if (prev - cur > this.reverse_thres2) {
                        return true;
                    }
                    if (cur < this.peak) {
                        this.peak = cur;
                    }
                    if (cur > ((double) max)) {
                        return true;
                    }
                    if (cur - this.peak > this.reverse_thres) {
                        return true;
                    }
                    return false;
                } else if (cur - prev > this.reverse_thres2) {
                    return true;
                } else {
                    if (cur > this.peak) {
                        this.peak = cur;
                    }
                    if (cur < Camera2ParamsFragment.TARGET_EV) {
                        return true;
                    }
                    if (this.peak - cur > this.reverse_thres) {
                        return true;
                    }
                    return false;
                }
            }

            private boolean isShaked() {
                if (this.this$1.this$0.mShaked) {
                    return true;
                }
                return false;
            }
        }

        private class UiUpdateRunnable implements Runnable {
            private int mDetectResult;

            private UiUpdateRunnable() {
            }

            /* synthetic */ UiUpdateRunnable(PanoramaPreview x0, AnonymousClass1 x1) {
                this();
            }

            public void setDetectResult(int result) {
                this.mDetectResult = result;
            }

            public void run() {
                int tv_visibility;
                switch (this.mDetectResult) {
                    case -2:
                    case -1:
                    case 1:
                        tv_visibility = 4;
                        break;
                    case 2:
                        tv_visibility = 0;
                        break;
                    case 3:
                        tv_visibility = 0;
                        break;
                    default:
                        tv_visibility = 4;
                        break;
                }
                RectF frame_rect = PanoramaPreview.this.detector.getFrameRect();
                int tv_width = PanoramaPreview.this.this$0.mWarningTextView.getWidth();
                int tv_height = PanoramaPreview.this.this$0.mWarningTextView.getHeight();
                if (!PanoramaPreview.this.this$0.isEngineRunning() || tv_width <= 0 || tv_height <= 0) {
                    PanoramaPreview.this.this$0.mWarningTextView.setVisibility(4);
                } else {
                    PanoramaPreview.this.this$0.mWarningTextView.setVisibility(tv_visibility);
                }
                if (PanoramaPreview.this.this$0.mCurPreviewFrame.getVisibility() != 0 && frame_rect.width() > 0.0f) {
                    PanoramaPreview.this.this$0.mCurPreviewFrame.setLayoutParams(new LayoutParams(Math.round(frame_rect.width()), Math.round(frame_rect.height())));
                    PanoramaPreview.this.this$0.mCurPreviewFrame.setVisibility(4);
                    PanoramaPreview.this.this$0.mPreviewArrow.setVisibility(0);
                    PanoramaPreview.this.this$0.mPreviewLine3.setVisibility(0);
                }
                float transX = frame_rect.left;
                float transY = frame_rect.top;
                if (Camera2App.PREVIEW_SPREAD_BOTH_SIDES) {
                    transX += (float) PanoramaPreview.this.detector.getPreviewCenteringMarginLeft();
                    transY += (float) PanoramaPreview.this.detector.getPreviewCenteringMarginTop();
                    if (PanoramaPreview.this.this$0.mPreviewImageView != null && PanoramaPreview.this.this$0.mPreviewImageView.getWidth() > 0) {
                        PanoramaPreview.this.this$0.mPreviewImageView.setTranslationX((float) PanoramaPreview.this.detector.getPreviewCenteringMarginLeft());
                        PanoramaPreview.this.this$0.mPreviewImageView.setTranslationY((float) PanoramaPreview.this.detector.getPreviewCenteringMarginTop());
                        ToastUtil.showToast(PanoramaPreview.this.this$0.mActivity, PanoramaPreview.this.this$0.mActivity.getString(R.string.keep_arrow_on_the_top_of_line), 1);
                    }
                }
                PanoramaPreview.this.this$0.mCurPreviewFrame.setTranslationX(transX);
                PanoramaPreview.this.this$0.mCurPreviewFrame.setTranslationY(transY);
                if (PanoramaPreview.this.this$0.mMorphoPanoramaGP3 != null) {
                    float tmp_transY;
                    float tmp_transX = transX;
                    float tmp_transY2 = transY;
                    float scaleD = PanoramaPreview.this.this$0.mActivity.getResources().getDisplayMetrics().density;
                    LayoutParams tmplp = null;
                    switch (PanoramaPreview.this.this$0.mArrowDir) {
                        case 0:
                            tmp_transX += (float) ((-PanoramaPreview.this.this$0.mPreviewArrow.getWidth()) - 2);
                            if (((double) scaleD) < 1.65d) {
                                tmp_transY = (float) (((double) tmp_transY2) + (((double) (((frame_rect.height() - ((float) PanoramaPreview.this.this$0.mPreviewArrow.getHeight())) / 2.0f) - ((float) PanoramaPreview.this.this$0.mPreviewFrame.getHeight()))) + 74.7d));
                            } else if (((double) scaleD) > 1.68d && ((double) scaleD) < 1.75d) {
                                tmp_transY = (float) (((double) tmp_transY2) + (((double) (((frame_rect.height() - ((float) PanoramaPreview.this.this$0.mPreviewArrow.getHeight())) / 2.0f) - ((float) PanoramaPreview.this.this$0.mPreviewFrame.getHeight()))) + 64.7d));
                            } else if (((double) scaleD) > 1.8d && ((double) scaleD) < 1.95d) {
                                tmp_transY = (float) (((double) tmp_transY2) + (((double) (((frame_rect.height() - ((float) PanoramaPreview.this.this$0.mPreviewArrow.getHeight())) / 2.0f) - ((float) PanoramaPreview.this.this$0.mPreviewFrame.getHeight()))) + 52.3d));
                            } else if (((double) scaleD) > 2.02d && ((double) scaleD) < 2.1d) {
                                tmp_transY = (float) (((double) tmp_transY2) + (((double) (((frame_rect.height() - ((float) PanoramaPreview.this.this$0.mPreviewArrow.getHeight())) / 2.0f) - ((float) PanoramaPreview.this.this$0.mPreviewFrame.getHeight()))) + 37.7d));
                            } else if (((double) scaleD) > 1.98d && ((double) scaleD) < 2.02d) {
                                tmp_transY = (float) (((double) tmp_transY2) + (((double) (((frame_rect.height() - ((float) PanoramaPreview.this.this$0.mPreviewArrow.getHeight())) / 2.0f) - ((float) PanoramaPreview.this.this$0.mPreviewFrame.getHeight()))) + 40.7d));
                            } else if (((double) scaleD) <= 2.2d || ((double) scaleD) >= 2.3d) {
                                tmp_transY = (float) (((double) tmp_transY2) + (((double) (((frame_rect.height() - ((float) PanoramaPreview.this.this$0.mPreviewArrow.getHeight())) / 2.0f) - ((float) PanoramaPreview.this.this$0.mPreviewFrame.getHeight()))) + 22.7d));
                            } else {
                                tmp_transY = (float) (((double) tmp_transY2) + (((double) (((frame_rect.height() - ((float) PanoramaPreview.this.this$0.mPreviewArrow.getHeight())) / 2.0f) - ((float) PanoramaPreview.this.this$0.mPreviewFrame.getHeight()))) + 21.7d));
                            }
                            tmp_transY2 = tmp_transY;
                            tmplp = new LayoutParams(-1, 2);
                            tmplp.setMargins(0, PanoramaPreview.this.this$0.mPreviewLine1.getTop() + (PanoramaPreview.this.this$0.mPreviewFrame.getHeight() / 2), (int) ((((float) PanoramaPreview.this.this$0.mRootView.getWidth()) - frame_rect.left) + 10.0f), 0);
                            if (tmp_transY2 >= 449.0f || tmp_transY2 <= 0.0f) {
                                if (tmp_transY2 <= 459.0f) {
                                    PanoramaPreview.this.this$0.mShaked = false;
                                    PanoramaPreview.this.this$0.mWarningMessage.setVisibility(0);
                                    PanoramaPreview.this.this$0.mWarningMessage.setText(PanoramaPreview.this.this$0.mActivity.getString(R.string.keep_arrow_on_the_top_of_line));
                                    break;
                                }
                                PanoramaPreview.this.this$0.mWarningMessage.setVisibility(0);
                                PanoramaPreview.this.this$0.mWarningMessage.setText(PanoramaPreview.this.this$0.mActivity.getString(R.string.warn_up));
                                if (tmp_transY2 > 469.0f) {
                                    PanoramaPreview.this.this$0.mShaked = true;
                                    break;
                                }
                            }
                            PanoramaPreview.this.this$0.mWarningMessage.setVisibility(0);
                            PanoramaPreview.this.this$0.mWarningMessage.setText(PanoramaPreview.this.this$0.mActivity.getString(R.string.warn_down));
                            if (tmp_transY2 < 439.0f && tmp_transY2 > 100.0f) {
                                PanoramaPreview.this.this$0.mShaked = true;
                                break;
                            }
                            break;
                        case 1:
                            float tmp_transY3 = tmp_transY2;
                            tmp_transX += frame_rect.width() - 2.0f;
                            if (((double) scaleD) < 1.65d) {
                                tmp_transY = (float) (((double) tmp_transY3) + (((double) (((frame_rect.height() - ((float) PanoramaPreview.this.this$0.mPreviewArrow.getHeight())) / 2.0f) - ((float) PanoramaPreview.this.this$0.mPreviewFrame.getHeight()))) + 74.7d));
                            } else {
                                tmp_transY2 = tmp_transY3;
                                if (((double) scaleD) > 1.68d && ((double) scaleD) < 1.75d) {
                                    tmp_transY = (float) (((double) tmp_transY2) + (((double) (((frame_rect.height() - ((float) PanoramaPreview.this.this$0.mPreviewArrow.getHeight())) / 2.0f) - ((float) PanoramaPreview.this.this$0.mPreviewFrame.getHeight()))) + 64.7d));
                                } else if (((double) scaleD) > 1.8d && ((double) scaleD) < 1.95d) {
                                    tmp_transY = (float) (((double) tmp_transY2) + (((double) (((frame_rect.height() - ((float) PanoramaPreview.this.this$0.mPreviewArrow.getHeight())) / 2.0f) - ((float) PanoramaPreview.this.this$0.mPreviewFrame.getHeight()))) + 52.3d));
                                } else if (((double) scaleD) > 2.02d && ((double) scaleD) < 2.1d) {
                                    tmp_transY = (float) (((double) tmp_transY2) + (((double) (((frame_rect.height() - ((float) PanoramaPreview.this.this$0.mPreviewArrow.getHeight())) / 2.0f) - ((float) PanoramaPreview.this.this$0.mPreviewFrame.getHeight()))) + 36.7d));
                                } else if (((double) scaleD) > 1.98d && ((double) scaleD) < 2.02d) {
                                    tmp_transY = (float) (((double) tmp_transY2) + (((double) (((frame_rect.height() - ((float) PanoramaPreview.this.this$0.mPreviewArrow.getHeight())) / 2.0f) - ((float) PanoramaPreview.this.this$0.mPreviewFrame.getHeight()))) + 40.7d));
                                } else if (((double) scaleD) <= 2.2d || ((double) scaleD) >= 2.3d) {
                                    tmp_transY = (float) (((double) tmp_transY2) + (((double) (((frame_rect.height() - ((float) PanoramaPreview.this.this$0.mPreviewArrow.getHeight())) / 2.0f) - ((float) PanoramaPreview.this.this$0.mPreviewFrame.getHeight()))) + 22.7d));
                                } else {
                                    tmp_transY = (float) (((double) tmp_transY2) + (((double) (((frame_rect.height() - ((float) PanoramaPreview.this.this$0.mPreviewArrow.getHeight())) / 2.0f) - ((float) PanoramaPreview.this.this$0.mPreviewFrame.getHeight()))) + 21.7d));
                                }
                            }
                            tmp_transY2 = tmp_transY;
                            tmplp = new LayoutParams(-1, 2);
                            tmplp.setMargins(((int) frame_rect.right) + 6, PanoramaPreview.this.this$0.mPreviewLine1.getTop() + (PanoramaPreview.this.this$0.mPreviewFrame.getHeight() / 2), 0, 0);
                            if (tmp_transY2 >= 449.0f || tmp_transY2 <= 0.0f) {
                                if (tmp_transY2 <= 459.0f) {
                                    PanoramaPreview.this.this$0.mWarningMessage.setVisibility(0);
                                    PanoramaPreview.this.this$0.mShaked = false;
                                    PanoramaPreview.this.this$0.mWarningMessage.setText(PanoramaPreview.this.this$0.mActivity.getString(R.string.keep_arrow_on_the_top_of_line));
                                    break;
                                }
                                PanoramaPreview.this.this$0.mWarningMessage.setVisibility(0);
                                PanoramaPreview.this.this$0.mWarningMessage.setText(PanoramaPreview.this.this$0.mActivity.getString(R.string.warn_up));
                                if (tmp_transY2 > 469.0f) {
                                    PanoramaPreview.this.this$0.mShaked = true;
                                    break;
                                }
                            }
                            PanoramaPreview.this.this$0.mWarningMessage.setVisibility(0);
                            PanoramaPreview.this.this$0.mWarningMessage.setText(PanoramaPreview.this.this$0.mActivity.getString(R.string.warn_down));
                            if (tmp_transY2 < 439.0f && tmp_transY2 > 100.0f) {
                                PanoramaPreview.this.this$0.mShaked = true;
                                break;
                            }
                            break;
                        case 2:
                            tmp_transX = (float) (((double) tmp_transX) + (((double) ((frame_rect.width() - ((float) PanoramaPreview.this.this$0.mPreviewArrow.getWidth())) / 2.0f)) + 18.48d));
                            tmp_transY2 += (float) ((-PanoramaPreview.this.this$0.mPreviewArrow.getHeight()) - 97);
                            tmplp = new LayoutParams(2, -1);
                            tmplp.setMargins(PanoramaPreview.this.this$0.mPreviewLine1.getLeft() + (PanoramaPreview.this.this$0.mPreviewFrame.getWidth() / 2), 0, 0, (int) (((((float) PanoramaPreview.this.this$0.mRootView.getHeight()) - frame_rect.top) - 170.0f) - ((float) PanoramaPreview.this.this$0.mCurPreviewFrame.getHeight())));
                            if (((double) tmp_transX) >= 328.75d || tmp_transX <= 0.0f) {
                                if (((double) tmp_transX) <= 338.75d) {
                                    PanoramaPreview.this.this$0.mShaked = false;
                                    PanoramaPreview.this.this$0.mWarningMessage.setVisibility(0);
                                    PanoramaPreview.this.this$0.mWarningMessage.setText(PanoramaPreview.this.this$0.mActivity.getString(R.string.keep_arrow_on_the_top_of_line));
                                    break;
                                }
                                PanoramaPreview.this.this$0.mWarningMessage.setVisibility(0);
                                PanoramaPreview.this.this$0.mWarningMessage.setText(PanoramaPreview.this.this$0.mActivity.getString(R.string.warn_down));
                                if (((double) tmp_transX) > 348.75d) {
                                    PanoramaPreview.this.this$0.mShaked = true;
                                    break;
                                }
                            }
                            PanoramaPreview.this.this$0.mWarningMessage.setVisibility(0);
                            PanoramaPreview.this.this$0.mWarningMessage.setText(PanoramaPreview.this.this$0.mActivity.getString(R.string.warn_up));
                            if (((double) tmp_transX) < 318.75d && tmp_transX > 100.0f) {
                                PanoramaPreview.this.this$0.mShaked = true;
                                break;
                            }
                            break;
                        case 3:
                            tmp_transX = (float) (((double) tmp_transX) + (((double) ((frame_rect.width() - ((float) PanoramaPreview.this.this$0.mPreviewArrow.getWidth())) / 2.0f)) + 18.48d));
                            tmp_transY2 += frame_rect.height() - 93.0f;
                            tmplp = new LayoutParams(2, -1);
                            tmplp.setMargins(PanoramaPreview.this.this$0.mPreviewLine1.getLeft() + (PanoramaPreview.this.this$0.mPreviewFrame.getWidth() / 2), (int) (frame_rect.bottom - 70.0f), 0, 0);
                            if (((double) tmp_transX) >= 328.75d || tmp_transX <= 0.0f) {
                                if (((double) tmp_transX) <= 338.75d) {
                                    PanoramaPreview.this.this$0.mShaked = false;
                                    PanoramaPreview.this.this$0.mWarningMessage.setVisibility(0);
                                    PanoramaPreview.this.this$0.mWarningMessage.setText(PanoramaPreview.this.this$0.mActivity.getString(R.string.keep_arrow_on_the_top_of_line));
                                    break;
                                }
                                PanoramaPreview.this.this$0.mWarningMessage.setVisibility(0);
                                PanoramaPreview.this.this$0.mWarningMessage.setText(PanoramaPreview.this.this$0.mActivity.getString(R.string.warn_down));
                                if (((double) tmp_transX) > 348.75d) {
                                    PanoramaPreview.this.this$0.mShaked = true;
                                    break;
                                }
                            }
                            PanoramaPreview.this.this$0.mWarningMessage.setVisibility(0);
                            PanoramaPreview.this.this$0.mWarningMessage.setText(PanoramaPreview.this.this$0.mActivity.getString(R.string.warn_up));
                            if (((double) tmp_transX) < 318.75d && tmp_transX > 100.0f) {
                                PanoramaPreview.this.this$0.mShaked = true;
                                break;
                            }
                            break;
                    }
                    PanoramaPreview.this.this$0.mPreviewArrow.setTranslationX(tmp_transX);
                    PanoramaPreview.this.this$0.mPreviewArrow.setTranslationY(tmp_transY2);
                    PanoramaPreview.this.this$0.mPreviewLine3.setLayoutParams(tmplp);
                    if (PanoramaPreview.this.this$0.mWarningTextView.getVisibility() == 0) {
                        tmp_transY = transX;
                        float tmp_transY4 = transY;
                        switch (PanoramaPreview.this.this$0.mWarningTextDir) {
                            case 0:
                                tmp_transY += (float) (-tv_width);
                                tmp_transY4 += (frame_rect.height() - ((float) tv_height)) / 2.0f;
                                if (PanoramaPreview.this.this$0.mInitParam.output_rotation == 0 || PanoramaPreview.this.this$0.mInitParam.output_rotation == MediaProviderUtils.ROTATION_180) {
                                    tmp_transY += (float) tv_height;
                                    break;
                                }
                            case 1:
                                tmp_transY += frame_rect.width();
                                tmp_transY4 += (frame_rect.height() - ((float) tv_height)) / 2.0f;
                                if (PanoramaPreview.this.this$0.mInitParam.output_rotation == 0 || PanoramaPreview.this.this$0.mInitParam.output_rotation == MediaProviderUtils.ROTATION_180) {
                                    tmp_transY -= (((float) tv_width) / 2.0f) - ((float) tv_height);
                                    break;
                                }
                            case 2:
                                tmp_transY += (frame_rect.width() - ((float) tv_width)) / 2.0f;
                                tmp_transY4 += (float) (-tv_height);
                                if (PanoramaPreview.this.this$0.mInitParam.output_rotation == 0 || PanoramaPreview.this.this$0.mInitParam.output_rotation == MediaProviderUtils.ROTATION_180) {
                                    tmp_transY4 += ((float) (-tv_width)) / 2.0f;
                                    break;
                                }
                            case 3:
                                tmp_transY += (frame_rect.width() - ((float) tv_width)) / 2.0f;
                                tmp_transY4 += frame_rect.height();
                                if (PanoramaPreview.this.this$0.mInitParam.output_rotation == 0 || PanoramaPreview.this.this$0.mInitParam.output_rotation == MediaProviderUtils.ROTATION_180) {
                                    tmp_transY4 += ((float) tv_width) / 2.0f;
                                    break;
                                }
                        }
                        tmp_transY2 = tmp_transY4;
                        PanoramaPreview.this.this$0.mWarningTextView.setTranslationX(tmp_transY);
                        PanoramaPreview.this.this$0.mWarningTextView.setTranslationY(tmp_transY2);
                    }
                }
            }
        }

        private class PreviewAttach extends AttachRunnable {
            private boolean mIsAttachEnd = false;
            private int mResultCode;
            private final PerformanceCounter pc = PerformanceCounter.newInstance(false);
            private final PostAttachRunnable postAttachRunnable = new PostAttachRunnable(this, null);

            private class PostAttachRunnable implements Runnable {
                private PostAttachRunnable() {
                }

                /* synthetic */ PostAttachRunnable(PreviewAttach x0, AnonymousClass1 x1) {
                    this();
                }

                /* JADX WARNING: Missing block: B:23:0x006c, code skipped:
            return;
     */
                /* JADX WARNING: Missing block: B:45:0x00cb, code skipped:
            return;
     */
                public void run() {
                    /*
                    r4 = this;
                    r0 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.PreviewAttach.this;
                    r0 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;
                    r0 = r0.this$0;
                    r0 = r0.mSettings;
                    r0 = r0.ui_control_mode;
                    r1 = 2;
                    if (r0 != r1) goto L_0x0073;
                L_0x000f:
                    r0 = com.morphoinc.app.panoramagp3.CameraConstants.CameraSynchronizedObject;
                    monitor-enter(r0);
                    r1 = com.morphoinc.app.panoramagp3.Camera2App.PreviewImageSynchronizedObject;	 Catch:{ all -> 0x0070 }
                    monitor-enter(r1);	 Catch:{ all -> 0x0070 }
                    r2 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.PreviewAttach.this;	 Catch:{ all -> 0x006d }
                    r2 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ all -> 0x006d }
                    r2 = r2.this$0;	 Catch:{ all -> 0x006d }
                    r2 = r2.mPreviewBitmap;	 Catch:{ all -> 0x006d }
                    if (r2 != 0) goto L_0x002f;
                L_0x0023:
                    r2 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.PreviewAttach.this;	 Catch:{ all -> 0x006d }
                    r2 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ all -> 0x006d }
                    r2 = r2.this$0;	 Catch:{ all -> 0x006d }
                    r2.setAttachExit();	 Catch:{ all -> 0x006d }
                    monitor-exit(r1);	 Catch:{ all -> 0x006d }
                    monitor-exit(r0);	 Catch:{ all -> 0x0070 }
                    return;
                L_0x002f:
                    r2 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.PreviewAttach.this;	 Catch:{ all -> 0x006d }
                    r2 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ all -> 0x006d }
                    r2 = r2.this$0;	 Catch:{ all -> 0x006d }
                    r2 = r2.mPreviewFitBitmap;	 Catch:{ all -> 0x006d }
                    if (r2 == 0) goto L_0x0053;
                L_0x003b:
                    r2 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.PreviewAttach.this;	 Catch:{ all -> 0x006d }
                    r2 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ all -> 0x006d }
                    r2 = r2.this$0;	 Catch:{ all -> 0x006d }
                    r2 = r2.mPreviewImageView;	 Catch:{ all -> 0x006d }
                    r3 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.PreviewAttach.this;	 Catch:{ all -> 0x006d }
                    r3 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ all -> 0x006d }
                    r3 = r3.this$0;	 Catch:{ all -> 0x006d }
                    r3 = r3.mPreviewFitBitmap;	 Catch:{ all -> 0x006d }
                    r2.setImageBitmap(r3);	 Catch:{ all -> 0x006d }
                    goto L_0x006a;
                L_0x0053:
                    r2 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.PreviewAttach.this;	 Catch:{ all -> 0x006d }
                    r2 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ all -> 0x006d }
                    r2 = r2.this$0;	 Catch:{ all -> 0x006d }
                    r2 = r2.mPreviewImageView;	 Catch:{ all -> 0x006d }
                    r3 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.PreviewAttach.this;	 Catch:{ all -> 0x006d }
                    r3 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ all -> 0x006d }
                    r3 = r3.this$0;	 Catch:{ all -> 0x006d }
                    r3 = r3.mPreviewBitmap;	 Catch:{ all -> 0x006d }
                    r2.setImageBitmap(r3);	 Catch:{ all -> 0x006d }
                L_0x006a:
                    monitor-exit(r1);	 Catch:{ all -> 0x006d }
                    monitor-exit(r0);	 Catch:{ all -> 0x0070 }
                    return;
                L_0x006d:
                    r2 = move-exception;
                    monitor-exit(r1);	 Catch:{ all -> 0x006d }
                    throw r2;	 Catch:{ all -> 0x0070 }
                L_0x0070:
                    r1 = move-exception;
                    monitor-exit(r0);	 Catch:{ all -> 0x0070 }
                    throw r1;
                L_0x0073:
                    r0 = com.morphoinc.app.panoramagp3.Camera2App.PreviewImageSynchronizedObject;
                    monitor-enter(r0);
                    r1 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.PreviewAttach.this;	 Catch:{ all -> 0x00cc }
                    r1 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ all -> 0x00cc }
                    r1 = r1.this$0;	 Catch:{ all -> 0x00cc }
                    r1 = r1.mPreviewBitmap;	 Catch:{ all -> 0x00cc }
                    if (r1 != 0) goto L_0x008f;
                L_0x0084:
                    r1 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.PreviewAttach.this;	 Catch:{ all -> 0x00cc }
                    r1 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ all -> 0x00cc }
                    r1 = r1.this$0;	 Catch:{ all -> 0x00cc }
                    r1.setAttachExit();	 Catch:{ all -> 0x00cc }
                    monitor-exit(r0);	 Catch:{ all -> 0x00cc }
                    return;
                L_0x008f:
                    r1 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.PreviewAttach.this;	 Catch:{ all -> 0x00cc }
                    r1 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ all -> 0x00cc }
                    r1 = r1.this$0;	 Catch:{ all -> 0x00cc }
                    r1 = r1.mPreviewFitBitmap;	 Catch:{ all -> 0x00cc }
                    if (r1 == 0) goto L_0x00b3;
                L_0x009b:
                    r1 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.PreviewAttach.this;	 Catch:{ all -> 0x00cc }
                    r1 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ all -> 0x00cc }
                    r1 = r1.this$0;	 Catch:{ all -> 0x00cc }
                    r1 = r1.mPreviewImageView;	 Catch:{ all -> 0x00cc }
                    r2 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.PreviewAttach.this;	 Catch:{ all -> 0x00cc }
                    r2 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ all -> 0x00cc }
                    r2 = r2.this$0;	 Catch:{ all -> 0x00cc }
                    r2 = r2.mPreviewFitBitmap;	 Catch:{ all -> 0x00cc }
                    r1.setImageBitmap(r2);	 Catch:{ all -> 0x00cc }
                    goto L_0x00ca;
                L_0x00b3:
                    r1 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.PreviewAttach.this;	 Catch:{ all -> 0x00cc }
                    r1 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ all -> 0x00cc }
                    r1 = r1.this$0;	 Catch:{ all -> 0x00cc }
                    r1 = r1.mPreviewImageView;	 Catch:{ all -> 0x00cc }
                    r2 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.PreviewAttach.this;	 Catch:{ all -> 0x00cc }
                    r2 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ all -> 0x00cc }
                    r2 = r2.this$0;	 Catch:{ all -> 0x00cc }
                    r2 = r2.mPreviewBitmap;	 Catch:{ all -> 0x00cc }
                    r1.setImageBitmap(r2);	 Catch:{ all -> 0x00cc }
                L_0x00ca:
                    monitor-exit(r0);	 Catch:{ all -> 0x00cc }
                    return;
                L_0x00cc:
                    r1 = move-exception;
                    monitor-exit(r0);	 Catch:{ all -> 0x00cc }
                    throw r1;
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.morphoinc.app.panoramagp3.Camera2App$PanoramaPreview$PreviewAttach$PostAttachRunnable.run():void");
                }
            }

            /* JADX WARNING: Missing block: B:24:0x0057, code skipped:
            if (com.morphoinc.app.panoramagp3.Camera2App.access$5200(r10.this$1.this$0).save_input_images == 2) goto L_0x0065;
     */
            /* JADX WARNING: Missing block: B:26:0x0063, code skipped:
            if (com.morphoinc.app.panoramagp3.Camera2App.access$5200(r10.this$1.this$0).save_input_images != 1) goto L_0x006c;
     */
            /* JADX WARNING: Missing block: B:27:0x0065, code skipped:
            com.morphoinc.app.panoramagp3.Camera2App.access$8100(r10.this$1.this$0, r0);
     */
            /* JADX WARNING: Missing block: B:28:0x006c, code skipped:
            closeSrc();
     */
            /* JADX WARNING: Missing block: B:29:0x006f, code skipped:
            if (r8 != false) goto L_0x007d;
     */
            /* JADX WARNING: Missing block: B:30:0x0071, code skipped:
            com.morphoinc.app.LogFilter.e("Camera2App", "PreviewAttach.run() attach error.");
            r10.mResultCode = -1;
     */
            /* JADX WARNING: Missing block: B:34:0x0087, code skipped:
            if (com.morphoinc.app.panoramagp3.Camera2App.access$5200(r10.this$1.this$0).save_input_images != 0) goto L_0x00a3;
     */
            /* JADX WARNING: Missing block: B:36:0x008f, code skipped:
            if (com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.access$10100(r10.this$1) > 0) goto L_0x009e;
     */
            /* JADX WARNING: Missing block: B:37:0x0091, code skipped:
            com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.access$10200(r10.this$1, r10.pc);
            com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.access$10102(r10.this$1, 0);
     */
            /* JADX WARNING: Missing block: B:38:0x009e, code skipped:
            com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.access$10110(r10.this$1);
     */
            /* JADX WARNING: Missing block: B:39:0x00a3, code skipped:
            checkAttachEnd(r2);
     */
            /* JADX WARNING: Missing block: B:41:0x00a7, code skipped:
            r5 = r8;
     */
            /* JADX WARNING: Missing block: B:44:0x00aa, code skipped:
            if (r10.mIsAttachEnd == false) goto L_0x00ae;
     */
            /* JADX WARNING: Missing block: B:46:0x00ae, code skipped:
            com.morphoinc.app.panoramagp3.Camera2App.access$200(r10.this$1.this$0).runOnUiThread(r10.postAttachRunnable);
     */
            /* JADX WARNING: Missing block: B:48:?, code skipped:
            java.lang.Thread.sleep(com.morphoinc.app.panoramagp3.Camera2App.access$8500(), com.morphoinc.app.panoramagp3.Camera2App.access$8600());
     */
            /* JADX WARNING: Missing block: B:74:0x0110, code skipped:
            if (com.morphoinc.app.panoramagp3.Camera2App.access$5200(r10.this$1.this$0).save_input_images == 2) goto L_0x011e;
     */
            /* JADX WARNING: Missing block: B:76:0x011c, code skipped:
            if (com.morphoinc.app.panoramagp3.Camera2App.access$5200(r10.this$1.this$0).save_input_images != 1) goto L_0x0125;
     */
            /* JADX WARNING: Missing block: B:77:0x011e, code skipped:
            com.morphoinc.app.panoramagp3.Camera2App.access$8100(r10.this$1.this$0, r0);
     */
            /* JADX WARNING: Missing block: B:78:0x0125, code skipped:
            closeSrc();
     */
            /* JADX WARNING: Missing block: B:79:0x0128, code skipped:
            if (r7 != false) goto L_0x0134;
     */
            /* JADX WARNING: Missing block: B:80:0x012a, code skipped:
            com.morphoinc.app.LogFilter.e("Camera2App", "PreviewAttach.run() attach error.");
            r10.mResultCode = -1;
     */
            /* JADX WARNING: Missing block: B:82:0x013e, code skipped:
            if (com.morphoinc.app.panoramagp3.Camera2App.access$5200(r10.this$1.this$0).save_input_images != 0) goto L_0x015a;
     */
            /* JADX WARNING: Missing block: B:84:0x0146, code skipped:
            if (com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.access$10100(r10.this$1) > 0) goto L_0x0155;
     */
            /* JADX WARNING: Missing block: B:85:0x0148, code skipped:
            com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.access$10200(r10.this$1, r10.pc);
            com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.access$10102(r10.this$1, 0);
     */
            /* JADX WARNING: Missing block: B:86:0x0155, code skipped:
            com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.access$10110(r10.this$1);
     */
            /* JADX WARNING: Missing block: B:87:0x015a, code skipped:
            checkAttachEnd(r2);
     */
            /* JADX WARNING: Missing block: B:88:0x015f, code skipped:
            if (r10.mIsAttachEnd == false) goto L_0x0162;
     */
            /* JADX WARNING: Missing block: B:90:0x0162, code skipped:
            com.morphoinc.app.panoramagp3.Camera2App.access$200(r10.this$1.this$0).runOnUiThread(r10.postAttachRunnable);
     */
            public void run() {
                /*
                r10 = this;
                r0 = 0;
                r1 = 2;
                r2 = new double[r1];
                r3 = -1;
                r4 = 1;
                r5 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ InterruptedException -> 0x0175 }
                r5 = r5.this$0;	 Catch:{ InterruptedException -> 0x0175 }
                r5 = r5.mSettings;	 Catch:{ InterruptedException -> 0x0175 }
                r5 = r5.ui_control_mode;	 Catch:{ InterruptedException -> 0x0175 }
                r6 = 0;
                if (r5 != r1) goto L_0x00d0;
            L_0x0013:
                r5 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ InterruptedException -> 0x0175 }
                r5 = r5.this$0;	 Catch:{ InterruptedException -> 0x0175 }
                r5 = r5.mAttachImageQueue;	 Catch:{ InterruptedException -> 0x0175 }
                r5 = r5.take();	 Catch:{ InterruptedException -> 0x0175 }
                r5 = (com.morphoinc.app.panoramagp3.CaptureImage) r5;	 Catch:{ InterruptedException -> 0x0175 }
                r0 = r5;
                r7 = com.morphoinc.app.panoramagp3.Camera2App.sAttachExit;	 Catch:{ InterruptedException -> 0x0175 }
                if (r5 == r7) goto L_0x0174;
            L_0x0028:
                r10.setImage(r0);	 Catch:{ InterruptedException -> 0x0175 }
                r5 = com.morphoinc.app.panoramagp3.CameraConstants.CameraSynchronizedObject;	 Catch:{ InterruptedException -> 0x0175 }
                monitor-enter(r5);	 Catch:{ InterruptedException -> 0x0175 }
                r7 = com.morphoinc.app.panoramagp3.CameraConstants.EngineSynchronizedObject;	 Catch:{ all -> 0x00cd }
                monitor-enter(r7);	 Catch:{ all -> 0x00cd }
                r8 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ all -> 0x00ca }
                r8 = r8.this$0;	 Catch:{ all -> 0x00ca }
                r8 = r8.isEngineRunning();	 Catch:{ all -> 0x00ca }
                if (r8 != 0) goto L_0x0048;
            L_0x003b:
                r1 = "Camera2App";
                r6 = "attach thread exit. (engine is stop.)";
                com.morphoinc.app.LogFilter.d(r1, r6);	 Catch:{ all -> 0x00ca }
                r10.closeSrc();	 Catch:{ all -> 0x00ca }
                monitor-exit(r7);	 Catch:{ all -> 0x00ca }
                monitor-exit(r5);	 Catch:{ all -> 0x00cd }
                return;
            L_0x0048:
                r8 = r10.attach(r2, r0);	 Catch:{ all -> 0x00ca }
                monitor-exit(r7);	 Catch:{ all -> 0x00ca }
                r7 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ all -> 0x00cd }
                r7 = r7.this$0;	 Catch:{ all -> 0x00cd }
                r7 = r7.mSettings;	 Catch:{ all -> 0x00cd }
                r7 = r7.save_input_images;	 Catch:{ all -> 0x00cd }
                if (r7 == r1) goto L_0x0065;
            L_0x0059:
                r7 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ all -> 0x00cd }
                r7 = r7.this$0;	 Catch:{ all -> 0x00cd }
                r7 = r7.mSettings;	 Catch:{ all -> 0x00cd }
                r7 = r7.save_input_images;	 Catch:{ all -> 0x00cd }
                if (r7 != r4) goto L_0x006c;
            L_0x0065:
                r7 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ all -> 0x00cd }
                r7 = r7.this$0;	 Catch:{ all -> 0x00cd }
                r7.encodeMovie(r0);	 Catch:{ all -> 0x00cd }
            L_0x006c:
                r10.closeSrc();	 Catch:{ all -> 0x00cd }
                if (r8 != 0) goto L_0x007d;
            L_0x0071:
                r1 = "Camera2App";
                r6 = "PreviewAttach.run() attach error.";
                com.morphoinc.app.LogFilter.e(r1, r6);	 Catch:{ all -> 0x00cd }
                r10.mResultCode = r3;	 Catch:{ all -> 0x00cd }
                monitor-exit(r5);	 Catch:{ all -> 0x00cd }
                goto L_0x0174;
            L_0x007d:
                r7 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ all -> 0x00cd }
                r7 = r7.this$0;	 Catch:{ all -> 0x00cd }
                r7 = r7.mSettings;	 Catch:{ all -> 0x00cd }
                r7 = r7.save_input_images;	 Catch:{ all -> 0x00cd }
                if (r7 != 0) goto L_0x00a3;
            L_0x0089:
                r7 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ all -> 0x00cd }
                r7 = r7.preview_skip_count;	 Catch:{ all -> 0x00cd }
                if (r7 > 0) goto L_0x009e;
            L_0x0091:
                r7 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ all -> 0x00cd }
                r9 = r10.pc;	 Catch:{ all -> 0x00cd }
                r7.createPreviewImage(r9);	 Catch:{ all -> 0x00cd }
                r7 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ all -> 0x00cd }
                r7.preview_skip_count = r6;	 Catch:{ all -> 0x00cd }
                goto L_0x00a3;
            L_0x009e:
                r7 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ all -> 0x00cd }
                r7.preview_skip_count = r7.preview_skip_count - 1;	 Catch:{ all -> 0x00cd }
            L_0x00a3:
                r10.checkAttachEnd(r2);	 Catch:{ all -> 0x00cd }
                monitor-exit(r5);	 Catch:{ all -> 0x00cd }
                r5 = r8;
                r7 = r10.mIsAttachEnd;	 Catch:{ InterruptedException -> 0x0175 }
                if (r7 == 0) goto L_0x00ae;
            L_0x00ac:
                goto L_0x0174;
            L_0x00ae:
                r7 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ InterruptedException -> 0x0175 }
                r7 = r7.this$0;	 Catch:{ InterruptedException -> 0x0175 }
                r7 = r7.mActivity;	 Catch:{ InterruptedException -> 0x0175 }
                r8 = r10.postAttachRunnable;	 Catch:{ InterruptedException -> 0x0175 }
                r7.runOnUiThread(r8);	 Catch:{ InterruptedException -> 0x0175 }
                r7 = com.morphoinc.app.panoramagp3.Camera2App.SLEEP_MILLISEC;	 Catch:{ InterruptedException -> 0x00c7 }
                r9 = com.morphoinc.app.panoramagp3.Camera2App.SLEEP_NANOSEC;	 Catch:{ InterruptedException -> 0x00c7 }
                java.lang.Thread.sleep(r7, r9);	 Catch:{ InterruptedException -> 0x00c7 }
                goto L_0x00c8;
            L_0x00c7:
                r7 = move-exception;
            L_0x00c8:
                goto L_0x0013;
            L_0x00ca:
                r1 = move-exception;
                monitor-exit(r7);	 Catch:{ all -> 0x00ca }
                throw r1;	 Catch:{ all -> 0x00cd }
            L_0x00cd:
                r1 = move-exception;
                monitor-exit(r5);	 Catch:{ all -> 0x00cd }
                throw r1;	 Catch:{ InterruptedException -> 0x0175 }
            L_0x00d0:
                r5 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ InterruptedException -> 0x0175 }
                r5 = r5.this$0;	 Catch:{ InterruptedException -> 0x0175 }
                r5 = r5.mAttachImageQueue;	 Catch:{ InterruptedException -> 0x0175 }
                r5 = r5.take();	 Catch:{ InterruptedException -> 0x0175 }
                r5 = (com.morphoinc.app.panoramagp3.CaptureImage) r5;	 Catch:{ InterruptedException -> 0x0175 }
                r0 = r5;
                r7 = com.morphoinc.app.panoramagp3.Camera2App.sAttachExit;	 Catch:{ InterruptedException -> 0x0175 }
                if (r5 == r7) goto L_0x0174;
            L_0x00e5:
                r10.setImage(r0);	 Catch:{ InterruptedException -> 0x0175 }
                r5 = com.morphoinc.app.panoramagp3.CameraConstants.EngineSynchronizedObject;	 Catch:{ InterruptedException -> 0x0175 }
                monitor-enter(r5);	 Catch:{ InterruptedException -> 0x0175 }
                r7 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ all -> 0x0171 }
                r7 = r7.this$0;	 Catch:{ all -> 0x0171 }
                r7 = r7.isEngineRunning();	 Catch:{ all -> 0x0171 }
                if (r7 != 0) goto L_0x0101;
            L_0x00f5:
                r1 = "Camera2App";
                r6 = "attach thread exit. (engine is stop.)";
                com.morphoinc.app.LogFilter.d(r1, r6);	 Catch:{ all -> 0x0171 }
                r10.closeSrc();	 Catch:{ all -> 0x0171 }
                monitor-exit(r5);	 Catch:{ all -> 0x0171 }
                return;
            L_0x0101:
                r7 = r10.attach(r2, r0);	 Catch:{ all -> 0x0171 }
                monitor-exit(r5);	 Catch:{ all -> 0x0171 }
                r5 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ InterruptedException -> 0x0175 }
                r5 = r5.this$0;	 Catch:{ InterruptedException -> 0x0175 }
                r5 = r5.mSettings;	 Catch:{ InterruptedException -> 0x0175 }
                r5 = r5.save_input_images;	 Catch:{ InterruptedException -> 0x0175 }
                if (r5 == r1) goto L_0x011e;
            L_0x0112:
                r5 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ InterruptedException -> 0x0175 }
                r5 = r5.this$0;	 Catch:{ InterruptedException -> 0x0175 }
                r5 = r5.mSettings;	 Catch:{ InterruptedException -> 0x0175 }
                r5 = r5.save_input_images;	 Catch:{ InterruptedException -> 0x0175 }
                if (r5 != r4) goto L_0x0125;
            L_0x011e:
                r5 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ InterruptedException -> 0x0175 }
                r5 = r5.this$0;	 Catch:{ InterruptedException -> 0x0175 }
                r5.encodeMovie(r0);	 Catch:{ InterruptedException -> 0x0175 }
            L_0x0125:
                r10.closeSrc();	 Catch:{ InterruptedException -> 0x0175 }
                if (r7 != 0) goto L_0x0134;
            L_0x012a:
                r1 = "Camera2App";
                r5 = "PreviewAttach.run() attach error.";
                com.morphoinc.app.LogFilter.e(r1, r5);	 Catch:{ InterruptedException -> 0x0175 }
                r10.mResultCode = r3;	 Catch:{ InterruptedException -> 0x0175 }
                goto L_0x0174;
            L_0x0134:
                r5 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ InterruptedException -> 0x0175 }
                r5 = r5.this$0;	 Catch:{ InterruptedException -> 0x0175 }
                r5 = r5.mSettings;	 Catch:{ InterruptedException -> 0x0175 }
                r5 = r5.save_input_images;	 Catch:{ InterruptedException -> 0x0175 }
                if (r5 != 0) goto L_0x015a;
            L_0x0140:
                r5 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ InterruptedException -> 0x0175 }
                r5 = r5.preview_skip_count;	 Catch:{ InterruptedException -> 0x0175 }
                if (r5 > 0) goto L_0x0155;
            L_0x0148:
                r5 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ InterruptedException -> 0x0175 }
                r8 = r10.pc;	 Catch:{ InterruptedException -> 0x0175 }
                r5.createPreviewImage(r8);	 Catch:{ InterruptedException -> 0x0175 }
                r5 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ InterruptedException -> 0x0175 }
                r5.preview_skip_count = r6;	 Catch:{ InterruptedException -> 0x0175 }
                goto L_0x015a;
            L_0x0155:
                r5 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ InterruptedException -> 0x0175 }
                r5.preview_skip_count = r5.preview_skip_count - 1;	 Catch:{ InterruptedException -> 0x0175 }
            L_0x015a:
                r10.checkAttachEnd(r2);	 Catch:{ InterruptedException -> 0x0175 }
                r5 = r10.mIsAttachEnd;	 Catch:{ InterruptedException -> 0x0175 }
                if (r5 == 0) goto L_0x0162;
            L_0x0161:
                goto L_0x0174;
            L_0x0162:
                r5 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;	 Catch:{ InterruptedException -> 0x0175 }
                r5 = r5.this$0;	 Catch:{ InterruptedException -> 0x0175 }
                r5 = r5.mActivity;	 Catch:{ InterruptedException -> 0x0175 }
                r8 = r10.postAttachRunnable;	 Catch:{ InterruptedException -> 0x0175 }
                r5.runOnUiThread(r8);	 Catch:{ InterruptedException -> 0x0175 }
                goto L_0x00d0;
            L_0x0171:
                r1 = move-exception;
                monitor-exit(r5);	 Catch:{ all -> 0x0171 }
                throw r1;	 Catch:{ InterruptedException -> 0x0175 }
            L_0x0174:
                goto L_0x017b;
            L_0x0175:
                r1 = move-exception;
                r1.printStackTrace();
                r10.mResultCode = r3;
            L_0x017b:
                r1 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;
                r1 = r1.this$0;
                r1 = r1.mDateTaken;
                r5 = java.lang.System.currentTimeMillis();
                r1[r4] = r5;
                r1 = com.morphoinc.app.panoramagp3.Camera2App.sAttachExit;
                if (r0 != r1) goto L_0x0197;
            L_0x018f:
                r1 = "Camera2App";
                r3 = "attach thread exit. (request exit)";
                com.morphoinc.app.LogFilter.d(r1, r3);
                return;
            L_0x0197:
                r1 = r10.mResultCode;
                r3 = com.morphoinc.app.panoramagp3.Camera2App.PanoramaPreview.this;
                r3 = r3.this$0;
                r3 = r3.mActivity;
                r4 = new com.morphoinc.app.panoramagp3.Camera2App$PanoramaPreview$PreviewAttach$1;
                r4.<init>(r1);
                r3.runOnUiThread(r4);
                r3 = "Camera2App";
                r4 = "attach thread exit.";
                com.morphoinc.app.LogFilter.d(r3, r4);
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.morphoinc.app.panoramagp3.Camera2App$PanoramaPreview$PreviewAttach.run():void");
            }

            private boolean attach(double[] center, CaptureImage image) {
                PanoramaPreview.this.this$0.setSensorFusionValue(image);
                this.pc.start();
                int morRet = PanoramaPreview.this.this$0.mMorphoPanoramaGP3.attach(this.byteBuffer[0], this.byteBuffer[1], this.byteBuffer[2], this.rowStride[0], this.rowStride[1], this.rowStride[2], this.pixelStride[0], this.pixelStride[1], this.pixelStride[2], PanoramaPreview.this.this$0.mCurrentSensorInfoManager, center, PanoramaPreview.this.this$0.mActivity.getBaseContext());
                this.pc.stop();
                this.pc.putLog("Camera2App", "mMorphoPanoramaGP3.attach");
                if (morRet == 0) {
                    return true;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mMorphoPanoramaGP3.attach error ret:");
                stringBuilder.append(morRet);
                LogFilter.e("Camera2App", stringBuilder.toString());
                return false;
            }

            private void checkAttachEnd(double[] center) {
                if (PanoramaPreview.this.this$0.mSettings.save_input_images == 0) {
                    int detect_result = PanoramaPreview.this.detector.detect(center[0], center[1]);
                    if (detect_result != 1) {
                        switch (detect_result) {
                            case -4:
                                PanoramaPreview.this.this$0.playPanoSound();
                                this.mResultCode = 0;
                                this.mIsAttachEnd = true;
                                PanoramaPreview.this.this$0.mWarningMessage.setVisibility(4);
                                PanoramaPreview.this.this$0.mHandler.postDelayed(new Runnable() {
                                    public void run() {
                                        ToastUtil.showToast(PanoramaPreview.this.this$0.mActivity, PanoramaPreview.this.this$0.mActivity.getString(R.string.movement_too_fast_toast), 1);
                                    }
                                }, 100);
                                break;
                            case -3:
                                PanoramaPreview.this.this$0.playPanoSound();
                                this.mResultCode = 0;
                                this.mIsAttachEnd = true;
                                PanoramaPreview.this.this$0.mShaked = false;
                                PanoramaPreview.this.this$0.mWarningMessage.setVisibility(4);
                                PanoramaPreview.this.this$0.mHandler.postDelayed(new Runnable() {
                                    public void run() {
                                        ToastUtil.showToast(PanoramaPreview.this.this$0.mActivity, PanoramaPreview.this.this$0.mActivity.getString(R.string.too_much_camera_shake), 1);
                                    }
                                }, 100);
                                break;
                            case -2:
                                PanoramaPreview.this.this$0.playPanoSound();
                                this.mResultCode = 0;
                                this.mIsAttachEnd = true;
                                PanoramaPreview.this.this$0.mWarningMessage.setVisibility(4);
                                PanoramaPreview.this.this$0.mHandler.postDelayed(new Runnable() {
                                    public void run() {
                                        ToastUtil.showToast(PanoramaPreview.this.this$0.mActivity, PanoramaPreview.this.this$0.mActivity.getString(R.string.wrong_dierction_toast), 1);
                                    }
                                }, 100);
                                break;
                            case -1:
                                PanoramaPreview.this.this$0.playPanoSound();
                                this.mResultCode = 0;
                                this.mIsAttachEnd = true;
                                PanoramaPreview.this.this$0.mWarningMessage.setVisibility(4);
                                PanoramaPreview.this.this$0.mHandler.postDelayed(new Runnable() {
                                    public void run() {
                                        ToastUtil.showToast(PanoramaPreview.this.this$0.mActivity, PanoramaPreview.this.this$0.mActivity.getString(R.string.only_one_frame_taken_toast), 1);
                                    }
                                }, 100);
                                break;
                        }
                    }
                    PanoramaPreview.this.this$0.playPanoSound();
                    this.mResultCode = 0;
                    this.mIsAttachEnd = true;
                    PanoramaPreview.this.uiUpdateRunnable.setDetectResult(detect_result);
                    PanoramaPreview.this.this$0.mActivity.runOnUiThread(PanoramaPreview.this.uiUpdateRunnable);
                } else if (PanoramaPreview.this.this$0.mRoundDetector.detect()) {
                    this.mResultCode = 0;
                    this.mIsAttachEnd = true;
                }
                if (!this.mIsAttachEnd && PanoramaPreview.this.this$0.mDirectionFunction.isImageComplete()) {
                    this.mResultCode = 0;
                    this.mIsAttachEnd = true;
                }
            }
        }

        public PanoramaPreview(Camera2App camera2App) {
            View view;
            boolean z;
            Camera2App camera2App2 = camera2App;
            this.this$0 = camera2App2;
            int scale = camera2App.mDirectionFunction.getScale();
            Size previewSize = camera2App.mDirectionFunction.getPreviewSize();
            LogFilter.d("Camera2App", String.format(Locale.US, "previewSize %dx%d, scale %d", new Object[]{Integer.valueOf(previewSize.getWidth()), Integer.valueOf(previewSize.getHeight()), Integer.valueOf(scale)}));
            int direction = camera2App.mDirectionFunction.getDirection();
            if (camera2App.mSurfaceView == null || camera2App.mSurfaceView.getVisibility() != 0) {
                view = camera2App.mTextureView;
            } else {
                view = camera2App.mSurfaceView;
            }
            if (camera2App.mInitParam.output_rotation == 0 || camera2App.mInitParam.output_rotation == MediaProviderUtils.ROTATION_180) {
                if (direction == 3 || direction == 2) {
                    this.mPreviewHeight = 860;
                    this.mPreviewWidth = 168;
                    this.mDrawPreviewFitScale = ((float) view.getWidth()) / ((float) this.mPreviewHeight);
                } else {
                    this.mPreviewWidth = 1120;
                    this.mPreviewHeight = MediaProviderUtils.ROTATION_180;
                    this.mDrawPreviewFitScale = ((((float) view.getHeight()) / ((float) this.mPreviewWidth)) * Camera2App.PREVIEW_LONG_SIDE_CROP_RATIO) + 0.2f;
                }
            } else if (direction == 3 || direction == 2) {
                this.mPreviewWidth = MediaProviderUtils.ROTATION_180;
                this.mPreviewHeight = 1120;
                this.mDrawPreviewFitScale = ((((float) view.getHeight()) / ((float) this.mPreviewHeight)) * Camera2App.PREVIEW_LONG_SIDE_CROP_RATIO) + 0.2f;
            } else {
                this.mPreviewHeight = 168;
                this.mPreviewWidth = 860;
                this.mDrawPreviewFitScale = ((float) view.getWidth()) / ((float) this.mPreviewWidth);
            }
            camera2App2.mPreviewBitmap = Bitmap.createBitmap(this.mPreviewWidth, this.mPreviewHeight, Config.ARGB_8888);
            int ret = camera2App.mMorphoPanoramaGP3.setPreviewImage(this.mPreviewWidth, this.mPreviewHeight);
            if (ret != 0) {
                Object[] objArr = new Object[1];
                z = false;
                objArr[0] = Integer.valueOf(ret);
                LogFilter.e("Camera2App", String.format(Locale.US, "MorphoSensorFusion.setPreviewImage error ret:0x%08X", objArr));
            } else {
                z = false;
            }
            if (((double) this.mDrawPreviewFitScale) < 0.99d || 1.01d < ((double) this.mDrawPreviewFitScale)) {
                Matrix matrix = new Matrix();
                matrix.setRotate((float) (360 - camera2App.mCurOrientation));
                matrix.postScale(this.mDrawPreviewFitScale, this.mDrawPreviewFitScale);
                camera2App2.mPreviewFitBitmap = Bitmap.createBitmap(camera2App.mPreviewBitmap, 0, 0, camera2App.mPreviewBitmap.getWidth(), camera2App.mPreviewBitmap.getHeight(), matrix, true);
                camera2App2.mPreviewFitBitmapCanvas = new Canvas(camera2App.mPreviewFitBitmap);
                camera2App2.mPreviewFitBitmapPaint = new Paint();
                camera2App.mPreviewFitBitmapPaint.setXfermode(new PorterDuffXfermode(Mode.SRC));
                camera2App2.mPreviewFitMatrix = new Matrix();
                int access$4600 = camera2App.mCurOrientation;
                if (access$4600 == 90) {
                    camera2App.mPreviewFitMatrix.setRotate((float) (360 - camera2App.mCurOrientation));
                    camera2App.mPreviewFitMatrix.postTranslate(0.0f, (float) camera2App.mPreviewBitmap.getWidth());
                } else if (access$4600 != MediaProviderUtils.ROTATION_270) {
                    camera2App.mPreviewFitMatrix.setRotate((float) camera2App.mCurOrientation, ((float) camera2App.mPreviewBitmap.getWidth()) / 2.0f, ((float) camera2App.mPreviewBitmap.getHeight()) / 2.0f);
                } else {
                    camera2App.mPreviewFitMatrix.setRotate((float) (360 - camera2App.mCurOrientation));
                    camera2App.mPreviewFitMatrix.postTranslate((float) camera2App.mPreviewBitmap.getHeight(), 0.0f);
                }
                camera2App.mPreviewFitMatrix.postScale(this.mDrawPreviewFitScale, this.mDrawPreviewFitScale);
            } else {
                camera2App2.mPreviewFitBitmap = null;
            }
            this.detector = new PositionDetector(this, direction, camera2App.mMaxWidth, camera2App.mMaxHeight);
            if (direction == 0 || direction == 1) {
                camera2App.mRoundDetector.setStartPosition(camera2App.mInitParam.output_rotation, direction, camera2App.mViewAngleH, camera2App.mViewAngleV, camera2App.mSettings.make_360);
            }
            MorphoPanoramaGP3 access$3900 = camera2App.mMorphoPanoramaGP3;
            if (camera2App.mSettings.save_input_images == 0) {
                z = true;
            }
            access$3900.setAttachEnabled(z);
            camera2App.mRootView.findViewById(R.id.mini_preview_guide).setVisibility(4);
            camera2App.mMiniPreviewImageView.setVisibility(4);
            if (Camera2App.PREVIEW_SPREAD_BOTH_SIDES) {
                AlphaAnimation aa = new AlphaAnimation(1.0f, 0.0f);
                aa.setDuration(400);
                camera2App.mMiniPreviewImageView.startAnimation(aa);
                return;
            }
            float toX;
            float toY;
            AnimationSet set = new AnimationSet(true);
            AlphaAnimation aa2 = new AlphaAnimation(1.0f, 0.0f);
            if (camera2App.mInitParam.output_rotation != 90 && camera2App.mInitParam.output_rotation != MediaProviderUtils.ROTATION_270) {
                switch (direction) {
                    case 0:
                        toX = 0.0f;
                        toY = 0.45f;
                        break;
                    case 1:
                        toX = 0.0f;
                        toY = -0.45f;
                        break;
                    case 2:
                        toX = -0.45f;
                        toY = 0.0f;
                        break;
                    case 3:
                        toX = 0.45f;
                        toY = 0.0f;
                        break;
                }
            }
            switch (direction) {
                case 0:
                    toX = 0.45f;
                    toY = 0.0f;
                    break;
                case 1:
                    toX = -0.45f;
                    toY = 0.0f;
                    break;
                case 2:
                    toX = 0.0f;
                    toY = 0.45f;
                    break;
                case 3:
                    toX = 0.0f;
                    toY = -0.45f;
                    break;
            }
            toX = 0.0f;
            toY = 0.0f;
            TranslateAnimation ta = new TranslateAnimation(2, 0.0f, 2, toX, 2, 0.0f, 2, toY);
            set.setDuration(400);
            set.addAnimation(aa2);
            set.addAnimation(ta);
            set.setInterpolator(new AccelerateInterpolator());
            camera2App.mMiniPreviewImageView.startAnimation(set);
        }

        /* JADX WARNING: Missing block: B:12:0x0060, code skipped:
            return;
     */
        private void createPreviewImage(com.morphoinc.app.panoramagp3.PerformanceCounter r7) {
            /*
            r6 = this;
            r0 = com.morphoinc.app.panoramagp3.Camera2App.PreviewImageSynchronizedObject;
            monitor-enter(r0);
            r7.start();	 Catch:{ all -> 0x0061 }
            r1 = r6.this$0;	 Catch:{ all -> 0x0061 }
            r1 = r1.mMorphoPanoramaGP3;	 Catch:{ all -> 0x0061 }
            r2 = r6.this$0;	 Catch:{ all -> 0x0061 }
            r2 = r2.mPreviewBitmap;	 Catch:{ all -> 0x0061 }
            r1 = r1.updatePreviewImage(r2);	 Catch:{ all -> 0x0061 }
            r7.stop();	 Catch:{ all -> 0x0061 }
            r2 = "Camera2App";
            r3 = "mMorphoPanoramaGP3.updatePreviewImage";
            r7.putLog(r2, r3);	 Catch:{ all -> 0x0061 }
            if (r1 == 0) goto L_0x003c;
        L_0x0024:
            r2 = "Camera2App";
            r3 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0061 }
            r3.<init>();	 Catch:{ all -> 0x0061 }
            r4 = "mMorphoPanoramaGP3.updatePreviewImage error ret:";
            r3.append(r4);	 Catch:{ all -> 0x0061 }
            r3.append(r1);	 Catch:{ all -> 0x0061 }
            r3 = r3.toString();	 Catch:{ all -> 0x0061 }
            com.morphoinc.app.LogFilter.e(r2, r3);	 Catch:{ all -> 0x0061 }
            monitor-exit(r0);	 Catch:{ all -> 0x0061 }
            return;
        L_0x003c:
            r2 = r6.this$0;	 Catch:{ all -> 0x0061 }
            r2 = r2.mPreviewFitBitmap;	 Catch:{ all -> 0x0061 }
            if (r2 == 0) goto L_0x005f;
        L_0x0044:
            r2 = r6.this$0;	 Catch:{ all -> 0x0061 }
            r2 = r2.mPreviewFitBitmapCanvas;	 Catch:{ all -> 0x0061 }
            r3 = r6.this$0;	 Catch:{ all -> 0x0061 }
            r3 = r3.mPreviewBitmap;	 Catch:{ all -> 0x0061 }
            r4 = r6.this$0;	 Catch:{ all -> 0x0061 }
            r4 = r4.mPreviewFitMatrix;	 Catch:{ all -> 0x0061 }
            r5 = r6.this$0;	 Catch:{ all -> 0x0061 }
            r5 = r5.mPreviewFitBitmapPaint;	 Catch:{ all -> 0x0061 }
            r2.drawBitmap(r3, r4, r5);	 Catch:{ all -> 0x0061 }
        L_0x005f:
            monitor-exit(r0);	 Catch:{ all -> 0x0061 }
            return;
        L_0x0061:
            r1 = move-exception;
            monitor-exit(r0);	 Catch:{ all -> 0x0061 }
            throw r1;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.morphoinc.app.panoramagp3.Camera2App$PanoramaPreview.createPreviewImage(com.morphoinc.app.panoramagp3.PerformanceCounter):void");
        }

        public boolean onSaveImage(CaptureImage image) {
            if (this.this$0.isEngineRunning()) {
                this.this$0.addAttachQueue(image);
                if (this.mAttachRunnable == null) {
                    this.mAttachRunnable = new PreviewAttach();
                    this.this$0.mExecutor.submit(this.mAttachRunnable);
                }
                return true;
            }
            LogFilter.e("Camera2App", "PanoramaPreview.onSaveImage mMorphoPanoramaGP3 is null!!");
            image.close();
            return false;
        }

        private void attachEnd(int code) {
            this.this$0.initAttachQueue();
            this.listener.requestEnd(this, code);
        }

        public boolean hasImage() {
            return true;
        }

        public void repeatTakePicture() {
            switch (this.this$0.mSettings.getCaptureMode()) {
                case 1:
                    this.this$0.mMorphoCamera.takePicture();
                    return;
                case 2:
                    this.this$0.mMorphoCamera.takePictureZSL();
                    return;
                case 3:
                    this.this$0.mMorphoCamera.takePictureBurst();
                    return;
                default:
                    return;
            }
        }

        public boolean isEnableTvAnalysis() {
            return this.detector.isEnableTvAnalysis();
        }

        public void notifyTvAnalyzed() {
            this.detector.notifyTvAnalyzed();
        }

        public boolean isEnableTvChange() {
            return this.detector.isEnableTvChange();
        }

        public void notifyTvChanged() {
            this.detector.notifyTvChanged();
        }
    }

    private class SavePictureState extends CameraState implements ISaveThreadEventListener {
        private String imageFormat;
        private boolean isPanoramaSave;

        /* JADX WARNING: Missing block: B:23:0x0075, code skipped:
            if (r2 != false) goto L_0x0078;
     */
        /* JADX WARNING: Missing block: B:24:0x0077, code skipped:
            return false;
     */
        /* JADX WARNING: Missing block: B:25:0x0078, code skipped:
            if (r4 == null) goto L_0x018a;
     */
        /* JADX WARNING: Missing block: B:26:0x007a, code skipped:
            r0 = new java.lang.StringBuilder();
            r0.append(com.morphoinc.app.panoramagp3.Camera2App.access$4300(com.morphoinc.app.panoramagp3.Camera2App.access$4200(r1.this$0)[0]));
            r0.append(com.hmdglobal.app.camera.Storage.JPEG_POSTFIX);
            r3 = (com.hmdglobal.app.camera.app.CameraApp) com.morphoinc.app.panoramagp3.Camera2App.access$200(r1.this$0).getApplication();
            r12 = r0.toString();
            r13 = com.morphoinc.app.panoramagp3.Camera2App.access$4500(r1.this$0, com.morphoinc.app.panoramagp3.Camera2App.access$4400(r1.this$0), r12);
     */
        /* JADX WARNING: Missing block: B:27:0x00af, code skipped:
            if (r13 == null) goto L_0x0176;
     */
        /* JADX WARNING: Missing block: B:28:0x00b1, code skipped:
            r14 = com.morphoinc.app.panoramagp3.MorphoPanoramaGP3.saveNotPanorama(r4, r13);
     */
        /* JADX WARNING: Missing block: B:29:0x00b6, code skipped:
            if (r14 == 0) goto L_0x00d3;
     */
        /* JADX WARNING: Missing block: B:30:0x00b8, code skipped:
            r6 = new java.lang.StringBuilder();
            r6.append("mMorphoPanoramaGP3.saveNotPanorama error ret:");
            r6.append(r14);
            com.morphoinc.app.LogFilter.e("Camera2App", r6.toString());
            r2 = false;
            r18 = r0;
     */
        /* JADX WARNING: Missing block: B:31:0x00d3, code skipped:
            r5 = null;
     */
        /* JADX WARNING: Missing block: B:32:0x00e6, code skipped:
            if (com.morphoinc.app.panoramagp3.Camera2App.access$200(r1.this$0).getSettingsManager().getBoolean(com.hmdglobal.app.camera.settings.SettingsManager.SCOPE_GLOBAL, com.hmdglobal.app.camera.settings.Keys.KEY_RECORD_LOCATION) == false) goto L_0x00f6;
     */
        /* JADX WARNING: Missing block: B:33:0x00e8, code skipped:
            r5 = com.morphoinc.app.panoramagp3.Camera2App.access$200(r1.this$0).getLocationManager().getCurrentLocation();
     */
        /* JADX WARNING: Missing block: B:34:0x00f6, code skipped:
            r15 = r5;
            r11 = com.morphoinc.app.panoramagp3.Camera2App.access$1400(r1.this$0).cameraInfo().getCaptureWidth();
            r10 = com.morphoinc.app.panoramagp3.Camera2App.access$1400(r1.this$0).cameraInfo().getCaptureHeight();
            r6 = new java.lang.StringBuilder();
            r6.append("location 1 = ");
            r6.append(r15);
            com.morphoinc.app.LogFilter.e("Camera2App", r6.toString());
            com.morphoinc.app.panoramagp3.Camera2App.access$4900(r13, r15, 360 - com.morphoinc.app.panoramagp3.Camera2App.access$4600(r1.this$0), com.morphoinc.app.panoramagp3.Camera2App.access$4700(r1.this$0), com.morphoinc.app.panoramagp3.Camera2App.access$4800(r1.this$0));
            r18 = r0;
            r0 = r10;
            r19 = r2;
            r2 = r11;
            com.morphoinc.app.panoramagp3.Camera2App.access$5000(r1.this$0, r12, r13, r15, com.morphoinc.app.panoramagp3.Camera2App.access$4600(r1.this$0), r1.this$0, null);
            com.morphoinc.app.panoramagp3.Camera2App.scanFile(com.morphoinc.app.panoramagp3.Camera2App.access$200(r1.this$0).getBaseContext(), new java.io.File(r13));
            r3.setLastShotFile(r13, r2, r0);
            r2 = r19;
     */
        /* JADX WARNING: Missing block: B:35:0x0176, code skipped:
            r18 = r0;
            r19 = r2;
            com.morphoinc.app.LogFilter.e("Camera2App", "FileWriteErr");
            r3.clearLastShotFile();
            r2 = false;
     */
        /* JADX WARNING: Missing block: B:36:0x0186, code skipped:
            r4.close();
     */
        /* JADX WARNING: Missing block: B:37:0x018a, code skipped:
            r19 = r2;
     */
        /* JADX WARNING: Missing block: B:38:0x018c, code skipped:
            return r2;
     */
        private boolean saveNotPanoramaPicture() {
            /*
            r20 = this;
            r1 = r20;
            r2 = 1;
            r0 = 0;
            r3 = com.morphoinc.app.panoramagp3.CameraConstants.EngineSynchronizedObject;
            monitor-enter(r3);
            r4 = r0;
        L_0x0008:
            r0 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ InterruptedException -> 0x0020 }
            r0 = r0.mAttachImageQueue;	 Catch:{ InterruptedException -> 0x0020 }
            r0 = r0.take();	 Catch:{ InterruptedException -> 0x0020 }
            r0 = (com.morphoinc.app.panoramagp3.CaptureImage) r0;	 Catch:{ InterruptedException -> 0x0020 }
            r4 = r0;
            r0 = com.morphoinc.app.panoramagp3.Camera2App.sAttachExit;	 Catch:{ InterruptedException -> 0x0020 }
            if (r4 == r0) goto L_0x0008;
            goto L_0x0025;
        L_0x001d:
            r0 = move-exception;
            goto L_0x0190;
        L_0x0020:
            r0 = move-exception;
            r0.printStackTrace();	 Catch:{ all -> 0x001d }
            r2 = 0;
        L_0x0025:
            r0 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x018d }
            r5 = new com.morphoinc.app.panoramagp3.PanoramaState;	 Catch:{ all -> 0x018d }
            r5.<init>();	 Catch:{ all -> 0x018d }
            r0.mPanoramaState = r5;	 Catch:{ all -> 0x018d }
            r0 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x018d }
            r0.setAttachExit();	 Catch:{ all -> 0x018d }
            r0 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x018d }
            r0 = r0.mMorphoPanoramaGP3;	 Catch:{ all -> 0x018d }
            r5 = 0;
            if (r0 == 0) goto L_0x0066;
        L_0x003d:
            r0 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x001d }
            r0 = r0.mMorphoPanoramaGP3;	 Catch:{ all -> 0x001d }
            r6 = 0;
            r8 = 1;
            r0 = r0.end(r8, r6);	 Catch:{ all -> 0x001d }
            if (r0 == 0) goto L_0x0061;
        L_0x004c:
            r6 = "Camera2App";
            r7 = java.util.Locale.US;	 Catch:{ all -> 0x001d }
            r9 = "MorphoPanoramaGP3.end error ret:0x%08X";
            r8 = new java.lang.Object[r8];	 Catch:{ all -> 0x001d }
            r10 = java.lang.Integer.valueOf(r0);	 Catch:{ all -> 0x001d }
            r8[r5] = r10;	 Catch:{ all -> 0x001d }
            r7 = java.lang.String.format(r7, r9, r8);	 Catch:{ all -> 0x001d }
            com.morphoinc.app.LogFilter.e(r6, r7);	 Catch:{ all -> 0x001d }
        L_0x0061:
            r6 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x001d }
            r6.finishEngine();	 Catch:{ all -> 0x001d }
        L_0x0066:
            r0 = com.morphoinc.app.panoramagp3.Camera2App.this;	 Catch:{ all -> 0x018d }
            r0 = r0.mActivity;	 Catch:{ all -> 0x018d }
            r6 = new com.morphoinc.app.panoramagp3.Camera2App$SavePictureState$1;	 Catch:{ all -> 0x018d }
            r6.<init>();	 Catch:{ all -> 0x018d }
            r0.runOnUiThread(r6);	 Catch:{ all -> 0x018d }
            monitor-exit(r3);	 Catch:{ all -> 0x018d }
            if (r2 != 0) goto L_0x0078;
        L_0x0077:
            return r5;
        L_0x0078:
            if (r4 == 0) goto L_0x018a;
        L_0x007a:
            r0 = new java.lang.StringBuilder;
            r0.<init>();
            r3 = com.morphoinc.app.panoramagp3.Camera2App.this;
            r3 = r3.mDateTaken;
            r5 = r3[r5];
            r3 = com.morphoinc.app.panoramagp3.Camera2App.createName(r5);
            r0.append(r3);
            r3 = ".jpg";
            r0.append(r3);
            r3 = com.morphoinc.app.panoramagp3.Camera2App.this;
            r3 = r3.mActivity;
            r3 = r3.getApplication();
            r3 = (com.hmdglobal.app.camera.app.CameraApp) r3;
            r12 = r0.toString();
            r5 = com.morphoinc.app.panoramagp3.Camera2App.this;
            r6 = com.morphoinc.app.panoramagp3.Camera2App.this;
            r6 = r6.mFolderPath;
            r13 = r5.getSaveFilePath(r6, r12);
            if (r13 == 0) goto L_0x0176;
        L_0x00b1:
            r5 = 0;
            r14 = com.morphoinc.app.panoramagp3.MorphoPanoramaGP3.saveNotPanorama(r4, r13);
            if (r14 == 0) goto L_0x00d3;
        L_0x00b8:
            r5 = "Camera2App";
            r6 = new java.lang.StringBuilder;
            r6.<init>();
            r7 = "mMorphoPanoramaGP3.saveNotPanorama error ret:";
            r6.append(r7);
            r6.append(r14);
            r6 = r6.toString();
            com.morphoinc.app.LogFilter.e(r5, r6);
            r2 = 0;
            r18 = r0;
            goto L_0x0175;
        L_0x00d3:
            r5 = 0;
            r6 = com.morphoinc.app.panoramagp3.Camera2App.this;
            r6 = r6.mActivity;
            r6 = r6.getSettingsManager();
            r7 = "default_scope";
            r8 = "pref_camera_recordlocation_key";
            r6 = r6.getBoolean(r7, r8);
            if (r6 == 0) goto L_0x00f6;
        L_0x00e8:
            r6 = com.morphoinc.app.panoramagp3.Camera2App.this;
            r6 = r6.mActivity;
            r6 = r6.getLocationManager();
            r5 = r6.getCurrentLocation();
        L_0x00f6:
            r15 = r5;
            r5 = com.morphoinc.app.panoramagp3.Camera2App.this;
            r5 = r5.mMorphoCamera;
            r5 = r5.cameraInfo();
            r11 = r5.getCaptureWidth();
            r5 = com.morphoinc.app.panoramagp3.Camera2App.this;
            r5 = r5.mMorphoCamera;
            r5 = r5.cameraInfo();
            r10 = r5.getCaptureHeight();
            r5 = "Camera2App";
            r6 = new java.lang.StringBuilder;
            r6.<init>();
            r7 = "location 1 = ";
            r6.append(r7);
            r6.append(r15);
            r6 = r6.toString();
            com.morphoinc.app.LogFilter.e(r5, r6);
            r5 = com.morphoinc.app.panoramagp3.Camera2App.this;
            r5 = r5.mCurOrientation;
            r5 = 360 - r5;
            r6 = com.morphoinc.app.panoramagp3.Camera2App.this;
            r6 = r6.mCameraOrientation;
            r7 = com.morphoinc.app.panoramagp3.Camera2App.this;
            r7 = r7.mIsFrontCamera;
            com.morphoinc.app.panoramagp3.Camera2App.setInExif(r13, r15, r5, r6, r7);
            r5 = com.morphoinc.app.panoramagp3.Camera2App.this;
            r6 = com.morphoinc.app.panoramagp3.Camera2App.this;
            r9 = r6.mCurOrientation;
            r8 = com.morphoinc.app.panoramagp3.Camera2App.this;
            r16 = 0;
            r6 = r12;
            r7 = r13;
            r17 = r8;
            r8 = r15;
            r18 = r0;
            r0 = r10;
            r10 = r17;
            r19 = r2;
            r2 = r11;
            r11 = r16;
            r5.insertMediaStore(r6, r7, r8, r9, r10, r11);
            r5 = com.morphoinc.app.panoramagp3.Camera2App.this;
            r5 = r5.mActivity;
            r5 = r5.getBaseContext();
            r6 = new java.io.File;
            r6.<init>(r13);
            com.morphoinc.app.panoramagp3.Camera2App.scanFile(r5, r6);
            r3.setLastShotFile(r13, r2, r0);
            r2 = r19;
        L_0x0175:
            goto L_0x0186;
        L_0x0176:
            r18 = r0;
            r19 = r2;
            r0 = "Camera2App";
            r2 = "FileWriteErr";
            com.morphoinc.app.LogFilter.e(r0, r2);
            r3.clearLastShotFile();
            r0 = 0;
            r2 = r0;
        L_0x0186:
            r4.close();
            goto L_0x018c;
        L_0x018a:
            r19 = r2;
        L_0x018c:
            return r2;
        L_0x018d:
            r0 = move-exception;
            r19 = r2;
        L_0x0190:
            monitor-exit(r3);	 Catch:{ all -> 0x001d }
            throw r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.morphoinc.app.panoramagp3.Camera2App$SavePictureState.saveNotPanoramaPicture():boolean");
        }

        public SavePictureState(boolean panorama) {
            this.isPanoramaSave = panorama;
        }

        public boolean onFinish() {
            return true;
        }

        public boolean requestSaveProcess() {
            if (!this.isPanoramaSave) {
                return saveNotPanoramaPicture();
            }
            if (Camera2App.this.mMorphoPanoramaGP3 == null) {
                LogFilter.e("Camera2App", "mMorphoPanoramaGP3 is null");
                return false;
            }
            int isoAve;
            Camera2App.this.mShotSettings.noiseReductionStrength = 0;
            if (Camera2App.this.mSettings.nr_auto) {
                isoAve = Camera2App.this.mSensorSensitivityAverageManager.get();
                if (isoAve >= 300 && isoAve <= 400) {
                    Camera2App.this.mShotSettings.noiseReductionStrength = 8;
                } else if (isoAve > 400) {
                    Camera2App.this.mShotSettings.noiseReductionStrength = Math.round(((float) isoAve) * 0.0375f);
                } else {
                    Camera2App.this.mShotSettings.noiseReductionStrength = 0;
                }
                if (600 < isoAve) {
                    Camera2App.this.mShotSettings.unsharpStrength = 3072;
                } else {
                    Camera2App.this.mShotSettings.unsharpStrength = UnsharpStrengthPreference.DEFAULT_VALUE;
                }
            } else {
                Camera2App.this.mShotSettings.noiseReductionStrength = Camera2App.this.mSettings.nr_strength;
                Camera2App.this.mShotSettings.unsharpStrength = Camera2App.this.mSettings.unsharp_strength;
            }
            isoAve = Camera2App.this.mMorphoPanoramaGP3.setNoiseReductionParam(Camera2App.this.mShotSettings.noiseReductionStrength);
            if (isoAve != 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mMorphoPanoramaGP3.setNoiseReductionParam error ret:");
                stringBuilder.append(isoAve);
                LogFilter.e("Camera2App", stringBuilder.toString());
                return false;
            }
            if (Camera2App.this.mMorphoPanoramaGP3.setUnsharpStrength(Camera2App.this.mShotSettings.unsharpStrength) != 0) {
                LogFilter.e("Camera2App", String.format(Locale.US, "MorphoPanoramaGP3.setUnsharpStrength error ret:0x%08X", new Object[]{Integer.valueOf(Camera2App.this.mMorphoPanoramaGP3.setUnsharpStrength(Camera2App.this.mShotSettings.unsharpStrength))}));
                return false;
            }
            StringBuilder stringBuilder2;
            isoAve = Camera2App.this.mMorphoPanoramaGP3.end(Camera2App.this.mSettings.make_360 ^ 1, (double) Camera2App.this.mRoundDetector.currentDegree0Base());
            if (isoAve != 0) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("mMorphoPanoramaGP3.end error ret:");
                stringBuilder2.append(isoAve);
                LogFilter.e("Camera2App", stringBuilder2.toString());
            }
            Camera2App.this.mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    Camera2App.this.releaseImageBitmap();
                }
            });
            Rect clippingRect = new Rect();
            isoAve = Camera2App.this.mMorphoPanoramaGP3.getClippingRect(clippingRect);
            if (isoAve != 0) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("mMorphoPanoramaGP3.getClippingRect error ret:");
                stringBuilder2.append(isoAve);
                LogFilter.e("Camera2App", stringBuilder2.toString());
                return false;
            }
            LogFilter.i("Camera2App", String.format(Locale.US, "ClippingRect(Save) (%d,%d)-(%d,%d) %dx%d", new Object[]{Integer.valueOf(clippingRect.left), Integer.valueOf(clippingRect.top), Integer.valueOf(clippingRect.right), Integer.valueOf(clippingRect.bottom), Integer.valueOf(clippingRect.width()), Integer.valueOf(clippingRect.height())}));
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("ClippingRect.left");
            stringBuilder3.append(clippingRect.left);
            stringBuilder3.append("Clipping right");
            stringBuilder3.append(clippingRect.right);
            Log.i("Camera2App", stringBuilder3.toString());
            int outWidth = clippingRect.width();
            int outHeight = clippingRect.height();
            isoAve = Camera2App.this.mMorphoPanoramaGP3.createOutputImage(clippingRect);
            if (isoAve != 0) {
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("mMorphoPanoramaGP3.createOutputImage error ret:");
                stringBuilder4.append(isoAve);
                LogFilter.e("Camera2App", stringBuilder4.toString());
                return false;
            }
            boolean isLGE;
            String outputFileName;
            int outWidth2;
            int outHeight2;
            Object[] objArr;
            this.imageFormat = Camera2App.this.mInitParam.output_format;
            int endStatus = Camera2App.this.mMorphoPanoramaGP3.getEndStatus();
            float fps = Camera2App.this.mMorphoPanoramaGP3.getAttachFps();
            float ave = Camera2App.this.mMorphoPanoramaGP3.getAttachAve();
            float std = Camera2App.this.mMorphoPanoramaGP3.getAttachStandardDeviation();
            GravityParam gravity = Camera2App.this.mMorphoPanoramaGP3.getLastGravity();
            Location location = null;
            if (Camera2App.this.mActivity.getSettingsManager().getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_RECORD_LOCATION)) {
                location = Camera2App.this.mActivity.getLocationManager().getCurrentLocation();
            }
            Location location2 = location;
            StringBuilder filenameBuilder = new StringBuilder();
            filenameBuilder.append(Camera2App.createName(Camera2App.this.mDateTaken[0]));
            filenameBuilder.append(Storage.JPEG_POSTFIX);
            String outputFileName2 = filenameBuilder.toString();
            String filepath = Camera2App.this.getSaveFilePath(Camera2App.this.mFolderPath, outputFileName2);
            Location location3 = location2;
            String first_date = Camera2App.createDateStringForAppSeg(Camera2App.this.mDateTaken[0]);
            String last_date = Camera2App.createDateStringForAppSeg(Camera2App.this.mDateTaken[1]);
            boolean isLGE2 = BuildUtil.isG5() || BuildUtil.isV20();
            boolean z = isLGE2 && endStatus == 0;
            boolean addMargin = z;
            GalleryInfoData galleryInfoData = new GalleryInfoData();
            StringBuilder stringBuilder5;
            if (Camera2App.this.mSettings.use_wdr2) {
                isLGE = isLGE2;
                outputFileName = outputFileName2;
                isoAve = Camera2App.this.mMorphoPanoramaGP3.preparePanorama360(outWidth, outHeight, first_date, last_date, addMargin, galleryInfoData);
                if (isoAve != 0) {
                    stringBuilder5 = new StringBuilder();
                    stringBuilder5.append("preparePanorama360 ret = ");
                    stringBuilder5.append(isoAve);
                    LogFilter.e("PanoramaGP3", stringBuilder5.toString());
                    return false;
                }
            }
            isLGE = isLGE2;
            outputFileName = outputFileName2;
            StringBuilder stringBuilder6 = filenameBuilder;
            isoAve = Camera2App.this.mMorphoPanoramaGP3.savePanorama360(outWidth, outHeight, filepath, first_date, last_date, addMargin, galleryInfoData, isLGE);
            if (isoAve != 0) {
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("savePanorama360 ret = ");
                stringBuilder5.append(isoAve);
                LogFilter.e("PanoramaGP3", stringBuilder5.toString());
                return false;
            }
            StringBuilder logStringBuilder = new StringBuilder();
            logStringBuilder.append("***** Init param *****\r\n");
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{"input_format   ", Camera2App.SETTING_SEPARATOR, Camera2App.this.mInitParam.input_format}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{"output_format  ", Camera2App.SETTING_SEPARATOR, Camera2App.this.mInitParam.output_format}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{"direction      ", Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mInitParam.direction)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{"input_width    ", Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mInitParam.input_width)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{"input_height   ", Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mInitParam.input_height)}));
            Object[] objArr2 = new Object[3];
            objArr2[0] = "aovx           ";
            objArr2[1] = Camera2App.SETTING_SEPARATOR;
            objArr2[2] = String.valueOf(Camera2App.this.mInitParam.aovx);
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", objArr2));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{"aovy           ", Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mInitParam.aovy)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{"output_rotation", Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mInitParam.output_rotation)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{"goal_angle     ", Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mInitParam.goal_angle)}));
            logStringBuilder.append("\r\n");
            logStringBuilder.append("***** Settings *****\r\n");
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{"Model", Camera2App.SETTING_SEPARATOR, BuildUtil.getModel()}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_OIS), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.use_ois)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_GPS), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.use_gps)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_AUTO_EXPOSURE_LOCK), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.auto_ae_lock)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_AUTO_WHITE_BALANCE_LOCK), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.auto_wb_lock)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_ANTI_BANDING), Camera2App.SETTING_SEPARATOR, Camera2App.this.mActivity.getResources().getStringArray(R.array.panoramagp3_ANTI_BANDING)[Camera2App.this.mSettings.anti_banding]}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_CAPTURE_MODE), Camera2App.SETTING_SEPARATOR, Camera2App.this.mActivity.getResources().getStringArray(R.array.panoramagp3_CAPTURE_MODE)[Camera2App.this.mSettings.capture_mode]}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_AOVX), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.aov_x)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_AOVY), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.aov_y)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_CALCSEAM_PIXNUM), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.calcseam_pixnum)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_SHRINK_RATIO), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.shrink_ratio)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_DEFORM), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.use_deform)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_LUMINANCE_CORRECTION), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.use_luminance_correction)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_SEAMSEARCH_RATIO), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.seamsearch_ratio)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_ZROTATION_COEFF), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.zrotation_coeff)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_DRAW_THRESHOLD), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.draw_threshold)}));
            String[] array = Camera2App.this.mActivity.getResources().getStringArray(R.array.panoramagp3_SENSOR_MODE_Value);
            for (int array_index = 0; array_index < array.length; array_index++) {
                if (array[array_index].equals(String.valueOf(Camera2App.this.mSettings.sensor_mode))) {
                    objArr2 = new Object[3];
                    objArr2[0] = Camera2App.this.mActivity.getString(2131690317);
                    objArr2[1] = Camera2App.SETTING_SEPARATOR;
                    objArr2[2] = Camera2App.this.mActivity.getResources().getStringArray(R.array.panoramagp3_SENSOR_MODE)[array_index];
                    logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", objArr2));
                    break;
                }
            }
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_SENSOR_USE_MODE), Camera2App.SETTING_SEPARATOR, Camera2App.this.mActivity.getResources().getStringArray(R.array.panoramagp3_SENSOR_USE_MODE)[Camera2App.this.mSettings.sensor_use_mode]}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_USE_GRAVITY_SENSOR), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.use_gravity_sensor)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_UNSHARP_STRENGTH), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mShotSettings.unsharpStrength)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_NR_AUTO), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.nr_auto)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_NR_STRENGTH), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mShotSettings.noiseReductionStrength)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_COLOR_CORRECTION_MODE), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.color_correction_mode)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_EDGE_MODE), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mShotSettings.edgeMode)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_NOISE_REDUCTION_MODE), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mShotSettings.noiseReductionMode)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_SHADING_MODE), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.shading_mode)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_TONEMAP_MODE), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.tonemap_mode)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_AOV_GAIN), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.aov_gain)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_DISTORTION_K1), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.distortion_k1)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_DISTORTION_K2), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.distortion_k2)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_DISTORTION_K3), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.distortion_k3)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_DISTORTION_K4), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.distortion_k4)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_ROTATION_RATIO), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.rotation_ratio)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_UI_CONTROL_MODE), Camera2App.SETTING_SEPARATOR, Camera2App.this.mActivity.getResources().getStringArray(R.array.panoramagp3_UI_CONTROL_MODE)[Camera2App.this.mSettings.ui_control_mode]}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_FOCUS_MODE), Camera2App.SETTING_SEPARATOR, Camera2App.this.mActivity.getResources().getStringArray(R.array.panoramagp3_FOCUS_MODE)[Camera2App.this.mSettings.focus_mode]}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_KEY_USE_CAMERA2), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.use_camera2)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_CAMERA_ID), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.camera_id)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_USE_WDR2), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.use_wdr2)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_MAKE_360), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.make_360)}));
            logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{Camera2App.this.mActivity.getString(R.string.setting_USE_60FPS), Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSettings.use_60fps)}));
            logStringBuilder.append("\r\n");
            logStringBuilder.append("***** Camera info *****\r\n");
            if (Camera2App.this.mSensorSensitivity[0] == Camera2App.this.mSensorSensitivity[1]) {
                logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{"Sensor Sensitivity", Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSensorSensitivity[0])}));
            } else {
                logStringBuilder.append(String.format(Locale.US, "%s%s%s - %s\r\n", new Object[]{"Sensor Sensitivity", Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mSensorSensitivity[0]), String.valueOf(Camera2App.this.mSensorSensitivity[1])}));
            }
            if (Camera2App.this.mExposureTime[0] == Camera2App.this.mExposureTime[1]) {
                logStringBuilder.append(String.format(Locale.US, "%s%s%s\r\n", new Object[]{"Exposure Time     ", Camera2App.SETTING_SEPARATOR, String.valueOf(Camera2App.this.mExposureTime[0])}));
                outWidth2 = outWidth;
                outHeight2 = outHeight;
            } else {
                objArr = new Object[4];
                objArr[0] = "Exposure Time     ";
                objArr[1] = Camera2App.SETTING_SEPARATOR;
                outWidth2 = outWidth;
                outHeight2 = outHeight;
                objArr[2] = String.valueOf(Camera2App.this.mExposureTime[0]);
                objArr[3] = String.valueOf(Camera2App.this.mExposureTime[1]);
                logStringBuilder.append(String.format(Locale.US, "%s%s%s - %s\r\n", objArr));
            }
            if (isoAve != 0) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("mMorphoPanoramaGP3.saveLog error ret:");
                stringBuilder2.append(isoAve);
                LogFilter.e("Camera2App", stringBuilder2.toString());
            }
            if (Camera2App.this.mSettings.use_wdr2) {
                boolean z2;
                if (Camera2App.this.mMorphoPanoramaGP3.finish(false) != 0) {
                    z2 = true;
                    LogFilter.e("Camera2App", String.format(Locale.US, "MorphoPanoramaGP3.finish error ret:0x%08X", new Object[]{Integer.valueOf(Camera2App.this.mMorphoPanoramaGP3.finish(false))}));
                } else {
                    z2 = true;
                }
                isoAve = Camera2App.this.mMorphoPanoramaGP3.savePanorama360Delay(filepath, z2, endStatus, isLGE);
                if (isoAve != 0) {
                    objArr = new Object[z2];
                    objArr[0] = Integer.valueOf(isoAve);
                    LogFilter.e("Camera2App", String.format(Locale.US, "MorphoPanoramaGP3.savePanorama360Delay error ret:0x%08X", objArr));
                }
                Camera2App.this.finishEngine2();
            } else {
                Camera2App.this.finishEngine();
            }
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("location 2 = ");
            Location location4 = location3;
            stringBuilder3.append(location4);
            LogFilter.e("Camera2App", stringBuilder3.toString());
            Camera2App.setInExif(filepath, location4, -1, -1, false);
            Camera2App.this.insertMediaStore(outputFileName, filepath, location4, -1, Camera2App.this, galleryInfoData);
            CameraApp app = (CameraApp) Camera2App.this.mActivity.getApplication();
            if (filepath == null) {
                LogFilter.e("Camera2App", "FileWriteErr");
                app.clearLastShotFile();
                return false;
            }
            Camera2App.scanFile(Camera2App.this.mActivity.getBaseContext(), new File(filepath));
            if (addMargin) {
                app.setLastShotFile(filepath, galleryInfoData.whole_width, galleryInfoData.whole_height);
                outHeight = outWidth2;
                int i = outHeight2;
            } else {
                app.setLastShotFile(filepath, outWidth2, outHeight2);
            }
            return true;
        }

        public void onSaveFinish(boolean result) {
            if (Camera2App.this.mSettings.save_input_images != 0) {
                Camera2App.this.saveSettings(Camera2App.this.mInputFolderPath, this.imageFormat);
            }
            if (!(Camera2App.this.mSettings.save_input_images == 0 || Camera2App.this.getCamera2ParamsFragment().auto())) {
                Camera2App.this.mInputSaveState.putParamFile(Camera2App.this.mInputFolderPath, Camera2App.this.mMorphoCamera.cameraInfo(), this.imageFormat, Camera2App.this.getCamera2ParamsFragment().sensorSensitivity(), Camera2App.this.getCamera2ParamsFragment().shutterSpeed(), Camera2App.this.getCamera2ParamsFragment().fps());
            }
            final boolean updateThumbnail = result;
            Camera2App.this.mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    if (Camera2App.this.getActionButtonsFragment() != null) {
                        CameraState cameraState;
                        Camera2App.this.updatePeekThumb();
                        if (updateThumbnail) {
                            Camera2App.this.updateViewRotation();
                        }
                        if (Camera2App.this.mUseCamera1) {
                            if (CameraConstants.AutoFocusType == 0) {
                                cameraState = new Camera1PreviewState(Camera2App.this.mMorphoCamera.cameraState());
                            } else {
                                cameraState = new Camera1UnlockFocusState(Camera2App.this.mMorphoCamera.cameraState());
                            }
                        } else if (CameraConstants.AutoFocusType == 0) {
                            cameraState = new PreviewState();
                        } else {
                            cameraState = new UnlockFocusState();
                        }
                        Camera2App.this.mMorphoCamera.updateCameraState(cameraState);
                        cameraState.onStart();
                    }
                }
            });
        }
    }

    private class SaveSensorState extends CameraState implements ISaveThreadEventListener {
        private SaveSensorInfo mSaveSensor;

        public SaveSensorState() {
            this.mSaveSensor = new SaveSensorInfo(Camera2App.this.mSensorInfoManagerList);
        }

        public boolean onFinish() {
            return true;
        }

        public boolean requestSaveProcess() {
            if (Camera2App.this.mSettings.save_input_images == 2 || Camera2App.this.mSettings.save_input_images == 1) {
                Camera2App.this.finalizeEncoder();
            }
            CameraInfo cameraInfo = Camera2App.this.mMorphoCamera.cameraInfo();
            cameraInfo.setCaptureWidth(2400);
            cameraInfo.setCaptureHeight(1800);
            this.mSaveSensor.save(Camera2App.this.mActivity.getBaseContext(), Camera2App.this.mInputFolderPath, new DeviceInfo(Camera2App.this.mInitParam.aovx, Camera2App.this.mInitParam.aovy, (double) cameraInfo.getPhysicalWidth(), (double) cameraInfo.getPhysicalHeight()));
            Camera2App.this.copyBuildProp();
            return true;
        }

        public void onSaveFinish(boolean result) {
            Camera2App.this.mSensorInfoManagerList.clear();
            LogFilter.i("Camera2App", "Save sensor end.");
            Camera2App.this.mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    CameraState cameraState;
                    if (Camera2App.this.mUseCamera1) {
                        if (CameraConstants.AutoFocusType == 0) {
                            cameraState = new Camera1PreviewState(Camera2App.this.mMorphoCamera.cameraState());
                        } else {
                            cameraState = new Camera1UnlockFocusState(Camera2App.this.mMorphoCamera.cameraState());
                        }
                    } else if (CameraConstants.AutoFocusType == 0) {
                        cameraState = new PreviewState();
                    } else {
                        cameraState = new UnlockFocusState();
                    }
                    Camera2App.this.mMorphoCamera.updateCameraState(cameraState);
                    cameraState.onStart();
                }
            });
        }
    }

    static {
        DEFAULT_UI_CONTROL_MODES.put("E6653,21", Integer.valueOf(1));
        DEFAULT_UI_CONTROL_MODES.put("E6653,22", Integer.valueOf(1));
        DEFAULT_UI_CONTROL_MODES.put("E6653,23", Integer.valueOf(1));
        DEFAULT_UI_CONTROL_MODES.put("E6683,21", Integer.valueOf(1));
        DEFAULT_UI_CONTROL_MODES.put("E6683,22", Integer.valueOf(1));
        DEFAULT_UI_CONTROL_MODES.put("E6683,23", Integer.valueOf(1));
        DEFAULT_UI_CONTROL_MODES.put("LG-H961N,22", Integer.valueOf(2));
    }

    public Camera2App(CameraActivity mActivity, PanoCaptureModule control) {
        this.mActivity = mActivity;
        this.mController = control;
        this.mDefaultPreviewSize = new Size(DEFAULT_PREVIEW_WIDTH, 720);
        this.mHandler = new Handler(Looper.getMainLooper());
        this.mSoundPlayer = new SoundPlayer(mActivity);
        this.mAudioManager = (AudioManager) mActivity.getSystemService("audio");
    }

    private void initAttachQueue() {
        while (this.mAttachImageQueue.size() > 0) {
            CaptureImage imgTmp = (CaptureImage) this.mAttachImageQueue.poll();
            if (imgTmp != null) {
                imgTmp.close();
            }
        }
    }

    private void addAttachQueue(CaptureImage image) {
        this.mAttachImageQueue.offer(image);
        int QUEUEING_NUM = this.mSettings.save_input_images == 2 ? 16 : 1;
        while (QUEUEING_NUM < this.mAttachImageQueue.size()) {
            CaptureImage imgTmp = (CaptureImage) this.mAttachImageQueue.poll();
            if (imgTmp != null) {
                imgTmp.close();
            }
        }
    }

    private void setAttachExit() {
        addAttachQueue(sAttachExit);
    }

    private Camera2ParamsFragment getCamera2ParamsFragment() {
        if (this.mCamera2ParamsFragment == null) {
            this.mCamera2ParamsFragment = (Camera2ParamsFragment) this.mActivity.getFragmentManager().findFragmentById(R.id.camera2_params);
        }
        return this.mCamera2ParamsFragment;
    }

    private ButtonsFragment getActionButtonsFragment() {
        if (this.mButtonsFragment == null) {
            this.mButtonsFragment = (ButtonsFragment) this.mActivity.getFragmentManager().findFragmentById(R.id.action_buttons);
        }
        return this.mButtonsFragment;
    }

    private void resetSettingForCameraId() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mActivity.getApplicationContext());
        Editor editor = sp.edit();
        boolean is_reset = false;
        boolean is_apply = false;
        if (BuildUtil.isG5()) {
            if (this.mSettings.camera_id == 2) {
                DEFAULT_SETTING_DISTORTION_K1 = 0.02999318d;
                DEFAULT_SETTING_DISTORTION_K2 = 0.2697905d;
                DEFAULT_SETTING_DISTORTION_K3 = -0.2363396d;
                DEFAULT_SETTING_DISTORTION_K4 = 0.3335534d;
                DEFAULT_SETTING_MOTION_DETECTION_MODE = 1;
            } else {
                DEFAULT_SETTING_DISTORTION_K1 = Camera2ParamsFragment.TARGET_EV;
                DEFAULT_SETTING_DISTORTION_K2 = Camera2ParamsFragment.TARGET_EV;
                DEFAULT_SETTING_DISTORTION_K3 = Camera2ParamsFragment.TARGET_EV;
                DEFAULT_SETTING_DISTORTION_K4 = Camera2ParamsFragment.TARGET_EV;
                DEFAULT_SETTING_MOTION_DETECTION_MODE = 0;
            }
            is_reset = true;
        }
        if (BuildUtil.isV20()) {
            if (this.mSettings.camera_id == 2) {
                DEFAULT_SETTING_DISTORTION_K1 = 0.02999318d;
                DEFAULT_SETTING_DISTORTION_K2 = 0.2697905d;
                DEFAULT_SETTING_DISTORTION_K3 = -0.2363396d;
                DEFAULT_SETTING_DISTORTION_K4 = 0.3335534d;
                DEFAULT_SETTING_MOTION_DETECTION_MODE = 1;
            } else {
                DEFAULT_SETTING_DISTORTION_K1 = Camera2ParamsFragment.TARGET_EV;
                DEFAULT_SETTING_DISTORTION_K2 = Camera2ParamsFragment.TARGET_EV;
                DEFAULT_SETTING_DISTORTION_K3 = Camera2ParamsFragment.TARGET_EV;
                DEFAULT_SETTING_DISTORTION_K4 = Camera2ParamsFragment.TARGET_EV;
                DEFAULT_SETTING_MOTION_DETECTION_MODE = 0;
            }
            is_reset = true;
        }
        if (BuildUtil.isG6() || BuildUtil.isV30()) {
            if (this.mSettings.camera_id == 2) {
                DEFAULT_SETTING_DISTORTION_K1 = 0.119392d;
                DEFAULT_SETTING_DISTORTION_K2 = -0.3646027d;
                DEFAULT_SETTING_DISTORTION_K3 = 0.4591976d;
                DEFAULT_SETTING_DISTORTION_K4 = -0.07026484d;
                DEFAULT_SETTING_MOTION_DETECTION_MODE = 1;
            } else {
                DEFAULT_SETTING_DISTORTION_K1 = Camera2ParamsFragment.TARGET_EV;
                DEFAULT_SETTING_DISTORTION_K2 = Camera2ParamsFragment.TARGET_EV;
                DEFAULT_SETTING_DISTORTION_K3 = Camera2ParamsFragment.TARGET_EV;
                DEFAULT_SETTING_DISTORTION_K4 = Camera2ParamsFragment.TARGET_EV;
                DEFAULT_SETTING_MOTION_DETECTION_MODE = 0;
            }
            is_reset = true;
        }
        if (is_reset) {
            if (sp.contains(this.mActivity.getResources().getString(R.string.KEY_DISTORTION_K1))) {
                editor.remove(this.mActivity.getResources().getString(R.string.KEY_DISTORTION_K1));
                is_apply = true;
            }
            if (sp.contains(this.mActivity.getResources().getString(R.string.KEY_DISTORTION_K2))) {
                editor.remove(this.mActivity.getResources().getString(R.string.KEY_DISTORTION_K2));
                is_apply = true;
            }
            if (sp.contains(this.mActivity.getResources().getString(R.string.KEY_DISTORTION_K3))) {
                editor.remove(this.mActivity.getResources().getString(R.string.KEY_DISTORTION_K3));
                is_apply = true;
            }
            if (sp.contains(this.mActivity.getResources().getString(R.string.KEY_DISTORTION_K4))) {
                editor.remove(this.mActivity.getResources().getString(R.string.KEY_DISTORTION_K4));
                is_apply = true;
            }
            if (sp.contains(this.mActivity.getResources().getString(R.string.KEY_MOTION_DETECTION_MODE))) {
                editor.remove(this.mActivity.getResources().getString(R.string.KEY_MOTION_DETECTION_MODE));
                is_apply = true;
            }
            if (is_apply) {
                editor.apply();
            }
        }
    }

    public void onCreate(View parent) {
        LogFilter.i("Camera2App", String.format(Locale.US, "%s, BuildVersion:%d", new Object[]{BuildUtil.getModel(), Integer.valueOf(BuildUtil.getSdkVersion())}));
        this.mRootView = parent;
        this.mActivity.getCameraAppUI().setSwipeEnabled(false);
        this.mActivity.getWindow().addFlags(1024);
        this.mActivity.getWindow().addFlags(128);
        this.mActivity.getLayoutInflater().inflate(R.layout.camera2, (ViewGroup) this.mRootView, true);
        this.mFolderPath = Storage.DIRECTORY;
        CameraApp app = (CameraApp) this.mActivity.getApplication();
        if (app != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mFolderPath);
            stringBuilder.append(File.separator);
            stringBuilder.append("input");
            app.setInputFilePath(stringBuilder.toString());
        }
        this.mPanoramaFrameView = (FrameLayout) this.mRootView.findViewById(R.id.panorama_frame_view);
        this.mMenuTopView = (RelativeLayout) this.mRootView.findViewById(R.id.menu_top);
        this.mTextureView = (TextureView) this.mRootView.findViewById(R.id.texture_view);
        this.mWarningMessage = (TextView) this.mRootView.findViewById(R.id.warning_messages);
        this.mPreviewFrame = (FrameLayout) this.mRootView.findViewById(R.id.FramePanoramaPreview);
        this.mPreviewImageView = (ImageView) this.mRootView.findViewById(R.id.ImagePanoramaPreview);
        this.mCurPreviewFrame = this.mRootView.findViewById(R.id.panoramagp3_prevew_frame);
        this.mPreviewLine1 = this.mRootView.findViewById(R.id.panoramagp3_prevew_line1);
        this.mPreviewLine2 = this.mRootView.findViewById(R.id.panoramagp3_prevew_line2);
        this.mPreviewLine3 = this.mRootView.findViewById(R.id.panoramagp3_prevew_line3);
        this.back_tophoto = (ImageView) this.mRootView.findViewById(R.id.back_tophoto);
        this.pano_view = (FrameLayout) this.mRootView.findViewById(R.id.root_view);
        this.lpp = (LayoutParams) this.mWarningMessage.getLayoutParams();
        this.mPreviewArrow = (ImageView) this.mRootView.findViewById(R.id.panoramagp3_prevew_arrow);
        this.mWarningTextView = (TextView) this.mRootView.findViewById(R.id.warning);
        this.mSurfaceView = (SurfaceView) this.mRootView.findViewById(R.id.camera1_surface_view);
        this.gridLines = (GridLines) this.mRootView.findViewById(R.id.grid_lines_2);
        this.mLocationManager = new PanoramaGP3LocationManager((LocationManager) this.mActivity.getSystemService("location"));
        getCamera2ParamsFragment().setEventHandler(new ICamera2ParamsFragmentEvent() {
            public void onParamChanged() {
                if (CameraConstants.CameraSynchronizedObject != null) {
                    synchronized (CameraConstants.CameraSynchronizedObject) {
                        Camera2App.this.mMorphoCamera.cameraState().onRequestParamChange();
                    }
                }
            }
        });
        this.baseImage = (ImageButton) this.mRootView.findViewById(R.id.take_picture_button);
        this.baseImage.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                Camera2App.this.StoreAceessDialog(Camera2App.this.mActivity);
                if (Camera2App.this.mStorageLocationCheck) {
                    Camera2App.this.mStorageLocationCheck = false;
                    return;
                }
                Camera2App.this.mShaked = false;
                ToastUtil.showToast(Camera2App.this.mActivity, Camera2App.this.mActivity.getString(R.string.move_device_in_the_shooting_direction_toast), 0);
                Camera2App.this.playPanoSound();
                if (Camera2App.this.mCamera2ParamsFragmentSelectedMode >= 0) {
                    Camera2App.this.mHandler.removeCallbacksAndMessages(null);
                    Camera2App.this.revertCamera2ParamsFragmentMode();
                } else {
                    Camera2App.this.mRootView.findViewById(R.id.mini_preview_guide).setVisibility(0);
                    Camera2App.this.mMiniPreviewImageView.setVisibility(4);
                }
                if (!Camera2App.this.mMorphoCamera.cameraState().onFinish()) {
                    Camera2App.this.mMorphoCamera.cameraState().onTakePictureStart();
                }
            }
        });
        this.back_tophoto.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                Camera2App.this.backToPhoto();
            }
        });
        this.mThumbnailButton = (ImageView) this.mRootView.findViewById(R.id.panoramagp3_thumbnail);
        this.mThumbnailButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                Camera2App.this.onClickThumbnail(view);
            }
        });
        this.mInputSaveState.setContext(this.mActivity);
        this.mSensorManager = (SensorManager) this.mActivity.getSystemService("sensor");
        for (Sensor sensor : this.mSensorManager.getSensorList(-1)) {
            if (sensor.getType() == 4) {
                this.mGyroscope = this.mSensorManager.getDefaultSensor(4);
            }
            if (sensor.getType() == 16) {
                this.mGyroscopeUncalibrated = this.mSensorManager.getDefaultSensor(16);
            }
            if (sensor.getType() == 1) {
                this.mAccelerometer = this.mSensorManager.getDefaultSensor(1);
            }
            if (sensor.getType() == 2) {
                this.mMagnetic = sensor;
            }
            if (sensor.getType() == 15) {
                this.mRotationVector = this.mSensorManager.getDefaultSensor(15);
            }
        }
        this.mRoundDetector = new RoundDetector();
        this.mSensorFusion = new SensorFusion(true);
        this.mSensorFusionMode = 4;
        if (this.mSensorFusion.setMode(this.mSensorFusionMode) != 0) {
            LogFilter.e("Camera2App", String.format(Locale.US, "SensorFusion.setMode error ret:0x%08X", new Object[]{Integer.valueOf(this.mSensorFusion.setMode(this.mSensorFusionMode))}));
        }
        if (this.mSensorFusion.setOffsetMode(0) != 0) {
            LogFilter.e("Camera2App", String.format(Locale.US, "SensorFusion.setOffsetMode error ret:0x%08X", new Object[]{Integer.valueOf(this.mSensorFusion.setOffsetMode(0))}));
        }
        if (this.mSensorFusion.setAppState(1) != 0) {
            LogFilter.e("Camera2App", String.format(Locale.US, "SensorFusion.setAppState error ret:0x%08X", new Object[]{Integer.valueOf(this.mSensorFusion.setAppState(1))}));
        }
        this.mSensorInfoManagerList = new ArrayList();
        AudioAttributes attr = new Builder().setUsage(1).setContentType(2).build();
        for (int i = 0; i < this.mCamera2ImageQualitySettings.length; i++) {
            this.mCamera2ImageQualitySettings[i] = new Camera2ImageQualitySettings();
        }
        ToastUtil.showToast(this.mActivity, this.mActivity.getString(R.string.enter_panorama_toast), 0);
    }

    private void playPanoSound() {
        int mode = this.mAudioManager.getRingerMode();
        if (!Keys.isShutterSoundOn(this.mActivity.getSettingsManager()) || mode != 2 || this.mSoundPlayer == null) {
            return;
        }
        if (CustomUtil.getInstance().isSkuid()) {
            this.mSoundPlayer.play(R.raw.shutter_sound_2, 1.0f);
        } else {
            this.mSoundPlayer.play(R.raw.shutter, 1.0f);
        }
    }

    private void backToPhoto() {
        this.pano_view.setVisibility(4);
        int moduleIndex = this.mActivity.getSettingsManager().getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_SQUARE_RETURN_TO_INDEX).intValue();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[Camera2App] backToPhoto1 moduleIndex = ");
        stringBuilder.append(moduleIndex);
        LogFilter.d("Camera2App", stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("[Camera2App] backToPhoto2 moduleIndex = ");
        stringBuilder.append(moduleIndex);
        LogFilter.d("Camera2App", stringBuilder.toString());
        this.mActivity.getCameraAppUI().onModeIdChanged(moduleIndex);
        cleanViewsAndFragments();
    }

    public void cleanViewsAndFragments() {
        if (this.mRootView != null) {
            View rootView = this.mRootView.findViewById(R.id.root_view);
            if (rootView != null) {
                ((ViewGroup) this.mRootView).removeView(rootView);
            }
        }
        FragmentManager fm = this.mActivity.getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment fragment = fm.findFragmentById(R.id.camera_info_view);
        if (fragment != null) {
            ft.remove(fragment);
        }
        fragment = fm.findFragmentById(R.id.camera2_params);
        if (fragment != null) {
            ft.remove(fragment);
        }
        fragment = fm.findFragmentById(R.id.action_buttons);
        if (fragment != null) {
            ft.remove(fragment);
        }
        ft.commit();
    }

    private void StoreAceessDialog(CameraActivity mActivity) {
        if (Storage.getSavePath().equals("1") && !SDCard.instance().isWriteable()) {
            mActivity.getCameraAppUI().setViewFinderLayoutVisibile(true);
            if (this.mMountedDialog != null && this.mMountedDialog.isShowing()) {
                this.mMountedDialog.dismiss();
                this.mMountedDialog = null;
            }
            this.mMountedDialog = CameraUtil.UnAccessDialog(mActivity, mActivity.getResources().getString(R.string.sd_access_error), mActivity.getResources().getString(R.string.sd_access_photo_error_message), mActivity.getResources().getString(R.string.alert_storage_dialog_ok));
            if (Storage.getSavePath().equals("1")) {
                Storage.setSavePath("0");
                mActivity.getSettingsManager().set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_SAVEPATH, "0");
                this.mFolderPath = Storage.DIRECTORY;
                this.mStorageLocationCheck = true;
            }
        }
    }

    public void onDestroy() {
        LogFilter.d("Camera2App", "onDestroy");
        this.mMorphoCamera.cameraState().onCancel();
        releaseImageBitmap();
        this.mMorphoCamera.exit();
        if (this.mSensorFusion != null) {
            this.mSensorFusion.release();
        }
        this.mSoundPlayer.release();
        ToastUtil.cancelToast();
        this.mExecutor.shutdown();
        try {
            this.mExecutor.awaitTermination(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            this.mExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static final int checkPermissions(Activity activity, String... permissions) {
        try {
            List<String> reqPermissions = new ArrayList();
            for (String permission : permissions) {
                if (((Integer) Activity.class.getMethod("checkSelfPermission", new Class[]{String.class}).invoke(activity, new Object[]{permission})).intValue() != 0) {
                    reqPermissions.add(permissions[r3]);
                }
            }
            if (reqPermissions.size() > 0) {
                Activity.class.getMethod("requestPermissions", new Class[]{String[].class, Integer.TYPE}).invoke(activity, new Object[]{reqPermissions.toArray(new String[0]), Integer.valueOf(0)});
                return -1;
            }
        } catch (Exception e) {
        }
        return 0;
    }

    public final void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult != 0) {
                this.mActivity.finish();
                return;
            }
        }
        this.mHandler.postDelayed(new Runnable() {
            public final void run() {
                Camera2App.this.mActivity.recreate();
            }
        }, 200);
    }

    public void onStart() {
        LogFilter.d("Camera2App", "onStart");
    }

    public void onResume() {
        LogFilter.d("Camera2App", "onResume");
        this.mSoundPlayer.loadSound(R.raw.shutter);
        this.mSoundPlayer.loadSound(R.raw.shutter_sound_2);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mActivity.getApplicationContext());
        this.mUseCamera1 = sp.getBoolean(this.mActivity.getString(R.string.KEY_USE_CAMERA2), true) ^ 1;
        Editor editor = sp.edit();
        String key = this.mActivity.getResources().getString(R.string.KEY_CAMERA_ID);
        if (!sp.contains(key)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(key);
            stringBuilder.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder.toString());
            editor.putString(key, String.valueOf(DEFAULT_SETTING_CAMERA_ID));
            editor.apply();
            this.mCameraIdTmp = -2;
        }
        this.mSettings.camera_id = Integer.parseInt(sp.getString(key, String.valueOf(DEFAULT_SETTING_CAMERA_ID)));
        int def_capture_mode = this.mUseCamera1 ? 1 : 4;
        key = this.mActivity.getResources().getString(R.string.KEY_CAPTURE_MODE);
        if (!sp.contains(key)) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(key);
            stringBuilder2.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder2.toString());
            editor.putString(key, String.valueOf(def_capture_mode));
            editor.apply();
        }
        this.mSettings.capture_mode = Integer.parseInt(sp.getString(key, String.valueOf(def_capture_mode)));
        if (!(this.mMorphoCamera == null || (this.mUseCamera1Tmp == this.mUseCamera1 && this.mCameraIdTmp == this.mSettings.camera_id))) {
            this.mAovs = null;
            editor.remove(this.mActivity.getResources().getString(R.string.KEY_AOVX));
            editor.remove(this.mActivity.getResources().getString(R.string.KEY_AOVY));
            editor.remove(this.mActivity.getResources().getString(R.string.KEY_CAPTURE_SIZE));
            editor.apply();
            getCamera2ParamsFragment().resetValues(this.mActivity.getSharedPreferences(CAMERA2_PARAM_FILE, 0));
        }
        if (!(this.mMorphoCamera != null && this.mUseCamera1Tmp == this.mUseCamera1 && this.mCameraIdTmp == this.mSettings.camera_id && this.mCaptureModeTmp == this.mSettings.getCaptureMode())) {
            if (this.mUseCamera1) {
                this.mMorphoCamera = new MorphoCamera1(this, this.mSettings.camera_id);
            } else {
                this.mMorphoCamera = new MorphoCamera(this, this.mActivity, this.mSettings.camera_id, this.mSettings.getCaptureMode());
            }
            this.mIsFrontCamera = this.mMorphoCamera.isFrontCamera(this.mSettings.camera_id);
        }
        if (!(this.mCameraIdTmp == this.mSettings.camera_id || this.mCameraIdTmp == -1)) {
            resetSettingForCameraId();
        }
        setNullDirectionFunction();
        this.mMorphoCamera.setMorphoPanoramaGP3Interface(this);
        if (this.mUseCamera1) {
            this.mTextureView.setVisibility(8);
            this.mSurfaceView.setVisibility(0);
            SurfaceHolder holder = this.mSurfaceView.getHolder();
            holder.removeCallback(this.mSurfaceListener);
            holder.addCallback(this.mSurfaceListener);
            this.mRootView.findViewById(R.id.camera_info_view).setVisibility(8);
            this.mRootView.findViewById(R.id.camera_param_frame).setVisibility(4);
            this.mRootView.findViewById(R.id.tv_view).setVisibility(4);
            this.mRootView.findViewById(R.id.anti_flicker_mode_layout).setVisibility(4);
        } else {
            this.mTextureView.setVisibility(0);
            this.mSurfaceView.setVisibility(8);
            this.mRootView.findViewById(R.id.camera_info_view).setVisibility(0);
            if (this.mSettings.anti_flicker_mode == 0) {
                ((Button) this.mRootView.findViewById(R.id.anti_flicker_mode_button)).setText(this.mActivity.getResources().getString(R.string.panoramagp3_ITEM_ON));
            } else {
                ((Button) this.mRootView.findViewById(R.id.anti_flicker_mode_button)).setText(this.mActivity.getResources().getString(R.string.panoramagp3_ITEM_OFF));
            }
            if (getCamera2ParamsFragment().tvAll()) {
                this.mRootView.findViewById(R.id.tv_view).setVisibility(0);
                if (!getCamera2ParamsFragment().tvSimple()) {
                    this.mRootView.findViewById(R.id.anti_flicker_mode_layout).setVisibility(4);
                }
            } else {
                this.mRootView.findViewById(R.id.tv_view).setVisibility(4);
                this.mRootView.findViewById(R.id.anti_flicker_mode_layout).setVisibility(4);
            }
        }
        getCamera2ParamsFragment().loadValues(this.mActivity.getSharedPreferences(CAMERA2_PARAM_FILE, 0));
        if (this.mOrientationEventListener == null) {
            this.mOrientationEventListener = new MyOrientationEventListener(this.mActivity.getApplicationContext());
        }
        this.mOrientationEventListener.enable();
        updatedOrientation(getDisplayRotation());
        setSupportedCaptureSize();
        getSettingValue();
        setPreviewSize();
        if (this.mActivity.getSettingsManager().getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_RECORD_LOCATION)) {
            this.mLocationManager.requestLocation();
        } else {
            this.mLocationManager.clearLocation();
        }
        if (!this.mUseCamera1) {
            ((MorphoCamera) this.mMorphoCamera).setCamera2Params(getCamera2ParamsFragment());
        }
        CameraApp app = (CameraApp) this.mActivity.getApplication();
        this.mMorphoCamera.resume(app.getSupportedPictureSizes()[this.mSettings.capture_size_index], app.getPreviewSize());
        CameraInfo cameraInfo = this.mMorphoCamera.cameraInfo();
        cameraInfo.setCaptureWidth(2400);
        cameraInfo.setCaptureHeight(1800);
        updateViewRotation();
        if (this.mUseCamera1) {
            layoutMiniPreview();
        } else if (this.mTextureView.isAvailable()) {
            openCamera();
            makeEngineParam();
        } else {
            this.mTextureView.setSurfaceTextureListener(this.mSurfaceTextureListener);
        }
        this.mRootView.findViewById(R.id.arrow_left).setVisibility(0);
        this.mRootView.findViewById(R.id.arrow_right).setVisibility(0);
        this.mRootView.findViewById(R.id.arrow_up).setVisibility(0);
        this.mRootView.findViewById(R.id.arrow_down).setVisibility(0);
        registGravitySensorListener();
        if (this.mSettings.sensor_mode != 0) {
            this.mSensorThread = new HandlerThread("SensorThread");
            this.mSensorThread.start();
            Handler handler = new Handler(this.mSensorThread.getLooper());
            switch (this.mSettings.sensor_mode) {
                case 0:
                    this.mSensorFusion.setMode(-1);
                    break;
                case 1:
                    this.mSensorFusion.setMode(1);
                    this.mSensorFusion.setCalibrated(true);
                    break;
                case 2:
                    this.mSensorFusion.setMode(1);
                    this.mSensorFusion.setCalibrated(false);
                    break;
                case 3:
                    this.mSensorFusion.setMode(4);
                    break;
            }
            if (this.mGyroscope != null) {
                this.mSensorManager.registerListener(this.mSensorFusion, this.mGyroscope, 0, handler);
            }
            if (this.mGyroscopeUncalibrated != null) {
                this.mSensorManager.registerListener(this.mSensorFusion, this.mGyroscopeUncalibrated, 0, handler);
            }
            if (this.mAccelerometer != null && this.mSettings.sensor_mode == 4) {
                this.mSensorManager.registerListener(this.mSensorFusion, this.mAccelerometer, 0, handler);
            }
            if (this.mRotationVector != null) {
                this.mSensorManager.registerListener(this.mSensorFusion, this.mRotationVector, 0, handler);
            }
            if (!(this.mAccelerometer == null || this.mSettings.save_input_images == 0)) {
                this.mSensorManager.registerListener(this.mSensorFusion, this.mAccelerometer, 0, handler);
            }
        }
        if (this.mSettings.use_round_auto_end == 2) {
            this.mRoundDetector = new RoundDetector();
            if (this.mAccelerometer != null) {
                this.mSensorManager.registerListener(this.mRoundDetector, this.mAccelerometer, 0);
            }
            if (this.mMagnetic != null) {
                this.mSensorManager.registerListener(this.mRoundDetector, this.mMagnetic, 0);
            }
        } else if (this.mSettings.use_round_auto_end == 1) {
            this.mRoundDetector = new GyroscopeRoundDetector();
            if (this.mSettings.sensor_mode == 2) {
                if (this.mGyroscopeUncalibrated != null) {
                    this.mSensorManager.registerListener(this.mRoundDetector, this.mGyroscopeUncalibrated, 0);
                }
            } else if (this.mGyroscope != null) {
                this.mSensorManager.registerListener(this.mRoundDetector, this.mGyroscope, 0);
            }
        } else {
            this.mRoundDetector = new RoundDetector();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ButtonsFragment.SLIP_UP_ACTION);
        LocalBroadcastManager.getInstance(this.mActivity).registerReceiver(this.localBroadcastReceiver, intentFilter);
        updatePeekThumb();
    }

    public void onPause() {
        LogFilter.d("Camera2App", "onPause");
        this.mSoundPlayer.unloadSound(R.raw.shutter);
        this.mSoundPlayer.unloadSound(R.raw.shutter_sound_2);
        this.mMorphoCamera.cameraState().onStop();
        this.mMorphoCamera.cameraInfo().abortCaptures();
        ToastUtil.cancelToast();
        this.mPreviewFrame.setVisibility(4);
        this.mCurPreviewFrame.setVisibility(4);
        this.mPreviewArrow.setVisibility(4);
        this.mPreviewLine1.setVisibility(4);
        this.mPreviewLine2.setVisibility(4);
        this.mPreviewLine3.setVisibility(4);
        LogFilter.d("Camera2App", "engine cancel.");
        this.mMorphoCamera.cameraState().onCancel();
        this.mMorphoCamera.setDefaultCameraState();
        this.mLocationManager.removeUpdates();
        switch (this.mCamera2ParamsFragmentSelectedMode) {
            case 1:
                this.mHandler.removeCallbacksAndMessages(null);
                getCamera2ParamsFragment().setTv();
                break;
            case 2:
                this.mHandler.removeCallbacksAndMessages(null);
                getCamera2ParamsFragment().setManual();
                break;
            case 3:
                this.mHandler.removeCallbacksAndMessages(null);
                getCamera2ParamsFragment().setTvSimple();
                break;
        }
        this.mCamera2ParamsFragmentSelectedMode = -1;
        getCamera2ParamsFragment().saveValues(this.mActivity.getSharedPreferences(CAMERA2_PARAM_FILE, 0));
        releaseImageBitmap();
        this.mMorphoCamera.pause();
        unregistGravitySensorListener();
        this.mSensorManager.unregisterListener(this.mSensorFusion);
        this.mSensorManager.unregisterListener(this.mRoundDetector);
        if (this.mSensorThread != null) {
            this.mSensorThread.quit();
            this.mSensorThread = null;
        }
        if (this.mOrientationEventListener != null) {
            this.mOrientationEventListener.disable();
            this.mOrientationEventListener = null;
        }
        this.mUseCamera1Tmp = this.mUseCamera1;
        this.mCameraIdTmp = this.mSettings.camera_id;
        this.mCaptureModeTmp = this.mSettings.getCaptureMode();
        LocalBroadcastManager.getInstance(this.mActivity).unregisterReceiver(this.localBroadcastReceiver);
    }

    public void onClickShutter(View view) {
        LogFilter.i("Camera2App", "onClickShutter ");
        this.mHandler.removeCallbacksAndMessages(null);
        revertCamera2ParamsFragmentMode();
        if (!this.mMorphoCamera.cameraState().onFinish()) {
            this.mMorphoCamera.cameraState().onTakePictureStart();
        }
    }

    public void onClickFlickerMode(View view) {
        Button flickerModeButton = (Button) view;
        if (this.mSettings.anti_flicker_mode == 0) {
            this.mSettings.anti_flicker_mode = 1;
            flickerModeButton.setText(this.mActivity.getResources().getString(R.string.panoramagp3_ITEM_OFF));
            return;
        }
        this.mSettings.anti_flicker_mode = 0;
        flickerModeButton.setText(this.mActivity.getResources().getString(R.string.panoramagp3_ITEM_ON));
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode != 4) {
            if (keyCode == 27) {
                onClickShutter(null);
            }
            return false;
        }
        backToPhoto();
        return true;
    }

    private static void initializeEngine(MorphoPanoramaGP3 engine, InitParam init_param) {
        if (engine.createNativeOutputInfo() != 0) {
            LogFilter.e("Camera2App", String.format(Locale.US, "MorphoPanoramaGP3.createNativeOutputInfo error ret:0x%08X", new Object[]{Integer.valueOf(engine.createNativeOutputInfo())}));
        }
        if (engine.initialize(init_param) != 0) {
            LogFilter.e("Camera2App", String.format(Locale.US, "MorphoPanoramaGP3.initialize error ret:0x%08X", new Object[]{Integer.valueOf(engine.initialize(init_param))}));
        }
    }

    private boolean createEngine() {
        if (this.mMorphoPanoramaGP3 != null) {
            return false;
        }
        this.mMorphoPanoramaGP3 = new MorphoPanoramaGP3();
        if ("YUV420_PLANAR".equals(this.mImageFormat)) {
            this.mInitParam.input_format = this.mImageFormat;
            this.mInitParam.output_format = "YUV420_SEMIPLANAR";
        } else {
            this.mInitParam.input_format = this.mImageFormat;
            this.mInitParam.output_format = this.mImageFormat;
        }
        if (this.mSettings.aov_x != Camera2ParamsFragment.TARGET_EV) {
            this.mViewAngleH = (float) this.mSettings.aov_x;
        }
        if (this.mSettings.aov_y != Camera2ParamsFragment.TARGET_EV) {
            this.mViewAngleV = (float) this.mSettings.aov_y;
        }
        CameraInfo cameraInfo = this.mMorphoCamera.cameraInfo();
        cameraInfo.setCaptureWidth(2400);
        cameraInfo.setCaptureHeight(1800);
        float[] ratios = new float[2];
        getRatios(this.mSensorAspectIndex, getAspectTableIndex((float) cameraInfo.getCaptureWidth(), (float) cameraInfo.getCaptureHeight()), ratios);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("createEngine mViewAngleH=");
        stringBuilder.append(this.mViewAngleH);
        stringBuilder.append(" mViewAngleV=");
        stringBuilder.append(this.mViewAngleV);
        stringBuilder.append(" ratiox=");
        stringBuilder.append(ratios[0]);
        stringBuilder.append(" ratioy=");
        stringBuilder.append(ratios[1]);
        LogFilter.i("Camera2App", stringBuilder.toString());
        this.mInitParam.aovx = ((Math.atan(Math.tan(((((double) this.mViewAngleH) * DEFAULT_SETTING_DRAW_THRESHOLD) * 3.141592653589793d) / 180.0d) * ((double) ratios[0])) * 2.0d) * 180.0d) / 3.141592653589793d;
        this.mInitParam.aovy = ((2.0d * Math.atan(Math.tan(((((double) this.mViewAngleV) * DEFAULT_SETTING_DRAW_THRESHOLD) * 3.141592653589793d) / 180.0d) * ((double) ratios[1]))) * 180.0d) / 3.141592653589793d;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("createEngine aovx=");
        stringBuilder2.append(this.mInitParam.aovx);
        stringBuilder2.append(" aovy=");
        stringBuilder2.append(this.mInitParam.aovy);
        LogFilter.i("Camera2App", stringBuilder2.toString());
        this.mInitParam.goal_angle = GOAL_ANGLE;
        initializeEngine(this.mMorphoPanoramaGP3, this.mInitParam);
        this.mInputSaveState.setMorphoPanoramaGP3(this.mMorphoPanoramaGP3);
        this.mMorphoPanoramaGP3.setAttachEnabled(true);
        return true;
    }

    private void showResultMessage(float fps, float ave, float std) {
        if (this.mInputSaveState.isEnabled()) {
            Toast.makeText(this.mActivity.getApplicationContext(), String.format(Locale.US, "save %.02f fps", new Object[]{Float.valueOf(fps)}), 1).show();
            return;
        }
        Toast makeText;
        if (this.isInvalidDir) {
            makeText = Toast.makeText(this.mActivity.getApplicationContext(), "Invalid direction.", 1);
        } else {
            makeText = Toast.makeText(this.mActivity.getApplicationContext(), String.format(Locale.US, "attach %.02f fps (ave:%.02f, std:%.02f)", new Object[]{Float.valueOf(fps), Float.valueOf(ave), Float.valueOf(std)}), 1);
        }
    }

    private void finishEngine() {
        synchronized (CameraConstants.EngineSynchronizedObject) {
            if (isEngineRunning()) {
                this.mInputSaveState.setMorphoPanoramaGP3(null);
                this.mMorphoPanoramaGP3.deleteNativeOutputInfo();
                if (this.mMorphoPanoramaGP3.finish(true) != 0) {
                    LogFilter.e("Camera2App", String.format(Locale.US, "MorphoPanoramaGP3.finish error ret:0x%08X", new Object[]{Integer.valueOf(this.mMorphoPanoramaGP3.finish(true))}));
                }
                final float fps = this.mInputSaveState.isEnabled() ? this.mInputSaveState.getFps() : this.mMorphoPanoramaGP3.getAttachFps();
                float std = 0.0f;
                final float ave = this.mInputSaveState.isEnabled() ? 0.0f : this.mMorphoPanoramaGP3.getAttachAve();
                if (!this.mInputSaveState.isEnabled()) {
                    std = this.mMorphoPanoramaGP3.getAttachStandardDeviation();
                }
                this.mMorphoPanoramaGP3 = null;
                this.mActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        Camera2App.this.showResultMessage(fps, ave, std);
                    }
                });
                return;
            }
        }
    }

    private void finishEngine2() {
        synchronized (CameraConstants.EngineSynchronizedObject) {
            this.mInputSaveState.setMorphoPanoramaGP3(null);
            this.mMorphoPanoramaGP3.deleteNativeOutputInfo();
            this.mMorphoPanoramaGP3.deleteObject();
            final float fps = this.mInputSaveState.isEnabled() ? this.mInputSaveState.getFps() : this.mMorphoPanoramaGP3.getAttachFps();
            float std = 0.0f;
            final float ave = this.mInputSaveState.isEnabled() ? 0.0f : this.mMorphoPanoramaGP3.getAttachAve();
            if (!this.mInputSaveState.isEnabled()) {
                std = this.mMorphoPanoramaGP3.getAttachStandardDeviation();
            }
            this.mMorphoPanoramaGP3 = null;
            this.mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    Camera2App.this.showResultMessage(fps, ave, std);
                }
            });
        }
    }

    private void makeEngineParam() {
        CameraInfo cameraInfo = this.mMorphoCamera.cameraInfo();
        cameraInfo.setCaptureWidth(2400);
        cameraInfo.setCaptureHeight(1800);
        this.mInitParam.direction = 0;
        this.mInitParam.input_width = cameraInfo.getCaptureWidth();
        this.mInitParam.input_height = cameraInfo.getCaptureHeight();
        if (this.mUseCamera1) {
            Size maxSize = ((MorphoCamera1) this.mMorphoCamera).getMaxPictureSize();
            this.mSensorAspectIndex = getAspectTableIndex((float) maxSize.getWidth(), (float) maxSize.getHeight());
        } else if (cameraInfo.getPhysicalWidth() == cameraInfo.getPhysicalHeight()) {
            SizeF physicalSize = new SizeF((cameraInfo.getPhysicalWidth() * ((float) cameraInfo.getPixelArrayWidth())) / 1000.0f, (cameraInfo.getPhysicalHeight() * ((float) cameraInfo.getPixelArrayHeight())) / 1000.0f);
            this.mViewAngleH = (float) (Math.toDegrees(Math.atan((double) (physicalSize.getWidth() / (cameraInfo.getFocalLength() * 2.0f)))) * 2.0d);
            this.mViewAngleV = (float) (2.0d * Math.toDegrees(Math.atan((double) (physicalSize.getHeight() / (2.0f * cameraInfo.getFocalLength())))));
            this.mSensorAspectIndex = getAspectTableIndex(physicalSize.getWidth(), physicalSize.getHeight());
        } else {
            this.mSensorAspectIndex = getBaseAspect();
        }
        if (this.mAovs == null) {
            this.mAovs = new double[2];
            this.mAovs[0] = (double) this.mViewAngleH;
            this.mAovs[1] = (double) this.mViewAngleV;
        }
        CameraInfo info = MorphoCamera1.getCameraInfo(this.mSettings.camera_id);
        if (this.mIsFrontCamera) {
            this.mCameraOrientation = ((info.orientation - MediaProviderUtils.ROTATION_180) + 360) % 360;
        } else {
            this.mCameraOrientation = info.orientation;
        }
        int degrees = getDisplayRotation();
        this.mInitParam.output_rotation = ((this.mCameraOrientation + (this.mCurOrientation + degrees)) + 360) % 360;
        if (this.mSensorFusion != null) {
            int rotation;
            int i = info.orientation;
            if (i == 90) {
                rotation = 1;
            } else if (i == MediaProviderUtils.ROTATION_180) {
                rotation = 2;
            } else if (i != MediaProviderUtils.ROTATION_270) {
                rotation = 0;
            } else {
                rotation = 3;
            }
            if (this.mSensorFusion.setRotation(rotation) != 0) {
                LogFilter.e("Camera2App", String.format(Locale.US, "SensorFusion.setRotation error ret:0x%08X", new Object[]{Integer.valueOf(this.mSensorFusion.setRotation(rotation))}));
            }
        }
    }

    public void onClickSetting(View view) {
        if (this.mCamera2ParamsFragmentSelectedMode >= 0) {
            this.mHandler.removeCallbacksAndMessages(null);
            revertCamera2ParamsFragmentMode();
        }
        if (!isEngineRunning()) {
            String[] constCaptureModes = this.mActivity.getResources().getStringArray(R.array.panoramagp3_CAPTURE_MODE);
            String[] constCaptureModeValues = this.mActivity.getResources().getStringArray(R.array.panoramagp3_CAPTURE_MODE_Value);
            int modeNum = constCaptureModes.length;
            if (!this.mMorphoCamera.cameraInfo().isEnabledZsl()) {
                modeNum--;
            }
            String[] captureModes = new String[modeNum];
            int[] captureModeValues = new int[modeNum];
            boolean z = false;
            int j = 0;
            for (int i = 0; i < constCaptureModes.length; i++) {
                boolean isEnabled = true;
                int mode = Integer.parseInt(constCaptureModeValues[i]);
                if (mode == 2 && !this.mMorphoCamera.cameraInfo().isEnabledZsl()) {
                    isEnabled = false;
                }
                if (isEnabled) {
                    captureModes[j] = constCaptureModes[i];
                    captureModeValues[j] = mode;
                    j++;
                }
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onClickSetting ");
            stringBuilder.append(this.mAovs[0]);
            stringBuilder.append(" ");
            stringBuilder.append(this.mAovs[1]);
            LogFilter.i("Camera2App", stringBuilder.toString());
            Intent intent = new Intent(this.mActivity, SettingActivity.class);
            intent.putExtra(SettingActivity.INTENT_KEY_AOV, this.mAovs);
            String str = SettingActivity.INTENT_KEY_SENSOR_ASPECT;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("");
            stringBuilder2.append(ASPECT_TABLE[this.mSensorAspectIndex][0]);
            stringBuilder2.append(":");
            stringBuilder2.append(ASPECT_TABLE[this.mSensorAspectIndex][1]);
            intent.putExtra(str, stringBuilder2.toString());
            intent.putExtra(SettingActivity.INTENT_KEY_CAMERA2_IMAGE_QUALITY_SETTINGS, this.mCamera2ImageQualitySettings);
            intent.putExtra(SettingActivity.INTENT_KEY_CAMERA, this.mMorphoCamera.getAllCameras());
            intent.putExtra(SettingActivity.INTENT_KEY_CAPTURE_MODE, captureModes);
            intent.putExtra(SettingActivity.INTENT_KEY_CAPTURE_MODE_VALUE, captureModeValues);
            intent.putExtra(SettingActivity.INTENT_KEY_HARDWARE_LEVEL_VALUE, this.mHardwareLevel);
            intent.putExtra(SettingActivity.INTENT_KEY_TIMESTAMP_SOURCE_VALUE, this.mTimestampSource);
            intent.putExtra(SettingActivity.INTENT_KEY_USE_OIS, useOis());
            str = SettingActivity.INTENT_KEY_OIS_AVAILABLE;
            if (!this.mUseCamera1 && ((MorphoCamera) this.mMorphoCamera).isAvailableOis()) {
                z = true;
            }
            intent.putExtra(str, z);
            this.mActivity.startActivity(intent);
        }
    }

    public void onClickCameraSetting(View view) {
        if (this.mCamera2ParamsFragmentSelectedMode >= 0) {
            this.mHandler.removeCallbacksAndMessages(null);
            revertCamera2ParamsFragmentMode();
        }
        if (!isEngineRunning()) {
            View cameraParamFrame = this.mRootView.findViewById(R.id.camera_param_frame);
            if (cameraParamFrame.getVisibility() == 0) {
                cameraParamFrame.setVisibility(4);
            } else {
                cameraParamFrame.setVisibility(0);
            }
        }
    }

    private void getSettingValue() {
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        StringBuilder stringBuilder3;
        StringBuilder stringBuilder4;
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mActivity.getApplicationContext());
        Editor editor = sp.edit();
        String key = this.mActivity.getResources().getString(R.string.KEY_CAPTURE_SIZE);
        if (!sp.contains(key)) {
            StringBuilder stringBuilder5 = new StringBuilder();
            stringBuilder5.append(key);
            stringBuilder5.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder5.toString());
            editor.putString(key, String.valueOf(getIndexOfDefaultCaptureSize()));
            editor.apply();
        }
        this.mSettings.capture_size_index = Integer.parseInt(sp.getString(key, String.valueOf(0)));
        key = this.mActivity.getResources().getString(R.string.KEY_OIS);
        if (!sp.contains(key)) {
            StringBuilder stringBuilder6 = new StringBuilder();
            stringBuilder6.append(key);
            stringBuilder6.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder6.toString());
            editor.putBoolean(key, DEFAULT_SETTING_USE_OIS);
            editor.apply();
        }
        this.mSettings.use_ois = sp.getBoolean(key, DEFAULT_SETTING_USE_OIS);
        key = this.mActivity.getResources().getString(R.string.KEY_GPS);
        if (!sp.contains(key)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(key);
            stringBuilder.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder.toString());
            editor.putBoolean(key, true);
            editor.apply();
        }
        this.mSettings.use_gps = this.mActivity.getSettingsManager().getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_RECORD_LOCATION);
        key = this.mActivity.getResources().getString(R.string.KEY_SAVE_INPUT_IMAGES);
        if (!sp.contains(key)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(key);
            stringBuilder.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder.toString());
            editor.putString(key, String.valueOf(0));
            editor.apply();
        }
        try {
            this.mSettings.save_input_images = Integer.parseInt(sp.getString(key, String.valueOf(0)));
        } catch (Exception e) {
            editor.putString(key, String.valueOf(0));
            editor.apply();
            this.mSettings.save_input_images = 0;
        }
        key = this.mActivity.getResources().getString(R.string.KEY_AUTO_EXPOSURE_LOCK);
        if (!sp.contains(key)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(key);
            stringBuilder.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder.toString());
            editor.putBoolean(key, true);
            editor.apply();
        }
        this.mSettings.auto_ae_lock = sp.getBoolean(key, true);
        key = this.mActivity.getResources().getString(R.string.KEY_AUTO_WHITE_BALANCE_LOCK);
        if (!sp.contains(key)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(key);
            stringBuilder.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder.toString());
            editor.putBoolean(key, true);
            editor.apply();
        }
        this.mSettings.auto_wb_lock = sp.getBoolean(key, true);
        key = this.mActivity.getResources().getString(R.string.KEY_ANTI_BANDING);
        if (!sp.contains(key)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(key);
            stringBuilder.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder.toString());
            editor.putString(key, String.valueOf(1));
            editor.apply();
        }
        this.mSettings.anti_banding = Integer.parseInt(sp.getString(key, String.valueOf(1)));
        key = this.mActivity.getResources().getString(R.string.KEY_ANTI_FLICKER_FREQ);
        if (!sp.contains(key)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(key);
            stringBuilder.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder.toString());
            editor.putString(key, String.valueOf(0));
            editor.apply();
        }
        this.mSettings.anti_flicker_freq = Integer.parseInt(sp.getString(key, String.valueOf(0)));
        key = this.mActivity.getResources().getString(R.string.KEY_RENDERING_AREA);
        if (!sp.contains(key)) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(key);
            stringBuilder2.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder2.toString());
            editor.putInt(key, 33);
            editor.apply();
        }
        this.mSettings.rendering_area = sp.getInt(key, 33);
        key = this.mActivity.getResources().getString(R.string.KEY_AOVX);
        if (!sp.contains(key)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(key);
            stringBuilder.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder.toString());
        }
        this.mSettings.aov_x = Double.valueOf(sp.getString(key, "0")).doubleValue();
        key = this.mActivity.getResources().getString(R.string.KEY_AOVY);
        if (!sp.contains(key)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(key);
            stringBuilder.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder.toString());
        }
        this.mSettings.aov_y = Double.valueOf(sp.getString(key, "0")).doubleValue();
        key = this.mActivity.getResources().getString(R.string.KEY_CALCSEAM_PIXNUM);
        if (!sp.contains(key)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(key);
            stringBuilder.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder.toString());
            editor.putString(key, String.valueOf(0));
            editor.apply();
        }
        this.mSettings.calcseam_pixnum = Integer.parseInt(sp.getString(key, String.valueOf(0)));
        key = this.mActivity.getResources().getString(R.string.KEY_SHRINK_RATIO);
        if (!sp.contains(key)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(key);
            stringBuilder.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder.toString());
            Size[] sizes = ((CameraApp) this.mActivity.getApplication()).getSupportedPictureSizes();
            int width = sizes[this.mSettings.capture_size_index].getWidth();
            int height = sizes[this.mSettings.capture_size_index].getHeight();
            double ratio = ShrinkRatioPreference.ShrinkRatioCalculation(width, height);
            StringBuilder stringBuilder7 = new StringBuilder();
            stringBuilder7.append("0 mSettings.shrink_ratio = ");
            stringBuilder7.append(ratio);
            stringBuilder7.append(" width ");
            stringBuilder7.append(width);
            stringBuilder7.append(" height ");
            stringBuilder7.append(height);
            LogFilter.i("Camera2App", stringBuilder7.toString());
            editor.putString(key, String.format("%.5f", new Object[]{Double.valueOf(ratio)}));
            editor.apply();
        }
        this.mSettings.shrink_ratio = 5.0d;
        stringBuilder = new StringBuilder();
        stringBuilder.append("1 mSettings.shrink_ratio = ");
        stringBuilder.append(this.mSettings.shrink_ratio);
        LogFilter.i("Camera2App", stringBuilder.toString());
        key = this.mActivity.getResources().getString(R.string.KEY_DEFORM);
        if (!sp.contains(key)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(key);
            stringBuilder.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder.toString());
            editor.putBoolean(key, false);
            editor.apply();
        }
        this.mSettings.use_deform = sp.getBoolean(key, false);
        key = this.mActivity.getResources().getString(R.string.KEY_LUMINANCE_CORRECTION);
        if (!sp.contains(key)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(key);
            stringBuilder.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder.toString());
            editor.putBoolean(key, false);
            editor.apply();
        }
        this.mSettings.use_luminance_correction = sp.getBoolean(key, false);
        key = this.mActivity.getResources().getString(R.string.KEY_SEAMSEARCH_RATIO);
        if (!sp.contains(key)) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(key);
            stringBuilder3.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder3.toString());
            editor.putString(key, String.valueOf(1.0d));
            editor.apply();
        }
        this.mSettings.seamsearch_ratio = Double.valueOf(sp.getString(key, String.valueOf(1.0d))).doubleValue();
        key = this.mActivity.getResources().getString(R.string.KEY_ZROTATION_COEFF);
        if (!sp.contains(key)) {
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append(key);
            stringBuilder4.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder4.toString());
            editor.putString(key, String.valueOf(DEFAULT_SETTING_ZROTATION_COEFF));
            editor.apply();
        }
        this.mSettings.zrotation_coeff = Double.valueOf(sp.getString(key, String.valueOf(DEFAULT_SETTING_ZROTATION_COEFF))).doubleValue();
        key = this.mActivity.getResources().getString(R.string.KEY_DRAW_THRESHOLD);
        if (!sp.contains(key)) {
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append(key);
            stringBuilder4.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder4.toString());
            editor.putString(key, String.valueOf(DEFAULT_SETTING_DRAW_THRESHOLD));
            editor.apply();
        }
        this.mSettings.draw_threshold = Double.valueOf(sp.getString(key, String.valueOf(DEFAULT_SETTING_DRAW_THRESHOLD))).doubleValue();
        key = this.mActivity.getResources().getString(R.string.KEY_SENSOR_MODE);
        if (!sp.contains(key)) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(key);
            stringBuilder3.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder3.toString());
            editor.putString(key, String.valueOf(0));
            editor.apply();
        }
        this.mSettings.sensor_mode = Integer.parseInt(sp.getString(key, String.valueOf(0)));
        key = this.mActivity.getResources().getString(R.string.KEY_SENSOR_USE_MODE);
        if (!sp.contains(key)) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(key);
            stringBuilder3.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder3.toString());
            editor.putString(key, String.valueOf(DEFAULT_SETTING_SENSOR_USE_MODE));
            editor.apply();
        }
        this.mSettings.sensor_use_mode = Integer.parseInt(sp.getString(key, String.valueOf(DEFAULT_SETTING_SENSOR_USE_MODE)));
        key = this.mActivity.getResources().getString(R.string.KEY_USE_GRAVITY_SENSOR);
        if (!sp.contains(key)) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(key);
            stringBuilder3.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder3.toString());
            editor.putBoolean(key, false);
            editor.apply();
        }
        this.mSettings.use_gravity_sensor = sp.getBoolean(key, false);
        key = this.mActivity.getResources().getString(R.string.KEY_USE_ROUND_AUTO_END);
        if (!sp.contains(key)) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(key);
            stringBuilder3.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder3.toString());
            editor.putString(key, String.valueOf(0));
            editor.apply();
        }
        this.mSettings.use_round_auto_end = Integer.parseInt(sp.getString(key, String.valueOf(0)));
        key = this.mActivity.getResources().getString(R.string.KEY_UNSHARP_STRENGTH);
        if (!sp.contains(key)) {
            StringBuilder stringBuilder8 = new StringBuilder();
            stringBuilder8.append(key);
            stringBuilder8.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder8.toString());
            editor.putString(key, String.valueOf(UnsharpStrengthPreference.DEFAULT_VALUE));
            editor.apply();
        }
        this.mSettings.unsharp_strength = Integer.parseInt(sp.getString(key, String.valueOf(UnsharpStrengthPreference.DEFAULT_VALUE)));
        key = this.mActivity.getResources().getString(R.string.KEY_NR_AUTO);
        if (!sp.contains(key)) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(key);
            stringBuilder3.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder3.toString());
            editor.putBoolean(key, true);
            editor.apply();
        }
        this.mSettings.nr_auto = sp.getBoolean(key, true);
        key = this.mActivity.getResources().getString(R.string.KEY_NR_STRENGTH);
        if (!sp.contains(key)) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(key);
            stringBuilder3.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder3.toString());
            editor.putString(key, String.valueOf(0));
            editor.apply();
        }
        this.mSettings.nr_strength = Integer.parseInt(sp.getString(key, String.valueOf(0)));
        key = this.mActivity.getResources().getString(R.string.KEY_INPUT_MOVIE_FPS);
        if (!sp.contains(key)) {
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append(key);
            stringBuilder4.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder4.toString());
            editor.putString(key, String.valueOf(DEFAULT_SETTING_INPUT_MOVIE_FPS));
            editor.apply();
        }
        this.mSettings.input_movie_fps = Double.valueOf(sp.getString(key, String.valueOf(DEFAULT_SETTING_INPUT_MOVIE_FPS))).doubleValue();
        key = this.mActivity.getResources().getString(R.string.KEY_COLOR_CORRECTION_MODE);
        if (!sp.contains(key)) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(key);
            stringBuilder3.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder3.toString());
            editor.putString(key, "-1");
            editor.apply();
        }
        try {
            this.mSettings.color_correction_mode = Integer.parseInt(sp.getString(key, "-1"));
        } catch (Exception e2) {
            this.mSettings.color_correction_mode = Integer.parseInt("-1");
        }
        key = this.mActivity.getResources().getString(R.string.KEY_EDGE_MODE);
        if (!sp.contains(key)) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(key);
            stringBuilder3.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder3.toString());
            editor.putString(key, "-1");
            editor.apply();
        }
        try {
            this.mSettings.edge_mode = Integer.parseInt(sp.getString(key, "-1"));
        } catch (Exception e3) {
            this.mSettings.edge_mode = Integer.parseInt("-1");
        }
        key = this.mActivity.getResources().getString(R.string.KEY_NOISE_REDUCTION_MODE);
        if (!sp.contains(key)) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(key);
            stringBuilder3.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder3.toString());
            editor.putString(key, "-1");
            editor.apply();
        }
        try {
            this.mSettings.noise_reduction_mode = Integer.parseInt(sp.getString(key, "-1"));
        } catch (Exception e4) {
            this.mSettings.noise_reduction_mode = Integer.parseInt("-1");
        }
        key = this.mActivity.getResources().getString(R.string.KEY_SHADING_MODE);
        if (!sp.contains(key)) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(key);
            stringBuilder3.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder3.toString());
            editor.putString(key, "-1");
            editor.apply();
        }
        try {
            this.mSettings.shading_mode = Integer.parseInt(sp.getString(key, "-1"));
        } catch (Exception e5) {
            this.mSettings.shading_mode = Integer.parseInt("-1");
        }
        key = this.mActivity.getResources().getString(R.string.KEY_TONEMAP_MODE);
        if (!sp.contains(key)) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(key);
            stringBuilder3.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder3.toString());
            editor.putString(key, "-1");
            editor.apply();
        }
        try {
            this.mSettings.tonemap_mode = Integer.parseInt(sp.getString(key, "-1"));
        } catch (Exception e6) {
            this.mSettings.tonemap_mode = Integer.parseInt("-1");
        }
        key = this.mActivity.getResources().getString(R.string.KEY_AOV_GAIN);
        if (!sp.contains(key)) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(key);
            stringBuilder3.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder3.toString());
            editor.putString(key, String.valueOf(1.0d));
            editor.apply();
        }
        this.mSettings.aov_gain = Double.valueOf(sp.getString(key, String.valueOf(1.0d))).doubleValue();
        key = this.mActivity.getResources().getString(R.string.KEY_DISTORTION_K1);
        if (!sp.contains(key)) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(key);
            stringBuilder3.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder3.toString());
            editor.putString(key, String.valueOf(DEFAULT_SETTING_DISTORTION_K1));
            editor.apply();
        }
        this.mSettings.distortion_k1 = Double.valueOf(sp.getString(key, String.valueOf(DEFAULT_SETTING_DISTORTION_K1))).doubleValue();
        key = this.mActivity.getResources().getString(R.string.KEY_DISTORTION_K2);
        if (!sp.contains(key)) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(key);
            stringBuilder3.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder3.toString());
            editor.putString(key, String.valueOf(DEFAULT_SETTING_DISTORTION_K2));
            editor.apply();
        }
        this.mSettings.distortion_k2 = Double.valueOf(sp.getString(key, String.valueOf(DEFAULT_SETTING_DISTORTION_K2))).doubleValue();
        key = this.mActivity.getResources().getString(R.string.KEY_DISTORTION_K3);
        if (!sp.contains(key)) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(key);
            stringBuilder3.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder3.toString());
            editor.putString(key, String.valueOf(DEFAULT_SETTING_DISTORTION_K3));
            editor.apply();
        }
        this.mSettings.distortion_k3 = Double.valueOf(sp.getString(key, String.valueOf(DEFAULT_SETTING_DISTORTION_K3))).doubleValue();
        key = this.mActivity.getResources().getString(R.string.KEY_DISTORTION_K4);
        if (!sp.contains(key)) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(key);
            stringBuilder3.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder3.toString());
            editor.putString(key, String.valueOf(DEFAULT_SETTING_DISTORTION_K4));
            editor.apply();
        }
        this.mSettings.distortion_k4 = Double.valueOf(sp.getString(key, String.valueOf(DEFAULT_SETTING_DISTORTION_K4))).doubleValue();
        key = this.mActivity.getResources().getString(R.string.KEY_ROTATION_RATIO);
        if (!sp.contains(key)) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(key);
            stringBuilder3.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder3.toString());
            editor.putString(key, String.valueOf(1.0d));
            editor.apply();
        }
        this.mSettings.rotation_ratio = Double.valueOf(sp.getString(key, String.valueOf(1.0d))).doubleValue();
        Integer defaultMode = (Integer) DEFAULT_UI_CONTROL_MODES.get(String.format(Locale.US, "%s,%d", new Object[]{BuildUtil.getModel(), Integer.valueOf(BuildUtil.getSdkVersion())}));
        if (defaultMode == null) {
            defaultMode = Integer.valueOf(0);
        }
        key = this.mActivity.getResources().getString(R.string.KEY_UI_CONTROL_MODE);
        if (!sp.contains(key)) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(key);
            stringBuilder3.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder3.toString());
            editor.putString(key, String.valueOf(defaultMode));
            editor.apply();
        }
        this.mSettings.ui_control_mode = Integer.parseInt(sp.getString(key, String.valueOf(defaultMode)));
        key = this.mActivity.getResources().getString(R.string.KEY_FOCUS_MODE);
        if (!sp.contains(key)) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(key);
            stringBuilder3.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder3.toString());
            editor.putString(key, String.valueOf(0));
            editor.apply();
        }
        this.mSettings.focus_mode = Integer.parseInt(sp.getString(key, String.valueOf(0)));
        key = this.mActivity.getResources().getString(R.string.KEY_PROJECTION_MODE);
        if (!sp.contains(key)) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(key);
            stringBuilder3.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder3.toString());
            editor.putString(key, String.valueOf(0));
            editor.apply();
        }
        this.mSettings.projection_mode = Integer.parseInt(sp.getString(key, String.valueOf(0)));
        key = this.mActivity.getResources().getString(R.string.KEY_MOTION_DETECTION_MODE);
        if (!sp.contains(key)) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(key);
            stringBuilder3.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder3.toString());
            editor.putString(key, String.valueOf(DEFAULT_SETTING_MOTION_DETECTION_MODE));
            editor.apply();
        }
        this.mSettings.motion_detection_mode = Integer.parseInt(sp.getString(key, String.valueOf(DEFAULT_SETTING_MOTION_DETECTION_MODE)));
        key = this.mActivity.getResources().getString(R.string.KEY_USE_CAMERA2);
        if (!sp.contains(key)) {
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(key);
            stringBuilder3.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder3.toString());
            editor.putBoolean(key, true);
            editor.apply();
        }
        this.mSettings.use_camera2 = sp.getBoolean(key, true);
        key = this.mActivity.getResources().getString(R.string.KEY_USE_WDR2);
        if (!sp.contains(key)) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(key);
            stringBuilder2.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder2.toString());
            editor.putBoolean(key, false);
            editor.apply();
        }
        this.mSettings.use_wdr2 = sp.getBoolean(key, false);
        key = this.mActivity.getResources().getString(R.string.KEY_MAKE_360);
        if (!sp.contains(key)) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(key);
            stringBuilder2.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder2.toString());
            editor.putBoolean(key, false);
            editor.apply();
        }
        this.mSettings.make_360 = sp.getBoolean(key, false);
        key = this.mActivity.getResources().getString(R.string.KEY_USE_60FPS);
        if (!sp.contains(key)) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(key);
            stringBuilder2.append(" is nothing!");
            LogFilter.i("Camera2App", stringBuilder2.toString());
            editor.putBoolean(key, false);
            editor.apply();
        }
        this.mSettings.use_60fps = sp.getBoolean(key, false);
        this.mSettings.print();
    }

    public void onClickThumbnail(View view) {
        if (!isEngineRunning()) {
            Uri uri = Thumbnail.getLastThumbnailUriFromContentResolver(this.mActivity.getContentResolver());
            Intent intent = new Intent(CameraUtil.REVIEW_ACTION, uri);
            intent.putExtra("camera_album", true);
            intent.setPackage("com.google.android.apps.photos");
            if (intent.resolveActivity(this.mActivity.getPackageManager()) != null) {
                try {
                    this.mActivity.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to start intent=");
                    stringBuilder.append(intent);
                    stringBuilder.append(": ");
                    stringBuilder.append(e);
                    Log.e("Camera2App", stringBuilder.toString());
                    try {
                        this.mActivity.startActivity(new Intent("android.intent.action.VIEW", uri));
                    } catch (Exception ex) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("No Activity could be found to open image or video");
                        stringBuilder2.append(ex);
                        Log.e("Camera2App", stringBuilder2.toString());
                    }
                }
            } else {
                Toast.makeText(this.mActivity, "Image viewer is not found.", 1).show();
            }
        }
    }

    public void onClickViewbutton(View view) {
        if (!isEngineRunning()) {
            String file_path = ((CameraApp) this.mActivity.getApplication()).getLastShotFile().filePath();
            if (file_path == "") {
                new AlertDialog.Builder(this.mActivity).setMessage("Please Take Photo Firstly.").setPositiveButton(17039370, null).show();
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("file_path");
                stringBuilder.append(file_path);
                Log.d("Camera2App", stringBuilder.toString());
                startPostView(file_path);
            }
        }
    }

    private void startPostView(String file_path) {
        Intent intent = new Intent(this.mActivity, ResultApp.class);
        intent.setAction("android.intent.action.VIEW");
        intent.putExtra(INTENT_FILENAME, file_path);
        this.mActivity.startActivity(intent);
    }

    private void configureTransform() {
        if (this.mTextureView != null) {
            CameraInfo cameraInfo = this.mMorphoCamera.cameraInfo();
            cameraInfo.setCaptureWidth(2400);
            cameraInfo.setCaptureHeight(1800);
            int rotation = this.mActivity.getWindowManager().getDefaultDisplay().getRotation();
            Matrix matrix = new Matrix();
            RectF viewRect = new RectF(0.0f, 0.0f, (float) this.mTextureView.getWidth(), (float) this.mTextureView.getHeight());
            RectF bufferRect = new RectF(0.0f, 0.0f, (float) cameraInfo.getCaptureHeight(), (float) cameraInfo.getCaptureWidth());
            float centerX = viewRect.centerX();
            float centerY = viewRect.centerY();
            if (1 == rotation || 3 == rotation) {
                bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
                matrix.setRectToRect(viewRect, bufferRect, ScaleToFit.FILL);
                float scale = Math.max(((float) this.mTextureView.getHeight()) / ((float) cameraInfo.getCaptureHeight()), ((float) this.mTextureView.getWidth()) / ((float) cameraInfo.getCaptureWidth()));
                matrix.postScale(scale, scale, centerX, centerY);
                matrix.postRotate((float) ((rotation - 2) * 90), centerX, centerY);
            } else if (2 == rotation) {
                matrix.postRotate(180.0f, centerX, centerY);
            }
            this.mTextureView.setTransform(matrix);
            View v = this.mRootView.findViewById(R.id.mini_preview_frame);
            viewRect = new RectF(0.0f, 0.0f, (float) v.getWidth(), (float) v.getHeight());
            centerX = viewRect.centerX();
            float centerY2 = viewRect.centerY();
            if (1 == rotation || 3 == rotation) {
                bufferRect.offset(centerX - bufferRect.centerX(), centerY2 - bufferRect.centerY());
                matrix.setRectToRect(viewRect, bufferRect, ScaleToFit.FILL);
                centerY = Math.max(((float) v.getHeight()) / ((float) cameraInfo.getCaptureHeight()), ((float) v.getWidth()) / ((float) cameraInfo.getCaptureWidth()));
                matrix.postScale(centerY, centerY, centerX, centerY2);
                matrix.postRotate((float) ((rotation - 2) * 90), centerX, centerY2);
            } else if (2 == rotation) {
                matrix.postRotate(180.0f, centerX, centerY2);
            }
            this.mMiniPreviewMatrix = new Matrix(matrix);
            if (this.mMiniPreviewImageView != null) {
                this.mMiniPreviewImageView.setImageMatrix(this.mMiniPreviewMatrix);
            }
        }
    }

    private void openCamera() {
        MorphoCamera morphoCamera = this.mMorphoCamera;
        CameraInfo cameraInfo = morphoCamera.cameraInfo();
        if (cameraInfo.isCameraEnabled() && cameraInfo.getOpenState() != 1 && cameraInfo.getOpenState() != 2) {
            if (!readViewAngle()) {
                Camera tmp_cam = MorphoCamera1.open(this.mSettings.camera_id);
                if (tmp_cam != null) {
                    Parameters parameters = tmp_cam.getParameters();
                    this.mViewAngleH = parameters.getHorizontalViewAngle();
                    this.mViewAngleV = parameters.getVerticalViewAngle();
                    if (BuildUtil.isHuaweiP9()) {
                        this.mViewAngleH = 65.01539f;
                        this.mViewAngleV = 51.05475f;
                    } else if (BuildUtil.isGalaxyS7()) {
                        this.mViewAngleH = 65.0f;
                        this.mViewAngleV = 51.0f;
                    } else if (BuildUtil.isSonyHinoki() || BuildUtil.isSonyRedwood()) {
                        this.mViewAngleH = 72.26766f;
                        this.mViewAngleV = 57.53211f;
                    }
                    tmp_cam.release();
                    writeViewAngle(this.mSettings.camera_id, this.mViewAngleH, this.mViewAngleV);
                }
            }
            if (morphoCamera.openCamera(this.mTextureView)) {
                if (!this.mUseCamera1) {
                    if (((MorphoCamera) this.mMorphoCamera).isOverLevelFull()) {
                        getCamera2ParamsFragment().setDefaultMode(this.mActivity.getSharedPreferences(CAMERA2_PARAM_FILE, 0), 3);
                    } else {
                        getCamera2ParamsFragment().setDefaultMode(this.mActivity.getSharedPreferences(CAMERA2_PARAM_FILE, 0), 0);
                    }
                }
                getCamera2ParamsFragment().initializeUI(cameraInfo, this.mSettings.use_60fps);
                getCamera2ParamsFragment().setSensorSensitivity(cameraInfo.getSensitivityMin());
                getCamera2ParamsFragment().setExposureTime(getCamera2ParamsFragment().frameDuration().longValue());
            }
        }
    }

    public void setNullDirectionFunction() {
        CameraInfo cameraInfo = this.mMorphoCamera.cameraInfo();
        cameraInfo.setCaptureWidth(2400);
        cameraInfo.setCaptureHeight(1800);
        this.mDirectionFunction = new DirectionFunction(cameraInfo.getCaptureWidth(), cameraInfo.getCaptureHeight(), 1, 1, 1, 0);
    }

    public void onCaptureCompleted(TotalCaptureResult result) {
        if (this.mSettings.save_input_images != 0) {
            dumpOisData(result);
        }
    }

    private void dumpOisData(CaptureResult captureResult) {
        if (this.mOisTimestampIndex != -2 && this.mOisShiftPixelXIndex != -2 && this.mOisShiftPixelYIndex != -2) {
            long[] ois_timestamps_boottime = new long[0];
            float[] ois_shift_pixel_x = new float[0];
            float[] ois_shift_pixel_y = new float[0];
            if (!(this.mOisTimestampIndex == -1 || this.mOisShiftPixelXIndex == -1 || this.mOisShiftPixelYIndex == -1)) {
                Key<?> key = (Key) captureResult.getKeys().get(this.mOisTimestampIndex);
                if (key.getName().matches("com.google.nexus.experimental2017.stats.ois_timestamps_boottime")) {
                    Object value = captureResult.get(key);
                    if (value != null && value.getClass().isArray()) {
                        ois_timestamps_boottime = (long[]) value;
                        key = (Key) captureResult.getKeys().get(this.mOisShiftPixelXIndex);
                        if (key.getName().matches("com.google.nexus.experimental2017.stats.ois_shift_pixel_x")) {
                            value = captureResult.get(key);
                            if (value != null && value.getClass().isArray()) {
                                ois_shift_pixel_x = (float[]) value;
                                key = (Key) captureResult.getKeys().get(this.mOisShiftPixelYIndex);
                                if (key.getName().matches("com.google.nexus.experimental2017.stats.ois_shift_pixel_y")) {
                                    value = captureResult.get(key);
                                    if (value != null && value.getClass().isArray()) {
                                        ois_shift_pixel_y = (float[]) value;
                                    } else {
                                        return;
                                    }
                                }
                                this.mOisShiftPixelYIndex = -1;
                            } else {
                                return;
                            }
                        }
                        this.mOisShiftPixelXIndex = -1;
                    } else {
                        return;
                    }
                }
                this.mOisTimestampIndex = -1;
            }
            if (this.mOisTimestampIndex == -1 || this.mOisShiftPixelXIndex == -1 || this.mOisShiftPixelYIndex == -1) {
                int index = -1;
                this.mOisShiftPixelYIndex = -2;
                this.mOisShiftPixelXIndex = -2;
                this.mOisTimestampIndex = -2;
                for (Key<?> key2 : captureResult.getKeys()) {
                    index++;
                    if (key2.getName().startsWith("com.google.nexus.experimental2017.stats.ois_")) {
                        boolean is_ois_shift_pixel_y = false;
                        boolean is_ois_shift_pixel_x = false;
                        boolean is_ois_timestamps_boottime = false;
                        if (key2.getName().matches("com.google.nexus.experimental2017.stats.ois_timestamps_boottime")) {
                            is_ois_timestamps_boottime = true;
                            this.mOisTimestampIndex = index;
                        } else if (key2.getName().matches("com.google.nexus.experimental2017.stats.ois_shift_pixel_x")) {
                            is_ois_shift_pixel_x = true;
                            this.mOisShiftPixelXIndex = index;
                        } else if (key2.getName().matches("com.google.nexus.experimental2017.stats.ois_shift_pixel_y")) {
                            is_ois_shift_pixel_y = true;
                            this.mOisShiftPixelYIndex = index;
                        } else {
                            continue;
                        }
                        Object value2 = captureResult.get(key2);
                        if (value2 != null && value2.getClass().isArray()) {
                            if (is_ois_timestamps_boottime) {
                                ois_timestamps_boottime = (long[]) value2;
                            } else if (is_ois_shift_pixel_x) {
                                ois_shift_pixel_x = (float[]) value2;
                            } else if (is_ois_shift_pixel_y) {
                                ois_shift_pixel_y = (float[]) value2;
                            }
                        }
                        if (this.mOisTimestampIndex != -2 && this.mOisShiftPixelXIndex != -2 && this.mOisShiftPixelYIndex != -2) {
                            break;
                        }
                    }
                }
            }
            if (this.mOisTimestampIndex != -2 && this.mOisShiftPixelXIndex != -2 && this.mOisShiftPixelYIndex != -2) {
                try {
                    File file = new File(this.mInputFolderPath, "sh.txt");
                    FileWriter fwriter = new FileWriter(file, true);
                    BufferedWriter writer = new BufferedWriter(fwriter);
                    for (int i = 0; i < ois_timestamps_boottime.length; i++) {
                        writer.write(String.format(Locale.US, "%d\t%f\t%f\r\n", new Object[]{Long.valueOf(ois_timestamps_boottime[i]), Float.valueOf(ois_shift_pixel_x[i]), Float.valueOf(ois_shift_pixel_y[i])}));
                    }
                    writer.close();
                    fwriter.close();
                    scanFile(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void setSupportedCaptureSize() {
        ((CameraApp) this.mActivity.getApplication()).setSupportedPictureSizes(this.mMorphoCamera.getSupportedPreviewSizes());
    }

    private void setPreviewSize() {
        int index;
        int[][] PREVIEW_RESOLUTION = new int[][]{new int[]{DEFAULT_PREVIEW_WIDTH, 720}, new int[]{DEFAULT_PREVIEW_WIDTH, 720}};
        DisplayMetrics metrics = new DisplayMetrics();
        this.mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        if (metrics.densityDpi >= 320) {
            index = 0;
        } else {
            index = 1;
        }
        CameraApp app = (CameraApp) this.mActivity.getApplication();
        Size[] sizes = app.getSupportedPictureSizes();
        Size size = sizes[this.mSettings.capture_size_index];
        int i = 0;
        while (i < sizes.length) {
            if (sizes[i].getWidth() == PREVIEW_RESOLUTION[index][0] && sizes[i].getHeight() == PREVIEW_RESOLUTION[index][1]) {
                size = sizes[i];
                break;
            }
            i++;
        }
        app.setPreviewSize(size);
    }

    public Size getPreviewSize() {
        return this.mDefaultPreviewSize;
    }

    private int getIndexOfDefaultCaptureSize() {
        Size[] sizes = ((CameraApp) this.mActivity.getApplication()).getSupportedPictureSizes();
        for (int i = 0; i < sizes.length; i++) {
            Size size = sizes[i];
            if (size.getWidth() == DEFAULT_PREVIEW_WIDTH && size.getHeight() == 720) {
                return i;
            }
            if (size.getWidth() == DEFAULT_PREVIEW_WIDTH2 && size.getHeight() == DEFAULT_PREVIEW_HEIGHT2) {
                return i;
            }
            if (size.getWidth() == DEFAULT_PREVIEW_WIDTH3 && size.getHeight() == DEFAULT_PREVIEW_HEIGHT3) {
                return i;
            }
        }
        return 0;
    }

    public boolean isEngineRunning() {
        boolean isSaving = false;
        ProgressBar progress = (ProgressBar) this.mRootView.findViewById(R.id.SavingProgressBar);
        if (progress != null) {
            isSaving = progress.getVisibility() == 0;
        }
        if (this.mMorphoPanoramaGP3 != null || isSaving) {
            return true;
        }
        return false;
    }

    public void onSaveUri(Uri uri) {
        ((CameraApp) this.mActivity.getApplication()).getLastShotFile().setUri(uri);
    }

    private int getThumbnailRotation(String file) {
        int degree = 0;
        if (file == null) {
            return 0;
        }
        try {
            int orientation = new ExifInterface(file).getAttributeInt("Orientation", 0);
            if (orientation == 6) {
                degree = 90;
            } else if (orientation == 3) {
                degree = MediaProviderUtils.ROTATION_180;
            } else if (orientation == 8) {
                degree = MediaProviderUtils.ROTATION_270;
            }
            return degree;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private Bitmap getThumbnailBitmap() {
        FileNotFoundException e;
        IOException e2;
        Size size;
        CameraApp app = (CameraApp) this.mActivity.getApplication();
        Size maxSize = getActionButtonsFragment().getThumbnailMaxSize();
        app.updateThumbnailMaxSize(maxSize.getWidth(), maxSize.getHeight());
        LogFilter.v("Camera2App", String.format(Locale.US, "Thumbnail max size=%dx%d", new Object[]{Integer.valueOf(65), Integer.valueOf(65)}));
        int outWidth = app.getLastShotFile().width();
        int outHeight = app.getLastShotFile().height();
        if (outWidth == 0) {
            return Bitmap.createBitmap(new int[(65 * 65)], 65, 65, Config.ARGB_8888);
        }
        Options options = new Options();
        options.inSampleSize = 1;
        int i;
        if (outHeight < outWidth) {
            for (i = 2; 65 < outHeight / i; i *= 2) {
                options.inSampleSize = i;
            }
        } else {
            for (i = 2; 65 < outWidth / i; i *= 2) {
                options.inSampleSize = i;
            }
        }
        int top = Math.max(0, (outHeight - (options.inSampleSize * 65)) / 2);
        int bottom = Math.min(outHeight, (options.inSampleSize * 65) + top);
        int left = Math.max(0, (outWidth - (options.inSampleSize * 65)) / 2);
        Rect rect = new Rect(left, top, Math.min(outWidth, (options.inSampleSize * 65) + left), bottom);
        Rect rect2;
        try {
            String str = "Camera2App";
            Locale locale = Locale.US;
            String str2 = "Decode thumbnail info. Clip=(%d,%d)-(%d,%d) Scale=%d";
            Object[] objArr = new Object[5];
            rect2 = rect;
            try {
                objArr[0] = Integer.valueOf(rect2.left);
                objArr[1] = Integer.valueOf(rect2.top);
                objArr[2] = Integer.valueOf(rect2.right);
                objArr[3] = Integer.valueOf(rect2.bottom);
                objArr[4] = Integer.valueOf(options.inSampleSize);
                LogFilter.i(str, String.format(locale, str2, objArr));
                FileInputStream inputStream = new FileInputStream(app.getLastShotFile().filePath());
                Bitmap thumbBmp = BitmapRegionDecoder.newInstance(inputStream, true).decodeRegion(rect2, options);
                inputStream.close();
                return thumbBmp;
            } catch (FileNotFoundException e3) {
                e = e3;
                e.printStackTrace();
                LogFilter.e("Camera2App", "Thumbnail file read error. (FileNotFoundException)");
                app.clearLastShotFile();
                return Bitmap.createBitmap(new int[(65 * 65)], 65, 65, Config.ARGB_8888);
            } catch (IOException e4) {
                e2 = e4;
                e2.printStackTrace();
                LogFilter.e("Camera2App", "Thumbnail file read error. (IOException)");
                app.clearLastShotFile();
                return Bitmap.createBitmap(new int[(65 * 65)], 65, 65, Config.ARGB_8888);
            }
        } catch (FileNotFoundException e5) {
            e = e5;
            size = maxSize;
            rect2 = rect;
            e.printStackTrace();
            LogFilter.e("Camera2App", "Thumbnail file read error. (FileNotFoundException)");
            app.clearLastShotFile();
            return Bitmap.createBitmap(new int[(65 * 65)], 65, 65, Config.ARGB_8888);
        } catch (IOException e6) {
            e2 = e6;
            size = maxSize;
            rect2 = rect;
            e2.printStackTrace();
            LogFilter.e("Camera2App", "Thumbnail file read error. (IOException)");
            app.clearLastShotFile();
            return Bitmap.createBitmap(new int[(65 * 65)], 65, 65, Config.ARGB_8888);
        }
    }

    private void releaseImageBitmap() {
        synchronized (PreviewImageSynchronizedObject) {
            this.mPreviewImageView.setImageDrawable(null);
            this.mPreviewBitmap = null;
            this.mPreviewFitBitmap = null;
        }
    }

    private void saveSettings(String dirPath, String imageFormat) {
        IOException e;
        try {
            try {
                File file = new File(dirPath, SETTING_FILE_NAME);
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), com.bumptech.glide.load.Key.STRING_CHARSET_NAME));
                CameraInfo cameraInfo = this.mMorphoCamera.cameraInfo();
                cameraInfo.setCaptureWidth(2400);
                cameraInfo.setCaptureHeight(1800);
                String write_str = String.format(Locale.US, "%s%s%s\r%n", new Object[]{SETTING_KEY_BUILD_MODEL, SETTING_SEPARATOR, BuildUtil.getModel()});
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{"width", SETTING_SEPARATOR, Integer.valueOf(cameraInfo.getCaptureWidth())}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{"height", SETTING_SEPARATOR, Integer.valueOf(cameraInfo.getCaptureHeight())}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{SETTING_KEY_MAX_WIDTH, SETTING_SEPARATOR, Integer.valueOf(this.mMaxWidth)}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{SETTING_KEY_MAX_HEIGHT, SETTING_SEPARATOR, Integer.valueOf(this.mMaxHeight)}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%s\r%n", new Object[]{SETTING_KEY_FORMAT, SETTING_SEPARATOR, imageFormat}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{SETTING_KEY_AUTO_AE_LOCK, SETTING_SEPARATOR, Integer.valueOf(this.mSettings.auto_ae_lock)}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{SETTING_KEY_AUTO_WB_LOCK, SETTING_SEPARATOR, Integer.valueOf(this.mSettings.auto_wb_lock)}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{SETTING_KEY_ANTI_BANDING, SETTING_SEPARATOR, Integer.valueOf(this.mSettings.anti_banding)}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{SETTING_KEY_PREVIEW_SCALE, SETTING_SEPARATOR, Integer.valueOf(this.mDirectionFunction.getScale())}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{SETTING_KEY_ANGLE, SETTING_SEPARATOR, Integer.valueOf(this.mAngle)}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{SETTING_KEY_ATTACH_NUM_DIRECTION_UNDECIDED, SETTING_SEPARATOR, Long.valueOf(this.mAttachNumDirectionUndecided)}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{SETTING_KEY_PANORAMA_DIRECTION, SETTING_SEPARATOR, Integer.valueOf(this.mDirectionFunction.getDirection())}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{SETTING_KEY_SENSOR_MODE, SETTING_SEPARATOR, Integer.valueOf(this.mSettings.sensor_mode)}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{SETTING_KEY_SENSOR_USE_MODE, SETTING_SEPARATOR, Integer.valueOf(this.mSettings.sensor_use_mode)}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{SETTING_KEY_USE_GRAVITY_SENSOR, SETTING_SEPARATOR, Integer.valueOf(this.mSettings.use_gravity_sensor)}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{SETTING_KEY_USE_ROUND_AUTO_END, SETTING_SEPARATOR, Integer.valueOf(this.mSettings.use_round_auto_end)}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{SETTING_KEY_UNSHARP_STRENGTH, SETTING_SEPARATOR, Integer.valueOf(this.mShotSettings.unsharpStrength)}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{SETTING_KEY_NR_AUTO, SETTING_SEPARATOR, Boolean.valueOf(this.mSettings.nr_auto)}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{SETTING_KEY_NR_STRENGTH, SETTING_SEPARATOR, Integer.valueOf(this.mShotSettings.noiseReductionStrength)}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{"capture_mode", SETTING_SEPARATOR, Integer.valueOf(this.mSettings.capture_mode)}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{SETTING_KEY_COLOR_CORRECTION_MODE, SETTING_SEPARATOR, Integer.valueOf(this.mSettings.color_correction_mode)}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{SETTING_KEY_EDGE_MODE, SETTING_SEPARATOR, Integer.valueOf(this.mShotSettings.edgeMode)}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{SETTING_KEY_NOISE_REDUCTION_MODE, SETTING_SEPARATOR, Integer.valueOf(this.mShotSettings.noiseReductionMode)}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{SETTING_KEY_SHADING_MODE, SETTING_SEPARATOR, Integer.valueOf(this.mSettings.shading_mode)}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{SETTING_KEY_TONEMAP_MODE, SETTING_SEPARATOR, Integer.valueOf(this.mSettings.tonemap_mode)}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{SETTING_KEY_USE_CAMERA2, SETTING_SEPARATOR, Integer.valueOf(this.mSettings.use_camera2)}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{SETTING_KEY_CAMERA_ID, SETTING_SEPARATOR, Integer.valueOf(this.mSettings.camera_id)}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{SETTING_KEY_USE_WDR2, SETTING_SEPARATOR, Integer.valueOf(this.mSettings.use_wdr2)}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{SETTING_KEY_PROJECTION_MODE, SETTING_SEPARATOR, Integer.valueOf(this.mSettings.projection_mode)}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{SETTING_KEY_MOTION_DETECTION_MODE, SETTING_SEPARATOR, Integer.valueOf(this.mSettings.motion_detection_mode)}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d %%\r%n", new Object[]{SETTING_KEY_RENDERING_AREA, SETTING_SEPARATOR, Integer.valueOf(this.mSettings.rendering_area)}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{SETTING_KEY_MAKE_360, SETTING_SEPARATOR, Integer.valueOf(this.mSettings.make_360)}));
                write_str = stringBuilder.toString();
                stringBuilder = new StringBuilder();
                stringBuilder.append(write_str);
                stringBuilder.append(String.format(Locale.US, "%s%s%d\r%n", new Object[]{SETTING_KEY_USE_60FPS, SETTING_SEPARATOR, Integer.valueOf(this.mSettings.use_60fps)}));
                writer.write(stringBuilder.toString());
                writer.close();
                scanFile(file);
            } catch (IOException e2) {
                e = e2;
            }
        } catch (IOException e3) {
            e = e3;
            String str = dirPath;
            e.printStackTrace();
        }
    }

    public static void d_save_raw(byte[] data, String folderPath, String name) {
        PerformanceCounter performance = PerformanceCounter.newInstance(false);
        OutputStream os;
        try {
            String str;
            String str2;
            performance.start();
            os = null;
            try {
                os = new FileOutputStream(String.format(Locale.US, "%s/%s.yuv", new Object[]{folderPath, name}));
                os.write(data);
                os.close();
                performance.stop();
                str = "Camera2App";
                str2 = "InputSave";
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                if (os != null) {
                    os.close();
                    performance.stop();
                    str = "Camera2App";
                    str2 = "InputSave";
                }
            }
            performance.putLog(str, str2);
        } catch (IOException e2) {
            e2.printStackTrace();
        } catch (Throwable th) {
            if (os != null) {
                os.close();
                performance.stop();
                performance.putLog("Camera2App", "InputSave");
            }
        }
    }

    private static int getAspectTableIndex(float width, float height) {
        float aspect = height / width;
        for (int i = 0; i < ASPECT_TABLE.length; i++) {
            if (((double) Math.abs(aspect - (((float) ASPECT_TABLE[i][1]) / ((float) ASPECT_TABLE[i][0])))) < 0.1d) {
                return i;
            }
        }
        return 0;
    }

    private static void getRatios(int baseIndex, int outputIndex, float[] ratios) {
        if (baseIndex == outputIndex) {
            ratios[0] = 1.0f;
            ratios[1] = 1.0f;
            return;
        }
        float baseAspect = ((float) ASPECT_TABLE[baseIndex][1]) / ((float) ASPECT_TABLE[baseIndex][0]);
        float outputAspect = ((float) ASPECT_TABLE[outputIndex][1]) / ((float) ASPECT_TABLE[outputIndex][0]);
        int widthLcm = getLCM(ASPECT_TABLE[outputIndex][0], ASPECT_TABLE[baseIndex][0]);
        int baseRatio = widthLcm / ASPECT_TABLE[baseIndex][0];
        int outputRatio = widthLcm / ASPECT_TABLE[outputIndex][0];
        if (outputAspect < baseAspect) {
            ratios[0] = 1.0f;
            ratios[1] = ((float) (ASPECT_TABLE[outputIndex][1] * outputRatio)) / ((float) (ASPECT_TABLE[baseIndex][1] * baseRatio));
        } else {
            ratios[0] = ((float) (ASPECT_TABLE[baseIndex][1] * baseRatio)) / ((float) (ASPECT_TABLE[outputIndex][1] * outputRatio));
            ratios[1] = 1.0f;
        }
    }

    private static int getGCD(int a, int b) {
        int temp;
        if (a > b) {
            temp = a;
            a = b;
            b = temp;
        }
        while (a != 0) {
            temp = a;
            a = b % a;
            b = temp;
        }
        return b;
    }

    private static int getLCM(int a, int b) {
        return (a * b) / getGCD(a, b);
    }

    private int getBaseAspect() {
        CameraInfo cameraInfo = this.mMorphoCamera.cameraInfo();
        cameraInfo.setCaptureWidth(2400);
        cameraInfo.setCaptureHeight(1800);
        int physicalIndex = getAspectTableIndex(cameraInfo.getPhysicalWidth(), cameraInfo.getPhysicalHeight());
        int activeIndex = getAspectTableIndex((float) cameraInfo.getActiveArrayWidth(), (float) cameraInfo.getActiveArrayHeight());
        if (physicalIndex == activeIndex) {
            return physicalIndex;
        }
        return activeIndex;
    }

    public static String addImageAsApplication(ContentResolver cr, ByteBuffer byteBuffer, int width, int height, Location location) {
        long dateTaken = System.currentTimeMillis();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(createName(dateTaken));
        stringBuilder.append(Storage.JPEG_POSTFIX);
        String name = stringBuilder.toString();
        return addImageAsApplication(cr, name, dateTaken, Storage.DIRECTORY, name, byteBuffer, width, height, "YUV420_SEMIPLANAR", location, new IPanoramaSaveListener() {
            public void onSaveUri(Uri uri) {
            }
        });
    }

    private static String createName(long dateTaken) {
        return DateFormat.format("yyyy-MM-dd_kk-mm-ss", dateTaken).toString();
    }

    public static String addImageAsApplication(ContentResolver cr, String name, long dateTaken, String directory, String filename, ByteBuffer byteBuffer, int width, int height, String format, Location location, IPanoramaSaveListener saveCallback) {
        String str = directory;
        File dir = new File(str);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                return null;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(dir.toString());
            stringBuilder.append(" create");
            LogFilter.d("PanoramaGP3", stringBuilder.toString());
        }
        String orgFileName = filename;
        File file = new File(str, orgFileName);
        int sequentialNo = 0;
        String filename2 = orgFileName;
        while (file.exists()) {
            sequentialNo++;
            String[] str2 = orgFileName.split("\\.");
            String sequentialNoStr = Integer.toString(sequentialNo);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(str2[0]);
            stringBuilder2.append("-");
            stringBuilder2.append(sequentialNoStr);
            stringBuilder2.append(".");
            stringBuilder2.append(str2[1]);
            filename2 = stringBuilder2.toString();
            file = new File(str, filename2);
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("NewFilename:");
            stringBuilder3.append(filename2);
            LogFilter.d("PanoramaGP3", stringBuilder3.toString());
            if (sequentialNo >= 1000) {
                LogFilter.e("PanoramaGP3", "NewFilename 1000 count over!!");
                return null;
            }
        }
        String filePath = new StringBuilder();
        filePath.append(str);
        filePath.append("/");
        filePath.append(filename2);
        filePath = filePath.toString();
        return null;
    }

    private String getSaveFilePath(String directory, String orgFileName) {
        File dir = new File(directory);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                return null;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(dir.toString());
            stringBuilder.append(" create");
            LogFilter.d("PanoramaGP3", stringBuilder.toString());
        }
        File file = new File(directory, orgFileName);
        int sequentialNo = 0;
        String filename = orgFileName;
        while (file.exists()) {
            sequentialNo++;
            String[] str = orgFileName.split("\\.");
            String sequentialNoStr = Integer.toString(sequentialNo);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(str[0]);
            stringBuilder2.append("-");
            stringBuilder2.append(sequentialNoStr);
            stringBuilder2.append(".");
            stringBuilder2.append(str[1]);
            filename = stringBuilder2.toString();
            file = new File(directory, filename);
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("NewFilename:");
            stringBuilder3.append(filename);
            LogFilter.d("PanoramaGP3", stringBuilder3.toString());
            if (sequentialNo >= 1000) {
                LogFilter.e("PanoramaGP3", "NewFilename 1000 count over!!");
                return null;
            }
        }
        String filePath = new StringBuilder();
        filePath.append(directory);
        filePath.append("/");
        filePath.append(filename);
        return filePath.toString();
    }

    private void insertMediaStore(String name, String filePath, Location location, int orientation, IPanoramaSaveListener saveCallback, GalleryInfoData galleryInfoData) {
        File file = new File(filePath);
        ContentValues values = new ContentValues(8);
        values.put("title", name);
        values.put("_display_name", name);
        int msOrientation = 0;
        values.put("datetaken", Long.valueOf(this.mDateTaken[0]));
        values.put("date_added", Long.valueOf(this.mDateTaken[0] / 1000));
        values.put(InfoTable.DATE_MODIFIED, Long.valueOf(this.mDateTaken[0] / 1000));
        values.put(InfoTable.MIME_TYPE, "image/jpeg");
        values.put("_data", filePath);
        values.put("_size", Long.valueOf(file.length()));
        if (location != null) {
            values.put(InfoTable.LATITUDE, Double.valueOf(location.getLatitude()));
            values.put(InfoTable.LONGITUDE, Double.valueOf(location.getLongitude()));
        }
        if (this.mSettings.make_360 && BuildUtil.isLGE() && !BuildUtil.isG4() && galleryInfoData != null) {
            values.put("camera_mode", Integer.valueOf(100));
            values.put("use_panorama_viewer", XMPConst.TRUESTR);
            values.put("projection_type", "equirectangular");
            values.put("cropped_area_image_width_pixels", Integer.valueOf(galleryInfoData.crop_width));
            values.put("cropped_area_image_height_pixels", Integer.valueOf(galleryInfoData.crop_height));
            values.put("cropped_area_left_pixels", Integer.valueOf(galleryInfoData.crop_left));
            values.put("cropped_area_top_pixels", Integer.valueOf(galleryInfoData.crop_top));
            values.put("full_pano_width_pixels", Integer.valueOf(galleryInfoData.whole_width));
            values.put("full_pano_height_pixels", Integer.valueOf(galleryInfoData.whole_height));
        }
        if (orientation == 0) {
            msOrientation = 90;
        } else if (orientation == 90) {
            msOrientation = MediaProviderUtils.ROTATION_180;
        } else if (orientation == MediaProviderUtils.ROTATION_180) {
            msOrientation = MediaProviderUtils.ROTATION_270;
        } else if (orientation != MediaProviderUtils.ROTATION_270) {
            msOrientation = -1;
        }
        int msOrientation2 = msOrientation;
        if (msOrientation2 > 0) {
            values.put("orientation", Integer.valueOf(msOrientation2));
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("insertMediaStore:");
        stringBuilder.append(values.toString());
        LogFilter.i("Camera2App", stringBuilder.toString());
        saveCallback.onSaveUri(this.mActivity.getContentResolver().insert(Media.EXTERNAL_CONTENT_URI, values));
    }

    public static String createDateStringForAppSeg(long dateTaken) {
        Date date = new Date(dateTaken);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(date);
    }

    private static void setInExif(String filename, Location location, int imageOrientation, int cameraOrientation, boolean front) {
        IOException e;
        int i = imageOrientation;
        String nowTime = DateFormat.format("yyyy:MM:dd kk:mm:ss", System.currentTimeMillis()).toString();
        int i2;
        try {
            try {
                ExifInterface exif = new ExifInterface(filename);
                exif.setAttribute("DateTime", nowTime);
                exif.setAttribute("DateTimeOriginal", nowTime);
                exif.setAttribute("DateTimeDigitized", nowTime);
                exif.setAttribute("Make", BuildUtil.getBrand());
                exif.setAttribute("Model", BuildUtil.getModel());
                if (i >= 0) {
                    int exifOrientation;
                    if (cameraOrientation == 90) {
                        if (front) {
                            if (i == 90) {
                                exifOrientation = 1;
                            } else if (i == 180) {
                                exifOrientation = 6;
                            } else if (i != 270) {
                                exifOrientation = 8;
                            } else {
                                exifOrientation = 3;
                            }
                        } else if (i == 90) {
                            exifOrientation = 1;
                        } else if (i == 180) {
                            exifOrientation = 8;
                        } else if (i != 270) {
                            exifOrientation = 6;
                        } else {
                            exifOrientation = 3;
                        }
                    } else if (front) {
                        if (i == 90) {
                            exifOrientation = 1;
                        } else if (i == 180) {
                            exifOrientation = 6;
                        } else if (i != 270) {
                            exifOrientation = 8;
                        } else {
                            exifOrientation = 3;
                        }
                    } else if (i == 90) {
                        exifOrientation = 3;
                    } else if (i == 180) {
                        exifOrientation = 6;
                    } else if (i != 270) {
                        exifOrientation = 8;
                    } else {
                        exifOrientation = 1;
                    }
                    try {
                        exif.setAttribute("Orientation", String.valueOf(exifOrientation));
                    } catch (IOException e2) {
                        e = e2;
                        e.printStackTrace();
                    }
                }
                i2 = cameraOrientation;
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    String lat_str = locationValueToString(latitude);
                    String lat_str_ref = latitudeValueToNorS(latitude);
                    String lon_str = locationValueToString(longitude);
                    String lon_str_ref = longitudeValueToEorW(longitude);
                    exif.setAttribute("GPSLatitude", lat_str);
                    exif.setAttribute("GPSLatitudeRef", lat_str_ref);
                    exif.setAttribute("GPSLongitude", lon_str);
                    exif.setAttribute("GPSLongitudeRef", lon_str_ref);
                }
                exif.saveAttributes();
            } catch (IOException e3) {
                e = e3;
                i2 = cameraOrientation;
                e.printStackTrace();
            }
        } catch (IOException e4) {
            e = e4;
            String str = filename;
            i2 = cameraOrientation;
            e.printStackTrace();
        }
    }

    private static String latitudeValueToNorS(double value) {
        if (value > Camera2ParamsFragment.TARGET_EV) {
            return "N";
        }
        return GpsLatitudeRef.SOUTH;
    }

    private static String longitudeValueToEorW(double value) {
        if (value > Camera2ParamsFragment.TARGET_EV) {
            return GpsLongitudeRef.EAST;
        }
        return GpsLongitudeRef.WEST;
    }

    private static String locationValueToString(double value) {
        long x = (long) Math.rint(360000.0d * Math.abs(value));
        return String.format("%d/1,%d/1,%d/100", new Object[]{Long.valueOf(x / 360000), Long.valueOf((x / 6000) % 60), Long.valueOf(x % 6000)});
    }

    public void onSensorChanged(SensorEvent event) {
        if (this.mIsSensorAverage) {
            float[] fArr = this.mGravities;
            fArr[0] = fArr[0] + event.values[0];
            fArr = this.mGravities;
            fArr[1] = fArr[1] + event.values[1];
            fArr = this.mGravities;
            fArr[2] = fArr[2] + event.values[2];
            this.mSensorCnt++;
            return;
        }
        this.mGravities[0] = event.values[0];
        this.mGravities[1] = event.values[1];
        this.mGravities[2] = event.values[2];
        this.mSensorCnt = 1;
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void registGravitySensorListener() {
        this.mIsSensorAverage = false;
        this.mSensorCnt = 0;
        this.mGravities = new float[3];
        if (this.mSettings.use_gravity_sensor && this.mSensorManager != null) {
            List<Sensor> sensors = this.mSensorManager.getSensorList(9);
            if (sensors.size() > 0) {
                this.mSensorManager.registerListener(this, (Sensor) sensors.get(0), 2);
            }
        }
    }

    private void unregistGravitySensorListener() {
        if (this.mSettings.use_gravity_sensor && this.mSensorManager != null) {
            this.mSensorManager.unregisterListener(this);
        }
    }

    private void setInitialRotationByGravity() {
        if (this.mSettings.use_gravity_sensor && this.mMorphoPanoramaGP3 != null && this.mSensorCnt > 0) {
            LogFilter.d("Camera2App", String.format(Locale.US, "Gravity Sensor Value X=%f Y=%f Z=%f cnt=%d", new Object[]{Float.valueOf(this.mGravities[0] / ((float) this.mSensorCnt)), Float.valueOf(this.mGravities[1] / ((float) this.mSensorCnt)), Float.valueOf(this.mGravities[2] / ((float) this.mSensorCnt)), Integer.valueOf(this.mSensorCnt)}));
            if (this.mMorphoPanoramaGP3.setInitialRotationByGravity((double) x, (double) y, (double) z) != 0) {
                LogFilter.e("Camera2App", String.format(Locale.US, "MorphoPanoramaGP3.setInitialRotationByGravity error ret:0x%08X", new Object[]{Integer.valueOf(this.mMorphoPanoramaGP3.setInitialRotationByGravity((double) x, (double) y, (double) z))}));
            }
        }
    }

    private void setSensorFusionValue(CaptureImage image) {
        if (this.mMorphoPanoramaGP3 != null && this.mSensorFusion != null && this.mSettings.sensor_mode != 0) {
            int[] sensorIx = new int[4];
            if (this.mSensorFusion.getSensorMatrix(null, null, null, sensorIx) != 0) {
                LogFilter.e("Camera2App", String.format(Locale.US, "SensorFusion.getSensorMatrix error ret:0x%08X", new Object[]{Integer.valueOf(this.mSensorFusion.getSensorMatrix(null, null, null, sensorIx))}));
            }
            ArrayList<ArrayList<SensorData>> stock_data = this.mSensorFusion.getStockData();
            if (this.mSettings.save_input_images != 0) {
                int size;
                SensorInfoManager prevSensorInfo;
                SensorInfoManager currentSensorInfo = new SensorInfoManager(4);
                currentSensorInfo.g_ix = sensorIx[0];
                currentSensorInfo.r_ix = sensorIx[3];
                currentSensorInfo.a_ix = sensorIx[1];
                currentSensorInfo.img_ix = this.mMorphoPanoramaGP3.getAttachCount();
                currentSensorInfo.timeMillis = System.currentTimeMillis();
                currentSensorInfo.imageTimeStamp = image.getTimestamp();
                currentSensorInfo.sensitivity = image.getSensitivity();
                currentSensorInfo.exposureTime = image.getExposureTime();
                currentSensorInfo.rollingShutterSkew = image.getRollingShutterSkew();
                currentSensorInfo.sensorTimeStamp = image.getSensorTimeStamp();
                currentSensorInfo.sensorData[0] = (ArrayList) ((ArrayList) stock_data.get(0)).clone();
                currentSensorInfo.sensorData[3] = (ArrayList) ((ArrayList) stock_data.get(3)).clone();
                currentSensorInfo.sensorData[1] = (ArrayList) ((ArrayList) stock_data.get(1)).clone();
                if (currentSensorInfo.sensorData[0].isEmpty()) {
                    size = this.mSensorInfoManagerList.size();
                    if (size > 0) {
                        prevSensorInfo = (SensorInfoManager) this.mSensorInfoManagerList.get(size - 1);
                        currentSensorInfo.g_ix = prevSensorInfo.g_ix;
                        currentSensorInfo.sensorData[0] = prevSensorInfo.sensorData[0];
                    }
                }
                if (currentSensorInfo.sensorData[3].isEmpty()) {
                    size = this.mSensorInfoManagerList.size();
                    if (size > 0) {
                        prevSensorInfo = (SensorInfoManager) this.mSensorInfoManagerList.get(size - 1);
                        currentSensorInfo.r_ix = prevSensorInfo.r_ix;
                        currentSensorInfo.sensorData[3] = prevSensorInfo.sensorData[3];
                    }
                }
                if (currentSensorInfo.sensorData[1].isEmpty()) {
                    size = this.mSensorInfoManagerList.size();
                    if (size > 0) {
                        prevSensorInfo = (SensorInfoManager) this.mSensorInfoManagerList.get(size - 1);
                        currentSensorInfo.a_ix = prevSensorInfo.a_ix;
                        currentSensorInfo.sensorData[1] = prevSensorInfo.sensorData[1];
                    }
                }
                this.mCurrentSensorInfoManager = currentSensorInfo;
                this.mSensorInfoManagerList.add(currentSensorInfo);
            }
            int size2;
            if (this.mSettings.sensor_mode == 1 || this.mSettings.sensor_mode == 2) {
                long attachNum = this.mMorphoPanoramaGP3.getAttachCount();
                size2 = ((ArrayList) stock_data.get(0)).size();
                if (size2 > 0 && attachNum > 0 && this.mMorphoPanoramaGP3.setGyroscopeData((SensorData[]) ((ArrayList) stock_data.get(0)).toArray(new SensorData[size2])) != 0) {
                    LogFilter.e("Camera2App", String.format(Locale.US, "MorphoPanoramaGP3.setGyroscopeData error ret:0x%08X", new Object[]{Integer.valueOf(this.mMorphoPanoramaGP3.setGyroscopeData((SensorData[]) ((ArrayList) stock_data.get(0)).toArray(new SensorData[size2])))}));
                }
            } else if (this.mSettings.sensor_mode == 5) {
                size2 = ((ArrayList) stock_data.get(3)).size();
                if (size2 > 0) {
                    if (this.mMorphoPanoramaGP3.setRotationVector(((SensorData) ((ArrayList) stock_data.get(3)).get(size2 - 1)).mValues) != 0) {
                        LogFilter.e("Camera2App", String.format(Locale.US, "MorphoPanoramaGP3.setRotationVector error ret:0x%08X", new Object[]{Integer.valueOf(this.mMorphoPanoramaGP3.setRotationVector(((SensorData) ((ArrayList) stock_data.get(3)).get(size2 - 1)).mValues))}));
                    }
                }
            }
            this.mSensorFusion.clearStockData();
        }
    }

    private int getDisplayRotation() {
        switch (this.mActivity.getWindowManager().getDefaultDisplay().getRotation()) {
            case 0:
                return 0;
            case 1:
                return 90;
            case 2:
                return MediaProviderUtils.ROTATION_180;
            case 3:
                return MediaProviderUtils.ROTATION_270;
            default:
                return 0;
        }
    }

    private void updatedOrientation(int orientation) {
        boolean bSetOrientation = false;
        orientation += getDisplayRotation();
        if (this.mCurOrientation == -1) {
            bSetOrientation = true;
        } else {
            int curDir = (((this.mCurOrientation + 45) / 90) * 90) % 360;
            if (curDir != (((orientation + 45) / 90) * 90) % 360 && Math.abs(orientation - curDir) > 60) {
                bSetOrientation = true;
            }
        }
        if (bSetOrientation) {
            this.mCurOrientation = (((orientation + 45) / 90) * 90) % 360;
            updateViewRotation();
        }
    }

    private void updateViewRotation() {
        if (getActionButtonsFragment() != null) {
            getActionButtonsFragment().rotateView(360 - this.mCurOrientation);
        }
    }

    public boolean isTvLock() {
        Switch s = (Switch) this.mRootView.findViewById(R.id.tv_lock);
        return s == null ? false : s.isChecked();
    }

    public boolean useOis() {
        return this.mSettings.use_ois;
    }

    public int getAntiBanding() {
        return this.mSettings.getAntiBanding();
    }

    public int getCaptureMode() {
        return this.mSettings.getCaptureMode();
    }

    public int getColorCorrectionMode() {
        return this.mSettings.color_correction_mode;
    }

    public int getEdgeMode() {
        return this.mSettings.edge_mode;
    }

    public int getNoiseReductionMode() {
        return this.mSettings.noise_reduction_mode;
    }

    public int getShadingMode() {
        return this.mSettings.shading_mode;
    }

    public int getTonemapMode() {
        return this.mSettings.tonemap_mode;
    }

    public int getAntiFlickerMode() {
        int mode = this.mSettings.anti_flicker_mode;
        if (mode == 1) {
            return mode;
        }
        switch (this.mSettings.anti_flicker_freq) {
            case 1:
                return 2;
            case 2:
                return 3;
            default:
                return 0;
        }
    }

    public boolean isAutoAELock() {
        return this.mSettings.auto_ae_lock;
    }

    public boolean isAutoWBLock() {
        return this.mSettings.auto_wb_lock;
    }

    public boolean isAutoEdgeNR() {
        return this.mSettings.nr_auto;
    }

    public void onPreviewStart() {
        this.mMiniPreviewImageView.setVisibility(8);
        int i = 4;
        this.mWarningTextView.setVisibility(4);
        this.mWarningMessage.setVisibility(4);
        if (getActionButtonsFragment() != null) {
            getActionButtonsFragment().setShutterIcon();
        }
        if (getCamera2ParamsFragment().tvAll()) {
            if (((Switch) this.mRootView.findViewById(R.id.tv_auto_lock)).isChecked()) {
                ((Switch) this.mRootView.findViewById(R.id.tv_lock)).setChecked(this.mIsTvLock);
            }
            getCamera2ParamsFragment().tvSimple();
        }
        if (this.mOrientationEventListener != null) {
            this.mOrientationEventListener.enable();
        }
        boolean is_full = (this.mMorphoCamera.cameraInfo().getHardwareLevel() == 2 || this.mMorphoCamera.cameraInfo().getHardwareLevel() == 0) ? false : true;
        if (getActionButtonsFragment() != null) {
            getActionButtonsFragment().setEnabledCamera2Setting(is_full);
        }
        CameraInfoViewFragment fragment = (CameraInfoViewFragment) this.mActivity.getFragmentManager().findFragmentById(R.id.camera_info_view);
        if (fragment != null) {
            View view = fragment.getView();
            if (view != null) {
                if (is_full) {
                    i = 0;
                }
                view.setVisibility(i);
            }
        }
        if (this.mSensorFusion != null) {
            this.mSensorFusion.clearStockData();
        }
        if (this.mNeedToSet30Fps) {
            this.mNeedToSet30Fps = false;
            if (getCamera2ParamsFragment().auto()) {
                getCamera2ParamsFragment().setAutoMode30fps();
            }
        }
    }

    public void playAutoFocusSound() {
    }

    public void requestUiRunnable(Runnable action) {
        this.mActivity.runOnUiThread(action);
    }

    public void onTakePicturePreprocess() {
        this.mOrientationEventListener.disable();
        CameraInfo info = MorphoCamera1.getCameraInfo(this.mSettings.camera_id);
        int degrees = getDisplayRotation();
        int orientation = info.orientation;
        if (!this.mIsFrontCamera) {
            this.mInitParam.output_rotation = (((this.mCurOrientation + degrees) + orientation) + 360) % 360;
        } else if (this.mCurOrientation == info.orientation) {
            this.mInitParam.output_rotation = 0;
        } else {
            this.mInitParam.output_rotation = (((this.mCurOrientation + degrees) + orientation) + 360) % 360;
        }
    }

    public void onTakePictureStart(IPanoramaStateEventListener listener) {
        this.back_tophoto.setVisibility(8);
        this.mRootView.findViewById(R.id.camera_param_frame).setVisibility(4);
        this.mSensorSensitivityAverageManager.init();
        int[] iArr = this.mSensorSensitivity;
        this.mSensorSensitivity[1] = 0;
        iArr[0] = 0;
        long[] jArr = this.mExposureTime;
        this.mExposureTime[1] = 0;
        jArr[0] = 0;
        this.mPanoramaState = new PanoramaInit(this, null);
        this.mPanoramaState.setPanoramaStateEventListener(listener);
        initAttachQueue();
        this.mSensorInfoManagerList.clear();
        getActionButtonsFragment().setPauseIcon();
        if (getCamera2ParamsFragment().tvAll() && ((Switch) this.mRootView.findViewById(R.id.tv_auto_lock)).isChecked()) {
            this.mIsTvLock = ((Switch) this.mRootView.findViewById(R.id.tv_lock)).isChecked();
            ((Switch) this.mRootView.findViewById(R.id.tv_lock)).setChecked(true);
        }
        this.mRootView.findViewById(R.id.anti_flicker_mode_layout).setVisibility(4);
    }

    public boolean onTakePictureFinish() {
        if (this.mDirectionFunction.enabled()) {
            this.mDirectionFunction.requestQuit();
            return true;
        }
        synchronized (CameraConstants.EngineSynchronizedObject) {
            setAttachExit();
        }
        this.mActivity.runOnUiThread(new Runnable() {
            public void run() {
                Camera2App.this.mRootView.findViewById(R.id.mini_preview_guide).setVisibility(4);
                Camera2App.this.mMiniPreviewImageView.setVisibility(4);
            }
        });
        return false;
    }

    public void onTakePictureFinish2NextState(int mResultCode) {
        this.back_tophoto.setVisibility(0);
        this.mRoundDetector.stop();
        if (mResultCode != 0) {
            int title = 0;
            int msg = 0;
            switch (mResultCode) {
                case -2:
                    title = R.string.panoramagp3_HEIGHT_OVER_TITLE;
                    msg = R.string.panoramagp3_HEIGHT_OVER_MESSAGE;
                    break;
                case -1:
                    title = R.string.panoramagp3_NO_EFFECTIVE_PIXEL_TITLE;
                    msg = R.string.panoramagp3_NO_EFFECTIVE_PIXEL_MESSAGE;
                    break;
            }
            if (title != 0) {
                new AlertDialog.Builder(this.mActivity.getApplicationContext()).setCancelable(false).setTitle(this.mActivity.getResources().getString(title)).setMessage(this.mActivity.getResources().getString(msg)).setPositiveButton(R.string.panoramagp3_OK, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int whichButton) {
                    }
                }).show();
            }
        } else if (this.mSettings.save_input_images == 0) {
            boolean isPanoramaSave = this.mPanoramaState.hasImage();
            SavePictureState newState = new SavePictureState(isPanoramaSave);
            this.mMorphoCamera.setDefaultCameraState();
            if (isPanoramaSave) {
                this.mPanoramaState = new PanoramaState();
            }
            new SaveThread(newState).start();
            return;
        } else {
            if (this.mSettings.save_input_images != 0) {
                this.mMorphoCamera.setDefaultCameraState();
                new SaveThread(new SaveSensorState()).start();
                LogFilter.i("Camera2App", "Save sensor start.");
            }
            this.mPanoramaState = new PanoramaState();
        }
        if (this.mInputSaveState.isEnabled()) {
            this.mInputSaveState.putParamFile(this.mInputFolderPath, this.mMorphoCamera.cameraInfo(), this.mMorphoPanoramaGP3.getInputImageFormat(), getCamera2ParamsFragment().sensorSensitivity(), getCamera2ParamsFragment().shutterSpeed(), getCamera2ParamsFragment().fps());
        }
        finishEngine();
        setAttachExit();
        this.mMorphoCamera.cameraState().onStart();
    }

    public void onTakePictureCancel() {
        this.back_tophoto.setVisibility(0);
        int ret = Error.ERROR_UNKNOWN;
        synchronized (CameraConstants.EngineSynchronizedObject) {
            if (this.mMorphoPanoramaGP3 != null) {
                ret = this.mMorphoPanoramaGP3.end(2, Camera2ParamsFragment.TARGET_EV);
            }
            if (ret != 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("mMorphoPanoramaGP3.end error ret:");
                stringBuilder.append(ret);
                LogFilter.e("Camera2App", stringBuilder.toString());
            }
        }
        releaseImageBitmap();
        if (this.mSettings.save_input_images == 2 || this.mSettings.save_input_images == 1) {
            finalizeEncoder();
        }
        this.mRoundDetector.stop();
        finishEngine();
        setAttachExit();
        this.mPanoramaState = new PanoramaState();
    }

    public void onAttachEnd() {
    }

    public void setGravitySensorListener(boolean register) {
        if (register) {
            registGravitySensorListener();
        } else {
            unregistGravitySensorListener();
        }
    }

    public void setUnsharpStrength(int strength) {
    }

    public void setEdgeMode(int mode) {
        this.mShotSettings.edgeMode = mode;
    }

    public void setNoiseReductionMode(int mode) {
        this.mShotSettings.noiseReductionMode = mode;
    }

    public boolean isInfinityFocus() {
        return this.mSettings.isInfinityFocus();
    }

    public void updateTvValue() {
        if (getCamera2ParamsFragment().tvAll()) {
            ((MorphoCamera) this.mMorphoCamera).updateTvValue();
        }
    }

    public void updateCameraState(CameraState newState) {
        this.mMorphoCamera.updateCameraState(newState);
    }

    public void onOpened() {
        String val;
        this.mNeedToSet30Fps = true;
        if (this.mMiniPreviewImageView == null) {
            this.mMiniPreviewImageView = new ImageView(this.mActivity.getApplicationContext());
            if (this.mUseCamera1) {
                this.mMiniPreviewImageView.setScaleType(ScaleType.FIT_CENTER);
            } else {
                this.mMiniPreviewImageView.setScaleType(ScaleType.MATRIX);
            }
            this.pano_view.addView(this.mMiniPreviewImageView);
        }
        this.mRootView.findViewById(R.id.mini_preview_frame).getGlobalVisibleRect(new Rect());
        configureTransform();
        this.mCamera2ImageQualitySettings[0].entries = this.mMorphoCamera.getAvailableColorCorrectionMode();
        this.mCamera2ImageQualitySettings[0].entryValues = this.mMorphoCamera.getAvailableColorCorrectionModeValues();
        this.mCamera2ImageQualitySettings[0].defaultValues = this.mMorphoCamera.getColorCorrectionModeDefaultValues();
        this.mCamera2ImageQualitySettings[1].entries = this.mMorphoCamera.getAvailableEdgeMode();
        this.mCamera2ImageQualitySettings[1].entryValues = this.mMorphoCamera.getAvailableEdgeModeValues();
        this.mCamera2ImageQualitySettings[1].defaultValues = this.mMorphoCamera.getEdgeModeDefaultValues();
        this.mCamera2ImageQualitySettings[2].entries = this.mMorphoCamera.getAvailableNoiseReductionMode();
        this.mCamera2ImageQualitySettings[2].entryValues = this.mMorphoCamera.getAvailableNoiseReductionModeValues();
        this.mCamera2ImageQualitySettings[2].defaultValues = this.mMorphoCamera.getNoiseReductionModeDefaultValues();
        this.mCamera2ImageQualitySettings[3].entries = this.mMorphoCamera.getAvailableShadingMode();
        this.mCamera2ImageQualitySettings[3].entryValues = this.mMorphoCamera.getAvailableShadingModeValues();
        this.mCamera2ImageQualitySettings[3].defaultValues = this.mMorphoCamera.getShadingModeDefaultValues();
        this.mCamera2ImageQualitySettings[4].entries = this.mMorphoCamera.getAvailableTonemapMode();
        this.mCamera2ImageQualitySettings[4].entryValues = this.mMorphoCamera.getAvailableTonemapModeValues();
        this.mCamera2ImageQualitySettings[4].defaultValues = this.mMorphoCamera.getTonemapModeDefaultValues();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this.mActivity.getApplicationContext());
        Editor editor = sp.edit();
        if (this.mCamera2ImageQualitySettings[0].isAvailable() && this.mSettings.color_correction_mode == Integer.parseInt("-1")) {
            String val2 = this.mCamera2ImageQualitySettings[0].getDefaultValue(this.mSettings.getCaptureMode());
            this.mSettings.color_correction_mode = Integer.parseInt(val2);
            editor.putString(this.mActivity.getString(R.string.KEY_COLOR_CORRECTION_MODE), val2);
            editor.apply();
        }
        if (this.mCamera2ImageQualitySettings[1].isAvailable() && this.mSettings.edge_mode == Integer.parseInt("-1")) {
            val = this.mCamera2ImageQualitySettings[1].getDefaultValue(this.mSettings.getCaptureMode());
            this.mSettings.edge_mode = Integer.parseInt(val);
            editor.putString(this.mActivity.getString(R.string.KEY_EDGE_MODE), val);
            editor.apply();
        }
        if (this.mCamera2ImageQualitySettings[2].isAvailable() && this.mSettings.noise_reduction_mode == Integer.parseInt("-1")) {
            val = this.mCamera2ImageQualitySettings[2].getDefaultValue(this.mSettings.getCaptureMode());
            this.mSettings.noise_reduction_mode = Integer.parseInt(val);
            editor.putString(this.mActivity.getString(R.string.KEY_NOISE_REDUCTION_MODE), val);
            editor.apply();
        }
        if (this.mCamera2ImageQualitySettings[3].isAvailable() && this.mSettings.shading_mode == Integer.parseInt("-1")) {
            val = this.mCamera2ImageQualitySettings[3].getDefaultValue(this.mSettings.getCaptureMode());
            this.mSettings.shading_mode = Integer.parseInt(val);
            editor.putString(this.mActivity.getString(R.string.KEY_SHADING_MODE), val);
            editor.apply();
        }
        if (this.mCamera2ImageQualitySettings[4].isAvailable() && this.mSettings.tonemap_mode == Integer.parseInt("-1")) {
            val = this.mCamera2ImageQualitySettings[4].getDefaultValue(this.mSettings.getCaptureMode());
            this.mSettings.tonemap_mode = Integer.parseInt(val);
            editor.putString(this.mActivity.getString(R.string.KEY_TONEMAP_MODE), val);
            editor.apply();
        }
        if (getCamera2ParamsFragment().auto()) {
            this.mCamera2ParamsFragmentSelectedMode = -1;
        } else {
            this.mCamera2ParamsFragmentSelectedMode = getCamera2ParamsFragment().getSelectedMode();
            getCamera2ParamsFragment().setAuto();
            getCamera2ParamsFragment().setEnabled(false);
            this.mHandler.postDelayed(new Runnable() {
                public void run() {
                    Camera2App.this.revertCamera2ParamsFragmentMode();
                }
            }, 1000);
        }
        updateHardwareLevel(sp);
        updateTimeStampSource(sp);
        this.gridLines.onPreviewAreaChanged(new RectF(0.0f, 0.0f, (float) this.mTextureView.getWidth(), (float) this.mTextureView.getHeight()));
        if (Keys.areGridLinesOn(this.mActivity.getSettingsManager())) {
            this.gridLines.setVisibility(0);
        } else {
            this.gridLines.setVisibility(4);
        }
    }

    private void updateHardwareLevel(SharedPreferences sp) {
        this.mHardwareLevel = this.mMorphoCamera.getHardwareLevel();
        Editor editor = sp.edit();
        editor.putString(this.mActivity.getString(R.string.KEY_HARDWARE_LEVEL), this.mHardwareLevel);
        editor.apply();
    }

    private void updateTimeStampSource(SharedPreferences sp) {
        this.mTimestampSource = this.mMorphoCamera.getTimestampSource();
        Editor editor = sp.edit();
        editor.putString(this.mActivity.getString(R.string.KEY_TIMESTAMP_SOURCE), this.mTimestampSource);
        editor.apply();
    }

    /* JADX WARNING: Missing block: B:22:0x0054, code skipped:
            return;
     */
    private synchronized void revertCamera2ParamsFragmentMode() {
        /*
        r2 = this;
        monitor-enter(r2);
        r0 = r2.mActivity;	 Catch:{ all -> 0x0055 }
        r0 = r0.isFinishing();	 Catch:{ all -> 0x0055 }
        if (r0 != 0) goto L_0x0053;
    L_0x0009:
        r0 = r2.mActivity;	 Catch:{ all -> 0x0055 }
        r0 = r0.isDestroyed();	 Catch:{ all -> 0x0055 }
        if (r0 == 0) goto L_0x0012;
    L_0x0011:
        goto L_0x0053;
    L_0x0012:
        r0 = r2.getCamera2ParamsFragment();	 Catch:{ all -> 0x0055 }
        if (r0 != 0) goto L_0x001a;
    L_0x0018:
        monitor-exit(r2);
        return;
    L_0x001a:
        r0 = r2.mCamera2ParamsFragmentSelectedMode;	 Catch:{ all -> 0x0055 }
        r1 = 1;
        switch(r0) {
            case 1: goto L_0x003f;
            case 2: goto L_0x0030;
            case 3: goto L_0x0021;
            default: goto L_0x0020;
        };	 Catch:{ all -> 0x0055 }
    L_0x0020:
        goto L_0x004e;
    L_0x0021:
        r0 = r2.getCamera2ParamsFragment();	 Catch:{ all -> 0x0055 }
        r0.setTvSimple();	 Catch:{ all -> 0x0055 }
        r0 = r2.getCamera2ParamsFragment();	 Catch:{ all -> 0x0055 }
        r0.setEnabled(r1);	 Catch:{ all -> 0x0055 }
        goto L_0x004e;
    L_0x0030:
        r0 = r2.getCamera2ParamsFragment();	 Catch:{ all -> 0x0055 }
        r0.setManual();	 Catch:{ all -> 0x0055 }
        r0 = r2.getCamera2ParamsFragment();	 Catch:{ all -> 0x0055 }
        r0.setEnabled(r1);	 Catch:{ all -> 0x0055 }
        goto L_0x004e;
    L_0x003f:
        r0 = r2.getCamera2ParamsFragment();	 Catch:{ all -> 0x0055 }
        r0.setTv();	 Catch:{ all -> 0x0055 }
        r0 = r2.getCamera2ParamsFragment();	 Catch:{ all -> 0x0055 }
        r0.setEnabled(r1);	 Catch:{ all -> 0x0055 }
    L_0x004e:
        r0 = -1;
        r2.mCamera2ParamsFragmentSelectedMode = r0;	 Catch:{ all -> 0x0055 }
        monitor-exit(r2);
        return;
    L_0x0053:
        monitor-exit(r2);
        return;
    L_0x0055:
        r0 = move-exception;
        monitor-exit(r2);
        throw r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.morphoinc.app.panoramagp3.Camera2App.revertCamera2ParamsFragmentMode():void");
    }

    public void onCaptureCompleted(CaptureRequest request, TotalCaptureResult result) {
        CameraInfo cameraInfo = this.mMorphoCamera.cameraInfo();
        cameraInfo.setCaptureWidth(2400);
        cameraInfo.setCaptureHeight(1800);
        if (cameraInfo.getHardwareLevel() != 2) {
            cameraInfo.getHardwareLevel();
        }
    }

    public boolean onPreviewImageAvailable() {
        this.mPreviewFrame.setVisibility(4);
        this.mCurPreviewFrame.setVisibility(4);
        this.mPreviewArrow.setVisibility(4);
        this.mPreviewLine1.setVisibility(4);
        this.mPreviewLine2.setVisibility(4);
        this.mPreviewLine3.setVisibility(4);
        if (PREVIEW_SPREAD_BOTH_SIDES) {
            this.mPreviewImageView.setTranslationX(-999999.0f);
        }
        if (isEngineRunning()) {
            return false;
        }
        return true;
    }

    public void onPreviewImage(byte[] data) {
        byte[] bArr = data;
        this.mPreviewFrame.setVisibility(4);
        this.mCurPreviewFrame.setVisibility(4);
        this.mPreviewArrow.setVisibility(4);
        this.mPreviewLine1.setVisibility(4);
        this.mPreviewLine2.setVisibility(4);
        this.mPreviewLine3.setVisibility(4);
        if (PREVIEW_SPREAD_BOTH_SIDES) {
            this.mPreviewImageView.setTranslationX(-999999.0f);
        }
        if (!(isEngineRunning() || this.mMiniPreviewImageView == null)) {
            CameraInfo info = MorphoCamera1.getCameraInfo(this.mSettings.camera_id);
            int rotation = getDisplayRotation();
            int degrees = ((info.orientation + rotation) + 360) % 360;
            if (!(this.mMiniPreviewBitmapForCamera1 != null && this.mMiniPreviewBitmapForCamera1.getWidth() == (this.mMiniPreviewImageView.getWidth() & -2) && this.mMiniPreviewBitmapForCamera1.getHeight() == (this.mMiniPreviewImageView.getHeight() & -2))) {
                this.mMiniPreviewBitmapForCamera1 = Bitmap.createBitmap(this.mMiniPreviewImageView.getWidth() & -2, this.mMiniPreviewImageView.getHeight() & -2, Config.ARGB_8888);
            }
            CameraInfo cameraInfo = this.mMorphoCamera.cameraInfo();
            cameraInfo.setCaptureWidth(2400);
            cameraInfo.setCaptureHeight(1800);
            if ("YUV420_PLANAR".equals(this.mImageFormat)) {
                if (MorphoPanoramaGP3.yuv2Bitmap8888(bArr, cameraInfo.getCaptureWidth(), cameraInfo.getCaptureHeight(), this.mMiniPreviewBitmapForCamera1, degrees) != 0) {
                    LogFilter.e("Camera2App", String.format(Locale.US, "MorphoPanoramaGP3.yuv2Bitmap8888 error ret:0x%08X", new Object[]{Integer.valueOf(MorphoPanoramaGP3.yuv2Bitmap8888(bArr, cameraInfo.getCaptureWidth(), cameraInfo.getCaptureHeight(), this.mMiniPreviewBitmapForCamera1, degrees))}));
                }
            } else {
                if (MorphoPanoramaGP3.yvu2Bitmap8888(bArr, cameraInfo.getCaptureWidth(), cameraInfo.getCaptureHeight(), this.mMiniPreviewBitmapForCamera1, degrees) != 0) {
                    LogFilter.e("Camera2App", String.format(Locale.US, "MorphoPanoramaGP3.yvu2Bitmap8888 error ret:0x%08X", new Object[]{Integer.valueOf(MorphoPanoramaGP3.yvu2Bitmap8888(bArr, cameraInfo.getCaptureWidth(), cameraInfo.getCaptureHeight(), this.mMiniPreviewBitmapForCamera1, degrees))}));
                }
            }
            if (this.mIsFrontCamera) {
                Matrix mirrorMatrix = new Matrix();
                mirrorMatrix.preScale(-1.0f, 1.0f);
                this.mMiniPreviewImageView.setImageBitmap(Bitmap.createBitmap(this.mMiniPreviewBitmapForCamera1, 0, 0, this.mMiniPreviewBitmapForCamera1.getWidth(), this.mMiniPreviewBitmapForCamera1.getHeight(), mirrorMatrix, true));
            } else {
                this.mMiniPreviewImageView.setImageBitmap(this.mMiniPreviewBitmapForCamera1);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mCurOrientation:");
            stringBuilder.append(this.mCurOrientation);
            LogFilter.d("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("info.orientation:");
            stringBuilder.append(info.orientation);
            LogFilter.d("Camera2App", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("rotation:");
            stringBuilder.append(rotation);
            LogFilter.d("Camera2App", stringBuilder.toString());
            LogFilter.d("Camera2App", String.format(Locale.US, "PreviewImage(%dx%d)", new Object[]{Integer.valueOf(this.mMiniPreviewBitmapForCamera1.getWidth()), Integer.valueOf(this.mMiniPreviewBitmapForCamera1.getHeight())}));
        }
    }

    public boolean onPictureTaken(CaptureImage image) {
        this.mPreviewFrame.setVisibility(0);
        if (this.mPreviewLine1.getVisibility() != 0) {
            Rect preview_rect = new Rect();
            this.mPreviewFrame.getGlobalVisibleRect(preview_rect);
            if (preview_rect.width() > 0) {
                int direction = this.mDirectionFunction.getDirection();
                boolean is_horizontal = true;
                if (this.mInitParam.output_rotation == 0 || this.mInitParam.output_rotation == MediaProviderUtils.ROTATION_180) {
                    if (direction == 0 || direction == 1) {
                        is_horizontal = false;
                    }
                } else if (direction == 3 || direction == 2) {
                    is_horizontal = false;
                }
                LayoutParams lp1;
                LayoutParams lp2;
                if (is_horizontal) {
                    lp1 = new LayoutParams(-1, 2);
                    lp1.setMargins(0, this.mPreviewFrame.getTop(), 0, 0);
                    this.mPreviewLine1.setLayoutParams(lp1);
                    lp2 = new LayoutParams(-1, 2);
                    lp2.setMargins(0, this.mPreviewFrame.getBottom(), 0, 0);
                    this.mPreviewLine2.setLayoutParams(lp2);
                } else {
                    lp1 = new LayoutParams(2, -1);
                    lp1.setMargins(preview_rect.left, 0, 0, 0);
                    this.mPreviewLine1.setLayoutParams(lp1);
                    lp2 = new LayoutParams(2, -1);
                    lp2.setMargins(preview_rect.right, 0, 0, 0);
                    this.mPreviewLine2.setLayoutParams(lp2);
                }
                this.mPreviewLine1.setVisibility(0);
                this.mPreviewLine2.setVisibility(0);
                this.mPreviewLine3.setVisibility(0);
            }
        }
        if (!this.mPanoramaState.onSaveImage(image)) {
            this.mPanoramaState = new PanoramaState();
            this.mMorphoCamera.cameraState().onCancel();
        }
        switch (this.mSettings.getCaptureMode()) {
            case 1:
            case 2:
                this.mPanoramaState.repeatTakePicture();
                break;
            case 3:
                int burstRemaining = this.mMorphoCamera.burstRemaining();
                if (burstRemaining <= 1) {
                    this.mPanoramaState.repeatTakePicture();
                    break;
                }
                this.mMorphoCamera.setBurstRemaining(burstRemaining - 1);
                break;
        }
        return true;
    }

    private void initializeEncoder() {
        if (Environment.getExternalStorageState().equals("mounted")) {
            String path = new StringBuilder();
            path.append(this.mInputFolderPath);
            path.append(File.separator);
            path.append("input.mp4");
            int ceil = (int) Math.ceil((((double) (this.mInitParam.input_width * this.mInitParam.input_height)) * this.mSettings.input_movie_fps) * 0.4d);
            this.mRecorder = new VideoRecorderRaw(this.mInitParam.input_width, this.mInitParam.input_height, ceil, (float) this.mSettings.input_movie_fps, this.mInitParam.input_format, path.toString());
            this.mRecordTimestampStart = 0;
        }
    }

    private void finalizeEncoder() {
        synchronized (mRecoderLock) {
            if (this.mRecorder != null) {
                this.mRecorder.end();
                scanFile(new File(this.mRecorder.getOutputFilePath()));
                this.mRecorder = null;
            }
            this.mRecordTimestampStart = 0;
        }
    }

    private void copyBuildProp() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.mInputFolderPath);
        stringBuilder.append(File.separator);
        stringBuilder.append("build.prop");
        File dst = new File(stringBuilder.toString());
        if (BuildPropJNI.getBuildProp(dst.getAbsolutePath())) {
            scanFile(dst);
        } else if (dst.exists()) {
            dst.delete();
        }
    }

    private void encodeFrame(CaptureImage image) {
        synchronized (mRecoderLock) {
            if (this.mRecorder != null) {
                long timestampNanos;
                if (this.mRecordTimestampStart == 0) {
                    timestampNanos = 0;
                    this.mRecordTimestampStart = image.getTimestamp();
                } else {
                    timestampNanos = image.getTimestamp() - this.mRecordTimestampStart;
                }
                this.mRecorder.encodeFrameAsync(image, timestampNanos / 1000, this.mRawRenderListener2);
            }
        }
    }

    private void layoutMiniPreview() {
        int h;
        int w;
        CameraInfo cameraInfo = this.mMorphoCamera.cameraInfo();
        cameraInfo.setCaptureWidth(2400);
        cameraInfo.setCaptureHeight(1800);
        int orientation = this.mActivity.getResources().getConfiguration().orientation;
        DisplayMetrics metrics = new DisplayMetrics();
        this.mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        View miniPreview = this.mRootView.findViewById(R.id.mini_preview_frame);
        int base_size = Math.round(74.0f * metrics.scaledDensity);
        float scale = ((float) cameraInfo.getCaptureWidth()) / ((float) cameraInfo.getCaptureHeight());
        if (orientation == 1) {
            h = Math.round(((float) base_size) * PREVIEW_LONG_SIDE_CROP_RATIO);
            w = Math.round(((float) base_size) / scale);
        } else {
            w = Math.round(((float) base_size) * PREVIEW_LONG_SIDE_CROP_RATIO);
            h = Math.round(((float) base_size) / scale);
        }
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(w, h);
        lp.addRule(13, -1);
        miniPreview.setLayoutParams(lp);
    }

    private boolean readViewAngle() {
        SharedPreferences sp = this.mActivity.getSharedPreferences(PREF_KEY, 0);
        if (sp.getInt(PREF_KEY_VIEW_ANGLE_CAMERA_ID, -1) != this.mSettings.camera_id) {
            return false;
        }
        float angleH = sp.getFloat(PREF_KEY_VIEW_ANGLE_H, 0.0f);
        float angleV = sp.getFloat(PREF_KEY_VIEW_ANGLE_V, 0.0f);
        if (angleH == 0.0f || angleV == 0.0f) {
            return false;
        }
        this.mViewAngleH = angleH;
        this.mViewAngleV = angleV;
        return true;
    }

    private void writeViewAngle(int camera_id, float h, float v) {
        Editor editor = this.mActivity.getSharedPreferences(PREF_KEY, 0).edit();
        editor.putInt(PREF_KEY_VIEW_ANGLE_CAMERA_ID, camera_id);
        editor.putFloat(PREF_KEY_VIEW_ANGLE_H, h);
        editor.putFloat(PREF_KEY_VIEW_ANGLE_V, v);
        editor.apply();
    }

    private void scanFile(File file) {
        scanFile(this.mActivity.getBaseContext(), file);
    }

    public static void scanFile(Context context, File file) {
        context.sendBroadcast(new Intent("android.intent.action.MEDIA_SCANNER_SCAN_FILE", Uri.fromFile(file)));
    }

    private void encodeMovie(CaptureImage image) {
        long intervalNanos = image.getTimestamp() - this.mRecordTimestampStart;
        double fpsNano = 1.0E9d / this.mSettings.input_movie_fps;
        if (this.mSettings.save_input_images == 2 || ((double) this.mNumEncodedFrames) * fpsNano <= ((double) intervalNanos)) {
            encodeFrame(image);
            this.mNumEncodedFrames++;
        }
    }

    private void updatePeekThumb() {
        Log.w("Camera2App", "[Camera2App] updatePeekThumb ");
        new AsyncTask<Void, Void, Thumbnail>() {
            /* Access modifiers changed, original: protected */
            public void onPreExecute() {
                super.onPreExecute();
                Log.w("Camera2App", "[Camera2App] update media,onPreExecute");
            }

            /* Access modifiers changed, original: protected */
            public Thumbnail doInBackground(Void[] params) {
                Thumbnail[] thumb = new Thumbnail[1];
                Thumbnail.getLastThumbnailFromContentResolver(Camera2App.this.mActivity.getContentResolver(), thumb);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[Camera2App] update media,doInBackground:");
                stringBuilder.append(thumb[0]);
                Log.w("Camera2App", stringBuilder.toString());
                return thumb[0];
            }

            /* Access modifiers changed, original: protected */
            public void onPostExecute(Thumbnail o) {
                super.onPostExecute(o);
                Log.w("Camera2App", "[Camera2App] need to update Thumb");
                if (o == null || o.getBitmap() == null) {
                    Camera2App.this.getActionButtonsFragment().setThumbnailBitmap(null);
                    return;
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[Camera2App] latest thumbnail: ");
                stringBuilder.append(o.getUri());
                Log.w("Camera2App", stringBuilder.toString());
                Camera2App.this.getActionButtonsFragment().setThumbnailBitmap(o.getBitmap());
                stringBuilder = new StringBuilder();
                stringBuilder.append("peekThumbUpdated");
                stringBuilder.append(o.getBitmap().getWidth());
                stringBuilder.append("    ");
                stringBuilder.append(o.getBitmap().getHeight());
                Log.w("Camera2App", stringBuilder.toString());
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }
}
