package com.hmdglobal.app.camera.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import com.hmdglobal.app.camera.R;

public class GalleryHelper {
    private static final String GALLERY_ACTIVITY_CLASS = "com.android.gallery3d.app.GalleryActivity";
    private static final int GALLERY_APP_NAME_ID = 2131689739;
    private static final String GALLERY_PACKAGE_NAME = "com.android.gallery3d";

    public static void setGalleryIntentClassName(Intent intent) {
        intent.setClassName(GALLERY_PACKAGE_NAME, GALLERY_ACTIVITY_CLASS);
    }

    public static Drawable getGalleryIcon(Context context, Intent galleryIntent) {
        if (galleryIntent != null) {
            try {
                return context.getPackageManager().getActivityIcon(galleryIntent);
            } catch (NameNotFoundException e) {
            }
        }
        return null;
    }

    public static CharSequence getGalleryAppName(Context context, Intent galleryIntent) {
        ComponentName componentName = galleryIntent.getComponent();
        if (componentName != null && GALLERY_PACKAGE_NAME.equals(componentName.getPackageName()) && GALLERY_ACTIVITY_CLASS.equals(componentName.getClassName())) {
            return context.getResources().getString(R.string.gallery_app_name);
        }
        return null;
    }

    public static void setContentUri(Intent intent, Uri uri) {
    }
}
