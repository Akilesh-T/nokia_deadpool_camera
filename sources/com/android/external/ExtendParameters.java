package com.android.external;

import android.hardware.Camera.Parameters;
import android.text.TextUtils;
import android.text.TextUtils.SimpleStringSplitter;
import android.text.TextUtils.StringSplitter;
import android.util.Log;
import com.hmdglobal.app.camera.util.CameraUtil;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ExtendParameters {
    private static final String CAPTURE_MODE_CONTINUOUS_SHOT = "continuousshot";
    private static final String CAPTURE_MODE_NORMAL = "normal";
    public static final String FOCUS_MODE_MANUAL = "manual";
    public static final String KEY_AUTO_ISO = "auto";
    public static final String KEY_CONTINUOUS_ISO = "continuous-iso";
    public static final String KEY_EXPOSURE_TIME = "exposure-time";
    public static final String KEY_FOCUS_ENG_STEP = "afeng-pos";
    public static final String KEY_FOCUS_MODE = "focus-mode";
    public static final String KEY_ISO_MODE = "iso";
    public static final String KEY_ISO_VALUES = "iso-values";
    public static final String KEY_MANUAL_FOCUS_POSITION = "manual-focus-position";
    public static final String KEY_MANUAL_FOCUS_POS_TYPE = "manual-focus-pos-type";
    public static final String KEY_MANUAL_ISO = "manual";
    public static final String KEY_MAX_EXPOSURE_TIME = "max-exposure-time";
    public static final String KEY_MAX_FOCUS_SCALE = "max-focus-pos-ratio";
    public static final String KEY_MAX_ISO = "max-iso";
    public static final String KEY_MIN_EXPOSURE_TIME = "min-exposure-time";
    public static final String KEY_MIN_FOCUS_SCALE = "min-focus-pos-ratio";
    public static final String KEY_MIN_ISO = "min-iso";
    public static final String KEY_QC_SUPPORTED_FLIP_MODES = "flip-mode-values";
    public static final int MANUAL_FOCUS_POS_TYPE_DIOPTER = 3;
    public static final int MANUAL_FOCUS_POS_TYPE_SCALE = 2;
    private static final String TAG = "ExtendParameters";
    private static ExtendParameters mInstance;
    private static Parameters mParameters;
    private HashMap<String, Method> mParamMethodMap = new HashMap();

    public enum ExposureMode {
        CENTER_WEIGHTED,
        FRAME_AVERAGE,
        SPOT_METERING;

        public String toString() {
            String parameterValue = "";
            switch (this) {
                case CENTER_WEIGHTED:
                    return "center-weighted";
                case FRAME_AVERAGE:
                    return "frame-average";
                case SPOT_METERING:
                    return "spot-metering";
                default:
                    return parameterValue;
            }
        }
    }

    public static synchronized ExtendParameters getInstance(Parameters param) {
        ExtendParameters extendParameters;
        synchronized (ExtendParameters.class) {
            if (mInstance == null || param != mParameters) {
                mInstance = new ExtendParameters(param);
            }
            extendParameters = mInstance;
        }
        return extendParameters;
    }

    private ExtendParameters(Parameters parameters) {
        mParameters = parameters;
        for (Method method : Parameters.class.getMethods()) {
            this.mParamMethodMap.put(method.getName(), method);
        }
    }

    private void set(String method, Object value) {
        if (value != null) {
            Method setMethod = (Method) this.mParamMethodMap.get(method);
            if (setMethod != null) {
                try {
                    setMethod.setAccessible(true);
                    setMethod.invoke(mParameters, new Object[]{value});
                } catch (Exception e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("set parameter failed!method:");
                    stringBuilder.append(method.toString());
                    stringBuilder.append(",value:");
                    stringBuilder.append(value);
                    Log.d(str, stringBuilder.toString());
                    e.printStackTrace();
                }
            }
        }
    }

    private List<String> query(String method) {
        Method queryMethod = (Method) this.mParamMethodMap.get(method);
        if (queryMethod != null) {
            try {
                queryMethod.setAccessible(true);
                return (List) queryMethod.invoke(mParameters, new Object[0]);
            } catch (Exception e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("query failed!method:");
                stringBuilder.append(method.toString());
                Log.d(str, stringBuilder.toString());
                e.printStackTrace();
            }
        }
        return new ArrayList();
    }

    public boolean isISOModeSupport() {
        if (this.mParamMethodMap.containsKey("getSupportedISOSpeed") || this.mParamMethodMap.containsKey("getSupportedIsoValues")) {
            return true;
        }
        return false;
    }

    @Deprecated
    public void setISOValue(String value) {
        if (this.mParamMethodMap.containsKey("setISOSpeed")) {
            set("setISOSpeed", value);
        } else if (this.mParamMethodMap.containsKey("setISOValue")) {
            set("setISOValue", value);
        }
    }

    public void setContinuousISOValue(int value) {
        if (!KEY_ISO_MODE.equals(mParameters.get(KEY_ISO_MODE))) {
            mParameters.set(KEY_ISO_MODE, "manual");
        }
        mParameters.set(KEY_CONTINUOUS_ISO, value);
    }

    public List<String> getSupportedZSLValues() {
        if (this.mParamMethodMap.containsKey("getSupportedZSDMode")) {
            return query("getSupportedZSDMode");
        }
        if (this.mParamMethodMap.containsKey("getSupportedZSLModes")) {
            return query("getSupportedZSLModes");
        }
        return new ArrayList();
    }

    public void setZSLMode(String value) {
        if (value != null) {
            boolean q = false;
            if (this.mParamMethodMap.containsKey("setZSDMode")) {
                set("setZSDMode", value);
                q = true;
            } else if (this.mParamMethodMap.containsKey("setZSLMode")) {
                set("setZSLMode", value);
                q = true;
            }
            if ((false || q) && this.mParamMethodMap.containsKey("setCameraMode")) {
                int cameraModeValue = 0;
                if (value.equalsIgnoreCase("on")) {
                    cameraModeValue = 1;
                }
                set("setCameraMode", Integer.valueOf(cameraModeValue));
            }
        }
    }

    public boolean getZSLMode() {
        String zsl = null;
        if (this.mParamMethodMap.containsKey("getZSDMode")) {
            zsl = get("getZSDMode");
        } else if (this.mParamMethodMap.containsKey("getZSLMode")) {
            zsl = get("getZSLMode");
        }
        if (TextUtils.equals(zsl, "on")) {
            return true;
        }
        return false;
    }

    private String get(String method) {
        Method getMethod = (Method) this.mParamMethodMap.get(method);
        if (getMethod != null) {
            try {
                getMethod.setAccessible(true);
                return (String) getMethod.invoke(mParameters, new Object[0]);
            } catch (Exception e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("set parameter failed!method:");
                stringBuilder.append(method.toString());
                Log.d(str, stringBuilder.toString());
                e.printStackTrace();
            }
        }
        return null;
    }

    public void setISO(String isoValue) {
        mParameters.set(KEY_ISO_MODE, isoValue);
    }

    public void setManualISO(int isoValue) {
        mParameters.set(KEY_CONTINUOUS_ISO, isoValue);
    }

    public void setExposureTime(String exposureTime) {
        mParameters.set(KEY_EXPOSURE_TIME, exposureTime);
    }

    public int getMinISO() {
        if (mParameters.get(KEY_MIN_ISO) != null) {
            return mParameters.getInt(KEY_MIN_ISO);
        }
        return 0;
    }

    public int getMaxISO() {
        if (mParameters.get(KEY_MAX_ISO) != null) {
            return mParameters.getInt(KEY_MAX_ISO);
        }
        return 0;
    }

    public List<String> getSupportedISOValues() {
        List<String> supportedISOValues = new ArrayList();
        String isoValues = mParameters.get(KEY_ISO_VALUES);
        if (isoValues == null) {
            isoValues = mParameters.get("iso-speed-values");
        }
        if (isoValues != null) {
            return split(isoValues);
        }
        return supportedISOValues;
    }

    private static ArrayList<String> split(String str) {
        if (str == null) {
            return null;
        }
        StringSplitter<String> splitter = new SimpleStringSplitter(',');
        splitter.setString(str);
        ArrayList<String> substrings = new ArrayList();
        for (String s : splitter) {
            substrings.add(s);
        }
        return substrings;
    }

    public String getMinExposureTime() {
        String minExposureTime = mParameters.get(KEY_MIN_EXPOSURE_TIME);
        if (minExposureTime != null) {
            return minExposureTime;
        }
        return "0.0";
    }

    public String getMaxExposureTime() {
        String maxExposureTime = mParameters.get(KEY_MAX_EXPOSURE_TIME);
        if (maxExposureTime != null) {
            return maxExposureTime;
        }
        return "0.0";
    }

    public void updateManualFocusPosition(int focusPosition) {
        mParameters.set(KEY_MANUAL_FOCUS_POS_TYPE, 2);
        mParameters.set(KEY_MANUAL_FOCUS_POSITION, focusPosition);
    }

    public int getMinFocusScale() {
        if (mParameters.get(KEY_MIN_FOCUS_SCALE) != null) {
            return mParameters.getInt(KEY_MIN_FOCUS_SCALE);
        }
        return 0;
    }

    public int getMaxFocusScale() {
        if (mParameters.get(KEY_MAX_FOCUS_SCALE) != null) {
            return mParameters.getInt(KEY_MAX_FOCUS_SCALE);
        }
        return 0;
    }

    public List<String> getSupportedFDModeValues() {
        return query("getSupportedFaceDetectionModes");
    }

    public void setFaceDetectionMode(String value) {
        set("setFaceDetectionMode", value);
    }

    public void SetBrightness(String value) {
        mParameters.set("luma-adaptation", value);
    }

    public List<String> getSupportedTouchAfAec() {
        return query("getSupportedTouchAfAec");
    }

    public void setTouchAfAec(String value) {
        set("setTouchAfAec", value);
    }

    public List<String> getSupportedSelectableZoneAf() {
        return query("getSupportedSelectableZoneAf");
    }

    public void setSelectableZoneAf(String value) {
        set("setSelectableZoneAf", value);
    }

    public List<String> getSupportedDenoiseModes() {
        return query("getSupportedDenoiseModes");
    }

    public void setDenoise(String value) {
        set("setDenoise", value);
    }

    public List<String> getSupportedRedeyeReductionModes() {
        return query("getSupportedRedeyeReductionModes");
    }

    public void setRedeyeReductionMode(String value) {
        set("setRedeyeReductionMode", value);
    }

    public void setSaturation(String value) {
        set("setSaturation", Integer.valueOf(value));
    }

    public void setContrast(String value) {
        set("setContrast", Integer.valueOf(value));
    }

    public void setSharpness(String value) {
        set("setSharpness", Integer.valueOf(value));
    }

    public List<String> getSupportedAutoexposure() {
        return query("getSupportedAutoexposure");
    }

    public void setAutoExposure(ExposureMode mode) {
        String value = mode.toString();
        if (CameraUtil.isSupported(value, getSupportedAutoexposure())) {
            set("setAutoExposure", value);
        }
    }

    public List<String> getSupportedVideoHDRModes() {
        return query("getSupportedVideoHDRModes");
    }

    public void setVideoHDRMode(String value) {
        set("setVideoHDRMode", value);
    }

    private void setCaptureNum(int num) {
        set("setBurstShotNum", Integer.valueOf(num));
    }

    public void setTintless(String value) {
        mParameters.set("tintless", value);
    }

    public void setBurstShot(boolean enable) {
        String setCaptureMode = "setCaptureMode";
        if (enable) {
            setCaptureNum(40);
            set("setCaptureMode", CAPTURE_MODE_CONTINUOUS_SHOT);
            return;
        }
        setCaptureNum(1);
        set("setCaptureMode", CAPTURE_MODE_NORMAL);
    }

    public static List<String> getSupportedFlipMode() {
        String str = mParameters.get(KEY_QC_SUPPORTED_FLIP_MODES);
        if (str == null) {
            return null;
        }
        return split(str);
    }

    public static boolean isFlipSupported(String value) {
        List<String> supported = getSupportedFlipMode();
        return supported != null && supported.indexOf(value) >= 0;
    }
}
