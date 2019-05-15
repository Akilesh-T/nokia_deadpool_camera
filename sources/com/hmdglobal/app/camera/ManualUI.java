package com.hmdglobal.app.camera;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import com.android.ex.camera2.portability.CameraCapabilities;
import com.android.ex.camera2.portability.CameraCapabilities.Stringifier;
import com.android.ex.camera2.portability.CameraSettings;
import com.hmdglobal.app.camera.HelpTipsManager.NextButtonListener;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.app.CameraAppUI;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.ui.ManualGroup;
import com.hmdglobal.app.camera.ui.ManualGroupWrapper;
import com.hmdglobal.app.camera.ui.ManualItem;
import com.hmdglobal.app.camera.ui.ManualItem.ManualStateChangeListener;
import com.hmdglobal.app.camera.ui.ModuleLayoutWrapper;
import com.hmdglobal.app.camera.ui.Rotatable.RotateEntity;
import com.hmdglobal.app.camera.ui.RotateLayout;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.hmdglobal.app.camera.util.CustomFields;
import com.hmdglobal.app.camera.util.CustomUtil;
import com.hmdglobal.app.camera.widget.FloatingActionsMenu;
import com.hmdglobal.app.camera.widget.FloatingActionsMenu.ManualMenuExpandChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

public class ManualUI extends PhotoUI implements ManualStateChangeListener, ManualMenuExpandChangeListener, NextButtonListener, OnClickListener, OnTouchListener {
    public static final String SETTING_AUTO = "auto";
    public static final String SETTING_INDEX = "index";
    public static final String SETTING_PROGRESS = "progress";
    private static final String TAG = "ManualUI";
    private int cameraId = -1;
    private CheckBox cbEC;
    private CheckBox cbET;
    private CheckBox cbFOCUS;
    private CheckBox cbISO;
    private List<CheckBox> cbList = new ArrayList();
    private CheckBox cbWEB;
    private CameraAppUI mCameraAppUI;
    private ArrayList<String> mExposureCompensationTitles = new ArrayList();
    private ArrayList<String> mExposureCompensationValues = new ArrayList();
    private ArrayList<String> mExposureTimeDouble = new ArrayList();
    private ArrayList<String> mExposureTimeTitles = new ArrayList();
    private HelpTipsManager mHelpTipsManager;
    private ArrayList<Integer> mISOValues = new ArrayList();
    private boolean mIsFirstUseManual = false;
    private ManualItem mItemF;
    private ManualItem mItemISO;
    private ManualItem mItemS;
    private ManualItem mItemWb;
    private LinearLayout mLLActionMenu;
    private ManualGroup mManualGroup;
    private FloatingActionsMenu mManualMenu;
    private ManualModeCallBackListener mManualModeCallBackListener;
    private int mMaxFocusPos = 0;
    private int mMinFocusPos = 0;
    private RotateLayout mRotateEC;
    private RotateLayout mRotateET;
    private RotateLayout mRotateFOCUS;
    private RotateLayout mRotateISO;
    private RotateLayout mRotateWEB;
    private ArrayList<String> mWBTitles = new ArrayList();
    private ArrayList<String> mWBValues = new ArrayList();

    public interface ManualModeCallBackListener {
        void updateExposureCompensation(boolean z, String str);

        void updateExposureTime(boolean z, String str);

        void updateISOValue(boolean z, int i);

        void updateManualFocusValue(boolean z, int i);

        void updateWBValue(boolean z, String str);
    }

    public ManualUI(CameraActivity activity, PhotoController controller, View parent, ManualModeCallBackListener l) {
        super(activity, controller, parent);
        this.mActivity.getLayoutInflater().inflate(R.layout.manual_items_layout, (ModuleLayoutWrapper) parent.findViewById(R.id.module_layout), true);
        this.mManualModeCallBackListener = l;
        this.mManualGroup = (ManualGroup) parent.findViewById(R.id.manual_items_layout);
        ManualGroupWrapper wrapper = (ManualGroupWrapper) parent.findViewById(R.id.manual_group_wrapper);
        this.mCameraAppUI = activity.getCameraAppUI();
        this.mManualGroup.setCaptureLayoutHelper(this.mCameraAppUI.getCaptureLayoutHelper());
        wrapper.setCaptureLayoutHelper(this.mCameraAppUI.getCaptureLayoutHelper());
        this.mManualGroup.setVisibility(8);
        this.mItemISO = (ManualItem) parent.findViewById(R.id.item_iso);
        this.mItemS = (ManualItem) parent.findViewById(R.id.item_s);
        this.mItemWb = (ManualItem) parent.findViewById(R.id.item_wb);
        this.mItemF = (ManualItem) parent.findViewById(R.id.item_f);
        this.mManualMenu = (FloatingActionsMenu) parent.findViewById(R.id.multiple_actions);
        this.mActivity.addRotatableToListenerPool(new RotateEntity(this.mItemISO, true));
        this.mActivity.addRotatableToListenerPool(new RotateEntity(this.mItemS, true));
        this.mActivity.addRotatableToListenerPool(new RotateEntity(this.mItemWb, true));
        this.mActivity.addRotatableToListenerPool(new RotateEntity(this.mItemF, true));
        this.mActivity.addLockableToListenerPool(this.mManualMenu);
        activity.getCameraAppUI().addManualModeListener(this.mManualGroup);
        this.mLLActionMenu = (LinearLayout) parent.findViewById(R.id.ll_multiple_actions);
        this.mLLActionMenu.setVisibility(8);
        this.mRotateWEB = (RotateLayout) parent.findViewById(R.id.rotate_web);
        this.mRotateISO = (RotateLayout) parent.findViewById(R.id.rotate_iso);
        this.mRotateFOCUS = (RotateLayout) parent.findViewById(R.id.rotate_focus);
        this.mRotateET = (RotateLayout) parent.findViewById(R.id.rotate_exposure_time);
        this.mRotateEC = (RotateLayout) parent.findViewById(R.id.rotate_exposure_compensation);
        this.cbWEB = (CheckBox) parent.findViewById(R.id.cb_web);
        this.cbFOCUS = (CheckBox) parent.findViewById(R.id.cb_focus);
        this.cbISO = (CheckBox) parent.findViewById(R.id.cb_iso);
        this.cbET = (CheckBox) parent.findViewById(R.id.cb_shutter);
        this.cbEC = (CheckBox) parent.findViewById(R.id.cb_exposure);
        this.cbList.add(this.cbWEB);
        this.cbList.add(this.cbFOCUS);
        this.cbList.add(this.cbISO);
        this.cbList.add(this.cbET);
        this.cbList.add(this.cbEC);
        this.mActivity.addRotatableToListenerPool(new RotateEntity(this.mRotateWEB, true));
        this.mActivity.addRotatableToListenerPool(new RotateEntity(this.mRotateISO, true));
        this.mActivity.addRotatableToListenerPool(new RotateEntity(this.mRotateFOCUS, true));
        this.mActivity.addRotatableToListenerPool(new RotateEntity(this.mRotateET, true));
        this.mActivity.addRotatableToListenerPool(new RotateEntity(this.mRotateEC, true));
        this.cbWEB.setOnClickListener(this);
        this.cbFOCUS.setOnClickListener(this);
        this.cbISO.setOnClickListener(this);
        this.cbET.setOnClickListener(this);
        this.cbEC.setOnClickListener(this);
        this.cbWEB.setOnTouchListener(this);
        this.cbFOCUS.setOnTouchListener(this);
        this.cbISO.setOnTouchListener(this);
        this.cbET.setOnTouchListener(this);
        this.cbEC.setOnTouchListener(this);
    }

    public void onAllViewRemoved(AppController controller) {
        super.onAllViewRemoved(controller);
        controller.removeRotatableFromListenerPool(this.mItemISO.hashCode());
        controller.removeRotatableFromListenerPool(this.mItemS.hashCode());
        controller.removeRotatableFromListenerPool(this.mItemWb.hashCode());
        controller.removeRotatableFromListenerPool(this.mItemF.hashCode());
        controller.removeLockableFromListenerPool(this.mManualMenu);
        this.mCameraAppUI.removeManualModeListener(this.mManualGroup);
        this.mCameraAppUI.getProArcSeekBarUI().removeManualStateChangeListener();
        controller.removeRotatableFromListenerPool(this.mRotateWEB.hashCode());
        controller.removeRotatableFromListenerPool(this.mRotateISO.hashCode());
        controller.removeRotatableFromListenerPool(this.mRotateFOCUS.hashCode());
        controller.removeRotatableFromListenerPool(this.mRotateET.hashCode());
        controller.removeRotatableFromListenerPool(this.mRotateEC.hashCode());
    }

    public void collapseManualMenu() {
        this.mManualMenu.collapse();
    }

    public void onCameraOpened(CameraCapabilities capabilities, CameraSettings settings) {
        super.onCameraOpened(capabilities, settings);
        initManualSettings(capabilities);
        this.mManualGroup.setVisibility(0);
        this.mCameraAppUI.getProArcSeekBarUI().setManualStateChangeListener(this);
        this.cameraId = this.mActivity.getCurrentCameraId();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[ManualUI]Pro cameraId = ");
        stringBuilder.append(this.cameraId);
        Log.d(str, stringBuilder.toString());
        if (this.cameraId == 0) {
            this.mRotateFOCUS.setVisibility(0);
        } else {
            this.mRotateFOCUS.setVisibility(8);
        }
    }

    public boolean isManualMode(String key) {
        String curState = this.mActivity.getSettingsManager().getString(this.mActivity.getModuleScope(), key);
        boolean z = false;
        if (curState == null) {
            return false;
        }
        try {
            if (!((Boolean) CameraUtil.parseJSON(curState, "auto")).booleanValue()) {
                z = true;
            }
            return z;
        } catch (JSONException e) {
            return false;
        }
    }

    private void initManualSettings(CameraCapabilities capabilities) {
        int i;
        int i2;
        int maxIso;
        ManualUI manualUI = this;
        Stringifier stringifier = capabilities.getStringifier();
        String[] wbValues = manualUI.mActivity.getResources().getStringArray(R.array.white_balance_values);
        String[] wbStrings = manualUI.mActivity.getResources().getStringArray(R.array.white_balance_strings);
        manualUI.mWBValues = new ArrayList();
        manualUI.mWBTitles = new ArrayList();
        for (i = 0; i < wbValues.length; i++) {
            if (capabilities.supports(stringifier.whiteBalanceFromString(wbValues[i]))) {
                manualUI.mWBValues.add(wbValues[i]);
                manualUI.mWBTitles.add(wbStrings[i]);
            }
        }
        CameraCapabilities cameraCapabilities = capabilities;
        manualUI.mMinFocusPos = capabilities.getMinFocusScale();
        manualUI.mMaxFocusPos = capabilities.getMaxFocusScale();
        i = capabilities.getMinISO();
        int maxIso2 = capabilities.getMaxIso();
        int[] isoValues = manualUI.mActivity.getResources().getIntArray(R.array.camera_iso_values);
        manualUI.mISOValues = new ArrayList();
        int i3 = 0;
        while (i3 < isoValues.length) {
            if (isoValues[i3] >= i && isoValues[i3] <= maxIso2) {
                manualUI.mISOValues.add(Integer.valueOf(isoValues[i3]));
            }
            i3++;
        }
        String minExposureTime = capabilities.getMinExposureTime();
        String maxExposureTime = capabilities.getMaxExposureTime();
        String[] exposureTimeDouble = manualUI.mActivity.getResources().getStringArray(R.array.exposure_time_double);
        String[] exposureTimeString = manualUI.mActivity.getResources().getStringArray(R.array.exposure_time_string);
        manualUI.mExposureTimeDouble = new ArrayList();
        manualUI.mExposureTimeTitles = new ArrayList();
        long maxT = Float.valueOf(maxExposureTime).longValue();
        long minT = Float.valueOf(minExposureTime).longValue();
        int i4 = 0;
        while (true) {
            Stringifier stringifier2 = stringifier;
            i2 = i4;
            if (i2 >= exposureTimeDouble.length) {
                break;
            }
            String[] wbValues2;
            if (Float.valueOf(exposureTimeDouble[i2]).longValue() > maxT || Float.valueOf(exposureTimeDouble[i2]).longValue() < minT) {
                wbValues2 = wbValues;
            } else {
                wbValues2 = wbValues;
                manualUI.mExposureTimeDouble.add(exposureTimeDouble[i2]);
                manualUI.mExposureTimeTitles.add(exposureTimeString[i2]);
            }
            i4 = i2 + 1;
            stringifier = stringifier2;
            wbValues = wbValues2;
        }
        float step = capabilities.getExposureCompensationStep();
        int max = capabilities.getMaxExposureCompensation();
        int min = capabilities.getMinExposureCompensation();
        manualUI.mExposureCompensationValues = new ArrayList();
        manualUI.mExposureCompensationTitles = new ArrayList();
        max = (int) Math.floor((double) (((float) max) * step));
        int minValue = (int) Math.ceil((double) (((float) min) * step));
        CharSequence[] entries = new CharSequence[((max - minValue) + 1)];
        int minValue2 = minValue;
        CharSequence[] entryValues = new CharSequence[((max - minValue) + 1)];
        i4 = minValue2;
        while (true) {
            int minIso = i;
            i = i4;
            if (i > max) {
                break;
            }
            float step2;
            maxIso = maxIso2;
            entryValues[max + i] = Integer.toString(Math.round(((float) i) / step));
            StringBuilder builder = new StringBuilder();
            if (i > 0) {
                step2 = step;
                builder.append(6.0E-44f);
            } else {
                step2 = step;
            }
            int i5 = max + i;
            builder.append(i);
            entries[i5] = builder.toString();
            i4 = i + 1;
            i = minIso;
            maxIso2 = maxIso;
            step = step2;
        }
        maxIso = maxIso2;
        int i6 = 0;
        while (true) {
            i2 = i6;
            CharSequence[] entryValues2;
            if (i2 <= 4) {
                maxIso2 = i2 - 2;
                int maxValue = max;
                max = (entryValues.length * i2) / 4;
                if (max == entryValues.length) {
                    max--;
                }
                String value = entryValues[max].toString();
                int index = max;
                max = new StringBuilder();
                if (maxIso2 > 0) {
                    entryValues2 = entryValues;
                    max.append(43);
                } else {
                    entryValues2 = entryValues;
                }
                manualUI.mExposureCompensationValues.add(value);
                ArrayList arrayList = manualUI.mExposureCompensationTitles;
                max.append(maxIso2);
                arrayList.add(max.toString());
                i6 = i2 + 1;
                max = maxValue;
                entryValues = entryValues2;
                manualUI = this;
            } else {
                entryValues2 = entryValues;
                return;
            }
        }
    }

    public void onManualSettingChanged(int settingType, int progressValue, int index, boolean auto) {
        if (this.mExposureCompensationValues.size() > 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[ManualUI] onManualSettingChanged progressValue: ");
            stringBuilder.append(progressValue);
            stringBuilder.append(",  settingType :");
            stringBuilder.append(settingType);
            stringBuilder.append(", ");
            stringBuilder.append(index);
            stringBuilder.append(", ");
            stringBuilder.append(auto);
            Log.d(str, stringBuilder.toString());
            SettingsManager settingsManager = this.mActivity.getSettingsManager();
            if (this.mManualModeCallBackListener != null && index >= 0) {
                Map<String, Object> curState = new HashMap();
                curState.put("auto", Boolean.valueOf(auto));
                curState.put("progress", Integer.valueOf(progressValue));
                curState.put(SETTING_INDEX, Integer.valueOf(index));
                String jsonString = CameraUtil.serializeToJson(curState);
                switch (settingType) {
                    case 0:
                        settingsManager.set(this.mActivity.getModuleScope(), this.cameraId != 1 ? Keys.KEY_CUR_WHITE_BALANCE_STATE : Keys.KEY_CUR_WHITE_BALANCE_STATE_FRONT, jsonString);
                        this.mManualModeCallBackListener.updateWBValue(auto, (String) this.mWBValues.get(index));
                        break;
                    case 1:
                        settingsManager.set(this.mActivity.getModuleScope(), this.cameraId != 1 ? Keys.KEY_CUR_FOCUS_STATE : Keys.KEY_CUR_FOCUS_STATE_FRONT, jsonString);
                        this.mManualModeCallBackListener.updateManualFocusValue(auto, index);
                        break;
                    case 2:
                        settingsManager.set(this.mActivity.getModuleScope(), this.cameraId != 1 ? Keys.KEY_MANUAL_ISO_STATE : Keys.KEY_MANUAL_ISO_STATE_FRONT, jsonString);
                        this.mManualModeCallBackListener.updateISOValue(auto, ((Integer) this.mISOValues.get(index)).intValue());
                        break;
                    case 3:
                        updateECState(auto, settingsManager.getString(this.mActivity.getModuleScope(), this.cameraId != 1 ? Keys.KEY_MANUAL_EXPOSURE_COMPENSATION_STATE : Keys.KEY_MANUAL_EXPOSURE_COMPENSATION_STATE_FRONT));
                        if (index >= this.mExposureTimeDouble.size()) {
                            index = this.mExposureTimeDouble.size() - 1;
                            progressValue = this.mExposureTimeDouble.size();
                            Map<String, Object> currentState = new HashMap();
                            currentState.put("auto", Boolean.valueOf(auto));
                            currentState.put("progress", Integer.valueOf(progressValue));
                            currentState.put(SETTING_INDEX, Integer.valueOf(index));
                            jsonString = CameraUtil.serializeToJson(curState);
                        }
                        settingsManager.set(this.mActivity.getModuleScope(), this.cameraId != 1 ? Keys.KEY_CUR_EXPOSURE_TIME_STATE : Keys.KEY_CUR_EXPOSURE_TIME_STATE_FRONT, jsonString);
                        this.mManualModeCallBackListener.updateExposureTime(auto, (String) this.mExposureTimeDouble.get(index));
                        break;
                    case 4:
                        settingsManager.set(this.mActivity.getModuleScope(), this.cameraId != 1 ? Keys.KEY_MANUAL_EXPOSURE_COMPENSATION_STATE : Keys.KEY_MANUAL_EXPOSURE_COMPENSATION_STATE_FRONT, jsonString);
                        this.mManualModeCallBackListener.updateExposureCompensation(auto, (String) this.mExposureCompensationValues.get(index));
                        break;
                }
                updateActionMenuUI(auto, settingType, index);
            }
        }
    }

    private void updateECState(boolean auto, String currentState) {
        boolean ecAuto = false;
        if (auto) {
            this.cbEC.setEnabled(true);
            if (currentState != null) {
                try {
                    int level = ((Integer) new JSONObject(currentState).get(SETTING_INDEX)).intValue();
                    if (level == 2) {
                        ecAuto = true;
                    }
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Pro chen6 ecAuto = ");
                    stringBuilder.append(ecAuto);
                    stringBuilder.append(" level = ");
                    stringBuilder.append(level);
                    Log.d(str, stringBuilder.toString());
                    if (ecAuto) {
                        this.cbEC.setText("0");
                        return;
                    } else {
                        this.cbEC.setText((CharSequence) this.mExposureCompensationTitles.get(level));
                        return;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }
            }
            return;
        }
        this.cbEC.setEnabled(false);
        this.cbEC.setText(null);
    }

    private void updateActionMenuUI(boolean auto, int settingType, int index) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[ManualUI] updateActionMenuUI auto = ");
        stringBuilder.append(auto);
        stringBuilder.append(" settingType = ");
        stringBuilder.append(settingType);
        stringBuilder.append(" index = ");
        stringBuilder.append(index);
        Log.d(str, stringBuilder.toString());
        switch (settingType) {
            case 0:
                if (auto) {
                    this.cbWEB.setText(R.string.pro_mode_arc_auto);
                    return;
                } else {
                    this.cbWEB.setText((CharSequence) this.mWBTitles.get(index));
                    return;
                }
            case 1:
                if (auto) {
                    this.cbFOCUS.setText(R.string.pro_mode_arc_auto);
                    return;
                } else if (index == 98) {
                    this.cbFOCUS.setText(R.string.pro_mode_arc_focus_mode_precent2);
                    return;
                } else {
                    this.cbFOCUS.setText(R.string.pro_mode_arc_focus_mode_precent1);
                    return;
                }
            case 2:
                if (auto) {
                    this.cbISO.setText(R.string.pro_mode_arc_auto);
                    return;
                } else {
                    this.cbISO.setText(String.valueOf(this.mISOValues.get(index)));
                    return;
                }
            case 3:
                if (auto) {
                    this.cbET.setText(R.string.pro_mode_arc_auto);
                    return;
                } else {
                    this.cbET.setText((CharSequence) this.mExposureTimeTitles.get(index));
                    return;
                }
            case 4:
                if (auto) {
                    this.cbEC.setText("0");
                    return;
                } else {
                    this.cbEC.setText((CharSequence) this.mExposureCompensationTitles.get(index));
                    return;
                }
            default:
                return;
        }
    }

    public void onVisibilityChanged(int settingType) {
        switch (settingType) {
            case 1000:
                this.mLLActionMenu.setVisibility(8);
                break;
            case 1001:
                this.mLLActionMenu.setVisibility(0);
                break;
            case 1002:
                updateCheckBoxState(new CheckBox(this.mActivity));
                break;
        }
        if (this.mIsFirstUseManual && this.mHelpTipsManager != null) {
            this.mHelpTipsManager.notifyEventFinshed();
            this.mIsFirstUseManual = false;
            this.mManualMenu.mAddButton.setEnabled(true);
            this.mManualMenu.setMenuExpandChangeListener(null, false);
        }
    }

    public void initManualUIForTutorial() {
        this.mIsFirstUseManual = this.mActivity.getSettingsManager().getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_HELP_TIP_MANUAL_FINISHED, false) ^ 1;
        this.mManualMenu.setMenuExpandChangeListener(this, this.mIsFirstUseManual);
        this.mHelpTipsManager = this.mActivity.getHelpTipsManager();
        if (this.mHelpTipsManager != null) {
            this.mHelpTipsManager.setManualUpdateUIListener(this);
            if (this.mIsFirstUseManual) {
                this.mManualMenu.toggleForTutorial();
            }
        }
    }

    public void onUpdateUIChangedFromTutorial() {
        this.mIsFirstUseManual = false;
        this.mManualMenu.setMenuExpandChangeListener(null, false);
    }

    public void onClick(View view) {
        boolean isChecked = ((CheckBox) view).isChecked();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Pro [ManualUI] onClick  view.getId() = ");
        stringBuilder.append(view.getId());
        stringBuilder.append(" isChecked = ");
        stringBuilder.append(isChecked);
        Log.d(str, stringBuilder.toString());
        int arcMode = -1;
        switch (view.getId()) {
            case R.id.cb_exposure /*2131230874*/:
                arcMode = 4;
                break;
            case R.id.cb_focus /*2131230875*/:
                arcMode = 1;
                break;
            case R.id.cb_iso /*2131230876*/:
                arcMode = 2;
                break;
            case R.id.cb_shutter /*2131230877*/:
                arcMode = 3;
                break;
            case R.id.cb_web /*2131230878*/:
                arcMode = 0;
                break;
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("[ManualUI] onClick arcMode = ");
        stringBuilder2.append(arcMode);
        Log.d(str2, stringBuilder2.toString());
        this.mCameraAppUI.updateComBarState(arcMode, isChecked);
        updateCheckBoxState((CheckBox) view);
    }

    public void updateCheckBoxState(CheckBox view) {
        for (CheckBox checkBox : this.cbList) {
            if (checkBox.getId() != view.getId()) {
                checkBox.setChecked(false);
            }
        }
    }

    public boolean onTouch(View view, MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (action != 3) {
            switch (action) {
                case 0:
                    Log.d(TAG, "Pro [ManualUI] onTouch ACTION_DOWN forceBlocking");
                    this.mActivity.getLockEventListener().forceBlocking();
                    break;
                case 1:
                    Log.d(TAG, "Pro [ManualUI] onTouch ACTION_UP onIdle");
                    break;
            }
        }
        Log.d(TAG, "Pro [ManualUI] onTouch ACTION_CANCEL onIdle");
        this.mActivity.getLockEventListener().onIdle();
        return false;
    }

    public void onManualMenuExpandChanged(boolean expand) {
        if (CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_HELP_TIP_TUTORIAL, false) && this.mIsFirstUseManual && this.mHelpTipsManager != null && expand) {
            this.mHelpTipsManager.createAndShowHelpTip(2, true);
        }
    }

    public void onManualMenuClick() {
        if (CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_HELP_TIP_TUTORIAL, false) && this.mIsFirstUseManual) {
            this.mIsFirstUseManual = false;
            if (this.mHelpTipsManager != null && this.mHelpTipsManager.isHelpTipShowExist()) {
                this.mHelpTipsManager.notifyEventFinshed();
            }
        }
    }
}
