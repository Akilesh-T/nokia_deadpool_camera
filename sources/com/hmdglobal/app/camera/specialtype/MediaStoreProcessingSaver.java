package com.hmdglobal.app.camera.specialtype;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import com.hmdglobal.app.camera.provider.InfoTable;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MediaStoreProcessingSaver {
    private static final String TAG = "ImageSaver";
    private final Context context;

    public MediaStoreProcessingSaver(Context context) {
        this.context = context;
    }

    /* Access modifiers changed, original: 0000 */
    @Nullable
    public Uri saveAsHidden(long dateTaken) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(InfoTable.DATE_MODIFIED, Long.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())));
        contentValues.put("datetaken", Long.valueOf(dateTaken));
        contentValues.putNull(InfoTable.MIME_TYPE);
        contentValues.put("media_type", Integer.valueOf(0));
        contentValues.put("_data", new File(getTargetFolder(), UUID.randomUUID().toString()).toString());
        Uri result = this.context.getContentResolver().insert(HmdThumbnailProvider.PLACE_HOLDER_URI, contentValues);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Saved Uri as hidden: ");
        stringBuilder.append(result);
        Log.d(str, stringBuilder.toString());
        return result;
    }

    /* Access modifiers changed, original: 0000 */
    public void updateToVisible(long mediaStoreId, String filename, Bitmap bitmap) {
        File toSave = findAvailableName(filename);
        Uri mediaStoreUri = ContentUris.withAppendedId(HmdThumbnailProvider.PLACE_HOLDER_URI, mediaStoreId);
        if (toSave == null) {
            this.context.getContentResolver().delete(mediaStoreUri, null, null);
            return;
        }
        OutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(toSave));
            bitmap.compress(CompressFormat.JPEG, 100, outputStream);
            outputStream.close();
            try {
                outputStream.close();
            } catch (IOException e) {
            }
            ContentValues values = new ContentValues();
            values.put("_data", toSave.getAbsolutePath());
            values.put("media_type", Integer.valueOf(1));
            values.put(InfoTable.MIME_TYPE, "image/jpeg");
            values.put(InfoTable.DATE_MODIFIED, Long.valueOf(TimeUnit.MILLISECONDS.toSeconds(toSave.lastModified())));
            this.context.getContentResolver().update(mediaStoreUri, values, null, null);
            if (Log.isLoggable(TAG, 3)) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Made uri visible: ");
                stringBuilder.append(mediaStoreUri);
                Log.d(str, stringBuilder.toString());
            }
        } catch (IOException e2) {
            if (Log.isLoggable(TAG, 3)) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failed to write to: ");
                stringBuilder2.append(toSave);
                Log.d(str2, stringBuilder2.toString(), e2);
            }
            this.context.getContentResolver().delete(mediaStoreUri, null, null);
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e3) {
                }
            }
        } catch (Throwable th) {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e4) {
                }
            }
        }
    }

    public void deleteUselessRecord(long mediaStoreId) {
        this.context.getContentResolver().delete(ContentUris.withAppendedId(HmdThumbnailProvider.PLACE_HOLDER_URI, mediaStoreId), null, null);
    }

    private static File getTargetFolder() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
    }

    @Nullable
    private File findAvailableName(String filename) {
        File folder = getTargetFolder();
        String resultName;
        if (folder.mkdirs() || (folder.exists() && folder.isDirectory())) {
            int count = 1;
            File file = new File(folder, filename);
            resultName = filename;
            while (file.exists()) {
                file = new File(folder, resultName);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(filename);
                stringBuilder.append("_");
                int count2 = count + 1;
                stringBuilder.append(count);
                resultName = stringBuilder.toString();
                count = count2;
            }
            return file;
        }
        if (Log.isLoggable(TAG, 3)) {
            resultName = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Cannot create directory, exists: ");
            stringBuilder2.append(folder.exists());
            stringBuilder2.append(", is directory: ");
            stringBuilder2.append(folder.isDirectory());
            Log.d(resultName, stringBuilder2.toString());
        }
        return null;
    }
}
