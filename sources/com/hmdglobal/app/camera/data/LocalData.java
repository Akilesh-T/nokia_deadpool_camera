package com.hmdglobal.app.camera.data;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.filmstrip.ImageData;
import java.util.Comparator;

public interface LocalData extends ImageData {
    public static final int DATA_ACTION_DELETE = 2;
    public static final int DATA_ACTION_EDIT = 4;
    public static final int DATA_ACTION_NONE = 0;
    public static final int DATA_ACTION_PLAY = 1;
    public static final int DATA_ACTION_SHARE = 8;
    public static final int LOCAL_CAMERA_PREVIEW = 1;
    public static final int LOCAL_IMAGE = 3;
    public static final int LOCAL_IN_PROGRESS_DATA = 5;
    public static final int LOCAL_VIDEO = 4;
    public static final int LOCAL_VIEW = 2;
    public static final String MIME_TYPE_JPEG = "image/jpeg";
    public static final String MIME_TYPE_PNG = "image/png";
    public static final Tag TAG = new Tag("LocalData");

    public interface ActionCallback {
        void playVideo(Uri uri, String str);
    }

    public static class NewestFirstComparator implements Comparator<LocalData> {
        private static int compareDate(long v1, long v2) {
            int i = 0;
            if (v1 < 0 || v2 < 0) {
                if (v2 < v1) {
                    i = 1;
                } else if (v2 > v1) {
                    i = -1;
                }
                return i;
            }
            if (v1 < v2) {
                i = 1;
            } else if (v1 > v2) {
                i = -1;
            }
            return i;
        }

        public int compare(LocalData d1, LocalData d2) {
            int cmp = compareDate(d1.getDateTaken(), d2.getDateTaken());
            if (cmp == 0) {
                cmp = compareDate(d1.getDateModified(), d2.getDateModified());
            }
            if (cmp == 0) {
                return d1.getTitle().compareTo(d2.getTitle());
            }
            return cmp;
        }
    }

    boolean canSwipeInFullScreen();

    boolean delete(Context context);

    long getContentId();

    long getDateModified();

    long getDateTaken();

    LocalDataViewType getItemViewType();

    int getLocalDataType();

    MediaDetails getMediaDetails(Context context);

    Bundle getMetadata();

    String getMimeType();

    String getPath();

    String getSignature();

    long getSizeInBytes();

    String getTitle();

    View getView(Context context, View view, int i, int i2, int i3, LocalDataAdapter localDataAdapter, boolean z, ActionCallback actionCallback);

    boolean isDataActionSupported(int i);

    boolean isMetadataUpdated();

    void loadFullImage(Context context, int i, int i2, View view, LocalDataAdapter localDataAdapter);

    void onFullScreen(boolean z);

    LocalData refresh(Context context);
}
