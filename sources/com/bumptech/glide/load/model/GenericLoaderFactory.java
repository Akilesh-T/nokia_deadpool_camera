package com.bumptech.glide.load.model;

import android.content.Context;
import com.bumptech.glide.load.data.DataFetcher;
import java.util.HashMap;
import java.util.Map;

public class GenericLoaderFactory {
    private static final ModelLoader NULL_MODEL_LOADER = new ModelLoader() {
        public DataFetcher getResourceFetcher(Object model, int width, int height) {
            throw new NoSuchMethodError("This should never be called!");
        }

        public String toString() {
            return "NULL_MODEL_LOADER";
        }
    };
    private final Map<Class, Map<Class, ModelLoader>> cachedModelLoaders = new HashMap();
    private final Context context;
    private final Map<Class, Map<Class, ModelLoaderFactory>> modelClassToResourceFactories = new HashMap();

    public GenericLoaderFactory(Context context) {
        this.context = context.getApplicationContext();
    }

    public synchronized <T, Y> ModelLoaderFactory<T, Y> unregister(Class<T> modelClass, Class<Y> resourceClass) {
        ModelLoaderFactory result;
        this.cachedModelLoaders.clear();
        result = null;
        Map<Class, ModelLoaderFactory> resourceToFactories = (Map) this.modelClassToResourceFactories.get(modelClass);
        if (resourceToFactories != null) {
            result = (ModelLoaderFactory) resourceToFactories.remove(resourceClass);
        }
        return result;
    }

    public synchronized <T, Y> ModelLoaderFactory<T, Y> register(Class<T> modelClass, Class<Y> resourceClass, ModelLoaderFactory<T, Y> factory) {
        ModelLoaderFactory previous;
        this.cachedModelLoaders.clear();
        Map<Class, ModelLoaderFactory> resourceToFactories = (Map) this.modelClassToResourceFactories.get(modelClass);
        if (resourceToFactories == null) {
            resourceToFactories = new HashMap();
            this.modelClassToResourceFactories.put(modelClass, resourceToFactories);
        }
        previous = (ModelLoaderFactory) resourceToFactories.put(resourceClass, factory);
        if (previous != null) {
            for (Map<Class, ModelLoaderFactory> factories : this.modelClassToResourceFactories.values()) {
                if (factories.containsValue(previous)) {
                    previous = null;
                    break;
                }
            }
        }
        return previous;
    }

    @Deprecated
    public synchronized <T, Y> ModelLoader<T, Y> buildModelLoader(Class<T> modelClass, Class<Y> resourceClass, Context context) {
        return buildModelLoader(modelClass, resourceClass);
    }

    /* JADX WARNING: Missing block: B:17:0x0029, code skipped:
            return r0;
     */
    public synchronized <T, Y> com.bumptech.glide.load.model.ModelLoader<T, Y> buildModelLoader(java.lang.Class<T> r4, java.lang.Class<Y> r5) {
        /*
        r3 = this;
        monitor-enter(r3);
        r0 = r3.getCachedLoader(r4, r5);	 Catch:{ all -> 0x002a }
        if (r0 == 0) goto L_0x0014;
    L_0x0007:
        r1 = NULL_MODEL_LOADER;	 Catch:{ all -> 0x002a }
        r1 = r1.equals(r0);	 Catch:{ all -> 0x002a }
        if (r1 == 0) goto L_0x0012;
    L_0x000f:
        r1 = 0;
        monitor-exit(r3);
        return r1;
    L_0x0012:
        monitor-exit(r3);
        return r0;
    L_0x0014:
        r1 = r3.getFactory(r4, r5);	 Catch:{ all -> 0x002a }
        if (r1 == 0) goto L_0x0025;
    L_0x001a:
        r2 = r3.context;	 Catch:{ all -> 0x002a }
        r2 = r1.build(r2, r3);	 Catch:{ all -> 0x002a }
        r0 = r2;
        r3.cacheModelLoader(r4, r5, r0);	 Catch:{ all -> 0x002a }
        goto L_0x0028;
    L_0x0025:
        r3.cacheNullLoader(r4, r5);	 Catch:{ all -> 0x002a }
    L_0x0028:
        monitor-exit(r3);
        return r0;
    L_0x002a:
        r4 = move-exception;
        monitor-exit(r3);
        throw r4;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.bumptech.glide.load.model.GenericLoaderFactory.buildModelLoader(java.lang.Class, java.lang.Class):com.bumptech.glide.load.model.ModelLoader");
    }

    private <T, Y> void cacheNullLoader(Class<T> modelClass, Class<Y> resourceClass) {
        cacheModelLoader(modelClass, resourceClass, NULL_MODEL_LOADER);
    }

    private <T, Y> void cacheModelLoader(Class<T> modelClass, Class<Y> resourceClass, ModelLoader<T, Y> modelLoader) {
        Map<Class, ModelLoader> resourceToLoaders = (Map) this.cachedModelLoaders.get(modelClass);
        if (resourceToLoaders == null) {
            resourceToLoaders = new HashMap();
            this.cachedModelLoaders.put(modelClass, resourceToLoaders);
        }
        resourceToLoaders.put(resourceClass, modelLoader);
    }

    private <T, Y> ModelLoader<T, Y> getCachedLoader(Class<T> modelClass, Class<Y> resourceClass) {
        Map<Class, ModelLoader> resourceToLoaders = (Map) this.cachedModelLoaders.get(modelClass);
        if (resourceToLoaders != null) {
            return (ModelLoader) resourceToLoaders.get(resourceClass);
        }
        return null;
    }

    private <T, Y> ModelLoaderFactory<T, Y> getFactory(Class<T> modelClass, Class<Y> resourceClass) {
        Map<Class, ModelLoaderFactory> resourceToFactories = (Map) this.modelClassToResourceFactories.get(modelClass);
        ModelLoaderFactory result = null;
        if (resourceToFactories != null) {
            result = (ModelLoaderFactory) resourceToFactories.get(resourceClass);
        }
        if (result == null) {
            for (Class<? super T> registeredModelClass : this.modelClassToResourceFactories.keySet()) {
                if (registeredModelClass.isAssignableFrom(modelClass)) {
                    Map<Class, ModelLoaderFactory> currentResourceToFactories = (Map) this.modelClassToResourceFactories.get(registeredModelClass);
                    if (currentResourceToFactories != null) {
                        result = (ModelLoaderFactory) currentResourceToFactories.get(resourceClass);
                        if (result != null) {
                            break;
                        }
                    } else {
                        continue;
                    }
                }
            }
        }
        return result;
    }
}
