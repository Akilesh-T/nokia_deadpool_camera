package com.hmdglobal.app.camera.util;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.hmdglobal.app.camera.CameraModule;
import com.hmdglobal.app.camera.app.AppController;

public class PhotoSphereHelper {
    public static final PanoramaMetadata NOT_PANORAMA = new PanoramaMetadata(false, false);

    public static class PanoramaMetadata {
        public final boolean mIsPanorama360;
        public final boolean mUsePanoramaViewer;

        public PanoramaMetadata(boolean usePanoramaViewer, boolean isPanorama360) {
            this.mUsePanoramaViewer = usePanoramaViewer;
            this.mIsPanorama360 = isPanorama360;
        }
    }

    public static class PanoramaViewHelper {
        public PanoramaViewHelper(Activity activity) {
        }

        public void onStart() {
        }

        public void onCreate() {
        }

        public void onResume() {
        }

        public void onPause() {
        }

        public void onStop() {
        }

        public Intent showPanorama(Activity activity, Uri uri) {
            return null;
        }

        public void showRgbz(Uri uri) {
        }
    }

    public static boolean hasLightCycleCapture(Context context) {
        return false;
    }

    public static PanoramaMetadata getPanoramaMetadata(Context context, Uri uri) {
        return NOT_PANORAMA;
    }

    public static CameraModule createPanoramaModule(AppController app) {
        return null;
    }

    public static CameraModule createWideAnglePanoramaModule(AppController app) {
        return null;
    }

    public static String getPathFromURI(ContentResolver contentResolver, Uri contentUri) {
        return null;
    }

    public static long getModifiedTimeFromURI(ContentResolver contentResolver, Uri contentUri) {
        return 0;
    }

    public static int getPanoramaHorizontalDrawableId() {
        return 0;
    }

    public static int getPanoramaVerticalDrawableId() {
        return 0;
    }

    public static int getPanoramaOrientationOptionArrayId() {
        return 0;
    }

    public static int getPanoramaOrientationDescriptions() {
        return 0;
    }

    public static int getPanoramaOrientationIndicatorArrayId() {
        return 0;
    }
}
