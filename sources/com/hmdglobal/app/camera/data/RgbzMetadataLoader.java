package com.hmdglobal.app.camera.data;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import com.hmdglobal.app.camera.util.RefocusHelper;

public class RgbzMetadataLoader {
    private static final String KEY_RGBZ_INFO = "metadata_key_rgbz_info";

    public static boolean hasRGBZData(LocalData data) {
        return data.getMetadata().getBoolean(KEY_RGBZ_INFO);
    }

    public static void loadRgbzMetadata(Context context, Uri contentUri, Bundle metadata) {
        if (RefocusHelper.isRGBZ(context, contentUri)) {
            metadata.putBoolean(KEY_RGBZ_INFO, true);
        }
    }
}
