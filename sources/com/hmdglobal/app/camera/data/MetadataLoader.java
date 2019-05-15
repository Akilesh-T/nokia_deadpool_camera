package com.hmdglobal.app.camera.data;

import android.content.Context;

public class MetadataLoader {
    private static final String KEY_METADATA_CACHED = "metadata_cached";

    public static boolean loadMetadata(Context context, LocalData data) {
        boolean metadataAdded = false;
        if (data.getLocalDataType() == 3) {
            PanoramaMetadataLoader.loadPanoramaMetadata(context, data.getUri(), data.getMetadata());
            RgbzMetadataLoader.loadRgbzMetadata(context, data.getUri(), data.getMetadata());
            metadataAdded = true;
        } else if (data.getLocalDataType() == 4) {
            VideoRotationMetadataLoader.loadRotationMetdata(data);
            metadataAdded = true;
        }
        data.getMetadata().putBoolean(KEY_METADATA_CACHED, true);
        return metadataAdded;
    }

    static boolean isMetadataCached(LocalData data) {
        return data.getMetadata().getBoolean(KEY_METADATA_CACHED);
    }
}
