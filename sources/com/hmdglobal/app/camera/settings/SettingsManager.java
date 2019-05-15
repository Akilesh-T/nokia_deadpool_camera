package com.hmdglobal.app.camera.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Rect;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.preference.PreferenceManager;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.util.Size;
import java.util.ArrayList;
import java.util.List;

public class SettingsManager {
    public static final String SCOPE_GLOBAL = "default_scope";
    private static final Tag TAG = new Tag("SettingsManager");
    private ArrayList<CameraCharacteristics> mCharacteristics = new ArrayList();
    private final Context mContext;
    private SharedPreferences mCustomPreferences;
    private final SharedPreferences mDefaultPreferences;
    private final DefaultsStore mDefaultsStore = new DefaultsStore();
    private final List<OnSettingChangedListener> mListeners = new ArrayList();
    private final String mPackageName;
    private final List<OnSharedPreferenceChangeListener> mSharedPreferenceListeners = new ArrayList();

    public interface OnSettingChangedListener {
        void onSettingChanged(SettingsManager settingsManager, String str);
    }

    public SettingsManager(Context context) {
        this.mContext = context;
        this.mPackageName = this.mContext.getPackageName();
        this.mDefaultPreferences = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        CameraManager manager = (CameraManager) this.mContext.getSystemService("camera");
        try {
            String[] cameraIdList = manager.getCameraIdList();
            for (int i = 0; i < cameraIdList.length; i++) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraIdList[i]);
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("cameraIdList size =");
                stringBuilder.append(cameraIdList.length);
                Log.d(tag, stringBuilder.toString());
                this.mCharacteristics.add(i, characteristics);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public SharedPreferences getDefaultPreferences() {
        return this.mDefaultPreferences;
    }

    /* Access modifiers changed, original: protected */
    public SharedPreferences openPreferences(String scope) {
        SharedPreferences preferences = this.mContext;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.mPackageName);
        stringBuilder.append(scope);
        preferences = preferences.getSharedPreferences(stringBuilder.toString(), 0);
        for (OnSharedPreferenceChangeListener listener : this.mSharedPreferenceListeners) {
            preferences.registerOnSharedPreferenceChangeListener(listener);
        }
        return preferences;
    }

    /* Access modifiers changed, original: protected */
    public void closePreferences(SharedPreferences preferences) {
        for (OnSharedPreferenceChangeListener listener : this.mSharedPreferenceListeners) {
            preferences.unregisterOnSharedPreferenceChangeListener(listener);
        }
    }

    private OnSharedPreferenceChangeListener getSharedPreferenceListener(final OnSettingChangedListener listener) {
        return new OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                listener.onSettingChanged(SettingsManager.this, key);
            }
        };
    }

    public void addListener(OnSettingChangedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("OnSettingChangedListener cannot be null.");
        } else if (!this.mListeners.contains(listener)) {
            this.mListeners.add(listener);
            OnSharedPreferenceChangeListener sharedPreferenceListener = getSharedPreferenceListener(listener);
            this.mSharedPreferenceListeners.add(sharedPreferenceListener);
            this.mDefaultPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceListener);
            if (this.mCustomPreferences != null) {
                this.mCustomPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceListener);
            }
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("listeners: ");
            stringBuilder.append(this.mListeners);
            Log.v(tag, stringBuilder.toString());
        }
    }

    public void removeListener(OnSettingChangedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException();
        } else if (this.mListeners.contains(listener)) {
            int index = this.mListeners.indexOf(listener);
            this.mListeners.remove(listener);
            OnSharedPreferenceChangeListener sharedPreferenceListener = (OnSharedPreferenceChangeListener) this.mSharedPreferenceListeners.get(index);
            this.mSharedPreferenceListeners.remove(index);
            this.mDefaultPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceListener);
            if (this.mCustomPreferences != null) {
                this.mCustomPreferences.unregisterOnSharedPreferenceChangeListener(sharedPreferenceListener);
            }
        }
    }

    public void removeAllListeners() {
        for (OnSharedPreferenceChangeListener listener : this.mSharedPreferenceListeners) {
            this.mDefaultPreferences.unregisterOnSharedPreferenceChangeListener(listener);
            if (this.mCustomPreferences != null) {
                this.mCustomPreferences.unregisterOnSharedPreferenceChangeListener(listener);
            }
        }
        this.mSharedPreferenceListeners.clear();
        this.mListeners.clear();
    }

    private SharedPreferences getPreferencesFromScope(String scope) {
        if (scope.equals(SCOPE_GLOBAL)) {
            return this.mDefaultPreferences;
        }
        if (this.mCustomPreferences != null) {
            closePreferences(this.mCustomPreferences);
        }
        this.mCustomPreferences = openPreferences(scope);
        return this.mCustomPreferences;
    }

    public void setDefaults(String key, String defaultValue, String[] possibleValues) {
        this.mDefaultsStore.storeDefaults(key, defaultValue, possibleValues);
    }

    public void setDefaults(String key, int defaultValue, int[] possibleValues) {
        String defaultValueString = Integer.toString(defaultValue);
        String[] possibleValuesString = new String[possibleValues.length];
        for (int i = 0; i < possibleValues.length; i++) {
            possibleValuesString[i] = Integer.toString(possibleValues[i]);
        }
        this.mDefaultsStore.storeDefaults(key, defaultValueString, possibleValuesString);
    }

    public void setDefaults(String key, boolean defaultValue) {
        this.mDefaultsStore.storeDefaults(key, defaultValue ? "1" : "0", new String[]{"0", "1"});
    }

    public String getStringDefault(String key) {
        return this.mDefaultsStore.getDefaultValue(key);
    }

    public Integer getIntegerDefault(String key) {
        String defaultValueString = this.mDefaultsStore.getDefaultValue(key);
        return Integer.valueOf(defaultValueString == null ? 0 : Integer.parseInt(defaultValueString));
    }

    public boolean getBooleanDefault(String key) {
        String defaultValueString = this.mDefaultsStore.getDefaultValue(key);
        if (defaultValueString == null || Integer.parseInt(defaultValueString) == 0) {
            return false;
        }
        return true;
    }

    public String getString(String scope, String key, String defaultValue) {
        SharedPreferences preferences = getPreferencesFromScope(scope);
        try {
            return preferences.getString(key, defaultValue);
        } catch (ClassCastException e) {
            Log.w(TAG, "existing preference with invalid type, removing and returning default", e);
            preferences.edit().remove(key).apply();
            return defaultValue;
        }
    }

    public String getString(String scope, String key) {
        return getString(scope, key, getStringDefault(key));
    }

    public Integer getInteger(String scope, String key, Integer defaultValue) {
        return Integer.valueOf(convertToInt(getString(scope, key, Integer.toString(defaultValue.intValue()))));
    }

    public Integer getInteger(String scope, String key) {
        return getInteger(scope, key, getIntegerDefault(key));
    }

    public boolean getBoolean(String scope, String key, boolean defaultValue) {
        return convertToBoolean(getString(scope, key, defaultValue ? "1" : "0"));
    }

    public long getLong(String scope, String key, long defaultValue) {
        SharedPreferences preferences = getPreferencesFromScope(scope);
        try {
            return preferences.getLong(key, defaultValue);
        } catch (ClassCastException e) {
            Log.w(TAG, "existing preference with invalid type, removing and returning default", e);
            preferences.edit().remove(key).apply();
            return defaultValue;
        }
    }

    public boolean getBoolean(String scope, String key) {
        return getBoolean(scope, key, getBooleanDefault(key));
    }

    public Size getSize(String scope, String key) {
        String strValue = getString(scope, key);
        if (strValue == null) {
            return null;
        }
        String[] widthHeight = strValue.split("x");
        if (widthHeight.length != 2) {
            return null;
        }
        try {
            return new Size(Integer.parseInt(widthHeight[0]), Integer.parseInt(widthHeight[1]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public int getIndexOfCurrentValue(String scope, String key) {
        String[] possibleValues = this.mDefaultsStore.getPossibleValues(key);
        if (possibleValues == null || possibleValues.length == 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("No possible values for scope=");
            stringBuilder.append(scope);
            stringBuilder.append(" key=");
            stringBuilder.append(key);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        String value = getString(scope, key);
        for (int i = 0; i < possibleValues.length; i++) {
            if (value.equals(possibleValues[i])) {
                return i;
            }
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Current value for scope=");
        stringBuilder2.append(scope);
        stringBuilder2.append(" key=");
        stringBuilder2.append(key);
        stringBuilder2.append(" not in list of possible values");
        throw new IllegalStateException(stringBuilder2.toString());
    }

    public void set(String scope, String key, String value) {
        getPreferencesFromScope(scope).edit().putString(key, value).apply();
    }

    public void set(String scope, String key, int value) {
        set(scope, key, convert(value));
    }

    public void set(String scope, String key, boolean value) {
        set(scope, key, convert(value));
    }

    public void set(String scope, String key, long value) {
        getPreferencesFromScope(scope).edit().putLong(key, value).apply();
    }

    public void setToDefault(String scope, String key) {
        set(scope, key, getStringDefault(key));
    }

    public void setToDefault(String scope) {
        getPreferencesFromScope(scope).edit().clear().apply();
    }

    public void setValueByIndex(String scope, String key, int index) {
        String[] possibleValues = this.mDefaultsStore.getPossibleValues(key);
        StringBuilder stringBuilder;
        if (possibleValues.length == 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("No possible values for scope=");
            stringBuilder.append(scope);
            stringBuilder.append(" key=");
            stringBuilder.append(key);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (index < 0 || index >= possibleValues.length) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("For possible values of scope=");
            stringBuilder.append(scope);
            stringBuilder.append(" key=");
            stringBuilder.append(key);
            throw new IndexOutOfBoundsException(stringBuilder.toString());
        } else {
            set(scope, key, possibleValues[index]);
        }
    }

    public String getValueByIndex(String scope, String key, int index) {
        String[] possibleValues = this.mDefaultsStore.getPossibleValues(key);
        StringBuilder stringBuilder;
        if (possibleValues.length == 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("No possible values for scope=");
            stringBuilder.append(scope);
            stringBuilder.append(" key=");
            stringBuilder.append(key);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (index >= 0 && index < possibleValues.length) {
            return possibleValues[index];
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("For possible values of scope=");
            stringBuilder.append(scope);
            stringBuilder.append(" key=");
            stringBuilder.append(key);
            throw new IndexOutOfBoundsException(stringBuilder.toString());
        }
    }

    public Rect getSensorActiveArraySize(int id) {
        if (this.mCharacteristics == null || this.mCharacteristics.size() == 0) {
            return null;
        }
        return (Rect) ((CameraCharacteristics) this.mCharacteristics.get(id)).get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
    }

    public boolean isSet(String scope, String key) {
        return getPreferencesFromScope(scope).contains(key);
    }

    public boolean isDefault(String scope, String key) {
        String defaultValue = getStringDefault(key);
        String value = getString(scope, key);
        return value == null ? false : value.equals(defaultValue);
    }

    public void remove(String scope, String key) {
        getPreferencesFromScope(scope).edit().remove(key).apply();
    }

    static String convert(int value) {
        return Integer.toString(value);
    }

    static int convertToInt(String value) {
        return Integer.parseInt(value);
    }

    public static boolean convertToBoolean(String value) {
        return Integer.parseInt(value) != 0;
    }

    static String convert(boolean value) {
        return value ? "1" : "0";
    }
}
