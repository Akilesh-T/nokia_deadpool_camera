package com.hmdglobal.app.camera;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.ui.PeekImageView;
import com.hmdglobal.app.camera.ui.RotatableButton;
import java.util.ArrayList;
import java.util.List;

public class MultiHelpTip extends HelpTip implements OnClickListener {
    private static final int[] WELCOME_GROUP_LAYOUT_IDS = new int[]{R.layout.welcome_tip, R.layout.snap_tip, R.layout.camera_key_tip, R.layout.recent_tip, R.layout.video_tip};
    private static boolean mJumpCameraKeyTip = false;
    private Button mCanCelTourBtn;
    private PeekImageView mPeekThumb;
    private ShutterButton mShutterButton;
    private Button mTakeTourBtn;
    private RotatableButton mVideoShutterButton;
    private LinearLayout mWelcomeLayout;
    List<Integer> mWelcomeTipList = new ArrayList();

    public MultiHelpTip(int groupId, int tipId, HelpTipController controller, CameraActivity activity) {
        super(tipId, controller, activity);
        this.mCurTipGroupId = groupId;
        this.mLayoutResId = WELCOME_GROUP_LAYOUT_IDS[tipId];
        this.mWelcomeTipList = new ArrayList();
        this.mWelcomeTipList.add(Integer.valueOf(0));
        this.mWelcomeTipList.add(Integer.valueOf(1));
        this.mWelcomeTipList.add(Integer.valueOf(2));
        this.mWelcomeTipList.add(Integer.valueOf(3));
        this.mWelcomeTipList.add(Integer.valueOf(4));
    }

    /* Access modifiers changed, original: protected */
    public void initWidgets() {
        boolean bNeedinitCommom = true;
        if (this.mCurTipId == 0) {
            bNeedinitCommom = false;
        }
        if (bNeedinitCommom) {
            initCommomWidget();
        }
        switch (this.mCurTipId) {
            case 0:
                this.mDrawType = -1;
                this.mTakeTourBtn = (Button) this.mHelpTipCling.findViewById(R.id.take_tour_btn);
                this.mCanCelTourBtn = (Button) this.mHelpTipCling.findViewById(R.id.cancel_tour_btn);
                this.mWelcomeLayout = (LinearLayout) this.mHelpTipCling.findViewById(R.id.take_tour_layout);
                this.mTakeTourBtn.setOnClickListener(this);
                this.mCanCelTourBtn.setOnClickListener(this);
                break;
            case 1:
                this.mShutterButton = (ShutterButton) this.mActivity.findViewById(R.id.shutter_button);
                this.mDrawType = 0;
                playAnimation();
                break;
            case 2:
                this.mShutterButton = (ShutterButton) this.mActivity.findViewById(R.id.shutter_button);
                this.mDrawType = -1;
                break;
            case 3:
                this.mPeekThumb = (PeekImageView) this.mActivity.findViewById(R.id.peek_thumb);
                this.mDrawType = 0;
                playAnimation();
                break;
            case 4:
                this.mVideoShutterButton = (RotatableButton) this.mActivity.findViewById(R.id.video_shutter_button);
                this.mDrawType = 0;
                playAnimation();
                break;
        }
        this.mHelpTipCling.setListener(this, this.mDrawType);
    }

    /* Access modifiers changed, original: protected */
    public void goToNextTip(boolean dismiss) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Tony before dismiss goToNextTip mCurTipId = ");
        stringBuilder.append(this.mCurTipId);
        stringBuilder.append("dismiss =");
        stringBuilder.append(dismiss);
        stringBuilder.append(",mJumpCameraKeytip = ");
        stringBuilder.append(mJumpCameraKeyTip);
        Log.e(tag, stringBuilder.toString());
        if (dismiss) {
            if (this.mCurTipId == 2) {
                this.mCurTipId = 4;
                this.mVideoReadyFlag = true;
            } else if (this.mCurTipId != 3) {
                this.mCurTipId++;
            } else if (mJumpCameraKeyTip) {
                this.mCurTipId = 2;
            } else {
                this.mVideoReadyFlag = true;
                this.mCurTipId++;
            }
        } else if (this.mCurTipId == 1) {
            mJumpCameraKeyTip = true;
            this.mCurTipId = 3;
        } else if (this.mCurTipId != 2) {
            this.mCurTipId++;
        } else if (mJumpCameraKeyTip) {
            this.mCurTipId = 4;
            this.mVideoReadyFlag = true;
        } else {
            this.mCurTipId++;
        }
        tag = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Tony after dismiss goToNextTip mCurTipId = ");
        stringBuilder.append(this.mCurTipId);
        stringBuilder.append("dismiss =");
        stringBuilder.append(dismiss);
        stringBuilder.append(",mJumpCameraKeyTip = ");
        stringBuilder.append(mJumpCameraKeyTip);
        Log.e(tag, stringBuilder.toString());
        if (this.mWelcomeTipList == null || this.mCurTipId > this.mWelcomeTipList.size() - 1) {
            updateCurHelpTipStep(this.mCurTipId - 1, true);
            closeAndFinishHelptip();
            if (dismiss) {
                this.mHelpTipController.checkAlarmTaskHelpTip();
                return;
            }
            return;
        }
        cleanUpHelpTip();
        showDelayHelpTip();
    }

    public void showDelayHelpTip() {
        this.mLayoutResId = WELCOME_GROUP_LAYOUT_IDS[this.mCurTipId];
        this.mIsShowExist = true;
        updateCurHelpTipStep(this.mCurTipId, false);
        super.showDelayHelpTip();
        if (this.mCurTipId == 3) {
            this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_RECENT_FINISHED, true);
        }
    }

    /* Access modifiers changed, original: protected */
    public void updateCurHelpTipStep(int tipId, boolean isOver) {
        if (isOver) {
            this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_WELCOME_FINISHED, true);
        } else {
            this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_WELCOME_STEP, tipId);
        }
    }

    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.anim_focus) {
            clickAnimFucus();
        } else if (id == R.id.cancel_tour_btn) {
            updateSettingsAllTipsFinished();
            closeAndFinishHelptip();
        } else if (id == R.id.take_tour_btn) {
            goToNextTip(false);
            this.mWelcomeLayout.setVisibility(8);
        }
    }

    public void updateSettingsAllTipsFinished() {
        this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_WELCOME_FINISHED, true);
        this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_PANO_FINISHED, true);
        this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_MANUAL_FINISHED, true);
        this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_PINCH_ZOOM_FINISHED, true);
        this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_QUICK_SETTINGS_FINISHED, true);
        this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_SETTINGS_FINISHED, true);
        this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_FRONT_CAMERA_FINISHED, true);
        this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_GESTURE_FINISHED, true);
        this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_MODE_FINISHED, true);
        this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_STOP_VIDEO_FINISHED, true);
        this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_VIDEO_SNAP_FINISHED, true);
        this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_RECENT_FINISHED, true);
    }

    /* Access modifiers changed, original: protected */
    public void clickAnimFucus() {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("clickAnimFucus mCurTipGroupId =");
        stringBuilder.append(this.mCurTipGroupId);
        stringBuilder.append(",mCurTipId = ");
        stringBuilder.append(this.mCurTipId);
        Log.i(tag, stringBuilder.toString());
        int i = this.mCurTipId;
        boolean z = true;
        if (i != 1) {
            switch (i) {
                case 3:
                    if (this.mPeekThumb != null) {
                        this.mPeekThumb.performClick();
                        if (mJumpCameraKeyTip) {
                            updateCurHelpTipStep(2, false);
                        } else {
                            updateCurHelpTipStep(this.mCurTipId + 1, false);
                        }
                        closeAndFinishHelptip();
                        return;
                    }
                    return;
                case 4:
                    tag = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("MultiHelptip HelpTipsManager.VIDEO_TIP mVideoReadyFlag=");
                    stringBuilder2.append(this.mVideoReadyFlag);
                    stringBuilder2.append(",mVideoShutterButton=");
                    if (this.mVideoShutterButton == null) {
                        z = false;
                    }
                    stringBuilder2.append(z);
                    Log.d(tag, stringBuilder2.toString());
                    if (this.mVideoReadyFlag && this.mVideoShutterButton != null) {
                        this.mVideoShutterButton.performClick();
                        this.mVideoReadyFlag = false;
                        return;
                    }
                    return;
                default:
                    return;
            }
        } else if (this.mShutterButton != null) {
            this.mShutterButton.performClick();
        }
    }

    public void longClickAnimFucus() {
        if (this.mShutterButton != null) {
            this.mShutterButton.performLongClick();
            this.mLongClickAnimFucus = true;
        }
    }

    /* Access modifiers changed, original: protected */
    public void dismissHelpTip() {
        goToNextTip(true);
    }
}
