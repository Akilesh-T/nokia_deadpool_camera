package com.hmdglobal.app.camera.ui;

import android.content.res.TypedArray;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import com.android.ex.camera2.portability.CameraAgent.CameraProxy;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraCapabilities.Stringifier;
import com.hmdglobal.app.camera.CameraActivity;
import com.hmdglobal.app.camera.ManualUI;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.ui.ManualItem.ManualStateChangeListener;
import com.hmdglobal.app.camera.ui.Rotatable.RotateEntity;
import com.hmdglobal.app.camera.util.CameraUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProArcSeekBarUI implements ArcSeekBarEventListener {
    public static final int FRONT_CAMERA = 1;
    public static final int HIDE_ACTION_MENU = 1000;
    public static final int HIDE_COM_SEEK_BAR = 1002;
    private static final String PRO_ARC_SEEKBAR_ID = "ProArcSeekBarUI";
    public static final int REAR_CAMERA = 0;
    public static final int SHOW_ACTION_MENU = 1001;
    private static final Tag TAG = new Tag(PRO_ARC_SEEKBAR_ID);
    private int cameraId;
    private CameraActivity mActivity;
    private ArcSeekBar mArcSeekBarAWB;
    private ArcSeekBar mArcSeekBarCOM;
    private ArcSeekBar mArcSeekBarEC;
    private ArcSeekBar mArcSeekBarET;
    private ArcSeekBar mArcSeekBarFM;
    private ArcSeekBar mArcSeekBarIOS;
    private CameraProxy mCameraProxy;
    private String mCurExposureCompensationState;
    private String mCurExposureTimeState;
    private String mCurFocusState;
    private String mCurIsoState;
    private String mCurWBState;
    private ArrayList<String> mExposureCompensationTitles = new ArrayList();
    private ArrayList<String> mExposureCompensationValues = new ArrayList();
    private ArrayList<String> mExposureTimeDouble = new ArrayList();
    private ArrayList<String> mExposureTimeTitles = new ArrayList();
    private ArrayList<Integer> mISOValues = new ArrayList();
    private int mMaxFocusPos = 0;
    private int mMinFocusPos = 0;
    private MainActivityLayout mRootView;
    private List<ArcSeekBar> mSeekBarList = new ArrayList();
    private ArrayList<String> mWBTitles = new ArrayList();
    private ArrayList<String> mWBValues = new ArrayList();
    private ManualStateChangeListener manualStateChangeListener;
    private TypedArray srcId;
    private TypedArray srcIdPress;

    public ProArcSeekBarUI(AppController controller, MainActivityLayout appRootView) {
        this.mActivity = (CameraActivity) controller;
        this.mRootView = appRootView;
        this.srcId = this.mActivity.getResources().obtainTypedArray(R.array.awb_ball_src);
        this.srcIdPress = this.mActivity.getResources().obtainTypedArray(R.array.awb_ball_src_press);
        initView();
    }

    private void initView() {
        if (this.mRootView != null) {
            this.mSeekBarList.clear();
            this.mArcSeekBarAWB = (ArcSeekBar) this.mRootView.findViewById(R.id.arc_awb);
            this.mArcSeekBarAWB.setArcSeekBarEventListener(this);
            this.mArcSeekBarAWB.setArcMode(0);
            this.mArcSeekBarAWB.setIsDrag(true);
            this.mArcSeekBarAWB.setWabBallSrcIdArray(this.srcId);
            this.mArcSeekBarAWB.setWabBallSrcIdPressArray(this.srcIdPress);
            this.mSeekBarList.add(this.mArcSeekBarAWB);
            this.mArcSeekBarFM = (ArcSeekBar) this.mRootView.findViewById(R.id.arc_focus_mode);
            this.mArcSeekBarFM.setArcSeekBarEventListener(this);
            this.mArcSeekBarFM.setArcMode(1);
            this.mArcSeekBarFM.setIsDrag(true);
            this.mSeekBarList.add(this.mArcSeekBarFM);
            this.mArcSeekBarIOS = (ArcSeekBar) this.mRootView.findViewById(R.id.arc_iso);
            this.mArcSeekBarIOS.setArcSeekBarEventListener(this);
            this.mArcSeekBarIOS.setArcMode(2);
            this.mArcSeekBarIOS.setIsDrag(true);
            this.mSeekBarList.add(this.mArcSeekBarIOS);
            this.mArcSeekBarET = (ArcSeekBar) this.mRootView.findViewById(R.id.arc_exposure_time);
            this.mArcSeekBarET.setArcSeekBarEventListener(this);
            this.mArcSeekBarET.setArcMode(3);
            this.mArcSeekBarET.setIsDrag(true);
            this.mSeekBarList.add(this.mArcSeekBarET);
            this.mArcSeekBarEC = (ArcSeekBar) this.mRootView.findViewById(R.id.arc_exposure_compensation);
            this.mArcSeekBarEC.setArcSeekBarEventListener(this);
            this.mArcSeekBarEC.setArcMode(4);
            this.mArcSeekBarEC.setIsDrag(true);
            this.mSeekBarList.add(this.mArcSeekBarEC);
            this.mArcSeekBarCOM = (ArcSeekBar) this.mRootView.findViewById(R.id.arc_common);
            this.mArcSeekBarCOM.setArcSeekBarEventListener(this);
            this.mArcSeekBarCOM.setIsDrag(false);
            this.mActivity.addRotatableToListenerPool(new RotateEntity(this.mArcSeekBarAWB, true));
            this.mActivity.addRotatableToListenerPool(new RotateEntity(this.mArcSeekBarFM, true));
            this.mActivity.addRotatableToListenerPool(new RotateEntity(this.mArcSeekBarIOS, true));
            this.mActivity.addRotatableToListenerPool(new RotateEntity(this.mArcSeekBarET, true));
            this.mActivity.addRotatableToListenerPool(new RotateEntity(this.mArcSeekBarEC, true));
            this.mActivity.addRotatableToListenerPool(new RotateEntity(this.mArcSeekBarCOM, true));
        }
    }

    public void setCameraProxy(CameraProxy camera) {
        this.mCameraProxy = camera;
        this.cameraId = camera.getCameraId();
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Pro cameraId = ");
        stringBuilder.append(this.cameraId);
        Log.d(tag, stringBuilder.toString());
        this.mSeekBarList.clear();
        this.mSeekBarList.add(this.mArcSeekBarAWB);
        this.mSeekBarList.add(this.mArcSeekBarIOS);
        this.mSeekBarList.add(this.mArcSeekBarET);
        this.mSeekBarList.add(this.mArcSeekBarEC);
        if (this.cameraId == 0) {
            this.mArcSeekBarAWB.setLevel(5);
            this.mSeekBarList.add(this.mArcSeekBarFM);
        } else {
            this.mArcSeekBarAWB.setLevel(4);
            this.mArcSeekBarFM.setVisibility(8);
        }
        initProSettings(camera.getCapabilities());
        this.mActivity.getCameraAppUI().resetRings();
    }

    private void initProSettings(CameraCapabilities capabilities) {
        int i;
        int minIso;
        Stringifier stringifier = capabilities.getStringifier();
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        String[] wbValues = this.mActivity.getResources().getStringArray(R.array.white_balance_values);
        String[] wbStrings = this.mActivity.getResources().getStringArray(R.array.white_balance_strings);
        this.mWBValues = new ArrayList();
        this.mWBTitles = new ArrayList();
        for (i = 0; i < wbValues.length; i++) {
            if (capabilities.supports(stringifier.whiteBalanceFromString(wbValues[i]))) {
                this.mWBValues.add(wbValues[i]);
                this.mWBTitles.add(wbStrings[i]);
            }
        }
        CameraCapabilities cameraCapabilities = capabilities;
        this.mMinFocusPos = capabilities.getMinFocusScale();
        this.mMaxFocusPos = capabilities.getMaxFocusScale();
        i = capabilities.getMinISO();
        int maxIso = capabilities.getMaxIso();
        int[] isoValues = this.mActivity.getResources().getIntArray(R.array.camera_iso_values);
        this.mISOValues = new ArrayList();
        int i2 = 0;
        while (i2 < isoValues.length) {
            if (isoValues[i2] >= i && isoValues[i2] <= maxIso) {
                this.mISOValues.add(Integer.valueOf(isoValues[i2]));
            }
            i2++;
        }
        String minExposureTime = capabilities.getMinExposureTime();
        String maxExposureTime = capabilities.getMaxExposureTime();
        String[] exposureTimeDouble = this.mActivity.getResources().getStringArray(R.array.exposure_time_double);
        String[] exposureTimeString = this.mActivity.getResources().getStringArray(R.array.exposure_time_string);
        this.mExposureTimeDouble = new ArrayList();
        this.mExposureTimeTitles = new ArrayList();
        long maxT = Float.valueOf(maxExposureTime).longValue();
        long minT = Float.valueOf(minExposureTime).longValue();
        int i3 = 0;
        while (true) {
            Stringifier stringifier2 = stringifier;
            if (i3 >= exposureTimeDouble.length) {
                break;
            }
            SettingsManager settingsManager2;
            if (Float.valueOf(exposureTimeDouble[i3]).longValue() > maxT || Float.valueOf(exposureTimeDouble[i3]).longValue() < minT) {
                settingsManager2 = settingsManager;
            } else {
                settingsManager2 = settingsManager;
                this.mExposureTimeDouble.add(exposureTimeDouble[i3]);
                this.mExposureTimeTitles.add(exposureTimeString[i3]);
            }
            i3++;
            stringifier = stringifier2;
            settingsManager = settingsManager2;
        }
        float step = capabilities.getExposureCompensationStep();
        int max = capabilities.getMaxExposureCompensation();
        i3 = capabilities.getMinExposureCompensation();
        this.mExposureCompensationValues = new ArrayList();
        this.mExposureCompensationTitles = new ArrayList();
        max = (int) Math.floor((double) (((float) max) * step));
        int minValue = (int) Math.ceil((double) (((float) i3) * step));
        CharSequence[] entries = new CharSequence[((max - minValue) + 1)];
        int minValue2 = minValue;
        CharSequence[] entryValues = new CharSequence[((max - minValue) + 1)];
        int i4 = minValue2;
        while (true) {
            int min = i3;
            i3 = i4;
            if (i3 > max) {
                break;
            }
            float step2;
            minIso = i;
            entryValues[max + i3] = Integer.toString(Math.round(((float) i3) / step));
            StringBuilder builder = new StringBuilder();
            if (i3 > 0) {
                step2 = step;
                builder.append(6.0E-44f);
            } else {
                step2 = step;
            }
            int i5 = max + i3;
            builder.append(i3);
            entries[i5] = builder.toString();
            i4 = i3 + 1;
            i3 = min;
            i = minIso;
            step = step2;
        }
        minIso = i;
        int i6 = 0;
        while (true) {
            int i7 = i6;
            CharSequence[] entryValues2;
            CharSequence[] entries2;
            if (i7 <= 4) {
                i = i7 - 2;
                int maxValue = max;
                max = (entryValues.length * i7) / 4;
                if (max == entryValues.length) {
                    max--;
                }
                String value = entryValues[max].toString();
                int index = max;
                max = new StringBuilder();
                if (i > 0) {
                    entryValues2 = entryValues;
                    max.append(43);
                } else {
                    entryValues2 = entryValues;
                }
                this.mExposureCompensationValues.add(value);
                ArrayList arrayList = this.mExposureCompensationTitles;
                max.append(i);
                entries2 = entries;
                arrayList.add(max.toString());
                i6 = i7 + 1;
                max = maxValue;
                entryValues = entryValues2;
                entries = entries2;
            } else {
                entryValues2 = entryValues;
                entries2 = entries;
                initCurState();
                return;
            }
        }
    }

    private void initCurState() {
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        this.mCurWBState = settingsManager.getString(this.mActivity.getModuleScope(), this.cameraId != 1 ? Keys.KEY_CUR_WHITE_BALANCE_STATE : Keys.KEY_CUR_WHITE_BALANCE_STATE_FRONT);
        this.mCurFocusState = settingsManager.getString(this.mActivity.getModuleScope(), this.cameraId != 1 ? Keys.KEY_CUR_FOCUS_STATE : Keys.KEY_CUR_FOCUS_STATE_FRONT);
        this.mCurIsoState = settingsManager.getString(this.mActivity.getModuleScope(), this.cameraId != 1 ? Keys.KEY_MANUAL_ISO_STATE : Keys.KEY_MANUAL_ISO_STATE_FRONT);
        this.mCurExposureTimeState = settingsManager.getString(this.mActivity.getModuleScope(), this.cameraId != 1 ? Keys.KEY_CUR_EXPOSURE_TIME_STATE : Keys.KEY_CUR_EXPOSURE_TIME_STATE_FRONT);
        this.mCurExposureCompensationState = settingsManager.getString(this.mActivity.getModuleScope(), this.cameraId != 1 ? Keys.KEY_MANUAL_EXPOSURE_COMPENSATION_STATE : Keys.KEY_MANUAL_EXPOSURE_COMPENSATION_STATE_FRONT);
        try {
            initViewSettings();
        } catch (NullPointerException e) {
        }
    }

    private void initViewSettings() {
        this.mArcSeekBarAWB.setWBTitles(this.mWBTitles);
        this.mArcSeekBarIOS.setISOValues(this.mISOValues);
        this.mArcSeekBarET.setExposureTimeTitles(this.mExposureTimeTitles);
        this.mArcSeekBarEC.setExposureCompensationTitles(this.mExposureCompensationTitles);
        this.mArcSeekBarAWB.initValue(this.mCurWBState);
        this.mArcSeekBarIOS.initValue(this.mCurIsoState);
        this.mArcSeekBarFM.initValue(this.mCurFocusState);
        this.mArcSeekBarET.initValue(this.mCurExposureTimeState);
        this.mArcSeekBarEC.initValue(this.mCurExposureCompensationState);
    }

    public void OnProgressChanged(int mode, int level) {
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[ProArcSeekBarUI] OnProgressChanged mode = ");
        stringBuilder.append(mode);
        stringBuilder.append(" level = ");
        stringBuilder.append(level);
        Log.d(tag, stringBuilder.toString());
        boolean auto = level == 1;
        if (mode == 4) {
            auto = level == 3;
        }
        if (this.manualStateChangeListener != null) {
            if (mode == 3) {
                if (auto) {
                    this.mArcSeekBarEC.setECEnabled(true);
                } else {
                    this.mArcSeekBarEC.setECEnabled(false);
                }
            }
            this.manualStateChangeListener.onManualSettingChanged(mode, level, level - 1, auto);
        }
    }

    public void onMovementChanged(View view, boolean touch) {
        if (view.getId() == R.id.arc_common) {
            return;
        }
        if (touch) {
            hideAllArcSeekBar(view, false);
        } else {
            showAllArcSeekBar(view, false);
        }
    }

    public void hideAllArcSeekBar(View view, boolean isAnimation) {
        if (view == null) {
            setActionMenuVisibility(1001);
        }
        for (ArcSeekBar seekBar : this.mSeekBarList) {
            if (view == null || view.getId() != seekBar.getId()) {
                rotateAnimEnd(seekBar, isAnimation);
            }
        }
    }

    public void showAllArcSeekBar(View view, boolean isAnimation) {
        View asLayout = this.mRootView.findViewById(R.id.manual_arcseekbar_layout);
        if (asLayout != null) {
            asLayout.setVisibility(0);
        }
        if (view == null) {
            setActionMenuVisibility(1000);
        }
        if (this.mArcSeekBarCOM.getVisibility() == 0) {
            this.mArcSeekBarCOM.setVisibility(8);
        }
        initCurState();
        for (ArcSeekBar seekBar : this.mSeekBarList) {
            if (view == null || view.getId() != seekBar.getId()) {
                rotateAnimStart(seekBar, isAnimation);
            }
        }
    }

    public void setActionMenuVisibility(int visibility) {
        if (this.manualStateChangeListener != null) {
            this.manualStateChangeListener.onVisibilityChanged(visibility);
        }
    }

    private void rotateAnimStart(final ArcSeekBar arcView, boolean isAnimation) {
        if (isAnimation) {
            Animation animStart = new RotateAnimation(-90.0f, 0.0f, 1, 0.5f, 1, 1.0f);
            animStart.setDuration(500);
            animStart.setInterpolator(new DecelerateInterpolator());
            arcView.startAnimation(animStart);
            animStart.setAnimationListener(new AnimationListener() {
                public void onAnimationStart(Animation animation) {
                    arcView.setEnabled(true);
                }

                public void onAnimationEnd(Animation animation) {
                    Tag access$000 = ProArcSeekBarUI.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Pro animStart onAnimationEnd id = ");
                    stringBuilder.append(ProArcSeekBarUI.this.mActivity.getCurrentModuleIndex());
                    Log.d(access$000, stringBuilder.toString());
                    if (ProArcSeekBarUI.this.mActivity.getCurrentModuleIndex() != ProArcSeekBarUI.this.mActivity.getResources().getInteger(R.integer.camera_mode_pro)) {
                        for (ArcSeekBar seekBar : ProArcSeekBarUI.this.mSeekBarList) {
                            ProArcSeekBarUI.this.rotateAnimEnd(seekBar, false);
                        }
                        return;
                    }
                    ProArcSeekBarUI.this.mActivity.getCameraAppUI().setStereoModeStripViewSlideable(false);
                }

                public void onAnimationRepeat(Animation animation) {
                }
            });
        }
        arcView.setVisibility(0);
    }

    private void rotateAnimEnd(final ArcSeekBar arcView, boolean isAnimation) {
        if (isAnimation) {
            Animation animEnd = new RotateAnimation(0.0f, 90.0f, 1, 0.5f, 1, 1.0f);
            animEnd.setDuration(500);
            animEnd.setInterpolator(new AccelerateInterpolator());
            arcView.startAnimation(animEnd);
            animEnd.setAnimationListener(new AnimationListener() {
                public void onAnimationStart(Animation animation) {
                    arcView.setEnabled(false);
                }

                public void onAnimationEnd(Animation animation) {
                    Tag access$000 = ProArcSeekBarUI.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Pro animEnd onAnimationEnd id = ");
                    stringBuilder.append(ProArcSeekBarUI.this.mActivity.getCurrentModuleIndex());
                    Log.d(access$000, stringBuilder.toString());
                    ProArcSeekBarUI.this.mActivity.getCameraAppUI().setStereoModeStripViewSlideable(true);
                }

                public void onAnimationRepeat(Animation animation) {
                }
            });
        }
        arcView.setVisibility(8);
    }

    public void setManualStateChangeListener(ManualStateChangeListener manualStateChangeListener) {
        this.manualStateChangeListener = manualStateChangeListener;
    }

    public void removeManualStateChangeListener() {
        this.manualStateChangeListener = null;
    }

    public void updateComBar(int arcMode, boolean isShow) {
        this.mArcSeekBarCOM.setVisibility(8);
        if (isShow) {
            View asLayout = this.mRootView.findViewById(R.id.manual_arcseekbar_layout);
            if (asLayout != null) {
                asLayout.setVisibility(0);
            }
            initCurState();
            String title = "";
            switch (arcMode) {
                case 0:
                    this.mArcSeekBarCOM.setArcMode(arcMode);
                    title = this.mActivity.getString(R.string.pro_mode_arc_title_wab);
                    this.mArcSeekBarCOM.setWBTitles(this.mWBTitles);
                    this.mArcSeekBarCOM.initValue(this.mCurWBState);
                    this.mArcSeekBarCOM.setWabBallSrcIdArray(this.srcId);
                    this.mArcSeekBarCOM.setWabBallSrcIdPressArray(this.srcIdPress);
                    break;
                case 1:
                    this.mArcSeekBarCOM.setArcMode(arcMode);
                    title = this.mActivity.getString(R.string.pro_mode_arc_title_focus_mode);
                    this.mArcSeekBarCOM.initValue(this.mCurFocusState);
                    this.mArcSeekBarCOM.setBallSrcId(R.drawable.focus_normal);
                    this.mArcSeekBarCOM.setBallSrcIdPress(R.drawable.focus_press);
                    break;
                case 2:
                    this.mArcSeekBarCOM.setArcMode(arcMode);
                    title = this.mActivity.getString(R.string.pro_mode_arc_title_iso);
                    this.mArcSeekBarCOM.setISOValues(this.mISOValues);
                    this.mArcSeekBarCOM.initValue(this.mCurIsoState);
                    this.mArcSeekBarCOM.setBallSrcId(R.drawable.iso_normal);
                    this.mArcSeekBarCOM.setBallSrcIdPress(R.drawable.iso_press);
                    break;
                case 3:
                    this.mArcSeekBarCOM.setArcMode(arcMode);
                    title = this.mActivity.getString(R.string.pro_mode_arc_title_shutter_speed);
                    this.mArcSeekBarCOM.setExposureTimeTitles(this.mExposureTimeTitles);
                    this.mArcSeekBarCOM.initValue(this.mCurExposureTimeState);
                    this.mArcSeekBarCOM.setBallSrcId(R.drawable.shutterspeed_normal);
                    this.mArcSeekBarCOM.setBallSrcIdPress(R.drawable.shutterspeed_press);
                    break;
                case 4:
                    this.mArcSeekBarCOM.setArcMode(arcMode);
                    title = this.mActivity.getString(R.string.pro_mode_arc_title_exposure_compensation);
                    this.mArcSeekBarCOM.setExposureCompensationTitles(this.mExposureCompensationTitles);
                    this.mArcSeekBarCOM.initValue(this.mCurExposureCompensationState);
                    this.mArcSeekBarCOM.setBallSrcId(R.drawable.ev_normal);
                    this.mArcSeekBarCOM.setBallSrcIdPress(R.drawable.ev_press);
                    break;
            }
            this.mArcSeekBarCOM.setDisplayTitle(true);
            this.mArcSeekBarCOM.setTitleStr(title);
            rotateAnimStart(this.mArcSeekBarCOM, true);
            return;
        }
        this.mArcSeekBarCOM.setVisibility(8);
        setActionMenuVisibility(1002);
    }

    public void resetSeekBar() {
        Log.d(TAG, "[ProArcSeekBarUI] resetSeekBar");
        SettingsManager settingsManager = this.mActivity.getSettingsManager();
        for (ArcSeekBar seekBar : this.mSeekBarList) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[ProArcSeekBarUI] ArcMode = ");
            stringBuilder.append(seekBar.getArcMode());
            Log.d(tag, stringBuilder.toString());
            int settingType = seekBar.getArcMode();
            Map<String, Object> curState = new HashMap();
            curState.put("auto", Boolean.valueOf(true));
            curState.put("progress", Integer.valueOf(1));
            curState.put(ManualUI.SETTING_INDEX, Integer.valueOf(settingType == 4 ? 2 : 0));
            String jsonString = CameraUtil.serializeToJson(curState);
            switch (settingType) {
                case 0:
                    settingsManager.set(this.mActivity.getModuleScope(), this.cameraId != 1 ? Keys.KEY_CUR_WHITE_BALANCE_STATE : Keys.KEY_CUR_WHITE_BALANCE_STATE_FRONT, jsonString);
                    break;
                case 1:
                    settingsManager.set(this.mActivity.getModuleScope(), this.cameraId != 1 ? Keys.KEY_CUR_FOCUS_STATE : Keys.KEY_CUR_FOCUS_STATE_FRONT, jsonString);
                    break;
                case 2:
                    settingsManager.set(this.mActivity.getModuleScope(), this.cameraId != 1 ? Keys.KEY_MANUAL_ISO_STATE : Keys.KEY_MANUAL_ISO_STATE_FRONT, jsonString);
                    break;
                case 3:
                    settingsManager.set(this.mActivity.getModuleScope(), this.cameraId != 1 ? Keys.KEY_CUR_EXPOSURE_TIME_STATE : Keys.KEY_CUR_EXPOSURE_TIME_STATE_FRONT, jsonString);
                    break;
                case 4:
                    settingsManager.set(this.mActivity.getModuleScope(), this.cameraId != 1 ? Keys.KEY_MANUAL_EXPOSURE_COMPENSATION_STATE : Keys.KEY_MANUAL_EXPOSURE_COMPENSATION_STATE_FRONT, jsonString);
                    break;
                default:
                    break;
            }
        }
    }

    public void onDestroy() {
        if (this.mActivity != null) {
            this.mActivity.removeRotatableFromListenerPool(this.mArcSeekBarAWB.hashCode());
            this.mActivity.removeRotatableFromListenerPool(this.mArcSeekBarFM.hashCode());
            this.mActivity.removeRotatableFromListenerPool(this.mArcSeekBarIOS.hashCode());
            this.mActivity.removeRotatableFromListenerPool(this.mArcSeekBarET.hashCode());
            this.mActivity.removeRotatableFromListenerPool(this.mArcSeekBarEC.hashCode());
            this.mActivity.removeRotatableFromListenerPool(this.mArcSeekBarCOM.hashCode());
        }
    }
}
