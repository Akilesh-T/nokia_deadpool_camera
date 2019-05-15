package com.hmdglobal.app.camera.util;

import android.hardware.camera2.CaptureResult.Key;

public class DepthUtil {
    public static final int DEPTH_HEIGHT_16_9 = 504;
    public static final int DEPTH_HEIGHT_18_9 = 500;
    public static final int DEPTH_HEIGHT_4_3 = 672;
    public static final int DEPTH_WIDTH_16_9 = 896;
    public static final int DEPTH_WIDTH_18_9 = 1000;
    public static final int DEPTH_WIDTH_4_3 = 896;
    public static final Key<Float> bokeh_gdepth_far = new Key("org.codeaurora.qcamera3.bokeh.gdepth_far", Float.class);
    public static final Key<Byte> bokeh_gdepth_format = new Key("org.codeaurora.qcamera3.bokeh.gdepth_format", Byte.class);
    public static final Key<Float> bokeh_gdepth_near = new Key("org.codeaurora.qcamera3.bokeh.gdepth_near", Float.class);
}
