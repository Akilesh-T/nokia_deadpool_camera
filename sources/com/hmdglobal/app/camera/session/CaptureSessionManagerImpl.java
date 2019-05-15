package com.hmdglobal.app.camera.session;

import android.content.ContentResolver;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import com.hmdglobal.app.camera.Storage;
import com.hmdglobal.app.camera.app.MediaSaver;
import com.hmdglobal.app.camera.app.MediaSaver.OnMediaSavedListener;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.exif.ExifInterface;
import com.hmdglobal.app.camera.session.CaptureSession.ProgressListener;
import com.hmdglobal.app.camera.session.CaptureSessionManager.SessionListener;
import com.hmdglobal.app.camera.session.PlaceholderManager.Session;
import com.hmdglobal.app.camera.util.FileUtil;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

public class CaptureSessionManagerImpl implements CaptureSessionManager {
    private static final Tag TAG = new Tag("CaptureSessMgrImpl");
    public static final String TEMP_SESSIONS = "TEMP_SESSIONS";
    private final ContentResolver mContentResolver;
    private final HashMap<Uri, CharSequence> mFailedSessionMessages = new HashMap();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final MediaSaver mMediaSaver;
    private final PlaceholderManager mPlaceholderManager;
    private final SessionStorageManager mSessionStorageManager;
    private final Map<String, CaptureSession> mSessions = new HashMap();
    private final LinkedList<SessionListener> mTaskListeners = new LinkedList();

    private class CaptureSessionImpl implements CaptureSession {
        private Uri mContentUri;
        private Location mLocation;
        private boolean mNoPlaceHolderRequired;
        private Session mPlaceHolderSession;
        private final HashSet<ProgressListener> mProgressListeners;
        private CharSequence mProgressMessage;
        private int mProgressPercent;
        private final long mSessionStartMillis;
        private final String mTitle;
        private Uri mUri;

        /* synthetic */ CaptureSessionImpl(CaptureSessionManagerImpl x0, String x1, long x2, Location x3, AnonymousClass1 x4) {
            this(x1, x2, x3);
        }

        private CaptureSessionImpl(String title, long sessionStartMillis, Location location) {
            this.mProgressPercent = 0;
            this.mNoPlaceHolderRequired = false;
            this.mProgressListeners = new HashSet();
            this.mTitle = title;
            this.mSessionStartMillis = sessionStartMillis;
            this.mLocation = location;
        }

        public String getTitle() {
            return this.mTitle;
        }

        public Location getLocation() {
            return this.mLocation;
        }

        public void setLocation(Location location) {
            this.mLocation = location;
        }

        public synchronized void setProgress(int percent) {
            this.mProgressPercent = percent;
            CaptureSessionManagerImpl.this.notifyTaskProgress(this.mUri, this.mProgressPercent);
            Iterator it = this.mProgressListeners.iterator();
            while (it.hasNext()) {
                ((ProgressListener) it.next()).onProgressChanged(percent);
            }
        }

        public synchronized int getProgress() {
            return this.mProgressPercent;
        }

        public synchronized CharSequence getProgressMessage() {
            return this.mProgressMessage;
        }

        public synchronized void setProgressMessage(CharSequence message) {
            this.mProgressMessage = message;
            CaptureSessionManagerImpl.this.notifyTaskProgressText(this.mUri, message);
            Iterator it = this.mProgressListeners.iterator();
            while (it.hasNext()) {
                ((ProgressListener) it.next()).onStatusMessageChanged(message);
            }
        }

        public void startEmpty() {
            this.mNoPlaceHolderRequired = true;
        }

        public synchronized void startSession(byte[] placeholder, CharSequence progressMessage) {
            this.mProgressMessage = progressMessage;
            this.mPlaceHolderSession = CaptureSessionManagerImpl.this.mPlaceholderManager.insertPlaceholder(this.mTitle, placeholder, this.mSessionStartMillis);
            this.mUri = this.mPlaceHolderSession.outputUri;
            CaptureSessionManagerImpl.this.putSession(this.mUri, this);
            CaptureSessionManagerImpl.this.notifyTaskQueued(this.mUri);
        }

        public synchronized void startSession(Uri uri, CharSequence progressMessage) {
            this.mUri = uri;
            this.mProgressMessage = progressMessage;
            this.mPlaceHolderSession = CaptureSessionManagerImpl.this.mPlaceholderManager.convertToPlaceholder(uri);
            CaptureSessionManagerImpl.this.mSessions.put(this.mUri.toString(), this);
            CaptureSessionManagerImpl.this.notifyTaskQueued(this.mUri);
        }

        public synchronized void cancel() {
            if (this.mUri != null) {
                CaptureSessionManagerImpl.this.removeSession(this.mUri.toString());
            }
        }

        public synchronized void saveAndFinish(byte[] data, int width, int height, int orientation, ExifInterface exif, OnMediaSavedListener listener) {
            synchronized (this) {
                if (this.mNoPlaceHolderRequired) {
                    CaptureSessionManagerImpl.this.mMediaSaver.addImage(data, this.mTitle, this.mSessionStartMillis, null, width, height, orientation, exif, listener, CaptureSessionManagerImpl.this.mContentResolver);
                } else if (this.mPlaceHolderSession != null) {
                    CaptureSessionManagerImpl.this.mPlaceholderManager.finishPlaceholder(this.mPlaceHolderSession, this.mLocation, orientation, exif, data, width, height, "image/jpeg");
                    CaptureSessionManagerImpl.this.removeSession(this.mUri.toString());
                    CaptureSessionManagerImpl.this.notifyTaskDone(this.mPlaceHolderSession.outputUri);
                } else {
                    throw new IllegalStateException("Cannot call saveAndFinish without calling startSession first.");
                }
            }
        }

        public void finish() {
            if (this.mPlaceHolderSession != null) {
                final String path = getPath();
                AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
                    public void run() {
                        try {
                            ExifInterface exif;
                            byte[] jpegData = FileUtil.readFileToByteArray(new File(path));
                            Options options = new Options();
                            options.inJustDecodeBounds = true;
                            BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length, options);
                            int width = options.outWidth;
                            int height = options.outHeight;
                            try {
                                exif = new ExifInterface();
                                exif.readExif(jpegData);
                            } catch (IOException e) {
                                Log.w(CaptureSessionManagerImpl.TAG, "Could not read exif", e);
                                exif = null;
                            }
                            CaptureSessionImpl.this.saveAndFinish(jpegData, width, height, 0, exif, null);
                        } catch (IOException e2) {
                        }
                    }
                });
                return;
            }
            throw new IllegalStateException("Cannot call finish without calling startSession first.");
        }

        public String getPath() {
            if (this.mUri != null) {
                try {
                    File tempDirectory = new File(CaptureSessionManagerImpl.this.getSessionDirectory(CaptureSessionManagerImpl.TEMP_SESSIONS), this.mTitle);
                    tempDirectory.mkdirs();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(this.mTitle);
                    stringBuilder.append(Storage.JPEG_POSTFIX);
                    File tempFile = new File(tempDirectory, stringBuilder.toString());
                    try {
                        if (!tempFile.exists()) {
                            tempFile.createNewFile();
                        }
                        return tempFile.getPath();
                    } catch (IOException e) {
                        Log.e(CaptureSessionManagerImpl.TAG, "Could not create temp session file", e);
                        throw new RuntimeException("Could not create temp session file", e);
                    }
                } catch (IOException e2) {
                    Log.e(CaptureSessionManagerImpl.TAG, "Could not get temp session directory", e2);
                    throw new RuntimeException("Could not get temp session directory", e2);
                }
            }
            throw new IllegalStateException("Cannot retrieve URI of not started session.");
        }

        public Uri getUri() {
            return this.mUri;
        }

        public Uri getContentUri() {
            return this.mContentUri;
        }

        public boolean hasPath() {
            return this.mUri != null;
        }

        public void onPreviewAvailable() {
            CaptureSessionManagerImpl.this.notifySessionPreviewAvailable(this.mPlaceHolderSession.outputUri);
        }

        public void updatePreview(String previewPath) {
            final String path = getPath();
            AsyncTask.SERIAL_EXECUTOR.execute(new Runnable() {
                public void run() {
                    try {
                        byte[] jpegData = FileUtil.readFileToByteArray(new File(path));
                        Options options = new Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length, options);
                        CaptureSessionManagerImpl.this.mPlaceholderManager.replacePlaceholder(CaptureSessionImpl.this.mPlaceHolderSession, jpegData, options.outWidth, options.outHeight);
                        CaptureSessionImpl.this.onPreviewAvailable();
                    } catch (IOException e) {
                    }
                }
            });
        }

        public void finishWithFailure(CharSequence reason) {
            if (this.mPlaceHolderSession != null) {
                this.mProgressMessage = reason;
                CaptureSessionManagerImpl.this.removeSession(this.mUri.toString());
                CaptureSessionManagerImpl.this.mFailedSessionMessages.put(this.mPlaceHolderSession.outputUri, reason);
                CaptureSessionManagerImpl.this.notifyTaskFailed(this.mPlaceHolderSession.outputUri, reason);
                return;
            }
            throw new IllegalStateException("Cannot call finish without calling startSession first.");
        }

        public void addProgressListener(ProgressListener listener) {
            listener.onStatusMessageChanged(this.mProgressMessage);
            listener.onProgressChanged(this.mProgressPercent);
            this.mProgressListeners.add(listener);
        }

        public void removeProgressListener(ProgressListener listener) {
            this.mProgressListeners.remove(listener);
        }
    }

    public CaptureSessionManagerImpl(MediaSaver mediaSaver, ContentResolver contentResolver, PlaceholderManager placeholderManager, SessionStorageManager sessionStorageManager) {
        this.mMediaSaver = mediaSaver;
        this.mContentResolver = contentResolver;
        this.mPlaceholderManager = placeholderManager;
        this.mSessionStorageManager = sessionStorageManager;
    }

    public CaptureSession createNewSession(String title, long sessionStartTime, Location location) {
        return new CaptureSessionImpl(this, title, sessionStartTime, location, null);
    }

    public CaptureSession createSession() {
        return new CaptureSessionImpl(this, null, System.currentTimeMillis(), null, null);
    }

    public void putSession(Uri sessionUri, CaptureSession session) {
        synchronized (this.mSessions) {
            this.mSessions.put(sessionUri.toString(), session);
        }
    }

    public CaptureSession getSession(Uri sessionUri) {
        CaptureSession captureSession;
        synchronized (this.mSessions) {
            captureSession = (CaptureSession) this.mSessions.get(sessionUri.toString());
        }
        return captureSession;
    }

    public void saveImage(byte[] data, String title, long date, Location loc, int width, int height, int orientation, ExifInterface exif, OnMediaSavedListener listener) {
        this.mMediaSaver.addImage(data, title, date, loc, width, height, orientation, exif, listener, this.mContentResolver);
    }

    public void addSessionListener(SessionListener listener) {
        synchronized (this.mTaskListeners) {
            this.mTaskListeners.add(listener);
        }
    }

    public void removeSessionListener(SessionListener listener) {
        synchronized (this.mTaskListeners) {
            this.mTaskListeners.remove(listener);
        }
    }

    public File getSessionDirectory(String subDirectory) throws IOException {
        return this.mSessionStorageManager.getSessionDirectory(subDirectory);
    }

    private void removeSession(String sessionUri) {
        synchronized (this.mSessions) {
            this.mSessions.remove(sessionUri);
        }
    }

    private void notifyTaskQueued(final Uri uri) {
        this.mMainHandler.post(new Runnable() {
            public void run() {
                synchronized (CaptureSessionManagerImpl.this.mTaskListeners) {
                    Iterator it = CaptureSessionManagerImpl.this.mTaskListeners.iterator();
                    while (it.hasNext()) {
                        ((SessionListener) it.next()).onSessionQueued(uri);
                    }
                }
            }
        });
    }

    private void notifyTaskDone(final Uri uri) {
        this.mMainHandler.post(new Runnable() {
            public void run() {
                synchronized (CaptureSessionManagerImpl.this.mTaskListeners) {
                    Iterator it = CaptureSessionManagerImpl.this.mTaskListeners.iterator();
                    while (it.hasNext()) {
                        ((SessionListener) it.next()).onSessionDone(uri);
                    }
                }
            }
        });
    }

    private void notifyTaskFailed(final Uri uri, final CharSequence reason) {
        this.mMainHandler.post(new Runnable() {
            public void run() {
                synchronized (CaptureSessionManagerImpl.this.mTaskListeners) {
                    Iterator it = CaptureSessionManagerImpl.this.mTaskListeners.iterator();
                    while (it.hasNext()) {
                        ((SessionListener) it.next()).onSessionFailed(uri, reason);
                    }
                }
            }
        });
    }

    private void notifyTaskProgress(final Uri uri, final int progressPercent) {
        this.mMainHandler.post(new Runnable() {
            public void run() {
                synchronized (CaptureSessionManagerImpl.this.mTaskListeners) {
                    Iterator it = CaptureSessionManagerImpl.this.mTaskListeners.iterator();
                    while (it.hasNext()) {
                        ((SessionListener) it.next()).onSessionProgress(uri, progressPercent);
                    }
                }
            }
        });
    }

    private void notifyTaskProgressText(final Uri uri, final CharSequence message) {
        this.mMainHandler.post(new Runnable() {
            public void run() {
                synchronized (CaptureSessionManagerImpl.this.mTaskListeners) {
                    Iterator it = CaptureSessionManagerImpl.this.mTaskListeners.iterator();
                    while (it.hasNext()) {
                        ((SessionListener) it.next()).onSessionProgressText(uri, message);
                    }
                }
            }
        });
    }

    private void notifySessionPreviewAvailable(final Uri uri) {
        this.mMainHandler.post(new Runnable() {
            public void run() {
                synchronized (CaptureSessionManagerImpl.this.mTaskListeners) {
                    Iterator it = CaptureSessionManagerImpl.this.mTaskListeners.iterator();
                    while (it.hasNext()) {
                        ((SessionListener) it.next()).onSessionPreviewAvailable(uri);
                    }
                }
            }
        });
    }

    public boolean hasErrorMessage(Uri uri) {
        return this.mFailedSessionMessages.containsKey(uri);
    }

    public CharSequence getErrorMesage(Uri uri) {
        return (CharSequence) this.mFailedSessionMessages.get(uri);
    }

    public void removeErrorMessage(Uri uri) {
        this.mFailedSessionMessages.remove(uri);
    }

    public void fillTemporarySession(final SessionListener listener) {
        this.mMainHandler.post(new Runnable() {
            public void run() {
                synchronized (CaptureSessionManagerImpl.this.mSessions) {
                    for (String sessionUri : CaptureSessionManagerImpl.this.mSessions.keySet()) {
                        CaptureSession session = (CaptureSession) CaptureSessionManagerImpl.this.mSessions.get(sessionUri);
                        listener.onSessionQueued(session.getUri());
                        listener.onSessionProgress(session.getUri(), session.getProgress());
                        listener.onSessionProgressText(session.getUri(), session.getProgressMessage());
                    }
                }
            }
        });
    }
}
