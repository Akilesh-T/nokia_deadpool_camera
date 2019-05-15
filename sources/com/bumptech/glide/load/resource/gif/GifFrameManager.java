package com.bumptech.glide.load.resource.gif;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.bumptech.glide.GenericRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.NullEncoder;
import com.bumptech.glide.load.resource.UnitTransformation;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.UUID;

class GifFrameManager {
    private DelayTarget current;
    private final GifDecoder decoder;
    private boolean isLoadInProgress;
    private final Handler mainHandler;
    private DelayTarget next;
    private final GenericRequestBuilder<GifDecoder, GifDecoder, Bitmap, Bitmap> requestBuilder;
    private final FrameSignature signature;
    private final int targetHeight;
    private final int targetWidth;
    private Transformation<Bitmap> transformation;

    public interface FrameCallback {
        void onFrameRead(int i);
    }

    private static class FrameSignature implements Key {
        private int id;
        private final UUID uuid = UUID.randomUUID();

        public void increment() {
            this.id++;
        }

        public boolean equals(Object o) {
            boolean z = false;
            if (!(o instanceof FrameSignature)) {
                return false;
            }
            FrameSignature other = (FrameSignature) o;
            if (other.uuid.equals(this.uuid) && this.id == other.id) {
                z = true;
            }
            return z;
        }

        public int hashCode() {
            return (31 * this.uuid.hashCode()) + this.id;
        }

        public void updateDiskCacheKey(MessageDigest messageDigest) throws UnsupportedEncodingException {
            throw new UnsupportedOperationException("Not implemented");
        }
    }

    class DelayTarget extends SimpleTarget<Bitmap> implements Runnable {
        private final FrameCallback cb;
        private int index;
        private Bitmap resource;
        private final long targetTime;

        public DelayTarget(FrameCallback cb, long targetTime) {
            super(GifFrameManager.this.targetWidth, GifFrameManager.this.targetHeight);
            this.cb = cb;
            this.targetTime = targetTime;
        }

        public void setFrameIndex(int index) {
            this.index = index;
        }

        public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
            this.resource = resource;
            GifFrameManager.this.mainHandler.postAtTime(this, this.targetTime);
        }

        public void run() {
            GifFrameManager.this.isLoadInProgress = false;
            this.cb.onFrameRead(this.index);
            if (GifFrameManager.this.current != null) {
                final DelayTarget recycleCurrent = GifFrameManager.this.current;
                GifFrameManager.this.mainHandler.post(new Runnable() {
                    public void run() {
                        Glide.clear(recycleCurrent);
                    }
                });
            }
            GifFrameManager.this.current = this;
        }

        public void onLoadCleared(Drawable placeholder) {
            this.resource = null;
        }
    }

    public GifFrameManager(Context context, GifDecoder decoder, int targetWidth, int targetHeight) {
        this(context, Glide.get(context).getBitmapPool(), decoder, new Handler(Looper.getMainLooper()), targetWidth, targetHeight);
    }

    public GifFrameManager(Context context, BitmapPool bitmapPool, GifDecoder decoder, Handler mainHandler, int targetWidth, int targetHeight) {
        this.transformation = UnitTransformation.get();
        this.decoder = decoder;
        this.mainHandler = mainHandler;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.signature = new FrameSignature();
        GifFrameResourceDecoder frameResourceDecoder = new GifFrameResourceDecoder(bitmapPool);
        GifFrameModelLoader frameLoader = new GifFrameModelLoader();
        this.requestBuilder = Glide.with(context).using(frameLoader, GifDecoder.class).from(GifDecoder.class).as(Bitmap.class).signature(this.signature).sourceEncoder(NullEncoder.get()).decoder(frameResourceDecoder).skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE);
    }

    public void setFrameTransformation(Transformation<Bitmap> transformation) {
        if (transformation != null) {
            this.transformation = transformation;
            return;
        }
        throw new NullPointerException("Transformation must not be null");
    }

    public void getNextFrame(FrameCallback cb) {
        if (!this.isLoadInProgress) {
            this.isLoadInProgress = true;
            this.decoder.advance();
            this.next = new DelayTarget(cb, SystemClock.uptimeMillis() + ((long) this.decoder.getNextDelay()));
            this.next.setFrameIndex(this.decoder.getCurrentFrameIndex());
            this.signature.increment();
            this.requestBuilder.load(this.decoder).transform(this.transformation).into(this.next);
        }
    }

    public Bitmap getCurrentFrame() {
        return this.current != null ? this.current.resource : null;
    }

    public void clear() {
        this.isLoadInProgress = false;
        if (this.current != null) {
            Glide.clear(this.current);
            this.mainHandler.removeCallbacks(this.current);
            this.current = null;
        }
        if (this.next != null) {
            Glide.clear(this.next);
            this.mainHandler.removeCallbacks(this.next);
            this.next = null;
        }
        this.decoder.resetFrameIndex();
    }
}
