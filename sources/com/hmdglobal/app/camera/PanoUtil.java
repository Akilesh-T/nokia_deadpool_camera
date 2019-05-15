package com.hmdglobal.app.camera;

import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import com.morphoinc.app.panoramagp3.Camera2ParamsFragment;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PanoUtil {
    public static String createName(String format, long dateTaken) {
        return new SimpleDateFormat(format).format(new Date(dateTaken));
    }

    public static double calculateDifferenceBetweenAngles(double firstAngle, double secondAngle) {
        double difference1 = (secondAngle - firstAngle) % 360.0d;
        if (difference1 < Camera2ParamsFragment.TARGET_EV) {
            difference1 += 360.0d;
        }
        double difference2 = (firstAngle - secondAngle) % 360.0d;
        if (difference2 < Camera2ParamsFragment.TARGET_EV) {
            difference2 += 360.0d;
        }
        return Math.min(difference1, difference2);
    }

    public static void decodeYUV420SPQuarterRes(int[] rgb, byte[] yuv420sp, int width, int height) {
        int i = width;
        int i2 = height;
        int frameSize = i * i2;
        int j = 0;
        int ypd = 0;
        while (j < i2) {
            int uvp = ((j >> 1) * i) + frameSize;
            int u = 0;
            int v = 0;
            int ypd2 = ypd;
            ypd = 0;
            while (ypd < i) {
                int uvp2;
                int y = (yuv420sp[(j * i) + ypd] & 255) - 16;
                if (y < 0) {
                    y = 0;
                }
                if ((ypd & 1) == 0) {
                    uvp2 = uvp + 1;
                    u = (yuv420sp[uvp2] & 255) - 128;
                    int i3 = (uvp2 + 1) + 2;
                    v = (yuv420sp[uvp] & 255) - 128;
                    uvp = i3;
                }
                uvp2 = 1192 * y;
                int r = (1634 * v) + uvp2;
                int g = (uvp2 - (833 * v)) - (400 * u);
                int b = uvp2 + (2066 * u);
                if (r < 0) {
                    r = 0;
                } else if (r > 262143) {
                    r = 262143;
                }
                if (g < 0) {
                    g = 0;
                } else if (g > 262143) {
                    g = 262143;
                }
                if (b < 0) {
                    b = 0;
                } else if (b > 262143) {
                    b = 262143;
                }
                rgb[ypd2] = ((b >> 10) & 255) | ((ViewCompat.MEASURED_STATE_MASK | ((r << 6) & 16711680)) | ((g >> 2) & MotionEventCompat.ACTION_POINTER_INDEX_MASK));
                ypd += 4;
                ypd2++;
                i = width;
            }
            j += 4;
            ypd = ypd2;
            i = width;
        }
    }
}
