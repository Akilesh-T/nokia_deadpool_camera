package com.hmdglobal.app.camera;

import android.view.ViewGroup;
import com.hmdglobal.app.camera.ShutterButton.OnShutterButtonListener;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.ui.TouchCoordinate;
import com.hmdglobal.app.camera.util.ExternalExifInterface;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class HelpTipsManager implements HelpTipController, OnShutterButtonListener {
    public static final int CAMERAKEY_TIP = 2;
    private static final int DAY = 86400000;
    private static final int FIRST_DAY = 1;
    private static final int FIRST_START_RECORD = 1;
    private static final int FOURTH_DAY = 4;
    private static final int FRONT_CAMERA_OPEND_TIMES = 2;
    public static final int GESTURE_CONFIRM_GROUP = 4;
    private static final int INIT_DAY = 0;
    public static final int MANUAL_GROUP = 2;
    public static final int MODE_GROUP = 8;
    public static final int PANORAMA_GROUP = 1;
    public static final int PINCH_ZOOM_GROUP = 6;
    public static final int QUICK_SETTINGS_GROUP = 5;
    private static final int READY = 1;
    public static final int RECENT_GROUP = 11;
    public static final int RENCET_TIP = 3;
    private static final int SECOND_DAY = 2;
    private static final int SECOND_START_RECORD = 2;
    private static final int SECOND_TIME_USE_CAMERA = 2;
    public static final int SETTINGS_GROUP = 7;
    private static final int SINGLE_MEMBER_TIP_ID = 0;
    public static final int SNAP_TIP = 1;
    public static final int SWITCH_FRONT_CAMERA_GROUP = 3;
    private static final Tag TAG = new Tag("HelpTipsManager");
    private static final int THIRD_DAY = 3;
    public static final int UNKOWN_GROUP_ID = -1;
    private static final long UNKOWN_TIME = -1;
    public static final int UNKOWN_TIP_ID = -1;
    public static final int VIDEO_SNAP_GROUP = 10;
    public static final int VIDEO_STOP_GROUP = 9;
    public static final int VIDEO_TIP = 4;
    public static final int WELCOME_GROUP = 0;
    public static final int WELCOME_TIP = 0;
    private int SECOND_TIME_USE_GESTURE = 2;
    private Map<Integer, Integer> alarmTasksMap = new TreeMap();
    private final CameraActivity mActivity;
    private int mCurTipGroupId = -1;
    private int mCurTipMemberId = -1;
    private HelpTip mHelpTip;
    private ManualUI mManualUpdateUIListener;
    private final ViewGroup mRootView;
    private SettingsManager mSettingsManager;
    private boolean mSwitchToPanoMode = false;
    private final WeakReference<CameraActivity> mWrActivity;

    public interface NextButtonListener {
        void onUpdateUIChangedFromTutorial();
    }

    public HelpTipsManager(CameraActivity cameraActivity) {
        this.mWrActivity = new WeakReference(cameraActivity);
        this.mActivity = (CameraActivity) this.mWrActivity.get();
        this.mSettingsManager = this.mActivity.getSettingsManager();
        this.mRootView = (ViewGroup) this.mActivity.findViewById(R.id.helptips_placeholder_wrapper);
    }

    public void startAlarmTask() {
        this.alarmTasksMap.clear();
        long systemTime = this.mSettingsManager.getLong(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_SYSTEM_TIME, -1);
        int days = 0;
        if (systemTime == -1) {
            this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_SYSTEM_TIME, System.currentTimeMillis());
        } else {
            days = ((int) (new Date(System.currentTimeMillis()).getTime() - new Date(systemTime).getTime())) / DAY;
        }
        while (days > 0) {
            if (days == 1) {
                if (!checkHelpTipOverByGroupId(5)) {
                    addAlarmTask(5);
                }
            } else if (days == 2) {
                if (!checkHelpTipOverByGroupId(6)) {
                    addAlarmTask(6);
                }
            } else if (days == 3) {
                if (!checkHelpTipOverByGroupId(7)) {
                    addAlarmTask(7);
                }
            } else if (days == 4 && !checkHelpTipOverByGroupId(8)) {
                addAlarmTask(8);
            }
            days--;
        }
    }

    public void calcCameraUseTimes() {
        this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_USER_APP_TIMES, this.mSettingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_USER_APP_TIMES, Integer.valueOf(0)).intValue() + 1);
    }

    public void gestureShotResponse() {
        if (isHelpTipShowExist() && this.mHelpTip.getCurTipGroupId() == 4) {
            this.mHelpTip.goToNextTip(false);
        }
    }

    public boolean isNeedBoomKeyResponse() {
        if (this.mHelpTip != null && this.mHelpTip.getCurTipGroupId() == 0 && this.mHelpTip.getCurTipId() == 2) {
            return true;
        }
        return false;
    }

    private boolean isBackCameraFacing() {
        if (this.mSettingsManager != null) {
            return Keys.isCameraBackFacing(this.mSettingsManager, SettingsManager.SCOPE_GLOBAL);
        }
        return true;
    }

    public boolean isAutoMode() {
        return this.mActivity.getCurrentModuleIndex() == this.mActivity.getResources().getInteger(R.integer.camera_mode_photo);
    }

    public boolean isVideoMode() {
        return this.mActivity.getCurrentModuleIndex() == this.mActivity.getResources().getInteger(R.integer.camera_mode_video);
    }

    public boolean isPanoMode() {
        return this.mActivity.getCurrentModuleIndex() == this.mActivity.getResources().getInteger(R.integer.camera_mode_pano);
    }

    public boolean isManualMode() {
        return this.mActivity.getCurrentModuleIndex() == this.mActivity.getResources().getInteger(R.integer.camera_mode_manual);
    }

    public boolean scheduleTaskHelpTip(int groudId) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("scheduleTaskHelpTip groudId = ");
        stringBuilder.append(printGroupName(groudId));
        Log.i(tag, stringBuilder.toString());
        if (!checkConditions(groudId)) {
            return false;
        }
        createAndShowHelpTip(groudId, false);
        return true;
    }

    public void removeAlarmTask(int groudid) {
        if (this.alarmTasksMap != null) {
            this.alarmTasksMap.remove(Integer.valueOf(groudid));
        }
    }

    public void addAlarmTask(int groudid) {
        if (this.alarmTasksMap != null) {
            this.alarmTasksMap.put(Integer.valueOf(groudid), Integer.valueOf(1));
        }
    }

    public void checkAlarmTaskHelpTip() {
        Log.i(TAG, "checkAlarmTaskHelpTip E");
        boolean bShowSwitchFront = this.mSettingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_USER_APP_TIMES, Integer.valueOf(0)).intValue() >= 2 && !this.mSettingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_FRONT_CAMERA_FINISHED, false);
        if (bShowSwitchFront) {
            addAlarmTask(3);
        }
        boolean bShowGesture = !this.mSettingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_GESTURE_FINISHED, false) && this.mSettingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_FRONT_CAMERA_OPENED_TIMES, Integer.valueOf(0)).intValue() >= this.SECOND_TIME_USE_GESTURE;
        if (bShowGesture) {
            addAlarmTask(4);
        }
        if (!isHelpTipShowExist()) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("checkAlarmTaskHelpTip alarmTasksMap.size = ");
            stringBuilder.append(this.alarmTasksMap.size());
            Log.i(tag, stringBuilder.toString());
            for (Integer key : this.alarmTasksMap.keySet()) {
                int key2 = key.intValue();
                int value = ((Integer) this.alarmTasksMap.get(Integer.valueOf(key2))).intValue();
                Tag tag2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("checkAlarmTaskHelpTip begin = ");
                stringBuilder2.append(printGroupName(key2));
                stringBuilder2.append(",value = ");
                stringBuilder2.append(value);
                Log.i(tag2, stringBuilder2.toString());
                if (value == 1) {
                    boolean executeTaskFlags = true;
                    if (this.mSwitchToPanoMode) {
                        this.mSwitchToPanoMode = false;
                        executeTaskFlags = false;
                    }
                    if (executeTaskFlags && scheduleTaskHelpTip(key2)) {
                        return;
                    }
                }
            }
        }
    }

    public void openGestureHelpTip(int cameraId) {
        int frontCameraId = Integer.valueOf(this.mActivity.getAndroidContext().getString(R.string.pref_camera_id_index_front)).intValue();
        boolean bGesturePrompted = this.mActivity.getSettingsManager().getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_GESTURE_FINISHED, false);
        boolean isGestureDetectionOn = Keys.isGestureDetectionOn(this.mSettingsManager);
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isEnableGestureRecognization");
        stringBuilder.append(isGestureDetectionOn);
        Log.d(tag, stringBuilder.toString());
        if (isGestureDetectionOn && !bGesturePrompted && cameraId == frontCameraId) {
            int frontCameraOpenedTimes = this.mSettingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_FRONT_CAMERA_OPENED_TIMES, Integer.valueOf(0)).intValue() + 1;
            this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_FRONT_CAMERA_OPENED_TIMES, frontCameraOpenedTimes);
            if (isHelpTipShowExist()) {
                goNextHelpTipStage();
            } else if (frontCameraOpenedTimes == 2 && !scheduleTaskHelpTip(4)) {
                checkAlarmTaskHelpTip();
            }
        } else if (isHelpTipShowExist()) {
            notifyEventFinshed();
        } else {
            checkAlarmTaskHelpTip();
        }
    }

    private boolean checkConditions(int groupId) {
        boolean z = true;
        boolean canShow = (!checkHelpTipOverByGroupId(0) || isHelpTipShowExist() || checkHelpTipOverByGroupId(groupId)) ? false : true;
        switch (groupId) {
            case 0:
            case 5:
                if (!(canShow && isAutoMode() && isBackCameraFacing())) {
                    z = false;
                }
                canShow = z;
                break;
            case 1:
                if (!(canShow && isPanoMode())) {
                    z = false;
                }
                canShow = z;
                break;
            case 2:
                if (!(canShow && isManualMode())) {
                    z = false;
                }
                canShow = z;
                break;
            case 4:
                if (!canShow || isBackCameraFacing()) {
                    z = false;
                }
                canShow = z;
                break;
            case 6:
                if (!canShow || isPanoMode()) {
                    z = false;
                }
                canShow = z;
                break;
            case 7:
                if (!(canShow && isAutoMode())) {
                    z = false;
                }
                canShow = z;
                break;
            case 9:
            case 10:
                if (!(canShow && isVideoMode())) {
                    z = false;
                }
                canShow = z;
                break;
            case 11:
                boolean bWelComePrompted = this.mSettingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_WELCOME_FINISHED, false);
                boolean bRecentPrompted = this.mSettingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_RECENT_FINISHED, false);
                if (!(canShow && bWelComePrompted && !bRecentPrompted)) {
                    z = false;
                }
                canShow = z;
                break;
        }
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("checkConditions groupId = ");
        stringBuilder.append(printGroupName(groupId));
        stringBuilder.append(",isAutoMode() = ");
        stringBuilder.append(isAutoMode());
        stringBuilder.append(",WELCOME_GROUP is over = ");
        stringBuilder.append(checkHelpTipOverByGroupId(0));
        stringBuilder.append(",HelpTip is ShowExist() = ");
        stringBuilder.append(isHelpTipShowExist());
        stringBuilder.append(",special groupId is over = ");
        stringBuilder.append(checkHelpTipOverByGroupId(groupId));
        stringBuilder.append(",isBackCameraFacing =");
        stringBuilder.append(isBackCameraFacing());
        stringBuilder.append(",canShow = ");
        stringBuilder.append(canShow);
        Log.i(tag, stringBuilder.toString());
        return canShow;
    }

    public boolean createHelpTip(int groupid, boolean outCall) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("createHelpTip groupid = ");
        stringBuilder.append(printGroupName(groupid));
        Log.e(tag, stringBuilder.toString());
        StringBuilder stringBuilder2;
        if (checkHelpTipOverByGroupId(groupid)) {
            tag = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("createHelpTip is over");
            stringBuilder2.append(printGroupName(groupid));
            Log.e(tag, stringBuilder2.toString());
            return false;
        }
        this.mCurTipGroupId = groupid;
        switch (this.mCurTipGroupId) {
            case 0:
                if (outCall) {
                    this.mCurTipMemberId = Integer.valueOf(this.mSettingsManager.getString(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_WELCOME_STEP, "0")).intValue();
                }
                if (this.mCurTipMemberId <= 4) {
                    this.mHelpTip = new MultiHelpTip(0, this.mCurTipMemberId, this, this.mActivity);
                    break;
                }
                tag = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("member tip index is invalid :groupid = ");
                stringBuilder2.append(printGroupName(groupid));
                stringBuilder2.append(",mCurTipMemberId = ");
                stringBuilder2.append(this.mCurTipMemberId);
                Log.e(tag, stringBuilder2.toString());
                return false;
            case 1:
            case 2:
            case 4:
            case 6:
            case 8:
            case 9:
            case 10:
            case 11:
                break;
            case 3:
            case 5:
            case 7:
                this.mActivity.getButtonManager().setHelpTipListener(this);
                break;
        }
        this.mHelpTip = new SingleHelpTip(groupid, 0, this, this.mActivity);
        return true;
    }

    public boolean checkHelpTipOverByGroupId(int specialGroupId) {
        switch (specialGroupId) {
            case 0:
                return this.mSettingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_WELCOME_FINISHED, false);
            case 1:
                return this.mSettingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_PANO_FINISHED, false);
            case 2:
                return this.mSettingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_MANUAL_FINISHED, false);
            case 3:
                return this.mSettingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_FRONT_CAMERA_FINISHED, false);
            case 4:
                return this.mSettingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_GESTURE_FINISHED, false);
            case 5:
                return this.mSettingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_QUICK_SETTINGS_FINISHED, false);
            case 6:
                return this.mSettingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_PINCH_ZOOM_FINISHED, false);
            case 7:
                return this.mSettingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_SETTINGS_FINISHED, false);
            case 8:
                return this.mSettingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_MODE_FINISHED, false);
            case 9:
                return this.mSettingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_STOP_VIDEO_FINISHED, false);
            case 10:
                return this.mSettingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_VIDEO_SNAP_FINISHED, false);
            case 11:
                return this.mSettingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_RECENT_FINISHED, false);
            default:
                return false;
        }
    }

    public void showHelpTipTutorial() {
        if (this.mHelpTip != null) {
            this.mHelpTip.showHelpTipCling();
        }
    }

    public void goNextHelpTipStage() {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("goNextHelpTipStage mCurTipGroupId = ");
        stringBuilder.append(this.mCurTipGroupId);
        stringBuilder.append(",mCurTipMemberId = ");
        stringBuilder.append(this.mCurTipMemberId);
        Log.i(tag, stringBuilder.toString());
        if (isHelpTipShowExist()) {
            this.mHelpTip.goToNextTip(false);
        }
    }

    public void notifyFinishHelpTip() {
        this.mActivity.getButtonManager().setHelpTipListener(null);
        if (this.mHelpTip != null) {
            this.mHelpTip = null;
        }
    }

    public void onUpdateUIChangedFromTutorial() {
        if (this.mManualUpdateUIListener != null) {
            this.mManualUpdateUIListener.onUpdateUIChangedFromTutorial();
        }
    }

    public void onBurstShotResponse() {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onBurstShotResponse E isHelpTipShowExist = ");
        stringBuilder.append(isHelpTipShowExist());
        Log.i(tag, stringBuilder.toString());
        if (isHelpTipShowExist()) {
            tag = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("mHelpTip.getCurTipGroupId() = ");
            stringBuilder.append(this.mHelpTip.getCurTipGroupId());
            stringBuilder.append(",mHelpTip.getCurTipId() = ");
            stringBuilder.append(this.mHelpTip.getCurTipId());
            Log.i(tag, stringBuilder.toString());
            if (this.mHelpTip.getCurTipGroupId() != 0) {
                return;
            }
            if (this.mHelpTip.getCurTipId() == 1 || this.mHelpTip.getCurTipId() == 2) {
                this.mHelpTip.goToNextTip(false);
            }
        } else if (!scheduleTaskHelpTip(11)) {
            checkAlarmTaskHelpTip();
        }
    }

    public void onBoomKeySingleShotResponse() {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onBoomKeySingleShotResponse isHelpTipShowExist =");
        stringBuilder.append(isHelpTipShowExist());
        stringBuilder.append("mHelpTip.getCurTipGroupId() = ");
        stringBuilder.append(this.mHelpTip.getCurTipGroupId());
        stringBuilder.append(",mHelpTip.getCurTipId() = ");
        stringBuilder.append(this.mHelpTip.getCurTipId());
        Log.i(tag, stringBuilder.toString());
        if (isHelpTipShowExist() && this.mHelpTip.getCurTipGroupId() == 0 && this.mHelpTip.getCurTipId() == 2) {
            this.mHelpTip.goToNextTip(false);
        }
    }

    public void onRecentTipResponse() {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onRecentTipResponse E isHelpTipShowExist = ");
        stringBuilder.append(isHelpTipShowExist());
        Log.e(tag, stringBuilder.toString());
        if (!isHelpTipShowExist() && !scheduleTaskHelpTip(11)) {
            checkAlarmTaskHelpTip();
        }
    }

    public boolean isHelpTipShowExist() {
        return this.mHelpTip != null ? this.mHelpTip.IsShowExist() : false;
    }

    public void notifyEventFinshed() {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyEventFinshed mCurTipGroupId = ");
        stringBuilder.append(this.mCurTipGroupId);
        stringBuilder.append(",mCurTipMemberId = ");
        stringBuilder.append(this.mCurTipMemberId);
        stringBuilder.append(",isShowExist =");
        stringBuilder.append(isHelpTipShowExist());
        Log.i(tag, stringBuilder.toString());
        if (isHelpTipShowExist()) {
            goNextHelpTipStage();
        }
    }

    public void notifyModeChanged(int modeIndex, Runnable modeChangeRunnable) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyModeChanged mCurTipGroupId = ");
        stringBuilder.append(this.mCurTipGroupId);
        stringBuilder.append(",mCurTipMemberId = ");
        stringBuilder.append(this.mCurTipMemberId);
        stringBuilder.append(",modeIndex = ");
        stringBuilder.append(modeIndex);
        stringBuilder.append(" ,isShowExist = ");
        stringBuilder.append(isHelpTipShowExist());
        Log.i(tag, stringBuilder.toString());
        if (modeIndex == this.mActivity.getResources().getInteger(R.integer.camera_mode_pano)) {
            this.mSwitchToPanoMode = true;
        }
        if (modeChangeRunnable != null) {
            modeChangeRunnable.run();
        }
        if (isHelpTipShowExist()) {
            if (this.mCurTipGroupId == 8) {
                this.mHelpTip.notifyModeChanged();
            }
        } else if (modeIndex != this.mActivity.getResources().getInteger(R.integer.camera_mode_video)) {
            checkAlarmTaskHelpTip();
        }
    }

    public void setManualUpdateUIListener(ManualUI updateUIListener) {
        this.mManualUpdateUIListener = updateUIListener;
    }

    public void pause() {
        if (this.mHelpTip != null) {
            this.mHelpTip.doPause();
            this.mHelpTip = null;
        }
        this.mCurTipGroupId = -1;
        this.mCurTipMemberId = -1;
    }

    public void onShutterButtonFocus(boolean pressed) {
    }

    public void onShutterCoordinate(TouchCoordinate coord) {
    }

    public void onShutterButtonClick() {
        if (isHelpTipShowExist() && this.mHelpTip != null && this.mHelpTip.getCurTipGroupId() == 0 && this.mHelpTip.getCurTipId() == 1) {
            this.mHelpTip.goToNextTip(false);
        }
    }

    public void onShutterButtonLongClick() {
    }

    public ViewGroup getHelpTipView() {
        return this.mRootView;
    }

    public boolean helpTipCheckToIntercept() {
        return this.mHelpTip != null ? this.mHelpTip.checkToIntercept() : false;
    }

    public void destroy() {
        if (this.mHelpTip != null) {
            this.mHelpTip.cleanUpHelpTip();
            this.mHelpTip = null;
        }
        this.alarmTasksMap.clear();
        this.alarmTasksMap = null;
    }

    public void setVideoReadlyFlags() {
        if (isHelpTipShowExist() && Integer.valueOf(this.mActivity.getSettingsManager().getString(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_WELCOME_STEP, "0")).intValue() == 4) {
            this.mHelpTip.setVideoReadyFlag(true);
        }
    }

    public String printGroupName(int groudId) {
        switch (groudId) {
            case 0:
                return "welcome";
            case 1:
                return ExternalExifInterface.PANORAMA_TAG;
            case 2:
                return "manual";
            case 3:
                return "front camera";
            case 4:
                return "gesture confirm";
            case 5:
                return "quick settings";
            case 6:
                return "pinch zoom";
            case 7:
                return "SETTINGS_GROUP";
            case 8:
                return "mode";
            case 9:
                return "stop video";
            case 10:
                return "video snap";
            case 11:
                return "recent";
            default:
                return "unknow";
        }
    }

    public void clickSettingResponse() {
        if (isHelpTipShowExist() && this.mHelpTip.getCurTipGroupId() == 7) {
            this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_SETTINGS_FINISHED, true);
            removeAlarmTask(7);
        }
    }

    public void createAndShowHelpTip(int groupId, boolean outCall) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("createAndShowHelpTip groupId = ");
        stringBuilder.append(printGroupName(groupId));
        stringBuilder.append(",outCall = ");
        stringBuilder.append(outCall);
        Log.i(tag, stringBuilder.toString());
        if (!isHelpTipShowExist() && !checkHelpTipOverByGroupId(groupId)) {
            if (createHelpTip(groupId, outCall)) {
                showHelpTipTutorial();
            } else {
                Log.e(TAG, "createAndShowHelpTip failed");
            }
        }
    }

    public void startRecordVideoResponse() {
        int videoTimes = this.mSettingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_VIDEO_TIMES, Integer.valueOf(0)).intValue() + 1;
        this.mSettingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_VIDEO_TIMES, videoTimes);
        int groupID = -1;
        if (videoTimes == 1) {
            groupID = 9;
        }
        if (videoTimes == 2) {
            groupID = 10;
        }
        if (groupID != -1 && !scheduleTaskHelpTip(groupID)) {
            checkAlarmTaskHelpTip();
        }
    }
}
