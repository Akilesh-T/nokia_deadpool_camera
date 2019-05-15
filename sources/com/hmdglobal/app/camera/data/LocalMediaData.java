package com.hmdglobal.app.camera.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.media.CamcorderProfile;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.provider.MediaStore.Video;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import com.android.ex.camera2.portability.Size;
import com.bumptech.glide.BitmapRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.ResourceEncoder;
import com.bumptech.glide.load.resource.bitmap.BitmapEncoder;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.Storage;
import com.hmdglobal.app.camera.data.LocalData.ActionCallback;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.provider.InfoTable;
import com.morphoinc.app.panoramagp3.Camera2ParamsFragment;
import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public abstract class LocalMediaData implements LocalData {
    private static final String CAMERA_PATH;
    private static final int JPEG_COMPRESS_QUALITY = 90;
    private static final BitmapEncoder JPEG_ENCODER = new BitmapEncoder(CompressFormat.JPEG, 90);
    private static final int MEDIASTORE_THUMB_HEIGHT = 384;
    private static final int MEDIASTORE_THUMB_WIDTH = 512;
    static final int QUERY_ALL_MEDIA_ID = -1;
    private static final String SELECT_BY_PATH = "_data LIKE ?";
    protected final long mContentId;
    protected final long mDateModifiedInSeconds;
    protected final long mDateTakenInMilliSeconds;
    protected final int mHeight;
    protected final double mLatitude;
    protected final double mLongitude;
    protected final Bundle mMetaData;
    protected final String mMimeType;
    protected final String mPath;
    protected final long mSizeInBytes;
    protected final String mTitle;
    protected Boolean mUsing = Boolean.valueOf(false);
    protected final int mWidth;

    private interface CursorToLocalData {
        LocalData build(Cursor cursor);
    }

    private static class VideoViewHolder {
        private final ImageView mPlayButton;
        private final ImageView mVideoView;

        public VideoViewHolder(ImageView videoView, ImageView playButton) {
            this.mVideoView = videoView;
            this.mPlayButton = playButton;
        }
    }

    private static class VideoDataBuilder implements CursorToLocalData {
        private VideoDataBuilder() {
        }

        public VideoData build(Cursor cursor) {
            return VideoData.buildFromCursor(cursor);
        }
    }

    public static final class PhotoData extends LocalMediaData {
        public static final int COL_DATA = 5;
        public static final int COL_DATE_MODIFIED = 4;
        public static final int COL_DATE_TAKEN = 3;
        public static final int COL_HEIGHT = 8;
        public static final int COL_ID = 0;
        public static final int COL_LATITUDE = 10;
        public static final int COL_LONGITUDE = 11;
        public static final int COL_MIME_TYPE = 2;
        public static final int COL_ORIENTATION = 6;
        public static final int COL_SIZE = 9;
        public static final int COL_TITLE = 1;
        public static final int COL_WIDTH = 7;
        static final Uri CONTENT_URI = Media.EXTERNAL_CONTENT_URI;
        private static final int MAXIMUM_TEXTURE_SIZE = 2048;
        private static final String QUERY_ORDER = "_id DESC";
        private static final String[] QUERY_PROJECTION = new String[]{"_id", "title", InfoTable.MIME_TYPE, "datetaken", InfoTable.DATE_MODIFIED, "_data", "orientation", InfoTable.WIDTH, InfoTable.HEIGHT, "_size", InfoTable.LATITUDE, InfoTable.LONGITUDE};
        private static final Tag TAG = new Tag("PhotoData");
        private static final int mSupportedDataActions = 14;
        private static final int mSupportedUIActions = 7;
        private final int mOrientation;
        private final String mSignature;

        private static class PhotoDataBuilder implements CursorToLocalData {
            private PhotoDataBuilder() {
            }

            public PhotoData build(Cursor cursor) {
                return PhotoData.buildFromCursor(cursor);
            }
        }

        public static LocalData fromContentUri(ContentResolver cr, Uri contentUri) {
            List<LocalData> newPhotos = query(cr, contentUri, -1);
            if (newPhotos.isEmpty()) {
                return null;
            }
            return (LocalData) newPhotos.get(0);
        }

        public PhotoData(long id, String title, String mimeType, long dateTakenInMilliSeconds, long dateModifiedInSeconds, String path, int orientation, int width, int height, long sizeInBytes, double latitude, double longitude) {
            int i = orientation;
            super(id, title, mimeType, dateTakenInMilliSeconds, dateModifiedInSeconds, path, width, height, sizeInBytes, latitude, longitude);
            int i2 = orientation;
            this.mOrientation = i2;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(mimeType);
            stringBuilder.append(i2);
            stringBuilder.append(dateModifiedInSeconds);
            this.mSignature = stringBuilder.toString();
        }

        static List<LocalData> query(ContentResolver cr, Uri uri, long lastId) {
            return LocalMediaData.queryLocalMediaData(cr, uri, QUERY_PROJECTION, lastId, QUERY_ORDER, new PhotoDataBuilder());
        }

        private static PhotoData buildFromCursor(Cursor c) {
            Bitmap b;
            int height;
            long sizeInBytes;
            double latitude;
            double longitude;
            Cursor cursor = c;
            long id = cursor.getLong(0);
            String title = cursor.getString(1);
            String mimeType = cursor.getString(2);
            long dateTakenInMilliSeconds = cursor.getLong(3);
            long dateModifiedInSeconds = cursor.getLong(4);
            String path = cursor.getString(5);
            int orientation = cursor.getInt(6);
            int width = cursor.getInt(7);
            int height2 = cursor.getInt(8);
            if (width <= 0 || height2 <= 0) {
                Tag tag = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Zero dimension in ContentResolver for ");
                stringBuilder.append(path);
                stringBuilder.append(":");
                stringBuilder.append(width);
                stringBuilder.append("x");
                stringBuilder.append(height2);
                Log.w(tag, stringBuilder.toString());
                Options opts = new Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, opts);
                if (opts.outWidth <= 0 || opts.outHeight <= 0) {
                    Tag tag2 = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Dimension decode failed for ");
                    stringBuilder.append(path);
                    Log.w(tag2, stringBuilder.toString());
                    int width2 = BitmapFactory.decodeFile(path);
                    Tag tag3;
                    StringBuilder stringBuilder2;
                    if (width2 == 0) {
                        tag3 = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("PhotoData skipped. Decoding ");
                        stringBuilder2.append(path);
                        stringBuilder2.append("failed.");
                        Log.w(tag3, stringBuilder2.toString());
                        return null;
                    }
                    String path2;
                    width = width2.getWidth();
                    height2 = width2.getHeight();
                    if (width == 0) {
                        path2 = path;
                    } else if (height2 == 0) {
                        path2 = path;
                    }
                    tag3 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("PhotoData skipped. Bitmap size 0 for ");
                    stringBuilder2.append(path2);
                    Log.w(tag3, stringBuilder2.toString());
                    return null;
                }
                b = opts.outWidth;
                height = opts.outHeight;
                sizeInBytes = cursor.getLong(9);
                latitude = cursor.getDouble(10);
                longitude = cursor.getDouble(11);
                return new PhotoData(id, title, mimeType, dateTakenInMilliSeconds, dateModifiedInSeconds, path, orientation, b, height, sizeInBytes, latitude, longitude);
            }
            b = width;
            height = height2;
            sizeInBytes = cursor.getLong(9);
            latitude = cursor.getDouble(10);
            longitude = cursor.getDouble(11);
            return new PhotoData(id, title, mimeType, dateTakenInMilliSeconds, dateModifiedInSeconds, path, orientation, b, height, sizeInBytes, latitude, longitude);
        }

        public int getRotation() {
            return this.mOrientation;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Photo:,data=");
            stringBuilder.append(this.mPath);
            stringBuilder.append(",mimeType=");
            stringBuilder.append(this.mMimeType);
            stringBuilder.append(Size.DELIMITER);
            stringBuilder.append(this.mWidth);
            stringBuilder.append("x");
            stringBuilder.append(this.mHeight);
            stringBuilder.append(",orientation=");
            stringBuilder.append(this.mOrientation);
            stringBuilder.append(",date=");
            stringBuilder.append(new Date(this.mDateTakenInMilliSeconds));
            return stringBuilder.toString();
        }

        public int getViewType() {
            return 2;
        }

        public boolean isUIActionSupported(int action) {
            return (action & 7) == action;
        }

        public boolean isDataActionSupported(int action) {
            return (action & 14) == action;
        }

        public boolean delete(Context context) {
            ContentResolver cr = context.getContentResolver();
            Uri uri = CONTENT_URI;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("_id=");
            stringBuilder.append(this.mContentId);
            cr.delete(uri, stringBuilder.toString(), null);
            return super.delete(context);
        }

        public Uri getUri() {
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(this.mContentId)).build();
        }

        public MediaDetails getMediaDetails(Context context) {
            MediaDetails mediaDetails = super.getMediaDetails(context);
            MediaDetails.extractExifInfo(mediaDetails, this.mPath);
            mediaDetails.addDetail(7, Integer.valueOf(this.mOrientation));
            return mediaDetails;
        }

        public int getLocalDataType() {
            return 3;
        }

        public LocalData refresh(Context context) {
            PhotoData newData = null;
            Cursor c = context.getContentResolver().query(getUri(), QUERY_PROJECTION, null, null, null);
            if (c != null) {
                if (c.moveToFirst()) {
                    newData = buildFromCursor(c);
                }
                c.close();
            }
            return newData;
        }

        public String getSignature() {
            return this.mSignature;
        }

        /* Access modifiers changed, original: protected */
        public ImageView fillImageView(Context context, ImageView v, int thumbWidth, int thumbHeight, int placeHolderResourceId, LocalDataAdapter adapter, boolean isInProgress) {
            loadImage(context, v, thumbWidth, thumbHeight, placeHolderResourceId, false);
            int stringId = R.string.photo_date_content_description;
            if (PanoramaMetadataLoader.isPanorama(this) || PanoramaMetadataLoader.isPanorama360(this)) {
                stringId = R.string.panorama_date_content_description;
            } else if (PanoramaMetadataLoader.isPanoramaAndUseViewer(this)) {
                stringId = R.string.photosphere_date_content_description;
            } else if (RgbzMetadataLoader.hasRGBZData(this)) {
                stringId = R.string.refocus_date_content_description;
            }
            v.setContentDescription(context.getResources().getString(stringId, new Object[]{LocalMediaData.getReadableDate(this.mDateModifiedInSeconds)}));
            return v;
        }

        private void loadImage(Context context, ImageView imageView, int thumbWidth, int thumbHeight, int placeHolderResourceId, boolean full) {
            if (thumbWidth > 0 && thumbHeight > 0) {
                int overrideWidth;
                int overrideHeight;
                BitmapRequestBuilder thumbnailRequest;
                if (full) {
                    overrideWidth = Math.min(getWidth(), 2048);
                    overrideHeight = Math.min(getHeight(), 2048);
                    thumbnailRequest = loadUri(context).override(thumbWidth, thumbHeight).fitCenter().thumbnail(loadMediaStoreThumb(context));
                } else {
                    overrideWidth = thumbWidth;
                    overrideHeight = thumbHeight;
                    thumbnailRequest = loadMediaStoreThumb(context);
                }
                loadUri(context).placeholder(placeHolderResourceId).fitCenter().override(overrideWidth, overrideHeight).thumbnail(thumbnailRequest).into(imageView);
            }
        }

        private BitmapRequestBuilder<Uri, Bitmap> loadMediaStoreThumb(Context context) {
            return loadUri(context).override(512, (int) LocalMediaData.MEDIASTORE_THUMB_HEIGHT);
        }

        private BitmapRequestBuilder<Uri, Bitmap> loadUri(Context context) {
            return Glide.with(context).loadFromMediaStore(getUri(), this.mMimeType, this.mDateModifiedInSeconds, this.mOrientation).asBitmap().encoder((ResourceEncoder) LocalMediaData.JPEG_ENCODER);
        }

        public void recycle(View view) {
            super.recycle(view);
            if (view != null) {
                Glide.clear(view);
            }
        }

        public LocalDataViewType getItemViewType() {
            return LocalDataViewType.PHOTO;
        }

        public void loadFullImage(Context context, int thumbWidth, int thumbHeight, View v, LocalDataAdapter adapter) {
            loadImage(context, (ImageView) v, thumbWidth, thumbHeight, 0, true);
        }
    }

    public static final class VideoData extends LocalMediaData {
        public static final int COL_DATA = 5;
        public static final int COL_DATE_MODIFIED = 4;
        public static final int COL_DATE_TAKEN = 3;
        public static final int COL_DURATION = 11;
        public static final int COL_HEIGHT = 7;
        public static final int COL_ID = 0;
        public static final int COL_LATITUDE = 9;
        public static final int COL_LONGITUDE = 10;
        public static final int COL_MIME_TYPE = 2;
        public static final int COL_SIZE = 8;
        public static final int COL_TITLE = 1;
        public static final int COL_WIDTH = 6;
        static final Uri CONTENT_URI = Video.Media.EXTERNAL_CONTENT_URI;
        private static final String QUERY_ORDER = "datetaken DESC, _id DESC";
        private static final String[] QUERY_PROJECTION = new String[]{"_id", "title", InfoTable.MIME_TYPE, "datetaken", InfoTable.DATE_MODIFIED, "_data", InfoTable.WIDTH, InfoTable.HEIGHT, "_size", InfoTable.LATITUDE, InfoTable.LONGITUDE, InfoTable.DURATION};
        private static final int mSupportedDataActions = 11;
        private static final int mSupportedUIActions = 3;
        private final long mDurationInSeconds;
        private final String mSignature;

        public VideoData(long id, String title, String mimeType, long dateTakenInMilliSeconds, long dateModifiedInSeconds, String path, int width, int height, long sizeInBytes, double latitude, double longitude, long durationInSeconds) {
            super(id, title, mimeType, dateTakenInMilliSeconds, dateModifiedInSeconds, path, width, height, sizeInBytes, latitude, longitude);
            this.mDurationInSeconds = durationInSeconds;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(mimeType);
            stringBuilder.append(dateModifiedInSeconds);
            this.mSignature = stringBuilder.toString();
        }

        public static LocalData fromContentUri(ContentResolver cr, Uri contentUri) {
            List<LocalData> newVideos = query(cr, contentUri, -1);
            if (newVideos.isEmpty()) {
                return null;
            }
            return (LocalData) newVideos.get(0);
        }

        static List<LocalData> query(ContentResolver cr, Uri uri, long lastId) {
            return LocalMediaData.queryLocalMediaData(cr, uri, QUERY_PROJECTION, lastId, QUERY_ORDER, new VideoDataBuilder());
        }

        private int getBestWidth() {
            int metadataWidth = VideoRotationMetadataLoader.getWidth(this);
            if (metadataWidth > 0) {
                return metadataWidth;
            }
            return this.mWidth;
        }

        private int getBestHeight() {
            int metadataHeight = VideoRotationMetadataLoader.getHeight(this);
            if (metadataHeight > 0) {
                return metadataHeight;
            }
            return this.mHeight;
        }

        public int getWidth() {
            return VideoRotationMetadataLoader.isRotated(this) ? getBestHeight() : getBestWidth();
        }

        public int getHeight() {
            return VideoRotationMetadataLoader.isRotated(this) ? getBestWidth() : getBestHeight();
        }

        private static VideoData buildFromCursor(Cursor c) {
            int height;
            int height2;
            Cursor cursor = c;
            long id = cursor.getLong(0);
            String title = cursor.getString(1);
            String mimeType = cursor.getString(2);
            long dateTakenInMilliSeconds = cursor.getLong(3);
            long dateModifiedInSeconds = cursor.getLong(4);
            String path = cursor.getString(5);
            int width = cursor.getInt(6);
            int height3 = cursor.getInt(7);
            if (width == 0 || height3 == 0) {
                Log.w(TAG, "failed to retrieve width and height from the media store, defaulting  to camera profile");
                height = CamcorderProfile.get(1);
                width = height.videoFrameWidth;
                height2 = height.videoFrameHeight;
                height = width;
            } else {
                height = width;
                height2 = height3;
            }
            return new VideoData(id, title, mimeType, dateTakenInMilliSeconds, dateModifiedInSeconds, path, height, height2, cursor.getLong(8), cursor.getDouble(9), cursor.getDouble(10), cursor.getLong(11) / 1000);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Video:,data=");
            stringBuilder.append(this.mPath);
            stringBuilder.append(",mimeType=");
            stringBuilder.append(this.mMimeType);
            stringBuilder.append(Size.DELIMITER);
            stringBuilder.append(this.mWidth);
            stringBuilder.append("x");
            stringBuilder.append(this.mHeight);
            stringBuilder.append(",date=");
            stringBuilder.append(new Date(this.mDateTakenInMilliSeconds));
            return stringBuilder.toString();
        }

        public int getViewType() {
            return 2;
        }

        public boolean isUIActionSupported(int action) {
            return (action & 3) == action;
        }

        public boolean isDataActionSupported(int action) {
            return (action & 11) == action;
        }

        public boolean delete(Context context) {
            ContentResolver cr = context.getContentResolver();
            Uri uri = CONTENT_URI;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("_id=");
            stringBuilder.append(this.mContentId);
            cr.delete(uri, stringBuilder.toString(), null);
            return super.delete(context);
        }

        public Uri getUri() {
            return CONTENT_URI.buildUpon().appendPath(String.valueOf(this.mContentId)).build();
        }

        public MediaDetails getMediaDetails(Context context) {
            MediaDetails mediaDetails = super.getMediaDetails(context);
            mediaDetails.addDetail(8, MediaDetails.formatDuration(context, this.mDurationInSeconds));
            return mediaDetails;
        }

        public int getLocalDataType() {
            return 4;
        }

        public LocalData refresh(Context context) {
            Cursor c = context.getContentResolver().query(getUri(), QUERY_PROJECTION, null, null, null);
            if (c == null || !c.moveToFirst()) {
                return null;
            }
            return buildFromCursor(c);
        }

        public String getSignature() {
            return this.mSignature;
        }

        /* Access modifiers changed, original: protected */
        public ImageView fillImageView(Context context, ImageView v, int thumbWidth, int thumbHeight, int placeHolderResourceId, LocalDataAdapter adapter, boolean isInProgress) {
            if (thumbWidth <= 0 || thumbHeight <= 0) {
                return v;
            }
            Glide.with(context).loadFromMediaStore(getUri(), this.mMimeType, this.mDateModifiedInSeconds, 0).asBitmap().encoder((ResourceEncoder) LocalMediaData.JPEG_ENCODER).thumbnail(Glide.with(context).loadFromMediaStore(getUri(), this.mMimeType, this.mDateModifiedInSeconds, 0).asBitmap().encoder((ResourceEncoder) LocalMediaData.JPEG_ENCODER).override(512, (int) LocalMediaData.MEDIASTORE_THUMB_HEIGHT)).placeholder(placeHolderResourceId).fitCenter().override(thumbWidth, thumbHeight).into(v);
            return v;
        }

        public View getView(Context context, View recycled, int thumbWidth, int thumbHeight, int placeHolderResourceId, LocalDataAdapter adapter, boolean isInProgress, ActionCallback actionCallback) {
            VideoViewHolder viewHolder;
            View result;
            View view = recycled;
            if (view != null) {
                viewHolder = (VideoViewHolder) view.getTag(R.id.mediadata_tag_target);
                result = view;
            } else {
                View result2 = LayoutInflater.from(context).inflate(R.layout.filmstrip_video, null);
                result2.setTag(R.id.mediadata_tag_viewtype, Integer.valueOf(getItemViewType().ordinal()));
                VideoViewHolder viewHolder2 = new VideoViewHolder((ImageView) result2.findViewById(R.id.video_view), (ImageView) result2.findViewById(R.id.play_button));
                result2.setTag(R.id.mediadata_tag_target, viewHolder2);
                result = result2;
                viewHolder = viewHolder2;
            }
            fillImageView(context, viewHolder.mVideoView, thumbWidth, thumbHeight, placeHolderResourceId, adapter, isInProgress);
            final ActionCallback actionCallback2 = actionCallback;
            viewHolder.mPlayButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    actionCallback2.playVideo(VideoData.this.getUri(), VideoData.this.mTitle);
                }
            });
            result.setContentDescription(context.getResources().getString(R.string.video_date_content_description, new Object[]{LocalMediaData.getReadableDate(this.mDateModifiedInSeconds)}));
            return result;
        }

        public void recycle(View view) {
            super.recycle(view);
            Glide.clear(((VideoViewHolder) view.getTag(R.id.mediadata_tag_target)).mVideoView);
        }

        public LocalDataViewType getItemViewType() {
            return LocalDataViewType.VIDEO;
        }
    }

    public abstract int getViewType();

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Storage.DIRECTORY);
        stringBuilder.append("%");
        CAMERA_PATH = stringBuilder.toString();
    }

    public LocalMediaData(long contentId, String title, String mimeType, long dateTakenInMilliSeconds, long dateModifiedInSeconds, String path, int width, int height, long sizeInBytes, double latitude, double longitude) {
        this.mContentId = contentId;
        this.mTitle = title;
        this.mMimeType = mimeType;
        this.mDateTakenInMilliSeconds = dateTakenInMilliSeconds;
        this.mDateModifiedInSeconds = dateModifiedInSeconds;
        this.mPath = path;
        this.mWidth = width;
        this.mHeight = height;
        this.mSizeInBytes = sizeInBytes;
        this.mLatitude = latitude;
        this.mLongitude = longitude;
        this.mMetaData = new Bundle();
    }

    private static List<LocalData> queryLocalMediaData(ContentResolver contentResolver, Uri contentUri, String[] projection, long minimumId, String orderBy, CursorToLocalData builder) {
        Cursor cursor = contentResolver.query(contentUri, projection, "_data LIKE ? AND _id > ?", new String[]{getCameraPath(), Long.toString(minimumId)}, orderBy);
        List<LocalData> result = new ArrayList();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                LocalData data = builder.build(cursor);
                if (data != null) {
                    result.add(data);
                } else {
                    int dataIndex = cursor.getColumnIndexOrThrow("_data");
                    Tag tag = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Error loading data:");
                    stringBuilder.append(cursor.getString(dataIndex));
                    Log.e(tag, stringBuilder.toString());
                }
            }
            cursor.close();
        }
        return result;
    }

    private static String getCameraPath() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Storage.DIRECTORY);
        stringBuilder.append("%");
        return stringBuilder.toString();
    }

    public long getDateTaken() {
        return this.mDateTakenInMilliSeconds;
    }

    public long getDateModified() {
        return this.mDateModifiedInSeconds;
    }

    public long getContentId() {
        return this.mContentId;
    }

    public String getTitle() {
        return this.mTitle;
    }

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }

    public int getRotation() {
        return 0;
    }

    public String getPath() {
        return this.mPath;
    }

    public long getSizeInBytes() {
        return this.mSizeInBytes;
    }

    public boolean isUIActionSupported(int action) {
        return false;
    }

    public boolean isDataActionSupported(int action) {
        return false;
    }

    public boolean delete(Context context) {
        return new File(this.mPath).delete();
    }

    public void onFullScreen(boolean fullScreen) {
    }

    public boolean canSwipeInFullScreen() {
        return true;
    }

    /* Access modifiers changed, original: protected */
    public ImageView fillImageView(Context context, ImageView v, int thumbWidth, int thumbHeight, int placeHolderResourceId, LocalDataAdapter adapter, boolean isInProgress) {
        Glide.with(context).loadFromMediaStore(getUri(), this.mMimeType, this.mDateModifiedInSeconds, 0).fitCenter().placeholder(placeHolderResourceId).into(v);
        v.setContentDescription(context.getResources().getString(R.string.media_date_content_description, new Object[]{getReadableDate(this.mDateModifiedInSeconds)}));
        return v;
    }

    public View getView(Context context, View recycled, int thumbWidth, int thumbHeight, int placeHolderResourceId, LocalDataAdapter adapter, boolean isInProgress, ActionCallback actionCallback) {
        ImageView imageView;
        if (recycled != null) {
            imageView = (ImageView) recycled;
        } else {
            imageView = (ImageView) LayoutInflater.from(context).inflate(R.layout.filmstrip_image, null);
            imageView.setTag(R.id.mediadata_tag_viewtype, Integer.valueOf(getItemViewType().ordinal()));
        }
        return fillImageView(context, imageView, thumbWidth, thumbHeight, placeHolderResourceId, adapter, isInProgress);
    }

    public void loadFullImage(Context context, int thumbWidth, int thumbHeight, View view, LocalDataAdapter adapter) {
    }

    public void prepare() {
        synchronized (this.mUsing) {
            this.mUsing = Boolean.valueOf(true);
        }
    }

    public void recycle(View view) {
        synchronized (this.mUsing) {
            this.mUsing = Boolean.valueOf(false);
        }
    }

    public double[] getLatLong() {
        if (this.mLatitude == Camera2ParamsFragment.TARGET_EV && this.mLongitude == Camera2ParamsFragment.TARGET_EV) {
            return null;
        }
        return new double[]{this.mLatitude, this.mLongitude};
    }

    /* Access modifiers changed, original: protected */
    public boolean isUsing() {
        boolean booleanValue;
        synchronized (this.mUsing) {
            booleanValue = this.mUsing.booleanValue();
        }
        return booleanValue;
    }

    public String getMimeType() {
        return this.mMimeType;
    }

    public MediaDetails getMediaDetails(Context context) {
        MediaDetails mediaDetails = new MediaDetails();
        mediaDetails.addDetail(1, this.mTitle);
        mediaDetails.addDetail(5, Integer.valueOf(this.mWidth));
        mediaDetails.addDetail(6, Integer.valueOf(this.mHeight));
        mediaDetails.addDetail(200, this.mPath);
        mediaDetails.addDetail(3, getReadableDate(this.mDateModifiedInSeconds));
        if (this.mSizeInBytes > 0) {
            mediaDetails.addDetail(10, Long.valueOf(this.mSizeInBytes));
        }
        if (!(this.mLatitude == Camera2ParamsFragment.TARGET_EV || this.mLongitude == Camera2ParamsFragment.TARGET_EV)) {
            mediaDetails.addDetail(4, String.format(Locale.getDefault(), "%f, %f", new Object[]{Double.valueOf(this.mLatitude), Double.valueOf(this.mLongitude)}));
        }
        return mediaDetails;
    }

    private static String getReadableDate(long dateInSeconds) {
        return DateFormat.getDateTimeInstance().format(new Date(1000 * dateInSeconds));
    }

    public Bundle getMetadata() {
        return this.mMetaData;
    }

    public boolean isMetadataUpdated() {
        return MetadataLoader.isMetadataCached(this);
    }
}
