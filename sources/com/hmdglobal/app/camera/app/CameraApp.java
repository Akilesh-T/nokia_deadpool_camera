package com.hmdglobal.app.camera.app;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.app.NotificationManager;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.util.Size;
import com.hmdglobal.app.camera.CameraActivity;
import com.hmdglobal.app.camera.DeviceInfo;
import com.hmdglobal.app.camera.MediaSaverImpl;
import com.hmdglobal.app.camera.SDCard;
import com.hmdglobal.app.camera.SecureCameraActivity;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.debug.LogHelper;
import com.hmdglobal.app.camera.instantcapture.InstantViewImageActivity;
import com.hmdglobal.app.camera.processing.ProcessingServiceManager;
import com.hmdglobal.app.camera.remote.RemoteShutterListener;
import com.hmdglobal.app.camera.session.CaptureSessionManager;
import com.hmdglobal.app.camera.session.CaptureSessionManagerImpl;
import com.hmdglobal.app.camera.session.PlaceholderManager;
import com.hmdglobal.app.camera.session.SessionStorageManager;
import com.hmdglobal.app.camera.session.SessionStorageManagerImpl;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.hmdglobal.app.camera.util.CustomUtil;
import com.hmdglobal.app.camera.util.FileUtil;
import com.hmdglobal.app.camera.util.RemoteShutterHelper;
import com.hmdglobal.app.camera.util.SessionStatsCollector;
import com.hmdglobal.app.camera.util.UsageStatistics;
import com.morphoinc.app.viewer.MorphoPanoramaViewer;
import com.morphoinc.app.viewer.MorphoPanoramaViewer.ViewParam;
import java.io.File;

public class CameraApp extends Application implements CameraServices, ActivityLifecycleCallbacks {
    private static CameraApp mCameraApp;
    private Tag TAG = new Tag("CameraApp");
    private String mInputFilePath;
    private boolean mInstantViewActivityActive;
    private Boolean mIsReversibleEnabled = null;
    private final LastShotFile mLastShot = new LastShotFile();
    private boolean mMainActivityActive;
    private MediaSaver mMediaSaver;
    private MemoryManagerImpl mMemoryManager;
    private MorphoPanoramaViewer mMorphoImageStitcher;
    private MotionManager mMotionManager;
    private PlaceholderManager mPlaceHolderManager;
    private ViewParam mPostviewDefaultParam;
    private ViewParam mPostviewParam;
    private Size mPreviewSize = new Size(0, 0);
    private RemoteShutterListener mRemoteShutterListener;
    private CaptureSessionManager mSessionManager;
    private SessionStorageManager mSessionStorageManager;
    private SettingsManager mSettingsManager;
    private int[] mSupportedPictureSizes = new int[0];
    private Size mThumbnailMaxSize = new Size(1, 1);

    public static class LastShotFile {
        private File mFile = null;
        private int mHeight = 0;
        private Uri mUri = null;
        private int mWidth = 0;

        public void setFile(File file) {
            this.mFile = file;
        }

        public String filePath() {
            if (this.mFile == null) {
                return "";
            }
            return this.mFile.getAbsolutePath();
        }

        public Uri uri() {
            return this.mUri;
        }

        public void setUri(Uri uri) {
            this.mUri = uri;
        }

        public int width() {
            return this.mWidth;
        }

        public void setWidth(int width) {
            this.mWidth = width;
        }

        public int height() {
            return this.mHeight;
        }

        public void setHeight(int height) {
            this.mHeight = height;
        }
    }

    private class SettingObserver extends ContentObserver {
        public SettingObserver() {
            super(null);
        }

        public void onChange(boolean selfChange) {
            Log.v(CameraApp.this.TAG, "reversible setting changed");
            CameraApp.this.mIsReversibleEnabled = Boolean.valueOf(DeviceInfo.isReversibleOn(CameraApp.this.getContentResolver()));
        }
    }

    public static CameraApp getContext() {
        return mCameraApp;
    }

    public void onCreate() {
        super.onCreate();
        mCameraApp = this;
        Context context = getApplicationContext();
        LogHelper.initialize(context);
        Log.v(this.TAG, "onCreate CameraApplication");
        UsageStatistics.instance().initialize(this);
        SessionStatsCollector.instance().initialize(this);
        CameraUtil.initialize(this);
        CustomUtil.getInstance(context).setCustomFromSystem();
        SDCard.initialize(this);
        ProcessingServiceManager.initSingleton(context);
        this.mMediaSaver = new MediaSaverImpl();
        clearNotifications();
        this.mMotionManager = new MotionManager(context);
        Uri uri = DeviceInfo.getReversibleSettingUri();
        if (uri != null) {
            Log.v(this.TAG, "start observing reversible");
            getContentResolver().registerContentObserver(uri, false, new SettingObserver());
        }
        registerActivityLifecycleCallbacks(this);
        FileUtil.saveAssetsToSdcard();
    }

    public synchronized CaptureSessionManager getCaptureSessionManager() {
        if (this.mSessionManager == null) {
            this.mPlaceHolderManager = new PlaceholderManager(this);
            this.mSessionStorageManager = SessionStorageManagerImpl.create(this);
            this.mSessionManager = new CaptureSessionManagerImpl(this.mMediaSaver, getContentResolver(), this.mPlaceHolderManager, this.mSessionStorageManager);
        }
        return this.mSessionManager;
    }

    public synchronized MemoryManager getMemoryManager() {
        if (this.mMemoryManager == null) {
            this.mMemoryManager = MemoryManagerImpl.create(getApplicationContext(), this.mMediaSaver);
        }
        return this.mMemoryManager;
    }

    public MotionManager getMotionManager() {
        return this.mMotionManager;
    }

    @Deprecated
    public MediaSaver getMediaSaver() {
        return this.mMediaSaver;
    }

    public synchronized RemoteShutterListener getRemoteShutterListener() {
        if (this.mRemoteShutterListener == null) {
            this.mRemoteShutterListener = RemoteShutterHelper.create(this);
        }
        return this.mRemoteShutterListener;
    }

    public synchronized SettingsManager getSettingsManager() {
        if (this.mSettingsManager == null) {
            this.mSettingsManager = new SettingsManager(this);
        }
        return this.mSettingsManager;
    }

    private void clearNotifications() {
        NotificationManager manager = (NotificationManager) getSystemService("notification");
        if (manager != null) {
            manager.cancelAll();
        }
    }

    public boolean isReversibleEnabled() {
        if (this.mIsReversibleEnabled == null) {
            this.mIsReversibleEnabled = Boolean.valueOf(DeviceInfo.isReversibleOn(getContentResolver()));
        }
        return this.mIsReversibleEnabled.booleanValue();
    }

    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    public void onActivityDestroyed(Activity activity) {
    }

    public void onActivityPaused(Activity activity) {
    }

    public void onActivityResumed(Activity activity) {
    }

    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    public void onActivityStarted(Activity activity) {
        if ((activity instanceof CameraActivity) || (activity instanceof SecureCameraActivity)) {
            this.mMainActivityActive = true;
        } else if (activity instanceof InstantViewImageActivity) {
            this.mInstantViewActivityActive = true;
        }
    }

    public void onActivityStopped(Activity activity) {
        if ((activity instanceof CameraActivity) || (activity instanceof SecureCameraActivity)) {
            this.mMainActivityActive = false;
        } else if (activity instanceof InstantViewImageActivity) {
            this.mInstantViewActivityActive = false;
        }
    }

    public boolean isMainActivityActive() {
        return this.mMainActivityActive;
    }

    public boolean isInstantViewActivityActive() {
        return this.mInstantViewActivityActive;
    }

    public MorphoPanoramaViewer getMorphoImageStitcher() {
        if (this.mMorphoImageStitcher == null || MorphoPanoramaViewer.isFinished()) {
            this.mMorphoImageStitcher = new MorphoPanoramaViewer();
        }
        return this.mMorphoImageStitcher;
    }

    public Size[] getSupportedPictureSizes() {
        int num = this.mSupportedPictureSizes.length >> 1;
        Size[] sizes = new Size[num];
        for (int i = 0; i < num; i++) {
            sizes[i] = new Size(this.mSupportedPictureSizes[i * 2], this.mSupportedPictureSizes[(i * 2) + 1]);
        }
        return sizes;
    }

    public void setSupportedPictureSizes(int[] sizes) {
        this.mSupportedPictureSizes = (int[]) sizes.clone();
    }

    public Size getPreviewSize() {
        return this.mPreviewSize;
    }

    public void setPreviewSize(Size size) {
        this.mPreviewSize = size;
    }

    public void setLastShotFile(String path, int width, int height) {
        this.mLastShot.setFile(new File(path));
        this.mLastShot.setWidth(width);
        this.mLastShot.setHeight(height);
    }

    public LastShotFile getLastShotFile() {
        return this.mLastShot;
    }

    public void clearLastShotFile() {
        this.mLastShot.setFile(null);
        this.mLastShot.setWidth(0);
        this.mLastShot.setHeight(0);
        this.mLastShot.setUri(null);
    }

    public void updateThumbnailMaxSize(int width, int height) {
        if (width * height >= this.mThumbnailMaxSize.getWidth() * this.mThumbnailMaxSize.getHeight()) {
            this.mThumbnailMaxSize = new Size(width, height);
        }
    }

    public final Size getThumbnailMaxSize() {
        return this.mThumbnailMaxSize;
    }

    public void setInputFilePath(String path) {
        this.mInputFilePath = path;
    }

    public String getInputFilePath() {
        return this.mInputFilePath;
    }

    public ViewParam getPostviewParam() {
        return this.mPostviewParam;
    }

    public ViewParam getPostviewDefaultParam() {
        return this.mPostviewDefaultParam;
    }
}
