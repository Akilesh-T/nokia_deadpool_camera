package com.morphoinc.core;

public class MorphoSensorFusion {
    public static final int MAXIMUM_DATA_SIZE = 512;
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
    public static final int SENSOR_TYPE_ROTATION_VECTOR = 3;
    public static final int STATE_CALC_OFFSET = 0;
    public static final int STATE_PROCESS = 1;
    private long mNative = 0;

    public static class SensorData {
        public final long mTimeStamp;
        public final double[] mValues;

        public SensorData(long time_stamp, float[] values) {
            this.mTimeStamp = time_stamp;
            this.mValues = new double[values.length];
            for (int i = 0; i < values.length; i++) {
                this.mValues[i] = (double) values[i];
            }
        }

        public SensorData(long time_stamp, double[] values) {
            this.mTimeStamp = time_stamp;
            this.mValues = (double[]) values.clone();
        }
    }

    private native int calc(long j);

    private native long createNativeObject();

    private native void deleteNativeObject(long j);

    private native int finish(long j);

    private native int initialize(long j);

    private static native String nativeGetVersion();

    private native int outputRotationAngle(long j, double[] dArr);

    private native int outputRotationMatrix3x3(long j, int i, double[] dArr);

    private native int setAppState(long j, int i);

    private native int setMode(long j, int i);

    private native int setOffset(long j, SensorData sensorData, int i);

    private native int setOffsetMode(long j, int i);

    private native int setRotation(long j, int i);

    private native int setSensorData(long j, Object[] objArr, int i);

    private native int setSensorReliability(long j, int i, int i2);

    static {
        try {
            System.loadLibrary("morpho_sensor_fusion");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }

    public static String getVersion() {
        return nativeGetVersion();
    }

    public MorphoSensorFusion() {
        long ret = createNativeObject();
        if (ret != 0) {
            this.mNative = ret;
        } else {
            this.mNative = 0;
        }
    }

    public int initialize() {
        if (this.mNative != 0) {
            return initialize(this.mNative);
        }
        return Error.ERROR_STATE;
    }

    public int finish() {
        if (this.mNative == 0) {
            return Error.ERROR_STATE;
        }
        int ret = finish(this.mNative);
        deleteNativeObject(this.mNative);
        this.mNative = 0;
        return ret;
    }

    public int setMode(int mode) {
        if (this.mNative != 0) {
            return setMode(this.mNative, mode);
        }
        return Error.ERROR_STATE;
    }

    public int setAppState(int state) {
        if (this.mNative != 0) {
            return setAppState(this.mNative, state);
        }
        return Error.ERROR_STATE;
    }

    public int setRotation(int rotation) {
        if (this.mNative != 0) {
            return setRotation(this.mNative, rotation);
        }
        return Error.ERROR_STATE;
    }

    public int setSensorReliability(int rel, int sensor_type) {
        if (this.mNative != 0) {
            return setSensorReliability(this.mNative, rel, sensor_type);
        }
        return Error.ERROR_STATE;
    }

    public int setOffsetMode(int offset_mode) {
        if (this.mNative != 0) {
            return setOffsetMode(this.mNative, offset_mode);
        }
        return Error.ERROR_STATE;
    }

    public int setOffset(SensorData data, int sensor_type) {
        if (this.mNative != 0) {
            return setOffset(this.mNative, data, sensor_type);
        }
        return Error.ERROR_STATE;
    }

    public int setSensorData(Object[] data, int sensor_type) {
        if (this.mNative != 0) {
            return setSensorData(this.mNative, data, sensor_type);
        }
        return Error.ERROR_STATE;
    }

    public int calc() {
        if (this.mNative != 0) {
            return calc(this.mNative);
        }
        return Error.ERROR_STATE;
    }

    public int outputRotationMatrix3x3(int sensor_type, double[] dst_mat) {
        if (this.mNative != 0) {
            return outputRotationMatrix3x3(this.mNative, sensor_type, dst_mat);
        }
        return Error.ERROR_STATE;
    }

    public int outputRotationAngle(double[] angle) {
        if (this.mNative != 0) {
            return outputRotationAngle(this.mNative, angle);
        }
        return Error.ERROR_STATE;
    }
}
