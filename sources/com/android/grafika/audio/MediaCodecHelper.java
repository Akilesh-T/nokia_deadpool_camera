package com.android.grafika.audio;

import android.media.MediaCodec;
import android.media.MediaCodecInfo.CodecProfileLevel;
import android.media.MediaFormat;
import android.util.Log;
import com.hmdglobal.app.camera.provider.InfoTable;
import java.io.IOException;

public class MediaCodecHelper {
    public static MediaCodec createSoftVideoMediaCodec(MediaMakerConfig config, MediaFormat videoFormat) {
        videoFormat.setString("mime", "video/avc");
        videoFormat.setInteger(InfoTable.WIDTH, config.videoWidth);
        videoFormat.setInteger(InfoTable.HEIGHT, config.videoHeight);
        videoFormat.setInteger("bitrate", config.mediacdoecAVCBitRate);
        videoFormat.setInteger("frame-rate", config.mediacodecAVCFrameRate);
        videoFormat.setInteger("i-frame-interval", config.mediacodecAVCIFrameInterval);
        videoFormat.setInteger("profile", 1);
        videoFormat.setInteger("level", 512);
        videoFormat.setInteger("bitrate-mode", 2);
        MediaCodec result = null;
        try {
            result = MediaCodec.createEncoderByType(videoFormat.getString("mime"));
            int[] colorful = result.getCodecInfo().getCapabilitiesForType(videoFormat.getString("mime")).colorFormats;
            int dstVideoColorFormat = -1;
            if (isArrayContain(colorful, 21)) {
                dstVideoColorFormat = 21;
                config.mediacodecAVCColorFormat = 21;
            }
            if (dstVideoColorFormat == -1 && isArrayContain(colorful, 19)) {
                dstVideoColorFormat = 19;
                config.mediacodecAVCColorFormat = 19;
            }
            if (dstVideoColorFormat == -1) {
                Log.e("", "!!!!!!!!!!!UnSupport,mediaCodecColorFormat");
                return null;
            }
            videoFormat.setInteger("color-format", dstVideoColorFormat);
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static MediaCodec createAudioMediaCodec(MediaMakerConfig config, MediaFormat audioFormat) {
        audioFormat.setString("mime", "audio/mp4a-latm");
        audioFormat.setInteger("aac-profile", config.mediacodecAACProfile);
        audioFormat.setInteger("sample-rate", config.mediacodecAACSampleRate);
        audioFormat.setInteger("channel-count", config.mediacodecAACChannelCount);
        audioFormat.setInteger("bitrate", config.mediacodecAACBitRate);
        audioFormat.setInteger("max-input-size", config.mediacodecAACMaxInputSize);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("creatingAudioEncoder,format=");
        stringBuilder.append(audioFormat.toString());
        Log.d("", stringBuilder.toString());
        try {
            return MediaCodec.createEncoderByType(audioFormat.getString("mime"));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static MediaCodec createHardVideoMediaCodec(MediaMakerConfig config, MediaFormat videoFormat) {
        videoFormat.setString("mime", "video/avc");
        videoFormat.setInteger(InfoTable.WIDTH, config.videoWidth);
        videoFormat.setInteger(InfoTable.HEIGHT, config.videoHeight);
        videoFormat.setInteger("color-format", 2130708361);
        videoFormat.setInteger("bitrate", config.mediacdoecAVCBitRate);
        videoFormat.setInteger("frame-rate", config.mediacodecAVCFrameRate);
        videoFormat.setInteger("i-frame-interval", config.mediacodecAVCIFrameInterval);
        videoFormat.setInteger("profile", 1);
        videoFormat.setInteger("level", 512);
        videoFormat.setInteger("bitrate-mode", 2);
        MediaCodec result = null;
        try {
            return MediaCodec.createEncoderByType(videoFormat.getString("mime"));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static boolean isArrayContain(int[] src, int target) {
        for (int color : src) {
            if (color == target) {
                return true;
            }
        }
        return false;
    }

    private static boolean isProfileContain(CodecProfileLevel[] src, int target) {
        for (CodecProfileLevel color : src) {
            if (color.profile == target) {
                return true;
            }
        }
        return false;
    }
}
