package com.morphoinc.utils.multimedia;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore.Images.Media;
import android.provider.MediaStore.Images.Thumbnails;
import android.provider.MediaStore.Video;
import com.hmdglobal.app.camera.provider.InfoTable;
import java.io.File;

public class MediaProviderUtils {
    public static final int ROTATION_180 = 180;
    public static final int ROTATION_270 = 270;
    public static final int ROTATION_90 = 90;
    public static final int ROTATION_NORMAL = 0;

    public static Uri getExternalImageContentUri(ContentResolver cr, String filePath) {
        return getImageContentUri(cr, filePath, Media.EXTERNAL_CONTENT_URI);
    }

    public static Uri getInternalImageContentUri(ContentResolver cr, String filePath) {
        return getImageContentUri(cr, filePath, Media.INTERNAL_CONTENT_URI);
    }

    private static Uri getImageContentUri(ContentResolver cr, String filePath, Uri storageUri) {
        if (cr == null || filePath == null) {
            return null;
        }
        ContentResolver contentResolver = cr;
        Uri uri = storageUri;
        Cursor cursor = contentResolver.query(uri, new String[]{"_id"}, "_data=?", new String[]{filePath}, "bucket_display_name");
        if (cursor == null) {
            return null;
        }
        if (cursor.getCount() <= 0) {
            cursor.close();
            return null;
        }
        cursor.moveToFirst();
        String id = cursor.getString(cursor.getColumnIndex("_id"));
        String content = new StringBuilder();
        content.append(storageUri.toString());
        content.append(File.separator);
        content.append(id);
        content = content.toString();
        cursor.close();
        return Uri.parse(content);
    }

    public static String getImageFilePath(ContentResolver cr, Uri uri) {
        return getContentFilePath(cr, uri, "_data");
    }

    public static String getVideoFilePath(ContentResolver cr, Uri uri) {
        return getContentFilePath(cr, uri, "_data");
    }

    private static String getContentFilePath(ContentResolver cr, Uri uri, String colunmName) {
        if (cr == null || uri == null) {
            return null;
        }
        Cursor cursor = cr.query(uri, new String[]{colunmName}, null, null, null);
        if (cursor == null) {
            return null;
        }
        if (cursor.getCount() <= 0) {
            cursor.close();
            return null;
        }
        cursor.moveToFirst();
        String filePath = cursor.getString(cursor.getColumnIndex(colunmName));
        cursor.close();
        return filePath;
    }

    public static String getMimeType(ContentResolver cr, Uri uri) {
        Cursor cursor = cr.query(uri, new String[]{InfoTable.MIME_TYPE}, null, null, null);
        if (cursor == null) {
            return "";
        }
        cursor.moveToFirst();
        String mime_type = cursor.getString(cursor.getColumnIndex(InfoTable.MIME_TYPE));
        cursor.close();
        return mime_type;
    }

    public static Uri addImageExternal(ContentResolver cr, String filePath, String mime, int rotation) {
        return addImage(cr, filePath, mime, rotation, Media.EXTERNAL_CONTENT_URI);
    }

    public static Uri addImageInternal(ContentResolver cr, String filePath, String mime, int rotation) {
        return addImage(cr, filePath, mime, rotation, Media.INTERNAL_CONTENT_URI);
    }

    private static Uri addImage(ContentResolver cr, String filePath, String mime, int rotation, Uri storageUri) {
        if (cr == null || filePath == null || mime == null) {
            return null;
        }
        File file = new File(filePath);
        String fileName = file.getName();
        ContentValues contentValues = new ContentValues(9);
        long time = System.currentTimeMillis();
        if (!(rotation == 0 || rotation == 90 || rotation == ROTATION_180 || rotation == ROTATION_270)) {
            rotation = 0;
        }
        contentValues.put("title", fileName);
        contentValues.put("_display_name", fileName);
        contentValues.put("datetaken", Long.valueOf(time));
        contentValues.put("date_added", Long.valueOf(time / 1000));
        contentValues.put(InfoTable.DATE_MODIFIED, Long.valueOf(time / 1000));
        contentValues.put(InfoTable.MIME_TYPE, mime);
        contentValues.put("orientation", Integer.valueOf(rotation));
        contentValues.put("_data", filePath);
        contentValues.put("_size", Long.valueOf(file.length()));
        return cr.insert(storageUri, contentValues);
    }

    public static int deleteImageExternal(ContentResolver cr, String filePath) {
        return deleteImage(cr, filePath, Media.EXTERNAL_CONTENT_URI);
    }

    public static int deleteImageInternal(ContentResolver cr, String filePath) {
        return deleteImage(cr, filePath, Media.INTERNAL_CONTENT_URI);
    }

    private static int deleteImage(ContentResolver cr, String filePath, Uri storageUri) {
        int result = -1;
        if (cr == null || filePath == null) {
            return -1;
        }
        ContentResolver contentResolver = cr;
        Uri uri = storageUri;
        Cursor cursor = contentResolver.query(uri, new String[]{"_id"}, "_data=?", new String[]{filePath}, "bucket_display_name");
        if (cursor != null) {
            if (cursor.getCount() != 0) {
                cursor.moveToFirst();
                result = cr.delete(ContentUris.appendId(storageUri.buildUpon(), cursor.getLong(cursor.getColumnIndex("_id"))).build(), null, null);
            }
            cursor.close();
        }
        return result;
    }

    public static Uri addVideoExternal(ContentResolver cr, String filePath, String mime, long duration) {
        return addVideo(cr, filePath, mime, duration, Video.Media.EXTERNAL_CONTENT_URI);
    }

    public static Uri addVideoInternal(ContentResolver cr, String filePath, String mime, long duration) {
        return addVideo(cr, filePath, mime, duration, Video.Media.INTERNAL_CONTENT_URI);
    }

    private static Uri addVideo(ContentResolver cr, String filePath, String mime, long duration, Uri storageUri) {
        if (cr == null || filePath == null || mime == null) {
            return null;
        }
        File file = new File(filePath);
        String fileName = file.getName();
        ContentValues contentValues = new ContentValues(9);
        long time = System.currentTimeMillis();
        contentValues.put("title", fileName);
        contentValues.put("_display_name", fileName);
        contentValues.put("datetaken", Long.valueOf(time));
        contentValues.put("date_added", Long.valueOf(time / 1000));
        contentValues.put(InfoTable.DATE_MODIFIED, Long.valueOf(time / 1000));
        contentValues.put(InfoTable.MIME_TYPE, mime);
        contentValues.put("_data", filePath);
        contentValues.put("_size", Long.valueOf(file.length()));
        if (duration > 0) {
            contentValues.put(InfoTable.DURATION, Long.toString(duration));
        }
        return cr.insert(storageUri, contentValues);
    }

    public static int deleteVideoExternal(ContentResolver cr, String filePath) {
        return deleteVideo(cr, filePath, Video.Media.EXTERNAL_CONTENT_URI);
    }

    public static int deleteVideoInternal(ContentResolver cr, String filePath) {
        return deleteVideo(cr, filePath, Video.Media.INTERNAL_CONTENT_URI);
    }

    private static int deleteVideo(ContentResolver cr, String filePath, Uri storageUri) {
        int result = -1;
        if (cr == null || filePath == null) {
            return -1;
        }
        ContentResolver contentResolver = cr;
        Uri uri = storageUri;
        Cursor cursor = contentResolver.query(uri, new String[]{"_id"}, "_data=?", new String[]{filePath}, "title");
        if (cursor != null) {
            if (cursor.getCount() != 0) {
                cursor.moveToFirst();
                result = cr.delete(ContentUris.appendId(storageUri.buildUpon(), cursor.getLong(cursor.getColumnIndex("_id"))).build(), null, null);
            }
            cursor.close();
        }
        return result;
    }

    public static Bitmap getImageThumbnailBitmapExternal(ContentResolver cr, String filePath, int size) {
        return getImageThumbnailBitmap(cr, filePath, size, Media.EXTERNAL_CONTENT_URI);
    }

    public static Bitmap getImageThumbnailBitmapInternal(ContentResolver cr, String filePath, int size) {
        return getImageThumbnailBitmap(cr, filePath, size, Media.INTERNAL_CONTENT_URI);
    }

    private static Bitmap getImageThumbnailBitmap(ContentResolver cr, String filePath, int size, Uri storageUri) {
        Bitmap thumbnail = null;
        if (cr == null || filePath == null) {
            return null;
        }
        if (!(size == 1 || size == 3)) {
            size = 1;
        }
        String[] strArr = new String[1];
        int i = 0;
        strArr[0] = filePath;
        Cursor cursor = cr.query(storageUri, new String[]{"_id"}, "_data=?", strArr, "bucket_display_name");
        if (cursor != null) {
            int count = cursor.getCount();
            int index = cursor.getColumnIndex("_id");
            int[] videoIds = new int[count];
            cursor.moveToFirst();
            while (i < count) {
                videoIds[i] = cursor.getInt(index);
                thumbnail = Thumbnails.getThumbnail(cr, (long) videoIds[i], size, null);
                if (thumbnail != null) {
                    break;
                }
                cursor.moveToNext();
                i++;
            }
            cursor.close();
        }
        return thumbnail;
    }

    public static Bitmap getVideoThumbnailBitmapExternal(ContentResolver cr, String filePath, int size) {
        return getVideoThumbnailBitmap(cr, filePath, size, Video.Media.EXTERNAL_CONTENT_URI);
    }

    public static Bitmap getVideoThumbnailBitmapInternal(ContentResolver cr, String filePath, int size) {
        return getVideoThumbnailBitmap(cr, filePath, size, Video.Media.INTERNAL_CONTENT_URI);
    }

    private static Bitmap getVideoThumbnailBitmap(ContentResolver cr, String filePath, int size, Uri storageUri) {
        Bitmap thumbnail = null;
        if (cr == null || filePath == null) {
            return null;
        }
        if (!(size == 1 || size == 3)) {
            size = 1;
        }
        String[] strArr = new String[1];
        int i = 0;
        strArr[0] = filePath;
        Cursor cursor = cr.query(storageUri, new String[]{"_id"}, "_data=?", strArr, "title");
        if (cursor != null) {
            int count = cursor.getCount();
            int index = cursor.getColumnIndex("_id");
            int[] videoIds = new int[count];
            cursor.moveToFirst();
            while (i < count) {
                videoIds[i] = cursor.getInt(index);
                thumbnail = Video.Thumbnails.getThumbnail(cr, (long) videoIds[i], size, null);
                if (thumbnail != null) {
                    break;
                }
                cursor.moveToNext();
                i++;
            }
            cursor.close();
        }
        return thumbnail;
    }
}
