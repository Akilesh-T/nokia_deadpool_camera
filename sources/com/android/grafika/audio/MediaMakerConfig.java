package com.android.grafika.audio;

import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class MediaMakerConfig {
    public static final int FLAG_DIRECTION_FLIP_HORIZONTAL = 1;
    public static final int FLAG_DIRECTION_FLIP_VERTICAL = 2;
    public static final int FLAG_DIRECTION_ROATATION_0 = 16;
    public static final int FLAG_DIRECTION_ROATATION_180 = 64;
    public static final int FLAG_DIRECTION_ROATATION_270 = 128;
    public static final int FLAG_DIRECTION_ROATATION_90 = 32;
    public static final int RENDERING_MODE_OPENGLES = 2;
    public int audioBufferQueueNum = -1;
    public int audioRecoderBufferSize;
    public int audioRecoderChannelConfig;
    public int audioRecoderFormat;
    public int audioRecoderSampleRate;
    public int audioRecoderSliceSize;
    public int audioRecoderSource;
    public int backCameraDirectionMode;
    public float cropRatio;
    public boolean done = false;
    public int frontCameraDirectionMode;
    public boolean isFaceDetectEnable = false;
    public boolean isPortrait;
    public boolean isSquare = false;
    public int mediacdoecAVCBitRate = -1;
    public int mediacodecAACBitRate = -1;
    public int mediacodecAACChannelCount = -1;
    public int mediacodecAACMaxInputSize = -1;
    public int mediacodecAACProfile = -1;
    public int mediacodecAACSampleRate = -1;
    public int mediacodecAVCColorFormat = -1;
    public int mediacodecAVCFrameRate = -1;
    public int mediacodecAVCIFrameInterval = -1;
    public int mediacodecAVCProfile = -1;
    public int mediacodecAVClevel = -1;
    public int previewBufferSize;
    public int previewColorFormat = -1;
    public int previewMaxFps;
    public int previewMinFps;
    public int previewVideoHeight;
    public int previewVideoWidth;
    public boolean printDetailMsg = false;
    public int renderingMode;
    public boolean saveVideoEnable = false;
    public String saveVideoPath;
    public int videoBufferQueueNum = -1;
    public int videoFPS = -1;
    public int videoGOP = 1;
    public int videoHeight = -1;
    public int videoWidth = -1;

    public void dump() {
        Log.e("", toString());
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ResParameter:");
        for (Field field : getClass().getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                try {
                    sb.append(field.getName());
                    sb.append('=');
                    sb.append(field.get(this));
                    sb.append(';');
                } catch (IllegalAccessException e) {
                }
            }
        }
        return sb.toString();
    }
}
