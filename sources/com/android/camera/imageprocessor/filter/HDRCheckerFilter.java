package com.android.camera.imageprocessor.filter;

import android.util.Log;
import com.android.camera.imageprocessor.listener.MAlgoProcessCallback;

public class HDRCheckerFilter implements ImageFilter {
    private static final String DR_CHECK_SUGGEST_NEGATIVE_ONE = "dr_check_suggest_negative_one";
    private static final String DR_CHECK_SUGGEST_NEGATIVE_TWO = "dr_check_suggest_negative_two";
    private static final String DR_CHECK_SUGGEST_ONE = "dr_check_suggest_one";
    private static final String DR_CHECK_SUGGEST_TWO = "dr_check_suggest_two";
    private static final String DR_CHECK_SUGGEST_ZERO = "dr_check_suggest_zero";
    private static final int LOW_LIGHT_MODE = 1;
    public static final int NOISY = 600;
    private static final int NORMAL_MODE = 0;
    private static String TAG = "HDRCheckerFilter";
    private static boolean mIsSupported;
    private final int ERR_HANDLE = 0;
    private final String YUV420_SEMIPLANAR = "YVU420_SEMIPLANAR";
    private Object lock = new Object();
    private float[] mEvValue;
    private long mHandler = 0;
    private int mHeight;
    private boolean mIsInitialized = false;
    public int mMode;
    private MAlgoProcessCallback mProcessCallback;
    private int[] mResult;
    private int mStride;
    private int mWidth;

    enum SENSITIVE {
        MORPHO_HDR_CHECKER_SENSITIVITY_SENSITIVE(0),
        MORPHO_HDR_CHECKER_SENSITIVITY_NORMAL(1),
        MORPHO_HDR_CHECKER_SENSITIVITY_INSENSITIVE(2);
        
        private int value;

        private SENSITIVE(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    private native int[] evaluate(int i, int[] iArr, byte[] bArr);

    private native int[] evaluate(long j, int[] iArr, byte[] bArr);

    private native int finalize(int i);

    private native int finalize(long j);

    private native int getBufferSize(int i, int i2, int i3, String str);

    private native void getVersion();

    private native int initialize(int i, byte[] bArr, int i2, int i3, int i4, String str);

    private native long initialize(long j, byte[] bArr, int i, int i2, int i3, String str);

    private native void setSensitivity(int i, int i2);

    private native void setSensitivity(long j, int i);

    static {
        mIsSupported = false;
        try {
            System.loadLibrary("jni_hdr_checker");
            Log("jni_hdr_checker lib load successfully .");
            mIsSupported = true;
        } catch (UnsatisfiedLinkError e) {
            Log(e.toString());
            Log("jni_hdr_checker lib load failed !");
            mIsSupported = false;
        }
    }

    public HDRCheckerFilter(MAlgoProcessCallback processCallback) {
        this.mProcessCallback = processCallback;
    }

    public void process(byte[] data) {
        synchronized (this.lock) {
            if (!(!this.mIsInitialized || this.mHandler == 0 || this.mResult == null || data == null)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("process >>> E , width = ");
                stringBuilder.append(this.mWidth);
                stringBuilder.append(" , height = ");
                stringBuilder.append(this.mHeight);
                stringBuilder.append(" , stride = ");
                stringBuilder.append(this.mStride);
                Log(stringBuilder.toString());
                this.mResult = evaluate(this.mHandler, this.mResult, data);
                Log("process >>> X");
                float negativeTwo = this.mEvValue[0];
                float negativeOne = this.mEvValue[1];
                float zero = this.mEvValue[2];
                float one = this.mEvValue[3];
                float two = this.mEvValue[4];
                if (this.mProcessCallback != null) {
                    if (this.mMode == 1) {
                        if (this.mResult[3] != 0) {
                            this.mProcessCallback.onCheckerProcessDone(new float[]{zero, negativeTwo});
                        } else if (this.mResult[2] != 0) {
                            this.mProcessCallback.onCheckerProcessDone(new float[]{zero, negativeOne});
                        } else {
                            this.mProcessCallback.onCheckerProcessDone(new float[]{zero});
                        }
                    } else if (this.mResult[0] != 0) {
                        if (this.mResult[3] != 0) {
                            this.mProcessCallback.onCheckerProcessDone(new float[]{zero, negativeTwo, two});
                        } else if (this.mResult[2] != 0) {
                            this.mProcessCallback.onCheckerProcessDone(new float[]{zero, negativeOne, two});
                        } else {
                            this.mProcessCallback.onCheckerProcessDone(new float[]{zero, two});
                        }
                    } else if (this.mResult[1] != 0) {
                        if (this.mResult[3] != 0) {
                            this.mProcessCallback.onCheckerProcessDone(new float[]{zero, negativeTwo, one});
                        } else if (this.mResult[2] != 0) {
                            this.mProcessCallback.onCheckerProcessDone(new float[]{zero, negativeOne, one});
                        } else {
                            this.mProcessCallback.onCheckerProcessDone(new float[]{zero, one});
                        }
                    } else if (this.mResult[3] != 0) {
                        this.mProcessCallback.onCheckerProcessDone(new float[]{zero, negativeTwo});
                    } else if (this.mResult[2] != 0) {
                        this.mProcessCallback.onCheckerProcessDone(new float[]{zero, negativeOne});
                    } else {
                        this.mProcessCallback.onCheckerProcessDone(new float[]{zero});
                    }
                }
            }
        }
    }

    public void deinit() {
        synchronized (this.lock) {
            if (this.mIsInitialized && this.mHandler != 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("deinit >>> E , width = ");
                stringBuilder.append(this.mWidth);
                stringBuilder.append(" , height = ");
                stringBuilder.append(this.mHeight);
                stringBuilder.append(" , stride = ");
                stringBuilder.append(this.mStride);
                Log(stringBuilder.toString());
                this.mIsInitialized = false;
                finalize(this.mHandler);
                this.mHandler = 0;
                MAlgoProcessCallback mAlgoProcessCallback = this.mProcessCallback;
                this.mResult = null;
                Log("deinit >>> X");
            }
        }
    }

    public boolean isInitiallized() {
        return this.mIsInitialized;
    }

    public boolean isSupported() {
        return mIsSupported;
    }

    public void init(int width, int height, int yStride, int iso, float[] evValue) {
        int i = width;
        int i2 = height;
        int i3 = yStride;
        if (this.mIsInitialized || this.mHandler != 0) {
            int i4 = iso;
            float[] fArr = evValue;
            return;
        }
        Log("init >>> E");
        this.mWidth = i;
        this.mHeight = i2;
        this.mStride = i3;
        this.mEvValue = evValue;
        getVersion();
        this.mResult = new int[4];
        int buffer_size = getBufferSize(i, i2, i3, "YVU420_SEMIPLANAR");
        byte[] buffers = new byte[buffer_size];
        byte[] buffers2 = buffers;
        this.mHandler = initialize(this.mHandler, buffers, buffer_size, i, i2, "YVU420_SEMIPLANAR");
        if (iso > 600) {
            this.mMode = 1;
        } else {
            this.mMode = 0;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DR CHECK mode : ");
        stringBuilder.append(this.mMode);
        Log.d(str, stringBuilder.toString());
        if (this.mHandler != 0) {
            setSensitivity(this.mHandler, SENSITIVE.MORPHO_HDR_CHECKER_SENSITIVITY_NORMAL.getValue());
            this.mIsInitialized = true;
            Log("init >>> X");
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("mHandler = ");
        stringBuilder2.append(this.mHandler);
        stringBuilder2.append(" , buffers = ");
        stringBuilder2.append(buffers2);
        stringBuilder2.append(", buffer_size = ");
        stringBuilder2.append(buffer_size);
        stringBuilder2.append(" , width = ");
        stringBuilder2.append(i);
        stringBuilder2.append(" , height = ");
        stringBuilder2.append(i2);
        stringBuilder2.append(" , mFormat = ");
        stringBuilder2.append("YVU420_SEMIPLANAR");
        Log(stringBuilder2.toString());
    }

    private static void Log(String msg) {
        Log.d(TAG, msg);
    }
}
