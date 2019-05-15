package com.morphoinc.app.panoramagp3;

import android.media.Image;

public abstract class CaptureImage {
    private long mExposureTime;
    protected Image mImage;
    private long mImageTimeStamp;
    private byte[] mRaw;
    private long mRollingShutterSkew;
    private int mSensitivity;
    private long mSensorTimeStamp;

    public abstract int getHeight();

    public abstract String getImageFormat();

    public abstract int getWidth();

    public Image image() {
        return this.mImage;
    }

    public byte[] raw() {
        return (byte[]) this.mRaw.clone();
    }

    CaptureImage(Image image) {
        this.mImage = image;
        this.mRaw = new byte[0];
        this.mImageTimeStamp = image != null ? image.getTimestamp() : System.nanoTime();
        setSensitivity(0);
        setExposureTime(0);
        setRollingShutterSkew(0);
        setSensorTimeStamp(0);
    }

    CaptureImage(byte[] raw) {
        this.mRaw = (byte[]) raw.clone();
        this.mImage = null;
        this.mImageTimeStamp = System.nanoTime();
        setSensitivity(0);
        setExposureTime(0);
        setRollingShutterSkew(0);
        setSensorTimeStamp(0);
    }

    public void close() {
        if (this.mImage != null) {
            this.mImage.close();
            this.mImage = null;
        }
        this.mRaw = new byte[0];
    }

    public long getTimestamp() {
        return this.mImageTimeStamp;
    }

    public void setSensitivity(int value) {
        this.mSensitivity = value;
    }

    public int getSensitivity() {
        return this.mSensitivity;
    }

    public void setExposureTime(long value) {
        this.mExposureTime = value;
    }

    public long getExposureTime() {
        return this.mExposureTime;
    }

    public void setRollingShutterSkew(long value) {
        this.mRollingShutterSkew = value;
    }

    public long getRollingShutterSkew() {
        return this.mRollingShutterSkew;
    }

    public void setSensorTimeStamp(long value) {
        this.mSensorTimeStamp = value;
    }

    public long getSensorTimeStamp() {
        return this.mSensorTimeStamp;
    }
}
