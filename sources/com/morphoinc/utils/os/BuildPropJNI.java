package com.morphoinc.utils.os;

import android.util.Log;

public final class BuildPropJNI {
    private static native boolean nativeGetBuildProp(String str);

    static {
        try {
            System.loadLibrary("morpho_buildprop_util");
            Log.d("buildpropJNI", "successfully loaded");
        } catch (UnsatisfiedLinkError e) {
            Log.e("buildpropJNI", e.getMessage());
            Log.e("buildpropJNI", "can't loadLibrary");
        }
    }

    public static boolean getBuildProp(String filename) {
        return nativeGetBuildProp(filename);
    }
}
