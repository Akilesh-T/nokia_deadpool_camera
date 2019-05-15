package com.morphoinc.core;

import android.graphics.Bitmap;
import android.util.Log;
import java.util.Arrays;

public class ImageRefiner {
    public static final int IMAGE_TYPE_INPUT_JPG = 1;
    public static final int IMAGE_TYPE_INPUT_RAW = 0;
    public static final int IMAGE_TYPE_RESULT = 2;
    private long mNative = 0;

    public static class EngineParam {
        public double camera_motion_threshold;
        public double chroma_noise_reduction_coeff;
        public int chroma_noise_reduction_iteration;
        public int[] cnr_table = new int[10];
        public double color_enhancement_contrast_level;
        public double color_enhancement_saturation_level;
        public int ghost_map_sharing_enabled;
        public double ghost_rate_threshold;
        public double ghost_removal_strength;
        public double luma_noise_reduction_coeff;
        public double mfnr_color_gamma;
        public int mfnr_contrast_strength;
        public double mfnr_ghost_removal_strength;
        public double mfnr_luminance_gain;
        public int mfnr_saturation_compensation_level;
        public int mfnr_texture_preservation_level;
        public int sharpness_enhancement_level;
        public double synth_ev_stop;
        public double[] ynr_table = new double[10];

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("EngineParam{synth_ev_stop=");
            stringBuilder.append(this.synth_ev_stop);
            stringBuilder.append(", camera_motion_threshold=");
            stringBuilder.append(this.camera_motion_threshold);
            stringBuilder.append(", ghost_removal_strength=");
            stringBuilder.append(this.ghost_removal_strength);
            stringBuilder.append(", ghost_rate_threshold=");
            stringBuilder.append(this.ghost_rate_threshold);
            stringBuilder.append(", color_enhancement_contrast_level=");
            stringBuilder.append(this.color_enhancement_contrast_level);
            stringBuilder.append(", color_enhancement_saturation_level=");
            stringBuilder.append(this.color_enhancement_saturation_level);
            stringBuilder.append(", ghost_map_sharing_enabled=");
            stringBuilder.append(this.ghost_map_sharing_enabled);
            stringBuilder.append(", mfnr_ghost_removal_strength=");
            stringBuilder.append(this.mfnr_ghost_removal_strength);
            stringBuilder.append(", mfnr_texture_preservation_level=");
            stringBuilder.append(this.mfnr_texture_preservation_level);
            stringBuilder.append(", luma_noise_reduction_coeff=");
            stringBuilder.append(this.luma_noise_reduction_coeff);
            stringBuilder.append(", chroma_noise_reduction_coeff=");
            stringBuilder.append(this.chroma_noise_reduction_coeff);
            stringBuilder.append(", chroma_noise_reduction_iteration=");
            stringBuilder.append(this.chroma_noise_reduction_iteration);
            stringBuilder.append(", mfnr_saturation_compensation_level=");
            stringBuilder.append(this.mfnr_saturation_compensation_level);
            stringBuilder.append(", mfnr_luminance_gain=");
            stringBuilder.append(this.mfnr_luminance_gain);
            stringBuilder.append(", mfnr_color_gamma=");
            stringBuilder.append(this.mfnr_color_gamma);
            stringBuilder.append(", mfnr_contrast_strength=");
            stringBuilder.append(this.mfnr_contrast_strength);
            stringBuilder.append(", sharpness_enhancement_level=");
            stringBuilder.append(this.sharpness_enhancement_level);
            stringBuilder.append(", ynr_table=");
            stringBuilder.append(Arrays.toString(this.ynr_table));
            stringBuilder.append(", cnr_table=");
            stringBuilder.append(Arrays.toString(this.cnr_table));
            stringBuilder.append('}');
            return stringBuilder.toString();
        }
    }

    public static class MergeInfo {
        public int merge_status;
        public int[] merged_image_indices;
        public int num_merged_image;

        public MergeInfo(int maxNum) {
            this.merged_image_indices = new int[maxNum];
        }
    }

    private final native int addImage(long j, byte[] bArr, long j2, int i);

    private final native int addImageRaw(long j, byte[] bArr, long j2, int i);

    private final native long createNativeObject();

    private final native void deleteNativeObject(long j);

    private final native int finalizeNativeObject(long j);

    private final native int getEVs(long j, boolean z, boolean z2, float[] fArr);

    private final native int getImage(long j, int i, int i2, Bitmap bitmap);

    private final native int getMergeInfo(long j, MergeInfo mergeInfo);

    private final native int getOutputImageRaw(long j, byte[] bArr);

    private final native int getParam(long j, EngineParam engineParam);

    public static final native String getVersion();

    private final native int initializeNativeObject(long j, int i, int i2, int i3, int i4, String str, int i5);

    private final native int processImage(long j);

    private final native int save(long j, int i, int i2, String str);

    private final native int setParam(long j, EngineParam engineParam);

    static {
        try {
            System.loadLibrary("morpho_image_refiner");
            Log.d("ImageRefiner", "successfully loaded");
        } catch (UnsatisfiedLinkError e) {
            Log.e("ImageRefiner", e.getMessage());
            Log.e("ImageRefiner", "can't loadLibrary");
        }
    }

    public int initialize(int imageWidth, int imageHeight, int imageNum, int evNum, String format, boolean enable_synthetic_ev_image) {
        if (this.mNative == 0) {
            this.mNative = createNativeObject();
            if (this.mNative == 0) {
                return -1;
            }
        }
        return initializeNativeObject(this.mNative, imageWidth, imageHeight, imageNum, evNum, format, enable_synthetic_ev_image);
    }

    public void finish() {
        if (this.mNative != 0) {
            finalizeNativeObject(this.mNative);
            deleteNativeObject(this.mNative);
            this.mNative = 0;
        }
    }

    public int addImage(byte[] src, long exposureTime, int iso) {
        if (this.mNative == 0) {
            return -1;
        }
        return addImage(this.mNative, src, exposureTime, iso);
    }

    public int addImageRaw(byte[] src, long exposureTime, int iso) {
        if (this.mNative == 0) {
            return -1;
        }
        return addImageRaw(this.mNative, src, exposureTime, iso);
    }

    public int processImage() {
        if (this.mNative == 0) {
            return -1;
        }
        return processImage(this.mNative);
    }

    public int getImage(int image_type, int index, Bitmap out_bitmap) {
        if (this.mNative == 0) {
            return -1;
        }
        return getImage(this.mNative, image_type, index, out_bitmap);
    }

    public int save(int type, int index, String path) {
        if (this.mNative == 0) {
            return -1;
        }
        return save(this.mNative, type, index, path);
    }

    public int getParam(EngineParam param) {
        if (this.mNative == 0) {
            return -1;
        }
        return getParam(this.mNative, param);
    }

    public int setParam(EngineParam param) {
        if (this.mNative == 0) {
            return -1;
        }
        return setParam(this.mNative, param);
    }

    public int getMergeInfo(MergeInfo info) {
        if (this.mNative == 0) {
            return -1;
        }
        return getMergeInfo(this.mNative, info);
    }

    public int getOutputImageRaw(byte[] data) {
        if (this.mNative == 0) {
            return -1;
        }
        return getOutputImageRaw(this.mNative, data);
    }

    public int getEVs(boolean isLowLightMode, boolean isBackCamera, float[] EV) {
        if (this.mNative == 0) {
            return -1;
        }
        return getEVs(this.mNative, isLowLightMode, isBackCamera, EV);
    }
}
