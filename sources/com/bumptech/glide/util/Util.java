package com.bumptech.glide.util;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.Build.VERSION;
import android.os.Looper;
import java.util.ArrayDeque;
import java.util.Queue;

public final class Util {
    private static final char[] HEX_CHAR_ARRAY = "0123456789abcdef".toCharArray();
    private static final char[] SHA_1_CHARS = new char[40];
    private static final char[] SHA_256_CHARS = new char[64];

    /* renamed from: com.bumptech.glide.util.Util$1 */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$android$graphics$Bitmap$Config = new int[Config.values().length];

        static {
            try {
                $SwitchMap$android$graphics$Bitmap$Config[Config.ALPHA_8.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$graphics$Bitmap$Config[Config.RGB_565.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$graphics$Bitmap$Config[Config.ARGB_4444.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$graphics$Bitmap$Config[Config.ARGB_8888.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    private Util() {
    }

    public static String sha256BytesToHex(byte[] bytes) {
        return bytesToHex(bytes, SHA_256_CHARS);
    }

    public static String sha1BytesToHex(byte[] bytes) {
        return bytesToHex(bytes, SHA_1_CHARS);
    }

    private static String bytesToHex(byte[] bytes, char[] hexChars) {
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 255;
            hexChars[j * 2] = HEX_CHAR_ARRAY[v >>> 4];
            hexChars[(j * 2) + 1] = HEX_CHAR_ARRAY[v & 15];
        }
        return new String(hexChars);
    }

    @Deprecated
    public static int getSize(Bitmap bitmap) {
        return getBitmapByteSize(bitmap);
    }

    @TargetApi(19)
    public static int getBitmapByteSize(Bitmap bitmap) {
        if (VERSION.SDK_INT >= 19) {
            try {
                return bitmap.getAllocationByteCount();
            } catch (NullPointerException e) {
            }
        }
        return bitmap.getHeight() * bitmap.getRowBytes();
    }

    public static int getBitmapByteSize(int width, int height, Config config) {
        return (width * height) * getBytesPerPixel(config);
    }

    private static int getBytesPerPixel(Config config) {
        if (config == null) {
            config = Config.ARGB_8888;
        }
        switch (AnonymousClass1.$SwitchMap$android$graphics$Bitmap$Config[config.ordinal()]) {
            case 1:
                return 1;
            case 2:
            case 3:
                return 2;
            default:
                return 4;
        }
    }

    public static void assertMainThread() {
        if (!isOnMainThread()) {
            throw new IllegalArgumentException("You must call this method on the main thread");
        }
    }

    public static void assertBackgroundThread() {
        if (!isOnBackgroundThread()) {
            throw new IllegalArgumentException("YOu must call this method on a background thread");
        }
    }

    public static boolean isOnMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public static boolean isOnBackgroundThread() {
        return isOnMainThread() ^ 1;
    }

    public static <T> Queue<T> createQueue(int size) {
        return new ArrayDeque(size);
    }
}
