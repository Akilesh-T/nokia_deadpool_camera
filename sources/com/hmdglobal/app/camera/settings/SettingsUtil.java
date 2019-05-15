package com.hmdglobal.app.camera.settings;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources.NotFoundException;
import android.media.CamcorderProfile;
import android.text.TextUtils;
import android.util.SparseArray;
import com.android.ex.camera2.portability.CameraDeviceInfo;
import com.android.ex.camera2.portability.CameraDeviceInfo.Characteristics;
import com.android.ex.camera2.portability.CameraSettings;
import com.android.ex.camera2.portability.Size;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.motion.MotionPictureHelper;
import com.hmdglobal.app.camera.util.ApiHelper;
import com.hmdglobal.app.camera.util.Callback;
import com.hmdglobal.app.camera.util.CustomFields;
import com.hmdglobal.app.camera.util.CustomUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class SettingsUtil {
    public static final CameraDeviceSelector CAMERA_FACING_BACK = new CameraDeviceSelector() {
        public boolean useCamera(Characteristics info) {
            return info.isFacingBack();
        }
    };
    public static final CameraDeviceSelector CAMERA_FACING_FRONT = new CameraDeviceSelector() {
        public boolean useCamera(Characteristics info) {
            return info.isFacingFront();
        }
    };
    private static final boolean DEBUG = false;
    private static final float MEDIUM_RELATIVE_PICTURE_SIZE = 0.5f;
    public static final String QUALITY_1080P_60FPS = "-6";
    public static final String QUALITY_1080P_60FPS_NAME = "QUALITY_1080P_60FPS";
    public static String[] RESOLUTION_CHANGE_MPS = new String[]{"8MP", "8MP", "6MP", "5MP"};
    private static final String SIZE_LARGE = "large";
    private static final String SIZE_MEDIUM = "medium";
    private static final String SIZE_SMALL = "small";
    private static final float SMALL_RELATIVE_PICTURE_SIZE = 0.25f;
    private static final Tag TAG = new Tag("SettingsUtil");
    public static final HashMap<String, Integer> VIDEO_QUALITY_TABLE = new HashMap();
    public static final HashMap<String, Integer> VIDEO_QUALITY_VALUE_TABLE = new HashMap();
    public static SparseArray<SelectedPictureSizes> sCachedSelectedPictureSizes = new SparseArray(2);
    public static SparseArray<SelectedVideoQualities> sCachedSelectedVideoQualities = new SparseArray(2);
    public static int[] sVideoQualities = new int[]{8, 6, 5, 4, 3, 7, 2};

    public interface CameraDeviceSelector {
        boolean useCamera(Characteristics characteristics);
    }

    public static class SelectedPictureSizes {
        public Size large;
        public Size medium;
        public Size small;

        public Size getFromSetting(String sizeSetting, List<Size> supportedSizes) {
            if (SettingsUtil.SIZE_LARGE.equals(sizeSetting)) {
                return this.large;
            }
            if (SettingsUtil.SIZE_MEDIUM.equals(sizeSetting)) {
                return this.medium;
            }
            if (SettingsUtil.SIZE_SMALL.equals(sizeSetting)) {
                return this.small;
            }
            if (sizeSetting != null && sizeSetting.split("x").length == 2) {
                Size desiredSize = SettingsUtil.sizeFromString(sizeSetting);
                if (supportedSizes.contains(desiredSize)) {
                    return desiredSize;
                }
            }
            return this.large;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SelectedPictureSizes: ");
            stringBuilder.append(this.large);
            stringBuilder.append(", ");
            stringBuilder.append(this.medium);
            stringBuilder.append(", ");
            stringBuilder.append(this.small);
            return stringBuilder.toString();
        }
    }

    public static class SelectedVideoQualities {
        public int large = -1;
        public int medium = -1;
        public int small = -1;

        public int getFromSetting(String sizeSetting) {
            Object sizeSetting2;
            if (!(SettingsUtil.SIZE_SMALL.equals(sizeSetting2) || SettingsUtil.SIZE_MEDIUM.equals(sizeSetting2))) {
                sizeSetting2 = SettingsUtil.SIZE_LARGE;
            }
            if (SettingsUtil.SIZE_LARGE.equals(sizeSetting2)) {
                return this.large;
            }
            if (SettingsUtil.SIZE_MEDIUM.equals(sizeSetting2)) {
                return this.medium;
            }
            return this.small;
        }
    }

    static {
        VIDEO_QUALITY_TABLE.put("3840x2160", Integer.valueOf(8));
        VIDEO_QUALITY_TABLE.put("1920x1080", Integer.valueOf(6));
        VIDEO_QUALITY_TABLE.put("1280x720", Integer.valueOf(5));
        VIDEO_QUALITY_TABLE.put("720x480", Integer.valueOf(4));
        VIDEO_QUALITY_TABLE.put("352x288", Integer.valueOf(3));
        VIDEO_QUALITY_TABLE.put("320x240", Integer.valueOf(7));
        VIDEO_QUALITY_TABLE.put("176x144", Integer.valueOf(2));
        VIDEO_QUALITY_VALUE_TABLE.put(QUALITY_1080P_60FPS, Integer.valueOf(6));
    }

    public static int getMaxVideoDuration(Context context) {
        try {
            return context.getResources().getInteger(R.integer.max_video_recording_length);
        } catch (NotFoundException e) {
            return 0;
        }
    }

    public static void setCameraPictureSize(String sizeSetting, List<Size> supported, CameraSettings settings, int cameraId) {
        Size selectedSize = getCameraPictureSize(sizeSetting, supported, cameraId);
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Selected ");
        stringBuilder.append(sizeSetting);
        stringBuilder.append(" resolution: ");
        stringBuilder.append(selectedSize.width());
        stringBuilder.append("x");
        stringBuilder.append(selectedSize.height());
        Log.d(tag, stringBuilder.toString());
        settings.setPhotoSize(selectedSize);
    }

    public static Size getPhotoSize(String sizeSetting, List<Size> supported, int cameraId) {
        if (ResolutionUtil.NEXUS_5_LARGE_16_BY_9.equals(sizeSetting)) {
            return ResolutionUtil.NEXUS_5_LARGE_16_BY_9_SIZE;
        }
        return getCameraPictureSize(sizeSetting, supported, cameraId);
    }

    private static Size getCameraPictureSize(String sizeSetting, List<Size> supported, int cameraId) {
        return getSelectedCameraPictureSizes(supported, cameraId).getFromSetting(sizeSetting, supported);
    }

    static SelectedPictureSizes getSelectedCameraPictureSizes(List<Size> supported, int cameraId) {
        List<Size> supportedCopy = new LinkedList(supported);
        if (sCachedSelectedPictureSizes.get(cameraId) != null) {
            return (SelectedPictureSizes) sCachedSelectedPictureSizes.get(cameraId);
        }
        SelectedPictureSizes selectedSizes = new SelectedPictureSizes();
        Collections.sort(supportedCopy, new Comparator<Size>() {
            public int compare(Size lhs, Size rhs) {
                return (rhs.width() * rhs.height()) - (lhs.width() * lhs.height());
            }
        });
        selectedSizes.large = (Size) supportedCopy.remove(0);
        float targetAspectRatio = ((float) selectedSizes.large.width()) / ((float) selectedSizes.large.height());
        ArrayList<Size> aspectRatioMatches = new ArrayList();
        for (Size size : supportedCopy) {
            if (((double) Math.abs((((float) size.width()) / ((float) size.height())) - targetAspectRatio)) < 0.01d) {
                aspectRatioMatches.add(size);
            }
        }
        List<Size> searchList = aspectRatioMatches.size() >= 2 ? aspectRatioMatches : supportedCopy;
        if (searchList.isEmpty()) {
            Log.w(TAG, "Only one supported resolution.");
            selectedSizes.medium = selectedSizes.large;
            selectedSizes.small = selectedSizes.large;
        } else if (searchList.size() == 1) {
            Log.w(TAG, "Only two supported resolutions.");
            selectedSizes.medium = (Size) searchList.get(0);
            selectedSizes.small = (Size) searchList.get(0);
        } else if (searchList.size() == 2) {
            Log.w(TAG, "Exactly three supported resolutions.");
            selectedSizes.medium = (Size) searchList.get(0);
            selectedSizes.small = (Size) searchList.get(1);
        } else {
            int largePixelCount = selectedSizes.large.width() * selectedSizes.large.height();
            int smallTargetPixelCount = (int) (((float) largePixelCount) * 1048576000);
            int mediumSizeIndex = findClosestSize(searchList, (int) (((float) largePixelCount) * 1056964608));
            int smallSizeIndex = findClosestSize(searchList, smallTargetPixelCount);
            if (((Size) searchList.get(mediumSizeIndex)).equals(searchList.get(smallSizeIndex))) {
                if (smallSizeIndex < searchList.size() - 1) {
                    smallSizeIndex++;
                } else {
                    mediumSizeIndex--;
                }
            }
            selectedSizes.medium = (Size) searchList.get(mediumSizeIndex);
            selectedSizes.small = (Size) searchList.get(smallSizeIndex);
        }
        sCachedSelectedPictureSizes.put(cameraId, selectedSizes);
        return selectedSizes;
    }

    public static int getVideoQuality(String qualitySetting, int cameraId) {
        return getSelectedVideoQualities(cameraId).getFromSetting(qualitySetting);
    }

    static SelectedVideoQualities getSelectedVideoQualities(int cameraId) {
        if (sCachedSelectedVideoQualities.get(cameraId) != null) {
            return (SelectedVideoQualities) sCachedSelectedVideoQualities.get(cameraId);
        }
        int largeIndex = getNextSupportedVideoQualityIndex(cameraId, -1);
        int mediumIndex = getNextSupportedVideoQualityIndex(cameraId, largeIndex);
        int smallIndex = getNextSupportedVideoQualityIndex(cameraId, mediumIndex);
        SelectedVideoQualities selectedQualities = new SelectedVideoQualities();
        selectedQualities.large = sVideoQualities[largeIndex];
        selectedQualities.medium = sVideoQualities[mediumIndex];
        selectedQualities.small = sVideoQualities[smallIndex];
        sCachedSelectedVideoQualities.put(cameraId, selectedQualities);
        return selectedQualities;
    }

    private static int getNextSupportedVideoQualityIndex(int cameraId, int start) {
        int i = start + 1;
        while (i < sVideoQualities.length) {
            if (isVideoQualitySupported(sVideoQualities[i]) && CamcorderProfile.hasProfile(cameraId, sVideoQualities[i])) {
                return i;
            }
            i++;
        }
        if (start >= 0 && start < sVideoQualities.length) {
            return start;
        }
        throw new IllegalArgumentException("Could not find supported video qualities.");
    }

    public static boolean isVideoQualitySupported(int videoQuality) {
        if (ApiHelper.isLOrHigher() || videoQuality != 8) {
            return true;
        }
        return false;
    }

    private static int findClosestSize(List<Size> sortedSizes, int targetPixelCount) {
        int closestMatchIndex = 0;
        int closestMatchPixelCountDiff = ActivityChooserViewAdapter.MAX_ACTIVITY_COUNT_UNLIMITED;
        for (int i = 0; i < sortedSizes.size(); i++) {
            Size size = (Size) sortedSizes.get(i);
            int pixelCountDiff = Math.abs((size.width() * size.height()) - targetPixelCount);
            if (pixelCountDiff < closestMatchPixelCountDiff) {
                closestMatchIndex = i;
                closestMatchPixelCountDiff = pixelCountDiff;
            }
        }
        return closestMatchIndex;
    }

    public static String sizeToSetting(Size size) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Integer.valueOf(size.width()).toString());
        stringBuilder.append("x");
        stringBuilder.append(Integer.valueOf(size.height()).toString());
        return stringBuilder.toString();
    }

    public static Size sizeFromString(String sizeSetting) {
        String[] parts = sizeSetting.split("x");
        if (parts.length == 2) {
            return new Size(Integer.valueOf(parts[0]).intValue(), Integer.valueOf(parts[1]).intValue());
        }
        return null;
    }

    public static Builder getFirstTimeLocationAlertBuilder(Builder builder, Callback<Boolean> callback) {
        if (callback == null) {
            return null;
        }
        getLocationAlertBuilder(builder, callback).setMessage(R.string.remember_location_prompt);
        return builder;
    }

    public static Builder getLocationAlertBuilder(Builder builder, final Callback<Boolean> callback) {
        if (callback == null) {
            return null;
        }
        builder.setTitle(R.string.remember_location_title).setPositiveButton(R.string.remember_location_yes, new OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                callback.onCallback(Boolean.valueOf(true));
            }
        }).setNegativeButton(R.string.remember_location_no, new OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {
                callback.onCallback(Boolean.valueOf(false));
            }
        });
        return builder;
    }

    public static int getCameraId(CameraDeviceInfo info, CameraDeviceSelector chooser) {
        int numCameras = info.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            Characteristics props = info.getCharacteristics(i);
            if (props != null && chooser.useCamera(props)) {
                return i;
            }
        }
        return -1;
    }

    public static String getDefaultPictureSize(boolean isFrontCamera) {
        String key;
        String flag;
        if (CustomUtil.getInstance().isPanther()) {
            key = isFrontCamera ? CustomFields.DEF_PANTHER_PICTURE_SIZE_FRONT : CustomFields.DEF_PANTHER_PICTURE_SIZE_REAR;
            if (isFrontCamera) {
                flag = CustomUtil.getInstance().getString(CustomFields.PREF_PANTHER_PICTURE_SIZE_DEF_FRONT, "0");
            } else {
                flag = CustomUtil.getInstance().getString(CustomFields.PREF_PANTHER_PICTURE_SIZE_DEF_REAR, "0");
            }
        } else {
            key = isFrontCamera ? CustomFields.DEF_DEADPOOL_PICTURE_SIZE_FRONT : CustomFields.DEF_DEADPOOL_PICTURE_SIZE_REAR;
            if (isFrontCamera) {
                flag = CustomUtil.getInstance().getString(CustomFields.PREF_DEADPOOL_PICTURE_SIZE_DEF_FRONT, "0");
            } else {
                flag = CustomUtil.getInstance().getString(CustomFields.PREF_DEADPOOL_PICTURE_SIZE_DEF_REAR, "0");
            }
        }
        int value = Integer.valueOf(flag).intValue();
        String sizeQueue = CustomUtil.getInstance().getString(key, null);
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("get ");
        stringBuilder.append(key);
        stringBuilder.append("  sizequ =");
        stringBuilder.append(sizeQueue);
        stringBuilder.append("  value =");
        stringBuilder.append(value);
        stringBuilder.append("  flag =");
        stringBuilder.append(flag);
        Log.i(tag, stringBuilder.toString());
        if (sizeQueue == null) {
            tag = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("get ");
            stringBuilder.append(key);
            stringBuilder.append(" is null");
            Log.i(tag, stringBuilder.toString());
            return null;
        }
        String[] sizes = TextUtils.split(sizeQueue, Size.DELIMITER);
        Tag tag2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" size value0  =");
        stringBuilder2.append(sizes[value]);
        Log.d(tag2, stringBuilder2.toString());
        if (sizes != null && sizes.length > value) {
            return sizes[value];
        }
        tag2 = TAG;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Cannot get default size for flag ");
        stringBuilder2.append(value);
        stringBuilder2.append("in queue ");
        stringBuilder2.append(sizeQueue);
        Log.i(tag2, stringBuilder2.toString());
        return null;
    }

    public static Size getBokehPhotoSize(Context context, String size) {
        try {
            String[] sizes = TextUtils.split(CustomUtil.getInstance().getString(CustomFields.DEF_PANTHER_PICTURE_SIZE_REAR, null), Size.DELIMITER);
            String[] bokehSizes = TextUtils.split(CustomUtil.getInstance().getString(CustomFields.DEF_BOKEH_PICTURE_SIZE_REAR, null), Size.DELIMITER);
            for (int i = 0; i < sizes.length; i++) {
                if (sizes[i].equals(size)) {
                    String bokehSize = bokehSizes[i];
                    Tag tag = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("bokehPhotoSize = ");
                    stringBuilder.append(bokehSize);
                    Log.d(tag, stringBuilder.toString());
                    return sizeFromString(bokehSize);
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "bokeh photo size not support");
        }
        return new Size(3264, 2448);
    }

    public static Size getBokehPreviewSize(String size, boolean isFront) {
        Exception e;
        if (isFront) {
            try {
                e = CustomFields.DEF_PANTHER_PICTURE_SIZE_FRONT;
            } catch (Exception e2) {
                Log.d(TAG, "bokeh preview size not support");
            }
        } else {
            e = CustomFields.DEF_PANTHER_PICTURE_SIZE_REAR;
        }
        String[] sizes = TextUtils.split(CustomUtil.getInstance().getString(e, null), Size.DELIMITER);
        String[] bokehSizes = TextUtils.split(CustomUtil.getInstance().getString(CustomFields.DEF_BOKEH_PREVIEW_SIZE_REAR, null), Size.DELIMITER);
        for (int i = 0; i < sizes.length; i++) {
            if (sizes[i].equals(size)) {
                String bokehSize = bokehSizes[i];
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("bokehPreviewSize = ");
                stringBuilder.append(bokehSize);
                Log.d(tag, stringBuilder.toString());
                return sizeFromString(bokehSize);
            }
        }
        return new Size(960, MotionPictureHelper.FRAME_HEIGHT_9);
    }
}
