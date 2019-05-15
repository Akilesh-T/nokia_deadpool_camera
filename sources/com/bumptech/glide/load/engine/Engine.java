package com.bumptech.glide.load.engine;

import android.os.Looper;
import android.os.MessageQueue.IdleHandler;
import android.util.Log;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.MemoryCache;
import com.bumptech.glide.load.engine.cache.MemoryCache.ResourceRemovedListener;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.provider.DataLoadProvider;
import com.bumptech.glide.request.ResourceCallback;
import com.bumptech.glide.util.LogTime;
import com.bumptech.glide.util.Util;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class Engine implements EngineJobListener, ResourceRemovedListener, ResourceListener {
    private static final String TAG = "Engine";
    private final Map<Key, WeakReference<EngineResource<?>>> activeResources;
    private final MemoryCache cache;
    private final DiskCache diskCache;
    private final EngineJobFactory engineJobFactory;
    private final Map<Key, EngineJob> jobs;
    private final EngineKeyFactory keyFactory;
    private final ResourceRecycler resourceRecycler;
    private final ReferenceQueue<EngineResource<?>> resourceReferenceQueue;

    static class EngineJobFactory {
        private final ExecutorService diskCacheService;
        private final EngineJobListener listener;
        private final ExecutorService sourceService;

        public EngineJobFactory(ExecutorService diskCacheService, ExecutorService sourceService, EngineJobListener listener) {
            this.diskCacheService = diskCacheService;
            this.sourceService = sourceService;
            this.listener = listener;
        }

        public EngineJob build(Key key, boolean isMemoryCacheable) {
            return new EngineJob(key, this.diskCacheService, this.sourceService, isMemoryCacheable, this.listener);
        }
    }

    public static class LoadStatus {
        private final ResourceCallback cb;
        private final EngineJob engineJob;

        public LoadStatus(ResourceCallback cb, EngineJob engineJob) {
            this.cb = cb;
            this.engineJob = engineJob;
        }

        public void cancel() {
            this.engineJob.removeCallback(this.cb);
        }
    }

    private static class RefQueueIdleHandler implements IdleHandler {
        private final Map<Key, WeakReference<EngineResource<?>>> activeResources;
        private final ReferenceQueue<EngineResource<?>> queue;

        public RefQueueIdleHandler(Map<Key, WeakReference<EngineResource<?>>> activeResources, ReferenceQueue<EngineResource<?>> queue) {
            this.activeResources = activeResources;
            this.queue = queue;
        }

        public boolean queueIdle() {
            ResourceWeakReference ref = (ResourceWeakReference) this.queue.poll();
            if (ref != null) {
                this.activeResources.remove(ref.key);
            }
            return true;
        }
    }

    private static class ResourceWeakReference extends WeakReference<EngineResource<?>> {
        private final Key key;

        public ResourceWeakReference(Key key, EngineResource<?> r, ReferenceQueue<? super EngineResource<?>> q) {
            super(r, q);
            this.key = key;
        }
    }

    public Engine(MemoryCache memoryCache, DiskCache diskCache, ExecutorService diskCacheService, ExecutorService sourceService) {
        this(memoryCache, diskCache, diskCacheService, sourceService, null, null, null, null, null);
    }

    Engine(MemoryCache cache, DiskCache diskCache, ExecutorService diskCacheService, ExecutorService sourceService, Map<Key, EngineJob> jobs, EngineKeyFactory keyFactory, Map<Key, WeakReference<EngineResource<?>>> activeResources, EngineJobFactory engineJobFactory, ResourceRecycler resourceRecycler) {
        this.cache = cache;
        this.diskCache = diskCache;
        if (activeResources == null) {
            activeResources = new HashMap();
        }
        this.activeResources = activeResources;
        if (keyFactory == null) {
            keyFactory = new EngineKeyFactory();
        }
        this.keyFactory = keyFactory;
        if (jobs == null) {
            jobs = new HashMap();
        }
        this.jobs = jobs;
        if (engineJobFactory == null) {
            engineJobFactory = new EngineJobFactory(diskCacheService, sourceService, this);
        }
        this.engineJobFactory = engineJobFactory;
        if (resourceRecycler == null) {
            resourceRecycler = new ResourceRecycler();
        }
        this.resourceRecycler = resourceRecycler;
        this.resourceReferenceQueue = new ReferenceQueue();
        Looper.myQueue().addIdleHandler(new RefQueueIdleHandler(activeResources, this.resourceReferenceQueue));
        cache.setResourceRemovedListener(this);
    }

    public <T, Z, R> LoadStatus load(Key signature, int width, int height, DataFetcher<T> fetcher, DataLoadProvider<T, Z> loadProvider, Transformation<Z> transformation, ResourceTranscoder<Z, R> transcoder, Priority priority, boolean isMemoryCacheable, DiskCacheStrategy diskCacheStrategy, ResourceCallback cb) {
        ResourceCallback resourceCallback = cb;
        Util.assertMainThread();
        long startTime = LogTime.getLogTime();
        EngineKey key = this.keyFactory.buildKey(fetcher.getId(), signature, width, height, loadProvider.getCacheDecoder(), loadProvider.getSourceDecoder(), transformation, loadProvider.getEncoder(), transcoder, loadProvider.getSourceEncoder());
        EngineResource<?> cached = getFromCache(key);
        if (cached != null) {
            cached.acquire();
            this.activeResources.put(key, new ResourceWeakReference(key, cached, this.resourceReferenceQueue));
            resourceCallback.onResourceReady(cached);
            if (Log.isLoggable(TAG, 2)) {
                logWithTimeAndKey("Loaded resource from cache", startTime, key);
            }
            return null;
        }
        WeakReference<EngineResource<?>> activeRef = (WeakReference) this.activeResources.get(key);
        if (activeRef != null) {
            EngineResource<?> active = (EngineResource) activeRef.get();
            if (active != null) {
                active.acquire();
                resourceCallback.onResourceReady(active);
                if (Log.isLoggable(TAG, 2)) {
                    logWithTimeAndKey("Loaded resource from active resources", startTime, key);
                }
                return null;
            }
            this.activeResources.remove(key);
        }
        EngineJob current = (EngineJob) this.jobs.get(key);
        if (current != null) {
            current.addCallback(resourceCallback);
            if (Log.isLoggable(TAG, 2)) {
                logWithTimeAndKey("Added to existing load", startTime, key);
            }
            return new LoadStatus(resourceCallback, current);
        }
        EngineJob engineJob = this.engineJobFactory.build(key, isMemoryCacheable);
        EngineRunnable runnable = new EngineRunnable(engineJob, new DecodeJob(key, width, height, fetcher, loadProvider, transformation, transcoder, this.diskCache, diskCacheStrategy, priority), priority);
        this.jobs.put(key, engineJob);
        engineJob.addCallback(resourceCallback);
        engineJob.start(runnable);
        if (Log.isLoggable(TAG, 2)) {
            logWithTimeAndKey("Started new load", startTime, key);
        }
        return new LoadStatus(resourceCallback, engineJob);
    }

    private static void logWithTimeAndKey(String log, long startTime, Key key) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(log);
        stringBuilder.append(" in ");
        stringBuilder.append(LogTime.getElapsedMillis(startTime));
        stringBuilder.append("ms, key: ");
        stringBuilder.append(key);
        Log.v(str, stringBuilder.toString());
    }

    private EngineResource<?> getFromCache(Key key) {
        Resource<?> cached = this.cache.remove(key);
        if (cached == null) {
            return null;
        }
        if (cached instanceof EngineResource) {
            return (EngineResource) cached;
        }
        return new EngineResource(cached, true);
    }

    public void release(Resource resource) {
        if (resource instanceof EngineResource) {
            ((EngineResource) resource).release();
            return;
        }
        throw new IllegalArgumentException("Cannot release anything but an EngineResource");
    }

    public void onEngineJobComplete(Key key, EngineResource<?> resource) {
        if (resource != null) {
            resource.setResourceListener(key, this);
            this.activeResources.put(key, new ResourceWeakReference(key, resource, this.resourceReferenceQueue));
        }
        this.jobs.remove(key);
    }

    public void onEngineJobCancelled(EngineJob engineJob, Key key) {
        if (engineJob.equals((EngineJob) this.jobs.get(key))) {
            this.jobs.remove(key);
        }
    }

    public void onResourceRemoved(Resource<?> resource) {
        this.resourceRecycler.recycle(resource);
    }

    public void onResourceReleased(Key cacheKey, EngineResource resource) {
        this.activeResources.remove(cacheKey);
        if (resource.isCacheable()) {
            this.cache.put(cacheKey, resource);
        } else {
            this.resourceRecycler.recycle(resource);
        }
    }
}
