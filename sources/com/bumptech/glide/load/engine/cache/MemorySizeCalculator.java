package com.bumptech.glide.load.engine.cache;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build.VERSION;
import android.util.DisplayMetrics;
import android.util.Log;

public class MemorySizeCalculator {
    static final int BITMAP_POOL_TARGET_SCREENS = 3;
    static final int BYTES_PER_ARGB_8888_PIXEL = 4;
    static final float LOW_MEMORY_MAX_SIZE_MULTIPLIER = 0.33f;
    static final float MAX_SIZE_MULTIPLIER = 0.4f;
    static final int MEMORY_CACHE_TARGET_SCREENS = 2;
    private static final String TAG = "MemorySizeCalculator";
    private final int bitmapPoolSize;
    private final int memoryCacheSize;

    interface ScreenDimensions {
        int getHeightPixels();

        int getWidthPixels();
    }

    private static class DisplayMetricsScreenDimensions implements ScreenDimensions {
        private final DisplayMetrics displayMetrics;

        public DisplayMetricsScreenDimensions(DisplayMetrics displayMetrics) {
            this.displayMetrics = displayMetrics;
        }

        public int getWidthPixels() {
            return this.displayMetrics.widthPixels;
        }

        public int getHeightPixels() {
            return this.displayMetrics.heightPixels;
        }
    }

    public MemorySizeCalculator(Context context) {
        this((ActivityManager) context.getSystemService("activity"), new DisplayMetricsScreenDimensions(context.getResources().getDisplayMetrics()));
    }

    MemorySizeCalculator(ActivityManager activityManager, ScreenDimensions screenDimensions) {
        int maxSize = getMaxSize(activityManager);
        int screenSize = (screenDimensions.getWidthPixels() * screenDimensions.getHeightPixels()) * 4;
        int targetPoolSize = screenSize * 3;
        int targetMemoryCacheSize = screenSize * 2;
        if (targetMemoryCacheSize + targetPoolSize <= maxSize) {
            this.memoryCacheSize = targetMemoryCacheSize;
            this.bitmapPoolSize = targetPoolSize;
        } else {
            int part = Math.round(((float) maxSize) / 1084227584);
            this.memoryCacheSize = part * 2;
            this.bitmapPoolSize = part * 3;
        }
        if (Log.isLoggable(TAG, 3)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Calculated memory cache size: ");
            stringBuilder.append(toMb(this.memoryCacheSize));
            stringBuilder.append(" pool size: ");
            stringBuilder.append(toMb(this.bitmapPoolSize));
            stringBuilder.append(" memory class limited? ");
            stringBuilder.append(targetMemoryCacheSize + targetPoolSize > maxSize);
            stringBuilder.append(" max size: ");
            stringBuilder.append(toMb(maxSize));
            stringBuilder.append(" memoryClass: ");
            stringBuilder.append(activityManager.getMemoryClass());
            stringBuilder.append(" isLowMemoryDevice: ");
            stringBuilder.append(isLowMemoryDevice(activityManager));
            Log.d(str, stringBuilder.toString());
        }
    }

    public int getMemoryCacheSize() {
        return this.memoryCacheSize;
    }

    public int getBitmapPoolSize() {
        return this.bitmapPoolSize;
    }

    private static int getMaxSize(ActivityManager activityManager) {
        return Math.round(((float) ((activityManager.getMemoryClass() * 1024) * 1024)) * (isLowMemoryDevice(activityManager) ? LOW_MEMORY_MAX_SIZE_MULTIPLIER : MAX_SIZE_MULTIPLIER));
    }

    private static int toMb(int bytes) {
        return bytes / 1048576;
    }

    @TargetApi(19)
    private static boolean isLowMemoryDevice(ActivityManager activityManager) {
        int sdkInt = VERSION.SDK_INT;
        return sdkInt < 11 || (sdkInt >= 19 && activityManager.isLowRamDevice());
    }
}
