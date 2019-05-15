package com.bumptech.glide.load.resource.gif;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.ConstantState;
import android.os.Build.VERSION;
import android.view.Gravity;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifDecoder.BitmapProvider;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.load.resource.gif.GifFrameManager.FrameCallback;

public class GifDrawable extends GlideDrawable implements FrameCallback {
    private boolean applyGravity;
    private final GifDecoder decoder;
    private final Rect destRect;
    private final GifFrameManager frameManager;
    private boolean isRecycled;
    private boolean isRunning;
    private boolean isStarted;
    private boolean isVisible;
    private int loopCount;
    private int maxLoopCount;
    private final Paint paint;
    private final GifState state;

    static class GifState extends ConstantState {
        private static final int GRAVITY = 119;
        BitmapPool bitmapPool;
        BitmapProvider bitmapProvider;
        Context context;
        byte[] data;
        Bitmap firstFrame;
        Transformation<Bitmap> frameTransformation;
        GifHeader gifHeader;
        int targetHeight;
        int targetWidth;

        public GifState(GifHeader header, byte[] data, Context context, Transformation<Bitmap> frameTransformation, int targetWidth, int targetHeight, BitmapProvider provider, BitmapPool bitmapPool, Bitmap firstFrame) {
            if (firstFrame != null) {
                this.gifHeader = header;
                this.data = data;
                this.bitmapPool = bitmapPool;
                this.firstFrame = firstFrame;
                this.context = context.getApplicationContext();
                this.frameTransformation = frameTransformation;
                this.targetWidth = targetWidth;
                this.targetHeight = targetHeight;
                this.bitmapProvider = provider;
                return;
            }
            throw new NullPointerException("The first frame of the GIF must not be null");
        }

        public GifState(GifState original) {
            if (original != null) {
                this.gifHeader = original.gifHeader;
                this.data = original.data;
                this.context = original.context;
                this.frameTransformation = original.frameTransformation;
                this.targetWidth = original.targetWidth;
                this.targetHeight = original.targetHeight;
                this.bitmapProvider = original.bitmapProvider;
                this.bitmapPool = original.bitmapPool;
                this.firstFrame = original.firstFrame;
            }
        }

        public Drawable newDrawable(Resources res) {
            return newDrawable();
        }

        public Drawable newDrawable() {
            return new GifDrawable(this);
        }

        public int getChangingConfigurations() {
            return 0;
        }
    }

    public GifDrawable(Context context, BitmapProvider bitmapProvider, BitmapPool bitmapPool, Transformation<Bitmap> frameTransformation, int targetFrameWidth, int targetFrameHeight, GifHeader gifHeader, byte[] data, Bitmap firstFrame) {
        GifState gifState = new GifState(gifHeader, data, context, frameTransformation, targetFrameWidth, targetFrameHeight, bitmapProvider, bitmapPool, firstFrame);
        this(gifState);
    }

    GifDrawable(GifState state) {
        this.paint = new Paint();
        this.destRect = new Rect();
        this.isVisible = true;
        this.maxLoopCount = -1;
        if (state != null) {
            this.state = state;
            this.decoder = new GifDecoder(state.bitmapProvider);
            this.decoder.setData(state.gifHeader, state.data);
            this.frameManager = new GifFrameManager(state.context, this.decoder, state.targetWidth, state.targetHeight);
            this.frameManager.setFrameTransformation(state.frameTransformation);
            return;
        }
        throw new NullPointerException("GifState must not be null");
    }

    GifDrawable(GifDecoder decoder, GifFrameManager frameManager, Bitmap firstFrame, BitmapPool bitmapPool) {
        this.paint = new Paint();
        this.destRect = new Rect();
        this.isVisible = true;
        this.maxLoopCount = -1;
        this.decoder = decoder;
        this.frameManager = frameManager;
        this.state = new GifState(null);
        this.state.bitmapPool = bitmapPool;
        this.state.firstFrame = firstFrame;
    }

    public Bitmap getFirstFrame() {
        return this.state.firstFrame;
    }

    public void setFrameTransformation(Transformation<Bitmap> frameTransformation, Bitmap firstFrame) {
        if (firstFrame == null) {
            throw new NullPointerException("The first frame of the GIF must not be null");
        } else if (frameTransformation != null) {
            this.state.frameTransformation = frameTransformation;
            this.state.firstFrame = firstFrame;
            this.frameManager.setFrameTransformation(frameTransformation);
        } else {
            throw new NullPointerException("The frame transformation must not be null");
        }
    }

    public GifDecoder getDecoder() {
        return this.decoder;
    }

    public Transformation<Bitmap> getFrameTransformation() {
        return this.state.frameTransformation;
    }

    public byte[] getData() {
        return this.state.data;
    }

    public int getFrameCount() {
        return this.decoder.getFrameCount();
    }

    private void resetLoopCount() {
        this.loopCount = 0;
    }

    public void start() {
        this.isStarted = true;
        resetLoopCount();
        if (this.isVisible) {
            startRunning();
        }
    }

    public void stop() {
        this.isStarted = false;
        stopRunning();
        if (VERSION.SDK_INT < 11) {
            reset();
        }
    }

    private void reset() {
        this.frameManager.clear();
        invalidateSelf();
    }

    private void startRunning() {
        if (this.decoder.getFrameCount() == 1) {
            invalidateSelf();
        } else if (!this.isRunning) {
            this.isRunning = true;
            this.frameManager.getNextFrame(this);
            invalidateSelf();
        }
    }

    private void stopRunning() {
        this.isRunning = false;
    }

    public boolean setVisible(boolean visible, boolean restart) {
        this.isVisible = visible;
        if (!visible) {
            stopRunning();
        } else if (this.isStarted) {
            startRunning();
        }
        return super.setVisible(visible, restart);
    }

    public int getIntrinsicWidth() {
        return this.state.firstFrame.getWidth();
    }

    public int getIntrinsicHeight() {
        return this.state.firstFrame.getHeight();
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    /* Access modifiers changed, original: 0000 */
    public void setIsRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    /* Access modifiers changed, original: protected */
    public void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        this.applyGravity = true;
    }

    public void draw(Canvas canvas) {
        if (!this.isRecycled) {
            if (this.applyGravity) {
                Gravity.apply(119, getIntrinsicWidth(), getIntrinsicHeight(), getBounds(), this.destRect);
                this.applyGravity = false;
            }
            Bitmap currentFrame = this.frameManager.getCurrentFrame();
            canvas.drawBitmap(currentFrame != null ? currentFrame : this.state.firstFrame, null, this.destRect, this.paint);
        }
    }

    public void setAlpha(int i) {
        this.paint.setAlpha(i);
    }

    public void setColorFilter(ColorFilter colorFilter) {
        this.paint.setColorFilter(colorFilter);
    }

    public int getOpacity() {
        return -2;
    }

    @TargetApi(11)
    public void onFrameRead(int frameIndex) {
        if (VERSION.SDK_INT >= 11 && getCallback() == null) {
            stop();
            reset();
        } else if (this.isRunning) {
            invalidateSelf();
            if (frameIndex == this.decoder.getFrameCount() - 1) {
                this.loopCount++;
            }
            if (this.maxLoopCount == -1 || this.loopCount < this.maxLoopCount) {
                this.frameManager.getNextFrame(this);
            } else {
                stop();
            }
        }
    }

    public ConstantState getConstantState() {
        return this.state;
    }

    public void recycle() {
        this.isRecycled = true;
        this.state.bitmapPool.put(this.state.firstFrame);
        this.frameManager.clear();
    }

    /* Access modifiers changed, original: 0000 */
    public boolean isRecycled() {
        return this.isRecycled;
    }

    public boolean isAnimated() {
        return true;
    }

    public void setLoopCount(int loopCount) {
        if (loopCount <= 0 && loopCount != -1 && loopCount != 0) {
            throw new IllegalArgumentException("Loop count must be greater than 0, or equal to GlideDrawable.LOOP_FOREVER, or equal to GlideDrawable.LOOP_INTRINSIC");
        } else if (loopCount == 0) {
            this.maxLoopCount = this.decoder.getLoopCount();
        } else {
            this.maxLoopCount = loopCount;
        }
    }
}
