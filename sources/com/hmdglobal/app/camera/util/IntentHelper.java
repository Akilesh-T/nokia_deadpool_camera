package com.hmdglobal.app.camera.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import com.hmdglobal.app.camera.debug.Log.Tag;

public class IntentHelper {
    private static final Tag TAG = new Tag("IntentHelper");

    public static Intent getGalleryIntent(Context context) {
        Intent intent = new Intent("android.intent.action.MAIN");
        GalleryHelper.setGalleryIntentClassName(intent);
        if (context.getPackageManager().queryIntentActivities(intent, 65536).size() == 0) {
            return null;
        }
        return intent;
    }

    public static Drawable getGalleryIcon(Context context, Intent galleryIntent) {
        return GalleryHelper.getGalleryIcon(context, galleryIntent);
    }

    public static CharSequence getGalleryAppName(Context context, Intent galleryIntent) {
        return GalleryHelper.getGalleryAppName(context, galleryIntent);
    }

    public static Intent getVideoPlayerIntent(Uri uri) {
        return new Intent("android.intent.action.VIEW").setDataAndType(uri, "video/*");
    }
}
