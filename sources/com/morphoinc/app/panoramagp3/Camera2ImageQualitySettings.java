package com.morphoinc.app.panoramagp3;

import java.io.Serializable;

class Camera2ImageQualitySettings implements Serializable {
    public static final int COLOR_CORRECTION_MODE = 0;
    public static final int DEFAULT_INDEX_PREVIEW = 0;
    public static final int DEFAULT_INDEX_STILL = 1;
    public static final int DEFAULT_INDEX_ZSL = 2;
    public static final int EDGE_MODE = 1;
    public static final int NOISE_REDUCTION_MODE = 2;
    public static final int NUM = 5;
    public static final int SHADING_MODE = 3;
    public static final int TONEMAP_MODE = 4;
    public String[] defaultValues = null;
    public String[] entries = null;
    public String[] entryValues = null;

    Camera2ImageQualitySettings() {
    }

    public boolean isAvailable() {
        return (this.entries == null || this.entryValues == null || this.defaultValues == null) ? false : true;
    }

    public String getDefaultValue(int capture_mode) {
        String ret = "";
        if (this.defaultValues == null) {
            return ret;
        }
        int i = 1;
        if (capture_mode == 0) {
            i = 0;
        } else if (capture_mode == 2) {
            i = 2;
        }
        if (i < this.defaultValues.length) {
            return this.defaultValues[i];
        }
        return ret;
    }
}
