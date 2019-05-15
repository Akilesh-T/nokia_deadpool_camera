package com.hmdglobal.app.camera.util;

import android.location.Location;
import com.hmdglobal.app.camera.exif.ExifInterface;
import com.morphoinc.app.panoramagp3.Camera2ParamsFragment;

public class ExifUtil {
    public static void addLocationToExif(ExifInterface exif, Location location) {
        exif.addGpsTags(location.getLatitude(), location.getLongitude());
        exif.addGpsDateTimeStampTag(location.getTime());
        double altitude = location.getAltitude();
        if (altitude != Camera2ParamsFragment.TARGET_EV) {
            short altitudeRef;
            if (altitude < Camera2ParamsFragment.TARGET_EV) {
                altitudeRef = (short) 1;
            } else {
                altitudeRef = (short) 0;
            }
            exif.setTag(exif.buildTag(ExifInterface.TAG_GPS_ALTITUDE_REF, Short.valueOf(altitudeRef)));
        }
    }
}
