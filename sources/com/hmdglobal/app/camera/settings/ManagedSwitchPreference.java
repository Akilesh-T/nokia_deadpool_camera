package com.hmdglobal.app.camera.settings;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import com.hmdglobal.app.camera.app.CameraApp;

public class ManagedSwitchPreference extends SwitchPreference {
    public ManagedSwitchPreference(Context context) {
        super(context);
    }

    public ManagedSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ManagedSwitchPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public boolean getPersistedBoolean(boolean defaultReturnValue) {
        CameraApp cameraApp = getCameraApp();
        if (cameraApp == null) {
            return defaultReturnValue;
        }
        SettingsManager settingsManager = cameraApp.getSettingsManager();
        if (settingsManager != null) {
            return settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, getKey());
        }
        return defaultReturnValue;
    }

    public boolean persistBoolean(boolean value) {
        CameraApp cameraApp = getCameraApp();
        if (cameraApp == null) {
            return false;
        }
        SettingsManager settingsManager = cameraApp.getSettingsManager();
        if (settingsManager == null) {
            return false;
        }
        settingsManager.set(SettingsManager.SCOPE_GLOBAL, getKey(), value);
        return true;
    }

    private CameraApp getCameraApp() {
        Context context = getContext();
        if (context instanceof Activity) {
            Application application = ((Activity) context).getApplication();
            if (application instanceof CameraApp) {
                return (CameraApp) application;
            }
        }
        return null;
    }
}
