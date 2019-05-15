package com.hmdglobal.app.camera;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.location.Location;
import android.media.SoundPool;
import android.net.Uri;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.util.SparseArray;
import com.android.ex.camera2.portability.CameraCapabilities.FlashMode;
import com.android.external.ExtendKey;
import com.hmdglobal.app.camera.data.LocalMediaData.PhotoData;
import com.hmdglobal.app.camera.exif.ExifInterface;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.hmdglobal.app.camera.util.ExternalExifInterface;
import com.hmdglobal.app.camera.util.NV21Convertor;
import com.morphoinc.utils.multimedia.MediaProviderUtils;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ContinueShot {
    private static ContinueShot CSHOT = null;
    private static final int MAX_CAPTURE_NUM = 10;
    private static final String TAG = "ContinueShotRoutine";
    private boolean destroyed = false;
    private int interval = 120;
    private boolean isJpegReady = false;
    private boolean isProcessing = true;
    private boolean isStop = false;
    private int jpegOrientation;
    private JpegInfo lastJpegInfo;
    private Location loc;
    private CameraActivity mActivity;
    private ActivityManager mActivityManager;
    private ContinueShotPictureCallback mCB;
    private Camera mCameraDevice;
    private ContentResolver mContentResolver;
    private int mDisplayCaptureNum = 0;
    private FlashMode mFlashMode;
    private onContinueShotFinishListener mListener;
    private int mMaxCaptureNum = 10;
    private MemoryInfo mMemInfo;
    private NV21Convertor mNV21Convertor;
    private PhotoModule mPhotoModule;
    private Bitmap mPreviewThumb = null;
    private ProgressDialog mProgressDialog;
    private ConditionVariable mReady = new ConditionVariable(true);
    private int mSaveCaptureNum = 0;
    private HandlerThread mSaveHT = null;
    private Handler mSaveHandler = null;
    private boolean mSoundEnable = true;
    private SoundPlay mSoundPlay = null;
    private int mTakenNum = 0;
    private Handler mUiHandler;
    private int originalJpegQuality;
    private Parameters param;
    private int rotation;
    private Size size;

    class ContinueShotPictureCallback implements PictureCallback {
        ArrayBlockingQueue<JpegInfo> mJpegQueue = new ArrayBlockingQueue(11);
        SparseArray<Bitmap> mThumbQueue = new SparseArray(11);

        public void onPictureTaken(byte[] jpegData, Camera camera) {
            ContinueShot.this.mTakenNum = ContinueShot.this.mTakenNum + 1;
            if (ContinueShot.this.mTakenNum == 1) {
                ContinueShot.this.mReady.open();
            }
            if (ContinueShot.this.mTakenNum <= 10) {
                try {
                    this.mJpegQueue.put(new JpegInfo(jpegData, System.currentTimeMillis(), ContinueShot.this.loc));
                } catch (Exception e) {
                }
                this.mThumbQueue.put(ContinueShot.this.mTakenNum, Exif.getExif(jpegData).getThumbnailBitmap());
                ContinueShot.this.isJpegReady = true;
                String str = ContinueShot.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("PictureCallback num = ");
                stringBuilder.append(ContinueShot.this.mTakenNum);
                Log.d(str, stringBuilder.toString());
            }
        }
    }

    private final class JpegInfo {
        long captureStartTime;
        byte[] jpegData;
        Location location;

        public JpegInfo(ContinueShot continueShot, byte[] data, long time) {
            this(data, time, null);
        }

        public JpegInfo(byte[] data, long time, Location l) {
            this.jpegData = data;
            this.captureStartTime = time;
            this.location = l;
        }
    }

    private class SaveHandlerCB implements Callback {
        int lastNum;
        int num;
        int retry;

        private SaveHandlerCB() {
            this.num = 0;
            this.lastNum = 0;
            this.retry = 0;
        }

        /* synthetic */ SaveHandlerCB(ContinueShot x0, AnonymousClass1 x1) {
            this();
        }

        public boolean handleMessage(Message arg0) {
            this.num = arg0.what;
            String str;
            StringBuilder stringBuilder;
            if (this.lastNum == this.num) {
                str = ContinueShot.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("mDisplayCapture num = ");
                stringBuilder.append(this.lastNum);
                stringBuilder.append("mPictureTaken num  = ");
                stringBuilder.append(ContinueShot.this.mTakenNum);
                stringBuilder.append("savePicture num = ");
                stringBuilder.append(ContinueShot.this.mSaveCaptureNum);
                Log.d(str, stringBuilder.toString());
                while (this.num > ContinueShot.this.mTakenNum) {
                    this.retry++;
                    if (this.retry == 1) {
                        ContinueShot.this.mUiHandler.sendEmptyMessage(this.num);
                    }
                    if (this.retry > 5) {
                        break;
                    }
                    str = ContinueShot.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("wait ... Picture taken  num = ");
                    stringBuilder.append(ContinueShot.this.mTakenNum + 1);
                    Log.d(str, stringBuilder.toString());
                    SystemClock.sleep(100);
                }
                ContinueShot.this.canclePicture();
                while (this.num > ContinueShot.this.mSaveCaptureNum) {
                    str = ContinueShot.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("wait ... saving picture num = ");
                    stringBuilder.append(ContinueShot.this.mSaveCaptureNum + 1);
                    Log.d(str, stringBuilder.toString());
                    SystemClock.sleep(100);
                }
                ContinueShot.this.close();
                return true;
            }
            this.lastNum = this.num;
            str = ContinueShot.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("send message savePicture num = ");
            stringBuilder.append(this.num);
            Log.d(str, stringBuilder.toString());
            new Thread(new Runnable() {
                public void run() {
                    ContinueShot.this.savePicture(SaveHandlerCB.this.num);
                }
            }).start();
            return true;
        }
    }

    private class SoundPlay {
        private boolean soundEnable;
        private int soundID;
        private SoundPool soundPool;
        private int streamID;

        /* synthetic */ SoundPlay(ContinueShot x0, boolean x1, AnonymousClass1 x2) {
            this(x1);
        }

        private SoundPlay(boolean s) {
            this.soundPool = null;
            this.soundID = 0;
            this.streamID = 0;
            this.soundEnable = s;
        }

        private void load() {
            if (this.soundEnable) {
                this.soundPool = new SoundPool(10, SoundClips.getAudioTypeForSoundPool(), 0);
                this.soundID = this.soundPool.load(ContinueShot.this.mActivity, R.raw.continuous_shot, 1);
            }
        }

        private void play() {
            if (this.soundEnable) {
                if (this.streamID != 0) {
                    this.soundPool.stop(this.streamID);
                }
                this.streamID = this.soundPool.play(this.soundID, 0.5f, 0.5f, 1, 0, 1.5f);
            }
        }

        private void unLoad() {
            if (this.soundEnable && this.soundPool != null) {
                this.soundPool.unload(this.soundID);
                this.soundPool.release();
                this.soundPool = null;
            }
        }
    }

    private class UiHandlerCB implements Callback {
        int lastNum;
        int num;

        private UiHandlerCB() {
            this.num = 0;
            this.lastNum = 0;
        }

        /* synthetic */ UiHandlerCB(ContinueShot x0, AnonymousClass1 x1) {
            this();
        }

        public boolean handleMessage(Message arg0) {
            this.num = arg0.what;
            if (this.lastNum == this.num) {
                ContinueShot.this.showSavingHint(this.num);
                return true;
            }
            this.lastNum = this.num;
            ContinueShot.this.mPhotoModule.getPhotoUI().updateBurstCount(this.num);
            if (this.num == 0) {
                return true;
            }
            if (ContinueShot.this.mCB != null) {
                Bitmap bmp = (Bitmap) ContinueShot.this.mCB.mThumbQueue.get(this.num, null);
                String str = ContinueShot.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("UiHandlerCB bmp:");
                stringBuilder.append(bmp);
                stringBuilder.append("  num:");
                stringBuilder.append(this.num);
                Log.d(str, stringBuilder.toString());
                if (bmp == null && ContinueShot.this.mCB.mThumbQueue.size() > 0) {
                    bmp = (Bitmap) ContinueShot.this.mCB.mThumbQueue.get(ContinueShot.this.mCB.mThumbQueue.size(), null);
                }
                if (bmp == null) {
                    bmp = ContinueShot.this.mPreviewThumb;
                    str = ContinueShot.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("UiHandlerCB mPreviewThumb:");
                    stringBuilder2.append(bmp);
                    stringBuilder2.append("  num:");
                    stringBuilder2.append(this.num);
                    Log.d(str, stringBuilder2.toString());
                }
                if (bmp != null) {
                    ContinueShot.this.mActivity.getCameraAppUI().updatePeekThumbBitmapWithAnimation(bmp);
                }
            }
            String str2 = ContinueShot.TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("UiHandlerCB updateBurstCount = ");
            stringBuilder3.append(this.num);
            Log.d(str2, stringBuilder3.toString());
            return true;
        }
    }

    public interface onContinueShotFinishListener {
        void onFinish();
    }

    private ContinueShot(PhotoModule module) {
        this.mActivity = module.mActivity;
        this.mNV21Convertor = new NV21Convertor(this.mActivity.getApplicationContext());
        this.mContentResolver = this.mActivity.getContentResolver();
        this.mPhotoModule = module;
        this.mCameraDevice = module.mCameraDevice.getCamera();
        this.param = this.mCameraDevice.getParameters();
        this.originalJpegQuality = this.param.getJpegQuality();
        this.mSoundEnable = Keys.isShutterSoundOn(this.mActivity.getSettingsManager());
        this.jpegOrientation = this.mPhotoModule.getJpegRotation(false);
        this.mFlashMode = this.mPhotoModule.mCameraSettings.getCurrentFlashMode();
        this.loc = this.mActivity.getLocationManager().getCurrentLocation();
        CameraUtil.setGpsParameters(this.mPhotoModule.mCameraSettings, this.loc);
        this.mPhotoModule.mCameraDevice.applySettings(this.mPhotoModule.mCameraSettings);
        this.mUiHandler = new Handler(module.mHandler.getLooper(), new UiHandlerCB(this, null));
        this.mSaveHT = new HandlerThread("ContinueShotSave", 1);
        this.mSaveHT.start();
        this.mSaveHandler = new Handler(this.mSaveHT.getLooper(), new SaveHandlerCB(this, null));
        CameraActivity cameraActivity = this.mActivity;
        CameraActivity cameraActivity2 = this.mActivity;
        this.mActivityManager = (ActivityManager) cameraActivity.getSystemService("activity");
        this.mMemInfo = new MemoryInfo();
        this.mSoundPlay = new SoundPlay(this, this.mSoundEnable, null);
    }

    public static synchronized ContinueShot create(PhotoModule module) {
        ContinueShot continueShot;
        synchronized (ContinueShot.class) {
            destroy();
            CSHOT = new ContinueShot(module);
            continueShot = CSHOT;
        }
        return continueShot;
    }

    public static synchronized void destroy() {
        synchronized (ContinueShot.class) {
            if (!(CSHOT == null || CSHOT.destroyed)) {
                CSHOT.destroyed = true;
                CSHOT.mSaveHT.quit();
            }
            CSHOT = null;
        }
    }

    public void prepare() {
        this.isJpegReady = false;
        this.rotation = this.mCameraDevice.getParameters().getInt("rotation");
        this.size = this.mCameraDevice.getParameters().getPreviewSize();
        this.mCameraDevice.setPreviewCallback(new PreviewCallback() {
            public void onPreviewFrame(byte[] arg0, Camera arg1) {
                if (ContinueShot.this.isJpegReady) {
                    ContinueShot.this.mCameraDevice.setPreviewCallback(null);
                    return;
                }
                Bitmap bmp = ContinueShot.this.compressToThumb(arg0, ContinueShot.this.size.width, ContinueShot.this.size.height);
                if (ContinueShot.this.jpegOrientation % 360 != 0) {
                    Matrix matrix = new Matrix();
                    matrix.setRotate((float) ContinueShot.this.jpegOrientation);
                    Bitmap thumb = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                    bmp.recycle();
                    ContinueShot.this.mPreviewThumb = thumb;
                    return;
                }
                ContinueShot.this.mPreviewThumb = bmp;
            }
        });
    }

    private void setflashMode() {
        if (this.mFlashMode != null) {
            if (this.mFlashMode == FlashMode.AUTO) {
                this.param.setFlashMode(ExtendKey.FLIP_MODE_OFF);
            } else if (this.mFlashMode == FlashMode.ON) {
                this.param.setFlashMode("torch");
            } else if (this.mFlashMode == FlashMode.OFF) {
                this.param.setFlashMode(ExtendKey.FLIP_MODE_OFF);
            }
        }
    }

    private Bitmap compressToThumb(byte[] yuv, int rw, int rh) {
        return this.mNV21Convertor.convertNV21ToBitmap(yuv, rw, rh);
    }

    public void takePicture() {
        takePicture(null);
    }

    public void takePicture(onContinueShotFinishListener l) {
        this.isJpegReady = false;
        this.mReady.close();
        this.mListener = l;
        this.mCB = new ContinueShotPictureCallback();
        start();
        this.param.set("snapshot-burst-num", 10);
        this.param.setJpegQuality(85);
        this.param.setRotation(this.jpegOrientation);
        setflashMode();
        this.mCameraDevice.enableShutterSound(false);
        this.mCameraDevice.setParameters(this.param);
        calcInterval(this.param.getPictureSize());
        this.mCameraDevice.takePicture(null, null, null, this.mCB);
    }

    public void start() {
        new Thread(new Runnable() {
            long time = 0;

            public void run() {
                String str;
                StringBuilder stringBuilder;
                ContinueShot.this.isStop = false;
                ContinueShot.this.mSoundPlay.load();
                Process.setThreadPriority(-2);
                ContinueShot.this.mReady.block(500);
                Log.d(ContinueShot.TAG, "mReady====");
                while (!ContinueShot.this.isStop && ContinueShot.this.mDisplayCaptureNum < ContinueShot.this.mMaxCaptureNum) {
                    this.time = System.currentTimeMillis() + ((long) ContinueShot.this.interval);
                    ContinueShot.this.mDisplayCaptureNum = ContinueShot.this.mDisplayCaptureNum + 1;
                    ContinueShot.this.mSoundPlay.play();
                    str = ContinueShot.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("sendEmptyMessage:");
                    stringBuilder.append(ContinueShot.this.mDisplayCaptureNum);
                    Log.d(str, stringBuilder.toString());
                    ContinueShot.this.mUiHandler.sendEmptyMessage(ContinueShot.this.mDisplayCaptureNum);
                    ContinueShot.this.mSaveHandler.sendEmptyMessage(ContinueShot.this.mDisplayCaptureNum);
                    long slptm = this.time - System.currentTimeMillis();
                    if (slptm > 0) {
                        SystemClock.sleep(slptm);
                    }
                }
                ContinueShot.this.mSoundPlay.unLoad();
                if (ContinueShot.this.isStop || ContinueShot.this.mDisplayCaptureNum == ContinueShot.this.mMaxCaptureNum) {
                    str = ContinueShot.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("sendEmptyMessage stop:");
                    stringBuilder.append(ContinueShot.this.mDisplayCaptureNum);
                    Log.d(str, stringBuilder.toString());
                    ContinueShot.this.mSaveHandler.sendEmptyMessage(ContinueShot.this.mDisplayCaptureNum);
                }
            }
        }).start();
    }

    public synchronized void stop() {
        Log.d(TAG, "Stop bustShot");
        this.isStop = true;
    }

    public synchronized void close() {
        Log.d(TAG, "close====");
        if (this.isProcessing) {
            this.isProcessing = false;
            this.mUiHandler.sendEmptyMessage(0);
            dismissSavingHint();
            this.param.set("snapshot-burst-num", 1);
            this.param.setJpegQuality(this.originalJpegQuality);
            this.param.setFlashMode(ExtendKey.FLIP_MODE_OFF);
            this.mCameraDevice.enableShutterSound(false);
            this.mCameraDevice.setParameters(this.param);
            destroy();
            this.mActivity.runOnUiThread(new Runnable() {
                public void run() {
                    if (ContinueShot.this.mListener != null) {
                        ContinueShot.this.mListener.onFinish();
                        ContinueShot.this.realseAfterFinish();
                    }
                }
            });
        }
    }

    public synchronized boolean canclePicture() {
        Method pictureMethod = null;
        try {
            Class.forName("android.hardware.Camera").getMethod("cancelPicture", null).invoke(this.mCameraDevice, null);
            Log.d(TAG, "canclePicture succeed");
        } catch (Exception e) {
            Log.d(TAG, "canclePicture failed");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void realseAfterFinish() {
        if (!this.isProcessing) {
            if (this.loc != null) {
                this.loc = null;
            }
            if (!(this.mCB == null || this.mCB.mJpegQueue == null)) {
                this.mCB.mJpegQueue.clear();
                this.mCB.mJpegQueue = null;
            }
            if (!(this.mCB == null || this.mCB.mThumbQueue == null)) {
                this.mCB.mThumbQueue.clear();
                this.mCB.mThumbQueue = null;
            }
            this.mCB = null;
        }
    }

    private void calcInterval(Size size) {
        this.interval = 80;
        int pixel = size.width * size.height;
        if (pixel >= 7990272) {
            this.interval = 80;
        } else if (pixel >= 5038848) {
            this.interval = 80;
        } else if (pixel >= 1920000) {
            this.interval = 80;
        } else if (pixel >= 786432) {
            this.interval = 80;
        } else {
            this.interval = 80;
        }
    }

    public int savePicture(int count) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("savePicture node1  num = ");
        int i = count;
        stringBuilder.append(i);
        Log.d(str, stringBuilder.toString());
        int tempCount = i;
        JpegInfo ji = null;
        try {
            ji = (JpegInfo) this.mCB.mJpegQueue.poll(500, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
        }
        if (ji == null) {
            ji = this.lastJpegInfo;
        } else {
            this.lastJpegInfo = ji;
        }
        if (ji == null) {
            return -1;
        }
        int width;
        int i2;
        byte[] tempdata = ji.jpegData;
        Location location = ji.location;
        long date = ji.captureStartTime;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Snapshot_");
        stringBuilder2.append(CameraUtil.createJpegName(date));
        String title = stringBuilder2.toString();
        ExifInterface exif = Exif.getExif(tempdata);
        int orientation = Exif.getOrientation(exif);
        String mimeType = "image/jpeg";
        Map<String, Object> externalBundle = new HashMap();
        externalBundle.put(ExternalExifInterface.BURST_SHOT_ID, Integer.valueOf(this.mCB.hashCode()));
        externalBundle.put(ExternalExifInterface.BURST_SHOT_INDEX, Integer.valueOf(tempCount));
        exif.setTag(exif.buildTag(ExifInterface.TAG_USER_COMMENT, CameraUtil.serializeToJson(externalBundle)));
        if ((this.rotation + orientation) % MediaProviderUtils.ROTATION_180 == 0) {
            width = this.size.width;
            i2 = this.size.height;
        } else {
            width = this.size.height;
            i2 = this.size.width;
        }
        int width2 = width;
        int height = i2;
        String jpegPath = Storage.generateFilepath(title);
        Storage.writeFile(jpegPath, tempdata, exif);
        str = TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("savePicture node2  num = ");
        stringBuilder3.append(tempCount);
        Log.d(str, stringBuilder3.toString());
        Uri uri = null;
        try {
            uri = this.mContentResolver.insert(Media.EXTERNAL_CONTENT_URI, Storage.getContentValuesForData(title, date, location, orientation, tempdata.length, jpegPath, width2, height, "image/jpeg"));
        } catch (Throwable th) {
        }
        str = TAG;
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("savePicture node2  num = ");
        stringBuilder4.append(tempCount);
        Log.d(str, stringBuilder4.toString());
        if (uri != null) {
            str = TAG;
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("notifyNewMedia  num = ");
            stringBuilder4.append(tempCount);
            Log.d(str, stringBuilder4.toString());
            CameraUtil.broadcastNewPicture(this.mActivity.getAndroidContext(), uri);
            PhotoData.fromContentUri(this.mActivity.getContentResolver(), uri);
            this.mActivity.getCameraAppUI().updatePeekThumbUri(uri);
        }
        this.mSaveCaptureNum++;
        str = TAG;
        stringBuilder4 = new StringBuilder();
        stringBuilder4.append("savePicture node3  num = ");
        stringBuilder4.append(tempCount);
        Log.d(str, stringBuilder4.toString());
        if (this.mSaveCaptureNum >= this.mMaxCaptureNum) {
            str = TAG;
            stringBuilder4 = new StringBuilder();
            stringBuilder4.append("All picture saved and close snapShot = ");
            stringBuilder4.append(this.mSaveCaptureNum);
            Log.d(str, stringBuilder4.toString());
        }
        return this.mSaveCaptureNum;
    }

    private void showSavingHint(int count) {
        if (count != 0) {
            if (this.mProgressDialog == null) {
                this.mProgressDialog = new ProgressDialog(this.mActivity);
                this.mProgressDialog.setProgressStyle(0);
                this.mProgressDialog.setCancelable(false);
            }
            this.mProgressDialog.setMessage(String.format(this.mActivity.getAndroidContext().getResources().getString(R.string.burst_saving_hint), new Object[]{Integer.valueOf(count)}));
            this.mProgressDialog.show();
        }
    }

    private void dismissSavingHint() {
        if (this.mProgressDialog != null && this.mProgressDialog.isShowing()) {
            this.mProgressDialog.dismiss();
        }
    }
}
