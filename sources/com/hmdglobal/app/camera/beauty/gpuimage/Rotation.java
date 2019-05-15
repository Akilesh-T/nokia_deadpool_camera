package com.hmdglobal.app.camera.beauty.gpuimage;

import com.morphoinc.utils.multimedia.MediaProviderUtils;

public enum Rotation {
    NORMAL,
    ROTATION_90,
    ROTATION_180,
    ROTATION_270;

    public int asInt() {
        switch (this) {
            case NORMAL:
                return 0;
            case ROTATION_90:
                return 90;
            case ROTATION_180:
                return MediaProviderUtils.ROTATION_180;
            case ROTATION_270:
                return MediaProviderUtils.ROTATION_270;
            default:
                throw new IllegalStateException("Unknown Rotation!");
        }
    }

    public static Rotation fromInt(int rotation) {
        if (rotation == 0) {
            return NORMAL;
        }
        if (rotation == 90) {
            return ROTATION_90;
        }
        if (rotation == MediaProviderUtils.ROTATION_180) {
            return ROTATION_180;
        }
        if (rotation == MediaProviderUtils.ROTATION_270) {
            return ROTATION_270;
        }
        if (rotation == 360) {
            return NORMAL;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(rotation);
        stringBuilder.append(" is an unknown rotation. Needs to be either 0, 90, 180 or 270!");
        throw new IllegalStateException(stringBuilder.toString());
    }
}
