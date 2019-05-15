package com.hmdglobal.app.camera;

import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.ui.HelpTipCling;

public abstract class HelpTip {
    public static final int CIRCLE = 0;
    public static final int HELP_TIP_SHOW_DELAY = 150;
    public static final int LINE = 2;
    public static final int NO_DRAW = -1;
    public static final int RECTANGLE = 1;
    public static final int SHOW_DELAY_TIME_MSG = 0;
    public static final Tag TAG = new Tag("HelpTip");
    protected final CameraActivity mActivity;
    protected int mCurTipGroupId = -1;
    protected int mCurTipId = -1;
    protected int mDrawType = -1;
    protected Handler mHandler = new MainHandler(Looper.getMainLooper());
    protected HelpTipCling mHelpTipCling;
    protected HelpTipController mHelpTipController;
    protected final LayoutInflater mInflater;
    protected boolean mIsShowExist = false;
    protected int mLayoutResId;
    protected boolean mLongClickAnimFucus = false;
    protected ImageView mRingAnimationImageView;
    protected ViewGroup mRootView;
    protected final SettingsManager mSettingsManager;
    protected ViewGroup mTipClingContentView;
    protected Button mTipNextButton;
    protected boolean mVideoReadyFlag = false;

    private class MainHandler extends Handler {
        public MainHandler(Looper mainLooper) {
            super(mainLooper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 0 && HelpTip.this.mIsShowExist) {
                HelpTip.this.showHelpTipCling();
            }
        }
    }

    public abstract void clickAnimFucus();

    public abstract void dismissHelpTip();

    public abstract void goToNextTip(boolean z);

    public abstract void initWidgets();

    public abstract void updateCurHelpTipStep(int i, boolean z);

    public HelpTip(int tipId, HelpTipController controller, CameraActivity activity) {
        this.mActivity = activity;
        this.mCurTipId = tipId;
        this.mHelpTipController = controller;
        this.mInflater = LayoutInflater.from(this.mActivity);
        this.mRootView = (ViewGroup) this.mActivity.findViewById(R.id.helptips_placeholder_wrapper);
        this.mSettingsManager = this.mActivity.getSettingsManager();
    }

    /* Access modifiers changed, original: protected */
    public boolean checkToIntercept() {
        if (this.mCurTipGroupId == 8) {
            return false;
        }
        return true;
    }

    /* Access modifiers changed, original: protected */
    public void notifyModeChanged() {
    }

    public void showHelpTipCling() {
    }

    public void doPause() {
        this.mCurTipGroupId = -1;
        this.mCurTipId = -1;
        cleanUpHelpTip();
        hideHelpTipCling();
        this.mIsShowExist = false;
    }

    public void hideHelpTipCling() {
        this.mRootView.setBackground(null);
        this.mRootView.setVisibility(8);
        this.mIsShowExist = false;
    }

    public void showDelayHelpTip() {
        this.mHandler.sendEmptyMessageDelayed(0, 150);
    }

    public void initCommomWidget() {
    }

    public int getCurTipGroupId() {
        return this.mCurTipGroupId;
    }

    public int getCurTipId() {
        return this.mCurTipId;
    }

    public boolean IsShowExist() {
        return this.mIsShowExist;
    }

    public void playAnimation() {
        this.mRingAnimationImageView = (ImageView) this.mRootView.findViewById(R.id.anim_focus);
        try {
            if (this.mRingAnimationImageView != null) {
                this.mRingAnimationImageView.setBackgroundResource(R.drawable.tutorial_shutter_animation);
                AnimationDrawable d = (AnimationDrawable) this.mRingAnimationImageView.getBackground();
                if (!(this.mRingAnimationImageView == null || d == null)) {
                    this.mRingAnimationImageView.setBackground(d);
                    d.start();
                }
                this.mRingAnimationImageView.setOnClickListener(null);
                this.mRingAnimationImageView.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        if (HelpTip.this.mCurTipId != 1 || !HelpTip.this.mLongClickAnimFucus) {
                            HelpTip.this.clickAnimFucus();
                        }
                    }
                });
                if (this.mCurTipId == 1) {
                    this.mRingAnimationImageView.setOnLongClickListener(new OnLongClickListener() {
                        public boolean onLongClick(View v) {
                            HelpTip.this.longClickAnimFucus();
                            return false;
                        }
                    });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("playAnimation OOM out of memory mCurTipId = ");
            stringBuilder.append(this.mCurTipId);
            Log.e(tag, stringBuilder.toString());
            if (this.mHelpTipCling != null) {
                closeAndFinishHelptip();
            }
        }
    }

    public void longClickAnimFucus() {
    }

    public void closeAndFinishHelptip() {
        this.mCurTipGroupId = -1;
        this.mCurTipId = -1;
        cleanUpHelpTip();
        hideHelpTipCling();
        this.mHelpTipController.notifyFinishHelpTip();
    }

    private void removeOverlayViews() {
        if (this.mRootView != null) {
            this.mRootView.removeAllViews();
            this.mRootView.setBackground(this.mActivity.getResources().getDrawable(R.color.tourial_semitransparent));
        }
    }

    /* Access modifiers changed, original: protected */
    public void cleanUpHelpTip() {
        if (this.mRingAnimationImageView != null) {
            AnimationDrawable ad = (AnimationDrawable) this.mRingAnimationImageView.getBackground();
            if (ad != null) {
                ad.stop();
            }
            this.mRingAnimationImageView.setBackground(null);
            this.mRingAnimationImageView.clearAnimation();
            this.mRingAnimationImageView.setOnClickListener(null);
        }
        if (this.mHelpTipCling != null) {
            this.mHelpTipCling.cleanDestroy();
            this.mHelpTipCling.removeAllViews();
        }
        this.mHelpTipCling = null;
        this.mRingAnimationImageView = null;
        removeOverlayViews();
        System.gc();
    }

    public void clickHitRectResponse(int index) {
    }

    public void setVideoReadyFlag(boolean videoReadyFlag) {
        this.mVideoReadyFlag = videoReadyFlag;
    }
}
