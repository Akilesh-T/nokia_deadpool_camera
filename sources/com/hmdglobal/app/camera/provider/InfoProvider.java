package com.hmdglobal.app.camera.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

public class InfoProvider extends ContentProvider {
    public static final int VIDETAIL_DIR = 0;
    public static final int VIDETAIL_ITEM = 1;
    private static UriMatcher mUriMacher = new UriMatcher(-1);
    private InfoDatabaseHelper mDbHelper;

    static {
        mUriMacher.addURI(ProviderUtil.AUTHORITY, InfoDatabaseHelper.TABLE_NAME, 0);
        mUriMacher.addURI(ProviderUtil.AUTHORITY, "videtail/#", 1);
    }

    public boolean onCreate() {
        this.mDbHelper = new InfoDatabaseHelper(getContext());
        return true;
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = this.mDbHelper.getReadableDatabase();
        Uri uri2 = uri;
        switch (mUriMacher.match(uri2)) {
            case 0:
                return db.query(InfoDatabaseHelper.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
            case 1:
                String videtailId = (String) uri2.getPathSegments().get(1);
                String[] strArr = new String[]{videtailId};
                return db.query(InfoDatabaseHelper.TABLE_NAME, projection, "id = ?", strArr, null, null, sortOrder);
            default:
                return null;
        }
    }

    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = this.mDbHelper.getWritableDatabase();
        switch (mUriMacher.match(uri)) {
            case 0:
            case 1:
                long newVidetailId = db.insert(InfoDatabaseHelper.TABLE_NAME, null, values);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(ProviderUtil.VIDETAIL_URI);
                stringBuilder.append(newVidetailId);
                return Uri.parse(stringBuilder.toString());
            default:
                return null;
        }
    }

    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = this.mDbHelper.getReadableDatabase();
        switch (mUriMacher.match(uri)) {
            case 0:
                return db.update(InfoDatabaseHelper.TABLE_NAME, values, selection, selectionArgs);
            case 1:
                String videtailId = (String) uri.getPathSegments().get(1);
                return db.update(InfoDatabaseHelper.TABLE_NAME, values, "id = ?", new String[]{videtailId});
            default:
                return 0;
        }
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = this.mDbHelper.getWritableDatabase();
        switch (mUriMacher.match(uri)) {
            case 0:
                return db.delete(InfoDatabaseHelper.TABLE_NAME, selection, selectionArgs);
            case 1:
                String videtailId = (String) uri.getPathSegments().get(1);
                return db.delete(InfoDatabaseHelper.TABLE_NAME, "id = ?", new String[]{videtailId});
            default:
                return 0;
        }
    }

    public String getType(Uri uri) {
        return "video/mp4";
    }
}
