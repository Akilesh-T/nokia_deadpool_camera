package com.bumptech.glide.request;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.engine.Engine.LoadStatus;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.animation.GlideAnimationFactory;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.util.LogTime;
import com.bumptech.glide.util.Util;
import java.util.Queue;

public final class GenericRequest<A, T, Z, R> implements Request, SizeReadyCallback, ResourceCallback {
    private static final Queue<GenericRequest<?, ?, ?, ?>> REQUEST_POOL = Util.createQueue(0);
    private static final String TAG = "GenericRequest";
    private static final double TO_MEGABYTE = 9.5367431640625E-7d;
    private GlideAnimationFactory<R> animationFactory;
    private Context context;
    private DiskCacheStrategy diskCacheStrategy;
    private Engine engine;
    private Drawable errorDrawable;
    private int errorResourceId;
    private boolean isMemoryCacheable;
    private LoadProvider<A, T, Z, R> loadProvider;
    private LoadStatus loadStatus;
    private boolean loadedFromMemoryCache;
    private A model;
    private int overrideHeight;
    private int overrideWidth;
    private Drawable placeholderDrawable;
    private int placeholderResourceId;
    private Priority priority;
    private RequestCoordinator requestCoordinator;
    private RequestListener<? super A, R> requestListener;
    private Resource<?> resource;
    private Key signature;
    private float sizeMultiplier;
    private long startTime;
    private Status status;
    private final String tag = String.valueOf(hashCode());
    private Target<R> target;
    private Class<R> transcodeClass;
    private Transformation<Z> transformation;

    private enum Status {
        PENDING,
        RUNNING,
        WAITING_FOR_SIZE,
        COMPLETE,
        FAILED,
        CANCELLED,
        PAUSED
    }

    public static <A, T, Z, R> GenericRequest<A, T, Z, R> obtain(LoadProvider<A, T, Z, R> loadProvider, A model, Key signature, Context context, Priority priority, Target<R> target, float sizeMultiplier, Drawable placeholderDrawable, int placeholderResourceId, Drawable errorDrawable, int errorResourceId, RequestListener<? super A, R> requestListener, RequestCoordinator requestCoordinator, Engine engine, Transformation<Z> transformation, Class<R> transcodeClass, boolean isMemoryCacheable, GlideAnimationFactory<R> animationFactory, int overrideWidth, int overrideHeight, DiskCacheStrategy diskCacheStrategy) {
        GenericRequest<A, T, Z, R> request = (GenericRequest) REQUEST_POOL.poll();
        if (request == null) {
            request = new GenericRequest();
        }
        request.init(loadProvider, model, signature, context, priority, target, sizeMultiplier, placeholderDrawable, placeholderResourceId, errorDrawable, errorResourceId, requestListener, requestCoordinator, engine, transformation, transcodeClass, isMemoryCacheable, animationFactory, overrideWidth, overrideHeight, diskCacheStrategy);
        return request;
    }

    private GenericRequest() {
    }

    public void recycle() {
        this.loadProvider = null;
        this.model = null;
        this.context = null;
        this.target = null;
        this.placeholderDrawable = null;
        this.errorDrawable = null;
        this.requestListener = null;
        this.requestCoordinator = null;
        this.transformation = null;
        this.animationFactory = null;
        this.loadedFromMemoryCache = false;
        this.loadStatus = null;
        REQUEST_POOL.offer(this);
    }

    private void init(LoadProvider<A, T, Z, R> loadProvider, A model, Key signature, Context context, Priority priority, Target<R> target, float sizeMultiplier, Drawable placeholderDrawable, int placeholderResourceId, Drawable errorDrawable, int errorResourceId, RequestListener<? super A, R> requestListener, RequestCoordinator requestCoordinator, Engine engine, Transformation<Z> transformation, Class<R> transcodeClass, boolean isMemoryCacheable, GlideAnimationFactory<R> animationFactory, int overrideWidth, int overrideHeight, DiskCacheStrategy diskCacheStrategy) {
        A a = model;
        Transformation<Z> transformation2 = transformation;
        this.loadProvider = loadProvider;
        this.model = a;
        this.signature = signature;
        this.context = context.getApplicationContext();
        this.priority = priority;
        this.target = target;
        this.sizeMultiplier = sizeMultiplier;
        this.placeholderDrawable = placeholderDrawable;
        this.placeholderResourceId = placeholderResourceId;
        this.errorDrawable = errorDrawable;
        this.errorResourceId = errorResourceId;
        this.requestListener = requestListener;
        this.requestCoordinator = requestCoordinator;
        this.engine = engine;
        this.transformation = transformation2;
        this.transcodeClass = transcodeClass;
        this.isMemoryCacheable = isMemoryCacheable;
        this.animationFactory = animationFactory;
        this.overrideWidth = overrideWidth;
        this.overrideHeight = overrideHeight;
        this.diskCacheStrategy = diskCacheStrategy;
        this.status = Status.PENDING;
        if (a != null) {
            check("ModelLoader", loadProvider.getModelLoader(), "try .using(ModelLoader)");
            check("Transcoder", loadProvider.getTranscoder(), "try .as*(Class).transcode(ResourceTranscoder)");
            check("Transformation", transformation2, "try .transform(UnitTransformation.get())");
            if (diskCacheStrategy.cacheSource()) {
                check("SourceEncoder", loadProvider.getSourceEncoder(), "try .sourceEncoder(Encoder) or .diskCacheStrategy(NONE/RESULT)");
            } else {
                check("SourceDecoder", loadProvider.getSourceDecoder(), "try .decoder/.imageDecoder/.videoDecoder(ResourceDecoder) or .diskCacheStrategy(ALL/SOURCE)");
            }
            if (diskCacheStrategy.cacheSource() || diskCacheStrategy.cacheResult()) {
                check("CacheDecoder", loadProvider.getCacheDecoder(), "try .cacheDecoder(ResouceDecoder) or .diskCacheStrategy(NONE)");
            }
            if (diskCacheStrategy.cacheResult()) {
                check("Encoder", loadProvider.getEncoder(), "try .encode(ResourceEncoder) or .diskCacheStrategy(NONE/SOURCE)");
            }
        }
    }

    private static void check(String name, Object object, String suggestion) {
        if (object == null) {
            StringBuilder message = new StringBuilder(name);
            message.append(" must not be null");
            if (suggestion != null) {
                message.append(", ");
                message.append(suggestion);
            }
            throw new NullPointerException(message.toString());
        }
    }

    public void begin() {
        this.startTime = LogTime.getLogTime();
        if (this.model == null) {
            onException(null);
            return;
        }
        this.status = Status.WAITING_FOR_SIZE;
        if (this.overrideWidth <= 0 || this.overrideHeight <= 0) {
            this.target.getSize(this);
        } else {
            onSizeReady(this.overrideWidth, this.overrideHeight);
        }
        if (!(isComplete() || isFailed() || !canNotifyStatusChanged())) {
            this.target.onLoadStarted(getPlaceholderDrawable());
        }
        if (Log.isLoggable(TAG, 2)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("finished run method in ");
            stringBuilder.append(LogTime.getElapsedMillis(this.startTime));
            logV(stringBuilder.toString());
        }
    }

    /* Access modifiers changed, original: 0000 */
    public void cancel() {
        this.status = Status.CANCELLED;
        if (this.loadStatus != null) {
            this.loadStatus.cancel();
            this.loadStatus = null;
        }
    }

    public void clear() {
        Util.assertMainThread();
        cancel();
        if (this.resource != null) {
            releaseResource(this.resource);
        }
        if (canNotifyStatusChanged()) {
            this.target.onLoadCleared(getPlaceholderDrawable());
        }
    }

    public boolean isPaused() {
        return this.status == Status.PAUSED;
    }

    public void pause() {
        clear();
        this.status = Status.PAUSED;
    }

    private void releaseResource(Resource resource) {
        this.engine.release(resource);
        this.resource = null;
    }

    public boolean isRunning() {
        return this.status == Status.RUNNING || this.status == Status.WAITING_FOR_SIZE;
    }

    public boolean isComplete() {
        return this.status == Status.COMPLETE;
    }

    public boolean isResourceSet() {
        return isComplete();
    }

    public boolean isCancelled() {
        return this.status == Status.CANCELLED;
    }

    public boolean isFailed() {
        return this.status == Status.FAILED;
    }

    private void setErrorPlaceholder(Exception e) {
        if (canNotifyStatusChanged()) {
            Drawable error = getErrorDrawable();
            if (error == null) {
                error = getPlaceholderDrawable();
            }
            this.target.onLoadFailed(e, error);
        }
    }

    private Drawable getErrorDrawable() {
        if (this.errorDrawable == null && this.errorResourceId > 0) {
            this.errorDrawable = this.context.getResources().getDrawable(this.errorResourceId);
        }
        return this.errorDrawable;
    }

    private Drawable getPlaceholderDrawable() {
        if (this.placeholderDrawable == null && this.placeholderResourceId > 0) {
            this.placeholderDrawable = this.context.getResources().getDrawable(this.placeholderResourceId);
        }
        return this.placeholderDrawable;
    }

    public void onSizeReady(int width, int height) {
        StringBuilder stringBuilder;
        if (Log.isLoggable(TAG, 2)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Got onSizeReady in ");
            stringBuilder.append(LogTime.getElapsedMillis(this.startTime));
            logV(stringBuilder.toString());
        }
        if (this.status == Status.WAITING_FOR_SIZE) {
            this.status = Status.RUNNING;
            int width2 = Math.round(this.sizeMultiplier * ((float) width));
            int height2 = Math.round(this.sizeMultiplier * ((float) height));
            ModelLoader<A, T> modelLoader = this.loadProvider.getModelLoader();
            DataFetcher<T> dataFetcher = modelLoader.getResourceFetcher(this.model, width2, height2);
            if (dataFetcher == null) {
                onException(new Exception("Got null fetcher from model loader"));
                return;
            }
            ResourceTranscoder<Z, R> transcoder = this.loadProvider.getTranscoder();
            if (Log.isLoggable(TAG, 2)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("finished setup for calling load in ");
                stringBuilder.append(LogTime.getElapsedMillis(this.startTime));
                logV(stringBuilder.toString());
            }
            this.loadedFromMemoryCache = true;
            boolean z = true;
            this.loadStatus = this.engine.load(this.signature, width2, height2, dataFetcher, this.loadProvider, this.transformation, transcoder, this.priority, this.isMemoryCacheable, this.diskCacheStrategy, this);
            this.loadedFromMemoryCache = this.resource != null ? z : false;
            if (Log.isLoggable(TAG, 2)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("finished onSizeReady in ");
                stringBuilder.append(LogTime.getElapsedMillis(this.startTime));
                logV(stringBuilder.toString());
            }
        }
    }

    private boolean canSetResource() {
        return this.requestCoordinator == null || this.requestCoordinator.canSetImage(this);
    }

    private boolean canNotifyStatusChanged() {
        return this.requestCoordinator == null || this.requestCoordinator.canNotifyStatusChanged(this);
    }

    private boolean isFirstReadyResource() {
        return this.requestCoordinator == null || !this.requestCoordinator.isAnyResourceSet();
    }

    public void onResourceReady(Resource<?> resource) {
        if (resource == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Expected to receive a Resource<R> with an object of ");
            stringBuilder.append(this.transcodeClass);
            stringBuilder.append(" inside, but instead got null.");
            onException(new Exception(stringBuilder.toString()));
            return;
        }
        Object received = resource.get();
        if (received == null || !this.transcodeClass.isAssignableFrom(received.getClass())) {
            releaseResource(resource);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Expected to receive an object of ");
            stringBuilder2.append(this.transcodeClass);
            stringBuilder2.append(" but instead got ");
            stringBuilder2.append(received != null ? received.getClass() : "");
            stringBuilder2.append("{");
            stringBuilder2.append(received);
            stringBuilder2.append("} inside Resource{");
            stringBuilder2.append(resource);
            stringBuilder2.append("}.");
            stringBuilder2.append(received != null ? "" : " To indicate failure return a null Resource object, rather than a Resource object containing null data.");
            onException(new Exception(stringBuilder2.toString()));
        } else if (canSetResource()) {
            onResourceReady(resource, received);
        } else {
            releaseResource(resource);
            this.status = Status.COMPLETE;
        }
    }

    /* JADX WARNING: Missing block: B:3:0x0015, code skipped:
            if (r7.requestListener.onResourceReady(r9, r7.model, r7.target, r7.loadedFromMemoryCache, isFirstReadyResource()) == false) goto L_0x0017;
     */
    private void onResourceReady(com.bumptech.glide.load.engine.Resource<?> r8, R r9) {
        /*
        r7 = this;
        r0 = r7.requestListener;
        if (r0 == 0) goto L_0x0017;
    L_0x0004:
        r1 = r7.requestListener;
        r3 = r7.model;
        r4 = r7.target;
        r5 = r7.loadedFromMemoryCache;
        r6 = r7.isFirstReadyResource();
        r2 = r9;
        r0 = r1.onResourceReady(r2, r3, r4, r5, r6);
        if (r0 != 0) goto L_0x0028;
    L_0x0017:
        r0 = r7.animationFactory;
        r1 = r7.loadedFromMemoryCache;
        r2 = r7.isFirstReadyResource();
        r0 = r0.build(r1, r2);
        r1 = r7.target;
        r1.onResourceReady(r9, r0);
    L_0x0028:
        r0 = com.bumptech.glide.request.GenericRequest.Status.COMPLETE;
        r7.status = r0;
        r7.resource = r8;
        r0 = "GenericRequest";
        r1 = 2;
        r0 = android.util.Log.isLoggable(r0, r1);
        if (r0 == 0) goto L_0x006b;
    L_0x0037:
        r0 = new java.lang.StringBuilder;
        r0.<init>();
        r1 = "Resource ready in ";
        r0.append(r1);
        r1 = r7.startTime;
        r1 = com.bumptech.glide.util.LogTime.getElapsedMillis(r1);
        r0.append(r1);
        r1 = " size: ";
        r0.append(r1);
        r1 = r8.getSize();
        r1 = (double) r1;
        r3 = 4517110426252607488; // 0x3eb0000000000000 float:0.0 double:9.5367431640625E-7;
        r1 = r1 * r3;
        r0.append(r1);
        r1 = " fromCache: ";
        r0.append(r1);
        r1 = r7.loadedFromMemoryCache;
        r0.append(r1);
        r0 = r0.toString();
        r7.logV(r0);
    L_0x006b:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.bumptech.glide.request.GenericRequest.onResourceReady(com.bumptech.glide.load.engine.Resource, java.lang.Object):void");
    }

    public void onException(Exception e) {
        if (Log.isLoggable(TAG, 3)) {
            Log.d(TAG, "load failed", e);
        }
        this.status = Status.FAILED;
        if (this.requestListener == null || !this.requestListener.onException(e, this.model, this.target, isFirstReadyResource())) {
            setErrorPlaceholder(e);
        }
    }

    private void logV(String message) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(message);
        stringBuilder.append(" this: ");
        stringBuilder.append(this.tag);
        Log.v(str, stringBuilder.toString());
    }
}
