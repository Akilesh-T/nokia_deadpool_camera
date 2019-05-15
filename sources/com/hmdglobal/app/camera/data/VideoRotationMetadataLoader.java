package com.hmdglobal.app.camera.data;

import android.media.MediaMetadataRetriever;
import android.text.TextUtils;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;

public class VideoRotationMetadataLoader {
    private static final String HEIGHT_KEY = "metadata_video_height";
    private static final String ROTATE_270 = "270";
    private static final String ROTATE_90 = "90";
    private static final String ROTATION_KEY = "metadata_video_rotation";
    private static final Tag TAG = new Tag("VidRotDataLoader");
    public static int VIDEO_HEIGHT = 1080;
    public static int VIDEO_WIDTH = 1920;
    private static final String WIDTH_KEY = "metadata_video_width";

    static boolean isRotated(LocalData localData) {
        String rotation = localData.getMetadata().getString(ROTATION_KEY);
        return ROTATE_90.equals(rotation) || ROTATE_270.equals(rotation);
    }

    static int getWidth(LocalData localData) {
        return localData.getMetadata().getInt(WIDTH_KEY);
    }

    static int getHeight(LocalData localData) {
        return localData.getMetadata().getInt(HEIGHT_KEY);
    }

    static void loadRotationMetdata(LocalData data) {
        String path = data.getPath();
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            data.getMetadata().putString(ROTATION_KEY, retriever.extractMetadata(24));
            String val = retriever.extractMetadata(18);
            if (TextUtils.isEmpty(val)) {
                val = String.valueOf(VIDEO_WIDTH);
            }
            data.getMetadata().putInt(WIDTH_KEY, Integer.parseInt(val));
            val = retriever.extractMetadata(19);
            if (TextUtils.isEmpty(val)) {
                val = String.valueOf(VIDEO_HEIGHT);
            }
            data.getMetadata().putInt(HEIGHT_KEY, Integer.parseInt(val));
            try {
                retriever.release();
            } catch (RuntimeException e) {
            }
        } catch (RuntimeException ex) {
            Log.e(TAG, "MediaMetdataRetriever.setDataSource() fail", ex);
            retriever.release();
        } catch (Throwable th) {
            try {
                retriever.release();
            } catch (RuntimeException e2) {
            }
            throw th;
        }
    }
}
