package com.hmdglobal.app.camera;

import android.content.Context;
import android.content.res.Configuration;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.util.CameraUtil;
import com.hmdglobal.app.camera.util.Size;
import com.morphoinc.utils.multimedia.MediaProviderUtils;
import java.util.ArrayList;

public class CaptureModuleUtil {
    private static final Tag TAG = new Tag("CaptureModuleUtil");

    public static int getDeviceNaturalOrientation(Context context) {
        Configuration config = context.getResources().getConfiguration();
        int rotation = CameraUtil.getDisplayRotation(context);
        return (((rotation == 0 || rotation == 2) && config.orientation == 2) || ((rotation == 1 || rotation == 3) && config.orientation == 1)) ? 2 : 1;
    }

    public static Size getOptimalPreviewSize(Context context, Size[] sizes, double targetRatio) {
        int i = 0;
        int count = 0;
        for (Size s : sizes) {
            if (s.getHeight() <= 1080) {
                count++;
            }
        }
        ArrayList<Size> camera1Sizes = new ArrayList(count);
        for (Size s2 : sizes) {
            if (s2.getHeight() <= 1080) {
                camera1Sizes.add(new Size(s2.getWidth(), s2.getHeight()));
            }
        }
        int optimalPreviewSizeIndex = CameraUtil.getOptimalPreviewSizeIndex(context, camera1Sizes, targetRatio);
        if (optimalPreviewSizeIndex == -1) {
            return null;
        }
        Size optimal = (Size) camera1Sizes.get(optimalPreviewSizeIndex);
        int length = sizes.length;
        while (i < length) {
            Size s3 = sizes[i];
            if (s3.getWidth() == optimal.getWidth() && s3.getHeight() == optimal.getHeight()) {
                return s3;
            }
            i++;
        }
        return null;
    }

    public static Size pickBufferDimensions(Size[] supportedPreviewSizes, double bestPreviewAspectRatio, Context context) {
        boolean z = false;
        boolean swapDimens = CameraUtil.getDisplayRotation(context) % MediaProviderUtils.ROTATION_180 == 90;
        if (getDeviceNaturalOrientation(context) == 1) {
            if (!swapDimens) {
                z = true;
            }
            swapDimens = z;
        }
        double bestAspect = bestPreviewAspectRatio;
        if (swapDimens) {
            bestAspect = 1.0d / bestAspect;
        }
        Size pick = getOptimalPreviewSize(context, supportedPreviewSizes, bestPreviewAspectRatio);
        Tag tag = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Picked buffer size: ");
        stringBuilder.append(pick.toString());
        Log.d(tag, stringBuilder.toString());
        return pick;
    }
}
