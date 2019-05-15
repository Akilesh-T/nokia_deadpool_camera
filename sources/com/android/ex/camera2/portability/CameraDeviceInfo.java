package com.android.ex.camera2.portability;

import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.RectF;
import android.util.Size;
import com.android.ex.camera2.portability.debug.Log;
import com.android.ex.camera2.portability.debug.Log.Tag;
import com.morphoinc.utils.multimedia.MediaProviderUtils;

public interface CameraDeviceInfo {
    public static final int NO_DEVICE = -1;

    public static abstract class Characteristics {
        private static final Tag TAG = new Tag("CamDvcInfChar");

        public abstract boolean canDisableShutterSound();

        public abstract int getSensorOrientation();

        public abstract Size[] getSupportRawSize();

        public abstract int getSupportedHardwareLevel(int i);

        public abstract boolean isFacingBack();

        public abstract boolean isFacingFront();

        public int getPreviewOrientation(int currentDisplayOrientation) {
            return getRelativeImageOrientation(currentDisplayOrientation, true);
        }

        public int getJpegOrientation(int currentDisplayOrientation) {
            return getRelativeImageOrientation(currentDisplayOrientation, false);
        }

        public int getRelativeImageOrientation(int currentDisplayOrientation, boolean compensateForMirroring) {
            if (!orientationIsValid(currentDisplayOrientation)) {
                return 0;
            }
            int result = 0;
            if (isFacingFront()) {
                result = (getSensorOrientation() + currentDisplayOrientation) % 360;
                if (compensateForMirroring) {
                    result = (360 - result) % 360;
                }
            } else if (isFacingBack()) {
                result = ((getSensorOrientation() - currentDisplayOrientation) + 360) % 360;
            } else {
                Log.e(TAG, "Camera is facing unhandled direction");
            }
            return result;
        }

        public Matrix getPreviewTransform(int currentDisplayOrientation, RectF surfaceDimensions) {
            return getPreviewTransform(currentDisplayOrientation, surfaceDimensions, new RectF(surfaceDimensions));
        }

        public Matrix getPreviewTransform(int currentDisplayOrientation, RectF surfaceDimensions, RectF desiredBounds) {
            if (!orientationIsValid(currentDisplayOrientation) || surfaceDimensions.equals(desiredBounds)) {
                return new Matrix();
            }
            Matrix transform = new Matrix();
            transform.setRectToRect(surfaceDimensions, desiredBounds, ScaleToFit.FILL);
            return transform;
        }

        protected static boolean orientationIsValid(int angle) {
            if (angle % 90 != 0) {
                Log.e(TAG, "Provided display orientation is not divisible by 90");
                return false;
            } else if (angle >= 0 && angle <= MediaProviderUtils.ROTATION_270) {
                return true;
            } else {
                Log.e(TAG, "Provided display orientation is outside expected range");
                return false;
            }
        }
    }

    Characteristics getCharacteristics(int i);

    int getFirstBackCameraId();

    int getFirstFrontCameraId();

    int getNumberOfCameras();
}
