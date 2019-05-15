package com.hmdglobal.app.camera.settings;

import android.content.SharedPreferences;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;

public abstract class SettingsUpgrader {
    protected static final String OLD_SETTINGS_VALUE_NONE = "none";
    protected static final String OLD_SETTINGS_VALUE_OFF = "off";
    protected static final String OLD_SETTINGS_VALUE_ON = "on";
    private static final Tag TAG = new Tag("SettingsUpgrader");
    private final int mTargetVersion;
    private final String mVersionKey;

    public abstract void upgrade(SettingsManager settingsManager, int i, int i2);

    public SettingsUpgrader(String versionKey, int targetVersion) {
        this.mVersionKey = versionKey;
        this.mTargetVersion = targetVersion;
    }

    public void upgrade(SettingsManager settingsManager) {
        int lastVersion = getLastVersion(settingsManager);
        if (lastVersion != this.mTargetVersion) {
            upgrade(settingsManager, lastVersion, this.mTargetVersion);
        }
        settingsManager.set(SettingsManager.SCOPE_GLOBAL, this.mVersionKey, this.mTargetVersion);
    }

    /* Access modifiers changed, original: protected */
    public int getLastVersion(SettingsManager settingsManager) {
        return settingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, this.mVersionKey).intValue();
    }

    /* Access modifiers changed, original: protected */
    public boolean removeBoolean(SharedPreferences oldPreferencesLocation, String key) {
        boolean value = false;
        try {
            value = oldPreferencesLocation.getBoolean(key, false);
        } catch (ClassCastException e) {
            Log.e(TAG, "error reading old value, removing and returning default", e);
        }
        oldPreferencesLocation.edit().remove(key).apply();
        return value;
    }

    /* Access modifiers changed, original: protected */
    public int removeInteger(SharedPreferences oldPreferencesLocation, String key, int defaultValue) {
        int value = 0;
        try {
            value = oldPreferencesLocation.getInt(key, 0);
        } catch (ClassCastException e) {
            Log.e(TAG, "error reading old int value, try string value", e);
            try {
                value = Integer.parseInt(oldPreferencesLocation.getString(key, Integer.toString(defaultValue)));
            } catch (ClassCastException e2) {
                Log.e(TAG, "error reading old string value, removing and returning default", e);
            }
        }
        oldPreferencesLocation.edit().remove(key).apply();
        return value;
    }

    /* Access modifiers changed, original: protected */
    public String removeString(SharedPreferences oldPreferencesLocation, String key) {
        String value = null;
        try {
            value = oldPreferencesLocation.getString(key, null);
        } catch (ClassCastException e) {
            Log.e(TAG, "error reading old value, removing and returning default", e);
        }
        oldPreferencesLocation.edit().remove(key).apply();
        return value;
    }
}
