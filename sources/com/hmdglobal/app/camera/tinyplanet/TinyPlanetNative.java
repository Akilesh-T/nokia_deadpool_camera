package com.hmdglobal.app.camera.tinyplanet;

import android.graphics.Bitmap;

public class TinyPlanetNative {
    public static native void process(Bitmap bitmap, int i, int i2, Bitmap bitmap2, int i3, float f, float f2);

    static {
        System.loadLibrary("jni_tinyplanet");
    }
}
