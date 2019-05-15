package com.morphoinc.app.panoramagp3;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import com.morphoinc.app.LogFilter;
import com.morphoinc.core.Error;
import com.morphoinc.core.MorphoSensorFusion;
import com.morphoinc.core.MorphoSensorFusion.SensorData;
import java.util.ArrayList;
import java.util.Locale;

class SensorFusion implements SensorEventListener {
    private static final String LOG_TAG = "SensorFusion";
    private static final int MAX_DATA_NUM = 512;
    public static final int MODE_USE_ACCELEROMETER_AND_MAGNETIC_FIELD = 3;
    public static final int MODE_USE_ALL_SENSORS = 0;
    public static final int MODE_USE_GYROSCOPE = 1;
    public static final int MODE_USE_GYROSCOPE_AND_ROTATION_VECTOR = 4;
    public static final int MODE_USE_GYROSCOPE_WITH_ACCELEROMETER = 2;
    public static final int OFFSET_MODE_DYNAMIC = 1;
    public static final int OFFSET_MODE_STATIC = 0;
    public static final int ROTATE_0 = 0;
    public static final int ROTATE_180 = 2;
    public static final int ROTATE_270 = 3;
    public static final int ROTATE_90 = 1;
    public static final int SENSOR_TYPE_ACCELEROMETER = 1;
    public static final int SENSOR_TYPE_GYROSCOPE = 0;
    public static final int SENSOR_TYPE_MAGNETIC_FIELD = 2;
    public static final int SENSOR_TYPE_NUM = 4;
    public static final int SENSOR_TYPE_ROTATION_VECTOR = 3;
    public static final int STATE_CALC_OFFSET = 0;
    public static final int STATE_PROCESS = 1;
    public static final Object SensorSynchronizedObject = new Object();
    private int camera_rotation = 1;
    private ArrayList<ArrayList<SensorData>> mAllValueList;
    private boolean mIsCalibrated;
    private int mMode;
    private MorphoSensorFusion mMorphoSensorFusion;
    private final ArrayList<SensorData> mPartOfAccelerometerList = new ArrayList();
    private final ArrayList<SensorData> mPartOfGyroscopeList = new ArrayList();
    private final ArrayList<SensorData> mPartOfGyroscopeUncalibratedList = new ArrayList();
    private final ArrayList<SensorData> mPartOfMagneticFieldList = new ArrayList();
    private final ArrayList<SensorData> mPartOfRotationVectorList = new ArrayList();
    private final double[][] mSensorMatrix;
    private final boolean mStock;

    public SensorFusion(boolean stock_sensor_data) {
        int i;
        this.mStock = stock_sensor_data;
        if (this.mStock) {
            this.mAllValueList = new ArrayList();
            for (i = 0; i < 4; i++) {
                this.mAllValueList.add(new ArrayList());
            }
        }
        this.mSensorMatrix = new double[4][];
        for (i = 0; i < this.mSensorMatrix.length; i++) {
            this.mSensorMatrix[i] = createMatrix();
        }
        this.mMorphoSensorFusion = new MorphoSensorFusion();
        if (this.mMorphoSensorFusion.initialize() != 0) {
            LogFilter.e(LOG_TAG, String.format(Locale.US, "MorphoSensorFusion.initialize error ret:0x%08X", new Object[]{Integer.valueOf(ret)}));
        }
    }

    public void release() {
        synchronized (this) {
            if (this.mMorphoSensorFusion.finish() != 0) {
                LogFilter.e(LOG_TAG, String.format(Locale.US, "MorphoSensorFusion.finish error ret:0x%08X", new Object[]{Integer.valueOf(ret)}));
            }
            this.mMorphoSensorFusion = null;
        }
    }

    public int setMode(int mode) {
        int ret;
        synchronized (this) {
            this.mMode = mode;
            ret = 0 | this.mMorphoSensorFusion.setMode(mode);
        }
        return ret;
    }

    public void setCalibrated(boolean is_calibrated) {
        synchronized (this) {
            this.mIsCalibrated = is_calibrated;
        }
    }

    public int setOffsetMode(int offset_mode) {
        int ret;
        synchronized (this) {
            ret = 0 | this.mMorphoSensorFusion.setOffsetMode(offset_mode);
        }
        return ret;
    }

    public int setOffset(SensorData sd, int sensor_type) {
        int ret;
        synchronized (this) {
            if (this.mMode == 4) {
                ret = 0 | this.mMorphoSensorFusion.setOffset(sd, sensor_type);
            } else {
                ret = Error.ERROR_STATE;
            }
        }
        return ret;
    }

    public int setAppState(int state) {
        int ret;
        synchronized (this) {
            ret = 0 | this.mMorphoSensorFusion.setAppState(state);
        }
        return ret;
    }

    public int setRotation(int rotation) {
        int ret;
        this.camera_rotation = rotation;
        synchronized (this) {
            ret = 0 | this.mMorphoSensorFusion.setRotation(rotation);
        }
        return ret;
    }

    public void setInitialOrientation(int orientation) {
        double z_rot = Math.toRadians((double) orientation);
        double d = z_rot;
        calcRotationMatrix(this.mSensorMatrix[0], Camera2ParamsFragment.TARGET_EV, Camera2ParamsFragment.TARGET_EV, d);
        calcRotationMatrix(this.mSensorMatrix[3], Camera2ParamsFragment.TARGET_EV, Camera2ParamsFragment.TARGET_EV, d);
        calcRotationMatrix(this.mSensorMatrix[1], Camera2ParamsFragment.TARGET_EV, Camera2ParamsFragment.TARGET_EV, d);
    }

    public void resetOffsetValue() {
        synchronized (this) {
            this.mMorphoSensorFusion.setAppState(1);
            this.mMorphoSensorFusion.calc();
        }
    }

    public int getSensorMatrix(double[] gyro_mat, double[] rv_mat, double[] acc_mat, int[] sensor_ix) {
        int ret = 0;
        synchronized (this) {
            if (isUpdateSensorMatrix()) {
                ret = 0 | updateSensorMatrix();
            }
            int i = 0;
            if (gyro_mat != null) {
                System.arraycopy(this.mSensorMatrix[0], 0, gyro_mat, 0, this.mSensorMatrix[0].length);
            }
            if (rv_mat != null) {
                System.arraycopy(this.mSensorMatrix[3], 0, rv_mat, 0, this.mSensorMatrix[3].length);
            }
            if (acc_mat != null) {
                System.arraycopy(this.mSensorMatrix[1], 0, acc_mat, 0, this.mSensorMatrix[1].length);
            }
            if (this.mStock && sensor_ix != null && sensor_ix.length == this.mAllValueList.size()) {
                while (i < this.mAllValueList.size()) {
                    sensor_ix[i] = ((ArrayList) this.mAllValueList.get(i)).size() - 1;
                    i++;
                }
            }
        }
        return ret;
    }

    public ArrayList<ArrayList<SensorData>> getStockData() {
        if (!this.mStock) {
            return new ArrayList();
        }
        ArrayList<ArrayList<SensorData>> all_data;
        synchronized (this) {
            all_data = this.mAllValueList;
        }
        return all_data;
    }

    public void clearStockData() {
        synchronized (this) {
            if (this.mStock) {
                for (int i = 0; i < this.mAllValueList.size(); i++) {
                    ((ArrayList) this.mAllValueList.get(i)).clear();
                }
            }
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        synchronized (SensorSynchronizedObject) {
            SensorData sd;
            int type = event.sensor.getType();
            if (type == 16) {
                float[] uncalibrated_val = new float[3];
                for (int i = 0; i < 3; i++) {
                    uncalibrated_val[i] = event.values[i];
                }
                sd = new SensorData(event.timestamp, uncalibrated_val);
            } else {
                sd = new SensorData(event.timestamp, event.values);
            }
            switch (type) {
                case 1:
                    this.mPartOfAccelerometerList.add(sd);
                    break;
                case 2:
                    this.mPartOfMagneticFieldList.add(sd);
                    break;
                case 4:
                    if (this.camera_rotation == 3) {
                        sd.mValues[0] = -sd.mValues[0];
                        sd.mValues[1] = -sd.mValues[1];
                    }
                    this.mPartOfGyroscopeList.add(sd);
                    break;
                case 15:
                    this.mPartOfRotationVectorList.add(sd);
                    break;
                case 16:
                    if (this.camera_rotation == 3) {
                        sd.mValues[0] = -sd.mValues[0];
                        sd.mValues[1] = -sd.mValues[1];
                    }
                    this.mPartOfGyroscopeUncalibratedList.add(sd);
                    break;
                default:
                    break;
            }
            while (this.mPartOfGyroscopeList.size() > 512) {
                this.mPartOfGyroscopeList.remove(0);
            }
            while (this.mPartOfGyroscopeUncalibratedList.size() > 512) {
                this.mPartOfGyroscopeUncalibratedList.remove(0);
            }
            while (this.mPartOfAccelerometerList.size() > 512) {
                this.mPartOfAccelerometerList.remove(0);
            }
            while (this.mPartOfMagneticFieldList.size() > 512) {
                this.mPartOfMagneticFieldList.remove(0);
            }
            while (this.mPartOfRotationVectorList.size() > 512) {
                this.mPartOfRotationVectorList.remove(0);
            }
        }
    }

    private boolean isUpdateSensorMatrix() {
        synchronized (SensorSynchronizedObject) {
            int isEmpty;
            switch (this.mMode) {
                case 0:
                    isEmpty = ((this.mPartOfGyroscopeList.isEmpty() ^ 1) & (this.mPartOfAccelerometerList.isEmpty() ^ 1)) & (this.mPartOfMagneticFieldList.isEmpty() ^ 1);
                    return isEmpty;
                case 1:
                    if (this.mIsCalibrated) {
                        isEmpty = this.mPartOfGyroscopeList.isEmpty() ^ 1;
                        return isEmpty;
                    }
                    isEmpty = this.mPartOfGyroscopeUncalibratedList.isEmpty() ^ 1;
                    return isEmpty;
                case 2:
                    if (this.mIsCalibrated) {
                        isEmpty = (this.mPartOfGyroscopeList.isEmpty() ^ 1) & (this.mPartOfAccelerometerList.isEmpty() ^ 1);
                        return isEmpty;
                    }
                    isEmpty = (this.mPartOfGyroscopeUncalibratedList.isEmpty() ^ 1) & (this.mPartOfAccelerometerList.isEmpty() ^ 1);
                    return isEmpty;
                case 3:
                    isEmpty = (this.mPartOfAccelerometerList.isEmpty() ^ 1) & (this.mPartOfMagneticFieldList.isEmpty() ^ 1);
                    return isEmpty;
                case 4:
                    if (this.mIsCalibrated) {
                        isEmpty = (this.mPartOfGyroscopeList.isEmpty() ^ 1) & (this.mPartOfRotationVectorList.isEmpty() ^ 1);
                        return isEmpty;
                    }
                    isEmpty = (this.mPartOfGyroscopeUncalibratedList.isEmpty() ^ 1) & (this.mPartOfRotationVectorList.isEmpty() ^ 1);
                    return isEmpty;
                default:
                    return false;
            }
        }
    }

    private int updateSensorMatrix() {
        ArrayList<SensorData> gyroscopeList;
        ArrayList<SensorData> gyroscopeUncalibratedList;
        ArrayList<SensorData> accelerometerList;
        ArrayList<SensorData> magneticFieldList;
        ArrayList<SensorData> rotationVectorList;
        int ret = 0;
        synchronized (SensorSynchronizedObject) {
            gyroscopeList = (ArrayList) this.mPartOfGyroscopeList.clone();
            gyroscopeUncalibratedList = (ArrayList) this.mPartOfGyroscopeUncalibratedList.clone();
            accelerometerList = (ArrayList) this.mPartOfAccelerometerList.clone();
            magneticFieldList = (ArrayList) this.mPartOfMagneticFieldList.clone();
            rotationVectorList = (ArrayList) this.mPartOfRotationVectorList.clone();
            this.mPartOfGyroscopeList.clear();
            this.mPartOfGyroscopeUncalibratedList.clear();
            this.mPartOfAccelerometerList.clear();
            this.mPartOfMagneticFieldList.clear();
            this.mPartOfRotationVectorList.clear();
        }
        if (this.mStock) {
            if (this.mIsCalibrated) {
                ((ArrayList) this.mAllValueList.get(0)).addAll(gyroscopeList);
            } else {
                ((ArrayList) this.mAllValueList.get(0)).addAll(gyroscopeUncalibratedList);
            }
            ((ArrayList) this.mAllValueList.get(1)).addAll(accelerometerList);
            ((ArrayList) this.mAllValueList.get(2)).addAll(magneticFieldList);
            ((ArrayList) this.mAllValueList.get(3)).addAll(rotationVectorList);
        }
        if (this.mIsCalibrated) {
            if (!gyroscopeList.isEmpty()) {
                ret = setInputSensorData(getSensorDataArray(gyroscopeList), 0);
                if (ret != 0) {
                    LogFilter.e(LOG_TAG, String.format(Locale.US, "SensorFusion.setSensorData(SENSOR_TYPE_GYROSCOPE) error ret:0x%08X", new Object[]{Integer.valueOf(ret)}));
                }
            }
        } else if (!gyroscopeUncalibratedList.isEmpty()) {
            ret = setInputSensorData(getSensorDataArray(gyroscopeUncalibratedList), 0);
            if (ret != 0) {
                LogFilter.e(LOG_TAG, String.format(Locale.US, "SensorFusion.setSensorData(SENSOR_TYPE_GYROSCOPE) error ret:0x%08X", new Object[]{Integer.valueOf(ret)}));
            }
        }
        if (!accelerometerList.isEmpty()) {
            ret = setInputSensorData(getSensorDataArray(accelerometerList), 1);
            if (ret != 0) {
                LogFilter.e(LOG_TAG, String.format(Locale.US, "SensorFusion.setSensorData(SENSOR_TYPE_ACCELEROMETER) error ret:0x%08X", new Object[]{Integer.valueOf(ret)}));
            }
        }
        if (!magneticFieldList.isEmpty()) {
            ret = setInputSensorData(getSensorDataArray(magneticFieldList), 2);
            if (ret != 0) {
                LogFilter.e(LOG_TAG, String.format(Locale.US, "SensorFusion.setSensorData(SENSOR_TYPE_MAGNETIC_FIELD) error ret:0x%08X", new Object[]{Integer.valueOf(ret)}));
            }
        }
        if (!rotationVectorList.isEmpty()) {
            ret = setInputSensorData(getSensorDataArray(rotationVectorList), 3);
            if (ret != 0) {
                LogFilter.e(LOG_TAG, String.format(Locale.US, "SensorFusion.setSensorData(SENSOR_TYPE_ROTATION_VECTOR) error ret:0x%08X", new Object[]{Integer.valueOf(ret)}));
            }
        }
        return (((ret | this.mMorphoSensorFusion.calc()) | this.mMorphoSensorFusion.outputRotationMatrix3x3(1, this.mSensorMatrix[1])) | this.mMorphoSensorFusion.outputRotationMatrix3x3(0, this.mSensorMatrix[0])) | this.mMorphoSensorFusion.outputRotationMatrix3x3(3, this.mSensorMatrix[3]);
    }

    private Object[] getSensorDataArray(ArrayList<SensorData> sd_list) {
        int input_num = sd_list.size();
        Object[] dst = new Object[input_num];
        for (int i = 0; i < input_num; i++) {
            dst[i] = new SensorData(((SensorData) sd_list.get(i)).mTimeStamp, ((SensorData) sd_list.get(i)).mValues);
        }
        sd_list.clear();
        return dst;
    }

    private int setInputSensorData(Object[] sd_array, int sensor_type) {
        if (sd_array == null) {
            return Error.ERROR_PARAM;
        }
        return this.mMorphoSensorFusion.setSensorData(sd_array, sensor_type);
    }

    private void calcRotationMatrix(double[] dst_mat, double alpah, double beta, double gamma) {
        double[] x_mat = createMatrix();
        double[] y_mat = createMatrix();
        double[] z_mat = createMatrix();
        double[] tmp_mat = createMatrix();
        x_mat[4] = Math.cos(beta);
        x_mat[5] = -Math.sin(beta);
        x_mat[7] = Math.sin(beta);
        x_mat[8] = Math.cos(beta);
        y_mat[0] = Math.cos(alpah);
        y_mat[2] = Math.sin(alpah);
        y_mat[6] = -Math.sin(alpah);
        y_mat[8] = Math.cos(alpah);
        z_mat[0] = Math.cos(gamma);
        z_mat[1] = -Math.sin(gamma);
        z_mat[3] = Math.sin(gamma);
        z_mat[4] = Math.cos(gamma);
        multMatrix(tmp_mat, x_mat, y_mat);
        multMatrix(dst_mat, tmp_mat, z_mat);
    }

    private double[] createMatrix() {
        return new double[]{1.0d, Camera2ParamsFragment.TARGET_EV, Camera2ParamsFragment.TARGET_EV, Camera2ParamsFragment.TARGET_EV, 1.0d, Camera2ParamsFragment.TARGET_EV, Camera2ParamsFragment.TARGET_EV, Camera2ParamsFragment.TARGET_EV, 1.0d};
    }

    private void multMatrix(double[] dst_mat, double[] src_mat1, double[] src_m2) {
        double[] matrix = new double[9];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                double sum = Camera2ParamsFragment.TARGET_EV;
                for (int k = 0; k < 3; k++) {
                    sum += src_mat1[(i * 3) + k] * src_m2[(k * 3) + j];
                }
                matrix[(i * 3) + j] = sum;
            }
        }
        System.arraycopy(matrix, 0, dst_mat, 0, matrix.length);
    }
}
