package com.hmdglobal.app.camera.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.CamcorderProfile;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import com.android.ex.camera2.portability.CameraSettings.BoostParameters;
import com.android.ex.camera2.portability.Size;
import com.android.ex.camera2.portability.debug.Log;
import com.android.ex.camera2.portability.debug.Log.Tag;
import com.android.external.CamcorderProfileEx;
import com.hmdglobal.app.camera.settings.CameraPictureSizesCacher;
import com.hmdglobal.app.camera.settings.Keys;
import com.hmdglobal.app.camera.settings.SettingsManager;
import com.hmdglobal.app.camera.settings.SettingsUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PictureSizePerso {
    private static final Tag TAG = new Tag("SizePerso");
    private static PictureSizePerso mPerso;
    private CustomUtil mCuzUtil = CustomUtil.getInstance();
    private SizeComparator mSizeComparator = new SizeComparator();

    private static class SizeComparator implements Comparator<Size> {
        private SizeComparator() {
        }

        public int compare(Size size1, Size size2) {
            if (size1.width() == size2.width()) {
                return size1.height() - size2.height();
            }
            return size1.width() - size2.width();
        }
    }

    private PictureSizePerso() {
    }

    public static PictureSizePerso getInstance() {
        if (mPerso == null) {
            mPerso = new PictureSizePerso();
        }
        return mPerso;
    }

    public void init(Context context, List<Size> supportedSizes, int cameraId, BoostParameters parameters) {
        if (supportedSizes != null) {
            Size defaultSize = true;
            if (cameraId == 0 || cameraId == 1) {
                String key_build = new StringBuilder();
                key_build.append(CameraPictureSizesCacher.PICTURE_SIZES_BUILD_KEY);
                key_build.append(cameraId);
                String thisCameraCachedBuild = PreferenceManager.getDefaultSharedPreferences(context).getString(key_build.toString(), null);
                if (parameters == null) {
                    if (thisCameraCachedBuild != null && thisCameraCachedBuild.equals(Build.DISPLAY)) {
                        return;
                    }
                } else if (thisCameraCachedBuild != null && thisCameraCachedBuild.equals(Build.DISPLAY)) {
                    SettingsManager settingsManager = parameters.settingsManager;
                    if (cameraId != 1) {
                        defaultSize = null;
                    }
                    if (supportedSizes.contains(getCachedPictureSize(settingsManager, defaultSize))) {
                        Log.i(TAG, "preference hit support list");
                        return;
                    } else {
                        Log.i(TAG, "unsupported sized detected in current preference , reset preference");
                        CameraUtil.cleanSharedPreference(context);
                    }
                }
                filterPersoUnSupportedPhotoSizes(context, supportedSizes, cameraId);
                filterPersoUnSupportedVideoSizes(context, cameraId);
            }
        }
    }

    private Size getCachedPictureSize(SettingsManager settingsManager, boolean isCameraFacingFront) {
        String pictureSizeKey;
        if (isCameraFacingFront) {
            pictureSizeKey = Keys.KEY_PICTURE_SIZE_FRONT;
        } else {
            pictureSizeKey = Keys.KEY_PICTURE_SIZE_BACK;
        }
        return SettingsUtil.sizeFromString(settingsManager.getString(SettingsManager.SCOPE_GLOBAL, pictureSizeKey, SettingsUtil.getDefaultPictureSize(isCameraFacingFront)));
    }

    public void init(Context context, List<Size> supportedSizes, int cameraId) {
        init(context, supportedSizes, cameraId, null);
    }

    public void filterPersoUnSupportedVideoSizes(Context context, int cameraId) {
        String keyQualityTitle;
        String keyQuality;
        String defaultFlag;
        int i;
        String qualityStr;
        String videoQualityKey;
        int i2 = cameraId;
        if (i2 == 0) {
            keyQualityTitle = CustomFields.DEF_VIDEO_QUALITIES_TITLE_REAR;
            keyQuality = CustomFields.DEF_VIDEO_QUALITIES_REAR;
            defaultFlag = this.mCuzUtil.getString(CustomFields.DEF_VIDEO_QUALITIES_FLAG_DEFAULT_REAR, "0");
        } else {
            keyQualityTitle = CustomFields.DEF_VIDEO_QUALITIES_TITLE_FRONT;
            keyQuality = CustomFields.DEF_VIDEO_QUALITIES_FRONT;
            defaultFlag = this.mCuzUtil.getString(CustomFields.DEF_VIDEO_QUALITIES_FLAG_DEFAULT_FRONT, "0");
        }
        String qualityTitles = this.mCuzUtil.getString(keyQualityTitle, "");
        String qualities = this.mCuzUtil.getString(keyQuality, "");
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("filterPersoUnSupportedVideoSizes ");
        stringBuilder.append(qualityTitles);
        stringBuilder.append(",  ");
        stringBuilder.append(qualities);
        Log.d(tag, stringBuilder.toString());
        String[] flatQualitiesTitles = TextUtils.split(qualityTitles, Size.DELIMITER);
        String[] flatQualities = TextUtils.split(qualities, Size.DELIMITER);
        List<String> filteredQualities = new ArrayList();
        List<String> filteredQualitiesTitles = new ArrayList();
        for (i = 0; i < flatQualities.length; i++) {
            qualityStr = flatQualities[i].trim();
            if (qualityStr == null || !qualityStr.equals(SettingsUtil.QUALITY_1080P_60FPS_NAME)) {
                int quality = CamcorderProfileEx.getQualityNum(flatQualities[i].trim());
                if (SettingsUtil.isVideoQualitySupported(quality) && CamcorderProfile.hasProfile(i2, quality)) {
                    filteredQualities.add(String.valueOf(quality));
                    filteredQualitiesTitles.add(flatQualitiesTitles[i]);
                }
            } else {
                filteredQualities.add(SettingsUtil.QUALITY_1080P_60FPS);
                filteredQualitiesTitles.add(flatQualitiesTitles[i]);
            }
        }
        if (filteredQualities.size() == 0) {
            if (i2 == 0) {
                filteredQualities.add("5");
                filteredQualitiesTitles.add("HD 720p");
            } else {
                filteredQualities.add("10");
                filteredQualitiesTitles.add("VGA");
            }
        }
        i = Integer.valueOf(defaultFlag).intValue();
        if (i >= filteredQualities.size()) {
            i = 0;
        }
        qualityStr = (String) filteredQualities.get(i);
        if (i2 == 0) {
            videoQualityKey = Keys.KEY_VIDEO_QUALITY_BACK;
        } else {
            videoQualityKey = Keys.KEY_VIDEO_QUALITY_FRONT;
        }
        String key_quality = new StringBuilder();
        key_quality.append(CameraPictureSizesCacher.VIDEO_QUALITIES_KEY);
        key_quality.append(i2);
        key_quality = key_quality.toString();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(CameraPictureSizesCacher.VIDEO_QUALITIES_TITLES_KEY);
        stringBuilder2.append(i2);
        String key_quality_titles = stringBuilder2.toString();
        Tag tag2 = TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("filterPersoUnSupportedVideoSizes filtered:");
        stringBuilder3.append(TextUtils.join(Size.DELIMITER, filteredQualities));
        stringBuilder3.append(",  ");
        stringBuilder3.append(TextUtils.join(Size.DELIMITER, filteredQualitiesTitles));
        Log.d(tag2, stringBuilder3.toString());
        SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String oldQualities = defaultPrefs.getString(key_quality, null);
        defaultFlag = TextUtils.join(Size.DELIMITER, filteredQualities);
        keyQualityTitle = defaultPrefs.getString(key_quality_titles, null);
        keyQuality = TextUtils.join(Size.DELIMITER, filteredQualitiesTitles);
        if (TextUtils.equals(oldQualities, defaultFlag) && TextUtils.equals(keyQualityTitle, keyQuality)) {
            String str = keyQualityTitle;
            return;
        }
        Editor editor = defaultPrefs.edit();
        editor.putString(key_quality, defaultFlag);
        editor.putString(key_quality_titles, keyQuality);
        editor.putString(videoQualityKey, qualityStr);
        editor.commit();
    }

    private void filterPersoUnSupportedPhotoSizes(Context context, List<Size> supportedSizes, int cameraId) {
        String keySize;
        String defaultFlag;
        String pictureSizeKey;
        List<Size> list = supportedSizes;
        int i = cameraId;
        if (i == 0) {
            if (CustomUtil.getInstance().isPanther()) {
                keySize = CustomFields.DEF_PANTHER_PICTURE_SIZE_REAR;
                defaultFlag = this.mCuzUtil.getString(CustomFields.PREF_PANTHER_PICTURE_SIZE_DEF_REAR, "0");
            } else {
                keySize = CustomFields.DEF_DEADPOOL_PICTURE_SIZE_REAR;
                defaultFlag = this.mCuzUtil.getString(CustomFields.PREF_DEADPOOL_PICTURE_SIZE_DEF_REAR, "0");
            }
        } else if (CustomUtil.getInstance().isPanther()) {
            keySize = CustomFields.DEF_PANTHER_PICTURE_SIZE_FRONT;
            defaultFlag = this.mCuzUtil.getString(CustomFields.PREF_PANTHER_PICTURE_SIZE_DEF_FRONT, "0");
        } else {
            keySize = CustomFields.DEF_DEADPOOL_PICTURE_SIZE_FRONT;
            defaultFlag = this.mCuzUtil.getString(CustomFields.PREF_DEADPOOL_PICTURE_SIZE_DEF_FRONT, "0");
        }
        Collections.sort(list, this.mSizeComparator);
        String sizes = this.mCuzUtil.getString(keySize, "");
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("filterPersoUnSupportedPhotoSizes ,  ");
        stringBuilder.append(sizes);
        stringBuilder.append(", ");
        stringBuilder.append(Size.listToString(supportedSizes));
        Log.i(tag, stringBuilder.toString());
        String[] flatSizes = TextUtils.split(sizes, Size.DELIMITER);
        List<Size> filteredSizes = new ArrayList();
        for (String size : flatSizes) {
            String size2 = size2.trim();
            if (size2.split("x").length == 2) {
                Size desiredSize = SettingsUtil.sizeFromString(size2);
                if (list.contains(desiredSize)) {
                    filteredSizes.add(desiredSize);
                }
            }
        }
        if (filteredSizes.size() == 0) {
            filteredSizes.add((Size) list.get(supportedSizes.size() - 1));
            if (supportedSizes.size() > 1) {
                filteredSizes.add((Size) list.get(supportedSizes.size() - 2));
            }
        }
        int i2 = Integer.valueOf(defaultFlag).intValue();
        if (i2 >= filteredSizes.size()) {
            i2 = 0;
        }
        String pictureSize = SettingsUtil.sizeToSetting((Size) filteredSizes.get(i2));
        if (i == 0) {
            pictureSizeKey = Keys.KEY_PICTURE_SIZE_BACK;
        } else {
            pictureSizeKey = Keys.KEY_PICTURE_SIZE_FRONT;
        }
        String key_build = new StringBuilder();
        key_build.append(CameraPictureSizesCacher.PICTURE_SIZES_BUILD_KEY);
        key_build.append(i);
        key_build = key_build.toString();
        String key_sizes = new StringBuilder();
        key_sizes.append(CameraPictureSizesCacher.PICTURE_SIZES_SIZES_KEY);
        key_sizes.append(i);
        key_sizes = key_sizes.toString();
        SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        Tag tag2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("filterPersoUnSupportedPhotoSizes filteredsize:");
        stringBuilder2.append(Size.listToString(filteredSizes));
        Log.d(tag2, stringBuilder2.toString());
        Editor editor = defaultPrefs.edit();
        editor.putString(key_build, Build.DISPLAY);
        String oldSizes = defaultPrefs.getString(key_sizes, null);
        String newSizes = Size.listToString(filteredSizes);
        if (!TextUtils.equals(oldSizes, newSizes)) {
            editor.putString(key_sizes, newSizes);
            editor.putString(pictureSizeKey, pictureSize);
        }
        editor.commit();
    }
}
