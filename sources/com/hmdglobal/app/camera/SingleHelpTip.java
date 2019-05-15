package com.hmdglobal.app.camera;

import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.ui.ManualItem;
import com.hmdglobal.app.camera.ui.PeekImageView;
import com.hmdglobal.app.camera.ui.RotatableButton;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.hmdglobal.app.camera.widget.FloatingActionsMenu;
import java.util.ArrayList;
import java.util.List;

public class SingleHelpTip extends HelpTip {
    private static final int COUNT_DOWN_TOGGLE = 2;
    private static final int FLASH_TOGGLE = 0;
    private static final int F_ITEM = 3;
    private static final int HDR_TOGGLE = 1;
    private static final int ISO_ITEM = 0;
    private static final int LOW_LIGHT_TOGGLE = 3;
    private static final int MANUAL_TIP_DELAY = 1500;
    private static final int[] ModeFrameList = new int[]{R.drawable.mode_animation1, R.drawable.mode_animation2, R.drawable.mode_animation3, R.drawable.mode_animation4, R.drawable.mode_animation5, R.drawable.mode_animation6, R.drawable.mode_animation7, R.drawable.mode_animation8, R.drawable.mode_animation9, R.drawable.mode_animation10, R.drawable.mode_animation11, R.drawable.mode_animation12, R.drawable.mode_animation13, R.drawable.mode_animation14, R.drawable.mode_animation15, R.drawable.mode_animation16, R.drawable.mode_animation17, R.drawable.mode_animation18, R.drawable.mode_animation19, R.drawable.mode_animation20, R.drawable.mode_animation21, R.drawable.mode_animation22, R.drawable.mode_animation23, R.drawable.mode_animation24, R.drawable.mode_animation25, R.drawable.mode_animation26, R.drawable.mode_animation27, R.drawable.mode_animation28, R.drawable.mode_animation29, R.drawable.mode_animation30};
    private static final int PANORAMA_TIP_DELAY = 1500;
    private static final int[] PinchZoomFrameList = new int[]{R.drawable.pinch_zoom_animation1, R.drawable.pinch_zoom_animation2, R.drawable.pinch_zoom_animation3, R.drawable.pinch_zoom_animation4, R.drawable.pinch_zoom_animation5, R.drawable.pinch_zoom_animation6, R.drawable.pinch_zoom_animation7, R.drawable.pinch_zoom_animation8, R.drawable.pinch_zoom_animation9, R.drawable.pinch_zoom_animation10, R.drawable.pinch_zoom_animation11, R.drawable.pinch_zoom_animation12, R.drawable.pinch_zoom_animation13, R.drawable.pinch_zoom_animation14, R.drawable.pinch_zoom_animation15, R.drawable.pinch_zoom_animation16, R.drawable.pinch_zoom_animation17, R.drawable.pinch_zoom_animation18, R.drawable.pinch_zoom_animation19, R.drawable.pinch_zoom_animation20, R.drawable.pinch_zoom_animation21, R.drawable.pinch_zoom_animation22, R.drawable.pinch_zoom_animation23, R.drawable.pinch_zoom_animation24, R.drawable.pinch_zoom_animation25, R.drawable.pinch_zoom_animation26, R.drawable.pinch_zoom_animation27};
    private static final int QUICK_SWITCH_TIP_DELAY = 500;
    private static final int SCENE_ANIM_FIRST_FRAME = 1;
    private static final int S_ITEM = 1;
    private static final int VIDEO_SNAP_TIP_DELAY = 1500;
    private static final int WB_ITEM = 2;
    private MultiToggleImageButton mCameraCountDownToggle;
    private MultiToggleImageButton mCameraFlashToggle;
    private MultiToggleImageButton mCameraHDRToggle;
    private MultiToggleImageButton mCameraLowLightToggle;
    private RotatableButton mCameraSettingsToggle;
    private MultiToggleImageButton mCameraToggleBtn;
    private RotatableButton mCapturebButton;
    private LinearLayout mItemF;
    private LinearLayout mItemISO;
    private LinearLayout mItemS;
    private LinearLayout mItemWb;
    private FloatingActionsMenu mManualMenu;
    private final int[] mModeDurations = new int[]{360, 120, 120, 120, 120, 120, 120, 120, 120, 120, 120, 120, 120, 360, 40, 40, 40, 40, 40, 40, 80, 80, 320, 120, 120, 120, 120, 120, 120, 10};
    private FrameLayout mPanoramaArrowFrameLayout;
    private PeekImageView mPeekThumb;
    private final int[] mPinchZoomDurations = new int[]{360, 120, 120, 120, 120, 120, 120, 120, 120, 120, 120, 120, 120, 360, 40, 40, 40, 40, 40, 40, 80, 80, 600, 440, 120, 120, 10};
    private PopupWindow mPopupWindow;
    private View mPreViewOverLay;
    private SceneAnimation mSceneAnimation;
    private ShutterButton mShutterButton;

    private class SceneAnimation {
        final Runnable SceneRunnable = new Runnable() {
            public void run() {
                try {
                    if (SceneAnimation.this.imageView != null) {
                        SceneAnimation.this.imageView.setBackgroundResource(SceneAnimation.this.mFrameRess[SceneAnimation.this.mFrameNumber]);
                    }
                    if (SceneAnimation.this.mFrameNumber == SceneAnimation.this.mLastFrameNo) {
                        SceneAnimation.this.playConstant(0);
                    } else {
                        SceneAnimation.this.playConstant(SceneAnimation.this.mFrameNumber + 1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Tag tag = HelpTip.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("SceneRunnable OOM out of memory mCurTipId = ");
                    stringBuilder.append(SingleHelpTip.this.mCurTipId);
                    Log.e(tag, stringBuilder.toString());
                    if (SingleHelpTip.this.mHelpTipCling != null) {
                        SingleHelpTip.this.cleanUpHelpTip();
                    }
                }
            }
        };
        private boolean bPlayEnd = false;
        private ImageView imageView;
        private final int[] mDurationList;
        private int mFrameNumber = 0;
        private final int[] mFrameRess;
        private final int mLastFrameNo;

        public SceneAnimation(ImageView pImageView, int[] pFrameRess, int[] durations) {
            this.imageView = pImageView;
            this.mFrameRess = pFrameRess;
            this.mLastFrameNo = pFrameRess.length - 1;
            this.imageView.setBackgroundResource(this.mFrameRess[0]);
            this.mDurationList = durations;
            this.bPlayEnd = false;
        }

        public void playConstant(int pFrameNo) {
            this.mFrameNumber = pFrameNo;
            if (!this.bPlayEnd && this.imageView != null) {
                this.imageView.postDelayed(this.SceneRunnable, (long) this.mDurationList[this.mFrameNumber]);
            }
        }

        public void stopAnimation() {
            Tag tag = HelpTip.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("stopAnimation E mCurTipGroupId = ");
            stringBuilder.append(SingleHelpTip.this.mCurTipGroupId);
            stringBuilder.append("mCurTipId = ");
            stringBuilder.append(SingleHelpTip.this.mCurTipId);
            Log.i(tag, stringBuilder.toString());
            this.bPlayEnd = true;
            this.imageView.setBackground(null);
            this.imageView.clearAnimation();
            this.imageView.removeCallbacks(this.SceneRunnable);
        }
    }

    public SingleHelpTip(int groupId, int tipId, HelpTipController controller, CameraActivity activity) {
        super(tipId, controller, activity);
        this.mCurTipGroupId = groupId;
        adapterLayoutById(groupId);
        if (this.mSceneAnimation != null) {
            this.mSceneAnimation.stopAnimation();
            this.mSceneAnimation = null;
        }
    }

    private void adapterLayoutById(int groupId) {
        switch (groupId) {
            case 1:
                this.mLayoutResId = R.layout.panorama_tip;
                return;
            case 2:
                this.mLayoutResId = R.layout.manual_tip;
                return;
            case 3:
                this.mLayoutResId = R.layout.switch_front_tip;
                return;
            case 4:
                this.mLayoutResId = R.layout.front_gesture_tip;
                return;
            case 5:
                this.mLayoutResId = R.layout.quick_menu_tip;
                return;
            case 6:
                this.mLayoutResId = R.layout.pinch_zoom_tip;
                return;
            case 7:
                this.mLayoutResId = R.layout.settings_menu_tip;
                return;
            case 8:
                this.mLayoutResId = R.layout.mode_tip;
                return;
            case 9:
                this.mLayoutResId = R.layout.video_stop_tip;
                return;
            case 10:
                this.mLayoutResId = R.layout.video_snap_tip;
                return;
            case 11:
                this.mLayoutResId = R.layout.recent_tip;
                return;
            default:
                return;
        }
    }

    /* Access modifiers changed, original: protected */
    public void initWidgets() {
        boolean bNeedinitCommom = true;
        if (this.mCurTipGroupId == 6) {
            bNeedinitCommom = false;
        }
        if (bNeedinitCommom) {
            initCommomWidget();
        }
        switch (this.mCurTipGroupId) {
            case 1:
                this.mDrawType = 2;
                playAnimation();
                this.mHandler.postDelayed(new Runnable() {
                    public void run() {
                        SingleHelpTip.this.mPanoramaArrowFrameLayout = (FrameLayout) SingleHelpTip.this.mActivity.findViewById(R.id.PanoramaPreview_Frame);
                        if (SingleHelpTip.this.mPanoramaArrowFrameLayout != null) {
                            int[] location = new int[2];
                            SingleHelpTip.this.mPanoramaArrowFrameLayout.getLocationOnScreen(location);
                            List<Rect> panoRectList = new ArrayList();
                            panoRectList.add(new Rect(location[0], location[1], location[0] + SingleHelpTip.this.mPanoramaArrowFrameLayout.getWidth(), location[1] + SingleHelpTip.this.mPanoramaArrowFrameLayout.getHeight()));
                            if (SingleHelpTip.this.mHelpTipCling != null) {
                                SingleHelpTip.this.mHelpTipCling.setHitRect(panoRectList);
                            }
                            if (SingleHelpTip.this.mRootView != null) {
                                SingleHelpTip.this.mRootView.requestLayout();
                                SingleHelpTip.this.mRootView.invalidate();
                            }
                        }
                    }
                }, 1500);
                break;
            case 2:
                this.mDrawType = 1;
                this.mHandler.postDelayed(new Runnable() {
                    public void run() {
                        SingleHelpTip.this.mManualMenu = (FloatingActionsMenu) SingleHelpTip.this.mActivity.findViewById(R.id.multiple_actions);
                        SingleHelpTip.this.mItemISO = (LinearLayout) ((ManualItem) SingleHelpTip.this.mActivity.findViewById(R.id.item_iso)).findViewById(R.id.item_root);
                        SingleHelpTip.this.mItemS = (LinearLayout) ((ManualItem) SingleHelpTip.this.mActivity.findViewById(R.id.item_s)).findViewById(R.id.item_root);
                        SingleHelpTip.this.mItemWb = (LinearLayout) ((ManualItem) SingleHelpTip.this.mActivity.findViewById(R.id.item_wb)).findViewById(R.id.item_root);
                        SingleHelpTip.this.mItemF = (LinearLayout) ((ManualItem) SingleHelpTip.this.mActivity.findViewById(R.id.item_f)).findViewById(R.id.item_root);
                        int[] location = new int[2];
                        List<Rect> manualRect = new ArrayList();
                        SingleHelpTip.this.mItemISO.getLocationOnScreen(location);
                        manualRect.add(new Rect(location[0], location[1], location[0] + SingleHelpTip.this.mItemISO.getWidth(), location[1] + SingleHelpTip.this.mItemISO.getHeight()));
                        SingleHelpTip.this.mItemS.getLocationOnScreen(location);
                        manualRect.add(new Rect(location[0], location[1], location[0] + SingleHelpTip.this.mItemS.getWidth(), location[1] + SingleHelpTip.this.mItemS.getHeight()));
                        SingleHelpTip.this.mItemWb.getLocationOnScreen(location);
                        manualRect.add(new Rect(location[0], location[1], location[0] + SingleHelpTip.this.mItemWb.getWidth(), location[1] + SingleHelpTip.this.mItemWb.getHeight()));
                        SingleHelpTip.this.mItemF.getLocationOnScreen(location);
                        manualRect.add(new Rect(location[0], location[1], location[0] + SingleHelpTip.this.mItemF.getWidth(), location[1] + SingleHelpTip.this.mItemF.getHeight()));
                        if (SingleHelpTip.this.mHelpTipCling != null) {
                            SingleHelpTip.this.mHelpTipCling.setHitRect(manualRect);
                        }
                        if (SingleHelpTip.this.mRootView != null) {
                            SingleHelpTip.this.mRootView.requestLayout();
                            SingleHelpTip.this.mRootView.invalidate();
                            LinearLayout manualMenu = (LinearLayout) SingleHelpTip.this.mRootView.findViewById(R.id.help_tip_manual_menu);
                            if (manualMenu != null) {
                                manualMenu.setOnClickListener(new OnClickListener() {
                                    public void onClick(View v) {
                                        if (SingleHelpTip.this.mManualMenu != null) {
                                            SingleHelpTip.this.mManualMenu.manualAddButtonClick();
                                        }
                                    }
                                });
                            }
                        }
                    }
                }, 1500);
                break;
            case 3:
                this.mCameraToggleBtn = (MultiToggleImageButton) this.mActivity.findViewById(R.id.camera_toggle_button);
                this.mDrawType = 0;
                playAnimation();
                break;
            case 4:
                this.mDrawType = -1;
                break;
            case 5:
                this.mDrawType = 1;
                this.mHandler.postDelayed(new Runnable() {
                    public void run() {
                        SingleHelpTip.this.mCameraFlashToggle = (MultiToggleImageButton) SingleHelpTip.this.mActivity.findViewById(R.id.flash_toggle_button);
                        SingleHelpTip.this.mCameraHDRToggle = (MultiToggleImageButton) SingleHelpTip.this.mActivity.findViewById(R.id.hdr_plus_toggle_button);
                        SingleHelpTip.this.mCameraCountDownToggle = (MultiToggleImageButton) SingleHelpTip.this.mActivity.findViewById(R.id.countdown_toggle_button);
                        SingleHelpTip.this.mCameraLowLightToggle = (MultiToggleImageButton) SingleHelpTip.this.mActivity.findViewById(R.id.lowlight_toggle_button);
                        int[] location = new int[2];
                        List<Rect> quickSettingsRectF = new ArrayList();
                        SingleHelpTip.this.mCameraFlashToggle.getLocationOnScreen(location);
                        quickSettingsRectF.add(new Rect(location[0], location[1], location[0] + SingleHelpTip.this.mCameraFlashToggle.getWidth(), location[1] + SingleHelpTip.this.mCameraFlashToggle.getHeight()));
                        SingleHelpTip.this.mCameraHDRToggle.getLocationOnScreen(location);
                        quickSettingsRectF.add(new Rect(location[0], location[1], location[0] + SingleHelpTip.this.mCameraHDRToggle.getWidth(), location[1] + SingleHelpTip.this.mCameraHDRToggle.getHeight()));
                        SingleHelpTip.this.mCameraCountDownToggle.getLocationOnScreen(location);
                        quickSettingsRectF.add(new Rect(location[0], location[1], location[0] + SingleHelpTip.this.mCameraCountDownToggle.getWidth(), location[1] + SingleHelpTip.this.mCameraCountDownToggle.getHeight()));
                        SingleHelpTip.this.mCameraLowLightToggle.getLocationOnScreen(location);
                        quickSettingsRectF.add(new Rect(location[0], location[1], location[0] + SingleHelpTip.this.mCameraLowLightToggle.getWidth(), location[1] + SingleHelpTip.this.mCameraLowLightToggle.getHeight()));
                        if (SingleHelpTip.this.mHelpTipCling != null) {
                            SingleHelpTip.this.mHelpTipCling.setHitRect(quickSettingsRectF);
                        }
                        if (SingleHelpTip.this.mRootView != null) {
                            SingleHelpTip.this.mRootView.requestLayout();
                            SingleHelpTip.this.mRootView.invalidate();
                        }
                    }
                }, 500);
                break;
            case 6:
                this.mDrawType = -1;
                this.mPreViewOverLay = this.mActivity.findViewById(R.id.preview_overlay);
                this.mHelpTipCling.setPreViewOverLay(this.mPreViewOverLay);
                startAnimation(PinchZoomFrameList, this.mPinchZoomDurations);
                popupTipDialog(this.mRingAnimationImageView);
                break;
            case 7:
                this.mCameraSettingsToggle = (RotatableButton) this.mActivity.findViewById(R.id.menu_setting_button);
                this.mDrawType = 0;
                playAnimation();
                break;
            case 8:
                this.mDrawType = 2;
                this.mRingAnimationImageView = (ImageView) this.mRootView.findViewById(R.id.anim_focus);
                startAnimation(ModeFrameList, this.mModeDurations);
                break;
            case 9:
                this.mShutterButton = (ShutterButton) this.mActivity.findViewById(R.id.shutter_button);
                this.mDrawType = 0;
                playAnimation();
                break;
            case 10:
                this.mDrawType = 0;
                playAnimation();
                this.mHandler.postDelayed(new Runnable() {
                    public void run() {
                        SingleHelpTip.this.mCapturebButton = (RotatableButton) SingleHelpTip.this.mActivity.findViewById(R.id.video_snap_button);
                        if (SingleHelpTip.this.mRootView != null) {
                            SingleHelpTip.this.mRootView.requestLayout();
                            SingleHelpTip.this.mRootView.invalidate();
                        }
                    }
                }, 1500);
                break;
            case 11:
                this.mPeekThumb = (PeekImageView) this.mActivity.findViewById(R.id.peek_thumb);
                this.mDrawType = 0;
                playAnimation();
                break;
        }
        this.mHelpTipCling.setListener(this, this.mDrawType);
    }

    private void popupTipDialog(View view) {
        int widght = this.mActivity.getResources().getInteger(R.integer.help_tip_pinch_zoom_dialog_width);
        int height = this.mActivity.getResources().getInteger(R.integer.help_tip_pinch_zoom_dialog_height);
        int offsetX = this.mActivity.getResources().getInteger(R.integer.help_tip_pinch_zoom_dialog_offset_x);
        int offsetY = this.mActivity.getResources().getInteger(R.integer.help_tip_pinch_zoom_dialog_offset_y);
        View tipView = this.mInflater.inflate(R.layout.help_tip_pinch_zoom_popwindow, null);
        this.mTipNextButton = (Button) tipView.findViewById(R.id.next);
        this.mTipNextButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (SingleHelpTip.this.mPopupWindow != null) {
                    SingleHelpTip.this.mPopupWindow.dismiss();
                    SingleHelpTip.this.mPopupWindow = null;
                }
                SingleHelpTip.this.goToNextTip(true);
            }
        });
        this.mPopupWindow = new PopupWindow(tipView, CameraUtil.dpToPixel(widght), -2);
        this.mPopupWindow.setContentView(tipView);
        this.mPopupWindow.setFocusable(false);
        this.mPopupWindow.setOutsideTouchable(false);
        this.mPopupWindow.setBackgroundDrawable(new BitmapDrawable());
        this.mPopupWindow.showAtLocation(view, 81, CameraUtil.dpToPixel(offsetX), CameraUtil.dpToPixel(offsetY));
    }

    public void clickHitRectResponse(int index) {
        if (this.mCurTipGroupId == 5) {
            switch (index) {
                case 0:
                    this.mCameraFlashToggle.performClick();
                    return;
                case 1:
                    this.mCameraHDRToggle.performClick();
                    return;
                case 2:
                    this.mCameraCountDownToggle.performClick();
                    return;
                case 3:
                    this.mCameraLowLightToggle.performClick();
                    return;
                default:
                    return;
            }
        } else if (this.mCurTipGroupId == 2) {
            switch (index) {
                case 0:
                    if (this.mItemISO != null) {
                        this.mItemISO.performClick();
                        return;
                    }
                    return;
                case 1:
                    if (this.mItemS != null) {
                        this.mItemS.performClick();
                        return;
                    }
                    return;
                case 2:
                    if (this.mItemWb != null) {
                        this.mItemWb.performClick();
                        return;
                    }
                    return;
                case 3:
                    if (this.mItemF != null) {
                        this.mItemF.performClick();
                        return;
                    }
                    return;
                default:
                    return;
            }
        } else if (this.mCurTipGroupId == 1 && this.mPanoramaArrowFrameLayout != null) {
            this.mPanoramaArrowFrameLayout.performClick();
        }
    }

    /* Access modifiers changed, original: protected */
    public void goToNextTip(boolean dismiss) {
        if (this.mPopupWindow != null) {
            this.mPopupWindow.dismiss();
            this.mPopupWindow = null;
        }
        if (this.mCurTipGroupId == 2 && this.mHelpTipController != null) {
            this.mHelpTipController.onUpdateUIChangedFromTutorial();
        }
        updateCurHelpTipStep(this.mCurTipGroupId, true);
        closeAndFinishHelptip();
        this.mHelpTipController.checkAlarmTaskHelpTip();
    }

    /* Access modifiers changed, original: protected */
    public void updateCurHelpTipStep(int tipId, boolean isOver) {
        boolean bNeedUpdateAlarmTask = false;
        switch (this.mCurTipGroupId) {
            case 1:
                this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_PANO_FINISHED, isOver);
                break;
            case 2:
                this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_MANUAL_FINISHED, isOver);
                break;
            case 3:
                this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_FRONT_CAMERA_FINISHED, isOver);
                bNeedUpdateAlarmTask = true;
                break;
            case 4:
                this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_GESTURE_FINISHED, isOver);
                bNeedUpdateAlarmTask = true;
                break;
            case 5:
                this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_QUICK_SETTINGS_FINISHED, isOver);
                if (isOver) {
                    bNeedUpdateAlarmTask = true;
                    break;
                }
                break;
            case 6:
                this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_PINCH_ZOOM_FINISHED, isOver);
                if (isOver) {
                    bNeedUpdateAlarmTask = true;
                    break;
                }
                break;
            case 7:
                this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_SETTINGS_FINISHED, isOver);
                if (isOver) {
                    bNeedUpdateAlarmTask = true;
                    break;
                }
                break;
            case 8:
                this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_MODE_FINISHED, isOver);
                if (isOver) {
                    bNeedUpdateAlarmTask = true;
                    break;
                }
                break;
            case 9:
                this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_STOP_VIDEO_FINISHED, true);
                break;
            case 10:
                this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_VIDEO_SNAP_FINISHED, true);
                break;
            case 11:
                this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_RECENT_FINISHED, true);
                break;
        }
        if (bNeedUpdateAlarmTask) {
            this.mHelpTipController.removeAlarmTask(this.mCurTipGroupId);
        }
    }

    /* Access modifiers changed, original: protected */
    public void clickAnimFucus() {
        int i = this.mCurTipGroupId;
        if (i != 3) {
            if (i != 7) {
                switch (i) {
                    case 9:
                        if (this.mShutterButton != null) {
                            this.mShutterButton.performClick();
                        }
                        updateCurHelpTipStep(this.mCurTipGroupId, true);
                        closeAndFinishHelptip();
                        return;
                    case 10:
                        if (this.mCapturebButton != null) {
                            this.mCapturebButton.performClick();
                            return;
                        }
                        return;
                    case 11:
                        if (this.mPeekThumb != null) {
                            this.mPeekThumb.performClick();
                            return;
                        }
                        return;
                    default:
                        return;
                }
            } else if (this.mCameraSettingsToggle != null) {
                this.mCameraSettingsToggle.performClick();
            }
        } else if (this.mCameraToggleBtn != null) {
            this.mCameraToggleBtn.performClick();
        }
    }

    /* Access modifiers changed, original: protected */
    public void notifyModeChanged() {
        goToNextTip(false);
    }

    /* Access modifiers changed, original: protected */
    public void dismissHelpTip() {
        if (this.mCurTipGroupId == 9) {
            updateCurHelpTipStep(this.mCurTipGroupId, true);
            closeAndFinishHelptip();
            return;
        }
        goToNextTip(true);
    }

    /* Access modifiers changed, original: protected */
    public void cleanUpHelpTip() {
        if (this.mSceneAnimation != null) {
            this.mSceneAnimation.stopAnimation();
            this.mSceneAnimation = null;
        }
        super.cleanUpHelpTip();
    }

    public void doPause() {
        if (this.mCurTipGroupId != 7) {
            updateCurHelpTipStep(this.mCurTipGroupId, false);
        }
        if (this.mPopupWindow != null) {
            this.mPopupWindow.dismiss();
            this.mPopupWindow = null;
        }
        super.doPause();
    }

    private void startAnimation(int[] modeFrameList, int[] durations) {
        this.mRingAnimationImageView = (ImageView) this.mRootView.findViewById(R.id.anim_focus);
        this.mSceneAnimation = new SceneAnimation(this.mRingAnimationImageView, modeFrameList, durations);
        this.mSceneAnimation.playConstant(1);
    }
}
