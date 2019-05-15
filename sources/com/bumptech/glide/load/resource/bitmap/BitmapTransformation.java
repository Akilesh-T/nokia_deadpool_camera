package com.bumptech.glide.load.resource.bitmap;

import android.content.Context;
import android.graphics.Bitmap;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;

public abstract class BitmapTransformation implements Transformation<Bitmap> {
    private BitmapPool bitmapPool;

    public abstract Bitmap transform(BitmapPool bitmapPool, Bitmap bitmap, int i, int i2);

    public BitmapTransformation(Context context) {
        this(Glide.get(context).getBitmapPool());
    }

    public BitmapTransformation(BitmapPool bitmapPool) {
        this.bitmapPool = bitmapPool;
    }

    public final Resource<Bitmap> transform(Resource<Bitmap> resource, int outWidth, int outHeight) {
        if (outWidth <= 0 || outHeight <= 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Cannot apply transformation on width: ");
            stringBuilder.append(outWidth);
            stringBuilder.append(" or height: ");
            stringBuilder.append(outHeight);
            stringBuilder.append(" less than or equal to zero");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        Bitmap toTransform = (Bitmap) resource.get();
        Bitmap transformed = transform(this.bitmapPool, toTransform, outWidth, outHeight);
        if (toTransform.equals(transformed)) {
            return resource;
        }
        return BitmapResource.obtain(transformed, this.bitmapPool);
    }
}
