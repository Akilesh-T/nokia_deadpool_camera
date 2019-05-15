package com.hmdglobal.app.camera;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.hmdglobal.app.camera.app.MediaSaver;
import com.hmdglobal.app.camera.app.MediaSaver.OnMediaSavedListener;
import com.hmdglobal.app.camera.app.MediaSaver.QueueListener;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.exif.ExifInterface;
import com.hmdglobal.app.camera.gdepthfilter.GDepth;
import com.hmdglobal.app.camera.gdepthfilter.GImage;
import com.hmdglobal.app.camera.specialtype.utils.ProviderUtils;
import com.hmdglobal.app.camera.util.XmpUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

public class MediaSaverImpl implements MediaSaver {
    private static final int SAVE_TASK_MEMORY_LIMIT = 62914560;
    private static final Tag TAG = new Tag("MediaSaverImpl");
    private static final String VIDEO_BASE_URI = "content://media/external/video/media";
    private long mMemoryUse = 0;
    private QueueListener mQueueListener;

    private class ImageSaveTask extends AsyncTask<Void, Void, Uri> {
        private final byte[] data;
        private final long date;
        private final ExifInterface exif;
        private int height;
        private final OnMediaSavedListener listener;
        private final Location loc;
        private final int orientation;
        private final ContentResolver resolver;
        private final String title;
        private int width;

        public ImageSaveTask(byte[] data, String title, long date, Location loc, int width, int height, int orientation, ExifInterface exif, ContentResolver resolver, OnMediaSavedListener listener) {
            this.data = data;
            this.title = title;
            this.date = date;
            this.loc = loc;
            this.width = width;
            this.height = height;
            this.orientation = orientation;
            this.exif = exif;
            this.resolver = resolver;
            this.listener = listener;
        }

        /* Access modifiers changed, original: protected */
        public void onPreExecute() {
        }

        /* Access modifiers changed, original: protected|varargs */
        public Uri doInBackground(Void... v) {
            if (this.width == 0 || this.height == 0) {
                Options options = new Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(this.data, 0, this.data.length, options);
                this.width = options.outWidth;
                this.height = options.outHeight;
            }
            return Storage.addImage(this.resolver, this.title, this.date, this.loc, this.orientation, this.exif, this.data, this.width, this.height);
        }

        /* Access modifiers changed, original: protected */
        public void onPostExecute(Uri uri) {
            if (!(this.listener == null || uri == null)) {
                this.listener.onMediaSaved(uri);
            }
            boolean previouslyFull = MediaSaverImpl.this.isQueueFull();
            MediaSaverImpl.access$022(MediaSaverImpl.this, (long) this.data.length);
            if (MediaSaverImpl.this.isQueueFull() != previouslyFull) {
                MediaSaverImpl.this.onQueueAvailable();
            }
        }
    }

    private class VideoSaveTask extends AsyncTask<Void, Void, Uri> {
        private final OnMediaSavedListener listener;
        private String path;
        private final ContentResolver resolver;
        private final ContentValues values;

        public VideoSaveTask(String path, ContentValues values, OnMediaSavedListener l, ContentResolver r) {
            this.path = path;
            this.values = new ContentValues(values);
            this.listener = l;
            this.resolver = r;
        }

        /* Access modifiers changed, original: protected|varargs */
        public Uri doInBackground(Void... v) {
            Tag access$200;
            StringBuilder stringBuilder;
            Uri uri = null;
            try {
                uri = this.resolver.insert(Uri.parse(MediaSaverImpl.VIDEO_BASE_URI), this.values);
                String finalName = this.values.getAsString("_data");
                if (new File(this.path).renameTo(new File(finalName))) {
                    this.path = finalName;
                }
                this.resolver.update(uri, this.values, null, null);
                access$200 = MediaSaverImpl.TAG;
                stringBuilder = new StringBuilder();
            } catch (Exception e) {
                Log.e(MediaSaverImpl.TAG, "failed to add video to media store", e);
                uri = null;
                access$200 = MediaSaverImpl.TAG;
                stringBuilder = new StringBuilder();
            } catch (Throwable th) {
                Tag access$2002 = MediaSaverImpl.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Current video URI: ");
                stringBuilder2.append(uri);
                Log.v(access$2002, stringBuilder2.toString());
            }
            stringBuilder.append("Current video URI: ");
            stringBuilder.append(uri);
            Log.v(access$200, stringBuilder.toString());
            return uri;
        }

        /* Access modifiers changed, original: protected */
        public void onPostExecute(Uri uri) {
            if (this.listener != null) {
                this.listener.onMediaSaved(uri);
            }
        }
    }

    private class XmpImageSaveTask extends AsyncTask<Void, Void, Uri> {
        private GImage bayer;
        private byte[] data;
        private long date;
        private ExifInterface exif;
        private GDepth gDepth;
        private int height;
        private String imageType;
        private OnMediaSavedListener listener;
        private Location loc;
        private byte[] mainImage;
        private int orientation;
        private String pictureFormat;
        private ContentResolver resolver;
        final /* synthetic */ MediaSaverImpl this$0;
        private String title;
        private int width;

        public XmpImageSaveTask(MediaSaverImpl mediaSaverImpl, byte[] mainImage, GImage bayer, GDepth gDepth, String title, long date, Location loc, int width, int height, int orientation, ExifInterface exif, String imageType, ContentResolver resolver, OnMediaSavedListener listener, String pictureFormat) {
            this.this$0 = mediaSaverImpl;
            this.mainImage = mainImage;
            this.gDepth = gDepth;
            this.bayer = bayer;
            this.title = title;
            this.date = date;
            this.loc = loc;
            this.width = width;
            this.height = height;
            this.orientation = orientation;
            this.exif = exif;
            this.resolver = resolver;
            this.listener = listener;
            this.pictureFormat = pictureFormat;
            this.imageType = imageType;
        }

        /* Access modifiers changed, original: protected */
        public void onPreExecute() {
        }

        /* Access modifiers changed, original: protected|varargs */
        public Uri doInBackground(Void... v) {
            this.data = embedGDepthAndBayerInClearSight(this.mainImage, this.imageType);
            if (this.data == null) {
                this.data = this.mainImage;
                Log.e(MediaSaverImpl.TAG, "embedGDepthAndBayerInClearSight fail");
            }
            if (this.width == 0 || this.height == 0) {
                Options options = new Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(this.data, 0, this.data.length, options);
                this.width = options.outWidth;
                this.height = options.outHeight;
            }
            Uri uri = Storage.addFormatImage(this.resolver, this.title, this.date, this.loc, this.orientation, this.exif, this.data, this.width, this.height, this.pictureFormat);
            ProviderUtils.insertOrUpdateHelperDB(this.resolver, uri, this.imageType);
            return uri;
        }

        /* Access modifiers changed, original: protected */
        public void onPostExecute(Uri uri) {
            if (!(this.listener == null || uri == null)) {
                this.listener.onMediaSaved(uri);
            }
            boolean previouslyFull = this.this$0.isQueueFull();
            MediaSaverImpl.access$022(this.this$0, (long) this.data.length);
            if (this.this$0.isQueueFull() != previouslyFull) {
                this.this$0.onQueueAvailable();
            }
        }

        private byte[] embedGDepthAndBayerInClearSight(byte[] clearSightImageBytes, String imageType) {
            Log.d(MediaSaverImpl.TAG, "embedGDepthInClearSight");
            if (clearSightImageBytes == null || (this.gDepth == null && this.bayer == null)) {
                Log.d(MediaSaverImpl.TAG, "clearSightImageBytes is null");
                return null;
            }
            XMPMeta xmpMeta = XmpUtil.createXMPMeta();
            Tag access$200 = MediaSaverImpl.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("gDepth = -----------");
            stringBuilder.append(this.gDepth);
            Log.d(access$200, stringBuilder.toString());
            access$200 = MediaSaverImpl.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("bayer = -----------");
            stringBuilder.append(this.bayer);
            Log.d(access$200, stringBuilder.toString());
            try {
                if (this.gDepth != null) {
                    xmpMeta.setProperty(GDepth.NAMESPACE_URL, "Mime", this.gDepth.getMime());
                    xmpMeta.setProperty(GDepth.NAMESPACE_URL, GDepth.PROPERTY_NEAR, Float.valueOf(this.gDepth.getNear()));
                    xmpMeta.setProperty(GDepth.NAMESPACE_URL, GDepth.PROPERTY_FAR, Float.valueOf(this.gDepth.getFar()));
                    xmpMeta.setProperty(GDepth.NAMESPACE_URL, GDepth.PROPERTY_FORMAT, this.gDepth.getFormat());
                }
                if (this.bayer != null) {
                    xmpMeta.setProperty(GImage.NAMESPACE_URL, "Mime", this.bayer.getMime());
                }
                if (imageType != null) {
                    xmpMeta.setProperty(XmpUtil.NS_GOOGLE_CAMERA, XmpUtil.SPECIAL_TYPE_ID, imageType);
                }
                XMPMeta extendXmpMeta = XmpUtil.createXMPMeta();
                try {
                    if (this.gDepth != null) {
                        extendXmpMeta.setProperty(GDepth.NAMESPACE_URL, "Data", this.gDepth.getData());
                    }
                    if (this.bayer != null) {
                        extendXmpMeta.setProperty(GImage.NAMESPACE_URL, "Data", this.bayer.getData());
                    }
                } catch (XMPException exception) {
                    Log.d(MediaSaverImpl.TAG, "create extended XMPMeta error", exception);
                }
                ByteArrayInputStream bais = new ByteArrayInputStream(clearSightImageBytes);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                if (XmpUtil.writeXMPMeta(bais, baos, xmpMeta, extendXmpMeta)) {
                    return baos.toByteArray();
                }
                Log.e(MediaSaverImpl.TAG, "embedGDepthInClearSight failure ");
                return null;
            } catch (XMPException exception2) {
                Log.d(MediaSaverImpl.TAG, "create XMPMeta error", exception2);
                return null;
            }
        }
    }

    static /* synthetic */ long access$022(MediaSaverImpl x0, long x1) {
        long j = x0.mMemoryUse - x1;
        x0.mMemoryUse = j;
        return j;
    }

    public boolean isQueueFull() {
        return this.mMemoryUse >= 62914560;
    }

    public void addImage(byte[] data, String title, long date, Location loc, int width, int height, int orientation, ExifInterface exif, OnMediaSavedListener l, ContentResolver resolver) {
        Location location = loc;
        if (isQueueFull()) {
            Log.e(TAG, "Cannot add image when the queue is full");
            return;
        }
        ImageSaveTask t = new ImageSaveTask(data, title, date, location == null ? null : new Location(location), width, height, orientation, exif, resolver, l);
        this.mMemoryUse += (long) data.length;
        if (isQueueFull()) {
            onQueueFull();
        }
        t.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
    }

    public void addImage(byte[] data, String title, long date, Location loc, int orientation, ExifInterface exif, OnMediaSavedListener l, ContentResolver resolver) {
        addImage(data, title, date, loc, 0, 0, orientation, exif, l, resolver);
    }

    public void addImage(byte[] data, String title, Location loc, int width, int height, int orientation, ExifInterface exif, OnMediaSavedListener l, ContentResolver resolver) {
        addImage(data, title, System.currentTimeMillis(), loc, width, height, orientation, exif, l, resolver);
    }

    public void addVideo(String path, ContentValues values, OnMediaSavedListener l, ContentResolver resolver) {
        new VideoSaveTask(path, values, l, resolver).execute(new Void[0]);
    }

    public void setQueueListener(QueueListener l) {
        this.mQueueListener = l;
        if (l != null) {
            l.onQueueStatus(isQueueFull());
        }
    }

    private void onQueueFull() {
        if (this.mQueueListener != null) {
            this.mQueueListener.onQueueStatus(true);
        }
    }

    private void onQueueAvailable() {
        if (this.mQueueListener != null) {
            this.mQueueListener.onQueueStatus(false);
        }
    }

    public void addXmpImage(byte[] mainImage, GImage bayer, GDepth gDepth, String title, long date, Location loc, int width, int height, int orientation, ExifInterface exif, String imageType, OnMediaSavedListener l, ContentResolver resolver, String pictureFormat) {
        Location location = loc;
        if (isQueueFull()) {
            Log.e(TAG, "Cannot add image when the queue is full");
            return;
        }
        XmpImageSaveTask t = new XmpImageSaveTask(this, mainImage, bayer, gDepth, title, date, location == null ? null : new Location(location), width, height, orientation, exif, imageType, resolver, l, pictureFormat);
        this.mMemoryUse += (long) mainImage.length;
        if (isQueueFull()) {
            onQueueFull();
        }
        t.execute(new Void[0]);
    }
}
