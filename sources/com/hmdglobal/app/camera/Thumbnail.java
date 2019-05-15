package com.hmdglobal.app.camera;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video.Thumbnails;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.util.CameraUtil;
import java.io.FileDescriptor;

public class Thumbnail {
    private static final Tag TAG = new Tag("Thumbnail");
    private Bitmap mBitmap;
    private Uri mUri;

    private static class Media {
        public final long dateTaken;
        public final long id;
        public final int orientation;
        public final Uri uri;

        public Media(long id, int orientation, long dateTaken, Uri uri) {
            this.id = id;
            this.orientation = orientation;
            this.dateTaken = dateTaken;
            this.uri = uri;
        }
    }

    public static Bitmap createVideoThumbnailBitmap(FileDescriptor fd, int targetWidth) {
        return createVideoThumbnailBitmap(null, fd, targetWidth);
    }

    public static Bitmap createVideoThumbnailBitmap(String filePath, int targetWidth) {
        return createVideoThumbnailBitmap(filePath, null, targetWidth);
    }

    private static Bitmap createVideoThumbnailBitmap(String filePath, FileDescriptor fd, int targetWidth) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Thumbnail  = ");
            stringBuilder.append(filePath);
            Log.d(tag, stringBuilder.toString() == null ? " Empty " : filePath);
            if (filePath != null) {
                retriever.setDataSource(filePath);
            } else {
                retriever.setDataSource(fd);
            }
            Log.d(TAG, "------ complete --------------");
            bitmap = retriever.getFrameAtTime(-1);
            Log.d(TAG, "------ video --------------");
            try {
                retriever.release();
            } catch (RuntimeException e) {
            }
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "------ e1 --------------", ex);
            retriever.release();
        } catch (RuntimeException ex2) {
            Log.d(TAG, "------ e12 --------------", ex2);
            retriever.release();
        } catch (Throwable th) {
            try {
                retriever.release();
            } catch (RuntimeException e2) {
            }
            throw th;
        }
        if (bitmap == null) {
            Log.d(TAG, "------ bitmap is null --------------");
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width > targetWidth) {
            float scale = ((float) targetWidth) / ((float) width);
            bitmap = Bitmap.createScaledBitmap(bitmap, Math.round(((float) width) * scale), Math.round(((float) height) * scale), true);
        }
        return bitmap;
    }

    public static Bitmap getLastThumbnailFromContentResolver(ContentResolver resolver, Thumbnail[] result) {
        Media image = getLastImageThumbnail(resolver);
        Media video = getLastVideoThumbnail(resolver);
        if (image == null && video == null) {
            return null;
        }
        Bitmap bitmap;
        Media lastMedia;
        if (image == null || (video != null && image.dateTaken < video.dateTaken)) {
            bitmap = Thumbnails.getThumbnail(resolver, video.id, 1, null);
            lastMedia = video;
        } else {
            bitmap = Images.Thumbnails.getThumbnail(resolver, image.id, 1, null);
            lastMedia = image;
        }
        if (!CameraUtil.isUriValid(lastMedia.uri, resolver) || bitmap == null) {
            return null;
        }
        result[0] = createThumbnail(lastMedia.uri, bitmap, lastMedia.orientation);
        return bitmap;
    }

    public static Bitmap getLastThumbnailFromContentResolver(ContentResolver resolver, Thumbnail[] result, int width, int height) {
        Media image = getLastImageThumbnail(resolver);
        Media video = getLastVideoThumbnail(resolver);
        if (image == null && video == null) {
            return null;
        }
        Bitmap bitmap;
        Media lastMedia;
        if (image == null || (video != null && image.dateTaken < video.dateTaken)) {
            bitmap = Thumbnails.getThumbnail(resolver, video.id, 1, null);
            lastMedia = video;
        } else {
            bitmap = Images.Thumbnails.getThumbnail(resolver, image.id, 1, null);
            lastMedia = image;
        }
        if (!CameraUtil.isUriValid(lastMedia.uri, resolver)) {
            return null;
        }
        result[0] = createThumbnail(lastMedia.uri, bitmap, width, height, lastMedia.orientation);
        return bitmap;
    }

    public static Uri getLastThumbnailUriFromContentResolver(ContentResolver resolver) {
        Media image = getLastImageThumbnail(resolver);
        Media video = getLastVideoThumbnail(resolver);
        if (image == null && video == null) {
            return null;
        }
        Media lastMedia;
        Bitmap bitmap;
        if (image == null || (video != null && image.dateTaken < video.dateTaken)) {
            bitmap = Thumbnails.getThumbnail(resolver, video.id, 1, null);
            lastMedia = video;
        } else {
            bitmap = Images.Thumbnails.getThumbnail(resolver, image.id, 1, null);
            lastMedia = image;
        }
        if (CameraUtil.isUriValid(lastMedia.uri, resolver)) {
            return lastMedia.uri;
        }
        return null;
    }

    public static Thumbnail createThumbnail(Uri uri, Bitmap bitmap, int orientation) {
        return createThumbnail(uri, bitmap, bitmap.getWidth(), bitmap.getHeight(), orientation);
    }

    public static Thumbnail createThumbnail(Uri uri, Bitmap bitmap, int width, int height, int orientation) {
        if (bitmap == null) {
            return null;
        }
        return new Thumbnail(uri, bitmap, width, height, orientation);
    }

    private static Media getLastImageThumbnail(ContentResolver resolver) {
        Uri baseUri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Uri query = baseUri.buildUpon().appendQueryParameter("limit", "1").build();
        String[] projection = new String[]{"_id", "orientation", "datetaken"};
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mime_type='image/jpeg' AND bucket_id=");
        stringBuilder.append(Storage.BUCKET_ID);
        Cursor cursor = null;
        try {
            cursor = resolver.query(query, projection, stringBuilder.toString(), null, "datetaken DESC,_id DESC");
            if (cursor == null || !cursor.moveToFirst()) {
                if (cursor != null) {
                    cursor.close();
                }
                return null;
            }
            long id = cursor.getLong(0);
            Media media = new Media(id, cursor.getInt(1), cursor.getLong(2), ContentUris.withAppendedId(baseUri, id));
            return media;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static Media getLastVideoThumbnail(ContentResolver resolver) {
        Uri baseUri = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        Uri query = baseUri.buildUpon().appendQueryParameter("limit", "1").build();
        String[] projection = new String[]{"_id", "_data", "datetaken"};
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("bucket_id=");
        stringBuilder.append(Storage.BUCKET_ID);
        Cursor cursor = null;
        try {
            cursor = resolver.query(query, projection, stringBuilder.toString(), null, "datetaken DESC,_id DESC");
            if (cursor == null || !cursor.moveToFirst()) {
                if (cursor != null) {
                    cursor.close();
                }
                return null;
            }
            long id = cursor.getLong(0);
            Media media = new Media(id, 0, cursor.getLong(2), ContentUris.withAppendedId(baseUri, id));
            return media;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static Bitmap rotateImage(Bitmap bitmap, int orientation) {
        return rotateImage(bitmap, bitmap.getWidth(), bitmap.getHeight(), orientation);
    }

    private static Bitmap rotateImage(Bitmap bitmap, int width, int height, int orientation) {
        if (orientation != 0) {
            Matrix m = new Matrix();
            m.setRotate((float) orientation, ((float) width) * 0.5f, ((float) height) * 0.5f);
            try {
                Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, width, height, m, true);
                if (rotated != bitmap) {
                    bitmap.recycle();
                }
                return rotated;
            } catch (Throwable t) {
                Log.w(TAG, "Failed to rotate thumbnail", t);
            }
        }
        return bitmap;
    }

    private Thumbnail(Uri uri, Bitmap bitmap, int width, int height, int orientation) {
        this.mUri = uri;
        this.mBitmap = rotateImage(bitmap, width, height, orientation);
    }

    public Uri getUri() {
        return this.mUri;
    }

    public Bitmap getBitmap() {
        return this.mBitmap;
    }
}
