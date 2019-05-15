package com.megvii.beautify.jni;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;

public class BeaurifyJniSdk {
    public static final int ABILITY_TYPE_DEFAULT = 0;
    public static final int ABILITY_TYPE_IMAGE = 1;
    public static final int ABILITY_TYPE_PREVIEW = 0;
    public static final int ABILITY_TYPE_VIDEO = 2;
    public static final int MG_BEAUTIFY_ADD_PINK = 5;
    public static final int MG_BEAUTIFY_BRIGHTEN_EYE = 7;
    public static final int MG_BEAUTIFY_BRIGHTNESS = 3;
    public static final int MG_BEAUTIFY_CONTACT_LENS = 10;
    public static final int MG_BEAUTIFY_DENOISE = 4;
    public static final int MG_BEAUTIFY_ENLARGE_EYE = 1;
    public static final int MG_BEAUTIFY_EYEBROW = 9;
    public static final int MG_BEAUTIFY_HIGH_NOSEBRIDGE = 14;
    public static final int MG_BEAUTIFY_LIP = 11;
    public static final int MG_BEAUTIFY_REMOVE_EYEBAGS = 13;
    public static final int MG_BEAUTIFY_REMOVE_EYEBROW = 16;
    public static final int MG_BEAUTIFY_REMOVE_SPECKLES = 12;
    public static final int MG_BEAUTIFY_SHRINK_FACE = 2;
    public static final int MG_BEAUTIFY_SKIN_BALANCE = 17;
    public static final int MG_BEAUTIFY_THIN_FACE = 6;
    public static final int MG_BEAUTIFY_TOOTH = 8;
    public static final int MG_BEAUTIFY_UPCHEEK = 15;
    public static final int MG_LOG_LEVEL_DEBUG = 4;
    public static final int MG_LOG_LEVEL_DISABLE = 0;
    public static final int MG_LOG_LEVEL_ERROR = 3;
    public static final int MG_LOG_LEVEL_INFO = 1;
    public static final int MG_LOG_LEVEL_WARNING = 2;
    private static final int PROCESS_IMAGE_ID = 2;
    private static final int PROCESS_PREVIEW_ID = 1;
    private static BeaurifyJniSdk sImageBeaurifyJniSdk = new BeaurifyJniSdk(1);
    private static BeaurifyJniSdk sPreviewBeaurifyJniSdk = new BeaurifyJniSdk(0);
    private static BeaurifyJniSdk sVideoBeaurifyJniSdk = new BeaurifyJniSdk(2);
    private long mAbilityType = 0;
    private volatile long mObject = 0;

    @Deprecated
    public static native synchronized void alignNV21Data(byte[] bArr, int i, int i2, byte[] bArr2, int i3, int i4);

    public static native synchronized void deAlignNV21Data(byte[] bArr, int i, int i2, byte[] bArr2, int i3, int i4);

    @Deprecated
    public static native synchronized int testFacePP(byte[] bArr, byte[] bArr2, int i, int i2);

    public native synchronized int nativeChangePackage(String str);

    public native synchronized int nativeCreateBeautyHandle(Context context, int i, int i2, int i3, int i4, byte[] bArr, byte[] bArr2, byte[] bArr3);

    public native synchronized int nativeDetectFace(byte[] bArr, int i, int i2, int i3);

    public native synchronized int nativeDetectFaceOrientation(byte[] bArr, double d, double d2, int i, int i2, int i3, int i4);

    public native synchronized int nativeDisablePackage();

    public native synchronized int nativeDoneGLContext();

    public native synchronized String nativeGetBeautyVersion();

    public native synchronized int nativeGetDenseLMPoints(int[] iArr);

    public native synchronized int nativeGetPoints(int[] iArr);

    public native synchronized int nativePreparePackage(String str);

    public native synchronized int nativeProcessImage(Bitmap bitmap, Bitmap bitmap2);

    public native synchronized int nativeProcessImageInImageOutNV12(byte[] bArr, byte[] bArr2, int i, int i2);

    public native synchronized int nativeProcessImageInImageOutNV21(byte[] bArr, byte[] bArr2, int i, int i2);

    public native synchronized int nativeProcessImageInTextureOut(byte[] bArr, int i, int i2, int i3);

    public native synchronized int nativeProcessImageNV12(byte[] bArr, byte[] bArr2, int i, int i2, boolean z);

    public native synchronized int nativeProcessImageNV21(byte[] bArr, byte[] bArr2, int i, int i2, boolean z);

    public native synchronized int nativeProcessTexture(int i, int i2);

    public native synchronized int nativeReleaseResources();

    public native synchronized int nativeRemoveFilter();

    public native synchronized int nativeReset(int i, int i2, int i3);

    public native synchronized int nativeSetBeautyParam(int i, float f);

    public native synchronized int nativeSetBeautyParam2(int i, float f, int i2, int i3, int i4, Bitmap bitmap, Bitmap bitmap2, float[] fArr, int i5);

    public native synchronized int nativeSetBeautyRemoveSpeckles(float f, byte[] bArr, String str);

    public native synchronized int nativeSetDetectMode(int i);

    public native synchronized int nativeSetFilter(String str);

    public native synchronized int nativeSetLogLevel(int i);

    public native synchronized int nativeSetStickerParam(float f);

    public native synchronized int nativeShareGLContext();

    public native synchronized int nativeUseFastFilter(boolean z);

    static {
        System.loadLibrary("MGBeauty");
        System.loadLibrary("MegviiDlmk");
        System.loadLibrary("MegviiBeautify-jni");
    }

    public static synchronized BeaurifyJniSdk preViewInstance() {
        BeaurifyJniSdk beaurifyJniSdk;
        synchronized (BeaurifyJniSdk.class) {
            beaurifyJniSdk = sPreviewBeaurifyJniSdk;
        }
        return beaurifyJniSdk;
    }

    public static synchronized BeaurifyJniSdk imageInstance() {
        BeaurifyJniSdk beaurifyJniSdk;
        synchronized (BeaurifyJniSdk.class) {
            beaurifyJniSdk = sImageBeaurifyJniSdk;
        }
        return beaurifyJniSdk;
    }

    public static synchronized BeaurifyJniSdk videoInstance() {
        BeaurifyJniSdk beaurifyJniSdk;
        synchronized (BeaurifyJniSdk.class) {
            beaurifyJniSdk = sVideoBeaurifyJniSdk;
        }
        return beaurifyJniSdk;
    }

    public BeaurifyJniSdk(int type) {
        this.mAbilityType = (long) type;
    }

    public synchronized int nativeProcessImageNV21(byte[] inData, byte[] outData, int w, int h) {
        boolean isQcom;
        isQcom = false;
        if (Build.HARDWARE.matches("qcom")) {
            isQcom = true;
        }
        return nativeProcessImageNV21(inData, outData, w, h, isQcom);
    }

    public static int getAlignSize(int size) {
        int outW = (size / 64) * 64;
        if (outW != size) {
            return outW + 64;
        }
        return outW;
    }
}
