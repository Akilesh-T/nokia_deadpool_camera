package com.bumptech.glide;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import com.bumptech.glide.load.Encoder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.Engine;
import com.bumptech.glide.load.resource.UnitTransformation;
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder;
import com.bumptech.glide.manager.Lifecycle;
import com.bumptech.glide.manager.RequestTracker;
import com.bumptech.glide.provider.ChildLoadProvider;
import com.bumptech.glide.provider.LoadProvider;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.GenericRequest;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.RequestCoordinator;
import com.bumptech.glide.request.RequestFutureTarget;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.ThumbnailRequestCoordinator;
import com.bumptech.glide.request.animation.GlideAnimationFactory;
import com.bumptech.glide.request.animation.NoAnimation;
import com.bumptech.glide.request.animation.ViewAnimationFactory;
import com.bumptech.glide.request.animation.ViewPropertyAnimation.Animator;
import com.bumptech.glide.request.animation.ViewPropertyAnimationFactory;
import com.bumptech.glide.request.target.PreloadTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.signature.EmptySignature;
import com.bumptech.glide.util.Util;
import java.io.File;

public class GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> implements Cloneable {
    private GlideAnimationFactory<TranscodeType> animationFactory;
    protected final Context context;
    private DiskCacheStrategy diskCacheStrategy;
    private int errorId;
    private Drawable errorPlaceholder;
    protected final Glide glide;
    private boolean isCacheable;
    private boolean isModelSet;
    private boolean isTransformationSet;
    protected final Lifecycle lifecycle;
    private ChildLoadProvider<ModelType, DataType, ResourceType, TranscodeType> loadProvider;
    private ModelType model;
    protected final Class<ModelType> modelClass;
    private int overrideHeight;
    private int overrideWidth;
    private Drawable placeholderDrawable;
    private int placeholderId;
    private Priority priority;
    private RequestListener<? super ModelType, TranscodeType> requestListener;
    protected final RequestTracker requestTracker;
    private Key signature;
    private Float sizeMultiplier;
    private Float thumbSizeMultiplier;
    private GenericRequestBuilder<?, ?, ?, TranscodeType> thumbnailRequestBuilder;
    protected final Class<TranscodeType> transcodeClass;
    private Transformation<ResourceType> transformation;

    /* renamed from: com.bumptech.glide.GenericRequestBuilder$2 */
    static /* synthetic */ class AnonymousClass2 {
        static final /* synthetic */ int[] $SwitchMap$android$widget$ImageView$ScaleType = new int[ScaleType.values().length];

        static {
            try {
                $SwitchMap$android$widget$ImageView$ScaleType[ScaleType.CENTER_CROP.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$widget$ImageView$ScaleType[ScaleType.FIT_CENTER.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$widget$ImageView$ScaleType[ScaleType.FIT_START.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$widget$ImageView$ScaleType[ScaleType.FIT_END.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    GenericRequestBuilder(LoadProvider<ModelType, DataType, ResourceType, TranscodeType> loadProvider, Class<TranscodeType> transcodeClass, GenericRequestBuilder<ModelType, ?, ?, ?> other) {
        this(other.context, other.modelClass, loadProvider, transcodeClass, other.glide, other.requestTracker, other.lifecycle);
        this.model = other.model;
        this.isModelSet = other.isModelSet;
        this.signature = other.signature;
        this.diskCacheStrategy = other.diskCacheStrategy;
        this.isCacheable = other.isCacheable;
    }

    GenericRequestBuilder(Context context, Class<ModelType> modelClass, LoadProvider<ModelType, DataType, ResourceType, TranscodeType> loadProvider, Class<TranscodeType> transcodeClass, Glide glide, RequestTracker requestTracker, Lifecycle lifecycle) {
        this.signature = EmptySignature.obtain();
        this.sizeMultiplier = Float.valueOf(1.0f);
        ChildLoadProvider childLoadProvider = null;
        this.priority = null;
        this.isCacheable = true;
        this.animationFactory = NoAnimation.getFactory();
        this.overrideHeight = -1;
        this.overrideWidth = -1;
        this.diskCacheStrategy = DiskCacheStrategy.RESULT;
        this.transformation = UnitTransformation.get();
        this.context = context;
        this.modelClass = modelClass;
        this.transcodeClass = transcodeClass;
        this.glide = glide;
        this.requestTracker = requestTracker;
        this.lifecycle = lifecycle;
        if (loadProvider != null) {
            childLoadProvider = new ChildLoadProvider(loadProvider);
        }
        this.loadProvider = childLoadProvider;
        if (context == null) {
            throw new NullPointerException("Context can't be null");
        } else if (modelClass != null && loadProvider == null) {
            throw new NullPointerException("LoadProvider must not be null");
        }
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> thumbnail(GenericRequestBuilder<?, ?, ?, TranscodeType> thumbnailRequest) {
        this.thumbnailRequestBuilder = thumbnailRequest;
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> thumbnail(float sizeMultiplier) {
        if (sizeMultiplier < 0.0f || sizeMultiplier > 1.0f) {
            throw new IllegalArgumentException("sizeMultiplier must be between 0 and 1");
        }
        this.thumbSizeMultiplier = Float.valueOf(sizeMultiplier);
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> sizeMultiplier(float sizeMultiplier) {
        if (sizeMultiplier < 0.0f || sizeMultiplier > 1.0f) {
            throw new IllegalArgumentException("sizeMultiplier must be between 0 and 1");
        }
        this.sizeMultiplier = Float.valueOf(sizeMultiplier);
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> decoder(ResourceDecoder<DataType, ResourceType> decoder) {
        if (this.loadProvider != null) {
            this.loadProvider.setSourceDecoder(decoder);
        }
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> cacheDecoder(ResourceDecoder<File, ResourceType> cacheDecoder) {
        if (this.loadProvider != null) {
            this.loadProvider.setCacheDecoder(cacheDecoder);
        }
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> sourceEncoder(Encoder<DataType> sourceEncoder) {
        if (this.loadProvider != null) {
            this.loadProvider.setSourceEncoder(sourceEncoder);
        }
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> diskCacheStrategy(DiskCacheStrategy strategy) {
        this.diskCacheStrategy = strategy;
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> encoder(ResourceEncoder<ResourceType> encoder) {
        if (this.loadProvider != null) {
            this.loadProvider.setEncoder(encoder);
        }
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> priority(Priority priority) {
        this.priority = priority;
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> transform(Transformation<ResourceType>... transformations) {
        this.isTransformationSet = true;
        if (transformations.length == 1) {
            this.transformation = transformations[0];
        } else {
            this.transformation = new MultiTransformation((Transformation[]) transformations);
        }
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> dontTransform() {
        return transform(UnitTransformation.get());
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> transcoder(ResourceTranscoder<ResourceType, TranscodeType> transcoder) {
        if (this.loadProvider != null) {
            this.loadProvider.setTranscoder(transcoder);
        }
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> dontAnimate() {
        return animate(NoAnimation.getFactory());
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> animate(int animationId) {
        return animate(new ViewAnimationFactory(this.context, animationId));
    }

    @Deprecated
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> animate(Animation animation) {
        return animate(new ViewAnimationFactory(animation));
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> animate(Animator animator) {
        return animate(new ViewPropertyAnimationFactory(animator));
    }

    /* Access modifiers changed, original: 0000 */
    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> animate(GlideAnimationFactory<TranscodeType> animationFactory) {
        if (animationFactory != null) {
            this.animationFactory = animationFactory;
            return this;
        }
        throw new NullPointerException("Animation factory must not be null!");
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> placeholder(int resourceId) {
        this.placeholderId = resourceId;
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> placeholder(Drawable drawable) {
        this.placeholderDrawable = drawable;
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> error(int resourceId) {
        this.errorId = resourceId;
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> error(Drawable drawable) {
        this.errorPlaceholder = drawable;
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> listener(RequestListener<? super ModelType, TranscodeType> requestListener) {
        this.requestListener = requestListener;
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> skipMemoryCache(boolean skip) {
        this.isCacheable = skip ^ 1;
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> override(int width, int height) {
        if (width <= 0) {
            throw new IllegalArgumentException("Width must be > 0");
        } else if (height > 0) {
            this.overrideWidth = width;
            this.overrideHeight = height;
            return this;
        } else {
            throw new IllegalArgumentException("Height must be > 0");
        }
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> signature(Key signature) {
        if (signature != null) {
            this.signature = signature;
            return this;
        }
        throw new NullPointerException("Signature must not be null");
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> load(ModelType model) {
        this.model = model;
        this.isModelSet = true;
        return this;
    }

    public GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> clone() {
        try {
            GenericRequestBuilder<ModelType, DataType, ResourceType, TranscodeType> clone = (GenericRequestBuilder) super.clone();
            clone.loadProvider = this.loadProvider != null ? this.loadProvider.clone() : null;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public <Y extends Target<TranscodeType>> Y into(Y target) {
        Util.assertMainThread();
        if (target == null) {
            throw new IllegalArgumentException("You must pass in a non null Target");
        } else if (this.isModelSet) {
            Request previous = target.getRequest();
            if (previous != null) {
                previous.clear();
                this.requestTracker.removeRequest(previous);
                previous.recycle();
            }
            Request request = buildRequest(target);
            target.setRequest(request);
            this.lifecycle.addListener(target);
            this.requestTracker.runRequest(request);
            return target;
        } else {
            throw new IllegalArgumentException("You must first set a model (try #load())");
        }
    }

    public Target<TranscodeType> into(ImageView view) {
        Util.assertMainThread();
        if (view != null) {
            if (!(this.isTransformationSet || view.getScaleType() == null)) {
                switch (AnonymousClass2.$SwitchMap$android$widget$ImageView$ScaleType[view.getScaleType().ordinal()]) {
                    case 1:
                        applyCenterCrop();
                        break;
                    case 2:
                    case 3:
                    case 4:
                        applyFitCenter();
                        break;
                }
            }
            return into(this.glide.buildImageViewTarget(view, this.transcodeClass));
        }
        throw new IllegalArgumentException("You must pass in a non null View");
    }

    public FutureTarget<TranscodeType> into(int width, int height) {
        final RequestFutureTarget<ModelType, TranscodeType> target = new RequestFutureTarget(this.glide.getMainHandler(), width, height);
        this.glide.getMainHandler().post(new Runnable() {
            public void run() {
                if (!target.isCancelled()) {
                    GenericRequestBuilder.this.into(target);
                }
            }
        });
        return target;
    }

    public Target<TranscodeType> preload(int width, int height) {
        return into(PreloadTarget.obtain(width, height));
    }

    /* Access modifiers changed, original: 0000 */
    public void applyCenterCrop() {
    }

    /* Access modifiers changed, original: 0000 */
    public void applyFitCenter() {
    }

    private Priority getThumbnailPriority() {
        if (this.priority == Priority.LOW) {
            return Priority.NORMAL;
        }
        if (this.priority == Priority.NORMAL) {
            return Priority.HIGH;
        }
        return Priority.IMMEDIATE;
    }

    private Request buildRequest(Target<TranscodeType> target) {
        if (this.priority == null) {
            this.priority = Priority.NORMAL;
        }
        return buildRequestRecursive(target, null);
    }

    private Request buildRequestRecursive(Target<TranscodeType> target, ThumbnailRequestCoordinator parentCoordinator) {
        ThumbnailRequestCoordinator coordinator;
        if (this.thumbnailRequestBuilder != null) {
            if (this.thumbnailRequestBuilder.animationFactory.equals(NoAnimation.getFactory())) {
                this.thumbnailRequestBuilder.animationFactory = this.animationFactory;
            }
            if (this.thumbnailRequestBuilder.priority == null) {
                this.thumbnailRequestBuilder.priority = getThumbnailPriority();
            }
            coordinator = new ThumbnailRequestCoordinator(parentCoordinator);
            coordinator.setRequests(obtainRequest(target, this.sizeMultiplier.floatValue(), this.priority, coordinator), this.thumbnailRequestBuilder.buildRequestRecursive(target, coordinator));
            return coordinator;
        } else if (this.thumbSizeMultiplier == null) {
            return obtainRequest(target, this.sizeMultiplier.floatValue(), this.priority, parentCoordinator);
        } else {
            coordinator = new ThumbnailRequestCoordinator(parentCoordinator);
            coordinator.setRequests(obtainRequest(target, this.sizeMultiplier.floatValue(), this.priority, coordinator), obtainRequest(target, this.thumbSizeMultiplier.floatValue(), getThumbnailPriority(), coordinator));
            return coordinator;
        }
    }

    private Request obtainRequest(Target<TranscodeType> target, float sizeMultiplier, Priority priority, RequestCoordinator requestCoordinator) {
        ChildLoadProvider childLoadProvider = this.loadProvider;
        Object obj = this.model;
        Key key = this.signature;
        Context context = this.context;
        Drawable drawable = this.placeholderDrawable;
        int i = this.placeholderId;
        Drawable drawable2 = this.errorPlaceholder;
        int i2 = this.errorId;
        RequestListener requestListener = this.requestListener;
        Engine engine = this.glide.getEngine();
        Transformation transformation = this.transformation;
        Class cls = this.transcodeClass;
        boolean z = this.isCacheable;
        GlideAnimationFactory glideAnimationFactory = this.animationFactory;
        int i3 = this.overrideWidth;
        Transformation transformation2 = transformation;
        return GenericRequest.obtain(childLoadProvider, obj, key, context, priority, target, sizeMultiplier, drawable, i, drawable2, i2, requestListener, requestCoordinator, engine, transformation2, cls, z, glideAnimationFactory, i3, this.overrideHeight, this.diskCacheStrategy);
    }
}
