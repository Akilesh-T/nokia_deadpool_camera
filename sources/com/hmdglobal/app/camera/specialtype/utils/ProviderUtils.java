package com.hmdglobal.app.camera.specialtype.utils;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.FileUtils;
import android.text.TextUtils;
import android.util.Log;
import com.google.android.apps.photos.api.PhotosOemApi;
import com.hmdglobal.app.camera.Storage;
import com.hmdglobal.app.camera.specialtype.HmdThumbnailProvider;
import com.hmdglobal.app.camera.specialtype.TypeIdTable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class ProviderUtils {
    private static final String TAG = "ProviderUtils";

    public static void insertOrUpdateHelperDB(ContentResolver resolver, Uri uri, String specialTypeId) {
        long mediaStoreId = PhotosOemApi.getMediaStoreIdFromQueryTypeUri(uri);
        if (TextUtils.isEmpty(specialTypeId)) {
            specialTypeId = "NONE";
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("specialTypeId = ");
        stringBuilder.append(specialTypeId);
        Log.d(str, stringBuilder.toString());
        if (isDBContainThisMediaId(resolver, mediaStoreId)) {
            updateDBByMediaId(resolver, mediaStoreId, specialTypeId);
        } else {
            insertDBByMediaId(resolver, mediaStoreId, specialTypeId);
        }
    }

    private static boolean isDBContainThisMediaId(ContentResolver resolver, long mediaStoreId) {
        boolean z = true;
        Cursor cursor = resolver.query(new Builder().scheme("content").authority(HmdThumbnailProvider.AUTHORITY).appendPath("helper").build(), new String[]{"media_store_id"}, TypeIdTable.SELECT_MEDIA_STORE_ID, new String[]{String.valueOf(mediaStoreId)}, null);
        if (cursor == null || cursor.getCount() <= 0) {
            z = false;
        }
        boolean contain = z;
        if (cursor != null) {
            cursor.close();
        }
        return contain;
    }

    private static void updateDBByMediaId(ContentResolver resolver, long mediaStoreId, String specialTypeId) {
        Uri updateUri = new Builder().scheme("content").authority(HmdThumbnailProvider.AUTHORITY).appendPath(String.valueOf(mediaStoreId)).build();
        ContentValues values = new ContentValues();
        values.put("special_type_id", specialTypeId);
        resolver.update(updateUri, values, null, null);
    }

    public static void insertDBByMediaId(ContentResolver resolver, long mediaStoreId, String specialTypeId) {
        Uri helpUri = new Builder().scheme("content").authority(HmdThumbnailProvider.AUTHORITY).appendPath("helper").build();
        ContentValues values = new ContentValues();
        values.put("media_store_id", Long.valueOf(mediaStoreId));
        values.put("special_type_id", specialTypeId);
        resolver.insert(helpUri, values);
    }

    private static void saveJPG(Bitmap bitmap, String name) {
        try {
            FileOutputStream out = new FileOutputStream(new File(name));
            if (bitmap.compress(CompressFormat.JPEG, 100, out)) {
                out.flush();
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String save2Private(Activity activity, Bitmap bitmap) {
        File thumbnailDir = activity.getDir("thumbnail", 0);
        FileUtils.deleteContents(thumbnailDir);
        String thumbnailName = new StringBuilder();
        thumbnailName.append(UUID.randomUUID().toString());
        thumbnailName.append(Storage.JPEG_POSTFIX);
        thumbnailName = thumbnailName.toString();
        String path = new StringBuilder(thumbnailDir.getAbsolutePath());
        path.append("/");
        path.append(thumbnailName);
        path = path.toString();
        saveJPG(bitmap, path);
        return path;
    }
}
