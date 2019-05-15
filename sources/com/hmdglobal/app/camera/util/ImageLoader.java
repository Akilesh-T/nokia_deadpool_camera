package com.hmdglobal.app.camera.util;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.webkit.MimeTypeMap;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.exif.ExifInterface;
import com.hmdglobal.app.camera.exif.ExifTag;
import com.morphoinc.utils.multimedia.MediaProviderUtils;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public final class ImageLoader {
    private static final int BITMAP_LOAD_BACKOUT_ATTEMPTS = 5;
    public static final int DEFAULT_COMPRESS_QUALITY = 95;
    public static final String JPEG_MIME_TYPE = "image/jpeg";
    public static final int ORI_FLIP_HOR = 2;
    public static final int ORI_FLIP_VERT = 4;
    public static final int ORI_NORMAL = 1;
    public static final int ORI_ROTATE_180 = 3;
    public static final int ORI_ROTATE_270 = 8;
    public static final int ORI_ROTATE_90 = 6;
    public static final int ORI_TRANSPOSE = 5;
    public static final int ORI_TRANSVERSE = 7;
    private static final Tag TAG = new Tag("ImageLoader");

    private ImageLoader() {
    }

    public static String getMimeType(Uri src) {
        String postfix = MimeTypeMap.getFileExtensionFromUrl(src.toString());
        if (postfix != null) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(postfix);
        }
        return null;
    }

    public static String getLocalPathFromUri(ContentResolver resolver, Uri uri) {
        Cursor cursor = resolver.query(uri, new String[]{"_data"}, null, null, null);
        if (cursor == null) {
            return null;
        }
        int index = cursor.getColumnIndexOrThrow("_data");
        cursor.moveToFirst();
        return cursor.getString(index);
    }

    public static int getMetadataOrientation(Context context, Uri uri) {
        if (uri == null || context == null) {
            throw new IllegalArgumentException("bad argument to getOrientation");
        }
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, new String[]{"orientation"}, null, null, null);
            if (cursor != null && cursor.moveToNext()) {
                int ori = cursor.getInt(0);
                if (ori == 90) {
                    closeSilently(cursor);
                    return 6;
                } else if (ori == MediaProviderUtils.ROTATION_180) {
                    closeSilently(cursor);
                    return 3;
                } else if (ori != MediaProviderUtils.ROTATION_270) {
                    closeSilently(cursor);
                    return 1;
                } else {
                    closeSilently(cursor);
                    return 8;
                }
            }
        } catch (SQLiteException | IllegalArgumentException | IllegalStateException e) {
        } catch (Throwable th) {
            closeSilently(null);
        }
        closeSilently(cursor);
        if ("file".equals(uri.getScheme())) {
            if (!"image/jpeg".equals(getMimeType(uri))) {
                return 1;
            }
            String path = uri.getPath();
            ExifInterface exif = new ExifInterface();
            try {
                exif.readExif(path);
                Integer tagval = exif.getTagIntValue(ExifInterface.TAG_ORIENTATION);
                if (tagval != null) {
                    int orientation = tagval.intValue();
                    switch (orientation) {
                        case 1:
                        case 2:
                        case 3:
                        case 4:
                        case 5:
                        case 6:
                        case 7:
                        case 8:
                            return orientation;
                        default:
                            return 1;
                    }
                }
            } catch (IOException e2) {
                Log.w(TAG, "Failed to read EXIF orientation", e2);
            }
        }
        return 1;
    }

    public static int getMetadataRotation(Context context, Uri uri) {
        int orientation = getMetadataOrientation(context, uri);
        if (orientation == 3) {
            return MediaProviderUtils.ROTATION_180;
        }
        if (orientation == 6) {
            return 90;
        }
        if (orientation != 8) {
            return 0;
        }
        return MediaProviderUtils.ROTATION_270;
    }

    public static Bitmap orientBitmap(Bitmap bitmap, int ori) {
        Matrix matrix = new Matrix();
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        if (ori == 6 || ori == 8 || ori == 5 || ori == 7) {
            int tmp = w;
            w = h;
            h = tmp;
        }
        int w2 = w;
        int h2 = h;
        switch (ori) {
            case 2:
                matrix.preScale(-1.0f, 1.0f);
                break;
            case 3:
                matrix.setRotate(180.0f, ((float) w2) / 2.0f, ((float) h2) / 2.0f);
                break;
            case 4:
                matrix.preScale(1.0f, -1.0f);
                break;
            case 5:
                matrix.setRotate(90.0f, ((float) w2) / 2.0f, ((float) h2) / 2.0f);
                matrix.preScale(1.0f, -1.0f);
                break;
            case 6:
                matrix.setRotate(90.0f, ((float) w2) / 2.0f, ((float) h2) / 2.0f);
                break;
            case 7:
                matrix.setRotate(270.0f, ((float) w2) / 2.0f, ((float) h2) / 2.0f);
                matrix.preScale(1.0f, -1.0f);
                break;
            case 8:
                matrix.setRotate(270.0f, ((float) w2) / 2.0f, ((float) h2) / 2.0f);
                break;
            default:
                return bitmap;
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Rect loadBitmapBounds(Context context, Uri uri) {
        Options o = new Options();
        loadBitmap(context, uri, o);
        return new Rect(0, 0, o.outWidth, o.outHeight);
    }

    public static Bitmap loadDownsampledBitmap(Context context, Uri uri, int sampleSize) {
        Options options = new Options();
        options.inMutable = true;
        options.inSampleSize = sampleSize;
        return loadBitmap(context, uri, options);
    }

    public static Bitmap loadBitmap(Context context, Uri uri, Options o) {
        if (uri == null || context == null) {
            throw new IllegalArgumentException("bad argument to loadBitmap");
        }
        InputStream is = null;
        Bitmap e;
        try {
            is = context.getContentResolver().openInputStream(uri);
            e = BitmapFactory.decodeStream(is, null, o);
            return e;
        } catch (FileNotFoundException e2) {
            e = e2;
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("FileNotFoundException for ");
            stringBuilder.append(uri);
            Log.e(tag, stringBuilder.toString(), e);
            return null;
        } finally {
            closeSilently(is);
        }
    }

    public static Bitmap loadConstrainedBitmap(Uri uri, Context context, int maxSideLength, Rect originalBounds, boolean useMin) {
        if (maxSideLength <= 0 || uri == null || context == null) {
            throw new IllegalArgumentException("bad argument to getScaledBitmap");
        }
        Rect storedBounds = loadBitmapBounds(context, uri);
        if (originalBounds != null) {
            originalBounds.set(storedBounds);
        }
        int w = storedBounds.width();
        int h = storedBounds.height();
        if (w <= 0 || h <= 0) {
            return null;
        }
        int imageSide;
        if (useMin) {
            imageSide = Math.min(w, h);
        } else {
            imageSide = Math.max(w, h);
        }
        int sampleSize = 1;
        while (imageSide > maxSideLength) {
            imageSide >>>= 1;
            sampleSize <<= 1;
        }
        if (sampleSize <= 0 || Math.min(w, h) / sampleSize <= 0) {
            return null;
        }
        return loadDownsampledBitmap(context, uri, sampleSize);
    }

    public static Bitmap loadOrientedConstrainedBitmap(Uri uri, Context context, int maxSideLength, int orientation, Rect originalBounds) {
        Bitmap bmap = loadConstrainedBitmap(uri, context, maxSideLength, originalBounds, null);
        if (bmap == null) {
            return bmap;
        }
        bmap = orientBitmap(bmap, orientation);
        if (bmap.getConfig() != Config.ARGB_8888) {
            return bmap.copy(Config.ARGB_8888, true);
        }
        return bmap;
    }

    public static Bitmap loadBitmapWithBackouts(Context context, Uri sourceUri, int sampleSize) {
        boolean noBitmap = true;
        int num_tries = 0;
        if (sampleSize <= 0) {
            sampleSize = 1;
        }
        Bitmap bmap = null;
        while (noBitmap) {
            try {
                bmap = loadDownsampledBitmap(context, sourceUri, sampleSize);
                noBitmap = false;
            } catch (OutOfMemoryError e) {
                num_tries++;
                if (num_tries < 5) {
                    bmap = null;
                    System.gc();
                    sampleSize *= 2;
                } else {
                    throw e;
                }
            }
        }
        return bmap;
    }

    public static Bitmap loadOrientedBitmapWithBackouts(Context context, Uri sourceUri, int sampleSize) {
        Bitmap bitmap = loadBitmapWithBackouts(context, sourceUri, sampleSize);
        if (bitmap == null) {
            return null;
        }
        return orientBitmap(bitmap, getMetadataOrientation(context, sourceUri));
    }

    public static Bitmap decodeResourceWithBackouts(Resources res, Options options, int id) {
        boolean noBitmap = true;
        int num_tries = 0;
        if (options.inSampleSize < 1) {
            options.inSampleSize = 1;
        }
        Bitmap bmap = null;
        while (noBitmap) {
            try {
                bmap = BitmapFactory.decodeResource(res, id, options);
                noBitmap = false;
            } catch (OutOfMemoryError e) {
                num_tries++;
                if (num_tries < 5) {
                    bmap = null;
                    System.gc();
                    options.inSampleSize *= 2;
                } else {
                    throw e;
                }
            }
        }
        return bmap;
    }

    public static List<ExifTag> getExif(ContentResolver resolver, Uri uri) {
        String path = getLocalPathFromUri(resolver, uri);
        if (path != null) {
            if (!"image/jpeg".equals(getMimeType(Uri.parse(path)))) {
                return null;
            }
            try {
                ExifInterface exif = new ExifInterface();
                exif.readExif(path);
                return exif.getAllTags();
            } catch (IOException e) {
                Log.w(TAG, "Failed to read EXIF tags", e);
            }
        }
        return null;
    }

    private static void closeSilently(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException t) {
                Log.w(TAG, "close fail ", t);
            }
        }
    }
}
