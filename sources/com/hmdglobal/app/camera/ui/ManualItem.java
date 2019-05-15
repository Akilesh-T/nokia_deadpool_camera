package com.hmdglobal.app.camera.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.hmdglobal.app.camera.AnimationManager;
import com.hmdglobal.app.camera.ManualUI;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.ui.CustomSeekBar.EnableStateChangedCallback;
import java.util.ArrayList;
import org.json.JSONException;
import org.json.JSONObject;

public class ManualItem extends LinearLayout implements OnClickListener, Rotatable, EnableStateChangedCallback {
    private static final int HIDE_MANUAL_PROGRESS = 0;
    private static final int HIDE_MANUAL_PROGRESS_DELAY = 3000;
    public static final int MANUAL_SETTING_EXPOSURE_COMPENSATION = 4;
    public static final int MANUAL_SETTING_EXPOSURE_TIME = 3;
    public static final int MANUAL_SETTING_FOCUS_POS = 1;
    public static final int MANUAL_SETTING_ISO = 2;
    public static final int MANUAL_SETTING_WHITE_BALANCE = 0;
    private static final String TAG = "ManualItem";
    private static final int UPDATE_MANUAL_SETTING = 1;
    private static final int UPDATE_MANUAL_SETTING_DELAY = 100;
    private final String INFINITE_CONSTANT = "∞";
    private final int SCALE_FACTOR = 10;
    private boolean mAuto;
    private ImageView mAutoIndicator;
    private LinearLayout mAutoLayout;
    private RotateLayout mAutoRotate;
    private String mCurWBState;
    private ArrayList<String> mExposureTimeDouble;
    private ArrayList<String> mExposureTimeTitle;
    private final Handler mHandler = new MainHandler();
    private ValueAnimator mHideSeekbarAnimator;
    private ArrayList<Integer> mISOValues;
    private int mIndex;
    private LayoutInflater mInflater;
    private RotateLayout mItemRotate;
    private ManualStateChangeListener mListener;
    private int mManualSettingType;
    private int mMax;
    private int mMaxFocusPos;
    private int mMin;
    private int mMinFocusPos;
    private int mProgress;
    private LinearLayout mProgressView;
    private LinearLayout mRoot;
    private SeekBar mSeekBar;
    private CustomSeekBar mSeekBarLayout;
    private ImageView mSettingName;
    private TextView mSettingValue;
    private ValueAnimator mShowSeekbarAnmator;
    private ArrayList<String> mWBTitles;
    private ArrayList<String> mWBValues;

    private class MainHandler extends Handler {
        public MainHandler() {
            super(Looper.getMainLooper());
        }

        public void handleMessage(Message message) {
            if (message.what == 0) {
                ManualItem.this.resetView();
            } else if (message.what == 1) {
                ManualItem.this.mListener;
            }
        }
    }

    public interface ManualStateChangeListener {
        void onManualSettingChanged(int i, int i2, int i3, boolean z);

        void onVisibilityChanged(int i);
    }

    public ManualItem(Context context) {
        super(context);
        init(context);
    }

    public ManualItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ManualItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public void setOrientation(int degree, boolean animation) {
        this.mAutoRotate.setRotation((float) (-degree));
        this.mItemRotate.setRotation((float) (-degree));
    }

    public void onEnableStateChanged(boolean enable) {
        if (enable && this.mAuto) {
            this.mAuto = false;
            updateUI();
            ManualStateChangeListener manualStateChangeListener = this.mListener;
        }
    }

    private void init(Context context) {
        this.mInflater = LayoutInflater.from(context);
        View v = this.mInflater.inflate(R.layout.manual_item, this);
        this.mSettingName = (ImageView) v.findViewById(R.id.item_title);
        this.mSettingValue = (TextView) v.findViewById(R.id.description_item);
        this.mRoot = (LinearLayout) v.findViewById(R.id.item_root);
        this.mAutoLayout = (LinearLayout) v.findViewById(R.id.auto_layout);
        this.mAutoIndicator = (ImageView) v.findViewById(R.id.auto);
        this.mAutoRotate = (RotateLayout) v.findViewById(R.id.rotateauto);
        this.mItemRotate = (RotateLayout) v.findViewById(R.id.manul_mode_rotatelayout);
        this.mSeekBar = (SeekBar) v.findViewById(R.id.customseekbar);
        this.mSeekBarLayout = (CustomSeekBar) v.findViewById(R.id.seekbar);
        this.mSeekBarLayout.setEnableOnTouch(true, this);
        this.mProgressView = (LinearLayout) v.findViewById(R.id.manual_progress_view);
        this.mRoot.setOnClickListener(this);
        this.mAutoLayout.setOnClickListener(this);
    }

    public void initType(ManualStateChangeListener l, ArrayList<Integer> values, String curState) {
        this.mISOValues = new ArrayList();
        this.mISOValues.addAll(values);
        initType(l, 2, curState);
    }

    public void initType(ManualStateChangeListener l, int settingType, ArrayList<String> values, ArrayList<String> titleList, String curState) {
        if (settingType == 3) {
            this.mExposureTimeDouble = new ArrayList();
            this.mExposureTimeTitle = new ArrayList();
            this.mExposureTimeDouble.addAll(values);
            this.mExposureTimeTitle.addAll(titleList);
        } else if (settingType == 0) {
            this.mWBTitles = new ArrayList();
            this.mWBValues = new ArrayList();
            this.mWBValues.addAll(values);
            this.mWBTitles.addAll(titleList);
        }
        initType(l, settingType, curState);
    }

    public void initType(ManualStateChangeListener l, int min, int max, String curState) {
        this.mMinFocusPos = min;
        this.mMaxFocusPos = max;
        initType(l, 1, curState);
    }

    public void initType(ManualStateChangeListener l, int settingType, String curState) {
        this.mManualSettingType = settingType;
        this.mProgress = 0;
        this.mAuto = true;
        this.mIndex = 0;
        if (curState != null) {
            try {
                JSONObject job = new JSONObject(curState);
                this.mAuto = ((Boolean) job.get("auto")).booleanValue();
                this.mProgress = ((Integer) job.get("progress")).intValue();
                this.mIndex = ((Integer) job.get(ManualUI.SETTING_INDEX)).intValue();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("initType ");
        stringBuilder.append(this.mManualSettingType);
        stringBuilder.append(", ");
        stringBuilder.append(this.mAuto);
        stringBuilder.append(", ");
        stringBuilder.append(this.mIndex);
        stringBuilder.append(", ");
        stringBuilder.append(this.mProgress);
        stringBuilder.append(", ");
        stringBuilder.append(curState);
        Log.d(str, stringBuilder.toString());
        switch (this.mManualSettingType) {
            case 0:
                this.mSettingName.setImageResource(R.drawable.ic_manual_setting_wb);
                this.mSettingName.setImageLevel(0);
                this.mMin = 0;
                this.mMax = (this.mWBValues.size() - 1) * 10;
                break;
            case 1:
                this.mSettingName.setImageResource(R.drawable.ic_manual_setting_focus);
                this.mSettingName.setImageLevel(0);
                this.mMin = this.mMinFocusPos + 1;
                this.mMax = this.mMaxFocusPos;
                break;
            case 2:
                this.mSettingName.setImageResource(R.drawable.icon_pro_iso);
                this.mSettingName.setImageLevel(0);
                this.mMin = 0;
                this.mMax = (this.mISOValues.size() - 1) * 10;
                break;
            case 3:
                this.mSettingName.setImageResource(R.drawable.ic_manual_setting_shutter);
                this.mSettingName.setImageLevel(0);
                this.mMin = 0;
                this.mMax = (this.mExposureTimeDouble.size() - 1) * 10;
                break;
        }
        this.mSeekBar.setMax(this.mMax - this.mMin);
        this.mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar seekBar) {
                ManualItem.this.mHandler.removeMessages(0);
                ManualItem.this.mHandler.sendEmptyMessageDelayed(0, 3000);
                ManualItem.this.mHandler.removeMessages(1);
                ManualItem.this.mHandler.sendEmptyMessage(1);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                ManualItem.this.mHandler.removeMessages(0);
                ManualItem.this.cancelAnimateHide();
            }

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ManualItem.this.mProgress = progress;
                ManualItem.this.setSettingValue();
                ManualItem.this.mHandler.removeMessages(1);
                ManualItem.this.mHandler.sendEmptyMessageDelayed(1, 100);
            }
        });
        this.mSeekBar.setProgress(this.mProgress);
        this.mListener = l;
        updateUI();
    }

    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.item_root) {
            if (this.mProgressView.getVisibility() == 0) {
                this.mSettingName.setImageLevel(0);
                this.mSettingValue.setTextColor(getResources().getColor(R.color.manual_item_normal));
                animateHide();
                this.mHandler.removeMessages(0);
            } else {
                this.mSettingName.setImageLevel(1);
                this.mSettingValue.setTextColor(getResources().getColor(R.color.manual_item_selected));
                animateShow();
                this.mHandler.removeMessages(0);
                this.mHandler.sendEmptyMessageDelayed(0, 3000);
            }
            if (this.mListener != null) {
                this.mListener.onVisibilityChanged(this.mManualSettingType);
            }
        } else if (id == R.id.auto_layout) {
            this.mHandler.removeMessages(0);
            this.mHandler.sendEmptyMessageDelayed(0, 3000);
            this.mAuto ^= 1;
            updateUI();
            ManualStateChangeListener manualStateChangeListener = this.mListener;
        }
    }

    private void setSettingValue() {
        String settingValue = null;
        this.mIndex = this.mProgress / 10;
        StringBuilder stringBuilder;
        switch (this.mManualSettingType) {
            case 0:
                if (this.mWBTitles.size() > 0) {
                    settingValue = (String) this.mWBTitles.get(this.mIndex);
                    break;
                }
                break;
            case 1:
                this.mIndex = this.mProgress + this.mMin;
                if (this.mProgress != this.mMax - this.mMin) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(this.mProgress + this.mMin);
                    stringBuilder.append("");
                    settingValue = stringBuilder.toString();
                    break;
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append(this.mMax);
                stringBuilder.append("");
                settingValue = stringBuilder.toString();
                break;
            case 2:
                if (this.mISOValues.size() > 0) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(this.mISOValues.get(this.mIndex));
                    stringBuilder.append("");
                    settingValue = stringBuilder.toString();
                    break;
                }
                break;
            case 3:
                if (this.mExposureTimeTitle.size() > 0) {
                    settingValue = (String) this.mExposureTimeTitle.get(this.mIndex);
                    break;
                }
                break;
        }
        if (settingValue != null) {
            this.mSettingValue.setText(settingValue);
        }
    }

    private void updateUI() {
        this.mSeekBar.setEnabled(this.mAuto ^ 1);
        if (this.mAuto) {
            this.mAutoIndicator.setImageResource(R.drawable.ic_manual_auto_normal);
            this.mSettingValue.setText(R.string.pref_camera_iso_s_f_entry_auto);
            return;
        }
        this.mAutoIndicator.setImageResource(R.drawable.ic_manual_auto_active);
        setSettingValue();
    }

    private void animateHide() {
        if (this.mProgressView.getVisibility() != 4) {
            if (this.mHideSeekbarAnimator == null || this.mShowSeekbarAnmator == null) {
                this.mShowSeekbarAnmator = AnimationManager.buildShowingAnimator(this.mProgressView);
                this.mHideSeekbarAnimator = AnimationManager.buildHidingAnimator(this.mProgressView);
            }
            if (this.mShowSeekbarAnmator.isRunning()) {
                this.mShowSeekbarAnmator.cancel();
            }
            if (!this.mHideSeekbarAnimator.isRunning()) {
                this.mHideSeekbarAnimator.start();
            }
        }
    }

    private void cancelAnimateHide() {
        if (this.mHideSeekbarAnimator != null && this.mHideSeekbarAnimator.isRunning()) {
            this.mHideSeekbarAnimator.cancel();
            this.mProgressView.setAlpha(1.0f);
            if (this.mListener != null) {
                this.mListener.onVisibilityChanged(this.mManualSettingType);
            }
        }
    }

    private void animateShow() {
        if (this.mProgressView.getVisibility() != 0) {
            if (this.mHideSeekbarAnimator == null || this.mShowSeekbarAnmator == null) {
                this.mShowSeekbarAnmator = AnimationManager.buildShowingAnimator(this.mProgressView);
                this.mHideSeekbarAnimator = AnimationManager.buildHidingAnimator(this.mProgressView);
            }
            if (this.mHideSeekbarAnimator.isRunning()) {
                this.mHideSeekbarAnimator.cancel();
            }
            if (!this.mShowSeekbarAnmator.isRunning()) {
                this.mShowSeekbarAnmator.start();
            }
        }
    }

    public void resetView() {
        this.mHandler.removeMessages(0);
        animateHide();
        this.mSettingName.setImageLevel(0);
        this.mSettingValue.setTextColor(getResources().getColor(R.color.manual_item_normal));
    }
}
