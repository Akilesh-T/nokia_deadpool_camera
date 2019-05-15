package com.hmdglobal.app.camera.settings;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.FragmentManager.OnBackStackChangedListener;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toolbar;
import com.android.ex.camera2.portability.CameraAgentFactory;
import com.android.ex.camera2.portability.CameraAgentFactory.CameraApi;
import com.android.ex.camera2.portability.CameraDeviceInfo;
import com.android.ex.camera2.portability.Size;
import com.hmdglobal.app.camera.CameraActivity;
import com.hmdglobal.app.camera.GuideActivity;
import com.hmdglobal.app.camera.ManualModule;
import com.hmdglobal.app.camera.NormalPhotoModule;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.Storage;
import com.hmdglobal.app.camera.app.CameraApp;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.settings.SettingsUtil.SelectedPictureSizes;
import com.hmdglobal.app.camera.util.ApiHelper;
import com.hmdglobal.app.camera.util.CameraSettingsActivityHelper;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.hmdglobal.app.camera.util.CustomFields;
import com.hmdglobal.app.camera.util.CustomUtil;
import com.hmdglobal.app.camera.util.PermissionsUtil;
import com.hmdglobal.app.camera.util.PermissionsUtil.RequestingPerms;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CameraSettingsActivity extends Activity implements OnBackStackChangedListener {
    public static final String PREF_SCREEN_EXTRA = "pref_screen_extra";
    private static final Tag TAG = new Tag("CameraSettingsActivity");
    private static boolean mSecureCamera = false;
    private CameraSettingsFragment dialog;
    private final BroadcastReceiver mMediaActionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (CameraSettingsActivity.this.dialog != null && action.equals("android.intent.action.MEDIA_EJECT")) {
                CameraSettingsActivity.this.dialog.refreshStorageSettings(false);
            } else if (CameraSettingsActivity.this.dialog != null && action.equals("android.intent.action.MEDIA_MOUNTED")) {
                CameraSettingsActivity.this.dialog.refreshStorageSettings(true);
            }
        }
    };
    private final BroadcastReceiver mShutdownReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            CameraSettingsActivity.this.finish();
        }
    };

    public static class CameraSettingsFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener, OnClickListener {
        public static final String PREF_CATEGORY_ADVANCED = "pref_category_advanced";
        public static final String PREF_CATEGORY_GENERAL = "pref_group_general";
        public static final String PREF_CATEGORY_PHOTO = "pref_group_photo_key";
        public static final String PREF_CATEGORY_RESOLUTION = "pref_category_resolution";
        public static final String PREF_CATEGORY_VIDEO = "pref_group_video_key";
        public static final String PREF_LAUNCH_HELP = "pref_launch_help";
        public static final String[] PREF_SETTINGS = new String[]{Keys.KEY_CAMERA_AUTO_HDR, Keys.KEY_CAMERA_RAW_FILE, Keys.KEY_CAMERA_SHUTTER_CONTROLL, Keys.KEY_SOUND, Keys.KEY_CAMERA_GRID_LINES, Keys.KEY_CAMERA_WATER_MARK, Keys.KEY_CAMERA_MIRROR_SELFIE, Keys.KEY_CAMERA_DEPTH, Keys.KEY_RECORD_LOCATION, Keys.KEY_CAMERA_SAVEPATH, Keys.KEY_CAMERA_PHOTO_RESOLUTION, Keys.KEY_CAMERA_VIDEO_RESOLUTION, Keys.KEY_RESTORE_SETTING};
        private static final Tag TAG = new Tag("SettingsFragment");
        private static DecimalFormat sMegaPixelFormat = new DecimalFormat("##0.0");
        private int cameraId;
        private boolean hideShowSummary = true;
        private boolean isFacingBack;
        private String[] mCamcorderProfileNames;
        private boolean mGetSubPrefAsRoot = true;
        private CameraDeviceInfo mInfos;
        private ManagedSwitchPreference mLowLightPreference;
        private SelectedPictureSizes mOldPictureSizesBack;
        private SelectedPictureSizes mOldPictureSizesFront;
        private List<Size> mPictureSizesBack;
        private List<Size> mPictureSizesFront;
        private String mPrefKey;
        private boolean mShowAntiBand = true;
        private boolean mShowAttentionseeker = true;
        private boolean mShowCameraGridLines = true;
        private boolean mShowCameraPicturesizeBack = true;
        private boolean mShowCameraPicturesizeFront = true;
        private boolean mShowCameraSavepath = true;
        private boolean mShowGestureShot = true;
        private boolean mShowMirrorSelfie = true;
        private String[] mVideoQualitiesBack;
        private String[] mVideoQualitiesFront;
        private String[] mVideoQualityTitlesBack;
        private String[] mVideoQualityTitlesFront;
        private ArrayList<String> mVisidonEntries;
        private ArrayList<String> mVisidonEntriesValue;
        private DialogInterface mWarnResetToFactory;
        private String moduleScope;

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
            Bundle arguments = getArguments();
            if (arguments != null) {
                this.moduleScope = arguments.getString(Keys.SOURCE_MODULE_SCOPE);
                this.cameraId = arguments.getInt(Keys.SOURCE_CAMERA_ID);
            }
            Context context = getActivity().getApplicationContext();
            addPreferencesFromResource(R.xml.camera_preferences);
            if (!CustomUtil.getInstance().isPanther()) {
                ((PreferenceGroup) findPreference(PREF_CATEGORY_PHOTO)).removePreference(findPreference(Keys.KEY_CAMERA_DEPTH));
                ((PreferenceGroup) findPreference(PREF_CATEGORY_PHOTO)).removePreference(findPreference(Keys.KEY_CAMERA_RAW_FILE));
            }
            ((PreferenceGroup) findPreference(PREF_CATEGORY_PHOTO)).removePreference(findPreference(Keys.KEY_CAMERA_ENABLE_ALGORITHM));
            ((PreferenceGroup) findPreference(PREF_CATEGORY_PHOTO)).removePreference(findPreference(Keys.KEY_CAMERA_LOW_LIGHT));
            this.mLowLightPreference = (ManagedSwitchPreference) findPreference(Keys.KEY_CAMERA_LOW_LIGHT);
            this.mGetSubPrefAsRoot = false;
            CameraSettingsActivityHelper.addAdditionalPreferences(this, context);
            this.mGetSubPrefAsRoot = true;
            this.mCamcorderProfileNames = getResources().getStringArray(R.array.camcorder_profile_names);
            this.mInfos = CameraAgentFactory.getAndroidCameraAgent(context, CameraApi.API_1).getCameraDeviceInfo();
        }

        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            super.onCreateOptionsMenu(menu, inflater);
        }

        public void onResume() {
            super.onResume();
            Activity activity = getActivity();
            getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
            setCameraPreference();
            if (!CameraUtil.checkGpsEnable(activity)) {
                setRememberLocation(false);
            }
        }

        private void setCameraPreference() {
            setVisibilities();
            fillEntriesAndSummaries();
            ListPreference savePath = (ListPreference) getPreferenceScreen().findPreference(Keys.KEY_CAMERA_SAVEPATH);
            if (!Storage.isSDCardAvailable() || savePath == null) {
                Preference preference = findPreference(Keys.KEY_CAMERA_SAVEPATH);
                if (preference != null) {
                    ((PreferenceGroup) findPreference("pref_group_general_key")).removePreference(preference);
                }
            } else {
                savePath.setEnabled(true);
                savePath.setValue(Storage.getSavePath());
                if (savePath.getValue().equals("1") && Storage.isSDCardAvailable()) {
                    Storage.setSavePath("1");
                }
            }
            if (!PermissionsUtil.isPermissionGranted(getActivity(), PermissionsUtil.PERMS_ACCESS_COARSE_LOCATION) || !PermissionsUtil.isPermissionGranted(getActivity(), PermissionsUtil.PERMS_ACCESS_FINE_LOCATION)) {
                setRememberLocation(false);
            }
        }

        public PreferenceScreen getPreferenceScreen() {
            PreferenceScreen root = super.getPreferenceScreen();
            if (!this.mGetSubPrefAsRoot || this.mPrefKey == null || root == null) {
                return root;
            }
            PreferenceScreen match = findByKey(root, this.mPrefKey);
            if (match != null) {
                return match;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("key ");
            stringBuilder.append(this.mPrefKey);
            stringBuilder.append(" not found");
            throw new RuntimeException(stringBuilder.toString());
        }

        private PreferenceScreen findByKey(PreferenceScreen parent, String key) {
            if (key.equals(parent.getKey())) {
                return parent;
            }
            for (int i = 0; i < parent.getPreferenceCount(); i++) {
                Preference child = parent.getPreference(i);
                if (child instanceof PreferenceScreen) {
                    PreferenceScreen match = findByKey((PreferenceScreen) child, key);
                    if (match != null) {
                        return match;
                    }
                }
            }
            return null;
        }

        private void refreshSummary() {
            PreferenceScreen root = getPreferenceScreen();
            for (int i = 0; i < PREF_SETTINGS.length; i++) {
                Preference rence = root.findPreference(PREF_SETTINGS[i]);
                if (rence != null) {
                    if (this.hideShowSummary) {
                        if (!Keys.KEY_CAMERA_SAVEPATH.equals(rence.getKey())) {
                            rence.setSummary("");
                        }
                    } else if (!Keys.KEY_CAMERA_SAVEPATH.equals(rence.getKey())) {
                        rence.setSummary(getResources().getStringArray(R.array.setting_summaries)[i]);
                    }
                }
            }
        }

        private void setVisibilities() {
            PreferenceGroup videoGroup = (PreferenceGroup) findPreference(PREF_CATEGORY_VIDEO);
            PreferenceGroup generalGroup = (PreferenceGroup) findPreference(PREF_CATEGORY_GENERAL);
            PreferenceScreen root = getPreferenceScreen();
            if (!(this.moduleScope.endsWith(NormalPhotoModule.AUTO_MODULE_STRING_ID) || this.moduleScope.endsWith(ManualModule.MANUAL_MODULE_STRING_ID))) {
                this.mShowCameraPicturesizeBack = false;
                this.mShowCameraPicturesizeFront = false;
            }
            if (!(this.moduleScope.endsWith(ManualModule.MANUAL_MODULE_STRING_ID) || this.moduleScope.endsWith(NormalPhotoModule.AUTO_MODULE_STRING_ID))) {
                this.mShowCameraGridLines = false;
            }
            if (!this.moduleScope.endsWith(NormalPhotoModule.AUTO_MODULE_STRING_ID) || !CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_ATTENTION_SEEKER, false)) {
                this.mShowAttentionseeker = false;
            } else if (!this.isFacingBack) {
                this.mShowAttentionseeker = false;
            }
            this.mShowGestureShot = CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_GESTURE_SHOT, false);
            if (this.mShowGestureShot) {
                if (!this.moduleScope.endsWith(NormalPhotoModule.AUTO_MODULE_STRING_ID)) {
                    this.mShowGestureShot = false;
                } else if (this.isFacingBack) {
                    this.mShowGestureShot = false;
                }
            }
            this.mShowAntiBand = CustomUtil.getInstance().getBoolean(CustomFields.DEF_ANTIBAND_MENU_VISIBLE, false);
            this.mShowAntiBand = false;
            if (!this.mShowCameraSavepath) {
                recursiveDelete(generalGroup, findPreference(Keys.KEY_CAMERA_SAVEPATH));
            }
            this.mShowMirrorSelfie = CustomUtil.getInstance().getBoolean(CustomFields.DEF_CAMERA_SUPPORT_MIRROR_SELFIE, false);
            if (!this.moduleScope.endsWith(NormalPhotoModule.AUTO_MODULE_STRING_ID)) {
                this.mShowMirrorSelfie = false;
            } else if (this.isFacingBack) {
                this.mShowMirrorSelfie = false;
            }
            if (!this.mShowAntiBand) {
                recursiveDelete(generalGroup, findPreference(Keys.KEY_ANTIBANDING));
            }
            if (videoGroup != null && videoGroup.getPreferenceCount() < 1) {
                recursiveDelete(root, videoGroup);
            }
            if (generalGroup != null && generalGroup.getPreferenceCount() < 1) {
                recursiveDelete(root, generalGroup);
            }
        }

        private void fillEntriesAndSummaries(PreferenceGroup group) {
            for (int i = 0; i < group.getPreferenceCount(); i++) {
                Preference pref = group.getPreference(i);
                if (pref instanceof PreferenceGroup) {
                    fillEntriesAndSummaries((PreferenceGroup) pref);
                }
                setSummary(pref);
            }
        }

        private void fillEntriesAndSummaries() {
            Preference pref = getPreferenceScreen().findPreference(Keys.KEY_PICTURE_SIZE_FRONT);
            pref = getPreferenceScreen().findPreference(Keys.KEY_CAMERA_SAVEPATH);
            if (pref != null) {
                setSummary(pref);
            }
            pref = getPreferenceScreen().findPreference(Keys.KEY_ANTIBANDING);
            if (pref != null) {
                setSummary(pref);
            }
        }

        private boolean recursiveDelete(PreferenceGroup group, Preference preference) {
            if (group == null) {
                Log.d(TAG, "attempting to delete from null preference group");
                return false;
            } else if (preference == null) {
                Log.d(TAG, "attempting to delete null preference");
                return false;
            } else if (group.removePreference(preference)) {
                return true;
            } else {
                for (int i = 0; i < group.getPreferenceCount(); i++) {
                    Preference pref = group.getPreference(i);
                    if ((pref instanceof PreferenceGroup) && recursiveDelete((PreferenceGroup) pref, preference)) {
                        return true;
                    }
                }
                return false;
            }
        }

        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            setSummary(findPreference(key));
        }

        public void refreshStorageSettings(boolean mounted) {
            ListPreference preferenceSavePath = (ListPreference) getPreferenceScreen().findPreference(Keys.KEY_CAMERA_SAVEPATH);
            if (preferenceSavePath != null) {
                if (preferenceSavePath.getDialog() != null && preferenceSavePath.getDialog().isShowing()) {
                    preferenceSavePath.getDialog().dismiss();
                }
                if (mounted) {
                    preferenceSavePath.setEnabled(true);
                } else {
                    preferenceSavePath.setEnabled(false);
                }
                if (!preferenceSavePath.getValue().equals("1")) {
                    return;
                }
                if (Storage.isSDCardAvailable()) {
                    Storage.setSavePath("1");
                    return;
                }
                preferenceSavePath.setValue("0");
                preferenceSavePath.setSummary(preferenceSavePath.getEntry());
                Storage.setSavePath("0");
            }
        }

        private void setSummary(Preference preference) {
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                listPreference.setSummary(listPreference.getEntry());
            }
        }

        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            boolean z = true;
            if (Keys.KEY_RESTORE_SETTING.equals(preference.getKey())) {
                warnResetToFactory();
                return true;
            }
            ManagedSwitchPreference raw_preference;
            if (Keys.KEY_CAMERA_ENABLE_ALGORITHM.equals(preference.getKey())) {
                if (((ManagedSwitchPreference) preference).getPersistedBoolean(true)) {
                    this.mLowLightPreference.setEnabled(true);
                } else {
                    this.mLowLightPreference.setChecked(false);
                    this.mLowLightPreference.setEnabled(false);
                }
            }
            if (Keys.KEY_CAMERA_DEPTH.equals(preference.getKey())) {
                raw_preference = (ManagedSwitchPreference) findPreference(Keys.KEY_CAMERA_RAW_FILE);
                if (((ManagedSwitchPreference) preference).getPersistedBoolean(true)) {
                    raw_preference.setChecked(false);
                }
            }
            if (Keys.KEY_CAMERA_RAW_FILE.equals(preference.getKey())) {
                raw_preference = (ManagedSwitchPreference) findPreference(Keys.KEY_CAMERA_DEPTH);
                if (((ManagedSwitchPreference) preference).getPersistedBoolean(true) && raw_preference != null) {
                    raw_preference.setChecked(false);
                }
            }
            CameraResolutionFragment dialog;
            FragmentTransaction transaction;
            if (Keys.KEY_CAMERA_PHOTO_RESOLUTION.equals(preference.getKey())) {
                getActivity().getActionBar().setTitle(R.string.pref_resolution_title);
                dialog = new CameraResolutionFragment();
                transaction = getFragmentManager().beginTransaction();
                dialog.setResolutionByType(1);
                if (ApiHelper.isLOrHigher()) {
                    transaction.replace(R.id.camera_settings, dialog);
                } else {
                    transaction.replace(16908290, dialog);
                }
                transaction.addToBackStack(Keys.BACK_STACK_PREFS);
                transaction.commit();
                getFragmentManager().executePendingTransactions();
                return true;
            } else if (Keys.KEY_CAMERA_VIDEO_RESOLUTION.equals(preference.getKey())) {
                getActivity().getActionBar().setTitle(R.string.pref_resolution_video_title);
                dialog = new CameraResolutionFragment();
                transaction = getFragmentManager().beginTransaction();
                dialog.setResolutionByType(2);
                if (ApiHelper.isLOrHigher()) {
                    transaction.replace(R.id.camera_settings, dialog);
                } else {
                    transaction.replace(16908290, dialog);
                }
                transaction.addToBackStack(Keys.BACK_STACK_PREFS);
                transaction.commit();
                getFragmentManager().executePendingTransactions();
                return true;
            } else {
                if (Keys.KEY_RECORD_LOCATION.equals(preference.getKey())) {
                    if (!(PermissionsUtil.isPermissionGranted(getActivity(), PermissionsUtil.PERMS_ACCESS_COARSE_LOCATION) && PermissionsUtil.isPermissionGranted(getActivity(), PermissionsUtil.PERMS_ACCESS_FINE_LOCATION))) {
                        z = false;
                    }
                    if (z) {
                        onGpsSaving();
                    } else {
                        if (CameraSettingsActivity.mSecureCamera) {
                            PermissionsUtil.showSnackBar(getActivity(), getView(), R.string.location_snack_setting, R.string.grant_access_settings);
                        } else {
                            PermissionsUtil.requestPermissions(getActivity(), 8, PermissionsUtil.PERMS_ACCESS_COARSE_LOCATION, PermissionsUtil.PERMS_ACCESS_FINE_LOCATION);
                        }
                        setRememberLocation(false);
                    }
                }
                return super.onPreferenceTreeClick(preferenceScreen, preference);
            }
        }

        private void setRememberLocation(boolean remember) {
            ((ManagedSwitchPreference) getPreferenceScreen().findPreference(Keys.KEY_RECORD_LOCATION)).setChecked(remember);
            SettingsManager settingsManager = ((CameraApp) getActivity().getApplication()).getSettingsManager();
            if (settingsManager.getBoolean(SettingsManager.SCOPE_GLOBAL, Keys.KEY_RECORD_LOCATION) != remember) {
                settingsManager.set(SettingsManager.SCOPE_GLOBAL, Keys.KEY_RECORD_LOCATION, remember);
            }
        }

        public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
            if (grantResults != null && permissions != null) {
                if (PermissionsUtil.inRequesting()) {
                    RequestingPerms request = PermissionsUtil.getRequestingPerms();
                    if (permissions.length == 0 || grantResults.length == 0) {
                        if (!(request.code == 0 || request.perms == null)) {
                            PermissionsUtil.requestPermissions(getActivity(), request.code, request.perms);
                        }
                        return;
                    }
                    PermissionsUtil.getRequestingPerms().clear();
                }
                if (PermissionsUtil.DEBUG) {
                    for (int i = 0; i < permissions.length; i++) {
                        Tag tag = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(i);
                        stringBuilder.append("For permission ");
                        stringBuilder.append(permissions[i]);
                        stringBuilder.append(",  grantResults is ");
                        stringBuilder.append(grantResults[i]);
                        Log.i(tag, stringBuilder.toString());
                    }
                }
                if (requestCode == 8) {
                    if (grantResults.length == 2 && grantResults[0] == 0 && grantResults[1] == 0) {
                        setRememberLocation(true);
                        onGpsSaving();
                    } else if (-1 == grantResults[0] || -1 == grantResults[1]) {
                        PermissionsUtil.showSnackBar(getActivity(), getView(), R.string.location_snack_setting, R.string.grant_access_settings);
                    }
                }
            }
        }

        public void onClick(DialogInterface dialog, int which) {
            SettingsManager settingsManager = ((CameraApp) getActivity().getApplication()).getSettingsManager();
            if (dialog == this.mWarnResetToFactory) {
                if (which == -1) {
                    restoreDefaultPreferences();
                }
            }
        }

        public void onDestroy() {
            super.onDestroy();
            if (this.mWarnResetToFactory != null) {
                this.mWarnResetToFactory.dismiss();
            }
            CameraAgentFactory.recycle(CameraApi.API_1);
        }

        private void warnResetToFactory() {
            AlertDialog dialog = new Builder(getActivity()).setMessage(getResources().getString(R.string.restore_settings_dialog_msg)).setPositiveButton(R.string.restore_settings_dialog_yes, this).setNegativeButton(R.string.restore_settings_dialog_no, this).show();
            dialog.getButton(-1).setTextColor(getActivity().getResources().getColor(R.color.dialog_button_font_color));
            dialog.getButton(-2).setTextColor(getActivity().getResources().getColor(R.color.dialog_button_font_color));
            this.mWarnResetToFactory = dialog;
        }

        private void restoreDefaultPreferences() {
            getActivity().sendBroadcast(new Intent(CameraActivity.EXIT_CAMERA_ACTION));
            CameraUtil.cleanSharedPreference(getActivity());
            startActivity(new Intent(getActivity(), GuideActivity.class));
            getActivity().finish();
        }

        private void onGpsSaving() {
            if (CameraUtil.gotoGpsSetting(getActivity(), ((CameraApp) getActivity().getApplication()).getSettingsManager(), R.drawable.gps_white)) {
                setRememberLocation(false);
            }
        }

        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == 1000) {
                if (CameraUtil.backFromGpsSetting(getActivity(), ((CameraApp) getActivity().getApplication()).getSettingsManager())) {
                    setRememberLocation(true);
                }
            }
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ApiHelper.isLOrHigher()) {
            setContentView(R.layout.camera_settings_layout);
            Toolbar mBar = (Toolbar) findViewById(R.id.toolbar);
            mBar.setBackgroundColor(getResources().getColor(R.color.camera_settings_toolbar_color));
            setActionBar(mBar);
        }
        setActionTitle();
        if (getIntent().getBooleanExtra("secure_camera", false)) {
            mSecureCamera = true;
        }
        if (mSecureCamera) {
            getWindow().addFlags(524288);
            registerReceiver(this.mShutdownReceiver, new IntentFilter("android.intent.action.SCREEN_OFF"));
            registerReceiver(this.mShutdownReceiver, new IntentFilter("android.intent.action.USER_PRESENT"));
        }
        String moduleScope = getIntent().getStringExtra(Keys.SOURCE_MODULE_SCOPE);
        int cameraId = getIntent().getIntExtra(Keys.SOURCE_CAMERA_ID, 0);
        if (cameraId == -1) {
            cameraId = ((CameraApp) getApplication()).getSettingsManager().getInteger(SettingsManager.SCOPE_GLOBAL, Keys.KEY_CAMERA_ID).intValue();
        }
        getFragmentManager().addOnBackStackChangedListener(this);
        this.dialog = new CameraSettingsFragment();
        Bundle bundle = new Bundle(2);
        bundle.putString(Keys.SOURCE_MODULE_SCOPE, moduleScope);
        bundle.putInt(Keys.SOURCE_CAMERA_ID, cameraId);
        this.dialog.setArguments(bundle);
        if (ApiHelper.isLOrHigher()) {
            getFragmentManager().beginTransaction().replace(R.id.camera_settings, this.dialog).commit();
        } else {
            getFragmentManager().beginTransaction().replace(16908290, this.dialog).commit();
        }
        IntentFilter filter_media_action = new IntentFilter("android.intent.action.MEDIA_EJECT");
        filter_media_action.addAction("android.intent.action.MEDIA_MOUNTED");
        filter_media_action.addDataScheme("file");
        registerReceiver(this.mMediaActionReceiver, filter_media_action);
    }

    /* Access modifiers changed, original: protected */
    public void onDestroy() {
        super.onDestroy();
        if (mSecureCamera) {
            unregisterReceiver(this.mShutdownReceiver);
        }
        unregisterReceiver(this.mMediaActionReceiver);
    }

    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (item.getItemId() == R.id.show_hide_summary) {
            if (this.dialog.hideShowSummary) {
                this.dialog.hideShowSummary = false;
            } else {
                this.dialog.hideShowSummary = true;
            }
            this.dialog.refreshSummary();
            return true;
        }
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            finish();
        } else {
            setActionTitle();
            setTitleFromBackStack();
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private void setTitleFromBackStack() {
        getFragmentManager().popBackStackImmediate();
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        this.dialog.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /* Access modifiers changed, original: protected */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        this.dialog.onActivityResult(requestCode, resultCode, data);
    }

    public void onBackStackChanged() {
        setActionTitle();
    }

    private void setActionTitle() {
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            ActionBar actionBar = getActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.mode_settings);
        }
    }
}
