package com.morphoinc.app.panoramagp3;

import com.morphoinc.utils.multimedia.MediaProviderUtils;
import java.util.ArrayList;

public class MathUtil {
    private static final double EPSILON = 1.0E-8d;
    private static final double NS2S = 9.999999717180685E-10d;
    private static final double[] correct_mat_000 = new double[]{Camera2ParamsFragment.TARGET_EV, 1.0d, Camera2ParamsFragment.TARGET_EV, -1.0d, Camera2ParamsFragment.TARGET_EV, Camera2ParamsFragment.TARGET_EV, Camera2ParamsFragment.TARGET_EV, Camera2ParamsFragment.TARGET_EV, 1.0d};
    private static final double[] correct_mat_090 = new double[]{1.0d, Camera2ParamsFragment.TARGET_EV, Camera2ParamsFragment.TARGET_EV, Camera2ParamsFragment.TARGET_EV, 1.0d, Camera2ParamsFragment.TARGET_EV, Camera2ParamsFragment.TARGET_EV, Camera2ParamsFragment.TARGET_EV, 1.0d};
    private static final double[] correct_mat_180 = new double[]{Camera2ParamsFragment.TARGET_EV, 1.0d, Camera2ParamsFragment.TARGET_EV, 1.0d, Camera2ParamsFragment.TARGET_EV, Camera2ParamsFragment.TARGET_EV, Camera2ParamsFragment.TARGET_EV, Camera2ParamsFragment.TARGET_EV, 1.0d};
    private static final double[] correct_mat_270 = new double[]{-1.0d, Camera2ParamsFragment.TARGET_EV, Camera2ParamsFragment.TARGET_EV, Camera2ParamsFragment.TARGET_EV, -1.0d, Camera2ParamsFragment.TARGET_EV, Camera2ParamsFragment.TARGET_EV, Camera2ParamsFragment.TARGET_EV, 1.0d};

    public static void getRotationMatrix(double[] out_mat, double x, double y, double z) {
        double sinx = Math.sin(x);
        double cosx = Math.cos(x);
        double[] x_rmat = new double[]{1.0d, Camera2ParamsFragment.TARGET_EV, Camera2ParamsFragment.TARGET_EV, Camera2ParamsFragment.TARGET_EV, cosx, -sinx, Camera2ParamsFragment.TARGET_EV, sinx, cosx};
        double siny = Math.sin(y);
        double cosy = Math.cos(y);
        double[] y_rmat = new double[]{cosy, Camera2ParamsFragment.TARGET_EV, siny, Camera2ParamsFragment.TARGET_EV, 1.0d, Camera2ParamsFragment.TARGET_EV, -siny, Camera2ParamsFragment.TARGET_EV, cosy};
        double sinz = Math.sin(z);
        double cosz = Math.cos(z);
        r4 = new double[9];
        r4[1] = -sinz;
        r4[2] = Camera2ParamsFragment.TARGET_EV;
        r4[3] = sinz;
        r4[4] = cosz;
        r4[5] = Camera2ParamsFragment.TARGET_EV;
        r4[6] = Camera2ParamsFragment.TARGET_EV;
        r4[7] = Camera2ParamsFragment.TARGET_EV;
        r4[8] = 1.0d;
        double[] z_rmat = r4;
        mulMatrix3x3(y_rmat, y_rmat, x_rmat);
        mulMatrix3x3(out_mat, z_rmat, y_rmat);
    }

    public static void convMatrix16to9(double[] dst, double[] src) {
        if (src.length == 16 || dst.length == 9) {
            dst[0] = src[0];
            dst[1] = src[1];
            dst[2] = src[2];
            dst[3] = src[4];
            dst[4] = src[5];
            dst[5] = src[6];
            dst[6] = src[8];
            dst[7] = src[9];
            dst[8] = src[10];
        }
    }

    public static void getAngleDiff(double[] angle, double[] mat, double[] prev_mat) {
        double[] dArr = angle;
        double[] dArr2 = mat;
        double[] dArr3 = prev_mat;
        if (dArr.length == 3 && dArr2.length == 9 && dArr3.length == 9) {
            double rd7 = ((dArr3[2] * dArr2[1]) + (dArr3[5] * dArr2[4])) + (dArr3[8] * dArr2[7]);
            double rd6 = ((dArr3[2] * dArr2[0]) + (dArr3[5] * dArr2[3])) + (dArr3[8] * dArr2[6]);
            double rd8 = ((dArr3[2] * dArr2[2]) + (dArr3[5] * dArr2[5])) + (dArr3[8] * dArr2[8]);
            dArr[0] = Math.atan2(((dArr3[0] * dArr2[1]) + (dArr3[3] * dArr2[4])) + (dArr3[6] * dArr2[7]), ((dArr3[1] * dArr2[1]) + (dArr3[4] * dArr2[4])) + (dArr3[7] * dArr2[7]));
            dArr[1] = Math.asin(-rd7);
            dArr[2] = Math.atan2(-rd6, rd8);
        }
    }

    public static void getDeltaRotationVector(double[] dst_vector, double[] values, double diff_time) {
        double axisZ;
        double dT = NS2S * diff_time;
        double axisX = values[0];
        double axisY = values[1];
        double axisZ2 = values[2];
        double axisZ3 = axisZ2;
        double omegaMagnitude = Math.sqrt(((axisX * axisX) + (axisY * axisY)) + (axisZ2 * axisZ2));
        if (omegaMagnitude > EPSILON) {
            axisX /= omegaMagnitude;
            axisY /= omegaMagnitude;
            axisZ = axisZ3 / omegaMagnitude;
        } else {
            axisZ = axisZ3;
        }
        double axisY2 = axisY;
        double thetaOverTwo = (omegaMagnitude * dT) / 2.0d;
        double sinThetaOverTwo = Math.sin(thetaOverTwo);
        axisZ3 = Math.cos(thetaOverTwo);
        dst_vector[0] = sinThetaOverTwo * axisX;
        dst_vector[1] = sinThetaOverTwo * axisY2;
        dst_vector[2] = sinThetaOverTwo * axisZ;
        dst_vector[3] = axisZ3;
    }

    public static void rotateMatrix(double[] in_mat, double[] out_mat, int rotate) {
        if (rotate == 0) {
            mulMatrix3x3(out_mat, in_mat, correct_mat_000);
        } else if (rotate == 90) {
            mulMatrix3x3(out_mat, in_mat, correct_mat_090);
        } else if (rotate == MediaProviderUtils.ROTATION_180) {
            mulMatrix3x3(out_mat, in_mat, correct_mat_180);
        } else if (rotate == MediaProviderUtils.ROTATION_270) {
            mulMatrix3x3(out_mat, in_mat, correct_mat_270);
        }
    }

    public static void mulMatrix3x3(double[] dst_mat, double[] in_m1, double[] in_m2) {
        double[] matrix = new double[9];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                double sum = Camera2ParamsFragment.TARGET_EV;
                for (int k = 0; k < 3; k++) {
                    sum += in_m1[(i * 3) + k] * in_m2[(k * 3) + j];
                }
                matrix[(i * 3) + j] = sum;
            }
        }
        System.arraycopy(matrix, 0, dst_mat, 0, matrix.length);
    }

    public static double radianToDegree(double rad) {
        return Math.toDegrees(rad);
    }

    public static boolean getAverage(double[] dst_value, ArrayList<double[]> src_value_list) {
        if (dst_value == null || src_value_list == null) {
            return false;
        }
        int i;
        double[] total = new double[dst_value.length];
        int size = src_value_list.size();
        for (i = 0; i < size; i++) {
            double[] value = (double[]) src_value_list.get(i);
            for (int j = 0; j < dst_value.length; j++) {
                total[j] = total[j] + value[j];
            }
        }
        if (size > 0) {
            for (i = 0; i < dst_value.length; i++) {
                dst_value[i] = total[i] / ((double) size);
            }
        }
        return true;
    }
}
