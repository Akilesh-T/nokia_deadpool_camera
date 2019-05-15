package com.hmdglobal.app.camera.one.v2;

import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.support.v4.os.EnvironmentCompat;
import com.hmdglobal.app.camera.debug.Log;
import com.hmdglobal.app.camera.debug.Log.Tag;
import com.hmdglobal.app.camera.one.OneCamera.AutoFocusState;
import com.hmdglobal.app.camera.one.Settings3A;
import com.hmdglobal.app.camera.util.CameraUtil;

public class AutoFocusHelper {
    private static final int CAMERA2_REGION_WEIGHT = ((int) CameraUtil.lerp(0.0f, 1000.0f, Settings3A.getMeteringRegionWeight()));
    private static final Tag TAG = new Tag("OneCameraAFHelp");
    private static final MeteringRectangle[] ZERO_WEIGHT_3A_REGION = new MeteringRectangle[]{new MeteringRectangle(0, 0, 0, 0, 0)};

    public static MeteringRectangle[] getZeroWeightRegion() {
        return ZERO_WEIGHT_3A_REGION;
    }

    public static AutoFocusState stateFromCamera2State(int state) {
        switch (state) {
            case 1:
                return AutoFocusState.PASSIVE_SCAN;
            case 2:
                return AutoFocusState.PASSIVE_FOCUSED;
            case 3:
                return AutoFocusState.ACTIVE_SCAN;
            case 4:
                return AutoFocusState.ACTIVE_FOCUSED;
            case 5:
                return AutoFocusState.ACTIVE_UNFOCUSED;
            case 6:
                return AutoFocusState.PASSIVE_UNFOCUSED;
            default:
                return AutoFocusState.INACTIVE;
        }
    }

    public static boolean checkControlAfState(CaptureResult result) {
        boolean missing = result.get(CaptureResult.CONTROL_AF_STATE) == null;
        if (missing) {
            Log.e(TAG, "\n!!!! TotalCaptureResult missing CONTROL_AF_STATE. !!!!\n ");
        }
        if (missing) {
            return false;
        }
        return true;
    }

    public static boolean checkLensState(CaptureResult result) {
        boolean missing = result.get(CaptureResult.LENS_STATE) == null;
        if (missing) {
            Log.e(TAG, "\n!!!! TotalCaptureResult missing LENS_STATE. !!!!\n ");
        }
        if (missing) {
            return false;
        }
        return true;
    }

    public static void logExtraFocusInfo(CaptureResult result) {
        if (checkControlAfState(result) && checkLensState(result)) {
            String str;
            Object tag = result.getRequest().getTag();
            Tag tag2 = TAG;
            String str2 = "af_state:%-17s  lens_foc_dist:%.3f  lens_state:%-10s  %s";
            Object[] objArr = new Object[4];
            objArr[0] = controlAFStateToString(((Integer) result.get(CaptureResult.CONTROL_AF_STATE)).intValue());
            objArr[1] = result.get(CaptureResult.LENS_FOCUS_DISTANCE);
            objArr[2] = lensStateToString(((Integer) result.get(CaptureResult.LENS_STATE)).intValue());
            if (tag == null) {
                str = "";
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[");
                stringBuilder.append(tag);
                stringBuilder.append("]");
                str = stringBuilder.toString();
            }
            objArr[3] = str;
            Log.v(tag2, String.format(str2, objArr));
        }
    }

    private static MeteringRectangle[] regionsForNormalizedCoord(float nx, float ny, float fraction, Rect cropRegion, int sensorOrientation) {
        int halfSideLength = (int) ((0.5f * fraction) * ((float) Math.min(cropRegion.width(), cropRegion.height())));
        PointF nsc = CameraUtil.normalizedSensorCoordsForNormalizedDisplayCoords(nx, ny, sensorOrientation);
        int xCenterSensor = (int) (((float) cropRegion.left) + (nsc.x * ((float) cropRegion.width())));
        int yCenterSensor = (int) (((float) cropRegion.top) + (nsc.y * ((float) cropRegion.height())));
        Rect meteringRegion = new Rect(xCenterSensor - halfSideLength, yCenterSensor - halfSideLength, xCenterSensor + halfSideLength, yCenterSensor + halfSideLength);
        meteringRegion.left = CameraUtil.clamp(meteringRegion.left, cropRegion.left, cropRegion.right);
        meteringRegion.top = CameraUtil.clamp(meteringRegion.top, cropRegion.top, cropRegion.bottom);
        meteringRegion.right = CameraUtil.clamp(meteringRegion.right, cropRegion.left, cropRegion.right);
        meteringRegion.bottom = CameraUtil.clamp(meteringRegion.bottom, cropRegion.top, cropRegion.bottom);
        return new MeteringRectangle[]{new MeteringRectangle(meteringRegion, CAMERA2_REGION_WEIGHT)};
    }

    public static MeteringRectangle[] afRegionsForNormalizedCoord(float nx, float ny, Rect cropRegion, int sensorOrientation) {
        return regionsForNormalizedCoord(nx, ny, Settings3A.getAutoFocusRegionWidth(), cropRegion, sensorOrientation);
    }

    public static MeteringRectangle[] aeRegionsForNormalizedCoord(float nx, float ny, Rect cropRegion, int sensorOrientation) {
        return regionsForNormalizedCoord(nx, ny, Settings3A.getMeteringRegionWidth(), cropRegion, sensorOrientation);
    }

    public static MeteringRectangle[] gcamAERegionsForNormalizedCoord(float nx, float ny, Rect cropRegion, int sensorOrientation) {
        return regionsForNormalizedCoord(nx, ny, Settings3A.getGcamMeteringRegionFraction(), cropRegion, sensorOrientation);
    }

    public static Rect cropRegionForZoom(CameraCharacteristics characteristics, float zoom) {
        Rect sensor = (Rect) characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        int xCenter = sensor.width() / 2;
        int yCenter = sensor.height() / 2;
        int xDelta = (int) ((((float) sensor.width()) * 0.5f) / zoom);
        int yDelta = (int) ((0.5f * ((float) sensor.height())) / zoom);
        return new Rect(xCenter - xDelta, yCenter - yDelta, xCenter + xDelta, yCenter + yDelta);
    }

    private static String controlAFStateToString(int controlAFState) {
        switch (controlAFState) {
            case 0:
                return "inactive";
            case 1:
                return "passive_scan";
            case 2:
                return "passive_focused";
            case 3:
                return "active_scan";
            case 4:
                return "focus_locked";
            case 5:
                return "not_focus_locked";
            case 6:
                return "passive_unfocused";
            default:
                return EnvironmentCompat.MEDIA_UNKNOWN;
        }
    }

    private static String lensStateToString(int lensState) {
        switch (lensState) {
            case 0:
                return "stationary";
            case 1:
                return "moving";
            default:
                return EnvironmentCompat.MEDIA_UNKNOWN;
        }
    }
}
