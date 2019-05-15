package com.hmdglobal.app.camera;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.location.Location;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore.Images.Media;
import com.hmdglobal.app.camera.app.CameraApp;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.exif.ExifInterface;
import com.hmdglobal.app.camera.provider.InfoTable;
import com.hmdglobal.app.camera.specialtype.ProcessingMediaManager;
import com.hmdglobal.app.camera.specialtype.ProcessingMediaManager.ProcessingMedia;
import com.hmdglobal.app.camera.util.ApiHelper;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Storage {
    public static String BUCKET_ID = String.valueOf(DIRECTORY.toLowerCase().hashCode());
    public static final String CAMERA_SESSION_SCHEME = "camera_session";
    public static String DCIM = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
    public static String DIRECTORY = null;
    private static final String GOOGLE_COM = "google.com";
    public static final String JPEG_POSTFIX = ".jpg";
    public static final long LOW_STORAGE_THRESHOLD_BYTES = 50000000;
    public static final String PHONE_STORAGE = "0";
    public static final long PREPARING = -2;
    public static final String RAW_DIRECTORY;
    public static final String SDCARD_STORAGE = "1";
    private static final Tag TAG = new Tag("Storage");
    public static final long UNAVAILABLE = -1;
    public static final long UNKNOWN_SIZE = -3;
    public static final long UPDATE_MASK = 4611686018427387904L;
    public static final long UPDATE_UNMASK = -4611686018427387905L;
    private static boolean isSDCardStorage;
    private static String phoneRootDirectory;
    private static HashMap<Uri, Uri> sContentUrisToSessions = new HashMap();
    private static HashMap<Uri, Uri> sSessionsToContentUris = new HashMap();
    private static HashMap<Uri, byte[]> sSessionsToPlaceholderBytes = new HashMap();
    private static HashMap<Uri, Integer> sSessionsToPlaceholderVersions = new HashMap();
    private static HashMap<Uri, Point> sSessionsToSizes = new HashMap();
    private static String sdcardDirectory;
    private static String sdcardRootDirectory;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(DCIM);
        stringBuilder.append("/Camera");
        DIRECTORY = stringBuilder.toString();
        stringBuilder = new StringBuilder();
        stringBuilder.append(DCIM);
        stringBuilder.append("/Camera/raw");
        RAW_DIRECTORY = stringBuilder.toString();
    }

    public static Uri addImage(ContentResolver resolver, String title, long date, Location location, int orientation, ExifInterface exif, byte[] jpeg, int width, int height) {
        return addImage(resolver, title, date, location, orientation, exif, jpeg, width, height, "image/jpeg");
    }

    public static Uri addPNGImage(ContentResolver resolver, String title, long date, Location location, int orientation, ExifInterface exif, int width, int height, Bitmap bitmap) {
        return null;
    }

    /* JADX WARNING: Removed duplicated region for block: B:11:0x0040  */
    /* JADX WARNING: Removed duplicated region for block: B:9:0x0027  */
    protected static android.net.Uri addImage(android.content.ContentResolver r18, java.lang.String r19, long r20, android.location.Location r22, int r23, com.hmdglobal.app.camera.exif.ExifInterface r24, byte[] r25, int r26, int r27, java.lang.String r28) {
        /*
        r12 = generateFilepath(r19);
        r13 = r24;
        r14 = r25;
        r15 = writeFile(r12, r14, r13);
        r11 = com.hmdglobal.app.camera.motion.MotionPictureHelper.getHelper();
        if (r11 == 0) goto L_0x0021;
    L_0x0012:
        r0 = r11.isMotionOn();
        if (r0 == 0) goto L_0x0021;
    L_0x0018:
        r11.onJPEGPictureFinished(r12);
    L_0x001b:
        r0 = r11.isComposeDone();
        if (r0 == 0) goto L_0x001b;
    L_0x0021:
        r0 = 0;
        r0 = (r15 > r0 ? 1 : (r15 == r0 ? 0 : -1));
        if (r0 < 0) goto L_0x0040;
    L_0x0027:
        r0 = r18;
        r1 = r19;
        r2 = r20;
        r4 = r22;
        r5 = r23;
        r6 = r15;
        r8 = r12;
        r9 = r26;
        r10 = r27;
        r17 = r11;
        r11 = r28;
        r0 = addImageToMediaStore(r0, r1, r2, r4, r5, r6, r8, r9, r10, r11);
        return r0;
    L_0x0040:
        r17 = r11;
        r0 = 0;
        return r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hmdglobal.app.camera.Storage.addImage(android.content.ContentResolver, java.lang.String, long, android.location.Location, int, com.hmdglobal.app.camera.exif.ExifInterface, byte[], int, int, java.lang.String):android.net.Uri");
    }

    public static Uri addFormatImage(ContentResolver resolver, String title, long date, Location location, int orientation, ExifInterface exif, byte[] jpeg, int width, int height, String mimeType) {
        String str = mimeType;
        String str2 = title;
        String path = generateFilepath(str2, str);
        int size = writeFile(path, jpeg, exif, str);
        File f = new File(path);
        if (f.exists() && f.isFile()) {
            size = (int) f.length();
        }
        return addFormatImage(resolver, str2, date, location, orientation, size, path, width, height, str);
    }

    public static String generateFilepath(String title, String pictureFormat) {
        StringBuilder stringBuilder;
        if (pictureFormat != null && !pictureFormat.equalsIgnoreCase("jpeg")) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(RAW_DIRECTORY);
            stringBuilder.append('/');
            stringBuilder.append(title);
            stringBuilder.append(".raw");
            return stringBuilder.toString();
        } else if (isSDCardStorage && SDCard.instance().isWriteable()) {
            try {
                String cameraDir = new StringBuilder();
                cameraDir.append(SDCard.instance().getDirectory());
                cameraDir.append("/DCIM/Camera");
                if (!new File(cameraDir.toString()).exists()) {
                    updateSavePath();
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(DIRECTORY);
                stringBuilder2.append('/');
                stringBuilder2.append(title);
                stringBuilder2.append(JPEG_POSTFIX);
                return stringBuilder2.toString();
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(DIRECTORY);
                stringBuilder3.append('/');
                stringBuilder3.append(title);
                stringBuilder3.append(JPEG_POSTFIX);
                return stringBuilder3.toString();
            }
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append(DIRECTORY);
            stringBuilder.append('/');
            stringBuilder.append(title);
            stringBuilder.append(JPEG_POSTFIX);
            return stringBuilder.toString();
        }
    }

    public static int writeFile(String path, byte[] jpeg, ExifInterface exif, String mimeType) {
        if (exif != null && (mimeType == null || mimeType.equalsIgnoreCase("jpeg"))) {
            try {
                return exif.writeExif(jpeg, path);
            } catch (Exception e) {
                Log.e(TAG, "Failed to write data", e);
            }
        } else if (jpeg != null) {
            if (!(mimeType.equalsIgnoreCase("jpeg") || mimeType == null)) {
                new File(RAW_DIRECTORY).mkdirs();
            }
            writeFormatFile(path, jpeg);
            return jpeg.length;
        }
        return 0;
    }

    private static Uri addFormatImage(ContentResolver resolver, String title, long date, Location location, int orientation, int jpegLength, String path, int width, int height, String mimeType) {
        ContentValues values = getContentValuesForData(title, date, location, orientation, jpegLength, path, width, height, mimeType);
        if (values.containsKey("datetaken")) {
            return insertImage(resolver, values);
        }
        return updateImage(resolver, values, date);
    }

    private static Uri insertImage(ContentResolver resolver, ContentValues values) {
        try {
            return resolver.insert(Media.EXTERNAL_CONTENT_URI, values);
        } catch (Throwable th) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to write MediaStore");
            stringBuilder.append(th);
            Log.e(tag, stringBuilder.toString());
            return null;
        }
    }

    private static Uri updateImage(ContentResolver resolver, ContentValues values, long date) {
        Uri uri = null;
        try {
            ProcessingMedia pm = ProcessingMediaManager.getInstance(CameraApp.getContext()).getByDate(date);
            if (pm == null || values == null) {
                Log.e(TAG, "Failed to update MediaStore: get processing media failed");
                return uri;
            }
            uri = ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, pm.getMediaStoreId());
            if (!values.containsKey("media_type")) {
                values.put("media_type", Integer.valueOf(1));
            }
            int r = resolver.update(uri, values, null, null);
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateImage uri - ");
            stringBuilder.append(uri);
            stringBuilder.append(", r num = ");
            stringBuilder.append(r);
            Log.d(tag, stringBuilder.toString());
            return uri;
        } catch (Throwable th) {
            Tag tag2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed to update MediaStore: ");
            stringBuilder2.append(th);
            Log.e(tag2, stringBuilder2.toString());
        }
    }

    private static Uri addImageToMediaStore(ContentResolver resolver, String title, long date, Location location, int orientation, long jpegLength, String path, int width, int height, String mimeType) {
        ContentValues values = getContentValuesForData(title, date, location, orientation, jpegLength, path, width, height, mimeType);
        if (values.containsKey("datetaken")) {
            return insertImage(resolver, values);
        }
        return updateImage(resolver, values, date);
    }

    private static boolean dateInProcessing(long date) {
        return ProcessingMediaManager.getInstance(CameraApp.getContext()).getByDateSynced(date) != null;
    }

    private static void setDate(ContentValues values, long date) {
        if (dateInProcessing(date)) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setDate - update - ");
            stringBuilder.append(date);
            Log.d(tag, stringBuilder.toString());
            return;
        }
        values.put("datetaken", Long.valueOf(date));
    }

    public static Uri addImagePNGToMediaStore(ContentResolver resolver, String title, long date, Location location, int orientation, long jpegLength, String path, int width, int height, String mimeType) {
        Throwable th;
        Tag tag;
        StringBuilder stringBuilder;
        Uri uri = null;
        try {
            try {
                return resolver.insert(Media.EXTERNAL_CONTENT_URI, getContentValuesForPNGData(title, date, location, orientation, jpegLength, path, width, height, mimeType));
            } catch (Throwable th2) {
                th = th2;
                tag = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to write MediaStore");
                stringBuilder.append(th);
                Log.e(tag, stringBuilder.toString());
                return uri;
            }
        } catch (Throwable th3) {
            th = th3;
            ContentResolver contentResolver = resolver;
            tag = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to write MediaStore");
            stringBuilder.append(th);
            Log.e(tag, stringBuilder.toString());
            return uri;
        }
    }

    public static ContentValues getContentValuesForData(String title, long date, Location location, int orientation, int jpegLength, String path, int width, int height, String mimeType) {
        ContentValues values = new ContentValues(9);
        values.put("title", title);
        StringBuilder stringBuilder;
        if (mimeType.equalsIgnoreCase("jpeg") || mimeType.equalsIgnoreCase("image/jpeg") || mimeType == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(title);
            stringBuilder.append(JPEG_POSTFIX);
            values.put("_display_name", stringBuilder.toString());
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append(title);
            stringBuilder.append(".raw");
            values.put("_display_name", stringBuilder.toString());
        }
        setDate(values, date);
        values.put(InfoTable.MIME_TYPE, "image/jpeg");
        values.put("orientation", Integer.valueOf(orientation));
        values.put("_data", path);
        values.put("_size", Integer.valueOf(jpegLength));
        setImageSize(values, width, height);
        if (location != null) {
            values.put(InfoTable.LATITUDE, Double.valueOf(location.getLatitude()));
            values.put(InfoTable.LONGITUDE, Double.valueOf(location.getLongitude()));
        }
        return values;
    }

    private static ContentValues getContentValuesForData(String title, long date, Location location, int orientation, long jpegLength, String path, int width, int height, String mimeType) {
        ContentValues values = new ContentValues(9);
        values.put("title", title);
        StringBuilder stringBuilder;
        if (mimeType.equalsIgnoreCase("jpeg") || mimeType.equalsIgnoreCase("image/jpeg") || mimeType == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(title);
            stringBuilder.append(JPEG_POSTFIX);
            values.put("_display_name", stringBuilder.toString());
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append(title);
            stringBuilder.append(".raw");
            values.put("_display_name", stringBuilder.toString());
        }
        setDate(values, date);
        values.put(InfoTable.MIME_TYPE, "image/jpeg");
        values.put("orientation", Integer.valueOf(orientation));
        values.put("_data", path);
        values.put("_size", Long.valueOf(jpegLength));
        setImageSize(values, width, height);
        if (location != null) {
            values.put(InfoTable.LATITUDE, Double.valueOf(location.getLatitude()));
            values.put(InfoTable.LONGITUDE, Double.valueOf(location.getLongitude()));
        }
        return values;
    }

    public static ContentValues getContentValuesForPNGData(String title, long date, Location location, int orientation, long jpegLength, String path, int width, int height, String mimeType) {
        String str = title;
        String str2 = path;
        long dateModifiedSeconds = TimeUnit.MILLISECONDS.toSeconds(new File(str2).lastModified());
        ContentValues values = new ContentValues(11);
        values.put("title", str);
        values.put("_display_name", str);
        values.put("datetaken", Long.valueOf(date));
        values.put(InfoTable.MIME_TYPE, mimeType);
        values.put(InfoTable.DATE_MODIFIED, Long.valueOf(dateModifiedSeconds));
        values.put("orientation", Integer.valueOf(orientation));
        values.put("_data", str2);
        values.put("_size", Long.valueOf(jpegLength));
        setImageSize(values, width, height);
        if (location != null) {
            values.put(InfoTable.LATITUDE, Double.valueOf(location.getLatitude()));
            values.put(InfoTable.LONGITUDE, Double.valueOf(location.getLongitude()));
        }
        return values;
    }

    public static Uri addPlaceholder(byte[] jpeg, int width, int height) {
        Builder builder = new Builder();
        builder.scheme(CAMERA_SESSION_SCHEME).authority(GOOGLE_COM).appendPath(UUID.randomUUID().toString());
        Uri uri = builder.build();
        replacePlaceholder(uri, jpeg, width, height);
        return uri;
    }

    public static void replacePlaceholder(Uri uri, byte[] jpeg, int width, int height) {
        sSessionsToSizes.put(uri, new Point(width, height));
        sSessionsToPlaceholderBytes.put(uri, jpeg);
        Integer currentVersion = (Integer) sSessionsToPlaceholderVersions.get(uri);
        sSessionsToPlaceholderVersions.put(uri, Integer.valueOf(currentVersion == null ? 0 : currentVersion.intValue() + 1));
    }

    public static Uri updateImage(Uri imageUri, ContentResolver resolver, String title, long date, Location location, int orientation, ExifInterface exif, byte[] jpeg, int width, int height, String mimeType) {
        byte[] bArr = jpeg;
        String path = generateFilepath(title);
        writeFile(path, bArr, exif);
        return updateImage(imageUri, resolver, title, date, location, orientation, bArr.length, path, width, height, mimeType);
    }

    public static Uri updatePNGImage(Uri imageUri, ContentResolver resolver, String title, long date, Location location, int orientation, ExifInterface exif, byte[] jpeg, int width, int height, String mimeType) {
        return updateImage(imageUri, resolver, title, date, location, orientation, jpeg.length, generatePNGFilepath(title), width, height, mimeType);
    }

    @TargetApi(16)
    private static void setImageSize(ContentValues values, int width, int height) {
        if (ApiHelper.HAS_MEDIA_COLUMNS_WIDTH_AND_HEIGHT) {
            values.put(InfoTable.WIDTH, Integer.valueOf(width));
            values.put(InfoTable.HEIGHT, Integer.valueOf(height));
        }
    }

    public static long writeFile(String path, byte[] jpeg, ExifInterface exif) {
        if (exif == null) {
            return writeFile(path, jpeg);
        }
        try {
            exif.writeExif(jpeg, path);
            return new File(path).length();
        } catch (Exception e) {
            Log.e(TAG, "Failed to write data", e);
            return -1;
        }
    }

    private static long writeFile(String path, byte[] data) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            out.write(data);
            long length = (long) data.length;
            try {
                out.close();
            } catch (Exception e) {
                Log.e(TAG, "Failed to close file after write", e);
            }
            return length;
        } catch (Exception e2) {
            Log.e(TAG, "Failed to write data", e2);
            try {
                out.close();
            } catch (Exception e22) {
                Log.e(TAG, "Failed to close file after write", e22);
            }
            return -1;
        } catch (Throwable th) {
            try {
                out.close();
            } catch (Exception e3) {
                Log.e(TAG, "Failed to close file after write", e3);
            }
            throw th;
        }
    }

    public static void writeFormatFile(String path, byte[] data) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            out.write(data);
            try {
                out.close();
            } catch (Exception e) {
                Log.e(TAG, "Failed to close file after write", e);
            }
        } catch (Exception e2) {
            Log.e(TAG, "Failed to write data", e2);
            out.close();
        } catch (Throwable th) {
            try {
                out.close();
            } catch (Exception e3) {
                Log.e(TAG, "Failed to close file after write", e3);
            }
            throw th;
        }
    }

    private static Uri updateImage(Uri imageUri, ContentResolver resolver, String title, long date, Location location, int orientation, int jpegLength, String path, int width, int height, String mimeType) {
        Uri uri = imageUri;
        ContentValues values = getContentValuesForData(title, date, location, orientation, jpegLength, path, width, height, mimeType);
        Uri resultUri = uri;
        if (isSessionUri(imageUri)) {
            resultUri = addImageToMediaStore(resolver, title, date, location, orientation, (long) jpegLength, path, width, height, mimeType);
            sSessionsToContentUris.put(uri, resultUri);
            sContentUrisToSessions.put(resultUri, uri);
            ContentResolver contentResolver = resolver;
            return resultUri;
        }
        int i = jpegLength;
        resolver.update(uri, values, null, null);
        return resultUri;
    }

    public static void updateFormatImage(Uri imageUri, ContentResolver resolver, String title, long date, Location location, int orientation, ExifInterface exif, byte[] jpeg, int width, int height, String mimeType) {
        byte[] bArr = jpeg;
        String str = mimeType;
        String str2 = title;
        String path = generateFilepath(str2, str);
        writeFile(path, bArr, exif, str);
        updateFormatImage(imageUri, resolver, str2, date, location, orientation, bArr.length, path, width, height, str);
    }

    public static void updateFormatImage(Uri imageUri, ContentResolver resolver, String title, long date, Location location, int orientation, int jpegLength, String path, int width, int height, String mimeType) {
        Uri uri = imageUri;
        ContentResolver contentResolver = resolver;
        ContentValues values = getContentValuesForData(title, date, location, orientation, jpegLength, path, width, height, mimeType);
        int rowsModified = contentResolver.update(uri, values, null, null);
        StringBuilder stringBuilder;
        if (rowsModified == 0) {
            Tag tag = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateImage called with no prior image at uri: ");
            stringBuilder.append(uri);
            Log.w(tag, stringBuilder.toString());
            insertImage(contentResolver, values);
        } else if (rowsModified != 1) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Bad number of rows (");
            stringBuilder.append(rowsModified);
            stringBuilder.append(") updated for uri: ");
            stringBuilder.append(uri);
            throw new IllegalStateException(stringBuilder.toString());
        }
    }

    public static String generateFilepath(String title) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(DIRECTORY);
        stringBuilder.append('/');
        stringBuilder.append(title);
        stringBuilder.append(JPEG_POSTFIX);
        return stringBuilder.toString();
    }

    public static String generatePNGFilepath(String title) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(DIRECTORY);
        stringBuilder.append('/');
        stringBuilder.append(title);
        stringBuilder.append(".png");
        return stringBuilder.toString();
    }

    public static byte[] getJpegForSession(Uri uri) {
        return (byte[]) sSessionsToPlaceholderBytes.get(uri);
    }

    public static int getJpegVersionForSession(Uri uri) {
        return ((Integer) sSessionsToPlaceholderVersions.get(uri)).intValue();
    }

    public static Point getSizeForSession(Uri uri) {
        return (Point) sSessionsToSizes.get(uri);
    }

    public static Uri getContentUriForSessionUri(Uri uri) {
        return (Uri) sSessionsToContentUris.get(uri);
    }

    public static Uri getSessionUriFromContentUri(Uri contentUri) {
        return (Uri) sContentUrisToSessions.get(contentUri);
    }

    public static boolean isSessionUri(Uri uri) {
        return uri.getScheme().equals(CAMERA_SESSION_SCHEME);
    }

    public static long getAvailableSpace() {
        String state = Environment.getExternalStorageState();
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("External storage state=");
        stringBuilder.append(state);
        Log.d(tag, stringBuilder.toString());
        if ("checking".equals(state)) {
            return -2;
        }
        if (!"mounted".equals(state)) {
            return -1;
        }
        File dir = new File(DIRECTORY);
        dir.mkdirs();
        if (!dir.isDirectory() || !dir.canWrite()) {
            return -1;
        }
        try {
            StatFs stat = new StatFs(DIRECTORY);
            return ((long) stat.getAvailableBlocks()) * ((long) stat.getBlockSize());
        } catch (Exception e) {
            Log.i(TAG, "Fail to access external storage", e);
            return -3;
        }
    }

    public static void ensureOSXCompatible() {
        File nnnAAAAA = new File(DCIM, "100ANDRO");
        if (!nnnAAAAA.exists() && !nnnAAAAA.mkdirs()) {
            Tag tag = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to create ");
            stringBuilder.append(nnnAAAAA.getPath());
            Log.e(tag, stringBuilder.toString());
        }
    }

    private static void setupRootDirectory() {
        phoneRootDirectory = "/storage/sdcard0";
        sdcardRootDirectory = "/storage/sdcard1";
    }

    private static void setupSDCardDirectory() {
        if (sdcardRootDirectory == null) {
            setupRootDirectory();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(sdcardRootDirectory);
        stringBuilder.append("/DCIM/Camera");
        sdcardDirectory = stringBuilder.toString();
    }

    public static void setSavePath(String path) {
        isSDCardStorage = false;
        if (path != null && path.equals("1") && isSDCardAvailable()) {
            isSDCardStorage = true;
        }
        updateSavePath();
    }

    public static boolean isSDCardAvailable() {
        return SDCard.instance().isWriteable();
    }

    private static boolean isSDCardMounted() {
        if (sdcardRootDirectory == null) {
            setupRootDirectory();
        }
        try {
            String state;
            if (ApiHelper.isLOrHigher()) {
                state = Environment.getExternalStorageState(new File(sdcardRootDirectory));
            } else {
                state = Environment.getStorageState(new File(sdcardRootDirectory));
            }
            if (state != null && state.equals("mounted")) {
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Fail to get sdcard state", e);
        }
        return false;
    }

    private static boolean isSDDirectoryWriteable() {
        if (sdcardDirectory == null) {
            setupSDCardDirectory();
        }
        File dir = new File(sdcardDirectory);
        dir.mkdirs();
        return dir.isDirectory() && dir.canWrite();
    }

    private static void updateSavePath() {
        if (isSDCardStorage) {
            StringBuilder stringBuilder;
            try {
                DCIM = SDCard.instance().getDirectory();
                stringBuilder = new StringBuilder();
                stringBuilder.append(DCIM);
                stringBuilder.append("/DCIM/Camera");
                File dir = new File(stringBuilder.toString());
                boolean suc = dir.exists();
                if (!dir.exists()) {
                    suc = dir.mkdirs();
                }
                Tag tag = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("updateSavePath: ");
                stringBuilder2.append(suc);
                stringBuilder2.append(", dir = ");
                stringBuilder2.append(dir.getAbsolutePath());
                Log.d(tag, stringBuilder2.toString());
                DIRECTORY = suc ? dir.getAbsolutePath() : DCIM;
                BUCKET_ID = String.valueOf(DIRECTORY.toLowerCase().hashCode());
            } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
                e.printStackTrace();
                stringBuilder = new StringBuilder();
                stringBuilder.append(DCIM);
                stringBuilder.append("/Camera");
                DIRECTORY = stringBuilder.toString();
                return;
            }
        }
        DCIM = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append(DCIM);
        stringBuilder3.append("/Camera");
        DIRECTORY = stringBuilder3.toString();
        BUCKET_ID = String.valueOf(DIRECTORY.toLowerCase().hashCode());
    }

    public static String getSavePath() {
        if (isSDCardStorage) {
            return "1";
        }
        return "0";
    }
}
