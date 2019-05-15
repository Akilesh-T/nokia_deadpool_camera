package com.hmdglobal.app.camera;

import android.app.ProgressDialog;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore.Video.Media;
import android.view.KeyEvent;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.app.CameraAppUI.BottomBarUISpec;
import com.hmdglobal.app.camera.app.MediaSaver.OnMediaSavedListener;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.decoder.MicroVideoRemixer;
import com.hmdglobal.app.camera.decoder.Remixer;
import com.hmdglobal.app.camera.decoder.Remixer.RemixProgressListener;
import com.hmdglobal.app.camera.motion.MotionPictureHelper;
import com.hmdglobal.app.camera.util.CameraUtil;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class MicroVideoModule extends VideoModule implements MicroVideoController {
    private static final Uri CONTENT_URI = Media.EXTERNAL_CONTENT_URI;
    private static final int MAX_DURATION_FOR_MICROVIDEO = 15000;
    private static final String MICROVIDEO_MODULE_STRING_ID = "MicroVideoModule";
    private static final float MIN_VIDEO_DURATION_LIMIT = 1000.0f;
    private static final float PROGRESS_LOWER_BOUND = 3000.0f;
    private static final int PROGRESS_START_DELAY = 800;
    private static final int PROGRESS_UPDATE_DELAY = 100;
    private static final float PROGRESS_UPPER_BOUND = 15000.0f;
    private static final int VIDEO_ORIENTATION = 90;
    private Tag TAG = new Tag(CameraMode.MICRO_VIDEO);
    private boolean mIsMaxVideoProgress;
    private boolean mIsMicroVideoSegmentAvailable = false;
    private boolean mIsProgressMaxAuto = false;
    private Integer mModeSelectionLock = null;
    private boolean mNeedAddToStore = false;
    private final OnMediaSavedListener mOnVideoSavedListener = new OnMediaSavedListener() {
        public void onMediaSaved(Uri uri) {
            if (MicroVideoModule.this.mNeedAddToStore) {
                MicroVideoModule.this.mCurrentVideoUri = uri;
                MicroVideoModule.this.mCurrentVideoUriFromMediaSaved = true;
                MicroVideoModule.this.onVideoSaved();
                MicroVideoModule.this.dismissRemixHint();
                MicroVideoModule.this.mAppController.getCameraAppUI().hideMicroVideoEditButtons(false);
                MicroVideoModule.this.mActivity.notifyNewMedia(uri);
                MicroVideoModule.this.mNeedAddToStore = false;
                for (String path : MicroVideoModule.this.mRemixVideoPath) {
                    MicroVideoModule.this.removeFileInStorage(path);
                }
                MicroVideoModule.this.mRemixVideoPath.clear();
                MicroVideoModule.this.mIsMicroVideoSegmentAvailable = false;
                MicroVideoModule.this.checkMicroVideoState();
                MicroVideoModule.this.mActivity.onPeekThumbClicked(MicroVideoModule.this.mCurrentVideoUri);
            } else {
                MicroVideoModule.this.mUI.enableMicroVideoButton();
                String videoFileName = MicroVideoModule.this.mCurrentVideoFilename.replace(".tmp", "");
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(videoFileName);
                String durationStr = mmr.extractMetadata(9);
                try {
                    mmr.release();
                } catch (RuntimeException e) {
                }
                float videoDuration = Float.parseFloat(durationStr);
                if (videoDuration < MicroVideoModule.MIN_VIDEO_DURATION_LIMIT) {
                    MicroVideoModule.this.onRecordingUnreasonably();
                    MicroVideoModule.this.removeFileInStorage(videoFileName);
                    if (MicroVideoModule.this.mRemixVideoPath.size() == 0) {
                        MicroVideoModule.this.mIsMicroVideoSegmentAvailable = false;
                        MicroVideoModule.this.checkMicroVideoState();
                        MicroVideoModule.this.mUI.resetProgress();
                    }
                    return;
                }
                if (MicroVideoModule.this.mRemixVideoPath.size() == 0) {
                    MicroVideoModule.this.mPendingOrientation = MicroVideoModule.this.getMediaRecorderRotation();
                }
                MicroVideoModule.access$824(MicroVideoModule.this, videoDuration);
                MicroVideoModule.this.mRemixVideoPath.add(videoFileName);
                MicroVideoModule.this.mUI.markSegment(videoDuration);
                MicroVideoModule.this.mAppController.getCameraAppUI().showMicroVideoEditButtons(true);
                Tag access$900 = MicroVideoModule.this.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("remaining duration is ");
                stringBuilder.append(MicroVideoModule.this.mRemainingProgress);
                Log.w(access$900, stringBuilder.toString());
            }
        }
    };
    private boolean mPaused;
    private int mPendingOrientation;
    ProgressDialog mProgressDialog;
    private long mRecordingStartTime = 0;
    private int mRemainingProgress = MAX_DURATION_FOR_MICROVIDEO;
    private final RemixProgressListener mRemixProgressListener = new RemixProgressListener() {
        public void onRemixDone() {
            MicroVideoModule.this.mNeedAddToStore = true;
            MicroVideoModule.this.saveVideo();
            MicroVideoModule.this.releaseRemixer();
            MicroVideoModule.this.mRemainingProgress = MicroVideoModule.MAX_DURATION_FOR_MICROVIDEO;
            MicroVideoModule.this.mHandler.post(new Runnable() {
                public void run() {
                    MicroVideoModule.this.mUI.resetProgress();
                }
            });
        }
    };
    private List<String> mRemixVideoPath = new LinkedList();
    Remixer mRemixer;
    private int mRemoveClickTimes;
    private MicroVideoUI mUI;
    private boolean mVolumeKeyLongPressed;
    final Runnable updateProgressRunnable = new Runnable() {
        public void run() {
            float sumDuration = MicroVideoModule.this.mUI.getSumDuration();
            long currentTime = System.currentTimeMillis();
            if (MicroVideoModule.this.mRecordingStartTime == 0) {
                Log.w(MicroVideoModule.this.TAG, "initialize recording start time");
                MicroVideoModule.this.mRecordingStartTime = currentTime;
            }
            float progress = ((float) (currentTime - MicroVideoModule.this.mRecordingStartTime)) + sumDuration;
            if (progress < MicroVideoModule.PROGRESS_UPPER_BOUND && MicroVideoModule.this.mIsMaxVideoProgress) {
                MicroVideoModule.this.mIsMaxVideoProgress = false;
            } else if (progress >= MicroVideoModule.PROGRESS_UPPER_BOUND && !MicroVideoModule.this.mIsMaxVideoProgress) {
                if (MicroVideoModule.this.mRemixVideoPath.size() == 0) {
                    MicroVideoModule.this.mNeedAddToStore = true;
                }
                MicroVideoModule.this.mAppController.getCameraAppUI().enableModeOptions();
                MicroVideoModule.this.mAppController.setShutterEnabled(false);
                boolean recordFail = MicroVideoModule.this.onStopVideoRecording();
                MicroVideoModule.this.mIsMaxVideoProgress = true;
                MicroVideoModule.this.mRecordingStartTime = 0;
                Tag access$900 = MicroVideoModule.this.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("video time is maximum : ");
                stringBuilder.append(MicroVideoModule.this.mIsMaxVideoProgress);
                Log.w(access$900, stringBuilder.toString());
                if (recordFail) {
                    MicroVideoModule.this.onRecordingUnreasonably();
                } else {
                    MicroVideoModule.this.mIsProgressMaxAuto = true;
                    MicroVideoModule.this.onRemixClicked();
                }
            } else if (MicroVideoModule.this.isRecording()) {
                MicroVideoModule.this.mUI.updateMicroVideoProgress(progress);
                MicroVideoModule.this.mHandler.postDelayed(this, 100);
            } else {
                MicroVideoModule.this.mRecordingStartTime = 0;
            }
        }
    };

    static /* synthetic */ int access$824(MicroVideoModule x0, float x1) {
        int i = (int) (((float) x0.mRemainingProgress) - x1);
        x0.mRemainingProgress = i;
        return i;
    }

    public MicroVideoModule(AppController app) {
        super(app);
    }

    /* Access modifiers changed, original: protected */
    public VideoUI getVideoUI() {
        this.mUI = new MicroVideoUI(this.mActivity, this, this.mActivity.getModuleLayoutRoot());
        return this.mUI;
    }

    /* Access modifiers changed, original: protected */
    public boolean isNeedStartRecordingOnSwitching() {
        return false;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (this.mPaused) {
            return true;
        }
        if (keyCode != CameraUtil.BOOM_KEY) {
            switch (keyCode) {
                case 24:
                case 25:
                    break;
                default:
                    return false;
            }
        }
        if (this.mUI.isMircoGuideShow()) {
            return false;
        }
        if (event.isLongPress() && !this.mActivity.mDuringCall) {
            this.mVolumeKeyLongPressed = true;
            this.mAppController.setShutterPress(true);
            onShutterButtonLongClick();
        }
        return true;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode != CameraUtil.BOOM_KEY) {
            switch (keyCode) {
                case 24:
                case 25:
                    break;
                default:
                    return false;
            }
        }
        if (this.mUI.isMircoGuideShow()) {
            return false;
        }
        if (this.mVolumeKeyLongPressed && !this.mActivity.mDuringCall) {
            this.mVolumeKeyLongPressed = false;
            this.mAppController.setShutterPress(false);
        }
        return true;
    }

    /* Access modifiers changed, original: protected */
    public OnMediaSavedListener getVideoSavedListener() {
        return this.mOnVideoSavedListener;
    }

    private final void segmentRemove(int index) {
        if (index < this.mRemixVideoPath.size()) {
            removeFileInStorage((String) this.mRemixVideoPath.remove(index));
            if (this.mRemixVideoPath.size() == 0) {
                this.mAppController.getCameraAppUI().hideMicroVideoEditButtons(true);
                this.mPendingOrientation = 0;
                this.mUI.hideMintimeTip();
            }
        }
    }

    public void resume() {
        super.resume();
        this.mPaused = false;
        if (!checkMediaFileValidation()) {
            this.mRemixVideoPath.clear();
            this.mUI.resetProgress();
            this.mAppController.getCameraAppUI().hideMicroVideoEditButtons(false);
            this.mRemainingProgress = MAX_DURATION_FOR_MICROVIDEO;
            this.mIsMicroVideoSegmentAvailable = false;
        }
        checkMicroVideoState();
        this.mUI.disableMicroIcons();
        this.mUI.playMicroGuide();
    }

    private boolean checkMediaFileValidation() {
        if (this.mRemixVideoPath.size() == 0) {
            return false;
        }
        for (String path : this.mRemixVideoPath) {
            if (!new File(path).exists()) {
                return false;
            }
        }
        return true;
    }

    public void pause() {
        recoverFromRecording();
        dismissRemixHint();
        this.mProgressDialog = null;
        this.mPaused = true;
        this.mUI.hideMintimeTip();
        this.mUI.hideShutterTip();
        this.mUI.enableMicroIcons();
        this.mRemoveClickTimes = 0;
        super.pause();
        if (this.mModeSelectionLock != null) {
            this.mActivity.unlockModuleSelection(this.mModeSelectionLock);
            this.mModeSelectionLock = null;
            this.mActivity.getButtonManager().showSettings();
        }
    }

    private void removeFileInStorage(String path) {
        File file = new File(path);
        if (file.exists()) {
            this.mContentResolver.delete(CONTENT_URI, "_data=?", new String[]{path});
            file.delete();
        }
    }

    /* Access modifiers changed, original: protected */
    public boolean needAddToMediaSaver() {
        return true;
    }

    public String getModuleStringIdentifier() {
        return MICROVIDEO_MODULE_STRING_ID;
    }

    public int getModuleId() {
        return this.mAppController.getAndroidContext().getResources().getInteger(R.integer.camera_mode_micro_video);
    }

    private void initRemixer() {
        this.mRemixer = new MicroVideoRemixer();
        this.mRemixer.setRemxingProgressListener(this.mRemixProgressListener);
    }

    private void releaseRemixer() {
        this.mRemixer.releaseRemixer();
    }

    /* Access modifiers changed, original: protected|final */
    public final int getOverrodeVideoDuration() {
        Tag tag = this.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("max duration for video recording is ");
        stringBuilder.append(this.mRemainingProgress);
        Log.w(tag, stringBuilder.toString());
        return this.mRemainingProgress;
    }

    private void startRemix() {
        if (this.mRemixVideoPath.size() != 0) {
            generateVideoFilename(this.mProfile.fileFormat);
            this.mRemixer.setDisplayOrientation(this.mPendingOrientation);
            this.mRemixer.prepareForRemixer(this.mCurrentVideoFilename, this.mRemixVideoPath);
            this.mRemixer.startRemix();
        }
    }

    /* Access modifiers changed, original: protected */
    public void onVideoRecordingStarted() {
        this.updateProgressRunnable.run();
    }

    public void onShutterButtonFocus(boolean pressed) {
        if (!this.mSwitchingCamera) {
            if (!pressed) {
                recoverFromRecording();
            }
            if (this.mCameraSettings != null) {
                this.mFocusManager.onShutterUp(this.mCameraSettings.getCurrentFocusMode());
            }
        }
    }

    private void recoverFromRecording() {
        if (this.mCameraState == 3 || this.mCameraState == 2) {
            this.mAppController.getCameraAppUI().enableModeOptions();
            this.mAppController.setShutterEnabled(true);
            if (this.mCameraState == 3) {
                this.mActivity.getButtonManager().hideSettings();
                if (onStopVideoRecording()) {
                    onRecordingUnreasonably();
                } else {
                    this.mIsMicroVideoSegmentAvailable = true;
                    checkMicroVideoState();
                    return;
                }
            }
            this.mUI.enableMicroVideoButton();
        }
    }

    public void onShutterButtonLongClick() {
        if (!this.mSwitchingCamera && this.mCameraState == 1) {
            this.mAppController.setShutterEnabled(false);
            this.mUI.disableMicroVideoButton();
            this.mAppController.getCameraAppUI().disableModeOptions();
            this.mUI.hideMintimeTip();
            this.mUI.hideShutterTip();
            this.mRemoveClickTimes = 0;
            if (this.mRemixVideoPath.size() == 0) {
                this.mAppController.getCameraAppUI().animateHidePeek();
            }
            startVideoRecording();
            if (this.mCameraSettings != null) {
                this.mFocusManager.onShutterUp(this.mCameraSettings.getCurrentFocusMode());
            }
        }
    }

    /* Access modifiers changed, original: protected */
    public void pendingRecordFailed() {
        onShutterButtonFocus(false);
        super.pendingRecordFailed();
    }

    public void onShutterButtonClick() {
        this.mUI.showShutterTip();
    }

    public boolean onBackPressed() {
        recoverFromRecording();
        for (String path : this.mRemixVideoPath) {
            removeFileInStorage(path);
        }
        return super.onBackPressed();
    }

    /* Access modifiers changed, original: protected */
    public boolean isVideoShutterAnimationEnssential() {
        return false;
    }

    public void onSegmentRemoveClicked() {
        if (this.mRemixVideoPath.size() != 0) {
            if (this.mRemoveClickTimes == 0) {
                this.mRemoveClickTimes++;
                this.mUI.changeLastSegmentColor();
                return;
            }
            this.mRemainingProgress += this.mUI.segmentRemoveOnProgress();
            segmentRemove(this.mRemixVideoPath.size() - 1);
            this.mRemoveClickTimes = 0;
            if (this.mRemixVideoPath.size() == 0) {
                this.mIsMicroVideoSegmentAvailable = false;
                checkMicroVideoState();
            }
        }
    }

    public void onRemixClicked() {
        Log.w(this.TAG, "onRemixClicked");
        if (this.mRemixVideoPath.size() <= 0 || this.mUI.getSumDuration() >= PROGRESS_LOWER_BOUND || this.mIsProgressMaxAuto) {
            initRemixer();
            startRemix();
            if (this.mRemixVideoPath.size() > 0 || this.mIsProgressMaxAuto) {
                this.mUI.disableMicroVideoButton();
                showRemixHint();
                if (this.mIsProgressMaxAuto) {
                    this.mIsProgressMaxAuto = false;
                }
            }
            this.mPendingOrientation = 0;
            return;
        }
        this.mUI.disableRemixButton();
        this.mUI.showMintimeTip();
    }

    private void showRemixHint() {
        if (this.mProgressDialog == null) {
            this.mProgressDialog = new ProgressDialog(this.mActivity);
            this.mProgressDialog.setCancelable(false);
            this.mProgressDialog.setMessage(this.mActivity.getResources().getString(R.string.micro_video_remix_hint));
        }
        this.mProgressDialog.show();
    }

    private void dismissRemixHint() {
        if (this.mProgressDialog != null) {
            this.mProgressDialog.dismiss();
        }
    }

    private void onRecordingUnreasonably() {
        Log.w(this.TAG, "stop recording fail or video time is less than 1s");
        if (this.mRemixVideoPath.size() == 0) {
            this.mAppController.getCameraAppUI().setModeSwitchUIVisibility(true);
            this.mAppController.getCameraAppUI().hideMicroVideoEditButtons(true);
        }
        this.mUI.showShutterTip();
        this.mUI.clearPendingProgress();
    }

    /* Access modifiers changed, original: protected */
    public void overrideProfileSize() {
        this.mProfile.videoFrameWidth = MotionPictureHelper.FRAME_HEIGHT_9;
        this.mProfile.videoFrameHeight = MotionPictureHelper.FRAME_HEIGHT_9;
    }

    public boolean updateModeSwitchUIinModule() {
        return false;
    }

    /* Access modifiers changed, original: protected */
    public boolean shouldHoldRecorderForSecond() {
        return false;
    }

    /* Access modifiers changed, original: protected */
    public void onPreviewStarted() {
        super.onPreviewStarted();
        this.mUI.disableMicroIcons();
    }

    /* Access modifiers changed, original: protected */
    public void setCameraState(int state) {
        super.setCameraState(state);
        checkMicroVideoState();
    }

    private void checkMicroVideoState() {
        if (this.mIsMicroVideoSegmentAvailable) {
            if (this.mModeSelectionLock == null) {
                this.mModeSelectionLock = this.mActivity.lockModuleSelection();
            }
        } else if (this.mModeSelectionLock != null) {
            this.mActivity.unlockModuleSelection(this.mModeSelectionLock);
            this.mModeSelectionLock = null;
        }
        Tag tag = this.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("is segment available ");
        stringBuilder.append(this.mIsMicroVideoSegmentAvailable);
        Log.v(tag, stringBuilder.toString());
        this.mAppController.getCameraAppUI().applyModuleSpecs(getHardwareSpec(), getBottomBarSpec());
    }

    public BottomBarUISpec getBottomBarSpec() {
        BottomBarUISpec bottomBarSpec = super.getBottomBarSpec();
        if (this.mIsMicroVideoSegmentAvailable) {
            bottomBarSpec.hideSetting = true;
            bottomBarSpec.hideCamera = true;
            bottomBarSpec.setCameraInvisible = true;
        }
        return bottomBarSpec;
    }

    public void destroy() {
        Log.w(this.TAG, "destory microvideo");
        recoverFromRecording();
        for (String path : this.mRemixVideoPath) {
            removeFileInStorage(path);
        }
        super.destroy();
    }

    /* Access modifiers changed, original: protected */
    public boolean isSendMsgEnableShutterButton() {
        return false;
    }
}
