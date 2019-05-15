package com.hmdglobal.app.camera.session;

import android.location.Location;
import android.net.Uri;
import com.hmdglobal.app.camera.app.MediaSaver.OnMediaSavedListener;
import com.hmdglobal.app.camera.exif.ExifInterface;
import java.lang.reflect.InvocationTargetException;

public interface CaptureSession {

    public interface ProgressListener {
        void onProgressChanged(int i);

        void onStatusMessageChanged(CharSequence charSequence);
    }

    void addProgressListener(ProgressListener progressListener);

    void cancel();

    void finish();

    void finishWithFailure(CharSequence charSequence);

    Uri getContentUri();

    Location getLocation();

    String getPath();

    int getProgress();

    CharSequence getProgressMessage();

    String getTitle();

    Uri getUri();

    boolean hasPath();

    void onPreviewAvailable();

    void removeProgressListener(ProgressListener progressListener);

    void saveAndFinish(byte[] bArr, int i, int i2, int i3, ExifInterface exifInterface, OnMediaSavedListener onMediaSavedListener) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException;

    void setLocation(Location location);

    void setProgress(int i);

    void setProgressMessage(CharSequence charSequence);

    void startEmpty();

    void startSession(Uri uri, CharSequence charSequence);

    void startSession(byte[] bArr, CharSequence charSequence);

    void updatePreview(String str);
}
