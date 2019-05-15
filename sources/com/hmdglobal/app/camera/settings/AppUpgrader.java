package com.hmdglobal.app.camera.settings;

import android.content.Context;
import android.content.SharedPreferences;
import com.android.ex.camera2.portability.CameraAgentFactory;
import com.android.ex.camera2.portability.CameraAgentFactory.CameraApi;
import com.android.ex.camera2.portability.CameraDeviceInfo;
import com.android.ex.camera2.portability.Size;
import com.hmdglobal.app.camera.CameraActivity;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.app.AppController;
import com.hmdglobal.app.camera.app.ModuleManager.ModuleAgent;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.module.ModuleController;
import com.hmdglobal.app.camera.settings.SettingsUtil.CameraDeviceSelector;
import java.util.List;
import java.util.Map.Entry;

public class AppUpgrader extends SettingsUpgrader {
    public static final int APP_UPGRADE_VERSION = 6;
    private static final int CAMERA_MODULE_SETTINGS_FILES_RENAMED_VERSION = 6;
    private static final int CAMERA_SETTINGS_SELECTED_MODULE_INDEX = 5;
    private static final int CAMERA_SETTINGS_STRINGS_UPGRADE = 5;
    private static final int CAMERA_SIZE_SETTING_UPGRADE_VERSION = 3;
    private static final int FORCE_LOCATION_CHOICE_VERSION = 2;
    private static final String OLD_CAMERA_PREFERENCES_PREFIX = "_preferences_";
    private static final String OLD_GLOBAL_PREFERENCES_FILENAME = "_preferences_camera";
    private static final String OLD_KEY_UPGRADE_VERSION = "pref_strict_upgrade_version";
    private static final String OLD_MODULE_PREFERENCES_PREFIX = "_preferences_module_";
    private static final Tag TAG = new Tag("AppUpgrader");
    private final AppController mAppController;

    public AppUpgrader(AppController appController) {
        super(Keys.KEY_UPGRADE_VERSION, 6);
        this.mAppController = appController;
    }

    /* Access modifiers changed, original: protected */
    public int getLastVersion(SettingsManager settingsManager) {
        SharedPreferences defaultPreferences = settingsManager.getDefaultPreferences();
        if (defaultPreferences.contains(OLD_KEY_UPGRADE_VERSION)) {
            Object oldVersion = defaultPreferences.getAll().get(OLD_KEY_UPGRADE_VERSION);
            defaultPreferences.edit().remove(OLD_KEY_UPGRADE_VERSION).apply();
            if (oldVersion instanceof Integer) {
                return ((Integer) oldVersion).intValue();
            }
            if (oldVersion instanceof String) {
                return SettingsManager.convertToInt((String) oldVersion);
            }
        }
        return super.getLastVersion(settingsManager);
    }

    public void upgrade(SettingsManager settingsManager, int lastVersion, int currentVersion) {
        Context context = this.mAppController.getAndroidContext();
        if (lastVersion < 5) {
            upgradeTypesToStrings(settingsManager);
        }
        if (lastVersion < 2) {
            forceLocationChoice(settingsManager);
        }
        if (lastVersion < 3) {
            CameraAgentFactory.getAndroidCameraAgent(context, CameraApi.API_1).getCameraDeviceInfo();
        }
        if (lastVersion < 6) {
            upgradeCameraSettingsFiles(settingsManager, context);
            upgradeModuleSettingsFiles(settingsManager, context, this.mAppController);
        }
        if (lastVersion < 5) {
            upgradeSelectedModeIndex(settingsManager, context);
        }
    }

    private void upgradeTypesToStrings(SettingsManager settingsManager) {
        SharedPreferences defaultPreferences = settingsManager.getDefaultPreferences();
        SharedPreferences oldGlobalPreferences = settingsManager.openPreferences(OLD_GLOBAL_PREFERENCES_FILENAME);
        if (defaultPreferences.contains(Keys.KEY_RECORD_LOCATION)) {
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_RECORD_LOCATION, removeBoolean(defaultPreferences, Keys.KEY_RECORD_LOCATION));
        }
        if (defaultPreferences.contains(Keys.KEY_USER_SELECTED_ASPECT_RATIO)) {
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_USER_SELECTED_ASPECT_RATIO, removeBoolean(defaultPreferences, Keys.KEY_USER_SELECTED_ASPECT_RATIO));
        }
        if (defaultPreferences.contains(Keys.KEY_EXPOSURE_COMPENSATION_ENABLED)) {
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_EXPOSURE_COMPENSATION_ENABLED, removeBoolean(defaultPreferences, Keys.KEY_EXPOSURE_COMPENSATION_ENABLED));
        }
        if (defaultPreferences.contains(Keys.KEY_CAMERA_FIRST_USE_HINT_SHOWN)) {
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_FIRST_USE_HINT_SHOWN, removeBoolean(defaultPreferences, Keys.KEY_CAMERA_FIRST_USE_HINT_SHOWN));
        }
        if (defaultPreferences.contains(Keys.KEY_STARTUP_MODULE_INDEX)) {
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_STARTUP_MODULE_INDEX, removeInteger(defaultPreferences, Keys.KEY_STARTUP_MODULE_INDEX, settingsManager.getIntegerDefault(Keys.KEY_STARTUP_MODULE_INDEX).intValue()));
        }
        if (defaultPreferences.contains(Keys.KEY_CAMERA_MODULE_LAST_USED)) {
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_MODULE_LAST_USED, removeInteger(defaultPreferences, Keys.KEY_CAMERA_MODULE_LAST_USED, settingsManager.getIntegerDefault(Keys.KEY_CAMERA_MODULE_LAST_USED).intValue()));
        }
        if (oldGlobalPreferences.contains(Keys.KEY_FLASH_SUPPORTED_BACK_CAMERA)) {
            boolean flashSupportedBackCamera = removeBoolean(oldGlobalPreferences, Keys.KEY_FLASH_SUPPORTED_BACK_CAMERA);
            if (flashSupportedBackCamera) {
                settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_FLASH_SUPPORTED_BACK_CAMERA, flashSupportedBackCamera);
            }
        }
        if (defaultPreferences.contains(Keys.KEY_SHOULD_SHOW_REFOCUS_VIEWER_CLING)) {
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_SHOULD_SHOW_REFOCUS_VIEWER_CLING, removeBoolean(defaultPreferences, Keys.KEY_SHOULD_SHOW_REFOCUS_VIEWER_CLING));
        }
        if (defaultPreferences.contains(Keys.KEY_SHOULD_SHOW_SETTINGS_BUTTON_CLING)) {
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_SHOULD_SHOW_SETTINGS_BUTTON_CLING, removeBoolean(defaultPreferences, Keys.KEY_SHOULD_SHOW_SETTINGS_BUTTON_CLING));
        }
        if (oldGlobalPreferences.contains(Keys.KEY_CAMERA_HDR_PLUS)) {
            if ("on".equals(removeString(oldGlobalPreferences, Keys.KEY_CAMERA_HDR_PLUS))) {
                settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR_PLUS, true);
            }
        }
        if (oldGlobalPreferences.contains(Keys.KEY_CAMERA_HDR)) {
            if ("on".equals(removeString(oldGlobalPreferences, Keys.KEY_CAMERA_HDR))) {
                settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_HDR, "on");
            }
        }
        if (oldGlobalPreferences.contains(Keys.KEY_CAMERA_GRID_LINES)) {
            if ("on".equals(removeString(oldGlobalPreferences, Keys.KEY_CAMERA_GRID_LINES))) {
                settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_GRID_LINES, true);
            }
        }
        if (oldGlobalPreferences.contains(Keys.KEY_CAMERA_MOTION)) {
            if ("on".equals(removeString(oldGlobalPreferences, Keys.KEY_CAMERA_MOTION))) {
                settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_MOTION, true);
            }
        }
    }

    private void forceLocationChoice(SettingsManager settingsManager) {
        SharedPreferences oldGlobalPreferences = settingsManager.openPreferences(OLD_GLOBAL_PREFERENCES_FILENAME);
        if (settingsManager.isSet(SettingsManager.SCOPE_GLOBAL, Keys.KEY_RECORD_LOCATION)) {
            if (!settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_RECORD_LOCATION)) {
                settingsManager.remove(SettingsManager.SCOPE_GLOBAL, Keys.KEY_RECORD_LOCATION);
            }
        } else if (oldGlobalPreferences.contains(Keys.KEY_RECORD_LOCATION)) {
            if ("on".equals(removeString(oldGlobalPreferences, Keys.KEY_RECORD_LOCATION))) {
                settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_RECORD_LOCATION, true);
            }
        }
    }

    private void upgradeCameraSizeSetting(SettingsManager settingsManager, Context context, CameraDeviceInfo infos, CameraDeviceSelector facing) {
        String key;
        if (facing == SettingsUtil.CAMERA_FACING_FRONT) {
            key = Keys.KEY_PICTURE_SIZE_FRONT;
        } else if (facing == SettingsUtil.CAMERA_FACING_BACK) {
            key = Keys.KEY_PICTURE_SIZE_BACK;
        } else {
            Log.w(TAG, "Ignoring attempt to upgrade size of unhandled camera facing direction");
            return;
        }
        if (infos == null) {
            settingsManager.remove(SettingsManager.SCOPE_GLOBAL, key);
            return;
        }
        String pictureSize = settingsManager.getString(SettingsManager.SCOPE_GLOBAL, key);
        int camera = SettingsUtil.getCameraId(infos, facing);
        if (camera != -1) {
            List<Size> supported = CameraPictureSizesCacher.getSizesForCamera(camera, context);
            if (supported != null) {
                settingsManager.set(SettingsManager.SCOPE_GLOBAL, key, SettingsUtil.sizeToSetting(SettingsUtil.getPhotoSize(pictureSize, supported, camera)));
            }
        }
    }

    private void copyPreferences(SharedPreferences oldPrefs, SharedPreferences newPrefs) {
        for (Entry<String, ?> entry : oldPrefs.getAll().entrySet()) {
            String key = (String) entry.getKey();
            Object value = entry.getValue();
            Tag tag;
            StringBuilder stringBuilder;
            if (value == null) {
                tag = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("skipped upgrade and removing entry for null key ");
                stringBuilder.append(key);
                Log.w(tag, stringBuilder.toString());
                newPrefs.edit().remove(key).apply();
            } else if (value instanceof Boolean) {
                newPrefs.edit().putString(key, SettingsManager.convert(((Boolean) value).booleanValue())).apply();
            } else if (value instanceof Integer) {
                newPrefs.edit().putString(key, SettingsManager.convert(((Integer) value).intValue())).apply();
            } else if (value instanceof Long) {
                long longValue = ((Long) value).longValue();
                if (longValue > 2147483647L || longValue < -2147483648L) {
                    Tag tag2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("skipped upgrade for out of bounds long key ");
                    stringBuilder2.append(key);
                    stringBuilder2.append(" : ");
                    stringBuilder2.append(longValue);
                    Log.w(tag2, stringBuilder2.toString());
                } else {
                    newPrefs.edit().putString(key, SettingsManager.convert((int) longValue)).apply();
                }
            } else if (value instanceof String) {
                newPrefs.edit().putString(key, (String) value).apply();
            } else {
                tag = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("skipped upgrade and removing entry for unrecognized key type ");
                stringBuilder.append(key);
                stringBuilder.append(" : ");
                stringBuilder.append(value.getClass());
                Log.w(tag, stringBuilder.toString());
                newPrefs.edit().remove(key).apply();
            }
        }
    }

    private void upgradeCameraSettingsFiles(SettingsManager settingsManager, Context context) {
        String[] cameraIds = context.getResources().getStringArray(R.array.camera_id_entryvalues);
        for (int i = 0; i < cameraIds.length; i++) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(OLD_CAMERA_PREFERENCES_PREFIX);
            stringBuilder.append(cameraIds[i]);
            SharedPreferences oldCameraPreferences = settingsManager.openPreferences(stringBuilder.toString());
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(CameraActivity.CAMERA_SCOPE_PREFIX);
            stringBuilder2.append(cameraIds[i]);
            copyPreferences(oldCameraPreferences, settingsManager.openPreferences(stringBuilder2.toString()));
        }
    }

    private void upgradeModuleSettingsFiles(SettingsManager settingsManager, Context context, AppController app) {
        int[] moduleIds = context.getResources().getIntArray(R.array.camera_modes);
        for (int i = 0; i < moduleIds.length; i++) {
            String moduleId = Integer.toString(moduleIds[i]);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("_preferences_module_");
            stringBuilder.append(moduleId);
            SharedPreferences oldModulePreferences = settingsManager.openPreferences(stringBuilder.toString());
            ModuleAgent agent = app.getModuleManager().getModuleAgent(moduleIds[i]);
            if (agent != null) {
                ModuleController module = agent.createModule(app);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("_preferences_module_");
                stringBuilder2.append(module.getModuleStringIdentifier());
                copyPreferences(oldModulePreferences, settingsManager.openPreferences(stringBuilder2.toString()));
            }
        }
    }

    private void upgradeSelectedModeIndex(SettingsManager settingsManager, Context context) {
        int gcamIndex = context.getResources().getInteger(R.integer.camera_mode_gcam);
        if (settingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_MODULE_LAST_USED).intValue() == 6) {
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_MODULE_LAST_USED, gcamIndex);
        }
        if (settingsManager.getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_STARTUP_MODULE_INDEX).intValue() == 6) {
            settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_STARTUP_MODULE_INDEX, gcamIndex);
        }
    }
}
