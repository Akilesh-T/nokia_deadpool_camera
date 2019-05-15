package com.bumptech.glide.load.engine.bitmap_recycle;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.Build.VERSION;
import android.util.Log;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class LruBitmapPool implements BitmapPool {
    private static final Config DEFAULT_CONFIG = Config.ARGB_8888;
    private static final String TAG = "LruBitmapPool";
    private int currentSize;
    private int evictions;
    private int hits;
    private final int initialMaxSize;
    private int maxSize;
    private int misses;
    private int puts;
    private final LruPoolStrategy strategy;
    private final BitmapTracker tracker;

    private interface BitmapTracker {
        void add(Bitmap bitmap);

        void remove(Bitmap bitmap);
    }

    private static class NullBitmapTracker implements BitmapTracker {
        private NullBitmapTracker() {
        }

        public void add(Bitmap bitmap) {
        }

        public void remove(Bitmap bitmap) {
        }
    }

    private static class ThrowingBitmapTracker implements BitmapTracker {
        private final Set<Bitmap> bitmaps = Collections.synchronizedSet(new HashSet());

        private ThrowingBitmapTracker() {
        }

        public void add(Bitmap bitmap) {
            if (this.bitmaps.contains(bitmap)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Can't add already added bitmap: ");
                stringBuilder.append(bitmap);
                stringBuilder.append(" [");
                stringBuilder.append(bitmap.getWidth());
                stringBuilder.append("x");
                stringBuilder.append(bitmap.getHeight());
                stringBuilder.append("]");
                throw new IllegalStateException(stringBuilder.toString());
            }
            this.bitmaps.add(bitmap);
        }

        public void remove(Bitmap bitmap) {
            if (this.bitmaps.contains(bitmap)) {
                this.bitmaps.remove(bitmap);
                return;
            }
            throw new IllegalStateException("Cannot remove bitmap not in tracker");
        }
    }

    LruBitmapPool(int maxSize, LruPoolStrategy strategy) {
        this.initialMaxSize = maxSize;
        this.maxSize = maxSize;
        this.strategy = strategy;
        this.tracker = new NullBitmapTracker();
    }

    public LruBitmapPool(int maxSize) {
        this(maxSize, getDefaultStrategy());
    }

    public int getMaxSize() {
        return this.maxSize;
    }

    public synchronized void setSizeMultiplier(float sizeMultiplier) {
        this.maxSize = Math.round(((float) this.initialMaxSize) * sizeMultiplier);
        evict();
    }

    public synchronized boolean put(Bitmap bitmap) {
        if (bitmap.isMutable()) {
            if (this.strategy.getSize(bitmap) <= this.maxSize) {
                int size = this.strategy.getSize(bitmap);
                this.strategy.put(bitmap);
                this.tracker.add(bitmap);
                this.puts++;
                this.currentSize += size;
                if (Log.isLoggable(TAG, 2)) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Put bitmap in pool=");
                    stringBuilder.append(this.strategy.logBitmap(bitmap));
                    Log.v(str, stringBuilder.toString());
                }
                dump();
                evict();
                return true;
            }
        }
        if (Log.isLoggable(TAG, 2)) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Reject bitmap from pool=");
            stringBuilder2.append(this.strategy.logBitmap(bitmap));
            stringBuilder2.append(" is mutable=");
            stringBuilder2.append(bitmap.isMutable());
            Log.v(str2, stringBuilder2.toString());
        }
        return false;
    }

    private void evict() {
        trimToSize(this.maxSize);
    }

    public synchronized Bitmap get(int width, int height, Config config) {
        Bitmap result;
        result = getDirty(width, height, config);
        if (result != null) {
            result.eraseColor(0);
        }
        return result;
    }

    @TargetApi(12)
    public synchronized Bitmap getDirty(int width, int height, Config config) {
        Bitmap result;
        result = this.strategy.get(width, height, config != null ? config : DEFAULT_CONFIG);
        if (result == null) {
            if (Log.isLoggable(TAG, 3)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Missing bitmap=");
                stringBuilder.append(this.strategy.logBitmap(width, height, config));
                Log.d(str, stringBuilder.toString());
            }
            this.misses++;
        } else {
            this.hits++;
            this.currentSize -= this.strategy.getSize(result);
            this.tracker.remove(result);
            if (VERSION.SDK_INT >= 12) {
                result.setHasAlpha(true);
            }
        }
        if (Log.isLoggable(TAG, 2)) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Get bitmap=");
            stringBuilder2.append(this.strategy.logBitmap(width, height, config));
            Log.v(str2, stringBuilder2.toString());
        }
        dump();
        return result;
    }

    public void clearMemory() {
        trimToSize(0);
    }

    @SuppressLint({"InlinedApi"})
    public void trimMemory(int level) {
        if (level >= 60) {
            clearMemory();
        } else if (level >= 40) {
            trimToSize(this.maxSize / 2);
        }
    }

    private synchronized void trimToSize(int size) {
        while (this.currentSize > size) {
            Bitmap removed = this.strategy.removeLast();
            this.tracker.remove(removed);
            this.currentSize -= this.strategy.getSize(removed);
            removed.recycle();
            this.evictions++;
            if (Log.isLoggable(TAG, 3)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Evicting bitmap=");
                stringBuilder.append(this.strategy.logBitmap(removed));
                Log.d(str, stringBuilder.toString());
            }
            dump();
        }
    }

    private void dump() {
        if (Log.isLoggable(TAG, 2)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Hits=");
            stringBuilder.append(this.hits);
            stringBuilder.append(" misses=");
            stringBuilder.append(this.misses);
            stringBuilder.append(" puts=");
            stringBuilder.append(this.puts);
            stringBuilder.append(" evictions=");
            stringBuilder.append(this.evictions);
            stringBuilder.append(" currentSize=");
            stringBuilder.append(this.currentSize);
            stringBuilder.append(" maxSize=");
            stringBuilder.append(this.maxSize);
            stringBuilder.append("\nStrategy=");
            stringBuilder.append(this.strategy);
            Log.v(str, stringBuilder.toString());
        }
    }

    private static LruPoolStrategy getDefaultStrategy() {
        if (VERSION.SDK_INT >= 19) {
            return new SizeStrategy();
        }
        return new AttributeStrategy();
    }
}
