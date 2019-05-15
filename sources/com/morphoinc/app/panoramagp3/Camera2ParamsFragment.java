package com.morphoinc.app.panoramagp3;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.media.MediaPlayer2;
import android.support.v7.widget.helper.ItemTouchHelper.Callback;
import android.util.Range;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import com.hmdglobal.app.camera.HelpTip;
import com.hmdglobal.app.camera.R;
import com.morphoinc.app.LogFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

public class Camera2ParamsFragment extends Fragment {
    public static final int AUTO = 0;
    private static final FpsParam[] FPS_PARAM_TABLE = new FpsParam[]{new FpsParam(3.0f, Long.valueOf(ONE_SECONDS_IN_NANOSECONDS.longValue() / 3)), new FpsParam(15.0f, Long.valueOf(ONE_SECONDS_IN_NANOSECONDS.longValue() / 15)), new FpsParam(23.98f, Long.valueOf(41708333)), new FpsParam(24.0f, Long.valueOf(ONE_SECONDS_IN_NANOSECONDS.longValue() / 24)), new FpsParam(29.97f, Long.valueOf(33366666)), new FpsParam(30.0f, Long.valueOf(ONE_SECONDS_IN_NANOSECONDS.longValue() / 30)), new FpsParam(50.0f, Long.valueOf(ONE_SECONDS_IN_NANOSECONDS.longValue() / 50)), new FpsParam(59.94f, Long.valueOf(16683333)), new FpsParam(60.0f, Long.valueOf(ONE_SECONDS_IN_NANOSECONDS.longValue() / 60))};
    private static final int[] ISO_VALUE_TABLE = new int[]{50, 100, HelpTip.HELP_TIP_SHOW_DELAY, 200, Callback.DEFAULT_SWIPE_ANIMATION_DURATION, 300, 350, 400, 450, 500, 600, MediaPlayer2.MEDIA_INFO_VIDEO_TRACK_LAGGING, 800, 1000, CameraConstants.MAX_ISO_VALUE};
    private static final String KEY_AEC = "key_aec";
    private static final String KEY_AUTO = "key_auto";
    private static final String KEY_FPS = "key_fps";
    private static final String KEY_FPS_UPPER = "key_fps_upper";
    private static final String KEY_ISO = "key_iso";
    private static final String KEY_SHUTTER_SPEED = "key_shutter_speed";
    private static final String KEY_TV = "key_tv";
    private static final String KEY_TV_SIMPLE = "key_tv_simple";
    private static final String LOG_TAG = "Camera2App";
    public static final int MANUAL = 2;
    public static final Long ONE_SECONDS_IN_NANOSECONDS = Long.valueOf(1000000000);
    private static final int[] SHUTTER_SPEED_TABLE = new int[]{8000, 4000, 2000, 1000, 500, Callback.DEFAULT_SWIPE_ANIMATION_DURATION, 120, 100, 60, 50, 30, 25};
    public static final double TARGET_EV = 0.0d;
    public static final int TV = 1;
    public static final int TV_SIMPLE = 3;
    private SeekBar mAecSeekBar;
    private double mAecStepValue;
    private final ArrayList<Integer> mAecValues = new ArrayList();
    private final ArrayList<FpsParam> mAutoModeFpsParams = new ArrayList();
    private SeekBar mAutoModeFpsSeekBar;
    private RadioButton mAutoRadio;
    private ICamera2ParamsFragmentEvent mEventHandler = new ICamera2ParamsFragmentEvent() {
        public void onParamChanged() {
        }
    };
    private final ArrayList<FpsParam> mFpsParams = new ArrayList();
    private SeekBar mFpsSeekBar;
    private final OnSeekBarChangeListener mFpsSeekBarChangeListener = new OnSeekBarChangeListener() {
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (seekBar == Camera2ParamsFragment.this.mFpsSeekBar) {
                Camera2ParamsFragment.this.onFpsProgressChanged(seekBar, progress, Camera2ParamsFragment.this.mFpsParams);
            } else if (seekBar == Camera2ParamsFragment.this.mAutoModeFpsSeekBar) {
                Camera2ParamsFragment.this.onFpsProgressChanged(seekBar, progress, Camera2ParamsFragment.this.mAutoModeFpsParams);
            }
        }

        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        public void onStopTrackingTouch(SeekBar seekBar) {
            Camera2ParamsFragment.this.mEventHandler.onParamChanged();
        }
    };
    private final ArrayList<Integer> mISOValues = new ArrayList();
    private SeekBar mIsoSeekBar;
    private RadioButton mManualRadio;
    private final OnCheckedChangeListener mModeRadioChangeListener = new OnCheckedChangeListener() {
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            View view = group.getRootView();
            if (Camera2ParamsFragment.this.auto()) {
                Camera2ParamsFragment.this.mIsoSeekBar.setEnabled(false);
                view.findViewById(R.id.iso_value_text_view).setEnabled(false);
                Camera2ParamsFragment.this.mSsSeekBar.setEnabled(false);
                view.findViewById(R.id.shutter_speed_text_view).setEnabled(false);
                Camera2ParamsFragment.this.mAutoModeFpsSeekBar.setEnabled(true);
                view.findViewById(R.id.fps_text_view).setEnabled(true);
                Camera2ParamsFragment.this.mFpsSeekBar.setVisibility(8);
                Camera2ParamsFragment.this.mAutoModeFpsSeekBar.setVisibility(0);
                Camera2ParamsFragment.this.onFpsProgressChanged(Camera2ParamsFragment.this.mAutoModeFpsSeekBar, -1, Camera2ParamsFragment.this.mAutoModeFpsParams);
                Camera2ParamsFragment.this.mAecSeekBar.setEnabled(true);
                if (Camera2ParamsFragment.this.mSettingFileValues.aec >= 0) {
                    Camera2ParamsFragment.this.mAecSeekBar.setProgress(Camera2ParamsFragment.this.mSettingFileValues.aec);
                }
                view.findViewById(R.id.ae_compensation_value_text_view).setEnabled(true);
                Camera2ParamsFragment.this.mEventHandler.onParamChanged();
            } else if (Camera2ParamsFragment.this.tv()) {
                Camera2ParamsFragment.this.mIsoSeekBar.setEnabled(false);
                view.findViewById(R.id.iso_value_text_view).setEnabled(false);
                Camera2ParamsFragment.this.mSsSeekBar.setEnabled(true);
                view.findViewById(R.id.shutter_speed_text_view).setEnabled(true);
                Camera2ParamsFragment.this.mFpsSeekBar.setEnabled(true);
                view.findViewById(R.id.fps_text_view).setEnabled(true);
                Camera2ParamsFragment.this.mAutoModeFpsSeekBar.setVisibility(8);
                Camera2ParamsFragment.this.mFpsSeekBar.setVisibility(0);
                Camera2ParamsFragment.this.onFpsProgressChanged(Camera2ParamsFragment.this.mFpsSeekBar, -1, Camera2ParamsFragment.this.mFpsParams);
                Camera2ParamsFragment.this.mAecSeekBar.setEnabled(true);
                if (Camera2ParamsFragment.this.mSettingFileValues.aec >= 0) {
                    Camera2ParamsFragment.this.mAecSeekBar.setProgress(Camera2ParamsFragment.this.mSettingFileValues.aec);
                }
                view.findViewById(R.id.ae_compensation_value_text_view).setEnabled(true);
                Camera2ParamsFragment.this.mEventHandler.onParamChanged();
            } else if (Camera2ParamsFragment.this.tvSimple()) {
                Camera2ParamsFragment.this.mIsoSeekBar.setEnabled(false);
                view.findViewById(R.id.iso_value_text_view).setEnabled(false);
                Camera2ParamsFragment.this.mSsSeekBar.setEnabled(false);
                view.findViewById(R.id.shutter_speed_text_view).setEnabled(false);
                Camera2ParamsFragment.this.mFpsSeekBar.setEnabled(true);
                view.findViewById(R.id.fps_text_view).setEnabled(true);
                Camera2ParamsFragment.this.mAutoModeFpsSeekBar.setVisibility(8);
                Camera2ParamsFragment.this.mFpsSeekBar.setVisibility(0);
                Camera2ParamsFragment.this.onFpsProgressChanged(Camera2ParamsFragment.this.mFpsSeekBar, -1, Camera2ParamsFragment.this.mFpsParams);
                Camera2ParamsFragment.this.mAecSeekBar.setEnabled(true);
                if (Camera2ParamsFragment.this.mSettingFileValues.aec >= 0) {
                    Camera2ParamsFragment.this.mAecSeekBar.setProgress(Camera2ParamsFragment.this.mSettingFileValues.aec);
                }
                view.findViewById(R.id.ae_compensation_value_text_view).setEnabled(true);
                Camera2ParamsFragment.this.mEventHandler.onParamChanged();
            } else if (Camera2ParamsFragment.this.manual()) {
                Camera2ParamsFragment.this.mIsoSeekBar.setEnabled(true);
                view.findViewById(R.id.iso_value_text_view).setEnabled(true);
                Camera2ParamsFragment.this.mSsSeekBar.setEnabled(true);
                view.findViewById(R.id.shutter_speed_text_view).setEnabled(true);
                Camera2ParamsFragment.this.mFpsSeekBar.setEnabled(true);
                view.findViewById(R.id.fps_text_view).setEnabled(true);
                Camera2ParamsFragment.this.mAutoModeFpsSeekBar.setVisibility(8);
                Camera2ParamsFragment.this.mFpsSeekBar.setVisibility(0);
                Camera2ParamsFragment.this.onFpsProgressChanged(Camera2ParamsFragment.this.mFpsSeekBar, -1, Camera2ParamsFragment.this.mFpsParams);
                Camera2ParamsFragment.this.mAecSeekBar.setProgress(Camera2ParamsFragment.this.mAecValues.indexOf(Integer.valueOf(0)));
                Camera2ParamsFragment.this.mAecSeekBar.setEnabled(false);
                view.findViewById(R.id.ae_compensation_value_text_view).setEnabled(false);
                Camera2ParamsFragment.this.mEventHandler.onParamChanged();
            } else {
                LogFilter.v(Camera2ParamsFragment.LOG_TAG, "mode radio buttons all off. (onCheckedChanged)");
            }
        }
    };
    private RadioGroup mModeRadioGroup;
    private SaveValues mSettingFileValues = new SaveValues();
    private final LocalValues mSettingLocalValues = new LocalValues();
    private final ArrayList<Integer> mShutterSpeed = new ArrayList();
    private SeekBar mSsSeekBar;
    private RadioButton mTvRadio;
    private RadioButton mTvSimpleRadio;

    public interface ICamera2ParamsFragmentEvent {
        void onParamChanged();
    }

    private static class LocalValues {
        public double calculatedEv;
        public long exposureTime;
        public int sensorSensitivity;

        private LocalValues() {
            this.sensorSensitivity = 0;
            this.exposureTime = 0;
        }

        /* synthetic */ LocalValues(AnonymousClass1 x0) {
            this();
        }
    }

    private static class SaveValues {
        public int aec = -1;
        public boolean auto = false;
        public float fps = 30.0f;
        public int fpsUpper = 30;
        public int iso = Camera2ParamsFragment.ISO_VALUE_TABLE[1];
        public int shutterSpeed = Camera2ParamsFragment.SHUTTER_SPEED_TABLE[Camera2ParamsFragment.SHUTTER_SPEED_TABLE.length - 6];
        public boolean tv = false;
        public boolean tvSimple = false;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LogFilter.i(LOG_TAG, "Camera2ParamsFragment.onCreate");
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        LogFilter.i(LOG_TAG, "Camera2ParamsFragment.onCreateView");
        final View view = inflater.inflate(R.layout.camera2_params_fragment, container);
        this.mAecSeekBar = (SeekBar) view.findViewById(R.id.ae_compensation_seek_bar);
        this.mAecSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (Camera2ParamsFragment.this.mAecValues.size() > 0) {
                    TextView label = (TextView) view.findViewById(R.id.ae_compensation_value_text_view);
                    double value = ((double) Camera2ParamsFragment.this.evSteps()) * Camera2ParamsFragment.this.mAecStepValue;
                    if (value == Camera2ParamsFragment.TARGET_EV) {
                        label.setText(String.format(Locale.US, "%dEV", new Object[]{Integer.valueOf(0)}));
                    } else if (Camera2ParamsFragment.TARGET_EV < value) {
                        label.setText(String.format(Locale.US, "+%.2fEV", new Object[]{Double.valueOf(value)}));
                    } else {
                        label.setText(String.format(Locale.US, "%.2fEV", new Object[]{Double.valueOf(value)}));
                    }
                    if (!Camera2ParamsFragment.this.manual()) {
                        Camera2ParamsFragment.this.mSettingFileValues.aec = progress;
                    }
                }
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                Camera2ParamsFragment.this.mEventHandler.onParamChanged();
            }
        });
        this.mIsoSeekBar = (SeekBar) view.findViewById(R.id.iso_seek_bar);
        this.mIsoSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (Camera2ParamsFragment.this.mISOValues.size() > 0) {
                    ((TextView) view.findViewById(R.id.iso_value_text_view)).setText(String.format(Locale.US, "%d", new Object[]{Camera2ParamsFragment.this.mISOValues.get(progress)}));
                }
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                Camera2ParamsFragment.this.mEventHandler.onParamChanged();
            }
        });
        this.mSsSeekBar = (SeekBar) view.findViewById(R.id.shutter_speed_seek_bar);
        this.mSsSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (Camera2ParamsFragment.this.mShutterSpeed.size() > 0) {
                    ((TextView) view.findViewById(R.id.shutter_speed_text_view)).setText(String.format(Locale.US, "1/%d", new Object[]{Camera2ParamsFragment.this.mShutterSpeed.get(progress)}));
                }
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                Camera2ParamsFragment.this.mEventHandler.onParamChanged();
            }
        });
        this.mFpsSeekBar = (SeekBar) view.findViewById(R.id.fps_seek_bar);
        this.mFpsSeekBar.setOnSeekBarChangeListener(this.mFpsSeekBarChangeListener);
        this.mAutoModeFpsSeekBar = (SeekBar) view.findViewById(R.id.auto_fps_seek_bar);
        this.mAutoModeFpsSeekBar.setOnSeekBarChangeListener(this.mFpsSeekBarChangeListener);
        this.mAutoRadio = (RadioButton) view.findViewById(R.id.camera2_auto_radio);
        this.mTvRadio = (RadioButton) view.findViewById(R.id.camera2_tv_radio);
        this.mTvSimpleRadio = (RadioButton) view.findViewById(R.id.camera2_tv_simple_radio);
        this.mManualRadio = (RadioButton) view.findViewById(R.id.camera2_manual_radio);
        this.mModeRadioGroup = (RadioGroup) view.findViewById(R.id.camera2_mode_group);
        this.mModeRadioGroup.setOnCheckedChangeListener(this.mModeRadioChangeListener);
        return view;
    }

    private void onFpsProgressChanged(SeekBar seekBar, int progress, ArrayList<FpsParam> params) {
        if (params.size() > 0) {
            TextView label = (TextView) seekBar.getRootView().findViewById(R.id.fps_text_view);
            if (progress < 0) {
                label.setText(((FpsParam) params.get(seekBar.getProgress())).getFpsString());
            } else {
                label.setText(((FpsParam) params.get(progress)).getFpsString());
            }
        }
    }

    public void setEventHandler(ICamera2ParamsFragmentEvent handler) {
        this.mEventHandler = handler;
    }

    public int sensorSensitivity() {
        if (tvAll()) {
            return this.mSettingLocalValues.sensorSensitivity;
        }
        return ((Integer) this.mISOValues.get(this.mIsoSeekBar.getProgress())).intValue();
    }

    public Long exposureTime() {
        if (tvAll()) {
            return Long.valueOf(this.mSettingLocalValues.exposureTime);
        }
        return Long.valueOf(ONE_SECONDS_IN_NANOSECONDS.longValue() / ((long) shutterSpeed()));
    }

    public double calculatedEv() {
        if (tvAll()) {
            return this.mSettingLocalValues.calculatedEv;
        }
        return Double.MAX_VALUE;
    }

    public Long exposureTimeMax() {
        return Long.valueOf(ONE_SECONDS_IN_NANOSECONDS.longValue() / ((long) ((Integer) this.mShutterSpeed.get(this.mSsSeekBar.getMax())).intValue()));
    }

    public int shutterSpeed() {
        return ((Integer) this.mShutterSpeed.get(this.mSsSeekBar.getProgress())).intValue();
    }

    public long shutterSpeedInNanoSeconds() {
        return ONE_SECONDS_IN_NANOSECONDS.longValue() / ((long) shutterSpeed());
    }

    public Long frameDuration() {
        if (auto()) {
            return ((FpsParam) this.mAutoModeFpsParams.get(this.mAutoModeFpsSeekBar.getProgress())).FrameDuration;
        }
        return ((FpsParam) this.mFpsParams.get(this.mFpsSeekBar.getProgress())).FrameDuration;
    }

    public float fps() {
        if (auto()) {
            return ((FpsParam) this.mAutoModeFpsParams.get(this.mAutoModeFpsSeekBar.getProgress())).Fps;
        }
        return ((FpsParam) this.mFpsParams.get(this.mFpsSeekBar.getProgress())).Fps;
    }

    public int evSteps() {
        return ((Integer) this.mAecValues.get(this.mAecSeekBar.getProgress())).intValue();
    }

    public boolean auto() {
        return this.mAutoRadio.isChecked();
    }

    public void setAuto() {
        this.mAutoRadio.setChecked(true);
    }

    public boolean tv() {
        return this.mTvRadio.isChecked();
    }

    public void setTv() {
        this.mTvRadio.setChecked(true);
    }

    public boolean tvSimple() {
        return this.mTvSimpleRadio.isChecked();
    }

    public void setTvSimple() {
        this.mTvSimpleRadio.setChecked(true);
    }

    public boolean tvAll() {
        return this.mTvRadio.isChecked() || this.mTvSimpleRadio.isChecked();
    }

    private boolean manual() {
        return this.mManualRadio.isChecked();
    }

    public void setManual() {
        this.mManualRadio.setChecked(true);
    }

    public int getSelectedMode() {
        if (auto()) {
            return 0;
        }
        if (tv()) {
            return 1;
        }
        if (tvSimple()) {
            return 3;
        }
        if (manual()) {
            return 2;
        }
        return 0;
    }

    public void loadValues(SharedPreferences sp) {
        this.mSettingFileValues.auto = sp.getBoolean(KEY_AUTO, this.mSettingFileValues.auto);
        this.mSettingFileValues.tv = sp.getBoolean(KEY_TV, this.mSettingFileValues.tv);
        this.mSettingFileValues.tvSimple = sp.getBoolean(KEY_TV_SIMPLE, this.mSettingFileValues.tvSimple);
        if (!sp.getBoolean(getString(R.string.KEY_USE_CAMERA2), true)) {
            this.mSettingFileValues.auto = true;
            this.mSettingFileValues.tv = false;
            this.mSettingFileValues.tvSimple = false;
        }
        this.mSettingFileValues.shutterSpeed = sp.getInt(KEY_SHUTTER_SPEED, this.mSettingFileValues.shutterSpeed);
        this.mSettingFileValues.iso = sp.getInt(KEY_ISO, this.mSettingFileValues.iso);
        this.mSettingFileValues.fps = sp.getFloat(KEY_FPS, this.mSettingFileValues.fps);
        this.mSettingFileValues.fpsUpper = sp.getInt(KEY_FPS_UPPER, this.mSettingFileValues.fpsUpper);
        this.mSettingFileValues.aec = sp.getInt(KEY_AEC, this.mSettingFileValues.aec);
    }

    public void saveValues(SharedPreferences sp) {
        if (!disabledSave()) {
            Editor editor = sp.edit();
            editor.putBoolean(KEY_AUTO, auto());
            editor.putBoolean(KEY_TV, tv());
            editor.putBoolean(KEY_TV_SIMPLE, tvSimple());
            editor.putInt(KEY_SHUTTER_SPEED, shutterSpeed());
            editor.putInt(KEY_ISO, sensorSensitivity());
            editor.putFloat(KEY_FPS, ((FpsParam) this.mFpsParams.get(this.mFpsSeekBar.getProgress())).Fps);
            editor.putInt(KEY_FPS_UPPER, (int) ((FpsParam) this.mAutoModeFpsParams.get(this.mAutoModeFpsParams.size() - 1)).Fps);
            editor.putInt(KEY_AEC, this.mSettingFileValues.aec);
            editor.apply();
        }
    }

    public void resetValues(SharedPreferences sp) {
        Editor editor = sp.edit();
        editor.remove(KEY_AUTO);
        editor.remove(KEY_TV);
        editor.remove(KEY_TV_SIMPLE);
        editor.remove(KEY_SHUTTER_SPEED);
        editor.remove(KEY_ISO);
        editor.remove(KEY_FPS);
        editor.remove(KEY_FPS_UPPER);
        editor.remove(KEY_AEC);
        editor.apply();
        this.mSettingFileValues = new SaveValues();
        loadValues(sp);
    }

    private boolean disabledSave() {
        return this.mShutterSpeed.isEmpty() || this.mISOValues.isEmpty() || this.mFpsParams.isEmpty();
    }

    public void initializeUI(CameraInfo cameraInfo, boolean use60fps) {
        initAeCompensationSeekBar(cameraInfo);
        initIsoSeekBar(cameraInfo);
        initShutterSpeedSeekBar(cameraInfo);
        initFpsSeekBar(cameraInfo, use60fps);
        this.mAutoRadio.setChecked(this.mSettingFileValues.auto);
        this.mTvRadio.setChecked(this.mSettingFileValues.tv);
        this.mTvSimpleRadio.setChecked(this.mSettingFileValues.tvSimple);
        this.mManualRadio.setChecked(((this.mSettingFileValues.auto | this.mSettingFileValues.tv) | this.mSettingFileValues.tvSimple) == 0);
        this.mModeRadioChangeListener.onCheckedChanged(this.mModeRadioGroup, 0);
    }

    private void initAeCompensationSeekBar(CameraInfo cameraInfo) {
        int i;
        int i2;
        int defaultIndex;
        this.mAecValues.clear();
        this.mAecStepValue = cameraInfo.getAeCompensationStep();
        int i3 = -1;
        while (true) {
            i = 0;
            if (cameraInfo.getAeCompensationMin() > i3) {
                break;
            }
            this.mAecValues.add(0, Integer.valueOf(i3));
            i3--;
        }
        this.mAecValues.add(Integer.valueOf(0));
        for (i2 = 1; i2 <= cameraInfo.getAeCompensationMax(); i2++) {
            this.mAecValues.add(Integer.valueOf(i2));
        }
        if (this.mSettingFileValues.aec == -1) {
            double ae_steps = cameraInfo.getAeCompensationStep();
            double min_diff = 1.0E7d;
            defaultIndex = 0;
            while (true) {
                i2 = i;
                if (i2 >= this.mAecValues.size()) {
                    break;
                }
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("using AE Compensation value=");
                stringBuilder.append(this.mAecValues.get(i2));
                LogFilter.v(str, stringBuilder.toString());
                double diff = TARGET_EV - (((double) ((Integer) this.mAecValues.get(i2)).intValue()) * ae_steps);
                if (diff < TARGET_EV) {
                    diff = -diff;
                }
                if (min_diff > diff) {
                    defaultIndex = i2;
                    min_diff = diff;
                    str = LOG_TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("min_diff=");
                    stringBuilder2.append(min_diff);
                    stringBuilder2.append(" defaultIndex ");
                    stringBuilder2.append(defaultIndex);
                    LogFilter.v(str, stringBuilder2.toString());
                }
                i = i2 + 1;
            }
            this.mSettingFileValues.aec = defaultIndex;
        } else {
            defaultIndex = this.mSettingFileValues.aec;
        }
        if (this.mAecValues.size() <= 0) {
            this.mAecStepValue = TARGET_EV;
            return;
        }
        this.mAecSeekBar.setMax(this.mAecValues.size() - 1);
        this.mAecSeekBar.setProgress(defaultIndex);
    }

    private void initIsoSeekBar(CameraInfo cameraInfo) {
        this.mISOValues.clear();
        int defaultIndex = 0;
        for (int value : ISO_VALUE_TABLE) {
            if (cameraInfo.containsSensitivity(Integer.valueOf(value))) {
                this.mISOValues.add(Integer.valueOf(value));
                if (value == this.mSettingFileValues.iso) {
                    defaultIndex = this.mISOValues.size() - 1;
                }
            }
        }
        if (this.mISOValues.size() > 0) {
            this.mIsoSeekBar.setMax(this.mISOValues.size() - 1);
            this.mIsoSeekBar.setProgress(defaultIndex);
        }
    }

    private void initShutterSpeedSeekBar(CameraInfo cameraInfo) {
        this.mShutterSpeed.clear();
        int defaultIndex = 0;
        for (int value : SHUTTER_SPEED_TABLE) {
            if (cameraInfo.containsExposureTime(Long.valueOf(ONE_SECONDS_IN_NANOSECONDS.longValue() / ((long) value)))) {
                this.mShutterSpeed.add(Integer.valueOf(value));
                if (value == this.mSettingFileValues.shutterSpeed) {
                    defaultIndex = this.mShutterSpeed.size() - 1;
                }
            }
        }
        if (this.mShutterSpeed.size() > 0) {
            this.mSsSeekBar.setMax(this.mShutterSpeed.size() - 1);
            this.mSsSeekBar.setProgress(defaultIndex);
        }
    }

    private void initFpsSeekBar(CameraInfo cameraInfo, boolean use60fps) {
        int i;
        ArrayList<Range<Integer>> fpsRanges = cameraInfo.getTargetFpsRanges();
        ArrayList<Integer> upperFpsList = new ArrayList();
        boolean can60fps = false;
        Iterator it = fpsRanges.iterator();
        while (it.hasNext()) {
            Range<Integer> fpsRange = (Range) it.next();
            if (((Integer) fpsRange.getUpper()).intValue() == 60) {
                can60fps = true;
            }
            if (!((Integer) fpsRange.getLower()).equals(fpsRange.getUpper())) {
                if (!upperFpsList.contains(fpsRange.getUpper())) {
                    upperFpsList.add((Integer) fpsRange.getUpper());
                }
            }
        }
        this.mAutoModeFpsParams.clear();
        boolean add60fps = false;
        Iterator it2 = upperFpsList.iterator();
        while (it2.hasNext()) {
            Integer upperFps = (Integer) it2.next();
            this.mAutoModeFpsParams.add(new FpsParam((float) upperFps.intValue(), Long.valueOf(ONE_SECONDS_IN_NANOSECONDS.longValue() / ((long) upperFps.intValue()))));
            if (upperFps.intValue() == 60) {
                add60fps = true;
            }
        }
        if (use60fps && can60fps && !add60fps) {
            this.mAutoModeFpsParams.add(new FpsParam(60.0f, Long.valueOf(ONE_SECONDS_IN_NANOSECONDS.longValue() / 60)));
        }
        int i2 = 0;
        if (this.mAutoModeFpsParams.size() <= 0) {
            this.mAutoModeFpsSeekBar.setMax(0);
        } else {
            this.mAutoModeFpsSeekBar.setMax(this.mAutoModeFpsParams.size() - 1);
        }
        if (use60fps && can60fps) {
            this.mSettingFileValues.fpsUpper = 60;
        }
        int autoModeDefaultIndex = this.mAutoModeFpsParams.size() - 1;
        for (i = autoModeDefaultIndex; i >= 0; i--) {
            if (((int) ((FpsParam) this.mAutoModeFpsParams.get(i)).Fps) == this.mSettingFileValues.fpsUpper) {
                autoModeDefaultIndex = i;
                break;
            }
        }
        if (autoModeDefaultIndex >= 0) {
            this.mAutoModeFpsSeekBar.setProgress(autoModeDefaultIndex);
        }
        this.mFpsParams.clear();
        for (FpsParam param : FPS_PARAM_TABLE) {
            if (cameraInfo.containsFrameDuration(param.FrameDuration)) {
                this.mFpsParams.add(param);
            }
        }
        CameraInfo cameraInfo2 = cameraInfo;
        if (this.mFpsParams.size() <= 0) {
            this.mFpsSeekBar.setMax(0);
        } else {
            this.mFpsSeekBar.setMax(this.mFpsParams.size() - 1);
        }
        i = 0;
        while (i2 < this.mFpsParams.size()) {
            if (((FpsParam) this.mFpsParams.get(i2)).Fps == this.mSettingFileValues.fps) {
                i = i2;
                break;
            }
            i2++;
        }
        this.mFpsSeekBar.setProgress(i);
    }

    public void setSensorSensitivity(int sensorSensitivity) {
        this.mSettingLocalValues.sensorSensitivity = sensorSensitivity;
    }

    public void setExposureTime(long exposureTime) {
        this.mSettingLocalValues.exposureTime = exposureTime;
    }

    public void setCalculatedEv(double ev) {
        this.mSettingLocalValues.calculatedEv = ev;
    }

    public void setEnabled(boolean enabled) {
        this.mAutoRadio.setEnabled(enabled);
        this.mTvRadio.setEnabled(enabled);
        this.mTvSimpleRadio.setEnabled(enabled);
        this.mManualRadio.setEnabled(enabled);
        View view = getView();
        if (view != null) {
            if (enabled) {
                this.mModeRadioGroup.setEnabled(false);
                if (auto()) {
                    this.mIsoSeekBar.setEnabled(false);
                    view.findViewById(R.id.iso_value_text_view).setEnabled(false);
                    this.mSsSeekBar.setEnabled(false);
                    view.findViewById(R.id.shutter_speed_text_view).setEnabled(false);
                    this.mAutoModeFpsSeekBar.setEnabled(true);
                    view.findViewById(R.id.fps_text_view).setEnabled(true);
                    this.mAecSeekBar.setEnabled(true);
                    view.findViewById(R.id.ae_compensation_value_text_view).setEnabled(true);
                } else if (tv()) {
                    this.mIsoSeekBar.setEnabled(false);
                    view.findViewById(R.id.iso_value_text_view).setEnabled(false);
                    this.mSsSeekBar.setEnabled(true);
                    view.findViewById(R.id.shutter_speed_text_view).setEnabled(true);
                    this.mFpsSeekBar.setEnabled(true);
                    view.findViewById(R.id.fps_text_view).setEnabled(true);
                    this.mAecSeekBar.setEnabled(true);
                    view.findViewById(R.id.ae_compensation_value_text_view).setEnabled(true);
                } else if (tvSimple()) {
                    this.mIsoSeekBar.setEnabled(false);
                    view.findViewById(R.id.iso_value_text_view).setEnabled(false);
                    this.mSsSeekBar.setEnabled(false);
                    view.findViewById(R.id.shutter_speed_text_view).setEnabled(false);
                    this.mFpsSeekBar.setEnabled(true);
                    view.findViewById(R.id.fps_text_view).setEnabled(true);
                    this.mAecSeekBar.setEnabled(true);
                    view.findViewById(R.id.ae_compensation_value_text_view).setEnabled(true);
                } else if (manual()) {
                    this.mIsoSeekBar.setEnabled(true);
                    view.findViewById(R.id.iso_value_text_view).setEnabled(true);
                    this.mSsSeekBar.setEnabled(true);
                    view.findViewById(R.id.shutter_speed_text_view).setEnabled(true);
                    this.mFpsSeekBar.setEnabled(true);
                    view.findViewById(R.id.fps_text_view).setEnabled(true);
                    this.mAecSeekBar.setEnabled(false);
                    view.findViewById(R.id.ae_compensation_value_text_view).setEnabled(false);
                } else {
                    LogFilter.v(LOG_TAG, "mode radio buttons all off. (setEnabled)");
                }
            } else {
                this.mIsoSeekBar.setEnabled(false);
                view.findViewById(R.id.iso_value_text_view).setEnabled(false);
                this.mSsSeekBar.setEnabled(false);
                view.findViewById(R.id.shutter_speed_text_view).setEnabled(false);
                this.mAutoModeFpsSeekBar.setEnabled(false);
                view.findViewById(R.id.fps_text_view).setEnabled(false);
                this.mAecSeekBar.setEnabled(false);
                view.findViewById(R.id.ae_compensation_value_text_view).setEnabled(false);
            }
        }
    }

    public void setDefaultMode(SharedPreferences sp, int mode) {
        if (mode != 3) {
            switch (mode) {
                case 0:
                    this.mSettingFileValues.auto = true;
                    this.mSettingFileValues.tv = false;
                    this.mSettingFileValues.tvSimple = false;
                    break;
                case 1:
                    this.mSettingFileValues.auto = false;
                    this.mSettingFileValues.tv = true;
                    this.mSettingFileValues.tvSimple = false;
                    break;
                default:
                    this.mSettingFileValues.auto = false;
                    this.mSettingFileValues.tv = false;
                    this.mSettingFileValues.tvSimple = false;
                    break;
            }
        }
        this.mSettingFileValues.auto = false;
        this.mSettingFileValues.tv = false;
        this.mSettingFileValues.tvSimple = true;
        loadValues(sp);
    }

    public void setAutoMode30fps() {
        for (int i = 0; i < this.mAutoModeFpsParams.size() - 1; i++) {
            if (((int) ((FpsParam) this.mAutoModeFpsParams.get(i)).Fps) == 30) {
                this.mAutoModeFpsSeekBar.setProgress(i);
                return;
            }
        }
    }
}
