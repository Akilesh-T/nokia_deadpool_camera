package com.hmdglobal.app.camera.filmstrip;

import android.net.Uri;
import android.view.View;

public interface ImageData {
    public static final int ACTION_DEMOTE = 2;
    public static final int ACTION_NONE = 0;
    public static final int ACTION_PROMOTE = 1;
    public static final int ACTION_ZOOM = 4;
    public static final int SIZE_FULL = -2;
    public static final int VIEW_TYPE_NONE = 0;
    public static final int VIEW_TYPE_REMOVABLE = 2;
    public static final int VIEW_TYPE_STICKY = 1;

    int getHeight();

    double[] getLatLong();

    int getRotation();

    Uri getUri();

    int getViewType();

    int getWidth();

    boolean isUIActionSupported(int i);

    void prepare();

    void recycle(View view);
}
