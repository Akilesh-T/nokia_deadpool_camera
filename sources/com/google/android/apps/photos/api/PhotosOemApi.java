package com.google.android.apps.photos.api;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.support.annotation.Nullable;
import com.hmdglobal.app.camera.specialtype.HmdThumbnailProvider;

public final class PhotosOemApi {
    public static final String ACTION_REVIEW_PROCESSING_URI_INTENT_EXTRA = "processing_uri_intent_extra";
    public static final int INITIAL_VERSION = 1;
    public static final String METHOD_EDITOR_DATA = "editor_data";
    public static final String METHOD_EDITOR_DATA_ACTIVITY_NAME = "editor_activity_name";
    public static final String METHOD_EDITOR_DATA_ICON_URI = "editor_icon_uri";
    public static final String METHOD_EDITOR_DATA_PACKAGE_NAME = "editor_package_name";
    public static final String METHOD_VERSION = "version";
    public static final String METHOD_VERSION_KEY_VERSION_INT = "version";
    public static final String PATH_DELETE = "delete";
    public static final String PATH_DISCOVER = "discover";
    public static final String PATH_PROCESSING_DATA = "processing";
    public static final String PATH_SPECIAL_TYPE_DATA = "data";
    public static final String PATH_SPECIAL_TYPE_ID = "type";

    public static int getVersion(Context context) {
        Bundle bundle = context.getContentResolver().call(new Builder().scheme("content").authority(getAuthority(context)).build(), "version", null, null);
        if (bundle == null) {
            return 1;
        }
        return bundle.getInt("version");
    }

    public static Bundle getPreferredEditorData(Context context) {
        return context.getContentResolver().call(new Builder().scheme("content").authority(getAuthority(context)).build(), METHOD_EDITOR_DATA, null, null);
    }

    public static String getAuthority(Context context) {
        return HmdThumbnailProvider.AUTHORITY;
    }

    public static long getMediaStoreIdFromQueryTypeUri(Uri queryTypeUri) {
        return Long.parseLong(Uri.decode(queryTypeUri.getLastPathSegment()));
    }

    public static String getSpecialTypeIdFromQueryDataUri(Uri queryDataUri) {
        return Uri.decode(queryDataUri.getLastPathSegment());
    }

    @Nullable
    public static String getSpecialTypeId(Context context, long mediaStoreId) {
        Cursor cursor = context.getContentResolver().query(getQueryTypeUri(context, mediaStoreId), null, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    String string = cursor.getString(cursor.getColumnIndexOrThrow("special_type_id"));
                    return string;
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        return null;
    }

    public static Uri getQueryProcessingUri(Context context, long mediaStoreId) {
        return getBaseBuilder(context).appendPath(PATH_PROCESSING_DATA).appendPath(String.valueOf(mediaStoreId)).build();
    }

    public static Uri getQueryProcessingUri(Context context) {
        return getBaseBuilder(context).appendPath(PATH_PROCESSING_DATA).build();
    }

    public static Uri getQueryTypeUri(Context context, long mediaStoreId) {
        return getBaseBuilder(context).appendPath(PATH_SPECIAL_TYPE_ID).appendPath(String.valueOf(mediaStoreId)).build();
    }

    public static Uri getQueryDataUri(Context context, String specialTypeId) {
        return getBaseBuilder(context).appendPath("data").appendEncodedPath(Uri.encode(specialTypeId)).build();
    }

    public static Uri getQueryDiscoverDataUri(Context context) {
        return getBaseBuilder(context).appendPath(PATH_DISCOVER).build();
    }

    public static Uri getQueryDiscoverSupportedSpecialTypesUri(Context context, String discoverId) {
        return getBaseBuilder(context).appendPath(PATH_DISCOVER).appendPath(discoverId).build();
    }

    public static Uri getDeleteUri(Context context, long mediaStoreId) {
        return getBaseBuilder(context).appendPath(PATH_DELETE).appendPath(String.valueOf(mediaStoreId)).build();
    }

    private static Builder getBaseBuilder(Context context) {
        return new Builder().scheme("content").authority(getAuthority(context));
    }
}
