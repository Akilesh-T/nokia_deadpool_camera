package com.hmdglobal.app.camera.rapidcapture;

import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.location.Location;
import android.media.CameraProfile;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import com.android.ex.camera2.portability.CameraAgent;
import com.android.ex.camera2.portability.CameraAgent.CameraOpenCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraPictureCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraPreviewDataCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.android.ex.camera2.portability.CameraAgent.CameraShutterCallback;
import com.android.ex.camera2.portability.CameraAgent.CameraStartPreviewCallback;
import com.android.ex.camera2.portability.CameraAgentFactory;
import com.android.ex.camera2.portability.CameraAgentFactory.CameraApi;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraCapabilities.Feature;
import com.android.ex.camera2.portability.CameraExceptionHandler;
import com.android.ex.camera2.portability.CameraExceptionHandler.CameraExceptionCallback;
import com.android.ex.camera2.portability.CameraSettings;
import com.android.ex.camera2.portability.CameraSettings.BoostParameters;
import com.android.ex.camera2.portability.Size;
import com.hmdglobal.app.camera.Exif;
import com.hmdglobal.app.camera.PhotoModule.NamedImages;
import com.hmdglobal.app.camera.PhotoModule.NamedImages.NamedEntity;
import com.hmdglobal.app.camera.app.CameraController;
import com.hmdglobal.app.camera.app.CameraServices;
import com.hmdglobal.app.camera.app.LocationManager;
import com.hmdglobal.app.camera.app.MediaSaver.OnMediaSavedListener;
import com.hmdglobal.app.camera.app.OrientationManager.OnOrientationChangeListener;
import com.hmdglobal.app.camera.app.OrientationManagerImpl;
import com.hmdglobal.app.camera.exif.ExifInterface;
import com.hmdglobal.app.camera.settings.CameraPictureSizesCacher;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.settings.SettingsUtil;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.hmdglobal.app.camera.util.ExternalExifInterface;
import com.hmdglobal.app.camera.util.GservicesHelper;
import com.morphoinc.utils.multimedia.MediaProviderUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RapidCaptureHelper implements CameraOpenCallback, OnOrientationChangeListener {
    private static final long AEC_SETTLE_TIMEOUT = 1000;
    private static final long BURSTSHOT_TIMEOUT = 60000;
    private static final int BURST_DELAY = 0;
    private static final int BURST_MAX = 10;
    private static final int MSG_AEC_SETTLE_TIMEOUT = 1;
    private static final int MSG_BURSTSHOT_TIMEOUT = 4;
    private static final int MSG_ONESHOT_TIMEOUT = 3;
    private static final int MSG_REMOVE_SCREEN_WAKELOCK = 2;
    private static final long ONESHOT_TIMEOUT = 10000;
    private static final long SCREEN_WAKELOCK_TIMEOUT = 3000;
    private static final String TAG = "RapidCaptureHelper";
    public static int TYPE_BURSTSHOT = 2;
    public static int TYPE_INVALID = 0;
    public static int TYPE_ONESHOT = 1;
    private static RapidCaptureHelper sInstance;
    private Runnable burstshot = new Runnable() {
        public void run() {
            String str = RapidCaptureHelper.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onShutter ");
            stringBuilder.append(RapidCaptureHelper.this.mBurstshotBreak);
            Log.i(str, stringBuilder.toString());
            if (RapidCaptureHelper.this.mBurstshotBreak || RapidCaptureHelper.this.mSnapshotBurstNum >= 10) {
                RapidCaptureHelper.this.mBurstshotBreak = true;
                if (RapidCaptureHelper.this.mBurstShotUris != null && RapidCaptureHelper.this.mBurstShotUris.size() == RapidCaptureHelper.this.mSnapshotBurstNum) {
                    RapidCaptureHelper.this.onCaptureDone((Uri) RapidCaptureHelper.this.mBurstShotUris.get(RapidCaptureHelper.this.mBurstShotUris.size() - 1));
                }
                return;
            }
            RapidCaptureHelper.this.mSnapshotBurstNum = RapidCaptureHelper.this.mSnapshotBurstNum + 1;
            RapidCaptureHelper.this.mCameraDevice.takePicture(RapidCaptureHelper.this.mMainHandler, RapidCaptureHelper.this.mLongshotShutterCallback, null, null, RapidCaptureHelper.this.mLongshotPictureTakenCallback);
        }
    };
    private long mAecsettledTime;
    private Context mAppContext;
    private Application mApplication;
    private ArrayList<Uri> mBurstShotUris;
    private boolean mBurstshotBreak = false;
    private WakeLock mCPUWakeLock;
    protected CameraCapabilities mCameraCapabilities;
    private CameraController mCameraController;
    private CameraProxy mCameraDevice;
    private final CameraExceptionCallback mCameraExceptionCallback = new CameraExceptionCallback() {
        public void onCameraError(int errorCode) {
            String str = RapidCaptureHelper.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Camera error callback. error=");
            stringBuilder.append(errorCode);
            Log.e(str, stringBuilder.toString());
            RapidCaptureHelper.this.mCameraFatalError = true;
            RapidCaptureHelper.this.onCaptureDone(null);
        }

        public void onCameraException(RuntimeException ex, String commandHistory, int action, int state) {
            Log.e(RapidCaptureHelper.TAG, "Camera Exception", ex);
            RapidCaptureHelper.this.mCameraFatalError = true;
            RapidCaptureHelper.this.onCaptureDone(null);
        }

        public void onDispatchThreadException(RuntimeException ex) {
            Log.e(RapidCaptureHelper.TAG, "DispatchThread Exception", ex);
            RapidCaptureHelper.this.mCameraFatalError = true;
            RapidCaptureHelper.this.onCaptureDone(null);
        }
    };
    private CameraExceptionHandler mCameraExceptionHandler;
    private boolean mCameraFatalError = false;
    private int mCameraId;
    private boolean mCameraPreviewParamsReady = false;
    private CameraSettings mCameraSettings;
    private long mCaptureStartTime;
    boolean mCaptureStarted = false;
    boolean mFirstFrameReceived = false;
    private long mFirtstFrameTime;
    private int mFrameCount;
    private boolean mInitialized;
    private long mJpegPictureCallbackTime;
    private int mJpegRotation;
    private LocationManager mLocationManager;
    private LongshotPictureCallback mLongshotPictureTakenCallback;
    private ShutterCallback mLongshotShutterCallback = new ShutterCallback(true);
    private Handler mMainHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Log.d(RapidCaptureHelper.TAG, "MSG_AEC_SETTLE_TIMEOUT");
                    if (RapidCaptureHelper.this.mCameraDevice != null) {
                        RapidCaptureHelper.this.mCameraDevice.setPreviewDataCallback(RapidCaptureHelper.this.mMainHandler, null);
                        RapidCaptureHelper.this.capture();
                        break;
                    }
                    Log.d(RapidCaptureHelper.TAG, "MSG_AEC_SETTLE_TIMEOUT and fail with timeout");
                    return;
                case 2:
                    Log.d(RapidCaptureHelper.TAG, "MSG_REMOVE_SCREEN_WAKELOCK");
                    RapidCaptureHelper.this.releaseScreenWakeLock();
                    break;
                case 3:
                    if (RapidCaptureHelper.this.mCameraDevice != null) {
                        RapidCaptureHelper.this.mCameraDevice.setPreviewDataCallback(RapidCaptureHelper.this.mMainHandler, null);
                    }
                    Log.d(RapidCaptureHelper.TAG, "MSG_ONESHOT_TIMEOUT");
                    RapidCaptureHelper.this.onCaptureDone(null);
                    break;
                case 4:
                    Log.d(RapidCaptureHelper.TAG, "MSG_BURSTSHOT_TIMEOUT");
                    if (RapidCaptureHelper.this.mCameraDevice != null) {
                        RapidCaptureHelper.this.mCameraDevice.setPreviewDataCallback(RapidCaptureHelper.this.mMainHandler, null);
                    }
                    RapidCaptureHelper.this.onCaptureDone(null);
                    break;
            }
        }
    };
    private NamedImages mNamedImages;
    private final OnMediaSavedListener mOnMediaSavedListener = new OnMediaSavedListener() {
        public void onMediaSaved(Uri uri) {
            String str = RapidCaptureHelper.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onMediaSaved  ");
            stringBuilder.append(uri);
            stringBuilder.append(", ");
            stringBuilder.append(RapidCaptureHelper.this.mCaptureStarted);
            Log.i(str, stringBuilder.toString());
            if (RapidCaptureHelper.this.mCaptureStarted) {
                if (uri != null) {
                    CameraUtil.broadcastNewPicture(RapidCaptureHelper.this.mAppContext, uri);
                }
                if (RapidCaptureHelper.this.mType == RapidCaptureHelper.TYPE_ONESHOT) {
                    RapidCaptureHelper.this.onCaptureDone(uri);
                } else if (RapidCaptureHelper.this.mType == RapidCaptureHelper.TYPE_BURSTSHOT) {
                    if (RapidCaptureHelper.this.mBurstShotUris == null) {
                        RapidCaptureHelper.this.mBurstShotUris = new ArrayList();
                    }
                    RapidCaptureHelper.this.mBurstShotUris.add(uri);
                    str = RapidCaptureHelper.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("onMediaSaved  ");
                    stringBuilder.append(RapidCaptureHelper.this.mBurstshotBreak);
                    stringBuilder.append(Size.DELIMITER);
                    stringBuilder.append(RapidCaptureHelper.this.mBurstShotUris.size());
                    stringBuilder.append(",  ");
                    stringBuilder.append(RapidCaptureHelper.this.mSnapshotBurstNum);
                    Log.i(str, stringBuilder.toString());
                    if (RapidCaptureHelper.this.mBurstshotBreak && RapidCaptureHelper.this.mBurstShotUris.size() == RapidCaptureHelper.this.mSnapshotBurstNum) {
                        RapidCaptureHelper.this.mCameraDevice.abortBurstShot();
                        RapidCaptureHelper.this.onCaptureDone(uri);
                    }
                }
            }
        }
    };
    private long mOneshotStartTime;
    private long mOneshotendTime;
    private int mOrientation = -1;
    private OrientationManagerImpl mOrientationManager;
    private WakeLock mScreenWakeLock;
    private Callback mServiceCallback;
    private SettingsManager mSettingsManager;
    private ShutterCallback mShutterCallback = new ShutterCallback(false);
    private long mShutterCallbackTime;
    private int mSnapshotBurstNum = 0;
    private SurfaceTexture mSurface;
    private int mType;
    private long mopenEndTime;
    private long mopenStartTime;
    private long mpreviewEndTime;
    private long mpreviewStartTime;

    public interface Callback {
        void onCaptureDone(boolean z);
    }

    private final class JpegPictureCallback implements CameraPictureCallback {
        Location mLocation;

        public JpegPictureCallback(Location loc) {
            this.mLocation = loc;
        }

        public void onPictureTaken(byte[] originalJpegData, CameraProxy camera) {
            Log.i(RapidCaptureHelper.TAG, "jpegCallback onPictureTaken");
            RapidCaptureHelper.this.mJpegPictureCallbackTime = System.currentTimeMillis();
            ExifInterface exif = Exif.getExif(originalJpegData);
            RapidCaptureHelper.this.saveFinalPhoto(originalJpegData, RapidCaptureHelper.this.mNamedImages.getNextNameEntity(), exif, camera, this.mLocation, null);
        }
    }

    private final class LongshotPictureCallback implements CameraPictureCallback {
        Location mLocation;
        private short mLongshotCount = (short) 0;

        public LongshotPictureCallback(Location loc) {
            this.mLocation = loc;
        }

        public void onPictureTaken(byte[] originalJpegData, CameraProxy camera) {
            String str = RapidCaptureHelper.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onPictureTaken");
            stringBuilder.append(RapidCaptureHelper.this.mBurstshotBreak);
            Log.d(str, stringBuilder.toString());
            ExifInterface exif = Exif.getExif(originalJpegData);
            NamedEntity name = RapidCaptureHelper.this.mNamedImages.getNextNameEntity();
            Map externalBundle = new HashMap();
            externalBundle.put(ExternalExifInterface.BURST_SHOT_ID, Integer.valueOf(hashCode()));
            String str2 = ExternalExifInterface.BURST_SHOT_INDEX;
            short s = this.mLongshotCount;
            this.mLongshotCount = (short) (s + 1);
            externalBundle.put(str2, Short.valueOf(s));
            RapidCaptureHelper.this.saveFinalPhoto(originalJpegData, name, exif, camera, this.mLocation, externalBundle);
        }
    }

    private final class ShutterCallback implements CameraShutterCallback {
        private boolean isFromLongshot = false;

        public ShutterCallback(boolean fromLongshot) {
            this.isFromLongshot = fromLongshot;
        }

        public void onShutter(CameraProxy camera) {
            RapidCaptureHelper.this.mShutterCallbackTime = System.currentTimeMillis();
            String str = RapidCaptureHelper.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onShutter ");
            stringBuilder.append(this.isFromLongshot);
            stringBuilder.append(", ");
            stringBuilder.append(RapidCaptureHelper.this.mBurstshotBreak);
            Log.i(str, stringBuilder.toString());
            if (this.isFromLongshot) {
                RapidCaptureHelper.this.mNamedImages.nameNewImage(RapidCaptureHelper.this.mShutterCallbackTime);
                RapidCaptureHelper.this.mMainHandler.postDelayed(RapidCaptureHelper.this.burstshot, 0);
            }
        }
    }

    private RapidCaptureHelper() {
    }

    public static RapidCaptureHelper getInstance() {
        if (sInstance == null) {
            sInstance = new RapidCaptureHelper();
        }
        return sInstance;
    }

    public void init(Application application, Callback cb) {
        if (!this.mInitialized) {
            CameraApi cameraApi;
            this.mInitialized = true;
            this.mApplication = application;
            this.mAppContext = this.mApplication.getBaseContext();
            this.mSettingsManager = getServices().getSettingsManager();
            this.mLocationManager = new LocationManager(this.mAppContext);
            this.mOrientationManager = new OrientationManagerImpl(this.mAppContext);
            this.mOrientationManager.addOnOrientationChangeListener(this.mMainHandler, this);
            this.mNamedImages = new NamedImages();
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
            this.mCameraExceptionHandler = new CameraExceptionHandler(this.mCameraExceptionCallback, this.mMainHandler);
            this.mCameraId = this.mCameraController.getFirstBackCameraId();
            this.mSurface = new SurfaceTexture(0);
            this.mSurface.detachFromGLContext();
            this.mServiceCallback = cb;
        }
    }

    public void resume(int type) {
        Log.i(TAG, "resume");
        if (type == TYPE_ONESHOT) {
            this.mMainHandler.sendEmptyMessageDelayed(3, ONESHOT_TIMEOUT);
        } else if (type == TYPE_BURSTSHOT) {
            this.mMainHandler.sendEmptyMessageDelayed(4, BURSTSHOT_TIMEOUT);
        }
        releaseScreenWakeLock();
        this.mOneshotStartTime = System.currentTimeMillis();
        acquireCpuLock(this.mAppContext);
        this.mCameraController.setCameraExceptionHandler(this.mCameraExceptionHandler);
        syncLocationManagerSetting();
        this.mOrientationManager.resume();
        this.mType = type;
        requestBackCamera();
    }

    public void pause() {
        if (this.mType == TYPE_ONESHOT) {
            this.mMainHandler.removeMessages(3);
        } else if (this.mType == TYPE_BURSTSHOT) {
            this.mMainHandler.removeMessages(4);
        }
        this.mOrientationManager.pause();
        this.mLocationManager.disconnect();
        pauseLocationManager();
        this.mCameraController.releaseCamera(this.mCameraId);
        if (this.mCameraDevice != null) {
            this.mCameraDevice.stopPreview();
            this.mCameraController.closeCamera(true);
        }
        this.mCameraController.setCameraExceptionHandler(null);
        this.mCameraDevice = null;
        this.mType = TYPE_INVALID;
        this.mCaptureStarted = false;
        releaseCpuLock();
        this.mOneshotendTime = System.currentTimeMillis();
        Log.i(TAG, "pause end");
        if (this.mCameraFatalError) {
            this.mCameraFatalError = false;
            destroy();
        }
    }

    public void destroy() {
        CameraApi cameraApi;
        Log.i(TAG, "destroy");
        releaseScreenWakeLock();
        this.mNamedImages = null;
        this.mServiceCallback = null;
        this.mCameraController.removeCallbackReceiver();
        this.mCameraController = null;
        CameraAgentFactory.recycle(CameraApi.API_1);
        if (GservicesHelper.useCamera2ApiThroughPortabilityLayer(this.mAppContext)) {
            cameraApi = CameraApi.AUTO;
        } else {
            cameraApi = CameraApi.API_1;
        }
        CameraAgentFactory.recycle(cameraApi);
        this.mSurface.release();
        sInstance = null;
    }

    private CameraServices getServices() {
        return (CameraServices) this.mApplication;
    }

    private void requestBackCamera() {
        this.mopenStartTime = System.currentTimeMillis();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("requestBackCamera");
        stringBuilder.append(this.mCameraId);
        stringBuilder.append(", ");
        stringBuilder.append(this.mType);
        Log.v(str, stringBuilder.toString());
        if (this.mCameraId != -1) {
            this.mCameraController.requestCamera(this.mCameraId, GservicesHelper.useCamera2ApiThroughPortabilityLayer(this.mAppContext));
        }
    }

    public void onCameraOpened(CameraProxy camera) {
        Log.v(TAG, "onCameraOpened");
        this.mopenEndTime = System.currentTimeMillis();
        this.mCameraDevice = camera;
        this.mCameraSettings = this.mCameraDevice.getSettings();
        initializeCapabilities();
        setCameraParameters();
        this.mCameraPreviewParamsReady = true;
        startPreview();
    }

    public void onCameraOpenedBoost(CameraProxy camera) {
    }

    public boolean isBoostPreview() {
        return false;
    }

    public Context getCallbackContext() {
        return null;
    }

    public BoostParameters getBoostParam() {
        return null;
    }

    public void startPreview() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startPreview ");
        stringBuilder.append(this.mType);
        Log.v(str, stringBuilder.toString());
        if (this.mCameraDevice == null || !this.mCameraPreviewParamsReady) {
            Log.v(TAG, "startPreview mCameraDevice null,start later");
            return;
        }
        this.mpreviewStartTime = System.currentTimeMillis();
        setupPreviewListener();
        this.mCameraDevice.setPreviewTexture(this.mSurface);
        CameraStartPreviewCallback startPreviewCallback = new CameraStartPreviewCallback() {
            public void onPreviewStarted() {
                RapidCaptureHelper.this.onPreviewStarted();
            }
        };
        if (GservicesHelper.useCamera2ApiThroughPortabilityLayer(this.mAppContext)) {
            this.mCameraDevice.fakeStartPreview();
            startPreviewCallback.onPreviewStarted();
        } else {
            this.mCameraDevice.startPreviewWithCallback(new Handler(Looper.getMainLooper()), startPreviewCallback);
        }
    }

    public void setupPreviewListener() {
        if (this.mCameraDevice != null) {
            this.mFirstFrameReceived = false;
            this.mFrameCount = 0;
            this.mCameraDevice.setPreviewDataCallback(this.mMainHandler, new CameraPreviewDataCallback() {
                public void onPreviewFrame(byte[] data, CameraProxy camera) {
                    if (!RapidCaptureHelper.this.mFirstFrameReceived) {
                        RapidCaptureHelper.this.mFirstFrameReceived = true;
                        RapidCaptureHelper.this.mFirtstFrameTime = System.currentTimeMillis();
                    }
                    RapidCaptureHelper.this.mFrameCount = RapidCaptureHelper.this.mFrameCount + 1;
                    RapidCaptureHelper.this.mCameraDevice.refreshSettings();
                    RapidCaptureHelper.this.mCameraSettings = RapidCaptureHelper.this.mCameraDevice.getSettings();
                    int aec = RapidCaptureHelper.this.mCameraSettings.getAec();
                    String str = RapidCaptureHelper.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("CameraPreviewDataCallback aec:");
                    stringBuilder.append(aec);
                    Log.d(str, stringBuilder.toString());
                    if (RapidCaptureHelper.this.mCameraSettings.getAec() == 1) {
                        RapidCaptureHelper.this.mAecsettledTime = System.currentTimeMillis();
                        RapidCaptureHelper.this.mCameraDevice.setPreviewDataCallback(RapidCaptureHelper.this.mMainHandler, null);
                        RapidCaptureHelper.this.mMainHandler.removeMessages(1);
                        RapidCaptureHelper.this.capture();
                    }
                }
            });
        }
    }

    private void capture() {
        if (!this.mCaptureStarted) {
            this.mCaptureStarted = true;
            this.mCaptureStartTime = System.currentTimeMillis();
            Log.v(TAG, "capture");
            this.mJpegRotation = this.mCameraController.getCharacteristics(this.mCameraId).getJpegOrientation(this.mOrientation);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("capture orientation  ");
            stringBuilder.append(this.mOrientation);
            stringBuilder.append(",  ");
            stringBuilder.append(this.mJpegRotation);
            Log.v(str, stringBuilder.toString());
            this.mCameraDevice.setJpegOrientation(this.mJpegRotation);
            this.mCameraDevice.enableShutterSound(false);
            Location loc = this.mLocationManager.getCurrentLocation();
            if (this.mType == TYPE_ONESHOT) {
                this.mCameraDevice.takePicture(this.mMainHandler, this.mShutterCallback, null, null, new JpegPictureCallback(loc));
            } else {
                this.mSnapshotBurstNum++;
                this.mLongshotPictureTakenCallback = new LongshotPictureCallback(loc);
                this.mCameraDevice.burstShot(null, null, null, null, null);
                this.mCameraDevice.takePicture(this.mMainHandler, this.mLongshotShutterCallback, null, null, this.mLongshotPictureTakenCallback);
            }
            this.mNamedImages.nameNewImage(this.mCaptureStartTime);
        }
    }

    private void onPreviewStarted() {
        this.mpreviewEndTime = System.currentTimeMillis();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onPreviewStarted");
        stringBuilder.append(this.mCaptureStarted);
        Log.v(str, stringBuilder.toString());
        if (!this.mCaptureStarted) {
            this.mMainHandler.sendEmptyMessageDelayed(1, AEC_SETTLE_TIMEOUT);
        }
    }

    public void stopBurst() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("stop burst shot ");
        stringBuilder.append(this.mType);
        Log.d(str, stringBuilder.toString());
        if (this.mType == TYPE_BURSTSHOT) {
            this.mBurstshotBreak = true;
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void saveFinalPhoto(byte[] jpegData, NamedEntity name, ExifInterface exif, CameraProxy camera, Location loc, Map<String, Object> externalInfos) {
        int width;
        int height;
        NamedEntity namedEntity = name;
        ExifInterface exifInterface = exif;
        int orientation = Exif.getOrientation(exif);
        String title = namedEntity == null ? null : namedEntity.title;
        long date = namedEntity == null ? -1 : namedEntity.date;
        Size s = this.mCameraSettings.getCurrentPhotoSize();
        if ((this.mJpegRotation + orientation) % MediaProviderUtils.ROTATION_180 == 0) {
            width = s.width();
            height = s.height();
        } else {
            width = s.height();
            height = s.width();
        }
        int width2 = width;
        int height2 = height;
        if (externalInfos != null) {
            exifInterface.setTag(exifInterface.buildTag(ExifInterface.TAG_USER_COMMENT, CameraUtil.serializeToJson(externalInfos)));
        }
        getServices().getMediaSaver().addImage(jpegData, title, date, loc, width2, height2, orientation, exifInterface, this.mOnMediaSavedListener, this.mAppContext.getContentResolver());
    }

    private void onCaptureDone(Uri uri) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onCaptureDone  ");
        stringBuilder.append(uri);
        stringBuilder.append(", ");
        stringBuilder.append(this.mType);
        Log.i(str, stringBuilder.toString());
        this.mOneshotendTime = System.currentTimeMillis();
        if (uri != null) {
            if (this.mType == TYPE_ONESHOT) {
                startViewImageActivity(uri);
            } else if (this.mType == TYPE_BURSTSHOT) {
                startGallery(uri);
            }
        }
        this.mMainHandler.removeCallbacks(this.burstshot);
        if (this.mBurstShotUris != null) {
            this.mBurstShotUris.clear();
            this.mBurstShotUris = null;
        }
        this.mSnapshotBurstNum = 0;
        this.mBurstshotBreak = false;
        if (this.mServiceCallback != null) {
            this.mServiceCallback.onCaptureDone(this.mCameraFatalError);
        }
    }

    private void startViewImageActivity(Uri uri) {
        Log.i(TAG, "startViewImageActivity");
        Intent intent = new Intent();
        intent.setClass(this.mAppContext, RapidViewImageActivity.class);
        intent.setDataAndType(uri, "image/jpeg");
        intent.addFlags(268435457);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startViewImageActivityoneshotduration:");
        stringBuilder.append(this.mOneshotendTime - this.mOneshotStartTime);
        stringBuilder.append(",cameraopentime:");
        stringBuilder.append(this.mopenEndTime - this.mopenStartTime);
        stringBuilder.append(",previewtime:");
        stringBuilder.append(this.mpreviewEndTime - this.mpreviewStartTime);
        stringBuilder.append(",firstframe:");
        stringBuilder.append(this.mFirtstFrameTime - this.mpreviewEndTime);
        stringBuilder.append(",aecsettled:");
        stringBuilder.append(this.mAecsettledTime - this.mFirtstFrameTime);
        stringBuilder.append(",framecount:");
        stringBuilder.append(this.mFrameCount);
        stringBuilder.append(",capture_shutter:");
        stringBuilder.append(this.mShutterCallbackTime - this.mCaptureStartTime);
        stringBuilder.append(",shutter_jpegcallback:");
        stringBuilder.append(this.mJpegPictureCallbackTime - this.mShutterCallbackTime);
        Log.d(str, stringBuilder.toString());
        intent.putExtra("oneshotduration", this.mOneshotendTime - this.mOneshotStartTime);
        intent.putExtra("cameraopentime", this.mopenEndTime - this.mopenStartTime);
        intent.putExtra("previewtime", this.mpreviewEndTime - this.mpreviewStartTime);
        intent.putExtra("firstframe", this.mFirtstFrameTime - this.mpreviewEndTime);
        intent.putExtra("aecsettled", this.mAecsettledTime - this.mFirtstFrameTime);
        intent.putExtra("framecount", this.mFrameCount);
        intent.putExtra("capture_shutter", this.mShutterCallbackTime - this.mCaptureStartTime);
        intent.putExtra("shutter_jpegcallback", this.mJpegPictureCallbackTime - this.mShutterCallbackTime);
        RapidViewImageActivity.mIsRunning = true;
        this.mAppContext.startActivity(intent);
    }

    private void startGallery(Uri uri) {
        Log.i(TAG, "startGallery");
        String GALLERY_PACKAGE_NAME = "com.android.gallery3d";
        String GALLERY_ACTIVITY_CLASS = "com.android.gallery3d.app.PermissionActivity";
        String REVIEW_ACTION = "com.hmdglobal.app.camera.action.REVIEW";
        Intent intent = new Intent("com.hmdglobal.app.camera.action.REVIEW");
        intent.setClassName("com.android.gallery3d", "com.android.gallery3d.app.PermissionActivity");
        intent.setFlags(268435457);
        intent.setType("*/*");
        intent.setData(uri);
        intent.putParcelableArrayListExtra("uriarray", this.mBurstShotUris);
        try {
            this.mAppContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.d(TAG, "not find the activity");
        }
    }

    private void initializeCapabilities() {
        this.mCameraCapabilities = this.mCameraDevice.getCapabilities();
    }

    private void setCameraParameters() {
        updateCameraParametersInitialize();
        updateCameraParametersPreference();
        updateParametersPictureSize();
        CameraUtil.setGpsParameters(this.mCameraSettings, this.mLocationManager.getCurrentLocation());
        if (this.mCameraDevice != null) {
            this.mCameraDevice.applySettings(this.mCameraSettings);
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

    private void updateCameraParametersPreference() {
        if (this.mCameraDevice != null) {
            this.mCameraSettings.isZslOn = true;
            updateParametersPictureQuality();
        }
    }

    private void updateParametersPictureQuality() {
        this.mCameraSettings.setPhotoJpegCompressionQuality(CameraProfile.getJpegEncodingQualityParameter(this.mCameraId, 2));
    }

    private void updateParametersPictureSize() {
        if (this.mCameraDevice == null) {
            Log.w(TAG, "attempting to set picture size without caemra device");
            return;
        }
        String str;
        StringBuilder stringBuilder;
        String pictureSize = getServices().getSettingsManager().getString(SettingsManager.SCOPE_GLOBAL, Keys.KEY_PICTURE_SIZE_BACK, SettingsUtil.getDefaultPictureSize(null));
        List<Size> supported = this.mCameraCapabilities.getSupportedPhotoSizes();
        CameraPictureSizesCacher.updateSizesForCamera(this.mAppContext, this.mCameraDevice.getCameraId(), supported);
        SettingsUtil.setCameraPictureSize(pictureSize, supported, this.mCameraSettings, this.mCameraDevice.getCameraId());
        Size size = SettingsUtil.getPhotoSize(pictureSize, supported, this.mCameraDevice.getCameraId());
        Size optimalSize = CameraUtil.getOptimalPreviewSize(this.mAppContext, this.mCameraCapabilities.getSupportedPreviewSizes(), ((double) size.width()) / ((double) size.height()));
        Size original = this.mCameraSettings.getCurrentPreviewSize();
        if (!optimalSize.equals(original)) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("setting preview size. optimal: ");
            stringBuilder.append(optimalSize);
            stringBuilder.append("original: ");
            stringBuilder.append(original);
            Log.v(str, stringBuilder.toString());
            this.mCameraSettings.setPreviewSize(optimalSize);
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Preview size is ");
        stringBuilder.append(optimalSize);
        Log.d(str, stringBuilder.toString());
    }

    public void onCameraDisabled(int cameraId) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Camera disabled: ");
        stringBuilder.append(cameraId);
        Log.w(str, stringBuilder.toString());
        this.mCameraFatalError = true;
        onCaptureDone(null);
    }

    public void onDeviceOpenFailure(int cameraId, String info) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Camera open failure: ");
        stringBuilder.append(info);
        Log.w(str, stringBuilder.toString());
        this.mCameraFatalError = true;
        onCaptureDone(null);
    }

    public void onDeviceOpenedAlready(int cameraId, String info) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Camera open already: ");
        stringBuilder.append(cameraId);
        stringBuilder.append(Size.DELIMITER);
        stringBuilder.append(info);
        Log.w(str, stringBuilder.toString());
        this.mCameraFatalError = true;
        onCaptureDone(null);
    }

    public void onReconnectionFailure(CameraAgent mgr, String info) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Camera reconnection failure:");
        stringBuilder.append(info);
        Log.w(str, stringBuilder.toString());
        onCaptureDone(null);
    }

    public void onCameraRequested() {
    }

    public void onCameraClosed() {
    }

    public boolean isReleased() {
        return false;
    }

    public void syncLocationManagerSetting() {
        Keys.syncLocationManager(this.mSettingsManager, this.mLocationManager);
    }

    public void pauseLocationManager() {
        if (this.mLocationManager != null) {
            Keys.pauseLocationManager(this.mLocationManager);
        }
    }

    public void onOrientationChanged(int orientation) {
        if (orientation != -1) {
            this.mOrientation = (360 - orientation) % 360;
        }
    }

    public void acquireScreenWakeLock(Context context) {
        if (this.mScreenWakeLock == null) {
            this.mScreenWakeLock = ((PowerManager) context.getSystemService("power")).newWakeLock(268435482, "screenwakelock");
            this.mScreenWakeLock.acquire();
            this.mScreenWakeLock.setReferenceCounted(false);
        }
        this.mMainHandler.removeMessages(2);
        this.mMainHandler.sendEmptyMessageDelayed(2, SCREEN_WAKELOCK_TIMEOUT);
    }

    private void releaseScreenWakeLock() {
        this.mMainHandler.removeMessages(2);
        if (this.mScreenWakeLock != null) {
            this.mScreenWakeLock.release();
            this.mScreenWakeLock = null;
        }
    }

    public void acquireCpuLock(Context context) {
        if (this.mCPUWakeLock == null) {
            PowerManager pm = (PowerManager) context.getSystemService("power");
            if (!pm.isScreenOn()) {
                this.mCPUWakeLock = pm.newWakeLock(1, "cpuwakelock");
                this.mCPUWakeLock.acquire();
                this.mCPUWakeLock.setReferenceCounted(false);
            }
        }
    }

    public void releaseCpuLock() {
        if (this.mCPUWakeLock != null) {
            this.mCPUWakeLock.release();
            this.mCPUWakeLock = null;
        }
    }
}
