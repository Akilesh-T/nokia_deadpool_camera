package com.hmdglobal.app.camera.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import com.hmdglobal.app.camera.data.LocalDataUtil;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.morphoinc.utils.multimedia.MediaProviderUtils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class ZoomView extends ImageView {
    private static final Tag TAG = new Tag("ZoomView");
    private int mOrientation;
    private DecodePartialBitmap mPartialDecodingTask;
    private BitmapRegionDecoder mRegionDecoder;
    private Uri mUri;
    private int mViewportHeight = 0;
    private int mViewportWidth = 0;

    private class DecodePartialBitmap extends AsyncTask<RectF, Void, Bitmap> {
        BitmapRegionDecoder mDecoder;

        private DecodePartialBitmap() {
        }

        /* synthetic */ DecodePartialBitmap(ZoomView x0, AnonymousClass1 x1) {
            this();
        }

        /* Access modifiers changed, original: protected */
        public void onPreExecute() {
            this.mDecoder = ZoomView.this.mRegionDecoder;
        }

        /* Access modifiers changed, original: protected|varargs */
        public Bitmap doInBackground(RectF... params) {
            RectF endRect = params[0];
            InputStream isForDimensions = ZoomView.this.getInputStream();
            if (isForDimensions == null) {
                return null;
            }
            Point imageSize = LocalDataUtil.decodeBitmapDimension(isForDimensions);
            try {
                isForDimensions.close();
            } catch (IOException e) {
                IOException iOException = e;
                Log.e(ZoomView.TAG, "exception closing dimensions inputstream", e);
            }
            if (imageSize == null) {
                return null;
            }
            RectF fullResRect = new RectF(0.0f, 0.0f, (float) (imageSize.x - 1), (float) (imageSize.y - 1));
            Matrix rotationMatrix = new Matrix();
            rotationMatrix.setRotate((float) ZoomView.this.mOrientation, 0.0f, 0.0f);
            rotationMatrix.mapRect(fullResRect);
            rotationMatrix.postTranslate(-fullResRect.left, -fullResRect.top);
            rotationMatrix.mapRect(fullResRect, new RectF(0.0f, 0.0f, (float) (imageSize.x - 1), (float) (imageSize.y - 1)));
            RectF visibleRect = new RectF(endRect);
            visibleRect.intersect(0.0f, 0.0f, (float) (ZoomView.this.mViewportWidth - 1), (float) (ZoomView.this.mViewportHeight - 1));
            Matrix mapping = new Matrix();
            mapping.setRectToRect(endRect, fullResRect, ScaleToFit.CENTER);
            RectF visibleAfterRotation = new RectF();
            mapping.mapRect(visibleAfterRotation, visibleRect);
            RectF visibleInImage = new RectF();
            Matrix invertRotation = new Matrix();
            rotationMatrix.invert(invertRotation);
            invertRotation.mapRect(visibleInImage, visibleAfterRotation);
            Rect region = new Rect();
            visibleInImage.round(region);
            region.intersect(0, 0, imageSize.x - 1, imageSize.y - 1);
            if (region.width() == 0 || region.height() == 0) {
                Tag access$200 = ZoomView.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid size for partial region. Region: ");
                stringBuilder.append(region.toString());
                Log.e(access$200, stringBuilder.toString());
                return null;
            } else if (isCancelled()) {
                return null;
            } else {
                Options options = new Options();
                if ((ZoomView.this.mOrientation + 360) % MediaProviderUtils.ROTATION_180 == 0) {
                    options.inSampleSize = ZoomView.this.getSampleFactor(region.width(), region.height());
                } else {
                    options.inSampleSize = ZoomView.this.getSampleFactor(region.height(), region.width());
                }
                if (this.mDecoder == null) {
                    InputStream is = ZoomView.this.getInputStream();
                    if (is == null) {
                        return null;
                    }
                    try {
                        this.mDecoder = BitmapRegionDecoder.newInstance(is, false);
                        is.close();
                    } catch (IOException e2) {
                        Log.e(ZoomView.TAG, "Failed to instantiate region decoder");
                    }
                }
                if (this.mDecoder == null) {
                    return null;
                }
                Bitmap b = this.mDecoder.decodeRegion(region, options);
                if (isCancelled()) {
                    return null;
                }
                Matrix rotation = new Matrix();
                rotation.setRotate((float) ZoomView.this.mOrientation);
                return Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), rotation, false);
            }
        }

        /* Access modifiers changed, original: protected */
        public void onPostExecute(Bitmap b) {
            ZoomView.this.mPartialDecodingTask = null;
            if (this.mDecoder != ZoomView.this.mRegionDecoder) {
                this.mDecoder.recycle();
            }
            if (b != null) {
                ZoomView.this.setImageBitmap(b);
                ZoomView.this.showPartiallyDecodedImage(true);
            }
        }
    }

    public ZoomView(Context context) {
        super(context);
        setScaleType(ScaleType.FIT_CENTER);
        addOnLayoutChangeListener(new OnLayoutChangeListener() {
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int w = right - left;
                int h = bottom - top;
                if (ZoomView.this.mViewportHeight != h || ZoomView.this.mViewportWidth != w) {
                    ZoomView.this.mViewportWidth = w;
                    ZoomView.this.mViewportHeight = h;
                }
            }
        });
    }

    public void resetDecoder() {
        if (this.mRegionDecoder != null) {
            cancelPartialDecodingTask();
            if (this.mPartialDecodingTask == null) {
                this.mRegionDecoder.recycle();
            }
            this.mRegionDecoder = null;
        }
    }

    public void loadBitmap(Uri uri, int orientation, RectF imageRect) {
        if (!uri.equals(this.mUri)) {
            resetDecoder();
            this.mUri = uri;
            this.mOrientation = orientation;
        }
        startPartialDecodingTask(imageRect);
    }

    private void showPartiallyDecodedImage(boolean show) {
        if (show) {
            setVisibility(0);
        } else {
            setVisibility(8);
        }
    }

    public void cancelPartialDecodingTask() {
        if (this.mPartialDecodingTask != null && !this.mPartialDecodingTask.isCancelled()) {
            this.mPartialDecodingTask.cancel(true);
            setVisibility(8);
        }
    }

    public static RectF adjustToFitInBounds(RectF rect, int viewportWidth, int viewportHeight) {
        float dx = 0.0f;
        float dy = 0.0f;
        RectF newRect = new RectF(rect);
        if (newRect.width() < ((float) viewportWidth)) {
            dx = ((float) (viewportWidth / 2)) - ((newRect.left + newRect.right) / 2.0f);
        } else if (newRect.left > 0.0f) {
            dx = -newRect.left;
        } else if (newRect.right < ((float) viewportWidth)) {
            dx = ((float) viewportWidth) - newRect.right;
        }
        if (newRect.height() < ((float) viewportHeight)) {
            dy = ((float) (viewportHeight / 2)) - ((newRect.top + newRect.bottom) / 2.0f);
        } else if (newRect.top > 0.0f) {
            dy = -newRect.top;
        } else if (newRect.bottom < ((float) viewportHeight)) {
            dy = ((float) viewportHeight) - newRect.bottom;
        }
        if (!(dx == 0.0f && dy == 0.0f)) {
            newRect.offset(dx, dy);
        }
        return newRect;
    }

    private void startPartialDecodingTask(RectF endRect) {
        cancelPartialDecodingTask();
        this.mPartialDecodingTask = new DecodePartialBitmap(this, null);
        this.mPartialDecodingTask.execute(new RectF[]{endRect});
    }

    private InputStream getInputStream() {
        try {
            return getContext().getContentResolver().openInputStream(this.mUri);
        } catch (FileNotFoundException e) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("File not found at: ");
            stringBuilder.append(this.mUri);
            Log.e(tag, stringBuilder.toString());
            return null;
        }
    }

    private int getSampleFactor(int width, int height) {
        int sampleFactor = (int) (1065353216 / Math.min(((float) this.mViewportHeight) / ((float) height), ((float) this.mViewportWidth) / ((float) width)));
        if (sampleFactor <= 1) {
            return 1;
        }
        for (int i = 0; i < 32; i++) {
            if ((1 << (i + 1)) > sampleFactor) {
                sampleFactor = 1 << i;
                break;
            }
        }
        return sampleFactor;
    }
}
