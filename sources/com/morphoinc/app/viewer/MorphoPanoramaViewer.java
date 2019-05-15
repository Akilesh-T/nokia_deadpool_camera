package com.morphoinc.app.viewer;

import com.morphoinc.app.panoramagp3.Camera2ParamsFragment;
import com.morphoinc.core.Error;

public class MorphoPanoramaViewer {
    public static final int DISP_TYPE_NONE = 0;
    public static final int DISP_TYPE_WIRE_FRAME = 1;
    public static final int MODE_VIEWING = 1;
    public static final int PROJECTION_TYPE_CYLINDRICAL_H = 3;
    public static final int PROJECTION_TYPE_CYLINDRICAL_V = 4;
    public static final int PROJECTION_TYPE_FISHEYE = 5;
    public static final int PROJECTION_TYPE_MERCATOR_H = 0;
    public static final int PROJECTION_TYPE_MERCATOR_V = 1;
    public static final int PROJECTION_TYPE_PERSPECTIVE = 2;
    public static final int RENDER_MODE_OPEN_GL = 1;
    public static final int STILL_IMAGE_FORMAT_JPEG = 256;
    public static final int STILL_IMAGE_FORMAT_YVU420SP = 17;
    private static boolean mFinished = true;
    private boolean mInitialized = false;
    private long mNative = 0;

    public static class FrameColor {
        public float A;
        public float B;
        public float G;
        public float R;
        public float Width;
    }

    public static class GalleryData {
        public int cropped_area_image_height;
        public int cropped_area_image_width;
        public int cropped_area_left;
        public int cropped_area_top;
        public int full_pano_height;
        public int full_pano_width;
    }

    public static class PanoramaViewerInitParam {
        public String format;
        public double input_angle_of_view_degree;
        public int input_height;
        public int input_width;
        public int mode;
        public FrameColor registered_frame_color = new FrameColor();
        public int render_mode;
        public int version;
    }

    public static class ViewParam {
        public double scale = 1.0d;
        public double x_rotate = Camera2ParamsFragment.TARGET_EV;
        public double y_rotate = Camera2ParamsFragment.TARGET_EV;
    }

    private final native long createNativeObject();

    private final native void deleteNativeObject(long j);

    private final native int finish(long j);

    private final native int initialize(long j, PanoramaViewerInitParam panoramaViewerInitParam, int[] iArr);

    private final native int nativeDecodePostview(long j, String str, int[] iArr, int[] iArr2, int[] iArr3, int[] iArr4);

    private final native int nativeGetGalleryDataOfAppSeg(long j, byte[] bArr);

    private static final native String nativeGetVersion();

    private final native int nativeRenderPostview(long j, double d, double d2, double d3, int i);

    private final native int nativeRenderPostviewAngle(long j, double d, double d2, double d3, int i);

    private final native int nativeRenderPostviewDefault(long j, int i);

    private final native int nativeSetGalleryData(long j, GalleryData galleryData, int i, int i2);

    static {
        try {
            System.loadLibrary("morpho_panorama_viewer");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    public static boolean isFinished() {
        return mFinished;
    }

    public MorphoPanoramaViewer() {
        long ret = createNativeObject();
        if (ret != 0) {
            this.mNative = ret;
        } else {
            this.mNative = 0;
        }
    }

    public static String getVersion() {
        return nativeGetVersion();
    }

    public int initialize(PanoramaViewerInitParam param, int[] buffer_size) {
        if (this.mNative == 0) {
            return 0;
        }
        int ret = initialize(this.mNative, param, buffer_size);
        this.mInitialized = true;
        return ret;
    }

    public int finish() {
        int ret = 0;
        if (this.mNative == 0) {
            return Error.ERROR_STATE;
        }
        mFinished = true;
        if (this.mInitialized) {
            ret = finish(this.mNative);
            this.mInitialized = false;
        }
        deleteNativeObject(this.mNative);
        this.mNative = 0;
        return ret;
    }

    public int getGalleryDataOfAppSeg(byte[] gallery_data) {
        if (this.mNative != 0) {
            return nativeGetGalleryDataOfAppSeg(this.mNative, gallery_data);
        }
        return Error.ERROR_STATE;
    }

    public int decodeGalleyData(String path, int[] out_width, int[] out_height, int[] exif_orientation, int[] gallery_data_size) {
        if (this.mNative == 0) {
            return Error.ERROR_STATE;
        }
        return nativeDecodePostview(this.mNative, path, out_width, out_height, exif_orientation, gallery_data_size);
    }

    public int setGalleryData(GalleryData gallery_data, int rotation, int render_low_image) {
        if (this.mNative == 0) {
            return Error.ERROR_STATE;
        }
        return nativeSetGalleryData(this.mNative, gallery_data, rotation, render_low_image);
    }

    public int renderPostview(double x_rotate, double y_rotate, double scale, int disp_type) {
        if (this.mNative == 0) {
            return Error.ERROR_STATE;
        }
        return nativeRenderPostview(this.mNative, x_rotate, y_rotate, scale, disp_type);
    }

    public int renderPostviewAngle(double x_rotate, double y_rotate, double scale, int disp_type) {
        if (this.mNative == 0) {
            return Error.ERROR_STATE;
        }
        return nativeRenderPostviewAngle(this.mNative, x_rotate, y_rotate, scale, disp_type);
    }

    public int renderPostviewDefault(int disp_type) {
        if (this.mNative != 0) {
            return nativeRenderPostviewDefault(this.mNative, disp_type);
        }
        return Error.ERROR_STATE;
    }
}
