package com.hmdglobal.app.camera.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.morphoinc.utils.multimedia.MediaProviderUtils;
import java.io.InputStream;

public class LocalDataUtil {
    private static final Tag TAG = new Tag("LocalDataUtil");

    public static boolean isMimeTypeVideo(String mimeType) {
        return mimeType != null && mimeType.startsWith("video/");
    }

    public static boolean isMimeTypeImage(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    /* JADX WARNING: Missing block: B:11:0x001a, code skipped:
            if (r1 == null) goto L_0x001d;
     */
    public static android.graphics.Point decodeBitmapDimension(java.lang.String r4) {
        /*
        r0 = 0;
        r1 = 0;
        r2 = new java.io.FileInputStream;	 Catch:{ FileNotFoundException -> 0x0016 }
        r2.<init>(r4);	 Catch:{ FileNotFoundException -> 0x0016 }
        r1 = r2;
        r2 = decodeBitmapDimension(r1);	 Catch:{ FileNotFoundException -> 0x0016 }
        r0 = r2;
    L_0x000e:
        r1.close();	 Catch:{ IOException -> 0x0012 }
        goto L_0x001d;
    L_0x0012:
        r2 = move-exception;
        goto L_0x001d;
    L_0x0014:
        r2 = move-exception;
        goto L_0x001e;
    L_0x0016:
        r2 = move-exception;
        r2.printStackTrace();	 Catch:{ all -> 0x0014 }
        if (r1 == 0) goto L_0x001d;
    L_0x001c:
        goto L_0x000e;
    L_0x001d:
        return r0;
    L_0x001e:
        if (r1 == 0) goto L_0x0025;
    L_0x0020:
        r1.close();	 Catch:{ IOException -> 0x0024 }
        goto L_0x0025;
    L_0x0024:
        r3 = move-exception;
    L_0x0025:
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.data.LocalDataUtil.decodeBitmapDimension(java.lang.String):android.graphics.Point");
    }

    public static Point decodeBitmapDimension(InputStream is) {
        Options justBoundsOpts = new Options();
        justBoundsOpts.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, justBoundsOpts);
        if (justBoundsOpts.outWidth > 0 && justBoundsOpts.outHeight > 0) {
            return new Point(justBoundsOpts.outWidth, justBoundsOpts.outHeight);
        }
        Log.e(TAG, "Bitmap dimension decoding failed");
        return null;
    }

    public static Bitmap loadImageThumbnailFromStream(InputStream stream, int imageWidth, int imageHeight, int widthBound, int heightBound, int orientation, int maximumPixels) {
        int imageWidth2;
        int imageHeight2;
        int i = widthBound;
        int i2 = heightBound;
        int i3 = orientation;
        int i4 = maximumPixels;
        byte[] decodeBuffer = new byte[32768];
        if (i3 % MediaProviderUtils.ROTATION_180 != 0) {
            imageWidth2 = imageHeight;
            imageHeight2 = imageWidth;
        } else {
            imageWidth2 = imageWidth;
            imageHeight2 = imageHeight;
        }
        int targetHeight = imageHeight2;
        int targetWidth = imageWidth2;
        int sampleSize = 1;
        while (true) {
            if (targetHeight <= i2 && targetWidth <= i && targetHeight <= 3379 && targetWidth <= 3379 && targetHeight * targetWidth <= i4) {
                break;
            }
            InputStream inputStream = stream;
            sampleSize <<= 1;
            targetWidth = imageWidth2 / sampleSize;
            targetHeight = imageWidth2 / sampleSize;
            i = widthBound;
        }
        if ((i2 > 3379 || i > 3379) && targetWidth * targetHeight < i4 / 4 && sampleSize > 1) {
            sampleSize >>= 2;
        }
        if (i3 % MediaProviderUtils.ROTATION_180 == 0 && targetWidth * targetHeight < i4 / 100) {
            sampleSize = 2;
        }
        Options opts = new Options();
        opts.inSampleSize = sampleSize;
        opts.inTempStorage = decodeBuffer;
        Bitmap b = BitmapFactory.decodeStream(stream, null, opts);
        if (b == null) {
            return null;
        }
        if (b.getWidth() > 3379 || b.getHeight() > 3379) {
            int maxEdge = Math.max(b.getWidth(), b.getHeight());
            b = Bitmap.createScaledBitmap(b, (b.getWidth() * 3379) / maxEdge, (b.getHeight() * 3379) / maxEdge, false);
        }
        if (!(i3 == 0 || b == null)) {
            Matrix m = new Matrix();
            m.setRotate((float) i3);
            b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, false);
        }
        return b;
    }

    public static Bitmap loadVideoThumbnail(String path) {
        Tag tag;
        StringBuilder stringBuilder;
        Log.d(TAG, "VideoThumbnail enter thumbnail KPI");
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            byte[] data = retriever.getEmbeddedPicture();
            if (data != null) {
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            }
            if (bitmap == null) {
                bitmap = retriever.getFrameAtTime();
            }
            try {
                retriever.release();
            } catch (RuntimeException e) {
            }
        } catch (IllegalArgumentException e2) {
            tag = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("MediaMetadataRetriever.setDataSource() fail:");
            stringBuilder.append(e2.getMessage());
            Log.e(tag, stringBuilder.toString());
            retriever.release();
        } catch (RuntimeException e3) {
            tag = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("MediaMetadataRetriever.setDataSource() fail:");
            stringBuilder.append(e3.getMessage());
            Log.e(tag, stringBuilder.toString());
            retriever.release();
        } catch (Throwable th) {
            try {
                retriever.release();
            } catch (RuntimeException e4) {
            }
            throw th;
        }
        Log.d(TAG, "VideoThumbnail out thumbnail KPI");
        return bitmap;
    }
}
