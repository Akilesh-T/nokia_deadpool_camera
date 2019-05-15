package com.hmdglobal.app.camera;

import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.android.ex.camera2.portability.CameraAgent.CameraShutterCallback;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.exif.ExifInterface;
import java.util.LinkedList;
import java.util.List;

public class OptimizeBurstPhotoModule extends NormalPhotoModule {
    private static final int BURST_ACTION_DISPLAY_DONE = 1;
    private static final int BURST_ACTION_FINISH = 3;
    private static final int BURST_ACTION_INIT = 0;
    private static final int BURST_ACTION_SNAP_DONE = 2;
    private static final int BURST_UPDATE_DELAY = 0;
    private static final int BURST_UPDATE_INTERVAL = 80;
    private static final String OPT_BURST_PHOTO_MODULE_STRING_ID = "OptBurstModule";
    private Tag TAG = new Tag(OPT_BURST_PHOTO_MODULE_STRING_ID);
    private int mBurstState = 0;
    private final Runnable mBurstUpdateRunnable = new Runnable() {
        public void run() {
            OptimizeBurstPhotoModule optimizeBurstPhotoModule = OptimizeBurstPhotoModule.this;
            optimizeBurstPhotoModule.mReceivedBurstNum++;
            OptimizeBurstPhotoModule.this.getPhotoUI().updateBurstCount(OptimizeBurstPhotoModule.this.mReceivedBurstNum);
            OptimizeBurstPhotoModule.this.checkBurstSaveQueue(OptimizeBurstPhotoModule.this.mReceivedBurstNum);
            if (OptimizeBurstPhotoModule.this.mReceivedBurstNum >= PhotoModule.BURST_MAX || !(OptimizeBurstPhotoModule.this.mCameraState == 8 || OptimizeBurstPhotoModule.this.mCameraState == 9)) {
                if (OptimizeBurstPhotoModule.this.mCameraState != 1 && OptimizeBurstPhotoModule.this.mSavingQueue.size() < OptimizeBurstPhotoModule.this.mReceivedBurstNum) {
                    OptimizeBurstPhotoModule.this.mSoundPlayer.stop(R.raw.camera_burst);
                    OptimizeBurstPhotoModule.this.showSavingHint(OptimizeBurstPhotoModule.this.mReceivedBurstNum);
                    OptimizeBurstPhotoModule.this.mHandler.postDelayed(new Runnable() {
                        public void run() {
                            if (!OptimizeBurstPhotoModule.this.isInBurstshot()) {
                                OptimizeBurstPhotoModule.this.getPhotoUI().updateBurstCount(0);
                            }
                        }
                    }, 80);
                }
                Log.v(OptimizeBurstPhotoModule.this.TAG, "onBurstStateUpdate BURST_ACTION_DISPLAY_DONE");
                OptimizeBurstPhotoModule.this.onBurstStateUpdate(1);
                return;
            }
            OptimizeBurstPhotoModule.this.mHandler.postDelayed(this, 80);
        }
    };
    private OptimizeLongshotPictureCallback mOptimizedLongshotCallback;
    private Handler mSaveQueueExecutorHandler;
    private HandlerThread mSaveQueueExecutorThread;
    private List<TaggedRunnable> mSavingQueue = new LinkedList();
    private final Runnable mSoundPlayRunnable = new Runnable() {
        public void run() {
            if (OptimizeBurstPhotoModule.this.mSoundPlayer != null) {
                OptimizeBurstPhotoModule.this.mSoundPlayer.play(R.raw.camera_burst, 1.0f, 98);
            }
        }
    };
    protected Handler mSoundPlayerHandler;
    private Object mSoundPlayerLock = new Object();
    private HandlerThread mSoundPlayerThread;

    private abstract class TaggedRunnable implements Runnable {
        protected boolean mIsJobInQueue;

        public abstract void run();

        private TaggedRunnable() {
            this.mIsJobInQueue = false;
        }

        /* synthetic */ TaggedRunnable(OptimizeBurstPhotoModule x0, AnonymousClass1 x1) {
            this();
        }

        public boolean isJobFinished() {
            return this.mIsJobInQueue;
        }

        public void tagJob() {
            this.mIsJobInQueue = true;
        }
    }

    private final class OptBurstShutterCallback implements CameraShutterCallback {
        private OptBurstShutterCallback() {
        }

        public void onShutter(CameraProxy camera) {
            Log.v(OptimizeBurstPhotoModule.this.TAG, "burst shot callback");
        }
    }

    protected class OptimizeLongshotPictureCallback extends LongshotPictureCallback {
        private int mDebugCount = 0;
        private int mOptimizedLongshotCount = 0;

        public OptimizeLongshotPictureCallback(Location loc) {
            super(loc);
        }

        public void onPictureTaken(byte[] originalJpegData, CameraProxy camera) {
            if (!OptimizeBurstPhotoModule.this.mPaused) {
                this.mOptimizedLongshotCount++;
                if (OptimizeBurstPhotoModule.this.mCameraState != 1) {
                    if (OptimizeBurstPhotoModule.this.mCameraState != 8 && OptimizeBurstPhotoModule.this.mCameraState != 9 && OptimizeBurstPhotoModule.this.mCameraState != 10) {
                        OptimizeBurstPhotoModule.this.stopBurst();
                    } else if (OptimizeBurstPhotoModule.this.mActivity.getStorageSpaceBytes() <= Storage.LOW_STORAGE_THRESHOLD_BYTES) {
                        OptimizeBurstPhotoModule.this.mVolumeButtonClickedFlag = false;
                        OptimizeBurstPhotoModule.this.stopBurst();
                    } else {
                        if (OptimizeBurstPhotoModule.this.mCameraState == 10 && OptimizeBurstPhotoModule.this.mSavingQueue.size() == 0) {
                            Log.v(OptimizeBurstPhotoModule.this.TAG, "onBurstStateUpdate BURST_ACTION_DISPLAY_DONE 2");
                            OptimizeBurstPhotoModule.this.onBurstStateUpdate(1);
                        } else if (OptimizeBurstPhotoModule.this.mCameraState == 8) {
                            if (OptimizeBurstPhotoModule.this.mSoundPlayer != null) {
                                OptimizeBurstPhotoModule.this.mSoundPlayer.play(R.raw.camera_burst, 1.0f, 98);
                            }
                            if (OptimizeBurstPhotoModule.this.mHandler != null) {
                                OptimizeBurstPhotoModule.this.mHandler.postDelayed(OptimizeBurstPhotoModule.this.mBurstUpdateRunnable, 0);
                            }
                            OptimizeBurstPhotoModule.this.setCameraState(9);
                        }
                        final byte[] bArr = originalJpegData;
                        final long currentTimeMillis = System.currentTimeMillis();
                        final CameraProxy cameraProxy = camera;
                        OptimizeBurstPhotoModule.this.mSavingQueue.add(new TaggedRunnable() {
                            public synchronized void run() {
                                OptimizeLongshotPictureCallback.this.mDebugCount = OptimizeLongshotPictureCallback.this.mDebugCount + 1;
                                Tag access$200 = OptimizeBurstPhotoModule.this.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("run tagged runnable :");
                                stringBuilder.append(OptimizeLongshotPictureCallback.this.mDebugCount);
                                Log.w(access$200, stringBuilder.toString());
                                final ExifInterface exif = Exif.getExif(bArr);
                                OptimizeBurstPhotoModule.this.mHandler.post(new Runnable() {
                                    public void run() {
                                        OptimizeBurstPhotoModule.this.updateThumbnail(exif);
                                    }
                                });
                                if (OptimizeBurstPhotoModule.this.mNamedImages != null) {
                                    OptimizeBurstPhotoModule.this.mNamedImages.nameNewImage(currentTimeMillis);
                                }
                                OptimizeLongshotPictureCallback.this.updateExifAndSave(exif, bArr, cameraProxy);
                            }
                        });
                        Tag access$200 = OptimizeBurstPhotoModule.this.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("saving queue size is ");
                        stringBuilder.append(OptimizeBurstPhotoModule.this.mSavingQueue.size());
                        stringBuilder.append(" longShot count is ");
                        stringBuilder.append(this.mOptimizedLongshotCount);
                        Log.w(access$200, stringBuilder.toString());
                        if ((OptimizeBurstPhotoModule.this.mSavingQueue.size() >= PhotoModule.BURST_MAX || (OptimizeBurstPhotoModule.this.mCameraState == 10 && OptimizeBurstPhotoModule.this.mSavingQueue.size() >= OptimizeBurstPhotoModule.this.mReceivedBurstNum)) && (OptimizeBurstPhotoModule.this.mBurstState & 2) == 0) {
                            OptimizeBurstPhotoModule.this.onBurstStateUpdate(2);
                        }
                    }
                }
            }
        }
    }

    public OptimizeBurstPhotoModule(AppController app) {
        super(app);
    }

    public boolean isFacebeautyEnabled() {
        return false;
    }

    /* Access modifiers changed, original: protected */
    public void showSavingHint(int count) {
    }

    private void checkBurstSaveQueue(int currentDisplayNum) {
        int jobNumToCheck = this.mSavingQueue.size() <= currentDisplayNum ? this.mSavingQueue.size() : currentDisplayNum;
        Tag tag = this.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("currentDisplayNum is  ");
        stringBuilder.append(currentDisplayNum);
        stringBuilder.append(" current saveQueueSize is ");
        stringBuilder.append(this.mSavingQueue.size());
        Log.v(tag, stringBuilder.toString());
        for (int i = 0; i < jobNumToCheck; i++) {
            final TaggedRunnable runnable = (TaggedRunnable) this.mSavingQueue.get(i);
            if (!runnable.isJobFinished()) {
                runnable.tagJob();
                this.mSaveQueueExecutorHandler.post(new Runnable() {
                    public void run() {
                        runnable.run();
                    }
                });
            }
        }
    }

    private void onBurstStateUpdate(int action) {
        Tag tag = this.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onBurstStateUpdate ,current Burst state is ");
        stringBuilder.append(this.mBurstState);
        stringBuilder.append(",Action is ");
        stringBuilder.append(action);
        Log.w(tag, stringBuilder.toString());
        this.mBurstState |= action;
        if (this.mBurstState != 1 || this.mSavingQueue.size() <= this.mReceivedBurstNum) {
            if (this.mBurstState == 3) {
                this.mBurstState = 0;
                checkBurstSaveQueue(this.mReceivedBurstNum);
                stopBurst();
            }
            return;
        }
        onBurstStateUpdate(2);
    }

    /* Access modifiers changed, original: protected */
    public boolean takeOptimizedBurstShot(Location loc) {
        return false;
    }

    public void resume() {
        this.mSaveQueueExecutorThread = new HandlerThread("OptBurstModule_saveExecutorThraed");
        this.mSaveQueueExecutorThread.start();
        this.mSaveQueueExecutorHandler = new Handler(this.mSaveQueueExecutorThread.getLooper());
        this.mSoundPlayerThread = new HandlerThread("OptBurstModule_soundPlayerThread");
        this.mSoundPlayerThread.start();
        this.mSoundPlayerHandler = new Handler(this.mSoundPlayerThread.getLooper());
        super.resume();
    }

    public void pause() {
        if (isInBurstshot()) {
            this.mHandler.removeCallbacks(this.mBurstUpdateRunnable);
            this.mSoundPlayerHandler.removeCallbacks(this.mSoundPlayRunnable);
            checkBurstSaveQueue(this.mReceivedBurstNum);
        }
        super.pause();
        if (this.mSaveQueueExecutorThread != null) {
            this.mSaveQueueExecutorThread.quitSafely();
            this.mSaveQueueExecutorHandler = null;
            this.mSaveQueueExecutorThread = null;
        }
        if (this.mSoundPlayerThread != null) {
            this.mSoundPlayerThread.quitSafely();
            this.mSoundPlayerThread = null;
            this.mSoundPlayerHandler = null;
        }
    }

    /* Access modifiers changed, original: protected */
    public void stopBurst() {
        Log.w(this.TAG, "stop burst ");
        resetHdrState();
        this.mSoundPlayer.stop(R.raw.camera_burst);
        this.mHandler.post(new Runnable() {
            public void run() {
                super.stopBurst();
            }
        });
    }

    /* Access modifiers changed, original: protected */
    public int getBurstShotMediaSaveAction() {
        return super.getBurstShotMediaSaveAction() | 4;
    }
}
