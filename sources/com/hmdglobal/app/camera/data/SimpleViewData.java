package com.hmdglobal.app.camera.data;

import android.content.Context;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.view.View;
import com.hmdglobal.app.camera.data.LocalData.ActionCallback;
import com.hmdglobal.app.camera.debug.Log.Tag;
import java.util.UUID;

public class SimpleViewData implements LocalData {
    private static final String SIMPLE_VIEW_URI_SCHEME = "simple_view_data";
    private static final Tag TAG = new Tag("SimpleViewData");
    private final long mDateModified;
    private final long mDateTaken;
    private final int mHeight;
    private final LocalDataViewType mItemViewType;
    private final Bundle mMetaData = new Bundle();
    private final Uri mUri;
    private final View mView;
    private final int mWidth;

    public SimpleViewData(View v, LocalDataViewType viewType, int width, int height, int dateTaken, int dateModified) {
        this.mView = v;
        this.mItemViewType = viewType;
        this.mWidth = width;
        this.mHeight = height;
        this.mDateTaken = (long) dateTaken;
        this.mDateModified = (long) dateModified;
        Builder builder = new Builder();
        builder.scheme(SIMPLE_VIEW_URI_SCHEME).appendPath(UUID.randomUUID().toString());
        this.mUri = builder.build();
    }

    public long getDateTaken() {
        return this.mDateTaken;
    }

    public long getDateModified() {
        return this.mDateModified;
    }

    public String getTitle() {
        return "";
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

    public int getViewType() {
        return 2;
    }

    public LocalDataViewType getItemViewType() {
        return this.mItemViewType;
    }

    public String getPath() {
        return "";
    }

    public Uri getUri() {
        return this.mUri;
    }

    public int getLocalDataType() {
        return 2;
    }

    public LocalData refresh(Context context) {
        return this;
    }

    public boolean isUIActionSupported(int action) {
        return false;
    }

    public boolean isDataActionSupported(int action) {
        return false;
    }

    public boolean delete(Context c) {
        return false;
    }

    public View getView(Context context, View recycled, int width, int height, int placeHolderResourceId, LocalDataAdapter adapter, boolean isInProgressSession, ActionCallback actionCallback) {
        return this.mView;
    }

    public void loadFullImage(Context context, int w, int h, View view, LocalDataAdapter adapter) {
    }

    public void prepare() {
    }

    public void recycle(View view) {
    }

    public void onFullScreen(boolean fullScreen) {
    }

    public boolean canSwipeInFullScreen() {
        return true;
    }

    public MediaDetails getMediaDetails(Context context) {
        return null;
    }

    public double[] getLatLong() {
        return null;
    }

    public String getMimeType() {
        return null;
    }

    public long getSizeInBytes() {
        return 0;
    }

    public long getContentId() {
        return -1;
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
}
