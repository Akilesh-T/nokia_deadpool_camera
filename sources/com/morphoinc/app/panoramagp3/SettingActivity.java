package com.morphoinc.app.panoramagp3;

import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.util.Size;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.app.CameraApp;
import java.util.ArrayList;
import java.util.Locale;

public class SettingActivity extends PreferenceActivity {
    public static final String INTENT_KEY_AOV = "INTENT_KEY_AOV";
    public static final String INTENT_KEY_CAMERA = "INTENT_KEY_CAMERA";
    public static final String INTENT_KEY_CAMERA2_IMAGE_QUALITY_SETTINGS = "INTENT_KEY_CAMERA2_IMAGE_QUALITY_SETTINGS";
    public static final String INTENT_KEY_CAPTURE_MODE = "INTENT_KEY_CAPTURE_MODE";
    public static final String INTENT_KEY_CAPTURE_MODE_VALUE = "INTENT_KEY_CAPTURE_MODE_VALUE";
    public static final String INTENT_KEY_HARDWARE_LEVEL_VALUE = "INTENT_KEY_HARDWARE_LEVEL_VALUE";
    public static final String INTENT_KEY_OIS_AVAILABLE = "INTENT_KEY_OIS_AVAILABLE";
    public static final String INTENT_KEY_SENSOR_ASPECT = "INTENT_KEY_SENSOR_ASPECT";
    public static final String INTENT_KEY_TIMESTAMP_SOURCE_VALUE = "INTENT_KEY_TIMESTAMP_SOURCE_VALUE";
    public static final String INTENT_KEY_USE_OIS = "INTENT_KEY_USE_OIS";

    public static class SettingFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
        private double[] mAovs;
        private String[] mCamera;
        private Camera2ImageQualitySettings[] mCamera2ImageQualitySettings;
        private Size[] mCameraSize;
        private boolean mIsAvailableOis;
        private String mSensorAspect;
        private boolean mUseCamera1;

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.setting);
            CameraApp app = (CameraApp) getActivity().getApplication();
            this.mCameraSize = app.getSupportedPictureSizes();
            Intent intent = getActivity().getIntent();
            this.mAovs = intent.getDoubleArrayExtra(SettingActivity.INTENT_KEY_AOV);
            this.mSensorAspect = intent.getStringExtra(SettingActivity.INTENT_KEY_SENSOR_ASPECT);
            this.mCamera2ImageQualitySettings = (Camera2ImageQualitySettings[]) intent.getSerializableExtra(SettingActivity.INTENT_KEY_CAMERA2_IMAGE_QUALITY_SETTINGS);
            this.mCamera = intent.getStringArrayExtra(SettingActivity.INTENT_KEY_CAMERA);
            boolean useOis = intent.getBooleanExtra(SettingActivity.INTENT_KEY_USE_OIS, false);
            this.mIsAvailableOis = intent.getBooleanExtra(SettingActivity.INTENT_KEY_OIS_AVAILABLE, useOis);
            this.mUseCamera1 = ((CheckBoxPreference) getPreferenceScreen().findPreference(getResources().getString(R.string.KEY_USE_CAMERA2))).isChecked() ^ 1;
            ListPreference list = (ListPreference) findPreference(getResources().getString(R.string.KEY_CAPTURE_SIZE));
            ArrayList<CharSequence> entries = new ArrayList();
            ArrayList<CharSequence> entryValues = new ArrayList();
            Size[] sizeArr = this.mCameraSize;
            int length = sizeArr.length;
            int value = 0;
            int value2 = 0;
            while (value2 < length) {
                Size size = sizeArr[value2];
                r14 = new Object[2];
                CameraApp app2 = app;
                r14[0] = Integer.valueOf(size.getWidth());
                r14[1] = Integer.valueOf(size.getHeight());
                entries.add(String.format(Locale.US, "%dx%d", r14));
                entryValues.add(Integer.toString(value));
                value++;
                value2++;
                app = app2;
            }
            list.setEntries((CharSequence[]) entries.toArray(new CharSequence[entries.size()]));
            list.setEntryValues((CharSequence[]) entryValues.toArray(new CharSequence[entryValues.size()]));
            initializeImageQualitySettingsList(R.string.KEY_COLOR_CORRECTION_MODE, 0);
            initializeImageQualitySettingsList(R.string.KEY_EDGE_MODE, 1);
            initializeImageQualitySettingsList(R.string.KEY_NOISE_REDUCTION_MODE, 2);
            initializeImageQualitySettingsList(R.string.KEY_SHADING_MODE, 3);
            initializeImageQualitySettingsList(R.string.KEY_TONEMAP_MODE, 4);
            ListPreference cameraList = (ListPreference) findPreference(getResources().getString(R.string.KEY_CAMERA_ID));
            String[] cameraListEntryValues = new String[this.mCamera.length];
            for (value2 = 0; value2 < cameraListEntryValues.length; value2++) {
                cameraListEntryValues[value2] = String.valueOf(value2);
            }
            cameraList.setEntries(this.mCamera);
            cameraList.setEntryValues(cameraListEntryValues);
            ListPreference captureModeList = (ListPreference) findPreference(getResources().getString(R.string.KEY_CAPTURE_MODE));
            String[] captureModeListEntry = intent.getStringArrayExtra(SettingActivity.INTENT_KEY_CAPTURE_MODE);
            int[] captureModeListEntryValues = intent.getIntArrayExtra(SettingActivity.INTENT_KEY_CAPTURE_MODE_VALUE);
            String hardware_level = intent.getStringExtra(SettingActivity.INTENT_KEY_HARDWARE_LEVEL_VALUE);
            String timestamp_source = intent.getStringExtra(SettingActivity.INTENT_KEY_TIMESTAMP_SOURCE_VALUE);
            ArrayList<CharSequence> captureModeEntryValues = new ArrayList();
            int i = 0;
            while (true) {
                ListPreference cameraList2 = cameraList;
                Intent intent2 = intent;
                int i2 = i;
                if (i2 < captureModeListEntryValues.length) {
                    captureModeEntryValues.add(Integer.toString(captureModeListEntryValues[i2]));
                    i = i2 + 1;
                    cameraList = cameraList2;
                    intent = intent2;
                } else {
                    captureModeList.setEntries(captureModeListEntry);
                    captureModeList.setEntryValues((CharSequence[]) captureModeEntryValues.toArray(new CharSequence[captureModeEntryValues.size()]));
                    setCameraSizeSummary(list);
                    setAntiBandingSummary();
                    setAntiFlickerFreqSummary();
                    setCaptureModeSummary();
                    setShrinkRatio(list);
                    setShrinkRatioSummary();
                    setCalcseamPixnumSummary();
                    setAovxSummary();
                    setAovySummary();
                    setSeamsearchRatioSummary();
                    setZrotationCoeffSummary();
                    setDrawThresholdSummary();
                    setSensorModeSummary();
                    setSensorUseModeSummary();
                    setUseRoundAutoEndSummary();
                    setUnsharpStrength();
                    setNoiseReductionParam();
                    updateOisEnabled();
                    setSaveInputImagesSummary();
                    setInputMovieFpsSummary();
                    checkSaveInputImages();
                    updateCaptureModeEnabled();
                    setColorCorrectionModeSummary();
                    setEdgeModeSummary();
                    setNoiseReductionModeSummary();
                    setShadingModeSummary();
                    setTonemapModeSummary();
                    setAovGainSummary();
                    setDistortionK1Summary();
                    setDistortionK2Summary();
                    setDistortionK3Summary();
                    setDistortionK4Summary();
                    setRotationRatioSummary();
                    setUiControlModeSummary();
                    setFocusModeSummary();
                    setProjectionModeSummary();
                    setMotionDetectionModeSummary();
                    setCameraSummary();
                    automaticallyChanged();
                    getHarwareLevelSummary(hardware_level);
                    getTimestampSource(timestamp_source);
                    Preference about_preference = findPreference(getResources().getString(R.string.KEY_ABOUT));
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(getResources().getString(R.string.panoramagp3_ABOUT_MESSAGE));
                    stringBuilder.append(MorphoPanoramaGP3.getVersion());
                    about_preference.setSummary(stringBuilder.toString());
                    return;
                }
            }
        }

        public void onResume() {
            super.onResume();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        }

        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(getResources().getString(R.string.KEY_CAPTURE_SIZE))) {
                ListPreference list = (ListPreference) getPreferenceScreen().findPreference(getResources().getString(R.string.KEY_CAPTURE_SIZE));
                setCameraSizeSummary(list);
                setShrinkRatio(list);
            } else if (key.equals(getResources().getString(R.string.KEY_ANTI_BANDING))) {
                setAntiBandingSummary();
            } else if (key.equals(getResources().getString(R.string.KEY_ANTI_FLICKER_FREQ))) {
                setAntiFlickerFreqSummary();
            } else if (key.equals(getResources().getString(R.string.KEY_CAPTURE_MODE))) {
                setCaptureModeSummary();
                captureModeChanged();
            } else if (key.equals(getResources().getString(R.string.KEY_AOVX))) {
                setAovxSummary();
            } else if (key.equals(getResources().getString(R.string.KEY_AOVY))) {
                setAovySummary();
            } else if (key.equals(getResources().getString(R.string.KEY_CALCSEAM_PIXNUM))) {
                setCalcseamPixnumSummary();
            } else if (key.equals(getResources().getString(R.string.KEY_SHRINK_RATIO))) {
                setShrinkRatioSummary();
            } else if (key.equals(getResources().getString(R.string.KEY_GPS))) {
                checkGpsSetting();
            } else if (key.equals(getResources().getString(R.string.KEY_SEAMSEARCH_RATIO))) {
                setSeamsearchRatioSummary();
            } else if (key.equals(getResources().getString(R.string.KEY_ZROTATION_COEFF))) {
                setZrotationCoeffSummary();
            } else if (key.equals(getResources().getString(R.string.KEY_DRAW_THRESHOLD))) {
                setDrawThresholdSummary();
            } else if (key.equals(getResources().getString(R.string.KEY_SENSOR_MODE))) {
                setSensorModeSummary();
            } else if (key.equals(getResources().getString(R.string.KEY_SENSOR_USE_MODE))) {
                setSensorUseModeSummary();
            } else if (key.equals(getResources().getString(R.string.KEY_USE_ROUND_AUTO_END))) {
                setUseRoundAutoEndSummary();
            } else if (key.equals(getResources().getString(R.string.KEY_UNSHARP_STRENGTH))) {
                setUnsharpStrength();
            } else if (key.equals(getResources().getString(R.string.KEY_NR_AUTO))) {
                automaticallyChanged();
            } else if (key.equals(getResources().getString(R.string.KEY_NR_STRENGTH))) {
                setNoiseReductionParam();
            } else if (key.equals(getResources().getString(R.string.KEY_INPUT_MOVIE_FPS))) {
                setInputMovieFpsSummary();
            } else if (key.equals(getResources().getString(R.string.KEY_SAVE_INPUT_IMAGES))) {
                setSaveInputImagesSummary();
                checkSaveInputImages();
            } else if (key.equals(getResources().getString(R.string.KEY_COLOR_CORRECTION_MODE))) {
                setColorCorrectionModeSummary();
            } else if (key.equals(getResources().getString(R.string.KEY_EDGE_MODE))) {
                setEdgeModeSummary();
            } else if (key.equals(getResources().getString(R.string.KEY_NOISE_REDUCTION_MODE))) {
                setNoiseReductionModeSummary();
            } else if (key.equals(getResources().getString(R.string.KEY_SHADING_MODE))) {
                setShadingModeSummary();
            } else if (key.equals(getResources().getString(R.string.KEY_TONEMAP_MODE))) {
                setTonemapModeSummary();
            } else if (key.equals(getResources().getString(R.string.KEY_AOV_GAIN))) {
                setAovGainSummary();
            } else if (key.equals(getResources().getString(R.string.KEY_DISTORTION_K1))) {
                setDistortionK1Summary();
            } else if (key.equals(getResources().getString(R.string.KEY_DISTORTION_K2))) {
                setDistortionK2Summary();
            } else if (key.equals(getResources().getString(R.string.KEY_DISTORTION_K3))) {
                setDistortionK3Summary();
            } else if (key.equals(getResources().getString(R.string.KEY_DISTORTION_K4))) {
                setDistortionK4Summary();
            } else if (key.equals(getResources().getString(R.string.KEY_ROTATION_RATIO))) {
                setRotationRatioSummary();
            } else if (key.equals(getResources().getString(R.string.KEY_UI_CONTROL_MODE))) {
                setUiControlModeSummary();
            } else if (key.equals(getResources().getString(R.string.KEY_FOCUS_MODE))) {
                setFocusModeSummary();
            } else if (key.equals(getResources().getString(R.string.KEY_PROJECTION_MODE))) {
                setProjectionModeSummary();
            } else if (key.equals(getResources().getString(R.string.KEY_MOTION_DETECTION_MODE))) {
                setMotionDetectionModeSummary();
            } else if (key.equals(getResources().getString(R.string.KEY_USE_CAMERA2))) {
                useCamera2Changed();
            } else if (key.equals(getResources().getString(R.string.KEY_CAMERA_ID))) {
                cameraChanged();
            }
        }

        private void setCameraSizeSummary(ListPreference listPreference) {
            int valueIndex = Integer.parseInt(listPreference.getValue());
            listPreference.setSummary(String.format(Locale.US, "%dx%d", new Object[]{Integer.valueOf(this.mCameraSize[valueIndex].getWidth()), Integer.valueOf(this.mCameraSize[valueIndex].getHeight())}));
        }

        private void setAntiBandingSummary() {
            ListPreference list = (ListPreference) findPreference(getResources().getString(R.string.KEY_ANTI_BANDING));
            list.setSummary(list.getEntry());
        }

        private void setAntiFlickerFreqSummary() {
            ListPreference list = (ListPreference) findPreference(getResources().getString(R.string.KEY_ANTI_FLICKER_FREQ));
            list.setSummary(list.getEntry());
        }

        private void setCaptureModeSummary() {
            ListPreference list = (ListPreference) findPreference(getResources().getString(R.string.KEY_CAPTURE_MODE));
            list.setSummary(list.getEntry());
        }

        private void setAovxSummary() {
            AovxPreference pref = (AovxPreference) findPreference(getResources().getString(R.string.KEY_AOVX));
            pref.setDefValue(String.valueOf(this.mAovs[0]));
            pref.setSummary();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(getResources().getString(R.string.setting_AOVX));
            stringBuilder.append("(");
            stringBuilder.append(this.mSensorAspect);
            stringBuilder.append(" Sensor)");
            pref.setTitle(stringBuilder.toString());
        }

        private void setAovySummary() {
            AovyPreference pref = (AovyPreference) findPreference(getResources().getString(R.string.KEY_AOVY));
            pref.setDefValue(String.valueOf(this.mAovs[1]));
            pref.setSummary();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(getResources().getString(R.string.setting_AOVY));
            stringBuilder.append("(");
            stringBuilder.append(this.mSensorAspect);
            stringBuilder.append(" Sensor)");
            pref.setTitle(stringBuilder.toString());
        }

        private void setCalcseamPixnumSummary() {
            ((CalcseamPixnumPreference) findPreference(getResources().getString(R.string.KEY_CALCSEAM_PIXNUM))).setSummary();
        }

        private void setShrinkRatio(ListPreference listPreference) {
            int i = Integer.parseInt(listPreference.getValue());
            int width = this.mCameraSize[i].getWidth();
            int height = this.mCameraSize[i].getHeight();
            double ratio = ShrinkRatioPreference.ShrinkRatioCalculation(width, height);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" ratio = ");
            stringBuilder.append(ratio);
            stringBuilder.append(" width ");
            stringBuilder.append(width);
            stringBuilder.append(" height ");
            stringBuilder.append(height);
            Log.d("shrink_ratio", stringBuilder.toString());
            ((ShrinkRatioPreference) findPreference(getResources().getString(R.string.KEY_SHRINK_RATIO))).setValue(String.format("%.5f", new Object[]{Double.valueOf(ratio)}));
        }

        private void setShrinkRatioSummary() {
            ((ShrinkRatioPreference) findPreference(getResources().getString(R.string.KEY_SHRINK_RATIO))).setSummary();
        }

        private void setSeamsearchRatioSummary() {
            ((DoublePreference) findPreference(getResources().getString(R.string.KEY_SEAMSEARCH_RATIO))).setSummary();
        }

        private void setZrotationCoeffSummary() {
            ((DoublePreference) findPreference(getResources().getString(R.string.KEY_ZROTATION_COEFF))).setSummary();
        }

        private void setDrawThresholdSummary() {
            ((DoublePreference) findPreference(getResources().getString(R.string.KEY_DRAW_THRESHOLD))).setSummary();
        }

        private void setSensorModeSummary() {
            ListPreference list = (ListPreference) findPreference(getResources().getString(R.string.KEY_SENSOR_MODE));
            list.setSummary(list.getEntry());
        }

        private void setSensorUseModeSummary() {
            ListPreference list = (ListPreference) findPreference(getResources().getString(R.string.KEY_SENSOR_USE_MODE));
            list.setSummary(list.getEntry());
        }

        private void setUseRoundAutoEndSummary() {
            ListPreference list = (ListPreference) findPreference(getResources().getString(R.string.KEY_USE_ROUND_AUTO_END));
            list.setSummary(list.getEntry());
        }

        private void setUnsharpStrength() {
            ((UnsharpStrengthPreference) findPreference(getResources().getString(R.string.KEY_UNSHARP_STRENGTH))).setSummary();
        }

        private void automaticallyChanged() {
            NoiseReductionParamPreference nrParamPref = (NoiseReductionParamPreference) findPreference(getResources().getString(R.string.KEY_NR_STRENGTH));
            UnsharpStrengthPreference unsharpPref = (UnsharpStrengthPreference) findPreference(getResources().getString(R.string.KEY_UNSHARP_STRENGTH));
            ListPreference edgePref = (ListPreference) findPreference(getResources().getString(R.string.KEY_EDGE_MODE));
            ListPreference nrModePref = (ListPreference) findPreference(getResources().getString(R.string.KEY_NOISE_REDUCTION_MODE));
            boolean isAuto = ((CheckBoxPreference) getPreferenceScreen().findPreference(getResources().getString(R.string.KEY_NR_AUTO))).isChecked();
            if (isAuto) {
                String auto = getResources().getString(R.string.panoramagp3_BTN_LBL_AUTO);
                nrParamPref.setSummary(auto);
                unsharpPref.setSummary(auto);
                edgePref.setSummary(auto);
                nrModePref.setSummary(auto);
            } else {
                nrParamPref.setSummary();
                unsharpPref.setSummary();
                edgePref.setSummary(edgePref.getEntry());
                nrModePref.setSummary(nrModePref.getEntry());
            }
            nrParamPref.setEnabled(isAuto ^ 1);
            unsharpPref.setEnabled(isAuto ^ 1);
            edgePref.setEnabled(isAuto ^ 1);
            nrModePref.setEnabled(isAuto ^ 1);
        }

        private void setNoiseReductionParam() {
            ((NoiseReductionParamPreference) findPreference(getResources().getString(R.string.KEY_NR_STRENGTH))).setSummary();
        }

        private void updateOisEnabled() {
            ((CheckBoxPreference) getPreferenceScreen().findPreference(getResources().getString(R.string.KEY_OIS))).setEnabled(this.mIsAvailableOis);
        }

        private void checkGpsSetting() {
            if (((CheckBoxPreference) getPreferenceScreen().findPreference(getResources().getString(R.string.KEY_GPS))).isChecked()) {
                LocationManager locationManager = (LocationManager) getActivity().getSystemService("location");
                if (!locationManager.isProviderEnabled("gps") && !locationManager.isProviderEnabled("network")) {
                    startActivity(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"));
                }
            }
        }

        private void setSaveInputImagesSummary() {
            ListPreference list = (ListPreference) getPreferenceScreen().findPreference(getResources().getString(R.string.KEY_SAVE_INPUT_IMAGES));
            list.setSummary(list.getEntry());
        }

        private void setInputMovieFpsSummary() {
            ((DoublePreference) findPreference(getResources().getString(R.string.KEY_INPUT_MOVIE_FPS))).setSummary();
        }

        private void checkSaveInputImages() {
            findPreference(getResources().getString(R.string.KEY_INPUT_MOVIE_FPS)).setEnabled(((ListPreference) getPreferenceScreen().findPreference(getResources().getString(R.string.KEY_SAVE_INPUT_IMAGES))).getValue().equals("1"));
        }

        private void updateCaptureModeEnabled() {
            ((ListPreference) getPreferenceScreen().findPreference(getResources().getString(R.string.KEY_CAPTURE_MODE))).setEnabled(this.mUseCamera1 ^ 1);
        }

        private void setColorCorrectionModeSummary() {
            ListPreference list = (ListPreference) getPreferenceScreen().findPreference(getResources().getString(R.string.KEY_COLOR_CORRECTION_MODE));
            list.setSummary(list.getEntry());
            list.setEnabled(this.mCamera2ImageQualitySettings[0].isAvailable());
        }

        private void setEdgeModeSummary() {
            ListPreference list = (ListPreference) getPreferenceScreen().findPreference(getResources().getString(R.string.KEY_EDGE_MODE));
            list.setSummary(list.getEntry());
            list.setEnabled(this.mCamera2ImageQualitySettings[1].isAvailable());
        }

        private void setNoiseReductionModeSummary() {
            ListPreference list = (ListPreference) getPreferenceScreen().findPreference(getResources().getString(R.string.KEY_NOISE_REDUCTION_MODE));
            list.setSummary(list.getEntry());
            list.setEnabled(this.mCamera2ImageQualitySettings[2].isAvailable());
        }

        private void setShadingModeSummary() {
            ListPreference list = (ListPreference) getPreferenceScreen().findPreference(getResources().getString(R.string.KEY_SHADING_MODE));
            list.setSummary(list.getEntry());
            list.setEnabled(this.mCamera2ImageQualitySettings[3].isAvailable());
        }

        private void setTonemapModeSummary() {
            ListPreference list = (ListPreference) getPreferenceScreen().findPreference(getResources().getString(R.string.KEY_TONEMAP_MODE));
            list.setSummary(list.getEntry());
            list.setEnabled(this.mCamera2ImageQualitySettings[4].isAvailable());
        }

        private void setAovGainSummary() {
            ((DoublePreference) findPreference(getResources().getString(R.string.KEY_AOV_GAIN))).setSummary();
        }

        private void setDistortionK1Summary() {
            ((DoublePreference) findPreference(getResources().getString(R.string.KEY_DISTORTION_K1))).setSummary();
        }

        private void setDistortionK2Summary() {
            ((DoublePreference) findPreference(getResources().getString(R.string.KEY_DISTORTION_K2))).setSummary();
        }

        private void setDistortionK3Summary() {
            ((DoublePreference) findPreference(getResources().getString(R.string.KEY_DISTORTION_K3))).setSummary();
        }

        private void setDistortionK4Summary() {
            ((DoublePreference) findPreference(getResources().getString(R.string.KEY_DISTORTION_K4))).setSummary();
        }

        private void setRotationRatioSummary() {
            ((DoublePreference) findPreference(getResources().getString(R.string.KEY_ROTATION_RATIO))).setSummary();
        }

        private void setUiControlModeSummary() {
            ListPreference list = (ListPreference) findPreference(getResources().getString(R.string.KEY_UI_CONTROL_MODE));
            list.setSummary(list.getEntry());
        }

        private void setFocusModeSummary() {
            ListPreference list = (ListPreference) findPreference(getResources().getString(R.string.KEY_FOCUS_MODE));
            list.setSummary(list.getEntry());
        }

        private void setProjectionModeSummary() {
            ListPreference list = (ListPreference) findPreference(getResources().getString(R.string.KEY_PROJECTION_MODE));
            list.setSummary(list.getEntry());
        }

        private void setMotionDetectionModeSummary() {
            ListPreference list = (ListPreference) findPreference(getResources().getString(R.string.KEY_MOTION_DETECTION_MODE));
            list.setSummary(list.getEntry());
        }

        private void getHarwareLevelSummary(String hardware_level) {
            getPreferenceScreen().findPreference(getString(R.string.KEY_HARDWARE_LEVEL)).setSummary(hardware_level);
        }

        private void getTimestampSource(String timestamp_source) {
            getPreferenceScreen().findPreference(getString(R.string.KEY_TIMESTAMP_SOURCE)).setSummary(timestamp_source);
        }

        private void captureModeChanged() {
            int capture_mode = Integer.parseInt(((ListPreference) getPreferenceScreen().findPreference(getResources().getString(R.string.KEY_CAPTURE_MODE))).getValue());
            ((ListPreference) getPreferenceScreen().findPreference(getResources().getString(R.string.KEY_COLOR_CORRECTION_MODE))).setValue(this.mCamera2ImageQualitySettings[0].getDefaultValue(capture_mode));
            ((ListPreference) getPreferenceScreen().findPreference(getResources().getString(R.string.KEY_EDGE_MODE))).setValue(this.mCamera2ImageQualitySettings[1].getDefaultValue(capture_mode));
            ((ListPreference) getPreferenceScreen().findPreference(getResources().getString(R.string.KEY_NOISE_REDUCTION_MODE))).setValue(this.mCamera2ImageQualitySettings[2].getDefaultValue(capture_mode));
            ((ListPreference) getPreferenceScreen().findPreference(getResources().getString(R.string.KEY_SHADING_MODE))).setValue(this.mCamera2ImageQualitySettings[3].getDefaultValue(capture_mode));
            ((ListPreference) getPreferenceScreen().findPreference(getResources().getString(R.string.KEY_TONEMAP_MODE))).setValue(this.mCamera2ImageQualitySettings[4].getDefaultValue(capture_mode));
        }

        private void initializeImageQualitySettingsList(int key, int index) {
            if (this.mCamera2ImageQualitySettings[index].isAvailable()) {
                ListPreference list = (ListPreference) findPreference(getResources().getString(key));
                list.setEntries(this.mCamera2ImageQualitySettings[index].entries);
                list.setEntryValues(this.mCamera2ImageQualitySettings[index].entryValues);
            }
        }

        private void useCamera2Changed() {
            exitSetting(R.string.panoramagp3_EXIT_SETTING);
        }

        private void setCameraSummary() {
            ListPreference list = (ListPreference) findPreference(getResources().getString(R.string.KEY_CAMERA_ID));
            list.setSummary(list.getEntry());
        }

        private void cameraChanged() {
            exitSetting(R.string.panoramagp3_EXIT_SETTING);
        }

        private void exitSetting(int msg_id) {
            new Builder(getActivity()).setCancelable(false).setMessage(getResources().getString(msg_id)).setPositiveButton(R.string.panoramagp3_OK, new OnClickListener() {
                public void onClick(DialogInterface di, int whichButton) {
                    SettingFragment.this.getActivity().finish();
                }
            }).show();
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager().beginTransaction().replace(16908290, new SettingFragment()).commit();
    }
}
