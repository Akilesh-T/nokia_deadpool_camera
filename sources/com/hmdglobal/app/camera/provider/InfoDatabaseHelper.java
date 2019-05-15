package com.hmdglobal.app.camera.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class InfoDatabaseHelper extends SQLiteOpenHelper {
    public static final String CREATE_TABLE = "create table videtail(id integer primary key autoincrement, title text, display_name text, mime_type text, date_taken integer, date_modified integer, data text, width integer, height integer, resolution text, size integer, latitude float, longitude float, duration integer, make text, weather text)";
    public static final String DATABASE_NAME = "VideoInfo.db";
    public static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "videtail";
    private Context mContext;

    public InfoDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
        this.mContext = context;
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS videtail");
        db.execSQL(CREATE_TABLE);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS videtail");
    }
}
