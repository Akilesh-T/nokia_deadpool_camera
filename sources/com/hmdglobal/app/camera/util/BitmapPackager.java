package com.hmdglobal.app.camera.util;

import android.graphics.Bitmap;
import java.lang.ref.WeakReference;

public class BitmapPackager extends WeakReference<Bitmap> {
    private Bitmap mBitmap;

    public BitmapPackager(Bitmap r) {
        super(r);
        this.mBitmap = r;
    }

    public void clear() {
        super.clear();
        if (this.mBitmap.isRecycled()) {
            this.mBitmap = null;
            return;
        }
        this.mBitmap.recycle();
        this.mBitmap = null;
    }

    public boolean enqueue() {
        return super.enqueue();
    }

    public Bitmap get() {
        return this.mBitmap;
    }

    public boolean isEnqueued() {
        return super.isEnqueued();
    }
}
