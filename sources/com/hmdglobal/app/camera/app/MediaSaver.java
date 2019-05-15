package com.hmdglobal.app.camera.app;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.location.Location;
import android.net.Uri;
import com.hmdglobal.app.camera.exif.ExifInterface;
import com.hmdglobal.app.camera.gdepthfilter.GDepth;
import com.hmdglobal.app.camera.gdepthfilter.GImage;

public interface MediaSaver {

    public interface OnMediaSavedListener {
        void onMediaSaved(Uri uri);
    }

    public interface QueueListener {
        void onQueueStatus(boolean z);
    }

    void addImage(byte[] bArr, String str, long j, Location location, int i, int i2, int i3, ExifInterface exifInterface, OnMediaSavedListener onMediaSavedListener, ContentResolver contentResolver);

    void addImage(byte[] bArr, String str, long j, Location location, int i, ExifInterface exifInterface, OnMediaSavedListener onMediaSavedListener, ContentResolver contentResolver);

    void addImage(byte[] bArr, String str, Location location, int i, int i2, int i3, ExifInterface exifInterface, OnMediaSavedListener onMediaSavedListener, ContentResolver contentResolver);

    void addVideo(String str, ContentValues contentValues, OnMediaSavedListener onMediaSavedListener, ContentResolver contentResolver);

    void addXmpImage(byte[] bArr, GImage gImage, GDepth gDepth, String str, long j, Location location, int i, int i2, int i3, ExifInterface exifInterface, String str2, OnMediaSavedListener onMediaSavedListener, ContentResolver contentResolver, String str3);

    boolean isQueueFull();

    void setQueueListener(QueueListener queueListener);
}
