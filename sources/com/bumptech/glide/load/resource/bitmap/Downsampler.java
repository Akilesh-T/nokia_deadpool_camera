package com.bumptech.glide.load.resource.bitmap;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Build.VERSION;
import android.util.Log;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.ImageHeaderParser.ImageType;
import com.bumptech.glide.util.ByteArrayPool;
import com.bumptech.glide.util.ExceptionCatchingInputStream;
import com.bumptech.glide.util.Util;
import com.morphoinc.utils.multimedia.MediaProviderUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Queue;
import java.util.Set;

public abstract class Downsampler implements BitmapDecoder<InputStream> {
    public static final Downsampler AT_LEAST = new Downsampler() {
        /* Access modifiers changed, original: protected */
        public int getSampleSize(int inWidth, int inHeight, int outWidth, int outHeight) {
            return Math.min(inHeight / outHeight, inWidth / outWidth);
        }

        public String getId() {
            return "AT_LEAST.com.bumptech.glide.load.data.bitmap";
        }
    };
    public static final Downsampler AT_MOST = new Downsampler() {
        /* Access modifiers changed, original: protected */
        public int getSampleSize(int inWidth, int inHeight, int outWidth, int outHeight) {
            return Math.max(inHeight / outHeight, inWidth / outWidth);
        }

        public String getId() {
            return "AT_MOST.com.bumptech.glide.load.data.bitmap";
        }
    };
    private static final int MARK_POSITION = 5242880;
    public static final Downsampler NONE = new Downsampler() {
        /* Access modifiers changed, original: protected */
        public int getSampleSize(int inWidth, int inHeight, int outWidth, int outHeight) {
            return 0;
        }

        public String getId() {
            return "NONE.com.bumptech.glide.load.data.bitmap";
        }
    };
    private static final Queue<Options> OPTIONS_QUEUE = Util.createQueue(0);
    private static final String TAG = "Downsampler";
    private static final Set<ImageType> TYPES_THAT_USE_POOL = EnumSet.of(ImageType.JPEG, ImageType.PNG_A, ImageType.PNG);

    public abstract int getSampleSize(int i, int i2, int i3, int i4);

    public Bitmap decode(InputStream is, BitmapPool pool, int outWidth, int outHeight, DecodeFormat decodeFormat) {
        int orientation;
        IOException iOException;
        BitmapPool bitmapPool = pool;
        ByteArrayPool byteArrayPool = ByteArrayPool.get();
        byte[] bytesForOptions = byteArrayPool.getBytes();
        byte[] bytesForStream = byteArrayPool.getBytes();
        Options options = getDefaultOptions();
        ExceptionCatchingInputStream stream = ExceptionCatchingInputStream.obtain(new RecyclableBufferedInputStream(is, bytesForStream));
        try {
            stream.mark(MARK_POSITION);
            orientation = 0;
            try {
                orientation = new ImageHeaderParser(stream).getOrientation();
                stream.reset();
            } catch (IOException e) {
                if (Log.isLoggable(TAG, 5)) {
                    Log.w(TAG, "Cannot determine the image orientation from header", e);
                }
                try {
                    stream.reset();
                } catch (IOException orientation2) {
                    Object obj = orientation2;
                    if (Log.isLoggable(TAG, 5)) {
                        Log.w(TAG, "Cannot reset the input stream", orientation2);
                    }
                }
            }
        } catch (IOException e2) {
            iOException = e2;
            if (Log.isLoggable(TAG, 5)) {
                Log.w(TAG, "Cannot reset the input stream", e2);
            }
        } catch (Throwable th) {
            byteArrayPool.releaseBytes(bytesForOptions);
            byteArrayPool.releaseBytes(bytesForStream);
            stream.release();
            releaseOptions(options);
        }
        int orientation22 = orientation;
        options.inTempStorage = bytesForOptions;
        int[] inDimens = getDimensions(stream, options);
        int inWidth = inDimens[0];
        int inHeight = inDimens[1];
        ExceptionCatchingInputStream exceptionCatchingInputStream = stream;
        Options options2 = options;
        BitmapPool bitmapPool2 = bitmapPool;
        int i = inWidth;
        int i2 = inHeight;
        Bitmap downsampled = downsampleWithSize(exceptionCatchingInputStream, options2, bitmapPool2, i, i2, getRoundedSampleSize(TransformationUtils.getExifOrientationDegrees(orientation22), inWidth, inHeight, outWidth, outHeight), decodeFormat);
        orientation = stream.getException();
        if (orientation == 0) {
            Bitmap rotated = null;
            if (downsampled != null) {
                rotated = TransformationUtils.rotateImageExif(downsampled, bitmapPool, orientation22);
                if (!(downsampled.equals(rotated) || bitmapPool.put(downsampled))) {
                    downsampled.recycle();
                }
            }
            byteArrayPool.releaseBytes(bytesForOptions);
            byteArrayPool.releaseBytes(bytesForStream);
            stream.release();
            releaseOptions(options);
            return rotated;
        }
        throw new RuntimeException(orientation);
    }

    private int getRoundedSampleSize(int degreesToRotate, int inWidth, int inHeight, int outWidth, int outHeight) {
        int exactSampleSize;
        if (degreesToRotate == 90 || degreesToRotate == MediaProviderUtils.ROTATION_270) {
            exactSampleSize = getSampleSize(inHeight, inWidth, outWidth, outHeight);
        } else {
            exactSampleSize = getSampleSize(inWidth, inHeight, outWidth, outHeight);
        }
        return Math.max(1, exactSampleSize == 0 ? 0 : Integer.highestOneBit(exactSampleSize - 1));
    }

    private Bitmap downsampleWithSize(ExceptionCatchingInputStream is, Options options, BitmapPool pool, int inWidth, int inHeight, int sampleSize, DecodeFormat decodeFormat) {
        Config config = getConfig(is, decodeFormat);
        options.inSampleSize = sampleSize;
        options.inPreferredConfig = config;
        if ((options.inSampleSize == 1 || 19 <= VERSION.SDK_INT) && shouldUsePool(is)) {
            setInBitmap(options, pool.getDirty((int) Math.ceil(((double) inWidth) / ((double) sampleSize)), (int) Math.ceil(((double) inHeight) / ((double) sampleSize)), config));
        }
        return decodeStream(is, options);
    }

    private static boolean shouldUsePool(InputStream is) {
        if (19 <= VERSION.SDK_INT) {
            return true;
        }
        is.mark(1024);
        try {
            boolean contains = TYPES_THAT_USE_POOL.contains(new ImageHeaderParser(is).getType());
            try {
                is.reset();
            } catch (IOException e) {
                if (Log.isLoggable(TAG, 5)) {
                    Log.w(TAG, "Cannot reset the input stream", e);
                }
            }
            return contains;
        } catch (IOException e2) {
            if (Log.isLoggable(TAG, 5)) {
                Log.w(TAG, "Cannot determine the image type from header", e2);
            }
            try {
                is.reset();
            } catch (IOException e22) {
                if (Log.isLoggable(TAG, 5)) {
                    Log.w(TAG, "Cannot reset the input stream", e22);
                }
            }
            return false;
        } catch (Throwable th) {
            try {
                is.reset();
            } catch (IOException e3) {
                if (Log.isLoggable(TAG, 5)) {
                    Log.w(TAG, "Cannot reset the input stream", e3);
                }
            }
            throw th;
        }
    }

    /* JADX WARNING: Missing block: B:12:0x0028, code skipped:
            if (android.util.Log.isLoggable(TAG, 5) == false) goto L_0x0061;
     */
    /* JADX WARNING: Missing block: B:13:0x002a, code skipped:
            android.util.Log.w(TAG, "Cannot reset the input stream", r2);
     */
    /* JADX WARNING: Missing block: B:24:0x005e, code skipped:
            if (android.util.Log.isLoggable(TAG, 5) == false) goto L_0x0061;
     */
    private static android.graphics.Bitmap.Config getConfig(java.io.InputStream r6, com.bumptech.glide.load.DecodeFormat r7) {
        /*
        r0 = com.bumptech.glide.load.DecodeFormat.ALWAYS_ARGB_8888;
        if (r7 == r0) goto L_0x007f;
    L_0x0004:
        r0 = android.os.Build.VERSION.SDK_INT;
        r1 = 16;
        if (r0 != r1) goto L_0x000c;
    L_0x000a:
        goto L_0x007f;
    L_0x000c:
        r0 = 0;
        r1 = 1024; // 0x400 float:1.435E-42 double:5.06E-321;
        r6.mark(r1);
        r1 = 5;
        r2 = new com.bumptech.glide.load.resource.bitmap.ImageHeaderParser;	 Catch:{ IOException -> 0x0034 }
        r2.<init>(r6);	 Catch:{ IOException -> 0x0034 }
        r2 = r2.hasAlpha();	 Catch:{ IOException -> 0x0034 }
        r0 = r2;
        r6.reset();	 Catch:{ IOException -> 0x0021 }
    L_0x0020:
        goto L_0x0061;
    L_0x0021:
        r2 = move-exception;
        r3 = "Downsampler";
        r1 = android.util.Log.isLoggable(r3, r1);
        if (r1 == 0) goto L_0x0031;
    L_0x002a:
        r1 = "Downsampler";
        r3 = "Cannot reset the input stream";
        android.util.Log.w(r1, r3, r2);
    L_0x0031:
        goto L_0x0061;
    L_0x0032:
        r2 = move-exception;
        goto L_0x0069;
    L_0x0034:
        r2 = move-exception;
        r3 = "Downsampler";
        r3 = android.util.Log.isLoggable(r3, r1);	 Catch:{ all -> 0x0032 }
        if (r3 == 0) goto L_0x0053;
    L_0x003d:
        r3 = "Downsampler";
        r4 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0032 }
        r4.<init>();	 Catch:{ all -> 0x0032 }
        r5 = "Cannot determine whether the image has alpha or not from header for format ";
        r4.append(r5);	 Catch:{ all -> 0x0032 }
        r4.append(r7);	 Catch:{ all -> 0x0032 }
        r4 = r4.toString();	 Catch:{ all -> 0x0032 }
        android.util.Log.w(r3, r4, r2);	 Catch:{ all -> 0x0032 }
    L_0x0053:
        r6.reset();	 Catch:{ IOException -> 0x0057 }
        goto L_0x0020;
    L_0x0057:
        r2 = move-exception;
        r3 = "Downsampler";
        r1 = android.util.Log.isLoggable(r3, r1);
        if (r1 == 0) goto L_0x0031;
    L_0x0060:
        goto L_0x002a;
    L_0x0061:
        if (r0 == 0) goto L_0x0066;
    L_0x0063:
        r1 = android.graphics.Bitmap.Config.ARGB_8888;
        goto L_0x0068;
    L_0x0066:
        r1 = android.graphics.Bitmap.Config.RGB_565;
    L_0x0068:
        return r1;
        r6.reset();	 Catch:{ IOException -> 0x006e }
        goto L_0x007e;
    L_0x006e:
        r3 = move-exception;
        r4 = "Downsampler";
        r1 = android.util.Log.isLoggable(r4, r1);
        if (r1 == 0) goto L_0x007e;
    L_0x0077:
        r1 = "Downsampler";
        r4 = "Cannot reset the input stream";
        android.util.Log.w(r1, r4, r3);
    L_0x007e:
        throw r2;
    L_0x007f:
        r0 = android.graphics.Bitmap.Config.ARGB_8888;
        return r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.bumptech.glide.load.resource.bitmap.Downsampler.getConfig(java.io.InputStream, com.bumptech.glide.load.DecodeFormat):android.graphics.Bitmap$Config");
    }

    public int[] getDimensions(ExceptionCatchingInputStream is, Options options) {
        options.inJustDecodeBounds = true;
        decodeStream(is, options);
        options.inJustDecodeBounds = false;
        return new int[]{options.outWidth, options.outHeight};
    }

    private static Bitmap decodeStream(ExceptionCatchingInputStream is, Options options) {
        if (options.inJustDecodeBounds) {
            is.mark(MARK_POSITION);
        } else {
            is.fixMarkLimit();
        }
        Bitmap result = BitmapFactory.decodeStream(is, null, options);
        try {
            if (options.inJustDecodeBounds) {
                is.reset();
            }
        } catch (IOException e) {
            if (Log.isLoggable(TAG, 6)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Exception loading inDecodeBounds=");
                stringBuilder.append(options.inJustDecodeBounds);
                stringBuilder.append(" sample=");
                stringBuilder.append(options.inSampleSize);
                Log.e(str, stringBuilder.toString(), e);
            }
        }
        return result;
    }

    @TargetApi(11)
    private static void setInBitmap(Options options, Bitmap recycled) {
        if (11 <= VERSION.SDK_INT) {
            options.inBitmap = recycled;
        }
    }

    @TargetApi(11)
    private static synchronized Options getDefaultOptions() {
        Options decodeBitmapOptions;
        synchronized (Downsampler.class) {
            synchronized (OPTIONS_QUEUE) {
                decodeBitmapOptions = (Options) OPTIONS_QUEUE.poll();
            }
            if (decodeBitmapOptions == null) {
                decodeBitmapOptions = new Options();
                resetOptions(decodeBitmapOptions);
            }
        }
        return decodeBitmapOptions;
    }

    private static void releaseOptions(Options decodeBitmapOptions) {
        resetOptions(decodeBitmapOptions);
        synchronized (OPTIONS_QUEUE) {
            OPTIONS_QUEUE.offer(decodeBitmapOptions);
        }
    }

    @TargetApi(11)
    private static void resetOptions(Options decodeBitmapOptions) {
        decodeBitmapOptions.inTempStorage = null;
        decodeBitmapOptions.inDither = false;
        decodeBitmapOptions.inScaled = false;
        decodeBitmapOptions.inSampleSize = 1;
        decodeBitmapOptions.inPreferredConfig = null;
        decodeBitmapOptions.inJustDecodeBounds = false;
        if (11 <= VERSION.SDK_INT) {
            decodeBitmapOptions.inBitmap = null;
            decodeBitmapOptions.inMutable = true;
        }
    }
}
