package com.hmdglobal.app.camera.settings;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import com.android.ex.camera2.portability.CameraAgentFactory;
import com.android.ex.camera2.portability.CameraAgentFactory.CameraApi;
import com.android.ex.camera2.portability.CameraDeviceInfo;
import com.android.ex.camera2.portability.Size;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.settings.SettingsUtil.SelectedPictureSizes;
import com.hmdglobal.app.camera.util.CameraSettingsActivityHelper;
import java.util.List;

public class CameraResolutionFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
    private boolean mGetSubPrefAsRoot = true;
    private CameraDeviceInfo mInfos;
    private SelectedPictureSizes mOldPictureSizesBack;
    private SelectedPictureSizes mOldPictureSizesFront;
    private List<Size> mPictureSizesBack;
    private List<Size> mPictureSizesFront;
    private String mPrefKey;
    private int mResolutionType;
    private String[] mVideoQualitiesBack;
    private String[] mVideoQualitiesFront;
    private String[] mVideoQualityTitlesBack;
    private String[] mVideoQualityTitlesFront;

    public void setResolutionByType(int id) {
        this.mResolutionType = id;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getActivity().getApplicationContext();
        addPreferencesFromResource(R.xml.camera_photo_resolution);
        this.mGetSubPrefAsRoot = false;
        CameraSettingsActivityHelper.addAdditionalPreferences(this, context);
        this.mGetSubPrefAsRoot = true;
        this.mInfos = CameraAgentFactory.getAndroidCameraAgent(context, CameraApi.API_1).getCameraDeviceInfo();
        Activity activity = getActivity();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        loadSizes();
        setVisibleSummary();
    }

    public void onResume() {
        super.onResume();
    }

    private void setVisibleSummary() {
        if (this.mResolutionType == 1) {
            recursiveDelete(Keys.KEY_VIDEO_QUALITY_FRONT);
            recursiveDelete(Keys.KEY_VIDEO_QUALITY_BACK);
            fillEntriesAndSummaries(getPreferenceScreen().findPreference(Keys.KEY_PICTURE_SIZE_FRONT));
            fillEntriesAndSummaries(getPreferenceScreen().findPreference(Keys.KEY_PICTURE_SIZE_BACK));
            return;
        }
        recursiveDelete(Keys.KEY_PICTURE_SIZE_FRONT);
        recursiveDelete(Keys.KEY_PICTURE_SIZE_BACK);
        fillEntriesAndSummaries(getPreferenceScreen().findPreference(Keys.KEY_VIDEO_QUALITY_FRONT));
        fillEntriesAndSummaries(getPreferenceScreen().findPreference(Keys.KEY_VIDEO_QUALITY_BACK));
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

    private void setEntriesForSelection(List<Size> selectedSizes, ListPreference preference) {
        if (selectedSizes != null) {
            String[] entries = new String[selectedSizes.size()];
            String[] entryValues = new String[selectedSizes.size()];
            for (int i = 0; i < selectedSizes.size(); i++) {
                Size size = (Size) selectedSizes.get(i);
                entries[i] = getSizeSummaryString(size);
                entryValues[i] = SettingsUtil.sizeToSetting(size);
            }
            preference.setEntries(entries);
            preference.setEntryValues(entryValues);
        }
    }

    private void setEntriesForSelection(String[] qualities, String[] titles, ListPreference preference) {
        preference.setEntries(titles);
        preference.setEntryValues(qualities);
    }

    private void loadSizes() {
        if (this.mInfos != null) {
            List<Size> rearPictureSize = CameraPictureSizesCacher.getSizesForCamera(0, getActivity().getApplicationContext());
            List<Size> frontPictureSize = CameraPictureSizesCacher.getSizesForCamera(1, getActivity().getApplicationContext());
            if (this.mResolutionType == 1) {
                if (rearPictureSize != null) {
                    this.mOldPictureSizesBack = SettingsUtil.getSelectedCameraPictureSizes(rearPictureSize, 0);
                    this.mPictureSizesBack = rearPictureSize;
                }
                if (frontPictureSize != null) {
                    this.mOldPictureSizesFront = SettingsUtil.getSelectedCameraPictureSizes(frontPictureSize, 1);
                    this.mPictureSizesFront = frontPictureSize;
                }
            } else {
                this.mVideoQualitiesBack = CameraPictureSizesCacher.getQualitiesForCamera(0, getActivity().getApplicationContext());
                this.mVideoQualityTitlesBack = CameraPictureSizesCacher.getQualityTitlesForCamera(0, getActivity().getApplicationContext());
                this.mVideoQualitiesFront = CameraPictureSizesCacher.getQualitiesForCamera(1, getActivity().getApplicationContext());
                this.mVideoQualityTitlesFront = CameraPictureSizesCacher.getQualityTitlesForCamera(1, getActivity().getApplicationContext());
            }
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (getActivity() != null) {
            setSummary(findPreference(key));
        }
    }

    private void setSummary(Preference preference) {
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            if (listPreference.getKey().equals(Keys.KEY_PICTURE_SIZE_BACK)) {
                setSummaryForSelection(this.mOldPictureSizesBack, this.mPictureSizesBack, listPreference);
            } else if (listPreference.getKey().equals(Keys.KEY_PICTURE_SIZE_FRONT)) {
                setSummaryForSelection(this.mOldPictureSizesFront, this.mPictureSizesFront, listPreference);
            } else if (listPreference.getKey().equals(Keys.KEY_VIDEO_QUALITY_BACK)) {
                setSummaryForSelection(this.mVideoQualityTitlesBack, listPreference);
            } else if (listPreference.getKey().equals(Keys.KEY_VIDEO_QUALITY_FRONT)) {
                setSummaryForSelection(this.mVideoQualityTitlesFront, listPreference);
            } else {
                listPreference.setSummary(listPreference.getEntry());
            }
        }
    }

    private void recursiveDelete(String key) {
        getPreferenceScreen().removePreference(getPreferenceScreen().findPreference(key));
    }

    private boolean recursiveDelete(PreferenceGroup group, Preference preference) {
        if (group == null || preference == null) {
            return false;
        }
        if (group.removePreference(preference)) {
            return true;
        }
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference pref = group.getPreference(i);
            if ((pref instanceof PreferenceGroup) && recursiveDelete((PreferenceGroup) pref, preference)) {
                return true;
            }
        }
        return false;
    }

    private void setSummaryForSelection(SelectedPictureSizes oldPictureSizes, List<Size> displayableSizes, ListPreference preference) {
        if (oldPictureSizes != null) {
            preference.setSummary(getSizeSummaryString(oldPictureSizes.getFromSetting(preference.getValue(), displayableSizes)));
        }
    }

    private void setSummaryForSelection(String[] titles, List<Size> displayableSizes, ListPreference preference) {
        preference.setSummary(titles[displayableSizes.indexOf(SettingsUtil.sizeFromString(preference.getValue()))]);
    }

    private void setSummaryForSelection(String[] titles, ListPreference preference) {
        if (titles != null) {
            int index = preference.findIndexOfValue(preference.getValue());
            if (index > titles.length) {
                index = titles.length - 1;
            } else if (index < 0) {
                return;
            }
            preference.setSummary(titles[index]);
        }
    }

    private void fillEntriesAndSummaries(Preference pref) {
        setEntries(pref);
        setSummary(pref);
    }

    private String getSizeSummaryString(Size size) {
        Size approximateSize = ResolutionUtil.getApproximateSize(size);
        long megaPixels = Math.round(((double) (size.width() * size.height())) / 4696837146684686336L);
        int numerator = ResolutionUtil.aspectRatioNumerator(approximateSize);
        int denominator = ResolutionUtil.aspectRatioDenominator(approximateSize);
        if (size.width() == 4160 && size.height() == 1970) {
            denominator = 9;
            numerator = 18;
        }
        if (size.width() == 3264 && size.height() == 1546) {
            denominator = 9;
            numerator = 18;
        }
        if (size.width() == 2592 && size.height() == 1296) {
            denominator = 9;
            numerator = 18;
        }
        if (numerator == 4) {
            return getResources().getString(R.string.aspect_ratio_and_megapixels_default_new_two, new Object[]{Long.valueOf(megaPixels), Integer.valueOf(size.width()), Integer.valueOf(size.height()), Integer.valueOf(numerator), Integer.valueOf(denominator)});
        }
        return getResources().getString(R.string.aspect_ratio_and_megapixels_default_new, new Object[]{Long.valueOf(megaPixels), Integer.valueOf(size.width()), Integer.valueOf(size.height()), Integer.valueOf(numerator), Integer.valueOf(denominator)});
    }

    private void setEntries(Preference preference) {
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            if (listPreference.getKey().equals(Keys.KEY_PICTURE_SIZE_BACK)) {
                setEntriesForSelection(this.mPictureSizesBack, listPreference);
            } else if (listPreference.getKey().equals(Keys.KEY_PICTURE_SIZE_FRONT)) {
                setEntriesForSelection(this.mPictureSizesFront, listPreference);
            } else if (listPreference.getKey().equals(Keys.KEY_VIDEO_QUALITY_BACK)) {
                setEntriesForSelection(this.mVideoQualitiesBack, this.mVideoQualityTitlesBack, listPreference);
            } else if (listPreference.getKey().equals(Keys.KEY_VIDEO_QUALITY_FRONT)) {
                setEntriesForSelection(this.mVideoQualitiesFront, this.mVideoQualityTitlesFront, listPreference);
            }
        }
    }
}
