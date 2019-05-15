package com.hmdglobal.app.camera.data;

import android.content.Context;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.hmdglobal.app.camera.R;
import com.hmdglobal.app.camera.Storage;
import com.hmdglobal.app.camera.data.LocalData.ActionCallback;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class LocalSessionData implements LocalData {
    private final long mDateTaken = new Date().getTime();
    private int mHeight;
    protected final Bundle mMetaData = new Bundle();
    private final Uri mUri;
    private int mWidth;

    public LocalSessionData(Uri uri) {
        this.mUri = uri;
        refreshSize(uri);
    }

    private void refreshSize(Uri uri) {
        Point size = Storage.getSizeForSession(uri);
        this.mWidth = size.x;
        this.mHeight = size.y;
    }

    public View getView(Context context, View recycled, int thumbWidth, int thumbHeight, int placeholderResourcedId, LocalDataAdapter adapter, boolean isInProgress, ActionCallback actionCallback) {
        ImageView imageView;
        if (recycled != null) {
            imageView = (ImageView) recycled;
        } else {
            imageView = new ImageView(context);
            imageView.setTag(R.id.mediadata_tag_viewtype, Integer.valueOf(getItemViewType().ordinal()));
        }
        byte[] jpegData = Storage.getJpegForSession(this.mUri);
        int currentVersion = Storage.getJpegVersionForSession(this.mUri);
        RequestManager with = Glide.with(context);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.mUri.toString());
        stringBuilder.append(currentVersion);
        with.load(jpegData, stringBuilder.toString()).fitCenter().into(imageView);
        imageView.setContentDescription(context.getResources().getString(R.string.media_processing_content_description));
        return imageView;
    }

    public LocalDataViewType getItemViewType() {
        return LocalDataViewType.SESSION;
    }

    public void loadFullImage(Context context, int width, int height, View view, LocalDataAdapter adapter) {
    }

    public long getDateTaken() {
        return this.mDateTaken;
    }

    public long getDateModified() {
        return TimeUnit.MILLISECONDS.toSeconds(this.mDateTaken);
    }

    public String getTitle() {
        return this.mUri.toString();
    }

    public boolean isDataActionSupported(int actions) {
        return false;
    }

    public boolean delete(Context c) {
        return false;
    }

    public void onFullScreen(boolean fullScreen) {
    }

    public boolean canSwipeInFullScreen() {
        return true;
    }

    public String getPath() {
        return "";
    }

    public String getMimeType() {
        return null;
    }

    public MediaDetails getMediaDetails(Context context) {
        return null;
    }

    public int getLocalDataType() {
        return 5;
    }

    public long getSizeInBytes() {
        return 0;
    }

    public LocalData refresh(Context context) {
        refreshSize(this.mUri);
        return this;
    }

    public long getContentId() {
        return 0;
    }

    public Bundle getMetadata() {
        return this.mMetaData;
    }

    public String getSignature() {
        return "";
    }

    public boolean isMetadataUpdated() {
        return true;
    }

    public int getRotation() {
        return 0;
    }

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }

    public int getViewType() {
        return 2;
    }

    public double[] getLatLong() {
        return null;
    }

    public boolean isUIActionSupported(int action) {
        return false;
    }

    public void prepare() {
    }

    public void recycle(View view) {
        Glide.clear(view);
    }

    public Uri getUri() {
        return this.mUri;
    }
}
