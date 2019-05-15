package com.hmdglobal.app.camera;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.ui.MicroVideoProgressBar;
import com.hmdglobal.app.camera.ui.ModuleLayoutWrapper;
import com.hmdglobal.app.camera.ui.PreviewOverlay;
import com.hmdglobal.app.camera.ui.StereoModeStripView;
import com.hmdglobal.app.camera.widget.MicroVideoGuideLayout;
import com.hmdglobal.app.camera.widget.MicroVideoGuideLayout.GuideSelectionListener;

public class MicroVideoUI extends VideoUI {
    private static final int MIN_CAMERA_LAUNCHING_TIMES = 3;
    private static final float PROGRESS_LOWER_BOUND = 3000.0f;
    private static final float PROGRESS_UPPER_BOUND = 15000.0f;
    private final Tag TAG = new Tag("MicroVideoUI");
    private final CameraActivity mActivity;
    private MicroVideoController mController;
    private MicroVideoGuideLayout mGuideLayout;
    private MicroVideoProgressBar mMicroVideoProgressbar;
    private TextView mMintimeTip;
    private StereoModeStripView mModeStripView;
    private PreviewOverlay mPreviewOverlay;
    private View mRemixButton;
    private View mSegmentRemoveButton;
    private TextView mShutterTip;

    public MicroVideoUI(CameraActivity activity, VideoController controller, View parent) {
        super(activity, controller, parent);
        this.mActivity = activity;
        this.mController = (MicroVideoController) controller;
        this.mMicroVideoProgressbar = (MicroVideoProgressBar) this.mRootView.findViewById(R.id.micro_video_progressbar);
        this.mSegmentRemoveButton = this.mRootView.findViewById(R.id.button_segement_remove);
        this.mSegmentRemoveButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                MicroVideoUI.this.mController.onSegmentRemoveClicked();
            }
        });
        this.mRemixButton = this.mRootView.findViewById(R.id.button_remix);
        this.mRemixButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                MicroVideoUI.this.mController.onRemixClicked();
            }
        });
        this.mMicroVideoProgressbar.setProgressUpperBound(PROGRESS_UPPER_BOUND);
        this.mMicroVideoProgressbar.setProgressLowerBound(PROGRESS_LOWER_BOUND);
        this.mPreviewOverlay = (PreviewOverlay) this.mRootView.findViewById(R.id.preview_overlay);
        this.mModeStripView = (StereoModeStripView) this.mRootView.findViewById(R.id.mode_strip_view);
        if (Keys.isShowMicroGuide(this.mActivity.getSettingsManager()) && Keys.isNewLaunchingForMicroguide(this.mActivity.getSettingsManager())) {
            this.mActivity.getLayoutInflater().inflate(R.layout.microvideo_guide_layout, (ModuleLayoutWrapper) this.mRootView.findViewById(R.id.module_layout), true);
            this.mGuideLayout = (MicroVideoGuideLayout) this.mRootView.findViewById(R.id.micro_video_guide_layout);
            this.mGuideLayout.changeVisibility(0);
            this.mGuideLayout.setGuideSelectionListener(new GuideSelectionListener() {
                public void onGuideSelected(boolean show) {
                    if (!show) {
                        Keys.setMicroGuide(MicroVideoUI.this.mActivity.getSettingsManager(), show);
                    }
                    MicroVideoUI.this.mGuideLayout.changeVisibility(8);
                    MicroVideoUI.this.enableMicroIcons();
                    MicroVideoUI.this.initializeShutterTip();
                    Keys.setNewLaunchingForMicroguide(MicroVideoUI.this.mActivity.getSettingsManager(), false);
                }
            });
        }
        this.mMicroVideoProgressbar.setProgressUpperBound(PROGRESS_UPPER_BOUND);
        this.mMintimeTip = (TextView) this.mRootView.findViewById(R.id.micro_minimum_time_tip);
        this.mShutterTip = (TextView) this.mRootView.findViewById(R.id.micro_shoot_help_tip);
        if (!isMircoGuideShow()) {
            initializeShutterTip();
        }
    }

    private void initializeShutterTip() {
        int launchingTimes = this.mActivity.getSettingsManager().getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_NEW_LAUNCHING_TIMES_FOR_MICROTIP).intValue();
        boolean isMicroTipShowWhenPreview = launchingTimes < 3 || (launchingTimes == 3 && !Keys.isNewLaunchingForMicrotip(this.mActivity.getSettingsManager()));
        if (isMicroTipShowWhenPreview) {
            if (Keys.isNewLaunchingForMicrotip(this.mActivity.getSettingsManager())) {
                this.mActivity.getSettingsManager().setValueByIndex(SettingsManager.SCOPE_GLOBAL, Keys.KEY_NEW_LAUNCHING_TIMES_FOR_MICROTIP, launchingTimes + 1);
                Keys.setNewLaunchingForMicrotip(this.mActivity.getSettingsManager(), false);
            }
            showShutterTip();
        }
    }

    public void disableMicroVideoButton() {
        this.mSegmentRemoveButton.setEnabled(false);
        this.mRemixButton.setEnabled(false);
    }

    public void disableRemixButton() {
        this.mRemixButton.setEnabled(false);
    }

    public void enableMicroVideoButton() {
        this.mSegmentRemoveButton.setEnabled(true);
        this.mRemixButton.setEnabled(true);
    }

    public void updateMicroVideoProgress(float progress) {
        this.mMicroVideoProgressbar.updateProgress(progress);
        if (progress > 0.0f) {
            this.mActivity.getCameraAppUI().setModeSwitchUIVisibility(false);
            this.mMicroVideoProgressbar.setVisibility(0);
        }
    }

    public void markSegment(float duration) {
        Tag tag = this.TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("markSegment, duration is ");
        stringBuilder.append(duration);
        Log.w(tag, stringBuilder.toString());
        this.mMicroVideoProgressbar.markSegmentStart(duration);
    }

    public float getSumDuration() {
        return this.mMicroVideoProgressbar.getSumDuration();
    }

    public int segmentRemoveOnProgress() {
        Log.w(this.TAG, "segmentRemoveOnProgress");
        int removedProgress = (int) this.mMicroVideoProgressbar.segmentRemove();
        if (getSumDuration() == 0.0f) {
            this.mActivity.getCameraAppUI().setModeSwitchUIVisibility(true);
            this.mMicroVideoProgressbar.setVisibility(8);
        }
        return removedProgress;
    }

    public void changeLastSegmentColor() {
        Log.w(this.TAG, "changeLastSegmentColor");
        this.mMicroVideoProgressbar.changeLastSegmentColor();
    }

    public void clearPendingProgress() {
        this.mMicroVideoProgressbar.clearPendingProgress();
    }

    public void resetProgress() {
        Log.w(this.TAG, "resetProgress");
        this.mMicroVideoProgressbar.clearProgress();
        this.mMicroVideoProgressbar.setVisibility(8);
        this.mActivity.getCameraAppUI().setModeSwitchUIVisibility(true);
    }

    public boolean isMircoGuideShow() {
        return this.mGuideLayout != null && this.mGuideLayout.getVisibility() == 0;
    }

    public void disableMicroIcons() {
        if (isMircoGuideShow()) {
            this.mActivity.getLockEventListener().onModeSwitching();
            this.mPreviewOverlay.setTouchEnabled(false);
        }
    }

    public void enableMicroIcons() {
        this.mActivity.getLockEventListener().onIdle();
        this.mPreviewOverlay.setTouchEnabled(true);
    }

    public void showRecordingUI(boolean recording) {
        super.showRecordingUI(false);
    }

    public void showMintimeTip() {
        if (this.mMintimeTip != null) {
            this.mMintimeTip.setVisibility(0);
        }
        hideShutterTip();
    }

    public void hideMintimeTip() {
        if (this.mMintimeTip != null) {
            this.mMintimeTip.setVisibility(8);
        }
    }

    public void showShutterTip() {
        if (this.mShutterTip != null) {
            this.mShutterTip.setVisibility(0);
        }
        hideMintimeTip();
    }

    public void hideShutterTip() {
        if (this.mShutterTip != null) {
            this.mShutterTip.setVisibility(8);
        }
    }

    public void onPause() {
        super.onPause();
        if (this.mGuideLayout != null) {
            this.mGuideLayout.stopPlaying();
        }
    }

    public void playMicroGuide() {
        if (this.mGuideLayout != null) {
            this.mGuideLayout.startPlaying();
        }
    }
}
