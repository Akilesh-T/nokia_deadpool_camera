package com.hmdglobal.app.camera.specialtype;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

final class ProviderDbHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String NAME = "provider_db_helper";
    private static volatile ProviderDbHelper helper;

    static ProviderDbHelper get(Context context) {
        if (helper == null) {
            synchronized (ProviderDbHelper.class) {
                if (helper == null) {
                    helper = new ProviderDbHelper(context);
                }
            }
        }
        return helper;
    }

    private ProviderDbHelper(Context context) {
        super(context, NAME, null, 1);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TypeIdTable.getCreateSql());
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        int currentVersion = oldVersion;
        while (currentVersion < newVersion) {
            if (currentVersion == 1) {
                db.delete("type_uri", null, null);
                currentVersion++;
            }
        }
    }
}
