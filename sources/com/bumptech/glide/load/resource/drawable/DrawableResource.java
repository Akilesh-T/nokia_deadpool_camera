package com.bumptech.glide.load.resource.drawable;

import android.graphics.drawable.Drawable;
import com.bumptech.glide.load.engine.Resource;

public abstract class DrawableResource<T extends Drawable> implements Resource<T> {
    protected final T drawable;
    private boolean returnedOriginalDrawable;

    public DrawableResource(T drawable) {
        if (drawable != null) {
            this.drawable = drawable;
            return;
        }
        throw new NullPointerException("Drawable must not be null!");
    }

    public final T get() {
        if (this.returnedOriginalDrawable) {
            return this.drawable.getConstantState().newDrawable();
        }
        this.returnedOriginalDrawable = true;
        return this.drawable;
    }
}
