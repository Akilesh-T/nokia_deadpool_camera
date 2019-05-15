package com.hmdglobal.app.camera.session;

import android.location.Location;
import android.net.Uri;
import com.hmdglobal.app.camera.app.MediaSaver.OnMediaSavedListener;
import com.hmdglobal.app.camera.exif.ExifInterface;
import java.io.File;
import java.io.IOException;

public interface CaptureSessionManager {

    public interface SessionListener {
        void onSessionDone(Uri uri);

        void onSessionFailed(Uri uri, CharSequence charSequence);

        void onSessionPreviewAvailable(Uri uri);

        void onSessionProgress(Uri uri, int i);

        void onSessionProgressText(Uri uri, CharSequence charSequence);

        void onSessionQueued(Uri uri);

        void onSessionUpdated(Uri uri);
    }

    void addSessionListener(SessionListener sessionListener);

    CaptureSession createNewSession(String str, long j, Location location);

    CaptureSession createSession();

    void fillTemporarySession(SessionListener sessionListener);

    CharSequence getErrorMesage(Uri uri);

    CaptureSession getSession(Uri uri);

    File getSessionDirectory(String str) throws IOException;

    boolean hasErrorMessage(Uri uri);

    void putSession(Uri uri, CaptureSession captureSession);

    void removeErrorMessage(Uri uri);

    void removeSessionListener(SessionListener sessionListener);

    void saveImage(byte[] bArr, String str, long j, Location location, int i, int i2, int i3, ExifInterface exifInterface, OnMediaSavedListener onMediaSavedListener);
}
