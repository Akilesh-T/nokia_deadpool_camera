package com.hmdglobal.app.camera.specialtype;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.ProviderInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore.Images.Media;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import com.google.android.apps.photos.api.IconQuery;
import com.google.android.apps.photos.api.IconQuery.Type;
import com.google.android.apps.photos.api.PhotosOemApi;
import com.google.android.apps.photos.api.ProcessingMetadataQuery;
import com.google.android.apps.photos.api.SpecialTypeMetadataQuery;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.specialtype.ProcessingMediaManager.ProcessingMedia;
import com.hmdglobal.app.camera.specialtype.utils.TrustedPartners;
import com.hmdglobal.app.camera.util.PermissionsUtil;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;

public final class HmdThumbnailProvider extends ContentProvider {
    public static final String AUTHORITY = "com.hmdglobal.app.camera.provider.HmdThumbnailProvider";
    private static final int DELETE_BY_ID = 7;
    private static final int LOAD_BADGE_ICON = 3;
    private static final int LOAD_DIALOG_ICON = 5;
    private static final int LOAD_INTERACT_ICON = 4;
    private static final int LOAD_SEARCH_ICON = 6;
    private static final String MATCH_HELPER_ALL = "helper";
    private static final String MATCH_HELPER_ITEM_BY_ID = "helper/#";
    private static final String MATCH_PATH_DELETE_BY_ID = "delete/#";
    private static final String MATCH_PATH_SPECIAL_TYPE_ID = "type/*";
    private static final String MATCH_PATH_SPECIAL_TYPE_METADATA = "data/*";
    private static final String MATCH_PROCESSING_BY_ID = "processing/#";
    private static final String MATCH_PROCESSING_METADATA = "processing";
    private static final String MATCH_REMAINDER = "/*";
    public static final Uri PLACE_HOLDER_URI = Media.EXTERNAL_CONTENT_URI;
    private static final int QUERY_HELPER_DB = 10;
    private static final int QUERY_HELPER_DB_BY_ID = 11;
    private static final int QUERY_PROCESSING_METADATA = 8;
    private static final int QUERY_PROCESSING_METADATA_BY_ID = 9;
    private static final int QUERY_SPECIAL_TYPE_ID = 1;
    private static final int QUERY_SPECIAL_TYPE_METADATA = 2;
    private static final int RESOURCE_ID_PATH_INDEX = 1;
    private static final String TAG = "HmdThumbnailProvider";
    private static final String[] TYPE_URI_PROJECTION = new String[]{"special_type_id"};
    private static final int VERSION = 4;
    private String authority;
    private TrustedPartners trustedPartners;
    private UriMatcher uriMatcher;

    public void attachInfo(Context context, ProviderInfo info) {
        super.attachInfo(context, info);
        this.trustedPartners = new TrustedPartners(context, new HashSet(Arrays.asList(context.getResources().getStringArray(R.array.trusted_certificates))));
        this.authority = info.authority;
        this.uriMatcher = new UriMatcher(-1);
        this.uriMatcher.addURI(this.authority, MATCH_PATH_SPECIAL_TYPE_ID, 1);
        this.uriMatcher.addURI(this.authority, MATCH_PATH_SPECIAL_TYPE_METADATA, 2);
        this.uriMatcher.addURI(this.authority, IconQuery.MATCH_PATH_BADGE, 3);
        this.uriMatcher.addURI(this.authority, IconQuery.MATCH_PATH_SEARCH, 6);
        this.uriMatcher.addURI(this.authority, IconQuery.MATCH_PATH_INTERACT, 4);
        this.uriMatcher.addURI(this.authority, IconQuery.MATCH_PATH_DIALOG, 5);
        this.uriMatcher.addURI(this.authority, MATCH_PATH_DELETE_BY_ID, 7);
        this.uriMatcher.addURI(this.authority, "processing", 8);
        this.uriMatcher.addURI(this.authority, MATCH_PROCESSING_BY_ID, 9);
        this.uriMatcher.addURI(this.authority, MATCH_HELPER_ALL, 10);
        this.uriMatcher.addURI(this.authority, MATCH_HELPER_ITEM_BY_ID, 11);
    }

    public boolean onCreate() {
        return true;
    }

    @Nullable
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("call: method = ");
        stringBuilder.append(method);
        Log.d(str, stringBuilder.toString());
        if (TextUtils.equals("version", method)) {
            return querySpecialTypesVersion();
        }
        return super.call(method, arg, extras);
    }

    @Nullable
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("query: uri = ");
        stringBuilder.append(uri);
        Log.d(str, stringBuilder.toString());
        validateCallingPackage();
        int match = this.uriMatcher.match(uri);
        switch (match) {
            case 1:
                return querySpecialTypeId(uri);
            case 2:
                return querySpecialTypeMetadata(uri, projection);
            default:
                switch (match) {
                    case 8:
                        return queryProcessingMetadata(null);
                    case 9:
                        return queryProcessingMetadata(Long.valueOf(ContentUris.parseId(uri)));
                    case 10:
                        return queryHelperDB(projection, selection, selectionArgs, sortOrder);
                    default:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Unrecognized uri: ");
                        stringBuilder.append(uri);
                        throw new IllegalArgumentException(stringBuilder.toString());
                }
        }
    }

    private Cursor queryHelperDB(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return ProviderDbHelper.get(getContext()).getReadableDatabase().query("type_uri", projection, selection, selectionArgs, null, null, sortOrder);
    }

    private Cursor querySpecialTypeId(Uri uri) {
        return queryOrScanAndQuery(ProviderDbHelper.get(getContext()).getReadableDatabase(), PhotosOemApi.getMediaStoreIdFromQueryTypeUri(uri));
    }

    private static Cursor queryOrScanAndQuery(SQLiteDatabase db, long mediaStoreId) {
        SpecialType type = querySpecialTypeId(db, mediaStoreId);
        SpecialType specialType = SpecialType.UNKNOWN;
        MatrixCursor result = new MatrixCursor(TYPE_URI_PROJECTION);
        if (!(type == SpecialType.NONE || type == SpecialType.UNKNOWN)) {
            result.addRow(new Object[]{type.name()});
        }
        return result;
    }

    private static SpecialType querySpecialTypeId(SQLiteDatabase db, long mediaStoreId) {
        SpecialType result = SpecialType.UNKNOWN;
        SQLiteDatabase cursor = db;
        Cursor cursor2 = cursor.query("type_uri", TYPE_URI_PROJECTION, TypeIdTable.SELECT_MEDIA_STORE_ID, new String[]{String.valueOf(mediaStoreId)}, null, null, null);
        try {
            if (cursor2.moveToFirst()) {
                result = SpecialType.valueOf(cursor2.getString(cursor2.getColumnIndexOrThrow("special_type_id")));
            }
            cursor2.close();
            return result;
        } catch (Throwable th) {
            cursor2.close();
        }
    }

    private static void scanUriAndUpdateDb(SQLiteDatabase db, long mediaStoreId) {
        SpecialType specialType = detectSpecialType(mediaStoreId);
        ContentValues values = new ContentValues();
        values.put("media_store_id", Long.valueOf(mediaStoreId));
        values.put("special_type_id", specialType.name());
        db.replace("type_uri", null, values);
    }

    private static SpecialType detectSpecialType(long mediaStoreId) {
        if (mediaStoreId % 3 == 0) {
            return SpecialType.NONE;
        }
        return mediaStoreId % 2 == 0 ? SpecialType.GDEPTH_TYPE : SpecialType.BOKEH_TYPE;
    }

    private Bundle querySpecialTypesVersion() {
        Bundle result = new Bundle();
        result.putInt("version", 4);
        return result;
    }

    private Cursor queryProcessingMetadata(@Nullable Long mediaStoreId) {
        MatrixCursor cursor = new MatrixCursor(new String[]{"media_store_id", ProcessingMetadataQuery.PROGRESS_STATUS, ProcessingMetadataQuery.PROGRESS_PERCENTAGE});
        ProcessingMediaManager processingMediaManager = ProcessingMediaManager.getInstance(getContext());
        boolean queryAll = mediaStoreId == null;
        for (ProcessingMedia media : processingMediaManager.getProcessingMedia()) {
            if (queryAll || media.getMediaStoreId() == mediaStoreId.longValue()) {
                cursor.addRow(new Object[]{Long.valueOf(media.getMediaStoreId()), Integer.valueOf(media.getProgressStatus()), Integer.valueOf(media.getProgressPercentage())});
            }
        }
        cursor.moveToPosition(-1);
        return cursor;
    }

    private Cursor querySpecialTypeMetadata(Uri uri, String[] projection) {
        SpecialType specialType = SpecialType.valueOf(PhotosOemApi.getSpecialTypeIdFromQueryDataUri(uri));
        MatrixCursor result = new MatrixCursor(projection);
        Object[] row = new Object[projection.length];
        int i = 0;
        for (String column : projection) {
            Object obj = -1;
            int hashCode = column.hashCode();
            if (hashCode != -2067576059) {
                if (hashCode != 221347946) {
                    if (hashCode != 1932752118) {
                        if (hashCode == 1971189053 && column.equals(SpecialTypeMetadataQuery.SPECIAL_TYPE_DESCRIPTION)) {
                            obj = 2;
                        }
                    } else if (column.equals(SpecialTypeMetadataQuery.SPECIAL_TYPE_CONFIGURATION)) {
                        obj = null;
                    }
                } else if (column.equals(SpecialTypeMetadataQuery.SPECIAL_TYPE_NAME)) {
                    obj = 1;
                }
            } else if (column.equals(SpecialTypeMetadataQuery.SPECIAL_TYPE_ICON_URI)) {
                obj = 3;
            }
            String string;
            switch (obj) {
                case null:
                    row[i] = specialType.getConfiguration().getKey();
                    break;
                case 1:
                    if (getContext() != null) {
                        string = getContext().getString(specialType.nameResourceId);
                    } else {
                        string = "error";
                    }
                    row[i] = string;
                    break;
                case 2:
                    if (getContext() != null) {
                        string = getContext().getString(specialType.descriptionResourceId);
                    } else {
                        string = "error";
                    }
                    row[i] = string;
                    break;
                case 3:
                    row[i] = new Builder().scheme("content").authority(this.authority).appendPath(IconQuery.PATH_ICON).appendPath(String.valueOf(specialType.iconResourceId));
                    break;
                default:
                    row[i] = null;
                    break;
            }
            i++;
        }
        result.addRow(row);
        return result;
    }

    @Nullable
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        validateCallingPackage();
        StringBuilder stringBuilder;
        if ("r".equals(mode)) {
            int match = this.uriMatcher.match(uri);
            String str;
            if (match != 9) {
                switch (match) {
                    case 3:
                        if (Log.isLoggable(TAG, 4)) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("loading badge icon ");
                            stringBuilder.append(uri);
                            Log.i(str, stringBuilder.toString());
                        }
                        return loadIcon(uri, Type.BADGE);
                    case 4:
                        if (Log.isLoggable(TAG, 4)) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("loading interact icon ");
                            stringBuilder.append(uri);
                            Log.i(str, stringBuilder.toString());
                        }
                        return loadIcon(uri, Type.INTERACT);
                    case 5:
                        if (Log.isLoggable(TAG, 4)) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("loading dialog icon ");
                            stringBuilder.append(uri);
                            Log.i(str, stringBuilder.toString());
                        }
                        return loadIcon(uri, Type.DIALOG);
                    case 6:
                        if (Log.isLoggable(TAG, 4)) {
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("loading search icon ");
                            stringBuilder.append(uri);
                            Log.i(str, stringBuilder.toString());
                        }
                        return loadIcon(uri, Type.SEARCH);
                    default:
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Unrecognized format: ");
                        stringBuilder.append(uri);
                        throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            if (Log.isLoggable(TAG, 4)) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("loading processing thumb ");
                stringBuilder.append(uri);
                Log.i(str, stringBuilder.toString());
            }
            return loadProcessingThumb(ContentUris.parseId(uri));
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Unsupported mode: ");
        stringBuilder.append(mode);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    @Nullable
    private ParcelFileDescriptor loadIcon(Uri uri, Type type) throws FileNotFoundException {
        int resourceId = 0;
        Type type2;
        try {
            resourceId = Integer.parseInt((String) uri.getPathSegments().get(1));
            if (getContext() != null) {
                Resources resources = getContext().getResources();
                BitmapDrawable bitmapDrawable = (BitmapDrawable) resources.getDrawable(resourceId);
                int pixels = resources.getDimensionPixelSize(type.getDimensionResourceId());
                Bitmap original = Bitmap.createScaledBitmap(bitmapDrawable.getBitmap(), pixels, pixels, false);
                Bitmap result = original;
                if (type == Type.DIALOG) {
                    float[] matrix = new float[]{0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f};
                    Bitmap colorSwapped = Bitmap.createBitmap(original.getWidth(), original.getHeight(), original.getConfig());
                    Canvas canvas = new Canvas(colorSwapped);
                    Paint paint = new Paint();
                    paint.setColorFilter(new ColorMatrixColorFilter(matrix));
                    canvas.drawBitmap(original, 0.0f, 0.0f, paint);
                    result = colorSwapped;
                }
                return writeBitmapToFd(result, CompressFormat.PNG);
            }
            type2 = type;
            Log.e(TAG, "loadIcon - getContext == null");
            throw new FileNotFoundException("loadIcon - getContext == null");
        } catch (NumberFormatException e) {
            type2 = type;
            NumberFormatException numberFormatException = e;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("loadIcon - Integer.parseInt");
            stringBuilder.append(e.getMessage());
            Log.e(TAG, stringBuilder.toString());
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("loadIcon - Integer.parseInt");
            stringBuilder2.append(e.getMessage());
            throw new FileNotFoundException(stringBuilder2.toString());
        }
    }

    private ParcelFileDescriptor loadProcessingThumb(long mediaStoreId) throws FileNotFoundException {
        ProcessingMedia media = ProcessingMediaManager.getInstance(getContext()).getById(mediaStoreId);
        if (media != null) {
            return transfer(media.getThumbnailPath());
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Media removed: ");
        stringBuilder.append(mediaStoreId);
        Log.d(TAG, stringBuilder.toString());
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Media removed: ");
        stringBuilder2.append(mediaStoreId);
        throw new FileNotFoundException(stringBuilder2.toString());
    }

    private ParcelFileDescriptor transfer(String path) throws FileNotFoundException {
        return ParcelFileDescriptor.open(new File(path), 268435456);
    }

    private ParcelFileDescriptor writeBitmapToFd(Bitmap bitmap, CompressFormat compressFormat) throws FileNotFoundException {
        IOException exception;
        ParcelFileDescriptor inputFd = null;
        OutputStream outputStream = null;
        try {
            Log.d(TAG, "--- n + ");
            ParcelFileDescriptor[] descriptors = ParcelFileDescriptor.createPipe();
            ParcelFileDescriptor outputFd = descriptors[null];
            inputFd = descriptors[1];
            outputStream = new BufferedOutputStream(new FileOutputStream(inputFd.getFileDescriptor()));
            bitmap.compress(compressFormat, 50, outputStream);
            outputStream.close();
            Log.d(TAG, "--2- n + ");
            if (inputFd != null) {
                try {
                    inputFd.close();
                } catch (IOException e) {
                    exception = e;
                }
            }
            try {
                outputStream.close();
            } catch (IOException e2) {
                exception = e2;
            }
            return outputFd;
        } catch (IOException e3) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("1 - e + ");
            stringBuilder.append(e3.getMessage());
            Log.d(str, stringBuilder.toString());
            exception = e3;
            if (inputFd != null) {
                try {
                    inputFd.close();
                } catch (IOException e32) {
                    exception = e32;
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e322) {
                    exception = e322;
                }
            }
            throw new FileNotFoundException(exception.getMessage());
        } catch (Throwable th) {
            if (inputFd != null) {
                try {
                    inputFd.close();
                } catch (IOException e4) {
                    exception = e4;
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e42) {
                    exception = e42;
                }
            }
        }
    }

    @Nullable
    public String getType(@NonNull Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        if (validateIsSelf()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("insert, uri = ");
            stringBuilder.append(uri);
            Log.i(str, stringBuilder.toString());
            if (getContext() != null) {
                Uri newUri = new Builder().scheme("content").authority(this.authority).appendPath(String.valueOf(ProviderDbHelper.get(getContext()).getWritableDatabase().insert("type_uri", "", values))).build();
                getContext().getContentResolver().notifyChange(uri, null);
                return newUri;
            }
            throw new UnsupportedOperationException();
        }
        throw new UnsupportedOperationException();
    }

    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if (validateIsSelf()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("update, uri = ");
            stringBuilder.append(uri);
            Log.i(str, stringBuilder.toString());
            if (getContext() != null) {
                int count = ProviderDbHelper.get(getContext()).getWritableDatabase().update("type_uri", values, TypeIdTable.SELECT_MEDIA_STORE_ID, selectionArgs);
                getContext().getContentResolver().notifyChange(uri, null);
                return count;
            }
            throw new UnsupportedOperationException();
        }
        throw new UnsupportedOperationException();
    }

    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        if (getContext() != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("delete, uri = ");
            stringBuilder.append(uri);
            Log.i(str, stringBuilder.toString());
            if (validateIsSelf()) {
                SQLiteDatabase db = ProviderDbHelper.get(getContext()).getWritableDatabase();
                long mediaStoreId = PhotosOemApi.getMediaStoreIdFromQueryTypeUri(uri);
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("delete(),  mediaStoreId = ");
                stringBuilder2.append(mediaStoreId);
                Log.i(str2, stringBuilder2.toString());
                int count = db.delete("type_uri", TypeIdTable.SELECT_MEDIA_STORE_ID, new String[]{String.valueOf(mediaStoreId)});
                str2 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("delete(),  deleteCount = ");
                stringBuilder2.append(count);
                Log.i(str2, stringBuilder2.toString());
                getContext().getContentResolver().notifyChange(uri, null);
                return count;
            }
            validateCallingPackage();
            if (this.uriMatcher.match(uri) == 7) {
                return 1;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Unrecognized uri: ");
            stringBuilder.append(uri);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
        throw new UnsupportedOperationException();
    }

    private void validateCallingPackage() {
    }

    private boolean validateIsSelf() {
        String packageName = getCallingPackage();
        return (packageName == null || getContext() == null || !packageName.equals(getContext().getPackageName())) ? false : true;
    }

    public static String getSpecialTypeIdFromExif(Context context, long mediaStoreId) {
        if (ContextCompat.checkSelfPermission(context, PermissionsUtil.PERMS_READ_EXTERNAL_STORAGE) == 0) {
            long time = System.currentTimeMillis();
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(PLACE_HOLDER_URI, new String[]{"_data"}, "_id=?", new String[]{String.valueOf(mediaStoreId)}, null, null);
                if (cursor == null) {
                    return "";
                }
                cursor.moveToFirst();
                String r = new ExifInterface(cursor.getString(cursor.getColumnIndex("_data"))).getAttribute("Orientation");
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ExifInterface.TAG_COLOR_SPACE - ");
                stringBuilder.append(r);
                Log.d(str, stringBuilder.toString());
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("getSpecialTypeIdFromExif - ");
                stringBuilder.append(System.currentTimeMillis() - time);
                Log.d(str, stringBuilder.toString());
                return r;
            } catch (Exception e) {
                e.printStackTrace();
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Throwable th) {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return "";
    }
}
