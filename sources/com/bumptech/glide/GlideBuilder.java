package com.bumptech.glide;

import android.content.Context;
import android.os.Build.VERSION;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPoolAdapter;
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.DiskCacheAdapter;
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.bumptech.glide.load.engine.executor.FifoPriorityThreadPoolExecutor;
import java.io.File;
import java.util.concurrent.ExecutorService;

public class GlideBuilder {
    private BitmapPool bitmapPool;
    private final Context context;
    private DecodeFormat decodeFormat;
    private DiskCache diskCache;
    private ExecutorService diskCacheService;
    private Engine engine;
    private MemoryCache memoryCache;
    private ExecutorService sourceService;

    public GlideBuilder(Context context) {
        this.context = context.getApplicationContext();
    }

    public GlideBuilder setBitmapPool(BitmapPool bitmapPool) {
        this.bitmapPool = bitmapPool;
        return this;
    }

    public GlideBuilder setMemoryCache(MemoryCache memoryCache) {
        this.memoryCache = memoryCache;
        return this;
    }

    public GlideBuilder setDiskCache(DiskCache diskCache) {
        this.diskCache = diskCache;
        return this;
    }

    public GlideBuilder setResizeService(ExecutorService service) {
        this.sourceService = service;
        return this;
    }

    public GlideBuilder setDiskCacheService(ExecutorService service) {
        this.diskCacheService = service;
        return this;
    }

    public GlideBuilder setDecodeFormat(DecodeFormat decodeFormat) {
        this.decodeFormat = decodeFormat;
        return this;
    }

    /* Access modifiers changed, original: 0000 */
    public GlideBuilder setEngine(Engine engine) {
        this.engine = engine;
        return this;
    }

    /* Access modifiers changed, original: 0000 */
    public Glide createGlide() {
        if (this.sourceService == null) {
            this.sourceService = new FifoPriorityThreadPoolExecutor(Math.max(1, Runtime.getRuntime().availableProcessors()));
        }
        if (this.diskCacheService == null) {
            this.diskCacheService = new FifoPriorityThreadPoolExecutor(1);
        }
        MemorySizeCalculator calculator = new MemorySizeCalculator(this.context);
        if (this.bitmapPool == null) {
            if (VERSION.SDK_INT >= 11) {
                this.bitmapPool = new LruBitmapPool(calculator.getBitmapPoolSize());
            } else {
                this.bitmapPool = new BitmapPoolAdapter();
            }
        }
        if (this.memoryCache == null) {
            this.memoryCache = new LruResourceCache(calculator.getMemoryCacheSize());
        }
        if (this.diskCache == null) {
            File cacheDir = Glide.getPhotoCacheDir(this.context);
            if (cacheDir != null) {
                this.diskCache = DiskLruCacheWrapper.get(cacheDir, 262144000);
            }
            if (this.diskCache == null) {
                this.diskCache = new DiskCacheAdapter();
            }
        }
        if (this.engine == null) {
            this.engine = new Engine(this.memoryCache, this.diskCache, this.diskCacheService, this.sourceService);
        }
        if (this.decodeFormat == null) {
            this.decodeFormat = DecodeFormat.DEFAULT;
        }
        return new Glide(this.engine, this.memoryCache, this.bitmapPool, this.context, this.decodeFormat);
    }
}
