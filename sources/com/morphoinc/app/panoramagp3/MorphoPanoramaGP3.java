package com.morphoinc.app.panoramagp3;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.Image;
import android.text.format.DateFormat;
import com.morphoinc.app.LogFilter;
import com.morphoinc.core.Error;
import com.morphoinc.core.MorphoSensorFusion.SensorData;
import com.morphoinc.utils.NativeMemoryAllocator;
import java.io.File;
import java.io.FileDescriptor;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Locale;

public class MorphoPanoramaGP3 {
    public static final int DIRECTION_AUTO = 0;
    public static final int DIRECTION_HORIZONTAL = 2;
    public static final int DIRECTION_HORIZONTAL_LEFT = 5;
    public static final int DIRECTION_HORIZONTAL_RIGHT = 6;
    public static final int DIRECTION_VERTICAL = 1;
    public static final int DIRECTION_VERTICAL_DOWN = 4;
    public static final int DIRECTION_VERTICAL_UP = 3;
    public static final int END_MODE_CANCEL = 2;
    public static final int END_MODE_MAKE_360 = 0;
    public static final int END_MODE_NOT_MAKE_360 = 1;
    public static final int END_STATUS_CANCEL = 2;
    public static final int END_STATUS_MAKE_360 = 0;
    public static final int END_STATUS_NOT_MAKE_360 = 1;
    public static final int ERROR_INVALID_DIR = -1073741823;
    private static double GAIN_COEF = 1.0d;
    public static final int MODE_PANORAMA = 0;
    public static final int MODE_SCANNER = 1;
    private static final int POINT_INFO_SIZE = 2;
    private static final int POINT_X_OFFSET = 0;
    private static final int POINT_Y_OFFSET = 1;
    private static final int RECT_BOTTOM_OFFSET = 3;
    private static final int RECT_INFO_SIZE = 4;
    private static final int RECT_LEFT_OFFSET = 0;
    private static final int RECT_RIGHT_OFFSET = 2;
    private static final int RECT_TOP_OFFSET = 1;
    private static final boolean USE_STANDARD_DEVIATION = true;
    private long mAttachCount;
    private boolean mAttachEnabled;
    private long mAttachFirstNanoTime;
    private long mAttachLastNanoTime;
    private String mFolderPathInputImages;
    private final GravityParam mGravity;
    private int[] mIndexBase;
    private String mInputImageFormat;
    private ArrayList<Long> mIntervalArray;
    private long mNative;
    private long mNativeOutputInfo;
    private boolean mSaveInputImages;

    public static class GalleryInfoData {
        public int crop_height;
        public int crop_left;
        public int crop_top;
        public int crop_width;
        public int whole_height;
        public int whole_width;
    }

    public static class GravityParam {
        public double x = Camera2ParamsFragment.TARGET_EV;
        public double y = Camera2ParamsFragment.TARGET_EV;
        public double z = Camera2ParamsFragment.TARGET_EV;

        public GravityParam copyInstance() {
            GravityParam instance = new GravityParam();
            instance.x = this.x;
            instance.y = this.y;
            instance.z = this.z;
            return instance;
        }
    }

    public static class InitParam {
        public double aovx;
        public double aovy;
        public int direction;
        public double goal_angle;
        public String input_format;
        public int input_height;
        public int input_width;
        public String output_format;
        public int output_rotation;
    }

    private native long createNativeObject();

    private native long createNativeOutputInfoObject();

    private native void deleteNativeObject(long j);

    private native void deleteNativeOutputInfoObject(long j);

    private native int nativeAttach(long j, byte[] bArr);

    private native int nativeAttachYuv(long j, ByteBuffer byteBuffer, ByteBuffer byteBuffer2, ByteBuffer byteBuffer3, int i, int i2, int i3, int i4, int i5, int i6, String str, double[] dArr);

    private native int nativeCreateOutputImage(long j, int i, int i2, int i3, int i4);

    private native int nativeEnd(long j, int i, double d);

    private native int nativeFinish(long j);

    private native int nativeGetClippingRect(long j, int[] iArr);

    private native int nativeGetDirection(long j, int[] iArr);

    private native int nativeGetEndStatus(long j);

    private static native int nativeGetGain(ByteBuffer byteBuffer, ByteBuffer byteBuffer2, ByteBuffer byteBuffer3, int i, int i2, int i3, int i4, int i5, int i6, String str, int i7, int i8, double[] dArr);

    private native int nativeGetImage(long j, byte[] bArr, int i, int i2, int i3, int i4);

    private native int nativeGetOutputImage(long j, byte[] bArr, int i, int i2, int i3, int i4);

    private native int nativeGetOutputSize(long j, int[] iArr);

    private native int nativeGetRotatedSmallImage(long j, byte[] bArr, int i, int i2, int i3, int i4, int i5, int i6);

    private static native String nativeGetVersion();

    private native int nativeInitialize(long j, InitParam initParam, long j2);

    private native int nativePreparePanorama360(long j, int i, int i2, String str, String str2, boolean z, GalleryInfoData galleryInfoData);

    private static native int nativeRenderByteArray(byte[] bArr, ByteBuffer byteBuffer, ByteBuffer byteBuffer2, ByteBuffer byteBuffer3, int i, int i2, int i3, int i4, int i5, int i6, String str, int i7, int i8);

    private static native int nativeRenderByteArrayRaw(byte[] bArr, byte[] bArr2, String str, int i, int i2);

    private static native int nativeRenderByteBuffer(ByteBuffer byteBuffer, ByteBuffer byteBuffer2, ByteBuffer byteBuffer3, ByteBuffer byteBuffer4, int i, int i2, int i3, int i4, int i5, int i6, String str, int i7, int i8);

    private static native int nativeRenderByteBufferRaw(ByteBuffer byteBuffer, byte[] bArr, String str, int i, int i2);

    private native int nativeSaveLog(long j, String str, String str2);

    private static native int nativeSaveNotPanorama(ByteBuffer byteBuffer, ByteBuffer byteBuffer2, ByteBuffer byteBuffer3, ByteBuffer byteBuffer4, int i, int i2, int i3, int i4, int i5, int i6, String str, int i7, int i8, String str2, int i9);

    private native int nativeSavePanorama360(long j, int i, int i2, String str, int i3, String str2, String str3, boolean z, GalleryInfoData galleryInfoData, boolean z2);

    private native int nativeSavePanorama360Delay(long j, String str, int i, boolean z, int i2, boolean z2);

    private native int nativeSaveYuv(long j, ByteBuffer byteBuffer, ByteBuffer byteBuffer2, ByteBuffer byteBuffer3, int i, int i2, int i3, int i4, int i5, int i6, String str);

    private native int nativeSetAovGain(long j, double d);

    private native int nativeSetCalcseamPixnum(long j, int i);

    private native int nativeSetDistortionCorrectionParam(long j, double d, double d2, double d3, double d4);

    private native int nativeSetDrawThreshold(long j, double d);

    private native int nativeSetGyroscopeData(long j, SensorData[] sensorDataArr);

    private native int nativeSetImageFormat(long j, String str);

    private native int nativeSetInitialRotationByGravity(long j, double d, double d2, double d3);

    private native int nativeSetMotionDetectionMode(long j, int i);

    private native int nativeSetNoiseReductionParam(long j, int i);

    private native int nativeSetPreviewImage(long j, int i, int i2);

    private native int nativeSetProjectionMode(long j, int i);

    private native int nativeSetRotationRatio(long j, double d);

    private native int nativeSetRotationVector(long j, double[] dArr);

    private native int nativeSetSeamsearchRatio(long j, double d);

    private native int nativeSetSensorUseMode(long j, int i);

    private native int nativeSetShrinkRatio(long j, double d);

    private native int nativeSetUnsharpStrength(long j, int i);

    private native int nativeSetUseDeform(long j, int i);

    private native int nativeSetUseLuminanceCorrection(long j, int i);

    private native int nativeSetZrotationCoeff(long j, double d);

    private native int nativeStart(long j, int i, int i2);

    private native int nativeUpdatePreviewImage(long j, Bitmap bitmap);

    private static native int nativeYuv2Bitmap8888(byte[] bArr, int i, int i2, Bitmap bitmap, int i3, int i4);

    static {
        try {
            System.loadLibrary("morpho_panorama_gp3");
        } catch (UnsatisfiedLinkError e) {
            LogFilter.e("MorphoPanoramaGP3", e.getMessage());
            LogFilter.e("MorphoPanoramaGP3", "can't loadLibrary");
        }
    }

    public MorphoPanoramaGP3() {
        this.mNative = 0;
        this.mNativeOutputInfo = 0;
        this.mSaveInputImages = false;
        this.mIntervalArray = null;
        this.mAttachEnabled = false;
        this.mGravity = new GravityParam();
        this.mIndexBase = new int[4];
        this.mAttachFirstNanoTime = 0;
        this.mAttachLastNanoTime = 0;
        this.mIntervalArray = new ArrayList();
    }

    public void setAttachEnabled(boolean enabled) {
        this.mAttachEnabled = enabled;
    }

    public static String getVersion() {
        return nativeGetVersion();
    }

    public int initialize(InitParam param) {
        if (this.mNativeOutputInfo == 0) {
            return Error.ERROR_STATE;
        }
        int ret;
        this.mNative = createNativeObject();
        if (this.mNative != 0) {
            ret = nativeInitialize(this.mNative, param, this.mNativeOutputInfo);
        } else {
            ret = Error.ERROR_MALLOC;
        }
        return ret;
    }

    public int finish(boolean delete) {
        if (this.mNative == 0) {
            return Error.ERROR_STATE;
        }
        int ret = nativeFinish(this.mNative);
        if (!delete) {
            return ret;
        }
        deleteNativeObject(this.mNative);
        this.mNative = 0;
        return ret;
    }

    public int deleteObject() {
        if (this.mNative != 0) {
            deleteNativeObject(this.mNative);
            this.mNative = 0;
        }
        return 0;
    }

    public int start(int input_width, int input_height) {
        if (this.mNative == 0) {
            return Error.ERROR_STATE;
        }
        int ret = nativeStart(this.mNative, input_width, input_height);
        this.mAttachCount = 0;
        return ret;
    }

    public int attach(byte[] input_image) {
        if (this.mNative == 0) {
            return Error.ERROR_STATE;
        }
        int ret = nativeAttach(this.mNative, input_image);
        if (this.mSaveInputImages) {
            Camera2App.d_save_raw(input_image, this.mFolderPathInputImages, String.format(Locale.US, "%06d", new Object[]{Long.valueOf(this.mAttachCount)}));
        }
        this.mAttachCount++;
        return ret;
    }

    public float getAttachAve() {
        if (this.mAttachCount == 0) {
            return 0.0f;
        }
        float spanAve = (((float) (this.mAttachLastNanoTime - this.mAttachFirstNanoTime)) / ((float) this.mAttachCount)) / 1000000.0f;
        if (spanAve == 0.0f) {
            return 0.0f;
        }
        return spanAve;
    }

    public float getAttachStandardDeviation() {
        float spanAve = getAttachAve();
        if (spanAve == 0.0f) {
            return 0.0f;
        }
        double var = Camera2ParamsFragment.TARGET_EV;
        int size = this.mIntervalArray.size();
        for (int i = 0; i < size; i++) {
            long intervalMillis = ((Long) this.mIntervalArray.get(i)).longValue() / 1000000;
            var += (double) ((((float) intervalMillis) - spanAve) * (((float) intervalMillis) - spanAve));
        }
        return (float) Math.sqrt(var / ((double) size));
    }

    public float getAttachFps() {
        float spanAve = getAttachAve();
        if (spanAve == 0.0f) {
            return 0.0f;
        }
        return 1000.0f / spanAve;
    }

    public int attach(ByteBuffer yBuffer, ByteBuffer uBuffer, ByteBuffer vBuffer, int yRowStride, int uRowStride, int vRowStride, int yPixelStride, int uPixelStride, int vPixelStride, SensorInfoManager sensorInfoManager, double[] cur_center, Context context) {
        SensorInfoManager sensorInfoManager2 = sensorInfoManager;
        if (this.mNative == 0) {
            return Error.ERROR_STATE;
        }
        int i;
        String filepath;
        if (this.mAttachCount == 0) {
            this.mAttachFirstNanoTime = System.nanoTime();
        }
        int ret = 0;
        long start = System.nanoTime();
        String filepath2 = null;
        if (this.mSaveInputImages) {
            String time;
            if (sensorInfoManager2 != null) {
                time = createName(sensorInfoManager2.timeMillis);
                String count = String.format(Locale.US, "%s/%06d_", new Object[]{this.mFolderPathInputImages, Long.valueOf(this.mAttachCount)});
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(count);
                stringBuilder.append(time);
                stringBuilder.append(String.format(Locale.US, "_sg%05d_sr%05d_sa%05d.yuv", new Object[]{Integer.valueOf(sensorInfoManager2.g_ix + this.mIndexBase[0]), Integer.valueOf(sensorInfoManager2.r_ix + this.mIndexBase[3]), Integer.valueOf(sensorInfoManager2.a_ix + this.mIndexBase[1])}));
                filepath2 = stringBuilder.toString();
                int[] iArr = this.mIndexBase;
                iArr[0] = iArr[0] + sensorInfoManager2.sensorData[0].size();
                iArr = this.mIndexBase;
                iArr[3] = iArr[3] + sensorInfoManager2.sensorData[3].size();
                iArr = this.mIndexBase;
                iArr[1] = iArr[1] + sensorInfoManager2.sensorData[1].size();
            } else {
                time = createName(System.currentTimeMillis());
                String count2 = String.format(Locale.US, "%s/%06d_", new Object[]{this.mFolderPathInputImages, Long.valueOf(this.mAttachCount)});
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(count2);
                stringBuilder2.append(time);
                stringBuilder2.append(".yuv");
                filepath2 = stringBuilder2.toString();
            }
        }
        String filepath3 = filepath2;
        if (this.mAttachEnabled) {
            String filepath4 = filepath3;
            i = 1;
            ret = nativeAttachYuv(this.mNative, yBuffer, uBuffer, vBuffer, yRowStride, uRowStride, vRowStride, yPixelStride, uPixelStride, vPixelStride, filepath3, cur_center);
            filepath = filepath4;
        } else {
            i = 1;
            filepath = filepath3;
            if (filepath != null) {
                ret = nativeSaveYuv(this.mNative, yBuffer, uBuffer, vBuffer, yRowStride, uRowStride, vRowStride, yPixelStride, uPixelStride, vPixelStride, filepath);
            }
        }
        long end = System.nanoTime();
        Object[] objArr = new Object[i];
        objArr[0] = Long.valueOf(end - start);
        LogFilter.v("Camera2App", String.format(Locale.US, "Performance.JNI %1$,3d nsec", objArr));
        this.mAttachCount++;
        if (this.mAttachCount > 1) {
            this.mIntervalArray.add(Long.valueOf(end - this.mAttachLastNanoTime));
        }
        this.mAttachLastNanoTime = end;
        if (filepath != null) {
            Camera2App.scanFile(context, new File(filepath));
        } else {
            Context context2 = context;
        }
        return ret;
    }

    private static String createName(long dateTaken) {
        return DateFormat.format("yyyy-MM-dd_kk-mm-ss", dateTaken).toString();
    }

    public int getImage(byte[] output_image, Rect rect) {
        if (this.mNative == 0) {
            return Error.ERROR_STATE;
        }
        return nativeGetImage(this.mNative, output_image, rect.left, rect.top, rect.right, rect.bottom);
    }

    public int getRotatedSmallImage(byte[] output_image, Rect rect, int shrink_ratio, int angle) {
        if (this.mNative == 0) {
            return Error.ERROR_STATE;
        }
        return nativeGetRotatedSmallImage(this.mNative, output_image, rect.left, rect.top, rect.right, rect.bottom, shrink_ratio, angle);
    }

    public int updatePreviewImage(Bitmap output_image) {
        if (this.mNative == 0) {
            return Error.ERROR_STATE;
        }
        return nativeUpdatePreviewImage(this.mNative, output_image);
    }

    public int setPreviewImage(int width, int height) {
        if (this.mNative == 0) {
            return Error.ERROR_STATE;
        }
        return nativeSetPreviewImage(this.mNative, width, height);
    }

    public int getClippingRect(Rect rect) {
        int ret;
        int[] rect_info = new int[4];
        if (this.mNative != 0) {
            ret = nativeGetClippingRect(this.mNative, rect_info);
            if (ret == 0) {
                rect.set(rect_info[0], rect_info[1], rect_info[2], rect_info[3]);
            }
        } else {
            ret = Error.ERROR_STATE;
        }
        if (ret != 0) {
            rect.set(0, 0, 0, 0);
        }
        return ret;
    }

    public String getInputImageFormat() {
        return this.mInputImageFormat;
    }

    public int setInputImageFormat(String format) {
        this.mInputImageFormat = "";
        if (this.mNative == 0) {
            return Error.ERROR_STATE;
        }
        int ret = nativeSetImageFormat(this.mNative, format);
        if (ret != 0) {
            return ret;
        }
        this.mInputImageFormat = format;
        return ret;
    }

    public long getAttachCount() {
        return this.mAttachCount;
    }

    public void enableSaveInputImages(String path) {
        this.mSaveInputImages = true;
        this.mFolderPathInputImages = path;
    }

    public void disableSaveInputImages() {
        this.mSaveInputImages = false;
    }

    public String getInputFolderPath() {
        return this.mFolderPathInputImages;
    }

    public int inputSave(Image image) {
        if (this.mNative == 0) {
            return Error.ERROR_STATE;
        }
        long start = System.currentTimeMillis();
        String filepath = null;
        if (this.mSaveInputImages) {
            filepath = String.format(Locale.US, "%s/%06d.yuv", new Object[]{this.mFolderPathInputImages, Long.valueOf(this.mAttachCount)});
        }
        String filepath2 = filepath;
        int ret = nativeSaveYuv(this.mNative, image.getPlanes()[0].getBuffer(), image.getPlanes()[1].getBuffer(), image.getPlanes()[2].getBuffer(), image.getPlanes()[0].getRowStride(), image.getPlanes()[1].getRowStride(), image.getPlanes()[2].getRowStride(), image.getPlanes()[0].getPixelStride(), image.getPlanes()[1].getPixelStride(), image.getPlanes()[2].getPixelStride(), filepath2);
        long end = System.currentTimeMillis();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Performance.JNI ");
        stringBuilder.append(end - start);
        stringBuilder.append(" msec");
        LogFilter.v("Camera2App", stringBuilder.toString());
        this.mAttachCount++;
        return ret;
    }

    public static double getGain(Image image) {
        String imageFormat = PanoramaGP3ImageFormat.getImageFormat(image);
        double[] gain = new double[1];
        int ret = nativeGetGain(image.getPlanes()[0].getBuffer(), image.getPlanes()[1].getBuffer(), image.getPlanes()[2].getBuffer(), image.getPlanes()[0].getRowStride(), image.getPlanes()[1].getRowStride(), image.getPlanes()[2].getRowStride(), image.getPlanes()[0].getPixelStride(), image.getPlanes()[1].getPixelStride(), image.getPlanes()[2].getPixelStride(), imageFormat, image.getWidth(), image.getHeight(), gain);
        if (ret != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("nativeGetGain error. ret=");
            stringBuilder.append(ret);
            LogFilter.e("Camera2App", stringBuilder.toString());
            return 1.0d;
        }
        gain[0] = GAIN_COEF * gain[0];
        return gain[0];
    }

    public int getDirection() {
        int[] directon = new int[]{-1};
        if (this.mNative != 0) {
            int ret = nativeGetDirection(this.mNative, directon);
            if (ret != 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("MorphoPanoramaGP3.getDirection error. ret=");
                stringBuilder.append(ret);
                LogFilter.e("Camera2App", stringBuilder.toString());
            }
        }
        return directon[0];
    }

    public int end(int mode, double hw_degree) {
        if (this.mNative == 0) {
            return 0;
        }
        return nativeEnd(this.mNative, mode, hw_degree);
    }

    public int getEndStatus() {
        if (this.mNative != 0) {
            return nativeGetEndStatus(this.mNative);
        }
        return -1;
    }

    public int getOutputImageSize(int[] size) {
        if (this.mNative != 0) {
            return nativeGetOutputSize(this.mNative, size);
        }
        return Error.ERROR_STATE;
    }

    public int getOutputImage(byte[] output_image, Rect rect) {
        if (this.mNative == 0) {
            return Error.ERROR_STATE;
        }
        return nativeGetOutputImage(this.mNative, output_image, rect.left, rect.top, rect.right, rect.bottom);
    }

    public int setShrinkRatio(double shrink_ratio) {
        if (this.mNative != 0) {
            return nativeSetShrinkRatio(this.mNative, shrink_ratio);
        }
        return Error.ERROR_STATE;
    }

    public int setCalcseamPixnum(int pixnum) {
        if (this.mNative != 0) {
            return nativeSetCalcseamPixnum(this.mNative, pixnum);
        }
        return Error.ERROR_STATE;
    }

    public int setInitialRotationByGravity(double x, double y, double z) {
        if (this.mNative != 0) {
            int ret = nativeSetInitialRotationByGravity(this.mNative, x, y, z);
            this.mGravity.x = x;
            this.mGravity.y = y;
            this.mGravity.z = z;
            return ret;
        }
        double d = x;
        double d2 = y;
        double d3 = z;
        return Error.ERROR_STATE;
    }

    public GravityParam getLastGravity() {
        return this.mGravity.copyInstance();
    }

    public int setUseDeform(boolean use) {
        if (this.mNative != 0) {
            return nativeSetUseDeform(this.mNative, use);
        }
        return Error.ERROR_STATE;
    }

    public int setUseLuminanceCorrection(boolean use) {
        if (this.mNative != 0) {
            return nativeSetUseLuminanceCorrection(this.mNative, use);
        }
        return Error.ERROR_STATE;
    }

    public int setSeamsearchRatio(double val) {
        if (this.mNative != 0) {
            return nativeSetSeamsearchRatio(this.mNative, val);
        }
        return Error.ERROR_STATE;
    }

    public int setZrotationCoeff(double val) {
        if (this.mNative != 0) {
            return nativeSetZrotationCoeff(this.mNative, val);
        }
        return Error.ERROR_STATE;
    }

    public int setDrawThreshold(double val) {
        if (this.mNative != 0) {
            return nativeSetDrawThreshold(this.mNative, val);
        }
        return Error.ERROR_STATE;
    }

    public int setGyroscopeData(SensorData[] sensor_data) {
        if (this.mNative != 0) {
            return nativeSetGyroscopeData(this.mNative, sensor_data);
        }
        return Error.ERROR_STATE;
    }

    public int setRotationVector(double[] val) {
        if (this.mNative != 0) {
            return nativeSetRotationVector(this.mNative, val);
        }
        return Error.ERROR_STATE;
    }

    public int setUnsharpStrength(int strength) {
        if (this.mNative != 0) {
            return nativeSetUnsharpStrength(this.mNative, strength);
        }
        return Error.ERROR_STATE;
    }

    public int setAovGain(double val) {
        if (this.mNative != 0) {
            return nativeSetAovGain(this.mNative, val);
        }
        return Error.ERROR_STATE;
    }

    public int setRotationRatio(double val) {
        if (this.mNative != 0) {
            return nativeSetRotationRatio(this.mNative, val);
        }
        return Error.ERROR_STATE;
    }

    public int setSensorUseMode(int mode) {
        if (this.mNative != 0) {
            return nativeSetSensorUseMode(this.mNative, mode);
        }
        return Error.ERROR_STATE;
    }

    public int setNoiseReductionParam(int val) {
        if (this.mNative != 0) {
            return nativeSetNoiseReductionParam(this.mNative, val);
        }
        return Error.ERROR_STATE;
    }

    public int setDistortionCorrectionParam(double val1, double val2, double val3, double val4) {
        if (this.mNative == 0) {
            return Error.ERROR_STATE;
        }
        return nativeSetDistortionCorrectionParam(this.mNative, val1, val2, val3, val4);
    }

    private static int getFD(FileDescriptor fd) {
        try {
            Field address = FileDescriptor.class.getDeclaredField("descriptor");
            address.setAccessible(true);
            return address.getInt(fd);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            return -1;
        }
    }

    public int savePanorama360(int width, int height, String path, String first_date, String last_date, boolean addMargin, GalleryInfoData galleryInfoData, boolean useEndStatus) {
        if (this.mNative == 0) {
            return Error.ERROR_STATE;
        }
        return nativeSavePanorama360(this.mNative, width, height, path, 0, first_date, last_date, addMargin, galleryInfoData, useEndStatus);
    }

    public int savePanorama360(int width, int height, FileDescriptor fd, String first_date, String last_date, boolean addMargin, GalleryInfoData galleryInfoData, boolean useEndStatus) {
        if (this.mNative == 0) {
            return Error.ERROR_STATE;
        }
        int descriptor = getFD(fd);
        if (descriptor < 0) {
            return Error.ERROR_UNSUPPORTED;
        }
        return nativeSavePanorama360(this.mNative, width, height, null, descriptor, first_date, last_date, addMargin, galleryInfoData, useEndStatus);
    }

    public int saveLog(String path, String log) {
        if (this.mNative != 0) {
            return nativeSaveLog(this.mNative, path, log);
        }
        return Error.ERROR_STATE;
    }

    public static int renderByteBuffer(ByteBuffer buffer, CaptureImage srcImage) {
        Image image = srcImage.image();
        if (image != null) {
            String imageFormat = PanoramaGP3ImageFormat.getImageFormat(image);
            ByteBuffer byteBuffer = buffer;
            return nativeRenderByteBuffer(buffer, image.getPlanes()[0].getBuffer(), image.getPlanes()[1].getBuffer(), image.getPlanes()[2].getBuffer(), image.getPlanes()[0].getRowStride(), image.getPlanes()[1].getRowStride(), image.getPlanes()[2].getRowStride(), image.getPlanes()[0].getPixelStride(), image.getPlanes()[1].getPixelStride(), image.getPlanes()[2].getPixelStride(), imageFormat, srcImage.getWidth(), srcImage.getHeight());
        }
        String imageFormat2 = srcImage.getImageFormat();
        int width = srcImage.getWidth();
        int height = srcImage.getHeight();
        return nativeRenderByteBufferRaw(buffer, srcImage.raw(), imageFormat2, srcImage.getWidth(), srcImage.getHeight());
    }

    public static int renderByteArrayForEncoder(byte[] buffer, CaptureImage srcImage) {
        Image image = srcImage.image();
        int i;
        if (image != null) {
            String imageFormat = PanoramaGP3ImageFormat.getImageFormat(image);
            i = 2;
            int i_u = imageFormat.startsWith("YUV") ? 1 : 2;
            if (!imageFormat.startsWith("YUV")) {
                i = 1;
            }
            int i_v = i;
            byte[] bArr = buffer;
            return nativeRenderByteArray(buffer, image.getPlanes()[0].getBuffer(), image.getPlanes()[i_u].getBuffer(), image.getPlanes()[i_v].getBuffer(), image.getPlanes()[0].getRowStride(), image.getPlanes()[i_u].getRowStride(), image.getPlanes()[i_v].getRowStride(), image.getPlanes()[0].getPixelStride(), image.getPlanes()[i_u].getPixelStride(), image.getPlanes()[i_v].getPixelStride(), imageFormat, srcImage.getWidth(), srcImage.getHeight());
        }
        int ret = srcImage.getImageFormat();
        i = srcImage.getWidth();
        int height = srcImage.getHeight();
        return nativeRenderByteArrayRaw(buffer, srcImage.raw(), ret, srcImage.getWidth(), srcImage.getHeight());
    }

    public static int yuv2Bitmap8888(byte[] data, int src_width, int src_height, Bitmap dst, int rotate) {
        return nativeYuv2Bitmap8888(data, src_width, src_height, dst, rotate, 1);
    }

    public static int yvu2Bitmap8888(byte[] data, int src_width, int src_height, Bitmap dst, int rotate) {
        return nativeYuv2Bitmap8888(data, src_width, src_height, dst, rotate, 0);
    }

    public int createNativeOutputInfo() {
        this.mNativeOutputInfo = createNativeOutputInfoObject();
        if (this.mNativeOutputInfo != 0) {
            return 0;
        }
        return Error.ERROR_STATE;
    }

    public int deleteNativeOutputInfo() {
        deleteNativeOutputInfoObject(this.mNativeOutputInfo);
        this.mNativeOutputInfo = 0;
        return 0;
    }

    public int createOutputImage(Rect rect) {
        if (this.mNative == 0) {
            return Error.ERROR_STATE;
        }
        return nativeCreateOutputImage(this.mNative, rect.left, rect.top, rect.right, rect.bottom);
    }

    public int preparePanorama360(int width, int height, String first_date, String last_date, boolean addMargin, GalleryInfoData galleryInfoData) {
        if (this.mNative == 0) {
            return Error.ERROR_STATE;
        }
        return nativePreparePanorama360(this.mNative, width, height, first_date, last_date, addMargin, galleryInfoData);
    }

    public int savePanorama360Delay(String path, boolean use_wdr2, int endStatus, boolean useEndStatus) {
        if (this.mNativeOutputInfo == 0) {
            return Error.ERROR_STATE;
        }
        return nativeSavePanorama360Delay(this.mNativeOutputInfo, path, 0, use_wdr2, endStatus, useEndStatus);
    }

    public int savePanorama360Delay(FileDescriptor fd, boolean use_wdr2, int endStatus, boolean useEndStatus) {
        if (this.mNativeOutputInfo == 0) {
            return Error.ERROR_STATE;
        }
        int ret;
        int descriptor = getFD(fd);
        if (descriptor >= 0) {
            ret = nativeSavePanorama360Delay(this.mNativeOutputInfo, null, descriptor, use_wdr2, endStatus, useEndStatus);
        } else {
            ret = Error.ERROR_UNSUPPORTED;
        }
        return ret;
    }

    public int setProjectionMode(int mode) {
        if (this.mNative != 0) {
            return nativeSetProjectionMode(this.mNative, mode);
        }
        return Error.ERROR_STATE;
    }

    public int setMotionDetectionMode(int mode) {
        if (this.mNative != 0) {
            return nativeSetMotionDetectionMode(this.mNative, mode);
        }
        return Error.ERROR_STATE;
    }

    public static int saveNotPanorama_sub(CaptureImage captureImage, String path, int fd) {
        int ret;
        Image image = captureImage.image();
        if (image != null) {
            String imageFormat = captureImage.getImageFormat();
            int width = captureImage.getWidth();
            int height = captureImage.getHeight();
            ret = nativeSaveNotPanorama(0, image.getPlanes()[0].getBuffer(), image.getPlanes()[1].getBuffer(), image.getPlanes()[2].getBuffer(), image.getPlanes()[0].getRowStride(), image.getPlanes()[1].getRowStride(), image.getPlanes()[2].getRowStride(), image.getPlanes()[0].getPixelStride(), image.getPlanes()[1].getPixelStride(), image.getPlanes()[2].getPixelStride(), imageFormat, width, height, path, fd);
        } else {
            Camera1Image camera1Image = (Camera1Image) captureImage;
            String imageFormat2 = camera1Image.getImageFormat();
            int width2 = camera1Image.getWidth();
            int height2 = camera1Image.getHeight();
            byte[] imageBytes = camera1Image.raw();
            ByteBuffer byteBuffer = NativeMemoryAllocator.allocateBuffer(imageBytes.length);
            byteBuffer.put(imageBytes);
            byteBuffer.clear();
            ByteBuffer byteBuffer2 = byteBuffer;
            ret = nativeSaveNotPanorama(byteBuffer, null, null, null, 0, 0, 0, 0, 0, 0, imageFormat2, width2, height2, path, fd);
            NativeMemoryAllocator.freeBuffer(byteBuffer2);
        }
        if (ret != 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("nativeSaveNotPanorama error. ret=");
            stringBuilder.append(ret);
            LogFilter.e("Camera2App", stringBuilder.toString());
        }
        return ret;
    }

    public static int saveNotPanorama(CaptureImage captureImage, FileDescriptor fd) {
        int descriptor = getFD(fd);
        if (descriptor >= 0) {
            return saveNotPanorama_sub(captureImage, null, descriptor);
        }
        return Error.ERROR_UNSUPPORTED;
    }

    public static int saveNotPanorama(CaptureImage captureImage, String path) {
        return saveNotPanorama_sub(captureImage, path, -1);
    }
}
