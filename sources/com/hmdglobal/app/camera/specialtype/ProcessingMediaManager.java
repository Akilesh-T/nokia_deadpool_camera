package com.hmdglobal.app.camera.specialtype;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore.Images.Media;
import android.support.annotation.Nullable;
import android.util.Log;
import com.google.android.apps.photos.api.ProcessingMetadataQuery.ProgressStatus;
import com.hmdglobal.app.camera.app.AppController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ProcessingMediaManager {
    public static final boolean NEED_PROCESSING = true;
    private static final long PROCESSING_TIME_OUT = TimeUnit.SECONDS.toMillis(15);
    private static final String TAG = "ProcessingManager";
    private static volatile ProcessingMediaManager instance;
    private final Object LOCK = new Object();
    private AppController appController;
    private final Context context;
    private final Handler handler;
    private long lastNotFoundDate;
    private final Map<Long, ProcessingMedia> processingMedia = Collections.synchronizedMap(new LinkedHashMap());
    private ProcessingQueueListener queueListener;

    public static final class ProcessingMedia {
        private final long dateTaken;
        private final long mediaStoreId;
        private final AtomicInteger progressPercentage = new AtomicInteger();
        private final ProgressStatus progressStatus;
        private Bitmap thumbnailBitmap;
        private String thumbnailPath;

        ProcessingMedia(long mediaStoreId, long dateTaken, ProgressStatus progressStatus) {
            this.mediaStoreId = mediaStoreId;
            this.dateTaken = dateTaken;
            this.progressStatus = progressStatus;
        }

        public int getProgressStatus() {
            return this.progressStatus.getIdentifier();
        }

        public int getProgressPercentage() {
            return this.progressPercentage.get();
        }

        public void updateProgressPercentage(int percentage) {
            if (percentage < 0 || percentage > 100) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Not a percentage: ");
                stringBuilder.append(percentage);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            this.progressPercentage.set(percentage);
        }

        public long getMediaStoreId() {
            return this.mediaStoreId;
        }

        public long getDateTaken() {
            return this.dateTaken;
        }

        public String getThumbnailPath() {
            return this.thumbnailPath;
        }

        public void setThumbnailPath(String thumbnailPath) {
            this.thumbnailPath = thumbnailPath;
        }

        public void setThumbnailBitmap(Bitmap bitmap) {
            this.thumbnailBitmap = bitmap;
        }

        public Bitmap getThumbnailBitmap() {
            return this.thumbnailBitmap;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mediaStoreId = ");
            stringBuilder.append(this.mediaStoreId);
            stringBuilder.append(", dateTaken = ");
            stringBuilder.append(this.dateTaken);
            stringBuilder.append(", progressStatus = ");
            stringBuilder.append(this.progressStatus);
            return stringBuilder.toString();
        }
    }

    public interface ProcessingQueueListener {
        void onQueueEmpty();
    }

    public static ProcessingMediaManager getInstance(Context context) {
        if (instance == null) {
            synchronized (ProcessingMediaManager.class) {
                if (instance == null) {
                    instance = new ProcessingMediaManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private ProcessingMediaManager(Context context) {
        this.context = context.getApplicationContext();
        this.handler = new Handler(Looper.getMainLooper());
    }

    public List<ProcessingMedia> getProcessingMedia() {
        List unmodifiableList;
        synchronized (this.processingMedia) {
            unmodifiableList = Collections.unmodifiableList(new ArrayList(this.processingMedia.values()));
        }
        return unmodifiableList;
    }

    public ProcessingMedia add(long dateTaken) {
        synchronized (this.LOCK) {
            if (this.lastNotFoundDate == dateTaken) {
                Log.d(TAG, "Already insert to MediaStore in class Storage!");
                return null;
            }
            Uri mediaStoreUri = new MediaStoreProcessingSaver(this.context).saveAsHidden(dateTaken);
            if (mediaStoreUri == null) {
                Log.d(TAG, "Failed to save hidden media store row");
                return null;
            }
            final long mediaStoreId = ContentUris.parseId(mediaStoreUri);
            ProcessingMedia processingMedia = new ProcessingMedia(mediaStoreId, dateTaken, ProgressStatus.INDETERMINATE);
            this.processingMedia.put(Long.valueOf(mediaStoreId), processingMedia);
            notifyProcessingUri(mediaStoreId);
            this.handler.postDelayed(new Runnable() {
                public void run() {
                    ProcessingMediaManager.this.handleProcessingTimeOut(mediaStoreId);
                }
            }, PROCESSING_TIME_OUT);
            AppController appController = this.appController;
            return processingMedia;
        }
    }

    private void handleProcessingTimeOut(long mediaStoreId) {
        if (this.processingMedia.get(Long.valueOf(mediaStoreId)) != null) {
            removeById(mediaStoreId);
            int delNum = this.context.getContentResolver().delete(ContentUris.withAppendedId(HmdThumbnailProvider.PLACE_HOLDER_URI, mediaStoreId), null, null);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("delete time out row from Media Store: del num = ");
            stringBuilder.append(delNum);
            Log.d(str, stringBuilder.toString());
        }
    }

    @Nullable
    public ProcessingMedia getById(long mediaStoreId) {
        return (ProcessingMedia) this.processingMedia.get(Long.valueOf(mediaStoreId));
    }

    public ProcessingMedia getByDateSynced(long date) {
        synchronized (this.LOCK) {
            for (ProcessingMedia media : getProcessingMedia()) {
                if (media != null && media.getDateTaken() == date) {
                    return media;
                }
            }
            this.lastNotFoundDate = date;
            return null;
        }
    }

    public ProcessingMedia getByDate(long date) {
        for (ProcessingMedia media : getProcessingMedia()) {
            if (media != null && Long.compare(media.getDateTaken(), date) == 0) {
                return media;
            }
        }
        return null;
    }

    public void removeById(long mediaStoreId) {
        synchronized (this.LOCK) {
            ProcessingMedia m = (ProcessingMedia) this.processingMedia.remove(Long.valueOf(mediaStoreId));
            if (m != null) {
                if (m.getThumbnailBitmap() != null) {
                    m.getThumbnailBitmap().recycle();
                }
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("remove: mediaStoreId = ");
                stringBuilder.append(mediaStoreId);
                Log.d(str, stringBuilder.toString());
                notifyProcessingUri(mediaStoreId);
                if (!(!this.processingMedia.isEmpty() || this.appController == null || this.queueListener == null)) {
                    this.queueListener.onQueueEmpty();
                }
            }
        }
    }

    public void setProcessingQueueListener(ProcessingQueueListener listener) {
        this.queueListener = listener;
    }

    public void removeByDate(long date) {
        ProcessingMedia media = getByDate(date);
        if (media != null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("removeByDate: date = ");
            stringBuilder.append(date);
            Log.d(str, stringBuilder.toString());
            removeById(media.getMediaStoreId());
        }
    }

    public void remove(ProcessingMedia m) {
        if (m != null) {
            removeById(m.getMediaStoreId());
        }
    }

    public void setAppController(AppController appController) {
        this.appController = appController;
    }

    private void notifyProcessingUri(long mediaStoreId) {
        this.context.getContentResolver().notifyChange(ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, mediaStoreId), null);
    }
}
