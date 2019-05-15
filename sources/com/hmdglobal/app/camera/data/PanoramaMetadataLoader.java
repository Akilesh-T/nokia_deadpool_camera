package com.hmdglobal.app.camera.data;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import com.hmdglobal.app.camera.util.PhotoSphereHelper;
import com.hmdglobal.app.camera.util.PhotoSphereHelper.PanoramaMetadata;

public class PanoramaMetadataLoader {
    private static final String KEY_IS_PANORAMA = "metadata_key_is_panorama";
    private static final String KEY_PANORAMA_360 = "metadata_key_panorama_360";
    private static final String KEY_USE_PANORAMA_VIEWER = "metadata_key_panorama_viewer";

    public static boolean isPanorama(LocalData data) {
        return data.getMetadata().getBoolean(KEY_IS_PANORAMA);
    }

    public static boolean isPanoramaAndUseViewer(LocalData data) {
        return data.getMetadata().getBoolean(KEY_USE_PANORAMA_VIEWER);
    }

    public static boolean isPanorama360(LocalData data) {
        return data.getMetadata().getBoolean(KEY_PANORAMA_360);
    }

    public static void loadPanoramaMetadata(Context context, Uri contentUri, Bundle metadata) {
        PanoramaMetadata panoramaMetadata = PhotoSphereHelper.getPanoramaMetadata(context, contentUri);
        if (panoramaMetadata != null) {
            metadata.putBoolean(KEY_IS_PANORAMA, panoramaMetadata != PhotoSphereHelper.NOT_PANORAMA);
            metadata.putBoolean(KEY_PANORAMA_360, panoramaMetadata.mIsPanorama360);
            metadata.putBoolean(KEY_USE_PANORAMA_VIEWER, panoramaMetadata.mUsePanoramaViewer);
        }
    }
}
