package com.hmdglobal.app.camera.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Camera;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import com.android.ex.camera2.portability.Size;
import java.util.List;

public class CameraPictureSizesCacher {
    public static final String PICTURE_SIZES_BUILD_KEY = "CachedSupportedPictureSizes_Build_Camera";
    public static final String PICTURE_SIZES_SIZES_KEY = "CachedSupportedPictureSizes_Sizes_Camera";
    public static final String VIDEO_QUALITIES_KEY = "CachedSupportedVideoQualities_Camera";
    public static final String VIDEO_QUALITIES_TITLES_KEY = "CachedSupportedVideoQualities_Titles_Camera";

    public static void updateSizesForCamera(Context context, int cameraId, List<Size> sizes) {
        String key_build = new StringBuilder();
        key_build.append(PICTURE_SIZES_BUILD_KEY);
        key_build.append(cameraId);
        key_build = key_build.toString();
        SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (defaultPrefs.getString(key_build, null) == null) {
            String key_sizes = new StringBuilder();
            key_sizes.append(PICTURE_SIZES_SIZES_KEY);
            key_sizes.append(cameraId);
            key_sizes = key_sizes.toString();
            Editor editor = defaultPrefs.edit();
            editor.putString(key_build, Build.DISPLAY);
            editor.putString(key_sizes, Size.listToString(sizes));
            editor.apply();
        }
    }

    public static List<Size> getSizesForCamera(int cameraId, Context context) {
        String key_build = new StringBuilder();
        key_build.append(PICTURE_SIZES_BUILD_KEY);
        key_build.append(cameraId);
        key_build = key_build.toString();
        String key_sizes = new StringBuilder();
        key_sizes.append(PICTURE_SIZES_SIZES_KEY);
        key_sizes.append(cameraId);
        key_sizes = key_sizes.toString();
        SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String thisCameraCachedBuild = defaultPrefs.getString(key_build, null);
        if (thisCameraCachedBuild != null && thisCameraCachedBuild.equals(Build.DISPLAY)) {
            String thisCameraCachedSizeList = defaultPrefs.getString(key_sizes, null);
            if (thisCameraCachedSizeList != null) {
                return Size.stringToList(thisCameraCachedSizeList);
            }
        }
        try {
            Camera thisCamera = Camera.open(cameraId);
            if (thisCamera == null) {
                return null;
            }
            List<Size> sizes = Size.buildListFromCameraSizes(thisCamera.getParameters().getSupportedPictureSizes());
            thisCamera.release();
            Editor editor = defaultPrefs.edit();
            editor.putString(key_build, Build.DISPLAY);
            editor.putString(key_sizes, Size.listToString(sizes));
            editor.apply();
            return sizes;
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static String[] getQualitiesForCamera(int cameraId, Context context) {
        String key_build = new StringBuilder();
        key_build.append(PICTURE_SIZES_BUILD_KEY);
        key_build.append(cameraId);
        key_build = key_build.toString();
        String key_qualities = new StringBuilder();
        key_qualities.append(VIDEO_QUALITIES_KEY);
        key_qualities.append(cameraId);
        key_qualities = key_qualities.toString();
        SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String thisCameraCachedBuild = defaultPrefs.getString(key_build, null);
        if (thisCameraCachedBuild != null && thisCameraCachedBuild.equals(Build.DISPLAY)) {
            String thisCameraCachedQualityList = defaultPrefs.getString(key_qualities, null);
            if (thisCameraCachedQualityList != null) {
                return TextUtils.split(thisCameraCachedQualityList, Size.DELIMITER);
            }
        }
        return null;
    }

    public static String[] getQualityTitlesForCamera(int cameraId, Context context) {
        String key_build = new StringBuilder();
        key_build.append(PICTURE_SIZES_BUILD_KEY);
        key_build.append(cameraId);
        key_build = key_build.toString();
        String key_quality_titles = new StringBuilder();
        key_quality_titles.append(VIDEO_QUALITIES_TITLES_KEY);
        key_quality_titles.append(cameraId);
        key_quality_titles = key_quality_titles.toString();
        SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        String thisCameraCachedBuild = defaultPrefs.getString(key_build, null);
        if (thisCameraCachedBuild != null && thisCameraCachedBuild.equals(Build.DISPLAY)) {
            String thisCameraCachedQualityList = defaultPrefs.getString(key_quality_titles, null);
            if (thisCameraCachedQualityList != null) {
                return TextUtils.split(thisCameraCachedQualityList, Size.DELIMITER);
            }
        }
        return null;
    }
}
