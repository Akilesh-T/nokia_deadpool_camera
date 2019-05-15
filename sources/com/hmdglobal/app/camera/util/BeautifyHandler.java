package com.hmdglobal.app.camera.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.beauty.util.ConUtil;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.megvii.beautify.jni.BeaurifyJniSdk;
import com.morphoinc.utils.multimedia.MediaProviderUtils;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BeautifyHandler {
    private static final Tag TAG = new Tag("BeautifyHandler");
    public static boolean isFinished = false;
    public static Executor mExecutor = Executors.newSingleThreadExecutor();
    public static Object mLock = new Object();
    private static int state = 0;

    public static synchronized void release() {
        synchronized (BeautifyHandler.class) {
            new Timer().start("BeautifyHandler.release");
            BeaurifyJniSdk.imageInstance().nativeShareGLContext();
            BeaurifyJniSdk.imageInstance().nativeReleaseResources();
        }
    }

    public static synchronized void init(final Context context) {
        synchronized (BeautifyHandler.class) {
            synchronized (mLock) {
                isFinished = false;
                mExecutor.execute(new Runnable() {
                    public void run() {
                        BeaurifyJniSdk beautySdk = BeaurifyJniSdk.imageInstance();
                        beautySdk.nativeShareGLContext();
                        beautySdk.nativeSetLogLevel(4);
                        beautySdk.nativeCreateBeautyHandle(context, 128, 128, MediaProviderUtils.ROTATION_270, 1, ConUtil.getFileContent(context, R.raw.mgbeautify_1_2_4_model), ConUtil.getFileContent(context, R.raw.detect_model), null);
                        beautySdk.nativeDoneGLContext();
                        synchronized (BeautifyHandler.mLock) {
                            BeautifyHandler.isFinished = true;
                            BeautifyHandler.mLock.notifyAll();
                        }
                    }
                });
                while (!isFinished) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static synchronized byte[] processImageNV21(Context context, byte[] in_nv21, int nv21_width, int nv21_height, float val, int angle) {
        byte[] bArr = in_nv21;
        synchronized (BeautifyHandler.class) {
            Timer timer = new Timer();
            timer.start("BeautifyHandler.processImageNV21");
            byte[] out_nv21 = new byte[bArr.length];
            synchronized (mLock) {
                isFinished = false;
                final int i = nv21_width;
                final int i2 = nv21_height;
                final byte[] bArr2 = bArr;
                final int i3 = angle;
                final float f = val;
                final byte[] bArr3 = out_nv21;
                mExecutor.execute(new Runnable() {
                    public void run() {
                        String str;
                        BeaurifyJniSdk beautySdk = BeaurifyJniSdk.imageInstance();
                        beautySdk.nativeShareGLContext();
                        beautySdk.nativeSetLogLevel(4);
                        Tag access$000 = BeautifyHandler.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("width = ");
                        stringBuilder.append(i);
                        stringBuilder.append("  height ");
                        stringBuilder.append(i2);
                        stringBuilder.append("    size ");
                        stringBuilder.append(bArr2);
                        if (stringBuilder.toString() == null) {
                            str = "0 ";
                        } else {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(" ");
                            stringBuilder.append(bArr2.length);
                            str = stringBuilder.toString();
                        }
                        Log.d(access$000, str);
                        beautySdk.nativeReset(i, i2, i3);
                        beautySdk.nativeSetBeautyParam(4, f);
                        beautySdk.nativeSetBeautyParam(3, f);
                        beautySdk.nativeSetBeautyParam(5, f);
                        beautySdk.nativeSetBeautyParam(6, f);
                        beautySdk.nativeSetBeautyParam(1, f);
                        BeautifyHandler.state = 0;
                        BeautifyHandler.state = beautySdk.nativeProcessImageNV21(bArr2, bArr3, i, i2);
                        beautySdk.nativeDoneGLContext();
                        synchronized (BeautifyHandler.mLock) {
                            BeautifyHandler.isFinished = true;
                            BeautifyHandler.mLock.notifyAll();
                        }
                        Tag access$0002 = BeautifyHandler.TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("thread id ");
                        stringBuilder2.append(Thread.currentThread().getId());
                        stringBuilder2.append("  time stamp  ");
                        stringBuilder2.append(System.currentTimeMillis());
                        stringBuilder2.append("  state ");
                        stringBuilder2.append(BeautifyHandler.state);
                        Log.d(access$0002, stringBuilder2.toString());
                    }
                });
                while (!isFinished) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            timer.end();
            if (state != 0) {
                return bArr;
            }
            return out_nv21;
        }
    }

    public static synchronized Bitmap processImageBitmap(Context context, Bitmap in_nv21, int nv21_width, int nv21_height, float val, int angle) {
        synchronized (BeautifyHandler.class) {
            Timer timer = new Timer();
            timer.start("BeautifyHandler.processImageNV21");
            Bitmap outBmp = Bitmap.createBitmap(in_nv21.getWidth(), in_nv21.getHeight(), Config.ARGB_8888);
            synchronized (mLock) {
                isFinished = false;
                final int i = nv21_width;
                final int i2 = nv21_height;
                final int i3 = angle;
                final float f = val;
                final Bitmap bitmap = in_nv21;
                final Bitmap bitmap2 = outBmp;
                mExecutor.execute(new Runnable() {
                    public void run() {
                        BeaurifyJniSdk beautySdk = BeaurifyJniSdk.imageInstance();
                        beautySdk.nativeShareGLContext();
                        beautySdk.nativeSetLogLevel(4);
                        beautySdk.nativeReset(i, i2, i3);
                        beautySdk.nativeSetBeautyParam(4, f);
                        beautySdk.nativeSetBeautyParam(3, f);
                        beautySdk.nativeSetBeautyParam(5, f);
                        beautySdk.nativeSetBeautyParam(6, f);
                        beautySdk.nativeSetBeautyParam(1, f);
                        BeautifyHandler.state = 0;
                        BeautifyHandler.state = beautySdk.nativeProcessImage(bitmap, bitmap2);
                        beautySdk.nativeDoneGLContext();
                        synchronized (BeautifyHandler.mLock) {
                            BeautifyHandler.isFinished = true;
                            BeautifyHandler.mLock.notifyAll();
                        }
                        Tag access$000 = BeautifyHandler.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("thread id ");
                        stringBuilder.append(Thread.currentThread().getId());
                        stringBuilder.append("  time stamp  ");
                        stringBuilder.append(System.currentTimeMillis());
                        stringBuilder.append("  state ");
                        stringBuilder.append(BeautifyHandler.state);
                        Log.d(access$000, stringBuilder.toString());
                    }
                });
                while (!isFinished) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            timer.end();
            if (state != 0) {
                return in_nv21;
            }
            return outBmp;
        }
    }
}
