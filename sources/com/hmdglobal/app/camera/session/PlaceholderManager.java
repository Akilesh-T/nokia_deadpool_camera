package com.hmdglobal.app.camera.session;

import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.location.Location;
import android.net.Uri;
import com.hmdglobal.app.camera.Storage;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.exif.ExifInterface;
import com.hmdglobal.app.camera.util.CameraUtil;

public class PlaceholderManager {
    private static final Tag TAG = new Tag("PlaceholderMgr");
    private final Context mContext;

    public static class Session {
        final String outputTitle;
        final Uri outputUri;
        final long time;

        Session(String title, Uri uri, long timestamp) {
            this.outputTitle = title;
            this.outputUri = uri;
            this.time = timestamp;
        }
    }

    public PlaceholderManager(Context context) {
        this.mContext = context;
    }

    public Session insertPlaceholder(String title, byte[] placeholder, long timestamp) {
        if (title == null || placeholder == null) {
            throw new IllegalArgumentException("Null argument passed to insertPlaceholder");
        }
        Options options = new Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(placeholder, 0, placeholder.length, options);
        int width = options.outWidth;
        int height = options.outHeight;
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Image had bad height/width");
        }
        Uri uri = Storage.addPlaceholder(placeholder, width, height);
        if (uri == null) {
            return null;
        }
        return new Session(title, uri, timestamp);
    }

    public Session convertToPlaceholder(Uri uri) {
        return createSessionFromUri(uri);
    }

    public void finishPlaceholder(Session session, Location location, int orientation, ExifInterface exif, byte[] jpeg, int width, int height, String mimeType) {
        Session session2 = session;
        Storage.updateFormatImage(session2.outputUri, this.mContext.getContentResolver(), session2.outputTitle, session2.time, location, orientation, exif, jpeg, width, height, mimeType);
        CameraUtil.broadcastNewPicture(this.mContext, session2.outputUri);
    }

    public void replacePlaceholder(Session session, byte[] jpeg, int width, int height) {
        Storage.replacePlaceholder(session.outputUri, jpeg, width, height);
        CameraUtil.broadcastNewPicture(this.mContext, session.outputUri);
    }

    private Session createSessionFromUri(Uri uri) {
        Cursor cursor = this.mContext.getContentResolver().query(uri, new String[]{"datetaken", "_display_name"}, null, null, null);
        if (cursor == null || cursor.getCount() == 0) {
            return null;
        }
        int dateIndex = cursor.getColumnIndexOrThrow("datetaken");
        int nameIndex = cursor.getColumnIndexOrThrow("_display_name");
        cursor.moveToFirst();
        long date = cursor.getLong(dateIndex);
        String name = cursor.getString(nameIndex);
        if (name.toLowerCase().endsWith(Storage.JPEG_POSTFIX)) {
            name = name.substring(0, name.length() - Storage.JPEG_POSTFIX.length());
        }
        return new Session(name, uri, date);
    }
}
