package com.hmdglobal.app.camera.session;

import android.content.Context;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.util.FileUtil;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

public class SessionStorageManagerImpl implements SessionStorageManager {
    private static final int MAX_SESSION_AGE_MILLIS = 86400000;
    private static final Tag TAG = new Tag("SesnStorageMgrImpl");
    private final File mBaseDirectory;
    private final File mDeprecatedBaseDirectory;

    public static SessionStorageManager create(Context context) {
        return new SessionStorageManagerImpl(context.getExternalCacheDir(), context.getExternalFilesDir(null));
    }

    SessionStorageManagerImpl(File baseDirectory, File deprecatedBaseDirectory) {
        this.mBaseDirectory = baseDirectory;
        this.mDeprecatedBaseDirectory = deprecatedBaseDirectory;
    }

    public File getSessionDirectory(String subDirectory) throws IOException {
        File sessionDirectory = new File(this.mBaseDirectory, subDirectory);
        StringBuilder stringBuilder;
        if (!sessionDirectory.exists() && !sessionDirectory.mkdirs()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Could not create session directory: ");
            stringBuilder.append(sessionDirectory);
            throw new IOException(stringBuilder.toString());
        } else if (sessionDirectory.isDirectory()) {
            cleanUpExpiredSessions(sessionDirectory);
            cleanUpExpiredSessions(new File(this.mDeprecatedBaseDirectory, subDirectory));
            return sessionDirectory;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Session directory is not a directory: ");
            stringBuilder.append(sessionDirectory);
            throw new IOException(stringBuilder.toString());
        }
    }

    private void cleanUpExpiredSessions(File baseDirectory) {
        File[] sessionDirs = baseDirectory.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        if (sessionDirs != null) {
            long nowInMillis = System.currentTimeMillis();
            for (File sessionDir : sessionDirs) {
                if (sessionDir.lastModified() < nowInMillis - 86400000 && !FileUtil.deleteDirectoryRecursively(sessionDir)) {
                    Tag tag = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Could not clean up ");
                    stringBuilder.append(sessionDir.getAbsolutePath());
                    Log.w(tag, stringBuilder.toString());
                }
            }
        }
    }
}
