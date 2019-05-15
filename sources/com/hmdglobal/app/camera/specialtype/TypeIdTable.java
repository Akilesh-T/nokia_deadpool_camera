package com.hmdglobal.app.camera.specialtype;

public final class TypeIdTable {
    public static final String SELECT_MEDIA_STORE_ID = "media_store_id = ?";
    static final String TABLE_NAME = "type_uri";

    public interface Columns {
        public static final String MEDIA_STORE_ID = "media_store_id";
        public static final String SPECIAL_TYPE_ID = "special_type_id";
    }

    static String getCreateSql() {
        return "CREATE TABLE type_uri (media_store_id INTEGER PRIMARY KEY, special_type_id TEXT NOT NULL)";
    }
}
