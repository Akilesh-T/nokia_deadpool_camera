package com.bumptech.glide.load.model;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import com.bumptech.glide.load.data.DataFetcher;

public class ResourceLoader<T> implements ModelLoader<Integer, T> {
    private final Resources resources;
    private final ModelLoader<Uri, T> uriLoader;

    public ResourceLoader(Context context, ModelLoader<Uri, T> uriLoader) {
        this(context.getResources(), (ModelLoader) uriLoader);
    }

    public ResourceLoader(Resources resources, ModelLoader<Uri, T> uriLoader) {
        this.resources = resources;
        this.uriLoader = uriLoader;
    }

    public DataFetcher<T> getResourceFetcher(Integer model, int width, int height) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("android.resource://");
        stringBuilder.append(this.resources.getResourcePackageName(model.intValue()));
        stringBuilder.append('/');
        stringBuilder.append(this.resources.getResourceTypeName(model.intValue()));
        stringBuilder.append('/');
        stringBuilder.append(this.resources.getResourceEntryName(model.intValue()));
        return this.uriLoader.getResourceFetcher(Uri.parse(stringBuilder.toString()), width, height);
    }
}
