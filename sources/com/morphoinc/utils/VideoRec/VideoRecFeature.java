package com.morphoinc.utils.VideoRec;

import com.morphoinc.utils.os.BuildUtil;

public class VideoRecFeature {
    public static final boolean FEATURE_VIDEO_REC = true;
    public static final boolean enable_rec = false;

    public static boolean isSupportVideoRec() {
        return BuildUtil.isOverJellyBeanMr2();
    }

    public static boolean isEnableVideoRec() {
        isSupportVideoRec();
        return false;
    }
}
